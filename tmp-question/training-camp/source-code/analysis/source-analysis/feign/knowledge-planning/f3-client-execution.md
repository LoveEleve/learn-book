# F-3 Client 执行 + 拦截体系 — 知识规划 (KP)
> **ℹ 8-16 深化版 (备查)**: 本 KP 为同会话 8-16 深化产物, 与权威版 (f6-template/f3-proxy/f1-builder) 重复。内容已并入权威版对应域, 本文件保留作增强参考。

> 域: F-3 | 级别: 🔴 | 方案: A | 大纲: outlines/f3-client-execution/outline.md (8 节)

## §01 域定位

Feign 执行链 = 接口方法到 HTTP 往返的"调用内核"。三层拦截 (Method 柯里化 / Request 线性 / Response 柯里化) + 重试循环 (5 次尝试/4 次重试, 1.5 指数退避) + ResponseHandler 响应链 (13.x 重构) + Client 可插拔面。定义特征: 13.x 响应链重构 + 退避数学 + safe-to-replay 契约。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| SynchronousMethodHandler.java | 226 | 执行链核心: invoke/runWithRetry/executeAndDecode/targetRequest | 1,2,4 |
| DefaultRetryer.java | — | 退避重试 (100ms/1s/5) | 3 |
| Retryer.java | — | 重试接口 | 3 |
| ResponseHandler.java | 98 | 响应链守卫 | 5 |
| ResponseInterceptor.java | 70 | 响应拦截接口 (柯里化) | 8 |
| InvocationContext.java | 144 | 响应链上下文 + proceed 解码 | 6 |
| RedirectionInterceptor.java | — | 3xx+Location 处理 | 8 |
| Client.java | 109 | Client 接口 + Default/Proxied | 7 |
| DefaultClient.java | 244 | JDK HttpURLConnection 实现 | 7 |
| MethodHandlerConfiguration.java | — | 10 字段装配载体 | 1 |
| interceptor/MethodInterceptor.java | — | 方法级拦截器 (@Experimental) | 1,8 |
| RequestInterceptor.java | 52 | 请求前拦截器 (root 包) | 8 |

## §05 闭环要点 (Pass 2 内化)

### q1 三层拦截
Method (方法级, 柯里化 reduce+apply, 可短路) → Request (请求前, 线性 for) → Client.execute → Response (响应后, 柯里化包 decode)。层级粒度递进 (13.x 演化: Request 远古 → Response 13.x → Method @Experimental)。

### q2 响应解码链
executeAndDecode → responseHandler.handleResponse (日志缓冲+守卫) → InvocationContext.proceed 五分支 (Response 直返/状态码判定/void/TypedResponse/默认 decode) + ensureClosed 双保险。

### q3 重试数学
attempt++ >= maxAttempts 抛 (5 次尝试 4 次重试); 退避 period×1.5^(attempt-1) 钳制 maxPeriod (150/225/337/506); retryAfter 优先; clone 隔离状态。

### q4 Client 面
单方法契约 + safe-to-replay; Default (JDK) / Proxied (代理) / 第三方模块各自实现。

## §06 负面空间 (6 条)

不重试非 Retryable / 不自动重放请求体 / 不异步 (F-6) / 不连接池 / 不自动跟随重定向 / 不缓存响应

## §07 交叉引用

- ← F-5 URI 模板 (RequestTemplate) + F-2 Contract (MethodMetadata)
- → F-1 Builder 代理 (MethodHandler.Factory)
- → F-6 异步 (AsynchronousMethodHandler 对照)
- 另见: OkHttp Interceptor / Spring Retry / HikariCP (连接池对照)
