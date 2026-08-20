# F-5 拦截器链 — 知识规划 (KP)

> 域级: 🟡 B | 模块: RequestInterceptor (55) + RequestInterceptors (32) + interceptor/ (MethodInterceptor 98/Invocation 105/MethodInterceptors 33) + ResponseInterceptor (69) + ResponseInterceptors (32) + auth/ (BasicAuthRequestInterceptor 67)
> 日期: 2026-08-15 | 版本: 13.14-SNAPSHOT

## 一、机制提取 (逐源)

### M1 RequestInterceptor (55)
- apply(RequestTemplate) (L55); **无执行顺序保证** (L20 文档); **在模板参数 resolve 之后应用** (L42-44 — 实现里能看到解析后的签名)
- 执行位置: SynchronousMethodHandler.targetRequest (L147-152) — for-each 逐个 apply → getTarget().apply(template) 生成不可变 Request

### M2 RequestInterceptors (32)
- 非"链": 不可变列表包装 (L26-32) — 顺序无关的模板变异

### M3 MethodInterceptor (98, 13.x @Experimental)
- **intercept(Invocation, Chain)** (L62); andThen 组合 (L75-78); Chain.DEFAULT 哨兵 (L84-87)
- **生命周期: 每次方法调用一次, 在 Contract 解析出模板之后、RequestInterceptor 之前** (L26-38); 能看到**类型化参数** + MethodMetadata + 可变模板 + 链末端 Response; **可短路** (不调 chain.next, L39 文档)
- 接线: SynchronousMethodHandler.java:59-65 — endOfChain = runWithRetry (L59) → reduce(andThen) (L62) → apply (L63) → chain.next(invocation) (L65)
- **包住整个请求生命周期** (含重试/HTTP 执行/响应拦截/解码)
- 使用面: validation/http-cache 模块 (AGENTS.md 实证)

### M4 Invocation (105)
- 构造: target/methodMetadata/requestTemplate/arguments (L42-51); **requestTemplate 可变** (L66 注释, 下游可见); **response volatile 后期填充** (L93-104)
- body() 按 bodyIndex 取请求体参数 (L80-86); 实例在 SynchronousMethodHandler.java:52-57 创建, L135 填充 response

### M5 ResponseInterceptor (69, 13.x 新)
- intercept(InvocationContext, Chain) (L34); Chain.DEFAULT = InvocationContext::proceed (L50)
- **挂在解码过程外围**: ResponseHandler.java:65-88 — logAndRebuffer (L68) → executionChain.next (L69-78) → 终端 = InvocationContext.proceed
- InvocationContext 分发 (L69-100): 2xx 或 (404 && dismiss404 && 非 void) → Decoder (L76-77, L121); 否则 decodeError → ErrorDecoder (L79-81, L133); void → 关 body 返回 null (L83-86)
- **与 MethodInterceptor 区别**: ResponseInterceptor 只在响应侧 (HTTP 已完成), MethodInterceptor 横跨全程

### M6 auth/BasicAuthRequestInterceptor (67)
- **构造时一次性算好 headerValue** (L28 缓存): "Basic " + base64(user:pass) (L52); 默认 ISO-8859-1 (RFC 7617, L37-39)
- apply 只做 header 覆盖 (L66) — RequestInterceptor 的标准应用范例

### M7 Retryer (59) + DefaultRetryer (85)
- **默认 period=100ms/maxPeriod=1s/maxAttempts=5** (L28-30); 退避 period × 1.5^(attempt-1) 封顶 maxPeriod (L77-80)
- **clone() 每次调用前克隆** (L69) — 每请求独立计数; Retry-After 优先 (L50-57); NEVER_RETRY 常量 (L47-59)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M3 MethodInterceptor 生命周期 | P1 | 13.x 核心扩展点 |
| M5 响应裁决 | P1 | 404/500 最终裁决 |
| M1 RequestInterceptor | P1 | 经典扩展点 |
| M7 Retryer | P2 | 重试语义 |
| M4/M6 | P2 | 上下文/范例 |

## 三、负面空间

- **不做拦截器顺序保证**: RequestInterceptor 顺序无关
- **不做请求级拦截器实例隔离**: 共享实例 (线程安全由实现负责)
- **不做全局过滤器注册**: 拦截器 per-builder
- **不做响应体重放**: 解码前 rebuffer 由 Logger 触发, 非拦截器职责
- **不做拦截器热插拔**: 构建期固定
