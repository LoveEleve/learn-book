# 03-06 注解扩展与自动注册

## 问题：Spring 的注解驱动存在三个空白

Spring 的 `@Enable*` 注解驱动模型由 `@Import` + `ImportSelector`/`ImportBeanDefinitionRegistrar` 组成，功能强大但存在三个空白：

| 场景 | Spring 能力 | 问题 |
|------|-----------|------|
| **可选 Import** | `@Import` 导入不存在的类会报错 | 无法声明"如果存在就导入"的可选依赖 |
| **注解属性运行时覆盖** | 注解属性是编译时固定的 | 无法通过 `application.properties` 覆盖 `@EnableXxx(timeout=5000)` 中的 `timeout` |
| **SpringFactoriesLoader 只加载类不注册 Bean** | `SpringFactoriesLoader.loadFactories` 返回实例但不注册到容器 | 框架开发者需要手动注册每个 SPI 实现为 Bean |
| **占位符不解析** | `@Value("${prop}")` 解析占位符，但 `@EnableXxx(name="${prop}")` 不解析 | 注解属性中的 `${...}` 不被 Environment 解析 |
| **无法按注解粒度禁用** | `@Conditional` 可以条件化，但无法通过属性控制单个 `@Enable*` | 需要为每个 `@Enable*` 写 `@ConditionalOnProperty` |

microsphere-spring 的 `context/annotation` 包通过**基础类层次 + 元注解 + SPI 自动注册**三层设计填补这些空白。

---

## 设计：五个扩展组件

### 整体架构

```
AnnotatedBeanCapableImportCandidate<A> (基类)
    │  ├── 注解类型解析（GenericTypeResolver）
    │  ├── 占位符解析（ResolvablePlaceholderAnnotationAttributes）
    │  ├── 按注解粒度禁用（microsphere.spring.{annotation}.enabled）
    │  └── 按类+注解粒度禁用（microsphere.spring.{class}@{annotation}.enabled）
    │
    ├── AnnotatedBeanCapableImportBeanDefinitionRegistrar<A>
    │       └── 子类：EventExtensionRegistrar, AutoRegistrationBeanRegistrar, ConfigurationBeanBindingRegistrar...
    │
    └── AnnotatedBeanCapableImportSelector<A>
            └── 子类：ImportOptionalSelector

@OverrideAnnotationAttributes (元注解)
    └── ConfigurationPropertyOverrideAnnotationAttributesStrategy
            └── 从 Environment 属性覆盖注解属性

@EnableAutoRegistrationBean (自动注册)
    └── AutoRegistrationBeanRegistrar
            └── 从 spring.factories 加载 AutoRegistrationBean 实现并注册为 Bean

@ImportOptional (可选 Import)
    └── ImportOptionalSelector
            └── 类不存在时静默跳过

BeanSource (三路发现)
    ├── BEAN_FACTORY
    ├── SPRING_FACTORIES
    └── JAVA_SERVICE_PROVIDER
```

---

### AnnotatedBeanCapableImportCandidate：注解驱动基类

#### 继承体系

```
BeanCapableImportCandidate (EnvironmentAware, BeanClassLoaderAware)
    │
    └── AnnotatedBeanCapableImportCandidate<A extends Annotation>
            │  - annotationType: 通过 ResolvableType 解析泛型参数
            │  - getAnnotationAttributes(): 解析注解属性 + 占位符替换
            │  - isEnabled(): 按属性检查是否启用
            │
            ├── AnnotatedBeanCapableImportBeanDefinitionRegistrar<A>
            │       implements ImportBeanDefinitionRegistrar
            │       final registerBeanDefinitions(metadata, registry, nameGenerator) {
            │           if (isEnabled(metadata)) {
            │               attrs = getAnnotationAttributes(metadata);  // 占位符已解析
            │               registerBeanDefinitions(metadata, registry, nameGenerator, attrs);
            │           }
            │       }
            │
            └── AnnotatedBeanCapableImportSelector<A>
                    implements ImportSelector
                    final selectImports(metadata) {
                        imports = newLinkedHashSet();
                        selectImports(metadata, getAnnotationAttributes(metadata), imports);
                        return imports.toArray();
                    }
```

