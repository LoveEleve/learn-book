# stage-3 · 第 10 节：第八节："高并发、高可用" RPC 架构升级 — 知识点提取

> 课程：stage-3 三高架构 第 10 节（容器/服务组收官 10/10）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/10. 第八节："高并发、高可用"RPC 架构升级.md`
> 提取时间：2026-08-12 | 权重：核心（RPC 架构——Dubbo 调用链/Cluster/协议设计/Triple；docs 关联 stage-2 21/22 RPC 微内核）
> 案例载体：my-xhs（决策 B）

---

## 一、本节概览

- **技术域**：Dubbo RPC（暴露/消费调用链/Registry/Cluster/Filter/Invoker 模型）、RPC 协议设计（数据格式/结构/通用性）、HTTP/1.1 vs gRPC、Triple 协议（Dubbo3 主力）
- **维度**：`[工程问题]`（Dubbo 调用链/SPI）+ `[分布式问题]`（Cluster/注册中心抽象）+ `[分布式理论]`（协议设计取舍）+ `[规范]`（Triple/gRPC）
- **核心命题**：**RPC 架构升级三主题**（docs 主要内容）——①gRPC 服务升级（REST vs gRPC）②Dubbo Triple 协议升级 ③RPC 服务定制（业务权重定制线程/协议/负载均衡）；docs 关联 stage-2 21/22（RPC 微内核/生态整合）
- **知识点数**：8 个
- **前置**：stage-2 21/22（RPC 微内核/生态——docs 关联）、06 篇（Feign 调用链）、07 篇（注册中心）、09 篇（HTTP/2）

## 前置条件清单
读者需先掌握：
1. **RPC 微内核设计**（stage-2 21：Invoker/Protocol/SPI——docs 关联）
2. **RPC 生态整合**（stage-2 22）
3. **注册中心机制**（07 篇：URL/订阅）
4. **HTTP/2**（09 篇：多路复用/头部压缩）
未达前置者，先补：stage-2 21/22、07/09 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **源码实证**：Dubbo（code/spring/dubbo——RegistryProtocol/TripleProtocol/RouterChain）
- **实例对照**：my-xhs 无 Dubbo（诚实标注），用 OpenFeign——RPC 定制主题对照 my-xhs 自定义负载均衡（LeastConnectionsLoadBalancer）
- **docs 场景 vs 现状**：docs 的 URL 示例为 Dubbo 2.7.10/zookeeper（2016 前后）；Dubbo3 Triple 是当前主力（2022 双十一全量）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Dubbo 定位与开源路线（RPC 框架 + 服务治理 + Dubbo3 云原生）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：RPC 概念（stage-2 21）
- **来源**：docs §Apache Dubbo 简介 + §开源路线
- **需求**：了解 Dubbo 的定位与演进——**国内 RPC 事实标准 → 云原生 Dubbo3**
- **自主实现**：若我设计——RPC 框架 = 远程调用 + 服务治理（发现/均衡/流量/观测/鉴权）一体化
- **参考实现**（docs）：**定位**——RPC 服务开发框架，解决微服务下的服务治理与通信（多语言 SDK）；原生具备远程地址发现与通信；**核心特性 11 项**（docs）——微服务开发/服务发现/负载均衡/流量管控/通信协议/扩展适配/观测服务/认证鉴权/服务网格/微服务生态/高级功能；**开源路线**——2008 阿里巴巴开源 → 国内事实标准 → 2017 Apache 顶级项目 → 阿里内部 HSF 与社区 Dubbo 融合 → **Dubbo3（2022 双十一全面取代 HSF，电商核心/阿里云全面运行）**；云原生方向——Dubbo3/Proxyless Mesh（docs 明确）
- **对比取舍**：**Dubbo（RPC+治理一体）vs 纯 HTTP 栈（REST/Feign）**——性能/治理丰富度 vs 通用性/穿透性（KP-07 协议取舍的框架级表现）；国内 RPC 主流 Dubbo，HTTP 栈通用
- **测试佐证**：docs §简介/§开源路线 + code/spring/dubbo（源码存在实证）

### KP-02 服务暴露与消费调用链（export/refer + Protocol 包装器链）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：SPI（stage-2 21）
- **来源**：docs §核心 SPI（调用链两图）+ dubbo 源码实证
- **需求**：掌握 RPC 框架的**两端调用链**——服务暴露（export）与服务消费（refer）的包装器链
- **自主实现**：若我设计——暴露：ServiceConfig → Protocol 链（QoS/Filter/Listener 包装）→ Registry 注册；消费：ReferenceConfig → 代理 → Protocol 链 → Registry 订阅
- **参考实现**（docs 调用链 + 源码实证）：**暴露链（docs）**——`ServiceConfig#export()` → `Protocol$Adaptive`（动态生成）→ `QosProtocolWrapper#export`（QoS 端口）→ `ProtocolFilterWrapper#export`（Filter 链）→ `ProtocolListenerWrapper#export`（ExporterListener）→ `RegistryProtocol#export` → `Registry#register(URL)`；**消费链（docs）**——`ReferenceConfig#get()` → `init()` → `createProxy()` → `Protocol$Adaptive#refer` → Qos/Filter/Listener Wrapper → `RegistryProtocol#refer`；**源码实证**——`dubbo-registry-api/.../integration/RegistryProtocol.java:145`（class，implements Protocol, ScopeModelAware）、`:272`（export）、`:557`（refer）；**组件关联表**（docs）——ServiceConfig/ReferenceConfig 与 Protocol$Adaptive/QosProtocolWrapper/ProtocolFilterWrapper/ProtocolListenerWrapper/RegistryProtocol 的映射
- **对比取舍**：**包装器链（Wrapper 模式）vs 硬编码**——横切能力（QoS/Filter/Listener）可插拔；**为什么暴露用 Invoker、订阅用 URL**（docs §问题 1）——暴露端服务接口确定、Invoker→Exporter 1:1；消费端先拿 URL 集合再合成 Invoker（docs 自答）
- **测试佐证**：docs §暴露/消费调用链（两链 + 关联表）+ `RegistryProtocol.java:145/272/557`

