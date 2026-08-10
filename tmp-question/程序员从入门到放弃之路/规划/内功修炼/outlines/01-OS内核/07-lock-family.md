# 锁家族 — mutex + qspinlock + rwlock + semaphore + futex + seqlock

> Cluster B: 10 KPs | 依赖: 06-原子操作与内存屏障 | 读者基线: 理解 CAS 自旋和 barrier 概念

---

### 1. mutex — 互斥锁，睡眠等待不空转
  - `struct mutex { atomic_long_t owner; spinlock_t wait_lock; struct list_head wait_list; }` — owner 存持有者 task_struct 指针(低位 4bit 存标记) (include/linux/mutex.h:53)
  - 锁争用路径: `mutex_lock → __mutex_lock_slowpath` → 设置 `TASK_UNINTERRUPTIBLE` → 加入 wait_list → schedule() 放弃 CPU → 唤醒后重试 (kernel/locking/mutex.c:649)
  - optimistic spinning: 锁持有时间短时不自旋 → OSQ 排队(MCS 锁) → 减少缓存行争用(多个等待者不在同一缓存行上自旋) (kernel/locking/mutex.c:362)
  - PI 优先级继承: 高优先任务等低优先级持锁 → 临时提升持有者优先级(rt_mutex) → 避免优先级反转(低优先抢占高优先) (kernel/locking/rtmutex.c:219)

### 2. spinlock — 自旋忙等，临界区极短
  - ticket lock(老): 两个变量 current_ticket + next_ticket → `xadd` 原子获取排队号 → 自旋到 `&lock->current_ticket == ticket` → FIFO 公平 → 缓存争用严重(每核自旋读同一缓存行) (arch/x86/include/asm/spinlock.h:101)
  - qspinlock(新, 4 字节): MCS 队列锁排队 → per-CPU 等待节点 → 自旋在本地缓存行(非全局) → 减少缓存 ping-pong → 嵌入其他结构(4 字节) (kernel/locking/qspinlock.c:289)
  - 不可递归: 自旋锁不支持同一 CPU 重复加锁 → 无 owner 跟踪 → `spin_lock(&lock); spin_lock(&lock);` → 死锁(卡住)

### 3. rwlock + seqlock — 读者多写者少场景
  - rwlock: `rwlock_t` → `read_lock/read_unlock`(读并发) / `write_lock/write_unlock`(写排他) → 读者优先可能导致写者饥饿 (include/linux/rwlock.h:45)
  - seqlock: 写不阻塞读 → 读记 sequence → 读完检查 sequence 是否变化(奇数=写进行中) → 变化则重读 → 写极少读极多的场景(如 jiffies) — `write_seqlock(&lock); write_sequnlock(&lock);` (include/linux/seqlock.h:318)
  - 区别: rwlock 读等写 / seqlock 读不等写(以重读为代价) → rwlock 适合读多写少 / seqlock 适合写极少的轻量数据

### 4. semaphore + futex — 计数型 + 用户态快路径
  - semaphore 计数型锁: `sem_init(sem, 0, N)` → `down`(P, 获取) / `up`(V, 释放) → 当 count<0 时阻塞 → `down_interruptible`(可唤醒) / `down_killable`(可杀) (include/linux/semaphore.h:15)
  - futex 快慢路径: 用户态无竞争 → 纯原子 CAS(无系统调用) / 有竞争 → `futex(FUTEX_WAIT)` 陷入内核 → 等待者加入 `futex_q`(plist 优先级) → 释放者 `futex(FUTEX_WAKE)` (kernel/futex/waitwake.c:87)
  - 互斥锁是 N=1 的信号量 → Java LockSupport.park() 基于 futex → pthread_mutex 基于 futex
  - 引用计数 kref: `struct kref { refcount_t refcount; }` → `kref_init / kref_get / kref_put` → `kref_put` 到 0 时调用 release 回调释放对象 → 比 `atomic_dec_and_test` 安全(防止 use-after-free 的 race) (include/linux/kref.h:47)

### 5. 收束
  - mutex = 睡眠等(适合临界区长) / spinlock = 忙等(适合临界区极短) / qspinlock 解决缓存争用
  - rwlock 读并行/写串行, seqlock 读不等写但需重试, futex 用户态快+内核忙路径 = 三层设计
  - 锁的选择取决于临界区长度、写比例、睡眠可行性

---

### 核心悬念
**"mutex 要睡眠等，spinlock 不做跟踪 — 那 RCU 怎么做到读完全无锁(零开销)？死锁怎么在内核中检测？"**

→ 引出 08-RCU 宽限期 + 死锁检测(lockdep) + 活锁 + per-CPU + 内核抢占
