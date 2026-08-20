# Pthreads 线程编程 — 生命周期、同步、TLS 与线程安全边界

> Cluster B: 8 KPs | 依赖: 01-file-io-basics、02-mmap-epoll-inotify、03-fork-exec-daemon | 读者基线: fork/exec、fd、地址空间、多线程概念
> 读者处境: 03 篇讲了进程复制和替换；本篇回答多个线程如何共享同一地址空间、怎样同步共享状态、线程结束如何回收，以及线程安全为什么不等于信号安全
> 打开新视角: 线程轻在共享资源，危险也在共享资源；真正要区分**线程生命周期、共享状态、同步协议、TLS、信号上下文**

---

### 概念依赖链

```
01-02 I/O/mmap + 03 fork/exec → 本篇: Pthreads
  ├─ §1 create/join/detach(线程生命周期)
  ├─ §2 mutex/条件与死锁(共享状态同步)
  ├─ §3 TLS(共享地址空间中的私有状态)
  ├─ §4 可重入/线程安全/信号安全(安全边界)
  └─ §5 1:1/N:1/M:N(执行模型)
先讲: 生命周期 → 锁 → TLS → 安全语义 → 映射模型
后续依赖: 05-signals-timers-malloc(进程/线程信号与异步上下文)
```

### 叙事顺序

1. 问题引入——多个线程共享 heap 和 fd，怎样避免同时修改同一状态？线程结束后又由谁回收？
2. pthread_create/join/detach——生命周期
3. mutex/条件变量/死锁——共享状态协议
4. TLS——共享进程中的私有副本
5. 可重入、线程安全与信号安全
6. 1:1/N:1/M:N——用户线程与内核调度
7. 收束——共享与隔离

### 1. pthread_create、join、detach — 线程生命周期

场景提示: 创建一个 worker 后，主线程如何知道它结束、取回结果或让资源自动回收？ [写作时展开]

关键设计: Pthreads 用 create 启动线程，join 等待并回收，detach 把回收责任交给运行时：

```[pseudocode]
pthread_create(&tid, attr, start_routine, arg)
  → 新线程开始执行 start_routine(arg)
  → 返回错误码(不是统一依赖 errno)

pthread_join(tid, &retval)
  → 等目标线程结束
  → 取得返回值/确认资源回收

pthread_detach(tid)
  → 标记为 detached
  → 结束后自动回收线程资源
  → 不能再 join
```

Why: 为什么“线程不 join”不能简单叫线程僵尸？——**joinable 线程结束后会保留等待回收所需的资源，但语义与进程 zombie 不完全相同**；如果永远不 join/detach，确实会积累资源。线程共享进程的地址空间和打开文件对象，但各自有栈、寄存器和线程 ID；多个线程共享 fd offset 时仍需同步。 [man 3 pthread_create/join/detach: 返回值、joinable/detached 和生命周期]

比喻锚点: joinable 线程像完成工作后把报告放在前台等领取，detach 线程像提交报告后由系统自动归档；两者不能重复领取。 [写作时展开]

### 2. mutex、条件变量与死锁 — 保护的是协议，不只是代码片段

场景提示: 两个线程同时更新余额，一个线程拿 m1 再拿 m2，另一个反过来，为什么可能永久卡住？ [写作时展开]

关键设计: mutex 保护临界区，条件变量等待状态变化，死锁通常来自不一致的锁顺序：

```[pseudocode]
pthread_mutex_lock(&m)
  → 检查/修改共享状态
pthread_mutex_unlock(&m)

condition variable:
  lock(m)
  while (!condition):
    pthread_cond_wait(&cv, &m)
  使用状态
  unlock(m)

典型死锁:
  A: lock(m1) → lock(m2)
  B: lock(m2) → lock(m1)
  → 循环等待

预防:
  全局锁顺序/缩小临界区/超时或 trylock
```

Why: 为什么条件变量必须配合 `while` 重新检查条件，而不是 `if`？——**可能有虚假唤醒，也可能被其他线程先消费条件**；唤醒只是“重新检查机会”。`trylock` 和 timedlock 能避免永久等待，但不自动解决死锁设计。mutex type、递归锁和 error-checking 行为要按实现/属性配置核对。 [man 3 pthread_mutex_lock/cond_wait: 锁、错误码、虚假唤醒与条件协议]

比喻锚点: mutex 是仓库钥匙，条件变量是“货到了”的通知；被叫醒后仍要重新查看库存，不能只听到铃就开始装货。 [写作时展开]

### 3. 线程局部存储 — 共享地址空间中的私有状态

场景提示: 多线程都使用 `errno` 或连接上下文，怎样做到每个线程看到自己的副本？ [写作时展开]

关键设计: TLS 让变量逻辑上属于每个线程，而不需要给每次函数调用显式传参：

