# 内存监控与统计接口 — 从 /proc 到 cgroup v2

> Cluster C: 4 KPs | 依赖: 11-overcommit-oom | 读者基线: Overcommit 策略 + OOM Killer 全流程
> 读者处境: 已读 11 篇（OOM 是结果）；本篇回答"结果发生前怎么看——MemAvailable 公式、meminfo 每字段、PSS 精确口径、调优开关、容器隔离"
> 打开新视角: 统计口径会骗人（MemFree≠可用、RSS≠真实占用）、/proc 是内核调试的第一现场、cgroup v2 让"容器内存"成为内核记账单位

---

### 概念依赖链

```
11-overcommit-oom(内存耗尽) → 本篇: 监控与统计(耗尽前怎么看/怎么调)
  ├─ §1 MemAvailable(真正可用量 — 依赖 11 的 OOM 触发语境)
  │    └─ §2 /proc/meminfo(全字段体检 — 依赖 §1 字段出处 + 09 篇 LRU 计数器)
  │         └─ §3 PSS 与 smaps(进程级精确画像 — 依赖 §2 的 RSS/共享概念 + 04 篇 VMA)
  │              └─ §4 vm 参数(运行时调优开关 — 依赖 §2 的水位/脏页/回收字段)
  │                   └─ §5 cgroup v2(容器内存控制 — 依赖 §4 限制理念 + 09 篇 per-cgroup LRU)
先讲: 可用量 → 全字段 → 进程级 → 调优 → 隔离
后续依赖: 13-ptmalloc 与栈(用户态 malloc 怎么拿这些页)
```

### 叙事顺序

1. 问题引入——线上"内存还有 30%"却 OOM？MemFree 显示的是假象（**Aha: 内存"够不够"要看 MemAvailable——可回收与预留的差额，不是 MemFree**）
   - 过渡: 公式里的字段从哪来？——/proc/meminfo
2. MemAvailable 计算——si_mem_available 的保守估计
   - 过渡: 单字段会骗人——全字段怎么读？
3. /proc/meminfo 全字段——六大分组的系统级体检
   - 过渡: 系统级有了——哪个进程吃的？RSS 准吗？
4. PSS 与 smaps——逐 VMA 摊分的精确口径
   - 过渡: 读出来了——怎么调优？
5. procfs vm 参数——水位/脏页/回收/承诺的运行时开关
   - 过渡: 单机调了——容器里怎么隔离控制？
6. cgroup v2——memory.max/high/low/min 四级 + 独立 OOM
   - 过渡: 监控与控制的完整链路已齐——收束
7. 收束——口径、现场、开关、隔离四件事 + Aha Moment

### 1. MemAvailable 计算 — 真正可用的内存

场景提示: 服务器 MemFree 只有 200MB，但系统并不 OOM；另一台 MemFree 3GB 却 OOM——为什么？ [写作时展开]

关键设计: `si_mem_available()`（mm/page_alloc.c）——"安全可用量"的保守估计：

```[pseudocode]
MemAvailable ≈ MemFree
  - totalreserve_pages(各 zone 水位预留: max(WMARK_LOW, WMARK_MIN+不可用) 之和)
  + 可回收部分(打五折):
      pagecache(ACTIVE_FILE + INACTIVE_FILE)      - min(一半, wmark_low)
      slab_reclaimable(NR_SLAB_RECLAIMABLE_B)     - min(一半, wmark_low)
  < 0 → 归 0
```

Why: 为什么是"MemFree + 可回收 - 预留"而非 MemFree？——**MemFree 只算完全空闲页，低估真实可用**：pagecache 和可回收 slab 随时能被回收（清脏/回写后复用），算上才接近"实际能再分配的"。但**可回收部分必须打五折**——内核不敢把 pagecache 全收光（read 还要读文件），预留 `wmark_low` 保底。**MemFree 高但 MemAvailable 低 = 预留吃掉了可回收空间**——这就是"明明有 free 却 OOM"的答案：OOM 判定看的是预留与水位，不是 MemFree（对照 11 篇承诺语境）。 [内核: totalreserve_pages 来自 02 篇 Buddy 水位——si_mem_available 是 02 篇 watermark 机制的用户态可见面]

