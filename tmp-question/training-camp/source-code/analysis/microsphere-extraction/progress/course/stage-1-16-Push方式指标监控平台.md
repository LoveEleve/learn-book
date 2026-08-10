# stage-1 · 第 16 节：基于 Push 方式指标监控平台设计 — 知识点提取

> 课程：stage-1 服务治理 第 16 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/16. 第十六节：基于 Push 方式指标监控平台设计.md`
> 提取时间：2026-08-09 | 权重：核心（Push 模式是可观测性另一半）

---

## 一、本节概览

- **技术域**：Push 模式监控（Prometheus Pushgateway）+ Micrometer 多注册中心（Prometheus/InfluxDB）
- **维度**：`[分布式问题]`（可观测性）+ `[工程问题]`（Actuator/注册中心）
- **核心命题**：与第 15 节 Pull 相对，如何设计 Push 模式监控——应用主动推送到 Pushgateway，Prometheus 拉 Pushgateway；以及 Micrometer 多注册中心及时序库差异
- **知识点数**：6 个
- **前置**：Prometheus（第 15 节）、Micrometer（第 13 节）、Spring Boot Actuator

## 前置条件清单
读者需先掌握：
1. **Prometheus/Pull 模式**（第 15 节：拉取、服务发现）
2. **Micrometer 指标模型**（第 13 节：Registry/指标类型）
3. **Spring Boot Actuator**（metrics export 配置）
4. **时序数据库概念**（Prometheus TSDB/InfluxDB）
未达前置者，先补：第 15 节 Pull 模式 + 第 13 节 Micrometer

## 掌握度
目标读者：**本人（读源码多，Spring/监控熟悉）** — 已确认
讲解策略：Pushgateway + Actuator 配置直接讲（你熟悉）；补多注册中心/时序库差异

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Push 模式监控的本质（应用主动推送）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：第 15 节 Pull
- **需求**：应用主动把指标推送到监控平台，而非平台拉取（第 15 节 Pull 的相对）
- **自主实现**：应用周期上报指标到 Pushgateway，Prometheus 再拉 Pushgateway
- **参考实现**：Prometheus **Pushgateway**——应用 push 到它，Prometheus 把它当 job 拉取；Spring Boot `management.metrics.export.prometheus.pushgateway.enabled=true`
- **对比取舍**：**Push vs Pull**——Push 应用主动(简单、适合短生命周期任务/批处理，但应用要知道平台地址)；Pull 平台主动(第 15 节，可控、易扩展，但需暴露端点/服务发现)
- **测试佐证**：Spring Boot `PrometheusPushGatewayManager`（actuator，已源码验证）

### KP-02 Prometheus Pushgateway 搭建与配置
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：搭建 Pushgateway 接收应用推送
- **自主实现**：启动 Pushgateway 进程 + 配置成 Prometheus job
- **参考实现**（docs）：Pushgateway 服务用 DNS 注册域名、Nginx 做集群反向代理；Prometheus 配置 `job_name: prometheus-pushgateway` + `static_configs` 指向 pushgateway
- **对比取舍**：Pushgateway 是"中转站"——应用 push 到它，Prometheus 拉它；集群用 Nginx 代理
- **待验证**：Pushgateway 集群细节 docs 未完全展开

### KP-03 Spring Boot 推送指标到 Pushgateway（配置）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Actuator、KP-01
- **需求**：Spring Boot 应用把 Micrometer 指标推送到 Pushgateway
- **自主实现**：加 Pushgateway client 依赖 + 配置 push 参数
- **参考实现**（docs）：
  - 依赖：`simpleclient_pushgateway`
  - 配置：`management.metrics.export.prometheus.pushgateway.enabled=true` / `baseUrl` / `pushRate`(10s) / `job`（spring.application.name-metrics-push-job）
- **对比取舍**：Micrometer 用 Prometheus 通讯协议推送到 Pushgateway；pushRate 控制推送频率
- **测试佐证**：`PrometheusPushGatewayManager` + PrometheusProperties（actuator）

### KP-04 Micrometer Prometheus 注册中心
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Micrometer（第 13 节）、Prometheus
- **需求**：用 Micrometer 的 Prometheus 注册中心（Pull 或 Push 适配）
- **自主实现**：选 Prometheus 注册中心，指标以 Prometheus 格式导出
- **参考实现**：Micrometer `PrometheusMeterRegistry`（独立 artifact `micrometer-registry-prometheus`）；Spring Boot 用它暴露 /actuator/prometheus（第 15 节 Pull）或 Pushgateway（本篇 Push）
- **对比取舍**：Prometheus 注册中心支持 Pull（暴露端点）与 Push（Pushgateway）两种模式
- **待验证**：PrometheusMeterRegistry 具体实现（本地无该模块）

### KP-05 Micrometer InfluxDB 注册中心（时序库差异）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：Micrometer、时序库
- **需求**：对比不同时序数据库的指标注册中心
- **自主实现**：切到 InfluxDB 注册中心，理解与 Prometheus 的差异
- **参考实现**：Micrometer `InfluxMeterRegistry`（`micrometer-registry-influxdb`）；docs"切换 InfluxDB 注册中心，了解两种时序库差异"
- **对比取舍**：**Prometheus vs InfluxDB**——Prometheus(拉取为主、内置 TSDB、PromQL)；InfluxDB(写入为主、类 SQL 查询)——不同时序库适配不同场景
- **待验证**：InfluxDB 注册中心具体实现（本地无该模块）

### KP-06 指标监控平台混合模式（Pull + Push 混搭）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：第 15 节 Pull、本篇 Push
- **需求**：结合 Pull 和 Push，按场景用不同模式
- **自主实现**：常规服务用 Pull（第 15 节），批处理/短生命周期任务用 Push（Pushgateway），混搭
- **参考实现**：docs"掌握 Pull 和 Push 监控数据混搭模式"
- **对比取舍**：**混搭原则**——长驻服务 Pull，短任务/批处理/无法暴露端点的用 Push；避免全 Push 或全 Pull 一刀切
- **关联 microsphere**：`[待验证]` microsphere-observability 是否有 Push 支持

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| Push 模式本质 | 分布式 | 核心 | P1 | 🔴 | High |
| Pushgateway 搭建 | 工程 | 核心 | P1 | 🟡 | High |
| Spring Boot 推送配置 | 工程 | 核心 | P1 | 🟡 | High |
| Micrometer Prometheus 注册中心 | 工程 | 核心 | P1 | 🟡 | High |
| Micrometer InfluxDB 注册中心 | 性能 | 支撑 | P2 | 🟡 | Medium |
| Pull+Push 混合模式 | 分布式 | 核心 | P1 | 🟡 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **PushGateway**：Spring Boot `PrometheusPushGatewayManager`（actuator，源码验证）
- **Prometheus 配置**：PrometheusProperties + PrometheusMetricsExportAutoConfiguration（第 15 节已见）
- **Micrometer registry**：Prometheus/InfluxDB 是独立 artifact，本地 micrometer 目录无独立模块（`micrometer-registry-*` 单独发布）
- `[待验证]` microsphere-observability 是否有 Push 支持

---

## 五、本节小结（三层次视角）

**需求**：设计 Push 模式监控——应用主动推送到 Pushgateway，Prometheus 拉取；理解 Micrometer 多注册中心及时序库差异。

**自主实现核心**：若我设计——
1. 应用配置 Pushgateway client + push 参数（enabled/baseUrl/pushRate/job）
2. Pushgateway 作为中转站（应用 push → Prometheus 拉）
3. Micrometer 选注册中心（Prometheus/InfluxDB）
4. **混搭**：长驻服务 Pull（第 15 节）、短任务 Push

**参考实现**：Spring Boot `PrometheusPushGatewayManager` + PrometheusProperties（源码验证）+ docs 的 Pushgateway 配置。Micrometer Prometheus/InfluxDB 注册中心为独立 artifact。

**对比取舍**：知识本体是"**Push 模式监控 + 多注册中心**"。核心洞察：**Push(应用主动) vs Pull(平台主动)** 按场景选，混搭最优。Micrometer 门面(第 13 节)适配多注册中心(Prometheus/InfluxDB)。

**待验证汇总**：
- Pushgateway 集群细节
- PrometheusMeterRegistry / InfluxMeterRegistry 具体实现（独立 artifact，本地无模块）
- microsphere-observability 是否有 Push 支持

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：本节大部分为架构师发散；与 docs/前篇重复处已交叉引用。

### 完整认知：Push 模式监控在真实架构中完整该讲什么

docs 覆盖了"Pushgateway 搭建 + Push 配置 + 多注册中心"。作为架构师，这个主题完整还该包含：

1. **Push vs Pull 的完整权衡**（衔接第 15 节，此处补 Push 侧）：
   - **Pull（第 15 节）**：平台主动抓取，适合**长驻服务**（能暴露端点/服务发现），可控、易水平扩展
   - **Push（本篇）**：应用主动上报，适合**短生命周期任务/批处理/无法暴露端点/网络隔离**的场景（cron 任务、Spark 作业），简单但应用要知道平台地址、可能丢点
   - **结论**：多数长驻微服务用 Pull；任务型/批处理用 Push——**混搭是常态**
2. **Pushgateway 的定位与局限**：Pushgateway 是"临时缓存/中转"，不是长期存储——它保存应用最后一次推送的指标，**不适合长期/多实例聚合**（指标会互相覆盖/丢失聚合语义）；大批量指标应直接进存储
3. **多注册中心/时序库选型**：不只 Prometheus/InfluxDB，而是**时序库选型全谱**——Prometheus（拉取+PromQL+内置TSDB）/ InfluxDB（写入+类SQL）/ Graphite/StatsD——按查询语言、写入模式、生态选（Micrometer 门面适配，第 13 节）
4. **监控可观测性闭环**：Push/Pull 采集 → 存储 → 告警（Alertmanager）→ 可视化（Grafana）→ 告警响应——完整闭环（衔接第 15 节）
5. **Push 场景的网络/安全**：应用主动 push 需要知道 Pushgateway 地址、鉴权——网络策略、认证

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| Pull vs Push | Pull（长驻服务，可控易扩展，需暴露端点）；Push（短任务/批处理，简单但应用知地址/可能丢点）——第 15 节对比 |
| Pushgateway 中转 vs 直推存储 | 中转（统一入口/临时缓存）但局限；直推（实时但应用知存储） |
| Prometheus vs InfluxDB | Prometheus（拉取/PromQL/内置TSDB）；InfluxDB（写入/类SQL）——按查询/写入模式选 |
| Push 频率 | 高频（实时但开销大）；低频（省资源但延迟） |
| 单注册中心 vs 多 | 单（简单）；多（多后端灵活但管理复杂） |

### 常见坑/反模式

1. **Pushgateway 当长期存储**：把 Pushgateway 当 Prometheus 用，指标长期堆积/覆盖——Pushgateway 只该临时中转，长期存储用 TSDB
2. **短任务没 Push 就丢指标**：cron 任务用 Pull（任务结束端点没了）导致指标丢失——任务型必须 Push
3. **多实例 push 覆盖**：多个实例推同一 job，指标互相覆盖丢失聚合——要区分实例/用聚合推送
4. **pushRate 不当**：push 太频繁（开销大）或太稀疏（延迟）——按需设
5. **全 Pull 或全 Push 一刀切**：不管场景全用 Pull 或全 Push——要混搭（长驻 Pull、任务 Push）
6. **安全缺失**：Pushgateway 无鉴权，任意应用可推/污染指标——要认证
7. **重复采集**：既 Pull 又 Push 同一指标，重复——选一种模式

### 生态位置

- **可观测性平台层**：第 13 节（指标模型）+ 第 15 节（Pull）+ 本篇（Push）+ 第 17 节（链路）构成完整可观测
- **衔接**：第 15 节（Pull，本 Push 是相对）、第 13 节（Micrometer 指标/门面）、第 17 节（链路）
- **Prometheus Pushgateway**：Push 模式代表；Spring Boot `PrometheusPushGatewayManager`（源码验证）
- **Micrometer 门面**：适配 Prometheus/InfluxDB 多注册中心（第 13 节）

**架构师视角结论**：本篇不只是"配 Pushgateway 推送"，而是"**理解 Push 模式与混搭**"——Push 适合短任务/批处理，Pull 适合长驻服务，混搭是常态；结合 Micrometer 多注册中心/时序库选型，构成完整可观测性。
