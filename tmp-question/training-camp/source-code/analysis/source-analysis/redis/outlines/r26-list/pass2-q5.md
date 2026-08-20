# 闭环笔记 q5: 阻塞状态机 — blockClient/unblockClient

## 假设
阻塞状态 = CLIENT_BLOCKED 标志 + btype (10 种) + timeout 表; unblock 分两路径 (重处理/超时)。

## 验证过程
- btype 枚举 (server.h:398-407): BLOCKED_NONE/LIST/WAIT/WAITAOF/MODULE/STREAM/ZSET/POSTPONE/SHUTDOWN/LAZYFREE — 10 种
- initClientBlockingState (blocked.c:54-64): bstate 初始化 (keys dict + timeout + unblock_on_nokey)
- **blockClient** (L67-80): CLIENT_BLOCKED 设置 (L75) + btype (L76) + blocked_clients 统计 (L77) + **blocked_clients_by_type[btype]++** (L78, R-26 就绪队列快检用) + **addClientToTimeoutTable** (L79, 超时表)
  - master 客户端限制 (L69-74): 仅 MODULE/LAZYFREE/POSTPONE 可阻 (复制流不能阻)
- **unblockClient** (L164-210): CLIENT_BLOCKED 清除 + btype 复位 + by_type 计数减 + **blocked_clients--** + queue_for_reprocessing → server.unblocked_clients (L195+, beforeSleep 消费 processUnblockedClients L105)
- unblockClientOnTimeout (L700-708): replyToBlockedClientTimedOut (L211, null 回复) + PENDING_COMMAND 清除 (L705-706) + unblockClient (L707)
- 超时表: addClientToTimeoutTable — 事件循环 cron 检查 (serverCron, R-20)

## 代码类型
Mechanism (状态机)

## 跨域关联
- R-20 (serverCron 超时检查) / R-28 (CLIENT_BLOCKED 断点 processInputBuffer)

## 结论
阻塞状态 = 标志 + 类型 + 双计数 (总/分类型)。分类型计数是就绪队列的 O(1) 快检依据。unblock 两路径: 有数据重处理 / 超时 null 回复。master 复制流不可阻 (除特定类型)。
源码位置: blocked.c:54-210,700-708; server.h:398-407
