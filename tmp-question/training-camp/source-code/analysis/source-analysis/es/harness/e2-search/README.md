# E-2 Search — harness 验证记录 (MiniSearch 12/12)

> 跑法: `javac MiniSearch.java MiniSearchTest.java && java MiniSearchTest` (JDK 21)
> 结果: **12/12 PASS** (首跑全绿)

## 验证矩阵

| # | 机制 | 验证点 | 源码对照 | 结果 |
|:--:|---|---|---|:--:|
| A1-A2 | BM25 参数 | k1=1.2 / b=0.75 | SimilarityProviders.java:255-262 | PASS |
| A3-A4 | 词频饱和 | tf=2 > tf=1 但 < 2倍分 | LegacyBM25Similarity.java:51-53 (k1 语义) | PASS |
| A5 | 无匹配 | 无关词分=0 | BM25 IDF 逻辑 | PASS |
| B1-B2 | 分片查询 | 返回 topN + 高 tf 优先 | QueryPhaseCollectorManager.java:111-146 (topDocs collector) | PASS |
| C1-C3 | 两阶段 | 跨分片归并 + Fetch _source | SearchPhaseController.java:185-229 (sortDocs) | PASS |
| C4-C5 | 深分页 | from+size 放大问题 | SearchPhaseController.java:319 (getLastEmittedDocPerShard) | PASS |

## 验证意义

- BM25 打分公式 (词频饱和+长度归一化) / Collector 分片收集+归并 / 两阶段查询 / 深分页问题 — 4 大机制全部可复现
- **未验证面**: 真实 Lucene CollectorManager 并行段执行、聚合 collector 共享遍历、DFS 全局词频、超时取消 (queryCancellation)
- 结论: "打分 → 收集 → 归并 → 获取" 的查询路径理解验证到位
