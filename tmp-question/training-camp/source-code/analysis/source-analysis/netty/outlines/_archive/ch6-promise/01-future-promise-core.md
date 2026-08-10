# Promise/Future — 异步结果的读与写

## 概念依赖链

```
Q1 (Future/Promise 分离) → Q2 (通知机制) → Q5 (异常传播)

Q1 定义"为什么分读和写" → Q2 回答"完成时怎么通知" → Q5 回答"失败了怎么传到调用方"
```

## 叙事顺序

1. **问题引入** — 从 Ch5 EventLoop 过渡
   - Ch5 结尾：EventLoop 驱动整个数据流 — 但异步操作的返回值怎么拿到？
   - JDK Future: `get()` 阻塞 — 但这违背了 EventLoop 的单线程模型

2. **Q1：Future/Promise — 读写分离防止误操作**
   - `Future<V>` (`Future.java:26`) — 只读接口: `isSuccess()`, `cause()`, `addListener()`, `await()`, `sync()` — 给调用方
   - `Promise<V>` (`Promise.java:21`) — 可写接口: `setSuccess()`, `setFailure()`, `trySuccess()`, `tryFailure()`, `setUncancellable()` — 给 EventLoop
   - `DefaultPromise<V>` (`DefaultPromise.java:37`) — 同时实现两个接口
   - **CAS 完成** (`setValue0`，`DefaultPromise.java:646-655`): `RESULT_UPDATER.compareAndSet(this, null, objResult)` — null→result 或 UNCANCELLABLE→result
   - `volatile Object result` (`DefaultPromise.java:63`): null=incomplete → SUCCESS/UNCANCELLABLE/CauseHolder/CancellationException
   - Channel 拿 Future (只读)，EventLoop 拿 Promise (可写) — 防止 Channel 意外修改异步结果

3. **Q2：通知机制 — 完成时通知 + 三层防护**
   - `setValue0` CAS 成功 → `checkNotifyWaiters()` + `notifyListeners()`
   - **栈溢出保护** (`DefaultPromise.java:498-520`): stackDepth < `MAX_LISTENER_STACK_DEPTH` (8，`DefaultPromise.java:52-53`) → 直接调用；超过 → `safeExecute(executor, notifyListenersNow)` 提交到 EventLoop
   - **单监听器优化** (`DefaultPromise.java:72-73`): `listener` 字段存第一个，≥2 个升级为 `DefaultFutureListeners` 数组
   - **立即通知**: `addListener()` — 如果已完成 → 立即调用 `notifyListeners()`
   - **双层唤醒**: `checkNotifyWaiters()` → `waiters>0` → `notifyAll()` 唤醒 await 等待者 + 返回 whether listeners exist

4. **Q5：异常传播 — 失败如何到达调用方**
   - `setFailure(cause)` → `CauseHolder` 包装 → `cause()` 返回
   - `GenericFutureListener.operationComplete(future)` — 检查 `future.isSuccess()` / `future.cause()`
   - `get()` (JDK Future) → `await()` → result 检查 → `ExecutionException(cause)`

5. **收束**: 没有 Promise → Channel 无法安全获得结果。没有 Future → 调用方须用 get() 阻塞 → 违背 EventLoop

## 核心悬念

**"Netty 的 Future 为什么不是 JDK Future 的简单包装 — 而是读写分离的 Promise 系统？"**

因为 EventLoop 单线程模型不能阻塞。Promise.setSuccess + addListener 是非阻塞异步完成通知。stack overflow 保护防止递归监听器链爆栈，checkDeadLock 防止 EventLoop 线程阻塞自己。
