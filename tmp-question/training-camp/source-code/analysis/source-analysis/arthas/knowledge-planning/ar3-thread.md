# 域 AR-3: Thread 线程诊断 — 知识规划

> 源码路径: core/command/monitor200/ThreadCommand.java + core/util/ThreadUtil.java + core/command/monitor200/ThreadSampler.java + core/command/view/ViewRenderUtil.java + core/command/view/ThreadView.java + core/command/model/(ThreadVO/BusyThreadInfo/BlockingLockInfo)
> 源码量: ~10 文件,核心 ~900 行
> 提取日期: 2026-08-10(v2 深审补充: 机制 13→19)
> 前置域: AR-0 篇 2(thread 命令使用)/AR-2(ThreadUtil.getThreadStackModel 栈帧裁剪)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| ThreadCommand.java:117-131 process() | **命令分派**: 按参数走 4 条路径——`id>0`→processThread(单线程栈)/`topNBusy`→processTopBusyThreads/`findMostBlockingThread`→processBlockingThread/默认→processAllThreads(全量表格) | High |
| ThreadUtil.java:29-56 getRoot/getThreads | **线程枚举**: `root = ThreadGroup.getParent()` 爬到根线程组(:29)→ `root.enumerate(threads, true)` 递归枚举全部线程(:44,容量不足翻倍重试)→ 逐个 `createThreadVO`(:57)拷贝 id/name/group/priority/state/interrupted/daemon | High |
| ThreadSampler.java:34-154 sample() | **CPU 两次采样差值**: 首次采样记基线(`lastSampleTimeNanos` + `getThreadCpuTime` 入 map,setTime(cpu/1_000_000):39-77);第二次算 `delta = time2 - time1`(:107-111,-1 视为不变)→ **CPU% = `rint(delta * 10000 / sampleIntervalNanos) / 100`**(:121)→ 按 delta 降序(:126-139) | High |
| ThreadSampler.java:157-181 getInternalThreadCpuTimes | **内部线程 CPU**: `ManagementFactoryHelper.getHotspotThreadMBean().getInternalThreadCpuTimes()`——GC/JIT 等 Java 内部线程,以 id=-1 的 ThreadVO 参与排序(:172) | High |
| ThreadSampler.java:182-184 pause() | **采样间隔**: `Thread.sleep(sampleInterval)`——调用方 `sample → pause → sample` 两遍(ThreadCommand.java:165-169/186-188) | High |
| ThreadCommand.java:131-173 processAllThreads | **全量表格**: `LinkedHashMap<State,Integer>` 状态统计(:135-144)→ `--state` 过滤(:148-159)→ 表格 | High |
| ThreadCommand.java:184-197 processTopBusyThreads | **Top N**: 两遍采样算 CPU → `-n` 截断 `subList(0, limit)`(:190-197) | High |
| ThreadCommand.java:199-219 processThread | **单线程详情**: `threadMXBean.getThreadInfo(tids, lockedMonitors, lockedSynchronizers)`(:206)——**深度 ThreadInfo**(锁+monitor+synchronizer)→ BusyThreadInfo(:212-219,ThreadVO CPU 数据 + ThreadInfo 锁/栈合并) | High |
| ThreadUtil.java:99-159 findMostBlockingLock | **锁争用热力**: `threadMXBean.dumpAllThreads(objectMonitorUsage, synchronizerUsage)`(:100-101)→ 遍历: 等待锁 `info.getLockInfo()` 按 `identityHashCode` 计数 `blockCountPerLock`(:113-121);持有锁映射 `ownerThreadPerLock`(:123-135)→ 挑"被最多线程等待且有人持有"的锁(:139-147)→ `BlockingLockInfo{threadInfo, lockIdentityHashCode, blockingThreadCount}`(:154-158) | High |
| ThreadUtil.java:180-266 getFullStacktrace | **栈渲染**: 普通栈 + 锁标记;等待锁行红字 `" <---- but blocks N other threads!"`(:238/258) | High |
| ViewRenderUtil.java:109-126 drawThreadInfo | **表格渲染**: 10 列 ID/NAME/GROUP/PRIORITY/STATE/%CPU/DELTA_TIME/TIME/INTERRUPTED/DAEMON(:113-126);STATE 颜色映射(:30-38) | High |
| ThreadView.java:23- | **四种 model 渲染**: 单线程栈/busy 线程/blocking lock/统计表格+状态汇总行("Threads Total: ...") | Medium |
| ThreadVO.java/BusyThreadInfo.java | **数据模型**: ThreadVO(基础拷贝+cpu/time/deltaTime);BusyThreadInfo(合并锁信息) | Medium |

| ThreadCommand.java:47-116 参数集 | **命令参数全集**: id/`-n` topNBusy/`-b` findMostBlockingThread/`-i` sampleInterval(默认200ms)/`--state`/`-a` all/`lockedMonitors`/`lockedSynchronizers`(:57-58)——后两个控制 getThreadInfo 的锁深度 | High |
| ThreadCommand.java:174-184 processBlockingThread | **-b 分派实现**: `findMostBlockingLock()` → 无结果返回 "No most blocking thread found!"(:178)→ ThreadModel(blockingLockInfo) | Medium |
| ThreadCommand.java:218-228 findThreadInfoById | **ID 匹配**: ThreadInfo 数组按 id 逐个匹配(数组可能含 null)——TopN 线程与深度信息的桥 | Medium |
| ThreadSampler.java:25-26,64,128 | **双排序与降级**: 首次采样按累计时间降序(:64),二次按 delta 降序(:128);`hotspotThreadMBeanEnable` 开关(:26)——内部线程采样失败自动降级不崩 | High |
| ThreadUtil.java:22 EMPTY_INFO + :75 getThreadList + :422 getThreadNode + :438 getThreadTitle + :454 getTCCL | **周边工具**: EMPTY_INFO 空锁结果;getThreadList;getThreadNode(AR-2 trace 根节点的线程信息);getThreadTitle(stack 首行 `thread_name=...;TCCL=...`);getTCCL | Medium |
| ThreadUtil.java:268 getFullStacktrace(BusyThreadInfo) | **busy 栈渲染重载**: 合并 CPU 数据与锁信息的栈输出(busy 线程详情用) | Medium |

