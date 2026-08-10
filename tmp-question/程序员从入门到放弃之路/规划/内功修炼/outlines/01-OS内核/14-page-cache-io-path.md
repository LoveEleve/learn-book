# 页缓存 I/O 路径 — 页缓存读写 + Direct I/O + mmap I/O + 零拷贝

> Cluster D: 4 KPs | 依赖: 13-VFS + ext4 | 读者基线: 理解页缓存结构和 VFS 路径解析

---

### 1. 页缓存读 — 四级缓存命中
  - `do_generic_file_read → find_get_page` → 检查 address_space 的 xarray(folio 索引) (mm/filemap.c:2265)
  - 页面状态: not present(需 readpage 磁盘 IO) / uptodate(内容有效, 直接返回) / locked(正在 IO, 等待) / dirty(有未写回数据)
  - 预读(readahead): 顺序读检测 → `page_cache_sync_readahead` → 预测下 N 页 → 异步预读 → 减少同步等待 → `ondemand_readahead` 动态调整预读窗口大小(最小窗口 4 页 → 逐步扩大) (mm/readahead.c:489)
  - 预读参数: `/sys/block/sda/queue/read_ahead_kb`(内核默认 128KB)

### 2. 页缓存写 — 延迟分配 + 脏页回写
  - `generic_perform_write` → 分配页缓存页面 → 用户数据从用户 buffer 拷贝到页缓存 → mark_buffer_dirty(标记块脏) → set_page_dirty(标记页脏) → 至此 write() 返回(无磁盘 IO) → 异步回写 (mm/filemap.c:3408)
  - 延迟分配(delayed allocation): ext4 的 `delalloc` → 不立即分配磁盘块 → 只分配页缓存 → 回写时才决定物理块位置 → 更好的 extent 连续性(连续分配) (fs/ext4/inode.c:3430)
  - 回写触发: `dirty_expire_interval`(30s 过期) / `dirty_background_ratio`(10% 脏页触发后台回写) / `sync/fsync`(手动同步) → flusher 线程异步回写

### 3. Direct I/O — 绕过页缓存直接到磁盘
  - 打开: `open(path, O_DIRECT)` → `do_direct_IO` → 用户 buffer 直接 DMA 到磁盘 → 无页缓存中转 (fs/direct-io.c:1158)
  - 约束: buffer 必须对齐(通常 512 字节) → 无预读(readahead) → 无写合并(需应用自己 batch) → `fio --direct=1 --bs=4k` 测试
  - 数据库场景: MySQL/PostgreSQL 常用 Direct I/O → 数据库自己管理缓存(比内核页缓存更适合工作负载, B+tree 预读) → `innodb_flush_method=O_DIRECT`

### 4. mmap I/O — 文件映射直接到用户地址空间
  - `do_mmap → addr → 缺页异常 → filemap_fault → readpage → page cache` → 共享页缓存(同一文件 mmap 和 read 共享同一页面) (mm/filemap.c:3180)
  - 拷贝次数对比: read = 页缓存(磁盘→内核) + 内核→用户 buffer = 2 次拷贝 / mmap = 仅一次(磁盘→页缓存, 页表直接映射到用户空间) — 节省一次 CPU 拷贝 (mm/mmap.c:1610)
  - `msync`(刷回): 确保 mmap 修改写入磁盘 / `munmap`(解除映射但不保证回写) / `MADV_DONTNEED`(标记不需要此映射, 回收内存)

### 5. 零拷贝 — sendfile + splice 绕过 CPU
  - `sendfile(out_fd, in_fd, &offset, count)`: 文件到 socket → DMA 磁盘→内核 buffer → DMA 内核 buffer→网卡 → CPU 零参与 → 比 read+write 减少一次内核→用户拷贝和一个系统调用 (fs/read_write.c:1378)
  - `splice(fd_in, &off_in, pipefd[1], NULL, len, SPLICE_F_MOVE)`: 管道零拷贝 → 数据只在内核空间流转(页引用) → 不拷贝 → `tee` 复制管道内容为两个 → 适合文件→socket 或 socket→文件
  - `SO_ZEROCOPY`: send 零拷贝 → 用户 buffer 被 pin 住(DMA 直接读取) → 复用 socket buffer → 适合高吞吐发送

### 6. 收束
  - 四级缓存命中(not present/uptodate/locked/dirty) + 预读 = 顺序读性能靠预读
  - Direct I/O(无缓存, 数据库) vs mmap I/O(少一次拷贝, 适合大数据) vs 零拷贝(无 CPU 拷贝, 适合文件传输)
  - 拷贝次数: read=2 次(磁盘→内核→用户) / mmap=1 次 / sendfile=0 次(DMA 直接搬运)

---

### 核心悬念
**"文件通过 ext4+VFS+页缓存读写 — 网络 IO 这边, epoll 怎么用红黑树+就绪链表做事件监控？bio 和 request 在块设备层怎么分层？"**

→ 引出 15-I/O 调度器 + blk-mq + bio/request + epoll 实现 + Netfilter
