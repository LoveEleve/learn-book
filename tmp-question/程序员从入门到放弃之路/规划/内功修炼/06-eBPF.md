# 06 — eBPF — 知识点规划

> 方法论: knowledge-planning/methodology/01-03 | TOC-only, 04跳过
> N=3本 | P1=≥2(>1.5) | P3=1(无P2 for N=3) | 标准: 8.5-9/10

---

## 贡献书籍

| # | 书名 | 特点 |
|---|------|------|
| B1 | BPF之巅 (Brendan Gregg) | 工具全景, BCC/bpftrace全工具链 |
| B2 | 深入理解eBPF与可观测性 | 6子系统实践 |
| B3 | eBPF开发指南 | 开发入门, libbpf/Golang/CO-RE |

---

## 01 提取 — 逐书映射

### `[01 #1/3]` B1 — BPF之巅

| Original Chapter | Inferred Knowledge Point | Confidence |
|-----------------|------------------------|------------|
| Ch1 §1-9 | eBPF核心价值(动态插桩vs静态/内核模块对比)/BCC+bpftrace初识 | High |
| Ch2 §1-3 | eBPF架构(验证器/JIT/Maps/sysfs/BTF/CO-RE) | High |
| Ch2 §4-6 | 调用栈回溯(帧指针/LBR/ORC/符号)+火焰图 | High |
| Ch2 §6-10 | kprobes/uprobes/tracepoints/USDT事件源/PMC/perf_events | High |
| Ch3 §1-5 | 性能方法论(USE/负载画像/下钻)/60秒分析/BCC检查清单 | High |
| Ch4 §1-4 | BCC funccount/stackcount/trace/argdist | High |
| Ch4 §5-8 | BCC内部实现(内核态/用户态)/调试/文档 | High |
| Ch5 §1-4 | bpftrace语法/探针格式/过滤器/动作/Hello World/函数/变量/映射表 | High |
| Ch5 §5-7 | bpftrace探针类型(tracepoint/usdt/kprobe/uprobe/software/hardware)/控制流/运算符 | High |
| Ch6 §1-5 | CPU性能(execsnoop/runqlat/cpudist/profile/offcputime/syscount) | Medium |
| Ch7 §1-2 | 内存(oomkill/memleak/mmapsnoop/faults/vmscan) | Medium |
| Ch8 §2-3 | 文件系统(opensnoop/filetop/cachestat/ext4slower) | Medium |
| Ch9 §2-3 | 磁盘IO(biolatency/biosnoop/biotop/blkflow) | Medium |
| Ch10 §2-3 | 网络(tcpconnect/tcplife/tcpretrans/superping) | Medium |
| Ch11 §2-3 | 安全(execsnoop/bashreadline/opensnoop/capable) | Medium |
| Ch12 §2-5 | 编程语言探针(C/Java/Go/Node) | Medium |
| Ch13 §1-2 | 应用程序观测(MySQL剖析/mysqld_qslower/signals/killsnoop/线程剖析) | Medium |
| Ch14 §2 | 内核(offcputime/wakeuptime/mutex/vfsstat) | Medium |
| Ch17 §1-3 | 容器eBPF(PCP/Grafana/Cilium/kubectl-trace) [修正: 原误标Ch15] | Medium |
| Ch16 | 虚拟机管理(Xen超级调用/HVM退出跟踪) | Medium |

`[01 #1/3 done]` 20 KPs。

---

### `[01 #2/3]` B2 — 深入理解eBPF与可观测性

