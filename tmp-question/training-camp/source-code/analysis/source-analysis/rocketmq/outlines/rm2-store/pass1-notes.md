# RM-2 存储底层 — Pass 1 探索笔记

> 域: RM-2 存储底层 (MappedFile/FlushManager) | 🔴 A 方案 | 2026-08-14
> 源码: store 模块 (37119 行/126 文件) 的 logfile/ + FlushManager + TransientStorePool + AllocateMappedFileService | RocketMQ 5.3.1

## 调用图

```
写入路径:
CommitLog (2460 行) → MappedFileQueue (917 行) → getLastMappedFile → DefaultMappedFile (946 行)
  appendMessage (writeBuffer 或 mappedByteBuffer 直写) → appendMessageReturn
  → FlushManager.handleDiskFlush → 三服务之一
刷盘三服务 (CommitLog 内部类):
  GroupCommitService (L1614): SYNC_FLUSH 组提交 (批量刷 + future 等待, syncFlushTimeout)
  FlushRealTimeService (L1487): ASYNC_FLUSH 定时/按页数 (flushIntervalCommitLog=500ms / leastPages=4 / thoroughInterval=10s)
  CommitRealTimeService (L1432): transientStorePool 启用时 writeBuffer→fileChannel 定期转储
内存映射:
DefaultMappedFile (946 行): fileChannel.map(READ_WRITE, 0, fileSize) → mappedByteBuffer
  + writeBuffer (TransientStorePool 借用 — 堆外池) 双缓冲; 写 buffer 刷盘转 fileChannel (mappedByteBuffer 只读面)
  isLoaded0 反射 (L118-121): 页加载状态统计
TransientStorePool (30 行): 堆外 ByteBuffer 池 (transientStorePoolSize=5, 1GB/块) + borrow/return/availableBufferNums
AllocateMappedFileService: 后台预分配 MappedFile (mmap 预热, 避免写时卡顿)
MappedFileQueue (917 行): 文件列表管理 — getLastMappedFile(create)/findMappedFileByOffset/deleteExpiredFile/rollNextFile
FlushDiskWatcher: 刷盘超时监控 (5.x)
配置 (MessageStoreConfig): mappedFileSizeCommitLog=1GB / flushIntervalCommitLog=500ms /
  flushCommitLogLeastPages=4 / flushCommitLogThoroughInterval=10s / transientStorePoolEnable=false 默认 /
  transientStorePoolSize=5 / syncFlushTimeout
```

## 基本元素分解

1. **MappedFile**: 单文件抽象 (mmap + 写缓冲 + 刷盘 + 卸载)
2. **双缓冲**: writeBuffer (堆外池) vs mappedByteBuffer (mmap) — 写入与读面分离
3. **MappedFileQueue**: 文件序列管理 (创建/定位/删除)
4. **刷盘三服务**: 组提交/定时/转储 (按 flush 类型 + 池状态)
5. **TransientStorePool**: 堆外池 (预分配 1GB 块)
6. **预分配**: AllocateMappedFileService (后台 mmap 预热)
7. **FlushDiskWatcher**: 刷盘监控 (5.x)

## 标记问题 (20 问)

1. mmap 怎么映射? (fileChannel.map READ_WRITE)
2. writeBuffer 与 mappedByteBuffer 何时用哪个? (append 双路径)
3. transientStorePool 启用的意义? (堆外直写 + 刷盘转 fileChannel)
4. GroupCommitService 组提交怎么聚合? (请求队列 + 批量刷)
5. FlushRealTimeService 的触发条件? (500ms/4 页/10s 三条件)
6. CommitRealTimeService 与 FlushRealTimeService 分工? (转储 vs 落盘)
7. 同步刷盘怎么等待? (future.get syncFlushTimeout)
8. MappedFileQueue 怎么定位文件? (offset → 文件索引)
9. 文件删除时机? (deleteExpiredFile — 保留时间/空间)
10. 预分配机制? (AllocateMappedFileService 后台 mmap)
11. 页加载统计? (isLoaded0 反射)
12. 文件预热? (mlock? warmMappedFile?)
13. 刷盘失败监控? (FlushDiskWatcher)
14. 写入锁? (PutMessageLock 自旋/重入 — 5.x)
15. 内存限制? (maxTransferBytesOnMessageInMemory=256KB)
16. 磁盘满处理? (磁盘空间检测)
17. 多路径? (MultiPathMappedFileQueue — 5.x 多盘)
18. 1GB 文件的缺陷? (mmap 大文件限制/页表压力)
19. 刷盘顺序保证? (消息有序落盘)
20. 与 ConsumeQueue 的关系? (消费队列索引 — RM-3)

## 时空溯源 (代码内痕迹)

- MappedFile mmap 模型: RocketMQ 3.x 初版即定型 (CommitLog 1GB 文件 + mmap)
- 演进: transientStorePool (4.x, 堆外池优化 — 默认关闭) / AllocateMappedFileService 预分配 / PutMessageLock 自旋 (4.x 高并发)
- 5.x: FlushDiskWatcher (刷盘监控) / MultiPathMappedFileQueue (多盘) / isLoaded0 页统计 / metrics
- 双缓冲 commit/flush 分离: CommitRealTimeService (writeBuffer→fileChannel) + FlushRealTimeService (fileChannel→磁盘)

## 大域拆分判断

store 37119 行大模块 — RM-2 聚焦 logfile+flush 面 (~2500 行主体); 🔴 A 单篇 (6 闭环)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "MappedFile/FlushManager/内存映射" | 面存在: logfile/ (3 文件 1350 行) + FlushManager 接口 + 三服务 (CommitLog 内部) | **接受** ✅ |
| 数字: mappedFileSizeCommitLog | = 1024³ = **1GB** (MessageStoreConfig:51) | **补充** ✅ |
| 数字: flushIntervalCommitLog | 500ms (L125) + leastPages=4 (L177) + thoroughInterval=10s (L184) | **补充** ✅ |
| "刷盘策略" (PLAN RM-3 提) | FlushDiskType: SYNC/ASYNC + 三服务 | **接受** ✅ |
