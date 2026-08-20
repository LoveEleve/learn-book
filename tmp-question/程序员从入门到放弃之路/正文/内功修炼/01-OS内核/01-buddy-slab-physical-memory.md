# 物理内存管理 — Buddy 分配器 + SLAB/SLUB + memblock + NUMA 节点

> Cluster A: 15 KPs | 依赖: 无 | 读者基线: C 语言基础 + 了解内存地址概念
> 🔴 含 struct/call chain | 写作深度 8.5-9/10

---

## 0. 开场：内存管理要回答的四个问题

任何操作系统内核都要回答四个问题，Linux 的答案构成了本章的骨架：

| 问题 | 内核的答案 | 对应章节 |
|------|-----------|---------|
| 物理页以什么粒度分配？ | 4KB 页，2^n 页块（Buddy） | §1 |
| 大量同尺寸小对象怎么避免内部碎片？ | SLAB/SLUB 对象缓存 | §2 |
| 多 CPU 下内存如何组织？ | NUMA node → zone → page 三级 | §3 |
| 内核启动早期用什么分配？ | memblock | §4 |

用户态程序员看到的是"连续的虚拟地址空间"，但物理内存是碎片化的、分层的。本章先建立物理侧的全貌——Buddy 管页块、SLUB 管对象、NUMA 管节点、memblock 管启动。

---

## 1. Buddy 分配器 — 2^n 页分裂与合并

### 1.1 为什么用 Buddy：碎片化问题的起源

场景: 内核要满足任意尺寸的物理内存请求——驱动要 3 页，文件系统要 64 页，网络栈要 1 页。如果按请求尺寸切分内存，反复分配释放后内存会碎成无法满足大请求的细条——**外部碎片**。

关键设计: Buddy 的核心思想是把内存按 **2^n 页**（order n）组织成块，只允许分配 2^n 页大小的块：

```[pseudocode]
order 0:  4KB   × 1024   ← 最小块
order 1:  8KB   × 512
order 2:  16KB  × 256
...
order 10: 4MB   × 1      ← 最大块（单区）
```

一个 order n 的块被分配后若只需更小块，就**分裂**成两个 order n-1 的块（buddy 对）；释放时若它的 buddy 也是空闲的，就**合并**回 order n+1。这种"分裂-合并"机制保证：

- 分配粒度永远是 2 的幂 → 不产生任意碎块
- 合并只发生在 buddy 对上 → 每个块只有一个可能的合并对象，查找 O(1)
- 相邻块分裂的逆操作是合并 → 大块总能被重新聚拢

### 1.2 数据结构 — 11 个 order × 3 种迁移类型的链表

核心结构是 `struct free_area`，每个 zone 一个，内部按 order 组织：

```c
// include/linux/mmzone.h — 每个 zone 内的空闲页块管理
struct free_area {
    struct list_head    free_list[MIGRATE_TYPES];  // 每个迁移类型一条链表
    unsigned long       nr_free;                    // 该 order 空闲块总数
};
```

一个 zone 里有 11 个 `free_area`（order 0 到 order 10），每个 free_area 里有 3 条链表（按 MIGRATE_TYPES 分类）。**"free_area[order].free_list[migratetype]"定位一个链表**——这是整个 Buddy 查找的核心索引方式。

`struct page` 是物理页的元数据描述符，每个物理页一个，关键字段：

```c
// include/linux/mm_types.h
struct page {
    unsigned long       flags;        // 页状态标志位（PG_locked/PG_dirty/PG_active...）
    atomic_t            _refcount;    // 引用计数（0 表示可回收）
    atomic_t            _mapcount;    // 页表映射数（-1 表示未映射）
    struct address_space *mapping;    // 页所属 address_space（文件页）或 anon_vma（匿名页）
    pgoff_t             index;        // 页内偏移
    struct list_head    lru;          // LRU 链表节点（活跃/不活跃链表）
    union {
        unsigned long   private;      // 私有数据（如缓冲头）
        ...
    };
};
```

每页 64 字节（struct page 数组即 `vmemmap`，存在于每个内存节点）。`_refcount` 与 `_mapcount` 的区别是理解"页什么时候能释放"的关键：**引用计数归零且映射计数归零**的页才能回到 Buddy。

