# 06 — 文件删除/扩展属性/文件锁: unlink 全链路与元数据扩展

> Cluster A: 4 KPs | 依赖: 02/03/04/05 — 对 inode/间接块/位图/目录项全理解 | 读者基线: 理解 i_block 间接块链释放原理

---

### 1. `sys_unlink` → 目录项删除 — 第一步: 从目录树中摘除
  - `sys_unlink(pathname) → do_unlinkat → vfs_unlink(mnt_userns, dir, dentry, &delegated_inode)` (`fs/namei.c → vfs_unlink`)
  - `dir->i_op->unlink(dir, dentry) ← ext2_unlink` (`fs/ext2/namei.c → ext2_unlink`)
  - `ext2_delete_entry(dir, de, bh)` — `de->inode=0`(空洞) (`fs/ext2/dir.c → ext2_delete_entry`)
  - 空洞向前合并: `pde->rec_len += de->rec_len` — 不需整理, `mark_buffer_dirty_inode(bh, dir)`

### 2. `i_links_count` → orphan list → `ext2_truncate`
  - `inode_dec_link_count(inode) → drop_nlink(inode)` — `i_links_count` 减 1 (`fs/inode.c → drop_nlink`)
  - 降至 0: `ext2_orphan_add(inode)` — 加入 orphan list (`fs/ext2/inode.c → ext2_orphan_add`)
  - `ext2_free_inode(inode) → clear_inode → ext2_clear_inode` — `inode->i_size=0, ext2_truncate(inode)` (`fs/ext2/inode.c → ext2_truncate`)

### 3. 间接块递归释放 — `ext2_free_branches` 三重递归
  - `ext2_truncate → ext2_free_data(inode, i_data, i_data+EXT2_N_BLOCKS-1)` (`fs/ext2/inode.c → ext2_truncate`)
  - 直接块: `ext2_free_blocks(sb, block, count)` → `ext2_clear_bit(block, bitmap_bh->b_data)` (`fs/ext2/balloc.c → ext2_free_blocks`)
  - 单重间接: 读 indirect block → 遍历 block 号 → 逐个 `ext2_free_blocks` → 最后释放 indirect block 自身
  - 二重间接: 读 block → 遍历二级 indirect → 每个二级走单重递归
  - 三重间接: `ext2_free_branches` 三层嵌套 — 每一层都是 block bit 清除 (`fs/ext2/inode.c → ext2_free_branches`)
  - 更新计数器: `sbi->s_free_blocks_count++`, `gdp->bg_free_blocks_count++`

### 4. inode bitmap 释放 — 最终清理
  - `ext2_clear_bit(ino, bitmap_bh->b_data)` — inode 空闲 (`fs/ext2/ialloc.c → ext2_free_inode`)
  - 从 orphan list 移除
  - `ext2_write_inode(inode)` — 写入 `i_dtime` 删除时间戳
  - 风险: 无日志 → 递归释放在中途崩溃 → fsck 需扫描 bitmap/间接块一致性

### 5. 扩展属性 xattr + ACL — inode 之外的另一块磁盘区域
  - `setxattr(path, name, value, size, flags) → ext2_xattr_set` — xattr 存在 `inode->i_file_acl` 额外块 (`fs/ext2/xattr.c → ext2_xattr_set`)
  - `ext2_xattr_header` (magic=0xEA020000) → `ext2_xattr_entry[]` (e_name_index, hash, name, value_start, value_len) → value 区 (`fs/ext2/xattr.c → ext2_xattr_set`)
  - ACL 是 xattr 特例: `system.posix_acl_access → posix_acl_from_xattr → ext2_set_acl` (`fs/ext2/acl.c → ext2_set_acl`)
  - 新文件默认继承父目录 ACL → `ext2_permission → generic_permission → ext2_acl_permission_check`

### 6. 文件锁 — flock / fcntl / lockf 三类锁
  - `flock(fd, LOCK_SH|LOCK_EX|LOCK_UN) → ext2_flock → locks_lock_file_wait` — 文件级劝告锁 (`fs/locks.c → sys_flock`)
  - `fcntl(fd, F_SETLK, &flock) → f_setlk → posix_lock_file` — 范围锁, 可对文件任意字节范围加锁 (`fs/locks.c → posix_lock_file`)
  - `lockf` — POSIX 范围锁的简化库函数封装
  - 三者的区别: flock(文件级/劝告), fcntl(范围/强制位/记录/进程), lockf(范围/POSIX)
  - 锁状态可见: `/proc/locks`, `lslocks` 命令行工具

### 7. 收束
  - unlink 全链路: 目录项空洞→i_links_count 归零→orphan list→间接块递归释放→bitmap 清理——每一步都可能在中途崩溃
  - xattr/ACL 扩展了 inode 的元数据容量, 而 flock/fcntl 建立了并发访问的协调机制

---

### 核心悬念
**"ext2 无日志, unlink 递归释放中途崩溃的残局只能靠 fsck 全盘扫描修复——ext4 的 JBD2 日志如何用一个 journal 文件把这种 O(n) 崩溃恢复变成 O(1)？"**

→ 引出 08-jbd2-journal (先桥接到日志, 07 文件系统对比可在读完 01-06 后穿插)
