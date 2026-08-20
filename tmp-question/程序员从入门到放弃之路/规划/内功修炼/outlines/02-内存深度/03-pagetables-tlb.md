# 页表与 TLB — 从虚拟地址到物理地址的五级跳

> Cluster B: 6 KPs | 依赖: 02-buddy-pcp-gfp | 读者基线: Buddy 分配器 + 虚拟内存概念
> 读者处境: 阶段1-02 篇已建立页表概念（4 级遍历/PTE 位/TLB）；本篇源码深挖——"结构长什么样、锁怎么分、硬件怎么加速"
> 打开新视角: 页表类型的逐级类型（pgd_t/pte_t）、软件域标志位、页表锁从全局到 split、TLB 硬件层次与 PCID、shootdown 的 IPI 代价

---

### 概念依赖链

```
阶段1-02(页表概念) + 02-buddy(物理页) → 本篇: 页表源码深挖
  ├─ §1 四级页表结构(pgd_t 等类型 + 遍历宏 — 依赖阶段1-02)
  │    ├─ §2 PTE 标志位全集(硬件域+软件域 — 依赖 §1)
  │    │    └─ §3 页表锁分级(全局→split PTL — 依赖 07 锁)
  │    ├─ §4 TLB 硬件组织(ITLB/DTLB/PCID — 依赖 §1)
  │    │    ├─ §5 TLB shootdown(跨核一致性 — 依赖 §4)
  │    │    └─ §6 大页(PMD/PUD 级映射 — 依赖 §1)
  │    │         └─ §7 缓存层次与 TLB 关系(硬件视角 — 依赖 §4)
先讲: 结构(类型) → 标志(双域) → 锁(分级) → 缓存(TLB) → 一致性(shootdown) → 优化(大页) → 硬件(缓存层次)
后续依赖: 04-mm_struct 与 VMA
```

### 叙事顺序

1. 问题引入——阶段1 说"4 级查表"——但源码里各级是什么"类型"？怎么从 mm 走到 PTE？（**Aha: 每级是一种 8 字节类型 + 一个偏移宏——遍历是'类型跳转'而非'通用查表'**）
   - 过渡: 查到最后得到的 PTE——64 位里藏了什么？
2. PTE 64 位全集——硬件域（Present/RW/NX...）+ 软件域（SPECIAL/PROTNONE）
   - 过渡: 多个 CPU 同时改页表——锁怎么分？
3. 页表锁分级——全局 page_table_lock → PTE lock/PMD lock
   - 过渡: 翻译快不快还看硬件——TLB
4. TLB 硬件组织——ITLB/DTLB 分离、L2 TLB、PCID 标签
   - 过渡: 多核改页表——其他核的 TLB 怎么办？
5. TLB shootdown——IPI 跨核刷新；mm_cpumask 精确定位
   - 过渡: TLB 覆盖太小——大页？
6. 大页——2MB/1GB 级映射；TLB 覆盖 512 倍
   - 过渡: TLB 与缓存的关系——硬件全景
7. 缓存层次与 TLB——walk 的内存访问成本；物理索引
   - 过渡: 完整图景已齐——收束
8. 收束——页表/PTE/锁/TLB 的完整链条

### 1. 四级页表完整结构 — PGD → PUD → PMD → PTE

场景提示: 源码里 `pgd_t`/`pte_t` 是什么？`pgd_offset(mm, addr)` 怎么从 mm 一路走到 PTE？ [写作时展开]

关键设计: 每级一个 8 字节类型（arch/x86/include/asm/pgtable_64_types.h）：

```[pseudocode]
虚拟地址分解: PGD(9) → PUD(9) → PMD(9) → PTE(9) → offset(12) — 48 位(4 级)
五级(La57): 加 P4D — 57 位
类型: pgd_t/p4d_t/pud_t/pmd_t/pte_t — 各 8 字节
遍历宏链: pgd_offset(mm, addr) → p4d_offset → pud_offset → pmd_offset → pte_offset_map
空条目检查: pgd_none(*pgd)(全 0) → p4d_alloc/pud_alloc/pmd_alloc 逐级分配中间页表
P4D 折叠: non-5LEVEL 时 p4d_offset 透明旁路 — CR3 → 4 次内存访问 = 1 次完整 walk
```