### KP-03 注册中心抽象与注册表结构（Registry 接口/URL/ZK 路径/Nacos）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（ZK 为 docs 场景，Nacos 现代主流） | **置信度**：High
- **前置**：07 篇（注册中心机制）
- **来源**：docs §Dubbo Registry（URL 示例/接口/路径模式）+ 架构师对照
- **需求**：掌握 **RPC 框架的注册中心抽象**——Registry 五接口 + URL 即数据模型 + 注册表路径结构
- **自主实现**：若我设计——Registry 接口（register/unregister/subscribe/unsubscribe/lookup）+ URL 作为统一配置/数据载体 + 按接口/分类建注册表
- **参考实现**（docs）：**RegistryFactory$Adaptive**——按 URL 的 `registry` 参数动态创建实现（`registry://127.0.0.1:2181/...` 示例：registry=zookeeper → `ZookeeperRegistryFactory` → `ZookeeperRegistry`）；**Registry 五接口**——注册（register/unregister）+ 发现（subscribe/unsubscribe/lookup）；**ZK 注册表路径模式**（docs）——`/${group:dubbo}/${interface}/${category:providers}/${encode(url)}`（`/dubbo/com.acme...EchoService/providers/...`）；**URL 即数据**——Dubbo 服务元数据全在 URL 参数（anyhost/application/interface/methods/side/timestamp...docs 完整示例）；**Nacos 实现**（docs 标题）——以数据接口为代表（stage-2 07/08 已深挖）；**FailbackRegistry**——抽象实现（失败重试语义）；**服务自省**（Cloud-Native，docs 标题）——metadata-type=composite（URL 示例实证）
- **对比取舍**：**URL 统一模型 vs 结构化模型**——URL 灵活（全参数化）vs 类型安全；Dubbo 用 URL 承载一切（注册表/配置/协议参数）
- **测试佐证**：docs §Registry（Factory/接口/路径模式/URL 全文）+ stage-2 07/08（Nacos 对照）

