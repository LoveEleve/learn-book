# 页缓存 — xarray、预读与用户态控制

> Cluster D: 6 KPs | 依赖: 05-page-fault-scenarios | 读者基线: 文件缺页 #PF 处理流程
> 读者处境: 已读 05 篇（文件缺页从页缓存取数据）；本篇回答"页缓存本身怎么组织（xarray）、怎么预读、用户态怎么控制"
> 打开新视角: xarray 替代 radix tree 的动机、预读状态机的倍增/缩小、fault-around 的顺带填充、Direct I/O 与 madvise 的用户态控制

---

### 概念依赖链

```
05-文件缺页(从页缓存取) → 本篇: 页缓存本体
  ├─ §1 address_space + xarray(索引核心 — 依赖 05)
  │    ├─ §2 Readahead(预读状态机 — 依赖 §1)
  │    │    └─ §3 Fault-Around(缺页顺带填充 — 依赖 §2)
  │    ├─ §4 Direct I/O(绕过页缓存 — 对照 §1)
  │    └─ §5 mlock/mprotect/madvise(用户态控制 — 依赖 §2 预读)
先讲: 索引(xarray) → 预读(状态机) → 顺带(fault-around) → 绕过(Direct I/O) → 控制(madvise)
后续依赖: 07-Writeback(脏页写回)
```

### 叙事顺序

1. 问题引入——读文件"第一次慢之后快"（阶段1-04 篇）——缓存页存在哪？怎么按偏移快速找？（**Aha: 页缓存的索引从 radix tree 换成了 xarray——4.20+ 的'数据结构升级'**）
   - 过渡: 索引有了——什么时候预读？
2. Readahead——ondemand 状态机；初始 4 页倍增/缩小
   - 过渡: 缺页时能不能"顺带"多映射几页？
3. Fault-Around——缺页时顺带填充邻页
   - 过渡: 有时不想走页缓存——Direct I/O
4. Direct I/O——绕过页缓存直通块层
   - 过渡: 用户态怎么"告诉"内核预读/回收策略？
5. mlock/mprotect/madvise——用户态控制三元组
   - 过渡: 完整图景已齐——收束
6. 收束——索引/预读/填充/绕过/控制的完整链条

### 1. address_space 与 xarray — 页缓存的索引核心

场景提示: 页缓存里成千上万页——怎么按"文件偏移"快速找到对应页？ [写作时展开]

关键设计: `struct address_space` + `xarray`（include/linux/fs.h, lib/xarray.c）： [内核: 阶段1-04 篇页缓存概念→本篇源码——xarray 是 address_space 的索引落地]

```[pseudocode]
struct address_space: host(inode) + i_pages(xarray) + nrpages + writeback_index + a_ops
xarray API: XA_STATE(xas, &mapping->i_pages, idx) 遍历状态
  → xas_load(查找) / xas_store(存储) / xas_find(范围) / xas_for_each(遍历)
  → xas_set_order(多阶索引) / xa_lock_irq(写锁)
RCU 读: rcu_read_lock → xas_load → rcu_read_unlock — 无锁查找
```

设计动机（替代 radix tree）: radix tree 需要额外 slot 数组管理 + 每节点独立分配（内存碎片）；xarray **将 slot/tag/value 内联到单条目**——内存密度更高 + RCU 无锁读天然并发友好。

Why: 为什么 xarray 比 radix tree 好？——**两点**：内存密度（radix tree 每层节点独立分配，碎片多；xarray 单条目内联，紧凑）+ **并发**（RCU 无锁读：读者不用锁，写者用 xa_lock——读路径零开销，写路径单锁）。页缓存读是最热路径（每次文件读都要查），**RCU 读让"查页缓存"从"加锁查"变成"无锁查"**——这与阶段1-08 篇 RCU 的"读者零开销"一脉相承。

