# epoll、LT/ET 与 Reactor — 就绪事件如何被持久化并分发

> Cluster C: 6 KPs | 依赖: 07-io-models | 读者基线: select/poll、socket readiness、非阻塞 I/O
> 读者处境: 07 篇已经发现 select/poll 每次都要重新遍历 fd；本篇回答：epoll 怎样保存监听关系、怎样在数据到达时把就绪项放进 ready list，以及 Reactor 如何建立在这套机制上
> 打开新视角: epoll 的优势不是“红黑树查找变成 O(1)”，而是**监听关系持久化、就绪事件由回调增量生产，`epoll_wait` 只消费当前 ready list**

---

### 概念依赖链

```
07-io-models(select/poll O(N)) → 本篇: epoll / LT-ET / Reactor / io_uring
  ├─ §1 eventpoll/epitem(监听树 + ready list + wait queue)
  ├─ §2 epoll_create/ctl/wait(生命周期)
  ├─ §3 ep_poll_callback(数据到达如何变成就绪事件)
  ├─ §4 LT/ET(事件重复报告语义)
  ├─ §5 Reactor(应用层事件循环抽象)
  └─ §6 io_uring(事件通知走向完成队列)
先讲: 内核数据结构 → 系统调用 → 回调入队 → LT/ET → Reactor → io_uring
后续依赖: 09-million-concurrency(大量 fd 的内存与调优成本)
```

### 叙事顺序

1. 问题引入——1 万个 socket 中只有 3 个有数据，为什么不必每轮扫描全部 fd？（**Aha: epoll 把“监听关系”和“本次就绪事件”拆成长期保存的树与增量维护的链表**）
2. `eventpoll` / `epitem`——监听关系和 ready list
3. `epoll_create1/epoll_ctl/epoll_wait`——注册、修改、消费
4. `ep_poll_callback`——数据到达时把事件推进 ready list
5. LT/ET——重复报告与边沿消费纪律
6. Reactor / io_uring——从内核事件到编程模型
7. 收束——epoll 的真正性能来源

### 1. `eventpoll` / `epitem` — 一棵监听索引与一条就绪链

场景提示: epoll 实例里注册了 10 万个 fd，内核怎样既记住它们，又快速找到当前有事件的那几个？ [写作时展开]

关键设计: Linux epoll 实例维护监听关系、就绪事件和等待者；每个被监视 fd 对应一个 `epitem`（fs/eventpoll.c）：

```[pseudocode]
struct eventpoll
  rbr     = 保存所有已注册 epitem 的索引结构
  rdllist = 当前 ready epitem 链表
  wq      = epoll_wait 睡眠等待队列

struct epitem
  关联一个被监视 file/fd
  保存用户注册的 events/data
  通过 poll wait queue callback 接收状态变化
```

Why: 为什么同时需要监听树和 ready list？——**监听树解决“这个 fd 是否已经注册、如何 ADD/MOD/DEL”，ready list 解决“这次 wait 直接返回哪些已就绪项”**：注册关系的查找/更新与事件消费是两个不同问题。红黑树通常提供 O(logN) 的注册管理；它不是 epoll 每次返回事件 O(1) 的根因，真正的关键是 ready list 的增量维护。 [内核: `eventpoll` 的 `rbr/rdllist/wq` 分别对应注册索引、就绪队列和等待者]

比喻锚点: 监听树像通讯录，记录你关注哪些人；ready list 像“刚刚来电的人”队列，`epoll_wait` 只处理来电者，不重读整本通讯录。 [写作时展开]

### 2. `epoll_create1` / `epoll_ctl` / `epoll_wait` — 注册、更新、消费

场景提示: 应用第一次启动时怎样把 socket 交给 epoll？数据到达后又是哪次调用真正取走事件？ [写作时展开]

关键设计: 三个系统调用分别管理 epoll 实例生命周期、监听关系和就绪事件消费：

