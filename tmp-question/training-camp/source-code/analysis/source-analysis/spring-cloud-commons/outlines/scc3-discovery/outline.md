# SCC-3 服务发现抽象 — 一个接口, 无数注册中心: DiscoveryClient 的组合与探活

> 前置: [[SCC-13-NamedContextFactory]] (消费面) + Spring Ordered | 引出: [[SCC-4-服务注册]] (autoRegister 联动) + [[SCC-6-LoadBalancer]] (实例消费) | 对照: Eureka/Nacos/Consul 客户端
> 🔴 A | 方案 A (全深度) | 闭环: q1(接口面) q2(Composite 组合) q3(ImportSelector) q4(健康/心跳)
> Pass 2 闭环: q1(接口四方法) q2(短路+合并) q3(autoRegister 分支) q4(HeartbeatMonitor)

**读者处境**: 应用怎么知道服务 X 有哪些实例? 为什么换了注册中心 (Eureka→Nacos) 业务代码不用改? "组合客户端"按什么顺序查? 注册中心挂了怎么暴露到健康检查?

### 1. DiscoveryClient 接口 — 服务发现的"最小契约"

场景: 所有注册中心的统一接口长什么样?
源码路径:
- DiscoveryClient (client/discovery/DiscoveryClient.java:32): `extends Ordered` + **DEFAULT_ORDER = 0** (L37)
- **四方法面**: description() (L43) / getInstances(serviceId) (L50) / getServices() (L55) / **probe() default** (L66-68, 默认调 getServices, 实现可覆写轻量操作)
- 实现族 (外部): EurekaDiscoveryClient (Eureka 仓库) / NacosDiscoveryClient (Nacos) / ConsulDiscoveryClient (Consul) — **所有注册中心的插槽**
关键设计 (q1): **description 是健康检查的展示面** (Javadoc 明示 "used in HealthIndicator"); probe 是"探活"契约 (默认 getServices, 实现可轻量); DEFAULT_ORDER=0 让实现类排序有基准。 [模式: 最小契约 + 探活]

### 2. CompositeDiscoveryClient — 短路优先 + 合并去重

场景: 多注册中心并存时, 按什么顺序查?
源码路径:
- CompositeDiscoveryClient (composite/CompositeDiscoveryClient.java:36): implements DiscoveryClient
- **构造时 AnnotationAwareOrderComparator.sort** (L41) — 按 @Order/Ordered 排序
- **getInstances 短路** (L51-56): 第一个非空非空集合即返回 — **顺序决定优先级**!
- getServices 合并 (L62-70): LinkedHashSet 去重合并全部 (L65)
- **probe 覆写** (L50-56): Composite **遍历所有子客户端 probe** — 不是用接口 default (全部探活)
- **sort 原地修改** (L41): AnnotationAwareOrderComparator.sort 直接改传入 List — **不可变列表会抛异常** (harness H1 实证); getDiscoveryClients 访问器 (L58-60)
- description: "Composite Discovery Client" (L42)
关键设计 (q2): **getInstances 短路 = "第一个有实例的就赢"** — 多注册中心按 order 主备; getServices 全合并 (去重) 因为要枚举所有服务; 排序在构造时一次完成 (Comparator.sort)。 [模式: 短路优先 + 合并]

### 3. SimpleDiscoveryClient — 属性驱动的"零注册中心"

场景: 没有注册中心 (本地测试/最小部署) 怎么办?
源码路径:
- SimpleDiscoveryClient (simple/SimpleDiscoveryClient.java:34): 属性驱动
- getInstances: **SimpleDiscoveryProperties.getInstances().get(serviceId)** (L48-51)
- getServices: 全部 key (L59-60)
- **getOrder: 用配置的 order** (L63-65, simpleDiscoveryProperties.getOrder()) — 可调优先级
- 装配: SimpleDiscoveryClientAutoConfiguration
关键设计 (q1): **Simple 是"配置即注册中心"** — application.yml 里写死实例列表, 无网络依赖; 用于本地开发/测试; order 可配让它在组合里排后 (真实注册中心优先)。 [模式: 属性驱动兜底]

### 4. @EnableDiscoveryClient — ImportSelector 的 autoRegister 双分支

