# MySQL 数据库 — 知识规划

> 2026-08-08 | 14 本书 | TOC-only, AI 生成全部内容
> N=14 — 跨书共识信号在全部提取完成后计算

---
## [01 提取] 逐书 Knowledge Point 提取

### [01 #1/14 done] Book 1: MySQL内核设计与实现 (赵景波, 9章) — 81 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch1 MySQL内核简介** | | |
| 1.5 开始编译MySQL(下载源码包/编译/IDE调试/调试技巧) | MySQL源码编译与调试环境搭建 | High (TOC explicitly names build+debug workflow) |
| **Ch2 MySQL内核整体架构** | | |
| 2.1 Server层-连接层 | Server层连接管理与线程模型 | Medium (parenthetical list item, generic heading) |
| 2.1 Server层-查询优化 | 查询优化器架构 | Medium (parenthetical, no subsection detail) |
| 2.1 Server层-参数状态performance_schema | Performance Schema监控框架 | High (explicit MySQL feature name) |
| 2.1 Server层-缓存 | Server层缓存机制(查询缓存/表缓存) | Medium (generic "缓存" term, ambiguous scope) |
| 2.1 Server层-日志 | Server层日志系统(错误日志/通用日志/慢查询日志) | Medium (generic "日志" term, ambiguous scope) |
| 2.1 Server层-锁 | Server层锁管理框架 | Medium (generic "锁" term, no lock type specified) |
| 2.1 Server层-存储过程/UDF | 存储过程与用户自定义函数 | High (explicit MySQL features) |
| 2.1 Server层-复制层 | 复制协议层架构 | Medium (parenthetical, no subsection detail) |
| 2.1 Server层-API层 | 存储引擎API接口层 | Low (very vague "API层", no specificity) |
| 2.2 存储引擎层-缓冲池 | 存储引擎缓冲池架构 | Medium (parenthetical, generic heading) |
| 2.2 存储引擎层-重做日志缓冲区 | 存储引擎重做日志缓冲区 | Medium (parenthetical, generic) |
| 2.2 存储引擎层-双写机制 | 双写缓冲区机制 | Medium (parenthetical) |
| 2.2 存储引擎层-后台线程 | 存储引擎后台线程模型 | Medium (parenthetical) |
| 2.3 文件层 | MySQL文件层抽象 | Medium (generic "文件层", no file types specified) |
| 2.4 MySQL启动流程(三个阶段) | MySQL启动流程三阶段 | High (explicit section with sub-description) |
| **Ch3 客户端和服务端交互协议** | | |
| 3.1 MySQL的连接方式(TCP/IP/UNIX域套接字/命名管道共享内存) | MySQL连接方式与传输层 | High (TOC explicitly lists all connection methods) |
| 3.2 交互过程-通信协议 | Client-Server通信协议(packet格式/命令码) | High (explicit section on protocol) |
| 3.2 交互过程-连接阶段 | 连接阶段握手与认证流程 | High (explicit sub-topic) |
| 3.2 交互过程-命令执行阶段 | 命令执行阶段处理流程 | High (explicit sub-topic) |
| 3.3 处理连接与创建线程(监听请求/创建连接线程/THD类) | 连接线程管理与THD类结构 | High (explicit section, THD explicitly named) |
| **Ch4 数据字典** | | |
| 4.1 数据字典简介(文件层/InnoDB层/Server层) | 数据字典三层架构 | High (explicit section with layer names) |
| 4.2 .frm文件 | .frm表定义文件格式 | High (explicit section) |
| 4.3 数据字典的使用(创建表/查询表/rowid) | 数据字典运行时使用(rowid分配/表查询) | High (explicit section with usage scenarios) |
| 4.4 MySQL 8.0数据字典-文件存储层 | MySQL 8.0事务型数据字典存储架构 | High (explicit section, version-specific) |
| 4.4 MySQL 8.0数据字典-缓存 | 数据字典客户端缓存机制 | High (explicit sub-topic) |
| 4.4 MySQL 8.0数据字典-使用 | MySQL 8.0事务型DD运行时使用方式 | High (explicit sub-topic) |
| 4.4 MySQL 8.0数据字典-SDI | 序列化字典信息(SDI) | High (explicit named feature) |
| 4.4 MySQL 8.0数据字典-原子DDL | 原子DDL实现机制 | High (explicit named feature) |
| **Ch5 InnoDB存储引擎** | | |
| 5.1 整体架构 | InnoDB存储引擎整体架构 | High (explicit section) |
| 5.2 缓冲池-架构与初始化 | InnoDB缓冲池架构(实例/页/LRU链表/Free链表/Flush链表) | High (explicit sub-topic) |
| 5.2 缓冲池-缓存淘汰策略 | InnoDB缓冲池LRU改进淘汰策略(young/old区)与相关参数 | High (explicit sub-topic, LRU is independent mechanism) |
| 5.3 插入缓冲区(流程/相关参数) | Change Buffer(插入缓冲)机制 | High (explicit section) |
| 5.4 自适应哈希-查询加速 | AHI自适应哈希索引的查询加速机制 | High (explicit sub-topic) |
| 5.4 自适应哈希-索引维护 | AHI在DML操作中的索引维护与更新策略 | High (explicit sub-topic, index maintenance is separate concern) |
| 5.5 重做日志缓冲区-架构 | Log Buffer整体架构与在InnoDB中的位置 | High (explicit sub-topic, architecture is independent) |
| 5.5 重做日志缓冲区-管理结构 | Log Buffer内部管理数据结构(log_t/log_block) | High (explicit sub-topic, data structures distinct from architecture) |
| 5.5 重做日志缓冲区-更新流程与刷盘 | Log Buffer写入流程与刷盘策略(innodb_flush_log_at_trx_commit) | High (explicit sub-topic, update+flush are operational aspects) |
| 5.6 双写机制(缓冲区管理/可靠性保证) | Doublewrite Buffer与部分写保护 | High (explicit section) |
| 5.7 后台线程(master线程/I/O线程/刷脏线程/清理线程) | InnoDB后台线程模型(Master/IO/Page Cleaner/Purge) | High (explicit section listing all thread types) |
| **Ch6 InnoDB文件组织** | | |
| 6.1 数据文件-逻辑组织 | 表空间逻辑组织结构(表空间/段/区/页层次) | High (explicit sub-topic, logical and physical are separate layers) |
| 6.1 数据文件-物理组织与更新 | 表空间物理文件布局与数据更新时的存储操作 | High (explicit sub-topic, physical layout + update mechanism) |
| 6.2 重做日志文件(总体架构/更新操作日志) | Redo Log文件架构与写入 | High (explicit section) |
| 6.3 回滚日志文件-架构 | Undo Log总体架构与回滚段(rollback segment)设计 | High (explicit sub-topic, architecture is foundational) |
| 6.3 回滚日志文件-管理与更新 | Undo Log管理机制与更新操作日志 | High (explicit sub-topic, management+operation separate from architecture) |
| **Ch7 InnoDB索引的实现** | | |
| 7.1 索引简介-B树/B+树 | B+Tree索引结构原理 | High (explicit section, B+Tree named) |
| 7.1 索引简介-全文索引 | 全文索引(FULLTEXT)实现 | High (explicit section) |
| 7.2 索引的结构-聚簇索引 | 聚簇索引结构(Clustered Index) | High (explicit section) |
| 7.2 索引的结构-二级索引 | 二级索引结构(Secondary Index)与回表 | High (explicit section) |
| 7.2 索引的结构-复合索引 | 复合索引结构(Composite Index)与最左前缀 | High (explicit section) |
| 7.3 索引的管理(内存管理/加载/创建) | 索引内存管理与创建流程 | High (explicit section) |
| 7.4 数据检索-聚簇检索 | 聚簇索引检索流程 | High (explicit sub-topic) |
| 7.4 数据检索-二级检索 | 二级索引检索与回表操作 | High (explicit sub-topic) |
| 7.4 数据检索-插入/删除/更新 | 索引维护操作(INSERT/DELETE/UPDATE) | High (explicit sub-topic) |
| 7.5 索引分裂和合并-页分裂 | 页分裂(Page Split)触发条件与流程 | High (explicit section with sub-mechanism) |
| 7.5 索引分裂和合并-页合并 | 页合并(Page Merge)触发条件与流程 | High (explicit sub-topic) |
| 7.5 索引分裂和合并-页重组 | 页重组(Page Reorganize)机制 | High (explicit sub-topic) |
| **Ch8 MySQL并发控制** | | |
| 8.1 MySQL事务的实现-事务管理 | 事务生命周期管理(BEGIN/COMMIT/ROLLBACK/SAVEPOINT) | High (explicit section) |
| 8.1 MySQL事务的实现-执行流程 | 事务内部执行流程(redo undo binlog 日志协调) | High (explicit sub-topic, clarified boundary vs lifecycle) |
| 8.1 MySQL事务的实现-ACID实现 | ACID四大特性实现机制 | High (explicit sub-topic, ACID named) |
| 8.1 MySQL事务的实现-MVCC | MVCC多版本并发控制实现 | High (explicit sub-topic, MVCC named) |
| 8.1 MySQL事务的实现-崩溃恢复 | 崩溃恢复(Crash Recovery)机制 | High (explicit sub-topic) |
| 8.1 MySQL事务的实现-组提交 | Binlog组提交(Group Commit) | High (explicit sub-topic) |
| 8.1 MySQL事务的实现-分布式事务 | XA分布式事务实现 | High (explicit sub-topic) |
| 8.2 MySQL锁实现-元数据锁 | 元数据锁(MDL)机制 | High (explicit sub-topic) |
| 8.2 MySQL锁实现-表锁 | 表级锁(READ/WRITE锁)与锁定语义 | High (explicit sub-topic) |
| 8.2 MySQL锁实现-InnoDB行锁 | InnoDB行锁(Record/Gap/Next-Key Lock) | High (explicit sub-topic) |
| 8.2 MySQL锁实现-InnoDB表锁 | InnoDB意向锁(Intention Lock) | High (explicit sub-topic) |
| 8.2 MySQL锁实现-互斥锁 | InnoDB内部互斥锁(Mutex)机制 | High (explicit sub-topic, mutex is distinct lock primitive) |
| 8.2 MySQL锁实现-读写锁 | InnoDB内部读写锁(RW-Lock)机制 | High (explicit sub-topic, rw-lock is distinct from mutex) |
| 8.2 MySQL锁实现-锁升级降级 | 锁升级与锁降级策略 | High (explicit sub-topic) |
| 8.2 MySQL锁实现-死锁 | 死锁检测与处理机制 | High (explicit sub-topic) |
| **Ch9 MySQL高可用实现** | | |
| 9.1 MySQL主从复制-数据同步流程 | 主从复制数据同步流程 | High (explicit section) |
| 9.1 MySQL主从复制-binlog详解 | Binary Log格式与事件类型详解 | High (explicit sub-topic) |
| 9.1 MySQL主从复制-半同步复制 | 半同步复制(Semi-Sync Replication) | High (explicit sub-topic, named feature) |
| 9.1 MySQL主从复制-并行复制 | 并行复制(Slave Parallel Workers) | High (explicit sub-topic) |
| 9.2 组复制-总体架构 | MySQL Group Replication(MGR)架构 | High (explicit section) |
| 9.2 组复制-数据流 | MGR消息数据流与Pipeline | High (explicit sub-topic) |
| 9.2 组复制-MGR Paxos协议优化 | MGR Paxos协议优化(XCom层) | High (explicit sub-topic) |
| 9.2 组复制-MGR冲突检测 | MGR冲突检测与认证机制 | High (explicit sub-topic) |
| 9.2 组复制-MGR流控 | MGR流控(Flow Control)机制 | High (explicit sub-topic) |

> **说明**: 
> - Ch1 1.1-1.4(历史/衍生/版本/社区) 和所有 总结 不提取——纯历史/非知识点(per methodology/01 §2)。
> - Ch2 2.1 括注项全分解(9/9)——各项是独立Server层模块，但因无独立§子节降Medium置信度。
> - Ch5-Ch6 括注项为同机制的子面向(如缓冲池的架构vs淘汰策略)——每项拆为独立KP。
> - 深审R1: 8项发现，7项修复(遗漏1+粒度过粗6)，74→81 KPs。
> - 深审R2: 8项发现，2项修复(事务命名边界+表锁命名精度)。


### [01 #2/14 done] Book 2: MySQL是怎样运行的 (小孩子4919, 23章) — 127 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch1 初识MySQL** | | |
| 1.1 MySQL的客户端/服务器架构 | MySQL C/S架构模型 | High (explicit section) |
| 1.5 客户端与服务器连接的过程(TCP/IP/命名管道/共享内存/UNIX域套接字) | MySQL客户端连接方式与传输层 | High (explicit section with sub-mechanisms) |
| 1.6.1 连接管理 | Server层连接管理与线程分配 | High (explicit sub-section) |
| 1.6.2 解析与优化 | SQL解析与查询优化概述 | High (explicit sub-section) |
| 1.6.3 存储引擎 | 存储引擎层职责与插件式架构 | High (explicit sub-section) |
| 1.7 常用存储引擎 | MySQL常用存储引擎(InnoDB/MyISAM/Memory)概述 | Medium (introductory overview, not in-depth) |
| 1.8 关于存储引擎的一些操作 | 存储引擎DDL管理操作 | Medium (operational knowledge, borderlined with tool usage) |
| **Ch2 启动选项和系统变量** | | |
| 2.1 启动选项和配置文件 | MySQL启动选项与my.cnf配置体系 | Medium (sysadmin/infrastructure knowledge) |
| 2.2 系统变量 | MySQL系统变量体系(查看/设置/作用域) | Medium (infrastructure knowledge) |
| 2.3 状态变量 | MySQL状态变量(SHOW STATUS)监控指标 | Medium (operations/monitoring knowledge) |
| **Ch3 字符集和比较规则** | | |
| 3.1 字符集和比较规则简介 | 字符集(Charset)与比较规则(Collation)基础概念 | Medium (conceptual foundation) |
| 3.2 MySQL中支持的字符集和比较规则 | MySQL字符集(utf8/utf8mb4)与比较规则体系 | High (explicit MySQL feature, critical for correctness) |
| 3.3 字符集和比较规则的应用 | 字符集在各级别(库/表/列/通信)中的应用与比较规则查询影响 | High (explicit section on application, affects query behavior) |
| **Ch4 InnoDB记录存储结构** | | |
| 4.2 InnoDB页简介 | InnoDB页(Page)基本概念与16KB固定大小 | High (explicit section, foundational storage unit) |
| 4.3.1 指定行格式的语法 | InnoDB行格式指定语法(ROW_FORMAT参数/CREATE TABLE/ALTER TABLE) | Medium (SQL syntax, gateway to row format understanding) |
| 4.3.2 COMPACT行格式 | COMPACT行格式(变长字段长度列表/NULL值列表/记录头/真实数据) | High (explicit sub-section, most common format) |
| 4.3.3 REDUNDANT行格式 | REDUNDANT行格式(旧版兼容格式) | Medium (legacy format, mention for completeness) |
| 4.3.4 溢出列 | 行溢出机制(溢出列/溢出页/Blob存储) | High (explicit sub-section, important mechanism) |
| 4.3.5 DYNAMIC和COMPRESSED行格式 | DYNAMIC/COMPRESSED行格式与COMPACT对比 | High (explicit sub-section, modern formats) |
| **Ch5 InnoDB数据页结构** | | |
| 5.1 不同类型的页简介 | InnoDB页类型概述(数据页/索引页/Undo页等) | Medium (introductory overview) |
| 5.2 数据页结构快览 | InnoDB数据页整体结构(7个组成部分) | High (explicit section) |
| 5.3 记录在页中的存储 | 记录在页内的存储布局(记录头/infimum/supremum/next_record) | High (explicit section, core mechanism) |
| 5.4 Page Directory(页目录) | 页目录(Page Directory)与槽(Slot)二分查找机制 | High (explicit section, key lookup mechanism) |
| 5.5 Page Header(页面头部) | 页面头部(Page Header)元数据字段 | High (explicit section) |
| 5.6 File Header(文件头部) | 文件头部(File Header)跨页通用字段 | High (explicit section) |
| 5.7 File Trailer(文件尾部) | 文件尾部(File Trailer)校验和与LSN | High (explicit section, page integrity) |
| **Ch6 B+树索引** | | |
| 6.1 没有索引时的查找(单页/多页)+6.2.1 简单索引方案 | 无索引查找问题→简单索引方案(目录页+数据页)概念演进 | Medium (combined problem+conceptual build-up) |
| 6.2.2 InnoDB中的索引方案 | InnoDB B+树聚簇索引完整方案(用户记录目录+内节点+叶子节点) | High (explicit sub-section, core mechanism) |
| 6.2.3 B+树索引注意事项 | B+树根页不变性/内节点目录项/叶子节点双向链表 | High (explicit sub-section) |
| 6.2.4 MyISAM索引方案简介 | MyISAM索引方案(数据分离/索引存地址)与InnoDB对比 | Medium (explicit sub-section, cross-engine comparison) |
| 6.2.5 创建和删除索引的语句 | 索引DDL操作(CREATE/DROP INDEX) | Medium (operational) |
| **Ch7 B+树索引的使用** | | |
| 7.2 索引的代价 | B+树索引代价(空间占用/写入性能/可能退化为全表扫描) | High (trade-off understanding essential) |
| 7.3.1 扫描区间和边界条件 | B+树扫描区间(Scan Range)与WHERE条件的边界匹配规则 | High (explicit sub-section, core mechanism) |
| 7.3.2 索引用于排序 | B+树索引用于排序(利用叶子节点有序性避免filesort) | High (explicit sub-section) |
| 7.3.3 索引用于分组 | B+树索引用于GROUP BY分组优化 | High (explicit sub-section) |
| 7.4 回表的代价 | 回表代价与二级索引+聚簇索引的两阶段查找开销 | High (explicit section, critical performance concept) |
| 7.5.1 只为搜索/排序/分组列创建索引 | 索引创建原则：仅对查询/排序/分组涉及的列建索引 | Medium (design guideline) |
| 7.5.2 索引列中不重复值的个数 | 索引列基数(Cardinality)与索引选择性 | High (fundamental index effectiveness metric) |
| 7.5.3 索引列的类型尽量小 | 索引列类型大小对B+树和数据页存储的影响 | Medium (optimization guideline) |
| 7.5.4 为列前缀建立索引 | 前缀索引(Prefix Index)机制与适用场景 | High (explicit named optimization technique) |
| 7.5.5 覆盖索引 | 覆盖索引(Covering Index)消除回表操作 | High (explicit named optimization, core concept) |
| 7.5.6 索引列以列名形式单独出现 | 索引列独立出现原则(避免函数/表达式包装导致索引失效) | Medium (practical guideline) |
| 7.5.7 主键大小对插入效率的影响 | 主键大小影响B+树内节点扇出和二级索引存储 | High (mechanism-level insight, not just guideline) |
| 7.5.8 冗余和重复索引 | 冗余索引和重复索引的识别与避免 | Medium (practical guideline) |
| **Ch8 MySQL的数据目录** | | |
| 8.1 数据库和文件系统的关系 | 数据库与文件系统的映射关系(库=目录/表=文件) | Medium (fundamental mapping concept) |
| 8.2 MySQL数据目录 | MySQL数据目录(datadir)概念与确定方法 | Medium (sysadmin knowledge) |
| 8.3 数据目录的结构 | 数据目录内部结构(数据库目录/表文件/.frm/ibdata1等) | High (explicit section, storage layout) |
| 8.4 文件系统对数据库的影响 | 文件系统特性对数据库的影响(最大文件大小/文件名大小写/原子操作) | Medium (cross-cutting concern) |
| 8.5 MySQL系统数据库简介 | MySQL系统数据库(mysql/information_schema/performance_schema/sys)简介 | Medium (introductory overview) |
| **Ch9 InnoDB的表空间** | | |
| 9.2.1 区的概念 | 表空间Extent(区)概念(64个页/1MB)与管理 | High (explicit sub-section, key organizational unit) |
| 9.2.2 段的概念 | 表空间Segment(段)概念(叶子节点段+非叶子节点段+回滚段) | High (explicit sub-section, higher-level container) |
| 9.2.3 区的分类 | Extent分类(FREE/FREE_FRAG/FULL_FRAG/FSEG)与状态转换 | High (explicit sub-section, allocation model) |
| 9.2.4 段的结构 | Segment结构(INODE Entry/3个Extent链表/碎片区) | High (explicit sub-section) |
| 9.2.5 各类型页面详细情况 | 表空间中各类页面的详细结构(FSP_HDR/XDES/INODE页) | High (explicit sub-section, page-level detail) |
| 9.2.6 Segment Header结构的运用 | Segment Header(INODE Entry链/FSEG Header)的运用 | High (explicit sub-section) |
| 9.2.7 真实表空间对应的文件大小 | 表空间文件大小管理(ibd文件扩展) | Medium (detail, filesystem-level) |
| 9.3 系统表空间 | 系统表空间(ibdata1)的结构与用途(数据字典/change buffer/doublewrite) | High (explicit section) |
| **Ch10 单表访问方法** | | |
| 10.1 访问方法的概念 | MySQL单表访问方法(Access Method)概念 | Medium (introductory) |
| 10.2 const | const访问方法(主键等值/唯一二级索引等值) | High (explicit named access method) |
| 10.3 ref | ref访问方法(普通二级索引等值匹配) | High (explicit named access method) |
| 10.4 ref_or_null | ref_or_null访问方法(等值匹配+NULL值扫描) | High (explicit named access method) |
| 10.5 range | range访问方法(索引范围扫描) | High (explicit named access method) |
| 10.6 index | index访问方法(索引全扫描/覆盖索引全扫描) | High (explicit named access method) |
| 10.7 all | ALL访问方法(全表扫描) | High (explicit named access method) |
| 10.8.1 重温二级索引+回表 | 二级索引检索→回表操作的完整路径重温 | High (explicit sub-section, reinforces key concept) |
| 10.8.2 索引合并 | 索引合并(Index Merge: Intersection/Union/Sort-Union) | High (explicit sub-section, advanced optimization) |
| **Ch11 连接的原理** | | |
| 11.1.1 连接的本质 | 连接的本质(笛卡尔积+过滤条件) | High (explicit sub-section, foundational concept) |
| 11.1.2 连接过程简介 | 连接执行的两步过程(生成全组合→过滤) | High (explicit sub-section) |
| 11.1.3 内连接和外连接 | 内连接(INNER)与外连接(LEFT/RIGHT)的语义差异 | High (explicit sub-section, essential concept) |
| 11.2.1 嵌套循环连接 | 嵌套循环连接(Nested-Loop Join/NLJ)算法 | High (explicit sub-section, fundamental join algorithm) |
| 11.2.2 使用索引加快连接速度 | 基于索引的嵌套循环连接(Index Nested-Loop Join) | High (explicit sub-section, key optimization) |
| 11.2.3 基于块的嵌套循环连接 | 基于块的嵌套循环连接(Block Nested-Loop Join/Join Buffer) | High (explicit sub-section, key optimization) |
| **Ch12 基于成本的优化** | | |
| 12.1 什么是成本 | 查询成本(Cost)概念：I/O成本+CPU成本 | Medium (introductory but foundational) |
| 12.2 单表查询的成本 | 单表查询成本计算(基于索引统计数据的代价估算) | High (explicit section, core optimizer logic) |
| 12.3 连接查询的成本 | 连接查询成本计算(Condition Filtering+两表/多表代价) | High (explicit section, core optimizer logic) |
| 12.4 调节成本常数 | 成本常数调节(mysql.server_cost/engine_cost表) | Medium (detail, advanced tuning) |
| **Ch13 InnoDB统计数据收集** | | |
| 13.1 统计数据的存储方式 | InnoDB统计数据的两种存储方式(磁盘持久化/内存临时) | High (explicit section) |
| 13.2 基于磁盘的永久性统计数据 | 磁盘持久化统计(innodb_table_stats/innodb_index_stats表+定期更新) | High (explicit section, source of optimizer input) |
| 13.3 基于内存的非永久性统计数据 | 内存非持久化统计(innodb_stats_persistent=OFF时) | Medium (alternative mode) |
| 13.4 innodb_stats_method的使用 | 统计方法参数(nulls_equal/null_unequal/null_ignored) | Medium (detail, edge-case parameter) |
| **Ch14 基于规则的优化** | | |
| 14.1 条件化简 | 查询条件化简(移除括号/常量传递/移除无用条件/表达式计算/HAVING合并) | High (explicit section, core rewrite mechanism) |
| 14.2 外连接消除 | 外连接消除(OUTER→INNER转换条件) | High (explicit section, key optimization) |
| 14.3 子查询优化 | 子查询优化(子查询语法/执行方式/Materialization/半连接转换) | High (explicit section, critical optimization area) |
| **Ch15 EXPLAIN详解** | | |
| 15.1 执行计划输出中各列详解 | EXPLAIN执行计划解读(type/rows/key/Extra等11列含义) | High (explicit section, essential diagnostic tool knowledge) |
| 15.2 JSON格式的执行计划 | EXPLAIN FORMAT=JSON执行计划详解 | Medium (supplementary format) |
| 15.3 Extended EXPLAIN | EXPLAIN ANALYZE/SHOW WARNINGS扩展用法 | Medium (extended diagnostic) |
| **Ch16 optimizer trace** | | |
| 16.1-16.2 optimizer trace | Optimizer Trace调试优化器决策过程(STRAIGHT_JOIN/成本/选择原因) | Medium (tool knowledge, useful for deep optimization debugging) |
| **Ch17 Buffer Pool** | | |
| 17.2.1 Buffer Pool核心概念 | Buffer Pool定义与核心作用(内存缓存数据页/减少磁盘IO) | High (explicit sub-section, foundational concept) |
| 17.2.2-17.2.3 Buffer Pool内部组成与free链表 | Buffer Pool组成(缓冲页+控制块)与free链表(空闲页管理) | High (core mechanism, internal data structure) |
| 17.2.4 缓冲页的哈希处理 | Buffer Pool页哈希索引(表空间号+页号→缓冲页快速定位) | High (explicit sub-section, lookup mechanism) |
| 17.2.5 flush链表的管理 | flush链表(脏页链表)管理与脏页追踪机制 | High (explicit sub-section, dirty page tracking) |
| 17.2.6 LRU链表的管理 | LRU链表改进算法(young/old区+midpoint insertion)与页淘汰策略 | High (explicit sub-section, critical eviction mechanism) |
| 17.2.8 刷新脏页到磁盘 | 脏页刷新机制(从flush链表刷盘/checkpoint关联) | High (explicit sub-section) |
| 17.2.9-17.2.12 多实例与配置 | Buffer Pool多实例(innodb_buffer_pool_instances)/chunk_size/配置注意事项/状态查看 | Medium (combined: configuration+operational aspects) |
| **Ch18 事务简介** | | |
| 18.1 事务的起源-ACID | 事务ACID特性(原子性/隔离性/一致性/持久性)详解 | High (explicit section, foundational concept) |
| 18.3.1-18.3.3 事务控制语句 | 事务控制语句(BEGIN/COMMIT/ROLLBACK) | High (explicit sub-sections, essential operations) |
| 18.3.5 自动提交 | autocommit自动提交行为与隐式事务 | High (explicit sub-section, critical default behavior) |
| 18.3.6 隐式提交 | 隐式提交触发场景(DDL/锁释放/事务嵌套) | High (explicit sub-section, important pitfall) |
| 18.3.7 保存点 | 事务保存点(Savepoint)与部分回滚 | High (explicit sub-section, named feature) |
| **Ch19 redo日志** | | |
| 19.2 redo日志是什么 | redo日志概念(WAL/物理日志/崩溃恢复基础) | High (explicit section, foundational concept) |
| 19.3 redo日志格式 | redo日志格式(简单MLOG_*类型/复杂类型/物理+逻辑) | High (explicit section, format detail) |
| 19.4 Mini-Transaction | Mini-Transaction(MTR)概念(原子性写入组/MTR日志不可分割) | High (explicit section, critical internal concept) |
| 19.5 redo日志的写入过程 | redo log block结构→log buffer写入→刷盘流程 | High (explicit section, write path) |
| 19.6 redo日志文件 | redo日志文件(刷盘时机/文件组/ib_logfile格式) | High (explicit section, file-level detail) |
| 19.7 LSN(Log Sequence Number) | LSN概念(flushed_to_disk_lsn/lsn偏移量映射/flush链表lsn) | High (explicit section, core sequence number) |
| 19.8 checkpoint | checkpoint机制(LSN推进/脏页控制/崩溃恢复加速) | High (explicit section, key recovery mechanism) |
| 19.9 用户线程批量刷脏页 | 用户线程参与flush链表脏页批量刷出 | Medium (optimization detail) |
| 19.11 innodb_flush_log_at_trx_commit | innodb_flush_log_at_trx_commit参数(0/1/2三种策略)详解 | High (explicit section, critical durability parameter) |
| 19.12 崩溃恢复 | 崩溃恢复流程(确定恢复起点/终点/恢复执行) | High (explicit section, core reliability mechanism) |
| **Ch20 undo日志** | | |
| 20.2 事务id | 事务ID(分配时机/生成方式/trx_id隐藏列) | High (explicit section) |
| 20.3 undo日志的格式 | undo日志格式(INSERT/DELETE/UPDATE三操作对应)与roll_pointer | High (explicit section, format detail) |
| 20.5 FIL_PAGE_UNDO_LOG页面 | FIL_PAGE_UNDO_LOG页面结构(Undo Page Header/Undo Record Segment) | High (explicit section) |
| 20.6 Undo页面链表 | Undo页面链表结构(单事务内/多事务间Undo链) | High (explicit section, data structure) |
| 20.7 undo日志写入过程 | undo日志写入过程(Undo Log Segment Header/Log Header/详细步骤) | High (explicit section, write path) |
| 20.8 重用Undo页面 | Undo页面重用机制(链表复用/空间管理) | Medium (optimization detail) |
| 20.9 回滚段 | 回滚段(Rollback Segment)体系(概念/申请/多个回滚段/分类/roll_pointer组成) | High (explicit section, core undo architecture) |
| 20.10 回滚段相关配置 | 回滚段配置(innodb_rollback_segments/undo表空间) | Medium (configuration detail) |
| 20.11 undo日志在崩溃恢复时的作用 | undo日志在崩溃恢复中的作用(未提交事务回滚+MVCC版本链保留) | High (explicit section, critical recovery role) |
| **Ch21 事务隔离级别与MVCC** | | |
| 21.2 事务隔离级别 | 事务隔离级别(并发问题/READ UNCOMMITTED~SERIALIZABLE 4级/MySQL实现) | High (explicit section, fundamental concept) |
| 21.3 MVCC原理 | MVCC原理(版本链/ReadView/可见性规则/二级索引MVCC) | High (explicit section, core concurrency mechanism) |
| 21.4 purge | Purge线程(清理过期undo/删除标记记录物理删除) | High (explicit section, important maintenance mechanism) |
| **Ch22 锁** | | |
| 22.1 并发控制的两种方式 | 并发控制基础(写-写/读-写/一致性读/锁定读/写操作加锁) | High (explicit section, foundational) |
| 22.2 多粒度锁 | 多粒度锁(表级意向锁IS/IX+S/X锁) | High (explicit section, key lock hierarchy) |
| 22.3 MySQL中的行锁和表锁 | InnoDB行锁(Record/Gap/Next-Key/Insert Intention)与表锁+锁内存结构 | High (explicit section, complete lock type coverage) |
| 22.4 语句加锁分析 | 语句加锁分析(SELECT锁定读/半一致性读/INSERT的加锁规则) | High (explicit section, practical lock analysis) |
| 22.5 查看事务加锁情况 | 查看锁信息(information_schema锁表/SHOW ENGINE INNODB STATUS) | Medium (operational/diagnostic) |
| 22.6 死锁 | 死锁(发生条件/检测机制/处理策略/避免方法) | High (explicit section, critical production concern) |

