# Spring Boot 自动装配 — 知识网络化规划 (v2 重构)

> **日期**: 2026-08-11 | **依据**: 原始执行计划 B-1~B-24 (24域) 重构
> **重构动因**: ①Boot 是"整合层" — 每个域都挂在下层大纲节点上, 必须知识网络化 ②漏了最核心的"自动装配加载机制"域 ③Redis/ES 与阶段3重叠 ④薄域占名额 ⑤外部化配置被低估
> **方法论**: 06 跨域交叉引用策略 + 06 §2.5 复用≠省略五件事检查

---

## 重构原则

1. **知识网络化**: 每域标注前置依赖 (挂到已有大纲), 分析时交叉引用, 不孤立
2. **复用≠省略**: 机制内核一行引用; 本层使用/组装/配置/生命周期/差异五件事必须展开
3. **核心机制优先**: 自动装配加载机制 > 属性绑定 > 条件评估 (Boot 灵魂)
4. **与阶段3分工**: Redis/ES 只讲"自动装配怎么接线", 深入在阶段3

---

## 重构后域清单 (S-1~S-24 24域 + 深探新增 S-25~S-29 5域 = 29 域)

### 第 1 层: 自动装配核心 (6 域, 全部 🔴)

| # | 域 | 核心主题 | 前置依赖 (已有大纲) | 复用/展开 |
|:--:|---|---|---|---|
| S-1 | @SpringBootApplication | 组合注解: @SpringBootConfiguration+@EnableAutoConfiguration+@ComponentScan | C-5 注解元数据、s9 @Configuration、s23 @Import、C-6 条件 | 展开: 三层组合的拆解; 复用: 注解元数据机制 |
| **S-2** | **自动装配加载机制** ⭐新增 | AutoConfigurationImportSelector/DeferredImportSelector→AutoConfiguration.imports→ImportFilter/Order→条件评估 | s23 @Import/DeferredImportSelector、S-1 | **全展开 — Boot 灵魂, 无下层可复用** |
| S-3 | 条件注解 | @ConditionalOnClass/@OnBean/@OnProperty/@OnMissingBean | **C-6 ConditionEvaluator** (同一引擎) | 展开: Boot 各 OnXxx 条件的实现; 复用: 评估引擎内核 |
| S-4 | SpringApplication.run | BootstrapContext/ApplicationContextInitializer/EnvironmentPostProcessor/异常报告器 | s8 refresh、C-3 Environment、C-9 ApplicationRunner | 展开: run 全流程与 refresh 衔接; 复用: refresh 12 步 |
| S-5 | @ConfigurationProperties | 属性绑定+**松弛绑定 RelaxedBinding**+ConfigurationPropertiesBindingPostProcessor | C-2 类型转换、C-3 Environment、C-5 注解元数据 | 展开: Binder/松弛绑定 (下层无) |
| S-6 | Starter 机制 | spring-boot-starter-* 依赖传递/自动装配入口 | S-2、s23 | 展开: starter→imports 的映射 |

### 第 2 层: Web 自动装配 (3 域 🔴)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| S-7 | MVC 自动装配 | WebMvcAutoConfiguration/ResourceProperties/HttpEncodingAutoConfiguration | **W-1 DispatcherServlet、W-5 MessageConverter、C-12/C-13** | 展开: 自动装配点(handlerMapping/adapter/converter 如何被装配); 复用: 各机制内核 |
| S-8 | 嵌入式容器 | ServletWebServerFactory 抽象 + **Jetty/Undertow 对照** (Tomcat vs Netty) | **Tomcat t7 (已深度覆盖 Tomcat 组装!)、T-1~T-6、Netty N-1~N-12** | **引用 t7 全部 Tomcat 组装细节** (getWebServer/Starter/配置映射/优雅关闭); **展开增量: 跨容器工厂抽象层次 + Jetty/Undertow 实现对照 + 容器切换机制** |
| S-9 | HTTP客户端+消息转换 | RestClient/HttpMessageConverters/JacksonAutoConfiguration | W-5、C-2 | 展开: Jackson 自动装配/转换器注册顺序 + **RestClient 本体 (spring-web 新类, 未覆盖过)**; 复用: 转换器机制 |

