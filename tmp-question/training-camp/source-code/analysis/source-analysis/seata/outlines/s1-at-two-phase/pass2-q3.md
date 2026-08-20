# 闭环笔记 q3: TC Phase2 — 分支遍历方向与提交路径

## 假设
TC 协调 = 状态迁移 (lockAndExecute 保护) + 分支遍历 (commit 正向/rollback 反向) + 失败转异步重试。

## 验证过程
- **begin** (DefaultCore:222-234): GlobalSession.createGlobalSession → **session.begin()** (GlobalSession:237-245: status=Begin + beginTime + active + **onBegin 持久化**) → MDC.put(xid)
- **commit** (L237-281): findGlobalSession → null → **Finished**; **isTimeout → TimeoutRollbacking** (TC 侧超时, L243-246) → **lockAndExecute** (L249-265, S-8 交叉): status==Begin → **session.close() (Highlight 注释: 先关会话 — 不能再注册分支)** → **canBeCommittedAsync ? asyncCommit : Committing+shouldCommitNow** → clean → doGlobalCommit(globalSession, false) → 剩余分支可异步 → asyncCommit → Committed
- **doGlobalCommit** (L284-397): saga → SAGA core; 否则 **getSortedBranches 正向遍历** (L292, S-13 实证) — 每分支: 非重试跳过 canBeCommittedAsync (L297-299) / **PhaseOne_Failed → removeBranch** (L302-305) / **XA RDONLY → removeBranch** (L309-313, Oracle 只读优化) / retrying && STOP_RETRY 跳过 → **getCore(branchType).branchCommit** (多态, L320) → PhaseTwo_Committed → removeBranch / **CommitFailed_Unretryable → endCommitFailed 终态** (L336-343) / 其他 → **queueToRetryCommit** (L347-349) — 异常: 非重试 → queueToRetryCommit + throw (L364-375); **PARALLEL_HANDLE_BRANCH 默认 false** (L61-62, 配置启用且 ≥2 分支才并行)
- **rollback** (L400-420): lockAndExecute → close → status==Begin → Rollbacking → doGlobalRollback → Rollbacked / 返回状态
- **doGlobalRollback** (L423-509): **getReverseSortedBranches 反向遍历** (逆序回滚 — L431) — 分支: PhaseOne_Failed 移除 / retrying && STOP_RETRY 跳过 → branchRollback → PhaseTwo_Rollbacked → removeBranch / **RollbackFailed_Unretryable → endRollbackFailed** / 其他 → queueToRetryRollback; **endRollbacked 延迟** (L502-503 注释: db 模式锁/分支残留问题)
- **doBranchDelete** (L148-219): **AT 用 commit 语义删** (branchDelete→Committed = "分支已提交无需删" → true) / TCC/XA 用 rollback 语义 / **XAER_NOTA retry timeout → 强制清理** (L187-194) / Unretryable → 停重试删除 (L204-211)
- **XAER_NOTA 数学** (L539-549): beginTime + timeout + **max(RETRY_XAER_NOTA_TIMEOUT, timeout)** — 资源不存在重试超时窗口

## 代码类型
Architecture (TC 协调)

## 跨域关联
- S-8: lockAndExecute + GlobalSession + SessionHolder
- S-7: queueToRetryCommit/Rollback + retrying 参数
- S-13: getCore(branchType) 多态 + 正向/反向遍历 (本域是 S-13 的消费面)
- S-3: DefaultCoordinator 调度 (本域被其调用)

## 结论
Phase2 = 关会话防新分支 → 状态迁移 (lockAndExecute 原子) → 正向 commit / 反向 rollback 遍历 → 失败转异步重试; 异步提交/并行处理可配。
源码位置: DefaultCore.java:222-549; GlobalSession.java:237-245,615-619
