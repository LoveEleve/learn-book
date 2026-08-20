# 闭环笔记 Q1 — NameResolver 生命周期: start 即解析, 错误后 Listener 负责重试

假设: NameResolver 是"一次性启动 + 多次刷新"的拉取模型: start 立即解析, refresh 重解析, Listener 收结果/错误。

验证过程:
- **DnsNameResolver.start** (DnsNameResolver.java:199-204): `checkState(this.listener == null, "already started")` (L200, 一次性) → executor 池取 (L201) → 记录 listener (L202) → **立即 resolve()** (L203) — start 即触发首轮解析
- **refresh** (L207-211): `checkState(listener != null, "not started")` (L208) → resolve()
- **API 面** (NameResolver.java): start(Listener) L87 / start(Listener2) L123 / **refresh() 默认空实现** (L146) / shutdown 抽象 (L132)
- **Listener2** (L256-305): onResult(ResolutionResult) 抽象 (L294); **onResult2 只能从 syncContext 调用** (L271-273 注释: "Calling onResult and not onResult2 because onResult2 can only be called from a synchronization context" — G-3 syncContext 约束)
- **onError** (L303): "The listener is responsible for eventually invoking refresh() to re-attempt resolution" — **错误后谁重试?Listener 负责**

代码类型: Interface (拉取契约)

结论: 解析是**拉取模型**: start 触发首轮 → 结果/错误经 Listener 回调 (syncContext 约束) → 错误后由**通道侧 (Listener 实现)** 决定何时 refresh (G-6 退避调度 → RetryingNameResolver); 解析器自己无定时重试。**被放弃的方案: 解析器内部自动重试** — 重试节奏 (退避/时机) 是策略, 交由上层; 解析器保持"被动服务"职责单一。 [跨域: G-3 exitIdleMode 触发 start/syncContext; G-6 退避→RetryingNameResolver] [模式: 拉取+回调] (DnsNameResolver.java:199-211; NameResolver.java:87-146,256-305)
