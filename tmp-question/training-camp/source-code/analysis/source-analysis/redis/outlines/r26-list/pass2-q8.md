# 闭环笔记 q8: handleClientsBlockedOnKeys — 消费与重处理

## 假设
消费 = 防递归 + 新列表交换 (BLMOVE 连环唤醒) + 类型匹配 + unblock 重处理。

## 验证过程
- handleClientsBlockedOnKeys (blocked.c:306-350):
  - **防递归** (L310-313): in_handling_blocked_clients 标志 (公平性保护)
  - **新列表交换** (L322-348): server.ready_keys 换新列表 — 处理中 signalKeyAsReady (BLMOVE 连环) 入新列表, 外层 while 继续
  - 每键: 先 db->ready_keys dict 摘除 (L338, 防重标记清除) → handleClientsBlockedOnKey
- handleClientsBlockedOnKey (L553-589):
  - **FIFO 顺序** (L555-556 注释 + L563 从头): 先阻塞者先服务
  - **防无限循环** (L567): count = 初始列表长 (重阻塞场景)
  - **类型匹配** (L578-580): `o->type == receiver->bstate.btype` 或 MODULE 或 unblock_on_nokey — 键类型变了不唤醒 (可能被错误类型覆盖)
  - unblockClientOnKey (L631-670): releaseBlockedEntry → unblockClient → **PENDING_COMMAND 重处理** (L648-668: processCommandAndResetClient + module 回调 + queueClientForReprocessing)
- 超时路径: unblockClientOnTimeout (L700) — null 回复
- 调用点: beforeSleep (server.c:1637+, processUnblockedClients 之前)

## 代码类型
Mechanism (消费调度)

## 跨域关联
- R-20 (beforeSleep) / R-28 (processCommandAndResetClient) / R-25 (stream 键类型)

## 结论
消费 = 防递归 + 换列表 (连环唤醒) + FIFO + 类型复核 (防错类型误醒) + 命令重处理闭环 (阻塞命令重新执行拿到数据)。BLMOVE 的连环唤醒由"新列表交换"支持。
源码位置: blocked.c:306-350,553-670
