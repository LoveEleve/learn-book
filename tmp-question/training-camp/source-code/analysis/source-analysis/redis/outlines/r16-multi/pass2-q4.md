# 闭环笔记 q4: WATCH — 嵌入式节点双向注册

## 假设
watchedKey 嵌入 listNode; 客户端列表 + db->watched_keys dict 双向注册; 修改键 → DIRTY_CAS。

## 验证过程
- watchedKey (multi.c:253-259): **listNode 嵌入 (node 字段)** + key/db/client + **expired:1 位域** (WATCH 时已过期标记)
- 双链注释 (L246-252): 客户端侧 = 普通 list (node 值指向 watchedKey); db 侧 = dict 值列表的**嵌入式节点** — "avoid the need for listSearchKey and dictFind when we remove from the list" (L252)
- **O(1) 摘除三内联** (L262-276): watchedKeyLinkToClients (node.value 指回客户端列表) / watchedKeyGetClients / watchedKeyGetClientNode
- watchForKey (L279-310): 客户端列表空 → watching_clients++ (L285); **同键去重** (L288-293, 同 db+同键早退); db 侧 dict 无则建列表 (L295-300); 双向注册 + **双引用** (L299/L307 各 incrRefCount) + **expired = keyIsExpired** (L306)
- unwatchAllKeys (L314-338): 逐键 O(1) 摘除 (L328) + 空列表删 dict 键 (L330-331) + 释放 (L333-335) + watching_clients--
- touchWatchedKey (L359-398): dict 空早退 (L364); **redis_member2struct 从嵌入 node 反解 watchedKey** (L372); **置 DIRTY_CAS + 立即 unwatchAllKeys** (L389-393, "no point in getting here again ... keep the memory overhead till EXEC")
  - **expired 键特例** (L375-387): WATCH 时已过期的键被删除 → 逻辑无变化 → 清标志跳过 (L377-384)
- watchCommand (L452-467): **MULTI 内拒** (L455-458) / DIRTY_CAS 早退回 OK (L460-463) — 已脏没必要 watch
- unwatchCommand (L469-473): 全退 + 清 DIRTY_CAS

## 代码类型
Mechanism (观察者/CAS)

## 跨域关联
- R-21: db->watched_keys dict / keyIsExpired (R-22 惰性过期交叉)
- R-3: dict 复用
- R-17 (未来): trackingInvalidateKey 同源 (signalModifiedKey)

## 结论
WATCH = 嵌入式节点双向注册 (O(1) 增删); touch 即 DIRTY_CAS + 提前退订 (省内存); expired 位处理"已过期键"的边界语义。
源码位置: multi.c:246-310,314-338,359-467
