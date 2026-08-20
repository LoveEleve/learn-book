# HANDOFF — gRPC-Java 源码分析交接文档 (详细版)

> **日期**: 2026-08-16 | **版本**: gRPC-Java 1.83.1 (build.gradle:24 "CURRENT_GRPC_VERSION" 实证)
> **给新 AI**: 本文是 gRPC 阶段的**唯一入口**。本会话已完成 Pass 0 (上下文/模块扫描/09 审计初稿/关键锚点验证) + **2026-08-16 二次深度重审** (GRPC-PLAN.md: 行数穷举/全模块扫描/依赖矩阵/未覆盖包关键类源码级设计决策测试 — **新增 G-7 xDS + G-8 RLS, 6→8 域**), **尚未开始任何域的正式交付** (KP/大纲/harness 均未写)。请按 §五 方法论逐域推进。
> **源码**: `/data/workspace/source-code/code/spring/grpc-java` (git 浅克隆单提交 8f621c0)
> **交接背景**: 本仓库由本会话接手 (此前 Feign 阶段已 6/6 收官)。**注意**: Dubbo 阶段已确认由另一 AI 负责, 本会话产物已全部作废删除 — 开工前先确认 gRPC 无其他 AI 并行, 避免再次撞车。

---

## 〇、三十秒总览

- gRPC-Java 1.83.1, 官方 Java 实现 (HTTP/2 + protobuf 的 RPC 框架)
- 核心模块: **api (134 文件, 接口面)** + **core (135 文件, 实现面)** + **stub (18 文件, 客户端三种形态)** + **netty (67 文件, HTTP/2 传输)** + **protobuf (6 文件)** + **services (27 文件, 健康检查/反射)** + **util (19 文件, MultiChildLoadBalancer/RoundRobin 等)** + **xds (184 文件, xDS 控制面与高级 LB — 全仓库最大模块)** + **rls (16 文件, RLS 数据面路由)** + context (在 api/src/context, Context 传播) + compiler (C++ protoc 插件, 12 文件, 生成 Java 代码)
- 执行计划 6 域 (G-1~G-6) 经 09 审计**基本接受** + 补充 4 处 (§二); **2026-08-16 二次深度重审: 新增 G-7 (xDS) + G-8 (RLS), 共 8 域** (见 GRPC-PLAN.md)
- **尚未开始任何域交付** — 从 G-1 或按 §三 拓扑逐域开工

---

## 一、项目全貌

### 1.1 版本与来源

| 项 | 值 | 证据 |
|---|---|---|
| 版本 | **1.83.1** | build.gradle:24 `version = "1.83.1" // CURRENT_GRPC_VERSION` |
| git | 浅克隆单提交 (8f621c0) | `git rev-parse --is-shallow-repository` = true |
| 构建 | Gradle (build.gradle) + Bazel (BUILD.bazel) | — |
| 历史 | 无 git 历史 — 时空溯源用 CHANGELOG.md / 代码内 @since 注释 | — |

### 1.2 模块规模 (src/main/java 文件数)

| 模块 | 文件数 | 角色 |
|---|---|---|
| api | 134 | 公共接口: Channel/ClientCall/ServerCall/Status/Metadata/NameResolver/LoadBalancer/ServerInterceptor/ClientInterceptor |
| core | 135 | 实现: ManagedChannelImpl (2200)/ServerImpl (980)/ClientCallImpl (784)/RetriableStream (1618)/InternalSubchannel (897)/DnsNameResolver (709) |
| stub | 18 | 客户端三形态 (Blocking/Async/Future) + ClientCalls (997)/ServerCalls (506) |
| netty | 67 | HTTP/2 传输: NettyServer (494)/NettyClientTransport (477)/GrpcHttp2ConnectionHandler |
| protobuf | 6 | ProtoUtils/StatusProto/Descriptor 供应商 |
| services | 27 | HealthStatusManager/ChannelzService/反射 |
| context | (api/src/context) | Context.java + Deadline.java |
| compiler | 12 (C++) | protoc 插件 java_generator.cpp/java_plugin.cpp — 生成 gRPC Stub 代码 |

### 1.3 关键文件行数速查

