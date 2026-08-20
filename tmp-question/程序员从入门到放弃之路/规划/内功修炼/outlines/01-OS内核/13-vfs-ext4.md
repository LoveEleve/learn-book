# VFS 四大对象 + inode/dentry + ext4 extent 树 + 日志 JBD2

> Cluster D: 6 KPs | 依赖: 04-页缓存 + 11-进程模型 | 读者基线: 理解页缓存和 Linux 文件 API(open/read/write/close)
> 读者处境: 已读完 04 篇（页缓存）和 11 篇（进程/文件描述符）；本篇回答"open/read/write 背后，内核怎么把几十种文件系统统一成一套 API？"
> 打开新视角: VFS 的抽象层设计、inode/dentry 的分工、ext4 extent 树为什么省元数据、JBD2 日志怎么保证崩溃一致性

---

### 概念依赖链

```
04-页缓存(文件数据缓存) + 11-进程(fd 表) → 本篇: 文件系统的统一层
  ├─ §1 VFS 四大对象(抽象层 — 依赖 04 页缓存 + 11 fd)
  │    ├─ §2 inode + dentry(元数据与名称解析 — 依赖 §1)
  │    │    └─ §3 ext4 extent 树(具体 FS 的索引 — 依赖 §2 inode)
  │    │         └─ §4 JBD2 日志(崩溃一致性 — 依赖 §3)
  │    │              └─ §5 日志替代方案(🟢: 无日志的一致性路线 — 对照 §4)
先讲: 抽象(VFS) → 元数据(inode/dentry) → 索引(extent) → 一致性(日志) → 替代(对比)
后续依赖: 14-页缓存 I/O 路径(读写流程)、15-块设备层
```

### 叙事顺序

1. 问题引入——`open("/data/a.txt")`——一个路径怎么变成文件操作？（**Aha: Linux 把几十种文件系统统一成四个对象，open/read/write 只是接口**）
   - 过渡: 四个对象各管什么？
2. VFS 四大对象——super_block/inode/dentry/file + 四级操作集
   - 过渡: 名称怎么找到数据？——inode 与 dentry 的分工
3. inode + dentry——元数据 vs 名称缓存；dcache；硬/软链接
   - 过渡: inode 指向的数据在磁盘上怎么组织？——ext4
4. ext4 extent 树——连续块索引替代间接块；大文件元数据开销对比
   - 过渡: 磁盘写入一半断电——怎么保证一致？
5. JBD2 日志——事务/提交/checkpoint；三种日志模式
   - 过渡: 日志不是唯一路线——无日志方案怎么保一致？
6. 日志替代方案（🟢）——Soft updates 写序 / LFS 追加写
   - 过渡: 完整图景已齐——回到 open 的旅程
7. 收束——VFS→inode→extent→日志 = 文件系统的四层

### 1. VFS 统一接口 — 四种文件系统的最大公约数

场景提示: `open/read/write` 对 ext4、NFS、tmpfs 都一样——内核怎么做到的？ [写作时展开]

关键设计: VFS（Virtual File System）用**四大对象**统一所有文件系统 (include/linux/fs.h)：

| 对象 | 含义 |
|------|------|
| super_block | 文件系统超级块（整体信息） |
| inode | 文件元数据（inode 号/权限/大小/数据位置） |
| dentry | 目录项（名称 → inode 的缓存） |
| file | 打开文件（当前偏移/打开标志） |

四级操作集（每个文件系统实现自己的）:
- super_operations: alloc_inode/destroy_inode/write_super
- inode_operations: create/link/mkdir/rename
- dentry_operations: d_compare/d_hash/d_delete
- file_operations: read/write/mmap/open/ioctl

路径解析: `namei → path_lookup → link_path_walk` → 逐分量查找 dentry → `do_last` 处理最后分量 → open 或 create——一次路径解析 = 多次 dentry 查找 + inode 读。

Why: 为什么需要 VFS 这层抽象？——应用只面对"文件"概念（open/read/write）；内核把"文件"实例化为四大对象 + 操作集指针。新文件系统只需实现操作集（接入 VFS），应用零改动。**统一接口 + 多态实现**——与 19 篇的内核设计模式（接口+回调）同构。

