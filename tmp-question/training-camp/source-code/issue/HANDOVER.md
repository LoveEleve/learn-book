# Spring 源码学习项目 — 交接文档

> **时间**: 2026-08-02
> **当前 AI**: 完成工作交接，下一个 AI 继续

---

## 一、项目背景

目标：系统学习 Spring 生态源码，最终支撑 microsphere 项目（34 个仓库）的深度分析。

---

## 二、源码仓库（全部在本地）

### 路径

```
/data/workspace/source-code/code/spring/    ← Spring 生态 32 个仓库（核心）
/data/workspace/source-code/code/microsphere/  ← microsphere 项目源码（34 个仓库，另一个项目）
/data/workspace/source-code/code/source-md/    ← 源码分析笔记积累（另一个项目）
```

### Spring 生态 32 个仓库清单

| 仓库 | 版本 | 路径 |
|---|---|---|
| spring-framework | v6.2.17 | `/data/workspace/source-code/code/spring/spring-framework/` |
| spring-boot | 3.5.16 | `/data/workspace/source-code/code/spring/spring-boot/` |
| tomcat | 10.1.34 | `/data/workspace/source-code/code/spring/tomcat/` |
| netty | 4.2.15.Final | `/data/workspace/source-code/code/spring/netty/` |
| spring-cloud-commons | v4.3.2 | `/data/workspace/source-code/code/spring/spring-cloud-commons/` |
| spring-cloud-gateway | v4.3.2 | `/data/workspace/source-code/code/spring/spring-cloud-gateway/` |
| spring-cloud-openfeign | v4.3.2 | `/data/workspace/source-code/code/spring/spring-cloud-openfeign/` |
| spring-cloud-alibaba | 2025.0.0.0 | `/data/workspace/source-code/code/spring/spring-cloud-alibaba/` |
| nacos | 3.0.3 | `/data/workspace/source-code/code/spring/nacos/` |
| sentinel | 1.8.9 | `/data/workspace/source-code/code/spring/sentinel/` |
| seata | v2.5.0 | `/data/workspace/source-code/code/spring/seata/` |
| rocketmq | 5.3.1 | `/data/workspace/source-code/code/spring/rocketmq/` |
| kafka | 4.1.2 | `/data/workspace/source-code/code/spring/kafka/` |
| skywalking | v10.4.0 | `/data/workspace/source-code/code/spring/skywalking/` |
| sofa-jraft | v1.4.1 | `/data/workspace/source-code/code/spring/sofa-jraft/` |
| xxl-job | 2.4.2 | `/data/workspace/source-code/code/spring/xxl-job/` |
| hikaricp | 7.0.2 | `/data/workspace/source-code/code/spring/hikaricp/` |
| druid | 1.2.27 | `/data/workspace/source-code/code/spring/druid/` |
| mybatis | 3.5.16 | `/data/workspace/source-code/code/spring/mybatis/` |
| mybatis-plus | 3.5.7 | `/data/workspace/source-code/code/spring/mybatis-plus/` |
| shardingsphere | 5.5.1 | `/data/workspace/source-code/code/spring/shardingsphere/` |
| redis | 7.4.2 | `/data/workspace/source-code/code/spring/redis/` |
| redisson | main | `/data/workspace/source-code/code/spring/redisson/` |
| elasticsearch | v8.12.2 | `/data/workspace/source-code/code/spring/elasticsearch/` |
| zookeeper | release-3.9.5 | `/data/workspace/source-code/code/spring/zookeeper/` |
| curator | 5.8.0 | `/data/workspace/source-code/code/spring/curator/` |
| dubbo | main | `/data/workspace/source-code/code/spring/dubbo/` |
| feign | main | `/data/workspace/source-code/code/spring/feign/` |
| micrometer | main | `/data/workspace/source-code/code/spring/micrometer/` |
| micrometer-tracing | main | `/data/workspace/source-code/code/spring/micrometer-tracing/` |
| grpc-java | v1.83.1 | `/data/workspace/source-code/code/spring/grpc-java/` |
| arthas | 4.3.2 | `/data/workspace/source-code/code/spring/arthas/` |

