# MySQL 数据库 — 知识大纲

> 14本书 716 KPs → 85知识元 → 11集群 11阶段 | 1666行规划全文见 `08-MySQL.md`
> 写作深度: 8.5-9/10 | 🔴=含struct/call flow+可选源码 | 🟡=机制+原因 | 🟢=1-2句

---

## 阶段1: MySQL 架构与 InnoDB 核心

### 1.1 MySQL Server 层与存储引擎层分离
- Server层(连接/解析/优化/缓存/存储引擎接口) vs 存储引擎层(插件式) (B1Ch2, B7Ch3)
- 存储引擎API接口层——Handler类作为Server与InnoDB的契约 (B1Ch2[API层])

### 1.2 InnoDB 整体架构 (🔴)
- 四大核心组件: 缓冲池 + 日志缓冲(Redo/Undo) + 后台线程 + 文件层 (B1/5.1, B2/17, B7/3.5)
- 数据流向: 查询→缓冲池(缓存命中)→文件层(未命中)→Redo/Undo(写操作) (B2/17.2)
- 后台线程模型: Master/Purge/PageCleaner/IO 四线程职责 (B1/5.7, B7/3.6)
- 前台线程: 连接线程/查询执行线程/复制线程 (B7/3.7)

### 1.3 InnoDB Buffer Pool — 内存核心 (🔴)
- 三种链表: Free链表(空闲页管理) + LRU链表(淘汰, young/old区分) + Flush链表(脏页追踪) (B2/17.2)
- LRU改进: midpoint insertion(new→old区)/innodb_old_blocks_pct/全表扫描扫描抵抗 (B2/17.2.6, B4/4.3)
- 页哈希: (表空间号+页号)→缓冲页 快速定位 (B2/17.2.4)
- 脏页刷新: Flush链表→Checkpoint LSN→批量刷盘 (B2/17.2.8)
- 多实例: innodb_buffer_pool_instances 减少竞争 (B2/17.2.9)

### 1.4 InnoDB 持久化组件 (🔴)
- Doublewrite Buffer: 16KB页的部分写保护——先写doublewrite再写数据文件 (B1/5.6, B2/17.2.8, B4/4.4)
- Change Buffer: 二级索引写入优化——缓存非唯一索引变更→后台merge (B1/5.3)
- 自适应哈希索引(AHI): 热点页的哈希加速→自动构建/innodb_adaptive_hash_index (B1/5.4)

### 1.5 数据字典 (🔴)
- MySQL 8.0事务型数据字典: 取代.frm→DD表(innodb_ddl_log)/SDI序列化字典/原子DDL (B1/4.4, B2/8/9)
- 数据字典三层架构: 文件层(.frm旧版)/InnoDB层(DD表)/Server层(缓存) (B1/4.1)

### 1.6 MySQL 启动流程与线程模型 (🟡)
- 启动三阶段: 初始化→恢复→提供服务 (B1/2.4)
- 连接线程模型: THD类(每个连接一个线程) vs 线程池(复用) (B1/3.3)

### 1.7 字符集与Collation (🟡)
- utf8 vs utf8mb4: utf8最大3字节(不支持emoji)/utf8mb4完整4字节 (B2/3, B5/7)
- Collation比较规则: utf8mb4_general_ci vs utf8mb4_0900_ai_ci→影响排序/比较/索引使用 (B2/3, B5/7)

---

## 阶段2: 持久化与崩溃恢复

### 2.1 Redo Log / WAL — 写前日志 (🔴)
- WAL原则: 先写日志→后写数据页→崩溃后重放恢复 (B2/19.2, B3/8.3)
- Log Buffer: 环形缓冲区(log_t/log_block)→事务写入→innodb_flush_log_at_trx_commit控制落盘 (B1/5.5, B2/19.5)
- Redo日志格式: MLOG_*简单类型 / 复杂类型(物理+逻辑) (B2/19.3, B10/2.2-2.6)
- Mini-Transaction(MTR): 原子写入组——MTR内日志不可分割 (B2/19.4)

