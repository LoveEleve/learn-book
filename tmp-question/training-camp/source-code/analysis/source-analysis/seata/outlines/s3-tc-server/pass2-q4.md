# 闭环笔记 q4: 会话管理面 — SessionHolder + 分布式锁

## 假设
TC 的会话存取统一走 SessionHolder; 定时任务用分布式锁防集群重复执行。

## 验证过程
- **SessionHolder** (server/session): findGlobalSessions(SessionCondition) — 按状态/条件查询 (lazyLoadBranch 懒加载分支); lockAndExecute (S-1 已实证 — 会话级原子操作); **distributedLockAndExecute(key, func)** (L429) — 定时任务防重
- **distributedLockAndExecute 语义** (L625-636 实证): 获锁 → 执行任务; **未获锁 → 重新 schedule(period)** (L630-632) — **多节点/多副本只一个执行** (RAFT 模式关键)
- **SessionCondition** (L407-408): 状态筛选 + setLazyLoadBranch(true) — 全量扫描按需加载分支
- **timeToDeadSession** (GlobalSession:222-228, S-1 实证): end 态 10s / 其他 70s 阈值 — 动态延迟调度依据
- **undoLogDelete** (L548-570): **ChannelManager.getRmChannels() → 逐 RM 渠道广播 UndoLogDeleteRequest (saveDays)** — 无 RM 渠道跳过; saveDays 默认 (UndoLogDeleteRequest.DEFAULT_SAVE_DAYS)
- **destroy 的 SessionHolder 收尾** (L843): 会话存储关闭 — 幂等面

## 代码类型
Implementation (会话管理)

## 跨域关联
- S-8: SessionHolder/SessionMode (本域是调度面, S-8 是存储面)
- S-11: undoLogDelete 广播 (RM 侧清理)
- S-1: lockAndExecute (状态迁移原子性)

## 结论
会话管理 = SessionHolder 统一存取 + 分布式锁防重 (未获锁重排) + 状态条件懒加载查询。
源码位置: SessionHolder.java:429; DefaultCoordinator.java:548-570,625-636,739-753
