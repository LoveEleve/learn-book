# Swap — 匿名页换出换入的完整闭环

> Cluster C: 4 KPs | 依赖: 09-lru-kswapd-reclaim | 读者基线: LRU 回收 + kswapd + Direct Reclaim
> 读者处境: 已读 09 篇（回收决定"换出匿名页"）；本篇回答"换去哪（swap entry 编码）、怎么换回（do_swap_page）、怎么防碎片（cluster）"
> 打开新视角: swap entry 的 64 位编码、Swap Cache 的加速、cluster 分配策略、批量回收的锁优化

---

### 概念依赖链

```
09-回收(决定换出) + 08-反向映射(解除映射) → 本篇: Swap 闭环
  ├─ §1 换出路径(add_to_swap → 磁盘写 — 依赖 08 try_to_unmap)
  │    └─ §2 换入路径(do_swap_page → 磁盘读 — 依赖 §1 的 entry)
  │         ├─ §3 碎片化处理(cluster 分配 — 依赖 §1)
  │         └─ §4 Folio Batches(批量回收 — 依赖 09)
先讲: 出(换出) → 入(换入) → 防碎(cluster) → 批量(batches)
后续依赖: 11-Overcommit 与 OOM
```

### 叙事顺序

1. 问题引入——匿名页被回收——它的数据"放哪"？下次访问怎么"找回"？（**Aha: swap 是匿名页的'仓库'——PTE 从'物理帧号'变成'仓库坐标'**）
   - 过渡: 仓库坐标怎么编码？——swap entry
2. 换出路径——add_to_swap → swap entry → 磁盘写
   - 过渡: 访问换出的页——怎么换回？
3. 换入路径——do_swap_page → Swap Cache 命中/未命中
   - 过渡: 磁盘是慢的——怎么减少随机读？
4. 碎片化处理——cluster 分配 + swapin_readahead
   - 过渡: 批量操作怎么减少锁？
5. Folio Batches——SWAP_CLUSTER_MAX 批量
   - 过渡: 完整图景已齐——收束
6. 收束——出→入→防碎→批量的完整闭环

### 1. Swap 换出路径 — add_to_swap 到磁盘写入

场景提示: 回收选中一个匿名页——它的数据怎么"存到仓库"？ [写作时展开]

关键设计: 换出链（mm/vmscan.c, mm/swapfile.c, mm/page_io.c）： [内核: 阶段1-04 篇 swap 概念→本篇源码——entry 编码与 do_swap_page 是 04 篇的落地]

```[pseudocode]
shrink_folio_list → add_to_swap(folio)
  → get_swap_page 分配 swap slot → swp_entry(val) 构造 swap entry
  → __swap_writepage → swap_writepage → bdev_write_page(直接写块设备)
换出后 PTE 编码: set_pte_at(swp_entry_to_pte(entry))
  → PTE 低 12 位=0(Present=0) → 高 48 位 = (type<<SWP_TYPE_SHIFT)|(offset<<SWP_OFFSET_SHIFT)
swap_info_struct → swap_map[offset]: 0=free / SWAP_HAS_CACHE=1 / SWAP_MAP_MAX=最大引用
```

Why: 为什么 swap entry 能"塞进 PTE"？——**PTE 的 Present=0 时硬件不看其他位**（阶段1-02 篇）：换出前 PTE 是"物理帧号"，换出后**低 12 位清零（Present=0）+ 高 48 位改编码"仓库坐标"**——type（哪个 swap 设备）+ offset（设备内第几个 slot）。硬件看到 Present=0 触发缺页，**内核读 PTE 的编码**知道去哪读回——**一个 PTE 两种用途：物理帧号（在内存）vs 仓库坐标（在 swap）**。

比喻锚点: swap entry=储物间钥匙牌——房间（PTE）原来挂"住址"（物理帧号），搬去仓库后换成"仓库钥匙牌"（type+offset 编码）；保安（硬件）见"无人"（Present=0）就叫管理员（缺页），管理员看钥匙牌去仓库取货。 [写作时展开]

### 2. Swap 换入路径 — do_swap_page 的完整恢复

场景提示: 访问一个换出的页——缺页处理器怎么知道"它在仓库"并取回？ [写作时展开]

关键设计: 换入链（mm/memory.c, mm/swap_state.c）：

```[pseudocode]
访问 → PTE Present=0 但非零 → is_swap_pte(pte) 检测 → do_swap_page(vmf)
Swap Cache Hit: lookup_swap_cache(entry) → swap address_space 的 i_pages 查找
  → folio 已在(别人刚换入) → 直接 set_pte 映射(免 IO)
Swap Cache Miss: swapin_readahead(entry) → swap_cluster_readahead 预读周围 slots
  → read_swap_cache_async → swap_readpage → swap_readpage_fs → bdev_read_page
  → 中断 → bio → end_swap_bio_read → folio_end_read(uptodate)
  → swap_free(entry)(减 slot ref) → set_pte_at(建立映射)
Swap Cache 生命周期: add_to_swap_cache → SetPageSwapCache → 换入完成 → delete_from_swap_cache
```

