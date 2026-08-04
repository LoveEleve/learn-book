# Stage 1：核心语法

> **本 stage 定位**：C 语言学习路线的第一阶段，打基础。对应 `C-C++技术专家学习路线.md` 的 **Layer 0-2**（C 基础与本质 + 内存模型 + 类型系统）+ **Layer 1 §1.2.9 C 安全编程**。
>
> **预计时间**：4 周（每日 2-3 小时投入）
>
> **完成本 stage 后**：能讲清楚 C 的设计哲学、编译模型四阶段、进程内存布局、指针本质、malloc 实现原理、对齐规则、严格别名、volatile 的真实含义、5 大 C 漏洞类型与防御手段。

---

## 1. 学习目标

完成本 stage 后，应能达成以下 6 个目标：

1. **能讲清楚 C 在技术栈中的位置**（OS / 数据库 / 中间件 / 运行时 / 嵌入式都用 C）+ **C 的 5 大设计哲学**（程序员至上 / 显式胜于隐式 / 零开销抽象 / 保持小 / 稳定性优先）
2. **能用 `gcc -E/-S/-c` 完整解释编译四阶段**（预处理 → 编译 → 汇编 → 链接）+ 用 `strace` 分析程序实际 syscall
3. **能讲清楚进程内存布局**（text/data/bss/heap/stack）+ 指针本质（地址 + 类型 + 步长）+ 数组退化规则
4. **能讲清楚 malloc/free 实现原理**（ptmalloc 的 arena / bin / fastbin / small bin / large bin / mmap 阈值）
5. **能解释对齐规则 / 严格别名规则 / volatile 的真实含义**（不是线程同步原语）
6. **能复现并修复一个 C CVE**（如心脏出血）+ 配置完整的加固编译选项

---

## 2. 对应 Layer

| Layer | 主题 | 本 stage 章节 |
|---|---|---|
| Layer 0 | C 语言基础与本质 | `01-C语言本质与编译模型.md` |
| Layer 1 | 内存模型 | `02-内存模型.md` |
| Layer 1 §1.2.9 | C 安全编程 | `04-C安全编程.md` |
| Layer 2 | 类型系统与表达式 | `03-类型系统与表达式.md` |

---

## 3. 学习内容（章节清单）

### 3.1 C 语言本质与编译模型（对应 Layer 0）

- C 在技术栈中的位置（OS / 数据库 / 中间件 / 运行时都是 C）
- C 的 5 大设计哲学（程序员至上 / 显式胜于隐式 / 零开销抽象 / 保持小 / 稳定性优先）
- C 标准演进（K&R 1978 → C89/C90 → C99 → C11 → C17 → C23）
- 编译模型四阶段（预处理 → 编译 → 汇编 → 链接）
- C 的"无"哲学（无 GC / 无异常 / 无构造析构 / 无运算符重载 / 无完整泛型 / 无字符串类型 / 无标准容器 / 无模块系统 / 无命名空间）

### 3.2 内存模型（对应 Layer 1）

- 进程内存布局（text / data / bss / heap / stack）
- 指针本质（地址 + 类型 + 步长）+ 数组退化规则
- C 字符串的设计缺陷（5 大缺陷 + SDS 解决方案）
- malloc / free 实现原理（ptmalloc 的 arena / bin / fastbin / small bin / large bin / mmap 阈值）
- 内存对齐规则（为什么需要 + 对齐规则 + packed 代价）
- 严格别名规则（例外是 char/signed char/unsigned char，不含 void *）
- volatile 的真实含义（不是线程同步原语）

### 3.3 类型系统与表达式（对应 Layer 2）

- 整型提升（char/short 自动提升为 int）
- 有符号 vs 无符号（size_t 为什么无符号 + 混用陷阱）
- 位运算实战（flags / mask / bitfield + 网络协议解析的位域坑）
- 函数指针（qsort / 表驱动编程 / vtable 雏形）

