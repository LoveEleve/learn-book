# 闭环笔记 q6: blockForKeys — 双向注册

## 假设
阻塞注册 = 双向映射: client→keys (bstate.keys) + db→clients (blocking_keys); unblock_on_nokey 引用计数。

## 验证过程
- blockForKeys (blocked.c:359-410):
  - timeout 保留语义 (L364-368): CLIENT_REPROCESSING_COMMAND 时不重置 (原超时保留)
  - **client→key 侧** (L370-375): c->bstate.keys 注册 + refcount
  - **db→client 侧** (L377-388): db->blocking_keys[keys[j]] 的 list 追加客户端; 首客户端建 list (L381-384); **双向节点关联** (L389: bstate.keys 的 value = list node, 供 O(1) 解链)
  - **unblock_on_nokey 计数** (L393-401): blocking_keys_unblock_on_nokey 引用计数 (XREADGROUP 语义)
  - PENDING_COMMAND (L407-408): 非 MODULE 阻塞 → 标记 (重处理用) + blockClient (L409)
- 多键语义: 任一键就绪即唤醒 (阻塞全部注册)
- releaseBlockedEntry (L507-540): 解链 (L516-518) → 空列表清键 (L526-528) → nokey 引用递减 (L529-537) → remove_key 时清 bstate.keys
- unblockClientWaitingData (L414-428): 批量释放全部键

## 代码类型
Mechanism (双向映射)

## 跨域关联
- R-21 (blocking_keys/ready_keys 结构) / R-28 (client 结构)

## 结论
双向注册 = **两字典互链**: client 侧记"等哪些键" (value=list node 供 O(1) 解链), db 侧记"键被谁等" (list 保公平 FIFO)。nokey 变体是引用计数 (多客户端共享)。多键任一触发即全醒。
源码位置: blocked.c:359-410,414-540
