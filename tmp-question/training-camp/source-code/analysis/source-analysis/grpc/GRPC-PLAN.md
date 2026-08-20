# gRPC — 知识网络化规划 (G-1~G-8, 09 深度怀疑审计后 v5 终版)

> **日期**: 2026-08-16 | **依据**: issue/源码分析执行计划.md 阶段5.3 (6 域) + HANDOFF-GRPC (§二 初稿) + **09 对既有规划保持怀疑 四轮深度重审** (v1: 行数穷举/全模块扫描/依赖矩阵; v2: 未覆盖包关键类源码级测试 → G-7/G-8; v3: ≥400 行单类反向扫描+锚点+测试地图+分类复查; v4: core/internal 135 + netty 67 全量文件级测试; **v5: api 134 文件全量归类 + xds internal 子包判定 + 入口链路验证 — 终版**)
> **源码**: `/data/workspace/source-code/code/spring/grpc-java` (**gRPC-Java 1.83.1**, build.gradle:24 `CURRENT_GRPC_VERSION` 实证; 浅克隆单提交 8f621c0)
> **定位**: 阶段 5.3 — RPC 与服务治理第三环 **HTTP/2 + protobuf 的高性能 RPC 框架** (API→core→netty 三层架构 + xDS/RLS 服务网格面)
> **知识网络**: 与阶段 5.1 Feign (声明式 HTTP 客户端对照) + 5.2 Dubbo (另一 AI 负责, 代理/RPC 对照) + 阶段2 Spring (拦截器/IoC 对照) + Netty (阶段1, HTTP/2 底层) 互联

---

## 〇、09 深度怀疑审计表 (gRPC, 2026-08-16 二次重审) — 必读

> 第一次审计 (HANDOFF §二) 结论"6 域 100% 覆盖 + 4 处补充"是**不完整的** — 漏掉了全仓库最大模块 xds (184 文件)。本次按 09 §2 四类怀疑对象全量重跑。

### 0.1 数字穷举验证 (09 §2 #2)

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| api 134 文件 | find 穷举 | 134 | **接受** ✅ |
| core 135 文件 | find 穷举 | 135 | **接受** ✅ |
| stub 18 / netty 67 / protobuf 6 / services 27 / util 19 / compiler 12 (C++) | find 穷举 | 18 / 67 / 6 / 27 / **19** / 12 | **接受** ✅ |
| ManagedChannelImpl 2200 / RetriableStream 1618 / LoadBalancer 1626 / NameResolver 1040 / ServerImpl 980 / ClientCallImpl 784 / InternalSubchannel 897 / DnsNameResolver 709 / Status 672 / Metadata 1051 / NettyServer 494 / NettyClientTransport 477 / ServerCallImpl 399 / ClientCalls 997 / ServerCalls 506 / RetryPolicy 96 / PickFirstLoadBalancer 202 / PickFirstLeafLoadBalancer 915 / AutoConfiguredLoadBalancerFactory 218 / ServerCall 269 / Abstract*Stub 70/69/70 | wc -l 逐文件 | **24/25 精确命中** (逐字节) | **接受** ✅ |
| **NettyServerBuilder 72 行** (HANDOFF §四 G-2 锚点) | wc -l | **实为 800 行** | **修正** ⚠ (锚点 "NettyServerBuilder.java:72" 必须改为 800) |
| HedgingPolicy 行数 (HANDOFF 未给) | wc -l | 72 行 | **补充** ✅ |
| Context/Deadline 在 api/src/context | ls | 存在; Context 1124 行 / Deadline 288 行 | **接受+补充** ✅ |
| "核心只内置 PickFirst" | rg "extends LoadBalancerProvider" core/ | 仅 3 个: AutoConfiguredLoadBalancerFactory + PickFirstLoadBalancerProvider + FixedPickerLoadBalancerProvider (固定 picker, 非策略) | **接受** ✅ |
| Abstract*Stub 模块归属 | ls stub/ | 在 **stub 模块** (非 api) — HANDOFF §四 未标模块, 无冲突 | **接受** ✅ |
| RetriableStreamTest/RetryPolicyTest/HedgingPolicyTest/BackoffPolicyRetrySchedulerTest 存在 | ls core/src/test | 全部存在 (+AutoConfigured/PickFirst/DnsName 测试) | **接受** ✅ |

### 0.2 域清单完整性 — 全模块扫描 + 关键类源码级设计决策测试 (00 §3 定量预检)

> v2 补充: 对每个未覆盖包按 00 §3 预检阈值 (≥50 文件 / 单类 ≥500 行 / ≥3 子目录) **读了关键类源码**, 不再凭文件清单下结论。