### 2.2 LSN 与 Checkpoint (🔴)
- LSN(Log Sequence Number): 全局递增序列号——flushed_to_disk_lsn+偏移量映射 (B2/19.7, B10/3)
- Checkpoint: LSN推进→脏页控制上限→崩溃恢复加速→日志截断 (B2/19.8, B3/8.6)
- 脏页批量刷出: 用户线程从Flush链表协助刷脏 (B2/19.9)

### 2.3 事务组提交 Group Commit (🔴)
- 三阶段: FLUSH(刷binlog cache到文件)→SYNC(fsync)→COMMIT(引擎层提交) (B1/8.1, B10/3.3)
- 组提交的batch效应: 多个事务合并一次fsync→TPS大幅提升 (B10/3.3)
- Binlog Cache: 事务Event先写入binlog_cache_size→溢出→磁盘临时文件(trx-cache.0) (B10/3.1-3.2)

### 2.4 Undo Log — 回滚与MVCC基础 (🔴)
- Undo日志格式: INSERT(只回滚)→DELETE(记录前镜像)→UPDATE(前后镜像) (B2/20.3, B3/8.3)
- 回滚段(Rollback Segment): 概念→申请→多个回滚段→分类→roll_pointer组成 (B2/20.9)
- Undo页面链表: 单事务内Undo链→多事务间Undo链 (B2/20.6)
- Undo在崩溃恢复中的作用: 未提交事务回滚 + MVCC版本链保留 (B2/20.11)
- UNDO表空间: innodb_undo_tablespaces 独立管理 (B2/20.10)

### 2.5 崩溃恢复流程 (🔴)
- 确定恢复起点: 最后完成的Checkpoint LSN (B2/19.12)
- 确定恢复终点: 最后一个完整的Redo日志 (B2/19.12)
- 恢复执行: Redo→Undo(回滚未提交)→Binlog协调(内部XA) (B2/19.12)

### 2.6 Purge线程 (🟡)
- 清理过期Undo: 不再被任何ReadView需要的旧版本 (B2/21.4, B3/9.9)
- 物理删除: delete-marked记录在Purge中转化为真正的删除

---

## 阶段3: 索引

### 3.1 B+Tree 索引结构 (🔴)
- B+Tree vs B-Tree: 所有数据在叶子节点+叶子节点双向链表=范围查询高效 (B1/7.1, B3/7.1-7.2)
- InnoDB B+Tree: 聚簇索引(主键+数据在叶子)→二级索引(索引键+主键在叶子)→回表 (B1/7.2, B2/6.2)

### 3.2 InnoDB 数据页结构 (🔴)
- 页(Page) = 16KB 固定大小 (B2/4.2, B2/5.2)
- 7个组成部分: File Header/Page Header/Infimum+Supremum/User Records/Free Space/Page Directory/File Trailer (B2/5.2-5.7)
- Page Directory: 稀疏索引+Slot二分查找→页内快速定位 (B2/5.4)

### 3.3 InnoDB 行格式 (🔴)
- COMPACT: 变长字段长度列表/NULL值列表/记录头/真实数据 (B2/4.3.2)
- DYNAMIC/COMPRESSED: 溢出列处理/BLOB存储 (B2/4.3.4-4.3.5)
- 行溢出: 大字段→溢出页(20字节指针指向外部存储页) (B2/4.3.4)

### 3.4 表空间(Extent/Segment) (🔴)
- Extent(区): 64个页=1MB, 空间分配的基本单位 (B2/9.2.1)
- Segment(段): 叶子节点段+非叶子节点段+回滚段→逻辑容器 (B2/9.2.2)
- 系统表空间 vs 独立表空间: ibdata1 vs *.ibd (B2/9.3)

### 3.5 索引操作与维护 (🔴)
- 页分裂(Page Split): 写入新记录→页满→分裂→更新父节点 (B1/7.5)
- 页合并(Page Merge): 删除→利用率过低→相邻页合并 (B1/7.5)
- 索引检索: 聚簇检索(一次定位)→二级检索(回表两阶段) (B1/7.4)
- 回表的代价: 随机IO→覆盖索引消除回表 (B2/7.4-7.5)

