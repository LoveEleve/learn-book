# E-6 SeqNo — harness 验证记录 (MiniSeqNo 16/16)

> 跑法: `javac MiniSeqNo.java MiniSeqNoTest.java && java MiniSeqNoTest` (JDK 21)
> 结果: **16/16 PASS** (首跑全绿)

## 验证矩阵

| # | 机制 | 验证点 | 源码对照 | 结果 |
|:--:|---|---|---|:--:|
| A1-A3 | seqNo 分配 | 单调递增 + advanceMaxSeqNo 推进 | LocalCheckpointTracker.java:83-92 | PASS |
| A4-A6 | 双 checkpoint | processed 推进但 persisted 不动 | LocalCheckpointTracker.java:99-108 | PASS |
| B1-B4 | 乱序推进 | 先标 2 不动 → 补齐 0/1 连续跳 2 | LocalCheckpointTracker.java:112-127,191-218 | PASS |
| B5 | 幂等 | 重复标记已推进 seqNo 安全 | LocalCheckpointTracker.java:116-119 | PASS |
| C1-C2 | globalCheckpoint | min of in-sync; 非 in-sync 不计 | ReplicationTracker.java:1349-1370 | PASS |
| C3-C4 | 双回退 | pendingInSync/UNASSIGNED → fallback | ReplicationTracker.java:1356-1362 | PASS |
| C5 | 并发 | 100 乱序完成最终水位 99 | updateCheckpoint 连续跳跃 | PASS |

## 验证意义

- seqNo 分配 / 双 checkpoint 分离 / 乱序水位连续跳跃 / globalCheckpoint min 聚合 + 双回退 — 4 大机制全部可复现
- **未验证面** (harness 边界): ReplicationOperation 完整状态机 (ack 聚合/重试), RetentionLease 租约管理, primaryTerm 脑裂隔离 (需多节点模拟), 真实 FixedBitSet 内存行为
- 结论: "分配 → 处理水位 → 跨副本水位" 的 SeqNo 三层语义理解验证到位
