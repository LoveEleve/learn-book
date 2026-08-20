# CPU 性能分析 — IPC/CPI、缓存、分支预测与火焰图如何拼出根因

> Cluster B: 5 KPs | 依赖: 02-benchmark-measurement | 读者基线: 会运行 `perf stat`，理解 cycles/instructions
> 读者处境: 02 篇已经证明测量数字可信；本篇进一步回答：CPU 100% 忙时，究竟是在执行有效指令，还是在等待缓存、分支、内存、锁或内核调度？
> 打开新视角: CPU 性能分析要把**利用率 → IPC/CPI → 缓存/分支/流水线 → 调用栈**逐层下钻，单个百分比无法解释根因

---

### 概念依赖链

```
02 benchmark/计时 → 本篇: CPU 微架构与调用栈分析
  ├─ §1 IPC/CPI/用户态内核态(先判断 CPU 是否有效工作)
  ├─ §2 cache/TLB(数据访问等待)
  ├─ §3 branch prediction(控制流等待)
  ├─ §4 perf stat/record/report(采集证据)
  └─ §5 on/off-CPU/differential flamegraph(定位代码路径)
先讲: 指标 → 数据/控制流瓶颈 → perf 采集 → 火焰图归因
后续依赖: 04-tma-roofline(自顶向下微架构与算力/带宽上限)
```

### 叙事顺序

1. 问题引入——CPU 利用率 100% 时，为什么接口仍然很慢？（**Aha: busy 不等于 productive，IPC/CPI 用来判断周期到底花在执行还是等待**）
2. IPC/CPI 与用户态/内核态——先判断“忙在哪里”
3. cache/TLB——数据访问为什么拖住流水线
4. 分支预测——控制流为什么造成 flush
5. perf 工具链——如何采集统计与调用栈
6. on/off-CPU 火焰图——把计数器连接到函数
7. 收束——从指标到根因

### 1. IPC/CPI — 利用率高不代表执行效率高

场景提示: 一个进程占满 CPU，但吞吐没有提高；如何区分它在执行大量指令，还是大部分周期在 stall？ [写作时展开]

关键设计: IPC/CPI 把“周期”和“退休指令”关联起来：

```[pseudocode]
IPC = instructions retired / CPU cycles
CPI = CPU cycles / instructions retired

perf stat -e cycles,instructions ./program
  → cycles/instructions
  → 计算 IPC 或 CPI

结合:
  %usr/%sys → 时间主要在用户态还是内核态
  context-switches/interrupts → 是否存在调度/中断压力
```

Why: 为什么 IPC 不能独立当作“性能分数”？——**不同指令、不同微架构、不同工作负载的 IPC 上限不同**：低 IPC 可能来自 cache miss、依赖链、分支错误，也可能是正常的复杂指令；高 IPC 也不代表延迟或吞吐满足业务。IPC 是下钻入口，不是最终根因。 [内核: perf 的 cycles/instructions 计数受 CPU PMU、采样模式、虚拟化和 multiplexing 影响]

比喻锚点: CPU 利用率像工厂开工时间，IPC 像每小时真正完成的产品数；机器一直响不代表产线没有在等原料。 [写作时展开]

### 2. Cache 与 TLB — 数据为什么会让流水线停下来

场景提示: 两段算法指令数量接近，为什么一个访问连续数组很快，另一个随机访问却慢很多？ [写作时展开]

关键设计: 缓存和地址转换层次决定数据从哪里来，延迟和容量是具体 CPU 的属性，不能写死成所有机器通用常数：

```[pseudocode]
CPU load/store
  → L1 cache 命中?
  → L2/LLC 命中?
  → DRAM
  → cache line refill

虚拟地址转换:
  TLB 命中?
  → page table walk
  → PTE/页表层级访问

perf 方向:
  cache-references/cache-misses
  LLC-loads/LLC-load-misses
  dTLB-load-misses 等
```

Why: 为什么顺序访问通常比随机访问友好？——**硬件预取、空间局部性和 cache line 会让相邻数据更可能提前进入缓存**；随机访问既难预取，又可能产生更多 TLB miss。伪共享则是多个核修改同一 cache line 的不同变量，引发 cache line bouncing；它和普通 cache miss 需要区分。 [内核: cache line、TLB、NUMA 和页分配策略共同影响内存访问，具体容量/周期必须查看目标 CPU 文档]

比喻锚点: L1/L2/LLC/DRAM 像办公桌、房间、楼层仓库和远程仓库；数据越远，取货等待越长；TLB 则是地址索引卡。 [写作时展开]

### 3. 分支预测 — 控制流猜错会丢掉流水线工作

场景提示: 同一段 `if` 代码，对已排序数据和随机数据的运行时间为什么可能明显不同？ [写作时展开]

关键设计: CPU 根据历史和模式预测分支方向，预测错时需要清理错误路径上的推测执行并重新取指：

