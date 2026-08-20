# K-3 Log 存储 — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: 无 (阶段4 首个域, 存储底座) | 对照: [[r8-persistence]] (Redis RDB/AOF) [[E-3-translog]] (ES WAL)
> 源码: storage/src/main/java/org/apache/kafka/storage/internals/log/ (76 文件)
> 测试地图: storage/src/test/.../internals/log/ (17 文件: LocalLogTest/LogSegmentTest/ProducerStateManagerTest/UnifiedLogTest)

## 继承树/调用图

```
LogManager (LogManager.java, 生命周期: open/roll/cleanup)
└── UnifiedLog (UnifiedLog.java:2685 行, Kafka 2.x+ 聚合层)
      ├── LocalLog (LocalLog.java:68, 1074 行) — 段管理核心
      │     ├── LogSegments (段容器: append/roll/truncate)
      │     │     └── LogSegment (LogSegment.java, 902 行) — 单段
      │     │           ├── FileRecords (.log 数据)
      │     │           ├── LazyIndex<OffsetIndex> (.index, LazyIndex.java:248 行)
      │     │           ├── LazyIndex<TimeIndex> (.timeindex)
      │     │           └── TransactionIndex (.txnindex)
      │     ├── LogLoader (LogLoader.java, 565 行) — 启动恢复
      │     └── ProducerStateManager (ProducerStateManager.java, 710 行) — 幂等/事务状态
      ├── LogConfig (配置)
      └── LogCleaner/LogCleanerManager (K-10 衔接)
```

## 基本元素分解 (原则二)

1. **分段存储** — 分区目录 → 多个 LogSegment, 按 baseOffset 排序 (LogSegments); 单段 4 文件: .log/.index/.timeindex/.txnindex
2. **append 写路径** — LocalLog.append (LocalLog.java:526) → LogSegment.append (LogSegment.java:250): 写 FileRecords + LEO 推进 + 按 indexIntervalBytes 稀疏索引
3. **轮转 roll** — LocalLog.roll (LocalLog.java:581): 达到 segment 上限/时间 → 封存旧段建新段
4. **读路径** — LocalLog.read (LocalLog.java:462) → LogSegment.read (LogSegment.java:431): startOffset/maxSize/maxPosition/minOneMessage
5. **索引** — OffsetIndex (offset→position 稀疏) + TimeIndex (timestamp→offset) + LazyIndex 延迟加载 (LazyIndex.java)
6. **启动恢复** — LogLoader: 加载全部段 + 从 lastOffset 回放 + 索引校验重建
7. **截断** — LocalLog.truncateTo (LocalLog.java:680) / truncateFullyAndStartAt (LocalLog.java:654) — 按 offset 删除段 + 索引截断
8. **ProducerState** — ProducerStateManager: producerId→(epoch,seq,txn) 状态, snapshot 文件持久化 (K-11 衔接)

## 标记问题 (≥5)

1. **Q1: 分段结构** — LogSegment 4 文件怎么协作?LogSegments 容器怎么管理段生命周期? (LogSegment.java:84-104 字段)
2. **Q2: append 写路径** — 写入顺序: FileRecords → LEO → 索引?索引什么时机写 (indexIntervalBytes)? (LocalLog.java:526 + LogSegment.java:250)
3. **Q3: 稀疏索引** — OffsetIndex 怎么用二分查 offset→position?LazyIndex 怎么延迟 mmap? (LazyIndex.java + OffsetIndex)
4. **Q4: roll 轮转时机** — segment.bytes 默认多大?时间条件?滚段时索引怎么收尾? (LocalLog.java:581 + LogConfig)
5. **Q5: 崩溃恢复** — LogLoader 加载顺序?recoveryPoint 语义?索引损坏怎么重建? (LogLoader.java + LocalLog.recoveryPoint L94)
6. **Q6: 截断语义** — truncateTo 删除哪些段?段内截断索引怎么修?与 HW 关系? (LocalLog.java:680 + LogSegment.truncateTo L557)
7. **Q7: ProducerStateManager** — producerId 状态条目字段?snapshot 什么时机写?崩溃恢复怎么重建? (ProducerStateManager.java)
8. **Q8: 读路径边界** — minOneMessage/maxPosition 语义?gap 处理? (LogSegment.read L431)

## 已读测试 (3 个)

- `LocalLogTest`: testLogAppend/testRollEmptyActiveSegment/testTruncateFullyAndStartAt — 段生命周期
- `LogSegmentTest`: testReadFromGap/testReadFromMiddleOfBatch/testRecoveryFixesCorruptIndex — 读边界+索引恢复
- `ProducerStateManagerTest`: testProducerSequenceWrapAround/testTxnFirstOffsetMetadataCached/testHasLateTransaction — 状态条目

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (8 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: K-3 Pass 1 — storage/internals/log 76 文件, LogSegment 4 文件结构与 ES 段合并 (E-8) 同构 (append-only + 索引 + 清理)
- 发现: ProducerStateManager 是 K-11 事务/幂等的基础; LogCleaner 是 K-10 的基础 — K-3 是存储面 Hub
- 已对照验证: 规划 R3 断言 (LazyIndex/ProducerStateManager snapshot) 待 Pass 2 源码实证
