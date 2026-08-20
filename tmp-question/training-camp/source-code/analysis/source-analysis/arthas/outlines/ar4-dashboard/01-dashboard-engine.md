# 01. 一个"自己会刷新"的面板 — 定时刷新引擎

> 🔴 Deep | 12 KP 中的 2 个(Timer 引擎/生命周期)
> 读者处境: dashboard 每 5 秒自己变一次——它不是死循环 while(true),而是一个"定时任务 + 可取消"的引擎。

### 1. "为什么是 Timer 不是线程" — scheduleAtFixedRate

场景: 面板要周期性刷新,实现方式有 Thread.sleep 循环、ScheduledExecutorService、Timer——arthas 选了哪个,为什么?

- `DashboardCommand.process`(monitor200/DashboardCommand.java:76): `timer = new Timer("Timer-for-arthas-dashboard-" + sessionId, true)`(:79,**守护线程**)→ `timer.scheduleAtFixedRate(new DashboardTimerTask(process), 0, getInterval())`(:108)
- 间隔: `getInterval()`(:57 附近,`-i` 参数,默认 5000ms)
- 每次 tick:`DashboardTimerTask.run()`(:218-270)——组装一次 DashboardModel 并输出
- 次数限制: `-n numOfExecutions`,`count >= numOfExecutions` 时 `timer.cancel()` 自动停(:230-236)
- [Java: `Timer.scheduleAtFixedRate` vs `schedule`——前者按**固定速率**(补偿漂移,两次执行间隔 = 设定值,哪怕上次执行超时),后者按**固定延迟**(上次结束后等满间隔)。面板刷新要节奏稳定,选 fixedRate]

关键设计: [模式: 定时器任务(Timer+TimerTask)+ 快照组装(每 tick 全量)] **守护线程 + 独立 Timer 实例**: `new Timer(name, true)` 的 daemon=true——终端断开/进程结束时不会阻止 JVM 退出;每个会话一个 Timer(名字带 sessionId),多终端 dashboard 互不干扰(和 AR-2 的命令实例"一次性"同一哲学: 会话隔离)。

### 2. "Ctrl-C 不会泄漏" — 生命周期管理

场景: 你按 Ctrl-C 退出面板——timer 线程如果不取消会怎样?

- `DashboardInterruptHandler.handle`(monitor200/DashboardInterruptHandler.java:20-24): **先 `timer.cancel()` 再结束进程**
- `suspend()`/`resume()`(DashboardCommand.java:87-102): 会话挂起时 `timer.cancel()` + 恢复时重建(`new Timer` + scheduleAtFixedRate :119-123)——**Timer 不能复用,取消后必须新建**
- q 退出: `QExitHandler`(:105)
- [Java: `Timer.cancel()` 后 Timer 线程终止且**不可重用**——只能 new 新实例;这是 java.util.Timer 的硬限制,也解释了 suspend/resume 为何要"重建"而不是"暂停"]

关键设计: **每条路径都 cancel**: Ctrl-C/超次数/q/挂起——任何退出路径都先停定时器,防止"面板没了但刷新线程还在跑"(每 5 秒白做一次全量采样,持续吃 CPU)。这是交互式工具"退出即清理"的纪律(与 AR-1 destroy 链同哲学)。

### 3. "一次 tick 的组装" — DashboardTimerTask.run

场景: 5 秒一到,run() 里干什么?

- 每次 tick(DashboardCommand.java:218-270): 新建 `DashboardModel` → 线程表格数据(`ThreadUtil.getThreads()` :241 + `threadSampler.sample(threads)` :242)→ 内存(`MemoryCommand.memoryInfo()` :245)→ GC 聚合 → 运行时 → Tomcat → `process.appendResult(model)` 整块输出
- 输出是"整块替换"还是"追加"?——**每 tick append 一整块**,终端靠清屏(ANSI)显示成"面板"
- **布局自适应**(view/DashboardView.java:24-64): `process.width()/height()`(:24-25)取终端尺寸——高屏线程表占半屏(:33),矮屏占三分之一(:36,保底 12 行 :38);memory+runtime 格高度动态分配(:64)

关键设计: **tick 是全量快照**: 每 5 秒采样全部数据拼一块——而不是增量更新(增量要维护跨 tick 状态,复杂度爆炸)。全量快照的代价是采样开销(1000 线程 enumerate + CPU 读取),5 秒一次可接受。

---

跨域桥: `ThreadUtil.getThreads` + `threadSampler.sample` = AR-3 篇 1/2 的组件**直接复用**;appendResult/Model 输出链 = AR-2 篇 4(watch 同款);数据来源细节 = 下一篇。

---

**OpenJDK 关联**:  [OpenJDK 域 39 Runtime Monitoring — outlines/39-runtime-monitoring/] — ServiceThread+Timer 周期任务,JVM 侧的"面板";
### 核心悬念

**"定时器在转——每 5 秒拼出来的那一屏数据,各块从哪来?"** — 线程表复用 AR-3 的采样器,内存复用 memory 命令,GC 直接读 MXBean——但右下角的 QPS,来源最出人意料。

> → [02-dashboard-data.md](02-dashboard-data.md)
