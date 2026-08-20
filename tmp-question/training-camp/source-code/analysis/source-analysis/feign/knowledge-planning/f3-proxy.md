# F-3 代理与调用链 — 知识规划 (KP)

> 域级: 🔴 A | 模块: ReflectiveFeign (212) + InvocationHandlerFactory + DefaultInvocationHandlerFactory + SynchronousMethodHandler (226) + ResponseHandler (98) + DefaultMethodHandler + 异步族 (AsyncFeign 285/AsynchronousMethodHandler 322/AsyncResponseHandler 77/AsyncClient 79) + Client 接口 + Target (HardCodedTarget)
> 日期: 2026-08-15 | 版本: 13.14-SNAPSHOT

## 一、机制提取 (逐源)

### M1 ReflectiveFeign.newInstance (L50-64)
- TargetSpecificationVerifier.verify (L56): **必须接口** (L150-152); ParseHandlersByName.apply → Map<Method, MethodHandler> (L58-59)
- **FeignInvocationHandler** (L75-119, 内部类): dispatch map + invoke 分发 (equals/hashCode/toString 直通 L100+ 附近)
- Proxy.newProxyInstance (L64)

### M2 ParseHandlersByName (L127-180)
- contract.parseAndValidateMetadata (L142) → 逐 MethodMetadata createMethodHandler (L161-178)
- Object 方法跳过 (L145); **default 方法 → DefaultMethodHandler** (L153-159); **isIgnored → 抛 IllegalStateException 的 handler** (L170-174)

### M3 SynchronousMethodHandler (226)
- invoke(argv) (L49): **retryer.clone() 每请求独立** (L69) → 循环 runWithRetry → executeAndDecode → 失败 retryer.continueOrPropagate (L72-75)
- **executeAndDecode** (L103-121): targetRequest(template) (L105) → client.execute (L119) → responseHandler.handleResponse
- **targetRequest** (L147-152): RequestInterceptor for-each → getTarget().apply(template) 生成不可变 Request
- **MethodInterceptor 接线** (L59-65): endOfChain = runWithRetry → reduce(andThen) → chain.next
- Factory (L170+): 构造时组装 client/retryer/interceptors/responseHandler/logger/options

### M4 ResponseHandler (98)
- handleResponse (L65-88): **logAndRebufferResponseIfNeeded** (L68) → executionChain.next(new InvocationContext) (L69-78)
- InvocationContext 分发: 2xx 或 (404 && dismiss404 && 非 void) → Decoder (L76-77); 否则 decodeError → ErrorDecoder (L79-81); void → 关 body null (L83-86)
- logAndRebuffer: 日志后流已消费必须重建 Response (Logger.java:129-136)

### M5 异步族 (763)
- AsyncFeign (285): 异步 Builder (AsyncClient/AsyncResponseHandler/AsyncContextSupplier 注入)
- **AsynchronousMethodHandler** (322): methodInfoResolver.resolve (L319) → isAsyncReturnType 决定是否等 Future (L72); underlyingReturnType 解码 (L232); CompletableFuture 链
- AsyncResponseHandler (77)/AsyncClient (79): 异步响应处理/客户端抽象

### M6 Client 接口 + 默认实现
- execute(Request, Options) throws IOException; 默认 DefaultClient (JDK HttpURLConnection) (Feign.java:99)
- 客户端实现族: okhttp/httpclient/hc5/java11 模块

### M7 Target (HardCodedTarget)
- type()/name()/url()/apply(RequestTemplate) — 绑定主机 (SynchronousMethodHandler targetRequest 末尾)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M3 执行链 + 重试 | P1 | 调用核心 |
| M1/M2 代理装配 | P1 | 反射面 |
| M4 响应处理 | P1 | 解码裁决 |
| M5 异步 | P2 | 变体 |
| M6/M7 | P2 | 支撑 |

## 三、负面空间

- **不做同步阻塞控制**: 超时由 Options (connectTimeoutMs/readTimeoutMs) 控制
- **不做连接池**: 客户端实现的事 (okhttp/httpclient)
- **不做请求重放 API**: 需自建
- **不做熔断**: hystrix 独立模块 (13.x 无内置)
- **不做代理缓存**: 每次 newInstance 重建
