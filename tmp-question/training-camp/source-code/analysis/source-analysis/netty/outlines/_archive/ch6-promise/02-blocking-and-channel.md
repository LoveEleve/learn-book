# Promise/Future — 阻塞等待与 Channel 集成

## 概念依赖链

```
Q3 (await/sync 阻塞) → Q4 (ChannelFuture/isVoid)

Q3 定义"同步等待怎么安全实现" → Q4 回答"Channel 的零分配优化"
```

## 叙事顺序

1. **问题引入** — 从篇一过渡
   - addListener 异步通知 — 但有时调用方需要同步等待 (如 main 线程 bind)
   - JDK Future.get() 用 `await()` 实现 — 但加了防死锁

2. **Q3：await/sync — 阻塞等待 + 防死锁**
   - `await()` (`DefaultPromise.java:253-274`): 已 done → return; 否则 `checkDeadLock()` → `synchronized while(!isDone) { incWaiters → wait() → decWaiters }`
   - **checkDeadLock** (`DefaultPromise.java:474-479`): `executor().inEventLoop()` → `throw BlockingOperationException` — 防止 EventLoop 线程自己等自己
   - `awaitUninterruptibly()` (`DefaultPromise.java:278-303`): 捕获 InterruptedException → 存标志 → finally 恢复中断
   - `sync()`: 调用 await → if failed → throw cause
   - `short waiters` (`DefaultPromise.java:77`) — Object.wait() 的线程计数
   - `setValue0` CAS → `checkNotifyWaiters()` → `if (waiters>0) notifyAll()` — 唤醒所有阻塞线程

3. **Q4：ChannelFuture — Channel 的零分配承诺**
   - `ChannelFuture` — Future + `channel()` 获得 Channel 引用
   - `ChannelPromise` — Promise + `channel()` + `isVoid()`
   - `isVoid()=true` → EventLoop.write() 返回零分配 promise — 不创建新的 DefaultChannelPromise。`DefaultChannelPipeline` 持有 `VoidChannelPromise` 实例（`DefaultChannelPipeline.java:68,94`），通过 `pipeline.voidPromise()` 暴露（`line 1070`），一次创建全程复用

4. **收束**: EventLoop 驱动 → Promise.setSuccess → notifyListeners + notifyAll → 调用方处理。checkDeadLock 保证 EventLoop 线程永不阻塞自己。await/sync 供外部线程同步使用。异步结果传递的管道已经就绪——但中间的每一步是谁在处理数据？数据在 Pipeline 中流过哪些 Handler？引出 Ch7 Pipeline+Handler。

## 核心悬念

**"EventLoop 不能阻塞，调用方为什么还需要 await/sync？"**

调用方通常不在 EventLoop 线程内。main 线程 `bind().sync()` — 需要在 bind 完成前阻塞。checkDeadLock 确保只有外部线程能这样用。Promise 的双面设计（addListener 供 EventLoop 异步使用，await 供外部线程同步使用）统一了两种使用模式。
