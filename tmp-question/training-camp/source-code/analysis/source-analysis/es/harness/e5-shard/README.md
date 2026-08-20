# E-5 Shard — harness 验证记录 (MiniShard 16/16)

> 跑法: `javac MiniShard.java MiniShardTest.java && java MiniShardTest` (JDK 21)
> 结果: **16/16 PASS** (首跑 15/16, 修复 1 处测试断言缺陷后全绿)

## 验证矩阵

| # | 机制 | 验证点 | 源码对照 | 结果 |
|:--:|---|---|---|:--:|
| A1-A2 | 状态机 | CREATED→RECOVERING→POST_RECOVERY→STARTED | IndexShardState.java:10-17 | PASS |
| A3 | CLOSED | 关闭后写被拒 | IndexShard.java:1672-1690 + Permits L136-140 | PASS |
| B1-B2 | 正常并发 | MAX_VALUE 信号量写几乎不互斥 | IndexShardOperationPermits.java:49-50,259-260 | PASS |
| B3-B4 | 阻塞全占 | block 期间 tryAcquire 失败 | IndexShardOperationPermits.java:82-153 | PASS |
| B5 | 释放恢复 | block 释放后写恢复 | Permits.java:149 (release TOTAL) | PASS |
| B6 | 排空等待 | block 等待在途操作完成 (≥10ms) | Permits.java:145 (tryAcquire 全占) | PASS |
| C1-C2 | 主升 | term 0→1 + 阻塞窗口 | IndexShard.java:576 (term 断言) | PASS |
| C3-C5 | 旧主拒写 | term 不匹配拒绝 (fencing) | InternalEngine.java:1358-1367 | PASS |
| C6-C7 | 并发升主 | 升主期间写最终一致 | IndexShard.java:609 (resync CAS) | PASS |

## harness 抓到的自身缺陷 (1 处)

1. **B3 断言缺陷**: 初版测试期望"阻塞期间 doWrite 抛异常" — 实际 Semaphore.tryAcquire 无 permit 时**返回 false 而非抛异常** (真实 ES 行为) → 修正断言为检查返回值。这是 harness 抓"测试自身对机制理解错误"的实例

## 验证意义

- 5 态状态机 / permits 双模式 (并发+全占) / 主升四步 (term+block+旧主拒写) — 3 大机制全部可复现
- **未验证面**: ReplicationGroup 派生逻辑 (需路由表), StoreRecovery 完整编排, resync 数据对齐 (需双分片), 真实 updateShardState 集群驱动
- 结论: "状态迁移 + 操作门控 + 角色切换" 的分片生命周期理解验证到位
