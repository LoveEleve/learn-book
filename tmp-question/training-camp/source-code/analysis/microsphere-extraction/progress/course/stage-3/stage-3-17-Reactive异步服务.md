# stage-3 · 第 17 节：第十二节："高并发" Reactive 异步服务 — 知识点提取

> 课程：stage-3 三高架构 第 17 节（加餐/事件组收官 13-17）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/17. 第十二节："高并发"Reactive 异步服务.md`
> 提取时间：2026-08-12 | 权重：核心（Reactive 本质/Reactive Streams 规范/WebFlux 编程模型/RSocket）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）

---

## 一、本节概览

- **技术域**：Reactive 编程（本质/定义多源/演进）、Reactive Streams 规范（四接口/背压）、Reactor（Mono/Flux/Scheduler）、WebFlux（编程模型/并发模型/性能真相）、RSocket
- **维度**：`[性能优化]`（Reactive 本质与演进）+ `[规范]`（Reactive Streams/Reactor）+ `[工程问题]`（WebFlux 模型）+ `[分布式问题]`（RSocket）
- **核心命题**：**Reactive 异步服务的完整认知**——docs 主要内容两条：①WebFlux 重构用户服务（客户端 Reactive）②RSocket 服务端 Reactive 化 + 背压重构订单（docs 的 RSocket 为**空节标题**）；docs 正文是 Reactive 深度教程（真相：不更快，少线程可预测扩展）
- **知识点数**：8 个
- **前置**：13 篇（WebFlux 架构——本篇深化）、09 篇（异步/非阻塞）、02 篇（性能方法论）

## 前置条件清单
读者需先掌握：
1. **WebFlux 架构**（13 篇 KP-03：DispatcherHandler/WebHandler）
2. **Servlet 异步/非阻塞**（09 篇 KP-04/05/08）
3. **事件驱动**（14 篇）
未达前置者，先补：13 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **真相优先**：docs 大量引用官方/JHipster 报告——Reactive"不更快"，认知纠偏是本篇核心
- **实例对照**：my-xhs 混合架构（gateway WebFlux / user+order WebMVC）；docs 的"用户服务 WebFlux 重构"在 my-xhs 未做
- **docs 场景 vs 现状**：RSocket（docs ②）my-xhs 未用

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Reactive 本质与性能真相（不更快/少线程/可预测扩展）【docs 认知纠偏】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：02 篇（性能方法论）
- **来源**：docs §理解 Reactive（讲法）+ §Reactor 观点 + §使用场景（Spring 官方 + JHipster 报告）
- **需求**：**纠偏 Reactive 三大误解**——docs 开篇列 3 讲法（异步非阻塞/提升性能/解决困境）+ 官方真相（不更快）
- **自主实现**：若我设计——先理解"Reactive 收益面"再选型：**省线程/可预测扩展 ≠ 更快**
- **参考实现**（docs 多源引用）：**3 讲法（docs）**——Reactive 是异步非阻塞编程/能够提升程序性能/解决传统编程模型困境；**Reactor 观点（docs）**——"Blocking Can Be Wasteful"：两途径（parallelize 加线程 vs 更高效用资源）+ **"并行不是银弹"**（加线程引竞争/并发问题）；**Spring 官方真相（docs 引用）**——"**Reactive and non-blocking generally do not make applications run faster**"——收益是"**以少量固定线程和更少内存扩展（scale with a small, fixed number of threads and less memory）**，负载下更可预测地扩展；**JHipster 报告（docs 引用，照录）**——MySQL/Mongo 应用：**"无速度提升（Gatling 结果甚至稍差）"** + 编码调试更复杂 + 缺文档（2018 时点）——**结论："WebFlux 尚未证明明显优于 MVC"**（docs 原文照录，标注第三方报告）
- **对比取舍**：**省线程 vs 更快**——Reactive 收益在"高连接/慢 IO 场景的资源效率"（docs：需要 latency + 混合慢不可预测网络 IO 时差异 dramatic）——**不是银弹**（docs 反复）
- **测试佐证**：docs §Reactor 观点/§Spring 官方说明/§JHipster 报告（原文照录）

### KP-02 阻塞→并行→异步→Reactive 演进（串行/CompletionService/CompletableFuture 推导）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：并发基础
- **来源**：docs §阻塞弊端/§并行复杂/§Future 阻塞与链式问题（完整推导）
- **需求**：理解 **Reactive 的演进动机**——docs 用 DataLoader 系列示例逐步推导（串行 6s → 并行 3s → Future 阻塞回退 → CompletableFuture 链式）
- **自主实现**：若我设计——无依赖并行（CompletionService）→ 有依赖链式（CompletableFuture thenRun）→ 需背压/流式（Reactive）
- **参考实现**（docs 示例推导）：**串行（docs）**——load 1s+2s+3s=6s（"Blocking 模式即串行执行"）；**并行（docs）**——CompletionService 三任务 max(1,2,3)=3s；**延伸思考（docs 3 问）**——①Reactive 能解决阻塞？②为何不用 Future#get() 强制等待？③无依赖才并行，有依赖怎么办？；**Future 阻塞（docs 答案）**——逐一 `future.get()` 从并行退回串行；**Future 链式缺陷（docs）**——无法链式处理 → **CompletableFuture**（`runAsync().thenRun().thenRun().whenComplete().join()` 示例——主线程切 CompletableFuture 线程再回主）
- **对比取舍**：**并行 vs 异步 vs Reactive**——并行（资源换时间）vs 异步（非阻塞复用线程）vs Reactive（+背压/流式/组合）——**演进线 = 解决组合与背压**
- **测试佐证**：docs §阻塞/§并行/§Future（DataLoader 三变体代码全文 + 结论）

### KP-03 Reactive Programming 定义多源（Manifesto/数据流/观察者延伸/推拉对比）
- **维度**：`[分布式理论]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **来源**：docs §Reactive Programming 定义（6 源引用）+ §特性
- **需求**：掌握 **Reactive 的多源定义与统一**——docs 引 Manifesto/维基/Spring/ReactiveX/Reactor/@andrestaltz 六源
- **自主实现**：若我设计——抓共性：**数据流（data streams）+ 传播变化（propagation of change）+ 观察者延伸 + 推模式（push）**
- **参考实现**（docs 六源关键字 + 发散）：**Reactive Manifesto**——系统四性：**Responsive（响应）/Resilient（韧性）/Elastic（弹性）/Message Driven（消息驱动）**；**维基**——声明式编程范式：数据流 + 传播变化（技术连接：Java 8 Stream/Observable-Observer/EventObject）；**Spring**——**reacting to change（响应变化）+ 非阻塞**（技术连接：Servlet 3.1 ReadListener/WriteListener + 3.0 AsyncListener）；**ReactiveX**——观察者模式扩展：数据/事件序列 + 操作符组合 + **屏蔽并发细节**；**Reactor**——观察者/响应流/迭代器三模式 + **推（push）vs 拉（pull）**（Iterable 拉 vs Observable 推对照表：next()↔onNext()/异常↔onError/结束↔onComplete）；**@andrestaltz**——"异步数据流编程"（并非新鲜事物，事件总线是流）；**特性小结（docs）**——编程模型（响应式/函数式 vs Imperative 命令式）+ 设计模式（Observer 推/Reactor Proactor 混合/Iterator 拉）+ 数据结构（流/序列/事件）+ 并发模式（非阻塞同步/异步）
- **对比取舍**：**推（push）vs 拉（pull）**——Reactive 的核心范式转换（订阅式数据消费）
- **测试佐证**：docs §定义（6 源关键字）+ §特性（四维小结）

