# 《MySQL 实战》—— 完整目录

> 作者：陈臣 | 12章 + 附录 | 大量实操命令、原理图、线上故障方案

---

### 第1章 MySQL 入门、安装与服务的管理
1.1 MySQL 发展历史与各版本特性
1.2 MySQL 多方式安装（官方安装包/二进制包/源码编译）
1.3 MySQL 服务两种主流管理方案（/etc/init.d传统脚本/systemd服务单元配置）
1.4 MySQL 初始化、密码校验组件配置
1.5 启动失败经典故障排查三板斧
1.6 数据目录文件详解

### 第2章 MySQL 复制核心原理与基础搭建
2.1 复制整体架构（binlog dump/IO线程/SQL线程流程图解）
2.2 传统位点复制搭建完整流程
2.3 GTID 全局事务复制（格式/gtid_executed/gtid_purged/gtid_mode四阶段/实用函数/限制与8.0优化）
2.4 半同步复制（AFTER_SYNC vs AFTER_COMMIT/两阶段提交/插件安装/超时降级）
2.5 并行复制演进方案（COMMIT_ORDER/WRITESET/调优参数与压测对比）
2.6 多源复制（channel通道配置/GTID环境多源导入冲突处理）
2.7 延迟复制（搭建步骤/适用场景/结合binlog找回丢失表）
2.8 复制日常管理、主从延迟成因排查

### 第3章 深入解析 Binary Log（binlog）
3.1 binlog 三种存储格式：STATEMENT/ROW/MIXED 优缺点对比
3.2 解析 binlog 内容（mysqlbinlog解析statement/row格式）
3.3 relay log 日志读取方法
3.4 binlog 全部事件类型详解
3.5 基于 python-mysql-replication 自研 binlog 解析消费工具
3.6 binlog 清理、归档实操方案

### 第4章 复制运维、延迟与故障排查
4.1 复制日常管理常用操作
4.2 Seconds_Behind_Master 延迟计算逻辑
4.3 主从延迟多类根因分析（SQL单线程/无索引/大事务/锁等待）
4.4 复制高频故障与修复方案（server_id重复/数据包超限/binlog缺失/GTID不匹配/主键冲突/DDL报错）
4.5 主从数据不一致定位与修复

### 第5章 MySQL 全量备份与恢复工具实战
5.1 mysqldump 逻辑备份全参数详解、搭建从库实操
5.2 mydumper 高性能并行备份工具用法
5.3 XtraBackup 物理热备份原理（完整备份/增量备份/搭建从库/指定时间点恢复）
5.4 MySQL Shell Dump & Load 跨版本迁移
5.5 binlog server 搭建，归档日志节省主库磁盘
5.6 备份有效性校验方法

### 第6�� MySQL 监控体系（Zabbix + PMM）
6.1 Zabbix 监控 MySQL 模板、告警规则配置
6.2 PMM 完整部署（架构/Query Analytics慢查询分析/自定义告警/常见问题排错）
6.3 MySQL 核心监控指标分类（连接/语句执行/临时表/表缓存/磁盘IO/缓冲池/redo/锁/复制）

### 第7章 DDL 变更与在线表修改方案
7.1 MySQL 原生 Online DDL 原理、优缺点、限制
7.2 pt-online-schema-change 工具完整实操（底层实现/参数调优/避免锁表）
7.3 DDL 阻塞元数据锁(MDL)定位方法（5.6/8.0不同排查手段）
7.4 大表 DDL 变更线上落地规范

### 第8章 连接池、线程池原理与选型
8.1 JDBC 连接池底层运行原理（c3p0/DBCP/HikariCP对比与生产配置模板）
8.2 MySQL 线程池（Percona Server/企业版/适用场景/压测结果）

### 第9章 MySQL 基准测试与 Percona Toolkit 工具集
9.1 sysbench 压测全套实操（安装/通用压测脚本/自定义业务脚本/服务器IO混合压测/结果分析）
9.2 pt 系列高频运维工具完整用法（pt-archive/pt-config-diff/pt-show-grants/pt-stalk/pt-table-checksum/pt-table-sync/pt-upgrade等，含原理/参数/线上故障实操案例）

### 第10章 中间件 ProxySQL 完整运维
10.1 Proxy 核心架构、读写分离配置
10.2 SQL 重写、黑名单、流量镜像(mirroring)
10.3 ProxySQL 集群部署、高可用方案
10.4 多库路由、权限管理、日常运维命令

### 第11章 MySQL 组复制 Group Replication
11.1 组复制环境部署（单主/多主模式）
11.2 单主、多主模式差异、业务适配场景
11.3 XCom 通信协议原理
11.4 分布式恢复机制、节点上下线管理
11.5 事务冲突检测 write_set 实现细节
11.6 网络分区故障自动检测、集群恢复
11.7 事务一致性级别参数调优

### 第12章 InnoDB Cluster 高可用集群
12.1 MySQL Router 路由工具部署与配置
12.2 InnoDB Cluster 完整搭建流程
12.3 集群日常管理：新增/删除节点、故障自动切换
12.4 集群升级、故障恢复实操

### 附录
全书常用生产参数模板、线上故障排查流程、命令速查表
