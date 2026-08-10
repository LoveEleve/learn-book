# CPU + 内存可观测 — execsnoop/runqlat/cpudist/profile/syscount + oomkill/memleak/cachestat/vmscan

> Cluster C: 4 KPs | 依赖: 01 架构 + 04 bpftrace + 05 追踪源 | 读者基线: 会 bpftrace 基础 + 理解 USE 方法论(CPU利用率/饱和度)

---

### 1. CPU 观测全景 — 五维工具矩阵
  - 维度一 execsnoop: 跟踪 exec() → 谁在创建新进程 → `execsnoop-bpfcc -t` → 发现异常进程启动 → 对应 `bpftrace -e 'tracepoint:syscalls:sys_enter_execve { printf("%s\n", str(args->filename)); }'` (BPF之巅 Ch6 §2)
  - 维度二 runqlat: 调度延迟分布 → `runqlat-bpfcc -m 10` → 线程在 run queue 等多久 → 直方图看 P99 延迟 → 调度瓶颈 (BPF之巅 Ch6 §3)
  - 维度三 cpudist: CPU 时间分布 → `cpudist-bpfcc -P 10` → 每个线程在 CPU 上连续执行多久 → 长片=CPU bound, 短片=I/O bound (BPF之巅 Ch6 §4)
  - 维度四 profile: CPU 火焰图 → `profile-bpfcc -F 99 10 > out.folded` → `flamegraph.pl` → 用 `kstack`/`ustack` 找函数热点 (BPF之巔 Ch6 §5)
  - 维度五 syscount: 系统调用计数 → `syscount-bpfcc -p PID 1` → 每进程哪种 syscall 最频繁 → 找 I/O 密集型或锁密集型 (BPF之巅 Ch6 §6)

### 2. CPU 观测进阶 — off-CPU + 硬软中断 + 唤醒链
  - offcputime: off-CPU(阻塞状态)火焰图 → `offcputime-bpfcc -K -f 10` → I/O wait/锁等待/睡眠 → CPU 剖析找不到的瓶颈 (BPF之巅 Ch6 §5)
  - hardirq/softirq: 中断观测 → `hardirqs-bpfcc -d 10` → `softirqs-bpfcc -d 10` → 网卡中断/定时器中断分布 (深入理解eBPF Ch8 §3)
  - wakeuptime: 线程被阻塞多久 → `wakeuptime-bpfcc -p PID` → 谁唤醒谁→唤醒链→找锁持有者 (BPF之巅 Ch14 §2)
  - B1 Ch6 综合策略: USE 找瓶颈资源 → runqlat+profile 定 CPU bottleneck → offcputime 定 I/O 阻塞 → syscount 定性 → execsnoop 找异常

### 3. 内存观测 — oomkill + memleak + faults
  - oomkill: OOM killer 事件 → `bpftrace -e 'kprobe:oom_kill_process { printf("killed pid=%d comm=%s\n", pid, comm); }'` → OOM 时谁被杀了+为什么 (BPF之巅 Ch7 §1)
  - memleak: 内存泄漏检测 → `memleak-bpfcc -p PID -a` → 跟踪 malloc/free 未配对 → 输出调用栈+泄漏字节 (BPF之巅 Ch7 §2, 深入理解eBPF Ch6 §3)
  - faults: 页面错误计数 → `bpftrace -e 'software:page-faults:1 { @[pid, comm] = count(); }'` → 分 major/minor fault → 大内存分配触发的缺页 (BPF之巅 Ch7 §2)
  - mmapsnoop: 映射文件 → `bpftrace -e 'tracepoint:syscalls:sys_enter_mmap { printf("%s\n", str(args->filename)); }'` → 谁在映射什么文件 (BPF之巅 Ch7 §1)

### 4. 内存观测进阶 — cachestat + vmscan + memleak 深度
  - cachestat: 页缓存命中率 → `cachestat-bpfcc 1 10` → HITS/MISSES/DIRTY/BUFFERED ratio → 缓存太小则增加内存, 命中率低则无用 (BPF之巅 Ch8 §3)
  - cachetop: 每个文件的缓存命中率 → `cachetop-bpfcc 10` → 谁占用最多页缓存+命中率 → 热点文件 (BPF之巅 Ch8 §3)
  - vmscan: 内存回收活动 → `bpftrace -e 'tracepoint:vmscan:mm_vmscan_kswapd_wake { printf("kswapd woke\n"); }'` → 系统接近内存压力 (BPF之巅 Ch7 §2)
  - 综合策略: 先用 cachestat 看缓存效果 → mmapsnoop 看热点文件 → faults 看缺页 → memleak 追泄漏 → oomkill 最后防线

