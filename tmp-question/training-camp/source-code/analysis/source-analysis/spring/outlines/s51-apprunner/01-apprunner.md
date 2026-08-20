# C-9 ApplicationRunner — 启动回调 (run 流程 → callRunners)

> 依赖 C-8 SpEL | 🟡 Working | 6 KP | [模式: 模板方法 + 标记接口分派]

**读者处境**: 启动后要预热缓存、检查配置、打印版本号 — 用什么回调？ApplicationRunner 和 CommandLineRunner 区别？它在启动流程哪个位置？和 @PostConstruct 谁先？

### 1. Runner 双接口 + SpringApplication.run 触发点

场景: `SpringApplication.run(MyApp.class, args)` 里 --server.port=8080 和 --name=x — 容器完整启动后, Runner bean 的 run() 拿到这些参数执行业务初始化。

源码路径:
- `ApplicationRunner.java:33,40` — **接口**: extends Runner — run(ApplicationArguments args) — 结构化参数(可查选项)
- `CommandLineRunner.java:36,43` — **接口**: run(String... args) — 原始字符串数组 — 老 API
- `Runner.java:26` — **标记接口**: 空接口 — 两个 Runner 统一收集, 分派靠 instanceof
- `SpringApplication.java:301` — **run() 流程**: L310 listeners.starting(事件广播) → L317 prepareContext(创建/准备容器) → L318 refreshContext(容器完整启动: 所有 bean 就绪) → **L325 callRunners(context, args)** — Runner 在 refresh 之后、run 返回之前

关键设计: **Why Runner 在 refreshContext 之后执行？** 启动回调需要"容器完整可用"(注入任意 bean) — refresh 前只有容器骨架; 且与 @PostConstruct(单 bean 就绪时)不同, Runner 是"全局就绪后"的业务钩子。[模式: 模板方法 — 流程末尾扩展点]

数据流: main → SpringApplication.run → listeners.starting → prepareContext(env 就绪) → refreshContext(容器启动完成) → callRunners → 容器找 Runner 类型 bean → 执行 run() → run 返回。

### 2. callRunners — 收集、排序、分派

场景: 多个 Runner bean — 谁先谁后？@Order 生效吗？两个接口怎么统一执行？

源码路径:
- `SpringApplication.java:763` — **callRunners()**: L766 getBeanNamesForType(Runner.class) → L767-769 IdentityHashMap(bean→name) → L771 getOrderComparator: 容器 dependencyComparator 或 AnnotationAwareOrderComparator.INSTANCE(带 FactoryAwareOrderSourceProvider — 工厂 bean 也能排) → L772 sorted.stream().forEach(callRunner)
- `SpringApplication.java:782,793` — **callRunner()**: L783 instanceof ApplicationRunner → callRunner(type, runner, r -> r.run(args)) / L786 instanceof CommandLineRunner → run(args.getSourceArgs()); L793-796 异常包装 IllegalStateException("Failed to execute ...")
- `DefaultApplicationArguments.java:47,63` — **参数对象**: getSourceArgs(原始)/getOptionValues("server.port")(--key=value)/getNonOptionArgs(裸参数)

关键设计: **Why 用 getBeanNamesForType + IdentityHashMap 排序而非直接遍历？** ①Runner 可能是 FactoryBean 产物(FactoryAwareOrderSourceProvider 取工厂 order) ②IdentityHashMap 避免多次 getBean 的重复实例化 ③排序器复用容器 dependencyComparator — 与 C-4 的 OrderComparator 体系完全一致。[模式: 收集-排序-执行]

数据流: 两个 Runner bean → callRunners: beanNames=[r1,r2] → 排序: @Order(-1) 的 r2 在前 → stream: r2.callRunner: instanceof ApplicationRunner → run(ApplicationArguments) → r1.run()。

### 3. 启动回调全家对照 — 什么时候用哪个

场景: "启动时做 X" — 有 @PostConstruct、InitializingBean、SmartInitializingSingleton、ApplicationReadyEvent、ApplicationRunner — 差异在"时机"和"能访问什么"。

源码路径:
- `SmartInitializingSingleton.java:44`(spring-beans) — **容器级回调**: 所有单例实例化完成后 afterSingletonsInstantiated — 早于 Runner
- 顺序表: ①`@PostConstruct`/`InitializingBean.afterPropertiesSet`(该 bean 属性填充后, 早于其他 bean) → ②`SmartInitializingSingleton.afterSingletonsInstantiated`(所有单例创建完) → ③`ApplicationRunner/CommandLineRunner.run`(L325, Boot 主流程倒数第二步) → ④`ApplicationReadyEvent`(@EventListener — L332 listeners.ready 发布, callRunners **之后**)
- 使用建议: 单 bean 初始化→@PostConstruct; 跨 bean 依赖→SmartInitializingSingleton; 全局启动任务→ApplicationRunner(@Order 控序); 需要异步/多次→@EventListener(ApplicationReadyEvent)

关键设计: **Why 提供这么多回调？** 时机粒度不同: @PostConstruct 时其他 bean 可能未就绪(依赖注入会触发创建, 有循环风险); ApplicationRunner 时一切就绪且可拿命令行参数 — "越晚的回调越安全, 但越晚"。[模式: 生命周期分级]

数据流: refresh: bean 逐个实例化→属性填充→@PostConstruct(该 bean) → 所有单例完成→SmartInitializingSingleton.afterSingletonsInstantiated → refresh 返回 → L325 callRunners(Runner.run, 拿命令行参数) → L332 listeners.ready(发布 ApplicationReadyEvent) → run 返回。预热缓存放 Runner 里: 容器与参数都就绪且早于 ready 事件, @Order 可保证先于其他任务。

→ 引出 2-E: ClassPathIndex — 启动还能更快吗？组件扫描的类路径全扫描 → META-INF/spring.components 索引直接定位 — Boot 启动性能的最后一块拼图。
