# 域 AR-4: Dashboard 面板 — 知识规划

> 源码路径: core/command/monitor200/DashboardCommand.java + DashboardInterruptHandler.java + core/command/monitor200/MemoryCommand.java + core/command/monitor200/JvmCommand.java + core/command/view/DashboardView.java + core/command/model/(DashboardModel/GcInfoVO/TomcatInfoVO) + core/util/NetUtils.java
> 源码量: ~10 文件,核心 ~650 行
> 提取日期: 2026-08-10(v2 深审补充: 机制 9→12)
> 前置域: AR-0 篇 5(dashboard/memory/jvm 使用)/AR-3(ThreadSampler 复用)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| DashboardCommand.java:76-133 process() | **面板生命周期**: `new Timer("Timer-for-arthas-dashboard-" + sessionId, true)`(:79,守护线程)→ `timer.scheduleAtFixedRate(new DashboardTimerTask(process), 0, getInterval())`(:108,默认 5000ms :57)→ suspend/resume 停/启 Timer(:87-102)→ `stop()`(:111)/`restart()`(:119)→ q 退出(QExitHandler :105) | High |
| DashboardCommand.java:218-270 DashboardTimerTask.run() | **每次 tick 组装数据**: 新建 DashboardModel → `ThreadUtil.getThreads()`(:241)→ **复用同一个 threadSampler**(构造时创建 :224)`sample(threads)`(:242,跨 tick 差值)→ `MemoryCommand.memoryInfo()`(:245)→ `addGcInfo`(:148-157,GarbageCollectorMXBean count/time 聚合 GcInfoVO)→ `addRuntimeInfo`(:135-146,os/Java 属性+getSystemLoadAverage+availableProcessors+getUptime)→ `addTomcatInfo`(:159)→ `count >= numOfExecutions` 停表(:230-236) | High |
| DashboardCommand.java:159-216 addTomcatInfo() | **Tomcat 探测(HTTP 桥)**: 先 `NetUtils.request("http://localhost:8006")`(:161,失败则整块不显示)→ 请求 `/connector/threadpool` + `/connector/stats`(:167-168)→ fastjson2 解析 → `SumRateCounter`(:50-53,构造 :181-190)算 QPS/error/收发字节速率 → `RT = processingTime / requestCount`(:187) | High |
| NetUtils.java:33- | **HTTP 请求封装**: `HttpURLConnection` GET,超时 1s/3s——轻量轮询而非 JMX | Medium |
| MemoryCommand.java:42- memoryInfo() | **内存明细**: heap/nonheap 各内存池(MemoryPoolMXBean)+ BufferPool 三组——dashboard 与 memory 命令共享 | High |
| DashboardCommand.java:135-157 addRuntimeInfo/addGcInfo | **运行时与 GC 聚合**: GcInfoVO{name, count, time};runtime 含 os.name/Java 版本/load average/uptime | High |
| DashboardView.java:23- draw() | **面板渲染**: 上半线程表(`ViewRenderUtil.drawThreadInfo`,:66)+ 下半田字格(memory+GC / runtime+tomcat,:67-68)→ `drawGcInfo`(:89-98,输出 `gc.<name>.count`/`gc.<name>.time(ms)`) | Medium |
| DashboardInterruptHandler.java:20-24 | **Ctrl-C 处理**: 先 `timer.cancel()` 再结束进程——防 Timer 泄漏 | Medium |
| JvmCommand.java:24-200(AR-4 补充) | **jvm 命令 9 数据块**: RUNTIME/CLASS-LOADING/COMPILATION/GC/MEMORY-MANAGERS/MEMORY/OS/THREAD/FILE-DESCRIPTOR;`findDeadlockedThreads`→DEADLOCK-COUNT(:193-200);与 dashboard 共享 MXBean 数据源 | High |

| SumRateCounter.java(util/metrics/) | **增量速率计算**: `update(value)` 先算 `value - previous` 增量再入 RateCounter(:28-36,注释示例: 5 秒请求数 267,457,635... 平均速率 282)——QPS/error/字节速率的数学基础 | High |
| DashboardView.java:24-64 | **布局自适应**: `process.width()/height()`(:24-25)按终端尺寸分配——线程表占半屏/三分之一(:29-38),memory+runtime 格自适应(:64);矮终端保底 12 行(:38) | Medium |
| JvmCommand.java:109-191 各块明细 | **9 块明细**: CLASS-LOADING 4 项/COMPILATION 2 项/GC 按名 Map/ **MEMORY-MANAGERS 含 isValid 校验**(:143-147)/MEMORY 的 `getMemoryUsageInfo`(init/used/committed/max :165-173)/OS 5 项/THREAD 5 项(COUNT/DAEMON/PEAK/STARTED/DEADLOCK :184-191) | Medium |

