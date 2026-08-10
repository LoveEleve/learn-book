# 10 — 挂载与权限管理: mount 全链路 + RWX/ACL/capability

> Cluster B: 2 KPs | 依赖: 01-ext2-disk-layout (SuperBlock 字段) | 读者基线: 理解 dentry/inode/SB 概念

---

### 1. `mount` 系统调用 — 从命令行到 `mount_bdev`
  - `mount -t ext2 /dev/sda1 /mnt → sys_mount → do_mount → path_mount → do_new_mount` (`fs/namespace.c → do_new_mount`)
  - `vfs_kern_mount(type=ext2_fs_type, flags, dev_name, data)` (`fs/super.c → vfs_kern_mount`)
  - `ext2_mount → mount_bdev(fs_type, flags, dev_name, data, ext2_fill_super)` (`fs/ext2/super.c → ext2_fill_super`)
  - `blkdev_get_by_path(dev_name) → bd_claim` — 打开块设备 (`fs/block_dev.c → blkdev_get_by_path`)

### 2. `ext2_fill_super` — 磁盘 SuperBlock 到内存 `struct super_block`
  - `sget(fs_type, ext2_test_super, ext2_set_super, flags, sb)` — 未找到 → `alloc_super(type, flags)` (`fs/super.c → sget`)
  - `sb->s_op = &ext2_sops` — 绑定 ext2 操作集 (`fs/ext2/super.c → ext2_fill_super`)
  - `sb_bread(sb, 1)` → 读 block 1: `es = (struct ext2_super_block*)bh->b_data` (`fs/ext2/super.c → ext2_fill_super`)
  - 验证 `s_magic == 0xEF53` → `sb_set_blocksize → parse_options → ext2_setup_super → ext2_check_descriptors` (`fs/ext2/super.c → ext2_fill_super`)
  - 创建 sbi (ext2_sb_info): 缓存 `s_inodes_per_group`, `s_blocks_per_group`, `s_desc_per_block` (`fs/ext2/super.c → ext2_fill_super`)

### 3. Root Inode 创建与 dentry 对接 — `d_make_root`
  - `iget_locked(sb, EXT2_ROOT_INO=2)` — 分配 root inode (inode# 永远为 2) (`fs/ext2/super.c → ext2_fill_super`)
  - `ext2_read_inode(inode) → inode_init_owner` — 从磁盘加载 root inode 内容 (`fs/ext2/inode.c → ext2_read_inode`)
  - `d_make_root(inode) → sb->s_root` — 创建 root dentry, 挂在 super_block 上 (`fs/dcache.c → d_make_root`)
  - 自此 `/mnt` 路径在 VFS 层完整: `task->fs->root` → `mount` → `dentry` → `inode` → `ext2_inode`

### 4. RWX 权限检查 — `generic_permission` 三步判断
  - `may_open → inode_permission → generic_permission(inode, mask)` (`fs/namei.c → generic_permission`)
  - Step 1: `mode & 0007` — 检查 RWX 位
  - Step 2: `uid_eq(inode->i_uid, fsuid)` → owner 权限(高 3bit) → `in_group_p(inode->i_gid)` → group 权限(中 3bit) → other 权限(低 3bit)
  - Step 3: `capable_wrt_inode_uidgid(CAP_DAC_OVERRIDE)` — root capability 旁路 (`kernel/capability.c → capable_wrt_inode_uidgid`)

### 5. POSIX ACL — 超越 RWX 的细粒度权限
  - ACL entries 优先级遍历: `ACL_USER_OBJ(owner) → ACL_USER(name) → ACL_GROUP_OBJ(group) → ACL_GROUP(name) → ACL_MASK → ACL_OTHER` (`fs/posix_acl.c → posix_acl_permission`)
  - `setfacl -m u:alice:rw file` — 为特定用户授权
  - `setfacl -m g:staff:r file` — 为组授权
  - `getfacl file` — 查看 ACL, 含 default ACL (新建文件继承)
  - 穿透 `mode` 位: ACL_MASK 限制所有 named user/group 的最大权限 (`fs/posix_acl.c → posix_acl_permission`)

### 6. 收束
  - mount 全链路: blkdev_get → alloc_super → sb_bread(读 SB) → iget(ROOT_INO=2) → d_make_root → 挂载完成
  - 权限检查顺序: capability(CAP_DAC_OVERRIDE) > ACL entries > mode bits

---

### 核心悬念
**"当文件不在本地磁盘上而在另一台服务器的磁盘上——NFS v4 的 COMPOUND 操作如何把 LOOKUP+OPEN+READ 三次 RPC 合并为一次网络往返？file handle 这个 opaque blob 在客户端和服务端之间传递了什么秘密？"**

→ 引出 11-nfs-network-fs
