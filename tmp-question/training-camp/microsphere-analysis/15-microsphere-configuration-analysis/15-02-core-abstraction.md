# 15-02 核心抽象层：元注解体系与事件驱动

## 目录

- [三层层级](#三层层级)
- [第 1 层：@PropertySourceExtension 元注解](#第-1-层propertysourceextension-元注解)
- [第 2 层：PropertySourceExtensionLoader 模板方法](#第-2-层propertysourceextensionloader-模板方法)
- [第 3 层：PropertySourcesChangedEvent 事件体系](#第-3-层propertysourceschangedevent-事件体系)
- [完整加载流程](#完整加载流程)
- [工程问题分析](#工程问题分析)

---

## 三层层级

microsphere-configuration 的核心代码不在 15 本身，而在共享库 `microsphere-spring-context` 中。15 的四个后端模块只是这个核心的三层结构的具体实现。

```
第 1 层：元注解（编译期）
  @PropertySourceExtension
    └─ 四个后端注解 @NacosPorpertySource / @ApolloPropertySource / @EtcdPropertySource / @ZookeeperPropertySource
       └─ 标注在 @Configuration 类上声明配置来源

第 2 层：加载器（启动时）
  PropertySourceExtensionLoader<A, EA>
    └─ NacosPropertySourceLoader / EtcdPropertySourceLoader / ZookeeperPropertySourceLoader
       └─ 通过 @Import(Loader.class) 触发，加载配置到 Environment

第 3 层：事件（运行时）
  PropertySourcesChangedEvent + PropertySourceChangedEvent
    └─ 配置变更时发布到 Spring ApplicationContext
       └─ 监听器（如 18-dynamic）收到事件后做热更新
```

每一层都不依赖具体的配置中心——Nacos/Apollo/etcd/ZK 的差异只在第 2 层的 `resolveResources()` 和 `configureResourcePropertySourcesRefresher()` 中体现。

---

## 第 1 层：@PropertySourceExtension 元注解

### 定义

位置：`microsphere-spring-context/.../config/context/annotation/PropertySourceExtension.java`

```java
@Target(ANNOTATION_TYPE)     // 只允许标注在其他注解上
@Retention(RUNTIME)
@Inherited                    // 子类可继承
@Documented
public @interface PropertySourceExtension {

    String name() default "";

    boolean autoRefreshed() default false;

    boolean first() default false;

    String before() default "";

    String after() default "";

    String[] value() default {};

    Class<? extends Comparator<Resource>> resourceComparator() default DefaultResourceComparator.class;

    boolean ignoreResourceNotFound() default false;

    String encoding() default "UTF-8";

    Class<? extends PropertySourceFactory> factory() default DefaultPropertySourceFactory.class;
}
```

### 属性说明

| 属性 | 作用 | 对应 Spring `@PropertySource` |
|------|------|------------------------------|
| `name` | PropertySource 名称 | `name` |
| `value` / `key` | 资源路径 | `value` |
| `encoding` | 编码 | `encoding` |
| `factory` | 资源工厂 | `factory` |
| `ignoreResourceNotFound` | 忽略资源不存在 | `ignoreResourceNotFound` |
| `first` | 放到 Environment 首位 | ❌ 不支持 |
| `before` | 在指定 PropertySource 之前 | ❌ 不支持 |
| `after` | 在指定 PropertySource 之后 | ❌ 不支持 |
| `autoRefreshed` | 运行时自动刷新 | ❌ 不支持 |
| `resourceComparator` | 资源排序器 | ❌ 不支持 |

前 5 个属性是 Spring `@PropertySource` 已有的，后 4 个是扩展属性。

### @AliasFor 映射

四个后端的注解各自标注 `@PropertySourceExtension`，并通过 `@AliasFor` 将属性映射到元注解：

```java
// @NacosPorpertySource 中的映射
@PropertySourceExtension
public @interface NacosPorpertySource {

    @AliasFor(annotation = PropertySourceExtension.class)
    String name() default "";

    @AliasFor(annotation = PropertySourceExtension.class)
    boolean autoRefreshed() default true;  // ← 默认 true

    @AliasFor(annotation = PropertySourceExtension.class)
    boolean first() default false;

    @AliasFor(annotation = PropertySourceExtension.class, attribute = "value")
    String[] key() default {};  // ← 将 key 映射到 value

    // Nacos 特有属性
    String serverAddress() default "http://127.0.0.1:8848";
    OpenApiVersion openApiVersion() default OpenApiVersion.V1;
}
```

当 Spring 处理 `@NacosPorpertySource(key = "app.json", autoRefreshed = true)` 时，会合成（synthesize）一个 `@PropertySourceExtension` 注解，其 `value` 属性为 `"app.json"`，`autoRefreshed` 为 `true`。后续的 Loader 只读取元注解的属性，不关心具体是哪个后端的注解。

### @Inherited 的效果

`@PropertySourceExtension` 标注了 `@Inherited`，这意味着：

```java
@NacosPorpertySource(key = "app.json")
public class BaseConfig {}

// 不加任何注解也能继承父类的配置
public class SubConfig extends BaseConfig {}
```

这在多环境配置继承的场景中有用——公共配置放在父类，子类按环境覆盖。

---

## 第 2 层：PropertySourceExtensionLoader 模板方法

### 两个并行的类体系

microsphere-spring-context 定义了两个独立的抽象类，分别对应不同的触发方式：

```
ImportSelector 体系（通过 @Import 触发）
  AnnotatedBeanCapableImportSelector
    └─ AnnotatedPropertySourceLoader<A>       （注解→属性→loadPropertySource）
         └─ PropertySourceExtensionLoader<A, EA>（抽象模板）
              ├─ NacosPropertySourceLoader
              ├─ EtcdPropertySourceLoader
              └─ ZookeeperPropertySourceLoader

ImportBeanDefinitionRegistrar 体系（通过 @Import 触发）
  ApolloPropertySourceBeanDefinitionRegistrar  （直接实现 ImportBeanDefinitionRegistrar）
```

注意：`@NacosPorpertySource` 上有 `@Import(NacosPropertySourceLoader.class)`，`NacosPropertySourceLoader` 通过继承链最终实现 `ImportSelector`，Spring 的 `ConfigurationClassParser` 处理 `@Import` 时调用 `selectImports()` 触发加载流程。Apollo 不走这个体系——它用 `ApolloPropertySourceBeanDefinitionRegistrar`（实现 `ImportBeanDefinitionRegistrar`），因为 Apollo 的 `ConfigPropertySource` 机制需要复用原生 SDK。

### PropertySourceExtensionLoader

位置：`microsphere-spring-context/.../config/context/annotation/PropertySourceExtensionLoader.java`

核心模板方法：

```java
public abstract class PropertySourceExtensionLoader<A extends Annotation, EA> {

    // 子类必须实现的抽象方法：从配置中心拉取内容
    protected abstract Resource[] resolveResources(EA attributes, String propertySourceName, String resourceValue) throws Throwable;

    // 子类可选覆盖的方法：注册配置监听器
    protected void configureResourcePropertySourcesRefresher(
            EA attributes,
            List<PropertySourceResource> propertySourceResources,
            CompositePropertySource propertySource,
            ResourcePropertySourcesRefresher refresher) throws Throwable {
        // 默认空实现，子类覆盖
    }

    // 模板方法：加载配置入口
    public void loadPropertySource(AnnotationAttributes attributes, ...) {

        // Step 1: 创建属性对象
        EA attrs = createAttributes(attributes);

        // Step 2: 获取资源 key（value 属性）
        String[] resourceValues = getResourceValues(attrs);
        String propertySourceName = resolvePropertySourceName(attrs);

        for (String resourceValue : resourceValues) {
            // Step 3: 子类从配置中心拉取内容
            Resource[] resources = resolveResources(attrs, propertySourceName, resourceValue);

            // Step 4: 包装为 ResourcePropertySource
            // Step 5: 加入 CompositePropertySource
            for (Resource resource : resources) {
                propertySource.addPropertySource(new ResourcePropertySource(resource));
            }
        }

        // Step 6: 注册到 Spring Environment
        // ...

        // Step 7: 如果启用自动刷新，配置监听器
        if (attrs.isAutoRefreshed()) {
            configureResourcePropertySourcesRefresher(attrs, propertySourceResources, propertySource, refresher);
        }
    }
}
```

### 模板方法的作用

| 步骤 | 谁负责 | 做了什么 |
|------|--------|---------|
| 解析注解属性 | 父类 `loadPropertySource()` | 读取 `@PropertySourceExtension` 的通用属性 |
| 拉取配置内容 | 子类 `resolveResources()` | Nacos 从 REST API 拉，etcd 从 Jetcd 拉，ZK 从 Curator 拉 |
| 组装 PropertySource | 父类 `loadPropertySource()` | 创建 `CompositePropertySource` + `ResourcePropertySource` |
| 注册到 Environment | 父类 `loadPropertySource()` | 调用 `environment.getPropertySources().addLast()` |
| 注册监听器 | 子类 `configureResourcePropertySourcesRefresher()` | Nacos 调用 `addEventListener()`，etcd 调用 `watch()` |

### AnnotatedPropertySourceLoader（上层父类）

`AnnotatedPropertySourceLoader<A>` 是 `PropertySourceExtensionLoader` 的父类，继承 `AnnotatedBeanCapableImportSelector`：

```
AnnotatedBeanCapableImportSelector（实现 ImportSelector）
  └─ AnnotatedPropertySourceLoader<A>（引入注解属性解析能力）
       └─ PropertySourceExtensionLoader<A, EA>（模板方法）
```

```java
public abstract class AnnotatedPropertySourceLoader<A extends Annotation>
        extends AnnotatedBeanCapableImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        // 获取 @NacosPorpertySource 等注解的属性
        AnnotationAttributes attributes = getAnnotationAttributes(metadata);
        // 调用 loadPropertySource(...)
        loadPropertySource(attributes, ...);
        return new String[0];  // 不导入其他配置类
    }
}
```

当 Spring 处理 `@Import(NacosPropertySourceLoader.class)` 时，由于 `NacosPropertySourceLoader` 的继承链（`→ PropertySourceExtensionLoader → AnnotatedPropertySourceLoader → AnnotatedBeanCapableImportSelector`）最终实现了 `ImportSelector`，所以 `selectImports()` 被调用，触发完整的配置加载流程。

---

## 第 3 层：PropertySourcesChangedEvent 事件体系

### 事件类型

两个事件类，都定义在 `microsphere-spring-context` 中：

```
PropertySourceChangedEvent（单个变更）
  └─ Kind: ADDED / REPLACED / REMOVED
  └─ 持有单个 PropertySource 的新旧值

PropertySourcesChangedEvent（聚合变更）
  └─ 包装一组 PropertySourceChangedEvent
  └─ 提供 getChangedProperties() / getAddedProperties() / getRemovedProperties()
```

### PropertySourceChangedEvent

```java
public class PropertySourceChangedEvent extends ApplicationContextEvent {

    public enum Kind { ADDED, REPLACED, REMOVED }

    private final Kind kind;
    private final PropertySource<?> newPropertySource;
    private final PropertySource<?> oldPropertySource;

    // 工厂方法
    public static PropertySourceChangedEvent added(ApplicationContext context, PropertySource<?> ps) { ... }
    public static PropertySourceChangedEvent replaced(ApplicationContext context, PropertySource<?> newPS, PropertySource<?> oldPS) { ... }
    public static PropertySourceChangedEvent removed(ApplicationContext context, PropertySource<?> ps) { ... }
}
```

### PropertySourcesChangedEvent

```java
public class PropertySourcesChangedEvent extends ApplicationContextEvent {

    private final List<PropertySourceChangedEvent> subEvents;

    // 便捷方法：获取所有变更的属性（ADDED + REPLACED）
    public Map<String, Object> getChangedProperties() { ... }

    // 获取新增的属性
    public Map<String, Object> getAddedProperties() { ... }

    // 获取移除的属性
    public Map<String, Object> getRemovedProperties() { ... }
}
```

### 发布路径

有两个发布路径：

**路径 A：通过 PropertySourceExtensionLoader（Nacos/etcd/ZK）**

```
配置中心推送变更
  → 子类的监听器收到通知（Nacos ConfigClient.addEventListener / etcd Watch / ZK CuratorCache）
  → 调用 ResourcePropertySourcesRefresher.refresh(resourceValue, resource)
    → 比对新旧资源，计算 diff
    → 创建 PropertySourceChangedEvent（ADDED / REPLACED / REMOVED）
    → 创建 PropertySourcesChangedEvent
    → context.publishEvent(event)
```

**路径 B：通过 ApolloPropertySourceBeanDefinitionRegistrar（Apollo 特殊路径）**

```
Apollo ConfigChangeListener 收到变更
  → 分类为 added / modified / deleted
  → 创建三个 MapPropertySource 分别包装
  → 创建 PropertySourceChangedEvent
  → 创建 PropertySourcesChangedEvent
  → context.publishEvent(event)
```

### 消费者

`PropertySourcesChangedEvent` 是一个标准的 Spring `ApplicationEvent`，任何 `ApplicationListener<PropertySourcesChangedEvent>` 都能收到：

```java
@Component
public class MyListener implements ApplicationListener<PropertySourcesChangedEvent> {
    @Override
    public void onApplicationEvent(PropertySourcesChangedEvent event) {
        Map<String, Object> changed = event.getChangedProperties();
        // 刷新缓存、重建连接等
    }
}
```

18-dynamic 的 `PropagatingDynamicJdbcConfigChangedEventListener` 就是这样一个消费者。

---

## 完整加载流程

把三层串联起来，一个配置注解从声明到生效的完整流程：

```
编译期：
  @NacosPorpertySource(key = "app.json", autoRefreshed = true)
    └─ @PropertySourceExtension（元注解）
       └─ @Import(NacosPropertySourceLoader.class)

启动时：
  Spring 处理 @Configuration 类
    → 发现 @Import(NacosPropertySourceLoader.class)
      → NacosPropertySourceLoader.selectImports() 被调用
        → loadPropertySource()
          → 解析注解属性（name, autoRefreshed, first 等）
          → resolveResources() 调用 Nacos REST API 拉取配置
          → 包装为 ByteArrayResource
          → 创建 CompositePropertySource
          → 添加到 Environment
          → 如果 autoRefreshed=true
            → configureResourcePropertySourcesRefresher()
              → ConfigClient.addEventListener() 注册监听器

运行时（配置变更）：
  Nacos 服务端配置变更
    → ConfigClient.addEventListener() 回调
      → refresher.refresh(resourceValue, resource)
        → 比对新旧 PropertySource
        → PropertySourceChangedEvent(REPLACED)
        → PropertySourcesChangedEvent 发布
          → 所有 ApplicationListener 收到事件
```

---

## 工程问题分析

### 为什么 Apollo 不走 Loader 体系

Apollo 的 `ConfigPropertySource` 机制与 Nacos/etcd/ZK 不同——Apollo 客户端内部已经管理了 `Config` 对象和 `ConfigChangeListener`，不需要像 Nacos 那样自实现客户端。复用 Apollo 的 `ConfigService.getConfig()` 比重新实现 Apollo 的客户端更合理。

代价是 Apollo 的加载路径与其他三个不一致——Nacos/etcd/ZK 通过 `PropertySourceExtensionLoader` 模板方法，Apollo 通过 `ApolloPropertySourceBeanDefinitionRegistrar`。这意味着 Apollo 模块无法利用 Loader 的通用能力（如 `first`/`before`/`after` 排序控制）。

### ResourcePropertySourcesRefresher 的 diff 机制

`refresher.refresh()` 内部通过比对新旧 `Resource` 的 `content`（字节数组）来判断是否有变化。只有内容变化时才发布事件。这意味着：

1. 配置中心推送了但没有内容变化 → 不发布事件
2. 配置中心推送了且内容变化 → 发布 `PropertySourcesChangedEvent`
3. 配置中心删除了配置 → 发布 `PropertySourcesChangedEvent(REMOVED)`

### @Inherited + @AliasFor 的组合复杂度

`@PropertySourceExtension` 用 `@Inherited`，四个后端注解用 `@AliasFor(annotation = PropertySourceExtension.class)`。Spring 在处理这种"继承的元注解 + 属性别名"时需要合成注解——这在 Spring 4.2+ 中支持，但在测试中容易被忽略（比如 `AnnotationMetadata.getAnnotationAttributes()` 在不理解合成的旧版本中可能返回空）。

### 事件发布的两次触发

在 `PropertySourceExtensionLoader` 中，`refresher.refresh()` 发布 `PropertySourcesChangedEvent`。在 Apollo 的 Registrar 中，`ConfigChangeListener` 也发布 `PropertySourcesChangedEvent`。同一个事件类型两种发布路径——消费者不需要关心事件从哪个路径来的，但调试时需要区分事件来源。当前没有在事件中标记来源。

### 事件的作用域限制

`PropertySourcesChangedEvent` 继承 `ApplicationContextEvent`，事件与发布它的 `ApplicationContext` 绑定。在 18-dynamic 的子上下文架构中，如果子上下文中发布了此事件，父上下文中的监听器**收不到**。18-dynamic 的 `PropagatingDynamicJdbcConfigChangedEventListener` 注册在父上下文中，所以它只能消费父上下文发布的事件——这意味着配置变更必须在根上下文（或正确的上下文中）发布才能被正确处理。
