# 02. 采样的火焰图 vs 插桩的 trace — native 边界与原理

> 🔴 Deep | 9 KP 中的 3 个(库加载/平台限制/采样原理)
> 读者处境: 命令层是"翻译器"——真正干活的是 native 库。它在哪、怎么加载、为什么和 trace 完全不同?

### 1. "native 库的入场" — 加载策略与重复 attach 的坑

场景: `profiler start` 时,libasyncProfiler 怎么进来?为什么反复 attach 不报错?

- `profilerInstance()`(monitor200/ProfilerCommand.java:551-585): `AsyncProfiler.getInstance(libPath)`(:580)——libPath 来自 arthas 安装目录的 libasyncProfiler.so
- **加载前复制到临时文件**(:562-575): 注释直说——"避免多次 attach 时出现 Native Library already loaded in another classloader"
- 平台限制: 仅 Linux/Mac(:583-588),其他 OS 抛 "Current OS do not support AsyncProfiler"
- [JNI: native 库按"类加载器+库路径"唯一加载——同一个 .so 被第二个 ClassLoader 加载会抛 `UnsatisfiedLinkError: Native Library already loaded`。复制成新路径 = 骗过 JNI 的幂等检查,支持 stop 后再 start(新 ClassLoader 加载新副本)]

关键设计: **把"反复 attach"变成可能**: arthas 可以 stop→再 attach→再 start profiler,每次都新建 ClassLoader 加载 core——如果没有临时文件复制,第二次 profiler start 直接崩。这是"工具自身可循环使用"的细节工程。

### 2. "不插桩的观测" — 采样 vs 插桩

场景: watch/trace 是插桩(织入 SpyAPI),profiler 是采样——两者本质区别?

- **插桩**(AR-2): 改字节码,方法级精确(每行/每调用点),开销与调用次数成正比,能拿参数/返回值
- **采样**(本域): 不改代码——perf_events(CPU)/JVMTI 定时中断,统计线程调用栈,**开销与时间成正比**(每毫秒采一次栈),拿不到参数但**零侵入、可长时间跑**
- 采样精度: 高频短方法可能被漏采(统计学误差);插桩无此问题但开销大
- [Linux: perf_events 内核子系统——CPU 硬件计数器按固定频率触发中断,中断里记录当前调用栈;JVM 配合 JVMTI 把 native 栈翻译成 Java 栈(栈帧翻译是 async-profiler 的核心价值之一)]

关键设计: [模式: 观察(采样)vs 织入(插桩)——两种观测范式的对比] **两种技术的分工**: 插桩回答"这个方法一次调用花多久、参数是什么"(精度),采样回答"CPU 时间整体花在哪"(全景)。所以生产顺序是: thread -n 定位 → profiler 全景确认 → trace 下钻细节(AR-0 篇 6 §3 的组合拳在原理层成立)。

### 3. "火焰图怎么读" — 形态与语义

场景: HTML 火焰图打开,一坨彩色块——从哪看起?

- x 轴 = 采样次数占比(块越宽 = 占 CPU 越多,不是时间线);y 轴 = 调用栈深度(下→上: 调用者→被调用者)
- 读法: 顶部找最宽的块(热点函数)→ 顺栈往下的路径就是热点调用链
- `--event` 切换: cpu(默认)/alloc(分配热点——找谁在 new)/lock(锁竞争)/wall-clock(阻塞)——AR-0 篇 6 §2 已覆盖
- 输出格式: flamegraph(html)/tree(jfr,给 JMC 用)/collapsed(给其他工具)/md(LLM 友好,:60)

关键设计: **火焰图是"采样的可视化"**: 宽块 = 采样命中的比例 = 该调用栈占 CPU 的比例——数学上是"伯努利采样的频度估计"。这就是它和 trace 树(精确耗时)的语义差异: **估计 vs 实测**。

---

跨域桥: 插桩机制 = AR-2 篇 2(ByteKit);thread -n 定位 → profiler 全景 → trace 下钻 = AR-0 篇 6 §3;lock 事件与 AR-3 篇 3 的锁争用互为补充视角。

---

**OpenJDK 关联**:  [OpenJDK 域 32 JFR — outlines/32-jfr/] — 采样框架对比;**另见** [OpenJDK 域 18 Safepoint — outlines/18-safepoint/] — async-profiler 的栈采样依赖 safepoint 一致性(safepoint bias 问题),这是采样精度的深层原因。

### 核心悬念

**"至此,arthas 的两大观测体系(插桩+采样)闭环了"** — 从 attach 寄生(AR-1)、命令与字节码(AR-2)、线程与锁(AR-3)、面板(AR-4)、表达式(AR-5)到火焰图(AR-6): 一条命令从回车到出图的完整旅程,你已经全部走过。面试时,这条链路就是你的答案。
