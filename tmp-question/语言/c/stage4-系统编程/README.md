# Stage 4：系统编程

> **本 stage 定位**：C 语言学习路线的第四阶段，系统编程核心。对应 `C-C++技术专家学习路线.md` 的 **Layer 4 + Layer 6 + Layer 7**（OS 接口 + 网络编程 + 数据结构自己写）。
>
> **预计时间**：5-7 周（每日 2-3 小时投入，本 stage 概念密度最高——合并 3 个 Layer = 11 个章节 + 13 个实战项目，可拆分为 OS 接口+网络 4 周 + 数据结构 2 周分开学习）
>
> **完成本 stage 后**：能脱离 stdio 直接用 syscall 写程序、能写一个并发 HTTP 服务器（epoll + 非阻塞 I/O + 线程池）、能自己实现 SDS / 哈希表 / 跳表 / 内存池等数据结构。

---

## 1. 学习目标

完成本 stage 后，应能达成以下 7 个目标：

1. **能讲清楚系统调用 vs 库函数的区别**，用 `strace` 分析任意程序的 syscall
2. **能用 syscall 级 API 写程序**（open/read/write/close 而非 fopen/fread/fwrite）
3. **能讲清楚 fork/exec/wait 三件套** + fork 后子进程继承什么、不继承什么 + 僵尸进程如何避免
4. **能讲清楚信号机制** + 异步信号安全函数的限制 + 信号处理函数中不能调用 `printf`/`malloc` 的原因
5. **能讲清楚 5 种 IPC 机制**（管道 / FIFO / 消息队列 / 共享内存 / 套接字）+ 共享内存为什么最快
6. **能写一个并发 HTTP 服务器**（epoll + 非阻塞 I/O + 线程池，能处理 1 万并发 + keepalive + chunked + 超时管理）
7. **能自己实现 SDS / 哈希表（含渐进式 rehash）/ 跳表 / 内存池** — 读懂 Redis 数据结构源码

---

## 2. 对应 Layer

| Layer | 主题 | 本 stage 章节 |
|---|---|---|
| Layer 4 | OS 接口 | `01-syscall与文件IO.md` / `02-进程与信号.md` / `03-IPC与mmap.md` / `04-IO多路复用.md` |
| Layer 6 | 网络编程 | `05-Socket编程.md` / `06-服务端模型与Reactor.md` / `07-io_uring与Proactor.md` |
| Layer 7 | 数据结构自己写 | `08-SDS与动态数组.md` / `09-链表与哈希表.md` / `10-跳表与树.md` / `11-内存池.md` |

---

## 3. 学习内容（章节清单）

### 3.1 OS 接口（对应 Layer 4）

#### 3.1.1 syscall 与文件 I/O

- 系统调用 vs 库函数（syscall = 用户态 → 内核态切换；库函数可能在用户态实现）
- 两套 API（syscall 级 `open/read/write/close` vs stdio 级 `fopen/fread/fwrite`）
- stdio 缓冲的坑（行缓冲 / 全缓冲 / 无缓冲；fork 后子进程继承父进程的 stdio 缓冲导致重复打印）

#### 3.1.2 进程与信号

- fork / exec / wait 三件套
- fork 后子进程继承什么、不继承什么
- exec 后保留什么（PID / 已打开的 fd（除非 `FD_CLOEXEC`）/ 当前工作目录 / 信号处理被重置为默认，但 `SIG_IGN` 被保留）
- 僵尸进程如何产生与避免（`SIGCHLD` 处理 / `signal(SIGCHLD, SIG_IGN)` / 双 fork）
- 信号机制 + 异步信号安全函数（约 100 个，`write`/`_exit` 安全，`printf`/`malloc` 不安全）
- `SIGKILL` vs `SIGTERM` 的区别

#### 3.1.3 IPC 与 mmap

- 5 种 IPC 机制对比（管道 / FIFO / 消息队列 / 共享内存 / 套接字）
- 共享内存的两种方式（System V `shmget` / POSIX `shm_open` + `mmap`）
- mmap 的 4 种用途（映射文件 / 匿名映射 / 共享内存 / 内存映射 I/O）
- `MAP_PRIVATE` vs `MAP_SHARED`（前者 COW，后者修改对其他映射者可见）

#### 3.1.4 I/O 多路复用

