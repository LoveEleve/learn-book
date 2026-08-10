# LRU 与回收 — kswapd 后台回收与 Direct Reclaim

> Cluster C: 4 KPs | 依赖: 08-reverse-mapping | 读者基线: 反向映射 anon_vma + try_to_unmap_one

---

### 1. LRU 四链表传统模型 — inactive/active anon/file 的状态机
  - 设计考量: 传统 4-list 需要显式 `folio_activate/deactivate` 操作维护页在链表中的位置, 频繁 `list_move` 导致 lru_lock 争用; MGLRU 用代标识+时间戳无需频繁移动, 减少锁竞争 — 这在多核大规模内存场景下改进最明显
  - `struct lruvec`: `lists[NR_LRU_LISTS]` — LRU_INACTIVE_ANON=0, LRU_ACTIVE_ANON=1, LRU_INACTIVE_FILE=2, LRU_ACTIVE_FILE=3, LRU_UNEVICTABLE=4 (include/linux/mmzone.h → struct lruvec)
  - 激活路径: `folio_mark_accessed` → 首次访问放 inactive → 二次访问 `folio_referenced` → `folio_activate` 升 active (mm/swap.c → folio_mark_accessed)
  - 降级路径: `folio_deactivate` → 长时间未访问从 active 移到 inactive → `lru_move_tail_fn` (mm/swap.c → folio_deactivate)
  - 回收从 inactive tail 开始: `shrink_inactive_list → isolate_lru_folios` → 回收失败 → `lru_add_fn` 移到 active(二次机会) (mm/vmscan.c → shrink_inactive_list)
  - **MGLRU**(6.1+): `lru_gen_folio` 多代模型 → 每代有时间戳 → 回收最老代 → `lru_gen_del_folio/lru_gen_add_folio` → `/sys/kernel/mm/lru_gen/enabled` 开关 (mm/vmscan.c → lru_gen_folio)

### 2. kswapd 后台回收 — per-NUMA-node 的守护者
  - `kswapd_run(nid) → kthread_run(kswapd, pgdat, "kswapd%d", nid)` — 每个 NUMA node 一个 kswapd (mm/vmscan.c → kswapd_run)
  - `kswapd → balance_pgdat(pgdat, order, highest_zoneidx)`: 检查每个 zone 的水位 → 低于 low → `kswapd_shrink_node → shrink_node` (mm/vmscan.c:6800 → balance_pgdat)
  - `shrink_node → shrink_node_memcgs` 每个 memory cgroup → `shrink_lruvec` (mm/vmscan.c → shrink_node)
  - `get_scan_count(lruvec, sc, nr)` 确定 anon/file 扫描比例(受 swappiness 影响) → `shrink_inactive_list → isolate_lru_folios → shrink_folio_list` (mm/vmscan.c → get_scan_count)
  - 达标检查: `pgdat_balanced` 各 zone 水位达 high → kswapd→`kswapd_try_to_sleep → schedule_timeout` 睡眠 (mm/vmscan.c → pgdat_balanced)

### 3. 直接回收 Direct Reclaim — 分配路径上的同步回收
  - 触发: Buddy 分配失败 → `__alloc_pages_direct_reclaim(gfp_mask, order)`(mm/page_alloc.c:4230 → __alloc_pages_direct_reclaim)
  - 条件检查: `gfpflags_allow_blocking(gfp_mask)` 允许阻塞 → `__perform_reclaim → try_to_free_pages(zonelist, order, gfp_mask)` (mm/page_alloc.c → __perform_reclaim)
  - `do_try_to_free_pages → shrink_zones → shrink_node → shrink_lruvec` — 在分配路径上阻塞当前任务 (mm/vmscan.c → do_try_to_free_pages)
  - 优先级递进: `sc.priority = DEF_PRIORITY(12)` → 下降到 0 → `sc.may_writepage`(允许脏页写回)→`sc.may_unmap`→`sc.may_swap` 越来越激进
  - 成功 → `did_some_progress=1` → 分配成功; 仍失败 → `__alloc_pages_may_oom → out_of_memory → oom_kill_process`
  - 无 `__GFP_DIRECT_RECLAIM` → 直接返回 NULL → 上层处理分配失败

### 4. 回收策略与调优 — anon vs file 的权衡
  - `swappiness`(0-200, 默认 60): 0=尽量回收文件页避 swap, 100=平等对待, 200=积极 swap (mm/vmscan.c → get_scan_count)
  - `vfs_cache_pressure`(100): 越低越保留 dentry/inode 缓存 (fs/dcache.c → shrink_dcache_memory)
  - `sc.may_writepage` 控制脏文件页写回 → `pageout()` 分发: 匿名页→`swap_writepage`, 文件页→`writepage` (mm/vmscan.c → pageout)
  - `shrink_folio_list` 每批 `SWAP_CLUSTER_MAX`(32) 个 folio → 减少 zone→lru_lock 获取次数 (mm/vmscan.c → shrink_folio_list)

### 5. 收束
  - 传统 LRU 四链表 inactive/active × anon/file 状态机 → 二次访问升 active → 长时间未访问降 inactive → 回收从 inactive tail 开始
  - kswapd per-NUMA-node 后台回收: 水位低于 low→唤醒 `shrink_node` → 水位达 high→睡眠, Direct Reclaim 在分配路径上同步阻塞
  - MGLRU(6.1+) 多代时间戳模型替代传统链表, swappiness 控制 anon/file 扫描比例(0=避 swap, 200=积极 swap)

---

### 核心悬念
**"匿名页回收时被写到哪里？swap entry 的 64 位是怎么编码的(类型+偏移)？换出后 PTE 变成什么样？换入时 do_swap_page 怎么找回？"**

→ 引出 10 Swap — add_to_swap + do_swap_page + swapin_readahead
