# D-6 负载均衡 — SPI→权重预热→6 算法→调用点

> 前置: [[D-1-SPI微内核]] [[D-3-服务引用]] | 引出: [[D-7-集群容错]] | 对照: Nginx 负载均衡 + Ribbon 认知
> 🟡 B | 8 KP | [模式: 算法族 + 权重/预热]
> Pass 2 闭环: q1(抽象面) q2(随机族) q3(状态族) q4(Adaptive+调用点)

**读者处境**: 集群里选哪个 provider 调? 权重/预热怎么影响? 3.x 自适应是什么? 这篇拆 AbstractLoadBalance + 6 算法 + 调用点。

### 1. 抽象面 — SPI 默认 random + 模板 + 权重预热

场景: 负载均衡怎么抽象? 权重怎么算?
源码路径:
- **LoadBalance @SPI(RandomLoadBalance.NAME)** (LoadBalance.java:36) — 默认 random; **SPI 注册表 6 算法** (random/roundrobin/leastactive/consistenthash/shortestresponse/adaptive)
- **AbstractLoadBalance.select 模板** (L51-60): 空 → null / 单节点 → 直接返回 / doSelect
- **getWeight** (L72-100): 方法级权重 (WEIGHT_KEY, 默认 **DEFAULT_WEIGHT=100** Constants.java:29) → **预热**: TIMESTAMP_KEY uptime < WARMUP (**DEFAULT_WARMUP=10min** L99) → **calculateWarmupWeight** (L46-50: 线性爬坡 min 1 max weight) → Math.max(weight, 0)
关键设计 (q1): **可插拔算法 + 方法级权重 + 预热爬坡 (新节点不压垮)**。[模式: 抽象面]

### 2. 随机族 — Random 加权 + RoundRobin 平滑

场景: 两个最常用算法怎么实现?
源码路径:
- **RandomLoadBalance**: **needWeightLoadBalance 快路径** (L108-130: 多注册中心权重/方法权重/**预热时间戳**三条件 — 无权重无预热直接 ThreadLocalRandom) → **权重前缀和数组** (weights[i] 累计区间) → 随机落点; sameWeight 优化
- **RoundRobinLoadBalance 平滑 WRR** (nginx 变体): 每 invoker 一个 **WeightedRoundRobin** (computeIfAbsent 缓存) → **increaseCurrent (current+=weight) → 选最大 → sel(totalWeight) (current-=totalWeight)** → 平滑分布; 权重变化检测; **RECYCLE_PERIOD 60000 回收** (L38)
关键设计 (q2): **前缀和加权随机 + nginx 式平滑轮询 (高权重不连续命中)**。[模式: 随机族]

### 3. 状态族 — LeastActive + ConsistentHash + ShortestResponse

场景: 三种"状态感知"算法分别感知什么?
源码路径:
- **LeastActiveLoadBalance** (L44-46): leastActive 遍历 → 同活跃权重加权; ⚠ **活跃数数据源 = RpcStatus.getStatus(url, method).getActive()** (L63-64) — 由 **ActiveLimitFilter (D-4 Filter 面) CAS +1 维护** (RpcStatus L115)
- **ConsistentHashLoadBalance**: ConsistentHashSelector + **selectors 缓存** (L46) + **identityHashCode 失效重建** (L56-64) + **环构建** (L88-110: address+i 的 md5 → 4 段哈希 → TreeMap, **160 虚拟节点/invoker**) + **select 键 = hash.arguments 参数子集** (⚠ **默认 "0" 只哈希第 0 个参数** L85, HASH_ARGUMENTS 可配) → 同参数恒同节点
- **ShortestResponseLoadBalance** (3.x): SucceededResponseTimeWindow 滑动窗口 (succeeded/elapsed offset L75-76) → 选平均响应最短
关键设计 (q3): **即时负载 (活跃数) vs 确定性 (哈希) vs 历史负载 (响应窗口)**。[模式: 状态族]

### 4. Adaptive P2C + 调用点

场景: 3.x 自适应是什么? 负载均衡在哪被触发?
源码路径:
- **AdaptiveLoadBalance** (3.x, L36-51): attachmentKey "mem,load" 指标附件 → **selectByP2C** (L62-90: **Power of Two Choices** — 随机 2 个取负载低) → **chooseLowLoadInvoker** (L107-130: **load = adaptiveMetrics.getLoad(serviceKey, weight, timeout)**)
- ⚠ **AdaptiveMetrics 负载公式** (rpc/AdaptiveMetrics.java:49-80): **load = providerCPULoad × (√EWMA + 1) × inflight** — EWMA 延迟 (beta=0.5) + 在途请求 (consumerReq−success−error) + 超时惩罚 (timeout×2) + **pickTime 超时 2 倍 → 强制 0** (必选)
- **调用点** (AbstractClusterInvoker.java:156-178): select(loadbalance, invocation, invokers, **selected**) — 每次调用选节点; 注释 L140-141: 选中的在 selected 列表/不可用 → **重选** (Failover 重试协作)
关键设计 (q4): **P2C O(1) 随机两样本取优 + 重选逻辑 (与 D-7 重试协作)**。[模式: 自适应]

## 代码类型
Algorithm (6 算法)

## 负面空间 (D-6, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不全局负载状态 | 各算法独立, 无共享统计 (q1) |
| 不预热加速 | 线性爬坡, 无指数 (q1) |
| 不权重动态下发 | 权重静态 URL 参数 (q1) |
| 不带权会话保持 | WRR 无会话绑定 (q2) |
| 不活跃数衰减 | LeastActive 活跃数为瞬时值 (q3) |
| 不替代 D-7 重试 | 只负责选, 不负责重试 (q4) |

## 结尾桥 OUTBOUND

- → [[D-7-集群容错]]: AbstractClusterInvoker.select + selected 重选 — 容错策略深潜
- → 对照: Nginx 平滑加权轮询 (同源算法) / Ribbon 负载均衡
