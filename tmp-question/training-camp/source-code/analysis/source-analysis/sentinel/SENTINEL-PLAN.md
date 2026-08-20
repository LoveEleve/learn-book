# Sentinel — 知识网络化规划 (S-1~S-10)

> **日期**: 2026-08-17 | **依据**: issue/源码分析执行计划.md 阶段5.9 (10 域 ST-1~ST-10) + **issue/Sentinel源码学习范围规划.md** (既有 10 域 S-1~S-10) + **09 对既有规划保持怀疑** 重审
> **源码**: `/data/workspace/source-code/code/spring/sentinel` (**1.8.9**, git d75a6687 实证; 全仓库主源 **906 文件**: core 189 + adapter 208 + cluster 127 + extension 67 + transport 71 + dashboard 111 + logging 2 + benchmark 1)
> **定位**: 阶段 5.9 — RPC 与服务治理末环 **Sentinel 流控引擎 (SphU.entry → ProcessorSlotChain 10 槽 → 滑动窗口统计 → 限流/熔断/热点/系统/授权规则)**
> **核心依赖**: 消费 **5.7 Spring Cloud Alibaba** (A-6 三路限流/A-7 数据源/A-8 网关+断路器 — Alibaba 侧装配, Sentinel 侧内核); 与 **5.4 SCC** (RetryTemplate) 无直接依赖
> **知识网络**: Alibaba (A-6/A-7/A-8 装配面 ↔ 本仓库内核面) + Gateway (GW-6 限流 → Sentinel Gateway 适配器) + Dashboard (运维面)
> **my-xhs 关联**: 训练营项目 my-xhs 用 Sentinel 做限流/熔断 (SCA 集成) — 本规划服务该项目理解

---

## 一、入口点与主线

`SphU.entry(resource) (SphU.java: static 门面) → CtSph.entry (Sph 实现: lookProcessChain 按 resource 缓存/创建 Slot 链 → chain.entry(Context, wrapper) 依次传递) → AbstractLinkedProcessorSlot.entry/fireEntry (next 链表) → 10 槽顺序执行: NodeSelectorSlot → ClusterBuilderSlot → LogSlot → StatisticSlot → AuthoritySlot → SystemSlot → FlowSlot → DegradeSlot → DefaultCircuitBreakerSlot (+ 扩展注入的 ParamFlowSlot) → Entry 返回 → 调用方 finally Entry.exit → fireExit 逆向统计`

## 二、顶层包扫描 + 包↔域覆盖矩阵 (09 怀疑对象 #1)

### 仓库模块 (11 顶层模块)

| 模块 | 主源文件 | 归属 |
|---|---:|---|
| sentinel-core (核心引擎) | 189 | S-1~S-7 (14 根类 + slots 67 + node 15 + eagleeye 15 + log 14 + cluster 11 + metric 6 + property 5 + context 4 + spi 3 + init 3 + config 2 + util 16 + concurrent 1 + annotation 1) |
| sentinel-adapter (框架适配) | 208 | S-8 |
| sentinel-extension (扩展模块) | 67 | S-6/S-8 |
| sentinel-cluster (集群限流, 4 模块) | 127 (client 26/common 19/server 71/envoy-rls 11) | S-9 |
| sentinel-transport (传输, 4 模块) | 71 (common 41/netty-http 12/simple-http 11/spring-mvc 7) | S-10 |
| sentinel-dashboard (控制台) | 111 | S-10 |
| sentinel-logging (slf4j 适配) | 2 | S-1 附 (Logger SPI 扩展) |
| sentinel-benchmark | 1 | **排除** (基准测试) |
| sentinel-demo | — | **排除** (示例) |
| doc | — | **排除** (文档) |
| sentinel-logging 其余 (jul) | — | core/log 内置 (S-1 附) |

### sentinel-core 顶层包 (09 定量预检: slots 67 ≥ 50 文件, 必读关键类后再决定)

| 包 | 文件 | 归属 | 设计决策 |
|---|---:|---|---|
| slots/ (67) | 67 | S-3/S-4/S-5/S-6/S-7 | 10 槽执行链 + 规则引擎 |
| ├ block/ (44) | 44 | S-3/S-4/S-6/S-7 | Rule/BlockException/RuleManager 基础 + flow 19 + degrade 13 + authority 5 |
| ├ statistic/ (13) | 13 | S-3/S-5 | **LeapArray 滑动窗口** (base 4: LeapArray 420 行/WindowWrap/UnaryLeapArray + metric 6: ArrayMetric/BucketLeapArray/FutureBucketLeapArray/OccupiableBucketLeapArray + StatisticSlot + MetricEvent + MetricBucket) |
| ├ system/ (5) | 5 | S-7 | SystemSlot 系统保护 |
| ├ logger/ (2) | 2 | S-1 | LogSlot 日志槽 |
| ├ nodeselector/ (1) + clusterbuilder/ (1) | 2 | S-5 | Node 构建槽 |
| node/ (15) | 15 | S-5 | StatisticNode/ClusterNode/DefaultNode/EntranceNode + metric/ 子包 |
| eagleeye/ (15) | 15 | S-5 | 独立日志系统 (EagleEye 族, 15 文件) |
| log/ (14) | 14 | S-1 附 | Logger SPI (LoggerSpiProvider) + LogBase/RecordLog |
| cluster/ (11) | 11 | S-9 | TokenService/TokenResult/ClusterStateManager (集群限流内核接口) |
| slotchain/ (11) | 11 | S-1 | AbstractLinkedProcessorSlot (next 链表+fireEntry) + DefaultProcessorSlotChain + SlotChainProvider (58) + SlotChainBuilder SPI |
| metric/ (6) | 6 | S-5 附 | MetricExtension 回调 (MetricCallbackInit — core 唯一 InitFunc) |
| property/ (5) | 5 | S-7 | SentinelProperty/DynamicSentinelProperty/PropertyListener — **规则管理基础设施** |
| context/ (4) | 4 | S-2 | Context/ContextUtil (281)/NullContext |
| spi/ (3) | 3 | S-1 | **SpiLoader 542 行 (Sentinel 自有 SPI, 非 JDK ServiceLoader)** |
| init/ (3) | 3 | S-1 | InitExecutor/InitFunc/InitOrder (启动初始化 SPI) |
| config/ (2) | 2 | S-1 附 | SentinelConfig/SentinelConfigLoader (全局配置) |
| util/ (16) | 16 | **排除** (纯工具) |
| 根目录 (14) | 14 | S-2 | Sph/SphU/SphO/CtSph/Entry/CtEntry/AsyncEntry/Tracer/ResourceTypeConstants/Env 等 |
| concurrent/ + annotation/ | 2 | S-2 附 (NamedThreadFactory + @SentinelResource) |