Why: 为什么有"Swap Cache 命中"？——**换入与换出可能并发**（进程 A 换出、进程 B 同时访问）：swap cache 是"换出页的临时缓存"（在换入完成前保留在内存）——**命中时直接映射（免 IO）**，未命中才读盘。且换入时 `swapin_readahead` **预读周围 slots**（换出是顺序写、换入大概率也顺序访问）——把"单页随机读"变成"多页顺序读"。

比喻锚点: Swap Cache=仓库取货顺带多取——你要 3 号柜的东西（换入），仓库管理员发现 3 号柜的东西**正好在门口待取区**（Swap Cache 命中：别人刚换出还没送走）——直接拿走（免 IO）；否则按单取货，顺便把 4/5 号柜的一起取（swapin_readahead 预读）。 [写作时展开]

### 3. Swap 碎片化处理 — cluster 分配策略

场景提示: 换出页分散在 swap 设备各处——换入时每页都随机读？ [写作时展开]

关键设计: cluster 连续分配（mm/swapfile.c）：

```[pseudocode]
swap_info_struct → cluster_info → cluster_next(下次分配指针)
scan_swap_map_slots: 设备级扫描 → cluster_is_free 找空闲 cluster
  → 每 cluster = SWAPFILE_CLUSTER(256) 个 slots
SSD 快速路径: get_swap_pages → scan_swap_map_try_ssd_cluster
Swap priority: swapon -p PRIORITY → highest_priority 优先 → swap_list->next 轮询
swapoff: try_to_unuse → unuse_pte_range 遍历所有进程页表重新映射 → 迁移完关闭
```

Why: 为什么按"簇（256 slots）"分配？——**换入的随机读代价**（阶段1-04 篇）：页散落各处 → 换入时每页随机寻道（HDD 灾难）；**连续分配（cluster）让换出页聚在一起 → 换入时可顺序/预读**。这是"分配策略服务访问模式"：**写时连续（cluster），读时可预读（swapin_readahead）**——两个机制配合让 swap 的随机访问变顺序。

比喻锚点: cluster 分配=仓库按区存放——换出的货按"整排"入库（cluster 连续 256 格），取货时（换入）一次搬一排（顺序）；乱放（分散分配）取货要满仓库跑（随机寻道）。 [写作时展开]

### 4. Folio Batches 批量回收 — 减少锁竞争

场景提示: 一次回收 1000 页——每页操作一次 lru_lock？ [写作时展开]

关键设计: 批量回收（mm/vmscan.c, mm/swap_state.c）：

```[pseudocode]
shrink_folio_list → folio_list 批量处理 → free_unref_page_list → PCP 回灌
rotate_reclaimable_folios → folio_rotate_reclaimable → lru_add_drain → lru_add_fn
batch_size = SWAP_CLUSTER_MAX(32) 个 folio — 减少 zone→lru_lock 获取 + TLB shootdown 累积
pageout 流程: shrink_folio_list → pageout()/pageout_writeback → submit_bio → PG_writeback
```

Why: 为什么批量=32？——**锁与跨核开销的分摊**（阶段1-02 篇 shootdown）：每页单独操作 = 每页拿一次 lru_lock（竞争）+ 每页触发 TLB flush（IPI）；**批量 32 页 = 一次锁 + 累积 flush**——锁竞争降 32 倍、IPI 合并。这是内核"批量分摊固定开销"模式的又一实例（与 PCP batch/MMU gather 同思想）。

比喻锚点: 批量回收=打包扔垃圾——每件垃圾单独下楼扔（每页单独锁+flush）累死；打包 32 件一次扔（批量）——下楼次数（锁）和通知邻居次数（IPI）都少 32 倍。 [写作时展开]

### 5. 收束

回到"匿名页的仓库闭环"：
- 出 = add_to_swap（entry 编码：type+offset 塞进 PTE）
- 入 = do_swap_page（Swap Cache 命中免 IO）
- 防碎 = cluster 分配（连续写 → 可预读）
- 批量 = SWAP_CLUSTER_MAX（锁与 IPI 分摊）

**Aha Moment**: "swap 的优雅在于'复用 PTE'：Present=0 的 PTE 从'物理帧号'变成'仓库坐标'（type+offset）——硬件不管（触发缺页），内核会读（do_swap_page 按坐标取回）。而 cluster 连续分配 + swapin_readahead 预读，把 swap 的'随机读'硬生生改造成'顺序读'——**存储系统的性能密码永远是'让随机变顺序'**。"
**回答读者三问**: ①换出页数据在哪=swap entry 编码（type+offset）；②换入快不快=Swap Cache 命中免 IO；③swap 随机读怎么办=cluster 连续 + 预读。

---

### 核心悬念

**"所有回收手段都失败了，内存用尽 — Overcommit 三策略 + OOM Killer 怎么选进程？oom_badness 评分公式是什么？OOM Reaper 怎么收割？"**

→ 引出 11 Overcommit 与 OOM — select_bad_process + oom_badness + OOM Reaper——swap 是"最后一层仓库"，仓库也满了就是 OOM 战场。
