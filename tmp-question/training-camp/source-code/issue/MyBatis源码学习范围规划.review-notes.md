# MyBatis 源码学习范围规划复盘与修订路线图

> 复盘基线：MyBatis 3.5.16，源码 `/data/workspace/source-code/code/spring/mybatis/`。
> 本文不是正文大纲，而是按照“先模块扫描，再按机制重组；补齐失败路径、测试证据、集成边界”的方法论，对原 5 域规划做卷级复盘。

## 一、复盘结论

原规划抓住了 MyBatis 的主执行链：

```text
SqlSession -> Executor -> StatementHandler -> ResultSetHandler
```

也抓住了 MapperProxy、动态 SQL、插件和缓存几个高价值主题。但原规划把以下内容直接放入淘汰清单，缺少机制闭环审查：

- `transaction/`：事务边界、提交/回滚、Session 关闭时的资源收束不是工具细节。
- `cursor/`：Cursor 是增量消费 ResultSet 的资源所有权协议，不能因为使用频率低就整体淘汰。
- `type/`：参数写入和结果读取都依赖 TypeHandler；它连接 Java 类型、JDBC 类型和数据库值。
- `reflection/`：MetaObject、Reflector、ObjectWrapper 直接支撑参数取值、结果对象写入、自动映射和延迟加载。
- `annotations/`：注解 Mapper 是 XML Mapper 的另一条配置入口，必须作为桥接证据，而非简单定义清单。
- 测试目录：889 个测试源码文件中包含 executor、transaction、cursor、cache、plugin、reflection、mapping、submitted 等边界证据，不能只扫主源码。

因此，原“4 个核心域 + 1 个扩展域”不足以作为完整 MyBatis 卷规划。当前更合理的结构是：`M-1..M-9` 形成 MyBatis 核心与机制补深层，`S-1/S-2` 形成 Spring 集成层与 Boot 装配层；`M-10` 不再作为独立正文域，而降级为主干边界说明。先写主干闭环，再按证据决定集成层与补层深度。

## 二、已验证的仓库事实

| 项目 | 已验证结果 | 证据 |
|---|---:|---|
| 版本 | 3.5.16 | `pom.xml:29-31` |
| 主源码 | 386 个 Java 文件 | `src/main/java/org/apache/ibatis/` 递归计数 |
| 测试源码 | 889 个 Java 文件 | `src/test/java/org/apache/ibatis/` 递归计数 |
| 主执行入口 | `SqlSessionFactoryBuilder.build()` → `XMLConfigBuilder.parse()` → `DefaultSqlSessionFactory` | `SqlSessionFactoryBuilder.java:47-51,95-97` |
| 配置状态中心 | `Configuration` 持有 registry、interceptor、type handler、mapped statement、cache、result map 与 incomplete 队列 | `Configuration.java:153-178` |
| Mapper 运行时桥 | `MapperProxy.invoke()` → cached invoker → `MapperMethod.execute()` 或 default method | `MapperProxy.java:80-107` |
| 执行器基础协议 | 一级缓存、deferred loads、query stack、事务与关闭清理 | `BaseExecutor.java:50-103,132-175` |
| SQL 到 JDBC | `RoutingStatementHandler` 按 STATEMENT/PREPARED/CALLABLE 选择 delegate | `RoutingStatementHandler.java:35-54` |
| 结果资源 | `DefaultSqlSession.selectCursor()` 注册 Cursor，`close()` 统一关闭 Executor 与 Cursor | `DefaultSqlSession.java:121-132,260-281` |
| Cursor 状态 | CREATED → OPEN → CLOSED/CONSUMED，单迭代器约束，消费完自动关闭 | `DefaultCursor.java:36-69,95-121,132-155` |
| XML 配置顺序 | properties/settings/插件/工厂/environment/databaseId/typeHandlers/mappers | `XMLConfigBuilder.java:105-134` |
| XML Mapper 解析 | cache、resultMap、sql fragments、statements；不完整元素进入待解析队列 | `XMLMapperBuilder.java:96-105,129-145` |
| 动态 SQL | `<trim>/<where>/<set>/<foreach>/<if>/<choose>/<bind>` 映射为 SqlNode；动态与静态 SqlSource 分流 | `XMLScriptBuilder.java:53-73,76-100` |
| 插件代理 | `Plugin.wrap()` 按 `@Intercepts` 签名只代理匹配接口；不匹配则原对象返回 | `Plugin.java:44-52,54-65,67-86` |
| 二级缓存事务语义 | `TransactionalCache` 在 commit flush，在 rollback 解锁并丢弃暂存项 | `TransactionalCache.java:30-33,94-133` |

