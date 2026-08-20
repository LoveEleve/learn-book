# G-7 xDS — 控制面驱动的负载均衡: 从 xds:/// target 到一致性哈希

> 前置: [[G-4-负载均衡]] (MultiChild/PickFirst 复用) + [[G-5-命名解析]] (XdsNameResolver) + [[G-3-客户端]] (syncContext) | 引出: [[G-8-RLS]] (数据面路由对照) | 对照: Envoy xDS + grpclb (前身)
> 🟡 B | 6 KP | [模式: 订阅-推送 + 组合树 + 一致性哈希]
> Pass 2 闭环: q1(控制面) q3(RingHash) q4(WRR) q5(分层) q6(WrrLocality)

**读者处境**: `ManagedChannelBuilder.forTarget("xds:///my-service")` — 地址从哪来?不是 DNS, 是一个叫"控制面"的服务。它下发的不是一个地址列表, 而是一棵树: 集群 → 子集群 → 端点 → 负载均衡算法。这套"服务网格"机制怎么运转?

### 1. 控制面 — 订阅-推送与 ACK/NACK

场景: 客户端怎么从控制面拿配置?拿错了怎么办?
源码路径:
- **XdsNameResolver 入口** (XdsNameResolver.java:89-95): "Resolving a gRPC target involves **contacting the control plane management server via xDS protocol**" — 与 DnsNameResolver 的本质差异: 地址源是控制面; 订阅 LDS/RDS (L209-248) 产出 **ConfigSelector** (L128, 含路由/策略) + **RPC_HASH_KEY** (L101-102, RingHash 消费)
- **watchXdsResource** (XdsClientImpl.java:251-280): syncContext 内订阅 (L252) — resourceSubscribers (类型→名→watcher) + typeUrl 注册 (L98) → manageControlPlaneClient (L273, 按 authority 建控制面连接) → adjustResourceSubscription (L277)
- **资源类型** (XdsResourceType.java:41-77): typeUrl 抽象 (L70); **LDS/CDS 全量 vs RDS/EDS 增量** (L76-77: "For LDS and CDS resources, the server must return all resources... For RDS and EDS, the server may only return")
- **version 协商** (ControlPlaneClient.java:74-77): versions Map — "Last successfully applied version_info for each resource type"
- **ACK/NACK** (L215, L231): ACK 带 nonce+version; NACK 带 errorDetail — 错误显式反馈控制面
- **断线重连**: 退避 (ControlPlaneClient.java:84,480-483) + 全量重发 (L291-306)
关键设计 (q1): **订阅-推送协议**: 客户端声明订阅 → 服务端经 ADS 长连接推送 (全量/增量按类型) → ACK/NACK 确认 (nonce 对应) → version_info 同步。**被放弃的方案: 客户端轮询** — 长连接推送免轮询延迟与负载; NACK 让配置错误显式可见。 [协议: xDS v3/ADS] [跨域: G-3 syncContext/G-6 退避]

### 2. RingHash — Ketama 一致性哈希

场景: 缓存类服务要求"同一 key 永远到同一后端", 后端增删怎么办?
源码路径:
- **算法注释** (RingHashLoadBalancer.java:60-64): "maps hosts onto a circle (the ring)... finding the nearest corresponding host clockwise... the addition or removal of one host from a set of N hosts will affect only 1/N requests"
- **buildRing** (L323-350): 每服务器 `targetHashes += scale * normalizedWeight` (L339) → **虚拟节点循环** (L340-349, 节点数 ∝ 权重) → "Per GRFC A61 use the first address for the hash" (RingHashLoadBalancer.java:331) → sort (L350)
- **pickSubchannel** (L416-450): 请求哈希三来源: **RPC_HASH_KEY** (xds config selector 生成, L425-428) / **请求头** (L430-433) / **随机** (L435) → **顺时针扫描** (L440) → **粘性 TF 跳过** (L443-447, "we ignore all TF subchannels")
- 哈希函数: **XxHash64** (L71)
关键设计 (q3): 一致性哈希核心保证: **增删节点只影响 1/N 请求** (L64 注释); 虚拟节点解决权重与哈希倾斜。**被放弃的方案: 模 N 哈希** — 增删节点全量重映射。 [算法: 一致性哈希/xxHash64] [跨域: G-4 MultiChild 基类]

### 3. WeightedRoundRobin — 负载报告驱动的动态权重

