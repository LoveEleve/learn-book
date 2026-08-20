# S-24 Elasticsearch — ElasticsearchRestClientAutoConfiguration (只讲接线)

> 依赖 阶段3 ES (连接/协议, 不展开) | 🟡 Working (降级) | 6 KP | [模式: 条件装配 + 连接细节抽象]

**读者处境**: 加 `spring-boot-starter-data-elasticsearch` + 配 `spring.elasticsearch.uris` — 注入的 RestClient 从哪来?本域只讲自动装配怎么把 uris 接成 RestClient bean(连接/协议深入在阶段3 ES)。

### 1. 装配入口与条件 — ElasticsearchRestClientAutoConfiguration

场景: 有 ES 依赖时, 自动装配怎么决定"要创建 ES 客户端"?

源码路径:
- `ElasticsearchRestClientAutoConfiguration.java:38,39,41` — **条件**: `@AutoConfiguration(after = SslAutoConfiguration.class)`(L38) + `@ConditionalOnClass(RestClientBuilder.class)`(L39, 需 ES 客户端类) + `@EnableConfigurationProperties(ElasticsearchProperties.class)`(L40, 绑定配置) + `@Import(RestClientBuilderConfiguration, RestClientConfiguration, RestClientSnifferConfiguration)`(L41)

关键设计: **Why @ConditionalOnClass(RestClientBuilder)？** 只有引入 ES 官方客户端(elasticsearch-rest-client)才有 RestClientBuilder — 有类才装配, 无依赖自动跳过; 该条件是 S-3 条件评估引擎(S-2 自动装配管线)的通用机制, 与 @ConditionalOnMissingBean(§2/§3 可被用户覆盖)配套。[模式: 条件装配]

数据流: 引入 ES 依赖 → ElasticsearchRestClientAutoConfiguration 评估 → @ConditionalOnClass(RestClientBuilder) 命中 → 绑定 ElasticsearchProperties + @Import 三个配置类(§2/§3)。

### 2. RestClientBuilder 装配 — uris → builder

场景: 配置里的 uris 地址怎么进到 RestClientBuilder?

源码路径:
- `ElasticsearchRestClientConfigurations.java:69,91,93` — **builder**: `@ConditionalOnMissingBean(RestClientBuilder)`(L69) → `elasticsearchRestClientBuilder(...)`(L91): `RestClient.builder(connectionDetails.getNodes()...)`(L93) — 用连接细节的节点数组建 builder, 再应用 customizers(SSL/请求配置)
- `ElasticsearchRestClientConfigurations.java:80` — **connectionDetails**: `PropertiesElasticsearchConnectionDetails`(L80) — 从 ElasticsearchProperties 读 uris 提供节点

关键设计: **Why 抽出 ElasticsearchConnectionDetails？** 连接地址来源可多种(属性/云/外部), 用抽象隔离 — RestClientBuilder 只依赖 ConnectionDetails.getNodes(), 不直接读 properties; 属性版是默认实现。[模式: 连接细节抽象]

数据流: ElasticsearchProperties.uris(默认 localhost:9200) → PropertiesElasticsearchConnectionDetails(L80) → getNodes() 得到节点数组 → RestClient.builder(nodes)(L93) → 应用 customizers(SSL/超时) → RestClientBuilder。

### 3. RestClient bean + 边界 — builder.build()

场景: builder 怎么变成可注入的 RestClient bean?

源码路径:
- `ElasticsearchRestClientConfigurations.java:127,131,132` — **RestClient**: `@ConditionalOnMissingBean(RestClient.class)`(L127) → `elasticsearchRestClient(RestClientBuilder)`(L131): `return restClientBuilder.build()`(L132) — 由 builder 构建最终客户端 bean
- 边界: 连接建立/HTTP 协议/索引操作等深入在**阶段3 ES**(非本仓库), 本域只讲"自动装配把 uris 接成 RestClient" — 降级只讲接线

关键设计: **Why 降级只讲接线？** ES 客户端机制庞大(连接/协议/CRUD), 与阶段3(数据与存储)重叠 — BOOT-PLAN 将本域标为降级 🟡, 只讲"自动装配怎么接线"; 深入(连接池/协议)留给阶段3 ES 域。**Why 分 builder/client 两个 Bean？** builder 可定制(SSL/超时), client 由 builder 构建 — 用户可注入 RestClientBuilder 自行定制或注入 RestClient 直接用。[模式: 只讲接线 + 边界声明]

数据流: RestClientBuilder(§2) → RestClientConfiguration(L127) → @Bean elasticsearchRestClient(L131) → restClientBuilder.build()(L132) → 容器里可注入的 RestClient bean → 用户 @Autowired RestClient 使用(连接/协议深入在阶段3 ES)。

→ 引出 BOOT 收束: 至此 BOOT-PLAN-v2 26 域全部完成 — 下一步进入原始计划阶段 3 数据与存储(如 HikariCP 等)。
