# RM-2 存储底层 — 内存映射文件与刷盘三服务

> 前置: [[RM-1-remoting]] (协议面) | 引出: [[RM-3-commitlog]] [[RM-8-消费]] [[RM-12-HA]] | 对照: [[R-8-rdb-aof]] (Redis 持久化) 
> 🔴 A | 6 KP | [模式: mmap 双缓冲 + 文件段序列 + 刷盘调度 + 堆外池]
> Pass 2 闭环: q1(MappedFile) q2(刷盘三服务) q3(文件队列) q4(预分配+锁) q5(堆外池) q6(写读链)

**读者处境**: 消息存哪? 为什么用 mmap? 同步/异步刷盘差多少? 1GB 文件怎么切? 这篇拆存储底层: mmap 双缓冲、文件段序列、刷盘三服务、堆外池。

### 1. MappedFile — mmap + 双缓冲生命周期

场景: 一个 1GB 文件怎么高效读写?
源码路径:
- **映射** (DefaultMappedFile:168): `fileChannel.map(READ_WRITE, 0, fileSize)` — mmap 页缓存
- **双缓冲** (L83-95): writeBuffer (堆外池借用) vs mappedByteBuffer; `getAppendBuffer`: **writeBuffer != null ? writeBuffer : mmap** — 池启用写堆外, 否则直写 mmap
- **三阶段**: append (写缓冲) → commit (transferTo 页缓存, 池启用) → flush (force 磁盘)
- **appendMessageUsingFileChannel** (L361): 池启用且缓冲满 → fileChannel 直写 ("never both" 注释)
- **读面** (selectMappedBuffer L503): mmap 切片 (零拷贝)
- **引用计数卸载** (cleanup L545-591): 被读时延迟 unmap (防页表提前释放)
- **isLoaded0 反射** (L118): 页加载统计 (5.x)
关键设计 (q1): **双缓冲 = 写入与页缓存解耦**: 堆外直写 (无页缓存颠簸) → 批量转储 → 强制落盘; 引用计数保读安全。[模式: mmap 文件抽象]

### 2. 刷盘三服务 — 组提交/定时/转储

场景: SYNC/ASYNC 怎么实现? 组提交怎么聚合?
源码路径:
- **选择** (DefaultFlushManager L2118): SYNC → GroupCommitService / ASYNC → FlushRealTimeService; 池启用额外 CommitRealTimeService (转储)
- **组提交** (L1614-1660): **双链表 swap** (写侧锁 + 读侧无锁); putRequest (自旋锁) → doCommit: `flushedWhere >= 请求水位` 判定 → 不满足循环 flush 最多 1000 次 + **sleep(1ms)** (池启用 commit 等待注释 L1647-1653) → wakeupCustomer (PUT_OK/超时)
- **定时刷** (FlushRealTimeService L1487-1530): 500ms 周期醒来 (flushCommitLogTimed 决定 sleep/wait) + **leastPages=4 攒页批量**; **每 10s (thoroughInterval) leastPages 降 0 = 强刷全量** (L1516-1519, 非"刷一次"而是"全部刷完"); doCommit 的 else 分支 (非 wait 消息) → flush(0)
- **同步等待** (handleDiskFlush L2133): future.get(**syncFlushTimeout=5s**, MessageStoreConfig:220) — 消息落盘才返回; 非 wait → wakeup (异步组提交)
- **双组提交服务**: 刷盘 = CommitLog.GroupCommitService (flushedWhere 水位); **复制 = ha/GroupTransferService (独立类, 双链表+自旋, doWaitTransfer 等从库 replicationOffset 水位)**; handleHA 的 needAckNums = **inSyncReplicas 配置** (L951-965: 同步副本数/ALL_ACK_IN_SYNC_STATE_SET/calcNeedAckNums) — 同步复制 = 双组提交 + thenCombine (RM-12 交叉)
- **磁盘保护** (DefaultMessageStore L2340-2360): 定时检测分区使用率 — > warningRatio → runningFlags **diskFull 标志 (写拒绝)**; > cleanForciblyRatio → 强制清理; 系统属性 rocketmq.broker.diskSpaceWarningLevelRatio/CleanForciblyRatio
关键设计 (q2): **双链表 swap = 无锁读侧批量消费** (生产者只加写链); flushedWhere 水位 = 组提交的判定锚。[模式: 刷盘调度]

