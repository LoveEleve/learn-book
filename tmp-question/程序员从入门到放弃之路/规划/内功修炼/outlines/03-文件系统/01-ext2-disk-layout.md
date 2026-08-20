# Ext2 磁盘布局 — 从 Boot Sector 到 Block Group 的六段结构

> Cluster A: 5 KPs | 依赖: 无 | 读者基线: 理解磁盘概念(扇区/块/分区)
> 读者处境: 阶段 3 开篇——内存篇刚收束（用户态 malloc 到物理页）；本篇回答"字节流怎么落到磁盘——磁盘上的一块区域怎么被组织成文件系统"
> 打开新视角: 文件系统=磁盘上的"城市分区规划"、六段结构让 4 次寻址(全局→BG→位图→数据块)成为可能、SuperBlock 是城市总规

---

### 概念依赖链

```
无前置(阶段3开篇) → 本篇: Ext2 磁盘布局(六段式 BG 结构 — 文件系统基石)
  ├─ §1 整体布局(六段式: SB→GDT→位图→inode表→数据块 — 本篇骨架)
  │    ├─ §2 SuperBlock(全局参数 — 依赖 §1 的第一段)
  │    ├─ §3 GDT(每 BG 入口 — 依赖 §1 的第二段)
  │    └─ §4 Block/Inode Bitmap(分配基础 — 依赖 §1 的位图段 + §3 的指针)
先讲: 整体 → 全局参数 → BG 入口 → 分配位图
后续依赖: 02-inode(文件元数据载体) / 03-文件创建(位图+inode 落地) / 04-写流程 / 10-挂载
```

### 叙事顺序

1. 问题引入——格式化一块空盘后，文件系统怎么知道"哪个扇区属于哪个文件"？（**Aha: 磁盘布局=城市分区规划——先划区，再在每区里分块记账**）
   - 过渡: 城市总规长什么样？——六段式
2. 整体磁盘布局——Boot Sector + Block Group 六段
   - 过渡: 全局参数谁定义？——SuperBlock
3. SuperBlock——全局参数 + 备份策略
   - 过渡: 全局知道了——每个 BG 怎么找到自己的位图和 inode 表？
4. GDT——每个 BG 的元数据入口
   - 过渡: 找到位图了——怎么分配一块？
5. Block/Inode Bitmap——位图分配基础
   - 过渡: 布局已齐——收束
6. 收束——六段布局如何支撑 4 次寻址 + Aha Moment

### 1. 整体磁盘布局 — 六段式 Block Group 结构

场景提示: 你格式化了一块 100GB 空盘——格式化后磁盘上"长出了"什么结构？为什么文件系统要分区管理而不是一整块？ [写作时展开]

关键设计: 磁盘最前 Boot Sector + 磁盘主体切分为等大 Block Group（每 BG 内部六段）：

```[pseudocode]
磁盘: [Boot Sector(逻辑扇区0, 512B, MBR/引导)] [BG0] [BG1] ... [BGN]
每个 BG 内六段: SuperBlock → GDT → Block Bitmap → Inode Bitmap → Inode Table → Data Blocks
Block Size: 创建时指定(1KB/2KB/4KB) — s_log_block_size = 1024 << log (mkfs.ext2 -b 4096)
BG 坐标(ino 从 1 开始): bg = (ino-1) / s_inodes_per_group; 组内偏移 = (ino-1) % s_inodes_per_group
SuperBlock 备份: BG0/BG1 全量 + 稀疏备份(sparse_super 特征) — 组号 = 3/5/7 的幂(3,5,7,9,25,27,49,81,125...)
```

Why: 为什么要把磁盘切分成"等大块组（BG）"而不是线性管理？——**分组 = 并行 + 局部性**：每个 BG 自带位图/inode 表，元数据与数据都在组内（就近）；多 BG 支持多线程并行分配（不同组互不竞争）；单个 BG 损坏只影响局部。**六段顺序固定 = 寻址可算**：给定 inode 号 → 公式直接算出它在哪个 BG 的哪个段——不需要全盘扫描。 [内核: "位置可计算"与阶段2-04 篇的 VMA 定位同一思想——不扫描, 算出来; 位图分配与阶段1-01 篇 Buddy 的 free_area 同构——位图找空闲] [man 8 mkfs.ext2: -b 指定块大小, 决定 s_log_block_size]

