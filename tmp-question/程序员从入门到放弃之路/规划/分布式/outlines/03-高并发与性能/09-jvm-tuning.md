# 你的代码跑得飞快, GC却每30秒卡你100ms — JVM调优不是背参数

> Cluster C: 8 KPs | 依赖: 08-code-optimization | 读者基线: 配过JVM参数(-Xmx -Xms), 看过GC日志但不知道如何根据日志调整

---

### 1. GC不是你的敌人 — 是你内存管理的"自动挡"
  你把GC停顿当成"JVM的问题" — 实际上GC是在帮你收拾你分配的内存垃圾。
  - 为什么需要GC? — 手动free()的代价: 悬空指针(use after free), 内存泄漏(forget to free), 二次释放(double free) — GC用CPU时间换来内存安全 (B5 Ch8 §8.2)
  - 分代假说: 弱分代假说(大多数对象朝生夕死)→强分代假说(越老的对象越不容易死)→跨代引用假说(老年代引用新生代极少) — 分代GC的理论基础 [理论: 分代收集的三个假说 — 伊甸园(Eden)中90%+对象在第一轮GC就被回收, 验证了弱分代假说; 这也是为什么JVM默认Eden:S0:S1=8:1:1]
  - GC的三个指标: 吞吐(GC时间占比, 高吞吐=服务端)→延迟(STW最长时间, 低延迟=交互式应用)→内存(堆大小, 小内存=嵌入式) — 三选二, 无法三者全优 (B5 Ch8 §8.2, B4 Ch5 §5.3)
  - 关键设计: 不同GC是为不同三选二组合设计的 — Parallel(吞吐+内存, 牺牲延迟), CMS(延迟+内存, 牺牲吞吐, 碎片问题), G1(三者平衡), ZGC(延迟优先, TB堆)

### 2. GC选型 — 你现在用什么GC? 应该用什么GC?
  你一直用默认GC, 从JDK8 Parallel→JDK11 G1→JDK17 G1 — 但业务从批量处理变成了实时API, 该换了。
  - 选型决策: Serial(JVM默认, <100MB堆, 单线程STW长)→Parallel(高吞吐, >4G堆, 批处理/科学计算)→G1(平衡, JDK9+默认, 大堆+可预测停顿)→ZGC(超低延迟<1ms, TB堆, JDK11实验→JDK15生产可用) (B5 Ch8 §8.2, B4 Ch5 §5.3)
  - G1核心: Region(堆分成2048个Region, 1-32MB)→Mixed GC(选收益最大的Region回收)→Garbage First — 为什么叫G1? 回收垃圾最多的Region优先 (B5 Ch8 §8.2)
  - ZGC: 染色指针(Colored Pointers)+读屏障(Load Barrier) — 64位指针中42位地址+4位GC状态(标记的4个颜色)→并发回收不需要STW (B5 Ch8 §8.2)
  - 关键设计: G1的预测模型 — 基于历史Mixed GC时间预测下次Mixed GC回收多少Region可满足停顿目标(-XX:MaxGCPauseMillis), 动态调整回收量

### 3. 堆内存配置 — -Xms=-Xmx不是玄学, 是有道理的
  你线上-Xms=1G -Xmx=4G, 堆从1G动态涨到4G的过程发生了什么?
  - 为什么设相等? — 堆大小动态变化=每次扩容导致Full GC(整理内存)+系统调用sbrk — 生产环境应为-Xms=-Xmx避免动态调整 (B5 Ch8 §8.2, B4 Ch5 §5.3)
  - 新生代:老年代比: 默认1:2(NewRatio=2) → IO密集型(请求周期短, Eden大)vs计算密集型(长生命周期, Old大)需调整 (B5 Ch8 §8.2)
  - 直接内存: -XX:MaxDirectMemorySize(默认=-Xmx) → ByteBuffer.allocateDirect不走堆, 受此参数限制 — 你的NIO文件传输/Netty连接数多的服务要调大 (B5 Ch8 §8.2, B4 Ch5 §5.3) [工程: 直接内存泄漏定位 — jcmd VC.native_memory detail → 找"Other"或"Internal"里增长的部分 → 怀疑DirectByteBuffer没被GC(有虚引用但ReferenceHandler线程处理慢)]
  - 关键设计: NMT(Native Memory Tracking) — 跟踪JVM的所有内存(堆内+堆外), 比jmap更全面, 生产环境开启(1-2%性能开销)

