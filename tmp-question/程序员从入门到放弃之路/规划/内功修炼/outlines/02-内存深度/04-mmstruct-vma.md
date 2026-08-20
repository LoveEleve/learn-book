# mm_struct 与 VMA — 进程地址空间的蓝图

> Cluster B: 4 KPs | 依赖: 03-pagetables-tlb | 读者基线: 页表四级遍历 + TLB shootdown
> 读者处境: 已读 03 篇（页表翻译器）；本篇回答"翻译器背后的'地图'——进程地址空间怎么描述、怎么组织、怎么动态变化"
> 打开新视角: mm_struct 的双引用计数、VMA 双索引（红黑树+链表）、merge/split 的动态重组、mmap 从 syscall 到 VMA 的完整路径

---

### 概念依赖链

```
03-页表(翻译器) → 本篇: mm_struct/VMA(地图)
  ├─ §1 mm_struct 字段(进程地址空间控制块 — 依赖 03 pgd)
  │    ├─ §2 VMA 描述(区间+双索引 — 依赖 §1)
  │    │    ├─ §3 VMA Merge/Split(动态重组 — 依赖 §2)
  │    │    └─ §4 mmap 全路径(syscall→VMA — 依赖 §2/§3)
先讲: 控制块(mm_struct) → 区间(VMA) → 重组(merge/split) → 路径(mmap)
后续依赖: 05-缺页四场景(访问时按 VMA 处理)
```

### 叙事顺序

1. 问题引入——`/proc/PID/maps` 每行一个区间——进程地址空间用什么结构描述？（**Aha: 地址空间=mm_struct 控制块 + 一串 VMA 区间——页表是翻译器，VMA 是地图**）
   - 过渡: 控制块里有什么？——mm_struct
2. mm_struct 字段——双索引/双引用/地址边界/统计
   - 过渡: 每个区间怎么描述？——VMA
3. VMA 描述——vm_start/end/flags/file/ops + 双索引设计
   - 过渡: 区间会变化（mmap 相邻合并/分裂）——怎么重组？
4. VMA Merge/Split——vma_merge 兼容性检查；__split_vma
   - 过渡: mmap 一次系统调用——完整路径？
5. mmap 全路径——get_unmapped_area → mmap_region → call_mmap；munmap
   - 过渡: 完整图景已齐——收束
6. 收束——控制块→区间→重组→路径的完整链条

### 1. mm_struct 完整字段 — 进程的虚拟内存控制块

场景提示: 每个进程一个 mm_struct——里面装了什么？为什么"线程共享 mm、进程不共享"？ [写作时展开]

关键设计: `struct mm_struct` 核心字段（include/linux/mm_types.h）：

| 字段 | 含义 |
|------|------|
| mmap / mm_rb | VMA 链表头 / 红黑树根（双索引） |
| pgd | 进程级 PGD（页表根，03 篇） |
| mmap_lock | 地址空间读写锁（rwsem，mmap/munmap/缺页都要） |
| mm_users / mm_count | 双引用计数（用户态引用 ≠ 内核引用） |
| task_size / mmap_base | 用户空间上限(128TB) / mmap 基址 |
| start_code~end_data | 代码/数据段边界 |
| start_stack / start_brk / brk | 栈 / 堆边界（sbrk 用） |
| total_vm/locked_vm/... | 统计字段（/proc/PID/status 来源） |

Why: 为什么"双引用计数"？——**两类持有者生命周期不同**：`mm_users` 是用户态引用（进程+线程共享，线程退出减一）；`mm_count` 是内核引用（page fault handler/OOM reaper 临时持有，用 mmgrab/mdrop）——**mm_users 归零但 mm_count>0 时，地址空间还在（内核正在用它），只是没有用户视图**。双计数保证"最后的使用者"（可能是内核）释放 mm，而非"最后的进程"。

比喻锚点: mm_struct=公司会议室预订——mm_users 是"预约人数"（进程+线程都算，走了减一）；mm_count 是"正在使用的人"（保洁/电工=内核还在里面收拾）——预约的人全走了，但电工还没撤（mm_count>0），会议室不能拆。 [写作时展开]

### 2. VMA (vm_area_struct) — 虚拟地址区间的完整描述

场景提示: `/proc/PID/maps` 的一行 = 一个 VMA——它描述什么？怎么快速找"地址 X 属于哪个 VMA"？ [写作时展开]

关键设计: `struct vm_area_struct`（include/linux/mm_types.h）：

```[pseudocode]
双索引设计: 红黑树(mm_rb) = O(log n) 查找任意地址(find_vma)
           链表(mmap) = O(1) 顺序遍历(/proc/pid/maps 按地址输出)
vm_start/vm_end: 半开区间 [start, end)
vm_flags: VM_READ|VM_WRITE|VM_EXEC|VM_SHARED|VM_GROWSDOWN(栈)|VM_LOCKED(mlock)|VM_HUGETLB|VM_MERGEABLE(KSM)...
vm_file + vm_pgoff: 文件映射→file+偏移; 匿名→NULL
vm_ops: open/close/fault/page_mkwrite — 缺页回调(05 篇用)
vm_page_prot: PTE 保护位模板
```

