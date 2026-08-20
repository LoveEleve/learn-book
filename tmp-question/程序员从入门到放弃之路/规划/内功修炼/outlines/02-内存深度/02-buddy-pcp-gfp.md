# Buddy Allocator — 伙伴系统的分裂合并与 GFP 控制

> Cluster A: 9 KPs | 依赖: 01-struct-page-folio | 读者基线: struct page 布局 + Zone 概念
> 读者处境: 已读 01 篇（struct page 身份证）；本篇回答"怎么从 100 万张身份证组装出任意大小的连续物理块？"
> 打开新视角: 分裂合并的位运算、PCP 无锁热路径、GFP 四类位域、水位线的三级闸门、vmalloc 与 kmalloc 的本质区别

---

### 概念依赖链

```
01-struct-page(物理页描述符) → 本篇: Buddy 分配器
  ├─ §1 free_area 数据结构(order×迁移类型 — 依赖 01)
  │    ├─ §2 分配全路径(fast/medium/slow — 依赖 §1)
  │    │    ├─ §3 PCP(无锁热页缓存 — 依赖 §2 的 medium 路径)
  │    │    └─ §5 迁移类型 fallback(反碎片 — 依赖 §1)
  │    ├─ §4 释放合并(__free_one_page — 依赖 §1)
  │    ├─ §6 GFP Flags(分配行为控制 — 依赖 §2)
  │    │    └─ §7 Watermark(准入闸门 — 依赖 §2 slow path)
  │    └─ §8 vmalloc(虚拟连续 — 对照 kmalloc)
先讲: 结构(链表) → 分配(三路径) → 加速(PCP) → 释放(合并) → 反碎片(迁移) → 控制(GFP) → 闸门(水位) → 对照(vmalloc)
后续依赖: 03-页表与 TLB
```

### 叙事顺序

1. 问题引入——驱动要 3 页、文件系统要 64 页——怎么从 100 万张"身份证"里快速找到连续块？（**Aha: 预先把空闲页按 2^n 分组——要 3 页就找 4 页块拆一半**）
   - 过渡: 分组存在哪？——free_area
2. free_area 结构——MAX_ORDER 11 级 × 迁移类型 3 类；伙伴判定
   - 过渡: 从哪条路径取？——三条路径
3. 分配全路径——fast（目标 order）/medium（PCP）/slow（回收/压缩/OOM）
   - 过渡: medium 的 PCP 是什么？
4. PCP——per-CPU 热页缓存；count/high/batch；无锁
   - 过渡: 释放怎么合并回去？
5. 释放合并——__free_one_page 位运算找伙伴；循环合并
   - 过渡: 不连续/不可移动页怎么办？——迁移类型
6. 迁移类型 fallback——三类型 + fallback 矩阵
   - 过渡: 分配的"行为"怎么控制？——GFP
7. GFP Flags——Zone/Reclaim/Watermark/Action 四类位域
   - 过渡: 水位线怎么卡"内存不足"？
8. Watermark——MIN/LOW/HIGH 三级闸门；错误示例
   - 过渡: 连续物理分配之外——虚拟连续呢？
9. vmalloc——虚拟连续物理不连续；vs kmalloc
   - 过渡: 完整图景已齐——收束
10. 收束——Buddy 从结构到控制的完整链条

### 1. 核心数据结构 — free_area[MAX_ORDER] + 迁移类型

场景提示: 内核要分配"2^order 页"——空闲页怎么组织才能快速找到？ [写作时展开]

关键设计: 空闲页**按 2^n 分组**（order 0=4KB ... order 10=4MB）：

```[pseudocode]
struct zone { struct free_area free_area[MAX_ORDER]; }  // MAX_ORDER=11
struct free_area { struct list_head free_list[MIGRATE_TYPES]; unsigned long nr_free; }
```

- **伙伴条件**: 两个 page 的 PFN 连续 + order 相同 + 都在 free_area + `PageBuddy(page)` 标记
- **链入**: `page→buddy_list` 链入 free_list / `page→private` 存 order / `__find_buddy_pfn(page_pfn, order)` 算伙伴 PFN

比喻锚点: free_area=百货仓库按箱规分架——货架（free_area[order]）按"每箱 1/2/4/8 件"分排，每排再按货品种类（迁移类型）分格；要 3 件就拿"4 件箱"拆成 2+2（分裂）。 [写作时展开]

