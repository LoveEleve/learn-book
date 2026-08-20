# 反向映射 — 如何从物理页反查所有映射它的进程

> Cluster C: 3 KPs | 依赖: 07-writeback-dirty | 读者基线: 脏页写回 + 匿名页换出需求
> 读者处境: 已读 07 篇（写回正向路径）；本篇回答"回收/换出时，内核怎么从'一个物理页'反查'所有映射它的进程'？"
> 打开新视角: anon_vma 的 fork 共享结构、rmap_walk 的反向遍历、try_to_unmap_one 的 PTE 替换、munmap 的批量释放

---

### 概念依赖链

```
07-写回(正向) + 04-VMA(映射) + 01-refcount → 本篇: 反向映射
  ├─ §1 anon_vma + anon_vma_chain(数据结构 — 依赖 04 VMA)
  │    └─ §2 rmap_walk 调用链(遍历映射者 — 依赖 §1)
  │         └─ §3 try_to_unmap_one(PTE 清除/替换 — 依赖 §2)
  │              └─ §4 munmap 全路径(批量释放 — 依赖 03 PTE)
先讲: 结构(anon_vma) → 遍历(rmap_walk) → 操作(unmap) → 批量(munmap)
后续依赖: 09-LRU 与回收(换出用反向映射)
```

### 叙事顺序

1. 问题引入——内核要换出"一个物理页"——但可能有 5 个进程映射它——怎么找到这 5 个？（**Aha: 正向映射是"页表→物理页"，反向映射是"物理页→所有页表"——回收需要反向**）
   - 过渡: 反向关系存在哪？——anon_vma
2. anon_vma + anon_vma_chain——fork 共享的根 + VMA 连接
   - 过渡: 有了结构——怎么遍历？
3. rmap_walk 调用链——anon/file 两种遍历；try_to_unmap_one
   - 过渡: 遍历到每个 VMA 后做什么？
4. try_to_unmap_one——清 PTE → swap entry / 直接清除
   - 过渡: munmap 是"主动解除"——路径？
5. munmap 全路径——zap_pte_range + MMU gather 批量
   - 过渡: 完整图景已齐——收束
6. 收束——结构→遍历→操作→批量的完整链条

### 1. 匿名反向映射 — anon_vma + anon_vma_chain 的数据结构

场景提示: fork 后父子共享匿名页——要换出时怎么找到"所有共享者"？ [写作时展开]

关键设计: 反向映射的数据结构（include/linux/rmap.h）：

```[pseudocode]
struct anon_vma: root(根, 最早 fork 那个) + rwsem + refcount + degree(fork 次数)
struct anon_vma_chain: 连接 VMA 和 anon_vma
  vma + anon_vma + same_vma/same_anon_vma 双链表
fork 链: dup_mmap → anon_vma_fork(vma, pvma) → 子 VMA 加入父的 anon_vma
  → anon_vma_chain_link
核心: 所有映射同一匿名页的 VMA 通过同一个 anon_vma 关联
```

Why: 为什么匿名页用"共享的 anon_vma"而非"每页记录映射者列表"？——**物理页与映射者的关系是"多对多"**：每页记映射者列表（页→列表）内存开销爆炸（每页都要列表）；**anon_vma 按"fork 家族"组织**（同一 fork 链共享一个 anon_vma）——页的 struct page 只需一个 `anon_vma` 指针（01 篇 mapping 字段），反查时沿 anon_vma 遍历家族所有 VMA。**把"页→列表"压缩成"页→家族"**——空间换遍历时间。

比喻锚点: anon_vma=家族族谱——一页（一个人）不需要记"认识我的所有人"，只需记"我属于哪个家族"（anon_vma 指针）；要找人（换出时找映射者）就翻家族族谱（沿 anon_vma 遍历所有 fork 分支的 VMA）。 [写作时展开]

### 2. 反向映射调用链 — rmap_walk 到 try_to_unmap_one

场景提示: 回收一个匿名页——调用链怎么从"页"走到"每个映射者的 PTE"？ [写作时展开]

关键设计: 反向遍历（mm/vmscan.c, mm/rmap.c）： [内核: 阶段1-04 篇 swap 概念→本篇反向映射源码——换出路径的"找映射者"环节]

```[pseudocode]
回收触发: shrink_folio_list → try_to_unmap(folio, TTU_BATCH_FLUSH)
匿名: rmap_walk_anon → anon_vma_interval_tree_foreach(anon_vma->rb_root, pgoff, pgoff)
  → 找所有映射此页的 VMA → 每个 VMA → try_to_unmap_one(page, vma, address)
文件: rmap_walk_file → vma_interval_tree_foreach(address_space->i_mmap 树)
  → try_to_unmap_one
辅助: page_vma_mapped_walk(边走边验证 page 仍映射) + mmap_read_lock(mm)
```

Why: 为什么"反向遍历"要精确匹配 pgoff？——物理页 → 虚拟地址的映射是**每 VMA 一个偏移**（同一物理页在不同进程映射在不同虚拟地址）；`anon_vma_interval_tree_foreach(rb_root, pgoff, pgoff)` 用**文件偏移区间树**精确找出"哪些 VMA 映射了这个 pgoff"——**区间树按 pgoff 索引**，一次查询 O(log n) 拿到所有相关 VMA，而不是遍历全部 VMA 逐个比对。

