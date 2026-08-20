# E-2 Search — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (1 处跨文件错误 + 74 裸行号根治)**

## 第一层: 锚点验证 (写后即验, 抓 1 处跨文件错误)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 | minScore (QueryPhase.java:276) | **QueryPhaseTests.java:276** — QueryPhase.java 仅 270 行, 276 超限 | 跨文件错误 (上限检查抓出) |

## 第二层: 机制实证 (全过)

- QueryPhase 全链: execute L61 / executeQuery L115 / preProcess L133 / addCollectorsAndSearch L135,149 / search L206 ✅
- CollectorManager: 基类 L77 / newCollector L111 / reduce L147 / 聚合挂载 L195 / trackTotalHitsUpTo L233-240 ✅
- BM25: createBM25Similarity L255-262 (k1 L258 / b L259) + Legacy boost*(1+k1) L67 ✅
- Fetch: StoredFieldsSpec L110 / StoredFieldLoader L113 / requiresSource L115 / hitContext L268 ✅
- 分布式: sortDocs L185 / mergeTopDocs L195 / getLastEmittedDocPerShard L319 / DfsPhase L53,152 ✅
- 超时: getTimeoutCheck L251 / addQueryCancellation L202 / allowPartialSearchResults L212 ✅

## 第三层: 编造检查 (零)

- v0.90 QueryPhase 148 行四分支 (L88-134) git show 实证 ✅
- CollectorManager 701 行分离实证 ✅

## 第四层: 覆盖缺口 (2 项 — completeness ⚠️ 已回补)

- 01-L3 terminateAfter (testTerminateAfterWithFilter L263) ✅
- 02-L2 per-field similarity (PerFieldSimilarityWrapper L17,130) ✅

## 第五层: 裸行号

- 修复前 74 处 → 修复后 **0 残留**
- 行号上限: 8 文件全 OK (QueryPhase 270 / CollectorManager 701 / FetchPhase 378 / SearchPhaseController / LegacyBM25 等)

## 第六层: 跨域引用核验

- redis/r21-db / r24-string / r28-networking ✅
- redisson/rd4-command ✅
- E-7/E-11/E-12 内部衔接 ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 自查修复后复核

> 目的: 验证自查修复精度 + 抓方法体内行号偏移 (前 7 域经验延续)

### 发现 7 处新偏差 (重大: 1 处批量跨文件错误)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 L20 | newCollector (QueryPhase.java:111-146) | **QueryPhaseCollectorManager.java:111-146** | **跨文件错误 (批量)** |
| 2 | 01 大纲 L20 | newTopDocsCollector (QueryPhase.java:113) / aggsCollectorManager (L125,139) / 组合 (QueryPhase.java:125-131) | **QueryPhaseCollectorManager.java:113/125,139/125-131** | 同上 |
| 3 | 01 大纲 L21 | reduce (QueryPhase.java:147-180) / 提取 (L157-159,161-163) | **QueryPhaseCollectorManager.java:147-180/157-159/161-163** | 同上 |
| 4 | 01 大纲 L22 | 聚合挂载 (QueryPhase.java:195) | **QueryPhase.java:193-199** (createQueryPhaseCollectorManager 调用) | 调用点偏差 |
| 5 | 01 大纲 L28 | trackTotalHitsUpTo 判定 (QueryPhase.java:233-240) | **QueryPhaseCollectorManager.java:233-240** | 跨文件错误 |
| 6 | pass1/pass2 | 聚合挂载 (QueryPhase.java:195) | QueryPhase.java:193-199 | 同 #4 |
| 7 | 01/pass2-q5 | getTimeoutCheck (QueryPhase.java:251-275) | 方法体 **L251-269** (文件 270 行) | 超限 |

### 根因 (本轮最重要教训)

- **#1-5 根因**: 上一轮 sed 批量替换时把 01-shard-query.md 的默认文件设成 QueryPhase.java, 导致**所有 CollectorManager 行号被错误标注到 QueryPhase.java** — 这是"批量替换的默认文件选错"造成的系统性错误
- 行号上限检查 (QueryPhase.java 270 行) 抓出了这个批量错误 — **验证了"上限检查"作为最后防线的价值**
- **#7**: getTimeoutCheck 是文件最后一个方法 (L251-269), 写 275 是未核对文件尾

### 已验证正确 (20+ 项)

- QueryPhase: execute L61 / executeQuery L115 / preProcess L133 / addCollectorsAndSearch L149 / search L206 / addQueryCancellation L202 / allowPartialSearchResults L212 / getTimeoutCheck L251 ✅
- CollectorManager: 基类 L77 / newCollector L111 / reduce L147 / aggsCollectorManager 字段 L80 / 挂载调用 QueryPhase.java:193-199 ✅
- BM25: createBM25Similarity L255-262 / Legacy boost L67 ✅
- Fetch: execute L59 / StoredFieldsSpec L110 / StoredFieldLoader L113 / requiresSource L115 ✅
- 分布式: sortDocs L185 / mergeTopDocs L195 / getLastEmittedDocPerShard L319 / DfsPhase L53,152 ✅

### 根治方案 (再升级)

**批量替换的默认文件必须逐文件确认**: 一个文件里混多个类的行号时 (如 01-shard-query.md 同时含 QueryPhase + QueryPhaseCollectorManager), 不能用单一默认文件替换 — 必须按方法名归属精确标注。行号上限检查 (wc -l) 作为终检防线, 成功抓出本次批量错误。