| 模块 | 文件数 | 关键类源码证据 (设计决策测试) | 结论 |
|---|---|---|---|
| api | 134 | 接口面 | G-1~G-6 已覆盖 |
| **xds** | **184** | 见 0.2 下节 (v1) — 通过定义特征测试 | **新增 G-7** ✅ |
| core | 135 | 已覆盖 | G-2~G-6 |
| netty | 67 | 已覆盖 | G-2/G-3 |
| **rls** | **16** | **CachingRlsLbClient (1104 行): "Every single request is routed by the server's decision. To reduce the performance penalty, LruCache is used" (L84-85)** — 数据面路由 + LinkedHashLruCache (329) + **AdaptiveThrottler (343, 自适应节流)** + BackoffCacheEntry (退避缓存) + RlsPicker; RlsLoadBalancerProvider 经 LoadBalancerRegistry SPI 注册 (G-4 机制消费方); **不依赖 xds** (0 引用), xds 的 RouteLookupServiceClusterSpecifierPlugin 反射加载 grpc-rls ("Dependency for 'io.grpc:grpc-rls' is missing", xds:61) | **⚠ 对照→新增 G-8** (原规划"对照"结论被推翻 — 单类 ≥500 行且含独立算法决策, 非 thin wrapper) |
| services | 27 | **HealthCheckingLoadBalancerFactory (511 行) = LoadBalancer.Factory 包装** — "Wraps a LoadBalancer and implements the client-side health-checking" (L66-73), 客户侧健康检查过滤 Subchannel; 其余为内置管理服务: ProtoReflectionServiceV1 (539)/ChannelzService+ChannelzProtoUtil (234+505)/BinlogHelper (880, 可观测)/HealthServiceImpl (198) | **排除+修正** ⚠: 主体仍排除 (应用面管理服务); **HealthCheckingLoadBalancerFactory → G-4 对照参考** (健康检查 LB 包装策略, 与 G-6 流控的健康检查语义相关) |
| binder | 49 | **Inbound (735)/BinderTransport (618)/BinderClientTransport (547)/SecurityPolicies (519)/Outbound (503)** — 30/49 文件 import android.*; SecurityPolicies = Android UID/权限安全模型 (internalOnly/permissionDenied, L54-77) | **排除**: Android 平台专属传输 (传输抽象第 3 实例 + Android 权限模型), 非训练营主线 |
| alts | 41 | AltsProtocolNegotiator (452)/AltsTsiFrameProtector (408)/AltsFraming (366)/AltsHandshakerClient (252) — 24 文件 google 专属; ALTS = 传输层安全协议 (TLS 替代) | **排除**: Google 专属安全面 (与 s2a 同族) |
| okhttp | 32 | OkHttpClientTransport (**1694**)/OkHttpServerTransport (1230) implements **ConnectionClientTransport** (L134) — 纯 Java HTTP/2 帧处理与 NettyClientTransport 同构 | **对照**: "传输可插拔"同一决策第 2 实例 → G-3 对照 |
| inprocess | 9 | InProcessTransport (988) 同时实现 ServerTransport + ConnectionClientTransport (L83) — 内存管道 | **对照**: 传输抽象第 4 实例 (测试面) → G-3 对照 |
| grpclb | 14 | **GrpclbState (1281 行)** — 控制面 LB 协议实现 (xDS 前身): CachedSubchannelPool/GrpclbClientLoadRecorder | **对照**: G-4/G-7 时空溯源前身 (旧版控制面协议, 与 xds 职责重叠) |
| util | 19 | MultiChildLoadBalancer (414, xds 全部多子策略基类)/RoundRobin (182)/OutlierDetection/RandomSubsetting/GracefulSwitch | G-4 覆盖 (v1 修正) |
| stub | 18 | 已覆盖 | G-1/G-3 |
| s2a | 15 | S2AProtocolNegotiatorFactory (282)/S2AStub (245) — 安全面 (同 alts) | **排除**: Google 安全面 |
| opentelemetry (13)/census (7)/gcp-observability (13)/gcp-csm (2) | 35 | 可观测面 | **排除** (HANDOFF 已定) |
| cronet (7)/servlet (6)/auth (4)/googleapis (3)/protobuf-lite (3) | 23 | 平台/应用面 | **排除** |
| **authz** | 3 | AuthorizationServerInterceptor + AuthorizationPolicyTranslator + FileWatcherAuthorizationServerInterceptor — 服务端授权 (RBAC) | **对照**: G-2 拦截器主题实例 (与 xds RbacFilter 呼应) |