### KP-04 Reactive Streams 规范（Publisher/Subscriber/Subscription/Processor + 背压）【docs 主要内容②背压基础】
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **来源**：docs §Reactive Streams 规范（四接口源码 + 背压）+ docs 主要内容②（背压重构订单）
- **需求**：掌握 **Reactive Streams 四接口与背压机制**——docs 主要内容第 2 条"利用背压重构订单处理"的规范基础
- **自主实现**：若我设计——Publisher（发布）→ Subscriber（订阅）→ Subscription（信号控制：request/cancel）→ Processor（两者合体）；**背压 = 下游 request(n) 控制上游速率**
- **参考实现**（docs 接口源码）：**四接口（docs 源码）**——`Publisher<T>.subscribe(Subscriber)` / `Subscriber.onSubscribe/onNext/onError/onComplete` / `Subscription.request(long n)/cancel()` / `Processor<T,R> extends Subscriber<T>, Publisher<R>`；**背压机制（docs 总结）**——下游在无边界流水线上消费不及上游生产时 → **传播 request 信号限制需求（Demand）或通知停止生产**；**Reactive Streams 目标（docs）**——管理异步边界（asynchronous boundary）的流式数据交换 + **强制非阻塞背压**（避免无界缓冲）；**docs 主要内容②意图**——订单处理场景用背压控制流量（削峰/防压垮）
- **对比取舍**：**request(n) 背压 vs 无界队列**——资源可控 vs 内存爆炸——背压是响应式流的核心价值（docs"mandatory non-blocking backpressure"）
- **测试佐证**：docs §Reactive Streams 规范（四接口源码 + 背压总结原文）+ docs 主要内容②（背压重构订单）