Why: 为什么每级用"独立类型"而非"统一指针"？——**类型即语义**：`pgd_t` 是"页全局目录项"、`pte_t` 是"页表项"——编译器防止混用（不能把 PGD 当 PTE 用）；且各级的分配策略不同（pgd 随 mm 分配一次、pmd/pte 按需懒分配）。**遍历宏链 = 类型安全的逐级跳转**——每步一个偏移宏（9 位索引），类型保护防止跨级误用。 [内核: 阶段1-02 篇概念版→本篇源码版——类型/宏/锁全部落地]

比喻锚点: 四级页表=快递分拣中心四级流水线——每级是不同"工位"（pgd_t/pud_t/pmd_t/pte_t 各自独立类型），包裹（虚拟地址）按 9 位分拣码逐级送：大区（PGD）→ 分区（PUD）→ 街道（PMD）→ 门牌（PTE）——每级工位只认自己的码段（类型安全），不会把门牌当大区。 [写作时展开]

### 2. PTE 64 位标志位全集 — 硬件 + 软件双域控制

场景提示: 阶段1 讲了 Present/RW/NX——还有哪些位？"软件域"是什么？ [写作时展开]

关键设计: PTE 64 位分**硬件域 + 软件域**（arch/x86/include/asm/pgtable_types.h）：

```[pseudocode]
硬件域(bits 0-8, 12-51, 63):
  bit0 _PAGE_PRESENT(0x001) / bit1 _PAGE_RW(0x002, 0 触发 COW)
  bit2 _PAGE_USER(0x004) / bit5 _PAGE_ACCESSED(0x020)
  bit6 _PAGE_DIRTY(0x040) / bit7 _PAGE_PSE(2MB) / bit8 _PAGE_GLOBAL(TLB 不刷)
  bit12-51 PFN / bit63 _PAGE_NX(No-eXecute)
软件域(bits 9-11):
  bit9 _PAGE_SPECIAL(非 struct page) / bit8 复用 _PAGE_PROTNONE(NUMA 迁移)
  bit11 _PAGE_SOFT_DIRTY(CRIU 迁移)
操作函数: set_pte / ptep_set_wrprotect(写保护+TLB flush) / ptep_set_access_flags
  / pte_mkclean/pte_mkdirty/pte_mkold/pte_young
```

Why: 为什么要有"软件域"？——**硬件只用 0-8/12-51/63，中间的位空闲**——内核把"自己需要但硬件不读"的状态塞进空闲位（SPECIAL/PROTNONE/SOFT_DIRTY）：硬件访问时忽略这些位（不触发行为），内核读 PTE 时检查（决策依据）。**同一 64 位，硬件看一部分、软件看一部分**——双域分治让"一个表项同时服务硬件翻译和内核管理"。

比喻锚点: PTE 双域=快递单两面——正面（硬件域）给机器扫码用（PRESENT/RW/NX），背面（软件域）给人工备注用（SPECIAL/PROTNONE）——同一张单子，扫码机看正面、管理员看背面。 [写作时展开]

### 3. 页表锁分级 — 从全局锁到 split PTL

场景提示: 多核同时 mmap/mprotect——改页表要锁吗？一把锁够吗？ [写作时展开]

关键设计: 锁粒度演进（mm/memory.c）：

```[pseudocode]
旧式(2.6.23 前): mm→page_table_lock 全局自旋锁 — 所有操作争用同一锁(瓶颈)
PMD lock: THP(大页=PMD 级映射) → pmd_lock(mm, pmd) → spin_lock
PTE lock: 普通页 → pte_lockptr(mm, pmd) → spin_lock — 每个 PTE 表一把锁
组合: pte_offset_map_lock(mm, pmd, addr, &ptl) 获取 / pte_unmap_unlock 释放
```

