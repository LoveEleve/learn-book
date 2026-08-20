# E-4 Cluster Routing 篇 2/3 — 分配: 决策器与平衡器

> 前置: [[E-4-routing-01]] (路由表) | 复用: — | 对照: [[rd1-connection]] (连接池分配) | 引出: [[E-4-routing-03]] [[E-10-clusterstate]]
> 🔴 A | 来源: AllocationDecider.java:32-91 + allocation/decider/ (19 文件) + BalancedShardsAllocator.java:65-77,84-112,145-156 + AllocationService.java:388-410,541-553
> 定位: Routing 卷中篇 — 回答"分片怎么分配? 19 决策器怎么投票?"

**读者处境**: 新节点加入, 100 个未分配分片怎么落位? 面试官问 "ES 分片分配规则?" 你答 "决策器" — 但再问 "19 个决策器怎么组合? 平衡器怎么算?" 你卡住了。这篇是分配机制的完整答案。

### 1. 问题引入 — 分片怎么"落户"

场景: 节点加入/宕机 → 分片需要分配/重分配 — 谁决定放哪?
- AllocationService.reroute (AllocationService.java:388-410): 分配编排入口
- 本篇问题: 决策器投票 (Q4) / 平衡算法 (Q5) / 分配时序 (Q6)

### 2. 决策器 — 19 个投票者

场景: 一个分片能否放到某节点?
- AllocationDecider 接口 (AllocationDecider.java:32-91): canRebalance (AllocationDecider.java:32) / canAllocate (AllocationDecider.java:40,56) / canRemain (AllocationDecider.java:48) — **5 类判定**
- 19 个决策器: DiskThreshold (磁盘) / SameShard (同分片不重复) / ShardsLimit (每节点分片数) / NodeVersion / Throttling...
- 聚合: 任一 NO → 拒绝; 全 YES → 允许; THROTTLE → 限速
- 语义: 决策器可插拔 — 每类约束独立投票

### 3. 平衡器 — 权重方差最小化

场景: 都允许分配, 放哪最平衡?
- BalancedShardsAllocator (BalancedShardsAllocator.java:65-77): "balance defined by four parameters" — shard/index/threshold
- 设置 (BalancedShardsAllocator.java:84-112): balance.shard (BalancedShardsAllocator.java:91) / balance.index (BalancedShardsAllocator.java:84) / balance.threshold (BalancedShardsAllocator.java:112)
- 阈值 (BalancedShardsAllocator.java:145-156): ensureValidThreshold (BalancedShardsAllocator.java:155, ≥1) — 防频繁抖动
- 算法: 节点权重 = shard 数 + 每索引权重, 方差最小化

### 4. 分配时序 — reroute 流程

场景: 一次 reroute 做什么?
- reroute (AllocationService.java:388-410): shardsAllocator.allocate (AllocationService.java:392)
- 未分配处理 (AllocationService.java:541-553): 主先 (AllocationService.java:541) 副本后 (AllocationService.java:553)
- 流程: 死节点检测 → 分配 (决策器+平衡器) → 新路由表发布 (E-10)
- 触发: 节点增删/索引变化/磁盘水印

### 5. 收束 — 分配的骨架

- 决策器 = 约束投票 (能不能放), 平衡器 = 目标优化 (放哪最好)
- 主先副本后 + 版本递增 = 分配时序
- 引出: 篇 3 (路由: 分配完怎么用)

### 核心悬念
"19 个决策器意见不一怎么办?" — 聚合规则: 任一 NO 拒绝 (硬约束), 全 YES 允许, THROTTLE 限速 — 决策器是"可插拔的约束层", 平衡器在约束内做优化。

### 概念依赖链
Q4 决策器 → Q5 平衡器 → Q6 时序 → (E-10 衔接)

### 源码锚点清单
- AllocationDecider.java:32-91 (接口) / 32 (canRebalance) / 40 (canAllocate) / 48 (canRemain)
- allocation/decider/ (19 决策器文件)
- BalancedShardsAllocator.java:65-77 (注释) / 84 (balance.index) / 91 (balance.shard) / 112 (balance.threshold) / 145-156 (阈值)
- AllocationService.java:388-410 (reroute) / 392 (allocate) / 541-553 (主先副本后)
