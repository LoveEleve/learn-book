# SCC-8 RefreshEndpoint + 事件 — 刷新的"按钮": HTTP 端点、程序化事件与健康面

> 前置: [[SCC-2-@RefreshScope]] (ContextRefresher 刷新) + [[SCC-1-Bootstrap]] (环境) | 引出: [[SCC-9-配置加密]] (decrypt 重跑面) | 对照: Spring Boot Actuator Endpoint + Apollo 推送
> 🟡 B | 方案 B (标准) | 闭环: q1(双触发面) q2(事件链) q3(健康集成) q4(装配条件)
> Pass 2 闭环: q1(Endpoint+EventListener) q2(EnvironmentChangeEvent 同源) q3(HealthIndicator) q4(AutoConfiguration)

**读者处境**: 配置改了, 除了 /actuator/refresh 还有别的触发方式吗? "刷新"事件怎么传播? 刷新失败怎么暴露? 程序化触发 (RefreshEvent) 和 HTTP 触发 (RefreshEndpoint) 的实现差异?

### 1. RefreshEndpoint — /actuator/refresh 的薄门面

场景: HTTP POST /actuator/refresh 背后是什么?
源码路径:
- RefreshEndpoint (endpoint/RefreshEndpoint.java:34): **@Endpoint(id="refresh")** (L33) + **@WriteOperation refresh()** (L44) — Spring Boot Actuator 端点
- 方法体 (L46-48): `contextRefresher.refresh()` → **返回变更键集合** (Collection\<String\>) — 决策全在 SCC-2 ContextRefresher
- 日志 (L47): "Refreshed keys : " + keys
- 极简设计: **51 行无任何自有决策** — 纯粹委托 SCC-2
关键设计 (q1): **薄门面是"触发面"的正确形态** — 刷新逻辑 (双阶段/双锁) 在 ContextRefresher 已封装; Endpoint 只做"HTTP 动词 + 委托 + 结果返回"; 变更键集合返回给调用方 (配置中心可据此判断哪些配置变了)。 [模式: 薄门面委托]

### 2. RefreshEvent + RefreshEventListener — 程序化触发面

场景: 不用 HTTP, 怎么在代码里触发刷新?
源码路径:
- **RefreshEvent** (endpoint/event/RefreshEvent.java:28-44): ApplicationEvent 子类 + event/eventDesc 双字段 (L40-44) — 携带"什么事件/描述"
- **⚠ 发布者在外部**: 主源码零发布 (grep 实证) — **配置中心适配器发布**: Nacos 的 NacosConfigRefreshEventListener (spring-cloud-alibaba 仓库, 监听配置变更 → 发 RefreshEvent → 本监听器 → 刷新); Spring Cloud Bus 广播场景也经此
- **RefreshEventListener** (endpoint/event/RefreshEventListener.java:37-72): **SmartApplicationListener** — supportsEventType 监听 **ApplicationReadyEvent + RefreshEvent** (L50-53)
- **ready 原子标志** (L41/L66/L70): ApplicationReadyEvent → `ready.compareAndSet(false, true)` (L66); handle(RefreshEvent) → `if (this.ready.get())` (L70) — **"应用未就绪前的 RefreshEvent 被忽略"** (注释 "don't handle events before app is ready")
- handle(RefreshEvent) (L68-72): contextRefresher.refresh() (L72) → 日志变更键
关键设计 (q1): **双触发面 = HTTP + 事件** — RefreshEndpoint (外部 HTTP) + RefreshEventListener (内部事件); ready 标志防"启动早期刷新" (bootstrap 阶段发布 RefreshEvent 会被忽略); SmartApplicationListener 支持事件类型过滤。 [模式: 双触发 + 就绪守卫]

### 3. 事件链 — EnvironmentChangeEvent 的"同源不同发布者"

场景: 刷新时谁发了什么事件?
源码路径:
- **EnvironmentChangeEvent** (context/environment/EnvironmentChangeEvent.java:31-42): Set\<String\> keys 携带 — 变更键集合
- **两个发布者**: SCC-2 ContextRefresher.refreshEnvironment (SCC-2 L103-105) / **EnvironmentManager.reset/setProperty** (environment/EnvironmentManager.java:73/92) — 同源事件不同触发路径
- **EnvironmentManager** (L43-102): @ManagedResource (JMX) + **"manager" 属性源** (L45) + reset/setProperty/getProperty (@ManagedOperation L68-98) — 运行时管理属性
- **setProperty 语义** (L79-95): manager 源**懒创建双检** (L81-87) + **addFirst 最高优先级** (L86) + **值变化才发布** (`!value.equals` L91-94, 幂等写入) + EnvironmentChangeEvent
- WritableEnvironmentEndpoint (L31-36): EnvironmentEndpoint 的可写扩展 (继承无新增 — 写能力来自 EnvironmentEndpoint 家族)
关键设计 (q2): **EnvironmentChangeEvent 是"环境变更"的统一信号** — 不管从刷新 (ContextRefresher) 还是运行时管理 (EnvironmentManager) 变更, 都发同一事件; 消费方 (ConfigurationPropertiesRebinder 等) 只需监听一种事件。 [模式: 统一变更信号]