### 3.6 索引策略 (🔴)
- 覆盖索引: SELECT列全部在索引中→不回表 (B2/7.5.5)
- 前缀索引: 长字段只索引前N字符→节省空间但可能降低选择性 (B2/7.5.4)
- 索引列基数: 高基数(不重复值多)=高选择性=索引有效 (B2/7.5.2)
- 主键选择: 自增(顺序插入)/UUID(随机插入→页分裂) (B2/7.5.7)
- 最左前缀: 复合索引从最左列开始匹配 (B2/7.3.1)

### 3.7 全文索引 FULLTEXT (🔴)
- 倒排索引原理: 词→文档列表→相关性排序 (B1/7.1)
- MySQL全文: ngram分词(中日韩)/MeCab/自然语言模式vs布尔模式 (B5/14.6)

### 3.8 索引统计信息 (🔴)
- innodb_table_stats/innodb_index_stats: 基数/页数/平均行长 (B2/13, B5/15, B7/14)
- ANALYZE TABLE: 手动或自动触发统计更新→影响优化器执行计划选择 (B5/15.6)
- **直方图 Histogram** (🟡): 列值分布统计—Singleton等高/Equi-height等宽→等值查询 JOIN列优化 (B5/16, B12/27.2)

### 3.9 Online DDL (🔴)
- 三种算法: INPLACE(不锁表+重建)/COPY(复制表)/INSTANT(只改元数据, MySQL 8.0.12+) (B5/25.1, B12/7)
- pt-online-schema-change: 触发器+影子表+分批拷贝 (B12/7.2)
- DDL阻塞MDL定位: 5.6(INFORMATION_SCHEMA) vs 8.0(performance_schema.metadata_locks) (B12/7.3)

### 3.10 索引结构扩展 (🟡🟢)
- 哈希连接 Hash Join (MySQL 8.0.18+): 替代BNL, 构建哈希表+探测 (B5/17.3)
- B+Tree并发控制: Latch锁分支/乐观锁分支 (B3/7.3)
- 哈希表索引: 链接法/开放寻址/Cuckoo/RobinHood (🟢 B3/Ch5)
- Blink/OLFIT/Bw树: 高并发/无锁索引变体 (🟢 B3/7.4-7.6)

---

## 阶段4: 事务与并发控制

### 4.1 事务 ACID (🔴)
- Atomicity(原子性): Undo Log回滚未提交事务 (B2/18.1, B11/11)
- Consistency(一致性): 约束检查/级联更新/触发器 (B2/18.1)
- Isolation(隔离性): MVCC+锁→四种隔离级别 (B2/18.1, B11/12)
- Durability(持久性): Redo Log+Doublewrite→崩溃后恢复已提交 (B2/18.1)
- 事务控制: BEGIN/COMMIT/ROLLBACK/SAVEPOINT/autocommit/隐式提交 (B2/18.3)

### 4.2 四种隔离级别 (🔴)
- READ UNCOMMITTED→READ COMMITTED→REPEATABLE READ(MySQL默认)→SERIALIZABLE (B2/21.2, B11/12)
- 三种并发异常: 脏读/不可重复读/幻读 (B2/21.2.1)

### 4.3 MVCC 多版本并发控制 (🔴)
- 版本链: trx_id+roll_pointer→Undo Log链接→历史版本 (B2/21.3, B3/9.3)
- ReadView: 活跃事务列表→可见性判断(creator_trx_id/m_min_trx_id/m_max_trx_id) (B2/21.3.2)
- 二级索引与MVCC: 索引记录+主键回表→聚簇索引上判断可见性 (B2/21.3.3)

### 4.4 InnoDB 行锁 (🔴)
- Record Lock: 索引记录上的精确锁 (B1/8.2, B2/22.3, B11/7)
- Gap Lock: 索引记录之间的间隙锁→防止幻读(RR隔离级) (B1/8.2, B11/7)
- Next-Key Lock: Record+Gap合并→前开后闭区间 (B1/8.2, B11/7)
- Insert Intention Lock: 插入前的意向锁→不阻塞其他插入 (B11/7)
- 锁兼容矩阵: S/X/IS/IX 四种锁的互斥关系 (B11/5/8)

