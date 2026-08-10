# stage-1 · 第 15 节：基于 Pull 方式指标监控平台设计 — 知识点提取

> 课程：stage-1 服务治理 第 15 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/15. 第十五节：基于 Pull 方式指标监控平台设计.md`
> 提取时间：2026-08-09 | 权重：核心（可观测性平台设计）

---

## 一、本节概览

- **技术域**：Pull 模式监控平台（Prometheus 拉取 + 服务发现 + Grafana 可视化）
- **维度**：`[分布式问题]`（可观测性/监控）+ `[工程问题]`（Spring Boot Actuator）
- **核心命题**：如何设计基于 Pull 方式的指标监控平台——Prometheus 通过注册中心发现实例并拉取指标
- **知识点数**：6 个
- **前置**：Prometheus 概念、Spring Boot Actuator、服务注册发现、Micrometer(第13节)

## 前置条件清单
读者需先掌握：
1. **Prometheus 概念**（抓取/时序库/Pull 模式）
2. **Spring Boot Actuator**（endpoint）
3. **服务注册发现**（Nacos/Eureka 实例元信息）
4. **Micrometer**(第 13 节)
未达前置者，先补：Prometheus 入门 + Spring Boot Actuator + 第13节 Micrometer

## 掌握度
目标读者：**本人（读源码多，Spring/监控熟悉）** — 已确认
讲解策略：Pull 模式 + Actuator endpoint 直接讲（你熟悉）；补 Prometheus 服务发现配置

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Pull 模式监控的本质（Prometheus 拉取）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Prometheus 概念
- **需求**：监控平台主动拉取(Pull)应用指标，而非应用推送(Push)
- **自主实现**：应用暴露指标 HTTP 端点，Prometheus 定期拉取
- **参考实现**：Prometheus 拉取应用 `/actuator/prometheus` 端点；对比 Push(如 Prometheus PushGateway)
- **对比取舍**：Pull vs Push——Pull 由平台主动抓取(更可控)，Push 由应用上报
- **关联 microsphere**：microsphere-observability 有 Prometheus 导出配置

### KP-02 Spring Boot Actuator Prometheus Endpoint
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Actuator、Micrometer
- **需求**：暴露 Prometheus 格式的指标端点
- **自主实现**：加 Actuator + micrometer-registry-prometheus 依赖，暴露 /actuator/prometheus
- **参考实现**（已源码验证）：
  - 依赖：spring-boot-starter-actuator + micrometer-registry-prometheus
  - 配置：`management.endpoints.web.exposure.include=*`
  - `PrometheusScrapeEndpoint`(spring-boot-actuator) + `PrometheusMetricsExportAutoConfiguration`(autoconfigure)
- **对比取舍**：Micrometer 适配 Prometheus 格式，Actuator 暴露 HTTP 端点
- **测试佐证**：`code/spring/spring-boot/.../actuator/.../PrometheusScrapeEndpoint.java` + PrometheusMetricsExportAutoConfiguration

### KP-03 Prometheus 服务发现（拉取目标发现）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[过时→Nacos/K8s]` | **置信度**：High
- **前置**：服务注册发现
- **需求**：Prometheus 需要发现所有应用实例（动态），而非手动配置
- **自主实现**：用注册中心做服务发现，Prometheus 通过 sd_configs 发现实例
- **参考实现**（docs）：Prometheus 用 eureka_sd_configs 从 Eureka 发现服务；relabel 从实例 metadata 读取 prometheus.scrape/path/port；Spring Boot 侧配置 eureka.instance.metadataMap.prometheus.scrape/path/port
- **对比取舍**：服务发现(Pull 目标自动发现)是动态监控的关键；**Eureka 过时→Nacos/K8s**，但 sd_configs 机制通用
- **待验证**：Nacos 的 Prometheus sd_configs 等价物

### KP-04 Spring Boot 侧 Prometheus 元信息配置
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[过时→Nacos]` | **置信度**：High
- **前置**：KP-03
- **需求**：应用注册时上报 Prometheus 抓取元信息(路径/端口)
- **自主实现**：在注册中心实例 metadata 里声明 prometheus 抓取信息
- **参考实现**（docs）：
  - `eureka.instance.metadataMap.prometheus.scrape=true`
  - `prometheus.path=${management.endpoints.web.basePath:/actuator}/prometheus`
  - `prometheus.port=${management.server.port}`
- **对比取舍**：实例 metadata 承载抓取信息，Prometheus 靠它定位抓取路径/端口
- **待验证**：Nacos metadata 等价配置

### KP-05 Grafana 可视化平台
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：监控概念
- **需求**：把 Prometheus 指标图形化展示
- **自主实现**：Grafana 整合 Prometheus 数据源，配置仪表盘
- **参考实现**：docs"整合 Prometheus 数据源，构建 Java 应用监控指标图形化"
- **对比取舍**：Grafana = 可视化层，Prometheus = 存储/查询层
- **待验证**：具体仪表盘配置 docs 未展开

### KP-06 监控平台架构（Pull 模式全链路）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~05
- **需求**：理解 Pull 模式监控平台全链路
- **自主实现**：应用暴露指标 → 注册中心发现 → Prometheus 拉取 → 存储 → Grafana 展示
- **参考实现**：应用(Actuator /actuator/prometheus) → 注册中心(实例发现) → Prometheus(拉取+存储) → Grafana(可视化)
- **对比取舍**：全链路 Pull 模式——服务发现 + 拉取 + 可视化三环节
- **关联 microsphere**：microsphere-observability(micrometer + prometheus)

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| Pull 模式本质 | 分布式 | 核心 | P1 | 🔴 | High |
| Actuator Prometheus Endpoint | 工程 | 核心 | P1 | 🔴 | High |
| Prometheus 服务发现 | 分布式 | 核心 | P1 | 🔴 | High（过时→Nacos/K8s） |
| Spring Boot 元信息配置 | 工程 | 支撑 | P2 | 🟡 | High（过时→Nacos） |
| Grafana 可视化 | 工程 | 支撑 | P3 | 🟢 | Medium |
| 监控平台架构 | 分布式 | 核心 | P1 | 🟡 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **Actuator Prometheus**：`code/spring/spring-boot/.../PrometheusScrapeEndpoint.java` + PrometheusMetricsExportAutoConfiguration
- **microsphere-observability**：`ConditionalOnEnabledPrometheusMetricsExport`（Prometheus 导出条件注解）
- **服务发现**：Eureka(过时)→Nacos(主流)

---

## 五、本节小结（三层次视角）

**需求**：设计 Pull 模式监控平台——Prometheus 通过注册中心发现实例并拉取指标，Grafana 可视化。

**自主实现核心**：若我设计——
1. 应用暴露指标端点(/actuator/prometheus)
2. 实例注册时上报 prometheus 抓取元信息(metadata)
3. Prometheus 用 sd_configs 从注册中心发现实例
4. Prometheus 拉取 + 存储 + Grafana 展示

**参考实现**：Spring Boot Actuator PrometheusScrapeEndpoint + PrometheusMetricsExportAutoConfiguration（源码验证）+ microsphere-observability 的 Prometheus 条件注解。Eureka(过时)→Nacos。

**对比取舍**：知识本体是"Pull 模式监控 + 服务发现"。核心洞察：**应用暴露端点 + 注册中心发现 + Prometheus 拉取**是 Pull 模式三环节。Prometheus 通过实例 metadata(prometheus.scrape/path/port)定位抓取目标。

**待验证汇总**：
- Nacos 的 Prometheus sd_configs 等价物
- Grafana 仪表盘具体配置
