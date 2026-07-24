# Socket 编程与 Reactor

> **对应 Layer**：Layer 6（网络编程），参考 C-C++技术专家学习路线.md § Layer 6  
> **参考书章节**：极致C 第19-20章；Mastering 第9章；需补充《UNIX 网络编程 卷1》Stevens  
> **前置依赖**：S4Ch03（epoll—本章用它做事件循环）+ S4Ch01（fd本质—socket也是fd）  
> **主线标准**：C11/C17 + Linux 系统调用（POSIX/Berkeley Sockets）

---

## 1. 引言：从文件描述符到网络连接

S4Ch01 讲了"文件描述符只是整数"，S4Ch03 讲了 epoll 能让一个线程监控数千个 fd。socket 是这一切的自然延伸——socket 也是 fd，`read`/`write`/`close` 对 socket 同样适用。区别在于 socket 的另一端跨越了网络。

---

## 2. BSD Socket API

### 2.1 服务端标准流程

```c
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

int listen_fd = socket(AF_INET, SOCK_STREAM, 0);

// SO_REUSEADDR——允许重启后立即复用 TIME_WAIT 状态的端口
int opt = 1;
setsockopt(listen_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

struct sockaddr_in addr = {
    .sin_family = AF_INET,
    .sin_port   = htons(8080),          // 主机字节序→网络字节序（大端）
    .sin_addr.s_addr = INADDR_ANY       // 监听所有网卡接口
};
bind(listen_fd, (struct sockaddr *)&addr, sizeof(addr));

listen(listen_fd, 128);   // backlog——内核全连接队列最大长度

while (1) {
    int conn_fd = accept(listen_fd, NULL, NULL);
    // 处理 conn_fd
    close(conn_fd);
}
```

### 2.2 关键参数背后的内核机制

**`SO_REUSEADDR`**：允许绑定到 TIME_WAIT 状态的端口。没有它，服务重启时需要等 2MSL（约 60-120 秒）端口才释放。几乎每个 TCP 服务都开这个选项。

**`listen` 的 backlog**：不是"等待 accept 的连接数"——是**全连接队列**（已完成三次握手的连接）的最大长度。Linux 实际值 = `backlog + 1`（内核曾经的最小值），但在 `/proc/sys/net/core/somaxconn` 处被截断。生产环境设 128 或更高。连接请求超过全连接队列 → 客户端收到 `ECONNREFUSED`。

**`accept` 的阻塞行为**：全连接队列为空时，accept 阻塞。配合 epoll EPOLLIN 事件——只有全连接队列有连接时才唤醒。ET 模式下必须循环 accept 直到 `EAGAIN`。

**TCP 连接耗时（三次握手）**：`client SYN → server SYN-ACK → client ACK`。在 accept 返回前，三次握手已完成，连接已在全连接队列中等待。

### 2.3 客户端

```c
int fd = socket(AF_INET, SOCK_STREAM, 0);
struct sockaddr_in addr = { AF_INET, htons(8080) };
inet_pton(AF_INET, "127.0.0.1", &addr.sin_addr);

connect(fd, (struct sockaddr *)&addr, sizeof(addr));
write(fd, "GET / HTTP/1.1\r\n\r\n", 18);
char buf[4096];
read(fd, buf, sizeof(buf));
close(fd);
```

---

## 3. 服务端模型演进

### 3.1 迭代模型——一次一个连接

```c
while (1) {
    int cfd = accept(fd, NULL, NULL);
    handle(cfd);  // 阻塞处理——其他连接排队等
    close(cfd);
}
// 问题：handle 处理 10ms → 每秒只能处理 100 个连接
```

### 3.2 多进程并发（fork per connection）

```c
while (1) {
    int cfd = accept(fd, NULL, NULL);
    if (fork() == 0) {  // 子进程
        close(listen_fd);
        handle(cfd);
        _exit(0);
    }
    close(cfd);  // 父进程关闭——子进程持有
}
// Apache prefork 模型——隔离性强，但进程开销大
```

### 3.3 多线程并发（thread per connection）

```c
void *worker(void *arg) {
    int cfd = *(int *)arg;
    handle(cfd);
    close(cfd);
    return NULL;
}
while (1) {
    int cfd = accept(fd, NULL, NULL);
    pthread_t tid;
    pthread_create(&tid, NULL, worker, &cfd);
    pthread_detach(tid);
}
// Memcached 线程模型——线程开销小，但全局锁竞争
```

### 3.4 Prefork/Prethread——预创建

