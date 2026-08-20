# G-7 xDS 控制面与高级负载均衡 — 知识规划 (KP)

> 域级: 🟡 B | 模块: xds/ (184: XdsNameResolver 1206/XdsClientImpl 1102/client 20/RingHash 575/LeastRequest 348/WRR 903/WrrLocality/Cds 651/ClusterImpl 571/Priority 368/XdsServerWrapper 960/ExternalProcessorClientInterceptor 1355/matcher 22) + grpclb 对照
> 日期: 2026-08-16 | 版本: 1.83.1 | Pass 2 闭环: q1(控制面) q3(RingHash) q4(WRR) q5(分层) q6(WrrLocality)

## 一、机制提取 (逐源)

### M1 控制面协议 (q1)
- XdsClientImpl.watchXdsResource (L251-280): syncContext 订阅 → resourceSubscribers + typeUrl 注册 → manageControlPlaneClient → adjustResourceSubscription
- XdsResourceType (L41-77): typeUrl; **LDS/CDS 全量 vs RDS/EDS 增量** (L76-77)
- ControlPlaneClient (L59): versions Map (L74-77, version_info 协商) + **ACK (L215)/NACK (L231, errorDetail)** + 退避重连 (L89-90) + sendDiscoveryRequests 全量重发 (L291-306)

### M2 RingHash (q3)
- Ketama 注释 (L60-64): 环 + 顺时针 + **1/N 影响**
- buildRing (L323-350): scale × normalizedWeight 虚拟节点 (L339-349) + "Per GRFC A61 use the first address" (L337) + sort
- pickSubchannel (L416-450): 哈希三来源 (RPC_HASH_KEY L425-428/请求头 L430-433/随机 L435) + 顺时针 (L440) + **粘性 TF 跳过** (L443-447)
- XxHash64 (L71); 默认子策略 PickFirst (L73)

### M3 WeightedRoundRobin (q4)
- 配置 (L70-75 注释): oobReportingPeriod 10s/weightExpirationPeriod 180s/weightUpdatePeriod 1s/errorUtilizationPenalty
- OrcaReportListener (L365-381): EPS > 0 惩罚权重
- **WeightedRandomPicker** (L113-140): totalWeight=0 均匀 (L116-119); 累积权重扫描 (L121-130, 明说不用二分)
- 权重失效回退 RR (L119 注释)

### M4 分层策略 (q5)
- CdsLoadBalancer2 (L77-81): 集群发现入口; 动态集群订阅 (L120-125)
- ClusterManagerLoadBalancer (L51-127): **extends MultiChildLoadBalancer** + deletionTimer
- ClusterImplLoadBalancer (L78-144): EDS 消费 + drop 统计 (L144)

### M5 WrrLocality (q6)
- WrrLocalityLoadBalancer (L44-80): GracefulSwitchLoadBalancer (L48) + ATTR_LOCALITY_NAME/WEIGHT (L77-78) → 加权 target 组合 (L69)
- "同 Zone 优先" = 权重倾斜非硬路由

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 控制面订阅-ACK/NACK / M2 RingHash 一致性哈希 |
| P2 | M3 WRR 负载驱动权重 / M4 分层组合树 |
| P3 | M5 WrrLocality 同 Zone / 过滤器面 / 服务端面 (对照) |

## 三、叙事线

场景: `xds:///my-service` — 一个 target, 背后是控制面驱动的整套服务网格。读者疑问链: 控制面怎么订阅 (M1) → 地址/策略怎么来 (M1/M4 分层) → 一致性哈希怎么做 (M2) → 权重怎么动态 (M3) → 同 Zone 怎么偏 (M5)。
