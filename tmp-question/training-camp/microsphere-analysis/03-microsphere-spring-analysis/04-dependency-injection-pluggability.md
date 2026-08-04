# 03-04 依赖注入可插拔化

## 问题：Spring 的依赖解析是"黑盒"

Spring 的依赖注入由 `DefaultListableBeanFactory#doResolveDependency` 驱动，核心委托给 `AutowireCandidateResolver`。这个解析器负责三件事：

1. **`isAutowireCandidate`**：判断 BeanDefinition 是否是自动装配候选
2. **`getSuggestedValue`**：获取 `@Value` 表达式或 `@Qualifier` 建议值
3. **`getLazyResolutionProxyIfNecessary`**：为 `@Lazy` 创建延迟代理

问题在于：**这个解析过程没有任何通知机制**。你无法知道某个 `@Value` 被解析成了什么值，无法知道 `@Lazy` 代理何时被创建，更无法在解析过程中插入自定义逻辑。

类似地，Spring 的注入点解析（`@Autowired`/`@Resource` 字段和方法）由 `AutowiredAnnotationBeanPostProcessor` 内部完成，不可扩展、不可插拔。如果你想在 `@Autowired` 解析时做额外处理（如条件过滤、审计日志），只能继承 `AutowiredAnnotationBeanPostProcessor` 覆盖方法--侵入性极高。

microsphere-spring 的 `beans/factory` 包通过**装饰器 + 监听器 + SPI** 三层设计，将依赖注入的解析过程从黑盒变为白盒。

---

## 设计：三个维度的可插拔化

### 整体架构

```
DefaultListableBeanFactory
    │
    ├── AutowireCandidateResolver (被 ListenableAutowireCandidateResolver 装饰)
    │       ├── delegate: 原始解析器（ContextAnnotationAutowireCandidateResolver）
    │       └── CompositeAutowireCandidateResolvingListener
    │               ├── LoggingAutowireCandidateResolvingListener (示例实现，需手动注册)
    │               └── 用户自定义 Listener (Bean 或 spring.factories)
    │
    ├── InjectionPointDependencyResolver (SPI 可插拔)
    │       ├── InjectionPointDependencyResolvers (复合)
    │       │       ├── ConstructionInjectionPointDependencyResolver
    │       │       ├── AutowiredInjectionPointDependencyResolver
    │       │       └── ResourceInjectionPointDependencyResolver
    │       └── 用于第 2 篇的并行初始化依赖分析
    │
    └── ConfigurationBeanBindingPostProcessor (配置 Bean 绑定)
            ├── ConfigurationBeanBinder (DataBinder 实现)
            └── ConfigurationBeanCustomizer (后置定制)
```

三个维度分别解决：
- **解析监听**：`ListenableAutowireCandidateResolver` 装饰原始解析器，在解析后通知 Listener
- **注入点可插拔**：`InjectionPointDependencyResolver` 通过 SPI 加载，支持自定义注解的注入点解析
- **配置 Bean 绑定**：`@EnableConfigurationBeanBinding` 将 Properties 绑定到 POJO Bean

---

### ListenableAutowireCandidateResolver：装饰器 + 监听器

#### 装饰器设计

`ListenableAutowireCandidateResolver` 实现了 `AutowireCandidateResolver` 接口，同时持有原始解析器作为委托：

```java
public class ListenableAutowireCandidateResolver implements AutowireCandidateResolver,
        BeanFactoryPostProcessor, BeanNameAware {

    private AutowireCandidateResolver delegate;  // 原始解析器
    private CompositeAutowireCandidateResolvingListener compositeListener;

    @Override
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
        Object suggestedValue = delegate.getSuggestedValue(descriptor);  // 委托原始解析
        compositeListener.suggestedValueResolved(descriptor, suggestedValue);  // 通知 Listener
        return suggestedValue;
    }

    @Override
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
        Object proxy = delegate.getLazyResolutionProxyIfNecessary(descriptor, beanName);
        compositeListener.lazyProxyResolved(descriptor, beanName, proxy);
        return proxy;
    }
}
```

其他方法（`isAutowireCandidate`、`isRequired`、`hasQualifier`、`cloneIfNecessary`）直接委托，不通知 Listener--因为这些方法不涉及"解析"，只是判断。

#### 作为 BeanFactoryPostProcessor 的自动安装