## 三、修订后的知识域

### A. 主干层

#### M-1 配置启动与元数据构建

**读者问题**：一份 `mybatis-config.xml` 和 Mapper XML 如何变成可执行的 `Configuration`？

**入口**：`SqlSessionFactoryBuilder`、`XMLConfigBuilder`、`XMLMapperBuilder`。

**状态核心**：`Configuration` 的 mappedStatements、resultMaps、caches、type handlers、mapper registry、插件链、loadedResources 和 incomplete 队列。

**失败路径**：未知 setting、空 namespace、重复 statement、Mapper XML 不完整元素、配置解析失败后的异常包装与资源关闭。

**跨域桥**：M-2 的 Mapper 注册、M-3 的 MappedStatement 执行、M-5 的 Cache/ResultMap、M-6 的插件和类型系统。

#### M-2 Mapper 接口代理与调用语义

**读者问题**：接口方法没有实现，为什么调用它就能执行 SQL？

**入口**：`MapperRegistry`、`MapperProxyFactory`、`MapperProxy`、`MapperMethod`。

**状态核心**：Proxy、methodCache、`SqlCommand`、`MethodSignature`、返回类型分发、参数名解析、default method。

**失败路径**：未注册 Mapper、statement 不存在、返回类型与结果数量不匹配、异常 unwrap、Cursor/Optional/Map/Collection 返回分支。

**跨域桥**：M-1 提供 MappedStatement；M-3 接收 SqlSession；annotations/ 与 XML Mapper 是两种入口，后续需分别取证。

#### M-3 SqlSession、事务与资源生命周期

**读者问题**：一次 SqlSession 如何持有 Executor、Connection、事务和 Cursor，commit/rollback/close 谁负责什么？

**入口**：`DefaultSqlSessionFactory`、`DefaultSqlSession`、`JdbcTransaction`、`ManagedTransaction`、`Executor.close()`。

**状态核心**：autoCommit、dirty、Executor closed、事务连接、Cursor 注册列表、ErrorContext。

**失败路径**：查询/更新异常、强制提交/回滚、关闭时回滚、关闭 Cursor、事务关闭异常被记录后资源置空。

**跨域桥**：连接由 DataSource 提供但不与 Druid/HikariCP 实现混写；M-5 二级缓存 commit/rollback 语义依赖此域；M-8 Cursor 依赖 Session 关闭。

#### M-4 Executor 执行链与 JDBC 落地

**读者问题**：Mapper 方法最终如何穿过 Executor、StatementHandler、ParameterHandler、JDBC Statement 和 ResultSetHandler？

**入口**：`SimpleExecutor`、`ReuseExecutor`、`BatchExecutor`、`BaseExecutor`、`CachingExecutor`、`RoutingStatementHandler`。

**状态核心**：MappedStatement、BoundSql、CacheKey、queryStack、localCache、deferredLoads、Statement 生命周期、BatchResult。

**失败路径**：Executor closed、SQL 执行异常、Statement 关闭、Batch flush 异常、结果处理异常、事务回滚。

**跨域桥**：M-5 缓存包裹 Executor；M-6 插件包裹 Executor/StatementHandler 等；M-7 类型处理器连接参数与结果。

#### M-5 缓存与一致性边界

