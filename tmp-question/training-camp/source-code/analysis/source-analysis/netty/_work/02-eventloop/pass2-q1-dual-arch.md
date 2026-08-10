## Loop Note: Q1 — 双轨架构动机

**Hypothesis**: Netty 4.2 将 IoHandler 从 EventLoop 中拆分是为了两件事：(1) 虚拟线程(Project Loom) 环境下 EventLoop 可挂起释放 carrier thread 但 IoHandler 仍需管理操作系统 I/O，(2) 不同的 I/O 实现 (NIO/Epoll/KQueue) 共享同一套任务调度。

**Verification** (grep against source):
- `IoHandler.java:20-33` — Javadoc: "Handles IO dispatching for a ThreadAwareExecutor" + IoHandle → IoOps → IoEvents 模型
- `IoHandlerContext.java:26` — 桥接接口: canBlock/delayNanos/deadlineNanos/reportActiveIoTime — 让 IoHandler 向 EventLoop 报告其状态
- `SingleThreadIoEventLoop.java:192-205` — `run()` 模板: initialize → runIO → runAllTasks → canSuspend。此方法被 `NioEventLoop` **继承而不重写**——`NioEventLoop.java` 中 grep "void run" 返回 0 个 override。核心循环对 NIO/Epoll/KQueue 完全不变——这是分离成功的终极证明：不同 I/O 实现共享完全相同的调度循环，IoHandler 可变但循环体不变
- `IoHandlerContext.java:26-83` — **分离不等于孤立的关键桥接**：
  - `canBlock()` → `!hasTasks() && !hasScheduledTasks()` — 查 EventLoop 任务队列决定是否阻塞
  - `delayNanos(t)` → delegate to EventLoop scheduler — 提供 select 超时
  - `deadlineNanos()` → delegate to EventLoop scheduler — 最近的定时任务
  - `reportActiveIoTime(ns)` → delegate to EventLoop — I/O 利用率回传（驱动 auto-scaling）
  - `SingleThreadIoEventLoop.java:43-71` — 匿名实现，将 EventLoop 方法包装为 Context 方法。IoHandler 不直接访问 EventLoop 的任务队列，EventLoop 不直接碰 Selector——双向通信全部通过 IoHandlerContext 的四条方法
- `SingleThreadIoEventLoop.java:85-91` — 构造函数接收 `IoHandlerFactory` 而非具体 IoHandler 类型
- `IoHandlerFactory.java:23` — 工厂接口: newHandler(executor) + isChangingThreadSupported()
- `IoHandlerFactory.isChangingThreadSupported()` — 关键: 指示 IoHandler 是否支持线程切换。NioIoHandlerFactory 返回 `true`（`NioIoHandler.java:809`）——NioIoHandler 的 Selector 可以在虚拟线程 carrier 切换时被迁移

**Code type**: Interface Design (architectural separation)

**设计权衡**:
| 维度 | Netty 4.1 (单体 NioEventLoop) | Netty 4.2 (双轨) |
|------|------|------|
| 职责 | 一个类同时做 select + task scheduling | IoHandler 做 IO, EventLoop 做调度 |
| 扩展性 | 新 I/O 实现需继承 NioEventLoop | 实现 IoHandler 即可（EpollIoHandler, KQueueIoHandler） |
| 虚拟线程 | EventLoop 线程无法挂起（阻塞在 select 中） | canSuspend() 允许 EventLoop 挂起释放 carrier thread + IoHandler 支持线程切换 (isChangingThreadSupported=true) |
| 测试性 | 需要真实 Selector | ManualIoEventLoop 绕过真实 I/O |

**Conclusion**: 双轨不是"重构"——是应对 Project Loom 的架构变更。IoHandler 持 selector/执行 select 是操作系统 I/O 通道，EventLoop 做任务调度/生命周期是 JVM 线程管理的通道。分离后 EventLoop 可以在虚拟线程下挂起而不阻塞 selector。source: IoHandler.java:20-33, SingleThreadIoEventLoop.java:85-205, IoHandlerFactory.java:23
