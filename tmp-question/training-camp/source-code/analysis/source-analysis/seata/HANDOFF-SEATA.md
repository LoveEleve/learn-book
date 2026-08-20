# Seata 源码分析 — 超详细交接文档 V13 (阶段 4.4, 13/13 收官)

> **日期**: 2026-08-15 | Seata 2.5.0 (build/pom.xml:74 实证) | git 多 commit (989db47c 实证, 可考古)
> **入口关系**: 阶段4.4 总入口 (执行计划) | 域规划 `SEATA-PLAN.md` (13 域, 09 审计: 3 补充 + 5 待验证) | 本文 V1 为唯一入口
> **给新 AI**: 本文 13/13 全量交付完成 — 读 §零 (13/13) + §一 (13 域速查) 即可复用知识; 后续进入阶段 4.5 Curator (5 域)。 每域: Pass 0-3 + 六层深审 + 时空溯源 + harness (🔴) + 多次 REVIEW + 回填 §一 + 更新 §零/§四 + HANDOFF-STAGE3。方法论铁律见 §二。

---

## §零 状态总览 (2026-08-15, 13/13 收官)

### 完成状态表

| 域 | 目录 | 类型 | 大纲行数 | 闭环 | questions | 域行数 | REVIEW 发现 | harness |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| S-1 AT 两阶段 | outlines/s1-at-two-phase | 🔴 | 65 | 8 | 20 | 436 | 17 | 4/4 (23 断言) |
| S-2 undo_log 机制 | outlines/s2-undo-log | 🔴 | 66 | 8 | 20 | 436 | 16 | 4/4 (19 断言) |
| S-3 TC Server | outlines/s3-tc-server | 🔴 | 66 | 8 | 20 | 436 | 14 | 4/4 (16 断言) |
| S-4 DataSource 代理 | outlines/s4-datasource-proxy | 🔴 | 66 | 8 | 20 | 436 | 15 | 4/4 (15 断言) |
| S-5 事务传播 | outlines/s5-propagation | 🔴 | 66 | 8 | 20 | 436 | 13 | 4/4 (20 断言) |
| S-6 TransactionHook | outlines/s6-transaction-hook | 🔴 | 66 | 8 | 20 | 436 | 15 | 4/4 (11 断言) |
| S-7 重试故障恢复 | outlines/s7-retry-recovery | 🔴 | 66 | 8 | 20 | 436 | 14 | 4/4 (16 断言) |
| S-8 Session 存储 | outlines/s8-session-store | 🔴 | 66 | 8 | 20 | 436 | 15 | 4/4 (12 断言) |
| S-9 Spring 集成 | outlines/s9-spring-integration | 🟡 | 66 | 8 | 20 | 436 | 15 | — |
| S-10 SQL 路由 | outlines/s10-sql-router | 🟡 | 66 | 8 | 20 | 436 | 13 | — |
| S-11 undo_log 可靠性 | outlines/s11-undo-reliability | 🔴 | 66 | 8 | 20 | 436 | 14 | 4/4 (13 断言) |
| S-12 全局锁体系 | outlines/s12-global-lock | 🔴 | 66 | 8 | 20 | 436 | 14 | 4/4 (12 断言) |
| S-13 Phase2 分支通知 | outlines/s13-phase2-branch | 🔴 | 66 | 8 | 20 | 436 | 15 | 4/4 (10 断言) |

### 执行序 (已完成 1, 剩余 12)

```
✅ S-1 → S-2 → S-3 → S-4 → S-5 → S-6 → S-7 → S-8 → S-9 → S-10 → S-11 → S-12 → S-13
```

### 交付物统计

- 大纲 (outline.md) 13 域 / 13 篇 / **857 行**
- 域文件全量 **5668 行** / 闭环 104 / questions 260
- REVIEW 发现 **190 处** / harness 11/11 (A-D 4 面)

---

## §一 1 域核心知识速查 (全量固化)

### S-1 AT 两阶段提交 — 模板编排与 TC 协调

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 编排 | TransactionalTemplate.execute: **6 传播决策** (REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/SUPPORTS/NEVER/MANDATORY — 无 NESTED) → begin → 业务 → commit/rollback → finally 清理 (L53-153) |
| 角色分离 | **Launcher 才 begin/commit/rollback** (L118-120,205-210,261-266); Participant join 只记日志; reload 禁止 begin (GlobalTransactionContext:73-80) |
| 异常映射 | **rollbackOn(ex) ? rollback : commit** — 非回滚异常提交 (L191-201); 客户端 commit 前超时 → 转 rollback (L211-220) |
| 生命周期 | begin: createTime + RootContext.bind; commit/rollback: TM 重试 (client.tm.*.retry.count) + **suspend(true) 解绑** (L154-156); DEFAULT timeout=60000ms (L41) |
| TM 协议 | DefaultTransactionManager: GlobalBeginRequest/Commit/Rollback → syncCall (Netty) |
| TC Phase2 | commit: **先 session.close() 再状态迁移** (防新分支竞态, DefaultCore:252 Highlight) → lockAndExecute 原子迁移 → **getSortedBranches 正向 commit**; rollback: **getReverseSortedBranches 反向 rollback** (L431) |
| 失败转重试 | queueToRetryCommit/Rollback (非重试失败); Unretryable → endCommitFailed/RollbackFailed 终态 (L336-343,461-467) |
| 异步提交 | canBeCommittedAsync → **asyncCommit (AsyncCommitting)** 双触发点 (L254-256,270-272); 并行分支 PARALLEL_HANDLE_BRANCH 默认 false (L378) |
| doBranchDelete | **AT 用 commit 语义删** (Committed → "Delete failed" 日志但 return true) / TCC/XA 用 rollback / XAER_NOTA (beginTime+timeout+max(RETRY_XAER_NOTA_TIMEOUT, timeout)) |
| 状态面 | **GlobalStatus 21 态** (code 0-20): isOnePhaseTimeout 4 态 / isTwoPhaseSuccess 4 态 / **isTwoPhaseHeuristic 仅 Finished**; **BranchType 5 值含 SAGA_ANNOTATION** |
| 超时双面 | 客户端 commit 前 (本地时钟) + TC commit 前 (服务端时钟) → TimeoutRollbacking; XID = **IP:PORT:transactionId** (common/XID:55-62) |
| ⚠ 钩子/MDC | 7 钩子异常只 log 不中断; **MDC xid 残留面**: begin 时 put (L226), 未见 remove |

**时空溯源**: 0.9~1.x 骨架 → 1.x 超时族扩展 (Timeout 四态) + SAGA_ANNOTATION → 2.x 异步提交 (AsyncCommitting/canBeCommittedAsync) + Stop* 态 → 2.5.0 #fix #5231 (rollback 状态映射) + XAER_NOTA

**深审 (17 处, 五轮)**: 21 态数字穷举 / SAGA_ANNOTATION / 非回滚异常提交 / 钩子不中断 / suspend 解绑 / doBranchDelete 反直觉 / 并行条件 / MDC 残留 / **isEndStatus 反直觉 (active 驱动阈值)** / **RETRY_DEAD 70s/10s** / **clean=全局锁释放** / **并行按资源分组** / **状态即指令** — 推理验证 17 项全过 + harness 23/23

**负面空间**: 不服务端业务判定/不全局事务日志 (状态在 Session 存储 S-8)/不 XA 标准 (补偿式)/不同步强一致 (最终一致)/不无锁回滚 (S-12)/不全局时钟 (双时钟)

