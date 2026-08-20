# F-3 代理与调用链 — 接口的"皮影戏": JDK 代理背后的一次完整调用

> 前置: [[F-2-契约解析]] (MethodMetadata) + [[F-6-模板引擎]] (RequestTemplate) + [[F-4-编解码]] (Decoder/ErrorDecoder) + [[F-5-拦截器链]] (三拦截器) | 引出: [[F-1-Builder装配]] (怎么拼起来) | 对照: Dubbo 代理 + Spring AOP 代理
> 🔴 A | 7 KP | [模式: JDK 动态代理 + 责任链]
> Pass 2 闭环: q1(代理装配) q2(执行链) q3(重试) q4(响应裁决) q5(异步)

**读者处境**: `api.getUser(42)` 一行调用, 从"接口方法"到"HTTP 请求发出"之间发生了什么? 重试了几次? 404 怎么变成异常? 返回 CompletableFuture 的接口为什么同步接口也能共用元数据?

### 1. 代理装配 — 接口怎么变成对象

场景: target(apiType, url) 返回的到底是什么?
源码路径:
- **TargetSpecificationVerifier.verify: 必须是接口** (ReflectiveFeign.java:150-152); **四检查** (L173-203): ① 接口校验 ② **同步方法任意返回类型跳过** (非 CF 可赋值 continue L183-185) ③ **CF 子类拒绝** (retType != CompletableFuture.class 抛 L187-189, 必须精确 CF) ④ CF 非参数化/Wildcard 泛型拒绝 (L192-201)
- ParseHandlersByName.apply (L137-160): contract 解析 → 逐方法建 MethodHandler; **default 方法 → DefaultMethodHandler** (L153-159); **@FeignIgnore → 抛错的 handler** (L170-174)
- **FeignInvocationHandler** (L75-119): dispatch map; equals/hashCode/toString 直通 — **equals 语义**: Proxy.getInvocationHandler 取对端 handler (L89-91), 非代理/异常 false (L92-93), 对端 FeignInvocationHandler → target.equals (L111-117, HardCodedTarget 比较 type+name+url Target.java:109-114)
- Proxy.newProxyInstance (L64)
- **default 方法 MethodHandle 直调** (DefaultMethodHandler.java:45,128-133): unreflectSpecial + bindTo(proxy) — 绕开 InvocationHandler (JDK 代理会拦截 default 方法, 只能 MethodHandle 直调接口实现)
关键设计 (q1): **方法 → handler 的一一映射在 newInstance 时固化** — 每个方法一个 MethodHandler (通常 SynchronousMethodHandler), 调用时 O(1) 分发; default 方法不建 HTTP handler 直通实现; verify 前置拒绝非法接口/返回类型, 不等到调用期。 [模式: 映射表代理]

### 2. 执行链 — invoke 到 Request

场景: 代理方法被调用后, 第一行代码是什么?
源码路径:
- SynchronousMethodHandler.invoke (L49): **retryer.clone()** (L69) → runWithRetry → executeAndDecode
- **MethodInterceptor 链** (L59-65): endOfChain = runWithRetry → reduce(andThen) → chain.next — 拦截器包住含重试的全程
- executeAndDecode (SynchronousMethodHandler.java:103-121): **targetRequest** (L105: RequestInterceptor for-each → getTarget().apply 绑定主机 → 不可变 Request) → client.execute (L119) → responseHandler.handleResponse
关键设计 (q2): **链的末端是 runWithRetry** — MethodInterceptor 能拦到重试中的每次执行; RequestInterceptor 在发送前一刻变异模板。 [模式: 分层责任链]

### 3. 重试 — clone 的独立性

场景: 重试策略怎么做到"每请求独立"?
源码路径:
- **retryer.clone()** (L69): 每次调用前克隆 — 计数互不污染
- DefaultRetryer: 100ms 起 ×1.5 退避 1s 封顶, maxAttempts=5 (DefaultRetryer.java:28-30, 77-80)
- Retry-After 优先 (L50-57); 中断恢复中断位 (L63-66)
关键设计 (q3): **克隆 = 状态隔离** — 并发调用同一 proxy 时每个请求独立计数; 只有 RetryableException 触发重试 (连接错误/带 Retry-After 的错误)。 [模式: 原型克隆]

### 4. 响应处理 — 日志与裁决

场景: HTTP 响应回来后, 解码前发生了什么?
源码路径:
- ResponseHandler.handleResponse (ResponseHandler.java:65-88): **logAndRebufferResponseIfNeeded** (L68) → executionChain.next(InvocationContext) (L69-78); configKey 贯穿日志 (logRetry/logRequest/logIOException, SynchronousMethodHandler.java:95-139 — 跨域共享键 F-1 §5)
- **日志后流已消费, 必须 rebuffer 重建 Response** (Logger.java:129-136)
- InvocationContext 分发 (InvocationContext.java:76-86): 2xx 或 404-dismiss → Decoder; 否则 → ErrorDecoder; void → 关 body null
关键设计 (q4): **日志与解码的顺序依赖** — 日志先读流, 解码需要完整 body, rebuffer 是两者的桥梁; ResponseInterceptor 挂在裁决外围。 [模式: 缓冲重建]

### 5. 异步变体 — 同步接口的"共用元数据"

场景: 异步接口怎么实现? 与同步差多少?
源码路径:
- AsyncFeign (285): 异步 Builder; AsynchronousMethodHandler (322)
- **methodInfoResolver.resolve** (AsynchronousMethodHandler.java:319): CompletableFuture<T> → 底层 T (MethodInfo.java:36-39); isAsyncReturnType 决定等不等 Future (L72); underlyingReturnType 解码 (L232)
- 同步/异步共用同一契约元数据 (F-2 M4)
关键设计 (q5): **异步性剥离出契约** — 解码器只见底层类型; 同步/异步/协程 (KotlinMethodInfo) 三客户端共用一套解析结果。 [模式: 类型剥离]

### 6. 客户端 — 最后一百米

场景: Request 交给谁发出?
源码路径:
- **Client.execute(Request, Options) throws IOException**; 默认 DefaultClient (JDK HttpURLConnection) (Feign.java:99)
- **Client 三形态**: Default (JDK 直连) / Proxied (装饰: 日志/重试/metrics) / 第三方实现 (okhttp/httpclient/hc5/java11 各模块)
- Options: connectTimeoutMs/readTimeoutMs (默认 10s?)
关键设计 (q6): **Client 是传输抽象** — 换 HTTP 库只换 Client; IOException → FeignException.errorExecuting 包装 (FeignException.java:302-311) → RetryableException 触发重试。 [模式: 传输抽象]

### 7. Target — 主机绑定

场景: url 里的主机哪来的?
源码路径:
- HardCodedTarget: type()/name()/url(); **apply(RequestTemplate) 绑定主机** (targetRequest 末尾)
- 每次调用 target.apply 到模板
关键设计 (q6): **模板不含主机, 绑定在最后** — 契约解析时不知道目标主机; Target 抽象允许动态目标 (Eureka 场景)。 [模式: 晚期绑定]

## 代码类型
Architecture (调用执行)

## 负面空间 — Feign 调用链刻意不做的事

- **不做同步阻塞控制**: 超时靠 Options, 无线程池内嵌
- **不做连接池**: Client 实现的事
- **不做熔断内建**: hystrix 独立模块 (13.x 移除内置)
- **不做代理缓存**: 每次 newInstance 重建 (README 建议调用方缓存)
- **不做请求重放正式 API**

→ 引出: 这些组件怎么被拼起来? → F-1 Builder 装配
