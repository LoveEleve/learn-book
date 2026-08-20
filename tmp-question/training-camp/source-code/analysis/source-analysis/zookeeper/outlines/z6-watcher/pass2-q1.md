# 闭环笔记 q1: 双实现 — WatchManager vs WatchManagerOptimized

## 假设
标准 HashMap 双向 vs 位图压缩 + 懒清理。

## 验证过程
- **WatchManager** (376): watchTable (path → Set<Watcher>) + watch2Paths (watcher → Map<path, WatchStats>) (L50-52) — synchronized 全方法
- **WatchManagerOptimized** (412): **pathWatches (ConcurrentHashMap<String, BitHashSet>, L61)** + **watcherBitIdMap (BitMap<Watcher>, L64)** — **watcher 位图**: 每 watcher 一个 bit id, 路径 watcher 集合 = 位集合 (内存压缩 + contains O(1))
- **RWLock**: addWatch readLock (L79-80 注释: 与 removeWatcher 互斥 — 防加死 watch) / removeWatcher writeLock (L154-155)
- **懒清理**: **IDeadWatcherListener + deadWatchers** (L200-201 注释: 避免每次 remove 锁竞争 — triggerWatch 时批量删)
- **工厂**: WatchManagerFactory (watchManagerClassName/isWatchManagerOptimized) — DataTree 构造 dataWatches/childWatches (Z-3)

## 代码类型
Implementation (双实现 + 优化)

## 跨域关联
- Z-3: dataWatches/childWatches 双管理器 (分离数据/子节点 watch)

## 结论
双实现: 标准 (synchronized 全锁) vs Optimized (位图压缩 + RWLock + 懒清理) — 高连接数场景性能关键。
源码位置: WatchManager.java:46-52; WatchManagerOptimized.java:57-64,77-94,200-201