> **B2 说明**:
> - Ch0(楔子) + Ch1.2-1.4(安装/启动) → 跳过(非KP)
> - Ch7.5 的 8 个子节全拆——每项是独立索引优化技术(覆盖索引/前缀索引等)
> - Ch16(optimizer trace) → 1 KP(工具知识, 降Medium)
> - Ch17.2.7(其他链表) → 跳过(边缘细节); Ch17.2.9-17.2.12 → 合并为配置KP
> - Ch19.10(查看LSN)/19.13(LOG_BLOCK_HDR_NO)/19.1(事先说明) → 跳过(操作/边缘)
> - Ch20.1(回滚需求)/20.4(通用链表结构) → 跳过(动机/基础设施)
> - Ch19-Ch20 TOC 粒度极细(§X.Y.Z 三级)→保留机制级 KP
> - B2深审R1: 3发现(遗漏2+footer矛盾1), 全部修复; 125→127 KPs


### [01 #3/14 done] Book 3: 数据库内核揭秘 (林金河, 9章) — 61 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch1 概述** | | |
| 1.1 数据库与数据库管理系统 | 数据库与DBMS的基本概念与职责划分 | Medium (introductory overview) |
| 1.3 数据模型 | 数据模型(关系模型/文档模型/键值模型/图模型)对比 | High (explicit section, fundamental concept) |
| 1.4 模块化(计算引擎/存储引擎) | 数据库计算引擎与存储引擎的模块化分离架构 | High (explicit section, architecture concept) |
| **Ch2 软件和硬件基础** | | |
| 2.1 多处理器架构(SMP/AMP) | 对称多处理器(SMP)与非对称多处理器(AMP)架构 | Medium (hardware prerequisite knowledge) |
| 2.2 CPU(高速缓存/流水线/SIMD) | CPU特性(缓存层次/流水线/SIMD指令)对数据库性能的影响 | Medium (hardware prerequisite knowledge) |
| 2.3 内存管理(虚拟内存/页表/缺页/TLB) | 虚拟内存管理(页表/TLB/缺页处理)与数据库缓冲池的关系 | High (explicit section, critical OS-DB intersection) |
| 2.4 存储设备(机械硬盘/固态硬盘) | 存储设备特性(HDD随机/顺序性能差 vs SSD无寻道/NAND擦写)对数据库IO设计的影响 | High (explicit section, storage hardware fundamentals) |
| 2.5 文件系统接口(缓冲I/O/直接I/O/异步I/O/io_uring) | 文件系统IO接口(缓冲IO/直接IO(Direct IO)/异步IO(AIO)/io_uring)对比与适用场景 | High (explicit section, I/O interface knowledge) |
| **Ch3 存储结构** | | |
| 3.1 页式存储 | 页式存储(Page-based Storage):页内记录管理与文件组织 | High (explicit section, fundamental storage model) |
| 3.2 日志式存储 | 日志式存储(Log-structured Storage):追加写+合并(Merge)模型 | High (explicit section, alternative storage paradigm) |
| 3.3 行式存储和列式存储 | 行式/列式/行列混合存储(PAX/Hybrid)的存储布局与查询模式对比 | High (explicit section, fundamental layout choice) |
| 3.4 数据压缩和编码 | 数据库压缩与编码技术(通用压缩/游程编码/位压缩/前缀压缩/字典编码/FSST) | High (explicit section, compression is critical for DB performance) |
| **Ch4 缓冲池** | | |
| 4.1 内存映射(接口原理/内存映射与缓冲池) | 内存映射(mmap)与缓冲池(Buffer Pool)两种缓存策略的设计对比 | High (explicit section, two fundamental caching approaches) |
| 4.2 缓冲池结构 | 缓冲池内部结构(页表/页帧/空闲链表/脏页链表) | High (explicit section, core data structure) |
| 4.3 缓存替换算法(LRU/FIFO/Clock/LFU/LRU-K/LRFU/LIRS) | 缓存替换算法族(LRU/FIFO/Clock/LFU/LRU-K/LRFU/LIRS)对比与适用场景 | High (explicit section, 7 algorithms covering fundamental page eviction strategies) |
| 4.4 脏页落盘的原子性(MySQL双写机制/PostgreSQL整页写入) | 脏页落盘原子性保证(MySQL Doublewrite Buffer / PostgreSQL Full Page Write) | High (explicit section, critical reliability mechanism) |
| 4.5 优化-多缓冲池与缓存污染隔离 | 多缓冲池实例(multiple buffer pool instances)与缓存污染隔离(scan-resistant)优化 | High (explicit sub-optimization, impacts multi-tenant performance) |
| 4.5 优化-预读取/缓冲池旁路/扫描共享 | 预读取(Read-ahead)、缓冲池旁路(Bypass)与扫描共享(Synchronous Scan)优化 | High (explicit sub-optimizations, query performance critical) |
| **Ch5 索引结构：哈希表** | | |
| 5.1 哈希表基本原理 | 哈希表作为数据库索引的基本原理(桶/槽/冲突) | High (explicit section, foundational concept) |
| 5.2 哈希函数 | 哈希函数设计与分布特性(MurmurHash/xxHash/CRC) | High (explicit section, core component) |
| 5.3 链接法 | 链接法(Chaining)解决哈希冲突 | High (explicit section, fundamental collision resolution) |
| 5.4 开放寻址法 | 开放寻址法(Open Addressing)族(线性探测/二次探测/双重哈希/删除标记) | High (explicit section, alternative collision strategy) |
| 5.5 Cuckoo Hashing | Cuckoo Hashing(布谷鸟哈希)双表驱逐算法 | High (explicit section, named algorithm) |
| 5.6 Hopscotch Hashing | Hopscotch Hashing(跳房子哈希)邻域探测优化 | High (explicit section, named algorithm) |
| 5.7 Robin Hood Hashing | Robin Hood Hashing(劫富济贫哈希)基于探测距离的公平置换 | High (explicit section, named algorithm) |
| 5.8 扩容(重新哈希/线性哈希) | 哈希表扩容策略(全量重新哈希/线性哈希增量扩容) | High (explicit section, critical scalability concern) |
| 5.9 完美哈希 | 完美哈希(Perfect Hashing)静态键集O(1)查找 | High (explicit section, named algorithm) |
| **Ch6 索引结构：LSM树** | | |
| 6.1 LSM树基本原理 | LSM树(Log-Structured Merge Tree)基本原理:内存表→SST文件→Compaction | High (explicit section, foundational LSM concept) |
| 6.2 内存表 | LSM内存表(MemTable)设计(跳表/红黑树/ART)与写入缓冲 | High (explicit section, key LSM component) |
| 6.3 合并(Compaction) | LSM Compaction合并策略(分层合并/分级合并/组合合并)与写放大 | High (explicit section, core LSM maintenance mechanism) |
| 6.4 点查询(布隆过滤器/布谷鸟过滤器/异或过滤器/带状过滤器) | LSM点查询加速过滤器(布隆/Bloom+Blocked/Cuckoo/XOR/Ribbon) | High (explicit section, probabilistic filters for read optimization) |
| 6.5 范围查询(前缀布隆过滤器/SuRF/REMIX) | LSM范围查询加速(前缀布隆/SuRF Learned Index/REMIX重排) | High (explicit section, range scan optimization) |
| 6.6 键值分离 | 键值分离(Key-Value Separation/WiscKey)降低Compaction写放大 | High (explicit section, advanced LSM optimization) |
| **Ch7 索引结构：B树家族** | | |
| 7.1 B树(搜索/插入/删除) | B树(B-Tree)搜索/插入/删除算法与磁盘I/O模型 | High (explicit section, foundational index structure) |
| 7.2 B+树(搜索/插入/删除) | B+树(B+Tree)搜索/插入/删除与叶子节点链表 | High (explicit section, most widely used DB index) |
| 7.3 B树并发控制 | B树并发控制(锁分支(Latching)/乐观锁分支(Optimistic Latching)/死锁问题) | High (explicit section, critical for multi-threaded DB engines) |
| 7.4 Blink树 | Blink树(B-link Tree)高并发B+树变体(兄弟指针/非阻塞搜索) | High (explicit section, named concurrent index) |
| 7.5 OLFIT树 | OLFIT树(Optimistic Latch-Free Index Traversal)无锁乐观索引遍历 | High (explicit section, named algorithm) |
| 7.6 Bw树 | Bw树(Bw-Tree)无锁索引结构(增量记录Delta/结点分裂合并映射表) | High (explicit section, modern latch-free index) |
| **Ch8 故障恢复** | | |
| 8.1 故障类型 | 数据库故障分类(事务故障/系统故障/介质故障) | High (explicit section, fault model taxonomy) |
| 8.2 影子分页 | 影子分页(Shadow Paging)原子性更新机制 | High (explicit section, classical crash recovery approach) |
| 8.3 预写式日志-重做日志(Redo) | 重做日志(Redo Log):记录已提交修改→崩溃后重放恢复已提交数据 | High (explicit sub-topic, distinct from Undo: redo=重放, undo=回滚) |
| 8.3 预写式日志-回滚日志(Undo) | 回滚日志(Undo Log):记录修改前值→崩溃后回滚未提交事务 | High (explicit sub-topic, distinct from Redo: undo=撤销, redo=重放) |
| 8.3 预写式日志-重做回滚组合 | 重做-回滚组合(Redo-Undo):同时记录新旧值→支持任意恢复策略 | High (explicit sub-topic, combined approach) |
| 8.4 物理日志和逻辑日志 | 物理日志(Physical)vs逻辑日志(Logical)vs物理-逻辑日志(Physiological)对比 | High (explicit section, log design taxonomy) |
| 8.5 刷盘策略 | WAL刷盘策略(Force/No-Force/Steal/No-Steal)与恢复需求对应 | High (explicit section, critical durability-performance trade-off) |
| 8.6 检查点 | 检查点(Checkpoint)机制(日志截断/恢复加速/脏页控制) | High (explicit section, essential recovery acceleration) |
| 8.7 ARIES | ARIES恢复算法(日志序列号LSN/事务提交/回滚/模糊检查点/三阶段恢复) | High (explicit section, industry-standard recovery algorithm) |
| 8.8 MARS和WBL | MARS(多核优化ARIES)与WBL(Write-Behind Logging)替代恢复方案 | High (explicit section, advanced recovery variants) |
| **Ch9 并发控制** | | |
| 9.1 事务(冲突/异常/隔离级别) | 事务概念(冲突类型/并发异常/四种隔离级别) | High (explicit section, foundational concurrency concepts) |
| 9.3 多版本并发控制(MVCC) | MVCC多版本并发控制(版本链/时间戳/可见性判断) | High (explicit section, dominant concurrency mechanism) |
| 9.4 基于锁的并发控制-锁类型与粒度 | 锁类型(共享S/排他X/意向IS/IX)与锁粒度(行级/页级/表级) | High (explicit sub-topic, lock taxonomy) |
| 9.4 基于锁的并发控制-两阶段锁 | 两阶段锁(2PL)/严格两阶段锁(SS2PL)/多版本两阶段锁(MV2PL)协议 | High (explicit sub-topic, foundational lock protocol) |
| 9.4 基于锁的并发控制-死锁处理 | 死锁检测(等待图)与处理策略(超时/死锁检测/死锁预防) | High (explicit sub-topic, critical production concern) |
| 9.4 基于锁的并发控制-热点优化 | 锁热点优化(锁分段/锁剥离/乐观锁退化) | High (explicit sub-topic, performance-critical optimization) |
| 9.5 基于时间戳顺序的并发控制 | 时间戳顺序并发控制(T/O算法/托马斯写入规则) | High (explicit section, alternative concurrency approach) |
| 9.6 乐观并发控制(OCC) | 乐观并发控制(OCC):验证阶段(向后验证/向前验证/并行验证)/可序列化条件 | High (explicit section, important alternative concurrency model) |
| 9.7 基于有向序列化图的并发控制 | 序列化快照隔离(SSI)/有向序列化图(DSG)并发控制 | High (explicit section, advanced concurrency control) |
| 9.8 多版本记录的存储方式 | MVCC多版本记录存储(追加写/时间穿梭/增量存储) | High (explicit section, MVCC implementation detail) |
| 9.9 多版本记录的过期回收 | MVCC过期版本回收(Garbage Collection/保留策略) | High (explicit section, critical MVCC maintenance) |
| 9.10 多版本数据的索引管理 | 多版本数据索引管理(主索引版本链/二级索引多版本) | High (explicit section, index-MVCC interaction) |

> **B3 说明**:
> - Ch1.2(为什么需要DBMS) → 跳过(动机叙述, 非KP)
> - Ch5.10(总结)/Ch8.9(总结) → 跳过
> - Ch9.2(并发控制算法概述) → 跳过(导览, 9.3-9.7各自展开)
> - Ch4.5(5项优化)分解为2 KP(多缓冲池+隔离 vs 预读+bpass+共享)——不同设计维度
> - Ch9.4(7项锁机制)分解为4 KP(锁类型/2PL/死锁/热点)——核心机制各不相同
> - Ch8.3(3类WAL)分解为3 KP(Redo/Undo/Redo-Undo)——Redo重放≠Undo回滚,本质不同(深审R1修复)
> - B3深审R1: 1发现(8.3粒度过粗), 已修复; 59→61 KPs
> - B3视角不同于B1/B2:通用数据库原理(非MySQL专属),覆盖LSM/Hash/Bw-tree/MVCC理论


### [01 #4/14 done] Book 4: 深入浅出存储引擎 (文小飞, 9章) — 40 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch1 存储引擎概述** | | |
| 1.1 数据存储体系(OLTP/OLAP/HTAP/NoSQL/NewSQL/内存型/磁盘型/读写) | 数据库存储体系全景(OLTP/OLAP/HTAP/NoSQL/NewSQL/内存与磁盘型/读写模式) | Medium (broad overview, multiple taxonomies in one section) |
| 1.2 数据存储的核心(整体架构/共性问题) | 存储引擎核心架构(接口层/索引层/存储层)与共性问题(一致性/持久性/并发) | High (explicit section on core concepts) |
| 1.3 存储引擎的分类(B+树系/LSM派系) | 存储引擎两大派系:B+树系(读优化)vs LSM派系(写优化) | High (explicit section, fundamental taxonomy) |
| **Ch2 索引数据结构** | | |
| 2.2 Hash类数据结构(Hash表/位图/布隆过滤器) | Hash表/位图/布隆过滤器在存储引擎中的索引应用 | High (explicit section, 3 named data structures) |
| 2.3 二叉树类数据结构(二叉搜索树/红黑树/跳表) | 二叉搜索树/红黑树/跳表在存储引擎内存结构中的应用 | High (explicit section, 3 named data structures) |
| 2.4 多叉树类数据结构(B树/B+树/其他多叉树) | B树/B+树/多叉树作为磁盘存储引擎索引的适配性 | High (explicit section, 3 named data structures) |
| **Ch3 数据存储介质** | | |
| 3.1 内存(基本内容/管理机制/虚拟内存管理) | 内存管理(虚拟内存/页表/内存映射)在存储引擎中的应用 | Medium (OS prerequisite, contextualized for storage engines) |
| 3.2 持久化内存 | 持久化内存(Persistent Memory/NVDIMM)作为新型存储介质的特性与应用 | Medium (emerging technology, niche) |
| 3.3 磁盘(基本内容/管理机制/加速访问方案) | 磁盘特性(HDD机械/SSD闪存)与加速访问方案(缓存/O_DIRECT)在存储引擎中的应用 | Medium (hardware prerequisite, contextualized) |
| **Ch4 宏观理解 B+树存储引擎** | | |
| 4.1 B+树产生背景与设计目标 | B+树存储引擎的诞生背景与设计目标(数据持久化/高效检索) | Medium (motivation + goals, conceptual) |
| 4.2 B+树方案选型(数据结构对比/磁盘因素/索引维护/B树vsB+树) | B+树方案选型(数据结构对比/磁盘I/O适配/索引维护代价/B树vsB+树抉择) | High (explicit section, design decision framework) |
| 4.3 B+树方案选型结果(方案选型/反向论证) | B+树选型结果审定与反向论证(为什么不是其他方案) | Medium (review/conclusion, reinforces 4.2) |
| **Ch5 B+树存储引擎工程细节** | | |
| 5.1 边界条件处理(磁盘内存映射/读操作/写操作) | B+树引擎边界条件:磁盘内存映射/mmap/读写操作处理 | High (explicit section, implementation details) |
| 5.2 异常情况处理(数据部分写入异常处理) | 数据部分写入(Torn Write)异常处理机制 | High (explicit section, critical reliability concern) |
| 5.3 事务(基本概念/并发控制) | 存储引擎事务支持(基本概念/并发控制) | High (explicit section, fundamental concept) |
| 5.4 范围查询与全量遍历 | B+树范围查询(Range Scan)与全量遍历(Full Scan)实现 | High (explicit section, key query pattern) |
| **Ch6 BoltDB 核心源码分析** | | |
| 6.1 BoltDB整体结构(项目结构/实现架构) | BoltDB整体架构(项目结构/模块划分/数据流) | High (explicit section, Go实现的嵌入式B+树引擎) |
| 6.2 page-基本结构 | BoltDB page基本结构(页头/数据区/类型标识) | High (explicit sub-topic, page format foundation) |
| 6.2 page-元数据页与空闲列表页 | BoltDB元数据页(meta page)与空闲列表页(freelist page)管理 | High (explicit sub-topic, allocation management pages) |
| 6.2 page-分支节点与叶子节点页 | BoltDB分支节点页(branch page)与叶子节点页(leaf page)结构 | High (explicit sub-topic, B+tree node pages) |
| 6.3 node-结构(B+树结构/node结构) | BoltDB node结构(B+树节点/内存表示/inode数组) | High (explicit sub-topic, node data structure) |
| 6.3 node-增删改查与分裂合并 | BoltDB node增删改查操作与node分裂合并算法 | High (explicit sub-topic, core operations) |
| 6.4 Bucket解析(结构/Cursor/增删改查/KV) | BoltDB Bucket(子B+树容器)结构/Cursor光标/CRUD/分裂合并 | High (explicit section, BoltDB核心抽象) |
| 6.5 Tx解析(Commit/Rollback) | BoltDB事务(Tx)读写事务管理与Commit/Rollback实现 | High (explicit section, ACID support) |
| 6.6 DB解析(Open/Begin/Update/View/Batch) | BoltDB数据库(DB)生命周期:Open/Begin/Update/View/Batch操作 | High (explicit section, database-level API) |
| **Ch7 LSM Tree 原理** | | |
| 7.2 从零推导LSM-写入路径 | LSM Tree写入路径:MemTable+WAL追加→批量刷盘SST | High (explicit sub-topic, write mechanism) |
| 7.2 从零推导LSM-读取路径 | LSM Tree读取路径:MemTable→SST层级搜索→合并返回 | High (explicit sub-topic, read mechanism distinct from write) |
| 7.3 LSM Tree的架构演进(双组件/多组件/实际结构) | LSM Tree架构演进(双组件→多组件→实际分层结构) | High (explicit section, design evolution) |
| 7.4 LSM Tree核心问题(Compaction/分区/读写放大/优化) | LSM Tree核心问题:Compaction合并/数据分区/读写放大与空间放大/写放大优化 | High (explicit section, four critical design challenges) |
| **Ch8 LSM 派系存储引擎** | | |
| 8.1 LSM Tree存储引擎(WiscKey KV分离) | LSM Tree存储引擎(WiscKey键值分离降低Compaction写放大) | High (explicit section, named implementation) |
| 8.2 LSM Hash存储引擎(BiLash/Bitcask) | LSM Hash存储引擎(BiLash哈希索引/Bitcask日志结构哈希表) | High (explicit section, named implementations) |
| 8.3 LSM Array存储引擎(Moss) | LSM Array存储引擎(Moss有序数组Compaction) | High (explicit section, named implementation) |
| 8.4 其他LSM存储引擎(Kafka存储引擎) | Kafka存储引擎(基于LSM思想的分段日志+稀疏索引) | Medium (explicit section, cross-domain application) |
| **Ch9 LevelDB 核心源码分析** | | |
| 9.1 LevelDB整体架构 | LevelDB整体架构(LSM树实现/C++嵌入式KV存储/项目模块) | High (explicit section, system overview) |
| 9.2 DB核心接口(DB结构/Open/Put/Delete/Get) | LevelDB核心接口:DB数据结构/Open/Put/Delete/Get实现 | High (explicit section, public API + internal structure) |
| 9.3 MemTable实现(结构/Add/Get/SkipList) | LevelDB MemTable实现(跳表SkipList/Add/Get/内存数据组织) | High (explicit section, write buffer component) |
| 9.4 WAL日志实现(日志格式/Writer/Reader) | LevelDB WAL日志(Log格式/Writer追加/Reader读取/崩溃恢复) | High (explicit section, durability guarantee) |
| 9.5 SSTable实现(数据格式/Block读写/SSTable读写) | LevelDB SSTable实现(Data Block/Index Block/Filter Block/SSTable读写) | High (explicit section, persistent file format) |
| 9.6 Compact实现(Minor Compact/Major Compact) | LevelDB Compaction实现(Minor: MemTable→SST / Major: 层级合并) | High (explicit section, core maintenance mechanism) |
| 9.7 多版本实现(Version/VersionEdit/VersionSet) | LevelDB MVCC多版本实现(Version快照/VersionEdit增量/VersionSet管理) | High (explicit section, snapshot isolation support) |

