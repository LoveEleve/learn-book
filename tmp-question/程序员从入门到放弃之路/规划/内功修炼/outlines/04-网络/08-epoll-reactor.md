# epoll内核实现 + LT/ET + Reactor模型 + io_uring

> Cluster C: 6 KPs | 依赖: 07-IO模型 | 读者基线: select/poll内核实现

---

### 1. epoll内核数据结构 — eventpoll/ep_item/红黑树/就绪链表
  - eventpoll: epoll实例核心结构, rbr(红黑树存所有监视fd), rdllist(就绪fd链表), wq(等待队列) (fs/eventpoll.c → struct eventpoll)
  - epitem: 每个被监视的fd对应一个epitem, 挂在eventpoll的rbr中 (fs/eventpoll.c → struct epitem)
  - 红黑树: O(logN)插入/删除/查找 — 解决select/poll每次遍历O(N)的重注册问题 (fs/eventpoll.c → ep_rbtree_insert)
  - 就绪链表: 数据到达时内核回调将epitem加入rdllist(双向链表), epoll_wait直接从链表取 — O(1)就绪事件返回 (fs/eventpoll.c → ep_poll)
  - 事件掩码: EPOLLIN/EPOLLOUT/EPOLLERR/EPOLLRDHUP, 一次性传入epoll_ctl保存到epitem

### 2. epoll_create/ctl/wait — 三个系统调用的完整交互
  - epoll_create1: 分配eventpoll结构, 创建匿名fd返回 — 分配内存+初始化rbr/rdllist/wq (fs/eventpoll.c → do_epoll_create)
  - epoll_ctl(ADD): 创建epitem→设置回调为ep_poll_callback→插入rbr→调tcp_poll注册到socket等待队列 (fs/eventpoll.c → ep_insert)
  - epoll_ctl(DEL): 从rbr删除epitem→从socket等待队列取消注册→ep_remove_wait_queue (fs/eventpoll.c → ep_remove)
  - epoll_ctl(MOD): 修改events掩码 — 不影响rbr位置, 仅更新epitem.events (fs/eventpoll.c → ep_modify)
  - epoll_wait: 检查rdllist有就绪项→拷贝events数组返回→否则ep_poll睡眠等待 (fs/eventpoll.c → ep_poll)

### 3. ep_poll_callback — 从数据到达到epoll_wait返回的关键回调
  - 调用链: 数据到达→tcp_data_queue→sk_data_ready→sock_def_readable→ep_poll_callback (fs/eventpoll.c → ep_poll_callback)
  - 回调动作: (1)检查epitem.events匹配当前事件, (2)将epitem加入rdllist, (3)wake_up唤醒epoll_wait (fs/eventpoll.c → ep_poll_callback)
  - 关键优化: **仅数据到达时执行** — 不是epoll_wait遍历所有fd, 而是在中断上下文直接入链 (fs/eventpoll.c → ep_poll_callback → list_add_tail)
  - 为什么比select快: select是"轮询检查所有fd"→O(N), epoll是"数据来了通知我"→O(1)返回

### 4. LT(Level Triggered) vs ET(Edge Triggered) — 触发语义本质
  - LT(默认): 只要fd可读(缓冲区有数据)→epoll_wait一直返回该事件 — "有数据就报告" (fs/eventpoll.c → ep_send_events → 检查revents)
  - ET(EPOLLET): 仅状态变化时报告一次(无可读到可读的边沿) — "只在状态变更时报告" (fs/eventpoll.c → ep_item_poll → 设置EPOLLET标记睡眠方式)
  - 内核区别: LT在每次epoll_wait重新调f_op->poll确认状态, ET在ep_poll_callback只通知一次+不重复poll确认 (fs/eventpoll.c → ep_item_poll)
  - ET trap: 必须用非阻塞IO+循环read直到EAGAIN, 否则丢事件(后续数据不再通知) — 这就是为什么所有Reactor都用nonblock+循环读
  - 应用选择: LT更安全(不会丢事件), ET更高性能(减少epoll_wait唤醒次数) — Nginx用ET

### 5. Reactor模型 — epoll之上的编程抽象
  - 单Reactor单线程: 一个线程epoll_wait+处理IO+执行业务(Redis 6.0前) (简单但业务阻塞影响IO)
  - 单Reactor多线程: 主线程epoll_wait分发IO, 线程池处理业务(业务隔离IO) (但读写仍串行)
  - 主从Reactor: 主Reactor仅accept→分发给从Reactor线程->从Reactor负责读写+编解码(Netty核心模型)
  - 事件分发: EventDemultiplexer(epoll封装)→dispatch event→EventHandler回调(read/write/accept) (参考Netty NioEventLoop)
  - 性能点: 多Reactor利用多核, 主从Reactor分离accept与IO, 编解码在Reactor线程做(避免线程切换)

### 6. io_uring — 下一代异步IO
  - 核心结构: 共享内存SQ(Submission Queue)+CQ(Completion Queue)环形缓冲区 — 零系统调用提交/收割 (fs/io_uring.c → io_uring_setup → io_ring_ctx)
  - SQ: 用户态写SQE(提交请求)到SQ Ring, 内核读 — 减少系统调用开销 (fs/io_uring.c → io_submit_sqes)
  - CQ: 内核写CQE(完成事件)到CQ Ring, 用户态读 — 事件通知零拷贝 (fs/io_uring.c → io_cqring_wait)
  - 与epoll对比: epoll是网络事件通知(仍要recv/read系统调用), io_uring支持read/write/send/recv等所有IO操作异步化
  - IORING_OP_READ/IORING_OP_WRITE: 提交读请求→内核读完直接回数据到用户buffer→用户收割CQE — 完整异步链

### 7. 收束
  - epoll三个系统调用(create/ctl/wait)构建了"事件源→事件通知→事件分发"的标准模式
  - 性能优势根因: ep_poll_callback在中断上下文直接将epitem入链, 消除select/poll的O(N)遍历
  - Reactor模型将epoll事件循环封装为框架内部驱动引擎, io_uring是下一跳(网络+磁盘统一异步化)

---

### 核心悬念
**"epoll能管100万个fd, 但一条TCP到底吃多少内存? 100万连接要多大内存才够?"**

→ 引出 百万并发与TCP内存开销(09-million-concurrency)
