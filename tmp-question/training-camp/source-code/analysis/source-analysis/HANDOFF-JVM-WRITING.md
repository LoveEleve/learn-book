# HANDOFF — 回到 JVM: 文章写作衔接

> **状态**: 🟢 衔接文档 v2(2026-08-10)— 工具探索阶段就绪,下一步实操
> **2026-08-11 v3 更新**: 域 00 实操执行完毕——A/B/C 组全部完成,48 域素材就绪度 ✅32/🟡9/🔴2/✗5(核对表 `openjdk-book/docs/openjdk/planning/knowledge-planning/00-jvm-tools-readiness.md`);素材检索入口 `materials/INDEX.md`;8 项 GUI 截图降级由 Ubuntu 桌面执行(执行计划附录 G)。**下一步 = 按本篇优先级进入文章写作**。
> **目标项目**: `/data/workspace/source-code/openjdk-book/`(49/49 域 + 158 篇大纲已完成——新增**域 00 JVM 工具层**)
> **衔接价值**: 两个工具项目的 14 个域 + 工具层规划,已为 JVM 写作积累"工具视角"素材
> **执行顺序**: 先工具探索(域 00 实操)→ 产出素材 → 文章写作(阶段 A)

---

## 一、三个项目的闭环关系

```
JVM(openjdk-book, C++ 48 域)          ← 底层事实来源
  ↑ 机制关联(13 域)                      ↑ 写作素材
Arthas(Java 命令层 7 域)  ←→  async-profiler(C++ 引擎层 7 域)
       execute("start,event=cpu") 一条字符串串起
```

- **Arthas**: 观察 JVM 的"应用层工具"(字节码增强/线程/内存)——写 JVM 文章时的"使用场景"素材
- **async-profiler**: 采样 JVM 的"引擎层工具"(perf/JVMTI/栈行走)——写 JVM 文章时的"内部机制"素材
- **回到 JVM** = 从这两个工具反推 JVM 内部实现(工具读什么,就写什么)

---

## 二、JVM 写作入口(按工具关联排序)

> openjdk-book 48 域中,被两个工具项目**深度关联**的 13 个域,是写作优先级最高的入口(有现成场景+机制素材)。

| 优先级 | OpenJDK 域 | 工具侧素材(可直接引用) | 写作角度 |
|:--:|---|---|---|
| ★★★ | 32-jfr | async-profiler AP-5(自研 JFR 元数据契约 jfrMetadata.cpp:41-155、recordEvent 分发)、AP-2(采样框架) | "JFR 事件流格式"——自研者视角的规范解读 |
| ★★★ | 28-jvmti | async-profiler AP-3(16 回调注册表、Agent 双入口)、Arthas AR-1(enhanceLoaders) | "JVMTI 事件分发"——16 回调是消费侧清单 |
| ★★★ | 24-frame-stack | async-profiler AP-4(寄存器行走/序言识别)、AP-3(walkVM 内联展开) | "物理帧 vs 逻辑帧"——0x55 序言是活例子 |
| ★★ | 18-safepoint | async-profiler AP-2(signal 内采样、JDK-8132510)、AP-0(safepoint bias) | "采样为什么偏向 safepoint"——工具视角 |
| ★★ | 36-attach | Arthas AR-1(loadAgent 链)、async-profiler AP-1(自研 jattach) | "attach 协议"——两种实现对照 |
| ★★ | 47-instrumentation | Arthas AR-1/AR-2(appendToBootstrap/ByteKit)、async-profiler AP-3(ClassFileLoadHook) | "instrumentation 全貌"——两个工具的挂接点 |
| ★★ | 44-class-verification | async-profiler AP-3(rewriteStackMapTable)、Arthas AR-2(织入后验证) | "字节码改写者的验证依赖" |
| ★★ | 19-synchronization | async-profiler AP-2(MonitorContendedEnter→lockTracer)、Arthas AR-3(thread -b) | "锁事件的两层观察" |
| ★★ | 16-code-cache | async-profiler AP-3(CodeCache/findBlobByAddress) | "JIT 代码的地址管理"——采样器的第三张表 |
| ★ | 27-jni | async-profiler AP-3/AP-6(RegisterNatives/shaded) | "JNI 注册语义" |
| ★ | 03-arguments-flags | async-profiler AP-1(枚举化参数)、Arthas AR-1(Configure) | "配置类型化"——工具与 JVM 同设计 |
| ★ | 09-memory-core | async-profiler AP-4(无锁分配)、Arthas AR-4(内存池) | "分配器思想" |
| ★ | 17-threads | Arthas AR-3(线程枚举)、async-profiler AP-4(线程命名) | "线程模型"——工具读什么 API |

---

## 三、写文章时的"工具视角"素材清单

### 每个域写作时的三步素材提取

```
1. 场景: 用工具问(如 "async-profiler 怎么在信号里拿栈?")
2. 机制: JVM 内部实现(openjdk-book 大纲已有)
3. 验证: 工具侧代码印证(如 walkFP 的 0x55 检测 = frame pointer 布局)
```

### 已沉淀的"工具↔JVM"对应关系(14 域学习成果)

| 工具机制 | JVM 内部真相 |
|---|---|
| Arthas SpyAPI 注入 Bootstrap | 类加载搜索顺序(域 07) |
| Arthas retransformClasses | JVMTI ClassFileLoadHook(域 28/47) |
| async-profiler SampledObjectAlloc | JFR 采样分配事件(域 32) |
| async-profiler perf_event_open | 内核 perf + 域 18 safepoint 交互 |
| async-profiler walkVM [inlined] | JIT 内联 scope(域 16/24) |
| Arthas findMostBlockingLock | ObjectMonitor 锁状态(域 19) |

