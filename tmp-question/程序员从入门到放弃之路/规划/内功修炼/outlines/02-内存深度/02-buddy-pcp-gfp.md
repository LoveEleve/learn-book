# Buddy Allocator — 伙伴系统的分裂合并与 GFP 控制

> Cluster A: 9 KPs | 依赖: 01-struct-page-folio | 读者基线: struct page 布局 + Zone 概念

---

### 1. 核心数据结构 — free_area[MAX_ORDER] + 迁移类型
  - `struct zone { struct free_area free_area[MAX_ORDER]; }` — MAX_ORDER=11, order 0=4KB...order 10=4MB (include/linux/mmzone.h)
  - `struct free_area { struct list_head free_list[MIGRATE_TYPES]; unsigned long nr_free; }` — 按迁移类型分类空闲链表
  - 伙伴条件: 两个 page 的 PFN 连续 + order 相同 + 都在 free_area 中 + `PageBuddy(page)` 标记
  - `page→buddy_list` 链入 free_list, `page→private` 存储 order, `__find_buddy_pfn(page_pfn, order)` 计算伙伴 PFN (mm/page_alloc.c → __find_buddy_pfn)

### 2. 分配全路径 — alloc_pages → rmqueue 层层深入
  - `alloc_pages(gfp_mask, order) → alloc_pages_node → __alloc_pages → __alloc_pages_nodemask` (mm/page_alloc.c → __alloc_pages_nodemask)
  - prepare alloc_context: 从 gfp_mask 提取 zone + watermark 要求 → `node_zonelist` 定义备用 zone 优先级
  - **Fast Path**: `get_page_from_freelist → rmqueue → __rmqueue → __rmqueue_smallest`(当前 order 有空闲) (mm/page_alloc.c:3015)
  - **Medium Path**: `rmqueue_pcplist`(Per-CPU 热页列表优先, 减少 zone→lock 竞争) (mm/page_alloc.c:2950)
  - **Slow Path**: `__alloc_pages_slowpath` → 唤醒 kswapd → 直接回收 → compact → 重试 → OOM Killer → cpuset fallback (mm/page_alloc.c:4982)

### 3. PCP (Per-CPU Pageset) — 无锁热页缓存
  - `struct per_cpu_pages { int count; int high; int batch; struct list_head lists[MIGRATE_PCPTYPES]; }` (include/linux/mmzone.h)
  - 分配: `rmqueue_pcplist` 优先从 PCP 取(无 zone lock) → PCP 空 → `rmqueue_bulk` 从 zone bulk refill(batch 个页)
  - 释放: `free_unref_page` → `free_unref_page_commit` 回灌 PCP → count 达 high → `free_pcppages_bulk` 回灌 zone
  - `high = max(batch*6, batch*4)` — 防止 PCP 囤积过多, cold/hot pages 分列表减少 false sharing (mm/page_alloc.c → free_pcppages_bulk)

### 4. 单页释放与分裂合并 — __free_one_page
  - `__free_pages → __free_pages_ok → free_one_page → __free_one_page` (mm/page_alloc.c)
  - `page_is_buddy`: 同 zone + 同 order + PG_buddy 标志 + PFN 连续 → 找到伙伴
  - 合并: `__del_page_from_free_list` 从链表删除 → `page_idx & (1<<order)` 确定伙伴 PFN → page(order+1) 合并 → 循环
  - 终结: 伙伴忙或达 MAX_ORDER → `add_to_free_list` 加入对应 order + 迁移类型链表 → `SetPageBuddy(page)` (mm/page_alloc.c:1075)

### 5. 迁移类型与 Fallback — 反碎片化策略
  - MIGRATE_UNMOVABLE(0): 内核核心(GFP_KERNEL 分配 slab/内核栈/页表) → MIGRATE_MOVABLE(2): 用户页(GFP_HIGHUSER_MOVABLE) → MIGRATE_RECLAIMABLE(1): 可回收 slab
  - `fallbacks[MIGRATE_TYPES][4]` 矩阵: MOVABLE→UNMOVABLE→RECLAIMABLE→CMA (mm/page_alloc.c:1965)
  - `__rmqueue_fallback`: 当前迁移类型 order 链表空 → 按矩阵从其他类型偷页 → `steal_suitable_fallback` 最多偷 1/2
  - `movablecore=nn[KMG]` 内核参数预留 MOVABLE 区 → 减少 UNMOVABLE 页导致的碎片化 (mm/page_alloc.c → movablecore)

