# 闭环笔记 q2: Prep 预校验 — 事务生成 + 变更暂存

## 假设
Prep = 校验 + 事务生成 + outstandingChanges 暂存三合一。

## 验证过程
- **pRequest2Txn 校验链** (Prep:315-637):
  - checkSession (sessionTracker, 每写)
  - validatePath (路径合法)
  - **checkACL** (CREATE/DELETE/WRITE 权限)
  - **checkQuota** (L387: 超限拒绝)
  - **getRecordForPath** (L165-190): **outstandingChangesForPath 先行** (未提交变更可见!) 树查兜底 — 读-改-写一致性
- **事务生成**: request.setTxn (CreateTxn/SetDataTxn/...) + **addChangeRecord** (outstandingChanges + ForPath map, L198-204)
- **顺序节点**: `path + %010d parentCVersion` (L669-671)
- **multi 预校验+回滚** (L216-251): getPendingChanges (**ZOOKEEPER-1624 父记录注释** L228-235) + rollbackPendingChanges — **multi 原子性在 Prep** (Z-3 交叉确认)
- **closeSession 清扫** (L573-615): outstandingChanges 同步块内调整 ephemeral 集合 (**竞态注释 L581-583** — 防与 in-flight deleteNode 冲突) + **CloseSessionTxn 携带删除路径列表** (L609-611)
- **digest**: precalculateDigest + setTxnDigest (L561-563,634-636)

## 代码类型
Implementation (预校验 + 事务生成)

## 跨域关联
- Z-3: outstandingChanges 与树交互 (ChangeRecord)
- Z-5: checkSession (会话有效期)
- Z-9: 事务日志 (Sync 下游)

## 结论
Prep = 校验链 (session/ACL/quota/路径) + 事务生成 + outstandingChanges 暂存; multi 原子性在此; closeSession 预计算清扫。
源码位置: PrepRequestProcessor.java:165-251,315-637,639-674
