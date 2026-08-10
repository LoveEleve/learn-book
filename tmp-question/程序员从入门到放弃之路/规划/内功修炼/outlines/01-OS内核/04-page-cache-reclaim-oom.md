# 页缓存 + 脏页回写 + LRU 回收 + Swap + OOM Killer

> Cluster A: 12 KPs | 依赖: 03-mmap + VMA + 缺页异常 | 读者基线: 了解 VMA 和缺页异常分支

---

### 1. 页缓存 — 文件 IO 的必经缓存层
  - `struct address_space`: host(inode) / i_pages(xarray, 4.20+ 替代 radix tree 索引) / a_ops(address_space_operations) — 每个 inode 一个 (include/linux/fs.h:445)
  - 读流程: `do_generic_file_read → find_get_page`(查 xarray) → 未命中 → `page_cache_sync_readahead`(预读) → `readpage`(磁盘 IO) → 加入 address_space → 再次访问命中(零 IO) (mm/filemap.c:2265)
  - 写流程: `generic_perform_write → __block_write_begin`(读块到页) → 用户写入修改 → `mark_buffer_dirty`(标记块脏) → `set_page_dirty`(标记页脏) → 异步回写 (mm/filemap.c:3408)
  - folio(Linux 5.16+): 替代单个 page 作为页缓存基本单位 → compound page → 减少 page→folio 转换 → writepages 批量操作 (include/linux/page-flags.h:358)

### 2. 脏页回写 — 从内存到磁盘的流水线
  - flusher 线程(per backing device): 替代旧 pdflush → `dirty_background_ratio`(默认 10%)触发后台回写不阻塞 → `dirty_ratio`(默认 20%)触发前台阻塞等待 (mm/page-writeback.c:1971)
  - 回写参数: `dirty_expire_interval`(默认 30s, 老化时间) / `dirty_writeback_interval`(默认 5s, 检查周期) / `dirty_bytes`(字节级阈值替代百分比) (mm/page-writeback.c:68)
  - 流程: `bdi_writeback → wb_writeback → writeback_sb_inodes → __writeback_single_inode → do_writepages → mapping->a_ops->writepages` (fs/fs-writeback.c:1649)

### 3. LRU 内存回收 — 四链表 + kswapd 水位线
  - 四 LRU 链表: lruvec→lists[LRU_INACTIVE_ANON]/[LRU_ACTIVE_ANON]/[LRU_INACTIVE_FILE]/[LRU_ACTIVE_FILE] — 页在这四个链表中升降 (include/linux/mmzone.h:278)
  - 水位线触发: high(安全)/low(后台回收唤醒 kswapd)/min(直接回收, 阻塞分配) → `/proc/zoneinfo` → 每 zone 独立水位 (mm/page_alloc.c:6855)
  - kswapd vs direct reclaim: 低于 low → kswapd(异步, 不阻塞分配) / 低于 min → direct reclaim(同步, 阻塞分配直到释放) / `shrink_node→shrink_lruvec→isolate_lru_pages→回收或换出` (mm/vmscan.c:4675)
  - refault distance: 页被回收后再次访问的距离—如果近(min_seq−refault distance 小)说明过早回收→升到 active (mm/workingset.c:268)

### 4. KSM 内核同页合并 — 相同内容的匿名页去重 (mm/ksm.c → ksm_do_scan)
  - KSM(Kernel Same-page Merging): 扫描匿名页 → memcmp 比较内容 → 相同 → 合并为写保护 COW 页 → 后续写触发 COW 裂开 → 适合虚拟机/容器部署(多个 OS 镜像的相同页)
  - 配置: `echo 1 > /sys/kernel/mm/ksm/run` → `pages_to_scan`(每次扫描页数) / `sleep_millisecs`(扫描间隔) / `pages_shared`(已合并数) — KVM 用 `MADV_MERGEABLE` 标记虚拟机内存
  - 代价: CPU 扫描开销(默认每次 100 页) + COW 裂开时的一页复制 → 适合 VPS/虚拟化场景而非通用负载

### 5. Swap 换页 — 匿名页的后备存储
  - swap entry: PTE 低 12 位为 0(标识 swap) → 高位存 swap_type + swap_offset → "此页在 swap 中, 不在物理内存" (mm/swapfile.c:1549)
  - do_swap_page: 缺页异常 → 检测 swap entry → 分配物理页 → 从 swap 分区/文件读回 → 恢复 PTE 映射 → 与 file-backed major fault 类似 (mm/memory.c:3180)
  - swappiness: `vm.swappiness`(0-100, 默认 60) → >60 倾向 swap 匿名页 / <60 倾向回收文件页 → 数据库建议 0-10(优先回收文件页, 有后备存储不需 swap)

### 6. OOM Killer — 内存耗尽时的最后一刀
  - 触发: direct reclaim 无效 → `out_of_memory` → `select_bad_process`(badness 评分) → `oom_kill_process` → SIGKILL (mm/oom_kill.c:401)
  - badness 公式: 进程 rss(驻留内存) / oom_score_adj / 运行时间 / CAP_SYS_ADMIN 减分 → `/proc/PID/oom_score`(动态) / `/proc/PID/oom_score_adj`(-1000 永不杀) (mm/oom_kill.c:255)
  - OOM 日志: "Out of memory: Killed process XXXX (name) total-vm:XXXXkB, anon-rss:XXXXkB" → `dmesg | tail -50`

### 7. 收束
  - 页缓存是文件 IO 的加速引擎，脏页回写是内存→磁盘的流水线
  - LRU 回收 = 四链表 + 水位线 + refault distance 三层决策
  - KSM 去重节省内存(适合 VPS/虚拟化) → Swap + OOM 是内存不足时的逐级应对: 换出旧页 → 换出不行杀进程

---

### 核心悬念
**"单核跑完了内存分配，多核同时分配页时 Buddy 分配器的 free_area 链表会被并发破坏——CPU 怎么保证缓存行一致性？什么是 MESI？"**

→ 引出 05-MESI 缓存一致性 + 伪共享 + Store Buffer + 内存模型
