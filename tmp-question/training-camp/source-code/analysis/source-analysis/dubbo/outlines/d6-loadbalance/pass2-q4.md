# D-6 负载均衡 — Pass 2 闭环 Q4: Adaptive P2C + 调用点

> 核心: AdaptiveLoadBalance (3.x) + AbstractClusterInvoker 选择逻辑 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 3.x 自适应负载均衡是什么? 负载均衡在调用链哪里被触发?**

## 机制链 (已实证)

```
AdaptiveLoadBalance (AdaptiveLoadBalance.java:36-51) — 3.x 自适应:
├── attachmentKey = "mem,load" (L41) — 消费 provider 上报的内存/负载指标附件 (输入之一)
├── selectByP2C (L62-90) — **Power of Two Choices (P2C)**:
│   ├── length==1 → 直接返回
│   ├── length==2 → chooseLowLoadInvoker(两个)
│   └── 随机 2 个不同位置 (pos1, pos2 修正重叠) → chooseLowLoadInvoker
├── chooseLowLoadInvoker (L107-130): load = adaptiveMetrics.getLoad(serviceKey, weight, timeout)
│   ├── load 相同 → 权重随机平局 (L117-127)
│   └── load1 > load2 → 选 2 (L129)
└── ⚠ AdaptiveMetrics 负载公式 (rpc/AdaptiveMetrics.java:49-80, 三次 REVIEW 实证):
    load = providerCPULoad x (sqrt(EWMA) + 1) x inflight
    ├── EWMA 指数移动平均延迟 (beta=0.5): ewma = beta*ewma + (1-beta)*lastLatency
    ├── 超时惩罚: providerTime == currentTime → lastLatency = timeout * 2
    ├── inflight 在途: consumerReq - consumerSuccess - errorReq
    └── pickTime 超时 2 倍 → 强制返回 0 (必选, 防饿死)

调用点 (AbstractClusterInvoker.java:156-178) — D-7 关联:
├── select(loadbalance, invocation, invokers, selected) — 集群 invoker 每次调用选节点
├── 注释 (L140-141): "a) Firstly, select an invoker using loadbalance. If this invoker is in
│   previously selected list, or... (re-select)" — 失败重试时 selected 列表重选
└── doSelect (L178) → loadbalance.select
```

## 关键设计 (why)

1. **P2C 数学原理**: 随机两样本取优 — 期望负载远低于纯随机 (著名分布式负载均衡技巧, 同 Cassandra/Envoy 思路)
2. **多维负载公式**: **CPU x sqrt(EWMA) x inflight** — 三个正交维度 (节点资源/延迟趋势/在途并发), 比单指标鲁棒
3. **EWMA 平滑**: beta=0.5 指数移动平均 — 延迟波动不剧烈跳变
4. **防饿死 (pickTime)**: 节点超时 2 倍未统计 → 强制 load=0 必选 — 避免被永久跳过
5. **O(1) 复杂度**: 与节点数无关 (对比遍历式算法) — 大规模集群友好
6. **重选逻辑 (selected 列表)**: Failover 重试时跳过已选过的节点 (L156-178) — 负载均衡与容错协作

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Adaptive 类 + attachmentKey | AdaptiveLoadBalance.java:36-51 |
| selectByP2C 完整 | AdaptiveLoadBalance.java:62-90 |
| chooseLowLoadInvoker + 平局 | AdaptiveLoadBalance.java:107-130 |
| AdaptiveMetrics 负载公式 (CPU/EWMA/inflight/防饿死) | rpc/AdaptiveMetrics.java:49-80 |
| 调用点 select + selected 重选 | AbstractClusterInvoker.java:156-178 |

## 负面空间 (Q4 面)

- 不做全局负载拓扑 (P2C 无全量排序)
- 不做指标持久化 (附件即用即弃)
- 不替代 D-7 重试 (只负责选, 不负责重试)
