# 05. OOM 前看内存,GC 频繁看面板 — JVM 与内存

> 🟢 使用域 | 覆盖: jvm/memory/dashboard/heapdump/logger/sysprop
> 读者处境: 内存报警、GC 频繁、接口整体变慢——先看 JVM 整体健康,再决定要不要导出堆。

### 1. "一屏全局健康" — dashboard

场景: 接口全面变慢,想 5 秒内看全貌。

- `dashboard`: 实时面板(monitor200/DashboardCommand.java:39 `@Name("dashboard")`)— 上半: 线程表格(含 %CPU),下半: 内存/GC/runtime/tomcat 四格
- `dashboard -i 2000`: 刷新间隔 2s(默认 5s)
- `dashboard -n 10`: 刷新 10 次自动停
- 退出: Ctrl-C(q)

关键设计: 面板是**周期性采样刷新**(源码 AR-4: java.util.Timer + DashboardTimerTask)——它的 %CPU 与 `thread` 命令同一套采样器(复用同一个 ThreadSampler 实例,跨 tick 差值);Tomcat 格子的 QPS/RT 来自 **localhost:8006 的 HTTP 轮询**(AR-4 细节,非 JMX)。

生产注意: dashboard 常驻有持续开销,排查期间用,结束即退出(Ctrl-C)。

### 2. "内存到底怎么分布的" — memory / jvm

场景: dashboard 看到 heap 涨,要看细节。

- `memory`: heap/nonheap 各内存池 + BufferPool 明细(monitor200/MemoryCommand.java:30 `@Name("memory")`)
- `jvm`: 9 块信息(monitor200/JvmCommand.java:24 `@Name("jvm")`)
  - RUNTIME: 启动参数/classpath/启动时间
  - GARBAGE-COLLECTORS: 每个 GC 的 count/time
  - MEMORY-MANAGERS / MEMORY: 堆非堆用量
  - THREAD: 线程数 + **DEADLOCK-COUNT**(真死锁数,JvmCommand.java:193-200)
  - FILE-DESCRIPTOR: 文件句柄(连接泄漏排查)
- `heapdump --live-only /tmp/h.hprof`: 导出堆(排 OOM 必备)

关键设计: `jvm` 的数据全部来自 `ManagementFactory` 的 8 个 MXBean 字段——[Java: MXBean = JMX 标准管理接口,Runtime/ClassLoading/Compilation/GarbageCollector/MemoryManager/Memory/OperatingSystem/Thread 是 java.lang.management 的核心管理接口]——所以它能看 DEADLOCK-COUNT 而 `thread -b` 不能互相替代:`jvm` 是"系统快照",`thread -b` 是"锁争用热力",两个命令回答两个问题。

生产注意: 堆快照导出大对象实例会卡顿(要遍历堆),选低峰期;`--live-only` 只导存活对象,分析泄漏必加。

### 3. "不用重启调日志和参数" — logger / sysprop / vmoption

场景: 线上偶发报错,日志级别是 INFO,报错细节被吞了。

- `logger`: 列出所有 logger 及级别(common/logger/LoggerCommand.java:40 `@Name("logger")`)
- `logger --name ROOT --level DEBUG`: 临时把日志级别调低(logback/log4j2 都支持)
- `sysprop`: 系统属性查看/修改(basic1000/SystemPropertyCommand.java:19)
- `sysenv` / `vmoption`: 环境变量 / VM 参数(含 `-Xmx` 等,可在线修改)

关键设计: logger 命令不走 JMX,而是**直接操作日志框架的上下文**(LogbackHelper/Log4j2Helper,源码在 command/logger/)——修改立即生效且**不重启进程**,这是线上抓现场的神器。

生产注意: 调级别抓完现场要调回;`vmoption` 改堆大小等参数生效但要谨慎(部分参数立即生效,部分需要 GC 后才生效)。

---

跨域桥: dashboard 面板数据 = AR-4(ThreadSampler 复用 + Timer + Tomcat 轮询 + MemoryCommand);jvm 9 块 = AR-4 JvmCommand;heapdump 的 MXBean 链路 = AR-4。

---

**OpenJDK 关联**:  [OpenJDK 域 33 JMX — outlines/33-jmx-management/] — 8 个 MXBean 字段的 JDK 侧实现;**另见** [OpenJDK 域 37 Heap Dumper — outlines/37-heap-dumper/] — heapdump 的 hprof 格式底层。

### 核心悬念

**"面板里的 QPS/RT 真是 JMX 给的吗?"** — 不是。它是 arthas 用 HTTP 轮询 localhost:8006 从 Tomcat 自己的接口"讨"来的——这解释了为什么非 Tomcat 应用面板上根本没有那格。

> → [AR-4 篇 2](../ar4-dashboard/02-dashboard-data.md)
