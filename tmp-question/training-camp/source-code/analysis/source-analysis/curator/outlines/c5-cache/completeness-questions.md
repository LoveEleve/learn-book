# C-5 缓存与监听 — completeness-questions (全视角提问验证)

## 开发者视角

1. NodeCache.start(true) 和 start(false) 区别? rebuild() 什么时候用?
2. NodeCache 监听回调有事件类型吗? 怎么知道是增还是改?
3. PathChildrenCache 的 StartMode 三种模式什么时候用?
4. getCurrentData() 返回顺序? 按什么排序?
5. TreeCache 的 maxDepth 怎么设置? maxDepth=0/1 分别等于什么?
6. CuratorCache.build(client, path, options...) 的 options 有哪些?
7. CuratorCache 的 listener 怎么只监听创建事件?
8. 旧三件套的监听器能用到 CuratorCache 上吗?

## 架构师视角

9. PathChildrenCache 为什么用双 watcher? children 和 data 分开挂有什么好处?
10. 一次性 watcher 重挂的竞态: 事件后、重挂前的变更会丢吗?
11. mzxid 为什么能防乱序? 后台回调乱序到达怎么办?
12. TreeCache 的 DEAD 哨兵解决什么问题? 为什么非根节点禁止 dead→live?
13. CuratorCache 为什么能 O(1) watcher? 持久 watcher 是什么?
14. cversion 差分怎么替代 children 事件? 父子节点事件丢失会怎样?
15. INITIALIZED 的三种实现 (initialSet 哨兵/outstandingOps 计数/OutstandingOps 类) 各解决什么?
16. 断连重连后缓存怎么重新同步? FORCE_GET_DATA_AND_STAT 是什么?
17. 为什么说缓存不可能事务性同步? 写缓存数据为什么必须带版本?
18. CuratorCache 为什么用 version 而旧类用 mzxid?
19. bridge 是怎么做到 ZK<3.6 兼容的?

## 学生视角

20. 什么是 watcher? 什么是一次性 watcher?
21. 什么是缓存? 为什么缓存会过期/不一致?
22. 什么是哨兵对象? NULL_CHILD_DATA 干嘛的?
23. 什么是递归监听? 持久 watcher 和普通 watcher 区别?
24. 事件驱动 vs 轮询, 缓存刷新为什么用事件?
