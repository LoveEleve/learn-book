# 07-系统编程 — 全视角完备性提问 (5 身份 x 4-5 题 = 23 题)

> 验证方法: 每个身份问 Why/What 非 How, 每题必须能用大纲中 1-3 篇的内容答出
> 书: B1 Linux-UNIX系统编程手册(13KPs) + B2 程序员进阶之路(5KPs) = 18KPs / 7篇大纲

---

## 1. 开发者视角 (Developer) — 5 题

Q1-1: `write(fd, buf, 4096)` 返回了 1024 — 这是错误吗？如果是 socket + O_NONBLOCK，返回了 -1 且 errno=EAGAIN — 这两个场景的本质区别是什么？
   → 覆盖: 01 (Partial Write + 非阻塞 I/O)

Q1-2: 你不小心写了 `mmap(NULL, sz, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0)` 之后没有 `msync` — 什么时候修改会自动写回磁盘？如果进程 crash 了，上次 `msync` 之后的修改会丢吗？
   → 覆盖: 02 (mmap + msync + 脏页回写)

Q1-3: fork 后你在子进程中 `close(fd)`、父进程也 `close(fd)` — 文件什么时候真正被内核关闭？如果 fork 后 fd 没设 `O_CLOEXEC`、然后又 exec 了新程序 — 这个 fd 会泄漏到新程序吗？
   → 覆盖: 03 (fork fd 继承 + O_CLOEXEC + close 引用计数)

Q1-4: 两个线程 A 和 B 各 `pthread_mutex_lock(&m)` — 为什么这不是死锁？如果线程 A 锁了 m1 后去锁 m2、线程 B 锁了 m2 后去锁 m1 — 死锁的两个必要条件是什么？怎么用 `pthread_mutex_trylock` 打破？
   → 覆盖: 04 (mutex + 死锁 + trylock)

Q1-5: 你在信号处理器里调了 `printf("caught SIGTERM\n")` — 程序可能在打印时 deadlock。为什么？如果换成 `write(STDOUT_FILENO, "caught\n", 7)` 就安全了——两者都是写字符串，区别在哪？
   → 覆盖: 05 (async-signal-safe + signalfd 替代方案)

---

## 2. 性能工程师视角 (Performance) — 5 题

Q2-1: 一个写日志的服务用 `fprintf` 每秒写 10000 行、磁盘 IOPS 只有 500 — 为什么 `fprintf` 能比磁盘 IOPS 高 20 倍？stdbuf 缓冲区在哪里、有多大？
   → 覆盖: 01 (stdio 缓冲—全缓冲/行缓冲/大小 + 页缓存)

Q2-2: 你做了一个 epoll LT 的 TCP 服务器，压测时 CPU 用户态和内核态都 50% — 换成 ET 后 CPU 降到 30%。但这 20% 省的是哪部分？ET 和 LT 的内核路径差别在哪一步？
   → 覆盖: 02 (epoll LT vs ET: ready list 操作次数差异)

Q2-3: 一个多线程服务 RSS 从启动的 200MB 涨到 2GB 后稳住了 — `valgrind` 显示无泄漏。这 1.8GB 是什么？`malloc_info` 的 `<unsorted>` bin 展开长什么样？是泄漏还是碎片？
   → 覆盖: 06 (伪内存泄漏 + malloc_info + arena 不归还机制)

Q2-4: 你用 `perf top` 看到 `_int_malloc` 占 15% CPU — `malloc` 为什么这么慢？glibc 是拿什么锁？为什么 per-thread arena 可以缓解但不会消除锁竞争？
   → 覆盖: 05 (malloc 全家桶—arena 锁 + per-thread arena)

Q2-5: C++ 无锁队列的 push 用 `head.store(idx+1, release)` — 为什么是 release 不是 seq_cst？换成 seq_cst 你的吞吐从 50M/s 掉到 30M/s — x86 上 release 比 seq_cst 便宜在哪条指令上？
   → 覆盖: 07 (memory_order: release vs seq_cst + x86-TSO)

---

## 3. SRE 视角 (SRE) — 4 题

Q3-1: 凌晨 3 点生产服务 OOM——第一个要跑的命令不是 `gdb` 或 `valgrind`，而是 `smem -P <PID>` 和 `cat /proc/<PID>/maps`。maps 输出里什么 pattern 暗示是 malloc 碎片而非泄漏？
   → 覆盖: 06 (进程地址空间 + smem + 伪泄漏判断链)

