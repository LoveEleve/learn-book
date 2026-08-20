# 闭环笔记 q6: 5.x 多实现与测试面 — queue/ 包 + 测试地图

## 假设
5.x CQ 四实现抽象; 测试面覆盖各实现。

## 验证过程
- **queue/ 包** (5.x 重构): ConsumeQueueStoreInterface + AbstractConsumeQueueStore — 实现: ConsumeQueueStore (默认 20B) / **BatchConsumeQueue** (批量消费队列, CQ_STORE_UNIT_SIZE 不同) / **SparseConsumeQueue** (稀疏) / **RocksDBConsumeQueue** (RocksDB 后端 + OffsetTable) — 配置选择 (enableConsumeQueueExt/consumeQueueType?)
- **Batch 面**: BatchConsumeQueue + BatchOffsetIndex + MultiDispatchUtils — 批量消息 (encode Batch) 的消费索引
- **测试地图** (store/src/test): ConsumeQueueTest / **BatchConsumeQueueTest** / **SparseConsumeQueueTest** / ConsumeQueueStoreTest / RocksDBConsumeQueueTest / IndexFileTest / ConsumeQueueExtTest — 各实现有专项
- **默认选择**: enableConsumeQueueExt 时 Ext 面; RocksDB 需配置 (RocksDBConsumeQueueStore)
- **Compaction 分发** (5.1): CommitLogDispatcherCompaction — 消息清理面 (RM-16 TieredStore 协同)

## 代码类型
Interface (多实现抽象)

## 跨域关联
- RM-2 (存储底层): MappedFileQueue 复用
- RM-16 (TieredStore): RocksDB/Compaction

## 结论
5.x CQ 抽象 = 接口 + 4 实现 (默认/Batch/Sparse/RocksDB); 测试 7 专项覆盖; Compaction 分发为 5.1 清理面。
源码位置: queue/ 包 17 文件; store/src/test (7 专项)