### KP-05 Reactor 核心（Mono/Flux/Scheduler 四线程池）
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §Reactor 框架运用（核心 API/实战）
- **需求**：掌握 **Reactor 三核心**——Mono/Flux/Scheduler（docs 完整参数）
- **自主实现**：若我设计——Mono（0-1 非阻塞 Optional）/Flux（0-N 非阻塞 Stream）/Scheduler 按场景选线程池
- **参考实现**（docs 完整参数）：**Mono**——0-1 非阻塞结果（实现 Reactive Streams Publisher；类比非阻塞 Optional；点对点模式）；**Flux**——0-N 非阻塞序列（类比非阻塞 Stream；发布者/订阅者模式）；**Scheduler 四线程池（docs 参数实证）**——`Schedulers.immediate()`（当前线程）/`Schedulers.single()`（单线程：内部名 single，底层 ScheduledThreadPoolExecutor core 1）/`Schedulers.elastic()`（弹性：无限制线程 + 60s 空闲回收）/`Schedulers.parallel()`（并行：**处理器数量**线程 + 60s 空闲）；**实战（docs）**——`Flux.generate`（同步一对一 + SynchronousSink）/`Flux.create`/`Flux.handle`（条件发射示例）+ `reactor-core` 依赖
- **对比取舍**：**elastic（无界）vs parallel（核数）**——IO 阻塞任务弹性 vs CPU 密集并行——**选错线程池 = 资源失控**（docs 参数意义）
- **测试佐证**：docs §Reactor 核心 API（Mono/Flux 定义 + Scheduler 四参数表 + generate/handle 代码）

