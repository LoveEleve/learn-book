# Stage 2：高级特性

> **本 stage 定位**：C 语言学习路线的第二阶段，深入编译/链接模型。对应 `C-C++技术专家学习路线.md` 的 **Layer 3**（编译/链接/目标文件 + 可移植性）。
>
> **预计时间**：1-2 周（每日 2-3 小时投入）
>
> **完成本 stage 后**：能分析 ELF 文件、用 `readelf`/`nm`/`objdump` 分析二进制、理解动态链接的 GOT/PLT 机制、理解 ABI vs API 的区别、写出可移植的跨平台 C 代码。

---

## 1. 学习目标

完成本 stage 后，应能达成以下 5 个目标：

1. **能讲清楚预处理 / 编译单元 / 符号 / 链接的完整流程**，回答"为什么头文件中不能放定义"等问题
2. **能用 `readelf` / `nm` / `objdump` 分析任意 C 二进制**（查看 ELF 节段 / 符号表 / 反汇编）
3. **能讲清楚静态链接 vs 动态链接的 trade-off**（体积 / 启动速度 / 升级 / 安全 / ABI 兼容）
4. **能讲清楚 PIC / GOT / PLT 的机制**（为什么共享库必须 `-fPIC`）
5. **能写出跨平台 C 代码**（处理 ILP32/LLP64/LP64 字长差异 + 字节序 + 编译器差异）

---

## 2. 对应 Layer

| Layer | 主题 | 本 stage 章节 |
|---|---|---|
| Layer 3 | 编译/链接/目标文件 + 可移植性 | `01-预处理与编译单元.md` / `02-符号与链接.md` / `03-ELF与静态动态链接.md` / `04-ABI与可移植性.md` |

---

## 3. 学习内容（章节清单）

### 3.1 预处理与编译单元

- 预处理器（只做文本替换，不做语法检查）
- 宏的陷阱（`MAX(i++, j++)` 副作用）+ 头文件保护（`#ifndef` vs `#pragma once`）
- include 顺序（自身 → 标准库 → 第三方 → 项目内）
- 编译单元（一个 `.c` + 所有 `#include` 展开后的内容）
- `static` / `inline` 在编译单元中的可见性

### 3.2 符号与链接

- Strong vs Weak Symbol（已初始化全局 = strong；未初始化 = weak）
- 链接规则（多个 strong 同名 = 错误；一个 strong + 多个 weak = 选 strong；多个 weak = 选第一个）
- 符号解析过程（收集 → 查找 → 重定位）

### 3.3 ELF 与静态/动态链接

- ELF 文件格式（ELF Header / Program Headers / Section Headers：.text/.rodata/.data/.bss/.symtab/.strtab/.plt/.got）
- 工具：`readelf -a` / `nm` / `objdump -d` / `size`
- 静态链接 vs 动态链接（5 维度对比：时机 / 体积 / 启动速度 / 升级 / 安全）
- 位置无关代码（PIC）+ GOT（Global Offset Table）+ PLT（Procedure Linkage Table）
- `-fPIC` vs `-fpic` 的区别

### 3.4 ABI 与可移植性

- ABI vs API（源代码层面 vs 二进制层面）
- ABI 兼容性陷阱（struct 加字段 = API 不变但 ABI 变了）
- 字长模型（ILP32 / LLP64 / LP64 — Linux 64 位 LP64，Windows 64 位 LLP64）
- 字节序（Big-Endian vs Little-Endian，检测方法，`htonl`/`ntohl`）
- 编译器差异（GCC / Clang / MSVC）
- 可移植性最佳实践（用 `<stdint.h>` 精确宽度类型 / `size_t` / `PRIu64` / 显式字节流序列化）

---

## 4. 实战项目

1. **分析一个简单 C 程序的 ELF**：用 `readelf` / `nm` / `objdump` 查看 `.text` / `.data` / `.bss` / 符号表
2. **写一个静态库 + 动态库**：用 `ar` 创建 `.a`，用 `-shared -fPIC` 创建 `.so`
3. **用 `LD_PRELOAD` 劫持 malloc**：写一个 `my_malloc.so`，记录每次分配，检测内存泄漏
4. **理解 ABI 破坏**：写两个版本的 `struct foo`（一个有新字段），分别编译 `.so` 和 client，演示 ABI 不兼容
5. **跨平台代码**：写一个能在 Linux + macOS + Windows 编译的 C 程序，处理字长 + 字节序 + 编译器差异

---

## 5. 参考书章节

| 主题 | 极致C | Fluent C | Practical | Mastering | 高效调试 | Memory Thinking |
|---|---|---|---|---|---|---|
| 预处理与编译单元 | Ch2 | — | — | — | — | — |
| 符号与链接 | Ch2, 3 | — | — | — | — | — |
| ELF 与静态/动态链接 | Ch2, 3 | — | — | — | Ch6 | — |
| ABI 与可移植性 | — | — | — | — | — | — |

**重点参考**：
- 极致C Ch2-3（从源文件到二进制 + 目标文件 — 最系统的编译/链接讲解）
- 高效调试 Ch6（二进制文件格式 + 运行期加载和链接 — 调试视角看 ELF）

**注**：可移植性主题 6 本书覆盖不足，需补充外部资料（如《C Interfaces and Implementations》David Hanson）。

---

## 6. 完成标准

能回答以下 8 个问题即算完成：

1. 预处理阶段会不会做语法检查？宏的副作用如何避免？
2. 编译单元是什么？`static` / `inline` 在编译单元中的可见性如何？
3. Strong vs Weak Symbol 的规则是什么？
4. ELF 文件有哪些节段？`.bss` 为什么在文件中无内容？
5. 静态链接 vs 动态链接的 5 维度 trade-off 是什么？
6. PIC 是什么？为什么共享库必须 `-fPIC`？GOT/PLT 的机制是什么？
7. ABI vs API 的区别是什么？struct 加字段会破坏哪个？
8. ILP32/LLP64/LP64 的区别？Linux 64 位和 Windows 64 位的 `long` 各是多少字节？

---

## 7. 下一步

完成本 stage 后，进入 **`stage3-并发编程/`**（对应 Layer 5：pthread / 同步原语 / 原子操作 / 内存序 / 无锁数据结构）。

stage3 将深入 C 最难的部分——并发编程，理解 pthread API、同步原语、C11 原子操作、内存序（x86-TSO vs ARM weak memory model）、无锁数据结构与 ABA 问题。
