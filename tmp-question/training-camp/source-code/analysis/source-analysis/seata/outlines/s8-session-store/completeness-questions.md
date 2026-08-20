# S-8 Session 存储 — completeness-questions (全视角提问验证)

## 开发者视角

1. SessionMode 有几种? (4: FILE/DB/REDIS/RAFT)
2. 怎么选? (StoreConfig + SPI 加载)
3. 生命周期怎么落库? (6 写操作)
4. onFailEnd? (解锁开关默认关)
5. lockAndExecute? (三模式)
6. GlobalSessionLock? (ReentrantLock 2s)
7. 存储实现? (DB 三表/Redis Lua/File)
8. 方言? (13)

## 架构师视角

9. 为什么 SPI 模式路由? (存储可插拔 — 单机到集群)
10. 为什么 DB 模式无本地锁? (多节点共享 — 本地锁无意义)
11. 为什么 Redis 用 Lua? (原子性 — 防并发竞态)
12. 为什么 onClose setActive(false)? (isEndStatus 驱动 — 会话结束标志)
13. 为什么状态联动锁标记? (Rollbacking 时锁状态同步)
14. 对照 ZK? (ZK 会话服务端管理 vs Seata TC 存储)
15. 为什么 FILE 是单机语义? (JVM 锁 — 多节点不安全)
16. 为什么 reload? (重启恢复 — 状态驱动续跑)

## 学生视角

17. 什么是 SessionMode? (会话存储模式)
18. 什么是会话管理? (TC 的会话增删改查)
19. 什么是 lockAndExecute? (带锁的执行)
20. 什么是 Lua 原子性? (Redis 脚本单线程执行)