场景: 注解怎么触发发现/注册的装配?
源码路径:
- EnableDiscoveryClient 注解 (EnableDiscoveryClient.java:38): @Import(EnableDiscoveryClientImportSelector) + **autoRegister() default true** (L44)
- **EnableDiscoveryClientImportSelector extends SpringFactoryImportSelector** (EnableDiscoveryClientImportSelector.java:37) — **SpringFactories 机制加载实现类**
- selectImports (L40-66): ① super.selectImports (工厂加载) ② **autoRegister=true → 追加 AutoServiceRegistrationConfiguration import** (L49-53, 服务注册配置!) ③ **autoRegister=false → 注入 spring.cloud.service-registry.auto-registration.enabled=false 属性源** (L56-61, 禁用注册) — **属性被 AutoServiceRegistrationAutoConfiguration (L30) 和 AutoServiceRegistrationConfiguration (L28) 的 @ConditionalOnProperty 消费** — 注册装配双关
- **isEnabled** (L68-69): `spring.cloud.discovery.enabled` 默认 true
关键设计 (q3): **autoRegister 决定"注册面"是否装配** — true 追加注册配置 (SCC-4 联动), false 注入禁用属性; ImportSelector 是"注解 → 条件装配"的桥梁; SpringFactoriesLoader 让实现类可插拔。 [模式: ImportSelector 双分支]

### 5. ReactiveDiscoveryClient — 响应式变体

场景: 响应式应用的服务发现?
源码路径:
- ReactiveDiscoveryClient (ReactiveDiscoveryClient.java): getInstances **Flux** (L54) / getServices **Flux** (L59) / **probe @Deprecated(forRemoval=true)** (L75, LOG.warn 提示) — **reactiveProbe() default 取代** (L94-95, `getServices().then()` Mono\<Void\> 响应式探活)
- 方法面: getInstances/getServices 返回 Flux
- ReactiveCompositeDiscoveryClient (composite/reactive/) + SimpleReactiveDiscoveryClient (simple/reactive/) — 响应式组合/简单
关键设计 (q1): **响应式面是"同构变体"** — 阻塞版 List → Flux; probe 演进 (probe → reactiveProbe, forRemoval 标记); Composite/Simple 各有响应式版。 [模式: 响应式变体]

### 6. 健康与心跳 — DiscoveryClientHealthIndicator + HeartbeatMonitor

场景: 注册中心故障怎么暴露?
源码路径:
- **DiscoveryClientHealthIndicator** (health/DiscoveryClientHealthIndicator.java:37-99): order = **HIGHEST_PRECEDENCE** (L48) + **discoveryInitialized 门控** (L55-59: InstanceRegisteredEvent 才标记, compareAndSet) + **双模式** (L69-80): **useServicesQuery=true → getServices() 列服务 UP** (L74-75) / false → **probe() 探活 UP** (L78-79) + **catch Exception → down(e)** (L83-84)
- DiscoveryCompositeHealthContributor (health/): 多客户端健康聚合
- **HeartbeatEvent** (event/HeartbeatEvent.java): source + **state 值语义** — "只要求变更时变化, 可版本计数" (L44-46)
- **HeartbeatMonitor** (event/HeartbeatMonitor.java): **AtomicReference latestHeartbeat + update() 比较变更** (L33-35) — 心跳状态变更检测
- 事件面: InstanceRegisteredEvent/InstancePreRegisteredEvent/ParentHeartbeatEvent
关键设计 (q4): **心跳是"状态变更信号"而非数据** — state 可以是版本计数 (Javadoc 明示不依赖内容); HeartbeatMonitor 用 AtomicReference 检测变更; 健康指示器把"能列出服务"当作 UP 依据。 [模式: 状态变更信号 + 健康聚合]

## 代码类型
Architecture (发现抽象) + SPI (注册中心插槽)

## 负面空间 — 服务发现抽象刻意不做的事

- **不做注册中心客户端内建**: 只定义接口, Eureka/Nacos 各自实现 (SPI)
- **不做实例缓存**: getInstances 每次实时查, 无本地缓存 (对比 Nacos 快照)
- **不做负载均衡**: 返回实例列表, 选择策略是 SCC-6 LoadBalancer 的职责
- **不做故障转移**: Composite 短路是"顺序优先"非"故障转移" (第一个有实例就返回, 不管健康)
- **不做推送订阅**: 无 watch/订阅机制 (对比 Nacos 长轮询); 心跳是状态信号非数据推送
- **不做实例排序**: 返回列表顺序由实现决定, 抽象层不排序

→ 引出: 服务怎么被注册上去? → SCC-4 服务注册抽象
