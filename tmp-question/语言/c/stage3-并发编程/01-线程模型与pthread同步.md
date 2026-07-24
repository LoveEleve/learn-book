# 线程模型与 pthread 同步

> **对应 Layer**：Layer 5（并发编程），参考 C-C++技术专家学习路线.md § Layer 5  
> **参考书章节**：极致C 第13-16章（并发/同步/线程/线程同步）+ 第18章（进程同步）；高效C/C++调试 第7章（竞争条件+死锁）  
> **前置依赖**：Stage1 Ch2（内存模型）——本章大量使用"栈/堆/地址空间"概念  
> **主线标准**：C11/C17（`<stdatomic.h>` 在 Ch02 展开）

---

## 1. 引言：为什么并发编程是 C 的分水岭

Java 工程师学并发时，JVM 已经提供了完整的内存模型（JMM）、`synchronized` 内置锁、`ConcurrentHashMap` 等高级工具。在 C 中学并发则是**从零开始搭建一切**——没有 JVM 帮你管锁膨胀、没有 GC 帮你回收线程间共享的数据、没有 `volatile` 做 happens-before 关系。

本章覆盖并发编程的基础层：线程模型（进程 vs 线程的选择）、pthread API（创建/同步/销毁）、5 大同步原语（mutex/condvar/spinlock/rwlock/semaphore）、以及死锁的检测与避免。原子操作和内存序放在 Ch02，内存模型（x86-TSO vs ARM weak）放在 Ch03，无锁数据结构放在 Ch04。

---

## 2. 线程 vs 进程：什么时候用哪个？

### 2.1 五维度对比

| 维度 | 进程 | 线程 |
|---|---|---|
| **地址空间** | 独立——thread A 不能直接访问 process B 的内存 | 共享——同一进程的所有线程看到相同的内存 |
| **创建开销** | 大——需要复制页表、文件描述符表（COW 缓解但仍有开销） | 小——只需分配栈空间和 TCB（Thread Control Block，线程控制块） |
| **通信方式** | IPC（管道/FIFO/共享内存/socket）——数据需要跨地址空间拷贝 | 直接读写共享变量——零拷贝 |
| **同步** | 信号量、文件锁——内核控制 | mutex/condvar/原子操作——用户态或轻量内核调用 |
| **崩溃隔离** | 强——一个进程崩溃不影响其他进程 | 弱——一个线程的 SIGSEGV 崩溃整个进程 |

### 2.2 两个经典选择：Nginx vs Memcached

**Nginx 选进程**：每个 worker 是独立的进程（`fork` 出来的）。为什么？
- HTTP 请求之间几乎没有共享状态——worker A 不需要访问 worker B 的变量
- 隔离性——一个 worker 处理异常请求崩了，不影响其他 worker
- 调试简单——可以单独 attach GDB 到一个 worker，不影响其他请求
- 多进程模型还天然支持了热升级——新 master 启动，老 master 将新的连接逐步迁移

**Memcached 选线程**：所有 worker 线程共享缓存数据。为什么？
- 缓存数据必须在 worker 之间共享——用户 A 放入缓存，用户 B 必须能读到
- 如果用多进程，每个进程维护自己的缓存 → 内存使用是线程方案的 N 倍
- 线程间共享数据零拷贝——不需要 IPC 传输缓存条目

**选择原则**：
- 共享数据多 → 线程（零拷贝、低开销）
- 隔离性要求高 → 进程（Nginx 模型、fork/exec 的天然隔离）
- 创建销毁频繁 → 线程（线程池复用，fork 开销太大）

### 2.3 线程生命周期与 NPTL

Linux 的标准线程实现叫 **NPTL**（Native POSIX Thread Library，本地 POSIX 线程库），自 glibc 2.3（2002）起默认使用。它采用 **1:1 模型**——每个用户态线程对应一个内核态任务（`task_struct`），由内核调度器直接管理。