| 文件 | 行数 | 所属域 |
|---|---|---|
| core/internal/ManagedChannelImpl | 2200 | G-3 |
| core/internal/RetriableStream | 1618 | G-6 |
| api/LoadBalancer | 1626 | G-4 |
| api/NameResolver | 1040 | G-5 |
| core/internal/ServerImpl | 980 | G-2 |
| core/internal/ClientCallImpl | 784 | G-3 |
| core/internal/InternalSubchannel | 897 | G-4 |
| core/internal/DnsNameResolver | 709 | G-5 |
| api/Status | 672 | 支撑 (各域) |
| api/Metadata | 1051 | 支撑 (各域) |
| netty/NettyServer | 494 | G-2 |
| netty/NettyClientTransport | 477 | G-3 |
| core/internal/ServerCallImpl | 399 | G-2 |
| stub/ClientCalls | 997 | G-1/G-3 |
| stub/ServerCalls | 506 | G-1/G-2 |
| core/internal/RetryPolicy | 96 | G-6 |
| core/internal/HedgingPolicy | (存在) | G-6 |

---

## 二、09 怀疑审计表 (Pass 0 初稿 — 每域开工前仍需复核)

| 既有规划断言 (执行计划 G-1~G-6) | 验证动作 (本会话) | 证据 | 结论 |
|---|---|---|---|
| 域清单 6 个 | 模块扫描 | api (134) 接口面 + core (135) 实现面 + stub (18) + netty (67) + protobuf (6) — 6 面全覆盖 | **接受** ✅ |
| G-1 "ProtoBuf 序列化: .proto→protoc→Stub" | protobuf/ + compiler/ + stub/ | protobuf 模块 6 文件 (ProtoUtils/StatusProto); **compiler 为 C++ protoc 插件** (java_generator.cpp 等 12 文件); stub 生成面 = ClientCalls (997)/ServerCalls (506) | **接受+补充** ✅ (注意 compiler 是 C++, 非 Java 域 — 大纲用对照方式覆盖) |
| G-2 "ServerBuilder→NettyServer→ServerCallHandler→ServerInterceptor" | 模块扫描 | netty/NettyServerBuilder (72)/NettyServer (494); core/ServerImpl (980); core/ServerCallImpl (399); api/ServerCall (269) + ServerInterceptor | **接受** ✅ |
| G-3 "ManagedChannel→ClientCall→StreamObserver→Deadline" | 模块扫描 | core/ManagedChannelImpl (2200); core/ClientCallImpl (784); stub 三形态 (Blocking 70/Async 69/Future 70); **Deadline 在 api/src/context** (非 api/main — 特殊源码目录) | **接受+补充** ✅ (Deadline 位置修正) |
| G-4 "LoadBalancer/Subchannel—PickFirst/RoundRobin/WeightedRoundRobin/优先同Zone" | 模块扫描 | api/LoadBalancer (1626); **核心只内置 PickFirst**: core/internal/PickFirstLoadBalancer + PickFirstLeafLoadBalancer + AutoConfiguredLoadBalancerFactory (218); **RoundRobinLoadBalancer 在 util 模块** (util/src/main/java/io/grpc/util/, 非 core); **WeightedRoundRobinLoadBalancer 在 xds 模块** (xds/src/main/java/io/grpc/xds/) — gRPC-Java 的模块化结构: 核心最小化, 策略外置 | **接受+修正** ✅ (执行计划未指明模块; RoundRobin 归属 util 是重要事实) |
| G-5 "NameResolver/DnsNameResolver" | 模块扫描 | api/NameResolver (1040); core/internal/DnsNameResolver (709) + DnsNameResolverProvider | **接受** ✅ |
| G-6 "RetryPolicy/HedgingPolicy—指数退避/对冲请求" | 模块扫描 | core/internal/RetryPolicy (96)/HedgingPolicy/RetriableStream (1618)/BackoffPolicyRetryScheduler; 测试 HedgingPolicyTest/BackoffPolicyRetrySchedulerTest 存在 | **接受** ✅ |
| 版本 | build.gradle:24 | **1.83.1** | **补充** ✅ |
| context 模块 (未单列域) | 设计决策测试 | Context.java 在 api/src/context (跨线程传播/取消传播) — 设计决策但为**贯穿面** | **并入 G-3** ✅ (调用上下文) 或大纲对照引用 |
| services (27 文件) | 设计决策测试 | 健康检查/反射/Channelz — 应用面服务 | **排除或对照** ✅ (非核心协议) |
| netty (67) | 设计决策测试 | HTTP/2 传输实现 — 承载设计决策但为 G-2/G-3 的传输层 | **并入 G-2/G-3** ✅ |
| census/opentelemetry (可观测) | 设计决策测试 | 可观测面, 与阶段6 对照 | **排除** ✅ |

