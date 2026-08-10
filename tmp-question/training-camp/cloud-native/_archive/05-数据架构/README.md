# 05 数据架构（Week 10-11 · 4课时）

## 学完能干什么
搞定生产环境数据层的三大核心：读写分离、分库分表、缓存策略。

## 课时 24：MySQL 主从 + 读写分离

**📖 读**: `stage-2/docs/26. 第二十五节：通用数据读写分离设计.md`（全文）

**MySQL 主从同步类型**:

| 类型 | 一致性 | 性能 | 适用 |
|------|:----:|:----:|------|
| 异步 | 可能丢数据 | 最高 | 读写分离 |
| 半同步 | 至少一个 Slave 确认 | 中等 | 数据安全 |
| 全同步 | 全部确认 | 最低 | 金融核心 |

**Docker 搭建**（文档提供完整命令）：
```bash
# Master: server_id=1, log_bin=ON, binlog_format=ROW, gtid_mode=ON
# Slave: server_id=2, read_only=ON
# CHANGE MASTER TO MASTER_HOST='...', MASTER_USER='repl'
```

**读写分离三种策略**：Java 方法切换 / 注解切换（`@Switchable` AOP）/ SQL 分析切换

## 课时 25：ShardingSphere 分库分表

**📖 读**: `stage-2/docs/27.`

**双形态**：ShardingSphere-JDBC（嵌入应用，轻量）vs ShardingSphere-Proxy（独立进程，透明代理）

**分片策略**：Standard（精确分片 =/IN）/ Complex（多键）/ Hint（强制路由）

## 课时 26：缓存策略

**📖 读**: `stage-2/docs/28.` + `29.`

**缓存设计**：命中率门槛 ≥50%、LRU/TTL/布隆过滤器、多级缓存（客户端→本地→分布式）

**Redisson 锁体系**：RLock 可重入 → RedissonFairLock（zset公平锁）→ ReadWriteLock（读写锁）→ RedissonSpinLock（自旋锁）→ RedissonSemaphore（信号量）

**TTLCacheResolver**（microsphere-spring-context）：ThreadLocal + try-finally 模式传递 TTL，@TTLCacheable 注解自动设置过期时间

## 课时 27：Shopizer SQL 优化

**📖 读**: `stage-3/docs/11.` + `12.`

**MySQL MGR**：基于 Paxos 的分布式状态机复制，单主/多主模式，冲突检测"首次提交获胜"，容错 n=2f+1

## 本阶段自检清单
- [ ] 能独立搭建 Docker MySQL 主从
- [ ] 知道读写分离的三种实现策略及取舍
- [ ] 理解 ShardingSphere 分库分表的路由算法
- [ ] 知道 Redisson RLock 怎么用 Redis HASH 实现可重入
