# 追踪源选择 — kprobe/kretprobe/uprobe/uretprobe/tracepoint/USDT + 辅助函数全景

> Cluster B: 5 KPs | 依赖: 01 架构 + 04 bpftrace | 读者基线: 会 bpftrace 一行命令 + 理解探针概念

---

### 1. kprobe / kretprobe — 内核函数级动态插桩
  - 原理: 内核函数入口处 int3 → do_int3 → kprobe handler → post_handler → 单步执行原始指令 → 恢复 (BPF之巅 Ch2 §7)
  - attach 方式: `bpftrace -e 'kprobe:tcp_v4_connect { ... }'` → `/sys/kernel/debug/tracing/available_filter_functions` 列出可 kprobe 的 4万+ 函数 (BPF之巅 Ch2 §8)
  - kretprobe: 函数返回时 → `kretprobe:tcp_v4_connect { @ret[retval] = count(); }` → 只能拿返回值, 拿不到输出参数 (BPF之巅 Ch2 §9)
  - 风险: 非稳定 API → 内核升级函数名/签名/内部逻辑都可能变 → tracepoint 有 ABI 保证但 kprobe 无 (详见 §5)

### 2. uprobe / uretprobe — 用户态函数级动态插桩
  - 原理: 用户态函数入口处 int3 → SIGTRAP → uprobe handler → 单步执行 → 恢复 (BPF之巅 Ch2 §9)
  - attach: `bpftrace -e 'uprobe:/usr/bin/nginx:ngx_http_process_request { ... }'` → 需要知道二进制路径+符号(或 offset) (eBPF开发指南 Ch10 §2)
  - 读取参数: `reg("di")` / `reg("ax")` → x86 调用约定: 前 6 个参数在 rdi/rsi/rdx/rcx/r8/r9 → `sarg0` 栈上第 0 个参数 (eBPF开发指南 Ch10 §1)
  - 适用场景: Nginx请求处理/MySQL查询解析/Java方法耗时 → 无改代码的透明观测

### 3. tracepoint — 内核静态打点 (ABI 稳定!)
  - 原理: 内核 `TRACE_EVENT` 宏 → 编译时生成 tracepoint → 不可随意删除/改名/改参数 → ABI 等价 (BPF之巅 Ch2 §10)
  - 列清单: `bpftrace -l 'tracepoint:syscalls:*'` → `/sys/kernel/debug/tracing/events/` 按子系统组织 → `perf list tracepoint` (BPF之巅 Ch2 §10)
  - 参数获取: `args->filename` (tracepoint) vs `str(arg1)` (kprobe) → tracepoint 带类型信息, 不靠调用约定猜 (eBPF开发指南 Ch10 §4)
  - 底层: `tracepoint:syscalls:sys_enter_openat` → `format` 文件定义字段类型/偏移 → BTF 也能解析

### 4. USDT (User-Level Statically Defined Tracing) — 用户态静态打点
  - 原理: 编译时在二进制中嵌入 NOP → `$0` 占位符 → 运行时替换为 int3 → 比 uprobe 更稳定(厂商承诺不删) (BPF之巅 Ch2 §9)
  - 探测: `readelf -n /usr/bin/node | grep NT_STAPSDT` → `bpftrace -l 'usdt:/usr/bin/node:*'` → 列出 USDT 探针 (eBPF开发指南 Ch10 §3)
  - 典型应用: Node.js(`gc__start`)/Python(`function__entry`)/PostgreSQL(`query__start`)/JVM(方法入口/GC) → 无需重新编译就能观测
  - 对比 uprobe: USDT = 开发者承诺稳定的度量点, uprobe = 动态注入任何函数=比 kprobe 更不承诺兼容

### 5. 选择指南 — 何时用哪个 (开销/稳定/维护 三角对比)
  - **稳定性**: tracepoint(ABI, 最高) > USDT(厂商承诺) > uprobe(函数不改名则 OK) > kprobe(随时可能变) (BPF之巅 Ch2 §10)
  - **开销**: 全部微秒级 → kprobe ~100ns → tracepoint ~50ns(无 int3 弹跳) → uprobe 最慢(跨内核↔用户态) → profile hz:99 采样可忽略 (BPF之巅 Ch2 §7-10)
  - **可用性**: kprobe(4万+函数) > uprobe(任意用户态函数) > tracepoint(~2000个) > USDT(特定应用)
  - 决策流程: 有 tracepoint→用它, 没有→有 USDT→用它, 都没→kprobe/uprobe 但标注"非稳 API" → 维护上优先跑 tracepoint 版本
  - 真案例: 追 memleak → BCC memleak.py 用 uprobe:malloc + uprobe:realloc + uprobe:free → 无 tracepoint/USDT 覆盖 → 合理但警告"glibc 版本依赖"