```
用户态：pthread_create → 创建一个 pthread 对象
内核态：clone(CLONE_VM|CLONE_FS|CLONE_FILES|CLONE_SIGHAND) → 创建 task_struct
调度器：新 task_struct 和其他进程/线程一样参与 CFS 调度

#### 2.3.1 pthread_create 的底层：`clone()` 系统调用

`pthread_create` 不是"创建一个线程"——它是对 Linux `clone()` 系统调用的封装。所有 Linux 线程实际都是**共享地址空间的进程**：

```
pthread_create(&tid, NULL, worker, arg)
    ↓ glibc（NPTL）
clone(CLONE_VM | CLONE_FS | CLONE_FILES | CLONE_SIGHAND | CLONE_THREAD | CLONE_SYSVSEM | CLONE_SETTLS | CLONE_PARENT_SETTID | CLONE_CHILD_CLEARTID, ...)
    ↓ 内核
创建新的 task_struct，但与父进程共享：
  - 地址空间（CLONE_VM）           → 同一份页表 → 共享全局变量
  - 文件系统信息（CLONE_FS）        → 共享当前工作目录、umask
  - 文件描述符表（CLONE_FILES）     → 线程A打开的 fd 在线程B中可见
  - 信号处理表（CLONE_SIGHAND）     → 屏蔽某个信号对所有线程生效
```

**每个 flag 在内核中的含义**：

| flag | 如果不设置 | 设置了（线程语义） |
|---|---|---|
| `CLONE_VM` | 新进程独立的地址空间（COW 复制页表） | 共享同一份页表——`mm_struct` 引用计数+1 |
| `CLONE_FILES` | 新进程独立的 fd 表 | 共享 `files_struct`——一个线程 `close(fd)` 其他线程立即可见 |
| `CLONE_SIGHAND` | 新进程独立的信号处理表 | 共享 `sighand_struct`——`pthread_sigmask` 影响所有线程 |
| `CLONE_THREAD` | 独立的 PID | 同一个 TGID（Thread Group ID）——`getpid()` 对所有线程返回相同值 |

**关键意义**：Linux 内核中**没有"线程"这个对象**——只有"task_struct"。线程只是比普通进程多了几个共享标志位的 task_struct。这就是为什么 `/proc/PID/task/` 下每个线程都有自己的子目录——它们确实是独立的内核任务。这个设计选择让 Linux 的线程和进程使用完全相同的调度器、相同的 PID 命名空间、相同的信号机制——统一带来简洁。
```

**为什么 1:1 赢了**：2002 年之前有 N:M 模型（Ngpt）——M 个内核线程服务 N 个用户线程。N:M 的上下文切换理论上更快（用户态协作调度），但实际上：
- 一个用户线程阻塞 I/O 会导致整个内核线程被阻塞 → 同组的所有其他用户线程也被阻塞
- 内核调度器无法区分哪些用户线程有高优先级
- Linux 2.6 内核的 O(1) 调度器让 1:1 的调度开销可接受

#### 2.4.1 CFS 完全公平调度器

Linux 从 2.6.23（2007）起使用 **CFS**（Completely Fair Scheduler，完全公平调度器）。核心数据结构是一棵红黑树，按 **vruntime**（虚拟运行时间—实际 CPU 时间除以权重）排序：

```
红黑树（按 vruntime 从小到大）
      任务A(vruntime=1ms)  ← 最左边=vruntime最小=最该被调度
     /                    \
  任务B(2ms)              任务C(5ms)
```

**nice 值与权重表**：

| nice | 权重 | 相对 CPU 份额 |
|---|---|---|
| -20（最高优先级） | 88761 | ~86.7x |
| 0（默认） | 1024 | 1.0x |
| 19（最低优先级） | 15 | ~0.015x |

`vruntime` 的计算：`vruntime += delta_exec * 1024 / weight`。权重越高（nice 越低），vruntime 增长越慢 → 红黑树中靠左的位置越保持 → 更多 CPU 时间。

