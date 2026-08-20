# S-4 SpringApplication.run — 启动全流程 (环境→容器→refresh→回调)

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | SpringApplication(1847行, run 流程)+SpringApplicationRunListeners+ApplicationContextFactory+WebApplicationType
> 基线: BOOT-PLAN-v2 S-4 — Boot 启动主流程; 前置: **s8 refresh(复用) + C-9 ApplicationRunner(复用 callRunners/ready 时序) + C-3 Environment** — 展开 run 的环境准备/容器创建/prepareContext 增量

---

## §0.8

- 🔴 Deep，1篇 — run 总流程(L301: starting→prepareEnvironment→createApplicationContext→prepareContext→refreshContext→callRunners→ready) → 环境准备(prepareEnvironment: WebApplicationType.deduceFromClasspath→getOrCreateEnvironment→configureEnvironment→environmentPrepared 事件→bindToSpringApplication) → 容器创建(createApplicationContext: ApplicationContextFactory 按类型产 Servlet/Reactive/普通容器) → prepareContext(初始化器/主源注册) → refreshContext(s8 refresh 复用)
- 设计模式: [模式: 模板方法]—run 固定流程; [模式: 工厂]—ApplicationContextFactory 按 Web 类型产容器; [模式: 事件广播]—SpringApplicationRunListeners 各阶段

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SpringApplication.java:301 | run() | **总流程**: L306 bootstrapContext → L310 starting → L313 prepareEnvironment → L315 createApplicationContext → L317 prepareContext → L318 refreshContext → L325 callRunners → L332 ready | High |
| SpringApplication.java:350,469 | 环境准备 | **prepareEnvironment**: L350 getOrCreateEnvironment(按 WebApplicationType 建 StandardServletEnvironment 等)→configureEnvironment(L492)→listeners.environmentPrepared(L353)→bindToSpringApplication(L358, 绑定 spring.main.*) | High |
| SpringApplication.java:275 | 类型推断 | **WebApplicationType.deduceFromClasspath**: 从 classpath 是否存在 Servlet/WebFlux 类推断 SERVLET/REACTIVE/NONE | High |
| SpringApplication.java:573 | 容器创建 | **createApplicationContext**: applicationContextFactory.create(webApplicationType) — AnnotationConfigServletWebServerApplicationContext/Reactive/AnnotationConfig 三选一 | High |
| SpringApplication.java:317 | prepareContext | **prepareContext**: 注册主配置源/ApplicationContextInitializer 应用/environment 注入 | High |
| SpringApplication.java:318,325,332 | 后续 | refreshContext(s8 refresh 复用) → callRunners(C-9 复用) → listeners.ready(C-9 已述 ready 在 runners 后) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: run 主流程+环境+容器创建约 500 行核心 — 知识主线: "run 六步 → 环境准备细节 → 容器类型选择". 1篇 (🔴 ~50行) 按"总流程→环境→容器"展开; callRunners/ready 复用 C-9, refresh 复用 s8(06 §2.5: 机制复用, 时序衔接展开)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | run() 六步总流程 (starting→prepareEnvironment→createApplicationContext→prepareContext→refreshContext→回调) | 🔴 | **为什么🔴**: Boot 启动地图 — 每步做什么、与 s8 refresh 的衔接点 |
| P1-2 | prepareEnvironment (类型推断→创建→配置→事件→绑定) | 🔴 | **为什么🔴**: 环境在 refresh 前就绪 — 外部化配置生效的入口(C-3 衔接) |
| P1-3 | createApplicationContext (Web 类型→容器工厂三选一) | 🔴 | **为什么🔴**: Servlet/Reactive/普通容器怎么选 — WebApplicationType.deduceFromClasspath |
| P2-1 | prepareContext (初始化器/主源) | 🟡 | **为什么🟡**: 容器创建到 refresh 之间的装配 |
| P2-2 | 与 C-9/s8 的时序衔接 (callRunners/ready/refresh) | 🟡 | **为什么🟡**: 复用≠省略 — 启动时序全景 |
| P3-1 | SpringApplicationRunListeners (starting/ready 事件) | 🟢 | **为什么🟢**: 阶段广播机制 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **run 主流程** (六步) | 🔴 | 启动地图 |
| B | **环境准备** (prepareEnvironment) | 🔴 | 配置生效入口 |
| C | **容器创建与衔接** (createApplicationContext + prepareContext + refresh) | 🟡 | 类型选择与装配 |

> **Cluster A (§1)**: run() 六步总流程 + SpringApplicationRunListeners 事件
> **Cluster B (§2)**: prepareEnvironment(类型推断/创建/配置/事件/绑定) + bindToSpringApplication
> **Cluster C (§3)**: createApplicationContext(三容器选型) + prepareContext(初始化器) + refreshContext(s8)/callRunners(C-9) 衔接

→ 引出 S-5: @ConfigurationProperties — 环境里 spring.main.* 怎么被绑定到 SpringApplication?Binder/松弛绑定 — 属性绑定的核心机制

(End of file - total 61 lines)