| Original Chapter | Inferred Knowledge Point | Confidence |
|-----------------|------------------------|------------|
| Ch1 §1-3 | eBPF发展史/cBPF区别/基础架构(加载/JIT编译/挂载执行)/内核模块对比 | High |
| Ch2 §1-2 | eBPF指令架构(指令集/C语言/汇编/字节码编写) | High |
| Ch2 §3-4 | eBPF系统调用(函数原型/类型/数据结构)/辅助函数(设计/实现) | High |
| Ch2 §5 | 跟踪诊断类eBPF程序(kprobe/kretprobe/uprobe/tracepoint/perf事件) | High |
| Ch2 §6 | XDP网络程序(基本原理/应用场景/内核解析) | High |
| Ch3 §1-2 | libbpf开发(BPF Type Format/CO-RE) | High |
| Ch3 §3-6 | BCC/bpftrace/eunomia-bpf/Coolbpf 开发框架对比 | High |
| Ch4 §1-6 | 应用观测(uprobe/USDT观测Nginx/Java GC/MySQL慢查询/TLS明文/Go协程) | Medium |
| Ch5 §1-2 | 网络观测(HTTP流量/TCP连接+RTT/XDP包处理/流量控制/sockmap/Virtio) | Medium |
| Ch6 §1-5 | 内存观测(页面错误/cachetop/memleak BPF实现) | Medium |
| Ch7 §1-5 | IO观测(iofstat/iolatency/IO hang检测/biopattern BPF实现) | Medium |
| Ch8 §1-5 | 调度观测(关中断检测/调度延迟/硬软中断/持续性追踪) | Medium |
| Ch9 §1-7 | 安全观测(文件访问/信号/退出/审计/LSM安全检测) | Medium |

`[01 #2/3 done]` 13 KPs。

---

### `[01 #3/3]` B3 — eBPF开发指南

| Original Chapter | Inferred Knowledge Point | Confidence |
|-----------------|------------------------|------------|
| Ch1 §1-4 | eBPF定义/发展/应用领域/BCC-bpftrace-libbpf初识 | High |
| Ch3 §1-5 | Linux动态追踪工具全景(strace/ltrace/SystemTap/LTTng/trace-cmd/perf/ftrace/kprobe/uprobe/tracepoint) | High |
| Ch4 §1-3 | eBPF程序入门(加载字节码/BCC版/C语言版/BPF系统调用) | High |
| Ch4 §4-6 | eBPF指令集(寄存器/编码/反汇编/验证器)/libbpf+skel重写 | High |
| Ch5 §1-4 | BCC Python API + libbcc开发(opensnoop解读) | High |
| Ch6 §1-7 | bpftrace语法/探针类型(kprobe/uprobe/tracepoint/USDT/定时器/软硬件事件)/变量/函数/工作原理 | High |
| Ch7 §1-3 | Golang eBPF(libbpfgo/Cilium ebpf-go/bpf2go) | High |
| Ch8 §1-5 | BTF详解(数据结构/内核API/生成)+CO-RE(BTHub/最小化BTF/读取内核字段) | High |
| Ch9 §1-4 | eBPF Maps(API/创建/查询/遍历/删除/持久化) | High |
| Ch9 §5-6 | perf事件(内核态写入/用户态读取)+ringbuf环形缓冲 | High |
| Ch10 §1-7 | kprobe/uprobe/USDT/bashreadline挂载点 | High |
| Ch11 §1-4 | eBPF内核辅助方法(helper): 如何查阅/实现原理/分类(网络/数据处理/跟踪/系统功能)/常用helper(bpf_map_update_elem/bpf_ktime_get_ns/bpf_probe_read) | High |
| Ch12 §1-5 | 性能分析(CPU/内存/磁盘/网络 eBPF工具+分析策略) | Medium |
| Ch13 §1-5 | 实战(动态分析/网络安全/应用运维) | Medium |

`[01 #3/3 done]` 13 KPs。

---

## 01 提取完成 — 3本书总计

| Book | KPs |
|------|:---:|
| B1 BPF之巅 | 20 |
| B2 eBPF与可观测性 | 13 |
| B3 eBPF开发指南 | 13 |
| **Raw total** | **46** |

---

## 01 聚合 — 跨书分组

N=3, P1=≥2(>1.5), P3=1。无P2

### P1 — Consensus (≥2/3)

| Knowledge Point | B1 | B2 | B3 |
|----------------|:--:|:--:|:--:|
| eBPF核心价值+架构(验证器/JIT/Maps) | ✅ | ✅ | ✅ |
| kprobes/uprobes/tracepoints 事件源 | ✅ | ✅ | ✅ |
| BTF+CO-RE | ✅ | ✅ | ✅ |
| BCC工具链 | ✅ | ✅ | ✅ |
| bpftrace语法+编程 | ✅ | ✅ | ✅ |
| eBPF Maps(创建/查询/ringbuf) | ✅ | — | ✅ |
| libbpf+skel开发流程 | ✅ | ✅ | ✅ |
| eBPF指令集/字节码/验证器 | ✅ | ✅ | ✅ |
| perf_event+ringbuf数据交换 | ✅ | ✅ | ✅ |
| CPU性能分析(ececsnoop/runqlat) | ✅ | ✅ | — |
| 内存分析(oomkill/memleak) | ✅ | ✅ | — |
| 网络观测(tcpconnect/tcplife) | ✅ | ✅ | — |
| 安全观测(opensnoop/bashreadline) | ✅ | ✅ | — |
| XDP网络程序 | ✅ | ✅ | — |
| 系统调用/辅助函数 | — | ✅ | ✅ |