在主循环之前预创建 N 个进程/线程。每个在 accept 上竞争。Nginx prefork（旧版）和 Apache prefork 都是这个模式——减少了 fork 的开销。

### 3.5 Reactor（事件驱动）

一个线程 + epoll 同时监控所有连接。**现代高并发标准模型**——Redis（6.0前）、Nginx、Netty 都基于此：

```
主循环：
    epoll_wait → 返回哪些 fd 有数据
    for each ready fd:
        if fd == listen_fd:
            accept → 新连接 → epoll_ctl(ADD)
        else:
            read data → process → write response
```

### 3.6 主从 Reactor（多核优化）

主 Reactor 线程只做 accept + 分发连接。多个从 Reactor 线程各自在自己的 epoll loop 中处理 I/O 事件：

```
主 Reactor（线程0）：accept → 分发连接给从 Reactor
从 Reactor1（线程1）：epoll_wait → read/write 连接组1
从 Reactor2（线程2）：epoll_wait → read/write 连接组2
...
```

**Nginx 模型**：主进程 accept，然后通过 `SO_REUSEPORT` 或锁将连接分给 worker 进程。每个 worker 有自己的 epoll loop。

### 3.7 Proactor（异步 I/O）

不是"等待 fd 可读通知我"——而是**内核直接把数据读完再通知我**。Windows IOCP 是 native Proactor。Linux 的 `io_uring`（5.1+）是向着 Proactor 方向演进的新一代异步 I/O。

### 3.8 六种模型速查

| 模型 | 代表 | 适用 | 缺点 |
|---|---|---|---|
| 迭代 | — | 学习 | 串行 |
| fork per conn | Apache prefork | 隔离要求高 | 进程开销大 |
| thread per conn | Memcached | 中等并发 | 线程开销+全局锁 |
| prefork/prethread | Nginx 早期 | 减少创建开销 | 仍受进程数限制 |
| **Reactor** | **Redis/Nginx** | **高并发长连接** | **编程复杂** |
| Proactor | io_uring | 异步 I/O | Linux 支持较新 |

---

## 4. Reactor 实战：简易 HTTP 服务器

```c
#include <sys/epoll.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>

#define MAX_EVENTS 512
#define BUF_SIZE 4096

void set_nonblocking(int fd) {
    fcntl(fd, F_SETFL, fcntl(fd, F_GETFL) | O_NONBLOCK);
}

int main() {
    int lfd = socket(AF_INET, SOCK_STREAM, 0);
    int opt = 1;
    setsockopt(lfd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &opt, sizeof(opt));

    struct sockaddr_in addr = { AF_INET, htons(8080), { INADDR_ANY } };
    bind(lfd, (struct sockaddr *)&addr, sizeof(addr));
    set_nonblocking(lfd);
    listen(lfd, SOMAXCONN);

    int epfd = epoll_create1(0);
    struct epoll_event ev = { .events = EPOLLIN, .data.fd = lfd };
    epoll_ctl(epfd, EPOLL_CTL_ADD, lfd, &ev);

    struct epoll_event events[MAX_EVENTS];
    while (1) {
        int n = epoll_wait(epfd, events, MAX_EVENTS, -1);
        for (int i = 0; i < n; i++) {
            int fd = events[i].data.fd;
            if (fd == lfd) {
                while (1) {  // ET 模式必须循环 accept
                    int cfd = accept(lfd, NULL, NULL);
                    if (cfd < 0) { if (errno == EAGAIN) break; perror("accept"); break; }
                    set_nonblocking(cfd);
                    ev.events = EPOLLIN | EPOLLET;  // 边沿触发
                    ev.data.fd = cfd;
                    epoll_ctl(epfd, EPOLL_CTL_ADD, cfd, &ev);
                }
            } else {
                char buf[BUF_SIZE];
                while (1) {  // ET 模式必须循环读到 EAGAIN
                    int r = read(fd, buf, sizeof(buf));
                    if (r > 0) { /* process & write response */ }
                    else if (r == 0) { close(fd); break; }
                    else if (errno == EAGAIN) break;
                    else { perror("read"); close(fd); break; }
                }
            }
        }
    }
}
```

---

## 5. 协议设计

socket 传输的是**字节流**——TCP 不保留消息边界。你需要一个协议来告诉接收端"一条消息从哪开始、到哪结束"。

