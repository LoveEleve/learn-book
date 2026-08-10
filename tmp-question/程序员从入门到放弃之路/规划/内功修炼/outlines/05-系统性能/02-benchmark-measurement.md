# 基准测试与测量方法论 — 统计检查 + 编译器干扰 + 计时器

> Cluster B: 5 KPs | 依赖: 01-methodology-foundation | 读者基线: 懂 Amdahl 定律 + 会运行 `perf stat`

---

### 1. 基准测试分类 + 统计检查清单
  - 微基准: 测一个函数/操作(Linux `hackbench`/`sysbench`/`iperf`) → 最可控但最难推断总体 (性能之巅 Ch12 §1-3)
  - 仿真基准: 模拟真实负载(sysbench oltp/TCPC-C/TPC-H) → 折中方案 (CPU性能分析 Ch2 §2-7)
  - 行业标准 + 生产环境: SPEC CPU2017/SPECjbb → 宏观性能回归 → 生产 `wrk2`/`k6` 用真实流量 (CPU性能分析 Ch2 §2-7)
  - 统计检查: 至少3次运行 → 报告中位数非均值(长尾dirty) → 预热(冷缓存/预热JIT) → 变异性来源(CPU频率/DVFS/NUMA)

### 2. 测量陷阱 — 编译器干扰
  - 死变量消除(DE): `int x = work();` → 编译器发现 x 未用 → `work()` 整行消除 → `__attribute__((used))` 或 volatile (深入理解软件性能 Ch2 §1-5)
  - 依赖变量消除: `x = y + z;` 但 y/z 已知 → 编译时常量折叠 → 计时零 → `asm volatile("" : : "r"(x) : "memory")` 加屏障
  - 循环优化干扰: 循环不变代码外提/自动向量化/展开 → 禁用优化 vs 启用的计时差10x → `-O0` vs `-O2` 对比识别
  - 测量检查法: 微基准必须检查汇编 `objdump -d` 或 `perf record` 确认编译器没把被测代码优化掉

### 3. 测量陷阱 — 循环开销 + 内存层次干扰
  - 空循环开销: `for(i=0;i<N;i++)` 本身有分支预测+加法+比较 → 每条循环 0.5-1 cycle → 毫秒级实验可忽略, 纳秒级必须扣除 (深入理解软件性能 Ch2 §1-5)
  - 缓存冷热态: 首次运行(populate cache) vs 重复运行 → 需要"预热+丢弃"后再测 → `echo 3 > /proc/sys/vm/drop_caches`
  - 多次传输时间区分: 时间总量 = 第一次(冷) + 后续(热) → 测的是"冷态延时"还是"稳态吞吐"必须声明 (深入理解软件性能 Ch5)
  - TLB flush + 大页影响: `perf stat -e dtlb_load_misses.miss_causes_a_walk` → 测量时默认 THP 状态要记录

### 4. 软硬件计时器 — 精度+开销选择
  - `gettimeofday()`: 微秒精度, 系统调用开销 → VDSO 加速(无系统调用) → 适合毫秒级测量 (CPU性能分析 Ch1 §1-5)
  - `clock_gettime(CLOCK_MONOTONIC)`: 纳秒精度, 单调递增(不受NTP跳变) → Linux标准基准测试首选
  - `rdtsc/rdtscp`: CPU周期级精度, 但跨核不一致/TSC同步问题(VirtualBox/halt C-state漂移) (CPU性能分析 Ch1 §5)
  - 硬件性能计数器: `perf stat -e cycles,instructions -- ./bench` → PMC 0开销读取 → 用 `rdpmc` 用户态读 PMC

### 5. 收束
  - 基准测试 ≠ 跑一遍取数 — 必须统计检查(中位数/预热/DVFS控制) + 编译器干扰验证(`objdump`) + 缓存态声明
  - 计时器选 `CLOCK_MONOTONIC`(毫秒级)或 `rdpmc`(周期级) → 不同场景选不同工具
  - 测量三原则: 声明被测的是什么(冷/热/平均/P99)、声明干扰已排除(编译器/缓存)、声明可复现条件

---

### 核心悬念
**"计时的数字对了, 但如果 CPU 利用率显示 100% — 这意味着 CPU 真的在'干活'吗？"**

→ 引出 03-IPC/CPI + 缓存 miss + 分支预测 + 火焰图
