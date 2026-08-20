# async-profiler 源码学习范围规划

> **版本**: v1 (2026-08-10 初版 — 全量文件盘点后起草)
> **仓库**: `/data/workspace/source-code/code/spring/async-profiler/`(github.com/async-profiler/async-profiler)
> **规模**: C++ 65+3 = 68 文件 ~25.5K 行 + Java 61 文件 ~6.7K 行,共 ~32K 行
> **定位**: Arthas AR-6 已学 ProfilerCommand 命令调用层——本项目补 **native 实现本体**(真实采样引擎)
> **方法论**: 参照 Arthas(HANDOFF-ARTHAS.md §二)+ openjdk-book(48 域 C++ 参考)

---

## 一、仓库概况

async-profiler 是 JVM 的采样型性能分析器(Andrei Pangin 主导)。核心机制: `perf_events`(Linux 硬件计数器)+ JVMTI 定时中断 → 信号处理器中行走 Java/native 混合调用栈 → 符号解析 → 火焰图/JFR 输出。被 Arthas/IDEA/YourKit 等广泛集成。

**核心模块**(src/):

| 模块 | 职责 | 状态 |
|---|---|---|
| `profiler.cpp`(1718行) | 核心编排:信号处理/线程采样/引擎调度 | ✅ 主战场 |
| `instrument.cpp`(1280行) | 字节码插桩(方法进入/返回采样增强) | ✅ |
| `perfEvents_linux.cpp`(995行) | perf_events 采样(硬件计数器+环形缓冲) | ✅ |
| `symbols_linux.cpp`(917行) | 符号解析(/proc 映射+动态符号) | ✅ |
| `vmStructs.cpp`(760行) | JVM 内部结构偏移解析(版本兼容) | ✅ |
| `flightRecorder.cpp`(1558行) | JFR 输出(自研 JFR 格式) | ✅ |
| `rustDemangle.cpp`(2039行) | Rust 符号 demangle | 🟡 专项 |
| `stackFrame_*`(7 架构) | 各 CPU 架构栈行走 | 🟡 保留 x64/aarch64 |
| `src/api/`(Java 7文件) | `one.profiler.AsyncProfiler` Java API 桥 | ✅ AP-6 |
| `src/converter/`(Java 51文件) | JFR→HTML 转换器 | 🟡 AP-5 |
| `src/main/` + `launcher/` + `jattach/` | asprof 命令行 + 启动器 | ✅ AP-1 |
| `src/j9*` | OpenJ9 支持 | 淘汰 |
| `src/converter/one/heatmap` | 热力图(旧输出) | 淘汰 |

---

## 二、知识域规划 (AP-0 ~ AP-6,共 7 域)

### 🟢 AP-0 使用与火焰图(入门实操域)

> 定位: 与 Arthas AR-0 篇 6 同型——先会用再学原理。产出=命令实操+火焰图读法。

**内容**: `asprof` 命令行(`-e cpu/alloc/lock/wall -d 30 -f out.html` 等);attach 方式(attach 到本地/远程 JVM);火焰图读法(x 轴占比/y 轴栈深/颜色语义);`convert` 转换 JFR;与 Arthas `profiler` 命令的对应(`--event`/`--format`/`--duration` 参数同源)。

---

### 🔴 AP-1 启动与参数解析

**核心文件**: `src/main/main.cpp`、`src/asprof.cpp`、`src/arguments.cpp/h`(602行)、`src/launcher/`、`src/jattach/`、`src/fdtransfer*`

**机制**(提取自源码):
- `arguments.cpp`: 命令行解析(`-e/-d/-f/-o/-i/-t` 等全参数)、`parse()` → `Arguments` 结构;`check()` 参数校验
- `main.cpp`: 入口——attach 目标 JVM 或启动 agent;launcher 逻辑(JVM attach + loadAgent)
- `jattach/`: 自研 jattach(不依赖 JDK)——`psutil` 找进程、attach 协议
- `fdtransfer*`: **文件描述符传递**(采样进程通过 fd 传输 /proc 信息——安全边界设计)
- `asprof.cpp`: 命令行的"asprof"包装(服务模式?)

**关键设计**: 无 JDK 依赖的自包含 attach;fd 传递避免采样线程直接读 /proc 的权限问题

---

### 🔴 AP-2 采样引擎与事件(核心)

**核心文件**: `profiler.cpp`(1718)、`engine.cpp/h`、`cpuEngine.cpp`、`perfEvents_linux.cpp`(995)、`itimer.cpp`、`ctimer_linux.cpp`、`wallClock.cpp`(281)、`processSampler.cpp`、`allocTracer.cpp`、`objectSampler.cpp`(193)、`mallocTracer.cpp`(254)、`lockTracer.cpp`(271)、`nativeLockTracer.cpp`、`hooks.cpp`(200)、`event.h`、`rateLimit.cpp`、`threadFilter.cpp`