比喻锚点: rmap_walk=按"页号"查住户——不用挨家敲门问"你家有没有这本书"（遍历所有 VMA 比对），而是查"页号登记册"（pgoff 区间树）直接知道哪几家有（O(log n) 命中所有相关 VMA）。 [写作时展开]

### 3. try_to_unmap_one — PTE 清除的核心操作

场景提示: 找到映射者了——怎么"解除"映射？直接清 PTE？ [写作时展开]

关键设计: 核心操作（mm/rmap.c）：

```[pseudocode]
ptep_get_and_clear(mm, address, pte): 原子获取并清除 PTE → 返回旧值
匿名页: 构造 swp_entry(val) → set_pte_at(swp_entry_to_pte(entry))
  → PTE 变成"swap 类型"(缺页时 do_swap_page 换回)
文件页: 清除 PTE → page_remove_rmap(减映射计数) → page_cache_release
通知: mmu_notifier_invalidate_range_start/end — 通知 KVM/Xen 虚拟化层
批量: TTU_BATCH_FLUSH — 收集需 flush 的地址 → 统一 flush_tlb_range(减少 IPI)
```

Why: 为什么匿名页要"替换为 swap entry"而非"直接清除"？——**数据不能丢**：匿名页内容只在内存（无文件后备）；换出（swap）后 PTE 要"记住这页去哪了"——`swp_entry_to_pte` 把"swap 位置"编码进 PTE，缺页时 `do_swap_page`（阶段1-04 篇）按 entry 读回。**文件页可以直接清除**（有后备，缺页重读）——两种页的 unmap 语义不同：匿名"搬家"，文件"退房"。

比喻锚点: try_to_unmap_one=退房 vs 搬家——文件页是"退房"（直接交钥匙=清 PTE，东西（数据）在仓库（磁盘）里）；匿名页是"搬家"（把东西搬进储物间=swap，门牌上贴'东西在储物间 X'=swap entry）——回来时（缺页）按门牌去储物间取。 [写作时展开]

### 4. Freeing Userland Memory — munmap → zap_pte_range 全路径

场景提示: munmap 一大段地址——怎么"批量"释放页表+物理页？ [写作时展开]

关键设计: munmap 全路径（mm/memory.c, mm/swap_state.c）：

```[pseudocode]
munmap → unmap_region → unmap_single_vma → unmap_page_range → zap_pte_range
zap_pte_range: 遍历 PTE → ptep_get_and_clear → tlb_remove_page_sync_one
MMU gather: tlb_gather_mmu 开始 → tlb_finish_mmu 结束(TLB flush + 页释放批处理)
free_pages_and_swap_cache → free_swap_cache/put_page → lru_add_drain
TLB 延迟刷新: 批量 flush 减少 IPI → tlb_flush_mmu_tlbonly → flush_tlb_mm_range
```

Why: 为什么用"MMU gather"批量？——**TLB flush 是按页的 IPI**（阶段1-02 篇 shootdown）：munmap 1MB = 256 页，逐页 flush = 256 次 IPI（灾难）。**MMU gather 把"清除 PTE"与"flush TLB"解耦**：先批量清 PTE（gather 记录地址），最后统一 flush_tlb_mm_range（一次 IPI 覆盖整个范围）——**把 256 次 IPI 合并成 1 次**。这是"批量分摊跨核开销"的又一实例（与 PCP batch 同思想）。

比喻锚点: MMU gather=整栋楼断电施工——不用每层楼单独拉闸（逐页 flush 256 次 IPI），而是整栋统一断电一次施工（tlb_finish_mmu 统一 flush）——停电（TLB 失效）一次解决，不用 256 次来回。 [写作时展开]

### 5. 收束

回到"从物理页反查所有映射者"：
- 结构 = anon_vma（fork 家族共享）+ anon_vma_chain（VMA 连接）
- 遍历 = rmap_walk（区间树按 pgoff 精确）
- 操作 = try_to_unmap_one（匿名 swap entry / 文件清除）
- 批量 = munmap MMU gather（256 次 IPI → 1 次）

**Aha Moment**: "反向映射把'页→映射者'的多对多关系压缩成'页→fork 家族'——每个物理页只需一个 anon_vma 指针，反查时沿家族遍历所有 VMA。而 try_to_unmap_one 的两条路径揭示了本质：匿名页是'搬家'（PTE 换成 swap entry 记住新址），文件页是'退房'（直接清除，缺页重读）。"
**回答读者三问**: ①怎么找所有映射者=anon_vma 家族遍历；②换出后 PTE 是什么=swap entry；③munmap 为何快=MMU gather 批量 flush。

---

### 核心悬念

**"回收路径怎么判断该回收匿名页还是文件页？LRU 四链表(inactive/active anon/file) 的状态转换规则是什么？6.1 的 MGLRU 怎么革命性地替代传统链表？"**

→ 引出 09 LRU 与回收 — kswapd 后台回收 + Direct Reclaim + MGLRU——反查映射者是为了"换出"做准备，下一篇讲"怎么决定换谁"。
