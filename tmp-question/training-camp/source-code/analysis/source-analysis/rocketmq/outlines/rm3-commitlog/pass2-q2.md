# 闭环笔记 q2: 写后分发 — dispatcherList 链

## 假设
消息写入 CommitLog 后, 同步分发构建 CQ + Index + Compaction。

## 验证过程
- **分发链** (DefaultMessageStore L266-272): dispatcherList = BuildConsumeQueue + BuildIndex (+ 5.x Compaction)
- **触发** (CommitLog L338-352/723-747): doAppend 写后 → onCommitLogDispatch(doDispatch, isRecover, isFileEnd) — **doDispatch && !isFileEnd 才真分发** (DefaultMessageStore L2108-2112, 恢复期跳过/文件尾跳过)
- **doDispatch** (L1989-1995): 遍历 dispatcherList 逐个 dispatch (CQ 先 Index 后 — 顺序链)
- **DispatchRequest** (244 行): 写后提取的中间结构 (topic/queueId/commitLogOffset/msgSize/tagsCode/bitMap/storeTimestamp/consumeQueueOffset) — 分发数据契约
- **重试面**: CQ putMessagePositionInfoWrapper 30 次重试 + isCQWriteable (ConsumeQueue 可写标志)
- **一致性格言** (L418 注释): "eliminating the dispatch inconsistency between the commitLog and consumeQueue at the end of recovery" — 恢复期分发对齐

## 代码类型
Implementation (写后链)

## 跨域关联
- RM-2 (写链): doAppend 后置
- RM-5 (Broker): 分发配置

## 结论
写后分发 = 同步链式 (CQ→Index→Compaction); DispatchRequest 中间契约; 恢复期跳过分发 (靠 recover 对齐); CQ 可写标志限流。
源码位置: DefaultMessageStore.java:169-272,1989-1995,2108-2112; DispatchRequest.java
