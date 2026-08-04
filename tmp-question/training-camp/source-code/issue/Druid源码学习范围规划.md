# Druid 源码学习范围规划

> **版本**: v1.2.27
> **仓库**: `/data/workspace/source-code/code/spring/druid/`
> **规模**: core 模块 1614 个源文件（SQL 解析占 1238 文件/76%），7 个子模块
> **日期**: 2026-08-03

---

## 一、仓库概况

Druid 是阿里巴巴开源的 JDBC 连接池 + SQL 监控组件。与 HikariCP 不同，Druid 不仅提供连接池，还内置了 SQL 解析器（完整 AST，支持多方言）、SQL 防火墙（WallFilter）、SQL 统计监控（StatFilter）和 Web 管理控制台。

**子模块清单**（7 个，全部审计）：

| 子模块 | 文件数 | 职责 | 状态 |
|---|---|---|---|
| `core/` | 1614 | 核心：pool(72) + filter(29) + sql解析(1238) + wall(46) + proxy(36) + stat(24) + util(35) + support(105) + root(7) | ✅ 已探索 |
| `druid-admin/` | 13 | Web 管理控制台（内置 Jetty） | 淘汰 |
| `druid-spring-boot-3-starter/` | 9 | Spring Boot 3 自动装配 | 🟡 略读 |
| `druid-spring-boot-starter/` | 8 | Spring Boot 2 自动装配 | 淘汰 |
| `druid-wrapper/` | 13 | 早期系统的 Wrapper 适配 | 淘汰 |
| `druid-demo-petclinic/` | — | Demo 项目 | 淘汰 |

**core 包结构深探**：

| 包 | 文件数 | 职责 |
|---|---|---|
| `pool/` | 72 | 连接池核心：DruidDataSource(3979行) + DruidAbstractDataSource(2388行) + DruidPooledConnection(1298行) |
| `pool/ha/` | 3 | 高可用数据源（淘汰） |
| `pool/vendor/` | 8 | 厂商适配：Informix/Sybase 等异常排序器 |
| `pool/xa/` | 4 | XA 分布式事务支持（淘汰） |
| `filter/` | 29 | Filter 拦截器链体系（Filter→FilterAdapter→FilterEventAdapter→FilterChainImpl） |
| `filter/config/` | 3 | 配置解密 Filter |
| `filter/encoding/` | 2 | 编码转换 Filter |
| `filter/logging/` | 5 | SQL 日志 Filter |
| `filter/stat/` | 10 | SQL 统计 Filter（StatFilter） |
| `sql/` | 1238 | 完整 SQL 解析器（AST 422 + 方言 713 + Parser 23 + Visitor 54 + 其他） |
| `wall/` | 46 | SQL 防火墙（WallFilter + 各数据库 SPI） |
| `proxy/` | 36 | JDBC 代理对象（DataSource/Connection/Statement/PreparedStatement/CallableStatement/ResultSet） |
| `stat/` | 24 | 统计数据结构（JdbcDataSourceStat / JdbcSqlStat 等） |
| `support/` | 105 | 外部集成：HTTP/JSON/Log4j/Spring 等（淘汰仅留 StatViewServlet） |
| `util/` | 35 | 工具类 |

---

## 二、知识域规划

