# 域 AP-2: 采样引擎与事件 — 知识规划

> 源码路径: src/profiler.cpp(1718行) + src/engine.cpp/h + src/cpuEngine.cpp/h + src/perfEvents_linux.cpp(995行) + src/itimer.cpp/h + src/ctimer_linux.cpp + src/wallClock.cpp/h + src/processSampler.cpp + src/allocTracer.cpp + src/objectSampler.cpp + src/mallocTracer.cpp + src/lockTracer.cpp + src/nativeLockTracer.cpp + src/hooks.cpp + src/rateLimit.cpp + src/threadFilter.cpp + src/event.h
> 源码量: ~20 文件,核心 ~6000 行
> 提取日期: 2026-08-10(v1 深读提取——范围规划声明已逐文件验证)
> 前置域: AP-1(参数解析——Engine 消费 Arguments)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| profiler.cpp:409-520 recordSample() | **采样主路径**: `RateLimit::allow(event_type)` 节流 + `tryLock(tid)` 并发信号防护(:411-415,失败计数+PERF_SAMPLE 时 `PerfEvents::resetBuffer` 重置环形缓冲 :420-424)→ 栈行走计时 → `recordExternalSample/recordEventOnly` 记录 | High |
| profiler.cpp:356-396 getJavaTraceAsync() | **信号内异步栈采集**: JDK-8132510 工作区(:351-354,信号处理器内不能安全 GetEnv)→ `VMThread::current()` 判定 Java 线程 → `VM::_asyncGetCallTrace`(ASGCT,:381)→ 失败映射错误帧(:391-394);`JitWriteProtection` 保护 JIT 写 | High |
| profiler.cpp:397-408 getJavaTraceJvmti() | **JVMTI 回退**: `GetStackTrace` 转 ASGCT 格式——非 Java 线程/AGCT 失败的兜底 | High |
| profiler.cpp:687-710 setupSignalHandlers() | **信号注册**: SIGTRAP(AllocTracer.trapHandler)/SIGSEGV/SIGBUS crash 处理器替换(记录**自身库边界**供 crash 判定 :700-704)/WAKEUP_SIGNAL(wakeupHandler :683-685) | High |
| profiler.cpp:56 noop_engine + 626-640 enableEvents | **引擎启停**: `_engine->enableEvents(true/nostop)`——trapHandler(:643-663)在采样陷阱命中时临时启停引擎 | High |
| perfEvents_linux.cpp:620-660 createForThread | **perf_event_open 封装**: **FdTransferClient::hasPeer() 时走 fdtransfer 要 fd**(:628-629,AP-1 权限桥的消费方),否则 `syscall(__NR_perf_event_open)`(:630)+ 旧内核无 CLOEXEC 重试(:631-636);失败时 `isResourceLimit`(EMFILE/ENOMEM)**紧急停机**(:646-651) | High |
| perfEvents_linux.cpp:532-568 RingBuffer | **mmap 环形缓冲**: `mmap(2*page_size, MAP_SHARED)`(:652-657)+ `perf_event_mmap_page` 头(:556)——perf 采样事件流的中转 | High |
| cpuEngine.cpp:14-40 | **CpuEngine 基类**: `_current` 指针 + `onThreadStart/End`(线程事件挂钩,JVMTI ThreadStart/End 触发 :26-40)/`createForAllThreads` 批量建 fd | High |
| itimer.cpp:13-45 | **ITimer(继承 CpuEngine)**: `setitimer(ITIMER_PROF)`(:36)+ SIGPROF 信号处理器(:29);`itimer.h:12` `class ITimer : public CpuEngine`——**软件定时器版 CPU 引擎**(无 perf 权限时用) | High |
| ctimer_linux.cpp + wallClock.cpp:81-136 | **ctimer/wallClock**: ctimer=**POSIX 定时器**(`syscall(__NR_timer_create)` + SIGEV_THREAD_ID,ctimer_linux.cpp:40-46);wallClock=**线程睡眠状态感知**(ThreadSleepMap :81-104,阻塞线程单独记录)+ `_start_time` TSC(:136) | High |
| processSampler.cpp:13-54 | **进程级采样**: `_process_history`(pid→历史 :13/:28)——系统进程 CPU 采样(非 JVM 线程) | Medium |
| allocTracer.cpp:27-34 | **分配采样(hook JVM)**: libjvm 符号前缀匹配(`_ZN11AllocTracer27send_allocation_in_new_tlab` 等多套签名 :27-34)——**hook 进 JVM 的分配事件,版本兼容的关键** | High |
| objectSampler.cpp:134-185 | **对象采样(JVMTI 事件)**: `class ObjectSampler : public Engine`(objectSampler.h:15);`SampledObjectAlloc`(:134)——**JVMTI SampledObjectAlloc 事件回调**;`_allocated_bytes` 计数(:14)触发;`initLiveRefs/dumpLiveRefs`(:159/:166,live 模式) | High |
| lockTracer.cpp:18-115 | **锁采样**: `pthread_key_t` TLS 存锁时间(:18)/`parkBlocker` 字段反射读取(:114)——测量线程在锁上等待的时长;`_interval` 用 TSC 频率换算(:41) | High |
| mallocTracer.cpp:254行 | **native 分配采样**: malloc/free 挂钩(与 libc 拦截) | Medium |
| hooks.cpp:72-110 | **native 钩子**: `pthread_create_hook`/`dlopen_hook`(LD_PRELOAD 注入)——采样线程生命周期/native 库加载跟踪(⚠️ 范围规划初版误写为"JVMTI 事件",已修正) | High |
| rateLimit.cpp:15-34 | **采样节流**: 预算+结转(`_budget[i].budget = limit`,carryover :33-34)——防事件风暴 | High |
| threadFilter.cpp | **线程过滤**: 只采样指定线程(--threads/线程名过滤) | Medium |

