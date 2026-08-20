# E-2 Search — 全视角提问验证 (completeness)

> 验证时机: 3 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | Collector | newCollector 每段调用几次? | ✅ 01-L2 (每段一次) |
| 2 | 开发者 | Collector | reduce 什么时候调用? | ✅ 01-L2 (跨段合并) |
| 3 | 开发者 | 快路径 | terminateAfter 与快路径关系? | ⚠️ 01 未提 → 补一句 (terminateAfter 也走 collector) |
| 4 | 开发者 | BM25 | k1/b 能按字段配吗? | ⚠️ 02 未提 → 补一句 (per-field similarity) |
| 5 | 架构师 | Collector | 为什么用组合模式替代 if-else? | ✅ 01-L2 (时空溯源) |
| 6 | 架构师 | 分布式 | 协调节点重新打分吗? | ✅ 03-L2 (不重新打分) |
| 7 | 架构师 | DFS | DFS 默认开吗? | ✅ 03-L3 (默认两阶段) |
| 8 | 性能工程师 | 快路径 | 计数模式省了什么? | ✅ 01-L3 (跳过排序) |
| 9 | 性能工程师 | Fetch | StoredFieldsSpec 怎么省 IO? | ✅ 02-L3 (按需加载) |
| 10 | SRE | 超时 | 超时后部分结果? | ✅ 01-L4 (allowPartialSearchResults) |
| 11 | SRE | 深分页 | from 大时的问题? | ✅ 03-L5 (search_after) |
| 12 | 研究者 | 对照 | Redis vs ES 查询模型? | ✅ 03-L4 |
| 13 | 研究者 | 时空 | v0.90 四分支 → CollectorManager? | ✅ 01-L2 + temporal |
| 14 | 子系统开发者 | 衔接 | 聚合怎么挂载? (E-12) | ✅ 01-L2 (aggsCollectorManager) |
| 15 | 子系统开发者 | 衔接 | DFS 词频给谁? (打分) | ✅ 03-L3 (IDF) |
| 16 | 学生 | 概念 | 两阶段查询是什么? | ✅ 03-L1 |
| 17 | 学生 | BM25 | k1=1.2 为什么? | ✅ 02-L2 (词频饱和) |
| 18 | 学生 | Collector | "collector" 通俗解释? | ✅ 01-L1 (收集结果) |

**统计**: ✅ 16 / ⚠️ 2 / ❌ 0 — ⚠️ 全部"补一句"级
→ 回补 2 项: 01-L3 terminateAfter 与快路径 / 02-L2 per-field similarity

## 回补清单

1. 01-L3: 补 terminateAfter 走 collector 一句 (testTerminateAfterWithFilter L263)
2. 02-L2: 补 similarity 可按字段配置一句 (SimilarityService per-field)