比喻锚点: xarray=升级版索引卡片柜——radix tree 是"每层卡片柜单独买"（每节点独立分配，柜子多占地方）；xarray 是"一张卡片柜全内联"（单条目紧凑）——而且查卡（读）不用锁柜子（RCU），直接拉开抽屉看（无锁读）。 [写作时展开]

### 2. Readahead 预读算法 — ondemand_readahead 的状态机

场景提示: 顺序读文件——内核怎么"猜"你接下来要读哪几页？猜错怎么办？ [写作时展开]

关键设计: 预读状态机（mm/readahead.c）：

```
触发: 缺页→do_sync_mmap_readahead(同步) / 顺序访问→do_async_mmap_readahead(异步)
struct file_ra_state: start(窗口开始) + size(窗口/页) + async_size(异步部分)
ondemand_readahead:
  初始窗口 → 4 页
  顺序访问 → ra->size *= 2(倍增) — 最大 VM_READAHEAD_PAGES(默认 512KB=128 页)
  随机访问 → ra->size /= 2(缩小)
提交: ra_submit → read_pages → a_ops->readahead → mpage_readahead
  → bio_alloc → bio_add_folio → submit_bio(启动 IO)
控制: fadvise(POSIX_FADV_SEQUENTIAL/RANDOM)
```

Why: 为什么预读是"状态机"而非"固定窗口"？——**访问模式会变**：顺序读（可信信号）时倍增窗口（读 4 页→8→16...，越多越赚）；随机读（不可信）时缩小（猜错代价大）。**倍增是"试探-确认"策略**：小窗口起步，确认顺序后激进扩张，发现随机立刻收缩——用"窗口自适应"匹配"访问模式"。

比喻锚点: 预读状态机=自助餐"先尝后加"——先小盘试 4 样（初始 4 页），发现你顺着吃（顺序读）就整盘整盘上（倍增）；发现你乱拿（随机读）就减量（缩小）——厨房（内核）用"你吃多少来判断你要什么"。 [写作时展开]

### 3. Fault-Around — 缺页时顺带填充邻页

场景提示: 一次缺页只映射 1 页——顺序读时每页都缺一次？ [写作时展开]

关键设计: 缺页时顺带映射邻页（mm/memory.c, mm/filemap.c）：

```[pseudocode]
do_fault_around(vmf, start_pgoff): 计算前后范围
  默认 fault_around_bytes/PAGE_SIZE = 16 页(前后共 32 页)
filemap_map_pages: find_get_page(页缓存查找) → 存在 → alloc_set_pte(建立映射)
仅文件缺页触发(vma->vm_ops->map_pages) — 匿名缺页不适用
```

Why: 为什么"顺带填充"能省缺页？——一次 #PF 处理有固定开销（异常入口+查找）；**fault-around 让一次缺页处理"顺带映射周围 32 页"**——顺序读时这 32 页接下来大概率都要访问，直接免掉 31 次 #PF。**代价是随机读时白映射**（填充了不访问的页）——所以仅文件缺页触发（顺序读为主），且范围可调。

比喻锚点: fault-around=开一扇门顺便开一排——你进走廊第一间（缺页），管理员（内核）顺手把隔壁 31 间的门都打开（顺带映射）——你顺序走（顺序读）就全用上；你乱窜（随机读）就白开了（浪费）。 [写作时展开]

### 4. Direct I/O — 绕过页缓存的直通路径

场景提示: 数据库有自己的缓冲池——不想让内核页缓存"再缓存一份"？ [写作时展开]

关键设计: O_DIRECT 直通（mm/filemap.c, fs/iomap/direct-io.c）：

```[pseudocode]
O_DIRECT → __generic_file_write_iter → generic_file_direct_write
  → invalidate_inode_pages2_range(使无效页缓存)
  → iomap_dio_rw → __iomap_dio_rw → bio_alloc → submit_bio(直接构造 bio)
约束: 块对齐 / 应用自管缓存 / O_DIRECT|O_DSYNC 每次 IO 后 fdatasync
```

