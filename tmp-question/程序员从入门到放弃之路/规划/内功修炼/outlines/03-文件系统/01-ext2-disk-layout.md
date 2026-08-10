# 01 — Ext2 磁盘布局：从 Boot Sector 到 Block Group 的完整六段结构

> Cluster A: 5 KPs | 依赖: 无 | 读者基线: 理解磁盘概念(扇区/块/分区)

---

### 1. 整体磁盘布局 — 六段式 Block Group 结构
  - 磁盘起始: Boot Sector (逻辑扇区0, 512B, 含 MBR 分区表) (`arch/x86/boot/tools/build.c:1`)
  - Block Group 按序排列 BG0→BG1→...→BGN, 每个内六段: SuperBlock→GDT→Block Bitmap→Inode Bitmap→Inode Table→Data Blocks
  - Block Size 创建时指定 (1KB/2KB/4KB), `s_log_block_size` 控制 (1024<<log) (`mkfs.ext2 -b 4096`)
  - bg# 坐标定位: `bg = ino / s_inodes_per_group`, BG内偏移=`ino % s_inodes_per_group`
  - SuperBlock 备份策略: BG0/BG1 全量备份, 及 3^n 号 BG (3,5,7,9,25...) 稀疏备份

### 2. SuperBlock 完整字段 — `struct ext2_super_block` 30+ 字段
  - `__le32 s_inodes_count;` / `s_blocks_count;` — inode/block 总数 (`include/linux/ext2_fs.h → struct ext2_super_block`)
  - `__le32 s_r_blocks_count;` — root 保留块 (5%), 触发 `ENOSPC` 前最后防线
  - `__le32 s_free_blocks_count;` / `s_free_inodes_count;` — 运行时空闲计数器
  - `__le32 s_first_data_block;` — 0(4KB block) 或 1(1KB block)
  - `__le32 s_log_block_size;` — `1024 << log`, 如 log=2→4KB
  - `__le32 s_blocks_per_group;` — 每 BG block 数, 通常 `8 * s_log_block_size MB`, 4KB→32768=128MB (`fs/ext2/super.c:ext2_fill_super`)
  - `__le16 s_magic;` — 0xEF53, 挂载时验证 (ext2_fill_super→validate)
  - `__le16 s_state;` — 0=干净 / 1=错误 / 2=孤儿文件
  - `__le16 s_mnt_count;` / `s_max_mnt_count;` — 挂载计数, 达上限强制 fsck
  - `char s_volume_name[16];` / `char s_last_mounted[64];` — 卷标与最后挂载点
  - 读取链路: `ext2_fill_super → sb_set_blocksize → parse_options → ext2_check_descriptors → ext2_setup_super` (`fs/ext2/super.c → ext2_fill_super`)

### 3. GDT (Group Descriptor Table) — 每个 BG 的元数据入口
  - `struct ext2_group_desc`: `bg_block_bitmap` / `bg_inode_bitmap` — bitmap 绝对块号 (非 BG 内偏移) (`fs/ext2/ext2.h → struct ext2_group_desc`)
  - `bg_inode_table` — inode table 起始块号
  - `bg_free_blocks_count` / `bg_free_inodes_count` / `bg_used_dirs_count` — 每个 BG 的统计计数器
  - GDT 总大小: `s_groups_count × group_desc_size` (ext2=32B, ext4=64B)
  - 读取: `ext2_get_group_desc(sb, bg, &bh) → bh->b_data + offset → gdp`

### 4. Block Bitmap 与 Inode Bitmap — 位图分配基础
  - Block Bitmap: 每 bit 对应一个 block, 0=空闲/1=已用 (`fs/ext2/balloc.c → ext2_free_blocks`)
  - 例: block size=4KB, `s_blocks_per_group`=32768, bitmap=4KB block→32768/8=4096 字节→恰好填满
  - 操作: `ext2_find_next_zero_bit(bitmap, end)` 找空闲 → `ext2_set_bit(i, bitmap)` 标记已用 → `ext2_clear_bit(i, bitmap)` 释放
  - Inode Bitmap 同理, 每 bit 对应一个 inode

### 5. 收束
  - Ext2 磁盘布局六段式的设计使一切元数据有序: SuperBlock 定义全局参数, GDT 索引每个 BG, Bitmap 管理分配, Inode Table 存文件元数据
  - 同一张磁盘上 block/inode 的逻辑地址到物理地址映射全部由这六段完成

---

### 核心悬念
**"inode 中的 i_block[15] 数组如何能让一个 2 字节的 inode 承载 4TB 的文件？"**

→ 引出 02-ext2-inode-indirect-blocks
