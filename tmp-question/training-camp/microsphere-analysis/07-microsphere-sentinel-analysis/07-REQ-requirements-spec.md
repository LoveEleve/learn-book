# 07-REQ：microsphere-sentinel 完整需求规格（v2，Sentinel 官方源码验证）

> v2 更新：官方适配器清单验证 + BlockException/context 泄漏源码确认 + N01/N03/N04 falsified 保留 + 4 个新发现
>
> 所有需求分三类：
> 1. **已实现需求**（REQ-001~006，6 项）
> 2. **待修复**（REQ-D01~D10，10 项）——含 4 个运行期阻断
> 3. **全新发散**（REQ-N01~N02，2 项）
>
> **基准环境**：Java 17+，Sentinel 1.8.x，对照源码 `/data/workspace/source-code/code/spring/sentinel/`

---

### Sentinel 官方适配器覆盖验证

官方 `sentinel-adapter/` 下已有：webmvc(-v6x)/webflux/web-servlet/gateway(-v6x)/dubbo/dubbo3/motan/sofa-rpc/grpc/jax-rs/okhttp/apache-httpclient/zuul/zuul2/quarkus/reactor。
**官方没有 mybatis/druid/hibernate/p6spy/redis 5 类——微球 5 个插件确为真增量。**

官方 `sentinel-extension/` 下 datasource-nacos、prometheus-metric-exporter、parameter-flow-control 均存在——**v1 的 N01/N03/N04 "官方缺失"声称已全部 falsified，v2 不再包含。**

---

## 项目定位

**microsphere-sentinel 是 Sentinel 的 Spring Boot 集成层**——不是重写 Sentinel，而是把 Sentinel 的流控能力"贴"到 6 个基础设施框架的**原生扩展点**上：Druid Filter、MyBatis ExecutorFilter、Hibernate EntityCallback、P6Spy JdbcEventListener、Spring RedisConnectionInterceptor、Web HandlerMethodInterceptor。全部通过 SPI 自动注册，加依赖即生效。

与 06-microsphere-nacos 的本质区别：06 是重写官方 SDK 的 thin HTTP wrapper（被废弃），07 是**把官方 SDK 的能力扩展到官方没覆盖的领域**——官方 sentinel-adapter 只有 webmvc/webflux/gateway，microsphere 补了 MyBatis/Druid/Hibernate/P6Spy/Redis 这 5 类适配器。

**源码信息**：
- 路径：`/data/workspace/java-training-camp/cloud-native-code/share/microsphere-sentinel/`（与 microsphere-alibaba-sentinel 内容一致）
- 模块：`commons`（11）+ `plugins`×6（12）+ `spring-boot`（6）+ `spring-cloud`（1）
- 官方 Sentinel 源码对照：`/data/workspace/source-code/code/spring/sentinel/`（907 文件）

### 与官方 Sentinel 的对比

| 能力 | 官方 sentinel-adapter | microsphere-sentinel |
|------|:---:|:---:|
| Spring Web（WebMVC/WebFlux） | ✅ 有 | ✅ 有（实现不同） |
| MyBatis 语句级流控 | **无** | ✅ 独有 |
| Druid Filter 级流控 | **无** | ✅ 独有 |
| Hibernate EntityCallback 流控 | **无** | ✅ 独有 |
| P6Spy JDBC 流控 | **无** | ✅ 独有 |
| Redis 命令级流控 | **无** | ✅ 独有 |
| 插件 SPI + JMX MBean 管理 | **无** | ✅ 独有 |
| 模板 API（免 try/catch 样板） | **无** | ✅ 独有 |

---

## 一、插件 SPI 与模板 API

### REQ-001：统一插件模型 + 流控模板方法

**问题**：Sentinel 官方的 `SphU.entry()` → `try/catch BlockException` → `finally entry.exit()` 样板代码在每个拦截点都要重复写。而且 6 个不同的框架扩展点（Filter/Interceptor/Callback）各有一套不同的上下文传递方式。需要一个统一的上层抽象。