| 格式 | 原理 | 优点 | 缺点 | 代表 |
|---|---|---|---|---|
| **定长** | 每条消息固定 N 字节 | 简单，解析极快 | 浪费带宽 | 固定头协议 |
| **边界符** | `\r\n` 结尾（HTTP 头部） | 简单 | 数据中不能含边界符 | HTTP/1.x, Redis RESP |
| **长度前缀** | 前 4 字节 = 消息长度，后跟数据 | 高效，二进制安全 | 需先读长度 | TCP 自定义 |
| **TLV**（Type-Length-Value） | 类型+长度+值，自描述 | 灵活，可扩展 | 解析复杂 | Protobuf, HTTP/2 |

**Redis RESP 协议示例**：

```
*3\r\n           ← 数组，3 个元素
$3\r\n           ← 字符串，长度 3
SET\r\n           ← 命令
$3\r\n           ← 字符串，长度 3
key\r\n           ← 键
$5\r\n           ← 字符串，长度 5
value\r\n         ← 值
```

---

## 6. 生产 socket 配置

| 选项 | 含义 | 何时用 |
|---|---|---|
| `SO_REUSEADDR` | 复用 TIME_WAIT 端口 | **所有 TCP 服务** |
| `SO_REUSEPORT` | 多进程绑定同一端口（内核负载均衡，Linux 3.9+） | 多 worker 模型+Nginx |
| `TCP_NODELAY` | 禁用 Nagle 算法（小包不等待合并） | 低延迟场景（游戏/交易） |
| `TCP_CORK` | 启用数据塞子（强制小包在发送前合并） | 批量写入优化（日志/文件��输） |
| `SO_KEEPALIVE` | TCP 长连接保活（2 小时默认间隔） | 需要心跳检测 |
| `SO_LINGER` | 控制 close 行为——是否等待缓冲区清空 | 优雅关闭 |

**`SO_REUSEPORT` 的生产意义**：多个进程同时 `bind` 同一端口。内核将客户端的 SYN 包分散到不同进程——实现用户态零锁的网络分发。Linux 3.9+（2013），已被 Nginx 广泛使用。

**`TCP_NODELAY` vs `TCP_CORK`**——互斥。`TCP_NODELAY` 是立即发送（不等待），`TCP_CORK` 是累积到阈值再发。低延迟用 NODELAY，批量大块写入用 CORK。

---

## 7. 总结

| 概念 | 一句话 |
|---|---|
| Socket | 文件描述符 + 网络地址——`read/write/close` 同文件 I/O |
| `SO_REUSEADDR` | 允许快速重启——复用 TIME_WAIT 端口 |
| TCP 三次握手 | 内核在 accept 前完成——连接已在全连接队列 |
| Reactor | 单线程 + epoll 管理数千连接——Redis/Nginx 模型 |
| 主从 Reactor | accept 分发 + 多 epoll loop——充分利用多核 |
| 协议设计 | TCP 是字节流——需要定长/边界符/长度前缀来划分消息 |
| `SO_REUSEPORT` | 多进程绑同一端口，内核负载分担 |
| `TCP_NODELAY` | 禁用 Nagle——低延迟必开 |

**核心观点**：socket 编程的本质是 fd + epoll 的延伸——文件 I/O 的 `read`/`write` 对 socket 同样适用。Reactor 让一个线程同时处理数千连接——不是"多线程每个连接一个线程"，而是"所有连接共享一个事件循环"。技术专家不是"知道 select 和 epoll 的区别"，而是"知道 Reactor 如何从 fork 演进而来的完整历史、知道 ET 模式下 accept 必须循环读到 EAGAIN、知道生产环境的端口复用和 Nagle 算法对延迟的影响"。

---

## 验收要点

1. `socket()`/`bind()`/`listen()`/`accept()` 四步各做了什么？TCP 三次握手在哪步完成？
2. `SO_REUSEADDR` 解决什么问题？没有它服务重启会发生什么？
3. `listen` 的 backlog 是什么？超出全连接队列时客户端收到什么？
4. 服务端模型从迭代到 Reactor 的演进——每一级解决了上一级什么问题？
5. Reactor 和主从 Reactor 的区别？Redis 和 Nginx 各属于哪种？
6. ET 模式 accept 为什么必须循环直到 EAGAIN？不循环会怎样？
7. 四种协议设计（定长/边界符/长度前缀/TLV）各有什么优缺点？
8. `SO_REUSEPORT` 解决什么问题？多进程如何同时绑定同一端口？
9. `TCP_NODELAY` 和 `TCP_CORK` 的区别？什么是 Nagle 算法？
10. 用 epoll + ET + 非阻塞写一个简易 HTTP 1.1 服务器——能解析 GET 请求 + 返回静态文件