**xds 定义特征测试证据** (09 §2 #1: ≥50 文件必须读关键类):
- **执行计划 G-4 主题 "WeightedRoundRobin/优先同Zone" 的落点就在 xds**: WrrLocalityLoadBalancer (同 Zone 优先!) + WeightedRoundRobinLoadBalancer (**903 行**) — 原规划把这些写成"核心 LB 的对照", 实为 xds 的策略族主体
- LB 家族: RingHashLoadBalancer (575, 一致性哈希) / LeastRequestLoadBalancer (348) / PriorityLoadBalancer (368, 主备故障转移) / CdsLoadBalancer2 (651) / ClusterImplLoadBalancer (571) / ClusterManagerLoadBalancer (263) / WeightedTarget / WrrLocality — 全部 extends **MultiChildLoadBalancer (util, 414 行, 2023 年统一多子策略基类)**
- **XdsNameResolver (1206 行, 比 DnsNameResolver 709 还大)** + XdsNameResolverProvider — 控制面命名解析
- XdsClientImpl + ControlPlaneClient + LoadReportClient + LoadStatsManager2 (client/ 20 文件) — xDS 控制面协议客户端 (ADS/LRS/LDS/RDS/CDS/EDS)
- 过滤器链: FaultFilter / RbacFilter / RouterFilter / ExternalProcessorFilter / GcpAuthenticationFilter
- orca/ (4): 带外负载报告
- 测试齐全: XdsClientImplV3Test / WeightedRoundRobinTest / RingHashTest / LeastRequestTest

**→ 结论: 新增 G-7 "xDS 控制面与高级负载均衡"** 🟡 (184 文件 > core 135, 是 gRPC-Java 的"服务网格"面, gRPC 1.50+ 官方主推方向)

### 0.3 依赖方向验证 (09 §2 #3, import 证据)

| 模块 | 引 core (io.grpc.internal) | 其他 | 方向结论 |
|---|---|---|---|
| netty | 25 文件 | — | netty → core → api |
| okhttp | 17 | util 1 | okhttp → core |
| util | 5 | — | util → core |
| **xds** | **41** | netty 7 / util 15 / stub 3 / protobuf 2 / services 7 | **最上层: 依赖面最广 (含 util 的 MultiChildLoadBalancer)** |
| stub | 0 (只引 api 顶层) | — | 与 core 平级, 同层 |
| protobuf | 0 | — | 只引 api |

### 0.6 v3 深度重审 (2026-08-16) — 反向扫描 + 锚点 + 测试地图 + 分类复查

> 09 教训 #5: 重审必须复查"既有通过"部分。v3 从源码反向扫 (单类 ≥400 行穷举 → 归属检查), 并逐一验证写作依赖的锚点。

**a) 漏网之鱼 — 单类 ≥400 行反向扫描 (读类声明确认归属, 全部补入对应域)**

| 类 (行数) | 角色 | 补入 |
|---|---|---|
| netty/NettyServerHandler (**1294**) | HTTP/2 服务端帧处理器 (FrameListener extends Http2FrameAdapter L940/KeepAlivePinger L1030) — gRPC 服务端 HTTP/2 核心, 比 NettyServer (494) 大 2.6 倍 | **G-2** |
| netty/NettyClientHandler (**1186**) | HTTP/2 客户端帧处理 (PingCountingFrameWriter L1128) | **G-3** |
| netty/ProtocolNegotiators (**1249**) | 协议协商族 (h2/h2c/TLS/ProxyNegotiator L496/ServerTlsHandler L424) | G-2/G-3 传输层 |
| core/ManagedChannelImplBuilder (**1071**) | 通道装配: NameResolver/LB/传输工厂选择 (FixedPortProvider L250) | **G-3** |
| core/DelayedClientCall (**642**) + DelayedStream (551) | 就绪前缓冲调用/流 (DeadlineExceededRunnable L136/DrainListenerRunnable L329) — 客户端状态机核心 | **G-3** |
| core/MessageDeframer (**550**) | gRPC 消息帧解析 (1 字节压缩标志 + 4 字节长度) — HTTP/2 之上的消息封装 | **G-3** (G-1 对照: 消息帧格式) |
| api/Uri (**1184**) | gRPC target 解析 (scheme://authority + query) — NameResolver 入口 | **G-5** |
| util/OutlierDetectionLoadBalancer (**1165**) | 离群检测 (EEDF/熔断) — 补行数 | G-4 |
| xds/XdsServerWrapper (**960**) | **xDS 服务端面** (LDS/RDS 服务端, RestartTask L370/DiscoveryState L377) — G-7 原聚焦客户端 LB, 补服务端面 | **G-7** |
| xds/ExternalProcessorClientInterceptor (**1355**) | ExtProc 外部处理拦截器 (DataPlaneDelayedCall L262) — 过滤器链 | **G-7** |
| xds/orca/OrcaOobUtil (632) + internal/rbac/engine/GrpcAuthorizationEngine (504) | 带外负载报告/RBAC 引擎 | G-7 |
| api/InternalChannelz (1105) / services/BinlogHelper (880) / core/GrpcUtil (973) / netty/Utils (606) | 可观测/工具面 | **排除确认** |

**b) 锚点行号逐一验证 (HANDOFF §四 写作依赖, 全部命中)**

