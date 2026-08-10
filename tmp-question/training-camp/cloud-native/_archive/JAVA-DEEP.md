# 「一入 Java 深似海」系列学习指南

> segfault-lessons 中最深度的 Java 核心系列，8 期 199 个 Java 文件。
> 这是训练营主体（架构+分布式）的**基本功补充**——缺了这些，Stage 1-4 讲的很多内容会看不懂。

---

## 一期：语言基础 + OOP + 函数式（30+ 文件）

**映射到**: Phase 1 工程地基

| Lesson | 内容 | 关键文件 | 为什么要学 |
|:------:|------|---------|-----------|
| 2 | 类名/Cloneable/枚举/内部类 | `ClassName.java`, `CloneableDemo.java`, `InnerClassDemo.java`, `IntegerDemo.java`, `StringDemo.java` | Java 基础，面试高频 |
| 3 | 泛型/方法重载 | `GenericTypeErasureDemo.java`, `GenericWildcardsTypeDemo.java`, `MethodArgumentsDemo.java` | 泛型擦除——理解 Spring `ResolvableType` 的前提 |
| 4 | 函数式编程 | `ConsumerDemo.java`, `FunctionDemo.java`, `PredicateDemo.java`, `SupplierDemo.java`, `StreamDemo.java` | 函数式接口——理解 Resilience4j 函数式 API、WebFlux Reactive 的前提 |

**代码路径**: `segfault-lessons/「一入 Java 深似海 」/代码/segmentfault/deep-in-java/stage-1/`

## 二期：集合框架

**映射到**: Phase 1/2

| 文件 | 知识点 | 为什么面试必问 |
|------|--------|--------------|
| `FailFastVsFailSafeDemo` | ArrayList modCount vs CopyOnWriteArrayList 快照 | ConcurrentModificationException 原理 |
| `IdentityHashMapDemo` | == 比较 vs equals() | HashMap 键的唯一性判断 |
| `WeakReferenceMapDemo` | WeakReference + ReferenceQueue + GC | 内存泄漏诊断基础 |
| `PriorityQueueDemo` | 小顶堆 | 任务调度、TopK 问题 |

## 三期：并发编程（31 文件）⭐

**映射到**: Phase 7 容错模式 + Phase 3 手写中间件

| 文件 | 知识点 | 跟训练营什么关系 |
|------|--------|----------------|
| `DeadLockDemo` | 循环等待四个必要条件 | 分布式死锁的本质 |
| `ProducerConsumerProblemDemo` | wait()/notify() + while 防虚假唤醒 | Kafka Producer/Consumer 模型 |
| `ReentrantLockDemo` | getHoldCount() 递归重入 + Condition | 分布式锁的 AQS 基础 |
| `CountDownLatchDemo` | 一次性计数器 | 启动时等待所有依赖服务就绪 |
| `CyclicBarrierDemo` | 可重用屏障 | ZooKeeper Quorum 投票模拟 |
| `AtomicDemo` | CAS/Unsafe/volatile | 无锁数据结构基础 |
| `ReadWriteLockDemo` | 读共享/写独占 | 缓存读写锁策略 |
| `SemaphoreDemo` | acquire/release | 连接池限流 |
| `CompletableFutureDemo` | 异步编排 | WebFlux 异步编程 |
| `ConcurrentHashMapDemo` | 分段锁实现 | 高并发容器选型 |

**代码路径**: `segfault-lessons/「一入 Java 深似海 」/代码/segmentfault/deep-in-java/stage-3/`

### 学习计划

| 天数 | 文件 | 做什么 |
|:----:|------|--------|
| Day 1 | DeadLockDemo, ProducerConsumerProblemDemo, SynchronizedDemo 系列 | 理解锁的底层语义 |
| Day 2 | ReentrantLockDemo, ReadWriteLockDemo, StampedLockDemo | 对比 synchronized 和 Lock |
| Day 3 | CountDownLatchDemo, CyclicBarrierDemo, SemaphoreDemo | 理解同步工具类 |
| Day 4 | AtomicDemo, ConcurrentHashMapDemo, CopyOnWriteArrayListDemo | 理解 CAS 和并发集合 |

**自测题**：
- CountDownLatch 和 CyclicBarrier 的区别？（一次性 vs 可重用）
- ReentrantLock 的 getHoldCount() 有什么用？（调试死锁）
- `wait()` 为什么必须用 `while` 而不是 `if`？（虚假唤醒）

## 四期：JMM + AQS + C POSIX Thread ⭐⭐

**映射到**: Phase 2 分布式理论 + Phase 8 JVM

**核心文件**:
- `HappensBeforeRelationshipDemo` —— Java 内存模型的可见性规则
- `AbstractQueuedSynchronizerDemo` —— J.U.C 所有锁的底层实现
- **C 语言 POSIX Thread 代码** —— 从 OS 层面理解线程

