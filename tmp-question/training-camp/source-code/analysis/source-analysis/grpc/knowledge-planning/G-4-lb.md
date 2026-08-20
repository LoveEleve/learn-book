# G-4 负载均衡 — 知识规划 (KP)

> 域级: 🟡 B | 模块: api/LoadBalancer (1626) + PickFirst 族 (202/915) + InternalSubchannel (897) + AutoConfiguredLoadBalancerFactory (218) + util 家族 (RoundRobin 182/MultiChild 414/OutlierDetection 1165) + services 对照 (HealthCheckingLoadBalancerFactory 511)
> 日期: 2026-08-16 | 版本: 1.83.1 | Pass 2 闭环: q1(SPI) q2(AutoConfigured) q3(PickFirst 两代) q4(InternalSubchannel) q5(RoundRobin/MultiChild) q6(OutlierDetection)

## 一、机制提取 (逐源)

### M1 LoadBalancer SPI (q1)
- Helper.createSubchannel (LoadBalancer.java:1050-1060): "logical connection to ... addresses ... considered equivalent" (EAG); 必须 syncContext 调用 (L1062-1063)
- SubchannelPicker.pickSubchannel (L461-463): 每 RPC 选址
- updateBalancingState (L1183); Factory (L1558); createOobChannel (L1072+)

### M2 AutoConfigured 策略委托 (q2)
- AutoConfiguredLoadBalancerFactory (41-49): LoadBalancerRegistry + defaultProvider
- 找不到 → FixedPicker(TRANSIENT_FAILURE) + 报错 (L51-56)
- acceptResolvedAddresses → PolicySelection → delegate 切换 (L89-105); parseLoadBalancingPolicyConfig (L186-191)
- 默认 PickFirst (测试 defaultIsPickFirst L130)

### M3 PickFirst 两代 (q3)
- 旧 (PickFirstLoadBalancer 40): 单 Subchannel; shuffle 配置 (L60-67)
- 新 (PickFirstLeafLoadBalancer 60): 每地址一 Subchannel (Map L69) + Index (L70) + **Happy Eyeballs** (L64-65, CONNECTION_DELAY_INTERVAL_MS=250 L63) + 退避重连 (L78-79)
- requestConnection 逐地址 (L508-520)

### M4 InternalSubchannel (q4)
- 单地址连接管理者 (72-79): transportFactory 注入; startNewTransport (L247-278, syncContext 强制 L249, 代理解包 L260-263, authority 覆盖 L265-267)
- 失败 → TRANSIENT_FAILURE (L309) + 退避重连 (L315-322, nextBackoffNanos)
- onStateChange 上报 (L368); updateAddresses 热更新 (L373); shutdown (L438)

### M5 RoundRobin/MultiChild (q5)
- MultiChildLoadBalancer (55-106): childLbStates + 默认子策略 PickFirst (L106) + createChildAddressesMap (L87)
- RoundRobin (30-67): sequence 随机起点 (L36) + READY 子流轮询 (L55, L60+) + 状态去重 (L64-67)

### M6 OutlierDetection (q6)
- SuccessRateEjection (L806-860): requestVolume/minimumHosts 过滤 → mean/stdev → requiredSuccessRate (L835) → 低于阈值 eject (L852-853) + enforcementPercentage 概率 (L856-858) + maxEjectionPercent (L842-848)
- eject → subchannel.eject() (L373-378); FailurePercentageEjection (L557); 定时检测+uneject (L209-212)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 SPI 三方协作 / M3 PickFirst 两代+Happy Eyeballs / M4 连接状态机 |
| P2 | M2 策略委托 / M5 组合复用 |
| P3 | M6 离群检测 (对照面) |

## 三、叙事线

场景: 解析器给了 5 个地址, 通道怎么选?读者疑问链: 谁能参与选址 (M1 SPI) → 默认选什么策略 (M2) → PickFirst 怎么连 (M3/M4) → 多地址轮询 (M5) → 坏的地址怎么办 (M6)。
