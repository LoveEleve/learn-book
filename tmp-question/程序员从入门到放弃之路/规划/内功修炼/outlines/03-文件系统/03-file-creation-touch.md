# 文件创建全链路 — `touch a.txt` 从路径解析到目录项落盘

> Cluster A: 3 KPs | 依赖: 02-ext2-inode-indirect-blocks | 读者基线: 理解 inode 结构/i_block/目录项格式
> 读者处境: 02 篇已经回答"文件数据怎么索引"；本篇回答"第一个文件怎么诞生——名字怎么登记、inode 怎么分配、位图怎么改"
> 打开新视角: `touch a.txt` 不是"凭空出现一个文件"，而是 VFS 先确认名字不存在，再由 ext2 改三本账——inode bitmap、group descriptor 计数器、父目录的数据块

---

### 概念依赖链

```
02-ext2-inode-indirect-blocks(inode/i_block/目录项) → 本篇: 文件创建全链路
  ├─ §1 路径解析(link_path_walk/lookup) — 依赖 02 篇目录项概念
  ├─ §2 O_CREAT 分叉(vfs_create → ext2_create) — 依赖 §1 文件不存在判定
  ├─ §3 ext2_new_inode(选组+bitmap+计数器) — 依赖 01 篇 BG/位图 + 02 篇 inode
  └─ §4 ext2_add_link(目录项插入) — 依赖 02 篇 ext2_dir_entry_2
先讲: 找名字 → 决定创建 → 分配 inode → 登记目录项
后续依赖: 04-写流程(Page Cache 脏页) / 05-读流程(lookup 后 read) / 06-文件删除(bitmap 回收)
```

### 叙事顺序

1. 问题引入——`touch a.txt` 只有 0 字节，为什么还要改磁盘？到底哪几本账被改了？（**Aha: 创建文件不是写数据，而是先把"名字→inode"这层索引关系落盘**）
   - 过渡: 第一步先确认这个名字不存在——怎么找？
2. 路径解析——`link_path_walk` / `lookup_fast` / `lookup_slow`
   - 过渡: 确认不存在后，`O_CREAT` 怎么把"查找"切成"创建"？
3. `O_CREAT` 分叉——`do_last` → `lookup_open` → `vfs_create` → `ext2_create`
   - 过渡: 文件系统拿到 create 请求后，先做哪一步？——分配 inode
4. `ext2_new_inode`——选组 + inode bitmap + 计数器更新
   - 过渡: inode 有了，但目录里还没这个名字——怎么登记？
5. `ext2_add_link`——目录项插入与父目录落盘
   - 过渡: inode 和目录项都齐了——收束
6. 收束——`touch` 改的是三本账（inode bitmap / BG 计数器 / 父目录目录项）+ Aha Moment

### 1. `link_path_walk` — 先确认这个名字不存在

场景提示: 你在空目录下执行 `touch a.txt`——内核怎么知道"a.txt 还没有"？为什么要先查再创？ [写作时展开]

关键设计: VFS 先做路径解析，再把最后一个分量交给具体文件系统判断是否存在（fs/namei.c + fs/ext2/namei.c）：

```[pseudocode]
sys_openat("test/a.txt", O_CREAT|O_WRONLY, 0644)
  → do_filp_open → path_openat → link_path_walk
  → 分量1: "test"
      lookup_fast → dentry cache 命中? 命中直接取 dentry/inode
      未命中 → lookup_slow
  → 分量2: "a.txt"(最后一段)
      dir->i_op->lookup(dir, dentry, flags) → ext2_lookup
      → ext2_find_entry(在父目录的数据块里逐个扫描 ext2_dir_entry_2)
      → 找到? 返回已有 inode : -EEXIST / 没找到? 返回 -ENOENT
```

Why: 为什么创建前必须先 `lookup` 一遍？——**POSIX 语义先于分配**：`open(O_CREAT|O_EXCL)` 需要原子地回答"这个名字此前存在吗"；只有先把目录扫一遍，才能决定是打开旧文件还是创建新 inode。**VFS 负责名字语义，ext2 负责磁盘账本**：VFS 做路径遍历、挂载点穿越、dcache 命中；ext2_lookup 只回答"这个目录文件里有没有这条名字记录"。 [内核: ext2_find_entry 扫描的正是 02 篇的 ext2_dir_entry_2 数组——目录就是普通文件] [man 2 open: O_CREAT/O_EXCL 的存在性语义]

