# Ftrace + BPF 深层跟踪 — 从 function_graph 到 bpftrace

> Cluster C: 6 KPs | 依赖: 05-memory-disk-network-observability | 读者基线: 会用 `perf` + 理解内核子系统架构

---

### 1. Ftrace function 跟踪 — 函数入口/出口
  - `cd /sys/kernel/debug/tracing` → `echo function > current_tracer` → `cat trace` 所有内核函数调用 (性能之巅 Ch14 §1-3)
  - `echo func_name > set_ftrace_filter` → 过滤特定函数 → `echo func_name > set_ftrace_notrace` 排除噪音
  - `echo 1 > options/func_stack_trace` → 每个函数调用带调用栈 → 找调用者
  - trace_pipe: 流式输出(`cat trace_pipe | head -100`) → trace: 完整记录(缓冲区读完) → buffer_size_kb 控制容量

### 2. kprobes + uprobes — 动态插桩
  - kprobe 事件: `echo 'p:myprobe do_sys_open filename=+0(%si):string' > kprobe_events` → 在 `do_sys_open` 入口读取参数 `filename` (性能之巅 Ch14 §4-5)
  - kretprobe 返回: `echo 'r:myretprobe do_sys_open ret=$retval' > kprobe_events` → 保存返回值到 `ret` 字段
  - uprobe 用户态: `echo 'p:uprobe_test /bin/bash:0x12345' > uprobe_events` → 需要知道指令地址 `objdump -d`
  - uprobe 返回: `echo 'r:uretprobe_test /bin/bash:0x12345' > uprobe_events` → 同上格式
  - 事件过滤: `echo 'state == 2' > /sys/kernel/debug/tracing/events/raw_syscalls/sys_enter/filter` → 按条件过滤减少噪音

### 3. Ftrace function_graph — 调用图 + 直方图
  - `echo function_graph > current_tracer` → `cat trace` 得嵌套调用关系树 → 每个函数的执行时间(微秒级) (性能之巅 Ch14 §6-7)
  - `echo 1 > options/funcgraph-duration` → 显示每次调用耗时 → `echo 1 > options/funcgraph-overhead` 显示跟踪开销
  - hwlat detector: `echo hwlat > current_tracer` → 检测硬件中断导致的延时抖动(SMI/NMI)
  - 直方图(单key): `echo 'hist:keys=common_pid.execname:vals=hitcount' > events/syscalls/sys_enter_read/trigger` → `cat events/syscalls/sys_enter_read/hist` 看每个进程read调用频率 (性能之巅 Ch14 §8)
  - 直方图(多key+字段): `echo 'hist:keys=pid:vals=$retval:sort=hitcount' > events/sched/sched_waking/trigger` → 看调度延迟分布
  - trace-cmd + KernelShark: `trace-cmd record -p function_graph -g func` → `trace-cmd report` → `kernelshark trace.dat` GUI可视化 (性能之巅 Ch14 §9)

### 4. BCC 工具链 — Python+eBPF 小工具族
  - `execsnoop-bpfcc`: 打印每个新进程 exec → `opensnoop-bpfcc` 打印每个 `open()` 调用路径 (性能之巅 Ch15 §1-2)
  - `tcpretrans-bpfcc`: 实时TCP重传(含重传原因) → `biotop-bpfcc` 磁盘IO top-N → `cachestat-bpfcc` 缓存命中率
  - `tcplife-bpfcc`: TCP连接寿命周期 → runqlat-bpfcc: CPU运行队列延迟直方图 → `cpuunclaimed-bpfcc` 闲置CPU运行队列
  - `profile-bpfcc -F 99 10 > folded`: 等同于 `perf record` 采样但BPF风格 → 可直接折叠为火焰图
  - BCC 本质: Python 脚本 → BPF C代码段 `bpf_text` → `bpf.attach_kprobe/attach_uprobe/attach_tracepoint` → `BPF_PERF_OUTPUT`

### 5. bpftrace 单行命令 — AWK for tracing
  - `bpftrace -e 'tracepoint:syscalls:sys_enter_openat { printf("%s %s\n", comm, str(args->filename)); }'` → 每个 `open` 调用的文件名 (性能之巅 Ch15 §1-2)
  - `bpftrace -e 'kprobe:do_nanosleep { @[comm] = count(); }'` → 聚合睡眠进程计数 → 内置 `@` 变量即 Map
  - `bpftrace -e 'kretprobe:vfs_read { @bytes = hist(retval); }'` → 返回值分布 histogram
  - `bpftrace -e 'tracepoint:sched:sched_switch { @latency[args->prev_comm] = hist(args->prev_prio); }'` → 多key 聚合
  - `bpftrace -e 'hardware:cache-misses:100000 { @[kstack] = count(); }'` → 每10w cache miss 取一次内核栈

### 6. 收束
  - Ftrace function_graph(调用图+每函数耗时) + kprobe/uprobe(插桩获取参数) = 内核级DTrace
  - BCC(biotop/tcpretrans/execsnoop) 是"面试/oncall 一键诊断60件套"
  - bpftrace = tracing界的AWK — 一行命令出 histogram/kstack/aggregation, 终极灵活度

---

### 核心悬念
**"你用 perf/ftrace/bpftrace 看到了函数耗时比例 — 但一个慢查询到底是 CPU 不够快(执行太多), IO 太慢(等磁盘), 还是锁竞争(排队等)？这七种等待你要定量区分。"**

→ 引出 07-等待分析: 7维定量区分(CPU/内存/磁盘/网络/锁/时间/队列)