**覆盖率报告**: 既有规划 6 域 → 重审后 **6 域** (100%), 补充 **4 处** (compiler 为 C++/Deadline 位置/context 并入 G-3/RoundRobin 位置待验证), 排除 **2 处** (services/census)。

> **⚠ 2026-08-16 二次深度重审修正 (详见 GRPC-PLAN.md, v1→v5 终版)**: 初稿**遗漏 xds 模块 (184 文件, 全仓库最大)** → **新增 G-7**; v2 读关键类后 **rls 升为独立域 G-8**; v3 反向扫描补 9 核心类 + 锚点验证; v4 core/netty 全量文件级测试 (压缩/线程/保活/代理/时钟/UDS 解析器/G-6 双模式); **v5 api 134 文件全量归类** (无遗漏, 补 SynchronizationContext/ProxyDetector 接口族/ClientTransportFilter/ServerTransportFilter)、xds internal 全归 G-7 (matcher 22 含 CEL)、入口链路验证 (Grpc L111→Registry→Builder)、**NettyServerBuilder "72" 真相 = 类声明 L72 (文件 800 行)**。修正汇总: NettyServerBuilder 800 行; compiler 核心 3 个 C++ 文件; services HealthCheckingLoadBalancerFactory (511) 入 G-4 对照。

---

## 三、域清单与拓扑建议

### 3.1 域清单 (6 域: 3🔴 + 3🟡, 按执行计划定级)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| G-1 | **ProtoBuf 与 Stub 生成** | protobuf/ (6) + stub/ (ClientCalls 997/ServerCalls 506/三形态) + compiler (C++ 对照) | .proto→protoc→Stub/调用分派/三形态语义 | 🔴 A |
| G-2 | **服务端** | core/ServerImpl (980) + ServerCallImpl (399) + netty/NettyServer (494) + NettyServerBuilder (**800**) + api/ServerCall (269) | ServerBuilder 装配/请求接收/调用处理/拦截器/HTTP2 服务端 | 🔴 A |
| G-3 | **客户端** | core/ManagedChannelImpl (2200) + ClientCallImpl (784) + stub 三形态 + netty/NettyClientTransport (477) + context (Context/Deadline) | 通道生命周期/调用执行/状态机/Deadline/取消传播 | 🔴 A |
| G-4 | **负载均衡 (核心+util)** | api/LoadBalancer (1626) + core/internal (PickFirstLoadBalancer/PickFirstLeafLoadBalancer/AutoConfiguredLoadBalancerFactory/InternalSubchannel 897) + **util 全家族** (RoundRobin 182/**MultiChild 414**/OutlierDetection/RandomSubsetting/GracefulSwitch) | Subchannel 抽象/Picker/状态管理/内置策略族/MultiChild 基类 | 🟡 B |
| G-5 | **命名解析** | api/NameResolver (1040) + core/internal/DnsNameResolver (709) | 解析器生命周期/Listener/地址更新/Attributes | 🟡 B |
| G-6 | **流控与重试** | core/internal/RetryPolicy (96) + HedgingPolicy (72) + RetriableStream (1618) + BackoffPolicyRetryScheduler + ClientCallImpl 重试集成 | Retry/Hedging 策略语义/缓冲/重放/对冲 | 🟡 B |
| G-7 | **xDS 控制面与高级 LB** (v1 新增) | xds/ (184: XdsNameResolver 1206/XdsClientImpl+client 20/RingHash 575/LeastRequest 348/WRR 903/WrrLocality/Cds 651/ClusterImpl 571/Priority 368) + grpclb (14: GrpclbState 1281) 对照 | 控制面协议/一致性哈希/加权轮询/同 Zone/过滤器链 | 🟡 B |
| G-8 | **RLS 数据面路由** (v2 新增) | rls/ (16: CachingRlsLbClient 1104/LbPolicyConfiguration 475/AdaptiveThrottler 343/LinkedHashLruCache 329/RlsLoadBalancer) + xds 反射集成对照 | 服务端决策路由/LRU 缓存/自适应节流/退避缓存 | 🟡 B |