比喻锚点: MemAvailable=可动用存款——余额（MemFree）是活期现钱，但真正能动用的是"余额 - 信用卡还款预留（水位）+ 可变现资产打五折（pagecache/slab）"——只看余额会高估或低估。 [写作时展开]

### 2. /proc/meminfo 全字段解读 — 系统级体检单

场景提示: `cat /proc/meminfo` 几十行——每行什么含义？排查内存问题先看哪几行？ [写作时展开]

关键设计: `meminfo_proc_show()`（fs/proc/meminfo.c）——按功能分六组读：

```[pseudocode]
核心: MemTotal / MemFree / MemAvailable(§1) / Buffers / Cached / SwapCached / SwapTotal / SwapFree
LRU 状态: Active / Inactive / Active(anon) / Inactive(anon) / Active(file) / Inactive(file)
          / Unevictable / Mlocked — 09 篇 LRU 六链的直接投影
IO 状态: Dirty / Writeback / NFS_Unstable / Bounce / WritebackTmp — 07 篇回写的账本
占用者: AnonPages / Mapped / Shmem / Slab / SReclaimable / SUnreclaim
          / KReclaimable / KernelStack / PageTables — 谁吃了内存
承诺: CommitLimit / Committed_AS / VmallocTotal / VmallocUsed / VmallocChunk — 11 篇承诺账本
大页: AnonHugePages / ShmemHugePages / FileHugePages / HugePages_Total/Free/Rsvd/Surp — 阶段1-02 篇大页
Cgroup: CmaTotal / CmaFree
```

Why: 为什么要按"组"读而不是逐行背？——**meminfo 是内核多个子系统的账本投影**：LRU 行来自 `NR_LRU_*` 计数、Dirty 行来自回写记账、承诺行来自 overcommit 记账——**排查先定位"组"再落"行"**：OOM 看承诺组（Committed_AS vs CommitLimit）、内存泄漏看占用者组（Slab/PageTables 异常）、吞吐下降看 IO 组（Dirty 堆积 = 回写跟不上）。泄漏定位工具链（阶段1-01 篇 kmemleak/slabtop）——Slab 异常 → slabtop 排序找异常 cache → kmemleak 扫描未释放对象。 [内核: meminfo 全部字段出自 09 篇 LRU/06 篇 page-cache 的 NR_* 计数——meminfo 是这些计数的格式化输出]

比喻锚点: meminfo=体检报告单——分科室（组）列指标，正常值范围（默认参数）在每栏备注里，医生（运维）先看异常科室再看具体指标。 [写作时展开]

### 3. PSS 与 /proc/PID/smaps — 精确到字节的进程内存画像

场景提示: 两个进程共享 10MB 动态库——RSS 把同一份库算了两遍，总内存"虚高"——怎么衡量真实占用？ [写作时展开]

关键设计: 三个接口（fs/proc/task_mmu.c）：

```[pseudocode]
/proc/PID/smaps: 每个 VMA 一行 — Rss / Size / Pss / Shared_Clean / Shared_Dirty
                / Private_Clean / Private_Dirty (show_smap)
PSS(Proportional Set Size): PSS = Private + Shared / n_shared
  两进程共享 10MB 库 → 各记 PSS = Private + 5MB
/proc/PID/smaps_rollup(4.14+): 全部 VMA 汇总 — Pss_Anon / Pss_File / Pss_Shmem (show_smaps_rollup)
/proc/PID/status: VmPeak / VmSize / VmRSS / RssAnon / RssFile / RssShmem
                  / VmData / VmStk / VmExe / VmLib / VmPTE / VmSwap (task_mem)
工具: htop 颜色 — 蓝=buffer 绿=cache 黄=swap 红=used
```