**读者问题**：一级缓存和二级缓存分别由谁持有，为什么二级缓存要等 commit 才真正写入？

**入口**：`PerpetualCache`、`LruCache`、`CacheKey`、`CachingExecutor`、`TransactionalCacheManager`、`TransactionalCache`。

**状态核心**：Session 级 localCache、namespace 级 delegate cache、entriesToAddOnCommit、entriesMissedInCache、clearOnCommit。

**失败路径**：update 清空一级缓存、flushCacheRequired、commit/rollback 分流、blocking cache 未命中后的解除锁定。

**跨域桥**：M-3 的事务生命周期、M-4 的 query/update、M-1 的 namespace/cache-ref/resultMap 解析。

### B. 机制补深层

#### M-6 动态 SQL、参数绑定与插件拦截

**读者问题**：动态 SQL 如何从 XML 节点变成最终 SQL 和参数，插件如何只拦截目标方法而不代理所有对象？

**入口**：`XMLScriptBuilder`、`SqlNode`、`DynamicSqlSource`、`BoundSql`、`ParameterHandler`、`InterceptorChain`、`Plugin`。

**状态核心**：SqlNode 递归 apply、DynamicContext、additionalParameters、ParameterMapping、Interceptor signature map。

**失败路径**：未知动态标签、OGNL 表达式错误、参数缺失、错误 `@Intercepts` 签名、目标没有匹配接口时不产生代理。

**跨域桥**：M-1 生成 MappedStatement/SqlSource；M-4 在执行器四层调用插件；M-7 提供 TypeHandler。

#### M-7 类型处理、反射映射与结果装配

**读者问题**：Java 对象字段如何变成 JDBC 参数，ResultSet 列如何写回嵌套对象？

**入口**：`TypeHandlerRegistry`、`BaseTypeHandler`、`ParameterMapping`、`MetaObject`、`Reflector`、`DefaultResultSetHandler`。

**状态核心**：Java/JDBC 类型解析、TypeHandler 选择、ObjectWrapper、自动映射、nestedResultObjects、ancestorObjects、延迟加载。

**失败路径**：未知类型处理器、列/属性不匹配、自动映射警告、构造器映射失败、嵌套结果关系未完成。

**跨域桥**：M-4 的 ParameterHandler/ResultSetHandler；M-1 的 resultMap/typeHandler 注册；reflection/ 与 annotation/ 都是该域输入。

#### M-8 Cursor、ResultHandler 与增量结果消费

**读者问题**：为什么 Cursor 不能随便多次迭代，谁关闭底层 ResultSet？

**入口**：`Cursor`、`DefaultCursor`、`DefaultResultSetHandler`、`DefaultSqlSession.selectCursor()`。

**状态核心**：CREATED/OPEN/CLOSED/CONSUMED、单 iterator、RowBounds、ResultContext.stop、Session cursorList。

**失败路径**：重复获取 iterator、已关闭 Cursor 继续读取、消费到末尾自动关闭、Session.close() 级联关闭 Cursor、ResultSet 异常。

**跨域桥**：M-3 Session 资源所有权；M-4 queryCursor；M-7 结果对象装配。

### C. 集成与生产补层

#### M-9 XML 与注解 Mapper 双入口

**范围**：`MapperAnnotationBuilder`、Mapper annotations、`ProviderSqlSource`，与 M-1/M-2 对照，不把注解定义逐个罗列。

**问题**：XML Mapper 与注解 Mapper 如何汇合到同一套 MappedStatement/SqlSource/MapperRegistry？

**已验证主链**：

