# 09 — COW 与快照: Btrfs/ZFS/LVM 的写时复制原理

> Cluster B: 3 KPs | 依赖: 08-jbd2-journal | 读者基线: 理解 ext4 journal 的崩溃恢复模型

---

### 1. Btrfs COW 原理 — 改一块, 写新块, 指针切换到新根
  - COW 核心: tree block 修改时 → 写新 block 到空闲空间 → 更新父节点指针指向新 block → 所有未修改 block 共享 (`fs/btrfs/ctree.c → btrfs_search_slot`)
  - 根节点原子切换: 所有子节点 COW 完成后, 原子更新 root 指针 → 要么全可见要么全不可见 (`fs/btrfs/disk-io.c → write_ctree_super`)
  - 无需 journal: COW 的树更新顺序本身就是"日志"——只在新数据完整写出后才切指针

### 2. Subvolume 与快照 — 冻结根指针的瞬间
  - Subvolume: 独立子树, `btrfs subvolume create /mnt/@` — 可独立挂载 (`fs/btrfs/ioctl.c → btrfs_ioctl_snap_create`)
  - 快照: `btrfs subvolume snapshot /mnt/@ /mnt/@snap` — 创建一个共享所有 block 的新根指针 (`fs/btrfs/ioctl.c → btrfs_ioctl_snap_create`)
  - 写操作 → 仅被修改的 block COW → 新位置 → 共享不变的 block — 快照的存储开销无限趋于零
  - `btrfs send /mnt/@snap_old → btrfs receive /mnt/@backup` — 增量快照传输 (`fs/btrfs/send.c → btrfs_send`)

### 3. Checksum — 静默数据损坏的最后防线
  - 写入: `csum_tree_block(eb, csum)` — 对每个 extent buffer 计算校验和 (`fs/btrfs/disk-io.c → csum_tree_block`)
  - 读取: `csum_verify(eb)` — 读 block→校验 csum, 失败 → mirror 重试 (RAID1/RAID10) (`fs/btrfs/disk-io.c → btrfs_check_eb`)
  - 若所有 mirror 失败: `btrfs_readpage_end_io` → EIO 报错给用户 (`fs/btrfs/extent_io.c → btrfs_readpage_end_io`)
  - 去重: `duperemove /mnt → ioctl(BTRFS_IOC_FILE_EXTENT_SAME)` → byte-by-byte 比对 → 共享 extent (`fs/btrfs/ioctl.c → BTRFS_IOC_FILE_EXTENT_SAME`)

### 4. ZFS 快照 — pool 级别的 COW
  - `zfs snapshot pool/fs@snap` → COW dnode → 瞬间完成 (`module/zfs/dmu_send.c → dmu_send_obj`)
  - `zfs rollback pool/fs@snap` → 原子回滚
  - `zfs clone pool/fs@snap pool/clone` → 写前复制, 可写 clone
  - `zfs send pool/fs@snap | zfs receive pool/backup` — full 传输 (`module/zfs/dmu_send.c → dmu_send_obj`)
  - `zfs send -i @old @new | zfs receive pool/backup` — incremental, 只传输变化的 block
  - ZFS vs Btrfs: ZFS 生产 15+ 年无重大 bug, Btrfs RAID5/6 仍有稳定性问题

### 5. LVM 快照 — 块设备层的 COW
  - `lvcreate -L 2G -s -n snap /dev/vg/lv` → 快照卷 (`drivers/md/dm-snap.c → dm_snap_ctr`)
  - COW 机制: 原卷被写 → 先 copy 原内容到快照卷(COW 区) → 然后允许写 → 快照卷存"修改前数据"
  - `mount -o ro /dev/vg/snap /mnt/snap` — 只读挂载见到"冻结时刻"的数据
  - 与 Btrfs/ZFS 的区别: LVM 同步 COW(写放大), Btrfs/ZFS 异步 COW(零额外 IO)

### 6. 收束
  - COW 三步: 修改→写新位置→切指针, 新数据未完成时原指针不动, 天然崩溃安全
  - Btrfs 的 COW+checksum 消除了 journal 的双写开销, ZFS 将 COW 扩展到 pool 级别的 send/receive 异地容灾

---

### 核心悬念
**"mount /dev/sda1 /mnt 这个看似简单的命令在 VFS 层做了哪些事——SuperBlock 如何从磁盘第一个块被读到内存变成 struct super_block？新创建的 root inode 如何对接 dentry 树, 让 /mnt 路径在用户空间可访问？"**

→ 引出 10-mount-permissions
