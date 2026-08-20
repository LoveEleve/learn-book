# CFS 调度器 — vruntime + 红黑树 + 负载均衡 + EEVDF

> Cluster C: 9 KPs | 依赖: 09-中断处理 | 读者基线: 理解进程挂起和恢复的基本概念
> 读者处境: 已读完 09 篇，知道时钟中断每 4ms 打断一次 CPU；本篇回答"被打断后，下一个运行谁、凭什么"
> 打开新视角: 公平的数学定义（vruntime）、红黑树如何做 O(log N) 选取、I/O 进程为何隐式优先、多核负载怎么搬、容器 CPU 限制的底层

---

### 概念依赖链

```
09-中断(时钟中断) → 本篇: CFS 调度器
  ├─ §1 vruntime 公平模型(公平的量化 — 依赖 09 的时钟 tick)
  │    └─ §2 调度周期与粒度(时间片边界 — 依赖 §1 vruntime)
  │         └─ §3 调度器演进(O(N)→O(1)→CFS→EEVDF — 对照 §1 的模型变迁)
  │              └─ §4 负载均衡(多核任务迁移 — 依赖 §1 的 load 概念)
  │                   └─ §5 CGroup CPU + 实时调度(限制与优先级 — 依赖 §1 weight)
先讲: 公平模型 → 时间边界 → 演进脉络 → 多核均衡 → 限制与实时
后续依赖: 11-进程(task_struct 含 sched_entity)、16-容器(cgroup cpu 限制)
```

### 叙事顺序

1. 问题引入——两个进程抢 CPU：一个写代码调试（交互型）、一个跑编译（CPU 密集）——怎么分才"公平"？（**Aha: 公平不是"每人一半"，是"按权重比例分时间"**）
   - 过渡: 公平怎么量化？——vruntime
2. vruntime 公平模型——实际运行时间×1024/weight；红黑树取最左
   - 过渡: 每次调度多久？太短频繁切换、太长不响应
3. 调度周期与粒度——sched_latency 6ms / min_granularity 0.75ms；唤醒 vruntime 防抢占
   - 过渡: CFS 是进化产物——之前的调度器什么样？
4. 调度器演进——O(N) 遍历 → O(1) 位图 → CFS 红黑树 → EEVDF deadline
   - 过渡: 单核选好了——多核怎么办？
5. 负载均衡——调度域/组；NEWIDLE/WAKE/FORK 触发；NUMA 跨 node 代价
   - 过渡: 进程怎么被限制/提权？——cgroup 与实时调度
6. CGroup CPU + 实时调度——shares/period/quota；SCHED_FIFO/RR/DEADLINE
   - 过渡: 调度全貌已齐——回到"谁在跑"的完整图景
7. 收束——CFS 本质 + 经典策略对照

### 1. CFS 核心 — vruntime 驱动的公平调度

场景提示: 一个 CPU 密集 + 一个交互型进程——每人一半时间，交互型还是卡；按权重分才能兼顾。 [写作时展开]

关键设计: CFS（Completely Fair Scheduler）把"公平"量化为 **vruntime（虚拟运行时间）**：

```[pseudocode]
vruntime 增长速率 = 实际运行时间 × 1024 / weight
nice 0 = weight 1024 → nice -1 = 1277 → nice -20 = 88761
高权重（低 nice）→ vruntime 增长慢 → 排在红黑树左边 → 获得更多 CPU
```

- `struct sched_entity { u64 vruntime; u64 exec_start; u64 sum_exec_runtime; struct load_weight load; }`——每个进程/线程的调度实体
- **红黑树选取**: `cfs_rq` 的 `rb_root_cached tasks_timeline` 按 vruntime 排序 → `pick_next_task_fair` 取最左节点（最小 vruntime）→ O(log N)
- **I/O 密集隐式优先**: I/O 进程睡眠多 → 运行时短 → vruntime 增长少 → 自然排左边——**不需要显式标记优先级**