> **B4 说明**:
> - Ch1.4/Ch2.1/Ch2.5/Ch3.4/Ch4.4/Ch5.5/Ch6.7/Ch7.1/Ch7.5/Ch8.5/Ch9.8(小结/背景/基础CS) → 跳过
> - Ch6.2(5种page)分解为3 KP(基本结构+meta/freelist页面+branch/leaf节点)——不同页面角色
> - Ch6.3(5种node操作)分解为2 KP(结构+增删改查/分裂合并)——结构与操作分离
> - Ch7.2(读写路径)分解为2 KP(写入+读取)——LSM读写机制本质不同
> - B4独有价值:BoltDB(Go)与LevelDB(C++)源码级实现分析, B1-B3无此深度
### [01 #5/14 done] Book 5: MySQL 8 查询性能优化 (Jesper Wisborg Krogh, 27章) — 98 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Part I — Ch1 MySQL性能优化** | | |
| 1.3 查询的生命周期 | MySQL查询生命周期(连接→解析→优化→执行→返回结果) | High (explicit section, foundational concept) |
| **Part I — Ch2 查询优化方法论** | | |
| 2.2-2.6 优化方法论(核实问题/确定原因/解决方案/实施/主动监控) | 结构化查询优化方法论(核实问题→确定原因→解决方案→实施→主动监控) | High (explicit methodology, essential workflow) |
| **Part I — Ch3 Sysbench基准测试** | | |
| 3.1-3.6 Sysbench基准测试 | Sysbench基准测试方法(安装/执行/自定义基准/TPC标准) | Medium (tool knowledge, benchmarking fundamentals) |
| **Part II — Ch5 performance_schema** | | |
| 5.2 线程 | Performance Schema线程监控模型 | High (explicit section, instrumentation foundation) |
| 5.3 instrument | Performance Schema instrument(检测点)注册与分类机制 | High (explicit section, core concept) |
| 5.4 消费者 | Performance Schema consumer(消费者表)数据处理管道 | High (explicit section, data flow) |
| 5.5 事件(事件类型/事件范围/事件嵌套/事件属性) | Performance Schema事件模型(类型/范围/嵌套/属性) | High (explicit section, event taxonomy) |
| 5.6 Actor与对象 | Performance Schema中Actor(用户/主机/角色)与Object(表/文件)追踪 | High (explicit section) |
| 5.7 摘要 | Performance Schema摘要(digest)表与SQL归一化 | High (explicit section, query fingerprinting) |
| 5.8 表类型 | Performance Schema表类型(当前/历史/汇总/实例) | Medium (reference classification) |
| 5.9 动态配置 | Performance Schema运行时动态配置(启用/禁用instrument) | Medium (operational) |
| **Part II — Ch6 sys库** | | |
| 6.1 sys库配置 | sys库配置(sysconfig表)与视图依赖 | Medium (configuration knowledge) |
| 6.2 格式化函数 | sys库格式化函数(时间/字节/语句格式化) | Medium (utility knowledge) |
| 6.3 视图 | sys库核心视图(语句/等待/IO/内存/Schema分析) | High (explicit section, key monitoring interface) |
| 6.4 辅助函数与过程 | sys库辅助存储过程与诊断函数 | Medium (utility knowledge) |
| **Part II — Ch7 information_schema** | | |
| 7.1 何为information库+7.2 权限 | information_schema概念与访问权限模型 | Medium (introductory + security context) |
| 7.3 视图(系统信息/方案信息/性能信息/权限信息) | information_schema视图分类(系统/方案/性能/权限四类信息) | High (explicit section, key metadata source) |
| 7.4 索引统计数据缓存 | information_schema中的索引统计数据(INDEX_STATISTICS)缓存机制 | High (explicit section, optimizer-relevant) |
| **Part II — Ch8 SHOW语句** | | |
| 8.1-8.2 SHOW与information_schema/performance_schema关系 | SHOW语句与information_schema/performance_schema的表映射关系 | Medium (relationship knowledge) |
| 8.3 引擎状态 | SHOW ENGINE INNODB STATUS输出(信号量/事务/IO/缓冲池)解读 | High (explicit section, essential diagnostic) |
| 8.4 复制与二进制日志 | SHOW BINARY LOGS/MASTER STATUS/REPLICA STATUS复制诊断 | High (explicit section, replication monitoring) |
| 8.5 其他SHOW语句 | SHOW STATUS/PROCESSLIST/VARIABLES等其他常用诊断语句 | Medium (miscellaneous but commonly used diagnostics) |
| **Part II — Ch9 慢查询日志** | | |
| 9.1 配置 | 慢查询日志配置(long_query_time/min_examined_row_limit/log_output) | High (explicit section, essential configuration) |
| 9.2 日志事件 | 慢查询日志事件格式(查询时间/锁定时间/行数/时间戳) | High (explicit section, log content) |
| 9.3 汇总 | 慢查询日志汇总分析(mysqldumpslow/pt-query-digest) | High (explicit section, analysis toolchain) |
| **Part III — Ch10 MySQL Enterprise Monitor** | | |
| 10.5 查询分析器 | MySQL Enterprise Monitor查询分析器(Query Analyzer) | Medium (tool knowledge, enterprise monitoring) |
| **Part III — Ch11 MySQL Workbench** | | |
| 11.3 使用MySQL Workbench(EXPLAIN可视化) | MySQL Workbench可视化执行计划(Visual Explain)与查询格式化 | Medium (tool knowledge, EXPLAIN visualization) |
| **Part III — Ch12 MySQL Shell** | | |
| 12.4 报表基础架构 | MySQL Shell报表基础架构(reporting framework) | Medium (tool knowledge, shell automation) |
| **Part IV — Ch13 数据类型** | | |
| 13.1 为何是数据类型(验证/文档/优化/性能/排序) | 正确数据类型选择对性能的影响(存储优化/排序/验证/IO) | Medium (motivational but practical implications) |
| 13.2 MySQL数据类型(数值/日期时间/字符串/JSON/空间) | MySQL 8数据类型全景(数值/日期时间/字符串二进制/JSON/空间/混合) | High (explicit section, data type taxonomy) |
| 13.3 性能 | 数据类型选择的性能影响(存储大小/索引效率/比较开销) | High (explicit section, performance implications) |
| 13.4 应该选择何种数据类型 | 数据类型选择决策指南(数值优选/最小化存储/避免隐式转换) | Medium (design guidelines) |
| **Part IV — Ch14 索引** | | |
| 14.3 索引的限制 | 索引的限制(键长度/列数/全文索引/空间索引约束) | High (explicit section, boundary knowledge) |
| 14.5 索引的缺点 | 索引的代价(写入开销/空间占用/优化器选择负担) | High (explicit section, trade-off understanding) |
| 14.6 索引类型(B-tree/全文/空间/多值/哈希) | MySQL 8索引类型(B-tree/全文(FULLTEXT)/空间(SPATIAL)/多值(Multi-Valued)/哈希) | High (explicit section, index taxonomy) |
| 14.7 索引特性(函数索引/前缀/不可见/降序/分区/自生成) | MySQL 8索引高级特性(函数索引/前缀索引/不可见索引/降序索引/分区索引) | High (explicit section, MySQL 8-specific features) |
| 14.8 InnoDB与索引(簇聚索引/二级索引/建议) | InnoDB索引结构(聚簇索引/二级索引/自适应哈希/最佳用例) | High (explicit section, InnoDB-specific) |
| 14.9 索引策略(何时添加/主键选择/二级索引/多列索引/覆盖索引) | 索引策略决策(主键选择/二级索引设计/多列索引顺序/覆盖索引) | High (explicit section, practical strategy) |
| **Part IV — Ch15 索引统计信息** | | |
| 15.2 InnoDB索引统计(统计收集/页采样/隔离级别/配置) | InnoDB索引统计信息收集机制(持久化/页采样/事务隔离级别影响) | High (explicit section, optimizer input source) |
| 15.3 持久索引统计(配置/索引统计表) | 持久索引统计信息(mysql.innodb_table_stats/innodb_index_stats表) | High (explicit section, persistent store) |
| 15.4 临时索引统计 | 临时索引统计信息(非持久化模式/innodb_stats_persistent=OFF) | Medium (alternative mode) |
| 15.5 监控 | 索引统计信息监控(innodb_stats_auto_recalc/统计过期检测) | Medium (monitoring knowledge) |
| 15.6 更新统计信息(自动更新/ANALYZE TABLE/mysqlcheck) | 索引统计信息更新(自动触发/手动ANALYZE TABLE/mysqlcheck工具) | High (explicit section, maintenance operation) |
| **Part IV — Ch16 直方图** | | |
| 16.2 何时应该添加直方图 | 直方图适用场景(数据分布不均/等值查询优化/JOIN列) | Medium (application guidance) |
| 16.3 直方图内部信息(bucket/累积频率/类型) | MySQL直方图内部机制(bucket/累积频率/Singleton等高/Equi-height等宽) | High (explicit section, mechanism detail) |
| 16.4 直方图的添加与维护 | 直方图管理(ANALYZE TABLE UPDATE HISTOGRAM/DROP HISTOGRAM) | Medium (operational) |
| **Part IV — Ch17 查询优化器** | | |
| 17.1 转换 | 查询优化器语句转换(常量折叠/外连接→内连接/子查询转换) | High (explicit section, rewrite phase) |
| 17.2 基于成本的优化 | 查询优化器基于成本的选择(Cost Model: I/O成本+CPU成本估算) | High (explicit section, core optimizer logic) |
| 17.3 联接算法(嵌套循环/块嵌套循环/哈希联接) | MySQL联接算法(嵌套循环连接NLJ/块嵌套循环BNL/哈希连接Hash Join) | High (explicit section, MySQL 8.0.18+新增Hash Join) |
| 17.4 联接优化(索引合并/MRR/BKA/其他) | MySQL联接优化技术(索引合并Index Merge/MRR/BKA/Semijoin/LooseScan) | High (explicit section, optimization techniques) |
| 17.5 配置优化器(引擎成本/服务器成本/优化器开关/提示/配置) | 优化器配置(engine_cost/server_cost表/optimizer_switch/优化器提示/Optimizer Hint) | High (explicit section, tunable parameters) |
| 17.6 资源组 | MySQL资源组(Resource Group)CPU亲和性分配 | Medium (explicit section, MySQL 8 feature) |
| **Part IV — Ch18 锁原理与监控** | | |
| 18.2 锁访问级别 | MySQL锁访问级别(S锁/X锁/意向锁IS/IX) | High (explicit section, lock type hierarchy) |
| 18.3 锁粒度(用户级/刷新/MDL/显式表/隐式表/记录/gap/插入意向/自增/备份/日志) | MySQL锁粒度全谱系(用户级/刷新/MDL/表级锁/记录锁/gap锁/插入意向锁/自增锁/备份锁) | High (explicit section, comprehensive lock taxonomy) |
| 18.4 获取锁失败(MDL超时/InnoDB锁超时/死锁) | 锁争用处理(MDL超时/lock_wait_timeout/死锁检测与回滚) | High (explicit section, conflict resolution) |
| 18.5 减少锁问题(事务大小/索引/记录访问顺序/隔离级别/抢占锁) | 锁优化策略(事务粒度/索引覆盖/访问顺序/隔离级别降级/SELECT FOR UPDATE NOWAIT/SKIP LOCKED) | High (explicit section, practical optimization) |
| 18.6 监控锁 | 锁监控(performance_schema.data_locks/data_lock_waits/sys.innodb_lock_waits) | Medium (monitoring knowledge) |
| **Part V — Ch19 查找待优化的查询** | | |
| 19.1 performance库(语句事件表/prepared汇总/表IO/文件IO/错误) | 通过Performance Schema定位慢查询(events_statements_*/表IO/文件IO/错误汇总) | High (explicit section, query identification) |
| 19.2 sys库(语句视图/表IO/文件IO/语句性能分析器) | 通过sys库定位慢查询(statement_analysis/表IO视图/语句性能分析器) | High (explicit section, query identification) |
| 19.5 慢查询日志 | 通过慢查询日志定位性能瓶颈(配置阈值/日志解析) | High (explicit section, slow log analysis) |
| **Part V — Ch20 分析查询** | | |
| 20.1 EXPLAIN用法(显式查询/EXPLAIN ANALYZE/连接用法) | EXPLAIN执行计划获取(显式EXPLAIN/EXPLAIN ANALYZE实际执行统计/FOR CONNECTION) | High (explicit section, EXPLAIN modes) |
| 20.2 EXPLAIN格式(传统/JSON/树状/Visual Explain) | EXPLAIN输出格式(传统表格/JSON/树状TREE/Visual Explain可视化) | High (explicit section, output formats) |
| 20.3 EXPLAIN输出(字段/选择类型/访问类型/Extra信息) | EXPLAIN输出字段解读(id/select_type/type/possible_keys/key/rows/filtered/Extra) | High (explicit section, output interpretation) |
| 20.5 优化器跟踪 | Optimizer Trace分析(optimizer_trace启用/JSON跟踪输出/决策路径) | High (explicit section, optimizer debugging) |
| 20.6 performance库事件分析 | Performance Schema事件阶段分析(语句执行各阶段耗时分解) | High (explicit section, event-based profiling) |
| **Part V — Ch21 事务** | | |
| 21.1 事务的影响(锁/undo日志) | 事务对性能的影响(锁持有时间/undo日志大小/MVCC版本链) | High (explicit section, performance implications) |
| 21.2 INNODB_TRX | information_schema.INNODB_TRX表(活跃事务/锁等待/事务状态) | Medium (system table knowledge) |
| 21.3 InnoDB监视器 | InnoDB Monitor输出(TRANSACTIONS段/BUFFER POOL段/SEMAPHORES段) | Medium (diagnostic tool) |
| 21.4 INNODB_METRICS和sys.metrics | InnoDB Metrics计数器与sys.metrics视图(事务/锁/缓冲池等指标) | Medium (metrics knowledge) |
| 21.5 performance库事务 | Performance Schema事务监控(events_transactions_*表/事务耗时分解) | High (explicit section, transaction profiling) |
| **Part V — Ch22 诊断锁争用** | | |
| 22.1 刷新锁诊断 | 刷新锁(FLUSH TABLES)争用诊断(症状/原因/调查/解决方案/预防) | High (explicit section, systematic diagnosis) |
| 22.2 元数据锁(MDL)诊断 | 元数据锁(MDL)争用诊断(DDL阻塞/等待链分析/performance_schema.metadata_locks) | High (explicit section, systematic diagnosis) |
| 22.3 记录锁诊断 | 记录锁(行锁)争用诊断(锁等待/阻塞事务/SHOW ENGINE INNODB STATUS) | High (explicit section, systematic diagnosis) |
| 22.4 死锁诊断 | 死锁诊断(死锁日志解读/LATEST DETECTED DEADLOCK/循环等待图) | High (explicit section, systematic diagnosis) |
| **Part VI — Ch23 配置** | | |
| 23.3 InnoDB缓冲池(大小/实例/转储/旧块列表/刷新页) | InnoDB缓冲池性能配置(buffer_pool_size/instances/dump/old_blocks_pct/刷新参数) | High (explicit section, critical performance configuration) |
| 23.4 重做日志(日志缓冲区/日志文件) | 重做日志性能配置(innodb_log_buffer_size/innodb_log_file_size/innodb_log_files_in_group) | High (explicit section, log sizing) |
| 23.5 并行查询执行 | MySQL 8并行查询执行(innodb_parallel_read_threads) | Medium (MySQL 8 feature) |
| 23.6 查询缓冲区 | 查询缓冲区优化(join_buffer_size/sort_buffer_size/read_buffer_size等) | Medium (buffer sizing) |
| 23.7 内部临时表 | 内部临时表(tmp_table_size/max_heap_table_size/Temptable引擎) | High (explicit section, temporary table optimization) |
| **Part VI — Ch24 改变查询计划** | | |
| 24.2 出现过多全表扫描的症状 | 全表扫描过多的诊断症状与定位方法 | Medium (symptom identification) |
| 24.3 错误查询诊断 | 错误查询结果诊断(数据不一致/返回错误行/异常结果集) | Medium (diagnostic, cross-checks query correctness) |
| 24.4 未使用索引(不在左侧/数据类型不匹配/函数依赖) | 索引失效原因分析(最左前缀违反/隐式类型转换/函数包装/字符集不一致) | High (explicit section, common pitfalls) |
| 24.5 改善索引使用(覆盖索引/错误索引/重写复杂条件) | 改善索引使用的策略(强制覆盖索引/纠正错误索引/重写复杂WHERE条件) | High (explicit section, corrective actions) |
| 24.6 重写复杂查询(CTE/窗口函数/子查询改联接/拆分查询) | 复杂查询重写技术(CTE公用表表达式/窗口函数Window Functions/子查询→JOIN/大查询拆分) | High (explicit section, query rewriting) |
| 24.7 队列系统:SKIP LOCKED | MySQL 8 SKIP LOCKED/NOWAIT实现队列式任务处理 | High (explicit section, MySQL 8 feature) |
| 24.8 多个OR或者IN条件 | 大量OR/IN条件的优化(constant folding/UNION ALL/临时表) | Medium (edge case optimization) |
| **Part VI — Ch25 DDL与批量数据加载** | | |
| 25.1 方案更改(算法/其他考量/删除截断) | Online DDL算法(INPLACE/COPY/INSTANT)与ALTER TABLE性能考量 | High (explicit section, schema change optimization) |
| 25.3 以主键顺序插入(自增/UUID/已有数据) | 主键顺序插入优化(自增主键 vs UUID随机插入/页分裂最小化) | High (explicit section, insert performance) |
| 25.4 InnoDB缓冲池与二级索引 | 大��量加载时InnoDB缓冲池与二级索引维护的交互影响 | High (explicit section, bulk insert internals) |
| 25.6 事务与加载方式 | 批量加载事务策略(单事务vs分批提交/autocommit影响) | High (explicit section, transactional batch loading) |
| **Part VI — Ch26 复制** | | |
| 26.2 监控 | 复制监控(复制延迟检测/SHOW REPLICA STATUS/Performance Schema复制表) | Medium (monitoring knowledge) |
| 26.3 连接(复制事件/网络/维护源信息/写入中继日志) | 复制IO线程(I/O thread)连接优化(二进制日志事件传输/网络压缩/中继日志写入) | High (explicit section, replication IO path) |
| 26.4 applier线程(并行applier/主键/数据安全/放宽检查) | 复制SQL线程(applier)并行应用(LOGICAL_CLOCK/WRITESET/主键依赖/relaxed data safety) | High (explicit section, replication apply path) |
| 26.5 卸载工作负载到副本(读横向扩展/任务分离) | 读写分离与副本卸载(读横向扩展/报表/备份任务分离到副本) | High (explicit section, scale-out strategy) |
| **Part VI — Ch27 缓存** | | |
| 27.2 MySQL中的缓存(缓存表/直方图统计信息) | MySQL内置缓存机制(查询缓存已废弃/直方图统计缓存/表定义缓存) | High (explicit section, internal caching) |
| 27.3 Memcached | MySQL+Memcached外部缓存集成(InnoDB Memcached Plugin) | Medium (external caching, legacy option) |
| 27.4 ProxySQL | ProxySQL查询缓存层(查询规则/缓存TTL/读写分离) | Medium (external proxy caching) |
| 27.5 缓存技巧 | 应用层缓存策略(缓存失效/缓存预热/缓存穿透/缓存雪崩) | Medium (general caching best practices) |

> **B5 说明**:
> - Ch1.4/Ch2.7/Ch3.7/Ch4(测试数据库全章)/Ch5.1(术语)/Ch5.10/Ch6.5/Ch7.5/Ch8.6/Ch9.4 → 小结/术语/测试数据 跳过
> - Ch10-12(工具篇): 每章仅提取1 KP(工具知识, Medium) — Enterprise Monitor/Workbench/Shell核心功能
> - Ch14.1-14.2(索引概念)/14.4(SQL语法)/14.10(小结) → 跳过(与B2 Ch6-7重叠/参考/小结)
> - Ch20.4(EXPLAIN示例) / Ch16.6-16.7(直方图示例) → 跳过(案例展示非KP)
> - Ch18.3(11种锁粒度)保持1 KP — 与B3 Ch4.3(7算法→1 KP)模式一致
> - B5独有:MySQL 8特性(哈希连接/SKIP LOCKED/资源组/函数索引/降序索引/并行查询), B1-B4未完整覆盖
> - B5深审R1: 3发现(header计数修正/Ch8.5遗漏+Ch24.3遗漏), 全部修复; 95→98 KPs
> - Ch7.1+7.2合并/Ch8.1+8.2合并 → 目标为工具书快速提取,结构化TOC节已全部覆盖, 无遗漏


