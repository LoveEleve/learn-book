# I/O 调度器 + blk-mq + bio/request + epoll 实现 + Netfilter

> Cluster D: 8 KPs | 依赖: 14-页缓存 I/O 路径 | 读者基线: 理解页缓存读写和文件 IO 路径
> 读者处境: 已读完 14 篇，知道数据在页缓存层流动；本篇回答"页缓存之下的块设备层怎么工作？网络事件怎么被 epoll 捕获？"
> 打开新视角: epoll 的 O(1) 秘密（红黑树+回调）、块设备的分层（bio/request/调度器）、I/O 调度器的取舍、数据包经过的 5 链 4 表

---

### 概念依赖链

```
14-页缓存 I/O(上层路径) → 本篇: 块设备与事件机制
  ├─ §1 epoll 实现(事件监控 — 依赖 11 进程 + 09 等待队列)
  │    └─ §2 I/O 调度器(请求合并排序 — 依赖 14 落盘路径)
  │         └─ §3 块设备层(bio/request 分层 — 依赖 §2)
  │              └─ §4 Netfilter(网络包检查点 — 网络侧)
  │                   └─ §5 socket 层(sk_buff 封装 — 网络侧, 依赖 §4)
先讲: 事件(epoll) → 落盘(调度器) → 分层(bio/request) → 网络检查(Netfilter) → 网络封装(socket)
后续依赖: 16-容器(网络隔离)、04 篇已铺垫页缓存
```

### 叙事顺序

1. 问题引入——百万连接的服务器——select/poll 为什么扛不住？epoll 凭什么 O(1)？（**Aha: epoll 的秘密是'回调'——事件来了才通知，不轮询**）
   - 过渡: 回调挂在哪？——红黑树+就绪链表
2. epoll 实现——rbr 红黑树/rdllist 就绪链表/wq；三调用流程；LT vs ET
   - 过渡: 文件数据落盘——请求怎么排队？
3. I/O 调度器——电梯算法/Deadline/kyber/none；合并排序的取舍
   - 过渡: 调度器的下层——请求怎么组织？
4. 块设备层——bio/request 分层；submit_bio 路径；请求合并
   - 过渡: 磁盘说完了——网络包呢？
5. Netfilter——5 链 4 表；数据包路径；Docker 底层
   - 过渡: 网络包的内核结构——sk_buff
6. socket 层——socket/sock/sk_buff 三层；协议栈分层（🟡 回填）
   - 过渡: 完整图景已齐——收束
7. 收束——事件/落盘/网络三条路径

### 1. epoll 实现 — 红黑树 + 就绪链表 + 回调驱动

场景提示: 100 万连接的聊天服务器——select 每次要遍历 100 万 fd；epoll 怎么做到"谁有事叫谁"？ [写作时展开]

关键设计: epoll 三数据结构 (fs/eventpoll.c)：

```[pseudocode]
eventpoll: rbr(红黑树, 注册的所有 fd) + rdllist(就绪链表, 有事件的 fd) + wq(等待队列)
```

- **epoll_ctl(ADD)**: fd 插入红黑树 → 注册回调 `ep_poll_callback` 到目标 fd 的等待队列（`ep_ptable_queue_proc → init_waitqueue_func_entry`）——**"有事叫我"的钩子**
- **epoll_wait**: 查就绪链表 → 非空直接返回 → 空则 sleep（挂入 wq）→ 事件到达 → 回调把 epitem 加入 rdllist → 唤醒 epoll_wait

对比: select 遍历 fd_set O(n) + 1024 限制 / poll 链表 O(n) / **epoll 回调 O(1)**（事件到达直接入就绪链表）。

LT vs ET: LT（默认）未处理完持续通知 / ET 只通知一次（需循环读到 EAGAIN，配合非阻塞 IO）。

Why: 为什么 epoll 是 O(1) 而 select 是 O(n)？——select **每次调用都全量扫描** fd_set（不管有没有事件）；epoll **只在注册时登记一次**（红黑树），之后靠**回调**把有事件的 fd 直接放进就绪链表——`epoll_wait` 只需取链表头，复杂度与事件数成正比而非 fd 数。**把"轮询"变成"订阅"**——这是事件驱动与轮询的本质区别。 [内核: epoll 回调挂在目标 fd 的等待队列(waitqueue)——与 09 篇中断唤醒同一机制, 事件驱动内核通用模式]

