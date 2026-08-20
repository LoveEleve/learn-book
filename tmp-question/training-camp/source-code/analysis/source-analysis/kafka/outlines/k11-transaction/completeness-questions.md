# K-11 事务与幂等 — 全视角提问验证 (completeness)

> 验证时机: 2 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 幂等 | producerId 哪来? | ✅ 01-L2 (handleInitProducerId) |
| 2 | 开发者 | 幂等 | 重试怎么不重? | ✅ 01-L2 (K-3 seq 判重) |
| 3 | 开发者 | 状态机 | 客户端五态? | ✅ 01-L3 (init/begin/commit/abort) |
| 4 | 开发者 | 两阶段 | Prepare/Complete 状态? | ✅ 02-L2 (TransactionCoordinator.scala:L562-593) |
| 5 | 开发者 | Marker | 提交结果发给谁? | ✅ 02-L3 (分区 leader) |
| 6 | 开发者 | 隔离 | read_committed 怎么过滤? | ✅ 02-L3 (LSO) |
| 7 | 架构师 | 设计 | 幂等和事务关系? | ✅ 01 核心悬念 (递进) |
| 8 | 架构师 | 设计 | 为什么需要 Marker 环? | ✅ 02 核心悬念 (决定→分发→标记) |
| 9 | 架构师 | 设计 | epoch 防僵尸? | ✅ 02-L3 (fencing) |
| 10 | 架构师 | 设计 | 状态存哪? | ✅ 02-L4 (__transaction_state) |
| 11 | 性能工程师 | 代价 | 事务开销? | ⚠️ 02 未显式提 → 补一句 (Marker 广播 RTT) |
| 12 | 性能工程师 | 隔离 | LSO 语义? | ✅ 02-L3 (ConsumerConfig ConsumerConfig.java:L368) |
| 13 | SRE | 故障 | 协调器挂了? | ⚠️ 02 未显式提 → 补一句 (K-5 协调器定位重试) |
| 14 | SRE | 恢复 | 状态怎么恢复? | ✅ 02-L4 (记录重建) |
| 15 | 研究者 | 对照 | vs Redis MULTI? | ✅ 02-L4 (单机 vs 跨 broker) |
| 16 | 研究者 | 对照 | vs fencing? | ✅ 02 header (rd2-rlock) |
| 17 | 子系统开发者 | 衔接 | 与 K-3 存储? | ✅ 02-L3 (ControlBatch) |
| 18 | 学生 | 概念 | 事务通俗解释? | ✅ 02-L1 (跨分区原子) |

**统计**: ✅ 16 / ⚠️ 2 / ❌ 0 — ⚠️ 2 项"补一句"级
→ 回补 2 项: 02-L2 补事务开销; 02-L3 补协调器故障重试

## 回补清单

1. 02-L2: 补 "代价: 每事务 Marker 广播 (跨分区 RTT) + 状态写 __transaction_state — 高频小事务慎用"
2. 02-L3: 补 "协调器故障: 客户端重试 (transactional.id 重新定位, K-5 衔接)"
