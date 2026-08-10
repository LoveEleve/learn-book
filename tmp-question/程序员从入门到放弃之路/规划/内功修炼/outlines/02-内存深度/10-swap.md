# Swap — 匿名页换出换入的完整闭环

> Cluster C: 4 KPs | 依赖: 09-lru-kswapd-reclaim | 读者基线: LRU 回收 + kswapd + Direct Reclaim

---

### 1. Swap 换出路径 — add_to_swap 到磁盘写入
  - 匿名页回收 → `shrink_folio_list → add_to_swap(folio)` (mm/vmscan.c → shrink_folio_list)
  - `get_swap_page` 分配 swap slot → `swp_entry(val)` 构造 swap entry: `type`(swap 设备索引) + `offset`(分区内 slot 偏移) (mm/swapfile.c → get_swap_page)
  - `__swap_writepage(page, wbc) → swap_writepage → bdev_write_page(bdev, sector, page)` 直接写块设备 (mm/page_io.c → swap_writepage)
  - **换出后 PTE 编码**: `set_pte_at(mm, addr, pte, swp_entry_to_pte(entry))` → PTE 低 12 位=0(Present=0) → 高 48 位 = `(type<<SWP_TYPE_SHIFT)|(offset<<SWP_OFFSET_SHIFT)` (mm/swapfile.c → swp_entry_to_pte)
  - `swap_info_struct → swap_map[offset]`: 0=free, SWAP_HAS_CACHE=1(有 swap cache 无映射), SWAP_MAP_MAX=最大引用 (include/linux/swap.h → struct swap_info_struct)
  - `__swap_count(entry)` 读 slot 引用计数, `swap_slot_free_notify` 通知 SSD 可 trim (mm/swapfile.c → __swap_count)

### 2. Swap 换入路径 — do_swap_page 的完整恢复
  - 访问 swap 出的页 → PTE Present=0 但非零 → `is_swap_pte(pte)` 检测 → `do_swap_page(vmf)` (mm/memory.c:3790 → do_swap_page)
  - **Swap Cache Hit**: `lookup_swap_cache(entry)` → swap address_space 的 i_pages 查找 → folio 已在 → 直接 `pte_unmap_unlock + set_pte` 映射 (mm/swap_state.c → lookup_swap_cache)
  - **Swap Cache Miss**: `swapin_readahead(entry, gfp, vmf) → swap_cluster_readahead` 预读周围 swap slots (mm/swap_state.c → swapin_readahead)
  - `read_swap_cache_async(entry, gfp, vma, addr)` → `swap_readpage → swap_readpage_fs → bdev_read_page` 从 swap 分区读 (mm/swap_state.c → read_swap_cache_async)
  - 中断 → bio → end_io → `end_swap_bio_read → folio_end_read` 设置 uptodate → `swap_free(entry)` 减 slot ref → `set_pte_at` 建立用户页表映射
  - **Swap Cache 生命周期**: `add_to_swap_cache` → `SetPageSwapCache` → 换入完成 → `delete_from_swap_cache → swapcache_free` (mm/swap_state.c → add_to_swap_cache)

### 3. Swap 碎片化处理 — cluster 分配策略
  - `swap_info_struct → cluster_info → cluster_next` 下次分配指针 (mm/swapfile.c → struct swap_info_struct cluster)
  - `scan_swap_map_slots` 设备级扫描 → `cluster_is_free` 找空闲 cluster → 每 cluster=SWAPFILE_CLUSTER(256) 个 slots (mm/swapfile.c → scan_swap_map_slots)
  - cluster 分配减少碎片 → `get_swap_pages` → `scan_swap_map_try_ssd_cluster` SSD 快速路径 (mm/swapfile.c → get_swap_pages)
  - Swap priority: `swapon -p PRIORITY` → `highest_priority` 优先分配 → `swap_list→next` 轮询
  - swapoff: `try_to_unuse → unuse_pte → unuse_pte_range` 遍历所有进程页表重新映射回物理页 → 迁移完 → 关闭 swap 文件/分区 (mm/swapfile.c → try_to_unuse)

### 4. Folio Batches 批量回收 — 减少锁竞争
  - `shrink_folio_list → folio_list` 批量处理 → `free_unref_page_list → free_unref_page_commit` PCP 回灌 (mm/vmscan.c → shrink_folio_list)
  - `rotate_reclaimable_folios → folio_rotate_reclaimable → lru_add_drain → lru_add_fn` 批量操作 (mm/vmscan.c → rotate_reclaimable_folios)
  - 每次 `batch_size = SWAP_CLUSTER_MAX`(32) 个 folio → 减少 zone→lru_lock 获取次数 + TLB shootdown 累积
  - `shrink_folio_list → pageout()/pageout_writeback` → `VM_BUG_ON_PAGE(PageWriteback(page))` → `submit_bio` 后进入 PG_writeback (mm/vmscan.c → pageout)

### 5. 收束
  - swap entry 64 位编码: type(swap 设备索引)+offset(slot 偏移), 换出后 PTE 从物理帧号变为 swap 类型编码
  - Swap Cache 加速换入: 命中时直接映射(免 IO), 未命中时 `swapin_readahead` 预读周围 slots + `bdev_read_page`
  - cluster 分配(256 slots/簇)减少碎片, swapoff 全量 `try_to_unuse` 遍历所有进程页表迁回物理页

---

### 核心悬念
**"所有回收手段都失败了，内存用尽 — Overcommit 三策略 + OOM Killer 怎么选进程？oom_badness 评分公式是什么？OOM Reaper 怎么收割？"**

→ 引出 11 Overcommit 与 OOM — select_bad_process + oom_badness + OOM Reaper
