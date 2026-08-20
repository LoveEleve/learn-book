# bpftrace 编程 — 探针、过滤、动作与 Map 聚合如何组成一次 tracing

> Cluster B: 5 KPs | 依赖: 01-ebpf-architecture、02-ebpf-maps-ringbuf | 读者基线: kprobe/uprobe/tracepoint、Map、verifier
> 读者处境: 03 篇讲了长期 libbpf 工程；本篇换成快速实验视角：不写完整 loader，如何用一行 bpftrace 验证一个性能假设？
> 打开新视角: bpftrace 的短脚本仍会经过**解析、编译、verifier、attach 和事件消费**；“一行命令”只是隐藏了工程流水线，不是绕过内核安全检查

---

### 概念依赖链

```
01 架构 + 02 Maps/事件管道 → 本篇: bpftrace 语言与探针
  ├─ §1 probe/filter/action(脚本基本结构)
  ├─ §2 kprobe/uprobe/tracepoint/USDT/PMU(事件来源)
  ├─ §3 控制流/参数/内置变量(表达式)
  ├─ §4 @ Map 聚合(count/hist/stats)
  └─ §5 编译/加载/验证流程
先讲: 语法 → 探针 → 变量 → 聚合 → 编译执行
后续依赖: 05-tracing-sources-selection(不同探针稳定性与选择)
```

### 叙事顺序

1. 问题引入——想知道 `openat` 被谁调用、延迟怎样分布，为什么不先写一个完整 C/Go loader？（**Aha: bpftrace 是快速表达观测假设的语言，但生成的 BPF 仍受内核 verifier、ABI 和资源边界约束**）
2. probe/filter/action——脚本骨架
3. 探针类型——事件发生在哪里
4. 控制流/内置变量/字符串
5. `@` 聚合 Map
6. 编译与加载流程
7. 收束——原型与生产的边界

### 1. probe、filter、action — 一条 bpftrace 语句的三部分

场景提示: 想只统计 nginx 的 `openat`，而不是全系统所有进程，过滤应放在哪里？ [写作时展开]

关键设计: bpftrace 脚本通常由探针、可选 predicate 和 action 组成：

```[pseudocode]
probe[,probe...]
/filter predicate/
{
  action statements
}

示例:
tracepoint:syscalls:sys_enter_openat
/comm == "nginx"/
{
  printf("pid=%d file=%s\n", pid, str(args->filename));
  @opens[comm] = count();
}
```

Why: 为什么过滤要尽可能靠近 eBPF 程序，而不是全量输出后再 grep？——**事件输出和用户态格式化本身很贵**：内核侧先过滤可以减少 ring/perf buffer、上下文切换和用户态处理。但 predicate 仍受 verifier、字符串读取和上下文可用字段限制，复杂过滤不一定免费。 [内核: bpftrace 生成的程序仍需 verifier，输出 helper 和 Map 操作也受程序类型限制]

比喻锚点: predicate 像仓库门口的筛选器，先只放行 nginx 的包裹，再进入昂贵的登记流程。 [写作时展开]

### 2. 探针类型 — 选择事件发生的边界

场景提示: 同样要观测函数调用，为什么 tracepoint 通常比 kprobe 更稳定，uprobe 又必须绑定用户二进制？ [写作时展开]

关键设计: 不同探针的稳定性、参数语义和触发时机不同：

```[pseudocode]
kprobe/kretprobe:
  内核函数入口/返回
  → 灵活, 但符号/参数/内联可能随内核变化

uprobe/uretprobe:
  用户态二进制/共享库指令或符号
  → 依赖路径、符号、偏移和构建

tracepoint:
  内核预定义静态事件
  → args 字段相对稳定, 适合长期脚本

USDT:
  用户程序主动提供的静态探针
  → 需要应用/运行时暴露对应 probe

software/hardware/profile/interval:
  软件计数器、PMU 事件或定时采样
  → 支持性、频率和权限依赖环境
```

Why: 为什么没有“最好的探针”？——**稳定性、细粒度、开销和参数可见性互相制约**：kprobe 灵活但 ABI 脆弱，tracepoint 稳定但字段固定，USDT 需要应用配合，PMU 事件需要硬件支持。探针选择要先确认目标系统可用性和事件语义。 [内核: tracepoint/kprobe/PMU attach 点由内核、架构和配置共同决定]

比喻锚点: tracepoint 是官方观景台，kprobe 是临时在路边架相机，uprobe 是进入某栋用户程序内部拍摄；视野和稳定性各不同。 [写作时展开]

### 3. 控制流、参数和内置变量 — 在受限语言里表达条件

场景提示: 怎样只统计返回值大于 0 的 `read`，并把进程名和延迟一起打印？ [写作时展开]

关键设计: bpftrace 提供条件、命令行参数、内置上下文、字符串读取和有限控制流：