**P1: 15**

### P3 — Isolated (1/3 books)

B1独家: 调用栈回溯/火焰图/60秒/BCC内部实现/文件系统工具/磁盘IO/编程语言探针/容器/Cilium → 🟡
B2独家: 程序类型设计/XDP原理/应用观测(Nginx/Java/MySQL/Go/TLS)/IO观测/调度观测/持续性追踪/LSM → 🟡  
B3独家: 动态追踪全景/Golang eBPF/eBPF Maps持久化/bashreadline/实战 → 🟡

---

## 02 深度分类

### 🔴 Deep (11项)

| Knowledge Point | 01 Pri | 为什么🔴 |
|----------------|:------:|----------|
| eBPF核心价值+内核进化 | P1 | 为什么不用内核模块？为什么安全？ |
| eBPF架构(验证器/JIT/Maps/sysfs) | P1 | 8.5分必须理解 |
| kprobes/uprobes/tracepoints 插桩分类 | P1 | 三个事件源各不同 |
| BTF+CO-RE(一次编译随处运行) | P1 | 核心技术 |
| eBPF Maps(Hash/Array/PerCPU/RingBuf) | P1 | 内核↔用户态通道 |
| perf_event+ringbuf数据管道 | P1 | BPF→用户态命脉 |
| eBPF验证器+指令健壮性 | P1 | 内核安全容器 |
| libbpf+CO-RE+skel开发流程 | P1 | 真正BPF写法 |
| bpftrace编程(探针类型/控制流) | P1 | 快速开发 |
| CPU/内存/网络/安全 4子系统观测 | P1 | 同一插桩不同维度 |
| XDP网络(网卡直通/DDoS) | P1 | 高性能数据路径 |

### 🟡 Working (12项)

| Knowledge Point | 01 Pri | 说明 |
|----------------|:------:|------|
| BCC工具链(bpftrace🔴, BCC配套) | P1 | 工具集 |
| eBPF程序类型设计 | P3 | 扩展 |
| 文件系统+磁盘IO+调度观测 | P1 | →对应Topic |
| 安全观测(process/file/LSM) | P3 | 辅助 |
| Golang eBPF(libbpfgo/ebpf-go) | P3 | ✅ |
| 动态追踪工具全景对比 | P3 | 辅助 |
| ftrace集成点 | P3 | 辅助 |
| 容器/K8s eBPF(kubectl-trace) | P3 | →K8s |
| Cilium+Grafana可视化 | P3 | 辅助 |
| 追踪源对比(uprobes vs USDT vs tracepoint) | P3 | 底层 |
| 调用栈回溯(LBR/ORC) | P3 | 技术细节 |
| eBPF安全LSM | P3 | 辅助 |

### 🟢 Surface (5项)

PMC/PEBS(→perf)/火焰图(→性能)/per-language探针/应用观测高级案例

---

## 03 聚类 — 3组

```
A(eBPF核心:7) → B(开发模式:5) → C(子系统观测:4)
```

### A: eBPF核心 (7项,零依赖)
核心价值→架构(验证/JIT/Maps)→指令集→BTF/CO-RE→Maps→ringbuf→程序类型定义

### B: 开发模式 (5项,依赖A)
libbpf+skel→bpftrace→BCC工具链→kprobe/uprobe/tracepoint选择→helper API

### C: 子系统观测 (4项,依赖A+B)
CPU观测→内存观测→网络+XDP→安全观测

---

| 深度 | 计数 |
|------|:---:|
| 🔴 Deep | 11 |
| 🟡 Working | 12 |
| 🟢 Surface | 5 |
| **Total** | **28** |

教学: A→B→C