# 14-01：核心机制——Statement 过滤器模板化与三源装配

> **核心命题**：Druid 官方 `Filter` 接口有 491 个方法（javap/源码双重验证），覆盖 Connection/Statement/ResultSet 全生命周期；`AbstractStatementFilter extends FilterAdapter` 只精确收敛了其中 13 个 Statement 执行方法，把它们统一路由进一个 `execute(statement, callback)` 模板，子类只需覆写 `beforeExecute`/`afterExecute` 两个回调。装配侧，`@EnableAlibabaDruid` 用三源扫描（BeanFactory/SpringFactories/JavaServiceProvider）找到 Filter Bean，交给 `DruidDataSourceBeanPostProcessor` 在 `DruidDataSource` 初始化前排序塞入 `getProxyFilters()`。本文逐层拆解：官方基线、`AbstractStatementFilter` 解剖、内置样板、三源装配、自动配置，以及 4 个问题（含一个 switch fallthrough 导致 Bean 重复注册的真实 bug）。

---

## 一、项目定位（是什么、为什么、作用是什么）

**是什么**：microsphere-alibaba-druid 是 Druid 连接池的"Statement 过滤器模板化"扩展——不改 Druid 本身，用一个抽象类把官方 `Filter` 接口里几十个 JDBC 方法收敛成两个回调，再用 Spring 三源扫描把实现类接入 `DruidDataSource`。

**为什么要有（从官方 Filter 接口的负担推导）**：

| # | 官方 `Filter` 接口的负担 | microsphere 的答案 |
|---|------------------------|-------------------|
| 1 | **接口方法多**：`Filter extends Wrapper`，491 个抽象方法覆盖 Connection/Statement/ResultSet/Blob/Clob 等全部 JDBC 对象的每个方法 | **精确收敛**：只关心"SQL 语句执行"这一类操作，13 个方法归并到 1 个模板 |
| 2 | **直接实现 `Filter` 要写 491 个方法**（哪怕大多数不关心）；即便继承 `FilterAdapter`（默认透传实现），要拦截 Statement 执行仍需覆写 7 个不同方法名（`preparedStatement_execute`/`_executeQuery`/`_executeUpdate` + `statement_execute`/`_executeQuery`/`_executeBatch`/`_executeUpdate`），因重载共 13 个具体签名 | **两个回调走天下**：`beforeExecute(statement, resourceName)` / `afterExecute(statement, resourceName, result, failure)` |
| 3 | **异常处理各自为战**：每个方法的 try/catch/finally 逻辑要自己写一遍 | **统一模板**：`execute()` 一处收敛 try/catch/finally + result/failure 语义 |
| 4 | **SQL 语句到"资源名"的映射**没有现成机制（多为业务自己维护埋点名） | **内置 `buildResourceName`**：解析 SQL AST，自动生成 `"SELECT users"`/`"UPDATE users"` 风格的资源名，直接对接 Sentinel 等限流生态的资源标识 |

**作用是什么**：
- **监控 SQL 执行**：`LoggingStatementFilter` 是内置样板，DEBUG 级别打印 SQL + resourceName + result/failure
- **横切能力接入点（设计目标，非已验证实现）**：项目根 README 声明"支持 Microsphere Sentinel/Resilience4j/Observability 等生态项目的扩展"——但 microsphere-alibaba-druid 项目**内部未见任何 Sentinel 集成实现类**（不同于 13-mybatis 有 `SentinelMyBatisExecutorFilter` 作为实证），这是官方声明的设计方向，尚待生态项目侧落地
- **auto-configuration**：Spring Boot 自动配置 + actuator 池指标暴露（`DruidDataSourcePoolMetadata`）

**在训练营体系里的位置**：14 与 13-mybatis 是"两个层级的拦截"——13 在 MyBatis Executor 层（ORM 之上，见 SQL 语义、MappedStatement），14 在 JDBC 层（Druid `FilterAdapter`，只见 JDBC 语句字符串）。同一条 SQL **先过 MyBatis Executor 管道，再进 Druid Filter**，顺序固定、粒度互补（13-03 第四节已预埋此对照，本文与 14-02 展开细节）。

**与官方 `FilterAdapter` 的差异（一句话对照表）**：

