# 01-Linux操作系统内核 · 逐书逐章知识点规划

> 16本书 / 7个子领域 / 逐章拆解核心知识点

---

## 子领域一：操作系统入门（必读2本）

### 📖 现代操作系统：原理与实现（陈海波）

**定位：** 单本书覆盖操作系统全部核心概念，中文质量最高的OS教材。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | 操作系统概述 | OS定义/分层/接口/内核类型(宏内核/微内核/混合内核)/系统调用机制 | OS和普通库的区别是什么？ |
| 2 | 硬件结构 | CPU架构(x86/ARM)/寄存器/MMU/TLB/中断控制器/IOMMU/DMA/总线 | 为什么需要MMU？没有MMU的系统怎么跑？ |
| 3 | 操作系统结构 | 内核设计模式/模块化/用户态与内核态切换成本/Linux内核结构 | 一次系统调用开销多少CPU周期？ |
| 4 | 内存管理 | 虚拟内存/分页/分段/页表/多级页表/TLB/缺页中断/换页策略(LRU/Clock)/内存碎片/伙伴系统/slab | 32位系统最多寻址多少内存？PAE怎么工作的？ |
| 5 | 进程与线程 | 进程概念/PCB/task_struct/线程/LWP(轻量级进程)/上下文切换开销/fork/clone | fork()子进程的内存怎么"复制"的？ |
| 6 | 操作系统调度 | 调度算法(FCFS/SJF/优先级/多级队列)/CFS调度器/实时调度/亲和性/负载均衡/NUMA调度 | CFS的vruntime怎么计算？为什么用红黑树？ |
| 7 | 进程间通信 | 管道(匿名/命名)/消息队列/共享内存/信号量/信号/Socket/eventfd/signalfd | 共享内存为什么是最快的IPC？有什么代价？ |
| 8 | 同步原语 | 竞态条件/临界区/互斥锁/自旋锁/读写锁/信号量/条件变量/RCU/死锁条件/活锁 | 自旋锁和互斥锁什么时候各用哪个？ |
| 9 | 文件系统与存储 | 文件概念/inode/VFS(虚拟文件系统)/目录结构/文件系统布局/磁盘调度算法/SSD与HDD差异 | 为什么删除文件需要目录的写权限而不是文件的？ |
| 10 | 设备管理 | 设备类型/设备文件/设备驱动模型/IO端口与IO内存/中断处理(top half/bottom half) | 中断上下文和进程上下文有什么区别？ |
| 11 | 系统虚拟化 | 虚拟化类型(Type1/Type2)/VT-x/EPT/内存虚拟化/设备虚拟化/KVM/QEMU | KVM为什么是Type2虚拟化？和Xen有什么区别？ |
| 12 | 多核与多处理器 | SMP架构/NUMA架构/缓存一致性(MESI)/内存屏障/per-CPU变量 | NUMA下跨节点内存访问慢多少？ |
| 13 | 文件系统崩溃一致性 | 崩溃一致性问题/日志(journal)/写时复制(COW)/fsck/软更新 | ext4的日志模式和有序模式区别？ |
| 14 | 网络协议栈与系统 | 协议栈架构/sk_buff/net_device/netfilter/数据包收发路径/GRO/GSO | 一个TCP数据包进入内核后经过了哪些数据结构？ |
| 15 | 轻量级虚拟化 | Namespace/Cgroups/UnionFS/容器原理/Docker核心 | 容器和虚拟机的本质区别是什么？ |
| 16 | 操作系统安全 | DAC/MAC/SELinux/AppArmor/capabilities/seccomp/地址空间随机化(ASLR)/栈保护 | Linux默认用DAC还是MAC？ |

### 📖 计算机系统：基于x86+Linux平台（袁春风）

