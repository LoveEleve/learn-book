# 缺页异常四场景 — #PF 来了之后内核做什么

> Cluster B: 6 KPs | 依赖: 04-mmstruct-vma | 读者基线: mm_struct + VMA 建立流程
> 读者处境: 已读 04 篇（VMA 地图）；本篇回答"地图上的地址第一次被访问——四种场景（匿名/COW/文件/栈）内核分别怎么兑现？"
> 打开新视角: 零页共享的只读优化、COW 的 page_count 判定与 reuse 优化、文件缺页三分支、GUP 的 pin 语义

---

### 概念依赖链

```
04-VMA(地图) + 02-Buddy(物理页) + 03-COW 概念 → 本篇: 缺页兑现
  ├─ §1 匿名缺页(do_anonymous_page — 依赖 02 Buddy)
  │    ├─ §2 COW 缺页(do_wp_page — 依赖 03 PTE RW + 01 refcount)
  │    ├─ §3 文件缺页(do_fault 三分支 — 依赖 06 页缓存预告)
  │    └─ §4 栈扩展 + GUP(expand_stack + get_user_pages)
先讲: 匿名(零页) → COW(复制) → 文件(缓存) → 栈/GUP(边界)
后续依赖: 06-页缓存(xarray/预读)
```

### 叙事顺序

1. 问题引入——mmap 返回了，第一次访问就崩溃？不——触发 #PF——内核怎么判断"该分配"还是"该报错"？（**Aha: 缺页不是错误，是'兑现承诺'的机制——按 VMA 类型分派到四条路径**）
   - 过渡: 匿名映射的第一次访问？
2. 匿名缺页——只读映射零页 / 写分配零页
   - 过渡: fork 后的写访问呢？——COW
3. COW 缺页——page_count>1 复制 / =1 直接 reuse
   - 过渡: 文件映射呢？——三分支
4. 文件缺页——SHARED 写/PRIVATE 写/PRIVATE 读
   - 过渡: 边界场景——栈扩展/GUP
5. 栈扩展 + GUP——expand_stack 自动增长；get_user_pages pin
   - 过渡: 完整图景已齐——收束
6. 收束——四场景分治总览

### 1. 匿名缺页 do_anonymous_page — 首次访问时的零页分配

场景提示: `mmap(MAP_ANONYMOUS)` 返回了——第一次访问这个地址，内核怎么处理？ [写作时展开]

关键设计: 触发链 `do_user_addr_fault → handle_mm_fault → handle_pte_fault`，匿名页两条分支（mm/memory.c）： [内核: 阶段1-03 篇缺页概念→本篇四场景源码——handle_pte_fault 按 PTE/VMA 分派]

```[pseudocode]
只读访问: do_anonymous_page → pte_alloc → 映射 zero page
  (my_zero_pfn 全零共享页, _PAGE_SPECIAL|PFN=zero_pfn, 只读 — 多个进程共享同一零页)
写访问: alloc_zeroed_user_highpage_movable(gfp) → Buddy 分配零页
  → pte_mkwrite 可写 → __SetPageUptodate
后续: page_add_new_anon_rmap(匿名反向映射) → lru_cache_add(LRU) → set_pte_at
首次缺页延迟: ~1-2 微秒(Buddy + zeroing + PTE)
```

Why: 为什么"只读访问"不分配页？——**零页共享**：读一个零页，内容永远是零——**所有进程的只读零访问映射到同一个物理零页**（my_zero_pfn），直到有人写才 COW 裂开（§2）。这是"按需分配"的极致：**连零页都不每人一份**。写访问才真正分配（Buddy 零页）——因为"每个进程要自己的零页"（写私有）。

比喻锚点: 匿名只读=公用白纸——大家只看（读零页）就共用一张白纸（my_zero_pfn），谁要写（写访问）才各自领一张新纸（Buddy 分配）——"看"不占资源，"写"才占。 [写作时展开]

### 2. COW 缺页 do_wp_page — fork 后的写时复制

场景提示: fork 后父子共享物理页（阶段1-03 篇）——子进程写，发生什么？ [写作时展开]

关键设计: fork 后所有可写页 `_PAGE_RW=0` → 子写 → #PF(write=1, present=1) → `do_wp_page`（mm/memory.c）：

```[pseudocode]
判定: page_count(page) > 1(多次共享) → 需要 COW
复制: wp_page_copy → alloc_page_vma(GFP_HIGHUSER_MOVABLE) 分配新页
  → cow_user_page(kmap_atomic 拷贝) → page_remove_rmap(旧页减映射)
  → page_add_new_anon_rmap(新页) → lru_cache_add
  → ptep_clear_flush(清旧 PTE+TLB) → set_pte_at_notify(新 PTE 可写+新页)
优化: reuse_old_page — page_count=1(仅当前进程映射)
  → 直接 pte_mkwrite 跳过拷贝
```

Why: 为什么 `page_count=1` 可以跳过拷贝？——COW 的前提是"多进程共享"；**page_count=1 意味着只有当前进程映射这页**（其他进程已各自复制或释放）——写它不会影响别人，直接改 PTE 可写即可（reuse_old_page）。**共享才需要复制，独享直接解锁**——这是 COW 的"最后一次复制"优化：共享链上最后一个用户免拷贝。