`ListenableAutowireCandidateResolver` 同时实现 `BeanFactoryPostProcessor`，在 `postProcessBeanFactory` 中自动替换 BeanFactory 的原始解析器：

```java
@Override
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    wrap(beanFactory);
}

public void wrap(BeanFactory beanFactory) {
    DefaultListableBeanFactory dbf = asDefaultListableBeanFactory(beanFactory);
    AutowireCandidateResolver original = dbf.getAutowireCandidateResolver();
    if (original != this) {
        this.delegate = original;  // 保存原始解析器
        dbf.setAutowireCandidateResolver(this);  // 替换为自身
    }
}
```

这个设计意味着：只要 `ListenableAutowireCandidateResolver` 注册为 Spring Bean，它就会在 BeanFactoryPostProcessor 阶段自动装饰原始解析器，对应用透明。

#### 监听器接口与发现

```java
public interface AutowireCandidateResolvingListener {

    default void suggestedValueResolved(DependencyDescriptor descriptor, Object suggestedValue) {
    }

    default void lazyProxyResolved(DependencyDescriptor descriptor, String beanName, Object proxy) {
    }
}
```

两个回调都是 `default` 空实现，Listener 可以只覆盖关心的方法。

**发现机制**（`loadListeners` 静态方法）：
1. 从 `SpringFactoriesLoader` 加载（`META-INF/spring.factories`）
2. 从 `BeanFactory.getBeansOfType` 加载
3. 合并后按 `AnnotationAwareOrderComparator` 排序

`LoggingAutowireCandidateResolvingListener` 是一个示例实现，在 TRACE 级别打印解析日志。注意它只在测试的 `spring.factories` 中注册，生产环境需要用户手动注册为 Bean 或添加到 `spring.factories`。

#### 启用方式

通过 `ListenableAutowireCandidateResolverInitializer`（`ApplicationContextInitializer`），注册为 Spring Bean。默认禁用：

```properties
microsphere.spring.listenable-autowire-candidate-resolver.enabled=true
```

启用后，`ListenableAutowireCandidateResolver` 被注册为 BeanDefinition，在 BeanFactoryPostProcessor 阶段自动装饰原始解析器。

---

### InjectionPointDependencyResolver：SPI 可插拔的注入点解析

第 2 篇已经从"并行初始化依赖分析"的角度介绍了这个体系。这里从"可插拔化"的角度重新审视。

#### SPI 架构

```
InjectionPointDependencyResolver (接口)
    │  resolve(Field/Method/Constructor/Parameter, BeanFactory, Set<String>)
    │
    ├── InjectionPointDependencyResolvers (复合，通过 SpringFactoriesLoader 加载)
    │       │
    │       ├── ConstructionInjectionPointDependencyResolver
    │       │       只处理 Constructor 参数
    │       │
    │       ├── AutowiredInjectionPointDependencyResolver
    │       │       只处理 @Autowired 标注的 Field/Parameter
    │       │       支持参数上无 @Autowired 但方法/构造器上有 @Autowired 的情况
    │       │
    │       └── ResourceInjectionPointDependencyResolver
    │               只处理 @Resource 标注的 Field/Parameter
    │               支持 @Resource(type) 按类型、@Resource(name) 按名称、默认按字段名
```

三个解析器通过 `META-INF/spring.factories` 注册：

```properties
io.microsphere.spring.beans.factory.InjectionPointDependencyResolver=\
io.microsphere.spring.beans.factory.ConstructionInjectionPointDependencyResolver,\
io.microsphere.spring.beans.factory.annotation.AutowiredInjectionPointDependencyResolver,\
io.microsphere.spring.beans.factory.annotation.ResourceInjectionPointDependencyResolver
```

**可扩展性**：用户可以实现 `InjectionPointDependencyResolver` 并通过 `spring.factories` 注册，支持自定义注解的注入点解析。例如，实现一个 `@InjectMe` 注解的解析器，只需：

```java
public class InjectMeDependencyResolver
        extends AnnotatedInjectionPointDependencyResolver<InjectMe> {
    // 继承 AbstractInjectionPointDependencyResolver 的先名后型解析逻辑
}
```

注册到 `META-INF/spring.factories` 即可被 `InjectionPointDependencyResolvers` 自动发现。

#### 先名后型解析策略

`AbstractInjectionPointDependencyResolver` 对每个注入点采用两步解析：