Why: 为什么用 vruntime 而非"优先级数字"？——优先级是静态的（写死的数字），vruntime 是动态的（反映实际使用）；公平的本质是"运行少的优先"。权重（weight）只影响 vruntime 的**增长速率**——高权重进程"走得慢"，自然轮到更多次。这把"公平"从口号变成可比较的数值。

比喻锚点: vruntime=跑步比赛记分牌——每个人跑的时间记在牌上（vruntime），跑得少的排前面（红黑树最左）；重的人（高权重）记分涨得慢（×1024/weight），自然多跑几圈（更多 CPU）。 [写作时展开]

### 2. 调度周期与粒度

场景提示: 100 个进程在跑——每个都公平地"轮一圈"要多久？切太快切换开销爆炸。 [写作时展开]

关键设计: 时间片边界由两个参数控制：

| 参数 | 默认 | 作用 |
|------|------|------|
| sysctl_sched_latency | 6ms | 调度周期——每个可运行进程在周期内至少运行一次 |
| sched_min_granularity | 0.75ms | 最小粒度——进程过多时分片太小，防止频繁切换 |

- 进程数 ≤ sched_nr_latency 时：时间片 = sched_latency / nr_running（每人 6ms/N）
- 进程数超限：时间片 = sched_min_granularity（0.75ms 兜底，牺牲公平保吞吐）
- **唤醒防抢占**: `try_to_wake_up` 新唤醒进程 vruntime = max(own, min_vruntime − sched_latency)——防止新进程"出生即插队"连续抢占；且唤醒抢占受 **`sched_wakeup_granularity` 阈值**控制——唤醒者与被唤醒者 vruntime 差距超过阈值才允许抢占（避免频繁切换） [内核: min_vruntime 是红黑树基准值——新进程从基准附近起步, 防止新/老进程 vruntime 差距过大]

Why: 为什么时间片不能太长也不能太短？——太长：交互型进程等待太久（不响应）；太短：切换开销（~µs）占时间片比例过大（吞吐崩）。sched_latency/min_granularity 是"响应性 vs 吞吐"的调节旋钮。

比喻锚点: 时间片=食堂窗口打饭——每人 6 秒（sched_latency/N），人多时每人 0.75 秒（min_granularity）保证都吃上；窗口换人（切换）要时间，太频繁全耗在换人上了。 [写作时展开]

### 3. 调度器演进 — O(N) → O(1) → CFS → EEVDF

场景提示: 内核 30 年调度器改了 4 代——每一代解决了上一代的什么痛点？ [写作时展开]

关键设计:

| 代 | 版本 | 机制 | 痛点 |
|------|------|------|------|
| O(N) | 2.4 | 每次调度遍历所有进程选最大 goodness | 进程多时 O(N) 慢 |
| O(1) | 2.6 早期 | 优先级数组位图 + active/expired 双队列 | 复杂度高、公平性差（惩罚交互型） |
| CFS | 2.6.23+ | 红黑树按 vruntime 排序 | O(log N) + 理论误差 ~1% |
| EEVDF | 6.6+ | 最早虚拟截止时间优先（deadline = vruntime + slice） | 大页/长进程 vruntime 滞后问题 |

Why: 为什么演进方向是"从优先级到虚拟时间"？——O(N)/O(1) 以**静态优先级**为核心（交互型被惩罚）；CFS 用**虚拟时间**重定义公平（谁用得少谁优先）；EEVDF 进一步给每个进程一个虚拟 deadline——从"按优先级排队"到"按公平度排队"到"按截止时间排队"，精度逐代提升。

比喻锚点: 调度演进=排队规则进化——O(N) 是"挨个问谁有急事"（遍历）、O(1) 是"按 VIP 等级分组"（优先级位图）、CFS 是"谁等得久谁先"（vruntime）、EEVDF 是"谁 deadline 近谁先"（截止时间）。 [写作时展开]

### 4. 负载均衡 — CPU 间的任务迁移

场景提示: 8 核机器，7 核空转、1 核跑满——调度器怎么"搬"任务过去？ [写作时展开]

关键设计: load_balance 周期检查 CPU 负载 → 按**调度域（sched_domain）→ 调度组（sched_group）**逐级找最忙 CPU → `can_migrate_task`（检查 cache affinity）→ pull task：