**为什么重要**：
- AQS 是 ZK 选举算法、Redisson 分布式锁、rpc-project ExchangeFuture 的底层基础
- JMM 是理解 volatile、synchronized、CAS 的前提
- POSIX Thread 对比 Java Thread —— 知道 JVM 线程就是 OS 线程（1:1 模型）

## 五期：类加载 ⭐

**映射到**: Phase 1 + Phase 8 JVM

| 文件 | 知识点 | 实际应用 |
|------|--------|---------|
| `ClassLoadingDemo` | 自定义 MyClassLoader + defineClass() vs loadClass() | Spring DevTools 热加载原理 |
| `ClassLoaderDemo` | AppClassLoader → ExtClassLoader → Bootstrap(null) | ClassLoader 隔离 |
| `ClassObjectDemo` | Object.class 和 int.class | 反射基础 |

**核心实验**: `myClassLoader.defineClass()`（绕过双亲委派）vs `classLoader.loadClass()`（遵循双亲委派）——同名 Class 的 == 比较结果是 false

## 六期：GC 调优

**映射到**: Phase 8 性能实战 + Stage 3 #05（ZGC 对比数据）

传统 GC + G1 调优。配合训练营 Stage 3 #05 的 ZGC 100 TPS vs G1 11.2 TPS 实测数据学习。

## 七期：反射 + APT + 字节码 ⭐⭐⭐

**映射到**: Phase 11 + 理解 Spring 底层原理

| 文件 | 知识点 | 跟训练营的关系 |
|------|--------|-------------|
| `RepositoryAnnotationProcessor` | 完整 APT 处理器 | **microsphere-annotation-processor 的蓝本** |
| `CGLibDemo` | CGLib 动态代理 | 理解 Spring AOP 的 proxy-target-class=true |
| `JavaDynamicProxyDemo` | JDK 动态代理 | 理解 Spring AOP 的 JDK Proxy 模式 |
| `JavaArrayDemo`, `JavaClassMemberDemo` | 反射 API | 理解 Spring BeanUtils/ReflectionUtils |
| `PersonBeanDemo`, `PersonIntrospectionDemo` | Java Beans 内省 | 理解 Spring BeanWrapper |

**核心实验**: `RepositoryAnnotationProcessor` 两阶段处理——
1. 处理阶段：扫描 @Repository 注解，提取泛型参数
2. 完成阶段（processingOver）：生成 `META-INF/crud-repos-mappings.properties`

**这就是 microsphere-java 的 `ConfigurationPropertyAnnotationProcessor` 的简化版。** 理解了这个，就理解了 Lombok/MapStruct 的原理。

## 八期：I/O + NIO（26 文件）⭐⭐

**映射到**: Phase 3 手写中间件（理解 Netty 的前置知识）

| 文件 | 知识点 | 对应 Netty |
|------|--------|----------|
| `SelectorServerDemo` | register(OP_ACCEPT) + selectedKeys + iterator.remove() | `EventLoopGroup` |
| `ChannelClientDemo` / `ChannelServerDemo` | configureBlocking(false) + finishConnect() | `NioSocketChannel` |
| `BufferDemo` | ByteBuffer position/limit/capacity + flip/clear | `ByteBuf`（自动扩容，PooledByteBufAllocator） |
| `FileMonitor` | 事件驱动文件变更检测 | - |

**NIO → Netty 对应**：
- Selector → EventLoopGroup
- ServerSocketChannel → NioServerSocketChannel
- SocketChannel → NioSocketChannel
- ByteBuffer → ByteBuf（自动扩容 + 池化）
- 手动 register SelectionKey → ChannelPipeline + ChannelHandler 链

## 学习路线总图

```
Session 1-2 (基础) → Phase 1 前（先打好 Java 基础）
Session 3 (并发)   → Phase 7 同时（理解锁和同步工具类）
Session 4 (JMM+AQS)→ Phase 2 同时（理解分布式一致性底层）
Session 5 (类加载)  → Phase 8 同时（理解 JVM）
Session 6 (GC)      → Phase 8 同时（配合 Stage 3 #05 实测数据）
Session 7 (APT+反射) → Phase 11（理解 Spring AOP/Lombok）
Session 8 (NIO)     → Phase 3 前（理解 Netty/RPC）
```

## 代码路径汇总

```
segfault-lessons/「一入 Java 深似海 」/代码/segmentfault/deep-in-java/
├── stage-1/  → 基础/OOP/函数式（30+文件）
├── stage-2/  → 集合框架
├── stage-3/  → 并发编程（31文件）
├── stage-4/  → JMM + AQS + C POSIX Thread
├── stage-5/  → 类加载
├── stage-6/  → GC 调优
├── stage-7/  → 反射 + APT + 字节码
└── stage-8/  → I/O + NIO（26文件）
```
