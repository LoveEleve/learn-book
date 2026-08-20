# 闭环笔记 Q6 — Deadline: 单调时钟 + 调度器取消

假设: Deadline 用单调时钟 (nanoTime) 而非墙上时钟 — 免疫 NTP 调整; 超时经调度器定时取消流。

验证过程:
- **单调时钟**: `Ticker SYSTEM_TICKER = new SystemTicker()` (Deadline.java:38) — System.nanoTime 底座; **MAX_OFFSET ±100 年防环绕** (L40-43, 注释: "nanoTime has a range of just under 300 years")
- **相对偏移**: `Deadline.after(duration, units)` (L69-70) — 注释 (L31-35): "Many systems use timeouts, which are relative to the start of the operation. However, a timeout can be converted to a Deadline at the start of the operation" — 计时起点固定, 不再受调用链中间延迟影响
- **调度取消** (ClientCallImpl.java:337-385): CancellationHandler — remainingNanos (L345-348) → `deadlineCancellationExecutor.schedule(new LogExceptionRunnable(this), remainingNanos)` (L361-362) → run → `stream.cancel(formatDeadlineExceededStatus())` (L385) — **超时→流取消**
- **start 时过期检查** (L248-260): `deadlineExceeded = ... remainingNanos <= 0` → "ClientCall started after %s deadline was exceeded %.9f seconds ago" — 已过期调用立即失败 (不建流)
- 传播: CallOptions.withDeadlineAfter → Context withDeadline → 服务端 grpc-timeout 头读取 (G-2 createContext)

代码类型: Algorithmic (时间语义 + 调度)

结论: Deadline 设计要点: ① **单调时钟** (nanoTime) — 墙上时钟回拨 (NTP) 不影响剩余时间计算; ② **偏移转绝对** — timeout 在起点换算为 deadline, 后续比较与链路延迟无关; ③ **调度器取消** — deadlineCancellationExecutor 定时 cancel, 与 Context 取消链汇合 (G-2 cancelled 里 hasDeadline+TimeoutException 分支)。**被放弃的方案: 墙上时钟 (System.currentTimeMillis)** — NTP 回拨会导致超时提前/延后; 轮询检查 — 无谓 CPU。 [跨域: G-2 服务端 grpc-timeout 对称] [并发: 定时取消竞态 (cancel(false))] (Deadline.java:38-43,69-70; ClientCallImpl.java:248-260,337-385)
