# 闭环笔记 q4: 快照面 — serialize/deserialize + ZKDatabase

## 假设
快照 = 树序列化 (DFS + 结束标记); 反序列化重建父链/分类; ZKDatabase 桥接存储。

## 验证过程
- **serialize** (DataTree:1331-1349): **serializeAcls + serializeNodes (DFS)**; 每节点: writeString(path) + writeRecord(node); **"/" 结束标记** (L1336-1339); serialize(oa, tag) 组合
- **deserialize** (L1350-1388): aclCache.deserialize → **nodes.clear + pTrie.clear + nodeDataSize.set(0)** → 逐节点循环: nodes.put + **parent.addChild 父链重建** (parent null → IOException "Invalid Datatree" L1371-1373) + **分类恢复** (containers/ttls/ephemerals, L1377-1386) + **aclCache.addUsage** (L1367)
- **ZKDatabase** (806 行): dataTree + **committedLog (ArrayDeque<Proposal> — 最近提案缓存, L98; getmaxCommittedLog/getminCommittedLog — Z-2 DIFF 数据源)** + **snapLog (FileTxnSnapLog — Z-9)** + getLogLock (读写锁)
- **loadDataBase** (L288-305): **snapLog.restore(dataTree, sessionsWithTimeouts, commitProposalPlaybackListener)** → fastForwardFromEdits (日志快进)

## 代码类型
Implementation (序列化 + 恢复)

## 跨域关联
- Z-2: committedLog 是 syncFollower DIFF 数据源
- Z-9: FileTxnSnapLog (快照+日志落盘)
- Z-5: sessionsWithTimeouts 恢复

## 结论
快照 = ACL + DFS 节点序列化 ("/" 结束) ↔ 反序列化重建 (父链/分类/ACL 引用); ZKDatabase 持 committedLog (广播桥) + snapLog (存储)。
源码位置: DataTree.java:1322-1388; ZKDatabase.java:83-116,198-249,288-305
