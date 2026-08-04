# 《深入理解 MySQL 主从原理》—— 完整目录

> 作者：高鹏 | 5章、342页、222个目录条目 | 聚焦 GTID/Event/主库/从库/案例

---

### 第1章 GTID
1.1 GTID 的基本概念（作用/基本表示/server_uuid生成/GTID生成/GTID_EVENT/PREVIOUS_GTIDS_LOG_EVENT/gtid_executed表）
1.2 mysql.gtid_executed 表、gtid_executed 变量、gtid_purged 变量的修改时机
1.3 GTID 模块初始化简介和参数 binlog_gtid_simple_recovery
1.4 GTID 中的运维（跳过事务/mysqldump导出/搭建主从/主从切换/在线离线开启GTID/注意事项/丢失数据测试）

### 第2章 Event
2.1 binary log Event 的总体格��（Event header/footer/具体解析/Event类型）
2.2 重点 Event 之 FORMAT_DESCRIPTION_EVENT 和 PREVIOUS_GTIDS_LOG_EVENT
2.3 重点 Event 之 GTID_EVENT（作用/源码接口/主体格式/生成时机/ANONYMOUS_GTID_EVENT/GTID三种模式）
2.4 重点 Event 之 QUERY_EVENT 和 MAP_EVENT
2.5 重点 Event 之 WRITE_EVENT 和 DELETE_EVENT
2.6 重点 Event 之 UPDATE_EVENT 和 XID_EVENT
2.7 参数 binlog_row_image 的影响
2.8 巧用 Event 发现问题（分析长期未提交事务/分析大事务/分析Event生成速度/分析DML Event分布）

### 第3章 主库
3.1 binlog cache 简介（使用流程/binlog_cache_size/临时文件/max_binlog_cache_size）
3.2 事务 Event 的生成和写入流程
3.3 MySQL 层事务提交流程简析（FLUSH→SYNC→COMMIT 阶段流程）
3.4 基于 WRITESET 的并行复制方式（WRITESET生成/last commit处理/WRITESET_SESSION/缺点）
3.5 主库的 DUMP 线程（POSITION MODE vs GTID AUTO_POSITION MODE/流程解析）
3.6 DUMP 线程查找和过滤 GTID 的基本算法

### 第4章 从库
4.1-4.2 从库 MTS 多线程并行回放（一/二）——协调线程分发机制/工作线程/检查点流程
4.3 MTS 中的 "gap" 测试和参数 slave_preserve_commit_order
4.4 从库的 I/O 线程
4.5 从库的 SQL 线程（MTS 协调线程）和参数 sql_slave_skip_counter
4.6 从库数据的查找和参数 slave_rows_search_algorithms
4.7 从库的关闭和异常恢复流程
4.8 安全高效的从库设置
4.9 Seconds_Behind_Master 的计算方式
4.10 Seconds_Behind_Master 延迟场景归纳

### 第5章 案例解析
5.1 线程简介和 MySQL 调试环境搭建
5.2 MySQL 排序详细解析（8个阶段：确认字段→计算长度→最大内存→排序→OPTIMIZER_TRACE）
5.3 MySQL 中的 MDL Lock 简介
5.4 奇怪的 FTWRL 堵塞案例
5.5 产生大量小 relay log 故障案例
5.6 从库 system lock 状态原因简析
