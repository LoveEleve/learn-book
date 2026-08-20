# Overcommit 与 OOM Killer — 内存耗尽的最后防线

> Cluster C: 4 KPs | 依赖: 10-swap | 读者基线: LRU 回收全路径 + Swap 换出换入
> 读者处境: 已读 10 篇（swap 是最后仓库）；本篇回答"仓库也满了怎么办——承诺（overcommit）与处决（OOM Killer）"
> 打开新视角: overcommit 三策略的数学、oom_badness 评分公式、OOM Reaper 的异步收割、oom_score_adj 保护关键进程

---

### 概念依赖链

```
10-swap(最后仓库) → 本篇: Overcommit 与 OOM
  ├─ §1 Overcommit 三策略(承诺管控 — 依赖 10 swap 空间)
  │    └─ §2 OOM Killer 选择(评分杀进程 — 依赖 §1 承诺失守)
  │         ├─ §3 OOM Reaper(异步收割 — 依赖 §2)
  │         ├─ §4 oom_score_adj(保护接口 — 依赖 §2)
  │         └─ §5 OOM 日志解读(诊断 — 依赖 §2/§3)
先讲: 承诺(overcommit) → 处决(选择) → 收割(reaper) → 保护(adj) → 诊断(日志)
后续依赖: 12-内存监控与统计
```

### 叙事顺序

1. 问题引入——程序 malloc 10GB（内存只有 8GB）——系统"答应"还是"拒绝"？（**Aha: overcommit 是'承诺经济'——答应你可以，兑现不了再说（OOM）**）
   - 过渡: 三种承诺策略？
2. Overcommit 三策略——0 启发式/1 总是/2 严格
   - 过渡: 承诺失守——杀谁？
3. OOM Killer——badness 评分公式；跳过规则
   - 过渡: 杀了之后——物理页谁回收？
4. OOM Reaper——异步收割 mmap 式释放
   - 过渡: 关键进程怎么免死？——oom_score_adj
5. oom_score_adj + OOM 日志——保护接口 + 日志解读
   - 过渡: 完整图景已齐——收束
6. 收束——承诺→处决→收割→保护→诊断的完整链条

### 1. Overcommit 三策略 — 系统怎么答应你未必付得起的内存

场景提示: `malloc(10GB)` 在 8GB 机器上——为什么没失败？ [写作时展开]

关键设计: 三种承诺策略（mm/util.c, /proc/sys/vm/overcommit_memory）： [内核: 阶段1-04 篇 OOM 概念→本篇源码——badness 公式与 Reaper 是 04 篇的落地]

```[pseudocode]
0 启发式: __vm_enough_memory → vm_acct_memory(pages)
  allowed = (totalram - hugetlb + swap) * ratio/100 + swap
  committed + pages > allowed → -ENOMEM(明显过分的拒绝)
1 总是(OVERCOMMIT_ALWAYS): 乐观从不拒绝 → 靠 OOM 处理 → 科学计算/批处理
2 严格(OVERCOMMIT_NEVER): allowed = (totalram - hugetlb) * ratio/100 + swap
  所有分配必须在 committed 上限内(强制)
overcommit_ratio 默认 50% → /proc/meminfo CommitLimit/Committed_AS 追踪
```

Why: 为什么"默认启发式"而非"严格"？——**绝大多数 malloc 是过度承诺**（申请大内存实际用一点，阶段1-03 篇延迟分配）：严格模式（2）会让"合理但大"的分配失败（如 Java 堆预留）；启发式（0）只拒绝"明显离谱"（committed 超 allowed）——**在"多数承诺不兑现"的现实下，宽松承诺提升可用性**；代价是承诺失守时靠 OOM 兜底（§2）。

比喻锚点: overcommit=信用卡额度——启发式（0）是"银行估着你工资批额度"（差不多就批）；总是（1）是"无限额随便刷"（出事再说）；严格（2）是"每笔消费都要现钱"（额度死卡）——默认用"估着批"因为大多数人不会刷爆。 [写作时展开]