### 1.3 分配流程 — 从 alloc_pages 到 __rmqueue

分配一条路径（fast path）：

```[pseudocode]
alloc_pages(gfp_mask, order)
  └─ __alloc_pages_nodemask(gfp_mask, order, preferred_nid, nodemask)
       ├─ get_page_from_freelist(...)          ← 从 zone 列表找可用 zone
       │    └─ rmqueue(zone, order)            ← 真正的链表操作
       │         └─ __rmqueue(zone, order)     ← fast path: 目标 order 有块吗?
       │              ├─ __rmqueue_smallest    ← 有: 取块, 若 order 不匹配则分裂
       │              └─ __rmqueue_fallback    ← 无: 跨迁移类型借用
       └─ __alloc_pages_slowpath(...)          ← slow path: 回收/压缩/OOM
```

`__rmqueue_smallest` 是分裂的核心（`mm/page_alloc.c`）：

```[pseudocode]
当前 order 有空闲块?
  → 取下链表头块, nr_free--
  → 若块 order > 请求 order:
      把块对半分裂, 低半块返回, 高半块挂回 (order-1) 链表
      递归直至达到请求 order
  → 返回低地址那一半
```

每个分裂操作把"块"切成两个 **buddy**。Buddy 对的判定规则是：**同 order 且物理地址只差 2^order 页**。判定用位运算：

```c
// 判断 page 的 buddy: 物理页号翻转 order 对应的那一位
struct page *__find_buddy_pfn(unsigned long page_pfn, unsigned int order)
{
    return page_pfn ^ (1 << order);   // 异或翻转第 order 位
}
```

**这个 `^ (1 << order)` 就是 Buddy 分配器的灵魂**——一个异或操作 O(1) 定位唯一可能的合并伙伴。

### 1.4 释放与合并流程

```[pseudocode]
__free_pages(page, order)
  └─ __free_one_page(page, zone, order, migratetype)
       ├─ 检查 buddy: __find_buddy_pfn(pfn, order) → 该页是否空闲且在 free_list
       │    ├─ 空闲: 从链表摘下, 向上合并 order+1, 递归继续
       │    └─ 不空闲: 挂到 free_area[order].free_list[migratetype]
       └─ zone->free_area[order].nr_free++
```

合并的条件（为什么必须检查 buddy 也空闲）：
1. **两页属于同一 order**（分裂时保证）
2. **buddy 页当前空闲**（在对应 free_list 上）
3. **迁移类型一致**（或可合并的类型）

合并保证了长期运行的系统中大块不会消失——这正是 Buddy 能抵抗外部碎片的原因。

### 1.5 迁移类型 — 解决"不可移动页造成永久碎片"

场景: 内核分配了一些不可移动的内存（如内核栈、页表），它们恰好在内存中间。长期运行后，可移动页全被挪走，但不可移动页像钉子一样卡住，导致无法获得大块连续内存。

关键设计: 每个 free_area 按 **MIGRATE_TYPES** 分成 3 类链表：

| 类型 | 内容 | 可移动性 |
|------|------|---------|
| MIGRATE_UNMOVABLE | 内核核心数据（内核栈、页表） | 不可移动 |
| MIGRATE_RECLAIMABLE | 可回收（如 SLAB 页） | 可回收 |
| MIGRATE_MOVABLE | 用户页 | 可移动（页面迁移/内存规整） |

分配请求携带迁移类型（GFP 标志映射），从对应链表取块。当链表耗尽时 `__rmqueue_fallback` 才允许跨类型借用——**借用是不得已的最后手段**，因为跨类型借用的块会被永久留在原链表上（Buddy 不把块移回原位），破坏迁移类型的纯度。

内存规整（compaction）依赖 MOVABLE：`compact_zone` 把可移动页集中到一端，把不可移动页留到另一端，重新拼出大块连续内存——这支撑了透明大页 THP 的分配和防碎片。

### 1.6 per-CPU page list (PCP) — 单页分配的热路径

场景: order 0 的分配（用户页最常见）如果每次都走 zone 全局锁，多核竞争会让分配变成瓶颈。