**定位：** CSAPP的中文等价教材，从硬件到软件的完整链路，体系结构入门的基石。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | 计算机系统概述 | 冯诺依曼结构/哈佛结构/总线/CPU-内存-I/O三级结构/计算机性能指标(CPI/MIPS) | 一台2GHz的CPU执行一个指令平均需要多少时间？ |
| 2 | 数据的机器级表示与处理 | 整数表示(原码/反码/补码)/浮点数(IEEE 754)/大小端/位运算/溢出/ALU | 为什么0.1+0.2 != 0.3？浮点数怎么表示的？ |
| 3 | 程序转换与指令系统 | 编译过程(预处理/编译/汇编/链接)/指令集(ISA)/CISC vs RISC/x86与ARM指令差异 | 静态链接和动态链接的可执行文件大小差多少？ |
| 4 | 程序的机器级表示 | x86汇编基础/栈帧/函数调用约定(calling convention)/条件码/跳转/循环的汇编实现 | 函数调用的`call`指令到底做了什么？ |
| 5 | 程序的链接与加载执行 | ELF格式/符号解析/重定位/静态链接/动态链接/PLT与GOT/延迟绑定(lazy binding)/动态库加载路径 | 为什么动态链接的程序启动更慢？GOT和PLT怎么配合的？ |
| 6 | 存储器层次结构 | 存储金字塔(寄存器/L1/L2/L3/主存/磁盘)/局部性原理(时间/空间)/缓存映射(直接/组相联/全相联)/缓存替换/写策略(write-back/write-through) | L1缓存访问延迟是多少CPU周期？L3呢？主存呢？ |
| 7 | 虚拟存储器 | 虚拟地址空间/页表/多级页表/TLB/地址翻译过程/缺页处理/内存映射(mmap) | 一个虚拟地址翻译到物理地址需要几次内存访问？TLB怎么加速的？ |
| 8 | 进程与异常控制流 | 进程上下文切换/异常/中断/系统调用/信号/sigaction/非本地跳转(setjmp/longjmp) | Ctrl+C是怎么终止一个进程的？信号处理的流程？ |
| 9 | I/O操作的实现 | I/O总线/设备控制器/DMA/中断驱动I/O/阻塞与非阻塞I/O/I/O多路复用(select/poll/epoll) | DMA为什么比PIO快？DMA和CPU怎么协调的？ |
| 10 | 程序性能的优化 | 编译器优化选项/循环展开/减少函数调用/消除不必要的内存引用/缓存友好的代码/位操作技巧 | 为什么按行遍历二维数组比按列快？ |
| 11 | 网络编程 | Socket API/client-server模型/并发服务器(多进程/多线程/事件驱动)/Web服务器原理 | 一个并发服务器的三种模型各有什么优缺点？ |
| 12 | 并发编程 | 多线程/线程安全/互斥锁/信号量/生产者-消费者/死锁/线程池/并发数据结构 | 什么是线程安全？如何判断一段代码是否线程安全？ |

---

## 子领域二：内核架构深度（主力2本+辅助1本）

### 📖 深入Linux内核架构与底层原理(第2版)（刘京洋）

**定位：** 全面型内核书籍，横跨进程/内存/存储/网络/内核机制，对比Windows和Fuchsia。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | 操作系统总览 | OS发展史/UNIX哲学/宏内核vs微内核vs混合内核/Linux内核版本号规则/主线vs长期支持/内核编译 | 为什么Linux选择宏内核？微内核有什么优势？ |
| 2 | 系统结构 | 内核空间布局/逻辑地址与物理地址映射/high memory/内核态与用户态的内存布局差异 | x86_64下内核空间和用户空间各占多少？ |
| 3 | 锁与系统调用 | 自旋锁(spinlock)/互斥锁(mutex)/读写锁(rwlock)/RCU/seqlock/信号量(semaphore)/完成量(completion) | RCU为什么读端无锁？写端怎么同步的？ |
| 4 | 信号、中断与系统调用 | 中断向量表/IDT/中断上半部与下半部/softirq/tasklet/workqueue/信号处理流程/系统调用表(sys_call_table)/快速系统调用(sysenter/sysexit) | 中断下半部、softirq、tasklet的区别和使用场景？ |
| 5 | Linux系统的启动与进程 | BIOS → Bootloader → 内核解压 → start_kernel → init进程/进程0与进程1/进程调度时机/抢占(preemption) | 内核启动后第一个进程是怎么创建的？ |
| 6 | 调度 | CFS调度器/vruntime/红黑树/调度实体(sched_entity)/调度组/负载均衡/NUMA调度/实时调度策略(SCHED_FIFO/SCHED_RR) | 多核系统中CFS怎么保证各核上的进程公平？ |
| 7 | 内存管理 | 物理内存模型(SPARSEMEM/FLATMEM)/Node/Zone/page结构/伙伴系统/slab/slub/slob/页分配器/kmalloc/vmalloc | kmalloc和vmalloc分配的物理内存为什么特性不同？ |
| 8 | 存储 | VFS核心(inode/dentry/file/super_block)/通用块层/IO调度器(CFQ/Deadline/NOOP)/Ext4文件系统/页缓存/预读机制 | 一次read()系统调用怎么经过VFS到达具体文件系统的？ |
| 9 | 套接字(socket) | socket核心结构(socket/sock)/BSD socket层/INET socket层/protocol族/Netlink/BPF与eBPF(概述) | socket()创建时内核做了什么？ |
| 10 | 网络 | 网络架构全景/net_device/net_device_ops/NAPI/IP层实现/TCP实现概要/Netfilter与iptables/负载均衡/IPVS | 网络数据包在内核中的处理路径是怎样的？ |

