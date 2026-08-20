# 02. CPU 采样的双引擎: 硬件计数器与软件定时器 — perf_events 与 itimer

> 🔴 Deep | 17 KP 中的 4 个(perf_event_open/RingBuffer/CpuEngine/ITimer)
> 读者处境: `-e cpu` 走哪条路?答案取决于权限——有 perf 权限走硬件计数器,没有就走软件定时器。两条路一个目标: 以固定频率打断线程。

### 1. "perf_event_open 的一生" — 创建与环形缓冲

场景: CPU 引擎为每个线程建一个 perf 事件。

- `PerfEvents::createForThread`(src/perfEvents_linux.cpp:620-660):
  - **双路径拿 fd**: `FdTransferClient::hasPeer()` → `requestPerfFd`(:628-629,AP-1 权限桥的消费端);否则 `syscall(__NR_perf_event_open, &attr, tid, _target_cpu, -1, PERF_FLAG_FD_CLOEXEC)`(:630);旧内核无 CLOEXEC 重试(:631-636)
  - **mmap 环形缓冲**: `mmap(2*page_size, MAP_SHARED, fd)`(:652-657)→ `perf_event_mmap_page` 头(:556,RingBuffer 类 :532)
  - **失败处理**: `isResourceLimit(err)`(EMFILE/ENOMEM)→ **紧急停机**(:646-651)——fd 耗尽时不能带病运行
- [Linux: perf_event_open 按 (tid, cpu, attr) 创建事件;mmap 头是内核与用户态共享的环形缓冲控制块(PERF_RECORD 事件流从中读)]

关键设计: **每线程一个 fd**: CPU 采样要精确到"这个线程此刻的栈",所以每线程独立 perf 事件 + 独立环形缓冲——代价是 fd 数量(1000 线程 = 1000 fd),资源限制触发紧急停机是"宁可停不要错"的哲学。

### 2. "CpuEngine: 引擎的骨架" — 线程事件集成

场景: 线程启动/结束,引擎怎么跟随?

- `CpuEngine::onThreadStart/onThreadEnd`(src/cpuEngine.cpp:26-40): 由 Profiler 的 JVMTI ThreadStart/End 回调触发——`_current` 指针判当前引擎 → `createForThread/destroyForThread`
- `_current` 指针(`enableThreadEvents`/`disableThreadEvents`,:42-52)——**引擎激活状态**: 未激活时线程事件不建 fd
- `createForAllThreads`(:54-): 启动时对既有线程批量建 fd
- [C++: 无锁读 `_current`(loadAcquire/storeRelease)——线程事件回调与引擎启停的并发安全]

关键设计: **引擎与线程生命周期绑定**: 线程是采样的"上下文",引擎必须在线程出现时就位——JVMTI 回调 + pthread hook(hooks.cpp:72-110)双通道覆盖 Java 与 native 线程。

### 3. "软件定时器兜底" — ITimer 与 ctimer

场景: 容器里没权限 perf_event_open——CPU 采样还能用吗?

- `ITimer : public CpuEngine`(itimer.h:12)——**软件定时器是 CpuEngine 的子类**: 同样走 CpuEngine 的线程管理,只是信号源不同
- `ITimer::start`(itimer.cpp:13-45): `setitimer(ITIMER_PROF, ...)`(:36)——内核按 **进程 CPU 时间** 发 SIGPROF;`stop` 清零(:45)
- OpenJ9 差异: SIGPROF 处理器不同(:23-24,`signalHandlerJ9`)
- `ctimer`(ctimer_linux.cpp): clock_gettime 定时器变体
- [Linux: ITIMER_PROF 按"进程消耗的 CPU 时间"触发(与墙钟无关);精度 ~10ms 级,低于 perf 的硬件计数器但无权限门槛]

关键设计: **权限分级降级链**: perf_events(硬件,精确)→ itimer(软件,CPU 时间)→ ctimer(时钟)——容器无 perf 权限时自动/手动降级(AP-0 篇 4 的 itimer 建议在此有源码依据)。`-e cpu` 在无权限环境实际走 itimer,这是"一个事件两种引擎"的透明设计。

---

跨域桥: fdtransfer 权限桥 = AP-1 篇 3;事件选择与降级 = AP-0 篇 2/4;引擎启停的 enableEvents = 上一篇(信号看门人);ASGCT 栈采集 = 上一篇 §2。

**OpenJDK 关联**: [域 32 JFR — outlines/32-jfr/] — JFR 的采样线程框架与 Engine 抽象的对照。
