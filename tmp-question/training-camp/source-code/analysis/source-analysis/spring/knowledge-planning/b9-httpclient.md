# S-9 HTTP客户端+消息转换 — Jackson/HttpMessageConverters/RestClient 自动装配

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | JacksonAutoConfiguration+HttpMessageConvertersAutoConfiguration+HttpMessageConverters+JacksonHttpMessageConvertersConfiguration+RestClientAutoConfiguration
> 基线: BOOT-PLAN-v2 S-9 — Web 层收尾; 前置: **W-5 MessageConverter(机制复用) + C-2 类型转换 + S-5 绑定** — 展开三族自动装配与 RestClient

---

## §0.8

- 🟡 Working，1篇 — Jackson(ObjectMapper 自动装配: Jackson2ObjectMapperBuilder→ObjectMapper, @ConditionalOnMissingBean 用户优先 + Module beans 自动注册) → 转换器(HttpMessageConvertersAutoConfiguration: @Import 三族(Jackson/Gson/Jsonb) + HttpMessageConverters 合并: 用户转换器优先+默认集合; MappingJackson2HttpMessageConverter 由 JacksonHttpMessageConvertersConfiguration 建) → 客户端(RestClientAutoConfiguration: RestClient.Builder + HttpMessageConvertersRestClientCustomizer 应用转换器)
- 设计模式: [模式: 条件装配]—用户优先; [模式: 合并]—HttpMessageConverters 定制+默认; [模式: 定制器]—RestClientCustomizer

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| JacksonAutoConfiguration.java:87,89,131 | ObjectMapper | **@AutoConfiguration L87**: @ConditionalOnClass(ObjectMapper) — jacksonObjectMapper L131(@ConditionalOnMissingBean — 用户自定义 ObjectMapper 则跳过; 用 Jackson2ObjectMapperBuilder 构建) | High |
| JacksonAutoConfiguration.java(Module 注册) | Module | **Module 自动注册**: 容器全部 Module beans → 注册进所有 ObjectMapper — 自定义序列化模块自动生效 | High |
| HttpMessageConvertersAutoConfiguration.java:62,64,67 | 转换器装配 | **@ConditionalOnClass(HttpMessageConverter) L62** + @Import(Jackson/Gson/Jsonb 三族转换器配置, L64) — messageConverters L71(@ConditionalOnMissingBean) | High |
| HttpMessageConverters.java:57,111,184 | 合并 | **HttpMessageConverters L57**: 构造 L111: additional(用户转换器) + addDefaultConverters → getDefaultConverters L184 — 用户优先, 默认补充 | High |
| JacksonHttpMessageConvertersConfiguration.java:44,50 | Jackson 转换器 | **MappingJackson2HttpMessageConverterConfiguration L44**: mappingJackson2HttpMessageConverter L50(@Bean @ConditionalOnMissingBean — 用 ObjectMapper 建 W-5 转换器) | High |
| RestClientAutoConfiguration.java:56,58 | RestClient | **@ConditionalOnClass(RestClient) L56**: Builder bean + httpMessageConvertersRestClientCustomizer L61-66(把 HttpMessageConverters 应用到 RestClient) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 三族自动装配约 700 行 — 知识主线: "ObjectMapper 装配 → 转换器装配与合并 → RestClient 应用". 1篇 (~46行) 按"Jackson→转换器→客户端"展开; W-5 转换器机制复用(只讲装配), RestClient 本体(spring-web 新类)展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | JacksonAutoConfiguration (ObjectMapper 装配 + 用户优先) | 🔴 | **为什么🔴**: 序列化基础 — 自定义 ObjectMapper 覆盖默认 |
| P1-2 | HttpMessageConverters 合并 (用户转换器优先 + 默认集合) | 🔴 | **为什么🔴**: 转换器链的装配规则 — W-5 的 Boot 侧 |
| P1-3 | RestClient 自动装配 (Builder + Customizer 应用转换器) | 🔴 | **为什么🔴**: 新 HTTP 客户端 — spring-web 新类, 本域展开 |
| P2-1 | Module 自动注册 (自定义序列化模块) | 🟡 | **为什么🟡**: Jackson 扩展点 |
| P2-2 | @Import 三族转换器 (Jackson/Gson/Jsonb 条件) | 🟡 | **为什么🟡**: 多 JSON 库切换 |
| P3-1 | 与 W-5 边界 (机制 vs 装配) | 🟢 | **为什么🟢**: 复用≠省略 — 转换器机制在 W-5 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **Jackson 装配** (ObjectMapper + Module) | 🔴 | 序列化基础 |
| B | **转换器装配** (AutoConfiguration + 合并) | 🔴 | W-5 的 Boot 侧 |
| C | **客户端** (RestClient + Customizer) | 🔴 | HTTP 客户端 |

> **Cluster A (§1)**: JacksonAutoConfiguration(ObjectMapper 条件装配 + Module 注册)
> **Cluster B (§2)**: HttpMessageConvertersAutoConfiguration(@Import 三族) + HttpMessageConverters 合并 + MappingJackson2 转换器 Bean
> **Cluster C (§3)**: RestClientAutoConfiguration(Builder + Customizer) + 与 RestTemplate/WebClient(C-15) 对照

→ 引出 S-10: DataSource/Hikari — Web 层收束, 数据访问: DataSourceAutoConfiguration 条件装配与池化选择(C-11 复用)

(End of file - total 61 lines)
