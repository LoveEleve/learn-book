# 03. thread -b 凭什么能"揪出"堵路的锁? — 锁争用热力检测

> 🔴 Deep | 19 KP 中的 3 个(findMostBlockingLock/dumpAllThreads 深度/高亮渲染)
> 读者处境: 接口全超时,线程一片 BLOCKED——`thread -b` 一秒钟指出"就是这把锁堵了 15 个线程"。它怎么知道的?jstack 做不到的它凭什么做到?

### 1. "数人头" — dumpAllThreads 深度快照 + 锁计数

场景: 死锁检测只能找"循环等待",但线上更常见的是"一堆线程等一把没人放的锁"。

- `ThreadUtil.findMostBlockingLock()`(core/util/ThreadUtil.java:99-159):
  - `threadMXBean.dumpAllThreads(objectMonitorUsage, synchronizerUsage)`(:100-101)——**全量深 dump**(每个线程的栈+锁明细,开销大,只在这里用)
  - 遍历所有线程: `info.getLockInfo()`(该线程正在**等待**的锁)→ 按 `System.identityHashCode(lock)` 计数 `blockCountPerLock`(:113-121)——**数出"每个锁被几个线程等着"**
  - 同时建 `ownerThreadPerLock`: 持有 monitor/synchronizer 的线程映射(:123-135)
  - 挑出 `blockCount 最大 && 有人持有` 的锁(:139-147,`ownerThreadPerLock.get(key) != null` 才候选)→ 返回 `BlockingLockInfo{threadInfo(持锁线程), lockIdentityHashCode, blockingThreadCount}`(:154-158)
- `processBlockingThread`(ThreadCommand.java:174-184): 拿到 `BlockingLockInfo` → 无结果(`EMPTY_INFO`,ThreadUtil.java:22)报 "No most blocking thread found!"(:178)→ 有则 ThreadModel 输出
- [Java: `ThreadInfo.getLockInfo()` 返回该线程当前"阻塞在"的锁(monitor 或 synchronizer);`ThreadMXBean.dumpAllThreads(boolean, boolean)` 是唯一能一次性拿到"所有线程的锁状态"的 API——jstack 的原生数据也是它]

- 周边: `ThreadUtil.getThreadNode(loader, thread)`(:422)产出 trace 根节点的线程信息(AR-2 篇 4 的 ThreadNode 数据来源);`getThreadTitle`(:438)是 stack 首行的格式化

关键设计: [模式: 直方图统计(blockCountPerLock)+ 装饰渲染(红字高亮)] **"最热锁"而非"死锁"**: 死锁(cycle)罕见,锁争用(contention)常见——`-b` 的算法就是**统计直方图**: 哪个锁被最多线程等待,哪个就最可能是罪魁。identityHashCode 作为锁的"身份证"(同一把锁对象同一哈希)——不比较 equals,只比较身份。

### 2. "渲染热力" — 红字标注

场景: 输出里持锁线程的栈上,等待锁的那一行被红字标出。

- `getFullStacktrace(BlockingLockInfo)`(ThreadUtil.java:180-266): 普通栈渲染 + 锁标记;对持锁线程,在"被 N 个线程等待的那把锁"对应的帧上输出 `" <---- but blocks N other threads!"`(:238/258)
- `ThreadView.draw`(view/ThreadView.java:23): 四种 model 分发(单线程栈/busy/blocking lock/统计表)
- [终端: Ansi 高亮——`highlighted.a(" <---- but blocks ").a(N).a(" other threads!")`(ThreadUtil.java:238)——红字直接指向问题行]

关键设计: **把"统计"翻译成"人话"**: 原始输出是持锁线程的整段栈,人眼找不到哪行是锁——红字标注把"热力"直接钉在锁所在帧上。这把"数据"变"答案"的最后一公里,是诊断工具体验的关键。

### 3. "两个命令的分工" — -b vs jvm DEADLOCK-COUNT

场景: `thread -b` 找争用,`jvm` 里有 DEADLOCK-COUNT——有什么区别,什么时候用哪个?

- `thread -b`(findMostBlockingLock): **锁争用热力**——任意"一堆人等一个锁"的场景(池子耗尽/慢持锁/误 synchronized)
- `jvm` 的 DEADLOCK-COUNT(JvmCommand.java:193-200): `threads.findDeadlockedThreads()`——**真正的死锁环**(A 等 B、B 等 A,循环等待)
- [Java: `ThreadMXBean.findDeadlockedThreads()` 用锁图找环(JVM 内置算法),返回环内线程 id;`findMonitorDeadlockedThreads` 是旧版(只查 monitor 不查 synchronizer)]
- 使用顺序: 先 `thread -b`(覆盖 99% 的"像死锁"场景)→ 仍有怀疑再 `jvm` 看 DEADLOCK-COUNT(真死锁确诊)

关键设计: **用"争用"覆盖"死锁"**: 真死锁一定是争用(环上每个锁都被至少一个线程等),所以 -b 的统计视角天然包含死锁场景;jvm 的 findDeadlockedThreads 是**确诊工具**(给结论),-b 是**定位工具**(给线索)——两者互补,生产上 -b 用得更多(AR-0 篇 2 §2 的实操顺序在此有源码依据)。

---

跨域桥: 采样差值的 CPU 排序 = 上一篇;深度 getThreadInfo(lockedMonitors/synchronizers) = 上一篇 §3 的数据基础;DEADLOCK-COUNT = AR-4(JvmCommand);线程表格渲染 = AR-3 篇 1 §3;AR-0 篇 2 的 `-b` 红字体验 = 本篇 §2。

---

**OpenJDK 关联**:  [OpenJDK 域 33 JMX — outlines/33-jmx-management/] — dumpAllThreads/findDeadlockedThreads 的 JDK 实现;**另见** [OpenJDK 域 19 Synchronization — outlines/19-synchronization/] — ObjectMonitor 是锁信息的数据来源。

### 核心悬念

**"这套枚举+采样+锁检测,在 dashboard 里怎么被复用?"** — 面板每 5 秒刷新一次,CPU 百分比竟然不用重新"采样两次"——因为 ThreadSampler 的状态跨 tick 保持。数据源与消费方分离,是 arthas 命令架构的隐性规则。

> → [AR-4 篇 2](../ar4-dashboard/02-dashboard-data.md)