**产出**：
- `SentinelPlugin` SPI：`name/contextName/origin/resourceType/trafficType/enabled`——统一的插件元数据模型。`install()` 自动注册到插件仓库 + JVM shutdown hook `uninstall()` 自动清理
- `SentinelPluginRepository`：SPI 仓库——`getPlugins()/getInstalledPlugins()/install/uninstall`
- `JMXSentinelPluginRepository`（SPI 默认实现）：每个插件注册为 `StandardMBean`（JMX 域 `io.microsphere.sentinel`）——运维可通过 JConsole 查看/管理插件状态
- `SentinelContext`：ThreadLocal 请求上下文——`resourceName/contextName/origin/Entry/result/failure/attributes`，`withinContext()`/`doInContext()` 函数式包裹
- `SentinelOperations` 接口：`call(Callable)` / `execute(Runnable)` 各 4 个重载——带/不带 contextName/origin/trafficType；两阶段 `begin()/end()`
- `SentinelTemplate`：`begin()` = `ContextUtil.enter()` + `SphU.entry()`；`end()` = `Tracer.traceEntry()` + `entry.exit()` + `ContextUtil.exit()`

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P1：SentinelTemplate.begin() 中 `ContextUtil.enter()` 先于 `SphU.entry()`**——entry 抛 `BlockException` 时 context 已创建但永不 exit（context 泄漏）
- **❌ P1：`SentinelOperations.call()` 中 `begin()` 在 try 块之外**——如果 begin() 抛 BlockException，finally 中的 end() 永远不会执行
- `SentinelUtils` 通过 `MethodHandle` 调包私有 `ContextUtil.resetContextMap()` 和私有 `FlowRuleManager.SCHEDULER`——依赖 Sentinel 内部实现

**配置规格**：通过 `JMXSentinelPluginRepository` 自动注册 JMX MBean。

---

## 二、数据库访问适配器

### REQ-002：Druid/Hibernate/MyBatis/P6Spy 四层数据库流控

**问题**：生产环境中数据库是最容易被打垮的资源——一条慢 SQL 可能导致连接池耗尽。Sentinel 官方只提供了 Web 层面的流控，对数据库访问层（JDBC/ORM）完全没有任何保护。需要把 Sentinel 的流控能力嵌入到数据库访问链路上，在每条 SQL 执行前判断是否限流。

**产出**（4 个插件，6 文件）：

| 插件 | 扩展点 | 资源名 | 拦截范围 |
|------|--------|--------|---------|
| `SentinelAlibabaDruidFilter` | Druid `Filter` SPI | SQL 解析：`SELECT users`/`UPDATE orders` | `beforeExecute()` → `begin()` + `withinContext()` |
| `SentinelMyBatisExecutorFilter` | MyBatis `ExecutorFilter` SPI | `MappedStatement.getId()` （如 `com.foo.UserMapper.selectById`） | `update()/query()` 两分组 |
| `SentinelHibernateEntityCallback` | Hibernate `EntityCallback` SPI | `Entity:ACTION:ClassName`（如 `Entity:INSERT:User`） | onPre/Post Insert/Update/Load/Delete 六组 |
| `SentinelJdbcEventListener` | P6Spy `JdbcEventListener` SPI | SQL 原文 | 仅 `PreparedStatementInformation` |

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P1：Druid 插件 BlockException 被包装成 SQLException**——调用方无法识别"被流控" vs "SQL 出错"
- **❌ P1：P6Spy/Hibernate 插件 BlockException 被包装成 RuntimeException**（通过 `ThrowableAction.execute` 的 handleException 无差别包装）
- **只有 MyBatis 插件正确传播原始 BlockException**
- P6Spy 只拦截 PreparedStatement（不拦 Statement/CallableStatement）
- MyBatis 插件同时注册 SPI + Spring Bean——可能双重计数

