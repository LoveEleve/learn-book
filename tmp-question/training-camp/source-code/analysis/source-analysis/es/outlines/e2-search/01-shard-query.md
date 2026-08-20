# E-2 Search 篇 1/3 — 分片查询: Collector 体系与快路径

> 前置: [[E-11-fielddata-01]] (docValues 面) [[E-7-mapping-02]] (字段类型) | 复用: — | 对照: [[rd4-command]] (Redisson 命令面) | 引出: [[E-2-search-02]] [[E-2-search-03]] [[E-12-aggregations]]
> 🔴 A | 来源: QueryPhase.java:61,115,133,135,193-206,251-275 + QueryPhaseCollectorManager.java:77,111-146,147-180,233-240
> 定位: Search 卷开篇 — 回答"一次分片查询, Lucene 怎么收集结果?"

**读者处境**: 你发一个 search 请求 — 面试官问 "ES 查询分几个阶段? Collector 是什么?" 你答 "Query+Fetch" — 但再问 "CollectorManager 的 newCollector/reduce 干嘛的? 聚合怎么和查询一起跑?" 你卡住了。这篇是分片查询的完整答案。

### 1. 问题引入 — 分片内的查询旅程

场景: 一个分片收到查询, 要返回"最相关的 N 条 + 总数" — Lucene 怎么做到?
- QueryPhase.execute (QueryPhase.java:61) → executeQuery (QueryPhase.java:115) → addCollectorsAndSearch (QueryPhase.java:135)
- CollectorManager 模式: 每段一个 collector, 最后归并
- 本篇问题: Collector 体系 (Q1) / hit count 快路径 (Q2) / 超时 (Q7)

### 2. CollectorManager — newCollector + reduce

场景: Lucene 的并行段查询怎么收集?
- QueryPhaseCollectorManager (QueryPhase.java:77): 抽象基类 implements CollectorManager
- newCollector (QueryPhaseCollectorManager.java:111-146): 每段构建 — newTopDocsCollector (QueryPhaseCollectorManager.java:113) + aggsCollectorManager.newCollector (QueryPhaseCollectorManager.java:125,139) → QueryPhaseCollector 组合 (QueryPhaseCollectorManager.java:125-131)
- reduce (QueryPhaseCollectorManager.java:147-180): 跨段提取 topDocs (QueryPhaseCollectorManager.java:157-159) / aggs (QueryPhaseCollectorManager.java:161-163) → 合并
- **聚合挂载** (QueryPhase.java:193-199): createQueryPhaseCollectorManager 参数注入 aggsCollectorManager — 查询与聚合同一次遍历
- 时空溯源: v0.90 内联四分支 (148 行) → v8.12 CollectorManager 组合 (701 行)

### 3. hit count 快路径 — 跳过排序

场景: 只需要总数不要文档时, 怎么快?
- trackTotalHitsUpTo 判定 (QueryPhaseCollectorManager.java:233-240): scroll → DISABLED (QueryPhaseCollectorManager.java:238-240)
- 计数模式 (QueryPhaseCollectorManager.java:399 附近): TRACK_TOTAL_HITS_DISABLED → 计数 collector 不排序
- **禁用条件** (测试实证): postFilter (QueryPhaseTests.java:229) / minScore (QueryPhaseTests.java:276) 必须完整打分 → 禁用快路径
- **terminateAfter**: 命中 N 条即停 (testTerminateAfterWithFilter, QueryPhaseTests.java:263) — 走 collector 的 terminateAfterChecker, 与快路径正交
- 设计: 无 postFilter/minScore + 不需 topDocs → TotalHitCountCollector 只计数

### 4. 超时与取消 — 协作取消

场景: 大查询跑太久怎么办?
- getTimeoutCheck (QueryPhase.java:251-269): scroll 无超时 (QueryPhase.java:252-254) → maxTime 计算 (QueryPhase.java:257-260) → Runnable 检查 (QueryPhase.java:262-265)
- 注册: searcher.addQueryCancellation (QueryPhase.java:202) — Lucene 每段前检查
- 降级: allowPartialSearchResults (QueryPhase.java:211-220) — 部分返回 or 失败

### 5. 收束 — 分片查询的骨架

- CollectorManager = 并行段收集 + 归并; 聚合共享遍历; 快路径跳过排序; 超时可取消
- 引出: 篇 2 (打分 BM25 + Fetch) — 篇 3 (分布式面)

### 核心悬念
"查询和聚合为什么能一次遍历完成?" — CollectorManager 的 newCollector 把 topDocs collector 和聚合 collector 组合成一个 QueryPhaseCollector, 一次文档遍历同时喂给两者。

### 概念依赖链
Q1 CollectorManager → Q2 快路径 → Q7 超时 → (时空溯源)

### 源码锚点清单
- QueryPhase.java:61 (execute) / 115 (executeQuery) / 133 (AggregationPhase.preProcess) / 135 (addCollectorsAndSearch) / 193-206 (collectorManager 构建+search) / 200-220 (超时注册+降级) / 251-275 (getTimeoutCheck)
- QueryPhaseCollectorManager.java:77 (基类) / 111-146 (newCollector) / 147-180 (reduce) / 195 (聚合挂载) / 233-240 (trackTotalHitsUpTo) / 399 (计数模式)
- QueryPhaseTests.java:198 (testCountWithoutDeletions) / 229 (testPostFilterDisables) / 276 (testMinScoreDisables)
