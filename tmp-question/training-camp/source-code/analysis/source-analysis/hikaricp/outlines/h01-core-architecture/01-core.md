# H-1 核心架构 — HikariConfig → HikariDataSource → HikariPool → ConcurrentBag

> 依赖 S-10 Boot DataSource (引出) + C-11 jdbc DataSource | 🔴 Deep | 6 KP | [模式: 门面 + 懒加载 + 组合]

**读者处境**: Spring Boot 里配个 `spring.datasource.hikari.*`, 注入 DataSource 就是 HikariDataSource — 拿到连接时背后是什么结构?`getConnection()` 第一调用和后续调用差在哪?为什么说 Hikari"快"?

### 1. 入口门面 — HikariDataSource 懒加载池

场景: HikariDataSource 实现了 DataSource — 它自己就是池的入口, 但池是何时真正创建的?

源码路径:
- `HikariDataSource.java:40` — **门面**: `public class HikariDataSource extends HikariConfig implements DataSource, Closeable`(L40) — 本身继承配置, 兼作 DataSource
- `HikariDataSource.java:74,80` — **急建**: `HikariDataSource(HikariConfig configuration)`(L74) → `pool = fastPathPool = new HikariPool(this)`(L80) — 带参构造立即建池
- `HikariDataSource.java:59,92,111` — **懒建**: 无参构造(L59); `getConnection`(L92) → 池为 null 时 `pool = result = new HikariPool(this)`(L111, double-checked) → `result.getConnection()`(L127)

关键设计: **Why 懒加载？** 无参构造(如 Spring Boot 反射创建)只建门面, 首次 getConnection 才真正建池 — 避免没用到就白建池; 带参构造(用户显式 new)立即建, 尽早暴露配置错误。**Why extends HikariConfig？** DataSource 本身承载全部配置, 一个对象既是配置又是门面, 减少对象。[模式: 门面 + 懒加载]

数据流: 无参 new HikariDataSource() → 只初始化配置 → 首次 getConnection(L92) → pool 为 null → L111 new HikariPool(this) → result.getConnection(L127) → 建池后复用 pool。

### 2. 池核心 — HikariPool

场景: 池到底长什么样?为什么能"快"?

源码路径:
- `HikariPool.java:52` — **结构**: `public final class HikariPool extends PoolBase implements HikariPoolMXBean, IBagStateListener`(L52) — 继承连接生命周期基类
- `HikariPool.java:75,92` — **核心容器**: `private final ConcurrentBag<PoolEntry> connectionBag`(L75); 构造 `new ConcurrentBag<>(this)`(L92) — 连接存这(无锁并发容器, H-2 展开)
- `HikariPool.java:118` — **后台维护**: `houseKeepingExecutorService.scheduleWithFixedDelay(new HouseKeeper(), 100L, housekeepingPeriodMs, ...)`(L118) — 定时线程(H-6 展开)
- `HikariPool.java:140,160` — **获取**: `getConnection()`(L140) → `connectionBag.borrow(timeout, MILLISECONDS)`(L160)

关键设计: **Why ConcurrentBag 而非阻塞队列？** Hikari 用无锁并发容器存连接 — borrow/requite 用 ThreadLocal + CAS 减少锁竞争, 这是 Hikari"快"的核心(H-2 深挖); 池聚合容器 + 定时维护, 职责清晰。[模式: 组合 — 池聚合无锁容器]

数据流: HikariDataSource 建 HikariPool(L80/111) → 构造 new ConcurrentBag(L92) + 启动 HouseKeeper(L118) → getConnection(L140) → connectionBag.borrow(L160) → 返回连接(借用细节 H-3)。

### 3. 架构全链路 + 与 Boot 衔接

场景: 四层架构怎么串起来?和 Spring Boot 的 DataSource 什么关系?

源码路径:
- 全链路: `HikariConfig`(配置/校验) → `HikariDataSource`(门面) → `HikariPool`(池) → `ConcurrentBag<PoolEntry>`(无锁容器) → `PoolEntry`(连接条目三态) → `PoolBase`(连接创建/验证, `new PoolEntry(newConnection(...))` L210)
- 衔接: `S-10 Boot DataSource`(s74, 已分析) — `DataSourceAutoConfiguration` 把 HikariDataSource 作为默认 DataSource, 池化内核"深入在阶段3 HikariCP" — 本域正是那部分的深入

关键设计: **Why 分层？** 配置(Config)→门面(DataSource)→池(Pool)→容器(Bag)→条目(Entry)→生命周期(PoolBase) 逐层解耦 — 每层单一职责, 替换点清晰(如换容器/换驱动); 这与 S-10 的自动装配接线衔接: Boot 装配 HikariDataSource, 池的机制在本域展开。[模式: 分层 + 复用边界]

数据流: Spring Boot 装配(DataSourceAutoConfiguration→DataSourceConfiguration.Hikari, S-10) → HikariDataSourceBuilder.build() → `new HikariDataSource(config)`(带参**急建**构造) → 池在 Bean 创建时已建好 → getConnection 走 fastPathPool 快路径 → borrow → 返回代理连接(ProxyConnection, H-12)。用户拿到的是代理, 底层是池管理。

→ 引出 H-13: DriverDataSource — 架构基石: JDBC 驱动的封装(getConnection 底层的连接来源, 池的叶子依赖)。