### KP-04 Cluster 消费端链路（Directory/Router/LoadBalance 职责链 + RPC 定制）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07 篇（订阅）
- **来源**：docs §Dubbo Cluster（Directory/Router/LoadBalance + 执行链路）+ my-xhs 对照
- **需求**：掌握 **RPC 消费端的选址链路**——URL 集合 → Directory Invoker 集合 → Router 链裁剪 → LoadBalance 选一（**docs 主要内容③"RPC 服务定制"的落点：按业务权重定制负载均衡/路由**）
- **自主实现**：若我设计——订阅存 Invoker 目录（Directory）→ 路由链裁剪（条件/权重/区域）→ 负载均衡选一执行
- **参考实现**（docs + 源码实证 + my-xhs）：**链路（docs 明确）**——`ReferenceConfig` 代理（`InvokerInvocationHandler` JDK 动态代理）→ `MigrationInvoker` → `MockClusterInvoker`（ClusterInterceptor）→ `AbstractClusterInvoker#list`（`:216` `directory.list(invocation)` docs 源码）→ **Directory**（静态 StaticDirectory/动态 DynamicDirectory/注册中心 RegistryDirectory——扩展 DynamicDirectory 并实现 NotifyListener）→ **RouterChain**（`route()` 逐 Router 裁剪：5 个 invokers 经 3 路由变 2 个，docs 源码）→ **LoadBalance**（选一个 Invoker）→ `Invoker#invoke`；**订阅更新**——`NotifyListener#notify` → `RegistryDirectory#refreshOverrideAndInvoker` → `refreshInvoker`（docs 调用链）；**源码实证**——`dubbo-cluster/.../RouterChain.java:120`（route）+ `AbstractClusterInvoker.java`；**RPC 定制（docs 主要内容③）**——"按业务权重定制线程消费、协议和负载均衡"——LoadBalance 可扩展（业务权重）；**my-xhs 对照**——`common/loadbalancer/LeastConnectionsLoadBalancer.java:53`（`implements ReactorServiceInstanceLoadBalancer`——**按连接数定制的负载均衡**，RPC/HTTP 调用侧的同等定制能力）+ zone（区域路由——Router 的同类能力，03 篇）
- **对比取舍**：**Router 裁剪 + LoadBalance 选一 vs 直接选**——流量管控（灰度/权重/区域）vs 简单——**路由与均衡分离**是 Dubbo Cluster 的设计精华（可插拔组合）
- **测试佐证**：docs §Cluster（list 源码/链路/RouterChain route 源码）+ `RouterChain.java:120` + my-xhs `LeastConnectionsLoadBalancer.java:53`

### KP-05 Filter 责任链（SPI/内建/设计缺陷/拦截模式）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：SPI、Servlet Filter（09 篇）
- **来源**：docs §Dubbo Filter（API 定义/内建/设计缺陷/注意事项/实现细节/设计模式）
- **需求**：掌握 **RPC 横切面（Filter）**——invoke 链 + Listener + 责任链/拦截模式；docs 指出其设计缺陷
- **自主实现**：若我设计——Filter 实现 invoke 链（类似 Servlet FilterChain 显式控制）+ onResponse/onError 监听
- **参考实现**（docs）：**API**——`@SPI interface Filter { Result invoke(Invoker, Invocation); }` + `interface Listener { onResponse; onError; }` + `isUsedForProvider()/isUsedForConsumer()`；**内建**——`AccessLogFilter`；**设计缺陷（docs 明确）**——Filter 与 Filter Chain 关系不明确（对比 Servlet Filter：FilterChain 显式控制是否继续）——**Dubbo Filter 靠调用 `invoker.invoke()` 隐式推进链**；**注意事项**——`@Activate` 指明适用 Provider/Consumer；**实现细节**——Provider：`export` 时 `buildInvokerChain(invoker, SERVICE_FILTER_KEY, PROVIDER)`（`?service.filter=filter1&service.filter=filter2` 参数）；Consumer：`refer` 时 `buildInvokerChain(..., REFERENCE_FILTER_KEY, CONSUMER)`；**设计模式**——责任链 + 拦截（前置 preFilter/后置 postFilter/异常 onError）
- **对比取舍**：**隐式链（Dubbo）vs 显式链（Servlet FilterChain）**——docs 明确缺陷：隐式推进可读性差；显式控制明确——**横切面设计要显式化**
- **测试佐证**：docs §Filter（API 全文/export-refer 链源码/缺陷原文）

