# 域 AP-3: JVM 集成(字节码改写与内部结构) — 知识规划

> 源码路径: src/vmEntry.cpp(536行) + src/instrument.cpp(1280行)/instrument.h + src/vmStructs.cpp(760行) + src/stackWalker.cpp(536行) + src/codeCache.cpp(332行) + src/javaApi.cpp(302行)
> 源码量: ~8 文件,核心 ~3800 行
> 提取日期: 2026-08-10(v1 深读提取——范围规划声明已逐文件验证)
> 前置域: AP-2(引擎消费 JVMTI 回调;插桩消费 BytecodeRewriter)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| vmEntry.cpp:467-530 | **Agent 双入口**: `Agent_OnLoad`(:467)/`Agent_OnAttach`(:488)——启动期(-agentpath)与运行期(attach)加载,与 Arthas agentmain/premain 同构(AR-1) | High |
| vmEntry.cpp:245-270 | **16 个 JVMTI 回调注册表**: VMInit/VMDeath/ClassLoad/ClassPrepare/**ClassFileLoadHook→Instrument::ClassFileLoadHook**/CompiledMethodLoad→Profiler/**SampledObjectAlloc→ObjectSampler**/MonitorContendedEnter→LockTracer/VMObjectAlloc→J9ObjectSampler/GC 回调——**AP-2 各引擎在此挂接 JVMTI** | High |
| vmEntry.cpp:243-249 | **能力申请**: can_get_source_file_name/can_get_line_numbers/can_generate_compiled_method_load_events/can_generate_monitor_events/can_generate_garbage_collection_events/can_tag_objects → `AddCapabilities` | High |
| instrument.cpp:312-341 BytecodeRewriter | **自研字节码改写器**: `rewriteCode`(:312)/`rewriteCodeForLatency`(:313)/`rewriteLineNumberTable`(:314)/`rewriteLocalVariableTable`(:315)/`rewriteStackMapTable`(:316)/`rewriteVerificationTypeInfo`(:317)/`rewriteMethod`/`rewriteClass`——**纯 C++ 手写 JVM 字节码改写,不用 ASM 库**(与 Arthas ByteKit 对照) | High |
| instrument.cpp:507-527 rewriteCodeForLatency | **首遍扫描+重定位表**: "First scan: fill relocation_table and rewrite code"(:527)——先扫指令填充重定位表,再重写(插入代码后偏移全变,重定位表记录) | High |
| instrument.h:22-66 | **Instrument=Engine + 回调**: `class Instrument : public Engine`(:22);`ClassFileLoadHook` JNICALL(:59)/`recordEntry`(:65)/`recordExit0(jni, unused, startTimeNs)`(:66)——**latency 模式**(记录方法进入时间戳) | High |
| instrument.h:26-31 | **采样节奏**: `_interval`(Latency)/`_calls` 计数(:27)/`shouldRecordSample()`(:31)——分配插桩的采样率控制 | Medium |
| vmStructs.cpp:134-166 | **JVM 内部结构偏移解析**: `init(libjvm)`(:134)→ `initOffsets`(:148)——**运行时从 JVM 内部表读取偏移**(如 `_klass_name_offset` :166),不写死版本 | High |
| vmStructs.cpp:445-470 | **偏移解析完成判定**: `resolveOffsets`(:445);`_has_class_names = _klass_name_offset >= 0`(:470)——部分 JVM 无该结构时降级 | Medium |
| vmStructs.cpp:591-659 | **线程桥**: `initTLS`(:591)/`initThreadBridge`(:600)/`VMThread::nativeThreadId`(:642)/`VMThread::jni`(:659)——信号内拿 Java 线程信息的通道 | High |
| stackWalker.cpp:73-266 | **三种栈行走**: `walkFP`(:73,**frame pointer 链**)/`walkDwarf`(:122,**DWARF 解帧**)/`walkVM`(:214,**VM 内部行走**含 scope 解析 :363)——按场景/架构选择 | High |
| codeCache.cpp:78-133 | **JIT 代码缓存管理**: `add`(:78)/`findBlobByAddress`(:124,地址→blob 二分)/`binarySearch`(:133)——采样地址→JIT 方法映射 | High |
| javaApi.cpp:56-216 | **JNI native 注册**: `execute0`(:56)/`execute1`(:97,禁止带输出文件)/`getSamples`(:128);`RegisterNatives` 注册(:179-216)——**AP-6 Java API 的 native 侧** | High |

