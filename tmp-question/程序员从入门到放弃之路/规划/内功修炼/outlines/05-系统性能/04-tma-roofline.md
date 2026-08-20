# TMA 与 Roofline — 如何判断 CPU 是算不动还是喂不饱

> Cluster B→D 桥接: 4 KPs | 依赖: 03-cpu-perf-flamegraph | 读者基线: IPC/CPI、perf、缓存与火焰图
> 读者处境: 03 篇已经告诉你函数在哪、cache miss 是否高；本篇继续下钻：CPU 周期到底浪费在取指、错误投机、后端等待，还是算法已经撞上算力/带宽天花板？
> 打开新视角: TMA 解释**流水线槽位花在哪里**，Roofline 解释**算法受算力还是内存带宽限制**，两者都依赖具体 CPU、PMU 和工作负载验证

---

### 概念依赖链

```
03 IPC/cache/perf → 本篇: TMA/PMU/PEBS/Roofline
  ├─ §1 TMA(Topdown 四类 pipeline slots)
  ├─ §2 PMU/PEBS/IBS/SPE(硬件采样证据)
  ├─ §3 Roofline(算术强度与带宽/算力上限)
  └─ §4 LLVM-MCA/编译器报告(静态解释与实际计数结合)
先讲: 周期分类 → 硬件采样 → 算法天花板 → 静态/动态交叉验证
后续依赖: 05-memory-disk-network-observability(从微架构到子系统观测)
```

### 叙事顺序

1. 问题引入——IPC 很低，究竟是前端取指慢、后端等内存，还是分支猜错？（**Aha: TMA 把周期分成流水线槽位，Roofline 再把算法放到算力/带宽坐标上**）
2. TMA——Retiring/Bad Speculation/Frontend Bound/Backend Bound
3. PMU 与精确采样——计数器如何获得证据
4. Roofline——算术强度决定撞哪面墙
5. LLVM-MCA/编译器报告——把静态预期和硬件事实对齐
6. 收束——微架构判断到优化方向

### 1. TMA — 自顶向下看流水线槽位

场景提示: CPU 利用率 100%、IPC 低，怎样判断时间是花在有效退休指令，还是花在取指、错误投机和后端等待？ [写作时展开]

关键设计: Intel Top-down Microarchitecture Analysis 把 pipeline slots 按一级类别分配；具体事件和层次依赖 CPU 微架构与 perf 支持：

```[pseudocode]
Topdown L1:
  Retiring
    有效退休的指令工作
  Bad Speculation
    错误投机、错误路径恢复
  Frontend Bound
    取指/指令缓存/解码供给不足
  Backend Bound
    执行端口、加载/存储、内存等后端等待

L1 → L2 → L3:
  逐层把 Frontend/Backend 等大类拆成更具体原因
```

Why: 为什么 TMA 不能直接用一组固定 event 编码跨 Intel、AMD、ARM？——**PMU 事件、计数公式、层次模型和工具支持都与微架构相关**：同名指标在不同 CPU 上可能不存在或语义不同。`perf stat -M TopdownL1` 能否工作要先看 `perf list` 和目标 CPU 支持，不能把 event `0x9c/0xb1` 当成通用接口。 [内核: perf 负责访问 PMU，但 TMA 公式通常由 perf 事件描述和 CPU 厂商模型共同定义]

比喻锚点: TMA 像工厂把每分钟分成四类：成品下线（Retiring）、返工（Bad Speculation）、等零件到位（Frontend）、等机器/材料（Backend）；先知道时间花在哪类，再继续查具体工位。 [写作时展开]

### 2. PMU 与 PEBS/IBS/SPE — 计数器之外的精确证据

场景提示: cache miss 计数告诉你很多 miss，但哪条指令、哪个地址附近最常发生？ [写作时展开]

关键设计: PMU 提供计数与采样能力，精确采样扩展可把事件更接近地关联到指令或执行上下文：

```[pseudocode]
通用 PMU:
  cycles / instructions / cache misses / branches
  → 计数或溢出采样

Intel PEBS:
  对部分事件提供更精确的采样记录

AMD IBS:
  记录取指/执行相关属性

ARM SPE/BRBE:
  SPE 统计执行与内存行为
  BRBE 记录分支历史(需硬件/内核支持)

perf:
  先检查 perf list/CPU 支持/权限
  再选择 event、采样频率、调用栈和过滤条件
```

Why: 为什么“精确采样”不等于“每次事件都完整记录”？——**硬件采样有支持范围、采样周期、过滤、buffer 和 skid 等限制**：事件发生的位置可能与记录指令存在偏差，PMU 还会受到 multiplexing、虚拟机和权限影响。精确采样是提高归因质量，不是消除测量不确定性。 [man 2 perf_event_open: PMU 事件、采样、权限和 buffer 行为依赖内核/硬件]