| | 官方 `FilterAdapter` | `AbstractStatementFilter` |
|---|---|---|
| 覆盖范围 | 491 个方法全部提供默认（透传）实现 | 只精确收敛 13 个 Statement 执行方法 |
| 拦截模型 | 子类自选覆写哪些方法，各自管异常处理 | 统一 `execute()` 模板，子类只覆写 2 个回调 |
| 资源命名 | 无 | 内置 SQL AST 解析生成资源名 |
| 覆盖对象 | Connection/Statement/PreparedStatement/CallableStatement/ResultSet 等全部 | 仅 `PreparedStatement` + `Statement`，**不覆盖 `CallableStatement`**（存储过程调用不受管） |

---

## 二、官方基线：Druid `Filter`/`FilterAdapter`/`FilterChain`（1.2.28 源码验证）

### 2.1 `Filter` 接口：491 个方法

`com.alibaba.druid.filter.Filter extends Wrapper`（官方源码 `Filter.java`）：4 空格缩进的方法声明精确统计为 **491 个**（javap 反编译字节码复核一致），方法名按 8 个前缀分组：`connection_*`（Connection 全部操作）、`statement_*`/`preparedStatement_*`/`callableStatement_*`（Statement 家族）、`resultSet_*`/`resultSetMetaData_*`（ResultSet）、`clob_*`（Clob 大对象；Blob 相关方法未独立分组，挂在 `connection_createBlob`/`resultSet_getBlob`/`preparedStatement_setBlob`/`callableStatement_getBlob` 等前缀下）、`dataSource_*`，每个方法都携带一个 `FilterChain` 参数用于继续调用链。

### 2.2 `FilterAdapter`：默认透传实现

`FilterAdapter implements Filter`（源码注释："提供 JdbcFilter 的基本实现，使得实现一个 JdbcFilter 更容易"）——491 个方法全部提供默认实现，逐一透传给 `FilterChain`，例如：

```java
// FilterAdapter.java:2482-2484（官方源码）
public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
    return chain.statement_execute(statement, sql);
}
```

子类继承 `FilterAdapter` 后只需覆写关心的方法，其余保持透传——这是 Druid 官方给出的"减负"方案，但要拦截 Statement 执行仍需覆写 7 个方法名、13 个具体方法签名（见下）。

### 2.3 `AutoLoad`：SPI 自动加载语义

```java
// DruidDataSource.java:1021-1038（官方源码）
ServiceLoader<Filter> autoFilterLoader = ServiceLoader.load(Filter.class);
for (Filter filter : autoFilterLoader) {
    AutoLoad autoLoad = filter.getClass().getAnnotation(AutoLoad.class);
    if (autoLoad != null && autoLoad.value()) {
        filters.add(filter);
    }
}
```

`DruidDataSource` 初始化时会通过 JDK `ServiceLoader` 扫描 `META-INF/services/com.alibaba.druid.filter.Filter`，凡是标注 `@AutoLoad`（`value()` 默认 `true`）的 Filter **会被官方机制自动挂载**，不需要用户手动 `addFilter`。这个官方语义是理解 14 项目里"三源扫描要跳过 `@AutoLoad` filter"设计的前提（见第四节）。

---

## 三、`AbstractStatementFilter` 解剖

### 3.1 init：DataSource 绑定 + validationSQL 缓存

```java
// AbstractStatementFilter.java:112-122
@Override
public final void init(DataSourceProxy dataSource) {
    this.dataSource = dataSource;
    if (dataSource instanceof DruidDataSource) {
        DruidDataSource druidDataSource = (DruidDataSource) dataSource;
        this.validationSQL = druidDataSource.getValidationQuery();
    }
    ...
}
```

`init` 是 `final`——不允许子类覆写初始化逻辑，只做两件事：持有 `dataSource` 引用（供 `buildResourceName` 取 `dbType`）、缓存 `validationSQL`（连接池心跳检测语句，通常是 `SELECT 1`）。**缓存 validationSQL 的目的**：避免每次执行心跳检测 SQL 都触发一次 SQL AST 解析（下面 3.3 会看到 `buildResourceName` 对 `validationSQL` 有短路判断）。

### 3.2 13 个 final 方法：精确收敛

```java
// AbstractStatementFilter.java:125-187（节选）
@Override
public final boolean preparedStatement_execute(FilterChain chain, PreparedStatementProxy statement) throws SQLException {
    return execute(statement, () -> super.preparedStatement_execute(chain, statement));
}
// ... 12 个同构方法
```

逐一列出覆写的 13 个方法签名（全部 `final`，禁止子类再覆写，保证收敛口径唯一）：

