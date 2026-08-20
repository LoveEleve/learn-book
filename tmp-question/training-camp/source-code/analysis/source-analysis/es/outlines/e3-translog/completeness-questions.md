# E-3 Translog — 全视角提问验证 (completeness)

> 验证时机: 3 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生 (7 身份)
> 覆盖统计: 见文末

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 写路径 | add() 并发下如何保证 offset 一致? (多个线程同时 add) | ✅ 01-L3/L5 (synchronized(this) 短临界区) |
| 2 | 开发者 | 写路径 | buffer 无界吗? 写太多内存会不会爆? | ⚠️ 01-L2 提到 forceWriteThreshold 未展开内存上限 → 补 01-L2 一句 |
| 3 | 开发者 | 写路径 | Location 语义: 为什么是 (gen, offset, size) 三元组? | ✅ 01-L2 (Location 记账) |
| 4 | 开发者 | 写路径 | 序列化格式: 操作怎么编码? size+checksum 头? | ⚠️ 02-L5 提 BufferedChecksumStreamInput 未展开格式 → 补 02-L5 一句 |
| 5 | 开发者 | checkpoint | trimmedAboveSeqNo 为什么不物理删? 空间不浪费吗? | ✅ 02-L5 (逻辑截断设计) |
| 6 | 开发者 | 恢复 | recoverFromFiles 打开失败 (文件缺失) 怎么处理? | ✅ 02-L3 (连续性断言) |
| 7 | 开发者 | 生命周期 | 引用计数泄漏会怎样? | ✅ 03-L3 (getMinTranslogGenRequiredByLocks) |
| 8 | 架构师 | 写路径 | 为什么选双缓冲不选直接写 channel? | ✅ 01-L2 (批量机会) |
| 9 | 架构师 | 写路径 | readLock 共享写 vs 独占锁 — 为什么敢并发写同一个日志? | ✅ 01-L5 (锁层次) |
| 10 | 架构师 | durability | REQUEST vs ASYNC 的取舍根因是什么? | ✅ 01-L4 (单线程 fsync) |
| 11 | 架构师 | checkpoint | 为什么 ckp 必须 <512B? 其他大小不行吗? | ✅ 02-L2 (单盘块原子写) |
| 12 | 架构师 | 生命周期 | 为什么用 generation 轮转不用单个文件? | ✅ 03-L2/L3 (封存+逐代删) |
| 13 | 架构师 | 生命周期 | translog 驱动 flush 而非定时 — 为什么? | ✅ 03-L4 (日志长=未提交多) |
| 14 | 性能工程师 | 写路径 | force(false) 能省多少? 元数据 fsync 的代价? | ✅ 01-L3 (force 语义) |
| 15 | 性能工程师 | 写路径 | 批量写多大最划算? forceWriteThreshold? | ⚠️ 01-L2 有阈值无数字 → 补数字 |
| 16 | 性能工程师 | durability | ASYNC 模式丢多少数据? 窗口多大? | ⚠️ 01-L4 未给量化 → 补一句 (async interval) |
| 17 | 性能工程师 | checkpoint | ckp 每次 sync 都写? 写盘频率? | ✅ 01-L3 (syncUpTo 每 sync 写) |
| 18 | SRE | 恢复 | 节点崩溃重启, 恢复耗时怎么评估? 与 translog 大小关系? | ⚠️ 02 未提恢复性能 → 补 02-L3 一句 |
| 19 | SRE | 恢复 | translog 损坏但 Lucene 完好 — 数据能救吗? | ✅ 02-L5 (corruption 检测) |
| 20 | SRE | tragedy | 生产事故: translog 失败后 shard 什么状态? 怎么恢复? | ⚠️ 02-L5 有机制无运维视角 → 补 02-L5 一句 (shard 进入 failed, 需 reroute) |
| 21 | SRE | 生命周期 | 磁盘上 translog 文件异常多, 怎么排查? | ⚠️ 03 未给运维信号 → 补 03-L3 一句 (stats API) |
| 22 | 研究者 | 写路径 | 与其他 WAL 对比: Kafka/MySQL binlog/LevelDB — 双缓冲是普遍模式吗? | ⚠️ 01 未对照 → 补 01-L6 一句 |
| 23 | 研究者 | checkpoint | 为什么 ES 用 checkpoint 文件而 Kafka 用 recovery point 内存态? | ⚠️ 02 未对照 → 补 02-L6 一句 |
| 24 | 研究者 | 生命周期 | AOF rewrite vs generation 轮转 — 什么场景会选反? | ✅ 03-L6 (差异根因) |
| 25 | 子系统开发者 | 写路径 | Engine 怎么消费 Location? (E-1 衔接) | ✅ 01-L2 (Location 供 ensureSynced) |
| 26 | 子系统开发者 | 恢复 | snapshot 给谁用? (peer recovery E-5) | ✅ 02-L3 引出 (newSnapshot L657) |
| 27 | 子系统开发者 | checkpoint | globalCheckpoint 字段谁写? (E-6 衔接) | ✅ 01-L3 (syncNeeded 三条件含 gcp) |
| 28 | 子系统开发者 | 生命周期 | flush 后 minTranslogGeneration 怎么更新? | ⚠️ 03-L4 未提 ckp 联动 → 补一句 |
| 29 | 学生 | 写路径 | translog 是什么? 为什么要它? | ✅ 01-L1 (问题引入) |
| 30 | 学生 | checkpoint | ckp 和 tlog 是什么关系? 先有谁? | ✅ 02-L1/L2 |
| 31 | 学生 | 生命周期 | 为什么叫 generation 不叫 version? | ⚠️ 03 未解释命名 → 补 03-L2 一句 |
| 32 | 学生 | 对照 | ES translog 和 Redis AOF 谁更像 MySQL binlog? | ⚠️ 03-L6 只对比 AOF → 补一句 |

**统计**: ✅ 19 / ⚠️ 13 / ❌ 0 — ⚠️ 项全部为"补一句"级 (无需改结构, 补数字/对照/运维视角即可)
→ 回大纲补全 (见下)

## 回补清单

1. 01-L2: 补 forceWriteThreshold 数字 + buffer 内存上限说明
2. 01-L4: 补 ASYNC 同步间隔量化 (async interval 默认 5s?)
3. 01-L6: 补一句 Kafka/MySQL binlog 同构 (WAL 通用模式)
4. 02-L3: 补恢复耗时评估 (重放多少操作)
5. 02-L5: 补操作格式 (size+checksum) + 运维视角 (shard failed + reroute)
6. 02-L6: 补 Kafka recovery point 对照一句
7. 03-L2: 补 generation 命名来源一句
8. 03-L3: 补 stats API 运维信号 + ckp 联动 (flush 后更新)
9. 03-L6: 补 MySQL binlog 对照一句
