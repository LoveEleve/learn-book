# R-2 事件驱动+IO 多线程 — Pass 1 探索笔记

> 域: R-2 事件驱动+IO 多线程 (ae.c 家族 + networking io threads) | 🔴 A 方案 | 2026-08-13
> 源码: ae.c (493) + ae.h (115) + ae_epoll.c (118) + ae_kqueue.c (190) + ae_select.c (89) + networking.c io threads 段 (L4180-4560) | Redis 7.4.2

## 调用图

```
main (server.c:7251) → aeMain (ae.c:474): while(!stop) aeProcessEvents(AE_ALL_EVENTS|BEFORE_SLEEP|AFTER_SLEEP)

aeProcessEvents (ae.c:342):
  - 等待超时计算: DONT_WAIT → 0; TIME_EVENTS → usUntilEarliestTimer (L245, O(N) 单链表); 否则 NULL 无限等
  - beforesleep (L359-360) → aeApiPoll (L380, 可被 beforesleep 改 flags)
  - aftersleep (L388-389) → 事件分派 (L391-443):
      读先于写 (L416-420); AE_BARRIER 逆序 (L408, 432-440); 同 proc 只调一次 (L424)
  - processTimeEvents (L446-447)

时间事件 (L200-325): 无序单链表 + nextId 递增 + refcount (递归防释放) + maxId (防迭代中新事件) + 返回 ms 重排 when

多路复用抽象 (ae.h 编译期 include, L31-43):
  六接口: aeApiCreate/Resize/Free/AddEvent/DelEvent/Poll/Name
  epoll (ae_epoll.c): EPOLLIN→READABLE, EPOLLOUT→WRITABLE, ERR/HUP→双触发 (L102-105);
    ADD-MOD 选择 (L58-59), DEL 时 mask==NONE (L79-84)

io threads (networking.c):
  initThreadedIO (L4295): io_threads_num==1 不 spawn; >128 退出 (L4305); 互斥锁初始锁定 (线程停)
  IOThreadMain (L4248): 自旋等 pending>0 (L4260-4264) → 处理 io_threads_list[id] → pending=0
  startThreadedIO (L4347): 解锁全部 → active=1
  stopThreadedIO (L4354): 先处理 pending reads → 上锁全部 → active=0
  stopThreadedIOIfNeeded (L4373): pending < num*2 → 停 (惰性停)
  写扇出扇入 (L4393): 分发 item_id%num (L4436); 从库客户端强制 main (L4427-4431);
    主线程处理 list[0] (L4456) → 等全 pending 归零 (L4460-4464) → op=IDLE → 装写处理器
  读扇出扇入 (postponeClientRead L4491): 五条件 (active && do_reads && !ProcessingEventsWhileBlocked
    && !(MASTER|SLAVE|BLOCKED) && op==IDLE)
    handleClientsWithPendingReadsUsingThreads (L4518): 分发 → 线程 readQueryFromClient → 归零
```

## 基本元素分解

1. **事件循环结构**: aeEventLoop (events/fired 数组 + timeEventHead + beforesleep/aftersleep + flags)
2. **文件事件**: 注册/删除/重注册 (mask 合并, maxfd 维护)
3. **主循环**: aeProcessEvents — 等待超时计算 → sleep 钩子 → poll → 分派
4. **事件分派顺序**: 读先写后 / BARRIER 逆序 / 同 proc 去重 / resize 后刷新指针
5. **时间事件**: 无序链表 O(N) / nextId / refcount / maxId / 周期重排
6. **多路复用抽象**: 六接口 + 三后端 (epoll/kqueue/select) 编译期选择 + evport
7. **io threads 生命周期**: 互斥锁启动/停止 + 128 上限 + 默认 1 (单线程)
8. **扇出扇入**: 写 (clients_pending_write) / 读 (clients_pending_read) — 分发→处理→归零
9. **线程安全约束**: 从库客户端强制 main / 互斥锁即栅栏 / op 标志

## 标记问题 (10 个)

1. aeProcessEvents 等待超时怎么算 (三种模式)?
2. 分派顺序为什么读先于写?BARRIER 为什么逆序?
3. 时间事件为什么无序链表?refcount/maxId 防什么?
4. 多路复用六接口的抽象边界?epoll 的 ERR/HUP 为什么映射双触发?
5. io threads 怎么用互斥锁当"停止信号"?
6. 扇出扇入的具体时序 (分发/处理/归零)?
7. 为什么 pending < num×2 就停线程?
8. 从库客户端为什么必须主线程处理?
9. 读线程化的五条件?ProcessingEventsWhileBlocked 为什么禁用?
10. io-threads-do-reads 默认关的原因?

## 时空溯源 (代码内痕迹)

- 2006 (antirez): ae 初版 — "Originally I wrote this code for the Jim's event-loop" (Tcl 解释器)
- 2009: epoll 后端 (ae_epoll.c 版权)
- 6.0: **io threads 引入** (networking.c, 扇出扇入)
- 6.2+: io-threads-do-reads 可选 (默认关); CACHE_LINE_SIZE 对齐 threads_pending (L4219-4223)
- 演进: aeResizeSetSize (动态扩容) / AE_BARRIER (beforeSleep fsync 后写) / 单调时钟 (getMonotonicUs 替代 gettimeofday)

## 大域拆分规划 (01 §大域)

8 闭环 → **2 篇**:
- 篇 1 (事件循环内核): q1 (结构与注册) + q2 (主循环与等待) + q3 (分派顺序) + q4 (时间事件) + q5 (多路复用抽象)
- 篇 2 (IO 多线程): q6 (生命周期) + q7 (写扇出扇入) + q8 (读扇出扇入)
