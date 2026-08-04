# 性能分析——perf、FlameGraph 与 gprof

> **对应 Layer**：Layer 9（调试与性能优化），参考 C-C++技术专家学习路线.md § Layer 9  
> **参考书章节**：高效C/C++调试 第5章（优化代码上的调试）  
> **前置依赖**：S3Ch02（CPU 缓存、MESI）；S4Ch03（epoll）；S5Ch06（ASan—perf 不检测内存，ASan 不测性能）  
> **主线标准**：C11/C17 + Linux perf_events

---

## 1. 引言：为什么用 perf 而不是 `gettimeofday` 包围代码？

`gettimeofday` 能告诉你一个函数执行了多久——但不能告诉你**为什么**它花时间。是 cache miss？branch mispredict？还是内存带宽不够？这些问题的答案不在 C 代码中——在 CPU 的 PMU（Performance Monitoring Unit，性能监控单元）中。

perf 是 Linux 内核的 `perf_events` 子系统的前端——它读取 PMU 的硬件计数器，把"这个函数为什么慢"的答案从 CPU 直接反馈给你。本章从计数器、采样、火焰图三条递进路径教你用 perf 定位 C 程序性能瓶颈。

---

## 2. perf stat——硬件计数器

### 2.1 基础用法

```bash
perf stat ./myapp
# 输出：
#  5,234,567  instructions          # 指令数
#    842,345  cache-misses          # L1/L2/L3 缓存缺失
#     12,345  branch-misses         # 分支预测错误
#      0.345  seconds time elapsed
```

**每个计数器的含义**：

| 计数器 | 含义 | 高值意味着什么 |
|---|---|---|
| `instructions` | 执行的指令总数 | 不一定是性能指标——看看 IPC |
| `cycles` | CPU 周期总数 | 直接的时间指标 |
| **`IPC`**（instructions per cycle） | `instructions / cycles` | <1 = 等待（memory/cache），>1 = 超标量执行好 |
| `cache-misses` | 在所有缓存层中未命中的 load | **性能瓶颈 #1**——每次 miss ~50-200 ns（L3）或 ~100 ns（主内存） |
| `cache-references` | 总 load 次数 | `misses/references` = 缓存缺失率 |
| `branch-misses` | 分支预测错误 | **性能瓶颈 #2**——每次 miss ~15-20 cycles（刷新流水线） |
| `context-switches` | 进程被调度出去的次数 | 高值→CPU 过载或 I/O 过多 |
| `page-faults` | 缺页中断 | 高值→内存访问模式差（mmap 后没 MAP_POPULATE） |

### 2.2 实验——两个版本的对比

```bash
cat > perf_test.c << 'EOF'
#include <stdlib.h>
// 版本 A：cache-friendly——顺序访问
void sum_rows(int *matrix, int n, long *result) {
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            result[i] += matrix[i * n + j];  // 行优先——连续内存
        }
    }
}
// 版本 B：cache-hostile——跳跃访问
void sum_cols(int *matrix, int n, long *result) {
    for (int j = 0; j < n; j++) {
        for (int i = 0; i < n; i++) {
            result[i] += matrix[i * n + j];  // 列优先——跨行跳跃
        }
    }
}
EOF
gcc -O2 -DN=1024 perf_test.c -o perf_test
perf stat -e cache-misses,instructions ./perf_test
# 版本 B 的 cache-misses 比版本 A 高 10-100 倍
```

---

## 3. perf record + perf report——采样找热点

### 3.1 采样原理

perf 不是插桩——它以固定频率（默认 4000 Hz）中断 CPU，记录中断时的**指令指针（IP）**和**调用栈**。函数执行越久，被采样的概率越高→采样计数越高→性能热点。

```bash
perf record -F 99 -g ./myapp      # -F 99 = 99 Hz 采样, -g = 记录调用栈
perf report                        # 交互式查看热点
# 输出：按采样次数排序的函数列表
#   45.2%  myapp  slow_function
#   23.1%  myapp  another_fx
#    8.3%  libc   __memcpy_avx_unaligned
```

**命令解释**：
- `-F 99`：99 Hz——接近 100 Hz 但不与系统 tick（通常 100/250/1000 Hz）对齐，避免采样偏差
- `-g`：记录调用栈——不仅知道在哪个函数，还知道**怎么到那里的**（调用栈）

### 3.2 附加到运行中的进程

```bash
perf record -F 99 -g -p 12345 -- sleep 30  # 采样 PID 12345 30 秒
perf report
```

### 3.3 perf top——实时查看

```bash
perf top -p 12345   # 实时显示 PID 12345 的热点函数——类似 top 但看的是 CPU 而非进程列表
```

---

## 4. FlameGraph——把调用栈变成"火"

### 4.1 生成火焰图

```bash
# 1. 采样
perf record -F 99 -g ./myapp

# 2. 生成火焰图数据
perf script | stackcollapse-perf.pl > folded.txt

# 3. 生成 SVG
cat folded.txt | flamegraph.pl > flame.svg
```

火焰图的规则：
- **X 轴 = 采样计数**——越宽越热
- **Y 轴 = 调用栈深度**——底部是入口，顶部是叶子函数
- **颜色 = 随机**——无特殊含义

### 4.2 如何读火焰图——一个完整的例子

```
                     malloc     ← 最热——占了整个宽度的 60%
                  ┌────┴────┐
               slow_fn     fast_fn
            ┌───┴───┐    ┌───┴───┐
         compute   sort  parse  validate
```

