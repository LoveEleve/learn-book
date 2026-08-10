# 02 — Inode 结构与间接块链：15 个指针如何撑起 TB 级文件

> Cluster A: 4 KPs | 依赖: 01-ext2-disk-layout | 读者基线: 理解 Block Group/Block Size/SuperBlock 布局

---

### 1. `struct ext2_inode` 完整字段 — 文件的"身份证"
  - `__le16 i_mode;` — 低 9bit: rwx×ugo, 高 3bit: IFREG(0x8000)/IFDIR(0x4000)/IFLNK(0xA000)/IFCHR(0x2000)/IFBLK(0x6000)/IFIFO(0x1000)/IFSOCK(0xC000) (`fs/ext2/ext2.h → struct ext2_inode`)
  - `__le16 i_uid;` / `__le16 i_gid;` — 所有者/组
  - `__le32 i_size;` — 文件实际大小(字节), 与 `i_blocks` 独立
  - `__le32 i_atime;` / `i_ctime;` / `i_mtime;` / `i_dtime;` — 四个时间戳: 访问/变更/修改/删除
  - `__le16 i_links_count;` — 硬链接数, 为 0 时触发删除
  - `__le32 i_blocks;` — 占用 512B 扇区数 (含间接块自身占用的块)
  - `__le32 i_flags;` — EXT2_SECRM_FL/UNRM_FL/COMPR_FL/SYNC_FL/IMMUTABLE_FL/APPEND_FL/NODUMP_FL/NOATIME_FL (`fs/ext2/ext2.h → struct ext2_inode`)

### 2. `i_block[15]` — 四种寻址层级的容量模型
  - `i_block[0-11]` — 12 个直接块, 4KB block→48KB 直接寻址
  - `i_block[12]` — 单重间接: 1 block 存 `block_size/4` 个块号, 4KB→1024 块→4MB
  - `i_block[13]` — 双重间接: `(block_size/4)^2`, 4KB→1024²=1M 块→4GB
  - `i_block[14]` — 三重间接: `(block_size/4)^3`, 4KB→1024³=1G 块→4TB (`fs/ext2/inode.c:ext2_block_to_path`)

### 3. 间接块解析 — `ext2_block_to_path` + `ext2_get_branch`
  - `ext2_block_to_path(inode, iblock, offsets, &boundary)` — 计算逻辑块号在第几级 (`fs/ext2/inode.c → ext2_block_to_path`)
  - `chain[0].bh = sb_bread(block = inode->i_block[i])` — 读 inode 中的直接/间接指针
  - `chain[1].bh = sb_bread(block = chain[0].p + offsets[1])` — 读下一级间接块
  - 逐级 traverse 直到最终数据块, `bh->b_blocknr` = 物理块号
  - `boundary` 标志: 当 iblock 触及间接边界时置位, 用于预分配决策

### 4. 目录项 `ext2_dir_entry_2` — 目录就是文件
  - `struct ext2_dir_entry_2 { __le32 inode; __le16 rec_len; __u8 name_len; __u8 file_type; char name[255]; }` (`fs/ext2/ext2.h → struct ext2_dir_entry_2`)
  - `rec_len` 必须 4 字节对齐, `inode=0` 为空洞(已删除条目)
  - `file_type`: EXT2_FT_REG_FILE=1 / FT_DIR=2 / FT_SYMLINK=7
  - 查找: `ext2_find_entry → ext2_get_page → 逐 entry 比对 name_len + name` (`fs/ext2/dir.c → ext2_find_entry`)
  - 新建: `ext2_add_link → 找 rec_len ≥ needed 的空洞 → ext2_set_de_type` (`fs/ext2/dir.c → ext2_add_link`)

### 5. 收束
  - `i_block[15]` 的四级寻址 (直接/单重/双重/三重) 在 inode 大小固定 128B 的前提下实现 48KB→4TB 的跨度
  - 目录的本质是存储 `ext2_dir_entry_2` 数组的普通文件, inode=0 的空洞设计使删除后无需重整

---

### 核心悬念
**"touch a.txt 时, VFS 如何一层层穿透 dentry cache→inode→bitmap, 把一个空文件在磁盘上真正落地？"**

→ 引出 03-file-creation-touch
