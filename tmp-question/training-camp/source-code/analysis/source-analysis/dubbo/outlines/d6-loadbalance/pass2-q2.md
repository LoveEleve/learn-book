# D-6 负载均衡 — Pass 2 闭环 Q2: 随机族 (Random 加权 + RoundRobin 平滑)

> 核心: RandomLoadBalance + RoundRobinLoadBalance | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 加权随机怎么实现? 平滑加权轮询 (nginx WRR) 怎么避免连续打同一节点?**

## 机制链 (已实证)

```
RandomLoadBalance.doSelect:
├── needWeightLoadBalance 判断 — 无权重时 ThreadLocalRandom 直接随机 (快路径)
├── 权重前缀和数组: weights[i] = Σ(0..i) — 累计区间
├── sameWeight 优化: 等权 → 直接随机
└── 随机数落在 [0, totalWeight) → 二分/线性找区间 → 选 invoker (加权)

RoundRobinLoadBalance (平滑加权轮询, nginx WRR 变体):
├── 每 invoker 一个 WeightedRoundRobin (map: identifyString → WRR, computeIfAbsent)
├── 算法三步:
│   1. increaseCurrent(): current += weight (每轮递增)
│   2. 选 current 最大的 invoker (maxCurrent 遍历)
│   3. sel(totalWeight): current -= totalWeight (选中后扣减)
│   → 平滑: 高权重节点不会被连续选中, 分布均匀
├── 权重变化检测: weight != WRR.getWeight → setWeight (动态生效)
├── RECYCLE_PERIOD = 60000 (L38): 60s 未更新的 WRR 从 map 回收 (removeIf lastUpdate 过期)
└── 返回 selectedInvoker (current 已被扣减)
```

## 关键设计 (why)

1. **加权随机 = 前缀和区间**: 权重转区间, 随机落点 — O(n) 简单可靠
2. **sameWeight 快路径**: 等权场景跳过加权计算 — 性能
3. **平滑 WRR**: current 递增 + 选中扣减总权重 — **高权重不会连续打同一节点** (对比朴素轮询)
4. **WRR 状态缓存**: identifyString → WeightedRoundRobin 复用, 权重变化检测更新 — 无状态重建
5. **60s 回收**: 失效节点 WRR 自动清理 — 防 map 无限膨胀

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 前缀和 + sameWeight 优化 | RandomLoadBalance.java:62-90 |
| WeightedRoundRobin 内部类 | RoundRobinLoadBalance.java:43-61 |
| sel (current -= total) | RoundRobinLoadBalance.java:61-63 |
| 选最大 current + 回收 | RoundRobinLoadBalance.java:97-145 |
| RECYCLE_PERIOD 60000 | RoundRobinLoadBalance.java:38 |

## 负面空间 (Q2 面)

- 不做带权会话保持 (WRR 无会话绑定)
- 不做预热特判 (预热已由 getWeight 统一处理)
- 不做一致性保证 (随机/轮询非确定 — 一致性由 ConsistentHash 提供)