| 分组 | 方法 | 数量 |
|------|------|------|
| PreparedStatement | `preparedStatement_execute` / `_executeQuery` / `_executeUpdate` | 3 |
| Statement.execute 重载 | `statement_execute(sql)` / `(sql, autoGeneratedKeys)` / `(sql, int[] columnIndexes)` / `(sql, String[] columnNames)` | 4 |
| Statement 其余 | `statement_executeQuery` / `statement_executeBatch` | 2 |
| Statement.executeUpdate 重载 | `statement_executeUpdate(sql)` / `(sql, autoGeneratedKeys)` / `(sql, int[])` / `(sql, String[])` | 4 |

共 **13 个**方法，每一个的方法体都是同一种写法：把原方法调用包成 `ThrowableSupplier` 回调，转交给 `execute(statement, callback)`。这 13 个方法覆盖了 `PreparedStatement` 和普通 `Statement` 的全部执行入口，但**不覆盖 `CallableStatement`**（存储过程调用没有对应的 `callableStatement_*` 覆写）——如果业务用存储过程，这层过滤器完全看不到。

### 3.3 `execute` 模板：唯一的异常处理入口

```java
// AbstractStatementFilter.java:205-222
protected <T> T execute(StatementProxy statement, ThrowableSupplier<T> callback) throws SQLException {
    String resourceName = buildResourceName(statement);
    T result = null;
    Throwable failure = null;
    try {
        beforeExecute(statement, resourceName);
        result = callback.get();
    } catch (Throwable e) {
        failure = e;
        throw wrap(e, SQLException.class);
    } finally {
        afterExecute(statement, resourceName, result, failure);
    }
    return result;
}
```

**执行序**：`buildResourceName` → `beforeExecute` → 实际执行（`callback.get()`，即调用链后续环节）→ 无论成功失败都走 `afterExecute`（`finally`）。异常处理走 `wrap(e, SQLException.class)`（`io.microsphere.util.ExceptionUtils.wrap`）——**不是无差别重新包装**：先判断 `isAssignableFrom(SQLException.class, e.getClass())`，若原始异常本就是 `SQLException`（或其子类，比如 `callback.get()` 内部直接抛出的 JDBC 异常），**直接原样返回，堆栈和类型完全保留**；只有原始异常类型不兼容时（比如业务代码在 Filter 链路上抛了 `RuntimeException`），才通过反射构造一个新的 `SQLException(message, cause)` 实例——`cause` 取原始异常本身（无 message 时）或其 `getCause()`（有 message 时）。这个设计的意义：**保证 `Filter` 接口方法签名契约**（几乎全部方法都声明 `throws SQLException`）不被打破，同时尽量不破坏原始异常的身份信息。`failure` 变量记录的是**原始异常**（wrap 前的 `e`），传给 `afterExecute` 的观察者能看到真实异常类型，不受 wrap 影响。

这个 try/catch/finally + result/failure 传递的**骨架结构**与 13-mybatis 的 `InterceptorsExecutorFilterAdapter.update`（13-01 第 3.4 节）同构——都是"before-call-after，failure 不吞异常，通过 finally 保证 after 回调必执行"。但两者的具体异常处理**不完全相同**：13-mybatis 的 `update` 只 `catch (SQLException e)`，捕获后原样 `throw e`（不做类型转换，因为 `Executor.update` 本身声明的检查异常就是 `SQLException`）；14-druid 的 `execute()` 是 `catch (Throwable e)`，捕获范围更宽（因为 `callback.get()` 内部可能抛任意 `RuntimeException`），并调用 `wrap()` 做类型归一化——**结构同构，捕获范围与归一化逻辑不同**，是同一设计思想在不同异常契约下的适配。

### 3.4 `buildResourceName`：SQL AST 解析生成资源名

```java
// AbstractStatementFilter.java:249-265
protected String buildResourceName(StatementProxy statement) {
    String sql = statement.getLastExecuteSql();
    if (Objects.equals(sql, validationSQL)) {
        return sql;   // 心跳检测 SQL 短路，不解析 AST
    }
    String dbType = dataSource.getDbType();
    List<SQLStatement> statementList = parseStatements(sql, dbType);
    SQLStatement sqlStatement = first(statementList);
    String resourceName = buildResourceName(sqlStatement);
    if (resourceName == null) {
        resourceName = "UNRECOGNIZED";
    }
    return resourceName;
}
```