### KP-06 Invoker/Invocation 模型（代理对象/调用上下文）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：stage-2 21（微内核）
- **来源**：docs §问题（2 问自答）
- **需求**：理解 RPC 的**核心抽象**——Invoker（统一代理）与 Invocation（调用上下文）
- **自主实现**：若我设计——Invoker = 服务句柄/代理接口的统一抽象（适配不同服务接口）；Invocation = 调用上下文（方法名/服务名/attachments）
- **参考实现**（docs 自答）：**Invoker 本质（docs 自答）**——服务接口暴露的服务句柄、代理接口、统一抽象（适配不同服务接口定义）；**Invocation**——`getMethodName`（服务方法名）/`getServiceName`（服务接口名）/`getAttachments`（**元数据，类似 HTTP Headers**——docs 类比）；**关系**——Invoker 作为代理对象，Invocation 作为调用上下文
- **对比取舍**：**Invoker 统一抽象 vs 逐接口代理**——RPC 框架统一处理（链式包装）vs 直连——stage-2 21 微内核的抽象基石
- **测试佐证**：docs §问题（Invoker/Invocation 自答原文）

### KP-07 RPC 协议设计取舍（数据格式/协议结构/HTTP1.1 vs gRPC vs TCP 私有）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]`（gRPC 数据为 2023 现状） | **置信度**：High
- **前置**：HTTP 协议（09 篇）
- **来源**：docs §RPC 协议的选择（数据交换/协议结构/HTTP1.1/gRPC 优缺点）
- **需求**：掌握 **RPC 协议设计的四性权衡**——通用性/扩展性/性能/穿透性（docs 明确"通用性和高性能通常无法同时达到"）
- **自主实现**：若我设计——协议四要素：数据交换格式（序列化）+ 协议结构（字段布局）+ 传输层选择（TCP 私有/HTTP）；按"跨语言需求 × 治理需求"取舍
- **参考实现**（docs）：**协议内容三部分（docs 明确）**——数据交换格式（序列化方式）+ 协议结构（字段/语义/排列）+ 规则（通信两端一致，否则"鸡同鸭讲"）；**设计考量四性**——通用性（跨语言跨平台）/扩展性（字段升级/元数据）/性能（As fast as it can）/穿透性（网关/代理可识别转发）——**通用性与高性能互斥**；**HTTP/1.1 优缺点（docs）**——优：语义/可扩展/全设备支持（穿透性）；劣：**Request-Response 模型 HOL（队头阻塞，一链路一等待请求）**/人类可读头部（性能差）/无 Server Push（需 Polling/Long-Polling）；**gRPC（docs）**——优：HTTP/2（多路复用/Server Push/流控）+ Protobuf（跨语言二进制）+ 云原生事实标准（k8s/etcd 天然支持）；劣：服务治理基础/强绑 Protobuf（迁移成本）
- **对比取舍**：**TCP 私有协议（Dubbo2 灵活但互通差）vs HTTP 之上（通用但性能/模型受限）vs HTTP/2 之上（gRPC/Triple 取两利）**——docs 的协议演进主线
- **测试佐证**：docs §RPC 协议的选择（四性/HTTP1.1/gRPC 优缺点全文）

