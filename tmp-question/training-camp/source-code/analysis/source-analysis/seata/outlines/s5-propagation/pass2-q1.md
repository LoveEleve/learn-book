# 闭环笔记 q1: 传播枚举 — 6 值语义 + 对照 Spring

## 假设
传播 = 事务进入时的参与策略; Seata 6 值无 NESTED。

## 验证过程
- **枚举 6 值** (Propagation:57-176): **REQUIRED / REQUIRES_NEW / NOT_SUPPORTED / SUPPORTS / NEVER / MANDATORY** — 每值带 Javadoc 伪代码 (参与/挂起/抛异常决策)
- **语义**:
  - REQUIRED: 有事务 join, 无则新建 (L57)
  - REQUIRES_NEW: **有则挂起 + 新建独立事务** (L90, 伪代码 try/finally resume)
  - NOT_SUPPORTED: **有则挂起 + 无事务执行** (L115)
  - SUPPORTS: 有则 join 无则直跑 (L136)
  - NEVER: **有则抛** "existing transaction" (L156)
  - MANDATORY: **无则抛** "not existing transaction" (L176)
- **对照 Spring 7 种**: Seata **无 NESTED** (Spring NESTED 基于 savepoint 内嵌 — Seata 无此语义, S-4 已证 savepoint 仅记录)
- **配置载体** (TransactionInfo:27-84): timeOut / **propagation** / lockRetryInterval / lockRetryTimes — @GlobalTransactional 注解 → TransactionInfo (S-9 交叉)

## 代码类型
Data (枚举语义)

## 跨域关联
- S-1: TransactionalTemplate switch 消费 (L66-113)
- S-9: @GlobalTransactional propagation 参数 → TransactionInfo
- S-4: savepoint 面 (无 NESTED 的底层原因)

## 结论
传播 6 值全实证; 无 NESTED (对照 Spring); 配置经 TransactionInfo 传递。
源码位置: Propagation.java:57-176; TransactionInfo.java:27-84