PickFirstLoadBalancer 类声明 **L40** ✅ | ClientCalls blockingUnaryCall **L140**/asyncUnaryCall **L81** ✅ | ServerCalls asyncUnaryCall **L49** ✅ | ManagedChannelImpl newCall **L809** ✅ | RetriableStream 类声明 **L55** ✅ | LoadBalancer SubchannelPicker **L453**/Helper **L1036** ✅ | DnsNameResolver 类声明 **L65** ✅ | WrrLocalityLoadBalancer **L44** (locality 权重语义 L41/L68-74 实证"同 Zone") ✅

**c) 测试地图** (src/test 计数): core **80** / netty 31 / stub 10 / util 17 / **xds 100** (最多) / rls 10 / services 13。关键测试存在: ClientCallImplTest/DelayedClientCallTest/InternalSubchannelTest/NettyClientTransportTest/NettyServerTransportTest/ManagedChannelImplBuilderTest ✅

**d) context 实现补验**: api/src/context 共 4 文件 — Context (1124)/Deadline (288)/**PersistentHashArrayMappedTrie (301, Context 快照的持久化 HAMT)**/ThreadLocalContextStorage (73) → G-3 需含 PHAMT 机制

**e) compiler 数字修正**: 核心为 **3 个 C++ 文件** (java_generator.cpp/h + java_plugin.cpp);"12 文件" 是目录文件数 (含 BUILD/golden 测试)

**f) 分类三信号复查 (00 §3.5)**: G-1/G-2/G-3 🔴 (定义特征: IDL→Stub/HTTP2 服务端/Channel-Call 状态机; 面试高频+生产主流+Hub 依赖 ✅ 高置信) / G-4~G-8 🟡 (支撑域, 建立在核心机制之上; 信号一致 ✅)。**分类成立, 无需调整**。

**g) 覆盖率报告 v3**: 域数仍 8 — v3 未增域, 但 **G-2/G-3/G-5/G-7 锚点面大幅扩充** (9 个 ≥500 行核心类补入), 排除项再确认 4 类 (可观测/工具)。

### 0.7 v4 深度重审 (2026-08-16) — core/internal 135 + netty 67 全量文件级设计决策测试

> 09 教训 #3 + 09 §2 #1 硬性要求: core/internal 80+ 文件必须过设计决策测试。v4 将 core/internal 135 文件与规划提及文件逐个 comm 对照, 未覆盖 93 文件全部分类判定; netty 67 文件同法对照。

**a) core/internal 93 个未覆盖文件判定结果: 无新域, 但暴露 6 个机制面补入既有域**

| 机制面 | 关键类 | 归属 |
|---|---|---|
| **压缩/消息帧面** | **new Codec.Gzip() (CompressorRegistry.java:34; Codec.java:35 内部类, JDK GZIP 流)** — 1.83 无独立 GzipCodec 类; MessageFramer (45, 编码)/GzipInflatingBuffer (38, gzip 解压)/ApplicationThreadDeframer/ThreadOptimizedDeframer/ReadableBuffer 族/WritableBuffer 族/TransportFrameUtil | **G-1** (消息帧格式+压缩: 1B 标志+4B 长度+压缩可选) |
| **传输抽象接口族** | ConnectionClientTransport/ClientTransport/ManagedClientTransport/ServerTransport/ServerTransportListener/ClientStream/ServerStream/Stream/StreamListener 等 — gRPC 传输可插拔的接口面 (netty/okhttp/inprocess/binder 全实现它) | G-2/G-3 传输层小节 |
| **线程模型面** | **SerializingExecutor (36, 回调串行化)**/SerializeReentrantCallsDirectExecutor/ContextRunnable/SharedResourceHolder/ObjectPool 族 | **G-3** |
| **连接生命周期面** | **KeepAliveManager (34, HTTP/2 PING 保活)**/KeepAliveEnforcer/MaxConnectionIdleManager/Http2Ping/Rescheduler/AtomicBackoff/GoAway 族 | **G-3** |
| **代理/时钟面** | **ProxyDetectorImpl (48, HTTP CONNECT 环境代理)**/TimeProvider (23, 时钟抽象)/ConcurrentTimeProvider/InstantTimeProvider | G-3 (代理)/G-6 (时钟→退避) |
| **service config 面** | JsonParser/JsonUtil — retry/hedging 策略的 JSON 来源 | **G-6** |
| 认证面 | CallCredentialsApplyingTransportFactory/AuthorityVerifier | G-3 认证对照 (api/CallCredentials + auth 模块 OAuth2) |
| 解析面 | RetryingNameResolver/UriWrapper/NameResolverFactoryToProviderFacade/OobNameResolverProvider | G-5 |
| 服务端面 | InternalHandlerRegistry/InternalServer/ServerListener/AbstractServerStream | G-2 |
| LB 支撑面 | PickSubchannelArgsImpl/InUseStateAggregator/FixedPickerLoadBalancerProvider | G-4 |
| **排除确认** | 可观测: ChannelTracer/CallTracer/TransportTracer/MetricRecorderImpl/LongCounter 族/SubchannelMetrics/ChannelLoggerImpl; 纯工具: ConscryptLoader/NoopSslSession/SpiffeUtil/CertificateUtils/GrpcAttributes/JsonUtil 内部分 | ✂️ |