**为什么 CFS 选红黑树？** 不是因为它是最快的查找结构（哈希表 O(1)），而是因为：
- 需要快速找到**最左边**的节点（最小 vruntime）——红黑树最左节点 O(log n)，但 CFS 实际用 `rb_leftmost` 缓存最左节点，将 `pick_next_task` 降为 O(1)
- 需要支持**动态插入和删除**——vruntime 每次调度后重新计算，树结构需要频繁更新
- 红黑树在最坏情况下仍保持 O(log n) 的时间——没有像哈希表那样的退化情况

一次线程上下文切换的实际开销（x86_64 Linux，约 2020 年后的 CPU）：

| 操作 | 近似耗时 | 来源 |
|---|---|---|
| 保存/恢复寄存器 | ~50 ns | `swapgs` / `mov` 指令 |
| 切换地址空间（进程切换） | ~100-500 ns | 刷新 TLB（Translation Lookaside Buffer，页表缓存） |
| 调度器选择下一任务 | ~50 ns | CFS（Completely Fair Scheduler，完全公平调度器）红黑树 O(1) 查找 |
| **总计**（线程切换） | **~1-2 μs** | 寄存器 + 内核进出 + 调度 |
| **总计**（进程切换） | **~2-5 μs** | 同上 + TLB 刷新 |

**实验——验证线程创建开销**：

```bash
cat > thread_overhead.c << 'EOF'
#include <stdio.h>
#include <pthread.h>
#include <time.h>
#define N 10000

void *do_nothing(void *arg) { return NULL; }

int main() {
    struct timespec start, end;
    clock_gettime(CLOCK_MONOTONIC, &start);

    for (int i = 0; i < N; i++) {
        pthread_t tid;
        pthread_create(&tid, NULL, do_nothing, NULL);
        pthread_join(tid, NULL);
    }

    clock_gettime(CLOCK_MONOTONIC, &end);
    double elapsed = (end.tv_sec - start.tv_sec) * 1e6 +
                     (end.tv_nsec - start.tv_nsec) / 1e3;
    printf("Average create+join: %.1f μs\n", elapsed / N);
    return 0;
}
EOF
gcc -O2 -Wall thread_overhead.c -lpthread -o thread_overhead && ./thread_overhead
# 典型结果：15-30 μs 每对 create+join
```

---

## 3. pthread API 全解

### 3.1 六个核心函数

```c
#include <pthread.h>

// 创建——在新线程中执行 thread_fn，arg 为传参
int pthread_create(pthread_t *tid, const pthread_attr_t *attr,
                   void *(*thread_fn)(void *), void *arg);

// 等待——阻塞直到 tid 线程结束，回收其资源
int pthread_join(pthread_t tid, void **retval);

// 分离——不需要 join，线程结束时自动回收资源（不可 join 了）
int pthread_detach(pthread_t tid);

// 退出——当前线程终止，retval 被 pthread_join 的调用者收到
void pthread_exit(void *retval);

// 自身——返回当前线程的 pthread_t
pthread_t pthread_self(void);
```

**标准用法**——创建 + join：

```c
#include <pthread.h>
#include <stdio.h>

void *worker(void *arg) {
    int id = *(int *)arg;
    printf("Thread %d running\n", id);
    return (void *)(intptr_t)(id * 10);   // 返回计算结果
}

int main() {
    pthread_t tids[4];
    int ids[4];

    for (int i = 0; i < 4; i++) {
        ids[i] = i;
        pthread_create(&tids[i], NULL, worker, &ids[i]);
    }

    for (int i = 0; i < 4; i++) {
        void *ret;
        pthread_join(tids[i], &ret);
        printf("Thread %d returned %ld\n", i, (intptr_t)ret);
    }
    return 0;
}
```

### 3.2 线程参数传递的经典陷阱

```c
// 陷阱——所有线程共享同一个 &i（循环变量地址）
for (int i = 0; i < 4; i++) {
    pthread_create(&tids[i], NULL, worker, &i);   // 危险！所有线程的 arg 指向同一个 &i
}
// 线程1 执行 *arg 时 i 可能已经是 2——读到错误的值
```

