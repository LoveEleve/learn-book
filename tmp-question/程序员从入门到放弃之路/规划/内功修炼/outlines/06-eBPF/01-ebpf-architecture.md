# eBPF 核心架构 — 为什么不用内核模块 + 验证器/JIT/Maps + BTF+CO-RE + 指令集 + 程序类型

> Cluster A: 7 KPs | 依赖: 无 | 读者基线: 知道内核模块概念 + 会 `dmesg`/`cat /proc`

---

### 1. eBPF 核心价值 — 为什么不写内核模块
  - 内核模块风险: 一次 bug → kernel panic 全机崩溃, 安全审计无边界, 发布流程需重编译内核 (BPF之巅 Ch1 §1-3)
  - eBPF 安全容器: 加载前先验证(验证器)→ JIT 编译→ sandbox 运行→ 事件驱动执行→ 零内核崩溃 (深入理解eBPF Ch1 §1-3)
  - 动态插桩: 无需重启/重编译/重部署 → `bpftrace -e 'kprobe:do_sys_open { printf("%s\n", str(arg1)); }'` 一行搞定 (BPF之巅 Ch1 §4)
  - 应用场景全景: 网络(XDP)/安全(LSM)/可观测性(perf)/追踪(trace)→ 同一套技术内核→用户态全栈覆盖 (eBPF开发指南 Ch1 §4)

### 2. eBPF 架构 — 验证器 + JIT + Maps + sysfs
  - 加载流程: 用户态 `bpf(BPF_PROG_LOAD)` → 验证器(depth-first DAG 无环检查+寄存器状态追踪+边界检查) → JIT 编译(x86/arm64) → attach 到 hook 点 (BPF之巅 Ch2 §1-3)
  - 验证器: 拒绝 unsafe 指针访问/无限循环/未初始化寄存器 → 最大指令数(初始 4096→现在 1M) → 等价于"内核里的 Rust borrow checker" (深入理解eBPF Ch2 §1)
  - JIT 编译器: BPF bytecode → x86-64 native → 每条 BPF 指令 1:1 或 1:N 映射 → `bpftool prog show` 查看 JIT 状态 (BPF之巅 Ch2 §3)
  - Maps: kernel-user 共享内存 → Hash/Array/PerCPU/PerfEventArray/RingBuf → `bpftool map show` (详见 02) (eBPF开发指南 Ch9 §1-2)
  - sysfs/bpffs 生命周期: `mount -t bpf bpffs /sys/fs/bpf` → pin map/prog → 跨进程共享 → 进程退出后 map 不丢失 (BPF之巅 Ch2 §2)

### 3. BTF + CO-RE — 一次编译, 随处运行
  - BTF (BPF Type Format): DWARF 精简替代 → 记录 struct layout/typedef/func prototype → `/sys/kernel/btf/vmlinux` (eBPF开发指南 Ch8 §1-3)
  - CO-RE (Compile Once Run Everywhere): BTF 让编译时不知道内核版本也能运行 → 内核字段偏移在加载时从 BTF 重定位 → `bpf_core_read()` 读字段 (深入理解eBPF Ch3 §1-2)
  - BTF Hub: 所有发行版的 BTF 信息仓库 → `bpftool btf dump file /sys/kernel/btf/vmlinux format c` 查看 struct (eBPF开发指南 Ch8 §4)
  - 最小化 BTF: 只包含程序引用的类型 → `pahole -J <.o>` → CO-RE 生成的 .bpf.o 自带 (eBPF开发指南 Ch8 §5)

### 4. eBPF 指令集 — 11 寄存器 + 字节码 + 反汇编
  - 11 个 64-bit 寄存器: R0(返回值) R1-R5(参数/scratch) R6-R9(callee-saved) R10(只读帧指针) (BPF之巅 Ch2 §1-2)
  - 指令编码: 64-bit 固定长度 → opcode(8bit) + dst(4bit) + src(4bit) + offset(16bit) + immediate(32bit) → 总共 8 字节 (深入理解eBPF Ch2 §1-2)
  - 指令类型: ALU(64/32-bit) + load/store(变长访存) + branch(call/exit/jump) → `bpftool prog dump xlated id <N>` 反汇编为伪C (eBPF开发指南 Ch4 §4-5)
  - JIT 后: `bpftool prog dump jited id <N>` → 看对应 x86 mov/jmp → 验证器 + JIT = 安全 + 快的组合

### 5. 程序类型分类 — XDP / TC / tracing / socket / cgroup
  - XDP (eXpress Data Path): 网卡驱动层提前执行 → 比内核网络栈早 → 返回 XDP_PASS/DROP/TX/ABORTED/REDIRECT → DDoS 防御 (深入理解eBPF Ch2 §6)
  - TC (Traffic Control): 内核网络栈 ingress/egress hook → cls_bpf 分类器 → 流量控制/负载均衡 (BPF之巅 Ch2 §6)
  - Tracing: kprobe/kretprobe(内核函数) + uprobe/uretprobe(用户态函数) + tracepoint(内核静态打点) → perf_event 驱动 (详见 05) (BPF之巅 Ch2 §6-10)
  - Socket: `BPF_PROG_TYPE_SOCKET_FILTER` → tcpdump 底层引擎 → sockmap 绕过内核 TCP/IP 栈 → Cilium Sidecar-less (BPF之巅 Ch10)
  - Cgroup: `BPF_PROG_TYPE_CGROUP_SOCK` → per-cgroup 网络策略 → Kubernetes NetworkPolicy 底层实现 (BPF之巅 Ch4)

### 6. 收束
  - eBPF 的三个核心竞争: 安全(验证器保证无 crash) + 快(JIT native) + 动态(无重启热插拔) — 任何一项单拎出来都能赢内核模块
  - BTF+CO-RE 是 eBPF 工业化的转折点 — 之前必须每内核版本编译, 之后一次编译全内核通用
  - 11 寄存器 + 固定长度指令 = 设计上就是为"在受限环境中安全运行" — 不是事后加的沙箱

---

### 核心悬念
**"验证器保证了安全, JIT 保证了速度, BTF 保证了兼容 — 但数据怎么从内核传出来？"**

→ 引出 02-eBPF Maps 全家桶 + ringbuf + perf_event 数据管道