完整清单文件（含拉取命令）：`/data/workspace/source-code/book/MinerU/spring-repos-list.md`

### MCP 索引

所有 32 个仓库已用 `mcp__codebase-memory-mcp__index_repository` 建立全量索引（mode=full）。

**项目名格式**：`data-workspace-source-code-code-spring-{仓库名}`（如 `data-workspace-source-code-code-spring-spring-framework`）

**使用方式**：
- `mcp__codebase-memory-mcp__search_graph({project, query})` — 查调用关系
- `mcp__codebase-memory-mcp__get_code_snippet({project, file_path, line})` — 取代码片段
- `mcp__codebase-memory-mcp__trace_path({project, source, target})` — 追踪数据流

**注意**：旧路径下的索引（如 `data-workspace-source-code-code-nacos`）已过时——仓库已移到 `spring/` 子目录，应使用新路径的索引。

---

## 三、已完成工作

### 3.1 方法论探索

文件：`/data/workspace/source-code/book/MinerU/tmp-question/training-camp/source-code/issue/01-源码学习方法论探索.md`

内容：第一性原理推导（源码 = 设计决策 × 实现细节）、三层循环（扫轮廓 → 盯关键点 → 收尾）、与四种方案（A/B/C/D）的融合。这个文件是**独立的方法论文档**，不是规划的一部分，下一个 AI **不需要**参考它写规划。

### 3.2 Spring Framework 学习范围规划（完成 ✅）

文件：`/data/workspace/source-code/book/MinerU/tmp-question/training-camp/source-code/issue/Spring源码学习范围规划.md`

**45 🔴 + 15 🟡**，覆盖 9 层：

