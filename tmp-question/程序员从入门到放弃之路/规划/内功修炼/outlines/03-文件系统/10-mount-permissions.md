# 挂载与权限管理 — `mount` 如何把磁盘文件系统接到 VFS，权限又如何层层判定

> Cluster B: 2 KPs | 依赖: 01-ext2-disk-layout | 读者基线: 理解 SuperBlock / dentry / inode / root inode
> 读者处境: 01-09 已经讲完 ext2/ext4/Btrfs/ZFS 的磁盘内部机制；本篇回到 VFS 接入点——这些磁盘结构怎样变成 `/mnt` 这条用户可见路径？
> 打开新视角: `mount` 做的不是"把分区显示出来"这么简单，而是**把磁盘上的 superblock/inode 读成内存里的 `struct super_block`/`struct inode`/`dentry`，再挂进命名空间树**；权限判断也不是只看 `chmod 755`，而是 VFS + capability + ACL 的叠层协议

---

### 概念依赖链

```
01 ext2 磁盘布局 + 02 inode + 03 目录项 → 本篇: mount 与权限
  ├─ §1 mount 链路(sys_mount → vfs_kern_mount → ext2_fill_super)
  ├─ §2 superblock/root dentry 建立(磁盘结构 → VFS 对象)
  ├─ §3 mode bits/capability(传统 RWX 权限)
  └─ §4 POSIX ACL(超越 RWX 的命名用户/组权限)
先讲: 挂载链路 → root 对接 → 基础权限 → 细粒度 ACL
后续依赖: 11-nfs-network-fs(当文件系统不在本地磁盘而在远端服务器)
```

### 叙事顺序

1. 问题引入——`mount -t ext2 /dev/sda1 /mnt` 之后，为什么 `/mnt` 这条路径突然就能 `ls` 了？是谁把磁盘上的 block group/inode 变成了 VFS 世界里的根目录？（**Aha: 挂载的本质是把磁盘格式翻译成 VFS 可遍历的 `super_block + root dentry + root inode` 三件套**）
   - 过渡: 先看 mount 命令如何走进文件系统驱动
2. `sys_mount` → `vfs_kern_mount` → `ext2_fill_super`
   - 过渡: super block 读进来后，怎么变成真正可遍历的根目录？
3. root inode + root dentry——把 inode#2 接到 VFS 树上
   - 过渡: 路径可见之后，访问控制首先看什么？
4. mode bits + capability——经典 RWX 权限判定
   - 过渡: `chmod 755` 不够表达"给 alice 单独开权限"时怎么办？
5. POSIX ACL——命名用户/组的细粒度授权
   - 过渡: 收束
6. 收束——挂载是接树，权限是判门禁 + Aha Moment

### 1. `mount` 系统调用 — 从块设备到文件系统实例

场景提示: 终端敲下 `mount -t ext2 /dev/sda1 /mnt` 时，内核首先在做什么——找目录、找块设备，还是分配 `super_block`？ [写作时展开]

关键设计: 挂载链路先选文件系统类型，再让该类型的 `mount` 回调把磁盘变成 VFS 实例（fs/namespace.c + fs/super.c + fs/ext2/super.c）：

```[pseudocode]
sys_mount(dev, dir, type="ext2", flags, data)
  → path_mount / do_new_mount
  → get_fs_type("ext2")
  → vfs_kern_mount(fs_type, flags, dev_name, data)
  → ext2_mount(...)
  → mount_bdev(..., ext2_fill_super)
      打开块设备
      分配/复用 super_block
      调 ext2_fill_super 读盘并初始化
```

Why: 为什么 VFS 不直接自己读磁盘 superblock，而要回调 `ext2_fill_super`？——**因为 VFS 只知道'挂载一个文件系统实例'，不知道 ext2/ext4/xfs 的 on-disk 格式差异**：块大小、superblock 位置、根 inode 号、操作集指针，都必须由具体文件系统自己解释。VFS 负责搭框架，ext2 负责翻译磁盘格式。 [内核: 01 篇的 superblock 字段，此刻第一次从磁盘描述变成运行时对象初始化参数]

比喻锚点: `mount` 像海关入境——VFS 是边检大厅，只知道要接待一个外来客；真正翻译护照格式、确认这个人来自哪个国家的是 ext2 驱动自己的 `fill_super`。 [写作时展开]

### 2. `ext2_fill_super` — 把磁盘上的 superblock 和 root inode 变成 VFS 根

场景提示: 01 篇里我们只知道磁盘上有 superblock、inode table；挂载时，这些字节是怎么变成 `sb->s_root` 的？ [写作时展开]

关键设计: `ext2_fill_super` 先把磁盘 superblock 读进来，再把 root inode#2 接成 root dentry（fs/ext2/super.c）：

```[pseudocode]
ext2_fill_super(sb, data, silent)
  → sb_bread(...): 读磁盘 superblock
  → 校验 s_magic == 0xEF53
  → sb_set_blocksize / parse_options / ext2_check_descriptors
  → sb->s_op = &ext2_sops
  → 初始化 ext2_sb_info(s_groups_count / s_inodes_per_group / ...)

  → ext2_iget(sb, EXT2_ROOT_INO=2)
     读取根 inode 的磁盘内容
  → d_make_root(root_inode)
  → sb->s_root = root_dentry
```

