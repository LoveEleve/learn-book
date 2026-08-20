# 闭环笔记 q7: 写扇出扇入 — 分发/处理/归零

## 假设
写扇出扇入 = 分发到 N 个线程列表 → 各线程 writeToClient → 主线程等 pending 归零 → 装写处理器。

## 验证过程
- 入口 (networking.c:4393-4484):
  - 空列表 → 0 (L4395); 单线程或 stopThreadedIOIfNeeded → 同步路径 handleClientsWithPendingWrites (L4399-4401)
  - 需要时 startThreadedIO (L4404)
  - **分发** (L4407-4437): CLIENT_PENDING_WRITE 清标; CLIENT_CLOSE_ASAP 跳过 (L4417-4421); **从库客户端强制 list[0] (主线程)** (L4427-4431, 共享 repl 缓冲线程安全); 普通客户端 item_id % num (L4435-4436)
  - **扇出** (L4439-4446): io_threads_op = WRITE (L4441) → 各线程 setIOPendingCount(j, 各自列表长度)
  - **主线程也干活** (L4449-4456): 处理 list[0] (从库 + 部分普通) → 清空
  - **扇入** (L4458-4464): 循环累加 getIOPendingCount 直到 0
  - 收尾 (L4466-4482): op = IDLE (L4467); 重跑列表装写处理器 (clientHasPendingReplies → installClientWriteHandler); 清空 pending_write 列表; stat_io_writes_processed
- 互斥保证: 主线程在 pending 归零前不碰其他线程列表 (注释 L4244-4245: "the main thread will never touch our list before we drop the pending count to 0")
- 写前检查 (L1499, 2034): io_threads_op == IDLE 才允许修改 client 状态

## 代码类型
Mechanism (扇出扇入)

## 跨域关联
- R-28 (writeToClient/installClientWriteHandler) / R-9 (从库缓冲共享)

## 结论
写扇出扇入 = **主线程等全部线程完成**的严格同步: pending 计数是唯一通信 (atomic, cache-line 对齐)。从库强制主线程是共享 repl 缓冲的线程安全要求 (R-9 的 replBufBlock 共享)。每 16 键淘汰时的 flushSlavesOutputBuffers (R-23) 也走这里。
源码位置: networking.c:4393-4484
