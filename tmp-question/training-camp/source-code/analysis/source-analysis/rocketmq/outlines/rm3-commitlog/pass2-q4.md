# 闭环笔记 q4: IndexFile — 40B 头 + 哈希槽表 + 20B 索引项

## 假设
按 key 查询 = 哈希槽 + 链表; 布局 = 头 + 槽表 + 索引项。

## 验证过程
- **布局** (IndexFile.java:58): INDEX_HEADER_SIZE(40) + hashSlotNum×hashSlotSize(4) + indexNum×indexSize(20)
- **IndexHeader 40B** (IndexHeader.java:37-45): BeginTs(8) + EndTs(8) + BeginPhyOffset(8) + EndPhyOffset(8) + HashSlotCount(4) + IndexCount(4)
- **索引项 20B** (indexSize=20): keyHash(4) + phyOffset(8) + timeDiff(4) + prevIndex(4) — **prevIndex 链式** (同槽冲突链表)
- **槽表**: 4B/槽 (指向索引项位置); **hashSlotSize=4** (IndexFile:32)
- **容量**: maxHashSlotNum 默认 **500 万槽** (MessageStoreConfig:199) — 索引文件 = 40 + 500万×4 + 索引项×20
- **写入** (IndexService.putKey L213-246): buildKey = **topic#uniqKey** (L232/244) → keyHash → 槽定位 → 索引项追加 (timeDiff 相对 beginTimestamp) → 冲突 prevIndex 链
- **滚动**: IndexFile 满 (索引项耗尽) → 新文件 (endTimestamp 截止)
- **查询**: QUERY_MESSAGE by key → 槽 → 链遍历 → phyOffset 定位

## 代码类型
Implementation (哈希索引)

## 跨域关联
- RM-8 (消费): 按 key 查询 (QUERY_MESSAGE)
- 对照 R-3 (Redis dict): 哈希+链

## 结论
IndexFile = 40B 头 + 500 万×4B 槽表 + 20B 链式索引项 (keyHash/phyOffset/timeDiff/prevIndex); key = topic#uniqKey; 容量 = 索引项数限制触发滚动。
源码位置: IndexFile.java:32,46,58; IndexHeader.java:36-45; IndexService.java:213-246; MessageStoreConfig.java:199