*13 个知识点*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| JVMTI 集成全貌(入口→能力→回调→消费) | vmEntry.cpp, instrument.cpp, vmStructs.cpp, stackWalker.cpp, javaApi.cpp | 16 回调把引擎/插桩/GC 全挂到 JVM 事件 |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| 字节码改写 | instrument.cpp(BytecodeRewriter), instrument.h(回调) |
| 内部结构解析 | vmStructs.cpp, codeCache.cpp(blob 结构与类名关联) |
| 栈行走 | stackWalker.cpp, vmStructs.cpp(线程桥) |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| JNI 注册 | javaApi.cpp |
| Agent 入口 | vmEntry.cpp(单文件但承载全貌) |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| 16 回调注册表(引擎挂接点) | "async-profiler 怎么接入 JVM"的总图——每个引擎对应哪个 JVMTI 事件 |
| BytecodeRewriter 手写改写 | 面试"alloc/锁采样怎么插桩";与 Arthas ByteKit 的框架 vs 手写对照 |
| 重定位表(插入后偏移修正) | 字节码改写正确性的核心难点 |
| vmStructs 运行时偏移解析 | "不写死 JVM 版本"的版本兼容技术 |
| 三种栈行走(walkFP/Dwarf/walkVM) | 信号内安全行走的完整答案 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| Agent 双入口 | 与 Arthas premain/agentmain 对照 |
| latency 模式(recordExit0) | 延迟分析专项 |
| codeCache blob 管理 | JIT 方法查找,配合栈行走 |
| 线程桥(nativeThreadId) | 工具性 |
| 偏移解析降级判定(resolveOffsets/_has_class_names) | 拿不到结构就少给功能,不崩 |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| 能力申请细节 | 标准 JVMTI 流程 |
| 采样节奏(_calls/shouldRecordSample) | 插桩采样率控制 |
| JNI 注册表 | AP-6 详述,此处提 native 侧 |

---

## 04 聚类 — 教学顺序与文章拆分

> 教学主线: agent 怎么进 JVM(入口)→ 怎么拿到事件(回调)→ 怎么改写字节码(插桩)→ 怎么解析内部结构(栈行走依赖)。

### 依赖图

```
01 Agent 入口与回调注册           ← 无前置
  ├─ 02 字节码改写                ← 依赖 01 (ClassFileLoadHook 驱动)
  ├─ 03 内部结构与栈行走           ← 依赖 01 (VMStructs 初始化)
  └─ 04 JNI 桥                    ← 依赖 01 (AP-6 native 侧)
```

### 教学顺序

```
01 Agent 入口(OnLoad/OnAttach) + 16 回调注册 + 能力
  → 02 BytecodeRewriter(手写改写/重定位表/latency)
    → 03 vmStructs(偏移解析) + stackWalker(三模式) + codeCache
      → 04 javaApi(JNI 注册,衔接收尾 AP-6)
```

### 文章拆分 (4 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-agent-jvmti.md | Agent 入口与回调 | 双入口/16 回调/能力申请 |
| 2 | 02-bytecode-rewriter.md | 字节码改写 | BytecodeRewriter/重定位表/latency/与 ByteKit 对照 |
| 3 | 03-vmstructs-stackwalk.md | 内部结构与栈行走 | vmStructs 偏移/线程桥/三模式栈行走/codeCache |
| 4 | 04-java-api-bridge.md | JNI 桥 | execute0/execute1/getSamples/RegisterNatives |

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "插入字节码后行号/局部变量表全乱了怎么办?" | 重定位表(02 篇) |
| "不同 JDK 版本内部结构不一样,怎么兼容?" | 运行时偏移解析(03 篇) |
| "信号里怎么走 Java 栈?" | walkFP/walkDwarf/walkVM 三模式(03 篇) |