| 层 | 模块 | 🔴 域 | 🟡 域 | 说明 |
|---|---|---|---|---|
| 0 | spring-core | 7 | — | Resource/Type/Env/Ordered/Annotation/Profile/**TaskExecutor** |
| 1 | spring-beans | 9 | — | BeanDefinition/Factory/生命周期/三级缓存/DI/BPP/BFPP/FactoryBean/作用域 |
| 2 | spring-context | 11 | 5 | refresh/@Configuration/事件/父子容器/@Import/@Conditional/@Async/@Scheduled/@Cacheable/@Lazy/**AOT/Native Image** |
| 3 | expression | — | 1 | SpEL（附在 @Value 处理中） |
| 4 | spring-aop | 4 | 1 | 代理/Advice链/@AspectJ解析/自动代理（+Pointcut🟡） |
| 5 | spring-tx | 4 | 1 | @Transactional链路/失效场景/传播行为/**DataAccessException**（+TxSync🟡） |
| 6 | spring-jdbc | 2 | 1 | JdbcTemplate/DataSource（+RowMapper🟡） |
| 7 | spring-web/webmvc | 6 | 2 | DispatcherServlet/RequestMapping/RequestBody/ResponseBody/拦截器/异常处理（+@InitBinder🟡/+WebFlux&codec🟡） |
| 8 | spring-test | 2 | 2 | MockMvc/TestContext（+@MockBean🟡/+@Sql🟡） |
| 周边 | websocket/messaging | — | 2 | 🟡 涉猎不深入；i18n 淘汰 |

这个规划经过多轮 review（包扫描 → 兄弟包发现 → 面试题对照 → 注解覆盖率 → 技术栈对齐 → **兄弟包二次核对遗漏补全**），**100% 包覆盖 + 全部生产常用注解覆盖**。

> **统计修正说明（2026-08-02）**：原声明 49🔴+24🟡 一直数错（git 考证：b5db82a 时实际就只有 42🔴+13🟡）。本次兄弟包二次核对发现 3 个 🔴 遗漏域（TaskExecutor/AOT/DataAccessException）+ 3 个 🟡 遗漏域（ClassPathIndex/@Sql/codec），补全后实际 **45🔴+15🟡**。详见规划文档"V7 演进记录"表。

### 3.3 Spring Boot 学习范围规划（已完成源码扫描 ✅）

文件：`/data/workspace/source-code/book/MinerU/tmp-question/training-camp/source-code/issue/SpringBoot源码学习范围规划.md`

**17 🔴 + 7 🟡 = 24 域**（基于 spring-boot-project 全部 13 个子模块 + autoconfigure 60+ 子包逐包扫描，837+376+448+101+173 Java 文件；每个淘汰包均经源码确认 + 用户技术栈对照）。不重复 Framework 的 45 🔴 域，聚焦 Boot 特有机制：

| 层 | 🔴 | 🟡 | 域 |
|---|---|---|---|
| 1 自动装配核心 | 5 | — | @SpringBootApplication/条件注解/SpringApplication.run+BootstrapContext+ApplicationContextInitializer/@ConfigurationProperties/Starter |
| 2 Web 自动装配 | 3 | — | DispatcherServlet+MVC/嵌入式容器/HTTP 客户端+消息转换器+Jackson |
| 3 数据访问自动装配 | 4 | — | **DataSource/HikariCP**/Redis/事务+异常翻译/缓存 |
| 4 异步与 AOT | 2 | — | TaskExecutor/AOT |
| 5 启动与运行时 | 1 | 5 | 诊断🔴 + EnvPostProcessor/日志/可用性/虚拟线程/WebFlux+Netty🟡 |
| 6 Actuator 与测试 | 2 | 2 | **Actuator 端点体系🔴**/**测试自动配置🔴** + Validation/Elasticsearch🟡 |

**本次源码扫描新发现的 9 个 🔴 遗漏域**（原 8🔴 规划完全没提）：
- B-9 DataSource/JDBC（HikariCP 自动装配，面试必问）
- B-10 Redis（Lettuce/Jedis + RedisTemplate，面试必问）
- B-11 事务自动配置（PlatformTransactionManager + PersistenceExceptionTranslation）
- B-12 缓存自动配置（CacheType/Caffeine/Redis）
- B-13 TaskExecutor/异步（ThreadPoolTaskExecutor 默认配置）
- B-14 AOT/Native Image 启动处理（SpringApplicationAotProcessor）
- B-15 启动失败诊断 FailureAnalyzers（生产常用）
- B-21 Actuator 端点体系（824 文件：端点机制/核心 endpoint/扩展点，原 🟡 升级 🔴）
- B-22 测试自动配置（274 文件：@SpringBootTest/@MockBean/@WebMvcTest 切片测试）

**深度探索补回的 1 个 🟡**（用户质疑"自动配置全部都不在规划内"后，逐包读源码发现误判）：
- B-24 Elasticsearch 自动配置（my-xhs 用 ES 8.19.19，原误判为"中间件"淘汰）

**合并到已有域的 5 个包**：http/client→B-8（RestClient）、dao→B-11（异常翻译）、netty→B-20（WebFlux 服务器）、jackson→B-8（JsonComponent）、context→B-3（ApplicationContextInitializer）、info→B-21（BuildProperties）

**用户明确淘汰**：Security（项目用自实现 HMAC 过滤器）、r2dbc（响应式 DB 不用）、SSL Bundle（my-xhs 不用）、DB 初始化（schema.sql 不用，面试低频）

**32 仓库与 SpringBoot 关系**：第 1 类（Boot 自动配置覆盖：tomcat/netty/hikaricp/druid/redis/redisson/elasticsearch/mybatis/micrometer 等）在对应 Boot 域读到源码；第 2 类（Spring Cloud 生态：cloud-commons/gateway/openfeign/alibaba）等 Spring Cloud 规划；第 3 类（独立中间件：nacos/sentinel/rocketmq 等）独立学习。

**已与 Framework 规划交叉覆盖确认**（12 个交叉点，见规划文档"与 Framework 规划的交叉覆盖"表），Framework 讲原理，Boot 讲应用/自动装配，不重复。

**✅ 已完成源码包扫描，下一步可直接开始逐篇写文章。**

### 3.4 HikariCP 学习范围规划（完成 ✅）

文件：`/data/workspace/source-code/book/MinerU/tmp-question/training-camp/source-code/issue/HikariCP源码学习范围规划.md`

**6 🔴 + 7 🟡 = 13 域**，单模块 48 个源文件 5 个包全部审计：

| 层 | 🔴 | 🟡 | 域 |
|---|---|---|---|
| 核心 | 6 | — | 核心架构/HikariPool(ConcurrentBag/获取流程/归还流程/生命周期/HouseKeeper) |
| 扩展 | — | 7 | 连接验证/泄漏检测/指标监控/JMX/SuspendResumeLock/代理生成/DriverDataSource |

**淘汰**：Hibernate 集成、JNDI、Dropwizard 3.x、Dropwizard HealthCheck、Prometheus 原生（Micrometer 桥接即可）、OSGi、SQLExceptionOverride、HikariCredentialsProvider

### 3.5 Druid 学习范围规划（完成 ✅）

文件：`/data/workspace/source-code/book/MinerU/tmp-question/training-camp/source-code/issue/Druid源码学习范围规划.md`

**5 🔴 + 4 🟡 = 9 域**，7 个子模块全部审计，core 1614 个源文件（SQL 解析器占 1238 文件/76%）：

| 层 | 域 |
|---|---|
| 🔴 核心 | 连接池核心(ReentrantLock+Condition)/Filter拦截链/StatFilter监控/WallFilter防火墙/后台维护线程 |
| 🟡 扩展 | SQL Parser体系架构概览/连接验证/PreparedStatementPool/Spring Boot3 Starter |

**Druid vs HikariCP 架构差异**：Druid 用 ReentrantLock+Condition 阻塞模型（vs ConcurrentBag 无锁），Filter 链拦截所有 JDBC 操作（vs 无拦截器的直接代理），内置 SQL 解析+监控+防火墙（vs 纯池无监控）。

**淘汰**：druid-admin(13)、wrapper(13)、demo、SpringBoot2 starter(8)、pool/ha/XA、C3P0 Adapter、ConfigFilter、EncodingFilter、LoggingFilter、SQL parser 1200+ 文件内部实现、support/~100文件

### 3.6 MyBatis-Plus 学习范围规划（完成 ✅）

文件：`/data/workspace/source-code/book/MinerU/tmp-question/training-camp/source-code/issue/MyBatis-Plus源码学习范围规划.md`

**5 🔴 + 4 🟡 = 9 域**，8 个子模块全部审计，385 个源文件（core 158 + annotation 16 + extension 111 + generator 100）：

| 层 | 域 |
|---|---|
| 🔴 核心 | SQL 自动注入(DefaultSqlInjector+17种内置方法+AbstractMethod模板)/表元数据解析(@TableName/@TableId/@TableField)/Lambda条件构造器(SerializedLambda反序列化)/MybatisPlusInterceptor插件体系(InnerInterceptor链5回调)/分页插件(物理分页SQL改寫12+方言) |
| 🟡 扩展 | 乐观锁(@Version+版本号CAS)/自动填充(MetaObjectHandler审计字段)/逻辑删除(@TableLogic DELETE转UPDATE)/BaseMapper+IService |

**关键设计模式**：通过替换 MyBatis Configuration 来"注入"能力——不是 AOP 代理，而是直接替换 MyBatis 核心组件（Configuration/MapperRegistry/MapperAnnotationBuilder）。

**淘汰**：generator(100)、starter(0 resources)、activerecord、DDL、p6spy、scripting、aggregator、BOM、gradle/、libs/ 等 11 个子模块/功能

### 3.7 Redisson 学习范围规划（完成 ✅）

文件：`/data/workspace/source-code/book/MinerU/tmp-question/training-camp/source-code/issue/Redisson源码学习范围规划.md`

**4 🔴 + 3 🟡 = 7 域**，12 个子模块全部审计，core 1570 个源文件（API 接口 801 + Client 协议 217 + 实现 127）：

| 层 | 域 |
|---|---|
| 🔴 核心 | Redisson主类(1532行)+ConnectionManager连接管理/RLock(600行)+Watchdog自动续期/Codec序列化体系(7种实现)/CommandAsyncExecutor命令执行流水线 |
| 🟡 扩展 | RMap分布式映射(1967行)+写后读通/Spring Cache集成(@Cacheable)/RBucket+RAtomicLong基础数据结构 |

**Redis C源码确认**：`/data/workspace/source-code/code/spring/redis/` 是 Redis 7.4.2 C 语言服务器源码，非 Java Spring 库——不纳入学习规划。

**淘汰**：801 API接口（遇到查API）、217 RESP协议文件、mapreduce、liveobject、executor、remote、reactive/rx、transaction、jcache、hibernate/mybatis/tomcat集成等 12 个子模块/功能

---

## 四、下一个 AI 需要做的事

### 当前进度（28/32 仓库，174🔴+105🟡=279域）

剩余仓库：**Elasticsearch(11810文件—Java)、Kafka(3484+108文件—Java/Scala)、ZooKeeper(524文件—Java)**

⚠️ 之前误判 ES/Kafka 为"Server C源码"——实际都是 Java 项目，应纳入学习规划。

### 🚀 立即开始（无歧义下一步）

**第一步：Seata 深度 review**
→ 读 `Seata源码学习范围规划.md` → MCP search_graph 验证核心类 → 修正初版规划

**第二步：逐一规划剩余 3 仓库**
→ Elasticsearch → Kafka → ZooKeeper（按文件数从大到小，one-at-a-time）

**第三步：规划收尾后从 Spring Boot 开始逐篇写作**

### 必须做的：

1. **Seata 规划需要深度 review**
   - Seata v2.5.0 已写入初版规划（4🔴=4域），但未经过深度源码审查
   - 需要：读 DefaultCoordinator/DataSourceProxy/UndoLogManager/GlobalTransaction 等核心类方法体
   - 验证 AT 模式 undo_log 两阶段提交描述准确性

2. **ES、Kafka、ZK 需要独立规划**
   - ES 11810 文件：聚焦 search engine 核心（索引引擎/查询DSL/分片/集群协调）
   - Kafka 3484 文件：聚焦 broker 核心（日志存储/分区/消费者组/控制器选举）
   - ZK 524 文件：聚焦 ZAB 协议/Leader选举/会话管理/Watcher机制
   - 遵循 one-at-a-time 规则，每个仓库：探索源码 → 写规划文档 → 查漏补缺 → 确认 → 下一个

### 注意事项：

1. **用户的学习方法论**：用户是资深工程师，要求深度源码分析。每篇文章标准：源码走读 + 面试点 + 生产陷阱。不限制行数/字数。每篇写完等用户确认后再写下一篇（不要批量）。

2. **淘汰标准**：
   - 基于技术过时（如 JPA/Hibernate、JSP/ViewResolver、XML 编组、JMS）
   - 基于"是否用过"**不是淘汰标准**——"没接触过"恰恰是学习的理由
   - 各种中间件自动配置——面试不问、不用不学，确认淘汰

3. **重要反馈规则**（已写入记忆系统，新 AI 应该能看到）：
   - `不要给自己设置限制` —— 学习范围不能自限
   - `逐个模块都详细扫描后再系统性的规划` —— 规划前必须包扫描
   - `不熟悉≠淘汰` —— 用户不熟悉的应该学，不应该跳过
   - `Spring 学习范围 = 技术专家标准 + 用户实际技术栈裁剪`
   - `规划文档留一个权威版即可` —— 不要搞 V1/V2/V3 版本堆积
   - `ls≠源码阅读` —— 必须读方法体才算探索
   - `子模块全覆盖审计` —— 每个仓库所有子模块都要标注状态
   - `MCP索引可用` —— 32仓库已建立索引，可search_graph/trace_path验证

4. **规划文档使用方式**：
   - 每个仓库有独立的 `{RepoName}源码学习范围规划.md`
   - 文件内包含：知识域表格 + 淘汰清单（每个子模块标注状态和理由）+ 统计 + 交叉引用
   - 13 个规划文档，182 个子模块全覆盖审计完成

---

## 五、关键路径

```
Framework 规划 (45🔴+15🟡=60域, 已修正+审计) 
→ Boot 规划 (17🔴+7🟡=24域, 四轮深度探索+审计)
→ Spring Cloud 规划 (24🔴+17🟡=41域, 四仓库全部完成+审计)
→ 独立中间件规划 ~~(18🔴+26🟡=44域)~~ → NET: 5🔴+7🟡=12域
→ HikariCP 规划 (6🔴+7🟡=13域)
→ Druid 规划 (5🔴+4🟡=9域)
→ MyBatis-Plus 规划 (5🔴+4🟡=9域)
→ Redisson 规划 (4🔴+3🟡=7域)
→ ShardingSphere 规划 (4🔴+2🟡=6域)
→ XXL-Job 规划 (5🔴+2🟡=7域)
→ SkyWalking 规划 (3🔴+3🟡=6域)
→ Dubbo 规划 (4🔴+3🟡=7域)
→ Arthas 规划 (4🔴+2🟡=6域)
→ Micrometer 规划 (4🔴+3🟡=7域)
→ gRPC-Java 规划 (4🔴+2🟡=6域)
→ Feign 规划 (3🔴+2🟡=5域)
→ SofaJRaft 规划 (4🔴+1🟡=5域)
→ Curator 规划 (3🔴+2🟡=5域)
→ MyBatis 规划 (4🔴+1🟡=5域)
→ MicrometerTracing 规划 (2🔴+1🟡=3域)
→ Tomcat 规划 (3🔴+1🟡=4域)
→ Seata 规划 (4🔴=4域) ← 需深度 review
→ 全局总计: **28 个规划文档完成** (174🔴+105🟡=279域)
→ 剩余待规划: Elasticsearch / Kafka / ZooKeeper (3个)
```

### 全局规划状态（进行中，18 仓库全覆盖审计）

| 仓库 | 子模块数 | 🔴 | 🟡 | 总域 | 方法体验证 |
|---|---|---|---|---|---|
| Spring Framework | 25 | 45 | 15 | 60 | ✅ |
| Spring Boot | 10 | 17 | 7 | 24 | ✅ |
| Cloud Commons | 5 | 8 | 5 | 13 | ✅ |
| Cloud Gateway | 11 | 5 | 4 | 9 | ✅ |
| Cloud OpenFeign | 1 | 5 | 4 | 9 | ✅ |
| Cloud Alibaba | 13 | 6 | 4 | 10 | ✅ |
| Netty | 45 | 5 | 7 | 12 | ✅ |
| Nacos | 24 | 3 | 4 | 7 | ✅ |
| Sentinel | 10 | 4 | 6 | 10 | ✅ |
| RocketMQ | 18 | 5 | 5 | 10 | ✅ |
| **HikariCP** | 5 | 6 | 7 | 13 | ✅ |
| **Druid** | **7** | **5** | **4** | **9** | ✅ |
| **MyBatis-Plus** | 8 | 5 | 4 | 9 | ✅ |
| **Redisson** | **12** | **4** | **3** | **7** | ✅ |
| **ShardingSphere** | **15** | **4** | **2** | **6** | ✅ |
| **合计** | **15** | **127** | **82** | **209** | |

### 32 仓库分类

| 类别 | 仓库 | 状态 |
|---|---|---|
| **已规划（28 仓库）** | Framework/Boot/Cloud×4/Netty/Nacos/Sentinel/RocketMQ/**HikariCP**/**Druid**/**MyBatis-Plus**/**Redisson**/**ShardingSphere**/**XXL-Job**/**SkyWalking**/**Dubbo**/**Arthas**/**Micrometer**/**gRPC-Java**/**Feign**/**SofaJRaft**/**Curator**/**MyBatis** | ✅ **174🔴+105🟡=279域** |
| **待规划（3 仓库）** | Elasticsearch / Kafka / ZooKeeper | 独立学习（均为 Java 项目） |
| **排除** | Redis | C 语言服务器，非 Java 学习目标 |

---

## 六、文档路径

### Git 仓库

所有文档通过 MinerU 的 git 仓库管理：
- **Remote**: `github.com:LoveEleve/learn-book.git`（main 分支）
- **本地根路径**: `/data/workspace/source-code/book/MinerU/`

### issue 目录（28 个规划文档）

```
/data/workspace/source-code/book/MinerU/tmp-question/training-camp/source-code/issue/
├── HANDOVER.md                          ← ★ 本文件（唯一入口）
├── Spring源码学习范围规划.md              ← Framework (45🔴+15🟡=60域 ✅)
├── SpringBoot源码学习范围规划.md           ← Boot (17🔴+7🟡=24域 ✅)
├── SpringCloudCommons源码学习范围规划.md   ← Cloud Commons (8🔴+5🟡=13域 ✅)
├── SpringCloudGateway源码学习范围规划.md   ← Cloud Gateway (5🔴+4🟡=9域 ✅)
├── SpringCloudOpenfeign源码学习范围规划.md ← Cloud OpenFeign (5🔴+4🟡=9域 ✅)
├── SpringCloudAlibaba源码学习范围规划.md   ← Cloud Alibaba (6🔴+4🟡=10域 ✅)
├── Netty源码学习范围规划.md               ← Netty (5🔴+7🟡=12域 ✅)
├── Nacos源码学习范围规划.md               ← Nacos (3🔴+4🟡=7域 ✅)
├── Sentinel源码学习范围规划.md            ← Sentinel (4🔴+6🟡=10域 ✅)
├── RocketMQ源码学习范围规划.md            ← RocketMQ (5🔴+5🟡=10域 ✅)
├── HikariCP源码学习范围规划.md            ← HikariCP (6🔴+7🟡=13域 ✅)
├── Druid源码学习范围规划.md                ← Druid (5🔴+4🟡=9域 ✅)
├── MyBatis-Plus源码学习范围规划.md          ← MyBatis-Plus (5🔴+4🟡=9域 ✅)
├── MyBatis源码学习范围规划.md              ← MyBatis (4🔴+1🟡=5域 ✅)
├── Redisson源码学习范围规划.md              ← Redisson (4🔴+3🟡=7域 ✅)
├── ShardingSphere源码学习范围规划.md         ← ShardingSphere (4🔴+2🟡=6域 ✅)
├── XXL-Job源码学习范围规划.md               ← XXL-Job (5🔴+2🟡=7域 ✅)
├── SkyWalking源码学习范围规划.md            ← SkyWalking (3🔴+3🟡=6域 ✅)
├── Dubbo源码学习范围规划.md                 ← Dubbo (4🔴+3🟡=7域 ✅)
├── Arthas源码学习范围规划.md                ← Arthas (4🔴+2🟡=6域 ✅)
├── Micrometer源码学习范围规划.md            ← Micrometer (4🔴+3🟡=7域 ✅)
├── MicrometerTracing源码学习范围规划.md      ← MicrometerTracing (2🔴+1🟡=3域 ✅)
├── gRPC-Java源码学习范围规划.md             ← gRPC-Java (4🔴+2🟡=6域 ✅)
├── Feign源码学习范围规划.md                 ← Feign (3🔴+2🟡=5域 ✅)
├── SofaJRaft源码学习范围规划.md             ← SofaJRaft (4🔴+1🟡=5域 ✅)
├── Curator源码学习范围规划.md               ← Curator (3🔴+2🟡=5域 ✅)
├── Tomcat源码学习范围规划.md                ← Tomcat (3🔴+1🟡=4域 ✅)
├── Seata源码学习范围规划.md                 ← Seata (4🔴=4域) ⚠️ 需深度 review
└── 01-源码学习方法论探索.md               ← 方法论（独立，不需要参考）
```

### 源码仓库路径

```
/data/workspace/source-code/code/spring/    ← 32 个 Spring 生态仓库
```

### MCP 索引

所有 32 个仓库已用 `mcp__codebase-memory-mcp__index_repository` 建立全量索引（mode=full）。
- 项目名格式：`data-workspace-source-code-code-spring-{仓库名}`
- 使用方式：`search_graph`/`get_code_snippet`/`trace_path`

### 仓库清单

```
/data/workspace/source-code/book/MinerU/spring-repos-list.md  ← 32 个仓库版本+拉取命令
```