#### 占位符解析

`getAnnotationAttributes` 返回 `ResolvablePlaceholderAnnotationAttributes`，它继承 Spring 的 `AnnotationAttributes`，在构造时用 `PropertyResolver` 解析所有属性值中的 `${...}` 占位符：

```java
// 原始注解：@EnableXxx(name = "${app.name}", timeout = "${app.timeout}")
// Environment: app.name=my-app, app.timeout=5000

ResolvablePlaceholderAnnotationAttributes<EnableXxx> attrs = getAnnotationAttributes(metadata);
attrs.getString("name");    // "my-app"（占位符已解析）
attrs.getInt("timeout");    // 5000
```

**层次结构**：
- `AnnotationAttributes`（Spring）-> `GenericAnnotationAttributes<A>`（添加 `annotationType()` + `equals`/`hashCode`）-> `ResolvablePlaceholderAnnotationAttributes<A>`（添加占位符解析）

`GenericAnnotationAttributes` 的 `equals`/`hashCode` 使用 `Arrays.deepEquals`/`deepHashCode`，正确处理数组类型属性（如 `String[]`）。

#### 按注解粒度禁用

`isEnabled` 检查两级属性，使用注解的**全限定类名**：

1. **类+注解级**（优先）：`microsphere.spring.{importing-class-full-name}@{annotation-full-name}.enabled`
2. **注解级**（fallback）：`microsphere.spring.{annotation-full-name}.enabled`

两者都未设置时，默认为 `true`（启用）。类+注解级优先于注解级，意味着可以全局启用某个注解，但为特定配置类禁用：

```properties
# 全局启用 @EnableEventExtension（使用全限定名）
microsphere.spring.io.microsphere.spring.context.event.EnableEventExtension.enabled=true

# 但为 AppConfig 禁用（类+注解级）
microsphere.spring.com.example.AppConfig@io.microsphere.spring.context.event.EnableEventExtension.enabled=false
```

**注意**：这与 `ConfigurableApplicationContextInitializer`（第 1/4/5 篇的 `EventPublishingBeanInitializer`、`ListenableAutowireCandidateResolverInitializer`、`ListenableConfigurableEnvironmentInitializer`）的禁用机制不同。后者默认禁用（`getDefaultEnabled()` 返回 `false`），而 `AnnotatedBeanCapableImportCandidate` 默认启用。两个机制独立运作，控制不同的组件。

---

### @OverrideAnnotationAttributes：运行时注解属性覆盖

#### 元注解设计

`@OverrideAnnotationAttributes` 是一个标注在注解上的元注解（`@Target(ANNOTATION_TYPE)`），声明该注解的属性可以被运行时配置覆盖：

```java
@OverrideAnnotationAttributes(strategy = ConfigurationPropertyOverrideAnnotationAttributesStrategy.class)
public @interface EnableEventExtension {
    boolean intercepted() default true;
    String executorForListener() default "N/E";
}
```

#### 覆盖策略

`ConfigurationPropertyOverrideAnnotationAttributesStrategy` 是默认策略，从 Environment 属性覆盖注解属性。

**属性命名规则**：`microsphere.spring.prefix.{annotation-simple-name}.{attribute-name}`

```properties
# 覆盖 @EnableEventExtension 的 intercepted 属性
microsphere.spring.prefix.EnableEventExtension.intercepted=false

# 覆盖 @EnableEventExtension 的 executorForListener 属性
microsphere.spring.prefix.EnableEventExtension.executorForListener=myExecutor
```

**前缀可定制**：默认前缀 `microsphere.spring.prefix.{annotation-simple-name}.` 可以通过 `microsphere.spring.prefix.{annotation-full-name}` 属性覆盖：