### 🔴 核心域（5 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| D-1 | **DruidDataSource 连接池核心** | DruidDataSource(3979行), DruidAbstractDataSource(2388行), DruidConnectionHolder(477行) | **与 HikariCP 架构对比**：ReentrantLock + Condition(notEmpty/empty) 阻塞等待模型 vs HikariCP ConcurrentBag 无锁模型；固定大小数组 `connections[maxActive]` 预分配；**init() 启动链路**：SPI 加载→Driver 解析→Filter 初始化→`asyncInit` 异步/同步创建 initialSize 个连接→`createAndLogThread`+`createAndStartCreatorThread`+`createAndStartDestroyThread` 三线程启动+`initedLatch` 同步等待→mBean 注册；**获取流程**：`getConnection()` → FilterChainImpl 拦截链 → `getConnectionDirect()` → `getConnectionInternal()`：① 检查 closed/enable/disableException ② **`createDirect` 优化**：池空时若 createScheduler 有排队任务，等待线程 CAS `creatingCountUpdater` 自己直接建连，跳过线程调度开销 ③ **`maxWaitThreadCount` 限流**：超过阈值直接拒绝防惊群 ④ **`onFatalError` 保护**：数据库致命错误时限制 `onFatalErrorMaxActive` 活跃连接数 ⑤ `pollLast()` → notEmpty.await() 阻塞 ⑥ 取出后检查 `holder.discard` 重试 → 超时抛丰富异常（active/maxActive/creating/createElapseMillis）；**归还流程**：`recycle()` → rollback 未提交事务→`holder.reset()` 重置 4 状态（readOnly/holdability/isolation/autoCommit）+清空事件监听器+关闭 traced statements→`phyMaxUseCount`/密码版本检查→`testOnReturn` 验证→`putLast()` 放回数组+`notEmpty.signal()`；**shutdown()**：interrupt 三个线程+cancel createSchedulerFutures+close 所有 pooling 连接 Statement Pool+close 物理连接+unregisterMBean+signal notEmpty 唤醒等锁者 |
| D-2 | **Filter 拦截链体系** | Filter 接口, FilterAdapter, FilterEventAdapter, FilterChainImpl(~3000行), DruidConnectionHolder | **Druid 独特设计**：每个 JDBC 操作通过 Filter 链代理——`getConnection()` → `FilterChainImpl.dataSource_connect()` → 每个 Filter 调用 `nextFilter().xxx()` 形成递归链；`FilterChainImpl.pos` 位置指针+`filterSize` 控制链进度；`FilterEventAdapter` 提供事件回调模板（connection_connectBefore/After）；**FilterChainImpl 对象池复用**：`DruidConnectionHolder.createChain()`/`recycleFilterChain()` 缓存 FilterChainImpl，避免每次操作都 new（per-connection 缓存）；连接池内置 5 类 Filter（Stat/Wall/Config/Logging/Encoding）；`DruidPooledConnection.close()` 中有/无 Filter 两条路径 |
| D-3 | **StatFilter SQL 监控** | StatFilter, JdbcDataSourceStat, JdbcSqlStat | SQL 全生命周期统计：执行次数/错误次数/总耗时/最大耗时/读取行数/更新行数/并发数/直方图；**SQL 参数化合并**：`ParameterizedOutputVisitorUtils` 将 `WHERE id=123` 合并为 `WHERE id=?`；**慢 SQL 检测**：`slowSqlMillis` 阈值（默认 3000ms）+ 日志输出；按 DataSource→SQL→表三级聚合统计 |
| D-4 | **WallFilter SQL 防火墙** | WallFilter, WallCheckVisitor, WallProvider(WallProvider_MySQL/Oracle/PG等) | SQL 注入防护四阶段：SQL AST 解析→Visitor 遍历 AST 节点→规则检查→违规拦截/记录；规则体系：表白名单、函数调用限制、语句类型限制（SELECT/UPDATE/DELETE）、条件必须有等；WallViolation 记录违规详情+SQL 片段 |
| D-5 | **连接池维护体系** | shrink()(核心), DestroyTask, CreateConnectionTask, LogStatsThread | **shrink() 是维护核心**（~120行），DestroyTask 运行 `shrink(true, keepAlive)` + `removeAbandoned()`：**四阶段维护**：① fatalError 处理（`fatalErrorCount` 增量检测，fatalError 后创建的老连接全部标记淘汰）；② 空闲驱逐 checkTime 模式（`phyTimeoutMillis` 物理超时→`minEvictableIdleTimeMillis` 逐出前 checkCount→`maxEvictableIdleTimeMillis` 全部逐出）；③ keepAlive 检测（`idleMillis >= keepAliveBetweenTimeMillis` + `lastKeepTimeMillis` 去重→`keepAliveConnections[]` 收集→后续 validationQuery 验证）；④ System.arraycopy 紧凑化数组；**CreateConnectionTask**：Scheduler 定时+多线程并行创建（`createSchedulerFutures` Map 管理），失败指数退避重试，`poolingCount >= notEmptyWaitThreadCount` 停止；**removeAbandoned()**：`removeAbandonedTimeoutMillis` 超时未归还的连接强制回收（WARN "not use in production"）；**LogStatsThread**：按 `timeBetweenLogStatsMillis` 定时输出池统计 |

