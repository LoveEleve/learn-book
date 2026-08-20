# 进程地址空间与内存排障 — maps、malloc、伪泄漏、调度和资源限制

> Cluster B: 8 KPs | 依赖: 01-02 + 03-05 | 读者基线: malloc/free、进程/线程、信号与 mmap
> 读者处境: 05 篇讲了分配器和 brk/mmap；本篇把“RSS 持续上升”“进程变慢”“mlock 失败”“线程跑不动”放进同一张进程资源地图
> 打开新视角: 内存排障不是看一个 RSS 数字，而是**虚拟地址映射 → 物理驻留/共享 → 分配器保留 → cgroup/rlimit → 调度**多层对齐

---

### 概念依赖链

```
01-02 I/O/mmap + 03-05 进程/线程/信号/malloc → 本篇: 地址空间与排障
  ├─ §1 /proc/PID/maps(虚拟地址区)
  ├─ §2 mmap/mlock/madvise(映射/驻留/访问提示)
  ├─ §3 malloc_info/mallinfo(分配器视角)
  ├─ §4 RSS增长/真泄漏/伪泄漏(证据链)
  └─ §5 nice/调度/rlimit(运行资源)
先讲: 地址空间 → 映射 → 分配器 → 泄漏判定 → 调度限制
后续依赖: 07-cpp-atomic-lockfree(多核共享内存的硬件一致性)
```

### 叙事顺序

1. 问题引入——进程 RSS 上升、CPU 变慢、mlock 失败，怎样确认是地址空间、分配器、cgroup 还是调度问题？
2. `/proc/PID/maps`——虚拟地址房产证
3. mmap/mlock/madvise——物理页与访问策略
4. malloc 统计——分配器保留与实际使用
5. 伪泄漏排查——增长不等于泄漏
6. nice/affinity/rlimit——调度与资源边界
7. 收束——多层账本

### 1. `/proc/PID/maps` — 虚拟地址空间的分区图

场景提示: `cat /proc/PID/maps` 中的 `.text`、heap、stack、共享库和匿名映射分别代表什么？ [写作时展开]

关键设计: maps 每行描述一段虚拟地址范围、权限、文件偏移/设备 inode 和 pathname；它描述的是映射，不是所有页都已经占用物理内存：

```[pseudocode]
address range  perms  offset  dev  inode  pathname

.text / r-xp:
  代码映射, 通常文件后备, 可共享
.data/.bss / rw-p:
  可写全局数据, 物理页按需建立
[heap]:
  传统 brk 扩展区域(具体 malloc 选择由 libc 决定)
匿名/file mmap:
  共享库、文件映射、匿名分配、大对象等
[stack]:
  线程栈映射/增长边界
[vvar]/[vdso]:
  内核向用户提供的特殊映射
```

Why: 为什么 maps 里有一段地址不代表 RSS 已经增加同样多？——**虚拟映射、页表、物理驻留、共享页和 COW 是不同层**：只有访问、缺页、回收策略和共享关系共同决定当前驻留。`[heap]` 也不能代表所有 malloc，线程栈和大分配常走其他映射。 [man 5 proc: `/proc/PID/maps/smaps` 字段与权限语义]

比喻锚点: maps 像房产证，记录房间分区和用途；房产证面积不等于房间里当前放了多少家具。 [写作时展开]

### 2. mmap、mlock、munmap、madvise — 映射与物理驻留不是一回事

场景提示: 程序 mmap 1GB 文件后，是否立刻占用 1GB 物理内存？mlock 又改变了什么？ [写作时展开]

关键设计: mmap 建立 VMA，访问和策略决定页是否驻留/回收：

```[pseudocode]
mmap(..., MAP_PRIVATE|MAP_ANONYMOUS)
  → 建立虚拟映射
  → 首次访问可能触发缺页

mlock(addr, len)
  → 请求页保持驻留, 不能被普通换出
  → 受 RLIMIT_MEMLOCK/权限/资源限制

munmap(addr, len)
  → 解除映射
  → 地址/长度/重叠边界必须满足系统调用要求

madvise(addr, len, advice)
  → 给内核访问/回收提示
  → 不是强制保证, 不同 advice 语义不同
```

Why: 为什么 mlock 不是“给进程无限物理内存”？——**它受限于权限、rlimit、可用物理页和锁定页总量**；madvise 也只是策略提示，不能保证预读、释放或访问模式一定按期待执行。Huge page/THP、NUMA 和 cgroup memory 会进一步改变结果。 [man 2 mmap/mlock/munmap/madvise: 映射、对齐、锁页和 advice 边界]

比喻锚点: mmap 是租下房间，mlock 是要求家具不被搬走，madvise 是告诉物业“这间房近期常用/可以清空”；物业仍受整栋楼资源约束。 [写作时展开]

### 3. malloc_info、mallinfo 与分配器视角

场景提示: RSS 上升时，究竟是应用仍持有对象，还是 glibc arena 暂时保留了空闲块？ [写作时展开]

关键设计: 分配器统计接口只能描述 libc 看到的部分内存，不能等同于进程全部 RSS：

