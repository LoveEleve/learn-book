# 闭环笔记 q5: 读写分离路由 — ReadMode 三态 + 双连接池

## 假设
MasterSlaveEntry 持有 master/slave 两套连接池; 读写路由由 ReadMode 决定: 写永远走 master 池, 读按模式走 master 或 slave 池。

## 验证过程
- MasterSlaveEntry.java:569-576 (connectionWriteOp): `masterConnectionPool.get(command, false)` — **写固定 master 池**
- L585-604 (connectionReadOp): `mode = override ?? config.getReadMode()`; **ReadMode.MASTER → 走 write 池** (trackChanges 时 trackedConnectionWriteOp); 否则 `slaveConnectionPool.get(command, trackChanges)` — 读默认 slave 池
- ReadMode.java:26-40: `SLAVE` ("Read from slave nodes. Uses MASTER if no SLAVES available") / `MASTER` / `MASTER_SLAVE` — 三态枚举
- 池结构 (ConnectionsHolder): allConnections (ConcurrentLinkedQueue) + freeConnections (ConcurrentLinkedDeque) + `freeConnectionsCounter = AsyncSemaphore(poolMaxSize, eventLoopGroup)` (L44-59); acquireConnection L224 (取 free → 空则 semaphore 控新建), releaseConnection L263
- slave 池无 slave 时的降级: SLAVE 模式 "Uses MASTER if no SLAVES" — slaveConnectionPool 内部无连接时回退 master (pool.get 逻辑)
- Cluster 多 entry 轮询: rrCounter (MasterSlaveConnectionManager:74) → getEntry floorMod 轮询 (L186)
- ConnectionPool balancer: balancer/ 包 (RoundRobinBalancer) 做 slave 间负载均衡

## 代码类型
Interface (路由契约) — ReadMode 是客户端可配置面

## 跨域关联
- RD-4 (CommandAsyncService readAsync/writeAsync) → 消费 connectionReadOp/WriteOp
- Q4 (DNSMonitor 切换) → 影响池成员
- Redis R-2/R-28 (服务端 io 线程) → 对照: 服务端单线程 vs 客户端多连接池

## 结论
读写分离 = ReadMode 三态 (SLAVE/MASTER/MASTER_SLAVE) 在 connectionReadOp 决策; 写固定 master 池, 读默认 slave 池 (无 slave 降级 master); 池容量由 AsyncSemaphore(poolMaxSize) 控制; slave 间 RoundRobin 均衡, Cluster 多 entry floorMod 轮询。
源码位置: MasterSlaveEntry.java:569-604, ConnectionsHolder.java:44-59,224,263, ReadMode.java:26-40