### 4.5 元数据锁 MDL (🔴)
- MDL类型: SHARED_READ/SHARED_WRITE/EXCLUSIVE (B1/8.2, B11/6)
- DDL阻塞: ALTER TABLE申请MDL_EXCLUSIVE→等待所有MDL_SHARED释放 (B5/18.3, B12/7.3)
- 诊断: 5.6→INFORMATION_SCHEMA.INNODB_LOCKS / 8.0→performance_schema.metadata_locks (B12/7.3)

### 4.6 死锁 (🔴)
- 产生条件: 循环等待(事务A等B释放/B等A释放) (B1/8.2, B2/22.6, B11/8/16)
- InnODB死锁检测: wait-for graph→检测到死锁→回滚代价最小的事务 (B11/8)
- 生产死锁三模式: 并发删除死锁/删除不存在数据死锁/插入意向锁死锁 (B7/34-36)
- 死锁日志: LATEST DETECTED DEADLOCK→事务1+事务2+锁等待图 (B11/2.2)

### 4.7 锁优化策略 (🔴)
- 减小事务粒度: 大事务拆小→减少锁持有时间 (B5/18.5, B11/9)
- 利用索引: 索引覆盖→锁定更少行 (B11/9)
- 隔离级别降级: RR→RC→减少Gap Lock (B11/9)
- SKIP LOCKED/NOWAIT (MySQL 8.0): 跳过已锁定行/立即返回而非等待 (B5/24.7, B11/9)

### 4.8 InnoDB 内部锁 (🟡)
- Mutex: 保护临界区(如buf_pool->mutex) (B1/8.2, B11/7)
- RW-Lock Semaphore: 允许并发读/独占写→自旋+os_wait (B11/7)
- 信号量等待诊断: SHOW ENGINE INNODB STATUS SEMAPHORES段 (B11/18)
- 显式表锁/用户级锁/刷新锁: LOCK TABLES/GET_LOCK()/FLUSH TABLES WITH READ LOCK→特殊场景 (B5/18.3, B11/6/13)

---

## 阶段5: 复制

### 5.1 主从复制基本原理 (🔴)
- 三线程模型: Binlog Dump(主)→IO线程(从:写RelayLog)→SQL线程(从:回放) (B1/9.1, B9/2, B12/2)
- 传统位点复制: MASTER_LOG_FILE+MASTER_LOG_POS (B9/4.1, B10/3.5)

### 5.2 GTID 复制 (🔴)
- GTID = server_uuid:transaction_id 全局唯一标识 (B9/4.2, B10/1.1)
- gtid_executed表(持久化)+gtid_executed变量(内存)+gtid_purged(已清理) (B10/1.2)
- auto_position: 从库自动定位→不再手动计算binlog位点 (B9/4.2)
- GTID生命周期: 事务提交→分配GTID→写入gtid_executed→DUMP线程传输 (B10/1.1)

### 5.3 半同步复制 (🔴)
- after_sync vs after_commit: 同步确认时机差异→数据一致性 vs 性能 (B9/5, B12/2.4)
- ACK机制: 至少一个从库确认收到→主库返回客户端 (B9/5.1)
- 超时降级异步: rpl_semi_sync_master_timeout→超时自动降级→从库追上后恢复 (B9/5)

### 5.4 并行复制 (🔴)
- DATABASE级别: 按库分发→不同库的事务可并行 (B9/6.2)
- LOGICAL_CLOCK: 基于组提交→同一组内的事务可并行 (B9/6.3)
- WRITESET: 基于写集合冲突检测→更高并行度→无主键退化为LOGICAL_CLOCK (B9/6.4, B10/3.4)

