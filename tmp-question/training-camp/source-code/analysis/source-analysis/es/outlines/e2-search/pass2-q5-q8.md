# E-2 闭环笔记 Q5-Q8: 分布式 merge/DFS/超时/Redis 对照

## Q5: 协调节点 merge — 跨分片 topDocs 合并

假设: 各分片返回 topDocs, 协调节点按 score/sort 归并 + 截断 from+size。

验证过程:
- Read SearchPhaseController.sortDocs (SearchPhaseController.java:185-229): mergeTopDocs (SearchPhaseController.java:195) 合并各分片 → 截断 → 排序
- getLastEmittedDocPerShard (SearchPhaseController.java:319): 计算每分片"还需 fetch 哪些 doc" (fetchHits 分配)
- 流程: 分片 QueryPhase → 协调节点 sortDocs → 决定 FetchPhase 拉取集合 → 分片 Fetch → 最终 merge
- 关键: 协调节点不重新打分, 只按分片返回的 score/sort 值归并 — 打分在分片内完成

代码类型: Implementation (分布式归并)

结论: **两阶段查询: 协调节点广播 Query → 分片各自 QueryPhase (打分+topDocs) → sortDocs 归并 (L195) → 计算每分片 fetch 清单 (L319) → FetchPhase 拉 _source — 打分分布化, 协调只做归并**。SearchPhaseController.java:185-229,319

## Q6: DFS 三阶段 — 全局词频精确打分

假设: 默认两阶段 (Query+Fetch) 用分片局部词频; DFS 先收集全局词频再 Query — 打分更精确 (BM25 的 IDF 依赖全局 docFreq)。

验证过程:
- Read DfsPhase (DfsPhase.java:53-62): 分片收集 term 统计 → AggregatedDfs (AggregatedDfs.java:22) 汇总到协调
- 词频统计 (DfsPhase.java:152-154): stats.keySet → TermStatistics 数组
- 使用: 协调节点把全局词频发回分片 → QueryPhase 用全局 df 打分
- 代价: 多一轮 RTT — 仅精确打分场景用 (默认关闭)

代码类型: Implementation (三阶段优化)

结论: **DFS = 可选三阶段: ① 收集各分片词频 → 协调汇总 (AggregatedDfs) → ② 全局词频下放 → ③ Query+Fetch — BM25 的 IDF 用全局 docFreq 更精确, 代价多一轮 RTT; 默认两阶段用局部词频**。DfsPhase.java:53-62,152-154

## Q7: 超时/取消 — queryCancellation

假设: 超时通过 searcher 取消机制 (Lucene queryCancellation), 超时抛 TimeExceededException。

验证过程:
- Read getTimeoutCheck (QueryPhase.java:251-275): scroll 无超时 (QueryPhase.java:252-254); timeoutSet 时计算 maxTime (QueryPhase.java:257-260) → 返回 Runnable 检查超时 (QueryPhase.java:262-265)
- 注册: addQueryCancellation (QueryPhase.java:202) — Lucene 每段查询前检查
- 超时处理: throwTimeExceededException (L263) → allowPartialSearchResults 决定部分返回还是整体失败 (QueryPhase.java:211-220)
- 测试: QueryPhaseTimeoutTests

代码类型: Implementation (协作取消)

结论: **超时 = 查询前检查 Runnable (getTimeoutCheck L251) 注册到 Lucene queryCancellation (L202), 超时抛 TimeExceededException; allowPartialSearchResults 决定降级 (部分结果) 还是失败**。QueryPhase.java:200-220,251-275

## Q8: 与 Redis 查询对照 — 两种查询模型

假设: Redis 键查找 (O(1) 哈希) vs ES 倒排+打分 (相关性排序) — 查询能力根本差异。

验证过程:
- Redis: 键空间哈希查找 (db.c, O(1)); 无相关性概念, 返回精确值
- ES: 倒排索引检索候选 → BM25 打分 → topDocs 排序 — 返回"最相关"
- 对照维度:
  - 数据模型: Redis 键值精确 / ES 文档字段
  - 查询: Redis 精确匹配 / ES 全文+打分+聚合
  - 返回: Redis 单值 / ES 排序列表
- 面试记忆点: "Redis 回答'有没有', ES 回答'哪个最相关'"

代码类型: 对照分析

结论: **Redis = 精确键查找 (哈希 O(1), 无打分); ES = 倒排检索 + BM25 打分 + 排序 — 两种查询模型对应两种数据用途 (缓存 vs 搜索引擎)**。对照锚点: db.c (Redis 键空间) + QueryPhase.java:206 (searcher.search)

跨域关联: [[r21-db]] (Redis 键空间) / [[r28-networking]] (RESP 协议面)