*19 个知识点(v1 13 → 补充 6)*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| 线程数据获取→采样→渲染管线 | ThreadCommand, ThreadUtil, ThreadSampler, ThreadVO, ViewRenderUtil, ThreadView | thread 命令全链;dashboard(AR-4)复用 ThreadUtil+ThreadSampler |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| 两次采样 CPU% | ThreadSampler, ThreadCommand(两遍调用) |
| 锁争用检测 | ThreadUtil.findMostBlockingLock, BlockingLockInfo |
| 深度 ThreadInfo | ThreadCommand.processThread, BusyThreadInfo |
| 线程枚举 | ThreadUtil.getRoot/getThreads, ThreadVO |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| 内部线程 CPU | ThreadSampler.getInternalThreadCpuTimes, hotspotThreadMBeanEnable 降级 |
| 参数集与分派 | ThreadCommand 参数(lockedMonitors/lockedSynchronizers/-a/-i), processBlockingThread, findThreadInfoById |
| 周边工具 | ThreadUtil(EMPTY_INFO/getThreadList/getThreadNode/getThreadTitle/getTCCL) |
| 状态统计/过滤 | ThreadCommand.processAllThreads |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| 两次采样差值 CPU% | "thread 的 %CPU 怎么算的"必答;差值法 vs 瞬时读数是经典设计 |
| findMostBlockingLock | "thread -b 与 jstack 的区别"必答;生产死锁/锁争用第一工具(AR-0 篇 2 的使用在此落地) |
| 线程枚举(root.enumerate) | 理解"所有线程"从哪来 |
| 深度 ThreadInfo(lockedMonitors/lockedSynchronizers) | ThreadMXBean 能力全景 |
| 命令分派(4 路径) | 为什么: id/-n/-b/全量 的入口分流 |
| Top N 截断 | 为什么: 先采样排序再 subList |
| 单线程详情(深度 getThreadInfo) | 为什么: 锁明细+栈的合并 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| 内部线程 CPU(HotspotThreadMBean) | CPU 高的另一来源,与 AR-0 篇 2 §3 对应;双排序+降级设计值得讲 |
| 命令参数全集(lockedMonitors/-a/-i) | 与 AR-0 篇 2 使用一一对应,源码落点 |
| 状态统计/过滤 | 简单逻辑,AR-0 已覆盖使用 |
| 表格渲染细节 | 教学价值低 |
| 全量表格+状态统计 | 为什么: LinkedHashMap 保序 |
| 数据模型(ThreadVO/BusyThreadInfo) | 为什么: 快照隔离 |
| -b 分派实现 | 为什么: EMPTY_INFO 失败处理 |
| 双排序与降级 | 为什么: 首次累计/二次 delta + 降级不崩 |
| 周边工具(getThreadNode/Title) | 为什么: trace/stack 复用 |
| 采样间隔(pause) | 为什么: 两遍采样间的等待,200ms 经验值 |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| 栈渲染颜色 | 渲染细节 |
| 数据模型字段 | 用时可查 |

| ID 匹配(findThreadInfoById) | 为什么: 工具细节 |
| busy 栈渲染重载 | 为什么: 渲染细节 |

---

## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 数据从哪来(枚举)→ 怎么算出 CPU(采样)→ 怎么找问题(锁争用)。

### 依赖图

```
01 线程枚举与数据模型               ← 无前置
  ├─ 02 两次采样算 CPU              ← 依赖 01 (VO 是采样对象)
  └─ 03 锁争用检测                  ← 依赖 01 (枚举→dumpAllThreads) + 02 (CPU 排序与锁信息合并)
```
### 教学顺序

01 线程枚举(数据从哪来)→ 02 CPU 采样(怎么算)→ 03 锁争用(怎么找问题)
### 文章拆分 (3 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-thread-enumeration.md | 线程枚举与数据模型 | ThreadUtil.getRoot/getThreads/ThreadVO/状态统计/参数集(lockedMonitors/-a/-i) |
| 2 | 02-cpu-sampling.md | 两次采样算 CPU | ThreadSampler(基线/差值/**双排序**/内部线程/**降级**/pause)+ ThreadCommand 两遍调用 + Top N + findThreadInfoById |
| 3 | 03-blocking-deadlock.md | 锁争用与死锁 | findMostBlockingLock/EMPTY_INFO/dumpAllThreads 深度/processBlockingThread 失败处理/getFullStacktrace 高亮/getThreadNode/与 jvm DEADLOCK 对比 |

### 文章内机制顺序

```
01: getRoot 爬组树 → enumerate 翻倍重试 → ThreadVO 快照 → 状态统计
02: 首次采样基线 → pause(200ms) → 二次采样差值 → 双排序 → TopN 截断 → 深度 getThreadInfo
03: dumpAllThreads 深度快照 → 锁计数直方图 → 最热锁 → 红字渲染 → 与 DEADLOCK-COUNT 分工
```

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "%CPU 为什么命令要等 1-2 秒才出结果?" | 两次采样差值,AR-0 篇 2 §1 的现象在此解释 |
| "thread -b 和 jstack 找的死锁有什么区别?" | 锁争用统计 vs 循环等待 |
| "GC 线程在忙,为什么 thread 表里也有?" | HotspotThreadMBean 内部线程采样 |
