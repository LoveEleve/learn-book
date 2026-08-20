# S-9 HTTP客户端+消息转换 — Jackson/转换器/RestClient 自动装配

> 依赖 W-5 + C-2 + S-5 | 🟡 Working | 6 KP | [模式: 条件装配 + 合并 + 定制器]

**读者处境**: ObjectMapper 谁创建的?怎么覆盖?消息转换器怎么配?RestClient 自动装配了什么?

### 1. JacksonAutoConfiguration — ObjectMapper 装配

场景: @Autowired ObjectMapper 直接用 — 谁建的?怎么让自定义生效?

源码路径:
- `JacksonAutoConfiguration.java:87,89,131` — **装配**: @AutoConfiguration(L87) + @ConditionalOnClass(ObjectMapper)(L89) — jacksonObjectMapper L131: @ConditionalOnMissingBean(用户自定义 ObjectMapper 则跳过) + 用 Jackson2ObjectMapperBuilder 构建(含默认配置: 时区/日期格式等)
- Module 自动注册: 容器全部 `Module` beans → 注册进所有 ObjectMapper — 自定义序列化模块(如 JSR310/Kotlin)自动生效

关键设计: **Why @ConditionalOnMissingBean？** 用户要定制序列化(自定义 ObjectMapper)时, 自动装配必须让位 — S-3 用户优先原则的又一应用; Jackson2ObjectMapperBuilder 是"默认配置的载体", 用户也可用它定制。[模式: 条件装配 + 构建器]

数据流: 有 jackson-databind(通过 starter-json) → JacksonAutoConfiguration 激活 → 容器无 ObjectMapper → L131 用 Jackson2ObjectMapperBuilder 构建 → 注册 → 所有 Module beans 注册进它 → @Autowired ObjectMapper 可用。用户 @Bean ObjectMapper(自定义) → @ConditionalOnMissingBean 不匹配 → 用用户的。

### 2. HttpMessageConverters 装配与合并

场景: 消息转换器(W-5 机制)在 Boot 怎么装配?用户添加的转换器怎么与默认共存?

源码路径:
- `HttpMessageConvertersAutoConfiguration.java:62,64,67` — **装配**: @ConditionalOnClass(HttpMessageConverter)(L62) + @Import(Jackson/Gson/Jsonb 三族转换器配置)(L64) — messageConverters L71(@ConditionalOnMissingBean — 用户自定义 HttpMessageConverters 则跳过)
- `HttpMessageConverters.java:57,111,184` — **合并**: L111 构造: additional(用户转换器) + addDefaultConverters ? getDefaultConverters(L184: 取 WebMvcConfigurationSupport.getMessageConverters + RestTemplate.getMessageConverters 合并 — ByteArray/String/Jackson 等, W-5 机制) — **用户优先, 默认补充**
- `JacksonHttpMessageConvertersConfiguration.java:44,50` — **Jackson 转换器**: mappingJackson2HttpMessageConverter L50(@Bean @ConditionalOnMissingBean — 用 ObjectMapper 建 W-5 的 MappingJackson2HttpMessageConverter)

关键设计: **Why 用户优先 + 默认补充？** 用户配置转换器是"覆盖"而非"全替换" — 用户加的排前面, 默认的补齐未覆盖类型; 与 W-5 的转换器链顺序语义一致。**Why 与 W-5 边界？** 转换器机制(canRead/canWrite/匹配)在 W-5, 本域只讲"集合怎么装配/合并" — 06 §2.5 复用。[模式: 合并集合]

数据流: HttpMessageConvertersAutoConfiguration 激活 → @Import: JacksonHttpMessageConvertersConfiguration → mappingJackson2HttpMessageConverter(L50, 用 §1 的 ObjectMapper) → messageConverters(L71): 收集用户转换器 Bean + getDefaultConverters(默认) → HttpMessageConverters(用户优先+默认) → 注入 requestMappingHandlerAdapter(S-7 W-3 使用)。

### 3. RestClient 自动装配

场景: RestClient.Builder 自动注入 — 怎么配好转换器/SSL?

源码路径:
- `RestClientAutoConfiguration.java:56,58` — **自动装配**: @ConditionalOnClass(RestClient)(L56) + @AutoConfiguration(after=HttpClient/HttpMessageConverters)(L54) — 提供 RestClient.Builder bean
- `RestClientAutoConfiguration.java:61,66` — **定制**: httpMessageConvertersRestClientCustomizer L61-66: 把 HttpMessageConverters(§2)应用到 RestClient.Builder — 客户端与服务器共用转换器配置
- RestClient(spring-web/client): 新 HTTP 客户端(接口式 fluent) — 与 C-15 的 WebClient(响应式)对照

关键设计: **Why Customizer 应用转换器？** RestClient 用 HttpMessageConverters 编解码 — 自动装配把 §2 的转换器集合通过 Customizer 接入 Builder, 客户端无需重复配置; 与服务器(S-7)共用一套转换器语义。[模式: 定制器 + 共享配置]

数据流: RestClientAutoConfiguration 激活 → RestClient.Builder bean(prototype) → httpMessageConvertersRestClientCustomizer: messageConverters.getIfUnique() → 应用进 Builder → 用户注入 Builder → restClient.get().uri(...).retrieve().body(...) — 编解码走 §2 转换器(W-5 机制)。

→ 引出 S-10: DataSource/Hikari — Web 层收束, 数据访问层: DataSourceAutoConfiguration 条件装配(嵌入式/池化选择) — 池化机制复用 C-11。
