# struct page — 内核物理页的 64 字节身份证

> Cluster A: 8 KPs | 依赖: 无 | 读者基线: C语言+基本虚拟内存概念
> 读者处境: 阶段 1 已建立全景（01-19 篇机制）；本篇进入源码深挖第一站——"每个物理页在内存里是什么？"
> 打开新视角: 64 字节的硬约束、六字段的状态编码、folio 的革命、物理内存的四级组织、直接映射的零页表访问

---

### 概念依赖链

```
阶段1-01(Buddy 概念) → 本篇: 物理页的描述符
  ├─ §1 struct page 布局(六字段 — 本篇基石)
  │    ├─ §2 PG_* 标志位(状态按位编码 — 依赖 §1 flags)
  │    │    └─ §3 folio(多页抽象 — 依赖 §1/§2)
  │    ├─ §4 物理内存模型(Section/PFN/Node/Zone — 依赖 §1 的定位)
  │    └─ §5 Direct Mapping(内核直接访问物理内存 — 依赖 §4)
先讲: 描述符(64B) → 状态(标志位) → 抽象(folio) → 组织(四层) → 访问(直接映射)
后续依赖: 02-buddy(从 page 组装连续块)、03-页表
```

### 叙事顺序

1. 问题引入——内核管理每个物理页——但"每个页"是什么？用什么数据结构表示？（**Aha: 每个物理页都有一个 64 字节的'身份证'——struct page，全内存的页都是它**）
   - 过渡: 为什么偏偏是 64 字节？——缓存行
2. struct page 布局——六字段各管什么；64B = cache line
   - 过渡: 页的状态怎么表达？——按位标志
3. PG_* 标志位——locked/dirty/writeback 等位编码；folio_test/set/clear 宏
   - 过渡: 多页一起操作怎么办？——folio
4. folio——嵌入首个 page 的超集；folio_order；锁的开销革命
   - 过渡: 这么多 struct page 存在哪？——物理内存模型
5. 物理内存模型——SPARSEMEM Section/PFN/Node/Zone 四级
   - 过渡: 内核怎么访问这些物理内存？——直接映射
6. Direct Mapping——__va/__pa 线性偏移；kmalloc vs vmalloc
   - 过渡: 完整图景已齐——收束
7. 收束——从身份证到四级组织的完整链条

### 1. struct page — 物理页描述符的 64 字节布局

场景提示: 一个 4GB 内存的机器有 100 万物理页——内核怎么"记住"每一页的状态？ [写作时展开]

关键设计: 每个物理页一个 `struct page`——**64 字节**（= x86 cache line 宽度）：

```[pseudocode]
设计考量: 64B = CPU 缓存行宽度 → 一个 cache line 精确装一个 struct page
  → 避免跨行访问 → 减少 cache miss → 这就是为什么不能缩到 48 字节
```

六核心字段 (include/linux/mm_types.h)：

| 字段 | 含义 |
|------|------|
| flags | 页状态位（PG_locked/PG_dirty/...按位编码） |
| _refcount | 引用计数（0 = 可回收） |
| _mapcount | 页表映射数（-1 无映射 / 0 单映射） |
| mapping | 文件页→address_space / 匿名页→anon_vma |
| index | 在 mapping 中的偏移（文件偏移 >> PAGE_SHIFT） |
| lru | LRU 链表节点（批量操作 pagevec） |

存放: vmemmap 数组——1GB 物理内存需 16MB struct page（1GB/4KB × 64B）；`pfn_to_page(pfn)`/`page_to_pfn(page)` 互转。

Why: 为什么 struct page 要"一页一个"且固定 64 字节？——**内存管理的一切判断都要查它**（这页脏吗/被映射了吗/在用吗）；64B 保证每个页描述符独占一个缓存行——遍历时（回收/扫描）不跨行取数。**64 字节是"信息量 × 缓存效率"的平衡点**：信息太少管不住页，太大内存开销爆炸（1GB 内存就要 16MB 描述符）。