比喻锚点: VFS=通用电源插座——不管发电站（文件系统）是水电/火电/核电（ext4/NFS/tmpfs），电器（应用）只认"两脚插头"（open/read/write），每家电站配一个转接头（操作集实现）。 [写作时展开]

### 2. inode + dentry — 文件系统的元数据中枢

场景提示: 文件名和文件内容——它们是一个东西吗？硬链接怎么让两个名字指同一文件？ [写作时展开]

关键设计:

**inode（文件的本体）**: `i_ino`（inode 号）/`i_mode`（类型+权限）/`i_size`/`i_blocks`/`i_atime~i_ctime` → `i_mapping`（address_space——文件数据的页缓存入口，04 篇）/`i_op`/`i_fop`。

**dentry（名称的缓存）**: `d_name`（文件名）/`d_inode`（指向 inode）/`d_parent` → `d_hash` 哈希 → `d_lookup` 命中直接返回，未命中 `d_alloc` + 读磁盘。

**dcache**: dentry cache 缓存"路径分量 → inode"——避免每次访问都查磁盘目录。

**链接的本质**:
- 硬链接 = **两个 dentry 指向同一 inode**（`ls -i` 同号，计数 +1）
- 软链接 = **一个特殊 inode 内容是目标路径**（不同 inode）

Why: 为什么 inode 与 dentry 分离？——**职责不同**：inode 是"文件本身"（数据/权限/大小，一次读入常驻内存）；dentry 是"名称解析"（路径分量→inode 的映射，可随时重建）。分离让"文件数据"与"名字"独立变化——硬链接改的是名字映射，mv 只动 dentry，都不碰 inode 数据。 [内核: dcache 是内存大户——大量小文件场景 dentry 缓存占用可观, slabtop 常见 top]

比喻锚点: inode=户口本（人的本体：出生年月/身份证号，跟名字无关）；dentry=门牌号（姓名→户口本的映射）——一个人可以有两块门牌（硬链接），改门牌不动户口本（mv）。 [写作时展开]

### 3. ext4 extent 树 — 大文件的低开销索引

场景提示: 100MB 文件在磁盘上怎么记录"哪些块属于它"？老办法要几千个指针。 [写作时展开]

关键设计: ext4 用 **extent 树**（连续块的 B 树）替代传统间接块 (fs/ext4/ext4_extents.h)：

```[pseudocode]
ext4_extent_header → ext4_extent_idx(索引节点, 12 字节/个)
→ ext4_extent(叶子: ee_block 起始逻辑块 + ee_len 连续块数 + ee_start 物理块)
```

- **连续分配**: 一个 extent 记录"从逻辑块 X 开始的 N 个连续物理块"——100MB 连续文件可能只需 **1 个 extent**（对比间接块要 25600 个指针）
- **元数据开销**: 连续分配 >> 间接块——索引更小、读更快；extent 最大化（allocate_blocks 尝试连续）
- **日志模式** (fs/ext4/super.c):

| 模式 | 语义 | 权衡 |
|------|------|------|
| ordered（默认） | 元数据日志 + 数据先写 | 一致性 + 性能折中 |
| writeback | 仅元数据日志 | 最快，崩溃后可能旧数据 |
| data（journal） | 元数据+数据都进日志（数据 double write） | 最安全但最慢（数据写两次） |

Why: 为什么 extent 比间接块省？——间接块用"指针数组"记录每个块（每 4KB 块 = 1 指针，100MB = 25600 指针）；extent 用"区间"记录（连续 N 块 = 1 条记录）。**文件越连续，extent 越省**——所以 ext4 分配器刻意尝试连续分配（延迟分配+多块预留），让 extent 最大化。

比喻锚点: extent=集装箱船仓位表——连续 100 个集装箱只需记"从 3 号位开始连放 100 个"（区间）；老办法（间接块）是每个箱子记一个位置（指针数组），箱子越多记录越臃肿。 [写作时展开]

