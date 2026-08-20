# 02. %CPU 是怎么算出来的? — 两次采样差值法

> 🔴 Deep | 19 KP 中的 4 个(采样/差值/内部线程/TopN)
> 读者处境: 你敲 `thread -n 3`,命令"卡"了 1-2 秒才出结果——这不是卡顿,是它故意等一个间隔。

### 1. "为什么等 1 秒" — 采样差值原理

场景: `ThreadMXBean.getThreadCpuTime(id)` 能拿到累计 CPU 时间——但"瞬时 CPU 百分比"没有 API。

- `ThreadSampler.sample(Collection<ThreadVO>)`(monitor200/ThreadSampler.java:34): 
  - **第一次调用**: 记 `lastSampleTimeNanos = System.nanoTime()`(:39-77)+ 每个线程 `threadMXBean.getThreadCpuTime(id)` 存 map,`setTime(cpu/1_000_000)`(ns→ms)
  - **间隔后第二次**: 再取 `getThreadCpuTime` → `delta = time2 - time1`(:107-111;返回 -1 视为没变化)
  - **CPU% = `Math.rint(delta * 10000.0 / sampleIntervalNanos) / 100.0`**(:121)——delta(纳秒)÷ 采样间隔(纳秒)= 该间隔内的 CPU 占用率,保留 2 位小数
  - 按 delta 降序排序(:126-139),写回 VO 的 cpu/deltaTime/time 字段(:141-150)
- 调用方两遍采样: `sample → pause(sampleInterval) → sample`(ThreadCommand.java:165-169,默认间隔 200ms)
- [Java: getThreadCpuTime 返回该线程累计消耗的 CPU 纳秒数(含系统态)。两次读差 ÷ 墙钟间隔 = 该窗口的平均 CPU 占比——数学上等价于"瞬时采样"的平滑版]

- **双排序**: 首次采样按累计 time 降序(:64),二次按 delta 降序(:128)——基线时没有"变化量"可排,只能排累计;真正有用的排序在第二次
- **降级设计**: `hotspotThreadMBeanEnable` 开关(ThreadSampler.java:25-26)——`getInternalThreadCpuTimes` 失败(某些 JVM 不支持)时置 false 不再尝试,内部线程列缺省但不崩

关键设计: [模式: 两阶段采样(基线+差值)+ 容错降级(hotspotThreadMBeanEnable)] **差值法解决"没有瞬时值"**: CPU 时间只有累计值,"某一秒用了多少"必须用两次读数的差来近似。间隔越短越接近瞬时但噪声越大(线程调度抖动),200ms 是经验平衡。**第一次调用是基线(不算数)**——所以 `thread` 命令"先测一次、等、再测一次"是故意行为。

### 2. "内部线程也在烧 CPU" — HotspotThreadMBean

场景: 业务线程全空,但 CPU 100%——GC/JIT 线程忙,它们不在 enumerate 的线程列表里怎么办?

- `ThreadSampler.getInternalThreadCpuTimes()`(:157-181): `ManagementFactoryHelper.getHotspotThreadMBean().getInternalThreadCpuTimes()`——Hotspot 内部线程(GC/Compiler/Watcher)的 CPU 时间
- 以 **id=-1** 的 ThreadVO 加入采样集合(:172),同样参与差值计算与排序
- 内部线程名字形如 "GC Thread#0"/"C2 CompilerThread0"——CPU 高时它们会出现在表格前列
- [Java: `com.sun.management.internal.HotspotThreadMBean`(sun 内部接口)——只有它能看到 JVM 内部线程的 CPU,业务线程的 getThreadCpuTime 看不到]

关键设计: **CPU 排查的盲区补全**: `thread` 表格包含内部线程 = "CPU 高但业务线程不忙"的场景直接指向 GC/JIT;`-i` 参数可调采样间隔(默认 200ms),排查内部线程时用 `-i 1000` 更稳(AR-0 篇 2 §3)。

### 3. "Top N 与详情" — 排序/截断/单线程深度信息

场景: 1000 个线程,只要 CPU 最高的 3 个;再点开某一个要看完整栈和锁。

- `processTopBusyThreads`(ThreadCommand.java:184-197): 两遍采样(含 `-i` 间隔)→ 已按 delta 降序 → `subList(0, limit)`(:190-197,`-n` 的默认 5)
- `processThread`(:199-219): `threadMXBean.getThreadInfo(tids, lockedMonitors, lockedSynchronizers)`(:206)——**深度 ThreadInfo**: 除栈外还带 monitor 持有/等待 + synchronizer;与采样数据合并成 `BusyThreadInfo`(:212-219)
- [Java: `getThreadInfo(id, lockedMonitors, lockedSynchronizers)` 比 `getThreadInfo(id)` 多返回 ObjectMonitor 和 AbstractOwnableSynchronizer 锁明细——栈里能标出 "locked: xxx"(持锁行)与 "waiting on: xxx"(等待行)]

关键设计: **两遍采样的信息不对称**: CPU 排序要两次读(慢),锁/栈是静态快照(快)——Top N 先只对采样过的 VO 排序截断,再对截出的少数线程做深度 getThreadInfo,避免 1000 个线程全量深度读取的开销。

---

跨域桥: 采样间隔/`-i`/`-n` 的使用 = AR-0 篇 2;dashboard 复用**同一个 ThreadSampler 跨 tick 差值**(AR-4);getThreadInfo 深度锁信息 = 下一篇 findMostBlockingLock 的数据基础。

---

**OpenJDK 关联**:  [OpenJDK 域 33 JMX — outlines/33-jmx-management/] — ThreadMXBean.getThreadCpuTime 的 JDK 侧实现(OS 层 CPU 时间);**另见** [OpenJDK 域 17 Threads — outlines/17-threads/] — 内部线程(HotspotThreadMBean 的来源)。

### 核心悬念

**"CPU 排名有了——'堵路'的那把锁怎么找?"** — jstack 只能告诉你死锁,arthas 却能指出"就是这把锁堵了 15 个线程"——它把全 JVM 的锁等待做了一次统计: 每个锁被几个线程等着,等得最多的就是罪魁。

> → [03-blocking-deadlock.md](03-blocking-deadlock.md)