```[pseudocode]
malloc_info
  → XML/arena/bin/分配统计
  → 观察分配器保留和空闲状态

mallinfo/mallinfo2
  → 汇总字段(版本/类型差异需核对)
  → 不能覆盖所有 mmap/线程/非glibc分配路径

malloc_stats
  → 输出分配器汇总

malloc_trim / mallopt
  → 请求释放部分可回收空间或调整策略
  → 是否真的归还/收益取决于块布局、arena 和内核
```

Why: 为什么 malloc 统计和 RSS 可能互相矛盾？——**RSS 还包含共享库、线程栈、mmap、页缓存影响和其他分配器路径，malloc 统计只看到自己的 arena/对象**；`mallinfo` 还有 API 版本和溢出/弃用边界，排障应优先核对目标 libc 文档。 [man 3 malloc_info/mallinfo/malloc_trim: 字段和线程安全/版本语义]

比喻锚点: malloc 统计是某个仓库的库存表，RSS 是整座园区的占用表；一个仓库说空闲，不代表园区没有其他租户。 [写作时展开]

### 4. 伪泄漏排查 — RSS 上升不等于对象泄漏

场景提示: 业务负载结束后 RSS 没降，但应用没有明显未释放对象；怎样把真实泄漏、分配器保留、页缓存和 cgroup 压力区分开？ [写作时展开]

关键设计: 使用多层证据和干预实验，而不是只看一个数字：

```[pseudocode]
RSS/PSS/USS 趋势
  → 判断进程/共享/私有驻留变化

malloc_info/mallinfo
  → 分配器是否仍持有大量空闲块

smaps_rollup / smaps
  → 哪类 VMA 在增长

heap profiler/Valgrind/memleak
  → 追踪真实分配与释放路径

malloc_trim(测试环境)
  → 观察可回收空间是否归还
  → 不能把“trim 后 RSS 下降”当作唯一泄漏证明
```

Why: 为什么 trim 后 RSS 下降也不能证明此前是“假泄漏”？——**trim 可能释放缓存/空闲块，但真实泄漏、内存碎片、页驻留和工作集变化可以同时存在**；Valgrind、memleak、heap profiler 的覆盖范围和扰动也不同。生产环境应优先低扰动采样，避免直接 attach gdb 修改进程状态。 [内核: RSS/PSS/USS、cgroup memory、分配器 arena 与 page reclaim 是不同账本]

比喻锚点: 水位上升可能是漏水，也可能是仓库暂存货、潮汐或排水阀关闭；开阀后下降只能说明有可排水部分，不能独自证明漏点。 [写作时展开]

### 5. nice、affinity 与 rlimit — 进程还能使用多少资源

场景提示: 内存没满但线程延迟很高，或程序突然 `EMFILE`/`mlock` 失败；调度和资源限制怎样参与诊断？ [写作时展开]

关键设计: nice/调度策略、CPU affinity 和 rlimit 改变的是不同边界：

```[pseudocode]
nice/setpriority
  → 改变普通调度权重/优先级
  → 不是直接分配固定 CPU 百分比

sched_setaffinity/taskset
  → 限制线程可运行 CPU 集合
  → 可能改善局部性, 也可能造成单核饱和

getrlimit/setrlimit
  → NOFILE/NPROC/CORE/MEMLOCK 等进程资源边界
  → 到达上限时系统调用返回对应错误

实时策略 chrt
  → 改变调度类别/优先级
  → 需权限与严格的饥饿/失控风险评估
```

Why: 为什么绑核和提高优先级可能让整体性能变差？——**它们把 CPU 资源重新分配给一个进程，可能挤压 IRQ、softirq、其他服务或造成 NUMA 远端访问**；rlimit 也可能被 systemd/cgroup/容器上层再次约束。固定“默认 1024 fd”“nice +1 等于固定 10%”都不是通用结论。 [man 2 nice/sched_setaffinity/getrlimit: 调度与资源上限语义]

比喻锚点: 调度和 rlimit 像给工厂分配工位、通行证和仓库额度；给一个团队更多权限可能让它更快，也可能让其他团队完全进不来。 [写作时展开]

### 6. 收束

内存排障闭环：

```[pseudocode]
RSS/延迟/错误异常
  → /proc/PID/maps/smaps 看虚拟映射与驻留
  → malloc_info/heap profiler 看分配器与对象
  → PSI/reclaim/cgroup 看系统压力
  → rlimit/affinity/scheduler 看运行边界
  → 低扰动实验与同 workload 复测
```

**Aha Moment**: "内存问题不是一张 RSS 曲线，而是**地址空间、物理驻留、分配器保留、内核回收、cgroup/rlimit 和调度**多本账的交叉结果；只有对齐这些账本，才能区分泄漏和正常缓存。"
**回答读者三问**: ①maps 面积是否等于物理占用=不等，需看 smaps/RSS/PSS/工作集；②RSS 上升是否就是泄漏=不一定，需结合 allocator/heap/回收证据；③mlock/绑核失败或变慢怎么办=检查权限、rlimit、cgroup、NUMA 和调度副作用。

---

### 核心悬念

**"地址空间和分配器问题搞清楚了；多核线程同时写共享数据时，CPU 如何保证 cache line 一致，atomic、内存序和无锁算法又怎样建立正确性？"**

→ 引出 07-cpp-atomic-lockfree — MESI、内存屏障、CAS 与无锁队列。