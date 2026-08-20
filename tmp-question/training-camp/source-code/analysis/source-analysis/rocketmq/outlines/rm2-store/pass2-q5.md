# 闭环笔记 q5: TransientStorePool + FlushDiskWatcher — 堆外池与刷盘监控

## 假设
堆外池预分配 mlock 固定; 借还 deque; 5.x 刷盘超时监控。

## 验证过程
- **TransientStorePool** (L30-85):
  - init (L44-56): **ByteBuffer.allocateDirect(fileSize) + mlock 锁页** (JNA LibC — 防 swap); "It's a heavy init method" 注释
  - 借还: borrowBuffer (pollFirst) / returnBuffer (offerFirst + 重置 position/limit) — ConcurrentLinkedDeque
  - **水位告警** (L73-77): 剩余 < poolSize*0.4 → 告警 (缓冲不足)
  - destroy: munlock
- **配置** (MessageStoreConfig): transientStorePoolEnable=false 默认 / transientStorePoolSize=5 (5×1GB 堆外)
- **收益**: 写堆外 → commit 批量 transferTo 页缓存 (减少页缓存颠簸, 大消息场景); 代价 = 5GB 堆外内存 + mlock 页
- **FlushDiskWatcher** (5.x, ServiceThread): 刷盘请求超时监控 (L44-64: 中断处理 + 异常等待) — 提交刷盘任务跟踪 (CommitLog 异步刷盘等待面)

## 代码类型
Implementation (内存池)

## 跨域关联
- RM-3 (CommitLog): 池借用面
- Netty (阶段1): 对照池化 ByteBuf

## 结论
堆外池 = 预分配 5×1GB + mlock 锁页 + deque 借还 + 水位告警 (默认关闭 — 大消息场景才开); 5.x FlushDiskWatcher 监控刷盘提交。
源码位置: TransientStorePool.java:30-85; MessageStoreConfig.java:237-238; FlushDiskWatcher.java:28-64
