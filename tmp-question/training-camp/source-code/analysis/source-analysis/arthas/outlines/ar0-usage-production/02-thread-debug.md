# 02. CPU 飙到 100% 怎么 1 分钟定位 — 线程排查

> 🟢 使用域 | 覆盖: thread 全命令
> 读者处境: 线上 CPU 报警,你不知道哪个线程在忙、哪些线程堵死了、有没有死锁。

### 1. "谁在烧 CPU" — thread 与 -n

场景: 4 核机器 CPU 满载,先看全局。

- `thread`: 全部线程表格,10 列 ID/NAME/GROUP/PRIORITY/STATE/%CPU/DELTA_TIME/TIME/INTERRUPTED/DAEMON(ViewRenderUtil.java:109-126,源码域 AR-3)
- `thread -n 3`: **按 CPU 使用率倒序取前 3**(ThreadCommand.java:117-131 分派)
- `thread <id>`: 单线程完整调用栈(定位到业务代码行)
- 命令定义: monitor200/ThreadCommand.java:36 `@Name("thread")` `@Summary("Display thread info, thread stack")`

关键设计: **%CPU 是"两次采样差值/间隔"算出来的瞬时值**——[Java: ThreadMXBean.getThreadCpuTime 只返回累计值,没有瞬时 API;两次读数之差 ÷ 墙钟间隔 = 窗口平均占用,这是无瞬时 CPU API 下的标准解法](ThreadSampler.java:121,源码 AR-3)——所以命令会先采样、等一个间隔、再采样,耗时约 1-2 秒,这是特性不是卡顿。TIME 列才是累计 CPU 时间。

生产注意: `-n` 默认 5;线上第一次跑建议 `thread -n 3` 快速定位,再用 `thread <id>` 看栈。

### 2. "谁堵住了大家" — thread -b 与 --state

场景: 接口全部超时,线程堆在 BLOCKED,像死锁又不像。

- `thread -b`: 找**被最多线程等待且有人持有**的锁(ThreadUtil.findMostBlockingLock,ThreadUtil.java:99-159,源码 AR-3)——输出红字 `" <---- but blocks N other threads!"`(ThreadUtil.java:238)
- `thread --state BLOCKED`: 只看 BLOCKED 状态的线程(ThreadCommand.java:148-159 过滤)
- `thread --state WAITING`: 常见于连接池取不到连接

关键设计: `-b` 不是 JDK 的 `findDeadlockedThreads`(那只能找"循环等待"的真死锁)——它是**锁争用统计**:数每个锁被多少线程等待,挑出最热的锁和持锁人。生产里"假死锁"(池子耗尽)比真死锁常见得多,`-b` 才是第一工具。

生产注意: 真死锁另看 `jvm` 的 DEADLOCK-COUNT(JvmCommand.java:193-200);`-b` 输出可能很长,配合 `thread --state` 交叉验证。

### 3. "内置线程也在忙" — 内部线程与守护线程

场景: 业务线程都不忙,但 CPU 还是高——GC/JIT 在忙。

- `thread -i 1000 -n 3`: 指定采样间隔(默认 200ms,`-i` 单位 ms)
- 内部线程(GC/Compiler)也参与采样: ThreadSampler 含 `getInternalThreadCpuTimes`(ThreadSampler.java:157,源码 AR-3)
- 看到 GC 线程 %CPU 高 → 转 `dashboard` 看 GC 频率(下一篇/AR-4)

关键设计: 采样覆盖 Hotspot 内部线程——CPU 高但不在这张表里,说明是 JVM 自身(GC 线程、JIT 编译线程)在烧,排查方向立刻转向内存/类加载。

生产注意: 排查 CPU 问题按序: `thread -n 3` → `thread <id>` → 栈里是业务代码则 `jad`/`trace`(下一篇),是 GC 线程则转内存分析。

---

跨域桥: %CPU 差值采样 = AR-3 ThreadSampler 两次采样;`-b` 锁统计 = AR-3 findMostBlockingLock;表格渲染 = AR-3 ViewRenderUtil。

---

**OpenJDK 关联**:  [OpenJDK 域 33 JMX — outlines/33-jmx-management/] — thread 数据全部来自 ThreadMXBean;**另见** [OpenJDK 域 17 Threads — outlines/17-threads/] — 线程状态机/线程组模型的底层。

### 核心悬念

**"%CPU 是瞬时值吗?"** — 你看到的 87.5% 其实是"过去 200ms 的平均占用率"——两次读累计 CPU 时间做差,再除以间隔。这个"故意等 1 秒"的设计,是 arthas 最容易被误认为卡顿的行为。

> → [AR-3 篇 2](../ar3-thread/02-cpu-sampling.md)
