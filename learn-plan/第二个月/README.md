# 第二个月：核心能力（170h）

> **目标**：深入掌握 JVM 原理（GC/JMM/JIT/类加载），建立"面试必杀技"；理解 Kafka ISR 和 Raft 协议，打通网络→中间件的完整链路。
> **核心理念**：第一个月建好了地基，这个月开始盖楼——JVM 是大厂面试的绝对核心，Kafka/Raft 是分布式系统的入门砖。
> **月末产出**：能脱稿讲清楚 G1 GC 全流程、JMM happens-before 规则、ZGC 染色指针、虚拟线程原理、Kafka ISR 机制、Raft 协议三子问题。
>
> **本规划依据**：[学习大纲.md](../../学习大纲.md) 第二阶段（JVM 14本 + 网络协议 13本）+ [4个月面试冲刺路线.md](../../4个月面试冲刺路线.md)

---

## 第二个月要攻克什么？

```
第二个月的核心命题：JVM 如何管理内存和并发？分布式系统如何保证一致性？

    ┌──────────┐      ┌──────────┐      ┌──────────┐
    │  G1 GC   │      │   JMM    │      │  Kafka   │
    │ Region   │      │  HB规则  │      │   ISR    │
    │ SATB     │      │  volatile│      │  事务    │
    │ Mixed GC │      │  DCL     │      │          │
    └────┬─────┘      └────┬─────┘      └────┬─────┘
         │                 │                 │
    ┌────▼─────┐      ┌────▼─────┐      ┌────▼─────┐
    │ 类加载    │      │  JIT     │      │  Raft    │
    │ 双亲委派  │      │  C1/C2   │      │  选举    │
    │ 自定义CL  │      │  内联    │      │ 日志复制 │
    └──────────┘      └──────────┘      └──────────┘
```

**三条主线并行推进**：
- **主线1（JVM 内存管理）**：G1 GC → ZGC → GC 日志解读 → GC 调优
- **主线2（JVM 并发与执行）**：JMM → 类加载 → JIT 编译 → 虚拟线程
- **主线3（网络与中间件）**：内核网络 → Socket 编程 → Kafka ISR → Raft 协议 → 高性能网络（DPDK/XDP/K8s）

**三个主线的交汇点**：
- G1 SATB 写屏障 ← → JMM volatile 内存屏障 ← → 内核网络 sk_buff 的并发安全
- JVM 线程模型 ← → 虚拟线程 ← → Kafka 消费者线程模型
- ZGC 染色指针 ← → Linux 虚拟地址空间 ← → C++ 对象指针布局（第一个月基础）

---

## 四周内容总览（与学习大纲完全对齐）

| 周次 | 主题 | 核心产出 | 目录 | 对应学习大纲 |
|------|------|---------|------|------------|
| **第5周** | JVM G1 GC 深度剖析 | 能讲清楚 G1 Region/RSet/SATB/Mixed GC 流程 | [第五周/](./第五周/) | 2.1.1 垃圾回收（书32 第1-5章 + 书33 算法篇） |
| **第6周** | JMM + ZGC + 类加载 + 泛型/集合 | 能讲清楚 JMM happens-before 规则、类加载过程、ZGC 染色指针、泛型/集合框架 | [第六周/](./第六周/) | 2.1.4 并发编程（书40 + 书42）+ 2.1.1 垃圾回收（书34 第6-8章）+ 2.1.2 虚拟机设计（书35 第1-3章） |
| **第7周** | JIT + GC 调优 + 故障排查 + 虚拟线程 | 能解读 GC 日志、用 JMH 做基准测试、排查线上故障、理解虚拟线程原理 | [第七周/](./第七周/) | 2.1.3 性能与调优（书37 第4-7章）+ 2.1.5 故障排查（书43 + 书44）+ 2.1.4 并发编程（书39 Ch1-2） |
| **第8周** | 内核网络 + Socket 编程 + 高性能网络 + Kafka ISR + Raft 协议 | 能讲清楚 TCP 内核路径、epoll 实现原理、DPDK/XDP 差异、Kafka ISR 机制、Raft 三子问题 | [第八周/](./第八周/) | 2.2 网络协议（书67 第2-6章 + 网络协议补充专题）+ 3.2 中间件（书85 第1-8章 + 书89 第2章） |

