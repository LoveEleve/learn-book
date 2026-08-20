# S-3 TC Server — 调度核心与重试面

> 前置: [[S-1-AT两阶段]] (core 消费面) + [[S-2-undo_log]] (RM 面) | 引出: [[S-7-重试故障恢复]] + [[S-8-Session存储]] | 对照: RocketMQ Broker 调度面 (阶段4.1)
> 🔴 A | 8 KP | [模式: 消息分发 + 分区定时调度 + 状态组驱动]
> Pass 2 闭环: q1(调度面) q2(状态组重试) q3(消息分发) q4(会话管理)

**读者处境**: TC (Transaction Coordinator) 怎么接收请求? 失败事务怎么被重试? 这篇拆 DefaultCoordinator (929): 6 线程池 + 5 状态组 + 限流分发 + 分布式锁防重。

### 1. 定时调度面 — 6 线程池 + 动态延迟自调度

场景: TC 怎么组织后台任务?
源码路径:
- **6 ScheduledThreadPool** (DefaultCoordinator:182-198): retryRollbacking / retryCommitting / asyncCommitting / timeoutCheck / undoLogDelete + **syncProcessing** — 全部 **1 线程**
- **5 fixedRate** (init L758-794): 0 初始延迟 — **全部包 SessionHolder.distributedLockAndExecute** (SessionHolder:429 — RAF/集群防重复执行); undoLogDelete 初始延迟 3min (L151) + 周期 24h
- **3 动态延迟自调度** (L625-753): rollbackingSchedule/committingSchedule/endSchedule — **无会话 → 70s; 有 → delay = max(timeToDeadSession, period)** — **最早到期会话驱动调度频率**; 未获分布式锁 → 重排 (L630-632)
- **branchRemoveExecutor** (L231-246): **cores×2 / queue 5000 / CallerRuns** — 仅 enableBranchAsyncRemove && mode!=FILE; BranchRemoveTask parallelStream (L857-928)
- **destroy 三步** (L816-845): 定时任务 (await 5s) → Netty → SessionHolder — 幂等 (instance=null)
关键设计 (q1): **分区线程池 + 动态延迟自调度 + 分布式锁防重**。[模式: 分区调度]

### 2. 状态组与重试语义 — 5 组筛选 + 重试边界

场景: 哪些会话被重试?
源码路径:
- **5 个状态组数组** (L200-210): **retryRollbacking 3** (TimeoutRollbacking/TimeoutRollbackRetrying/RollbackRetrying) / retryCommitting 1 (CommitRetrying) / rollbacking 1 / committing 1 / **end 4** (Rollbacked/TimeoutRollbacked/Committed/Finished) — 执行计划 "5 种状态组" = **5 组筛选条件**
- **handleRetryRollbacking** (L451-482): isRetryTimeout → **unlock (ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE 任一) + endRollbackFailed** / else doGlobalRollback(retrying)
- **handleRetryCommitting** (L487-519) + **handleAsyncCommitting** (L524-543): doGlobalCommit(retrying=true) — 异步提交消费面; Committed+分支空 → endCommitted 特判 (L506-509)
- ⚠ **isRetryTimeout 数学** (L572-574): `timeout >= 0 && now - beginTime > timeout` — **MAX_COMMIT/ROLLBACK_RETRY_TIMEOUT 默认 -1L** (DefaultValues:520-527) → **永不超时 → 默认永远重试** (靠 RETRY_DEAD_THRESHOLD 70s 终止)
- **timeoutCheck** (L406-446): Begin 态扫描 → **Begin && isTimeout → close + TimeoutRollbacking** (先关会话再迁移, 与 commit 同序) — 服务端超时入口
关键设计 (q2): **状态组驱动筛选 + 默认永远重试 (MAX=-1) + dead 阈值兜底**。[模式: 状态组驱动]

### 3. 消息分发 — onRequest + 限流装饰器

场景: TM/RM 请求怎么进 TC?
源码路径:
- **onRequest** (L797-806): AbstractTransactionRequestToTC → **setTCInboundHandler(this)** → **LimitRequestDecorator (RateLimiterHandler 限流)** → handle; ⚠ **限流实现 = TokenBucketLimiter SPI** (每秒/最大/初始令牌 3 参数, 默认关); **异常模板**: exceptionHandleTemplate + StoreException→FailedStore (AbstractTCInboundHandler:67-69)
- **doXxx 8 方法** (L321-401): doGlobalBegin/Commit/Rollback/Status/Report/BranchRegister/BranchReport/LockCheck — 全部 MDC.put(xid); branchRegister 返回 branchId; lockCheck 返回 lockable
- **getInstance** (L249-261): **SessionMode.RAFT → RaftCoordinator** 多态 (S-8)
- **onResponse** (L809-813): 仅类型校验
关键设计 (q3): **统一入口 + 限流装饰器 + 存储模式驱动实现选择**。[模式: 分发+装饰]

### 4. 会话管理面 — SessionHolder + 分布式锁

场景: TC 怎么管会话?
源码路径:
- **SessionHolder**: findGlobalSessions(SessionCondition, lazyLoadBranch) + lockAndExecute (S-1) + **distributedLockAndExecute(key)** (L429); ⚠ **实现**: DistributedLockerFactory 按 sessionMode + **DistributedLockDO(lockKey, ip:port, expire)** (L400-420) — S-8 交叉; 终态双路径: Committed/Finished→endCommitted, Rollbacked/TimeoutRollbacked→endRollbacked (SessionHelper:471-478)
- **防重语义**: 获锁执行 / **未获锁 → 重排 (L630-632)** — 多节点只一个执行
- **undoLogDelete** (L548-570): **ChannelManager.getRmChannels() → 逐 RM 广播 UndoLogDeleteRequest (saveDays)** — 无渠道跳过; ⚠ **终态延迟清理**: end 态会话延迟到 isRetryTimeout 后才 processEndState (L725-727)
- **timeToDeadSession** (GlobalSession:222-228): end 态 10s / 其他 70s — 动态延迟依据 (S-1 交叉)

## 代码类型
Architecture (协调器)

## 负面空间 — TC 刻意不做的事

- **不做业务执行**: TC 只协调状态 — 业务在 TM/RM
- **不做跨节点事务调度**: 每 TC 独立会话存储 (DB/FILE/REDIS 共享面 S-8) — RAFT 才集群协调
- **不做实时推重试**: 全部周期轮询 (1s~24h) — 无事件驱动 (状态即指令但轮询消费)
- **不做优先队列**: 全部按 beginTime 排序 — 无事务优先级
- **不做背压**: 限流装饰器可配但默认关 (RateLimiterHandler)
- **不做无限重试保证**: MAX=-1 语义 + dead 阈值 — 最终放弃 (CommitFailed/RollbackFailed)

→ 引出: 重试面深化 → [[S-7-重试故障恢复]]
