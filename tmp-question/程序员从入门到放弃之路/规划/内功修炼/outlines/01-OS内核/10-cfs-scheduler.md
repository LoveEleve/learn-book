# CFS 调度器 — vruntime + 红黑树 + 负载均衡 + EEVDF

> Cluster C: 9 KPs | 依赖: 09-中断处理 | 读者基线: 理解进程挂起和恢复的基本概念

---

### 1. CFS 核心 — vruntime 驱动的公平调度
  - `struct sched_entity { u64 vruntime; u64 exec_start; u64 sum_exec_runtime; struct load_weight load; }` — 每个进程/线程的调度实体 (include/linux/sched.h:659)
  - vruntime 公式: 实际运行时间 × 1024 / weight → nice 0 = weight 1024 → nice -1 = 1277 → nice -20 = 88761 → 高优先级 vruntime 增长慢(获得更多 CPU) (kernel/sched/fair.c:683)
  - 红黑树: `struct cfs_rq { struct rb_root_cached tasks_timeline; u64 min_vruntime; struct load_weight load; }` → 按 vruntime 排序 → `pick_next_task_fair` 取最左节点(最小 vruntime) → O(log N) (kernel/sched/fair.c:4751)
  - I/O 密集型隐式优先: I/O 进程睡眠多 → 运行时短 → vruntime 增长少 → 自然排在红黑树左边 → 不需显式标记优先级

### 2. 调度周期与粒度
  - 调度周期: `sysctl_sched_latency`(默认 6ms) → 保证每个可运行进程在周期内至少运行一次 (kernel/sched/fair.c:670)
  - 最小粒度: `sched_min_granularity`(默认 0.75ms) → 防止过多切换(进程过多时分片太小) → `nr_running > sched_nr_latency` 时使用
  - 唤醒: `try_to_wake_up` → 新进程 vruntime = max(own, min_vruntime - sched_latency) → 防止新进程立即抢占所有 CPU

### 3. 调度器演进 — O(N) → O(1) → CFS → EEVDF
  - O(N)(2.4): 每次调度遍历所有进程 → 选最大 goodness → 简单但 O(N)
  - O(1)(2.6 早期): 优先级数组位图 + active/expired 两组队列 → 位图查最高优先级 → O(1)
  - CFS(2.6.23+): 红黑树按 vruntime 排序 → O(log N) + 公平性保证(理论误差在 1%)
  - EEVDF(6.6+): 最早虚拟截至时间优先 → 每个进程有 deadline(vruntime + slice) → 选最早 deadline → 比 CFS 更精确(避免大页分配后 vruntime 滞后) (kernel/sched/fair.c:4879)

### 4. 负载均衡 — CPU 间的任务迁移
  - load_balance: 定期检查 CPU 负载 → 调度域(sched_domain) → 调度组(sched_group) → 找最忙 CPU → `can_migrate_task`(检查 cache affinity) → pull task (kernel/sched/fair.c:11563)
  - 触发: SD_BALANCE_NEWIDLE(某 CPU 空闲) / SD_BALANCE_WAKE(任务唤醒) / SD_BALANCE_FORK(新进程) → `active_load_balance` 热迁移
  - NUMA 域: `SD_NUMA` → 跨 node 迁移代价高 → 宁可本地 CPU 不对齐也不跨 node → migration 内核线程执行实际迁移 (kernel/sched/fair.c:12135)

### 5. CGroup CPU 控制 + 实时调度
  - cgroup v1: `cpu.shares`(权重) / `cpu.cfs_period_us`(周期, 默认 100ms) / `cpu.cfs_quota_us`(配额) → 容器 CPU 限制底层 → v2: `cpu.max "$MAX $PERIOD"` (kernel/sched/core.c:7456)
  - 实时调度: SCHED_FIFO(固定优先级不抢占) / SCHED_RR(同优先级轮转) / SCHED_DEADLINE(最早截止时间, 每周期预算) → `sched_setattr` → `chrt -r PID` (kernel/sched/rt.c:1736)
  - MLFQ 基础: 多级反馈队列 = 多优先级队列 + 时间片随级递增 + 定期提升机制防饥饿

### 6. 收束
  - CFS 本质是 vruntime 驱动的优先级队列 → 红黑树按 vruntime 选取下一个进程 → I/O 密集隐式优先
  - 经典调度策略对比: FCFS(First Come First Serve, 无抢占, 护航效应) / SJF(Shortest Job First, 最小平均等待, 需预知运行时间) / RR(Round Robin, 固定时间片, 交互性高) — CFS 是加权公平的改进版 RR
  - 负载均衡按调度域换 CPU 迁移 → NUMA 域跨 node 代价高
  - EEVDF 在 6.6+ 替代 CFS — deadline 驱逐比 vruntime 更精确

---

### 核心悬念
**"进程被红黑树调度跑起来了 — 但每个进程长什么样？task_struct 里存了什么？fork 怎么创建多个进程？ELF 怎么加载程序？"**

→ 引出 11-PCB(task_struct) + fork/clone + 上下文切换 + 系统调用 + 信号处理 + ELF
