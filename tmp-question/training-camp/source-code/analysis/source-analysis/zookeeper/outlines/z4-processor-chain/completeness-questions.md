# Z-4 Processor 链 — completeness-questions (全视角提问验证)

## 开发者视角

1. 四处理器顺序? (Prep→Sync→Commit→ToBeApplied→Final)
2. 每处理器线程模型? (独立线程 + 队列)
3. Prep 校验什么? (session/ACL/quota/路径)
4. 事务在哪生成? (pRequest2Txn + setTxn)
5. Sync 什么时候刷盘? (批量 toFlush + maxBatchSize)
6. 读请求怎么走? (Commit 直通 — 不等广播)
7. 写请求怎么走? (pendingRequests per-session 等 commit)
8. Final 干什么? (processTxn 应用 + sendResponse)

## 架构师视角

9. 为什么会话级串行? (per-session 队列 — 顺序性边界最小化)
10. outstandingChanges 意义? (未提交变更可见 — 读-改-写一致性)
11. multi 原子性在哪? (Prep 预校验 + rollbackPendingChanges)
12. snapCount 随机化? (防快照风暴)
13. 读旁路 Sync? (读不落盘 — 性能)
14. 提交匹配 (sessionId+cxid)? (本地写 vs 远端写区分)
15. waitForEmptyPool? (读写顺序边界)
16. 对照 Redis? (会话级 vs 全局单线程)

## 学生视角

17. 什么是处理器链? (请求逐级处理管线)
18. 什么是事务生成? (校验后产生要广播的操作)
19. 什么是刷盘? (事务日志写磁盘)
20. 读写分离? (读不等写提交)
