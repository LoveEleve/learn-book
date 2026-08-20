# G-4 负载均衡 — 谁来选这次调用的连接: LoadBalancer SPI 与策略家族

> 前置: [[G-3-客户端]] (syncContext/exitIdleMode 创建) + [[G-5-命名解析]] (地址输入, 已讲) | 引出: [[G-7-xDS]] (高级策略家族继承同一 SPI) | 对照: Dubbo LoadBalance (阶段5.2) + Envoy 集群管理
> 🟡 B | 6 KP | [模式: 策略 + 组合 + 连接状态机]
> Pass 2 闭环: q1(SPI) q2(策略委托) q3(PickFirst) q4(Subchannel) q5(轮询) q6(离群)

**读者处境**: 一次调用走到 `channel.newCall`, 最后选中哪个 IP 发起连接?通道不知道答案 — 它把这个问题交给了 LoadBalancer。而 LoadBalancer 自己也不知道——它把"连接"委托给 Subchannel, 把"选择"委托给 Picker。这套三层委托怎么运转?

### 1. LoadBalancer SPI — Helper/Picker/状态上报三方协作

场景: LoadBalancer 与通道之间谁给谁什么?
源码路径:
- **Helper.createSubchannel** (LoadBalancer.java:1050-1060): "a logical connection to the given group of addresses which are considered equivalent" — **等效地址组 (EAG)**: 一组地址 = 一个逻辑连接; **"All methods on the LoadBalancer interface are called from a Synchronization Context"** (LoadBalancer.java:58-63, 类 javadoc "The Synchronization Context")
- **SubchannelPicker.pickSubchannel** (L461-463): "Make a balancing decision for a new RPC" — **每 RPC 一次**
- **updateBalancingState** (L1183): LB 上报 (状态 + 新 Picker)
- 带外通道 createOobChannel (L1072+): LB 自己的控制面 RPC
关键设计 (q1): 三方协作: LB 收解析结果 → createSubchannel → 状态变化时 updateBalancingState (状态+新 Picker) → 通道存 Picker → 每 RPC pickSubchannel。**被放弃的方案: LB 直接持有连接** — Picker 解耦"状态变更"与"每 RPC 决策", 通道侧无锁读。 [模式: 策略模式] [跨域: G-3 syncContext]

### 2. AutoConfigured — 默认 PickFirst, service config 可换

场景: 你没配置任何策略, 用什么?配置了用什么?
源码路径:
- **构造** (AutoConfiguredLoadBalancerFactory.java:41-49): LoadBalancerRegistry + defaultProvider
- **找不到策略** (L51-56): `"Could not find policy '%s'... included in META-INF/services/io.grpc.LoadBalancerProvider"` → FixedPicker(TRANSIENT_FAILURE)
- **运行时切换** (L89-105): acceptResolvedAddresses → `(PolicySelection) getLoadBalancingPolicyConfig()` → delegate 换新策略 LB (L105)
- 测试: defaultIsPickFirst (AutoConfiguredLoadBalancerFactoryTest.java:130)
关键设计 (q2): **策略选择器**: 默认 PickFirst (1.83); service config 的 loadBalancingPolicy 驱动原子切换; provider SPI (META-INF/services) 自定义接入。**被放弃的方案: 单例策略** — 通道级策略可配 (xds 通道换 xds 策略)。 [跨域: G-3 exitIdleMode 创建时机; G-7 同 SPI 注册]

### 3. PickFirst 两代 — 从"整列表一连接"到"每地址一连接 + Happy Eyeballs"

场景: 5 个地址, PickFirst 怎么连?第一个连不上呢?
源码路径:
- **旧版** (PickFirstLoadBalancer.java:40): 单 Subchannel (L43) — 整个列表一个 EAG; shuffle 可选 (L60-67, "better distribute the load")
- **新版** (PickFirstLeafLoadBalancer.java:60): **每地址一 Subchannel** (Map<SocketAddress, SubchannelData> L69) + Index (L70)
- **Happy Eyeballs** (L64-65): `enableHappyEyeballs = !isSerializingRetries() && ...isEnabledHappyEyeballs()` — 当前地址失败 **250ms** (CONNECTION_DELAY_INTERVAL_MS L63) 后启动下一地址
- **requestConnection 逐地址** (L508-520): Index 顺序尝试 + 按子状态分支
关键设计 (q3): 两代差异 = **连接粒度**: 旧版一连接全列表 (首个地址不可达时重连代价高); 新版每地址独立 + 250ms 并行试探 (RFC 6555)。**被放弃的方案 (旧版): 单连接** — Happy Eyeballs 把"串行等待"变"并行试探"。 [网络: RFC 6555] [跨域: G-6 退避复用/hedging 同思想]