### KP-08 Triple 协议（Dubbo3 主力：HTTP/2 + Protobuf + gRPC 兼容 + Streaming）【docs 主要内容②】
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（Dubbo3 当前主力） | **置信度**：High
- **前置**：HTTP/2（09 篇）、KP-07
- **来源**：docs §Triple 协议设计（Objective/Background/Proposal/定义/内容/Streaming/总结）+ dubbo 源码实证
- **需求**：掌握 **Triple 协议的设计**——docs 主要内容第 2 条：Dubbo3 开放协议（解决 Dubbo2 私有协议互通性问题）
- **自主实现**：若我设计——基于 gRPC（HTTP/2）扩展：metadata/payload 分离 + 自定义 header（服务治理元数据）+ 多序列化 + Streaming
- **参考实现**（docs + 源码实证）：**Objective（docs）**——原生与 gRPC 互通/多语言生态（CPP/C#/RUST）/网关友好（无需参与序列化，Ingress 方案）/异步流式；**Dubbo2 问题（docs）**——TCP 私有协议（通用性差）、Header 无扩展（对比 HTTP Ascii Header）、Service Name 在 Body 与 Attachments 冗余、缺协议升级设计；**Proposal（docs）**——兼容 gRPC、HTTP/2 传输层、**metadata 与 payload 分离**（中间设备不解析 payload）、自定义 header 路由（灰度/容灾）、mTLS、Hessian/JSON 多序列化（升级成本≈0——一行协议配置）；**Triple 扩展头（docs）**——`tri-service-version`/`tri-service-group`（Dubbo 服务 version/group——gRPC path 只有 service/method）/`tri-trace-traceid`/`tri-trace-rpcid`（链路追踪 id/span id）/`tri-unit-info`（集群信息——路由）；**Streaming（docs）**——大文件/直播场景：多条用户态长连接 Stream（同 TCP 多 Stream、StreamId 标识、顺序读写）——解决大包拆分传输的排序/延迟问题；**总结（docs）**——分层设计：Protobuf（序列化）+ HTTP/2（传输）+ 开放标准互操作（浏览器/App/IoT）+ 无缝支持 Dubbo3 服务治理；**源码实证**——`dubbo-rpc/dubbo-rpc-triple/.../TripleProtocol.java:62`（class TripleProtocol extends AbstractProtocol——Triple 协议实现存在）
- **对比取舍**：**Triple vs 原生 gRPC**——Triple 补 gRPC 短板（服务治理元数据 header/多序列化/零改造）；**Triple vs Dubbo2 私有协议**——互通性/穿透性 vs 自有生态——**"开放协议"是云原生时代的 RPC 方向**
- **测试佐证**：docs §Triple（Objective/Proposal/内容/Streaming 全文）+ `TripleProtocol.java:62`

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Dubbo 定位与开源路线 | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| 暴露/消费调用链（export/refer） | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 注册中心抽象（Registry/URL/路径） | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| Cluster 消费端链路（Directory/Router/LB） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Filter 责任链（含设计缺陷） | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| Invoker/Invocation 模型 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| RPC 协议设计取舍（四性） | 分布式理论 | 核心 | P1 | 🔴 | 时间无关 | High |
| Triple 协议（HTTP/2+gRPC 兼容+Streaming） | 规范 | 核心 | P1 | 🔴 | 有效 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：Dubbo（`code/spring/dubbo`）+ my-xhs（对照）
- **关键源码**（本次实证）：
  - `dubbo-registry/dubbo-registry-api/.../integration/RegistryProtocol.java:145/272/557`（Protocol 链核心：export/refer）
  - `dubbo-cluster/.../RouterChain.java:120`（route 链）+ `AbstractClusterInvoker.java`
  - `dubbo-rpc/dubbo-rpc-triple/.../TripleProtocol.java:62`（Triple 协议实现）
  - my-xhs `common/loadbalancer/LeastConnectionsLoadBalancer.java:53`（implements ReactorServiceInstanceLoadBalancer——RPC 定制对照）
- **诚实标注**：**my-xhs 无 Dubbo**（pom grep 无依赖，用 OpenFeign HTTP 调用——06 篇已证）——本篇 RPC 知识点用 Dubbo 源码验证 + my-xhs 的 Feign/负载均衡/zone 作对照；docs URL 示例为 Dubbo 2.7.10/zookeeper（版本场景，机制时间无关）；gRPC 数据为 docs 2023 现状；docs §Dubbo Logger（类似 JCL/slf4j 的日志门面适配）`[跳过：日志门面边缘知识点，非 RPC 机制核心（03 跳过标注）]`
- **关联标注**：stage-2 21/22（RPC 微内核/生态——docs 明确关联，Invoker/Protocol/SPI 是同一抽象族）；**stage-2 06（SOFAJRaft——docs 关联内容第 4 条：RPC 状态同步/强一致场景衔接）**；07 篇（注册中心——Dubbo Registry 是消费端落地）；09 篇（HTTP/2——Triple 的传输基础）；06 篇（Feign——my-xhs 实际调用面）