---

## 完整书籍章节映射（来自学习大纲.md）

### 第5周：JVM G1 GC 深度剖析

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 2.1.1 垃圾回收 | **书32《JVM G1源码分析和调优》** | 第1-5章 | GC 概述与 G1 基本概念（Region/停顿预测/卡表/对象头）、对象分配（快速/慢速/GC 触发）、Refine 线程（RSet/Refinement Zone/写屏障）、新生代回收（YGC 算法/代码分析/日志/参数） |
| 2.1.1 垃圾回收 | **书33《深入Java虚拟机：JVM G1GC的算法与实现》** | 算法篇 第1-6章 | 堆结构（Region 详细布局）、并发标记（SATB/标记位图/初始标记/并发标记/最终标记/存活对象计数）、转移（卡表/写屏障/热卡片/回收集合选择）、软实时性、分代 G1GC 模式 |

**多书交叉印证**（学习大纲推荐）：
- G1 堆结构：书32（彭成寒，源码+调优视角）+ 书33（中村成洋，算法理论视角）+ 书34 第6章（彭成寒，GC 全景视角）
- SATB 并发标记：书33 算法篇 第3章（理论推导）+ 书32 第6章（源码实现对照）+ openjdk 源码 g1ConcurrentMark.cpp

### 第6周：JMM + ZGC + 类加载 + 泛型/集合

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 2.1.4 并发编程 | **书42《Java Generics and Collections》** | Part I 泛型 Ch1-2 + Part II 集合 Ch4/6/7/12 | 泛型类型擦除、通配符（? extends/? super）、PECS 原则、HashMap（数组+链表+红黑树）、ConcurrentHashMap（JDK7分段锁→JDK8 CAS+synchronized）、ArrayList vs LinkedList、TreeMap/LinkedHashMap（LRU缓存）、hashCode/equals 契约 |
| 2.1.4 并发编程 | **书40《Java Memory Model Unlearning Experience》** | 演讲幻灯片全部 + JSR-133 规范 | JMM happens-before 规则、因果关系与同步顺序、final 字段语义与安全发布、Double-Checked Locking（DCL）正确实现、VarHandles 优化 |
| 2.1.1 垃圾回收 | **书34《深入探索JVM垃圾回收》** | 第6-8章（G1/Shenandoah/ZGC） | G1 全景、Shenandoah 超低停顿、ZGC 染色指针/读屏障/亚毫秒级停顿 |
| 2.1.2 虚拟机设计 | **书35《深入浅出Java虚拟机设计与实现》** | 第1-3章（类加载器） | 双亲委派模型、类加载全过程（装载/验证/准备/解析/初始化）、自定义 ClassLoader |

**多书交叉印证**：
- 泛型/集合：书42（Maurice Naftalin，权威教材）+ 书44 第2章（朱晔，集合类实战坑点）+ OpenJDK 源码 HashMap.java/ConcurrentHashMap.java
- JMM：书40（Aleksey Shipilёv，规范级理解）+ 书37 第10章（性能视角的 JMM）+ 书44 第2章（朱晔，实战坑点——DCL/volatile/并发工具类）
- ZGC：书34 第8章（原理入门）+ 书32 第12章（G1 对比视角）+ openjdk 源码 zHeap.cpp/zRelocate.cpp
- 类加载：书35 第1-3章（华保健，设计视角）+ 书45 第6章（许令波，Web 实战视角）+ 书43 第9章（类加载器问题排查）