比喻锚点: `touch` 前半段像去房管局查门牌——先翻门牌册（目录项）确认这栋楼里还没有"a.txt"这个住户名；没这一步就不能发新房产证（inode）。 [写作时展开]

### 2. `O_CREAT` 分叉 — 从查找切到创建

场景提示: 路径解析确认 `a.txt` 不存在后，内核是在哪一行决定"那就创建它"？ [写作时展开]

关键设计: `O_CREAT` 让 VFS 在最后一个分量处从 `lookup` 分叉到 `create`（fs/namei.c → fs/ext2/namei.c）：

```[pseudocode]
do_last / lookup_open
  → flags 含 O_CREAT && lookup 返回 -ENOENT
  → vfs_create(dir, dentry, mode, true)
  → dir->i_op->create(...) → ext2_create(...)
ext2_create
  → ext2_new_inode(dir, mode, &dentry->d_name)
  → ext2_set_file_ops(inode) + mark_inode_dirty(inode)
  → ext2_add_nondir(dentry, inode)
      = ext2_add_link(dentry, inode) + d_instantiate_new(dentry, inode)
```

Why: 为什么 `vfs_create` 不自己改位图，而要下沉到 `ext2_create`？——**VFS 不知道磁盘格式**：它只知道"需要一个新 inode + 一个名字绑定"；具体是 inode 表、位图、group descriptor，还是 extent tree、日志事务，完全由文件系统决定。**统一接口 = 多文件系统共享同一套 open 语义**：ext2/ext4/xfs 都走 `vfs_create`，但底下的 on-disk 实现各不相同。

比喻锚点: `vfs_create` 像政务大厅的受理窗口——窗口只收"我要办新证"，真正去盖章、找空白证件、写入登记簿的是各区分局（ext2_create）。 [写作时展开]

### 3. `ext2_new_inode` — 选组、位图、计数器三本账

场景提示: ext2 收到 `create` 请求后，怎么决定这个新文件的 inode 号？在哪个 Block Group 里找空位？ [写作时展开]

关键设计: `ext2_new_inode`（fs/ext2/ialloc.c）做三件事：选组、找位、记账：

```[pseudocode]
1) 选组
   if S_ISDIR(mode):
      OLDALLOC? find_group_dir : find_group_orlov
   else:
      find_group_other(parent)
   含义: 目录尽量分散, 普通文件尽量靠近父目录组

2) inode bitmap 找空位
   read_inode_bitmap(sb, group) → bitmap_bh
   ino = ext2_find_next_zero_bit(bitmap, EXT2_INODES_PER_GROUP, start)
   ext2_set_bit_atomic(..., ino, bitmap_bh->b_data)
   mark_buffer_dirty(bitmap_bh)
   ino = ino + group * INODES_PER_GROUP + 1   // inode 编号从 1 开始

3) 填充内存 inode + 更新组计数器
   inode->i_mode / i_uid / i_gid / i_ino / i_blocks=0 / i_atime=i_mtime=i_ctime=now
   memset(ei->i_data, 0, sizeof(ei->i_data))   // i_block[15] 全 0, 空文件无数据块
   bg_free_inodes_count-- ; s_freeinodes_counter-- ; 若目录则 bg_used_dirs_count++
   mark_inode_dirty(inode)
```

Why: 为什么普通文件和目录的选组策略不一样？——**目录是流量入口，普通文件是叶子数据**：目录若都挤在一个 BG，会让后续子文件也拥挤；所以目录用 Orlov allocator 尽量分散一级目录，普通文件用 `find_group_other` 优先靠近父目录，缩短"目录项→inode→数据块"的寻道距离。**inode 号 = group×per_group + offset + 1**，说明 inode 不是随机 ID，而是磁盘坐标编码。 [内核: 01 篇的 inode bitmap/GDT 在这里第一次被真正写脏——创建文件改的第一本账就是 inode 位图] [man 2 creat: 空文件先分配 inode, 不必立即分配数据块]

