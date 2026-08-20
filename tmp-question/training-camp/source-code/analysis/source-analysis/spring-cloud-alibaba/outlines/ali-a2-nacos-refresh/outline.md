# ALI-A2 Nacos 配置动态刷新 — 配置变更后, "热刷新"的接力棒如何传递

> 前置: [[ALI-A1-配置加载]] (NacosConfigRefreshEvent 由 A1 的监听者发布) | 引出: [[SCC-8-RefreshEndpoint]] (RefreshEvent 消费方) + [[SCC-2-RefreshScope]] (rebind 面) | 对照: Commons SCC-2 双刷新器 / SCC-8 RefreshEventListener
> 🔴 A | 方案 A (全深度) | 闭环: q1(新旧双轨) q2(事件链) q3(Smart rebind) q4(仲裁)

**读者处境**: Nacos 控制台改了配置, 应用里的 @Value 和 @ConfigurationProperties 怎么自动变? 为什么有的配置改了就生效有的要重启? "刷新所有 Bean"和"只刷受影响的 Bean"谁说了算? 老项目和新项目刷新链路有什么不同?

### 1. 新旧双轨 — NacosContextRefresher 旧轨 vs RefreshEventListener 新轨

场景: 配置变更从 Nacos 长轮询到 Spring 刷新, 有两条路?
源码路径:
- **旧轨监听者**: NacosContextRefresher (refresh/NacosContextRefresher.java:49) — 监听 **ApplicationReadyEvent** (L86) + ready CAS 防多 context (L88) + **listenerMap.computeIfAbsent 防重复** (L116) → configService.addListener (L144, Nacos 长轮询监听)
- **旧轨回调内聚五连** (L117-138): ① refreshCountIncrement (静态 AtomicLong L55/81-83 — **A1 刷新节流的闸门**) ② addRefreshRecord (历史 L125) ③ **putConfigSnapshot** (快照 L126 — A1 容灾的写入方) ④ 发布 NacosConfigRefreshEvent (L128-132) ⑤ 不直接换源
- **新轨监听者**: NacosConfigRefreshEventListener (starter/configdata/NacosConfigRefreshEventListener.java:34) — SmartApplicationListener 只认 NacosConfigRefreshEvent (L41-43) + **转发 RefreshEvent** (L52, Commons SCC-8 的 RefreshEvent!)
- **@since 锚**: NacosConfigRefreshEventListener `@since 2024.10.17` (L32) — 新轨 2024 年引入
关键设计 (q1): **"旧轨 = 换源, 新轨 = 全链路"** — 旧轨只负责把新配置放进 Repository/快照, 刷新动作由新轨经 RefreshEvent → ContextRefresher.refresh() → EnvironmentChangeEvent → Rebind 完整链路驱动; 两条轨最终汇合在 @ConfigurationProperties 重新绑定。 [模式: 新旧双轨]

### 2. 旧轨落点 — NacosPropertySourceRefreshListener 的源替换与仲裁

场景: 旧轨的 NacosConfigRefreshEvent 谁消费? 为什么要有 containsBean 判断?
源码路径:
- **双角色**: NacosPropertySourceRefreshListener (refresh/NacosPropertySourceRefreshListener.java:43) — BeanPostProcessor (收集 ConfigurationPropertiesBean, L60-69) + SmartApplicationListener (L71-74: 认 ApplicationReadyEvent + NacosConfigRefreshEvent)
- **ready 门控** (L96-97): 就绪前忽略一切刷新事件 — 与 SCC-8 RefreshEventListener 的 ready 守卫同构
- **新旧仲裁** (L98): **`!containsBean("nacosConfigSpringCloudRefreshEventListener")` 才执行** — 装配了新轨 listener (starter 模块) 就不动 Environment, 没装配才走旧轨
- **旧轨换源** (L100-112): builder.build 重建 + **target.replace(sourceName, newProperSource)** (L109) — PropertySource 原地替换, 不发布 EnvironmentChangeEvent
- sourceName = dataId + "," + group (L103) — 与 Repository 复合 key 同构
关键设计 (q2): **"仲裁条件 = 装配探测"** — 用 containsBean 探测新轨是否在, 在则让贤; 旧轨是"没有新轨的退路", 保老项目行为不变。 [模式: 装配探测仲裁]

### 3. 新轨链路 — RefreshEvent → ContextRefresher → rebind (对接 Commons)

场景: 新轨的 RefreshEvent 发布后, 后续谁接手?
源码路径:
- NacosConfigRefreshEventListener.onApplicationEvent (L51-56): `publishEvent(new RefreshEvent(event.getSource(), null, "Refresh Nacos config"))`
- **消费方是 Commons SCC-8 的 RefreshEventListener** (spring-cloud-context 仓库): RefreshEvent → ContextRefresher.refresh() (SCC-2 双阶段: refreshEnvironment + refreshAll)
- **EnvironmentChangeEvent 发布** → ConfigurationPropertiesRebinder.onApplicationEvent → rebind() — 默认全量 rebind
- **Smart 变体拦截**: 装配条件 @ConditionalOnNonDefaultBehavior (NacosConfigSpringCloudAutoConfiguration.java:42-47) + **@ConditionalOnMissingBean(search=CURRENT)** (L41) — 有自定义 rebinder 则让贤
- 注释明言 (L44-46): "If using default behavior, not use SmartConfigurationPropertiesRebinder. Minimize the possibility of making mistakes." — **默认行为不用 Smart**
关键设计 (q3): **"Nacos 只发事件, 刷新是 Commons 的事"** — 集成层职责止于"通知", 刷新语义 (全量/局部/scope 重建) 全部复用 Commons 机制; Smart 是可选增强, 默认求稳。 [模式: 事件驱动 + 可选增强]