### S-2 undo_log 机制 — 镜像采集与反向 SQL

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 生命周期 | State **Normal(0)/GlobalFinished(1)**; flush (Normal 插入) → undo (行锁+校验+逆序执行) → delete / GlobalFinished 标记 (AbstractUndoLogManager:62-82,266-466) |
| ⚠ !exists 反直觉 | undo 时 undo_log 不存在 → **插 GlobalFinished 防 Phase1 后提交** (issue #489, L390-416) — 业务超时场景 |
| 无限重试边界 | **for(;;) 只对 SQLIntegrityConstraintViolationException 重试** (L419-423); dirty → Unretriable 人工校准; 其他 → Retriable |
| 逆序回滚 | sqlUndoLogs.size()>1 → **Collections.reverse** (L368-370) — 后执行先补偿 |
| 反向 SQL 三模板 | DELETE→**INSERT (beforeImage 重建, PK 最后)** / INSERT→**DELETE WHERE pk** / UPDATE→**UPDATE SET before 值 WHERE pk** (MySQL 三 executor) |
| 镜像采集 | beforeImage **SELECT ... FOR UPDATE 行锁** (SelectForUpdateExecutor:152) + afterImage 再查; **UPDATE 行数不等 → ShouldNeverHappenException (主键被改守卫)** (BaseTransactionalExecutor:408-411) |
| lockKey | **DELETE ? beforeImage : afterImage** (BaseTransactionalExecutor:415) — S-12 交叉 |
| 校验三步 | before==after 跳过 / after==current 执行 / **before==current 跳过 (重复回滚幂等)** / else **SQLUndoDirtyException → Unretriable** (AbstractUndoExecutor:234-283) |
| 压缩 | 默认 **enable=true / zip / >64k 严格大于** (DefaultValues:368-380); context 记录 compressorType + serializer + max_allowed_packet |
| 方言 | **13 方言 UndoExecutor** (mysql/oracle/pg/dm/kingbase/...) SPI; ⚠ CHECK_SQL "TODO 多主键" **注释过时** (已支持, L297-366) |

**时空溯源**: 0.9~1.x 骨架 (TODO 单主键锚) → 1.x GlobalFinished (#489) + 重复回滚防护 + 三步校验 → 2.x 压缩/子表/批删/13 方言 → 2.5.0 JSON/max_allowed_packet

**深审 (16 处, 五轮)**: GlobalFinished 反直觉 / 无限重试边界 / 逆序条件 / TODO 过时 / SERIALIZER ThreadLocal / max_allowed_packet / 幂等分支 / **jackson 默认序列化器** / **InsertExecutor 只绑 PK** / **fastjson 比较特例** / **锁重试** — 推理验证 17 项全过 + harness 19/19

**负面空间**: 不物理回滚 (逻辑补偿)/不数据备份 (镜像对)/不级联处理 (逆序缓解)/不并发写保护 (行锁+校验, dirty 人工)/不无限保留 (S-11 清理)/不跨方言 SQL 生成

### S-3 TC Server — 调度核心与重试面

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 线程池 | **6 ScheduledThreadPool 全 1 线程** (retryRollbacking/retryCommitting/asyncCommitting/timeoutCheck/undoLogDelete/syncProcessing, L182-198) + branchRemoveExecutor (cores×2/queue 5000/CallerRuns, 仅 enable && !=FILE) |
| 调度面 | 5 fixedRate (0 初始) + **3 动态延迟自调度** (rollbacking/committing/end: 无会话→70s, 有→delay = **max(timeToDeadSession, period)** 最早到期驱动) — 全部 **distributedLockAndExecute 防重** (未获锁重排 L630-632) |
| 状态组 | **5 组筛选数组** (retryRollbacking 3/retryCommitting 1/rollbacking 1/committing 1/end 4, L200-210) — 执行计划 "5 种状态组" 表述精确化 |
| ⚠ 重试边界 | **MAX_COMMIT/ROLLBACK_RETRY_TIMEOUT 默认 -1L** (DefaultValues:520-527) → isRetryTimeout 恒 false → **默认永远重试** — 靠 RETRY_DEAD_THRESHOLD 70s + timeToDeadSession 终止 |
| 超时扫描 | timeoutCheck: Begin 态 → **close + TimeoutRollbacking** (先关会话, 与 commit 同序) |
| 消息分发 | onRequest → **LimitRequestDecorator (RateLimiterHandler 限流)** → doXxx 8 方法 (MDC.put xid); **RAFT 模式 → RaftCoordinator 多态** (L253-256) |
| undo 清理 | undoLogDelete: ChannelManager.getRmChannels → 逐 RM 广播 UndoLogDeleteRequest (saveDays); 初始延迟 3min 周期 24h |
| 终态清理 | end 态会话延迟到 isRetryTimeout 后才 processEndState (L725-727) |
| 默认周期 | COMMITTING/ASYNC/ROLLBACKING/TIMEOUT=1000ms; END_STATUS=30s; UNDO_LOG_DELETE=24h (DefaultValues:465-492) |

**时空溯源**: 0.9~1.x 骨架 (retry 池+超时族) → 2.x 动态延迟自调度 + distributedLock + branchRemoveExecutor + RaftCoordinator → 2.5.0 限流装饰器 + MAX_RETRY_TIMEOUT 配置

**深审 (14 处, 五轮)**: 6 池/5 组数字穷举验证 / MAX=-1 永远重试认知修正 / 动态延迟数学 (harness 自抓) / Committed 特判 / 超时 close 顺序 / 终态延迟清理 / **TokenBucketLimiter 三参数** / **DistributedLockerFactory 按模式** / **异常模板 FailedStore** — 推理验证 17 项全过 + harness 16/16

**负面空间**: 不业务执行/不跨节点调度 (RAFT 才集群)/不实时推重试 (周期轮询 1s~24h)/不优先队列/不背压 (限流默认关)/不无限重试保证 (dead 阈值最终放弃)

### S-4 DataSource 代理 — 代理链与提交拦截

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 代理链 | DataSourceProxy (455) → ConnectionProxy (393) → Statement 代理; **嵌套解包** (L95-100); 构造期: dbType + **polardb-x 检测** + **undo_log 表 fast-fail** (L167-183) + **7 方言 resourceId** (L236-252) + registerResource |
| 提交拦截 | **doCommit 三分支** (L227-235): 全局事务 / 仅全局锁 (checkLock→commit) / 直通; ⚠ **setAutoCommit false→true 也触发 doCommit** (第二拦截点, L303-309) |
| 全局提交链 | **register → flushUndoLogs → targetConnection.commit**; 失败 report(false) + **补 rollback 防半提交** (L191-195); 成功 **IS_REPORT_SUCCESS_ENABLE 默认 false 才 report(true)** |
| register 守卫 | **!hasUndoLog \|\| !hasLockKey → 不注册** (L267-281) — 只读事务无分支; report 有 **branchId 守卫** (L312-314) |
| 锁重试 | **LockRetryPolicy**: branchRollbackOnConflict 默认 true — true && autoCommitChanged → 直通 (重试在 execute 层) / else doRetryOnLockConflict; **FailFast 降级** (L372-376) |
| 上下文 | ConnectionContext (416): xid/branchId/**undoItems/lockKeys**/savepoints/autoCommitChanged/**globalLockRequire** (@GlobalLock); **reset 提交后归零** |
| 执行链 | AbstractDMLBaseExecutor: autoCommit=true → executeAutoCommitTrue (LockRetryPolicy) / false → beforeImage→业务→afterImage→prepareUndoLog (S-2) |
| 默认值 | LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT=true / REPORT_RETRY_COUNT=5 / REPORT_SUCCESS_ENABLE=false (DefaultValues:38-60) |

**时空溯源**: 0.9~1.x 骨架 (三层代理+三分支提交+上下文) → 1.x savepoint 上下文/报告重试/全局锁本地提交 → 2.x LockRetryPolicy 重构+polardb-x+嵌套解包 → 2.5.0 report 成功默认关+7 方言

**深审 (15 处, 五轮)**: register 双条件守卫 / report 成功默认关 / autoCommit 第二拦截点 / LockRetryPolicy 双面语义 / FailFast 降级 / 失败补 rollback / branchId 守卫 / globalLockRequire / **单语句隐式全局事务** / **PK 数组识别** / **锁重试第 N+1 次超时** / **AsyncWorker 1000 分片** — 推理验证 17 项全过 + harness 15/15

**负面空间**: 不连接池/不 SQL 改写 (S-10)/不自动建表 (fast-fail 要求预建)/不读写分离/不连接缓存/不嵌套事务 (savepoint 仅记录)

### S-5 事务传播 — 6 传播语义与挂起恢复

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 传播枚举 | **6 值全实证** (Propagation:57-176): REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/SUPPORTS/NEVER/MANDATORY — **无 NESTED** (对照 Spring 7) |
| 决策矩阵 | **6×2 = 12 决策点** (TransactionalTemplate:66-113): REQUIRED: JOIN/新建; REQUIRES_NEW: 挂起+新建/新建; NOT_SUPPORTED: 挂起+裸跑/裸跑; SUPPORTS: JOIN/裸跑; NEVER: 抛/裸跑; MANDATORY: JOIN/抛 (**NEVER/MANDATORY 镜像对称**) |
| 挂起恢复 | suspend (L207-228): **先取 xid 再 unbind** (注释锚) + **clean ? null : SuspendedResourcesHolder**; resume → bind; **每层 finally resume (栈式 LIFO)** |
| ⚠ 挂起不冻结计时 | suspend 不改变 createTime — 挂起期间继续计时, 长挂起可触发客户端超时 (TransactionalTemplate:162-165) |
| 上下文 | RootContext: bind (**空 xid → 转 unbind**) + **MDC.put/remove 同步**; commit/rollback finally suspend(true) 结束解绑 |
| ContextCore SPI | **双实现**: ThreadLocalContextCore / **FastThreadLocalContextCore (Netty 场景)** (ContextCoreLoader:27-41) — 响应式可替换 |
| 配置载体 | TransactionInfo: timeOut/propagation/lockRetryInterval/Times (S-9 注解 → 本载体) |
| 嵌套效应 | REQUIRES_NEW 内 REQUIRES_NEW = 栈式两段挂起; **NOT_SUPPORTED 内 MANDATORY = 抛 (环境清空)** |

**时空溯源**: 0.9~1.x 骨架 (6 枚举 Javadoc 锚 + 挂起恢复) → 1.x RootContext 键扩展 + MDC → 2.x ContextCore SPI + FastThreadLocal → 2.5.0 TransactionInfo 扩展

**深审 (13 处, 五轮)**: 6 值数字穷举 / clean 双语义 / bind 空值防御 / ContextCore 双实现 / MDC.remove 闭环 / 结束解绑对称 / 挂起不冻结计时 / **rollbackOn=Spring RollbackRule 移植** / **响应式推断面修正** — 推理验证 17 项全过 + harness 20/20

**负面空间**: 不 NESTED (无 savepoint 内嵌)/不传播超时继承/不子线程传播 (ThreadLocal)/不异步传播 (需换 ContextCore)/不日志追踪 (仅 MDC)/不多 xid 并存

### S-6 TransactionHook — 7 生命周期钩子

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 钩子接口 | **7 方法** (TransactionHook:19-55): beforeBegin/afterBegin/beforeCommit/afterCommit/beforeRollback/afterRollback/afterCompletion — **3×2+1 分组**; 执行计划漏列 afterCommit/afterRollback |
| 管理器 | TransactionHookManager (66): **ThreadLocal 列表** + **getHooks 每次新建只读包装** (非缓存, 对照 TccHookManager CACHED) + registerHook (null→NPE) + clear (remove 防泄漏) |
| 触发编排 | TransactionalTemplate 7 点 (L223-315,321-402): begin/commit/rollback 各 before→动作→after + finally afterCompletion; **钩子异常独立 try/catch 不中断** |
| Launcher 守卫 | begin/commit/rollback trigger 由外层方法守卫; **afterCompletion 显式仅 Launcher** (L381-391); **cleanUp Launcher 才 clear** (Participant 共享外层钩子) |
| finally 顺序 | resumeGlobalLockConfig → afterCompletion → cleanUp (L141-146) — 钩子执行时锁配置已恢复 |
| 跨模式复用 | **Saga 同模板** (DefaultSagaTransactionalTemplate:125-160, 守卫粒度不同: 每 trigger 内判 Launcher); **TCC 独立体系** (TccHookManager: CopyOnWriteArrayList 全局 + 缓存视图) |
| 用途对比 | TransactionHook = 生命周期观察; TccHook = 门面拦截 (prepare/commit/rollback 前后) |

**时空溯源**: 0.9~1.x 骨架 (7 钩子+ThreadLocal) → 1.x afterCompletion 守卫 + cleanUp 时机 → 2.x Saga 复用 + TCC 独立体系 → 2.5.0 稳定

**深审 (15 处, 五轮)**: 7 钩子数字穷举 (计划漏 2 方法) / 只读包装非缓存 (harness 自抓) / 异常独立 catch / Participant 不清钩子 / finally 顺序 / Saga 守卫粒度 / **Saga 全 7 钩子修正** / **TccHook 6+ 方法** / **afterCompletion null 分支** / **无内置调用者** — 推理验证 17 项全过 + harness 11/11

**负面空间**: 不过滤器链 (广播无上下文)/不异步钩子/不钩子重试 (异常即吞)/不跨线程 (ThreadLocal)/不优先级 (注册序)/不事件总线 (对照 Spring ApplicationEvent)

### S-7 重试故障恢复 — 重试链与死阈值

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 重试状态族 | **三系 14 态**: Commit 6 (Committing/CommitRetrying/AsyncCommitting/Committed/CommitFailed/CommitRetryTimeout) + Rollback 5 + Timeout 4; **Rollbacking 不在 retryRollbacking 组** (S-3) |
| 重试链 | 失败 → **queueToRetry\* (状态即指令)** → retry 线程池定时消费 (retrying=true) → 成功: removeBranch+endCommitted / 超时: endCommitFailed; Committed+分支空特判 (DefaultCoordinator:506-509) |
| 死阈值 | **RETRY_DEAD_THRESHOLD=70s / END=10s** (DefaultValues:383-390); **MAX=-1 永不超时** (靠 dead 终止); timeToDeadSession: active 70s/已结束 10s |
| 动态延迟 | 无会话 70s; delay = **max(剩余, period)** — 最早到期驱动 (S-3) |
| 终态三态 | endRollbackFailed (SessionHelper:249-263): isRetryTimeout → **RollbackRetryTimeout(17)** / Timeout 系 → **TimeoutRollbackFailed(14)** / else → **RollbackFailed(12)**; "need to be handled it manually" |
| 解锁面 | **ROLLBACK_FAILED_UNLOCK_ENABLE 默认 false** (DefaultValues:532); **双开关 OR** 任一开才 clean (DefaultCoordinator:463-465); end() → isTwoPhaseSuccess ? clean : onFailEnd |
| DELAY_HANDLE_SESSION | `!(FILE \|\| RAFT)` (SessionHelper:76) — DB/REDIS 延迟清理 (防锁/分支残留) |
| retrying 参数 | 跳过 canBeCommittedAsync/STOP_RETRY; retryBranch 指标区分 (SessionHelper:148-155) |

**时空溯源**: 0.9~1.x 骨架 (入队+线程池+死阈值) → 1.x Timeout 族 + isRetryTimeout → 2.x 动态延迟 + DELAY_HANDLE + 解锁配置 → 2.5.0 MAX=-1 + RetryTimeout 终态族

**深审 (14 处, 五轮)**: 三系 14 态穷举 / 终态三态 / DELAY_HANDLE / 人工介入面 / Committed 特判 / retryBranch 指标 / 双解锁开关 OR / **isRetryTimeout begin 基准** / **校验器 4 方法族** / **Timeout 特判埋点** — 推理验证 17 项全过 + harness 16/16

**负面空间**: 不无限重试保证 (dead 后放弃)/不自动解锁 (默认人工)/不业务感知重试 (固定 1s 无退避, 对照 RocketMQ 延迟级别)/不优先级 (FIFO)/不跨节点恢复 (RAFT 才集群)/不补偿通知

### S-8 Session 存储 — 4 模式会话持久化

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| SessionMode | **4 值** (FILE/DB/REDIS/RAFT, SessionMode:19-35); SessionHolder.init **SPI 按模式加载** SessionManager + reload (L91-154); ROOT_SESSION_MANAGER_NAME="root.data" |
| 生命周期映射 | **writeSession 6 操作** (AbstractSessionManager:173-195, 失败→FailedWriteSession 异常族); onBegin→ADD / onStatusChange→UPDATE / onSuccessEnd→REMOVE / onClose→**setActive(false)** |
| ⚠ isEndStatus 根源 | **onClose → setActive(false)** (L154-156) — S-1 isEndStatus 反直觉 (返回 active) 的实证闭环 |
| 状态联动锁 | **Rollbacking/TimeoutRollbacking → 全部分支 LockStatus.Rollbacking** (L85-87) — 状态变更同步锁标记 (S-12) |
| onFailEnd | **rollbackFailedUnlockEnable → clean (解锁)** / else 保留 (L163-171, S-7 交叉) |
| 存储实现 | DB: **global/branch/lock 三表 + 13 方言 SQL** (LogStoreSqls/LockStoreSql) + GlobalTransactionDO 11 字段; Redis: **Lua 原子脚本** (Lua/Java 双实现); File: 序列化+FlushDiskMode+restoreSessions |
| lockAndExecute | **三模式**: FILE **GlobalSessionLock ReentrantLock tryLock(2s)** (L831-850) / **DB 直接 call** (多节点本地锁无意义) / RAFT 分布式锁 (S-3) |
| 分布式锁族 | DataBaseDistributedLocker / RedisDistributedLocker (Lua) / RaftDistributedLocker / FileLocker; GlobalSession 双锁 (globalSessionLock+resourceLock, L117-121) |

**时空溯源**: 0.9~1.x 骨架 (File root.data) → 1.x DB 模式 + onClose setActive(false) → 2.x REDIS Lua + RAFT + 分布式锁 → 2.5.0 13 方言

**深审 (15 处, 五轮)**: 4 值穷举 / isEndStatus 根源闭环 / 状态联动锁 / 三模式锁 / tryLock 2s 数学 (harness 自抓 2 处) / 异常族 / 双锁 / Redis 双实现 / **transactionName 静默截断** / **每写独立事务** / **Redis 4 脚本 pipeline** / **reload 状态机三面** — 推理验证 17 项全过 + harness 12/12

**负面空间**: 不跨模式迁移 (启动即定)/不缓存层/不分库分表/不本地锁集群化 (FILE 单机语义)/不消息队列/不数据压缩

### S-9 Spring 集成 — 注解装配与拦截链

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 注解 | @GlobalTransactional (integration-tx-api): timeoutMills (默认 60000) / rollbackFor+ClassName / noRollbackFor+ClassName (**Class/String 双形式**) / propagation 默认 REQUIRED / lockRetry×3 |
| ⚠ 超时重置语义 | <= 0 **\|\| == 60000** → 用 defaultGlobalTransactionTimeout (InterceptorHandler:208-211) — 未配置跟随全局动态默认 |
| 规则源闭环 | rollbackFor/noRollbackFor → **RollbackRule/NoRollbackRule** (L216-241) — S-5 rollbackOn 引擎的注解源 |
| 扫描器 | GlobalTransactionScanner (676): **extends AbstractAutoProxyCreator**; AT/MT 双模式; wrapIfNecessary: doCheckers (**FactoryBean 排除**) → NEED_ENHANCE 集合 → 未代理 super.wrap / **已代理 addAdvisor 按序织入** (L334-337) |
| 检查器族 | ScannerChecker 3 个 (ConfigBeans/Package/Scope); PROXYED_SET 去重 + BeanDefinition 预收集 (L541-560); ORDER_NUM=1024 与 Spring 事务共存 |
| 拦截链 | **双 handler** (L291-297 注释): handleGlobalTransaction (TM) + handleGlobalLock (GlobalLock, S-12); ExecutionException 状态化 (Participant 抛原异常/isTimeoutException/FailureHandler 钩子) |
| 自动配置 | SeataAutoConfiguration: failureHandler + globalTransactionScanner 两 bean; disable 动态开关; initClient (TM/RM) |

**时空溯源**: 0.9~1.x 骨架 (Scanner+Interceptor) → 1.x 检查器族+failureHandler → **2.x 模块化: 拦截器移入 integration-tx-api 双 handler** + AspectTransactional + Boot starter → 2.5.0 DefaultInterfaceParser

**深审 (15 处, 五轮)**: 拦截器模块化修正 / 超时重置语义 / 规则源闭环 / 状态化异常 / 检查器族 / TCC 织入面 / isTimeoutException / Order 共存 / **责任链 parser** / **相对定位重排** / **lockRetryTimes -1** / **1.5 默认组警告** — 推理验证 17 项全过 (🟡 无 harness)

**负面空间**: 不注解织入拦截器类 (FactoryBean 排除)/不方法级重试 (FailureHandler 兜底)/不表达式 pointcut (全扫描+检查器)/不事务名自动生成/不 @Transactional 冲突处理 (排序共存)/不多数据源路由

### S-10 SQL 路由 — 识别与 executor 族

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 路由核心 | ExecuteTemplate (186): **守卫直通** (!requireGlobalLock && AT != branchType → 原 statement, L69-73) → 识别 → **6 类型分支 + Multi + Plain** (L100-168) |
| 路由表 | INSERT → **InsertExecutor SPI 方言加载** / UPDATE/DELETE → SqlServer 变体 vs 通用 / SELECT_FOR_UPDATE (S-2 行锁, SqlServer 变体) / **INSERT_ON_DUPLICATE_UPDATE + UPDATE_JOIN → 仅 MySQL/Mariadb/PolarDBX** (其他 NotSupportYet) / default Plain |
| Multi | 多识别器 → MultiExecutor (MULTI_UPDATE/MULTI_DELETE; ⚠ sqlserver 变体 L81-88) |
| executor 族 | Executor → **AbstractDMLBaseExecutor** (S-4) → Insert/Update/Delete/SelectForUpdate; **10 方言目录** (mysql/oracle/pg/sqlserver/dm/kingbase/mariadb/oceanbase/oscar/polardbx) |
| SQL 识别 | SQLVisitorFactory → **SQLRecognizerFactory SPI** (sqlParserType 配置); **sqlparser 三模块** (core/druid/antlr 双解析器) |
| SQLType | **48 值** (0-44 + 101/102/103): SELECT 族 6+ 变体 / MULTI_DELETE(35)/MULTI_UPDATE(36) / **INSERT_ON_DUPLICATE_UPDATE(102)/UPDATE_JOIN(103)** |
| 边界面 | ⚠ **SELECT 普通查询也 Plain** (只读无镜像 — 镜像只对 DML); Plain 兜底 (未识别原样); 异常统一 SQLException; NotSupportYet 显式 |

**时空溯源**: 0.9~1.x 骨架 (路由+基础 SQLType+druid) → 1.x ON_DUPLICATE/UPDATE_JOIN 方言扩展 → 2.x sqlparser 三模块 + MultiExecutor → 2.5.0 48 值 + 10 方言

**深审 (13 处, 五轮)**: 6+Multi+Plain 表述精确化 / 2 类型仅 3 方言 / 守卫直通 / Insert SPI / 三模块 / Multi 方言变体 / SqlServer SelectForUpdate / SELECT 也 Plain / **三入口无旁路** / **Multi 仅两类型** — 推理验证 17 项全过 (🟡 无 harness)

**负面空间**: 不全部 SQLType 支持 (48 值仅消费 8)/不 SQL 改写 (镜像不改 SQL)/不解析缓存 (每次解析)/不方言全支持/不 DDL 拦截 (Plain 直通)/不 SQL 归一化

### S-11 undo_log 可靠性 — 压缩/子表/批删/清理

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 压缩面 | **needCompress: enable (默认 true) && >64k 严格大于** (AbstractUndoLogManager:571-573); context 记录 COMPRESSOR_TYPE_KEY → 解压按记录恢复; CompressorFactory SPI (默认 zip) |
| 子表面 | **limit = maxAllowedPacket × 0.8** (MySQLUndoLogManager:136-143; 默认 1MB mysql5.6); 超限 → **首片主行 (branchId)** + **UUID 子片子行** (SUB_SPLIT_KEY ",") + context SUB_ID_KEY; ⚠ **片数 = ceil((len-limit)/limit)** (harness 自抓 2 处) |
| 子表读取 | SELECT branch_id IN (...) AND xid (L83-120) → **主+子字节拼接** → 解压; 子行 BRANCH_ID_KEY 关联键 |
| AsyncWorker | **LinkedBlockingQueue(10000)** (DefaultValues:48) + 2 线程 1s; branchCommit 入队**立即返回 Committed**; **满时背压: 紧急清+重入** (L88-97); drainTo → **按 resourceId 分组** → **Lists.partition(1000)** → batchDeleteUndoLog → commit (autoCommit false 才) |
| requeue | 资源缺失 (L149) / SQLException (L165,189) → **addAllToCommitQueue 无限重试** — 失败不丢 |
| 清理面 | **DELETE WHERE log_created <= ? LIMIT ?** (分页防锁表, L61-66); TC undoLogDelete 广播触发 (S-3:548-570, saveDays); **GlobalFinished 并发保护** (#489); deleteUndoLogByLogCreated 基类默认 0 (方言实现) |
| ⚠ IN 对齐 | **1000 分片双语义**: 批删分批 + **MySQL IN 上限规避** |

**时空溯源**: 0.9~1.x 骨架 (压缩 + GlobalFinished #489) → 1.x 子表拆分 (80% + UUID) → 2.x AsyncWorker 异步批删 + 分片 + requeue → 2.5.0 缓冲配置 10000

**深审 (14 处, 五轮)**: 片数数学 (harness 自抓 2 处) / 满时背压 / requeue 无限重试 / maxAllowedPacket 默认 / 子行关联键 / commit 语义 / IN 上限对齐 / **8 压缩类型** / **RM 删除链 LIMIT 3000 迭代** / **saveDays 兜底 7 天** — 推理验证 17 项全过 + harness 13/13

**负面空间**: 不无限保留 (saveDays 窗口)/不异地备份/不片内压缩 (拆分后不压)/不清理限速配置/不孤儿检测 (崩溃残留无扫描)/不延迟删除确认

### S-12 全局锁体系 — 行锁存储与 Locker 族

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 锁管理器 | LockerManagerFactory (L32-52): **EnhancedServiceLoader.load(LockManager, lockMode)** — LockMode 4 值; AbstractLockManager (L38-194): acquireLock/releaseLock/**isLockable (lockQuery)**/cleanAllLocks/updateLockStatus |
| lockKey 解析 | **"table:pk1,pk2;t2:pk3" 三层格式** (L122-165): 分号表分隔/冒号表名:主键/逗号多主键 → RowLock (xid/tid/branchId/table/pk/resourceId) |
| Locker 族 | ⚠ **执行计划 "6种" 修正 → 7 实现**: **行锁 4** (DataBaseLocker/FileLocker/RedisLocker/RedisLuaLocker) + **分布式 3** (DataBase/Redis/RaftDistributedLocker) |
| 检查-插入 | LockStoreDataBaseDAO:109-160+: **rowKey 去重** → autoCommit false → **checkLock (SELECT row_key, xid WHERE row_key IN)** → **dbXID != currentXID → 冲突** (failFast) → insertLockBatch; **skipCheckLock 旁路** |
| lockQuery | **只查不锁** (isLockable — SELECT 检查不写入, harness C3) — RM 侧 checkLock 同源 (S-4) |
| 存储 | **LockDO 5 列**: rowKey/xid/transactionId/branchId/status; 13 方言 LockStoreSql (LockStoreSqlFactory) |
| LockStatus | **Locked(0)/Rollbacking(1)** (core/model) — S-8 状态联动; updateLockStatus xid 级 |
| 释放 | **分支级 + 全局级两级** (DataBaseLockManager:58-73); 终态默认不解锁 (人工, S-7) |

**时空溯源**: 0.9~1.x 骨架 (lock_table+DataBaseLocker+collectRowLocks) → 1.x LockStatus+updateLockStatus+failFast → 2.x LockerManagerFactory SPI + Redis/File/Raft 族 + 13 方言 → 2.5.0 分布式锁族稳定

**深审 (11 处)**: 7 实现数字修正 (计划 "6种") / 只查不锁语义 / lockKey 三层格式 / 检查-插入两段 / failFast / skipCheckLock 旁路 / LockDO 5 列 — 推理验证 12 项全过 + harness 12/12

**负面空间**: 不 RW 锁 (对照 ZK ReadWriteLock)/不锁粒度协商/不锁超时自动释放 (靠终态)/不公平队列 (冲突即失败)/不锁监控/不跨表原子锁

### S-13 Phase2 分支通知 — 处理器族与收束闭环

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 处理器族 | **5 实现** (RMHandlerAT/XA/TCC/Saga/**SagaAnnotation**) + **SPI loadAll 注册** (DefaultRMHandler:49-54) + **单例** (L94-100) |
| 多态分发 | getRMHandler(branchType) (L66-82) — **双面多态** (TC getCore / RM getRMHandler, BranchType 全链路贯穿) |
| 接收端 | **RmBranchCommit/RollbackProcessor** (client:42-63) → handler.onRequest; 协议 4 字段 (xid/branchId/resourceId/applicationData) |
| AT 提交 | **AsyncWorker 入队立即返回 PhaseTwo_Committed** (S-11) — 异步非阻塞; 无异步回调 (靠状态/重试 S-7) |
| AT 回滚 | **undoManager.undo** (S-2) — 镜像恢复 + 校验 + 无限重试 |
| 状态回传 | BranchStatus → TC 决策 (S-1:329-363): Committed → removeBranch / Unretryable → endCommitFailed / 其他 → queueToRetryCommit (S-7) |
| 异常模板 | exceptionHandleTemplate 双侧同构; UndoLogDeleteRequest 基类空 (issue #2226) — RMHandlerAT 覆盖 (S-11) |

**时空溯源**: 0.9~1.x 骨架 → 1.x SPI 注册+TCC/XA → 2.x AsyncWorker + SagaAnnotation + #2226 → 2.5.0 5 实现稳定

**深审 (15 处, 五轮)**: 5 实现数字穷举 / 提交异步语义 / 双面多态 / 单例 / 无异步回调 / 分支删除联动 / **资源管理器双面** / **异常码三态回填** / **回滚处理器对称** / **RMClient 两件套** — 推理验证 17 项全过 + harness 10/10

**负面空间**: 不同步两阶段/不分支级事务/不通知重试确认/不分支优先级/不批量通知/不端到端追踪

---

## §二 方法论执行报告 (S-1~S-13 实证, V13 收官)

### 1. 域级怀疑审计修正汇总 (S-1~S-13, 收官)

| 修正 | 证据 | 落点 |
|:--|:--|:--|
| **GlobalStatus 21 态**: 执行计划 S-3 "5种GlobalStatus状态组" — 实证 21 态 (code 0-20) | GlobalStatus.java:29-149 | S-1 速查 |
| **BranchType 5 值**: 执行计划未提 **SAGA_ANNOTATION** | BranchType.java:29-48 | S-1 速查 |
| **传播 6 种无 NESTED**: 对照 Spring 7 种 | TransactionalTemplate:66-113 | S-1 速查 |
| **"无限重试" 表述**: 执行计划 S-11 "for(;;)无限重试" — 实证只对约束冲突重试, dirty/其他抛分支状态 (S-7) | AbstractUndoLogManager:419-445 | S-2 速查 |
| **CHECK_SQL TODO 过时**: "TODO support multiple primary key" — 多列 PK 已完整支持 | AbstractUndoExecutor:70 vs 292-366 | S-2 速查 |
| **"6 个 ScheduledThreadPool" 验证通过**: 全 1 线程 (retryRollbacking/retryCommitting/asyncCommitting/timeoutCheck/undoLogDelete/syncProcessing) | DefaultCoordinator:182-198 | S-3 速查 |
| **"5 种状态组" 表述精确化**: 5 组**筛选条件数组** (3/1/1/1/4) 非 21 态分组; retryRollbacking 不含 Rollbacking 本身 | DefaultCoordinator:200-210 | S-3 速查 |
| **MAX_RETRY_TIMEOUT 默认 -1**: 执行计划未提 — isRetryTimeout 恒 false → 默认永远重试 (dead 70s 兜底) | DefaultValues:520-527 | S-3 速查 |
| **提交拦截点不止 commit**: setAutoCommit false→true 也触发 doCommit (JDBC 规范) | ConnectionProxy:303-309 | S-4 速查 |
| **register 双条件守卫**: 无 undo **或** 无 lockKey 均不注册分支 | ConnectionProxy:267-281 | S-4 速查 |
| **传播 6 值实证闭环**: S-1 消费面 + S-5 定义面一致 — 执行计划 "6种Propagation" 验证通过 | Propagation:57-176 | S-5 速查 |
| **挂起不冻结计时**: suspend 不改变 createTime — 长挂起触发超时 (执行计划未提) | TransactionalTemplate:162-165 | S-5 速查 |
| **7 钩子漏 2**: 执行计划列 beforeCommit/rollback 漏 afterCommit/afterRollback (总数 7 对) | TransactionHook:19-55 | S-6 速查 |
| **getHooks 非缓存**: 每次新建只读包装 (对照 TccHookManager CACHED volatile 缓存) | TransactionHookManager:42-46 | S-6 速查 |
| **"6个线程池retry+RETRY_DEAD_THRESHOLD+ROLLBACK_FAILED_UNLOCK_ENABLE" 全验证**: 6 池 (S-3) + 70s/10s + 默认 false (不解锁) | DefaultValues:383-390,530-532 | S-7 速查 |
| **三系 14 态**: 重试状态族 = Commit 6/Rollback 5/Timeout 4 — 执行计划未细分 | GlobalStatus:41-137 | S-7 速查 |
| **"SessionMode(DB/FILE/REDIS/RAFT)" 验证通过**: 4 值实证 + SessionManager 族 4 实现 | SessionMode:19-35 | S-8 速查 |
| **lockAndExecute 三模式**: 执行计划未提 — FILE 本地 2s / DB 直通 / RAFT 分布式 | FileSessionManager:184-193 | S-8 速查 |
| **onClose → setActive(false)**: S-1 isEndStatus 反直觉的根源实证 (跨域闭环) | AbstractSessionManager:154-156 | S-8 速查 |
| **拦截器模块化**: 2.x 移入 integration-tx-api (GlobalTransactionalInterceptorHandler 双 handler) — Scanner 只织入 | GlobalTransactionScanner:291-297 | S-9 速查 |
| **超时重置语义**: 注解未配置 (<=0 或 ==60000) → 跟随全局动态默认 | InterceptorHandler:208-211 | S-9 速查 |
| **"7种SQL类型" 表述精确化**: 路由 6 类型 + Multi + Plain; SQLType 48 值 | ExecuteTemplate:100-168 | S-10 速查 |
| **2 类型仅 3 方言**: ON_DUPLICATE/UPDATE_JOIN — 其他 NotSupportYet (执行计划未提) | ExecuteTemplate:125-158 | S-10 速查 |
| **"压缩+子表+无限重试" 全验证**: 压缩 >64k + 子表 80% + requeue — 执行计划断言通过 | AsyncWorker:88-192 | S-11 速查 |
| **片数 = ceil((len-limit)/limit)**: 子表拆分数学 (执行计划未提) — harness 自抓 2 处 | MySQLUndoLogManager:150-167 | S-11 速查 |
| **"6种Locker实现" 修正 → 7 实现**: 行锁 4 (DataBase/File/Redis/RedisLuaLocker) + 分布式 3 (DataBase/Redis/RaftDistributedLocker) | storage/*/lock/ | S-12 速查 |
| **lockQuery 只查不锁**: isLockable SELECT 检查不写入 — 执行计划未提语义 | AbstractLockManager:85-92 | S-12 速查 |
| **RM 处理器族 5 实现**: 含 SagaAnnotation (执行计划未提) — 双面多态收束 | DefaultRMHandler:49-82 | S-13 速查 |

### 2. 发现问题类型统计 (累计 190 处)

| 类型 | 数量 | 代表 |
|:--:|:--:|:--|
| **数字穷举** | 9 | 21 态 (S-1) / 5 值含 SAGA_ANNOTATION / 压缩默认 zip/64k (S-2) / **6 池 1 线程** (S-3) |
| **语义标注** | 15 | 非回滚异常提交 / doBranchDelete 反直觉 / MDC 残留 / 钩子不中断 / isEndStatus 反直觉 / clean=锁释放 / **GlobalFinished 防 Phase1 提交** (S-2) / **无限重试边界** (S-2) |
| **补充锚点** | 69 | suspend 解绑 / 并行条件 / XAER_NOTA 数学 / 双时钟 / 70s 阈值 / 状态即指令 / 并行分组 / **逆序条件** (S-2) / **max_allowed_packet** (S-2) |
| **表述精确化** | 9 | Phase1 不含 undo 细节 / 状态码映射四族 / **CHECK_SQL TODO 过时** (S-2) / **参数约定因操作类型而异** (S-2) / **5 组筛选条件** (S-3) |
| harness 自抓缺陷 | 1 | 决策矩阵首版缺 MANDATORY 无事务分支 (补全后通过) |

### 3. 方法论铁律 (V1 版, S-1 实证)

1. **执行计划是待验证假设** — S-1 修正 2 处数字 (21 态/5 值)
2. **harness 必须能自抓缺陷** — 决策矩阵首版缺分支 → 断言抓出
3. **REVIEW 追加** — 用户每次"深度 review"要求 = 新轮次
4. **时空溯源靠注释锚 + git** — Seata git 多 commit 可考古 (989db47c), 但仍以代码内注释为主 (#fix #5231)
5. **双链禁链未交付域** — 引出只指向已存在域
6. **数字/类名/模块归属必须穷举** — 21 态/5 值/60000ms 全部实证

---

## §三 高频坑汇总 (跨域 107 条)

### S-1 (17)
1. **非回滚异常提交** — rollbackOn(ex)? rollback : commit (异常不必然回滚)
2. **Launcher vs Participant** — 嵌套只 join, 驱动者是发起者
3. **先关会话再提交** — close 先于状态迁移, 防新分支注册
4. **正向 commit 反向 rollback** — 先提交先建者, 先回滚后建者
5. **doBranchDelete 反直觉** — AT 删除返回 Committed → "Delete failed" 日志但 return true
6. **isTwoPhaseHeuristic 仅 Finished** — "事务不存在" = 启发式判定
7. **双时钟超时** — 客户端/TC 各自判定, 竞态结果有界 (TimeoutRollbacking)
8. **MDC xid 残留** — begin 时 put, commit/rollback 未见 remove
9. **传播 6 种无 NESTED** — 对照 Spring 7 种 (无 savepoint 语义)
10. **TM 重试与 TC 重试分离** — client.tm.*.retry.count (客户端) vs queueToRetry* (服务端)
11. **XAER_NOTA 数学** — beginTime+timeout+max(RETRY_XAER_NOTA_TIMEOUT, timeout)
12. **XID = IP:PORT:transactionId** — TC 地址编码, 路由靠 xid
13. **isEndStatus 返回 active 非状态枚举** — timeToDeadSession 阈值 (70s/10s) 由 active 驱动, 注释误导
14. **clean() = 全局锁释放** — 非清数据; closeAndClean 仅 AT 分支 (S-12 交叉)
15. **并行按资源分组** — 同资源串行 (防同库锁竞争), 跨资源并行
16. **状态即指令** — queueToRetryCommit/Rollback 全是状态迁移, Stop 态守卫
17. **canBeCommittedAsync 全分支判定** — 任一分支不可异步 → 同步路径

### S-2 (5)
18. **GlobalFinished 防 Phase1 提交** — undo 时无 undo_log → 插标记 (issue #489), 反直觉不报错
19. **无限重试只对约束冲突** — for(;;) 其他异常抛 Retriable/Unretriable 交服务端 (S-7)
20. **默认序列化器 jackson** — 6 parser SPI; context 记录 serializer 跨版本解码
21. **InsertExecutor 只绑 PK** — 反向 SQL 参数约定因操作类型而异 (覆盖 undoPrepare)
22. **dirty → Unretriable 人工校准** — before/after/current 全不等时停重试

### S-3 (5)
23. **6 池各 1 线程** — 执行计划数字验证; branchRemoveExecutor 仅 enable && !=FILE
24. **MAX=-1 默认永远重试** — isRetryTimeout 恒 false; dead 70s 兜底终止
25. **动态延迟 = max(剩余, period)** — 最早到期会话驱动; 无会话 70s
26. **5 组筛选条件** — retryRollbacking 3 态不含 Rollbacking; end 4 态延迟清理
27. **分布式锁防重** — 未获锁重排; 多节点只一个执行 (RAFT 关键)
28. **限流 = TokenBucketLimiter** — 每秒/最大/初始令牌 3 参数 SPI, 默认关
29. **异常模板** — exceptionHandleTemplate + StoreException → FailedStore
30. **分布式锁实现** — DistributedLockerFactory 按 sessionMode (DistributedLockDO 含 ip:port 持有者)

### S-4 (4)
31. **register 双条件守卫** — 无 undo 或无 lockKey 均不注册 (只读无分支)
32. **report 成功默认关** — IS_REPORT_SUCCESS_ENABLE=false; 失败必报 5 次重试
33. **autoCommit 第二拦截点** — false→true 先 doCommit (JDBC 规范); 失败补 rollback 防半提交
34. **LockRetryPolicy 双面** — branchRollbackOnConflict && autoCommitChanged → 直通; FailFast 降级可重试
35. **单语句 = 隐式全局事务** — autoCommit=true 单条 DML 走完整提交链 (changeAutoCommit→execute→commit→reset)
36. **锁重试第 N+1 次超时** — --times<0 抛 LockWaitTimeout; FailFast 立即抛; GlobalLockConfig 优先
37. **prepareStatement PK 数组** — 表元数据主键 → 可读主键 statement (镜像定位底层)
38. **AsyncWorker 1000 分片** — 2 线程 1s 周期批删 undo_log (S-11)

### S-5 (4)
39. **无 NESTED** — 6 传播 vs Spring 7; NEVER/MANDATORY 镜像对称
40. **挂起不冻结计时** — suspend 不改 createTime, 长挂起触发超时
41. **clean 双语义** — suspend(true) 结束解绑 (null holder) vs suspend(false) 临时挂起 (holder)
42. **NOT_SUPPORTED 内 MANDATORY 抛** — 环境清空效应; 挂起栈 LIFO 恢复
43. **rollbackOn = Spring RollbackRule 移植** — getDepth 继承深度匹配; noRollbackFor → 提交
44. **响应式无内置实现** — ContextCore 仅 ThreadLocal/FastThreadLocal; "可替换"是推断面

### S-6 (4)
45. **7 钩子漏 2** — 执行计划漏 afterCommit/afterRollback (总数 7 对)
46. **getHooks 非缓存** — 每次新建只读包装; TCC 用 volatile 缓存 (两体系对比)
47. **Participant 不清钩子** — cleanUp 归 Launcher; 嵌套共享外层钩子
48. **finally 顺序** — 锁配置恢复 → afterCompletion → clear; 钩子异常独立 catch
49. **Saga 也全 7 钩子** — 模板同构 (模式无关); 守卫粒度: Saga 每 trigger 内判 vs AT 外层
50. **afterCompletion null 分支** — tx==null 也触发收尾 (早期失败路径)
51. **无内置调用者** — registerHook 纯 SPI 风格; TccHook 门面 3 周期 6+ 方法
52. **TCC 与 TM 钩子双体系** — CopyOnWriteArrayList 全局 (常驻) vs ThreadLocal (事务级)

### S-7 (4)
53. **MAX=-1 默认永远重试** — 靠 RETRY_DEAD_THRESHOLD 70s 终止; 终态后不再入队 (Stop 守卫)
54. **终态三态** — isRetryTimeout → RetryTimeout / Timeout 系 → TimeoutRollbackFailed / else → Failed
55. **默认不解锁** — ROLLBACK_FAILED_UNLOCK_ENABLE=false; 双开关 OR 任一开才解锁
56. **DELAY_HANDLE_SESSION** — DB/REDIS 模式终态延迟清理 (防锁/分支残留)
57. **isRetryTimeout 基准 = beginTime** — 重试窗口从 begin 起算 (非失败时刻); MAX 为正时压缩窗口
58. **校验器 4 方法族** — isTimeoutGlobalStatus 4 态 / isRollbackGlobalStatus 5 态 (含终态) / isEndGlobalStatus 4 态
59. **Timeout 特判埋点** — TimeoutRollbacking → TimeoutRollbacked 专属 Metrics 先发
60. **Metrics 计时键** — STATUS_VALUE_AFTER_COMMITTED/ROLLBACKED_KEY + beginTime + retryBranch (端到端耗时)

### S-8 (4)
61. **onClose → setActive(false)** — S-1 isEndStatus 反直觉根源 (跨域闭环)
62. **lockAndExecute 三模式** — FILE 本地 2s / DB 直通 / RAFT 分布式; ReentrantLock 可重入+持有者解锁
63. **状态联动锁标记** — Rollbacking/Timeout → 分支 LockStatus.Rollbacking
64. **Redis Lua 原子** — Lua 单脚本防竞态; Lua/Java 双实现可选
65. **transactionName 静默截断** — columnSize 从表结构读 + substring (数据丢失无警告)
66. **reload 状态机三面** — 终态收尾/错误态 removeInErrorState/重试续跑
67. **Redis 4 Lua 脚本** — acquire/release/update/lockable + pipeline 模式
68. **DB 每写独立事务** — setAutoCommit(true) 单写单提交 (无批事务)

### S-9 (4)
69. **超时重置语义** — 注解 <=0 或 ==60000 → 跟随全局动态默认 (非固定)
70. **拦截器 2.x 模块化** — integration-tx-api 双 handler (TM/GlobalLock); Scanner 只织入
71. **rollbackRules 注解源** — rollbackFor/noRollbackFor → RollbackRule 家族 (S-5 闭环)
72. **已代理按序织入** — ORDER_NUM=1024 + addAdvisor 定位; FactoryBean 排除
73. **责任链 parser** — SPI parser 族 + 同类型重复 Runtime + order 链式串联 (多注解共存)
74. **Advisor 相对定位重排** — After/Before 相对 Spring 事务自动调 Order
75. **lockRetryTimes 默认 -1** — AspectTransactional 默认 (S-12 语义面)
76. **1.5 默认组警告** — DEFAULT_TX_GROUP_OLD 迁移提示 + TM/RM 双客户端 init

### S-10 (4)
77. **路由 6+Multi+Plain** — 48 SQLType 仅消费 8; SELECT 普通查询也 Plain (只读无镜像)
78. **2 类型仅 3 方言** — ON_DUPLICATE/UPDATE_JOIN (MySQL/Mariadb/PolarDBX), 其他 NotSupportYet
79. **守卫直通** — 非 AT 无 GlobalLock 零开销; InsertExecutor SPI 方言
80. **sqlparser 三模块** — druid/antlr 双解析器 SPI 切换; Multi sqlserver 变体
81. **三入口无旁路** — executeQuery/executeUpdate/execute 全经 ExecuteTemplate
82. **Multi 仅 UPDATE/DELETE** — 按表分组; default UnsupportedOperationException

### S-11 (5)
83. **片数 = ceil((len-limit)/limit)** — 子表拆分 (1.7MB→2 子片/2.5MB→3 子片, harness 自抓)
84. **满时背压** — offer 失败 → 紧急清+重入; 队列不无限增长
85. **requeue 无限重试** — 资源缺失/SQLException → 重入队不丢
86. **1000 分片双语义** — 批删分批 + MySQL IN 上限规避
87. **maxAllowedPacket × 0.8** — 拆分阈值; 默认 1MB (mysql5.6)
88. **压缩 8 类型 SPI** — NONE/GZIP/ZIP/SEVENZ/BZIP2/LZ4/DEFLATER/ZSTD
89. **RM 删除链 LIMIT 3000** — RmUndoLogProcessor → RMHandlerAT do-while 迭代
90. **saveDays 兜底 7 天** — <=0 用默认; now(6) 微秒清理精度

### S-12 (5)
91. **Locker 7 实现** — 行锁 4 (DataBase/File/Redis/Lua) + 分布式 3 (DataBase/Redis/Raft); 计划 "6种" 修正
92. **lockQuery 只查不锁** — isLockable SELECT 不写入; 检查-插入两段 (去重→IN 检查→插入)
93. **lockKey 三层格式** — 表:主键 分号/冒号/逗号; LockDO 5 列
94. **LockStatus 2 值** — Locked/Rollbacking; S-8 状态联动; updateLockStatus xid 级
95. **默认不解锁** — 终态人工校准; skipCheckLock 旁路 (重试面)
96. **锁获取在分支注册** — branchRegister → branchSessionLock → acquireLock; Phase1 完成前锁齐
97. **客户端控制锁行为** — applicationData 传 AUTO_COMMIT/SKIP_CHECK_LOCK
98. **非 AT 无行锁** — AbstractCore branchSessionLock 空实现 (TCC/XA/SAGA)

### S-13 (5)
99. **处理器族 5 实现** — 含 SagaAnnotation; SPI loadAll + 单例分发
100. **提交异步回传 Committed** — AsyncWorker 入队立即; 无异步回调 (靠状态/重试)
101. **回滚同步补偿** — undoManager.undo (镜像+校验+重试)
102. **状态回传驱动** — Committed→removeBranch / Retryable→queueToRetry (S-7)
103. **异常双侧模板** — exceptionHandleTemplate TC/RM 同构; UndoLogDelete 基类空 (#2226)
104. **资源管理器双面** — DefaultResourceManager CHM + SPI; branchCommit/lockQuery 全多态
105. **异常码三态回填** — Success/TransactionExceptionCode/RuntimeException; LockKeyConflict 重试提示
106. **回滚处理器对称** — RmBranchRollbackProcessor 与 commit 同构
107. **RMClient 两件套** — setResourceManager + setTransactionMessageHandler

---

## §四 阶段 4.4 收官 — 无剩余域

- **13/13 全量交付** (2026-08-15): 大纲 857 行 / 域文件 5668 行 / REVIEW 190 处 / harness 11/11 (A-D 4 面)
- **跨域闭环**: S-1 状态机 → S-2 undo → S-3 调度 → S-4 代理 → S-5 传播 → S-6 钩子 → S-7 重试 → S-8 存储 → S-9 集成 → S-10 路由 → S-11 可靠性 → S-12 锁 → S-13 收束
- **后续**: 阶段 4.5 Curator (5 域, 依赖 ZK 已满足) — SofaJRaft (4.6, 5 域)

| 序 | 域 | 文件 (行) | 方案 | 预判 | 预计闭环 |
|:--:|:--|:--|:--:|:--|:--|
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 5 | S-5 事务传播 | spring/tm Propagation | 🔴 A | 6 传播 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore+rm 处理器族 | 🔴 A | 多态收束 | 8 |
| 13 | S-13 Phase2 分支通知 | server/DefaultCore | 🔴 A | 多态收束 | 8 |

---

## §五 完成检查单 (阶段 4.4 收官)

- [x] **13/13 全量交付** (2026-08-15): 大纲 857 行 / 域文件 5668 行 / REVIEW 186 处 / harness 11/11
- [x] 每域: Pass 0-3 + 六层深审 + 时空溯源 + harness (9/9) + 多次 REVIEW
- [x] HANDOFF-STAGE3 注记已更新 (阶段 4.4 → 13/13)
- [ ] 每域: Pass 0-3 + 六层深审 + 时空溯源 + harness (🔴) + 多次 REVIEW + 更新本文 §零/§一/§四 + HANDOFF-STAGE3
- [ ] 每域 REVIEW 记录真实问题, 零发现=不合格
- [ ] 每域完成时把该域速查从 §四 详案区移入 §一

---

## §六 文件路径

```
analysis/source-analysis/seata/
├── SEATA-PLAN.md      ← 13 域规划 (v1, 09 审计: 3 补充 + 5 待验证)
├── HANDOFF-SEATA.md   ← 本文 V1 (超详细全量交接, 唯一入口)
├── outlines/
│   ├── s1-at-two-phase/   s2-undo-log/   s3-tc-server/   s4-datasource-proxy/   s5-propagation/   s6-transaction-hook/   s7-retry-recovery/   s8-session-store/   s9-spring-integration/   s10-sql-router/   s11-undo-reliability/   s12-global-lock/   s13-phase2-branch/  (每域 9 文件)
├── harness/
│   ├── s1-at-two-phase/MiniSeataPhase.java   s2-undo-log/MiniUndoLog.java   s3-tc-server/MiniSeataCoordinator.java   s4-datasource-proxy/MiniDataSourceProxy.java   s5-propagation/MiniPropagation.java   s6-transaction-hook/MiniTransactionHook.java   s7-retry-recovery/MiniSeataRetry.java   s8-session-store/MiniSeataSessionStore.java   s11-undo-reliability/MiniUndoReliability.java   s12-global-lock/MiniGlobalLock.java   s13-phase2-branch/MiniPhase2Branch.java
源码: /data/workspace/source-code/code/spring/seata/  (Seata 2.5.0, 主包 org.apache.seata)
上级: ../HANDOFF-STAGE3.md (阶段3 总入口 — 阶段 4.4 状态 13/13 收官注记)
后续: 阶段 4.5 Curator (5 域) — 4.6 SofaJRaft (5 域)
```