1. **按名解析**：`AutowireCandidateResolver#getSuggestedValue(DependencyDescriptor)` 返回 `@Qualifier` 或 `@Resource(name)` 指定的名称
2. **按型解析**：如果按名失败，`getBeanNamesForType(dependentType, false, false)` 按类型查找所有匹配 Bean

参数化类型递归解析到最后一个类型参数：`List<B>` -> `B`、`Map<String, B>` -> `B`、`Optional<B>` -> `B`。

`ResolvableDependencyTypeFilter` 过滤掉容器内对象类型（`BeanFactory`、`ApplicationContext` 等），避免虚假依赖。

---

### ConfigurationBeanBinding：Properties 到 POJO 的绑定

#### Spring 的空白

Spring Boot 的 `@ConfigurationProperties` 将 Properties 绑定到 Bean，但有几个限制：
- 绑定目标必须是 Spring Bean（通过 `@Component` 或 `@EnableConfigurationProperties` 注册）
- 不支持从 Properties 动态生成**多个** Bean（如多个数据源配置）
- 绑定逻辑使用 Spring Boot 的 `Binder` API，不可替换

microsphere 的 `@EnableConfigurationBeanBinding` 提供了更灵活的方案：

```java
@EnableConfigurationBeanBinding(
    prefix = "app.datasource",      // Properties 前缀
    type = DataSourceConfig.class,  // 绑定目标类型
    multiple = true,                // 是否生成多个 Bean
    ignoreUnknownFields = true,     // 忽略未知字段
    ignoreInvalidFields = true      // 忽略无效字段
)
@Configuration
public class MyConfig {
}
```

#### 工作流程

```
@EnableConfigurationBeanBinding(prefix="app.datasource", type=DataSourceConfig.class, multiple=true)
    │
    ├── ConfigurationBeanBindingRegistrar (ImportBeanDefinitionRegistrar)
    │       ├── 从 Environment 读取 "app.datasource" 下的子前缀
    │       │   如 "app.datasource.primary" 和 "app.datasource.secondary"
    │       ├── 为每个子前缀注册一个 DataSourceConfig 的 BeanDefinition
    │       │   Bean name = "primary" 或 "secondary"
    │       │   BeanDefinition 属性: configurationProperties, ignoreUnknownFields, ignoreInvalidFields
    │       └── 注册 ConfigurationBeanBindingPostProcessor（如果尚未注册）
    │
    └── ConfigurationBeanBindingPostProcessor (BeanPostProcessor, PriorityOrdered)
            ├── postProcessBeforeInitialization:
            │       ├── 检查 BeanDefinition 是否是 configuration bean
            │       ├── 从 BeanDefinition 取出 configurationProperties (Map<String, Object>)
            │       ├── ConfigurationBeanBinder.bind(properties, bean)
            │       │       └── DefaultConfigurationBeanBinder:
            │       │               DataBinder + MutablePropertyValues + ConversionService
            │       └── ConfigurationBeanCustomizer.customize(beanName, bean)
            └── postProcessAfterInitialization: 直接返回 bean
```

#### `multiple = true` 的多 Bean 生成

当 `multiple = true` 时，`ConfigurationBeanBindingRegistrar` 从 Properties 中解析子前缀。例如：

```properties
app.datasource.primary.url=jdbc:mysql://host1:3306/db
app.datasource.primary.username=root
app.datasource.secondary.url=jdbc:mysql://host2:3306/db
app.datasource.secondary.username=root
```

会生成两个 Bean：`primary`（`DataSourceConfig`）和 `secondary`（`DataSourceConfig`），各自绑定 `app.datasource.primary.*` 和 `app.datasource.secondary.*` 的属性。

#### ConfigurationBeanBinder 的可替换性

`ConfigurationBeanBinder` 是一个接口，默认实现 `DefaultConfigurationBeanBinder` 使用 Spring 的 `DataBinder`：

```java
public void bind(Map<String, Object> properties, boolean ignoreUnknown,
                 boolean ignoreInvalid, Object bean) {
    DataBinder dataBinder = new DataBinder(bean);
    dataBinder.setIgnoreInvalidFields(ignoreUnknown);
    dataBinder.setIgnoreUnknownFields(ignoreInvalid);
    dataBinder.initDirectFieldAccess();  // 直接字段访问，不通过 setter
    dataBinder.setConversionService(conversionService);
    dataBinder.bind(new MutablePropertyValues(properties));
}
```