### 2. OOM Killer 选择流程 — select_bad_process 到发送 SIGKILL

场景提示: 内存真不够了——杀哪个进程？凭什么？ [写作时展开]

关键设计: 评分选择（mm/oom_kill.c）：

```[pseudocode]
触发: Buddy 分配 + 全部回收失败 → __alloc_pages_may_oom → out_of_memory
(1) select_bad_process → for_each_process → oom_evaluate_task
  跳过: is_global_init(PID 1) / same_thread_group(current) / TIF_MEMDIE(已被杀) / oom_unkillable
(2) oom_badness: points = get_mm_rss(mm) + get_mm_counter(mm, MM_SWAPENTS) + mm->nr_ptes/2
  → points = points * 1000 / totalpages(归一化) → += oom_score_adj(手动偏移)
  → 最高分被选中
(3) oom_kill_process → 有子进程不同 mm 则优先杀子 → send_sig(SIGKILL) → TIF_MEMDIE
```

Why: 为什么评分公式是"RSS + swap_ents + ptes/2"？——**杀"占内存最多"的**：RSS（常驻物理页）是直接能释放的；swap_ents（换出页）释放后避免换回；nr_ptes/2（页表开销）是附属成本。**归一化 ×1000/totalpages** 让分数可比（0-1000）；**oom_score_adj 手动偏移**让运维干预（§4 保护关键服务）。杀子进程优先：**父进程可能还有清理逻辑**（子进程通常是"干活的那部分"）。

比喻锚点: OOM 评分=裁员名单评分——优先裁"占工位最多"（RSS 大）的、签了合同没来上班（swap 页）的、带家属占宿舍（ptes）的；领导（运维）可以手动标记"骨干不能裁"（oom_score_adj -1000）。 [写作时展开]

### 3. OOM Reaper 收割 — 被杀进程物理页的异步回收

场景提示: SIGKILL 发了——进程的物理页什么时候释放？等它自己退？ [写作时展开]

关键设计: 异步收割（mm/oom_kill.c）：

```[pseudocode]
wake_oom_reaper(victim) → queue_delayed_work(system_wq, &oom_reaper_wait)
oom_reaper(victim) → mmap_read_lock(mm) → __oom_reap_task_mm
  → for_each_vma → MADV_DONTNEED 式 unmap → unmap_page_range(释放所有物理页) → tlb_finish_mmu
阻塞进程(D 状态): 无法获取 mmap_lock → 等 MAX_OOM_REAP_RETRIES=10 次(1s)
  → 失败 → force_sig(SIGKILL) 不等 reaper
TIF_MEMDIE 已设置 → alloc 路径快速返回失败 → 不重试
```

Why: 为什么需要"Reaper"而非"等进程自己退"？——**被杀进程可能无法退出**（D 状态卡在不可打断 IO、死锁）——等它退 = 内存永远不释放 = OOM 持续。**Reaper 独立线程直接用 mmap 式 unmap 释放物理页**（不等进程自己清理）——**"处决"与"收尸"分离**：SIGKILL 负责停进程，Reaper 负责回收内存（异步、不阻塞）。D 态进程拿不到 mmap_lock 时，10 次重试后 force_sig 强制处理。

比喻锚点: OOM Reaper=法院执行庭——法院判了（SIGKILL 处决）但"老赖"不搬（D 态不退）——执行庭（Reaper）直接上门清房（unmap 释放物理页），不用等"老赖"自己搬；实在撬不开锁（拿不到 mmap_lock）就申请强制（force_sig）。 [写作时展开]

### 4. OOM Score 调整接口 — 保护关键进程

场景提示: 数据库/监控被杀过？怎么让 OOM 永不选它？ [写作时展开]

关键设计: 两个评分文件（fs/proc/base.c）：