### 第7周：JIT + GC 调优 + 故障排查 + 虚拟线程

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 2.1.3 性能与调优 | **书37《Java性能权威指南》** | 第4-7章 | JIT 编译器（C1/C2/分层编译/内联/逃逸分析）、GC 算法选型与调优（-Xlog:gc* 解读）、堆内存与原生内存最佳实践、线程与同步性能分析 |
| 2.1.3 性能与调优 | **书38《JVM Performance Engineering》** | JMH 权威参考 | 微基准测试方法论、JMH 高级用法（@Fork/@Warmup/@Measurement/Blackhole）、避免 JIT 优化陷阱（死代码消除/常量折叠） |
| 2.1.5 故障排查 | **书43《Java深度调试技术》** | 第1-15章（26个典型案例） | 线程堆栈分析（死锁检测/阻塞分析）、内存泄漏排查（MAT/heap dump）、高 CPU 定位（top -H + jstack）、数据库连接池泄漏、OOM 系统性分析 |
| 2.1.5 故障排查 | **书44《Java开发坑点解析》** | 第1-5章（150+坑点） | 并发坑（死锁/活锁/ABA）、集合坑（fail-fast/Arrays.asList/subList）、内存坑（OOM/泄漏）、日期坑（SimpleDateFormat 线程不安全）、equals 不对称、finally 中的 return、自动拆箱 NPE |
| 2.1.2 虚拟机设计 | **书36《虚拟机设计与实现--以JVM为例》** | 第8章（JIT 编译器内部） | JIT 编译器内部实现视角：C1 HIR→LIR 流水线、C2 Sea of Nodes 理想图、寄存器分配策略（线性扫描 vs 图着色） |
| 2.1.4 并发编程 | **书39《Modern Concurrency in Java》** | Chapter 1-2（Virtual Threads） | 虚拟线程定义（平台线程 vs 虚拟线程）、吞吐量与可扩展性、载体线程/挂载卸载机制、Pinning 问题与 ReentrantLock 解决 |
| 2.1.4 并发编程 | **书41《Virtual Threads, Structured Concurrency, and Scoped Values》** | 全文（虚拟线程补充） | 虚拟线程完整实现细节、StructuredTaskScope 结构化并发、ScopedValue 替代 ThreadLocal、虚拟线程性能调优 |

**多书交叉印证**：
- JIT 编译：书37 第4章（Scott Oaks，实用视角）+ 书36 第8章（李晓峰，虚拟机内部视角）+ openjdk 源码 c1_Compiler.cpp/c2_Compiler.cpp
- 故障排查：书43（张民卫，26个典型案例）+ 书44（朱晔，150+实战坑点）+ jstack/jmap/jstat 工具链实战
- 虚拟线程：书39 Ch1-2（原理入门）+ 书41 第1章（Ron Veen，完整实现细节）+ openjdk 源码 virtualThread.cpp

### 第8周：内核网络 + Socket 编程 + 高性能网络 + Kafka ISR + Raft 协议

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 2.2 网络协议 | **书67《深入理解Linux网络》** | 第2-6章 | 网络包接收全路径（NAPI/软中断/协议栈）、Socket 层与系统调用、网络包发送全路径、本机网络 IO、TCP 三次握手内核实现细节 |
| 2.2 网络协议（补充专题） | **Socket 编程实战** | Day 2.5 自研专题 | TCP Socket API 全流程、五种 I/O 模型对比（阻塞/非阻塞/多路复用/信号驱动/异步I/O）、epoll 内核实现（红黑树+就绪链表）、ET/LT 模式/EPOLLONESHOT、UNIX 域 Socket、select/poll/epoll 性能对比 |
| 2.2 网络协议（补充专题） | **高性能网络技术** | Day 2.8 自研专题 | DPDK 原理（PMD/HugePage/无锁队列/Run-to-Completion）、RDMA（InfiniBand/RoCEv2/iWARP）、XDP/eBPF 数据面加速、零拷贝技术链（sendfile/splice/tee/DMA）、K8s 网络模型（CNI/Flannel/Calico/Service/iptables-vs-IPVS） |
| 3.2 中间件 | **书85《Kafka权威指南》** | 第1-8章 | 生产者/消费者/分区机制、集群成员关系与控制器、ISR 机制与高水位、请求处理与物理存储、可靠性保证、幂等生产者与事务 |
| 3.2 中间件 | **书89《Etcd源码解析》** | 第2章（Raft协议） | Raft 三子问题（领导人选举/日志复制/安全性）、集群成员变化、日志压缩、客户端交互 |