```properties
# 将 @EnableEventExtension 的属性前缀改为 "my.event."
microsphere.spring.prefix.io.microsphere.spring.context.event.EnableEventExtension=my.event.
# 此时覆盖属性变为：
my.event.intercepted=false
```

**覆盖逻辑**：
1. 计算属性前缀：先查 `microsphere.spring.prefix.{annotation-full-name}` 属性，未设置则用默认 `microsphere.spring.prefix.{annotation-simple-name}.`
2. 从 Environment 读取该前缀下的所有子属性
3. 对每个注解属性，查找同名的配置属性
4. 如果找到，用 `ConversionService` 转换为属性类型
5. 替换原始值；未找到则保留原始值

**可替换策略**：`@OverrideAnnotationAttributes(strategy = MyStrategy.class)` 允许用户提供自定义策略，实现 `OverrideAnnotationAttributesStrategy` 接口。

#### 与 AnnotatedBeanCapableImportCandidate 的配合

`AnnotatedBeanCapableImportCandidate` 在 `getAnnotationAttributes` 中检查注解是否标注了 `@OverrideAnnotationAttributes`。如果是，应用策略覆盖属性值，然后再做占位符解析。顺序是：**覆盖 -> 占位符解析**。

这意味着：先用配置属性覆盖注解属性（可能覆盖为 `${...}` 占位符），再解析占位符。两层组合提供了极大的灵活性。

---

### @EnableAutoRegistrationBean：SPI 自动注册

#### 问题

Spring 的 `SpringFactoriesLoader.loadFactories(Interface.class, classLoader)` 返回接口的实现实例，但不注册到 Spring 容器。框架开发者需要手动将每个实现注册为 Bean，或者为每个实现写 `@Bean` 方法。

#### 设计

`AutoRegistrationBean` 接口标记可自动注册的 Bean：

```java
public interface AutoRegistrationBean extends Ordered {
    String getBeanName();           // 默认：类名首字母小写
    Class<?> getBeanType();         // 默认：实现类本身
    String getScope();              // 默认：singleton
    boolean isAutoRegistered(ConfigurableEnvironment env);  // 默认：true
    void customize(BeanDefinitionBuilder builder);           // 默认：空
}
```

`@EnableAutoRegistrationBean` 注解触发注册：

```java
@EnableAutoRegistrationBean
@Configuration
public class MyConfig { }
```

`AutoRegistrationBeanRegistrar`（`ImportBeanDefinitionRegistrar`）的注册流程：

1. `SpringFactoriesLoader.loadFactories(AutoRegistrationBean.class, classLoader)` 加载所有实现
2. 对每个实现：
   - 检查 `registry.containsBeanDefinition(beanName)` -> 已注册则跳过
   - 检查 `autoRegistrationBean.isAutoRegistered(environment)` -> 为 false 则跳过
   - 创建 `BeanDefinition`，设置 `InstanceSupplier` 返回 SPI 实例
   - 调用 `customize(builder)` 允许自定义
   - 注册到 `BeanDefinitionRegistry`

#### 按 Bean 粒度禁用

每个 `AutoRegistrationBean` 可以通过属性单独禁用：

```properties
# 禁用名为 "myService" 的自动注册 Bean
microsphere.spring.beans.myService.auto-registered=false
```

属性命名规则：`microsphere.spring.beans.{bean-name}.auto-registered`

#### 自动启用

`AutoRegistrationBeanInitializer`（`ApplicationContextInitializer`）在主 `spring.factories` 中注册，自动激活 `@EnableAutoRegistrationBean`。它通过注册一个内部 `@EnableAutoRegistrationBean` 配置类来实现，无需用户手动添加注解。

全局开关：`microsphere.spring.beans.auto-registered=true`（默认 `true`，即 `AutoRegistrationBeanInitializer` 默认启用）。

---

### @ImportOptional：可选 Import

