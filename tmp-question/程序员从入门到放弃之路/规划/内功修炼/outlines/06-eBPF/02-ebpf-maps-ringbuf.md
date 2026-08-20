# eBPF Maps、Ring Buffer 与调用栈 — 内核事件如何可靠到达用户态

> Cluster A: 7 KPs | 依赖: 01-ebpf-architecture | 读者基线: verifier/JIT/attach/程序类型
> 读者处境: 01 篇讲完 eBPF 程序如何运行；本篇回答程序产生的计数、状态和高频事件如何离开内核，以及如何把事件归因到调用栈
> 打开新视角: eBPF 数据面分成两类——**Map 保存可查询状态，ring/perf buffer 传递事件流**；调用栈又是把事件连接到代码路径的第三层证据

---

### 概念依赖链

```
01-ebpf-architecture(verifier/JIT/hook) → 本篇: Maps/事件管道/栈回溯
  ├─ §1 Hash/Array/PerCPU(Map 状态)
  ├─ §2 RingBuf(事件流)
  ├─ §3 Perf Event Array(BCC/兼容事件流)
  └─ §4 栈回溯(frame pointer/ORC/LBR/火焰图)
先讲: 状态容器 → 新事件流 → 兼容事件流 → 调用栈归因
后续依赖: 03-libbpf-skel-development(程序如何定义/加载这些对象)
```

### 叙事顺序

1. 问题引入——eBPF 程序知道一次 `open/read` 发生了，但如何累计计数、传递事件，并回答“是谁调用的”？（**Aha: Map 适合状态，RingBuf/Perf buffer 适合事件，栈回溯负责路径归因**）
2. Maps——Hash/Array/PerCPU 状态容器
3. RingBuf——高频事件的顺序数据管道
4. Perf Event Array——传统事件输出路径
5. 调用栈回溯——把事件连接到函数
6. 收束——状态、事件和归因三层

### 1. Maps — eBPF 程序的状态容器

场景提示: 每次系统调用都触发 eBPF 程序，计数和跨事件状态不能只放在栈上；内核和用户态如何读写这些状态？ [写作时展开]

关键设计: 不同 Map 类型对应不同并发、访问和生命周期取舍：

```[pseudocode]
HASH:
  key → value
  lookup/update/delete
  适合动态对象、聚合状态、连接跟踪

ARRAY:
  u32 index → value
  固定大小/索引访问
  适合配置、固定槽位

PERCPU_HASH/ARRAY:
  每 CPU 一份 value
  更新时减少跨 CPU 竞争
  用户态读取时聚合各 CPU 副本

Map 生命周期:
  create → lookup/update/delete/get_next_key
  → fd 引用或 bpffs pin 保持生命周期
```

Why: 为什么 PerCPU Map 不是简单“无锁 Map”？——**它减少同一 value 的跨 CPU 更新竞争，但读取时要聚合副本，内存占用也随 CPU 数量增长**；Hash/Array 的预分配、大小、LRU 和并发行为也取决于具体 Map 类型。Map 是状态存储，不等于任意指针共享内存。 [内核: Map 类型决定 helper 能力、内存布局、并发和 verifier 可接受的访问方式]

比喻锚点: Hash/Array 像不同账本：Hash 是按客户建档，Array 是固定编号柜，PerCPU 是每个营业窗口各有一本副账，最后再汇总总账。 [写作时展开]

### 2. RingBuf — 事件流的生产与消费

场景提示: tracing 程序每秒产生大量事件，逐个 syscall 或 printf 会发生什么？ [写作时展开]

关键设计: BPF ring buffer 通过 reserve/submit/discard 把事件写入环形队列，用户态通过 libbpf 消费：

```[pseudocode]
内核 BPF 程序:
  record = bpf_ringbuf_reserve(size)
  record->fields = ...
  成功:
    bpf_ringbuf_submit(record)
  异常/过滤:
    bpf_ringbuf_discard(record)

用户态 libbpf:
  ring_buffer__new(map_fd, callback)
  → ring_buffer__poll(timeout)
  → callback(event)
```

Why: 为什么 RingBuf 能减少事件传递开销，但不能保证“永不丢事件”？——**环形缓冲区有固定容量，用户态消费跟不上时会失败/丢弃或产生背压**；reserve/submit 保证单条记录不会被用户读到半截，不等于整个系统无限可靠。RingBuf 的并发、顺序和唤醒语义应以目标内核/libbpf版本为准。 [内核: RingBuf Map 由 BPF helper 写入、用户态 mmap/poll 消费]