### 4. JBD2 日志 — 崩溃恢复的保证

场景提示: 写文件到一半断电——磁盘上元数据（大小）和数据不一致怎么办？ [写作时展开]

关键设计: JBD2（Journaling Block Device 2）——**写前日志** (fs/jbd2/transaction.c)：

```[pseudocode]
jbd2_journal_start/stop → 修改包装成事务
事务提交(写入日志区) → 事务完成(写入 checkpoint) → 崩溃恢复(重放已提交事务)
```

日志结构: journal superblock + descriptor block（描述修改哪些块）+ data blocks（修改副本）+ commit block（事务结束标记）——块设备上连续区域。

ordered 模式流程: 先写数据到磁盘 → 元数据修改写入日志 → 日志提交 → 元数据写入对应位置（checkpoint）→ 日志区释放。

Why: 为什么"先写日志再改元数据"能防崩溃？——崩溃只可能发生三种位置：日志未写完（事务未提交，丢弃即可）/日志已提交但元数据未写（重放日志恢复）/都完成（正常）。**日志让"半完成状态"可判定**——要么重放（提交了）要么丢弃（没提交），不会出现"改了一半"的中间态。代价是两次写（日志+实际位置）。

比喻锚点: JBD2=手术前签字确认——先把手术方案（日志）写好存档，再动刀（改元数据）；半路断电（崩溃）看存档：方案签了（已提交）就按方案补完（重放），没签就推倒重来（丢弃）。 [写作时展开]

### 5. 日志替代方案 (🟢)

场景提示: 日志是"先记账再干活"——有没有"不用记账"的一致性方案？ [写作时展开]

关键设计: 两条无日志路线 (B1 Ch12§5-6)：

- **Soft updates**: 依赖追踪保证磁盘块**写入顺序** → 免日志也能一致 → FreeBSD UFS 采用，Linux 未用 → **复杂度转移而非消除**（从"写两次"变成"严格排序"）
- **LFS（日志结构 FS）**: 所有写**追加到日志尾部** → 顺序写 + segment cleaning（回收碎片）→ SSD 友好 → F2FS 继承该思路

Why: 为什么 Linux 选 JBD2 而非 Soft updates？——Soft updates 的写入顺序约束极其复杂（依赖追踪易错）；JBD2 用"多写一次"换简单正确。**三种路线对比**：写前日志（JBD2，记账）/写序约束（Soft updates，排序）/追加写（LFS，只增不改）——都是"崩溃后状态可判定"的不同实现。

比喻锚点: 三种一致性路线=三种记账法——JBD2 是"先记流水账再干活"（写前日志）、Soft updates 是"按顺序干活不许插队"（写序约束）、LFS 是"永远只在账本末尾加新页"（追加写）。 [写作时展开]

### 6. 收束

回到 `open("/data/a.txt")` 的旅程：
- VFS = 统一接口（四大对象 + 操作集）
- dentry = 名称解析（路径 → inode）
- inode = 文件本体（元数据 + 数据索引入口）
- extent = 数据索引（连续块区间）
- JBD2 = 崩溃一致性（写前日志）

**Aha Moment**: "文件名不是文件——dentry 只是指向 inode 的路牌（所以硬链接能两个名字一个文件）；文件数据在磁盘上的位置用 extent 区间记录（连续分配省元数据）；而崩溃安全靠'先记账（日志）再干活'——三种一致性路线（记账/排序/追加）本质都是'让半完成状态可判定'。"
**回答读者三问**: ①硬链接怎么实现=两 dentry 一 inode；②ext4 为什么快=extent 连续区间省元数据；③断电不坏=JBD2 写前日志重放。

---

### 核心悬念

**"ext4 用 extent 树存数据 — 但读写文件时到底经过多少个缓冲区？为什么 mmap 比 read 少一次拷贝？零拷贝 sendfile 怎么跳过 CPU？"**

→ 引出 14-页缓存读写流程 + Direct I/O + mmap I/O + 零拷贝(sendfile/splice)——文件怎么存的讲完了，怎么读写的讲下一篇。
