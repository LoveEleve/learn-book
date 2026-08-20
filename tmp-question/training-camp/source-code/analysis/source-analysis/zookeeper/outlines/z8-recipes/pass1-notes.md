# Z-8 Recipes — Pass 1 探索笔记

> 域: Z-8 Recipes | 🟡 B 方案 (无 harness) | 2026-08-15
> 源码: zookeeper-recipes/ 三模块 (election 471/lock 303+202+144+41+40/queue 302) | ZooKeeper 3.9.5
> 注: 官方 recipes 为 **LeaderElectionSupport/WriteLock/DistributedQueue** (09 审计修正: LeaderLatch/InterProcessMutex 是 Curator 类)

## 调用图

```
选举: start() → makeOffer (create n_ EPHEMERAL_SEQUENTIAL) → determineElectionStatus
        → getChildren(rootNode) → 排序 → 最小序号 = leader / 否则 exists(前驱, this)
        → 前驱 NodeDeleted → process() → determineElectionStatus() 重跑
锁:   lock() → retryOperation(zop) [SessionExpired 直抛 / ConnectionLoss 重试×10 线性退避]
        → zop.execute(): id==null → findPrefixInChildren("x-<sessionId>-") 幂等恢复
        → getChildren → TreeSet 排序 → ownerId=first → headSet(idName)
        → 有前驱: exists(前驱, LockWatcher) → 等删除事件 → lock() 重跑
        → 无前驱: lockAcquired() 回调
队列: offer → create qn- PERSISTENT_SEQUENTIAL; remove/element → getChildren → TreeMap<Long> 排序
        → 最小序号 getData+delete; take → 空则 LatchChildWatcher.await 阻塞等变更
```

## 基本元素分解

1. **共同模式**: 顺序节点 (EPHEMERAL_SEQUENTIAL/PERSISTENT_SEQUENTIAL) + 最小序号判定 + 前驱 watch
2. **LeaderElectionSupport**: 事件驱动状态机 (12 EventType / 7 State) + LeaderOffer (id/path/hostName)
3. **WriteLock**: 幂等创建 (sessionId 前缀) + 前驱监听 + ProtocolSupport 重试面 (RETRY_COUNT=10)
4. **DistributedQueue**: FIFO 顺序 (TreeMap<Long>) + 阻塞 take (LatchChildWatcher) + 并发消费

## 标记问题 (20 问)

1. 三 recipes 共性? (顺序节点+最小序号+前驱 watch)
2. 选举判定怎么做的? (排序找自己位置)
3. 前驱 watch 怎么注册? (exists 带 watcher)
4. 前驱消失竞态? (getChildren 与 exists 之间)
5. 会话失效怎么处理? (NodeDeleted 排除自身 / SessionExpired 直抛)
6. 状态机? (7 State / 12 EventType)
7. 锁的幂等创建? (findPrefixInChildren sessionId 前缀)
8. 重试面? (RETRY_COUNT=10 线性退避)
9. SessionExpired vs ConnectionLoss 处理差异?
10. ownerId 判定? (sortedNames.first + isOwner)
11. 无前驱时? (lockAcquired 回调 / isOwner 检查)
12. unlock 语义? (delete + lockReleased 回调)
13. ZNodeName 排序? (序号优先 + 前缀兜底 + 负序号)
14. 队列 FIFO 怎么保证? (TreeMap<Long> 按序号)
15. take 阻塞? (LatchChildWatcher await)
16. 目录不存在? (NoNode → create(dir) 自举)
17. 并发消费竞态? (getData+delete NoNode 重试)
18. 父目录创建竞态? (双 takers 同时 create → NodeExists)
19. 对照 Curator? (LeaderLatch/InterProcessMutex 同模式不同实现)
20. 序号溢出? (Integer vs Long vs %010d 10 位)

## 时空溯源 (代码内注释锚)

- WriteLock:122-124 "ZK will remove ephemeral files" — 临时节点自动清理语义 (3.4 锚)
- WriteLock:216-218 "look up the current ID if we failed in the middle of creating" — 幂等创建恢复 (3.4 锚)
- WriteLock:228 "they do seem to come back in order ususally :)" — 排序显式化 (3.4 锚)
- ZNodeName Optional + parseSequenceString — Java 8 Optional 现代化 (3.5+ 锚)
- WriteLockTest:142-143 "workAroundClosingLastZNodeFails ... due to bug!" — 已知缺陷 workaround
- LeaderElectionSupport:66-87 Javadoc caveats "best effort" — 边界承认

## 大域拆分判断

Z-8 = 三 recipes 统一模式; 单篇 🟡 B (8 闭环 q1-q4 + 验证); 深度 = 竞态面 (前驱消失/会话失效/并发消费) + 对照 Curator

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "LeaderElectionSupport (471)/WriteLock (303)/DistributedQueue (302)" | 行数全实证 (471/303/302); lock 模块另有 ProtocolSupport 202 + ZNodeName 144 + LockListener 41 + ZooKeeperOperation 40 | **接受+补充** ✅ |
| "共同模式 = 顺序节点 + watch 前驱" | 三 recipes 全实证 (LES n_/WriteLock x-<sid>-/queue qn-) | **接受** ✅ |
| "对照 Curator (4.5: 同功能不同实现)" | Curator 4.5 LeaderLatch/InterProcessMutex 同模式 (公知对照, 阶段 4.5 再深入) | **接受** ✅ |
| 数字: RETRY_COUNT | =10 (ProtocolSupport:41) | **补充** ✅ |
| 数字: retryDelay | =500ms (ProtocolSupport:45); 退避 = attempt×500 线性 | **补充** ✅ |
| 数字: 序号位宽 | LES/ZNodeName Integer (32 位) vs DQueue Long (64 位) — 不一致 | **补充** ✅ |