关键设计: 每个 CPU 有一个 `per_cpu_pages` 缓存（`struct per_cpu_pages`），批量从 zone 取页到本地，单页分配直接从本地取——**避免频繁访问全局链表和 zone->lock**：

```[pseudocode]
rmqueue
  ├─ order == 0 → pcp 分配: list 非空 → 直接取头部（无锁或短临界区）
  │                list 空   → rmqueue_pcplist 批量取 batch 个页到 pcp
  └─ order > 0  → 全局 __rmqueue（zone->lock 保护）
```

PCP 批量数（batch）随 zone 大小动态调整，释放也先入 pcp，攒够批量再还回 Buddy——**批量分摊锁开销**是内核所有缓存设计的共同模式（后面 SLUB 的 per-CPU partial 同源）。

---

## 2. SLAB/SLUB 分配器 — 内核对象缓存

### 2.1 为什么需要 SLAB：内部碎片的来源

场景: 内核要频繁创建/销毁同尺寸小对象——`struct task_struct`（约 2KB）、`struct inode`、`struct sk_buff`。如果用 Buddy 直接分配，每个对象独占一页，一页只放 1-2 个对象，其余全浪费——**内部碎片**。

关键设计: SLAB 的思路是**为每种对象建一个缓存（kmem_cache）**，缓存里预先切好固定大小的槽（object），分配就是拿一个槽，释放就是还一个槽——零分配开销、零内部碎片、还带有对象构造/析构回调。

### 2.2 经典 SLAB — 三链表模型

```c
struct kmem_cache {                    // include/linux/slab_def.h
    struct kmem_cache_cpu  cpu_slab;   // per-CPU 快速路径（SLUB 用）
    ...
    struct list_head       slabs_full;     // 全满的 slab 页
    struct list_head       slabs_partial;  // 部分使用（分配优先从这里取）
    struct list_head       slabs_free;     // 全空的 slab 页
    unsigned int           object_size;    // 对象大小
    unsigned int           size;           // 含对齐/元数据的实际槽大小
    unsigned int           gfporder;       // 每个 slab 页块大小（order）
    ...
};
```

一个 slab = 若干连续物理页（通常 1-2 页），内部切成等尺寸槽。三个链表管理 slab 的状态机：

```[pseudocode]
分配 kmem_cache_alloc:
  slabs_partial 非空 → 取头部 slab 的空闲槽
  slabs_partial 空   → 从 slabs_free 取一个 slab, 初始化后转为 partial
  slabs_free 也空    → 从 Buddy 分配新页组, 创建新 slab

释放 kmem_cache_free:
  槽归还 → slab 变满则移入 slabs_full → 全空则移入 slabs_free
  slabs_free 攒够 → 归还给 Buddy
```

SLAB 的着色（coloring）：对象在 slab 内的起始位置按缓存行错开，使不同 slab 中相同索引的对象不落在同一 cache line 上——减少多 CPU 竞争同一缓存行的概率。但着色也增加了复杂性，SLUB 直接砍掉了它。

### 2.3 SLUB — 为什么 2.6.23 起成为默认

场景: SLAB 的三链表 + 着色 + per-CPU 队列在大 NUMA 机器上开销过高：全局锁、复杂的 slab 状态迁移、CPU 间对象流动导致伪共享。

关键设计: SLUB（`mm/slub.c`）做了三件事：

1. **去掉三链表与着色**——每个 slab 头部一个 `freelist` 指针串起空闲槽，状态由 `inuse` 计数判断，不再需要链表状态迁移
2. **per-CPU partial 链表**——每个 CPU 维护一个 partial 列表，释放的 slab 先留在本地，避免频繁锁全局 partial 表
3. **NUMA 感知**——`kmem_cache_cpu` 取本地节点；本地 partial 耗尽才从节点 partial 表取；remote 分配会记录计数（`remote_node_defrag_ratio` 控制回流）