```[pseudocode]
/proc/PID/oom_score: 动态评分(0-1000) — 内核算的, 只读
/proc/PID/oom_score_adj: 手动偏移(-1000~1000) — 运维写的
-1000: 永不杀 / 1000: 必杀
工具: choom -n <score_adj> -p <PID>
保护: echo -1000 > /proc/$(pidof sshd)/oom_score_adj
诊断: /proc/vmstat oom_kill(历史次数) / dmesg: "Out of memory: Killed process %d (%s)"
```

Why: 为什么需要"手动偏移"？——**评分公式只看内存占用**（§2），但运维知道"谁是关键"（sshd/mysqld/prometheus 不能死）——`oom_score_adj=-1000` 让该进程**从候选列表彻底移除**（永不杀）；正值则"牺牲优先"（实验/可丢弃任务）。**公式给基准，人给偏好**——自动评分 + 人工调整的结合。

比喻锚点: oom_score_adj=紧急疏散名单——系统按"谁占空间大"（评分）排序疏散；但消防员（运维）手里有"重点保护名单"（adj -1000：数据库/监控必须保住）——名单上的人优先保护，不在名单的按占位大小牺牲。 [写作时展开]

### 5. OOM Report 解读 — dmesg OOM 日志逐行分析

场景提示: dmesg 出现 OOM 日志——每行数字什么意思？怎么定位"谁吃光了内存"？ [写作时展开]

关键设计: 日志字段（mm/oom_kill.c）：

```[pseudocode]
"Out of memory: Killed process 12345 (java)"
total-vm:52345678kB — 虚拟地址空间总量
anon-rss:8234567kB — 匿名常驻(堆/栈) — OOM 主嫌疑
file-rss:123456kB — 文件映射常驻
shmem-rss:45678kB — 共享内存常驻
oom_score_adj:0 — 手动偏移 / UID:1000 / pgtables:12345kB(页表占用)
```

Why: 为什么"anon-rss"是 OOM 主嫌疑？——**匿名页无法直接丢弃**（无后备，要 swap 或杀进程释放）；file-rss 可回收（重读）、shmem-rss 可释放——**anon-rss 大 = 进程'独占不可再生'内存多 = 最值得杀**。看日志先找 anon-rss 最大的进程 + total-vm 异常的（如 Java 堆配置过大）——**OOM 日志是'谁吃了内存'的第一现场**。

比喻锚点: OOM 日志=事故现场勘验单——"哪个工人（进程）占了多少'私有材料'（anon-rss）"——占私有材料多的（不可回收）是主要嫌疑；占"公家材料"（file-rss 可重读）的嫌疑小。 [写作时展开]

### 6. 收束

回到"内存耗尽的最后防线"：
- 承诺 = overcommit 三策略（0 启发/1 总是/2 严格）
- 处决 = oom_badness 评分（RSS+swap+ptes）
- 收割 = OOM Reaper（异步 unmap）
- 保护 = oom_score_adj（-1000 永不杀）
- 诊断 = OOM 日志（anon-rss 主嫌疑）

**Aha Moment**: "overcommit 是'承诺经济'——系统大方承诺（启发式），失守时用'评分处决'（OOM 杀最占内存的）兜底，再用'Reaper 收尸'（异步释放）和'adj 保护'（关键服务豁免）完善流程。**整个设计承认一个现实：内存管理不能只有'拒绝'，还要有'止损'**——杀一个进程释放内存，比系统崩溃好。"
**回答读者三问**: ①malloc 10GB 为何不失败=overcommit 承诺；②OOM 杀谁=RSS+swap+ptes 评分；③关键服务怎么保护=oom_score_adj -1000。

---

### 核心悬念

**"线上出问题第一眼看什么？MemAvailable 的计算公式是什么？PSS 为什么比 RSS 精确？/proc/meminfo 和 /proc/PID/smaps 的每列怎么解读？"**

→ 引出 12 内存监控与统计接口 — MemAvailable/PSS/procfs/cgroup v2——OOM 是结果，监控是预防；下一篇讲"怎么看内存"。
