# RM-3 CommitLog+ConsumeQueue+IndexFile — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | 三文件体系定型: CommitLog (消息编码 18 段) + ConsumeQueue (20B 单元) + IndexFile (哈希槽 500 万/20B 项); dispatch 链 (BuildCQ+BuildIndex); 恢复对齐截断 |
| 4.x | **ConsumeQueueExt** (48MB: 过滤位图/存储时间/tagsCode 溢出转址); **批量消息** (MessageExtBatch + encode Batch); CRC32 保留位 |
| **5.0** | **queue/ 包重构**: ConsumeQueueStoreInterface + 多实现 (默认/Batch/Sparse/**RocksDB**); getMessageAsync (CompletableFuture 长轮询面); encodeWithoutProperties (多分派) |
| **5.1** | **Compaction 分发** (CommitLogDispatcherCompaction — 消息清理, TieredStore 协同); MESSAGE_VERSION_V2 (topic 长 short) |

## 痕迹证据

- MessageExtEncoder.java:60-83: calMsgLength 18 段求和 (字段序注释)
- MessageExtEncoder.java:198-205: 属性超限 (Short.MAX) 拒绝
- DefaultMessageStore.java:266-272: dispatcherList 三链
- DefaultMessageStore.java:418: 恢复一致性注释 ("eliminating the dispatch inconsistency")
- ConsumeQueue.java:59: CQ_STORE_UNIT_SIZE=20 (3.x 起)
- IndexHeader.java:37-45: 40B 头布局
- IndexFile.java:32: hashSlotSize=4 (3.x 起)
- queue/ 包 17 文件: 5.x 多实现

## 推断标注

- "3.x 三文件定型" — RocketMQ 公知版本线 (标注)
- "4.x Ext/批量/CRC" — 特性年代推断 (标注)
- "5.0 queue/ 重构 / 5.1 Compaction" — 与 proxy/tieredstore 同代推断 (标注)
- 未做 git 考古, 版本线为代码结构推断, 已逐条标注
