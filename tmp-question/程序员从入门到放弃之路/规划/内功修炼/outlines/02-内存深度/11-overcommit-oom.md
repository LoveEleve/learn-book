# Overcommit 与 OOM Killer — 内存耗尽的最后防线

> Cluster C: 4 KPs | 依赖: 10-swap | 读者基线: LRU 回收全路径 + Swap 换出换入

---

### 1. Overcommit 三策略 — 系统怎么答应你未必付得起的内存
  - `/proc/sys/vm/overcommit_memory`: **0(启发式)**: `__vm_enough_memory → vm_acct_memory(pages)` (mm/util.c → __vm_enough_memory)
  - 检查公式: `allowed = (totalram - hugetlb + swap) * overcommit_ratio/100 + swap` → `committed + pages > allowed` → -ENOMEM
  - **1(总是)**: 乐观从不拒绝 → 靠 OOM 处理 → 适合科学计算/批处理 (mm/util.c → __vm_enough_memory OVERCOMMIT_ALWAYS)
  - **2(严格)**: `allowed = (totalram - hugetlb) * overcommit_ratio/100 + swap` — 所有分配必须在 committed 上限内 (mm/util.c → __vm_enough_memory OVERCOMMIT_NEVER)
  - `overcommit_ratio` 默认 50%: 允许额外分配 RAM 的 50% → `/proc/meminfo CommitLimit/Committed_AS` 实时追踪

### 2. OOM Killer 选择流程 — select_bad_process 到发送 SIGKILL
  - 触发: Buddy 分配 + 所有回收尝试失败 → `__alloc_pages_may_oom → out_of_memory → oom_kill_process` (mm/oom_kill.c → out_of_memory)
  - (1) `select_bad_process(oc) → for_each_process(p) → oom_evaluate_task(p, oc)` (mm/oom_kill.c)
  - 跳过: `is_global_init`(PID 1) / `same_thread_group(current)` / `TIF_MEMDIE`(已被 kill) / `oom_unkillable_task` (mm/oom_kill.c)
  - (2) `oom_badness(p, oc, totalpages)`: `points = get_mm_rss(mm) + get_mm_counter(mm, MM_SWAPENTS) + mm→nr_ptes/2` (mm/oom_kill.c)
  - `points = points * 1000 / totalpages` 归一化 → `points += oom_score_adj` 加手动偏移 → 最高分者被选中
  - (3) `oom_kill_process(oc, chosen, points)` → 若有子进程不同 mm → 优先杀子进程 → `send_sig(SIGKILL, victim, 0)` → `TIF_MEMDIE` (mm/oom_kill.c)

### 3. OOM Reaper 收割 — 被杀进程物理页的异步回收
  - `wake_oom_reaper(victim) → queue_delayed_work(system_wq, &oom_reaper_wait, 0)` (mm/oom_kill.c)
  - `oom_reaper(victim) → mmap_read_lock(mm) → __oom_reap_task_mm` (mm/oom_kill.c)
  - `for_each_vma(vmi, vma)` MADV_DONTNEED 式 unmap → `unmap_page_range` 释放所有物理页 → `tlb_finish_mmu` (mm/oom_kill.c)
  - 阻塞进程(D 状态, 不可打断睡眠): 无法获取 mmap_lock → 等待 `MAX_OOM_REAP_RETRIES=10` 次(1s) → 失败 → `force_sig(SIGKILL)` 不等 reaper
  - `TIF_MEMDIE` 已设置 → alloc 路径快速返回失败 → 不重试

### 4. OOM Score 调整接口 — 保护关键进程
  - `/proc/PID/oom_score`(动态, 0-1000) vs `/proc/PID/oom_score_adj`(手动, -1000~1000) (fs/proc/base.c)
  - `-1000`: 永不杀; `1000`: 必杀 → `choom -n <score_adj> -p <PID>` 修改 (mm/oom_kill.c)
  - 保护示例: `echo -1000 > /proc/$(pidof sshd)/oom_score_adj`, 同理 mysqld/prometheus 等关键服务
  - `/proc/vmstat oom_kill` 统计历史 OOM 次数 → dmesg: `Out of memory: Killed process %d (%s)...`

### 5. OOM Report 解读 — dmesg OOM 日志逐行分析
  - `Out of memory: Killed process 12345 (java)` — 被杀进程 PID+名称
  - `total-vm:52345678kB` — 进程虚拟地址空间总量, `anon-rss:8234567kB` — 匿名常驻集(堆/栈), `file-rss:123456kB` — 文件映射常驻, `shmem-rss:45678kB` — 共享内存常驻
  - `oom_score_adj:0` — 手动偏移值, `UID:1000 pgtables:12345kB oom_score_adj:0` — 页表占用 + 评分调整

### 6. 收束
  - Overcommit 三策略(0 启发式/1 总是/2 严格), `overcommit_ratio` 默认 50%, `CommitLimit/Committed_AS` 实时追踪
  - `oom_badness = (RSS + swap_ents + ptes/2) * 1000 / totalpages` + `oom_score_adj` 手动偏移 → 最高分进程被杀
  - OOM Reaper 异步收割 mmap 方式释放物理页, D 态进程无法获取 mmap_lock → 最多重试 10 次后 SIGKILL 不等

---

### 核心悬念
**"线上出问题第一眼看什么？MemAvailable 的计算公式是什么？PSS 为什么比 RSS 精确？/proc/meminfo 和 /proc/PID/smaps 的每列怎么解读？"**

→ 引出 12 内存监控与统计接口 — MemAvailable/PSS/procfs/cgroup v2
