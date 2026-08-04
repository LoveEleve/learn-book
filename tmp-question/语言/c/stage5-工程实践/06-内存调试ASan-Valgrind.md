# 内存调试——ASan、Valgrind 与 Sanitizer 家族

> **对应 Layer**：Layer 9（调试与性能优化），参考 C-C++技术专家学习路线.md § Layer 9  
> **参考书章节**：高效C/C++调试 第2-4章 + 第10章（调试工具）  
> **前置依赖**：S4Ch01（系统调用—valgrind 的底层机制）；S5Ch05（GDB—与 sanitizer 配合定位 bug）  
> **主线标准**：C11/C17 + GCC/Clang Sanitizer + Valgrind

---

## 1. 引言：为什么 GDB 不够

S5Ch05 讲了 GDB——你能追踪到崩溃的位置。但如果 bug 不崩溃呢？如果 `malloc` 写了一个字节超出边界，恰好那个字节在合法的内存区域——程序继续运行，直到某个完全不相关的变量被覆盖、2 小时后才挂掉。GDB 无法追踪"2 小时前被踩的内存是谁踩的"。

ASan 和 Valgrind 可以——它们在每次内存访问时插入检查代码。代价是性能（2x-20x），但回报是**2 小时后的崩溃回溯到 2 小时前的写入者**。

---

## 2. ASan（AddressSanitizer）——编译时插桩

### 2.1 基本原理

ASan 在每次内存访问前插入**影子内存检查**——为每 8 字节的真实内存维护 1 字节的影子状态（0=全可访问，1-7=部分可访问，负数=全部不可访问）。对每个 `*p` 操作，编译器生成的代码检查对应的影子字节——如果不可访问→立即报告。

```
真实内存: [  A  ][  A  ][  F  ][  F  ][  F  ]
影子内存: [    0    ][    1    ][   -1    ]
               ↑              ↑           ↑
            全可访问      只1字节可访问   不可访问（已经free）
```

**关键**：ASan 不需要重新编译依赖库（libc 等）——它在自己的运行时环境中拦截 `malloc`/`free` 并维护影子内存。

### 2.2 可检测的 bug 类型

| 类型 | 例子 | ASan 何时报告 |
|---|---|---|
| 堆缓冲区溢出 | `buf[n] = x`（n > 分配大小） | 写入越界时 |
| 栈缓冲区溢出 | `char buf[10]; buf[12] = 'X';` | 写入越界时 |
| Use-After-Free | `free(p); *p = 42;` | 访问已释放内存时 |
| Double-Free | `free(p); free(p);` | 第二次 `free` 时 |
| 栈 Use-After-Return | 返回指向栈变量的指针 | 访问时 |

### 2.3 使用方法

```bash
cat > asan_test.c << 'EOF'
#include <stdlib.h>
int main() {
    int *p = malloc(10 * sizeof(int));
    p[10] = 0;   // 越界写入！只分配了 10 个 int
    free(p);
    return 0;
}
EOF

gcc -g -fsanitize=address asan_test.c -o asan_test
./asan_test
# 输出：
# ==12345==ERROR: AddressSanitizer: heap-buffer-overflow
# WRITE of size 4 at 0x602000000050 thread T0
#     #0 0x4008a2 in main asan_test.c:4
# 0x602000000050 is located 0 bytes to the right of 40-byte region [0x602000000028,0x602000000050)
# allocated by thread T0 here:
#     #0 0x7f123 in malloc (...)
#     #1 0x40088d in main asan_test.c:3
```

**输出解读**：
- 第 4 行：越界写入的具体行
- "0 bytes to the right"：刚好在分配块的右边界外
- "allocated by"：这块内存在哪里分配的——完整的分配-释放-错误使用链

### 2.4 ASan 的性能开销与生产不可用

ASan 的每次内存访问都**无条件插入检查**——性能下降 2x。UAF 检测需要**释放后不立即归还内存**——ASan 将释放的内存放入隔离区而不是返还给分配器——内存额外消耗 2-3x。

**结论**：ASan 是**测试/CI 工具**，不能用于生产环境。生产环境的 UAF/越界检测需要低开销替代方案（见 §5）。

---

## 3. Valgrind——运行时模拟

### 3.1 基本原理

Valgrind 不插入代码——它**模拟 CPU 执行**。程序在 Valgrind 的虚拟 CPU 上运行——每条指令被翻译、每个内存访问被检查、每个系统调用被拦截。

| 工具 | 检测目标 | 性能开销 |
|---|---|---|
| memcheck | UAF / 越界 / 未初始化读 / 内存泄漏 | **10-20x** |
| helgrind | 数据竞争 / 锁顺序错误 | **20-50x** |

### 3.2 memcheck 示例

```bash
cat > leak.c << 'EOF'
#include <stdlib.h>
int main() {
    void *p = malloc(1024);  // 未释放
    return 0;
}
EOF

gcc -g -O0 leak.c -o leak
valgrind --leak-check=full ./leak
# 输出：
# ==12345== 1,024 bytes in 1 blocks are definitely lost
# ==12345==    at 0x... malloc (...)
# ==12345==    by 0x... main (leak.c:3)
```

### 3.3 未初始化读检测

```c
int x;            // 栈变量——未初始化
if (x > 10) {}    // Valgrind 检测到"条件分支依赖未初始化值"
```

这是 Valgrind 的独特优势——ASan 检测"越界/UAF/Double-Free"，但**不检测未初始化的值读取**。Valgrind memcheck 跟踪每个字节是否被初始化。

