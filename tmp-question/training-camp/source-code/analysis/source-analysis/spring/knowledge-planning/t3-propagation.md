# T-3 传播行为 — REQUIRED/REQUIRES_NEW/NESTED 等7种机制

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | AbstractPlatformTransactionManager.getTransaction/TransactionDefinition
> 基线: T-1 链路 — getTransaction 中 handleExistingTransaction 处理传播行为

---

## §0.8

- 🟡 Working，1篇 — TransactionDefinition 7种传播常量 + getTransaction 传播逻辑 + handleExistingTransaction 挂起/恢复机制
- 设计模式: [模式: 状态模式]—传播行为决定事务状态转换(NULL→NEW/EXISTING→NESTED/EXISTING→SUSPENDED)

---

## 01 提取 — 7种传播行为

| Propagation | 值 | 无事务时 | 有事务时 | 源码关键操作 |
|:--|:--:|------|------|------|
| REQUIRED | 0 | 创建新事务 | 加入现有事务 | getTransaction: def.getPropagation==REQUIRED→suspend(null)→startTransaction |
| SUPPORTS | 1 | 非事务执行 | 加入现有事务 | 无事务: return null transaction / 有事务: handleExisting→加入 |
| MANDATORY | 2 | **抛异常** | 加入现有事务 | 无事务: throw IllegalTransactionStateException |
| REQUIRES_NEW | 3 | 创建新事务 | **挂起当前**→新事务 | handleExisting: suspend(existing)→startTransaction(new) |
| NOT_SUPPORTED | 4 | 非事务执行 | **挂起当前**→非事务执行 | handleExisting: suspend(existing)→emptyTransaction(no actual transaction) |
| NEVER | 5 | 非事务执行 | **抛异常** | handleExisting: throw IllegalTransactionStateException |
| NESTED | 6 | 创建新事务 | **savepoint + 嵌套事务** | handleExisting: useSavepoint→createAndHoldSavepoint→nested transaction |

**源码验证**:
- `TransactionDefinition.java`: 7个int常量(PROPAGATION_REQUIRED=0~NESTED=6)
- `AbstractPlatformTransactionManager.java:getTransaction(L373)`: 无事务→判断MANDATORY→REQUIRED/REQUIRES_NEW/NESTED→suspend(null)→startTransaction
- `AbstractPlatformTransactionManager.java:handleExistingTransaction`: 有事务→NEVER(抛异常)→NOT_SUPPORTED(挂起)→REQUIRES_NEW(挂起+新事务)→NESTED(savepoint)→REQUIRED/SUPPORTS/MANDATORY(加入)

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 传播行为是源码中 getTransaction 的 switch/case 展开——7种行为共享同一个方法。1篇(~40行)覆盖全部7种+挂起恢复机制。

**P1 核心: 传播行为的四组语义** 🔴 — **为什么**: 7 种传播可归为四组(需要/可选/禁止/独立)— 分组理解比逐个记常量高效 — 每种传播对应 getTransaction 的一个明确分支

| 组 | 传播 | 语义 |
|:--|------|------|
| 需要事务 | REQUIRED/MANDATORY | 必须有事务—MANDATORY无事务直接抛异常 |
| 创建独立事务 | REQUIRES_NEW/NESTED | 挂起当前→新事务—NESTED用savepoint轻量嵌套 |
| 非事务执行 | SUPPORTS/NOT_SUPPORTED/NEVER | 挂起或不参与—NEVER有事务抛异常 |