**配置规格**：
```properties
microsphere.sentinel.druid.enabled = true
microsphere.sentinel.mybatis.enabled = true
microsphere.sentinel.hibernate.enabled = true
microsphere.sentinel.p6spy.enabled = true
```

---

## 三、Redis 适配器

### REQ-003：Redis 命令级流控

**问题**：Redis 虽然是内存操作，但高并发下的热点 key 访问仍然可能打满 Redis 连接或 CPU。Sentinel 官方没有 Redis 适配器。需要在 Redis 客户端命令执行前植入流控判断。

**产出**：
- `SentinelRedisCommandInterceptor`：实现 `RedisConnectionInterceptor` SPI——`afterPropertiesSet` 时索引 `RedisClusterConnection` 全部接口方法，为每个方法生成资源名（如 `RedisConnection.get(String)`）。拦截时将 RedisMethodContext 存入 SentinelContext attributes，保证异步安全。

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：与所有插件一致，需依赖正确传播 BlockException

**配置规格**：
```properties
microsphere.sentinel.redis.enabled = true
```

---

## 四、Spring Web 适配器

### REQ-004：Spring MVC/WebFlux 的 Sentinel 拦截

**问题**：Sentinel 官方已有 `sentinel-spring-webmvc-adapter` 和 `sentinel-spring-webflux-adapter`，但它们只提供基础的 URL 级别流控。microsphere 需要与自己的 `HandlerMethodInterceptor` 体系（来自 03-microsphere-spring）集成，获取更丰富的请求上下文。

**产出**：
- `SentinelHandlerMethodInterceptor`：实现 `HandlerMethodInterceptor`（03 的扩展点）——监听 `WebEndpointMappingsReadyEvent`（03 的端点就绪事件），为每个 `HandlerMethod` 建资源名缓存。资源名格式：`GET /api/users/{id}#com.foo.UserController.getUser`。上下文存 `NativeWebRequest` 属性。

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **P2：`methodResourceNamesCache` 在 `WebEndpointMappingsReadyEvent` 触发前为 null**——事件未触发时 `getResourceName()` 的 `computeIfAbsent` NPE
- 官方 adapter 已经覆盖了基础 Web 流控——这个插件与官方有部分重叠（约 1/6）

**配置规格**：
```properties
microsphere.sentinel.web.enabled = true
```

---

## 五、AutoConfiguration

### REQ-005：Spring Boot 零配置自动装配

**问题**：6 个插件需要分别启用——在 Spring Boot 项目中，每个插件都应该"加依赖即生效"，不需要手动写 Configuration 类。

**产出**（`spring-boot`，6 文件）：
- `SentinelSpringWebAutoConfiguration`：`@ConditionalOnWebApplication(ANY)` + `@ConditionalOnSentinelAvailable` + `@ConditionalOnMissingBean`
- `SentinelAlibabaDruidAutoConfiguration` / `SentinelMyBatisAutoConfiguration`（内含 `@EnableMyBatisExtension`）/ `SentinelRedisAutoConfiguration`（内含 `@ConditionalOnRedisInterceptorEnabled`）—条件装配模式一致
- `@ConditionalOnSentinelAvailable` = `@ConditionalOnSentinelEnabled(matchIfMissing=true)` + `@ConditionalOnClass(SphU.class, SentinelPlugin.class)`
- `SentinelCloudAutoConfiguration`（spring-cloud）：把已安装插件输出为 Spring Cloud Actuator `HasFeatures`

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **P2：per-plugin enabled 属性只读 `System.getProperty`——application.yml 配置无效**（`@ConfigurationProperty` 声明的 APPLICATION_SOURCE 与运行时行为不符）
- enabled 值在构造时快照，运行期改 `-D` 不生效

**配置规格**：`microsphere.sentinel.enabled=true`（全局），`microsphere.sentinel.<plugin>.enabled=true`（各插件）。

