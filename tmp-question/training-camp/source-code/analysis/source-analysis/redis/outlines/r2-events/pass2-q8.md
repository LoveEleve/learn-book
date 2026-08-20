# 闭环笔记 q8: 读扇出扇入 — postpone 五条件

## 假设
读线程化 = 可读事件回调把客户端"延期"入 pending 队列, 之后批量扇出扇入 parse。

## 验证过程
- **postponeClientRead** (networking.c:4491-4510): 五条件全满足才延期:
  1. io_threads_active (线程已启动)
  2. io_threads_do_reads (配置开, 默认 0)
  3. !ProcessingEventsWhileBlocked (阻塞中禁用, #6988)
  4. !(CLIENT_MASTER|CLIENT_SLAVE|CLIENT_BLOCKED) (主从/阻塞客户端直读)
  5. io_threads_op == IDLE (无并发操作)
  - 满足 → 头插 clients_pending_read + 标记 (L4506-4507)
- 调用点 (L2662): readQueryFromClient 开头 — 被延期则直接 return (事件循环后续统一处理)
- **handleClientsWithPendingReadsUsingThreads** (L4518-4560):
  - 不活跃或未开读 → 0 (L4526); 空队列 → 0
  - 分发 item_id % num (L4536-4540) → op = READ (L4542) → setIOPendingCount 扇出
  - 主线程处理 list[0] (L4553) → 等归零 → op = IDLE
- 线程内处理 = readQueryFromClient (L4276) — 读 + parse 第一命令 (进程内缓冲)
- 执行顺序: 事件循环 afterSleep 后 → handleClientsWithPendingReadsUsingThreads (beforeSleep 前?) — 实际在 processCommand 前由 networking 协调
- stopThreadedIO 时先处理残留 reads (L4354)
- 语义: 读线程化只做"读+解析入缓冲", **命令执行仍在主线程** (单线程执行模型不变)

## 代码类型
Mechanism (读延期批处理)

## 跨域关联
- R-28 (readQueryFromClient) / R-2 (事件循环协调)

## 结论
读扇出扇入 = 可读回调"延期"客户端 → 批量扇出 → 线程读+parse → 扇入 → 主线程执行命令。五条件把主从/阻塞/阻塞中场景排除 (线程安全 + 语义安全)。核心不变式: **IO 可并行, 执行仍单线程**。
源码位置: networking.c:2662,4491-4560