- select / poll / epoll / kqueue 对比
- LT（水平触发）vs ET（边沿触发）
- **Redis 用 epoll + LT**（`ae_epoll.c` 无 `EPOLLET`，单线程模型下 LT 更简单，避免慢客户端饿死其他客户端）
- **Nginx 用 epoll + ET + 非阻塞**（`ngx_epoll_module.c` 含 `EPOLLET`，ET 每次状态变化只通知一次，减少 `epoll_wait` 调用次数）
- epoll 内部实现（红黑树管理 fd + 双向链表管理就绪 fd — 详见 §3.3 数据结构 + §stage5 源码阅读）

### 3.2 网络编程（对应 Layer 6）

#### 3.2.1 Socket 编程

- BSD Socket API（socket / bind / listen / accept / connect / send / recv）
- `SO_REUSEADDR` 的作用（允许绑定到 TIME_WAIT 状态的端口）
- `listen` 的 backlog 含义
- accept 在 ET 模式下如何处理（必须循环 accept 直到 EAGAIN）

#### 3.2.2 服务端模型与 Reactor

- 服务端模型演进（迭代 → 多进程 → 多线程 → prefork/prethread → Reactor → Proactor → 多 Reactor）
- Reactor 三种模式：
  - 单 Reactor 单线程（Redis 6.0 之前）
  - 单 Reactor 多线程（Redis 6.0+ IO 多线程：主线程负责 accept + 命令执行，IO 线程负责 read parse + write send）
  - 主从 Reactor 多线程（Nginx：main Reactor 处理 accept，sub Reactor 处理 read/write）

#### 3.2.3 io_uring 与 Proactor

- I/O 模型对比（阻塞 / 非阻塞 / 信号驱动 / I/O 多路复用 / 异步 I/O）
- io_uring（Linux 5.1+，2019）：内核与用户态共享两个环形缓冲区（SQ / CQ），批量化提交与收割大幅减少系统调用频率，接近零系统调用开销的理想情况
- Proactor 模式（Windows IOCP / io_uring）

#### 3.2.4 协议设计

- 4 种格式对比（定长 / 边界符 / TLV / 长度前缀）
- TLV（Type-Length-Value）自描述二进制格式
- RESP（REdis Serialization Protocol，Redis 序列化协议）

### 3.3 数据结构自己写（对应 Layer 7）

#### 3.3.1 SDS 与动态数组

- SDS（Simple Dynamic Strings，Redis）设计要点（二进制安全 / O(1) 长度 / 预分配 / 惰性释放 / 与 `char *` 兼容）
- 动态数组（vector 模式）：扩容策略（2 倍）

#### 3.3.2 链表与哈希表

- 侵入式链表（Linux kernel `list.h` 风格）vs 非侵入式
- 侵入式优势（一个数据可同时在多个链表 / 无需 `void *` 类型安全 / 不需要单独分配节点）
- 哈希表：开地址法 vs 链地址法
- Redis dict 渐进式 rehash（扩容时不一次性搬迁，每次操作搬迁一部分）

#### 3.3.3 跳表与树

- 跳表（Redis 有序集合）：期望 O(log n)，实现比红黑树简单，范围查询友好
- 并发跳表（Java ConcurrentSkipListMap 思路：CAS + 标记删除）
- 树：BST / AVL / 红黑树 / B 树 / B+ 树（数据库索引）

#### 3.3.4 内存池

- 为什么需要内存池（减少 malloc 次数 / 减少内存碎片 / 批量释放）
- nginx palloc 模型（每次 alloc 大块，小块从大块中切，不释放，整个池释放时一次性 free）
- Redis zmalloc（包装 malloc，记录已分配总量，支持多种后端）

---

## 4. 实战项目

### 4.1 OS 接口部分

1. **实现一个简易 shell**：`fork` + `exec` + `wait` + 管道 + 重定向
2. **写一个 echo 服务器**（单进程 select 版 / 多进程 fork 版 / epoll 版，对比性能）
3. **用 `mmap` 实现共享内存聊天室**（父进程 + 子进程通过共享内存通信，用信号量同步）
4. **用 `strace` 分析 `printf("Hello\n")` 的实际系统调用**

### 4.2 网络编程部分

