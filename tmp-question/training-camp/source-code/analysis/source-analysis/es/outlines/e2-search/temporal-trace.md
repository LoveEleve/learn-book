# E-2 Search 查询路径 — 时空溯源 (v0.90 → v8.12.2)

> 方法: git show 早期 tag + git log commit 日期实证
> 断代锚点: v0.90.0 / v5.0.0-alpha1 / v8.12.2

## 演进主线 (3 代)

| 代 | 版本 | 结构 | 关键决策 |
|:--:|---|---|---|
| 1 | v0.90 | QueryPhase 148 行单方法: execute (QueryPhase.java:88, v0.90) 内 **if-else 四分支** — scan/TotalHitCountCollector/sort/topDocs (L113-134, v0.90) | **内联分支**: 按查询类型 (scan/计数/排序/普通) 手写判断; 无 CollectorManager |
| 2 | v5.0 | QueryPhase 独立 (search/query/) + FetchPhase 分离 | 结构稳定期: 两阶段查询定型 |
| 3 | v8.12 | QueryPhase 270 行 + **QueryPhaseCollectorManager 701 行分离** | **CollectorManager 模式**: newCollector/reduce 两阶段 (QueryPhaseCollectorManager.java:111/147) + 聚合挂载 (L125) + 快路径判定 (L233-240) — 从"内联 if-else"到"组合模式" |

## 三个核心设计变迁

### 1. 内联 if-else → CollectorManager 组合

```
v0.90: execute 内四分支 (QueryPhase.java:113-134, v0.90 版本): scan? 计数? sort? 普通 — 手写判断
v8.12: QueryPhaseCollectorManager (QueryPhaseCollectorManager.java:77): newCollector (L111) 组合 topDocs+聚合
       reduce (QueryPhaseCollectorManager.java:147) 跨段合并 — 子 collector 可插拔
```
- 设计原因: 查询类型组合爆炸 (计数/排序/聚合/postFilter/minScore/terminateAfter) — 组合模式替代分支

### 2. 聚合内联 → 挂载注入

```
v0.90: 聚合在 QueryPhase 外单独执行
v8.12: AggregationPhase.preProcess (QueryPhase.java:133) + aggsCollectorManager 注入 (L195)
       — 查询与聚合同一次文档遍历
```
- 设计原因: 一次遍历完成查询+聚合, 避免两次全扫

### 3. 无超时 → queryCancellation

```
v0.90: 无超时机制
v8.12: getTimeoutCheck (QueryPhase.java:251) + searcher.addQueryCancellation (QueryPhase.java:202)
       — Lucene 协作取消 + allowPartialSearchResults 降级
```
- 设计原因: 大集群查询需要可取消性 (OOM/长查询保护)

## 对照 Redis 查询演进

- Redis: 键查找 O(1) 从未需要"查询阶段" — 无打分无排序
- ES: 从简单 topDocs 到 CollectorManager 组合 — 复杂度来自"相关性排序 + 聚合 + 取消"
- 结论: 查询引擎的复杂度 = 排序/打分/聚合需求的直接函数

## REVIEW 修正记录 (2026-08-14)

- ✅ v0.90 QueryPhase 148 行 + 四分支 (git show 实证)
- ✅ QueryPhaseCollectorManager 701 行 (v8 分离实证)

## 完成检查

- [x] v0.90 QueryPhase 四分支已读 (QueryPhase.java:88-134, v0.90)
- [x] v5.0 独立 QueryPhase 实证
- [x] 三核心变迁逐代对照
- [x] Redis 对照
