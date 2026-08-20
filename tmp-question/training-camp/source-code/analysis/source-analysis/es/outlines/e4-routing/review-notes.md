# E-4 Cluster Routing — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (1 处行号偏差 + 56 裸行号根治)**

## 第一层: 锚点验证 (写后即验, 抓 1 处)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 03 大纲 | Preference.parse (L214) | **L218** (preferenceActiveShardIterator 内) | -4 |

## 第二层: 机制实证 (全过)

- RoutingTable L45,52,59-60 / IndexShardRoutingTable L45-79 / ShardRouting L447,479,600 ✅
- ShardRoutingState 4 态 L15-32 / MasterService 版本递增 L508 ✅
- AllocationDecider 5 类判定 L32-91 / 19 决策器 ✅
- BalancedShardsAllocator 3 参数 L84-112 + 阈值 L145-156 ✅
- AllocationService reroute L388-410 / 主先副后 L541-553 ✅
- OperationRouting getShards L63-77 / preference 解析 L206-240 / 自适应 L39 ✅

## 第三层: 编造检查 (零)

- BalancedShardsAllocator 2013-01-17 (2eb09e6b1ab) / ShardRouting 合并 2015-06-24 (c57951780e0) 日期实证 ✅
- v0.90 Mutable/Immutable 分离 (143/339 行) 实证 ✅

## 第四层: 覆盖缺口 (1 项 — completeness ⚠️ 已回补)

- 01-L2 路由表重建时机 ✅

## 第五层: 裸行号

- 修复前 56 处 → 修复后 **0 残留**
- 行号上限: 8 文件全 OK

## 第六层: 跨域引用核验

- redis/r14-sentinel ✅
- redisson/rd1-connection ✅
- E-5/E-6/E-10 内部衔接 ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 自查修复后复核

> 目的: 验证自查修复精度 + 抓方法体内行号偏移 (前 9 域经验延续)

### 发现 2 处新偏差 (延续收敛趋势)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 L35 | 路由态 (ShardRoutingState L15-32) 裸行号 | 补 ShardRoutingState.java:15-32 | 格式 |
| 2 | 03 大纲锚点清单 | Preference.parse (214) | **L218** (正文已修, 清单残留) | 锚点清单残留 |

### 已验证正确 (30+ 项)

- RoutingTable L45,52,59 / IndexShardRoutingTable L45-79 / ShardRouting L447,479,600 ✅
- ShardRoutingState 枚举 L15-32 (值 L19-32) / MasterService 版本递增 L508 ✅
- AllocationDecider 5 类判定 L32-81 / BalancedShardsAllocator 注释 L68-77 + 参数 L84-112 + 阈值 L145-156 ✅
- AllocationService reroute L388-410 / 主先副后 L541-553 ✅
- OperationRouting getShards L63-77 / preference 解析 L206-240 / Preference.parse L218 / 自适应 L39 ✅
- 时空溯源 2 commit 日期复核 (2013-01-17 / 2015-06-24) ✅

### 根因与根治 (确认收敛)

- **根因**: ① 正文修了但锚点清单漏 (03 的 214→218) ② 01 L35 裸行号漏网 — "修正文必须同步锚点清单" 仍是关键
- **确认**: 三遍验证闭环持续收敛 — E-4 本轮仅 2 处 (E-12 抓 2, E-11 抓 3) — **稳定在低位, 方法论已完全内化**
- harness 13/13 复跑通过 / 裸行号零残留 / 上限 8 文件全 OK / 跨域 2 个 [ -d ] 通过
