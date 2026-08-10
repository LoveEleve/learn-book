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

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：本节大部分为架构师发散；与 docs/前篇重复处已交叉引用。

### 完整认知：Pull 方式指标监控平台在真实架构中完整该讲什么

docs 覆盖了"Actuator Prometheus Endpoint + Eureka 服务发现 + Grafana"。作为架构师，这个主题完整还该包含：

1. **Pull vs Push 的完整权衡**：不只"Prometheus 用 Pull"，而是**Pull vs Push 对比**——Pull(平台主动抓取，可控、易水平扩展，但需服务发现/暴露端点) vs Push(应用主动上报，简单但应用要知道平台地址、可能丢点)——选型看场景（Push 模式本篇只简述，第 16 节为 Push 专题，后续提取时再展开）
2. **指标生命周期与存储**：指标抓取后的**存储、保留期、压缩、查询**（Prometheus 时序库 TSDB 模型、第 13 节指标）+ 远程存储(Thanos/Cortex 水平扩展)
3. **监控平台的完整组件**：不止"抓取+展示"，而是**采集器(Prometheus)、存储(TSDB)、告警(Alertmanager)、可视化(Grafana)、目标发现(服务发现)**——一套完整可观测平台
4. **告警体系**：指标 → 告警规则 → Alertmanager 通知——指标要可告警(第 13 节指标与告警衔接)
5. **服务发现的目标管理**：不只"发现实例"，而是**动态目标发现、目标状态(up/down)、抓取失败处理**——监控要覆盖目标生命周期
6. **监控成本与覆盖**：全量抓取 vs 采样、抓取频率、监控哪些指标(第 13 节指标设计)——监控本身有成本
7. **高可用**：Prometheus 本身要 HA(多副本/远程存储)，避免监控单点

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| Pull vs Push | Pull(可控、易扩展，需发现/暴露端点)；Push(简单，应用主动上报，可能丢点)——Push 为第 16 节专题，本篇仅简述 |
| 内嵌监控 vs 独立平台 | 内嵌(Actuator 端点)轻；独立平台(Prometheus+Grafana)完整但重 |
| 全量抓取 vs 采样 | 全量准但成本高；采样省资源但丢细节(第 13 节) |
| 服务发现 vs 静态配置 | 服务发现(动态、自动，Nacos/K8s)灵活；静态配置(简单)但手动维护 |
| 本地存储 vs 远程存储 | 本地简单但有容量/单点；远程(Thanos/Cortex)可扩展但复杂 |

### 常见坑/反模式

1. **端点未暴露/路径错误**：应用没暴露 /actuator/prometheus 或 metadata 的 prometheus.path 配错，Prometheus 抓不到——配置对不上
2. **服务发现失效**：sd_configs 从 Eureka/Nacos 拿不到实例，或 metadata 标签(prometheus.scrape)没配——动态目标发现失败
3. **抓取失败静默**：Prometheus 抓取失败(应用挂/超时)没告警——要监控 up 状态 + 抓取失败告警
4. **高基数指标撑爆存储**：第 13 节高基数问题在 Prometheus 存储放大——控制 tag 基数
5. **监控单点**：Prometheus 单实例挂了监控全失——要 HA/远程存储
6. **只采集不告警**：指标进了 Prometheus/Grafana 但没配告警规则——异常发现滞后
7. **抓取频率不当**：抓取太频繁(开销大)或太稀疏(延迟发现)——按需设 interval

### 生态位置

- **可观测性平台层**：第 13 节(指标模型) + 第 15 节(Pull 采集) + 第 16 节(Push) + 第 17 节(链路)共同构成完整可观测
- **衔接**：第 13 节(Micrometer 指标，数据来源)、第 11 节(负载均衡用指标)、第 17 节(链路追踪)
- **Prometheus**：国内主流的时序监控，Pull 模式代表；Actuator PrometheusScrapeEndpoint(已源码验证) + microsphere-observability Prometheus 条件注解
- **服务发现**：Eureka(过时)→Nacos/K8s；Prometheus 通过 sd_configs 发现(第 15 节 KP-03)

**架构师视角结论**：本篇不只是"配几个 Prometheus 配置"，而是"**设计完整的 Pull 模式监控平台**"——Pull/Push 权衡、采集/存储/告警/可视化组件、服务发现目标管理、HA 与成本，是可观测性的监控核心。