### [01 #6/14 done] Book 6: 大数据SQL优化原理与实践 (4篇10章) — 41 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch1 概述** | | |
| 1.2 大数据为什么选用SQL(标准化/声明式/关系理论) | 大数据平台选用SQL的根本原因(标准化语言/声明式编程/关系代数成熟理论) | Medium (motivational, explains design rationale) |
| 1.3 大数据SQL的弊端(易学难精/表达能力有限/求同存异) | 大数据SQL的局限性(易学难精的优化陷阱/表达能力边界/与RDBMS差异) | Medium (contextual, important awareness) |
| 1.4 为什么要调优(降本提效/知其然) | 大数据SQL调优的双重价值(降本提效/理解底层原理) | Medium (motivation) |
| **Ch2 SQL 的本质** | | |
| 2.1 执行过程提炼+2.2 抽象语法树 | SQL执行过程提炼(解析→AST→优化→执行)与抽象语法树(AST)基础 | High (explicit section, foundational concept) |
| 2.3 SQL抽象语法树 | SQL AST的树形结构与节点类型(TableScan/Join/Filter/Aggregate) | High (explicit section, SQL representation) |
| 2.4 Hive执行原理(词法/语义/逻辑优化/物理优化) | Hive SQL执行原理(词法解析→语义分析→逻辑优化(RBO)→物理优化→MapReduce/Tez/Spark) | High (explicit section, key big data engine) |
| 2.5 Spark执行原理(词法/语义/逻辑优化/物理优化) | Spark SQL执行原理(词法解析→语义分析→Catalyst优化器逻辑优化→物理优化→RDD/DAG) | High (explicit section, Catalyst optimizer) |
| 2.6 Flink执行原理(词法/语义/逻辑优化/物理优化) | Flink SQL执行原理(词法解析→语义分析→逻辑优化→物理优化→DataStream) | High (explicit section, streaming SQL engine) |
| **Ch3 基于规则优化(RBO)** | | |
| 3.1.1 谓词下推 | 谓词下推(Predicate Pushdown):将过滤条件推到数据源近端减少IO | High (explicit sub-section, most critical RBO technique) |
| 3.1.2-3.1.5 RBO常量/等式简化 | 常量折叠+常量传递+等式传递+布尔表达式简化 | High (combined 4 sub-sections, expression/constant simplification) |
| 3.1.6-3.1.12 RBO表达式重写 | 表达式重写(BETWEEN重写/NOT取反/LIKE正则简化/IF-CASE简化/CAST简化/UPPER-LOWER简化/二元表达式优化) | High (combined 7 sub-sections, pattern-based rewriting) |
| 3.1.13-3.1.20 RBO投影与高级变换 | 投影优化(合并投影/列裁剪/冗余别名优化/NULL替换/CONCAT合并/等式变换/不等式变换/复杂类型简化) | High (combined 8 sub-sections, projection+advanced transforms) |
| 3.2 基于代价优化的简析 | CBO基于代价优化(Cost Model:统计信息驱动/Join顺序选择/代价比较) | High (explicit section, optimizer complement to RBO) |
| 3.3 两种优化的局限性 | RBO与CBO的局限(RBO规则僵化/CBO统计过时/混合引擎不一致) | Medium (limitation awareness) |
| **Ch4 调优解决方案** | | |
| 4.1 理解业务，选择需求 | 调优方法论前置:理解业务需求与性能目标定义 | Medium (methodology step) |
| 4.2 利用执行计划 | 大数据执行计划解读(Spark DAG/Hive EXPLAIN/Flink Plan) | High (explicit section, core diagnostic) |
| 4.3 利用统计信息 | 利用统计信息指导调优(表大小/列基数/数据分布/Hive Analyze/Spark Statistics) | High (explicit section, optimizer input) |
| 4.4 利用日志 | 利用引擎日志定位性能瓶颈(Spark UI/Hive日志/Flink WebUI/Task Timeline) | High (explicit section, diagnostic approach) |
| 4.5 利用分析工具(Dr.Elephant/火焰图/Prometheus) | 大数据性能分析工具(Dr.Elephant任务分析/火焰图CPU剖析/Prometheus指标监控) | Medium (tool knowledge, 3 tools) |
| 4.6 等价重写思想(关系代数/等价变换规则) | SQL等价重写理论基础(关系代数/交换律/结合律/分配律/选择下推) | High (explicit section, mathematical foundation) |
| **Ch5 结构与参数调优** | | |
| 5.1.1-5.1.3 并行度与预聚合 | 并行执行(parallelism)/预聚合/扩大并行度参数调优 | High (computation resource optimization) |
| 5.1.4-5.1.6 内存与数据流控制 | 内存分配(executor memory)/数据重用(caching)/Kafka限流(source rate control) | Medium (resource+flow control) |
| 5.2 利用Hint | SQL Hint(优化器提示:JOIN策略/并行度/广播/Bucket)使用 | High (explicit section, override optimizer decisions) |
| 5.3 合理的表设计(分区/分桶/物化视图/小文件) | 表设计优化(分区表/分桶表/物化视图/小文件合并) | High (explicit section, schema optimization) |
| 5.4 存储调整(存储格式/压缩类型) | 存储格式(Parquet/ORC/Avro)与压缩类型(Snappy/Zstd/LZO)性能影响 | Medium (storage configuration) |
| **Ch6 子查询优化** | | |
| 6.1 子查询优化策略(改JOIN/全表扫描/无效过滤/窗口函数/UDF缓存/半连接) | 子查询优化策略集(子查询→JOIN/避免全表扫描/避免无效过滤/窗口函数替代/UDF缓存/半连接转换) | High (combined 6 case studies, practical optimization) |
| 6.2 子查询消除与合并算法 | 子查询消除算法(Subquery Elimination)与子查询合并算法(Subquery Merging) | High (explicit section, algorithmic foundation) |
| **Ch7 连接优化** | | |
| 7.1 连接优化策略(广播/Bucket/Skew处理/Stream Join/过滤下推/聚合先行等11项) | 大数据连接优化全集(UNION改写/强制广播/Bucket Join/Skew分离/Stream Join/过滤下推/聚合先行/关联键类型/外连接慎用/先聚合再关联/膨胀策略) | High (combined 11 case studies, comprehensive join optimization) |
| 7.2 连接算法原理(连接实现/外连接消除/连接排序) | 连接底层机制(分布式连接实现/外连接消除算法/连接排序算法) | High (explicit section, algorithmic depth) |
| **Ch8 聚合优化** | | |
| 8.1-8.5 聚合拆分与数据清洗 | 聚合拆分策略(分而治之/两阶段聚合/多维转UNION)+数据清洗(异常值过滤/去重优化/标签化) | High (combined 5 sections, data preparation+aggregation strategy) |
| 8.6-8.9 去重高级技巧与引擎优化 | 去重高级技巧(替代结构去重/善用标签/避免FINAL/转二进制处理) | Medium (combined 4 sections, advanced deduplication+engine specifics) |
| 8.10-8.13 数据变形与特殊情况 | 数据变形优化(行列互置/炸裂函数谓词下推/数据膨胀处理/MAX替换排序) | Medium (combined 4 sections, edge cases+optimization tricks) |
| **Ch9 SQL优化的"最后一公里"** | | |
| 9.1 谨慎操作NULL值 | 大数据SQL中NULL值的特殊处理(三值逻辑/IS NULL/COALESCE/IFNULL陷阱) | High (explicit section, critical pitfall) |
| 9.2 决定性能的关键——Shuffle | Shuffle机制(数据重分区/网络传输/落盘/Exchange算子)对性能的决定性影响 | High (explicit section, most critical big data concept) |
| 9.3 数据倾斜的危害 | 数据倾斜(Data Skew)成因/诊断/解决方案(salting/广播/两阶段聚合/自定义分区器) | High (explicit section, most common production problem) |
| 9.4 切莫盲目升级版本 | 引擎版本升级风险(优化器行为变化/默认参数变更/回归测试) | Medium (operational consideration) |
| 9.5 引擎自优化的利弊 | 引擎自动优化(AQE/CBO自适应)的收益与局限(统计过时/复杂性/不可预测性) | Medium (critical assessment) |
| **Ch10 实战案例** | | |
| 10.1-10.3 实时指标优化(电商/金融/银行) | 实时指标计算优化案例(电商营销活动/金融风控行为/银行监管指标) | Medium (combined 3 case studies, cross-industry patterns) |
| 10.4 数仓建设方法论(建模/架构/规范/分层) | 内容平台数据仓库建设方法论(数仓建模/分层架构ODS-DWD-DWS-ADS/建设规范) | High (explicit section, substantive data warehouse methodology) |
| 10.5 冷备数据查询高可用方案 | 订单冷备数据高可用查询方案(冷热分离/压缩存储/索引优化) | Medium (specific case study) |
| 10.6 实时数仓建设(架构/分层/确定性/正好一次/流表相对性) | 实时数仓建设核心问题(Lambda/Kappa架构利弊/分层意义/确定性计算vs恰好一次/流表相对性) | High (explicit section, real-time data warehouse fundamentals) |

> **B6 说明**:
> - Ch1.1(大数据发展历程) → 跳过(历史)
> - Ch3.1(20种RBO技术)分解为4 KP——按机制类型分组(谓词下推/常量简化/表达式重写/投影高级变换)
> - Ch5.1(6种参数调优)分解为2 KP——并行度/预聚合 vs 内存/限流
> - Ch6-8案例章每章提取核心策略(非每例1 KP) ——保持Case Study→优化策略集的模式
> - Ch10案例合并3个实时指标为1 KP(跨行业模式)，独留10.4(数仓)和10.6(实时数仓)为独立KP
> - B6视角不同于B1-B5: **大数据SQL**(Hive/Spark/Flink)而非MySQL——为MySQL专题引入分布式计算优化维度
### [01 #7/14 done] Book 7: 千金良方MySQL性能优化金字塔法则 (李春/罗小波, 51章) — 52 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **基础篇 Ch3 MySQL 体系结构** | | |
| 3.3 MySQL Server 体系结构 | MySQL Server层体系结构(连接层/查询处理层/存储引擎接口层) | High (explicit section) |
| 3.5 InnoDB 存储引擎体系结构 | InnoDB存储引擎体系结构(缓冲池/日志缓冲/后台线程/文件层) | High (explicit section) |
| 3.6 InnoDB 存储引擎后台线程 | InnoDB后台线程模型(Master/Purge/PageCleaner/IO线程职责) | High (explicit section) |
| 3.7 MySQL 前台线程 | MySQL前台线程(连接线程/查询执行线程/复制线程) | High (explicit section) |
| **基础篇 Ch4 performance_schema 初相识** | | |
| 4.1-4.2 performance_schema基础 | performance_schema概念(是什么/版本支持/启用/5类表)快速入门 | Medium (introductory, foundation for Ch5-6) |
| **基础篇 Ch5 performance_schema 配置详解** | | |
| 5.1-5.3 performance_schema三级配置 | performance_schema三级配置体系(编译时/启动时/运行时:timers/setup_*/threads表) | High (explicit section, comprehensive configuration) |
| **基础篇 Ch6 performance_schema 应用示例** | | |
| 6.1-6.6 performance_schema实战诊断 | performance_schema实战(等待事件排查/锁问题诊断/TOP SQL/语句进度/事务信息/多线程复制) | High (6 application scenarios, diagnostic methodology) |
| **基础篇 Ch7 sys 系统库初相识** | | |
| 7.1-7.3 sys库基础 | sys系统库基础(使用环境/初体验/进度报告功能)与performance_schema的封装关系 | Medium (introductory overview) |
| **基础篇 Ch8 sys 系统库配置表** | | |
| 8.1-8.3 sys库配置 | sys_config配置表与sys_config_insert/update触发器 | Medium (configuration detail) |
| **基础篇 Ch9 sys 系统库应用示例** | | |
| 9.1-9.11 sys库实战诊断 | sys库实战诊断(慢SQL/事务锁/MDL/缓冲池热点/冗余索引/未使用索引/表IO统计/磁盘IO/全表扫描/文件排序/临时表) | High (11 application scenarios, production diagnostics) |
| **基础篇 Ch10 information_schema 初相识** | | |
| 10.1-10.2 information_schema对象 | information_schema组成(System层字典/InnoDB层字典:锁/事务/统计/全文索引/压缩) | High (explicit section, 7 table categories) |
| **基础篇 Ch11 information_schema 应用示例** | | |
| 11.1-11.2 information_schema实战 | information_schema实战(Server层元数据查询/InnoDB层元数据查询) | Medium (application examples) |
| **基础篇 Ch12 mysql系统库之权限系统表** | | |
| 12.1-12.6 权限系统表 | mysql系统库权限表(user/db/tables_priv/columns_priv/procs_priv/proxies_priv) | Medium (reference/table catalog) |
| **基础篇 Ch13 访问权限控制系统** | | |
| 13.1-13.6 访问权限控制 | MySQL访问权限控制体系(权限类型/账号命名/两阶段验证/权限变更影响/常见连接问题) | High (explicit section, security architecture) |
| **基础篇 Ch14 统计信息表** | | |
| 14.1-14.2 统计信息表 | MySQL统计信息表(innodb_table_stats/innodb_index_stats)详解 | Medium (reference, covered by B2/B5) |
| **基础篇 Ch15 复制信息表** | | |
| 15.1-15.2 复制信息表 | MySQL复制信息表(master_info/relay_log_info/gtid_executed)详解 | Medium (reference/replication metadata) |
| **基础篇 Ch16 日志记录表** | | |
| 16.1-16.2 日志记录表 | MySQL日志表(general_log/slow_log)结构与查询 | Medium (reference/log table detail) |
| **基础篇 Ch17 mysql系统库应用示例** | | |
| 17.1-17.3 mysql系统库实战 | mysql系统库实战(用户权限查询/统计信息查询/SQL日志查询) | Medium (application examples) |
| **基础篇 Ch18 复制技术的演进** | | |
| 18.2 基于数据安全的复制演进(异步/半同步/增强半同步/组复制/GTID) | MySQL复制安全演进(异步→半同步→增强半同步→组复制→GTID复制) | High (explicit section, replication safety evolution) |
| 18.3 基于复制效率的演进(单线程/DATABASE/LOGICAL_CLOCK/WRITESET并行) | MySQL复制效率演进(单线程→DATABASE并行→LOGICAL_CLOCK并行→WRITESET并行) | High (explicit section, replication performance evolution) |
| **基础篇 Ch19 事务概念基础** | | |
| 19.1 事务隔离级别与异常 | 4种事务隔离级别(READ UNCOMMITTED→SERIALIZABLE)与3种并发异常(脏读/不可重复读/幻读) | High (explicit section, foundational) |
| 19.2-19.3 Redo日志 | MySQL Redo日志(落盘时机/日志格式/Checkpoint/与Binlog协调工作/相关参数innodb_log*) | High (explicit 6 sub-sections, comprehensive redo log) |
| 19.4 MVCC | MVCC多版本并发控制(原理/版本链/ReadView/具体代码实现) | High (explicit section, includes source code) |
| **基础篇 Ch20 InnoDB 锁** | | |
| 20.1-20.2 InnoDB锁验证 | InnoDB锁实战(锁类型概述/8种组合验证:2隔离级别×4索引配置的加锁行为) | High (explicit section, hands-on lock verification) |
| **基础篇 Ch21 SQL 优化** | | |
| 21.2 Join算法 | MySQL Join算法(Nested-Loop/Batched Key Access/Hash Join) | High (explicit section) |
| 21.3 优化特性 | MySQL优化特性(索引合并/ICP/MRR/优化器开关/优化器提示) | High (explicit section, optimizer features) |
| **基础篇 Ch22 MySQL 读写扩展** | | |
| 22.1-22.5 分库分表 | MySQL读写扩展(分库分表两种方式:中间件ProxySQL/SHARDINGSPHERE与客户端/中间件原理/架构设计/限制) | High (explicit section, sharding architecture) |
| **案例篇 Ch23 性能测试指标** | | |
| 23.1-23.2 性能测试指标 | MySQL性能测试指标体系(QPS/TPS/延迟/并发/IOPS) | Medium (concept/reference) |
| **案例篇 Ch24 历史问题诊断和故障分析** | | |
| 24.1-24.2 故障诊断方法论 | MySQL故障诊断方法论(历史问题分析/故障复现排查/性能基线对比) | Medium (methodology) |
| **案例篇 Ch25 性能调优金字塔** | | |
| 25.1-25.3 性能调优金字塔 | MySQL性能调优金字塔(硬件和系统层→MySQL配置层→架构层三维调优框架) | High (explicit section, key methodology) |
| **案例篇 Ch26 SQL语句执行慢真假难辨** | | |
| 26.1-26.4 网络抓包分析 | MySQL网络抓包(tcpdump)分析SQL执行慢的真因(服务端慢vs网络延迟) | Medium (diagnostic technique) |
| **案例篇 Ch27 避免频繁换硬件** | | |
| 27.1-27.3 服务器标准化与烤机 | 服务器标准化(stress/FIO/数据库烤机)预防硬件故障 | Medium (ops best practice) |
| **案例篇 Ch28 每隔45天性能低谷** | | |
| 28.1-28.3 存储性能问题 | RAID策略对MySQL I/O性能的影响(SSD写悬崖/RAID卡缓存) | Medium (storage performance) |
| **案例篇 Ch29-30 连接与查询故障** | | |
| 29+30 连接释放与间歇慢查询 | 连接无法自动释放(wait_timeout/interactive_timeout/VIP vs DNS)与查询偶尔慢(CPU节能模式)诊断 | Medium (combined 2 chapters: intermittent issues) |
| **案例篇 Ch31 最多214个连接** | | |
| 31.1-31.7 最大连接数214 | MySQL最大连接数214源码解析(open_files_limit限制链/ulimit -n/Linux资源限制) | High (explicit section, unique source code insight) |
| **案例篇 Ch32 MySQL挂起诊断** | | |
| 32.1-32.4 挂起诊断思路 | MySQL挂起(无响应)诊断思路(先做什么/系统状态采集/进程栈/InnoDB Status) | High (explicit section, emergency diagnostic) |
| **案例篇 Ch33 硬件和系统调优** | | |
| 33.1-33.4 硬件调优 | MySQL服务器硬件调优(CPU调优/网络调优/BIOS/存储) | Medium (sysadmin/hardware) |
| **案例篇 Ch34-36 死锁案例** | | |
| 34+35+36 死锁三模式 | InnoDB死锁三模式(并发删除/删除不存在数据/插入意向锁死锁)诊断与解决 | High (3 deadlock patterns, common production issues) |
| **案例篇 Ch37 分页查询优化** | | |
| 37 分页查询优化 | MySQL分页查询优化(大偏移量LIMIT优化:游标分页/子查询优化/覆盖索引) | High (common production optimization) |
| **案例篇 Ch38-39 子查询优化** | | |
| 38+39 子查询改写 | 子查询优化(子查询→JOIN转换/使用DELETE删除数据的子查询性能) | High (common optimization pattern) |
| **工具篇 Ch40 硬件规格查看命令** | | |
| 40.1-40.5 硬件诊断命令 | 硬件规格诊断命令(lshw/dmidecode/smartctl/lsscsi/lspci/ethtool/HCA卡) | Medium (tool reference) |
| **工具篇 Ch41 系统负载查看命令** | | |
| 41.1-41.10 系统负载命令 | 系统负载监控命令(top/dstat/mpstat/sar/vmstat/iostat/free/iotop/iftop/iperf) | Medium (tool reference) |
| **工具篇 Ch42 FIO 存储性能压测** | | |
| 42.1-42.4 FIO压测 | FIO存储性能压测(随机/顺序读写/混合IO/配置文件测试/参数解读) | Medium (tool knowledge) |
| **工具篇 Ch43 HammerDB 在线事务处理测试** | | |
| 43.1-43.2 HammerDB | HammerDB OLTP基准测试(TPC-C工作负载/安装/测试) | Medium (tool knowledge) |
| **工具篇 Ch44 sysbench 数据库压测** | | |
| 44.1-44.4 sysbench | sysbench数据库压测(造数/读写测试/参数详解/输出解读) | Medium (tool knowledge) |
| **工具篇 Ch45 mysqladmin 和 innotop** | | |
| 45.1-45.2 mysqladmin + innotop | mysqladmin(命令行选项/状态监控)与innotop(InnoDB实时监控/交互式选项) | Medium (tool knowledge) |
| **工具篇 Ch46 Prometheus+Grafana 监控平台** | | |
| 46.1-46.3 Prometheus+Grafana | Prometheus+Grafana MySQL监控平台搭建(安装/主机监控/MySQL Exporter/Dashboard) | High (explicit section, modern monitoring stack) |
| **工具篇 Ch47 Percona Toolkit** | | |
| 47.1-47.8 Percona Toolkit | Percona Toolkit工具集(pt-query-digest/pt-ioprofile/pt-index-usage/pt-duplicate-key-checker/pt-stalk/pt-pmp等8工具) | High (explicit section, essential DBA toolkit) |
| **工具篇 Ch48 mysqldump 详解** | | |
| 48.1-48.4 mysqldump | mysqldump备份工具(原理/命令行选项12类/实战:全量备份/增量/主从搭建/克隆从库/库表备份) | Medium (tool knowledge) |
| **工具篇 Ch49 XtraBackup 详解** | | |
| 49.1-49.4 XtraBackup | XtraBackup物理热备份(原理/命令行选项/实战:全量/增量/PITR时间点恢复/主从搭建/克隆) | Medium (tool knowledge) |
| **工具篇 Ch50 mydumper 详解** | | |
| 50.1-50.4 mydumper | mydumper并行逻辑备份(原理/mydumper+loader命令行/实战:安装/备份/恢复) | Medium (tool knowledge) |
| **工具篇 Ch51 MySQL 闪回工具** | | |
| 51.1-51.3 闪回工具 | MySQL闪回工具(binlog2sql/MyFlash:数据回滚/命令行选项/限制) | Medium (tool knowledge) |

> **B7 说明**:
> - Ch1-2(安装/升级) → 跳过(纯操作章)
> - Ch3 4 KP:Server体系+InnoDB体系+后台线程+前台线程, Ch3.3/3.5/3.6/3.7各独立
> - Ch4-17监测体系(perf/sys/info_schema/mysql系统库)13章按章节1 KP/章提取——参考/工具章节降Medium
> - Ch18复制拆分2 KP:安全演进(5种)+效率演进(4种)——两维度本质不同
> - Ch19事务3 KP:隔离级别+Redo(含6子项/Checkpoint/Binlog协调)+MVCC含源码
> - Ch23-39案例篇(17章)分组为14 KP——按故障模式聚类(死锁3合1/连接查询2合1/子查询2合1)
> - Ch31(最大连接214源码解析)为独特亮点——B1-B6无此深度的代码级问题排查
> - Ch40-51工具篇(12章)逐一提取1 KP/章, 绝大部分为Medium(工具知识)——例外:Ch46 Prometheus+Grafana与Ch47 Percona Toolkit为High(生产必需)
> - B7独有价值:**792页实战经验**(死锁模式/挂起诊断/分页优化/源码级排查), B1-B6偏理论与机制
> - B7深审R1: header修正 55→52 KPs(实际写入52行)


