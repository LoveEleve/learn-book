# S-1 AT 两阶段提交 — 模板编排与 TC 协调

> 前置: 无 (总纲) | 引出: [[S-2-undo_log]] (Phase1 数据面) + [[S-3-TC-Server]] + [[S-4-DataSource代理]] + [[S-5-事务传播]] + [[S-6-TransactionHook]] | 对照: Spring 事务管理器 (7 传播) + RocketMQ RM-12 (消息事务)
> 🔴 A | 8 KP | [模式: 模板方法 + 角色分离 + 状态机]
> Pass 2 闭环: q1(模板编排) q2(生命周期) q3(TC Phase2) q4(状态面)

**读者处境**: 一个 @GlobalTransactional 从进入方法到全局提交/回滚, 中间经历了什么? 谁负责 begin/commit/rollback? 这篇拆 TransactionalTemplate (客户端编排) + DefaultGlobalTransaction (生命周期) + DefaultCore (TC 协调核心)。

### 1. 模板编排 — 传播决策 + 角色分离

场景: 全局事务怎么编排?
源码路径:
- **execute** (TransactionalTemplate:53-153): **6 种 Propagation 决策** (L66-113: NOT_SUPPORTED/REQUIRES_NEW/SUPPORTS/REQUIRED/NEVER/MANDATORY — **无 NESTED**, 对照 Spring 7 种) → 挂起返回 SuspendedResourcesHolder, 外层 finally resume (L147-152)
- **角色分离** (Launcher vs Participant): begin/commit/rollback 全 **Launcher 才执行** (L118-120,205-210,261-266) — Participant join 只记日志 (L119)
- **beginTransaction** (L304-319): triggerBeforeBegin → tx.begin → triggerAfterBegin
- **异常映射** (L191-201): **rollbackOn(exception) ? rollback : commit** — 非回滚异常提交 (Spring 事务语义)
- **状态码映射** (L225-301): commit 后 Finished→CommitFailure; rollback 后 **#fix #5231** 四族映射 (RollbackFailed/Retrying/Timeout 族)
- **钩子 7 个** (L321-391): beforeBegin/afterBegin/beforeCommit/afterCommit/beforeRollback/afterRollback + **afterCompletion (仅 Launcher)**; 钩子异常只 log 不中断
- **清理面** (L141-146): resumeGlobalLockConfig + afterCompletion + **TransactionHookManager.clear (仅 Launcher)** (L393-402)
关键设计 (q1): **模板方法编排 + 角色分离**; 非回滚异常提交语义; 钩子异常不中断。[模式: 模板方法]

### 2. 生命周期 — begin/commit/rollback + TM 重试

场景: 全局事务对象怎么运作?
源码路径:
- **对象** (DefaultGlobalTransaction:69-85): Launcher/UnKnown 默认; getCurrent 构造 **Participant** (GlobalTransactionContext:45-51); **reload 禁止 begin** (L73-80)
- **begin** (L98-119): createTime=now (超时基准) → **RootContext 已有 xid → IllegalStateException** → transactionManager.begin → status=Begin → **RootContext.bind**
- **commit/rollback** (L123-159+): Participant 忽略 → **COMMIT_RETRY_COUNT/ROLLBACK_RETRY_COUNT 重试循环** (client.tm.*.retry.count) → finally **suspend(true) 解绑** (xid 匹配才 unbind, L154-156); ⚠ **MDC xid 残留面**: begin 时 MDC.put (DefaultCore:226), commit/rollback 路径未见 remove
- **TM 协议** (DefaultTransactionManager): GlobalBeginRequest (name+timeout) → **syncCall** → xid; Failed → TmTransactionException(BeginFailed)
- **默认值**: DEFAULT_GLOBAL_TX_TIMEOUT=**60000ms** (L41) + name="default" (L43)
- **XID 格式** (common/XID:55-62): **IP:PORT:transactionId** — TC 地址编码 (路由面 XIDLoadBalance)
关键设计 (q2): **线程绑定 + TM 重试**; reload 语义 (只查状态不 begin)。[模式: 生命周期]

### 3. TC Phase2 — 分支遍历方向与提交路径

