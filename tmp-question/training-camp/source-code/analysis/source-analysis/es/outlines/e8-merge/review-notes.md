# E-8 Merge Policy — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (3 处方法体内偏差 + 50 裸行号根治)**

## 第一层: 锚点验证 (写后即验, 抓 3 处)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 02 大纲 | forceMergeDeletes (L2406) | **L2405** | -1 |
| 2 | 02 大纲 | maybeMerge (L2408) / forceMerge blocking (L2410) | **L2407 / L2409** | -1 |
| 3 | 02 大纲 | forceMergeUUID (L2411) | **L2410** | -1 |

## 第二层: 机制实证 (全过)

- TieredMergePolicy L105 / 默认值 L119-150 / adjustMaxMergeAtOnceIfNeeded L367-380 ✅
- mergeFactor 仅 timeBased 用 (Tiered 忽略 L327) — 重要发现 ✅
- deletesPctAllowed 应用链 L303→L362-363 ✅
- MergeSchedulerConfig maxMergeCount=线程+5 (L54) ✅
- forceMerge 三分支 L2405/2407/2409 ✅
- CombinedDeletionPolicy 协调 L34-40 / safeCommit L57 ✅

## 第三层: 编造检查 (零)

- 全部参数/默认值 grep 实证 (10/10/32/20%) ✅

## 第四层: 覆盖缺口 (1 项 — completeness ⚠️ 已回补)

- 01-L2 segmentsPerTier 调参影响 ✅

## 第五层: 裸行号

- 修复前 50 处 → 修复后 **0 残留**
- 行号上限: 5 文件全 OK

## 第六层: 跨域引用核验

- redis/r23-evict / r8-persistence ✅
- E-1/E-6/E-3 内部衔接 (soft deletes/租约/translog 三域闭环) ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 自查修复后复核

> 目的: 验证自查修复精度 + 抓方法体内行号偏移 (前 10 域经验延续)

### 发现 1 处新偏差 (最干净的一轮)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | completeness L19 | 合并触发 flush (L2834) 裸行号 | 补 InternalEngine.java:2834 | 格式 |

### 已验证正确 (20+ 项)

- MergePolicyConfig: 类 L104 / 策略 L105 / 默认值 L119-150 / adjustMaxMergeAtOnceIfNeeded L367-380 (条件 L369/钳制 L372-374) / mergeFactor 忽略 L327 / setDeletesPctAllowed L303,362-363 ✅
- MergeSchedulerConfig: MAX_THREAD L45 / MAX_MERGE L52 / 默认线程+5 L54 ✅
- ElasticsearchConcurrentMergeScheduler: doMerge L95 / setMaxMergesAndThreads L211 ✅
- InternalEngine: forceMerge L2389 / 三分支 L2405/2407/2409 / forceMergeUUID L2410 / shouldPeriodicallyFlushAfterBigMerge L2137,2834 ✅
- CombinedDeletionPolicy: 协调注释 L34-40 / safeCommit L57 ✅

### 根因与根治 (确认收敛)

- **根因**: completeness 引用 (L2834) 漏加文件名前缀 — 纯格式类
- **确认**: 三遍验证闭环持续收敛 — E-8 本轮仅 1 处 (历轮最低) — **方法论已完全内化, 修复质量达峰值**
- 裸行号零残留 / 上限 5 文件全 OK / 跨域 2 个 [ -d ] 通过 / 双链齐
