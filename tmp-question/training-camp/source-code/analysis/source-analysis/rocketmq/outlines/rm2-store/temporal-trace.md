# RM-2 存储底层 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | 存储骨架定型: MappedFile (mmap) + MappedFileQueue (1GB 段序列) + CommitLog/ConsumeQueue 分离; 刷盘 SYNC/ASYNC 二服务 (GroupCommit/FlushRealTime); 写锁 ReentrantLock |
| 4.x | **TransientStorePool** (堆外池 + mlock — 默认关闭); AllocateMappedFileService 预分配 (mmap 预热防卡顿); PutMessageSpinLock (自旋锁, 低竞争建议); warmMappedFile |
| 5.x | **FlushManager 接口化** (DefaultFlushManager 替代直接依赖); **FlushDiskWatcher** (刷盘监控); **MultiPathMappedFileQueue** (多盘); **isLoaded0 页加载统计** (反射); ServiceLoader 加载 MappedFile 自定义实现 (扩展点); metrics |

## 痕迹证据

- DefaultMappedFile.java:83-95: 双缓冲字段注释 ("Message will put to here first, and then reput to FileChannel if writeBuffer is not null")
- DefaultMappedFile.java:118-121: isLoaded0 反射 (JDK 链接注释 — 5.x 页统计)
- CommitLog.java:1647-1653: 组提交 sleep 1ms 注释 (池启用 commit 等待)
- CommitLog.java:2118-2131: DefaultFlushManager 构造选择注释 (SYNC/ASYNC + 池启动转储)
- TransientStorePool.java:44: "It's a heavy init method" (mlock 重初始化)
- AllocateMappedFileService.java:190-194: >10ms 映射告警
- PutMessageSpinLock.java:22: "suggest using this with low race conditions"

## 推断标注

- "3.x 骨架定型" — RocketMQ 公知版本线 (标注)
- "4.x 池/预分配/自旋锁" — 特性年代推断 (标注)
- "5.x 接口化/Watcher/多盘" — 代码结构 + 5.x 模块推断 (标注)
- 未做 git 考古, 版本线为代码结构推断, 已逐条标注
