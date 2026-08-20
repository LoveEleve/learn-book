# R-2 上 — 事件循环内核: 从 epoll 到分派顺序

> 前置: [[R-20-server]] (aeCreateEventLoop L2657/aeMain L7251/beforeSleep) + [[R-33-zmalloc]] (分配) | 引出: [[R-2-下]] (io threads) + [[R-28-networking]] (读写回调挂接) | 对照: [[R-20-server]] (beforeSleep 挂钩)
> 🔴 A | 5 KP | [模式: fd 索引数组+睡眠编排+顺序契约+惰性删除]
> Pass 2 闭环: q1(结构与注册) q2(主循环与等待) q3(分派顺序) q4(时间事件) q5(多路复用抽象)

**读者处境**: redis-server 是怎么"同时"服务上万连接的?为什么读事件总在写事件之前?AE_BARRIER 是什么黑魔法?serverCron 每 100ms 跑一次, 谁在催它?这篇拆事件循环内核: fd 索引事件表、睡眠编排、读先写后、时间事件链表、三后端抽象。

### 1. 事件表 — fd 即下标的双数组

场景: 一万个连接怎么管理监听?
源码路径:
- 结构 (ae.h:78-90): events[] (fd 直接索引) + fired[] (本轮触发) + timeEventHead + maxfd + setsize
- **aeCreateEventLoop** (ae.c:46-78): setsize = maxclients+CONFIG_FDSET_INCR (server.c:2657) → 双数组 + aeApiCreate
- **aeCreateFileEvent** (L143-161): mask 合并 + r/w proc 分挂 + maxfd 提升; fd ≥ setsize → ERANGE 拒绝
- **aeDeleteFileEvent** (L163-183): 删 WRITABLE 连带删 BARRIER (L169-171); maxfd 回退扫描
- 掩码 (ae.h:20-27): NONE=0 / READABLE=1 / WRITABLE=2 / **BARRIER=4** (读后不写屏障)
关键设计 (q1): fd 直接索引数组 = 注册/删除 **O(1)** 无哈希; mask 合并支持同 fd 多事件。[模式: 索引数组]
数据流: 新连接 → aeCreateFileEvent(fd, READABLE) → events[fd] → poll 消费。

### 2. aeProcessEvents — 睡眠编排

场景: 事件循环怎么决定睡多久?
源码路径:
- **三种等待** (ae.c:367-377): DONT_WAIT → 立即返回; TIME_EVENTS → 睡到最早时间事件 (usUntilEarliestTimer L245, O(N)); 否则无限等
- poll 条件 (L353-354): 有文件事件或 (要处理时间事件且可等) — 只有时间事件也要 poll 来睡
- beforesleep (L359) → aeApiPoll (L380) → aftersleep (L388) — 每轮三明治 (beforesleep 内含 AOF flush/FAST 过期/客户端写, R-20/R-22/R-28)
- DONT_WAIT 可被 beforesleep 侧写 (L362-367, 参数优先)
- aeMain (L474-481): while(!stop) 全事件类型 — 唯一主循环
- processEventsWhileBlocked (networking.c:4169-4211): 阻塞中 4 次喂事件 (加载/脚本)
关键设计 (q2): 睡眠 = **"睡到最早事件"**: 时间事件决定最迟唤醒, 文件事件提前唤醒。[模式: 睡眠编排]
数据流: aeMain → 算等待 → beforeSleep → poll → 事件分派 → 时间事件。

### 3. 分派顺序 — 读先写后与 BARRIER

场景: 同一个 fd 读写都触发, 先调谁?
源码路径:
- **读先于写** (ae.c:416-428): 可立即应答刚读的请求 (L397-401 注释)
- **AE_BARRIER 逆序** (L408, L432-440): 写先读后 — beforeSleep 先 fsync 再统一应答 (L402-407 注释, AOF 持久化优先)
- **同 proc 去重** (L424): rfileProc == wfileProc → 一次调用处理双事件
- **回调安全** (L416,419): mask 复查 (回调可能删事件) + resize 后刷新 fe 指针
- 触发掩码: EPOLLERR/HUP → WRITABLE|READABLE 双触发 (ae_epoll.c:104-105) — 断开也会唤醒读回调
关键设计 (q3): 顺序契约 = **读先写后** (吞吐) / **BARRIER 逆序** (持久化) 双模式; 回调内可改事件表。[模式: 顺序契约]
数据流: fired[j] → invert? → 读/写回调 → 回调内可重注册。

### 4. 时间事件 — 无序链表与双重防重入

场景: 定时任务怎么调度?
源码路径:
- 注册 (ae.c:200-221): nextId 递增 + 头插 + when = 单调时钟 + ms×1000
- **惰性删除** (L223-234, L273-295): 标记 AE_DELETED_EVENT_ID, 迭代时摘链
- **usUntilEarliestTimer** (L245-258): O(N) 扫最早 (注释: 有序插入/跳表是可选优化, Redis 不需要)
- **双重防重入** (L267, L302-305: maxId 防迭代中新事件被处理; L311-313: refcount 防递归释放)
- **周期重排** (L316-317): 回调返回 ms → when += retval×1000 — serverCron 返回 1000/hz (R-20), evictionTimeProc (R-23 淘汰续清) 同挂时间事件
- 单调时钟 (monotonic.h) — 不受 NTP 跳变影响
关键设计 (q4): 无序链表 + 回调返回周期 = **零插入成本**, O(N) 扫描在事件少时可接受 (注释明示权衡)。[模式: 惰性管理]
数据流: serverCron 到期 → 调用 → 返回 1000/hz → when 重排 → 下一轮。

### 5. 多路复用抽象 — 编译期特化三后端

场景: epoll 和 select 怎么共存?
源码路径:
- 编译期选择 (ae.c:29-43): evport → epoll → kqueue → select (性能降序)
- 六接口 (ae_epoll.c): Create/Resize/Free/AddEvent/DelEvent/Poll/Name — 静态函数, 无虚表
- **epoll 细节**: epoll_create(1024) 只是内核提示 (L27); ADD vs MOD 按 mask (L58-59); **EPOLLERR/HUP → 双触发** (L104-105); DEL 时 mask 空才 EPOLL_CTL_DEL (L79-84)
- kqueue 190 行 / select 89 行 (同接口异实现)
- 超时转换 (L92-93): timeval → ms 向上取整
关键设计 (q5): 编译期特化 = **零运行时开销** (无函数指针间接); epoll 后端承载 Linux 生产环境。[模式: 编译期特化]
数据流: aeApiPoll → epoll_wait → fired 数组 → 分派。

### 负面空间 — 事件循环刻意不做的事

- **不做事件优先级**: 所有文件事件同等 (fired 顺序 = 内核返回顺序)
- **不做定时器红黑树**: 无序链表 O(N) (事件数少, 注释明示权衡)
- **不做跨线程事件投递**: 单线程执行模型 (io threads 只做 IO, R-2 下)
- **不做 epoll ET 模式**: 全程 LT (水平触发), 无边沿复杂度
- **不做事件队列持久化**: 每轮 poll 拉取, 无积压缓冲

→ 引出: IO 怎么线程化?→ [[R-2-下]]
