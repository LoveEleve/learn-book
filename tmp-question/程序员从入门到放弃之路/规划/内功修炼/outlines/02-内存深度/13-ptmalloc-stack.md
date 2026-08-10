# ptmalloc 与栈 — 用户态内存的 glibc 实现

> Cluster C: 2 KPs | 依赖: 12-memory-stats-tools | 读者基线: Buddy 分配器 + /proc/meminfo 解读

---

### 1. ptmalloc 核心结构 — glibc 的五级 bins 体系
  - `malloc(size) → __libc_malloc(bytes) → arena_get(ar_ptr, bytes)` 获取或创建 arena (glibc/malloc/malloc.c → __libc_malloc)
  - **fastbins**(≤64B, 默认 7 个): 单链表 LIFO → 不合并相邻 chunk → 极致速度, 减少碎片化 (glibc/malloc/malloc.c → fastbin)
  - **smallbins**(≤512B, 62 个): 双链表 → free 时与相邻 free chunk 合并 → `unlink_chunk` (glibc/malloc/malloc.c → smallbin)
  - **largebins**(>512B, 63 个): 排序双链表 → 按大小排序 → `best-fit` 查找 (glibc/malloc/malloc.c → largebin)
  - **unsorted bin**(1 个): 缓存层 → 任何刚释放的 chunk 先放这里 → 下次 malloc 先查 → 未命中才分类到 small/large (glibc/malloc/malloc.c → unsorted_chunks)
  - Arena 数量: 默认 `MALLOC_ARENA_MAX = 8 * cpu_cores` → 每个 arena 独立锁 → 减少多线程竞争

### 2. 系统调用分发 — brk vs mmap 的阈值决策
  - **brk 路径**(≤MMAP_THRESHOLD=128KB): 堆扩展 → `sys_brk → brk` → arena top chunk → top chunk 不够 → `sbrk → sys_brk` 扩展 program break (glibc/malloc/malloc.c → sys_brk)
  - brk 是线性的: 释放时若 top chunk 上的内存 → `malloc_trim(pad) → sbrk(-pad)` 缩回 OS → 分配/释放不触发系统调用(fastbin/tcache 缓存)
  - **mmap 路径**(>128KB): `sys_mmap` 独立分配 → 释放 `sys_munmap` 立即归还 → 无缓存 (glibc/malloc/malloc.c → mmap 分配)
  - ptmalloc vs Buddy: ptmalloc 在用户态(Buddy 在内核) → free 不一定触发系统调用 → 碎片化: 连续 alloc→free 产生 hole → Buddy `alloc_pages` 返回连续物理页无此问题

### 3. 双链表管理 — chunk 的复用与合并
  - `malloc_chunk`: `prev_size`(前一 chunk 大小) + `size`(本 chunk 大小, 低 3 位存标志: PREV_INUSE/IS_MMAPPED/NON_MAIN_ARENA) (glibc/malloc/malloc.c → struct malloc_chunk)
  - free chunk: `fd`(前向指针) + `bk`(后向指针, 仅 small/large/unsorted) → `unlink` 宏移除 (glibc/malloc/malloc.c → unlink_chunk)
  - 合并: `free → unlink_chunk` + 检查相邻 chunk 是否 free → `unlink` → 合并成更大 chunk → 放入 unsorted bin (glibc/malloc/malloc.c → _int_free)
  - tcache(glibc 2.26+): per-thread 缓存 → 每个 bin 最多 7 个 entry → 单链表无锁 → 先查 tcache → 再查 fastbin → 再查 small/large (glibc/malloc/malloc.c → tcache)

### 4. 进程栈与线程栈 — VM_GROWSDOWN + 保护页
  - **主线程栈**: `mm→start_stack → VM_GROWSDOWN` → 自动扩展 → `RLIMIT_STACK`(默认 8MB) → `expand_stack → acct_stack_growth → vma→vm_start -= PAGE_SIZE` (mm/mmap.c → expand_stack)
  - 栈缺页: 访问栈下未映射区域 → #PF → `do_anonymous_page → expand_stack` 自动分配匿名页 → 匿名页不重复分配 (mm/memory.c → do_anonymous_page)
  - **线程栈**: `pthread_create → mmap MAP_ANONYMOUS|MAP_STACK|MAP_GROWSDOWN` → `pthread_attr_setstacksize` 默认 2-8MB(glibc) → `/proc/PID/maps [stack:THREAD_ID]` (glibc/nptl/allocatestack.c → __pthread_create)
  - **保护页**: guard page(1页) → 栈溢出 → #PF → SIGSEGV 而非扩展 → `pthread_attr_setguardsize` 可调整保护页大小
  - 栈大小不足 → 无限递归 → 栈持续扩展 → 耗尽地址空间 → SIGSEGV — `pthread_getattr_np` 获取运行中线程栈的属性

### 5. 收束
  - ptmalloc 五级 bins: fastbins(≤64B LIFO 无合并)→smallbins(≤512B 双链表)→largebins(>512B best-fit)→unsorted bin(缓存层)→tcache(per-thread 无锁)
  - brk(≤128KB 堆线性扩展, 释放不还 OS) vs mmap(>128KB 独立映射, 释放立即归还): 系统调用分发阈值决策
  - 主线程栈 VM_GROWSDOWN 自动扩展 + guard page 溢出保护, 线程栈 mmap 独立分配 + pthread_attr_setstacksize 控制

---

### 核心悬念
**"从 struct page 到 swap out，我们已经走完了 Linux 内存管理的完整闭环。但帧间分配(Buddy)和用户态分配(ptmalloc)之间存在天然的断层 — 内核怎么应对这种分层带来的碎片化？终章没有下一篇了，带读者回顾全链路。"**

→ 引出 03-文件系统 — 从 Ext2 磁盘布局到 NFS/FUSE 跨域文件系统
