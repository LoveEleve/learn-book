# 闭环笔记 Q4 — ClientCallImpl 状态机: 短路路径 + 用户异常优先级

假设: 客户端调用有"短路"路径 (Context 已取消/压缩器不存在 → 不建流直接回调失败), 用户回调异常优先于服务端状态。

验证过程:
- **startInternal** (ClientCallImpl.java:188-235): `checkState(stream == null, "Already started")` / `checkState(!cancelCalled)` (L189-190) — 启动一次性
- **Context 已取消短路** (L197-216): `context.isCancelled()` → `stream = NoopClientStream.INSTANCE` (L199, 不建真实流) → ContextRunnable 在 callExecutor 上 `closeObserver(observer, statusFromCancelled(context), ...)` (L210, 类定义 L204-211, execute L212) — **取消状态的调用零网络开销**
- **压缩器短路** (L223-231): 找不到 → NoopClientStream + INTERNAL "Unable to find compressor" 立即回调
- **Context 取消监听** (L382-391): `cancelled(Context)` → deadline 超时 → `formatDeadlineExceededStatus()` (L386); 否则 `stream.cancel(statusFromCancelled(context))` (L391) — **上下文取消→流取消传播**
- **exceptionThrown** (L588-594): 用户回调异常 → `exceptionStatus = status; stream.cancel(status)` — 注释: "we can only call onClose() when we are sure there will be no further callbacks. We set the status here and overwrite the onClose() details when it arrives" — **用户异常优先语义**
- **closeObserver** (L564-568): onClose 用户异常被捕获记 WARNING (不传播)
- 测试实证: exceptionInOnMessageTakesPrecedenceOverServer (ClientCallImplTest.java:190) / exceptionInOnHeadersHasOnCloseQueuedLast (L262)

代码类型: Implementation (调用状态机)

结论: 客户端调用是"提前失败"设计: 可预见的失败 (取消/坏压缩器) 在 start 时就短路, 零流创建; 不可预见的 (用户回调异常) 用 exceptionStatus 覆盖服务端状态 — **用户代码异常 > 服务端返回状态** 的优先级是刻意的 (用户错误不能静默)。**被放弃的方案: 用户异常直接抛给调用线程** — 异步回调场景无调用线程可抛, 只能走状态通道。与 G-2 服务端状态机对称: 服务端管"响应状态机", 客户端管"请求状态机+异常优先级"。 [跨域: G-2 对称 / G-1 STUB_TYPE_OPTION] [并发: 回调线程归属 callExecutor] (ClientCallImpl.java:188-235,382-391,564-594)