**网络协议专题说明（学习大纲 2.2 覆盖补充）**：
- 学习大纲 2.2 网络协议涵盖 13 本书，原第 8 周仅覆盖 2 本（书 67《深入理解 Linux 网络》+ 补充）
- Day 2.5（Socket 编程实战）：补充 TCP Socket 编程、epoll 深度使用、I/O 模型系统对比——这是理解 Kafka/Etcd 网络层（NIO/Reactor）的技术基础
- Day 2.8（高性能网络技术）：补充 DPDK/RDMA/XDP/K8s 网络——这是打破"网络栈从内核到应用"认知边界的关键，理解"什么样的场景需要旁路内核"
- 两个专题与 Kafka ISR / Raft 形成完整学习链路：**内核如何收发数据 → Socket 如何高效管理连接 → 消息中间件如何可靠复制 → 共识算法如何达成一致 → 高性能场景如何突破内核瓶颈**

**多书交叉印证**：
- 内核网络：书67（张彦飞，2023 新书/内核路径视角）+ 书66 第3-5章（罗钰，2.6.18 源码逐行视角）+ 书10 第3-4章（应用层→内核追踪）
- Kafka ISR：书85 第6章（官方视角）+ 书90 第3章（分布式系统视角，Paxos 对比）+ tcpdump 抓 Kafka 消息验证
- Raft 协议：书89 第2章（Etcd 实现视角）+ 书90 第4章（Paxos 对比视角）+ 书94 第4-6章（Paxos→Raft 演进视角）

---

## 多书交叉印证策略（每周围绕主线展开）

| 周次 | 主线 | 核心书（主线） | 印证路线（2-3本书） |
|------|------|-------------|------------------|
| **第5周** | G1 GC 全链路 | 书32 + 书33 算法篇 | 书34 第6章（G1 全景）+ openjdk 源码 g1CollectedHeap.cpp/g1ConcurrentMark.cpp（源码验证）+ GDB 观察堆结构 |
| **第6周** | JMM + ZGC + 类加载 + 泛型/集合 | 书40 + 书34 第6-8章 + 书35 第1-3章 + 书42 | 书37 第10章（性能视角 JMM）+ 书44 第2章（实战坑点）+ OpenJDK 源码 HashMap.java |
| **第7周** | JIT + GC 调优 + 故障排查 + 虚拟线程 | 书37 第4-7章 + 书39 Ch1-2 + 书43 + 书44 | 书36 第8章（JIT 内部视角）+ JMH 基准测试 + -Xlog:gc* 实战 + jstack/jmap 工具链 |
| **第8周** | 内核网络 + Socket 编程 + 高性能网络 + Kafka + Raft | 书67 第2-6章 + 自研 Socket/高性能网络专题 + 书85 第1-8章 + 书89 第2章 | 书66 第3-5章（内核源码对照）+ DPDK/RDMA 白皮书 + 书90 第3-4章（分布式对比）+ tcpdump/K8s 抓包验证 |

---

## 必须掌握的面试话题（第二月）

### JVM GC（面试高频区）

1. **G1 GC 的 Region 布局**：Region 类型（Eden/Survivor/Old/Humongous）、大小（1-32MB，默认2048个 Region）、为什么这样设计？——停顿预测模型的基础
2. **SATB 并发标记算法**：三色标记、写屏障（pre-write barrier）、SATB 队列/缓冲区、最终标记（remark）阶段——为什么需要 STW？与 CMS 的增量更新有什么不同？
3. **RSet（记忆集）**：为什么需要 RSet？每个 Region 对应一个 RSet、Coarse/Medium/Fine 三级粒度、写屏障维护 RSet——这是 G1 跨 Region 引用追踪的核心
4. **Mixed GC 流程**：并发标记（初始标记→并发标记→最终标记→清理）→ 混合回收（多次 Mixed GC 回收 Old Region）、CSet（Collection Set）选择策略、停顿预测模型
5. **ZGC 染色指针**：指针着色（42位地址 + 4位元数据）、读屏障（load barrier）vs G1 写屏障、自愈能力（self-healing）——为什么能实现亚毫秒级停顿？

