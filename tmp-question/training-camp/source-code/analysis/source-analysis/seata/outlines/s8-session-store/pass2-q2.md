# 闭环笔记 q2: 生命周期映射 — writeSession 6 操作 + 状态关联

## 假设
会话生命周期事件映射到存储操作; 状态变更带锁状态关联。

## 验证过程
- **writeSession 6 操作** (AbstractSessionManager:173-195): GLOBAL_ADD/UPDATE/REMOVE + BRANCH_ADD/UPDATE/REMOVE — 全部经 TransactionStoreManager.writeSession; **失败 → FailedWriteSession 异常族** (Global/Branch 区分)
- **onBegin → addGlobalSession** (L127-129) — S-1 GlobalSession.begin 持久化
- **onStatusChange → updateGlobalSessionStatus** (L132-134): ⚠ **Rollbacking/TimeoutRollbacking → 全部分支锁标记 LockStatus.Rollbacking** (L85-87) — 状态变更联动锁状态 (S-12 交叉)
- **onSuccessEnd → removeGlobalSession** (L159-161) — 成功终态删会话
- **onFailEnd** (L163-171): **rollbackFailedUnlockEnable → clean (解锁) + return** / else 保留会话 — 失败终态解锁开关的第二个消费点 (S-7 交叉)
- **onClose → setActive(false)** (L154-156) — **S-1 isEndStatus 反直觉的根源** (active 标志源头)
- **onAddBranch/onRemoveBranch/onBranchStatusChange** (L137-151): 分支操作映射

## 代码类型
Implementation (生命周期映射)

## 跨域关联
- S-1: begin/end 持久化链
- S-7: onFailEnd 解锁开关
- S-12: LockStatus.Rollbacking 联动

## 结论
生命周期 = 6 存储操作映射 + 状态变更联动锁标记 + 失败终态解锁开关 (默认关)。
源码位置: AbstractSessionManager.java:73-195
