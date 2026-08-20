# 02. 手写字节码改写器 — BytecodeRewriter 与重定位表

> 🔴 Deep | 13 KP 中的 3 个(BytecodeRewriter/重定位表/latency)
> 读者处境: 插桩要在方法进入处塞代码——但字节码是紧凑的二进制,插入会破坏偏移、行号表、栈映射表。async-profiler 不用 ASM 库,手写改写器解决这一切。

### 1. "不用 ASM,自己写" — BytecodeRewriter

场景: 为什么不用现成的 ASM 库?因为 C++ 项目不想引入 Java 生态依赖。

- `BytecodeRewriter`(instrument.cpp:312-341)核心方法:
  - `rewriteCode`(:312)/`rewriteCodeForLatency`(:313,延迟分析版)
  - `rewriteLineNumberTable`(:314)/`rewriteLocalVariableTable`(:315)——**行号/局部变量表重定位**
  - `rewriteStackMapTable`(:316)/`rewriteVerificationTypeInfo`(:317)——**栈映射表重算**(插入代码改变栈深/类型,验证器要求)
  - `rewriteMethod`/`rewriteAttributes`/`rewriteClass`(:318-322)——逐层改写
- 触发: JVMTI `ClassFileLoadHook`(instrument.h:59)——每个类加载时改写一次
- 与 Arthas 对照: Arthas 用 **ByteKit 框架**(注解驱动拦截器,AR-2 篇 2);async-profiler **纯手写**——两种路线: 框架(可读性)vs 手写(零依赖)

关键设计: **改写的最小侵入**: 只注入"计数+采样检查"类指令(分配插桩),方法语义不变——但**任何字节改动都要修三类表**: 行号表(调试)、局部变量表(调试)、栈映射表(验证)。`rewriteVerificationTypeInfo` 是"插入指令后栈状态变化"的修正——这是改写器最易错的部分。

### 2. "插进去之后一切偏移都变了" — 重定位表

场景: 方法体里插了 5 条指令,后面的所有跳转/行号/变量索引全部偏移——怎么一次修对?

- `rewriteCodeForLatency`(instrument.cpp:507): 注释直说——**"First scan: fill relocation_table and rewrite code"**(:527)
- 两遍处理: ①**首遍扫描**: 逐指令解析,构建 `relocation_table`(原偏移 → 新偏移映射,:527)②**重写**: 按表修正所有引用
- 消费方: `rewriteLineNumberTable(relocation_table)`(:672)/`rewriteLocalVariableTable`(:686)/`rewriteStackMapTable`(:715)全部接收同一张表
- [JVM: 字节码指令长度不一(1-5 字节),跳转有相对/绝对;插入点之后的每一条指令地址都可能变——重定位表是"一次扫描,多处修正"的标准解法]

关键设计: **两遍法与单一事实源**: 首遍只做"度量"(建表),第二遍"施工"(按表改)——重定位表是所有后续修正(行号/变量/栈映射)的**唯一事实源**。这与 Arthas ByteKit 的内部做法同思路(插桩点定位+偏移修正),只是 async-profiler 全部手写。

### 3. "latency 模式: 记录时间戳" — recordEntry/recordExit0

场景: `-e trace`/延迟分析要方法级耗时——插桩里埋计时点。

- `recordEntry(JNIEnv*, jobject)`(instrument.h:65)/`recordExit0(JNIEnv*, jobject, jlong startTimeNs)`(:66)——进入记录开始时间戳,退出带回时间戳
- `rewriteCodeForLatency`(:313)专门生成带 start_time 局部变量的改写
- `_calls`/`shouldRecordSample`(instrument.h:27/:31)——**采样率控制**: 不是每次调用都记录(高频方法会炸),按计数器节流

关键设计: **插桩的采样率**: 与信号采样(固定频率)不同,插桩是"调用驱动"——高频方法必须节流(shouldRecordSample 的计数器),否则插桩开销反超收益。这是"事件驱动"与"周期驱动"在插桩侧的融合。

---

跨域桥: 插桩消费方 = AP-2 篇 3(objectSampler 的分配计数)/篇 4(lockTracer 的等待检测);框架对照 = Arthas AR-2 篇 2(ByteKit);栈映射表语义 = OpenJDK 域 44 Class Verification。

**OpenJDK 关联**: [域 44 Class Verification — outlines/44-class-verification/] — 改写后的字节码必须过验证器(StackMapTable 重算的语义基础)。