Why: 为什么数据库首选 Direct I/O？——**双缓存是浪费**（阶段1-14 篇）：数据库自管 buffer pool（B+tree 预读/LRU 更懂工作负载），内核页缓存是"第二份拷贝"。O_DIRECT 让数据**直接 DMA 到用户 buffer**（跳过页缓存中转）——**内存占用减半 + 一致性免维护**。代价：无预读、无写合并、需应用自己 batch（对齐约束）。

比喻锚点: Direct I/O=自带仓库的餐厅——大厨（数据库）有自己的冷库（buffer pool），不让中央厨房（内核页缓存）再存一份食材（双缓存浪费）——直接送货上门（DMA 到用户 buffer），但自己负责订货（预读）和盘点（对齐）。 [写作时展开]

### 5. mlock/mprotect/madvise — 用户态内存控制三元组

场景提示: 用户态怎么"指挥"内核——锁住页？改权限？提示预读模式？ [写作时展开]

关键设计: 三个控制接口（mm/mlock.c, mprotect.c, madvise.c）：

```[pseudocode]
mlock(addr, len): __mm_populate → get_user_pages 锁物理页
  → vma->vm_flags |= VM_LOCKED → 不可换出
mprotect(addr, len, prot): mprotect_fixup → change_protection
  → walk_page_range → change_pte_range(逐 PTE 改保护位 + TLB flush)
madvise: MADV_SEQUENTIAL(增预读) / MADV_RANDOM(减预读) / MADV_WILLNEED(强制预读)
  / MADV_DONTNEED(zap_page_range 释放) / MADV_FREE(可立即回收)
  / MADV_COLD(移出活跃 LRU) / MADV_PAGEOUT(立即回收) / MADV_HUGEPAGE(khugepaged)
```

Why: 为什么需要"用户态控制"？——**内核的通用策略不一定最优**：内核预读假设"顺序读"（通用）；应用知道自己的模式（顺序/随机/一次读/永不再用）——madvise 让应用**把"我的访问模式"告诉内核**（提示而非命令），内核据此调整预读/回收/大页。**控制与策略分离**：用户态声明意图（hint），内核执行（是否采纳由内核决定）。

比喻锚点: madvise=点餐备注"这份文件怎么吃"——普通点餐（默认策略）厨师按通用做法；备注"我要边吃边看下一道（顺序）"或"这道尝一口就行（随机/一次读）"——厨师（内核）按备注调整上菜节奏（预读/回收），但最终怎么做还是厨师决定。 [写作时展开]

### 6. 收束

回到"页缓存怎么组织"：
- 索引 = xarray（RCU 无锁读）
- 预读 = ondemand 状态机（倍增/缩小）
- 顺带 = fault-around（一次缺页映射 32 页）
- 绕过 = Direct I/O（数据库自管）
- 控制 = madvise 三元组（用户态 hint）

**Aha Moment**: "页缓存的每个设计都在回答'读路径怎么最快'：xarray 让查找无锁（RCU）、预读让 IO 批量化（状态机）、fault-around 让缺页顺带多映射（少 31 次 #PF）、Direct I/O 让数据库绕开双缓存、madvise 让应用告诉内核'我的模式'——**五层优化，层层递进，全部服务于'减少延迟'**。"
**回答读者三问**: ①页缓存怎么找页=xarray RCU 无锁；②预读怎么猜=状态机倍增/缩小；③数据库为何绕过=Direct I/O 自管缓冲。

---

### 核心悬念

**"脏页标记后谁来写回？Flusher 线程的 per-backing-device 模型 + pid 控制器 Dirty Throttling 限流怎么运行的？"**

→ 引出 07 Writeback — 脏页标记 + Flusher 线程 + Dirty Throttling——缓存怎么"进"讲完了，下一篇讲怎么"出"（落盘）。
