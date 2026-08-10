# Pthreads 线程编程 — 创建/同步/死锁/可重入 + 线程局部存储 + 线程安全 vs 信号安全

> Cluster B: 8 KPs | 依赖: A (01-02) + B (03 fork) | 读者基线: 理解 fork 和进程模型，知道多线程概念但没写过 pthread

---

### 1. pthread_create/join/detach — 线程的三种生命周期
  - `pthread_create(&tid, NULL, func, arg)`: Linux 上 pthread 是 1:1 映射——每个线程 = 一个内核 task_struct → NPTL(Native POSIX Thread Library) (man 3 pthread_create)
  - `pthread_join(tid, &retval)`: 等 thread 结束取返回值 → 类似 waitpid → **不 join = 线程僵尸**(类似进程，资源不释放) (man 3 pthread_join)
  - `pthread_detach(tid)`: 线程分离——结束自动回收，不能再 join → 适合"fire-and-forget"场景 (man 3	pthread_detach)
  - 线程 vs 进程: 线程共享地址空间(全局变量/heap/mmap)、fd 表、信号处理——但各自有栈和寄存器 (B1 Ch7 §1-3)
  - `pthread_self()`: 获取自身 TID; `pthread_equal(t1, t2)`: 不直接用==比较(实现可能是 struct) (man 3 pthread_self)

### 2. 互斥锁 — mutex + 死锁检测
  - `pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER` → `pthread_mutex_lock(&m)` → `pthread_mutex_unlock(&m)` (man 3 pthread_mutex_lock)
  - 互斥锁类型: `PTHREAD_MUTEX_NORMAL`(不检死锁/不递归)/`ERRORCHECK`(递归 lock 返回 EDEADLK)/`RECURSIVE`(同一线程可递归 lock)/`DEFAULT`(行为未定义) (man 3 pthread_mutexattr_settype)
  - 典型死锁: A: lock(m1)→lock(m2) vs B: lock(m2)→lock(m1) → 互锁 → `pthread_mutex_trylock` 非阻塞尝试+退避重试 (B1 Ch7 §5-6)
  - `pthread_mutex_timedlock`: 超时获取——设定 deadline 避免永久阻塞 (man 3 pthread_mutex_timedlock)
  - 排查工具: `strace -f -e trace=futex` 看 futex 调用 → `gdb + thread apply all bt` → ThreadSanitizer(TSan) 地址消毒器

### 3. 线程局部存储 — __thread + pthread_key
  - `__thread int errno;`: GCC/Clang 扩展——每个线程独家副本 → 编译器在 TLS(Thread Local Storage)段分配 → 等价 `errno` 就是 `__thread` 变量 (B1 Ch7 §4)
  - `pthread_key_create(&key, destructor)` → `pthread_setspecific(key, ptr)` → `pthread_getspecific(key)`: 动态 TLS——数量有限(PTHREAD_KEYS_MAX=1024) (man 3 pthread_key_create)
  - 内核实现: TLS 通过 `arch_prctl(ARCH_SET_FS, addr)` 设线程的 `%fs` 段寄存器 → 所有 `__thread` 变量通过 `%fs:offset` 访问 → 每条线程有独立 `%fs`

### 4. 可重入函数 + 线程安全 vs 信号安全
  - 可重入(reentrant): 函数可在中断后再进入——不打全局/静态变量、不调不可重入函数、不分配不加锁——`strtok`不可重入(static 缓冲区)，`strtok_r` 可重入 (B1 Ch7 §5)
  - 线程安全(thread-safe): 多线程调同一函数不出数据竞态——加互斥锁或用 per-thread 数据——`malloc`线程安全(libc 内用 arena 分区) (B1 Ch7 §5)
  - 信号安全(async-signal-safe): 信号处理器中能安全调用的函数——`write`/`_exit`/`sem_post`(有限的约 120 个)——`printf` 不安全(内部 malloc) (man 7 signal-safety)
  - **线程安全 ≠ 信号安全**: malloc 线程安全但信号不安全(拿锁时信号打断→死锁) (B1 Ch10 §4)

### 5. 线程模型对比 — 1:1 / N:1 / M:N
  - NPTL(1:1): 每条用户线程=1条内核线程——真实并行(多核)、调度在 kernel → Linux 从 2.6 起默认 (B1 Ch7 §1)
  - GNU Pth(N:1): N 条用户线程复用 1 条内核线程——无真实并行、任一阻塞全局阻塞、调度在用户态——历史方案
  - M:N(混合): M 条用户线程映射到 N 条内核线程——Go runtime M:N——理论最优但实现复杂 (B1 Ch7 §1)

### 6. 收束
  - pthread 1:1 模型每条线程都是完整 task_struct——内核调度器看不出来"线程"和"进程"的区别(CGROUP 权重一样)
  - `__thread` 变量的本质是 `%fs` 偏移——两条线程写同一个 `__thread` 变量永远不会踩到对方，因为访问不同 `%fs` 基址
  - 线程安全 vs 信号安全的混淆是生产级 bug 的热土——`printf` 信号不安全但 99% 的信号处理器都在用

---

### 核心悬念
**"线程用锁保护共享数据没问题——但如果线程要响应信号怎么办？`sigaction` 是 per-process 还是 per-thread？信号来了哪个线程处理？"**

→ 引出 05-信号 + 定时器 + malloc — 信号处理全链路与内存分配
