# 10 JVM 云原生架构

> 这不是"JVM 调优理论课"，而是**生产排障保命技能清单**——容器环境下的 JVM 问题排查和性能诊断。

## 讲什么

### 必备技能（Must Have）

| 工具 | 场景 | 内容 |
|------|------|------|
| **JMX** | 运行时监控 | MBean 体系、自定义 MBean、JConsole/JVisualVM 连接 Docker 容器内的 JVM |
| **JMap** | 内存分析 | Heap dump、Histogram（对象统计）、Metaspace 分析 |
| **JStack** | 线程诊断 | 死锁检测、死循环定位、线程池饥饿、WAITING/TIMED_WAITING 分析 |

### 进阶技能（Should Have）

| 工具 | 场景 | 内容 |
|------|------|------|
| **Java Debugger** | 远程调试 | JDWP 协议、Docker 容器内远程调试配置 |
| **JFR** (Java Flight Recorder) | 性能诊断 | 零开销持续采集、热点方法分析、GC 事件分析、I/O 瓶颈定位 |
| **MAT** (Memory Analyzer Tool) | 内存泄漏 | 引用链分析、Dominator Tree、Leak Suspects |

### 容器环境特有问题

- **Docker 内存限制 vs JVM 堆大小**：`-Xmx` 不能超过容器限制，否则 OOM Killer
- **JVM 与 CGroup**：JDK 8u191+/JDK 11+ 才原生支持容器内存感知
- **PID 1 问题**：容器内 JVM 作为 PID 1 时的信号处理陷阱

## 涉及的代码仓库

> 无独立仓库，主要是 Shopizer 项目的 JVM 调优实战。
