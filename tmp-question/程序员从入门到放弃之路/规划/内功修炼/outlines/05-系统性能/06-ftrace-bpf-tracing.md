# Ftrace、kprobe 与 eBPF — 从聚合计数器跟踪到具体代码路径

> Cluster C: 6 KPs | 依赖: 05-memory-disk-network-observability | 读者基线: perf、内核子系统与 tracepoint
> 读者处境: 05 篇能告诉你内存、磁盘、网络哪个子系统异常；本篇继续回答：一次慢 I/O、重传或调度延迟，究竟经过了哪些函数、参数和调用路径？
> 打开新视角: tracing 工具按侵入性与灵活性形成梯度——**ftrace 看函数路径，kprobe/uprobe 动态插桩，BCC/bpftrace 按事件聚合并输出证据**

---

### 概念依赖链

```
05 子系统观测(perf/计数器) → 本篇: 路径级 tracing
  ├─ §1 ftrace function/function_graph(函数路径)
  ├─ §2 kprobe/uprobe(动态获取参数/返回值)
  ├─ §3 tracepoint/直方图/trace-cmd(结构化事件)
  ├─ §4 BCC(可复用工具)
  └─ §5 bpftrace(一行脚本与聚合)
先讲: 函数调用 → 动态插桩 → 事件聚合 → 工具箱 → 一行脚本
后续依赖: 07-wait-analysis-optimization(把事件路径归类为不同等待)
```

### 叙事顺序

1. 问题引入——`iostat` 说磁盘慢，但一次请求到底在哪段路径停了？（**Aha: tracing 把“某资源慢”推进到“哪条路径、哪个参数、哪个阶段慢”**）
2. ftrace function/function_graph——看函数入口、调用关系和耗时
3. kprobe/uprobe——动态插入参数与返回值观测
4. tracepoint/hist/trace-cmd——结构化事件和时间线
5. BCC——现成的 eBPF 诊断工具
6. bpftrace——用脚本构造临时观测
7. 收束——计数器到路径证据

### 1. ftrace function 与 function_graph — 看内核函数路径

场景提示: 想知道一次系统调用进入内核后调用了哪些函数，为什么不能只看一个顶层耗时？ [写作时展开]

关键设计: ftrace 提供函数跟踪器和 function_graph 调用关系；实际可用函数、权限和开销由内核配置决定：

```[pseudocode]
cd /sys/kernel/debug/tracing

function:
  echo function > current_tracer
  → 记录匹配函数的调用事件

function_graph:
  echo function_graph > current_tracer
  → 以嵌套形式显示调用关系
  → 可观察入口/返回与部分耗时

过滤:
  echo target_func > set_ftrace_filter
  echo target_func > set_ftrace_notrace

输出:
  trace      → 读取当前缓冲
  trace_pipe → 流式消费
```

Why: 为什么不能打开 function_graph 后就把所有调用都采集？——**函数数量和调用频率会迅速放大 trace buffer、CPU 开销和输出量**；应先按 PID、CPU、函数过滤，短时间采集，并确认 tracing 本身没有改变被测现象。 [内核: ftrace buffer、tracer options 和过滤器受内核配置/权限影响]

比喻锚点: function_graph 像给工厂每个工位装摄像头；全厂同时录像成本极高，先缩小到一条生产线更有价值。 [写作时展开]

### 2. kprobe 与 uprobe — 不改源码也能观测参数/返回值

场景提示: 现有内核函数没有你需要的日志，能不能临时查看它被谁调用、传入什么参数、返回什么结果？ [写作时展开]

关键设计: kprobe 动态插入内核函数探针，uprobe 插入用户程序指令位置；kretprobe/uretprobe 观察返回：

```[pseudocode]
kprobe:
  attach kernel_symbol
  → 入口触发
  → 读取架构相关寄存器/参数

kretprobe:
  attach kernel_symbol
  → 函数返回时读取 retval

uprobe:
  attach /path/to/binary + offset
  → 用户态指令执行时触发

安全边界:
  参数读取格式依赖 ABI/内核版本/符号
  探针有执行开销和递归/上下文限制
```

