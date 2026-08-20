# CPU 与内存可观测 — 从 exec/run queue 到 OOM、缓存和回收路径

> Cluster C: 4 KPs | 依赖: 01-ebpf-architecture、04-bpftrace、05-tracing-sources | 读者基线: USE、bpftrace、tracepoint、Map
> 读者处境: 05 篇建立了追踪源选择；本篇把它们落到现成诊断工具：CPU 跑满、线程排队、内存压力、泄漏和缓存命中分别怎样观察？
> 打开新视角: 工具不是“症状快捷键”，每个工具都只观察一段生命周期；要把 **exec、run queue、on/off-CPU、fault、回收、OOM、缓存** 拼成时间线

---

### 概念依赖链

```
01 架构 + 04 bpftrace + 05 追踪源 → 本篇: CPU/内存 eBPF 观测工具
  ├─ §1 CPU 事件矩阵(exec/runqlat/cpudist/profile/syscount)
  ├─ §2 CPU 等待(off-CPU/IRQ/wakeup)
  ├─ §3 OOM/fault/memleak(内存事件)
  ├─ §4 cachestat/cachetop/vmscan(缓存/回收)
  ├─ §5 文件/磁盘工具(opensnoop/filetop/biolatency)
  └─ §6 工具选择与证据拼接
先讲: CPU → 内存 → 文件/磁盘 → 调度 → 综合诊断
后续依赖: 07-network-security-xdp(网络观测与安全数据面)
```

### 叙事顺序

1. 问题引入——CPU 使用率 100% 或内存告警出现时，怎样从“哪个指标高”推进到“哪个进程、哪个路径造成”？
2. CPU 五类工具——创建、排队、执行、采样、系统调用
3. off-CPU/中断/唤醒——CPU 不忙但延迟高
4. OOM/缺页/泄漏——内存生命周期
5. cache/reclaim——缓存效果与回收压力
6. 文件/块 I/O——从文件到设备
7. 综合工具链与边界

### 1. CPU 工具矩阵 — execsnoop、runqlat、cpudist、profile、syscount

场景提示: 服务突然变慢，先判断是异常进程启动、CPU 排队、某个调用栈热点，还是系统调用风暴。 [写作时展开]

关键设计: 每个工具观察 CPU 生命周期的不同切面：

```[pseudocode]
execsnoop:
  exec 事件 → 谁启动了什么程序

runqlat:
  runnable → 真正获得 CPU 的时间
  → 调度等待分布

cpudist:
  线程连续/累计运行片段分布
  → 辅助判断 CPU 计算与调度形态

profile:
  定时采样调用栈
  → on-CPU 热点

syscount:
  系统调用次数/类型
  → 用户态与内核边界的行为画像
```

Why: 为什么不能把 `runqlat` 高直接等同于“CPU 不够”，或把 `cpudist` 短片直接等同于“I/O bound”？——**调度、抢占、优先级、锁、cgroup、IRQ 和采样窗口都会影响形态**；工具提供线索，仍需结合 CPU 饱和、off-CPU、应用指标和 workload。 [内核: 工具依赖 sched/exec/syscall tracepoint，事件字段和可用性以目标内核为准]

比喻锚点: 五个工具像机场监控：execsnoop 看谁进场，runqlat 看谁等跑道，cpudist 看飞行片段，profile 看飞机正在飞哪条航线，syscount 看哪个柜台最忙。 [写作时展开]

### 2. Off-CPU、中断与唤醒 — “CPU 不高”也可能很慢

场景提示: 请求耗时上升，但进程 CPU 使用率很低；它可能在等 I/O、锁、网络、定时器还是其他线程？ [写作时展开]

关键设计: 把线程离开 CPU 的原因和重新运行的路径分开观察：

```[pseudocode]
offcputime:
  线程离开 CPU 的时间
  → 按阻塞调用栈聚合
  → 找 sleep/lock/I/O 等等待来源

hardirqs/softirqs:
  中断与软中断处理量/时间
  → 检查网卡、定时器、存储 IRQ 是否挤占 CPU

wakeup/调度跟踪:
  观察线程被唤醒、变 runnable、再次运行的时间线
  → 关联唤醒者与等待者(具体工具语义需核对)

综合:
  runqlat = 想运行但排队
  off-CPU = 已离开 CPU 在等待什么
  wakeup timeline = 谁何时让它重新可运行
```

Why: 为什么 off-CPU 不能简单叫“CPU 瓶颈”？——**它描述的是不在 CPU 上的时间，可能是正常 sleep，也可能是锁、I/O 或下游慢**；中断/softirq 也可能抢占应用，但要看 CPU 分布和事件时间线。工具名称和参数不能跨发行版机械照抄。 [内核: sched_switch、sched_wakeup、IRQ/softirq tracepoint 提供不同时间边界]

比喻锚点: on-CPU 看工人正在干什么，off-CPU 看工人放下工具后在等材料、等电梯还是等主管；唤醒链说明谁通知他回来。 [写作时展开]

### 3. OOM、fault 与 memleak — 从内存压力到分配生命周期

