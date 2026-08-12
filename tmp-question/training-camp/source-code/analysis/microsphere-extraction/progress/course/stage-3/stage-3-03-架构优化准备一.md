# stage-3 · 第 03 节：[公开课] 第一节：架构优化准备（一）— 知识点提取

> 课程：stage-3 三高架构 第 03 节（公开课）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/03. [公开课] 第一节："高并发、高性能与高可用"架构优化准备（一）.md`
> 提取时间：2026-08-12 | 权重：支撑（动手准备篇——可观测/容错整合的环境与模式）
> 案例载体：my-xhs（决策 B，2026-08-12）

---

## 一、本节概览

- **技术域**：可观测性整合（Micrometer+Prometheus+Grafana）、服务容错整合（Alibaba Sentinel）、基础设施容器化、数据源配置
- **维度**：`[工程问题]`（整合/环境准备）+ `[分布式问题]`（容错可观测化）+ `[性能优化]`（监控）
- **核心命题**：**优化动手前的"准备四件套"**——可观测三件套 + 容错平台 + 基础设施容器化 + 数据源配置；docs 整合节为**空节**，知识本体在架构师补全 + my-xhs 实例
- **知识点数**：8 个
- **前置**：stage-1 第 13/14 节（Micrometer 基础/整合）、第 15/16 节（Pull/Push 监控）、第 08 节（Sentinel 容错）、Docker 基础

## 前置条件清单
读者需先掌握：
1. **Micrometer 指标门面**（stage-1 13/14）
2. **Prometheus 拉取模型 / Grafana 展示**（stage-1 15 Pull 监控）
3. **Sentinel 限流/熔断**（stage-1 08）
4. **Docker Compose 基础**（本篇补）
未达前置者，先补：stage-1 13/14/15/08

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **实例锚定**：可观测/容错/监控基础设施全部对照 my-xhs 实际配置（prometheus.yml/grafana provisioning/SentinelBulkheadConfig）
- **工程化弱**：Docker Compose/连接池参数补基础
- **必做**：docs 空节（§项目整合）按 08 §2 架构师发散补全

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 可观测性整合架构（Micrometer + Prometheus + Grafana 拉取模型）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：stage-1 13/15（Micrometer/Pull 监控）
- **来源**：docs §主要内容 + §监控平台准备 + my-xhs 配置实证
- **需求**：把 JVM/REST/Tomcat/JDBC/JPA 指标统一收进监控平台——优化动作的"仪表盘"
- **自主实现**：若我设计——应用内 Micrometer 注册 Prometheus 注册表暴露 `/actuator/prometheus`，Prometheus 定时拉取，Grafana 展示
- **参考实现**（docs + my-xhs 实证）：**三件套分工**——Micrometer（指标门面，应用内）、Prometheus（拉取存储）、Grafana（可视化）；docs 整合对象：**JVM、REST、Tomcat、JDBC、JPA** 五类指标（衔接 stage-1 13 Micrometer 基础）；docs 关联 stage-1 第八周服务监控平台（15/16 Pull/Push）；**my-xhs 实证**——`my-xhs-common/pom.xml:40-41` `micrometer-registry-prometheus` + `config/prometheus/prometheus.yml:3-7`（global 块：scrape_interval 5s 高频采集利于实时告警、evaluation_interval 15s、scrape_timeout 10s）+ `:9-11`（`rule_files: alert_rules/*.yml` 告警规则文件化）+ `:16-18`（`job_name: my-xhs-services`、`metrics_path: '/actuator/prometheus'`、scrape_interval 10s）
- **对比取舍**：**拉取（Prometheus）vs 推送（Pushgateway/其他）**——拉取天然可发现/存活探测；推送适合短生命周期任务（stage-1 16 Push 对照）
- **测试佐证**：docs §主要内容 + my-xhs `config/prometheus/prometheus.yml:3-7/9-11/16-18` + `my-xhs-common/pom.xml:40-41`

### KP-02 Sentinel 容错整合与 Metrics → Micrometer 适配（容错可观测化）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-1 08（Sentinel）、13/14（Micrometer）
- **来源**：docs §主要内容（第 2 条）+ §项目整合（**空节**）+ my-xhs SentinelBulkheadConfig 实证
- **需求**：容错平台的指标要进监控体系——"限流/熔断发生多少"与"RT/错误率"一起看
- **自主实现**：若我设计——Sentinel 指标经适配器转 Micrometer 指标，随 `/actuator/prometheus` 统一暴露
- **参考实现**（docs 明确 + 架构师发散）：**docs 明确**——Shopizer 整合 Alibaba Sentinel，**适配 Sentinel Metrics 到 Micrometer**（统一进 Prometheus+Grafana）；**架构师发散（docs §项目整合空节，08 §2）**——适配三要素：①依赖 `sentinel-micrometer-adapter`（注册 SentinelMetrics 为 Meter）②配置规则数据源（dashboard/Nacos）③适配 `MetricsRegistry` 到 Prometheus 注册表；**my-xhs 实证**——`common/config/SentinelBulkheadConfig.java`（`@ConditionalOnClass(name = "com.alibaba.csp.sentinel.SphU")` 确认 Alibaba Sentinel）：**舱壁隔离（Bulkhead）+ 热点参数限流**，为不同 Feign 调用配置独立线程池（注释实证：防止库存慢查询拖慢订单创建、支付回调阻塞 Feed 推送），**Nacos 数据源动态规则**（dataId `my-xhs-sentinel-bulkhead-rules`，本地降级兜底）；gateway `GatewayConfig` 的 `sentinel-json-gw-flow-converter`（网关流控规则）
- **对比取舍**：**容错指标进统一监控 vs 平台内看**——统一视图交叉分析（限流触发率 vs 上游 RT）优于 dashboard 孤岛
- **测试佐证**：docs §主要内容 + my-xhs `common/config/SentinelBulkheadConfig.java`（头注释/PostConstruct/Nacos dataId）+ gateway GatewayConfig.java:32-34
- **命名冲突警示**：`Sentinel` 同名两物——**Alibaba Sentinel**（容错限流，docs 03/my-xhs 的 `com.alibaba.csp.sentinel`）vs **Redis Sentinel**（高可用，my-xhs start-all.sh 注释"Redis Sentinel 模式"）——交叉引用时不得混淆（04 §3.2 命名空间意识）

### KP-03 监控基础设施容器化（Docker Compose 编排 Prometheus/Grafana/exporter）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：Docker Compose
- **来源**：docs §监控平台准备（compose 全文）+ my-xhs docker-compose 实证
- **需求**：监控栈一键起——Prometheus/Grafana/主机/DB 四类容器编排
- **自主实现**：若我设计——compose 编排 prometheus + grafana + node-exporter + mysqld-exporter，端口宿主映射
- **参考实现**（docs + my-xhs 实证）：**docs compose 模式**——`prom/prometheus`（宿主 19090→9090）、`grafana/grafana`（13000→3000）、`quay.io/prometheus/node-exporter`（19100→9100，主机指标）、`prom/mysqld-exporter`（9104，`DATA_SOURCE_NAME` 环境变量注入 DB 凭据）；**exporter 模式**——基础设施指标靠 exporter 暴露而非埋点；**MySQL 容器化（docs §环境准备）**——`docker run -itd --name mysql-docker -p 13306:3306 -e MYSQL_ROOT_PASSWORD=123456 mysql:5.7`（**凭据走环境变量注入**模式）+ 初始化 `CREATE DATABASE`；`[过时→mysql:8.x]`（docs 5.7 已老，my-xhs 用 MySQL 8.0.33/3306）；**my-xhs 实证**——`config/docker-compose.yml:591-598`（`prom/prometheus:v2.48.1` + `--config.file`/`--storage.tsdb.path` 启动参数）、`:125`（MySQL healthcheck `mysqladmin ping`）、`:493`（xxl-job-admin 2.4.2）、Sentinel（:59 8858）/Nacos（18848）/Redis 全编排 + 主机名实证
- **对比取舍**：**exporter 旁路采集 vs 应用内埋点**——基础设施指标用 exporter（无侵入），应用指标用 Micrometer（代码内）；**容器化 vs 宿主机安装**——版本一致/可复现 vs 调试直观
- **测试佐证**：docs §环境准备 MySQL + §监控平台准备 compose（19090/13000/19100/9104）+ my-xhs `config/docker-compose.yml:591-598/125/493`

### KP-04 Grafana 仪表盘 Provisioning（配置即代码）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01
- **来源**：架构师发散 + my-xhs config/grafana 实证（docs 未提）
- **需求**：仪表盘可版本化/可复现——"点鼠标配仪表盘"不可审计
- **自主实现**：若我设计——datasource + dashboard JSON 全部 provisioning 文件化
- **参考实现**（my-xhs 实证）：`config/grafana/provisioning/`——`datasources/prometheus.yml`（数据源自动注册）+ `dashboards/dashboard-provider.yml` + `dashboards/json/{biz-metrics,jvm-monitor,api-monitor,tomcat-monitor}.json`（**四类仪表盘**：业务/ JVM / API / Tomcat——正好对应 docs 03 的 JVM/REST/Tomcat 监控对象）
- **对比取舍**：**Provisioning（配置即代码）vs UI 手配**——可版本化/CI 化 vs 上手快
- **测试佐证**：my-xhs `config/grafana/provisioning/`（4 个 dashboard JSON + datasource + provider）

### KP-05 数据源配置化与连接池（database.properties / C3P0 → HikariCP）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[过时→HikariCP/配置中心]` | **置信度**：High
- **前置**：JDBC 连接池概念
- **来源**：docs §Shopizer 项目准备（database.properties 全文）+ my-xhs pom 实证
- **需求**：数据源参数化——环境差异（库地址/凭据/池大小）不硬编码
- **自主实现**：若我设计——连接池参数外置配置文件，环境相关走环境变量/配置中心
- **参考实现**（docs + 架构师）：**docs database.properties 模式**——`db.jdbcUrl/db.user/db.password/db.driverClass` + `db.initialPoolSize=4/db.minPoolSize=4/db.maxPoolSize=8`（C3P0 连接池参数）+ `hibernate.hbm2ddl.auto=update`；**过时处理**——`com.mysql.jdbc.Driver` `[过时→com.mysql.cj.jdbc.Driver]`、`MySQL5InnoDBDialect` `[过时→MySQL8Dialect]`、C3P0 `[过时→HikariCP（Spring Boot 默认）]`（参数对照：minPoolSize/maxPoolSize→Hikari `minimumIdle/maximumPoolSize`）；**现代形态**——配置进 Nacos 配置中心（stage-2 23/24）+ 连接池用 HikariCP；**my-xhs 实证**——`pom.xml:51/169-172` HikariCP 5.1.0 + MyBatis-Plus（`mybatis-plus-spring-boot3-starter`）
- **对比取舍**：**配置文件 vs 配置中心**——docs 时代本地 properties（多环境复制）→ 现代 Nacos 集中管理（动态刷新）
- **测试佐证**：docs §Shopizer 项目准备 + my-xhs `pom.xml:51/169-172` + common pom mybatis-plus

### KP-06 hbm2ddl.auto=update（生产反模式）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[过时/反模式→Flyway/Liquibase]` | **置信度**：High
- **前置**：JPA/Hibernate、数据库迁移
- **来源**：docs §Shopizer 项目准备（database.properties）+ 架构师发散
- **需求**：理解 schema 演化工具选型——"自动改表"不可用于生产
- **自主实现**：若我设计——schema 变更走版本化迁移脚本（Flyway/Liquibase）
- **参考实现**（docs + 架构师）：docs 用 `hibernate.hbm2ddl.auto=update`（开发便利：实体变更自动 DDL）；**反模式**——生产环境 update 会隐式改表结构：无审计/不可回滚/可能锁表/与索引设计冲突；**现代替代**——Flyway/Liquibase 版本化迁移 `[过时→Flyway]`（CI 门禁/可回滚/评审）
- **对比取舍**：**自动 DDL（开发快）vs 迁移脚本（生产稳）**——分环境策略：dev=update，prod=迁移工具
- **测试佐证**：docs database.properties + 架构师实践

### KP-07 Sentinel Dashboard 部署与规则下发
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：Sentinel 基本概念（stage-1 08）
- **来源**：docs §Alibaba Sentinel 平台准备 + my-xhs 实证
- **需求**：容错规则的**管理面**——dashboard 看护实时数据 + 下发规则
- **自主实现**：若我设计——dashboard（可视化/规则下发）+ 规则持久化（Nacos）双轨
- **参考实现**（docs + 架构师 + my-xhs）：**docs 启动命令**——`java -Dserver.port=18080 -Dcsp.sentinel.dashboard.server=... -Dproject.name=sentinel-dashboard -Dsentinel.dashboard.auth.username/password=... -jar sentinel-dashboard.jar`（**JVM 参数配置模式**：dashboard 自身端口 + 连接地址 + 认证）；**规则下发链路**——dashboard → 客户端内存（**不持久化，重启丢失**）→ 生产需接 Nacos 持久化；**my-xhs 实证**——规则走 **Nacos 数据源**（SentinelBulkheadConfig 注释：`dataId: my-xhs-sentinel-bulkhead-rules`，本地降级兜底；gateway `sentinel-json-gw-flow-converter` 拉 Nacos `GatewayFlowRule`）；`config/docker-compose.yml` 含 Sentinel（8858）
- **对比取舍**：**Dashboard 直下（临时）vs Nacos 持久化（生产）**——docs 演示用 dashboard；生产规则必须持久化 + 推送
- **测试佐证**：docs §Sentinel 平台准备 + my-xhs SentinelBulkheadConfig（Nacos dataId）+ GatewayConfig.java:26-34
- **端口警示**：docs 用 18080 跑 dashboard；**my-xhs 18080 是 XXL-Job admin**（config/docker-compose.yml:59）——端口规划要全局视图，同类冲突教训

### KP-08 优化准备方法学（环境/项目/监控/容错四件套）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **来源**：docs 全篇结构 + 架构师整合
- **需求**：建立"优化动手前先造好观察环境"的纪律——**没有监控就谈不上优化**（承接第 02 篇 KP-01 流程第②步"修改前测量"）
- **自主实现**：若我设计——①基础设施容器化 ②项目可构建 ③监控三件套就位 ④容错平台就位，四步齐再动手
- **参考实现**（docs 结构 + 架构师）：**docs 准备四件套**——①环境准备（MySQL Docker + 数据）②Shopizer 项目准备（checkout/install/配置）③监控平台准备（Prometheus+Grafana compose）④Sentinel 平台准备（dashboard）；**方法论意义**——与第 02 篇 KP-01 呼应：调优流程第②步"修改前测量"的**测量基础设施**必须先建好；docs 结尾 §项目整合（空节）预告后续篇的真正整合动作
- **对比取舍**：**先建观察环境 vs 边改边看**——先建成本高但每步优化可量化；边改边看无法回答"改好没"
- **测试佐证**：docs 全篇四节结构 + my-xhs config/（docker-compose/prometheus/grafana/sentinel 全套实证）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 可观测性整合（M+P+G 拉取模型） | 性能优化 | 核心 | P1 | 🔴 | 时间无关 | High |
| Sentinel 整合与 Metrics→Micrometer | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| 监控基础设施容器化（compose/exporter） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| Grafana Provisioning（配置即代码） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| 数据源配置与连接池（C3P0→Hikari） | 工程问题 | 支撑 | P2 | 🟡 | 过时→Hikari | High |
| hbm2ddl.auto=update 反模式 | 工程问题 | 支撑 | P3 | 🟢 | 过时→Flyway | High |
| Sentinel Dashboard 与规则下发 | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| 优化准备方法学（四件套） | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（实例）+ stage-1 产出（交叉引用）
- **关键源码**（本次实证）：
  - `my-xhs-common/pom.xml:40-41`（micrometer-registry-prometheus）
  - `config/prometheus/prometheus.yml:3-7/9-11/16-18`（global 5s、rule_files 告警规则、`/actuator/prometheus` 抓取）
  - `config/grafana/provisioning/`（datasources + 4 dashboard JSON：biz/jvm/api/tomcat）
  - `common/config/SentinelBulkheadConfig.java`（Alibaba Sentinel 舱壁隔离 + Nacos dataId `my-xhs-sentinel-bulkhead-rules`）
  - `pom.xml:51/169-172`（HikariCP 5.1.0）+ `config/docker-compose.yml`（全编排 + Sentinel 8858 + XXL-Job 18080）
- **诚实标注**：docs §项目整合为**完全空节**（仅两行标题）→ KP-02 架构师发散补全（适配三要素）；docs §Prometheus 配置为空节（无 prometheus.yml 内容）→ KP-03 以 my-xhs 实证补全；docs §环境准备 MySQL（docker run + CREATE DATABASE）→ KP-03 已覆盖（含 5.7→8.x 过时标注）；docs 用 mysql:5.7/13306（my-xhs 用 MySQL 8.0.33/3306）——版本/端口差异标注
- **关联标注**：衔接 stage-1 13/14/15/16（Micrometer/Pull/Push，交叉引用前已核对标题）；stage-2 23/24（配置中心承载数据源参数）；后续 29/30 节（日志/监控平台深化）

---

## 五、本节小结（三层次视角）

**需求**：优化动手前的**准备四件套**——可观测三件套 + 容错平台 + 基础设施容器化 + 数据源配置。

**自主实现核心**：若我设计——①compose 编排监控栈（prometheus/grafana/exporter）②应用 Micrometer 暴露 `/actuator/prometheus` ③Sentinel 指标适配 Micrometer 统一看 ④规则持久化 Nacos ⑤连接池 Hikari + 迁移工具 Flyway。

**参考实现**：docs（环境准备 + 整合意图）+ **my-xhs 全套实证**（prometheus.yml/grafana provisioning/SentinelBulkheadConfig/HikariCP）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**优化准备模式**"——先造观察环境再动手（承接 02 篇"测量驱动"）；docs 空节（整合细节）由 my-xhs 实例补全。核心洞察：**可观测性 = 优化的先决条件**。

**待验证汇总**：
- my-xhs 是否用 `sentinel-micrometer-adapter` 显式适配（当前只见 SentinelBulkheadConfig，metrics 适配未 grep 到）——29/30 节展开
- docs §Prometheus 配置空节的原始 prometheus.yml（my-xhs 实证替代）
- my-xhs Grafana 仪表盘 4 JSON 的具体面板内容（本层只确认存在）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标/知识点 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 可观测三件套（M+P+G） | ✅ 完整落地：micrometer-registry-prometheus（common pom:40-41）+ prometheus.yml（/actuator/prometheus 抓取 + alert_rules）+ Grafana provisioning 4 dashboard | 无（最完整的一项） |
| Sentinel Metrics→Micrometer 适配 | ❌ **未落地**：Sentinel 有（Bulkhead 舱壁 + Nacos 规则，SentinelBulkheadConfig 实证），但 **sentinel 指标未适配进 Micrometer/Prometheus**（grep 无 sentinel-micrometer 相关） | 差距：sentinel 指标（限流/熔断计数）未进统一监控——补充适配器或自定义 Meter |
| 监控基础设施容器化 | ✅ docker-compose 全编排（prometheus:591-598 等实证） | 无 |
| Grafana Provisioning | ✅ 4 dashboard JSON（biz/jvm/api/tomcat） | 无 |
| 数据源配置与连接池 | ⚠️ HikariCP 依赖（pom:51/169-172）+ Grafana 采 hikaricp 指标（tomcat-monitor.json 实证）；**池参数未见显式配置** | 现状：默认值；如需调优显式配 minimumIdle/maximumPoolSize |
| hbm2ddl.auto=update | ✅ **已规避**：MyBatis-Plus（无 Hibernate DDL） | 无（若引入 Flyway 更好，当前无迁移工具 `[待验证]`） |
| Sentinel Dashboard/规则 | ✅ 采用现代路径：规则 Nacos 持久化（my-xhs-sentinel-bulkhead-rules dataId 实证） | 无 |

**结论**：03 篇可观测准备几乎全落地；**唯一明显差距 = Sentinel 容错指标未接入统一监控**（docs 主要内容第 2 条未完全达成）。

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为环境准备+整合意图（§项目整合完全空节、§Prometheus 配置空节）；实例全部 my-xhs 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：优化准备的完整架构该讲什么

docs 覆盖环境/监控/容错平台准备。完整还该包含：

1. **可观测性是优化的先决条件**（docs 主线 + 发散）：没有基线测量（02 篇第②步）就没有优化决策——本篇准备的监控栈就是后续每篇优化的"尺子"
2. **指标分层**（架构师发散）：基础设施（node-exporter/mysqld-exporter）→ 应用框架（JVM/Tomcat/JDBC，Micrometer 自动绑定）→ 业务（biz-metrics 自定义）→ 容错（Sentinel 适配）——四层指标各有来源，统一进 Prometheus
3. **Sentinel 规则的生命周期**（docs 演示 + 发散）：dashboard 直下=内存态（重启丢）→ 生产必须 Nacos 持久化 + 灰度下发（my-xhs `my-xhs-sentinel-bulkhead-rules` 实证）——**规则即配置，配置即治理**
4. **配置演进路径**（docs 旧栈 → 现代）：database.properties（本地文件）→ 环境变量 → Nacos 配置中心（stage-2 23/24）——本节的"配置化"思想在后续 25-27 节（配置中心）系统化
5. **Dashboard 数量管控**（架构师发散）：Grafana 面板是团队资产——provisioning 文件化（my-xhs 实证）比手配可审计
6. **连接池参数是性能优化的第一课**（docs 明确 + 发散）：initialPoolSize/maxPoolSize（→Hikari minimumIdle/maximumPoolSize）直接影响吞吐——05 节容器调优时数据源层是重要参数面

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 拉取（Prometheus）vs 推送 | 可发现/存活探测 vs 短生命周期任务适配 |
| Metrics 统一 vs 平台孤岛 | 交叉分析强 vs 适配器成本 |
| exporter vs 埋点 | 无侵入 vs 基础设施专用 |
| Dashboard Provisioning vs UI 手配 | 可版本化/CI vs 上手快 |
| C3P0→HikariCP | 性能/维护活跃度 vs 参数迁移成本 |
| Dashboard 直下 vs Nacos 规则 | 演示快/临时 vs 持久化/灰度 |
| hbm2ddl update vs Flyway | 开发快 vs 生产可控可回滚 |

### 常见坑/反模式

1. **没有监控就优化**（本篇最大坑）：02 篇流程第②步缺失——"改前测量"无法执行，优化变赌博
2. **Sentinel 规则存内存**：dashboard 直下规则重启丢失（生产事故源）——必须 Nacos 持久化
3. **hbm2ddl.auto=update 上生产**：隐式 DDL/锁表/无回滚——Flyway/Liquibase 迁移
4. **"Sentinel" 命名混淆**：Alibaba Sentinel（容错）vs Redis Sentinel（高可用）——同名不同物，排障时先分清
5. **端口规划无全局视图**：docs dashboard 18080 与 my-xhs XXL-Job 18080 撞车——基础设施端口统一登记
6. **仪表盘手配不可审计**：变更无记录、无版本——provisioning 文件化
7. **旧驱动照搬**：`com.mysql.jdbc.Driver` 在新驱动已弃用（8.0 需 `com.mysql.cj.jdbc.Driver`）

### 生态位置

- **stage-3 教学主线**：03/04 准备组——本篇=可观测+容错准备；04 节=优化准备（二）（预判：JVM/工程等更多准备）；05 起进入实操（容器调优/微服务升级）——每篇的"改前测量"都靠本篇的监控栈
- **前后篇衔接**：stage-1 13-16（Micrometer/监控平台——本篇是其在真实项目的整合落地）→ 本篇 → 29/30（日志/监控平台深化）→ 18 节（JVM 故障分析依赖本篇的 JVM 指标面板）
- **与源码提取的关系**：micrometer/sentinel（code/spring）为机制源；my-xhs 为整合实例；29/30 节将深挖 prometheus/grafana 配置

**架构师视角结论**：本篇不是"搭环境教程"，而是**确立"观察优先"的优化纪律**——可观测三件套 + 容错平台 + 配置化数据源是后续所有优化动作的"测量基础设施"；docs 空节（整合细节）在 my-xhs 有完整现代落地（provisioning/舱壁隔离/Nacos 规则），是"docs 骨架 × 实例实证"的典型篇目。
