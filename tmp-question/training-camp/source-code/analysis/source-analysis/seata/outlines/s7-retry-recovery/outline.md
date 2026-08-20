# S-7 重试故障恢复 — 重试链与死阈值

> 前置: [[S-1-AT两阶段]] (入口迁移) + [[S-3-TC-Server]] (定时消费) + [[S-2-undo_log]] (Unretriable 面) | 对照: RocketMQ 延迟重试 (阶段4.1)
> 🔴 A | 8 KP | [模式: 状态驱动重试闭环 + 双阈值]
> Pass 2 闭环: q1(状态族) q2(重试链) q3(死阈值) q4(终态解锁)

**读者处境**: 事务失败后 Seata 怎么自动恢复? 重试多久才放弃? 这篇拆重试状态族 + 完整链 + 死阈值数学 + 终态解锁。

### 1. 重试状态族 — 三系 14 态完整性

场景: 重试有哪些状态?
源码路径:
- **Commit 系 6 态** (GlobalStatus:41-137): Committing/CommitRetrying/AsyncCommitting/Committed/CommitFailed/CommitRetryTimeout
- **Rollback 系 5 态**: Rollbacking/RollbackRetrying/Rollbacked/RollbackFailed/RollbackRetryTimeout
- **Timeout 系 4 态**: TimeoutRollbacking/TimeoutRollbackRetrying/TimeoutRollbacked/TimeoutRollbackFailed
- **入口迁移** (GlobalSession:871-893, S-1): queueToRetryCommit → CommitRetrying; queueToRetryRollback → Timeout 派生/普通; Stop 态守卫
- **校验助手** (SessionStatusValidator): **4 方法族**: isTimeoutGlobalStatus 4 态 / isTimeoutRollbacking 2 态 / **isRollbackGlobalStatus 5 态 (含终态族)** / **isEndGlobalStatus 4 态** (与 S-3 endStatuses 同)
关键设计 (q1): **三系 14 态 + 入口/终态迁移方向确定**。[模式: 状态族]

### 2. 重试链 — queueToRetry → 定时消费 → 终态

场景: 重试怎么闭环?
源码路径:
- **起点** (S-1): 非重试失败 → queueToRetryCommit/Rollback (状态即指令)
- **中段** (S-3): retry 线程池 → 状态组筛选 → **core.doGlobalCommit/Rollback(retrying=true)** — retrying 参数: 跳过 canBeCommittedAsync/STOP_RETRY
- **成功端**: 分支移除 → 全完成 → **endCommitted/endRollbacked** (DefaultCoordinator:390-396,504-507); Committed+分支空 → endCommitted 特判 (L506-509)
- **超时端**: isRetryTimeout → endCommitFailed/endRollbackFailed (S-3:498-501,603-608)
- **AsyncWorker 侧** (S-4): AsyncCommitting 异步提交 → 批删 undo (1000 分片)
关键设计 (q2): **状态入队 → 定时消费 → 成功/超时终态** 完整闭环。[模式: 重试闭环]

### 3. 死阈值数学 — 70s/10s + 动态延迟

场景: 重试多久才停?
源码路径:
- **阈值** (DefaultValues:383-390): **RETRY_DEAD_THRESHOLD = 70s / END = 10s**
- **timeToDeadSession** (GlobalSession:222-228): active 70s / 已结束 10s — 剩余 = 阈值 - 已流逝
- **isRetryTimeout** (DefaultCoordinator:572-574): **MAX=-1 → 永不超时** — 重试终止靠 dead 阈值; ⚠ **基准 = beginTime** (L462,498): 重试窗口**从 begin 起算** (非失败时刻) — MAX 配置为正时, 长执行期压缩重试窗口
- **动态延迟** (S-3): 无会话 70s / **delay = max(剩余, period)** — 最早到期驱动
- **END 10s**: 终态会话快速清理 (防滞留)
关键设计 (q3): **双阈值 (MAX=-1 永不 + dead 70s/10s) + 动态调度**。[模式: 双阈值]

### 4. 终态助手与解锁 — end* 迁移 + 延迟处理

场景: 终态怎么落定?
源码路径:
- **endRollbackFailed 三态** (SessionHelper:249-263): isRetryTimeout → **RollbackRetryTimeout** / Timeout 系 → **TimeoutRollbackFailed** / else → **RollbackFailed**; "need to be handled it manually" (人工介入)
- **endCommitted** (L145-173): **DELAY_HANDLE_SESSION 分支** — DB/REDIS 模式延迟清理
- **DELAY_HANDLE_SESSION** (L76): `!(FILE || RAFT)` — 持久化模式延迟 (防锁/分支残留)
- **end() 链** (GlobalSession:263-276): **isTwoPhaseSuccess → clean (解锁) + onSuccessEnd** / else onFailEnd — 终态即解锁点
- **解锁开关**: **ROLLBACK_FAILED_UNLOCK_ENABLE 默认 false** (DefaultValues:530-532) — 默认重试超时不解锁 (人工校准); ⚠ **双开关 OR**: ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE \|\| ROLLBACK_FAILED_UNLOCK_ENABLE 任一开才解锁 (DefaultCoordinator:463-465); retryBranch 指标区分重试提交 (SessionHelper:148-155)
关键设计 (q4): **终态三态迁移 + end 解锁 + 默认人工介入**。[模式: 终态治理]

## 代码类型
Architecture (故障恢复)

## 负面空间 — 重试刻意不做的事

- **不做无限重试保证**: 默认永远重试 (MAX=-1) 但 dead 阈值后终态 — **最终放弃**
- **不做自动解锁**: ROLLBACK_FAILED_UNLOCK_ENABLE 默认 false — 重试超时锁保留 (人工)
- **不做业务感知重试**: 统一周期 (1s) — 无退避/无指数 (对照 RocketMQ 延迟级别)
- **不做优先级恢复**: 按 beginTime 排序 — 先失败先恢复 (FIFO)
- **不做跨节点恢复**: 单 TC 独立 (RAFT 才集群协调)
- **不做补偿通知**: 终态人工介入 — 无自动告警通道 (Metrics 除外)

→ 引出: 会话存储面 → [[S-8-Session存储]]