### KP-06 WebFlux 编程模型与并发模型（注解 API 相同/函数式端点/线程池对比）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：13 篇 KP-03/04
- **来源**：docs §WebFlux 核心（编程模型/并发模型/核心组件/Web MVC vs WebFlux 对照表）
- **需求**：掌握 **WebFlux 编程模型与并发模型**——docs 的 API 对照表 + 线程池本质（13 篇 KP-03 深化）
- **自主实现**：若我设计——注解控制器 API 与 MVC 相同（迁移成本低）+ 函数式端点（RouterFunction）+ 小固定线程池（事件循环）
- **参考实现**（docs 对照表 + 发散）：**编程模型两形态（docs）**——**注解驱动**（与 Web MVC 几乎相同：@Controller/@RequestMapping 系/@RequestParam 系/@RequestBody/ResponseEntity/@ControllerAdvice/@CrossOrigin（CorsFilter vs CorsWebFilter）——**API 表面相同，运行时不同**）+ **函数式端点**（13 篇 KP-04 已提取：RouterFunction/HandlerFunction/ServerRequest/ServerResponse + Java 函数式基础 @FunctionalInterface/Consumer/Supplier/Function/Predicate）；**并发模型（docs 官方）**——MVC："**假设应用可能阻塞**（如远程调用）→ 容器用**大线程池**吸收阻塞" vs WebFlux："**假设不阻塞** → 小固定线程池（event loop workers）"——**13 篇"更省线程"的官方表述**；**核心组件（docs）**——HttpHandler（最低层契约：`Mono<Void> handle(ServerHttpRequest, ServerHttpResponse)`）+ WebHandler API（Bean 表：WebExceptionHandler/WebFilter/webHandler/webSessionManager/serverCodecConfigurer/localeContextResolver）+ **MVC vs WebFlux 对照表**（DispatcherServlet ↔ DispatcherHandler/HandlerMapping/HandlerAdapter/HandlerExceptionResolver ↔ HandlerResult.exceptionHandler/@EnableWebMvc ↔ @EnableWebFlux/WebMvcConfigurer ↔ WebFluxConfigurer 等）；**动机（docs）**——**去 Servlet 化**（Servlet 3.1 非阻塞 API 与同步契约（Filter/getParameter）割裂 → 新通用 API；Netty 为默认运行时）
- **对比取舍**：**同 API 双运行时**——注解迁移成本低 vs 并发模型差异（阻塞假设）——**迁移时最大陷阱：阻塞代码进 WebFlux**
- **测试佐证**：docs §编程模型（注解/函数式对照表）+ §并发模型（官方原文）+ §核心组件（HttpHandler 源码 + Bean 表 + MVC/WebFlux 对照表）

### KP-07 WebFlux 使用场景（何时有优势：慢不可预测网络 IO）【docs 性能决策】
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §使用场景（Spring 官方 + JHipster）+ 架构师整合
- **需求**：掌握 **WebFlux 的选型时机**——docs 官方：需要"latency + 混合慢不可预测网络 IO"时收益 dramatic
- **自主实现**：若我设计——选型判据：①高并发连接（网关/入口）②慢/不可预测 IO 混合（聚合调用/流式）③CPU 密集/DB 密集→不选
- **参考实现**（docs 官方 + 发散 + my-xhs 对照）：**官方条件（docs）**——"**需要一些延迟，包括混合慢且不可预测的网络 IO**——那里 Reactive 开始显优势，差异 dramatic"；**JHipster 反面（docs）**——MySQL/Mongo 阻塞 DB 场景无提升甚至稍差——**阻塞数据层抵消收益**；**my-xhs 对照**——**gateway（入口/聚合/IO 密集）已 WebFlux**（05/13 篇实证——正符合选型判据）；**user/order（DB 密集 WebMVC）未重构**（docs 主要内容①"用户服务 WebFlux 重构"在 my-xhs 未做——**符合判据：DB 阻塞栈收益有限**）
- **对比取舍**：**入口/网关 Reactive + 业务 DB 阻塞 WebMVC**——混合架构（my-xhs 实证）vs 全栈 Reactive——**按层选型而非运动**
- **测试佐证**：docs §使用场景（官方条件 + JHipster 报告）+ my-xhs（gateway WebFlux/user WebMVC 实证）