用户可以提供自定义 `ConfigurationBeanBinder` 实现（如使用 Spring Boot 的 `Binder` API），通过 `spring.factories` 或 Bean 注册替换默认实现。

`ConfigurationBeanCustomizer` 是后置定制接口，允许在绑定完成后对 Bean 做额外处理（如验证、初始化）。

---

### BeanRegistrar：统一的 Bean 注册工具

`BeanRegistrar` 是一个纯静态工具类，提供多种 Bean 注册方式：

| 方法 | 用途 | 机制 |
|------|------|------|
| `registerBean(registry, name, bean)` | 注册实例 | `AbstractBeanDefinition#setInstanceSupplier(() -> bean)` |
| `registerFactoryBean(registry, name, bean)` | 注册为 FactoryBean | `DelegatingFactoryBean` 包装 |
| `registerGenericBean(registry, beanClass)` | 按类注册 | `genericBeanDefinition(beanClass)` |
| `registerSpringFactoriesBeans(registry, factoryClass)` | 从 spring.factories 注册 | `SpringFactoriesLoader.loadFactoryNames` |
| `registerInfrastructureBean(registry, name, type)` | 基础设施 Bean | `role = ROLE_INFRASTRUCTURE` |
| `registerSingleton(registry, name, bean)` | 注册单例 | `SingletonBeanRegistry#registerSingleton` |

**`registerBean` vs `registerFactoryBean`**：

`registerBean` 使用 `setInstanceSupplier`（Spring 5.0+），直接将实例作为 Bean。这是最轻量的方式，不经过 FactoryBean 中间层。

`registerFactoryBean` 使用 `DelegatingFactoryBean` 包装实例。`DelegatingFactoryBean` 是一个通用 FactoryBean，持有委托对象，在 `getObject()` 时返回它。它还传播 Aware 接口（`ApplicationContextAware`、`BeanNameAware`）和生命周期回调（`InitializingBean`、`DisposableBean`）到委托对象。

**何时用 FactoryBean**：当注册的对象需要参与 Spring 的 Aware 回调和生命周期管理时，用 `DelegatingFactoryBean`。当只需要注册一个静态实例时，用 `registerBean`。

---

### DelegatingFactoryBean：通用 FactoryBean 适配器

`DelegatingFactoryBean` 将任意对象适配为 Spring FactoryBean：

```java
public class DelegatingFactoryBean implements FactoryBean<Object>, InitializingBean,
        DisposableBean, ApplicationContextAware, BeanNameAware {

    private final Object delegate;
    private final Class<?> objectType;  // 通过 AopUtils.getTargetClass 解析
    private final boolean singleton;

    @Override
    public Object getObject() { return delegate; }

    @Override
    public Class<?> getObjectType() { return objectType; }

    @Override
    public void afterPropertiesSet() {
        invokeInitializingBean(delegate);  // 传播 InitializingBean
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        invokeAwareInterfaces(delegate, context);  // 传播 Aware 接口
    }

    @Override
    public void destroy() {
        if (delegate instanceof DisposableBean d) d.destroy();  // 传播 DisposableBean
    }
}
```

**设计要点**：

- `objectType` 使用 `AopUtils.getTargetClass(delegate)` 而非 `delegate.getClass()`，正确处理 AOP 代理场景
- Aware 接口传播通过 `BeanUtils.invokeAwareInterfaces` 统一处理（`EnvironmentAware`、`ResourceLoaderAware`、`ApplicationEventPublisherAware` 等）
- `isSingleton` 可配置，支持 prototype 模式

---

### GenericBeanPostProcessorAdapter：类型安全的 BeanPostProcessor

Spring 的 `BeanPostProcessor` 对所有 Bean 调用，开发者需要手动判断 Bean 类型。`GenericBeanPostProcessorAdapter<T>` 通过泛型自动过滤：

```java
public abstract class GenericBeanPostProcessorAdapter<T> implements BeanPostProcessor {

    private final Class<T> beanType;

    public GenericBeanPostProcessorAdapter() {
        this.beanType = (Class<T>) resolveTypeArgument(getClass(),
                GenericBeanPostProcessorAdapter.class);
    }

    @Override
    public final Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> beanClass = ultimateTargetClass(bean);  // 处理 AOP 代理
        if (beanType.isAssignableFrom(beanClass)) {
            return doPostProcessAfterInitialization((T) bean, beanName);
        }
        return bean;  // 类型不匹配，直接返回
    }
}
```