Why: 为什么 root inode 必须先变成 `dentry`，而不是只把 inode 挂到 `super_block` 上？——**VFS 遍历路径时走的是 dentry 树，不是 inode 表**：inode 只描述"这个目录是什么"，dentry 才描述"这个名字在命名空间树里挂在哪"。**所以挂载的完成标志不是'root inode 读出来了'，而是'`sb->s_root` 已经是一个可遍历的根 dentry'。** [内核: 03 篇创建文件是把名字绑定到 inode；挂载根目录时做的是最大的那次绑定——把名字空间入口 `/mnt` 绑定到 root dentry]

比喻锚点: superblock 像城市总规，root inode 像市政府档案，root dentry 才是挂在城市入口的那块"欢迎来到本市"路牌——没路牌，路人无法从命名空间进入这座城。 [写作时展开]

### 3. RWX + capability — 传统权限判定并不只是 `chmod 755`

场景提示: 一个进程试图 `open("/mnt/a.txt", O_WRONLY)`，VFS 到底是如何判断它有没有写权限的？ [写作时展开]

关键设计: 经典权限判定从 mode bits 出发，但 capability 可以越过它们（fs/namei.c 等）：

```[pseudocode]
open / may_open
  → inode_permission(inode, MAY_READ/MAY_WRITE/MAY_EXEC)
  → generic_permission / ACL 钩子

传统 mode bits 判定
  1) 先看当前进程 fsuid/fsgid
  2) 若 fsuid == inode->i_uid → 用 owner 位
  3) else 若进程属于 inode->i_gid → 用 group 位
  4) else → 用 other 位

capability 旁路
  某些场景 CAP_DAC_OVERRIDE / CAP_DAC_READ_SEARCH 可越过普通 DAC 检查
```

Why: 为什么 Linux 权限不直接做成"只看 rwx 三组位"？——**因为 VFS 要同时支持普通用户、组协作、root/特权进程以及后面的 ACL**：mode bits 是最低共同语义，capability 则把"特权"从 uid=0 粗暴特判里拆出来。**所以 root 之所以像万能钥匙，不是 inode 里有 root 特别位，而是 capability 框架在 DAC 之上又加了一层旁路能力。** [man 7 capabilities: CAP_DAC_OVERRIDE / CAP_DAC_READ_SEARCH 的含义]

比喻锚点: RWX 像门禁卡的三档权限（业主/住户组/访客），capability 像物业总控卡——它不是改门锁规则，而是在门锁之上再给某些人发一张能旁路的主卡。 [写作时展开]

### 4. POSIX ACL — 当三组 rwx 位表达不够时

场景提示: 你想让 `alice` 对文件有 `rw`，但不想让同组其他人也有 `rw`，`chmod` 三组位根本表达不了，怎么办？ [写作时展开]

关键设计: POSIX ACL 在 owner/group/other 之外，再引入命名用户/组条目和 mask（fs/posix_acl.c + 文件系统 ACL 钩子）：

```[pseudocode]
ACL 条目概念
  USER_OBJ(owner)
  USER(named user)
  GROUP_OBJ(group)
  GROUP(named group)
  MASK
  OTHER

访问时
  先匹配 owner / named user / group / named group / other
  再用 MASK 限制 group 类和 named user/group 的最大权限

效果
  chmod = 三档广播权限
  ACL   = 点名授权 + 上限裁剪
```

Why: 为什么 ACL 还需要 `MASK` 这层，看起来很绕？——**因为一旦有多个命名用户/组条目，就需要一个统一上限来和传统 group 语义兼容**：不然 `chmod g-w` 之后，命名组/命名用户是否还能写就会失控。`MASK` 的存在，就是把"ACL 扩展出来的细粒度授权"重新压回可管理的上界。 [内核: 06 篇的 xattr 已讲过 ACL 物理上常挂在扩展属性块里；这里补的是它的判定语义]

比喻锚点: ACL 像门卫的白名单——rwx 只是大楼统一规则，ACL 则允许你单独写一张"Alice 可以进机房、Bob 只能进会议室"的名单，而 MASK 像保安队长规定的"这栋楼今晚最多开放到几层"。 [写作时展开]

### 5. 收束

回到 `mount` 和权限管理这两个看似无关的问题：
- `mount` 解决的是"这套磁盘格式如何接进 VFS 命名空间"
- 权限解决的是"命名空间接进来后，谁能穿过这扇门"

完整链路：

```[pseudocode]
mount()
  → vfs_kern_mount → ext2_fill_super
  → 读 superblock / 读 root inode#2 / d_make_root
  → sb->s_root 可遍历

open/read/write
  → inode_permission
  → mode bits / capability / ACL 共同决定是否放行
```

**Aha Moment**: "挂载的真正完成，不是'块设备打开了'，而是**root inode 已经被接成 `sb->s_root`，整个磁盘格式第一次变成 VFS 可遍历的树**；权限判断的真正本质，也不是'看一眼 755'，而是**在 mode bits、capability、ACL 三层语义上做门禁决策**。"
**回答读者三问**: ①`mount` 后为什么 `/mnt` 突然可见= root dentry 接进命名空间了；②root 目录为何从 inode#2 开始= ext2 约定的根 inode 号；③为什么 `chmod` 不够用= 细粒度授权要靠 ACL，特权旁路要靠 capability。

---

### 核心悬念

**"如果文件系统根本不在本地块设备，而在另一台服务器上——客户端又是如何拿着一个 opaque file handle，通过一次 NFSv4 COMPOUND 把 LOOKUP/OPEN/READ 串成一轮网络交互的？"**

→ 引出 11-nfs-network-fs — 网络文件系统——本地 VFS 树接好了，下一篇看远端树如何投影到本地。