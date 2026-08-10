# 惊群问题 + SO_REUSEPORT + EPOLLEXCLUSIVE + Nginx 案例

> Cluster C: 5 KPs | 依赖: 11-进程模型 + epoll 相关知识 | 读者基线: 理解进程模型和网络编程基础

---

### 1. 惊群现象 — 多个进程同时被唤醒来抢一个 accept
  - 经典场景: 多进程/线程同时 accept 同一个 socket → 新连接到达 → 所有等待者被唤醒(包括调用 epoll_wait 的) → 只有一个 accept 成功 → 其余返回 EAGAIN → 大量无效上下文切换 → CPU 浪费 (net/ipv4/inet_connection_sock.c:497)
  - epoll 惊群: 多个进程 epoll_wait 同一 fd → 事件到达 → 全部唤醒 → 只有一人拿到事件 → 其余回去睡 → 高负载下恶性循环
  - 根本原因: accept/select/poll/epoll 的唤醒语义是"唤醒所有等待者"而非"唤醒一个"

### 2. SO_REUSEPORT — Socket 按 Hash 分配到特定进程
  - 原理: socket(AF_INET, SOCK_STREAM, 0); setsockopt(sockfd, SOL_SOCKET, SO_REUSEPORT, ...); bind → 内核按五元组 hash 将连接分配到特定 sk → 只有一个进程的 epoll 收到事件 → 无惊群 (net/core/sock_reuseport.c:35)
  - 内核实现(4.5+): `reuseport_select_sock` → `inet_lookup_reuseport` → 按源 IP/端口 hash → `reuseport->socks[hash]` → 绑定到该进程 → 每个进程的 epoll 独立 (net/ipv4/inet_hashtables.c:273)
  - 注意: SO_REUSEPORT 每个进程需独立 bind(同 IP/端口) → 不能一进程多线程(epoll_ctl 加 EPOLLEXCLUSIVE)

### 3. EPOLLEXCLUSIVE — Epoll 只唤醒一个等待者
  - 标志: `epoll_ctl(epfd, EPOLL_CTL_ADD, fd, &event)` + EPOLLEXCLUSIVE → 内核保证事件只唤醒该 fd 上的一个 epoll_wait 调用者 — 不唤醒全部 (fs/eventpoll.c:1930)
  - 两个机制协同: SO_REUSEPORT(连接分配) + EPOLLEXCLUSIVE(事件唤醒) → 各进程收自己的连接 → 零惊群
  - 4.5 内核前: accept 惊群 → nginx 用 accept_mutex(互斥锁)规避 → 一次只有一个 worker accept

### 4. Nginx/memcached 案例 — 工业级惊群解决方案
  - master 进程: 配置管理(读取 nginx.conf) → signal 通知 worker 重载配置 → 本身不处理请求
  - worker 进程: N 个(通常 = CPU 核心) → 每个 worker 独立 epoll 事件循环 → socket(SO_REUSEPORT) → bind → 每个 worker 独立监听端口 → accept 自己的连接
  - 事件驱动: `use epoll` → `multi_accept on`(一次性接受所有连接) → `EPOLLEXCLUSIVE` 保证无重复唤醒
  - memcached 对比: 多线程模型(非多进程) → 单个 epoll 事件循环 + libevent → 不需要 SO_REUSEPORT → 用 `memcached -t N` 绑定各线程到 CPU 核

### 5. 收束
  - 惊群 = 唤醒全部等待者 + 只有一人成功 = 无效上下文切换
  - SO_REUSEPORT(连接分发) + EPOLLEXCLUSIVE(事件唤醒) = 零惊群
  - Nginx 用这个双层机制实现每核独立处理连接，无互斥锁

---

### 核心悬念
**"网络流量到了网卡后变成了 epoll 事件 — 文件系统这边的数据怎么从磁盘到内存再到用户程序？VFS 统一接口是什么？ext4 的 extent 树怎么存大文件？"**

→ 引出 13-VFS 四大对象 + inode/dentry + ext4 extent 树 + 日志 JBD2
