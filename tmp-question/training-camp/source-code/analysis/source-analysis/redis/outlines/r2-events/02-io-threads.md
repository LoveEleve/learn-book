# R-2 下 — IO 多线程: 扇出扇入与互斥锁栅栏

> 前置: [[R-2-上]] (事件循环) + [[R-20-server]] (initThreadedIO) | 引出: [[R-28-networking]] (读写回调消费) + [[R-9-replication]] (从库共享缓冲) | 对照: [[R-2-上]] (单线程执行模型不变)
> 🔴 A | 3 KP | [模式: 扇出扇入+互斥锁栅栏+强制主线程]
> Pass 2 闭环: q6(生命周期) q7(写扇出扇入) q8(读扇出扇入)

**读者处境**: Redis 号称单线程, 为什么有 io threads?线程怎么被"启停"?为什么一万个写请求还能保序?从库客户端为什么特殊?这篇拆 IO 多线程: 互斥锁启停、写扇出扇入、读延期五条件。

### 1. 生命周期 — 互斥锁即启停信号

场景: io 线程什么时候存在?怎么睡怎么醒?
源码路径:
- 配置 (config.c:3149): io-threads 1-128 默认 1 (单线程), **IMMUTABLE 启动定**; io-threads-do-reads 默认 0 (L3051)
- **initThreadedIO** (networking.c:4295-4325): ==1 不 spawn; >128 退出; 每额外线程创建时**锁住互斥锁** (L4319-4320) — 线程诞生即阻塞
- **IOThreadMain** (L4248-4284): 自旋等 pending>0 (L4260); 等不到 → 尝试锁 (L4267) — 锁住 = 被停止, 阻塞睡; 锁不住 = 继续自旋
  - 工作循环: 按 io_threads_op 处理 list[id] (写/读) → 清空 → pending=0
- **startThreadedIO** (L4347): 解锁全部 → active=1 — 线程醒来
- **stopThreadedIO** (L4354): 先清残留 reads → 锁全部 → active=0
- **惰性停** (stopThreadedIOIfNeeded L4373-4384): pending < num×2 → 停 — 小流量免线程开销
- 对齐细节 (L4217-4223): threads_pending 原子变量按 CACHE_LINE_SIZE (64/128) 对齐 — 防伪共享
关键设计 (q6): 互斥锁 = **双向栅栏**: 启动解锁、停止上锁; 自旋为主、锁为兜底。[模式: 互斥锁栅栏]
数据流: init 锁住 → start 解锁 → 处理 → stop 上锁 → 线程睡。

### 2. 写扇出扇入 — 分发/处理/归零

场景: 一万个客户端要回复, 怎么并行写?
源码路径:
- 入口 (L4393-4484): 单线程或 stopThreadedIOIfNeeded → 同步路径 (L4399-4401)
- **分发** (L4407-4437): CLOSE_ASAP 跳过; **从库客户端强制 list[0] (主线程)** (L4427-4431 — 共享 repl 缓冲线程安全); 普通 item_id % num (L4435)
- **扇出** (L4439-4446): op=WRITE → 各线程 setIOPendingCount
- **主线程也干** (L4449-4456): 处理 list[0] → 清空
- **扇入** (L4458-4464): 循环累加 pending 直到 0 — 主线程在归零前不碰其他列表 (L4244-4245 注释)
- 收尾 (L4466-4482): op=IDLE → 重跑装写处理器 (clientHasPendingReplies) → 清列表 → stat_io_writes_processed
关键设计 (q7): 扇出扇入 = **pending 计数唯一通信** (atomic + cache-line 对齐), 严格同步无锁竞争; 从库强制主线程 = 共享缓冲的必然。[模式: 扇出扇入]
数据流: pending_write 列表 → 分发 N 路 → 线程 writeToClient → 归零 → 装写处理器。

### 3. 读扇出扇入 — postpone 五条件

场景: 读也能并行?哪些客户端不能?
源码路径:
- **postponeClientRead** (L4491-4510) 五条件:
  1. io_threads_active 2. io_threads_do_reads (默认关) 3. !ProcessingEventsWhileBlocked (阻塞中禁用, #6988)
  4. !(MASTER|SLAVE|BLOCKED) (主从/阻塞客户端直读) 5. io_threads_op == IDLE
- 调用点 (L2662): readQueryFromClient 开头 — 满足则入 pending 队列直接 return
- **handleClientsWithPendingReadsUsingThreads** (L4518-4560): 分发 % num → op=READ → 扇出 → 主线程 list[0] → 归零 → IDLE
- 线程内 = readQueryFromClient (L4276): **读 + parse 第一命令入缓冲** — 命令执行仍在主线程
- stopThreadedIO 先处理残留 (L4354)
- 核心不变式: **IO 可并行, 执行仍单线程** — 读线程化只加速"读+解析"
关键设计 (q8): 读延期 = 可读回调"排队"而非"干活", 事件循环批量扇出; 五条件排除所有语义敏感场景。[模式: 延期批处理]
数据流: 可读事件 → postpone? → pending_read 队列 → 批量扇出 → 线程 parse → 主线程执行命令。

### 负面空间 — IO 多线程刻意不做的事

- **不做命令并行执行**: 执行模型始终单线程 (io threads 只读/写+解析)
- **不做写线程池负载均衡**: 简单轮询分发 (item_id % num), 无动态权重
- **不做读线程持久化**: do_reads 默认关, 需显式开启且 IMMUTABLE
- **不做线程动态扩缩**: 线程数启动固定 (1-128), 只有"启/停"两态
- **不做多线程写共享结构**: 从库强制主线程 (repl 缓冲), 不解决共享写

→ 引出: 读写回调怎么实现?→ [[R-28-networking]]