### 📖 Linux技术内幕（罗秋明）

**定位：** 以"内存模型 + 时空模型"为分析主线，与上一本书的互补视角。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | Linux内核概述 | 内核编译/内核模块(insmod/rmmod)/printk/内核调试方法/kgdb/kdump | 怎么给正在运行的内核加一个新功能而不重启？ |
| 2 | 进程影像 | task_struct/thread_info/thread_struct/进程地址空间/mm_struct/vm_area_struct/页表 | 进程切换时哪些寄存器/结构体变了？ |
| 3 | 虚拟空间的物理支撑 | 页表(四级)/PGD/PUD/PMD/PTE/TLB刷新/ASID(地址空间标识符)/大页(hugepage/THP) | TLB flush为什么那么贵？什么时候可以避免？ |
| 4 | 进程组织与基础行为 | 进程树/父子关系/等待队列/进程状态(TASK_RUNNING/TASK_INTERRUPTIBLE等)/进程创建(fork/clone)/进程终止(do_exit)/僵尸进程/孤儿进程 | 僵尸进程怎么形成的？怎么清理？ |
| 5 | 进程调度与负载均衡 | CFS调度器详细实现/调度类(sched_class)/负载计算/PELT/RUNQUEUE/调度域与调度组/负载均衡策略 | 新进程被创建后放到哪个CPU上执行？ |
| 6 | 进程间通信与同步 | System V IPC(共享内存/消息队列/信号量)/POSIX IPC(消息队列/共享内存/信号量)/futex | futex为什么比传统信号量更适合做用户态锁？ |
| 7 | 内核活动 | 内核线程/workqueue/interrupt上下文/软中断(softirq)各种类型(TIMER/NETBLOCK等) | 什么时候用workqueue什么时候用tasklet？ |
| 8 | 时间管理 | 节拍(tick)/jiffies/HZ/timekeeper/clocksource/高精度定时器(hrtimer)/NTP时间同步 | 为什么有些系统选择无节拍(tickless)？ |
| 9 | 内核并发与同步 | 原子操作/内存屏障/smp_mb/RCU(读写侧详细)/per-CPU变量/顺序锁(seqlock) | RCU的宽限期(grace period)怎么定义的？ |
| 10-13 | 文件系统系列 | VFS核心对象(超级块/inode/dentry/file/file_operations/address_space)/文件操作(open/read/write/close/mmap)/Ext文件系统家族 | open()创建了哪些内核对象？和用户态的fd怎么映射的？ |
| 14-15 | 页缓存与内存回收 | 页缓存(radix tree/xarray)/回写机制(writeback)/内存回收(PFRA)/kswapd/内存水位(watermark)/OOM Killer | 系统内存不足时kswapd做了什么？ |
| 16-17 | 设备管理 | 设备模型(device/bus/driver/class)/设备树/块设备层/bio/IO调度/设备驱动框架 | 用户态的write()怎么变成一个设备上的IO操作？ |

### 📖 图解Linux内核：基于6.x（辅助）

**定位：** 图解式入门，降低概念门槛。

**四大篇章核心知识点：**