## 三、怀疑审计表 (09 规范 — 执行计划 ST + issue 规划 S 双基准)

| 断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| 版本 1.8.9 | git describe | d75a6687 "1.8.9" | **接受** |
| "906 文件" | find 全仓库主源 | **906** (core 189/adapter 208/cluster 127/extension 67/transport 71/dashboard 111/logging 2/benchmark 1) | **接受** |
| "sentinel-core 189" | find | **189** 主源 (总 266 含测试) | **接受** |
| "dashboard 111" | find | **111** | **接受** |
| "cluster client 26/server 71/common 19/envoy 11" | find 穷举 | **26/71/19/11 = 127** | **接受** |
| "transport common 41/netty 12/simple 11/spring-mvc 7" | find 穷举 | **41/12/11/7 = 71** | **接受** |
| S-1 "10 个 Slot" | SPI 配置文件穷举 | core `META-INF/services/ProcessorSlot` **9 个** + extension parameter-flow-control SPI **1 个 (ParamFlowSlot)** = **10 个分置两处** | **修正**: core 内置 **9** 非 10; ParamFlowSlot 在 sentinel-extension 经 SPI 注入 (S-6 边界) |
| ST-3 "8 种 ProcessorSlot" | 同上 | core SPI 实为 **9 种** (含 DefaultCircuitBreakerSlot) | **修正**: 8 → **9 内置** (10 含扩展) |
| ST-1 "SphU.entry→ProcessorSlotChain→FlowSlot→FlowRuleChecker→TrafficShapingController" | grep + 行数 | CtSph (lookProcessChain) + FlowSlot→FlowRuleChecker→TrafficShapingController 三实现 (Default/WarmUp/Throttling) | **接受** (并入 S-3) |
| ST-2 "DegradeSlot→DegradeRule→CircuitBreaker" | grep | AbstractCircuitBreaker + ExceptionCircuitBreaker + ResponseTimeCircuitBreaker + circuitbreaker/ 7 文件 + DefaultCircuitBreakerSlot | **接受** (并入 S-4) |
| ST-4 "LeapArray 滑动窗口单独成域" | 定量预检 | statistic/base 4 文件 + metric 6 文件 (LeapArray 420 行) — 算法级设计决策 | **接受但并入 S-3** (与 FlowSlot 同域教学, 详见 §六) |
| ST-5 "热点参数 ParamFlowSlot" | SPI 验证 | ParamFlowSlot 在 **sentinel-extension/parameter-flow-control** (SPI 注册第 10 槽), 非 core | **接受** (S-6, 边界修正: 扩展模块) |
| ST-6 "SystemSlot→SystemRule" | grep | system/ 5 文件: SystemSlot + SystemRuleManager + SystemRule | **接受** (并入 S-7) |
| ST-7 "AuthoritySlot→AuthorityRule" | grep | authority/ 5 文件 | **接受** (并入 S-7) |
| ST-9 "Dashboard 规则推送→MachineInfo" | dashboard 包扫描 | domain 34/controller 17/datasource 16/repository 11/auth 8 等 | **接受** (并入 S-10) |
| **ST-10 "控制台 SentinelWebInterceptor/SentinelFeign"** | 模块归属验证 | SentinelWebInterceptor/SentinelFeign 在 **sentinel-adapter** (webmvc/dubbo-feign 适配器), 非"控制台"; 控制台 = sentinel-dashboard | **修正**: 命名混乱 — 适配器并入 S-8, 控制台并入 S-10 |
| 执行计划缺 "Context+Entry" 域 | 顶层包扫描 | context/ 4 + 根 14 类 (CtSph/CtEntry/AsyncEntry/Tracer/SphU) — 定义特征级 (资源访问入口) | **新增 S-2** (🔴, issue 规划已有) |
| 执行计划缺 "Node 体系" 域 | 顶层包扫描 | node/ 15 + eagleeye/ 15 + nodeselector/clusterbuilder — 统计数据结构 = 限流/熔断的判据来源 | **新增 S-5** (🟡, issue 规划已有) |
| 执行计划缺 "规则管理" 面 | 顶层包扫描 | property/ 5 (SentinelProperty 动态属性) + RuleManager 族 — 数据源推送入口 | **新增 S-7 组成** (🟡) |
| 执行计划缺 "传输" 域 | 模块扫描 | transport 4 模块 71 文件 (CommandCenter/HeartbeatSender + 17 CommandHandler SPI) | **新增 S-10 组成** (🟡) |
| issue S-8 "15+ 适配器" | 模块穷举 | sentinel-adapter **21 个模块** 208 文件 (4 基础模块 + 17 具体适配器) | **修正**: 21 模块 (非"15+") |
| issue S-8 "14 扩展" | 模块穷举 | sentinel-extension **14 个模块** (8 datasource + annotation 2 + parameter-flow + metric-exporter 2 + datasource-extension) | **接受**: 14 模块 |
| issue S-8 "sentinel-web-servlet 用 javax.servlet" | pom 验证 | 1.8.9 web-servlet 基于 javax (Boot 3.x 需 v6x 适配器 — 仓库已有 webmvc-v6x) | **接受** (注意代际) |
| issue S-9 "envoy-rls 11" | find | **11** | **接受** |
| issue 淘汰 "sentinel-logging 2 文件" | find | **2** (slf4j Logger 实现) — 核心日志机制在 core/log (14 文件, 归 S-1) | **接受** (logging 模块并入 S-1 附) |
| issue 淘汰 "eagleeye" | 定量预检 | eagleeye/ **15 文件** (独立日志框架: EagleEye/EagleEyeAppender/EagleEyeLogDaemon/TokenBucket 等) | **接受并入 S-5** (不独立成域, 但 S-5 中详述) |