```[pseudocode]
kmem_cache_alloc(s, gfp):
  ├─ s->cpu_slab.freelist 非空 → 直接取头部对象（无锁热路径）
  ├─ 空 → 从 per-CPU partial 取一个 slab 补上 freelist
  │        → 本地节点 partial 表取 slab
  │        → 从 Buddy 分配新页组 → 初始化 freelist
  └─ 释放: 对象回 freelist → slab 全空且 partial 超限 → 归还 Buddy
```

SLUB 用"最少元数据 + 最少锁"换掉了 SLAB 的"最优缓存行为"，在吞吐量上全面胜出——这解释了为什么它是默认。

### 2.4 对象缓存 — 预构造与内存回收

每个 `kmem_cache` 可以带构造/析构函数（`kmem_cache_create(..., ctor)`）：首次分配对象时调用构造函数（如初始化互斥锁），之后复用不再重复构造——省去重复初始化成本。注意 SLUB 在 `CONFIG_SLUB_TINY` 等配置下不调用 ctor 的旧对象，只保证首次构造。

### 2.5 观测工具

| 工具 | 看什么 |
|------|--------|
| `/proc/slabinfo` | 所有 kmem_cache 的活跃对象数/空闲槽/每 slab 对象数 |
| `slabtop` | 实时排序——找出占内存最多的缓存（常见 top 是 dentry/inode/task_struct） |
| `/sys/kernel/debug/kmemleak` | 泄漏检测：`echo scan > /sys/kernel/debug/kmemleak` 后读报告 |
| `slub_debug=F` | 启动参数：开启 freelist 完整性校验/对象毒化（排查越界写） |

> 生产排障思路：`slabtop` 看到 `dentry` 缓存爆涨 → 通常是文件描述符泄漏或目录遍历缓存未释放 → 用 `echo 2 > /proc/sys/vm/drop_caches` 只是治标，要查持有路径。

---

## 3. 物理内存模型 — node → zone → page 三级结构

### 3.1 为什么分层：硬件与内核需求的分割

场景: 一台 32 路机器，每个 CPU 插槽有自己的内存——CPU 访问本地内存快、远程内存慢；同时老设备（ISA DMA）只能访问低 16MB；32 位内核内存地址空间有限。

关键设计: 内核把物理内存组织成三级：

```[pseudocode]
NUMA node（节点）  — 一组 CPU + 本地内存的物理单元
   └─ zone（区）   — 同一 node 内按用途/地址范围分区
        └─ page   — 最小分配单位（4KB）
```

### 3.2 Zone 分类 — 每类为什么存在

```c
// include/linux/mmzone.h
enum zone_type {
    ZONE_DMA,     // 0-16MB: 老 ISA 设备 DMA 只能访问低 16MB
    ZONE_DMA32,   // 0-4GB:  64 位设备 DMA 可访问区域
    ZONE_NORMAL,  // 直接映射区: 内核线性映射, 多数内核分配
    ZONE_HIGHMEM, // 32 位内核 >896MB 部分（64 位内核无此区）
    __MAX_NR_ZONES
};
```

| Zone | 范围 | 谁用 |
|------|------|------|
| ZONE_DMA | 低 16MB | ISA 设备 DMA 缓冲区（`GFP_DMA` 显式请求） |
| ZONE_DMA32 | 0-4GB | 64 位设备 DMA |
| ZONE_NORMAL | 直接映射区 | 内核栈、页表、多数内核对象 |
| ZONE_HIGHMEM | 32 位 >896MB | 64 位已废弃，永远为空 |

**ZONE_NORMAL 直接映射**：`PAGE_OFFSET + pfn * 4KB` 线性映射所有物理内存，内核用 `__va(page_to_virt(p))` 一步换算——这就是"内核大部分时间不需要查页表"的原因。`page_to_pfn`/`pfn_to_page` 是在 struct page 数组与物理页号间换算的常量级操作。

### 3.3 NUMA — 本地内存优先

```
/sys/devices/system/node/node0/    ← 每个 node 一个目录
    ├── distance                   ← 距离矩阵: node0 到 nodeN 的相对距离
    ├── meminfo
    └── node0/cpu0/...

命令:
  numactl --hardware    ← 查看拓扑与距离矩阵
  numactl -m 1 -N 1 cmd ← 绑定内存节点 1 + CPU 节点 1
  numastat -p PID       ← 查看进程各节点内存命中率
```

