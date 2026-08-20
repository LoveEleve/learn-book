# E-6 SeqNo — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (2 处跨文件标注错误 + 裸行号 46 处根治)**

## 第一层: 锚点验证 (全过, 写后即验)

- LocalCheckpointTracker: generateSeqNo L83 / advanceMaxSeqNo L90 / markProcessed L99 / markPersisted L108 / markSeqNo L112 / updateCheckpoint L191 / BIT_SET_SIZE L25 — 全部 grep 实证 ✅
- ReplicationTracker: globalCheckpoint L147 / computeGlobalCheckpoint L1349 / 双回退 L1356-1362 / 断言 L830-832 ✅
- ReplicationOperation: execute L107 / handlePrimaryResult L129 / performOnReplicas L210 / performOnReplica L230 / failShardIfNeeded L276 ✅
- RetentionLease: 定义注释 L24-27 / 结构 L31-65 / add L302 / renew L390 / persist L488 ✅
- IndexShard: pendingPrimaryTerm L231 / 构造 L360-361 ✅

## 第二层: 跨文件标注错误 (2 处 — 行号上限检查抓出)

| # | 文件 | 错误 | 修正 |
|:--:|---|---|---|
| 1 | 01 大纲 | persisted (LocalCheckpointTracker.java:1247) — 1247 超文件上限 249 | **InternalEngine.java:1247** (translog 回调 L255 同) |
| 2 | 02 大纲 | inSync/min (ReplicationOperation.java:1359,1365) — 超上限 698 | **ReplicationTracker.java:1359,1365** |

## 第三层: 机制实证 (全过)

- 双 checkpoint 消费链 (InternalEngine.java:1243-1248,255) — processed/persisted 分离 ✅
- updateCheckpoint 连续跳跃 + 段清理 (L191-218) ✅
- computeGlobalCheckpoint 双回退语义 (pendingInSync/UNASSIGNED) ✅
- 复制失败分流 (重试 vs stale, L247-280) ✅
- 时空溯源: 3 commit 日期实证 (2015-10-21/11-19/12-15) + v6.0→v7.0 合并 ✅

## 第四层: 覆盖缺口 (4 项 — completeness ⚠️ 已回补)

- 01-L3 markSeqNo 幂等 (L116-119) ✅
- 02-L4 stale 后 peer recovery (E-5 衔接) ✅
- 03-L2 租约过期后果 ✅
- 03-L2 Redis 无租约等价物 ✅

## 第五层: 裸行号

- 修复前 46 处 → 修复后 **0 残留** (正则 + 手工 + 复扫)
- 行号上限检查: 抓出 2 处跨文件错误 (LocalCheckpointTracker.java:1247 实为 InternalEngine)

## 第六层: 跨域引用核验

- redis/r9-replication / r14-sentinel ✅ ([[r14-sentinel]] 已存在)
- redisson/rd2-rlock ✅
- E-1/E-3/E-5/E-8 内部衔接 ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 自查修复后复核

> 目的: 验证自查修复精度 + 抓方法体内分支行号偏移 (E-1 REVIEW-2 教训)

### 发现 5 处新偏差 (全部方法体内/调用点级)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 | updateCheckpoint (L190-218) | 方法起始 **L191** | -1 |
| 2 | 01 大纲 | BIT_SET_SIZE (L25 裸行号) | 补 LocalCheckpointTracker.java:25 | 格式 |
| 3 | pass2-q1 | "keep it simple" 注释 (L193) | **L195** | -2 |
| 4 | pass2-q1 + 02 大纲 | failShardIfNeeded (L273-275) | **L276** (273-275 是记录 failure 区) | +1~3 |
| 5 | pass2-q1 + 02 大纲 | performOnReplica (L233-280) | 方法起始 **L230** | -3 |
| 6 | 02 大纲 + pass2-q1 | updateGlobalCheckpointOnPrimary (L1098) | 定义 **L1375** (L1098 是调用点) | 调用/定义 |

### 已验证正确 (20+ 项)

- generateSeqNo L83 / advanceMaxSeqNo L90 / markProcessed L99 / markPersisted L108 / markSeqNo L112 ✅
- computeGlobalCheckpoint L1349 / 双回退 L1356-1362 / min L1365 / 断言 L830-832 ✅
- RetentionLease L29 / add L302 / renew L390 / persist L488 / IndexShard pendingPrimaryTerm L231 ✅
- onFailure L251 / execute L107 / handlePrimaryResult L129 / performOnReplicas L210 ✅
- 时空溯源 3 commit 日期复核 (2015-10-21/11-19/12-15) + v6.0→v7.0 ReplicationTracker 出现实证 ✅

### 根因与根治 (延续 E-1 终版方案)

- **根因**: 方法体内注释/调用行仍靠 sed 偏移推算 (L193/L273-275 偏差 1-3 行)
- **确认根治有效**: 本域写完即 grep 已减少偏差 (第一轮仅 2 处跨文件错误), REVIEW-2 抓的 5 处全是"方法体内细节行" — 三遍验证闭环 (写→自查→复审) 持续生效
- harness 16/16 重跑通过 / 裸行号零残留 / 行号上限 OK (仅 review-notes 记录) / 跨域 3 个 [ -d ] 通过
