## Loop Note: Q3 — await/checkDeadLock 阻塞机制

**Hypothesis**: await 不只是 Object.wait() — 有防死锁检查 (checkDeadLock) 和 waiters 计数优化。如果 EventLoop 线程调用 await 自己未完成的 Promise → 死锁 → BlockingOperationException。

**Verification**:
- `DefaultPromise.java:253-274` — `await()`: 已 done → return; 否则 checkDeadLock → synchronized while(!isDone) { incWaiters → wait() → decWaiters }
- `DefaultPromise.java:278-303` — `awaitUninterruptibly()`: 同 await 但捕获 InterruptedException → 存储标志 → finally 恢复中断状态
- `DefaultPromise.java:474-479` — `checkDeadLock()`: `if (executor().inEventLoop()) throw new BlockingOperationException` — 防止死锁
- `DefaultPromise.java:77` — `private short waiters` — Object.wait() 的线程计数
- `DefaultPromise.java:661-666` — `checkNotifyWaiters()`: `if (waiters > 0) notifyAll()` — 完成时唤醒所有阻塞线程
- `DefaultPromise.java:349-365` — `get()` (JDK Future 接口): 调用 await() → 检查 result → 返回或抛异常

**Code type**: Implementation

**设计意图**: checkDeadLock 是整个系统的安全阀——EventLoop 的单线程模型如果允许 await，线程会永久阻塞。没有这个检查，一个 Handler 中误写的 `channelFuture.await()` 会让整个 EventLoop 卡死。checkDeadLock 用最小值防最大的错。

**结论**: await = Object.wait() 的 Promise 封装 + checkDeadLock 安全阀 + waiters 计数优化。sync() 在 await 基础上增加异常抛出（失败时扔 cause）。source: DefaultPromise.java:77,253-274,278-303,474-479,661-666