分配策略（`numa_node_id` → `alloc_pages_node`）：

```[pseudocode]
优先: 本地节点（preferred_nid）→ 本地内存最快
回退: 逐级远程（zonelist 按距离排序）→ 远程内存有额外延迟
```

zonelist 是每个 node 预生成的内存分配候选列表——按距离从近到远排好，`get_page_from_freelist` 逐个尝试。NUMA 陷阱在真实场景：`numastat` 看到大量 remote 分配 → 通常是 `memory cgroup` 迁移或线程迁移导致 → 可考虑 `numactl --interleave`（内存交错）均衡带宽。

---

## 4. memblock — 启动早期分配器

### 4.1 为什么需要：Buddy 还没就绪

场景: 内核启动早期（`start_kernel` 之前），内存管理子系统尚未初始化——没有 zone、没有 Buddy 链表、没有 SLUB。但内核已经要分配内存：加载驱动、建立页表、保留内核镜像。需要一个"最小可用"的分配器。

关键设计: memblock（`mm/memblock.c`）是一个极简的内存注册表：

```c
struct memblock {
    struct memblock_type memory;   // 所有可用内存区域（从 e820/DT 解析）
    struct memblock_type reserved; // 已预留区域（内核镜像/页表/驱动）
    ...
};
```

- `memblock_add()` 注册可用区域（BIOS e820 表或 DT 的 memory 节点）
- `memblock_reserve()` 标记保留（内核镜像、initrd、页表）
- `memblock_alloc()` 从未保留区域分配（自顶向下或自底向上，按对齐）

### 4.2 生命周期 — 退役与移交

```[pseudocode]
start_kernel
  ├─ memblock 初始化（解析 e820 → memory 数组）
  ├─ 各种早期分配（页表、root_task、VMA 前哨）
  ├─ setup_arch → paging_init
  └─ mm_init
       ├─ memblock_free_all()  ← 把 memblock 里所有未保留区域
       │                        一次性批量移交给 Buddy（free_unused_memmap 后）
       │                        此后 memblock 退役
       └─ Buddy/SLUB 正式上线
```

排查工具：`memblock=debug` 内核参数在 dmesg 打印 memblock 操作日志；`/sys/kernel/debug/memblock/reserved`（需 CONFIG_DEBUG_FS）查看启动时到底保留了哪些区域——**"内存去哪了"类问题第一站**（常见元凶：ACPI 表、ramdisk、iommu 保留）。

---

## 5. 收束：四机制的分工与一条完整内存路径

| 机制 | 粒度 | 解决什么 | 何时活跃 |
|------|------|---------|---------|
| memblock | 任意/区域 | 启动早期"裸分配" | 启动到 mm_init |
| Buddy | 2^n 页块 | 外部碎片 | 内核运行全程 |
| SLAB/SLUB | 固定对象槽 | 内部碎片 + 分配开销 | 内核运行全程 |
| NUMA | node/zone | 访问延迟差异 | 分配决策时刻 |

一条完整的物理页生命周期：

```[pseudocode]
启动早期:  e820 表 → memblock 注册 → 预留内核镜像/页表
建立 Buddy: mm_init → memblock_free_all → 未保留区域成为 free_area
运行时分配: kmalloc → SLUB 缓存 → 无槽 → Buddy 取 2^n 页 → 切成对象槽
           mmap → alloc_pages → rmqueue（order 0 走 PCP）→ 用户页（MOVABLE）
释放:      SLUB 对象还槽 → slab 全空 → 还 Buddy → buddy 合并 → 大块重生
```

---

### 核心悬念

**"Buddy 给你物理页，NUMA 告诉你从哪个节点拿。但用户进程看到的不是物理页——它看到一个连续的 4GB 虚拟地址空间。内核怎么把 '物理页 0x1000、0x4000...' 这些碎片化的物理地址，伪装成进程眼里连续且私有的虚拟地址？"**

→ 引出 02-分页机制 — 四级/五级页表 + PTE 标志位 + TLB。虚拟地址 → 物理地址的翻译机制，是理解 mmap、COW、缺页、以及一切内存优化的地基。