Why: 为什么从"一把全局锁"演进到"每 PTE 表一把锁"？——**锁粒度决定并发度**：全局锁 = 两个 CPU 改不同区域的页表也要互等（07 篇锁粒度演进的内核实例）；split PTL 让"不同 PTE 表"的修改并行（不同地址区域互不干扰）。**代价是锁数量暴涨**（每 PTE 表一把）——但页表操作是短临界区（改一个 PTE），锁多开销可忽略，并发收益显著。

比喻锚点: 页表锁分级=图书馆管理——旧式是"全馆一把钥匙"（改任何书架都要等这把钥匙）；split PTL 是"每排书架一把钥匙"（改 A 排不影响 B 排）——钥匙多了但排队少了。 [写作时展开]

### 4. TLB 硬件组织 — 页表缓存的层次结构

场景提示: 翻译查 4 级页表太慢——TLB 硬件怎么组织才能"大多数命中"？ [写作时展开]

关键设计: 两级 TLB + 标签机制：

```[pseudocode]
L1 TLB: ITLB(指令)+DTLB(数据)分离, 全相联/组相联, ~64 项, 命中 ~1 cycle
L2 TLB: ~1024-2048 项, 统一, miss → PMH(Page Miss Handler) 硬件 walk
PCID(x86)/ASID(ARM): 12 位 → 4096 标签 — TLB 条目打 PCID 标签
  → 上下文切换不刷 TLB, 只切 CR3 低 12 位
invlpg addr(单条无效) vs CR3 重写(全刷新, ~1000+ cycles)
```

Why: 为什么 TLB 要"分离 + 分级 + 标签"？——**分离**（ITLB/DTLB）：指令/数据访问模式不同，独立缓存互不污染；**分级**（L1 快小/L2 慢大）：局部性命中 L1、miss 才查 L2；**PCID 标签**：上下文切换时 TLB 条目"打上进程标签"而非清空——**切换进程的 TLB 开销从'全刷'降到'换标签'**。三者都是"按访问模式优化"的硬件工程。

比喻锚点: TLB=快递柜分层——L1 是门口小柜（常用几格，秒开）；L2 是楼道大柜（更多格）；PCID 是"每户标签"——换住户（进程切换）不用清空整个柜子，只看标签拿自己的件。 [写作时展开]

### 5. TLB shootdown — 跨核刷新的一致性协议

场景提示: CPU0 改了一个页的权限——CPU1 的 TLB 还缓存旧权限——怎么办？ [写作时展开]

关键设计: 跨核一致性（arch/x86/mm/tlb.c）：

```[pseudocode]
触发: mprotect/munmap/KSM 合并/THP 拆分 → 修改页表
flush_tlb_mm_range(mm, start, end) → smp_call_function_many(向 mm→cpumask 所有核)
IPI vector=0xfd(CALL_FUNCTION_VECTOR) → 目标核中断上下文执行
  flush_tlb_func → __flush_tlb_one_user(addr): invlpg addr
优化: tlb_is_not_lazy 检测(lazy TLB 延迟刷新) / mm_cpumask 精确定位(只发需要的核)
```

Why: 为什么 shootdown 是"昂贵"的？——**每次改页表都要 IPI 中断其他核**（中断上下文执行 invlpg）：一次 mprotect 的代价 = 目标核数 × 中断处理开销。频繁 munmap（如 JVM 卸载类）会产生大量 IPI——**这就是"改页表"的隐性成本**：不是改一行内存，是跨核广播。mm_cpumask 精确定位（只通知用过该 mm 的核）是主要的优化手段。

比喻锚点: TLB shootdown=全校改作息表——改一次（改页表）要广播到每个班（核），各班撕旧表贴新表（invlpg）；只通知"用过旧表的班"（mm_cpumask）能省一半广播费——但广播本身（IPI）永远不便宜。 [写作时展开]

### 6. 大页 — TLB 覆盖率的指数级提升

场景提示: 阶段1 提过 THP——源码里 2MB 页映射在哪一级？ [写作时展开]

关键设计: 大页 = 跳过中间级直接映射（arch/x86/mm/hugetlbpage.c）：

