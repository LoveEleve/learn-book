# S-19 篇① 启动运行时 — 可用性 (Liveness/Readiness 状态机 + ApplicationReadyEvent)

> 依赖 C-9 (ApplicationRunner, ready 前运行) | 🟡 Working | 3 KP | [模式: 状态机 + 事件 + 缓存查询]

**读者处境**: K8s 的 livenessProbe/readinessProbe 打到哪?actuator 的 /actuator/health 怎么知道应用"活着但还没就绪"?启动完成后谁发出 "Started Application in X seconds" 那条日志对应的 ApplicationReadyEvent?— 可用性状态机。

### 1. 状态模型 — Liveness / Readiness 二态

场景: 应用有两个正交状态 — "进程活着"(Liveness)和"能接流量"(Readiness) — 怎么抽象?

源码路径:
- `LivenessState.java:34,39` — **存活态**: enum `LivenessState implements AvailabilityState`: `CORRECT`(L34, 正常)/`BROKEN`(L39, 致命错误需重启)
- `ReadinessState.java:34,39` — **就绪态**: enum: `ACCEPTING_TRAFFIC`(L34, 能接流量)/`REFUSING_TRAFFIC`(L39, 暂不接)
- `ApplicationAvailability.java:33,39,47` — **查询接口**: `getLivenessState()`(L39, 默认 BROKEN)/`getReadinessState()`(L47, 默认 REFUSING_TRAFFIC)/`getState(stateType, default)`(L60) — 统一查询入口

关键设计: **Why 分成两个正交状态？** Liveness 只关心"进程该不该被重启"(OOM/死锁→BROKEN), Readiness 关心"能不能接流量"(启动中/依赖未就绪→REFUSING) — K8s 用 livenessProbe 决定 kill, readinessProbe 决定摘流量; 分开才能独立处理。[模式: 状态机 + 枚举]

数据流: 外部探针调用 applicationAvailability.getLivenessState() → getState(LivenessState.class, BROKEN) → 查缓存(L§3) → 返回当前存活状态。

### 2. 发布时机 — 启动阶段的状态翻转

场景: 状态什么时候从"未就绪"变成"就绪"?和 ApplicationReadyEvent 什么关系?

源码路径:
- `EventPublishingRunListener.java:102,104` — **started**: 启动成功(容器刷新后)→ L104 `AvailabilityChangeEvent.publish(context, LivenessState.CORRECT)` — 进程存活
- `EventPublishingRunListener.java:108,110` — **ready**: L108 → `context.publishEvent(new ApplicationReadyEvent(...))` + L110 `AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC)` — 就绪接流量
- `ApplicationReadyEvent.java:36` — **事件**: extends SpringApplicationEvent, 携带 context+timeTaken — run 的最后阶段(C-9 的 ApplicationRunner 在 ready 前执行完)
- 失败路径: `EventPublishingRunListener.java:114` — **failed**: 发布 ApplicationFailedEvent(L115), 状态不再翻转(保持 REFUSING)

关键设计: **Why 用 AvailabilityChangeEvent 而非直接 set？** 状态变化以事件发布, 任何监听者(actuator 健康检查、探针、监控)都能收到并响应; 与 Spring 事件模型一致。**Why ready 先发 ApplicationReadyEvent 再设 ACCEPTING_TRAFFIC？** 保证"就绪事件已广播"后流量才被接受 — 时序严谨。[模式: 事件驱动状态转换]

数据流: run → ... → started(context)(L102): publish ApplicationStartedEvent + LivenessState.CORRECT → C-9 ApplicationRunner 执行 → ready(context)(L108): publish ApplicationReadyEvent → ReadinessState.ACCEPTING_TRAFFIC(L110)。启动失败 → failed(L114): ApplicationFailedEvent, 无就绪翻转。

### 3. 缓存与查询 — ApplicationAvailabilityBean

场景: 探针调 getLivenessState 时, 状态存在哪?多个状态类型怎么区分?

源码路径:
- `ApplicationAvailabilityBean.java:38,74` — **实现**: `implements ApplicationAvailability, ApplicationListener<AvailabilityChangeEvent<?>>`(L38); onApplicationEvent(L74): 收到状态变更事件 → 按状态类型存进 `Map<Class, AvailabilityChangeEvent>`(ConcurrentHashMap)
- `ApplicationAvailabilityBean.java:54,62` — **查询**: getState(stateType, default)(L54) → getState(stateType)(L62) 从 Map 取 last event 返回其 state(无则返回默认)
- `ApplicationAvailabilityAutoConfiguration.java:33,38` — **装配**: `@AutoConfiguration`(L33) + `@Bean ApplicationAvailabilityBean applicationAvailability()`(L38) + `@ConditionalOnMissingBean(ApplicationAvailability.class)`(L37) — 经自动装配(S-2)注册为单例, 可用 `ApplicationAvailability` 注入(可被用户覆盖)

关键设计: **Why 监听事件缓存而非主动查？** 状态由各阶段事件驱动, Bean 被动监听并缓存每个状态类型的最近一次变更 — 查询时 O(1) 直接取, 且天然线程安全(ConcurrentHashMap); 这也是 actuator 健康检查/K8s 探针的数据来源。[模式: 监听器 + 缓存]

数据流: EventPublishingRunListener.publish(ReadinessState.ACCEPTING_TRAFFIC) → ApplicationAvailabilityBean.onApplicationEvent(L74) → 存 Map[ReadinessState.class]=event → 外部 getState(ReadinessState.class, REFUSING_TRAFFIC)(L62) → 返回 ACCEPTING_TRAFFIC。

→ 引出 S-19 篇②: 虚拟线程开关 — 可用性之外的另一运行时开关: spring.threads.virtual.enabled 的线程模型切换。
