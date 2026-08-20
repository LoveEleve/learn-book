# S-6 TransactionHook — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: TransactionHook 7 方法 + TransactionHookManager (ThreadLocal) — 随模板定型 |
| 1.x | afterCompletion 语义细化 (仅 Launcher); cleanUp 清除时机 (Launcher 才 clear) |
| 2.x | **跨模式复用**: Saga DefaultSagaTransactionalTemplate 同模式触发; TCC 独立体系 (TccHookManager CopyOnWriteArrayList + 缓存) |
| 2.5.0 | 钩子机制稳定 (无重大变更) |

## 痕迹证据

- TransactionalTemplate.java:124: "Of course, the hooks will still be triggered" — Participant 也触发 (1.x 锚)
- TransactionalTemplate.java:381-391: triggerAfterCompletion 仅 Launcher — 守卫注释 (1.x 锚)
- TransactionalTemplate.java:393-402: cleanUp Launcher 才 clear — 清除时机 (1.x 锚)
- DefaultSagaTransactionalTemplate.java:125-160: Launcher 守卫触发 (2.x 锚)
- TccHookManager.java:33-35: CopyOnWriteArrayList + CACHED_UNMODIFIABLE_HOOKS (2.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x Saga/TCC 体系" — 文件存在性推断 (标注)
- "2.5.0 稳定" — 无重大变更推断 (标注)
- git 多 commit 可考古 — 本域以注释锚为主

## 对照线 (阶段 3 已交付)

- Spring TransactionSynchronization: beforeCommit/afterCommit/afterCompletion 同步器 — 与 Seata 钩子同构 (afterCompletion 语义一致)
- Spring ApplicationEvent: 通用事件总线 vs Seata 事务 7 点观察 — 范围对比
