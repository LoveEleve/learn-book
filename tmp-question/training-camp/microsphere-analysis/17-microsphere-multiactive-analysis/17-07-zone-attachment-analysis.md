# 17-07 区域信息传播深度分析

## 目录

- [区域信息传播解决什么问题](#区域信息传播解决什么问题)
- [三条传播路径](#三条传播路径)
- [路径 A：HTTP 调用间传播](#路径-ahttp-调用间传播)
- [路径 B：Eureka 注册时广播](#路径-beureka-注册时广播)
- [路径 C：区域变更事件传播](#路径-c区域变更事件传播)
- [信息传播链全貌](#信息传播链全貌)
- [工程问题分析](#工程问题分析)

---

## 区域信息传播解决什么问题

在异地多活架构中，"同区域优先"的前提是**每个服务实例都知道自己的区域，并且能把区域信息传递给下游**。

```
服务 A（北京）调用服务 B（北京实例 + 上海实例）
  → 服务 A 需要知道服务 B 的实例中哪些在北京、哪些在上海
  → 服务 A 的调用结果需要告诉下游"这次调用是北京的实例处理的"
```

### 信息从哪来，到哪去

```
来源                         用途
ZoneLocator 定位当前区域  →  1. 注册到注册中心（Eureka metadata）
                           2. 附加到 HTTP 请求（Header）
                           3. 区域变更时通知所有监听器
```

区域信息传播的核心问题可以总结为：

| 问题 | 方案 |
|------|------|
| 服务启动时怎么告知注册中心自己的区域？ | `ZoneAttachmentPreRegistrationHandler` 在 Eureka 注册时写入 metadata |
| 服务间调用时怎么传递区域信息？ | `ZoneAttachmentHandler` 写入/读取 HTTP Header |
| 区域变更时怎么通知所有组件？ | `ZoneContextChangedListener` + `ZoneContextChangedEvent` |

### 为什么需要三条路径

因为三个场景的需求不同，不能用一个方案覆盖所有：

| 路径 | 时序 | 目标 |
|------|------|------|
| A：HTTP 调用 | 每次请求 | 上游实例的区域信息 |
| B：注册中心 | 启动时 + 续约时 | 所有实例的区域信息 |
| C：事件 | 区域变更时 | 当前实例内的所有组件 |

---

## 三条传播路径

```
路径 A（调用时，每次请求）
  ┌──────────┐       HTTP Header       ┌──────────┐
  │  服务 A   │ ───── X-Zone: bj ─────→ │  服务 B   │
  │ (当前 bj) │                         │ 解析 Header│
  └──────────┘                         └──────────┘

路径 B（注册时，启动 + 续约）
  ┌──────────┐   Eureka Register   ┌────────────┐
  │  服务 A   │ ─── metadata ─────→ │  Eureka    │
  │ (当前 bj) │   zone=bj          │  注册中心   │
  └──────────┘                     └────────────┘
                                         │
  ┌──────────┐   Eureka Discovery  ┌─────┘
  │  服务 B   │ ←─── instanceInfo ──┘
  │ 解析 zone │     metadata.zone=bj
  └──────────┘

路径 C（变更时，事件驱动）
  ┌────────────┐  PropertyChange   ┌────────────────────┐
  │ ZoneContext │ ── "zone" ─────→ │ ZoneContextChanged  │
  │  setZone()  │    old=new       │ Listener            │
  └────────────┘                   └─────────┬──────────┘
                                             │
                    ┌────────────────────────┼────────────────┐
                    ▼                        ▼                ▼
            ┌──────────────┐   ┌──────────────────┐   ┌──────────────┐
            │ 更新 System  │   │ 发布 Spring 事件  │   │ 外部监听器   │
            │ property     │   │ ZoneContextChanged│   │ (如 18-dynamic)│
            └──────────────┘   └──────────────────┘   └──────────────┘
```

---

## 路径 A：HTTP 调用间传播

### ZoneAttachmentHandler

位置：`microsphere-multiactive-commons/.../ZoneAttachmentHandler.java`（43 行）

```java
public class ZoneAttachmentHandler {

    public void attach(Map<String, String> headers) {
        // 将当前区域写入 HTTP Header
        headers.put("X-Zone", ZoneContext.get().getZone());
    }

    public String resolve(Map<String, String> headers) {
        // 从 HTTP Header 中读取区域
        return headers.get("X-Zone");
    }
}
```

设计要点：

| 设计 | 说明 |
|------|------|
| **43 行，极简** | 整个传播链路的入口，职责单一——只做区域信息的读写 |
| **无框架依赖** | 在 commons 包中，零 Spring 依赖。纯 Map 操作，任何 HTTP 客户端/服务端框架都能集成 |
| **无状态的** | 不持有任何状态，每次调用从 ZoneContext 读取当前区域 |
| **Header 名固定** | 用 `X-Zone` 作为 Header 名称，调用方和接收方约定一致 |

### 在 Spring Cloud 中的集成

`ZoneAttachmentListener`（spring-cloud 模块）作为 `ApplicationListener<ZoneContextChangedEvent>`，在区域变更时更新 HTTP Header 的 Attachment 配置，确保后续请求携带最新的区域信息。它从接收到事件到更新配置的完整链路：

```
ZoneContextChangedEvent
  → ZoneAttachmentListener.onApplicationEvent()
    → 读取事件中的新区域值
    → 更新 HTTP 拦截器中的 Attachment 配置
    → 后续请求的 Header 使用新区域
```

### 调用方的集成

```java
// 服务 A 调用服务 B 之前
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();

// 将当前区域附加到请求
zoneAttachmentHandler.attach(headers);
// headers 中现在有 X-Zone: beijing-1

// 发送请求
HttpEntity<String> entity = new HttpEntity<>(headers);
restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
```

### 接收方的集成

```java
// 服务 B 收到请求
// 从请求 Header 中解析区域
HttpHeaders headers = request.getHeaders();
String callerZone = zoneAttachmentHandler.resolve(headers);
// callerZone = "beijing-1"
```

### 实际使用场景

`ZoneAttachmentHandler` 在以下场景中使用：

```
服务间 HTTP 调用
  → 调用方 attach(zone) → HTTP Header X-Zone: beijing-1
  → 接收方 resolve(headers) → "beijing-1"
  → 接收方根据调用方区域做不同的处理逻辑

网关转发
  → 客户端请求到达 Gateway
  → Gateway 附加当前区域信息到转发请求
  → 下游服务读取区域信息做同区域优先路由
```

---

## 路径 B：Eureka 注册时广播

### ZoneAttachmentPreRegistrationHandler

位置：`microsphere-multiactive-netflix/.../eureka/ZoneAttachmentPreRegistrationHandler.java`

这个类在服务实例向 Eureka 注册时，将当前区域写入 InstanceInfo 的 metadata 中。

```java
// ZoneAttachmentPreRegistrationHandler 的核心逻辑
// 在 Eureka 注册/续约前执行
// 将当前区域写入 InstanceInfo 的 metadata
// metadata 中的 zone 信息会被其他服务通过 EurekaInstanceInfoZoneResolver 读取
```

### 调用链路

```
服务启动
  → Eureka 注册流程
    → ZoneAttachmentPreRegistrationHandler 拦截注册
      → 读取 ZoneContext.get().getZone() 获取当前区域
      → 写入 instanceInfo.getMetadata().put("zone", currentZone)
    → Eureka Server 收到注册信息，metadata 中包含 zone

其他服务发现
  → 服务 B 通过 Eureka 获取服务 A 的实例列表
  → 每个实例的 InstanceInfo 中包含 metadata.zone
  → EurekaInstanceInfoZoneResolver 从 metadata 中解析区域
  → ZonePreferenceFilter 根据区域信息做同区域优先路由
```

### 与路径 A 的区别

| | 路径 A：HTTP Header | 路径 B：Eureka metadata |
|---|---|---|
| 传播范围 | 单次请求的上下游 | 注册中心的所有订阅者 |
| 更新频率 | 每次请求 | 实例启动/续约时 |
| 用途 | 调用链路追踪 | 服务发现 + 负载均衡 |
| 依赖 | HTTP 客户端/服务端 | Spring Cloud Netflix |

### EurekaInstanceInfoZoneResolver

位置：`microsphere-multiactive-netflix/.../eureka/EurekaInstanceInfoZoneResolver.java`

```java
// 从 Eureka 的 InstanceInfo 中解析区域
public class EurekaInstanceInfoZoneResolver implements ZoneResolver<InstanceInfo> {
    @Override
    public String resolve(InstanceInfo instanceInfo) {
        // 从 InstanceInfo 的 metadata 中读取 zone
        return instanceInfo.getMetadata().get("zone");
    }
}
```

这个类在 `ZonePreferenceServerListFilter`（Ribbon）中使用，用于从 Eureka 实例中过滤出同区域的实例。

### DiscoveryClientServer / DiscoveryClientServerList

这两个适配器类将 Eureka 的 `InstanceInfo` 转为 Ribbon 的 `Server`：

```
Eureka 的 InstanceInfo
  → DiscoveryClientServerList 获取实例列表
  → DiscoveryClientServer 包装每个实例为 Server
  → ZonePreferenceServerListFilter 使用 ZoneResolver 从 Server 中解析区域
  → 过滤出同区域实例
```

---

## 路径 C：区域变更事件传播

### ZoneContextChangedListener

位置：`microsphere-multiactive-spring/.../event/ZoneContextChangedListener.java`（251 行）

这是 17-multiactive 中最关键的文件之一（251 行）。它负责将 `ZoneContext` 的本地属性变更（`PropertyChangeSupport`）转化为全局可见的状态变更（System property）和 Spring 事件（`ZoneContextChangedEvent`）。

### 完整代码逻辑

`ZoneContextChangedListener` 同时实现了 `PropertyChangeListener`（接收 ZoneContext 的属性变更）和 `ApplicationEventPublisherAware`（获取发布事件的能力）：

```java
public class ZoneContextChangedListener
        implements PropertyChangeListener, ApplicationEventPublisherAware {

    private String originalZone;
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.eventPublisher = publisher;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (!"zone".equals(event.getPropertyName())) {
            return;
        }

        String newZone = (String) event.getNewValue();
        String oldZone = (String) event.getOldValue();

        // 首次切换时记录 originalZone
        if (originalZone == null) {
            originalZone = oldZone;
        }

        // 更新 System property（全局可见）
        System.setProperty(CURRENT_ZONE_PROPERTY_NAME, newZone);

        // 通过 ApplicationEventPublisher 发布 Spring 事件
        eventPublisher.publishEvent(new ZoneContextChangedEvent(this, oldZone, newZone));
    }
}
```

关键点：

| 方面 | 说明 |
|------|------|
| **事件发布能力** | 通过 `ApplicationEventPublisherAware` 注入 publisher，不是直接 `publishEvent()` |
| **PropertyChangeListener** | 监听 `ZoneContext` 的属性变更（JavaBeans 标准，无框架依赖） |
| **非 ApplicationListener** | 不实现 `ApplicationListener`——它只负责发布事件，不消费自己发布的事件 |

### 事件发布与消费的分工

| 类 | 角色 | 接口 |
|---|------|------|
| `ZoneContextChangedListener` | **发布者** | `PropertyChangeListener` + `ApplicationEventPublisherAware` |
| 其他 Listener（如 `PropagatingDynamicJdbcConfigChangedEventListener`） | **消费者** | `ApplicationListener<ZoneContextChangedEvent>` |

`ZoneContextChangedListener` 不消费自己发布的事件。它从 `PropertyChangeSupport` 收到变更通知，转为 Spring 事件后广播出去。真正的消费方是 18-dynamic 的 `PropagatingDynamicJdbcConfigChangedEventListener` 等外部监听器。

### originalZone 回退机制

```
首次切换：
  ZoneContext.setZone("beijing-1" → "shanghai-2")
    → ZoneContextChangedListener.propertyChange()
      → originalZone = "beijing-1"（首次设置）
      → System.setProperty("microsphere.current.availability.zone", "shanghai-2")
      → publishEvent ZoneContextChangedEvent

再次切换（回到 original）：
  ZoneContext.setZone("shanghai-2" → "beijing-1")
    → 触发变更事件
    → System property 更新
    → originalZone 保持不变（仍然是 "beijing-1"）

外部系统回退：
  健康检查发现新 zone 不可用
  → ZoneContext.setZone(originalZone)
  → 触发变更事件
  → 所有组件回到原始配置
```

### 与 18-dynamic 的集成

`ZoneContextChangedListener` 发布 `ZoneContextChangedEvent` 后，18-dynamic 的 `PropagatingDynamicJdbcConfigChangedEventListener` 收到事件，触发 DataSource 重建。

完整链路已在 17-05 中详细分析。

---

## 信息传播链全貌

综合三条路径，区域信息在服务间的完整传播链：

```
服务 A 启动
  │
  ├─ ZoneLocator 定位当前区域：beijing-1
  │
  ├─ ZoneContext.setZone("beijing-1")
  │   → PropertyChangeSupport 触发
  │   → ZoneContextChangedListener 收到事件
  │     → 更新 System property
  │     → 发布 ZoneContextChangedEvent
  │
  ├─ 路径 B：注册到 Eureka
  │   → ZoneAttachmentPreRegistrationHandler
  │   → metadata.zone = "beijing-1"
  │   → Eureka 注册
  │
  ├─ 服务 A 调用服务 B
  │   → 路径 A：ZoneAttachmentHandler.attach(headers)
  │   → HTTP Header X-Zone: beijing-1
  │   → 服务 B 收到请求
  │     → ZoneAttachmentHandler.resolve(headers)
  │     → 获取调用方区域："beijing-1"
  │
  └─ 区域变更（beijing-1 → shanghai-2）
      → 路径 C：ZoneContextChangedListener
        → 更新 System property
        → 发布 ZoneContextChangedEvent
          → 18-dynamic 收到事件
          → DataSource 重建
```

### 关键交互关系

```
ZoneLocator（定位）──→ ZoneContext（持有）──→ ZoneAttachmentHandler（HTTP 传播）
                          │
                          ├──→ ZoneAttachmentPreRegistrationHandler（Eureka 广播）
                          │
                          └──→ ZoneContextChangedListener（事件传播）
                                  │
                                  ├──→ System property（进程内全局可见）
                                  │
                                  └──→ ZoneContextChangedEvent（Spring 事件）
                                          │
                                          ├──→ ZoneAttachmentListener（更新 Attachment）
                                          │
                                          └──→ PropagatingDynamicJdbcConfigChangedEventListener
                                                  → DataSource 重建
```

---

## 工程问题分析

### 三条路径的冗余设计

设计三条传播路径不是功能堆叠，而是三个不同维度的需求：

| 路径 | 如果不这么做会怎样 |
|------|------------------|
| HTTP Header | 下游服务不知道调用方来自哪个区域，无法做区域感知处理 |
| Eureka metadata | ZonePreferenceFilter 没有区域信息来过滤实例，只能返回全部 |
| 事件 | ZoneContext 变了但 LoadBalancer、DataSource 不知道，继续用旧配置路由 |

### ZoneAttachmentHandler 为什么只有 43 行

43 行是因为它的职责被刻意限制——只做"读 ZoneContext → 写 Map"和"读 Map → 返回 String"。不做 Header 解析的具体实现，不做 HTTP 框架的适配。框架适配交给集成方（Spring Cloud Gateway、RestTemplate 拦截器等）。

### Eureka 注册时广播的时序问题

服务启动时，`ZoneAttachmentPreRegistrationHandler` 在 Eureka 注册前执行。但如果 ZoneLocator 定位失败（比如 AWS 元数据端点超时），区域为空，`ZoneAttachmentPreRegistrationHandler` 不会写入空的 metadata。其他服务通过 `EurekaInstanceInfoZoneResolver` 解析时，metadata 中没有 zone，该实例在 `ZonePreferenceFilter` 中不被计入"有区域信息的实例"，影响 `zoneReadyPercentage` 的计算。

### ZoneContextChangedListener 为什么不直接实现 ApplicationListener

`ZoneContextChangedListener` 只实现了 `PropertyChangeListener`，没有实现 `ApplicationListener`。事件发布通过 `ApplicationEventPublisherAware` 获取 publisher，事件消费由独立的 `ApplicationListener` 完成。

这种分工的原因是：

| 职责 | 谁干 | 为什么分开 |
|------|------|-----------|
| 监听 ZoneContext 变更 | `ZoneContextChangedListener.propertyChange()` | 只能通过 PropertyChangeListener 接收（ZoneContext 不是 Spring Bean） |
| 发布 Spring 事件 | `ZoneContextChangedListener`（通过 injected publisher） | 将 JavaBeans 事件转为 Spring 事件 |
| 消费 Spring 事件 | 独立的 `ApplicationListener` 实现（如 18-dynamic 的 Listener） | 关注点分离——发布者和消费者不应该耦合 |

如果让 `ZoneContextChangedListener` 同时实现 `ApplicationListener<ZoneContextChangedEvent>`，会导致它发布一个事件后又立刻消费自己发布的事件。这不是级联处理，而是不必要的自消费。