### KP-08 RSocket 服务端 Reactive 化（docs 空节发散 + 背压应用）【docs 主要内容②】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-04（背压）、13 篇
- **来源**：docs 主要内容② + §RSocket（**空节标题**）+ 架构师发散
- **需求**：掌握 **RSocket 服务端 Reactive 化的机制**——docs 主要内容第 2 条（docs 空节，发散补全）：RSocket 协议 + 背压重构订单
- **自主实现**：若我设计——RSocket（二进制/多路复用/四交互模型/背压内建）作为服务间 Reactive 传输，替代 REST/RPC 的阻塞调用
- **参考实现**（docs 意图 + 发散 + my-xhs 对照）：**RSocket 协议（发散）**——二进制协议（TCP/WebSocket 传输）；**四交互模型**——Request-Response/Request-Stream/Channel（双向流）/Fire-and-Forget；**背压内建**——Reactive Streams 语义贯穿（KP-04 的 request(n) 在服务间生效）；**docs 意图**——"基于 RSocket 实现服务端 Reactive 化，利用背压重构订单处理，提升吞吐"——**订单服务间调用（阻塞 HTTP）→ RSocket（响应式背压）**：削峰（下游 request(n) 限速）；**my-xhs 对照**——未用 RSocket（grep 实证无依赖）——服务间走 Feign（HTTP 阻塞，06 篇）——**现状：背压重构订单未落地**（订单为 WebMVC + CompletableFuture 异步，09 篇）；**选型（发散）**——RSocket vs gRPC（08 篇 Triple）——**两者都是服务间响应式/流式协议**（RSocket 重背压交互模型，gRPC 重 HTTP/2 生态）——现代服务间调用主流仍 HTTP/gRPC，RSocket 场景有限
- **对比取舍**：**RSocket（背压交互）vs gRPC（HTTP/2 生态）vs Feign（HTTP 阻塞）**——服务间传输选型矩阵；my-xhs 用 Feign（现状合理，背压场景未触发）
- **测试佐证**：docs 主要内容②（意图原文）+ §RSocket（空节标题）+ my-xhs（grep 无 RSocket + Feign 06 篇）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Reactive 本质与性能真相 | 性能优化 | 核心 | P1 | 🔴 | 时间无关 | High |
| 阻塞→并行→异步→Reactive 演进 | 性能优化 | 核心 | P2 | 🟡 | 时间无关 | High |
| Reactive 定义多源 | 分布式理论 | 支撑 | P2 | 🟡 | 时间无关 | High |
| Reactive Streams 规范（四接口/背压） | 规范 | 核心 | P1 | 🔴 | 时间无关 | High |
| Reactor 核心（Mono/Flux/Scheduler） | 规范 | 支撑 | P2 | 🟡 | 有效 | High |
| WebFlux 编程/并发模型 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| WebFlux 使用场景（选型时机） | 性能优化 | 支撑 | P2 | 🟡 | 有效 | High |
| RSocket 服务端 Reactive 化 | 分布式问题 | 核心 | P1 | 🟡 | 有效 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（混合架构实证）+ spring-framework（13 篇）
- **关键源码**（本次实证）：
  - my-xhs `my-xhs-user/pom.xml:22`（spring-boot-starter-web——**用户服务 WebMVC 未重构**，docs 主要内容①对照）+ `my-xhs-gateway/pom.xml:22`（spring-cloud-starter-gateway——**WebFlux**，05/13 篇）+ gateway `RateLimiterConfig.java:7`（`import reactor.core.publisher.Mono`——Reactor 实证）
  - my-xhs 无 RSocket（grep 实证）+ order 无 Flux/Mono（grep 实证）
- **诚实标注**：docs §RSocket 为**空节标题**（仅链接 rsocket.io）→ KP-08 架构师发散补全；docs 的 JHipster 报告为第三方引用（2018 时点，照录原文含"WebFlux 尚未证明优于 MVC"结论）；docs §执行流程两张图（核心组件初始化/请求处理流程）为图片佐证（与 MVC 流程类似，docs 原文），不独立提取；docs 引 Spring 5.0.x 文档（版本过时，机制有效）；docs 主要内容①"基于 WebFlux 重构用户管理 REST 服务"在 my-xhs **未重构**（user=WebMVC，pom 实证）——按选型判据（DB 密集）合理 `[现状说明]`
- **关联标注**：13 篇（WebFlux 架构——本篇深化：编程/并发模型）；09 篇（异步/非阻塞三路径——本篇 Reactive 栈）；02 篇（性能方法论——JHipster 报告的口径）；08 篇（gRPC/Triple——RSocket 选型对照）

---

## 五、本节小结（三层次视角）

**需求**：Reactive 异步服务完整认知——本质真相、演进动机、Reactive Streams 规范、Reactor、WebFlux 模型、RSocket。

