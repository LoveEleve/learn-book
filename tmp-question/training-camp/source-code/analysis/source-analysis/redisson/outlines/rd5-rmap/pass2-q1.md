# 闭环笔记 q1: mapWriterFuture 三路 — 写操作与外部数据源的同步

## 假设
RMap 写操作 (put/putAll/delete) 经 mapWriterFuture 包装: Redis 成功后按 WriteMode 三选一 (write-behind 缓冲 / write-through 同步 / write-through 异步) 同步到 MapWriter。

## 验证过程
- `mapWriterFuture` (RedissonMap.java:659-698):
  - L664-670 **WRITE_BEHIND**: `future.whenComplete` → Redis 成功 && condition → `writeBehindTask.addTask(task)` — 缓冲异步写
  - L673-683 **WRITE_THROUGH 同步**: `future.thenCompose` → condition → `supplyAsync(() -> { Add→writer.write(map); delete→writer.delete(keys) }, executor)` — ServiceManager executor 同步
  - L686-692 **WRITE_THROUGH 异步**: `writerAsync.write/delete` — 异步接口
- condition 门控 (L666/674): `r -> true` 默认; 可自定义 (如条件写入)
- MapWriterTask.Add/delete 分派 (L677-681)
- WriteMode 枚举: WRITE_THROUGH (默认) / WRITE_BEHIND
- 触发时机: Redis 操作成功后 (不阻塞 Redis 本身)

## 代码类型
Glue (数据同步门控) — 写操作的双写编排

## 跨域关联
- RD-4 (命令) → future 来自 evalWriteAsync
- Q2 (writeBehind 缓冲) → addTask 进 WriteBehindService
- 数据一致性: Redis + 外部 DB 双写

## 结论
RMap 写 = Redis 命令 + MapWriter 双写编排: WRITE_BEHIND (缓冲异步) vs WRITE_THROUGH (同步/异步写通), condition 门控 Redis 成功才写外部。设计: Redis 是主存储, MapWriter 是"同步影子"。
源码位置: RedissonMap.java:659-698