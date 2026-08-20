# 子系统观测 — 内存、磁盘、网络和调度如何从指标下钻到压力路径

> Cluster C: 6 KPs | 依赖: 01-methodology-foundation | 读者基线: USE、`vmstat`、`iostat`、`ss`
> 读者处境: 01 篇用 USE 找到了“哪类资源可能有问题”；本篇把每类资源的观测工具串起来，回答“压力是容量不足、排队、错误，还是应用消费不及时”
> 打开新视角: 子系统观测不能只看利用率，而要同时看**资源状态、排队/压力、错误和分布**，最后再把聚合指标交给 tracing 工具验证路径

---

### 概念依赖链

```
01 USE/RED → 本篇: 子系统深度观测
  ├─ §1 内存(分配/压力/进程地址/NUMA)
  ├─ §2 磁盘(IOPS/带宽/队列/延迟分布)
  ├─ §3 网络(队列/重传/缓冲/硬件错误)
  └─ §4 调度(运行队列/上下文切换/中断亲和性)
先讲: 内存 → 磁盘 → 网络 → 调度
后续依赖: 06-ftrace-bpf-tracing(从聚合计数器跟踪到代码路径)
```

### 叙事顺序

1. 问题引入——`vmstat/iostat/ss` 告诉你“某个子系统异常”，怎样判断谁在排队、谁在丢包、谁在回收？
2. 内存——从系统压力到进程/NUMA
3. 磁盘——从吞吐到延迟分布
4. 网络——从连接队列到网卡硬件
5. 调度——从 run queue 到中断亲和性
6. 收束——指标定位后进入 tracing

### 1. 内存观测 — 物理压力、进程地址与 NUMA 要分层看

场景提示: 主机 free 内存很少，应用却不一定 OOM；另一个主机 free 很多，应用却因 cgroup/PSI 变慢，为什么？ [写作时展开]

关键设计: 内存观测至少分系统、内核对象、进程地址、压力和 NUMA 五层：

```[pseudocode]
系统/回收:
  vmstat 1 → free/swap-in/swap-out/reclaim 相关趋势
  /proc/meminfo → Available/Committed/Swap 等账本

内核对象:
  slabtop → dentry/inode/网络等 slab 消耗

进程地址:
  pmap -x PID / smaps_rollup → RSS/PSS/私有/共享映射

压力:
  /proc/pressure/memory → some/full 的 stall 时间

NUMA:
  numastat -p PID → 本地/远端访问与分配
```

