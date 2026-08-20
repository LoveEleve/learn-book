# S-3 TC Server — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: DefaultCoordinator (AbstractTCInboundHandler + TransactionMessageHandler) + core 委托; retry 线程池初版 |
| 1.x | **超时族**: timeoutCheck 线程池 + TimeoutRollbacking 状态; retryRollbackingStatuses 扩展至 3 态 |
| 2.x | **动态延迟自调度**: rollbacking/committing/end Schedule (timeToDeadSession 驱动) + syncProcessing 线程池; distributedLockAndExecute (多节点防重); branchRemoveExecutor 异步删分支; RaftCoordinator 多态 (RAFT 模式) |
| 2.5.0 | LimitRequestDecorator 限流装饰器; MAX_COMMIT/ROLLBACK_RETRY_TIMEOUT 配置 (-1 永不超时默认) |

## 痕迹证据

- DefaultCoordinator.java:153: "ALWAYS_RETRY_BOUNDARY = 0" — isRetryTimeout 边界 (2.x 锚)
- DefaultCoordinator.java:253-256: SessionMode.RAFT → RaftCoordinator — 存储模式驱动 (2.x 锚)
- DefaultCoordinator.java:469: "The function of this 'return' is 'continue'" — SessionHelper.forEach 语义 (1.x 锚)
- DefaultCoordinator.java:817-818: "1. first shutdown timed task" — destroy 三步 (1.x 锚)
- DefaultCoordinator.java:234: "create branchRemoveExecutor" + enableBranchAsyncRemove (2.x 锚)
- DefaultValues.java:520-527: MAX_COMMIT/ROLLBACK_RETRY_TIMEOUT = -1L (2.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Seata 前身 Fescar 起 (公知版本线) (标注)
- "2.x 动态延迟/分布式锁/RAFT" — 特性年代推断 (标注)
- "2.5.0 限流" — LimitRequestDecorator 存在性 (标注)
- git 多 commit 可考古 — 本域以代码内注释锚为主

## 对照线 (阶段 4.1 已交付)

- RocketMQ Broker: 定时消息/延迟队列 vs Seata 周期轮询重试 — 调度对照
- ZK (4.3): QuorumPeer 主循环 vs Seata TC 定时面 — 状态机驱动对照