**自主实现核心**：若我设计——①先认知纠偏（不更快，省线程可预测扩展）②演进判断（组合/背压需求才 Reactive）③Reactive Streams 四接口 + request(n) 背压 ④Mono/Flux + Scheduler 按场景选池 ⑤WebFlux 选型判据（高连接/慢 IO 混合）⑥服务间背压（RSocket）按需。

**参考实现**：docs（Reactive 深度教程 + 官方/JHipster 引用 + 四接口源码）+ my-xhs 混合架构实证（gateway WebFlux + user/order WebMVC + 无 RSocket）。**docs 原文照录，未编造**。

**对比取舍**：知识本体是"**Reactive 异步服务的完整认知**"——真相（不更快）+ 规范（四接口/背压）+ 模型（WebFlux 编程/并发）+ 选型（何时用/何时不用）；my-xhs 的混合架构（入口 Reactive + 业务阻塞）符合 docs 官方选型判据。

**待验证汇总**：
- my-xhs 网关的 Reactor 实际用法（RateLimiterConfig Mono 实证已见，全貌 19 节）
- RSocket 引入评估（当前 Feign 满足，背压场景未触发）
- JHipster 报告时点后的 WebFlux 演进（docs 引用 2018 数据）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标（升级动作） | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| ① 客户端 Reactive（WebFlux 重构用户服务） | ⚠️ **未重构**：user 服务 WebMVC（pom:22 实证） | 现状说明：按 docs 官方选型判据（DB 密集阻塞栈），用户服务转 WebFlux 收益有限——**决策合理非差距**；gateway 已 WebFlux（入口侧 Reactive 已落地） |
| ② 服务端 Reactive（RSocket + 背压重构订单） | ❌ 未采用：无 RSocket 依赖（grep 实证）+ order 无 Flux/Mono | 现状说明：订单服务间走 Feign（HTTP 阻塞，06 篇）+ 内部 CompletableFuture 异步（09 篇）；背压场景（削峰）未触发——**决策待定** |
| Reactive 认知落地 | ✅ 混合架构（入口 Reactive + 业务阻塞）符合官方选型判据 | 无 |

### 差距清单（Reactive 层）

1. **P3**：网关 Reactor 用法全貌核对（RateLimiter/路由——19 节展开）
2. **P3**：订单背压场景评估（若大促/削峰需求出现 → RSocket/流式方案）
3. **P3**：WebFlux 性能实测（docs 的"对比 Servlet 异步+非阻塞性能"意图——my-xhs 无对比数据，02 篇同一缺口）

**结论**：17 篇——my-xhs 的 **Reactive 落地 = 网关层**（符合选型判据）；docs 的①②（用户服务重构/RSocket 订单）在 my-xhs 为"现状说明/决策待定"（按判据不做的决策是合理的，非差距）；Reactive 组（13-17）收官。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Reactive 深度教程（官方/JHipster 多源引用 + 接口源码）；RSocket 空节；my-xhs 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：Reactive 异步服务的完整认知该讲什么

docs 覆盖 Reactive 全谱。完整还该包含：