比喻锚点: 普通计数器像统计工厂一天发生多少次故障，PEBS/IBS/SPE 像抽取带时间位置的故障样本；样本更具体，但仍不是每次故障的录像。 [写作时展开]

### 3. Roofline — 算术强度决定撞算力墙还是带宽墙

场景提示: 一个循环 CPU 很忙，应该向量化，还是应该改变数据布局减少内存流量？ [写作时展开]

关键设计: Roofline 用算术强度把算法工作量和硬件上限放在同一张图：

```[pseudocode]
横轴: Arithmetic Intensity = 计算操作数 / 内存流量(bytes)
纵轴: 可达到性能(如 FLOP/s)

带宽屋顶:
  Performance <= Memory Bandwidth × Arithmetic Intensity

计算屋顶:
  Performance <= Peak Compute Throughput

拐点:
  带宽屋顶与计算屋顶交汇处
  → 左侧通常受内存带宽限制
  → 右侧通常受算力/执行吞吐限制
```

Why: 为什么 Roofline 不是“画出两条线就完成了性能分析”？——**算术强度、有效内存流量、带宽和峰值算力都要用目标 workload/硬件测量或可信规格估计**：缓存层级、写回、NUMA、向量宽度、频率、混合指令和数据类型都会改变坐标。Roofline 给出上限与方向，不直接告诉你哪一行代码需要改。 [内核: perf/uncore 事件可辅助测带宽，但事件名和可观测层级依赖 CPU；STREAM 结果也依赖线程/NUMA 配置]

比喻锚点: Roofline 像工厂产能图：横轴表示每吨原料能做多少加工，纵轴表示每秒产量；低强度时原料运输限制产量，高强度时机器加工速度成为上限。 [写作时展开]

### 4. LLVM-MCA 与编译器报告 — 静态模型必须和动态事实对照

场景提示: perf 说 Backend Bound 高，如何判断是指令端口、依赖链，还是编译器没有向量化？ [写作时展开]

关键设计: 静态工具预测一段汇编的吞吐/资源压力，编译器报告解释优化决策，perf/PMU 验证真实运行：

```[pseudocode]
LLVM-MCA
  输入: 已生成汇编 + target CPU
  输出: 预计吞吐、指令延迟、资源/端口压力

编译器报告
  GCC: -fopt-info...
  Clang: -Rpass / -Rpass-missed / -Rpass-analysis
  → 为什么没有内联/向量化/展开

交叉验证:
  静态预测 → perf TMA/PMU → benchmark 延迟/吞吐
  不一致时检查输入、频率、缓存、分支、混合 workload
```

Why: 为什么 LLVM-MCA 的结果不能直接当作真实吞吐？——**它通常分析一段理想化指令序列，真实程序还受缓存、分支、其他线程、内存层次、调度和输入数据影响**；相反，perf 看到 Backend Bound 也不能独自说明编译器哪一项决策错。静态和动态证据必须互相约束。 [内核: PMU 只能观察运行时事件，LLVM-MCA 属于用户态静态分析，两者证据边界不同]

比喻锚点: LLVM-MCA 像在实验室用机器图纸计算产能，perf 像在真实工厂装传感器；图纸与现场不一致时，要检查原料、工人和机器状态。 [写作时展开]

### 5. 收束

微架构分析闭环：

```[pseudocode]
IPC/CPI 异常
  → TMA 判断 Retiring/Frontend/Backend/Speculation
  → PMU/PEBS/IBS/SPE 获取更具体证据
  → Roofline 判断计算/带宽方向
  → 汇编/LLVM-MCA/编译器报告检查实现
  → benchmark 验证真实收益
```

**Aha Moment**: "TMA 解释周期落在哪类流水线槽位，Roofline 解释算法撞在哪面硬件屋顶；前者偏微架构归因，后者偏算法-硬件上限。两者都不是魔法答案，必须结合目标 CPU、PMU 和 workload。"
**回答读者三问**: ①IPC 低如何继续下钻=TMA 分类槽位；②哪条指令在等=精确采样/调用栈/地址证据；③优化算力还是内存=用 Roofline 算术强度与实测带宽/算力判断。

---

### 核心悬念

**"CPU 微架构已经定位了算力和内存瓶颈，但线上性能还可能卡在内存回收、磁盘队列、网络丢包与系统调用；如何用子系统观测工具把这些资源逐一拆开？"**

→ 引出 05-memory-disk-network-observability — 内存、磁盘、网络观测工具链。