场景: 权重是配死的吗?端点负载不均怎么办?
源码路径:
- **配置面** (WeightedRoundRobinLoadBalancer.java:70-75): `enableOobLoadReport: true` / `blackoutPeriod: 10s` / `oobReportingPeriod: 10s` / `weightExpirationPeriod: 180s` / `weightUpdatePeriod: 1s` / `errorUtilizationPenalty: 1.0`
- **权重来源**: OrcaReportListener (L365-381) — 端点带外上报负载, `report.getEps() > 0 && errorUtilizationPenalty > 0` 时按错误率惩罚权重 (L381)
- **加权随机** (WeightedRandomPicker.java:113-140): totalWeight=0 → 均匀 (L116-119); 否则 `rand = random.nextLong(totalWeight)` → **累积权重线性扫描** (L121-130, "Not using Arrays.binarySearch for better readability")
- **失效回退** (L119 注释): 权重过期 → 回退普通 RR
关键设计 (q4): **闭环加权**: 端点 orca 上报负载 → 权重动态调整 → 加权随机选择; 流量自然流向空闲端点。**被放弃的方案: 固定权重轮询** — 无法感知负载不均。 [算法: 加权随机/反馈控制] [跨域: G-7 orca 模块]

### 4. 分层策略 — CDS → ClusterManager → ClusterImpl → 叶子

场景: 一个 target 背后怎么组织这么多策略?
源码路径:
- **CdsLoadBalancer2** (CdsLoadBalancer2.java:77-81): "One instance per top-level cluster... a plain EDS/logical-DNS cluster or an aggregate cluster formed by a group of sub-clusters in a tree hierarchy" — 集群发现入口 (L120-125 动态订阅)
- **ClusterManagerLoadBalancer** (L51-127): **extends MultiChildLoadBalancer** (util, G-4 q5) + 子集群 deletionTimer (L126-127)
- **ClusterImplLoadBalancer** (L78-144): EDS 消费 (edsServiceName L99) + drop 统计 (L144, LRS)
- **叶子**: RingHash/WRR/LeastRequest — 全部 MultiChild + PickFirst 默认子策略
关键设计 (q5): **组合树**: 控制面资源 (CDS/EDS 分离) 天然分层, LB 层一一对应, 每层独立替换。**被放弃的方案: 单层巨策略** — 无法对应控制面数据模型。 [架构: 组合树] [跨域: G-4 MultiChild/G-8 挂 ClusterManager]

### 5. WrrLocality — 同 Zone 优先是权重倾斜

场景: 执行计划 G-4 的"优先同Zone"到底怎么实现?
源码路径:
- **类结构** (WrrLocalityLoadBalancer.java:44-54): GracefulSwitchLoadBalancer (L48, util) + LoadBalancerRegistry
- **权重提取** (L68-80): 地址属性 **ATTR_LOCALITY_NAME** (L77) + **ATTR_LOCALITY_WEIGHT** (L78) → localityWeights Map → "combined with the locality weights to produce the weighted target LB config" (L69)
- **"优先" = 权重**: 同 Zone locality 权重高 → 选中概率高; 非硬路由
关键设计 (q6): 同 Zone 亲和 = **权重倾斜**: 控制面下发 locality 权重 → WeightedTarget 组合; 本 Zone 全挂时自然降级到其他 Zone。**被放弃的方案: 硬路由 (只连同 Zone)** — 无兜底。执行计划 G-4 "优先同Zone" 的真实落点。 [分布式: 同 Zone 亲和] [跨域: G-4 主题落点]

### 6. 过滤器面与服务端 — 数据面拦截 (对照)

场景: 服务网格的故障注入/路由在哪?
源码路径:
- **RouterFilter** (xds 过滤器): 默认路由 (HTTP 语义)
- **FaultFilter**: 故障注入 (延迟/丢弃, 测试混沌); matcher/ 22 文件 = 路由匹配引擎 (MatcherTree/CelMatcher)
- **ExternalProcessorClientInterceptor** (1355): ExtProc 外部处理
- **服务端面**: XdsServerWrapper (960) — LDS/RDS 服务端 (XdsServerBuilder)
关键设计: 过滤器链与 xds 控制面联动 (每个过滤器由配置驱动); 服务端 xDS 让"服务端配置也控制面化"。 [跨域: G-2 拦截器对照] [生态: Envoy 过滤器模型]

### 核心悬念

"控制面管配置, 那'每次请求的服务端决策路由' (RLS) 呢?——它把路由决策外包给一个 RLS 服务器, 数据面自己用 LRU 缓存。" 下一域 [[G-8-RLS]] 揭示数据面+控制面的第三种分工。

### 负面空间 (不做)

1. 不写 xDS 协议 wire format (protobuf 消息细节)
2. 不写 bootstrap 配置 (grpc server 地址发现)
3. 不写 LRS/CSDS/RBAC 全细节 (对照面)
4. 不写 matcher CEL 表达式语言细节
5. 不写 PriorityLoadBalancer/LeastRequest 细节 (同族, 一行带过)
6. 不写 grpclb 协议 (时空对照, temporal-trace 提及)