- `compute` 在 `malloc` 下的哪一层——它的调用栈是 `main → slow_fn → compute → malloc`
- `malloc` 占 60%——compute 和 sort 各调用 malloc——需要看哪一层调用最多
- 同一个函数（`malloc`）可能出现在不同位置——因为它的不同调用栈

### 4.3 火焰图对比——优化前后

优化前：`malloc` 占据 60% 宽度。优化后（改内存池-见 S4Ch01 §6 内存分配器部分）：`malloc` 缩小到 10%，`compute` 的 CPU 时间占比提升——实际计算占比变高了。

---

## 5. gprof——函数调用次数 + 耗时

### 5.1 基本用法

```bash
gcc -pg -O2 myapp.c -o myapp    # -pg = 插桩——每个函数入口记录调用者+时间
./myapp
gprof ./myapp gmon.out
```

gprof 输出一个表格：
```
% time  cumulative  self  calls  self ms/call  total ms/call  name
 45.2      4.52     4.52  1000       4.52          6.78       compute
 23.1      6.83     2.31  1000       2.31          2.31       sort
```

**gprof 的局限**：
- **不能用于动态库**——`-pg` 只为编译时的 TU 插桩，不跟踪动态库调用
- **不能用于多线程**——只有一个 `gmon.out`，多线程覆盖
- **不能用于信号处理器**——信号中断时 gprof 的插桩失效

**perf 替代 gprof**：`perf record -g` + `perf report` 不受这些限制——sampling 而非插桩，对动态库、多线程、信号处理器都有效。

---

## 6. 生产环境的性能分析策略

1. **perf stat**——先看大图：IPC、cache-misses、branch-misses。如果 IPC < 0.5 → cache/branch 瓶颈。如果 context-switches 高 → CPU 过载
2. **perf record + 火焰图**——定位具体的热点函数和调用栈
3. **perf annotate**——在函数内部看**哪条指令**最热（`perf annotate hot_function`）→ 汇编级分析
4. **根据发现改代码**：cache-friendly 布局（行优先）、减少分支（无分支算法）、预取（`__builtin_prefetch`）
5. **再次 profile**——确认改进效果

---

## 7. perf 自身的生产陷阱

**高负载下采样精度下降**：perf 依赖 PMU 中断——如果 CPU 已经被其他中断淹没（网络/磁盘 I/O 在高负载下），PMU 中断的处理可能被延迟或丢弃。采样次数不等于实际执行次数——高并发服务器的 perf 数据可能存在系统性偏差。

**多核采样的 NUMA 效应**：`perf record -a` 在所有 CPU 上同时采样——每个 CPU 上的采样独立。如果函数被迁移到不同核心，采样次数被分散——热点函数的采样次数可能被低估。`perf stat --per-core` 分别查看每个核心的计数。

**perf 自身的内存开销**：`perf record -g` 记录调用栈——每个采样写入约 8KB 的调用栈数据到 perf 缓冲区。如果采样频率高且运行时间长→perf 数据文件可能达到 GB 级别→磁盘 I/O 影响被测程序性能。

---

## 8. 总结

| 概念 | 核心知识 |
|---|---|
| `perf stat` | 硬件计数器—IPC/cache-miss/branch-miss—回答"为什么慢" |
| `perf record` + `report` | 采样—99 Hz—按函数排序的热点 |
| FlameGraph | SVG 火焰图——X=宽度=热度，Y=调用栈深度 |
| gprof | 插桩—`-pg`—函数调用次数+耗时；不能用于动态库/多线程 |
| IPC < 0.5 | CPU 在等待内存/cache—不是计算瓶颈 |

**核心观点**：性能分析不是"加 printf 看看哪里慢"——这是猜测。性能分析是**读 CPU 的硬件计数器**：每条指令都有 cache 命中/未命中记录、每个分支有预测正确/错误记录——CPU 已经告诉你答案，perf 只是把这个答案翻译成人类可读的语言。

---

## 验收要点

1. `perf stat` 的 `IPC` 代表什么？`IPC < 0.5` 意味着什么？
2. `perf record -F 99` 为什么选 99 Hz 而不是 100 Hz？——避免与系统 tick 对齐导致的采样偏差
3. 火焰图的 X 轴和 Y 轴各代表什么？同一个函数为什么会出现在多个位置？
4. gprof 的三个主要局限是什么？perf 如何克服？
5. 如何用 `perf annotate` 找到具体的热点 CPU 指令？
6. 如何采样一个已经运行中的进程（不需要重新启动）？

---

## 面试题

**Q1：perf 是采样还是插桩？为什么选采样？**

perf 使用**硬件中断采样**——PMU 每 N 个事件（如每 99 个周期）触发一次中断，记录当时的指令指针。采样不修改被测代码——零插桩开销，适用于生产环境（<1% 性能影响）。插桩（如 gprof 的 `-pg`）为每个函数入口/出口插入计数代码，修改了代码→可能改变缓存布局→测量的不是你真正的程序。

**追问**："99 Hz 为什么不会引起时钟偏差？"——系统 tick 通常是 100/250/1000 Hz 的整数倍。如果 perf 采样也设为 100 Hz，可能与系统 tick 在每个 clock edge 对齐 → 某些代码路径被系统性过采样或欠采样。99 Hz 与任何系统 tick 都不同步 → 采样分布均匀。

**Q2：一个程序 IPC < 0.5，说明什么？怎么修？**

IPC < 0.5 说明 CPU 大部分时间在等待（memory stall）而非执行。优先检查 `cache-misses`——大概率是内存访问模式差（顺序 vs 随机）。修复：行优先的循环遍历、使用 `__builtin_prefetch`、增大 TLB 覆盖范围（用大页）。