**机制**:
- `engine.cpp`: **采样引擎抽象**(Engine 接口: start/stop + 信号回调)——cpu/itimer/ctimer/wallClock/processSampler 都是 Engine 实现(策略模式)
- `perfEvents_linux.cpp`: `perf_event_open` + **mmap 环形缓冲** + 信号通知;`perfEvents_linux.cpp:995` 行含解析/配置
- `cpuEngine.cpp`: perf_events 包装为 CPU 采样
- `itimer.cpp`/`ctimer.cpp`: 定时器采样(ITIMER/clock_gettime)
- `wallClock.cpp`: 墙钟采样(线程状态感知)
- `profiler.cpp`: **信号处理器**(SIGPROF 等)→ 当前线程栈采样 → 调用栈数组 → 聚合;线程生命周期事件
- `allocTracer.cpp`/`objectSampler.cpp`: 分配采样——`allocTracer` 通过 **hook JVM 的 AllocTracer 符号**(libjvm 符号前缀匹配,多套签名兼容,allocTracer.cpp:27-34)+ `objectSampler`(instrument 注入 + 栈回溯)
- `mallocTracer.cpp`/`nativeLockTracer.cpp`: native 分配/锁采样
- `lockTracer.cpp`: Java 锁采样(与 Arthas AR-3 的锁视角互补)
- `hooks.cpp`: **native 层钩子**——`pthread_create_hook`/`dlopen_hook`(hooks.cpp:72-110,LD_PRELOAD 注入,非 JVMTI 事件)
- `rateLimit.cpp`/`threadFilter.cpp`: 采样节流/线程过滤

**关键设计**: 采样(perf_events 硬件中断)vs 插桩(instrument 方法进入)——两种引擎并存;信号处理器里**无锁**栈行走(不能调用 malloc 等非 async-signal-safe 函数)

---

### 🔴 AP-3 JVM 集成(字节码与内部结构)

**核心文件**: `instrument.cpp`(1280)、`vmStructs.cpp`(760)、`vmEntry.cpp`(536)、`javaApi.cpp`(302)、`stackWalker.cpp`(536)、`codeCache.cpp`(332)

**机制**:
- `vmEntry.cpp`: JVMTI 回调注册(VMInit/ClassLoad/ThreadStart...)、agent 生命周期
- `instrument.cpp`: **字节码插桩**——方法进入/返回注入采样代码(ASM 风格 C++ 字节码改写);alloc 采样靠它织入
- `vmStructs.cpp`: **JVM 内部结构布局解析**(通过 JDK 类获取偏移——版本无关的关键技术)
- `stackWalker.cpp`: 栈行走入口(JVMTI GetStackTrace + 自研 native 行走)
- `codeCache.cpp`: JIT 代码缓存(查找已编译方法地址→Java 帧)
- `javaApi.cpp`: Java API 侧的实现(AsyncProfiler.getSamples 等)

**关键设计**: vmStructs 不写死 JVM 版本——运行时解析偏移;栈行走优先 native 快速路径,失败回退 JVMTI

---

### 🔴 AP-4 栈行走与符号解析

**核心文件**: `stackFrame_x64.cpp`(245)/`stackFrame_aarch64.cpp`(353)(其余 5 架构淘汰)、`frameName.cpp`(403)、`dwarf.cpp`(418)、`symbols_linux.cpp`(917)、`demangle.cpp`、`rustDemangle.cpp`(2039,淘汰或简述)、`lookup.cpp`(187)、`dictionary.cpp`、`linearAllocator.cpp`、`callTraceStorage.cpp`(323)

**机制**:
- `stackFrame_x64.cpp`: **frame pointer 行走**(叶函数优化处理——frame pointer omission 的补偿)
- `dwarf.cpp`: DWARF 解帧(无 frame pointer 时的回退)
- `frameName.cpp`: 帧名解析(类名/方法名/行号——结合 JVMTI 与符号)
- `symbols_linux.cpp`: 符号解析(/proc/self/maps + ELF 符号表 + 动态符号 + 内联表)
- `lookup.cpp`: 地址→符号缓存(LRU)
- `callTraceStorage.cpp`: 采样栈的存储(去重+合并——**调用树节点复用**)
- `linearAllocator.cpp`: 无锁线性分配器(采样热路径)

**关键设计**: 采样线程中不能 malloc——linearAllocator 解决;帧行走在信号上下文中安全

---

### 🟡 AP-5 输出与格式