### 3.2 拓扑建议 (依赖序)

**G-1 → G-2 → G-3 → G-6 → G-5 → G-4 → G-7 → G-8**

> 或按执行计划 G-1→G-2→G-3→G-4→G-5→G-6。拓扑理由: Stub/序列化 (G-1, 一切调用的载体) → 服务端 (G-2) → 客户端 (G-3, 核心) → 流控重试 (G-6, ClientCallImpl 内联) → 命名解析 (G-5, 地址来源, 依赖 G-6 退避) → 负载均衡 (G-4, 地址消费) → **xDS (G-7, 最上层 — import 实证: xds→core 41/util 15/netty 7/services 7; 且依赖 G-4 的 util MultiChild 基类与 G-5 的解析面)** → **RLS (G-8, rls→core 4/util 3/stub 1, 数据面路由, xds 反射集成)**。**注意**: G-4/G-5 相互引用 (NameResolver 结果喂 LoadBalancer), 顺序可调, 开工时用 00 §4 复核。

---

## 四、每域 Pass 0 已验锚点速查 (写作/大纲时作为起点, 必须重新 grep 验证)

### G-1 ProtoBuf 与 Stub
- ProtoUtils.java / StatusProto.java (protobuf/src/main/java/io/grpc/protobuf/)
- ClientCalls.java:997 行 (**blockingUnaryCall L140/asyncUnaryCall L81**) (Blocking/Async/Future 调用分派)
- ServerCalls.java:506 行 (**asyncUnaryCall L49**) (服务端 Unary/ServerStreaming/ClientStreaming/Bidi 分派)
- AbstractBlockingStub (70)/AbstractAsyncStub (69)/AbstractFutureStub (70) (**均在 stub 模块**)
- **压缩/消息帧面 (v4)**: api/Codec.java 内部类 **Gzip (L35**, JDK GZIP 流) + core/MessageFramer (45) + GzipInflatingBuffer (38)
- compiler/src/java_plugin/cpp/java_generator.cpp (**核心 3 个 C++ 文件**: java_generator.cpp/h + java_plugin.cpp)
- **✅ G-1 已交付 + 已深审 (2026-08-16)**: KP (8 机制) + outline (8 节, **锚点 55 个**) + completeness-questions (32 问) + review-notes (**两轮审查: 锚点密度 7→55 / Codec.GZIP→new Codec.Gzip() / 替代方案 4 处 / grpc-encoding 协商头**) + temporal-trace (@since 溯源) + harness MiniG1 (5/5 通过)

### G-2 服务端
- core/internal/ServerImpl.java:980 (服务端生命周期/注册服务/请求分派)
- core/internal/ServerCallImpl.java:399
- **netty/NettyServerHandler.java:1294 (HTTP/2 服务端帧处理 — FrameListener L940/KeepAlivePinger L1030, v3 补)**
- **netty/ProtocolNegotiators.java:1249 (h2/h2c/TLS/Proxy 协商, v3 补)**
- netty/NettyServerBuilder.java (**800 行, 类声明 L72** — 原 HANDOFF "72" 真相是类声明行, v5 查明) / NettyServer.java:494
- api/ServerCall.java:269 / ServerInterceptor (api) / ServerTransportFilter/ServerCallExecutorSupplier (v5)
- 入口链路: ServerBuilder.build 抽象 (L436) → NettyServerBuilder.build → NettyServer