#### 问题

Spring 的 `@Import` 导入不存在的类会在 `ConfigurationClassParser` 阶段抛出 `ClassNotFoundException`。对于可选依赖（如 "如果 classpath 中有 X 就导入 XConfig"），需要写 `@ConditionalOnClass` + `@Import` 组合，繁琐且不直观。

#### 设计

```java
@ImportOptional("com.example.OptionalConfig")
@ImportOptional("com.another.OptionalFeature")
@Configuration
public class MyConfig { }
```

`ImportOptionalSelector`（`ImportSelector`）解析 `value` 中的类名，用 `ClassLoaderUtils.resolveClass` 检查类是否存在。存在则导入，不存在则静默跳过。

```java
protected void selectImports(AnnotationMetadata metadata,
                              ResolvablePlaceholderAnnotationAttributes<ImportOptional> attrs,
                              Set<String> imports) {
    String[] classNames = attrs.getStringArray("value");
    Stream.of(classNames)
          .map(className -> resolveClass(className, getClassLoader()))
          .filter(Objects::nonNull)     // 类不存在则跳过
          .map(Class::getName)
          .forEach(imports::add);
}
```

`@ImportOptional` 支持 `@Repeatable`，可以声明多个。同时标注了 `@OverrideAnnotationAttributes`，允许通过配置属性覆盖 `value`。

---

### BeanSource：三路 Bean 发现

`BeanSource` 枚举定义了三种 Bean 发现方式，被 `@EnableEventExtension(sources = ...)` 等注解使用：

| Source | 发现方式 | 返回 |
|--------|---------|------|
| `BEAN_FACTORY` | `beanFactory.getBeanNamesForType(type)` | 已注册的 Bean 名称 |
| `SPRING_FACTORIES` | `SpringFactoriesLoader.loadFactoryNames(type, classLoader)` | `META-INF/spring.factories` 中的类名 |
| `JAVA_SERVICE_PROVIDER` | `ServiceLoader.load(type)` 遍历 | `META-INF/services` 中的实现类 |

`BeanSource.registerBeans(beanFactory, registry, sources, type)` 方法：
1. 按 `sources` 顺序收集所有实现类
2. 对 `BEAN_FACTORY` 来源：只查询不注册（Bean 已存在）
3. 对 `SPRING_FACTORIES` 和 `JAVA_SERVICE_PROVIDER` 来源：通过 `BeanRegistrar.registerGenericBeans` 注册为 Bean

这个设计让框架开发者可以通过一个注解属性控制 Bean 的发现来源，而不是硬编码 `SpringFactoriesLoader` 或 `ServiceLoader`。

---

### ExposingClassPathBeanDefinitionScanner：暴露 protected 方法

Spring 的 `ClassPathBeanDefinitionScanner` 有两个 protected 方法：
- `doScan(basePackages)` - 执行扫描
- `checkCandidate(beanName, beanDefinition)` - 检查候选 Bean 是否冲突

`ExposingClassPathBeanDefinitionScanner` 将这两个方法暴露为 public，并添加了 `registerBeanDefinition` 和 `registerSingleton` 便捷方法。它还在构造时调用 `registerAnnotationConfigProcessors(registry)`，确保注解驱动的后置处理器已注册。

这是一个简单的"暴露模式"--通过子类化将 protected 方法变为 public，供框架代码使用。

---

## 永恒原理

### 1. 泛型类型解析与注解类型推断

`AnnotatedBeanCapableImportCandidate<A>` 在构造时通过 `ResolvableType` 解析泛型参数 `A` 的实际类型：

```java
protected Class<A> resolveAnnotationType() {
    return resolveGeneric(AnnotatedBeanCapableImportCandidate.class, 0);
}
```

这利用了 Java 的泛型信息在子类中保留的特性（通过 `Class.getGenericSuperclass()`）。子类声明 `class EventExtensionRegistrar extends AnnotatedBeanCapableImportBeanDefinitionRegistrar<EnableEventExtension>` 时，`A` 被解析为 `EnableEventExtension.class`。

