# 信号、定时器与 malloc — 异步事件、事件循环和内存分配边界

> Cluster B: 8 KPs | 依赖: 01-02 + 03 fork/exec + 04 pthread | 读者基线: kill、SIGTERM、线程与 fd
> 读者处境: 04 篇已经说明线程安全不等于信号安全；本篇把信号递送、定时器事件和 libc 分配器放在同一条系统编程边界上
> 打开新视角: 现代 Linux 程序常把异步事件“同步化”——**pthread_sigmask 阻塞、sigwait/signalfd 消费、timerfd 入 epoll**；malloc 则把地址空间、线程并发和内存回收连接起来

---

### 概念依赖链

```
01-02 I/O/mmap + 03 fork/exec + 04 pthread → 本篇: 信号/定时器/malloc
  ├─ §1 sigaction/掩码/信号分类
  ├─ §2 进程信号与线程信号
  ├─ §3 async-signal-safe 与同步化处理
  ├─ §4 clock/timerfd(时间事件 fd 化)
  └─ §5 malloc/brk/mmap/arena(分配器边界)
先讲: 信号机制 → 线程递送 → 安全处理 → 定时事件 → 内存分配
后续依赖: 06-memory-malloc-debug(地址空间、泄漏与伪泄漏)
```

### 叙事顺序

1. 问题引入——服务收到 SIGTERM 时既要停止接收、刷新数据，又不能在 handler 里调用 mutex/printf/malloc，怎么办？
2. sigaction 与掩码——信号如何定义和阻塞
3. 线程递送与同步化——sigwait/signalfd
4. async-signal-safe——为什么 handler 只能做极少事情
5. clock/timerfd——定时器如何加入 epoll
6. malloc/brk/mmap/arena——分配器如何向内核要内存
7. 收束——异步事件与分配器边界

### 1. sigaction 与信号掩码 — 信号不是普通函数调用

场景提示: `SIGTERM` 到达时，进程正在多个线程中运行；系统怎样决定使用默认动作、忽略，还是调用 handler？ [写作时展开]

关键设计: sigaction 定义处理动作，signal mask 决定当前线程暂时阻塞哪些信号：

```[pseudocode]
struct sigaction sa
  sa_handler = handler
  或 sa_sigaction = handler_with_siginfo + SA_SIGINFO
  sa_mask = handler 执行期间额外阻塞集合
  sa_flags = SA_RESTART/SA_SIGINFO 等

sigaction(SIGTERM, &sa, NULL)

pthread_sigmask(SIG_BLOCK, &set, &old)
  → 当前线程阻塞集合变化
  → 信号未必丢失, 解除阻塞后可能递送
```

Why: 为什么阻塞信号不等于忽略信号？——**阻塞只是暂缓递送，信号的默认动作/handler 语义仍然存在**；标准信号可能合并，实时信号通常具有排队和 payload 语义，实际限制需按 man page 和信号类型理解。 [man 2 sigaction/sigprocmask、man 7 signal: 动作、mask、pending 与实时信号]

比喻锚点: 阻塞像把通知放进待处理文件夹，忽略像直接把通知丢进废纸篓；解除阻塞后文件夹里的通知仍可能送达。 [写作时展开]

### 2. 信号与线程 — 处理器通常进程共享，mask 却是线程私有

场景提示: `kill(pid, SIGTERM)` 发给进程后，究竟由哪个线程处理？`pthread_kill` 又有什么不同？ [写作时展开]

关键设计: 信号动作通常属于进程级 disposition，线程各自有 signal mask；发送方式决定目标范围：

```[pseudocode]
kill(process_pid, SIGTERM)
  → 进程定向信号
  → 由满足递送条件的线程处理

pthread_kill(thread, SIGUSR1)
  → pthread API 定向特定线程

tgkill(tgid, tid, SIGUSR1)
  → Linux 级别精确指定进程/线程

pthread_sigmask
  → 每线程独立阻塞/解除阻塞集合
```

Why: 为什么生产服务常让所有工作线程阻塞 SIGTERM，只让一个信号线程处理？——**这样把异步递送集中到可控的同步点，避免任意线程被 handler 打断**；但线程创建、继承 mask、同步等待和关闭顺序要统一设计。 [man 3 pthread_sigmask/pthread_kill、man 2 tgkill: 进程/线程目标与 mask 语义]

比喻锚点: 进程信号像发到公司总邮箱，线程 mask 像每个员工的过滤规则；专用信号线程则是指定的值班员统一处理。 [写作时展开]

### 3. async-signal-safe — handler 中只能做很少的事

场景提示: 为什么 SIGTERM handler 里 `printf`、`malloc` 或 `pthread_mutex_lock` 可能让进程死锁？ [写作时展开]

关键设计: 异步信号可能在任意指令点打断线程，handler 只能调用 POSIX 明确允许的 async-signal-safe 函数：

```[pseudocode]
不安全示例:
  handler → printf
  handler → malloc/free
  handler → pthread_mutex_lock
  可能重入 libc/线程锁, 造成死锁或未定义行为

较安全模式:
  volatile sig_atomic_t stop = 1
  或 write(pipe_fd, &byte, 1)
  或 signalfd/sigwait 把处理移到普通线程上下文

主循环:
  看到 stop/event
  → 正常加锁、刷新、释放资源、退出
```

