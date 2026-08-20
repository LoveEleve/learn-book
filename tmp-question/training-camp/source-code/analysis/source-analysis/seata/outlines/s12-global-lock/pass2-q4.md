# 闭环笔记 q4: 锁状态与释放 — LockStatus + 释放链

## 假设
锁有状态标记; 释放分分支/全局两级。

## 验证过程
- **LockStatus 2 值** (core/model/LockStatus): **Locked(0) / Rollbacking(1)** — S-8 状态联动 (Rollbacking 全局状态 → 分支锁标记)
- **updateLockStatus** (AbstractLockManager:194): xid 级更新锁状态 — 回滚中锁标记 (并发读感知)
- **释放两级** (DataBaseLockManager:58-73): **releaseLock(branchSession) 分支级** + **releaseLock(globalSession) 全局级** — S-1 clean 链 (releaseGlobalSessionLock)
- **cleanAllLocks** (L100-101): 全清 (测试/运维)
- **释放时机** (S-7/S-8 交叉): 终态 end() → isTwoPhaseSuccess ? clean : onFailEnd (默认不解锁 — 人工)
- **RollbackFailed 解锁开关**: ROLLBACK_FAILED_UNLOCK_ENABLE (S-7 已实证)

## 代码类型
Implementation (锁状态)

## 跨域关联
- S-8: LockStatus.Rollbacking 联动 (updateGlobalSessionStatus)
- S-7: 终态解锁开关
- S-1: clean=释放全局锁

## 结论
状态 = Locked/Rollbacking 2 值; 释放分分支/全局两级; 终态解锁默认关 (人工)。
源码位置: core/model/LockStatus.java:25-30; AbstractLockManager.java:71-101,194