**与 `GenericBeanPostProcessorAdapter` 的对比**：第 4 篇的 `GenericBeanPostProcessorAdapter<T>` 使用 `GenericTypeResolver.resolveTypeArgument` 做同样的事情。两者用了不同的 API（`ResolvableType` vs `GenericTypeResolver`），但原理相同--从子类的泛型签名中提取类型参数。

### 2. 注解属性的"三层处理"

microsphere 对注解属性的处理分三层，顺序固定：

```
原始注解属性
    │
    ├── 第一层：@OverrideAnnotationAttributes 覆盖
    │       从 Environment 属性覆盖注解属性值
    │       例如：microsphere.spring.EnableEventExtension.intercepted=false
    │
    ├── 第二层：占位符解析
    │       解析 ${...} 占位符
    │       例如：@EnableXxx(name="${app.name}") -> name="my-app"
    │
    └── 第三层：业务逻辑使用
            子类的 registerBeanDefinitions/selectImports 方法中使用解析后的属性
```

第一层和第二层都是可选的--如果没有标注 `@OverrideAnnotationAttributes`，跳过第一层；如果属性中没有 `${...}`，第二层无效果。

### 3. SPI 三路发现的统一抽象

Spring 生态中有三种 Bean 发现机制：
- **BeanFactory**：已注册的 Spring Bean
- **SpringFactoriesLoader**：`META-INF/spring.factories`
- **ServiceLoader**：`META-INF/services/`

三者各自独立，使用方式不同。`BeanSource` 枚举将三者统一为"发现来源"概念，通过 `sources` 属性让用户选择启用哪些来源。这是一个"策略枚举"模式--每个枚举值封装了不同的发现逻辑。

### 4. 按注解粒度的条件化

Spring 的 `@ConditionalOnProperty` 可以条件化整个 `@Configuration` 类或 `@Bean` 方法，但无法条件化单个 `@Enable*` 注解。microsphere 的 `isEnabled` 检查提供了两级粒度：

- **注解级**：`microsphere.spring.{annotation-class}.enabled` -> 对所有使用该注解的配置类生效
- **类+注解级**：`microsphere.spring.{class}@{annotation}.enabled` -> 只对特定配置类生效

这种设计在框架开发中很有用：框架可以声明 `@EnableXxx` 默认启用，用户通过属性按需禁用，而无需修改代码。

---

## 边界与反例

### 1. 泛型类型解析在匿名类中失效

`ResolvableType` 依赖 `Class.getGenericSuperclass()` 返回的 `ParameterizedType`。如果子类是匿名类：

```java
// 正确：具名类
class MyRegistrar extends AnnotatedBeanCapableImportBeanDefinitionRegistrar<EnableMyFeature> { }

// 错误：匿名类（泛型信息可能丢失）
new AnnotatedBeanCapableImportBeanDefinitionRegistrar<EnableMyFeature>() { }
```

匿名类的泛型签名在某些 JVM 版本中可能不被保留，导致 `resolveAnnotationType` 返回 null 或 Object。

### 2. @OverrideAnnotationAttributes 的属性名冲突

`ConfigurationPropertyOverrideAnnotationAttributesStrategy` 使用注解的简单名（`annotationType.getSimpleName()`）作为属性前缀。如果两个不同包的同名注解都标注了 `@OverrideAnnotationAttributes`，它们的属性前缀相同，可能导致配置冲突。

**缓解**：使用全限定类名作为前缀可以避免冲突，但会使得属性名冗长。microsphere 选择了简单名以提升可用性。

### 3. AutoRegistrationBean 的 InstanceSupplier 语义

`AutoRegistrationBeanRegistrar` 使用 `InstanceSupplier` 返回 SPI 实例。这意味着 SPI 实例是**预先创建**的（在 `SpringFactoriesLoader.loadFactories` 时），而非由 Spring 容器创建。SPI 实例不经过 `BeanPostProcessor` 处理，不支持 `@Autowired` 注入。

