# S-13 Phase2 分支通知 — 处理器族与收束闭环

> 前置: 全部 13 域 (本域是收束点) | 对照: RocketMQ 事务消息 (阶段4.1 RM-12)
> 🔴 A | 8 KP | [模式: 双面多态 + 状态回传]
> Pass 2 闭环: q1(处理器族) q2(接收端协议) q3(AT 收束) q4(完整闭环)

**读者处境**: TC 决定提交/回滚后, 分支怎么被通知执行? 这篇拆 RM 处理器族 (5 实现) + 接收端 + AT 收束双路径 — 13 域收官。

### 1. 处理器族 — 5 实现 + SPI 注册

场景: RM 侧怎么多态?
源码路径:
- **处理器族 5 实现**: **RMHandlerAT / RMHandlerXA / RMHandlerTCC / RMHandlerSaga / RMHandlerSagaAnnotation** (执行计划未提 SagaAnnotation)
- **SPI 注册** (DefaultRMHandler:49-54): **EnhancedServiceLoader.loadAll** → allRMHandlersMap.put(getBranchType)
- **多态分发** (L66-82): handle(Commit/Rollback/UndoLogDelete) → **getRMHandler(branchType)**; 单例 (SingletonHolder L94-100); ⚠ **资源管理器双面**: DefaultResourceManager CHM + SPI (L43-73) — branchCommit/lockQuery 全多态
- **资源管理器多态** (RMHandlerAT:122-123): DefaultResourceManager.getResourceManager(branchType)
关键设计 (q1): **5 处理器 SPI + 双面多态** (getCore/getRMHandler)。[模式: 处理器族]

### 2. 接收端与协议 — RmBranchCommitProcessor

场景: 通知怎么到达?
源码路径:
- **接收端** (RmBranchCommitProcessor:42-63): handleBranchCommit → **handler.onRequest(BranchCommitRequest)** → 响应回传; ⚠ **RmBranchRollbackProcessor 对称** (L58-64); **RMClient 装配两件套**: setResourceManager + setTransactionMessageHandler (RMClient:31-45)
- **RmBranchRollbackProcessor**: 对称
- **协议字段**: **xid / branchId / resourceId / applicationData** — 分支身份完整
- **状态回传**: BranchStatus (PhaseTwo_*) → TC 决策 (S-1:329-363)
关键设计 (q2): **协议 4 字段 + 状态回传驱动 TC 决策**。[模式: 协议+回传]

### 3. AT 收束 — 提交异步 + 回滚补偿

场景: AT 分支怎么执行?
源码路径:
- **提交** (RMHandlerAT.doBranchCommit → DataSourceManager.branchCommit): **AsyncWorker 入队立即返回 PhaseTwo_Committed** (S-11) — Phase2 提交非阻塞; 无异步回调 (TC 靠状态/重试 S-7)
- **回滚** (doBranchRollback): **undoManager.undo** (S-2) — 镜像恢复 + 校验 + 无限重试
- **异常模板**: exceptionHandleTemplate — **三态错误码** (Success/TransactionExceptionCode/RuntimeException 回填, AbstractExceptionHandler:95-135) + **LockKeyConflict 重试提示日志**
- **UndoLogDeleteRequest**: 基类默认空 (issue #2226) — RMHandlerAT 覆盖 (S-11)
关键设计 (q3): **提交异步 + 回滚补偿双路径**。[模式: AT 收束]

### 4. 完整闭环 — S-1→S-13 收束

场景: 13 域怎么闭合?
源码路径:
- **完整链路**: S-1 doGlobalCommit (正向) → getCore.branchCommit → S-3 Netty 通知 → 本域 RmBranchCommitProcessor → DefaultRMHandler → RMHandlerAT → **S-11 AsyncWorker / S-2 undo** → BranchStatus 回传 → S-1 removeBranch → 终态; 终态后 doBranchDelete 联动 (S-1:148-219)
- **状态回传消费** (S-1:328-363): Committed → removeBranch; Unretryable → endCommitFailed; 其他 → queueToRetryCommit (S-7)
- **双面多态**: BranchType → getCore (TC) / getRMHandler (RM)

## 代码类型
Architecture (收束闭环)

## 负面空间 — Phase2 分支通知刻意不做的事

- **不做同步两阶段**: AT 提交异步化 — 无同步确认 (最终一致)
- **不做分支级事务**: 通知即执行 — 无本地事务包装 (undo 自带)
- **不做通知重试确认**: 状态回传后 TC 决定 — 无 ACK 握手 (对照 RocketMQ 事务消息确认)
- **不做分支优先级**: 通知顺序 = 遍历顺序 (S-1)
- **不做批量通知**: 逐分支发送 (无合并)
- **不做端到端追踪**: MDC xid 仅日志关联

→ 阶段 4.4 收官 → Curator (4.5) / SofaJRaft (4.6)
