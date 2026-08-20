# HANDOFF — async-profiler 源码分析知识规划(完结交接)

> **状态**: ✅ 知识规划完结(2026-08-10)— 7/7 域 + 88 机制 + 24 大纲 + 110 问 + 7 轮深审
> **下一阶段**: 按大纲写文章(另行启动);**下一步项目**: 回到 JVM(openjdk-book 文章写作,见 HANDOFF-JVM-WRITING.md)
> 接收者: 新 AI

---

## 一、成果总览

| 交付物 | 数量 | 位置 |
|---|---|---|
| 范围规划 | 1(v1.2) | `issue/async-profiler源码学习范围规划.md` |
| KP(四章+依赖图+教学顺序+文章内机制顺序+悬念设计) | 7 | `knowledge-planning/ap{0-6}-*.md` |
| 大纲(v5 标准+OpenJDK 关联) | 24 | `outlines/ap{0-6}-*/0*.md` |
| completeness-questions(4-5 身份/110 问/审查结论表) | 7 | `outlines/ap{0-6}-*/completeness-questions.md` |
| 执行计划 | 1 | `issue/源码分析执行计划.md` 6.6 节(7 域) |
| 深审缺陷档案(通用 13 类) | 1 | `issue/源码分析深审缺陷档案.md` |

**域清单**: AP-0 使用与火焰图(🟢) / AP-1 启动与参数解析(🔴) / AP-2 采样引擎与事件(🔴,17 机制最大) / AP-3 JVM 集成(🔴) / AP-4 栈行走与符号解析(🔴) / AP-5 输出与格式(🟡) / AP-6 Java API(🟡) — 共 **88 机制**。

**项目定位**: Arthas AR-6 只学了命令层;本项目是 **native 采样引擎本体**(65 C++ + 61 Java,~32K 行)。`execute("start,event=cpu")` 一条字符串串起两个项目的全部机制。

---

## 二、方法论要点(Arthas 10 轮 + async-profiler 7 轮实证)

### 每篇大纲 v5 标准

```
### N. "悬念标题" — 机制名
场景: [真实生产场景一句话]
[技术描述 + file.cpp:行号 + 方法名]
关键设计: [为什么] + [模式: XXX](框架项目)
[C++:/Linux:/x86:/JVMTI:/JFR:] 跨层标注
(篇末) 跨域桥 + **OpenJDK 关联** + **核心悬念**(悬念 + → 下一篇桥)
```

### 每份 KP 四章

```
01 逐源提取: 源文件 → 机制 + Confidence | 02 聚合: P1(≥5文件)/P2/P3
03 深度分类: 🔴Deep/🟡Working/🟢Surface(每项含"为什么",必须全覆盖 01 表)
04 聚类: 依赖图 + 教学顺序 + 文章拆分 + 文章内机制顺序 + 关键悬念设计
```

### 深审档案(13 类,详见 `issue/源码分析深审缺陷档案.md`)

**async-profiler 特有高发**: #3 文件名推断编造(本项目中招 3 次: hooks.cpp"JVMTI 事件"→实为 pthread/dlopen 钩子;objectSampler"instrument 插桩"→实为 JVMTI SampledObjectAlloc;ctimer"clock_gettime"→实为 timer_create)。**范围规划的机制描述不可信,KP 必须逐文件重读。**

---

## 三、已验证事实档案(81 组引用全量语义验证,写文章直接引用)

### 关键行号锚点(按域)

**AP-1 启动与参数**
| 机制 | 位置 |
|---|---|
| 第一层解析/动作识别/PMU event | main.cpp:415-470(:423-426 动作, :441-457 -e/PMU) |
| get_tracepoint_id(降权前解析) | main.cpp:387 |
| run_jattach(一切动作=load) | main.cpp:365-373 |
| 逗号串解析(strtok+CASE) | arguments.cpp:41-60, CASE 表 :62-285 |
| 动作/格式/事件/参数枚举 | arguments.cpp:62-84/87-141/147-211/238-285 |
| 单位/超时解析 | arguments.cpp:529/553 |
| fdtransfer 权限桥注释 | fdtransferServer_linux.cpp:134 |
| C API 导出(asprof_execute) | asprof.cpp:27-40 |

**AP-2 采样引擎**
| 机制 | 位置 |
|---|---|
| recordSample 主路径(限流/并发锁/重置) | profiler.cpp:409-520(:411-415, :420-424) |
| tryLock 三级锁 | profiler.cpp:185-191 |
| ASGCT 信号安全栈采集 | profiler.cpp:356-396(JDK-8132510 :351-354) |
| JVMTI 回退 | profiler.cpp:397-408 |
| 信号注册 | profiler.cpp:687-710(SIGTRAP :689, 库边界 :700-704) |
| perf_event_open + fdtransfer 双路径 | perfEvents_linux.cpp:620-660(:628/:630, mmap :652) |
| Engine 抽象 | engine.h:12-54 |
| ITimer(继承 CpuEngine) | itimer.h:12, itimer.cpp:13-45(setitimer :36) |
| AllocTracer 符号 hook | allocTracer.cpp:27-34 |
| objectSampler(JVMTI 事件) | objectSampler.h:15, objectSampler.cpp:134 |
| lockTracer(TLS+parkBlocker) | lockTracer.cpp:18/:41/:114 |
| wallClock 睡眠判定(syscall 检测) | wallClock.cpp:112-131 |
| native 钩子 | hooks.cpp:72-110 |
| 平滑限流 | rateLimit.cpp:15-34 |