| 篇章 | 章节范围 | 核心知识点 |
|:---|:---|:---|
| 知识储备篇 | 1-4章 | 内核概览/链表(双向循环链表)/红黑树/基数树/中断(上半部/下半部)/时钟/时间子系统 |
| 内存管理篇 | 5-9章 | 物理内存管理(伙伴系统)/虚拟内存管理(页表/VMA)/缺页处理/mmap/madvise/内存回收(KSM/压缩/zswap) |
| 文件系统篇 | 10-12章 | VFS/超级块/inode/dentry/file/sysfs文件系统/文件挂载/Ext4核心 |
| 进程管理篇 | 13-16章 | task_struct/进程创建/CFS调度器/信号生成与递送/进程间通信 |
| 综合应用篇 | 17-21章 | 程序加载执行/IO多路复用/Binder(Android)/Camera驱动案例/KVM虚拟化案例 |

---

## 子领域三：内存管理专项（2本+1本进程内存）

### 📖 THE LINUX MEMORY MANAGER（Lorenzo Stoakes, Early Access 2025）

**定位：** 全英文内存管理器专项深度教材，2025年新书，基于最新内核。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | Introduction | Linux内存管理概述/为什么需要虚拟内存/物理vs虚拟地址/MMU | 虚拟内存的核心价值是什么？（不只是"内存更大"） |
| 2 | Physical Memory | 物理内存布局/NUMA/SMP/page/buddy allocator/memory zones(DMA/NORMAL/HIGHMEM) | 为什么还有DMA zone？现在不是都用64位了吗？ |
| 3 | Virtual Memory | 页表(4级/5级)/PGD/P4D/PUD/PMD/PTE/TLB flush/地址空间布局 | 5级页表什么场景下需要？比4级多了什么？ |
| 4 | Process Memory | task_struct ↔ mm_struct ↔ vm_area_struct/进程地址空间布局/Brk堆/mmap区域/栈的自动扩展 | 一个进程的/proc/PID/maps文件怎么看？ |
| 5 | Memory Mapping | mmap系统调用/文件映射与匿名映射/共享映射与私有映射/mmap flags详解 | mmap读文件为什么比read快？有什么代价？ |
| 6 | Page Faults | 缺页中断类型(major/minor/invalid)/缺页处理流程(handle_mm_fault)/按需分页(demand paging)/预读 | minor fault和major fault区别是什么？ |
| 7 | Reverse Mappings | 反向映射(r_map)/anon_vma/i_mmap/为什么需要反向映射/回收时需要 | 正向页表和反向页表各用于什么场景？ |
| 8 | Manipulating Userland Memory | copy_from_user/copy_to_user/fault_in_readable/fault_in_writable/进程间内存访问(ptrace/proc/PID/mem) | 内核怎么安全地访问用户态内存？ |
| 9 | The Page Cache | 页缓存(radix tree → xarray)/缓存查找与插入/缓存回收/脏页/thrashing | 为什么Linux用xarray替代了radix tree？ |
| 10 | Writeback | 脏页回写/回写线程(pdflush → flusher threads)/sync/fsync/fdatasync/回写策略 | fdatasync比fsync少做了什么？ |
| 11 | Reclaim and Memory Pressure | 内存回收(PFRA)/kswapd/水位(high/low/min)/direcl reclaim vs background reclaim/LRU链表(active/inactive) | 什么时候触发直接回收(direct reclaim)？有什么危害？ |
| 12 | Swap Memory | 交换分区/换出(swap out)/换入(swap in)/swap cache/swap token/swappiness | swappiness=0和swappiness=100时的行为有什么不同？ |
| 13 | The OOM Killer | OOM Killer触发条件/打分机制(badness score)/oom_score_adj/内存cgroup OOM | OOM Killer怎么选择杀哪个进程？如何保护关键进程？ |
| 14 | Practical Memory Management | 内存泄漏检测(valgrind/asan/lsan)/内存压力测试/memcg/cgroup内存限制/perf内存事件/eBPF内存追踪 | 如何用eBPF追踪一个进程的所有内存分配？ |

### 📖 内存管理-基于Linux 3.10

