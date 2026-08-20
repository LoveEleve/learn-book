# 闭环笔记 q3: 触发链 — PathParentIterator + 一次性移除

## 假设
触发即移除 (STANDARD); 递归父路径; suppress 去重。

## 验证过程
- **triggerWatch** (WatchManager:140-199):
  1. **PathParentIterator 迭代** (L144): 路径自身 + 父路径逐级 (递归 watch 面)
  2. 逐 watcher: **STANDARD → removeMode + iterator.remove (触发即移除)** (L161-167); **PERSISTENT_RECURSIVE → 父路径保持** (L168-170); stats null → 不一致警告 (L155-157)
  3. **watchers 集合去重** (L142: HashSet) — 同 watcher 多路径只触发一次
  4. 空路径清理 (L172-174)
- **suppress** (L185-187): WatcherOrBitSet.contains → 跳过 (Z-3 deleteNode 双 NodeDeleted 用)
- **分发** (L188-192): ServerWatcher.process(e, acl) / Watcher.process(e) → **ServerCnxn.process → 事件入队投递客户端** (Z-7)
- **metrics** (L195-199): NODE_CREATED_WATCHER/NODE_DELETED_WATCHER/DATA_WATCHES 等计数
- **DataTree 触发点** (Z-3): createNode (NodeCreated + NodeChildrenChanged) / deleteNode (NodeDeleted ×2 + NodeChildrenChanged) / setData

## 代码类型
Implementation (触发 + 移除 + 分发)

## 跨域关联
- Z-3: triggerWatch 调用点 (L519-521,621-624)
- Z-7: ServerCnxn 事件投递

## 结论
触发 = 父路径迭代 + STANDARD 即移除 + PERSISTENT_RECURSIVE 保持 + 去重 + suppress; 分发经 ServerCnxn。
源码位置: WatchManager.java:140-199
