# E-3 Translog — 时空溯源 (v0.90 → v8.12.2)

> 方法: git show 早期 tag 源码 + git log 关键 commit; 2026-08-14 已 fetch --unshallow (509 tags)
> 断代锚点: v0.90.0 / v2.0.0 / v5.0.0-alpha1 / v6.0.0-alpha1 / v7.0.0-alpha1 / v8.12.2

## 演进主线 (6 代)

| 代 | 版本 | 结构 | 关键决策 |
|:--:|---|---|---|
| 1 | v0.90 | `TranslogService` (定时任务) + `fs/` 子包 (FsTranslog/FsTranslogFile/BufferingFsTranslogFile) | **独立定时线程轮询 flush**: interval=5s, 三阈值 (ops>5000 / size>200MB / period>30min) (TranslogService.java:77-80) |
| 2 | v2.0 | `TranslogWriter/BufferingTranslogWriter + ChannelReference + ImmutableTranslogReader` 成形; TranslogService 仍在 | **TRANSLOG_UUID_KEY 引入** (Translog.java:103, javadoc "since Elasticsearch 2.0") — Lucene commit 与 translog 强关联防错恢复 |
| 3 | v5.0 | `BaseTranslogReader/Checkpoint/TranslogReader/TranslogSnapshot` 成形, fs/ 子包消失, **TranslogService 删除** (2015-09-23, 75e816400c2) | **seqNo 化**: Operation 带 seqNo; 定时 flush → **同步 afterWriteOperation 决策** ("size and ops based flush into a synchronous API into IndexShard... removes the time-based flush alltogether" — commit msg) |
| 4 | v6.0 | + ChannelFactory / TruncateTranslogCommand | **TranslogDeletionPolicy 引入** (commit 1775e4253eb, 2017-06-01, "Introducing a translog deletion policy #24950") — 引用计数按"安全水位"删代 (实为 6.x 早期, v5.0 目录无此文件 — ls-tree 实证) |
| 5 | v7.0 | + TranslogHeader / TragicExceptionHolder | soft-deletes 与 translog 分工 (commit ac84879a716, 2018-04-19, "Use soft deletes to maintain doc history #29549"); header 独立成文件承载 uuid+primaryTerm |
| 6 | v8.12 | 现行 19 文件 | trimmedAboveSeqNo (peer recovery 截断) + durability 双模式 + 双缓冲 writer |

## 三个核心设计变迁

### 1. 定时轮询 → 写路径同步决策 (v5.0)

```
v0.90: TranslogService.schedule(5s) → 轮询三阈值 → asyncFlush (独立线程, 最多 5s 延迟)
v8.12: IndexShard.afterWriteOperation (IndexShard.java:3763) → shouldPeriodicallyFlush() || shouldRollTranslogGeneration()
       → flushOrRollRunning CAS 单飞 (IndexShard.java:3761) → FLUSH 线程池异步执行
```
- 设计原因 (commit 75e816400c2): "we can actually make all the decisions in a sync manner which is way easier to control and to test"
- 现代 flush 阈值: size=512MB/2 (write 时) 或 512MB (periodic), age=1min/2 (InternalEngine.java:2116-2123)
- **对照**: flush 判定从"后台定时器"变为"写操作副作用" — 语义上"每次写后自查", 零额外线程

### 2. 无 UUID → 强关联 (v2.0)

```
v0.90: translog 与 lucene 无绑定, 恢复靠 generation 号
v2.0:  TRANSLOG_UUID_KEY (Translog.java:103) — commit 时写 lucene 索引 + translog header 存 uuid
       TranslogHeader.read (TranslogHeader.java:111-158): 校验 uuid 不匹配 → "this translog file belongs to a different translog"
```
- 设计原因: 崩溃后误用旧 translog 恢复 = 数据错乱; uuid 是"日志属于哪个引擎"的防呆校验

### 3. 全量文件管理 → checkpoint 水位 + 引用计数删除 → 逻辑 trim (v2.0→v8.12)

```
v2.0:  TranslogWriter + ImmutableTranslogReader 直接管理文件
v6.0:  + TranslogDeletionPolicy (引用计数, commit 1775e4253eb) — 删除按"安全水位"
v8.12: + trimmedAboveSeqNo (逻辑截断, 不物理删) — 旧 primaryTerm 操作只改 checkpoint
```

## 对照 Redis AOF 演进

- Redis AOF: 全量操作日志, rewrite 后台全量重写 (server.c:1434-1440, auto-aof-rewrite-percentage=100)
- ES translog: 只含未提交操作, commit 后按 checkpoint 水位删代 — **不需要"重写"机制**, 因为 Lucene commit 承担了"压缩"
- 结论: 两种 WAL 的差异根因 = 数据模型 (内存引擎全量日志 vs 磁盘索引增量日志)

## REVIEW 修正记录 (2026-08-14)

- ❌ 初稿写 "v5.0: TranslogDeletionPolicy 出现" — **编造**: v5.0.0-alpha1 ls-tree 实证无此文件; 实际引入 = commit 1775e4253eb (2017-06-01, 6.x 时代) → 已改代 4
- ❌ 初稿写 "v6.0: soft-deletes 与 translog 分工" — soft deletes 实际 2018-04-19 (ac84879a716, 7.x) → 已改代 5
- ❌ afterWriteOperation (L3764) → IndexShard.java:3763; TranslogHeader.read (L137-158) → TranslogHeader.java:111-158

## 完成检查

- [x] 最早 tag v0.90 结构已读 (TranslogService+fs/)
- [x] v2.0 (UUID) / v5.0 (seqNo+删 TranslogService) / v6.0 (DeletionPolicy) / v7.0 (soft deletes) 断代 — 全部 commit 日期实证
- [x] 关键 commit 75e816400c2 + 1775e4253eb + ac84879a716 日期验证
- [x] 与 Redis AOF 演进对照