**定位：** 基于较老但稳定的内核版本的内存管理源码分析。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | 物理内存的布局 | 物理地址空间/e820图/内存探测/保留内存/BIOS/UEFI内存映射 | BIOS怎么告诉内核有多少物理内存？ |
| 2 | 物理内存管理模型 | NUMA Node/pg_data_t/内存域Zone(ZONE_DMA/ZONE_NORMAL/ZONE_HIGHMEM)/page结构体/struct page的union设计 | 32位内核的HIGHMEM zone为什么是必要的？ |
| 3 | 内存初始化 | 实模式vs保护模式/分页开启/页表初始化(setup_arch)/per-CPU区域初始化/bootmem/memblock分配器 | 内核在伙伴系统还没初始化时用什么分配内存？ |
| 4 | 物理内存管理 | 伙伴系统(Buddy System)核心算法/阶(order)/分裂与合并/伙伴系统API(alloc_pages/free_pages)/slab分配器(通用缓存/专用缓存) | slab分配器怎么解决伙伴系统内碎片问题的？ |
| 5 | 进程虚拟内存 | 进程地址空间(mm_struct)/虚拟内存区域(vm_area_struct)/匿名映射/文件映射/堆和栈的增长/do_brk/mmap_region | 用户态malloc(1024)在内核层面做了什么？ |

### 📖 深入理解Linux进程与内存（扫描版）

**说明：** 本书为扫描版PDF（542页），无文本版。内容介于 "内存管理"和"进程管理"两个方向之间。与上述2本配合阅读。

**可从书名推断的核心主题：**
- 进程生命周期：创建(fork/clone)、执行(execve)、终止(exit/wait)
- 进程地址空间管理：虚拟内存的创建、修改、销毁
- 内存分配器：glibc malloc实现（ptmalloc/jemalloc/tcmalloc对比）
- 进程间内存共享：共享内存/写时复制(COW)/KSM(内核同页合并)

---

## 子领域四：系统编程（1本）

### 📖 LINUX系统编程(第2版)（Robert Love）

**定位：** 系统调用API实战手册，Linux编程的"字典"。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | 入门和基本概念 | API vs ABI/文件描述符(fd)/错误处理(errno)/系统调用开销/getpid/getuid | 一个附加了O_CLOEXEC的fd什么时候关闭？ |
| 2 | 文件I/O | open/read/write/lseek/close/文件偏移量/O_APPEND/原子操作/pread/pwrite/O_DIRECT/同步I/O(fsync/sync) | pwrite和lseek+write有什么本质区别？ |
| 3 | 缓冲I/O | 标准IO库(stdio)/FILE结构/缓冲区类型(全缓冲/行缓冲/无缓冲)/setvbuf/fflush/fopen/fread/fwrite/fclose | printf输出到终端和文件时缓冲行为为什么不同？ |
| 4 | 高级文件I/O | 分散聚集IO(readv/writev)/事件驱动IO(select/poll/epoll)/内存映射(mmap)/sendfile/splice/tee | sendfile实现零拷贝的原理是什么？ |
| 5 | 进程管理 | fork/vfork/clone/exec家族(execl/execv/execve等)/wait/waitpid/僵尸进程/孤儿进程/进程组/会话/守护进程 | fork()之后父子进程共享了什么？什么不共享？ |
| 6 | 高级进程管理 | 进程优先级(nice/setpriority)/处理器亲和性(sched_setaffinity)/实时调度/sched_yield/进程资源限制(getrlimit/setrlimit) | nice值从-20到19，"友好"的进程CPU时间少？ |
| 7 | 线程 | POSIX线程(pthread)/创建(create)/终止(join/detach)/线程安全/可重入/Pthread同步(互斥锁/条件变量/RW锁)/线程局部存储(TLS) | Pthread和Linux native thread(NPTL)是什么关系？ |
| 8 | 文件和目录管理 | stat/lstat/目录操作(opendir/readdir)/链接(硬链接/软链接)/inode/文件类型与权限/扩展属性(xattr) | 硬链接和软链接在inode层面有什么区别？ |
| 9 | 内存管理 | 进程地址空间/sbrk/brk/malloc/free/mmap/munmap/madvise/posix_fadvise/内存对齐(posix_memalign) | malloc底层怎么决定调用brk还是mmap？ |
| 10 | 信号 | 信号概念/发送(kill/raise/alarm)/信号集(sigset)/阻塞/递送/信号处理器(sigaction)/信号安全函数列表 | 为什么信号处理函数里不能调用printf？ |
| 11 | 时间 | 墙上时间/单调时间(monotonic)/jiffies/定时器/高精度定时器/gettimeofday/clock_gettime | 为什么计算时间差要用单调时钟而不是墙上时钟？ |

