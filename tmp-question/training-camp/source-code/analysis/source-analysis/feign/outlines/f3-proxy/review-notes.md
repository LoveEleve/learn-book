# F-3 review-notes — 六层深审记录 (2026-08-15)

## 审法: 本审源码精读 + 极简复现 harness

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "必须接口校验" — ReflectiveFeign.java:150-152 TargetSpecificationVerifier | 通过 ✅ |
| 2 | 事实 | §1 "default → DefaultMethodHandler / isIgnored → 抛错 handler" — ParseHandlersByName L153-159, L170-174 | 通过 ✅ |
| 3 | 事实 | §1 "FeignInvocationHandler 内部类 + Proxy.newProxyInstance" — L75-119, L64 | 通过 ✅ |
| 4 | 事实 | §2 "invoke: retryer.clone() (L69) → runWithRetry" — SynchronousMethodHandler.java:49-75 | 通过 ✅ |
| 5 | 事实 | §2 "MI 链末端 runWithRetry" — L59-65 | 通过 ✅ |
| 6 | 事实 | §2 "executeAndDecode: targetRequest → client.execute → handleResponse" — L103-121 | 通过 ✅ |
| 7 | 事实 | §3 "Retryer 100ms/×1.5/1s/5 次" — DefaultRetryer.java:28-30, 77-80 | 通过 ✅ |
| 8 | 事实 | §4 "logAndRebuffer → executionChain.next" — ResponseHandler.java:65-88 (L68, L69-78) | 通过 ✅ |
| 9 | 事实 | §4 "InvocationContext 2xx/404-dismiss→Decoder, 否则 ErrorDecoder, void→null" — L76-86 | 通过 ✅ |
| 10 | 事实 | §5 "methodInfoResolver.resolve L319 / isAsyncReturnType L72 / underlyingReturnType L232" — AsynchronousMethodHandler | 通过 ✅ |
| 11 | 事实 | §6 "Client.execute + DefaultClient JDK 实现" — Feign.java:99 | 通过 ✅ |
| 12 | 事实 | §6 "IOException → FeignException.errorExecuting → RetryableException" — FeignException.java:302-311 | 通过 ✅ |
| 13 | 事实 | §7 "Target.apply 绑定主机在 targetRequest 末尾" — SynchronousMethodHandler.java:150-152 | 通过 ✅ |
| 14 | 数字 | 锚点密度 ≥8 达标 (19); 异步族总行数 763 实证 | 通过 ✅ |
| 15 | 结构 | 负面空间 "不做熔断 (hystrix 独立模块)" — 13.x 确实无内置熔断 (hystrix 模块存在) | 通过 ✅ |
| 16 | 过程 | **代际确认**: 13.x 无 requestLine()/build(); MethodHandler 为接口 + Factory (Feign.java:228-239) | ✅ 大纲按 13.x 撰写 |

**结论**: 16 项核对 0 修正。harness 验证代理装配/执行链/重试/裁决语义。

## harness 设计 (MiniFeign — 代理+执行链极简复现)

- A. 方法→handler 映射: 代理分发 O(1)
- B. 执行链: invoke → 模板 → Request → "HTTP" → Response
- C. 重试: RetryableException 触发, clone 独立计数
- D. 裁决: 2xx→decode / 404 默认→error / dismiss404→空值
- E. 拦截器链: MI 包住重试全程 + RI 发送前变异

---

## 8-16 补充 REVIEW (深化整合 — 独有发现并入)

> 背景: 8-16 深化审查的独有新增发现 (并入大纲 §1), 与 8-15 权威版融合。

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 17 | 事实新增 | **verify 四检查** (ReflectiveFeign.java:173-203): ① 接口 ② 同步方法任意返回类型跳过 (非 CF continue L183-185) ③ **CF 子类拒绝** (必须精确 CompletableFuture, L187-189) ④ CF 非参数化/Wildcard 拒绝 (L192-201) | 并入 §1 ✅ |
| 18 | 事实新增 | **FeignInvocationHandler.equals 语义**: Proxy.getInvocationHandler 取对端 (L89-91) → 非代理/异常 false (L92-93) → target.equals (L111-117); HardCodedTarget 比较 type+name+url (Target.java:109-114, hashCode 17×31) | 并入 §1 ✅ |
| 19 | 事实新增 | **default 方法 MethodHandle 直调**: unreflectSpecial + bindTo(proxy) (DefaultMethodHandler.java:45,128-133) — JDK 代理会拦截 default 方法, 只能 MethodHandle 绕开 InvocationHandler; 三路 Lookup (safeReadLookup/androidLookup/legacyReadLookup L54-62) | 并入 §1 ✅ |
| 20 | 验证 | 8-15 §1 "default → DefaultMethodHandler" 与 MethodHandle 细节互补 (8-15 只写结果, 补充机制) | 通过 ✅ |
