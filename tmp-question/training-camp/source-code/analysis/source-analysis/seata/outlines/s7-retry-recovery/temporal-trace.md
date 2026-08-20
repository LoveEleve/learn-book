# S-7 重试故障恢复 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: queueToRetryCommit/Rollback + retry 线程池 + 终态助手 (endRollbackFailed/endCommitFailed) + RETRY_DEAD_THRESHOLD |
| 1.x | Timeout 族扩展 (TimeoutRollbacking/Retrying/Failed); isRetryTimeout + MAX_RETRY_TIMEOUT 配置 |
| 2.x | **动态延迟自调度** (timeToDeadSession 驱动); DELAY_HANDLE_SESSION (DB/REDIS); ROLLBACK_FAILED_UNLOCK_ENABLE 配置面; SessionStatusValidator |
| 2.5.0 | MAX=-1 默认永不超时; RollbackRetryTimeout/CommitRetryTimeout 终态族 |

## 痕迹证据

- SessionHelper.java:76: DELAY_HANDLE_SESSION 存储模式驱动 (2.x 锚)
- SessionHelper.java:152: "TODO: If the globalSession status in the database is Committed, don't set status again" (1.x 锚)
- SessionHelper.java:257-260: "need to be handled it manually" — 人工介入 (1.x 锚)
- DefaultValues.java:383-390: RETRY_DEAD_THRESHOLD 70s/END 10s (1.x 锚)
- DefaultValues.java:520-532: MAX=-1 + ROLLBACK_FAILED_UNLOCK_ENABLE=false (2.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x 动态延迟/DELAY_HANDLE" — 配置存在性推断 (标注)
- "2.5.0 MAX=-1" — DefaultValues 实证 (实证)
- git 多 commit 可考古 — 本域以注释锚 + 配置键为主

## 对照线 (阶段 4.1 已交付)

- RocketMQ: 延迟消息级别 (1s/5s/10s/30s/1m...) vs Seata 固定 1s 周期 — 无退避
- Kafka (4.2): 重试幂等面 vs Seata retrying 参数语义