Why: 为什么动态探针不能当作“任意函数都能无风险打印参数”？——**参数位置、类型、内联、优化、符号变化和探针上下文都会影响可靠性**；高频函数中 `printf` 式输出会严重扰动系统，应使用过滤、采样和聚合。 [内核: kprobe/uprobe 的可附着点与寄存器参数解释依赖架构和内核；[man 2 perf_event_open: 用户/内核探针能力也受权限控制]

比喻锚点: kprobe/uprobe 像临时在流水线入口贴传感器，不改机器设计，但传感器位置和采样方式不对就会读错或拖慢生产。 [写作时展开]

### 3. tracepoint、histogram 与 trace-cmd — 事件时间线和聚合

场景提示: 逐条打印事件太吵，怎样统计“哪个进程调用最多、延迟分布怎样、哪个阶段最常发生”？ [写作时展开]

关键设计: tracepoint 提供相对稳定的结构化事件接口，hist trigger 和 trace-cmd 将事件转成聚合或时间线：

```[pseudocode]
tracepoint:
  syscalls/sys_enter_read
  sched/sched_switch
  block/block_rq_issue/complete
  → 事件字段由内核定义

histogram:
  keys = pid/comm/device 等
  vals = hitcount/latency histogram
  → 聚合后输出分布

trace-cmd:
  record -e <events> / -p function_graph
  → report 时间线
  → KernelShark 等工具可视化
```

Why: 为什么结构化事件通常比 printk 更适合性能分析？——**tracepoint 字段稳定、可过滤、可聚合，避免把每条事件都格式化成文本**；但事件定义、采样窗口、丢事件和聚合维度仍要记录。直方图告诉你分布，不自动告诉你因果关系。 [内核: tracepoint 是内核预定义事件接口，事件名/字段应通过目标系统的 tracing events 检查]

比喻锚点: printk 是把每辆车都喊停登记，tracepoint/hist 是安装自动计数器和分桶器，既保留问题特征，又减少人工登记开销。 [写作时展开]

### 4. BCC — 把常见诊断路径封装成工具

场景提示: 你不想每次都手写 eBPF，怎样快速看 exec、TCP 重传、块 I/O 延迟和运行队列？ [写作时展开]

关键设计: BCC 提供 Python/C 混合工具，把探针、内核程序、事件输出和用户态聚合封装起来：

```[pseudocode]
BCC 工具的一般结构:
  Python 解析参数/打印结果
  → 编译或加载 BPF 程序
  → attach kprobe/tracepoint/uprobe
  → BPF map/perf buffer 收集事件
  → 用户态聚合/格式化

工具例子:
  execsnoop → exec 事件
  opensnoop → open 路径
  tcpretrans → TCP 重传
  biotop/biolatency → 块 I/O
  runqlat → 运行队列延迟
  profile → CPU 栈采样
```

Why: 为什么现成工具仍要先读源码和帮助，而不是盲信输出？——**工具默认探针、内核版本、事件字段、过滤范围和输出单位都会影响结果**；一个“重传”工具能告诉你事件发生，不一定能独立证明物理丢包根因。 [内核: BCC 工具依赖 BPF verifier、内核 helper、tracepoint/kprobe 可用性和权限]

比喻锚点: BCC 像工具箱里的专用扳手，比手工造工具快；但使用前仍要确认螺栓型号和扳手刻度。 [写作时展开]

### 5. bpftrace — 用短脚本快速构造观测

场景提示: 现成 BCC 工具不符合问题，怎样用几行脚本统计系统调用参数、函数返回值或调用栈？ [写作时展开]

关键设计: bpftrace 用高层探针语法、聚合变量和内置输出快速表达观测：

```[pseudocode]
tracepoint:syscalls:sys_enter_openat
  → 读取 args 字段
  → 按 comm/filename 过滤或计数

kprobe:vfs_read
  → 入口记录 pid/comm
kretprobe:vfs_read
  → 返回时记录 retval

@[comm] = count()
@latency = hist(value)
@[kstack] = count()
```

Why: 为什么 bpftrace 适合临时探查，却不等于生产永久监控？——**脚本可能采集高频事件、占用 map/buffer、受 verifier 和 ABI 变化影响**；生产使用要限制采样、过滤 PID/CPU、控制输出并验证卸载。硬件事件、kprobe、tracepoint 的语义也不同，不能只改探针名字就认为结果可比。 [内核: eBPF 程序必须通过 verifier，helper、map 和 attach 点受内核版本/权限限制]

比喻锚点: bpftrace 像 tracing 的 AWK：小问题能一行表达，但复杂生产诊断仍需要版本化脚本、资源预算和验证流程。 [写作时展开]

### 6. 收束

从聚合指标到路径证据：

```[pseudocode]
vmstat/iostat/ss 发现异常
  → ftrace 看函数路径
  → kprobe/uprobe 读关键参数
  → tracepoint/hist 看分布/时间线
  → BCC 复用成熟诊断模板
  → bpftrace 针对问题临时编程
  → 仍需 benchmark/业务指标验证因果与回归
```

**Aha Moment**: "tracing 的价值不是输出更多日志，而是把**资源异常、事件发生、函数路径、参数和时间分布**连成证据链；工具越灵活，越需要控制采样和确认观测本身没有制造新问题。"
**回答读者三问**: ①ftrace 和 eBPF 怎么选=ftrace 适合函数/时间线，eBPF 适合条件、参数和聚合；②为什么不能全量打印=高频事件的观测开销会污染系统；③工具输出能否直接当根因=不能，还需结合 workload、指标和路径验证。

---

### 核心悬念

**"tracing 看到了一条慢路径，但慢到底来自 CPU 执行、内存、磁盘、网络、锁、时间等待还是队列排队？如何把七类等待定量分开并选择优化动作？"**

→ 引出 07-wait-analysis-optimization — 七维等待分析与优化。