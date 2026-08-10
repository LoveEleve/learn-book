# 页缓存 — xarray、预读与用户态控制

> Cluster D: 6 KPs | 依赖: 05-page-fault-scenarios | 读者基线: 文件缺页 #PF 处理流程

---

### 1. address_space 与 xarray — 页缓存的索引核心
  - 设计考量: radix tree 需要额外 slot 数组管理 + 每个节点独立分配造成内存碎片; xarray 将 slot/tag/value 内联到单条目, 内存密度更高 + RCU 无锁读天然并发友好 (4.20+ 替代动机)
  - `struct address_space`: `struct inode *host`, `struct xarray i_pages`, `nrpages`, `writeback_index`, `const struct address_space_operations *a_ops` (include/linux/fs.h → struct address_space)
  - xarray API: `XA_STATE(xas, &mapping→i_pages, idx)` 定义遍历状态 → `xas_load(&xas) → folio` 查找 (lib/xarray.c → xas_load)
  - `xas_store(&xas, folio)` 存储, `xas_find(&xas, max)` 范围查找, `xas_for_each(&xas, folio, max)` 遍历 (lib/xarray.c → xas_store)
  - `xas_set_order(&xas, idx, order)` 多阶索引, `xa_lock_irq(&mapping→i_pages)` 写锁 (lib/xarray.c → xas_set_order)
  - RCU 保护读路径: `rcu_read_lock → xas_load → rcu_read_unlock` 无锁查找 — 4.20+ 替代 radix tree (lib/xarray.c → xa_load RCU 读)

### 2. Readahead 预读算法 — ondemand_readahead 的状态机
  - 触发时机: (1) 缺页→`filemap_fault → do_sync_mmap_readahead → page_cache_sync_ra` 同步预读; (2) 顺序访问→`do_async_mmap_readahead` 异步预读 (mm/filemap.c → filemap_fault)
  - `struct file_ra_state`: `start`(当前窗口开始), `size`(窗口大小/页), `async_size`(异步部分) (include/linux/fs.h → struct file_ra_state)
  - `ondemand_readahead`: 初始窗口→4 页 → 顺序访问→`ra→size *= 2` 倍增 → 最大 `VM_READAHEAD_PAGES`(默认 512KB=128 页) → 随机→`ra→size /= 2` 缩小 (mm/readahead.c → ondemand_readahead)
  - `ra_submit → read_pages → a_ops→readahead → mpage_readahead` → `bio_alloc → bio_add_folio → submit_bio` 启动 IO (mm/readahead.c → ra_submit)
  - `fadvise(POSIX_FADV_SEQUENTIAL) / POSIX_FADV_RANDOM` 控制预读策略 (mm/fadvise.c → fadvise)

### 3. Fault-Around — 缺页时顺带填充邻页
  - `do_fault_around(vmf, start_pgoff)`: 计算前后范围 → 默认 `fault_around_bytes/PAGE_SIZE`=16 页(前后共 32 页) (mm/memory.c → do_fault_around)
  - `filemap_map_pages(vmf, start_pgoff, end_pgoff)` → `find_get_page(mapping, pgoff)` 在页缓存查找 → 存在→`alloc_set_pte` 建立映射 (mm/filemap.c → filemap_map_pages)
  - 仅文件缺页触发(`vma->vm_ops->map_pages`) — 匿名缺页不适用 — 对顺序读有效, 随机读浪费 (mm/memory.c → fault_around_bytes)

### 4. Direct I/O — 绕过页缓存的直通路径
  - `O_DIRECT → __generic_file_write_iter → generic_file_direct_write → invalidate_inode_pages2_range` 使无效页缓存 (mm/filemap.c → generic_file_direct_write)
  - `iomap_dio_rw → __iomap_dio_rw → bio_alloc → submit_bio` 直接构造 bio (fs/iomap/direct-io.c → iomap_dio_rw)
  - 约束: 块对齐, 应用自管缓存, `O_DIRECT|O_DSYNC` 每次 IO 后 fdatasync — 数据库首选(自管理 buffer pool)

### 5. mlock/mprotect/madvise — 用户态内存控制三元组
  - **mlock**: `mlock(addr, len) → __mm_populate → get_user_pages` 锁物理页 → `vma→vm_flags|=VM_LOCKED` → 不可换出 (mm/mlock.c → __mm_populate)
  - **mprotect**: `mprotect(addr, len, prot) → mprotect_fixup → change_protection → walk_page_range → change_pte_range` 逐 PTE 改保护位 + TLB flush (mm/mprotect.c → mprotect_fixup)
  - **madvise**(mm/madvise.c → madvise): MADV_SEQUENTIAL(增加预读)/MADV_RANDOM(减少预读)/MADV_WILLNEED(`force_page_cache_readahead`)/MADV_DONTNEED(`zap_page_range → free_pages`)/MADV_FREE(可立即回收)/MADV_COLD(移出活跃 LRU)/MADV_PAGEOUT(立即回收)/MADV_HUGEPAGE(触发 khugepaged 扫描)

### 6. 收束
  - xarray(4.20+) RCU 读无锁替代 radix tree, `XA_STATE` 遍历状态 + `xas_load/store/find` 高效索引页缓存
  - 预读状态机 `ondemand_readahead` 顺序访问倍增(最大 512KB)→随机访问缩小, Fault-Around 顺带填充邻页(默认 32 页)
  - Direct I/O 绕过页缓存直通块层, madvise 用户态控制预读/回收/锁定行为

---

### 核心悬念
**"脏页标记后谁来写回？Flusher 线程的 per-backing-device 模型 + pid 控制器 Dirty Throttling 限流怎么运行的？"**

→ 引出 07 Writeback — 脏页标记 + Flusher 线程 + Dirty Throttling