比喻锚点: COW=合租房的最后一户——书（物理页）是合租的（多进程共享），谁要改就得自己买一本（复制）；但**只剩你一户在住**（page_count=1）——书就是你自己的了，直接划线（改 PTE 可写）不用再买。 [写作时展开]

### 3. 文件缺页 do_fault — File-Backed 映射的三条分支

场景提示: `mmap` 一个文件——第一次读/写这个地址，和匿名页有什么不同？ [写作时展开]

关键设计: 文件映射三分支（mm/memory.c）：

```[pseudocode]
MAP_SHARED 写: do_shared_fault → __do_fault → vm_ops→fault → filemap_fault
  → do_sync_mmap_readahead 预读 → __filemap_get_folio
  → 未命中 → readpage → submit_bio → IO → finish_fault(映射页缓存页)
MAP_PRIVATE 写: do_cow_fault → __do_fault 读原页 → do_wp_page COW 分支
  → 写不传播到文件(私有副本)
MAP_PRIVATE 读: do_read_fault → __do_fault → finish_fault → 映射页缓存页(只读)
```

Why: 为什么文件缺页要分三条？——**三种语义**：SHARED 写 = "改文件"（写到页缓存，回写落盘）；PRIVATE 写 = "改我的副本"（COW，不影响文件）；PRIVATE 读 = "看文件"（只读映射页缓存页）。**同一地址空间、同一文件，三种访问模式三种路径**——语义差异在 mmap 的 flags 就定下了（MAP_SHARED vs MAP_PRIVATE），缺页时按 VMA 的 flag 分派。

比喻锚点: 文件缺页三分支=借书三种方式——SHARED 写是"在公共书上做笔记"（写回书=文件）；PRIVATE 写是"抄一份再划"（COW 副本，不动原书）；PRIVATE 读是"只翻看"（只读映射）——借书时的约定（MAP_SHARED/PRIVATE）决定了还书时（缺页）怎么处理。 [写作时展开]

### 4. 栈扩展与 GUP — 原子访问与栈自动增长

场景提示: 递归函数栈溢出前——栈会"自动长"？内核怎么让用户态访问"往下"的区域？ [写作时展开]

关键设计: 两条边界路径：

```[pseudocode]
栈扩展: 访问栈下方未映射区域 → #PF → expand_stack(vma, addr)
  → 检查 vma→vm_start - stack_guard_gap - PAGE_SIZE + RLIMIT_STACK
  → vma→vm_start -= PAGE_SIZE × grow
  → 保护页(guard page)触发 #PF → SIGSEGV 而非扩展
GUP(Get User Pages): 内核访问用户地址(O_DIRECT/splice)
  → get_user_pages → __get_user_pages → find_vma → follow_page_mask → try_grab_folio
  → flags: FOLL_WRITE/FOLL_FORCE/FOLL_PIN/FOLL_LONGTERM
  → pin 过多引发内存压力; FOLL_LONGTERM 与 MOVABLE 冲突 → CMA 区域 preferred
```

Why: 为什么栈要"自动增长"？——**栈大小未知**（递归深度/局部变量取决于运行路径）；为"可能用到的栈"预分配会浪费（1MB 起）+ 地址空间限制。**按需增长**（访问到哪长到哪）+ **guard page 防失控**（长过界报 SIGSEGV）——与 mmap 的"承诺 vs 兑现"同一哲学。GUP 则是"内核借用户页"（O_DIRECT 直接 DMA 用户 buffer）——需要 pin（防页被回收/移动），FOLL_LONGTERM 长时间 pin 会阻碍内存回收。

比喻锚点: 栈扩展=走廊自动加长——住户（程序）往走廊尽头走，物业（内核）就加长走廊（expand_stack）；但走廊尽头有警示线（guard page）——越过就报警（SIGSEGV）不是继续加长。 [写作时展开]

### 5. 收束

回到"第一次访问 VMA 地址"：
- 匿名 = 零页共享（读）/ Buddy 零页（写）
- COW = page_count 判定 + reuse 优化
- 文件 = SHARED 写/PRIVATE 写/PRIVATE 读三分支
- 栈/GUP = 自动增长 + 内核借页

**Aha Moment**: "缺页不是'错误处理'，是'承诺兑现机制'——按 VMA 类型分派四路：匿名读共享零页（连零页都省）、COW 按 page_count 决定复制还是解锁、文件缺页走页缓存预读、栈按需自动增长。**内核把'第一次访问'设计成了最优化路径**——每种场景都有专属优化。"
**回答读者三问**: ①匿名第一次读为何快=共享零页不分配；②COW 何时免拷贝=page_count=1 直接 reuse；③文件写私有为何不落盘=COW 副本隔离。

---

### 核心悬念

**"文件页缺页时数据从磁盘读到页缓存 — address_space 的 xarray 怎么组织？预读算法怎么猜中下一次访问？"**

→ 引出 06 页缓存 — xarray + Readahead + Fault-Around + Direct I/O——缺页从页缓存取数据，下一篇讲缓存本身怎么组织。
