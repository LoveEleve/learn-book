# N-15-03 配置存储面 — 缓存与转储 (缓存/Dump 篇)

> 前置: N-15-01/02 (持久化/操作) | 对照: 内存缓存 + 磁盘转储双层
> 🔴 A | 方案 A (全深度) | 闭环: q1(缓存面) q2(dump 任务族) q3(磁盘双实现)

**读者处境**: 配置的内存缓存怎么组织? dump (转储) 任务怎么调度? 磁盘读写怎么支持 RocksDB?

### 1. 缓存面 — ConfigCacheService 与 CacheItem

场景: 配置缓存怎么组织?
源码路径:
- **ConfigCacheService** (service/ConfigCacheService.java:53): **ConcurrentHashMap CACHE** (L66: groupKey → CacheItem) + **dumpWithMd5** (L84-87: md5 校验写缓存 + 写锁失败日志)
- **CacheItem** (model/CacheItem.java:32): 缓存项 (内容/md5/时间戳)
- **ConfigCacheFactory** (model/ConfigCacheFactory.java:24) + **ConfigCache** (model/ConfigCache.java:29) + **ConfigCacheGray** (model/ConfigCacheGray.java:30): 缓存工厂与灰度缓存
- **ConfigCachePostProcessor** (model/ConfigCachePostProcessor.java:24): 缓存后处理器
关键设计 (q1): **"md5 门控缓存 = 内容一致性"** — dumpWithMd5 带 md5 比对; 缓存项含灰度变体。 [模式: md5 门控缓存]

### 2. Dump 任务族 — DumpProcessor 与任务链

场景: 配置怎么从存储到内存?
源码路径:
- **DumpProcessor** (dump/processor/DumpProcessor.java:40): implements NacosTaskProcessor — **processDump 处理 DumpTask** (L54)
- **DumpConfigHandler** (dump/DumpConfigHandler.java:33): dump 处理器 (distro 消费)
- **任务族** (dump/task/): DumpTask / DumpAllTask / DumpAllBetaTask / DumpAllTagTask / DumpAllGrayTask — 单条与全量
- **处理器族** (dump/processor/): DumpProcessor / DumpAllProcessor / DumpAllGrayProcessor
- **DumpTaskExecuteEngine**: 转储执行引擎 (distro 变更 → dump 任务)
关键设计 (q2): **"dump 任务族 = 变更驱动转储"** — 配置变更 → DumpTask → 内存缓存更新; 全量转储 (启动/定时)。 [模式: 变更驱动转储]

### 3. 磁盘双实现 — ConfigDiskService 族

场景: 磁盘读写怎么支持?
源码路径:
- **ConfigDiskService** (dump/disk/ConfigDiskService.java:26): 接口 — writeToDisk/readFromDisk/clearConfig
- **ConfigRawDiskService** (disk/ConfigRawDiskService.java:42): 原生文件实现
- **ConfigRocksDbDiskService** (disk/ConfigRocksDbDiskService.java:44): **RocksDB 实现**
- **ConfigDiskServiceFactory** (disk/ConfigDiskServiceFactory.java:24): 工厂选择
关键设计 (q3): **"磁盘双实现 = 文件/RocksDB 可选"** — 内嵌部署用 RocksDB, 外置用文件; 工厂切换。 [模式: 存储双实现]

### 4. 测试与行为锚

场景: 缓存/dump 边界?
源码路径:
- 测试: ConfigCacheServiceTest / DumpProcessorTest (config test)
- 日志锚: "[dump-error] write lock failed" (L92)
关键设计 (q1): **"写锁失败日志 = 并发语义可观测"**。 [模式: 日志锚]
