# Phase 1：Java 内核攻坚战（第 1-6 周）

## 目标
从"会用"到"理解为什么"。打通 JVM / 并发 / 集合 / I-O 底层。

---

## Week 1：JVM 内存模型与类加载

### 理论知识
- JVM 运行时数据区：堆/栈（虚拟机栈+本地方法栈）/方法区（元空间）/程序计数器
- 对象创建流程：new → 类加载检查 → 分配内存（TLAB/CAS）→ 初始化零值 → 设置对象头 → 执行 `<init>`
- 对象内存布局：MarkWord(8B) + KlassPointer(4/8B) + 实例数据 + 对齐填充
- 类加载器：Bootstrap → Extension/Platform → Application，双亲委派模型
- 打破双亲委派：Tomcat WebAppClassLoader、JDBC DriverManager、SPI 机制

### 源码阅读清单
- `java.lang.ClassLoader.loadClass()` — 双亲委派实现
- `java.util.ServiceLoader` — SPI 机制
- `org.apache.catalina.loader.WebappClassLoader` — Tomcat 打破双亲委派

### 实践任务
1. 用 `jmap -heap <pid>` 查看 Spring Boot 应用的堆内存分布
2. 写一个自定义类加载器（`CustomClassLoader.java`，~100 行），从指定目录加载 .class 文件
3. 用 `jcmd <pid> VM.native_memory summary` 分析 NMT（Native Memory Tracking）

### 输出
- 博客：《一个 Spring Boot 请求如何分配内存》——画出从 Controller 到返回的完整内存路径

### 检验标准
- [ ] 能手绘 JVM 内存结构图（堆的 Young/Old/MetaSpace）
- [ ] 能解释双亲委派为什么存在 + 什么场景需要打破
- [ ] 能用 jmap/jstack/jstat/jcmd 分析运行中的应用

---

## Week 2：GC 深入

### 理论知识
- GC 算法：标记-清除、标记-整理、复制算法、分代收集
- 三色标记法：白/灰/黑 + SATB（G1）/ Incremental Update（CMS）
- CMS：初始标记→并发标记→重新标记→并发清除，碎片化问题
- G1：Region 布局（Eden/Survivor/Old/Humongous）、RSet、Mixed GC
- ZGC：染色指针、并发整理、< 1ms 停顿

### 源码阅读清单
- `java.lang.ref.Reference` — 四种引用（强/软/弱/虚）
- `java.lang.ref.Finalizer` — Finalizer 线程
- OpenJDK `g1CollectedHeap.cpp` — G1 堆结构（了解即可）

### 实践任务
1. 模拟 OOM：`while(true) list.add(new byte[1024*1024])`，用 MAT 分析 dump
2. GC 日志分析脚本：解析 `-Xlog:gc*` 输出，输出"各 GC 类型触发次数和平均耗时"
3. 三组 GC 对比实验：同一应用分别用 CMS/G1/ZGC，记录吞吐量和停顿时间

### 输出
- 博客：《CMS → G1 → ZGC：三代 GC 的实战对比》

### 检验标准
- [ ] 能手绘 G1 的 Region 布局和 RSet 结构
- [ ] 能分析 GC 日志，判断 GC 是否正常
- [ ] 能根据业务场景推荐 GC 选型（吞吐优先 vs 延迟优先）

---

## Week 3：Java 内存模型与并发基础

### 理论知识
- JMM：主内存/工作内存、8 种原子操作、happens-before 规则
- volatile：可见性、禁止指令重排序（StoreStore/StoreLoad/LoadLoad/LoadStore 屏障）
- synchronized：偏向锁→轻量级锁→重量级锁膨胀过程、锁消除/锁粗化
- CAS：Unsafe.compareAndSwapInt、ABA 问题与解决方案

### 源码阅读清单
- `java.util.concurrent.locks.AbstractQueuedSynchronizer`：CLH 变种队列、acquire/release 模板方法
- `java.util.concurrent.locks.ReentrantLock`：公平锁 vs 非公平锁
- `java.util.concurrent.CountDownLatch` / `Semaphore`

### 实践任务
1. 手写 Mini-AQS（`MiniAQS.java`，~300 行）：支持 acquire/release + 条件队列
2. 手写 Mini-ReentrantLock（`MiniReentrantLock.java`，~150 行）：基于 Mini-AQS
3. 用 JMH 压测：synchronized vs ReentrantLock vs MiniReentrantLock

### 输出
- 博客：《AQS 源码精读：从 CLH 队列到 ReentrantLock》

### 检验标准
- [ ] 能手绘 AQS 的 CLH 变种队列 acquire 流程
- [ ] 能解释 synchronized 锁膨胀的完整过程
- [ ] 能手写一个基于 AQS 的自定义同步器

---

## Week 4：线程池与 Virtual Threads

### 理论知识：传统线程池
- ThreadPoolExecutor：corePoolSize/maximumPoolSize/keepAliveTime/workQueue
- ctl 变量位运算：高 3 位存状态，低 29 位存线程数
- Worker 线程复用机制：`runWorker()` 循环从队列取任务
- 四种拒绝策略：Abort/CallerRuns/Discard/DiscardOldest
- ForkJoinPool：工作窃取算法