Q3-2: 你的 nginx 以守护进程模式运行——如果它没有 `chdir("/")` 后果是什么？运维要 unmount `/home` 分区时被 `device is busy` 拒绝 — 这个 busy 来自哪里？
   → 覆盖: 03 (守护进程: chdir 的重要性)

Q3-3: 生产容器中 `ulimit -n` 默认 1024 — 你的服务 accept 到了第 1025 个连接返回了什么？`errno` 是什么？不修改 ulimit 代码中怎么优雅地 "等一分钟再试" 而不是 crash？
   → 覆盖: 01 (fd 限制 + EMFILE) + 06 (rlimit: RLIMIT_NOFILE)

Q3-4: `kill -9 1234` 之后发现了一个 <defunct> 僵尸进程 — kill -9 为什么杀不死僵尸？僵尸是进程状态还是资源泄漏？什么时候内核会清理掉它？
   → 覆盖: 03 (僵尸进程: 不能 kill, 只能父进程 wait 回收)

---

## 4. 架构师视角 (Architect) — 5 题

Q4-1: 你的系统要在 fork+exec 创建子进程和自己管理线程池之间做架构选择 — fork 的 COW 优势是共享代码段/共享库，但线程也能共享 — 进程和线程的"共享什么"本质上不同在哪两个地方？
   → 覆盖: 03 (fork 进程) + 04 (pthread 线程) — 地址空间 vs fd 表

Q4-2: 服务中要用定时器做"每个客户端连接 30 秒心跳超时" — 有四种方案: `alarm/SIGALRM`、`setitimer`、`timerfd`、`clock_gettime + poll` — 与 5000 个 epoll fd 共存在同一条线程中，最佳方案是什么？
   → 覆盖: 05 (timerfd vs 信号定时器 + epoll 集成)

Q4-3: 你准备从 C `pthread` 迁移到 C++ `std::thread` — 新系统的核心是线程池 + 无锁 SPSC 队列 + atomic — 迁移后需要保留哪些 POSIX API？C++ 标准库的和 POSIX 的重量级区别在哪？
   → 覆盖: 04 (pthread 1:1 模型) + 07 (C++ atomic + 无锁队列)

Q4-4: 内存屏障是 CPU 层面概念 — 在 x86(AVX-512/TSO)上编好通过的无锁代码，在 ARM(弱一致)上可能出错 — 设计跨平台 C++ 代码时怎么保证 `atomic` 的可移植性？C++ 标准库在 ARM 上会生成什么屏障？
   → 覆盖: 07 (x86-TSO vs ARM 弱模型 + C++ atomic memory_order)

Q4-5: 从内功修炼全 7 卷来看 — 01-OS 物理内存、02-内存深度、03-文件系统、04-网络、05-系统性能、06-eBPF、07-系统编程 — 给学生和社招各推荐一个最短阅读路径（≤3 卷），分别满足"面试能说清楚"和"产线能排障"。
   → 覆盖: 全 7 篇 + 全 7 域 — 验证大纲的模块化性和方向感

---

## 5. 学生视角 (Student) — 4 题

S5-1: `int x = 1; fork(); x = 2;` — 这一行代码读起来像是"先设 x=1，再 fork，再设 x=2"。父进程和子进程最后 x 各是多少？为什么父进程看不到子进程的 `x=2`？
   → 覆盖: 03 (fork COW + 地址空间独立)

S5-2: epoll 文档写"LT 是默认并兼容 select/poll"——ET 听起来更好(少通知=高性能)。为什么不是所有人都用 ET？什么 bug 是 ET 而不是 LT 最多出现的？
   → 覆盖: 02 (epoll LT vs ET 选择指南 + 非阻塞 I/O + EAGAIN 循环)

S5-3: 你在 C++ 里写了 `atomic<int> x{0}` — 两个线程各做了 `x.fetch_add(1)` 100 次，最后结果是 200（永远不会是 199）。volatile int 做同样的操作可能得到 200 也可能不是。C++ atomic 和 volatile 的本质区别是什么？
   → 覆盖: 07 (C++ atomic: 原子性 + memory_order) — 验证 atomic vs volatile 的教学

S5-4: 整个内功修炼 7 卷 79 篇读完后，你最震惊的三个事实是什么？至少一个与"进入系统调用看起来只是函数调用但成本远超你的想象"不同角度。
   → 覆盖: 全 7 域 — 验证教学叙事是否能产生深层理解
