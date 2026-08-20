# E-5 Shard — 全视角提问验证 (completeness)

> 验证时机: 3 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 状态机 | updateShardState 线程安全吗? (mutex?) | ✅ 01-L2 (changeState mutex) |
| 2 | 开发者 | 状态机 | 主→副本迁移为什么非法? | ✅ 01-L2 (防双主) |
| 3 | 开发者 | permits | acquire 超时语义? (timeout?) | ⚠️ 01-L3 未提超时 → 补一句 (tryAcquire timeout, ElasticsearchTimeoutException) |
| 4 | 开发者 | 主升 | resync 期间新写怎么办? | ✅ 02-L2 (blockOperations 延迟) |
| 5 | 架构师 | 状态机 | 为什么 RELOCATED 从分片状态移除? | ✅ 01-L2 (路由层概念) + 时空溯源 |
| 6 | 架构师 | permits | MAX_VALUE 信号量的设计意图? | ✅ 01-L3 (并发+阻塞两模式) |
| 7 | 架构师 | 主升 | 升主为什么必须补 seqNo 洞? | ✅ 02-L2 (testPrimaryFillsSeqNoGaps) |
| 8 | 架构师 | 复制组 | 复制组为什么是不可变快照? | ✅ 02-L3 (复制中路由变化不生效) |
| 9 | 性能工程师 | permits | blockOperations 阻塞多久? | ⚠️ 01-L3 有 timeout 未量化 → 补一句 (调用方传, 默认场景) |
| 10 | 性能工程师 | 恢复 | 恢复耗时与什么相关? | ✅ 03-L2 (Store 段 + translog 未提交量) |
| 11 | SRE | 状态机 | 分片卡在 POST_RECOVERY 怎么办? | ⚠️ 03 未提运维 → 补一句 (master 未确认 active, 检查集群状态) |
| 12 | SRE | 关闭 | close 时在途写会丢吗? | ✅ 01-L4 (flushEngine 可配 + 排空) |
| 13 | SRE | 主升 | 升主后旧主回来会怎样? | ✅ 02-L2 (term 落后拒写) + 03-L3 |
| 14 | SRE | 恢复 | 崩溃后从哪恢复? (store vs 副本?) | ✅ 03-L2 (本地 store + translog) |
| 15 | 研究者 | 对照 | Redis 哨兵 vs ES 状态机哲学? | ✅ 03-L3 |
| 16 | 研究者 | 时空 | 接口/实现分离 → 单类膨胀原因? | ✅ 01-L2 + temporal-trace |
| 17 | 研究者 | 对照 | ES 升主 vs Redis failover 数据安全? | ✅ 03-L3 (gcp 保证) |
| 18 | 子系统开发者 | 衔接 | updateShardState 谁调用? (E-10) | ✅ 01-L2 (集群驱动) |
| 19 | 子系统开发者 | 衔接 | 复制组消费方? (E-6) | ✅ 02-L3 (ReplicationOperation L136) |
| 20 | 子系统开发者 | 衔接 | 恢复回放谁提供? (E-3) | ✅ 03-L2 (translog newSnapshot) |
| 21 | 学生 | 状态机 | 分片有几种状态? | ✅ 01-L2 (5 态) |
| 22 | 学生 | permits | 为什么叫"许可"? | ✅ 01-L3 (操作资格) |
| 23 | 学生 | 主升 | 副本怎么变成主? | ✅ 02-L2 (四步) |
| 24 | 学生 | 恢复 | 节点重启分片数据还在吗? | ✅ 03-L2 (store 持久化) |

**统计**: ✅ 21 / ⚠️ 3 / ❌ 0 — ⚠️ 全部"补一句"级
→ 回补 3 项: 01-L3 acquire 超时 / 01-L3 blockOperations 阻塞时长 / 03-L2 POST_RECOVERY 卡住运维

## 回补清单

1. 01-L3: 补 acquireAll 超时 (tryAcquire timeout → ElasticsearchTimeoutException, IndexShardOperationPermits.java:152-153)
2. 01-L3: 补 blockOperations timeout 调用方语义 (调用方传值)
3. 03-L2: 补 POST_RECOVERY 卡住运维视角 (master 未确认 active)
