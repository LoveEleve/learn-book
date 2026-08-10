# 反向映射 — 如何从物理页反查所有映射它的进程

> Cluster C: 3 KPs | 依赖: 07-writeback-dirty | 读者基线: 脏页写回 + 匿名页换出需求

---

### 1. 匿名反向映射 — anon_vma + anon_vma_chain 的数据结构
  - `struct anon_vma`: `root`(根, 最早 fork 的那个), `struct rw_semaphore rwsem`, `atomic_t refcount`, `unsigned degree`(fork 次数) (include/linux/rmap.h → struct anon_vma)
  - `struct anon_vma_chain`: 连接 VMA 和 anon_vma — `struct vm_area_struct *vma`, `struct anon_vma *anon_vma`, `same_vma`/`same_anon_vma` 双链表 (include/linux/rmap.h → struct anon_vma_chain)
  - fork 链: `dup_mmap → anon_vma_fork(vma, pvma)` → 子进程 VMA 加入父进程的 anon_vma → `anon_vma_chain_link` (kernel/fork.c → dup_mmap)
  - 所有映射同一匿名页的 VMA 通过同一个 anon_vma 关联 → 回收时遍历所有 VMA 修改每个进程的 PTE

### 2. 反向映射调用链 — rmap_walk 到 try_to_unmap_one
  - 回收触发: `shrink_folio_list → try_to_unmap(folio, TTU_BATCH_FLUSH)` (mm/vmscan.c → shrink_folio_list)
  - `rmap_walk_anon(folio, &rwc, try_to_unmap_one_func)` → `anon_vma_interval_tree_foreach(avc, &anon_vma→rb_root, pgoff, pgoff)` 找所有映射此页的 VMA (mm/rmap.c → rmap_walk_anon)
  - 每个 VMA → `try_to_unmap_one(page, vma, address, arg)`: `pte_offset_map` 查页表 → `ptep_get_and_clear` 清除 PTE → 替换为 `swp_entry_to_pte` (mm/rmap.c:1480 → try_to_unmap_one)
  - `page_vma_mapped_walk` 辅助: 边走边验证 page 仍然映射 → `mmap_read_lock(mm)` (mm/rmap.c → page_vma_mapped_walk)
  - **File rmap**: `rmap_walk_file → vma_interval_tree_foreach` 用 `address_space→i_mmap` 树 → `try_to_unmap_one` 清除/swap (mm/rmap.c → rmap_walk_file)

### 3. try_to_unmap_one — PTE 清除的核心操作
  - `ptep_get_and_clear(mm, address, pte)` 原子获取并清除 PTE → 返回旧值 (mm/rmap.c → ptep_get_and_clear)
  - 匿名页: 构造 `swp_entry(val)` swap entry → `set_pte_at(mm, address, pte, swp_entry_to_pte(entry))` 替换为 swap 类型的 PTE (mm/rmap.c → try_to_unmap_one 匿名路径)
  - 文件页: 清除 PTE → `page_remove_rmap(page, vma, false)` 减映射计数 → `page_cache_release` (mm/rmap.c → page_remove_rmap)
  - `mmu_notifier_invalidate_range_start/end` 通知 KVM/Xen 等虚拟化层 TLB 无效化 (mm/rmap.c → mmu_notifier_invalidate_range_start)
  - TTU_BATCH_FLUSH: 批量收集需要 TLB flush 的地址 → 最后统一 `flush_tlb_range` 减少 IPI (mm/rmap.c → TTU_BATCH_FLUSH)

### 4. Freeing Userland Memory — munmap → zap_pte_range 全路径
  - `munmap → unmap_region → unmap_single_vma → unmap_page_range → zap_pte_range` (mm/memory.c → zap_pte_range)
  - `zap_pte_range`: 遍历 PTE → `ptep_get_and_clear → tlb_remove_page_sync_one` (mm/memory.c → zap_pte_range 遍历)
  - MMU gather: `tlb_gather_mmu(&tlb, mm)` 开始 → `tlb_finish_mmu` 结束(TLB flush + 页释放批处理) (mm/memory.c → tlb_gather_mmu)
  - `free_pages_and_swap_cache(pages)` → `free_swap_cache/put_page` → `lru_add_drain` (mm/swap_state.c → free_pages_and_swap_cache)
  - TLB 延迟刷新: 批量 flush 减少 IPI → `range_end = range_start + HPAGE_SIZE * n` → `tlb_flush_mmu_tlbonly → flush_tlb_mm_range` (mm/memory.c → tlb_flush_mmu_tlbonly)

### 5. 收束
  - 匿名反向映射: anon_vma 是所有 fork 子进程共享的根 + anon_vma_chain 连接每个 VMA → `rmap_walk_anon` 遍历所有映射进程
  - `try_to_unmap_one` 清除 PTE + 替换为 swap entry(匿名)或直接清除(文件), TTU_BATCH_FLUSH 批量 TLB flush 减少 IPI
  - munmap→zap_pte_range+MMU gather 批量释放, 延迟 TLB flush 合并多页减少跨核同步开销

---

### 核心悬念
**"回收路径怎么判断该回收匿名页还是文件页？LRU 四链表(inactive/active anon/file) 的状态转换规则是什么？6.1 的 MGLRU 怎么革命性地替代传统链表？"**

→ 引出 09 LRU 与回收 — kswapd 后台回收 + Direct Reclaim + MGLRU
