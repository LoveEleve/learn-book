# C-11 DataSource — 数据源与池化 (抽象 → 非池化 → 池化 → 路由)

> 依赖 C-10 ClassPathIndex | 🟡 Working | 6 KP | [模式: 策略 + 惰性初始化 + 模板方法]

**读者处境**: `@Autowired DataSource` — 为什么是接口？DriverManagerDataSource 和 HikariDataSource 什么区别？生产为什么必须用池？读写分离怎么做？

### 1. DataSource 抽象 + DriverManagerDataSource 非池化

场景: 简单测试/一次性脚本里 `new DriverManagerDataSource(url, user, pass)` — 每次 getConnection 都新建一个物理连接 — 无复用。这是"非池化"基线, 理解它才能懂池化的价值。

源码路径:
- `AbstractDataSource.java:40` — **抽象基类**: 实现通用逻辑, getConnection 留给子类 — 业务代码只依赖 DataSource 接口
- `DriverManagerDataSource.java:67,124` — **非池化实现**: setDriverClassName(L124)/setUrl/setUsername/setPassword — builder 式配置
- `DriverManagerDataSource.java:140,154` — **取连接**: getConnectionFromDriver(L140): getUrl→getConnectionFromDriverManager(L154: `DriverManager.getConnection(url, props)`) — **每调用新建物理连接**
- `AbstractDriverBasedDataSource.java:34,169,192` — **骨架**: getConnection(L169)→getConnectionFromDriver(username, password)(L192) — 参数校验/日志统一, 取连接下放

关键设计: **Why 每次 getConnection 都新建？** DriverManagerDataSource 定位是"无连接管理"的简单实现 — 建连成本(网络握手/认证)每次都付; 适合测试、低并发、或由上层(如 JTA)管理连接。生产高并发下建连会成为瓶颈 — 于是需要池。[模式: 策略 — 无状态直连]

数据流: dataSource.getConnection() → AbstractDataSource/AbstractDriverBasedDataSource → getConnectionFromDriver → DriverManager.getConnection(url, props) → 新的物理 Connection(每次!)。用完 conn.close() → 真正关闭(无池回收)。

### 2. HikariDataSource — 池化 (配置 → 池 → 借用)

场景: 生产 `spring.datasource.hikari.maximum-pool-size=10` — 连接复用: 池里维护一批, getConnection 借用、close 归还 — 避免每次建连。

源码路径:
- `HikariConfig.java:256,519` — **池配置**: setMaximumPoolSize(L256)/setMinimumIdle(L273)/setJdbcUrl(L519)/setUsername/setDriverClassName/setPoolName — fluent 配置对象
- `HikariDataSource.java:46,80` — **池初始化**: HikariDataSource(HikariConfig) L80: `pool = fastPathPool = new HikariPool(this)` — 配置→建池; fastPathPool 是"构造即建"的优化路径
- `HikariDataSource.java:92` — **getConnection()**: isClosed 检查 → fastPathPool.getConnection()(L98-99, 从池借用连接) / 空构造时 L111 懒建池
- 连接生命周期: 池内连接用后 close() 实际是**归还池**(Hikari 代理 Connection), 而非物理关闭

关键设计: **Why 连接要池化？** 建物理连接开销大(握手/认证/会话) — 池预建/复用小部分连接, 吞吐量数量级提升; maximumPoolSize 上限防耗尽, minimumIdle 保底。**Why 惰性/构造建池？** 配置驱动: 有配置立即建池(HikariConfig 构造), 空构造首次 getConnection 才建 — 两者都避免"未配置就建池"。[模式: 对象池 + 惰性初始化]

数据流: `new HikariDataSource(hikariConfig)` → new HikariPool(this): 读 HikariConfig(jdbcUrl/user/poolSize) → 预建 minIdle 条连接 → getConnection() → fastPathPool.getConnection(): 池有 idle→借用(标记 busy) / 无 idle 且未满→新建 / 满→等待 → 业务用完 conn.close() → Hikari 代理归还池(非物理关闭)。

### 3. AbstractRoutingDataSource + 三策略选型

场景: 读写分离 — 写库/读库两个 DataSource, 按事务/请求上下文切换 — 路由数据源代理分发。

源码路径:
- `AbstractRoutingDataSource.java:212,123` — **路由源**: getConnection L212→`determineTargetDataSource().getConnection()`; afterPropertiesSet L123 resolve 目标源(目标 key→DataSource 映射) — 子类覆写 determineCurrentLookupKey() 决定当前用哪个源
- 对比: **DriverManagerDataSource**(无池, 简单/测试) vs **HikariDataSource**(池化, 生产默认, Boot 3 内置) vs **AbstractRoutingDataSource**(多源路由, 读写分离/多租户)

关键设计: **Why 三策略都是 DataSource 接口实现？** 业务代码 @Autowired DataSource 依赖接口 — 换池/换路由不动业务代码(策略模式); 路由源本身再包装一个真实池(如 Hikari), 实现"路由+池化"叠加。[模式: 策略 + 代理]

数据流: 读写分离: @Transactional(readOnly=true) → RoutingDataSource.getConnection → determineCurrentLookupKey()(事务属性→"slave") → resolvedDataSources["slave"] → 底层 HikariDataSource.getConnection → 读库连接。普通写 → key="master" → 写库。

→ 引出 7-5: 拦截器 — 数据访问层结束, 回 Web MVC: HandlerInterceptor 三方法(preHandle/postHandle/afterCompletion)与 DispatcherServlet 执行顺序。
