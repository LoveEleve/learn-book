# F-5 review-notes — 六层深审记录 (2026-08-15)

## 审法: 探索代理锚点 + 本审抽查 (🟡 B 方案)

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "apply(RequestTemplate) + resolve 之后" — RequestInterceptor.java:55, L42-44 | 通过 ✅ |
| 2 | 事实 | §1 "执行在 targetRequest" — SynchronousMethodHandler.java:147-152 | 通过 ✅ |
| 3 | 事实 | §1 "BasicAuth 预计算 headerValue" — BasicAuthRequestInterceptor.java:28, L52, L66 | 通过 ✅ |
| 4 | 事实 | §2 "intercept(Invocation, Chain) + andThen" — MethodInterceptor.java:62, L75-78 | 通过 ✅ |
| 5 | 事实 | §2 "链末端 = runWithRetry" — SynchronousMethodHandler.java:59-65 | 通过 ✅ |
| 6 | 事实 | §2 "可短路" — MethodInterceptor.java:39 文档 | 通过 ✅ |
| 7 | 事实 | §3 "MI 生命周期在 RI 之前" — L26-38 文档 | 通过 ✅ |
| 8 | 事实 | §4 "ResponseInterceptor 挂解码外围" — ResponseHandler.java:65-88 | 通过 ✅ |
| 9 | 事实 | §4 "InvocationContext 分发 2xx→Decoder/404 dismiss→Decoder/否则 ErrorDecoder" — InvocationContext.java:76-81, L121, L133 | 通过 ✅ |
| 10 | 事实 | §5 "Invocation requestTemplate 可变 + response volatile" — Invocation.java:66, L93-104 | 通过 ✅ |
| 11 | 事实 | §6 "Retryer 默认 100ms/1s/5" — DefaultRetryer.java:28-30; 退避 ×1.5 L77-80; clone L69 | 通过 ✅ |
| 12 | 事实 | §7 "RequestInterceptors 不可变列表" — RequestInterceptors.java:26-32 | 通过 ✅ |
| 13 | 结构 | 负面空间 "不做顺序保证" 与 RI 无序文档自洽 | 通过 ✅ |
| 14 | 过程 | **13.x 代际确认**: MethodInterceptor/ResponseInterceptor 均为 @Experimental 新扩展 — 执行计划 F-5 只列 RequestInterceptor | ✅ PLAN 审计已记录并入 |

**结论**: 14 项核对 0 修正。🟡 B 方案完成。
