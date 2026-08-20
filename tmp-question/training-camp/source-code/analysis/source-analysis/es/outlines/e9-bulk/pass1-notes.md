# E-9 Bulk — Pass 1 探索笔记 (扫轮廓)

> 🟡 B | 依赖: E-1 ✅ (Engine) + E-5 ✅ (Shard) + E-4 ✅ (路由) | 对照: [[rd4-command]] (命令批) [[rd1-connection]] (连接批)
> 源码: server/src/main/java/org/elasticsearch/action/bulk/ (24 文件 6544 行)
> 测试地图: server/src/test/.../action/bulk/ (10+ 文件)

## 继承树/调用图

```
TransportBulkAction (TransportBulkAction.java:94) — 批量入口
├── doExecute (TransportBulkAction.java:227): 校验 → forkAndExecute (TransportBulkAction.java:290)
├── 按 shard 分组: entry → BulkShardRequest (TransportBulkAction.java:675-677)
└── TransportShardBulkAction (TransportShardBulkAction.java:74) — 分片级执行
      ├── executeBulkRequest (TransportShardBulkAction.java:223-260): while hasMoreOperationsToExecute 主循环
      ├── applyIndexOperationOnPrimary (TransportShardBulkAction.java:360) / applyDeleteOperationOnPrimary (TransportShardBulkAction.java:343) — E-1 衔接
      └── BulkPrimaryExecutionContext — 执行上下文 (结果收集)

BulkRequestParser (BulkRequestParser.java:46) — NDJSON 解析
BulkProcessor (BulkProcessor.java:540) — 客户端批量封装 (重试/背压)
```

## 基本元素分解 (原则二)

1. **两阶段批量** — 协调分组 (TransportBulkAction L675) → 分片执行 (TransportShardBulkAction L223)
2. **按 shard 分组** — entry.getKey() (TransportBulkAction.java:673) → BulkShardRequest (L675) — 同一分片操作合并
3. **主循环执行** — while hasMoreOperationsToExecute (TransportShardBulkAction.java:223) — 逐条 applyIndexOperationOnPrimary (TransportShardBulkAction.java:360)
4. **映射更新等待** — MAPPING_UPDATE_REQUIRED (TransportShardBulkAction.java:370) → 等待动态映射 → 重新执行
5. **NDJSON 解析** — BulkRequestParser (BulkRequestParser.java:46): 两行一组 (action + doc)
6. **客户端封装** — BulkProcessor (BulkProcessor.java:540): 批量/重试/背压

## 标记问题 (≥5)

1. **Q1: 按 shard 分组怎么工作?** — entry → BulkShardRequest (TransportBulkAction.java:675) — 分组键是什么? 同分片操作怎么合并?
2. **Q2: 主循环执行语义** — while hasMoreOperationsToExecute (TransportShardBulkAction.java:223) — 逐条执行? 失败怎么处理?
3. **Q3: 映射更新等待** — MAPPING_UPDATE_REQUIRED (TransportShardBulkAction.java:370) → break 等映射 (L233) → 重新执行 — 动态映射流程?
4. **Q4: 批量 vs 单条** — 批量的性能优势? (减少 RTT/批量 fsync?) — 与 E-3 translog 批量对照
5. **Q5: BulkProcessor 背压** — 客户端怎么限流? (重试/退避?)
6. **Q6: 失败语义** — 批量中单条失败影响其他吗? (partial success?)
7. **Q7: 与 Redis pipeline 对照** — Redis pipeline (命令批) vs ES bulk (操作批) — 差异?
8. **Q8: 复制面** — BulkShardRequest 复制到副本? (E-6 衔接)

## 已读测试 (2 个)

- `BulkPrimaryExecutionContextTests`: 执行上下文
- `BulkProcessorTests`: 客户端封装
- `BackoffPolicyTests`: 退避策略

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: E-9 Pass 1 — TransportShardBulkAction 是 E-1 Engine 的批量入口 (applyIndexOperationOnPrimary L360)
- 发现: MAPPING_UPDATE_REQUIRED 等待机制与 E-7 动态映射衔接 (映射更新后重试)
- 已对照验证: TransportShardBulkAction.java:360,370 + E-7 createDynamicUpdate
