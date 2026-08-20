# 闭环笔记 q1: 定时调度面 — 6 线程池 + 动态延迟自调度

## 假设
TC 用多线程池分区调度; 关键任务带分布式锁防集群重复执行。

## 验证过程
- **6 ScheduledThreadPool** (DefaultCoordinator:182-198): retryRollbacking / retryCommitting / asyncCommitting / timeoutCheck / undoLogDelete + **syncProcessing** — 全部 **1 线程** (NamedThreadFactory)
- **5 fixedRate 任务** (init L758-794): scheduleAtFixedRate(0 初始延迟) — retryRollbacking (ROLLBACKING_RETRY_PERIOD) / retryCommitting (COMMITTING) / asyncCommitting (ASYNC) / timeoutCheck (TIMEOUT) / undoLogDelete (UNDO_LOG_DELAY_DELETE=3min 初始, UNDO_LOG_DELETE_PERIOD=24h) — **全部包 SessionHolder.distributedLockAndExecute** (L429 — RAF/集群防重复执行)
- **3 动态延迟自调度** (syncProcessing.schedule 递归): rollbackingSchedule / committingSchedule / endSchedule (L625-753):
  - 无会话 → **schedule(RETRY_DEAD_THRESHOLD=70s)** (L585-586)
  - 有会话 → 按 beginTime 排序 → **timeToDeadSession <= 0 → 立即处理; else delay = max(time, period)** (L592-598) — **最早到期会话驱动调度频率**
  - **分布式锁未获 → 重新 schedule(period)** (L630-632)
- **branchRemoveExecutor** (L231-246): **cores×2 / queue 5000 / CallerRunsPolicy** — 仅 enableBranchAsyncRemove && mode != FILE; BranchRemoveTask (parallelStream 全删或单删, L857-928)
- **destroy 三步** (L816-845): 定时任务 shutdown + awaitTermination(5s) → Netty destroy → **SessionHolder.destroy** — 幂等收尾 (instance=null)

## 代码类型
Architecture (定时调度)

## 跨域关联
- S-7: retry 线程池消费 queueToRetry* 状态 (S-1 交叉: 状态即指令)
- S-8: SessionHolder + distributedLockAndExecute (会话存储面)
- S-11: undoLogDelete 广播 (RM 清理触发)

## 结论
调度 = 6 线程池分区 (5 fixedRate + 1 动态延迟) + 分布式锁防重 + 动态延迟由最早到期会话驱动。
源码位置: DefaultCoordinator.java:182-246,625-794; SessionHolder.java:429