**正确做法**：给每个线程分配独立的参数：

```c
int ids[4];   // 独立数组，每个元素地址不同
for (int i = 0; i < 4; i++) {
    ids[i] = i;
    pthread_create(&tids[i], NULL, worker, &ids[i]);   // 安全
}
```

**Java 对比**：Java 的 lambda 捕获是值拷贝（effectively final），循环变量没有这个问题——这是 C 初学者最容易踩的坑。

---

## 4. 五大同步原语

### 4.1 mutex（互斥锁）— 最基础

```c
#include <pthread.h>

pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;

void safe_increment(int *counter) {
    pthread_mutex_lock(&lock);
    (*counter)++;
    pthread_mutex_unlock(&lock);
}
```

#### 4.1.1 mutex 的 Linx 内核实现：futex 快速/慢速路径

`pthread_mutex_lock` 的性能秘密在于 **futex**（Fast Userspace muTEX，快速用户态互斥量）——一个设计精巧的混合锁：

```
pthread_mutex_lock(&lock)
    ↓
用户态快速路径（无竞争）：
    atomic_cmpxchg(&lock.__data.__lock, 0, 1)   ← 一条 CPU 指令
    如果 lock 原来是 0（无人持有）→ 设为 1 → 获取成功 ✓（~20ns）
    ↓
用户态慢速路径（有竞争）：
    atomic_xchg(&lock, 2)                        ← 标记为"有等待者"
    ↓
内核态：
    futex(FUTEX_WAIT, &lock, 2, NULL)           ← 系统调用，将线程放入等待队列
    线程在此睡眠（不消耗 CPU），等待 unlock 线程唤醒
```

**为什么这是精巧的设计**：绝大多数时候 mutex 是没有竞争的——代码路径极其短（一条原子指令）。只有在真正有竞争时，才付出系统调用的代价。这就是 futex 的核心理念：**用户态快速路径 + 内核态等待队列**。

**unlock 的对称流程**：
```
pthread_mutex_unlock(&lock)
    ↓
用户态快速路径（无等待者）：
    atomic_cmpxchg(&lock.__data.__lock, 1, 0)   ← 设为 0 → 成功（~20ns）
    ↓
内核态（有等待者）：
    futex(FUTEX_WAKE, &lock, 1)                 ← 唤醒等待队列中的一个线程
```

**性能**：无竞争时 ~20ns（一次原子 CAS），有竞争时 ~1-10μs（futex 系统调用 + 上下文切换）。

### 4.2 spinlock（自旋锁）— 短临界区专用

```c
#include <pthread.h>

pthread_spinlock_t slock;

pthread_spin_init(&slock, PTHREAD_PROCESS_PRIVATE);
pthread_spin_lock(&slock);     // 忙等——不进入内核
// 临界区——最多几十 ns 的操作
pthread_spin_unlock(&slock);
```

**何时用 spinlock 不用 mutex**：临界区短到执行时间 < mutex 的开销。典型场景：更新一个全局计数器、修改链表的头部指针。

**性能**：忙等消耗 CPU（while 循环反复检查），不睡眠。只有临界区短到几纳秒时才比 mutex 高效。

### 4.3 condvar（条件变量）— 等待条件成立

**生产者-消费者模式**：

```c
pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t  cond = PTHREAD_COND_INITIALIZER;
int buffer = -1;   // -1 = 空

void *consumer(void *arg) {
    pthread_mutex_lock(&lock);
    while (buffer == -1) {               // 🔑 为什么用 while？
        pthread_cond_wait(&cond, &lock);  // 解锁并睡眠，被唤醒时重新加锁
    }
    int val = buffer;
    buffer = -1;   // 标记为空
    pthread_mutex_unlock(&lock);
    printf("Consumer got: %d\n", val);
    return NULL;
}

void *producer(void *arg) {
    pthread_mutex_lock(&lock);
    buffer = 42;
    pthread_cond_signal(&cond);  // 唤醒一个等待的消费者
    pthread_mutex_unlock(&lock);
    return NULL;
}
```

