# 闭环笔记 q2: 写操作 — createNode/deleteNode/setData 校验链

## 假设
写 = 父锁 + digest + 分类维护 + watch 触发四步; version 乐观锁。

## 验证过程
- **createNode** (DataTree:433-522):
  1. 路径拆分 (lastSlash → parentName/childName, L434-436)
  2. **synchronized(parent) 父节点锁** (L443)
  3. **ACL 先入缓存** (aclCache.convertAcls — fuzzy snapshot race 注释 L446-457)
  4. 已存在 → **NodeExistsException** (L459-463)
  5. **preChange/postChange** (digest 钩子, L465/481)
  6. **parent.cversion/pzxid 递增** — 仅当 parentCVersion > 现有 (replay 模糊窗口保护注释 L470-478)
  7. parent.addChild + **nodes.put(path, child)** + nodeDataSize.addAndGet (L480-483)
  8. **ephemeral 分类**: CONTAINER→containers / TTL→ttls / 普通→ephemerals.computeIfAbsent (L484-494)
  9. **quota 更新**: pTrie.addPath (limit 节点) + updateQuotaStat (L500-517)
  10. **双 watch**: dataWatches NodeCreated + childWatches NodeChildrenChanged (L519-521)
- **deleteNode** (L533-625): parent.removeChild + **pzxid 保护 (zxid > pzxid 才更新 — 防 CreateTxn 覆盖注释 L548-553)** → nodes.remove + **aclCache.removeUsage** (L565) → 分类清理 (L575-589) → quota 递减 → **三 watch** (NodeDeleted ×2 + NodeChildrenChanged, L621-624)
- **setData** (L627+): version 校验 → stat 更新 (mzxid/mtime/version++) → watch
- **setACL** (L757+): version 校验 + ACL 缓存引用管理

## 代码类型
Implementation (校验链)

## 跨域关联
- Z-6: watch 触发 (data/child 管理器)
- Z-9: fuzzy snapshot (快照窗口一致性)

## 结论
写操作 = 父锁 → ACL 先入 → digest → 父 stat 单调更新 → 分类维护 → quota → watch 触发; version 乐观锁; 三写 API (create/delete/setData) 共享四步骨架。
源码位置: DataTree.java:433-522,533-625,627-757
