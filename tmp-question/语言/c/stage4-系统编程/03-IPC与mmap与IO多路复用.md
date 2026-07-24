# IPC、mmap 与 I/O 多路复用

> **对应 Layer**：Layer 4（OS 接口）+ Layer 6（网络编程前兆），参考 C-C++技术专家学习路线.md § Layer 4  
> **参考书章节**：Practical 全书；Mastering 第5章（IPC）+ 第9章（网络）；极致C 第19章（IPC）+ 第20章（套接字）  
> **前置依赖**：S4Ch01（fd 本质—epoll/pipe/mmap 都基于 fd）+ S4Ch02（信号—SIGPIPE）  
> **主线标准**：C11/C17 + Linux 系统调用（POSIX）

---

## 1. 引言：进程间如何通信？

S4Ch02 讲了进程的创建和隔离。但被隔离的进程需要通信——一个 Web 服务器 worker 需要把计算结果发给另一个 worker，一个 shell 管道的 `ls | wc -l` 需要传递数据。Linux 提供了五种 IPC（Inter-Process Communication，进程间通信）机制 + mmap + I/O 多路复用。

---

## 2. 五种 IPC 机制

### 2.1 管道（pipe）— 父子进程专用

```c
#include <unistd.h>

int fd[2];
pipe(fd);   // fd[0]=读端, fd[1]=写端

if (fork() == 0) {
    close(fd[0]);                    // 子进程关闭读端
    write(fd[1], "hello", 5);
    close(fd[1]);
} else {
    close(fd[1]);                    // 父进程关闭写端
    char buf[6];
    read(fd[0], buf, 5);
    buf[5] = '\0';
    printf("Parent received: %s\n", buf);
    close(fd[0]);
}
```

**关键**：
- 半双工（单向）——内核心缓冲区 500B-65536B（Linux 2.6.11+ 默认 65536B）
- 必须有亲缘关系（fork 出来的）
- 写端关闭→read 返回 0（EOF）；读端关闭→write 触发 SIGPIPE
- **PIPE_BUF（4096 字节原子写入上限）**：多进程写同一 pipe 时，只有 ≤4096 的写保证原子（不被其他写入交错）。超过 4096 的写入可能被打碎。Linux 的 PIPE_BUF 为 4096（POSIX 最小要求 512）

**pipe vs FIFO 对比**：

| 维度 | pipe | FIFO |
|---|---|---|
| 亲缘关系 | 必须 fork | 无要求，任意进程 |
| 内核存在 | 匿名，仅 fd 引用 | 文件系统节点（`mkfifo` 创建） |
| 生命周期 | 最后一个 fd 关闭后销毁 | 文件系统节点需 `unlink` 删除 |
| 持久性 | 数据在 fd 关闭后丢失 | 同 pipe——数据不持久 |

### 2.2 FIFO（命名管道）— 无亲缘进程

```c
mkfifo("/tmp/myfifo", 0644);   // 创建命名管道

// 进程 A——写
int fd = open("/tmp/myfifo", O_WRONLY);
write(fd, "data", 4);

// 进程 B——读（不同进程，不需要 fork 关系）
int fd = open("/tmp/myfifo", O_RDONLY);
```

`open` 阻塞行为详解：
- **读打开** → 阻塞直到有写方打开
- **写打开** → 阻塞直到有读方打开
- **O_NONBLOCK**：读打开立即成功（即使无写方），写打开失败返回 ENXIO（无读方）
- **一端关闭**：读端关闭 → 写端收到 SIGPIPE；写端关闭 → 读端 read 返回 0（EOF）
- **O_RDWR** 模式：读写同时打开自己——不阻塞，但失去了"阻塞等对端"的同步语义

### 2.3 消息队列（System V / POSIX）

System V 消息队列按"类型"分发消息——接收者只收特定类型的消息：

```c
#include <sys/msg.h>

struct msgbuf { long mtype; char mtext[128]; };

int mqid = msgget(IPC_PRIVATE, 0644 | IPC_CREAT);
struct msgbuf msg = { .mtype = 1, .mtext = "hello" };
msgsnd(mqid, &msg, strlen(msg.mtext)+1, 0);
msgrcv(mqid, &msg, sizeof(msg.mtext), 1, 0);  // 只接收 mtype=1
```

