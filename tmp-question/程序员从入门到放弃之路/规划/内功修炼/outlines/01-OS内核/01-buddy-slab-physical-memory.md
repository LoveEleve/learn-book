# 物理内存管理 — Buddy 分配器 + SLAB/SLUB + memblock + NUMA 节点

> Cluster A: 15 KPs | 依赖: 无 | 读者基线: C 语言基础 + 了解内存地址概念

---

### 1. Buddy 分配器 — 2^n 页分裂与合并
  - `struct free_area { struct list_head free_list[MIGRATE_TYPES]; unsigned long nr_free; }` — 11 个 order 链表，每 order 对应 2^n 页块 (mm/page_alloc.c:2932)
  - 分配流程: alloc_pages → __alloc_pages_nodemask → get_page_from_freelist → __rmqueue — 合适 order 无空闲则分裂大块返回小块 (mm/page_alloc.c:2350)
  - 合并流程: __free_pages → __free_one_page — 检查 buddy 是否空闲，是则合并升级到更高 order，递归直至 buddy 不空闲 (mm/page_alloc.c:1078)
  - MIGRATE_TYPES 三类: MIGRATE_UNMOVABLE(内核核心数据) / MIGRATE_RECLAIMABLE(可回收，如 slab 页) / MIGRATE_MOVABLE(用户页) — 减少不可移动页导致的碎片化 (mm/page_alloc.c:221)

### 2. SLAB/SLUB 分配器 — 内核对象缓存
  - SLAB 核心: kmem_cache_create → kmem_cache_alloc — 每个 cache 有 slab 三链表(full/partial/free)，着色(coloring)减少缓存行冲突 (mm/slab.c:1976)
  - SLUB 改进: 取消着色和 per-node 队列 → per-CPU partial list + NUMA 感知 → Linux 2.6.23+ 默认 → 更简化更少元数据 (mm/slub.c:385)
  - 关键参数: `/proc/slabinfo` 查看所有缓存 → `slabtop` 实时排序 → `kmemleak` 检测未释放 → `echo scan > /sys/kernel/debug/kmemleak`
  - `struct page` 关键字段: flags(flag 位) / _refcount(引用计数) / _mapcount(页表映射数) / mapping(address_space 或 anon_vma) / index(页内偏移) / lru(LRU 链表节点) — 每页 64 字节 (include/linux/mm_types.h:68)

### 3. 物理内存模型 — UMA → NUMA 三级结构
  - node → zone → page 三级: NUMA node(本地内存快/远程内存慢) → zone(同一 node 内按用途分区) → page(最小分配单位) (include/linux/mmzone.h:335)
  - Zone 分类: ZONE_DMA(低 16MB, ISA 设备遗留) / ZONE_DMA32(4GB 以下) / ZONE_NORMAL(直接映射区) / ZONE_HIGHMEM(32 位内核 >896MB, 64 位无此 Zone) (mm/page_alloc.c:68)
  - NUMA 距离矩阵: `/sys/devices/system/node/node*/distance` → `numactl --hardware` → `numastat -p PID` 查看进程 NUMA 命中率

### 4. memblock — 启动早期分配器
  - memblock_reserve 保留 → memblock_alloc 分配 → 伙伴系统初始化完成后 memblock 退役 → `memblock=debug` 内核参数可查 (mm/memblock.c:1240)
  - `/sys/kernel/debug/memblock/reserved` 查看启动时预留的内存区域 → `memblock_free` 释放不再需要的预留

### 5. 收束
  - Buddy 解决外部碎片(SLUB 解决内部碎片)，memblock 解决启动阶段无伙伴系统的问题
  - NUMA 三级结构决定"分配优先本地内存"的高性能原则
  - 物理内存管理的四大机制构成了用户态看到连续虚拟内存的物理基础

---

### 核心悬念
**"物理页有了，进程怎么看到一个连续的 4GB 虚拟地址空间？"**

→ 引出 02-分页机制 + 四级/五级页表 + PTE 标志位 + TLB