场景: TC 怎么协调 Phase2?
源码路径:
- **begin** (DefaultCore:222-234): GlobalSession.createGlobalSession → session.begin (GlobalSession:237-245: status=Begin + beginTime + onBegin 持久化)
- **commit** (L237-281): null → **Finished**; **isTimeout → TimeoutRollbacking** (TC 侧超时) → **lockAndExecute** (L249-265): status==Begin → **session.close() (先关会话 — 不能再注册分支, Highlight 注释)** → **canBeCommittedAsync ? asyncCommit : Committing** → doGlobalCommit
- **doGlobalCommit** (L284-397): **getSortedBranches 正向遍历** → 每分支: 非重试跳过 canBeCommittedAsync / **PhaseOne_Failed → removeBranch** / **XA RDONLY → removeBranch** (Oracle 只读优化) → **getCore(branchType).branchCommit 多态** → Committed → removeBranch / **Unretryable → endCommitFailed 终态** / 其他 → **queueToRetryCommit**; **PARALLEL_HANDLE_BRANCH 默认 false** (L61-62) — **并行条件: 配置启用 && branches ≥ 2** (L378); ⚠ **并行分组语义**: 按 resourceId 分组 (SessionHelper:353-385) — **同资源串行 (防同库锁竞争), 跨资源并行**; **状态即指令**: asyncCommit/queueToRetry* 全是 changeGlobalStatus (Stop 态守卫 + Timeout 派生, GlobalSession:871-893); ⚠ **isEndStatus 反直觉**: 返回 active 标志非状态枚举 (L270), timeToDeadSession 阈值由 active 驱动 (70s/10s, DefaultValues:385-390)
- **rollback** (L400-420): lockAndExecute → close → Begin→Rollbacking → doGlobalRollback
- **doGlobalRollback** (L423-509): **getReverseSortedBranches 反向遍历 (逆序回滚)** → Rollbacked → removeBranch / Unretryable → endRollbackFailed / 其他 → queueToRetryRollback; **endRollbacked 延迟** (db 模式锁/分支残留, L502-503); ⚠ **clean() = 全局锁释放** (GlobalSession:301-309, releaseGlobalSessionLock 失败抛) — closeAndClean 仅 AT 分支 (S-12 交叉)
- **doBranchDelete** (L148-219): **AT 用 commit 语义删** (branchDelete→Committed = 分支已提交无需删 → 返回 true, 日志 "Delete AT branch failed" 反直觉) / TCC/XA 用 rollback / **XAER_NOTA retry timeout** (beginTime+timeout+max(RETRY_XAER_NOTA_TIMEOUT, timeout)) / Unretryable 停重试
关键设计 (q3): **正向 commit / 反向 rollback 遍历 + 状态原子迁移 (lockAndExecute) + 失败转异步重试**。[模式: 两阶段协调]

### 4. 状态面 — GlobalStatus 21 态 + 超时判定

场景: 全局事务有哪些状态?
源码路径:
- **GlobalStatus 21 态** (core/model/GlobalStatus:29-149, code 0-20): UnKnown/Begin/Committing/CommitRetrying/Rollbacking/RollbackRetrying/**Timeout 四态 (6/7/13/14)/AsyncCommitting(8)**/Committed/CommitFailed/Rollbacked/RollbackFailed/Finished(15)/RetryTimeout (16/17)/Deleting(18)/Stop* (19/20)
- **状态组判定** (L200-245): **isOnePhaseTimeout** 4 态 / **isTwoPhaseSuccess** 4 态 / **isTwoPhaseHeuristic = Finished** (仅 1 态)
- **BranchType 5 值** (BranchType:29-48): **AT/TCC/SAGA/XA/SAGA_ANNOTATION** — 执行计划未提 SAGA_ANNOTATION
- **超时双面**: 客户端 commit 前 (TransactionalTemplate:211-220, 本地时钟) + TC commit 前 (DefaultCore:243-246, 服务端时钟)
- **TransactionalExecutor.Code** (L225-251): 客户端把 TC 状态映射为 BeginFailure/CommitFailure/RollbackFailure/Rollbacking/RollbackDone/Unknown

## 代码类型
Architecture (分布式事务框架)

## 负面空间 — Seata AT 刻意不做的事

- **不做服务端业务判定**: rollbackOn 由客户端模板判定 — TC 无业务语义
- **不做全局单事务日志**: 事务状态在 Session 存储 (S-8), 业务数据在 RM 本地 (undo_log)
- **不做 XA 两阶段标准**: AT 是"补偿式两阶段" — 锁在 RM 侧 (S-12), 非 XA 资源管理器
- **不做同步强一致**: Phase1 提交本地事务, Phase2 补偿 — **最终一致 + 窗口暴露**
- **不做无锁回滚**: 反向遍历仍需全局锁 (S-12) — 非 MVCC 隔离
- **不做事务超时全局时钟**: 客户端/服务端双时钟判定, 无统一时钟

→ 引出: Phase1 数据面 (undo_log) → [[S-2-undo_log]]