### 第 3 层: 数据访问自动装配 (4 域 🔴)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| S-10 | DataSource/Hikari | DataSourceAutoConfiguration→HikariDataSource→spring.datasource.hikari | **C-11 DataSource、阶段3 HikariCP(H-1~H-13)** | 展开: 自动装配/条件选择(Embedded/池化); 复用: 池化内核在阶段3 |
| S-11 | Redis 自动装配 | RedisAutoConfiguration→RedisTemplate | 阶段3 Redis (18域) | **只讲接线**(条件+模板装配), 连接/协议深入在阶段3 |
| S-12 | 事务自动配置 | DataSourceTransactionManagerAutoConfiguration | **s29-s33 (tx 层)** | 展开: 自动创建事务管理器+@EnableTransactionManagement; 复用: 事务链内核 |
| S-13 | 缓存自动配置 | CacheAutoConfiguration→Caffeine/Redis | **s19 @Cacheable** | 展开: CacheManager 自动装配/多实现选择; 复用: 缓存拦截器 |

### 第 4 层: 异步与 AOT (2 域 🔴)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| S-14 | TaskExecutor 自动配置 | TaskExecutionAutoConfiguration→applicationTaskExecutor | **C-7 TaskExecutor** | 展开: @Async 默认线程池的装配; 复用: 池化机制 |
| S-15 | AOT/Native Image | SpringApplicationAotProcessor/RuntimeHints/ReflectionHints | **s21 AOT** | 展开: Boot 的 AOT 处理管线; 复用: AOT 概念 |

### 第 5 层: 启动与运行时 (4 域 1🔴+3🟡)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| S-16 | 诊断 | FailureAnalyzer (PortInUse/NoSuchBean/BindException) | C-13 异常 | 展开: 启动失败分析机制; 复用: 异常分类概念 |
| S-17 | **外部化配置深化** ⭐ | ConfigDataEnvironmentPostProcessor/17级优先级/application.yml 加载 | **C-3 Environment** | **展开: 配置数据加载与优先级 (下层无)** |
| S-18 | 日志 | LoggingSystem/LogbackLoggingSystem | — (独立) | 展开: 日志系统抽象 |
| S-19 | 启动运行时 (合并) | 可用性(Liveness/Readiness)+虚拟线程+ApplicationReadyEvent — **拆 2 篇**: ①可用性+ReadyEvent ②虚拟线程(开关) | C-9 ApplicationRunner、C-7 | 展开: 状态机/开关/事件时序; 复用: 启动回调 |

### 第 6 层: Actuator 与测试 (2 域 🔴+2🟡)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| S-20 | WebFlux+Netty 自动装配 | ReactiveWebServerFactory/NettyReactiveWebServerFactory | **C-15 WebFlux、Netty N-1~N-12** | 展开: 响应式服务器工厂; 复用: WebFlux 内核 |
| S-21 | Actuator 端点 | Endpoint/WebEndpoint/ControllerEndpoint | C-12/C-13 (Web 机制) | 展开: 端点注册/暴露/健康检查 |
| S-22 | 测试自动配置 | @SpringBootTest/@WebMvcTest/@DataJpaTest 切片 | **C-16~C-19 (test 层)** | 展开: 切片自动配置; 复用: TestContext 机制 |
| S-23 | Validation | ValidationAutoConfiguration | **C-22 Bean Validation** | 展开: 自动注册校验器+MethodValidationPostProcessor; 复用: 校验机制 |

### 降级项 (🟡, 与阶段3重叠 — 只讲接线)

| # | 域 | 核心主题 | 前置依赖 | 说明 |
|:--:|---|---|---|---|
| S-24 | Elasticsearch | ElasticsearchRestClientAutoConfiguration | 阶段3 ES | 降级 🟡, 只讲接线 |

### 第 7 层: 深探新增 (5 域, 00 域发现全面普查后补 — 均承载真实 Boot/Actuator 级设计决策)

