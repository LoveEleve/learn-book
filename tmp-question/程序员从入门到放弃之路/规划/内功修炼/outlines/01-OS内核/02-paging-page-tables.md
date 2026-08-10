# 分页机制 — 四级/五级页表 + PTE 标志位 + TLB

> Cluster A: 11 KPs | 依赖: 01-物理内存管理 | 读者基线: 了解虚拟地址与物理地址的区别

---

### 1. 四级页表遍历 — 虚拟地址到物理页帧
  - 虚拟地址 48 位拆分: PGD(9bit, bits 39-47) → PUD(9bit, bits 30-38) → PMD(9bit, bits 21-29) → PTE(9bit, bits 12-20) → Offset(12bit) — 每级索引 512 项 (arch/x86/include/asm/pgtable_64_types.h:16)
  - 遍历宏链: `mm→pgd → pgd_offset → p4d_offset → pud_offset → pmd_offset → pte_offset_map → pte_page` — 每步取 9bit 偏移，4 次内存访问 (arch/x86/include/asm/pgtable.h:776)
  - CR3 寄存器存 PGD 物理基址 → 每个进程独立页表 → 进程切换写 CR3 导致 TLB 全刷新(除非 ASID/PCID) (arch/x86/mm/tlb.c:395)
  - 五级页表(Linux 4.14+ Intel La57): PGD → P4D → PUD → PMD → PTE — 57 位虚拟地址 → 冰湖+ 支持 (Documentation/x86/x86_64/5level-paging.rst:3)

### 2. PTE 页表项 — 64 位中的权限与控制
  - PTE 64 位含义: Present(bit0) / RW(bit1) / User/Supervisor(bit2) / PWT(bit3) / PCD(bit4) / Accessed(bit5) / Dirty(bit6) / PAT(bit7) / Global(bit8) / PFN(bit12-51) / NX(bit63 禁止执行) (arch/x86/include/asm/pgtable_types.h:29)
  - Accessed 位: MMU 自动置 1 → 内核通过检查实现 LRU 老化(page_referenced) → 清除后等待再次 Access 判断活跃度 (mm/rmap.c:1190)
  - Dirty 位: 写操作时 MMU 置 1 → 表示页已被修改 → 回写时需要 Dirty 页 → 用于 COW 检测(共享页写前需检查 Dirty)
  - NX 位: 禁止执行 → W^X 缓冲区溢出防护 → 代码段不可写 / 数据段不可执行 → `dmesg | grep NX`

### 3. TLB — 页表硬件缓存与跨核刷新
  - TLB 容量: 通常 64-1024 项 / 全相联 / 命中 <1 cycle / 缺失需 4 次内存访问 → ASID/PCID 标记进程，减少上下文切换时全刷 (arch/x86/include/asm/tlbflush.h:36)
  - TLB shootdown: 修改页表 → IPI 中断其他 CPU → 其他 CPU `invlpg` 刷新单条 → `flush_tlb_mm_range` 按范围刷新 → `mm_cpumask` 记录哪些核在用此 mm (mm/rmap.c:1125)
  - `invlpg [addr]` 刷新单条 vs CR3 写刷新全部 → `INVPCID` 新指令(指定 PCID 和地址) → 更精细的 TLB 管理

### 4. 大页 — 用更大的页面减少 TLB miss
  - 2MB 大页(PMD 级别): PTE 跳过 → PMD 直接指向 2MB 物理页 → TLB 覆盖 512 倍地址空间 → hugetlbfs 文件系统 (arch/x86/kernel/cpu/common.c:1520)
  - THP 透明大页: 内核自动尝试分配 2MB → khugepaged 扫描合并 4KB 页 → fragmentation 严重时分配延迟数十 ms → MongoDB/Redis 建议 `transparent_hugepage=never` (mm/huge_memory.c:1397)
  - 控制: `/sys/kernel/mm/transparent_hugepage/enabled`(always/madvise/never) → `defrag` → 启动参数: `hugepagesz=2M hugepages=1024` → `/proc/meminfo HugePages_Total`

### 5. 内核页表 vs 进程页表
  - init_mm 内核页表模板 → 进程 fork 时拷贝 → 内核地址映射在 pgd[511](0xFFFF800000000000+) → 0xffff888000000000(物理内存直接映射区) → vmemmap(struct page 数组) → vmalloc 区(非连续) (arch/x86/mm/init_64.c:835)

### 6. 收束
  - 四级页表遍历 = 4 次内存访问 + TLB 缓存 = 高效虚拟内存的基础
  - PTE 标志位是 COW/回收/预读/安全机制的核心触发器
  - 大页通过减少页表级数来降低 TLB miss，但带来碎片和延迟代价

---

### 核心悬念
**"页表建立映射了，mmap 怎么分配虚拟地址还不立即分配物理页？缺页异常什么时触发？"**

→ 引出 03-mmap + VMA + 缺页异常 + COW 写时拷贝
