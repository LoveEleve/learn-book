# 闭环笔记 q3: WriteLock — 幂等创建 + 前驱监听 + 重试面

## 假设
锁 = 选举同算法 ("exclusive write lock or to elect a leader", WriteLock:36-37); 创建幂等; 重试只在连接层。

## 验证过程
- **幂等创建** (L185-201): prefix = **"x-" + sessionId + "-"** (L216) → findPrefixInChildren 先扫描子节点找 "x-\<sid\>-" 前缀 — **中途失败恢复** (create 成功但响应丢失 → 重试不重复建) (L216-218 注释锚)
- **主循环** (L212-258 do-while): id==null 才创建; getChildren → TreeSet\<ZNodeName\> 显式排序 (L228 注释 "they do seem to come back in order ususally :)") → **ownerId = sortedNames.first()** (L233) → **lessThanMe = headSet(idName)** (L234)
  - 有前驱: **exists(前驱, new LockWatcher())** (L239) — stat!=null → return FALSE (等删除事件 → LockWatcher → lock() 重跑, L156-168); stat==null → **只 log.warn 不重试** (L243) ⚠
  - 无前驱: isOwner() 确认 → **lockAcquired() 回调** (L246-252)
  - 子节点空: id=null 强制重建 (L223-227)
- **重试面** (ProtocolSupport:121-140): **RETRY_COUNT=10** (L41) + **retryDelay=500ms** (L45) + 线性退避 attempt×500 (L192-200); **SessionExpired → 立即重抛** (L127-129) / **ConnectionLoss → 退避重试** (L130-136)
- **unlock** (L119-149): delete(id, -1) + **lockReleased() 回调** (finally, L142-146); "不重试 delete — ZK 会清理 ephemeral" (L122-124 注释锚)
- **ensurePathExists** (ProtocolSupport:148-176): exists→create 幂等; **异常只 warn 吞掉** (L173-175)
- **isOwner** (L289-291): id != null && id.equals(ownerId)

## 代码类型
Implementation (原语 + 重试)

## 跨域关联
- Z-5: sessionId 前缀 (会话标识 → 幂等键)
- Z-6: exists watch 语义 — ⚠ NONODE 时注册的是**创建 watch** (ZooKeeper ExistsWatchRegistration:321-323) — 前驱名唯一永不再创建 → 该 watch 永不触发
- Z-7: retryOperation 客户端面 (对照 ClientCnxn 连接重试 — 业务重试在此层, ZK 客户端不重试业务)

## 结论
锁 = 顺序节点 (x-\<sid\>-) + 幂等恢复 + 前驱 watch + 连接层重试 (10 次线性退避); 会话过期直抛不吞。
源码位置: WriteLock.java:119-258,289-291; ProtocolSupport.java:41-45,121-140,148-176
