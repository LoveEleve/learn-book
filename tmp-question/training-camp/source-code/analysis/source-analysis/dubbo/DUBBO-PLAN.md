# Dubbo — 知识网络化规划 (D-1~D-11)

> **日期**: 2026-08-16 | **依据**: issue/源码分析执行计划.md 阶段5.2 (7 域) + **09 对既有规划保持怀疑** 重审 (→ 11 域, 详见 §八)
> **源码**: `/data/workspace/source-code/code/spring/dubbo` (3.3.7-SNAPSHOT / 3.3 分支, HEAD c91027d 2026-07-10; 13 主模块 src/main 共 2378 文件)
> **定位**: 阶段5.2 — RPC 与服务治理. 核心 = **SPI 微内核 + URL 总线 (D-1) 之上: 导出/引用 (D-2/D-3) → 调用链 (D-4) → 服务治理 (D-5/D-6/D-7) → 网络与协议深潜 (D-8/D-9/D-10) → 3.x 服务发现升级 (D-11)** — Dubbo 之所以是 Dubbo
> **执行顺序调整**: 执行计划 7 域 (DB-1~DB-7) 全部保留; 审计新增 4 域 (triple/remoting/序列化/元数据) 追加为 D-8~D-11 (remoting 拆 D-8a/D-8b 共 11 域 12 篇), **已完成 D-1/D-2 序号不动**; DB-5/DB-7 分类 🟡→🔴 (定义特征测试重判)
> **知识网络**: 本文含 前置/复用/引出 双链; D-1/D-2 已定稿 (outlines/ + harness/), 其余域按拓扑推进
> **当前进度**: **D-1~D-11 ✅ 全部 11 域定稿 + 深审收敛** (每域两~六轮深审 + harness 5/5~12/12) — **Dubbo 阶段规划完成, 可进入写作阶段**

---

## 一、入口点与主线

Dubbo 无单一用户入口 (框架, 双角色对称) — 两个入口点:

```
Provider: ServiceConfig.export() → doExport → doExportUrls → 1Protocol → exportUrl → doExportUrl → Protocol.export → RegistryProtocol/DubboProtocol (D-2, 已定稿: 6 层链 ServiceConfig:326-993)
Consumer: ReferenceConfig.get() → Protocol.refer → RegistryProtocol.refer → Cluster 容错 (D-3)
调用面:   InvokerInvocationHandler → Filter 链 → ProtocolFilterWrapper → NettyClient (D-4)
```

主线: **配置 (dubbo-config) → URL (dubbo-common URL 总线) → invoker (dubbo-rpc-api) → 传输 (dubbo-remoting) → 治理 (dubbo-registry/dubbo-cluster)**. 旁路: remoting (D-8a/D-8b)/triple (D-9)/serialization (D-10)/metadata (D-11)/qos/metrics (排除, 见 §四).

---

## 二、顶层包扫描 + 包↔域覆盖矩阵 (09 怀疑对象 #1)

### 模块扫描 (src/main 文件数)

| 模块 | 文件 | 模块 | 文件 |
|---|---:|:--|---:|
| dubbo-common | 457 | dubbo-metrics | 156 |
| dubbo-rpc | 371 | dubbo-config | 129 |
| dubbo-plugin | 360 | dubbo-registry | 112 |
| dubbo-remoting | 317 | dubbo-compatible | 93 |
| dubbo-cluster | 195 | dubbo-metadata | 77 |
| dubbo-spring-boot-project | 69 | dubbo-serialization | 31 |
| dubbo-configcenter | 11 | | |

### 包 ↔ 域 覆盖对照表

