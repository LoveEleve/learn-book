# 17-08 配置中心集成分析

## 目录

- [配置中心集成解决什么问题](#配置中心集成解决什么问题)
- [Spring Cloud Config / Nacos / Apollo 的集成方式](#spring-cloud-config--nacos--apollo-的集成方式)
- [17 的实现：PropertySourcesChangedEvent](#17-的实现propertysourceschangedevent)
- [完整链路：配置变更到 Zone 切换](#完整链路配置变更到-zone-切换)
- [与 18-dynamic 的集成](#与-18-dynamic-的集成)
- [工程问题分析](#工程问题分析)

---

## 配置中心集成解决什么问题

17-multiactive 的 `ZoneContext` 可以在运行时通过 `setZone()` 切换区域。但 who 调用 `setZone()`？

三个来源：

| 来源 | 谁触发 | 调用方式 |
|------|--------|---------|
| ZoneLocator 启动定位 | `ZoneAutoConfiguration` | 启动时自动调用 |
| 运维手动调用 | 运维脚本 / JMX | 直接 `ZoneContext.get().setZone()` |
| **配置中心推送** | Nacos / Apollo / Spring Cloud Config | 配置变更 → `PropertySourcesChangedEvent` → `ZoneContext.setZone()` |

配置中心集成解决的是第三个来源——**运维在配置中心改一个值，所有实例自动切换区域**。

---

## Spring Cloud Config / Nacos / Apollo 的集成方式

### 标准的 Spring Boot 配置更新路径

```
配置中心推送新配置
  → Environment 中的 PropertySource 被更新
  → @ConfigurationProperties 绑定的 Bean 自动刷新
  → 需要 @RefreshScope 才能让 Bean 重新初始化
```

但 `ZoneContext` 不是 Spring Bean（是单例），不能使用 `@RefreshScope`。所以标准的 Spring Boot 配置刷新机制对 ZoneContext 无效。

### 三种配置中心的直接集成

| 配置中心 | 原生刷新机制 | 对 ZoneContext 是否有效 |
|----------|------------|----------------------|
| Spring Cloud Config | `@RefreshScope` + `/actuator/refresh` | ❌ ZoneContext 不是 Spring Bean |
| Nacos | `@NacosValue` 自动更新 | ❌ ZoneContext 不使用 Nacos API |
| Apollo | `@ApolloConfigChangeListener` | ❌ ZoneContext 不使用 Apollo API |

17-multiactive 不能直接依赖 Nacos/Apollo 的 API，因为它是通用组件，不绑定任何配置中心。

---

## 17 的实现：PropertySourcesChangedEvent

### 抽象事件层

`microsphere-spring-boot` 定义了一个抽象事件 `PropertySourcesChangedEvent`，作为配置中心变更的统一抽象：

```
Nacos 推送       → 适配层转为 PropertySourcesChangedEvent
Apollo 推送      → 适配层转为 PropertySourcesChangedEvent
Spring Cloud Config 推送 → EnvironmentChangeEvent → 适配层转为 PropertySourcesChangedEvent
```

无论底层是哪个配置中心，`microsphere-spring-boot` 的适配层都会发布统一的 `PropertySourcesChangedEvent`。具体的适配实现取决于 `microsphere-configuration` 模块对接了哪些配置中心。

### 17 中的响应

17 本身**没有直接监听** `PropertySourcesChangedEvent`。

它由 18-dynamic 的 `PropagatingDynamicJdbcConfigChangedEventListener` 监听，然后在 18 内部触发 `DynamicJdbcConfigChangedEvent`，最终由 `DynamicDataSource` 重建。

但这里有一个缺失：`ZoneContext.setZone()` 本身不是由 `PropertySourcesChangedEvent` 触发的。需要有人在中间"读取新配置 → 调用 setZone()"。

### 这个"中间人"是谁？

看 17 的代码结构，**目前没有**。17 没有监听 PropertySourcesChangedEvent 并自动调用 setZone() 的组件。

完整的链路应该是：

```
配置中心修改 microsphere.availability.zone=shanghai-2
  → Environment 中的属性被更新
  → PropertySourcesChangedEvent 发布
    → ❌ 17 没有监听这个事件
    → ❌ ZoneContext.setZone() 没有被调用
```

这是一个**功能缺口**——17 提供了 ZoneContext 和事件传播机制，但没有监听配置中心变更并调用 setZone() 的组件。需要外部系统（或用户代码）来完成这个调用。

---

## 完整链路：配置变更到 Zone 切换

### 当前实际链路

```
运维手动调用：
  运维脚本 → ZoneContext.setZone("shanghai-2")
    → PropertyChangeSupport.firePropertyChange(...)
    → ZoneContextChangedListener.propertyChange()
      → 更新 System property
      → 发布 ZoneContextChangedEvent
        → 18-dynamic 收到事件 → DataSource 重建
        → LoadBalancer 的 ZonePreferenceServiceInstanceListSupplier 生效 → 路由更新
```

### 需要补全的链路

```
配置中心修改 microsphere.availability.zone=shanghai-2
  → Environment 更新
  → PropertySourcesChangedEvent 发布
    → 需要中间组件：
      → 读取 Environment 中新值
      → ZoneContext.get().setZone(newValue)
        → 同上链路
```

### 中间组件的设计（可新增）

如果要在 17 内部补全这个缺口，需要新增一个 `ZonePropertyChangedListener`（或类似组件）：

```java
// 伪代码：需要注入 environment 和导入常量
public class ZonePropertyChangedListener implements ApplicationListener<PropertySourcesChangedEvent> {

    private final Environment environment;

    public ZonePropertyChangedListener(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(PropertySourcesChangedEvent event) {
        // 检查变更的属性中是否包含 zone 配置
        Set<String> changedKeys = event.getChangedProperties().keySet();
        for (String key : changedKeys) {
            if (key.contains("availability.zone")) {
                // 从 Environment 读取新值
                String newZone = environment.getProperty("microsphere.availability.zone");
                if (newZone != null && !newZone.isEmpty()) {
                    // 写入 ZoneContext
                    ZoneContext.get().setZone(newZone.trim());
                }
                break;
            }
        }
    }
}
```

### 注册到 AutoConfiguration

这个 Listener 需要在 `ZoneAutoConfiguration` 中注册为 `@Bean`，而不是使用 `@ComponentScan`：

```java
@Configuration
public class ZoneAutoConfiguration {

    @Bean
    public ZonePropertyChangedListener zonePropertyChangedListener(Environment environment) {
        return new ZonePropertyChangedListener(environment);
    }
}
```

这个组件目前不存在，属于缺失功能。

---

## 与 18-dynamic 的集成

### 当前的集成状态

```
ZoneContext.setZone()  ← 只能手动或通过 ZoneLocator
  → ZoneContextChangedEvent
    → 18-dynamic 的 RefreshingDynamicDataSourceListener
      → DynamicDataSource 重建  ✅ 这部分正常
    → 其他 Listener（路由、负载均衡）
      → 相应组件更新  ✅ 这部分正常
```

### 缺口的影响

没有自动的配置中心监听意味着：

| 场景 | 当前状态 | 影响 |
|------|---------|------|
| 启动时 ZoneLocator 定位 | ✅ 自动 | 正常工作 |
| 运行时配置中心改 zone | ❌ 不能自动 | 需要运维写脚本调用 setZone() 或手动重启 |
| Zone 切换后 DataSource 重建 | ✅ 自动 | 只要 setZone() 被调用，后续链路正常 |

### my-xhs 方案的参考

在 my-xhs 的异地多活方案（17-04）中，通过新增 `ZonePropertyChangedListener` 来补全这个缺口，并在 `ZoneAutoConfiguration` 中注册监听 Nacos 配置变更。

---

## 工程问题分析

### 为什么 17 没有内置配置中心监听

17 被设计为"区域基础设施"——提供 ZoneContext、ZoneLocator、ZonePreferenceFilter、事件传播，但不绑定任何配置中心。配置中心的集成被视为"上层集成"（由 18-dynamic 或其他模块完成）。

这是一种关注点分离的设计：

```
17-multiactive：区域核心（ZoneContext + 事件 + 路由）
18-dynamic：数据库层集成（监听 ZoneContextChangedEvent 重建 DataSource）
配置中心集成层：缺失（需要补充）
```

### 补全方案的选型

| 方案 | 优点 | 缺点 |
|------|------|------|
| 在 17 内部新增 `ZonePropertyChangedListener` | 开箱即用，用户不需要额外代码 | 17 需要依赖 `PropertySourcesChangedEvent`（来自 microsphere-spring-boot） |
| 在 18-dynamic 中触发 setZone() | 事件链路集中管理 | 18 职责越界（数据库模块不应该负责区域切换） |
| 用户自己实现 | 灵活 | 每个用户都要写同样的代码 |

### 时序一致性

即使补全了监听器，配置中心推送 → 实例收到 → 切换生效之间有时延：

```
T0: 运维在 Nacos 修改 zone = shanghai-2
T1: 实例 A 收到推送 → 切换 → DataSource 重建完成
T2: 实例 B 收到推送 → 切换 → DataSource 重建完成
T3: 所有实例切换完成
```

T1 到 T3 之间，不同实例可能处于不同区域，跨区域调用可能出现不一致。这是多活架构中固有的问题，不是 17 或 18 能解决的。
