# E-2 Search 查询路径 — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: E-7 Mapping ✅ + E-11 FieldData ✅ + E-1 ✅ | 对照: [[r28-networking]] (RESP) [[rd4-command]] (命令面)
> 源码: server/src/main/java/org/elasticsearch/search/ (809 文件; query/ 9 + fetch/ 55 核心)
> 测试地图: server/src/test/.../search/query/ (QueryPhaseTests 20+)

## 继承树/调用图

```
协调节点: SearchRequest → SearchService (1825 行) → 分片查询
分片侧: SearchService.executeQueryPhase (L522) → QueryPhase.execute (QueryPhase.java:61)
    → executeQuery (QueryPhase.java:115): AggregationPhase.preProcess (QueryPhase.java:133) → addCollectorsAndSearch (L135)
    → QueryPhaseCollectorManager.createQueryPhaseCollectorManager (QueryPhase.java:193-199)
    → searcher.search(query, collectorManager) (QueryPhase.java:206) → QueryPhaseResult
    → RescorePhase (QueryPhase.java:137) → SuggestPhase (L138)
    → FetchPhase.execute (FetchPhase.java:59): StoredFieldsSpec (L110) → StoredFieldLoader (L113) → 逐 hit 加载 _source
DFS: DfsPhase.execute (DfsPhase.java:53) — 三阶段查询的先收集词频

打分: SimilarityProviders.createBM25Similarity (SimilarityProviders.java:255-262): k1=1.2 (L258) / b=0.75 (L259)
```

## 基本元素分解 (原则二)

1. **两阶段查询** — QueryPhase (doc_id+score) + FetchPhase (_source/stored_fields) — 协调节点分片广播
2. **CollectorManager 体系** — QueryPhaseCollectorManager (QueryPhaseCollectorManager.java:77): newCollector (L111) 每段建 + reduce (L147) 跨段合并; 聚合挂载 (L125, aggsCollectorManager)
3. **hit count 快路径** — testCountWithoutDeletions (QueryPhaseTests.java:198): 无删除时 TotalHits 精确计数优化
4. **BM25 打分** — SimilarityProviders.java:255-262: k1=1.2 / b=0.75 (LegacyBM25Similarity)
5. **Fetch 子阶段** — FetchPhase (FetchPhase.java:59-378): storedFieldsSpec 构建 (L110) → StoredFieldLoader (L113) → _source 加载
6. **DFS 三阶段** — DfsPhase (DfsPhase.java:53): 收集全局词频 → 再 Query → 再 Fetch (精确打分)

## 标记问题 (≥5)

1. **Q1: QueryPhase 的 Collector 体系怎么组织?** — QueryPhaseCollectorManager: newCollector/reduce 两阶段 + 聚合/排序/postFilter 子 collector 组合
2. **Q2: hit count 快路径 (size=0/无删除)** — testCountWithoutDeletions (QueryPhaseTests.java:198): 怎么跳过 topDocs 直接计数?
3. **Q3: BM25 打分细节** — k1=1.2/b=0.75 是什么? 词频饱和 + 长度归一化? LegacyBM25Similarity 与标准 BM25 差异?
4. **Q4: FetchPhase 加载什么?** — _source/stored_fields/highlight? StoredFieldsSpec 怎么构建?
5. **Q5: 两阶段查询的分布式面** — 协调节点怎么 merge 各分片 topDocs? SearchPhaseController (SearchPhaseController.java:319 getLastEmittedDocPerShard)
6. **Q6: DFS 三阶段为什么需要?** — 全局词频 vs 局部词频的打分差异
7. **Q7: 超时/取消机制** — getTimeoutCheck (QueryPhase.java:202-203) + queryCancellation
8. **Q8: 与 Redis 查询对照** — Redis 全扫/索引 vs ES 倒排+打分 — 查询模型根本差异

## 已读测试 (2 个)

- `QueryPhaseTests.testCountWithoutDeletions` (QueryPhaseTests.java:198): 无删除时精确计数
- `QueryPhaseTests.testPostFilterDisablesHitCountShortcut` (QueryPhaseTests.java:229): postFilter 禁用快路径
- `QueryPhaseTests.testMinScoreDisablesHitCountShortcut` (QueryPhaseTests.java:276): minScore 禁用快路径

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: E-2 Pass 1 — QueryPhase.executeQuery (L115) 内 AggregationPhase.preProcess (QueryPhase.java:133) — 聚合是查询阶段的一部分 (E-12 衔接)
- 发现: QueryPhaseCollectorManager 挂载 aggsCollectorManager 挂载 (QueryPhase.java:193-199) — 聚合 collector 与查询 collector 并行
- 已对照验证: QueryPhase.java:133,195
