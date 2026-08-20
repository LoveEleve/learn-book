# E-6 SeqNo 篇 1/3 — 位点体系: seqNo 与双 checkpoint

> 前置: [[E-1-engine-01]] (seqNo 生成消费) [[E-3-translog-01]] (persisted 回调) | 复用: — | 对照: [[r9-replication]] | 引出: [[E-6-seqno-02]] [[E-6-seqno-03]] [[E-5-shard]]
> 🔴 A | 来源: LocalCheckpointTracker.java:19-127 + SequenceNumbers.java:23-32 + InternalEngine.java:1243-1248
> 定位: SeqNo 卷开篇 — 回答"seqNo 怎么分配? 乱序完成怎么推进水位?"

**读者处境**: ES 每个操作有个递增的数字 (seqNo), 还有个"水位" (checkpoint) — 面试官问 "seqNo 和 version 什么区别? 乱序写入 checkpoint 怎么推进?" 你答 "位图 + 连续跳跃" — 这篇是位点体系的完整答案。

### 1. 问题引入 — 为什么需要逻辑位点

场景: 主分片 1000 个并发写, 每个操作要能被复制/恢复精确定位 — version 号 (客户端乐观锁) 不够用, 因为它是"每文档"不是"每操作"。
- 三维版本: version (客户端) / seqNo (操作位点) / primaryTerm (主世代)
- 哨兵值 (SequenceNumbers.java:23-32): UNASSIGNED_SEQ_NO=-2 / NO_OPS_PERFORMED=-1 / UNASSIGNED_PRIMARY_TERM=0

### 2. seqNo 分配 — generateSeqNo

场景: seqNo 从哪来?
- LocalCheckpointTracker.generateSeqNo (LocalCheckpointTracker.java:83-85): `nextSeqNo.getAndIncrement()` — AtomicLong 单调递增
- advanceMaxSeqNo (LocalCheckpointTracker.java:90-92): 副本/恢复传入更大 seqNo 时推进 (accumulateAndGet Math::max)
- E-1 衔接: 主分片在 plan 阶段生成 (InternalEngine.java:1105), 副本复用传入值
- 时空溯源: v2.0 无 seqNo (grep 实证) → 2015-11-19 引入 (5fb0f9a88ff)

### 3. 双 checkpoint — processed vs persisted

场景: 为什么两个水位?
- 结构 (LocalCheckpointTracker.java:13-27): processedSeqNo/persistedSeqNo 两个位图 + 两个 AtomicLong
- 推进路径: markSeqNoAsProcessed (LocalCheckpointTracker.java:99) = 引擎处理完 (versionMap 已更新); markSeqNoAsPersisted (LocalCheckpointTracker.java:108) = translog 落盘
- E-1 衔接 (InternalEngine.java:1243-1248): 无 translogLocation 立即 persisted (InternalEngine.java:1247); 否则等 translog sync 回调 (InternalEngine.java:255, E-3 的 persistedSequenceNumberConsumer)
- 语义: processed 推进 globalCheckpoint (复制可用), persisted 是恢复安全水位
- **幂等**: markSeqNo 对 seqNo ≤ checkpoint 直接 return (LocalCheckpointTracker.java:116-119) — 恢复期重放已复制操作安全

### 4. CountedBitSet — 乱序水位推进

场景: seqNo 5 先完成, 4 后完成 — checkpoint 怎么动?
- markSeqNo (LocalCheckpointTracker.java:112-127): 乱序标记位图 (LocalCheckpointTracker.java:121) → 仅 `seqNo == checkpoint+1` 时 updateCheckpoint (LocalCheckpointTracker.java:124-126)
- updateCheckpoint (LocalCheckpointTracker.java:191-218): do-while 连续跳跃 — 位图段 (BIT_SET_SIZE=1024, LocalCheckpointTracker.java:25) 满即删
- 测试实证: testSimplePrimaryProcessed (LocalCheckpointTracker.java:45-67): 5 先标 checkpoint 不动 → 4 补齐后跳 6
- 不变式: "≤checkpoint 的所有 seqNo 必已处理"

### 5. 收束 — 位点体系的定位

- seqNo = 分片内单调位点, checkpoint = 连续前缀水位, primaryTerm = 主世代 (篇 3 展开)
- 对照 Redis: 字节偏移 vs 逻辑位点 (篇 3 详述)
- 引出: 篇 2 (globalCheckpoint 跨副本聚合) — 篇 3 (租约/脑裂/对照)

### 核心悬念
"乱序完成的操作, 水位怎么保证'≤N 全已处理'?" — 位图记录乱序完成, checkpoint 只在连续前缀补齐时一次性跳跃 — 位图窗口 + 连续水位的不变式。

### 概念依赖链
Q5 位点三元组 → Q1 双 checkpoint → Q2 位图推进 → (时空溯源)

### 源码锚点清单
- LocalCheckpointTracker.java:19-27 (双 map + 双 checkpoint) / 83-92 (generateSeqNo/advanceMaxSeqNo) / 99-108 (markSeqNoAsProcessed/Persisted) / 112-127 (markSeqNo 乱序) / 191-218 (updateCheckpoint 连续跳跃) / 25 (BIT_SET_SIZE=1024)
- SequenceNumbers.java:23-32 (哨兵值)
- InternalEngine.java:1105 (generateSeqNoForOperationOnPrimary) / 1243-1248 (processed→persisted) / 255 (translog sync 回调)
- LocalCheckpointTrackerTests.java:45-67 (乱序推进测试)