比喻锚点: 磁盘布局=城市分区规划——Boot Sector 是城市入口石碑，BG 是行政区（每个区有自己的"户籍册[inode表]、空地登记表[block bitmap]、人口登记表[inode bitmap]、区办公室[GDT]"），SuperBlock 是城市总规文件（每区都复印一份备份）。 [写作时展开]

### 2. SuperBlock 完整字段 — 文件系统的全局参数

场景提示: 挂载一个 ext2 分区时，内核怎么知道"这盘是不是 ext2、块多大、有多少 inode"？损坏了怎么恢复？ [写作时展开]

关键设计: `struct ext2_super_block`（include/linux/ext2_fs.h）30+ 字段——挂载第一步读它：

```[pseudocode]
总量: s_inodes_count / s_blocks_count — inode 与 block 总数
保留: s_r_blocks_count — root 保留块(默认 5%), ENOSPC 前的最后防线
运行: s_free_blocks_count / s_free_inodes_count — 空闲计数器
几何: s_first_data_block(0=4KB块 / 1=1KB块) / s_log_block_size(1024<<log)
      / s_blocks_per_group(4KB块→32768=128MB BG)
校验: s_magic = 0xEF53 — 挂载时验证 (ext2_fill_super → 校验)
状态: s_state(0=干净/1=错误/2=孤儿) / s_mnt_count / s_max_mnt_count(达上限强制 fsck)
标识: s_volume_name[16] / s_last_mounted[64]
读取链路: ext2_fill_super → sb_set_blocksize → parse_options → ext2_check_descriptors → ext2_setup_super (fs/ext2/super.c)
```

Why: 为什么 SuperBlock 要"全量+稀疏双备份"？——**SB 是文件系统的心脏，读不到它就挂载失败**：BG0/BG1 全量备份兜底（BG0 坏了用 BG1），稀疏备份（sparse_super 特征，现代 mkfs.ext2 默认开启）只存于组号为 3/5/7 的幂的 BG（3,5,7,9,25,27,49,81,125,243,343...），覆盖"同扇区物理损坏"场景；若未开启该特征（老格式），则每个 BG 都备份。**s_magic 是身份验证**（0xEF53 一眼认出 ext2）；**s_max_mnt_count 是体检周期**——挂载次数达上限强制 fsck，防止脏卸载的隐患累积。 **ext2 故意不做日志**：无 JBD2，写中途断电后元数据可能不一致，只能靠 fsck 全盘扫描修复（对照：ext4 用 JBD2 日志把一致性成本降到 O(日志大小)）。 [内核: ext2_fill_super 挂载链路在挂载篇展开——本篇只讲字段语义] [man 8 fsck: 强制检查与 s_max_mnt_count 的关系]

比喻锚点: SuperBlock=城市总规文件——记录"城市多大（总块数）、多少户籍（inode 数）、地块多大（块大小）、保留地多少（root 保留块）"；正本放市政府（BG0），副本散落各区（备份），每届市长上任先验总规真伪（s_magic 校验）。 [写作时展开]

### 3. GDT — 每个 BG 的元数据入口

场景提示: 内核要把文件 inode 落盘——怎么从 inode 号找到它在磁盘上的具体位置？ [写作时展开]

关键设计: `struct ext2_group_desc`（fs/ext2/ext2.h）——每 BG 一条，GDT 集中放在各 BG 的 SuperBlock 之后：

```[pseudocode]
bg_block_bitmap / bg_inode_bitmap — 位图的绝对块号(跨 BG 定位, 非组内偏移)
bg_inode_table — inode 表起始块号
bg_free_blocks_count / bg_free_inodes_count / bg_used_dirs_count — 本组统计
GDT 大小: s_groups_count × group_desc_size (ext2=32B, ext4=64B)
读取: ext2_get_group_desc(sb, bg, &bh) → bh->b_data + offset → gdp
```