**消息队列限制**：
- **MSGMAX**：单条消息最大 8192 字节（`cat /proc/sys/kernel/msgmax`）
- **MSGMNB**：队列总容量默认 16384 字节（`cat /proc/sys/kernel/msgmnb`）
- 超过限制 → msgsnd 阻塞或返回 EAGAIN

**POSIX 消息队列**（`mq_open`/`mq_send`）是更新的替代，支持优先级和异步通知。

### 2.4 共享内存（shm）— 最快

System V 共享内存是最快的 IPC——数据不经过内核拷贝：

```c
#include <sys/shm.h>

int shmid = shmget(IPC_PRIVATE, 4096, 0644 | IPC_CREAT);
char *p = shmat(shmid, NULL, 0);
sprintf(p, "Shared data");
```

**POSIX 共享内存**（`shm_open` + `mmap`）是推荐的现代替代——文件描述符化管理，可配合 `ftruncate`：

```c
int fd = shm_open("/myshm", O_CREAT | O_RDWR, 0644);
ftruncate(fd, 4096);
char *p = mmap(NULL, 4096, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0);
```

/dev/shm vs System V shm：
- **POSIX `shm_open`** → 在 `/dev/shm`（tmpfs 内存文件系统）下创建文件 → `df -h /dev/shm` 查看容量
- **System V `shmget`** → 内核 IPCs 维护，不映射到文件系统 → `ipcs -m` 查看所有共享内存段

**生命周期管理**：
- System V 共享内存段在进程退出后**不会自动删除**——必须 `shmctl(shmid, IPC_RMID, NULL)` 或 `ipcrm -m shmid`
- 生产常见：`ipcs -m` 看到大量 nattch=0 的 orphan 段——进程忘了 `IPC_RMID`
- POSIX `shm_open` 创建的文件在 `/dev/shm` 下——`unlink` 后仍可继续使用，最后一个 fd 关闭才真正删除

**为什么共享内存最快**：数据直接映射到两个进程的地址空间——没有 `read`/`write` 系统调用过路。但必须用信号量或 mutex 同步访问（共享内存本身不提供同步）。

### 2.5 套接字（socket）— 跨主机

本地 Unix Domain Socket 的路径名版本比 TCP 更快（零网络栈开销）：

```c
#include <sys/socket.h>
#include <sys/un.h>

int fd = socket(AF_UNIX, SOCK_STREAM, 0);
struct sockaddr_un addr = { .sun_family = AF_UNIX };
strcpy(addr.sun_path, "/tmp/mysock");
bind(fd, (struct sockaddr *)&addr, sizeof(addr));
listen(fd, 5);
```

详见 S4Ch04（Socket 编程与 Reactor）。

### 2.6 五种 IPC 速查

| IPC | 数据方向 | 亲缘关系要求 | 跨主机？ | 速度 |
|---|---|---|---|---|
| pipe | 单向 | fork | ❌ | 快（内核缓冲区） |
| FIFO | 单向 | 无 | ❌ | 快 |
| 消息队列 | 双向 | 无 | ❌ | 中等（内核拷贝） |
| 共享内存 | 双向 | 无 | ❌ | 最快（零拷贝） |
| socket | 双向 | 无 | ✅ | 取决于协议 |

---

## 3. mmap——四种用途

`mmap` 将文件或设备映射到进程地址空间——读写内存即读写文件。

### 3.1 文件映射

```c
#include <sys/mman.h>

int fd = open("data.bin", O_RDWR);
struct stat st;
fstat(fd, &st);

char *p = mmap(NULL, st.st_size, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0);
// 现在 p[0] 读取文件第一个字节, p[i] 读取第 i 个字节
p[0] = 'X';   // 写入——内核自动同步到文件
munmap(p, st.st_size);
```

### 3.2 匿名映射——大块内存分配

```c
char *p = mmap(NULL, 1024*1024, PROT_READ|PROT_WRITE,
               MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
// 分配 1MB 匿名内存——等价于简化版的 malloc
// MAP_PRIVATE：修改不写回文件（COW），MAP_SHARED：修改对 fork 子进程可见
```

### 3.3 共享内存（POSIX 风格）