**condvar 的三个"为什么"**：

**为什么 1：必须配合 mutex？**

如果不用 mutex，消费者检查 `buffer == -1` 和调用 `pthread_cond_wait` 之间有间隙——生产者在这个间隙里 `buffer = 42` 然后 `pthread_cond_signal`，signal 丢失。消费者进入睡眠等一个永远不会来的 signal → 死等。mutex 将这个检查和等待变为**原子的**。

**为什么 2：用 while 而不是 if？**

`pthread_cond_wait` 返回后，另一个消费者可能先拿到锁并处理了数据——buffer 又变成 -1。如果用 `if (buffer == -1)`，返回后直接读 `buffer` → 读到 -1。用 `while` 在返回后**重新检查**——这是正确的。

**为什么 3：signal 在 unlock 之前还是之后？**

两者都是正确的（POSIX 允许），但有细微的性能后果：
- signal 在 unlock 之前：被唤醒的消费者立即尝试抢锁 → 可能立即再次阻塞（因为锁还被生产者持有） → 多一次上下文切换
- signal 在 unlock 之后：被唤醒的消费者直接抢到锁 → 减少一次上下文切换

以下示例使用 signal 在 unlock 之前——这是传统的写法。

#### 4.3.1 condvar 的内核实现：futex requeue

`pthread_cond_signal` 的底层优化——**futex requeue**：

```
pthread_cond_signal(&cond)
    ↓
futex(FUTEX_REQUEUE, &cond, 1, &lock_mutex)
    ↓ 将一个等待者从 cond 的等待队列移到 lock_mutex 的等待队列
    ↓ 内核完成——不需要 wake→re-lock→sleep 的多次上下文切换
```

**为什么需要 requeue**：最简单的实现是信号→唤醒→被唤醒者抢锁→可能再次阻塞（thundering herd）。futex requeue 让内核直接把等待者从"等待条件"移动到"等待锁"——减少一半的上下文切换。

### 4.4 rwlock（读写锁）— 读多写少

```c
pthread_rwlock_t rwlock = PTHREAD_RWLOCK_INITIALIZER;

// 读者——多个读者可同时持有（共享）
pthread_rwlock_rdlock(&rwlock);
read_shared_data();
pthread_rwlock_unlock(&rwlock);

// 写者——唯一持有（排他）
pthread_rwlock_wrlock(&rwlock);
write_shared_data();
pthread_rwlock_unlock(&rwlock);
```

**注意**：读写锁有"写者饥饿"风险——如果有持续的读者，rpthread_rwlock 的默认实现可能永远不给写者锁。大多数实现提供 `pthread_rwlockattr_setkind_np` 设置写者优先。

### 4.5 semaphore（信号量）— 资源计数

```c
#include <semaphore.h>

sem_t sem;
sem_init(&sem, 0, 5);    // 0=线程间共享, 5=初始计数

sem_wait(&sem);           // 计数 -1——如果计数=0 则阻塞
// 使用资源
sem_post(&sem);           // 计数 +1——唤醒一个等待者
```

**与 mutex 的区别**：
- mutex = 二元（锁或不锁），必须同一线程 lock/unlock
- semaphore = N 元（0-N 个资源可用），任何一个线程可以 post，任何一个可以 wait

### 4.6 同步原语速查表

| 原语 | 用途 | 用户态/内核 | 无竞争开销 | 竞争开销 |
|---|---|---|---|---|
| mutex | 互斥访问共享资源 | 用户态+内核 | ~20ns | 1-10μs（futex） |
| spinlock | 互斥访问短临界区 | 纯用户态 | ~10ns | 忙等（CPU 100%） |
| condvar | 等待条件成立 | 用户态+内核 | ~30ns | 同 mutex |
| rwlock | 读多写少 | 用户态+内核 | ~30ns | 同 mutex |
| semaphore | 资源计数 | 内核 | ~50ns | 同 mutex |

