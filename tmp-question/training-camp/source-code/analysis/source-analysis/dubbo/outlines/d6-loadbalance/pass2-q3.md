# D-6 负载均衡 — Pass 2 闭环 Q3: 状态族 (LeastActive + ConsistentHash + ShortestResponse)

> 核心: 三个带状态算法 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 三种"状态感知"负载均衡分别感知什么? 一致性哈希怎么保证同 key 同节点?**

## 机制链 (已实证)

```
LeastActiveLoadBalance (最少活跃):
├── 遍历找 leastActive (活跃调用数最少) 的 invoker 集合
├── 活跃相同 → 权重加权随机选
└── 活跃数来自 RpcStatus (invoker 活跃计数, 调用中 +1 完成 -1)

ConsistentHashLoadBalance (一致性哈希):
├── ConsistentHashSelector + ConcurrentMap selectors 缓存 (L46, key=方法级)
├── identityHashCode 对比: invoker 列表变化 → 重建 selector (L56-64) — 环失效检测
├── replicaNumber = 160 虚拟节点 (L84, HASH_NODES 参数可配)
│   └── replicaNumber / 4 循环 (L92): md5 4 段哈希 → TreeMap 环
├── 同一 invocation 参数 (hash.arguments) → 哈希定位 → 最近虚拟节点
└── 特点: 同参数稳定打同节点 (缓存友好)

ShortestResponseLoadBalance (最短响应, 3.x):
├── SucceededResponseTimeWindow: succeeded/elapsed offset (L75-76) — 滑动窗口
├── 窗口内累计成功次数与总耗时 → 平均响应
└── 选平均响应最短的 (leastSucceededResponseTime) + 权重打破平局
```

## 关键设计 (why)

1. **LeastActive = 运行时感知**: 用活跃计数做"即时负载" — 慢节点活跃数高自然少选 (动态)
2. **ConsistentHash = 确定性**: 同 key 恒打同节点 — 缓存命中/有状态服务 (min 迁移, 节点增删只影响相邻)
3. **160 虚拟节点**: 平衡数据倾斜 (节点数量不均时均匀分布)
4. **identityHashCode 失效**: invoker 列表变化才重建环 — 避免每次调用重建
5. **ShortestResponse = 历史感知**: 滑动窗口平均响应 — 感知长期慢节点 (区别于 LeastActive 即时态)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| leastActive 遍历 + 权重平局 | LeastActiveLoadBalance.java:44-46 |
| selectors 缓存 + identityHashCode | ConsistentHashLoadBalance.java:46, 55-64 |
| replicaNumber 160 + md5 分段 | ConsistentHashLoadBalance.java:84, 92 |
| SucceededResponseTimeWindow | ShortestResponseLoadBalance.java:75-76 |

## 负面空间 (Q3 面)

- 不做一致性哈希的节点权重 (虚拟节点等权重)
- 不做活跃数衰减 (LeastActive 活跃数为瞬时值)
- 不做响应窗口自适应 (固定窗口)
