# eBPF 追踪源选择 — kprobe、uprobe、tracepoint、USDT 与 helper 如何取舍

> Cluster B: 5 KPs | 依赖: 01-ebpf-architecture、04-bpftrace-programming | 读者基线: bpftrace 探针、verifier、Map 聚合
> 读者处境: 04 篇已经会写脚本；本篇回答长期维护最关键的问题：探针该挂在哪里，怎样在稳定性、字段质量、覆盖范围和开销之间做选择？
> 打开新视角: 探针选择不是“越底层越强”，而是**先选稳定的语义边界，再在缺口处使用动态插桩，并把 ABI/版本风险写进维护方案**

---

### 概念依赖链

```
01 架构 + 04 bpftrace → 本篇: 追踪源与 helper 选择
  ├─ §1 kprobe/kretprobe(内核动态函数)
  ├─ §2 uprobe/uretprobe(用户态动态函数)
  ├─ §3 tracepoint(内核静态事件)
  ├─ §4 USDT(应用静态事件)
  ├─ §5 选择矩阵(稳定/字段/开销/维护)
  ├─ §6 helper(程序可调用的内核接口)
  └─ §7 与 strace/perf/ftrace 的边界
先讲: 动态内核 → 动态用户 → 静态内核/用户 → 选择 → helper → 工具定位
后续依赖: 06-cpu-memory-observability(按子系统选择现成工具)
```

### 叙事顺序

1. 问题引入——kprobe 能看到函数，为什么生产工具仍优先找 tracepoint/USDT？（**Aha: 稳定的语义事件往往比任意函数内部细节更适合长期维护**）
2. kprobe/kretprobe——覆盖广但 ABI 风险高
3. uprobe/uretprobe——用户二进制动态插桩
4. tracepoint/USDT——静态事件契约
5. 选择矩阵——稳定、字段、开销、维护
6. helper——程序能调用什么
7. 传统工具与 eBPF——互补而非替代
8. 收束——探针选择决策树

### 1. kprobe / kretprobe — 内核函数级动态插桩

场景提示: 内核没有现成 tracepoint，但必须观察某个函数入口和返回值，怎么办？ [写作时展开]

关键设计: kprobe/kretprobe 允许动态附着内核函数，但依赖函数符号、ABI 和当前实现：

```[pseudocode]
kprobe:func
  → 函数入口触发
  → 读取上下文/参数(架构与 ABI 相关)

kretprobe:func
  → 函数返回触发
  → 读取 retval

可用性:
  /sys/kernel/debug/tracing/available_filter_functions
  或 bpftrace/perf 列出实际可用目标
```

Why: 为什么 kprobe 不是稳定 API？——**函数名、签名、内联、调用路径和参数寄存器都可能随内核版本/配置改变**；它适合探索内部路径和临时诊断，但长期工具要有版本检测、失败降级和测试。返回探针还可能受递归、异常返回和高频开销影响。 [内核: kprobe 的 attach/跳板/执行语义由架构与内核实现提供，不是稳定用户 ABI]

比喻锚点: kprobe 像临时在机器内部某根管道上装传感器，覆盖面广，但换型号后管道位置可能变。 [写作时展开]

### 2. uprobe / uretprobe — 用户态二进制函数级动态插桩

场景提示: 想观察 malloc、数据库函数或应用内部函数，但不想改源码，怎样在用户态插入探针？ [写作时展开]

关键设计: uprobe 绑定 ELF 路径、符号或偏移，参数解释依赖目标 ABI 和编译产物：

```[pseudocode]
uprobe:/path/to/binary:symbol
  → 用户态指令位置触发

uretprobe:/path/to/binary:symbol
  → 函数返回触发

参数:
  x86-64 前几个整数/指针参数通常在寄存器
  → 具体寄存器/调用约定由架构与 ABI 决定

部署边界:
  ASLR/PIE、符号剥离、版本路径、内联和优化
  → 可能导致探针无法绑定或字段解释错误
```

