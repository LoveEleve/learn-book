# 闭环笔记 q6: 写入链与读面 — CommitLog putMessage 入口 + 消费读取

## 假设
写路径 = putMessage → 锁 → 定位文件 → doAppend (编码+落缓冲) → handleDiskFlushAndHA; 读面 = mmap 切片。

## 验证过程
- **写入口** (CommitLog.putMessage L1071 区域): 锁 (PutMessageLock) → MappedFileQueue 定位/滚动 → MappedFile.appendMessage → AppendMessageResult (wroteOffset/wroteBytes/msgId)
- **doAppend** (L1902/2010, 编码回调): MessageExtEncoder — 消息序列化入缓冲 (魔数/长度/topic/queue/body...) — RM-3 展开
- **写后链** (handleDiskFlushAndHA L1272): FlushManager.handleDiskFlush (刷盘, q2) + handleHA (复制, RM-12 交叉) — **刷盘与 HA 并行异步**
- **putMessageResult 状态**: PUT_OK / FLUSH_DISK_TIMEOUT / SLAVE_NOT_AVAILABLE (RM-12) / FLUSH_SLAVE_TIMEOUT
- **读面** (MappedFile.selectMappedBuffer L503-527): mmap 切片 (MappedByteBuffer.slice + position) — 零拷贝读; 引用计数 (SelectMappedBufferResult 持有 → cleanup 延迟)
- **消息读取**: GetMessageResult + MessageExtDecoder (消费拉取面 — RM-8 交叉)
- **监控面**: FlushDiskWatcher 5.x + 磁盘空间检测 (DiskSpaceWatcher? broker 面)

## 代码类型
Implementation (写入/读取链)

## 跨域关联
- RM-3 (CommitLog): doAppend 编码
- RM-8/9 (消费): selectMappedBuffer 读
- RM-12 (HA): handleHA

## 结论
写链 = 锁 → 文件定位 → doAppend 编码入缓冲 → 刷盘+HA 并行; 读面 = mmap 切片 + 引用计数延迟卸载 (零拷贝双向)。
源码位置: CommitLog.java:1071-1297,1902-2010; DefaultMappedFile.java:503-591