### 3.4 C 安全编程（对应 Layer 1 §1.2.9）

- 5 大漏洞类型（缓冲区溢出 / 格式化字符串 / 整数溢出 / UAF / Double-Free）
- 编译期加固选项（`-D_FORTIFY_SOURCE=2` / `-fstack-protector-strong` / `-fPIE -pie` / `-Wl,-z,relro,-z,now`）
- 经典 CVE 复现（Morris 蠕虫 / 心脏出血 / OpenSSH 2002）

---

## 4. 实战项目

1. **用 `gcc -E/-S/-c` 分析编译四阶段**：生成 `.i/.s/.o`，对比每个阶段产物 + 用 `readelf -h` 查看 ELF 头 + 用 `strace` 看 `Hello, world` 的实际 syscall
2. **手写简易内存分配器**：实现 `my_malloc` / `my_free`，基于 `mmap` 或 `sbrk`，支持空闲链表 + 合并
   - **生产级 checklist**：多线程（多 arena 或细粒度锁）/ size class 分桶 / 对齐保证 / 元数据保护 / 统计接口 — 对照 ptmalloc/jemalloc 找差距
3. **实现 SDS**：参考 Redis `sds.c`，实现 `sdsnew` / `sdscat` / `sdscpy` / `sdsfree`
4. **用 `valgrind` 调试内存问题**：写一个有内存泄漏 / 野指针 / double-free 的程序，用 valgrind 定位
5. **测试对齐与性能**：写 packed vs 非 packed struct 的访问性能对比
6. **复现一个 CVE**：选一个 C CVE（如心脏出血 CVE-2014-0160），用 ASan 复现并修复

---

## 5. 参考书章节

| 主题 | 极致C | Fluent C | Practical | Mastering | 高效调试 | Memory Thinking |
|---|---|---|---|---|---|---|
| C 本质与编译模型 | Ch1, 2, 12 | — | Ch1 | Ch1 | — | — |
| 内存模型 | Ch4, 5 | — | — | Ch6 | Ch2, 3, 4 | 全书 |
| 类型系统 | Ch1 | Ch3 | — | — | — | 部分 |
| C 安全编程 | — | — | — | — | Ch3（内存损坏）| — |

**重点参考**：
- 极致C Ch1-5, 12（基础特性 + 进程内存 + 栈堆 + 最新 C 语言）
- Memory Thinking 全书（指针哲学 — 内存模型的最佳辅助读物）
- 高效调试 Ch2-3（内存管理器 + 内存损坏 — 实战视角看内存）

---

## 6. 完成标准

能回答以下 10 个问题即算完成：

1. C 在计算机系统中的位置是什么？为什么技术专家必须学 C？
2. C 的 5 大设计哲学是什么？这些哲学如何塑造了 C 的工程模式？
3. C89/C99/C11/C17/C23 各引入了什么关键特性？C23 的生产现状是什么？
4. 编译模型四阶段是什么？预处理阶段会不会做语法检查？
5. 进程内存布局是什么？栈大小默认 8MB（Ubuntu）/ 10MB（CentOS）/ 1MB（容器），超出触发什么？容器环境为什么需关注栈大小？
6. 指针的本质是什么？`void *` 算术是否合法？
7. malloc/free 的实现原理是什么？fastbin/small bin/large bin 的区别？
8. 内存对齐规则是什么？为什么需要对齐？
9. 严格别名规则是什么？例外类型有哪些？`void *` 是例外吗？
10. volatile 的真实含义是什么？为什么不能用作线程同步原语？

---

## 7. 下一步

完成本 stage 后，进入 **`stage2-高级特性/`**（对应 Layer 3：编译/链接/ELF/ABI/可移植性）。

stage2 将深入理解编译/链接模型，能分析 ELF 文件、用 `readelf`/`nm`/`objdump` 分析二进制、理解动态链接的 GOT/PLT 机制、写出可移植的跨平台 C 代码。
