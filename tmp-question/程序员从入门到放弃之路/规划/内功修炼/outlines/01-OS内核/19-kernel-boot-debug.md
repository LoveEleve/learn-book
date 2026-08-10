# 内核架构与调试 — 宏内核/微内核 + 启动流程 + 模块 + strace/perf/ftrace/bpftrace

> Cluster E: 7 KPs | 依赖: 01-物理内存 + 09-中断处理 + 11-进程模型 | 读者基线: 理解进程、内存、中断的核心概念

---

### 1. 宏内核 vs 微内核 — 两种架构哲学
  - 宏内核(Linux): 所有服务(文件系统/网络/设备驱动/调度)运行在内核空间同一地址空间 → 高效(函数调用无 IPC 开销) → 模块化但有耦合 → 一个驱动崩溃可能导致全系统 panic (kernel/panic.c:202)
  - 微内核(Minix/L4): 最小内核(IPC+调度+内存管理基础) → 其余服务在用户态独立进程 → 隔离好(driver crash 不杀系统) → IPC 代价大(上下文切换+复制) → 典型 ~10% 性能损失 vs 宏内核
  - Linux 的模块化: 模块编译为 `.ko` → `modprobe` 动态加载 → 但仍在同一内核地址空间(不算微内核)

### 2. 内核启动流程 — 从 bootloader 到 init
  - GRUB → 解压内核 → `startup_32/startup_64`(早期架构相关设置, 解压 bzImage) → `start_kernel`(核心初始化) (init/main.c:524)
  - `setup_arch`(架构相关: 解析 CPU 特性/设置页表/设置 initial memory) → `mm_init`(内存初始化: buddy/memblock) → `sched_init`(调度初始化: init_task 创建) → `rest_init` (init/main.c:689)
  - `kernel_init`(第一个用户进程 PID=1) → `do_execve(/sbin/init)` → `idle` 进程(PID=0) — 永远不运行(只是后台) (init/main.c:1180)

### 3. 内核模块 — 动态扩展内核
  - 模块生命周期: `module_init(fn)`(注册) / `module_exit(fn)`(注销) → `insmod mod.ko`(插入) / `rmmod mod`(移除) / `modprobe mod`(含依赖解析) → `lsmod` 列出(读取 `/proc/modules`) (kernel/module/main.c:1845)
  - 编译: `make -C /lib/modules/$(uname -r)/build M=$PWD modules` → obj-m += mod.o → Makefile → 符号解析(内核 EXPORT_SYMBOL)
  - 内核符号表: `/proc/kallsyms`(所有符号地址) → `MODULE_LICENSE("GPL")`(许可证) → `dmesg | grep module`

### 4. 内核数据结构 — list_head + rbtree + xarray
  - list_head: 双向循环链表 → `list_for_each_entry` → `list_add` → `list_del` — 节点嵌入结构(非指针) → 不管理内存, 只是链接 (include/linux/list.h:532)
  - rb_node: 红黑树 → `rb_insert_color`(插入) / `rb_erase`(删除) / `rb_first`(最小) — 用于 CFS(vruntime)/VMA(mm_rb)/高精度定时器(hrtimer) (lib/rbtree.c:450)
  - xarray(4.20+): 替代 radix tree → 页缓存索引(address_space→i_pages) → `xa_store/xa_load/xa_erase` → RCU 安全读 (lib/xarray.c:1298)

### 5. 调试与诊断工具 — 从 strace 到 bpftrace
  - strace: `strace -p PID`(追踪系统调用) / `strace -c`(统计耗时) / `strace -e trace=file,network`(过滤类型) / `strace -f`(追踪子进程) / `strace -T`(显示耗时) — 解释"进程在卡什么系统调用" (no related kernel source, userspace tool)
  - perf: `perf top`(实时热点函数) / `perf record -g`(记录调用栈) → `perf report` / `perf stat`(性能计数器: cache-misses/instructions/branch-misses) / `perf c2c`(伪共享检测) / `perf lock`(锁分析) (tools/perf/builtin-top.c)
  - ftrace: tracefs(`/sys/kernel/tracing`) → `trace-cmd record -p function_graph` → 函数调用耗时 → `irqsoff/preemptoff` 延迟分析 → `function_profile_enabled`(热点统计) (kernel/trace/trace.c → tracing_start/tracing_stop)
  - bpftrace/eBPF: `bpftrace -e 'kprobe:do_sys_open { printf("%s: %s\n", comm, str(arg1)) }'` → 内核动态追踪 → eBPF 程序在内核安全运行(验证器保证无环/无越界) → 比 strace 显著低开销 (tools/bpf/bpftool/main.c → do_batch/do_subcommand)
  - SystemTap/DTrace 对比: SystemTap(Linux, 编译 kprobes 脚本为内核模块) → DTrace(Solaris/macOS, D 语言, 零开销静态探针) — 与 bpftrace 同属动态追踪但 bpftrace 更轻量(基于 eBPF 而非编译内核模块)

### 6. 收束
  - 宏内核效率高但风险集中，微内核隔离好但 IPC 代价大
  - 内核启动 = GRUB→start_kernel→mm_init(内存)→sched_init(调度)→kernel_init(PID 1)
  - 调试链: strace(用户态系统调用)→perf(采样热点)→ftrace(追踪函数)→bpftrace(自定义动态追踪)

---

### 核心悬念
**"你已从物理内存一路走到容器和 bpftrace — 但真正在生产内核的源码里找到 Buddy 的分裂逻辑、看到 COW 怎么分配新物理页、单步调试一次 CFS 队列选择 — 准备好拿起源码了吗？"**

→ 引出 02-内存深度 — 从 struct page 到 OOM 的内存管理全景
