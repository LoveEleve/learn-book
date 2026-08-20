# E-3 Translog — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 叶子域 (零 ES 内部依赖) | 对照: [[r8-persistence]] (Redis AOF)
> 源码: server/src/main/java/org/elasticsearch/index/translog/ (19 文件 4647 行)
> 测试地图: server/src/test/.../index/translog/ 7 文件 (TranslogTests 40+ 测试方法)

## 继承树/调用图

```
Translog (1941) ──持有──→ TranslogWriter (663) ←── BaseTranslogReader (156)
    │                           │                        ▲
    │ readers: List<TranslogReader>                  TranslogReader (161)
    │ current: TranslogWriter                          │
    │ deletionPolicy: TranslogDeletionPolicy (126)     └── newSnapshot → TranslogSnapshot (135)
    │ config: TranslogConfig (123)                    MultiSnapshot (97) 组合多代
    │ header: TranslogHeader (190)                    Checkpoint (262) 单盘块原子写
    └── Operation (Index/Delete/NoOp) ←── 序列化: BufferedChecksumStreamInput (147) / Output (60)

调用方 (E-1 Engine 消费):
    InternalEngine.index (InternalEngine.java:1131) → translog.add(operation) (Translog.java:575)
    InternalEngine.flush (InternalEngine.java:2173) → translog.rollGeneration (Translog.java:1628) + createNewTranslog
    Recovery: IndexShard → translog.newSnapshot (Translog.java:657) 回放
```

## 基本元素分解 (原则二)

1. **Operation 序列化单元** — Index/Delete/NoOp 三种操作, 写前拼 size+checksum 头 (writeOperationWithSize)
2. **Writer 双缓冲** — TranslogWriter.add (TranslogWriter.java:227): buffer (堆内可释放) → 批量 writeAndReleaseOps → FileChannel.force(false) — 内存缓冲 + 通道写入分离, sync 锁控制
3. **Checkpoint 单盘块** — 8 字段 (offset/numOps/gen/minSeqNo/maxSeqNo/globalCheckpoint/minTranslogGeneration/trimmedAboveSeqNo), V4_FILE_SIZE < 512B 保证原子写 (Checkpoint.java:228-229 assert)
4. **Generation 轮转** — rollGeneration (Translog.java:1628): 当前 writer closeIntoReader → readers.add → copyCheckpointTo(translog-gen.ckp) → createWriter(gen+1)
5. **删除策略** — TranslogDeletionPolicy: translogRefCounts 引用计数 + localCheckpointOfSafeCommit; getMinReferencedGen = min(锁引用代, seqNo 需要代)
6. **恢复回放** — recoverFromFiles (Translog.java:225): checkpoint.generation 倒序打开, translog-gen.ckp 逐代读, 校验 UUID/primaryTerm

## 标记问题 (≥5)

1. **Q1: add() 为什么不直接写磁盘?** — 双缓冲设计: buffer 聚合 → syncUpTo (TranslogWriter.java:465) 才 writeAndReleaseOps + force — 为什么 buffer 不 fsync 也算"持久化"? (durability 语义)
2. **Q2: syncNeeded 三条件** — totalOffset != lastSynced.offset || globalCheckpoint 变了 || minTranslogGeneration 变了 (TranslogWriter.java:356-360) — 为什么 globalCheckpoint 也要 fsync?
3. **Q3: force(false) vs force(true)** — Checkpoint.write(FileChannel) 用 force(false) (Checkpoint.java:202), write(ChannelFactory) 用 force(true) (Checkpoint.java:193) — 差异原因? (文件长度不变时 metadata 不需刷)
4. **Q4: 恢复为什么倒序开文件?** — recoverFromFiles (Translog.java:225): `for (i = checkpoint.generation; i >= minGenerationToRecoverFrom; i--)` (Translog.java:236) — 先验证最新代 UUID 再往前? 错误消息质量?
5. **Q5: trimmedAboveSeqNo 是什么?** — Checkpoint 8 字段最神秘: maxEffectiveSeqNo (Checkpoint.java:110-115) — peer recovery 截断本地 translog 的机制
6. **Q6: 锁层次** — readLock/writeLock/syncLock/synchronized(this) 四层 — 为什么 add 只持 readLock? (并发写 + 串行 sync 的设计)
7. **Q7: tragedy 机制** — TragicExceptionHolder: 任何 IO 异常 → closeOnTragicEvent → 整个 translog 关闭 — 为什么"一次失败全关"? (防止半持久化)
8. **Q8: 与 Redis AOF 对照** — 两者都是"先写日志再改数据"的 WAL, 但 AOF rewrite vs ES generation 轮转+删除策略 — 设计差异?

## 已读测试 (2 个)

- `TranslogTests.testSimpleOperations` (TranslogTests.java:364-435): add Index/Delete/NoOp → newSnapshot 顺序读回 → rollGeneration 后仍可读全部 — 三种 Operation 往返
- `TranslogTests.testRangeSnapshot` (TranslogTests.java:723): newSnapshot(from,to) seqNo 范围过滤
- `TranslogTests.testRecoveryUncommitted` (TranslogTests.java:1768): 未提交操作恢复

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 2 个测试文件 (实际 3 个)

## 跨域发现

- 来源: E-3 Pass 1 — InternalEngine 是 Translog 唯一生产者/消费者 (E-1 域)
- 发现: flush 时 translog 的 minTranslogGeneration 记录在 checkpoint, 供 recovery 只回放未提交部分 — E-1/E-5 依赖此
- 已对照验证: InternalEngine.java:2121 (shouldPeriodicallyFlush), flush() L2173