### 6. 辅助函数全景 — 内核态 BPF 可调用的稳定 API
  - 追踪辅助: `bpf_get_current_pid_tgid()` (pid+tgid, helper 14) / `bpf_get_current_comm()` (comm, helper 15) / `bpf_probe_read()` (读任意内核内存, helper 4) / `bpf_probe_read_str()` (读字符串, helper 28) (eBPF开发指南 Ch10 §5-7)
  - 时间/随机: `bpf_ktime_get_ns()` (纳秒, helper 5) / `bpf_get_prandom_u32()` (随机数, helper 7) / `bpf_get_numa_node_id()` (NUMA, helper 42) (深入理解eBPF Ch2 §4)
  - Map 操作: `bpf_map_lookup_elem()` (1) / `bpf_map_update_elem()` (2) / `bpf_map_delete_elem()` (3) / `bpf_map_get_next_key()` (迭代用, 4) → 见 02 (eBPF开发指南 Ch9 §3)
  - 输出: `bpf_trace_printk()` (调试 printf, helper 6, 最多 3 参数) / `bpf_perf_event_output()` (生产输出, helper 32) / `bpf_ringbuf_output()` (新建议, helper 84) (BPF之巅 Ch2 §5)

### 7. 动态追踪工具全景 — eBPF 的位置
  - 对比矩阵:
    - strace: 每个系统调用 2 次上下文切换(用户→内核→用户), 不可生产环境使用, 仅调试 (eBPF开发指南 Ch3 §1)
    - ltrace: 追踪动态库调用(用户态), 不覆盖内核行为, 开销比 strace 小但仍不可生产 (eBPF开发指南 Ch3 §2)
    - SystemTap: 内核态追踪, 但需签章模块(RedHat/Marvell 专有), 非主线内核 → 社区萎缩, RHEL 7 后不再推荐 (eBPF开发指南 Ch3 §3)
    - LTTng: 2.6 内核引入, 低开销追踪, 但用户态工具链独立、社区小、不如 eBPF 生态 (eBPF开发指南 Ch3 §4)
    - ftrace: 内核内置(function_graph/trace_printk), 零依赖但无编程能力 — 只能"看"不能"算" (eBPF开发指南 Ch3 §5)
    - perf: 硬件 PMU 采样, `perf record -g -F 99` 火焰图经典, 但无 BPF 灵活性(不能动态注入代码、不能条件过滤) (B2 Ch2 §5)
  - ftrace 与 eBPF 的关系: ftrace 提供 tracepoint + function_graph 基础设施 → eBPF 通过 `register_ftrace_function()` 复用 ftrace 的插桩点 → eBPF 不是替代 ftrace, 而是 ftrace 的"可编程界面" (B2 Ch2 §5)
  - eBPF 独特性: 安全(验证器保证不会 panic 内核) + 高效(JIT 编译为原生指令) + 弹性(动态挂载/卸载, 无重启) + 可编程(C 代码编写) — 四个维度中, 没有一个工具能做到三项以上
  - 定位: eBPF 不是替代 strace/perf/ftrace, 而是给所有传统工具加了一个"可编程层" → 以前只能"显示", 现在可以"计算并行动"

### 8. 收束
  - kprobe/uprobe 是"瑞士军刀"(什么都能切) — tracepoint/USDT 是"手术刀"(针对特定器官且可重复使用)
  - 稳定性选 tracepoint/USDT → 覆盖度选 kprobe/uprobe → 生产"tracepoint 优先, kprobe 兜底"原则
  - 辅助函数 100+ 但记住 "probe_read + get_current + ktime_get_ns + map_lookup/update" = 搞定 90% 的场景
  - 动态追踪全景: eBPF 不是替代品 — 是给传统工具(strace/perf/ftrace)加上"可编程层" — 从此不止"显示现象"而是"计算根因"

---

### 核心悬念
**"追踪源选好了 — 现在该看具体子系统了。CPU 跑满了？内存快 OOM 了？对应的观察工具是什么？"**

→ 引出 06-cpu-memory-observability — execsnoop/runqlat/cpudist/profile/syscount + oomkill/memleak/cachestat/vmscan