### 5. BCC 工具速查 — bpftrace 等效命令对照
  - `execsnoop-bpfcc` ≡ `bpftrace -e 'tracepoint:syscalls:sys_enter_execve { printf("%s\n", str(args->filename)); }'` (BPF之巅 Ch6)
  - `runqlat-bpfcc` ≡ `bpftrace -e 'kprobe:finish_task_switch { ... }'` (CPU scheduler 插桩, 需要手工计算) (BPF之巅 Ch6)
  - `profile-bpfcc` ≡ `bpftrace -e 'profile:hz:99 { @[kstack, ustack] = count(); }'` (BPF之巅 Ch6)
  - `memleak-bpfcc` ≡ libbpf 程序 hooking malloc/free/realloc → 维护分配表 → `interval:30 { print(@alloc_unfreed); }` 近似 (深入理解eBPF Ch6 §3)
  - **经验**: bpftrace 快速原型 → 如果长期运行/稳定生产用 BCC 专用工具 → 最深层用 libbpf 自写

### 6. 文件系统 — opensnoop + filetop + ext4slower
  - opensnoop: 追踪所有 open() 调用 → 发现配置文件/日志/临时文件访问模式 → `opensnoop-bpfcc -T` 含时间戳 → `bpftrace -e 'tracepoint:syscalls:sys_enter_openat { printf("%s %s\n", comm, str(args->filename)); }'` (B1 Ch8 §2, BPF之巅 Ch8 §2)
  - filetop: 按文件聚合并排行显示读写量/次数 → `filetop-bpfcc -C 10` → 定位热点文件 → 哪个进程在读什么文件, 读了多少 (BPF之巅 Ch8 §2)
  - ext4slower/xfsslower: 追踪慢文件操作(>10ms) → `ext4slower-bpfcc -j 10` → 按延迟/进程/操作类型分组 → 慢在 write/read/open/fsync (BPF之巅 Ch8 §3)
  - 场景: 应用突然变慢 → filetop 发现某索引文件 700MB/s 随机读 → ext4slower 确认 IO 延迟 200ms+ → 罪魁是文件而非代码

### 7. 磁盘 IO — biolatency + biosnoop + biotop
  - biolatency: I/O 延迟分布直方图 → `biolatency-bpfcc -m 10` → P50/P99/最大延迟 → 按磁盘分别统计 → `biolatency-bpfcc -D` 分磁盘 (BPF之巅 Ch9 §2)
  - biosnoop: 追踪每个 block I/O 请求 → `biosnoop-bpfcc` → 进程/LBA 扇区/大小/延迟/RW → `biosnoop-bpfcc -Q` 含排队时间 (BPF之巅 Ch9 §2)
  - biotop: 按进程聚合并排行 → `biotop-bpfcc -C 5` → 找出哪个进程在做 I/O → 类似 `top` 但显示的是磁盘吞吐而非 CPU (BPF之巅 Ch9 §2)
  - 场景: CPU iowait 70% → biotop 发现 mysqld 占 95% I/O → biolatency 确认 P99=800ms → biosnoop 定位到具体 SQL → 发现缺少索引的全表扫描

### 8. 调度观测 — runqlat + offcputime + wakeuptime
  - runqlat: 线程在 runqueue 上的等待时间直方图 → `runqlat-bpfcc -m` → 只统计线程"想跑但没 CPU"的时间 → P50 vs P99 差距大 = 调度不公平 (BPF之巅 Ch6 §1)
  - offcputime: 线程离开 CPU 的等待时间 → `offcputime-bpfcc -K -f 10` → 按调用栈聚合 → 包含睡眠/锁/I/O 全部原因 → 不是 CPU 瓶颈而是 off-CPU 瓶颈 (BPF之巅 Ch14 §2)
  - wakeuptime: 新线程创建到被第一个任务唤醒的延迟 → `wakeuptime-bpfcc -p PID` → 容器预热/进程 fork 的性能分析 → 谁在唤醒谁→唤醒链 (BPF之巅 Ch14 §2)
  - 综合: runqlat 看"拿到 CPU 要等多久" → offcputime 看"离开 CPU 后在等什么" → wakeuptime 看"新线程等多久才能开始干活"

### 9. 收束
  - CPU 五维工具(execsnoop/runqlat/cpudist/profile/syscount) 覆盖"创建了什么→等多久→执行多久→谁在跑→什么 syscall"的全通路
  - 文件系统+磁盘 IO 补齐"数据落盘前在哪→落盘多快→谁在写"的存储观测链
  - 调度观测(runqlat/offcputime/wakeuptime) 补齐"线程在哪等/等什么/等多久"的延迟分析链
  - 内存观测从"系统级(oomkill/页面回收)"到"应用级(memleak/faults)"到"效果级(cachestat)"三层递进
  - bpftrace 写原型 → BCC tool 跑生产 → libbpf 定制化 — 三阶提升

---

### 核心悬念
**"CPU 和内存搞清楚了 — 网络呢？DDoS 攻击了, 连到哪了？包在哪丢了？XDP 能在网卡层直接挡吗？"**

→ 引出 07-network-security-xdp — tcpconnect/tcplife/tcpretrans/superping + XDP 网卡直通 + opensnoop/bashreadline 安全观测 + LSM eBPF