```[pseudocode]
静态 TLS:
  __thread / _Thread_local variable
  → 编译器/链接器安排 TLS 存储

动态 TLS:
  key = pthread_key_create(destructor)
  pthread_setspecific(key, ptr)
  ptr = pthread_getspecific(key)
  线程退出时可调用 destructor

重要区别:
  TLS 解决“每线程私有状态”
  不等于共享对象内部自动线程安全
```

Why: 为什么 TLS 能减少锁，却不能解决所有并发问题？——**每线程副本消除了某些共享写竞争，但跨线程汇总、生命周期、内存占用和线程池复用仍需设计**；TLS key 数量、析构次数和动态库模型也由实现限制，不能把某个常数当作跨平台契约。 [内核: Linux TLS 常通过线程架构状态和 libc/runtime 管理，具体 ABI 依赖架构]

比喻锚点: TLS 是每个工人自己的工作本，不必抢公共草稿；但最后汇总所有工人的结果时，仍需要共享账本和同步。 [写作时展开]

### 4. 可重入、线程安全与信号安全 — 三个不同问题

场景提示: 一个函数在多线程中安全，为什么放进 SIGTERM handler 仍可能死锁？ [写作时展开]

关键设计: 三个术语关注的并发上下文不同：

```[pseudocode]
可重入:
  同一执行流被打断后再次进入
  → 不依赖不安全的共享静态状态/调用链

线程安全:
  多线程并发调用不会产生未处理的数据竞态
  → 可能靠锁、原子、TLS 或不可变状态

async-signal-safe:
  信号处理器上下文允许调用的有限函数集合
  → write/_exit 等
  → printf/malloc/pthread_mutex_lock 通常不可直接调用
```

Why: 为什么“malloc 线程安全”不代表“信号处理器里可以 malloc”？——**信号可能打断正持有 libc 内部锁的线程，handler 再进入同一锁就会死锁**；signal handler 应尽量设置标志、写入 async-signal-safe fd，或使用 signalfd/专用线程把复杂处理移出异步上下文。 [man 7 signal-safety: async-signal-safe 函数边界；[内核: 信号投递与线程屏蔽共同决定实际处理线程]

比喻锚点: 线程安全像多人排队使用工具，信号安全像有人在机器急停瞬间插手；急停场景允许的动作少得多，不能照搬普通工作流程。 [写作时展开]

### 5. 1:1、N:1、M:N — 用户线程与内核调度如何映射

场景提示: Linux 多线程为什么能利用多核，而某些用户态协程却不能在一个线程内自动并行？ [写作时展开]

关键设计: 用户线程和内核可调度实体的映射决定并行、阻塞和调度成本：

```[pseudocode]
1:1:
  一个用户线程对应一个内核可调度任务
  → 可多核并行
  → 内核承担调度/栈/阻塞管理

N:1:
  多个用户线程复用一个内核线程
  → 用户态切换便宜
  → 一个阻塞系统调用可能阻塞整个承载线程
  → 不能跨核并行

M:N:
  M 用户执行单元映射到 N 内核线程
  → 可在并行与用户调度间折中
  → runtime/调度器复杂度更高
```

Why: 为什么“线程比进程轻”不是无成本？——**线程共享地址空间减少资源复制，却增加共享状态、锁、缓存一致性和故障隔离复杂度**；1:1、协程和 M:N 的选择取决于阻塞 I/O、CPU 并行、调度需求和 runtime。Linux 内核仍会看到可调度任务，但线程组/共享资源属性与普通独立进程不同。 [内核: clone flags/线程组/调度实体共同决定共享与调度语义]

比喻锚点: 1:1 是每个工人有自己的工作台和调度号，N:1 是多个工人轮流用一张工作台，M:N 是多个工作台配多个工人；效率和管理成本不同。 [写作时展开]

### 6. 收束

线程编程闭环：

```[pseudocode]
create
  → 共享地址空间/fd + 私有栈/TLS
  → mutex/cond/atomic 协调共享状态
  → join/detach 回收生命周期
  → signal handler 遵守 async-signal-safe
  → 选择 1:1/runtime 映射与并发模型
```

**Aha Moment**: "线程的性能来自共享，线程的 bug 也来自共享；TLS、锁、条件变量和信号安全分别解决不同问题，不能用一个 mutex 或一个“线程安全”标签覆盖全部上下文。"
**回答读者三问**: ①不 join 会怎样=joinable 资源无法被正常回收；②TLS 解决什么=每线程私有状态；③线程安全为什么不等于信号安全=信号可在任意异步点打断并重入不安全调用链。

---

### 核心悬念

**"线程能共享状态，但信号是异步投递的；`sigaction` 的处理器到底属于进程还是线程，哪个线程收到信号，定时器又如何参与调度？"**

→ 引出 05-signals-timers-malloc — 信号、定时器与内存分配的异步安全边界。