*12 个知识点(v1 9 → 补充 3)*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| 面板数据管线(线程/内存/GC/运行时) | DashboardCommand, ThreadUtil(AR-3), ThreadSampler(AR-3), MemoryCommand, GcInfoVO | dashboard = 已有命令数据源的聚合器——复用而非重造 |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| Timer 生命周期 | DashboardCommand, DashboardInterruptHandler, QExitHandler |
| Tomcat HTTP 桥接 | DashboardCommand.addTomcatInfo, NetUtils, SumRateCounter |
| 面板渲染 | DashboardView(布局自适应), ViewRenderUtil(AR-3) |
| 速率计算 | SumRateCounter, RateCounter, DashboardCommand.addTomcatInfo |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| memoryInfo 内存池 | MemoryCommand |
| jvm 9 数据块 | JvmCommand |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| Timer + 跨 tick 复用 threadSampler | "dashboard 的 %CPU 为什么不用重新采样两次"——状态复用设计;面试"dashboard 原理" |
| Tomcat HTTP 轮询(而非 JMX) | v1 规划的错误点;理解"外部进程信息怎么进面板" |
| 面板数据聚合设计 | dashboard 与 thread/memory 命令的数据共享(一次采样多处用) |
| SumRateCounter 增量速率 | "QPS 怎么算的"——增量/间隔的数学;与 AR-3 的 CPU 差值采样同一思想 |
| 每次 tick 组装数据 | 为什么: 全量快照一屏输出 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| Timer 生命周期(暂停/恢复/取消) | 交互细节,AR-0 已覆盖使用 |
| jvm 9 数据块 | 数据罗列,按需查阅 |
| 面板渲染 | 教学价值低 |
| HTTP 请求封装(NetUtils) | 为什么: 超时 1s/3s 轻量轮询 |
| 内存明细(memoryInfo) | 为什么: 命令与面板共享 |
| jvm 命令 9 数据块 | 为什么: 全量快照 vs 面板 subset |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| SumRateCounter 速率计算 | 简单数学 |
| NetUtils 超时 | 工具细节 |

---

| Ctrl-C 处理 | 为什么: 先 cancel 再结束 |
| 面板渲染(DashboardView) | 为什么: 布局自适应细节 |
| 运行时与 GC 聚合 | 为什么: GcInfoVO 简单聚合 |
| 9 块明细 | 为什么: 逐项罗列 |
| 面板生命周期(suspend/resume/stop/restart) | 为什么: Timer 不可复用的重建策略 |
| 布局自适应(终端宽高分配) | 为什么: 保底 12 行,矮屏可用 |## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 一个"会自己刷新的面板" = 定时器 × 数据源聚合 × 渲染。

### 依赖图

```
01 定时刷新引擎                     ← 无前置
  ├─ 02 面板数据来源                ← 依赖 01 (tick 内组装)
  └─ 03 jvm/memory 命令数据块       ← 依赖 02 (数据源共享,可并行读)
```
### 教学顺序

01 Timer 引擎(怎么刷新)→ 02 面板数据(数据从哪来)→ 03 jvm/memory 命令(全量快照对照)
### 文章拆分 (3 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-dashboard-engine.md | 定时刷新引擎 | Timer/scheduleAtFixedRate/生命周期/Ctrl-C 取消 |
| 2 | 02-dashboard-data.md | 面板数据来源 | ThreadSampler 复用/MemoryCommand/GcInfoVO/runtime/Tomcat HTTP 轮询/**SumRateCounter 速率**/布局自适应 |
| 3 | 03-jvm-memory-commands.md | jvm/memory 命令数据块 | 9 数据块/DEADLOCK-COUNT/MemoryPoolMXBean/与 dashboard 共享 |

### 文章内机制顺序

```
01: new Timer(daemon) → scheduleAtFixedRate → DashboardTimerTask.run → cancel 生命周期
02: 复用 threadSampler 跨 tick → memoryInfo/GcInfoVO/runtime → Tomcat HTTP 轮询 → SumRateCounter
03: jvm 9 块(MXBean 编排) → memory 内存池 → 快照 vs 面板对比
```

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "dashboard 每 5 秒刷新,CPU 是重新采两次样吗?" | 复用跨 tick 状态——不是! |
| "面板里的 QPS/RT 从哪来的?JMX?" | localhost:8006 的 Tomcat HTTP 轮询 |
| "为什么 dashboard 停掉(Ctrl-C)不会泄漏定时器?" | DashboardInterruptHandler 先 cancel |
