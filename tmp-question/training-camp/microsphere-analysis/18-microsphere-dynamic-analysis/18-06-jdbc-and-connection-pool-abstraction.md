# 18-06 JDBC 与连接池抽象

## 目录

- [DataSource 接口设计](#datasource-接口设计)
- [AbstractRoutingDataSource：路由模式](#abstractroutingdatasource路由模式)
- [HikariCP 连接池机制详解](#hikaricp-连接池机制详解)
- [连接池配置调优](#连接池配置调优)
- [HikariCP vs Tomcat DBCP vs DBCP2](#hikaricp-vs-tomcat-dbcp-vs-dbcp2)
- [JDBC URL 的标准化](#jdbc-url-的标准化)
- [DataSourceConfigurationPropertiesSynthesizer 的池化类型适配](#datasourceconfigurationpropertiessynthesizer-的池化类型适配)

---

## DataSource 接口设计

`javax.sql.DataSource` 是 JDBC 规范中定义的数据库连接工厂接口，它是整个 18-dynamic 模块的"底层协议"——所有模块最终都服务于创建和管理 DataSource。

### 接口定义

```java
// javax.sql.DataSource
public interface DataSource  extends CommonDataSource, Wrapper {

    // 获取连接（核心方法）
    Connection getConnection() throws SQLException;
    Connection getConnection(String username, String password) throws SQLException;

    // 日志（非核心）
    PrintWriter getLogWriter() throws SQLException;
    void setLogWriter(PrintWriter out) throws SQLException;

    // 超时（非核心）
    int getLoginTimeout() throws SQLException;
    void setLoginTimeout(int seconds) throws SQLException;

    // 父日志（非核心，Java 1.7+）
    Logger getParentLogger() throws SQLFeatureNotSupportedException;

    // 接口适配（非核心）
    <T> T unwrap(Class<T> iface) throws SQLException;
    boolean isWrapperFor(Class<?> iface) throws SQLException;
}
```

### DataSource 的实现层次

18-dynamic 中的 DataSource 实现构成了一个多层委托链：

```java
// 最外层
DynamicDataSource (implements DataSource, InitializingBean, DisposableBean)
  // 热替换代理，每次操作委托给内部的 delegate
  └─ delegate → HikariDataSource (或 ShardingSphereDataSource)
                |
                ├─ HikariDataSource (implements DataSource)
                |   // 真正的连接池，管理一组数据库连接
                |   └─ 内部 → HikariPool
                |       └─ Connection[] (HikariConnection)
                |
                └─ ShardingSphereDataSource (implements DataSource)
                    // ShardingSphere 的包装 DataSource
                    └─ 内部 → ShardingSphereConnection
                        └─ 实际路由到 → HikariDataSource[ds0]
                                         HikariDataSource[ds1]
```

每一层都实现了 `javax.sql.DataSource`，因此对外部调用者透明——调用者只需要 `dataSource.getConnection()`，不需要知道内部是几层包装。

### DynamicDataSource 的委托模式

```java
// DynamicDataSource 的所有方法都是委托给内部的 delegate
public class DynamicDataSource implements DataSource, ... {

    private volatile DataSource delegate;

    @Override
    public Connection getConnection() throws SQLException {
        return getDelegate().getConnection();  // 委托
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getDelegate().getConnection(username, password);  // 委托
    }

    // 其他所有方法同上模式
    // ...
}
```

关键设计：`volatile DataSource delegate`。`volatile` 保证了 delegate 的写入对所有线程立即可见。切换时 `synchronized(mutex)` 保证原子性——不会出现两个线程同时看到不同的 delegate。

### 为什么需要层层包装

```
每一层代表一个不同的关注点：

DynamicDataSource     → 关注点：运行时热替换（配置变更、Zone 切换）
  delegate             → 指针原子切换
  childContext         → 子上下文生命周期管理
  closeScheduler       → 延迟关闭旧资源

HikariDataSource      → 关注点：连接池管理
  maximumPoolSize      → 连接数量控制
  connectionTimeout    → 连接超时
  idleTimeout          → 空闲回收

ShardingSphereDataSource → 关注点：SQL 路由与改写
  rules                → 分片/读写分离规则
  mode                 → 配置存储模式
```

如果你的应用场景是"每次请求走不同库"，在 DynamicDataSource 和 HikariDataSource 之间加一层 `ZoneRoutingDataSource`（继承 `AbstractRoutingDataSource`，根据当前 Zone 选择 DataSource），就是四层委托。

---

## AbstractRoutingDataSource：路由模式

Spring 提供了 `AbstractRoutingDataSource` 这个抽象基类，是实现"请求级数据源路由"的标准方式。

### 核心原理

```java
public abstract class AbstractRoutingDataSource extends AbstractDataSource {

    // 目标 DataSource 映射表
    private Map<Object, Object> targetDataSources;

    // 默认 DataSource
    private Object defaultTargetDataSource;

    // 已解析的目标 DataSource 缓存
    private Map<Object, DataSource> resolvedDataSources;

    // 已解析的默认 DataSource
    private DataSource resolvedDefaultDataSource;

    // 抽象方法：子类实现，返回当前上下文的 lookupKey
    protected abstract Object determineCurrentLookupKey();

    @Override
    public Connection getConnection() throws SQLException {
        // 1. 获取当前 lookupKey
        Object lookupKey = determineCurrentLookupKey();

        // 2. 根据 lookupKey 获取对应的 DataSource
        DataSource dataSource = lookupKey != null
                ? this.resolvedDataSources.get(lookupKey)
                : this.resolvedDefaultDataSource;

        if (dataSource == null) {
            // 3. 如果没找到，用默认的
            dataSource = this.resolvedDefaultDataSource;
        }

        // 4. 委托给目标 DataSource
        return dataSource.getConnection();
    }
}
```

### 常见实现：ThreadLocal 路由

```java
public class ThreadLocalRoutingDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> LOOKUP_KEY = new ThreadLocal<>();

    public static void setLookupKey(String key) {
        LOOKUP_KEY.set(key);
    }

    public static void removeLookupKey() {
        LOOKUP_KEY.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return LOOKUP_KEY.get();
    }
}
```

### 热替换 vs 路由

| 维度 | 热替换（DynamicDataSource） | 路由（AbstractRoutingDataSource） |
|------|---------------------------|----------------------------------|
| **切换粒度** | 全局（所有线程都切换） | 请求级（每个线程可不同） |
| **切换时机** | 配置变更 / Zone 切换 | 每次 getConnection() |
| **切换成本** | 秒级（子上下文 refresh） | µs 级（ThreadLocal lookup） |
| **DataSource 数量** | 1 个（当前活跃的） | N 个（所有可能的） |
| **适用场景** | 配置热更新、跨 Zone 容灾 | 读写分离、多租户隔离 |

**两者不是互斥的**，可以叠加使用：

```
DynamicDataSource（热替换层）
  └─ AbstractRoutingDataSource（路由层）
      ├─ DataSource A（Zone A）
      └─ DataSource B（Zone B）
```

DynamicDataSource 管理"配置版本"（配置变更时重建），AbstractRoutingDataSource 管理"Zone 路由"（每次请求时选择）。

---

## HikariCP 连接池机制详解

HikariCP 是 18-dynamic 默认的连接池（也是 Spring Boot 2.x+ 的默认连接池）。理解 HikariCP 的运行机制，对于理解 DynamicDataSource 的延迟关闭行为和连接池配置调优至关重要。

### HikariCP 的核心组件

```
HikariDataSource
  └─ HikariConfig
       ├─ jdbcUrl
       ├─ username / password
       ├─ maximumPoolSize / minimumIdle
       ├─ connectionTimeout / idleTimeout / maxLifetime
       └─ dataSourceClassName / dataSourceProperties
  └─ HikariPool
       ├─ ConcurrentBag<PoolEntry>  ← 连接容器
       ├─ ThreadPoolExecutor  ← 异步执行器
       └─ HouseKeeper  ← 维护任务
            ├─ 检查闲置连接 → 关闭超出的
            ├─ 检查过期连接 → 替换
            └─ 检查连接数 → 补充到 minimumIdle
```

### 连接的生命周期

```
                    getConnection() 调用
                           │
                           ▼
               ┌───────────────────┐
               │  获取 PoolEntry    │
               │  (从 ConcurrentBag │
               │   中获取)         │
               └────────┬──────────┘
                        │
               ┌────────▼──────────┐      ┌──────────────────┐
               │ 是否有可用连接？    │──否──→ 创建新连接         │
               └────────┬──────────┘      │  (最多到 maximum- │
                        │ 是              │   PoolSize)       │
                        ▼                 └────────┬─────────┘
               ┌──────────────────┐                │
               │  借用 PoolEntry   │←───────────────┘
               └────────┬─────────┘
                        │
               ┌────────▼─────────┐
               │  代理 Connection  │
               │  (ProxyConnection)│
               └────────┬─────────┘
                        │
               ┌────────▼─────────┐
               │     使用中...     │
               └────────┬─────────┘
                        │
               ┌────────▼─────────┐
               │  connection.close()│
               │  → ProxyConnection│
               │    → 归还到池中    │
               │    (ConcurrentBag)│
               └──────────────────┘
```

### ConcurrentBag：HikariCP 的高性能连接容器

HikariCP 最核心的性能优化在 `ConcurrentBag`——一个无锁并发容器：

```java
public class ConcurrentBag<T extends IConcurrentBagEntry> {

    // 1. 当前借出的连接（弱引用，避免阻止 GC）
    private final CopyOnWriteArrayList<T> sharedList;

    // 2. 可用的闲置连接（ThreadLocal 缓存）
    private final ThreadLocal<List<Object>> threadList;

    // 3. 等待连接的队列
    private final SynchronousQueue<T> handoffQueue;

    public T borrow(long timeout, final TimeUnit timeUnit) throws InterruptedException {
        // 先尝试从 ThreadLocal 获取（无锁）
        final List<Object> list = threadList.get();
        if (list != null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                final T entry = (T) list.remove(i);
                if (entry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
                    return entry;
                }
            }
        }

        // ThreadLocal 没有可用，遍历 sharedList 扫描
        // 仍未获取到，通过 handoffQueue 等待（最多 timeout）
        // ...
    }

    public void requite(final T entry) {
        entry.setState(STATE_NOT_IN_USE);
        // 尝试通过 handoffQueue 直接交给等待的消费者
        // offer 成功 → 消费者直接拿到，不需要放入 ThreadLocal
        // offer 失败（无等待消费者）→ 放入当前线程的 ThreadLocal
        final List<Object> threadLocalList = threadList.get();
        if (threadLocalList != null) {
            threadLocalList.add(entry);
        }
    }
}
```

**ThreadLocal 缓存**是 HikariCP 性能优于其他连接池的关键原因。归还连接时优先放回当前线程的 ThreadLocal，下次借用时从 ThreadLocal 获取——完全无锁操作。

### 连接验证

```java
// HikariCP 在 getConnection() 时的验证
// 如果配置了 connectionTestQuery 或 connectionInitSql
// 会在返回连接前执行验证 SQL

// 或者通过 JDBC 4.0 的 Connection.isValid() 验证
// 不需要额外 SQL，但需要 JDBC 驱动支持
```

HikariCP 的验证策略：

```java
// 验证时机
// 1. connectionTestQuery 配置时：连接从池中借出时执行验证 SQL（已废弃，推荐用 isValid()）
// 2. connectionInitSql 配置时：新连接创建时执行初始化 SQL
// 3. isValid() 方法：JDBC 4.0 驱动支持的轻量验证，无需额外 SQL

// 验证失败 → 关闭连接 → 重试获取下一个连接

// 验证超时：validationTimeout（默认 5000ms）
```

注意：HikariCP 默认不验证每个借出的连接（避免性能开销）。只有当连接空闲超过 `idleTimeout` 或配置了 `connectionTestQuery` 时，才在借出时验证。JDBC 4.0 的 `isValid()` 是推荐的验证方式，不需要额外配置 SQL。

### 关闭过程

当 HikariDataSource.close() 被调用时：

```java
public void close() {
    // 1. 标记为关闭状态
    // 2. 中断 HouseKeeper 定时任务
    // 3. 关闭所有连接
    pool.shutdown();
}

// HikariPool.shutdown()
// 1. 设置关闭标志（isClosing = true）
// 2. 中断所有等待 getConnection() 的线程（抛出 SQLException）
// 3. 逐条关闭所有 PoolEntry 中的物理连接
// 4. 关闭 ThreadPoolExecutor
// 5. 报告关闭完成
```

连接在关闭期间，正在执行的 SQL 会被中断（`Statement.cancel()` 或 `Socket.close()`）。这就是为什么 DynamicDataSource 需要 60s 延迟关闭——因为旧连接可能正在执行一个重要事务，不能被立即中断。

---

## 连接池配置调优

每个子上下文的连接池独立配置，在 DynamicJdbcConfig JSON 中通过 dataSource 字段的属性传递给 HikariCP。

### 可配置的连接池参数

```json
{
  "dataSource": [{
    "name": "order-ds",
    "type": "com.zaxxer.hikari.HikariDataSource",
    "url": "jdbc:mysql://host:3306/orders",
    "username": "root",
    "password": "root",
    "driverClassName": "com.mysql.cj.jdbc.Driver",
    "maximumPoolSize": 20,
    "minimumIdle": 5,
    "connectionTimeout": 30000,
    "idleTimeout": 600000,
    "maxLifetime": 1800000,
    "connectionTestQuery": "SELECT 1",
    "connectionInitSql": "SET NAMES utf8mb4"
  }]
}
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| maximumPoolSize | 10 | 最大连接数 |
| minimumIdle | 同 maximumPoolSize | 最小空闲连接数（默认等于 maximumPoolSize，即连接池维持全量） |
| connectionTimeout | 30000 | 等待连接的超时时间（ms） |
| idleTimeout | 600000 | 连接最大空闲时间（ms），仅当 minimumIdle < maximumPoolSize 时生效 |
| maxLifetime | 1800000 | 连接最大存活时间（ms），应小于 MySQL 的 wait_timeout（默认 28800s） |
| connectionTestQuery | 无 | 验证 SQL（已废弃，推荐 JDBC 4.0 的 isValid()） |
| connectionInitSql | 无 | 新连接创建时执行的 SQL |

### 经验配置值

**通用服务（10-50 QPS）**：
```properties
maximumPoolSize=20
minimumIdle=5
connectionTimeout=10000
idleTimeout=600000  # 10 分钟
maxLifetime=1800000  # 30 分钟
```

**高并发服务（100-500 QPS）**：
```properties
maximumPoolSize=50
minimumIdle=10
connectionTimeout=5000
idleTimeout=300000  # 5 分钟
maxLifetime=1800000  # 30 分钟
```

**批量处理服务（长 SQL）**：
```properties
maximumPoolSize=5
minimumIdle=2
connectionTimeout=60000  # 60 秒（SQL 可能执行久）
idleTimeout=600000
maxLifetime=1800000
```

### HikariCP 与 MySQL 的交互

MySQL 服务端有 `wait_timeout` 参数（默认 28800 秒 = 8 小时），如果连接空闲超过这个时间，MySQL 会主动关闭它。HikariCP 的 `maxLifetime` 应该小于 `wait_timeout`，以免拿到已被 MySQL 关闭的连接。

```properties
# MySQL 端
wait_timeout = 28800  # 8 小时

# HikariCP 端（小于 wait_timeout，留出余量。30-60 分钟是常见配置）
maxLifetime = 1800000  # 30 分钟（1800 秒）
```

注意：`maxLifetime` 设得越短，连接越频繁地重建，MySQL 端的连接创建开销越大。但设得太长（接近 `wait_timeout`），可能在连接刚好过期时被 MySQL 关闭，而 HikariCP 还没检测到就借给了应用。1800000ms（30 分钟）是一个保守值，大多数生产环境用 3600000ms（60 分钟）或更长。如果连接池空闲连接不多（minimumIdle 接近 maximumPoolSize），甚至可以设置到 12 小时以上——因为连接一直在活跃使用，不会到达 `wait_timeout`。

### 在子上下文中的连接池管理

每个子上下文有独立的 HikariCP 实例，意味着：

1. **独立的连接池统计**：活跃连接数、挂起请求数、超时次数
2. **独立的 HouseKeeper**：每个池有自己的后台维护线程
3. **独立的生命周期**：销毁时互不影响

在 DynamicDataSource 的延迟关闭期间，旧子上下文的 HikariCP 依然运行，旧连接池中的连接依然可用。60s 后 `closeContext()` 调用 `HikariDataSource.close()`，此时所有旧连接被关闭。

---

## HikariCP vs Tomcat DBCP vs DBCP2

18-dynamic 的 `DataSourceConfigurationPropertiesSynthesizer` 会根据连接池类型自动匹配配置前缀：

```java
private static Map<String, String> initDataSourcePropertiesPropertyNamePrefixes() {
    Map<String, String> prefixes = new HashMap<>(4);
    prefixes.put("org.apache.tomcat.jdbc.pool.DataSource", "spring.datasource.tomcat");
    prefixes.put("com.zaxxer.hikari.HikariDataSource", "spring.datasource.hikari");
    prefixes.put("org.apache.commons.dbcp2.BasicDataSource", "spring.datasource.dbcp2");
    return unmodifiableMap(prefixes);
}
```

### 三种连接池的对比

| 维度 | HikariCP | Tomcat DBCP | DBCP2 |
|------|----------|-------------|-------|
| **Spring Boot 默认** | 2.x+ | 1.x | 不默认 |
| **性能** | 最快 | 中等 | 慢 |
| **代码量** | 轻（~100KB） | 中等 | 重 |
| **连接验证** | `isValid()` (JDBC4) | `validationQuery` | `validationQuery` |
| **并发设计** | 无锁 ConcurrentBag | 有锁 | 有锁 |
| **JMX 监控** | 内置 | 需要配置 | 需要配置 |

### 为什么 18-dynamic 默认用 HikariCP

- 18-dynamic 的 `default.properties` 中 `DEFAULT_DATASOURCE_TYPE_NAME = HikariDataSource`
- Spring Boot 2.x 也默认 HikariCP
- HikariCP 的性能优势在子上下文频繁创建/销毁的场景中更明显

### 池类型的切换

```json
{
  "dataSource": [{
    "type": "org.apache.tomcat.jdbc.pool.DataSource",
    "url": "jdbc:mysql://host/db",
    ...
  }]
}
```

将 `type` 改为 Tomcat DBCP，Synthesizer 会自动匹配 `spring.datasource.tomcat.*` 前缀，合成正确的配置属性。

---

## JDBC URL 的标准化

18-dynamic 的 `JdbcURLAssembler` 负责将用户输入的 JDBC URL 标准化。

### 多字段兼容

用户可以通过三种字段名指定 JDBC URL：

```java
public static final String[] JDBC_URL_PROPERTY_NAMES = new String[]{
    "url",      // 标准名称
    "jdbc-url", // kebab-case
    "jdbcUrl"   // camelCase
};
```

`DataSourcePropertiesConfigPostProcessor.processDataSourceUrl()` 从 dataSourceProperties 中查找这三个字段，找到哪个用哪个：

```java
public Map.Entry<String, String> getDataSourceUrlEntry(Map<String, String> dataSourceProperties) {
    for (String name : DataSourceConstants.JDBC_URL_PROPERTY_NAMES) {
        String value = dataSourceProperties.get(name);
        if (StringUtils.hasText(value)) {
            return entry(name, value);
        }
    }
    return null;
}
```

### URL 标准化

```java
public String assemble(String rawJdbcURL) {
    // 1. 去除首尾空格
    String url = StringUtils.trim(rawJdbcURL);

    // 2. 如果 URL 没有 "jdbc:" 前缀，自动添加默认 scheme
    //    默认 scheme: jdbc:mysql//
    if (hasProtocol(url)) {
        url = url.substring(JDBC_URL_PREFIX_LENGTH);  // 去掉 "jdbc:"
    } else {
        url = defaultScheme + url;  // 添加默认 scheme
    }

    // 3. 解析 URI，处理查询参数
    URI uri = URI.create(url);
    MultiValueMap<String, String> queryParams = URLUtils.parseQueryParams(uri);

    // 4. 添加默认查询参数（如果用户没指定）
    setDefaultQueryParamsIfAbsent(queryParams);
    // 默认参数在 default.properties 中配置：
    // characterEncoding=utf-8, useSSL=false, useUnicode=true

    // 5. 重建 URL
    url = JDBC_URL_PREFIX + rebuildURL(uri, queryParams);
    return url;
}
```

### 标准化示例

```properties
# 用户输入
jdbc:mysql://localhost:3306/db?useSSL=true

# 标准化后
jdbc:mysql://localhost:3306/db?useSSL=true&characterEncoding=utf-8&useUnicode=true
```

如果用户没有输入 scheme，自动添加默认 scheme（`jdbc:mysql//`）：

```properties
# 用户输入（仅输入了 host:port/db）
localhost:3306/db

# 标准化后
jdbc:mysql://localhost:3306/db?characterEncoding=utf-8&useSSL=false&useUnicode=true
```

---

## DataSourceConfigurationPropertiesSynthesizer 的池化类型适配

这是第 5 步属性合成的关键组件，它连接了 DynamicJdbcConfig JSON 配置和 Spring Boot DataSource Auto-Configuration。

### 合成逻辑

```java
public class DataSourceConfigurationPropertiesSynthesizer
        extends AbstractModuleConfigConfigurationPropertiesSynthesizer {

    @Override
    protected boolean supports(DynamicJdbcConfig config, String module, Map<String, Object> properties) {
        // 如果配置了 ShardingSphere，跳过 DataSource 合成
        if (config.hasShardingDataSource()) {
            return false;
        }
        // 只处理单数据源情况
        return config.hasOnlySingleDataSource();
    }

    @Override
    protected void synthesize(DynamicJdbcConfig config, String module, Map<String, Object> properties) {
        // 获取当前 zone 的数据源属性
        List<Map<String, String>> dataSourcePropsList = config.getDataSourcePropertiesList();
        Map dataSourceProps = dataSourcePropsList.get(0);

        // 1. 合成 Spring Boot DataSourceProperties 绑定
        //    → spring.datasource.url, spring.datasource.username, ...
        synthesizeConfigurationProperties(module, DataSourceProperties.class, dataSourceProps, properties);

        // 2. 合成连接池专属属性
        //    → 根据 type 选择前缀
        //    HikariCP → spring.datasource.hikari.*
        //    Tomcat → spring.datasource.tomcat.*
        //    DBCP2 → spring.datasource.dbcp2.*
        synthesizeDataSourceProperties(module, dataSourceProps, properties);

        // 3. 合成模块排除配置
        synthesizeModuleExclusionAutoConfigurationProperty(module, properties);
    }
}
```

### DataSourceProperties 绑定

```java
private void synthesizeConfigurationProperties(String module, Class<?> configClass,
        Map<String, String> sourceProps, Map<String, Object> targetProps) {

    // 使用 Spring Boot DataSourceProperties 的字段定义
    // 从 sourceProps 中提取 name → url、username、password、driverClassName
    // 映射为 spring.datasource.xxx

    BeanWrapperImpl beanWrapper = new BeanWrapperImpl(DataSourceProperties.class);
    for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
        String propName = pd.getName();
        String sourceValue = sourceProps.get(propName);
        if (sourceValue != null) {
            targetProps.put("spring.datasource." + propName, sourceValue);
        }
    }
}
```

`DataSourceProperties` 是 Spring Boot 中代表 DataSource 配置的标准类，它定义了 `url`、`username`、`password`、`driverClassName`、`hikari` 等字段。Synthesizer 将 JSON 中的属性映射到这些标准字段上，使得 `DataSourceAutoConfiguration` 可以正确绑定。

### 池化类型感知

```java
private void synthesizeDataSourceProperties(String module,
        Map<String, String> dataSourceProps, Map<String, Object> properties) {
    // 根据 type 选择连接池专属前缀
    String type = getDataSourceType(dataSourceProps);
    String prefix = dataSourcePropertiesPropertyNamePrefixes.get(type);

    if (prefix != null) {
        // 将非标准属性（不属于 DataSourceProperties 的属性）
        // 放到连接池专属前缀下
        // 如 maximumPoolSize → spring.datasource.hikari.maximumPoolSize
        for (Map.Entry<String, String> entry : dataSourceProps.entrySet()) {
            String name = entry.getKey();
            if (!isDataSourcePropertiesPropertyName(name)) {
                String fullName = prefix + "." + name;
                properties.put(fullName, entry.getValue());
            }
        }
    }
}
```

这样区分的目的是——Spring Boot 的 `DataSourceAutoConfiguration` 只认 `spring.datasource.*` 下的标准属性，而 HikariCP 特有的属性（`maximumPoolSize`、`idleTimeout`）需要在 `spring.datasource.hikari.*` 下。Synthesizer 自动完成了这个拆分。

注意：这个 Synthesizer 只在非 ShardingSphere 场景下生效（`supports()` 检查 `hasShardingDataSource()` 时返回 `false`）。当配置了 ShardingSphere 时，数据源属性由 `ShardingSphereConfigConfigurationPropertiesSynthesizer` 合成为 `spring.shardingsphere.datasource.*` 前缀，而不是 `spring.datasource.*`。两者在处理管道的第 5 步通过 `supports()` 互斥。