Why: 为什么 uprobe 比 kprobe 更难写成“永远有效”？——**用户二进制升级、符号、偏移、编译器优化和多架构 ABI 都会变化**；USDT 若有同等语义点，通常更适合长期使用。高频用户函数也要控制采样与输出，避免探针改变应用本身。 [内核: uprobe 触发发生在用户指令执行路径，attach 对象生命周期与进程映射相关]

比喻锚点: uprobe 像在某个版本的机器电路板焊测试线，精确但换板子后焊点可能消失。 [写作时展开]

### 3. tracepoint — 内核提供的静态事件契约

场景提示: 想长期观察 `openat`/调度/块 I/O，为什么优先寻找 tracepoint？ [写作时展开]

关键设计: tracepoint 由内核源码显式定义事件和字段，工具通过事件名与格式读取结构化上下文：

```[pseudocode]
列出:
  bpftrace -l 'tracepoint:*'
  /sys/kernel/tracing/events/

使用:
  tracepoint:syscalls:sys_enter_openat
  → args->filename / args->dfd 等事件字段

验证:
  读取对应 format 文件
  → 确认字段类型/偏移/可用性
```

Why: 为什么 tracepoint “更稳定”不等于永远兼容？——**它是公开程度更高的内核事件接口，但事件、字段和配置仍可能随内核版本变化**；长期工具仍需检查字段存在、内核能力和权限。相对 kprobe，tracepoint 不需要猜函数内部参数调用约定，维护风险通常更低。 [内核: tracepoint 事件格式由 tracing/events 暴露，具体字段以目标内核为准]

比喻锚点: tracepoint 像工厂预留的标准仪表口，接口更稳定；kprobe 像直接夹在内部任意电线上，能看到更多但更容易随改版失效。 [写作时展开]

### 4. USDT — 应用主动提供的静态语义点

场景提示: Node.js、PostgreSQL、JVM 等应用想让外部工具稳定观测 GC、查询或请求生命周期，为什么要提供 USDT？ [写作时展开]

关键设计: USDT 是用户程序在构建时嵌入的静态探针，应用维护者承诺 probe 名称、参数和语义：

```[pseudocode]
应用构建:
  嵌入静态 probe metadata/触发点

发现:
  readelf/工具列出 USDT probe
  bpftrace -l 'usdt:/path/to/bin:*'

运行:
  usdt:binary:probe_name
  → 读取应用定义的参数
  → 不必猜内部函数名称或偏移
```

Why: 为什么 USDT 通常比 uprobe 更适合业务语义？——**它表达“请求开始/GC 开始/查询结束”这类稳定事件，而不是某个可能被内联的函数**；但是否存在、字段如何定义仍取决于应用版本和构建选项，不能把某项目的 USDT 当成所有版本统一接口。 [内核: USDT 最终仍通过用户态探针机制触发，应用 metadata 决定工具能看到什么]

比喻锚点: USDT 像应用开发者预留的标准检修口；uprobe 是外部工程师临时拆机寻找线缆。 [写作时展开]

### 5. 选择矩阵 — 稳定性、覆盖、字段和开销

场景提示: 同一个问题有 kprobe、tracepoint 和 USDT 三种可能入口，怎样做出可维护选择？ [写作时展开]

关键设计: 用决策树而不是固定排名：

```[pseudocode]
1. 有表达完整语义且版本稳定的 tracepoint/USDT?
   → 优先使用, 并验证字段/参数
2. 没有静态点, 但有稳定用户函数?
   → uprobe, 记录二进制/符号/版本约束
3. 只剩内核内部函数?
   → kprobe/kretprobe, 加版本探测/降级/测试
4. 需要采样而非逐事件?
   → profile/PMU/perf, 控制频率与开销

评估维度:
  语义稳定性 / 参数质量 / 事件频率 / attach 成本
  版本兼容 / 权限 / 回滚 / 对业务扰动
```

