# MySQL-数据库 — 知识点规划

> 方法论: knowledge-planning/methodology/01-03 | TOC-only, 04 跳过
> 主题: MySQL | 14 本书 | [No P1 — 14 books highly specialized across sub-domains]

---

## 01 提取 + 02 深度 + 03 聚类

### 书籍 (14 本)

MySQL内核设计与实现 / MySQL是怎样运行的 / 数据库内核揭秘 / 深入浅出存储引擎 / MySQL-8查询性能优化 / 大数据SQL优化原理与实践 / 千金良方金字塔法则 / MySQL高可用解决方案 / MySQL复制技术与生产实践 / 深入理解MySQL主从原理 / MySQL-Concurrency / MySQL实战 / DBA实战手记 / HikariCP连接池实战

---

### 🔴 Deep (20 项)

| Knowledge Point | Pri | Books |
|----------------|:---:|:---:|
| Buffer Pool (LRU/free/flush/checkpoint/预读) | P2 | 5 |
| Redo Log (MTR/block/LSN/刷盘/崩溃恢复) | P2 | 4 |
| Undo Log (回滚段/insert_undo/update_undo/purge) | P2 | 3 |
| B+Tree 索引原理 (聚簇/二级/复合/页分裂合并) | P2 | 5 |
| 索引使用 (最左前缀/覆盖索引/回表/索引下推/MRR) | P2 | 3 |
| MVCC + ReadView (版本链/可见性/trx_id) | P2 | 3 |
| 隔离级别实现 (脏读/不可重复/幻读+InnoDB做法) | P2 | 3 |
| InnoDB 锁全类型 (记录/间隙/临键/意向) | P2 | 5 |
| 死锁检测与管理 | P2 | 5 |
| EXPLAIN 解读 (type/key/rows/Extra/JSON) | P2 | 2 |
| CBO 成本模型 (单表/连接代价/Condition Filtering) | P2 | 4 |
| 查询改写 (子查询/外连接消除/CTE/窗口函数) | P2 | 3 |
| 主从复制 (binlog格式/GTID/半同步) | P2 | 6 |
| 并行复制 (单线程→DATABASE→LOGICAL_CLOCK→WRITESET) | P2 | 3 |
| Seconds_Behind_Master (源码解读+场景分类) | P2 | 2 |
| 主从故障转移+数据不一致修复 | P2 | 3 |
| MGR 组复制 (Paxos/GCS/冲突检测/分布式恢复) | P2 | 2 |
| 慢查询分析 (slow log/P_S/sys 三工具链) | P2 | 3 |
| 性能测试金字塔法 (硬件→MySQL→架构) | P3 | 1 |
| ConcurrentBag + FastList (无锁连接池) | P3 | 1 |

### 🟡 Working (28 项)

InnoDB架构/Doublewrite/Change Buffer/Adaptive Hash/Row format/Data page/Tablespace/File org/B+Tree macro/Index statistics/Histogram/Hash join/optimizer trace/Data type performance/Lock monitoring/MDL/CATS/减少锁竞争/DDL优化/Online DDL/binlog Event/Event troubleshooting/Relay log/MTS/DUMP thread/I/O thread/多源复制/过滤/MGR优化/Cluster/ClusterSet/Router/ReplicaSet/复制拓扑/故障排除/生产排故案例/死锁案例/加锁验证/HikariCP关键参数/监控/故障排查/ProxySQL/8.0语法/binlog自研/大表DDL规范/排序详解

### 🟢 Surface (~31 项)

安装/升级/历史连接池/MEM/Workbench/Zabbix+PMM/Backup工具/sysbench/FIO/Percona/Flashback/OGG/HTAP/数据库选型/CAP/哈希算法/B-link/OLFIT/LSM/LevelDB/数据库数学/参数模板

---

### 聚类 — 6 组

```
A(存储引擎:8) → B(索引查询:10) → C(事务锁+ConcurrentBag:9)
                                     ↓
                                D(复制HA:12) → Ea(性能诊断:6) → Eb(连接运维:7)
```

**A InnoDB 存储引擎**: BufferPool→Redo→Undo→Doublewrite→Row/Page/Tablespace→File org
**B 索引与查询**: B+Tree→索引使用→统计/直方图→CBO→EXPLAIN→查询改写→HashJoin→CTE→trace
**C 事务锁与并发**: MVCC→隔离级别→锁全类型→死锁→MDL→锁监控→减少锁竞争→案例→ConcurrentBag
**D 复制与高可用**: 主从→GTID→半同步→并行复制→故障转移→MGR→Cluster→Router
**Ea 性能诊断**: 慢查询→EXPLAIN深入→perf schema→性能金字塔→DDL优化→排序
**Eb 连接运维**: HikariCP→ProxySQL→大表DDL→生产排故→binlog自研→排障

---

### Summary

| 深度 | 计数 |
|------|:---:|
| 🔴 Deep | 20 |
| 🟡 Working | 28 |
| 🟢 Surface | ~31 |
| **Total** | **~79** |