**覆盖率报告**: 执行计划 10 域 + issue 10 域 → 重审后 **10 域** (100%)。差异: ①执行计划缺 Context+Entry/Node 体系/规则管理/传输 4 面 → issue S-2/S-5/S-7/S-10 补齐 ②ST-3 "8 种 Slot" → 9 内置 ③ST-10 "控制台"命名混乱 → 适配器/控制台归位 ④ST-4 滑动窗口不单独成域, 并入 S-3 ⑤ParamFlowSlot 在 extension 非 core (边界精确化)。

### 二次 REVIEW 修正记录 (2026-08-17, 09 §3 "重审也可能错")

| # | 首版 PLAN 断言 | 复查动作 | 证据 | 结论 |
|:--:|---|---|---|---|
| R1 | core 内置 9 Slot | SPI 文件逐行 | NodeSelectorSlot/ClusterBuilderSlot/LogSlot/StatisticSlot/AuthoritySlot/SystemSlot/FlowSlot/DegradeSlot/DefaultCircuitBreakerSlot = **9** | 通过 |
| R1 | 10 槽合计 | extension SPI | parameter-flow-control → ParamFlowSlot = **第 10 槽** (运行期注入) | 通过 |
| R2 | SpiLoader 542 行 | wc -l | **542** (Sentinel 自有 SPI 加载器 — 高缓存/排序/别名) | 通过 |
| R2 | SlotChainProvider 58 行 | wc -l | **58** (懒加载 + 缓存 chainMap) | 通过 |
| R2 | LeapArray 420 行 | wc -l | **420** | 通过 |
| R2 | ContextUtil 281 行 | wc -l | **281** | 通过 |
| R2 | InitExecutor 104 行 | wc -l | **104** (启动初始化 SPI) | 通过 |
| R3 | S-8 适配器基础/具体分群 | 模块归属 | 基础 4 (web-adapter-common/web-servlet/webmvc+webmvc-v6x 归 web 族) + 网关 4 (scg/scg-v6x/zuul/zuul2/api-gateway-common) + RPC 6 (dubbo/apache-dubbo/apache-dubbo3/grpc/sofa/motan) + HTTP 3 (okhttp/apache-httpclient/jax-rs) + 响应式 2 (reactor/webflux) + quarkus 1 | 通过 (21 模块归 6 族) |
| R3 | CommandHandler 16 个 | SPI 计数 | transport-common **17** 个命令处理器 (F10 修正) (规则查询/修改命令面) | 通过 |
| R4 | 依赖方向 | import 统计 | core→slots 内聚; adapter→core (208 文件全部消费核心 API); cluster→core (TokenService 内核); dashboard→transport (API 面) | 通过 |

### 深度 REVIEW 修正记录 (2026-08-17, 09 §3 "重审也可能错" — 逐类存在性/行数/机制实证)

