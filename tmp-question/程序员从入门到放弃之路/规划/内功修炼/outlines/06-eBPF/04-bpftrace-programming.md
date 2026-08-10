# bpftrace 编程 — 探针格式 + 过滤器 + 动作 + 探针类型 + 控制流 + 变量 + 映射表

> Cluster B: 5 KPs | 依赖: 01 架构 + 02 Maps | 读者基线: 理解 kprobe/uprobe/tracepoint 概念

---

### 1. bpftrace 语法全景 — 探针格式/过滤器/动作
  - 基础格式: `probe[,probe...] /filter/ { action }` → 一行 = 一个 eBPF 程序 (BPF之巅 Ch5 §1-2)
  - 探针格式: `provider:name[:frequency]` → `kprobe:do_sys_open` / `tracepoint:syscalls:sys_enter_openat` / `profile:hz:99` (BPF之巅 Ch5 §1)
  - 过滤器: `/pid == $1/` `/comm == "nginx"/` `/arg0 > 1000/` → 高性能(在 eBPF 里过滤, 不像 perf script 全量输出再 grep) (BPF之巅 Ch5 §2)
  - 动作: `{ printf("pid=%d file=%s\n", pid, str(args->filename)); @count = count(); }` → 任何有效的 bpftrace 语句 (BPF之巅 Ch5 §3)
  - Hello World: `bpftrace -e 'BEGIN { printf("hello world\n"); }'` → `bpftrace -e 'kprobe:do_sys_open { printf("%s\n", str(arg1)); }'` (eBPF开发指南 Ch6 §1)

### 2. 探针类型全谱 — 7 大类 + 触发时机
  - kprobe/kretprobe: 内核函数入口/返回 → `kprobe:tcp_connect` / `kretprobe:tcp_connect` → `arg0` 开始为函数参数 (BPF之巅 Ch5 §5)
  - uprobe/uretprobe: 用户态函数 → `uprobe:/usr/bin/bash:readline` / `uretprobe:/lib64/libc.so.6:malloc` → 需要二进制路径+符号 (eBPF开发指南 Ch6 §2)
  - tracepoint: 内核静态打点 → `tracepoint:syscalls:sys_enter_openat` → `args->filename` 比 kprobe 的 `str(arg1)` 稳定(BP不会变) (BPF之巅 Ch5 §6)
  - USDT: 用户态静态打点 → `usdt:/path/to/binary:probe_name` → DTrace 风格 → Node.js/Python/postgres 原生支持 (eBPF开发指南 Ch6 §2)
  - software: 内核软事件 → `software:page-faults:` `software:context-switches:` → 与 `perf stat` 的 software events 同名 (BPF之巅 Ch5 §7)
  - hardware: PMU 硬事件 → `hardware:cache-misses:` `hardware:instructions:` → 需要 root/perf_event_paranoid (BPF之巅 Ch5 §7)
  - interval/profile: 定时器 → `interval:s:1 { exit(); }` → `profile:hz:99 { @[kstack] = count(); }` → 采样非追踪 (BPF之巅 Ch5 §3)

### 3. 控制流 + 变量 + 内置变量
  - 控制流: `if/else` → `if (args->ret > 0) { ... }` / `?:` 三元 → 无显式 for/while(依赖 `unroll(N)`) → 最小化指令数 (BPF之巅 Ch5 §7)
  - 内置变量: `pid`/`tid`/`uid`/`cpu`/`comm`/`nsecs`/`kstack`/`ustack` → 无需从寄存器提取 → `printf("pid=%d comm=%s\n", pid, comm)` (eBPF开发指南 Ch6 §4)
  - 参数变量: `$1 $2 $3 ...` 从命令行传入 → `bpftrace -e 'kprobe:do_sys_open /comm == str($1)/ { ... }' -- nginx` (BPF之巅 Ch5 §3)
  - 字符串: `str(N)` 读取指针所指向的字符串 → 内核 `str(arg0)` 用于文件路径 → 用户态 `ustr(reg("si"))` 读取用户态字符串标记 (BPF之巅 Ch5 §3)

### 4. 映射表 — @ 开头的魔法变量
  - `@count = count()`: 事件计数器 → 每个 key 计数 → `interval:s:1 { print(@count); clear(@count); }` (BPF之巅 Ch5 §4)
  - `@dist = hist(N)`: 2的幂方直方图 → `kretprobe:vfs_read { @bytes = hist(retval); }` → 自动 bin → print 时输出 ASCII 柱状 (BPF之巅 Ch5 §4)
  - `@stats = stats(N)`: count/avg/total 聚合 → 比 hist 多统计 → `@timer = stats(nsecs);` (BPF之巅 Ch5 §4)
  - `@elapsed`: 配对测时 → `kprobe:entry { @start[tid] = nsecs; }` → `kretprobe:entry { @elapsed = hist(nsecs - @start[tid]); delete(@start[tid]); }` (BPF之巅 Ch5 §4)
  - `@map[key1, key2]`: 多维度 map key → `@call[comm, ustack] = count()` → 进程名×用户栈=二维计数

### 5. bpftrace 工作原理 — 编译流程
  - `bpftrace -e 'script'` → lexer → parser → AST → semantic analyzer → codegen(生成 LLVM IR) → B3 JIT → BPF → 加载+attach (eBPF开发指南 Ch6 §7)
  - `bpftrace -d` 查看 debug(不加载, 只输出 AST/code) → `bpftrace -p PID` 只匹配指定进程(减少无关事件) (BPF之巅 Ch5)
  - 安全: bpftrace 不替代验证器 → 生成的 BPF 字节码仍经内核验证器 → 写错了不会 crash (BPF之巅 Ch5 §1)

### 6. 收束
  - bpftrace = "awk for kernel" → 探针:7 种事件源 + 过滤:内核态过滤 + 动作:printf/count/hist → 一行就能做生产级tracing
  - 映射表 @ 是 bpftrace 最大魔法 → count/hist/stats/elapsed 四种聚合替代手写 Map 操作
  - bpftrace 从 "Hello World" 到 "生产工具" 的差距只有一行命令

---

### 核心悬念
**"bpftrace 选了 kprobe 探针, 但跑了一个小时后报错了 — 内核升级移除了那个函数。tracepoint 呢？USDT 呢？到底什么时候用哪个？"**

→ 引出 05-tracing-sources-selection — kprobe/uprobe/tracepoint/USDT 选择指南 + 辅助函数全景
