# Z-6 Watcher — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.x | WatchManager 骨架: watchTable/watch2Paths 双向 + synchronized 全锁 + 一次性触发 |
| 3.5.x | **WatchManagerOptimized**: 位图压缩 (watcherBitIdMap) + RWLock + deadWatchers 懒清理 (L47-48,200-201 注释) — 高连接数优化 |
| 3.6.x | **WatcherMode** (ZOOKEEPER-2251): PERSISTENT/PERSISTENT_RECURSIVE — 持久 watch 语义 (WatchMode 注释风格) |
| 3.9.x | WatchStats 细化 (addMode/removeMode); WatcherOrBitSet suppress; 触发 metrics 分类 |

## 痕迹证据

- WatchManager.java:83-85: HashSet(4) 4th 翻倍注释 (3.4 风格)
- WatchManagerOptimized.java:47-48: "efficiently remove the watcher when the session or socket is closed" (3.5 锚)
- WatchManagerOptimized.java:200-201: 懒清理注释 (避免锁竞争)
- WatcherMode.java:23-29: 三模式枚举 (3.6 锚, ZOOKEEPER-2251)
- WatchManagerFactory: 可插拔 (3.5+)

## 推断标注

- "3.4.x 骨架" — 公知版本线 (watch 机制 3.4 定型) (标注)
- "3.5.x Optimized" — 位图优化年代推断 (标注); 高连接数问题 3.5 场景
- "3.6.x WatcherMode" — ZOOKEEPER-2251 编号年代推断 (标注)
- git shallow (1 commit) — 无考古, 全注释锚
