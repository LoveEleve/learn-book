# S-12 全局锁体系 — completeness-questions (全视角提问验证)

## 开发者视角

1. LockMode? (4 值 FILE/DB/REDIS/RAFT)
2. lockKey 格式? (表:主键,分号分隔)
3. acquireLock? (检查-插入)
4. lockQuery? (只查不锁)
5. Locker 族? (行锁 4 + 分布式 3)
6. LockStatus? (Locked/Rollbacking)
7. 释放? (分支/全局两级)
8. 冲突? (dbXID != currentXID)

## 架构师视角

9. 为什么检查-插入两段? (先查冲突再写 — 防覆盖)
10. 为什么只查不锁? (lockQuery 不改变状态 — 查询语义)
11. 为什么模式 SPI? (存储可插拔)
12. 为什么 LockStatus.Rollbacking? (回滚中并发读感知)
13. 为什么默认不解锁? (防误解锁 — 人工校准, S-7)
14. 对照 ZK WriteLock? (事件驱动 vs 轮询重试)
15. 为什么无 RW 锁? (行锁简化 — 读不阻塞)
16. 为什么无锁超时? (靠事务终态)

## 学生视角

17. 什么是全局锁? (跨服务数据锁)
18. 什么是 rowKey? (表+主键复合键)
19. 什么是 lockQuery? (查可锁性)
20. 什么是检查-插入? (先查后写)