---

## 四、域 00 工具探索实操(当前阶段)

### 环境就绪状态(2026-08-11,全部实测)

| 组件 | 状态 |
|---|---|
| JDK 17 | ✅ TencentKona 17(`/opt/codev/TencentKona`,14 个工具全装) |
| **JDK 21** | ✅ TencentKona 21.0.12(`/opt/codev/TencentKona-21.0.12.b1`)——**JMC 9.1.2 必需**(插件要求 Java 21+,JDK 17 会报 UnsupportedClassVersionError,已实测) |
| JMC 9.1.2 | ✅ 用 JDK 21 启动(`jmc.ini` 已加 `-vm /opt/codev/TencentKona-21.0.12.b1/bin/java`);自带火焰图/热力图/依赖视图/MBean 浏览器/JOverflow |
| Xvfb + 截图 | ✅ `Xvfb :99` + Java Robot(`/data/tmp/opencode/Shot.java`)+ x11-screen MCP(重启 opencode 会话生效) |
| jfr CLI | ✅ 无头素材提取主力(summary/print/metadata/configure 已实测) |
| MAT 1.16.1 | ✅ `/opt/tools/MAT/`——`ParseHeapDump.sh <dump> org.eclipse.mat.api:suspects` 实测跑通(需 `PATH` 含 JDK21 + `DISPLAY=:99`) |
| VisualVM 2.1.10 | ✅ `/opt/tools/visualvm_2110/` |
| JITWatch 1.5.0 | ✅ `/opt/tools/jitwatch-ui.jar`(未实测启动,待篇 4) |
| GCViewer 1.37 | ✅ `/data/workspace/gcviewer-1.37.jar`(未实测,待篇 3) |
| FlameGraph 脚本 | ✅ `/opt/tools/FlameGraph/`(git clone) |
| perf | ✅ yum 已装(6.6.119) |
| async-profiler | ⚠️ Arthas 仓库仅 3 个 .so,未带 converter.jar/profiler.sh(官方 release 才有 jfr2flame converter);对照改用 JMC 自带 Flame Graph 视图 |

### 实操计划(产出=写作素材)

```
阶段 1(域 00 篇 1): 录 JFR 看全景
  jcmd <pid> JFR.start duration=30s filename=rec.jfr
  → jfr CLI 提取(summary/print 事件/热点/GC 时序,无头主力)
  → xvfb + JMC(打开 rec.jfr,截图 29 页签按需: Overview/Threads/LockInstances/VMOperation/Tlab/Event Browser/Flame Graph)
  → JMC 自带 Flame Graph 视图(对照)
  → 素材: 事件列表/GC 时间线/线程时间线
阶段 2(篇 2): jcmd 子命令探索 → 素材: 50 子命令清单(含 VM.events/VM.log/VM.cds/VM.metaspace)/输出样例
阶段 3(篇 3): jmap -histo → MAT(已装) → 素材: 直方图/支配树/泄漏报告
阶段 4(篇 4): javap + JITWatch(已装) → 素材: 字节码/编译日志
阶段 5(篇 5): jhsdb hsdb(xvfb) → 素材: 对象头截图 + jsnap PerfData 计数器
阶段 6(篇 6): jconsole + perf(已装) + FlameGraph(已装) → 素材: MBean 树/采样对照
阶段 7(篇 7, v3): jimage/jlink/jdeps → 素材: 镜像结构/依赖图样例
```

### 实操目标进程

- 用 math-game(Arthas 仓库 demo)或自写 demo(死循环/大对象分配)作为采样对象
- 每个素材标注: 工具/命令/输出/对应 JVM 域——直接进文章

## 五、建议写作顺序

> **2026-08-11 修订(依赖驱动排序)**: 原阶段 A-D 按"工具素材关联深度"排序,与 WRITING-GUIDELINES §2 依赖驱动排序冲突(32-jfr 依赖 06/17/18/24 等基础域,不能最先写)。**正式顺序见 `openjdk-book/docs/openjdk/planning/knowledge-planning/00-domain-writing-order.md`(48 域依赖图 + 拓扑 7 层)**。

```
写作顺序(拓扑 7 层,从基础到上层):
第 1 批(地基):     01 → 05 → 45 → 48
第 2 批(原语):     02 → 03 → 04 → 06 → 16 → 38 → 41 → 42
第 3 批(对象/类):  07 → 09 → 17
第 4 批(执行/帧):  10 → 19 → 23 → 24 → 08 → 31 → 44
第 5 批(VM 核心):  11 → 12 → 13 → 18 → 20 → 27 → 30 → 32 → 34 → 36 → 37 → 39 → 46
第 6 批(JIT/GC):   14 → 15 → 21 → 25 → 28 → 29 → 33 → 43
第 7 批(上层):     22 → 26 → 35 → 40 → 47

工具素材策略: 工具卷(卷 T)已完成,素材作为每篇文内实证引用,不影响域依赖序。
```

## 六、参考材料

| 材料 | 位置 |
|---|---|
| openjdk-book 交接(48 域完成) | `openjdk-book/docs/openjdk/planning/HANDOFF-NEW-AI.md` |
| 域发现 v3.1(48 域权威清单) | `openjdk-book/docs/openjdk/planning/00-domain-discovery-v3.md` |
| Arthas 完结交接 | `source-analysis/arthas/HANDOFF-ARTHAS.md` |
| async-profiler 完结交接 | `source-analysis/async-profiler/HANDOFF-ASYNC-PROFILER.md` |
| 深审缺陷档案(13 类) | `issue/源码分析深审缺陷档案.md` |
| 总执行计划(33 仓库) | `issue/源码分析执行计划.md` |
