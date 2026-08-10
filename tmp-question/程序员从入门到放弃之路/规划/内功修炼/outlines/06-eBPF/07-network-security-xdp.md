# 网络 + 安全 + XDP — tcpconnect/tcplife/tcpretrans/superping + XDP 网卡直通 + opensnoop/bashreadline + LSM eBPF

> Cluster C: 4 KPs | 依赖: 01 架构 + 04 bpftrace + 05 追踪源 | 读者基线: 会 tcpdump/ss 等传统网络工具 + 理解 TCP 三次握手/四次挥手

---

### 1. 网络观测四件套 — 连接的生死全貌
  - tcpconnect: 主动连接追踪 → `tcpconnect-bpfcc -t` → 谁连了谁(`kprobe:tcp_v4_connect`) → `bpftrace -e 'kprobe:tcp_v4_connect { printf("%s -> %s\n", comm, kstack()); }'` → 发现异常连接/蜜罐 (BPF之巅 Ch10 §2)
  - tcplife: TCP 连接生命周期 → `tcplife-bpfcc -t` → PID/进程/LADDR/LPORT/DADDR/DPORT/DURATION(ms)/TX/RX/RETRANS → 连接的完整传记 (BPF之巅 Ch10 §2)
  - tcpretrans: TCP 重传 → `tcpretrans-bpfcc` → 类型(Lost/SackAll/LostRetransmit) → 重传 IP 元组 → 网络延时/丢包/拥塞的根因定位 (BPF之巅 Ch10 §3)
  - superping: ICMP 回显 → `bpftrace -e 'kprobe:icmp_rcv { printf("ICMP from %s\n", str(arg0)); }'` → 不限于 ping 回复, 所有 ICMP 包类型(不可达/超时/源抑制) (BPF之巅 Ch10 §3)

### 2. XDP (eXpress Data Path) — 网卡直通数据面
  - 原理: BPF 程序在网卡驱动 NAPI 层执行 → 比内核协议栈(ip_rcv/tcp_v4_rcv)早 → 返回 `XDP_DROP/PASS/TX/ABORTED/REDIRECT` → 硬中断处理中运行 (深入理解eBPF Ch2 §6)
  - 内核路径: `网卡 RX ring → NAPI poll → XDP hook → XDP_PASS → netif_receive_skb → 内核网络栈` → XDP_DROP 在第一步直接扔掉 (BPF之巅 Ch15 §4)
  - DDoS 防护: `if (is_bad_ip(iph->saddr) || rate_limit()) return XDP_DROP` → 零 CPU 来百万 pps 黑名单 → Cloudflare Gatebot 用 XDP 挡 10M+ pps (BPF之巅 Ch11 §1)
  - TC 流量控制: 内核网络栈层的 BPF → `cls_bpf` 分类器 → ingress/egress → 比 XDP 晚但能修改数据包/做负载均衡 (深入理解eBPF Ch5 §2)
  - Cilium 数据面: XDP+TC+BGP → Service Mesh Sidecar-less → 容器网络(ip 分配/路由/NAT)全在 BPF → 比 iptables 快 10x (BPF之巅 Ch15 §4)
  - sockmap: BPF 程序绕过内核 TCP/IP 栈 → SkData redirect → 同主机两个 socket 通信零 copy 直通 → Envoy 同 Pod 通信加速 (深入理解eBPF Ch5 §2)

### 3. 安全观测 — 文件访问 + 命令记录 + 能力检查
  - opensnoop: 文件访问跟踪 → `opensnoop-bpfcc -t` → PID/PATH/FLAGS → 谁在打开文件 → 找恶意进程/配置错误 → `bpftrace -e 'tracepoint:syscalls:sys_enter_openat { printf("%s pid=%d\n", str(args->filename), pid); }'` (BPF之巅 Ch11 §2)
  - bashreadline: 即时命令记录 → `bpftrace -e 'uretprobe:/bin/bash:readline { printf("%s\n", str(retval)); }'` → 实时交互式命令(不依赖 .bash_history) (BPF之巅 Ch11 §2)
  - execsnoop 安全版: 进程执行跟踪 → `bpftrace -e 'tracepoint:syscalls:sys_enter_execve { printf("exec %s %s\n", comm, str(args->filename)); }'` → 进程创建链(比 ps 快得多) (BPF之巅 Ch11 §2)
  - capable: 能力位检查 → `bpftrace -e 'kprobe:cap_capable { @[comm] = count(); }'` → 谁在请求 CAP_SYS_ADMIN/CAP_NET_RAW → 权限滥用检测 (BPF之巅 Ch11 §2)
  - signals: 信号跟踪 → `bpftrace -e 'tracepoint:signal:signal_generate { printf("sig=%d from=%s to=%d\n", args->sig, comm, args->pid); }'` → 谁向谁发信号 (BPF之巅 Ch13 §2)

### 4. LSM eBPF + 安全综合策略
  - LSM (Linux Security Module) BPF: `BPF_PROG_TYPE_LSM` → hook LSM hooks → 在内核安全检查链中插入自定义逻辑 → 全内核覆盖 (深入理解eBPF Ch9 §7)
  - 审计订阅: signal_generate/security_bprm_check → `bpftrace -e 'tracepoint:syscalls:sys_enter_kill { ... }'` → auditd 替代 (深入理解eBPF Ch9 §4)
  - 安全策略: 检测(opensnoop 监控) → 审计(bashreadline 记录) → 预防(LSM BPF 截断) → 三阶段递进 (BPF之巅 Ch11)
  - Cilium 安全: NetworkPolicy + L7 策略(HTTP/gRPC) → per-pod identity → 比 kube-proxy iptables 更快更细粒度 (BPF之巅 Ch15)

### 5. 综合对比 — CPU/内存/网络/安全观测体系
  - 同一技术栈四种视角: 同一套 kprobe/tracepoint 底座 → CPU(调度/系统调用) + 内存(分配/缺页/缓存) + 网络(连接/重传/XDP) + 安全(文件/命令/权限) (BPF之巅 Ch6/Ch7/Ch10/Ch11)
  - 工具链汇总: execsnoop 跨 CPU 和安全 → opensnoop 跨内存和安全 → bpftrace 一行全部搞定 → BCC/libbpf 生产部署
  - 书级覆盖: B1 Ch6-11/Ch15 (10 子系统全) + B2 Ch6-9 (6 子系统) + B3 Ch12 (性能分析策略) → 三书互补形成 360° 可观测

### 6. 收束
  - tcpconnect→tcplife→tcpretrans 三件套覆盖 TCP 连接的"建立→存活→故障"全生命周期 — 比 tcpdump 更聚焦(只拿元组不拿 payload)
  - XDP 是 eBPF 最快的 hook(网卡层面) → DDoS 防护/负载均衡/服务网格 bypas iptables → Cilium 是生产实践
  - 安全观测四合一(opensnoop/bashreadline/execsnoop/capable) = 没有 .bash_history 的实时审计 — 比 auditd 轻 1000x

---

### 核心悬念
**"eBPF 的七个模块都讲完了 — 但作为读者, 你真的能把它们串起来吗？哪些应该先讲？哪些必须前置？"**

→ 引出 07-系统编程 — POSIX API、信号、进程间通信的系统编程全景