| 包 (模块/子包) | 文件 | 执行计划域 | 审计后归属 | 状态 |
|---|---|:--|:--|:--|
| dubbo-common/common/extension (22) + deploy (9) + url (15) + config (27) | **73** | DB-1 SPI 微内核 | **D-1** (含 3.x ScopeModel/配置模型面) | ✅ 已覆盖 |
| dubbo-config-api (ServiceConfig/ReferenceConfig) | 44 | DB-2 导出 / DB-3 引用 | **D-2 / D-3** (export(RegisterTypeEnum) L325 / get(boolean check) L230) | ✅/⏳ |
| dubbo-rpc-api (Invoker/Filter/Protocol) | 113 | DB-4 RPC 调用 | **D-4** | ⏳ |
| dubbo-registry-api (+nacos/zookeeper/multicast/multiple) | 112 | DB-5 注册中心 | **D-5** (+Multicast/Multiple/ServiceDiscoveryRegistry) | ⏳ 扩充 |
| dubbo-cluster/loadbalance | 7 | DB-6 负载均衡 | **D-6** (Abstract + 6 算法) | ⏳ 数字修正 |
| dubbo-cluster/cluster | 9 实现 | DB-7 集群容错 | **D-7** | ⏳ 数字修正 |
| dubbo-remoting-api (118) + netty4 (29) | 147 | (DB-2 "Netty 启动" 一笔带过) | **D-8a 传输抽象 + exchange** | ➕ 新增 |
| dubbo-remoting-http12 (h1/h2/message/rest/netty4) | **126** | (未覆盖) | **D-8b HTTP 传输栈** (triple 依赖 106 处 import 实证) | ➕ 新增 |
| dubbo-rpc-triple | 228 | (未覆盖) | **D-9 Triple 协议** (含 REST_ENABLED 面) | ➕ 新增 |
| dubbo-serialization-api + hessian2 + fastjson2 | 31 | (未覆盖) | **D-10 序列化** | ➕ 新增 |
| dubbo-metadata-api + report | 77 | (未覆盖) | **D-11 元数据/服务发现升级** | ➕ 新增 |
| dubbo-plugin/qos | 85 | (未覆盖) | **排除** (运维命令面: 43 命令实现薄壳) | ✂️ |
| dubbo-metrics | 156 | (未覆盖) | **排除** (可观测面, 与阶段6 Micrometer 域重叠) | ✂️ |
| dubbo-plugin/rest-jaxrs (33) + spring (23) + openapi (57) | **113** | (未覆盖) | **排除** (REST 适配, 3.x 已并入 triple REST_ENABLED) | ✂️ |
| dubbo-config-spring (78) / spring-boot-project (69) | 147 | (未覆盖) | **排除** (Spring 集成面) | ✂️ |
| dubbo-compatible | 93 | (未覆盖) | **排除** (2.6 兼容 glue) | ✂️ |
| dubbo-configcenter | 11 | (未覆盖) | **排除** (SPI 适配薄壳) | ✂️ |
| dubbo-native | 26 | (未覆盖) | **排除** (GraalVM native image 环境适配) | ✂️ |
| dubbo-mcp | 17 | (未覆盖) | **排除** (MCP 服务器, 3.3 工具新功能面) | ✂️ |
| dubbo-filter-cache (15) + filter-validation (10) | 25 | (未覆盖) | **排除** (Filter 实现族, D-4 Filter 链的实例, 域内提) | ✂️ |
| dubbo-auth (13) + security 族 (security 7/spring-security 8/spring6-security 11) | 39 | (未覆盖) | **排除** (3.x 认证 SPI, 安全面, D-4 提) | ✂️ |
| dubbo-reactive (12) + mutiny (12) | 24 | (未覆盖) | **排除** (响应式/异步 API 适配, D-4 异步面实例) | ✂️ |
| dubbo-triple-servlet (13) + triple-websocket (6) + remoting-http3 (13) + remoting-websocket (11) + remoting-netty (11) | 54 | (未覆盖) | **排除** (协议/传输变体: servlet/websocket/http3 面, D-8b/D-9 提) | ✂️ |
| remoting-zookeeper-curator5 | 9 | (未覆盖) | **排除** (ZK client 适配薄壳, D-5 内提) | ✂️ |
| dubbo-common/utils (65) + convert (29) + logger (23) + threadpool (22) + 其余 <10 | ~150 | (未覆盖) | **排除** (纯工具/线程池 SPI 支撑面; threadpool 在 D-4/D-8 提) | ✂️ |

---

## 三、重构后域清单 (11 域)

### 第 1 层: 内核 (1 域 🔴)

| # | 域 | 核心主题 | 前置依赖 | 分类置信度 | 状态 |
|:--:|---|---|---|:--:|:--:|
| D-1 | SPI 微内核 | ExtensionLoader 加载/创建/自适应/激活 + Wrapper 织入 (AOP) + 3.x ExtensionDirector/ScopeModel | 无 (一切基础) | 高 (面试高/生产高/框架依赖高) | ✅ 定稿 (harness 10/10, 锚点 ~30) |

