# 15-06 与 18-dynamic 的集成链路

## 目录

- [集成的本质](#集成的本质)
- [事件链全景](#事件链全景)
- [三个角色](#三个角色)
- [PropagatingDynamicJdbcConfigChangedEventListener 源码解析](#propagatingdynamicjdbcconfigchangedeventlistener-源码解析)
- [配置热更新的完整流程](#配置热更新的完整流程)
- [常见问题](#常见问题)

---

## 集成的本质

15-configuration 和 18-dynamic 的关系很简单：

```
15-configuration（配置中心抽象层）         18-dynamic（数据库动态层）
  配置变更 → 发布 PropertySourcesChangedEvent → 监听事件 → DynamicJdbcConfig 热更新
```

15 是**发布者**，18 是**消费者**。两者之间没有直接依赖——通过 Spring 事件解耦。

### 依赖方向

```
18-dynamic
  └── 依赖 microsphere-spring-context（事件类型定义）
        └── 不依赖 microsphere-configuration（15 的模块）
```

18-dynamic 只依赖事件类型的定义（`PropertySourcesChangedEvent`），不依赖 15 的任何具体后端模块。这意味着：

- 使用 Nacos 时：引入 `microsphere-configuration-nacos-spring`
- 使用 Apollo 时：引入 `microsphere-configuration-apollo-spring`
- 不引入任何 15 模块时：18-dynamic 正常工作（监听器注册了但没人发事件）

---

## 事件链全景

```
配置中心（Nacos / Apollo / etcd / ZooKeeper）
  │
  │ 配置变更
  ▼
15-configuration（发布者）
  │
  │ 发布 PropertySourcesChangedEvent
  ▼
Spring ApplicationContext（事件总线）
  │
  ├──► 18-dynamic: PropagatingDynamicJdbcConfigChangedEventListener
  │       │
  │       │ 检查变更的 key 是否匹配 DynamicJdbcConfig 配置
  │       ▼
  │     DynamicJdbcConfigChangedEvent
  │       │
  │       ▼
  │     DynamicDataSource.RefreshingDynamicDataSourceListener
  │       │
  │       ▼
  │     initializeDataSource() → 重建子上下文 → 热替换 delegate
  │
  └──► 其他监听器（用户自定义、监控、缓存刷新等）
```

---

## 三个角色

### 角色 1：15-configuration 的发布者

15 的四个后端模块在配置变更时发布 `PropertySourcesChangedEvent`：

| 后端 | 发布触发点 |
|------|-----------|
| Nacos | `ConfigClient.addEventListener()` → `refresher.refresh()` |
| Apollo | `ConfigChangeListener.onChanged()` → `context.publishEvent()` |
| etcd | `Watch.watch()` → `onConfigChanged()` → `refresher.refresh()` |
| ZooKeeper | ❌ 未实现刷新 |

### 角色 2：18-dynamic 的消费者

`PropagatingDynamicJdbcConfigChangedEventListener` 监听事件，检查变更的 key 是否是 `microsphere.dynamic.jdbc.configs.*` 前缀下的配置。

### 角色 3：DynamicDataSource 的执行者

`RefreshingDynamicDataSourceListener` 收到 `DynamicJdbcConfigChangedEvent` 后重建 DataSource。

---

## PropagatingDynamicJdbcConfigChangedEventListener 源码解析

位置：`microsphere-dynamic-jdbc-spring-boot/.../context/PropagatingDynamicJdbcConfigChangedEventListener.java`（108 行）

### 类声明

```java
class PropagatingDynamicJdbcConfigChangedEventListener implements SmartApplicationListener {

    private final Set<String> dynamicJdbcConfigPropertyNames;
    private final ConfigurableApplicationContext context;
    private final ConfigurableEnvironment environment;
}
```

实现 `SmartApplicationListener` 而不是 `ApplicationListener`——通过 `supportsEventType()` 声明只处理两种事件，避免收到无关事件时的类型转换。

### supportsEventType()

```java
@Override
public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
    return PropertySourcesChangedEvent.class.equals(eventType)
            || ZoneContextChangedEvent.class.equals(eventType);
}
```

监听两种事件：
1. `PropertySourcesChangedEvent`——来自 15-configuration 的配置变更
2. `ZoneContextChangedEvent`——来自 17-multiactive 的区域切换

注意：`eventType.equals()` 是**精确匹配**，不是 `isAssignableFrom()`。如果未来有人发布 `PropertySourcesChangedEvent` 的子类事件，这个监听器不会收到——需要改为 `eventType.isAssignableFrom(...)` 才能兼容子类。

### onPropertySourcesChangedEvent()

```java
private void onPropertySourcesChangedEvent(PropertySourcesChangedEvent event) {
    Set<String> keys = event.getChangedProperties().keySet();

    for (String key : keys) {
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

处理逻辑：

```
1. 获取变更的属性 key 集合
2. 遍历，检查是否匹配 DynamicJdbcConfig 的 property name
   （microsphere.dynamic.jdbc.configs.{name}）
3. 匹配 → 重新解析 JSON 为 DynamicJdbcConfig
4. 发布 DynamicJdbcConfigChangedEvent
```

关键点：**只响应 `microsphere.dynamic.jdbc.configs.*` 前缀的变更**。修改其他配置不会触发 DataSource 重建。

---

## 配置热更新的完整流程

### 场景：Nacos 中修改 DynamicJdbcConfig

```
T0: 运维在 Nacos 修改 microsphere.dynamic.jdbc.configs.orders 的值
  │
  ▼
T1: Nacos 服务端推送变更
  → NacosPropertySourceLoader 的 ConfigClient.addEventListener() 回调
  → refresher.refresh(dataId, resource)  // 第一个参数是 dataId（如 "app.json"）
  → diff 发现内容变化
  → PropertySourcesChangedEvent 发布
  │
  ▼
T2: PropagatingDynamicJdbcConfigChangedEventListener 收到事件
  → event.getChangedProperties() 包含 "microsphere.dynamic.jdbc.configs.orders"
  → 匹配 dynamicJdbcConfigPropertyNames
  → getDynamicJdbcConfig(environment, key) 重新解析 JSON
  → 发布 DynamicJdbcConfigChangedEvent
  │
  ▼
T3: RefreshingDynamicDataSourceListener 收到事件
  → 检查 propertyName 匹配
  → findParentContext() 找到正确的父上下文
  → initializeDataSource(config, propertyName, parentContext)
  │
  ▼
T4: DynamicDataSource 重建
  → createDynamicDataSourceConfig() 深克隆
  → 新 DynamicJdbcChildContext 创建
  → mergeParentEnvironment() + refresh()
  → 从新子上下文获取 DataSource
  → synchronized(mutex) 原子替换 delegate
  → 旧子上下文 60s 后延迟关闭
  │
  ▼
T5: 新配置生效
```

### 场景：修改非 JDBC 配置

```
运维修改 microsphere.availability.zone 或其他无关配置
  → PropertySourcesChangedEvent 发布
  → PropagatingDynamicJdbcConfigChangedEventListener 收到
  → 遍历 key，没有匹配 microsphere.dynamic.jdbc.configs.*
  → 不做任何事（忽略）
```

### 场景：ZooKeeper 后端

```
运维修改 ZooKeeper 中的配置
  → ZookeeperPropertySourceLoader 没有实现刷新
  → 不发布 PropertySourcesChangedEvent
  → 18-dynamic 收不到事件
  → 配置不生效（需要重启）
```

---

## 常见问题

### 1. 事件作用域

`PropertySourcesChangedEvent` 继承 `ApplicationContextEvent`，事件只在本 ApplicationContext 内传播。

在 18-dynamic 的多层上下文中：

```
根上下文（发布事件）→ 根上下文内的监听器收到 ✅
子上下文（发布事件）→ 根上下文内的监听器收不到 ❌
```

`PropagatingDynamicJdbcConfigChangedEventListener` 注册在根上下文（`DynamicJdbcContextApplicationListener` 中通过 `context.addApplicationListener()` 注册）。所以**配置变更必须在根上下文发布**才能触发 DataSource 重建。

15-configuration 的 Loader 在哪个上下文发布？——取决于 `@NacosPorpertySource` 标注的配置类所在的上下文。如果标注在根上下文的 `@Configuration` 类上，事件在根上下文发布，18-dynamic 能收到。

### 2. 一次变更多个 key 的情况

如果一次变更同时修改了多个 `microsphere.dynamic.jdbc.configs.*` 配置，事件中的 `changedProperties` 包含多个 key。监听器会遍历所有 key，对每个匹配的 key 发布一个 `DynamicJdbcConfigChangedEvent`——导致多个 DynamicDataSource 同时重建。

### 3. 变更内容相同的情况

Nacos 和 etcd 的 refresher 会在发布事件前做 diff（比较新旧内容）。如果内容没有变化，不发布事件。但 Apollo 模块不区分"内容相同"——Apollo 的 `ConfigChangeEvent` 只在 key 变化时触发，所以 Apollo 不会出现"内容相同但发布事件"的情况。

### 4. 配置中心不可用的容错

如果配置中心（Nacos/Apollo）启动时不可用：
- Nacos：`resolveResources()` 抛出异常 → 配置类加载失败 → 应用可能启动失败
- Apollo：Apollo 客户端本身有本地缓存，会降级使用缓存
- etcd：`kv.get().get()` 无参调用会**无限期阻塞**（没有超时），etcd 不可用时应用启动挂起
- ZooKeeper：Curator 有 `RetryForever(300)` 重试策略，会一直重试直到可用

### 5. 与 17-multiactive 的联动

`PropagatingDynamicJdbcConfigChangedEventListener` 同时监听 `ZoneContextChangedEvent`。Zone 切换时：

```
ZoneContext.setZone("shanghai-2")
  → ZoneContextChangedEvent（17-multiactive 发布）
    → PropagatingDynamicJdbcConfigChangedEventListener
      → 检查 config.hasHighAvailabilityDataSource()
      → 发布 DynamicJdbcConfigChangedEvent
        → DynamicDataSource 重建（用新 Zone 的配置）
```

这条链路与 15-configuration 无关——Zone 切换不经过配置中心。