### G-3 客户端
- core/internal/ManagedChannelImpl.java:2200 (通道生命周期/NameResolver 集成/LoadBalancer 装配; **newCall L809**)
- **core/internal/ManagedChannelImplBuilder.java:1071 (通道装配: NameResolver/LB/传输工厂, v3 补)**
- core/internal/ClientCallImpl.java:784
- **core/internal/DelayedClientCall.java:642 + DelayedStream.java:551 (就绪前缓冲, v3 补)**
- **core/internal/MessageDeframer.java:550 (gRPC 消息帧: 1B 压缩标志+4B 长度, v3 补)**
- **线程模型/连接生命周期/代理 (v4/v5)**: SerializingExecutor (36)/**SynchronizationContext (api, v5)**/KeepAliveManager (34)/MaxConnectionIdleManager/**ProxyDetectorImpl (48) + api ProxyDetector 接口族/HttpConnectProxiedSocketAddress (v5)**/TimeProvider (23)/SecurityLevel/ClientTransportFilter (v5)
- netty/NettyClientTransport.java:477 + **NettyClientHandler.java:1186 + NettyChannelBuilder.java:928 (v3 补, 类声明 L80)** + **WriteQueue/SendGrpcFrameCommand 命令写路径族 (v4)**
- 入口链路: Grpc.newChannelBuilder (L111) → ManagedChannelRegistry → NettyChannelBuilder → 传输工厂 → ManagedChannelImpl (v5)
- api/src/context/java/io/grpc/Context.java:1124 + Deadline.java:288 + **PersistentHashArrayMappedTrie.java:301 (Context 快照, v3 补)** (**注意: 在 api/src/context 而非 api/src/main**)
- stub 三形态

### G-4 负载均衡
- api/LoadBalancer.java:1626 (**SubchannelPicker L453/Helper L1036**)
- core/internal/PickFirstLoadBalancer.java (**202**, 类声明于 **L40** ✅已验证) + PickFirstLeafLoadBalancer (**915**) + AutoConfiguredLoadBalancerFactory (218)
- core/internal/InternalSubchannel.java:897
- **util 模块全家族** (util/src/main/java/io/grpc/util/): RoundRobinLoadBalancer (182) + **MultiChildLoadBalancer (414, xds 多子策略公共基类)** + **OutlierDetectionLoadBalancer (1165)** + RandomSubsettingLoadBalancer + GracefulSwitchLoadBalancer
- services 对照: **HealthCheckingLoadBalancerFactory (511, LoadBalancer.Factory 健康检查包装)**
- **WeightedRoundRobinLoadBalancer (903) 在 xds 模块** (xds/src/main/java/io/grpc/xds/) — G-7
- LoadBalancerRegistry (api)/LoadBalancerProvider (api) — 注册机制

### G-5 命名解析
- api/NameResolver.java:1040
- **api/Uri.java:1184 (gRPC target 解析入口, v3 补)**
- core/internal/DnsNameResolver.java:709 (**类声明 L65**) + DnsNameResolverProvider
- **netty/UdsNameResolver.java + UdsNameResolverProvider (第二解析器实现: Unix Domain Socket, v4 补)**
- core/internal/RetryingNameResolver (v4)
- 测试: DnsNameResolverTest/ManagedChannelImplGetNameResolverTest

### G-6 流控与重试
- core/internal/RetryPolicy.java:96 (**字段 L33-36: maxAttempts/initialBackoffNanos/maxBackoffNanos/backoffMultiplier/retryableStatusCodes**) / HedgingPolicy (**72, 字段 L30-33: maxAttempts/hedgingDelayNanos/nonFatalStatusCodes**) / RetriableStream.java:1618 (**双模式: retryPolicy+hedgingPolicy 互斥 L146-147, isHedging L148, scheduledHedging L122**)
- BackoffPolicyRetryScheduler + ExponentialBackoffPolicy (v4) + JsonParser/JsonUtil (service config 策略来源, v4) + TimeProvider (时钟抽象, v4)
- 测试: HedgingPolicyTest/BackoffPolicyRetrySchedulerTest/RetriableStreamTest/RetryPolicyTest (已验证存在)

