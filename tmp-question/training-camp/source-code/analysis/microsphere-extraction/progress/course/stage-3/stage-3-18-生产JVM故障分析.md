# stage-3 · 第 18 节：[公开课] 生产环境 JVM 故障分析 — 知识点提取

> 课程：stage-3 三高架构 第 18 节（JVM 组）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/18.[公开课] 生产环境JVM故障分析.md`
> 提取时间：2026-08-12 | 权重：核心（生产排障实战——3 个真实案例：WAITING 线程/线程失控 Crash/拒绝策略）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**本篇 my-xhs 的 AsyncConfig 是案例二修改建议的完整正确实现**

---

## 一、本节概览

- **技术域**：线程池生命周期陷阱、线程 dump 分析、非池化线程池风险、线程失控与 OOM Kill、JVM 内存全景（NMT）、内存占用分析、拒绝策略
- **维度**：`[工程问题]`（线程池/排障）+ `[性能优化]`（JVM 内存全景）
- **核心命题**：**生产 JVM 故障的三种典型形态**——docs 三个真实案例：①WAITING 线程堆积（线程池未关闭）②线程失控 Crash（非池化 @Async + 无熔断）③线程池拒绝（空节）；docs 是真实事故记录（数字精确），知识本体 = 事故模式 + 排障方法
- **知识点数**：7 个
- **前置**：05 篇（JVM 参数/GC）、04 篇（JFR）、09 篇（异步配套）

## 前置条件清单
读者需先掌握：
1. **JVM 内存结构**（05 篇：堆/metaspace/GC）
2. **J.U.C 线程池**（ThreadPoolExecutor 参数/Worker 结构）
3. **异步与 trace 配套**（09 篇：MdcAwareExecutorService）
未达前置者，先补：05 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **案例对照**：docs 三个真实案例 ↔ my-xhs 现状（AsyncConfig 正确实现对照）
- **数字精确**：docs 事故数字（15 万线程号/600M vs 319M）照录
- **诚实标注**：案例三（拒绝策略）为空节；jstack 附件为外部链接（无本地文件）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 线程池生命周期陷阱（方法内创建未 shutdown → core 线程永久 WAITING）【docs 案例一】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：ThreadPoolExecutor
- **来源**：docs 案例（一）（初步诊断 + 示例代码）
- **需求**：掌握**线程池泄漏模式**——docs 案例一：大量 ThreadPoolExecutor 对象 → 大量 WAITING core 线程
- **自主实现**：若我设计——线程池**单例共享 + 生命周期管理**（Bean 管理启停），杜绝方法内创建
- **参考实现**（docs 案例一 + 发散）：**事故现象（docs）**——应用存在大量 ThreadPoolExecutor 及派生类对象（jstack 见大量 WAITING 线程 + 未命名线程）；**两种成因（docs 明确）**——①预初始化 core 线程（数量 1）无任务执行 ②执行过一次任务再无新任务（**computeIfAbsent 缓存机制失效** / **方法内部创建 ThreadPoolExecutor 未合理 shutdown**——docs 示例代码：`Executors.newFixedThreadPool(1)` 在方法内创建、不 shutdown → **core 线程 Keep-Alive=0 永久等待 → Thread.State.WAITING**）；**线程结构链（docs 明确）**——`ThreadPoolExecutor → workers(HashSet<Worker>) → Worker → Thread`；**修改建议（docs）**——①排查并及时关闭 ThreadPoolExecutor ②**单例共享**（docs 划掉了"设置 Keep-Alive=60s"——**共享优于调参**）
- **对比取舍**：**单例共享 vs 每处新建**——生命周期可控 vs 泄漏（docs 建议①的明确答案）；**Keep-Alive 调参治标**（docs 划掉=否决）
- **测试佐证**：docs 案例一（诊断 2 条 + 示例代码 + 修改建议原文）

### KP-02 线程 dump 分析流程（jstack → fastthread.io → 线程命名规范）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：无
- **来源**：docs 案例一（jstack 分析/fastthread.io）
- **需求**：掌握**线程 dump 的分析工具链**——jstack 采集 → fastthread.io 在线分析
- **自主实现**：若我设计——jstack 抓 dump（多份时间点对比）→ fastthread.io/Arthas 分析线程状态分布 → 定位可疑线程
- **参考实现**（docs + 发散）：**工具链（docs）**——`jstack` 抓取（docs 附件 jstack0727.txt）+ **fastthread.io**（在线线程分析报告——线程状态分类）；**命名规范（docs 观察）**——"项目里搜了使用线程的地方都加了命名"（未命名线程=排查障碍——**线程名是排障第一线索**）；**分析要点（发散）**——WAITING/BLOCKED/RUNNABLE 分布 + 同名线程计数（docs 案例二 SimpleAsyncTaskExecutor-149252 即线程名前缀 + 序号）
- **对比取舍**：**jstack 快照 vs Arthas 在线**——离线分析 vs 实时诊断（04 篇 JFR 衔接）——快照看状态分布，在线看栈详情
- **测试佐证**：docs 案例一（jstack 附件链接 + fastthread 报告链接 + 命名观察）

### KP-03 非池化线程池风险（SimpleAsyncTaskExecutor 无界线程 + @Async 误用）【docs 案例二 + my-xhs 正确对照】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：@Async（09 篇）
- **来源**：docs 案例（二）（线程 Dump/初步诊断/修改建议）+ my-xhs AsyncConfig 实证
- **需求**：掌握**非池化线程池的致命风险**——docs 案例二：SimpleAsyncTaskExecutor（**每任务新建线程，无界**）→ 15 万+ 线程
- **自主实现**：若我设计——@Async 必须配**池化 TaskExecutor**（ThreadPoolTaskExecutor）+ 命名 + 优雅停机
- **参考实现**（docs 案例二 + my-xhs 实证）：**事故（docs）**——线程数超高（**线程号 #157740——15 万+线程**）→ 服务极慢 → **无熔断降级、流量持续进** → node 主机 kill 进程；**线程 Dump 分析（docs）**——`SimpleAsyncTaskExecutor-149252` WAITING (parking) → **logback `OutputStreamAppender.writeBytes` 锁竞争**（线程都挤在日志写入锁）；**诊断（docs）**——Spring **非池化 TaskExecutor（SimpleAsyncTaskExecutor）**——"可能是不正确使用 @Async"（**Sleuth + Skywalking + @Async 组合**）；**修改建议（docs）**——增加**池化 Spring TaskExecutor Bean（ThreadPoolTaskExecutor/ThreadPoolTaskScheduler）**+ **Spring Bean 生命周期管理 J.U.C 线程池启停**；**my-xhs 正确实现（重大对照）**——`common/config/AsyncConfig.java`（注释实证：**"自定义异步线程池（替代默认的 SimpleAsyncTaskExecutor）"**——正是 docs 建议的落地）：`ThreadPoolTaskExecutor`（**core 10/max 20/queue 200/keepAlive 60s/线程名前缀 "async-"**）+ **TaskDecorator 透传 MDC + TraceContext**（docs 的 Sleuth+Skywalking+@Async 组合问题——my-xhs 用 TaskDecorator 解决透传）+ **优雅停机（waitForTasksToCompleteOnShutdown + awaitTerminationSeconds 30）**（docs 案例一"未合理 shutdown"的反面）
- **对比取舍**：**池化（ThreadPoolTaskExecutor）vs 非池化（SimpleAsyncTaskExecutor）**——有界可控 vs 无界爆炸（docs 案例二核心教训）——**@Async 默认 SimpleAsyncTaskExecutor 是生产陷阱**
- **测试佐证**：docs 案例二（dump 原文 + 诊断 + 建议）+ my-xhs `AsyncConfig.java`（替代 SimpleAsyncTaskExecutor/池参数/TaskDecorator/优雅停机注释实证）

### KP-04 线程失控与 OOM Kill 死亡螺旋（无熔断 + 无界线程）【docs 案例二深化】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：03 篇（Sentinel 熔断）
- **来源**：docs 案例二（问题描述）+ 架构师发散
- **需求**：理解**故障放大链**——线程失控（无界）+ 无熔断（流量持续进）= 宿主 kill 进程（docs 案例二因果链）
- **自主实现**：若我设计——三道防线：①有界线程池（防无界）②熔断降级（防流量持续进）③线程数监控告警
- **参考实现**（docs 因果链 + 发散 + my-xhs 对照）：**docs 因果链（明确）**——"线程数量占用超级高 → 服务请求超级慢 → **没有加熔断降级，流量还一直进** → **node 主机直接把 java 进程 kill 掉**"——**慢 → 无保护 → 更慢 → 进程被杀（OOM Kill/宿主裁决）**；**OOM 排查步骤（docs §OOM KILLER 日志）**——`grep "Out of memory" /var/log/messages`（宿主 OOM Killer 裁决证据）+ `ps -A -ostat,ppid,pid,cmd | grep -e '^[Zz]'`（僵尸进程检查——docs 案例结果为无）；**死亡螺旋机制（发散）**——每请求建线程（SimpleAsyncTaskExecutor）→ 线程数 ↑ → 内存/上下文切换 ↑ → 更慢 → 更多请求滞留 → 线程更多……直到宿主 OOM Kill；**防护（发散 + my-xhs）**——①**有界线程池**（my-xhs AsyncConfig 实证）②**熔断降级**（my-xhs Sentinel Bulkhead/网关流控——03 篇）③**线程数监控**（03 篇 Prometheus——jvm_threads 指标）
- **对比取舍**：**熔断（流量闸）vs 有界池（资源闸）**——双闸缺一不可（docs 案例二两者皆缺才 Crash）
- **测试佐证**：docs 案例二（因果链原文）+ my-xhs（AsyncConfig + Sentinel 03 篇）

### KP-05 JVM 内存全景（10 项分布 + NativeMemoryTracking）【docs 案例一核心】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：JVM 内存结构（05 篇）
- **来源**：docs 案例一（jcmd VM.native_memory summary + 10 项分布）
- **需求**：掌握 **JVM 进程内存的完整构成**——堆外被低估的部分（线程栈/GC/代码缓存）——docs 10 项清单
- **自主实现**：若我设计——排障时用 `-XX:NativeMemoryTracking=summary` + `jcmd pid VM.native_memory summary` 看 committed 分布
- **参考实现**（docs 10 项 + 发散）：**JVM 进程内存 10 项（docs 明确）**——①**heap**（-Xmx 限制）②**class**（metaSpace：metadata 受 MaxMetaspaceSize + classSpace 受 CompressedClassSpaceSize）③**thread**（线程栈受 -Xss，**总大小无限制**——线程失控的内存面）④**code**（JIT 编译代码，受 ReservedCodeCacheSize）⑤**gc**（GC 结构内存：CardTable/标记数/区域记录——**G1 最多堆 10% 额外、ZGC 15-20%**，不受限）⑥compiler ⑦internal ⑧symbol（常量池，StringTableSize 限制个数）⑨**Native Memory Tracking**（采集本身开销）⑩Arena Chunk；**工具（docs）**——`-XX:NativeMemoryTracking=summary` 启动参数 + `jcmd pid VM.native_memory summary`（**committed = 实际使用**）
- **对比取舍**：**NMT（精确分布）vs 粗略估算（堆+堆外）**——排障必需精确（docs 600M 之谜靠 NMT 解）
- **测试佐证**：docs 案例一（NMT 命令 + 10 项清单原文）

### KP-06 内存占用分析（top RSS vs JVM 内存/保留-提交模型）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-05
- **来源**：docs 案例一（top 与 jvm 实际占用/保留提交）
- **需求**：理解 **RSS（top）与 JVM 内部内存的差异**——docs 案例：实际 600MB >（堆内+堆外）319MB
- **自主实现**：若我设计——top 看到 RSS（进程视角）vs jcmd NMT（JVM 视角）——差异排查（线程栈/GC 结构/元空间/编译代码）
- **参考实现**（docs + 发散）：**docs 数据**——top 600MB vs（堆内+堆外）319MB——"600M 内存的确被 jvm 所属进程使用了"；**堆外线索（docs §OOM/§其他）**——`arthas vmtool --action forceGc` 强制 Full GC **不能减少进程内存占用**（内存不在堆内——堆外/线程栈/GC 结构嫌疑）+ OS 内存**保留（reserve）/提交（commit）**两阶段；**NMT 解开**——committed 分布（thread/code/gc/class 等堆外项解释差值）；**保留/提交（docs 明确）**——OS 内存两阶段：**保留（reserve：连续虚拟内存预留）→ 提交（commit：映射物理内存）**——**JVM 的 -Xmx 是保留上限，实际占用看 committed**；**排查面（发散）**——RSS 高排查：线程数（-Xss×线程数）/元空间/GC 结构（G1 堆 10%）/DirectBuffer
- **对比取舍**：**reserve vs commit**——预留（虚拟）vs 实际（物理）——**看内存占用看 committed 不是 -Xmx**
- **测试佐证**：docs 案例一（600M vs 319M 数据 + 保留提交原文）

### KP-07 线程池拒绝策略（docs 案例三空节发散）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01/03
- **来源**：docs 案例（三）（**空节：仅问题描述图片，无正文**）+ 架构师发散
- **需求**：掌握**线程池满时的拒绝策略**——docs 案例三标题（空节，发散补全）
- **自主实现**：若我设计——拒绝策略按场景选：快速失败（AbortPolicy + 告警）/调用者执行（CallerRunsPolicy 自然限速）/丢弃（慎用）
- **参考实现**（docs 标题 + 发散 + my-xhs 对照）：**J.U.C 四策略（发散）**——`AbortPolicy`（默认：抛 RejectedExecutionException——需监控告警）/`CallerRunsPolicy`（调用线程执行——**自然背压**，17 篇背压思想同源）/`DiscardPolicy`（静默丢弃——慎用）/`DiscardOldestPolicy`（丢最旧）；**my-xhs 对照**——`AsyncConfig` 未显式设拒绝策略（默认 AbortPolicy `[待验证]`）+ **Sentinel 线程池隔离**（03 篇 Bulkhead：maxQueueSize 超限快速失败——**拒绝策略的服务级版本**）；**生产纪律（发散）**——拒绝必须**可观测**（指标/日志/告警——03 篇 Prometheus）
- **对比取舍**：**Abort（快速失败+告警）vs CallerRuns（限速）**——可观测失败 vs 隐式限流——按业务容忍度选
- **测试佐证**：docs 案例三（标题）+ my-xhs（AsyncConfig + Sentinel Bulkhead 03 篇）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 线程池生命周期陷阱（WAITING 泄漏） | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 线程 dump 分析流程 | 性能优化 | 支撑 | P2 | 🟡 | 有效 | High |
| 非池化线程池风险（@Async 误用） | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| 线程失控与 OOM Kill 死亡螺旋 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| JVM 内存全景（10 项 + NMT） | 性能优化 | 核心 | P1 | 🔴 | 有效 | High |
| 内存占用分析（RSS vs committed） | 性能优化 | 支撑 | P2 | 🟡 | 有效 | High |
| 线程池拒绝策略 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（AsyncConfig 正确实现对照）+ 03/09 篇实证
- **关键源码**（本次实证）：
  - `common/config/AsyncConfig.java`——注释实证：**"自定义异步线程池（替代默认的 SimpleAsyncTaskExecutor）"**（docs 案例二建议的落地）+ `ThreadPoolTaskExecutor`（core 10/max 20/queue 200/keepAlive 60s/前缀 "async-"）+ `TaskDecorator`（**MDC + TraceContext 透传**——docs 的 Sleuth+Skywalking+@Async 组合问题解）+ 优雅停机（waitForTasksToCompleteOnShutdown/awaitTerminationSeconds 30）
  - `start-all.sh` 无 NativeMemoryTracking（grep 实证——与 docs 案例一同状况 `[差距]`）
- **诚实标注**：docs 案例三（拒绝策略）为**空节**（仅问题描述图）→ KP-07 发散补全；jstack 附件/fastthread 报告为**外部链接**（无本地文件 `[无本地文件]`）；docs 案例数字（#157740 线程号/600M vs 319M/-Xms4700m）照录；docs 案例二为 2023 事故（Sleuth 时代——现代 Micrometer Tracing，迁移≠机制）
- **关联标注**：05 篇（JVM 参数/GC——10 项内存中的 gc 项衔接）；04 篇（JFR——排障工具链）；09 篇（异步配套——AsyncConfig/MdcAwareExecutorService）；03 篇（Sentinel 熔断/Prometheus——死亡螺旋防护）；02 篇（性能方法论）

---

## 五、本节小结（三层次视角）

**需求**：生产 JVM 排障三种典型形态——WAITING 线程泄漏、线程失控 Crash、拒绝策略（docs 三个真实案例）。

**自主实现核心**：若我设计——①线程池单例共享 + Bean 生命周期管理（杜绝方法内创建）②@Async 必配池化 TaskExecutor（有界 + 命名 + 优雅停机 + MDC 透传）③三道防线（有界池/熔断/线程监控）④NMT 精确看 committed。

**参考实现**：docs（三个真实案例 + 数字精确）+ **my-xhs AsyncConfig 正确实现对照**（"替代 SimpleAsyncTaskExecutor"注释 + 池参数 + TaskDecorator + 优雅停机——**docs 案例二修改建议的完整落地**）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**生产 JVM 故障模式与防线**"——线程池泄漏（生命周期）、非池化爆炸（有界化）、死亡螺旋（熔断+资源双闸）、内存真相（committed/NMT）；my-xhs 在线程池侧已正确（AsyncConfig），NMT 未开启（与 docs 案例一同状况）。

**待验证汇总**：
- AsyncConfig 拒绝策略（默认 AbortPolicy 未显式设）
- my-xhs 线程数监控指标（Prometheus jvm_threads——03 篇核对）
- NMT 开启评估（docs 案例教训：未开 NMT 排障受阻）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 案例（故障模式） | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 案例一：方法内线程池泄漏 | ✅ 已规避：AsyncConfig 单例 Bean + 优雅停机（awaitTerminationSeconds 30）——docs 建议①②落地 | 无 |
| 案例二：SimpleAsyncTaskExecutor 无界爆炸 | ✅ 已规避：AsyncConfig 注释实证"**替代默认的 SimpleAsyncTaskExecutor**"（ThreadPoolTaskExecutor 有界 + TaskDecorator 透传 + 优雅停机）——**docs 修改建议的完整落地** | 无 |
| 案例二：无熔断流量持续进 | ✅ 已防护：Sentinel Bulkhead/网关流控（03 篇）+ 网关限流过滤器（05 篇） | 无 |
| 案例一：NMT 未开启排障受阻 | ⚠️ start-all.sh 无 NativeMemoryTracking（grep 实证——同 docs 案例一状况） | **差距 P2**：开启 `-XX:NativeMemoryTracking=summary`（有 ~1% 开销）+ 排障时 jcmd 查询 |
| 线程 dump 工具链 | ⚠️ jstack/Arthas 可用（工具在环境）；无系统化 dump 流程 | `[待验证]`：排障 SOP 未建 |
| 拒绝策略 | ⚠️ AsyncConfig 未显式设（默认 AbortPolicy）+ Sentinel Bulkhead 服务级兜底 | `[待验证]`：显式化 + 拒绝告警 |

### 差距清单（JVM 排障层）

1. **P2**：开启 NativeMemoryTracking（docs 案例一直接教训——未开 NMT 的 600M 之谜只能靠猜）
2. **P2**：线程/GC 监控指标核对（Prometheus jvm_threads/jvm_gc——03 篇面板已有 jvm-monitor.json）
3. **P3**：拒绝策略显式化 + 告警联动

**结论**：18 篇——my-xhs 的**线程池侧已是 docs 两个案例修改建议的正确实现**（AsyncConfig 完整落地）；最大差距 = **NMT 未开启**（docs 案例一现场同款问题）——建议直接补上。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为真实生产事故记录（数字精确，照录）；案例三空节；my-xhs 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：生产 JVM 排障的完整认知该讲什么

docs 覆盖三个案例。完整还该包含：

1. **"线程是 JVM 最贵的资源之一"**（docs 两案例共通 + 发散）：线程栈（-Xss）× 线程数 = 无上限内存面（docs 10 项中 thread"总大小无限制"）；**每线程 1MB 栈（默认）→ 15 万线程 ≈ 150GB 虚拟/上下文切换爆炸**——案例二 Crash 的内存机制
2. **排障的"证据链"**（docs 案例结构 + 发散）：监控图（先看症状）→ jstack（抓现场）→ fastthread（分类）→ 代码排查（找成因）→ 修改建议（修复）——**docs 每案例都按此结构**；**多份 jstack 时间点对比**是定位"泄漏还是波动"的关键（发散）
3. **死亡螺旋的三道防线**（docs 案例二 + 发散）：**有界池（资源闸）+ 熔断（流量闸）+ 监控（发现闸）**——docs 案例二三者皆缺（SimpleAsyncTaskExecutor + 无熔断）；my-xhs 前两道已落地（AsyncConfig + Sentinel），第三道（线程数告警）待核对
4. **内存真相：committed 而非 -Xmx**（docs 案例一 + 发散）：-Xmx 是保留上限；**实际占用 = committed（NMT 视角）**；RSS（top）与 JVM 内部视角的差值来自堆外（线程栈/GC 结构/元空间/代码缓存）——**排障先开 NMT**（docs 教训：未开只能猜）
5. **异步配套是排障的前置条件**（docs 案例二 + 发散）：Sleuth+Skywalking+@Async 组合暴露的是 **MDC/上下文透传缺失**——my-xhs TaskDecorator（实证）是标准解；**没有 trace 的异步 = 排障盲区**
6. **日志写入锁是高频聚集点**（docs 案例二 dump + 发散）：线程全挤在 logback writeBytes 锁（dump 实证）——**日志量失控会放大线程竞争**（29 节日志平台衔接）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 单例共享 vs 每处新建线程池 | 生命周期可控 vs 泄漏（docs 案例一） |
| 池化 vs 非池化（@Async） | 有界可控 vs 无界爆炸（docs 案例二） |
| 快速失败（Abort）vs 调用者执行（CallerRuns） | 可观测失败 vs 隐式限速 |
| NMT 开启 vs 不开 | 精确排障 vs ~1% 开销 |
| 熔断 vs 有界池 | 流量闸 vs 资源闸（双闸缺一不可） |
| reserve vs commit 视角 | 预留上限 vs 实际占用（看 committed） |

### 常见坑/反模式

1. **方法内创建线程池不 shutdown**（docs 案例一）：core 线程永久 WAITING——单例 + Bean 生命周期
2. **@Async 用默认 SimpleAsyncTaskExecutor**（docs 案例二）：无界线程爆炸——必配池化
3. **无熔断的慢服务**（docs 案例二）：流量持续进 = 死亡螺旋——Sentinel 熔断（03 篇）
4. **看 -Xmx 判内存占用**：实际看 committed（docs 案例一 600M 之谜）
5. **不开 NMT**：堆外谜团只能猜（docs 教训）
6. **异步无 MDC 透传**：链路断裂排障盲区（my-xhs TaskDecorator 示范）
7. **拒绝策略不可观测**：静默丢弃/异常无告警——拒绝必须进指标

### 生态位置

- **stage-3 教学主线**：JVM 组——05 容器调优（JVM 参数/GC）→ 04 JFR（诊断工具）→ **18 生产故障分析（本篇）**——JVM 认知闭环；19 起转网关组
- **前后篇衔接**：05 篇（10 项内存中的 gc/thread 项衔接）→ 本篇（排障实战）；04 篇（JFR 工具链）→ 本篇（jstack/fastthread/NMT 工具链）；09 篇（异步配套——AsyncConfig 实证）；03 篇（Sentinel/Prometheus——死亡螺旋防护）
- **与源码提取的关系**：my-xhs AsyncConfig 为核心对照；JDK 线程池机制（openjdk11u——ThreadPoolExecutor 源码可查 `[待验证：Worker 结构源码引用]`）

**架构师视角结论**：本篇以 **docs 三个真实事故**讲生产 JVM 故障模式（WAITING 泄漏/无界爆炸/死亡螺旋）、**my-xhs AsyncConfig 实证**展示正确姿势（"替代 SimpleAsyncTaskExecutor"注释 + 有界池 + TaskDecorator + 优雅停机——docs 案例二建议的完整落地）——知识本体是"**生产 JVM 排障的证据链与防线**"：线程池生命周期、三道防线（资源/流量/发现）、内存真相（committed/NMT）、异步配套（MDC 透传）；my-xhs 线程池侧已正确，**最大差距 = NMT 未开启**（docs 案例一直接教训）。
