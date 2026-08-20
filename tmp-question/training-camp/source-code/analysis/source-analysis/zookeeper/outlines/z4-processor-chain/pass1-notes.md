# Z-4 Processor 链 — Pass 1 探索笔记

> 域: Z-4 Processor 链 | 🔴 A 方案 | 2026-08-15
> 源码: PrepRequestProcessor (1122) + SyncRequestProcessor (281) + CommitProcessor (quorum, 644) + FinalRequestProcessor (680) + ToBeAppliedRequestProcessor (Leader 内部) + Request (561) + ZooKeeperServer.setupRequestProcessors | ZooKeeper 3.9.5

## 调用图

```
客户端请求 → NIOServerCnxn → ZooKeeperServer.submitRequest → PrepRequestProcessor
  → pRequest: checkSession + pRequest2Txn (OpCode 分发: 校验链 + setTxn + addChangeRecord)
  → SyncRequestProcessor: toFlush 批量 → flush (txnlog) → snapCount 判定 → takeSnapshot
  → CommitProcessor (leader): 读直通 / 写 per-session 等 commit (committedRequests)
  → ToBeAppliedRequestProcessor → FinalRequestProcessor
      → zks.processTxn (树应用 Z-3) → cnxn.sendResponse
```

## 基本元素分解

1. **链装配**: 五节点链 (Prep/Sync/Commit/ToBeApplied/Final) + 每级独立线程
2. **Prep 预校验**: session/ACL/quota/路径 + 事务生成 + outstandingChanges 暂存 + multi 回滚
3. **Sync 刷盘**: toFlush 批量 + snapCount 随机快照 + 读旁路
4. **Commit+Final**: 读写分离 (per-session) + 提交匹配 + 树应用 + 响应

## 标记问题 (20 问)

1. 链装配在哪? (ZooKeeperServer.setupRequestProcessors)
2. Request 封装字段? (sessionId/cxid/zxid/type/authInfo/cnxn)
3. Prep 主循环? (submittedRequests.take → pRequest)
4. pRequest2Txn 校验链? (checkSession/checkACL/checkQuota/validatePath)
5. getRecordForPath? (outstandingChanges 先行)
6. addChangeRecord? (变更暂存)
7. 顺序节点序号? (%010d parentCVersion)
8. multi 回滚? (getPendingChanges + rollback)
9. closeSession 清扫? (ephemeral 预计算)
10. Sync 批量? (toFlush + maxBatchSize)
11. snapCount 判定? (logCount > snapCount/2 + randRoll)
12. 读旁路? (toFlush 空 → next)
13. Commit 双队列? (queuedRequests/committedRequests)
14. 读直通条件? (needCommit false)
15. 写暂存? (per-session pendingRequests)
16. 提交匹配? (sessionId+cxid)
17. waitForEmptyPool? (读 drain)
18. Final 应用? (zks.processTxn)
19. Final 响应? (sendResponse + decInProcess)
20. throttled? (THROTTLEDOP)

## 时空溯源 (代码内注释锚)

- ZOOKEEPER-1624: multi 回滚父记录 (L228-235)
- closeSession 竞态注释 (L581-583)
- CommitProcessor 读写分离 (maxReadBatchSize 注释 L261-270)
- ToBeAppliedRequestProcessor (Leader.java:1117 — Z-2 交叉)

## 大域拆分判断

Z-4 = 五处理器 (Prep/Sync/Commit/ToBeApplied/Final) 管线; 单篇 🔴 A (8 闭环 q1-q4 + 验证); 事务日志细节归 Z-9 (Sync 仅引用)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划) | 验证 | 结论 |
|:--|:--|:--|
| "Prep(10+OpCode+multi-op)→Sync(toFlush+snapCount)→Commit(读写分离)→Final" | 四处理器全实证 + ToBeApplied 中间层 | **接受+补充** ✅ |
| "10+OpCode" | OpCode 分支: create 族/delete 族/setData/setACL/reconfig/check/createSession/closeSession/multi/error — **10+ 确认** | **接受** ✅ |
| 数字: snapCount | DEFAULT=100000, 判定 snapCount/2 + randRoll | **补充** ✅ |
| 数字: 队列 | toFlush maxBatchSize / per-session pendingRequests | **补充** ✅ |