---

## 子领域五：文件系统（1本）

### 📖 文件系统技术内幕（张书宁）

**定位：** 文件系统全栈——从本地到分布式，从原理到Ext2源码。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | 文件系统是什么 | 文件系统定义/文件系统层次(用户接口 → VFS → 具体FS → 通用块层 → IO调度 → 设备驱动) | 为什么需要VFS这一层抽象？ |
| 2 | 如何使用文件系统 | 挂载(mount)/文件系统类型/格式化和分区/文件系统工具(fsck/resize2fs等)/磁盘配额/文件系统基准测试(fio) | 根文件系统怎么在启动时挂载的？ |
| 3 | 本地文件系统原理及核心技术 | 超级块/组描述符/数据块位图/inode位图/inode表/数据块/日志(journal)/写时复制(COW)/B-Tree/B+Tree | Ext4、XFS、Btrfs的核心设计差异是什么？ |
| 4 | Ext2文件系统代码详解 | Ext2超级块结构/inode结构/目录项/数据块寻址(直接/间接/二级间接/三级间接)/extent(Ext4扩展)/block group | 为什么Ext2/3有子目录数量限制？Ext4怎么解决的？ |
| 5 | 网络文件系统 | NFS协议/v3 vs v4/文件锁/缓存一致性/POSIX语义/SMB/CIFS | NFS客户端缓存了什么？怎么保证缓存一致性？ |
| 6 | 分布式文件系统 | GFS/HDFS的设计/Ceph(CRUSH算法/RADOS)/GlusterFS/分布式元数据管理/副本与纠删码 | Ceph的CRUSH算法为什么不需要中心化元数据服务器？ |
| 7 | 文件系统的其他形态 | 对象存储(S3/Swift)/FUSE用户态文件系统/内存文件系统(tmpfs/ramfs)/procfs/sysfs/debugfs | FUSE文件系统的性能瓶颈在哪里？ |

---

## 子领域六：网络协议栈（2本）

### 📖 Linux 4.4.0内核源码分析-TCP实现

**定位：** 专注于Linux内核TCP实现的源码级别剖析。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | 准备部分 | 源码阅读工具(cscope/ctags/LXR)/内核源码目录结构/net/ipv4/核心文件 | `tcp_input.c`和`tcp_output.c`分别管什么？ |
| 2 | 核心数据结构 | sk_buff/sock/tcp_sock/inet_connection_sock/路由相关结构/网络子系统层次 | sk_buff怎么在协议栈各层之间传递的？ |
| 3 | TCP输出 | tcp_sendmsg/tcp_push/数据分段(tcp_write_xmit)/Nagle算法/tcp_transmit_skb/窗口与cwind/TSO/GSO | Nagle算法什么时候delay数据发送？ |
| 4 | TCP输入 | 接收路径(tcp_v4_rcv → tcp_v4_do_rcv → tcp_rcv_established)/乱序队列/快速路径vs慢速路径 | TCP收到乱序包后如何处理？out_of_order_queue怎么管理的？ |
| 5 | TCP连接建立 | 三次握手源码(tcp_v4_connect → tcp_connect / tcp_v4_rcv → tcp_rcv_state_process)/SYN cookies/SYN队列/accept队列 | SYN cookie怎么防止SYN flood攻击？代价是什么？ |
| 6 | TCP拥塞控制 | 拥塞控制接口(congestion_ops)/CUBIC/Reno/BBR/慢启动/拥塞避免/快速恢复/快速重传 | CUBIC和BBR的核心思想分别是什么？ |
| 7 | TCP释放连接 | 四次挥手(tcp_shutdown/tcp_close/FIN_WAIT/TIME_WAIT)/TIME_WAIT作用与优化(tcp_tw_reuse) | TIME_WAIT状态为什么等2MSL？tcp_tw_reuse安全吗？ |
| 8 | 非核心代码 | BSD socket层(通用socket接口)/INET socket层(地址族抽象)/Netfilter钩子 | socket层是内核态还是用户态代码？ |
| 9 | 附录：基础知识 | 计算机网络基础回顾/OSI模型/TCP状态机/滑动窗口/流量控制 | TCP滑动窗口如何保证不溢出接收方缓冲区？ |