**b) netty 67 文件对照: 重大补充 + 1 个新实现**

| 发现 | 类 | 归属 |
|---|---|---|
| **⚠ UdsNameResolver + UdsNameResolverProvider** — **gRPC 第二个命名解析实现 (Unix Domain Socket)** | netty/src/main/java/io/grpc/netty/ | **G-5 补** (第二解析器, 与 DnsNameResolver 并列) |
| **netty 写路径命令模式** | WriteQueue + SendGrpcFrameCommand/CancelClientStreamCommand/SendPingCommand/SendResponseHeadersCommand/GracefulCloseCommand/ForcefulCloseCommand + WriteBufferingAndExceptionHandler — 流操作→写命令队列 | **G-3 补** (传输写路径) |
| 传输生命周期 | ClientTransportLifecycleManager | G-3 |
| DoS 防护 | Http2ControlFrameLimitEncoder | G-2/G-3 |
| UDS/SSL 支撑 | NegotiationType/NettyChannelCredentials/NoopSslEngine/JettyTlsUtil/FixedKeyManagerFactory 等 | 支撑/排除 |

**c) G-6 机制验证 (双模式 RetriableStream)**: RetriableStream 同时持有 retryPolicy + hedgingPolicy, **互斥** (L146-147 "Should not provide both retryPolicy and hedgingPolicy"), isHedging = hedgingPolicy != null, scheduledHedging 定时对冲 (L122) — **Retry/Hedging 是同一流包装器的双模式**, 非两套实现 ✅。HedgingPolicy 字段 = maxAttempts/hedgingDelayNanos/nonFatalStatusCodes (L30-33); RetryPolicy 字段 = maxAttempts/initialBackoffNanos/maxBackoffNanos/backoffMultiplier/retryableStatusCodes (L33-36)。

**d) 覆盖率报告 v4**: 域数仍 8 — 无新域 (core/netty 全量文件均可归入既有域或排除), 但 **G-1/G-3/G-5/G-6 机制面扩充** (压缩面/线程模型/连接生命周期/代理时钟/service config/UDS 解析器/netty 写路径), G-5 出现**第二个解析器实现** (UdsNameResolver)。

### 0.8 v5 深度重审 (2026-08-16) — api 134 文件全量归类 + xds internal 判定 + 入口链路验证

**a) api 134 文件全量归类 (134/134, 无遗漏, 无新域)**: 服务端面 ~28 (Server/ServerBuilder/ServerCall/ServerCallHandler/ServerInterceptor/ServerInterceptors/ServerServiceDefinition/HandlerRegistry/BindableService/ServiceDescriptor/Forwarding 族/PartialForwarding 族/ServerCredentials 族/ServerProvider/ServerRegistry/ServerStreamTracer/ServerTransportFilter/ServerCallExecutorSupplier) / 客户端面 ~22 (Channel/ManagedChannel/ManagedChannelBuilder/ClientCall/ClientInterceptor/ClientInterceptors/Forwarding 族/ManagedChannelRegistry/ManagedChannelProvider/ClientTransportFilter) / LB 面 ~8 (LoadBalancer 族/ConnectivityState 族/EquivalentAddressGroup/Attributes) / 解析面 ~5 (NameResolver 族/Uri/QueryParams) / 编解码面 ~10 (MethodDescriptor/Codec/Compressor 族/Decompressor 族) / 认证面 ~10 (CallCredentials/ChannelCredentials/Choice/Composite/Tls/Insecure) / 状态支撑 (Status 族) / **排除**: Metric* 族 **14 文件** (1.83 新度量 API: MetricInstrument/LongCounter 族等)+ Configurator+FeatureFlags (特性开关) + Internal* 族 **30+** (内部 API) + BinaryLog/InternalChannelz (可观测) + Grpc 门面/TimeUtils/ExperimentalApi 等工具。**补充**: **SynchronizationContext (线程模型面, G-3)** + **ProxyDetector/ProxiedSocketAddress/HttpConnectProxiedSocketAddress (代理接口族, G-3)** + SecurityLevel (G-3) + **ClientTransportFilter (G-3)/ServerTransportFilter+ServerCallExecutorSupplier (G-2)** + Grpc.TRANSPORT_ATTR 族 (L40-63, 传输属性)。

