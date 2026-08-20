# 03. jvm/memory: 信息快照命令的数据块 — 与面板共享的数据源

> 🟡 Working | 12 KP 中的 2 个(jvm 9 块/memory 内存池)
> 读者处境: dashboard 是"持续刷新",jvm/memory 是"拍一张照片"——同一批 MXBean,两种消费方式。

### 1. "jvm 的 9 块数据" — JvmCommand 数据块清单

场景: `jvm` 命令输出 9 个分组,每个分组对应一个 MXBean 或一类系统信息。

- `JvmCommand.process`(monitor200/JvmCommand.java:24 起)依次填充(JvmCommand.java:40-68 调度):
  1. **RUNTIME**(`addRuntimeInfo` :86-107): `RuntimeMXBean`——MACHINE-NAME/启动时间/SPEC/VM/INPUT-ARGUMENTS/CLASS-PATH/BOOT-CLASS-PATH/LIBRARY-PATH
  2. **CLASS-LOADING**(:109-115): `ClassLoadingMXBean`——已加载/未加载/总数
  3. **COMPILATION**(:117-126): `CompilationMXBean`(无编译监控则跳过)
  4. **GARBAGE-COLLECTORS**(:128-138): 每个 `GarbageCollectorMXBean` 的 name/collectionCount/collectionTime 各成一项
  5. **MEMORY-MANAGERS**(:140-150): `MemoryManagerMXBean` 列表
  6. **MEMORY**(:152-163): `MemoryMXBean` heap/nonheap 的 init/used/committed/max + PENDING-FINALIZE-COUNT
  7. **OPERATING-SYSTEM**(:175-182): `OperatingSystemMXBean`
  8. **THREAD**(:184-191): 线程数 + **DEADLOCK-COUNT**
  9. **FILE-DESCRIPTOR**(:70-74): 文件句柄(连接泄漏排查)
- MXBean 来源: 8 个字段——`getRuntimeMXBean()/getClassLoadingMXBean()/getCompilationMXBean()/getGarbageCollectorMXBeans()/getMemoryManagerMXBeans()/getMemoryMXBean()/getOperatingSystemMXBean()/getThreadMXBean()`(JvmCommand.java:29-37)
- **DEADLOCK-COUNT**(:193-200): `threads.findDeadlockedThreads()` 返回环内线程数(null→0)——AR-3 篇 3 的"确诊工具"

关键设计: [模式: 外观/编排(jvm 命令=MXBean 数据的排版器)+ 数据源与消费方分离] **9 块 = 8 个 MXBean 字段的编排**: jvm 命令本身不采集,是"MXBean 数据的排版器"——每个 addXxx 一个分组,与 dashboard 的 addXxx 一一对应(dashboard 的 addRuntimeInfo/addGcInfo 就是它的子集)。信息架构上: dashboard 是"持续的 subset",jvm 是"全量的快照"。

### 2. "memory 的内存池" — MemoryPoolMXBean

场景: `memory` 输出 heap 各代(Eden/Survivor/Old)+ nonheap(CodeCache/Metaspace)的 used/committed/max。

- `MemoryCommand.memoryInfo()`(monitor200/MemoryCommand.java:42 起): `ManagementFactory.getMemoryPoolMXBeans()` 遍历各内存池 + `getMemoryMXBean().getHeapMemoryUsage()` 汇总 + BufferPool
- 输出三组: HEAP(eden/survivor/old)/NON-HEAP(metaspace/code-cache/compressed-class-space)/BUFFER-POOL(direct/mapped)
- [Java: `MemoryPoolMXBean` 每代一个实例(名称如 "G1 Eden Space"),`getUsage()` 返回 init/used/committed/max——committed vs used 之差是"已分配未用"内存,排查"heap 到底涨没涨"看这两列]

关键设计: **committed 与 used 的语义差**是内存排查的关键: used 是对象占用,committed 是 JVM 已从 OS 拿的——committed 大 used 小 = 分配了没用(常见于堆被撑大后不缩);OOM 看 max(物理上限),性能看 committed。

### 3. "快照 vs 面板" — 三种消费方式对比

```
thread/memory/jvm   = 一次性快照(命令结束即完)
dashboard           = 周期性快照(复用 sampler 状态)
```
- 共同数据源: ThreadUtil(枚举)/ThreadSampler(CPU)/MemoryCommand(内存池)/GarbageCollectorMXBean(GC)
- 差异只在"采样时机与次数": 命令按需一次,面板按 interval 循环——**采集逻辑 0 复制**

关键设计: 这个"数据源与消费方分离"是 arthas 命令架构的隐性规则: 采集(util/command 静态方法)与呈现(命令/面板)分层——后续新增命令只要组合既有数据源。

---

跨域桥: DEADLOCK-COUNT = AR-3 篇 3(与 -b 的分工);MemoryPoolMXBean 的 committed/used = AR-0 篇 5 的内存解读;addRuntimeInfo/addGcInfo 的聚合 = 上一篇 dashboard 数据。

---

**OpenJDK 关联**:  [OpenJDK 域 33 JMX — outlines/33-jmx-management/] — MemoryPoolMXBean;**另见** [OpenJDK 域 09 Memory 核心 — outlines/09-memory-core/] — 堆结构(committed/used 的 JDK 侧语义)。

### 核心悬念

**"信息采集看完了——但表达式引擎凭什么能在这些命令里通用?"** — watch 的条件、tt 的搜索、ognl 的直调,共用同一个 ThreadLocal 弱引用池——它不只是一层封装,是防 ArthasClassLoader 泄漏的最后防线。

> → [AR-5 篇 1](../ar5-ognl/01-express-engine.md)