### 📖 追踪Linux TCP/IP代码运行——基于2.6.6内核

**定位：** 通过代码追踪的方式理解内核网络协议栈，版本虽老但流程不变。

| 章节 | 章节名 | 核心知识点 |
|:---:|------|:---|
| 1 | 本书的计划 | 代码追踪方法论/源码编译环境搭建 |
| 2 | socket的创建 | socket()系统调用完整代码路径/sock_create → inet_create/协议族注册/传输层协议注册 |
| 3 | socket地址设置 | bind()的内核实现/inet_bind/端口检查/地址绑定到socket |
| 4 | 路由 | 路由子系统/路由表(route table)/fib_lookup/路由缓存(已移除)/策略路由 |
| 5 | 通知链 | notifier chain 机制/内核事件通知/register_netdevice_notifier |
| 6 | netlink概述 | Netlink协议族/用户态-内核态通信(/proc vs netlink)/rtnetlink |
| 7 | 监听连接请求 | listen()内核实现/inet_listen/LISTEN状态/半连接队列(syn queue) |
| 8 | 接收连接请求 | accept()内核实现/inet_accept/全连接队列/三次握手完成后的处理 |
| 9 | 准备连接请求 | connect()内核实现/tcp_v4_connect/路由查找/SYN包构造与发送 |
| 10 | 邻居子系统 | ARP实现(arp_solicit)/邻居项(neighbour)/ARP缓存/ARP状态机 |
| 11 | 流量控制 | Traffic Control(tc)/排队规则(qdisc)/分类(class)/过滤器(filter) |
| 12 | 建立连接的过程 | 三次握手完整追踪(客户端+服务端视角)/状态变迁/tcp_rcv_state_process |
| 13 | Internet控制信息的传输 | ICMP协议实现/ICMP错误处理与TCP交互/path MTU discovery |
| 14 | 数据包的分段与重组 | IP分片(fragment)/分段重组/MSS/PMTU/DF标志位 |
| 15 | 发送和接收数据包 | tcp_sendmsg完整路径/tcp_v4_rcv接收路径/数据拷贝/checksum |
| 16 | socket的关闭 | tcp_shutdown/tcp_close/FIN/FIN_WAIT/TIME_WAIT/linger |

---

## 子领域七：调试与诊断（1本）

### 📖 Accelerated Linux Core Dump Analysis（Dmitry Vostokov）

**定位：** 面向模式的软件诊断——Core Dump分析的完整方法论。

**核心知识点（本书以"pattern"为组织单位，非章节）：**

| 知识模块 | 核心内容 |
|:---|:---|
| Core Dump基础 | /proc/sys/kernel/core_pattern/coredump_filter/coredump产生条件(GDB生成/系统生成) |
| GDB调试核心 | bt(backtrace)/frame切换/info registers/disassemble/examine内存/thread apply all bt/多线程调试 |
| 常见崩溃模式 | NULL指针解引用/Double Free/Use After Free/Stack Buffer Overflow/Heap Corruption/死锁/活锁/栈溢出 |
| 用户态和内核态交互 | 系统调用追踪(strace)/信号与coredump关系/用户态崩溃如何触发内核 |
| 取证分析纲要 | 从coredump重建事故现场/时间线重建/毒模式追踪/根因识别方法 |

---

## 子领域八：并发与网络编程（1本+1本高级）

### 📖 Linux多线程服务端编程——muduo C++网络库（陈硕）

**定位：** 工业级C++网络服务端编程的最佳实践。

