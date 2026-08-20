# S-6 TransactionHook — Pass 1 探索笔记

> 域: S-6 TransactionHook | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: tm/api/transaction/ (TransactionHook 55 + TransactionHookManager 66) + tm/api/TransactionalTemplate (触发面, S-1 已实证) + saga/DefaultSagaTransactionalTemplate + integration-tx-api/TccHookManager | Seata 2.5.0

## 调用图

```
业务 (S-9 集成) → TransactionHookManager.registerHook (ThreadLocal 栈)
  → TransactionalTemplate.execute (S-1):
    begin 周期: triggerBeforeBegin → tx.begin → triggerAfterBegin
    commit 周期: triggerBeforeCommit → tx.commit → triggerAfterCommit
    rollback 周期: triggerBeforeRollback → tx.rollback → triggerAfterRollback
    finally: triggerAfterCompletion (仅 Launcher) → cleanUp (Launcher 才 TransactionHookManager.clear)
  钩子异常: catch(Exception) → LOGGER.error → 继续 (不中断主流程)
跨模式复用: Saga DefaultSagaTransactionalTemplate 同模式触发 (Launcher 守卫)
对比: TccHookManager (CopyOnWriteArrayList 全局 + 缓存不可变) — 与 ThreadLocal 模型不同
```

## 基本元素分解

1. **钩子接口**: TransactionHook 7 方法 (beforeBegin/afterBegin/beforeCommit/afterCommit/beforeRollback/afterRollback/afterCompletion)
2. **管理器**: TransactionHookManager (ThreadLocal<List> + getHooks 只读 + registerHook + clear)
3. **触发面**: TransactionalTemplate 7 trigger 方法 (S-1 已实证)
4. **异常语义**: 钩子异常只 log 不中断
5. **跨模式**: Saga 复用同机制; TCC 独立体系 (全局列表)

## 标记问题 (20 问)

1. 接口 7 钩子? (beforeBegin~afterCompletion)
2. afterCompletion 语义? (全完成收尾)
3. 管理器存储? (ThreadLocal<List>)
4. getHooks 只读? (unmodifiableList)
5. registerHook? (null 检查 + lazy ArrayList)
6. clear? (LOCAL_HOOKS.remove)
7. 触发时机? (TransactionalTemplate 7 处)
8. afterCompletion 守卫? (仅 Launcher)
9. cleanUp? (Launcher 才 clear)
10. 钩子异常? (catch + log + 继续)
11. Saga 复用? (DefaultSagaTransactionalTemplate 同模式)
12. TCC 钩子? (TccHookManager 全局列表)
13. 对照 TCC? (ThreadLocal vs CopyOnWriteArrayList)
14. Participant 钩子? (begin 不触发, afterCompletion 不触发?)
15. 注册时机? (业务执行前 registerHook)
16. 空钩子? (getHooks emptyList)
17. 并发? (ThreadLocal 隔离)
18. 多次事务? (clear 后重新注册)
19. 钩子顺序? (注册序)
20. 钩子与传播? (挂起时钩子?)

## 时空溯源 (代码内注释锚)

- TransactionHookManager: 接口与管理器早期定型 (0.9 锚, 无版本注释)
- TransactionalTemplate:124 "Of course, the hooks will still be triggered" — Participant 也触发钩子 (S-1 注释锚)
- DefaultSagaTransactionalTemplate: Launcher 守卫触发 (Saga 模式复用)
- TccHookManager: 独立体系 (TCC 门面钩子)

## 大域拆分判断

S-6 = 钩子机制面 (接口 + 管理器 + 触发编排 + 跨模式对比); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "7个生命周期钩子(beforeBegin→afterBegin→beforeCommit/rollback→afterCompletion)" | 接口 7 方法实证 (TransactionHook:19-55); 触发面 S-1 已实证 — **计划缺 afterCommit/afterRollback 但总数 7 对** | **接受+表述补充** ✅ |
| "钩子注册/清空时机" | registerHook (业务前) + cleanUp (Launcher 才 clear, S-1) | **接受** ✅ |
| "钩子异常不中断" | 全部 trigger catch(Exception) → log (S-1 实证) | **接受** ✅ |
| 对照 TCC | TccHookManager 全局 CopyOnWriteArrayList + 缓存 — 与 ThreadLocal 不同 | **补充** ✅ |
