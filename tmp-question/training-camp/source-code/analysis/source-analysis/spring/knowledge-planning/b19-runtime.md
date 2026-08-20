# S-19 启动运行时 — 可用性(Liveness/Readiness) + ApplicationReadyEvent + 虚拟线程开关

> 项目: Spring Boot 3.x | 🟡 Working / 2 篇 | 篇①可用性: AvailabilityState(30行)+LivenessState(40行)+ReadinessState(40行)+ApplicationAvailability(80行)+ApplicationAvailabilityBean(110行)+AvailabilityChangeEvent(55行)+EventPublishingRunListener(154行)+ApplicationReadyEvent(70行); 篇②虚拟线程: Threading(65行)+OnThreadingCondition(55行)+ConditionalOnThreading(48行)+TaskExecutorConfigurations(160行)
> 基线: BOOT-PLAN-v2 S-19 (合并+拆2篇) — 启动运行时状态/事件/开关; 前置: **C-9 ApplicationRunner(启动回调, ready 前运行) + C-7 TaskExecutor(线程池)** — 展开状态机/事件时序/开关

---

## §0.8

- 🟡 Working，2篇 — **篇① 可用性+ReadyEvent**: 状态模型(AvailabilityState 接口 + LivenessState[CORRECT/BROKEN] + ReadinessState[ACCEPTING/REFUSING] + ApplicationAvailability 查询接口) → 发布时机(EventPublishingRunListener: started→LivenessState.CORRECT, ready→ApplicationReadyEvent+ReadinessState.ACCEPTING_TRAFFIC) → 缓存(ApplicationAvailabilityBean: ApplicationListener<AvailabilityChangeEvent> 缓存每个状态类型 last event, getState 返回) — 供 K8s 探针/actuator 查询
- **篇② 虚拟线程开关**: 开关(spring.threads.virtual.enabled 属性 + Threading enum: VIRTUAL.isActive = 属性 true && Java≥21) → 条件接线(@ConditionalOnThreading→OnThreadingCondition.isActive → TaskExecutorConfigurations: virtual→SimpleAsyncTaskExecutor(virtualThreads), platform→ThreadPoolTaskExecutor) → 差异(虚拟每任务一线程无池化 vs 平台池化)
- 设计模式: [模式: 状态机]—Liveness/Readiness 状态转换; [模式: 事件]—AvailabilityChangeEvent; [模式: 条件配置]—@ConditionalOnThreading

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| LivenessState.java:34,39 | 状态 | **enum LivenessState**: CORRECT(L34)/BROKEN(L39) | High |
| ReadinessState.java:34,39 | 状态 | **enum ReadinessState**: ACCEPTING_TRAFFIC(L34)/REFUSING_TRAFFIC(L39) | High |
| ApplicationAvailability.java:33,39,47 | 接口 | **接口 L33**: getLivenessState(默认 BROKEN)/getReadinessState(默认 REFUSING_TRAFFIC)/getState(stateType, default) | High |
| EventPublishingRunListener.java:102,104,108,110 | 发布 | **started L102**: publish LivenessState.CORRECT(L104); **ready L108**: publish ApplicationReadyEvent+ReadinessState.ACCEPTING_TRAFFIC(L110) | High |
| ApplicationAvailabilityBean.java:38,74 | 缓存 | **implements ApplicationListener<AvailabilityChangeEvent> L38**: onApplicationEvent(L74) 缓存 last event 到 Map; getState(L62) 返回 | High |
| Threading.java:28,48 | 开关 | **enum Threading L28**: VIRTUAL.isActive(L48)= spring.threads.virtual.enabled true && Java≥21; PLATFORM=!VIRTUAL | High |
| OnThreadingCondition.java:36,45 | 条件 | **getMatchOutcome L36**: threading.isActive(environment)(L45) | High |
| TaskExecutorConfigurations.java:61,68,129 | 接线 | **@ConditionalOnThreading(VIRTUAL) L61**→SimpleAsyncTaskExecutor virtualThreads(L129); (PLATFORM) L68→ThreadPoolTaskExecutor | High |

---

## 02-04 聚合+分类+聚类 (2篇)

**2篇理由**: S-19 合并了两个薄域(可用性+虚拟线程)— 机制完全独立, 拆 2 篇(每篇 ~44行) 各自完整; 可用性前置 C-9(启动回调), 虚拟线程前置 C-7/S-14(线程池)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 可用性状态模型 (AvailabilityState + Liveness/Readiness) | 🔴 | **为什么🔴**: 应用"活/就绪"二态抽象 |
| P1-2 | 发布时机 (EventPublishingRunListener: started→Liveness, ready→ReadyEvent+Readiness) | 🔴 | **为什么🔴**: 状态何时翻转 |
| P1-3 | 虚拟线程开关 (Threading enum + spring.threads.virtual.enabled) | 🔴 | **为什么🔴**: 开关属性到线程模型的映射 |
| P2-1 | ApplicationAvailabilityBean 缓存与查询 | 🟡 | **为什么🟡**: 状态怎么被外部(K8s/actuator)读到 |
| P2-2 | @ConditionalOnThreading 条件接线 (virtual vs platform executor) | 🟡 | **为什么🟡**: 开关如何选择执行器 |
| P3-1 | 与 C-9/C-7 边界 (启动回调/线程池在已有域) | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| 篇 | Cluster | KP | 级别 |
|:--:|------|------|:--:|
| 篇① | A **状态模型** | P1-1 | 🔴 |
| 篇① | B **发布时机** | P1-2 | 🔴 |
| 篇① | C **缓存查询** | P2-1 | 🟡 |
| 篇② | D **开关属性** | P1-3 | 🔴 |
| 篇② | E **条件接线** | P2-2 | 🟡 |
| 篇② | F **复用边界** | P3-1 | 🟢 |

> **篇① §1**: LivenessState/ReadinessState/AvailabilityState/ApplicationAvailability
> **篇① §2**: EventPublishingRunListener.started/ready → AvailabilityChangeEvent.publish + ApplicationReadyEvent
> **篇① §3**: ApplicationAvailabilityBean 缓存(last event per state) + getState 查询
> **篇② §1**: Threading enum(spring.threads.virtual.enabled + Java≥21)
> **篇② §2**: @ConditionalOnThreading → TaskExecutorConfigurations(virtual SimpleAsyncTaskExecutor vs platform ThreadPoolTaskExecutor)
> **篇② §3**: 差异(虚拟每任务一线程 vs 池化) + 边界(C-7/S-14 内核)

→ 引出 S-20: WebFlux+Netty — 运行时之后: 响应式服务器工厂 ReactiveWebServerFactory(进入 Actuator/测试层)