### JMM/JIT/类加载（面试区分度区）

5.5 **泛型与集合框架**：类型擦除是什么？PECS 原则？HashMap 的 put 流程？ConcurrentHashMap JDK7 分段锁 vs JDK8 CAS+synchronized？——能脱稿画出 HashMap 数据结构和 ConcurrentHashMap 演进
6. **JMM happens-before 规则**：程序顺序规则、volatile 变量规则、监视器锁规则、传递性——能脱稿写出 DCL（Double-Checked Locking）的正确实现并解释每个 volatile/synchronized 的作用
7. **类加载双亲委派模型**：Bootstrap→Extension→Application→Custom ClassLoader、loadClass vs findClass、为什么需要双亲委派（安全+避免重复加载）？什么场景需要打破双亲委派（Tomcat/SPI/OSGI）？
8. **JIT 编译（C1/C2/分层编译）**：C1（client 编译器，低延迟）、C2（server 编译器，高吞吐）、分层编译 5 个级别、内联（inline）条件与逃逸分析、-XX:+PrintCompilation 解读
9. **synchronized 锁升级**：偏向锁→轻量级锁（CAS）→重量级锁（monitor）、锁膨胀/撤销条件、为什么 Java 15 默认关闭偏向锁？——与虚拟线程的关系

### 故障排查与坑点（面试实战区）⭐新增
9.5 **线上故障排查**：如何排查死锁？如何定位高 CPU？如何分析内存泄漏？OOM 有哪些类型各怎么排查？——能完整回答"你遇到过什么线上问题，怎么排查的"
9.6 **开发坑点 Top 20**：SimpleDateFormat 线程不安全、ArrayList 遍历时删除、自动拆箱 NPE、finally 中 return、ThreadLocal 未清理、HashMap 并发问题、equals 不对称等——面试高频基础坑点

### JIT 深度（新增）
10. **分层编译有哪几个级别？Level 0-4 各自的特点？**：Level 0=解释执行+收集 Profiling 数据、Level 1=C1 简单编译（无 Profiling）、Level 2=C1+有限 Profiling、Level 3=C1+完整 Profiling（收集类型/分支信息）、Level 4=C2 激进优化——Profiling 数据是 C2 激进优化的基础
11. **JIT 的方法内联条件是什么？逃逸分析能做哪些优化？**：内联条件（MaxInlineSize/FreqInlineSize/MaxInlineLevel/虚方法先去虚拟化）、逃逸分析三优化（标量替换—对象分解为标量无堆分配、锁消除—线程局部对象的 sync 可消除、栈上分配—HotSpot 主要靠标量替换实现）

### 网络/中间件（面试综合区）

12. **TCP 三次握手内核实现**：客户端 connect → 服务端 listen/accept、syn queue vs accept queue、syncookies 防御 SYN flood——理解内核中 sk_buff 的流转路径
13. **Kafka ISR 机制与高水位**：ISR（In-Sync Replicas）定义、HW（High Watermark）与 LEO（Log End Offset）、ack=all 的可靠性保证、ISR 收缩/扩张条件、Unclean Leader Election 的风险——能画出 ISR/HW/LEO 的关系图
14. **Raft 协议三子问题**：领导者选举（term/RequestVote RPC）、日志复制（AppendEntries RPC/committed index）、安全性（选举限制/提交限制）——为什么 Raft 比 Paxos 更容易理解？Raft 如何保证线性一致性？
15. **epoll 实现原理**：为什么 epoll 比 select/poll 快？ET 和 LT 模式的区别？EPOLLONESHOT 解决什么问题？——能画出 eventpoll 红黑树+就绪链表的数据结构图
16. **DPDK vs XDP**：Kernel Bypass vs Kernel Fast Path 的根本差异？各自适合什么场景？为什么 DPDK 需要独占网卡？XDP 的优势在哪？（与内核网络栈共存+eBPF安全验证）
17. **K8s 网络模型**：Pod 网络（CNI）→ Service（kube-proxy）→ Ingress 三层分别解决什么问题？Flannel VXLAN 和 Calico BGP 的工作原理和性能差异？

