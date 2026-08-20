# 闭环笔记 q3: ConsumeQueue — 20B 单元 + Ext 扩展

## 假设
逻辑队列 = 20B 单元序列 (offset/size/tagsCode); Ext 承载过滤位图; 文件 5.7MB。

## 验证过程
- **单元** (ConsumeQueue.java:59): **CQ_STORE_UNIT_SIZE=20B** = commitLogOffset(8) + size(4) + tagsCode(8)
- **写入** (putMessagePositionInfoWrapper L200-230): tagsCode 计算 (tag 哈希) → **isExtWriteEnable → ConsumeQueueExt 写入** (CqExtUnit: filterBitMap + msgStoreTime + tagsCode) → extAddr 高位标记 (isExtAddr) — **tagsCode 20 位空间不足时转 Ext**
- **文件**: mappedFileSizeConsumeQueue = **300000×20 = 6,000,000B ≈ 5.7MB** (MessageStoreConfig:112); Ext 48MB (L116)
- **读面**: 消费按 queueOffset → 单元 → commitLogOffset/size → CommitLog 定位
- **刷盘**: flushIntervalConsumeQueue=1000ms (独立于 CommitLog)
- **多实现** (5.x queue/ 包): ConsumeQueueStoreInterface — ConsumeQueueStore (默认) / BatchConsumeQueue / SparseConsumeQueue / RocksDBConsumeQueue (RocksDB 后端) — RM-16 交叉
- **最大物理偏移**: maxPhysicOffset 恢复时推进 (L146)

## 代码类型
Implementation (逻辑队列)

## 跨域关联
- RM-8/9 (消费): queueOffset 索引
- RM-16 (TieredStore): RocksDB CQ

## 结论
CQ = 20B 单元逻辑队列 (CommitLog 的 topic/queue 分片索引); Ext 扩展承载位图 (tagsCode 溢出转址); 5.x 四实现抽象; 独立刷盘 (1s)。
源码位置: ConsumeQueue.java:59,200-230; queue/ 包; MessageStoreConfig.java:112-117