---

## 五、本节小结（三层次视角）

**需求**：RPC 架构升级三主题——gRPC 服务升级、Dubbo Triple 升级、RPC 服务定制（业务权重定制线程/协议/负载均衡）。

**自主实现核心**：若我设计——①RPC 调用链（export/refer + Wrapper 链）②消费端选址链（Directory→Router→LoadBalance）③协议选型（四性权衡：通用性/扩展性/性能/穿透性）④开放协议（HTTP/2 + 多序列化 + 服务治理 header）。

**参考实现**：docs（Dubbo 调用链/Cluster/Triple 全文）+ **Dubbo 源码实证**（RegistryProtocol/RouterChain/TripleProtocol）+ my-xhs 对照（Feign + LeastConnectionsLoadBalancer + zone）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**RPC 架构与协议设计**"——调用链（Wrapper 可插拔）、选址链（路由与均衡分离）、协议四性权衡、开放协议演进（Dubbo2 私有 → gRPC/Triple HTTP/2）；my-xhs 用 Feign（HTTP 栈）对照 Dubbo（RPC 栈）——框架选型是工程决策。

**待验证汇总**：
- Triple 协议在 my-xhs 的迁移评估（当前 Feign——无实证数据）
- gRPC vs REST 在 my-xhs 场景的实测对比（docs 意图无数据）
- Dubbo2 协议头格式细节（docs 关联 D10 proposal 文档）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标（升级动作） | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| ① gRPC 服务升级 | ❌ 未采用（无 gRPC 依赖） | 现状：HTTP 栈（Feign）；gRPC 升级需业务论证（协议互通/多语言场景才值得） |
| ② Dubbo Triple 升级 | ❌ 未采用（无 Dubbo 依赖，pom grep 实证） | 现状：Feign + Nacos + LoadBalancer 已满足；Triple 为可选项（RPC 栈选型是工程决策，06 篇已述） |
| ③ RPC 服务定制（线程/协议/负载均衡） | ✅ **负载均衡定制已落地**：LeastConnectionsLoadBalancer（implements ReactorServiceInstanceLoadBalancer，:53 实证——最小连接数）；zone 区域路由（Router 同类能力，03 篇） | 线程消费定制（隔离舱壁）Sentinel Bulkhead 已落地（03 篇）——三项定制中两项已落地，协议定制 N/A（无 RPC 栈） |
| 服务间调用（对照） | ✅ Feign 59 处 + order 3 客户端（06 篇） | 无 |

**结论**：10 篇的"RPC 定制"在 my-xhs 已有对应落地（负载均衡/舱壁/区域路由）；gRPC/Triple 属**未采用现状**（Feign 栈满足当前需求，升级需业务驱动评估）——不做为差距，标注为"决策待定"。

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Dubbo 架构文档 + Triple 设计提案（2023 时点，Dubbo 2.7.10 示例）；Dubbo 源码实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：RPC 架构升级的完整认知该讲什么

docs 覆盖 Dubbo 调用链与 Triple。完整还该包含：

