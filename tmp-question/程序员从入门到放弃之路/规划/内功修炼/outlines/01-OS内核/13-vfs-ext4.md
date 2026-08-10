# VFS 四大对象 + inode/dentry + ext4 extent 树 + 日志 JBD2

> Cluster D: 6 KPs | 依赖: 04-页缓存 + 11-进程模型 | 读者基线: 理解页缓存和 Linux 文件 API(open/read/write/close)

---

### 1. VFS 统一接口 — 四种文件系统的最大公约数
  - 四大对象: super_block(文件系统超级块) / inode(文件元数据) / dentry(目录项, 名称→inode 的缓存) / file(打开文件, 含当前偏移) → 统一接口: open/read/write/lseek/close/ioctl → 每个文件系统实现自己的操作集 (include/linux/fs.h:597)
  - 四级操作集: super_operations(alloc_inode/destroy_inode/write_super) / inode_operations(create/link/mkdir/rename) / dentry_operations(d_compare/d_hash/d_delete) / file_operations(read/write/mmap/open/ioctl) (include/linux/fs.h:1847-1860)
  - 路径解析: `namei → path_lookup → link_path_walk` → 逐分量查找 dentry → `do_last` 处理最后分量 → open 或 create → 一次路径解析 = 多次 dentry 查找 + inode 读 (fs/namei.c:3396)

### 2. inode + dentry — 文件系统的元数据中枢
  - `struct inode`: i_ino(inode 号) / i_mode(类型+权限) / i_uid / i_gid / i_size / i_blocks / i_atime/i_mtime/i_ctime → i_mapping(address_space, 文件数据的页缓存) / i_op(inode_operations) / i_fop(file_operations) (include/linux/fs.h:640)
  - `struct dentry`: d_name(文件名) / d_inode(指向 inode) / d_parent(父目录) → `d_hash`(dentry 哈希) → `d_lookup`(哈希查找) → 命中则直接返回 → 未命中则 `d_alloc` + 读磁盘
  - dentry 缓存: dcache(dentry cache) → 软链接(相同 inode 不同 dentry) / 硬链接(同一 dentry) → `ls -i` 显示 inode 号

### 3. ext4 extent 树 — 大文件的低开销索引
  - extent 节点: `ext4_extent_header → ext4_extent_idx`(索引节点, 每个 12 字节) → `ext4_extent`(叶子节点: ee_block 起始逻辑块号 + ee_len 连续块数 + ee_start_lo/ee_start_hi 物理块号) — 比间接块减少元数据开销 (fs/ext4/ext4_extents.h:117)
  - 大文件存储: 连续分配 >> 间接块 → 100MB 文件可能只需 1 个 extent 而非 25600 个间接块 → 索引更小, 读更快 → extent 最大化(allocate_blocks 尝试连续)
  - 日志模式三种: ordered(元数据日志 + 数据先写, 默认, 保证一致性 + 性能折中) / writeback(仅元数据日志, 最快但崩溃后可能旧数据) / data(元数据+数据都日志, 最安全但最慢) (fs/ext4/super.c → ext4_fill_super)

### 4. JBD2 日志 — 崩溃恢复的保证
  - 事务: `jbd2_journal_start/stop` → 将所有修改包装成事务 → 事务提交(写入日志 journal block device) → 事务完成(写入 checkpoint) → 崩溃恢复(重放已完成事务) (fs/jbd2/transaction.c:462)
  - 日志结构: journal superblock + descriptor block(描述修改哪些块) + data blocks(被修改的块副本) + commit block(事务结束标记) → 块设备上用连续区域
  - ordered 模式流程: 先写数据到磁盘 → 元数据修改写入日志 → 日志提交 → 元数据写入对应位置(checkpoint) → 日志区释放

### 5. 收束
  - VFS 是文件系统的抽象层 — super_block + inode + dentry + file = 四种对象统一读写路径
  - ext4 extent 树 = 连续寻址的 B 树, 大文件用极少元数据 → JBD2 日志保证崩溃后一致

---

### 核心悬念
**"ext4 用 extent 树存数据 — 但读写文件时到底经过多少个缓冲区？为什么 mmap 比 read 少一次拷贝？零拷贝 sendfile 怎么跳过 CPU？"**

→ 引出 14-页缓存读写流程 + Direct I/O + mmap I/O + 零拷贝(sendfile/splice)