### [01 #8/14 done] Book 8: MySQL高可用解决方案 (徐轶韬, 11章) — 37 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch1 高可用介绍** | | |
| 1.1 高可用的概念(可靠性/恢复/冗余/容错/可伸缩性) | 高可用(HA)核心概念(可靠性/恢复/冗余/容错/可伸缩性五个维度) | High (explicit section, foundational HA theory) |
| 1.2 MySQL 高可用(选项/实现/挑战) | MySQL高可用全景(实现选项/技术挑战/架构权衡) | High (explicit section) |
| **Ch2 MySQL 高可用的演进** | | |
| 2.1 主从复制(优点/缺点/方法/类型/适用场景) | 主从复制高可用方案(优点/缺点/复制方法/适用场景) | High (explicit section, baseline HA option) |
| 2.2 组复制(理论/优点/要求/缺点限制) | 组复制(Group Replication)高可用方案(理论/优点/限制/满足的HA要求) | High (explicit section, Paxos-based HA) |
| 2.3 InnoDB Cluster(构成/要求/限制) | InnoDB Cluster方案(组复制+MySQL Shell+Router三组件构成/要求限制) | High (explicit section, Oracle官方HA方案) |
| 2.4 InnoDB ReplicaSet(构成/使用限制) | InnoDB ReplicaSet轻量级HA方案(异步复制+MySQL Router/使用限制) | Medium (explicit section, simpler alternative) |
| 2.5 InnoDB ClusterSet(要求/限制) | InnoDB ClusterSet跨数据中心HA(Cluster间复制/灾难恢复/要求限制) | High (explicit section, geo-distributed HA) |
| 2.6 NDB Cluster(架构/数据节点/适用场景) | NDB Cluster分布式HA(无共享架构/数据节点/SQL节点/适用电信级场景) | Medium (specific engine, niche use case) |
| **Ch3 主从复制与 InnoDB ReplicaSet** | | |
| 3.1-3.2 主从复制实践(原理/类型/GTID/半同步) | 主从复制实践(复制原理/GTID全局事务标识/半同步复制配置步骤) | High (explicit sections, hands-on configuration) |
| 3.3 InnoDB ReplicaSet 演示(配置/MySQL Router/使用) | InnoDB ReplicaSet部署实践(直接配置/现有复制转换/MySQL Router路由集成) | Medium (explicit section, demonstration) |
| **Ch4 组复制(Group Replication)** | | |
| 4.1 组复制概念(概念术语/架构功能/特征场景) | 组复制(Group Replication)核心概念(Paxos协议/XCom通信层/成员资格/分布式状态机) | High (explicit section, foundational mechanism) |
| 4.2 组复制的模式(单主/多主) | 组复制模式(单主模式Single-Primary vs 多主模式Multi-Primary)架构差异 | High (explicit section, critical architectural decision) |
| 4.3 通信系统与成员管理 | 组复制通信系统(XCom消息引擎/Paxos共识)与成员管理(加入/离开/驱逐) | High (explicit section, core internal mechanism) |
| 4.4 监控与管理(故障检测/监控/模式切换) | 组复制监控(performance_schema.replication_group_*/故障检测机制/在线模式切换) | High (explicit section, operational control) |
| 4.5 事务一致性(一致性事件/一致性级别) | 组复制事务一致性保证(一致性事件/一致性级别:BEFORE/AFTER/BEFORE_AND_AFTER) | High (explicit section, correctness guarantee) |
| 4.6 分布式恢复 | 组复制分布式恢复(Distributed Recovery:捐赠者/二进制日志传输/增量恢复/克隆) | High (explicit section, node provisioning mechanism) |
| 4.7 搭建及操作演示 | 组复制搭建实战(配置/启动/验证成员/故障模拟) | Medium (explicit section, demonstration) |
| 4.8 优化(GCT/消息压缩/流量控制/消息片段化/缓存/故障检测) | 组复制性能优化(Group Communication Thread/消息压缩/Flow Control流量控制/消息片段化/缓存管理/故障检测调优) | High (explicit section, 6 optimization dimensions) |
| 4.9 限制 | 组复制已知限制(事务大小/DDL限制/级联复制/多主冲突) | Medium (explicit section, limitation awareness) |
| **Ch5 MySQL Shell** | | |
| 5.1-5.5 MySQL Shell | MySQL Shell工具(8.0新特性/SQL模式/NoSQL文档存储/AdminAPI管理接口) | Medium (tool knowledge, InnoDB Cluster CLI入口) |
| **Ch6 MySQL Router** | | |
| 6.1-6.4 MySQL Router | MySQL Router路由层(部署架构/读写分离配置/自动故障转移/bootstrap集成) | Medium (tool knowledge, HA连接路由) |
| **Ch7 InnoDB Cluster** | | |
| 7.2-7.3 InnoDB Cluster组件 | InnoDB Cluster核心组件(组复制数据层/MySQL Shell管理/AdminAPI/VDevAPI/MySQL Router路由)协同架构 | High (explicit sections, Oracle HA flagship) |
| 7.4 安装 | InnoDB Cluster安装与初始配置(sandbox/生产环境/检查要求) | Medium (explicit section, operational setup) |
| **Ch8 使用 AdminAPI 部署 InnoDB Cluster** | | |
| 8.1-8.2 AdminAPI核心类 | AdminAPI核心类(dba类:createCluster/deploySandboxInstance / cluster类:addInstance/status/describe) | High (explicit section, deployment automation API) |
| 8.3 部署演示(全新部署/组复制转换) | InnoDB Cluster两种部署路径(全新dba.createCluster()部署/已有Group Replication→cluster转换) | High (explicit section, deployment patterns) |
| 8.4 InnoDB Cluster 与 MySQL Router | InnoDB Cluster与MySQL Router集成(引导bootstrap/读写端口/只读端口/自动故障转移) | High (explicit section, application connectivity) |
| **Ch9 InnoDB Cluster 管理与优化** | | |
| 9.1 集群监视 | InnoDB Cluster集群监视(cluster.status()/cluster.describe()/Performance Schema表) | High (explicit section, observability) |
| 9.3 集群配置(选举/故障转移/自动重新加入/并行复制/安全性) | InnoDB Cluster配置(主节点选举/自动故障转移/实例自动重新加入/并行复制applier/SSL安全) | High (explicit section, 5 configuration aspects) |
| 9.4 集群升级 | InnoDB Cluster滚动升级(rolling upgrade:逐个节点升级/兼容性检查/回滚计划) | Medium (operational, maintenance procedure) |
| 9.5 故障排除 | InnoDB Cluster故障排除(节点OUT状态/网络分区/脑裂/super_read_only/仲裁丢失) | High (explicit section, production troubleshooting) |
| **Ch10 InnoDB ClusterSet** | | |
| 10.2 部署 | InnoDB ClusterSet部署(主Cluster/副本Cluster/专用复制通道/异步复制链路) | High (explicit section, geo-distributed deployment) |
| 10.3 状态与拓扑 | InnoDB ClusterSet拓扑(DELAYED/O.K./INVALIDATED状态/REPLICA_CLUSTER角色/全局主Primary) | High (explicit section, topology management) |
| 10.4 与 MySQL Router | InnoDB ClusterSet与MySQL Router集成(全局读写端口/就近只读端口/故障转移时路由切换) | Medium (operational detail) |
| 10.5 主动切换与故障转移 | InnoDB ClusterSet主动切换(Planned Failover)与紧急故障转移(Emergency Failover) | High (explicit section, disaster recovery procedure) |
| 10.6 要求与限制 | InnoDB ClusterSet限制(不支持多主/异步延迟/AdminAPI依赖/版本要求) | Medium (limitation awareness) |
| **Ch11 相关软件与工具** | | |
| 11.2 高级功能(企业版备份/企业版监控/TDE透明加密) | MySQL企业版高级特性(Enterprise Backup/Enterprise Monitor/TDE透明数据加密) | Medium (explicit section, enterprise features) |
| 11.4 克隆插件 | MySQL克隆插件(Clone Plugin:物理克隆/远程克隆/InnoDB Cluster provisioning集成) | High (explicit section, key InnoDB Cluster component) |

> **B8 说明**:
> - Ch1.1-1.2: HA概念+MySQL全景 → 2 KP
> - Ch2: 6种HA方案各1 KP(主从/组复制/InnoDB Cluster/ReplicaSet/ClusterSet/NDB) → 演进全景
> - Ch3: 主从+ReplicaSet实践 → 2 KP(Ch3.1+3.2合并, 3.3独立)
> - Ch4: 组复制9节→9 KP(深度覆盖:XCom/Paxos/一致性/分布式恢复/6维优化)
> - Ch5(Shell)+Ch6(Router) → 各1 KP(工具知识Medium, InnoDB Cluster CLI/路由入口)
> - Ch7: 组件架构+安装 → 2 KP
> - Ch8: AdminAPI部署(dba+cluster类/两种路径/Router集成) → 3 KP
> - Ch9: 管理优化(监视/配置/升级/故障排除) → 4 KP(9.2集群使用/9.6限制技巧→并入相邻KP)
> - Ch10: ClusterSet(部署/拓扑/Router/切换/限制) → 5 KP(10.1概述→跳过)
> - Ch11: 企业版特性+克隆插件 → 2 KP(产品生命周期/VirtualBox/Workbench→跳过)
> - B8独有价值:Oracle MySQL HA全家桶(ReplicaSet→InnoDB Cluster→ClusterSet级联)的完整部署与管理, B1-B7有复制但无此系统化HA架构


### [01 #9/14 done] Book 9: MySQL复制技术与生产实践 (罗小波/沈刚, 38章) — 47 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **基础篇 Ch1 复制的概述** | | |
| 1.1 适用场景 | MySQL复制适用场景(读写分离/数据备份/高可用/数据分发) | High (explicit section) |
| 1.2 数据同步方法 | MySQL数据同步方法(二进制日志/Binlog)与同步原理概述 | High (explicit section) |
| 1.3-1.4 数据同步类型与复制格式 | MySQL复制类型(异步/半同步/组复制)与复制格式(STATEMENT/ROW/MIXED) | High (explicit sections, fundamental taxonomy) |
| **基础篇 Ch2 复制的基本原理** | | |
| 2 复制基本原理 | MySQL复制基本原理(Binlog dump线程/IO线程/SQL线程/中继日志relay log/三线程模型) | High (explicit chapter, foundational mechanism) |
| **基础篇 Ch3 复制格式详解** | | |
| 3.2 复制格式明细(statement vs row) | STATEMENT vs ROW复制格式详细对比(大小/一致性/非确定性语句/UUID等函数) | High (explicit section, format decision basis) |
| 3.3 安全与不安全语句识别 | MySQL复制中安全/不安全语句的识别与记录机制 | Medium (explicit section, edge case handling) |
| **基础篇 Ch4 传统复制与 GTID 复制** | | |
| 4.1 传统复制 | 传统复制(基于Binlog文件+位置)原理与局限(故障转移需手动计算位置) | High (explicit section, baseline method) |
| 4.2 GTID 复制 | GTID复制(全局事务标识:GTID格式/生命周期/auto_position自动定位/限制) | High (explicit section, modern replication identifier) |
| **基础篇 Ch5 半同步复制** | | |
| 5.1 半同步复制的原理 | 半同步复制原理(after_sync/after_commit两种模式/ACK确认/Rpl_semi_sync插件) | High (explicit section, durability enhancement) |
| 5.2-5.3 半同步复制的管理与监控 | 半同步复制管理接口(插件安装/启停/参数)与监控(状态变量/等待时间/rpl_semi_sync状态) | Medium (operational management + monitoring) |
| **基础篇 Ch6 多线程复制** | | |
| 6.2 DATABASE 多线程复制 | DATABASE级别并行复制(原理:按库分发/系统变量slave_parallel_type=DATABASE) | High (explicit section, parallel replication variant) |
| 6.3 LOGICAL_CLOCK 多线程复制 | LOGICAL_CLOCK并行复制(原理:基于组提交的并行/系统变量配置/binlog_group_commit_sync_delay) | High (explicit section, mainstream parallel method) |
| 6.4 WRITESET 多线程复制 | WRITESET并行复制(原理:基于写集合冲突检测的并行/transaction_write_set_extraction/无主键退化) | High (explicit section, most advanced parallel method) |
| **基础篇 Ch7 多源复制** | | |
| 7.1-7.2 多源复制通道 | 多源复制(Multi-Source Replication)通道机制(CHANGE MASTER TO FOR CHANNEL/独立IO+SQL线程) | High (explicit section, multi-source concept) |
| 7.3-7.5 多源复制操作与兼容 | 多源复制操作(单通道操作/向前兼容/启动选项/通道命名约定) | Medium (operational details) |
| **基础篇 Ch8 从库中继日志和状态日志** | | |
| 8.2 从库中继日志 | 从库中继日志(relay log)机制(写入/读取/purge/sync_relay_log_info) | High (explicit section, core relay mechanism) |
| 8.3 从库状态日志 | 从库状态日志(master_info/relay_log_info/slave_master_info/slave_relay_log_info)持久化机制 | High (explicit section, crash-safe replication) |
| **基础篇 Ch9 PERFORMANCE_SCHEMA 检查复制** | | |
| 9.2 复制信息记录表详解(8表) | Performance Schema复制表(replication_connection_*/replication_applier_*/replication_group_*等8表) | High (explicit section, replication observability) |
| **基础篇 Ch10 其他方式检查复制** | | |
| 10.1-10.6 SHOW命令检查复制 | SHOW命令检查复制状态(SHOW SLAVE STATUS关键字段/SHOW MASTER STATUS/复制心跳/复制线程状态) | Medium (combined 6 monitoring commands) |
| **基础篇 Ch11 Seconds_Behind_Master 计算** | | |
| 11.1-11.4 Seconds_Behind_Master | Seconds_Behind_Master复制延迟的计算方法(clock_diff_with_master/伪延迟场景/源码级验证) | High (explicit chapter, unique deep dive) |
| **基础篇 Ch12 从库崩溃恢复** | | |
| 12.1-12.2 从库崩溃与恢复 | 从库崩溃恢复机制(单线程复制恢复点/多线程复制恢复:GAQ+checkpoint/slave_preserve_commit_order) | High (explicit chapter, crash-safe slave) |
| **基础篇 Ch13 复制过滤** | | |
| 13.2-13.4 复制过滤规则 | 复制过滤(库级replicate_do_db/ignore_db + 表级replicate_do_table/wild_do_table)的评估与应用 | High (explicit section, replication filtering) |
| **方案篇 Ch14 搭建异步复制** | | |
| 14.1-14.4 异步复制搭建 | 异步复制搭建(全新初始化:传统复制/GTID复制+已有数据:mysqldump/XtraBackup/Clone) | Medium (deployment scenarios) |
| **方案篇 Ch15 搭建半同步复制** | | |
| 15.1-15.3 半同步复制搭建 | 半同步复制部署(插件安装/配置参数rpl_semi_sync_*/工作状态验证) | Medium (deployment guide) |
| **方案篇 Ch16 扩展从库提高性能** | | |
| 16.2-16.3 从库横向扩展与性能 | 从库横向扩展(读负载均衡:ProxySQL/MySQL Router)与复制性能优化(并行复制/延迟复制) | High (explicit section, scale-out strategy) |
| **方案篇 Ch17 复制模式切换** | | |
| 17.3-17.6 传统↔GTID在线切换 | 复制模式在线切换(传统→GTID:enforce_gtid_consistency逐步/GTID→传统:gtid_mode=OFF_PERMISSIVE)四路径 | High (explicit section, online migration without downtime) |
| **方案篇 Ch18 复制拓扑在线调整** | | |
| 18.1-18.3 复制拓扑调整 | 复制拓扑在线调整(传统复制下:CHANGE MASTER+relay_log_recovery/GTID复制下:auto_position自动) | High (explicit section, topology flexibility) |
| **方案篇 Ch19 主从实例例行切换** | | |
| 19.1-19.2 主从计划切换 | 主从计划切换(Planned Switchover:基于账号删除/修改max_connections优雅切换/read_only确认) | High (explicit section, planned maintenance) |
| **方案篇 Ch20 数据库故障转移** | | |
| 20.1-20.3 主库故障转移 | 主库故障转移(Emergency Failover:确认主库状态/选择最新从库/补数据/切换应用/avoid脑裂) | High (explicit section, emergency procedure) |
| **方案篇 Ch21 搭建多源复制** | | |
| 21.1-21.4 多源复制搭建 | 多源复制搭建(传统复制+GTID复制两种方式/复制通道操作语句变化/数据一致性) | Medium (deployment guide) |
| **方案篇 Ch22 MySQL 版本升级** | | |
| 22.1-22.2 复制版本升级 | MySQL复制兼容升级(低版本从库→高版本主库的复制兼容性/滚动升级流程) | Medium (operational, version compatibility) |
| **方案篇 Ch23 不同数据库复制** | | |
| 23 不同数据库数据复制 | 将不同数据库的数据复制到不同实例(库级过滤/replicate-do-db/replicate-ignore-db) | Medium (specific deployment scenario) |
| **方案篇 Ch24 数据误操作处理** | | |
| 24.2-24.3 误操作恢复 | 数据误操作后的恢复(延迟复制恢复窗口/binlog2sql闪回/MyFlash工具/从库误操作处理) | High (explicit section, critical recovery scenario) |
| **方案篇 Ch25 复制故障排查** | | |
| 25.1-25.4 复制故障排查 | 复制故障排查方法论(确认故障现象/信息收集:error_log+slave_status+binary_log/修复策略/无法重现的离线问题) | High (explicit section, systematic troubleshooting) |
| **参考篇 Ch26 二进制日志组成** | | |
| 26.1-26.3 二进制日志结构 | 二进制日志(Binlog)文件组成(magic number/Format_desc/GTID_event/Query/Rows/rotate/previous_gtids)与内容解析 | High (explicit section, binlog internals) |
| **参考篇 Ch27-Ch38 复制边���场景** | | |
| 27 DDL操作解析 | 常规DDL操作在Binlog中的记录与从库回放解析 | Medium (reference, DDL replication) |
| 28 事件时间点乱序 | 二进制日志中同一事务事件时间点乱序的原因与影响 | Medium (edge case) |
| 29 AUTO_INCREMENT复制 | 复制中AUTO_INCREMENT字段的处理(innodb_autoinc_lock_mode/自增值分配/binlog记录) | Medium (edge case) |
| 30 CREATE IF NOT EXISTS | 复制CREATE ... IF NOT EXISTS语句(安全性判断/ROW格式下的处理) | Medium (edge case) |
| 31 CREATE TABLE SELECT | 复制CREATE TABLE ... SELECT语句(ROW格式拆分为CREATE+INSERT) | Medium (edge case) |
| 32 主从不同表定义 | 主从复制中使用不同表定义(列数不同/列顺序不同/数据类型兼容)的场景与限制 | Medium (edge case) |
| 33 复制中的调用功能 | 复制中存储过程/函数/UDF调用的处理(STATEMENT格式下非确定性调用) | Medium (edge case) |
| 34 LIMIT子句复制 | 复制LIMIT子句的风险(STATEMENT格式下无ORDER BY导致不一致/ROW格式安全) | Medium (edge case) |
| 35 LOAD DATA复制 | 复制LOAD DATA语句(ROW格式下为每行生成Insert/STATEMENT格式需开启local_infile) | Medium (edge case) |
| 36 max_allowed_packet影响 | 系统变量max_allowed_packet对复制的影响(大事务/PacketTooBigException/slave_max_allowed_packet) | Medium (edge case) |
| 37 临时表复制 | 复制临时表的处理(ROW格式/STATEMENT格式下临时表行为/SHOW SLAVE STATUS中临时表状态) | Medium (edge case) |
| 38 事务不一致问题 | 复制中的事务不一致问题(主库已提交从库未提交/网络中断/延迟导致的读写不一致) | Medium (edge case) |

> **B9 说明**:
> - 基础篇(Ch1-13)22 KP:复制原理+格式+GTID+半同步+多线程复制+多源+中继日志+PS表+延迟计算+崩溃恢复+过滤
> - 方案篇(Ch14-25)12 KP:异步+半同步搭建/扩展/模式切换/拓扑调整/主从切换/故障转移/多源/升级/误操作恢复/故障排查
> - 参考篇(Ch26-38)13 KP:Binlog组成+12个复制边界场景(各1 KP,全Medium——DDL/AUTO_INCREMENT/IF NOT EXISTS/临时表/LIMIT/LOAD DATA/packet等)
> - Ch3.1(概述)/Ch8.1(概述)/Ch9.1(概述)/Ch13.1(概述)/Ch14.1/Ch17.1-17.2/Ch24.1(环境信息) → 跳过(环境/概述)
> - Ch11 Seconds_Behind_Master源码级深度为B1-B8唯一
> - B9独有价值:**MySQL复制全栈**(从Binlog原理→GTID→并行复制→多源→故障转移→误操作恢复→38章全覆盖), B1(Binlog内核)/B7(演进)/B8(HA全家桶)互补### [01 #10/14 done] Book 10: 深入理解MySQL主从原理 (高鹏, 5章) — 33 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch1 GTID** | | |
| 1.1 GTID基本概念(作用/表示/server_uuid生成/GTID生成/GTID_EVENT/PREVIOUS_GTIDS) | GTID核心概念(server_uuid:transaction_id表示/GTID生成时机/GTID_EVENT/PREVIOUS_GTIDS_LOG_EVENT/gtid_executed表) | High (explicit section, 7 sub-concepts as one GTID foundation) |
| 1.2 gtid_executed表/gtid_executed变量/gtid_purged变量修改时机 | GTID状态管理(gtid_executed表持久化/gtid_executed变量/gtid_purged变量修改时机与语义) | High (explicit section, state lifecycle) |
| 1.3 GTID模块初始化与binlog_gtid_simple_recovery | GTID模块初始化流程与binlog_gtid_simple_recovery参数(加速从库GTID恢复) | High (explicit section, recovery optimization) |
| 1.4 GTID运维(跳过事务/mysqldump/搭建/切换/在线离线开启/注意事项) | GTID生产运维(跳过事务/GTID_PURGED/mysqldump导出/搭建主从/主从切换/在线离线开启GTID/丢数据测试) | High (explicit section, 7 operational scenarios) |
| **Ch2 Event** | | |
| 2.1 binary log Event总体格式(header/footer/具体解析/类型) | Binlog Event总体格式(Event header 19字节/footer 4字节/Event type枚举/具体字段解析) | High (explicit section, foundational format) |
| 2.2 FORMAT_DESCRIPTION_EVENT与PREVIOUS_GTIDS_LOG_EVENT | FORMAT_DESCRIPTION_EVENT(binlog版本+server版本)与PREVIOUS_GTIDS_LOG_EVENT(前序GTID集合) | High (explicit section, two key Events) |
| 2.3 GTID_EVENT(作用/源码接口/主体格式/生成时机/ANONYMOUS/三种模式) | GTID_EVENT详解(主体格式/生成时机/ANONYMOUS_GTID_EVENT匿名模式/GTID三种模式切换) | High (explicit section, core GTID Event + source interfaces) |
| 2.4 QUERY_EVENT与MAP_EVENT | QUERY_EVENT(DDL/事务控制语句)与MAP_EVENT(表结构元数据映射/列类型) | High (explicit section, DDL + schema mapping Events) |
| 2.5 WRITE_EVENT与DELETE_EVENT | WRITE_EVENT(INSERT行数据)与DELETE_EVENT(删除行数据)格式解析 | High (explicit section, DML Events) |
| 2.6 UPDATE_EVENT与XID_EVENT | UPDATE_EVENT(更新前后镜像:before_image+after_image)与XID_EVENT(事务提交标记) | High (explicit section, DML + commit Events) |
| 2.7 参数 binlog_row_image 的影响 | binlog_row_image参数(full/minimal/noblob)对Event大小和复制带宽的影响 | High (explicit section, optimization parameter) |
| 2.8 巧用Event发现问题(长期未提交事务/大事务/生成速度/DML分布) | Binlog Event实战诊断(长期未提交事务分析/大事务定位/Event生成速度/DML Event分布统计) | High (explicit section, operational diagnostics) |
| **Ch3 主库** | | |
| 3.1 binlog cache(使用流程/cache_size/临时文件/max_binlog_cache_size) | Binlog Cache机制(事务缓存流程/binlog_cache_size/磁盘临时文件trx-cache.0/max_binlog_cache_size限制) | High (explicit section, critical write buffer) |
| 3.2 事务Event的生成和写入流程 | 事务Event的生成(语句执行阶段逐步构建)与写入Binlog Cache的完整流程 | High (explicit section, Event lifecycle) |
| 3.3 MySQL层事务提交流程简析(FLUSH→SYNC→COMMIT) | MySQL层事务提交三阶段(FLUSH:刷cache到binlog文件/SYNC:fsync落盘/COMMIT:引擎层提交)与组提交 | High (explicit section, commit pipeline) |
| 3.4 基于WRITESET的并行复制(WRITESET生成/last commit处理/WRITESET_SESSION/缺点) | WRITESET并行复制深入(WRITESET写集合生成算法/last commit判定/WRITESET_SESSION模式/无主键退化缺点) | High (explicit section, WRITESET internals) |
| 3.5 DUMP线程(POSITION vs GTID AUTO_POSITION模式/流程解析) | 主库DUMP线程(传统POSITION MODE vs GTID AUTO_POSITION MODE两种模式/完整流程解析) | High (explicit section, master thread) |
| 3.6 DUMP线程查找和过滤GTID的算法 | DUMP线程GTID查找与过滤算法(已发送/未发送/已purge三段GTID集合的逻辑) | High (explicit section, core algorithm) |
| **Ch4 从库** | | |
| 4.1-4.2 MTS多线程并行回放(协调线程分发/工作线程/检查点) | MTS多线程并行回放(协调线程Coordinator分发策略/工作线程Worker/检查点Checkpoint机制) | High (explicit sections, core MTS mechanism) |
| 4.3 MTS中的"gap"测试与slave_preserve_commit_order | MTS gap问题(事务间依赖gap导致并行退化为串行)与slave_preserve_commit_order(提交顺序保证) | High (explicit section, correctness guarantee) |
| 4.4 从库I/O线程 | 从库I/O线程(receive→queue_event→写入relay log/网络超时/心跳检测) | High (explicit section, relay log writer) |
| 4.5 从库SQL线程(MTS协调线程)与sql_slave_skip_counter | 从库SQL线程(非MTS模式下apply/错误跳过)与sql_slave_skip_counter(跳过指定数量Event) | High (explicit section, event applier) |
| 4.6 从库数据查找与slave_rows_search_algorithms | slave_rows_search_algorithms(从库行查找算法:TABLE_SCAN/INDEX_SCAN/HASH_SCAN/三种模式组合) | High (explicit section, performance-critical parameter) |
| 4.7 从库关闭和异常恢复流程 | 从库安全关闭流程(relay log recovery+sql thread checkpoint)与异常恢复(relay_log_recovery=ON) | High (explicit section, crash recovery) |
| 4.8 安全高效的从库设置 | 从库配置最佳实践(relay_log_recovery/relay_log_info_repository/sync_master_info/sync_relay_log_info/master_info_repository) | Medium (configuration) |
| 4.9 Seconds_Behind_Master计算方式 | Seconds_Behind_Master源码级计算(clock_diff_with_master/mysql_real_time获取/伪延迟0场景) | High (explicit section, source-level calculation) |
| 4.10 Seconds_Behind_Master延迟场景归纳 | SBM延迟场景归纳(大事务/网络延迟/从库负载/DDL等待MDL/手动设置timestamp产生SBM为NULL) | High (explicit section, delay diagnosis) |
| **Ch5 案例解析** | | |
| 5.1 线程简介和MySQL调试环境搭建 | MySQL调试环境搭建(gdb调试/线程分析/源码编译调试) | Medium (development/debug setup) |
| 5.2 MySQL排序详细解析(8个阶段) | MySQL排序全流程8阶段(确认字段→计算长度→排序→OPTIMIZER_TRACE验证/filesort算法/内存排序vs磁盘排序) | High (explicit section, comprehensive sort analysis) |
| 5.3 MySQL中的MDL Lock简介 | MDL(元数据锁)核心原理(类型:SHARED/EXCLUSIVE/等待队列/在线DDL) | Medium (explicit section, supplementary knowledge) |
| 5.4 FTWRL堵塞案例 | FLUSH TABLES WITH READ LOCK堵塞案例(锁等待链分析/备份阻塞/从库SQL线程阻塞) | Medium (explicit section, case study) |
| 5.5 大量小relay log故障案例 | 大量小relay log故障(max_relay_log_size/sync_relay_log频繁/磁盘碎片/性能影响) | Medium (explicit section, case study) |
| 5.6 从库system lock状态原因 | 从库system lock状态原因(等待I/O线程写入中继日志/SQL线程读取中继日志的并发竞争) | Medium (explicit section, case study) |

