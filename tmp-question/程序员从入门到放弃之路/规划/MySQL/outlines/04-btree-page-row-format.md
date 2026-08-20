# InnoDB B+Tree 与数据页 — 一次索引查找如何落到行记录

> Cluster A | 覆盖知识元: 3.1 B+Tree + 3.2 数据页 + 3.3 行格式 | 依赖: 01 架构、阶段2 Redo/Undo | 读者基线: Buffer Pool、磁盘页、事务基础
> 文章定位: MySQL 阶段 3 索引的第一篇；建立“索引树 → 16KB 页 → 行格式/溢出页”的物理心智模型
> 打开新视角: SQL 条件不是直接命中一行，而是**先在 B+Tree 导航到页，再在页目录定位记录，必要时沿二级索引主键回表**

---

### 概念依赖链

```
01 InnoDB架构 + 02 Redo/Undo → 本篇: B+Tree/页/行格式
  ├─ §1 B+Tree(聚簇/二级索引与回表)
  ├─ §2 InnoDB Page(页内目录与记录)
  ├─ §3 Row Format(COMPACT/DYNAMIC/溢出)
  └─ §4 表空间(Extent/Segment/ibd)
先讲: 索引树 → 页结构 → 行格式 → 空间组织
后续依赖: 05-index-maintenance(分裂/合并/检索代价)
```

### 叙事顺序

1. 问题引入——`SELECT * FROM t WHERE id=...` 为什么不是“查一次磁盘”，而是树、页、记录多层定位？
2. B+Tree——聚簇索引、二级索引、回表
3. 16KB 数据页——页头、记录、目录与校验
4. 行格式——变长、NULL、溢出页
5. 表空间——页如何聚成 Extent/Segment 并落到 ibd
6. 收束——从 key 到行记录的完整路径

### 1. B+Tree — 聚簇索引与二级索引如何导航记录

场景提示: 主键查找和非主键查找为什么可能产生不同的 I/O 路径？ [写作时展开]

关键设计: InnoDB 的聚簇索引叶子保存整行记录，二级索引叶子通常保存索引键与主键值：

```[pseudocode]
聚簇索引:
  root/non-leaf pages
  → key comparison
  → leaf page
  → clustered record(整行)

二级索引:
  root/non-leaf pages
  → secondary key
  → leaf entry(secondary key + primary key)
  → 用 primary key 再查聚簇索引
  → 回表取得整行

覆盖索引:
  SELECT 列都在二级索引叶子
  → 可以不回表
```

Why: 为什么 InnoDB 二级索引叶子放主键而不是物理地址？——**页可能移动、分裂、重组，物理地址不稳定；主键作为逻辑定位再次导航更稳健**。代价是非覆盖查询可能多一次树访问；主键宽度也会放大所有二级索引叶子。B+Tree 的范围扫描还依赖叶子节点的有序组织与链式遍历。 [MySQL: 聚簇/二级索引具体存储细节以目标版本和行格式为准]

比喻锚点: 聚簇索引像按学号排列的完整档案柜，二级索引像按姓名排列的索引卡，卡片上还写着学号，查到卡片后要再去学号柜取完整档案。 [写作时展开]

### 2. InnoDB 数据页 — 16KB 不是一条记录，而是一座页内小城市

场景提示: B+Tree 找到一个叶子页后，页内部怎样从几十/几百条记录中快速找到目标？ [写作时展开]

关键设计: InnoDB 默认页大小常为 16KB，但页结构由版本、页类型和配置共同决定；典型索引页包含头部、系统记录、用户记录、空闲区、Page Directory 和 trailer：

```[pseudocode]
Index/data page(典型概念布局)
  File Header
  Page Header
  Infimum / Supremum 系统记录
  User Records
  Free Space
  Page Directory(Slots)
  File Trailer/checksum 等

查询:
  页内 Page Directory 提供稀疏槽位
  → 先定位大致 record range
  → 再沿记录链比较 key
```

Why: 为什么页目录不为每条记录建立完整数组索引？——**稀疏目录在空间开销与查找速度间折中**：槽位减少目录空间，页内记录链保留插入/删除灵活性。页大小固定并不意味着每页记录数固定，行格式、NULL、变长字段和溢出都会改变容量。 [MySQL/InnoDB: 页大小、Page Directory 和 record header 随页类型/版本实现核对]