### 4. RefreshScopeHealthIndicator — 刷新错误的健康面

场景: 刷新失败 (Bean 创建异常) 怎么被运维发现?
源码路径:
- RefreshScopeHealthIndicator (health/RefreshScopeHealthIndicator.java:35): **extends AbstractHealthIndicator** — Spring Boot 健康指示器
- doHealthCheck (L48-61): **聚合 RefreshScope.getErrors() + rebinder.getErrors()** (L50-51) → 空 → **up** (L53) / 非空 → **down + withException/withDetail** (L56-60)
- 装配 (RefreshEndpointAutoConfiguration.java:60-64): @ConditionalOnEnabledHealthIndicator("refresh") (L61)
- 依赖: RefreshScope errors (SCC-2 GenericScope.get 的 errors.put L179-181) + ConfigurationPropertiesRebinder errors
关键设计 (q3): **刷新错误 = 健康状态** — Bean 创建异常 (GenericScope.errors) 和重绑定异常 (Rebinder.errors) 汇总到 /actuator/health/refresh; 运维无需看日志, 健康检查直接 down。 [模式: 错误聚合健康面]

### 5. 装配面 — RefreshEndpointAutoConfiguration 的条件链

场景: 什么条件下这些 Bean 才被装配?
源码路径:
- RefreshEndpointAutoConfiguration (autoconfigure/RefreshEndpointAutoConfiguration.java:57-80): RefreshScopeHealthIndicator (**@ConditionalOnEnabledHealthIndicator("refresh")** L61-62) + RefreshEndpoint (**四条件**: @ConditionalOnBean(ContextRefresher) L71 + @ConditionalOnAvailableEndpoint L72 + @ConditionalOnMissingBean L73)
- **RefreshEventListener 装配在 RefreshAutoConfiguration** (L125-126, 无条件 @Bean) — 与 RefreshEndpoint (RefreshEndpointAutoConfiguration) 不同装配类
- RefreshAutoConfiguration (autoconfigure/RefreshAutoConfiguration.java:69-70): RefreshScope (**@ConditionalOnClass + REFRESH_SCOPE_ENABLED matchIfMissing=true**) + **双刷新器条件装配: @ConditionalOnBootstrapEnabled → LegacyContextRefresher (L104-106) / @ConditionalOnBootstrapDisabled → ConfigDataContextRefresher (L112-114)** — 新旧选择由 bootstrap 开关决定 (跨域: SCC-2 已详)
- 条件链: 类存在 → 属性开启 → 依赖 Bean → 端点可用 — 完整装配矩阵 (Endpoint 四条件)
关键设计 (q4): **条件链保证"不用不装"** — 无 ContextRefresher 不装 Endpoint, 无 RefreshScope 不装健康指示器; 每个条件都是"依赖的类/配置存在才装配"。 [模式: 条件装配]

## 代码类型
Architecture (触发面) + Operations (健康/运维面)

## 负面空间 — 刷新触发面刻意不做的事

- **不做配置中心客户端内建**: 无订阅/轮询逻辑 — 配置中心适配器 (Nacos 的 NacosConfigRefreshEventListener 等) 在外部仓库监听变更后发 RefreshEvent, 本仓库只提供监听器 (对比 Apollo 推送式)
- **不做刷新调度**: 无定时刷新/自动刷新 — 全手动触发
- **不做刷新结果持久化**: 变更键只返回/日志, 不存状态
- **不做分布式协调**: 多实例各自 refresh, 无广播 (对比 Spring Cloud Bus 事件广播)
- **不做刷新鉴权内建**: 端点安全靠 Actuator 安全配置
- **不处理刷新并发**: 多触发同时 refresh 靠 ContextRefresher synchronized (SCC-2), 无额外控制

→ 引出: 配置加密怎么融入这条刷新链? → SCC-9 配置加密
