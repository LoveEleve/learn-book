# I/O 调度器 + blk-mq + bio/request + epoll 实现 + Netfilter

> Cluster D: 8 KPs | 依赖: 14-页缓存 I/O 路径 | 读者基线: 理解页缓存读写和文件 IO 路径

---

### 1. epoll 实现 — 红黑树 + 就绪链表 + 回调驱动
  - `epoll_create`: 创建 eventpoll 结构 → rbr(红黑树根, 存储所有注册 fd) / rdllist(就绪链表, 存储就绪事件) / wq(等待队列, epoll_wait 睡眠时挂入) (fs/eventpoll.c:317)
  - `epoll_ctl(EPOLL_CTL_ADD)`: 将 fd 插入红黑树 → `tfile_check_list`(防环路) → 注册回调 `ep_ptable_queue_proc → init_waitqueue_func_entry(&pwq->wait, ep_poll_callback)` — 在目标 fd 的等待队列中加回调 (fs/eventpoll.c:1930)
  - `epoll_wait`: 检查就绪链表 → 非空直接返回就绪列表 → 空则 sleep(挂入 wq) → 事件到达 → `ep_poll_callback` → 将 epitem 加入 rdllist → 唤醒 epoll_wait (fs/eventpoll.c:2222)
  - epoll vs select/poll: select 遍历 fd_set O(n), 1024 fd 限制 / poll 链表无限制但 O(n) / epoll 回调 O(1) → 大并发必须 epoll
  - LT vs ET: LT(默认, 未处理完持续通知) / ET(只通知一次, 需循环读到 EAGAIN, 配合非阻塞 IO) (fs/eventpoll.c:2068)

### 2. I/O 调度器 — 请求合并 + 排序
  - 电梯算法: 按 LBA 排序(磁头单向扫) → 减少寻道 → 但延迟增加(中间扇区先服务) → Deadline: 读写分开队列 + 超时保证每个请求 (block/mq-deadline.c:326)
  - kyber(Cyber): 域值检测读/写延迟 → 当读延迟上升时限制写 dispatching depth → 自适应 (block/kyber-iosched.c:489)
  - none(无调度): NVMe 用 → 多队列深度 + 设备本身调度 → 内核无需干预 → 最小延迟 (block/blk-mq.c:1984)
  - blk-mq: 多队列 → per-CPU 软件队列(sctx) → 硬件派发队列(hctx, 每个硬件队列一个) → NVMe 多个硬件队列并行 → 减少锁竞争 (block/blk-mq.c:2354)

### 3. 块设备层 — bio + request
  - `struct bio`: 块 I/O 操作 → bi_io_vec(向量链表, 页指针+长度+偏移) / bi_iter(起始扇区+大小) / bi_end_io(完成回调) → 一个 I/O 请求 (include/linux/blk_types.h:103)
  - `struct request`: 加入 scheduler(排队+合并+排序) → 多个 bio 可能合并为一个 request(相邻扇区的 bio) → `request_queue` → gendisk → `submit_bio → generic_make_request → blk_mq_make_request → dispatch → 驱动` (block/blk-mq.c:2620)
  - 请求合并: `bio_try_merge` → bio 扇区与已有 request 相邻 → 合并(减少 dispatch 次数) → I/O 调度器的主要工作

### 4. Netfilter — 内核网络包过滤框架
  - 5 链: PREROUTING(路由前) / INPUT(到本机) / FORWARD(转发) / OUTPUT(从本机发出) / POSTROUTING(路由后) (include/uapi/linux/netfilter.h:18)
  - 4 表: raw(跟踪豁免) / mangle(修改包头) / nat(地址转换) / filter(过滤) → 每链+表组合有 hook 函数 → `nf_register_net_hook` 注册 (net/netfilter/core.c:452)
  - 数据包路径: PREROUTING(conntrack→mangle→nat) → 路由决策 → FORWARD / INPUT(本地) → OUTPUT(发出) → POSTROUTING(mangle→nat) → 出网卡
  - 调试: `iptables -t nat -L -n -v`(查看规则) → `nft`(新一代) → `conntrack -L`(跟踪连接表) → Docker/K8s 网络底层基于 Netfilter

### 5. 收束
  - epoll = 红黑树(注册) + 就绪链表(快速返回) + 回调(事件驱动) = O(1) 事件监控
  - blk-mq(多队列) + bio/request 分层(合并排序) → NVMe 用 none 调度器
  - Netfilter(5 链 4 表) = 内核数据包经过的检查点 → 每个包触发 hook

---

### 核心悬念
**"内核的代码跑完了 — Docker 怎么用 Namespace 和 Cgroup 把进程关在小隔间里？七个 Namespace 各隔离了什么？"**

→ 引出 16-容器原理 + 7 种 Namespace + Cgroups v1/v2
