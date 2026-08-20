# E-2 Search 篇 3/3 — 分布式面: 两阶段/DFS/Redis 对照

> 前置: [[E-2-search-01]] [[E-2-search-02]] | 复用: — | 对照: [[r21-db]] (Redis 键空间) [[r28-networking]] | 引出: [[E-10-clusterstate]] [[E-12-aggregations]]
> 🔴 A | 来源: SearchPhaseController.java:185-229,319 + DfsPhase.java:53-62,152-154 + SearchService.java:522
> 定位: Search 卷收尾 — 回答"协调节点怎么合并? DFS 什么时候用? 与 Redis 差在哪?"

**读者处境**: 一次搜索跨 5 个分片 — 面试官问 "ES 分布式搜索怎么工作? 协调节点干嘛?" 你答 "广播+合并" — 但再问 "DFS 三阶段是什么? 什么时候用?" 你答不上来。这篇是分布式查询面的完整答案, 收束 Search 域。

### 1. 问题引入 — 从单分片到多分片

场景: 查询发到协调节点, 5 个分片各返回 top10 — 怎么合成最终结果?
- 两阶段: 广播 Query → 各分片打分 → 协调归并 → Fetch
- 本篇问题: 归并算法 (Q5) / DFS 优化 (Q6) / Redis 对照 (Q8)

### 2. 协调归并 — sortDocs

场景: 各分片的 topDocs 怎么合并?
- sortDocs (SearchPhaseController.java:185-229): mergeTopDocs (SearchPhaseController.java:195) 合并 → 截断
- getLastEmittedDocPerShard (SearchPhaseController.java:319): 计算每分片"还需 fetch 哪些 doc"
- 关键: 协调节点**不重新打分**, 按分片返回的 score/sort 归并 — 打分分布化
- 流程: 分片 Query → 协调 sortDocs → 分配 fetch 清单 → 分片 Fetch → 最终 merge

### 3. DFS — 三阶段精确打分

场景: 什么时候需要全局词频?
- DfsPhase (SearchPhaseController.java:53-62): 分片收集 term 统计 → AggregatedDfs 汇总到协调 (SearchPhaseController.java:22)
- 词频统计 (SearchPhaseController.java:152-154): stats.keySet → TermStatistics
- 使用: 全局词频下放 → QueryPhase 用全局 df 打分 (BM25 的 IDF 更精确)
- 代价: 多一轮 RTT — 默认两阶段用局部词频, DFS 仅精确打分场景

### 4. 与 Redis 查询对照 — 两种查询模型

场景: Redis 和 ES 的"查询"差在哪?
- Redis: 键空间哈希查找 (O(1), db.c) — 无相关性, 返回精确值
- ES: 倒排检索 → BM25 打分 → topDocs 排序 — 返回"最相关"
- 对照维度: 数据模型 (键值 vs 文档) / 查询 (精确 vs 全文) / 返回 (单值 vs 排序列表)
- 面试记忆点: "Redis 回答'有没有', ES 回答'哪个最相关'"

### 5. 收束 — Search 域总结

- 分片查询 (篇 1): CollectorManager 体系 + 快路径 + 超时
- 打分获取 (篇 2): BM25 参数 + Fetch 按需加载
- 分布式 (本篇): sortDocs 归并 + DFS 三阶段 + Redis 对照
- 引出: E-12 Aggregations (聚合与查询同遍历) — E-10 ClusterState

### 核心悬念
"协调节点合并 5 个分片的 top10, 会漏掉真正的前 10 吗?" — 不会: 每个分片返回 top(from+size), 协调归并后精确; 但深度分页 (from 大) 是"每个分片都要取 from+size"的放大问题 — 这就是 search_after 存在的原因。

### 概念依赖链
Q5 sortDocs → Q6 DFS → Q8 Redis 对照 → (E-12 衔接)

### 源码锚点清单
- SearchPhaseController.java:185-229 (sortDocs) / 195 (mergeTopDocs) / 319 (getLastEmittedDocPerShard)
- DfsPhase.java:53-62 (execute) / 152-154 (词频统计)
- AggregatedDfs.java:22 (汇总结果)
- SearchService.java:522 (executeQueryPhase 入口)
