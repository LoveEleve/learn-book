# 闭环笔记 q4: 跨模式复用 — Saga/TCC 对比

## 假设
钩子机制跨事务模式复用 (AT/Saga); TCC 有独立体系; 对比存储模型。

## 验证过程
- **Saga 复用** (DefaultSagaTransactionalTemplate:125-160): triggerBeforeBegin/AfterBegin/BeforeRollback — **同模式**: Launcher 守卫 + catch 不中断 — **钩子机制与模式解耦** (S-13 交叉)
- **TCC 独立体系** (TccHookManager, integration-tx-api): **CopyOnWriteArrayList 全局列表** + **CACHED_UNMODIFIABLE_HOOKS 缓存** (L33-35) — 与 TransactionHookManager 的 **ThreadLocal** 完全不同:
  | 维度 | TransactionHookManager | TccHookManager |
  |:--|:--|:--|
  | 存储 | ThreadLocal (事务级) | CopyOnWriteArrayList (全局) |
  | 作用域 | 单线程单事务 | 全局注册 (应用级) |
  | 清空 | clear (事务结束) | 无 clear (常驻) |
  | 视图 | unmodifiableList (每次) | 缓存不可变 (volatile) |
- **用途差异**: TransactionHook = 事务生命周期观察; TccHook = TCC 门面拦截 (prepare/commit/rollback 前后)

## 代码类型
Architecture (复用对比)

## 跨域关联
- S-13: Saga (本域机制被复用)
- S-9: Spring 集成 (钩子注入面)

## 结论
钩子机制 = 模式无关 (AT/Saga 同模板); TCC 独立全局体系 — 存储模型 ThreadLocal vs 全局列表。
源码位置: DefaultSagaTransactionalTemplate.java:125-160; TccHookManager.java:27-62