**设计要点**：

- 构造时通过 `GenericTypeResolver.resolveTypeArgument` 解析泛型参数 `T` 的实际类型
- `ultimateTargetClass` 解析 AOP 代理的目标类，确保类型匹配正确
- `postProcessBeforeInitialization` 和 `postProcessAfterInitialization` 标记为 `final`，子类只能覆盖 `doPostProcessBeforeInitialization`/`doPostProcessAfterInitialization`
- 子类可以选择覆盖 `processBeforeInitialization`/`processAfterInitialization`（void 返回值，更简洁）或 `doPostProcess*`（返回可能修改的 Bean）

第 3 篇的 `InterceptingApplicationEventMulticasterProxy` 就继承了 `GenericBeanPostProcessorAdapter<ApplicationListener>`，只处理 `ApplicationListener` 类型的 Bean。

---

### 别名生成器：可插拔的 Bean 命名策略

Spring 的 `BeanNameGenerator` 生成 Bean 名称，但没有"别名生成"概念。microsphere 提供了 `ConfigurationBeanAliasGenerator` 接口：

```java
public interface ConfigurationBeanAliasGenerator {
    String generateAlias(String prefix, String beanName, Class<?> configClass);
}
```

三个内置实现：

| 实现 | 规则 | 示例 |
|------|------|------|
| `DefaultConfigurationBeanAliasGenerator` | `SimpleName(configClass) + capitalize(beanName)` | `DataSourceConfigPrimary` |
| `HyphenAliasGenerator` | `prefix + "-" + beanName` | `app-primary` |
| `UnderScoreJoinAliasGenerator` | `prefix + "_" + beanName` | `app_primary` |

`HyphenAliasGenerator` 和 `UnderScoreJoinAliasGenerator` 继承 `JoinAliasGenerator`，通过 `delimiter()` 方法差异化分隔符。这是模板方法模式的简洁应用。

`GenericBeanNameGenerator` 是 `BeanNameGenerator` 的实现，通过 `BeanUtils.generateBeanName(beanType)` 生成名称（类名首字母小写），作为 `BeanRegistrar` 的默认命名策略。

---

## 永恒原理

### 1. 装饰器模式与"不可扩展接口"的扩展

Spring 的 `AutowireCandidateResolver` 是一个接口，但 `DefaultListableBeanFactory` 在构造时硬编码了 `ContextAnnotationAutowireCandidateResolver` 作为默认实现。用户虽然可以通过 `setAutowireCandidateResolver` 替换，但替换后丢失原始实现的功能（如 `@Value` 解析）。

`ListenableAutowireCandidateResolver` 用装饰器模式解决了这个矛盾：它实现同一接口，持有原始解析器作为委托，在委托调用后通知 Listener。原始解析器的功能完全保留，新增的监听能力"附加"在装饰器上。

**装饰器的核心约束**：装饰器必须实现与被装饰者相同的接口，并将所有方法委托给被装饰者。`ListenableAutowireCandidateResolver` 的 `isAutowireCandidate`、`isRequired`、`hasQualifier` 等方法都是纯委托，只有 `getSuggestedValue` 和 `getLazyResolutionProxyIfNecessary` 在委托后附加了通知。

### 2. SPI 与 SpringFactoriesLoader 的结合

microsphere 的 `InjectionPointDependencyResolver` 和 `AutowireCandidateResolvingListener` 都使用 `SpringFactoriesLoader` 作为发现机制。`SpringFactoriesLoader` 是 Spring 的 SPI 机制，类似于 Java 的 `ServiceLoader` 但更灵活：

| 维度 | Java ServiceLoader | Spring SpringFactoriesLoader |
|------|--------------------|-----------------------------|
| 配置文件 | `META-INF/services/接口全名` | `META-INF/spring.factories` |
| 格式 | 每行一个实现类全名 | `接口全名=实现类1,实现类2,...` |
| 加载方式 | `ServiceLoader.load(接口)` | `SpringFactoriesLoader.loadFactories(接口, classLoader)` |
| 与 Spring 集成 | 无 | 可通过 `BeanFactory` 增强（如注入依赖） |

