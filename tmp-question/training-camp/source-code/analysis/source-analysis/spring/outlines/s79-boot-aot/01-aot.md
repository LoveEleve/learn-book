# S-15 AOT/Native Image — SpringApplicationAotProcessor (Boot AOT 入口)

> 依赖 s21 (复用) + S-4 | 🟡 Working | 6 KP | [模式: 钩子拦截 + 管线复用]

**读者处境**: Spring Boot 3 的 AOT 是怎么触发的?运行 main 却不启动服务?AOT 机制本身在 s21 已讲, 本域讲 Boot 侧入口。

### 1. Boot AOT 入口 — 运行 main 启动应用

场景: AOT 需要"完整启动应用"来收集反射/资源 hints — Boot 的 SpringApplicationAotProcessor 运行 main 方法, 拿容器交 AOT 管线。

源码路径:
- `SpringApplicationAotProcessor.java:43` — **入口**: extends ContextAotProcessor(spring-framework/context/aot — s21 已分析管线)
- `SpringApplicationAotProcessor.java:60` — **触发**: `new AotProcessorHook(application).run(...)` — 挂钩子运行 main

关键设计: **Why 运行 main？** AOT 编译期需要"完整应用上下文"来枚举 Bean/反射需求 — 与其模拟, 不如真启动(所有自动装配/配置生效); 但启动后不能真正服务 — 用钩子拦截(§2)。**Why 复用 ContextAotProcessor？** AOT 管线(收集→生成)在 s21 已深度分析, Boot 只提供"如何启动并拿到容器" — 06 §2.5 复用≠省略。[模式: 管线复用]

数据流: 构建插件(maven/gradle)编译期调 SpringApplicationAotProcessor → new AotProcessorHook(application) → 运行 main(§2) → 拿 GenericApplicationContext → 父类 ContextAotProcessor 管线(s21): 收集 RuntimeHints → 生成 AOT 产物。

### 2. AotProcessorHook — 运行 main 并拦截容器

场景: main 方法正常运行会启动整个应用 — AOT 只要容器不要服务, 怎么拦截?

源码路径:
- `SpringApplicationAotProcessor.java:103,123` — **钩子**: AotProcessorHook L103 — run L123: `SpringApplication.withHook(this, action)`(带钩子运行 main) → main 里 SpringApplication.run 创建容器 → 钩子拦截 → 抛 AbandonedRunException(携带已创建的容器) → catch: 校验 GenericApplicationContext → 返回容器
- L135: 无容器则报错("Does it run a SpringApplication?")

关键设计: **Why withHook + AbandonedRunException？** main 方法是用户代码(无法改)— 钩子机制让 SpringApplication 在"容器创建完成但服务未启动"时主动放弃(Abandoned), 通过异常把容器传出来; 这是"运行用户代码但拦截其副作用"的优雅方案。[模式: 钩子 + 异常传值]

数据流: withHook(this, () -> MyApplication.main(args)) → main 执行 → SpringApplication.run → 容器创建 → 钩子触发: 抛 AbandonedRunException(context) → run 方法 catch → context 是 GenericApplicationContext → 返回 → 交 §3 管线。main 不是 SpringApplication → 无拦截 → IllegalStateException。

### 3. 管线复用与触发

场景: 拿到容器后谁做 hints 收集?AOT 什么时候跑?

源码路径:
- `ContextAotProcessor`(spring-framework/context/aot) — **管线**: GenericApplicationContext → 遍历 Bean 的 AotContributor → 收集 RuntimeHints(reflection()/resources()/serialization()/proxies(), spring-core/aot/hint) → 生成 AOT 编译产物 — **s21 机制全部复用**
- 触发: `spring-boot-maven-plugin`(maven) / `spring-aot-maven-plugin` 的 **process-aot** goal / Gradle 任务 — 编译期执行
- 消费: Native Image(GraalVM)编译时用 AOT 产物 — 免运行时反射扫描

关键设计: **Why 编译期做？** Native Image 无法运行时动态反射 — AOT 在编译期"预演"启动, 把反射/资源需求固化为 hints, 原生镜像直接用; 这也是 Boot 3 Native 的基石。**Why 与 s21 边界？** 机制(s21)引用, 入口/触发(本域)展开。[模式: 构建期处理 + 边界声明]

数据流: ./mvnw package -Pnative → process-aot goal → SpringApplicationAotProcessor(§1) → 容器 → ContextAotProcessor: 收集 hints → 生成 AOT 类 → GraalVM 编译 native 镜像(含 hints) → 启动 native 应用(免反射扫描)。

→ 引出 S-16: 诊断 — AOT 之外的运行时: FailureAnalyzer — 启动失败的友好诊断(进入启动与运行时层)。
