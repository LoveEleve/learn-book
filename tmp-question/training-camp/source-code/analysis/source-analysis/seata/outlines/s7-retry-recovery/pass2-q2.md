# 闭环笔记 q2: 重试链 — queueToRetry → 定时消费 → 终态

## 假设
重试是完整闭环: 失败入队 (状态) → 定时消费 → 重试 → 成功/超时终态。

## 验证过程
- **链起点** (S-1 实证): 非重试失败 → **queueToRetryCommit/Rollback** (DefaultCore:347-349,473-475) — 状态即指令 (CommitRetrying/RollbackRetrying)
- **链中段** (S-3 实证): retryCommitting/retryRollbacking 定时线程 → 筛选状态组 → **core.doGlobalCommit/Rollback(retrying=true)** — retrying 参数语义: 跳过 canBeCommittedAsync/STOP_RETRY 处理不同 (S-1:297-299,315-317)
- **链成功端**: 分支 Committed/Rollbacked → removeBranch → 全部分支完成 → **endCommitted/endRollbacked** (S-3:390-396,504-507)
- **链超时端**: isRetryTimeout → **endCommitFailed/endRollbackFailed (isRetryTimeout=true 分支)** (S-3:498-501,603-608)
- **链终态**: endRollbackFailed 三态 (q1) + **globalSession.end()** (SessionHelper:263) — end → isTwoPhaseSuccess ? clean(解锁)+onSuccessEnd : onFailEnd (GlobalSession:263-276, S-1 交叉)
- **AsyncWorker 侧** (S-4 交叉): AsyncCommitting 异步提交 → 分支完成 → 批删 undo (1000 分片)

## 代码类型
Architecture (重试闭环)

## 跨域关联
- S-1: 入口 + retrying 参数
- S-3: 定时消费 (本域是汇总面)
- S-4: AsyncWorker (异步提交面)

## 结论
重试链 = 状态入队 → 定时消费 → 成功/超时终态 → end 清理 (解锁); retrying 参数贯穿。
源码位置: DefaultCore.java:347-349,473-475; DefaultCoordinator.java:451-543; SessionHelper.java:145-263