1. **"RPC 栈 vs HTTP 栈"是架构选型**（docs 主线 + 发散）：Dubbo（性能/治理一体）vs REST/Feign（通用/穿透）——**国内电商/金融大规模选 Dubbo（HSF 同源），互联网通用场景 HTTP 栈**；my-xhs 用 Feign（06 篇）——"升级到 Dubbo"是决策不是必然（docs 主要内容①③的"升级"需按业务权衡）
2. **调用链设计的可插拔性**（docs 调用链 + 发散）：Wrapper 链（QoS/Filter/Listener）+ SPI 扩展——**横切能力零侵入**（stage-2 21 微内核的延续）；Dubbo Filter 的隐式链缺陷（docs 明确）提醒"横切面设计要显式"
3. **协议是 RPC 的核心资产**（docs 四性 + 发散）：**通用性与高性能互斥**——私有协议（性能）vs 开放协议（互通）——云原生时代天平倒向开放（Triple/gRPC）；**穿透性**让网关/Service Mesh 不参与解析（Triple metadata/payload 分离）
4. **HTTP/2 是 RPC 协议的共同底座**（docs + 09 篇衔接）：gRPC/Triple 都选 HTTP/2（多路复用/流控/Server Push）——**09 篇的 HTTP/2 升级与 RPC 协议升级是同一底层演进**
5. **服务治理元数据进协议头**（docs Triple 扩展头 + 发散）：version/group/traceid/unit-info 放 header——**网关/路由/追踪基础设施直接识别**（对比 Dubbo2 Body/Attachments 冗余）——"治理数据协议化"的设计趋势
6. **Streaming 是数据面扩展**（docs + 发散）：大文件/直播/IM 场景（my-xhs 有 IM 模块 `[待验证：my-xhs IM 传输方案]`）——unary 不够时流式（StreamId/顺序读写）
7. **注册表数据模型（URL）的启示**（docs URL 示例 + 发散）：Dubbo 用 URL 承载一切元数据（interface/methods/side/timestamp）——**配置即数据**；对比 Nacos 结构化模型（07 篇）——各有取舍

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| RPC 栈（Dubbo）vs HTTP 栈（Feign） | 性能/治理 vs 通用/穿透（my-xhs 选 Feign） |
| 暴露用 Invoker/订阅用 URL | 1:1 确定 vs 先取集合再合成（docs 自答） |
| Router 与 LoadBalance 分离 | 流量管控可插拔 vs 链长开销 |
| Filter 隐式链 vs 显式链 | 简洁 vs 可读性（docs 缺陷） |
| 私有协议 vs 开放协议 | 性能 vs 互通（云原生选开放） |
| Triple vs 原生 gRPC | 治理元数据/多序列化 vs 标准纯净 |
| unary vs Streaming | 简单 vs 大数据场景（顺序/吞吐） |

### 常见坑/反模式

1. **协议不互通**：Dubbo2 私有协议——跨语言/跨栈调用鸡同鸭讲（docs 三部分定义）
2. **Filter 隐式链失控**：不调 `invoker.invoke()` 链就断——显式契约（docs 缺陷原文）
3. **通用性/性能不取舍**：又要私有协议性能又要全栈互通——四性权衡（docs 明确互斥）
4. **HOL 无感知**：HTTP/1.1 一链路一请求——高并发场景换 HTTP/2（09 篇衔接）
5. **治理数据散落 Body**：Dubbo2 Service Name 冗余——治理元数据应进协议头（Triple header）
6. **RPC 升级无评估**：docs 主要内容"对比 REST 与 gRPC 性能变化"——控制变量评估（02 篇纪律）
7. **无负载均衡定制**：业务权重场景用默认策略——my-xhs LeastConnectionsLoadBalancer 示范定制（docs 主要内容③）

### 生态位置

- **stage-3 教学主线**：容器/服务组（05-10）**收官**——05 容器 → 06 微服务化 → 07/08 注册中心 → 09 HTTP → **10 RPC（本篇）**——**HTTP 栈与 RPC 栈双线闭环**；11/12 转入数据组
- **前后篇衔接**：stage-2 21/22（RPC 微内核/生态——docs 明确关联）→ 本篇（Dubbo 落地调用链）→ 23/24 节（Dubbo 生态——docs 24 节主题）；07 篇（注册中心）→ 本篇 Registry 抽象；09 篇（HTTP/2）→ 本篇 Triple 传输基础
- **与源码提取的关系**：Dubbo（code/spring/dubbo）为核心参考源；my-xhs 对照（Feign/负载均衡/zone）

**架构师视角结论**：本篇以 **Dubbo 源码讲 RPC 机制**（export/refer 包装器链、Directory→Router→LoadBalance 选址链、Registry URL 模型、Filter 责任链与缺陷）、**Triple 讲协议演进**（HTTP/2 开放协议、metadata/payload 分离、治理 header、Streaming）——知识本体是"RPC 架构与协议设计"；my-xhs 用 Feign（HTTP 栈）作对照——"RPC 栈 vs HTTP 栈"是工程选型，容器/服务组（05-10）至此收官，下篇转入数据组（11/12 MySQL 高可用与数据存储）。