比喻锚点: epoll=酒店前台登记——select 是前台每 5 分钟**挨个房间敲门**问"有事吗"（轮询 O(n)）；epoll 是每个房间装**服务铃**（回调），客人按铃前台才知道（订阅 O(1)）——房间再多也不影响前台效率。 [写作时展开]

### 2. I/O 调度器 — 请求合并 + 排序

场景提示: 机械硬盘随机 IO 慢（寻道）——一堆乱序请求怎么排成高效顺序？ [写作时展开]

关键设计: I/O 调度器在请求队列里做**合并与排序**：

| 调度器 | 机制 | 适用 |
|--------|------|------|
| 电梯算法（老） | 按 LBA 排序单向扫 | 减少寻道，但尾部请求延迟高 |
| Deadline | 读写分队列 + 超时保证 | 每个请求有截止时间（防饿死） |
| kyber | 检测读写延迟，读慢时限制写深度 | 自适应（SSD/HDD 通用） |
| none | 无调度（多队列深度） | **NVMe**——设备自己调度，内核最小干预 |

blk-mq（多队列）: per-CPU 软件队列（sctx）→ 硬件派发队列（hctx，每硬件队列一个）→ NVMe 多硬件队列并行 → 减少锁竞争。

Why: 为什么 NVMe 用 none 而 HDD 需要调度器？——HDD 的寻道代价巨大（排序能省几十 ms）；NVMe 无寻道（随机 IO 与顺序 IO 差距小），且设备内部队列深、自己会调度——内核再排序是**多此一举还添延迟**。**调度器的存在价值 = 设备物理特性的函数**：有寻道需要排序，无寻道直接放行。

比喻锚点: I/O 调度器=机场塔台——HDD 是单跑道（一次一架，按目的地排序省调头）；NVMe 是 64 条跑道（同时起飞，塔台不用管顺序）——塔台（内核调度器）只在跑道少时有价值。 [写作时展开]

### 3. 块设备层 — bio + request

场景提示: 页缓存里的脏页怎么变成磁盘上的数据？中间经过了什么结构？ [写作时展开]

关键设计: 两层结构 (include/linux/blk_types.h)：

```[pseudocode]
struct bio: 一次块 I/O 操作 — bi_io_vec(页向量: 页指针+长度+偏移) + bi_iter(起始扇区+大小) + bi_end_io(完成回调)
struct request: 加入调度器的请求 — 多个相邻 bio 可能合并为一个 request
```

路径: `submit_bio → generic_make_request → blk_mq_make_request → dispatch → 驱动`——bio 是"我要读/写哪些页"，request 是"调度器排好队的请求"。

**请求合并**: `bio_try_merge`——bio 扇区与已有 request 相邻 → 合并（减少 dispatch 次数）→ I/O 调度器的主要工作。

Why: 为什么 bio 和 request 两层？——bio 描述"**是什么**"（数据页 + 位置，来自上层）；request 描述"**怎么送**"（排好队的顺序，调度器产物）。分层让上层（文件系统）只关心"读/写哪些数据"，下层（调度器/驱动）只关心"按什么顺序送"——**语义与调度分离**，各层独立演进。

比喻锚点: bio=快递单（寄什么/寄哪），request=装车顺序（怎么装最顺路）——仓库（文件系统）只管填单，车队（调度器）只管排路线；单子和路线互不干扰。 [写作时展开]

### 4. Netfilter — 内核网络包过滤框架

场景提示: 防火墙规则（iptables）——内核在哪一步检查每个包？Docker 的端口映射怎么实现的？ [写作时展开]

关键设计: 数据包在内核经过的**检查点**（5 链 × 4 表）：

```[pseudocode]
5 链: PREROUTING(路由前) / INPUT(到本机) / FORWARD(转发)
     / OUTPUT(从本机发出) / POSTROUTING(路由后)
4 表: raw(跟踪豁免) / mangle(改包头) / nat(地址转换) / filter(过滤)
```

