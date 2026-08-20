# E-6 SeqNo — 全视角提问验证 (completeness)

> 验证时机: 3 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 位点 | generateSeqNo 线程安全吗? | ✅ 01-L2 (AtomicLong getAndIncrement) |
| 2 | 开发者 | 位点 | advanceMaxSeqNo 什么时候被调? (副本/恢复) | ✅ 01-L2 |
| 3 | 开发者 | checkpoint | markSeqNoAsProcessed 重复调用会怎样? | ⚠️ 01 未提幂等 → 补一句 (seqNo ≤ checkpoint 直接 return, L116-119) |
| 4 | 开发者 | 复制 | performOnReplica 失败后 pendingActions 怎么减? | ✅ 02-L4 (decPendingAndFinishIfNeeded) |
| 5 | 架构师 | 位点 | 为什么 version 不够用, 要 seqNo? | ✅ 01-L1 (三维版本) |
| 6 | 架构师 | checkpoint | processed 和 persisted 为什么分离? | ✅ 01-L3 (复制可用 vs 恢复安全) |
| 7 | 架构师 | 复制 | globalCheckpoint 为什么必须 min 聚合? | ✅ 02-L2 (不完整不推进) |
| 8 | 架构师 | 保护 | 租约过期后会发生什么? | ⚠️ 03 未提过期后果 → 补一句 (历史被 merge 清理, 相关恢复失败) |
| 9 | 性能工程师 | 位点 | 位图 1024 分段的内存效率? | ✅ 01-L4 (段满即删) |
| 10 | 性能工程师 | checkpoint | persisted 滞后 (ASYNC) 的影响? | ✅ 01-L3 (E-3 衔接 5s) |
| 11 | 性能工程师 | 复制 | 副本慢对写入吞吐的影响? | ✅ 02-L2 (min 聚合拖后腿) |
| 12 | SRE | 复制 | 副本失败后怎么恢复? (peer recovery?) | ⚠️ 02-L4 提 stale 未展开 recovery → 补一句 (E-5 展开) |
| 13 | SRE | 保护 | 租约持久化防什么? (重启丢租约?) | ✅ 03-L2 (persistRetentionLeases) |
| 14 | SRE | 脑裂 | 旧主恢复后怎么降级? | ✅ 03-L3 (term 落后降级) |
| 15 | 研究者 | 对照 | Redis offset vs ES seqNo 各自适用? | ✅ 03-L4 (命令流 vs 操作集) |
| 16 | 研究者 | 时空 | seqNo 三连发 (2015-10/11/12) 的设计演进? | ✅ 01-L2 + temporal-trace |
| 17 | 研究者 | 对照 | 租约 vs Redis 的什么机制? | ⚠️ 03 未对照 → 补一句 (Redis 无等价物, 因其副本不读历史) |
| 18 | 子系统开发者 | 衔接 | persisted 推进谁触发? (E-3) | ✅ 01-L3 (translog sync 回调) |
| 19 | 子系统开发者 | 衔接 | globalCheckpoint 消费方? (E-5 recovery) | ✅ 02-L5 (peer recovery 从 +1) |
| 20 | 子系统开发者 | 衔接 | 租约保护谁? (E-8 merge) | ✅ 03-L2 |
| 21 | 学生 | 位点 | seqNo 从 0 开始吗? | ✅ 01-L2 (NO_OPS_PERFORMED=-1 起始) |
| 22 | 学生 | checkpoint | "水位"是什么意思? | ✅ 01-L4 (连续前缀) |
| 23 | 学生 | 复制 | 副本挂了主还能写吗? | ✅ 02-L4 (可写, 失败标 stale) |
| 24 | 学生 | 脑裂 | ES 和 Redis 谁防脑裂更强? | ✅ 03-L4 (term 硬隔离) |

**统计**: ✅ 20 / ⚠️ 4 / ❌ 0 — ⚠️ 全部"补一句"级
→ 回补 4 项: 01-L3 幂等 / 03-L2 租约过期后果 / 02-L4 peer recovery 衔接 / 03-L2 Redis 无租约等价物

## 回补清单

1. 01-L3: 补 markSeqNo 幂等 (seqNo ≤ checkpoint 直接 return, LocalCheckpointTracker.java:116-119)
2. 02-L4: 补 stale 后 peer recovery 恢复 (E-5 衔接)
3. 03-L2: 补租约过期后果 (历史被 merge 清理, 依赖它的恢复失败)
4. 03-L2: 补 Redis 无租约等价物一句 (副本不读历史)
