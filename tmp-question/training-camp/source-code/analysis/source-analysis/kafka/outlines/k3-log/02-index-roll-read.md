# K-3 Log 存储 篇 2/3 — 找到它: 稀疏索引/轮转/读路径

> 前置: [[K-3-log-01]] (分段+写路径) | 复用: — | 对照: [[r8-persistence]] (AOF 重写 vs 段轮转) | 引出: [[K-3-log-03]] (收束)
> 🔴 A | 来源: OffsetIndex.java:97-107 + LazyIndex.java:28-46 + LogSegment.java:167-173,421,431-459 + LocalLog.java:462,581-646 + LogConfig.java:125
> 定位: K-3 卷中篇 — 回答"消费怎么定位到 offset? 段什么时候滚? 读怎么零拷贝?"

**读者处境**: 面试官问 "消费者怎么知道 offset N 在文件哪个位置? 段多大滚一个?" 你答 "索引、1GB" — 但再问 "索引稀疏到什么程度? 怎么二分? LazyIndex 是什么? 读怎么保证不超 maxSize?" 你答不上来。这篇是定位与读取的完整答案。

### 1. 问题引入 — offset 到字节位置的桥

场景: 消费者要读 offset 10000, 文件里怎么找到它?
- 顺序扫描太慢 → 稀疏索引 + 二分 (OffsetIndex.java:97-107)
- 本篇问题: 索引 (Q3) / 轮转 (Q4) / 读路径 (Q8)

### 2. 稀疏索引 — mmap 二分 + LazyIndex

场景: 索引记什么? 什么时候记?
- OffsetIndex.lookup (OffsetIndex.java:97-107): mmap 内存映射 (OffsetIndex.java:101) → largestLowerBoundSlotFor 二分 (OffsetIndex.java:103) — 找 ≤ target 的最大条目
- 索引条目: (相对 baseOffset 的 int offset, 物理 position) — 相对偏移防 int 溢出 (canConvertToRelativeOffset)
- LazyIndex (LazyIndex.java:28-46): 首次 get() 才 mmap (LazyIndex.java:40-42) — broker 启动数千段不用全映射
- 对照 E-3: translog checkpoint 单块 vs Kafka 每段双索引

### 3. 轮转 — 什么时候开新段

场景: 段多大滚? 什么条件?
- shouldRoll 四条件 (LogSegment.java:167-173): 大小超限 (LogSegment.java:170, 1GB, LogConfig.java:125) / 时间到 (LogSegment.java:171) / 索引满 (LogSegment.java:172) / 相对 offset 溢出 (LogSegment.java:172)
- roll 执行 (LocalLog.java:581-646): 封存旧段 (onBecomeInactiveSegment LocalLog.java:627-629) → LogSegment.open 新段 (LocalLog.java:631) + segments.add (LocalLog.java:637)
- 索引上限 10MB (ServerLogConfigs.java:77, LOG_INDEX_SIZE_MAX_BYTES_DEFAULT) — 对应 10MB/8B×4096 ≈ 5.2GB 数据; 正常 1GB 段先触大小限制, 索引满只在异常小段 (索引间隔小) 时触发

### 4. 读路径 — 二分定位 + 零拷贝 slice

场景: 定位到 position 后怎么返回数据?
- LogSegment.read (LogSegment.java:431-459): translateOffset 二分 (LogSegment.java:435) → minOneMessage: max(maxSize, 第一条大小) (LogSegment.java:445-446) → **log.slice 零拷贝** (LogSegment.java:457)
- fetchSize = min(maxPosition - startPosition, adjustedMaxSize) (LogSegment.java:455)
- 段读线程安全 (LogSegment.java:421) — 多消费者并发读同一段
- 跨段: LocalLog.read (LocalLog.java:462) — 段内定位 + 跨段衔接 (K-12 FetchSession 消费)

### 核心悬念
"为什么 Kafka 消费快? — 不是'顺序读'一个原因" — 三层: 稀疏索引二分定位 (O(log n)) + 零拷贝 slice (无用户态拷贝) + 段内顺序读; 索引稀疏到 4096 字节一条 (8 字节/条, OffsetIndex.java:56), 一个 1GB 段索引才 ~2MB (1GB/4096×8B), 上限 10MB (ServerLogConfigs.java:77), 全程 mmap 免系统调用。

### 概念依赖链
Q3 索引 → Q4 轮转 → Q8 读路径 → (03 篇: 恢复/截断) → (K-12 FetchSession 消费)

### 源码锚点清单
- OffsetIndex.java:97-107 (lookup 二分) / 101 (mmap) / 103 (largestLowerBoundSlotFor) / 105 (slot==-1 兜底)
- LazyIndex.java:28-46 (延迟加载 javadoc) / 40-42 (首次 get 才 mmap)
- LogSegment.java:167-173 (shouldRoll) / 168 (reachedRollMs) / 170 (大小条件) / 172 (索引满/溢出) / 421 (线程安全注释) / 431-459 (read) / 435 (translateOffset) / 445-446 (minOneMessage) / 455 (fetchSize) / 457 (slice)
- LocalLog.java:462 (read) / 581-646 (roll) / 587 (newOffset) / 627-629 (封存旧段) / 631 (LogSegment.open) / 637 (segments.add)
- LogConfig.java:125 (DEFAULT_SEGMENT_BYTES=1GB)