数据包路径: PREROUTING(conntrack→mangle→nat) → 路由决策 → FORWARD / INPUT(本地) → OUTPUT(发出) → POSTROUTING(mangle→nat) → 出网卡。注册: `nf_register_net_hook`。调试: `iptables -t nat -L -n -v` / `nft` / `conntrack -L`。

Why: 为什么用"链+表"而非"每设备规则"？——链定义**时机**（包在哪个阶段），表定义**操作类型**（改/转/滤）——组合出"在 PREROUTING 做 NAT"这种精确语义。Docker 端口映射（`-p 8080:80`）= PREROUTING 的 nat 表 DNAT 规则——**容器网络隔离（16 篇）与端口暴露都建立在 Netfilter 上**。

比喻锚点: Netfilter=小区门禁流程——每个包（访客）进门要过"岗亭序列"（链）：进门登记（PREROUTING）→ 检查身份（INPUT）→ 出门登记（POSTROUTING）；每个岗亭查不同本子（表）：黑名单（filter）/改访客牌（nat）。 [写作时展开]

### 5. socket 层 — 网络协议栈的封装骨架 (🟡)

场景提示: `socket()/bind()/listen()/accept()`——用户态看到的"文件描述符"，内核里是什么结构？ [写作时展开] [AI补充]

关键设计: 网络协议栈三层骨架 (net/socket.c, include/linux/skbuff.h) [AI补充]：

```[pseudocode]
struct socket(BSD 抽象, 用户 API 层)
  → struct sock(协议无关, 含接收/发送队列)
    → 传输层具体实现(TCP/UDP 状态机)
struct sk_buff(数据包核心): data/head/tail/end 四指针 — 各协议层移动指针封装头, 零拷贝分层
```

- **协议栈分层**: TCP→IP→netfilter→驱动→网卡；收包逆序: 网卡→驱动→IP→TCP→socket 队列→用户 read [AI补充]
- **与用户态**: `read` 从 socket 接收队列取数据 → `send` 把数据封装进 sk_buff → backlog（接收队列满则丢弃或阻塞） [AI补充]

Why: 为什么 sk_buff 用"四指针"而非复制？——每个协议层都要**加自己的头**（TCP 头/IP 头/以太网头）：复制 4 次 = 4 次内存拷贝；移动指针 = 0 次拷贝。sk_buff 的 head/data/tail/end 让各层"往指针范围里写头"——**分层封装零拷贝**，与 14 篇的零拷贝理念一脉相承。 [内核: sk_buff 是网络栈的核心结构——协议层共享同一 buffer, 只移动指针]

比喻锚点: sk_buff=可伸缩行李箱——TCP 层放一层衣服（TCP 头）、IP 层再放一层（IP 头）……每层只需把隔板（data 指针）往前推一格，不用重新打包整个箱子（零拷贝）。 [写作时展开]

### 6. 收束

三条路径的完整图景：
- 事件 = epoll（红黑树+回调，O(1)）
- 落盘 = 调度器（合并排序）→ bio/request 分层
- 网络 = Netfilter（5 链 4 表）→ socket/sk_buff 封装

**Aha Moment**: "epoll 的 O(1) 靠'订阅'而非'轮询'；I/O 调度器的价值取决于设备物理特性（HDD 需排序、NVMe 不需要）；数据包每过一个阶段（链）都可能被改写（表）——这三条路径的共同点是'分层 + 回调/钩子'：每层只做自己的事，通过钩子协作。"
**回答读者三问**: ①epoll 为何快=回调入就绪链表 O(1)；②NVMe 为何无调度=none 设备自调度；③Docker 端口映射=Netfilter PREROUTING nat。

---

### 核心悬念

**"内核的代码跑完了 — Docker 怎么用 Namespace 和 Cgroup 把进程关在小隔间里？七个 Namespace 各隔离了什么？"**

→ 引出 16-容器原理 + 7 种 Namespace + Cgroups v1/v2——内核的完整机制已铺完，接下来是"怎么隔离"。
