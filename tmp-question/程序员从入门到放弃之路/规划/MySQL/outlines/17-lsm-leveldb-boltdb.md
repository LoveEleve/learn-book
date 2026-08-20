# LSM、LevelDB 与 BoltDB — 为什么行存 B+Tree 之外还会有另一套存储世界

> Cluster A | 覆盖知识元: 10.1 LSM Tree + 10.2 LevelDB + 10.3 BoltDB | 依赖: 04 B+Tree/页/行格式、03 Redo/Undo/WAL | 读者基线: B+Tree、WAL、页/表空间、事务恢复
> 文章定位: MySQL 扩展专题第一篇；把 InnoDB 之外的两条典型存储路线放在一起：LSM 系列（LevelDB）与 B+Tree+COW 系列（BoltDB）
> 打开新视角: 存储引擎设计不是只有“InnoDB 的页 + B+Tree”这一条路，**写优化的 LSM、读写平衡的 B+Tree、COW 的 MVCC** 都是对“写放大、读放大、空间放大、恢复复杂度”不同权衡的结果

---

### 概念依赖链

```
04 B+Tree/页/行格式 + 03 WAL/恢复 → 本篇: LSM / LevelDB / BoltDB
  ├─ §1 LSM Tree(写入路径/Compaction/Bloom/写放大)
  ├─ §2 LevelDB(MemTable/WAL/SST/VersionSet)
  ├─ §3 BoltDB(page/bucket/tx/COW)
  └─ §4 对比(InnoDB B+Tree vs LSM vs COW)
先讲: LSM 原理 → 一个典型 LSM 实现 → 一个典型 B+Tree+COW 实现 → 三者权衡
后续依赖: 18-bigdata-cdc-htap(大数据 SQL、CDC 与 HTAP)
```

### 叙事顺序

1. 问题引入——如果写入吞吐远比点查更重要，或者 SSD 写放大成为瓶颈，还一定要用 InnoDB 那种 B+Tree 吗？
2. LSM Tree——顺序写、Compaction 和读放大
3. LevelDB——把 LSM 拆成 WAL/MemTable/SSTable/VersionSet
4. BoltDB——B+Tree + COW 的另一条单机路线
5. 三者对比——写放大、读放大、空间放大和恢复语义
6. 收束——从 MySQL 引擎走向更广义的存储系统

### 1. LSM Tree — 把随机写改造成顺序写，再用 Compaction 还债

场景提示: 如果工作负载主要是持续写入和追加日志，为什么随机更新 B+Tree 叶子页会变得昂贵？ [写作时展开]

关键设计: LSM 的核心不是“没有索引”，而是把写入先落到内存有序结构和顺序日志，再分层刷成不可变文件：

```[pseudocode]
write(k, v)
  → append WAL
  → insert/update MemTable
  → MemTable 满
      → freeze immutable memtable
      → flush 成新的 SSTable

read(k)
  → 先查 MemTable/immutable
  → 再查多层 SSTable
  → 借助 filter/index 减少无效读

后台:
  compaction 合并/重写 SSTable
```

Why: 为什么 LSM 写快却常伴随读放大和 Compaction 成本？——**写入被推迟成顺序追加，随机覆盖变少，但读取时同一个 key 可能分散在 MemTable 和多层 SST 文件中**；Compaction 负责把旧版本归并、删除 tombstone、维持层级结构，同时带来额外 I/O 和写放大。Bloom Filter 只能减少“不存在 key”的无效读取，不会消除范围查询和 Compaction 代价。 [存储引擎: 写放大/读放大/空间放大三者需要一起讨论，不能只用一个 QPS 指标判断 LSM 更优] [阶段5性能: Compaction 的后台 I/O 会进入设备队列，需用吞吐/延迟/写放大观测验证]

比喻锚点: LSM 像把所有新订单先记在前台流水账和临时白板上，夜间再整理进多层档案柜；白天写得快，晚上要付整理账本的代价。 [写作时展开]

### 2. LevelDB — 一个典型 LSM 实现如何组织 WAL、MemTable 和 SST

场景提示: LSM 是概念，LevelDB 这种具体引擎是怎样把它落成可恢复、可查询的单机系统？ [写作时展开]

关键设计: LevelDB 把 LSM 的几个关键部件拆成清晰对象：

```[pseudocode]
写入:
  WAL append
  → MemTable(skiplist) 写入
  → 满后切换 immutable memtable
  → background minor compaction → SSTable(L0)

SSTable:
  data blocks
  index block
  filter block(Bloom)
  footer/meta

元数据:
  VersionEdit 记录文件增量变化
  Version/VersionSet 维护当前“哪些 SST 文件组成这棵 LSM”
```

