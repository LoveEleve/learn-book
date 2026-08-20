# D-6 负载均衡 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-cluster loadbalance/ 7 文件)
> 09 域级审计: 执行计划 DB-6 断言 "LoadBalance—Random/RoundRobin/LeastActive/ConsistentHash/ShortestResponse" (5 算法) + PLAN 修正 (→6, +Adaptive) — 已穷举, 见文末审计表

## 入口展开 (Level-1~2, 已读源码)

### Level-1: LoadBalance SPI + AbstractLoadBalance.select 模板

```
LoadBalance @SPI(RandomLoadBalance.NAME) — 默认 random (LoadBalance.java:36)
SPI 注册表穷举 (6 算法, internal 文件实证):
├── random=RandomLoadBalance (默认)
├── roundrobin=RoundRobinLoadBalance
├── leastactive=LeastActiveLoadBalance
├── consistenthash=ConsistentHashLoadBalance
├── shortestresponse=ShortestResponseLoadBalance (3.x)
└── adaptive=AdaptiveLoadBalance (3.x)

AbstractLoadBalance.select (L51-60) — 模板方法:
├── invokers 空 → null
├── size==1 → 直接返回 (无选择)
└── doSelect(invokers, url, invocation) — 各算法实现
```

### Level-2: getWeight + calculateWarmupWeight (权重/预热)

```
getWeight (L72-100):
├── 方法级权重: url.getMethodParameter(method, WEIGHT_KEY, DEFAULT_WEIGHT)
├── 预热: TIMESTAMP_KEY (启动时间) → uptime < WARMUP_KEY → calculateWarmupWeight
└── return Math.max(weight, 0)

calculateWarmupWeight (L46-50): ww = uptime / (warmup/weight) — 线性爬坡, min 1, max weight
```

### Level-3: 6 算法核心 (穷举实证)

```
Random (RandomLoadBalance.java):
├── needWeightLoadBalance 判断 (无权重 → 直接 ThreadLocalRandom)
├── 权重前缀和数组 weights[i] (累计) → 随机落在区间选 invoker
└── sameWeight 优化 (等权直接随机)

RoundRobin (平滑加权轮询):
├── WeightedRoundRobin (per-invoker 权重轮询对象) + sel(totalWeight)
├── RECYCLE_PERIOD = 60000 (L38) — 60s 回收未更新的 WRR
└── 平滑性: 避免短时间连续打同一节点

LeastActive (LeastActiveLoadBalance.java:44-46):
├── leastActive 遍历 — 找活跃调用数最少的
└── 活跃相同 → 权重加权选

ConsistentHash (ConsistentHashLoadBalance.java):
├── ConsistentHashSelector + ConcurrentMap selectors 缓存 (L46)
├── identityHashCode 对比 — invoker 列表变化 → 重建 selector (L56-64)
└── TreeMap 虚拟节点 (160 个/invoker?)

ShortestResponse (ShortestResponseLoadBalance.java):
├── succeeded/elapsed offset (L75-76) — 滑动窗口响应统计
└── 选响应时间最短的

Adaptive (AdaptiveLoadBalance.java:36-51) — 3.x 自适应:
├── attachmentKey = "mem,load" (L41) — 指标附件
├── selectByP2C (L62) — **Power of Two Choices: 随机选 2 个, 取负载低的**
└── load 比较 (L112-117: doubleToLongBits)
```

## 09 域级审计表 (执行计划 DB-6 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "Random/RoundRobin/LeastActive/ConsistentHash/ShortestResponse" (5) | ls loadbalance/ = Abstract + **6 算法** (含 Adaptive) | 修正: 5→6 (PLAN 已记录) |
| 执行计划未提: 默认算法 | @SPI(RandomLoadBalance.NAME) — random 默认 | 补锚 |
| 执行计划未提: 权重/预热 | getWeight + calculateWarmupWeight (L46-50, 72-100) | 补锚 |
| 执行计划未提: 平滑轮询 | WeightedRoundRobin + RECYCLE_PERIOD 60000 | 补锚 |
| 执行计划未提: 自适应 P2C | AdaptiveLoadBalance selectByP2C (L62) + mem,load 指标 | 补锚 |
| 执行计划未提: 一致性哈希缓存 | selectors ConcurrentMap + identityHashCode 失效 | 补锚 |

## 待展开 (下一层)

1. ConsistentHash 虚拟节点数 + 哈希环细节 (TreeMap)
2. ShortestResponse 滑动窗口统计细节 (SucceededResponseTimeWindow)
3. RoundRobin 平滑算法数学 (WRR sel 逻辑)
4. Adaptive P2C 完整逻辑 (负载计算/权重)
5. LoadBalance 在哪里被调用 (AbstractClusterInvoker/AbstractCluster — D-7 关联点)