### 第 2 层: 服务生命周期 (2 域 🔴)

| # | 域 | 核心主题 | 前置依赖 | 分类置信度 | 状态 |
|:--:|---|---|---|:--:|:--:|
| D-2 | 服务导出 | export 6 层链 (ServiceConfig:325 起, export(RegisterTypeEnum)) / scope 三态 / serverMap 缓存 + reset / EXT_PROTOCOL / 回调双工 | D-1 | 高 | ✅ 定稿 (harness 12/12, 锚点 ~30) |
| D-3 | 服务引用 | ReferenceConfig.get(boolean check) (L230) → RegistryProtocol.refer (L557) → Cluster 容错 / injvm 直连 / 多注册中心 | D-1、D-2 | 高 | ✅ 定稿 (四轮深审, harness 5/5) |

### 第 3 层: 调用链 (1 域 🔴)

| # | 域 | 核心主题 | 前置依赖 | 分类置信度 | 状态 |
|:--:|---|---|---|:--:|:--:|
| D-4 | RPC 调用 | InvokerInvocationHandler → Filter 链 (activate) → ProtocolFilterWrapper → NettyClient / 异步 (AsyncRpcResult) / 上下文 (RpcContext) / 线程池模型 (ThreadPool SPI) | D-1、D-2、D-3 | 高 | ✅ 定稿 (四轮深审, harness 5/5) |

### 第 4 层: 服务治理 (3 域 2🔴+1🟡)

| # | 域 | 核心主题 | 前置依赖 | 分类置信度 | 状态 |
|:--:|---|---|---|:--:|:--:|
| D-5 | 注册中心 | Registry SPI / ZookeeperRegistry / NacosRegistry / Multicast / Multiple / subscribe-notify / 失败重试 (FailbackRegistry) / RegistryProtocol 装配 | D-1、D-2、D-3 | 高 (**🟡→🔴**) | ✅ 定稿 (四轮深审, harness 5/5) |
| D-6 | 负载均衡 | AbstractLoadBalance / Random / RoundRobin / LeastActive / ConsistentHash / ShortestResponse / **Adaptive (3.x)** | D-3 | 中 (面试中/生产高/框架依赖高) | ✅ 定稿 (四轮深审, harness 5/5) |
| D-7 | 集群容错 | Cluster SPI / Failover / Failfast / Failsafe / Failback / Forking / Broadcast / Available / Mergeable / **ZoneAware (3.x)** / MockCluster / ClusterInvoker | D-6 | 高 (**🟡→🔴**) | ✅ 定稿 (四轮深审, harness 5/5) |

### 第 5 层: 网络与协议深潜 (3 域 🟡) ← 审计新增

| # | 域 | 核心主题 | 前置依赖 | 分类置信度 | 状态 |
|:--:|---|---|---|:--:|:--:|
| D-8a | 传输抽象 + exchange | Transporter SPI (Transporters 门面, URL 自适应) / exchange 层 (Request/Response/心跳) / Codec2 编解码 / 连接管理 / netty4 实现 (NettyServer/NettyClient) / telnet | D-1、D-4 | 高 (面试高/生产高/框架依赖高) | ✅ 定稿 (四轮深审, harness 5/5) |
| D-8b | HTTP 传输栈 (http12) | HTTP/1.1 + HTTP/2 消息模型 / h1-h2 编解码 (codec) / netty4 服务器适配 / REST 面 / HttpChannel 抽象 | D-8a | 中 (面试中/生产中/triple 依赖强) | ✅ 定稿 (四轮深审, harness 5/5) |
| D-9 | Triple 协议 | 3.x 默认协议: HTTP/2 + protobuf / gRPC 兼容 (pathResolver) / 流式 (stream) / REST_ENABLED 双协议面 (H2_SETTINGS_REST_ENABLED) / TripleProtocol export/refer | D-8a、D-8b | 中-高 | ✅ 定稿 (四轮深审, harness 5/5) |
| D-10 | 序列化 | Serialization SPI (api 16) / hessian2 / fastjson2 / 选择器 (DefaultSerializationSelector) / optimizeSerialization (SerializationOptimizer, D-2 已挂钩) / protobuf 面 (triple 内) | D-1、D-9 | 中 (面试中/生产高/框架依赖中) | ✅ 定稿 (四轮深审, harness 5/5) |