### 6. GFP Flags — 分配行为的四类位域控制字
  - **Zone**: `__GFP_DMA`(bit0)/`__GFP_DMA32`(bit1)/`__GFP_HIGHMEM`(bit2) (include/linux/gfp.h)
  - **Reclaim**: `__GFP_RECLAIM`/`__GFP_DIRECT_RECLAIM`(bit18,阻塞)/`__GFP_KSWAPD_RECLAIM`(bit20,不阻塞)
  - **Watermark**: `__GFP_HIGH`(bit3,MIN 水位)/`__GFP_ATOMIC`(bit4,不睡眠)/`__GFP_MEMALLOC`(bit12,全部内存)
  - **常用宏**: `GFP_KERNEL`=`RECLAIM|IO|FS`, `GFP_ATOMIC`=`HIGH|ATOMIC|NOWARN`, `GFP_USER`, `GFP_NOWAIT`=`ATOMIC&~HIGH` (include/linux/gfp.h:315)

### 7. Watermark 三级水位 — 内存压力的分水岭
  - `zone→watermark[WMARK_MIN]`(紧急预留, `min_free_kbytes` 默认 ~4MB) → `WMARK_LOW`(kswapd 唤醒) → `WMARK_HIGH`(kswapd 停止)
  - `__GFP_HIGH` 允许访问 MIN → 分配检查: `zone_watermark_fast → __zone_watermark_ok` → free_pages ≥ mark
  - `zone→lowmem_reserve[classzone_idx]` 保护低 zone 不被高 zone 耗尽, MIN 计算: `min=sqrt(zone_managed_pages*16)` (mm/page_alloc.c)
  - 错误示例: 中断上下文用 GFP_KERNEL → 睡眠 → kernel panic; `GFP_ATOMIC` 必须传 __GFP_HIGH 访问紧急预留

### 8. vmalloc — 虚拟连续物理不连续的内存区
  - `vmalloc(size) → __vmalloc_node_range` 从 vmalloc 区分配虚拟地址空间 — 与直接映射区(kmalloc)不同, vmalloc 是独立的虚拟地址区 (mm/vmalloc.c)
  - `alloc_vmap_area → __alloc_vmap_area` 在 vmap_area_root 红黑树中 first-fit 查找空闲虚拟区间 → 找到后 `vmap_pages_range` 逐页通过 `alloc_pages` 分配物理页并建立页表映射 (mm/vmalloc.c)
  - `struct vmap_area { unsigned long va_start, va_end; unsigned long flags; struct rb_node rb_node; struct list_head list; }` — 红黑树 + 链表双索引管理 (include/linux/vmalloc.h)
  - kmalloc vs vmalloc 区别: kmalloc 返回直接映射区地址(连续物理页, 快), vmalloc 返回 vmalloc 区地址(物理页可能不连续, 逐页建页表, 慢) (mm/vmalloc.c)

### 9. 收束
  - Buddy 通过 `__free_one_page` 位运算找伙伴、分裂合并返回 `2^order` 连续物理页, PCP 无锁热页缓存减少 zone→lock 竞争
  - GFP 四类位域 (Zone/Reclaim/Watermark/Action) 组合出 GFP_KERNEL/GFP_ATOMIC/GFP_USER 等不同分配行为
  - Watermark MIN/LOW/HIGH 三级水位控制分配准入 + kswapd 唤醒/睡眠, 迁移类型 fallback 矩阵防碎片化

---

### 核心悬念
**"虚拟地址到物理地址需要 4 级页表遍历 — PGD/PUD/PMD/PTE 各 9 位偏移怎么组织？缺页时硬件怎么触发软件处理？"**

→ 引出 03 页表与 TLB — 四级页表结构 + PTE 64 位标志位 + TLB shootdown
