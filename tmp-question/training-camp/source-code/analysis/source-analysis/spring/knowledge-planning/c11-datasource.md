# C-11 DataSource — 数据源与池化 (DriverManagerDataSource → HikariDataSource)

> 项目: Spring Framework 6.x + HikariCP | 🟡 Working / 1 篇 | DriverManagerDataSource(158行)+AbstractDriverBasedDataSource(224行)+AbstractDataSource(98行)+AbstractRoutingDataSource(277行)+HikariDataSource(376行)+HikariConfig(1269行)
> 基线: C-10 结尾桥 — 组件索引定"类在哪", 数据源定"连接在哪" — 进入 JDBC 数据访问层; 原始执行计划 6-2

---

## §0.8

- 🟡 Working，1篇 — 抽象(DataSource 接口 + AbstractDataSource) → 非池化(DriverManagerDataSource: 每次 getConnection 新建 DriverManager.getConnection) → 层级(AbstractDriverBasedDataSource: getConnection→getConnectionFromDriver) → 路由(AbstractRoutingDataSource: 按 key 选目标源, 读写分离) → 池化(HikariDataSource: HikariConfig 配置→HikariPool→getConnection 借用)
- 设计模式: [模式: 策略]—DataSource 可替换非池化/池化/路由; [模式: 惰性初始化]—Hikari 首次 getConnection 才建池; [模式: 模板方法]—AbstractDriverBasedDataSource 固定连接获取骨架

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AbstractDataSource.java:40 | 抽象基类 | **DataSource 抽象**: 实现日志/包装等通用, getConnection 留给子类 — 数据源统一入口 | High |
| DriverManagerDataSource.java:67,124,140 | 非池化实现 | **每次新连接**: setDriverClassName(L124)/setUrl/setUsername/setPassword → getConnectionFromDriver(L140): getUrl→getConnectionFromDriverManager(L154: DriverManager.getConnection(url, props)) — **无复用, 每调用建新连接** | High |
| AbstractDriverBasedDataSource.java:34,169 | 连接骨架 | **模板**: getConnection(L169)→getConnectionFromDriver(username, password)(L192) → 子类实现 Driver/DataSource 两种取连接 | High |
| AbstractRoutingDataSource.java:212,123 | 路由源 | **按 key 选源**: getConnection L212→determineTargetDataSource().getConnection; afterPropertiesSet L123 resolve 目标源 — 读写分离/多数据源 | High |
| HikariConfig.java:256,519 | 池配置 | **builder 风格**: setMaximumPoolSize(L256)/setMinimumIdle(L273)/setJdbcUrl(L519)/setUsername/setDriverClassName/setPoolName — 配置即构建 | High |
| HikariDataSource.java:80,92 | 池化实现 | **池入口**: HikariDataSource(HikariConfig) L80: pool=fastPathPool=new HikariPool(this); getConnection L92: isClosed→fastPathPool.getConnection(借用) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 抽象+非池化+路由+池化约 2400 行 — 知识主线: "DataSource 抽象 → 两种策略(非池化/池化) → 路由". 1篇 (~45行) 按"抽象→非池化→池化→对比"展开; 若分 2 篇则 Spring 侧与 Hikari 侧割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | DriverManagerDataSource 非池化语义 (每次 getConnection 新建) | 🔴 | **为什么🔴**: 理解"非池化"基线 — 与池化对比, 是数据源选型的起点 |
| P1-2 | HikariDataSource 池化 (HikariConfig→HikariPool→getConnection 借用) | 🔴 | **为什么🔴**: 生产默认(Boot 3) — 连接复用的核心机制 |
| P1-3 | AbstractDriverBasedDataSource 连接骨架 (getConnection→getConnectionFromDriver) | 🔴 | **为什么🔴**: Spring 数据源层级 — Driver/DataSource 两种取连接策略的统一 |
| P2-1 | AbstractRoutingDataSource (按 key 选目标源) | 🟡 | **为什么🟡**: 读写分离/多数据源的实现基础 |
| P2-2 | DataSource 抽象与替换 (非池化/池化/路由三策略) | 🟡 | **为什么🟡**: 为何业务代码只依赖 DataSource 接口 |
| P3-1 | HikariConfig builder 配置 (maximumPoolSize/minimumIdle) | 🟢 | **为什么🟢**: 池参数语义 — 调优基础 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **抽象与骨架** (DataSource + AbstractDriverBasedDataSource) | 🔴 | 连接获取的统一入口与模板 |
| B | **非池化 vs 池化** (DriverManagerDataSource vs Hikari) | 🔴 | 两种策略的对比核心 |
| C | **路由与配置** (AbstractRoutingDataSource + HikariConfig) | 🟡 | 多源路由与池调优 |

> **Cluster A (§1)**: DataSource 抽象 + DriverManagerDataSource(非池化) + AbstractDriverBasedDataSource 骨架
> **Cluster B (§2)**: HikariDataSource(HikariConfig→HikariPool→借用) 池化机制
> **Cluster C (§3)**: AbstractRoutingDataSource 路由 + 三策略选型对比(测试/简单→DriverManager, 生产→Hikari, 多源→Routing)

→ 引出 7-5: 拦截器 — 数据访问层结束, 回到 Web MVC — HandlerInterceptor 三方法(preHandle/postHandle/afterCompletion)与执行顺序

(End of file - total 61 lines)
