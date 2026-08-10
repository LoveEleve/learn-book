# mmap + VMA + 缺页异常 + COW 写时拷贝

> Cluster A: 10 KPs | 依赖: 02-分页机制 | 读者基线: 了解页表遍历和虚拟地址空间概念

---

### 1. mmap — 分配虚拟地址空间但不分配物理页
  - mmap 类型四类: File-backed(页缓存) / Anonymous(匿名, demand-zero) / Shared(MAP_SHARED, 多进程共享) / Private(MAP_PRIVATE, COW) (mm/mmap.c:1610)
  - brk vs mmap: brk 扩展堆(连续增长, sbrk 系统调用) → mmap 任意地址 → malloc 小用 brk 大用 mmap(默认 >128KB) — 阈值 `M_MMAP_THRESHOLD` (glibc malloc)
  - mmap 标志: MAP_SHARED / MAP_PRIVATE / MAP_ANONYMOUS / MAP_FIXED(精确指定地址, 危险) / MAP_LOCKED(锁内存防换出) (include/uapi/linux/mman.h:20)

### 2. VMA 虚拟内存区域 — 进程地址空间的结构
  - `struct vm_area_struct`: vm_start / vm_end(区间) / vm_flags(权限位) / vm_file(映射文件, 匿名则为 NULL) / vm_ops(操作集, fault/close/split) — 由 mm_struct 的红黑树 + 链表双重索引 (include/linux/mm_types.h:313)
  - vm_flags 关键位: VM_READ(0x1) / VM_WRITE(0x2) / VM_EXEC(0x4) / VM_SHARED(0x8) / VM_GROWSDOWN(栈) / VM_GROWSUP / VM_DONTEXPAND / VM_ACCOUNT (include/linux/mm.h:254)
  - `struct mm_struct`: mmap(VMA 链表头) / mm_rb(红黑树根) / pgd(页表基址) / mm_users(用户引用计数) / start_code / end_code / start_data / end_data / start_brk / brk / start_stack (include/linux/mm_types.h:413)
  - 可视化: `/proc/PID/maps`(文本) / `/proc/PID/smaps`(详细统计) / `pmap PID`(摘要) → 每行 = 一个 VMA

### 3. 缺页异常 — 从 #PF 到物理页的三个分支
  - 触发: 访问虚拟地址 → MMU 查 TLB 未命中 → 查页表 → Present=0 → 触发 #PF 异常 → `do_page_fault` → `handle_mm_fault` (arch/x86/mm/fault.c:1290)
  - Minor Fault: 页已分配但页表未建立 → 只需建立页表映射 → 最快(无需 IO) → `do_anonymous_page` 或 `do_fault`(filemap_fault) (mm/memory.c:3713)
  - Major Fault: 页不在物理内存 → 需磁盘 IO(file-backed) 或 swap(匿名页) → 最慢(可能阻塞数十 ms) → `do_swap_page` 或 `do_fault(→readpage→磁盘IO)` (mm/memory.c:3180)
  - Invalid Fault: 地址不在任何 VMA 内 → `bad_area` → `force_sig(SIGSEGV)` → 进程终止 → 段错误 (arch/x86/mm/fault.c:861)

### 4. COW 写时拷贝 — fork 的灵魂优化
  - fork 子进程: 复制页表(父子共享物理页) → 父子页表都标记只读(_RW 清零) → 任一写触发 #PF → `do_wp_page` → 分配新物理页 → 复制内容 → 更新页表为可写 (mm/memory.c:3005)
  - COW 检测: PTE 中 _RW 清零但 VMA vm_flags & VM_WRITE 为真 → #PF → 判断 COW → 非 COW 则 SIGSEGV
  - overcommit 参数: `vm.overcommit_memory`(0=启发式 / 1=总是允许 / 2=严格按比例) → `vm.overcommit_ratio`(严格模式的百分比阈值) → 数据库建议设为 2

### 5. 收束
  - mmap 是"承诺分配"而非"立即分配" — 延迟分配(deferred allocation)减少物理内存浪费
  - COW 是 fork 的高性能秘诀(不拷贝数据只拷贝页表)
  - VMA 是缺页异常的判据 — 地址不在 VMA 内 = SIGSEGV

---

### 核心悬念
**"页缓存是文件 IO 和缺页异常的中间层 — 读写文件时不直接进磁盘，而是先进页缓存再回写。那回收时怎么挑哪个页驱逐？"**

→ 引出 04-页缓存 + 脏页回写 + LRU 回收 + Swap + OOM Killer