---

## 六、SentinelContext 请求上下文

### REQ-006：跨框架统一的 Sentinel 请求上下文

**问题**：Sentinel 官方的 `ContextUtil.enter()` 只提供了 context name 和 origin——没有携带"这次请求的结果是什么"、"有没有失败"、"有没有业务属性"。6 个框架适配器各自需要存储这些信息，没有一个统一的上下文对象。

**产出**：
- `SentinelContext`：ThreadLocal 请求上下文——`resourceName/contextName/origin/Entry/result/failure/attributes`
- `withinContext(contextName, origin, runnable)`：函数式包裹——自动管理 ThreadLocal 的创建和清理
- `doInContext(runnable)`：在已有 Context 中执行——不创建新 Context
- Redis/Web 插件将框架特定对象存入 `attributes`，保证异步安全

**状态**：[已验证实现] 📝 **待编写原理文档**

**配置规格**：跟随插件自动启用。

---

## 七、待实现需求（bug 修复）

### REQ-D01：BlockException 透传修复（Druid/P6Spy/Hibernate）

**方案**：Druid 插件不再 `wrap(e, SQLException.class)`——检测 `BlockException` 时直接抛出；P6Spy/Hibernate 改为直接在回调中 catch `BlockException` 后 re-throw，不使用 `ThrowableAction.execute` 包装。

**状态**：[待修复] — 当前只有 MyBatis/Redis/Web 正确传播 BlockException，其余三个丢失阻断语义。

---

### REQ-D02：SentinelTemplate context 生命周期修复

**方案**：`begin()` = `ContextUtil.enter()` + `SphU.entry()`——将 `ContextUtil.enter()` 移到 `SphU.entry()` **之后**（entry 抛异常时 Context 未创建，无泄漏）。`SentinelOperations.call()` 将 `begin()` 移到 try 块内。

**状态**：[待修复] — 当前 context 泄漏 + finally 永不执行。

---

### REQ-D03：Web 插件 resourceName 缓存 NPE 修复

**方案**：`methodResourceNamesCache` 初始化为空 `ConcurrentHashMap` 而非 `null`——`getResourceName()` 的 `computeIfAbsent` 在事件未触发时降级为 `handlerMethod.toString()`。

**状态**：[待修复] — `WebEndpointMappingsReadyEvent` 未触发时 NPE。

---

### REQ-D04：per-plugin enabled 配置源修正

**方案**：将 `System.getProperty()` → 从 `Environment` 读取（或从 `@ConfigurationProperty` 前缀读取），使 `application.yml` 的配置生效。

**状态**：[待修复] — 当前只能用 `-Dmicrosphere.sentinel.druid.enabled=false` 禁用，yml 无效。

---

### REQ-D05：SentinelUtils 内部 API 依赖替代

**方案**：`ContextUtil.resetContextMap()` 的 MethodHandle 调用改为公开 API 替代方案；`FlowRuleManager.SCHEDULER` 的反射读取改为 Sentinel 公开 API。

**状态**：[待修复] — 依赖 Sentinel 内部实现，跨版本脆弱。

---

### REQ-D06：sentinel-transport 心跳依赖移除

**方案**：评估 `sentinel-transport-simple-http` 依赖是否需要——如果不需要 Sentinel Dashboard 连接，可以降依赖为仅 `sentinel-core`。

**状态**：[待评估] — 当前 pom 中声明 transport 依赖但插件代码无直接引用。

---

### REQ-D07：Hibernate/P6Spy 缺少 Spring Boot AutoConfiguration

**方案**：Hibernate 和 P6Spy 插件已通过 META-INF/services 注册了框架 SPI（`EntityCallback` / `JdbcEventListener`），但在 Spring Boot 中缺少 AutoConfiguration 保证 SentinelPlugin 的 `auto-install` 正确触发——需要新增 `SentinelHibernateAutoConfiguration` 和 `SentinelP6SpyAutoConfiguration`，并在 `AutoConfiguration.imports` 中注册（当前 4 项中不包含这 2 个）。