**AP-3 JVM 集成**
| 机制 | 位置 |
|---|---|
| Agent 双入口 | vmEntry.cpp:467/488 |
| 16 回调注册表 | vmEntry.cpp:245-270 |
| BytecodeRewriter(手写改写) | instrument.cpp:312-341 |
| 重定位表两遍法 | instrument.cpp:507-527 |
| latency(recordEntry/Exit) | instrument.h:59-66 |
| vmStructs 偏移解析 | vmStructs.cpp:134-166(_klass_name_offset :166) |
| 三种栈行走 | stackWalker.cpp:73(walkFP)/122(walkDwarf)/214(walkVM) |
| CodeCache | codeCache.cpp:78/124 |
| JNI 桥 + shaded 栈回溯 | javaApi.cpp:56-216(:196-216 找真实类) |

**AP-4 栈行走与符号**
| 机制 | 位置 |
|---|---|
| 寄存器访问器 | stackFrame_x64.cpp:21-58 |
| 序言识别(0x55) | stackFrame_x64.cpp:123-127 |
| ELF 多路径符号 | symbols_linux.cpp:505-514, gnu_hash :489-499 |
| demangle(C++/Rust) | demangle.cpp:13-36 |
| 帧命名(JMethodCache) | frameName.cpp:75-98/:151-165 |
| 无锁分配(竞争重试) | linearAllocator.cpp:54-81 |
| 调用栈去重存储 | callTraceStorage.cpp:67-143(overflow :84) |

**AP-5 输出**
| 机制 | 位置 |
|---|---|
| INCBIN 模板嵌入 | flameGraph.cpp:17 |
| Trie 树/minwidth | flameGraph.cpp:82/:111 |
| Recording 头部区 | flightRecorder.cpp:237-312(8 段 write :295-312) |
| recordEvent 分发 | flightRecorder.cpp:1473-1535 |
| JfrSync(JDK JFR 同步) | flightRecorder.cpp:1311/:1404/:1454 |
| JFR 元数据契约 | jfrMetadata.cpp:41-155(完整事件类型) |
| OTLP protobuf | otlp.cpp:12-43 |
| Writer 抽象 | writer.h:13, writer.cpp:44 |

**AP-6 Java API**
| 机制 | 位置 |
|---|---|
| 单例/五级加载 | AsyncProfiler.java:19-60 |
| execute 家族 | AsyncProfiler.java:188-228 |
| Instrument helper | Instrument.java:13-33 |
| LockTracer trusted context | LockTracer.java:9-22 |
| MXBean | AsyncProfilerMXBean.java:19-33 |

---

## 四、OpenJDK 关联(13 域,24/24 篇已挂)

| 本项目机制 | OpenJDK 域(outlines/) |
|---|---|
| attach/launcher | 36-attach、40-launcher |
| 参数枚举化 | 03-arguments-flags |
| 采样/safepoint/锁 | 32-jfr、18-safepoint、19-synchronization |
| JVMTI/字节码改写 | 28-jvmti、44-class-verification |
| 栈行走/CodeCache | 24-frame-stack、16-code-cache |
| 无锁分配 | 09-memory-core |
| JNI/命名 | 27-jni、17-threads |

---

## 五、写文章阶段指引

1. 从 AP-0 逐篇展开(大纲 → 800-1500 字),保留: 场景/行号/关键设计/跨层标注/核心悬念
2. 引用"已验证事实档案"——81 组行号已核,不必重验
3. 每篇自查: 四要素 grep + 文字锚(`\.(cpp|h)(?!:\d)`)+ 对照大纲
4. **写作素材补充建议**: 每个机制可加"与 Arthas 对照"(execute 协议/锁视角/字节码改写)和"与 JVM 对照"(safepoint/JVMTI/栈布局)

---

## 六、下一步: 回到 JVM(见 HANDOFF-JVM-WRITING.md)

- openjdk-book: 48/48 域 + 152 篇大纲已完成,**进入文章写作阶段**
- 本项目的 24 篇大纲已关联 13 个 OpenJDK 域——"工具视角"的素材可直接反哺 JVM 写作(如: async-profiler 的 SampledObjectAlloc 消费 = 域 32 JFR 的分配事件写作素材;walkVM 内联展开 = 域 24 Frame 的内联帧素材)