1. **"Reactive 不更快"是选型的第一课**（docs 多源 + 发散）：JHipster 实证（无提升甚至稍差）与 Spring 官方（省线程可预测扩展）——**Reactive 解决"资源效率"不解决"单请求速度"**；**收益面 = 高连接数 + 慢不可预测 IO 混合**（docs 官方条件）——**没有这种负载，迁移是负收益**
2. **背压是 Reactive 的核心资产**（docs 规范 + 发散）：request(n) 让**消费速率决定生产速率**——订单削峰/流控的机制基础（docs ②）；**对比传统消息队列的削峰**（14/16 篇）——MQ 削峰靠队列缓冲，背压削峰靠速率协商——两个维度
3. **WebFlux 迁移的最大陷阱是"阻塞泄漏"**（docs 并发模型 + 发散）：MVC 假设可阻塞（大线程池），WebFlux 假设不阻塞（小固定线程池）——**迁移时任何 JDBC/Thread.sleep 都会堵死事件循环**（13 篇纪律）；**DB 层不响应式化就别迁**（JHipster 报告的 MySQL 场景教训）
4. **同 API 双运行时的双刃剑**（docs 对照表 + 发散）：注解迁移成本低（API 表面相同）**但语义不同**（线程模型/阻塞假设/事务）——**"表面迁移"是最大的隐性风险**
5. **Scheduler 选池纪律**（docs 参数 + 发散）：elastic（无界，IO 阻塞任务）vs parallel（核数，CPU 密集）——**选错 = 线程失控**（无界 elastic 高峰爆炸）
6. **服务间响应式传输的选型**（docs ② + 发散）：RSocket（背压交互模型）vs gRPC（HTTP/2 生态，08 篇）vs Feign（阻塞 HTTP）——**my-xhs 用 Feign（现状）；服务间背压需求出现时再评估**——传输选型跟业务场景走

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| Reactive vs 阻塞 | 资源效率 vs 简单（不更快！） |
| 全栈 Reactive vs 混合 | 一致性 vs 按层务实（my-xhs 混合实证） |
| request(n) 背压 vs 无界队列 | 速率协商 vs 内存爆炸 |
| elastic vs parallel Scheduler | 无界 IO vs 核数 CPU（选错失控） |
| 注解 vs 函数式端点 | API 熟悉 vs 组合灵活 |
| RSocket vs gRPC vs Feign | 背压交互 vs HTTP/2 生态 vs 阻塞简单 |
| 迁移 vs 不迁移 | 收益面（慢 IO 混合）vs 阻塞泄漏风险 |

### 常见坑/反模式

1. **把 Reactive 当性能银弹**：JHipster 实证无提升——按收益面选（docs 多源）
2. **阻塞泄漏进事件循环**：JDBC/Thread.sleep 堵死 WebFlux——小固定线程池全卡（13 篇纪律）
3. **Scheduler 用错**：elastic 无界线程高峰爆炸（docs 参数）
4. **"表面迁移"**：注解 API 相同但并发模型不同——迁移要重测（docs 对照表）
5. **Future.get() 强制等待**：并行退回串行（docs 示例教训）
6. **无背压的流式处理**：无界缓冲内存爆炸（docs 规范强制背压）
7. **DB 层阻塞还硬迁 WebFlux**：JHipster MySQL 场景教训——先响应式化数据层

### 生态位置

- **stage-3 教学主线**：加餐/事件组（13-17）**收官**——13 WebFlux 架构 → 14 事件设计 → 16 分布式事件 → **17 Reactive 异步服务（本篇）**（15 缺失）——事件/Reactive 组闭环；18 转 JVM 故障分析
- **前后篇衔接**：13 篇（WebFlux 架构）→ 本篇（编程/并发模型深化 + 背压）；09 篇（异步三路径）→ 本篇（Reactive 栈完整认知）；02 篇（性能方法论——JHipster 口径）；08 篇（gRPC——RSocket 对照）
- **与源码提取的关系**：spring-framework（WebFlux）+ my-xhs（gateway Reactor 实证）为参考源；RSocket 无本地源码 `[无本地源码]`

**架构师视角结论**：本篇以 **docs 深度教程讲 Reactive 完整认知**（真相：不更快/JHipster 实证、规范：四接口+背压、模型：WebFlux 编程/并发、Reactor：Mono/Flux/Scheduler）、**my-xhs 混合架构实证**（gateway WebFlux + user/order WebMVC——符合官方选型判据）——知识本体是"**Reactive 异步服务的选型与机制**"：收益面判据（慢 IO 混合）、背压机制（request(n)）、迁移陷阱（阻塞泄漏）、服务间传输选型（RSocket 空节发散）；docs 的①②（用户服务重构/RSocket 订单）在 my-xhs 为合理不做的决策；**事件/Reactive 组（13-17）收官**，下篇转 18（生产环境 JVM 故障分析）。