**状态**：[待补全] — 当前 6 个插件中只有 4 个有 AutoConfiguration。

---

### REQ-D08：SpringWeb 需 @EnableWebExtension 手动启用

`SentinelSpringWebAutoConfiguration` 只注册 bean，**不导入 @EnableWebExtension**。用户不加此注解 → `WebEndpointMappingRegistry` 缺失 → `WebEndpointMappingsReadyEvent` 永不发布 → `methodResourceNamesCache` 永久为 null → 首个请求 NPE。Hibernate 同因——EntityCallback SPI 自发现依赖 Hibernate 框架启动，非 Spring Boot 标准路径。

### REQ-D09：Redis method cache 静默跳过

`beforeExecute` 中 `SentinelRedisCommandInterceptor` 的 `getResourceName` 对缓存未命中返回 null → 整个 Sentinel 拦截静默跳过（无流控、无指标）。Druid/MyBatis 的 resourceName 由 SQL/MappedStatement 保证非 null，Redis 的 cache 可能因 RedisClusterConnection 接口方法变更 miss。

### REQ-D10：end() 清理语义正确但 begin() 泄漏未修

`entry.exit()` + `ContextUtil.exit()` 序列正确（自定义 context 下 CtEntry 不会自动 exit，需显式）。问题只在 `begin()` 的 `enter()` 先于 `entry()`——此 bug 修复后 end() 无需改动。

---

## 八、发散需求（生产环境需要的全新能力）

### REQ-N01：替换死代码 getFlowDataId 为官方 datasource

**生产痛点**：`SentinelUtils.getFlowDataId()` 是死代码——预留了 `{}-flow-rules` dataId 模式但从未实现。官方 `sentinel-datasource-nacos` / `sentinel-datasource-apollo` 已经提供了完整的 `ReadableDataSource<String, List<FlowRule>>` 实现——microsphere 不应该再造轮子，应该集成官方 datasource 并在 Spring Boot 中自动装配。

**产出**：`SentinelDatasourceAutoConfiguration` —— 读取 `microsphere.sentinel.datasource.nacos.server-addr/dataId/group` 配置 → 构造官方 `NacosDataSource` → 自动 `FlowRuleManager.register2Property()`，规则变更无需重启。

**状态**：[待实现]（不造轮子，直接集成官方 `sentinel-datasource-nacos` + `sentinel-datasource-apollo`）

---

### REQ-N02：JMX 动态插件启停（写操作）

**生产痛点**：当前 `JMXSentinelPluginRepository` 已注册 JMX MBean，但只有只读属性——无法运行时禁用/启用某个插件。这仍是真正独特的能力（官方 Sentinel 没有 JMX MBean 管理概念，更无插件启停操作）。

**产出**：`SentinelPluginMBean` 接口暴露 `uninstall(pluginName)` / `install(pluginName)` 写操作——运行时禁用 Druid 流控（发现某个 SQL 被误杀），或启用已关闭的插件。

**状态**：[待实现]（JMX MBean 已经注册，只缺写操作接口。microsphere 的 `SentinelPlugin` SPI 框架使这项能力成为可能——官方没有插件概念）

---

> **已删除的初版发散（经源码验证官方已有）**：
> - ~~N03 多维度流控~~ → 官方 `sentinel-parameter-flow-control` 已有 `ParamFlowRule` 热点参数限流
> - ~~N04 Prometheus 指标~~ → 官方 `sentinel-prometheus-metric-exporter` 已有 pass/block 计数导出
> - **教训**：发散前必须先 grep 官方 `/data/workspace/source-code/code/spring/sentinel/`

---

## 九、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| — | 2026-08-02 | REQ 文档编写（第一版） |