### 5.5 MTS 多线程并行回放 (🔴)
- Coordinator协调线程: 分发Event→Worker工作线程 (B10/4.1-4.2)
- Checkpoint机制: Worker完成→更新low-watermark→安全重启 (B10/4.1-4.2)
- Gap问题: 事务间依赖导致部分Worker空闲→slave_preserve_commit_order提交顺序保证 (B10/4.3)

### 5.6 Binlog Event 格式 (🔴)
- Event header(19字节)+Event data+footer(4字节) (B10/2.1, B9/3/26)
- 核心Event: GTID_EVENT/QUERY_EVENT(DDL)/MAP_EVENT(表结构)/WRITE_ROWS/DELETE_ROWS/UPDATE_ROWS/XID_EVENT(提交) (B10/2.2-2.6)
- binlog_row_image: full/minimal/noblob→控制Event大小 (B10/2.7)

### 5.7 DUMP 线程 (🔴)
- POSITION MODE: 从指定binlog文件+位置开始发送 (B10/3.5)
- GTID AUTO_POSITION MODE: 从库发送gtid_executed→主库过滤已发送的GTID (B10/3.5)
- GTID查找/过滤算法: 已发送/未发送/已purge三段集合逻辑 (B10/3.6)

### 5.8 Seconds_Behind_Master 延迟计算 (🔴)
- 公式: SBM = 当前时间 - SQL线程正在执行Event的时间戳 (B9/11, B10/4.9, B12/4.2)
- 源码变量: clock_diff_with_master(主从时钟差)/mi->rli->last_master_timestamp (B10/4.9)
- 伪延迟场景: 大事务(0→突发大值→0)/网络延迟/从库负载 (B10/4.10)

### 5.9 从库基础设施 (🔴)
- 中继日志(Relay Log): IO线程写入→SQL线程读取→purge (B9/8)
- 从库状态日志: master_info(Master信息持久化)/relay_log_info(SQL线程进度) (B9/8, B10/4.7)
- 从库崩溃恢复: relay_log_recovery=ON→丢弃未完整的中继日志→重新拉取 (B10/4.7)
- slave_rows_search_algorithms: TABLE_SCAN/INDEX_SCAN/HASH_SCAN (B10/4.6)

### 5.10 复制扩展 (🟡)
- 多源复制: 多个Master→一个Slave→FOR CHANNEL隔离 (B9/7)
- 复制过滤: replicate_do_db/replicate_ignore_db/replicate_do_table (B9/13)
- 延迟复制: CHANGE MASTER TO MASTER_DELAY=N→数据误删恢复窗口 (B12/2.7)

---

## 阶段6: 复制运维与故障处理

### 6.1 故障转移 (🔴)
- 计划切换(Planned Switchover): 确认从库追上→原主read_only→切换应用 (B8/19, B12/20)
- 紧急故障转移(Emergency Failover): 确认主库状态→选最新从库→补数据→切换+避免脑裂 (B8/20, B9/20)
- 模式切换: 传统↔GTID 在线迁移四路径(enforce_gtid_consistency逐步) (B9/17)

### 6.2 故障修复 (🔴)
- 复制中断修复: server_id重复/数据包超限/binlog缺失/GTID不匹配/主键冲突/DDL报错 (B12/4.4)
- 主从不一致修复: pt-table-checksum校验→pt-table-sync修复 (B12/4.5, B9/24)
- 误操作恢复: 延迟复制窗口→binlog2sql/MyFlash闪回 (B9/24, B12/2.7)

### 6.3 备份恢复 (🔴)
- mysqldump: 逻辑备份→SQL语句→跨版本兼容 (B7/48, B12/5.1)
- XtraBackup: 物理热备份→直接拷贝数据文件→PITR时间点恢复 (B7/49, B12/5.3)
- mydumper: 多线程并行逻辑备份 (B7/50)
- binlog2sql/MyFlash: 误操作闪回工具 (B7/51)

### 6.5 基准测试 (🟡)
- sysbench: 压测安装→通用脚本→自定义业务脚本→结果分析(QPS/TPS/P95/P99延迟) (B5/3, B7/44, B12/9.1)
- 根因链: open_files_limit→max_connections→ulimit -n→Linux资源限制 (B7/31)