- `MapperAnnotationBuilder.parse()` 先 `loadXmlResource()`，再设置 namespace、解析 cache/cache-ref，并遍历 mapper 方法；解析失败的方法进入 `configuration.addIncompleteMethod(...)`，最后统一 `parsePendingMethods(false)`：`MapperAnnotationBuilder.java:114`。
- `parseStatement(Method)` 根据注解构造 `SqlSource`、`SqlCommandType`、`Options`、`KeyGenerator`、`ResultMap`，最终调用 `assistant.addMappedStatement(...)`，说明注解入口与 XML 入口在同一个 MappedStatement 汇合点收束：`MapperAnnotationBuilder.java:282`。
- `ProviderSqlSource` 负责 `@SelectProvider/@UpdateProvider/...` 这一路：解析 provider type/value、解析 provider method、组装 `ProviderContext` 和参数，再交给 `languageDriver.createSqlSource(...)`；provider method 不存在、重载冲突、参数组合非法都会抛 `BuilderException`：`ProviderSqlSource.java:36`。

**结论**：M-9 已提升为已验证可写主域。它不是注解清单，而是“第二条配置入口如何并回 MappedStatement 主线”的桥接专题。

#### M-10 数据源与事务适配边界（降级为边界说明）

**范围**：MyBatis 自带 `datasource/` 与原生 `transaction/`，只回答“原生 Session 自己管连接时做什么”。Spring 接管后的责任转移，拆到后续 `S-1` / `S-2`。

**问题**：MyBatis 自己管理 Connection 与事务时做什么；哪些职责会在进入 Spring 后移交给外层容器？

**已验证证据**：

- `JdbcTransaction` 延迟到 `getConnection()` 时才真正 `openConnection()`；只在 `!autoCommit` 时提交/回滚；关闭前尝试把 autocommit 复位到 `true`，以兼容“只执行 select 但数据库仍要求先 commit/rollback 再 close”的场景：`JdbcTransaction.java:38`。
- `PooledDataSource` 自己维护 `PoolState`、`ReentrantLock`、`Condition`、idle/active 列表、overdue claim、ping query 和 bad connection 容忍度；它说明 MyBatis 并非完全不碰连接池，只是这套池比 HikariCP/Druid 更像内建边界设施，而不是本卷主线：`PooledDataSource.java:43`。

**结论**：M-10 更适合作为 M-3 的“原生连接责任边界”补段，而不是独立正文主题。真正值得单独成层的，是后续 `S-1` 中责任如何转交给 Spring。

## 四、边界重分类

### 不再直接淘汰

- `transaction/`：并入 M-3。
- `cursor/`：并入 M-8。
- `type/`：并入 M-7。
- `reflection/`：并入 M-7。
- `annotations/`：并入 M-9，按入口机制讲。
- `datasource/`：并入 M-10，只讲 MyBatis 原生连接获取与事务适配边界，不再和 Spring 集成层混写。
- `jdbc/`：只纳入脚本执行和测试证据，不单独成篇。
- `logging/`、`io/`、`parsing/`、`lang/`、`util/`：作为支撑证据，暂不独立成域。

### 当前明确暂缓

- `submitted/` 下大量单 issue 回归测试：作为失败路径索引，不逐篇展开。
- Testcontainers 与厂商数据库：作为验证矩阵，不在主干首轮展开。
- Starter samples、站点文档和多模板语言驱动样例：作为装配与生态侧证据，不单开卷。

## 五、推荐阅读顺序

```text
M-1 配置启动与元数据构建
  -> M-2 Mapper 代理与调用语义
    -> M-3 SqlSession、事务与资源生命周期
      -> M-4 Executor 执行链与 JDBC 落地
        -> M-5 缓存与一致性边界
          -> M-6 动态 SQL、参数绑定与插件拦截
            -> M-7 类型处理、反射映射与结果装配
              -> M-8 Cursor、ResultHandler 与增量结果消费
                -> M-9 XML/注解双入口
                  -> S-1 Spring 会话/事务桥
                    -> S-2 Boot 自动装配桥
```

M-8 可以在 M-4 后作为专题跳读；M-5 与 M-3 有双向语义桥，但调用方向仍是 Executor → Cache → Transaction commit/rollback，不构成架构回环。`M-10` 只作为 M-3 与 S-1 之间的边界说明，不再进入主阅读链。