再按语句类型分派到 4 个重载（`SQLSelectStatement`/`SQLUpdateStatement`/`SQLInsertStatement`/`SQLDeleteStatement`），分别拼出 `"SELECT <table>"`/`"UPDATE <table>"`/`"INSERT <table>"`/`"DELETE <table>"` 格式的资源名（表名来自 `SQLTableSource.computeAlias()`）。

**两个设计细节**：
1. **validationSQL 短路**：心跳检测语句（如 `SELECT 1`）直接返回原 SQL 字符串作资源名，不进 AST 解析——避免每次心跳检测都触发一次 SQL 解析的开销，且心跳 SQL 本身语义简单，用它自己当资源名足够。
2. **解析失败兜底为 `"UNRECOGNIZED"`**：`SQLUtils.parseStatements` 遇到方言不支持的语句、`buildResourceName(SQLStatement)` 遇到未识别的语句类型（非 SELECT/UPDATE/INSERT/DELETE，比如 DDL），都会返回 `null`，最终兜底成固定字符串——保证 `buildResourceName` 永不返回 `null`（`beforeExecute`/`afterExecute` 拿到的 resourceName 一定非空）。

**resourceName 构建方法可覆写化（一次真实的 bug-fix 演进）**：git 提交 `54b49e6`（2026-01-14, "Change resource name builder methods to protected"）把 5 个 `buildResourceName` 重载从 `private` 改成 `protected`，允许子类定制资源名生成逻辑；**同一提交还顺带修复了一个真实 bug**——`SQLUpdateStatement` 版本原来调用 `updateStatement.getFrom()`，被改成 `updateStatement.getTableSource()`。这不是无意义重命名：UPDATE 语句语义上没有 FROM 子句（FROM 是 SELECT 特有的），调用 `getFrom()` 对 UPDATE 语句而言是语义错误的 API 使用（可能返回 null 或抛异常，具体取决于 druid AST 实现），改成 `getTableSource()` 才是 UPDATE 语句正确的表来源获取方式。

### 3.5 `LoggingStatementFilter`：内置样板

```java
// LoggingStatementFilter.java:61-89
public class LoggingStatementFilter extends AbstractStatementFilter {
    @Override
    protected void beforeExecute(StatementProxy statement, String resourceName) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("beforeExecute(statement : {} , resource name : '{}') : {}",
                    statement.getLastExecuteSql(), resourceName);
        }
    }
    @Override
    protected void afterExecute(StatementProxy statement, String resourceName, Object result, Throwable failure) {
        if (logger.isDebugEnabled()) {
            logger.debug("afterExecute(statement : {} , resource name : '{}' , result : {} , failure : {})",
                    statement.getLastExecuteSql(), resourceName, result, failure);
        }
    }
}
```

两个回调都在 DEBUG 级别打印。**一个小瑕疵**：`beforeExecute` 的格式化字符串有 3 个 `{}` 占位符但只传了 2 个参数（`afterExecute` 对比是对的，4 占位符对应 4 参数）——纯日志格式问题，不影响功能，`LoggingStatementFilterTest` 未对日志内容做断言，因此未被测试捕获。

---

## 四、装配：`@EnableAlibabaDruid` 三源扫描 + BeanPostProcessor

### 4.1 三源扫描的设计意图

```java
// EnableAlibabaDruid.java:79/87
Class<? extends Filter>[] filterClasses() default {Filter.class};
BeanSource[] sources() default {BEAN_FACTORY, SPRING_FACTORIES};
```

`BeanSource` 枚举三个来源：
- `BEAN_FACTORY`：Spring 容器里已注册的 Bean（用户自己 `@Bean` 声明的 Filter）
- `SPRING_FACTORIES`：`META-INF/spring.factories` 声明的 Filter 类
- `JAVA_SERVICE_PROVIDER`：`META-INF/services/io.microsphere.alibaba.druid.filter.AbstractStatementFilter` 声明的 Filter 类（JDK SPI）

### 4.2 `AlibabaDruidRegistrar`：三源分派（含一个真实 bug）

```java
// AlibabaDruidRegistrar.java:67-79
private void registerFilterBeans(BeanDefinitionRegistry registry, Class<? extends Filter>[] filterClasses, BeanSource[] sources) {
    for (BeanSource source : sources) {
        switch (source) {
            case SPRING_FACTORIES:
                registerFiltersBySpringFactories(registry, filterClasses);
            case JAVA_SERVICE_PROVIDER:
                registerFiltersByJavaServiceProvider(registry, filterClasses);
            case BEAN_FACTORY:
            default:
                registerDruidDataSourceBeanPostProcessor(registry, filterClasses);
        }
    }
}
```