### 理论知识：Virtual Threads（Java 21 重磅特性）
- 原理：用户态线程、Continuation、Carrier Thread（平台线程）调度
- 与传统线程对比：1000 平台线程 ~1GB 内存 vs 1000 虚拟线程 ~10MB
- 适用场景：高 IO 密集型（HTTP 请求、数据库调用、RPC 调用）
- 局限：CPU 密集型不适用、synchronized 会 pin 住 Carrier Thread
- Spring Boot 3.2+ 配置：`spring.threads.virtual.enabled=true`

### 源码阅读清单
- `java.util.concurrent.ThreadPoolExecutor.execute()` — 线程池核心方法
- `java.util.concurrent.ThreadPoolExecutor.Worker` — Worker 内部类
- jdk.internal.vm 包 — Virtual Threads 底层（了解即可，非公开 API）

### 实践任务
1. 手写 Mini-ThreadPool（`MiniThreadPool.java`，~350 行）：支持核心参数、队列、拒绝策略
2. 用 Virtual Threads 重写 Tomcat 请求处理：对比 100/1000/10000 并发的内存和 RT
3. JMH 压测：`Executors.newVirtualThreadPerTaskExecutor()` vs `Executors.newFixedThreadPool(200)`

### 输出
- 博客：《Virtual Threads 实战：从线程池到虚拟线程的性能跃升》

### 检验标准
- [ ] 能手写 ThreadPoolExecutor 的核心 execute 方法
- [ ] 能解释 Virtual Threads 为什么不适合 CPU 密集型
- [ ] 能用 Virtual Threads 改造一个 Spring Boot 应用并压测对比

---

## Week 5：集合框架源码

### 理论知识
- HashMap：JDK 7 头插法（死循环风险）→ JDK 8 尾插法 + 红黑树化
- 扩容：1.7 rehash vs 1.8 高位运算（`(e.hash & oldCap) == 0`）
- ConcurrentHashMap：JDK 7 Segment 分段锁 → JDK 8 CAS + synchronized
- LinkedHashMap：双向链表维护插入/访问顺序 → LRU 实现
- ArrayList：`grow()` 扩容 1.5 倍，`System.arraycopy()`

### 源码阅读清单
- `java.util.HashMap.putVal()` / `resize()` / `treeifyBin()`
- `java.util.concurrent.ConcurrentHashMap.putVal()` / `transfer()` / `addCount()`
- `java.util.LinkedHashMap.afterNodeInsertion()` / `afterNodeAccess()`

### 实践任务
1. 手写 LRU Cache（`LRUCache.java`，~100 行）：基于 LinkedHashMap，O(1) get/put
2. 手写 LRU Cache v2（`LRUCacheV2.java`，~150 行）：基于 HashMap + 双向链表，不依赖 LinkedHashMap
3. 用 JMH 压测：HashMap vs ConcurrentHashMap 在不同线程数下的吞吐量

### 输出
- 博客：《HashMap 的 8 次进化：从 1.0 到 21 的设计变迁》

### 检验标准
- [ ] 能手绘 HashMap 1.8 的 putVal 完整流程
- [ ] 能解释 ConcurrentHashMap 扩容时多线程如何协作
- [ ] 能手写 O(1) 的 LRU Cache

---

## Week 6：I/O 模型与 Netty 入门

### 理论知识
- BIO：`ServerSocket.accept()` 阻塞，一连接一线程
- NIO：Channel/Buffer/Selector，多路复用
- AIO：异步回调，Windows IOCP / Linux 模拟
- 零拷贝：`FileChannel.transferTo()` → `sendfile()` 系统调用
- Reactor 模式：单 Reactor 单线程 → 单 Reactor 多线程 → 主从 Reactor

### 源码阅读清单
- `java.nio.channels.Selector.open()` — Selector 创建
- `io.netty.channel.ChannelPipeline` — 责任链模式
- `io.netty.channel.nio.NioEventLoop` — 事件循环
- `io.netty.buffer.PooledByteBufAllocator` — 内存池

### 实践任务
1. 手写 NIO HTTP Server（`NioHttpServer.java`，~200 行）：基于 ServerSocketChannel + Selector
2. 手写 Netty HTTP Server（`NettyHttpServer.java`，~100 行）：ChannelInitializer + 自定义 Handler
3. 手写 Netty RPC 调用（`RpcClient.java` + `RpcServer.java`，~300 行）：LengthField 编解码 + 反射调用

### 输出
- 博客：《从 BIO 到 Netty：Java I/O 模型演进之路》

### 检验标准
- [ ] 能手绘 Netty 的 ChannelPipeline 职责链结构
- [ ] 能解释 Netty 如何实现零拷贝
- [ ] 能用 Netty 完成一个简单的 RPC 通信

---

## Phase 1 总结

### 核心交付物
- [ ] MiniAQS + MiniReentrantLock（~450 行）
- [ ] MiniThreadPool（~350 行）
- [ ] LRUCache × 2 版本（~250 行）
- [ ] NioHttpServer + Netty RPC（~600 行）
- [ ] 5 篇深度博客

### 与 my-xhs 的关联点（预告）
- Week 2 的 GC 知识 → 理解 my-xhs 的 JVM 配置
- Week 4 的线程池 → 理解 Tomcat connector 线程池
- Week 5 的 ConcurrentHashMap → 理解 my-xhs 缓存的 ConcurrentHashMap 实现
- Week 6 的 Netty → 理解 my-xhs-gateway 的底层网络模型
