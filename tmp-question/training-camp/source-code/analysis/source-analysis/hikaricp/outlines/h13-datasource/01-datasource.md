# H-13 DriverDataSource — JDBC 驱动封装 (池的连接来源)

> 依赖 C-11 jdbc DataSource (复用) | 🟡 Working | 6 KP | [模式: 门面/适配 + 多级解析]

**读者处境**: 连接池最后还是要从 JDBC 驱动拿原始连接 — DriverDataSource 就是"把 Driver 包成 DataSource"的桥。它怎么定位/实例化驱动?getConnection 底层做什么?

### 1. Driver 解析策略 — 怎么拿到 JDBC 驱动

场景: 配了 jdbcUrl + driverClassName, 池需要一个能产连接的 Driver — 怎么找到/实例化它?

源码路径:
- `DriverDataSource.java:40,50` — **桥**: `implements DataSource`(L40); 构造 `DriverDataSource(jdbcUrl, driverClassName, properties, username, password)`(L50)
- `DriverDataSource.java:64,68` — **className 优先**: 有 driverClassName → 遍历 `DriverManager.getDrivers()`(L68) 找已注册的匹配驱动
- `DriverDataSource.java:81,91,100` — **classloader 回退**: 已注册找不到 → 从 **Thread 上下文类加载器**(L81)/**HikariConfig 类加载器**(L91) loadClass → `getDeclaredConstructor().newInstance()`(L100) 直接实例化
- `DriverDataSource.java:112` — **jdbcUrl 回退**: 无 className 或实例化失败 → `DriverManager.getDriver(jdbcUrl)`(L112) 按 URL 解析

关键设计: **Why 多级解析？** 驱动可能已注册(DriverManager)、可能只在特定 classloader 里(插件/TCCL)、可能只能靠 URL 让 DriverManager 匹配 — 多级回退保证最大兼容; 直接 newInstance 避免依赖 DriverManager 自动注册时序问题。[模式: 多级解析 + 回退]

数据流: 构造(L50) → 有 driverClassName? → 是: DriverManager.getDrivers 找(L68)→ 找不到 → TCCL.loadClass(L81)→HikariConfig CL(L91) → newInstance(L100) → driver 就绪; 无 className → DriverManager.getDriver(jdbcUrl)(L112)。

### 2. getConnection 委托 — driver.connect

场景: DataSource.getConnection() 底层做什么?怎么带上用户名密码?

源码路径:
- `DriverDataSource.java:127` — **无参**: `driver.connect(jdbcUrl, driverProperties)`(L127) — 直接委托给已解析的 driver
- `DriverDataSource.java:133,144` — **带参**: `(Properties) driverProperties.clone()`(L133) → 注入 `USER`(L135)/`PASSWORD`(L141) → `driver.connect(jdbcUrl, cloned)`(L144)
- 边界: 未实现方法(LogWriter/unwrap 等)抛 `SQLFeatureNotSupportedException`(L150/180) — 薄桥只做产连接

关键设计: **Why 直接 delegate 而非管理？** DriverDataSource 只负责"用 driver 产原始连接", 不缓存/不管理 — 连接管理由池(H-2~H-5)负责; 薄桥职责单一。**Why clone props？** 带参 getConnection 不改共享的 driverProperties, 克隆后注入临时用户名/密码, 避免并发污染。[模式: 委托 + 隔离]

数据流: getConnection()(L127) → driver.connect(jdbcUrl, driverProperties) → 返回原始 JDBC Connection。带参版(L133-144): clone props + 注入 USER/PASSWORD → connect。

### 3. 与池衔接 + 边界 — PoolBase 的连接来源

场景: DriverDataSource 在池里扮演什么角色?和 Spring 的 DataSource 什么关系?

源码路径:
- 衔接: `PoolBase` 持有 DataSource, `newConnection()` 时从它拿原始连接(池的叶子依赖, H-5 展开) — DriverDataSource 就是那个 DataSource
- 前置: C-11(jdbc DataSource)/s34-s36(jdbc template) — 已覆盖 JDBC 驱动/DataSource 内核, 本域只讲 Hikari 的驱动桥
- 与 S-10: Boot 通常直接用第三方驱动池(Tomcat/Hikari), 但 DriverDataSource 是"无连接池时的纯驱动 DataSource"备选

关键设计: **Why 单独成域？** driver→Connection 是池的**基石依赖**(H-13 是 H-2/H-5 的叶子), 先讲清连接从哪来, 后续 borrow/建连才有着落; 机制在 C-11 复用, 本域只讲 Hikari 的桥接。[模式: 基石 + 复用边界]

数据流: HikariPool 需要新连接(H-5) → PoolBase 用 DataSource(=DriverDataSource) → driver.connect(jdbcUrl, props)(L127) → 原始 Connection → 交给池管理(代理包装 H-12)。

→ 引出 H-2: ConcurrentBag — driver 之后: 无锁并发容器(borrow/requite/PoolEntry 三态), 池"快"的秘密。
