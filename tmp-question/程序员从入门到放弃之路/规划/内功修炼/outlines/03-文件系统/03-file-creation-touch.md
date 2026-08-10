# 03 — 文件创建全链路: "touch a.txt" 从 VFS 到磁盘的每一步

> Cluster A: 3 KPs | 依赖: 02-ext2-inode-indirect-blocks | 读者基线: 理解 inode 结构/i_block/目录项格式

---

### 1. `link_path_walk` — 逐分量路径解析
  - `sys_open("test/a.txt", O_CREAT|O_WRONLY, 0644) → do_filp_open → path_openat → link_path_walk(&nd, name)` (`fs/namei.c → link_path_walk`)
  - 第一分量 `test`: `walk_component → lookup_fast → __d_lookup` — dentry cache 命中 → `follow_managed → __follow_mount` — 穿越挂载点 (`fs/namei.c → walk_component`)
  - 第二分量 `a.txt`: `lookup_fast` 未命中 → `lookup_slow → d_alloc_parallel` 分配新 dentry (`fs/dcache.c → d_alloc_parallel`)
  - `dir->i_op->lookup(dir, dentry, flags) ← ext2_lookup` → `ext2_find_entry` — 筛选目录项, 返回 `-ENOENT` (`fs/ext2/namei.c → ext2_lookup`)

### 2. `O_CREAT` 分叉 — `do_last → lookup_open → vfs_create`
  - flags 含 `O_CREAT` → `do_last` 中调用 `lookup_open` (`fs/namei.c → lookup_open`)
  - `atomic_open` 尝试 → 文件不存在 → 降级到 `vfs_create(dir, dentry, mode, true)` (`fs/namei.c → vfs_create`)
  - 调用文件系统级的 `ext2_create(dir, dentry, mode, bool)` (`fs/ext2/namei.c → ext2_create`)

### 3. `ext2_new_inode` — 选组/bitmap/分配三部曲
  - 选组: 目录→`find_group_dir`(均匀分布), 普通文件→`find_group_orlov`(父目录组优先) (`fs/ext2/ialloc.c → ext2_new_inode`)
  - Inode bitmap: `read_inode_bitmap(sb, bg) → bh → ext2_find_next_zero_bit` 找空闲 → `ext2_set_bit(ino, bh->b_data)` 标记 (`fs/ext2/ialloc.c → ext2_new_inode`)
  - 填充 inode: `new_inode(sb, bg, ino) → ext2_read_inode` → `i_block[0..14]=0` → `i_uid=current_fsuid(), i_gid=current_fsgid(), i_mode=mode` → `ext2_write_inode` 脏标记 (`fs/ext2/ialloc.c → ext2_new_inode`)

### 4. `ext2_add_link` — 目录项插入与空洞合并
  - 定位父目录数据块, 找到 `rec_len >= needed` 的空洞 (`fs/ext2/dir.c → ext2_add_link`)
  - 写入 `ext2_dir_entry_2{ inode=新inode, name_len, name, file_type }` (`fs/ext2/dir.c → ext2_add_link`)
  - 空洞 merge: 前任 `pde->rec_len += de->rec_len` — 不需碎片整理
  - `mark_buffer_dirty_inode(bh, dir)` — 标记脏, 等待 writeback (`fs/ext2/dir.c → ext2_add_link`)

### 5. 孤儿文件 — 创建中途崩溃的防护
  - inode 刚分配时 `i_links_count=0`, 加入 `sbi->s_orphan` 链表 (`fs/ext2/ialloc.c → ext2_new_inode`)
  - 创建完成后 → `inode_inc_link_count → i_links_count=1` → 从 orphan list 移除
  - 若中途崩溃: 重启后 `ext2_orphan_cleanup(sb)` → 遍历 orphan list → 删除或修复 (`fs/ext2/super.c → ext2_orphan_cleanup`)
  - ext3/ext4 `has_journal` 后不再需要 orphan list, 改用日志回滚

### 6. 收束
  - touch 全链路: VFS 路径解析→dentry cache→O_CREAT 分叉→inode bitmap 分配→目录项插入, 每一步的失败都通过 orphan list 保证不留下半成品
  - 孤儿文件机制是 ext2 无日志时代的事务保障雏形

---

### 核心悬念
**"write() 返回成功时数据还在 Page Cache 里——ext2 的 write_begin→copy→mark_dirty 链路把数据卡在了哪里？writeback 什么时候才真正把页写入磁盘？"**

→ 引出 04-write-data-flow