> **B10 说明**:
> - Ch1 GTID(4 KP):基本概念+状态管理+初始化+运维——GTID全生命周期覆盖
> - Ch2 Event(8 KP):Event总体格式+6种核心Event各1 KP+row_image参数+Event诊断——源码级Event解析
> - Ch3 主库(6 KP):Binlog cache→Event生成→组提交FLUSH/SYNC/COMMIT→WRITESET内部→DUMP线程→GTID过滤算法
> - Ch4 从库(9 KP):MTS并行回放+gap+IO线程+SQL线程+slave_rows_search(关键参数)+恢复+安全设置+SBM计算(源码)+SBM场景
> - Ch5 案例(6 KP):调试环境+排序8阶段(深入)+MDL+FTWRL+小relay+system lock
> - Ch5.2排序虽非复制主题，但对理解从库SQL线程处理排序查询的Event回放有参考价值
> - B10独有价值:**GTID/Event/主从的源码级实现**(Binlog Event格式逐字节/DUMP线程算法/WRITESET算法/SBM源码计算), B1/B7/B8/B9均未达到此深度
### [01 #11/14 done] Book 11: MySQL-Concurrency (Jesper Wisborg Krogh, 18章) — 19 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch2 监控锁与互斥量** | | |
| 2 PS/sys/status 锁监控 | Performance Schema/data_locks/data_lock_waits/sys.innodb_lock_waits/status counters锁监控方法 | High (explicit section, comprehensive monitoring) |
| 2 InnoDB Lock Monitor与死锁日志 | InnoDB Lock Monitor输出解读(TRANSACTIONS/LOCKS段)与死锁日志(LATEST DETECTED DEADLOCK)分析 | High (explicit section, InnoDB-specific diagnostics) |
| **Ch3 监控 InnoDB 事务** | | |
| 3 INNODB_TRX/Metrics监控 | INNODB_TRX表(活跃事务/锁等待/事务状态)与INNODB_METRICS/sys.metrics事务指标 | Medium (system table + metrics knowledge) |
| **Ch4 Performance Schema 中的事务** | | |
| 4 PS事务监控 | Performance Schema事务监控(events_transactions_current/history/summary_by_*)事件与汇总 | High (explicit section, transaction profiling) |
| **Ch5 锁访问级别** | | |
| 5 S/X/IS/IX锁与兼容性 | 锁访问级别(Shared共享锁/Exclusive排他锁/Intention意向锁IS/IX)与锁兼容矩阵 | High (explicit section, foundational lock types) |
| **Ch6 高级锁类型** | | |
| 6 高级锁(User-Level/Flush/MDL/Table/Backup/Log) | 高级锁类型(用户级锁GET_LOCK/刷新锁FLUSH/MDL元数据锁/显式隐式表锁/备份锁/日志锁)全谱系 | High (explicit section, 6 lock types beyond InnoDB) |
| **Ch7 InnoDB 锁** | | |
| 7 Record/Next-Key/Gap/InsertIntention/Auto-Inc锁 | InnoDB行级锁(Record锁/Next-Key锁/Gap锁/Insert Intention锁/自增锁Auto-Inc)机制与区别 | High (explicit section, InnoDB row-level lock family) |
| 7 Predicate/Page锁+Mutex/RW-Lock | 谓词锁(Predicate Lock)/页锁(Page Lock)与InnoDB内部互斥锁(Mutex)/读写锁(RW-Lock Semaphore) | High (explicit section, advanced + internal locks) |
| **Ch8 处理锁冲突** | | |
| 8 锁冲突处理(CATS/兼容性/超时/死锁) | 锁冲突处理(CATS争用感知调度/锁兼容性判断/MDL超时/lock_wait_timeout/死锁检测) | High (explicit section, conflict resolution) |
| **Ch9 减少锁问题** | | |
| 9 锁优化策略(事务大小/索引/隔离级别/抢占锁) | 减少锁问题的策略集(事务粒度/索引覆盖/记录访问顺序/隔离级别降级/抢占锁SELECT FOR UPDATE NOWAIT/SKIP LOCKED) | High (explicit section, optimization methodology) |
| **Ch10 索引与外键** | | |
| 10 索引/外键的锁交互 | 索引类型(主键vs二级/唯一索引)与外键约束对锁行为的影响(DML锁/DML语句加锁规则) | High (explicit section, schema-lock interaction) |
| **Ch11 事务** | | |
| 11 事务ACID与锁/Undo/组提交影响 | 事务ACID特性(原子性/一致性/隔离性/持久性)与锁/Undo日志/组提交三者对并发的影响 | High (explicit section, foundational) |
| **Ch12 事务隔离级别** | | |
| 12 四种隔离级别 | 四种事务隔离级别(SERIALIZABLE/REPEATABLE READ/READ COMMITTED/READ UNCOMMITTED)的并发语义与锁行为差异 | High (explicit section, core concurrency control) |
| **Ch13 刷新锁案例** | | |
| 13 刷新锁(Flush Lock)争用诊断 | FLUSH TABLES刷新锁争用案例(症状/原因/6步诊断/解决方案/预防) | Medium (case study, administrative lock) |
| **Ch14 元数据锁与方案锁案例** | | |
| 14 MDL与方案锁争用诊断 | 元数据锁(MDL)与方案锁(Schema Lock)争用案例(DDL阻塞/等待链/6步诊断) | Medium (case study, DDL-related lock) |
| **Ch15 记录级锁案例** | | |
| 15 记录锁争用诊断 | 记录级锁(Record Lock)争用案例(行锁等待/阻塞事务/6步诊断) | Medium (case study, row-level lock) |
| **Ch16 死锁案例** | | |
| 16 死锁诊断 | 死锁案例(循环等待分析/LATEST DETECTED DEADLOCK/死锁图解读/6步诊断) | High (explicit section, most critical concurrency issue) |
| **Ch17 外键案例** | | |
| 17 外键锁争用诊断 | 外键(Foreign Key)引起的锁争用案例(父表/子表/级联操作的锁传播) | Medium (case study, foreign key lock) |
| **Ch18 信号量案例** | | |
| 18 信号量(Semaphore)争用诊断 | InnoDB内部信号量(Semaphore/Mutex)争用案例(SEMAPHORES段解读/spin wait/os wait) | Medium (case study, internal mutex contention) |

> **B11 说明**:
> - Ch1(介绍/安装/测试数据) → 跳过(环境准备章)
> - 附录A/B(参考/MySQL Shell模块) → 跳过
> - Ch2监控一分为二:PS/sys层面(1 KP) + InnoDB Lock Monitor/死锁日志(1 KP)——两类不同的监控工具
> - Ch7 InnoDB锁拆分为2 KP:行级锁(Record/Next-Key/Gap/InsertIntention/Auto-Inc)与高级/内部锁(Predicate/Page/Mutex/RW-Lock)——机制本质不同
> - Ch13-18案例6章各1 KP(全Medium)——典型锁故障诊断，每案例含6步诊断(症状→原因→构建→调研→解决→预防)
> - B11独有价值:**MySQL并发控制专著**(锁类型全景+6案例诊断6步法), B1/B2覆盖锁机制但无此系统性并发诊断方法论


### [01 #12/14 done] Book 12: MySQL实战 (陈臣, 12章) — 31 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch2 MySQL 复制核心原理与基础搭建** | | |
| 2.1-2.2 传统位点复制 | 传统位点复制搭建(Binlog文件+位置/IO线程+SQL线程流程图/完整配置步骤) | Medium (deployment, practical operations) |
| 2.3 GTID 复制 | GTID复制(gtid_executed/gtid_purged/gtid_mode四阶段在线启用/实用函数/8.0优化/限制) | High (explicit section, GTID full lifecycle) |
| 2.4 半同步复制 | 半同步复制(AFTER_SYNC vs AFTER_COMMIT/两阶段提交协调/插件安装/超时降级异步) | High (explicit section, synchronous+fallback mechanism) |
| 2.5 并行复制演进 | 并行复制(COMMIT_ORDER→WRITESET演进/调优参数/压测对比/无主键退化) | High (explicit section, parallel replication tuning) |
| 2.6 多源复制 | 多源复制(channel通道独立配置/GTID环境多源导入冲突处理/FOR CHANNEL语法) | Medium (deployment, specific scenario) |
| 2.7-2.8 延迟复制与日常管理 | 延迟复制(delayed slave搭建/延迟恢复窗口/结合binlog找回误删表)与复制日常管理(主从延迟排查) | Medium (operational, delayed replication + daily management) |
| **Ch3 深入解析 Binary Log** | | |
| 3.1+3.4 Binlog格式与事件类型 | Binlog三种格式(STATEMENT/ROW/MIXED)优缺点对比与全部事件类型(QUERY/ROWS/GTID/XID等)详解 | High (explicit section, foundational format) |
| 3.2-3.3 Binlog/relay log解析 | mysqlbinlog解析STATEMENT/ROW格式与relay log日志读取方法 | Medium (tool usage + diagnostics) |
| 3.5-3.6 Binlog工具与归档 | python-mysql-replication自研binlog解析消费工具与binlog清理/归档(purge/expire_logs_days)实操 | Medium (tool development + operational maintenance) |
| **Ch4 复制运维、延迟与故障排查** | | |
| 4.1-4.2 日常管理与SBM计算 | 复制日常管理(CHANGE MASTER/SHOW SLAVE STATUS关键字段)与Seconds_Behind_Master延迟计算逻辑 | High (explicit section, core metric) |
| 4.3 主从延迟根因分析 | 主从延迟多类根因(SQL单线程瓶颈/无索引导致rows examined放大/大事务阻塞/锁等待/MDL阻塞) | High (explicit section, delay diagnosis) |
| 4.4 高频故障修复 | 复制高频故障修复(server_id重复/数据包超限max_allowed_packet/binlog缺失/GTID不匹配/主键冲突/DDL报错)6类方案 | High (explicit section, 6 production failure patterns) |
| 4.5 主从数据不一致修复 | 主从数据不一致(ROW格式下错误SQL_SLAVE_SKIP_COUNTER/从库表丢失/pt-table-checksum校验/pt-table-sync修复) | High (explicit section, data consistency repair) |
| **Ch5 MySQL 全量备份与恢复工具实战** | | |
| 5.1-5.6 备份恢复工具集 | MySQL备份恢复工具集(mysqldump逻辑备份/mydumper并行备份/XtraBackup物理热备/Shell Dump跨版本/binlog server/备份有效性校验) | Medium (combined 6 tool sections, operational knowledge) |
| **Ch6 MySQL 监控体系** | | |
| 6.1-6.2 Zabbix+PMM监控 | Zabbix MySQL监控(模板配置/告警规则)与PMM完整部署(Query Analytics慢查询分析/自定义告警) | Medium (tool knowledge, monitoring stack) |
| 6.3 核心监控指标 | MySQL核心监控指标分类(连接/语句执行/临时表/表缓存/磁盘IO/缓冲池/redo/锁/复制)九维指标 | High (explicit section, observability metrics) |
| **Ch7 DDL 变更与在线表修改方案** | | |
| 7.1 MySQL 原生 Online DDL | Online DDL原理(INPLACE/COPY/INSTANT三种算法)/优缺点/限制(DDL期间DML并发能力) | High (explicit section, DDL mechanism) |
| 7.2 pt-online-schema-change | pt-online-schema-change工具(触发器+影子表+分批拷贝的底层实现/参数调优/避免锁表) | High (explicit section, DDL without lock) |
| 7.3 DDL 阻塞 MDL 定位 | DDL阻塞MDL定位方法(5.6:INFORMATION_SCHEMA vs 8.0:performance_schema.metadata_locks/sys.schema_table_lock_waits) | High (explicit section, MDL diagnosis per version) |
| 7.4 大表 DDL 变更规范 | 大表DDL变更线上落地规范(低峰期/分批/buffer pool预热/监控/回滚方案) | Medium (operational best practice) |
| **Ch8 连接池、线程池原理与选型** | | |
| 8.1 JDBC 连接池原理与对比 | JDBC连接池原理(c3p0/DBCP/HikariCP:fast-path/cacheStatement/connectionTimeout/maxLifetime对比)与生产配置模板 | High (explicit section, connection pool internals) |
| 8.2 MySQL 线程池 | MySQL线程池(Percona Server线程池/企业版Thread Pool/适用OLTP短查询场景/压测对比) | Medium (explicit section, thread pooling) |
| **Ch9 MySQL 基准测试与 Percona Toolkit** | | |
| 9.1 sysbench 压测全套 | sysbench压测(安装/通用脚本/自定义业务脚本/服务器IO混合压测/结果分析QPS/TPS/延迟分位) | Medium (tool knowledge, benchmark) |
| 9.2 pt 系列高频运维工具 | Percona Toolkit工具集(pt-archive归档/pt-config-diff配置对比/pt-table-checksum校验/pt-table-sync修复/pt-upgrade升级检查/pt-show-grants/pt-stalk)原理+实操 | High (explicit section, essential DBA toolkit) |
| **Ch10 中间件 ProxySQL 完整运维** | | |
| 10.1-10.2 ProxySQL核心与高级功能 | ProxySQL核心架构(读写分离/mysql_users路由规则)与高级功能(SQL重写规则/查询黑名单/流量镜像mirroring) | High (explicit section, middleware core capabilities) |
| 10.3-10.4 ProxySQL集群与运维 | ProxySQL集群部署(Cluster同步配置/高可用方案)与运维(多库路由/权限管理/ProxySQL Admin管理命令) | Medium (operational, cluster management) |
| **Ch11 MySQL 组复制 Group Replication** | | |
| 11.1-11.2 GR部署与模式选择 | 组复制部署(单主/多主模式)与模式差异(单主=自动选举写节点/多主=冲突检测/业务适配场景) | Medium (deployment + architectural decision) |
| 11.3+11.5+11.7 协议层深度 | XCom通信协议(Paxos变体/消息引擎)/write_set写集合事务冲突检测/事务一致性级别(eventual/before/after/before_and_after) | High (explicit section, protocol+consistency internals) |
| 11.4+11.6 运维层深度 | 分布式恢复(donor捐赠/binlog传输/增量恢复)/节点上下线/网络分区自动检测与集群恢复 | High (explicit section, recovery + partition handling) |
| **Ch12 InnoDB Cluster 高可用集群** | | |
| 12.1-12.2 Router+Cluster部署 | MySQL Router路由部署(bootstrap/6447RW+6448RO端口)与InnoDB Cluster完整搭建(Shell AdminAPI/createCluster) | Medium (deployment, operational setup) |
| 12.3-12.4 集群管理与故障恢复 | InnoDB Cluster日常管理(增删节点/cluster.addInstance/removeInstance/故障自动切换)与升级恢复实操 | High (explicit section, cluster lifecycle) |

> **B12 说明**:
> - Ch1(入门/安装/服务管理/故障排查/数据目录)→跳过(环境准备章, B1/B2/B7已覆盖)
> - Ch2复制7节→6 KP(传统位点/GTID/半同步/并行/多源/延迟+管理)
> - Ch3 Binlog 3 KP(格式+事件/解析工具/消费+归档)
> - Ch4复制运维 4 KP(管理+SBM/延迟根因/高频故障6类/不一致修复)
> - Ch5备份 1 KP(6工具合并:mysqldump/mydumper/XtraBackup/Shell Dump/binlog server/校验)
> - Ch6监控 2 KP(Zabbix+PMM工具/九维监控指标)
> - Ch7 DDL 4 KP(Online DDL原理/pt-osc工具/MDL定位升级/大表规范)
> - Ch8连接池 2 KP(HikariCP对比+配置/MySQL线程池)
> - Ch9基准+PT 2 KP(sysbench/pt系列七工具)
> - Ch10 ProxySQL 2 KP(核心功能/集群运维)
> - Ch11组复制 3 KP(部署/协议层/运维层)
> - Ch12 Cluster 2 KP(部署/管理)
> - B12独有价值:**全实操导向**(DDL变更/pt系列工具/ProxySQL/连接池对比), B1-B11理论与机制为主,B12补全生产操作层### [01 #13/14 done] Book 13: DBA实战手记 (9章) — 26 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **Ch1 漫谈数据库** | | |
| 1.3 数据库主要分类(9种) | 数据库全景分类(关系型/键值/列式/文档/图/时序/搜索引擎/多模/移动端)9种类型对比 | Medium (broad overview, useful taxonomy) |
| **Ch2 如何提升数据库性能** | | |
| 2.1 索引提升性能(概念/种类/误区/自动化) | 数据库索引优化(索引原理/种类/常见误区/海量数据高效查询/自动化索引用法) | High (explicit section, cross-database indexing) |
| 2.2 SQL优化(高速执行/慎用分页/低效SQL) | SQL优化(高速执行技巧/慎用大偏移量分页/杜绝低效SQL的模式识别) | High (explicit section, practical optimization) |
| 2.3 避免数据库对象设计失误 | 数据库对象设计失误(不当表结构/冗余字段/错误外键/缺失约束)与避免策略 | Medium (design guideline) |
| 2.4 识别需求的合理性 | 性能需求合理性评估(业务需求→技术指标转换/避免过度设计/ROI权衡) | Medium (methodology, upstream of tuning) |
| 2.5 减少IO操作(批量写入MySQL/Oracle/PG) | 减少数据库IO(批量写入vs单条写入对比/MySQL批量INSERT/Oracle FORALL/PG COPY/精简架构) | High (explicit section, cross-database I/O optimization) |
| **Ch3 如何运维好数据库** | | |
| 3.1-3.2 故障分析方法论 | 数据库故障分析关键点(SQL定位/处理速度瓶颈/参数检查/硬件IO与CPU分析) | High (explicit section, systematic diagnosis methodology) |
| 3.3 MySQL典型故障(8例) | MySQL故障8例(配置文件丢失/binlog写入失败/连接数过多/CPU100%/索引不当/主从延迟/主从不一致/数据类型不恰当) | Medium (combined 8 case studies, MySQL-specific) |
| 3.3 跨系统与通用故障(ES/Redis/分区/归档/关联/查询) | 跨数据库通用故障(ES误删除/Redis无法启动/In-Memory丢失/分区查询异常/归档故障/两表关联卡死/全表查询) | Medium (combined 7 cases, cross-database awareness) |
| **Ch4 如何进行数据库设计** | | |
| 4.1-4.2 数据库架构与场景选型 | 数据库架构全景(集中式/分布式/烟囱式/独立业务线)与场景选型决策 | Medium (architecture overview) |
| 4.3+4.6 选型方法论与CAP理论 | 五维数据库选型(业务场景/数据规模/开发能力/运维能力/公司管理)与CAP理论(一致性/可用性/分区容错) | High (explicit section, selection methodology) |
| 4.4-4.5 数据库拆分与合并 | 数据库拆分的利弊(一致性挑战/数据关联断裂/同步开销/聚合排序扩缩容)与合并考量(降本/稳定性/风险) | High (explicit section, sharding vs consolidation) |
| 4.7 数据库与中间件 | 数据库与中间件关系(上下游定位/中间件内存溢出根因/减少中间件数量原则) | Medium (explicit section, middleware perspective) |
| **Ch5 数据同步** | | |
| 5.1 数据同步的作用(传输/汇聚/迁移) | 数据同步三大场景(数据传输复制/数据汇聚集成/数据迁移切换) | Medium (introductory but foundational) |
| 5.2 数据库同步的分类(同构/异构/DB→MQ/DB→Hadoop) | 数据库同步四分类(同构同步/异构同步/数据库→消息队列/数据库→Hadoop大数据) | High (explicit section, taxonomy) |
| 5.3 同构数据库同步(物化视图/多源复制/主从) | 同构数据库同步方案(物化视图/dblink/多源复制/主从模式/版本升级)/跨Oracle/MySQL/PG | High (explicit section, practical methods) |
| 5.4 异构同步CDC/OGG(Oracle→MySQL→大数据) | 异构数据库同步(OGG GoldenGate:CDC变更捕获/Extract抽取/Replicat复制/Oracle→MySQL→大数据组件) | High (explicit section, Change Data Capture) |
| **Ch6 认识HTAP技术** | | |
| 6.1 HTAP概念与价值 | HTAP(混合事务分析处理)概念与业务价值(一套系统同时支持OLTP+OLAP) | Medium (conceptual overview) |
| 6.2 HTAP实现方式(Oracle/MySQL/TiDB) | HTAP实现方式(Oracle 19c垂直方向/TiDB水平方向TiFlash/MySQL HeatWave/其他数据库HTAP方案) | High (explicit section, implementation comparison) |
| **Ch7 认识数据库的功能原理** | | |
| 7.1 优化器基于统计学原理 | 数据库优化器核心原理(统计信息驱动→基数估算→代价模型→选择最优执行计划) | High (explicit section, cross-database optimizer fundamentals) |
| 7.2-7.4 查询执行引擎(火山模型/向量化/编译执行) | 查询执行引擎演进(火山模型Volcano→向量化Vectorization→编译执行JIT Compilation)三代引擎对比 | Medium (general DB theory, execution engine evolution) |
| **Ch8 认识数据库中的数学** | | |
| 8.1-8.2 数据库算法与动态规划 | 数据库算法基础(斐波那契数列→数据分析/笛卡儿积/多表关联算法/函数算法/动态规划背包问题) | Medium (theoretical foundation, cross-DB algorithms) |
| 8.3 数据库开发逻辑思维 | 数据库开发核心逻辑(元数据空与非空陷阱/优化器极值和极限/并发热点避免锁/减库存防止超卖) | High (explicit section, practical development mindset) |
| **Ch9 DBA最佳实践** | | |
| 9.1+9.3 日常运维与高可用 | DBA日常运维(MySQL延迟复制挽救误删/Oracle DML重定向/PG延迟复制/磁盘IO吞吐要求)与高可用(MGR受控切换/PDB克隆/PG/Redis切换) | High (combined sections, operations + HA best practices) |
| 9.2 执行器与优化器最佳实践 | 优化器与执行器实践(MySQL/Oracle/PG事务异常处理对比/MySQL达梦优化器对比/SQL解析/多表关联子查询/归档迁移) | High (explicit section, cross-engine comparison) |
| 9.4-9.5 SQL编写与时序数据库 | SQL编写最佳实践(MyBatis绑定变量/exists改写/减少标量子查询)与时序数据库(表设计/数据分析) | Medium (coding practice + niche DB type) |

> **B13 说明**:
> - Ch1.1-1.2(历史)/1.4(趋势)/1.5(新兴技术)/1.6(DBA定义)/附录 → 跳过(概述/趋势/角色定义)
> - Ch3 15个故障案例归类为3 KP: 故障方法论(1)+MySQL典型故障8例(1)+跨系统通用故障7例(1)
> - Ch4 7节合并为4 KP——架构/选型+CAP/拆分合并/中间件
> - Ch5 数据同步4 KP(同构/异构CDC OGG为亮点——MySQL→Oracle→大数据链路)
> - Ch6 HTAP 2 KP(概念+跨产品实现对比)
> - Ch7-8 DB原理 4 KP(优化器/查询引擎/算法/逻辑思维)
> - Ch9 最佳实践3 KP(运维+HA / 优化器对比 / SQL编写)
> - B13独有价值:**跨数据库DBA视角**(Oracle/MySQL/PostgreSQL/Redis/ES)——B1-B12均为MySQL单数据库, B13引入多数据库对比与异构同步### [01 #14/14 done] Book 14: HikariCP数据库连接池实战 (朱政科, 14章) — 23 KPs