Why: 为什么按 2^n 分组？——**任何大小的请求都能用"拆大块"满足**（要 3 页 = 4 页块拆 2+2 取其一）；**释放时"伙伴"唯一确定**（同 order 相邻块只有一个可能合并对象——`PFN ^ (1<<order)` 一步算出）。2^n 分组让"找伙伴"从搜索变成位运算——这是 Buddy 效率的灵魂。 [内核: 阶段1-01 篇 Buddy 概念→本篇源码深挖——free_area 结构是 01 篇的落地]

### 2. 分配全路径 — alloc_pages → rmqueue 层层深入

场景提示: `alloc_pages(GFP_KERNEL, 2)`——内核经历了哪几步才拿到 16KB？ [写作时展开]

关键设计: 三层路径（mm/page_alloc.c）：

```[pseudocode]
alloc_pages(gfp, order) → __alloc_pages_nodemask
├─ Fast Path:  get_page_from_freelist → rmqueue → __rmqueue_smallest(目标 order 有空闲)
├─ Medium Path: rmqueue_pcplist(Per-CPU 热页列表, 减少 zone lock 竞争)
└─ Slow Path:  __alloc_pages_slowpath
   → 唤醒 kswapd → 直接回收 → compact → 重试 → OOM Killer → cpuset fallback
```

- prepare alloc_context: 从 gfp_mask 提取 zone + watermark 要求 → `node_zonelist` 定义备用 zone 优先级

Why: 为什么分三层？——**按"代价递进"**：fast 路径 O(1) 拿现成块（99% 情况）；medium 从 PCP 拿（无锁）；slow 要回收/压缩（昂贵，万不得已）。**每层失败才降级**——正常系统只走 fast/medium，slow 出现意味着内存压力（这正是水位线存在的意义，§8）。

比喻锚点: 分配三路径=买票三层级——fast 是自动售票机（秒出票=现成块）；medium 是常客窗口（PCP 熟客无锁）；slow 是人工窗口（要处理退票/换座=回收/压缩）——大多数人不排人工窗口。 [写作时展开]

### 3. PCP (Per-CPU Pageset) — 无锁热页缓存

场景提示: 每核频繁分配单页——每次都锁 zone 的全局链表？ [写作时展开]

关键设计: per-CPU 页缓存（mmzone.h）：

```[pseudocode]
struct per_cpu_pages { int count; int high; int batch;
                       struct list_head lists[MIGRATE_PCPTYPES]; }
分配: rmqueue_pcplist 优先从 PCP 取(无 zone lock) → 空则 rmqueue_bulk 批量 refill
释放: free_unref_page → 回灌 PCP → count 达 high → free_pcppages_bulk 回灌 zone
high = max(batch*6, batch*4) — 防 PCP 囤积; cold/hot 分列表减少 false sharing
```

Why: 为什么 PCP 能"无锁"？——**每核只操作自己的列表**（per-CPU 数据，05 篇的 per-CPU 模式）；zone 全局链表只在"批量补充/回灌"时访问（一次锁取 batch 个）。**把"每次分配锁一次"变成"每 batch 次分配锁一次"**——锁竞争降 batch 倍。这是内核"批量分摊锁开销"的经典实现（与 04 篇 flusher 批量回写同思想）。

比喻锚点: PCP=每位收银员自己的零钱盒——顾客（分配）找眼前的收银员（本核）直接拿零钱（PCP 无锁），零钱盒空了才去保险柜（zone）批量取（rmqueue_bulk）；盒子太满再存回去（回灌）——不用每次找收银员都开一次保险柜。 [写作时展开]

### 4. 单页释放与分裂合并 — __free_one_page

场景提示: 释放一个页——怎么知道该和谁合并？ [写作时展开]

关键设计: 释放链 `__free_pages → free_one_page → __free_one_page`（mm/page_alloc.c）：

```[pseudocode]
page_is_buddy: 同 zone + 同 order + PG_buddy + PFN 连续 → 找到伙伴
合并: __del_page_from_free_list 删链表 → page_idx & (1<<order) 定伙伴 PFN
   → 合并成 order+1 → 循环
终结: 伙伴忙或达 MAX_ORDER → add_to_free_list(对应 order+迁移类型) → SetPageBuddy
```

Why: 为什么合并是"递归"的？——释放 2 页 → 与伙伴合并成 4 页块 → 4 页块的伙伴可能也空闲 → 再合并成 8 页 → ……直到伙伴忙或达 MAX_ORDER。**合并让碎片持续减少**：系统长期运行的"大块再生"全靠这个递归——与分配时的"递归分裂"互为逆操作（§2 拆、§4 合）。