5. **写一个 HTTP/1.1 静态文件服务器**（epoll + 非阻塞 I/O + sendfile）
   - **生产级 checklist**：keepalive / chunked transfer encoding / pipelining / malformed request 容错 / 超时管理（读/写/keepalive）/ 连接数限制 / 慢连接检测 / 请求头大小限制（防 Slowloris 变体攻击）/ Host 头校验（虚拟托管场景）
6. **写一个 Redis 协议（RESP）解析器**
7. **实现主从 Reactor 模型**（main reactor 接受连接，sub reactors 处理 I/O）
8. **用 io_uring 重写 echo 服务器**（Linux 5.1+）

### 4.3 数据结构部分

9. **实现 SDS**（参考 Redis `sds.c`）
10. **实现侵入式双向链表**（参考 Linux `list.h`）
11. **实现哈希表**（链地址 + 渐进式 rehash）
12. **实现跳表**（参考 Redis `t_zset.c`）— 进阶：实现并发跳表（CAS + 标记删除）
13. **实现内存池**（参考 nginx `palloc`）

---

## 5. 参考书章节

| 主题 | 极致C | Fluent C | Practical | Mastering | 高效调试 | Memory Thinking |
|---|---|---|---|---|---|---|
| syscall 与文件 I/O | Ch10, 11 | — | 全书 | Ch2-5 | — | — |
| 进程与信号 | Ch17, 18 | — | — | Ch4 | — | — |
| IPC 与 mmap | Ch19 | — | — | Ch5 | — | — |
| I/O 多路复用 | — | — | — | — | — | — |
| Socket 编程 | Ch20 | — | — | Ch9 | — | — |
| 服务端模型与 Reactor | — | — | — | — | — | — |
| io_uring | — | — | — | — | — | — |
| SDS 与动态数组 | — | Ch6-10（设计模式视角） | — | — | — | — |
| 链表与哈希表 | — | Ch6-10 | — | — | — | — |
| 跳表与树 | — | Ch6-10 | — | — | — | — |
| 内存池 | — | Ch6-10 | — | — | — | — |

**重点参考**：
- Practical 全书（Linux 系统编程入门 — syscall / 文件 / Shell）
- Mastering Ch2-5, 9（系统编程进阶 — File Ops / Advanced I/O / Process / IPC / Network）
- 极致C Ch10-11, 17-20（Unix 历史 / 系统调用 / 进程执行 / 进程同步 / IPC / 套接字）
- Fluent C Ch6-10（设计模式视角看数据结构）

**注**：数据结构部分 6 本书覆盖不足，主要参考 Redis / Nginx / Linux kernel 源码（详见 stage5）。

---

## 6. 完成标准

能回答以下 12 个问题即算完成：

1. 系统调用 vs 库函数的区别？`printf` 最终调用哪个 syscall？
2. stdio 缓冲的三种模式（行缓冲 / 全缓冲 / 无缓冲）？为什么 fork 后子进程可能重复打印？
3. fork 后子进程继承什么、不继承什么？exec 后保留什么？
4. 僵尸进程如何产生？如何避免？
5. 信号处理函数中为什么不能调用 `printf` / `malloc`？异步信号安全函数有哪些？
6. 5 种 IPC 机制的对比？共享内存为什么最快？
7. mmap 的 4 种用途？`MAP_PRIVATE` vs `MAP_SHARED` 的区别？
8. select / poll / epoll 的对比？LT vs ET 的区别？
9. Redis 用 epoll + LT 还是 ET？Nginx 呢？为什么？
10. Reactor 的三种模式是什么？Redis 6.0 IO 多线程的实际行为是什么？
11. io_uring 相比 epoll 的优势是什么？
12. SDS 的 5 个设计要点？Redis dict 渐进式 rehash 如何工作？

---

## 7. 下一步

完成本 stage 后，进入 **`stage5-工程实践/`**（对应 Layer 8-11：OOP/FFI + 调试 + 源码阅读 + 生产化）。

stage5 将进入工程化阶段，学习 OOP in C（封装/继承/多态）、错误处理、模块化、测试、构建、FFI 互操作，掌握 GDB / ASan / perf 调试与性能优化，阅读 Redis / Nginx / Memcached 真实源码，最后学习生产化工程实践（可观测性 / 优雅关闭 / 热升级 / 安全加固 / CI/CD）。