---

## 阶段7: InnoDB 高可用

### 7.1 组复制 Group Replication (🔴)
- XCom通信引擎: Paxos变体→消息共识→成员资格管理 (B8/4.3, B1/9.2, B12/11.3)
- 单主模式 vs 多主模式: 自动选举写节点 vs 多节点并发写+冲突检测 (B8/4.2, B12/11.2)
- 事务一致性级别: BEFORE/AFTER/BEFORE_AND_AFTER→控制读取的数据版本 (B8/4.5, B12/11.7)
- 分布式恢复: 捐赠者(donor)→binlog传输→增量恢复→赶上集群 (B8/4.6, B12/11.4)
- 网络分区: 多数派存活→分区外节点驱逐→恢复后重新加入 (B12/11.6)
- 流量控制 Flow Control: 限制写入速度→防止从节点落后太多 (B8/4.8)

### 7.2 InnoDB Cluster (🔴)
- 三层组件: 组复制(数据层)+MySQL Shell+AdminAPI(管理层)+MySQL Router(路由层) (B8/7, B12/12)
- AdminAPI: dba.createCluster()→cluster.addInstance()→一键部署 (B8/8)
- MySQL Router: bootstrap→6447(RW)/6448(RO)→自动故障转移 (B8/6)
- 故障自动切换: 主节点故障→组复制选举新主→Router自动路由 (B12/12)

### 7.3 InnoDB ClusterSet 跨数据中心 (🔴)
- 架构: 主Cluster(同城多AZ)→副本Cluster(异地)→异步复制链路 (B8/10)
- 计划切换: Planned Failover→主Cluster→副本Cluster (B8/10.5)
- 紧急故障转移: Emergency→副本成为主→原主恢复后作为副本 (B8/10.5)
- ClusterSet限制: 异步延迟/不支持多主/AdminAPI依赖 (B8/10.6)

### 7.4 NDB Cluster / 克隆插件 (🟡)
- NDB Cluster: 无共享架构/数据节点/SQL节点/电信级 (B8/2.6)
- Clone Plugin: 物理克隆/远程克隆/InnoDB Cluster节点供给 (B8/11.4)

---

## 阶段8: 查询优化与性能诊断

### 8.1 查询优化器 (🔴)
- RBO基于规则优化: 条件化简/常量折叠/外连接消除/BETWEEN重写 (B2/14, B5/17, B6/3)
- CBO基于代价优化: I/O成本+CPU成本→索引统计驱动→代价比较→选最小代价 (B2/12, B5/17.2)
- 访问方法(Access Method): const/ref/ref_or_null/range/index/all (B2/10)
- 联接算法: NLJ嵌套循环/BNL块嵌套循环/Hash Join哈希连接(8.0.18+) (B2/11, B5/17.3)
- 优化器配置: engine_cost/server_cost表/optimizer_switch/Optimizer Hint (B5/17.5)

### 8.2 EXPLAIN 执行计划 (🔴)
- EXPLAIN输出字段: id/select_type/type/possible_keys/key/rows/filtered/Extra (B2/15, B5/20)
- EXPLAIN ANALYZE: 实际执行统计(MySQL 8.0.18+)→替换估算值 (B5/20.1)
- EXPLAIN FORMAT=JSON/TREE: 结构化输出→可视化执行树 (B5/20.2)

### 8.3 慢查询日志 (🔴)
- 配置: long_query_time/min_examined_row_limit/log_output=FILE/TABLE (B2/9, B5/9)
- 日志解析: mysqldumpslow/pt-query-digest→汇总/排序/样例 (B2/9.3, B7/47.1)
- 慢查询定位: events_statements_summary_by_digest→TOP N→EXPLAIN验证 (B5/19)

### 8.4 子查询优化 (🔴)
- 子查询消除: IN→EXISTS转换/Semijoin半连接/Materialization物化 (B2/14.3, B5/6)
- 子查询优化策略: 子查询→JOIN/窗口函数替代/UDF缓存/避免全表扫描 (B5/6)