**选择原则**：
- 互斥访问短临界区 → spinlock（< 100ns 临界区）
- 互斥访问长临界区或需要睡眠 → mutex
- 等待条件成立 → condvar（永远配合 mutex）
- 大多数读、偶尔写 → rwlock
- 限制并发资源数 → semaphore

---

## 5. 死锁：检测与避免

### 5.1 四必要条件

死锁的 4 个条件（必须全部成立才会死锁）：

| 条件 | 含义 | 可消除？ |
|---|---|---|
| **互斥** | 资源只有一份，只能由一个线程同时持有 | ❌ 必须满足（这就是为什么要用锁） |
| **持有并等待** | 线程持有资源 A，同时等待资源 B | ✅ 可以打破（一次性获取所有锁） |
| **不可剥夺** | 其他线程不能强行拿走已经分配的锁 | ✅ 可以打破（`pthread_mutex_timedlock` + 超时释放） |
| **循环等待** | 线程1等线程2的资源，线程2等线程1的资源 | ✅ 可以打破（固定锁顺序） |

### 5.2 避免死锁的三个方法

**方法 1：固定锁顺序**——所有线程按相同的顺序获取锁：

```c
// 错误——两个线程获取顺序不同
// 线程A: lock(L1) → lock(L2)
// 线程B: lock(L2) → lock(L1)    ← 死锁！

// 正确——所有线程先获取 L1 再获取 L2
// 线程A: lock(L1) → lock(L2)
// 线程B: lock(L1) → lock(L2)    ← 安全
```

**方法 2：一次性获取所有锁**：

```c
// 用 trylock 检测是否所有锁都可用
while (1) {
    pthread_mutex_lock(&L1);
    if (pthread_mutex_trylock(&L2) == 0) break;
    pthread_mutex_unlock(&L1);
    sched_yield();   // 短暂等待后重试
}
```

**方法 3：使用 `pthread_mutex_timedlock` 避免永久阻塞**：

```c
struct timespec ts;
clock_gettime(CLOCK_REALTIME, &ts);
ts.tv_sec += 1;   // 等 1 秒

if (pthread_mutex_timedlock(&lock, &ts) == ETIMEDOUT) {
    // 超时——不要死等！
}
```

### 5.3 死锁检测工具

**ThreadSanitizer (TSan)** — 编译时插桩，运行时检测数据竞争和死锁：

```bash
gcc -fsanitize=thread -g deadlock.c -lpthread -o deadlock_tsan
./deadlock_tsan
# 输出：WARNING: ThreadSanitizer: lock-order-inversion (potential deadlock)
#        Cycle in lock order graph: M1 => M2 => M1
```

**helgrind**（Valgrind 工具）— 也检测数据竞争，但更慢（20-50x 开销）：

```bash
gcc -g -O0 deadlock.c -lpthread -o deadlock
valgrind --tool=helgrind ./deadlock
```

---

## 6. 生产进阶：线程池、TLS 与 mutex 类型学

### 6.1 线程池—��从实验数据到生产实践

§2.4 的实验测出每次 `pthread_create` + `pthread_join` 需要 15-30 μs。如果每秒处理 10000 个任务，光是线程创建/销毁就消耗 150-300ms——这还没有计算任务本身的时间。

**线程池的实现思想**：预创建固定数量的 worker 线程，挂在一个任务队列上等待。每个任务到来时不创建新线程，而是把任务放入队列，空闲的 worker 线程取走一个任务执行。

**示意结构**（完整实现见 Ch04 无锁数据结构）：

```c
// 简化线程池的几个关键组件
pthread_t workers[NUM_THREADS];        // 预创建的线程
queue_t *task_queue;                    // 任务队列
pthread_mutex_t queue_lock;            // 保护队列的锁
pthread_cond_t  queue_cond;            // 通知 worker 有新任务

void *worker_thread(void *arg) {
    while (1) {
        pthread_mutex_lock(&queue_lock);
        while (queue_empty(task_queue)) {          // 等待任务
            pthread_cond_wait(&queue_cond, &queue_lock);
        }
        task_t *task = queue_pop(task_queue);
        pthread_mutex_unlock(&queue_lock);
        task->execute(task);                       // 执行任务—不在临界区内
    }
}
```