**b) xds internal 子包全量判定 (全部归 G-7, 无新域)**: matcher/ **22 文件 = 完整路由匹配引擎** (Matcher/MatcherTree/MatcherList/MatcherRunner/UnifiedMatcher/MatchInput/OnMatch + **CelMatcher/CelEnvironment (CEL 表达式匹配)**) — G-7 路由面核心; headermutations/ 8 (HeaderMutationFilter 路由后改头) + extproc/ 6 + extauthz/ 2 + rbac/engine (GrpcAuthorizationEngine) = 过滤器面; security/ 30 + certprovider/ 10 = 传输安全面; grpcservice/ 5 (GrpcServiceConfig/CachedChannelManager) + orca/ 4 = 支撑。

**c) 入口链路验证 (00 §2)**: `Grpc.newChannelBuilder (L111) → ManagedChannelRegistry → NettyChannelBuilder (L80, extends ForwardingChannelBuilder2) → forAddress (L157) → 传输工厂 → ManagedChannelImpl` ✅ / `ServerBuilder.build 抽象 (L436) → NettyServerBuilder.build → NettyServer` ✅。**NettyServerBuilder 类声明恰在 L72 (文件 800 行)** — 原 HANDOFF "72" 真相是**类声明行**, 误记为文件行数, 两处语义现在都明确。

**d) 覆盖率报告 v5 (终版)**: 8 域, 全部 30 个顶层模块 (api 134/core 135/netty 67/stub 18/protobuf 6/util 19/xds 184/rls 16/grpclb 14/inprocess 9/okhttp 32/services 27/context 4/compiler 3 C++) **文件级覆盖或排除完毕**, 无未判定文件。

### 0.9 收官全量审查 v6 (2026-08-16, 07 方法论五维度)

**R1 桥一致性 + R3 前向引用 — 拓扑重排 (实质发现)**: G-4 前置声明 G-5 (序 6>5 违规) + G-5 引出 G-4 (回引已讲域违规) — 真依赖 (地址喂 LB) 但无环可解 → **重排: G-1→G-2→G-3→G-6→G-5→G-4→G-7→G-8** (新序: G-4 前置 G-5 ✅ 前位, G-5 前置 G-6 ✅ 前位, 无环; 教学序也更顺: 先地址来源后选址)。

**R2 锚点密度 8/8 达标**: G-1 57 / G-2 22 / G-3 14 (🔴≥8 ✅); G-4 11 / G-5 6 / G-6 8 / G-7 10 / G-8 6 (🟡≥4 ✅)。

**R4 横切关注点矩阵**: 取消链 (G-1→G-2→G-3→G-6 连续) / 退避链 (G-4→G-5→G-6→G-7→G-8 全绿) / syncContext (G-3 定义→G-4/G-5/G-6/G-7 消费) — 无"应有未提"缺口。

**R5 负面空间+开篇**: 8 域各 6 条负面空间, 每节场景句 — 全达标。

**G-8 单域回源**: 2 处修正 (MIN_EVICTION_TIME_DELTA_NANOS L94 / isInBackoffPeriod L827)。

### 0.4 执行顺序重排 (09 §2 #4, v6 更新)

既有: G-1→G-2→G-3→G-6→G-4→G-5 (HANDOFF §三)
**重排后: G-1→G-2→G-3→G-6→G-4→G-5→G-7→G-8**
理由: G-7 依赖 util 的 MultiChildLoadBalancer (G-4 讲 util 全家族后)→ 依赖 core (41 文件) → 依赖 XdsNameResolver 面 (G-5 之后); 且 grpclb (G-7 时空对照) 是 G-4 负载均衡的历史前身, 在 G-4 之后讲才有对照锚点。G-8 (rls) 依赖 core/util (G-4 后), 与 xds 反射关联 (RouteLookupServiceClusterSpecifierPlugin), 放 G-7 后作为"控制面家族收尾" (RLS 数据面路由)。

### 0.5 覆盖率报告

