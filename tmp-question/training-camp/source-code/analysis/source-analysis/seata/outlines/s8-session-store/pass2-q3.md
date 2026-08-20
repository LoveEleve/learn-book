# 闭环笔记 q3: 存储实现 — DB 13 方言 + Redis Lua + File

## 假设
三种持久化实现: DB 表 + Redis Lua 原子脚本 + File 序列化。

## 验证过程
- **DB 实现** (storage/db/store/DataBaseTransactionStoreManager + LogStoreDataBaseDAO): **global_table / branch_table / lock_table** 三表; **13 方言 SQL** (LogStoreSqls + LockStoreSql: mysql/oracle/pg/sqlserver/dm/kingbase/mariadb/oceanbase/oscar/polardbx/h2...)
- **GlobalTransactionDO 11 字段** (core/store/GlobalTransactionDO): xid/transactionId/status/applicationId/transactionServiceGroup/transactionName/timeout/beginTime/applicationData/gmtCreate/gmtModified — 会话表结构
- **ALL_GLOBAL_COLUMNS** (AbstractLogStoreSqls:22-30): 11 列查询
- **Redis 实现** (RedisLuaTransactionStoreManager + RedisLuaLocker): **Lua 脚本原子性** (防并发读写竞态 — 单脚本执行)
- **File 实现** (FileTransactionStoreManager + TransactionWriteStore): 序列化追加 + **FlushDiskMode** (刷盘模式) + restoreSessions (L195-210: 未处理分支缓冲恢复)
- **LockStore**: LockStoreDataBaseDAO — lock_table 行锁 (S-12 交叉)

## 代码类型
Implementation (存储实现)

## 跨域关联
- S-12: lock_table (行锁存储)
- S-7: restoreSessions (重启恢复)
- Z-9 (4.3): File 刷盘对照 (FlushDiskMode vs ZK forceSync)

## 结论
存储 = DB 三表 13 方言 / Redis Lua 原子 / File 序列化+刷盘; GlobalTransactionDO 11 字段定义表结构。
源码位置: storage/db|redis|file/store/; core/store/GlobalTransactionDO.java; db/sql/log|lock/ (13 方言)
