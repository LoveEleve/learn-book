# 闭环笔记 q3: 事务应用 — processTxn 分发

## 假设
广播提交的事务经 processTxn 分发到树 API; multi 子事务循环。

## 验证过程
- **三层入口** (DataTree:845-855): `processTxn(header, txn, digest)` (带 TxnDigest) / `processTxn(header, txn)` / `processTxn(header, txn, isSubTxn)`
- **OpCode 分发** (L865-960):
  - **create/create2/createTTL/createContainer** → createNode (ephemeralOwner 区分)
  - **delete/deleteContainer** → deleteNode
  - **reconfig/setData** → setData
  - **setACL** → setACL
  - **multi** → 子事务循环 (isSubTxn=true, L1028)
  - ErrorTxn → 忽略 (L1073 注释)
- **TxnDigest**: 3.5+ 事务级校验 — 快照/日志重放的一致性验证 (digestFromLoadedSnapshot/lastProcessedZxidDigest L177-180)
- **失败语义**: 抛异常 → 上层 (FinalRequestProcessor) 处理; multi 原子性由 PrepRequestProcessor 预校验保证 (Z-4 交叉)

## 代码类型
Implementation (事务分发)

## 跨域关联
- Z-2: COMMIT 后 learner/follower 侧 zk.processTxn (Learner:690,724)
- Z-4: multi 预校验 (Prep) + 应用 (Final)
- Z-9: 日志重放 processTxn

## 结论
processTxn = OpCode switch 分发到树 API (create 族/delete 族/setData/setACL/multi); TxnDigest 校验; 同一 API 被提交回放与写路径共享。
源码位置: DataTree.java:845-960,1028,1073
