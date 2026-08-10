# 04 — 写数据流程: write() 不立即落盘的完整链路

> Cluster A: 3 KPs | 依赖: 03-file-creation-touch | 读者基线: 理解 inode/页缓存/bitmap 分配

---

### 1. `sys_write` → `generic_perform_write` — 入口到页级迭代
  - `sys_write(fd, buf, count) → ksys_write → vfs_write → file->f_op->write_iter ← ext2_file_write_iter` (`fs/read_write.c → vfs_write`)
  - `ext2_file_write_iter → generic_file_write_iter → __generic_file_write_iter` (`mm/filemap.c → __generic_file_write_iter`)
  - `generic_perform_write(file, &iocb, &iter)` — 循环逐 folio: write_begin → copy → write_end (`mm/filemap.c → generic_perform_write`)

### 2. `ext2_write_begin` — 获取/分配目标块
  - `a_ops->write_begin → ext2_write_begin → block_write_begin(mapping, pos, len, flags, &folio, ext2_get_block)` (`fs/ext2/inode.c → ext2_write_begin`)
  - `ext2_get_block(inode, iblock, bh_result, create=1)` — 解析 `i_block` 间接块, 设置 `bh_result->b_blocknr` (`fs/ext2/inode.c → ext2_get_block`)
  - 需要新块时: `ext2_new_blocks(inode, goal, &count, err)` — block bitmap 分配 (`fs/ext2/balloc.c → ext2_new_blocks`)

### 3. copy + mark_dirty — page cache 中的"已写入"
  - `iov_iter_copy_from_user_atomic(folio, iter, offset, bytes)` — 内核→用户空间拷贝 (`lib/iov_iter.c → copy_page_from_iter_atomic`)
  - `folio_mark_accessed(folio)` — LRU 热度更新
  - `folio_mark_dirty(folio) → __folio_mark_dirty → __xa_set_mark(PG_dirty)` — Radix Tree 脏标记 (`mm/page-writeback.c → __folio_mark_dirty`)
  - `__mark_inode_dirty(inode, I_DIRTY_PAGES)` — inode 加入 `bdi->wb` 脏链表

### 4. `write_end` — 缓冲头标记与 writeback 触发
  - `block_write_end → __block_commit_write(inode, folio, from, to)` (`fs/buffer.c → __block_commit_write`)
  - `SetPageUptodate(folio)` — 标记页内容有效
  - `mark_buffer_dirty(bh) → test_set_buffer_dirty` — 缓冲头脏标记 (`fs/buffer.c → mark_buffer_dirty`)
  - `balance_dirty_pages_ratelimited(mapping)` — 检查脏页量 → 超 `dirty_ratio` → `balance_dirty_pages(bdi) → sleep(pause)` — 写被阻塞 (`mm/page-writeback.c → balance_dirty_pages`)
  - writeback 线程 (`wb_workfn`) 异步提交 → `ext2_writepages → mpage_writepages → submit_bh(REQ_OP_WRITE)` → 磁盘 (`fs/ext2/inode.c → ext2_writepages`)

### 5. ext4 延迟分配陷阱 — 写+rename+crash 数据丢失
  - ext4: `ext4_da_write_begin → ext4_da_get_block_prep` — 只 reserve 不分配
  - 回写时才真正分配: `ext4_da_write_pages → mpage_da_map_and_submit → ext4_map_blocks → ext4_mb_new_blocks` (`fs/ext4/inode.c → ext4_da_write_pages`)
  - 场景: write("0") → close → rename(oldfile, newfile) → crash → 分配未完成 → rename 销毁旧文件 → write 未提交 → 新文件为空
  - 解决方案: ① `ext4_alloc_da_blocks(inode)` close 时强制分配 ② `fallocate(fd, 0, 0, size)` 预分配绕过 delay alloc (`fs/ext4/extents.c → ext4_fallocate`)

### 6. 收束
  - ext2 写路径: write_begin(块映射) → copy_from_user → mark_dirty → write_end → dirty_ratio 触发阻塞 → 后台 writeback
  - ext4 延迟分配在性能与安全之间引入 tradeoff——多个写合并为大 extent 减少碎片, 但 crash 窗口期有数据丢失风险

---

### 核心悬念
**"read() 首次打开一个从未被读过的文件——页缓存完全未命中时, ext2_get_block 如何从 inode 的间接块链中把一个 4KB 页面完整地从磁盘捞到内存？"**

→ 引出 05-read-data-flow