### G-7 xDS 控制面与高级 LB (v1 新增, v3 扩充)
- xds/src/main/java/io/grpc/xds/: XdsNameResolver (**1206**, 比 DnsNameResolver 709 大) + WeightedRoundRobinLoadBalancer (**903**) + RingHashLoadBalancer (**575**) + LeastRequestLoadBalancer (348) + PriorityLoadBalancer (368) + CdsLoadBalancer2 (651) + ClusterImplLoadBalancer (571) + ClusterManagerLoadBalancer (263) + WrrLocalityLoadBalancer (**L44, 同 Zone/权重语义 L41/L68-74 实证**)
- xds/src/main/java/io/grpc/xds/client/ (20): **XdsClientImpl (1102)**/ControlPlaneClient (593)/LoadReportClient/LoadStatsManager2
- **xds internal 子包 (v5 判定, 全部归 G-7)**: matcher/ **22 文件路由匹配引擎** (Matcher/MatcherTree/MatcherList/UnifiedMatcher + **CelMatcher/CelEnvironment CEL 匹配**) + headermutations/ 8 (HeaderMutationFilter) + security/ 30 + certprovider/ 10 + extproc/ 6 + extauthz/ 2 + rbac/engine
- **服务端面 (v3 补)**: XdsServerWrapper (960, LDS/RDS)/XdsDependencyManager (947)/XdsClusterResource (814)
- **过滤器面 (v3 补)**: ExternalProcessorClientInterceptor (**1355**)/GrpcAuthorizationEngine (504)/OrcaOobUtil (632)
- 测试: **xds 共 100 测试** (XdsClientImplV3Test/WeightedRoundRobinLoadBalancerTest/RingHashLoadBalancerTest/LeastRequestLoadBalancerTest)
- 对照: grpclb (14: **GrpclbState 1281 行**, 旧控制面 LB 协议, xDS 前身 — 时空溯源材料)

### G-8 RLS 数据面路由 (v2 新增)
- rls/src/main/java/io/grpc/rls/: **CachingRlsLbClient (1104 行, "Every single request is routed by the server's decision... LruCache is used" L84-85)** + AdaptiveThrottler (343) + LinkedHashLruCache (329) + LbPolicyConfiguration (475) + RlsProtoData (253) + RlsLoadBalancer + RlsLoadBalancerProvider (经 LoadBalancerRegistry SPI 注册)
- 与 xds 集成: RouteLookupServiceClusterSpecifierPlugin 反射加载 grpc-rls (xds:61 "Dependency for 'io.grpc:grpc-rls' is missing"); rls 不依赖 xds (0 引用)
- 依赖: rls→core 4/util 3/stub 1
- 测试: (rls/src/test 待验证)

---

## 五、方法论要求 (逐域必走, 不可批量)

### 5.1 核心管线 (每域)

```
Pass 0 读上下文 (README/文档/测试地图) → Pass 1 扫轮廓 (≥5 真问题/读 2 测试)
→ Pass 2 闭环 (假设→grep 验证→结论, 每闭环写文件, ≥3 闭环)
→ Pass 3 产出 KP + 大纲 (四要素: 场景→技术描述(file:line)→关键设计→跨层)
→ completeness-questions (5 身份, ≥20 问)
→ 六层深审 (锚点回源, 零发现=不合格) + 时空溯源 (🔴 域, 用 CHANGELOG/代码痕迹)
→ harness (🔴 A 强制: 极简复现 ~300 行纯逻辑, 如 MiniChannel/MiniRetry)
→ 更新 HANDOFF + 用户确认后再下一个域
```

### 5.2 关键规范速查

- **锚点密度**: 🔴 ≥8 / 🟡 ≥4 (outline.md 中 `File.java:NNN`)
- **负面空间**: 每域 5-6 条"不做 X" (与对照框架形成差异)
- **桥链**: 每大纲结尾 "→ 引出: 下一域"; 前置只声明拓扑前位域
- **数字**: 任何数字 grep 穷举 (如 LoadBalancer 内置策略数/RetryPolicy 参数默认值)
- **代码块**: 逐字来自源码或标 [伪代码]; 行号写作时重新 grep (大纲是线索不是事实)

### 5.3 五维检查表 (每篇必过)

| 维度 | 检查项 |
|---|---|
| 场景 | 每节开头有场景句 |
| 源码 | 每个机制 file:line + 函数名, 全部 grep 验证 |
| 关键设计 | 每节有"关键设计"解释 why |
| 跨层 | [HTTP/2:][TCP:][序列化:][并发:] 标注 |
| 悬念 | 结尾"核心悬念" + OUTBOUND 桥 |

---