比喻锚点: RingBuf 像传送带：生产者预留一段位置，填完后才亮“可取”标志；传送带塞满时，不能假装仍能无限放货。 [写作时展开]

### 3. Perf Event Array — 传统事件输出管道

场景提示: BCC 工具如何把内核 BPF 程序中的结构化事件送到 Python？ [写作时展开]

关键设计: Perf Event Array 借助 perf event ring buffer，BCC/libbpf 提供用户态封装：

```[pseudocode]
BPF 程序:
  bpf_perf_event_output(ctx, map, flags, data, size)

用户态:
  perf_event_open + mmap ring
  → poll/read 消费样本

BCC:
  BPF_PERF_OUTPUT(events)
  → events.perf_submit(...)
  → open_perf_buffer(callback)
  → perf_buffer_poll()
```

Why: 为什么新代码常考虑 RingBuf，而旧 BCC 工具仍大量使用 Perf Event Array？——**它们在并发、每 CPU 组织、事件顺序、兼容性和用户态库支持上有不同取舍**：不能用一句“RingBuf 全面替代 PerfBuffer”抹掉内核版本、BCC 生态和既有工具约束。选择时要看事件顺序、丢弃策略、吞吐和部署环境。 [内核: perf event buffer 与 RingBuf 都需要用户态消费，数据结构和 helper 协议不同]

比喻锚点: Perf Event Array 像每个 CPU 一个装货窗口，适合并行生产但用户端要汇总；RingBuf 像共享传送带，事件顺序和共享容量更直观。 [写作时展开]

### 4. 调用栈回溯 — 把事件归因到函数路径

场景提示: 你知道 TCP 重传发生了，但想知道是哪条内核调用路径触发的；栈回溯依赖什么？ [写作时展开]

关键设计: frame pointer、ORC、用户/内核栈和硬件分支记录是不同的回溯证据：

```[pseudocode]
frame pointer:
  rbp/架构帧指针链
  → 简单快速, 但要求编译保留帧指针

ORC:
  内核 unwind metadata
  → 不必完全依赖 frame pointer
  → 受内核构建与架构支持影响

LBR:
  记录最近分支转移
  → 不是完整任意深度调用栈
  → 依赖 CPU/PMU

栈采样:
  @[kstack] / @[ustack] 聚合
  → 符号化
  → folded stacks → FlameGraph
```

Why: 为什么没有一种栈回溯方案适合所有场景？——**准确性、开销、编译选项、内核/用户符号、架构和深度都不同**：frame pointer 会占寄存器并影响编译，ORC 依赖内核元数据，LBR 记录的是分支历史而不等于完整调用栈。火焰图宽度表示采样分布，不是精确逐次执行时间。 [内核: `/proc/kallsyms`、BTF、ORC/架构 unwind 和用户符号共同影响可读性]

比喻锚点: 栈回溯像查物流扫描记录：帧指针是每层仓库的手写交接单，ORC 是标准化路线表，LBR 是最近几次转弯记录；它们提供的路线范围不同。 [写作时展开]

### 5. 收束

eBPF 数据通道闭环：

```[pseudocode]
BPF hook 触发
  → Map 更新状态/聚合
  → RingBuf/Perf Event 输出事件
  → 用户态 callback/poll 消费
  → kstack/ustack/BTF 符号化
  → histogram/trace/flamegraph 归因
```

**Aha Moment**: "eBPF 的数据不是只有一种出口：**Map 保存可查询状态，RingBuf/Perf Event 传事件，调用栈把事件连接到路径**。选择错误的通道，可能造成高开销、丢事件或无法解释。"
**回答读者三问**: ①Map 和 RingBuf 差在哪=状态容器与事件流；②RingBuf 能否保证不丢=固定容量和消费速度仍有限；③为什么栈回溯困难=编译、架构、符号和采样证据各有边界。

---

### 核心悬念

**"数据管道已经打通；如何用 libbpf、CO-RE 和 skeleton 写一个可编译、可加载、可维护的 eBPF 程序？"**

→ 引出 03-libbpf-skel-development — libbpf、CO-RE、skeleton 与开发流程。