**后果**：`AutoRegistrationBean` 实现不应依赖其他 Spring Bean。如果需要依赖注入，应该注册为普通 `@Component`，而非通过 SPI。

### 4. @ImportOptional 的类加载时序

`ImportOptionalSelector` 在 `ConfigurationClassParser` 阶段执行，此时 BeanFactory 尚未完全初始化。`ClassLoaderUtils.resolveClass` 使用线程上下文 ClassLoader，如果在某些容器中（如 OSGi）ClassLoader 不一致，可能找不到实际存在的类。

### 5. isEnabled 的属性检查时序

`AnnotatedBeanCapableImportCandidate#isEnabled` 在 `registerBeanDefinitions`/`selectImports` 入口处检查。此时 Environment 已经初始化（`ApplicationContextInitializer` 阶段之后），但可能尚未加载所有 PropertySource。如果 `enabled` 属性定义在尚未加载的 PropertySource 中，检查结果可能不正确。

---

## 现代 Spring（6.x）是否已支持？

| microsphere 特性 | Spring 6.x 是否有等价物 | 说明 |
|------------------|----------------------|------|
| `@ImportOptional` | 无 | Spring 6.x 的 `@Import` 仍不支持可选导入 |
| `@OverrideAnnotationAttributes` | 无 | Spring 6.x 的注解属性仍不可运行时覆盖 |
| `@EnableAutoRegistrationBean` | 无 | Spring 6.x 的 `SpringFactoriesLoader` 仍不自动注册 Bean |
| 注解属性占位符解析 | 部分 | Spring 6.x 的 `@Value` 支持占位符，但 `@Enable*` 注解属性不支持 |
| 按注解粒度禁用 | 无 | Spring 6.x 没有 `@Enable*` 级别的条件化机制 |
| `AnnotatedBeanCapableImportCandidate` 基类 | 无 | Spring 6.x 没有提供带占位符解析和禁用检查的 Import 基类 |
| `BeanSource` 三路发现 | 无 | Spring 6.x 没有统一 BeanFactory/SpringFactories/ServiceLoader 的抽象 |

Spring 6.0 引入了 `@Import` 对 `ImportSelector` 返回 `null` 的支持（之前返回 null 会报错），但 `@ImportOptional` 的"类不存在就跳过"语义仍然需要单独实现。

Spring Boot 的 `@ConditionalOnClass` 可以实现类似 `@ImportOptional` 的效果，但它是条件化整个配置类，不是可选导入特定类。且 `@ConditionalOnClass` 是 Spring Boot 特有的，不属于 Spring Framework。

---

## 小结

microsphere-spring 的注解扩展与自动注册，通过五个组件将 Spring 的注解驱动模型从"编译时固定"变为"运行时可配置"：

1. **`AnnotatedBeanCapableImportCandidate`** 基类：泛型类型解析 + 占位符解析 + 按注解粒度禁用，为所有 `@Enable*` 注解的 Registrar/Selector 提供统一基础
2. **`@OverrideAnnotationAttributes`** 元注解：从 Environment 属性覆盖注解属性，实现运行时配置
3. **`@EnableAutoRegistrationBean`**：从 `spring.factories` 自动加载并注册 Bean，支持按 Bean 粒度禁用
4. **`@ImportOptional`**：可选导入，类不存在时静默跳过
5. **`BeanSource`** 三路发现：统一 BeanFactory/SpringFactories/ServiceLoader

这五个组件的共同设计理念是：**让框架开发者的 `@Enable*` 注解更灵活、更可配置**。用户通过 `application.properties` 就能控制注解行为（覆盖属性、禁用启用、选择来源），无需修改代码。这对框架开发尤为重要--框架不能要求用户修改源码来调整行为，一切可配置性都应该通过属性暴露。
