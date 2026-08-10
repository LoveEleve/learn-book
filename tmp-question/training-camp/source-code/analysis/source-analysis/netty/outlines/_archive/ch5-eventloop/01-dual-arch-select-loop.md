# EventLoop 核心 — 双轨架构与 select 循环

## 概念依赖链

```
Q1 (双轨架构) → Q2 (select 决策) → Q3 (select 循环体) → Q10 (事件分发)

Q1 定义架构骨架 → Q2 回答"阻塞还是跳过" → Q3 深入循环体细节 → Q10 把就绪事件送到目的地
```

## 叙事顺序

1. **问题引入** — 从 Ch4 ByteBuf 过渡
   - 上卷三章讲了 NIO 如何读写数据，下卷 Ch4 讲了 Netty 怎么重新发明缓冲区
   - 数据载体 ByteBuf 已经准备好在 Pipeline 中传递——但谁来驱动整个数据流？什么时候读、什么时候写？
   - Netty 4.1: NioEventLoop 一个类装下全部。**4.2 为什么拆开？** Project Loom — 虚拟线程要释放 carrier，但 `selector.select()` OS 层阻塞

2. **Q1：双轨架构 — IoHandler 管 I/O，EventLoop 管线程**
   - IoHandler 接口（`IoHandler.java:33`）— `initialize() → run(context) → destroy()` + `register(IoHandle)` + `wakeup()`
   - SingleThreadIoEventLoop（`SingleThreadIoEventLoop.java:36`）— 持有 IoHandler + 主循环体
   - **关键桥接：IoHandlerContext**（`IoHandlerContext.java:26-83`）— `canBlock()`/`delayNanos()`/`deadlineNanos()`/`reportActiveIoTime()` — IoHandler 通过这四条方法查询 EventLoop 的状态，不直接碰任务队列或 Selector
   - **分离成功的证据：NioEventLoop 不重写 `run()`** — `NioEventLoop.java` 中 `grep "void run"` 返回 0 个 override。核心循环对 NIO/Epoll/KQueue 完全不变
   - 构造函数通过 `IoHandlerFactory` 接收 IoHandler（`SingleThreadIoEventLoop.java:85-91`），`isChangingThreadSupported=true`（`NioIoHandler.java:809`）

3. **过渡：run() — 什么时候阻塞 select，什么时候跳过？**

4. **Q2：SelectStrategy — 任务优先的检查哨**
   - `DefaultSelectStrategy.calculateStrategy(selectSupplier, hasTasks)`（`DefaultSelectStrategy.java:29-31`）
   - `hasTasks=true` → `selectNow()` — 非阻塞轮询，0 returns CONTINUE，否则返回就绪数
   - `hasTasks=false` → SELECT — 阻塞等待
   - 三种策略：CONTINUE(-2) / SELECT(-1) / BUSY_WAIT(-3)（`SelectStrategy.java:31-39`）

5. **过渡：select 循环跑起来了 — JDK epoll 有个著名 bug**

6. **Q3：select 循环体 — epoll bug 的自救**
   - `NioIoHandler.select()`（`NioIoHandler.java:630-709`）— 完整循环体
   - `selectCnt = 0` 开始，每次 `selector.select(timeoutMillis)` → `selectCnt++`
   - 返回后检查 4 条件（selectedKeys≠0 / oldWakenUp / wakenUp / hasTasks）— 任意为真则 break
   - 全 false → 假唤醒 → continue
   - **time-elapsed 双重区分**（`NioIoHandler.java:693-704`）：超时真的过了 → normal timeout → selectCnt=1；时间不够 → epoll bug → selectCnt 续加 → ≥512 → `selectRebuildSelector()` 重建 Selector
   - JDK-6427854 + Netty #203

7. **过渡：select 跑完了 — 一组 SelectionKey。怎么告诉 Channel？**

8. **Q10：事件分发 — select → Channel**
   - `processSelectedKey(SelectionKey k)`（`NioIoHandler.java:586`）
   - `k.attachment()` = **`DefaultNioRegistration`**（`NioIoHandler.java:318`）— 不是 Channel，不是 IoHandle
   - `DefaultNioRegistration.handle(int ready)`（`NioIoHandler.java:384-389`）— `NioIoOps.eventOf(ready)` 将 readyOps 位掩码转 IoEvents → `IoHandle.handle(registration, ioEvent)`
   - 分发链：`select → selectedKeys → processSelectedKey → key.attachment()=DefaultNioRegistration → readyOps→IoEvent 转换 → IoHandle.handle() → Channel 级业务逻辑`

9. **收束：完整的 select 循环**
   ```
   run():
     initialize()
     do:
       select(strategy, wakenUp)      ← Q2+Q3
       processSelectedKeys()          ← Q4+Q10
       runAllTasks(maxQuantumNs)      ← Q7 (第三篇)
     while (!confirmShutdown && !canSuspend)
   ```

## 核心悬念

**"Netty 4.2 的事件循环为什么不只是一个 select() 的包装——而是一套完整的 IoHandler/EventLoop 双轨架构？"**

因为 Project Loom 改写了规则。虚拟线程要释放 carrier，但 selector.select() 是 OS 层阻塞。IoHandler 封装 I/O + 线程切换，EventLoop 用 canSuspend 管理线程，IoHandlerContext 桥接两端——三者协同才能让 I/O 和线程各自独立演化。select 循环内的 selectCnt 自愈机制进一步证明 Netty 把正确性看得比优雅重要。