```[pseudocode]
2MB page: 虚拟地址 bit21-47 → PMD 级直接指向 → 1 个 TLB 条目 = 512 个 4KB 条目
1GB page: bit30-47 → PUD 级 → Gigantic Huge Page
缺页: hugetlb_fault → hugetlb_no_page → alloc_huge_page
配置: hugepagesz=2M hugepages=1024 / /proc/sys/vm/nr_hugepages / mmap MAP_HUGETLB
缺点: compaction + 碎片
```

Why: 为什么大页是"指数级"提升？——4KB 页的 TLB 覆盖 = 4KB × 1024 项 = 4MB；2MB 页 = 2MB × 1024 = **2GB（512 倍）**。数据库 100GB 工作集：4KB 页要 25600 次 TLB miss，2MB 页只要 50 次。**代价是物理连续**（2MB 连续块难找）——所以配 compaction（02 篇的 MOVABLE 迁移）支持大页分配。

比喻锚点: 大页=整栋楼一个门牌 vs 每户一个门牌——整栋（2MB）一个门牌（TLB 条目）就能找到，但整栋必须连着盖（物理连续）；每户（4KB）门牌多但好找地盖。 [写作时展开]

### 7. CPU 缓存层次与 TLB 的硬件关系

场景提示: TLB miss 的"4 次内存访问"到底多贵？和缓存什么关系？ [写作时展开]

关键设计: 硬件全景：

```[pseudocode]
TLB miss → PMH 硬件 walk → 每次 walk 4 次内存访问(L1→L2→L3→DRAM)
  L1 命中 <10 cycles / DRAM >100 cycles
L1/L2/L3 用物理地址索引(physically indexed) → TLB 翻译必须在 L1 访问前完成
  → TLB 命中率直接影响所有内存访问延迟
预取器与 TLB 独立工作 → 可能预取未映射页 → 浪费带宽
查看: /sys/devices/system/cpu/cpu*/cache/
```

Why: 为什么 TLB 命中率"决定一切"？——**缓存是物理寻址的**：L1 缓存要先知道物理地址才能查（物理索引）；而物理地址来自 TLB 翻译——**TLB miss 先于缓存 miss**：每次缓存访问都依赖 TLB 先命中。所以 TLB 命中率是"所有内存访问延迟的地基"——这也解释了大页（§6）为何如此重要：TLB 覆盖大了，地基才稳。 [内核: 缓存一致性(阶段1-05 篇 MESI)建立在物理地址之上——TLB 翻译是缓存访问的前置]

比喻锚点: TLB=门禁卡，缓存=房间——进房间（缓存命中）之前先刷卡（TLB 翻译）知道门牌（物理地址）；卡刷不上（TLB miss）连房间都进不去——门禁系统（TLB）卡顿，整栋楼（内存访问）都慢。 [写作时展开]

### 8. 收束

回到"虚拟地址到物理地址的五级跳"：
- 结构 = 四级类型（pgd_t→pte_t）+ 遍历宏链
- 标志 = 硬件域 + 软件域双域分治
- 锁 = 全局 → split PTL（并发演进）
- 缓存 = ITLB/DTLB + L2 + PCID 标签
- 一致性 = shootdown IPI（mm_cpumask 精确）
- 优化 = 大页（PMD/PUD 级映射）

**Aha Moment**: "页表翻译不是'查一张表'，是'四级类型跳转 + 双域标志 + 分级锁 + 硬件缓存'的完整系统——而最反直觉的是：**TLB 命中率决定缓存访问延迟**（缓存物理寻址依赖 TLB 先翻译）。理解了这条因果链，大页/PCID/shootdown 的每个设计都水到渠成。"
**回答读者三问**: ①mm 怎么走到 PTE=遍历宏链类型跳转；②改页表要锁吗=split PTL 每表一把；③shootdown 为何贵=IPI 中断目标核。

---

### 核心悬念

**"进程的地址空间怎么组织？mm_struct 和 VMA 红黑树 + 链表双索引怎么让内核 O(log n) 找到任意地址的 VMA？"**

→ 引出 04 mm_struct 与 VMA — 进程地址空间的数据结构蓝图——页表是"翻译器"，mm_struct/VMA 是"地图"。
