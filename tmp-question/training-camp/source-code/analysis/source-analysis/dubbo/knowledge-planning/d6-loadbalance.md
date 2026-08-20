# D-6 负载均衡 — 6 算法与权重预热

> 项目: Dubbo | 🟡 Deep / 1 篇 | AbstractLoadBalance+Random/RoundRobin/LeastActive/ConsistentHash/ShortestResponse/Adaptive
> 基线: DUBBO-PLAN D-6 (治理, 数字修正 5→6) — 前置: **D-1 (SPI) + D-3 (invoker 概念)** — 展开 抽象→随机族→状态族→自适应

---

## §0.8

- 🟡 Deep，1篇 — 抽象(**LoadBalance @SPI(RandomLoadBalance.NAME) L36 默认 random; AbstractLoadBalance.select L51-60[空→null/单节点→直接返回/doSelect]; getWeight L72-100[方法级权重 WEIGHT_KEY 默认 100+预热 TIMESTAMP→WARMUP 10min→calculateWarmupWeight L46-50 线性爬坡 min1])**) → 随机族(**Random: needWeightLoadBalance 快路径 L108-130[多注册/方法权重/预热三条件]→前缀和数组→随机落点+sameWeight 优化; RoundRobin: 平滑 WRR[increaseCurrent 选最大→sel 减总权重]+WeightedRoundRobin 缓存+RECYCLE_PERIOD 60000 回收**) → 状态族(**LeastActive: RpcStatus 活跃数[ActiveLimitFilter CAS 维护]+同活跃权重加权; ConsistentHash: 160 虚拟节点[address+i md5 4 段]+identityHashCode 失效重建+hash.arguments 默认 "0"[L85 首参]; ShortestResponse: 滑动窗口 succeeded/elapsed**) → 自适应(**AdaptiveLoadBalance: P2C 随机两样本取优[selectByP2C L62-90]+chooseLowLoadInvoker L107-130[getLoad(serviceKey,weight,timeout)]+AdaptiveMetrics 公式[CPU×√EWMA×inflight+超时惩罚+防饿死 L49-80]**) → 调用点(**AbstractClusterInvoker.select L156-178: sticky 粘滞[CLUSTER_STICKY_KEY 默认 false]+selected 重选[D-7 协作]**)
- 设计模式: [模式: 算法族+模板方法+权重预热]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| LoadBalance.java:36 | 默认 | **@SPI(RandomLoadBalance.NAME)** — 默认 random; SPI 注册表 6 算法 | High |
| AbstractLoadBalance.java:46-50 | 预热 | **calculateWarmupWeight: uptime/(warmup/weight) 线性爬坡, min 1** | High |
| AbstractLoadBalance.java:72-100 | 权重 | **getWeight: 方法级权重 + 预热判断 + Math.max(weight,0)**; DEFAULT_WEIGHT=100/DEFAULT_WARMUP=10min | High |
| RandomLoadBalance.java:108-130 | 快路径 | **needWeightLoadBalance 三条件**: 多注册中心权重/方法权重/预热时间戳 | High |
| RoundRobinLoadBalance.java:97-145 | 平滑 | **平滑 WRR: increaseCurrent→选最大→sel(totalWeight); RECYCLE_PERIOD 60s 回收** | High |
| ConsistentHashLoadBalance.java:84-92 | 哈希 | **replicaNumber=160 虚拟节点 + address+i md5 4 段 → TreeMap 环** | High |
| ConsistentHashLoadBalance.java:85 | 键 | **hash.arguments 默认 "0"** — 只哈希第 0 个参数 | High |
| AdaptiveLoadBalance.java:62-130 | P2C | **selectByP2C 随机两样本 + chooseLowLoadInvoker 取优** | High |
| AdaptiveMetrics.java:49-80 | 公式 | **load = CPU × (√EWMA+1) × inflight** + 超时惩罚 (timeout×2) + pickTime 防饿死 | High |
| AbstractClusterInvoker.java:155-185 | 调用 | **sticky 粘滞 (默认 false) + doSelect + selected 重选** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 负载均衡是算法集合 (统一抽象+6 算法) — 1篇按"抽象→随机族→状态族→自适应"展开; 调用点粘滞/重选与 D-7 协作 (导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 权重+预热统一抽象 (getWeight) | 🔴 | **为什么🔴**: 算法共用基础 |
| P1-2 | 平滑 WRR (nginx 式) | 🔴 | **为什么🔴**: 主流算法 |
| P1-3 | 一致哈希 160 虚拟节点 | 🔴 | **为什么🔴**: 确定性路由 |
| P1-4 | Adaptive P2C + 多维负载公式 | 🔴 | **为什么🔴**: 3.x 自适应 |
| P2-1 | LeastActive 活跃数 (RpcStatus) | 🟡 | **为什么🟡**: 即时负载 |
| P2-2 | ShortestResponse 滑动窗口 | 🟡 | **为什么🟡**: 历史负载 |
| P3-1 | sticky 粘滞 + selected 重选 | 🟢 | **为什么🟢**: 调用点细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **抽象+权重预热** | 🔴 | 基础 |
| B | **随机族** (Random/WRR) | 🔴 | 主流 |
| C | **状态族** (Least/CH/Shortest) | 🟡 | 状态感知 |
| D | **自适应+调用点** | 🟡 | 3.x/协作 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 抽象面 | SPI 默认 random 可插拔 (loadbalance= 参数); select 模板 (空/单节点); getWeight 方法级权重+预热爬坡 (新节点不压垮, DEFAULT_WEIGHT=100/预热 10min) | LoadBalance.java:36; AbstractLoadBalance.java:46-100 |
| q2 | 随机族 | Random 前缀和区间+sameWeight 快路径+needWeightLoadBalance 三条件; **RoundRobin 平滑 WRR (nginx 式: current+=weight 选最大→-=totalWeight) 高权重不连续命中; WRR 60s 回收** | RandomLoadBalance.java:60-130; RoundRobinLoadBalance.java:97-145 |
| q3 | 状态族 | LeastActive 活跃数 (ActiveLimitFilter CAS 维护的 RpcStatus); **ConsistentHash 160 虚拟节点+默认只哈希第 0 参数 (hash.arguments="0")** 同 key 同节点; ShortestResponse 滑动窗口平均响应 | LeastActiveLoadBalance.java:63-64; ConsistentHashLoadBalance.java:84-92; ShortestResponseLoadBalance.java:75-76 |
| q4 | 自适应 | **P2C: 随机两样本取负载低 (O(1) 优于遍历); 负载公式 = CPU × (√EWMA+1) × inflight (三正交维度) + 超时惩罚 + pickTime 防饿死**; 调用点 select (D-6 面) vs doInvoke (D-7 面) 归属清晰 | AdaptiveLoadBalance.java:62-130; AdaptiveMetrics.java:49-80 |

→ 引出 D-7 集群容错: AbstractClusterInvoker.select + selected 重选 (Failover 协作)。