| 来源 | 域数 | 覆盖率 |
|---|---|---|
| 执行计划 (issue) | 6 | — |
| HANDOFF §二 初稿 | 6 | 100% (但遗漏 xds 184 文件 — 错误) |
| 深度重审 v1 | 7 | +xds |
| 深度重审 v2 | 8 | +rls |
| **深度重审 v3** | **8** | 域数不变, 锚点面扩充: +9 个 ≥500 行核心类 (NettyServerHandler/NettyClientHandler/ProtocolNegotiators/ManagedChannelImplBuilder/DelayedClientCall/MessageDeframer/Uri/XdsServerWrapper/ExternalProcessorClientInterceptor); 测试地图: core 80/xds 100 等; 分类复查 🔴3+🟡5 成立 |
| **深度重审 v4** | **8** | 域数不变 — core/internal 135 文件全量文件级测试 (93 未覆盖全部归类, 无新域): 补压缩面 (new Codec.Gzip())/线程模型/连接生命周期/代理时钟/service config 机制; **G-5 新增第二解析器 netty/UdsNameResolver**; G-6 双模式互斥验证 |
| **深度重审 v5 (终版)** | **8** | 域数不变 — api 134 文件全量归类 (无遗漏: 服务端 28/客户端 22/LB 8/解析 5/编解码 10/认证 10/排除 Metric 14+Internal 30+); xds internal 全归 G-7 (matcher 22 路由匹配引擎含 CEL); 入口链路验证 (Grpc L111→Registry→Builder); NettyServerBuilder "72" 真相=类声明 L72 (非文件行数) |

---

## 一、入口点与主线

`stub 生成 (G-1: .proto→protoc→Abstract*Stub, 70/69/70 行三形态) → ManagedChannel 装配 (G-3: ManagedChannelImplBuilder 1071 → ManagedChannelImpl 2200) → NameResolver 解析 (G-5: Uri 1184 target 解析 → DnsNameResolver 709) → LoadBalancer 选址 (G-4: LoadBalancer 1626 + PickFirst 族) → ClientCall 调用 (G-3: ClientCallImpl 784 → DelayedClientCall 642 缓冲 → NettyChannelBuilder 928 → NettyClientHandler 1186/NettyClientTransport 477 → HTTP/2 → MessageDeframer 550 消息帧) → 服务端 (G-2: NettyServerHandler 1294 帧处理/ProtocolNegotiators 1249 协商 → ServerImpl 980 → ServerCallImpl 399 → ServerCalls 506 分派) → 流控重试 (G-6: RetriableStream 1618 缓冲重放/对冲)` — 上层: `xDS 控制面 (G-7: XdsClientImpl 1102 → XdsNameResolver 1206 → 高级 LB 家族 + XdsServerWrapper 960 服务端面)` → `RLS 数据面 (G-8: CachingRlsLbClient 1104)`。

## 二、域清单 (8 域: 3🔴 + 5🟡)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| G-1 | **ProtoBuf 与 Stub 生成** | protobuf/ (6) + stub/ (18: ClientCalls 997/ServerCalls 506/Abstract*Stub 70/69/70) + compiler (C++ 3 核心, 对照) + **压缩/消息帧面 (v4)**: api/Codec.java 内部类 Gzip (L35)/core MessageFramer (45)/GzipInflatingBuffer (38) | .proto→protoc→Stub/调用分派/三形态语义/请求构造/消息帧格式+压缩 | 🔴 A |
| G-2 | **服务端** | core/ServerImpl (980) + ServerCallImpl (399) + InternalHandlerRegistry + netty/NettyServer (494) + **NettyServerHandler (1294)** + **ProtocolNegotiators (1249)** + **NettyServerBuilder (800)** + Http2ControlFrameLimitEncoder + api/ServerCall (269) | ServerBuilder 装配/请求接收/调用处理/拦截器/HTTP2 帧处理/协议协商 | 🔴 A |
| G-3 | **客户端** | core/ManagedChannelImpl (2200) + ManagedChannelImplBuilder (1071) + ClientCallImpl (784) + DelayedClientCall (642)/DelayedStream (551) + MessageDeframer (550) + **SerializingExecutor (线程模型)/KeepAliveManager/ProxyDetectorImpl (v4)** + netty/NettyClientTransport (477) + NettyClientHandler (1186) + NettyChannelBuilder (928) + **WriteQueue 命令族 (v4)** + stub 三形态 + context (Context 1124/Deadline 288/PHAMT 301) | 通道生命周期/调用执行/状态机/缓冲/消息帧/线程模型/保活/代理/Deadline/取消传播 | 🔴 A |
| G-4 | **负载均衡 (核心+util)** | api/LoadBalancer (1626) + PickFirst 族 (202/915) + InternalSubchannel (897) + AutoConfiguredLoadBalancerFactory (218) + **util 全家族** (RoundRobin 182/MultiChild 414/OutlierDetection 1165/RandomSubsetting/GracefulSwitch) + services 对照 (HealthCheckingLoadBalancerFactory 511) | Subchannel 抽象/Picker/状态管理/内置策略族/MultiChild 基类/离群检测/健康检查包装 | 🟡 B |
| G-5 | **命名解析** | api/NameResolver (1040) + api/Uri (1184) + core/DnsNameResolver (709) + **netty/UdsNameResolver (v4, 第二解析器: Unix Domain Socket)** + RetryingNameResolver + Provider | 解析器生命周期/Listener/地址更新/Attributes/target 格式/DNS+UDS | 🟡 B |
| G-6 | **流控与重试** | core/RetryPolicy (96) + HedgingPolicy (72) + RetriableStream (1618, **双模式互斥 L146-147**) + BackoffPolicyRetryScheduler + ExponentialBackoffPolicy + **JsonParser/JsonUtil (service config 来源, v4)** + TimeProvider (时钟抽象) | Retry/Hedging 双模式/指数退避/缓冲/重放/对冲/策略来源 | 🟡 B |
| G-7 | **xDS 控制面与高级负载均衡** (v1 新增) | xds/ (184: XdsNameResolver 1206/XdsClientImpl 1102/client 20/RingHash 575/LeastRequest 348/WRR 903/WrrLocality/Cds 651/ClusterImpl 571/Priority 368/XdsServerWrapper 960/ExternalProcessorClientInterceptor 1355/OrcaOobUtil 632) + grpclb (14: GrpclbState 1281) 对照 | 控制面协议/一致性哈希/加权轮询/同 Zone/过滤器链/服务端面 | 🟡 B |
| G-8 | **RLS 数据面路由** (v2 新增) | rls/ (16: CachingRlsLbClient 1104/LbPolicyConfiguration 475/AdaptiveThrottler 343/LinkedHashLruCache 329/RlsProtoData 253/RlsLoadBalancer) + xds 反射集成对照 | 服务端决策路由/LRU 缓存/自适应节流/退避缓存 | 🟡 B |

