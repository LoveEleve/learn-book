# RCU 宽限期 + 死锁检测(lockdep) + per-CPU + 内核抢占

> Cluster B: 7 KPs | 依赖: 07-锁家族 | 读者基线: 理解 mutex/spinlock 和睡眠互斥概念

---

### 1. RCU — 读零开销、写复制替换
  - 读无锁: `rcu_read_lock/rcu_read_unlock`(空操作, 仅关内核抢占) → 遍历链表/radix tree 不需要锁 → 读者不会阻塞写者 (include/linux/rcupdate.h:386)
  - 写复制替换: 写先复制对象 → 修改副本 → 替换指针(wmb 保证可见) → `call_rcu` 注册回调 → 等待 Grace Period → 原有读者都离开 → 释放旧数据 (kernel/rcu/tree.c:2378)
  - Grace Period 关键: 每个 CPU 经历一次上下文切换(或 quiescent state) → `rcu_gp_kthread` 检测 → `synchronize_rcu` 阻塞等 → 典型使用 nf_conntrack / 链表遍历 (kernel/rcu/tree.c:1456)
  - GP 加速: `CONFIG_RCU_BOOST`(提升 RCU reader 优先级防止被抢占) → `rcu_nocbs`(offload callback 处理到 kthread) → 避免软中断延迟

### 2. lockdep 死锁检测 — 内核内置测试
  - lockdep 原理: 跟踪每个锁的获取顺序 → 构建依赖图 → 检测潜在回路(ABBA 死锁) → 报告 "possible circular locking dependency detected" (kernel/locking/lockdep.c:3520)
  - 每种锁类(lock class)赋一个序号 → 每次锁获取记录 class1→class2 → 图中已有 class2→class1 则报回路
  - 死锁 4 条件: 互斥 + 持有等待 + 不可剥夺 + 循环等待 → 内核预防(加锁顺序) + lockdep(检测>预防)
  - 诊断: `echo t > /proc/sysrq-trigger` → dmesg 输出所有任务栈 → 分析锁持有者 → 定位死锁

### 3. 活锁 + 对比死锁
  - 活锁: 两个线程不断改变状态但都无法进展 → 死锁=停止 / 活锁=活跃但无意义 → 示例: 皮特森算法 flag[i]=true; turn=j; 两个线程同时 flag 导致来回翻转 (kernel/locking/rtmutex.c → rt_mutex_slowlock)
  - 解决方案: 随机退避 + 超时 → `cpu_relax()` → 指数回退 → 比死锁更难检测(无工具)

### 4. per-CPU + 内核抢占
  - per-CPU 变量: `DEFINE_PER_CPU(type, name)` → `get_cpu_var/put_cpu_var`(关抢占保护) → 每个 CPU 独立副本 → 无锁 → 适合统计计数器 → `this_cpu_inc`(底层用 `%gs` 偏移) (include/linux/percpu-defs.h:123)
  - 内核抢占模式: CONFIG_PREEMPT_NONE(自愿抢占) / CONFIG_PREEMPT(可抢占) / CONFIG_PREEMPT_RT(实时, 完全可抢占) → `preempt_count` 跟踪 → 自旋锁自动关抢占 → `preempt_disable/enable` (include/linux/preempt.h:184)

### 5. 收束
  - RCU = 写复制 + Grace Period + 读者零开销 = 内核最优雅的并发设计
  - lockdep 在启动时自动检测潜在死锁(静态检查编译时)
  - per-CPU 免锁 + 内核抢占 = 减少不必要的上下文切换

---

### 核心悬念
**"锁和 RCU 解决了并发的数据安全 — 但进程调度时怎么停下一个进程改运行另一个？中断来了是怎么打断正在执行的代码的？"**

→ 引出 09-中断处理(IDT/APIC) + 上半部/下半部 + softirq/tasklet/workqueue + 时间管理