## 六、教训 (本会话三次复现, 必读)

1. **探索代理超范围写文件 ×3**: Curator/Feign/Dubbo 阶段, explore 子代理多次在 outlines/harness 目录写入幽灵文件 (f3-client-execution/f5-uri-template/d1-spi-kernel/d2-service-export 等), 造成重复交付与数字污染。**对策**: ① 派探索代理时 prompt 首行声明 "严禁写任何文件, 只返回文本" (已做但无效); ② **每次探索后立即 `ls -la outlines/ harness/` diff 检查**; ③ 发现幽灵目录立即删除并修正数字。
2. **分工确认**: Dubbo 阶段因未确认分工与另一 AI 撞车 (我做完 7 域后用户告知另一 AI 负责) — **开工任何新仓库前, 先确认无其他 AI 并行**。
3. **域清单完整性**: 09 §1.3 "错误率随仓库规模上升" — gRPC 的 api/core 均为 130+ 文件大包, 每域开工时对未覆盖包 (如 core/internal 下 80+ 文件) 必须过设计决策测试。

---

## 七、环境与运行

- **JDK**: Java 21 (环境实测)
- **构建**: Gradle (build.gradle), 但 harness 应坚持**纯 JDK 极简复现** (javac/java, 无依赖) — 与 Curator/SofaJRaft/Feign 阶段一致
- **测试参考**: core/src/test 80 个测试 (DnsNameResolverTest/ManagedChannelImplGetNameResolverTest/HedgingPolicyTest/BackoffPolicyRetrySchedulerTest/AutoConfiguredLoadBalancerFactoryTest 等)
- **输出目录**: `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/grpc/`
  - knowledge-planning/ (G-1~G-6 每域 1 个 KP)
  - outlines/G-N-域名/ (outline.md + completeness-questions.md + review-notes.md + temporal-trace.md)
  - harness/G-N-xxx/ (MiniXxx.java)
  - GRPC-PLAN.md (09 审计表 — **尚未创建**, 开工第一件事)
  - HANDOFF-GRPC.md (本文, 每域完成回填)

---

## 八、完成检查单 (每域开工前)

