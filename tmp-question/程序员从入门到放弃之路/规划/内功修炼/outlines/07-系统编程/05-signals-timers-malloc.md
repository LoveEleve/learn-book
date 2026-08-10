# 信号 + 定时器 + malloc — 信号处理全链路 + 高精度定时 + 内存分配器

> Cluster B: 8 KPs | 依赖: A (01-02) + B (03 fork + 04 pthread) | 读者基线: 会用 kill 命令，写过 SIGTERM 处理器，理解线程概念

---

### 1. 信号处理全链路 — sigaction + 信号掩码
  - `sigaction(SIGTERM, &sa, NULL)`: 三类 sa_handler/sa_sigaction(带 siginfo_t 上下文)/SIG_DFL/SIG_IGN — SA_SIGINFO 可拿到发送者 PID/UID/错误地址 (man 2 sigaction)
  - 信号掩码: `sigprocmask(SIG_BLOCK/SIG_UNBLOCK/SIG_SETMASK, &set, &old)` — 阻塞信号≠忽略信号→解除阻塞后立即递送 (man 2 sigprocmask)
  - `sigwait(&set, &sig)`: 同步等待信号——在专用线程中等待→不用信号处理器→避开所有可重入限制 (man 2 sigwait)
  - `signalfd`: 把信号变成 fd → epoll_ctl 注册 → epoll_wait → 读到 `struct signalfd_siginfo`——完美融入事件循环 (man 2 signalfd)
  - 信号分类: 标准信号(SIGTERM/SIGINT/SIGSEGV 1-31) + 实时信号(SIGRTMIN–SIGRTMAX, 支持排队+附带数据 `sigqueue`) (man 7 signal)

### 2. 信号与线程 — per-thread 信号掩码
  - 每线程独立信号掩码: `pthread_sigmask`(同 sigprocmask 但 POSIX 保证) — 线程 1 屏蔽 SIGTERM 不影响线程 2 (man 3 pthread_sigmask)
  - 信号递送: 进程信号 → 任意一条不屏蔽此信号的线程处理 — `kill(pid, SIGTERM)` 对整个进程 (B1 Ch10 §5)
  - `pthread_kill(tid, SIGUSR1)`: 给特定线程发信号 (man 3 pthread_kill)
  - `tgkill(pid, tid, SIGUSR1)`: 底层系统调用——精确到某进程某线程——JVM 线程诊断用 (man 2 tgkill)
  - 信号处理器限制: handler 中只能调 async-signal-safe 函数——`write()` 安全 / `printf()` 非法——多数线程同步原语(mutex/cond/sem)不安全 (man 7 signal-safety)

### 3. 可重入限制 — async-signal-safe 严格列表
  - 安全函数清单(~120个): `_exit()`/`write()`/`read()`/`open()`/`close()`/`sem_post()`/`signal()`/`fork()` — 来源 POSIX.1 (man 7 signal-safety)
  - 不安全函数: `malloc`/`free`(内部 arena 锁) / `printf`(内部 malloc) / `pthread_mutex_lock`(阻塞)→信号处理器调用这些=未定义行为 (B1 Ch10 §4)
  - 替代方案: signalfd(将信号变成 fd→在正常线程上下文中读写) → 完全避开信号安全的坑 (B1 Ch10 §5)

### 4. 高精度定时器 — clock_gettime + timerfd
  - `clock_gettime(CLOCK_MONOTONIC, &ts)`: 单调时间不受系统时间调校影响——测量 elapsed time 必须用 MONOTONIC (man 2 clock_gettime)
  - `CLOCK_REALTIME`: 墙钟时间——受 NTP 调整/jump 影响——仅用于显示人类可读时间 (man 2 clock_gettime)
  - `CLOCK_MONOTONIC_RAW`: 无 NTP 频率校正的纯硬件时间——基准测试首选 (man 2 clock_gettime)
  - `timerfd_create(CLOCK_MONOTONIC, TFD_NONBLOCK)` + `timerfd_settime(fd, 0, &new, &old)` → read 返回 8 字节溢出计数 → epoll 集成 (man 2 timerfd_create)
  - 对比: `nanosleep`(简单延时)/`setitimer`(发信号→有可重入问题)/`timerfd`(fd 模型→事件循环首选) — 现代代码用 timerfd (B1 Ch11 §1-6)

### 5. malloc 全家桶 — brk/sbrk/mmap + 分配策略
  - `malloc`: libc 实现(ptmalloc2/glibc) → 小分配(<128KB)用 brk/sbrk 扩展堆(heap arena) → 大分配(≥128KB)走 mmap(MAP_ANONYMOUS) (B1 Ch9 §1-2)
  - `sbrk(0)`: 获取当前 brk(program break)地址 → `sbrk(N)` 移动 brk → 堆区扩展/压缩 → 无法回缩空洞(mid-free 后不能降 brk) (man 2 sbrk)
  - Arena(分配域): glibc 为多线程性能创建多 arena(per-thread → 上限 8×CPU核) — 避免全局锁竞态 (B1 Ch9 §5)
  - `malloc_info(0, stdout)`: 输出 XML 格式内存统计(arena数/块大小分布/bin 使用量) → 排查内存碎片 (man 3 malloc_info)
  - `malloc_trim(0)`: 释放空闲页回内核→手动触发——`mallopt` 调参(MMAP_THRESHOLD/arena_max) (man 3 malloc_trim)

### 6. 收束
  - 信号处理的"三角冲突": 线程(独立掩码) + handler(可重入限制) + 同步(sigwait/signalfd避开限制)——现代代码用 signalfd 一劳永逸
  - timerfd 是 Linux 的"定时器 fd 化"胜利——信号 → fd → epoll → 统一事件循环，与网络 I/O 共享同一个线程
  - malloc 不是"一块铁板"——底层是 brk(小) + mmap(大) + per-thread arena(并发)三层，性能瓶颈通常在 arena 锁竞争

---

### 核心悬念
**"malloc 分配了内存——但进程的地址空间长什么样？brk 和 mmap 分别在哪？/proc/PID/maps 里每一行代表什么？"**

→ 引出 06-进程地址空间 + 伪内存泄漏 + 调度 — 从内存布局到排障案例