场景提示: RSS 不断上升、系统出现 OOM，但如何区分应用泄漏、缓存增长、cgroup 限制和正常页缓存？ [写作时展开]

关键设计: 内存工具观察的是不同事件：

```[pseudocode]
oomkill:
  OOM 事件 → 谁被杀/所在 cgroup/当时压力
  → 不是泄漏证明

faults:
  minor/major page fault 计数与调用路径
  → 区分已在内存中建立映射与需要磁盘/回收参与

memleak:
  追踪 malloc/free 或内核分配释放
  → 保留未匹配分配栈
  → 需要覆盖全部分配路径与合理生命周期窗口

mmapsnoop/映射事件:
  谁创建/打开/映射了哪些文件或区域
```

Why: 为什么 OOM 时被杀的进程不一定是“泄漏者”？——**OOM killer 根据内存策略、cgroup、oom_score 和当前状态选择对象，泄漏可能来自另一个进程或更早的时间窗口**；memleak 工具也通常只覆盖它挂到的分配器和采样范围，不能代替完整 heap profiler。 [内核: OOM、memcg、page fault 和 slab 分属不同内存生命周期事件]

比喻锚点: OOM 是水库溃坝，oomkill 是被迫关闭的一座工厂；要找漏水点，还要检查管道、缓存池和各工厂的进水记录。 [写作时展开]

### 4. cachestat、cachetop、vmscan — 缓存命中不等于越高越好

场景提示: 页缓存命中率下降时，应该增加内存、调整访问模式，还是其实只是一次性扫描污染缓存？ [写作时展开]

关键设计: 缓存工具和回收工具需要结合工作集与访问模式：

```[pseudocode]
cachestat:
  观察页缓存命中/未命中、dirty 等趋势

cachetop:
  按进程/文件观察缓存访问热点

vmscan:
  观察 kswapd/direct reclaim、回收与扫描活动

综合:
  命中低 + 工作集反复访问 → 缓存容量/局部性问题
  命中低 + 一次性顺序扫描 → 未必值得扩大缓存
  reclaim/PSI 高 → 结合 memcg、swap、匿名页和文件页
```

Why: 为什么“缓存命中率越高越好”是错误目标？——**缓存服务的是工作集和端到端延迟，一次性数据、缓存污染、内存压力和预取都会改变最佳策略**；cachestat 是趋势证据，不直接给出“应增加多少内存”的答案。 [内核: LRU、vmscan、page cache 和 PSI 共同决定回收压力]

比喻锚点: 图书馆借阅命中率高不一定代表馆藏合理；如果大家只借一本书，命中率很高但扩大整座图书馆也未必有价值。 [写作时展开]

### 5. 文件与块 I/O — 把“内存慢”追到具体文件和设备

场景提示: CPU iowait 上升，如何找出哪个进程读了哪个文件，最后对应到哪个 block I/O 延迟？ [写作时展开]

关键设计: 从文件系统事件到块层请求逐层下钻：

```[pseudocode]
opensnoop:
  open/openat → 谁访问了哪些路径

filetop:
  按进程/文件聚合读写次数/字节

ext4slower/xfsslower:
  文件系统操作延迟与类型

biotop:
  哪个进程产生块 I/O

biosnoop:
  单次 block request 的设备/大小/排队/完成时间

biolatency:
  设备请求延迟分布/P99
```

Why: 为什么“发现一个热点文件”还不能证明它是根因？——**缓存、预读、文件系统、块层队列、设备和应用请求模式可能在不同层放大延迟**；要把路径、文件操作和 block request 用时间窗口对齐，不能只看排行。 [内核: VFS/page cache、filesystem tracepoint、block tracepoint 是不同观测边界]

比喻锚点: 先看谁打开仓库、再看谁搬货、再看哪条传送带堵、最后看设备哪个请求慢；每一层都可能是新的排队点。 [写作时展开]

### 6. 收束

CPU/内存观测的证据链：

```[pseudocode]
USE/RED 异常
  → exec/run queue/profile/syscall
  → on/off-CPU/IRQ/wakeup
  → fault/reclaim/OOM/leak
  → cache/file/block I/O
  → 用时间线和 workload 交叉验证
```

**Aha Moment**: "eBPF 工具不是一组命令别名，而是一组生命周期观察窗口：**谁产生、谁排队、谁执行、谁等待、谁分配、谁回收、谁做 I/O**。只有把窗口拼起来，才接近根因。"
**回答读者三问**: ①CPU 不高但慢看什么=off-CPU、runqlat、wakeup 和 I/O；②OOM 是否等于泄漏=不是，要区分策略、cgroup、缓存和分配生命周期；③缓存命中低怎么办=先看工作集与回收压力，再决定布局/内存/访问模式。

---

### 核心悬念

**"CPU/内存路径已经能观测；网络 DDoS、连接建立、重传和丢包又如何在 XDP、TC、socket 和 LSM eBPF 层快速定位与阻断？"**

→ 引出 07-network-security-xdp — 网络观测、安全策略与 XDP。