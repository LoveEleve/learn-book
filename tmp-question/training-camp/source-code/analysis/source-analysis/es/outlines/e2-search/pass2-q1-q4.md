# E-2 闭环笔记 Q1-Q4: Collector 体系/hit count/BM25/Fetch

## Q1: QueryPhaseCollectorManager — 每段建 + 跨段归并

假设: CollectorManager 是 Lucene 的"每段一个 collector + 最后 reduce"模式; ES 在其中挂载聚合/排序/postFilter 子 collector。

验证过程:
- Read QueryPhaseCollectorManager (QueryPhaseCollectorManager.java:77): 抽象基类 implements CollectorManager<Collector, QueryPhaseResult>
- newCollector (QueryPhaseCollectorManager.java:111-146): newTopDocsCollector (QueryPhaseCollectorManager.java:113) + aggsCollectorManager.newCollector (QueryPhaseCollectorManager.java:125,139) → QueryPhaseCollector 组合 (QueryPhaseCollectorManager.java:125-131) — **topDocs + 聚合并行收集**
- reduce (QueryPhaseCollectorManager.java:147-180): 逐 collector 提取 topDocs (QueryPhaseCollectorManager.java:157-159) / aggs (QueryPhaseCollectorManager.java:161-163) → reduceCollectorResults 合并
- 挂载: 聚合由 aggsCollectorManager 参数注入 (QueryPhase.java:193-199) — 查询与聚合同一次遍历

代码类型: Implementation (CollectorManager 模式)

结论: **QueryPhase = CollectorManager 模式: 每个 segment 一个 QueryPhaseCollector (内部组合 topDocs + 聚合子 collector), reduce 阶段跨段合并 — 查询与聚合共享一次文档遍历**。QueryPhaseCollectorManager.java:77,111-146,147-180

跨域关联: E-12 Aggregations (aggsCollectorManager 注入) / Lucene CollectorManager

## Q2: hit count 快路径 — size=0/无删除

假设: 不需要 topDocs 时用计数 collector, 跳过排序 — 但 postFilter/minScore 禁用 (需完整打分)。

验证过程:
- trackTotalHitsUpTo 判定 (QueryPhaseCollectorManager.java:233-240): scroll 时 DISABLED (QueryPhaseCollectorManager.java:238-240); 否则 ACCURATE/阈值
- newCollector 分支 (QueryPhaseCollectorManager.java:399 附近): TRACK_TOTAL_HITS_DISABLED → 计数模式
- 测试: testCountWithoutDeletions (QueryPhaseTests.java:198) 精确计数; testPostFilterDisablesHitCountShortcut (QueryPhaseTests.java:229) / testMinScoreDisables (QueryPhaseTests.java:276) — **postFilter/minScore 禁用快路径** (需真正打分)
- 语义: 无 postFilter/minScore + 不需 topDocs → TotalHitCountCollector 只计数不排序

代码类型: Algorithmic (快路径判定)

结论: **hit count 快路径条件: 不需要 topDocs (size=0/scroll) 且无 postFilter/minScore — 此时用计数 collector 跳过排序开销; postFilter/minScore 必须完整打分所以禁用**。QueryPhaseCollectorManager.java:233-240,399 + QueryPhaseTests.java:198,229,276

## Q3: BM25 — Legacy 包装与 boost 差异

假设: ES 用 LegacyBM25Similarity 包装 Lucene BM25Similarity, 区别在 boost 处理 (k1+1 因子)。

验证过程:
- Read SimilarityProviders.createBM25Similarity (SimilarityProviders.java:255-262): k1=1.2 (SimilarityProviders.java:258) / b=0.75 (SimilarityProviders.java:259) / discountOverlaps=true (SimilarityProviders.java:260)
- Read LegacyBM25Similarity (LegacyBM25Similarity.java:35-69): 内部委托 bm25Similarity (LegacyBM25Similarity.java:57) — **关键差异**: `boost * (1 + k1)` (LegacyBM25Similarity.java:67) — 旧版打分把 boost 乘进 (1+k1), 兼容 1.x 打分
- 参数语义 (javadoc L51-53): k1 词频饱和 (saturation), b 长度归一化程度
- @Deprecated (LegacyBM25Similarity.java:33): 应直接用 BM25Similarity, Legacy 为兼容保留

代码类型: Implementation (打分兼容)

结论: **BM25 默认 k1=1.2 (词频饱和) + b=0.75 (长度归一化); ES 用 LegacyBM25Similarity 包装 Lucene 实现, 唯一差异 = boost 乘 (1+k1) 兼容旧打分 (LegacyBM25Similarity.java:67); deprecated 标记表明新索引可直接用 Lucene BM25Similarity**。SimilarityProviders.java:255-262 + LegacyBM25Similarity.java:35-69

## Q4: FetchPhase — 按需加载

假设: Fetch 按 StoredFieldsSpec 精确加载所需字段 (source/stored_fields), 不整文档加载。

验证过程:
- Read FetchPhase.execute (FetchPhase.java:59-378): StoredFieldsSpec.build(processors) (FetchPhase.java:110) — **各子阶段声明所需字段** → merge sourceLoader.requiredStoredFields (FetchPhase.java:111) → StoredFieldLoader.fromSpec (FetchPhase.java:113)
- requiresSource (FetchPhase.java:115): 是否需 _source
- 逐 hit: hitContext (FetchPhase.java:268) 携带 leafStoredFieldLoader.storedFields + source
- 设计: 子阶段 (highlight/vectors/...) 各自声明需求, 合并后一次性加载 — 最小 IO

代码类型: Implementation (按需加载)

结论: **FetchPhase = 按需加载: StoredFieldsSpec 由所有子阶段声明合并 (L110-111), StoredFieldLoader 按 spec 一次性读 (FetchPhase.java:113) — 只加载查询需要的字段, 避免整文档 IO**。FetchPhase.java:59,110-115,268

跨域关联: E-2 篇 2 (两阶段查询分布式面) / E-7 (source 存储面)
