# 07 — 文件系统对比: ext4 / XFS / Btrfs / ZFS 四代进化

> Cluster A→B 过渡: 4 KPs | 依赖: 01-06 完成 ext2 全链路 | 读者基线: 理解 ext2 的间接块/位图/无日志三大限制

---

### 1. ext4 — ext2 + extent 树 + JBD2 日志
  - Extent 树替代间接块: `ext4_extent { ee_block, ee_len, ee_start_hi, ee_start_lo }` — 连续块用一对 (起始,长度) (`fs/ext4/extents.c → struct ext4_extent`)
  - `ext4_ext_tree_init` → Htree 目录索引: `ext4_dir_entry_2` 有序树, 大幅加速大目录查找 (`fs/ext4/namei.c → ext4_add_entry`)
  - 容量: max 1EB (文件系统)/16TB (单文件), 默认 Linux/RHEL/SUSE
  - 块分配器: `ext4_mb_regular_allocator` — buddy 算法查找连续块供 extent (`fs/ext4/mballoc.c → ext4_mb_regular_allocator`)

### 2. XFS — SGI 的 64 位 B+tree 引擎
  - 全 B+tree: inode(`xfs_inode`)→extent(`xfs_bmbt_rec`)→目录(`xfs_da_btree`)→空闲空间(`xfs_alloc_btree`)→引用计数(`xfs_refcount_btree`) (`fs/xfs/libxfs/xfs_btree.h → xfs_btree_core`)
  - 元数据 journal: 只记元数据, 数据不记 (`fs/xfs/xfs_log.c → xfs_log_write`)
  - 容量: max 8EB, 在线扩展(不可缩), RHEL7/8 默认
  - 分配组(AG): 并行 IO——每个 AG 独立的空闲空间 B+tree, 多核无锁竞争 (`fs/xfs/libxfs/xfs_alloc.c → xfs_alloc_ag_vextent`)

### 3. ext4 vs XFS — 选型决策表
  - ext4 优势: 更广泛应用, 可回退到 ext2/ext3, 社区更大, recovery 工具成熟
  - XFS 优势: 更大扩展(8EB), 更高并发(AG 并行), 在线 defrag, 原生 quota journal
  - 场景: ext4→通用/桌面/嵌入式, XFS→大文件/高并发服务器/NAS/流媒体

### 4. Btrfs — COW + 快照 + checksum 的新一代
  - COW: tree block 修改→新位置写→指针更新→旧数据保留 (`fs/btrfs/ctree.c → btrfs_search_slot`)
  - 子卷/快照: `btrfs subvolume snapshot → btrfs send/receive` 增量备份 (`fs/btrfs/ioctl.c → btrfs_ioctl_snap_create`)
  - Checksum: `csum_tree_block → csum_verify` — 读取时校验, 失败 → mirror 重试或报 EIO (`fs/btrfs/disk-io.c → csum_tree_block`)
  - 压缩(zlib/lzo/zstd)+去重(`duperemove→btrfs-extent-same`)+ RAID — Fedora 默认

### 5. ZFS — 128 位宇宙级文件系统
  - 核心概念: pool(存储池)→dataset→dnode→checksum+快照 (`module/zfs/dmu.c → dmu_tx_assign`)
  - 128 位寻址: 理论容量超过宇宙原子数
  - `zfs snapshot pool/fs@snap → zfs send -i @old @new | zfs receive` — 异地容灾
  - L2ARC(读缓存)+SLOG(写日志) — 混合存储层次, `openzfs` Linux 实现
  - 对比 Btrfs: ZFS 更成熟(15+年生产), Btrfs RAID5/6 仍有稳定性问题

### 6. 收束
  - ext2→ext4→XFS→Btrfs→ZFS 进化主线: 间接块→extent 树→B+tree 索引→COW+快照+校验
  - 选型: 稳定通用→ext4, 大并发→XFS, 快照去重→Btrfs, 极致完整性→ZFS

---

### 核心悬念
**"ext4 的 JBD2 日志内部, 一个 `handle_t` 事务如何从一个 buffer 的 dirty_metadata 调用开始, 经过 commit→checkpoint→replay 三阶段完成原子性保证？"**

→ 引出 08-jbd2-journal
