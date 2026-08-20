# S-24 Elasticsearch — ElasticsearchRestClientAutoConfiguration (只讲接线)

> 项目: Spring Boot 3.x | 🟡 Working (降级) / 1 篇 | ElasticsearchRestClientAutoConfiguration.java(42行)+ElasticsearchRestClientConfigurations.java(160行)+ElasticsearchProperties.java(110行)+ElasticsearchConnectionDetails.java(40行)
> 基线: BOOT-PLAN-v2 S-24 (降级 🟡) — 只讲自动装配接线; 前置: **阶段3 ES(连接/协议深入 — 本域不展开)** — 展开接线(uris → RestClient)

---

## §0.8

- 🟡 Working，1篇 — 装配入口(ElasticsearchRestClientAutoConfiguration: @AutoConfiguration + @ConditionalOnClass(RestClientBuilder) + @EnableConfigurationProperties(ElasticsearchProperties) + @Import 3 配置类) → RestClientBuilder 装配(RestClientBuilderConfiguration: ElasticsearchConnectionDetails.getNodes 从 ElasticsearchProperties.uris[默认 localhost:9200] → RestClient.builder + 应用 customizers) → RestClient bean(RestClientConfiguration: restClientBuilder.build() → RestClient)
- 设计模式: [模式: 条件装配]—@ConditionalOnClass; [模式: 连接细节抽象]—ElasticsearchConnectionDetails; [模式: 只讲接线]—连接/协议在阶段3

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ElasticsearchRestClientAutoConfiguration.java:38,39,41 | 入口 | **@AutoConfiguration(L38)+@ConditionalOnClass(RestClientBuilder)(L39)+@Import 3 配置类(L41)** | High |
| ElasticsearchRestClientConfigurations.java:69,91,93 | Builder | **@ConditionalOnMissingBean(L69)+elasticsearchRestClientBuilder(L91)**: RestClient.builder(connectionDetails.getNodes())(L93) | High |
| ElasticsearchProperties.java:38,72 | 配置 | **uris(L38): 默认 http://localhost:9200; getUris(L72)** | High |
| ElasticsearchRestClientConfigurations.java:127,131,132 | RestClient | **@ConditionalOnMissingBean(L127)+elasticsearchRestClient(L131)**: restClientBuilder.build()(L132) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 降级域只讲接线 — 1篇 (~40行) 按"入口 → builder → client"展开; 连接/协议在阶段3 ES(非本仓库), 本域只讲自动装配怎么把 uris 配成 RestClient bean。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 装配入口与条件 (@AutoConfiguration + @ConditionalOnClass) | 🔴 | **为什么🔴**: 何时装配 ES 客户端 |
| P1-2 | RestClientBuilder 装配 (uris → builder) | 🔴 | **为什么🔴**: 连接地址怎么进 builder |
| P1-3 | RestClient bean (builder.build()) | 🔴 | **为什么🔴**: 最终客户端怎么产出 |
| P2-1 | ElasticsearchProperties (uris 配置) | 🟡 | **为什么🟡**: 地址从哪配置 |
| P2-2 | ElasticsearchConnectionDetails 抽象 | 🟡 | **为什么🟡**: 连接细节解耦 |
| P3-1 | 与阶段3 ES 边界 (只讲接线) | 🟢 | **为什么🟢**: 降级边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配入口** | 🔴 | 何时装 |
| B | **builder+client 接线** | 🔴 | uris→RestClient |
| C | **配置与边界** | 🟡 | 地址/降级 |

> **Cluster A (§1)**: ElasticsearchRestClientAutoConfiguration 条件与 @Import
> **Cluster B (§2)**: RestClientBuilderConfiguration(RestClient.builder) + RestClientConfiguration(build)
> **Cluster C (§3)**: ElasticsearchProperties.uris + ConnectionDetails + 阶段3 边界

→ 引出 BOOT 收束: 至此 BOOT-PLAN-v2 26 域全部完成 — 下一步原始计划阶段 3 数据与存储(HikariCP 等)
