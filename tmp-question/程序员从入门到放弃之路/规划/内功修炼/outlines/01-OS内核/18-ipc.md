# IPC 进程间通信 — 管道/共享内存/消息队列 + Binder + Netlink

> Cluster E: 6 KPs | 依赖: 11-进程模型 + 02-分页机制 | 读者基线: 理解进程地址空间和 fd 概念

---

### 1. 管道 — 单向字节流，Shell 的 `|`
  - 匿名管道: `pipe(int fd[2])` → fd[0] 读端 / fd[1] 写端 → 内核环形缓冲区(底 16 页=64KB, 可调 `/proc/sys/fs/pipe-max-size`) → 写满阻塞 → 读空阻塞 → 父 fork 后子继承 fd 通信 (fs/pipe.c:723)
  - FIFO(命名管道): `mkfifo(path, 0644)` → 文件系统节点 → 持久化(进程退出后 FIFO 还在) → 无亲缘关系进程通过路径 open → 与匿名管道相同的内核机制 → `O_NONBLOCK` 控制阻塞 (fs/pipe.c:1122)
  - 实现: `struct pipe_inode_info` → `pipe_buffer` 环(每个 4KB) → `pipe_read/pipe_write` → `pipe_write` 写满 `wait_event` 睡眠 → `pipe_read` 唤醒写者

### 2. 共享内存 — 最快 IPC，零拷贝
  - System V: `shmget(key, size, IPC_CREAT|0666) → shmat(shmid, addr, 0)` → 物理页同时映射到两个进程 → 无需拷贝 → 需要额外同步(信号量/mutex 锁) — `ipcs -m` 查看 (ipc/shm.c:487)
  - POSIX: `shm_open(name, O_CREAT|O_RDWR, 0666) → ftruncate → mmap(NULL, size, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0)` → `/dev/shm/` 路径下 tmpfs 文件(默认 RAM 一半大小) — 更现代
  - 底层: 两个进程的页表项指向同一物理页帧 → 任一进程写立即可见(无拷贝无系统调用) → 速度快于其他 IPC(1-2 倍)

### 3. 消息队列 — 有类型 + 优先级
  - System V: `msgget(key, IPC_CREAT|0666) → msgsnd/msgrcv → msgctl(IPC_RMID)` → 消息结构 `{long mtype; char mtext[];}` → `msgrcv` 可按类型过滤(非 FIFO) → 持久化到内核(进程退出后队列还在) — `ipcs -q` (ipc/msg.c:487)
  - POSIX: `mq_open(name, O_CREAT|O_RDWR) → mq_send/mq_receive → mq_close/mq_unlink` → `mq_getattr` 获取属性(maxmsg/msgsize) → 有优先级(整数) → 可设持久化(但非内核持久, 仅在系统运行期间) (ipc/mqueue.c:852)
  - 与管道对比: 消息有边界(管道是字节流) / 有类型(可按类型过滤, 管道是 FIFO) / 持久化(管道进程退出后消失)

### 4. Binder — Android 的 IPC 核心
  - `/dev/binder` 驱动: `ioctl(BINDER_WRITE_READ)` → 一次拷贝(发送方→binder 内核驱动→接收方, 不用拷贝两次) → binder 驱动维护 userspace 的 binder_proc→binder_thread (drivers/android/binder.c:2419)
  - Binder 上下文管理: service manager(binder context manager, `/dev/binder` 上的 server) → 注册服务(类似 DNS) → 客户端 `transact` 发送 binder 消息 → 服务端 `onTransact` 接收 → AIDL 定义接口
  - 安全: UID/PID 验证(每个 binder 请求携带发送方身份, 不能伪造) → 一次拷贝(目标 mmap 到接收方地址空间, binder 直接写入) → 比 Socket+共享内存更高效

### 5. Netlink — 内核与用户态通信
  - 内核侧: `netlink_kernel_create → nlmsg_put → nlmsg_unicast` → 异步, 内核向用户态发送消息时无需用户态先请求 → 用于路由/ACPI/防火墙 (net/netlink/af_netlink.c:1856)
  - 用户侧: `socket(AF_NETLINK, SOCK_RAW, NETLINK_ROUTE) → bind → sendmsg/recvmsg` → `ip` 命令底层(`ip route add` 发送 Netlink 消息) → 优于 ioctl(异步, 支持多播)
  - 场景: iproute2(路由) / udev(设备热插拔) / conntrack(连接跟踪) → 可以组播到多个用户态进程

### 6. 收束
  - IPC 三级: 管道(字节流, 阻塞) / 消息队列(有类型, 持久化) / 共享内存(零拷贝, 需同步)
  - Binder: 一次拷贝 + UID 认证 = Android IPC 最优解
  - Netlink: 内核→用户态的异步/多播通道(替代 ioctl 和 /proc)

---

### 核心悬念
**"Linux 内核本身是怎么启动的？宏内核和微内核有什么区别？strace/perf/ftrace/bpftrace 这些工具怎么用？"**

→ 引出 19-内核架构(宏/微核) + 启动流程 + 内核模块 + strace/perf/ftrace/bpftrace
