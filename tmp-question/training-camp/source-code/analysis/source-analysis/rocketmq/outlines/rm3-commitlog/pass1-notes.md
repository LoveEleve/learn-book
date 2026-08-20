# RM-3 CommitLog+ConsumeQueue+IndexFile — Pass 1 探索笔记

> 域: RM-3 存储主链路 | 🔴 A 方案 | 2026-08-14
> 源码: store 模块: CommitLog (2460) + MessageExtEncoder + queue/ (5.x 多实现) + index/ | RocketMQ 5.3.1

## 调用图

```
写入面 (CommitLog, 2460 行):
putMessage (L1071+) → putMessageLock → 定位/滚动 → DefaultMappedFile.appendMessage
  → doAppend (L1902/2010: MessageExtEncoder 编码入缓冲) → 写后: dispatch + handleDiskFlushAndHA
消息编码 (MessageExtEncoder):
encode (L175+): 字段序 TOTALSIZE(4)/MAGICCODE(4)/BODYCRC(4)/QUEUEID(4)/FLAG(4)/QUEUEOFFSET(8)/
  PHYSICALOFFSET(8)/SYSFLAG(4)/BORNTIMESTAMP(8)/BORNHOST/STORETIMESTAMP/STOREHOST/.../BODY/PROPERTIES
  双上限: maxMessageBodySize / maxMessageSize; PROPERTIES_SIZE_EXCEEDED (propertiesLength > Short.MAX)
  encodeWithoutProperties (5.x 多分派场景) / encode Batch (L282)
分发面 (DefaultMessageStore):
doDispatch → dispatcherList: BuildConsumeQueue + BuildIndex (+ 5.x Compaction)
  → ConsumeQueueStore.putMessagePositionInfoWrapper (30 次重试 + isCQWriteable)
消费队列 (queue/ 5.x 多实现):
ConsumeQueueStoreInterface: ConsumeQueueStore (默认) / BatchConsumeQueue (批量) / SparseConsumeQueue (稀疏) / RocksDBConsumeQueue (RocksDB)
单元: CQ_STORE_UNIT_SIZE=20B (commitLogOffset 8 + size 4 + tagsCode 8); ConsumeQueueExt (48MB: 位图/时间/tagsCode 扩展, isExtAddr 高位标记)
文件: mappedFileSizeConsumeQueue = 300000×20 = 6,000,000B ≈ 5.7MB; Ext 48MB; Batch 300000×单元
索引面 (index/):
IndexService (398): putKey → IndexFile (257): INDEX_HEADER_SIZE + hashSlotNum×4 + indexNum×indexSize
  hashSlotSize=4 (每槽 4B); maxHashSlotNum=500 万 (默认); key = topic#uniqKey (buildKey)
  IndexHeader: begin/endTimestamp + begin/endPhyOffset + slotLogicReadOffset + indexLogicReadOffset
测试面: ConsumeQueueTest/Batch/Sparse/RocksDB + IndexFileTest + ConsumeQueueExtTest
```

## 基本元素分解

1. **消息编码**: 固定字段序 + 长度计算 (calMsgLength) + 双上限
2. **写后分发**: dispatcherList 链 (CQ + Index + Compaction)
3. **ConsumeQueue**: 20B 单元序列 (逻辑队列) + Ext 扩展 + 多实现 (5.x)
4. **IndexFile**: 哈希槽索引 (500 万槽/4B + 20B 索引项)
5. **消费读面**: getMessage (offset → CQ → CommitLog 定位)
6. **恢复面**: 启动 recover (CQ 截断对齐)

## 标记问题 (20 问)

1. 消息字段序? (24 字段, 编码顺序)
2. calMsgLength 怎么算? (固定头 + 变长段)
3. 双上限语义? (body 4MB? / message 4MB?)
4. PROPERTIES_SIZE_EXCEEDED 条件? (propertiesLength > Short.MAX)
5. 多分派 encodeWithoutProperties? (5.x 事务/批量?)
6. dispatch 时机? (写后同步/异步?)
7. 20B 单元三字段? (offset/size/tagsCode)
8. ConsumeQueueExt 扩展? (位图/时间/大 tagsCode)
9. 5.x 四实现差异? (默认/Batch/Sparse/RocksDB)
10. IndexFile 布局? (头 + 槽表 + 索引项)
11. hashSlotSize=4 与 maxHashSlotNum 关系?
12. IndexFile 滚动? (文件切换条件)
13. 查询路径? (QUERY_MESSAGE 按 key)
14. 消费读面? (offset → 定位)
15. 恢复面? (CQ 与 CommitLog 对齐截断)
16. 批量消息 Batch? (encode Batch 格式)
17. Compaction 分发? (5.x 清理)
18. 消费队列刷盘? (flushIntervalConsumeQueue=1000ms)
19. 消费进度? (ConsumerOffsetManager — RM-9)
20. tagsCode 哈希? (tag 哈希到 8B)

## 时空溯源 (代码内痕迹)

- 3.x 定型: CommitLog+CQ+Index 三文件体系 (20B 单元/哈希槽索引)
- 4.x: ConsumeQueueExt (过滤位图扩展) / 批量消息 Batch
- 5.x: **queue/ 包重构** (ConsumeQueueStoreInterface + Batch/Sparse/RocksDB 多实现) / Compaction 分发 (5.1) / encodeWithoutProperties (多分派)
- IndexFile: hashSlotSize=4 静态常量 (3.x 起)

## 大域拆分判断

RM-3 覆盖 CommitLog 编码 + CQ + Index 三面; 🔴 A 单篇 (6 闭环); 与 RM-2 (文件底层) 边界: RM-2 讲文件/刷盘, RM-3 讲消息格式/队列/索引

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "主链路/IndexFile (hashSlotSize=4 桶内常数, maxHashSlotNum 默认 500 万)" | IndexFile.java:32 (hashSlotSize=4) + MessageStoreConfig:199 (500 万) | **接受** ✅ |
| "刷盘策略" (归属 RM-3 提) | 已在 RM-2 交付 (FlushDiskType 三服务) | 边界已划 (RM-2 §2) |
| "ConsumeQueue 构建" | dispatcherList BuildConsumeQueue + ConsumeQueueStore | **接受** ✅ |
| 数字: CQ 单元 | CQ_STORE_UNIT_SIZE=20 (offset 8+size 4+tagsCode 8) | **补充** ✅ |
| 数字: CQ 文件 | 300000×20 = 6,000,000B ≈ 5.7MB; Ext 48MB | **补充** ✅ |