**switch 没有任何 `break`**——这不是设计上的有意 fallthrough（对照命名和语义，三个 case 是三个独立分支，不像是故意穿透），而是遗漏。**实际影响**：只要 `sources` 数组包含 `SPRING_FACTORIES` 或 `JAVA_SERVICE_PROVIDER`，由于 fallthrough，最终都会落到 `default` 分支执行 `registerDruidDataSourceBeanPostProcessor`——而 `@EnableAlibabaDruid` **默认** `sources = {BEAN_FACTORY, SPRING_FACTORIES}`，这意味着**默认配置下 `registerDruidDataSourceBeanPostProcessor` 会被调用两次**（一次是 `BEAN_FACTORY` 分支落到 default 执行，一次是 `SPRING_FACTORIES` 分支 fallthrough 到 default 执行）。

这个 bug 是提交 `81b8043b`（2026-05-30，"Add BeanSource support and rename filterClasses"）引入的，至今未修复；`EnableAlibabaDruidTest` 用了全部三个 source 但没有断言"BPP 只应注册一次"，因此没有被测试捕获。

**实际后果分析**：`registerDruidDataSourceBeanPostProcessor` 内部调用 `registerBeanDefinition(registry, BEAN_NAME, DruidDataSourceBeanPostProcessor.class, filterClasses)`——`BEAN_NAME` 是固定字符串 `"druidDataSourceBeanPostProcessor"`。第二次注册会因为同名 Bean 已存在触发 `BeanRegistrar.registerBeanDefinition` 内部的"已存在则跳过并 warn"逻辑（`allowBeanDefinitionOverriding=false` 路径），**实际运行结果是无害的**（第二次注册被跳过，只多一条 WARN 日志），但这是"因为下游有幂等保护才没暴露"，switch 本身的 fallthrough 仍是代码缺陷,如果下游改为允许覆盖注册就会出问题。

### 4.3 `DruidDataSourceBeanPostProcessor`：排序注入

```java
// DruidDataSourceBeanPostProcessor.java:137-147
protected void initializeFilterBeans(DruidDataSource druidDataSource) {
    List<Filter> filterBeans = new LinkedList<>();
    for (Class<? extends Filter> filterBeanClass : filterBeanClasses) {
        filterBeans.addAll(getSortedBeans(this.beanFactory, filterBeanClass));
    }
    sort(filterBeans);
    druidDataSource.getProxyFilters().addAll(filterBeans);
}
```

在 `DruidDataSource` bean **初始化之前**（`processBeforeInitialization`）执行：按配置的 `filterBeanClasses` 从 Spring 容器取出全部匹配的 Filter Bean，用 `AnnotationAwareOrderComparator` 排序后，整批塞进 `DruidDataSource.getProxyFilters()`。

**装配全景**：

```
@EnableAlibabaDruid(filterClasses=..., sources=...)
  → AlibabaDruidRegistrar.registerBeanDefinitions
    → 遍历 sources：
        SPRING_FACTORIES → 从 spring.factories 找到的 Filter 类注册成 Bean
        JAVA_SERVICE_PROVIDER → 从 META-INF/services 找到的（跳过 @AutoLoad 标注的类）
        BEAN_FACTORY / fallthrough 落地 → 注册 DruidDataSourceBeanPostProcessor
  → Spring 容器初始化阶段：
      DruidDataSourceBeanPostProcessor.processBeforeInitialization(druidDataSource)
        → 从容器取出全部 Filter Bean → 排序 → druidDataSource.getProxyFilters().addAll(...)
      DruidDataSource.init() → 官方 SPI 扫描（@AutoLoad Filter 自动加入）
```

**跳过 `@AutoLoad` 的设计动机**（`AlibabaDruidRegistrar.registerFiltersByJavaServiceProvider`）：

```java
// AlibabaDruidRegistrar.java:85-97
for (Class<? extends Filter> serviceClass : serviceClasses) {
    if (serviceClass.isAnnotationPresent(AutoLoad.class)) {
        continue;   // 跳过：官方 ServiceLoader 已经会自动加载它
    }
    registerBeanDefinition(registry, serviceClass);
}
```