microsphere 的 `SpringFactoriesLoaderUtils.loadFactories(BeanFactory, Class)` 是对原生 `SpringFactoriesLoader` 的增强：加载后如果实现类是 `BeanFactoryAware`，会注入 BeanFactory。

### 3. 先名后型的依赖解析策略

`AbstractInjectionPointDependencyResolver` 的解析策略是"先名后型"：先用 `AutowireCandidateResolver#getSuggestedValue` 获取名称建议，失败后按类型查找。

这与 Spring 原生的 `@Autowired` 解析顺序一致（先 `@Qualifier`，后类型），但 `@Resource` 不同--`@Resource` 默认按名称（字段名），找不到再按类型。microsphere 的 `ResourceInjectionPointDependencyResolver` 正确处理了这个差异：`@Resource(name)` 按名称，`@Resource(type)` 按类型，默认按字段名。

### 4. DataBinder 与 Spring Boot Binder 的对比

`DefaultConfigurationBeanBinder` 使用 Spring 的 `DataBinder` 进行属性绑定，而 Spring Boot 2.x 使用自研的 `Binder` API。两者的核心差异：

| 维度 | DataBinder | Spring Boot Binder |
|------|-----------|-------------------|
| 验证 | 支持（JSR-303） | 不直接支持 |
| 字段访问 | setter 或直接字段（`initDirectFieldAccess`） | 直接字段或 setter |
| 嵌套 | 通过 `PropertyValues` 扁平化 | 通过 `ConfigurationPropertyName` 导航 |
| 类型转换 | `ConversionService` | 内置 `ConversionService` + 专用转换器 |
| 松散绑定 | 不支持 | 支持（kebab-case/camelCase/underscore） |

microsphere 选择 `DataBinder` 是因为它属于 Spring Framework（不依赖 Spring Boot），且支持验证。`ConfigurationBeanBinder` 接口的可替换性允许用户在 Spring Boot 环境中替换为 `Binder` 实现。

---

## 边界与反例

### 1. ListenableAutowireCandidateResolver 的安装时序

`ListenableAutowireCandidateResolver` 通过 `BeanFactoryPostProcessor#postProcessBeanFactory` 替换原始解析器。但 `BeanFactoryPostProcessor` 在 `invokeBeanFactoryPostProcessors` 阶段执行，此时其他 `BeanFactoryPostProcessor` 可能已经使用了原始解析器。

**后果**：在 `ListenableAutowireCandidateResolver` 安装之前的 `BeanFactoryPostProcessor` 中的依赖解析不会触发 Listener。这通常不影响业务 Bean（它们在更晚的阶段被创建），但可能影响基础设施 Bean。

### 2. ConfigurationBeanBinding 的 `multiple = true` 依赖 Properties 结构

`multiple = true` 要求 Properties 按两级结构组织：`prefix.beanName.property=value`。如果 Properties 只有一级（`prefix.property=value`），Registrar 无法确定要生成多少个 Bean。

**边界**：当 `multiple = false` 时，Properties 可以是一级结构（`prefix.property=value`），只生成一个 Bean。这是 `@ConfigurationProperties` 的行为模式。

### 3. DefaultConfigurationBeanBinder 的参数交换 Bug

`DefaultConfigurationBeanBinder#bind` 方法存在参数交换 bug：

```java
public void bind(Map<String, Object> properties, boolean ignoreUnknownFields,
                 boolean ignoreInvalidFields, Object bean) {
    DataBinder dataBinder = new DataBinder(bean);
    dataBinder.setIgnoreInvalidFields(ignoreUnknownFields);   // 传错了！应为 ignoreInvalidFields
    dataBinder.setIgnoreUnknownFields(ignoreInvalidFields);   // 传错了！应为 ignoreUnknownFields
```

`ignoreUnknownFields` 参数被传给了 `setIgnoreInvalidFields`，`ignoreInvalidFields` 参数被传给了 `setIgnoreUnknownFields`。这意味着用户设置 `ignoreUnknownFields=true`、`ignoreInvalidFields=false` 时，实际行为是 `ignoreInvalidFields=true`、`ignoreUnknownFields=false`--完全相反。

**影响**：使用 `@EnableConfigurationBeanBinding(ignoreUnknownFields=true, ignoreInvalidFields=false)` 时，未知字段不会被忽略（可能抛异常），而无效字段会被忽略（可能隐藏错误）。

