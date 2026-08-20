# 02. 面板里每个数字的来处 — 数据聚合与 Tomcat 探测

> 🔴 Deep | 12 KP 中的 4 个(复用采样/内存/GC/运行时/Tomcat HTTP 桥)
> 读者处境: dashboard 一屏四个格子(线程/内存/GC/运行时+Tomcat)——每个数字都来自哪里?最反直觉的是: Tomcat 的 QPS 不是 JMX 给的,是"轮询"来的。

### 1. "%CPU 不用重新采两次样" — 跨 tick 状态复用

场景: `thread` 命令要"采样→等→采样"两次才算得出 CPU%,dashboard 每 5 秒刷新一次,难道每次都来两遍?

- **不——**`threadSampler` 是**实例字段**(DashboardCommand.java:224 附近构造一次),每个 tick 只 `sample()` 一次(:242)
- 第一次 tick 是基线;此后**每个 tick 都是"上次 vs 本次"的差值**(ThreadSampler 内部 `lastCpuTimes` 状态保持,AR-3 篇 2 的机制)
- 所以: dashboard 的 %CPU 是"过去 5 秒平均",thread 的 %CPU 是"过去 200ms 平均"——**同一算法,不同窗口**

关键设计: [模式: 状态复用/享元(同一 ThreadSampler 跨 tick)+ HTTP 适配器(Tomcat jmx_http 桥)+ 聚合器(dashboard=数据源组合)] **状态在 sampler 里,不在调用方**: `thread` 命令两遍采样是"临时用一下",dashboard 是"常驻用"——同一组件两种用法: 临时配对(thread)vs 持续复用(dashboard)。这就是 AR-3 篇 2 说的"复用同一个 threadSampler 跨 tick 差值"。

### 2. "内存与 GC" — memoryInfo 与 GcInfoVO

场景: 下半格左上是内存,左下有 GC。

- 内存: `MemoryCommand.memoryInfo()`(monitor200/MemoryCommand.java:42 起)——heap/nonheap 各 `MemoryPoolMXBean` 的 used/committed/max + BufferPool;与 `memory` 命令**同一个方法**(命令复用: DashboardCommand.java:245 直接调)
- GC: `addGcInfo`(DashboardCommand.java:148-157)——遍历 `GarbageCollectorMXBean` 聚合 `GcInfoVO{name, count, time}`;渲染成 `gc.PS MarkSweep.count` / `gc.PS MarkSweep.time(ms)`(DashboardView.java:89-98)
- 运行时: `addRuntimeInfo`(:135-146)——`getSystemLoadAverage`/`Runtime.availableProcessors`/`getUptime` + os/Java 系统属性

关键设计: **聚合器而非数据源**: dashboard 不实现任何采集逻辑,全部复用 thread(memory/jvm)命令的方法——**一份数据源,两个出口**(命令 vs 面板)。这是"面板 = 定时聚合器"架构的核心: 新增指标 = 加一个 addXxx 块,不碰其他命令。

### 3. "Tomcat 的 QPS 是轮询来的" — localhost:8006 HTTP 桥

场景: 面板右下角的 QPS/RT——Tomcat 在 JVM 里,MBean 也够得着,为什么用 HTTP?

- `addTomcatInfo`(DashboardCommand.java:159-216): 先探测 `NetUtils.request("http://localhost:8006")`(:161,**失败则整个 Tomcat 块不显示**——不是报错)
- 再请求 `/connector/threadpool` + `/connector/stats`(:167-168)→ fastjson2 解析 JSON(:172)
- `SumRateCounter`(:50-53 定义,:181-190 使用)算 QPS/error/收发字节速率;**`RT = processingTime / requestCount`**(:187)
- **速率算法**(util/metrics/SumRateCounter.java:28-36): `update(value)` 先算 `value - previous` 增量,再喂给内部 RateCounter 求窗口平均——注释示例: 5 秒请求数 267→457→635→894→1398,平均速率 (190+178+259+504)/4 = **282/s**——与 AR-3 的 CPU 差值采样是同一思想: 增量 ÷ 窗口
- `NetUtils.request`(core/util/NetUtils.java:33): `HttpURLConnection` GET,超时 1s/3s
- [Tomcat: `localhost:8006` 是 Tomcat 的 JMX HTTP 适配器端口(非 arthas 的)——Tomcat 9 的 `jmx_http` 协议,输出 connector 统计的 JSON。即: arthas 通过 **HTTP 桥接**读 Tomcat 的运行时统计,不是直接读 MBean]

关键设计: **外部进程信息 = 网络协议,内部信息 = 直接 API**——Tomcat 虽然是"同一个 JVM",但 arthas 定位它靠的是 Tomcat 暴露的 HTTP 接口(端口 8006)——因为 Tomcat 的统计是它自己的服务,通过其公开协议读最稳(解耦版本);失败静默降级(不显示该块),面板不因 Tomcat 缺席而崩。这就是为什么 v1 规划说"基于 Instrument 计数器"是错的——真实来源是 HTTP 轮询([Tomcat: jmx_http 适配器]见 §3)。

### 4. 面板渲染

- `DashboardView.draw`(view/DashboardView.java:23): 上半线程表(`ViewRenderUtil.drawThreadInfo` :66,AR-3 篇 1 同款渲染)+ 下半田字格(memory+GC / runtime+tomcat,:67-68)

关键设计: **渲染复用**: 线程表与 `thread` 命令共用 ViewRenderUtil——再次印证"聚合器"架构: 面板的每一格都能在某个命令的渲染器里找到对应实现。

---

跨域桥: ThreadSampler 状态复用 = AR-3 篇 2;thread 表格渲染 = AR-3 篇 1;memory/jvm 命令数据块 = 下一篇;AR-0 篇 5 的 dashboard 使用体验(每 5 秒刷新/QPS 数字)在本篇找到源码答案。

---

**OpenJDK 关联**:  [OpenJDK 域 33 JMX — outlines/33-jmx-management/] — MXBean 全家族;**另见** [OpenJDK 域 25 GC Framework — outlines/25-gc-framework/] 与 [OpenJDK 域 26 G1 GC — outlines/26-g1-gc/] — GarbageCollectorMXBean 背后的 GC 计数/耗时。

### 核心悬念

**"面板的数据都齐了——那 jvm/memory 命令的完整数据块,和面板是什么关系?"** — 一个是全量快照(9 块),一个是持续的 subset——共享同一批 MXBean,只是消费方式不同。

> → [03-jvm-memory-commands.md](03-jvm-memory-commands.md)
