# EventLoopGroup 与线程生命周期

## 概念依赖链

```
Q6 (Group 分配) → Q7 (调度策略) → Q8 (线程挂起)

Q6 定义"Channel 被哪个线程处理" → Q7 管理"I/O 和任务的时间分配" → Q8 处理"什么时候可以休息"
```

## 叙事顺序

1. **问题引入** — 从篇二过渡
   - 篇二的 Selector 三层防护在单个 EventLoop 内部工作——但生产线上的服务器有数百个 EventLoop
   - Channel 怎么被分配到具体的 EventLoop？谁统一管理整个线程池？

2. **Q6：EventLoopGroup — 线程池 + Channel 分配**
   - `NioEventLoopGroup` 继承 `MultiThreadIoEventLoopGroup` → `MultithreadEventLoopGroup`
   - 默认线程数：`availableProcessors() * 2`（`MultithreadEventLoopGroup.java:40-41`），可用 `io.netty.eventLoopThreads` 覆盖
   - **与 Arena 对齐**：线程数 = Arena 数 = cores×2 → 新 Channel 分配到 EventLoop N 时，其 ByteBuf 也被 Arena N 处理 — 零跨 Arena 锁竞争
   - `next()` 分配：`EventExecutorChooserFactory` → `isPowerOfTwo` 判断 → Power-of-2 位运算 `idx & (length-1)`（`DefaultEventExecutorChooserFactory.java:32-53`）

3. **过渡：每个 EventLoop 上 — I/O 和任务谁先用 CPU？**

4. **Q7：ioRatio → maxTaskProcessingQuantumNs — 4.2 的时间预算革命**
   - 4.1 时代：`setIoRatio(50)` — "至少 50% CPU 给 I/O"
   - 4.2 现实：**ioRatio 完全废弃** — `NioEventLoop.getIoRatio()` 总是 0，`setIoRatio()` no-op（`NioEventLoop.java:143-155`）
   - 替代机制：`maxTaskProcessingQuantumNs`（`SingleThreadIoEventLoop.java:39-41`）— 任务最多跑 1 秒，然后回去做 I/O
   - 循环模型：`runIo()` 完整一轮 I/O → `runAllTasks(maxQuantumNs)` 任务最多 N 纳秒 → 循环
   - 为什么废弃比例模型？IoHandler/EventLoop 分离后 I/O 时间的自然边界由 `IoHandlerContext.canBlock()` + `reportActiveIoTime()` 决定，不需要人工比例

5. **过渡：EventLoop 没任何 Channel 注册时 — 能不能不空转 CPU？**

6. **Q8：canSuspend — 事件循环的休眠按钮**
   - `SingleThreadIoEventLoop.run()` 结束条件：`!canSuspend()`（`SingleThreadIoEventLoop.java:204`）
   - `canSuspend(int state)`：`super.canSuspend(state) && numRegistrations.get() == 0`（`SingleThreadIoEventLoop.java:212-215`）
   - **虚拟线程联动**：canSuspend=true → EventLoop 释放 carrier → JVM 把 carrier 派给其他虚拟线程
   - 为什么 numRegistrations==0？有活跃 Channel → Selector 可能在 select 中等待 → 不能挂起
   - NioIoHandler 的 `isChangingThreadSupported=true`（`NioIoHandler.java:809`）→ 恢复时 Selector 可安全迁移到新 carrier

7. **收束：EventLoop 生命周期全景**
   ```
   创建:   NioEventLoopGroup.next() → 分配 Channel 到 EventLoop (位运算)
   运行:   runIo() → runAllTasks(maxQuantumNs) → loop
   空闲:   numRegistrations==0 → canSuspend=true → 释放 carrier thread
   恢复:   新 Channel 注册 → execute() → canSuspend=false → 线程恢复
   ```
   EventLoop 驱动任务、任务产生结果 — 异步结果怎么传递到调用方？引出 Ch6 Promise/Future。

## 核心悬念

**"Netty 4.2 的 EventLoop 不仅管理线程——它知道什么时候该休眠释放 CPU，这是为虚拟线程和云计算按需计费时代做的准备。"**