**缓解**：可以通过实现自定义 `ConfigurationBeanBinder` 替换默认实现，或在 `@EnableConfigurationBeanBinding` 中将两个参数值互换以"对冲"这个 bug（不推荐，因为修复版本后行为会反转）。

### 4. DelegatingFactoryBean 的 AOP 限制

`DelegatingFactoryBean#getObjectType` 使用 `AopUtils.getTargetClass(delegate)` 解析目标类。如果 delegate 是 AOP 代理，返回的是被代理类（而非代理类）。这意味着 Spring 在类型匹配时会使用被代理类，而非代理类。

**反例**：如果 delegate 是通过 CGLIB 创建的代理，`getTargetClass` 返回原始类。但 `getObject()` 返回代理对象。如果其他 Bean 按"代理类"类型注入（而非接口），可能找不到匹配的 Bean。

### 5. GenericBeanPostProcessorAdapter 的泛型擦除

`GenericTypeResolver.resolveTypeArgument` 在运行时通过反射解析泛型参数。如果子类是匿名类或继承了参数化父类，解析可能失败。例如：

```java
// 可以正确解析 T = MyBean
class MyProcessor extends GenericBeanPostProcessorAdapter<MyBean> { }

// 无法正确解析 T（匿名类）
BeanPostProcessor p = new GenericBeanPostProcessorAdapter<MyBean>() { };
```

**缓解**：始终使用具名类继承 `GenericBeanPostProcessorAdapter`，避免匿名类。

### 6. InjectionPointDependencyResolver 的静态发现

`InjectionPointDependencyResolvers` 通过 `SpringFactoriesLoader.loadFactories` 加载解析器，这是静态发现--在构造时一次性加载，运行时不可动态增减。如果需要运行时添加解析器，需要直接操作 `InjectionPointDependencyResolvers` 的内部列表（但它是 final 的）。

---

## 现代 Spring（6.x）是否已支持？

| microsphere 特性 | Spring 6.x 是否有等价物 | 说明 |
|------------------|----------------------|------|
| `ListenableAutowireCandidateResolver` | 无 | Spring 6.x 的 `AutowireCandidateResolver` 仍无监听机制 |
| `InjectionPointDependencyResolver` SPI | 无 | Spring 6.x 的注入点解析仍由 `AutowiredAnnotationBeanPostProcessor` 内部完成 |
| `@EnableConfigurationBeanBinding` | 部分 | Spring Boot 的 `@ConfigurationProperties` + `@EnableConfigurationProperties` 类似，但不支持 `multiple` |
| `BeanRegistrar` | 无 | Spring 6.x 没有统一的 Bean 注册工具类 |
| `DelegatingFactoryBean` | 无 | Spring 没有通用的 FactoryBean 适配器 |
| `GenericBeanPostProcessorAdapter` | 无 | Spring 没有类型安全的 BeanPostProcessor 基类 |

Spring 6.0 引入了 `BeanDefinition#setInstanceSupplier`（替代 `Supplier`），microsphere 的 `BeanRegistrar.registerBean` 已经使用这个 API。Spring 6.1 没有新增依赖注入可插拔化的特性。

---

## 小结

microsphere-spring 的依赖注入可插拔化，通过三个维度将 Spring 的"黑盒解析"变为"白盒可扩展"：

1. **解析监听**（`ListenableAutowireCandidateResolver`）：装饰器模式，在 `AutowireCandidateResolver` 的解析后通知 Listener，支持 `@Value` 和 `@Lazy` 的解析监听
2. **注入点可插拔**（`InjectionPointDependencyResolver`）：SPI 机制，通过 `spring.factories` 加载解析器，支持 `@Autowired`/`@Resource`/构造器/`@Bean` 方法，可扩展自定义注解
3. **配置 Bean 绑定**（`@EnableConfigurationBeanBinding`）：Properties 到 POJO 的绑定，支持 `multiple` 多 Bean 生成、`DataBinder` 可替换

辅助工具（`BeanRegistrar`、`DelegatingFactoryBean`、`GenericBeanPostProcessorAdapter`、别名生成器）降低了框架开发的门槛，让自定义 `BeanPostProcessor`、`FactoryBean` 和 Bean 注册变得类型安全且简洁。