### 第 6 层: 3.x 服务发现升级 (1 域 🟡) ← 审计新增

| # | 域 | 核心主题 | 前置依赖 | 分类置信度 | 状态 |
|:--:|---|---|---|:--:|:--:|
| D-11 | 元数据/服务发现 | MetadataService (getServiceDefinition/getMetadataInfo) / publishServiceDefinition (D-2 导出时挂钩) / 元数据发布订阅 / ServiceDiscoveryRegistry (应用级服务发现) / 注册中心双面 (接口级 vs 应用级) | D-2、D-5 | 中 (面试中/生产高/框架依赖中) | ✅ 定稿 (四轮深审, harness 5/5) |

> **教学顺序**: D-1 → D-2 → D-3 → D-4 → D-5 → D-6 → D-7 → **D-8a → D-8b → D-9 → D-10 → D-11** (11 域 / D-8 拆 2 篇 → 12 篇)

---

## 四、已排除 (00 §3 thin-wrapper/边缘 — 防"存在=域")

| 包 | 文件 | 原因 |
|---|---:|---|
| dubbo-plugin/qos | 85 | 运维命令面: QosProtocolWrapper (D-1 Wrapper 机制实例) + command/impl **43 个命令薄壳** + 服务器复用 netty; 无框架级新决策; 面试/生产双低. 命令面在 D-4 提 Wrapper 时带过 |
| dubbo-metrics | 156 | 可观测面: MetricsCollector SPI + 聚合 + prometheus/otlp 导出 — 概念与阶段6 Micrometer 域重叠, 训练营在 Micrometer 域覆盖; 无定义特征 |
| dubbo-plugin/rest-* | 113 | REST 适配薄壳: 3.x 已并入 Triple 的 REST_ENABLED 面 (TripleProtocol.java:73 REST_ENABLED=true, H2_SETTINGS_REST_ENABLED); JAX-RS 注解语义属 JAX-RS, dubbo 侧仅适配 |
| dubbo-config-spring | 78 | Spring 集成面: schema 解析/ServiceBean/ReferenceBean 生命周期适配 — Spring 机制在训练营 spring 域覆盖, dubbo 侧是映射薄壳 |
| dubbo-spring-boot-project | 69 | Spring Boot starter 集成面: 自动装配 glue (Boot 域覆盖) |
| dubbo-compatible | 93 | 2.6 兼容层: 废弃 API 的委托封装, 无设计决策 |
| dubbo-configcenter | 11 | 配置中心 SPI 适配 (nacos/apollo/zookeeper): 11 文件薄壳, 无框架级决策 (D-5 注册中心域可带过) |
| dubbo-native | 26 | GraalVM native image 环境适配 (registerReflection 等), 非运行时机制 |
| dubbo-mcp | 17 | MCP (Model Context Protocol) 服务器: 3.3 工具新功能面, 把 dubbo 服务暴露为 MCP tool — 适配面, 训练营不覆盖 |
| dubbo-filter-cache / filter-validation | 25 | Filter 实现族 (服务缓存/参数校验): 有少量设计决策 (缓存键/校验器) 但属 D-4 Filter 链的实例, 域内提即可 |
| dubbo-auth + security 族 | 39 | 3.x 认证 SPI (AccessKey 等) + spring security 适配: 安全面, 面试/生产低, D-4 提 |
| dubbo-reactive / mutiny | 24 | 响应式 API 适配 (Reactive invoker/Mutiny 包装): 薄适配, D-4 异步面实例 |
| dubbo-triple-servlet / triple-websocket / remoting-http3 / remoting-websocket / remoting-netty | 54 | 协议/传输变体: triple-over-servlet / websocket / HTTP/3 / 旧 netty3 — 均为单一传输适配, D-8b/D-9 提 |
| remoting-zookeeper-curator5 | 9 | ZK client 适配 (curator5 封装): 薄壳, D-5 内提 |
| dubbo-common/utils (65) + convert (29) + logger (23) + threadpool (22) + 其余小包 | ~150 | 纯工具面 (utils/convert/logger/stream/json...): 00 §3 明确排除; **threadpool 承载 ThreadPool SPI (D-4/D-8 线程模型面, 域内提)** |

