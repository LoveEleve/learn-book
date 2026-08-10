# 《MySQL Concurrency》（MySQL 并发：锁与事务）—— 完整目录

> 作者：Jesper Wisborg Krogh | 18章 + 2附录、388页

---

### 第1章 介绍
- Why Are Locks Needed?（为什么需要锁）
- Lock Levels（锁级别）
- Locks and Transactions（锁与事务）
- Examples（示例）
- Prerequisites / Installing concurrency_book.generate Module
- Test Data: world / sakila / employees Schema
- Summary

### 第2章 监控锁与互斥量
- The Performance Schema
- Metadata and Table Locks
- Data Locks
- Synchronization Waits
- Statement and Error Tables
- The sys Schema
- Status Counters and InnoDB Metrics
- InnoDB Lock Monitor and Deadlock Logging
- InnoDB Mutexes and Semaphores
- Summary

### 第3章 监控 InnoDB 事务
- Information Schema INNODB_TRX
- InnoDB Monitor
- INNODB_METRICS and sys.metrics
- Summary

### 第4章 Performance Schema 中的事务
- Transaction Events and Their Statements
- Transaction Summary Tables
- Summary

### 第5章 锁访问级别
- Shared Locks（共享锁）
- Exclusive Locks（排他锁）
- Intention Locks（意向锁）
- Lock Compatibility（锁兼容性）
- Summary

### 第6章 高级锁类型
- User-Level Locks / Flush Locks / Metadata Locks
- Explicit Table Locks / Implicit Table Locks
- Backup Locks / Log Locks
- Summary

### 第7章 InnoDB 锁
- Record Locks and Next-Key Locks
- Gap Locks / Predicate and Page Locks
- Insert Intention Locks / Auto-Increment Locks
- Mutexes and RW-Lock Semaphores
- Summary

### 第8章 处理锁冲突
- Contention-Aware Transaction Scheduling (CATS)
- InnoDB Data Lock Compatibility
- Metadata and Backup Lock Wait Timeouts
- InnoDB Lock Wait Timeouts / Deadlocks
- InnoDB Mutex and Semaphore Waits
- Summary

### 第9章 减少锁问题
- Transaction Size and Age / Indexes
- Record Access Order / Transaction Isolation Levels
- Resource Partitioning / Disabling InnoDB Adaptive Hash Index
- Reducing Priority of Metadata Write Locks / Preemptive Locking
- Summary

### 第10章 索引与外键
- Primary vs. Secondary Indexes / Ascending vs. Descending
- Unique Indexes / Foreign Keys / DML & DDL Statement
- Summary

### 第11章 事务
- Transactions and ACID（原子性/一致性/隔离性/持久性）
- Impact of Transactions（Locks/Undo Logs/Group Commit）
- Summary

### 第12章 事务隔离级别
- Serializable / Repeatable Read / Read Committed / Read Uncommitted
- Summary

### 第13-18章 案例研究
- 第13章：刷新锁案例（症状/原因/构建/调研/解决方案/预防）
- 第14章：元数据锁与方案锁案例
- 第15章：记录级锁案例
- 第16章：死锁案例
- 第17章：外键案例
- 第18章：信号量案例

### 附录 A：参考资料
### 附录 B：MySQL Shell 模块