## 三、执行顺序 (拓扑: 序列化 → 服务端 → 客户端 → 流控 → 负载均衡 → 命名 → xDS → RLS)

**G-1 → G-2 → G-3 → G-6 → G-5 → G-4 → G-7 → G-8**

> 拓扑理由 (import 证据): Stub/序列化 (G-1, 一切调用载体, 无子包依赖) → 服务端 (G-2, 独立面) → 客户端 (G-3, 核心, ManagedChannelImpl 消费 NameResolver+LB SPI) → 流控重试 (G-6, RetriableStream 内嵌于 ClientCallImpl, 紧邻 G-3) → **命名解析 (G-5, 地址来源, 依赖 G-6 退避)** → **负载均衡 (G-4, 地址消费)** — **v6 重排: 消除 G-4/G-5 相互引用违规 (07 §维度3: 依赖必须前位), 新序无环** → **xDS (G-7, 最上层: xds→core 41 + util 15 (MultiChild 基类) + netty 7 + services 7, 全部前置域齐备后才有对照锚点)** → **RLS (G-8, rls→core 4 + util 3 + stub 1; 数据面路由, xds 反射集成 — 控制面家族收尾)**。

## 四、知识网络图

```
← 复用: Netty (阶段1, HTTP/2 帧层) + Spring (阶段2, 拦截器模式对照) + Feign (阶段5.1, 声明式客户端对照)
→ 对照: Dubbo (阶段5.2, 另一 AI — 代理/RPC/负载均衡对照) + Spring Cloud Commons (阶段5.4, LoadBalancer 消费)
→ 引出: Nacos (阶段5.7, 注册中心喂 NameResolver 对照) + Sentinel (阶段5.8, 流控对照 G-6)
```

## 五、完成检查单

- [x] 顶层模块扫描 ↔ 域覆盖矩阵 (34 顶层目录, 6→8 域, +2: xds/rls 通过设计决策测试)
- [x] 行数断言 25 项穷举 (1 修正: NettyServerBuilder 72→800)
- [x] **未覆盖包关键类源码级测试** (00 §3 定量预检): rls (CachingRlsLbClient 1104)/services (HealthCheckingLoadBalancerFactory 511)/binder (30 文件 android.*)/alts (24 文件 google)/okhttp (1694)/grpclb (GrpclbState 1281)/inprocess (988)/authz
- [x] 依赖方向 import 证据 (xds 最上层, rls 独立, xds 反射集成 rls)
- [x] 拓扑重排 (G-7/G-8 追加末尾, 理由记录)
- [x] 排除项全部附源码证据 (binder/alts/s2a 平台专属等)
- [x] **v3 反向扫描**: ≥400 行单类穷举 → 9 个核心类补入 G-2/G-3/G-5/G-7; 锚点行号 8 项验证全命中; 测试地图 (xds 100 测试最多); 分类三信号复查成立; compiler 数字修正 (3 个 C++ 核心文件); context 补 PHAMT (301)
- [ ] 每域开工前: 对 PLAN 中该域断言复查 (重点: 行号/默认值)
- [ ] 偏差已同步 HANDOFF-GRPC (§二 审计表更新 + §三 域清单 + §四 锚点修正)