结合第二节确认的官方语义（`DruidDataSource.init()` 内部通过 `ServiceLoader.load(Filter.class)` 自动加载 `@AutoLoad` 标注的 Filter），这里的"跳过"是**避免重复注册**——标了 `@AutoLoad` 的 Filter 类，官方机制自己会在 `init()` 时挂载一次，如果 microsphere 的三源扫描再注册一次成 Spring Bean 并塞进 `getProxyFilters()`，同一个 Filter 类就会产生两个实例、被挂载两次。这个设计逻辑是对的，不是 bug。

---

## 五、自动配置与条件注解

### 5.1 两级条件注解

```java
// ConditionalOnAlibabaDruidEnabled.java:50
@ConditionalOnProperty(name = ALIBABA_DRUID_ENABLED_PROPERTY_NAME, matchIfMissing = true)

// ConditionalOnAlibabaDruidAvailable.java:55-58
@ConditionalOnAlibabaDruidEnabled
@ConditionalOnClass(name = {"com.alibaba.druid.pool.DruidDataSource"})
```

`ConditionalOnAlibabaDruidAvailable` 是复合条件：属性开关（`microsphere.alibaba.druid.enabled`，默认 `true`，`matchIfMissing=true`）+ classpath 存在 `DruidDataSource`。与 13-mybatis 的两层开关体系（`@ConditionalOnMyBatisEnabled` + `@ConditionalOnMyBatisAvailable`）同构——同一作者的惯用模式。

### 5.2 `AlibabaDruidAutoConfiguration`：BPP + actuator 指标

```java
// AlibabaDruidAutoConfiguration.java:68-93
@Bean(name = BEAN_NAME)
public BeanPostProcessor druidDataSourceBeanPostProcessor(AlibabaDruidProperties alibabaDruidProperties) {
    Filter filter = alibabaDruidProperties.getFilter();
    return new DruidDataSourceBeanPostProcessor(filter.getClasses());
}

@Bean
@ConditionalOnBean(DruidDataSource.class)
public DataSourcePoolMetadataProvider druidDataSourcePoolMetadataProvider() {
    return dataSource -> {
        DruidDataSource druidDataSource = unwrap(dataSource, DruidDataSourceMBean.class, DruidDataSource.class);
        return druidDataSource == null ? null : new DruidDataSourcePoolMetadata(druidDataSource);
    };
}
```

两个 Bean：
1. **`DruidDataSourceBeanPostProcessor`**——用 `AlibabaDruidProperties.Filter.getClasses()`（默认 `{Filter.class}`，即"所有 Filter Bean"）构造，走的是 Spring Boot 属性绑定路线，不经过 `@EnableAlibabaDruid` 的三源扫描（自动配置场景下没有用户代码去标注 `@EnableAlibabaDruid`）。
2. **`DataSourcePoolMetadataProvider`**——把 `DruidDataSource` 适配成 Spring Boot Actuator 的 `DataSourcePoolMetadata`，暴露连接池的 `usage`/`active`/`idle`/`max`/`min`/`validationQuery`/`defaultAutoCommit` 等指标到 `/actuator/metrics`。`unwrap` 保证兼容"DataSource 被其他代理包装"的场景（先 unwrap 拿到真实 `DruidDataSource` 再构造 metadata）。

### 5.3 spring-cloud：空壳（自动配置层面）

```java
// AlibabaDruidCloudAutoConfiguration.java:38-40
@ConditionalOnAlibabaDruidAvailable
public class AlibabaDruidCloudAutoConfiguration {
}
```

自动配置类本身**无任何 `@Bean` 方法**——但这不意味着 spring-cloud 模块完全没有内容：`features.yaml` 里登记了 `DruidDataSource`/`Filter` 两个抽象特征，通过 Spring Cloud Actuator 的 `/features` 端点暴露"当前生效的具体实现类"（`AlibabaDruidCloudAutoConfigurationIntegrationTest` 验证了这一点：`microsphere-alibaba-druid-core.features` 下有 2 个 named feature——`DruidDataSource` 和用户配置的 `LoggingStatementFilter`）。**空的是自动配置逻辑，不是整个模块**——这个区分留给 14-02 展开。

---

## 六、问题清单（已证）

