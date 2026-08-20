# 闭环笔记 q4: 终态助手与解锁 — end* 迁移 + 延迟处理

## 假设
终态处理有确定状态迁移; 解锁默认不自动 (人工介入面)。

## 验证过程
- **endRollbackFailed 三态** (SessionHelper:249-263): isRetryTimeout → **RollbackRetryTimeout(17)** / isTimeoutRollbacking → **TimeoutRollbackFailed(14)** / else → **RollbackFailed(12)**; 然后 **end()** + Metrics; 日志 "need to be handled it manually" (L257-260) — **人工介入面**
- **endCommitFailed** (L175-208): 类似三态 (CommitRetryTimeout/CommitFailed)
- **endCommitted** (L145-173): **DELAY_HANDLE_SESSION 分支** — retryGlobal || !DELAY → 立即 (Committed + end); else → 延迟 (setStatus + Saga end) — **DB/REDIS 模式延迟清理**
- **DELAY_HANDLE_SESSION** (L76): `!(FILE || RAFT)` — **DB/REDIS 持久化模式延迟** (防锁/分支残留, S-1 endRollbacked 延迟注释根源)
- **end() 链** (GlobalSession:263-276, S-1 交叉): **isTwoPhaseSuccess → clean (解锁) + onSuccessEnd** / else onFailEnd — 终态即解锁点
- **解锁开关** (DefaultValues:530-532): **ROLLBACK_FAILED_UNLOCK_ENABLE 默认 false** + ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE (DefaultCoordinator:463-465, S-3) — **默认重试超时不解锁** (防误解锁, 人工校准)
- **unlockBranch** (SessionHelper:440,461): end 时分支级解锁 (S-12 交叉)

## 代码类型
Implementation (终态处理)

## 跨域关联
- S-1: clean=解锁 (S-12) / end() 链
- S-3: isRetryTimeout 调用 (终态触发)
- S-12: 解锁体系 (本域是解锁时机面)

## 结论
终态 = 三态迁移 (RetryTimeout/Timeout 系/普通失败) + end() 解锁; DB/REDIS 延迟清理; 默认不解锁 (人工)。
源码位置: SessionHelper.java:76,145-263; DefaultValues.java:530-532; GlobalSession.java:263-276
