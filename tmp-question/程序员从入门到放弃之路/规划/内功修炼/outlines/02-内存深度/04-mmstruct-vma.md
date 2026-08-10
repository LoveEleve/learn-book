# mm_struct 与 VMA — 进程地址空间的蓝图

> Cluster B: 4 KPs | 依赖: 03-pagetables-tlb | 读者基线: 页表四级遍历 + TLB shootdown

---

### 1. mm_struct 完整字段 — 进程的虚拟内存控制块
  - `struct mm_struct` 核心字段全景: `mmap`(VMA 链表), `mm_rb`(VMA 红黑树), `pgd`(进程级 PGD), `mmap_lock(rwsem)` (include/linux/mm_types.h → struct mm_struct)
  - 引用计数双通道: `atomic_t mm_users`(用户态引用: 进程+线程共享) ≠ `atomic_t mm_count`(内核引用: page fault handler/OOM reaper → mmgrab/mdrop)
  - 地址空间边界: `task_size`(用户空间上限 TASK_SIZE_MAX=47 位 128TB), `mmap_base`(mmap 基址), `start_code/end_code/start_data/end_data`(代码/数据段)
  - 统计字段: `total_vm, locked_vm, pinned_vm, data_vm, exec_vm, stack_vm` — `/proc/PID/status` 来源
  - 栈与堆边界: `start_stack`(主线程栈), `start_brk/brk`(堆边界, sbrk 使用), `arg_start/arg_end/env_start/env_end`(命令行+环境变量)

### 2. VMA (vm_area_struct) — 虚拟地址区间的完整描述
  - 设计考量: 红黑树 = O(log n) 快速查找任意地址的 VMA(`find_vma`), 链表 = O(1) 顺序遍历所有 VMA(`/proc/pid/maps` 需要按地址输出) — 双索引各自服务不同访问模式
  - `vm_start, vm_end` — 半开区间 [start, end), 按地址排序的链表(`vm_next/vm_prev`) + 红黑树(`mm_rb`) 双索引 (include/linux/mm_types.h → struct vm_area_struct)
  - `vm_flags` 全集: VM_READ|VM_WRITE|VM_EXEC|VM_SHARED|VM_GROWSDOWN(栈)|VM_GROWSUP|VM_DONTEXPAND|VM_LOCKED(mlock)|VM_IO|VM_SEQ_READ(顺序预读)|VM_RAND_READ(随机)|VM_HUGETLB|VM_ACCOUNT|VM_NORESERVE|VM_MERGEABLE(KSM) (include/linux/mm.h)
  - `vm_file + vm_pgoff` — 文件映射时: 指向 file, vm_pgoff 为文件偏移(PAGE_SHIFT 单位); 匿名映射时: NULL
  - `vm_ops`: `struct vm_operations_struct` — open/close/fault/page_mkwrite/pfn_mkwrite (include/linux/mm.h)
  - `vm_page_prot` — PTE 保护位模板, `vm_private_data` — 扩展私有数据

### 3. VMA Merge & Split — 地址空间的动态重组
  - `vma_merge(mm, prev, addr, end, vm_flags, ...)`: 检查相邻 VMA 兼容性(相同 vm_flags + 相同 vm_file + vm_pgoff 连续 + anon_vma 一致 + policy 一致) → 合并 (mm/mmap.c → vma_merge)
  - `__split_vma(mm, vma, addr, new_below)`: 在 addr 处分裂 → 分配新 vm_area_struct → 复制字段 → 调整 vm_start/vm_end → 插入双索引 (mm/mmap.c → __split_vma)
  - `vma_adjust`: 通用 adjust → 处理合并+分裂+扩展+收缩 4 种场景 → 多 VMA 级联 (mm/mmap.c → vma_adjust)
  - 合并意义: 减少 VMA 数量 → 减小 find_vma 开销 → 减小 `/proc/pid/maps` 大小, VMA 过多导致 mmap_lock 竞争

### 4. mmap 内核完整路径 — syscall 到 VMA 建立的全过程
  - `sys_mmap → ksys_mmap_pgoff → vm_mmap_pgoff → do_mmap` (mm/mmap.c → do_mmap)
  - **Step 1**: `get_unmapped_area(file, addr, len, pgoff, flags)` — 找空闲虚拟地址: 自下而上 first-fit / 顶部向下(ASLR) (arch/x86/kernel/sys_x86_64.c)
  - **Step 2**: `mmap_region` — 检查地址空间配额(`may_expand_vm → RLIMIT_AS`) → `vm_area_alloc` 创建 VMA → `vma_link` 插入双索引 (mm/mmap.c → mmap_region)
  - **Step 3**: `call_mmap(file, vma)` — 文件: `ext4_file_mmap → generic_file_mmap → vma→vm_ops=&ext4_file_vm_ops`; 匿名: `vma_set_anonymous → shmem_zero_setup` (mm/mmap.c → call_mmap)
  - mmap flags: MAP_SHARED(写回文件)/MAP_PRIVATE(COW)/MAP_ANONYMOUS/MAP_FIXED/MAP_LOCKED(mlock)/MAP_POPULATE(预分配→`mm_populate→get_user_pages`)/MAP_HUGETLB
  - munmap: `sys_munmap → __do_munmap → unmap_region → unmap_vmas → zap_page_range` 释放页表+物理页 → `remove_vma_list` (mm/mmap.c → __do_munmap)

### 5. 收束
  - `mm_struct` 是进程地址空间控制块, `mmap` 链表 + `mm_rb` 红黑树双索引实现 O(log n) VMA 查找
  - VMA merge/split 动态重组减少 VMA 数量 → 降低 `find_vma` 开销 + `/proc/pid/maps` 大小
  - mmap 全路径: 找空闲地址(get_unmapped_area) → 创建 VMA → 插入双索引 → 设置 vm_ops(文件/匿名)

---

### 核心悬念
**"VMA 建好了，第一次访问虚拟地址触发缺页异常 — 匿名页、COW、文件页、栈扩展四种场景的 #PF 处理路径各有什么区别？"**

→ 引出 05 缺页异常四场景 — do_anonymous_page / do_wp_page / do_fault / expand_stack
