# 01. "所有线程"从哪来? — 线程枚举与数据模型

> 🔴 Deep | 19 KP 中的 3 个(枚举/模型/状态统计)
> 读者处境: `thread` 命令要列出**所有**线程——但 JVM 里没有一张全局线程表,Arthas 怎么拿到?

### 1. "没有全局表,就爬线程组" — ThreadUtil.getThreads

场景: ThreadMXBean 只给死锁/信息,但"所有线程的列表"不在任何 API 里。

- `ThreadUtil.getRoot()`(core/util/ThreadUtil.java:29): 从某个线程组沿 `getParent()` 一路爬到**根线程组**(main 线程组的父级,顶级 ThreadGroup)
- `getThreads()`(:41-44): `root.enumerate(threads, true)`——**递归枚举**(true=recursive)全部线程;**容量不足时数组翻倍重试**(enumerate 只填已给的数组,返回值为实际数量,若等于容量可能截断)
- 逐个 `createThreadVO(thread)`(:57): 拷贝 id/name/groupName/priority/state/interrupted/daemon 到 `ThreadVO`(轻量快照模型)

关键设计: 没有 `Thread.getAllThreads()` 这种 API——[Java: JVM 的线程组织是 ThreadGroup 树(main 是根业务组),`enumerate(threads, recursive)` 是唯一枚举手段;返回容量不够时只填满就停,所以必须翻倍重试]——JVM 的线程组织方式是**ThreadGroup 树**(main 的子孙全是业务线程),所以枚举 = 爬树。翻倍重试处理并发创建(枚举期间新线程出现,数组不够,enumerate 只填满就停)。

### 2. "快照而非引用" — ThreadVO 数据模型

场景: 1000 个线程,为什么不直接拿 Thread 对象,要拷贝一份?

- `ThreadVO`(core/command/model/ThreadVO.java:10): id/name/group/priority/state/interrupted/daemon + 采样用 cpu/time/deltaTime 字段
- 采样与渲染**解耦**: ThreadSampler 填 cpu 字段,View 只读 VO——两次采样之间线程可能已结束,VO 是稳定快照

关键设计: [模式: 值对象快照(ThreadVO)+ 聚合统计(stateCountMap)] **快照隔离**: 线程对象是"活引用"(随时可能结束),拷贝成 VO 后,采样/排序/渲染在稳定的副本上进行——1000 线程的 CPU 采样期间结束了几百个也不影响结果一致性。

### 3. "状态统计与过滤" — processAllThreads

场景: `thread --state BLOCKED` 只显示阻塞线程,统计行 "Threads Total" 怎么来的?

- 参数全集(ThreadCommand.java:47-116): `-n` topNBusy/`-b` findMostBlockingThread/`-i` sampleInterval(默认 200ms)/`--state`/`-a` all/`lockedMonitors`+`lockedSynchronizers`(:57-58,控制 getThreadInfo 的锁深度——AR-0 篇 2 的操作在此有落点)

- `ThreadCommand.processAllThreads`(monitor200/ThreadCommand.java:131-144): `LinkedHashMap<State, Integer>` 状态计数(TIMED_WAITING/BLOCKED/RUNNABLE/WAITING...)
- `--state XXX` 过滤(:148-159): 只保留指定状态的 VO
- 表格输出: `ViewRenderUtil.drawThreadInfo`(view/ViewRenderUtil.java:109-126)——10 列: ID/NAME/GROUP/PRIORITY/STATE/%CPU/DELTA_TIME/TIME/INTERRUPTED/DAEMON;STATE 按颜色映射(红=BLOCKED,黄=TIMED_WAITING,:30-38)

关键设计: `LinkedHashMap` 保序计数——输出行与统计一致;STATE 颜色映射是"终端可读性"的设计: 一眼看红黄,问题线程可视化(这在 AR-0 篇 2 的使用体验里是"看到一片红就知道堵了"的来源)。

---

跨域桥: 线程枚举也被 dashboard 复用(AR-4 DashboardTimerTask 每 tick `ThreadUtil.getThreads()`);ThreadVO 的 cpu 字段由下一篇(ThreadSampler)填充;getThreadStackModel(AR-2 篇 4 用)在同一 ThreadUtil 里。

---

**OpenJDK 关联**:  [OpenJDK 域 17 Threads — outlines/17-threads/] — JavaThread 对象模型与 ThreadGroup 树;**另见** [OpenJDK 域 18 Safepoint — outlines/18-safepoint/] — 线程枚举在 safepoint 语义下的视角。

### 核心悬念

**"线程列表有了——但 %CPU 那一列怎么填?"** — 没有 API 能直接回答"这线程占了多少 CPU"。答案是两次读数的差: 采样、等待、再采样。这也是为什么命令要"故意等 1 秒"。

> → [02-cpu-sampling.md](02-cpu-sampling.md)