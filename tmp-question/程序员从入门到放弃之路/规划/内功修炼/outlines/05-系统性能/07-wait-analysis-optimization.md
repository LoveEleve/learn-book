# 等待分析 — 7维定量区分 + 执行太多 vs 太慢 vs 排队

> Cluster D: 4 KPs | 依赖: 05-memory-disk-network-observability + 06-ftrace-bpf-tracing | 读者基线: 会用 perf+Ftrace 测量系统指标

---

### 1. 等待分析 7 维 — CPU/内存/磁盘/网络/锁/时间/队列
  - CPU等待: 线程在 rq 排队(`vmstat r`列) → 上下文切换等待时间(`perf sched latency`) → 本质是CPU不够分 (深入理解软件性能 Ch20-29)
  - 内存等待: 缺页异常延迟(`perf stat -e page-faults` + PSI `pressure/memory`) → 冷内存首次分配、swap 换入、TLB miss 产生 page walk
  - 磁盘等待: I/O 阻塞时长(biolatency P99) → 文件系统缓存miss→读盘 → 同步写 fsync() 导致全flush延迟
  - 网络等待: RPC服务端/数据库连接/Redis命令延时 → 网络层重传延迟(sar ETCP) + 服务端业务逻辑(后端慢) + 排队
  - 锁等待: mutex/spinlock/rwlock 持有时间 → `perf lock record` → `perf lock report` 排序 (性能之巅 Ch5 §2-4)
  - 时间等待: `sleep()` 或 `nanosleep()` 延迟后执行 → 定时器精度(jiffies HZ=100~1000) → 应用程序中的 `time.After()` 误用
  - 队列等待: 在服务前在队列中等待的时间(非CPU rq, 是消息/任务/请求队列) → `net.core.netdev_budget`(网络softirq) 或线程池队列

### 2. 定量诊断公式 — 执行太多 vs 执行太慢
  - 执行太多: IPC正常(~1+), 但 instruction_count 过高(同业务量比较) → 算法/循环/Fnv 重计算 (深入理解软件性能 Ch20-29)
  - 执行太慢: IPC低(<0.5), instruction_count 不高 → 流水线停顿 → TMA Backend Bound(数据在L3/MEM) 或 Frontend Bound(ICache)
  - 排除法顺序: 有CPU时间占比 → 无CPU时间=在排队 → 分类: CPU(=执行太慢)/IO(磁盘+网络+内存)/锁(等待时间)/人为(sleep/时间任务)
  - 排队深度: 同一时刻排队的任务数=`vmstat r`(CPU)+`iostat aqu-sz`(磁盘)+`ss Send-Q`(网络) → 如果都小但延时高: 异步等待问题

### 3. 应用性能技术 — 缓冲区/非阻塞IO/并发模型/锁分析
  - 缓冲区: 小IO→先攒大IO(4KB→64KB) → 减少系统调用+磁盘磁头移动 → 缓冲区大小选择(太大→延时, 太小→吞吐不够) (性能之巅 Ch5 §2-4)
  - 非阻塞 I/O: epoll/poll → 一个线程可同时等多个FD → 当IO比CPU多时(Nginx/Redis)
  - 并发模型: 单线程(event-loop Redis) vs 多线程(event-per-thread Nginx) vs 线程池(Netty) → 看是CPU密集(多线程)还是IO密集(单线程)
  - 锁分析: `perf lock record -p PID -- sleep 10` → `perf lock report` → 持有时间前5的锁 → 优化: 缩小临界区/读写锁/无锁CAS/COW

### 4. 等待 vs 性能退化 — 案例诊断
  - 案例1(CPU等待): `vmstat r=8`(8任务排队等CPU) → `perf sched latency` 最大延迟200ms → 加CPU或减少计算 → CPU bound
  - 案例2(磁盘等待): `iostat %util=100, await=50ms` → `biolatency-bpfcc` P99=800ms → SSD换成NVMe或异步化
  - 案例3(锁等待): 吞吐不随线程数线性上升 → `perf lock report` 显示 `futex` 80%等待 → 删掉粗粒度大锁
  - 案例4(内存泄漏): `RSS` 持续上升但 `vmstat` free 减小 → `memleak-bpfcc` 定位 `alloc()` 无 `free()` 位置

### 5. 收束
  - 慢查询诊断 = CPU + IO + 锁 + 队列 + 内存 + 网络 + 时间 七维逐一排除
  - "执行太多"(正常IPC但过高instructions) vs "执行太慢"(低IPC) 两叉分化 → 分别指向算法改换 vs 数据布局
  - 缓冲/非阻塞/并发模型/锁 四项应用技术是业务代码级的性能优化手段

---

### 核心悬念
**"等待分析找到瓶颈(锁、缓存 miss、分支预测错) — 但你把能改的都改了, 还能做什么？答案在编译器都不知道的地方: 循环体、机器码布局、缓存友好的数据结构。"**

→ 引出 08-源码级调优: 循环优化/SIMD/缓存布局/机器码排列/多线程