| # | 首版 PLAN 断言 | 复查动作 | 证据 | 结论 |
|:--:|---|---|---|---|
| D1 | cluster (core) 11 文件 | find 穷举 | **12** (client 2: ClusterTokenClient/TokenClientProvider + server 3: ClusterTokenServer/EmbeddedClusterTokenServer/EmbeddedClusterTokenServerProvider + log 2 + 根 5) | **修正: 12** |
| D2 | S-4 "SimpleErrorCounter/SlowRequestCounter" 独立类 | 类存在性 | **1.8.9 不存在独立文件** — 是 ExceptionCircuitBreaker (L115) / ResponseTimeCircuitBreaker (L120) 的**内部静态类** (issue 规划类名过时; 机制描述正确: 慢调用比例 slowCount/totalCount) | **修正: 内部类** |
| D3 | S-3 "TrafficShapingController 3 实现" | controller/ 穷举 | **4 实现**: Default/WarmUp/Throttling/**WarmUpRateLimiterController (88 行, 1.4.0, 预热+匀速排队组合)** — issue 规划漏 | **修正: 4** |
| D4 | S-3 flow 包归属 | tokenbucket/ 消费方追踪 | **tokenbucket/ 4 文件 (TokenBucket 接口 + Abstract/Default/Strict) 无任何消费者** (grep import 零命中) — 预留/未接线代码 | **标注排除** (附注: 与 eagleeye/TokenBucket 同名不同包) |
| D5 | SlotChainProvider "缓存 chainMap" | 读方法体 | **缓存的是 SPI slotChainBuilder (懒加载 volatile), 链每次 newSlotChain() 新建**; 按资源缓存链的是 **CtSph.chainMap (static volatile + 双检锁 + MAX_SLOT_CHAIN_SIZE=6000)** (CtSph L51-54/194-207) | **修正: 语义反转** |
| D6 | "SPI 配置文件是权威调用图" | @Spi order 实证 | **执行序权威 = @Spi order (Constants.ORDER_*_SLOT)**: NodeSelector -10000 → ClusterBuilder -9000 → Log -8000 → Statistic -7000 → Authority -6000 → System -5000 → **ParamFlow -3000 (扩展)** → Flow -2000 → DefaultCircuitBreaker -1500 → Degrade -1000; 配置文件只定**加载集合**, DefaultSlotChainBuilder 用 loadInstanceListSorted() 按 order 排序 | **修正: order 为权威排序, 配置为加载集合** (10 槽执行序实证) |
| D7 | ParamFlowSlot 统计机制 | extension InitFunc 读码 | **ParamFlowStatisticSlotCallbackInit 经 StatisticSlotCallbackRegistry.addEntryCallback/addExitCallback 织入 StatisticSlot** (参数统计回调注册, 非独立统计链) | **补锚 S-6** |
| D8 | S-8 类名路径 | 类存在性 | SentinelWebInterceptor 在 `adapter/spring/webmvc/` (非 servlet 包); reactor 无 SentinelReactorFilter → **SentinelReactorTransformer 族** (Mono/FluxSentinelOperator); zuul 在 `adapter/gateway/zuul/`; metric-exporter 是 **JMXMetricExporter** (非 console); prometheus 在 `metric/prom/` 包 (PromExporterInit) | **修正 5 处类名** |
| D9 | S-9 类名 | 类存在性 | **DefaultClusterClientInitFunc** (非 DefaultClusterClientInit); EmbeddedClusterTokenServerProvider **在 core** (非 server-default); server-default 根 5 类: DefaultEmbeddedTokenServer/NettyTransportServer/SentinelDefaultTokenServer/TokenServiceProvider/ServerConstants; 子包 9 个 (codec 12/command 10/connection 7/processor 4/config 4) | **修正 2 处** |
| D10 | S-10 传输面 | 类存在性 | CommandCenter + CommandCenterProvider + CommandCenterInitFunc (SPI); HeartbeatSender + HeartbeatSenderProvider + HeartbeatSenderInitFunc; 三实现: NettyHttpCommandCenter/SimpleHttpCommandCenter/SpringMvcHttpCommandCenter + Http/SimpleHttp/SpringMvc HeartbeatSender | **通过** (补 Provider 模式锚) |
| D11 | dashboard 111 | find | 子包 110 (domain 34/controller 17/datasource 16/repository 11/auth 8/discovery 5/util 4/rule 4/config 4/service 3/client 3/metric 1) + 根 1 = **111** | 通过 |
| D12 | S-5 ClusterBuilderSlot | 读码 | **clusterNodeMap 静态 volatile Map + COW (copy-on-write 重建新 Map) + 双检锁** (L70-89); clusterNode 实例 volatile | **补锚 S-5** |
| D13 | SpiLoader 能力 | 读码 | 类缓存 sortedClassList + loadInstanceListSorted/loadHighestOrderInstance (order 定义于 @Spi(order), isDefault 兜底) — 自有 SPI 非 JDK | 通过 |
| D14 | AsyncEntry | 读码 | initAsyncContext (L75-81, 空则 newAsyncContext) + cleanCurrentEntryInLocal (L45) + exitForContext — CtSph L58-60 异步路径实证 | 通过 |

### 二轮深度 REVIEW (2026-08-17, 09 反模式 5 — 对全部域断言重跑 + 补齐上轮未实证项)

| # | 断言 | 复查动作 | 证据 | 结论 |
|:--:|---|---|---|---|
| E1 | **S-9 "ClusterFlowSlot 动态切换" (PLAN) / ST-8 "ClusterFlowSlot→TokenServer/TokenClient" (执行计划)** | find 全仓库 | **ClusterFlowSlot 在 1.8.9 不存在** (执行计划过时); 集群判定 = **FlowRuleChecker.passClusterCheck 内联** (L72/L147-176) → pickClusterService (L176-182: **isClient→TokenClientProvider.getClient() / isServer→EmbeddedClusterTokenServerProvider.getServer()**) → TokenService.requestToken(flowId, acquireCount, prioritized); 判定结果三态 applyTokenResult (OK/SHOULD_WAIT sleep/NO_RULE_EXISTS) | **修正: 删除 ClusterFlowSlot 引用** |
| E2 | S-9 集群状态机 | 读码 | **ClusterStateManager**: CLUSTER_CLIENT=0/CLUSTER_SERVER=1/CLUSTER_NOT_STARTED=-1, volatile mode + switchToClient/switchToServer (L40-42/83-140) | **补锚** |
| E3 | S-8 "datasource 8" | 模块穷举 | **9 个 datasource 模块** (8 具体: nacos/apollo/consul/etcd/eureka/redis/zk/sc-config + **datasource-extension 底座**); 扩展总数仍 14 | **修正: 8+1** |
| E4 | S-8 "21 模块 6 族" 无数字 | 族内穷举 | **Web 族 5 模块 39 文件** (web-adapter-common 1/web-servlet 10/webmvc 11/webmvc-v6x 12/webflux 5) + **网关族 5 模块 84** (api-gateway-common 22/scg 13/scg-v6x 13/zuul 20/zuul2 16) + **RPC 族 6 模块 49** (dubbo 11/apache-dubbo 11/apache-dubbo3 11/grpc 2/sofa-rpc 8/motan 6) + **HTTP 族 3 模块 23** (okhttp 6/apache-httpclient 6/jax-rs 11) + **reactor 9** + **quarkus 嵌套 7 子模块 4 主源** (annotation/jax-rs/native-image × runtime/deployment) = **208 ✅** | **补数字** |
| E5 | dashboard "datasource 16" | 包内容验证 | datasource/ 实为 **entity 实体 16 个** (FlowRuleEntity/DegradeRuleEntity/MachineEntity 等, 非数据源适配器); **规则推送 = rule/ 包**: DynamicRuleProvider/DynamicRulePublisher 接口 + FlowRuleApiProvider/FlowRuleApiPublisher (基于 transport Command API) | **修正: entity + rule 双包** |
| E6 | 依赖方向 (R4 无数字) | import 全量统计 | **adapter→core 208/208 (100%) / cluster→core 99/127 (78%) / transport→core 71/71 (100%) / extension→core 52/67 (78%) / dashboard→core+transport 68/111 (61%) / core→adapter 0 (无反向)** | **补数字 (方向全接受)** |
| E7 | LeapArray 算法细节 | 读方法体 | **calculateTimeIdx = (timeMillis/windowLengthInMs) % array.length()** (环形取模) + **calculateWindowStart = time - time%windowLength** + currentWindow 三分支: 空→CAS 新建 / up-to-date→返回 / 过期→resetWindowTo (L80-130) | **补锚 S-3** |
| E8 | StatisticNode 统计面 | 读码 | **双级滚动窗口**: rollingCounterInSecond = ArrayMetric(SAMPLE_COUNT, INTERVAL) + rollingCounterInMinute = ArrayMetric(60, 60*1000) (L96-103) | **补锚 S-5** |
| E9 | 优先级限流 (OccupySupport) | 读接口 | **tryOccupyNext**: 占用未来窗口 token, 返回 sleep 时长, ≥occupyTimeout 拒绝 + addWaitingRequest/addOccupiedPass/occupiedPassQps — FutureBucketLeapArray 底座 | **补锚 S-3** |
| E10 | S-2 Tracer | 读码 | trace/traceContext/traceEntry + **setExceptionsToTrace (可配异常白名单)** + traceEntry 关联 Entry (L45-129) | 通过 |
| E11 | S-7 SystemRuleManager | 读码 | **highestCpuUsage volatile + checkSystemStatus AtomicBoolean 开关** (L72-96) — 系统保护判定门控 | 通过 |
| E12 | S-7 DegradeRuleManager | 读码 | register2Property(SentinelProperty) + loadRules — 标准规则管理模式 (L64/102) | 通过 |
| E13 | S-6 ParamFlowChecker | 读码 | **passLocalCheck/passClusterCheck 双分支** (与 FlowRuleChecker 同构, L69-72/270) — 集群热点参数判定 | 补锚 |
| E14 | S-8 annotation-cdi | 模块验证 | SentinelResourceInterceptor + SentinelResourceBinding + ResourceMetadataRegistry (CDI 拦截器, 5 文件) | 通过 |

### 三轮深度 REVIEW (2026-08-17, 09 §3 — 逐槽机制实证 + 上轮结论复查, 域结构稳定无推翻)

| # | 断言 | 复查动作 | 证据 | 结论 |
|:--:|---|---|---|---|
| F1 | S-1 logger 包组成 | 文件验证 | **slots/logger/ 2 文件 = LogSlot + EagleEyeLogUtil** (block 日志桥接, 非 eagleeye 包); **eagleeye/ 15 是独立日志框架底座** (EagleEye/Appender/LogDaemon); **log/ 14 是 Logger SPI 框架** (LoggerSpiProvider + jul 实现) — 三套日志体系边界清晰 | **补锚 S-1/S-5** |
| F2 | S-5 StatisticSlot 统计时序 | 读 entry/exit | **entry 侧: 先 fireEntry (链后检查) 后统计** (increaseThreadNum/addPassRequest → origin node → ENTRY_NODE IN 全局 + CallbackRegistry onPass); **PriorityWaitException 分支特判**; **exit 侧: RT = completeTimestamp - createTimestamp + recordCompleteFor 三节点** (node/origin/ENTRY_NODE) + exit 回调 | **补锚 (统计后置语义)** |
| F3 | S-4 DegradeSlot 状态机反馈侧 | 读 entry/exit | **entry: performChecking (遍历 DegradeRuleManager.getCircuitBreakers tryPass)** → fireEntry; **exit: blockError 非空直接 fireExit, 否则 onRequestComplete** (状态机推进在 exit 侧) | **补锚** |
| F4 | S-5 NodeSelectorSlot map key | 读注释+代码 | **map key = context.getName() 非资源名** — 同一资源多 context 多 DefaultNode; 同资源共享 ClusterNode (注释原文实证) | **补锚 (关键机制)** |
| F5 | S-7 origin 语义闭环 | 读 ClusterBuilderSlot | context.origin 非空 → **getOrCreateOriginNode + setOriginNode** (L120+) — AuthoritySlot 判据来源闭环 | **补锚** |
| F6 | S-1 链双向差异 | 读链实现 | fireEntry 走 **transformEntry (泛型转换)** / fireExit **直接 exit**; DefaultProcessorSlotChain 匿名头节点 (entry→fireEntry) + addFirst/addLast | **补锚** |
| F7 | S-3 OccupyTimeoutProperty | 读码 | **默认 500ms** (volatile, L40) | **补锚默认值** |
| F8 | S-3 FutureBucketLeapArray | 读码 | **注释实证: 原 "BorrowBucketArray"** (L12) + isWindowDeprecated "只算未来" | 通过 |
| F9 | S-2 SphU 入口 | grep | **SphU.entry 重载 14 个** (368 行); SphO 同步布尔入口 | **补数字** |
| F10 | S-10 CommandHandler 16 清单 | SPI 逐行 | BasicInfo/FetchActiveRule/FetchClusterNodeById/FetchClusterNodeHuman/FetchJsonTree/FetchOrigin/FetchSimpleClusterNode/FetchSystemStatus/FetchTree/ModifyRules/OnOffGet/OnOffSet/SendMetric/Version/FetchClusterMode/ModifyClusterMode/ApiCommand = **17 个** | **修正: 16→17** |
| F11 | dashboard controller 17 | find 全量 | 根 12 + cluster 2 + gateway 2 + v2 1 = **17** | 通过 (数字复核) |
| F12 | S-1 SentinelConfig | 读码 | props ConcurrentHashMap + 静态键 (project.name/csp.sentinel.app.name 等) — 全局配置面 | 通过 |
| F13 | S-7 FlowRuleManager | 读码 | register2Property(SentinelProperty) + loadRules — 标准模式 (L93/117) | 通过 |

> **三轮总结**: 无新域/无域删除/无教学序变更 — 域结构收敛稳定。修正 1 处 (CommandHandler 16→17), 补锚 12 处 (统计后置语义/状态机反馈侧/context 名做 key/origin 闭环/三套日志体系/链双向差异/500ms 默认等)。

## 四、重构后域清单 (10 域: 4🔴 + 6🟡)

| # | 域 | 核心类 (行数) | 核心主题 | 前置 | 方案 |
|:--:|---|---|---|---|:--:|
| S-1 | **ProcessorSlot 链 + SPI 调用图** | AbstractLinkedProcessorSlot (58)/DefaultProcessorSlotChain (83)/SlotChainProvider (58, **缓存 SPI builder**)/DefaultSlotChainBuilder (@Spi isDefault)/**SpiLoader (542, 自有 SPI: sortedClassList 缓存 + order 排序 + isDefault 兜底)**/InitExecutor (104)/LogSlot (56)/log (14)/config (2) | 10 槽责任链 (fireEntry/fireExit) + **Sentinel 自有 SPI 机制** (@Spi(order)+SpiLoader, 非 JDK) + **执行序权威 = @Spi order** (SPI 配置只定加载集合) + 启动初始化 InitFunc | 无 | 🔴 A |
| S-2 | **Context + Entry (资源访问入口)** | SphU/SphO/CtSph/CtEntry/AsyncEntry/Entry/Tracer/ContextUtil (281)/Context/NullContext/@SentinelResource | CtSph.entry: lookProcessChain → chain.entry; Entry 生命周期 (exit 逆向); **AsyncEntry 异步上下文** (initAsyncContext/cleanCurrentEntryInLocal 限制) | S-1 | 🔴 A |
| S-3 | **FlowSlot 流控 + LeapArray 滑动窗口** | FlowSlot (183)/FlowRuleChecker (209)/FlowRule (240)/TrafficShapingController **4 实现** (Default 85/WarmUp 177/Throttling 167/**WarmUpRateLimiter 88 预热+匀速**)/**LeapArray (420)/WindowWrap/BucketLeapArray/ArrayMetric (337)/MetricBucket (139)/OccupiableBucketLeapArray (101, 抢占式)**/RuleConstant/FlowRuleUtil/FlowRuleComparator | 流控四行为 (直接拒绝/预热/匀速排队/预热+匀速) + **滑动窗口算法** (calculateTimeIdx 桶索引/isWindowDeprecated/values 遍历) + QPS/线程数双 grade + 优先级限流 (OccupySupport); **tokenbucket/ 4 文件无消费者 (预留)** | S-2 + S-5 | 🔴 A |
| S-4 | **DegradeSlot 熔断降级 + CircuitBreaker** | DegradeSlot (82)/DegradeRule (185)/AbstractCircuitBreaker (163)/ExceptionCircuitBreaker (166, 内部类 SimpleErrorCounter)/ResponseTimeCircuitBreaker (170, 内部类 SlowRequestCounter)/**DefaultCircuitBreakerSlot (96)**/CircuitBreakerStateChangeObserver/EventObserverRegistry/circuitbreaker/ 7 文件 | 状态机 CLOSED→OPEN→HALF_OPEN + 慢调用比例 (内部 SlowRequestCounter: slowCount/totalCount) + 异常比例/数 (内部 SimpleErrorCounterLeapArray) + 断路器默认槽 (第 9 槽) + 状态变更观察者 | S-2 + S-5 | 🔴 A |
| S-5 | **Node 体系 + StatisticSlot + EagleEye** | StatisticNode (337)/ClusterNode (126)/DefaultNode (170)/EntranceNode (127)/NodeBuilder/NodeSelectorSlot (181)/ClusterBuilderSlot (165, **clusterNodeMap 静态 volatile + COW + 双检锁**)/**StatisticSlot (166)**/StatisticSlotCallbackRegistry/**eagleeye 15 文件 (独立日志: EagleEye/TokenBucket 同名不同包)** | 调用链节点构建 (NodeSelector/ClusterBuilder) + 统计入口 (StatisticSlot 记录 PASS/BLOCK/RT/EXCEPTION + **CallbackRegistry 供扩展注入回调 — S-6 织入点**) + 节点持有滑动窗口 + EagleEye 日志系统 | S-1 | 🟡 B |
| S-6 | **ParamFlowSlot 热点参数限流** | ParamFlowSlot (@Spi order=-3000)/ParamFlowRule/ParamFlowChecker/ParameterMetric/ParameterMetricStorage/CacheMap/ConcurrentLinkedHashMapWrapper/ParamMapBucket/**ParamFlowStatisticSlotCallbackInit (回调注册织入 StatisticSlot)**/HotParamSlotChainBuilder (兼容旧版) (extension/parameter-flow-control 模块, 22 文件) | 参数级限流 (热点 key) + CacheMap 参数计数 + **SPI 注入第 10 槽 (order=-3000 插在 System 与 Flow 之间, 非 core 内置)** + 统计经 CallbackRegistry 织入 | S-3 | 🟡 B |
| S-7 | **AuthoritySlot + SystemSlot + 规则管理** | AuthoritySlot/AuthorityRule/AuthorityRuleManager/SystemSlot/SystemRule/SystemRuleManager/**SentinelProperty/DynamicSentinelProperty/PropertyListener (property/ 5)**/RuleManager 族 | 黑白名单 (origin) + 系统保护 (CPU/RT/线程/QPS) + **规则管理基础设施** (动态属性发布-监听, 数据源推送入口) | S-2 + S-5 | 🟡 B |
| S-8 | **适配器 + 扩展模块** | SentinelWebInterceptor/AbstractSentinelInterceptor (webmvc, 11 文件)/SentinelWebFluxFilter (69)/SentinelGatewayFilter (118, scg)/**SentinelReactorTransformer 族 (reactor 9 文件, 无 Filter 类)**/SentinelDubboProviderFilter (104)/SentinelOkHttpInterceptor (66)/SentinelGrpcClientInterceptor (142)/SentinelSofaRpcProviderFilter (93)/SentinelJaxRsProviderFilter (88)/zuul 族 (adapter/gateway/zuul)/SentinelResourceAspect (79, annotation-aspectj)/datasource 9 (8 具体 + extension 底座: NacosDataSource 163/AbstractDataSource 56)/metric-exporter (JMXMetricExporter + prometheus PromExporterInit) | **21 适配器模块 208 文件**: Web 族 39 + 网关族 84 + RPC 族 49 + HTTP 族 23 + reactor 9 + quarkus 嵌套 4 + **14 扩展模块 67 文件** (数据源 SPI 推送 + 注解切面 aspectj/cdi + 指标导出) | S-1~S-7 | 🟡 B |
| S-9 | **集群限流** | core/cluster 12 (TokenService 接口: requestToken/requestParamToken/requestConcurrentToken + **ClusterStateManager 状态机** CLIENT=0/SERVER=1/NOT_STARTED=-1 + EmbeddedClusterTokenServerProvider) + **FlowRuleChecker 集群判定内联** (passClusterCheck L147-176: isClient→TokenClientProvider / isServer→EmbeddedClusterTokenServerProvider) + client-default 26 (**DefaultClusterTokenClient 233/NettyTransportClient/TokenClientHandler/DefaultClusterClientInitFunc**/codec/registry/config) + server-default 71 (**根 5: DefaultEmbeddedTokenServer/NettyTransportServer/SentinelDefaultTokenServer/TokenServiceProvider/ServerConstants** + cluster/flow: ClusterFlowChecker/ConcurrentClusterFlowChecker/DefaultTokenService + codec 12/command 10/connection 7/processor 4) + envoy-rls 11 (SentinelEnvoyRlsServer/SentinelRlsGrpcServer/SimpleClusterFlowChecker) | **无 ClusterFlowSlot (1.8.9)** — 集群判定在 FlowRuleChecker 内联 (clusterMode + TokenService 双 Provider: 嵌入/独立) + ClusterFlowConfig/FlowRule.clusterMode + Envoy RLS 集成 | S-3 + S-7 | 🟡 B |
| S-10 | **传输 + Dashboard** | transport-common 41 (**CommandCenter + CommandCenterProvider + CommandCenterInitFunc / HeartbeatSender + HeartbeatSenderProvider / CommandHandler SPI 17**)/netty-http 12 (**NettyHttpCommandCenter/HttpHeartbeatSender**)/simple-http 11 (**SimpleHttpCommandCenter/SimpleHttpHeartbeatSender**)/spring-mvc 7 (**SpringMvcHttpCommandCenter/SpringMvcHttpHeartbeatSender**) + dashboard 111 (domain 34/controller 17/**entity 16 (datasource 包实为实体类)**/repository 11/auth 8 + **rule/ 推送: DynamicRuleProvider/DynamicRulePublisher + FlowRuleApiProvider/Publisher**) | 命令端口三实现 (规则查询/修改) + 心跳上报 (MachineInfo→Dashboard) + 控制台 (规则推送经 Command API/监控/集群管理) | S-7 | 🟡 B |

## 五、执行顺序 (拓扑)

**S-1 → S-2 → S-5 → S-3 → S-4 → S-6 → S-7 → S-9 → S-8 → S-10**

> 拓扑理由: SPI 调用图 (S-1 链怎么建) → Context+Entry (S-2 入口怎么进) → Node 统计体系 (S-5 数据怎么记 — **提前**: Flow/Degrade 的判据来自统计节点) → 流控 (S-3 滑动窗口算法) → 熔断 (S-4 状态机) → 热点 (S-6 参数级扩展) → 规则管理+系统/授权 (S-7 规则怎么管) → 集群 (S-9 单机→分布式) → 适配器 (S-8 框架装配面) → 传输/控制台 (S-10 运维面)。
>
> **注意**: 与 issue 规划 S-1~S-10 顺序差异仅在 **S-5 前移** (统计数据结构是 S-3/S-4 的判据来源, 先讲判据再讲规则引擎; 详细规划把 S-5 放第 5, 拓扑序应在第 3)。

## 六、与既有规划的差异说明

| 差异 | 既有规划 | 本 PLAN | 理由 |
|---|---|---|---|
| 滑动窗口独立域? | 执行计划 ST-4 独立成域 | **并入 S-3** | LeapArray 是 FlowSlot 的统计底座, 与流控同域教学连贯 (与 issue 规划一致) |
| 10 槽归属 | 全部归 S-1 (含糊) | core 9 + extension 1 分置 | ParamFlowSlot 在 sentinel-extension (SPI 实证), 边界精确化 |
| Slot 执行序权威 | issue 规划 "SPI 配置文件是权威调用图" | **@Spi order 排序为权威, 配置只定加载集合** | DefaultSlotChainBuilder.loadInstanceListSorted() 实证 (深度 REVIEW D6) |
| 熔断统计类 | issue 规划独立类名 | **内部静态类** (1.8.9) | 类存在性实证 (深度 REVIEW D2) |
| 流控行为数 | "3 种控制行为" | **4 实现** (+WarmUpRateLimiter) | controller/ 穷举 (深度 REVIEW D3) |
| Node 体系 | S-5 (第 5 位) | S-5 内容不变, 教学序提前至第 3 | 拓扑序 (S-3/S-4 依赖统计判据) |
| 适配器数量 | "15+" | **21 模块 (6 族)** | 模块穷举 |
| 传输 | 执行计划缺 | S-10 组成 (与 dashboard 同域) | 模块扫描 (71 文件承载命令/心跳设计决策) |
| ST-10 命名 | "控制台 SentinelWebInterceptor/SentinelFeign" | **修正**: 适配器 (S-8) vs 控制台 (S-10) 分离 | 类归属验证 |
| 链缓存位置 | "SlotChainProvider 缓存链" | **CtSph.chainMap 按资源缓存链, SlotChainProvider 只缓存 SPI builder** | 读方法体 (深度 REVIEW D5) |
| ParamFlow 统计机制 | 未述 | **CallbackRegistry 回调织入 StatisticSlot** | 读码 (深度 REVIEW D7) |
| 集群判定 | ST-8 "ClusterFlowSlot→TokenServer/TokenClient" | **无 ClusterFlowSlot (1.8.9)** — FlowRuleChecker.passClusterCheck 内联 + TokenService 双 Provider | 全仓库 find (二轮 E1) |
| dashboard 规则面 | 未述 | **rule/ 包 DynamicRuleProvider/Publisher + entity 实体** (datasource 包实为实体类) | 包内容验证 (二轮 E5) |

## 七、已排除 (00 §3)

| 包/类 | 文件 | 理由 |
|---|---|---|
| sentinel-benchmark | 1 | 基准测试 (issue 淘汰同理由) |
| sentinel-demo | — | 示例代码 |
| doc | — | 文档 |
| util/ (core) | 16 | 纯工具 (无设计决策) |
| sentinel-logging 独立成域 | 2 | 仅 slf4j Logger 实现 — 日志机制在 core/log (归 S-1 附) |
| eagleeye 独立成域 | 15 | 独立日志框架但承载度低 — 并入 S-5 详述 (issue 淘汰同理由) |

## 八、完成检查单 (09 + 00 §8)

- [x] 双基准怀疑审计 (执行计划 ST-1~ST-10 + issue S-1~S-10 全部断言验证)
- [x] 数字穷举: 906 文件 / core 189 / adapter 208 / cluster 127 (26/71/19/11) / transport 71 (41/12/11/7) / extension 14 模块 / adapter 21 模块 / dashboard 111 / Slot 9 内置 + 1 扩展 / CommandHandler 17 / TrafficShapingController 4 实现
- [x] 依赖方向: adapter→core / cluster→core / dashboard→transport (import 证据)
- [x] 修正 6 处 (Slot 数 8→9 内置 / ST-10 命名 / 适配器 21 模块 / ParamFlowSlot 归属 / 滑动窗口并入 S-3 / Node 教学序提前) + 新增面 4 处 (S-2/S-5/S-7 规则管理/S-10 传输 — issue 规划已有, 执行计划补齐)
- [x] 二次 REVIEW (09 §3): 行数重验 (SpiLoader 542/LeapArray 420/ContextUtil 281) + 适配器 6 族归群 + CommandHandler 17
- [x] **深度 REVIEW (09 §3 "重审也可能错"): 14 项全量实证 — D1 cluster 12 (非 11) / D2 熔断统计内部类 (非独立文件) / D3 流控 4 实现 (+WarmUpRateLimiter) / D4 tokenbucket 无消费者 / D5 SlotChainProvider 缓存语义反转 / D6 @Spi order 为执行序权威 / D7 ParamFlow 回调织入 / D8 S-8 类名 5 处 / D9 S-9 类名 2 处 / D10 Provider 模式 / D11 dashboard 111 / D12 clusterNodeMap COW / D13 SpiLoader 能力 / D14 AsyncEntry 异步链**
- [x] **二轮深度 REVIEW (09 反模式 5, 全量重跑): 14 项 — E1 ClusterFlowSlot 不存在 (集群判定内联 FlowRuleChecker) / E2 ClusterStateManager 状态机 / E3 datasource 8+1 / E4 适配器 6 族数字穷举 (39+84+49+23+9+4=208) / E5 dashboard entity+rule 双包 / E6 依赖方向 import 数字 (100%/78%/100%/78%/61%/0) / E7 LeapArray 环形索引算法 / E8 StatisticNode 双级窗口 / E9 OccupySupport 优先级 / E10 Tracer 白名单 / E11 SystemRuleManager 门控 / E12 DegradeRuleManager 模式 / E13 ParamFlowChecker 双分支 / E14 annotation-cdi**
- [x] **三轮深度 REVIEW (09 §3, 逐槽机制实证): 13 项 — F1 三套日志体系边界 / F2 StatisticSlot 统计后置时序 (entry 先 fireEntry 后统计, exit 算 RT) / F3 DegradeSlot 状态机反馈在 exit / F4 NodeSelectorSlot context 名做 key / F5 origin 语义闭环 / F6 链双向差异 (transformEntry vs 直接 exit) / F7 OccupyTimeout 默认 500ms / F8 FutureBucketLeapArray=原 BorrowBucketArray / F9 SphU.entry 14 重载 / F10 CommandHandler 16→**17** / F11 controller 17 复核 / F12 SentinelConfig / F13 FlowRuleManager — 无域结构变更, 收敛稳定**
- [ ] 每域开工前: 对该域断言复查 (重点: 行号/默认值)
- [ ] 域清单已同步 HANDOFF (待 HANDOFF-SENTINEL.md 创建)

---

## 附: 源码关键锚点 (深度 REVIEW 实证, 写作时仍须 re-grep)

| 锚点 | 位置 |
|---|---|
| core Slot SPI 配置 (9 内置) | sentinel-core/src/main/resources/META-INF/services/com.alibaba.csp.sentinel.slotchain.ProcessorSlot |
| ParamFlowSlot SPI (第 10 槽, @Spi order=-3000) | sentinel-extension/sentinel-parameter-flow-control/src/main/resources/META-INF/services/...ProcessorSlot |
| **Slot 执行序权威 (@Spi order 排序)** | sentinel-core/.../Constants.java L76-84 (ORDER_NODE_SELECTOR_SLOT=-10000 → ... → ORDER_DEGRADE_SLOT=-1000) + DefaultSlotChainBuilder (loadInstanceListSorted) |
| SlotChainBuilder SPI | sentinel-core/src/main/resources/META-INF/services/...SlotChainBuilder (DefaultSlotChainBuilder @Spi isDefault) |
| core InitFunc SPI | sentinel-core/src/main/resources/META-INF/services/...init.InitFunc (MetricCallbackInit) |
| CommandHandler SPI (17, F10 修正) | sentinel-transport/sentinel-transport-common/src/main/resources/META-INF/services/...command.CommandHandler |
| **CtSph.chainMap 按资源缓存链 (MAX_SLOT_CHAIN_SIZE=6000)** | sentinel-core/.../CtSph.java L51-54/194-207 + Constants.java L37 |
| SlotChainProvider 缓存 SPI builder (非链) | sentinel-core/.../slotchain/SlotChainProvider.java (58 行) |
| LeapArray 算法核心 | sentinel-core/.../slots/statistic/base/LeapArray.java (420 行) |
| StatisticSlotCallbackRegistry (扩展织入点) | sentinel-core/.../slots/statistic/StatisticSlotCallbackRegistry.java + extension ParamFlowStatisticSlotCallbackInit L33-35 |
| 熔断内部统计类 (非独立文件) | ExceptionCircuitBreaker.java L115 (内部 SimpleErrorCounter) / ResponseTimeCircuitBreaker.java L120 (内部 SlowRequestCounter) |
| **集群判定入口 (无 ClusterFlowSlot)** | FlowRuleChecker.java L72 (clusterMode→passClusterCheck) / L147-176 (pickClusterService: isClient→TokenClientProvider / isServer→EmbeddedClusterTokenServerProvider) + ClusterStateManager L40-42 (CLIENT=0/SERVER=1/NOT_STARTED=-1) |
| **LeapArray 环形索引算法** | LeapArray.java calculateTimeIdx (timeId % array.length) + calculateWindowStart + currentWindow 三分支 |
| StatisticNode 双级窗口 | StatisticNode.java L96-103 (ArrayMetric(SAMPLE_COUNT, INTERVAL) 秒级 + ArrayMetric(60, 60*1000) 分钟级) |
| 优先级限流 OccupySupport | node/OccupySupport.java (tryOccupyNext: 返回 sleep 时长 ≥occupyTimeout 拒绝) |
| dashboard 规则推送 | dashboard/rule/ (DynamicRuleProvider/DynamicRulePublisher + FlowRuleApiProvider/Publisher) + dashboard/datasource/entity (16 实体) |
