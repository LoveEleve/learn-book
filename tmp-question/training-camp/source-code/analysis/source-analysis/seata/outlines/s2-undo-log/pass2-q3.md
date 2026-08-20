# 闭环笔记 q3: 镜像采集 — before/after + 主键守卫

## 假设
镜像 = 业务 SQL 前后各查一次 (行锁保证一致性); UPDATE 主键被改有显式守卫。

## 验证过程
- **beforeImage 采集**: SelectForUpdateExecutor — **SELECT ... FOR UPDATE** (L85-94,137-152) — **行锁先锁再查** (Phase1 期间锁定目标行, 防并发改)
- **afterImage 采集**: 业务 SQL 执行后 buildTableRecords 再查 (BaseTransactionalExecutor:527-530 "SELECT ") — 与 before 同结构
- **prepareUndoLog** (BaseTransactionalExecutor:403-422): 双镜像全空 → 跳过; **UPDATE 时 before.size != after.size → ShouldNeverHappenException "probably because you updated the primary keys"** (L408-411) — 主键被改守卫 (UPDATE 前后行数必等, 除非 PK 变化)
- **lockKey 选择** (L415): **DELETE ? beforeImage : afterImage** — DELETE 锁删除前的行 (行已不在 after), 其他锁 after — 全局锁键 (S-12 交叉)
- **appendUndoLog** (L420): SQLUndoLog 入 ConnectionContext — 本地事务提交时统一 flush (q1)
- **SQLUndoLog 结构** (SQLUndoLog:27-124): sqlType/tableName/beforeImage/afterImage; setTableMeta 同时给双镜像 (L44-51)

## 代码类型
Implementation (镜像采集)

## 跨域关联
- S-4: ExecuteTemplate 调用链 (本域被调用)
- S-12: lockKey 采集 (全局锁键来源)
- S-10: SQL 识别器 (sqlRecognizer 判定 SQLType)

## 结论
镜像 = SELECT FOR UPDATE 前置锁 + 业务执行 + 再查; UPDATE 主键被改守卫; lockKey = DELETE 取 before。
源码位置: BaseTransactionalExecutor.java:403-422,527-530; SelectForUpdateExecutor.java:85-152; SQLUndoLog.java:27-124