### 4. JIT编译 — 为什么你的代码"越跑越快"?
  启动后前10分钟P99延迟高, 后面下降了 — 不是预热足够, 是JIT在编译你的热点代码。
  - 解释执行vs编译执行: 解释器(逐条翻译, 启动快)+C1(Client编译器, 快速编译, 低延迟)+C2(Server编译器, 深度优化, 高吞吐) (B4 Ch5 §5.3, B5 Ch8 §8.1)
  - 分层编译: Level 0(解释)→Level 1(C1, 无 profiling)→Level 2(C1, 简单 profiling)→Level 3(C1, 全 profiling)→Level 4(C2) — 逐层"加热", 收集运行时信息(C2基于这些信息做激进优化) (B5 Ch8 §8.1)
  - C2的优化: 方法内联(最重要的优化, 消除调用开销+扩大优化范围)→逃逸分析(栈上分配+锁消除+标量替换)→循环展开 — 一个简单getter()因为内联可能被完全消除 (B5 Ch8 §8.1)
  - 关键设计: 去优化(Deoptimization) — C2做了基于profiling的激进优化(如"这个if分支从来没走→直接删除"), 但如果真的走了 → 回退到解释执行 → 这就是"偶发性RT毛刺"的来源之一

### 5. GC日志与Arthas — 在线诊断的"火眼金睛"
  线上突然GC频繁, 你不能重启不能加参数, 怎么诊断?
  - GC日志七要素: 时间→GC类型(Young/Mixed/Full)→回收前→回收后→总→耗时→原因(Allocation Failure/Ergonomics/G1 Humongous) (B5 Ch8 §8.2)
  - GCEasy/gcviewer: 上传GC日志→可视化(GC频率/STW时长/晋升速率/碎片) → 回答: GC正常吗? 有内存泄漏吗? 需要调大堆吗? (B5 Ch8 §8.2)
  - Arthas四命令: dashboard(实时面板: 线程/内存/GC)→thread(找最忙的线程)→trace(跟踪方法调用链+耗时)→watch(观察方法入参/返回值) — 不需要重启JVM (B5 Ch8 §8.1) [案例: GC调优前后对比 — 优化前: Young GC 每30s一次, 每次80ms, G1 Mixed GC 每5分钟; 优化后: 调整Eden大小+对象池+确认大小堆, Young GC 每5分钟, 每次20ms, Mixed GC极少触发]
  - 关键设计: Arthas的核心原理 — Java Agent(Instrumentation API) → Attach API attach到目标JVM→加载Agent→通过字节码增强采集运行时数据 → 热插拔, 零重启

### 6. 收束 — JVM调优的"不要做"清单
  - 不要凭经验调参数: 每个应用的GC行为不同, 先看GC日志再决定
  - 不要盲目调大堆: 大堆→GC时间长→停顿大, 先看是否内存泄漏
  - 不要忽略直接内存: 堆够大但OOM, 可能是直接内存/元空间
  - JVM调优的最终目标是: 让GC不成为你P99毛刺的来源

---

### 核心悬念
**"你的单体应用优化到顶了 — 但业务增长要求系统支撑100x流量, 单体肯定不行, 但拆成微服务后性能更差了 — 为什么?"**

→ 引出 架构演进中的性能优化: 单体→微服务→云原生各阶段的性能陷阱 (10-architecture-evolution-performance)