---

## 五、依赖方向 import 证据 (09 怀疑对象 #3)

| 断言 | 正向证据 | 反向 | 结论 |
|---|---|---|:--:|
| rpc 依赖 remoting | dubbo-rpc-api→remoting 8; dubbo-rpc-dubbo→remoting 20; dubbo-rpc-triple→remoting 121 | remoting→rpc 实为 rpc.model (dubbo-common 内, 55 处), 非 dubbo-rpc 模块 | 接受: 传输层在协议层之下 |
| triple 依赖 http12 | dubbo-rpc-triple→http12 106; rest-jaxrs→http12 28; rest-spring→http12 16 | http12→triple 0 | 接受: HTTP 传输栈 (D-8b) 在协议 (D-9) 之下 |
| registry 依赖 cluster | dubbo-registry→rpc.cluster 14 (RegistryProtocol 用 Cluster/ClusterInvoker/Configurator) | dubbo-cluster→registry 0 | 接受: RegistryProtocol 在 registry 模块, 集群机制被注册层消费 |
| config 依赖 registry | dubbo-config→registry 8; config→rpc.cluster 3 | — | 接受: 配置层在治理层之上 |
| registry 依赖 metadata | dubbo-registry→metadata 22 (ServiceDiscoveryRegistry 面) | cluster→metadata 0 | 接受: 元数据域排在 D-5 之后 (D-11) |
| serialization 在底层 | serialization→rpc 12 全为 rpc.model (common), 协议不直接 import serialization (URL 参数动态选择) | — | 接受: 序列化经 SPI 解耦, D-10 教学上排协议后 |

## 六、教学顺序与拓扑 (09 怀疑对象 #4)

**原执行计划顺序**: DB-1 → DB-2 → DB-3 → DB-4 → DB-5 → DB-6 → DB-7 — 无硬性反拓扑 (黑盒先行教学序: DB-3 用 RegistryProtocol/Cluster SPI 黑盒, DB-5/DB-7 再深入实现), **保留原 1-7 顺序, 已完成 D-1/D-2 序号不动**。

**重排后 (11 域 12 篇)**: D-1 → D-2 → D-3 → D-4 → D-5 → D-6 → D-7 → **D-8a → D-8b → D-9 → D-10 → D-11**

前向引用检查 (每域前置依赖序号 < 自身):
- D-8a 依赖 D-1/D-4 ✓ (8 > 4); D-8b 依赖 D-8a ✓ (8b > 8a)
- D-9 依赖 D-8a/D-8b (传输) + D-4 ✓ (9 > 8)
- D-10 依赖 D-1/D-9 ✓ (10 > 9)
- D-11 依赖 D-2/D-5 ✓ (11 > 5)

**插入理由** (为什么新增域放治理层之后): D-4 从协议层看调用链 (止于 NettyClient) → D-5~D-7 治理 → D-8a 从网络层回看同一调用链 (exchange/transport) → D-8b HTTP 传输栈 (http12: triple 的 106 处 import 实证) → D-9 协议实例深潜 → D-10 协议背后的序列化 → D-11 服务发现升级收尾. 与 D-2 先黑盒 (Exchangers.bind) 后深入 (D-8) 的教学序同构.

---

## 七、知识网络双链 (06 跨域交叉引用)

- **前置/复用**: D-1 SPI (全框架基座) → D-2/D-3 消费 URL + Protocol SPI; D-8a 复用 D-1 Transporter SPI; D-8b 复用 D-8a; D-9 复用 D-8a/D-8b 传输 + D-1; D-10 复用 D-1 (Serialization SPI) + D-9; D-11 复用 D-2 (publishServiceDefinition 挂钩) + D-5 (RegistryProtocol)
- **引出**: D-2 引出 D-3/D-4/D-5; D-4 引出 D-8a; D-8a 引出 D-8b; D-8b 引出 D-9; D-9 引出 D-10; D-5 引出 D-11; D-7 收尾后 OUTBOUND → 阶段 5.3 gRPC (G-1 ProtoBuf 序列化 / G-2 服务端 / G-3 客户端 — 与 D-9 Triple 的 gRPC 兼容面对照)
- **对照**: D-1 对照 Java SPI/Spring IoC; D-2/D-9 对照 ZK Netty; D-9 对照 gRPC (HTTP/2); D-8a 对照 Netty (N-1~N-12); D-8b 对照 Netty HTTP codec (N-9); D-5 对照 Nacos (NC 阶段)
- **引回**: 阶段5.1 Feign (F-1~F-5) — HTTP 客户端面 vs Dubbo 自定义协议面

