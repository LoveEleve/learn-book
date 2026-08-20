# 闭环笔记 q7: signalKeyAsReadyLogic — 就绪队列

## 假设
就绪信号 = 三快速返回 (类型门槛/无阻塞者/nokey 特判) + ready_keys dict 防重 + 入 server.ready_keys 列表。

## 验证过程
- signalKeyAsReadyLogic (blocked.c:447-494):
  - **快速返回 1: 类型可阻塞性** (L451-455): getBlockedTypeByType (L430-438, OBJ→BLOCKED 映射) — 类型从不阻塞 (如 STRING) → 免
  - **快速返回 2: 无阻塞者** (L456-463): `!blocked_clients_by_type[btype] && !blocked_clients_by_type[BLOCKED_MODULE]` — 分类型计数 O(1) 快检 (q5 的 by_type 计数消费点!)
  - **快速返回 3: 键无等待者** (L465-474): deleted 时查 blocking_keys_unblock_on_nokey (L467-468); 否则查 blocking_keys (L472-473)
  - **ready_keys dict 防重** (L476-486): db->ready_keys[key] 存在 → 已排队免重 (脚本/MULTI 多 push 场景注释 L442-444)
  - **入队** (L488-493): server.ready_keys 列表 + refcount
- signalKeyAsReady (L542, 非删) / signalDeletedKeyAsReady (L546, 删)
- 消费端: handleClientsBlockedOnKeys (beforeSleep 前)

## 代码类型
Mechanism (就绪队列)

## 跨域关联
- R-21 (signalKeyAsReady 调用面 db.c:192) / R-20 (beforeSleep) / R-2 (事件循环)

## 结论
就绪队列 = 三级快检 (类型/无阻塞者/无等待者) + dict 防重 (同键一次排队)。分类型计数 (q5) 在这里兑现价值: 无该类型阻塞者时 O(1) 免排队。防重让 MULTI/脚本内多 push 只唤醒一次。
源码位置: blocked.c:430-494