比喻锚点: 释放合并=积木回箱——拆开的积木（分裂的小块）放回时，先看旁边的积木（伙伴）是不是同规格且空着，是就拼成大一格（合并），再和更大格的拼……直到旁边有主（伙伴忙）或到箱子顶（MAX_ORDER）。 [写作时展开]

### 5. 迁移类型与 Fallback — 反碎片化策略

场景提示: 内核页表/内核栈是不可移动的——它们卡在内存中间，大块连续内存就没了。 [写作时展开]

关键设计: 空闲页按**可移动性**分 3 类（mm/page_alloc.c）：

```[pseudocode]
MIGRATE_UNMOVABLE(0): 内核核心(GFP_KERNEL slab/内核栈/页表)
MIGRATE_RECLAIMABLE(1): 可回收 slab
MIGRATE_MOVABLE(2): 用户页(GFP_HIGHUSER_MOVABLE)
fallbacks[MIGRATE_TYPES][4]: MOVABLE→UNMOVABLE→RECLAIMABLE→CMA 偷页矩阵
__rmqueue_fallback: 当前类型链表空 → 按矩阵偷页(最多 1/2)
movablecore=nn[KMG]: 预留 MOVABLE 区 → 减少 UNMOVABLE 造成的碎片
```

Why: 为什么按"可移动性"分链表？——**不可移动页是碎片的钉子**：内核栈/页表不能搬走，卡在哪就在哪制造"永久空洞"。把可移动页（用户页）与不可移动页分链管理 → 回收时只搬 MOVABLE（compaction）→ 拼出连续块。**分类让"碎片可治理"**：没有分类，compaction 无从下手（不知道哪些页能搬）。

比喻锚点: 迁移类型=仓库分区——"不可动区"（UNMOVABLE）放承重柱（内核页表）、"可搬区"（MOVABLE）放普通货箱（用户页）；要腾出整块空地（大块连续内存）时只搬可搬区的箱子（compaction）——承重柱不动，空地照样腾出来。 [写作时展开]

### 6. GFP Flags — 分配行为的四类位域控制字

场景提示: `GFP_KERNEL` 和 `GFP_ATOMIC` 有什么区别？中断上下文为什么不能用前者？ [写作时展开]

关键设计: GFP 是四类位域的组合（include/linux/gfp.h）：

| 类 | 位 | 含义 |
|----|----|------|
| Zone | __GFP_DMA / __GFP_DMA32 / __GFP_HIGHMEM | 从哪个 zone 分配 |
| Reclaim | __GFP_DIRECT_RECLAIM（阻塞）/ __GFP_KSWAPD_RECLAIM（不阻塞） | 能否回收 |
| Watermark | __GFP_HIGH（MIN 水位）/ __GFP_ATOMIC（不睡眠） | 准入级别 |
| 常用宏 | GFP_KERNEL=RECLAIM+IO+FS / GFP_ATOMIC=HIGH+ATOMIC+NOWARN / GFP_USER / GFP_NOWAIT | 组合 |

Why: 为什么"分配"还要声明这么多行为？——**分配方知道自己的限制**：中断上下文不能睡眠（必须 GFP_ATOMIC 禁止 reclaim）、实时路径不能阻塞（GFP_NOWAIT）、普通进程可以等回收（GFP_KERNEL）。**GFP 是"分配方与内核的契约"**——分配方声明"我能等多久/从哪拿"，内核按契约选择路径（fast/medium/slow 的准入条件）。

比喻锚点: GFP=点餐备注——同样的菜（分配内存）可以备注"不要辣/马上要/可以等"（GFP_ATOMIC/NOWAIT/KERNEL）；厨房（内核）按备注决定"先做（fast）/插队（medium）/排队等（slow）"——备注是食客（分配方）和厨房（内核）的契约。 [写作时展开]

### 7. Watermark 三级水位 — 内存压力的分水岭

场景提示: 内存快满了——怎么在"完全耗尽"前预警并开始回收？ [写作时展开]

关键设计: 三级水位（mm/page_alloc.c）：

