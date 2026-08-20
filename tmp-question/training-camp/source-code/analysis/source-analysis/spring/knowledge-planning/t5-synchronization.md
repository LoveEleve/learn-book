# T-5 TransactionSynchronization — afterCommit/afterCompletion 回调

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | TransactionSynchronizationManager + TransactionSynchronization 接口
> 基线: T-1 链路 — commit/rollback 时触发 TransactionSynchronization 回调链

---

## §0.8

- 🟡 Working，1篇 — TransactionSynchronization 回调接口(5个方法) + TransactionSynchronizationManager(ThreadLocal List管理) + @TransactionalEventListener
- 设计模式: [模式: 观察者]—事务事件回调注册到commit/rollback生命周期

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TransactionSynchronization.java:45 | 接口 | 5个回调: beforeCommit/afterCommit/beforeCompletion/afterCompletion/flush → Ordered(排序) | High |
| TransactionSynchronizationManager.java:74 | 管理器 | ThreadLocal<List<TransactionSynchronization>>—initSynchronization创建list→registerSynchronization添加→getSynchronizations排序→commit时触发回调 | High |
| TransactionSynchronizationManager.java:271 | registerSynchronization() | 注册回调到当前线程的事务→AbstractPlatformTransactionManager.commit→triggerBeforeCommit→txObject.getSynchronizations→逐个 beforeCommit→doCommit→triggerAfterCompletion→逐个 afterCompletion | High |
| AbstractPlatformTransactionManager.commit/rollback | 触发时机 | commit: triggerBeforeCommit→doCommit→triggerAfterCommit→triggerAfterCompletion / rollback: triggerAfterCompletion(直接→不调commit回调) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 事务回调是单一职责机制—核心是5个回调+ThreadLocal管理+commit/rollback触发点。1篇(~35行)。

**P1 核心** 🔴: | # | beforeCommit→commit→afterCommit→afterCompletion 执行顺序 + commit vs rollback 触发差异 — **为什么** 🔴: @TransactionalEventListener(phase=AFTER_COMMIT)依赖此机制—beforeCommit是"事务提交前最后一刻的检查点"(如发MQ消息前确认事务一定提交—beforeCommit在doCommit之前/afterCommit在doCommit之后)
