# K-3 Log 存储 — 时空溯源 (2026-08-15)

> 🔴 A 域强制 | 方法: git log --diff-filter=A 找引入 commit + git log -1 日期实证

## 断代链

| 时间 | commit | 里程碑 | 演进 |
|---|---|---|---|
| 2011-08-01 | Initial checkin (Apache SVN) | **Log.scala 初始** | 单一日志文件结构 (LinkedIn 时代) |
| 2012-10-08 | KAFKA-506 "Move to logical offsets" | **分段存储成型** | LogSegment + OffsetIndex 引入 — 逻辑 offset 体系, 4 文件结构 (log/index/timeindex) 确立 |
| 2017-05-06 | KAFKA-5121 (KIP-98) | **事务状态** | ProducerStateManager 引入 — 幂等/事务的 broker 侧状态, snapshot 机制 |
| 2019-12-02 | KAFKA-9156 | **延迟加载** | LazyIndex (LazyTimeIndex/LazyOffsetIndex) — 数千段启动免全量 mmap |
| 2024-09-17 | KAFKA-14482 | **Java 化** | LogLoader/LocalLog 移入 storage 模块 (#17042) — 从 Scala 全面 Java 化 |

## 关键观察

1. **2012 KAFKA-506 是设计定型点**: 逻辑 offset (相对 baseOffset 索引) + 分段 + 稀疏索引 — 此后 14 年结构未变, 只在细节演进
2. **2017 KIP-98 是语义升级点**: 纯 append log → 带事务/幂等状态机 (ProducerStateManager), 引入 .txnindex 第 4 个文件
3. **2019 LazyIndex 是规模优化点**: broker 段数从百级到千级, 全量 mmap 启动太慢 → 延迟加载
4. **2024 Java 化**: Kafka 4.x 完成 Scala→Java 迁移 (LocalLog/UnifiedLog 分裂: LocalLog 段操作 vs UnifiedLog 聚合逻辑)

## 对照锚点

- 与 ES Translog (E-3): ES generation 轮转 vs Kafka segment roll — 同为 append-only WAL, 但 Kafka 段是**可读数据** (消费直接读), ES translog 是**暂存** (flush 后废弃) — 本质差异
- 与 Redis AOF (r8): AOF rewrite 压缩 vs Kafka 不压缩 (compaction 独立, K-10)
