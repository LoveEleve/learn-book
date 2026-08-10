# TMA + 屋顶线 + 硬件采样 — 微架构性能的天花板

> Cluster B→D 桥接: 4 KPs | 依赖: 03-cpu-perf-flamegraph | 读者基线: 懂 IPC/CPI + perf 工具

---

### 1. TMA 自顶向下微架构分析
  - 四条 pipeline slot: Retiring(有效退役) + Bad Speculation(错误投机) + Frontend Bound(取指/解码等) + Backend Bound(存储访问等) → 四种槽位总和=4 (CPU性能分析 Ch6 §1-2)
  - Intel TMA: 0x9c(Retiring) + 0x9c(umask=0x01:Frontend) + 0xb1(BadSpec) + Backend=1-前三 → `perf stat -M TopdownL1`
  - AMD 等效: `ibs_op` → bad speculation + frontend + backend 细分 → L3 miss/L1 miss/cycle stall 分别定量 (CPU性能分析 Ch6 §1-2)
  - ARM 等效: Neoverse N1 SPE → `perf stat -e arm_spe_0` → spe-fetch/operation filter 读的分发/提交/后端 ID
  - TMA 层次化: L1四维 → L2 (Backend Bound 分解为 Memory Bound + Core Bound) → L3 (Memory Bound 再分解为 L1/L2/L3/DRAM bound) → 精确到子系统

### 2. PMU 性能监控单元 + 硬件采样
  - PMU: 每个核心的几个通用计数器和固定计数器 → 计数事件(cycles/instructions/misses) → `cpuid` 查看counters数量 (CPU性能分析 Ch4 §1-4)
  - 固定计数器(Fixed counters): instructions_retired + cpu_clk_unhalted.thread(不动) → LBR记录最后N次分支(16/32条) (CPU性能分析 Ch6 §2-3)
  - PEBS (Intel Precise Event Based Sampling): 事件发生位置精确 → `perf record -e cpu/event=0xd1,umask=0x01,pp/` → 用于定位高 miss 指令 (CPU性能分析 Ch6 §2-3)
  - IBS (AMD Instruction Based Sampling): op fetch/op → 微操作取指+执行全部属性 → `perf record -e ibs_op//pp`
  - BRBE (ARM Branch Record Buffer Extension) / SPE (Statistical Profiling Extension): 同 PEBS → `perf record -e arm_spe_0/branch_filter=1/` → 也是精确事件位置

### 3. 屋顶线模型 (Roofline) — CPU bound vs Memory bound
  - 横轴: 算数密集度(FLOP/byte) → 纵轴: 可达到的性能(GFLOP/s) → 两条线: 算力峰值 + 内存带宽 × 算数密集度 (CPU性能分析 Ch5 §5-7)
  - 内存带宽天花板: `sudo perf stat -e uncore_imc/data_reads/` 或 `STREAM` benchmark → L3/DRAM 带宽最大值
  - 算力天花板: `perf stat -e fp_arith_inst_retired.*` 浮点 + `SIMD` 倍增 → Intel VNNI/AVX-512 加倍
  - 分类: 点在竖线左 = Memory Bound(改数据布局/缓存效率) → 点在横线下方 = CPU Bound(向量化/展开/流水线)
  - Roofline 实践: `Intel Advisor` (Intel) → `likwid-bench` (AMD) → `perf stat -e LLC-load-misses,instructions`

### 4. 静态分析 + 编译器优化报告
  - `perf record -e cycles -M TopdownL1 -- ./prog` 即得 L1 TMA → `perf report` 展开到 L2 按模块 (CPU性能分析 Ch5 §7)
  - LLVM-MCA: `llvm-mca --mtriple=x86_64-unknown-unknown -mcpu=skylake code.S` → 静态分析循环吞吐+瓶颈
  - nawk/musl 优化案例: 改数据布局将 Memory Bound 转为 CPU Bound → 2x 吞吐
  - 编译器优化报告: `gcc -fopt-info-all code.c` / `clang -Rpass=.*` → 编译器为什么没向量化/展开/内联

### 5. 收束
  - TMA 将"CPU 慢"这个模糊说法转换为 Retiring/BadSpec/Frontend/Backend 四个精确水位
  - PMU+PEBS/IBS/SPE 让你看到"哪条指令在等什么" — 从函数级到指令级精确定位
  - Roofline 区分"算不动"(CPU bound)vs"喂不饱"(Memory bound)→两个完全不同的优化方向

---

### 核心悬念
**"Roofline 告诉你天花板、TMA 告诉你瓶颈槽位 — 但内存/磁盘/网络三个子系统互相影响, 怎么从 USE 找到的资源瓶颈, 逐一深度观测？"**

→ 引出 05-内存/磁盘/网络观测工具链