```[pseudocode]
if (condition)
  → predictor 预测 taken/not-taken
  → 按预测路径提前取指/执行

预测正确:
  流水线继续
预测错误:
  flush/recovery
  → 重新从正确目标取指

perf stat:
  branches
  branch-misses
  miss rate = branch-misses / branches
```

Why: 为什么不能看到 branch miss 就一律改成无分支代码？——**条件移动、查表、SIMD、数据重排各有成本，分支本身也可能是最便宜的选择**；优化要结合分支可预测性、数据分布和汇编验证。0.5% 不是所有 CPU/程序的通用“高”阈值。 [内核: 分支预测属于 CPU 微架构，perf 事件名称与可用性依赖架构和 PMU]

比喻锚点: 分支预测像流水线提前猜下一站；猜对就不停，猜错就得拆掉已经铺出的路重新铺。 [写作时展开]

### 4. perf 工具链 — 从计数器到调用栈

场景提示: `perf stat` 告诉你 cache miss 高，但哪个函数在制造这些 miss？ [写作时展开]

关键设计: perf 工具链从“总量计数”逐步走向“调用路径归因”：

```[pseudocode]
perf stat
  → cycles/instructions/cache/branch/context-switch 等总量

perf record
  → 采样 CPU 或硬件事件 + 调用栈

perf report
  → 按符号/调用路径聚合热点

perf annotate
  → 对热点函数看源码/汇编与采样分布

perf script
  → 原始样本
  → stackcollapse-perf.pl
  → FlameGraph
```

Why: 为什么采样频率越高不一定越好？——**采样本身有开销，过高可能扰动 workload；过低则可能漏掉短生命周期热点**。调用栈还依赖符号、帧指针/DWARF、编译选项和权限；火焰图宽度代表样本占比，不是函数执行时间的精确逐次累加。 [man 1 perf-record/perf-report: 事件、采样频率、调用栈和符号化必须结合实验环境解释]

比喻锚点: `perf stat` 像工厂总表，`perf record` 像抽样摄像头，`perf report/火焰图` 像把摄像头画面按工位聚合，告诉你拥堵集中在哪条生产线。 [写作时展开]

### 5. On-CPU、Off-CPU 与 Differential Flame Graph

场景提示: CPU 火焰图看起来不热，但请求仍然很慢；它可能在等待锁、I/O 或网络，怎样看到这部分时间？ [写作时展开]

关键设计: 不同火焰图回答不同时间问题：

```[pseudocode]
On-CPU flame graph
  采样线程正在 CPU 上执行时的调用栈
  宽度 ≈ CPU 样本占比
  → 找真正消耗 CPU 的路径

Off-CPU / wait analysis
  观察线程被调度出去、等待锁/I/O/条件的时间
  → 找“没在 CPU 上但请求仍在变慢”的路径

Differential flame graph
  对比基线与回归样本
  → 新增样本/消失样本提示开销变化
```

Why: 为什么 on-CPU 火焰图不能解释所有延迟？——**线程等待时不在 CPU 样本里**：锁竞争、磁盘、网络、调度和 sleep 需要 off-CPU/tracepoint 等另一类证据。Differential 图也只显示样本分布差异，仍要控制 workload 和采样条件，不能把颜色直接当作因果证明。 [内核: sched_switch、锁事件、I/O tracepoint 等可帮助构建 off-CPU 等待时间线]

比喻锚点: on-CPU 是拍正在工作的工位，off-CPU 是拍排队和等材料的工位；两张图合起来才知道工厂为什么交付慢。 [写作时展开]

### 6. 收束

CPU 性能下钻闭环：

```[pseudocode]
业务变慢
  → perf stat: cycles/instructions/IPC/cache/branch
  → 判断执行效率与等待类型
  → perf record/report: 定位调用路径
  → on-CPU: CPU 时间花在哪
  → off-CPU: 等待时间花在哪
  → annotate/汇编/代码验证具体优化
```

**Aha Moment**: "CPU 利用率只告诉你机器忙不忙，IPC/CPI 告诉你每个周期产出多少，cache/branch 解释流水线为何停，火焰图再把这些现象连接到具体函数；**性能分析是逐层缩小解释空间，不是找一个神奇指标**。"
**回答读者三问**: ①IPC 低说明什么=周期没有有效退休足够指令，但还需继续找 cache/branch/依赖根因；②cache miss 和 branch miss 怎么定位=PMU 计数器结合 perf 调用栈；③请求慢但 CPU 不高怎么办=转向 off-CPU/等待与 I/O/锁分析。

---

### 核心悬念

**"IPC/CPI 和火焰图告诉你哪里慢，但怎样按 Frontend/Backend、内存墙、计算峰值和带宽上限系统判断 CPU 到底被哪类瓶颈限制？"**

→ 引出 04-tma-roofline — TMA 自顶向下分析与 Roofline 模型。