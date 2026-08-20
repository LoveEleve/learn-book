# RD-7 篇3 — data-tx: RedisTemplate 连接适配与事务

> 前置: [[RD-7-篇1]] (starter) + [[s29-tx-chain]] (Spring 事务抽象) | 复用: [[rd4-command]] (命令执行) | 对照: [[s75-boot-redis]] (Lettuce/Jedis 连接) | 引出: [[rd8-basic]]
> 🟡 B | 2 KP | [模式: 适配层 + 模板方法 + 版本隔离]
> Pass 2 闭环: q4(data 适配) q5(Transaction) q6(版本矩阵)

**读者处境**: 你的 `RedisTemplate.opsForValue().set("k","v")` 和 `@Transactional` 方法 — 怎么让它们跑在 Redisson 上?Spring Data Redis 的 RedisConnection 接口有几十个方法, Redisson 怎么全实现?这篇拆 spring-data 适配层 (RedissonConnectionFactory/RedissonConnection 映射 Redisson 命令) 和事务集成 (AbstractPlatformTransactionManager 三模板方法), 以及 18 个版本子模块的编译期隔离。

### 概念依赖链
q4(data 适配) ← q5(Transaction) ← q6(版本矩阵) — 先讲 RedisTemplate 的连接适配, 再讲事务模板, 最后讲版本隔离。

### 核心悬念
"RedisTemplate 的几十个操作, 怎么全映射到 Redisson?@Transactional 怎么管 Redis?"

### 叙事顺序
1. 问题引入: RedisTemplate + @Transactional 的 Redisson 化
2. data 适配 (q4) — ConnectionFactory + 操作映射
3. Transaction (q5) — AbstractPlatformTransactionManager 模板
4. 版本矩阵 (q6) — 18 子模块编译期隔离
5. 收束: "四模块合体 = 完整 Spring 生态"

### 1. data 适配 — RedisTemplate 的 Redisson 后端

场景: RedisTemplate 操作怎么变 Redisson 命令?
源码路径:
- `RedissonConnectionFactory implements RedisConnectionFactory, ReactiveRedisConnectionFactory` (data-26/RedissonConnectionFactory.java:49-50)
  - getConnection (RedissonConnectionFactory.java:118) → RedissonConnection
  - getClusterConnection (RedissonConnectionFactory.java:126) / Sentinel
- `RedissonConnection` (RedissonConnection.java): get (RedissonConnection.java:516) / set (RedissonConnection.java:535-540) / setNX (RedissonConnection.java:568) / setEx (RedissonConnection.java:575) / getRange (RedissonConnection.java:646) / setBit (RedissonConnection.java:663) — **Spring Data Redis 操作集**
- execute(command, args) (RedissonConnection.java:178-201): 反射式命令分发 — 遍历 getDeclaredMethods 找匹配 command 名的 public 方法 (参数长度匹配); 失败 → RedisPipelineException (pipelined) / InvalidDataAccessApiUsageException (普通)
- 同步 + Reactive 双实现 (RedisTemplate 和 reactive 都能用)
关键设计 (q4): 适配层 = 实现 RedisConnectionFactory 接口, RedissonConnection 逐操作映射到 Redisson 命令。[模式: 接口适配]
数据流: RedisTemplate.set → RedissonConnection.set → Redisson 命令 → Redis。

### 2. Transaction — @Transactional 管 Redis

场景: Spring 事务怎么接 Redisson 事务?
源码路径:
- `RedissonTransactionManager extends AbstractPlatformTransactionManager` (RedissonTransactionManager.java:37)
- 模板方法:
  - doBegin (RedissonTransactionManager.java:73-76): 建立 TransactionHolder
  - doCommit (RedissonTransactionManager.java:92-95): `transaction.commit()`
  - doRollback (RedissonTransactionManager.java:102-105): `transaction.rollback()`
  - getTransaction (RedissonTransactionManager.java:52) / isExistingTransaction (RedissonTransactionManager.java:69): 嵌套判定
- 复用 Spring 框架: 传播/隔离/回滚规则全继承, 只实现 3 方法
关键设计 (q5): 模板方法: 继承 Spring 事务框架, 只实现 doBegin/Commit/Rollback 接入 Redisson 事务。[模式: 模板方法]
数据流: @Transactional → TM.doBegin → Redisson 事务 → 业务 → doCommit/doRollback。

### 3. 版本矩阵 — 18 子模块编译期隔离

场景: 为什么 spring-data 有 18 个子模块?
源码路径:
- 结构: redisson-spring-data/data-16~41 (18 子模块), 每版 14-56 文件
- 每版核心: RedissonConnectionFactory/RedissonConnection/RedissonSubscription
- 为什么分版: Spring Data Redis API 随版本演进 → 编译期隔离 (每版编译针对其 API)
- 版本对应: 16~27 → SD Redis 2.x; 30~35 → 3.x; 40/41 → 4.x
关键设计 (q6): 版本隔离 = 编译期适配每版 API, 避免运行时版本判断/冲突。[模式: 多版本编译隔离]
数据流: 用户依赖 data-26 → 编译用 SD Redis 2.x API → 运行匹配。

### 负面空间 — data/tx 刻意不做的事

- **不运行时版本适配**: 版本在编译期定死 (无反射版本分发)
- **不实现全部响应式**: Reactive 适配依赖 Spring Data 版本支持
- **不跨版本混合**: 一个项目只能一个 data 版本 (编译期隔离的代价)
- **不做事务传播增强**: 传播规则全默认 (AbstractPlatformTransactionManager)
- **事务不覆盖 Redis 外资源**: @Transactional 只管 Redisson 事务, 不跨 DB/Redis

→ 引出: 基础数据结构 (RBucket/AtomicLong) 怎么消费命令层?→ [[rd8-basic]]