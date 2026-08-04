# 《eBPF 开发指南：从原理到应用》—— 完整目录

> 13章 | eBPF概述→开发环境→动态追踪→入门→BCC/bpftrace/Golang→BTF/CO-RE→数据交换→挂载点→辅助方法→性能分析→实战

---

### 第1章 eBPF 概述
1.1 eBPF 是什么 / 发展历史 / 应用领域
1.2 eBPF 如何运行
1.3 eBPF 相关工具与库（BCC/bpftrace/libbpf）
1.4 初识 eBPF 程序

### 第2章 eBPF 开发环境准备
2.1 Linux 发行版本的选择 / 编程语言的选择
2.2 安装和配置 Linux 操作系统环境（Windows/macOS）
2.3 二进制方式安装 eBPF 开发工具与库（BCC/bpftrace/libbpf）
2.4 源码方式编译安装

### 第3章 Linux 动态追踪技术
3.1 Linux 动态追踪系统前端工具和库（strace/ltrace/DTrace/SystemTap/LTTng/trace-cmd/perf）
3.2 数据采集机制（ptrace/perf_event_open/BPF系统调用）
3.3 跟踪文件系统（挂载/目录/跟踪器/选项/环形缓冲区）
3.4 Linux 内核数据源（ftrace/kprobe/kretprobe/uprobe/uretprobe/tracepoint）
3.5 eBPF 数据采集点

### 第4章 eBPF 程序入门
4.1 第一个 eBPF 程序（BCC/C语言版本）
4.2 eBPF 程序功能解读（加载字节码/BPF系统调用/attach_kprobe/perf_event_open）
4.3 eBPF 授权协议
4.4 eBPF 指令集（寄存器/指令编码/指令列表/指令分析/BCC中指令生成/反汇编/验证机制）
4.5 libbpf（功能/接口/案例程序）
4.6 重写 eBPF 程序（编译/内核态/生成skel头文件/用户态）

### 第5章 BCC
5.1 BCC 工具集（tools/libbpf-tools）
5.2 BCC 常用的工具（opensnoop/execsnoop/exitsnoop）
5.3 使用 Python 开发 eBPF 程序（BPF API/opensnoop程序解读）
5.4 使用 libbcc 开发 eBPF 程序

### 第6章 bpftrace
6.1 bpftrace 的功能和特性（工程结构/探针类型/特性/主程序）
6.2 bpftrace 的脚本语法
6.3 探针类型（kprobe/uprobe/tracepoint/USDT/定时器/软硬件事件/内存监视点/kfunc/迭代器/开始结束块）
6.4 bpftrace 变量（内置/基础/关联数组）
6.5 bpftrace 函数（基础/映射表）
6.6 bpftrace 的工作原理
6.7 bpftrace 工具集

### 第7章 使用 Golang 开发 eBPF 程序
7.1 Go 语言开发环境介绍
7.2 使用 libbpfgo 开发 eBPF 程序
7.3 Cilium 与 ebpf-go（开发环境/bpf2go/bpftool）

### 第8章 BTF 与 CO-RE
8.1 什么是 CO-RE
8.2 BTF 详解（数据结构/内核API/生成BTF/二进制BTF/辅助函数）
8.3 对 BTF 的处理（编译器处理/libbpf处理）
8.4 读取内核结构体字段（直接访问/bpf_get_current_task/BPF_CORE_READ/其他宏）
8.5 低版本系统如何支持 BTF（BTHub/最小化BTF/BTF-App）

### 第9章 eBPF 程序的数据交换
9.1 eBPF 程序的数据结构（eBPF map/数据类型）
9.2 map 操作接口（API/创建/添加/查询/遍历/删除/bpftool操作map）
9.3 map 在内核中的实现（创建/生命周期/持久化）
9.4 ftrace 的 eBPF 数据交换接口（bpf_trace_printk/bpf_printk宏/日志输出格式）
9.5 perf 事件（map类型/内核态写入/用户态读取/BCC中处理）
9.6 环形缓冲区 ringbuf（map类型/内核态/用户态/完整实例）

### 第10章 eBPF 程序类型与挂载点
10.1 常见的 eBPF 程序类型（跟踪分析类/网络类）
10.2 eBPF 程序挂载点
10.3 函数跟踪技术（内核态/用户态）
10.4 kprobe（内核中使用/uretprobe/eBPF中创建）
10.5 uprobe（单行程序测试/eBPF中创建）
10.6 bashreadline 程序
10.7 USDT（BCC中使用/libbpf中使用）

### 第11章 eBPF 内核辅助方法
11.1 如何查阅内核辅助方法
11.2 辅助方法的实现原理
11.3 eBPF 内核辅助方法分类（网络/数据处理/跟踪/系统功能）
11.4 常用的 eBPF 内核辅助方法

### 第12章 Linux 性能分析
12.1 CPU（基础知识/传统工具/eBPF工具/分析策略）
12.2 内存（基础知识/传统工具/eBPF工具/分析方法）
12.3 磁盘 I/O（基础知识/传统工具/BCC工具/性能分析方法）
12.4 网络（基础知识/传统工具/eBPF工具/性能分析方法）
12.5 常用分析方法和案例

### 第13章 eBPF 实战应用
13.1 软件动态分析
13.2 网络安全
13.3 安全环境增强
13.4 网络数据处理
13.5 应用系统运维