```[pseudocode]
epoll_create1(flags)
  → 分配 eventpoll
  → 初始化 rbr / rdllist / wq
  → 返回 epoll fd

epoll_ctl(epfd, ADD, fd, event)
  → 创建 epitem
  → 保存 events/data
  → 插入监听索引
  → 调用目标 fd->f_op->poll 注册 ep_poll_callback

MOD / DEL
  → 更新事件掩码, 或从索引/等待队列/ready list 移除 epitem

epoll_wait(epfd, events, maxevents, timeout)
  → ready list 非空? 拷贝就绪事件到用户数组
  → 为空? 睡眠直到回调唤醒/超时/信号
```

Why: 为什么 `epoll_ctl(ADD)` 要把回调挂到 socket 等待队列，而不是 `epoll_wait` 时再询问 socket？——**因为事件发生的时刻才是最便宜的记录时刻**：数据到达时 socket 已经在执行 readiness 唤醒链，epoll 只需顺手把对应 epitem 放入 ready list；如果等 wait 才查，就又回到了 select/poll 的全量扫描。 [内核: `ep_insert` 通过目标文件的 `poll` 方法建立等待队列回调]

比喻锚点: `epoll_ctl` 像订阅通知，`epoll_wait` 像收取通知；不是每次收取时都去挨个问所有联系人“你有没有新消息”。 [写作时展开]

### 3. `ep_poll_callback` — 数据到达如何变成 ready event

场景提示: TCP 数据已经进入 socket 接收队列，epoll 实例如何知道应该唤醒哪个等待线程？ [写作时展开]

关键设计: socket readiness 回调和 epoll callback 串在同一条通知链上：

```[pseudocode]
tcp_data_queue
  → sk_data_ready
  → sock_def_readable
  → ep_poll_callback
      检查 epitem 关心的事件掩码
      若尚未在 ready list:
        list_add_tail(epitem, rdllist)
      wake_up(epoll wait queue)

epoll_wait 被唤醒
  → ep_send_events_proc
  → 从 rdllist 取出并复制 event/data
```

Why: 为什么 epoll 能减少等待时的 O(N)，却不能说所有 epoll 操作都是 O(1)？——**就绪消费仍与本次返回的事件数相关，ADD/DEL 也有索引维护成本；epoll 的优势是避免每次 wait 对所有未就绪 fd 做检查**。如果 10 万个 fd 同时就绪，复制 10 万个事件本身当然不可能是 O(1)。 [内核: `ep_poll_callback` 负责生产 ready event，`ep_send_events_proc` 负责消费它们]

比喻锚点: 数据到达像快递员把“已到货”标签直接贴进待取货架；`epoll_wait` 只拿当前有标签的包裹，不重新检查整座仓库。 [写作时展开]

### 4. LT 与 ET — 是报告策略，不是两种不同的 socket

场景提示: 同一个非阻塞 socket，为什么 LT 模式会反复提醒，ET 模式却可能只提醒一次？ [写作时展开]

关键设计: LT/ET 定义的是 readiness 事件如何重复呈现给应用：

```[pseudocode]
LT(Level Triggered, 默认)
  只要 fd 仍然可读/可写
  → epoll_wait 后续仍可继续返回该事件
  优点: 应用漏处理一次, 下次仍有机会

ET(EPOLLET)
  关注从“不可读”到“可读”的状态变化
  → 事件到达后应用应循环 read/recv
  → 直到返回 EAGAIN, 把当前就绪状态消耗干净

共同纪律:
  使用非阻塞 fd
  事件处理函数不能假设一次 read 读完所有数据
```