Why: 为什么 malloc 线程安全却不等于信号安全？——**信号可能打断一个正持有 malloc 内部锁的线程，handler 再进入 malloc 就无法前进**；即使某次运行没死锁，也不构成安全保证。`volatile sig_atomic_t` 只适合简单标志，不能替代完整同步协议。 [man 7 signal-safety: async-signal-safe 函数清单和 handler 限制]

比喻锚点: 信号 handler 像火警时的紧急出口，只能按报警、开门和引导，不能在出口里重新装修楼梯。 [写作时展开]

### 4. clock_gettime 与 timerfd — 把时间事件接入 epoll

场景提示: 服务既要处理 socket，又要处理超时/周期任务；为什么 timerfd 比在信号 handler 里处理定时器更容易组合？ [写作时展开]

关键设计: 单调时钟用于 elapsed time，timerfd 把过期事件变成可读 fd：

```[pseudocode]
clock_gettime(CLOCK_MONOTONIC, &ts)
  → 不受墙钟跳变直接影响

CLOCK_REALTIME
  → 人类时间/日历时间
  → 可能受校时影响

fd = timerfd_create(CLOCK_MONOTONIC, TFD_NONBLOCK|TFD_CLOEXEC)
timerfd_settime(fd, 0, &new_value, NULL)

epoll_ctl(epfd, ADD, fd, EPOLLIN)
read(fd, &expirations, sizeof(expirations))
  → 读取累计过期次数
```

Why: 为什么 timerfd 适合事件循环，但不等于高精度硬实时定时器？——**实际唤醒仍受调度、系统负载、时钟分辨率和 timer slack 影响**；读取 8 字节计数能发现过期合并，但不能让错过的每个周期自动补执行。 [man 2 clock_gettime/timerfd_create/timerfd_settime: 时钟、过期和非阻塞语义]

比喻锚点: timerfd 像把闹钟接到值班台，闹铃响时变成一封可读通知；值班台统一处理，不需要每个闹钟单独打断业务线程。 [写作时展开]

### 5. malloc、brk、mmap 与 arena — libc 分配器的多层实现

场景提示: `malloc(64)` 和 `malloc(1GB)` 为什么不一定走同一条内核路径？多线程 malloc 又怎样避免单个全局锁？ [写作时展开]

关键设计: malloc 是 libc 分配器接口，底层可能通过 brk/mmap 等机制取得区域，具体阈值和 arena 策略会随实现/版本/调参变化：

```[pseudocode]
malloc(size)
  → libc 分配器的线程缓存/arena/bin
  → 现有空闲块可复用?
      是: 用户态返回
      否: 向内核申请/扩展区域
          brk/sbrk 或 mmap 等路径

多线程:
  多 arena/per-thread cache 减少共享锁
  → 代价是碎片/内存保留/跨线程释放复杂度

观测:
  malloc_info / mallinfo 类接口
  malloc_trim / mallopt 等调优接口
  → 具体字段与策略按 libc 文档核对
```

Why: 为什么不能写死“<128KB 用 brk、≥128KB 用 mmap”或“arena 上限等于 8×CPU”？——**glibc 版本、架构、动态阈值、配置和分配历史都会改变选择**；arena 也不是每个线程永远一一对应。`sbrk` 是遗留接口，现代程序更应依赖 malloc/mmap 的明确用途和实现文档。 [man 3 malloc/mallopt/malloc_info、man 2 brk/mmap: 分配器与内核映射边界]

比喻锚点: malloc 像仓库管理员：小件优先从已有货架取，大件可能开新仓区；多线程时增加多个仓库能减少排队，却可能留下更多闲置库存。 [写作时展开]

### 6. 收束

异步事件与分配器边界：

```[pseudocode]
线程/进程信号
  → mask/sigwait/signalfd
  → 普通线程上下文安全处理

timerfd
  → 时间过期 → epoll 可读事件

malloc
  → 用户态缓存/arena/bin
  → brk/mmap 等内核资源
  → 释放、trim、回收受实现与工作集影响
```

**Aha Moment**: "系统编程的危险边界集中在两个地方：**异步信号可能在任何时刻重入代码，malloc 又可能隐藏锁、缓存和地址空间行为**。把信号同步化、把定时器 fd 化，才能让主循环用普通线程语义处理复杂清理。"
**回答读者三问**: ①信号来了哪个线程处理=进程信号由未屏蔽线程处理，定向信号可指定线程；②timerfd 为什么适合 epoll=过期计数变成 fd 事件；③malloc 为什么不是简单 syscall=libc 先在用户态分配，必要时再通过多种内核路径扩展。

---

### 核心悬念

**"malloc 分配了内存，但进程的地址空间到底长什么样？brk、mmap、栈、共享库和线程 TLS 如何出现在 `/proc/PID/maps` 里？"**

→ 引出 06-memory-malloc-debug — 地址空间、内存泄漏、伪泄漏与调度排障。