## 六、Spring 集成层与 Boot 装配层补入

### S-1 MyBatis 与 Spring 的会话/事务桥

**读者问题**：把原生 `SqlSession` 放进 Spring 之后，谁负责复用当前事务里的 Session，谁禁止手工 commit/rollback/close？

**已验证主链**：

- `SqlSessionTemplate` 是 Spring 管理的 `SqlSession` 门面，内部通过 JDK 代理把每次调用转发给当前线程上的真实 Session；同时显式禁止手工 `commit()` / `rollback()` / `close()`：`mybatis-spring/src/main/java/org/mybatis/spring/SqlSessionTemplate.java:75`。
- 代理内部调用 `SqlSessionUtils.getSqlSession(...)`：先从 `TransactionSynchronizationManager` 取 `SqlSessionHolder`，没有才 `sessionFactory.openSession(executorType)`，再注册同步：`mybatis-spring/src/main/java/org/mybatis/spring/SqlSessionUtils.java:94`。
- `SpringManagedTransaction.openConnection()` 通过 `DataSourceUtils.getConnection(dataSource)` 取连接，并判断当前连接是否已被 Spring 事务管理；后续 `commit/rollback/close` 会按事务归属做 no-op 或委托：`mybatis-spring/src/main/java/org/mybatis/spring/transaction/SpringManagedTransaction.java:82`。
- `MapperFactoryBean.checkDaoConfig()` 在 Bean 初始化时确保 mapperInterface 已配置，并可按需把 Mapper 注册回 MyBatis `Configuration`：`mybatis-spring/src/main/java/org/mybatis/spring/mapper/MapperFactoryBean.java:79`。

**卷内定位**：这不是独立框架卷，而是 MyBatis 的“集成层主干”。应放在 MyBatis 核心主干之后，作为“原生 Session 进入 Spring 责任世界”的桥接专题。

### S-2 MyBatis 在 Spring Boot 中如何自动装起来

**读者问题**：只加 starter，为什么就会自动有 `SqlSessionFactory`、`SqlSessionTemplate` 和 Mapper 扫描？

**已验证主链**：

- `MybatisAutoConfiguration.sqlSessionFactory(DataSource)` 创建 `SqlSessionFactoryBean`，注入 DataSource、configLocation、configurationProperties、interceptors、databaseIdProvider、typeAliases/typeHandlers、mapperLocations，并最终 `factory.getObject()`：`mybatis-spring-boot-starter/mybatis-spring-boot-autoconfigure/src/main/java/org/mybatis/spring/boot/autoconfigure/MybatisAutoConfiguration.java:136`。
- `MybatisProperties.resolveMapperLocations()` 把配置的 mapper locations 展平成 `Resource[]`：`mybatis-spring-boot-starter/mybatis-spring-boot-autoconfigure/src/main/java/org/mybatis/spring/boot/autoconfigure/MybatisProperties.java:211`。
- `AutoConfiguredMapperScannerRegistrar.registerBeanDefinitions(...)` 在自动配置包存在时注册 `MapperScannerConfigurer`，默认按 `@Mapper` 扫描，并在 `SqlSessionTemplate` 与 `SqlSessionFactory` 之间择一注入：`mybatis-spring-boot-starter/mybatis-spring-boot-autoconfigure/src/main/java/org/mybatis/spring/boot/autoconfigure/MybatisAutoConfiguration.java:239`。
- `sqlSessionTemplate(...)` 由自动配置层在缺失 Bean 条件下创建，作为上层调用入口：`mybatis-spring-boot-starter/mybatis-spring-boot-autoconfigure/src/main/java/org/mybatis/spring/boot/autoconfigure/MybatisAutoConfiguration.java:217`。

**卷内定位**：Boot 层不是重讲 MyBatis 核心，而是“装配桥专题”。它应建立在 S-1 之后，因为 Boot 自动配置最终装配的仍是 `SqlSessionFactoryBean`、`SqlSessionTemplate` 和 Mapper 扫描器。