| 章节 | 章节名 | 核心知识点 | 检验问题 |
|:---:|------|:---|:---|
| 1 | 线程安全的对象生命期管理 | 对象析构与线程安全/race condition/观察者模式中的竞态/shared_ptr的线程安全边界/weak_ptr解决循环引用 | shared_ptr是线程安全的吗？引用计数更新呢？ |
| 2 | 线程同步精要 | 互斥锁(mutex)/条件变量(condition_variable)/不要用读写锁/不要用信号量/RAII/锁的粒度/死锁避免(按序加锁) | 为什么陈硕建议避免使用读写锁？ |
| 3 | 多线程服务器适用场合与编程模型 | 单线程/acceptor+worker/reactor/proactor/one loop per thread/进程间通信 vs 线程间通信 | 什么场景用多进程？什么场景用多线程？ |
| 4 | C++多线程系统编程精要 | thread_local/__thread/pthread_atfork/fork与多线程的兼容性/写时复制与fork的安全问题 | 多线程程序fork()后子进程为什么只有调用线程被复制？ |
| 5 | 高效的多线程日志 | 日志库设计/双缓冲(double buffering)/异步日志/Log4j模式/Frontend和Backend分离 | 高并发场景下日志库如何做到不阻塞业务线程？ |
| 6 | muduo网络库简介 | reactor模式/EventLoop/Poller/Channel/TimerQueue/Acceptor/Connector/TcpConnection | muduo的核心类图是怎样的？ |
| 7 | muduo编程示例 | Echo Server/Discard Server/Daytime Server/HTTP/文件传输/聊天服务器(multi-room)/Hub服务 | 如何用muduo写一个支持10000并发连接的echo服务器？ |
| 8 | muduo设计与实现 | EventLoop源码/多线程下跨线程调用runInLoop/Poller封装(epoll)/Timer实现(timerfd)/Buffer设计 | muduo为什么用timerfd而不是自己管理定时器？ |
| 9 | 分布式系统工程实践 | 分布式系统的难点/CAP理论/SOA/微服务/分布式对象/RPC/服务发现/ZooKeeper | 分布式系统中"部分失败"如何应对？ |
| 10 | C++编译链接模型精要 | One Definition Rule(ODR)/头文件与源文件/动态库/符号可见性/链接错误谜题 | 为什么模板类定义要放在头文件里？ |
| 11 | 反思C++面向对象与虚函数 | 基于对象(object-based) vs 面向对象/虚函数开销(虚表/虚指针)/函数对象(std::function)/C++与Java面向对象差异 | C++虚函数调用的运行时代价是多少？ |
| 12 | C++经验谈 | C++最佳实践/RAII/智能指针使用原则/异常安全/性能陷阱/代码风格/C++11/14/17新特性 | C++11引入了哪些重要的多线程设施？ |

### 📖 现代体系结构上的UNIX系统（Curt Schimmel）

**定位：** SMP(对称多处理)和缓存技术的经典著作，内核程序员必读。

**说明：** 本书为扫描版PDF，无法提取完整文字章节。但其核心主题非常明确：

| 主题模块 | 核心知识点 |
|:---|:---|
| SMP架构 | 多处理器启动/CPU间中断(IPI)/TLB shootdown/处理器间同步 |
| 缓存一致性 | MESI协议演变/缓存行(cache line)/伪共享(false sharing)/缓存行对齐(cache line padding) |
| 自旋锁深度 | 自旋锁在SMP下的正确实现/自旋锁与抢占/自旋锁持有时间约束/自适应自旋锁 |
| 内核同步 | 调度延迟/synchronize_rcu/内存屏障在SMP下的完整语义/per-CPU变量在SMP的优势 |
| 多处理器调度 | 处理器亲和性/调度域/负载均衡/NUMA感知调度 |

---

## 本领域学习检查表

完成16本书后，你应该能够：

- [ ] 画出进程从fork到exit的完整生命周期图，标注每个状态的内核函数
- [ ] 解释一次malloc(1024)在内核中的完整路径（缺页 → 伙伴系统 → slab → 返回虚拟地址）
- [ ] 解释一次write()系统调用从VFS到块设备的完整路径
- [ ] 手写一个简单的内核模块（字符设备驱动或/proc文件）
- [ ] 用GDB分析一个coredump，找出崩溃根因
- [ ] 解释RCU的读端无锁原理和宽限期机制
- [ ] 理解CFS调度器中vruntime的计算和红黑树的使用
- [ ] 能基于muduo库写一个高并发TCP服务器
