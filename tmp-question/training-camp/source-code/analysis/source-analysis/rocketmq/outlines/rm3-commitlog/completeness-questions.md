# RM-3 CommitLog+ConsumeQueue+IndexFile — completeness-questions (全视角提问验证)

## 开发者视角

1. 消息 18 段字段序? (TOTALSIZE 到 CRC32 保留)
2. PHYSICALOFFSET 怎么回填? (写后按位置更新)
3. V6 地址标志影响? (BORNHOST 8B→20B)
4. 分发链有哪几个? (BuildCQ + BuildIndex + Compaction)
5. 20B 单元三个字段? (commitLogOffset/size/tagsCode)
6. ConsumeQueueExt 什么时候用? (tagsCode 溢出/位图)
7. IndexFile 怎么按 key 查? (topic#uniqKey → 槽 → 链)
8. 恢复怎么对齐? (单元校验 + 尾文件截断)

## 架构师视角

9. 为什么消息要定长头+变长尾? (长度预算 + 顺序读)
10. 为什么 CQ 20B 固定单元? (O(1) 逻辑定位)
11. 写后同步分发的取舍? (读一致 vs 写放大)
12. 双索引 (CQ/Index) 的分工? (导航 vs 查找)
13. 哈希槽 500 万怎么定? (容量 vs 内存 — 40B 头+20MB 槽表)
14. 恢复截断为什么安全? (半写单元校验丢弃)
15. 5.x RocksDB CQ 的意义? (存储后端解耦)
16. Batch CQ 解决什么? (批量消息消费索引)

## 学生视角

17. CommitLog 和 ConsumeQueue 什么关系? (全量日志 + 分片索引)
18. 为什么叫"逻辑队列"? (物理在 CommitLog, 20B 只是指针)
19. 按 key 查和按 offset 查区别? (哈希索引 vs 顺序导航)
20. 消息删了 CQ 怎么办? (文件级截断, 无单条墓碑)