### 🟡 扩展域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| D-6 | **SQL Parser 体系架构** | SQLStatementParser, SQLExprParser, SQLASTVisitor, SchemaRepository, DbType | 体系结构概览（不深入 1238 文件实现）：Parser 层（23 文件，Lexer→Token→AST 节点）→ AST 层（422 文件，SQLStatement/SQLExpr/SQLTableSource 等）→ Visitor 层（54 文件，OutputVisitor/SchemaStatVisitor/WallCheckVisitor 等）→ Dialect 层（713 文件，MySQL/Oracle/PG/SQL Server/DB2/Odps/SQLite/H2/Phoenix 9 种方言）；被 StatFilter 用于 SQL 参数化，被 WallFilter 用于安全检查 |
| D-7 | **连接验证与健康检查** | ValidConnectionChecker, JDBC4ValidConnectionChecker, MySQLValidConnectionChecker, OracleValidConnectionChecker | testWhileIdle/testOnBorrow/testOnReturn 三种验证时机；ValidConnectionChecker SPI 4 种实现：JDBC4 `isValid()`、MySQL `SELECT 1` ping、Oracle `SELECT 'x' FROM DUAL` ping、PG `SELECT 1` ping；`validationQuery` 兜底配置 |
| D-8 | **PreparedStatementPool** | PreparedStatementPool, PreparedStatementHolder | per-connection 的 LRU 缓存 PreparedStatement 对象；maxPoolPreparedStatementPerConnectionSize(默认10) 控制上限；避免重复 SQL 解析的开销 |
| D-9 | **Spring Boot 3 Starter** | DruidDataSourceAutoConfigure, DruidStatProperties | 自动装配链路：`@EnableConfigurationProperties(DruidStatProperties)` → `@ConditionalOnClass` 检查 → `@Bean DruidDataSourceWrapper` 绑定 `spring.datasource.druid.*` → StatViewServlet + WebStatFilter 自动注册 |

---

## 三、淘汰清单

| 子模块/包 | 文件数 | 理由 |
|---|---|---|
| `druid-admin/` | 13 | Web 监控控制台，独立管理页面，非连接池学习核心 |
| `druid-wrapper/` | 13 | 老系统 Wrapper 适配器，过时 |
| `druid-demo-petclinic/` | — | Demo 项目 |
| `druid-spring-boot-starter/` | 8 | Spring Boot 2 版本，项目用 Boot 3 |
| `pool/ha/` HighAvailableDataSource | 3 | HA 高可用场景，部署层面非连接池 |
| `pool/xa/` | 4 | XA 分布式事务，JTA 集成用，面试低频 |
| `pool/vendor/` | 8 | 厂商特定异常排序器（Informix/Sybase/PG/MSSQL），按需查阅 |
| `pool/DruidDataSourceC3P0Adapter` | 2 | C3P0 兼容适配器，过时框架迁移用途 |
| `filter/config/` ConfigFilter | 3 | 配置文件密码解密，边缘功能 |
| `filter/encoding/` | 2 | 编码转换，边缘功能 |
| `filter/logging/` | 5 | SQL 日志输出，用 StatFilter 慢 SQL 日志覆盖 |
| `sql/` 包内部实现（~1200+ 文件） | 1238 | 完整 SQL 解析器内部，D-6 🟡 仅学架构概览，不深入 AST 生成/Lexer/Parser 算法 |
| `support/` 外部集成（~100 文件） | 105 | Log4j/Logback/HTTP/JSON/Spring MVC 等集成适配，仅 StatViewServlet 留用 |
| `mock/` | 若干 | 单元测试 Mock 数据源 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 5 |
| 🟡 扩展域 | 4 |
| **总域** | **9** |
| 淘汰子模块/包 | 13 个 |

---

## 五、Druid vs HikariCP 架构对比要点

| 维度 | HikariCP (v7.0.2) | Druid (v1.2.27) |
|---|---|---|
| 连接存储 | ConcurrentBag（无锁 ThreadLocal+CAS） | 固定数组 + ReentrantLock + Condition |
| 连接获取 | borrow() 无锁轮询 → parkNanos 等待 | pollLast() → notEmpty.await() 阻塞 |
| 拦截器 | 无（代理类直接 JDBC 修改） | Filter 链拦截所有 JDBC 操作 |
| SQL 监控 | 无（需外部 Micrometer 指标） | 内置 StatFilter：SQL 统计 + 慢 SQL + 参数化 |
| SQL 安全 | 无 | 内置 WallFilter：SQL 注入防护 |
| SQL 解析 | 无 | 完整 SQL AST 解析器（多方言） |
| 大小 | 48 文件 / 9200 行 | 1614 文件（pool 72 文件 / ~8000 行） |
| 性能哲学 | "极致精简" | "功能全面" |

**学习价值**：Druid 和 HikariCP 是连接池设计的两极——HikariCP 追求零开销的极致性能，Druid 追求全功能的监控和安全。对比学习可深入理解连接池的设计权衡。

---

## 六、学习顺序建议

```
D-1 连接池核心（理解 Lock+Condition 模型+借还流程+shutdown）
  → 对比 HikariCP H-1~H-4（理解两种池设计）
    → D-2 Filter 拦截链（理解 Druid 独特扩展点）
      → D-3 StatFilter（理解 SQL 监控如何实现）
        → D-5 shrink() 维护体系（理解 keepAlive/驱逐/removeAbandoned）
          → D-4 WallFilter + D-6 SQL Parser 概览（按需）
```

以上规划完成，共 **5🔴+4🟡=9 域**，等你确认。