```c
int fd = shm_open("/myshm", O_CREAT|O_RDWR, 0644);
ftruncate(fd, 4096);
char *p = mmap(NULL, 4096, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0);
// 父子进程/无关进程都可通过 shm_open 同一 name 获得映射
```

### 3.4 内存映射 I/O（MMIO）— 硬件寄存器

```c
int fd = open("/dev/mem", O_RDWR);
volatile uint32_t *reg = mmap(NULL, 4096, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0x3F000000);
*reg = 0x01;   // 写入硬件寄存器——需要 root 权限
```

### 3.5 mmap 性能优化：MAP_POPULATE 与 MAP_LOCKED

```c
// MAP_POPULATE——提前触发缺页
char *p = mmap(NULL, size, PROT_READ, MAP_PRIVATE|MAP_POPULATE, fd, 0);
// 内核立即预读文件内容到 page cache——后续访问不触发缺页中断
// 代价：mmap 调用本身变慢（需等待预读完成）

// MAP_LOCKED——锁定物理页（需要 CAP_IPC_LOCK）
char *p = mmap(NULL, size, PROT_READ, MAP_PRIVATE|MAP_LOCKED, fd, 0);
// 防止被 swap 换出——对安全敏感数据（如密钥）或实时系统至关重要
```

### 3.6 mmap vs read 对比

| 维度 | mmap | read |
|---|---|---|
| 数据拷贝 | 零拷贝（内核 page cache 直接映射到用户态） | 一次拷贝（page cache → 用户态缓冲区） |
| 随机访问 | 快（直接 `p[offset]`） | 慢（需 `lseek` + `read`） |
| 大文件 | 适合（按需缺页加载） | 每次读固定大小，需多次调用 |
| 小文件/顺序读 | 不值得（mmap 开销 > 收益） | 直接 read |
| 网络 I/O | ❌ 不适合（socket 无 page cache 映射） | ✅ |
| 错误处理 | SIGBUS（文件截断+访问）| 返回 -1 + errno |

### 3.7 mmap 的 4 种常见错误

1. **MAP_SHARED 文件截断**——如果文件大小 < mmap 的大小，SIGBUS
2. **MAP_PRIVATE 不写回文件**——MAP_PRIVATE 的写入只对当前进程可见
3. **忘记 munmap**——进程退出时自动取消映射，但长期运行的程序会泄漏虚拟地址空间
4. **MAP_ANONYMOUS 的 fd = -1**——必须是 -1（某些 BSD 系统有特殊要求）

---

## 4. I/O 多路复用：select / poll / epoll

### 4.1 问题：如何同时等 1000 个 socket？

如果每个 `accept` 后用单线程 `read` 阻塞等待——只能串行处理。如果用多线程每个连接一个线程——1000 线程的上下文切换不可接受。I/O 多路复用用一个线程同时监控 1000 个 fd——哪个有数据就处理哪个。

### 4.2 select——最古老

```c
#include <sys/select.h>

fd_set readfds;
FD_ZERO(&readfds);
FD_SET(listen_fd, &readfds);
FD_SET(client_fd, &readfds);

select(max_fd+1, &readfds, NULL, NULL, NULL);
// 返回后: FD_ISSET(listen_fd) → 新连接, FD_ISSET(client_fd) → 数据
```

**局限**：
- `FD_SETSIZE = 1024`——最多只能监控 1024 个 fd
- 每次调用需要重新初始化 fd_set、从用户态拷贝到内核
- 返回后需要 O(n) 扫描全部 fd 找到有数据的

### 4.3 poll——去掉 1024 限制

```c
#include <poll.h>

struct pollfd fds[] = {
    { .fd = listen_fd, .events = POLLIN },
    { .fd = client_fd,  .events = POLLIN },
};
poll(fds, 2, -1);  // nfds=2, timeout=-1=无限等待
// 返回后检查 fds[i].revents & POLLIN
```

比 select 更简单——不用重新初始化，无 fd 数量限制。但仍需 O(n) 扫描。

### 4.4 epoll——Linux 专用，O(1)

