# 18-dynamic 模块深度分析

## 目录

- [Q1: 简历中的 3 点在代码中有对应吗？](#q1-简历中的-3-点在代码中有对应吗)
- [Q2: 这个项目能提取出什么简历点？](#q2-这个项目能提取出什么简历点)
- [Q3: 如果我来实现简历所对应的功能，会怎么做？](#q3-如果我来实现简历所对应的功能会怎么做)
- [Q4: 要讲透需要多少篇文档？](#q4-要讲透需要多少篇文档)

---

## Q1: 简历中的 3 点在代码中有对应吗？

### 1.1 "模块化设计：MyBatis、MyBatis-Plus、Transaction、多 DataSource、多活架构能力"

| 模块 | 代码中有没有？ | 实现方式 | 文件 |
|------|--------------|---------|------|
| MyBatis | ✅ 完整实现 | 4 个 SPI 各有一个 Mybatis 实现 | `mybatis/config/MybatisConfigPostProcessor.java` (空), `mybatis/validation/MybatisConfigValidator.java` (与 MP 互斥), `mybatis/env/MybatisConfigConfigurationPropertiesSynthesizer.java` (合成 mybatis.base-packages), `mybatis/context/MybatisConfigurationConfigBeanDefinitionRegistrar.java` (注册 MybatisMapperScanConfiguration) |
| MyBatis-Plus | ✅ 完整实现 | 同上模式，与 Mybatis 互斥 | `mybatisplus/` 下 4 个同源文件 |
| Transaction | ✅ 完整实现 | 注册 PlatformTransactionManager alias + customizer | `transaction/` 下 4 个文件 |
| 多 DataSource | ✅ 完整实现 | datasource 和 ha-datasource 两种模式，Jackson 反序列化为 `List<Map<String,Object>>` | `DynamicJdbcConfig.java` 的 `dataSource` / `highAvailabilityDataSource` 字段 |
| **多活架构能力** | ⚠️ **部分实现** | ZoneContext 集成仅做到"配置选择级"的多活，非"运行时路由级" | — |

**关键代码验证**：

MyBatis 和 MyBatis-Plus 的共存不是"在同一个上下文中共存"，而是通过 banned-modules 互斥 + 子上下文隔离实现的"在同一个进程中共存"：

```java
// MybatisConfigValidator.java
// 验证 mybatis 和 mybatis-plus 互斥（不能在同一个上下文中同时存在）
public class MybatisConfigValidator extends AbstractConfigurationConfigValidator<DynamicJdbcConfig.Mybatis> {
    @Override
    protected void doValidate(DynamicJdbcConfig dynamicJdbcConfig, ..., DynamicJdbcConfig.Mybatis config) {
        if (dynamicJdbcConfig.hasMybatisPlus()) {
            errors.addError("'mybatis' and 'mybatis-plus' can't be configured for one DataSource");
        }
    }
}
```

```properties
# default.properties 中配置的互斥
microsphere.dynamic.jdbc.modules.mybatis.auto-configuration.banned-modules=mybatis-plus
microsphere.dynamic.jdbc.modules.mybatis-plus.auto-configuration.banned-modules=mybatis
```

**所以"并存"的真实含义**：config A 用 MyBatis，config B 用 MyBatis-Plus，各在各自的子上下文中跑。**不是**同一个子上下文同时加载两个 ORM。

### 1.2 "动态 Spring 应用上下文：支持多活架构，能够做到 JDBC DataSource 动态更新，如：同区域优先、故障转移以及故障恢复。实现 JDBC 框架隔离和并存，如：MyBatis、MyBatis-Plus 或其他 ORM 框架"

| 子能力 | 代码中有没有？ | 分析 |
|-------|--------------|------|
| 动态 Spring 应用上下文 | ✅ | `DynamicJdbcChildContext extends AnnotationConfigApplicationContext` |
| JDBC DataSource 动态更新 | ✅ | `DynamicDataSource` 热替换，`initializeDataSource()` 重建子上下文 |
| **同区域优先** | ⚠️ 仅配置选择级 | `DynamicJdbcConfig.getDataSourcePropertiesList()` 根据 `ZoneContext.getZone()` 选择对应 zone 的数据源列表。**不是请求级路由**——zone 切换是通过重建整个 DataSource 实现的（秒级），不是每次查询 |
| **故障转移** | ❌ **代码中没有实现** | 见下方详细分析 |
| **故障恢复** | ❌ **代码中没有实现** | 见下方详细分析 |
| JDBC 框架隔离和并存 | ✅ | 子上下文机制，不同 config 各自独立跑 Auto-Configuration |

**"故障转移/故障恢复"的真相**：

我在全代码库中搜索了 `failover`、`failback`、`health`、`retry`、`circuit`、`unavailable` 等关键词，**无任何匹配**。

代码中唯一的"降级"逻辑是 `DynamicJdbcConfig.getDataSourcePropertiesList()` 的 DEFAULT_ZONE 回退：

```java
// DynamicJdbcConfig.java:229-244
public final List<Map<String, String>> getDataSourcePropertiesList() {
    if (hasHighAvailabilityDataSource()) {
        String zone = zoneContext.getZone();
        dataSourcePropertiesList = highAvailabilityDataSourcePropertiesMap.get(zone);
        if (CollectionUtils.isEmpty(dataSourcePropertiesList)) {
            // 如果请求的 zone 在 map 中不存在，回退到 defaultZone
            return highAvailabilityDataSourcePropertiesMap.get(DEFAULT_ZONE);
        }
    }
    ...
}
```

**但这只是配置级别的 fallback**（key 在 Map 中不存在时的防御），不是运行时健康检查后的故障转移。

真正的故障转移需要：
1. 健康检查（定期验证数据库可达）
2. 故障检测（连续失败 → 熔断）
3. 自动切换（检测到故障后自动发布 ZoneContextChangedEvent）
4. 恢复检测（定期检查故障 zone 是否恢复）
5. 自动回切（恢复后自动切回）

**这些代码都没有。**

那这个项目怎么实现"故障转移"呢？依赖外部：
- 外部系统检测到数据库故障
- 外部系统更新 `ZoneContext` 的 zone 值
- 这触发 `ZoneContextChangedEvent` → `PropagatingDynamicJdbcConfigChangedEventListener` → `DynamicJdbcConfigChangedEvent` → `DynamicDataSource` 重建

所以"故障转移"的能力实际上在外部系统，不在这个模块中。

**"同区域优先"的真相**：

代码中没有 `AbstractRoutingDataSource` 风格的请求级路由（没有 lookupKey，没有 determineCurrentLookupKey）。"同区域优先"是通过**配置选择**实现的——zone 变化时重建 DataSource。

这意味着切换代价很高（子上下文 refresh），不适合频繁切换场景。

### 1.3 "支持 Apache ShardingSphere 5.x"

| 能力 | 代码中有没有？ | 证据 |
|------|--------------|------|
| 版本 | ✅ 5.1.1 | `pom.xml`: `<shardingsphere.version>5.1.1</shardingsphere.version>` |
| 依赖 | ✅ | `shardingsphere-jdbc-core-spring-boot-starter` |
| YAML 配置加载 | ✅ | `DynamicJdbcUtils.loadShardingSphereYamlRootConfiguration()` 使用 `YamlEngine.unmarshal()` |
| ModeConfiguration 注册 | ✅ | `ShardingSphereConfigurationConfigBeanDefinitionRegistrar` 用 `ModeConfigurationYamlSwapper` 转换并注册 |
| RuleConfiguration 注册 | ✅ | `YamlRuleConfigurationSwapperEngine.swapToRuleConfiguration()` 转换所有规则 |
| 属性合成 | ✅ | `ShardingSphereConfigConfigurationPropertiesSynthesizer` 合成 `spring.shardingsphere.datasource.*` + `spring.shardingsphere.props.*` |
| Shutdown hook | ✅ | `SyncExecutionShutdownHookApplicationListener` + `ShardingSphereShutdownHookThreadFilter` |
| DynamicDataSource 兼容 | ✅ | ShardingSphere 在子上下文内创建 `ShardingSphereDataSource`，DynamicDataSource 提取该 DataSource 作为 delegate |

**但有一些限制**（见 Q3 分析）。

### Q1 总结

| 简历点 | 实际状态 |
|--------|---------|
| 模块化设计 | ✅ |
| 多 DataSource | ✅ |
| MyBatis/MyBatis-Plus 并存 | ✅（通过子上下文隔离，非同一上下文） |
| 多活架构 | ⚠️ 配置级 zone 感知，非运行时路由 |
| 同区域优先 | ⚠️ 配置选择，非请求级路由 |
| 故障转移 | ❌ 无健康检查/熔断/自动切换 |
| 故障恢复 | ❌ 无自动回切 |
| ShardingSphere 5.x | ✅ |

---

## Q2: 这个项目能提取出什么简历点？

### 2.1 可以如实写的

**1. SPI 插件化框架设计**

设计并实现了 4 个 SPI 扩展点（ConfigPostProcessor、ConfigValidator、ConfigConfigurationPropertiesSynthesizer、ConfigBeanDefinitionRegistrar），采用 3 层抽象链（Base → Module → Configuration），通过 `spring.factories` 实现插件化加载。新增一个模块只需添加 Inner Config 类 + 4 个 SPI 实现 + 在 factories 中注册，框架自动发现 `@Module` 注解。

**为什么值得写**：这展示了框架设计能力——你设计的不是一次性功能，而是一个让其他人也能扩展的开放体系。

**2. Spring Boot Auto-Configuration 源码级扩展**

深入研究 Spring Boot 的 `AutoConfigurationImportFilter`、`AutoConfigurationImportListener` 和 `AutoConfigurationImportSelector` 三个扩展点，实现了子上下文的 Auto-Configuration 过滤与缓存机制，支持模块间互斥（banned-modules）。通过 `OnceMainApplicationPreparedEventListener` 在 `ApplicationPreparedEvent` 阶段动态注入 `spring.autoconfigure.exclude`。

**为什么值得写**：大多数开发者只用过 `@SpringBootApplication(exclude=...)`，这个项目展示了如何在运行时用代码动态控制 Auto-Configuration——这是对 Spring Boot 机制非常深入的理解。

**3. 子 Spring 上下文隔离架构**

使用 `AnnotationConfigApplicationContext` 作为隔离单元，每个数据源配置运行在独立的子上下文中。子上下文继承父上下文 Environment，独立运行 Auto-Configuration，通过 `DynamicJdbcChildContextRefreshedListener` 选择性暴露 Bean 到父上下文。支持多 config 并行初始化（`ThreadPoolExecutor` + `InitializeErrors` 错误收集）。

**为什么值得写**：展示了 Spring 容器的高级用法——大多数人认为 ApplicationContext 是"系统级"不可创建的，这个项目展示了它是可编程创建和管理的。

**4. 热替换 DataSource 设计与线程安全**

实现 `DynamicDataSource`（`javax.sql.DataSource`），支持运行时无感切换：深克隆配置 → 新建子上下文 → 原子替换 delegate（`volatile` + `synchronized`）→ 延迟关闭旧上下文（`ScheduledExecutorService`，默认 60s）。Zone 变更时通过事件链自动触发切换。

**为什么值得写**：涉及线程安全（volatile + mutex + 延迟关闭）、事件驱动（4 级事件传播链）、资源生命周期管理（子上下文关闭调度）。

**5. Tomcat 连接池 → HikariCP 配置标准化与 URL 组装**

实现 `JdbcURLAssembler` 统一规范化 JDBC URL（添加默认 scheme、默认查询参数、多字段别名映射：`url`/`jdbcUrl`/`jdbc-url`）。`DataSourceConfigurationPropertiesSynthesizer` 根据连接池类型自动选择 `spring.datasource.hikari.*` / `spring.datasource.tomcat.*` / `spring.datasource.dbcp2.*` 前缀。

**为什么值得写**：展现了配置兼容性设计的工程思维——用户写 `url` 或 `jdbc-url` 都行，用什么连接池就自动配对应前缀。

### 2.2 不建议写在简历上的

- **"多活架构"**——这个项目只有 ZoneContext 配置读取，没有运行时多活路由
- **"故障转移/故障恢复"**——代码中没有对应实现
- **"同区域优先"**——不是运行时路由，说出来会被面试官问倒

### 2.3 简历点与代码的对应关系

| 简历点 | 对应源码 | 真实度 |
|--------|---------|--------|
| SPI 插件化框架 | `ConfigPostProcessor.java` + `AbstractConfigPostProcessor` + `AbstractModuleConfigPostProcessor` + `AbstractConfigurationConfigPostProcessor` | 真实，且是亮点 |
| Spring Boot Auto-Configuration 扩展 | `DynamicJdbcAutoConfigurationImportSelector.java` + `Filter` + `Listener` + `Repository` | 真实，且是亮点 |
| 子上下文隔离 | `DynamicJdbcChildContext.java` + `DynamicJdbcChildContextRefreshedListener.java` | 真实 |
| 热替换 DataSource | `DynamicDataSource.java` | 真实 |
| 模块互斥与并存 | `banned-modules` 机制 + Validator 互斥检查 | 真实 |
| ShardingSphere 5.x 集成 | `ShardingSphere*.java` 4 个文件 | 真实 |
| 配置标准化 | `JdbcURLAssembler.java` + `ConfigurationPropertiesFlatter.java` + `DataSourceConstants.java` | 真实 |

---

## Q3: 如果我来实现简历所对应的功能，会怎么做？

### 3.1 关于"故障转移/故障恢复"

简历声称的功能比实际代码多。如果我要真正实现"故障转移+故障恢复"，我会在现有架构基础上增加：

**新增：HealthCheckExecutor**

```
DynamicDataSource
  └─ HealthCheckExecutor (ScheduledExecutorService)
       ├─ 定期执行 connection.validate() / connection.isValid(5)
       ├─ 连续失败 N 次 → 标记 zone 为 DEGRADED
       ├─ 连续成功 M 次 → 标记 zone 为 RECOVERED
       └─ 状态变化 → 自动发布 ZoneContextChangedEvent
```

```java
public class HealthCheckExecutor {

    private final Map<String, ZoneHealthState> zoneStates = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler;

    private static final int FAILURE_THRESHOLD = 3;

    private static final int SUCCESS_THRESHOLD = 5;

    private static final Duration CHECK_INTERVAL = Duration.ofSeconds(10);

    private final ConfigurableApplicationContext context;

    public HealthCheckExecutor(ConfigurableApplicationContext context) {
        this.context = context;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startZoneHealthCheck(Map<String, DataSource> zoneDataSources) {
        scheduler.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, DataSource> entry : zoneDataSources.entrySet()) {
                checkZone(entry.getKey(), entry.getValue());
            }
        }, 0, CHECK_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void checkZone(String zone, DataSource dataSource) {
        boolean healthy = false;
        try (Connection conn = dataSource.getConnection()) {
            healthy = conn.isValid(5);
        } catch (SQLException e) {
            // zone unreachable, mark as unhealthy
        }
        recordResult(zone, healthy);
    }

    private void recordResult(String zone, boolean success) {
        ZoneHealthState state = zoneStates.computeIfAbsent(zone, k -> new ZoneHealthState());
        state.record(success);

        if (state.shouldFailover()) {
            String healthyZone = findHealthyZone(zone);
            if (healthyZone != null) {
                publishZoneChanged(healthyZone);
            }
        } else if (state.shouldFailback()) {
            // original primary zone recovered
            publishZoneChanged(zone);
        }
    }

    private void publishZoneChanged(String zone) {
        ZoneContext.get().setZone(zone);
        // triggers ZoneContextChangedEvent → DynamicJdbcConfigChangedEvent
        // → DynamicDataSource hot-swap
    }

    private String findHealthyZone(String failedZone) {
        return zoneStates.entrySet().stream()
            .filter(e -> !e.getKey().equals(failedZone))
            .filter(e -> e.getValue().isHealthy())
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    static class ZoneHealthState {
        private int consecutiveFailures = 0;
        private int consecutiveSuccesses = 0;
        private boolean failed = false;

        void record(boolean success) {
            if (success) {
                consecutiveSuccesses++;
                consecutiveFailures = 0;
            } else {
                consecutiveFailures++;
                consecutiveSuccesses = 0;
            }
        }

        boolean shouldFailover() {
            return !failed && consecutiveFailures >= FAILURE_THRESHOLD;
        }

        boolean shouldFailback() {
            return failed && consecutiveSuccesses >= SUCCESS_THRESHOLD;
        }

        boolean isHealthy() {
            return !failed;
        }
    }
}
```

**改动范围**：
- 新增 `HealthCheckExecutor.java`
- 在 `DynamicDataSource.initializeDataSource()` 中启动健康检查
- 在 destroy() 中关闭 scheduler
- `DynamicJdbcConfig` 增加 `health-check` 配置段（interval、threshold、validation-query）

### 3.2 关于"同区域优先"的运行时路由

当前实现是"zone 切换 → 重建 DataSource"，每次切换秒级。如果要做真正的**请求级同区域优先**，我会叠加两层：

**第一层：ZoneRoutingDataSource（请求级路由）**

在 DynamicDataSource 和实际 DataSource 之间加一层：

```
DynamicDataSource (热替换壳，仅配置变更时重建)
  └─ ZoneRoutingDataSource (请求级路由，每次 getConnection 决定)
       ├─ zone-a → HikariDataSource[master-a]
       └─ zone-b → HikariDataSource[master-b]
```

```java
public class ZoneRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<String, DataSource> zoneDataSources;

    private final String defaultZone;

    public ZoneRoutingDataSource(Map<String, DataSource> zoneDataSources, String defaultZone) {
        this.zoneDataSources = zoneDataSources;
        this.defaultZone = defaultZone;
        setTargetDataSources(new HashMap<>(zoneDataSources));
        setDefaultTargetDataSource(zoneDataSources.get(defaultZone));
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // 每次 getConnection() 时获取当前 zone
        // 这是 µs 级的，不需要重建子上下文
        String zone = ZoneContext.get().getZone();
        return zone != null && zoneDataSources.containsKey(zone) ? zone : defaultZone;
    }
}
```

这样 zone 切换是 µs 级的（只是 ThreadLocal 读取 → Map 查找 → 返回已有 DataSource），不需要重建子上下文。

**第二层：DynamicDataSource 只做配置热更新**

DynamicDataSource 只负责"配置变更时重建子上下文"，不负责"zone 切换时重建"。Zone 切换由 ZoneRoutingDataSource 处理。

```java
public class LayeredDynamicDataSource implements DataSource {

    private final Object mutex = new Object();

    // 内部持有路由 DataSource
    private volatile ZoneRoutingDataSource zoneRoutingDataSource;

    // 配置变更时重建（秒级），zone 切换时不重建
    public void onConfigChanged(DynamicJdbcConfig newConfig) {
        // 为每个 zone 创建对应的 DataSource
        Map<String, DataSource> perZoneDS = new HashMap<>();
        for (String zone : newConfig.getHighAvailabilityDataSource().keySet()) {
            DynamicJdbcConfig zoneConfig = buildZoneConfig(newConfig, zone);
            DataSource ds = buildDataSourceForZone(zoneConfig);
            perZoneDS.put(zone, ds);
        }

        ZoneRoutingDataSource newRouting = new ZoneRoutingDataSource(perZoneDS, DEFAULT_ZONE);
        synchronized (mutex) {
            this.zoneRoutingDataSource = newRouting;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        // 每次请求都走路由，毫秒级
        return zoneRoutingDataSource.getConnection();
    }
}
```

**改动范围**：新增 `ZoneRoutingDataSource.java`（继承 AbstractRoutingDataSource，约 60 行），修改 `DynamicDataSource.java` 增加分层逻辑。

### 3.3 关于子上下文重建的性能优化

当前方案每次子上下文 refresh 需要几秒。优化方案：

**预建池（Pre-warming）**

```java
private volatile Future<DynamicJdbcChildContext> prebuildingContext;

private DataSource initializeDataSource(...) {
    // 在后台预建新上下文
    DynamicJdbcChildContext newContext = prebuildChildContext();
    synchronized (mutex) {
        DataSource newDS = extractDataSource(newContext);
        DataSource oldDS = this.delegate;
        this.delegate = newDS;
        this.childContext = newContext;
        closeAsync(oldDS, oldContext); // 60s 延迟
    }
    return newDS;
}

// 后台预建：配置变更事件触发后立即开始，不阻塞当前 DataSource
private void triggerPrebuild(DynamicJdbcConfig config) {
    prebuildingContext = executor.submit(() -> buildChildContext(config));
}
```

**减少不必要的 refresh**：当前 `mergeParentEnvironment()` 每次都合并整个 Environment。可以改为增量合并——只合并变化的属性。

### 3.4 关于事务安全的 Zone 切换

借鉴 my-xhs 的 ConnectionWrapper 模式：

```java
public class TransactionAwareDynamicDataSource implements DataSource {

    private final AtomicInteger activeConnections = new AtomicInteger(0);

    private final Object mutex = new Object();

    private volatile DataSource delegate;

    @Override
    public Connection getConnection() throws SQLException {
        activeConnections.incrementAndGet();
        try {
            Connection conn = delegate.getConnection();
            return new ConnectionWrapper(conn, activeConnections);
        } catch (SQLException e) {
            activeConnections.decrementAndGet();
            throw e;
        }
    }

    public void switchDataSource(DataSource newDelegate) {
        // 等待活跃连接完成，最长 30s
        waitForActiveConnections(30, TimeUnit.SECONDS);

        synchronized (mutex) {
            DataSource old = this.delegate;
            this.delegate = newDelegate;
            // 旧连接继续使用旧 delegate，新连接走新 delegate
        }
    }

    private void waitForActiveConnections(long timeout, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline && activeConnections.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

### 3.6 关于整体架构的改进建议汇总

| 改进点 | 优先级 | 原因 |
|--------|--------|------|
| ZoneRoutingDataSource（请求级路由） | P0 | 当前 zone 切换要秒级重建，不合理 |
| HealthCheckExecutor（故障检测） | P0 | 简历声称有故障转移但实际没有 |
| ConnectionWrapper（事务安全切换） | P1 | 当前没有事务协调，60s 延迟可能不够 |
| 子上下文预建池（性能优化） | P1 | 重建成本高，预建可降低延迟 |
| YAML 解析缓存 | P2 | 当前 YAML 被解析两次 |
| Cluster mode 配置示例 | P2 | 只有 Memory/Standalone 示例 |

---

## Q4: 要讲透需要多少篇文档？

这个模块涉及 3 个知识域。每个知识域内的文档有明确的前置依赖关系，必须按序阅读。

### 知识域 A：业务/架构知识——"为什么要这么设计"

**阅读顺序**：18-01 → 18-02 → 18-03

| 文档 | 内容 | 前置依赖 |
|------|------|---------|
| **18-01 多活架构基础** | 同城双活 vs 异地多活，Zone/Region 概念，数据库层的多活面临的核心挑战：主从同步延迟、脑裂问题、failover 策略（计划内切换 vs 故障切换）、数据一致性模型，这些挑战如何驱动了 18-dynamic 的设计决策 | 无 |
| **18-02 数据库框架共存问题** | 为什么企业需要同时使用 MyBatis 和 MyBatis-Plus（迁移期、多租户、技术异构），ORM 框架在 Spring Boot 中的 Auto-Configuration 冲突机制，banned-modules 设计思想的来源，子上下文隔离为什么是解决框架共存的可行方案 | 18-01 |
| **18-03 ShardingSphere 5.x 架构** | ShardingSphere 的三层架构（Driver → JDBC → Spring Boot），5.x 的 Mode 体系（Memory/Standalone/Cluster），YAML 配置的结构化模型，ShardingSphere 与 Spring Boot Auto-Configuration 的集成方式 | 无 |

### 知识域 B：技术原理——"每个技术点的底层机制"

**阅读顺序**：18-04 → 18-05 → 18-06 → 18-07

| 文档 | 内容 | 前置依赖 |
|------|------|---------|
| **18-04 Spring 子上下文深度** | `AnnotationConfigApplicationContext` 的完整生命周期：构造 → BeanFactory 准备 → postProcessBeanFactory → 注册 BeanPostProcessor → refresh → finishRefresh。父子上下文的 Environment 合并规则（`merge()` 的语义、PropertySource 优先级、去重机制）。Bean 的跨上下文升迁：为什么子 Bean 需要注册到父上下文、基础设施 Bean 的识别和过滤、作用域传播。自定义 BeanNameGenerator | 18-01 |
| **18-05 Auto-Configuration 内部机制** | `AutoConfigurationImportSelector.selectImports()` 的完整过程：加载 `AutoConfiguration.imports` → 过滤（Filter chain）→ 监听（Listener）→ 去重 → ClassLoader 校验。`AutoConfigurationImportFilter` 的调用时机和生命周期。`AutoConfigurationImportListener` 的事件内容。Spring Factories 机制的 SPI 加载细节。与 `@Conditional` 的关系 | 18-01, 18-03 |
| **18-06 JDBC 与连接池抽象** | `javax.sql.DataSource` 接口体系的设计意图。`AbstractRoutingDataSource` 的路由策略 vs 热替换模式的取舍。HikariCP 的池化机制（minimumIdle、maximumPoolSize、connectionTimeout 等参数的含义和调优）。HikariCP vs Tomcat DBCP vs DBCP2 的配置差异和选型依据 | 无 |
| **18-07 线程安全与并发设计** | `volatile` 在 DataSource 热替换中的作用（可见性保障 + 禁止指令重排）。`synchronized(mutex)` 原子切换的临界区保护。`ConcurrentHashMap` 在 InitializeErrors 中的并发收集。`ScheduledExecutorService` 延迟关闭的资源管理。`ThreadPoolExecutor` 并行创建子上下文的线程池配置 | 无 |

### 知识域 C：源码分析——"代码具体怎么实现的"

**阅读顺序**：18-08 → 18-09 → 18-10

| 文档 | 内容 | 前置依赖 |
|------|------|---------|
| **18-08 18-dynamic 逐模块源码解析** | 从 `main()` 到 DataSource 就绪的完整代码路径：`DynamicJdbcContextApplicationListener` 入口 → 配置加载 → 子上下文创建 → `DynamicJdbcContextProcessor` 6 步管道（每步为什么在这个顺序、SPI 加载方式、错误处理策略）。`DynamicJdbcChildContext` 生命周期：postProcessBeanFactory 中每个步骤的意图。`DynamicDataSource` 完整生命周期：`afterPropertiesSet` → `getDelegate` → `initializeDataSource` → 深克隆 → 子上下文 refresh → delegate 原子替换 → 延迟关闭。RefreshingDynamicDataSourceListener 的事件响应链路。Bean 升迁的完整流程：`DynamicJdbcChildContextRefreshedListener` 的遍历逻辑、暴露策略、@Primary 标记 | 18-04, 18-05, 18-06, 18-07 |
| **18-09 配置系统源码解析** | JSON → POJO 反序列化（ObjectMapper 的配置细节、`@JsonProperty` 映射、`@JsonIgnore` 排除、`DynamicJdbcConfig.Config` 基类的继承序列化）。ConfigPostProcessor 链如何处理 POJO（名称填充、类型默认值、前一个填充行为的逐行分析）。ConfigConfigurationPropertiesSynthesizer 链如何将 POJO 转为 Spring 属性（AbstractModuleConfigConfigurationPropertiesSynthesizer 的骨架 + DataSource/Transaction/ShardingSphere/MyBatis/MyBatis-Plus 各实现的差异化逻辑）。属性名别名系统（url/jdbcUrl/jdbc-url 的三字段映射）。JdbcURLAssembler 的 URL 标准化（scheme 添加、默认查询参数注入） | 18-06 |
| **18-10 事件驱动源码解析** | 完整事件链：`ZoneContextChangedEvent` (来自 17-multiactive) → `PropagatingDynamicJdbcConfigChangedEventListener` → `DynamicJdbcConfigChangedEvent` → `DynamicDataSource.RefreshingDynamicDataSourceListener` → `initializeDataSource()`。`PropertySourcesChangedEvent` 的配置热更新路径。`ContextRefreshedEvent` 的 Bean 升迁路径。`ContextClosedEvent` 的子上下文自动关闭路径（`registerParentContextClosedEventListener`）。`ApplicationStartedEvent` 的 ShardingSphere shutdown hook 同步执行路径（`SyncExecutionShutdownHookApplicationListener`）。每个 Listener 的 `supportsEventType` 配置和 order 执行顺序 | 18-04, 18-08 |

### 阅读路线图

```
第一阶段：理解业务背景
  18-01 多活架构基础
  18-02 数据库框架共存问题
  18-03 ShardingSphere 5.x 架构

第二阶段：理解技术原理
  18-04 Spring 子上下文深度      ← 前置 18-01
  18-05 Auto-Configuration 机制  ← 前置 18-01, 18-03
  18-06 JDBC 与连接池抽象
  18-07 线程安全与并发设计

第三阶段：理解源码实现
  18-08 18-dynamic 逐模块源码    ← 前置 18-04, 18-05, 18-06, 18-07
  18-09 配置系统源码解析         ← 前置 18-06
  18-10 事件驱动源码解析         ← 前置 18-04, 18-08
```

共计 **11 篇**：3 业务 + 4 技术 + 3 源码 + 1 篇综合分析（本文）。

---

## 已产出文档

| 文档 | 完成 |
|------|------|
| 18-microsphere-dynamic-analysis.md（本文：Q1-Q4 综合分析） | ✅ |
| 18-01-multi-active-architecture-basics.md | ✅ |
| 18-02-database-framework-coexistence.md | ✅ |
| 18-03-shardingsphere-architecture.md | ✅ |
| 18-04-spring-child-context-deep-dive.md | ✅ |
| 18-05-auto-configuration-internals.md | ✅ |
| 18-06-jdbc-and-connection-pool-abstraction.md | ✅ |
| 18-07-thread-safety-and-concurrency-design.md | ✅ |
| 18-08-module-source-code-walkthrough.md | ✅ |
| 18-09-configuration-system-source-code.md | ✅ |
| 18-10-event-driven-source-code.md | ✅ |