Why: 为什么 PSS 比 RSS 精确？——**RSS 把共享页在每个进程里各算一遍**（10MB 库 × 2 进程 = 20MB 虚账）；PSS 按共享者数量**摊分**（各 5MB），**把全系统 RSS 相加 = 真实物理占用**（不重不漏）。**RSS 回答"这个进程账上挂了什么"，PSS 回答"这个进程实际占了多少"**——容量规划、找内存大头、容器超卖核算都该用 PSS；smaps_rollup 免去逐 VMA 相加的成本（4.14+）。**PSS 的局限**：摊分分母（共享者数）随进程生命周期动态变化——同一进程的 PSS 会因别人共享/退出而波动；"独占口径"用 USS（Unique Set Size = Private 部分）——PSS 是"公平账"，USS 是"你的独占地盘"。 [内核: smaps 逐 VMA 遍历依赖 04 篇 mm→vma 红黑树——PSS 摊分是 04 篇 VMA 结构的统计应用]

比喻锚点: PSS=宿舍水电 AA 制——RSS 是"每个人账上都记满额"（共享客厅算每人全价），PSS 是"按人头摊"（共享部分除以住宿人数）——查谁耗电大户用 AA 账才准。 [写作时展开]

### 4. procfs vm 参数全集 — 运行时调优开关

场景提示: 线上抖动/回收频繁/脏页堆积——哪些旋钮能调？默认值是什么？ [写作时展开]

关键设计: 五组旋钮（/proc/sys/vm/）：

```[pseudocode]
水位: min_free_kbytes(默认自适应, 约 4×√(lowmem×16); 大内存建议提高)
      watermark_scale_factor(默认 10=0.1%, 公式 可用页×scale/10000) — 控制 high/emergency 水位间距 (mm/page_alloc.c → min_free_kbytes_sysctl_handler)
脏页: dirty_ratio 20% / dirty_background_ratio 10%
      dirty_expire_centisecs 3000(30s) / dirty_writeback_centisecs 500(5s)
      (mm/page-writeback.c → dirty_ratio_handler)
回收: swappiness 0-200 默认 60(文件页与匿名页回收偏好) / vfs_cache_pressure 100
      (mm/vmscan.c → swappiness_handler)
      观测: vmstat si/so(换入/换出页速率) — 持续高 so = 回收失衡的信号
承诺: overcommit_memory 0/1/2 / overcommit_ratio 50% (mm/util.c → overcommit_memory_handler)
OOM: oom_kill_allocating_task 0/1 / panic_on_oom 0/1/2 (mm/oom_kill.c → oom_kill_allocating_task_handler)
```

Why: 为什么这些参数"能调但不能乱调"？——**每个旋钮背后都是一个触发阈值的数学关系**：min_free_kbytes 抬高 = 提前触发 kswapd 回收（更早、更频繁）；watermark_scale_factor 放大 = 水位间距拉开（分配压力更早传导）；swappiness=60 表示**匿名页与文件页的回收"五五偏好"向文件页倾斜**——调 0 是"除非 swap 不够否则不换匿名页"、调高是"宁可换出匿名页也别清文件缓存"（数据库场景常用）。**参数是"把平衡点搬到哪里"的旋钮，不是越调越好的开关**——改前先看 meminfo/回收日志定位是哪个子系统失衡。 [内核: 水位旋钮直通 02 篇 zone->watermark 数组；swappiness 直通 09 篇 kswapd 扫描比例——调参即改内核回收策略的输入]

比喻锚点: vm 参数=汽车仪表台的旋钮——水位是"油量报警阈值"（抬早报警=更早去加油）、swappiness 是"省电优先还是性能优先"（0=死守电池、200=优先空调）、dirty_ratio 是"水箱满到多少才开泄"——每个旋钮调的是"触发时机"，不是"好坏"。 [写作时展开]