比喻锚点: `ext2_new_inode` 像发新房产证——先决定落在哪个行政区（选组），再在该区的空白证件编号簿里找第一个空号（bitmap），最后把区里"剩余证件数"减一（group desc 计数器）。 [写作时展开]

### 4. `ext2_add_link` — 把名字写进父目录

场景提示: inode 号已经有了，但 `ls` 还看不到 `a.txt`——还差哪一步？ [写作时展开]

关键设计: `ext2_add_link`（fs/ext2/dir.c）在父目录的数据块里插入一条新的 `ext2_dir_entry_2`：

```[pseudocode]
父目录 = 普通文件, 内容是 ext2_dir_entry_2 数组
ext2_add_nondir(dentry, inode)
  → ext2_add_link(dentry, inode)
      → ext2_find_entry/页扫描: 找 rec_len >= needed 的空位或可切分条目
      → 写入新目录项:
          inode = 新 inode 号
          name_len = 5
          name = "a.txt"
          file_type = fs_umode_to_ftype(inode->i_mode)
      → 若原条目太大: 切出尾部空洞给新条目
      → mark_buffer_dirty_inode(bh, dir)
  → 成功: d_instantiate_new(dentry, inode)
  → 失败: inode_dec_link_count(inode) + discard_new_inode(inode)
```

Why: 为什么 ext2 先分配 inode，再写目录项，而不是反过来？——**inode 是目标，目录项只是指针**：目录项里存的是 inode 号，没 inode 号就没法登记名字；而先拿到 inode，即使目录项写失败，也只需回滚一个未完成绑定的新 inode。**`ext2_add_nondir` 是提交点**：它先 `ext2_add_link` 写目录项，成功后 `d_instantiate_new` 把 dentry 绑定到 inode；失败则 `inode_dec_link_count + discard_new_inode` 回滚，避免留下半成品。 [内核: 两本账在这里会合——02 篇 inode 元数据 + 目录项数组绑定成"名字→inode"映射; 脏目录页后续经阶段2-06 篇页缓存/回写链路落盘]

比喻锚点: 这一步像把新房产证号码写进楼栋门牌册——证件已经印好（inode 已分配），但只有门牌册上出现"a.txt → 12345 号证"，住户才算真正对外存在。 [写作时展开]

### 5. 收束

回到 `touch a.txt` 的本质：它**不是写文件内容**，而是写三本账：
- inode bitmap：占掉一个新的 inode 号
- Block Group 计数器：空闲 inode 数减一（目录还会增 used_dirs）
- 父目录数据块：插入一条 `ext2_dir_entry_2{name → ino}`

整条创建链路：

```[pseudocode]
open(O_CREAT) → link_path_walk 确认名字不存在
  → vfs_create → ext2_create
  → ext2_new_inode: 选组 + inode 位图占位 + 初始化空 inode
  → ext2_add_link: 把名字写入父目录
  → d_instantiate: dentry ↔ inode 绑定
结果: 0 字节文件已存在, 但 i_block[15] 仍全 0 —— 数据块分配等第一次 write 再发生
```

**Aha Moment**: "`touch` 创建的不是'内容'，而是**索引关系**——先拿到一个空 inode（身份证），再把名字写进父目录（门牌册）。0 字节文件之所以已经存在，是因为'名字→inode'这层映射已经落盘；真正的数据块一块都还没分配。**文件系统先建索引，后写内容**。"
**回答读者三问**: ①`touch` 为什么改磁盘=要登记名字→inode 映射；②0 字节文件为什么也有 inode=文件先有元数据再有数据块；③创建时先改什么=先占 inode bitmap，再写父目录目录项。

---

### 核心悬念

**"`write()` 返回成功时数据常常还在 Page Cache 里——ext2 的 `write_begin → memcpy → mark_dirty` 把字节卡在了哪一层？什么时候才真的落盘？"**

→ 引出 04-write-data-flow — 写路径与回写——文件已经诞生，下一篇讲"第一个字节怎么写进去"。