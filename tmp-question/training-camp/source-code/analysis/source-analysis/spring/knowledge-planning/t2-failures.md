# T-2 @Transactional 8种失效场景

> 项目: Spring Framework 6.x | 🔴 Deep / 1 篇 | 基于源码机制的实践分析
> 基线: T-1 链路 — 理解完整调用链后才能理解为什么某些场景会失效

---

## §0.8

^- 🔴 Deep，1篇 — 自调用/non-public/异常类型/exclude/多数据源/传播行为/跨线程/非事务引擎 — 8种@Transactional失效场景的根因分析

---

## 01 提取 — 8种失效及根因

| # | 失效场景 | 根因 | 源码验证 |
|:--:|------|------|------|
| 1 | **自调用** `this.method()` | AOP代理只拦截外部调用—this.method()不走代理→TransactionInterceptor.invoke不可能被触发 | TransactionInterceptor.java:112 invoke需MethodInvocation→this调用不经过Proxy |
| 2 | **non-public** 方法 | CGLIB代理生成子类→只能覆写非final非private方法—private方法无法被覆写→代理不存在→AOP无效 | CglibAopProxy: Enhancer只覆写非final/protected/public方法 |
| 3 | **异常类型** 默认RuntimeException | TransactionAttribute.rollbackOn→默认RuntimeException/Error→checked Exception不触发rollback—`@Transactional(rollbackFor=Exception.class)`修复 | RuleBasedTransactionAttribute.java:rollbackOn |
| 4 | **exclude/noRollbackFor** | 相反: @Transactional(noRollbackFor=IllegalArgumentException)→IEA异常时不rollback | 同上 |
| 5 | **多数据源** 选错 TransactionManager | determineTransactionManager→默认PlatformTransactionManager bean→多个DataSource时需@Transactional("orderTm")指定 | TransactionAspectSupport.determineTransactionManager |
| 6 | **传播行为** PROPAGATION_NEVER | PROPAGATION_NEVER在有事务时抛异常—propagation=SUPPORTS在无事务时非事务执行 | handleExistingTransaction→NEVER→IllegalTransactionStateException |
| 7 | **跨线程** new Thread() | TransactionSynchronizationManager用ThreadLocal绑连接—新线程无连接→独立事务(或无事务) | TransactionSynchronizationManager.getResource→per-thread ThreadLocal |
| 8 | **非事务引擎** MyISAM | MySQL MyISAM不支持事务→commit/rollback无效→数据仍然写入 | 数据库层面—非Spring问题但需标注 |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 实践分析域—8种失效场景都是源码机制(代理拦截/ThreadLocal/传播行为)的直接后果。1篇(~40行)覆盖全部8种+根因+检测方法。

**P1 核心 (4对8种根因)** 🔴:

| 根因类 | 失效场景 | 为什么 |
|:--|------|------|
| 代理拦截缺失 | #1自调用 + #2 non-public | **为什么**: 最常见(80%的失效)—AOP代理是@Transactional的前提—没有代理就没有事务 |
| 异常语义错误 | #3 checked异常 + #4 noRollbackFor | **为什么**: 默认rollback策略与开发者预期不一致—默认只回滚RuntimeException—checked异常不回滚 |
| TransactionManager 错误 | #5多数据源 + #6传播行为 | **为什么**: 多数据源需显式指定TM—默认只选第一个PlatformTransactionManager Bean |
| 跨线程/引擎 | #7 new Thread + #8 MyISAM | **为什么**: ThreadLocal绑定连接被线程隔离—MyISAM是数据库层面的约束 |