### 3.4 Valgrind vs ASan 选择

| 维度 | ASan | Valgrind |
|---|---|---|
| 检测方式 | 编译时插桩 | 运行时模拟 |
| 性能开销 | ~2x | 10-20x (memcheck) |
| 需重新编译 | 是（`-fsanitize=address`） | 否（任何二进制都可以） |
| 内存开销 | 2-3x（隔离区） | ~1.5x |
| 未初始化读 | **不检测** | **检测** |
| 内存泄漏 | 程序退出时报告（需 LSAN） | 可以增量检测 |

---

## 4. TSan——数据竞争检测

### 4.1 使用方法

```bash
gcc -g -fsanitize=thread -lpthread race.c -o race_tsan
./race_tsan
# 输出：
# WARNING: ThreadSanitizer: data race
#   Write of size 4 at 0x7b0000 by thread T2:
#   Previous write of size 4 at 0x7b0000 by thread T1:
```

### 4.2 TSan 的实现原理

TSan 编译器为每个内存访问**无条件插桩**——记录线程 ID、锁状态和访问地址。当两个不同线程访问同一地址且无共同的锁保护→报告数据竞争。检测发生在**运行时**——即使竞争不产生可见 bug（当前没有触发），TSan 仍能检测。

### 4.3 TSan 的性能与编译器要求

**开销**：5-15x 性能下降、5-10x 内存增加。**编译器要求**：GCC 4.8+ 或 Clang 3.3+。go 语言的 race detector 也是基于这个原理。

---

## 5. UBSan——未定义行为检测

| 检测类别 | 例子 | 开销 |
|---|---|---|
| 有符号整数溢出 | `INT_MAX + 1` | 几乎免费 |
| 除零 | `x / 0` | 几乎免费 |
| 非法移位 | `1 << 32`（超过 int 宽度） | 几乎免费 |
| 空指针解引用 | `*NULL` | 几乎免费 |
| 对齐违规 | 访问未对齐指针 | 中等 |

```bash
gcc -g -fsanitize=undefined ub_test.c -o ub_test
./ub_test
# 输出：runtime error: signed integer overflow: 2147483647 + 1
```

**UBSan 可以用于生产环境吗？** 可以——`-fsanitize=undefined` 中的整数溢出、除零、移位检查几乎零开销。对齐检查虽有些许开销，但仍远低于 ASan/TSan。

---

## 6. 生产环境替代方案

生产不能跑 ASan/TSan（性能不可接受）。替代方案：

| 工具 | 检测目标 | 生产开销 |
|---|---|---|
| jemalloc profiling | 内存分配热点、泄漏 | ~5% |
| tcmalloc heap profiler | 同上 | ~5% |
| perf record | CPU profiling | <1% |
| eBPF / bpftrace | 系统级追踪 | <1% |

---

## 7. 总结

| 工具 | 检测 | 开销 | 生产可用 |
|---|---|---|---|
| ASan | UAF/越界/Double-Free | 2x | ❌ |
| Valgrind | UAF/越界/未初始化/泄漏 | 10-20x | ❌ |
| TSan | 数据竞争 | 5-15x | ❌ |
| UBSan | 整数溢出/除零/移位 | ~0x | ✅ |

**核心观点**：内存 bug 不是"用 GDB 定位"——GDB 看到的是 bug 的**后果**（崩溃的调用栈），ASan 看到的是 bug 的**起因**（谁在 2 小时前写了越界字节）。技术专家不是"知道 Valgrind 怎么用"，而是"知道 ASan 通过影子内存让每个内存访问多了 1 字节的检查——这个检查让你的程序慢 2 倍但让你从 2 小时的不相关崩溃中回溯到 2 小时前的越界写入者——这就是开发期的安全网与生产期的性能之间的平衡"。

---

## 验收要点

1. ASan 的影子内存机制是什么？每 8 字节真实内存占多少影子内存？
2. ASan 和 Valgrind 的性能开销差异为什么这么大？（插桩 vs 模拟）
3. Valgrind 检测"未初始化读"而 ASan 不检测——为什么？
4. TSan 如何检测数据竞争？为什么它需要 5-15x 开销？
5. UBSan 可检测哪些类未定义行为？哪些可以用于生产环境？
6. ASan 为什么不能用于生产？生产环境的替代方案有哪些？
---

## 面试题

**Q1：ASan 的影子内存机制——每 8 字节真实内存占多少影子字节？**

1 字节影子内存。ASan 将真实内存的每 8 字节映射为影子内存中的 1 字节——状态值：0=全部可访问，1-7=部分可访问（代表该 8 字节组中只有前 K 字节可访问），负数=全部不可访问（已 free 或从未分配）。影子内存的地址 = (真实地址 >> 3) + 固定偏移（64 位下为 0x7fff8000）。

**追问**："为什么是 8 字节？不是 4 或 16？"——8 字节映射到 1 字节影子，内存开销 = 1/8 = 12.5%。4 字节影子→25% 开销——太多。16 字节→6.25% 开销但精度更低。

**Q2：Valgrind 为什么不需要重新编译？**

Valgrind 在启动时用 `PTRACE_ATTACH` 接管目标进程——不是执行原始二进制，而是将二进制加载到自己的虚拟 CPU 中。每条 x86 指令被翻译为 VEX IR（Valgrind 的中间表示），在 VEX IR 中插入内存检查，然后编译回 x86 执行。没有修改目标二进制——所以不需要重新编译。这也是为什么 Valgrind 慢 10-20x——每条指令都要翻译+插桩。