### 8.5 sys Schema 诊断 (🔴)
- statement_analysis: 归一化SQL→总执行时间→平均延迟→锁等待 (B2/6, B5/6, B7/9)
- innodb_lock_waits: 锁等待链→阻塞者/被阻塞者/等待时间 (B7/9.2-9.3)
- schema_unused_indexes/schema_redundant_indexes: 未使用/冗余索引 (B7/9.5-9.6)

### 8.6 information_schema (🟡)
- Server层字典: TABLES/COLUMNS/STATISTICS/KEY_COLUMN_USAGE (B2/7, B5/7)
- InnoDB层字典: INNODB_TRX/INNODB_LOCKS/INNODB_LOCK_WAITS (B7/10-11)

---

## 阶段9: 监控与可观测性

### 9.1 Performance Schema (🔴)
- 三要素: instrument(检测点注册)+consumer(消费者表)+event(事件采集) (B2/5, B5/5, B7/4-6)
- event模型: 类型/范围/嵌套/属性→当前/历史/汇总三层表 (B2/5.5, B5/5.5)
- 线程监控: threads表→每个连接线程的instrument配置 (B2/5.2, B7/5.3.7)
- 摘要(digest): SQL归一化→DIGEST/DIGEST_TEXT→聚合统计 (B2/5.7)
- 复制监控表: replication_connection_*/replication_applier_*等8表 (B9/9)
- 九维核心监控指标: 连接/语句执行/临时表/表缓存/磁盘IO/缓冲池/redo/锁/复制 (B5/23.7, B7/46, B12/6.3)

### 9.2 锁监控 (🔴)
- data_locks: 当前持有的所有InnoDB锁→ENGINE/LOCK_TYPE/LOCK_MODE/LOCK_DATA (B5/18.6, B11/2)
- data_lock_waits: 锁等待关系→REQUESTING_ENGINE_LOCK_ID/BLOCKING_ENGINE_LOCK_ID (B11/2)
- InnoDB Lock Monitor: SHOW ENGINE INNODB STATUS TRANSACTIONS段 (B11/2.2)
- 死锁日志: LATEST DETECTED DEADLOCK→事务1+2+锁等待图 (B11/2.2)

### 9.3 数据库连接池 (🔴)
- HikariCP核心: 精简字节码(减少编译开销)+FastList(无范围检查)+ConcurrentBag(无锁并发) (B14/6-7)
- Fixed Pool Design: 固定连接数vs动态扩缩→为什么固定更好 (B14/4.2-4.4)
- 连接池对比: HikariCP vs Druid vs c3p0 vs DBCP2→性能/代码复杂度/功能 (B14/2, B12/8.1)
- 连接生命周期: borrow→requite→close→create→ConcurrentBag状态机 (B14/7)
- leakDetectionThreshold: 连接泄露检测→超过阈值打印堆栈 (B14/8.4)

### 9.4 ProxySQL 中间件 (🔴)
- 读写分离: mysql_users→default_hostgroup(W)→read_hostgroup(R) (B12/10)
- SQL重写+黑名单: 自动改写→禁止危险查询 (B12/10.2)
- 流量镜像: mirroring→生产流量复制到测试环境 (B12/10.2)

### 9.5 Prometheus + Grafana (🟡)
- MySQL Exporter → Prometheus 拉取 → Grafana Dashboard (B7/46, B14/11)
- Micrometer: 统一指标API(MeterRegistry)→SpringBoot Actuator集成 (B14/11.1-11.4)
- 7个核心HikariCP指标: active/idle/pending/total/connectionTimeout/creationTime/threadsAwaiting (B14/10.3)

---

## 阶段10: 扩展专题

### 10.1 LSM Tree (🔴)
- 核心原理: MemTable(内存)→WAL(持久化)→SST文件(刷盘)→Compaction(合并) (B3/Ch6, B4/Ch7)
- Compaction策略: 分层合并(Leveled)/分级合并(Tiered)/组合合并 (B3/6.3, B4/7.4)
- 布隆过滤器: 点查询加速→避免不必要的SST读取 (B3/6.4, B4/6.4)
- 键值分离(WiscKey): Key在LSM+Value在单独日志→降低Compaction写放大 (B3/6.6, B4/8.1)

