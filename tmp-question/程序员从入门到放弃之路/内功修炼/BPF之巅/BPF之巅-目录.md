# 《BPF 之巅：洞悉 Linux 系统和应用性能》—— 完整目录

> 作者：Brendan Gregg | 18章 + 5附录 | BCC/bpftrace全工具链 | CPU/内存/文件系统/磁盘/网络/安全/语言/容器/虚拟化

---

### 第1章 引言
1.1 BPF 和 eBPF 是什么
1.2 跟踪、嗅探、采样、剖析和可观测性
1.3 BCC、bpftrace 和 IO Visor
1.4 初识 BCC：快速上手
1.5 BPF 跟踪的能见度
1.6 动态插桩：kprobes 和 uprobes
1.7 静态插桩：tracepoint 和 USDT
1.8 初识 bpftrace：跟踪 open()
1.9 再回到 BCC：跟踪 open()

### 第2章 技术背景
2.1 图释 BPF
2.2 BPF / 扩展版 BPF（与内核模块对比/编写/指令集/API/并发控制/sysfs/BTF/CO-RE/局限性）
2.3 调用栈回溯（帧指针/调试信息/LBR/ORC/符号）
2.4 火焰图（调用栈信息/剖析/特性/变体）
2.5 事件源
2.6 kprobes（工作原理/接口/BPF和kprobes）
2.7 uprobes（工作原理/接口/BPF和uprobes/开销）
2.8 tracepoints（添加跟踪点/原理/接口/BPF/原始跟踪点）
2.9 USDT（添加探针/原理/BPF和USDT/动态USDT）
2.10 性能监控计数器 PMC（模式/PEBS/云计算）
2.11 perf_events

### 第3章 性能分析
3.1 概览（目标/分析工作/多重性能问题）
3.2 性能分析方法论（业务负载画像/下钻分析/USE方法论/检查清单法）
3.3 Linux 60 秒分析（uptime/dmesg/vmstat/mpstat/pidstat/iostat/free/sar/top）
3.4 BCC 工具检查清单（execsnoop/opensnoop/ext4slower/biolatency/biosnoop/cachestat/tcpconnect/tcpaccept/tcpretrans/runqlat/profile）

### 第4章 BCC
4.1 BCC 的组件 / 特性（内核态/用户态）
4.2 安装 BCC / BCC 的工具（重点工具/特点/多用途工具）
4.3 funccount（示例/语法/单行程序）
4.4 stackcount（示例/火焰图/残缺调用栈/单行程序）
4.5 trace（示例/语法/单行程序/结构体/调试文件描述符泄露）
4.6 argdist（语法/单行程序）
4.7 工具文档示例：opensnoop
4.8 开发 BCC 工具 / BCC 内部实现 / BCC 调试

### 第5章 bpftrace
5.1 bpftrace 的组件 / 特性（事件源/动作/一般特性/对比）
5.2 bpftrace 的安装 / 工具（重点工具/特征/单行程序）
5.3 bpftrace 编程（用法/程序结构/注释/探针格式/通配/过滤器/动作/Hello World/函数/变量/映射表/计时）
5.4 bpftrace 探针类型（tracepoint/usdt/kprobe/uprobe/software/hardware/profile/interval）
5.5 控制流（过滤器/三元操作符/if/循环展开）
5.6 运算符 / 内置变量 / 函数 / 映射表操作函数
5.7 bpftrace 内部运作 / 调试

### 第6章 CPU
6.1 背景知识 / 传统工具 / BPF 工具
6.2 execsnoop / exitsnoop / runqlat / runqlen / runqslower
6.3 cpudist / cpufreq / profile / offcputime / syscount
6.4 funccount / softirqs / hardirqs / smpcalls / llstat
6.5 BPF 单行程序（BCC/bpftrace版本）

### 第7章 内存
7.1 背景知识 / 传统工具
7.2 BPF 工具（oomkill/memleak/mmapsnoop/brkstack/shmsnoop/faults/fltrace/vmscan/drsnoop/hvmstat）
7.3 BPF 单行程序

### 第8章 文件系统
8.1 背景知识 / 传统工具（df/mount/strace/perf）
8.2 BPF 工具（opensnoop/statsnoop/syncsnoop/syncfile/cap/fmapfault/vfsstat/vfslife/fsrwstat/fileslower/filetop/writeback/filetype/cachestat/dcstat/dcsnoop/xfsslower/xfsdist/ext4dist/ext4slower/btrfsdist/btrfsslower）
8.3 BPF 单行程序

### 第9章 磁盘 I/O
9.1 背景知识 / 传统工具（iostat/blktrace）
9.2 BPF 工具（biolatency/biosnoop/bitesize/biopattern/biostacks/blkflow/ioflat/ioschedlat/scsilat/nvmesnoop）
9.3 BPF 单行程序

### 第10章 网络
10.1 背景知识 / 传统工具（ss/ip/netstat/sar/ethtool/tcpdump/proc）
10.2 BPF 工具（sockstat/sofamily/soprotocol/soconnect/socksize/sockmem/soconnlat/tcpconnect/tcpaccept/tcplife/tcptop/tcpnstat/tcpretrans/tcpsynbl/tcpwin/tcpnagle/udpconnect/gethostlatency/superping/netxlat/skbdrop/skblife/ieee80211scan）
10.3 BPF 单行程序

### 第11章 安全
11.1 背景知识（无特权BPF用户/配置BPF安全策略）
11.2 BPF 工具（execsnoop/elffsnoop/modsnoop/bashreadline/shellsnoop/ttysnoop/opensnoop/capable/setuid）
11.3 BPF 单行程序

### 第12章 编程语言
12.1 背景知识（编译型/JIT/解释型语言）
12.2 C（函数符号/调用栈/偏移量跟踪/USDT/单行程序）
12.3 Java（跟踪libjvm/jnstacks/线程/方法栈/USDT/profile/offcputime/javastat/javathreads/javacalls/javalock/javagc）
12.4 bash shell / JavaScript(Node.js) / C++ / Golang

### 第13章 应用程序
13.1 背景知识（应用程序基础/MySQL示例/BPF分析能力/策略）
13.2 BPF 工具（execsnoop/threadsnoop/profile/offcputime/syscount/mysqld_qslower/signals/killsnoop/pmlock/pmheld/naptime）

### 第14章 内核
14.1 背景知识 / 传统工具（Ftrace/perf sched/slabtop）
14.2 BPF 工具（offcputime/loadsnoop/wakeuptime/vfsstat/mutex/mheld/kmem/memleak/numamove/workq）

### 第15章 容器
15.1 背景知识 / 挑战 / 传统工具 / BPF 工具（runqlat/pidstat/blktrace）

### 第16章 虚拟机管理
16.1 背景知识 / 访客系统 / 宿主机 BPF 工具（Xen超级�用/回调/HVM退出跟踪）

### 第17章 其他 BPF 性能工具
17.1 Performance Co-Pilot(PCP) / Grafana 和 PCP
17.2 Cilium eBPF Prometheus Exporter + Grafana
17.3 eBPF 跟踪节点和容器（kubectl-trace）

### 第18章 建议、技巧和常见问题
18.1 采样频率和开销 / 49Hz或99Hz采样
18.2 调用栈缺失 / 符号缺失（ELF/函数名）/ 被误回的事件 / 反向跟踪

## 附录
- 附录 A：bpftrace 单行程序
- 附录 B：bpftrace 备忘单
- 附录 C：BCC 工具的开发
- 附录 D：CBPF
- 附录 E：BPF 指令