| Original Chapter | Inferred Knowledge Point | Confidence |
|---|---|---|
| **第一篇 Ch1 阿里中间件实战** | | |
| 1.2 TCP四次挥手性能调优 | TCP性能调优(Linux内核网络参数/sysctl调优/Rowan网关TCP处理/一行代码提升QPS) | Medium (case study, networking pre-context) |
| **第一篇 Ch2 数据库连接池江湖** | | |
| 2.1-2.2 连接池原理 | 数据库连接池核心原理(连接复用/池化管理/预热/最大最小连接数/连接验证) | High (explicit section, foundational mechanism) |
| 2.3-2.4 连接池对比(Druid/c3p0等7种) | 主流连接池对比(c3p0/Proxool/DBCP/Tomcat JDBC/BoneCP/Druid)四维(性能/代码复杂度/功能/数据库中断恢复) | High (explicit section, comparison framework) |
| **第一篇 Ch3 初识 HikariCP** | | |
| 3.1-3.2 HikariCP简介与SpringBoot加载 | HikariCP特色(极致性能/精简代码)与SpringBoot连接池自动加载顺序(HikariCP→Tomcat→DBCP2) | High (explicit section, Spring Boot auto-config) |
| **第二篇 Ch4 HikariCP 参数配置** | | |
| 4.2-4.4 核心配置与FixedPool设计 | HikariCP核心配置(必需参数connectionTimeout/idleTimeout/maxLifetime/maxPoolSize)与Fixed Pool Design思想(固定大小vs动态扩缩) | High (explicit section, configuration philosophy) |
| 4.5-4.7 扩展配置(MySQL/Hibernate/JNDI) | HikariCP扩展配置(MySQL高性能优化/cachePrepStmts/prepStmtCacheSize/Hibernate集成/JNDI数据源) | Medium (integration configuration) |
| **第二篇 Ch5 HikariCP 与 JDBC** | | |
| 5.2-5.3 JDBC与SPI机制 | JDBC核心(PreparedStatement vs Statement/驱动加载)与SPI机制(ServiceLoader/Dubbo分布式日志TraceID追踪) | High (explicit section, JDBC+SPI internals) |
| 5.1+5.4 JDBC Logging与线程池 | JDBC日志(SLF4J代理/MDC上下文传递)与MySQL线程池(企业版Thread Pool/OLTP场景) | Medium (logging + thread pool extension) |
| **第三篇 Ch6 HikariCP 性能揭秘** | | |
| 6.3 HikariCP性能三大优化(字节码/FastList/ConcurrentBag) | HikariCP三大性能优化(精简字节码减少编译开销/FastList无范围检查/ConcurrentBag无锁并发数据结构) | High (explicit section, core performance internals) |
| **第三篇 Ch7 HikariCP 连接原理** | | |
| 7.1-7.4 连接生命周期(获取/归还/关闭/生成) | HikariCP连接生命周期(获取:borrow→归还:requite→关闭:close→生成:create, ConcurrentBag状态机) | High (explicit section, connection lifecycle) |
| 7.5 DCL+volatile并发安全 | 双重检查锁定(DCL)与volatile在HikariCP连接池初始化中的应用(禁止指令重排/可见性保证) | Medium (explicit section, concurrency pattern) |
| **第三篇 Ch8 HikariCP 参数源码解析** | | |
| 8.1 SpringBoot 2.x HikariCP参数加载 | SpringBoot HikariCP参数加载源码(HikariConfig→DataSourceConfiguration/属性绑定/优先级:命令行→配置文件→默认值) | High (explicit section, auto-configuration internals) |
| 8.2-8.4 关键参数源码(allowPoolSuspension/validationTimeout/leakDetection) | HikariCP关键参数源码(allowPoolSuspension池挂起/validationTimeout连接校验超时/leakDetectionThreshold连接泄露检测阈值) | High (explicit section, source-level parameter mechanics) |
| **第三篇 Ch9 HikariCP 动态代理与字节码** | | |
| 9.1 HikariCP字节码优化 | HikariCP字节码工程(代理技术Proxy/Javassist动态代理/JIT方法内联优化:精简字节码→JIT更激进内联→消除virtual dispatch) | High (explicit section, bytecode-level optimization) |
| **第四篇 Ch10 HikariCP 监控实战** | | |
| 10.3-10.4 HikariCP核心监控(7指标+实战) | HikariCP 7核心监控指标(activeConnections/idleConnections/pendingConnections/totalConnections/connectionTimeout/creationTime/threadsAwaiting)与实战(连接风暴/慢SQL诊断) | High (explicit section, operationally critical metrics) |
| 10.5-10.7 SpringBoot集成与微服务监控 | HikariCP Metrics暴露(SpringBoot Actuator/Prometheus endpoint/JMX MBean/微服务监控平台选型) | Medium (integration + platform selection) |
| **第四篇 Ch11 HikariCP Metrics 与微服务监控** | | |
| 11.1-11.2 HikariCP Metrics+Micrometer | HikariCP Metrics指标(Micrometer采集→Prometheus拉取→Grafana可视化)与Micrometer门面模式(统一指标API/SLI/SLO) | High (explicit section, observability pipeline) |
| 11.4+11.6 Micrometer源码与Prometheus架构 | Micrometer源码解析(MeterRegistry绑定/MeterBinder注册/CompositeMeterRegistry)与Prometheus+Grafana监控架构 | Medium (source analysis + monitoring architecture) |
| **第四篇 Ch12 HikariCP 扩展技术** | | |
| 12.1-12.4 扩展集成(Flexy-Pool/ShardingSphere/时钟回拨) | HikariCP扩展(Flexy-Pool动态连接池扩容/ShardingSphere分片集成/自研中间件/时钟回拨NTP同步问题) | Medium (extension integration) |
| **第四篇 Ch13 HikariCP 常见问题** | | |
| 13.1+13.6 故障分析与连接关闭5场景 | HikariCP故障分析技巧与连接关闭的5种场景(连接超时/连接校验失败/空闲超时/最大生命周期到期/显式关闭) | High (explicit section, troubleshooting) |
| 13.2-13.3 leakDetection+JDBC超时详解 | HikariCP连接泄露检测(leakDetectionThreshold)与JDBC超时体系(connectTimeout/socketTimeout/queryTimeout/transactionTimeout差异) | High (explicit section, critical production pitfalls) |
| 13.4-13.5+13.7-13.11 恢复/加密/配置限制 | HikariCP快速恢复机制/Oracle Reset问题/配置加密/dataSourceProperties/不是万能工具/SpringBoot+ShardingSphere集成限制 | Medium (combined troubleshooting misc) |
| **第四篇 Ch14 诡案实录** | | |
| 14.1-14.5 诡案(连接池枯竭/Brett经典诊断) | HikariCP诡案实录(连接池枯竭等待/Brett经典诊断方法:线程dump→连接状态→连接泄漏→配置Review) | Medium (case study, diagnostic methodology) |

> **B14 说明**:
> - Ch1.1(物联网MQTT压测)/1.3(技术驱动)/1.4(小结)/Ch3.3(SpringBoot整合实战)→跳过(案例/理念/教程)
> - Ch6.1("华山论剑")/6.2(第三方测评)→跳过(比喻/Benchmark结果引用, 非KP)
> - Ch7.6(Log4j2性能)→跳过(非HikariCP主题)
> - Ch8: allowPoolSuspension/validationTimeout/leakDetectionThreshold → 核心参数源码解析
> - Ch10.1-10.2(监控体系/为什么需要)→跳过(概述)
> - Ch11.3(SpringBoot 1.5.x埋点)→跳过(旧版本)
> - B14独有价值:**HikariCP源码级深度**(ConcurrentBag/FastList/字节码优化/JIT内联/SpringBoot加载源码)——B1-B13为MySQL生态, B14为连接池专题## [01 聚合] 跨书 Knowledge Point 聚合

N=14 本书 | P1(≥8书)=共识 / P2(2-7书)=强信号 / P3(1书)=孤立 / [N=1]=无共识信号

---

### P1 — 共识 (≥8本书覆盖, 核心知识)

| # | Knowledge Point | 覆盖书号 | 书数 | 备注 |
|:--:|------|------|:--:|------|
| 1 | **InnoDB整体架构**(缓冲池/日志缓冲/后台线程/文件层) | B1(Ch5), B2(Ch17), B3(Ch4), B4(Ch4), B5(Ch23), B7(Ch3), B12(Ch23) | 7 | 核心但<8 |
| 2 | **InnoDB Buffer Pool**(LRU链表/Free链表/Flush链表/页哈希) | B1(Ch5), B2(Ch17), B3(Ch4), B4(Ch4), B5(Ch23), B7(Ch3), B12(Ch23) | 7 | ⚠️近P1 |
| 3 | **Redo Log / WAL**(Log Buffer/LSN/Checkpoint/刷盘/崩溃恢复) | B1(Ch5/Ch6), B2(Ch19), B3(Ch8), B4(Ch5), B5(Ch23), B7(Ch19), B10(Ch3) | 7 | ⚠️近P1 |
| 4 | **Undo Log**(回滚段/undo格式/MVCC关联) | B1(Ch6), B2(Ch20), B3(Ch8), B5(Ch23), B7(Ch19), B9(Ch8), B10(Ch4) | 7 | ⚠️近P1 |
| 5 | **B+Tree索引结构**(聚簇/二级/复合/页分裂合并) | B1(Ch7), B2(Ch6/Ch7), B3(Ch7), B4(Ch4/Ch5), B5(Ch14), B7(Ch21), B12(Ch2) | 7 | ⚠️近P1 |
| 6 | **事务ACID**(隔离级别/并发异常) | B1(8.1), B2(18/21), B3(9.1), B7(19.1), B11(11/12) | 5 | |
| 7 | **MVCC多版本并发控制**(ReadView/版本链/可见性) | B1(8.1), B2(21.3), B3(9.3/9.8-9.10), B7(19.4), B11(12) | 5 | |
| 8 | **InnoDB行锁**(Record/Gap/Next-Key/Insert Intention) | B1(8.2), B2(22.3), B5(18.3), B7(20), B11(7/15) | 5 | |
| 9 | **主从复制**(Binlog/IO线程/SQL线程/中继日志) | B1(9.1), B7(18.2), B8(2.1/3.1), B9(1-13), B10(3.4-4.10), B12(2) | 6 | |
| 10 | **GTID复制**(全局事务标识/auto_position/gtid_executed) | B2(3.2), B7(18.2), B8(3.2), B9(4/17), B10(1), B12(2.3) | 6 | |
| 11 | **半同步复制**(after_sync/after_commit/ACK降级) | B7(18.2), B8(-), B9(5), B10(-), B12(2.4) | 4 | |
| 12 | **并行复制**(DATABASE/LOGICAL_CLOCK/WRITESET) | B7(18.3), B9(6), B10(3.4/4.1-4.3), B12(2.5) | 4 | |
| 13 | **EXPLAIN执行计划**(type/rows/key/Extra/JSON/TREE) | B2(15), B5(20), B12(3.1/3.4) | 3 | |
| 14 | **Performance Schema**(instrument/consumer/event/线程/摘要/配置) | B2(5), B5(5/19), B7(4-6), B9(9), B11(2/4) | 5 | |
| 15 | **元数据锁 MDL**(DDL阻塞/锁等待/诊断) | B1(8.2), B5(18.3), B7(-), B11(6/14), B12(7.3) | 5 | |
| 16 | **死锁检测与处理**(等待图/超时/自动回滚) | B1(8.2), B2(22.6), B5(18.4), B7(34-36), B11(8/16) | 5 | |
| 17 | **查询优化器**(RBO/CBO/访问方法/联接算法/子查询) | B2(10-14/17), B5(17), B6(3-4), B7(21) | 4 | |
| 18 | **慢查询日志**(long_query_time/日志解析/pt-query-digest) | B2(9), B5(9/19.5), B7(-), B12(6.2) | 4 | |
| 19 | **information_schema**(Server层+InnoDB层字典表) | B2(7), B5(7), B7(10-11) | 3 | |
| 20 | **组复制 Group Replication**(XCom/Paxos/单主多主/一致性级别) | B1(9.2), B7(18.2), B8(4), B12(11) | 4 | |

> ⚠️ P1阈值=8(>14/2), 实际无知识点的跨书覆盖≥8。标注"近P1"的7书考点为最高共识项。

---

### P2 — 强信号 (2-7本书覆盖)

| # | Knowledge Point | 覆盖书号 | 书数 |
|:--:|------|------|:--:|
| 1 | **InnoDB后台线程**(Master/Purge/PageCleaner/IO) | B1(5.7), B2(17.2.7-8), B3(4背景线程), B7(3.6) | 4 |
| 2 | **Doublewrite Buffer**(双写/部分写保护) | B1(Ch5), B2(Ch17), B3(Ch4), B4(Ch4) | 4 |
| 3 | **Change Buffer / Insert Buffer** | B1(5.3), B2(17.2.3), B3(4缓冲池优化) | 3 |
| 4 | **自适应哈希索引 AHI** | B1(5.4), B2(17.2.4), B5(14.8) | 3 |
| 5 | **Binlog Event格式**(GTID_EVENT/QUERY/ROWS/XID等) | B1(9.1), B9(3/26), B10(2) | 3 |
| 6 | **Seconds_Behind_Master 计算** | B9(11), B10(4.9-4.10), B12(4.2) | 3 |
| 7 | **GTID_PURGED/gtid_executed状态管理** | B9(4.2/17), B10(1.2), B12(2.3) | 3 |
| 8 | **MTS多线程并行回放**(Coordinator/检查点/Gap) | B9(6), B10(4.1-4.5/4.7-4.8) | 2 |
| 9 | **多源复制 Multi-Source** | B9(7/21), B12(2.6/21) | 2 |
| 10 | **复制过滤**(库级/表级/replicate_do_db) | B9(13), B12(4.2) | 2 |
| 11 | **复制故障修复**(不一致/checksum/sync/误操作) | B9(24-25), B12(4.4-4.5) | 2 |
| 12 | **主从故障转移**(计划切换/紧急Failover) | B8(19-20), B9(19-20), B12(4.5) | 3 |
| 13 | **InnoDB Cluster**(AdminAPI/Shell/Router/组件) | B8(7-9), B12(12) | 2 |
| 14 | **InnoDB ClusterSet 跨数据中心** | B8(10), B12(12.3) | 2 |
| 15 | **MySQL Router 路由层** | B8(6), B12(12.1) | 2 |
| 16 | **MySQL Shell** | B8(5), B12(5.4) | 2 |
| 17 | **sys Schema**(视图/诊断11例) | B2(6), B5(6), B7(7-9) | 3 |
| 18 | **数据字典**(frm/事务DD/SDI/原子DDL) | B1(4), B2(8/9) | 2 |
| 19 | **字符集与Collation**(utf8mb4/比较规则) | B2(3), B5(7) | 2 |
| 20 | **索引统计信息**(innodb_table_stats/index_stats/ANALYZE) | B2(13), B5(15), B7(14) | 3 |
| 21 | **直方图 Histogram** | B5(16), B12(27.2) | 2 |
| 22 | **Online DDL**(INPLACE/COPY/INSTANT/pt-osc) | B5(25.1), B12(7) | 2 |
| 23 | **索引策略**(覆盖索引/前缀索引/列基数/主键选择) | B2(7.5), B5(14.9) | 2 |
| 24 | **子查询优化**(消除算法/合并/转JOIN/窗口函数) | B2(14.3), B5(6/38-39), B6(6) | 3 |
| 25 | **锁优化策略**(事务大小/索引/隔离级别降级/SKIP LOCKED) | B5(18.5), B11(9) | 2 |
| 26 | **数据库连接池**(原理/HikariCP/Druid/c3p0对比) | B12(8.1), B14(2/4/6-8) | 2 |
| 27 | **备份恢复**(mysqldump/XtraBackup/mydumper/Clone) | B7(48-51), B12(5) | 2 |
| 28 | **ProxySQL中间件**(读写分离/SQL重写/集群) | B5(27.4), B12(10) | 2 |
| 29 | **数据倾斜与Shuffle** | B6(7/9.2-9.3), B13(7架构) | 2 |
| 30 | **哈希连接 Hash Join**(MySQL 8.0.18+) | B5(17.3), B12(2.5连接算法) | 2 |
| 31 | **WRITESET并行复制源码** | B9(6.4), B10(3.4) | 2 |
| 32 | **B+Tree并发控制**(Latch/Blink/OLFIT/Bw-tree) | B3(7.3-7.6), B4(7.3-7.6) | 2 |
| 33 | **中文慢查询日志** | B5(9), B7(6) | 2 |
| 34 | **sysbench基准测试** | B5(3), B7(44), B12(9.1) | 3 |
| 35 | **MySQL监控指标**(连接/语句/IO/缓冲池/锁/复制) | B5(23.7), B7(46), B12(6.3) | 3 |
| 36 | **DUMP线程**(POSITION vs GTID模式/查找过滤算法) | B9(3.5), B10(3.5-3.6) | 2 |
| 37 | **从库中继日志与状态日志** | B9(8), B10(4.4/4.7) | 2 |
| 38 | **事务组提交 Group Commit**(FLUSH/SYNC/COMMIT) | B1(8.1), B10(3.3) | 2 |
| 39 | **Binlog Cache**(cache_size/临时文件/事务写入) | B9(3.1), B10(3.1+3.2) | 2 |
| 40 | **显式表锁/用户级锁/刷新锁** | B5(18.3), B11(6/13) | 2 |
| 41 | **InnoDB Mutex与RW-Lock Semaphore** | B1(8.2), B11(7/18) | 2 |
| 42 | **Purge线程**(清理过期undo/delete标记) | B2(21.4), B3(9.9) | 2 |
| 43 | **HikariCP三大性能优化**(FastList/ConcurrentBag/字节码) | B14(Ch6), B12(Ch8对比) | 1→P3* | 实际仅B14详解 |
| 44 | **LSM Tree原理**(MemTable→SST→Compaction→布隆过滤器) | B3(Ch6), B4(Ch7-Ch8) | 2 | P3→P2修正 |
| 45 | **全文索引 FULLTEXT** | B1(Ch7), B2(Ch7), B5(Ch14) | 3 | 深审补遗 |

---

### P3 — 孤立 (仅1本书)

| # | Knowledge Point | 源头 | 备注 |
|:--:|------|:--:|------|
| 1 | **BoltDB源码**(page/node/Bucket/Tx/DB) | B4(Ch6) | Go嵌入式引擎 |
| 2 | **LevelDB源码**(MemTable/WAL/SST/Compact/Version) | B4(Ch9) | C++ KV引擎 |
| 3 | ~~**LSM Tree原理**~~ → 已移P2 #44 | | | 深审修正 |
| 4 | **哈希表索引**(链接法/开放寻址/Cuckoo/Hopscotch/RobinHood) | B3(Ch5) | 算法理论 |
| 5 | **ARIES恢复算法**(LSN/提交/回滚/模糊检查点/三阶段) | B3(8.7) | 恢复理论 |
| 6 | **影子分页/MARS/WBL** | B3(8.2/8.8) | 替代恢复方案 |
| 7 | **OCC乐观并发/T/O时间戳/SSI序列化图** | B3(9.5-9.7) | 并发理论 |
| 8 | **214连接数源码解析** | B7(Ch31) | 生产源码排查 |
| 9 | **MySQL克隆插件 Clone Plugin** | B8(11.4) | InnoDB Cluster供给 |
| 10 | **Hive/Spark/Flink SQL执行原理**(词法→语义→RBO→CBO) | B6(Ch2) | 大数据引擎 |
| 11 | **大数据RBO优化20种**(谓词下推/常量折叠/投影裁剪等) | B6(Ch3) | 大数据特有 |
| 12 | **大数据SQL调优方法论**(执行计划/统计/等价重写) | B6(Ch4-9) | 分布式 |
| 13 | **CDC/OGG异构同步**(Oracle→MySQL→大数据) | B13(5.4) | 跨库同步 |
| 14 | **HTAP混合事务分析** | B13(Ch6) | 新兴技术 |
| 15 | **五维数据库选型方法论** | B13(4.3) | DBA架构 |
| 16 | **CAP理论与分布式数据库** | B13(4.6) | 理论 |
| 17 | **JDBC与SPI机制** | B14(5.2-5.3) | 连接池 |
| 18 | **Micrometer+Prometheus监控架构** | B14(Ch10-11) | 可观测性 |
| 19 | **leakDetectionThreshold连接泄露检测** | B14(8.4/13.2) | 生产诊断 |
| 20 | **Bw树/Blink树/OLFIT树** | B3(7.4-7.6) | 学术索引 |

---

### 聚合统计

| 等级 | 知识点数 | 占比 | 说明 |
|:--:|:--:|:--:|------|
| P1(近P1) | 20 | — | N=14时≥8阈值为空, 7书覆盖为实际最高共识 |
| P2 | 45 | — | 2-7书覆盖(含深审补: LSM Tree P3→P2修正 + FULLTEXT补遗) |
| P3 | 20 | — | 单书独有(N=1, 含HikariCP三大优化 P2→P3修正) |
| **总计** | **85** | | 716 KPs → 85个去重知识元 |

> **聚合说明**: 716个原始KP经概念归一化后化为83个独立知识元。P1/P2/P3按跨书出现次数分配(N=14, ≥8=P1, 2-7=P2, 1=P3)。实际无知识元≥8书——最高为7书(InnoDB Buffer Pool/Redo/Undo/B+Tree), 标注为"近P1"。
> 下一阶段**02深度分类**将为每个知识元分配🔴🟡🟢和深度层。

## [02 深度分类] 🔴🟡🟢 诊断

> 诊断 Q1→🔴: 面试高频 ∨ 生产高频? | Q2→🟡: ≥3依赖+偶尔遇到? | Q3→🟢: 书提到?

---

### 🔴 Deep Layer — 高频/核心 (54项)

必须先讲透，含结构组件(struct/field/call flow)，可选源码证据。

