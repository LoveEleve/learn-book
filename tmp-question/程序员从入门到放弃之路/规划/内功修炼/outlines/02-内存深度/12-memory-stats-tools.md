# 内存监控与统计接口 — 从 /proc 到 cgroup v2

> Cluster C: 4 KPs | 依赖: 11-overcommit-oom | 读者基线: Overcommit 策略 + OOM Killer 全流程

---

### 1. MemAvailable 计算 — 真正可用的内存
  - `si_mem_available()`: `MemAvailable = MemFree + pagecache + slab_reclaimable - (watermark_low + reserved)` (mm/page_alloc.c → si_mem_available)
  - `MemFree`(`/proc/meminfo`) — 完全空闲的页 → `pagecache` = `NR_ACTIVE_FILE + NR_INACTIVE_FILE`(可回收文件页) → `slab_reclaimable` = `NR_SLAB_RECLAIMABLE_B`
  - `reserved` = 每个 zone 的 `watermark[WMARK_LOW]` 预留 — 比 MemFree 准 — 避免"明明有 free 却 OOM"的困惑

### 2. /proc/meminfo 全字段解读
  - 核心指标: MemTotal, MemFree, MemAvailable, Buffers, Cached, SwapCached, SwapTotal, SwapFree (fs/proc/meminfo.c → meminfo_proc_show)
  - LRU 状态: Active, Inactive, Active(anon), Inactive(anon), Active(file), Inactive(file), Unevictable, Mlocked (fs/proc/meminfo.c → meminfo_proc_show)
  - IO 状态: Dirty, Writeback, NFS_Unstable, Bounce, WritebackTmp (fs/proc/meminfo.c → meminfo_proc_show)
  - Slot 用户: AnonPages, Mapped, Shmem, KReclaimable, Slab, SReclaimable, SUnreclaim, KernelStack, PageTables (fs/proc/meminfo.c → meminfo_proc_show)
  - Overcommit: CommitLimit, Committed_AS, VmallocTotal, VmallocUsed, VmallocChunk (fs/proc/meminfo.c → meminfo_proc_show)
  - **大页相关**: AnonHugePages, ShmemHugePages, ShmemPmdMapped, FileHugePages, FilePmdMapped, HugePages_Total, HugePages_Free, HugePages_Rsvd, HugePages_Surp, Hugepagesize (fs/proc/meminfo.c → meminfo_proc_show)
  - Cgroup: CmaTotal, CmaFree (fs/proc/meminfo.c → meminfo_proc_show)

### 3. PSS 与 /proc/PID/smaps — 精确到字节的进程内存画像
  - `/proc/PID/smaps`: 每个 VMA → Rss, Size, Pss, Shared_Clean, Shared_Dirty, Private_Clean, Private_Dirty (fs/proc/task_mmu.c → show_smap)
  - **PSS**(Proportional Set Size): `Private + Shared / n_shared` — 两进程共享 10MB 库 → 每进程 PSS = Private + 5MB (fs/proc/task_mmu.c → show_smap)
  - `smaps_rollup`(4.14+): 汇总所有 VMA → Pss_Anon, Pss_File, Pss_Shmem — 比 RSS 更精确衡量真实内存占用 (fs/proc/task_mmu.c → show_smaps_rollup)
  - `/proc/PID/status`: VmPeak, VmSize, VmLck, VmPin, VmHWM, VmRSS, RssAnon, RssFile, RssShmem, VmData, VmStk, VmExe, VmLib, VmPTE, VmSwap (fs/proc/task_mmu.c → task_mem)
  - htop 颜色解读: 蓝色=buffer, 绿色=cache, 黄色=swap, 红色=used

### 4. procfs vm 参数全集 — 运行时调优开关
  - 水位: `min_free_kbytes`(默认 ~4MB, 大内存建议增), `watermark_scale_factor`(10/1000=0.1%) (mm/page_alloc.c → min_free_kbytes)
  - 脏页: `dirty_ratio`(20%), `dirty_background_ratio`(10%), `dirty_expire_centisecs`(3000=30s), `dirty_writeback_centisecs`(500=5s) (mm/page-writeback.c → dirty_ratio_handler)
  - 回收: `swappiness`(0-200, 默认 60), `vfs_cache_pressure`(100) (mm/vmscan.c → swapiness_handler)
  - Overcommit: `overcommit_memory`(0/1/2), `overcommit_ratio`(50%) (mm/util.c → overcommit_memory_handler)
  - OOM: `oom_kill_allocating_task`(0=杀最坏/1=杀当前), `panic_on_oom`(0=仅 OOM/1=OOM+cgroup/2=panic) (mm/oom_kill.c → oom_kill_allocating_task_handler)

### 5. 内存 cgroup v2 — 容器环境的内存控制
  - 接口: `memory.max`(硬限制), `memory.high`(软限制, 节流), `memory.low`(尽力保障), `memory.min`(硬保障) (mm/memcontrol.c → memory_max_write)
  - 统计: `memory.stat`(anon/file/swap/pgfault/kmem/slab), `memory.events`(low/high/max/oom/oom_kill) (mm/memcontrol.c → memory_stat_show)
  - cgroup OOM: 比全局 OOM 先触发 → `mem_cgroup_oom → mem_cgroup_out_of_memory → oom_kill_process` (mm/memcontrol.c → mem_cgroup_out_of_memory)
  - 使用场景: Docker `--memory`, Kubernetes `resources.limits.memory` 底层实现 — 内核对每个 cgroup 维护独立 LRU (mm/memcontrol.c → mem_cgroup_css_alloc)

### 6. 收束
  - MemAvailable = MemFree + 可回收页缓存 + 可回收 slab - 水位预留, 比 MemFree 准确, 避免"明明有 free 却 OOM"的困惑
  - PSS = Private + Shared/n_shared 精确衡量共享内存真实占用, smaps_rollup 汇总所有 VMA
  - cgroup v2 memory.max/high/low/min 四级限制 + memory.stat 统计 + 独立 OOM, 容器环境底层实现

---

### 核心悬念
**"内核用了 Buddy 分配器，那用户态 malloc 怎么实现的？ptmalloc 的 fastbins/smallbins/largebins/unsorted bin 和 Buddy 是什么关系？栈怎么自动增长？"**

→ 引出 13 ptmalloc 与栈 — glibc 用户态分配器 + 进程/线程栈
