# 源码级调优 — 循环/SIMD/缓存布局/机器码 + 多线程优化

> Cluster D: 4 KPs | 依赖: 07-wait-analysis-optimization | 读者基线: 会用 perf annotate + 懂 IPC/缓存行

---

### 1. 循环优化 — 展开/向量化SIMD/分支消除
  - 循环展开(unrolling): `for(i=0;i<N;i+=4) a[i]+=b[i];a[i+1]+=b[i+1];...` → 减少分支+循环变量计算 → 1.5-2x → 太多则ICache压力大 (CPU性能分析 Ch9 §1-5)
  - 自动向量化: `gcc -O3 -march=native -ftree-vectorize` → 检查 `-fopt-info-vec` 报告 → 为什么vectorized不成功(指针别名/struct padding/类型不匹配) (CPU性能分析 Ch9 §1-5)
  - 手动 SIMD intrinsics: `_mm_loadu_ps / _mm_add_ps`(SSE) / `_mm256_loadu_si256`(AVX2) / `_mm512_*`(AVX-512) → data alignment 必须 32/64B
  - 分支消除: 用查找表(256-entry array)或 `cmov` 替换 `if` → 消除分支预测失败惩罚(20cycles) → for random data 可2x加速 (CPU性能分析 Ch10 §1-4)
  - `perf annotate`: 检查热循环是否向量化 → 查看 SIMD 指令占比 → 没看到 v* 指令→为什么没向量化

### 2. 缓存友好数据结构 — 顺序/打包/重排/大页
  - 顺序访问: `array[i]` 是 cache-friendly, 链表 `p = p->next` 是 cache-hostile → 尽量使用紧凑数组/栈分配 (CPU性能分析 Ch8 §1-5)
  - 打包: `struct { 填满热字段, 字段按使用频率排序, 减少padding }` → `pahole` 分析 → `__attribute__((packed))` 当确实需要
  - 数组结构重排: `AoS` (struct of array-of-struct) → `SoA` (array-of-struct fields) → SoA 对 SIMD 友好(GPU style)
  - 动态分配: `malloc` + 首次访问(page fault) → 延迟200ns+(冷) vs 0.5ns(热L1命中) → 大批量alloc的缓存友好: `calloc` 零页复用
  - 大页(Huge Pages): 2MB页降低TLB miss → `echo always > /sys/kernel/mm/transparent_hugepage/enabled` → `perf stat -e dtlb_load_misses` 大页降低70% miss

### 3. 机器码布局 — 基本块/对齐/FDO/ITLB
  - 基本块排列: 将热路径基本块相邻放置 → 热路径紧凑→ICache hit → `__attribute__((hot))`/`__attribute__((cold))` 声明 (CPU性能分析 Ch11 §1-9)
  - 函数拆分: hot-code 在函数头, cold-code(错误处理/init)在函数尾 → ld linker `--function-sections` + `--gc-sections` 丢弃未引用代码
  - 对齐: `-falign-loops=32`/`-falign-functions=32` → 避免 loop body 跨fetch line → 对齐可能增大代码大小(ICache折衷)
  - FDO(Feedback-Directed Optimization): `gcc -fprofile-generate` → 跑代表性负载 → `gcc -fprofile-use -O3` → 编译器自动热/冷函数重新排列
  - BOLT(二进制优化布局): meta/LLVM 的后link优化 → 不重编译 → 根据profiling数据把热基本块聚到一起 → 3-15% 性能提升
  - ITLB: 代码大小超过大页→ITLB miss penalty → `perf stat -e itlb_misses.miss_causes_a_walk` → 拆大函数或使用大页代码内存

### 4. 多线程优化 — 伪共享/扩展性/缓存一致性
  - 伪共享(False Sharing): 两个线程写不同的cache-line字段 ⇒ 缓存一致协议互相invalidate ⇒ cache-line bouncing (CPU性能分析 Ch13 §1-5)
  - 检测: `perf c2c record -p PID -- sleep 10` → `perf c2c report` → `%hitm`(修改命中率)>0.1% 就是伪共享
  - 解决: `__aligned__(64)` 或 `char padding[64]` → 或把频繁冲突的字段分到不同 slot 数组
  - 扩展性(Amdahl): 10核并行但共享锁 → 加速比暴跌 → `perf lock report` 找 Amdahl 的瓶颈锁
  - 缓存一致性: MESI states → 多核同时读=共享(ok), 交替写=inv(costly) → `perf stat -e l2_rqsts.all_rfo`(读所有权请求,是inv信号)
  - Coz 因果剖析: 虚拟加速 → 假想"如果lock的时间减半, 整体吞吐上升多少？" → 不同于 profiling(找热点), coz 找"你想优化的有没有道理"

### 5. 收束
  - 循环优化(展开/SIMD/分支消除) → 编译器不知道你的数据分布, 你可以
  - 缓存友好数据布局(顺序/打包/SoA/大页) → 是 Memory Bound 降到 Memory Wall 的唯一武器
  - FDO/BOLT 机器码布局需要工具链, 但排序+`__attribute__((hot))` 纯人工可做 80% 同样效果

---

### 核心悬念
**"你从 USE 开始, 到 perf 分析 CPU, 到 ftrace/bpftrace 跟踪, 到等待分析 7 维, 到循环/缓存/机器码 — 这条路走完就是系统性能的标准成长路径。"**

→ 引出 06-eBPF — 从 BCC 一行命令到 XDP 网卡直通的内核可编程观测
