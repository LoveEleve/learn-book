# 01. 从 Agent_OnLoad 到 16 个回调 — JVMTI 集成总图

> 🔴 Deep | 13 KP 中的 3 个(双入口/回调注册/能力)
> 读者处境: async-profiler 是 .so——它怎么"进入"JVM 并拿到所有事件?答案: JVMTI 的标准剧本: 入口 → 能力 → 回调。

### 1. "两条路进 JVM" — Agent_OnLoad 与 Agent_OnAttach

场景: 启动时(-agentpath)与运行期(asprof attach)两条加载路径。

- `Agent_OnLoad`(src/vmEntry.cpp:467)/`Agent_OnAttach`(:488)——JVMTI 规定的两个 agent 入口
- 启动加载: `java -agentpath:libasyncProfiler.so=...`;运行期加载: asprof 的 jattach load action(AP-1 篇 3)
- 与 Arthas 对照: 这就是 Arthas 的 `premain`/`agentmain`(AR-1 篇 1)——**同一 JVMTI 剧本,两种语言实现**
- [JVMTI: Agent_OnLoad 在 JVM 启动早期调用(VMInit 前),Agent_OnAttach 在运行期——后者不能做某些早期初始化]

关键设计: **双入口共享同一初始化**: 无论哪条路进来,最终都走到"能力申请 + 回调注册 + VMInit 启动引擎"——asprof attach 与 -agentpath 的体验一致(AP-0 篇 1)。

### 2. "16 个回调,各归其主" — 回调注册表

场景: 采样器要的事件太多了——类加载、分配、锁、GC、线程,全都要。

- `jvmtiEventCallbacks callbacks = {0}`(vmEntry.cpp:253)注册 **16 个回调**:
  - **类**: ClassLoad/ClassPrepare/**ClassFileLoadHook→Instrument::ClassFileLoadHook**(插桩入口,:257)
  - **编译**: CompiledMethodLoad→Profiler(记录 JIT 方法,:259)
  - **线程**: ThreadStart/ThreadEnd→Profiler(引擎跟随,:261-262,AP-2 篇 2 的 onThreadStart 触发源)
  - **锁**: MonitorContendedEnter/Entered→LockTracer(:263-264,AP-2 篇 4 的事件源)
  - **分配**: **SampledObjectAlloc→ObjectSampler**(:266)/VMObjectAlloc→J9ObjectSampler(:265)
  - **GC**: GarbageCollectionStart/Finish(:267-268)
- 引擎↔回调的挂接表是**AP-2 各域机制在 JVM 层的落点**
- [JVMTI: SetEventCallbacks + 事件通知模式(setEventNotificationMode)控制开闭——注册是"能力",通知是"开关"]

关键设计: **一份回调表,全引擎共享**: 采样引擎本身不碰 JVMTI——profiler.cpp 的 Profiler::ThreadStart 等是回调的"收件箱",引擎通过它感知线程生命周期。**JVM 事件 → Profiler 静态方法 → 引擎**三层转发,耦合降到最低。

### 3. "先要能力,再谈事件" — capabilities

场景: 某些事件要 JVMTI 能力解锁。

- `capabilities.can_get_source_file_name/can_get_line_numbers/can_generate_compiled_method_load_events/can_generate_monitor_events/can_generate_garbage_collection_events/can_tag_objects = 1`(vmEntry.cpp:245-249)→ `AddCapabilities`
- [JVMTI: 能力(capability)是权限声明——部分能力 AddCapabilities 可能失败(如 can_tag_objects),需检查返回值并降级]

关键设计: **能力是"尽力而为"**: 某些 JVM(如 OpenJ9)不提供部分能力——async-profiler 对失败降级(如 objectSampler 的 SampledObjectAlloc 在 JDK16 前没有,回退 allocTracer 符号 hook,AP-2 篇 3 的双轨设计在此闭环)。

---

跨域桥: 双入口 = Arthas AR-1 篇 1(premain/agentmain 对照);attach 路径 = AP-1 篇 3(jattach load);回调消费方 = AP-2 各篇(引擎挂接点);插桩回调 = 下一篇。

**OpenJDK 关联**: [域 28 JVMTI — outlines/28-jvmti/] — Agent_OnLoad/OnAttach 与 JVMTI 事件分发的 JDK 侧实现。