Why: 为什么位图/inode 表用"绝对块号"而非组内偏移？——**绝对块号一次寻址直达**：拿到 bg_block_bitmap 就是磁盘块号，直接下发 I/O 读那一块——避免"组号→偏移→转换"的中间计算；**GDT 就是"每区的设施目录"**：区办公室（GDT）告诉你"本区空地登记表在哪块、户籍册从哪块开始"。inode 定位的起点就在这。 [内核: GDT 数组在内存中以 sb 描述符缓存——ext2_get_group_desc 从缓存取, 与阶段2-06 篇页缓存同属"磁盘数据的内存缓存"思想]

比喻锚点: GDT=区办公室的设施目录——"本区空地登记表在城东 3 号街（bg_block_bitmap 绝对位置）、户籍册从城北 7 号街起（bg_inode_table）"——不用问总规，问区办就行。 [写作时展开]

### 4. Block Bitmap 与 Inode Bitmap — 位图分配基础

场景提示: 创建文件要占一个 inode、写数据要占若干 block——内核怎么快速知道"哪块是空的"？ [写作时展开]

关键设计: 每 BG 两个位图（fs/ext2/balloc.c）——每 bit 对应一个 block/inode：

```[pseudocode]
Block Bitmap: 每 bit = 一个 block(0=空闲/1=已用)
  例: block=4KB, s_blocks_per_group=32768 → bitmap=32768/8=4096B=恰好一个 block
Inode Bitmap: 同理, 每 bit = 一个 inode
操作: ext2_find_next_zero_bit(bitmap, end) 找第一个空闲
      → ext2_set_bit(i, bitmap) 标记已用 → ext2_clear_bit(i, bitmap) 释放
```

Why: 为什么用"位图"而不是"链表"？——**位图是空间效率+随机访问的平衡**：1 bit 管 1 个 block（1KB 块→8MB 块组只要 1KB 位图；4KB 块→128MB 块组只要 4KB 位图），而链表要 8B 指针/块（32 倍空间）；且位图**按位索引**天然支持"找第一个空闲"（find_next_zero_bit 一次扫描 64 位）；回收时 set/clear 都是 O(1) 位操作。**代价**：分配方向固定（从前到后扫）→ 长期运行产生外部碎片——这正是间接块寻址与块分配策略存在的理由。 [内核: 位图分配 = 阶段1-01 篇 Buddy 的 free_area 同思想——"找第一个空闲"在磁盘层用 find_next_zero_bit, 在内存层用 order 链表]

比喻锚点: 位图=停车场的空位指示灯板——每格一个灯（bit），绿灯空红灯占；管理员扫一眼板子找第一个绿灯（find_next_zero_bit），停完把灯拨红（set_bit）。 [写作时展开]

### 5. 收束

回到"磁盘布局怎么支撑文件系统"：
- Boot Sector = 入口（引导）
- SuperBlock = 全局参数（城市总规 + 备份）
- GDT = 每 BG 的设施目录（绝对块号定位）
- 位图 = 分配账本（block/inode 空闲标记）
- Inode Table = 文件元数据（每个 inode 记录一个文件的名字/大小/数据块位置）
- Data Blocks = 数据本体

一次 inode 定位的完整寻址链：

```
ino → bg = (ino-1) / s_inodes_per_group → GDT[bg] → bg_inode_table + (ino-1)%s_inodes_per_group×inode_size → inode 落盘位置
```

**Aha Moment**: "文件系统不是'把文件排着放'，而是**先划区（BG）、每区配齐账本（位图+inode表+设施目录）**——给定任意 inode 号，用公式算出它在哪区的哪一段，全程 4 次寻址（总规→区目录→位图→数据）——**布局的优雅在于'位置可计算'**，这也是文件按索引定位（inode 索引块）与故障恢复（SB 备份 + fsck）的地基。"
**回答读者三问**: ①格式化后磁盘长出什么=六段式 BG 结构；②内核怎么知道块大小/总容量=SuperBlock 全局参数；③怎么找空闲块/空闲 inode=位图 find_next_zero_bit。

---

### 核心悬念

**"inode 里 15 个指针（i_block[15]）怎么支撑 4TB 的文件？直接指针、一级间接、二级间接怎么分工？"**

→ 引出 02-ext2-inode-indirect-blocks — inode 与间接块寻址——布局给了"位置怎么算"，下一篇讲"文件数据怎么索引"。