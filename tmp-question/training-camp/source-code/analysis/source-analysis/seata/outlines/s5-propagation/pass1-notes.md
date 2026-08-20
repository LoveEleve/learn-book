# S-5 事务传播 — Pass 1 探索笔记

> 域: S-5 事务传播 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: tm/api/transaction/ (Propagation 177 + SuspendedResourcesHolder 44 + TransactionInfo) + tm/api/DefaultGlobalTransaction (suspend/resume) + core/context/RootContext + ContextCore SPI (ThreadLocal/FastThreadLocal) | Seata 2.5.0

## 调用图

```
@GlobalTransactional (propagation=X, S-9) → TransactionInfo (timeOut/propagation/lockRetry*)
  → TransactionalTemplate.execute (S-1 消费面, L66-113):
    switch (propagation):
      NOT_SUPPORTED: 有事务 → suspend(false) → 无事务执行 → finally resume
      REQUIRES_NEW:  有事务 → suspend(false) → 新建 tx → 执行 → finally resume
      SUPPORTS:      无事务 → 直接执行
      REQUIRED:      getCurrentOrCreate
      NEVER:         有事务 → 抛
      MANDATORY:     无事务 → 抛
  suspend → RootContext.unbind() → SuspendedResourcesHolder(xid) (clean=true → null)
  resume  → RootContext.bind(xid)
RootContext: CONTEXT_HOLDER = ContextCoreLoader.load() (SPI: ThreadLocal/FastThreadLocal)
```

## 基本元素分解

1. **传播枚举**: Propagation 6 值 (REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/SUPPORTS/NEVER/MANDATORY — 无 NESTED)
2. **挂起恢复**: suspend/resume + SuspendedResourcesHolder (单 xid)
3. **上下文存储**: RootContext + ContextCore SPI (ThreadLocalContextCore/FastThreadLocalContextCore)
4. **消费面**: TransactionalTemplate switch (S-1 已实证) — 本域是定义面
5. **配置载体**: TransactionInfo (timeOut/propagation/lockRetryInterval/Times)

## 标记问题 (20 问)

1. Propagation 枚举? (6 值)
2. 无 NESTED? (对照 Spring 7 种)
3. REQUIRED 语义? (getCurrentOrCreate)
4. REQUIRES_NEW? (挂起+新建)
5. NOT_SUPPORTED? (挂起+无事务执行)
6. SUPPORTS? (有则 join 无则直跑)
7. NEVER? (有则抛)
8. MANDATORY? (无则抛)
9. suspend? (unbind + holder)
10. suspend(clean)? (clean → null holder)
11. resume? (bind)
12. SuspendedResourcesHolder? (单 xid + null 检查)
13. RootContext? (bind/unbind + MDC)
14. ContextCore SPI? (ThreadLocal/FastThreadLocal)
15. 嵌套 REQUIRES_NEW? (两段挂起栈)
16. NOT_SUPPORTED 内 MANDATORY? (抛 — 无事务环境)
17. 对照 Spring? (7 vs 6; NESTED 缺)
18. TransactionInfo? (timeOut/propagation/lockRetry*)
19. commit/rollback 后? (suspend(true) 解绑)
20. 异步场景? (ContextCore 替换面 — 响应式)

## 时空溯源 (代码内注释锚)

- Propagation Javadoc 伪代码 (L35-177): 每值附代码示例 (0.9 锚)
- DefaultGlobalTransaction:213 "In order to associate the following logs with XID, first get and then unbind" — 挂起顺序注释
- RootContext:127 "xid is blank, switch to unbind operation!" — bind 空值语义
- ContextCoreLoader:31-41 SPI 加载 — 上下文存储可替换

## 大域拆分判断

S-5 = 传播定义面 (枚举语义 + 挂起恢复 + 上下文存储); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面); S-1 已实证消费面

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "6种Propagation(REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/SUPPORTS/NEVER/MANDATORY)" | 枚举 6 值全实证 (Propagation:57-176) | **接受** ✅ |
| "挂起语义" | suspend/resume + SuspendedResourcesHolder (L207-240) | **接受** ✅ |
| 对照 Spring | Spring 7 种含 NESTED — Seata 无 (无 savepoint 语义) | **接受+对照** ✅ |
| 数字: 上下文 SPI | ContextCore 2 实现 (ThreadLocal/FastThreadLocal) | **补充** ✅ |
