# 闭环笔记 q5: Transaction 模板 — AbstractPlatformTransactionManager 的 Redisson 实现

## 假设
RedissonTransactionManager extends AbstractPlatformTransactionManager (Spring 事务抽象), 用模板方法 doBegin/doCommit/doRollback 把 Redisson 事务接入 Spring @Transactional。

## 验证过程
- RedissonTransactionManager (transaction/.../RedissonTransactionManager.java:37): `extends AbstractPlatformTransactionManager implements ResourceTransactionManager`
- 模板方法:
  - doBegin (L73-76): 事务开始 → TransactionHolder 建立 Redisson 事务
  - doCommit (L92-95): `transactionHolder.getTransaction().commit()`
  - doRollback (L102-105): `transactionHolder.getTransaction().rollback()`
  - getTransaction (L52): 事务对象获取
  - isExistingTransaction (L69): 嵌套事务判定
- 复用 Spring 事务框架: 传播行为/隔离/回滚规则全由 AbstractPlatformTransactionManager 处理, Redisson 只实现 3 个模板方法
- RedissonTransactionObject: 事务状态载体 (TransactionHolder)

## 代码类型
Implementation (模板方法) — Spring 事务抽象的实现

## 跨域关联
- s29-tx-chain (Spring 事务抽象) → AbstractPlatformTransactionManager 框架
- RD-4 (命令) → Redisson 事务基于命令编排
- 面试点: "怎么让 @Transactional 管 Redis?"

## 结论
Transaction 集成 = 模板方法: extends AbstractPlatformTransactionManager (继承传播/隔离/回滚框架), 只实现 doBegin/doCommit/doRollback 三方法把 Redisson 事务接入。Spring @Transactional 即可管理 Redis 操作。
源码位置: RedissonTransactionManager.java:37-105