### 4. InternalSubchannel — 连接状态机: 建连/失败/退避重连/热更新

场景: Subchannel 内部的连接怎么管理?
源码路径:
- **结构** (InternalSubchannel.java:72-79): TransportProvider + transportFactory 注入 + BackoffPolicy.Provider
- **startNewTransport** (L247-278): `syncContext.throwIfNotInThisSynchronizationContext` (L249) → 地址索引 (L256) → **代理解包** (L260-263, HttpConnectProxiedSocketAddress) → authority 覆盖 (L265-267) → 建传输 (L276)
- **失败重连** (L305-322): 全地址失败 → TRANSIENT_FAILURE (L309) → `reconnectPolicy.nextBackoffNanos()` (L315-319, **G-6 退避复用**) → "Will reconnect after {} ns"
- **热更新** (L373): updateAddresses; 状态回调 (L368) → LB 的 updateBalancingState
关键设计 (q4): Subchannel = **单地址连接管理者**: 建传输 (代理/authority 注入) → 状态回调 → 失败退避 → 地址热更新。**被放弃的方案: Subchannel 持有多个连接** — 每地址一 Subchannel 让"连接粒度 = 地址粒度", 上层策略精确控制。 [跨域: G-3 syncContext/代理; G-6 退避]

### 5. RoundRobin — 组合设计: 子 PickFirst + 轮询选择

场景: 轮询策略自己管理连接吗?
源码路径:
- **MultiChildLoadBalancer 基类** (MultiChildLoadBalancer.java:55-106): childLbStates + **默认子策略 PickFirst** (L106) + createChildAddressesMap (L87, 按 key 拆地址) + 抽象 updateOverallBalancingState (L80)
- **RoundRobin** (RoundRobinLoadBalancer.java:30-37): `sequence = new AtomicInteger(new Random().nextInt())` (L36, 随机起点) 
- **updateOverallBalancingState** (RoundRobinLoadBalancer.java:55-72): 无 READY → CONNECTING/TRANSIENT_FAILURE (L61-70); 有 READY → **READY + 轮询 Picker** (L72)
- **状态去重** (L64-67): 状态/Picker 未变不上报
关键设计 (q5): gRPC 轮询 = **组合**: MultiChild 管子策略生命周期 (每个地址组一个子 LB, 默认 PickFirst), RoundRobin 只做"选哪个 READY 子流"。**被放弃的方案: 自己实现连接管理** — 与 PickFirst 重复; 组合让 xds 的 ClusterManager 等全部复用同一骨架。 [跨域: G-7 多子策略同基类] [模式: 组合复用]

### 6. OutlierDetection — 统计驱逐: 坏地址自动出局

场景: 有个地址持续失败, 轮询还选它吗?
源码路径:
- **SuccessRateEjection** (OutlierDetectionLoadBalancer.java:806-860): 请求量过滤 (L810-815) → 成功率 mean/stdev → `requiredSuccessRate = mean - stdev * (stdevFactor / 1000f)` (L835) → 低于阈值 eject (L837-838)
- **概率执行** (L856-858): `new Random().nextInt(100) < enforcementPercentage` — 留观察样本
- **上限** (L842-848): maxEjectionPercent — "This behavior matches what Envoy proxy does"
- eject → subchannel.eject() (L373-378); 定时检测+uneject (L209-212)
关键设计 (q6): **统计自适应阈值**: 固定阈值 (成功率<90%) 无法适应集群健康波动; mean-stdev 阈值自适应; 概率执行防误伤全灭。源自 Envoy outlier detection 规范。**对照: HealthCheckingLoadBalancerFactory (services 模块, 511 行)** — 主动健康检查 (LB 包装器, 用健康探测结果过滤 Subchannel) vs 本节的被动统计驱逐 (观察成功率); 两种"健康感知"路线。 [算法: 均值-标准差] [跨域: G-7 Envoy 生态同源]

### 核心悬念

"核心只有 PickFirst, 高级策略 (一致性哈希/加权轮询/同 Zone) 全在 xds 模块 — 它们怎么接入这套 SPI?" 下一域 [[G-7-xDS]] 将看到 MultiChild 基类的家族谱系与 xds 控制面驱动的策略族。

### 负面空间 (不做)

1. 不写 ConnectivityState 全状态机细节 (G-3 已覆盖)
2. 不写 LoadBalancerRegistry/Provider 的 SPI 加载机制穷举
3. 不写 HealthCheckingLoadBalancerFactory 细节 (对照面)
4. 不写 OOB channel 场景 (G-7 xds 控制面)
5. 不写 GracefulSwitch/RandomSubsetting 细节 (同名策略族, 一行带过)
6. 不写服务的 DNS 地址来源 (G-5 专属)
