# 04. 锁等待与墙钟阻塞 — lock/wall 事件与节流

> 🟡 Working | 17 KP 中的 5 个(lockTracer/wallClock/processSampler/rateLimit/threadFilter)
> 读者处境: CPU 采样回答"谁在烧",锁采样回答"谁在等",墙钟回答"谁在睡"——三张图合起来才是全貌。

### 1. "测量'等待'而不是'占用'" — lockTracer

场景: `-e lock` 火焰图——为什么能看到"等锁花了多久"?

- `lockTracer.cpp`:
  - `pthread_key_t lock_tracer_tls`(:18)——**线程本地存锁等待时间**(64 位平台注释: "store lock time in a pthread local")
  - `_interval` 用 `TSC::frequency()` 换算(:41)——纳秒↔TSC tick
  - `_parkBlocker` 反射读 `java.lang.Thread.parkBlocker`(:114)——**识别被阻塞的锁对象**
- 机制: 锁竞争发生时(monitor enter 失败/Unsafe.park),记录等待开始;锁到手,记录等待时长 → 按锁聚合
- 与 Arthas 对照: Arthas `thread -b` 是"数人头"(锁争用热力,AR-3 篇 3),async-profiler `-e lock` 是"测等待时长"——**一个看热度,一个看代价**
- [JVM: 锁等待是 HotSpot 的同步子系统状态(Synchronizer 篇,OpenJDK 域 19)——async-profiler 通过字节码插桩 + parkBlocker 反射捕获]

关键设计: **采样还是事件?**: [C++: 锁等待事件在字节码插桩点直接捕获(方法进入/返回处注入的探针),不经信号路径——事件驱动与信号采样的机制分叉]锁采样是**事件驱动**(每次等待都记),不是周期性采样——因为锁等待是"低频高价值",事件捕获比采样精确。这解释了为什么 lock 事件走 instrument 插桩而非 perf。

### 2. "墙钟: 阻塞在哪" — wallClock

场景: 接口慢,CPU 不高——线程在 IO/睡眠,CPU 采样看不见。

- `wallClock.cpp:81-136`: `ThreadSleepMap`(:104-105)——**线程睡眠状态表**(Mutex 保护);采样时区分"运行中"与"睡眠中"
- 记录 `_start_time = TSC::ticks()`(:136)——等待起点
- 输出: 阻塞时长火焰图(`-e wall`),能看到 `Thread.sleep`/`socketRead` 等
- 与 CPU 采样的差异: CPU 采样"谁占用 CPU",wall 采样"谁占着时间"——两个视角

关键设计: **状态感知采样**: wallClock 不只打断线程,还**知道线程在干嘛**(睡眠表)——采到的栈配上"状态"信息,才能区分"忙"与"等"。这是"采样 + 状态"的组合设计。

### 3. "防风暴与收尾" — rateLimit / processSampler

场景: 采样事件太多(如高频分配),信号风暴打垮应用。

- `rateLimit.cpp:15-34`: **预算+结转**: `_budget[i].budget = limit`(:16)——每周期预算,没用完的结转(`carryover` :33-34)——**平滑限流**(不是硬截断)
- 消费方: `Profiler::recordSample` 第一道关卡(:411 `RateLimit::allow`)
- `processSampler.cpp:13-54`: 进程级采样(`_process_history` :13/:28)——`-e proc`,采样系统进程(非 JVM 线程)
- `threadFilter.cpp`: `--threads` 过滤——只采样指定线程

关键设计: **限流是"平滑"不是"截断"**: 预算+结转让高频事件被摊平而非突然消失——采样统计的连续性优先(硬截断会制造统计假象)。这是采样器"统计学正确性"的细节。

---

跨域桥: parkBlocker 的锁语义 = OpenJDK 域 19 Synchronization;字节码插桩 = AP-3;TSC 时钟 = AP-4(tsc.cpp);限流消费方 = AP-2 篇 1(recordSample 第一关)。

**OpenJDK 关联**: [域 19 Synchronization — outlines/19-synchronization/] — MonitorContendedEnter 事件背后的 ObjectMonitor 状态机。