**生产建议**：16 核 CPU 的线程池通常设为 16-32 个线程（核数 × 1-2）。线程数多于 CPU 核数时，多余的线程不会被同时调度——它们只会增加上下文切换开销。

### 6.2 `pthread_once` — C 的一次性初始化

```c
#include <pthread.h>

static pthread_once_t init_once = PTHREAD_ONCE_INIT;
static config_t *global_config;

static void init_config(void) {
    global_config = load_config_from_file("config.json");
}

// 任何线程都可以安全调用—init_config 确保只执行一次
pthread_once(&init_once, init_config);
```

**为什么不能用 `static int initialized + mutex` 替代？** 因为检查 `initialized` 和设置 `initialized` 不在同一个原子操作中——即使在 mutex 保护下，也有"检查-加锁"的间隙。`pthread_once` 在内部用低开销的原子操作解决此问题。

### 6.3 pthread_key — C 版的 ThreadLocal

```c
#include <pthread.h>

static pthread_key_t key;

void destructor(void *value) {
    free(value);   // 线程退出时自动清理 TLS 数据
}

void init_tls(void) {
    pthread_key_create(&key, destructor);
}

void set_thread_data(void *data) {
    pthread_setspecific(key, data);
}

void *get_thread_data(void) {
    return pthread_getspecific(key);
}
```

**Java 对照**：`pthread_key` ↔ `ThreadLocal<T>`。关键区别——Java 的 `ThreadLocal` 在 GC 回收时自动清理，C 的 `pthread_key` 需要手动注册 destructor 或在线程退出前手动清理。

### 6.4 线程属性——栈大小与 detached 状态

默认线程栈为 8MB。1000 个线程 = 8GB 虚拟地址空间，在某些 32 位系统上直接超出地址空间限制。生产代码通过 `pthread_attr_t` 控制：

```c
#include <pthread.h>

pthread_attr_t attr;
pthread_attr_init(&attr);

// 设置栈大小（比如只需要 128KB）
pthread_attr_setstacksize(&attr, 128 * 1024);

// 创建 detached 线程—不需要 join，线程结束时自动回收
pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

pthread_t tid;
pthread_create(&tid, &attr, worker, NULL);
pthread_attr_destroy(&attr);
```

**生产事故案例**：某服务使用递归算法，每层递归的 C 线程用完默认 8MB 栈。堆栈溢出 → SIGSEGV → 进程崩溃。修复：`pthread_attr_setstacksize` 设为 1MB → 递归上限从 8MB 提升到 1MB/层（但线程数减少）。

**detached 线程的陷阱**：
- detached 线程无法 join → 无法获取返回值
- 如果 detached 线程访问创建函数的**栈变量**，创建函数可能在线程读取前已经返回 → 栈变量已销毁 → UAF
- detached 线程的 pthread_key 析构器在线程退出时自动调用——但如果有遗留的锁或其他全局资源，清理顺序不确定

### 6.5 signal vs broadcast

`pthread_cond_signal` 唤醒**至少一个**等待线程。`pthread_cond_broadcast` 唤醒**所有**等待线程。

**何时用 broadcast**：
- 多个消费者等待同一个生产者——signal 只唤醒一个，其余七个继续等——饥饿
- 状态变化需要所有等待者重新评估条件——如 shutdown 信号

**何时用 signal**：
- 单个消费者/单个任务——signal 就够了
- 生产者-消费者是一对一关系——signal 足够了

**原则**：不确定用 signal 还是 broadcast → 用 broadcast（最安全、性能可承受）。

### 6.6 mutex 类型学——三种特殊 mutex