比喻锚点: 数据页像一座城市：Page Directory 是街区索引，记录是房屋；先到街区，再沿门牌顺序找具体房子。 [写作时展开]

### 3. 行格式与溢出 — 一行如何放进页，放不下怎么办

场景提示: 一行包含很多变长字段或 BLOB，16KB 页装不下整行时，InnoDB 如何保持索引记录可定位？ [写作时展开]

关键设计: 行格式记录变长长度、NULL 信息、记录头和真实字段；大字段可能使用外部页/溢出存储：

```[pseudocode]
COMPACT 等格式:
  变长字段长度列表
  → NULL bitmap
  → record header
  → fixed/variable data

DYNAMIC/COMPRESSED 等格式:
  行内保留必要前缀/指针
  → 大字段内容放外部页

查询大字段:
  先定位记录
  → 根据外部存储指针读取溢出页
```

Why: 为什么不能把所有列都固定塞进行内？——**页内空间、行大小、缓存命中和大字段访问频率需要平衡**：把大字段全放行内会减少单页记录数和缓存效率，全部外置又增加额外 I/O。具体行格式、溢出阈值和压缩行为依赖 MySQL/InnoDB 版本、页大小和列类型，不能把“20 字节指针”等实现细节泛化为所有版本。 [MySQL: `ROW_FORMAT`、页大小和 BLOB/TEXT 外部存储规则按版本文档核对]

比喻锚点: 行格式像档案柜中的文件夹：常用摘要放首页，超厚附件放旁边的附件柜，首页保留索引号。 [写作时展开]

### 4. 表空间、Extent 与 Segment — 页如何组织成可分配空间

场景提示: 页是查询单位，但磁盘空间不可能每次只随机申请一个页；InnoDB 如何组织更大的分配单位？ [写作时展开]

关键设计: 表空间、Segment 和 Extent 构成从文件到页的空间组织层：

```[pseudocode]
Tablespace:
  系统表空间 / 独立表空间(.ibd) / Undo 等

Segment:
  为索引/用途管理一组页的逻辑空间
  叶子与非叶子页可能有不同分配需求

Extent:
  一组连续页的分配单位
  典型 16KB 页配置下常见为 64 页量级

查询路径:
  B+Tree page → page_no
  → tablespace/file mapping
  → buffer pool 或磁盘 I/O
```

Why: 为什么需要 Extent/Segment，而不是所有页都从一个全局空闲链表取？——**更大的连续分配单位有助于管理局部性、减少元数据和支持索引不同部分的空间策略**；代价是内部碎片、预留空间和复杂回收。系统表空间、独立表空间和压缩/加密/文件格式选项会改变具体布局。 [MySQL/InnoDB: Extent 大小与页大小/表空间配置相关，不能脱离配置固定理解]

比喻锚点: 页是单本书，Extent 是一排书架，Segment 是某类书的专区，Tablespace 是整座图书馆；查询拿书，分配管理却按更大的区域组织。 [写作时展开]

### 5. 收束

从 SQL key 到物理记录：

```[pseudocode]
WHERE key
  → B+Tree root/non-leaf
  → leaf page
  → Page Directory/record chain
  → row format
  → inline data 或 overflow page
  → Buffer Pool/tablespace

二级索引非覆盖查询:
  secondary leaf → primary key → clustered leaf → row
```

**Aha Moment**: "索引不是一张扁平目录，而是**B+Tree 导航、页内目录、行格式和表空间分配**四层结构；查询慢可能发生在树高、回表、页缓存、溢出页或 I/O 任一层。"
**回答读者三问**: ①主键和二级索引差在哪=聚簇叶子有整行，二级叶子通常有主键；②16KB 页里有什么=页头/记录/目录/校验等结构；③大字段放不下怎么办=行格式用外部页/溢出存储策略。

---

### 核心悬念

**"索引查找路径已经清楚；当插入把页写满，InnoDB 如何分裂/合并页，为什么随机主键会制造更多写放大和碎片？"**

→ 引出 05-index-maintenance — 页分裂、页合并、回表与索引维护。