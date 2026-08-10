# 页表与 TLB — 从虚拟地址到物理地址的五级跳

> Cluster B: 6 KPs | 依赖: 02-buddy-pcp-gfp | 读者基线: Buddy 分配器 + 虚拟内存概念

---

### 1. 四级页表完整结构 — PGD → PUD → PMD → PTE
  - 虚拟地址 48 位分解(4 级): PGD(9) → PUD(9) → PMD(9) → PTE(9) → offset(12), 57 位(5 级 La57) (arch/x86/include/asm/pgtable_64_types.h)
  - 每级数据类型: `pgd_t/p4d_t/pud_t/pmd_t/pte_t` — 各 8 字节 (arch/x86/include/asm/pgtable_64_types.h)
  - 遍历宏链: `pgd_offset(mm, addr) → p4d_offset(pgd, addr) → pud_offset(p4d, addr) → pmd_offset(pud, addr) → pte_offset_map(pmd, addr)` (arch/x86/include/asm/pgtable.h)
  - `pgd_none(*pgd)` 检查空条目(全 0) → `p4d_alloc/pud_alloc/pmd_alloc` 逐级分配中间页表 (mm/memory.c → pgd_none)
  - P4D 5 级用, non-5LEVEL 时 `p4d_offset` 透明折叠 → P4D→PUD 旁路, CR3 → 物理地址 → 4 次内存访问 = 1 次完整 walk

### 2. PTE 64 位标志位全集 — 硬件 + 软件双域控制
  - **硬件域(bits 0-8,12-51,63)**: bit0 `_PAGE_PRESENT`(0x001), bit1 `_PAGE_RW`(0x002, 0 时写触发 COW), bit2 `_PAGE_USER`(0x004), bit5 `_PAGE_ACCESSED`(0x020), bit6 `_PAGE_DIRTY`(0x040), bit7 `_PAGE_PSE`(2MB), bit8 `_PAGE_GLOBAL`(TLB 不刷新), bit12-51 PFN, bit63 `_PAGE_NX`(No-eXecute) (arch/x86/include/asm/pgtable_types.h)
  - **软件域(bits 9-11)**: `_PAGE_SPECIAL`(bit9, 非 struct page), `_PAGE_PROTNONE`(bit8 复用, NUMA 迁移), `_PAGE_SOFT_DIRTY`(bit11, CRIU 迁移)
  - 操作函数: `set_pte(ptep, pte)`, `ptep_set_wrprotect`(写保护+TLB flush), `ptep_set_access_flags`(设 Accessed+Dirty+可写), `pte_mkclean/pte_mkdirty/pte_mkold/pte_young` (arch/x86/include/asm/pgtable.h)

### 3. 页表锁分级 — 从全局锁到 split PTL
  - 旧式(2.6.23 前): `mm→page_table_lock` 全局自旋锁 — 所有操作争用同一锁 (mm/memory.c → page_table_lock 历史)
  - **PMD lock**: THP(大页=PMD 级映射) → `pmd_lock(mm, pmd) → pmd_lockptr → spin_lock` (mm/pgtable-generic.c)
  - **PTE lock**: 普通页 → `pte_lockptr(mm, pmd) → spin_lock(ptl)` (mm/memory.c → pte_offset_map_lock)
  - 组合函数: `pte_offset_map_lock(mm, pmd, addr, &ptl)` 获取, `pte_unmap_unlock(ptep, ptl)` 释放 — 每个 PTE 表对应一个 spinlock

### 4. TLB 硬件组织 — 页表缓存的层次结构
  - L1 TLB: ITLB(指令)+DTLB(数据)分离, 全相联/组相联, ~64 项, 命中 latency~1 cycle
  - L2 TLB: ~1024-2048 项, 统一, miss → PMH(Page Miss Handler) 硬件 walk → 查页表 → 填充 TLB
  - PCID(x86)/ASID(ARM): 12 位 → 4096 个标签, TLB 条目打 PCID 标签 → 上下文切换不刷 TLB, 只切换 CR3 低 12 位
  - `invlpg addr`(单条 TLB 无效化) vs `mov %cr3, %rax; mov %rax, %cr3`(全刷新, ~1000+ cycles)

### 5. TLB shootdown — 跨核刷新的一致性协议
  - 触发: mprotect/munmap/KSM 合并/THP 拆分 → 修改页表 → 无效其他 CPU 的 stale TLB (arch/x86/mm/tlb.c)
  - `flush_tlb_mm_range(mm, start, end) → smp_call_function_many` 向 mm→cpumask 所有核发 IPI
  - IPI vector=0xfd(CALL_FUNCTION_VECTOR), 目标核中断上下文执行 `flush_tlb_func → __flush_tlb_one_user(addr): invlpg addr`
  - 优化: `tlb_is_not_lazy` 检测 → lazy TLB 延迟刷新, `mm_cpumask(mm)` 精确定位 — 频繁 munmap 导致大量 IPI

### 6. 大页 — TLB 覆盖率的指数级提升
  - 2MB page: 虚拟地址 bit21-47 直接映射 → PMD 级 → 1 个 TLB 条目 = 512 个 4KB 条目
  - 1GB page: bit30-47 → PUD 级 → Gigantic Huge Page (arch/x86/mm/hugetlbpage.c)
  - `hugetlb_fault → hugetlb_no_page → alloc_huge_page` (mm/hugetlb.c)
  - 配置: `hugepagesz=2M hugepages=1024`, `/proc/sys/vm/nr_hugepages`, `mmap MAP_HUGETLB`, 缺点: compaction + 碎片

### 7. CPU 缓存层次与 TLB 的硬件关系
  - TLB 位于 L1 缓存附近, TLB miss → PMH 硬件页表 walk → 每次 walk 4 次内存访问(L1→L2→L3→DRAM), 命中 L1 缓存则<10 cycles, DRAM 则>100 cycles
  - L1/L2/L3 缓存用物理地址索引(physically indexed, physically tagged), TLB 翻译必须在 L1 缓存访问之前完成 → TLB 命中率直接影响所有内存访问延迟
  - CPU 硬件预取器与 TLB 独立工作 → 预取器可能预取未映射页 → 浪费带宽 →`/sys/devices/system/cpu/cpu*/cache/` 查看缓存拓扑

### 8. 收束
  - 四级(48位)→五级(57位 La57)页表逐级 9 位索引, PTE 64 位中硬件域 (Present/RW/NX) 与软件域 (SPECIAL/PROTNONE/SOFT_DIRTY) 分治
  - PTE lock(普通页)/PMD lock(THP) 替代旧式全局 `mm→page_table_lock`, 每 PTE 表一个 spinlock 提升并发
  - TLB 多级缓存(PCID/ASID)减少上下文切换刷新, TLB shootdown IPI 跨核一致性, 大页(2MB/1GB)指数级提升 TLB 覆盖率

---

### 核心悬念
**"进程的地址空间怎么组织？mm_struct 和 VMA 红黑树 + 链表双索引怎么让内核 O(log n) 找到任意地址的 VMA？"**

→ 引出 04 mm_struct 与 VMA — 进程地址空间的数据结构蓝图
