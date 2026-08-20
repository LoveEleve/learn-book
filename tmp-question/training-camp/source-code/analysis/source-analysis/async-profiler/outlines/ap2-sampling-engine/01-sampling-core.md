# 01. 信号响起的一瞬间 — 采样主路径

> 🔴 Deep | 17 KP 中的 4 个(recordSample/ASGCT/JVMTI 回退/信号注册)
> 读者处境: perf 计数器触发信号——从信号到一条采样记录,这中间发生了什么?核心约束: 信号处理器里**什么都不能调**(不能 malloc、不能拿锁、不能碰 JVM 内部)。

### 1. "一次采样的完整旅程" — recordSample

场景: SIGPROF(或 perf 信号)到达,recordSample 被调。

- `Profiler::recordSample(void* ucontext, u64 counter, EventType, Event*)`(src/profiler.cpp:409):
  1. `atomicInc(_total_samples)`(:410)
  2. **节流**: `RateLimit::allow(event_type)`(:411)——预算不足直接丢弃
  3. **并发防护**: `tryLock(tid)`(:411-415)——**CONCURRENCY_LEVEL 三级锁**(:185-191: 3 个锁槽按线程 id 轮转 tryLock)——"Too many events or too many concurrent signals"(:409 注释);丢弃时 **PERF_SAMPLE 必须 `PerfEvents::resetBuffer(tid)`**(:420-424,否则环形缓冲残留陈旧采样)
  4. 栈行走计时(:416-418)→ `recordExternalSample`/`recordEventOnly` 记录
- [Linux: 信号处理器执行在**被打断线程的上下文**——栈行走必须用当前 ucontext 的寄存器,不能切换线程]

关键设计: **信号处理器三禁令**: 不分配(线性分配器 AP-4)/不拿锁(3 槽 tryLock)/不调 JVM 非安全 API(ASGCT 例外,见 §2)。违反任一条 = 死锁或崩溃——这是采样器正确性的地基。

### 2. "信号里怎么拿 Java 栈" — ASGCT 与 JVMTI 回退

场景: 采样点要有 Java 调用栈——但 `GetStackTrace` 在信号处理器里不安全。

- `getJavaTraceAsync`(profiler.cpp:356): 
  - JDK-8132510 工作区(:351-354)——JDK 9 起信号内 `GetEnv()` 不安全,只对已注册线程用
  - `VMThread::current()`(:360)判 Java 线程 → `VM::_asyncGetCallTrace`(**ASGCT**,:381,HotSpot 的内部安全 API)
  - `JitWriteProtection`(:379)——采样期间防 JIT 写代码(栈行走与 JIT 编译竞争)
  - 失败映射错误帧 `BCI_ERROR`(:391-394,如 `GC_active`/`unknown_stub`)
- `getJavaTraceJvmti`(profiler.cpp:397): `GetStackTrace` → 转 ASGCT 格式——**非 Java 线程/AGCT 不可用时的兜底**
- [JVMTI: AsyncGetCallTrace 是 HotSpot 的非官方 API(VMStructs 里找符号)——唯一能在信号处理器安全拿 Java 栈的路径]

关键设计: **双层栈采集**: ASGCT(快、信号安全、主路径)+ JVMTI GetStackTrace(慢、非信号场景、回退)——错误帧编码让用户看到"GC 活跃/未知桩"而非空白。

### 3. "信号的看门人" — 信号注册与引擎联动

场景: 哪些信号是采样器的?怎么防冲突?

- `setupSignalHandlers`(profiler.cpp:687): SIGTRAP(AllocTracer 用,:689)/SIGSEGV+SIGBUS(crash 处理器替换,**记录自身库边界**判定 crash 是否在 profiler 代码里 :700-704)/WAKEUP_SIGNAL(:683-685 wakeup)
- `trapHandler`(:643-663): 采样陷阱命中时 `_engine->enableEvents(true/nostop)`——**陷阱期间暂停引擎**,避免信号风暴
- `noop_engine`(profiler.cpp:56): 无引擎时的空操作——未启动时信号安全降级

关键设计: **信号是有限资源**: SIGTRAP/SIGSEGV/WAKEUP 各有主(alloc/崩溃恢复/唤醒),引擎信号(SIGPROF 等)由 Engine 管理——冲突检测(prev_handler 比对,itimer.cpp:19-24)+ 库边界判定,构成"信号安全网"。

---

跨域桥: 引擎怎么触发 recordSample = 下一篇(perf/itimer 的信号来源);RateLimit = AP-2 篇 4;栈行走本身 = AP-4(stackWalker);错误帧映射 = AP-4(frameName)。

**OpenJDK 关联**: [域 18 Safepoint — outlines/18-safepoint/] — 信号内采样与 safepoint 的一致性/偏向问题(safepoint bias)。