---

## 关键工具掌握

| 工具 | 用途 | 第几周 | 对应学习大纲建议 |
|------|------|--------|----------------|
| GDB 调试 JVM | 观察 G1 堆结构（Region/RSet/CSet）、查看 C++ 对象布局 | 第5周 | 书32 附录（编译调试 JVM）+ openjdk 源码 slowdebug 构建 |
| HSDB（HotSpot Debugger） | 查看 Java 对象内存布局、验证 JMM | 第6周 | 书43 第10章 + `jhsdb` 命令行 |
| JMH（Java Microbenchmark Harness） | 基准测试、验证 JIT 优化效果、对比虚拟线程性能 | 第7周 | 书37 第2章 + 书38 JMH 微基准测试 |
| -Xlog:gc*（GC 日志） | 解读 GC 行为：YGC/Mixed GC/Full GC 频率与耗时 | 第7周 | 书37 第5-6章 + 书32 第2章（日志解读） |
| jstat/jmap/jstack | 实时监控 GC 统计、堆转储分析、线程堆栈诊断 | 第7周 | 书43 第1-3章（线程堆栈/内存泄漏/堆分析） |
| MAT（Memory Analyzer Tool） | heap dump 分析、内存泄漏追踪、支配树分析 | 第7周 | 书43 第3章 + MAT 官方文档 |
| jstack + top -H | 线程堆栈分析、CPU 热点定位、死锁检测 | 第7周 | 书43 第1-2章 + 实战演练 |
| tcpdump | 抓包分析 Kafka 消息、验证 TCP 协议行为 | 第8周 | 书10 附录 + 书67 实战 |
| perf | 分析 JVM 进程的 CPU/内存热点 | 第7周 | 书37 第3章 + 书43 第15章 |
| openjdk-cut-new源码 | JIT编译器/G1 GC源码阅读（C1/C2/编译策略） | 第5、7周 | 书32 附录（编译调试 JVM）+ openjdk-cut-new slowdebug 构建 |

---

## 学习方法提醒（来自学习大纲）

1. **先骨架后细节**：先通读建立脉络（第1遍），再精读核心章节并动手验证（第2遍）——例如 G1 GC 先理解 Region→RSet→SATB→Mixed GC 的宏观流程，再深入每个环节的源码实现
2. **多书交叉印证**：同一主题参考 2-3 本不同作者的书——例如 G1 GC 同时看彭成寒（源码+调优）、中村成洋（算法理论）、Scott Oaks（性能视角）三种角度
3. **动手验证优先**：结合 openjdk-cut-new 源码阅读 + GDB 验证堆结构 + JMH 基准测试——代码不会骗人
4. **费曼学习法**：学完后用自己话讲一遍，讲不出来 = 没学会——特别是 JMM happens-before 和 Raft 协议，能脱稿画图讲解才算掌握
5. **面试导向**：每个主题学完后问自己"面试官会怎么问这个问题"，准备 3 分钟回答——G1 GC 的 SATB 标记、JMM 的 DCL、Kafka 的 ISR 是高概率面试题
6. **不要贪多**：每阶段主攻 3-4 本核心书，其余书作为查阅/补充——第二个月核心书：书32/33/40/42/34/35/37/43/44/39/67/85/89
7. **关联第一个月**：学 G1 写屏障时回想 C++ volatile/memory barrier、学 ZGC 染色指针时回想虚拟地址空间/页表、学内核网络时回想 sk_buff 和 epoll——底层知识是理解上层机制的钥匙

---

## 前置知识检查表（进入第二个月需要掌握）

