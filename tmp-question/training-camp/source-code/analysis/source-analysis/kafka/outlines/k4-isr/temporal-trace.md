# K-4 Partition & ISR — 时空溯源 (2026-08-15)

> 🔴 A 域强制 | 方法: git log --diff-filter=A 找引入 commit + git log -1 日期实证

## 断代链

| 时间 | commit | 里程碑 | 演进 |
|---|---|---|---|
| 2011-08-01 | Initial checkin (Apache SVN) | **Partition.scala 初始** | 副本管理从第一天就是核心 (LinkedIn 时代) |
| 2012-03-23 | KAFKA-307 | **ReplicaManager 重构** | server 代码解耦 — 副本管理独立成中枢 |
| 2012-06-23 | KAFKA-339 | **AbstractFetcherThread** | follower 用 MultiFetch 批量拉取 — 拉取协议定型 |
| 2013-03-12 | kafka-763 | **unclean election 选项** | "replica from the largest offset during unclean leader election" — 可用性/一致性开关引入 (0.11.0.0 起默认 false, 设计文档) |
| **2017-04-06** | **KIP-101** (0baea2ac13) | **epoch 截断诞生** | "Alter Replication Protocol to use Leader Epoch rather than High Watermark for Truncation" — 截断从 HW 升级为 leader epoch (源码注释 "post-KIP-101 and pre-KIP-279" 即此) |
| 2018-05-09 | KAFKA-6361 | **快速故障转移修复** | "Fix log divergence between leader and follower after fast leader fail over" — 连续选举的截断正确性 |
| 2019-09-18 | KAFKA-8841 | **updateFollowerFetchState 优化** | "Reduce overhead of ReplicaManager.updateFollowerFetchState" |
| 2020-01-17 | expandIsr 测试改进 | **ISR 并发语义加固** | "ensure read threads not blocked on write" — leaderIsrUpdateLock 读写锁语义 |

## 关键观察

1. **2017 KIP-101 是截断语义的分水岭**: 之前用 HW 截断 (可能丢已提交数据), 之后用 leader epoch 定位 (4 规则) — 这就是为什么 4 规则里有"老协议回退 HW" (规则 1/2 的 IBP 兼容分支)
2. **ISR 模型 2011 年就是设计** (Partition 初始即含), 但"动态维护+锁外上报"是 2019-2020 年才完全成型 (KAFKA-8841/KAFKA-19264 系列)
3. **unclean election 是 2013 引入的选项, 0.11.0.0 才默认关闭** — 设计文档明示 "By default from version 0.11.0.0"
4. 与 K-3 对照: K-3 分段存储 2012 定型后 14 年结构未变; K-4 的 epoch 语义 2017 才定型 — **存储层稳定早, 复制层演进晚** (一致性协议随实践迭代)

## 对照锚点

- ES E-6 SeqNo: ES 2015 引入 seqNo/term 复制协议; Kafka 2017 KIP-101 epoch — 两家分别在各自时间线用"逻辑位点"解决截断问题
- Redis r9: Redis 主从复制偏移量 (字节位点) vs Kafka leader epoch (逻辑位点) — K-4 篇 4 对照扩展
