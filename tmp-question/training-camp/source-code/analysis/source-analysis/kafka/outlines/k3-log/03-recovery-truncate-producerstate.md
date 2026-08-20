# K-3 Log 存储 篇 3/3 — 崩溃与一致: 恢复/截断/ProducerState

> 前置: [[K-3-log-01]] [[K-3-log-02]] | 复用: — | 对照: [[E-3-translog]] (崩溃恢复对照) [[r22-expire]] (删除语义) | 引出: [[K-4-isr-01]] (ISR 副本同步) [[K-4-isr-04]] (HW 保护)
> 🔴 A | 来源: LogLoader.java:357-420,464-466 + LogSegment.java:478-524,557 + LocalLog.java:654-686 + ProducerStateManager.java:85-93,296,428-455 + UnifiedLog.java:1826
> 定位: K-3 卷收尾 — 回答"broker 崩溃重启怎么恢复? 截断怎么保一致? 幂等状态怎么持久?"

**读者处境**: 面试官问 "broker 断电重启, 数据会丢吗? 怎么恢复? 索引坏了呢?" 你答 "恢复" — 但再问 "干净关停和崩溃区别? recoveryPoint 是什么? 事务状态怎么恢复? 截断时 HW 怎么保护?" 你答不上来。这篇是可靠性机制的完整答案。

### 1. 问题引入 — 断电后的世界

场景: broker 突然断电, 重启后日志目录还是乱的 — 怎么恢复成一致状态?
- 恢复点 (recoveryPoint) + 段回放 (LogLoader.java:357-420)
- 本篇问题: 恢复 (Q5) / 截断 (Q6) / ProducerState (Q7)

### 2. 崩溃恢复 — 扫描/回放/重建

场景: 启动时 LogLoader 做什么?
- loadSegmentFiles (LogLoader.java:357-391): 孤儿索引删除 (LogLoader.java:364-371) → 每 .log 文件 LogSegment.open (LogLoader.java:376) → sanityCheck 失败/索引损坏 → recoverSegment (LogLoader.java:383-387)
- recoverSegment (LogLoader.java:400-420): 重建 ProducerStateManager (LogLoader.java:401-406) → rebuildProducerState (LogLoader.java:407-414) → segment.recover (LogLoader.java:415) → **立即快照** (LogLoader.java:418)
- 干净关停标记: hadCleanShutdown 跳过恢复 (LogLoader.java:466) — kafka_cleanshutdown 文件
- segment.recover (LogSegment.java:478-524): 索引重建 (LogSegment.java:479-481,496-500) → 坏数据截断 (LogSegment.java:518)
- 对照 E-3: ES translog 按 checkpoint 水位回放 vs Kafka 按 recoveryPoint + 干净关停标记 — 两代 WAL 恢复哲学

### 2.5 刷盘 — recoveryPoint 的前提

场景: "恢复点"到底是什么? 为什么不是每消息刷盘?
- 刷盘 = 四文件全刷: log + offsetIndex + timeIndex + txnIndex (LogSegment.flush, LogSegment.java:624-645)
- recoveryPoint 语义 (LocalLog.java:101): "第一个未刷盘到磁盘的 offset" — 恢复只重放 recoveryPoint 之后
- 双配置: LOG_FLUSH_INTERVAL_MESSAGES (LogConfig.java:166) + LOG_FLUSH_INTERVAL_MS (LogConfig.java:169), topic 级 FLUSH_MESSAGES_INTERVAL (LogConfig.java:194) / FLUSH_MS (LogConfig.java:196) — 对照设计文档 M 消息/S 秒 (docs/implementation/log.md)
- 负面空间: **不做每消息 fsync** — 刷盘是批量/定时, 崩溃最多丢 M 消息或 S 秒数据 (设计文档明示) — 性能 vs 持久性的显式取舍, 与 ES translog durability=REQUEST (每请求 fsync) 形成两代设计对照

### 3. 截断 — 删段与 HW 保护

场景: 分区截断 (leader 变更/删除) 怎么做?
- truncateTo (LocalLog.java:680-686): 删 baseOffset > target 的段 (LocalLog.java:681) → 活动段内截断 (LocalLog.java:683) → LEO 重置 (LocalLog.java:684)
- truncateFullyAndStartAt (LocalLog.java:654-672): 先建新段再删旧段 (LocalLog.java:663-666, 防活动段缺失窗口)
- **HW 保护**: highWatermark >= upperBoundOffset 才删 (UnifiedLog.java:1826) — 日志起点永不超过 HW (K-4 衔接)

### 4. ProducerState — 幂等/事务的持久化

场景: 幂等 producer 的状态存哪? 怎么恢复?
- 三容器 (ProducerStateManager.java:85-93): producers (ProducerStateManager.java:85) / ongoingTxns (ProducerStateManager.java:90) / unreplicatedTxns (ProducerStateManager.java:93)
- snapshot (ProducerStateManager.java:428-455): lastMapOffset > lastSnapOffset 才写 (ProducerStateManager.java:432) — 避免重复快照
- 恢复: loadFromSnapshot (ProducerStateManager.java:296) + snapshot 后回放 (LogLoader 衔接)
- K-11 衔接: 这是事务/幂等的 broker 侧状态底座

### 核心悬念
"断电重启 Kafka 为什么几乎不丢数据?" — 三重机制: ①恢复点之后才回放 (干净关停直接跳过) ②索引可重建 (数据文件是唯一真相) ③事务状态有快照 (幂等不重不漏) — 数据文件损坏才丢数据, 索引损坏永远可重建。**但"几乎不丢"≠"不丢": 未刷盘的消息在断电时丢失 (批量/定时刷盘的取舍, §2.5)**。

### 概念依赖链
Q5 恢复 → Q5.5 刷盘 → Q6 截断 → Q7 ProducerState → (K-4 HW/ISR / K-11 事务衔接)

### 源码锚点清单
- LogLoader.java:357-391 (loadSegmentFiles) / 364-371 (孤儿索引) / 376 (open) / 383-387 (recoverSegment 触发) / 400-420 (recoverSegment) / 401-406 (重建 ProducerStateManager) / 407-414 (rebuildProducerState) / 415 (segment.recover) / 418 (takeSnapshot) / 464-466 (recoverLog+干净关停)
- LogSegment.java:478-524 (recover) / 479-481 (索引 reset) / 496-500 (重建) / 518 (truncateTo validBytes) / 557 (truncateTo) / 624-645 (flush 四文件全刷)
- LocalLog.java:101 (recoveryPoint 语义) / 251 (unflushedMessages) / 654-672 (truncateFullyAndStartAt) / 663-666 (先建后删) / 668 (updateLEO) / 680-686 (truncateTo) / 681 (段级删除) / 683 (段内截断) / 684 (LEO 重置)
- ProducerStateManager.java:85-93 (三容器) / 296 (loadFromSnapshot) / 428-455 (takeSnapshot) / 432 (防重复)
- UnifiedLog.java:1826 (HW 保护)
- LogConfig.java:166 (LOG_FLUSH_INTERVAL_MESSAGES) / 169 (LOG_FLUSH_INTERVAL_MS) / 194 (FLUSH_MESSAGES_INTERVAL) / 196 (FLUSH_MS)
- docs/implementation/log.md (设计文档: M 消息/S 秒持久化保证)