```c
#include <sys/epoll.h>

int epfd = epoll_create1(0);

struct epoll_event ev = { .events = EPOLLIN, .data.fd = listen_fd };
epoll_ctl(epfd, EPOLL_CTL_ADD, listen_fd, &ev);

struct epoll_event events[512];  // maxevents 调优：太小→多次 epoll_wait，太大→栈浪费，生产 512-1024
int n = epoll_wait(epfd, events, 64, -1);
for (int i = 0; i < n; i++) {
    if (events[i].data.fd == listen_fd) {
        int cfd = accept(listen_fd, NULL, NULL);
        // 把新 fd 也加入 epoll
    } else {
        char buf[4096];
        int n = read(events[i].data.fd, buf, sizeof(buf));
        if (n <= 0) { close(events[i].data.fd); }
        else { process(buf, n); }
    }
}
```

**epoll 的核心优势**：`epoll_wait` 返回的 events 只包含**有数据的** fd——不需要 O(n) 扫描所有监控的 fd。`epoll_ctl` 只调用一次（注册 fd），不需要每次等待时重新拷贝。

### 4.5 LT vs ET 触发模式

| 模式 | 行为 | 何时用 |
|---|---|---|
| **LT（Level-Triggered，水平触发，默认）** | 只要 fd 有数据可读就持续返回——直到读完 | 简单开发，Redis 用 LT（单线程，避免慢客户端饿死其他客户端） |
| **ET（Edge-Triggered，边沿触发，`EPOLLET`）** | 只在 fd 状态变化（不可读→可读）时返回**一次**——必须循环读到 EAGAIN | 高性能，Nginx 用 ET（减少 epoll_wait 调用次数，配合非阻塞 I/O） |

**ET 的正确循环读**：

```c
while (1) {
    ssize_t n = read(fd, buf, sizeof(buf));
    if (n <= 0) {
        if (n == 0) { close(fd); break; }       // EOF
        if (errno == EAGAIN) break;              // 数据读完—正常退出
        perror("read"); break;
    }
    process(buf, n);
}
```

### 4.6 epoll 的内核实现

epoll 在内核中用两样东西实现 O(1) 通知：

```
epoll_create  →  创建 eventpoll 对象（包含红黑树 + 就绪链表）

epoll_ctl(EPOLL_CTL_ADD)  →  将 fd 插入红黑树（key=fd），注册回调
                              当 fd 有数据到达 → 回调 → 将 fd 加入就绪链表

epoll_wait  →  检查就绪链表是否非空
               非空 → 立即返回链表内容
               空 → 睡眠，等待回调唤醒
               返回时只返回 events 数组中的就绪 fd
```

**关键**：
- **红黑树**存储所有被监控的 fd——增删 O(log n)
- **就绪链表**只存有数据的 fd——epoll_wait 不需要遍历红黑树
- 回调机制是"推"通知——当网卡数据到达 → 内核处理 → 就绪 fd 被回调推入链表

### 4.7 生产进阶：EPOLLONESHOT、惊群与 eventfd

**EPOLLONESHOT**——多线程安全：

```c
struct epoll_event ev = { .events = EPOLLIN | EPOLLONESHOT, .data.fd = fd };
epoll_ctl(epfd, EPOLL_CTL_ADD, fd, &ev);
// fd 就绪后只被通知一次——线程处理后必须 epoll_ctl(MOD, fd, ev) 重新注册
// 防止同一 fd 被多个线程同时处理 → 数据乱序
```

**惊群问题 + EPOLLEXCLUSIVE**：

多进程/多线程各自 `epoll_create` 监听同一 fd——一个客户端连接唤醒所有等待者（accept 惊群）。Linux 4.5+ 的 `EPOLLEXCLUSIVE` 将唤醒限制为一个：

```c
ev.events = EPOLLIN | EPOLLEXCLUSIVE;  // 仅唤醒一个等待者，内核 4.5+
```

注意：`EPOLLEXCLUSIVE` 不能与 `EPOLLONESHOT` 同时使用。

**eventfd / timerfd / signalfd**——现代 epoll loop 标配：

