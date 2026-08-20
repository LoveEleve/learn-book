# 闭环笔记 q4: WatcherMode — 三模式 + 会话清理

## 假设
触发语义参数化; 会话关闭清理双路径。

## 验证过程
- **WatcherMode 枚举** (WatcherMode.java:23-29): **STANDARD(false,false) / PERSISTENT(true,false) / PERSISTENT_RECURSIVE(true,true)** + DEFAULT_WATCHER_MODE=STANDARD (L29) + fromMode/toMode (L30-40)
- **WatchStats**: 路径级模式状态 — addMode/removeMode 合并 (watch2Paths 值); WatchStats.NONE 移除条件 (WatchManager:162-167)
- **会话关闭清理**: 标准 **removeWatcher(watcher) 全清** (watch2Paths 一次拿全部); Optimized **deadWatchers 懒清理** (会话/连接关闭 → addDeadWatcher → triggerWatch 时批量 — 注释 L47-48 "efficiently remove the watcher when the session or socket is closed")
- **WatchManagerFactory**: 工厂创建 (watchManagerClassName 可插拔 / isWatchManagerOptimized) — 配置切换
- **递归计数**: recursiveWatchQty (L103-105,127-131) — 递归 watch 统计

## 代码类型
Implementation (模式 + 清理)

## 跨域关联
- Z-5: 会话关闭 → ServerCnxn.close → removeWatcher
- Z-7: addWatch API (addWatch mode 参数 — 4.0 协议 addWatch 包)

## 结论
模式三档 (STANDARD/PERSISTENT/PERSISTENT_RECURSIVE); 会话清理双路径 (立即全清 vs 懒批量); 工厂可插拔。
源码位置: WatcherMode.java:23-46; WatchManager.java:127-131,162-167