## 七、修订后的整卷层次

```text
A. MyBatis 核心主干层
  M-1 配置启动与元数据构建
  M-2 Mapper 接口代理与调用语义
  M-3 SqlSession、事务与资源生命周期
  M-4 Executor 执行链与 JDBC 落地
  M-5 缓存与一致性边界

B. MyBatis 机制补深层
  M-6 动态 SQL、参数绑定与插件拦截
  M-7 类型处理、反射映射与结果装配
  M-8 Cursor、ResultHandler 与增量结果消费
  M-9 XML 与注解 Mapper 双入口
  M-10 datasource/transaction 原生边界说明（不单列正文）

C. Spring 集成层
  S-1 SqlSessionTemplate / SqlSessionUtils / SpringManagedTransaction / MapperFactoryBean

D. Boot 装配层
  S-2 MybatisAutoConfiguration / MybatisProperties / MapperScannerConfigurer / AutoConfiguredMapperScannerRegistrar
```

推荐阅读顺序更新为：

```text
M-1 -> M-2 -> M-3 -> M-4 -> M-5 -> M-6 -> M-7 -> M-8 -> M-9 -> S-1 -> S-2
```

这样能先立住 MyBatis 自身责任边界，再解释它如何进入 Spring，再解释 Boot 如何把 Spring 集成层自动装起来。

## 八、方法论自检结果

- 机制域不再按目录简单切分：通过执行链、资源所有权、配置解析和扩展桥重组。
- 每个主干域已补入口、状态核心、失败路径、跨域连接点。
- 测试目录已纳入扫描：executor、transaction、cursor、cache、plugin、mapping、reflection 和 submitted 都有对应证据类别。
- 旧规划的“淘汰”已改为“当前主线/支撑证据/暂缓”，保留重新纳入条件。
- 已区分 MyBatis 核心、Spring 集成层与 Boot 装配层，避免把不同责任边界揉成一个“Starter 黑箱”。
- 当前尚未完成：M-9 注解入口深读、M-10 原生 datasource 边界细化、各域逐条源码锚点表，以及正文级 rewrite-plan。
- 当前卷仍缺生产层：例如大结果集/批处理/懒加载/缓存一致性排障，不应误判为已完整收口。
- 已找到可进入生产层候选的源码与测试证据：
  - `cursor_cache_oom` 指向 Cursor + nested result map 的内存边界。
  - `blocking_cache` 指向二级缓存锁语义。
  - `BatchExecutor` 在 `doFlushStatements()` 中把“前面部分子执行器已成功，但整批仍需回滚”显式编码进异常消息，适合作为批处理失败专题：`BatchExecutor.java:74`。
  - `ResultLoader` 在“创建线程变化”或“原 Executor 已关闭”时重建 SIMPLE Executor，再做懒加载查询，适合作为懒加载线程/生命周期边界专题：`ResultLoader.java:40`。
  - `CachingExecutor` 明确禁止带 OUT 参数的 CallableStatement 进入二级缓存，并把 flushCache/commit/rollback 语义与 `TransactionalCacheManager` 绑定，适合作为缓存一致性与禁区专题：`CachingExecutor.java:39`。
  后续可围绕“大结果集与 Cursor 使用边界”“二级缓存一致性与阻塞锁”“批处理与异常收束”“懒加载与对象膨胀”提炼生产排障主题。

## 九、下一步执行顺序

1. 深读 M-1 到 M-4 的完整调用链并建立源码锚点表。
2. 深读 M-5 到 M-9 的缓存、插件、类型映射、Cursor 和注解入口失败路径。
3. 收束生产层候选专题：Cursor 大结果集、blocking cache、批处理失败、懒加载边界，并判断哪些足以单开篇。
4. 深读 S-1 与 S-2，建立 MyBatis -> mybatis-spring -> mybatis-spring-boot-starter 的三层桥接图。
5. 对每个域补测试证据和边界清单，再进入正文 rewrite-plan。
