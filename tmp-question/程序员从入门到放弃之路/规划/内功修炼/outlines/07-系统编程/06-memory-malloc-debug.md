# 进程地址空间 + 伪内存泄漏 + 调度 — /proc/PID/maps 全景 + 排障案例 + nice/CPU 亲和性

> Cluster B: 8 KPs | 依赖: A (01-02) + B (03-05) | 读者基线: 理解 malloc/free 和进程/线程概念

---

### 1. 进程地址空间 — /proc/PID/maps 逐区解剖
  - `cat /proc/PID/maps` 输出: `address perms offset dev inode pathname` — 每区一段(segment)vs 实际段(.text/.data/.bss/heap/mmap stack/vDSO/vvar) (man 5 proc)
  - Text(.text): r-xp — 代码段只读只执行，同一可执行文件的所有进程共享 (B1 Ch9 §1)
  - Data/BSS: rw-p — 全局变量(.data 已初始化, .bss 零初始→匿名页) — 写时才分配物理页 (B1 Ch9 §1)
  - Heap: `[heap]` — brk 区，malloc 小分配来源 → 地址区间随 `sbrk` 向上增长 (B1 Ch9 §2)
  - Mmap 区: 匿名映射(MAP_ANONYMOUS=大 malloc)+文件映射+共享库 → 地址区间在堆上方(通常) (B1 Ch9 §3)
  - Stack: `[stack]` — 从高地址向低地址增长 → `RLIMIT_STACK`(默认 8MB) → 递归过深=stack overflow→SIGSEGV (B1 Ch9 §1)
  - vDSO/vvar: 内核只读页映射到用户态→部分系统调用(clock_gettime 在不进内核的路径上) → 快速 gettimeofday (B1 Ch9 §1)

### 2. 匿名 mmap + mlock — 大内存分配与物理页锁定
  - `mmap(NULL, sz, PROT_READ|PROT_WRITE, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0)`: >128KB 分配等价——不占用 brk 区 (man 2 mmap)
  - `mlock(p, len)`: 锁定物理页防换出 → RLIMIT_MEMLOCK 上限(默认 64KB!)→ 需要 CAP_IPC_LOCK 或提高 limit (man 2 mlock)
  - `munmap(p, sz)`: 释放映射——注意 SIZE 必须对齐页(4KB/2MB/1GB) → 未对齐时按页边界隐式对齐 (man 2 munmap)
  - `madvise(p, len, MADV_DONTNEED/MADV_WILLNEED/MADV_SEQUENTIAL)`: 内核访问模式提示→主动释放脏页/预读 (man 2 madvise)

### 3. malloc_info — 内存统计与碎片分析
  - `malloc_info(0, stdout)`: XML 输出 `<heap nr="0">` → `sizes`(每线程分配大小分布) + `total`(system mmap 用量) (man 3 malloc_info)
  - `mallinfo()`: arena 综合统计(fordblks=空闲块总和/uordblks=使用量/hblkhd=mmap用量) — 即 `printf("%d\n", mallinfo().fordblks)` (man 3 mallinfo)
  - `malloc_stats()`: 文本输出 arena 系统总量 → `malloc_stats()` 到 stderr (man 3 malloc_stats)
  - 碎片判断: `malloc_info` 中 `<unsorted>` bin 积压 → 内存碎片化 → arena 空闲但无法分配连续大块 (B1 Ch9 §6)

### 4. 伪内存泄漏排查 — 增长 ≠ 泄漏
  - 伪泄漏经典: RSS 上升但 `mallinfo().fordblks` 不降 → glibc arena 不归还(性能优化：keep chunk for reuse) → 不是真泄漏只是效率 trade-off (B2 Ch9 §1-2)
  - 排查链: `ps aux` RSS 上升 → `smem -P PID` PSS/USS → `gdb -p PID` → `call malloc_info(0, stderr)` → `call malloc_trim(0)`(强制归还是否降)→ 不降是真泄漏 (B2 Ch9 §1)
  - 周期性事故: 业务低峰用 `malloc_trim(0)` 归还 → 内存"尖峰平滑" → 配合 `mallopt(M_TRIM_THRESHOLD, ...)` 调低归还阈值 (B2 Ch9 §2)
  - `valgrind --leak-check=full --show-leak-kinds=all ./prog` — 检测真泄漏(但开销 10x-50x，不能在产线长时间跑) (B2 Ch9 §2)

### 5. 调度与资源限制 — nice/sched/rlimit
  - `nice(inc)`: 调整静态优先级 nice(-20~19，越小优先级越高) → 实际是 CFS 权重 → `nice -n -5 ./prog` (man 2 nice)
  - `sched_setscheduler(pid, SCHED_FIFO/SCHED_RR/SCHED_OTHER, &sp)`: 实时优先级(1-99)→SCHED_FIFO(不抢占⏱同一线程)/SCHED_RR(时间片轮转)——需 CAP_SYS_NICE (man 2 sched_setscheduler)
  - CPU 亲和性: `sched_setaffinity(pid, sizeof(cpuset), &mask)` → 绑核(减少 cache miss/避免 NUMA 跨节点) — `taskset -c 0-3 ./prog` (man 2 sched_setaffinity)
  - `getrlimit/setrlimit(resource, &rlim)`: RLIMIT_NOFILE(fd上限 1024→65536)/RLIMIT_NPROC(进程上限)/RLIMIT_CORE(core dump 大小)/RLIMIT_MEMLOCK(mlock 上限) → 守护进程必须提高 (man 2 getrlimit)

### 6. 收束
  - `/proc/PID/maps` 是进程地址空间的"房产证"——每区域精确记录起始地址、权限、是否文件后备——排障第一步不是 gdb，而是 cat maps
  - 伪内存泄漏是性能优化 vs 系统压力的权衡——glibc arena 不归还碎片是为了下次分配更快，但对容器 RSS quota 是灾难
  - nice→nice+1≈CFS 权重降低 10%—不是 1:39 的优先级; CPU 亲和性与 NUMA 绑核需一起考虑

---

### 核心悬念
**"进程地址空间、线程调度、信号——都是在内核的'上层'。那 CPU 本身怎么保证多核写同一缓存行数据的正确性？MESI 协议是怎么工作的？"**

→ 引出 07-C++ atomic + 无锁编程 — MESI/内存屏障/CAS/无锁队列