| # | Knowledge Point | 01 Pri | 为什么🔴 |
|:--:|------|:--:|------|
| 1 | **InnoDB整体架构** | 近P1 | 后端工程师必须理解MySQL内部组件关系，面试最高频 |
| 2 | **InnoDB Buffer Pool** | 近P1 | 缓冲池是MySQL性能调优的第一入口：缓存命中率/脏页/LRU |
| 3 | **Redo Log / WAL** | 近P1 | 事务持久性核心机制，崩溃恢复基础，面试必问 |
| 4 | **Undo Log** | 近P1 | 事务回滚+MVCC版本链的基础，不理解undo=不理解MVCC |
| 5 | **B+Tree索引结构** | 近P1 | MySQL默认索引结构，聚簇/二级/页分裂/页合并直接影响SQL性能 |
| 6 | **事务ACID** | 近P1 | 数据库核心概念，4个特性各自有独立实现机制 |
| 7 | **MVCC多版本并发控制** | 近P1 | ReadView/版本链/可见性规则=MySQL并发控制核心 |
| 8 | **InnoDB行锁(Record/Gap/Next-Key/Insert Intention)** | 近P1 | 生产死锁/锁等待的根因，区别Record/Gap/Next-Key是排查前提 |
| 9 | **主从复制(Binlog/IO/SQL线程/中继日志)** | 近P1 | 读写分离/高可用的基础设施，后端架构必备 |
| 10 | **GTID复制** | 近P1 | 现代MySQL复制的标准标识，auto_position自动定位故障恢复 |
| 11 | **并行复制(DATABASE/LOGICAL_CLOCK/WRITESET)** | 近P1 | 解决主从延迟的核心技术，LOGICAL_CLOCK和WRITESET的区别决定生产配置 |
| 12 | **EXPLAIN执行计划** | 近P1 | SQL调优第一工具，type/rows/key/Extra解读是后端日常技能 |
| 13 | **Performance Schema** | 近P1 | MySQL唯一深度诊断框架，instrument/consumer/event模型是性能分析的入口 |
| 14 | **元数据锁 MDL** | 近P1 | DDL阻塞的根因，online DDL失败/备份卡住→都是MDL问题 |
| 15 | **死锁检测与处理** | 近P1 | 生产高频故障，等待图/超时/自动回滚→后端必须会排查 |
| 16 | **查询优化器(RBO/CBO/访问方法/联接算法/子查询)** | 近P1 | SQL性能的决定者，理解const/ref/range/all/NLJ/BNL/Hash Join |
| 17 | **慢查询日志** | 近P1 | 性能问题第一信号源，long_query_time/日志解析/pt-query-digest |
| 18 | **组复制 Group Replication** | 近P1 | InnoDB Cluster的数据层，XCom/Paxos/单主多主/一致性级别 |
| 19 | **半同步复制** | P2 | 核心耐久性保证，after_sync/after_commit/ACK降级异步 |
| 20 | **Doublewrite Buffer** | P2 | 部分写保护的唯一机制，理解为什么16KB页需要doublewrite |
| 21 | **Change Buffer / Insert Buffer** | P2 | 二级索引写入性能的关键优化，merge时机和参数 |
| 22 | **Binlog Event格式(GTID_EVENT/QUERY/ROWS/XID)** | P2 | 复制故障排查需要理解Event级别内容，B10逐字节解析 |
| 23 | **Seconds_Behind_Master 计算** | P2 | 复制延迟最常用指标，源码级理解clock_diff_with_master |
| 24 | **GTID_PURGED/gtid_executed状态管理** | P2 | 主从切换/误操作恢复的核心状态变量 |
| 25 | **MTS多线程并行回放(Coordinator/检查点/Gap)** | P2 | 并行复制从库侧实现，Coordinator分发策略+gap问题 |
| 26 | **主从故障转移(计划切换/紧急Failover)** | P2 | 高可用运维核心操作，避免脑裂+数据一致性保证 |
| 27 | **InnoDB Cluster(AdminAPI/Shell/Router)** | P2 | Oracle官方HA方案，三组件协同+自动故障转移 |
| 28 | **索引策略(覆盖索引/前缀索引/列基数/主键)** | P2 | SQL性能调优的实操层，覆盖索引消除回表/前缀索引节省空间 |
| 29 | **子查询优化(消除/合并/转JOIN/窗口函数)** | P2 | 生产SQL重构的常见模式，子查询→JOIN/半连接转换 |
| 30 | **锁优化策略(事务大小/索引/隔离级别/SKIP LOCKED)** | P2 | 锁争用缓解的实操方法，SKIP LOCKED=MySQL 8队列式任务处理 |
| 31 | **事务组提交 Group Commit(FLUSH/SYNC/COMMIT)** | P2 | Binlog写入的核心机制，组提交的batch效应直接影响TPS |
| 32 | **Binlog Cache(cache_size/临时文件/事务写入)** | P2 | 大事务导致binlog cache溢出→磁盘临时文件的根因 |
| 33 | **DUMP线程(POSITION vs GTID/查找过滤算法)** | P2 | 主库发送binlog的核心线程，GTID模式下的过滤算法 |
| 34 | **从库中继日志与状态日志** | P2 | relay log=复制持久化的关键，crash-safe slave=relay_log_recovery |
| 35 | **InnoDB ClusterSet 跨数据中心** | P2 | 异地灾备方案，主Cluster→副本Cluster异步复制链路 |
| 36 | **数据字典(frm/事务DD/SDI/原子DDL)** | P2 | MySQL 8.0引入事务型数据字典，取代frm，理解DDL原子性的基础 |
| 37 | **Online DDL(INPLACE/COPY/INSTANT/pt-osc)** | P2 | 生产DDL变更的核心技术，三种算法的并发能力和锁行为差异 |
| 38 | **数据库连接池(原理/HikariCP/Druid/c3p0)** | P2 | 后端基础设施，连接复用/池大小公式/FixedPool思想 |
| 39 | **备份恢复(mysqldump/XtraBackup/mydumper/Clone)** | P2 | DBA核心技能，逻辑vs物理备份/时间点恢复PITR |
| 40 | **ProxySQL中间件(读写分离/SQL重写/集群)** | P2 | 生产读写分离常用方案，SQL路由+黑名单+流量镜像 |
| 41 | **WRITESET并行复制源码** | P2 | MySQL 8并行复制最高级模式，write_set生成/last commit判定 |
| 42 | **LSM Tree原理(MemTable→SST→Compaction)** | P2 | 现代KV存储引擎核心(BoltDB/LevelDB/RocksDB)，区别于B+Tree的写优化 |
| 43 | **全文索引 FULLTEXT** | P2 | MySQL内置全文搜索，倒排索引+ngram/MeCab分词 |
| 44 | **数据倾斜与Shuffle** | P2 | 大数据SQL核心问题，Shuffle=网络重分区/数据倾斜=hot key |
| 45 | **哈希连接 Hash Join(MySQL 8.0.18+)** | P2 | MySQL 8重要新特性，替代BNL降低大表Join内存开销 |
| 46 | **索引统计信息(innodb_table_stats/index_stats)** | P2 | 优化器决策的数据源，ANALYZE TABLE更新时机影响执行计划 |
| 47 | **复制故障修复(不一致/checksum/误操作)** | P2 | 生产高频场景，pt-table-checksum校验+pt-table-sync修复 |
| 48 | **MySQL监控指标(连接/语句/IO/缓冲池/锁/复制)** | P2 | 九维监控指标是生产可观测性的基础 |
| 49 | **sys Schema(视图/诊断11例)** | P2 | performance_schema的易用封装，statement_analysis/innodb_lock_waits |
| 50 | **214连接数源码解析** | P3 | B7独有源码排查案例，open_files_limit→max_connections限制链 |
| 51 | **BoltDB源码(page/node/Bucket/Tx/DB)** | P3 | 唯一Go嵌入式B+Tree引擎源码分析 |
| 52 | **LevelDB源码(MemTable/WAL/SST/Compact/Version)** | P3 | 经典LSM引擎源码，BigTable架构的C++实现 |
| 53 | **HikariCP三大性能优化(FastList/ConcurrentBag/字节码)** | P3 | 连接池源码级优化，ConcurrentBag无锁并发+字节码精简 |
| 54 | **CDC/OGG异构同步(Oracle→MySQL→大数据)** | P3 | 跨数据库实时数据同步方案，Change Data Capture |

---

### 🟡 Working Layer — 常见/支撑 (22项)

机制+原因，不含源码组件。理解其存在和价值即可。

| # | Knowledge Point | 01 Pri | 说明 |
|:--:|------|:--:|------|
| 55 | **InnoDB后台线程(Master/Purge/PageCleaner/IO)** | P2 | 后台基础设施，理解各线程职责对调优有帮助但非面试核心 |
| 56 | **自适应哈希索引 AHI** | P2 | InnoDB自动内部优化，DBA需要知道但普通后端不直接操作 |
| 57 | **information_schema(Server+InnoDB层字典表)** | P1 | 系统元数据库，日常查询参考——不要求深挖实现 |
| 58 | **多源复制 Multi-Source** | P2 | 特定场景(多主合一)，不通用 |
| 59 | **复制过滤(库级/表级/replicate_do_db)** | P2 | 运维配置项，不是核心机制 |
| 60 | **MySQL Router 路由层** | P2 | InnoDB Cluster的连接层工具，部署后自动工作 |
| 61 | **MySQL Shell** | P2 | AdminAPI CLI工具，运维操作入口 |
| 62 | **字符集与Collation(utf8mb4/比较规则)** | P2 | 基础但不需要深度机制——知道utf8mb4 vs utf8即可 |
| 63 | **直方图 Histogram** | P2 | MySQL 8优化器增强，列值分布的辅助统计 |
| 64 | **B+Tree并发控制(Latch/Blink/OLFIT/Bw-tree)** | P2 | 学术研究级，普通后端不需了解OLFIT/Bw-tree实现 |
| 65 | **sysbench基准测试** | P2 | 工具知识，不是MySQL原理 |
| 66 | **Purge线程(清理过期undo/delete标记)** | P2 | MVCC维护机制，自动化运行不需人工干预 |
| 67 | **InnoDB Mutex与RW-Lock Semaphore** | P2 | InnoDB内部锁，DBA排查信号量等待时需要但非常规知识 |
| 68 | **显式表锁/用户级锁/刷新锁** | P2 | 特殊场景锁(LOCK TABLES/GET_LOCK/FLUSH)，日常不用 |
| 69 | **线程模型(连接线程/THD/SQL线程)** | P2 | 内部实现细节，理解职责即可不需源码 |
| 70 | **MySQL插件(Clone Plugin克隆)** | P3 | InnoDB Cluster供给组件，特定运维场景 |
| 71 | **Hive/Spark/Flink SQL执行原理** | P3 | 大数据引擎，MySQL专项中为扩展知识 |
| 72 | **大数据RBO优化20种** | P3 | 大数据特有，MySQL专项中降级为🟡 |
| 73 | **CAP理论与分布式数据库** | P3 | 理论概念，非MySQL核心 |
| 74 | **JDBC与SPI机制** | P3 | Java生态基础知识，非MySQL专属 |
| 75 | **Micrometer+Prometheus监控架构** | P3 | 可观测性工具链，非MySQL原理 |
| 76 | **leakDetectionThreshold连接泄露检测** | P3 | HikariCP特定参数，单库单场景 |

---

### 🟢 Surface Layer — 边缘/扩展 (9项)

1-2句定义即可，不展开机制。

| # | Knowledge Point | 01 Pri | 放在哪 |
|:--:|------|:--:|------|
| 77 | **哈希表索引(链接法/开放寻址/Cuckoo/Hopscotch/RobinHood)** | P3 | 索引结构对比章节：B+Tree为主流，哈希表为补充 |
| 78 | **ARIES恢复算法** | P3 | Redo/Undo章节末尾：MySQL未完整实现ARIES |
| 79 | **影子分页/MARS/WBL** | P3 | WAL章节替代方案一栏 |
| 80 | **OCC乐观并发/T/O时间戳/SSI序列化图** | P3 | MVCC章节末尾：MySQL之外的并发控制选项 |
| 81 | **HTAP混合事务分析** | P3 | InnoDB章节末尾：新一代数据库方向 |
| 82 | **五维数据库选型方法论** | P3 | DBA视角章节，提供选型框架 |
| 83 | **大数据SQL调优方法论** | P3 | 附录/扩展阅读：大数据与MySQL调优的异同 |
| 84 | **Bw树/Blink树/OLFIT树** | P3 | B+Tree章节末尾：学术前沿变体 |
| 85 | **数据压缩与编码** | P3 | 存储结构章节，提及但不展开 |

---

### 深度分类统计

| 等级 | 数量 | 说明 |
|:--:|:--:|------|
| 🔴 Deep | **54** (64%) | 含struct/field/call flow，可选源码证据 |
| 🟡 Working | **22** (26%) | 机制原理+原因，不含源码结构 |
| 🟢 Surface | **9** (10%) | 1-2句定义 |

> 8.5-9/10深度目标：🔴含源码级结构组件(如Buffer Pool→LRU/Free/Flush链表→struct定义)，🟡机制+行为解释，🟢概念名+一句话。
> MySQL专项中B6大数据/B14连接池/B13跨库DBA的部分知识元降级为🟡/🟢——不属于MySQL核心但作为扩展知识保留。

## [03 聚类] 机制边界 + 依赖图 + 教学顺序

> 聚类原则: 共享底层机制 → 同集群 | 依赖 = 不懂B无法理解A | 拓扑排序 → 零前向引用

---

### Cluster A: MySQL 架构与存储引擎核心 (🔴 12项)

**机制边界**: Server层→存储引擎层→文件层的体系结构，InnoDB核心组件的关系与作用。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| A1 | InnoDB整体架构 | 🔴 | 近P1 |
| A2 | InnoDB Buffer Pool | 🔴 | 近P1 |
| A3 | InnoDB后台线程 | 🟡 | P2 |
| A4 | Doublewrite Buffer | 🔴 | P2 |
| A5 | Change Buffer | 🔴 | P2 |
| A6 | 自适应哈希索引 AHI | 🟡 | P2 |
| A7 | 数据字典 | 🔴 | P2 |
| A8 | 线程模型(连接/THD/SQL) | 🟡 | P2 |
| A9 | 索引统计信息 | 🔴 | P2 |
| A10 | 直方图 Histogram | 🟡 | P2 |
| A11 | MySQL监控指标 | 🔴 | P2 |
| A12 | 数据压缩与编码 | 🟢 | P3 |

> **依赖**: 零依赖 (MySQL知识的起点, CPU/内存/磁盘硬件基础从B3或独立前置引入)

---

### Cluster B: 持久化与崩溃恢复 (🔴 6项)

**机制边界**: 事务如何被持久化到磁盘——Redo Log WAL→Checkpoint→Crash Recovery完整链路。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| B1 | Redo Log / WAL | 🔴 | 近P1 |
| B2 | Undo Log | 🔴 | 近P1 |
| B3 | Binlog Cache | 🔴 | P2 |
| B4 | 事务组提交 Group Commit | 🔴 | P2 |
| B5 | 从库中继日志与状态日志 | 🔴 | P2 |
| B6 | Purge线程 | 🟡 | P2 |

> **依赖**: A1(InnoDB架构)→B1-4(持久化组件在InnoDB架构内) | A2(Buffer Pool)→B1(Redo Log Buffer在BP内)

---

### Cluster C: 索引 (B+Tree主体) (🔴 9项)

**机制边界**: B+Tree为核心的索引体系的完整覆盖——从结构原理到使用策略到变更工具。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| C1 | B+Tree索引结构 | 🔴 | 近P1 |
| C2 | 索引策略(覆盖/前缀/基数/主键) | 🔴 | P2 |
| C3 | 全文索引 FULLTEXT | 🔴 | P2 |
| C4 | Online DDL(INPLACE/COPY/INSTANT) | 🔴 | P2 |
| C5 | 哈希连接 Hash Join | 🔴 | P2 |
| C6 | B+Tree并发控制(Latch/Blink) | 🟡 | P2 |
| C7 | 哈希表索引(Cuckoo/Hopscotch) | 🟢 | P3 |
| C8 | Bw树/Blink树/OLFIT树 | 🟢 | P3 |
| C9 | LSM Tree原理 | 🔴 | P2 |

> **依赖**: A1(InnoDB架构)→C1(索引在InnoDB架构内) | A2(Buffer Pool)→C1(索引页在BP中缓存) | C1→C2(理解结构才能理解策略) | C1→C4(理解索引才能理解DDL对索引的影响)

---

### Cluster D: 事务与并发控制 (🔴 8项)

**机制边界**: ACID的实现——MVCC提供一致性读、锁提供并发写隔离、隔离级别串起两者。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| D1 | 事务ACID | 🔴 | 近P1 |
| D2 | MVCC多版本并发控制 | 🔴 | 近P1 |
| D3 | InnoDB行锁(Record/Gap/Next-Key) | 🔴 | 近P1 |
| D4 | 元数据锁 MDL | 🔴 | 近P1 |
| D5 | 死锁检测与处理 | 🔴 | 近P1 |
| D6 | 锁优化策略(SKIP LOCKED等) | 🔴 | P2 |
| D7 | InnoDB Mutex与RW-Lock | 🟡 | P2 |
| D8 | 显式表锁/用户级锁/刷新锁 | 🟡 | P2 |

> **依赖**: B1(Redo Log=ACID的D)→D1 | B2(Undo Log=ACID的A+MVCC版本链)→D2 | C1(B+Tree索引)→D3(Gap/Next-Key锁依赖B+Tree结构) | D3+D4→D5(死锁=锁冲突的极端情况) | D3→D6(理解锁才能优化)

---

### Cluster E: 复制 (🔴 12项)

**机制边界**: Binlog从主库产生→传输→从库回放的全链路——覆盖传统复制、GTID、半同步、并行回放、故障处理。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| E1 | 主从复制(Binlog/IO/SQL线程) | 🔴 | 近P1 |
| E2 | GTID复制 | 🔴 | 近P1 |
| E3 | 半同步复制 | 🔴 | 近P1 |
| E4 | 并行复制(DATABASE/LOGICAL_CLOCK/WRITESET) | 🔴 | 近P1 |
| E5 | GTID_PURGED/gtid_executed状态 | 🔴 | P2 |
| E6 | MTS多线程并行回放 | 🔴 | P2 |
| E7 | Seconds_Behind_Master 计算 | 🔴 | P2 |
| E8 | Binlog Event格式 | 🔴 | P2 |
| E9 | DUMP线程(POSITION/GTID模式) | 🔴 | P2 |
| E10 | WRITESET并行复制源码 | 🔴 | P2 |
| E11 | 多源复制 Multi-Source | 🟡 | P2 |
| E12 | 复制过滤(库级/表级) | 🟡 | P2 |

> **依赖**: B4(Group Commit)→E1(Binlog在组提交中生成) | B3(Binlog Cache)→E9(DUMP线程读取Cache) | E1→E2(GTID是复制标识的升级) | E1→E3(半同步是传输确认的加强) | E1→E4(并行复制是SQL线程的升级) | D1(事务概念)→E1(复制的单位是事务)

---

### Cluster F: 复制运维与故障处理 (🔴 5项)

**机制边界**: 复制在生产中的运维操作——计划切换、紧急故障转移、数据不一致修复。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| F1 | 主从故障转移(计划/紧急Failover) | 🔴 | P2 |
| F2 | 复制故障修复(不一致/checksum) | 🔴 | P2 |
| F3 | 备份恢复(mysqldump/XtraBackup) | 🔴 | P2 |
| F4 | sysbench基准测试 | 🟡 | P2 |
| F5 | 214连接数源码解析 | 🔴 | P3 |

> **依赖**: E1-E12(复制全栈)→F1(故障转移以复制机制为基础) | E1+E7→F2(理解复制+延迟才能诊断不一致)

---

### Cluster G: InnoDB 高可用方案 (🔴 5项)

**机制边界**: 从组复制到InnoDB Cluster到跨数据中心ClusterSet的HA全栈。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| G1 | 组复制 Group Replication | 🔴 | 近P1 |
| G2 | InnoDB Cluster(AdminAPI/Shell/Router) | 🔴 | P2 |
| G3 | InnoDB ClusterSet 跨数据中心 | 🔴 | P2 |
| G4 | MySQL Router 路由层 | 🟡 | P2 |
| G5 | MySQL Clone Plugin | 🟡 | P3 |

> **依赖**: E1(主从复制)→G1(组复制=增强型复制, Paxos共识替代异步) | G1→G2(InnoDB Cluster=组复制+Shell+Router) | G2→G3(ClusterSet=Cluster间复制)

---

### Cluster H: 查询优化与性能诊断 (🔴 7项)

**机制边界**: SQL从提交到返回结果的完整路径——优化器决策→执行计划→慢查询→调优。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| H1 | 查询优化器(RBO/CBO/访问方法/联接算法) | 🔴 | 近P1 |
| H2 | EXPLAIN执行计划 | 🔴 | 近P1 |
| H3 | 慢查询日志 | 🔴 | 近P1 |
| H4 | 子查询优化(消除/合并/半连接) | 🔴 | P2 |
| H5 | information_schema | 🟡 | P1 |
| H6 | sys Schema(视图/诊断) | 🔴 | P2 |
| H7 | MySQL Shell | 🟡 | P2 |

> **依赖**: C1(索引)→H1(优化器基于索引选访问方法) | H1→H2(EXPLAIN是优化器决策的输出) | H2→H3(EXPLAIN解读→识别慢查询) | H1→H4(子查询优化在优化器内部执行)

---

### Cluster I: 监控与可观测性 (🔴 4项)

**机制边界**: MySQL内置监控框架——Performance Schema的instrument/consumer/event模型，Prometheus集成。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| I1 | Performance Schema(instrument/consumer/event) | 🔴 | 近P1 |
| I2 | 数据库连接池(HikariCP/Druid/c3p0) | 🔴 | P2 |
| I3 | ProxySQL中间件(读写分离/SQL重写) | 🔴 | P2 |
| I4 | Micrometer+Prometheus监控 | 🟡 | P3 |

> **依赖**: 全集群完成后引入——监控需要先理解被监控的对象(A-H全部完成后才讲I)

---

### Cluster J: 扩展专题 (🟡 8项)

**机制边界**: MySQL核心之外的扩展知识——大数据SQL、跨库DBA、存储引擎源码、并发理论。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| J1 | BoltDB源码(page/node/Bucket/Tx/DB) | 🔴 | P3 |
| J2 | LevelDB源码(MemTable/WAL/SST/Compact) | 🔴 | P3 |
| J3 | HikariCP性能优化(FastList/ConcurrentBag/字节码) | 🔴 | P3 |
| J4 | CDC/OGG异构同步 | 🔴 | P3 |
| J5 | Hive/Spark/Flink SQL执行原理 | 🟡 | P3 |
| J6 | 大数据RBO优化20种 | 🟡 | P3 |
| J7 | 大数据SQL调优方法论 | 🟢 | P3 |
| J8 | 数据倾斜与Shuffle | 🔴 | P2 |

> **依赖**: C1(B+Tree)→J1(对比BoltDB B+Tree实现) | C9(LSM Tree)→J2(LevelDB=LSM实现) | I2→J3(连接池概念→HikariCP源码) | E1→J4(MySQL复制→CDC异构同步) | H1→J5-7(MySQL优化器→大数据优化器)

---

### Cluster K: 理论/学术/选型 (🟢 6项)

**机制边界**: 通用数据库理论知识，非MySQL专属，作为参考和扩展。

| # | Knowledge Point | 深度 | 01 Pri |
|:--:|------|:--:|:--:|
| K1 | ARLES恢复算法 | 🟢 | P3 |
| K2 | 影子分页/MARS/WBL | 🟢 | P3 |
| K3 | OCC乐观并发/T/O时间戳/SSI | 🟢 | P3 |
| K4 | CAP理论与分布式DB | 🟡 | P3 |
| K5 | HTAP混合事务分析 | 🟢 | P3 |
| K6 | 五维数据库选型方法论 | 🟢 | P3 |

> **依赖**: B1(WAL)→K1(ARIES是WAL恢复算法的标准实现) | D2(MVCC)→K3(OCC/T/O是MVCC之外的并发方案) | E1(复制)→K4(CAP理论约束分布式DB设计)

---

## 依赖图与教学顺序

### 跨集群依赖图

```
A(M架构核心) ──┬── B(持久化) ──┬── D(事务并发) ──┬── E(复制) ──┬── F(复制运维)
               │               │                 │              │
               ├── C(索引) ────┘                 │              ├── G(InnoDB HA)
               │                                 │              │
               └── H(查询优化) ←──────────────┘              │
                                                              │
               I(监控可观测) ←── 全部A-H完成后引入            │
                                                              │
               J(扩展专题) ←── A/C/E/I交叉依赖                │
               K(理论学术) ←── B/D/E交叉参考                  │
```

### 教学顺序 (拓扑排序)

```
阶段1 — 地基: A(11项) — MySQL架构+InnoDB核心组件 (零依赖)
阶段2 — 持久化: B(6项) — Redo/Undo/WAL/Checkpoint/Binlog Cache (依赖A)
阶段3 — 索引:   C(9项) — B+Tree+LSM+Hash+DDL (依赖A)
阶段4 — 事务:   D(8项) — ACID+MVCC+锁全栈 (依赖B+C)
阶段5 — 复制:   E(12项) — 主从→GTID→并行→MTS→WRITESET (依赖B+D)
阶段6 — 运维:   F(5项) — 故障修复+备份+Failover (依赖E)
阶段7 — 高可用: G(5项) — Group Replication→InnoDB Cluster→ClusterSet (依赖E)
阶段8 — 优化:   H(7项) — 优化器→EXPLAIN→慢查询→子查询 (依赖C+D)
阶段9 — 监控:   I(4项) — Perf Schema→连接池→ProxySQL (全栈完成后)
阶段10 — 扩展:  J(8项) — BoltDB/LevelDB/HikariCP/BigData/CDC (交叉依赖)
阶段11 — 理论:  K(6项) — ARIES/OCC/CAP/HTAP (交叉参考)
```

### 集群统计

| 集群 | 名称 | 知识点 | 🔴 | 🟡 | 🟢 | 阶段 |
|:--:|------|:--:|:--:|:--:|:--:|:--:|
| A | 架构与存储引擎核心 | 12 | 8 | 3 | 1 | 1 |
| B | 持久化与崩溃恢复 | 6 | 5 | 1 | 0 | 2 |
| C | 索引(B+Tree主体) | 9 | 6 | 1 | 2 | 3 |
| D | 事务与并发控制 | 8 | 6 | 2 | 0 | 4 |
| E | 复制 | 12 | 10 | 2 | 0 | 5 |
| F | 复制运维与故障 | 5 | 4 | 1 | 0 | 6 |
| G | InnoDB高可用方案 | 5 | 3 | 2 | 0 | 7 |
| H | 查询优化与性能 | 7 | 5 | 2 | 0 | 8 |
| I | 监控与可观测性 | 4 | 3 | 1 | 0 | 9 |
| J | 扩展专题 | 8 | 5 | 2 | 1 | 10 |
| K | 理论/学术/选型 | 6 | 0 | 1 | 5 | 11 |
| **总计** | | **82** | **55** | **18** | **9** | |

> ⚠️ 注意: 85个知识元中，sysbench(P2🟡)未分配独立集群——已并入F(复制运维)作为基准测试工具。
> 哈希表索引C7(Cuckoo/Hopscotch)已并入C(索引)集群作为B+Tree的备选和对比。