| # | 域 | 核心主题 | 前置依赖 | 复用/展开 |
|:--:|---|---|---|---|
| S-25 | **Error 处理自动装配** | ErrorMvcAutoConfiguration/BasicErrorController/DefaultErrorAttributes/ErrorViewResolver | **S-7 MVC 自动装配 + C-13 异常** | 展开: 错误响应语义/whitelabel//error 映射/ErrorAttributes; 复用: MVC 内核 |
| S-26 | **Actuator Health 聚合** | AutoConfiguredHealthEndpointGroups/StatusAggregator/HealthContributorRegistry/IncludeExcludeGroupMemberPredicate | **S-21 Actuator 端点 + S-19 可用性** | 展开: 组件健康→整体状态聚合/groups 编排/status 合并; 复用: 端点机制 |
| S-27 | **Metrics/Micrometer 编排** | MeterRegistryPostProcessor/CompositeMeterRegistry/PropertiesMeterFilter | **S-21 + S-9 HTTP 客户端** | 展开: 多 registry 组合/customizer/filter 编排; 复用: 端点机制 |
| S-28 | **Spring Data 仓库自动注册** | AbstractRepositoryConfigurationSourceSupport/各 RepositoriesRegistrar/OnRepositoryTypeCondition | **S-10/S-11 + Spring Data 层** | 展开: 仓库扫描注册编排 + imperative/reactive 选择; **JPA 仓库按现代主流降级**(Redis/Mongo/JDBC 为主) |
| S-29 | **SQL 初始化** | SqlDataSourceScriptDatabaseInitializer/OnDatabaseInitializationCondition | **S-10 DataSource** | 展开: schema.sql/data.sql 执行时机/条件编排; 复用: DataSource |

---

## 与原始规划 (B-1~B-24) 的差异

| 原始 | 重构 | 理由 |
|:--:|:--:|---|
| B-1/B-2 | S-1 + **S-2 新增** + S-3 | 自动装配加载机制 (AutoConfigurationImportSelector) 是 Boot 灵魂, 必须独立 |
| B-4 | S-5 | 补松弛绑定/绑定底层 (Binder) |
| B-16 | S-17 | 🟡→🔴 深化: 外部化配置 17 级优先级/ConfigData |
| B-18+B-19 | S-19 (合并) | 两个薄域合并进启动运行时 |
| B-10/B-24 | S-11/S-24 | 降级为"只讲接线", 深入在阶段3 |
| — | 全部域 | 新增"前置依赖 + 复用/展开"双标注 — 知识网络化 |

## 执行顺序

按层推进: S-1→S-2→S-3 (核心机制) → S-4→S-5→S-6 → S-7→S-8→S-9 (Web) → S-10~S-13 (数据) → S-14~S-15 → S-16~S-19 → S-20~S-24 → **S-25~S-29 (深探新增: Error→Health→Metrics→SpringData仓库→SQL初始化)**。每域走 v5 全管线 (KP→大纲→questions→六层深审), 跨域引用按 06 策略, 复用按 06 §2.5 五件事检查。

## 知识网络图 (跨大纲边)

```
Tomcat (T-1~T-6) ──→ S-8 嵌入式容器
Tomcat (t7-springboot-integration) ──→ S-8 (⚠️ t7 已覆盖 Tomcat 组装 — S-8 引用它, 增量=Jetty/Undertow 对照)
Netty (N-1~N-12) ──→ S-8, S-20
Spring core (C-1~C-10) ──→ S-1~S-6 (注解/环境/转换/条件)
Spring context (s8-s23) ──→ S-4, S-12, S-13, S-15
Spring aop/tx (s24-s33) ──→ S-12
Spring jdbc (s34-s36, C-11) ──→ S-10
Spring web (W-1~W-6, C-12~C-15, C-20~C-21) ──→ S-7, S-8, S-9, S-20
Spring test (C-16~C-19) ──→ S-22
Spring validation (C-22) ──→ S-23
阶段3 (Hikari/Redis/ES) ──→ S-10, S-11, S-24
S-7 MVC ──→ S-25 Error 处理
S-21 Actuator ──→ S-26 Health, S-27 Metrics
S-19 可用性 ──→ S-26 Health (probes)
S-10 DataSource ──→ S-28 SpringData 仓库, S-29 SQL 初始化
```