Why: 为什么 LevelDB 不直接改写原 SST 文件，而要新增 VersionEdit/VersionSet？——**SST 是不可变文件，元数据也需要一种可恢复、可切换的版本管理方式**；VersionEdit 记录“新增/删除哪些 SST”，VersionSet 维护当前可见集合，这让恢复和快照比直接原地修改元数据更简单。SkipList、Bloom、compaction 策略则共同决定延迟和放大。 [KV 引擎: LevelDB 是单机嵌入式 LSM 代表，RocksDB 等会在 Compaction、缓存、压缩上进一步扩展]

比喻锚点: LevelDB 像图书馆每次只增量登记“新增了哪些书架、废弃了哪些书架”，而不是每次整理后重写整本馆藏目录。 [写作时展开]

### 3. BoltDB — 不用 LSM，也能用 COW 保持单机一致性

场景提示: 如果你想要单文件、事务简单、读性能稳定，为什么有人会选 BoltDB 这种 B+Tree + COW 路线，而不是 LSM？ [写作时展开]

关键设计: BoltDB 用 mmap + page + B+Tree + copy-on-write 组织数据；写事务复制脏页并在提交时切换元数据：

```[pseudocode]
页面类型:
  meta / freelist / branch / leaf

Bucket:
  一棵逻辑 B+Tree
  → Cursor 遍历/定位键值

写事务:
  读取当前页
  → 在内存中修改 node 表示
  → COW 写新页
  → 更新 freelist/meta
  → 提交时切换到新 meta
```

Why: 为什么 BoltDB 的事务模型和 LSM 完全不同？——**它不靠后台 Compaction 维持有序层，而是靠页级 COW 和 meta 切换提供一致性与快照视图**；优点是读路径简单、单文件结构直观，缺点是写入并发、空间回收、长写事务和 mmap 行为需要谨慎对待。COW 也不自动等于“性能更好”，它只是把写放大和恢复路径改成另一种形态。 [系统编程: BoltDB 把 mmap、页、B+Tree 和 COW 组合在一起，和 MySQL/InnoDB 的页/日志体系形成对照]

比喻锚点: BoltDB 像每次改文件都先复印要修改的页，最后换一本新的目录页；旧页保留下来，直到确定不再需要才回收。 [写作时展开]

### 4. InnoDB、LSM 与 COW — 三种路线的根本权衡

场景提示: 行存数据库、嵌入式 KV、单文件事务库，为什么会走向三种不同结构？ [写作时展开]

关键设计: 三条路线分别优化不同目标：

```[pseudocode]
InnoDB/B+Tree:
  优势: 范围查询、二级索引、事务/锁/MVCC 完整
  成本: 随机写与页维护、回表、复杂恢复

LSM/LevelDB:
  优势: 写入顺序化、吞吐好、适合写密集
  成本: Compaction、读放大、空间放大、范围/点查权衡

BoltDB/COW B+Tree:
  优势: 读路径直观、单文件、事务/快照语义清晰
  成本: 写时复制、空间管理、并发写限制和 mmap 风险
```

Why: 为什么不能简单说“LSM 一定适合写、B+Tree 一定适合读”？——**真实系统还要考虑事务模型、二级索引、压缩、SSD/HDD、缓存、range scan、compaction 抖动和运维复杂度**；路线选择是工作负载与工程约束的综合问题，不是结构名的输赢。 [存储系统: 写放大、读放大、空间放大与恢复语义要一起评价]

### 5. 收束

存储引擎三路线：

```[pseudocode]
InnoDB:
  B+Tree + 页 + Redo/Undo + Buffer Pool

LevelDB:
  WAL + MemTable + SSTable + Compaction + VersionSet

BoltDB:
  mmap + page + B+Tree + freelist + COW meta switch
```

**Aha Moment**: "存储引擎设计的核心不是‘用什么数据结构’，而是**你愿意把成本放在哪个阶段支付：写入时、读取时、后台整理时，还是恢复时**。InnoDB、LSM、BoltDB 只是三种不同的付费时机。"
**回答读者三问**: ①LSM 为什么写快=先顺序 WAL/MemTable，后台再整理；②LevelDB 为什么需要 VersionSet=不可变 SST 需要版本元数据切换；③BoltDB 和 InnoDB 都是 B+Tree，区别在哪=BoltDB 用 mmap+COW 组织单文件事务，InnoDB 有 Buffer Pool/Redo/Undo 与更复杂并发控制。

---

### 核心悬念

**"单机存储结构已经比较清楚；当数据要进入大数据 SQL、CDC 链路和 HTAP 体系时，为什么又会出现 Shuffle、数据倾斜和异构同步这些全新问题？"**

→ 引出 18-bigdata-cdc-htap — 大数据 SQL、CDC/OGG 与 HTAP。