*17 个知识点*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| 采样主路径(信号→栈采集→记录) | profiler.cpp, engine.cpp, cpuEngine.cpp, perfEvents_linux.cpp, itimer.cpp | recordSample 是唯一入口,各引擎只负责"发信号" |
| 线程生命周期集成 | cpuEngine.cpp(onThreadStart/End), profiler.cpp(ThreadStart/End JVMTI), hooks.cpp(pthread_create) | 线程出现/消失都挂钩 |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| perf fd 获取双路径 | perfEvents_linux.cpp, fdtransfer(AP-1) |
| 分配采样 | allocTracer.cpp, objectSampler.cpp, instrument.cpp(AP-3) |
| 锁采样 | lockTracer.cpp, nativeLockTracer.cpp |
| 节流与防护 | rateLimit.cpp, profiler.cpp(tryLock) |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| 进程级采样 | processSampler.cpp |
| 线程过滤 | threadFilter.cpp |
| ctimer | ctimer_linux.cpp |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| recordSample 主路径(限流+并发锁+重置) | "一次采样发生了什么"的完整答案;并发信号防护是采样器正确性关键 |
| getJavaTraceAsync(ASGCT 信号安全) | **信号处理器里怎么安全拿 Java 栈**——面试"async-profiler 为什么能在信号里采样";JDK-8132510 |
| perf_event_open + fdtransfer 双路径 | 权限桥的消费端;AP-1 的设计闭环 |
| 分配采样(hook JVM AllocTracer 符号) | "alloc 事件怎么来的"——与 instrument 插桩的对比 |
| 引擎家族(CpuEngine/ITimer 继承) | 策略模式;perf vs itimer 的权限取舍 |
| 信号注册与引擎联动(setupSignalHandlers/trapHandler) | 信号是有限资源——冲突检测+库边界判定+陷阱暂停引擎 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| RingBuffer mmap | perf 数据结构,理解事件流即可 |
| wallClock 线程睡眠感知 | 阻塞采样的机制 |
| 锁采样(parkBlocker) | 锁等待测量的实现 |
| native 钩子(pthread/dlopen) | LD_PRELOAD 机制 |
| 节流(rateLimit) | 保护设计 |
| JVMTI 回退(getJavaTraceJvmti) | 非 Java 线程/AGCT 失败的兜底路径 |
| 引擎启停(enableEvents/nostop) | 陷阱期间暂停引擎防信号风暴 |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| processSampler | 系统级,边角 |
| threadFilter | 简单过滤 |
| ctimer | 第三引擎变体 |
| mallocTracer | native 分配,专项 |

---

## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 信号来了之后发生了什么(主路径)→ CPU 引擎怎么发信号(perf/itimer)→ 其他事件怎么采集(alloc/lock/wall)。

### 依赖图

```
01 采样主路径(recordSample)              ← 无前置
  ├─ 02 CPU 引擎(perf/itimer)            ← 依赖 01 (引擎触发主路径)
  ├─ 03 分配事件(alloc/object)           ← 依赖 01
  └─ 04 锁与墙钟事件(lock/wall)          ← 依赖 01
```

### 教学顺序

```
01 采样主路径(信号→栈→记录,含限流/并发/重置)
  → 02 CPU 引擎(perf_events 硬件 vs itimer 软件 + fdtransfer)
    → 03 分配事件(AllocTracer hook + objectSampler 插桩)
      → 04 锁/墙钟事件(parkBlocker/TLS + 睡眠感知)+ 节流收尾
```

### 文章拆分 (4 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-sampling-core.md | 采样主路径 | recordSample/限流/并发锁/ASGCT/JVMTI 回退/信号注册 |
| 2 | 02-cpu-engines.md | CPU 采样引擎 | perf_event_open+fdtransfer+RingBuffer/CpuEngine/ITimer/ctimer/紧急停机 |
| 3 | 03-allocation-events.md | 分配事件 | AllocTracer hook/objectSampler/mallocTracer |
| 4 | 04-lock-wall-events.md | 锁与墙钟 | lockTracer(parkBlocker)/nativeLock/wallClock/processSampler/rateLimit |

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "信号处理器里能调 malloc 吗?" | 不能——ASGCT+无锁设计(01 篇) |
| "没权限 perf_event_open 怎么 CPU 采样?" | ITimer 软件定时器(02 篇) |
| "alloc 事件是插桩还是 hook?" | 双轨: AllocTracer 符号 hook + objectSampler 插桩(03 篇) |