Why: 为什么 ET 必须非阻塞并循环读到 EAGAIN？——**因为边沿只保证状态变化被报告，不保证缓冲区一次被清空**：如果只读一小部分就返回事件循环，剩余数据可能没有新的边沿触发。LT 更容易写对，ET 在高吞吐场景可能减少重复唤醒，但把“排空 fd”的责任交给应用。 [man 7 epoll: LT/ET 语义；[内核: `ep_send_events` 根据当前状态和事件标志决定报告方式]

比喻锚点: LT 像门一直开着就不断亮灯，ET 像门从关到开时按一次铃；ET 下听到铃后必须把门口积压的货一次处理到没有货。 [写作时展开]

### 5. Reactor — epoll 之上的事件循环抽象

场景提示: epoll 只返回“哪个 fd 就绪”，它怎样变成 Redis、Netty、Nginx 这类框架里的事件循环？ [写作时展开]

关键设计: Reactor 把事件多路复用器、事件分发器和业务 handler 组织成持续运行的循环：

```[pseudocode]
while (running):
  events = epoll_wait(epfd)
  for event in events:
    handler = event.data
    if EPOLLIN:  handler.read/accept()
    if EPOLLOUT: handler.write/flush()
    if EPOLLERR/HUP: handler.close/error()
```

常见拓扑:
- 单 Reactor 单线程：I/O 和业务都在一个循环，简单但业务阻塞会拖住所有连接
- 单 Reactor + worker：事件循环只做 I/O，重业务交给线程池
- 主从 Reactor：主线程 accept，从 Reactor 负责连接读写；常见于多核网络框架，但具体实现会有差异

Why: 为什么 Reactor 线程里不能执行长时间业务？——**它不仅处理业务，还承担下一轮 epoll_wait 和所有连接的事件分发**：一个慢 handler 就会阻塞同一线程上的其他连接。拆 worker 的代价是任务投递、线程同步和连接状态管理复杂化。 [内核: epoll 只提供 readiness 事件；Reactor 的 handler 分发、线程拓扑属于用户态框架设计]

比喻锚点: Reactor 像总调度台，接到“某线路有货”通知后快速分派；它自己不能花半小时处理一单，否则整座调度台都停摆。 [写作时展开]

### 6. io_uring — 从“就绪通知”走向“操作完成队列”

场景提示: epoll 告诉你“可以 read 了”，但应用还要再调用 read；能不能把读请求直接提交给内核，完成后再收割结果？ [写作时展开]

关键设计: io_uring 使用用户/内核共享的 submission queue 与 completion queue，把“提交操作”和“收割结果”分开：

```[pseudocode]
io_uring_setup
  → 建立 SQ/CQ ring 和 io_uring context

提交:
  用户准备 SQE(read/recv/send/write/...)
  → 提交到 SQ
  → 内核取 SQE 执行操作

完成:
  内核写 CQE {user_data, res, flags} 到 CQ
  → 用户读取 CQE 得到完成结果

注意:
  是否真正无系统调用、是否真正零拷贝
  取决于提交/收割方式、操作类型、固定 buffer/文件等配置
```

Why: 为什么 io_uring 和 epoll 不是简单的替代关系？——**epoll 主要报告 readiness，应用仍负责调用 read/recv；io_uring 目标是直接提交 I/O 操作并收获 completion**：两者的事件语义不同，实际系统也可能组合使用。共享 ring 减少 syscall 边界，但不能对所有操作承诺“零 syscall、零拷贝”。 [内核: `io_uring_setup` 建立 ring；SQE/CQE 分别表达提交请求和完成结果]

比喻锚点: epoll 像仓库通知“这批货可以取了”，io_uring 像把“取货任务单”直接交给仓库，完成后只收一张结果单。 [写作时展开]

### 7. 收束

回到 1 万个 fd 只有 3 个就绪的场景：
- select/poll：每轮把整批 fd 重新交给内核检查
- epoll：监听关系长期保存，数据到达时把 epitem 增量放入 ready list
- LT/ET：决定 ready 状态如何重复报告，ET 要求非阻塞循环排空
- Reactor：把 epoll 的事件分发组织成用户态事件循环
- io_uring：把“可读通知”进一步推进到“提交操作/收割完成”

**Aha Moment**: "epoll 的真正突破不是“红黑树让 wait 变成 O(1)”，而是**把监听关系变成长期状态，把就绪事件变成数据到达时增量生产的 ready list**；`epoll_wait` 只消费当前就绪集合。"
**回答读者三问**: ①epoll 为什么比 select/poll 少扫=事件到达时入 ready list；②LT/ET 差在哪=重复报告还是边沿报告；③io_uring 与 epoll 差在哪=readiness 通知还是 I/O completion 队列。

---

### 核心悬念

**"epoll 能管理大量 fd，但每条 TCP 连接的 socket、缓冲区、定时器和内核对象各占多少内存？100 万连接的真正瓶颈是 fd 数量，还是内存与协议状态？"**

→ 引出 09-million-concurrency — 百万并发与 TCP 内存开销——从事件分发进入资源账本与容量规划。