```c
// eventfd——用户态事件通知
int efd = eventfd(0, EFD_NONBLOCK);   // 初始值 0，非阻塞
write(efd, &(uint64_t){1}, 8);       // 触发事件
read(efd, &val, 8);                   // 读取并清零

// timerfd——定时器替代 sleep
int tfd = timerfd_create(CLOCK_MONOTONIC, TFD_NONBLOCK);
timerfd_settime(tfd, 0, &(struct itimerspec){.it_value={1,0}}, NULL);
// epoll_wait 在 1 秒后返回 tfd——不需要 sleep()

// signalfd——统一信号处理为 fd 事件
sigset_t mask;
sigemptyset(&mask); sigaddset(&mask, SIGINT);
int sfd = signalfd(-1, &mask, SFD_NONBLOCK);
// SIGINT 到达 → epoll_wait 返回 sfd → read(sfd) 读取信号信息
```

### 4.8 三种复用对比

| 维度 | select | poll | epoll |
|---|---|---|---|
| fd 数量限制 | 1024（FD_SETSIZE） | 无限制 | 无限制 |
| 内核数据结构 | 位图（fd_set） | 数组（struct pollfd） | 红黑树 + 链表 |
| 每次调用的复杂度 | O(n)（拷贝 + 扫描） | O(n)（拷贝 + 扫描） | O(1)（就绪链表直接返回） |
| fd 注册方式 | 每次调用重新设置 | 每次调用重新设置 | `epoll_ctl` 一次注册 |
| 平台 | 所有 POSIX | 所有 POSIX | **Linux only** |

---

## 5. 总结

| 概念 | 一句话 |
|---|---|
| pipe | 半双工、父子进程、内核缓冲区 |
| FIFO | 命名管道—无亲缘进程可通信 |
| 共享内存 | 最快—零拷贝，需要同步 |
| mmap | 映射文件到内存—匿名/共享内存/MMIO |
| select/poll | POSIX 标准—O(n) 扫描 |
| epoll | Linux 专用—O(1) 通知（红黑树+就绪链表） |
| EPOLLONESHOT | 多线程安全—同一 fd 只通知一次 |
| 惊群 | 一个事件唤醒所有等待者—EPOLLEXCLUSIVE 解 |
| eventfd/timerfd/signalfd | 用户态通知/定时器/信号转为 fd—epoll loop 标配 |
| LT | 水平触发—有数据就返回（Redis） |
| ET | 边沿触发—只在变化时返回（Nginx） |
| PIPE_BUF | 4096 字节原子写入上限—超过可能交错 |
| MAP_POPULATE | 提前预读 page cache—避免后续缺页中断 |
| mmap vs read | 零拷贝/随机访问 vs 一次拷贝/顺序读 |

**核心观点**：IPC 解决"进程间怎么传数据"，I/O 多路复用解决"怎么同时等 1000 个 fd"。共享内存是最快的 IPC（零拷贝），epoll 是最快的多路复用（O(1) 通知）。技术专家不是"知道 select 和 epoll 的区别"，而是"知道 epoll 用红黑树+就绪链表实现 O(1)、知道 ET 模式必须一口气读完所有数据、知道 MAP_PRIVATE 写入不写回文件"。

---

## 验收要点

1. 五种 IPC 各适用于什么场景？最快的是哪个？为什么？
2. pipe 为什么只能用于父子进程？FIFO 如何绕过这个限制？
3. `mmap` 的四种用途各是什么？MAP_PRIVATE 和 MAP_SHARED 的区别？
4. mmap 文件映射后修改 `p[0]`——数据何时写回磁盘？
5. select 的 FD_SETSIZE 限制是多少？如何绕过？
6. poll 相比 select 改进了什么？还有什么仍未解决？
7. epoll 为什么是 O(1)？红黑树和就绪链表分别做什么？
8. LT 和 ET 的区别是什么？Redis 用哪种？Nginx 用哪种？为什么？
9. ET 模式下读数据的正确循环是什么？为什么必须读到 EAGAIN 才停止？
10. epoll 的回调机制是什么？为什么它不需要遍历所有 fd？
11. PIPE_BUF 是什么？多进程同时写 pipe，超过 4096 字节会怎样？
12. EPOLLONESHOT 解决什么问题？多线程处理同一 fd 有什么风险？
13. epoll 惊群是什么？EPOLLEXCLUSIVE 如何解决？Linux 哪个版本引入？
14. eventfd、timerfd、signalfd 各解决什么问题？给出一个 epoll loop 集成示例
15. mmap 和 read 的区别是什么？什么时候用 mmap、什么时候用 read？