Why: 为什么不能给 kprobe、tracepoint、uprobe 规定固定纳秒级开销排名？——**架构、内核版本、事件频率、指令路径、采样方式和用户态消费都会改变总成本**；生产决策应在目标环境 benchmark，而不是引用一个脱离上下文的常数。 [内核: helper、probe 和 tracepoint 的实际开销与实现/频率/上下文有关]

比喻锚点: 选择传感器像选医学检查：标准化心电图适合长期监测，临时穿刺能看细节但风险更高；要按问题选，不按工具名排位。 [写作时展开]

### 6. Helper — eBPF 程序可调用的受限内核 API

场景提示: 探针拿到 PID、时间、Map 和字符串数据时，为什么不能直接调用任意内核函数？ [写作时展开]

关键设计: helper 通过 ID 和 prototype 暴露受控能力，verifier 按程序类型检查参数与返回：

```[pseudocode]
身份/时间:
  bpf_get_current_pid_tgid
  bpf_get_current_comm
  bpf_ktime_get_ns

Map:
  bpf_map_lookup_elem
  bpf_map_update_elem
  bpf_map_delete_elem

读取/输出:
  bpf_probe_read_kernel/user[_str]
  bpf_perf_event_output
  bpf_ringbuf_output/reserve/submit

能力:
  helper 是否存在、可否在当前 program type 调用
  → 用 bpftool feature probe/文档/加载验证确认
```

Why: 为什么 helper 是“稳定 API”却不能把 helper id 和可用性写死？——**helper 集合、参数 prototype、程序类型权限和内核版本都会变化**；工具列出的支持能力只对当前环境有效。`bpf_trace_printk` 适合调试，不应作为高频生产输出通道。 [内核: verifier 用 helper prototype 和 program type 白名单限制调用]

比喻锚点: helper 像内核提供的受控服务柜台，程序只能办理规定业务，不能直接进入后端仓库。 [写作时展开]

### 7. 传统工具与 eBPF — 不是“新旧替代”，而是证据层互补

场景提示: 一个系统调用慢，应该用 strace、perf、ftrace 还是 eBPF？ [写作时展开]

关键设计: 工具回答的问题不同：

```[pseudocode]
strace:
  系统调用边界/参数/返回/时间

perf:
  CPU/PMU/采样/调用栈/调度

ftrace:
  内核函数/tracepoint/时间线

eBPF:
  条件过滤、状态聚合、动态参数、策略/数据面

选择:
  先用最小扰动且语义匹配的工具
  → 需要条件/关联/聚合时再上 eBPF
```

Why: 为什么 eBPF 不是所有工具的替代品？——**strace 的系统调用可读性、perf 的 PMU/采样、ftrace 的内核时间线仍各有优势**；eBPF 增加可编程性，同时带来 verifier、版本、权限和资源管理成本。 [内核: eBPF、ftrace、perf 可共享部分事件基础设施，但观测语义和开销不同]

### 8. 收束

追踪源选择闭环：

```[pseudocode]
明确问题语义/生命周期
  → 查 tracepoint/USDT
  → 没有时选择 uprobe/kprobe
  → 按事件频率选择追踪或采样
  → 用 helper 获取必要上下文
  → Map 聚合/Buffer 输出
  → 在目标内核和 workload 验证稳定性/开销
```

**Aha Moment**: "探针选择不是 kprobe 越底层越好，而是**语义稳定性优先、覆盖缺口再用动态插桩、最后用目标环境验证开销和维护成本**。"
**回答读者三问**: ①长期工具优先什么=稳定的 tracepoint/USDT；②没有静态点怎么办=uprobe/kprobe，并记录版本/ABI 风险；③helper 怎么确认=按当前内核、程序类型和 verifier 能力探测。

---

### 核心悬念

**"追踪源选好了、helper 也会用了；CPU 跑满、内存逼近 OOM 时，哪些 eBPF 工具能快速把症状连到进程、调用栈和内核路径？"**

→ 引出 06-cpu-memory-observability — execsnoop、runqlat、profile、oomkill、memleak 与 cachestat。