### 5. 内存 cgroup v2 — 容器环境的内存控制

场景提示: Docker `--memory 512m` / K8s `resources.limits.memory`——容器内存怎么被"硬性"限制住的？超了杀谁？ [写作时展开]

关键设计: cgroup v2 内存控制器（mm/memcontrol.c）——每容器独立记账 + 独立回收 + 独立 OOM：

```[pseudocode]
四档限制: memory.max(硬上限, 超则回收→OOM) / memory.high(软上限, 只节流不杀)
          memory.low(尽力保障) / memory.min(硬保障, 不可侵犯) (memory_max_write)
统计: memory.stat(anon/file/swap/pgfault/slab) / memory.events(low/high/max/oom/oom_kill)
      (memory_stat_show)
Swap 接口: memory.swap.max(容器 swap 上限) / memory.swap.current — swap 与 anon 分开记账
独立 OOM: mem_cgroup_oom → mem_cgroup_out_of_memory → oom_kill_process
  — 优先杀本 cgroup 内进程, 不碰别家
实现: mem_cgroup_css_alloc — 每个 cgroup 一套独立 LRU 与水位
```

Why: 为什么容器内存是"真限制"而非"软约束"？——**cgroup v2 让内核对每个容器维护独立的 LRU 与回收水位**（mem_cgroup_css_alloc）：容器 A 吃紧先回收 A 自己的页（per-cgroup kswapd 维度），A 超硬限触发 **cgroup 内 OOM**（先杀 A 里 oom_score 最高的，不惊动全局）——**"隔离记账"让一个容器吃内存不影响邻居**。memory.high 只节流（用户态被 throttle，不杀进程）适合"可慢不可死"；memory.max 是最后硬墙。**v2 vs v1**：v2 统一层级（无内部进程）、swap 并入统一账本、新增 high/min 两档——Docker/K8s 的 limit 正是映射到 memory.max——**容器不是"虚拟机"，是内核为每个 cgroup 独立记账的进程集合**。 [内核: cgroup 独立 LRU 是 09 篇全局 LRU 的 per-cgroup 版本——同机制、按容器维度运行]

比喻锚点: cgroup v2=公司部门预算制——每个部门（容器）独立核算（memory.stat）、超支先内部开源节流（回收本部门）、预算硬顶（memory.max）超了先裁本部门的人（cgroup 内 OOM）——财务（内核）不再"全公司一刀切"看总账。 [写作时展开]

### 6. 收束

回到"内存监控与统计"四件事：
- 口径 = MemAvailable（MemFree + 可回收打五折 - 水位预留）
- 现场 = /proc/meminfo（六组账本投影）与 /proc/PID/smaps（PSS 摊分）
- 开关 = vm 参数（水位/脏页/回收/承诺/OOM 五组旋钮）
- 隔离 = cgroup v2（max/high/low/min + 独立 LRU + 独立 OOM）

**Aha Moment**: "内存统计不是'看剩余'——**MemFree 是假象，MemAvailable 才是可动用**；**RSS 是虚账，PSS 才是真占用**；**容器内存不是'共享主机'，而是内核给每个 cgroup 独立记账**——统计口径决定你会不会误判；监控的意义是把 OOM（11 篇的处决）变成"提前看到"而不是"事后收尸"。"
**回答读者三问**: ①明明有 free 却 OOM=MemAvailable 才是真可用（预留吞掉可回收）；②共享库算重复=RSS 虚高、PSS 摊分才准；③容器内存怎么限=cgroup v2 四档 + 独立 OOM。

---

### 核心悬念

**"内核把物理页管得明明白白——用户态 malloc 到底怎么拿到这些页？ptmalloc 的 fastbins/smallbins/largebins/unsorted bin 和 Buddy 是什么关系？栈为什么自动增长？"**

→ 引出 13-ptmalloc 与栈 — glibc 用户态分配器 + 进程/线程栈——监控看的是"内核账本"，下一篇进"用户态怎么花钱"。