# 五种IO模型 + select/poll 内核实现对比

> Cluster C: 6 KPs | 依赖: 05-收包, 06-发包 | 读者基线: 内核网络路径 + socket

---

### 1. 阻塞IO — 最简单的模型, 最低的效率
  - 调用recv: 数据未就绪→进程进入TASK_INTERRUPTIBLE睡眠→加入sk_sleep等待队列→收到数据内核唤醒 (net/core/sock.c → sock_def_readable → wake_up)
  - 等待队列: wait_queue_head_t sk_sleep, 放入socket→sk_sleep, 收到数据时回调唤醒 (include/net/sock.h → sk_sleep)
  - 问题: 一个线程只能处理一个连接 — recv阻塞时无法处理其他连接的到达数据 (net/ipv4/tcp.c → tcp_recvmsg 阻塞路径)
  - 适用: 短连接/单连接场景, 模型简单但无法并发

### 2. 非阻塞IO — 轮询的代价
  - 设置O_NONBLOCK: fcntl(sockfd, F_SETFL, O_NONBLOCK)→recv无数据立即返回EAGAIN而不是阻塞 (fs/fcntl.c → setfl)
  - 内核路径: tcp_recvmsg检查sk_receive_queue为空→返回EAGAIN而非sleep (net/ipv4/tcp.c → tcp_recvmsg nonblock路径)
  - 轮询(select/poll): 用户态循环调用select检查哪些fd可读→逐个处理 (fs/select.c → do_select)
  - 问题: N个连接→每轮遍历N个fd→O(N)复杂度→CPU大量浪费在无数据fd上

### 3. IO多路复用 — 一次等待多个fd
  - 核心思想: 一次系统调用等N个fd, 返回可操作的fd子集 — 内核负责监听, 用户态负责处理 (fs/select.c / fs/eventpoll.c)
  - select: 传入读/写/异常fd_set→内核检查→返回可读/可写/异常集合 (fs/select.c → core_sys_select)
  - select局限: fd_set最大1024(受FD_SETSIZE限制), 每次调用需拷贝整个fd_set(内核→用户), 内核遍历fd_set O(N) (include/linux/posix_types.h → __FD_SETSIZE)
  - poll: 传入pollfd数组(无fd数量限制)→内核遍历→返回revents (fs/select.c → do_sys_poll)
  - poll vs select: poll无1024限制(仅受RLIMIT_NOFILE), 但仍需O(N)遍历+用户/内核拷贝

### 4. 信号驱动IO + 异步IO — 理论vs实践
  - 信号驱动IO: fcntl/sigaction设置SIGIO信号→数据就绪时内核发信号通知→信号处理函数中recv — 但信号上下文不可靠 (fs/fcntl.c → fcntl_setlease)
  - POSIX AIO(aio_read): 提交读请求→立即返回→内核完成后通知, 但实现复杂实际少用 (fs/aio.c → aio_read)
  - 问题: 信号驱动IO在TCP中不稳定(SIGIO在高速包下可能丢失), POSIX AIO内核实现是glibc线程模拟

### 5. select内核实现 — 深度走读
  - 入口: sys_select→core_sys_select→do_select 大循环 (fs/select.c → core_sys_select)
  - 核心逻辑: 遍历3个fd_set的每个bit, 对每个fd调用f_op->poll(ref: tcp_poll)检查状态 (fs/select.c → do_select内层循环)
  - tcp_poll: 根据sk_receive_queue(可读?), sk_write_queue空间(可写?), sk_err(异常?) 设置对应mask返回 (net/ipv4/tcp.c → tcp_poll)
  - 睡眠等待: 所有fd无就绪→poll_schedule_timeout等待超时或被唤醒 (fs/select.c → poll_schedule_timeout)
  - 唤醒机制: 数据到达→sock_def_readable→pollwake→唤醒select等待的进程 (net/core/sock.c)
  - 返回: 统计就绪fd数, 拷贝fd_set回用户态, 返回就绪数量

### 6. poll内核实现 — 对select的修复
  - 入口: sys_poll→do_sys_poll→do_poll (fs/select.c → do_sys_poll)
  - 数据结构: 用户传入pollfd数组(每个struct pollfd{fd, events, revents}) — 无fd数量硬限制 (include/uapi/asm-generic/poll.h → pollfd)
  - 内核遍历: do_poll遍历poll_list, 每个pollfd调f_op->poll(tcp_poll)检查 (fs/select.c → do_poll)
  - 优于select: 不限1024, 返回revents(无需用户态遍历fd_set重建就绪集合), 但O(N)遍历本质未变
  - 共同局限: 都需要用户态→内核拷贝fd列表, 都需O(N)遍历 — 10000个fd时select/poll性能显著下降

### 7. 收束
  - 五种IO模型本质是"数据未就绪时线程做什么": 阻塞(睡)、非阻塞(轮询)、多路复用(等)、信号(通知)、异步(回调)
  - select和poll都陷入O(N)遍历陷阱 — N增大时线性退化, poll仅解决1024上限未解决遍历成本
  - 根本问题: select/poll每次都重新注册等待 → 有状态的监听才是解(epoll)

---

### 核心悬念
**"epoll是怎么做到O(1)返回就绪事件, 而不是像select/poll那样O(N)遍历的?"**

→ 引出 epoll内核实现与Reactor模型(08-epoll-reactor)
