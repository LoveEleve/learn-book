# 《Modern Concurrency in Java》—— 完整目录（中文）

> 8章 | Java虚拟线程(Virtual Threads)为核心 | 结构化并发/ScopedValue/响应式定位/SpringBoot/Quarkus

---

### 第1章 Introduction 简介
- Java 线程简史（Java1.0线程起源/启动线程/隐性开销/最多能创建多少线程）
- 大规模应用资源效率 / 并行执行策略
- Executor 框架介绍 / 现存挑战
- 超越基础线程池（缓存亲和性/任务分发/工作窃取算法）
- CompletableFuture 可组合性 / 异步编程全新范式
- 响应式框架的弊端 / 虚拟线程愿景
- 和现有代码无缝集成 / 虚拟线程与平台线程 / 阻塞操作智能处理

### 第2章 Understanding Virtual Threads 理解虚拟线程
- Java 中的两类线程 / 和平台线程核心区别 / 环境准备 / 创建虚拟线程
- 吞吐量与可扩展性 / 虚拟线程高扩展性底层原理
- 虚拟线程底层运行原理（栈帧与内存管理/载体线程与OS/阻塞操作处理/透明无感知）
- 简化异步操作 / 结构化并发价值 / Semaphore限流管控
- 虚拟线程局限性（Pinning线程钉住/ReentrantLock解决/本地方法Pinning/ThreadLocal难题）
- 监控手段（ThreadLocal监控/Pinning监控/jcmd线程快照/HotSpotDiagnosticsMXBean）
- 迁移虚拟线程实战建议

### 第3章 The Mechanics of Modern Concurrency 现代并发内部机制
- 线程池（为什么需要/手写简易Java线程池）
- Executor 执行器框架 / Callable & Future
- ForkJoinPool（虚拟线程为什么关联）
- Continuation 续体（协程底层机制）
- 从零实现简易虚拟线程 / 虚拟线程与IO轮询

### 第4章 Structured Concurrency 结构化并发
- 非结构化并发挑战 / 结构化并发优势
- StructuredTaskScope API / 作用域与子任务关系和生命周期
- Joiner 等待策略 / 常用等待策略 / 异常处理
- 配置项 / Custom Joiners / 内存一致性 / 嵌套作用域 / 可观测性

### 第5章 Scoped Values 域值（替代ThreadLocal）
- 上下文传递痛点（参数污染/接口脆弱性/耦合与可测试性）
- ThreadLocal 局限 / 轻量级上下文共享
- ScopedValue 核心组件 / 与结构化并发 / 性能考量
- 易用性与API设计 / 迁移至ScopedValue

### 第6章 虚拟线程时代下响应式Java的定位
- Java响应式编程 / 阻塞IO与非阻塞IO / 事件驱动架构
- 异步API / 响应式流规范 / 背压
- 响应式编程优缺点

### 第7章 运用虚拟线程的主流现代框架
- Spring Boot 手动配置
- Quarkus
- Jakarta EE

### 第8章 Conclusion and Takeaways 总结与核心收获
