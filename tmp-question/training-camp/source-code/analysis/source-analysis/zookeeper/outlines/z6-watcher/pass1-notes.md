# Z-6 Watcher — Pass 1 探索笔记

> 域: Z-6 Watcher | 🔴 A 方案 | 2026-08-15
> 源码: server/watch/ WatchManager (376) + WatchManagerOptimized (412) + WatchManagerFactory + IWatchManager + WatcherMode + WatcherOrBitSet + BitMap/BitHashSet + ServerCnxn.process | ZooKeeper 3.9.5

## 调用图

```
注册: 客户端 getData/getChildren (Z-7) → DataTree.getData/getChildren → watchManager.addWatch
触发: DataTree.triggerWatch (Z-3: createNode/deleteNode/setData)
  → WatchManager.triggerWatch: PathParentIterator 迭代 + STANDARD 移除
  → watcher.process → ServerCnxn.process → 事件入队 → 客户端投递

清理: 会话关闭 → removeWatcher (标准全清) / deadWatchers 懒清理 (Optimized)
```

## 基本元素分解

1. **双实现**: WatchManager (HashMap 双向) vs WatchManagerOptimized (位图 + RWLock + 懒清理)
2. **双向注册**: watchTable + watch2Paths + 死 watcher 守卫
3. **触发链**: PathParentIterator + 一次性移除 + suppress + 分发
4. **WatcherMode**: STANDARD/PERSISTENT/PERSISTENT_RECURSIVE + WatchStats

## 标记问题 (20 问)

1. 双实现差异? (位图/RWLock/懒清理)
2. watchTable 结构? (path → Set)
3. watch2Paths? (watcher → Map<path, WatchStats>)
4. 初始容量? (HashSet(4))
5. 死 watcher? (cnxn 关闭)
6. removeWatcher(watcher)? (全清)
7. removeWatcher(path, watcher)? (单路径)
8. triggerWatch 迭代? (PathParentIterator)
9. STANDARD 移除? (触发即删)
10. PERSISTENT_RECURSIVE? (父路径保持)
11. suppress? (去重)
12. watcherBitIdMap? (位图)
13. RWLock 语义? (读锁 add / 写锁 remove)
14. 懒清理? (deadWatchers → trigger 时删)
15. WatcherMode 枚举? (3 模式 + DEFAULT)
16. WatchStats? (路径级模式)
17. WatchManagerFactory? (可插拔)
18. ServerCnxn.process? (事件投递)
19. 触发 metrics? (NODE_CREATED_WATCHER 等)
20. DataTree 触发点? (Z-3 双/三 watch)

## 时空溯源 (代码内注释锚)

- ZOOKEEPER-2251: WatcherMode (PERSISTENT/PERSISTENT_RECURSIVE — 3.6+)
- WatchManagerOptimized: 3.5+ 性能优化 (位图 + 懒清理注释 L47-48,200-201)
- HashSet(4) 注释 (L83-85): 内存妥协

## 大域拆分判断

Z-6 = watch 包 (双实现 + 模式 + 触发); 单篇 🔴 A (8 闭环 q1-q4 + 验证); 事件投递细节归 Z-7 (仅引用)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划) | 验证 | 结论 |
|:--|:--|:--|
| "watchTable+watch2Paths双向" | WatchManager L50-52 实证 | **接受** ✅ |
| "WatchStats位掩码(3模式)" | WatcherMode 3 模式 + WatchStats addMode/removeMode | **接受+补充**: PERSISTENT_RECURSIVE 递归面 | ✅ |
| 数字: 模式 | STANDARD/PERSISTENT/PERSISTENT_RECURSIVE (3) + DEFAULT | **补充** ✅ |
| 数字: 初始容量 | HashSet(4) 4th 翻倍 | **补充** ✅ |
| 双实现 (09 修正) | WatchManagerOptimized 412 行 | **已修正** (PLAN) | ✅ |