| 知识领域 | 你需要先掌握的内容 | 参考来源 |
|---------|-----------------|---------|
| **C++ 对象模型** | vtable 布局、虚函数调用机制、对象内存对齐 | 第一个月 第4周 书17 第1-4章 |
| **Linux 内存管理** | 虚拟内存/页表/TLB、伙伴系统/SLAB、VMA/缺页异常 | 第一个月 第3周 书5 Ch2-6 |
| **Linux 进程/线程** | fork/exec、内核线程 vs 用户线程、CFS 调度器 | 第一个月 第2周 书3 第1-5章 |
| **网络协议栈** | TCP 状态机、sk_buff 结构、epoll LT/ET | 第一个月 第3周 书66 第3-5章 |
| **GDB 调试** | 反汇编、栈帧分析、查看内存/寄存器 | 第一个月 第1周 动手 + 第4周 GDB |
| **Java 基础** | 对象创建过程、基本 GC 概念（标记-清除/复制/标记-整理） | 日常工作经验 + 书32 第1章回顾 |

---

## 与第一个月的衔接说明

**第一个月建立了底层认知地基，第二个月开始在这个地基上盖 JVM 这层楼**：

| 第一个月（根基） | → | 第二个月（核心） | 关联说明 |
|----------------|---|----------------|---------|
| C++ vtable/虚函数调用机制 | → | JVM 对象头/mark word/Klass 指针 | JVM 的 vtable/itable 直接借鉴 C++ 对象模型 |
| Linux 虚拟内存/页表/TLB | → | ZGC 染色指针/多映射 | ZGC 利用虚拟地址空间的空闲位存储 GC 元数据 |
| Linux 内存管理（伙伴系统/SLAB） | → | G1 Region 分配/PLAB/TLAB | G1 的内存管理直接复用 Linux 的 mmap/munmap |
| fork() COW 机制 | → | G1 SATB 写屏障/COW 快照 | SATB 本质是在 GC 开始时刻创建堆的逻辑快照 |
| TCP 状态机/sk_buff | → | Kafka ISR/HW/LEO | Kafka 的复制协议本质是分布式日志的可靠传输 |
| epoll Reactor 模式 | → | Kafka 网络层/NIO | Kafka 的生产者/消费者网络层基于 Java NIO epoll |
| strace 系统调用追踪 | → | GC 日志/jstat/jmap 分析 | 从 OS 层追踪到 JVM 层监控的进阶 |
| C++ memory barrier | → | JMM volatile/写屏障 | 硬件内存屏障 → Java 内存模型的映射 |

---

## 第二月综合自测（面试模拟）

完成四周学习后，用以下问题做一次完整的面试模拟。每题准备 3 分钟脱稿回答。

### JVM GC（6题）
1. G1 的 Region 有哪些类型？一个 Region 多大？为什么这样设计？
2. SATB 并发标记的流程是什么？SATB 和 CMS 的增量更新有什么区别？
3. RSet（记忆集）是什么？为什么需要它？它的三级粒度（Coarse/Medium/Fine）分别是什么场景？
4. Mixed GC 的完整流程是什么？CSet 如何选择？停顿预测模型怎么工作？
5. ZGC 的染色指针是什么？42+4 位分别代表什么？为什么能实现亚毫秒级停顿？
6. G1 的写屏障和 ZGC 的读屏障有什么区别？各自解决了什么问题？

### JMM/JIT/类加载（8题）
6.5 类型擦除是什么？PECS 原则？HashMap 的 put 流程？ConcurrentHashMap JDK7 和 JDK8 的区别？
7. JMM happens-before 有哪些规则？能写出 DCL（Double-Checked Locking）的正确实现吗？
8. 类加载的双亲委派模型是什么？什么场景需要打破它？
9. JIT 的 C1/C2/分层编译分别是什么？内联的条件有哪些？
10. synchronized 锁升级的过程是什么？偏向锁/轻量级锁/重量级锁如何转换？
11. 虚拟线程和平台线程的区别？什么是 Pinning 问题？如何避免？
12. 为什么 Java 15 默认关闭了偏向锁？它与虚拟线程有什么关系？
13. 分层编译有哪几个级别？Level 0-4 各自的特点？Profiling 数据如何驱动编译升级？
14. JIT 的方法内联条件是什么？逃逸分析能做哪些优化？标量替换/锁消除/栈上分配各自触发条件？