比喻锚点: struct page=每间客房的门牌卡——酒店（内存）每间房（物理页）一张卡（64B），卡上写"住客（引用）、是否打扫（脏）、是否退房（可回收）"；卡片做成统一尺寸（64B）方便前台（内核）快速翻查。 [写作时展开]

### 2. PG_* 标志位 — 页状态的按位编码

场景提示: 一页有十几种状态（锁着/脏/写回中/正在回收）——用 10 个字段？ [写作时展开]

关键设计: 状态**按位编码**在 `flags` 字段（1 bit = 1 状态）：

| 标志 | 位 | 含义 |
|------|----|------|
| PG_locked | bit0 | 页被锁（IO 保护） |
| PG_error | bit1 | 错误 |
| PG_uptodate | bit2 | 内容有效 |
| PG_referenced | bit3 | 被访问过（LRU 老化） |
| PG_dirty | bit4 | 脏（未写回） |
| PG_writeback | bit6 | 写回进行中 |
| PG_reclaim | bit17 | 正在回收 |
| PG_swapbacked | bit18 | 匿名页（swap 后备） |
| PG_unevictable | bit19 | 不可回收（mlock） |
| PG_mlocked | bit20 | 被 mlock 锁定 |

操作: `folio_test_*`/`folio_set_*`/`folio_clear_*` 宏家族——原子与非原子版本混用（非原子读存在竞态）。

Why: 为什么用"按位"而非"多个布尔字段"？——**一个原子操作同时判断/修改多状态**：如"检查 dirty 且开始写回"一次位运算完成；多个布尔字段要么拆多次访问（竞态），要么打包成整数（又要位运算）。位标志 + 原子位操作 = **零锁的状态机**（页状态变化本身就是位翻转）。

比喻锚点: PG_* 标志=客房服务状态牌——每间房门口一块翻牌板（flags），各格子（位）翻上翻下表示"请勿打扰（locked）/需打扫（dirty）/已消毒（uptodate）"；服务员（内核）一次看整块板（位运算）就知道全部状态。 [写作时展开]

### 3. struct folio — 5.16+ 的多页抽象革命

场景提示: 页缓存里一个文件页可能是"1 个 page"也可能是"8 个连续 page"——代码怎么统一处理？ [写作时展开]

关键设计: `struct folio { struct page page; }`——**嵌入首个 page 的超集**：

```[pseudocode]
核心字段: flags(folio 级) + _folio_order(阶数, 0=单页) + _refcount
API: folio_order(folio) / folio_nr_pages(folio)(1<<order) / folio_page(folio, n)
锁:  folio_lock/folio_end_read/folio_end_writeback — folio 级锁替代 page 级
转换: page_folio(page) → compound_head(page) → folio
```

- **解决 compound page 碎片化**: 以前"多页操作"靠 compound_head 反推（每页都要算）；folio 把"这是一个多页单元"作为**显式类型**
- **锁开销革命**: 一个 folio 一把锁 vs N 个 page N 把锁——多页批量 IO 锁开销从 O(N) 降 O(1)

Why: 为什么 folio 是"革命"而非"改名"？——以前代码拿到 page 不知道它是否属于一个大单元（要 compound_head 探测）；folio 让"多页单元"成为**第一类类型**——API 直接表达意图（folio_nr_pages 明确"这是几页"），编译器检查类型（page 与 folio 不可混用），锁粒度显式（folio 级）。**类型即文档**——C 语言里少见的"类型安全"改进。

比喻锚点: folio=整层公寓的业主证 vs 单间房卡——以前（page）每间房一张卡，查"这层楼几间房"要挨个数（compound_head 探测）；现在（folio）整层一张证（folio_nr_pages 直接说"这层 8 间"）——锁也按层一把（folio_lock），不用每间房各锁一次。 [写作时展开]

### 4. 物理内存模型 — Sections + PFN + Node + Zone

场景提示: 100 万个 struct page 存在哪？怎么按"物理地址"找到对应的那个？ [写作时展开]

关键设计: 四级组织（SPARSEMEM）：

