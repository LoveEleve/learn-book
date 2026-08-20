# 闭环笔记 q2: 双向注册 — watchTable + watch2Paths

## 假设
path→watchers (触发) + watcher→paths (清理) 双向一致。

## 验证过程
- **addWatch** (WatchManager:75-110): isDeadWatcher 忽略 (L76-78) → watchTable.get → **HashSet(4) 懒创建** (注释 L83-85: "rehash when the 4th entry is added, doubling size thereafter" — 内存妥协) → watch2Paths (WatchStats 合并: addMode) + recursiveWatchQty 计数 (L103-105)
- **removeWatcher(watcher)** (L113-132): watch2Paths.remove (拿全部路径) → **watchTable 反向清理** (逐路径 list.remove + 空路径 watchTable.remove) → recursiveWatchQty 递减 (L127-131)
- **removeWatcher(path, watcher)** (Optimized:130-138): 单路径移除 (writeLock)
- **isDeadWatcher** (L76-78): cnxn.isStale — 防死连接添加后永不清理

## 代码类型
Implementation (双向索引)

## 跨域关联
- Z-7: ServerCnxn.isStale (死连接判定)

## 结论
双向索引: 触发查 watchTable / 清理查 watch2Paths; HashSet(4) 懒创建内存妥协; 死 watcher 守卫。
源码位置: WatchManager.java:75-132; WatchManagerOptimized.java:130-138
