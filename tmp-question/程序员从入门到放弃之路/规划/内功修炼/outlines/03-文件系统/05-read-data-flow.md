# 05 — 读数据流程: 页缓存命中/未命中/预读三级加速

> Cluster A: 2 KPs | 依赖: 04-write-data-flow | 读者基线: 理解页缓存/folio/buffer_head 概念

---

### 1. `sys_read` → `filemap_read` — VFS 到页缓存入口
  - `sys_read(fd, buf, count) → ksys_read → vfs_read → file->f_op->read_iter ← ext2_file_read_iter` (`fs/read_write.c → vfs_read`)
  - `ext2_file_read_iter → generic_file_read_iter → filemap_read(iocb, to, &iocb->ki_pos, 0)` (`mm/filemap.c → filemap_read`)
  - 外层循环: `filemap_get_pages(iocb, to, &pvec)` — 获取页向量 (`mm/filemap.c → filemap_get_pages`)

### 2. 同步预读 — `page_cache_sync_readahead`
  - `filemap_get_pages → page_cache_sync_readahead(mapping, ra, filp, index, last_index - index)` (`mm/readahead.c → page_cache_sync_readahead`)
  - `ondemand_readahead → ra_submit → read_pages` — 提交初始读窗口
  - 窗口计算: `req_size` 控制初始预读量, 顺序读检测扩大窗口

### 3. 页缓存命中 — fast path
  - `filemap_get_entry(mapping, index) → folio` — Radix Tree 查找 (`mm/filemap.c → filemap_get_entry`)
  - folio 存在: `folio_lock → folio_put → copy_folio_to_iter(folio, offset, bytes, iter)` — 直接拷贝到用户 buf
  - 无需任何磁盘 IO — 纯内存操作

### 4. 页缓存未命中 — `ext2_read_folio` 触发磁盘 IO
  - `filemap_create_folio(gfp, mapping, index)` — 分配新 folio (`mm/filemap.c → filemap_create_folio`)
  - `filemap_read_folio(file, mapping, folio) → a_ops->read_folio ← ext2_read_folio` (`fs/ext2/inode.c → ext2_read_folio`)
  - `ext2_read_folio → block_read_full_folio(folio, ext2_get_block)` (`fs/ext2/inode.c → ext2_read_folio`)
  - `create_empty_buffers(folio, blocksize, 0)` — 为 folio 创建 buffer_head 数组 (`fs/buffer.c → create_empty_buffers`)
  - 每个 bh: `bh->b_blocknr = ext2_get_block` — 通过 `ext2_block_to_path` 解析间接块 → `submit_bh(REQ_OP_READ, bh)` → IO → `wait_on_buffer(bh)` 等待完成 (`fs/ext2/inode.c → ext2_get_block`)
  - folio 标记 `uptodate` → 解锁 → `copy_folio_to_iter` 返回数据

### 5. 异步预读 — 下一次缺页已提前缓存
  - 每次 `filemap_read` 循环末尾: `page_cache_async_readahead(mapping, ra, filp, folio, index, last_index - index)` (`mm/readahead.c → page_cache_async_readahead`)
  - 异步提交 → 后台 IO, 不阻塞当前读取
  - 顺序读检测: 连续 index 递增 → 扩大 `ra_pages` 窗口 → 下一次缺页命中率提升
  - `/sys/block/<dev>/queue/read_ahead_kb` — 内核预读窗口大小, 默认 128KB

### 6. 零拷贝路径 — sendfile / splice / mmap（补充）
  - `sendfile(out_fd, in_fd, &offset, count)` — DMA 磁盘→DMA 网卡, 零 CPU 拷贝, 数据不经用户态 (`fs/read_write.c → sys_sendfile64`)
  - `splice(pipefd, in_fd, NULL, len, SPLICE_F_MOVE)` — 管道零拷贝, 内核态页面引用传递 (`fs/splice.c → sys_splice`)
  - `mmap()` 映射文件到用户空间 — 缺页中断触发 `filemap_fault → ext2_get_block` 读磁盘, 后续直接 mem 访问 (`mm/filemap.c → filemap_fault`)

### 7. 收束
  - 读路径三态: 缓存命中(0 IO)→未命中(block_read_full_folio 单次 IO)→预读未命中(后台 IO 已就绪)
  - `ext2_get_block` 是读写共用的块映射核心——同样的间接块解析被 `write_begin` 和 `read_folio` 共享

---

### 核心悬念
**"rm a.txt 执行后, inode 的 i_block[14] 三重间接链上的几百个数据块如何被一个个递归找到并释放 bitmap？如果没有日志, 这个递归过程中途崩溃会留下什么？"**

→ 引出 06-file-deletion-xattr-locks
