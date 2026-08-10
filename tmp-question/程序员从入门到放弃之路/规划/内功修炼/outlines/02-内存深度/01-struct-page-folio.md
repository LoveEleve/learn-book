# struct page — 内核物理页的 64 字节身份证

> Cluster A: 8 KPs | 依赖: 无 | 读者基线: C语言+基本虚拟内存概念

---

### 1. struct page — 物理页描述符的 64 字节布局
  - 设计考量: 64 字节 = CPU 缓存行宽度(x86 cache line = 64B), 一个 cache line 精确装一个 struct page, 避免跨行访问减少 cache miss — 这就是为什么不能缩到 48 字节
  - `struct page` 核心字段: `unsigned long flags`(PG_locked/PG_dirty/PG_writeback/PG_reclaim/PG_swapbacked/PG_uptodate/PG_referenced/PG_unevictable/PG_mlocked/PG_error/PG_slab 按位编码) (include/linux/mm_types.h → struct page)
  - `atomic_t _refcount` — 引用计数: `page_ref_inc/page_ref_dec`, 0=可回收 (include/linux/page_ref.h → page_ref_inc)
  - `atomic_t _mapcount` — 页表映射数: `page_mapcount()`, -1=无映射, 0=单映射 (include/linux/mm_types.h → struct page)
  - `struct address_space *mapping` — file-backed→address_space / anonymous→anon_vma→root 双重语义 (mm/filemap.c → __filemap_get_folio)
  - `pgoff_t index` — 在 mapping 中的偏移, 文件偏移 >> PAGE_SHIFT (include/linux/mm_types.h → struct page)
  - `void *private` — buffer_head for buffers / swp_entry_t for swap (include/linux/mm_types.h → struct page)
  - `struct list_head lru` — LRU 链表节点, 通过 pagevec 批量操作 (include/linux/mm_types.h → struct page)
  - vmemmap 数组存放: 1GB 物理内存需 16MB struct page, `pfn_to_page(pfn)`/`page_to_pfn(page)` 转换 (mm/sparse.c → pfn_to_page)

### 2. PG_* 标志位全集 — 页状态的按位编码
  - PG_locked(bit0): `folio_trylock/folio_lock/folio_unlock` → bit_spin_lock 实现 (include/linux/page-flags.h)
  - PG_dirty(bit4): `folio_mark_dirty/folio_clear_dirty_for_io` — 脏页标记与 IO 清理 (mm/page-writeback.c)
  - PG_writeback(bit6): `folio_start_writeback/folio_end_writeback` — IO 进行中保护 (mm/page-writeback.c)
  - PG_reclaim(bit17) / PG_swapbacked(bit18) / PG_uptodate(bit2) / PG_referenced(bit3) — 回收 & 访问标志
  - PG_unevictable(bit19: mlock) / PG_mlocked(bit20) / PG_error(bit1) — 不可回收 & 错误标志
  - `folio_test_*`/`folio_set_*`/`folio_clear_*` 宏家族 — 原子与非原子版本混用, page_flags 非原子读存在竞态

### 3. struct folio — 5.16+ 的多页抽象革命
  - `struct folio { struct page page; }` — 嵌入首个 page 的超集, 解决 compound page 碎片化 (include/linux/page-flags.h → struct folio)
  - 核心字段: `unsigned long flags`(folio 级), `unsigned int _folio_order`(阶数, 0=单页), `atomic_t _refcount`
  - API: `folio_order(folio)`/`folio_nr_pages(folio)`(1<<order), `folio_page(folio, n)` 取第 n 页
  - `folio_lock/folio_end_read/folio_end_writeback` — folio 级锁替代 page 级, 减少多页 subpage 独立锁开销
  - 转换: `page_folio(page) → compound_head(page) → folio` (include/linux/page-flags.h → page_folio)

### 4. 物理内存模型 — Sections + PFN + Node + Zone
  - SPARSEMEM: `SECTION_SIZE_BITS=30`(1 section=1GB), `mem_section[NR_SECTION_ROOTS][SECTIONS_PER_ROOT]` 稀疏存储 (include/linux/mmzone.h → struct mem_section)
  - PFN: 物理地址 >> PAGE_SHIFT, `pfn_to_page/pfn_valid(pfn)`, PFN_ALIGN 对齐 (include/asm-generic/memory_model.h → pfn_to_page)
  - NUMA Node: `pg_data_t *node_data[MAX_NUMNODES]`, `pgdat→node_zones[ZONELIST_FALLBACK]`, `pgdat→node_start_pfn` (include/linux/mmzone.h → struct pglist_data)
  - Zone 类型: ZONE_DMA(0-16MB)/ZONE_DMA32(0-4GB)/ZONE_NORMAL(直接映射区)/ZONE_HIGHMEM(32位,64位无)/ZONE_MOVABLE(可移动) (include/linux/mmzone.h → zone_type)
  - `struct zone`: `watermark[NR_WMARK]`, `free_area[MAX_ORDER]`, `per_cpu_pageset __percpu *pageset`, `spinlock_t lock` (include/linux/mmzone.h → struct zone)

### 5. Direct Mapping — 物理内存到内核虚拟地址的直接映射
  - `__va(phys) = phys + PAGE_OFFSET` (PAGE_OFFSET=0xffff880000000000) — 物理地址线性偏移 (arch/x86/mm/init.c → init_mem_mapping)
  - `__pa(virt) = virt - PAGE_OFFSET` — 虚拟地址反查物理地址
  - kmalloc 从直接映射区分配(无额外页表) vs vmalloc 从 vmalloc 区逐页建页表(慢)
  - 直接映射大小: MAXMEM=64TB(5级页表)/64TB(4级页表), 内核态可直接访问所有物理内存 (Documentation/x86/x86_64/mm.rst)

### 6. 收束
  - `struct page` 每页 64 字节=所有物理页的最小描述符, flags/_refcount/_mapcount/mapping/index/lru 六字段编码页的全部状态
  - folio(5.16+) 是 compound page 的统一抽象, `folio_order` 统一多页操作, 解决 subpage 独立锁开销
  - 物理内存模型 SPARSEMEM→Node→Zone→Page 四级层次, 直接映射 `__va/__pa` 线性偏移 64TB 空间

---

### 核心悬念
**"Buddy 分配器怎么从这些 struct page 中组装出 4KB→4MB 的连续物理页？它的分裂合并算法是什么？"**

→ 引出 02 Buddy 分配器 — free_area[MAX_ORDER] + 分裂合并全路径