- **触发时机**: SD_BALANCE_NEWIDLE（某 CPU 空闲）/ SD_BALANCE_WAKE（任务唤醒）/ SD_BALANCE_FORK（新进程）→ `active_load_balance` 热迁移
- **NUMA 域**: SD_NUMA——跨 node 迁移代价高（内存访问变慢）→ **宁可本地 CPU 不对齐也不跨 node**；migration 内核线程执行实际迁移

Why: 为什么均衡不是"平均分任务"？——迁移有代价：缓存冷启动（任务换了核，L1/L2 全失效）+ NUMA 远端内存访问。所以均衡是"粗粒度"的：空闲 CPU 拉任务（NEWIDLE）、但跨 NUMA node 宁可牺牲均衡保局部性——**均衡目标不是负载平均，是吞吐最优**。

比喻锚点: 负载均衡=搬家公司——哪屋空了就往哪搬（NEWIDLE）；但搬到隔壁小区（跨 NUMA node）要过路费（远端内存访问），除非空得太多，否则不搬。 [写作时展开]

### 5. CGroup CPU 控制 + 实时调度

场景提示: 容器 `--cpus=2` 怎么实现？为什么实时任务（音视频）需要特殊调度？ [写作时展开]

关键设计:

**CGroup CPU 限制**（容器 CPU 限额的底层）:
- v1: `cpu.shares`（权重，空闲时按比例）/ `cpu.cfs_period_us`（周期 100ms）/ `cpu.cfs_quota_us`（配额）——`--cpus=2` = 200000/100000
- v2: `cpu.max "$MAX $PERIOD"` 统一

**实时调度**:
- SCHED_FIFO: 固定优先级不抢占（同优先级先到先跑）
- SCHED_RR: 同优先级轮转
- SCHED_DEADLINE: 最早截止时间，每周期预算
- 工具: `sched_setattr` / `chrt -r PID`

Why: 为什么 CFS 之外还要实时调度类？——CFS 保证"公平"但不保证"及时"（vruntime 小的才先跑）；实时任务（音视频/控制）要求**确定性的响应时间**——SCHED_FIFO 让实时进程永远优先于普通进程（CFS 只在实时类空闲时才运行）。这是"公平"与"确定性"的取舍。 [内核: SCHED_DEADLINE 用 CBS(常量带宽服务器)算法保证每周期预算内确定性]

比喻锚点: CGroup=食堂限时用餐（这个食堂（容器）每天限量供应 CPU 时间）；实时调度=急诊病人（实时任务）来了先看，普通门诊（CFS）排队等。 [写作时展开]

### 6. 收束

回到"两个进程怎么分 CPU"：
- vruntime = 公平的量化（运行少的优先）
- 周期/粒度 = 响应 vs 吞吐的旋钮
- 演进 = 从优先级排队到虚拟时间排队
- 负载均衡 = 吞吐最优而非负载平均
- CGroup/实时 = 限制与确定性的两种扩展

经典策略对照: FCFS（先来先服务，护航效应）/ SJF（最短作业优先，需预知时长）/ RR（时间片轮转，交互性好）——CFS 是加权公平的 RR 改进版。

**Aha Moment**: "公平不是'每人一半'，是'按权重比例、运行少的优先'——vruntime 让公平可计算。而调度器 30 年的演进就是一句话：从'按优先级排队'到'按虚拟时间排队'。"
**回答读者三问**: ①交互型为何不卡=vruntime 隐式优先；②`--cpus=2` 怎么实现=cfs_quota/period；③多核不均衡怎么办=load_balance 调度域迁移。

---

### 核心悬念

**"进程被红黑树调度跑起来了 — 但每个进程长什么样？task_struct 里存了什么？fork 怎么创建多个进程？ELF 怎么加载程序？"**

→ 引出 11-PCB(task_struct) + fork/clone + 上下文切换 + 系统调用 + 信号处理 + ELF——调度器选了"谁"，这篇讲"谁"到底是什么。
