# 08 — JBD2 日志架构: 从事务到恢复的完整生命周期

> Cluster B: 2 KPs | 依赖: 07-filesystem-comparison | 读者基线: 理解 ext2 无日志的崩溃恢复问题

---

### 1. JBD2 架构 — `struct journal_s` 核心对象
  - `struct journal_s { j_flags, j_errno, struct block_device *j_dev, struct inode *j_inode; unsigned long j_head, j_tail, j_free; unsigned int j_max_transaction_buffers; struct task_struct *j_task; }` (`fs/jbd2/journal.c → struct journal_s`)
  - Journal 文件可以是磁盘上的特殊 inode (`j_inode`) 或独立设备 (`j_dev`)
  - `j_head`/`j_tail` — 环形缓冲区指针, journal 写满后 checkpoint 回收空间

### 2. `handle_t` 事务生命周期 — begin→get_write_access→dirty→stop
  - `jbd2_journal_start(handle, nblocks) → handle_t` — 预留 nblocks 的 journal 空间 (`fs/jbd2/transaction.c → jbd2_journal_start`)
  - `jbd2_journal_get_write_access(handle, bh)` — 获得元数据块的修改权限, 对 bh 做 copy (`fs/jbd2/transaction.c → jbd2_journal_get_write_access`)
  - modify the buffer — 实际业务代码修改 `bh->b_data`
  - `jbd2_journal_dirty_metadata(handle, bh)` — 标记该 bh 属于当前事务的脏元数据 (`fs/jbd2/transaction.c → jbd2_journal_dirty_metadata`)
  - `jbd2_journal_stop(handle)` — 结束事务, 若事务满则触发 commit (`fs/jbd2/transaction.c → jbd2_journal_stop`)

### 3. Commit — `jbd2_journal_commit_transaction` 提交块结构
  - 后台线程 `kjournald2` → `jbd2_journal_commit_transaction(journal)` (`fs/jbd2/commit.c → jbd2_journal_commit_transaction`)
  - 写入 descriptor block: 前导块, 列出本事务所有元数据块的 tag 数组 — 每个 tag 含 blocknr+flags (`fs/jbd2/commit.c → jbd2_journal_commit_transaction`)
  - 写入 metadata blocks: 所有脏元数据块的完整内容
  - 写入 commit block: 事务结束标记, 含 `commit_sec` 时间戳
  - IO 顺序: descriptor→metadata→commit — 先写描述再写数据再写提交标记
  - 屏障: `blkdev_issue_flush` → commit block 落盘 → 事务从此可恢复

### 4. ext4 三种日志模式 — ordered / writeback / data
  - `data=writeback`: 元数据 journal, 数据异步 — 最快, 崩溃后文件内容可能有垃圾 (`fs/ext4/super.c:ext4_load_journal`)
  - `data=ordered` (默认): 元数据 journal, 数据先 flush 到磁盘再 commit journal — 平衡
  - `data=journal`: 元数据+数据两者都写 journal — 最安全最慢, 每次写两遍 (journal+最终位置)
  - 查看: `/proc/fs/ext4/<dev>/options`, 修改: `tune2fs -o journal_data /dev/sdX`, 关闭: `tune2fs -O ^has_journal /dev/sdX`

### 5. Replay — 崩溃恢复的扫日志流程
  - 挂载时 `jbd2_journal_recover(journal) → do_one_pass` — 扫描 journal 从头到尾 (`fs/jbd2/recovery.c → jbd2_journal_recover`)
  - 找到 descriptor block → 读取 tag 数组 → 对每个 tag: 读取对应的 journal 中数据块副本 → `jbd2_journal_do_checkpoint` 写入磁盘最终位置
  - 看到 commit block: 确认本事务完整 → 标记所有 tag 块为已恢复
  - 无 commit block: 事务未完成, 丢弃该事务所有块
  - Checkpoint: `jbd2_journal_do_checkpoint` — 将已恢复的事务块从 journal 区移出, 回收 journal 空间 (`fs/jbd2/checkpoint.c → jbd2_journal_do_checkpoint`)

### 6. 收束
  - JBD2 三步: start(预留空间)→dirty_metadata(标记脏)→stop(触发commit)→commit(写入journal三块)→replay(扫描恢复)→checkpoint(回收空间)
  - 三种日志模式在性能(40%→200%差异)与数据安全性间的 tradeoff

---

### 核心悬念
**"Btrfs 的 COW 机制不用 journal——它如何通过原子更新树根指针和 checksum 实现与 JBD2 等效的崩溃一致性？zfs send/receive 的增量快照又是如何在两个 pool 之间只传输变化的 block？"**

→ 引出 09-cow-snapshot