---

## 八、与原始执行计划 (issue/源码分析执行计划.md 阶段5.2) 的差异 — 怀疑审计表 (09 规范)

| 原始断言 | 重审后 | 验证动作 | 证据 | 理由 |
|---|---|---|---|---|
| 7 域 (DB-1~DB-7) | **11 域** (D-1~D-11) | 顶层包扫描 (09 §2 #1) + 设计决策测试 (00 §3, ≥50 文件读关键类) | 未覆盖大包: triple(228)/remoting-api+netty4(147)/**http12(126)**/metadata(77)/serialization(31) 全部通过设计决策测试 (已读 TripleProtocol/Transporters/MetadataService/Serialization SPI; http12 被 triple 依赖 106 处 import 实证) | 修正: 新增 D-8a/D-8b/D-9/D-10/D-11 |
| D-8 传输层覆盖面 (首版 PLAN) | **D-8 拆 2 篇** (D-8a remoting-api+netty4 / D-8b http12) | 二次 REVIEW 重扫 remoting 子模块 | http12 独立 126 文件 (h1/h2/message/rest/netty4), triple→http12 **106 处 import** — 是 HTTP 传输栈, 不能并入 D-8a 一笔带过 | 修正: D-8 拆 2 篇, 域数不变 (11 域 12 篇) |
| DB-6 "Random/RoundRobin/LeastActive/ConsistentHash/ShortestResponse" (5 算法) | **6 算法** (+AdaptiveLoadBalance 3.x) | ls loadbalance/ 穷举 | 7 文件 = Abstract + 6 算法 | 修正: 5→6 |
| DB-7 "Failover/Failfast/Failsafe/Failback/Forking/Broadcast/Available/Mergeable" (8 实现) | **9 实现** (+ZoneAwareCluster 3.x) | find *Cluster*.java 穷举 | 9 实现 + Mock/Scope 装饰器 | 修正: 8→9 |
| DB-5 "Registry—ZookeeperRegistry/NacosRegistry" (2 实现) | **4 本地实现 + ServiceDiscoveryRegistry** (ZK/Nacos/Multicast/Multiple + 应用级) | find *Registry.java 穷举 | AbstractRegistry/FailbackRegistry/CacheableFailbackRegistry 抽象 + 4 实现 + ServiceDiscoveryRegistry | 修正: 补充 3.x 应用级服务发现面 |
| DB-5 分类 🟡 | **🔴** | 定义特征测试 (00 §3.5) | 没有注册中心/集群容错, dubbo 失去服务治理定义特征; 3 信号 面试高/生产高/框架依赖高 | 修正: 🟡→🔴 |
| DB-7 分类 🟡 | **🔴** | 同上 | 同上 | 修正: 🟡→🔴 |
| DB-1 "SPI 微内核" | 保留 (含 3.x 扩展: ExtensionDirector/ScopeModel) | D-1 outline 六轮深审 (review-notes 一~六) | 锚点 ~30 全 grep 验证, 3 加载目录穷举, 修复 18 处 | 接受 (D-1 已定稿) |
| DB-2 "ServiceConfig.export()—Protocol.export—Netty 启动" | 保留 | D-2 outline 四轮深审 | export 6 层链实证 (ServiceConfig:326-993), scope 三态, serverMap 缓存 | 接受 (D-2 已定稿) |
| D-3/D-4 入口锚点 (ReferenceConfig.get / RegistryProtocol.refer) | 保留 | 二次 REVIEW grep 验证 | ReferenceConfig.get(boolean check) L230 / RegistryProtocol.refer L557 / ServiceConfig.export(RegisterTypeEnum) L325 — 3.x 签名带参数, 与执行计划文字一致 | 接受 (锚点真实存在) |

### 二次 REVIEW 修正记录 (2026-08-16, 09 §3: "重审也可能错" — 对首版 PLAN 逐数字重验)

| # | 首版 PLAN 断言 | 复查动作 | 证据 | 结论 |
|:--:|---|---|---|---|
| R1 | 首版数字全量重跑 | 模块文件数 2378 / loadbalance 6 / cluster 9 / registry 4+1 / import 8·20·121·14·8·22·0 / REST_ENABLED 58·73·92 / D-2·D-3 锚点 L325·L230·L557 | 全部吻合 | 通过 |
| R2 | 排除表 "qos 9 命令实现" | find command/impl 穷举 | command/impl **43 个命令类** | 修正: 9→43 (qos 排除理由改 43 薄壳) |
| R2 | 排除表 "rest 141 文件" | 去 target 重数 | openapi 57 + jaxrs 33 + spring 23 = **113** | 修正: 141→113 (首版混入 target 编译产物) |
| R2 | 覆盖矩阵漏 http12 | 重扫 remoting 子模块 + triple import | http12 **126 文件** (h1/h2/message/rest), triple→http12 **106 处 import** | 修正: D-8 拆 D-8a/D-8b, http12 独立成篇 |
| R2 | 覆盖矩阵漏 plugin 小包 | dubbo-plugin 全子模块穷举 | native 26 / mcp 17 / filter-cache 15 / triple-servlet 13 / auth 13 / reactive 12 / mutiny 12 / security 族 26 / filter-validation 10 — 全部过设计决策测试 | 修正: 全部进排除表附理由 |
| R2 | 覆盖矩阵漏 remoting 变体 | remoting 全子模块穷举 | http3 13 / websocket 11 / remoting-netty 11 / zookeeper-curator5 9 | 修正: 进排除表 (D-8b/D-9 提) |
| R2 | 覆盖矩阵 "extension 等 ~100" | dubbo-common 子包穷举 | extension 22 + deploy 9 + url 15 + config 27 = **73**; utils 65 / convert 29 / logger 23 / threadpool 22 为工具面 | 修正: ~100→73, 工具面进排除表 |
| R3 | 依赖方向全量重跑 | 7 条 import 计数 | 全部吻合 (triple→http12 106 为新证) | 通过 |
| R4 | 反写测试 + 结构 | 只读 PLAN 能否开工 D-3~D-11 | 入口/主线/锚点/排除/置信度列齐全 | 通过 |

**覆盖率报告 (00 §第九步)**: 重审 11 域 vs 执行计划 7 域 = **157%**。
差距解释: ①triple/http12/remoting/serialization/metadata 为 ≥50 文件的承载设计决策包, 执行计划遗漏 (方法 00 顶层包扫描 + 设计决策测试抓出) ②DB-5/DB-7 分类升级 (定义特征测试重判) ③执行计划无多余域 — 全部 7 域保留。
**注意**: 本对照比 M-PLAN §八 的教训更进一步 — 除了域数量对照, 本次对每个执行计划域的数字断言 (算法数/实现数/实现清单) 逐条穷举 (09 §2 #2), 发现 3 处数字错误, 全部修正; 首版 PLAN 自身再经二次 REVIEW 修正 6 处 (R2 系列), 符合 09 §3 "重审结论也必须再过一遍"。

## 九、完成检查单 (09 + 00 §8)

- [x] 顶层包扫描 ↔ 域清单对照表已建, ≥10 文件未覆盖包全部过设计决策测试 (§二)
- [x] 规划中全部数字断言已穷举验证 (loadbalance 6 / cluster 9 / registry 4+1 / 模块文件数 2378 / qos 43 / rest 113 / http12 126) (§八)
- [x] 依赖方向已用正反双向 import 证据验证 (9 条) (§五)
- [x] 拓扑重排已跑, 与规划顺序差异逐条记录理由 (§六)
- [x] 新增域已过 00 §3 + §3.5 测试 (§三, 附证据 + 置信度)
- [x] 二次 REVIEW (09 §3): 首版 PLAN 逐数字重验, 修正 6 处 (§八 二次修正记录)
- [x] 怀疑审计表已写入 §八
- [ ] 偏差已同步 HANDOFF (待写 HANDOFF-DUBBO.md 时同步)