- [ ] 已确认 gRPC 无其他 AI 并行
- [x] **已建 GRPC-PLAN.md** (2026-08-16 二次深度重审 v1→v5 终版: 行数穷举/依赖矩阵/未覆盖包关键类测试 → **G-7 xDS + G-8 RLS, 共 8 域**; v3 反向扫描补 9 核心类; v4 core/netty 全量文件级测试; **v5 api 134 全量归类 + xds internal 判定 + 入口链路验证 — 全部 30 模块文件级覆盖或排除完毕, 无未判定文件**)
- [x] **已验证**: RoundRobin 在 util / WeightedRoundRobin 在 xds / Deadline+Context 在 api/src/context (+PHAMT 301) / compiler 为 C++ 插件 (核心 3 文件) / RetriableStreamTest+RetryPolicyTest 存在 / **util 有 MultiChildLoadBalancer (414, xds 基类)** / **NettyServerBuilder 800 行且类声明在 L72 (原 72 实为类声明行)** / **rls CachingRlsLbClient (1104) 独立设计决策** / **services HealthCheckingLoadBalancerFactory (511) 入 G-4 对照** / **锚点行号: PickFirst L40/blockingUnaryCall L140/ServerCalls asyncUnaryCall L49/newCall L809/RetriableStream L55/SubchannelPicker L453/Helper L1036/DnsNameResolver L65/WrrLocality L44** / **G-5 第二解析器 netty/UdsNameResolver** / **G-6 RetriableStream 双模式互斥 (L146-147)** / **压缩面 new Codec.Gzip() (CompressorRegistry.java:34)** / **入口链路 Grpc L111→Registry→Builder**
- [ ] 每域按 §五 管线逐域交付 (一次一域, 不批量)
- [ ] 每次探索后检查幽灵文件
- [ ] 每域完成回填本文 §四 锚点速查 (替换为已验证版本)
- [x] **G-1 完成** (2026-08-16): KP + outline (8 节, 锚点 55) + 32 问 + 深审 (两轮: Codec.GZIP→new Codec.Gzip()/锚点密度 7→55) + 时空溯源 + harness 5/5
- [x] **G-2 完成 + 已深审** (2026-08-16): KP (9 机制) + outline (9 节, 锚点 22, 每节含被放弃方案) + 32 问 + review-notes (两轮: ServerInterceptors 锚点修正/跨域 2/9→9/9/补 maxConcurrentCalls) + temporal-trace + harness MiniG2 8/8
- [x] **G-3 完成 + 已深审** (2026-08-16): KP (8 机制) + outline (8 节, 锚点 12) + 42 问 (6 身份) + review-notes (两轮: 行内引用 3 处修正 Context:169→180 等/补 MessageDeframer 落点) + temporal-trace + harness MiniG3 5/5 (syncContext 可重入/懒启动/缓冲放行/Context 短路)
- [x] **G-6 完成 + 已深审** (2026-08-16): KP (6 机制) + outline (6 节) + 30 问 + review-notes (两轮: L191+L509 行号修正/缓冲默认/必填状态码集/transparent retry 细化/harness 实测抖动超封顶) + temporal-trace + harness MiniG6 5/5
- [x] **G-4 完成 + 已深审** (2026-08-16): KP (6 机制) + outline (6 节, 锚点 9) + 30 问 (30/30 全 ✅ 零补强) + review-notes (两轮: 行内引用 3 处修正 syncContext L58-63/RoundRobin L55-72/OutlierDetection L837-838, 补 HealthChecking 对照) + temporal-trace + harness MiniG4 5/5 (Picker 每 RPC 选址/RoundRobin READY 轮询/PickFirst 逐地址+全败退避)
- [x] **G-5 完成 + 已深审** (2026-08-16): KP (4 机制) + outline (4 节, 引用内容回源 10/10 零修正) + 28 问 + review-notes (两轮: A2 注释 L462-465 修正/补 UdsNameResolver 对照) + temporal-trace + harness MiniG5 4/4 (Uri 解析含 dns:/// 空 authority 语义实测修正/拉取生命周期/失败退避重试)
- [x] **G-7 完成 + 已深审** (2026-08-16): KP (5 机制) + outline (6 节, 引用内容回源 10/10 + blackoutPeriod 补强) + 32 问 + review-notes (两轮: ControlPlaneClient 退避 L84/GRFC A61 L331 修正 + 补 XdsNameResolver 闭环 q2) + harness MiniG7 3/3 (订阅-推送/一致性哈希 1/N 实测 353/1000≈1/3/加权随机 9:1)
- [x] **G-8 完成 + 已深审** (2026-08-16): KP (5 机制) + outline (5 节, rlsStub L130/fallback L136 修正) + 26 问 + review-notes + temporal-trace + harness MiniG8 3/3 (路由三层/在途去重/比例节流)
- [x] **✅ 全部 8 域完成 (2026-08-16 收官)**: 全量回归 harness **8/8 全绿 (38/38 断言)**
- [x] **✅ 收官全量审查 (07 五维度)**: R1/R3 拓扑重排 (G-4/G-5 互换消除前向引用违规) / R2 锚点密度 8/8 达标 / R4 横切矩阵合理 / R5 负面+开篇全达标 / G-8 回源 2 处修正 (L94/L827)

---

**本会话已确认的事实**: 版本 1.83.1 / 模块结构 / **8 域清单 (G-1~G-8, G-7 xDS + G-8 RLS 为重审新增)** / 关键文件行数 (25 项穷举, 1 修正 NettyServerBuilder 72→800) / Deadline 与 Context 在 api/src/context (1124/288) / compiler 为 C++ 插件 / **RoundRobin 在 util、WeightedRoundRobin+RingHash+WrrLocality 等高级策略在 xds (184 文件, 核心仅 PickFirst)** / util 全家族 (MultiChild 414 为 xds 策略基类) / 依赖矩阵 (xds 最上层: core 41/util 15/netty 7; rls 独立: core 4/util 3/stub 1, xds 反射集成) / RetriableStreamTest+RetryPolicyTest 存在 / **rls 数据面路由 (CachingRlsLbClient 1104) 独立成域** / **services 的 HealthCheckingLoadBalancerFactory (511) 为 G-4 对照**。**其余全部待逐域交付**。