```[pseudocode]
内置变量:
  pid/tid/uid/cpu/comm/nsecs
  kstack/ustack

参数:
  $1/$2/... 从命令行传入

内核参数:
  kprobe 参数/寄存器语义依赖架构与函数 ABI
tracepoint 参数:
  args->field 来自事件定义, 通常更稳定

字符串:
  str(kernel_ptr)
  ustr(user_ptr) / 具体版本语法按 bpftrace 文档

控制流:
  if/else/三元/有限 unroll
  → 最终仍受 BPF 指令与 verifier 限制
```

Why: 为什么不能把 kprobe 的 `arg0` 当成所有内核函数的稳定参数？——**参数传递受架构 ABI、函数原型、编译优化、内联和内核版本影响**；tracepoint 的结构化字段通常更适合长期使用。字符串读取还可能失败、截断或产生开销，不能在高频路径无条件打印。 [man 1 bpftrace: 内置变量、参数和探针上下文按版本语法核对]

比喻锚点: 内置变量像工具箱里已经标号的仪表，函数参数像临时接线；临时接线换机器后可能插错孔。 [写作时展开]

### 4. `@` Map — count、hist、stats 与配对计时

场景提示: 逐条输出百万次事件会把系统拖垮，怎样在内核侧聚合成计数和延迟分布？ [写作时展开]

关键设计: `@` 变量是 bpftrace 对 Map/聚合操作的语法封装：

```[pseudocode]
@count[comm] = count()
  → 按进程聚合事件次数

@dist = hist(value)
  → 生成分桶直方图

@stats = stats(value)
  → 聚合 count/avg/total 等统计

入口/返回配对:
  kprobe:func { @start[tid] = nsecs; }
  kretprobe:func {
    @latency = hist(nsecs - @start[tid]);
    delete(@start[tid]);
  }
```

Why: 为什么 Map 聚合比 printf 更适合高频事件？——**它减少事件传输和用户态格式化，但仍会占用 Map 内存、聚合更新和读取开销**；入口/返回配对还要处理线程退出、递归、丢失返回和异常路径。hist 的桶是近似分布，不是完整原始样本。 [内核: Map 聚合的内存、并发更新和 verifier 限制仍然存在]

比喻锚点: 逐条 printf 像每辆车都登记完整行程，Map 聚合像只记录每个收费站通过多少辆、延迟落在哪个桶。 [写作时展开]

### 5. bpftrace 编译与加载 — 一行命令背后的完整链路

场景提示: bpftrace 脚本报 verifier error 或 attach failed 时，应该从哪一层排查？ [写作时展开]

关键设计: bpftrace 隐藏了编译和加载细节，但错误仍可以落在多个阶段：

```[pseudocode]
bpftrace -e 'script'
  → lexer/parser
  → semantic/type analysis
  → BPF/LLVM code generation
  → bpf() load
  → verifier
  → attach probe
  → Map/ring/perf event poll

排查层次:
  语法/类型错误
  → 目标探针不存在/字段不匹配
  → 权限/BTF/helper/内核能力
  → verifier 拒绝
  → 事件输出/用户态消费不足
```

Why: 为什么 bpftrace 不是“零编译、零加载”？——**它只是把编译、加载、attach 和事件消费自动完成**；每次运行仍可能需要 clang/LLVM、BTF、权限、内核 helper 和探针支持。生产长期工具还要考虑脚本版本、输出速率、卸载和兼容测试。 [内核: 最终生成的 BPF 字节码仍由 verifier 验证，bpftrace 不绕过安全边界]

比喻锚点: 一行命令像自动售货机，按按钮很简单，但机器内部仍要验货、出库、传送和记录库存；任一环节故障都可能导致取不到结果。 [写作时展开]

### 6. 收束

一次 bpftrace 观测闭环：

```[pseudocode]
选择事件边界
  → probe
  → predicate 过滤
  → action/Map 聚合
  → 编译生成 BPF
  → verifier + attach
  → poll 事件/读取聚合结果
  → 结合 workload 与其他观测验证
```

**Aha Moment**: "bpftrace 的强项是快速表达观测假设，而不是把复杂性消失；探针稳定性、ABI、verifier、Map 容量和输出开销仍决定结果是否可信。"
**回答读者三问**: ①如何减少 tracing 开销=内核过滤、聚合、采样而不是全量 printf；②kprobe/tracepoint 怎么选=灵活性与稳定性取舍；③脚本报错怎么查=语法、探针、权限/能力、verifier、消费链逐层定位。

---

### 核心悬念

**"kprobe 可能随内核升级失效，tracepoint 又不一定有足够字段；怎样按稳定性、参数、开销和生命周期选择 kprobe、uprobe、tracepoint、USDT 与 PMU？"**

→ 引出 05-tracing-sources-selection — tracing 数据源选择与辅助函数全景。