```[pseudocode]
PFN(页帧号) = 物理地址 >> PAGE_SHIFT — 物理页的"门牌号"
Section: SECTION_SIZE_BITS=30(1 section = 1GB) — mem_section[] 稀疏存储
Node:    pg_data_t *node_data[MAX_NUMNODES] — NUMA 节点
Zone:    ZONE_DMA/DMA32/NORMAL/HIGHMEM/MOVABLE — 用途分区
struct zone: watermark[NR_WMARK] + free_area[MAX_ORDER] + pageset + lock
```

- **PFN ↔ page**: `pfn_to_page(pfn)` / `page_to_pfn(page)`——门牌号与身份证互查
- **SPARSEMEM**: 物理内存可能不连续（热插拔/内存空洞）——section 稀疏存储避免"为空洞也建描述符"

Why: 为什么要"四层"而非"一张大表"？——**三组不同的划分逻辑**：Section 解决"物理内存不连续"（稀疏）、Node 解决"多 CPU 访问延迟"（NUMA）、Zone 解决"用途限制"（DMA/可移动）——一张线性表无法同时表达三种维度；分层后每层管一种组织需求，`pfn→section→node→zone→page` 一次定位。

比喻锚点: 物理内存模型=图书馆找书——先按馆区（Section，可能隔街不相连）、再按楼层（Node）、再按书架（Zone）、最后按书位（page）——四步定位一本书（物理页），任何一步都独立于其他（四种划分互不干扰）。 [写作时展开]

### 5. Direct Mapping — 物理内存到内核虚拟地址的直接映射

场景提示: 内核代码访问物理内存（读写页缓存）——每次都要查页表？ [写作时展开]

关键设计: **直接映射**——物理地址线性偏移成内核虚拟地址：

```[pseudocode]
__va(phys) = phys + PAGE_OFFSET — 物理→虚拟(线性偏移, 零页表)
__pa(virt) = virt - PAGE_OFFSET — 虚拟→物理(反查)
PAGE_OFFSET = 0xffff888000000000 — 直接映射区起点
MAXMEM = 64TB — 内核可直接访问全部物理内存
```

对比: **kmalloc** 从直接映射区分配（无额外页表，快）/ **vmalloc** 从 vmalloc 区逐页建页表（虚拟连续但物理不连续，慢）。

Why: 为什么直接映射能"零页表访问"？——直接映射区在**启动时一次性建好页表**（线性映射整段物理内存，页表固定不变）；之后内核访问物理内存 = 地址偏移计算（`__va` 一行）——**页表开销从"每次访问"摊平为"启动一次"**。vmalloc 慢正是因为要动态建页表（每页一个 PTE）。这就是为什么内核热路径（页缓存/网络）都用 kmalloc 而非 vmalloc。 [内核: 直接映射区与 02 篇分页机制衔接——启动时建好, 运行零开销]

比喻锚点: Direct Mapping=酒店员工专用通道——开业（启动）时一次性规划好（建页表），之后员工（内核）去任何客房（物理内存）直接走固定通道（__va 偏移），不用每次问路（查页表）；vmalloc 是临时搭桥（动态建页表），搭一次用一次。 [写作时展开]

### 6. 收束

回到"内核怎么管理每个物理页"：
- 身份证 = struct page（64B 六字段）
- 状态 = PG_* 位标志（按位编码 + 原子位操作）
- 抽象 = folio（多页单元第一类类型 + O(1) 锁）
- 组织 = Section/PFN/Node/Zone 四级
- 访问 = Direct Mapping（__va 零页表）

**Aha Moment**: "物理内存不是'一块大数组'，是 100 万个 64 字节身份证 + 一套位编码状态机 + 四级索引。而 folio 是 30 年来少见的'类型级'改进——它把'多页'从运行时探测变成编译期类型。"
**回答读者三问**: ①内存每页怎么被记住=struct page 64B；②页状态怎么表示=PG_* 位标志；③内核访问物理内存为何快=直接映射 __va 零页表。

---

### 核心悬念

**"Buddy 分配器怎么从这些 struct page 中组装出 4KB→4MB 的连续物理页？它的分裂合并算法是什么？"**

→ 引出 02 Buddy 分配器 — free_area[MAX_ORDER] + 分裂合并全路径——身份证有了，下一步：怎么按需组装连续块。