| 类型 | 行为 | 用途 |
|---|---|---|
| `PTHREAD_MUTEX_NORMAL` | 默认——重复 lock 同一线程 = 死锁，unlock 未持有的锁 = UB | 一般互斥 |
| `PTHREAD_MUTEX_ERRORCHECK` | 重复 lock → 立即返回 `EDEADLK`（不阻塞） | **开发阶段**——快速发现 double-lock bug |
| `PTHREAD_MUTEX_RECURSIVE` | 同一线程可重复 lock（内部计数器），需匹配相同次数的 unlock | 递归函数、可重入库函数 |

```c
// 设置 mutex 类型
pthread_mutexattr_t attr;
pthread_mutexattr_init(&attr);
pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_ERRORCHECK);

pthread_mutex_t lock;
pthread_mutex_init(&lock, &attr);   // 动态初始化——必须使用 pthread_mutex_init
pthread_mutexattr_destroy(&attr);

// 注意：PTHREAD_MUTEX_INITIALIZER 只适用于静态初始化
// 动态分配的结构体中的 mutex 必须用 pthread_mutex_init
```

---

## 7. 总结

| 概念 | 一句话 |
|---|---|
| 线程 vs 进程 | 共享地址空间=线程（数据共享零拷贝），隔离=进程（Nginx 模型） |
| NPTL 1:1 | 每个用户线程对应一个内核 task_struct — 简单可靠，2002 起默认 |
| pthread API | create→join/detach→exit — joinable 或 detached 必须选一个 |
| mutex | 互斥锁——无竞争 ~20ns，竞争时 futex 陷入内核 |
| spinlock | 忙等——短临界区+高争用专用，不要睡眠或 I/O |
| condvar | 等待条件成立——必须配合 mutex、用 while 不用 if |
| rwlock | 读多写少——可能有写者饥饿 |
| semaphore | 资源计数——N 元互斥 |
| 死锁 4 条件 | 互斥/持有并等待/不可剥夺/循环等待 |
| 死锁避免 | 固定锁顺序 + trylock 重试 + timedlock 超时 |
| 线程池 | 预创建 worker 线程 + 任务队列 + condvar 通知 — 避免每请求 create/join |
| `pthread_once` | C 的一次性初始化——比 `static int + mutex` 更安全高效 |
| `pthread_key`（TLS） | C 版 ThreadLocal——每线程独立数据，需手动 destructor |
| 线程属性 | `pthread_attr_setstacksize` 控制栈大小——生产代码避免 8MB 默认浪费 |
| signal vs broadcast | signal=唤醒一个，broadcast=唤醒全部——不确定时用 broadcast |
| ERRORCHECK/RECURSIVE | 调试用 double-lock 检测/递归可重入——不同场景不同 mutex 类型 |

**核心观点**：并发编程不是"知道了 API 就会了"——每一行多线程代码都是一份**隐含的假设**：假设这两个线程不会同时访问这个变量、假设这个锁按预期顺序获取、假设 `pthread_cond_signal` 不会在检查条件前丢失。技术专家不是"能运行"，而是"知道每一行的隐含假设是什么，以及对这个假设能给出验证"。

---

## 验收要点

1. 线程和进程的 5 维度区别是什么？Nginx 为什么选进程，Memcached 为什么选线程？
2. pthread_create 的经典参数传递陷阱是什么？Java 为什么没有这个问题？
3. mutex、spinlock、rwlock、semaphore 各适用于什么场景？各自的无竞争开销是多少？
4. condvar 的三个"为什么"（必须配合 mutex / 用 while / signal 时机）是什么？
5. 死锁的 4 个必要条件是什么？如果用"固定锁顺序"打破了哪个条件？
6. 线程池解决了什么问题？§2.4 的实验数据如何证明线程池的必要性？
7. `pthread_once` 解决什么问题？为什么不能用 `static int initialized + mutex` 替代？
8. `pthread_key`（TLS）与 `ThreadLocal<T>` 的对应关系是什么？C 版本有什么额外风险？
9. `PTHREAD_MUTEX_INITIALIZER` 有什么限制？动态分配的 mutex 应该如何初始化？
10. signal vs broadcast 的区别？什么情况下用 broadcast？
