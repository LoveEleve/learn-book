# Z-6 Watcher — completeness-questions (全视角提问验证)

## 开发者视角

1. watch 存哪? (watchTable path→watchers + watch2Paths watcher→paths)
2. 触发一次还有吗? (STANDARD 触发即移除)
3. 两种实现差在哪? (HashMap 双向 vs 位图压缩)
4. WatcherMode 有哪些? (STANDARD/PERSISTENT/PERSISTENT_RECURSIVE)
5. 会话关闭怎么清? (removeWatcher 全清 / Optimized 懒清理)
6. 死 watcher 是什么? (cnxn 已关闭)
7. suppress 干什么? (deleteNode 双触发去重)
8. 递归 watch? (父路径迭代)

## 架构师视角

9. 为什么位图? (watcher 集合内存压缩 + contains O(1))
10. 为什么一次性? (事件不排队 — 客户端需重查; 简化语义)
11. 双向索引意义? (触发查询 + 清理查询分离)
12. RWLock vs synchronized? (读多写少 — 触发性能)
13. 懒清理换什么? (锁竞争 vs 内存暂留)
14. 递归模式代价? (父路径全迭代)
15. 对照 Redis 发布订阅? (ZK 有状态注册 vs Redis 无状态广播)
16. 为什么不持久事件? (简化 — 客户端重查语义)

## 学生视角

17. 什么是 watch? (监听节点变化, 变化时通知)
18. 一次性? (通知一次就失效)
19. 什么是递归 watch? (子节点变化也触发)
20. 事件怎么到达? (ServerCnxn → 客户端)