### 故障排查与坑点（4题）⭐新增
14.5 你遇到过什么线上问题，怎么排查的？（死锁/高CPU/内存泄漏/OOM）
14.6 说一下 Java 开发中最常见的 5 个坑？（SimpleDateFormat/ArrayList删除/自动拆箱NPE/finally中return/ThreadLocal）
14.7 如何用 jstack 检测死锁？如何用 top -H + jstack 定位 CPU 热点？
14.8 OOM 有哪些类型？分别怎么排查？

### 网络/中间件（7题）
15. TCP 三次握手在内核中是如何实现的？syn queue 和 accept queue 的区别？
16. Kafka ISR 是什么？HW（高水位）和 LEO 的关系？ack=all 如何保证可靠性？
17. Raft 协议的三个子问题是什么？领导者选举的流程？日志复制的流程？
18. Kafka 的幂等生产者和事务是如何实现的？精确一次性语义的前提条件？
19. 从 TCP 可靠传输到 Kafka 的 ISR 机制，再到 Raft 的日志复制，它们在保证"可靠"上有什么共同的思路？
20. epoll 为什么比 select 快？红黑树和就绪链表分别解决了什么问题？
21. DPDK 和 XDP 有什么区别？各自适合解决什么样的性能问题？

### 动手能力（3题）
20. 如何用 GDB 调试 openjdk，观察 G1 的 Region 布局和 RSet 结构？
21. 如何解读 -Xlog:gc* 的日志输出？YGC 和 Mixed GC 的日志各有什么特征？
22. 如何用 JMH 做基准测试？有哪些常见的 JMH 陷阱（死代码消除/常量折叠）？

### 关联思考（2题）
23. G1 的 SATB 写屏障和 JMM 的 volatile 写屏障有什么本质区别？（一个是 GC 的正确性保证，一个是多线程的可见性保证）
24. 从 epoll Reactor 模式到 Kafka 的网络层实现，再到虚拟线程的调度，它们在 IO 模型上的演进思路是什么？

---

## 项目主线：短链服务 M2-核心里程碑

> 继续向核心实战项目推进：[贯穿项目-短链服务](../贯穿项目-短链服务/README.md)

### 本月项目任务

本月是短链服务的核心功能实现阶段——**发号器 + 重定向 + GC调优 + Kafka集成**。

| 任务 | 对应学习 | 产出 |
|------|---------|------|
| 号段模式发号器 + Base62编码 | 第5-7周 JVM/JMM/JIT | 核心算法代码 |
| 短链重定向服务（Redis→302跳转） | 第5-6周 GC + 内存管理 | 重定向核心链路 |
| JVM GC调优（G1参数配置+前后对比） | 第5周 G1堆结构/混合回收 | GC调优报告 |
| Kafka异步处理访问日志 | 第8周 Kafka ISR/高吞吐 | Kafka集成代码 |

### 学以致用

- **G1 Region/记忆集**（第5周）→ 理解跨Region引用如何影响重定向的GC停顿
- **SATB并发标记**（第5周）→ 理解SATB写屏障在高并发读写下的开销
- **Kafka ISR/PageCache**（第8周）→ 访问日志生产者配置acks=all保证不丢消息
- **Raft一致性**（第8周）→ 类比发号器的号段分配如何保证多实例一致

### 里程碑产出

- [ ] 号段模式发号器实现
- [ ] GC调优报告（调优前后GC日志对比）
- [ ] Kafka集成（访问日志异步处理）
- [ ] 技术方案-发号器选型文档

详见：[M2-核心里程碑.md](../贯穿项目-短链服务/M2-核心里程碑.md)
