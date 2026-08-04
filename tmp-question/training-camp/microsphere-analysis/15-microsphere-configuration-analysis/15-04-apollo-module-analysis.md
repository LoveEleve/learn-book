# 15-04 Apollo 模块源码解析

## 目录

- [模块概述](#模块概述)
- [ApolloPropertySource 注解](#apollopropertysource-注解)
- [ApolloPropertySourceBeanDefinitionRegistrar 注册器](#apollopropertysourcebeandefinitionregistrar-注册器)
- [与 Nacos 模块的对比](#与-nacos-模块的对比)
- [完整工作流程](#完整工作流程)
- [工程问题分析](#工程问题分析)

---

## 模块概述

Apollo 模块是四个后端实现中**唯一复用原生 SDK**的模块。它直接使用 `com.ctrip.framework.apollo:apollo-client` 的 `Config` 和 `ConfigChangeListener`，不做自己的客户端实现。

2 个文件，约 330 行：

| 文件 | 行数 | 职责 |
|------|------|------|
| `ApolloPropertySource.java` | 121 | 注解声明 |
| `ApolloPropertySourceBeanDefinitionRegistrar.java` | 208 | 注册器 + 事件监听 + 事件转换 |

### 与 Nacos 模块的根本差异

| 维度 | Nacos | Apollo |
|------|-------|--------|
| 触发方式 | `@Import(Loader.class)` → `ImportSelector.selectImports()` | `@Import(Registrar.class)` → `ImportBeanDefinitionRegistrar.registerBeanDefinitions()` |
| 配置加载 | Loader 自实现：从 Nacos 拉取 → 注册到 Environment | 依赖 Apollo 原生 `PropertySourcesProcessor`：`@EnableApolloConfig` 自动完成 |
| 元注解 | `@PropertySourceExtension`（15 的统一抽象） | `@EnableApolloConfig`（Apollo 原生注解） |
| 刷新 | `autoRefreshed` → 注册 `ConfigClient.addEventListener()` | `autoRefreshed` → 注册 `Config.addChangeListener()` |
| 事件 | 由 `PropertySourceExtensionLoader.refresher` 统一发布 | 由 Registrar 自己发布 |
| 属性设置 | 无 | 设置 System properties（appId、meta、cluster、namespace） |

Apollo 模块不通过 `@PropertySourceExtension` 体系——它直接使用 `@EnableApolloConfig`（Apollo 原生注解），通过 `@AliasFor` 将 `namespace` 和 `order` 映射到 `@EnableApolloConfig`。`@ApolloPropertySource` 相当于在 `@EnableApolloConfig` 外面包了一层，加了 `autoRefreshed` 属性和事件转换逻辑。

---

## ApolloPropertySource 注解

位置：`apollo-spring/.../annotation/ApolloPropertySource.java`（121 行）

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@EnableApolloConfig
@Import(ApolloPropertySourceBeanDefinitionRegistrar.class)
public @interface ApolloPropertySource {

    @AliasFor(annotation = EnableApolloConfig.class, attribute = "value")
    String[] namespace() default "${apollo.bootstrap.namespaces:application}";

    @AliasFor(annotation = EnableApolloConfig.class, attribute = "order")
    int order() default Ordered.LOWEST_PRECEDENCE;

    String appId() default "${app.id:default}";

    String meta() default "${apollo.meta:}";

    String cluster() default "${apollo.cluster:default}";

    boolean autoRefreshed() default true;
}
```

### 设计要点

| 设计 | 说明 |
|------|------|
| `@EnableApolloConfig` | Apollo 原生的启用注解——触发 Apollo 的 `PropertySourcesProcessor`，自动加载配置并注册到 Environment |
| `@Import(ApolloPropertySourceBeanDefinitionRegistrar.class)` | 触发自定义注册器，在 Apollo 配置加载完成后添加事件监听 |
| `@AliasFor(annotation = EnableApolloConfig.class)` | `namespace` 和 `order` 映射到 `@EnableApolloConfig`，保持与原生 Apollo API 兼容 |
| `autoRefreshed` | 不属于 `@EnableApolloConfig`——是 15 自己加的属性，控制是否注册 `ConfigChangeListener` |
| Placeholder 默认值 | 所有属性使用 `${...}` 表达式支持从 Spring Environment 读取默认值 |

### 属性说明

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `namespace` | `"${apollo.bootstrap.namespaces:application}"` | Apollo 命名空间，映射到 `@EnableApolloConfig.value()` |
| `order` | `Ordered.LOWEST_PRECEDENCE` | PropertySource 顺序，映射到 `@EnableApolloConfig.order()` |
| `appId` | `"${app.id:default}"` | Apollo AppId，写入 System property `app.id` |
| `meta` | `"${apollo.meta:}"` | Apollo Meta Server 地址，写入 System property `apollo.meta` |
| `cluster` | `"${apollo.cluster:default}"` | 集群名，写入 System property `apollo.cluster` |
| `autoRefreshed` | `true` | 是否启用自动刷新 |

---

## ApolloPropertySourceBeanDefinitionRegistrar 注册器

位置：`apollo-spring/.../annotation/ApolloPropertySourceBeanDefinitionRegistrar.java`（208 行）

### 类声明

```java
public class ApolloPropertySourceBeanDefinitionRegistrar extends BeanCapableImportCandidate
        implements ImportBeanDefinitionRegistrar, BeanFactoryPostProcessor, ApplicationContextAware {

    private ResolvablePlaceholderAnnotationAttributes attributes;
    private ApplicationContext context;
    private ConcurrentMap<String, PropertySource> oldPropertySourcesMap;
}
```

三个接口：

| 接口 | 用途 |
|------|------|
| `ImportBeanDefinitionRegistrar` | 被 `@Import` 触发——解析注解属性，设置 System properties |
| `BeanFactoryPostProcessor` | 在 BeanFactory 准备后执行——此时 Apollo 的 `PropertySourcesProcessor` 已完成，`CompositePropertySource` 可用 |
| `ApplicationContextAware` | 获取 ApplicationContext 用于发布事件 |

### registerBeanDefinitions()——解析注解

```java
@Override
public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
    Class<? extends Annotation> annotationType = ApolloPropertySource.class;
    Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(annotationType.getName());
    ResolvablePlaceholderAnnotationAttributes attributes = of(annotationAttributes, annotationType, getEnvironment());
    setSystemPropertiesFromAttributes(attributes);
    this.attributes = attributes;
}
```

做的事情：读取 `@ApolloPropertySource` 的属性 → 设置 System properties → 保存 attributes 供后续使用。

#### setSystemPropertiesFromAttributes()

```java
private static void setSystemPropertiesFromAttributes(ResolvablePlaceholderAnnotationAttributes attributes) {
    String appId = attributes.getString("appId");
    String meta = attributes.getString("meta");
    String cluster = attributes.getString("cluster");
    String[] namespace = attributes.getStringArray("namespace");

    setSystemProperty(System.getProperties(), APP_ID, appId);
    setSystemProperty(System.getProperties(), APOLLO_META, meta);
    setSystemProperty(System.getProperties(), APOLLO_CLUSTER, cluster);
    setSystemProperty(System.getProperties(), APOLLO_BOOTSTRAP_NAMESPACES, StringUtils.arrayToCommaDelimitedString(namespace));
}

private static void setSystemProperty(Properties systemProperties, String key, String value) {
    if (!StringUtils.hasText(value)) { return; }
    if (systemProperties.contains(key)) { return; }  // ← 不覆盖已有的 System property
    systemProperties.put(key, value);
}
```

写入的 System properties：

| Key | 来源 | 用途 |
|-----|------|------|
| `app.id` | `@ApolloPropertySource.appId()` | Apollo 使用 `System.getProperty("app.id")` 确定应用 ID |
| `apollo.meta` | `@ApolloPropertySource.meta()` | Apollo 使用 `System.getProperty("apollo.meta")` 确定 Meta Server |
| `apollo.cluster` | `@ApolloPropertySource.cluster()` | Apollo 使用 `System.getProperty("apollo.cluster")` 确定集群 |
| `apollo.bootstrap.namespaces` | `@ApolloPropertySource.namespace()` | Apollo 使用此参数确定拉取哪些命名空间 |

### postProcessBeanFactory()——注册监听器

这是 Apollo 模块的核心。`BeanFactoryPostProcessor` 的执行时机在 Apollo 原生 `PropertySourcesProcessor` 之后——此时 Apollo 已经将配置加载到 Environment 中，`CompositePropertySource`（名为 `ApolloPropertySource`）可用。

```java
@Override
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    MutablePropertySources propertySources = environment.getPropertySources();
    PropertySource propertySource = propertySources.get(APOLLO_PROPERTY_SOURCE_NAME);

    if (!(propertySource instanceof CompositePropertySource)) {
        logger.warn("No Apollo PropertySource was found by name : {}", APOLLO_PROPERTY_SOURCE_NAME);
        return;
    }

    CompositePropertySource compositePropertySource = (CompositePropertySource) propertySource;

    boolean autoRefreshed = attributes.getBoolean("autoRefreshed");

    if (autoRefreshed) {
        Collection<PropertySource<?>> subPropertySources = compositePropertySource.getPropertySources();
        oldPropertySourcesMap = new ConcurrentHashMap<>(subPropertySources.size());

        for (PropertySource<?> subPropertySource : subPropertySources) {
            if (subPropertySource instanceof ConfigPropertySource) {
                ConfigPropertySource configPropertySource = (ConfigPropertySource) subPropertySource;
                Config config = configPropertySource.getSource();
                String configPropertySourceName = configPropertySource.getName();

                oldPropertySourcesMap.computeIfAbsent(configPropertySourceName, name -> {
                    config.addChangeListener(event -> onChanged(name, configPropertySource, event));
                    return clonePropertySource(name, configPropertySource);
                });
            }
        }
    }
}
```

处理流程：

```
1. 从 Environment 获取名为 "ApolloPropertySource" 的 CompositePropertySource
   → 由 Apollo 原生的 PropertySourcesProcessor 在更早的阶段注册
2. 遍历 CompositePropertySource 中的子 PropertySource
3. 对每个 ConfigPropertySource：
   a. 调用 configPropertySource.getSource() 获取 Apollo 的 Config 对象
   b. 保存当前配置的快照（clonePropertySource 用于后续 diff）
   c. 注册 ConfigChangeListener
```

### onChanged()——配置变更处理

```java
private void onChanged(String configPropertySourceName, ConfigPropertySource configPropertySource,
        ConfigChangeEvent configChangeEvent) {

    PropertySource oldPropertySource = oldPropertySourcesMap.get(configPropertySourceName);

    Map<String, Object> addedProperties = new HashMap<>();
    Map<String, Object> modifiedProperties = new HashMap<>();
    Map<String, Object> deletedProperties = new HashMap<>();

    for (String key : configChangeEvent.changedKeys()) {
        ConfigChange configChange = configChangeEvent.getChange(key);
        switch (configChange.getChangeType()) {
            case ADDED:    addedProperties.put(key, configChange.getNewValue()); break;
            case MODIFIED: modifiedProperties.put(key, configChange.getNewValue()); break;
            case DELETED:  deletedProperties.put(key, configChange.getNewValue()); break;
        }
    }

    MapPropertySource addedPS = new MapPropertySource(name + "#added", addedProperties);
    MapPropertySource modifiedPS = new MapPropertySource(name + "#modified", modifiedProperties);
    MapPropertySource deletedPS = new MapPropertySource(name + "#deleted", deletedProperties);

    PropertySourceChangedEvent addEvent = added(context, addedPS);
    PropertySourceChangedEvent modifiedEvent = replaced(context, oldPropertySource, modifiedPS);
    PropertySourceChangedEvent deletedEvent = removed(context, deletedPS);

    context.publishEvent(new PropertySourcesChangedEvent(context, addEvent, modifiedEvent, deletedEvent));

    // 更新快照
    oldPropertySource = clonePropertySource(name, configPropertySource);
    oldPropertySourcesMap.put(name, oldPropertySource);
}
```

### clonePropertySource()——快照

```java
private PropertySource clonePropertySource(String name, ConfigPropertySource configPropertySource) {
    String[] propertyNames = configPropertySource.getPropertyNames();
    Map<String, Object> properties = new HashMap<>(propertyNames.length);
    for (String propertyName : propertyNames) {
        Object propertyValue = configPropertySource.getProperty(propertyName);
        properties.put(propertyName, propertyValue);
    }
    return new MapPropertySource(name, properties);
}
```

每次变更后将当前所有属性复制到一个新的 `MapPropertySource` 中，作为下次 diff 的"旧值"。

---

## 与 Nacos 模块的对比

| 维度 | Nacos 模块 | Apollo 模块 |
|------|-----------|-------------|
| 注解触发 | `@PropertySourceExtension` 元注解体系 | `@EnableApolloConfig` 原生注解 |
| 配置加载 | 自实现：Loader 拉取 REST API | 复用：Apollo 原生 `PropertySourcesProcessor` |
| 监听注册 | `ConfigClient.addEventListener()` | `Config.addChangeListener()` |
| 事件发布 | `PropertySourceExtensionLoader.refresher.refresh()` | Registrar 直接 `context.publishEvent()` |
| 客户端 | 自实现 OpenApiNacosClient | 复用 apollo-client |
| System properties | 无 | 写入 app.id / apollo.meta / apollo.cluster / 命名空间 |
| diff 机制 | Loader 父类的 refresher 通用 diff | Registrar 手动 clonePropertySource + 分类 diff |

---

## 完整工作流程

```
编译期：
  @ApolloPropertySource(appId = "my-app", meta = "http://apollo.meta:8080", namespace = "application")
    └─ @EnableApolloConfig（Apollo 原生）
    └─ @Import(ApolloPropertySourceBeanDefinitionRegistrar.class)

启动时（第 1 阶段：ImportBeanDefinitionRegistrar）：
  Spring 处理 @Import(ApolloPropertySourceBeanDefinitionRegistrar.class)
    → registerBeanDefinitions()
      → 读取 @ApolloPropertySource 属性
      → setSystemPropertiesFromAttributes()
        → System.setProperty("app.id", "my-app")
        → System.setProperty("apollo.meta", "http://apollo.meta:8080")
        → System.setProperty("apollo.cluster", "default")
        → System.setProperty("apollo.bootstrap.namespaces", "application")

启动时（第 2 阶段：Apollo PropertySourcesProcessor）：
  @EnableApolloConfig 触发 Apollo 原生处理
    → PropertySourcesProcessor.postProcessBeanFactory()
      → 读取 System properties（app.id、apollo.meta 等）
      → 连接 Apollo Meta Server
      → 拉取 "application" 命名空间的配置
      → 创建 ConfigPropertySource
      → 注册 CompositePropertySource（名 "ApolloPropertySource"）到 Environment

启动时（第 3 阶段：Registrar 的 BeanFactoryPostProcessor）：
  ApolloPropertySourceBeanDefinitionRegistrar.postProcessBeanFactory()
    → 从 Environment 获取 "ApolloPropertySource"
    → 遍历子 PropertySource
    → 对每个 ConfigPropertySource：
      → clonePropertySource() 保存快照
      → config.addChangeListener() 注册监听

运行时（配置变更）：
  Apollo 服务端配置变更
    → ConfigChangeListener.onChanged()
      → 遍历 changedKeys()
      → 分类 ADDED / MODIFIED / DELETED
      → 创建三个 MapPropertySource
      → PropertySourceChangedEvent（ADDED / REPLACED / REMOVED）
      → PropertySourcesChangedEvent 发布
      → 更新快照
```

---

## 工程问题分析

### 执行顺序依赖

`ApolloPropertySourceBeanDefinitionRegistrar` 同时实现了 `ImportBeanDefinitionRegistrar` 和 `BeanFactoryPostProcessor`。`registerBeanDefinitions()` 在配置类解析时执行，`postProcessBeanFactory()` 在 BeanFactory 准备后执行。Apollo 的 `PropertySourcesProcessor` 也是一个 `BeanFactoryPostProcessor`——它必须在 `ApolloPropertySourceBeanDefinitionRegistrar` 的 `postProcessBeanFactory()` **之前**执行，否则 "ApolloPropertySource" 还不存在。

这个执行顺序能保证不是因为 order 值，而是因为 `PropertySourcesProcessor` 实现了 `BeanDefinitionRegistryPostProcessor`（`PriorityOrdered`），而 `ApolloPropertySourceBeanDefinitionRegistrar` 只实现了 `BeanFactoryPostProcessor`。Spring 在执行顺序上保证所有 `BeanDefinitionRegistryPostProcessor` 先于所有 `BeanFactoryPostProcessor` 执行——所以 `PropertySourcesProcessor` 一定先执行。

### 快照更新时机

`clonePropertySource()` 在变更事件处理完成后**立即**更新快照。这意味着如果两次变更非常接近，第二次变更的 diff 使用的是第一次变更后的快照——不会丢失 diff。但如果第一次变更处理过程中又来了第二次变更（并发），`ConcurrentHashMap` 保证快照更新的可见性。

### 属性分类的精度问题

Apollo 的 `ConfigChangeEvent.changedKeys()` 返回所有变更的 key。Registrar 将它们分为 ADDED / MODIFIED / DELETED。但在发布 `PropertySourcesChangedEvent` 时，`MODIFIED` 事件使用了 `replaced(context, oldPropertySource, modifiedPropertySource)`，这里的 `oldPropertySource` 是**前一次快照的完整 PropertySource**，不是某个具体 key 的旧值。这可能导致事件消费者无法定位到具体的旧值。

### `setSystemProperty()` 的 `contains()` vs `containsKey()` 问题

```java
private static void setSystemProperty(Properties systemProperties, String key, String value) {
    if (!StringUtils.hasText(value)) { return; }
    if (systemProperties.contains(key)) { return; }  // ← 应该用 containsKey()
    systemProperties.put(key, value);
}
```

`Properties` 继承 `Hashtable<Object, Object>`。`Hashtable.contains(Object value)` 检查的是**值**是否存在，不是**键**。这行代码的意图是"如果 System property 已存在就不覆盖"，但 `contains(key)` 实际上检查的是"是否有某个属性的值等于 key 这个字符串"——几乎永远返回 false，所以"不覆盖"的防御无效。应该使用 `containsKey(key)`。

### 与 `@PropertySourceExtension` 体系的关系

Apollo 模块是唯一不通过 `@PropertySourceExtension` 的后端。原因是 Apollo 的配置加载机制与 Nacos/etcd/ZK 完全不同——Apollo 有自己完整的 `PropertySourcesProcessor`，15 不需要重新实现加载逻辑。代价是 Apollo 模块无法使用 `first`/`before`/`after` 排序控制（这些由 `@PropertySourceExtension` 提供，Apollo 不走这个注解）。
