# 18-10 事件驱动源码解析

## 目录

- [事件驱动架构概览](#事件驱动架构概览)
- [事件定义与类型](#事件定义与类型)
- [ZoneContextChangedEvent → DynamicJdbcConfigChangedEvent](#zonecontextchangedevent--dynamicjdbcconfigchangedevent)
- [PropertySourcesChangedEvent → DynamicJdbcConfigChangedEvent](#propertysourceschangedevent--dynamicjdbcconfigchangedevent)
- [DynamicJdbcConfigChangedEvent → DynamicDataSource 重建](#dynamicjdbcconfigchangedevent--dynamicdatasource-重建)
- [ContextRefreshedEvent → Bean 升迁](#contextrefreshedevent--bean-升迁)
- [ContextClosedEvent → 父子上下文关闭](#contextclosedevent--父子上下文关闭)
- [ApplicationStartedEvent → ShardingSphere Shutdown Hook](#applicationstartedevent--shardingsphere-shutdown-hook)
- [ApplicationPreparedEvent → 处理入口](#applicationpreparedevent--处理入口)
- [事件时间线总图](#事件时间线总图)

---

## 事件驱动架构概览

18-dynamic 大量使用 Spring 的事件机制。完整的 Listener 列表：

| Listener | 监听的事件 | 何时注册 | 干什么 |
|----------|-----------|---------|--------|
| `DynamicJdbcContextApplicationListener` | `ApplicationPreparedEvent` | spring.factories | 创建子上下文 |
| `DynamicJdbcAutoConfigurationImportListener` | `AutoConfigurationImportEvent` | spring.factories | 缓存过滤结果 |
| `PropagatingDynamicJdbcConfigChangedEventListener` | `PropertySourcesChangedEvent` + `ZoneContextChangedEvent` | 程序化注册 | 传播配置/Zone 变更 |
| `DynamicJdbcChildContextRefreshedListener` | `ContextRefreshedEvent` | 子上下文 postProcessBeanFactory | Bean 升迁 |
| `RefreshingDynamicDataSourceListener` | `DynamicJdbcConfigChangedEvent` | DynamicDataSource.afterPropertiesSet | 热替换 DataSource |
| `SyncExecutionShutdownHookApplicationListener` | `ApplicationStartedEvent` + `ContextClosedEvent` | 程序化注册 | ShardingSphere hook |
| `DynamicJdbcChildContextRefreshedListener` 在父上下文注册的子上下文关闭 Listener | `ContextClosedEvent` | 子上下文刷新时（由 refreshed listener 注册） | 子上下文关闭传播 |

### 事件种类

| 事件 | 发布者 | 触发条件 |
|------|-------|---------|
| `ApplicationPreparedEvent` | `SpringApplication` | prepareContext 阶段（refresh 之前），上下文已准备未 refresh |
| `ApplicationStartedEvent` | `SpringApplication` | Spring 上下文已启动 |
| `AutoConfigurationImportEvent` | `AutoConfigurationImportSelector` | Auto-Configuration 过滤完成 |
| `ContextRefreshedEvent` | `AbstractApplicationContext` | BeanFactory 初始化完成 |
| `ContextClosedEvent` | `AbstractApplicationContext` | 上下文关闭 |
| `PropertySourcesChangedEvent` | 外部（17-configuration） | 配置属性变更 |
| `ZoneContextChangedEvent` | 外部（17-multiactive） | Zone 切换 |
| `DynamicJdbcConfigChangedEvent` | `PropagatingDynamicJdbcConfigChangedEventListener` | 配置或 Zone 变更 |

---

## 事件定义与类型

### DynamicJdbcConfigChangedEvent

```java
public class DynamicJdbcConfigChangedEvent extends ApplicationEvent {

    private final DynamicJdbcConfig dynamicJdbcConfig;
    private final String propertyName;

    public DynamicJdbcConfigChangedEvent(
            ConfigurableApplicationContext source,
            DynamicJdbcConfig dynamicJdbcConfig,
            String propertyName) {
        super(source);
        this.dynamicJdbcConfig = dynamicJdbcConfig;
        this.propertyName = propertyName;
    }

    public DynamicJdbcConfig getDynamicJdbcConfig() {
        return dynamicJdbcConfig;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public ConfigurableApplicationContext getSource() {
        return (ConfigurableApplicationContext) super.getSource();
    }
}
```

事件源是发布事件的 `ConfigurableApplicationContext` 实例，payload 是变更的 config 和 propertyName。

---

## ZoneContextChangedEvent → DynamicJdbcConfigChangedEvent

### PropagatingDynamicJdbcConfigChangedEventListener

```java
class PropagatingDynamicJdbcConfigChangedEventListener
        implements SmartApplicationListener {

    private final Set<String> dynamicJdbcConfigPropertyNames;
    private final ConfigurableApplicationContext context;
    private final ConfigurableEnvironment environment;

    public PropagatingDynamicJdbcConfigChangedEventListener(
            Set<String> configPropertyNames,
            ConfigurableApplicationContext context) {
        this.dynamicJdbcConfigPropertyNames = configPropertyNames;
        this.context = context;
        this.environment = context.getEnvironment();
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        // 支持两种事件
        return PropertySourcesChangedEvent.class.equals(eventType)
            || ZoneContextChangedEvent.class.equals(eventType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PropertySourcesChangedEvent) {
            onPropertySourcesChangedEvent((PropertySourcesChangedEvent) event);
        } else if (event instanceof ZoneContextChangedEvent) {
            onZoneContextChangedEvent((ZoneContextChangedEvent) event);
        }
    }
}
```

### Zone 变更处理

```java
private void onZoneContextChangedEvent(ZoneContextChangedEvent event) {
    if (isZoneChanged(event)) {
        // 对每个 config，如果它有 HA 数据源，重新读取配置并发布变更事件
        dynamicJdbcConfigPropertyNames.forEach(propertyName -> {
            DynamicJdbcConfig config = getDynamicJdbcConfig(environment, propertyName);
            if (config.hasHighAvailabilityDataSource()) {
                publishDynamicJdbcConfigChangedEvent(config, propertyName);
            }
        });
    }
}

private boolean isZoneChanged(ZoneContextChangedEvent event) {
    List<PropertyChangeEvent> changes = event.getPropertyChangeEvents();
    for (PropertyChangeEvent change : changes) {
        if ("zone".equals(change.getPropertyName())) {
            return true;
        }
    }
    return false;
}
```

为什么只处理 HA DataSource？因为非 HA 数据源的配置不依赖于 Zone，Zone 变化不需要重建。HA 数据源不同 zone 有不同的数据库配置，Zone 切换时必须重新读取新的配置并重建 DataSource。

### 事件发布

```java
private void publishDynamicJdbcConfigChangedEvent(
        DynamicJdbcConfig config, String propertyName) {
    context.publishEvent(
        new DynamicJdbcConfigChangedEvent(context, config, propertyName));
}
```

注意 `context` 是父上下文（`DynamicJdbcContextApplicationListener` 中注册时传入的根上下文/父上下文）。事件在父上下文中发布，这样才能被父上下文中的 Listener 收到。

### SmartApplicationListener 的 supportsEventType

`PropagatingDynamicJdbcConfigChangedEventListener` 实现了 `SmartApplicationListener` 而不是 `ApplicationListener`。为什么？

`SmartApplicationListener` 可以指定 `supportsEventType` 和 `supportsSourceType`，允许 Listener 声明自己只处理特定类型的事件。Spring 的事件广播器在遍历 Listener 时，会先调用 `supportsEventType`，如果不支持则跳过，避免了类型转换异常和空判断。

---

## PropertySourcesChangedEvent → DynamicJdbcConfigChangedEvent

### 配置变更处理

```java
private void onPropertySourcesChangedEvent(PropertySourcesChangedEvent event) {
    // 获取所有变更的属性 key
    Set<String> keys = event.getChangedProperties().keySet();

    for (String key : keys) {
        // 检查变更的 key 是否是 DynamicJdbcConfig 的 property name
        if (dynamicJdbcConfigPropertyNames.contains(key)) {
            // 重新读取 JSON 配置并发布事件
            publishDynamicJdbcConfigChangedEvent(key);
        }
    }
}

private void publishDynamicJdbcConfigChangedEvent(String propertyName) {
    DynamicJdbcConfig config = getDynamicJdbcConfig(environment, propertyName);
    publishDynamicJdbcConfigChangedEvent(config, propertyName);
}
```

`PropertySourcesChangedEvent` 来自 `microsphere-spring-boot` 的 `microsphere-configuration` 模块。当配置中心的属性发生变化时（如 Nacos/Apollo 配置热更新），会发布这个事件。18-dynamic 监听这个事件，当 `microsphere.dynamic.jdbc.configs.*` 下的配置变更时，重新读取 JSON → 重建 DynamicJdbcConfig → 发布 DynamicJdbcConfigChangedEvent → DynamicDataSource 重建。

### getDynamicJdbcConfig 的重新读取

```java
// onPropertySourcesChangedEvent 中调用 getDynamicJdbcConfig
// 这个调用读取的是 Environment 中最新的值（PropertySources 已被更新）
DynamicJdbcConfig config = getDynamicJdbcConfig(environment, propertyName);

// 注意：这里强制 dynamic = true
// 之前在 Process 第 4 步中移除了 DataSource 配置
// 所以重新读取后的 config 包含原始的 dataSource 配置（JSON 中的原始值）
```

---

## DynamicJdbcConfigChangedEvent → DynamicDataSource 重建

### RefreshingDynamicDataSourceListener

```java
private class RefreshingDynamicDataSourceListener
        implements ApplicationListener<DynamicJdbcConfigChangedEvent> {

    @Override
    public void onApplicationEvent(DynamicJdbcConfigChangedEvent event) {
        DynamicJdbcConfig config = event.getDynamicJdbcConfig();
        String propertyName = event.getPropertyName();
        ConfigurableApplicationContext sourceContext = event.getSource();

        // 1. 检查 propertyName 是否匹配
        if (!Objects.equals(DynamicDataSource.this.dynamicJdbcConfigPropertyName, propertyName)) {
            return;
        }

        // 2. 在正确的父上下文中重建
        ConfigurableApplicationContext parentContext = findParentContext(sourceContext);

        if (parentContext != null) {
            // 3. 重建
            initializeDataSource(config, propertyName, parentContext);
        }
    }
}
```

### findParentContext 的三种路径

```java
private ConfigurableApplicationContext findParentContext(
        ConfigurableApplicationContext eventSourceContext) {

    DynamicJdbcChildContext currentChildContext = this.dynamicDataSourceChildContext;
    ConfigurableApplicationContext parent = currentChildContext.getParentContext();

    // 情况 A: 事件源直接是父上下文
    if (Objects.equals(parent, eventSourceContext)) {
        return parent;
    }

    // 情况 B: 父上下文不是子上下文（比如是根上下文）
    if (!(parent instanceof DynamicJdbcChildContext)) {
        return null;
    }

    // 情况 C: 父上下文的父上下文是事件源（多层嵌套）
    DynamicJdbcChildContext parentAsChild = (DynamicJdbcChildContext) parent;
    if (Objects.equals(parentAsChild.getParentContext(), eventSourceContext)) {
        return parent;
    }

    return null;
}
```

三种情况对应的场景：

```
情况 A:
  RootContext (事件源)
    └─ DynamicDataSource.childContext (parent = RootContext)

情况 B:
  不支持。parent 不是 DynamicJdbcChildContext → 说明是根上下文 → 由情况 A 处理

情况 C:
  RootContext (事件源)
    └─ DynamicJdbcChildContext[configA] (parent = RootContext)
         └─ DynamicDataSource.childContext (parent = DynamicJdbcChildContext[configA])
  
  事件源 = RootContext
  currentChildContext.parent = DynamicJdbcChildContext[configA]
  DynamicJdbcChildContext[configA].parent = RootContext = 事件源 → 匹配
```

### initializeDataSource 的重新执行

```java
private DataSource initializeDataSource(DynamicJdbcConfig config,
        String propertyName, ConfigurableApplicationContext context) {

    // 与初始化的流程完全相同
    DynamicJdbcConfig dsConfig = createDynamicDataSourceConfig(config);
    DynamicJdbcChildContext newCtx = new DynamicJdbcChildContext(
            dsConfig, propertyName, context, idGenerator);
    newCtx.mergeParentEnvironment();
    newCtx.refresh();

    DataSource newDS = getDataSource(newCtx);

    synchronized (mutex) {
        DataSource oldDS = DynamicDataSource.this.delegate;
        DynamicDataSource.this.delegate = newDS;

        ConfigurableApplicationContext oldCtx =
                DynamicDataSource.this.dynamicDataSourceChildContext;
        DynamicDataSource.this.dynamicDataSourceChildContext = newCtx;

        closeDynamicDataSourceChildContext(oldCtx, true);  // 延迟 60s 关闭
    }

    return newDS;
}
```

注：`createDynamicDataSourceConfig` 再次读取 `config.getDataSourcePropertiesList()`，此时 ZoneContext 可能已变化，会返回新 Zone 的数据源配置。

---

## ContextRefreshedEvent → Bean 升迁

### DynamicJdbcChildContextRefreshedListener

```java
class DynamicJdbcChildContextRefreshedListener
        implements ApplicationListener<ContextRefreshedEvent> {

    // ...（字段和构造在 18-04 中已分析）

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ConfigurableApplicationContext childContext =
                (ConfigurableApplicationContext) event.getApplicationContext();

        // 1. 注册父上下文关闭事件 → 自动关闭本子上下文
        registerParentContextClosedEventListener(childContext);

        // 2. 将非基础设施 Bean 升迁到父上下文
        registerParentBeansFromChildContext(childContext);
    }
}
```

### 触发时机

子上下文的 `refresh()` 过程中，第 11 步 `finishRefresh()` 发布 `ContextRefreshedEvent`：

```java
// AbstractApplicationContext.finishRefresh()
protected void finishRefresh() {
    // 初始化 LifecycleProcessor
    initLifecycleProcessor();
    // 发布 ContextRefreshedEvent
    publishEvent(new ContextRefreshedEvent(this));
    // 注册 LiveBeansView（JMX）
    LiveBeansView.registerApplicationContext(this);
}
```

注意时序：Bean 升迁发生在**子上下文 refresh 完成时**，而不是"子上下文中的所有 Bean 初始化之后"。这意味着如果在升迁过程中有异常，子上下文的 refresh 会失败，父上下文也不会被污染。

### 子上下文关闭注册

```java
private void registerParentContextClosedEventListener(
        ConfigurableApplicationContext childContext) {
    parentContext.addApplicationListener(
        (ApplicationListener<ContextClosedEvent>) event -> {
            childContext.close();
        });
}
```

在父上下文中注册了一个 `ContextClosedEvent` 的 Lambda 监听器。当父上下文关闭时，这个监听器会被调用，依次关闭子上下文。

注意：如果父上下文关闭时，子上下文已经被 DynamicDataSource 的延迟关闭逻辑关掉了，再次调用 `close()` 是安全的（Spring 的 `AbstractApplicationContext.close()` 有 `synchronized(startupShutdownMonitor)` 保护，防止重复关闭）。

---

## ContextClosedEvent → 父子上下文关闭

### 父上下文关闭时的关闭顺序

```
父上下文 doClose()
  │
  ├─ 1. publishEvent(new ContextClosedEvent(this))
  │     │
  │     ├─ DynamicJdbcChildContextRefreshedListener 注册的 Lambda:
  │     │   → childContext.close()
  │     │     → 子上下文的 doClose()
  │     │       → publishEvent(new ContextClosedEvent(...)) // 子上下文关闭事件
  │     │       → destroyBeans()
  │     │         → DynamicDataSource.destroy()
  │     │           → closeDynamicDataSourceChildContext(current, false)  // 立即关闭
  │     │           → shutdownScheduler(closeScheduler)
  │     │       → closeBeanFactory()
  │     │
  │     ├─ 其他父上下文 Listener...
  │     │
  ├─ 2. destroyBeans()
  │     → 父上下文中的 Bean 销毁
  │     → 包括从子上下文升迁过来的 Bean
  │
  ├─ 3. closeBeanFactory()
  │
  └─ 4. onClose()
```

关键点：**子上下文先于父上下文 Bean 销毁之前关闭**。这是正确的顺序——如果父上下文 Bean 先销毁，子上下文在关闭时可能引用到已销毁的父上下文 Bean。

### DynamicDataSource.destroy

```java
@Override
public void destroy() {
    // 立即关闭当前 DataSource 的子上下文
    closeDynamicDataSourceChildContext(dynamicDataSourceChildContext, false);
    // 关闭调度器（取消所有等待中的延迟关闭任务）
    shutdownScheduler(closeScheduler);
}
```

注意这里 `async=false`，因为是关闭时，不需要延迟，立即关闭。

---

## ApplicationStartedEvent → ShardingSphere Shutdown Hook

### SyncExecutionShutdownHookApplicationListener

```java
public class SyncExecutionShutdownHookApplicationListener
        implements ApplicationListener<ApplicationStartedEvent> {

    private final ConfigurableApplicationContext context;
    private final Predicate<Thread> shutdownHookThreadFilter;

    public SyncExecutionShutdownHookApplicationListener(
            ConfigurableApplicationContext context,
            Predicate<Thread> shutdownHookThreadFilter) {
        this.context = context;
        this.shutdownHookThreadFilter = shutdownHookThreadFilter;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ConfigurableApplicationContext eventContext = event.getApplicationContext();
        if (eventContext != context) {
            return;  // 确保只处理对应该 Listener 的上下文
        }

        // 找到匹配的 shutdown hook 线程
        Set<Thread> hooks = findSyncShutdownHookThreads();

        // 注册 ContextClosedEvent 监听器，同步执行 hook
        registerSyncExecutionListener(context, hooks);
    }
}
```

### 为什么需要这个 Listener

ShardingSphere 5.x 在初始化 `ShardingSphereDataSource` 时，会向 JVM 注册 shutdown hook：

```java
// ShardingSphereDataSourceFactory.create()
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // 关闭连接池、释放资源
    closeDataSources();
}));
```

在正常 Spring Boot 应用中，这个 hook 会在 JVM 关闭时执行。但在 18-dynamic 的子上下文中：
1. 子上下文销毁时，JVM 不会关闭（只是 Spring 上下文关闭），所以 shutdown hook 不会执行
2. 如果 hook 不执行，ShardingSphere 的连接池不会被关闭，导致连接泄漏

解决方案：在 `ApplicationStartedEvent` 时找到这个 hook 线程，在 `ContextClosedEvent` 时用 `Thread.run()` 同步执行。

### registerSyncExecutionListener

```java
private void registerSyncExecutionListener(
        ConfigurableApplicationContext context,
        Set<Thread> syncShutdownHookThreads) {

    if (isEmpty(syncShutdownHookThreads)) {
        return;
    }

    context.addApplicationListener(
        (ApplicationListener<ContextClosedEvent>) event -> {
            // 同步执行：用 Thread.run() 而不是 Thread.start()
            // 确保在 Spring 关闭线程中执行，而不是在 JVM shutdown 线程中
            syncShutdownHookThreads.forEach(Thread::run);
        });
}
```

`Thread.run()` 与 `Thread.start()` 的区别：
- `Thread.start()` — 在新的线程中异步执行
- `Thread.run()` — 在当前线程中同步执行

使用 `Thread.run()` 确保 ShardingSphere 的资源释放逻辑在 Spring 的关闭线程中同步执行，而不是在新线程中异步执行——这样能保证关闭顺序可控。

### ShardingSphereShutdownHookThreadFilter

```java
public class ShardingSphereShutdownHookThreadFilter implements Predicate<Thread> {

    @Override
    public boolean test(Thread thread) {
        // ShardingSphere 注册的 shutdown hook 线程名称以 "DelayedShutdownHook-for-" 开头
        return thread.getName().startsWith("DelayedShutdownHook-for-");
    }
}
```

---

## ApplicationPreparedEvent → 处理入口

### 完整时序

```
SpringApplication.run()
  │
  ├─ 步骤 1: 准备 Environment
  │     └─ ApplicationEnvironmentPreparedEvent → Environment 就绪
  │
  ├─ 步骤 2: 创建 ApplicationContext
  │     └─ new AnnotationConfigApplicationContext()
  │
  ├─ 步骤 3: 准备 Context
  │     └─ 设置 Environment、BeanFactoryPostProcessor、ApplicationListener
  │
  ├─ 步骤 4: prepareContext
  │     └─ ApplicationPreparedEvent 发布 ← 这是 18-dynamic 的入口！
  │         ← DynamicJdbcContextApplicationListener
  │            ├─ 读取配置
  │            ├─ 注册 PropagatingDynamicJdbcConfigChangedEventListener
  │            └─ 单 config：执行 6 步管道，注册 DynamicDataSource BeanDefinition
  │               多 config：线程池并行创建业务子上下文（子上下文 refresh 内执行 6 步管道）
  │
  ├─ 步骤 5: refreshContext
  │     ├─ invokeBeanFactoryPostProcessors（Auto-Configuration 扫描）
  │     ├─ finishBeanFactoryInitialization
  │     │   ← 单 config：DynamicDataSource 实例化 → afterPropertiesSet() → 创建子上下文
  │     │   ← 多 config：子上下文 refresh 中 DynamicDataSource 初始化
  │     └─ finishRefresh
  │         ├─ ContextRefreshedEvent（父上下文）
  │         └─ ApplicationStartedEvent（父上下文）
  │
  └─ 步骤 6: afterRefresh
```

为什么在 `ApplicationPreparedEvent` 而不是 `ContextRefreshedEvent`？因为 `ApplicationPreparedEvent` 在 `prepareContext` 阶段（refresh 之前）发布——此时注册 DynamicDataSource BeanDefinition，会在本次 refresh 的 `finishBeanFactoryInitialization` 中被实例化。如果等到 `ContextRefreshedEvent`（refresh 之后），BeanDefinition 注册时机太晚。

**两种模式的子上下文都在启动时创建**：单 config 在父上下文 refresh 中（afterPropertiesSet → initializeDataSource），多 config 在子上下文 refresh 中。真正延迟到首次请求的只有 HikariCP 连接池建连。

---

## 事件时间线总图

```
启动阶段：

SpringApplication.run() 开始
  │
  │ ApplicationEnvironmentPreparedEvent
  │   ← DynamicJdbcDefaultPropertiesPostProcessor 加载 default.properties
  │
  ├─ prepareContext
  │   └─ ApplicationPreparedEvent（refresh 之前）
  │       ← DynamicJdbcContextApplicationListener
  │          ├─ 读取配置
  │          ├─ 注册 PropagatingDynamicJdbcConfigChangedEventListener
  │          │   (监听 ZoneContextChangedEvent + PropertySourcesChangedEvent)
  │          ├─ 注册 SyncExecutionShutdownHookApplicationListener
  │          │   (如果有 ShardingSphere)
  │          └─ 单 config：执行 6 步管道，注册 DynamicDataSource BeanDefinition
  │             多 config：创建 DynamicJdbcChildContext
  │              │
  │              └─ childContext.refresh()
  │                  ├─ postProcessBeanFactory
  │                  │   ← 注册 DynamicJdbcChildContextRefreshedListener
  │                  │   ← 执行 DynamicJdbcContextProcessor 6 步
  │                  │
  │                  ├─ finishBeanFactoryInitialization
  │                  │   └─ DynamicDataSource.afterPropertiesSet()
  │                  │       └─ initializeDataSource() 创建嵌套子上下文
  │                  │
  │                  └─ finishRefresh
  │                      │ ContextRefreshedEvent (子上下文)
  │                      │ ← DynamicJdbcChildContextRefreshedListener
  │                      │    ├─ 注册父上下文关闭的子上下文关闭监听器
  │                      │    └─ Bean 升迁到父上下文
  │
  └─ refresh() 开始
      │
      ├─ invokeBeanFactoryPostProcessors
      │   └─ ConfigurationClassPostProcessor
      │       └─ AutoConfigurationImportSelector.selectImports()
      │           │
      │           │ AutoConfigurationImportEvent (首次，缓存未命中)
      │           │ ← DynamicJdbcAutoConfigurationImportListener 缓存
      │
      ├─ finishBeanFactoryInitialization
      │   └─ 单 config：DynamicDataSource 实例化
      │       → afterPropertiesSet() → initializeDataSource() → 创建子上下文
      │
      └─ finishRefresh
          │
          │ ContextRefreshedEvent (父上下文)
          │
          └─ ApplicationStartedEvent
              ← SyncExecutionShutdownHookApplicationListener
                 → 注册 ContextClosedEvent 的 ShardingHook 执行器

运行阶段：

Zone 切换:
  ZoneContext.setZone("zone-b")
    → ZoneContextChangedEvent("zone" changed from "zone-a" to "zone-b")
      ← PropagatingDynamicJdbcConfigChangedEventListener
        → DynamicJdbcConfigChangedEvent
          ← RefreshingDynamicDataSourceListener
            → initializeDataSource() (用新 Zone 的配置重建)
              → 创建新的 DynamicJdbcChildContext
              → 原子替换 delegate
              → 延迟 60s 关闭旧上下文

配置热更新:
  Environment 属性变更
    → PropertySourcesChangedEvent(key="microsphere.dynamic.jdbc.configs.xxx")
      ← PropagatingDynamicJdbcConfigChangedEventListener
        → 重新读取配置
        → DynamicJdbcConfigChangedEvent
          ← RefreshingDynamicDataSourceListener
            → initializeDataSource() (用新配置重建)

关闭阶段:

父上下文关闭:
  ContextClosedEvent
    ← DynamicJdbcChildContextRefreshedListener 注册的 Lambda
      → childContext.close()
        ContextClosedEvent (子上下文)
          ← DynamicDataSource.destroy()
            → closeDynamicDataSourceChildContext(current, false)
            → shutdownScheduler(closeScheduler)
        ← 子上下文的 destroyBeans()
    ← SyncExecutionShutdownHookApplicationListener 注册的 Lambda
      → ShardingSphere shutdown hook 同步执行 (Thread.run())
    ← 其他父上下文 Listener
  destroyBeans()
    → 父上下文中的 Bean 销毁
  closeBeanFactory()
```

这个事件时间线展示了 18-dynamic 完整运行过程中，从启动到运行到关闭的全部事件流。