### 3. MappedFileQueue — 文件段序列

场景: 千万消息怎么定位文件?
源码路径:
- **段序列**: 1GB 对齐段 (createOffset = startOffset - (startOffset % fileSize), L299-322)
- **滚动** (shouldRoll L337): 满/越界 → 下一文件
- **定位** (findMappedFileByOffset L670-700): **索引除法** ((offset/fileSize) - (firstOffset/fileSize)) → mappedFiles.get(index) → 边界校验 → 不匹配 **线性扫描兜底** → returnFirstOnNotFound (越界返回首文件)
- **批量 commit/flush**: 全队列推进水位 (flushedWhere/committedWhere); 引用计数 **hold() 语义**: 读 (selectMappedBuffer)/刷/转储路径都先 hold — 防文件释放中操作 (L209/384/422/506)
- **删除** (deleteExpiredFile L210): 保留策略 (时间/空间)
- **5.x MultiPath**: 多盘分配
关键设计 (q3): **对齐段 = 除法定位 O(1)**; 文件即日志 (顺序写) — 消息系统的核心存储形态。[模式: 段式日志]

### 4. 预分配与写锁 — 防卡顿

场景: mmap 慢 (几十 ms) 怎么办? 并发写怎么锁?
源码路径:
- **写锁双实现**: PutMessageSpinLock (CAS 自旋 — **默认**, CommitLog:133 isUseReentrantLockWhenPutMessage 默认 false) / PutMessageReentrantLock (重入); **组提交内部也用自旋** (L1617); 池不足降级 (跳过预分配告警)
- **预分配** (AllocateMappedFileService 后台线程): mmapOperation — 路径去重 (requestTable) + ServiceLoader 自定义 MappedFile (5.x) + **创建后即 warmMappedFile** (L199 — 预分配即预热闭环); **warm = 逐 OS_PAGE_SIZE 页写 0 (触发缺页加载) + SYNC 间隔 force + mlock 全文件锁页** (DefaultMappedFile:621-660); >10ms 告警
关键设计 (q4): **预分配 = 把 mmap 延迟移到写路径外**; 自旋锁 = 低竞争下免上下文切换。[模式: 后台预热]

### 5. TransientStorePool — 堆外池

场景: 5GB 堆外内存干什么?
源码路径:
- **init** (L44-56): `allocateDirect(1GB) + mlock 锁页` (JNA LibC — 防 swap, "heavy init" 注释)
- **借还**: ConcurrentLinkedDeque (borrow pollFirst / return offerFirst)
- **水位告警**: 剩余 < 40% → 告警
- **配置**: 默认关闭 (transientStorePoolEnable=false, size=5)
关键设计 (q5): **堆外直写 → 批量转储页缓存** — 减少页缓存写放大; mlock 防换出; 5GB 代价换大消息吞吐。[模式: 堆外池]

### 6. 写读链 — putMessage 与消费读

场景: 一条消息从入缓冲到被读走?
源码路径:
- **写链** (CommitLog.putMessage L1071): 锁 → 定位/滚动 → doAppend (编码入缓冲) → **handleDiskFlushAndHA 并行** (刷盘 + 复制, L1272)
- **状态**: PUT_OK / FLUSH_DISK_TIMEOUT / SLAVE_NOT_AVAILABLE (RM-12)
- **读面**: selectMappedBuffer mmap 切片 + 引用计数 → 消费端零拷贝
- **监控**: FlushDiskWatcher (5.x) + 磁盘空间检测
关键设计 (q6): **刷盘与 HA 并行异步** — 主链路不阻塞; 零拷贝双向 (写堆外/读 mmap)。[模式: 写读链]

### 负面空间 — 存储底层刻意不做的事

- **不做磁盘级压缩**: 段文件不压缩 (对比 Kafka 的 log compaction)
- **不做稀疏存储**: 1GB 文件预分配 (空间换延迟)
- **不做页缓存绕过**: 依赖 OS 页缓存 (对比 RocksDB DirectIO 选项)
- **不做小文件聚合**: 一 topic 一 ConsumeQueue (RM-3) 而非共享文件
- **不做热数据分层**: 全冷热同盘 (TieredStore 5.1 才引入 — RM-16 一句话)

→ 引出: 消息怎么编进 CommitLog? → [[RM-3-commitlog]]
