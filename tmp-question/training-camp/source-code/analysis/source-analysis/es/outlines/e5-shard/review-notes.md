# E-5 Shard — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (1 处行号偏差 + 90 裸行号根治)**

## 第一层: 锚点验证 (全过, 写后即验)

- IndexShardState: CREATED L12 / RELOCATED 兼容 L25-26 ✅
- IndexShard: updateShardState L493 / changeState L885 / close L1672 / postRecovery L1704 / recoverFromStore L2370 / term 断言 L576 / resync L609,748 / updateFromMaster **L526** (初写 523, 已修) ✅
- IndexShardOperationPermits: Semaphore L49-50 / blockOperations L82-116 / acquireAll L138-153 / 超时抛错 L152 / 单 permit L259-260 ✅
- ReplicationGroup: 双集合 L24-25 / 派生 L43-80 ✅
- GlobalCheckpointSyncer: 触发 javadoc L13-24 ✅

## 第二层: 机制实证 (全过)

- 状态迁移双轨 (集群驱动 L529-536 vs 本地驱动 L744/L1723) ✅
- permits 双模式 (MAX_VALUE 信号量 + 全占) ✅
- promotion 四步 (term+resync+补洞) ✅
- 复制组派生 (inSync→targets/skipped) ✅
- close 顺序 (拒新→排空→通知) ✅
- 时空溯源: RELOCATED 移除 2018-03-28 (848c7e4917c) + v0.90 InternalIndexShard 876 行实证 ✅

## 第三层: 编造检查 (零)

- v0.90 5 态 (含 RELOCATED) / v5.0 6 态 (+POST_RECOVERY) / v8.12 5 态 全部 git show 实证 ✅

## 第四层: 覆盖缺口 (3 项 — completeness ⚠️ 已回补)

- 01-L3 acquireAll 超时语义 (L152-153) ✅
- 01-L3 blockOperations timeout 调用方语义 ✅
- 03-L2 POST_RECOVERY 卡住运维视角 ✅

## 第五层: 裸行号

- 修复前 90 处 → 修复后 **0 残留**
- 行号上限检查: 5 文件全 OK (IndexShard 4242 / Permits 284 / ReplicationGroup 146 / State 46 / Syncer 25)

## 第六层: 跨域引用核验

- redis/r14-sentinel / r20-server ✅
- E-3/E-6/E-10 内部衔接 ✅
- 全部 [ -d ] 验证; 对照声明含"同词+一句摘要" ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 自查修复后复核

> 目的: 验证自查修复精度 + 抓方法体内行号偏移 (前 5 域经验延续)

### 发现 8 处新偏差 (全部方法体内/调用点级)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 L21 | POST_RECOVERY→STARTED (L529-536) 裸行号 | 补 IndexShard.java:529-536 | 格式 |
| 2 | 01 大纲 L23 | 主→副本非法 (L517-524) 裸行号 | 补 IndexShard.java:517-524 | 格式 |
| 3 | 01 大纲 L39 | delayOperations 调用 (L83) → waitUntilBlocked (L85) | 实际 **L88/L89** (blockOperations 内) | -5 |
| 4 | 01 大纲 L39 | close 内 flushAndClose (L1682-1683) | **L1683** | -1 |
| 5 | 01 大纲 L39 | IOUtils.close (L1687) → permits.close (L1688) | **L1688/L1689** | +1 |
| 6 | 01 大纲 L40 | delayOperations 检查 closed (L136-140) | 定义 **L128-136** (closed 检查 L130) | -8 |
| 7 | pass2-q5 | IOUtils.close (L1687) / permits.close (L1688) | **L1688/L1689** | +1 |
| 8 | pass2-q5 | delayOperations (L136-140) | **L128-136** | -8 |

### 已验证正确 (20+ 项)

- 5 态枚举 L12-17 / RELOCATED 兼容 L27 ✅
- updateShardState L493 / changeState L885 / close L1672 / postRecovery L1704 / recoverFromStore L2370 ✅
- term 断言 L576 / resync CAS L609 / resync 标志 L748 / updateFromMaster L526 ✅
- ReplicationGroup 双集合 L24-25 / unavailableInSyncShards L43 / targets.add L56 / skipped.add L53,60 / relocationTarget L65 ✅
- GlobalCheckpointSyncer javadoc L13-24 ✅
- blockOperations L82 / waitUntilBlocked L92 / acquireAll L138 / tryAcquire 全占 L145 ✅
- 时空溯源 848c7e4917c 2018-03-28 复核 ✅

### 根因与根治 (确认收敛)

- **根因**: ① 上一轮 sed 只替换"无文件名上下文"的 (Lxxx), 漏了"文件名在前+括号内 (Lxxx)"的混合格式 (01 L21/L23/L39) ② 方法体内调用行仍靠偏移推算 (L83→实 88)
- **确认**: 三遍验证闭环持续生效 — 本域 REVIEW-2 抓 8 处 (E-6 抓 6 处, E-1 抓 10 处) — 逐域收敛趋势明显
- harness 16/16 复跑通过 / 裸行号零残留 / 行号上限 OK / 跨域 2 个 [ -d ] 通过