**核心文件**: `flameGraph.cpp`(231)、`flightRecorder.cpp`(1558)、`jfrMetadata.cpp`(333)、`otlp.cpp`、`writer.cpp`、`converter/`(Java 51 文件: convert 16 + jfr 9 + jfr/event 17 + proto 1)

**机制**:
- `flameGraph.cpp`: 火焰图 HTML 生成(自研渲染)
- `flightRecorder.cpp`: **JFR 格式输出**(自研——与 JDK JFR 二进制格式对齐:事件定义/元数据/压缩)
- `jfrMetadata.cpp`: JFR 事件元数据定义
- `otlp.cpp`: OpenTelemetry 协议输出(新)
- `writer.cpp`: 输出文件管理(轮转?)
- `converter/`(Java): JFR→HTML 转换器(用户用 jfr 格式后离线转换)

**关键设计**: 不依赖 JDK 的 jdk.jfr 模块——自研写 JFR;与 OpenJDK 域 32 JFR 对照学习

---

### 🟡 AP-6 Java API 与外部集成

**核心文件**: `src/api/one/profiler/`(7 文件: AsyncProfiler.java 等)、`src/helper/`(3 文件)、`src/converter/one/proto`

**机制**: `AsyncProfiler.java` 的 native 方法声明(`execute/getSamples/dumpCollapsed`...)、`Instance` 单例、与 Arthas `ProfilerCommand` 的调用对应(AR-6 已学命令层,此处补 API 层)

**关键设计**: Java API 是 native 的薄桥——`execute("start,event=cpu")` 字符串协议(Arthas 的 executeArgs 拼的就是它)

---

## 三、淘汰清单

| 模块/功能 | 理由 |
|---|---|
| `src/j9*`(OpenJ9 支持,5 文件) | 小众 JVM——学习 JVM 集成以 HotSpot 为准 |
| `stackFrame_{arm,i386,loongarch64,ppc64,riscv64}` | 架构专项——保留 x64/aarch64 两个主流 |
| `rustDemangle.cpp`(2039行) | 专项符号——简述存在即可 |
| `converter/one/heatmap`(8 文件) | 旧热力图输出 |
| `jattach/` 细节 | 工具层——AP-1 提及即可 |
| `chk.cpp`/`zInit.cpp`/`tsc.cpp`/`safeAccess.cpp` 等工具 | 支撑设施 |

## 四、统计

| 类别 | 数量 |
|---|---|
| 🟢 入门实操域 | 1 (AP-0) |
| 🔴 核心域 | 4 (AP-1~AP-4) |
| 🟡 扩展域 | 2 (AP-5~AP-6) |
| **总域** | **7** |

## 五、学习顺序

```
AP-0 使用与火焰图(先用——与 Arthas AR-6 衔接)
  → AP-1 启动与参数解析(怎么跑起来)
    → AP-2 采样引擎与事件(核心:怎么采样——perf_events/信号处理)
      → AP-3 JVM 集成(怎么插桩/怎么拿内部结构)
        → AP-4 栈行走与符号解析(采样到的地址怎么变回调用栈)
          → AP-5 输出与格式(怎么变成火焰图/JFR)
            → AP-6 Java API(与 Arthas 衔接收尾)
```

## 六、跨项目关联

| 本项目域 | Arthas | OpenJDK(openjdk-book) |
|---|---|---|
| AP-0 | AR-0 篇 6 / AR-6 | — |
| AP-1 | AR-6(参数同源) | 域 40 Launcher、域 36 Attach |
| AP-2 | AR-6(命令层) | **域 32 JFR(采样框架)**、域 18 Safepoint、域 34 NMT |
| AP-3 | AR-2(字节码增强对比) | **域 28 JVMTI**、域 47 Instrumentation、域 16 Code Cache |
| AP-4 | — | 域 24 Frame & Stack、域 23 StubRoutines |
| AP-5 | — | 域 32 JFR(输出侧)、域 44 Class Verification |
| AP-6 | AR-6(execute 协议) | 域 27 JNI |

## 七、修订记录

| 日期 | 版本 | 内容 |
|---|---|---|
| 2026-08-10 | v1 | 初版:全量文件盘点(65 C++ + 61 Java)+ 7 域划分 + 淘汰清单 |
| 2026-08-10 | v1.1 | 深审修正:①hooks.cpp 职责(编造"JVMTI 事件"→实测 native 钩子);②allocTracer 机制细化(hook JVM AllocTracer 符号);③AP-1 03 分类补 3 机制 |
| 2026-08-10 | v1.2 | 二轮深审:①objectSampler 机制修正(编造"instrument 插桩"→实测 JVMTI SampledObjectAlloc 事件回调);②ctimer 修正(timer_create+SIGEV_THREAD_ID,非 clock_gettime);③AP-2 03 分类补 3(信号注册/JVMTI 回退/引擎启停) |