| # | 问题 | 证据 |
|---|------|------|
| P1 | **双开关冗余**：`AlibabaDruidProperties.enabled` 与 `@ConditionalOnAlibabaDruidEnabled` 各自独立判断，语义重叠——`AlibabaDruidProperties.enabled` 字段本身在自动配置逻辑里未被实际读取用于开关判断，只是暴露给使用者查看/绑定 | `AlibabaDruidProperties.java:42-59` + `AlibabaDruidAutoConfiguration.java`（未见对 `isEnabled()` 的分支判断） |
| P2 | **switch fallthrough 导致 BPP 重复注册**：`AlibabaDruidRegistrar.registerFilterBeans` 三个 case 均无 `break`，默认 `sources` 下会重复调用 `registerDruidDataSourceBeanPostProcessor`——因下游 Bean 同名去重才未暴露为运行时错误 | `AlibabaDruidRegistrar.java:67-79`，git `81b8043b`（2026-05-30） |
| P3 | **spring-cloud 自动配置空壳**：`AlibabaDruidCloudAutoConfiguration` 无 `@Bean` 方法，真正的内容在 `features.yaml`（第 5.3 节） | `AlibabaDruidCloudAutoConfiguration.java:38-40` |
| P4 | **`LoggingStatementFilter.beforeExecute` 日志占位符数量不匹配**：3 个 `{}` 只传 2 个参数（纯格式瑕疵，不影响功能） | `LoggingStatementFilter.java:64` |

---

## 七、测试佐证

- `AbstractStatementFilterTest.testInitOnMockDataSource`：验证 `init()` 对非 `DruidDataSource` 场景下 `validationSQL` 保持 `null`（mock 的 `DataSourceProxy` 不是 `DruidDataSource` 实例）。
- `AbstractStatementFilterTest.testExecuteOnFailed`：验证 `execute()` 把任意 `RuntimeException` 包装成 `SQLException` 抛出——3.3 节的异常语义有测试保护（但测试只覆盖了"类型不兼容需要包装"的分支，未覆盖"原始异常已是 SQLException 直接原样返回"的分支）。
- `AbstractStatementFilterTest.testBuildResourceNameOnNullPointerException`：对空的 `SQLSelectStatement`（未设置任何字段）调用 `buildResourceName`，断言返回 `null`——验证"未识别/构造失败"分支被 catch 而不是直接抛异常上抛。
- `EnableAlibabaDruidTest`：用全部三个 `BeanSource`（`BEAN_FACTORY`/`SPRING_FACTORIES`/`JAVA_SERVICE_PROVIDER`），配合 `META-INF/services`+`spring.factories` 测试桩（`AutoLoadFilter`/`LoadFilter`/`TestStatementFilter`），验证三源扫描能找到对应 Filter——但未断言 BPP 注册次数，P2 因此未被捕获。
- `AlibabaDruidAutoConfigurationIntegrationTest`：验证 `microsphere.alibaba.druid.filter.classes` 属性正确绑定到 `AlibabaDruidProperties.Filter.classes`，且 `DataSourcePoolMetadataProvider` 返回 `DruidDataSourcePoolMetadata` 实例。

---

## 八、小结（引用要点）

- **一句话**：`AbstractStatementFilter` 用一个 `execute()` 模板收敛了官方 `Filter` 接口 491 个方法中的 13 个 Statement 执行方法，子类只需实现 `beforeExecute`/`afterExecute` 两个回调；装配侧用三源扫描找 Filter Bean，`DruidDataSourceBeanPostProcessor` 在 `DruidDataSource` 初始化前统一注入。
- **能力边界**：只覆盖 `PreparedStatement`/`Statement`，不覆盖 `CallableStatement`（存储过程不受管）。
- **与 13-mybatis 的同构**：`execute()` 模板的 try/catch/finally + result/failure **骨架结构**与 `InterceptorsExecutorFilterAdapter.update`（13-01）同构，但捕获范围（`Throwable` vs `SQLException`）与异常归一化逻辑（`wrap()` 类型判断 vs 原样 `throw`）不同——同一设计思想在不同异常契约下的适配，14-02 展开对照细节。
- **一个真实 bug（P2）**：switch fallthrough 导致默认配置下 BPP 被注册两次，因下游 Bean 去重才未暴露成运行时错误——这类"因幂等保护掩盖设计缺陷"的模式值得警惕：换一个不做去重的场景就会出问题。
- **一次真实的历史 bug-fix**：`buildResourceName(SQLUpdateStatement)` 曾错误调用 `getFrom()`（UPDATE 语句没有 FROM 语义），git 提交 `54b49e6` 修正为 `getTableSource()`。