### 10.2 LevelDB 源码 (🔴)
- MemTable: 跳表SkipList→内存写入缓冲 (B4/9.3)
- WAL(Log): 日志格式→Writer追加→Reader顺序读取→崩溃恢复 (B4/9.4)
- SSTable: Data Block/Index Block/Filter Block→SSTable读写全流程 (B4/9.5)
- Compaction: Minor(MemTable→SST)/Major(层级合并) (B4/9.6)
- MVCC多版本: Version→VersionEdit增量→VersionSet→快照隔离 (B4/9.7)

### 10.3 BoltDB 源码 (🔴)
- page类型: meta(元数据)+freelist(空闲管理)+branch(B+树内节点)+leaf(叶子节点) (B4/6.2)
- node: B+树节点→内存表示(inode数组)→CRUD+分裂合并 (B4/6.3)
- Bucket: 子B+树容器→Cursor光标→KV操作 (B4/6.4)
- Tx事务: 读写事务→MVCC→COW(Copy-on-Write)→Commit/Rollback (B4/6.5)

### 10.4 大数据 SQL (🟡)
- Hive/Spark/Flink SQL执行原理: 词法→语义→逻辑RBO→物理CBO→DAG/MapReduce (B6/Ch2)
- 大数据RBO技术: 谓词下推/常量折叠/等式传递/投影裁剪/列裁剪 (B6/Ch3)
- 数据倾斜: 成因(Skew Key)/诊断/解决方案(salting/广播/两阶段聚合) (B6/9.2-9.3)
- Shuffle: 数据重分区→网络传输→落盘→Exchange算子→大数据性能关键 (B6/9.2)

### 10.5 CDC/OGG 异构同步 (🔴)
- OGG(GoldenGate): CDC变更捕获→Extract抽取→Replicat复制→Oracle→MySQL→大数据 (B13/5.4)

### 10.6 HTAP (🟢)
- HTAP混合事务分析: TiDB(TiFlash)/Oracle 19c/MySQL HeatWave (B13/Ch6)

---

## 阶段11: 理论/学术/选型

### 11.1 并发控制理论 (🟢)
- ARIES恢复算法: LSN/提交/回滚/模糊检查点/三阶段恢复(Analysis→Redo→Undo) (B3/8.7)
- OCC乐观并发控制: 验证阶段(向后/向前/并行验证) (B3/9.6)
- 时间戳排序(T/O): 基础T/O算法/托马斯写入规则 (B3/9.5)
- SSI序列化快照隔离: 有向序列化图(DSG)→检测读写冲突 (B3/9.7)
- 影子分页/MARS/WBL: 影子分页=PRE-Image写/无Undo→MARS多核优化/WBL落后写日志→替代WAL方案 (B3/8.2/8.8)

### 11.2 分布式理论 (🟢)
- CAP: 一致性/可用性/分区容错→三者不可兼得 (B13/4.6)
- 分库分表: 中间件(ProxySQL/ShardingSphere) vs 客户端→一致性挑战 (B7/22)
- 垂直拆分 vs 水平拆分: 业务维度 vs 数据维度→聚合排序扩缩容 (B13/4.4)

### 11.3 DBA 选型方法论 (🟢)
- 五维选型: 业务场景/数据规模/开发能力/运维能力/公司管理 (B13/4.3)
- 数据库全景分类: 关系型/键值/列式/文档/图/时序/搜索 (B13/1.3)

---

> **来源**: 14本书 716 KPs → 85知识元 → 11集群 → 11阶段教学顺序
> **规划文件**: `规划/MySQL/08-MySQL.md` (1666行, 含完整01提取/01聚合/02深度/03聚类)
> **写作深度**: 🔴=含struct/call flow+可选源码 | 🟡=机制+原因 | 🟢=1-2句
