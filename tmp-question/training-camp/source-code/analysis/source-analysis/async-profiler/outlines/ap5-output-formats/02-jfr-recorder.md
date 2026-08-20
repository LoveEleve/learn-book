# 02. 不依赖 JDK 的 JFR 写入器 — 自研 Recording 与事件分发

> 🔴 Deep | 12 KP 中的 4 个(Recording 头部区/recordEvent 分发/JfrSync/元数据)
> 读者处境: `-o jfr` 输出的文件,JDK 的 JMC 能直接打开——但 async-profiler 一行 jdk.jfr 代码都没用。1558 行的 flightRecorder.cpp 是怎么"无中生有"写出 JFR 的?

### 1. "JFR 文件的结构" — Recording 与头部区

场景: JFR 不是纯事件流——文件开头要有一整套元数据。

- `Recording`(flightRecorder.cpp:237): 持 `RecordingBuffer _buf[CONCURRENCY_LEVEL]`(:244)——**并发缓冲**(信号线程各自写)
- 构造时按序写头部区(:295-312): `writeHeader`(魔数+版本)/`writeMetadata`(类型系统)/`writeRecordingInfo`/`writeSettings`/`writeOsCpuInfo`/`writeJvmInfo`/`writeSystemProperties`/`writeNativeLibraries`——**8 段系统信息区**
- `--live`/内存文件模式(:314,`createMemoryFile`)
- [JFR: JDK Flight Recorder 的二进制格式——Chunk 头部 + 元数据事件 + 数据事件;async-profiler 按规范手工写,保证 JMC 可读]

关键设计: **格式兼容 = 逐字节对齐**: 自研写 JFR 的关键不是"写事件",而是**元数据区与 JDK 完全一致**(类型 ID/字段布局)——JMC/converter 才能解析。jfrMetadata.cpp 的流式 type/field 定义(jfrMetadata.cpp:41-155)就是这份"类型契约"。

### 2. "事件怎么落盘" — recordEvent 分发

场景: 采样线程调一次,事件进缓冲。

- `FlightRecorder::recordEvent(lock_index, tid, call_trace_id, event_type, event)`(flightRecorder.cpp:1473):
  - **per-thread 时间戳更新**(:1476-1482): `tld->sample_counter`——只更新,不分配
  - 按 `event_type` 分发: PERF_SAMPLE/EXECUTION_SAMPLE/INSTRUMENTED_METHOD → `recordExecutionSample`;alloc → allocSample;lock → lockSample
  - 写进 `_rec->buffer(lock_index)`——与 AP-2 篇 1 的 lock_index 同一套并发槽
- [JFR: 事件 = 类型 ID + 时间戳 + 字段;时间戳用 TSC→纳秒换算]

关键设计: **写入也走并发槽**: recordEvent 与 recordSample 共用 `lock_index`(AP-2 篇 1 的三级锁)——信号处理器内写入是"无锁缓冲追加",落盘/压缩是后台(flush :1368)事。**"信号内只写缓冲"是 JFR 输出不阻塞采样的保证**。

### 3. "--jfrsync: 与 JDK JFR 合流" — JfrSync

场景: 想要 JDK JFR 的原生事件(GC/JIT/类加载)和 async-profiler 的采样事件在**同一个录制文件**里。

- `start`(flightRecorder.cpp:1311)→ `startMasterRecording`(:1404): **先启动 JDK 自己的 JFR**(`jdk.jfr.Recording`),async-profiler 的 JFR 作为"从属"追加
- `JfrSync` 类: 反射调用(`CallStaticVoidMethod` :1454,`one.profiler.JfrSync` 的 start/stop)
- 数据流: JDK JFR(系统事件)+ async-profiler JFR(采样事件)→ **converter 合并解析**

关键设计: **两个 JFR 的合流**: 不自己造系统事件(GC/JIT 那是 JDK 的事),而是**启动 JDK JFR 并把自己的采样事件对齐进同一时间线**——`--jfrsync` 的语义是"取长补短": JDK 提供系统事件,async-profiler 提供高分辨率采样。

---

跨域桥: 并发槽 = AP-2 篇 1(lock_index 同源);时间戳 = AP-4(tsc.cpp);转换消费 = 下一篇(converter)+ AP-0 篇 3(jfr+JMC 离线);JFR 规范对照 = OpenJDK 域 32 JFR。

**OpenJDK 关联**: [域 32 JFR — outlines/32-jfr/] — 自研 JFR 与 JDK JFR 的格式/元数据对齐(最强关联域)。