Why: 为什么不能看到 `si/so` 非零就断定系统“已经严重 swap”，或看到 `free` 少就断定内存不足？——**swap、page cache、cgroup 限制、回收压力和工作集变化需要结合多个账本解释**；PSI 关注任务因资源压力停滞的时间，补充了容量指标看不到的等待。`drop_caches` 会改变实验状态，不能当作日常“清内存”按钮。 [内核: PSI、memcg、LRU 回收与 slab 是不同压力面；[man 5 proc: `/proc/meminfo` 字段语义需结合内核版本]

比喻锚点: 内存观测像查仓库：库存少不一定缺货，可能只是缓存货架占用；真正重要的是有多少订单在等货、哪些区域被挤压。 [写作时展开]

### 2. 磁盘 I/O — IOPS、带宽、队列和延迟分布必须一起看

场景提示: 磁盘 `%util` 只有 60%，用户 P99 却从 10ms 升到 1s；只看一个利用率为什么会漏掉问题？ [写作时展开]

关键设计: 存储性能至少要同时观察请求量、吞吐、队列和延迟分布：

```[pseudocode]
iostat -xz 1
  → r/s,w/s: 请求率
  → rMB/s,wMB/s: 吞吐
  → aqu-sz: 平均队列
  → await/r_await/w_await: 请求延迟
  → %util: 设备忙碌/服务时间的聚合视角

延迟分布:
  biolatency/bpftrace block tracepoint
  → P50/P95/P99

实验:
  fio 选择明确的 rw/bs/iodepth/direct/sync
  → IOPS 与带宽结果必须写清工作负载
```

Why: 为什么 `%util>80%`、`await>20ms` 不能当成跨设备通用报警阈值？——**SSD/HDD、RAID、队列深度、请求大小、设备并行度和文件系统都会改变基线**；`%util` 还可能在多队列/虚拟设备/镜像层被误读。P99 才能揭示少量长尾，但也要检查样本量和采集边界。 [内核: block layer tracepoint 能把设备请求生命周期与上层 I/O 对齐]

比喻锚点: 磁盘像收费站：吞吐是每小时通过车辆数，IOPS 是车次数，队列是等待车辆，await 是单辆车从进入到通过的时间；只看收费站是否开门不够。 [写作时展开]

### 3. 网络观测 — 队列、重传、缓冲与硬件错误

场景提示: TCP 接口慢，究竟是 accept 队列、rwnd/cwnd、路径丢包，还是网卡 CRC 错误？ [写作时展开]

关键设计: 网络要同时看 socket 状态、TCP 信息、协议统计和网卡计数：

```[pseudocode]
ss -lnt / ss -ti
  → listen Recv-Q/Send-Q
  → rtt/cwnd/retrans/rwnd 等连接状态

sar -n TCP,ETCP / nstat
  → 重传、失败、连接与内核累计统计

ethtool -S eth0
  → 设备/驱动相关 rx_missed、CRC、队列与 offload 计数

tcpdump
  → Seq/Ack、SACK、RTO、RST 与时间线

iperf3/fio-like network workload
  → 在明确并发、方向、MTU、拥塞控制下验证吞吐
```

Why: 为什么 `retrans/s>0` 不能直接等同于“物理链路丢包”？——**重传可能来自拥塞、乱序、ACK 延迟、应用路径、虚拟设备或网络设备丢弃**；网卡计数器的名字也由驱动决定。`ss` 适合连接级状态，`ethtool -S` 适合设备级线索，抓包负责把两者放到时间线上。 [内核: 05/06 篇的 socket、qdisc、NAPI、邻居队列分别对应不同网络排队点]

比喻锚点: 网络观测像查物流：`ss` 看单个订单，协议统计看全仓趋势，网卡计数看装卸设备，抓包看每一站时间戳。 [写作时展开]

### 4. 调度与中断 — CPU 慢可能是等不到运行机会

场景提示: 进程 CPU 使用率不高，延迟却很大；它可能不是算得慢，而是长时间没有获得 CPU。 [写作时展开]

关键设计: 调度观测需要区分运行队列、上下文切换、线程等待和 IRQ/软中断分布：

```[pseudocode]
perf sched / run queue 指标
  → 线程等待 CPU 的时间与唤醒延迟

pidstat -w / perf stat
  → voluntary context switch: 主动等待 I/O/锁
  → involuntary context switch: 被抢占/时间片结束

/proc/interrupts / irq affinity / irqbalance
  → 网卡/存储 IRQ 是否集中到少数 CPU

CPU affinity/NUMA:
  → 观察绑核是否改善局部性
  → 避免把 IRQ、应用和 ksoftirqd 互相挤压
```

Why: 为什么“上下文切换超过 1000/s 就一定有问题”是错误的？——**阈值依赖进程数、工作负载、CPU 频率、I/O 和调度策略**；上下文切换可能是正常 I/O 并发，也可能是锁竞争或线程风暴。`chrt`、CPU isolation 等调优会改变系统公平性，必须配合回滚方案和全局观测。 [内核: 调度延迟、IRQ、softirq 和应用线程共享 CPU，绑核是资源重新分配而不是免费性能]

比喻锚点: 调度像机场跑道分配：发动机没坏不代表飞机立刻能起飞，可能是在等跑道、登机口或优先级调度。 [写作时展开]

### 5. 收束

子系统观测闭环：

```[pseudocode]
USE/RED 发现异常
  → 内存: 账本 + PSI + 进程/NUMA
  → 磁盘: IOPS + 带宽 + 队列 + 延迟分布
  → 网络: socket + TCP + 网卡 + 抓包
  → 调度: run queue + switch + IRQ/affinity
  → 仍不能解释? 进入 ftrace/BPF tracing
```

**Aha Moment**: "观测工具不是一张命令清单，而是每个子系统的一套证据层：**资源状态、排队/压力、错误、分布和时间线**。聚合指标找到异常，tracing 才能回答它经过了哪条路径。"
**回答读者三问**: ①内存 free 少是不是缺内存=结合 Available、PSI、memcg、回收和工作集；②磁盘 `%util` 低为何仍慢=看队列和 P99；③网络重传是否一定是网线坏=结合 socket、协议、设备计数和抓包。

---

### 核心悬念

**"vmstat/iostat/ss 只能告诉你资源或队列异常；如何跟踪一次分配、一次 block I/O、一个网络包到底经过了哪些函数？"**

→ 引出 06-ftrace-bpf-tracing — ftrace、tracepoint、BPF 与路径级跟踪。