Why: 为什么"红黑树+链表"双索引？——**两种访问模式**：缺页时要"按地址查归属"（O(log n) 红黑树）；遍历输出（/proc/maps）要"按地址顺序"（链表天然有序）。**单一结构无法同时服务两种需求**——红黑树定位快但遍历要中序；链表遍历快但查找 O(n)。双索引 = 空间换时间（两个指针域），这是"访问模式决定数据结构"的典型实例。 [内核: 阶段1-03 篇 VMA 概念→本篇字段级——find_vma 是缺页路径高频调用]

比喻锚点: VMA 双索引=酒店房间登记——按房号快速查"301 谁住"用编号册（红黑树 O(log n)）；按入住顺序查"今天入住了哪些"用流水账（链表）——两种查询方式，两本账。 [写作时展开]

### 3. VMA Merge & Split — 地址空间的动态重组

场景提示: 连续 mmap 多次——VMA 会越来越多吗？相邻区间为什么能合并？ [写作时展开]

关键设计: 动态重组（mm/mmap.c）：

```[pseudocode]
vma_merge(mm, prev, addr, end, flags, ...): 检查相邻 VMA 兼容性
  相同 vm_flags + 相同 vm_file + vm_pgoff 连续 + anon_vma 一致 + policy 一致 → 合并
__split_vma(mm, vma, addr, new_below): 在 addr 处分裂
  分配新 vm_area_struct → 复制字段 → 调整 start/end → 插入双索引
vma_adjust: 通用 adjust — 处理合并/分裂/扩展/收缩 4 场景
```

Why: 为什么合并重要？——**VMA 数量直接影响性能**：`find_vma` 红黑树 O(log n)（VMA 多则深）、`/proc/maps` 输出大小、**mmap_lock 竞争**（每次 mmap/munmap 要拿写锁，VMA 多则锁持时长）——合并把"相邻同属性区间"折叠成一个，**VMA 数量从"每次 mmap 一个"降到"属性边界一个"**。这是地址空间的"碎片整理"（与 Buddy 合并同思想）。

比喻锚点: VMA 合并=同楼层打通——隔壁两间房（相邻 VMA）同规格（相同 flags/file）就打通成一间（merge）；打通不了就隔开（split）——房间越少，找房（find_vma）越快，物业（mmap_lock）越省心。 [写作时展开]

### 4. mmap 内核完整路径 — syscall 到 VMA 建立的全过程

场景提示: 一次 `mmap` 系统调用——内核做了哪三件事？ [写作时展开]

关键设计: 全路径（mm/mmap.c）：

```[pseudocode]
sys_mmap → ksys_mmap_pgoff → vm_mmap_pgoff → do_mmap
Step 1: get_unmapped_area — 找空闲虚拟地址(自下而上 first-fit / 顶部向下 ASLR)
Step 2: mmap_region — 配额检查(may_expand_vm → RLIMIT_AS) → vm_area_alloc 创建
        → vma_link 插入双索引
Step 3: call_mmap — 文件: ext4_file_mmap → vma→vm_ops; 匿名: vma_set_anonymous
munmap: sys_munmap → __do_munmap → unmap_region → zap_page_range(释放页表+物理页)
```

flags: MAP_SHARED（写回文件）/MAP_PRIVATE（COW）/MAP_ANONYMOUS/MAP_FIXED/MAP_LOCKED（mlock）/MAP_POPULATE（预分配→mm_populate）/MAP_HUGETLB。

Why: 为什么 mmap 是"三步"？——**每步一个独立职责**：找地方（地址分配）、登记（VMA 建立+索引）、挂钩（vm_ops 决定缺页行为）。**注意 mmap 不分配物理页**（阶段1-03 篇承诺 vs 兑现）——Step 2 只建 VMA（登记承诺），物理页等缺页（05 篇兑现）。三步分离让"承诺"（mmap）与"兑现"（fault）解耦。

比喻锚点: mmap 三步=租商铺——先看位置（get_unmapped_area 找空铺位）、再签合同（mmap_region 建 VMA 登记）、最后定经营方式（call_mmap 挂 vm_ops 决定"卖什么=缺页怎么处理"）——签合同不进货（不分配物理页），开张（首次访问）才进货。 [写作时展开]

### 5. 收束

回到"进程地址空间的蓝图"：
- 控制块 = mm_struct（双索引/双引用/边界/统计）
- 区间 = VMA（双索引设计，O(log n) 定位）
- 重组 = merge/split（VMA 数量控制）
- 路径 = mmap 三步（找地方/登记/挂钩）

**Aha Moment**: "地址空间不是'一张大图'，是 mm_struct 控制块 + 一串 VMA 区间——每个区间自带权限、文件映射、缺页回调。而双索引（红黑树+链表）和 merge/split 都指向同一个目标：**VMA 操作要快，VMA 数量要少**——因为缺页是最高频路径，每次都要 find_vma。"
**回答读者三问**: ①地址 X 属于哪个 VMA=红黑树 O(log n)；②VMA 为何不多=merge 合并相邻同属性；③mmap 为什么不分配物理页=三步只登记承诺，缺页才兑现。

---

### 核心悬念

**"VMA 建好了，第一次访问虚拟地址触发缺页异常 — 匿名页、COW、文件页、栈扩展四种场景的 #PF 处理路径各有什么区别？"**

→ 引出 05 缺页异常四场景 — do_anonymous_page / do_wp_page / do_fault / expand_stack——地图有了，访问时怎么"按图兑现"。