### 4. SmartConfigurationPropertiesRebinder — 反射读 BeanMap 与 SPECIFIC_BEAN

场景: "只刷新受影响的 Bean"怎么知道哪些 Bean 受影响?
源码路径:
- **反射读私有字段** (SmartConfigurationPropertiesRebinder.java:66-75): ConfigurationPropertiesBeans 的 `beans` 字段 (L68) + setAccessible + Optional 兜底 — **Commons 未暴露 API, 反射补齐**
- **onApplicationEvent 双源判断** (L88-97): `context.equals(source)` (事件源是 context) **||** `event.getKeys().equals(source)` (向后兼容) → 才处理
- **RefreshBehavior 两态** (refresh/RefreshBehavior.java:27-36): ALL_BEANS (默认) / SPECIFIC_BEAN — 配置 `spring.cloud.nacos.config.refresh-behavior` (L82-84)
- **rebindSpecificBean 前缀匹配** (L99-108): `changeKey.startsWith(prefix)` (bean 的 @ConfigurationProperties 前缀, AnnotationUtils 取值 L102) + **refreshedSet 防同一 Bean 重复 rebind** (L104)
关键设计 (q4): **"前缀即归属"** — 变更 key 以哪个前缀开头就 rebind 哪个 Bean; 一个 key 可能匹配多个 Bean (refreshedSet 只防同 Bean 重入); 反射是因为 Spring 未开放此 API — "框架不给就反射拿"。 [模式: 前缀匹配 + 反射兼容]

### 5. @NacosConfig 注解族 — NacosAnnotationProcessor 的注入与监听

场景: 不用 @Value, 注解直接绑 Nacos 配置?
源码路径:
- **NacosAnnotationProcessor** (annotation/NacosAnnotationProcessor.java:59): BeanPostProcessor + **PriorityOrdered.getOrder()=0** (L69) — 初始化后处理
- **三级扫描** (L117-133): 类级 @NacosConfig → 字段级 → 方法级
- **getGroupKeyContent 双检缓存** (L76-108): groupKeyCache + synchronized 双检 (L80-83) + getConfig(5000) + **refreshed=false → 不注册监听** (L84-88) + addListener 更新缓存 (L91-101)
- **refreshed 语义** (L170-173): false → 注入一次即止 "do not register listener"
- **两监听器**: 有 key → NacosPropertiesKeyListener (L186, 只刷新指定 key); 无 key → NacosConfigRefreshableListener (L214, 整配置转对象 + copyProperties)
- 方法级: @NacosConfigListener 单参数强校验 (L317: "must be over a method with a single parameter") / @NacosConfigKeysListener (L245, interestedKeys/KeyPrefixes)
关键设计 (q5): **"注解 = 配置即对象"** — 字段/类注入是拉取一次 + 监听变更重新注入; key 级监听粒度最小 (单一配置项), 不触发全局刷新 — 与 Smart rebind 的"粒度控制"哲学一致。 [模式: 注解注入 + 粒度监听]

### 6. NacosRefreshHistory — 20 条刷新历史的 MD5 留痕

场景: 刷新过什么, 怎么查?
源码路径:
- **NacosRefreshHistory** (refresh/NacosRefreshHistory.java:34): LinkedList + **MAX_SIZE=20** (L38) + addFirst/removeLast (L71-77)
- **MD5 摘要** (L83-97): 内容摘要而非明文 — 端点展示用
- ThreadLocal SimpleDateFormat (L42-43) — 线程安全格式化
- 消费方: NacosConfigEndpoint (endpoint 面, A1 装配 NacosConfigEndpointAutoConfiguration)
关键设计 (q6): **"历史 = 审计留痕"** — MD5 不存明文 (安全) + 20 条环形 (内存有界) + 端点可查 — 刷新的可观测性。 [模式: 有界审计]

### 7. 测试与行为锚 — 新旧轨互斥怎么保证?

场景: 双轨同时启用会不会双份刷新?
源码路径:
- 仲裁链: NacosPropertySourceRefreshListener.containsBean (L98) + Smart rebinder @ConditionalOnMissingBean (L41) + NacosConfigSpringCloudAutoConfiguration @ConditionalOnNonDefaultBehavior
- 测试: SmartConfigurationPropertiesRebinderIntegrationTest (test 目录) — 验证 specific 刷新
- 注释锚: "Minimize the possibility of making mistakes" (L45) — 默认不用 Smart 的官方理由
- @since 锚: SmartConfigurationPropertiesRebinder `@since 2021.0.1.1` (L49) / RefreshBehavior `@since 2021.0.1.1` (L25) — 双文件同版本引入
关键设计 (q1): **"互斥靠条件注解 + Bean 探测双保险"** — 装配期条件注解防共存, 运行期 containsBean 再兜底; 双保险保"旧行为不变"。 [模式: 互斥双保险]
