# F-5 拦截器链 — 请求的"安检通道": 三类拦截器各守一段

> 前置: [[F-2-契约解析]] (MethodMetadata) + [[F-4-编解码]] (ErrorDecoder) | 引出: [[F-3-代理与调用链]] (执行链接线) | 对照: Servlet Filter + Spring HandlerInterceptor
> 🟡 B | 7 KP | [模式: 生命周期拦截 + 链式聚合]
> Pass 2 闭环: q1(RI) q2(MI) q3(响应裁决) q4(重试)

**读者处境**: 要给所有请求加认证头, 往哪插? 想在调用前后做监控/校验, 三类拦截器 (Request/Method/Response) 各能干什么? 差在哪?

### 1. RequestInterceptor — 模板的"最后一公里"

场景: Basic Auth 头怎么加上的?
源码路径:
- apply(RequestTemplate) (RequestInterceptor.java:55); **无顺序保证** (L20); **resolve 之后应用** (L42-44)
- 执行: SynchronousMethodHandler.targetRequest (L147-152) — for-each → getTarget().apply 生成不可变 Request
- **BasicAuthRequestInterceptor** (auth/ 67): 构造时**一次性算好 headerValue** "Basic " + base64 (L52, 默认 ISO-8859-1 RFC 7617); apply 只覆盖头 (L66)
关键设计 (q1): **模板变异点** — 拦截器在解析后、发送前一刻改模板; BasicAuth 预计算 headerValue 是性能细节 (每次 apply 零计算)。 [模式: 变异点]

### 2. MethodInterceptor — 13.x 的"环绕式"拦截

场景: 想在请求前后都做点事 (校验/监控), 怎么插?
源码路径:
- **intercept(Invocation, Chain)** (MethodInterceptor.java:62); andThen 组合 (L75-78)
- **生命周期**: 每次方法调用一次, 在契约解析后、RequestInterceptor 前 (L26-38); 能看到**类型化参数** + 可变模板 + 链末端 Response; **可短路** (L39)
- 接线: SynchronousMethodHandler.java:59-65 — endOfChain = **runWithRetry** (L59) → reduce(andThen) (L62) → chain.next (L65)
关键设计 (q2): **包住含重试的全程** — 链末端是 runWithRetry, 所以 MethodInterceptor 能拦到"重试中的每一次 HTTP 执行"; validation/http-cache 模块用它 (AGENTS.md)。 [模式: 责任链]

### 3. 生命周期对比 — 三类拦截器的位置

场景: RequestInterceptor/MethodInterceptor/ResponseInterceptor 什么时候跑?
源码路径:
- **RI**: 发送前一刻, 只碰已解析模板, 无顺序
- **MI**: 调用即触发, 包住全程 (参数/重试/响应), 可短路
- **ResponseInterceptor**: HTTP 完成后, 挂在**解码过程外围** (ResponseHandler.java:65-88)
关键设计 (q2): **三阶段覆盖** — 请求前 (RI 模板变异) / 全程 (MI 环绕) / 响应后 (RI 响应拦截); 13.x 补齐了环绕与响应两个阶段。 [模式: 阶段分工]

### 4. ResponseInterceptor — 解码前的最后裁决

场景: 404 到底是异常还是空值, 谁说了算?
源码路径:
- intercept(InvocationContext, Chain) (ResponseInterceptor.java:34); 挂在解码外围 (ResponseHandler.java:69-78)
- **InvocationContext 分发** (L69-100): 2xx 或 (404 && dismiss404 && 非 void) → Decoder (L76-77); 否则 → **ErrorDecoder** (L79-81); void → 关 body 返回 null (L83-86)
关键设计 (q3): **404/500 的最终裁决点** — 这是 F-4 默认语义落地的执行处; 拦截器可包住裁决做横切 (监控/缓存)。 [模式: 裁决分发]

### 5. Invocation — 调用上下文

场景: 拦截器能看到什么?
源码路径:
- Invocation 构造: target/methodMetadata/requestTemplate/arguments (L42-51); **requestTemplate 可变** (L66, 下游可见)
- **response volatile 后期填充** (L93-104): HTTP 失败时为 null; body() 按 bodyIndex 取参数 (L80-86)
关键设计 (q3): **可变模板 + 后期响应 = 上下文对象** — 拦截器改模板下游可见; response 的 volatile 保证发布安全。 [模式: 上下文对象]

### 6. Retryer — 5 次的耐心

场景: RetryableException 触发重试, 默认等多久?
源码路径:
- **默认 period=100ms/maxPeriod=1s/maxAttempts=5** (DefaultRetryer.java:28-30); 退避 ×1.5 封顶 (L77-80)
- **clone() 每请求独立** (L69); **Retry-After 优先** (L50-57); NEVER_RETRY 常量 (L47-59)
关键设计 (q4): **重试策略是"尝试次数 × 间隔增长"** — 100ms 起 ×1.5, 1s 封顶, 最多 5 次尝试 (4 次重试); 服务端 Retry-After 覆盖本地退避 — 协议优先。 [模式: 指数退避]

### 7. 聚合 — 列表不是链, 链在建 handler 时

场景: RequestInterceptors 是链吗?
源码路径:
- RequestInterceptors = 不可变列表 (L26-32) — 遍历在 targetRequest
- **真正的链在 SynchronousMethodHandler**: MethodInterceptor reduce(andThen) 构建 (L60-64)
关键设计 (q4): **两套聚合语义** — RI 顺序无关 (列表), MI 顺序相关 (链); 由执行语义决定聚合结构。 [模式: 语义驱动聚合]

## 代码类型
Architecture (扩展点)

## 负面空间 — Feign 拦截器刻意不做的事

- **不做顺序保证**: RI 顺序无关 (对比 Spring HandlerInterceptor 有序)
- **不做实例隔离**: 拦截器共享实例, 线程安全由实现负责
- **不做全局注册**: per-builder 配置
- **不做响应流式拦截**: 解码前整体 rebuffer
- **不做拦截器热插拔**: 构建期固定

→ 引出: 拦截器怎么被装进执行链? → F-3 代理与调用链
