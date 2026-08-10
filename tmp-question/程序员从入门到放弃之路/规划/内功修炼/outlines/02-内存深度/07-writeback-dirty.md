# Writeback — 脏页如何安全落盘

> Cluster D: 4 KPs | 依赖: 06-page-cache-xarray | 读者基线: address_space + xarray 索引

---

### 1. 脏页标记与限流触发链 — 从 folio_mark_dirty 到进度睡眠
  - `folio_mark_dirty → folio_account_dirtied → __folio_mark_dirty` (mm/page-writeback.c → folio_mark_dirty)
  - `__xa_set_mark(&mapping→i_pages, folio_index(folio), PAGECACHE_TAG_DIRTY)` xarray 标记 DIRTY (mm/page-writeback.c → __xa_set_mark)
  - `inc_node_page_state(folio, NR_FILE_DIRTY)` 全局脏页计数 +1 → `wb_dirty_exceeded(wb, bg_thresh)` 检查超后台阈值 → 超 → `wb_start_background_writeback(wb)` (mm/page-writeback.c)
  - **第二层**: `balance_dirty_pages_ratelimited` → `balance_dirty_pages(bdi)` → `global_dirty_limits(&bg_thresh, &dirty_thresh)` (mm/page-writeback.c)
  - `bg_thresh = total_available_memory * dirty_background_ratio/100`(默认 10%), `dirty_thresh = total * dirty_ratio/100`(默认 20%)
  - 脏页超 dirty_thresh → `current→nr_dirtied`, `dirty_pause`, `io_schedule_timeout(pause)` 进度睡眠 → 直到脏页低于阈值

### 2. Flusher 线程工作机制 — per-backing-device 的写回循环
  - `struct bdi_writeback` → `wb_workfn → wb_do_writeback → wb_check_background_flush` (fs/fs-writeback.c)
  - `wb_writeback(wb, work)`: `writeback_sb_inodes(wb, sb)` 遍历 sb 脏 inode (fs/fs-writeback.c)
  - `__writeback_single_inode(inode, wbc) → do_writepages(mapping, wbc) → a_ops→writepages → ext4_writepages` (fs/fs-writeback.c:1530)
  - `mpage_map_and_submit_extent → mpage_submit_bio → bio_alloc → bio_add_folio → submit_bio` 创建 bio → 块层 → 磁盘
  - 写完 → `folio_end_writeback(folio) → folio_clear_dirty → __xa_clear_mark(&mapping→i_pages, index, PAGECACHE_TAG_DIRTY)` (mm/page-writeback.c)
  - sync 模式: `WB_SYNC_NONE`(后台, 不等待) vs `WB_SYNC_ALL`(sync 系统调用, 等待每个 inode) (fs/fs-writeback.c → wb_writeback)

### 3. Dirty Throttling 限流数学 — pid 控制器思想
  - 核心目标: 脏页量稳定在 `setpoint = (bg_thresh + dirty_thresh) / 2` (mm/page-writeback.c)
  - `dirty_ratelimit`: 平滑的长期写速率(页/秒) — 每 200ms 更新一次 (mm/page-writeback.c)
  - `pos_ratio`: 当前脏页量偏离 setpoint 的比例 → `bdi_position_ratio(bdi, thresh, bg_thresh, dirty, bdi_dirty)` 返回 0-2 倍间值 (mm/page-writeback.c)
  - `task_ratelimit = dirty_ratelimit * pos_ratio >> RATELIMIT_CALC_SHIFT` 每个任务的实际写速率
  - `bdi_dirty_limit(bdi, dirty)` 此设备的脏页上限 — 5.9+ 统一 domain: `domain_dirty_limits` 替代 `global_dirty_limits`
  - 可调参数: `/proc/sys/vm/dirty_ratio`(20%), `dirty_background_ratio`(10%), `dirty_bytes/dirty_background_bytes`(绝对值), `dirty_expire_interval`(30s)

### 4. memfd — 匿名文件的内存共享机制
  - `memfd_create(name, MFD_CLOEXEC|MFD_ALLOW_SEALING) → anon_inode_getfile("[memfd]", &memfd_fops)` (mm/memfd.c)
  - `shmem_file_setup(name, size, VM_NORESERVE)` 匿名文件 → 无磁盘后备 → 完全在页缓存中 (mm/shmem.c)
  - `ftruncate(fd, size)` → 写(fd, "data") → shmem writepage 可 swap → `F_ADD_SEALS(F_SEAL_SHRINK|F_SEAL_GROW|F_SEAL_WRITE)` 不可修改
  - Android/Flatpak 图形 buffer → 高效率 IPC 共享内存机制 (mm/memfd.c)

### 5. 收束
  - 脏页标记 → 超 bg_thresh(10%) 唤醒后台写回 → 超 dirty_thresh(20%) 阻塞写入者 → pid 控制器稳定脏页量在 setpoint
  - Flusher per-backing-device 模型独立写回循环, `WB_SYNC_NONE`(后台)/`WB_SYNC_ALL`(sync 阻塞等待)两种模式
  - memfd=匿名文件无磁盘后备→完全在页缓存, 配合 seal 实现不可变的共享内存 IPC

---

### 核心悬念
**"回收换出匿名页时，内核怎么找到所有映射该页的进程的 PTE？反向映射 anon_vma + rmap_walk 怎么反向追到每个 VMA？"**

→ 引出 08 反向映射 — anon_vma + rmap_walk + try_to_unmap_one