```[pseudocode]
WMARK_MIN: 紧急预留(默认 min_free_kbytes ~4MB) — 只能 __GFP_HIGH/ATOMIC 用
WMARK_LOW: 低于此 → 唤醒 kswapd(异步回收)
WMARK_HIGH: 高于此 → kswapd 停止
分配检查: zone_watermark_fast → __zone_watermark_ok → free_pages ≥ mark
lowmem_reserve: 保护低 zone 不被高 zone 耗尽; MIN = sqrt(managed_pages*16)
```

错误示例: 中断上下文用 GFP_KERNEL → 睡眠 → kernel panic；GFP_ATOMIC 必须传 __GFP_HIGH 访问紧急预留。

Why: 为什么需要"三级"而非"满/空"两级？——**回收需要提前量**：kswapd 从 LOW 开始异步回收（不阻塞分配），到 HIGH 停止——**在 MIN（危险线）之前就把内存回收回安全区**；MIN 是"最后防线"（原子分配/中断用）。两级（满/空）会"一次到底"——要么没预警直接 OOM，要么过度回收浪费。三级水位 = 预警线（LOW）+ 安全线（HIGH）+ 生死线（MIN）。

比喻锚点: 三级水位=水库三级警戒——LOW 是"水位到黄线开始放水"（kswapd 异步回收）、HIGH 是"回落到绿线停"、MIN 是"红线以下只能消防用水"（原子分配专用）——没有黄线预警，直接见底（OOM）就晚了。 [写作时展开]

### 8. vmalloc — 虚拟连续物理不连续的内存区

场景提示: 要一块 100MB 的"连续"内存——物理连续找不到（碎片）怎么办？ [写作时展开]

关键设计: vmalloc 提供**虚拟连续、物理不连续**的内存（mm/vmalloc.c）：

```[pseudocode]
vmalloc(size) → __vmalloc_node_range(vmalloc 区)
alloc_vmap_area: vmap_area_root 红黑树 first-fit 找空闲虚拟区间
vmap_pages_range: 逐页 alloc_pages 分配物理页 + 建页表映射
struct vmap_area: va_start/va_end/flags + rb_node(红黑树) + list(链表) 双索引
```

对比: **kmalloc** 返回直接映射区地址（物理连续，零页表，快）/ **vmalloc** 返回 vmalloc 区地址（物理可能不连续，逐页建页表，慢）。

Why: 为什么存在 vmalloc？——**大块虚拟连续的需求 vs 物理碎片现实**：模块/驱动要大片"看起来连续"的地址（代码期待连续访问），但物理内存可能碎片化（找不到 100MB 连续块）。vmalloc 用"虚拟连续"骗过需求方——**代价是逐页建页表的慢路径**（所以只在非热路径用：内核模块/大缓冲区），热路径（页缓存/网络）坚持 kmalloc。

比喻锚点: vmalloc=拼租房——租客（驱动）要"一整层"（连续地址），但整层（物理连续块）没有，就租 5 间分散的房间（物理页）+ 一条虚拟走廊（页表）串起来——看着是一层（虚拟连续），实际分散（物理不连续）；kmalloc 是整层现房（物理连续），有就快，没有就等。 [写作时展开]

### 9. 收束

回到"怎么从身份证组装连续块"：
- 结构 = free_area（order × 迁移类型）
- 分配 = fast/medium/slow 三层 + PCP 无锁
- 释放 = 位运算找伙伴 + 递归合并
- 反碎片 = 迁移类型 + fallback 矩阵
- 控制 = GFP 四类位域 + 三级水位
- 对照 = vmalloc（虚拟连续）vs kmalloc（物理连续）

**Aha Moment**: "Buddy 的全部智慧浓缩在一条位运算里：`PFN ^ (1<<order)` 找伙伴、拆大块满足小块、合并恢复大块——O(1) 的分配与释放。而 GFP/水位/迁移类型都是'把决策前置'：分配方声明限制（GFP）、内核预置闸门（水位）、空闲页预先分类（迁移）——让分配路径尽量走 fast。"
**回答读者三问**: ①分配 16KB 怎么走=fast→medium→slow 三层；②每核分配为何快=PCP 无锁热页；③中断为何不能 GFP_KERNEL=reclaim 会睡眠。

---

### 核心悬念

**"虚拟地址到物理地址需要 4 级页表遍历 — PGD/PUD/PMD/PTE 各 9 位偏移怎么组织？缺页时硬件怎么触发软件处理？"**

→ 引出 03 页表与 TLB — 四级页表结构 + PTE 64 位标志位 + TLB shootdown——物理块组装好了，怎么翻译给进程看。
