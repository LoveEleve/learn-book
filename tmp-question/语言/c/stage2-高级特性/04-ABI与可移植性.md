# ABI 与可移植性

> **对应 Layer**：Layer 3（编译/链接/目标文件 + 可移植性），参考 C-C++技术专家学习路线.md § Layer 3  
> **参考书章节**：极致C 第2-3章；高效C/C++调试 第6章；需补充外部资料（《C Interfaces and Implementations》David Hanson）  
> **前置依赖**：S2Ch02（符号与链接）、S2Ch03（ELF与静态动态链接）  
> **主线标准**：C11/C17  
> **章节定位**：本章是 stage2 最后一章，也是 C 学习中"从学会写到能跨平台"的分水岭——前面的章节教你写好 C，这一章教你把 C 写出生产可移植性

---

## 1. 引言：一份代码，三个平台，四套编译器

S2Ch02 讲了符号和链接，S2Ch03 讲了 ELF 和动态链接。这些知识全部围绕 Linux/x86/GCC 展开。但生产中的 C 代码很少只在一种平台上运行——一个成熟的 C 库至少要在 Linux（GCC/Clang）、macOS（Clang）、Windows（MSVC）三种平台上能编译运行。

跨平台的障碍不是语法——C 语法在所有平台上都一样。障碍在 ABI（Application Binary Interface，应用程序二进制接口）层面：`long` 在 Linux 64 位是 8 字节，在 Windows 64 位是 4 字节；struct 的成员对齐在不同编译器间不同；网络数据在 x86 上是小端序，在 ARM 上可切换；GCC 支持的 `__attribute__((cleanup))` 在 MSVC 上完全不存在。

本章不教你"在哪个命令行参数下哪个平台能用"——那些是手册的内容，半年后会过时。本章教你**为什么会有这些差异、差异的根本来源是什么、以及如何用"检测而非假设"的原则写出在任何平台上都能编译运行的 C 代码**。

---

## 2. ABI vs API：源码兼容 vs 二进制兼容

### 2.1 核心区别

| 维度 | API（Application Programming Interface） | ABI（Application Binary Interface） |
|---|---|---|
| **层面** | 源代码层面 | 二进制层面 |
| **关注** | 函数签名、类型定义、宏、头文件 | 调用约定、类型大小/对齐、符号命名、系统调用号 |
| **如果改变** | 旧代码重新编译可能报错或警告 | 旧 `.o` / `.so` 必须重新编译——二进制不兼容 |
| **检查方式** | 编译器（语法/语义错误） | 链接器或运行时（segfault / 数据损坏） |
| **跨平台一致性** | C 标准保证（语法层面） | **无**——每个平台定义自己的 ABI |

**关键区别**：API 兼容 ≠ ABI 兼容。

**一个 struct 加字段的例子**：

```c
// v1.h — 最初版本
typedef struct {
    int id;
    char name[32];
} user_t;

// v2.h — 只加字段（API 兼容、ABI 不兼容！）
typedef struct {
    int id;
    int age;            // 新字段——加在 name 之前
    char name[32];
} user_t;
```

- **API 层面**：v1 的代码 `printf("%s", u.name)` 在 v2 下仍然编译通过——API 兼容 ✓
- **ABI 层面**：v1 编译的 `.so` 中 `user_t` 是 36 字节（4 + 32），v2 编译的 client 期望 40 字节（4 + 4 + 32）。新旧混用时 `u.name` 的偏移量不同 → 读到的是 `age` 的字节 → 数据损坏或 segfault — ABI 不兼容 ✗

**这就是为什么 C++ 库的对外接口通常用 C ABI**——C ABI 极其稳定（C89 至今 struct 布局规则不变），而 C++ ABI 不稳定（名称修饰规则随编译器版本变化、虚表布局随类层次变化）。

### 2.2 ABI 包含什么

完整的 ABI 定义包括 7 个维度：

| 数量 | 维度 | 例子：x86_64 Linux (System V AMD64 ABI) |
|---|---|---|
| 1 | 基本类型大小 | `int` = 4, `long` = 8, `pointer` = 8 |
| 2 | 基本类型对齐 | `int` 对齐到 4, `long` 对齐到 8 |
| 3 | struct 布局 | 成员按声明顺序排列，填充满足对齐 |
| 4 | 调用约定 | 前 6 个整数参数通过寄存器 %rdi/%rsi/%rdx/%rcx/%r8/%r9 传递，其余的入栈 |
| 5 | 符号命名 | C 函数名就是符号名（不加修饰），C++ 加类型/命名空间修饰 |
| 6 | 栈帧结构 | 返回地址在栈顶，帧指针 `%rbp` 可选 |
| 7 | 系统调用 | `syscall` 指令，rax=系统调用号，rdi/rsi/rdx/r10/r8/r9=参数 |

**跨书视角**：极致C 第3章从"目标文件内部"讲 ABI（聚焦于 `.o` 文件如何描述重定位），而高效C/C++调试 第6章从"调试二进制需要知道什么"讲 ABI（聚焦于 GDB 如何利用 DWARF 调试信息将机器地址映射回源码行）。两本书互补：极致C 告诉你"存在差异"，高效调试 告诉你"差异在调试时如何暴露"。

---

## 3. 字长模型：ILP32 / LLP64 / LP64

### 3.1 为什么需要三种模型

当计算机从 32 位过渡到 64 位时，每个 OS 必须决定：哪些基本类型需要变宽？选择不同导致了 ILP32 / LLP64 / LP64 三个模型的分裂。

| 模型 | short | int | long | long long | pointer | 代表平台 |
|---|---|---|---|---|---|---|
| **ILP32** | 16 | 32 | 32 | 64 | 32 | 32 位 Linux/Unix/Windows |
| **LLP64** | 16 | 32 | **32** | 64 | 64 | Windows x64 |
| **LP64** | 16 | 32 | **64** | 64 | 64 | 64 位 Linux/macOS/BSD |

### 3.2 各平台为什么会选不同的模型

**Windows x64 选了 LLP64**（`long` = 32 位）：向后兼容是首要目标。如果 `long` 从 32 变 64，所有使用 `LONG`/`DWORD`（Windows API 中大量使用 `long`）的旧代码都会因类型大小变化而 ABI 不兼容。所以 Windows 保持了 `long` = 32 位，唯一变宽的是 pointer（32 → 64 位）。

**Unix/Linux 选了 LP64**（`long` = 64 位）：认为 64 位平台应该能直接处理 64 位值。`long` 在 32 位 Unix 上是 32 位（ILP32），在 64 位上变成 64 位（LP64）——这意味着 32 位 Unix 代码直接移植到 64 位时，`long` 变量的大小会变。但在 Unix 生态中这种重构是可接受的代价。

### 3.3 字长模型对 C 代码的具体影响

**陷阱 1：`long` 的大小不确定**

```c
// Linux x86_64: sizeof(long) = 8
// Windows x64:  sizeof(long) = 4
// 这行代码在 Linux 上安全，在 Windows 上溢出：
long file_size = 5L * 1024 * 1024 * 1024;  // 5GB — Windows 上溢出！
```

**陷阱 2：`int` 始终是 32 位——但只是"通常"**

```c
// C 标准：int 至少 16 位。实际所有现代平台都是 32 位
// 但某些 DSP/嵌入式平台 int = 16 位
int counter = 0xFFFFFFFF;  // DSP 上溢出！
```

**陷阱 3：`size_t` 的大小随 pointer 大小变化**

```c
// 32 位平台：sizeof(size_t) = 4
// 64 位平台：sizeof(size_t) = 8
// 永远用 %zu 打印——32 位平台上 %lu 是错误的
printf("size: %zu\n", sizeof(arr));
```

### 3.4 解决方案：精确宽度类型

`<stdint.h>` 提供三种语义层次的类型：

| 类别 | 类型示例 | 语义 | 使用场景 |
|---|---|---|---|
| **精确宽度** | `int32_t`, `uint64_t` | 保证恰好 N 位 — 如果平台不支持则编译错误（C99 起可选） | 网络协议、文件格式、跨平台数据交换 |
| **最小宽度** | `int_least32_t` | 至少 N 位 — 所有平台必须提供 | 需要至少 N 位的计算 |
| **最快宽度** | `int_fast32_t` | 至少 N 位，该平台上操作最快的宽度 | 局部变量、循环计数器 |

**格式宏**（`<inttypes.h>`）：

```c
#include <inttypes.h>
int64_t val = 0x7FFFFFFFFFFFFFFF;
printf("val = %" PRId64 "\n", val);   // 在 Linux 上展开为 %ld，在 Windows 上展开为 %lld

uint64_t uval = 0xFFFFFFFFFFFFFFFF;
printf("uval = %" PRIu64 " (0x%" PRIX64 ")\n", uval, uval);
```

**实验——验证当前平台的字长**：

```bash
cat > wordsize.c << 'EOF'
#include <stdio.h>
#include <stdint.h>
int main() {
    printf("Platform model: ");
    if (sizeof(long) == 8)      printf("LP64 (Linux/macOS 64-bit)\n");
    else if (sizeof(long) == 4) printf("LLP64 (Windows x64) or ILP32\n");
    printf("sizeof(short)     = %zu\n", sizeof(short));
    printf("sizeof(int)       = %zu\n", sizeof(int));
    printf("sizeof(long)      = %zu\n", sizeof(long));
    printf("sizeof(long long) = %zu\n", sizeof(long long));
    printf("sizeof(void*)     = %zu\n", sizeof(void*));
    printf("sizeof(size_t)    = %zu\n", sizeof(size_t));
    printf("INT32_MAX = %" PRId32 "\n", INT32_MAX);
    return 0;
}
EOF
gcc -Wall wordsize.c -o wordsize && ./wordsize
```

---

## 4. 字节序：大端与小端

### 4.1 Big-Endian vs Little-Endian

**Little-Endian（小端）**：低位字节在低地址。

```
int x = 0x01020304;
内存中：04 03 02 01  （从低地址到高地址）
```

**Big-Endian（大端）**：低位字节在高地址。

```
int x = 0x01020304;
内存中：01 02 03 04  （从低地址到高地址）
```

**来源**："Endian"一词来自 Jonathan Swift 的《格列佛游记》——两个国家为"鸡蛋应该从大头敲还是小头敲"而打仗。Danny Cohen 1980 年在 IEN-137 中将其借用到计算机领域。

### 4.2 各平台的字节序

| 平台 | 默认字节序 | 可切换？ |
|---|---|---|
| x86 / x86_64 | 小端 | ❌ 不可切换 |
| ARM (AArch32/AArch64) | 小端（默认） | ✅ 可切换（bi-endian） |
| SPARC | 大端（默认） | ✅ 可切换 |
| MIPS | 大端（默认） | ✅ 可切换（mipsel = 小端 / mips = 大端） |

### 4.3 字节序对 C 代码的影响

**陷阱 1：指针类型转换**

```c
int x = 0x01020304;
char first_byte = *(char *)&x;
// 小端平台：first_byte = 0x04（最低位在低地址）
// 大端平台：first_byte = 0x01（最高位在低地址）
```

**陷阱 2：网络与主机的不同**

网络协议规定必须用大端序（网络字节序）。但大多数现代机器是小端序（x86/ARM 默认）。因此任何发送到网络的数据必须转换：

```c
#include <arpa/inet.h>
uint32_t val = 0x01020304;
uint32_t net_order = htonl(val);   // Host TO Network Long — 转换为大端
uint32_t back      = ntohl(net_order); // Network TO Host Long — 转回主机序
```

**陷阱 3：跨机器发送二进制数据**

```c
// 危险：直接发送 struct（字节序 + 对齐 + padding 全部不统一）
send(fd, &my_struct, sizeof(my_struct), 0);

// 安全：逐字段序列化为网络字节序
uint32_t id_net = htonl(my_struct.id);
send(fd, &id_net, sizeof(id_net), 0);
send(fd, my_struct.name, strlen(my_struct.name) + 1, 0);
```

### 4.4 检测字节序的可靠方法

```bash
cat > endian.c << 'EOF'
#include <stdio.h>
#include <stdint.h>
int main() {
    uint16_t x = 0x0001;      // 高位=0x00，低位=0x01
    if (*(uint8_t *)&x == 0x01) {
        printf("Little-Endian (低地址=低字节)\n");
    } else {
        printf("Big-Endian (低地址=高字节)\n");
    }
    return 0;
}
EOF
gcc -Wall endian.c -o endian && ./endian
```

---

## 5. struct 布局的跨平台差异

### 5.1 对齐规则的平台依赖

Ch2 §7 介绍了内存对齐——每个成员对齐到自身大小的整数倍地址，整个 struct 大小是最大成员大小的整数倍。但**最大成员是谁**取决于平台：

```c
struct data {
    int  a;     // 4 字节
    long b;     // Linux: 8 字节, Windows: 4 字节
    char c;     // 1 字节
};

// Linux x86_64 (LP64)：sizeof = 24（a=4 + padding=4 + b=8 + c=1 + padding=7）
// Windows x64 (LLP64)： sizeof = 12（a=4 + b=4 + c=1 + padding=3）
```

**同一个 struct，在两个平台上有完全不同的 sizeof**——这就是为什么不能直接 memcpy struct 到网络、不能假设 offsetof 的值在不同平台间相同。

### 5.2 序列化的正确方式

**错误**：

```c
// 直接 memcpy struct —— 字节序、对齐、padding 都不统一
memcpy(net_buffer, &my_data, sizeof(my_data));
```

**正确**：逐字段转换为网络字节序：

```c
void serialize_data(uint8_t *buf, const struct data *d) {
    uint32_t a_net = htonl(d->a);   // int → 网络序
    memcpy(buf, &a_net, 4);
    // long 跨平台：用 int64_t 替代，保证统一宽度
    int64_t b_net = htonll(d->b);   // 注意：htonll 不是 POSIX，某些平台需自写
    memcpy(buf + 4, &b_net, 8);
    buf[12] = d->c;                  // char 不需要转换
}
```

这里的关键原则：**序列化后的字节流**是跨平台的，**struct 的字节布局**不是。

### 5.3 `__attribute__((packed))` 的生产建议

```c
struct __attribute__((packed)) {
    char a;
    int  b;     // 紧跟 a 的后面——不对齐
};              // sizeof = 5
```

**代价**：
- x86：未对齐的 int 访问需要两次内存读取 + 字节拼接 → 2-3 倍慢
- ARM：未对齐访问会触发内核 trap → 10-50 倍慢（取决于 trap handler 效率）
- SPARC：SIGBUS — 直接崩溃

**生产建议**：packed struct 用于内存打包（如仿真硬件寄存器映射），**不用**于跨平台数据传输。跨平台数据用显式的字节流序列化。

### 5.4 其他跨平台结构问题

**bit-field 布��**：C 标准规定 bit-field 的布局（从高位向低位还是反向分配）是实现定义的。不同编译器的位域布局不可互换：

```c
struct flags {
    unsigned int a : 1;
    unsigned int b : 3;
};
// GCC 和 Clang 可能从低位向高位分配，MSVC 可能不同
// 跨越不同编译器发送 bit-field struct = 数据损坏
```

**`#pragma once` vs header guard 可移植性**：`#pragma once` 在 GCC/Clang/MSVC 上都支持，但 IAR/Keil 等嵌入式编译器不支持。跨平台开源项目应同时提供 header guard 作为回退：

```c
#pragma once
#ifndef MYHEADER_H
#define MYHEADER_H
// ... content ...
#endif
```

---

## 6. 编译器差异：GCC / Clang / MSVC

### 6.1 三大编译器的 C 标准支持

| 编译器 | 默认 C 标准 | C11 完整支持 | C17 完整支持 | C23 支持 |
|---|---|---|---|---|
| **GCC** | `-std=gnu17`（GCC 8+） | GCC 4.9+ | GCC 8+ | GCC 14+ |
| **Clang** | `-std=gnu17` | Clang 3.3+ | Clang 6+ | Clang 18+ |
| **MSVC** | C89（带少量 C99 扩展） | **不支持 C11 完整特性** | **不支持** | — |

**MSVC 的 C 支持是跨平台 C 编程的最大障碍**：MSVC 从 C89 之后几乎没有更新 C 标准支持——不支持 VLA、不支持 `<stdatomic.h>`、不支持 `_Generic`、不支持 `_Static_assert`（但支持同名 MSVC 扩展）。微软的立场是"C++ 是未来，C 只是为了兼容旧代码"。

### 6.2 条件编译处理编译器差异

```c
#if defined(__GNUC__)
    // GCC 或 Clang
    #define NORETURN __attribute__((noreturn))
    #define PACKED   __attribute__((packed))
    #define ALWAYS_INLINE __attribute__((always_inline))

#elif defined(_MSC_VER)
    // MSVC
    #define NORETURN __declspec(noreturn)
    #define PACKED
    #define PACKED_BEGIN __pragma(pack(push, 1))
    #define PACKED_END   __pragma(pack(pop))
    #define ALWAYS_INLINE __forceinline

#else
    #error "Unsupported compiler"
#endif
```

### 6.3 各编译器的独特扩展

| 编译器 | 独有特性 | 标准化替代 |
|---|---|---|
| GCC | `__attribute__((cleanup))` — 自动清理变量 | 无 C 标准替代（C++ 有 RAII） |
| Clang | `_Nonnull` / `_Nullable` — 空指针注解 | 可以用 `assert(p != NULL)` 或文档约定 |
| MSVC | `__try` / `__except` — 结构化异常处理 | `setjmp` / `longjmp`（功能弱很多） |

**生产建议**：能不依赖编译器扩展就尽量不用。如果必须用，用条件编译提供备选实现。

---

## 7. 标准版本检测与特性适配

### 7.1 标准版本宏

```c
#include <stdio.h>
int main() {
#if __STDC_VERSION__ >= 201710L
    printf("C17 or later\n");
#elif __STDC_VERSION__ >= 201112L
    printf("C11\n");
#elif __STDC_VERSION__ >= 199901L
    printf("C99\n");
#else
    printf("C89/C90 or earlier\n");
#endif
    printf("__STDC_HOSTED__ = %d (0=freestanding, 1=hosted)\n", __STDC_HOSTED__);
    return 0;
}
```

- **`__STDC_HOSTED__`** = 1 表示有完整的标准库（桌面/服务器环境），= 0 表示裸露硬件（freestanding，如嵌入式 RTOS）
- **`__STDC_VERSION__`** = 仅在有标准库时定义（hosted environment），freestanding 下可能未定义

### 7.2 特性检测：不要假设，要检测

**错误**——假设编译器支持 C11：

```c
#include <stdatomic.h>  // 如果 MSVC 编译，这行找不到头文件
_Atomic int counter;
```

**正确**——检测后条件使用：

```c
#if __STDC_VERSION__ >= 201112L && !defined(__STDC_NO_ATOMICS__)
    #include <stdatomic.h>
    atomic_int counter;
#else
    #include <pthread.h>
    pthread_mutex_t lock;
    int counter;
#endif
```

---

## 8. 跨平台构建：CMake 的跨平台能力

### 8.1 平台检测

CMake 通过变量和生成器表达式实现平台检测：

```cmake
cmake_minimum_required(VERSION 3.10)
project(myapp C)

# 平台检测
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    add_definitions(-DPLATFORM_LINUX)
elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    add_definitions(-DPLATFORM_MACOS)
elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
    add_definitions(-DPLATFORM_WINDOWS)
endif()

# 编译器检测
if(CMAKE_C_COMPILER_ID STREQUAL "GNU")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra -Wpedantic")
elseif(CMAKE_C_COMPILER_ID STREQUAL "Clang")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra -Wpedantic -Weverything")
elseif(CMAKE_C_COMPILER_ID STREQUAL "MSVC")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /W4")
endif()

# 查找第三方库
find_package(CURL REQUIRED)

add_executable(myapp main.c)
target_link_libraries(myapp PRIVATE CURL::libcurl)
```

### 8.2 `try_compile`：检测编译器是否支持某个特性

```cmake
# 检测当前编译器是否支持 -fstack-protector-strong
check_c_compiler_flag("-fstack-protector-strong" HAS_STACK_PROTECTOR_STRONG)
if(HAS_STACK_PROTECTOR_STRONG)
    target_compile_options(myapp PRIVATE -fstack-protector-strong)
endif()
```

### 8.3 包管理

- **Conan**：C/C++ 包管理器（两个主要选项之一，与 vcpkg 并列；大部分项目仍用系统包管理器或源码包含）
- **vcpkg**：Microsoft 出品，CMake 原生集成
- **手工管理**：Linux 用系统包管理器（`apt install libcurl-dev`），macOS 用 Homebrew，Windows 需要预编译或编译三方库

---

## 9. 可移植性实战案例：跨平台文件操作库

将本章所有知识合并为一个完整的跨平台文件操作库。这个库需要：
- 字长无关的文件大小表示
- 字节序无关的元数据结构
- 平台无关的文件 API
- 条件编译处理平台差异

```c
// portable_file.h — 跨平台接口
#ifndef PORTABLE_FILE_H
#define PORTABLE_FILE_H

#include <stdint.h>
#include <stddef.h>

// 字长无关的文件大小类型
typedef uint64_t file_size_t;

// 字节序无关的文件元数据（跨编译器 packed — GCC 用 __attribute__，MSVC 用 pragma）
#if defined(__GNUC__) || defined(__clang__)
typedef struct __attribute__((packed)) {
#elif defined(_MSC_VER)
#pragma pack(push, 1)
typedef struct {
#endif
    uint32_t magic;       // 魔数，标识文件格式（网络字节序 = 大端）
    uint16_t version;     // 版本号（网络字节序）
    uint64_t data_size;   // 数据大小（网络字节序）
} file_header_t;
#if defined(_MSC_VER)
#pragma pack(pop)
#endif

// 平台无关的函数
file_size_t portable_fsize(const char *path);   // 获取文件大小（跨平台）
int         portable_fwrite_header(FILE *f, const file_header_t *hdr);
int         portable_fread_header(FILE *f, file_header_t *hdr);

#endif
```

```c
// portable_file.c — 跨平台实现
#include "portable_file.h"
#include <stdio.h>
#include <stdint.h>

#if defined(__linux__) || defined(__APPLE__)
    #include <sys/stat.h>

    file_size_t portable_fsize(const char *path) {
        struct stat st;
        if (stat(path, &st) != 0) return 0;
        return (file_size_t)st.st_size;
    }

#elif defined(_WIN32)
    #include <windows.h>

    file_size_t portable_fsize(const char *path) {
        WIN32_FILE_ATTRIBUTE_DATA attr;
        if (!GetFileAttributesExA(path, GetFileExInfoStandard, &attr)) return 0;
        return ((file_size_t)attr.nFileSizeHigh << 32) | attr.nFileSizeLow;
    }
#else
    #error "Unsupported platform"
#endif

// 字节序无关的序列化（使用 htonl/ntohl — 在 Windows 上需要 winsock2.h + ws2_32.lib）
#ifdef _WIN32
    #include <winsock2.h>
    // CMake: target_link_libraries(myapp ws2_32)
#else
    #include <arpa/inet.h>
#endif

static inline uint64_t htonll(uint64_t host) {
    // 小端机：将高低半部各用 htonl 交换后重新组合
    // 注意：大端机 htonl 为 no-op，此公式在大端机上错误 — 生产代码需检测字节序
    return ((uint64_t)htonl(host >> 32) << 32) | (uint64_t)htonl((uint32_t)host);
}
#define ntohll(x) htonll(x)

int portable_fwrite_header(FILE *f, const file_header_t *hdr) {
    file_header_t net_hdr;
    net_hdr.magic     = htonl(hdr->magic);
    net_hdr.version   = htons(hdr->version);
    net_hdr.data_size = htonll(hdr->data_size);
    return fwrite(&net_hdr, sizeof(net_hdr), 1, f) == 1 ? 0 : -1;
}

int portable_fread_header(FILE *f, file_header_t *hdr) {
    if (fread(hdr, sizeof(*hdr), 1, f) != 1) return -1;
    hdr->magic     = ntohl(hdr->magic);
    hdr->version   = ntohs(hdr->version);
    hdr->data_size = ntohll(hdr->data_size);
    return 0;
}
```

这个例子展示了本章全部 6 个可移植性原则：

1. **字长**：`file_size_t` = `uint64_t`（不依赖平台 unsigned long）
2. **字节序**：`htonl`/`ntohl` 统一转换为大端序
3. **API 差异**：`#ifdef` 区分 `stat()`（Unix）和 `GetFileAttributesEx()`（Windows）
4. **头文件差异**：`arpa/inet.h`（Unix）vs `winsock2.h`（Windows）
5. **struct 序列化**：packed struct 保证字节布局统一 + 逐字段字节序转换
6. **编译器检测**：`#if defined(__linux__) || defined(__APPLE__)` 区分平台家族

---

## 10. 总结

| 概念 | 核心内容 |
|---|---|
| ABI vs API | API=源码兼容、ABI=二进制兼容 — struct 加字段 API 兼容但 ABI 不兼容 |
| 字长模型 | ILP32=32位/LLP64(long=32bit, pointer=64)=Windows/LP64(long=64bit, pointer=64)=Unix |
| `long` 陷阱 | Linux 64位=8B, Windows 64位=4B — 协议/文件格式用 `int64_t` |
| 字节序 | 小端（x86/ARM默认）=低位低地址，大端（网络序）=高位低地址 |
| `htonl`/`ntohl` | Host TO Network Long — 发送前转换、接收后转回，永远如此 |
| struct 跨平台 | `sizeof` 依赖平台字长和编译器对齐规则 — 序列化用显式字节流 |
| `<stdint.h>` | `intN_t`=精确宽度, `int_leastN_t`=最小宽度, `int_fastN_t`=最快宽度 |
| `<inttypes.h>` | `PRIu64`/`PRId64` 等格式宏 — 跨平台打印固定宽度类型 |
| 条件编译 | `#if __STDC_VERSION__ >= 201112L` — 检测而非假设 |
| 跨平台构建 | CMake + Conan/vcpkg + try_compile — 检测编译器支持的特性和三方库 |
| MSVC 的 C 支持 | 停留在 C89，不支持 `_Generic`/`<stdatomic.h>`/VLA — Windows C 开发首选 MinGW 或 Clang |
| 可移植性五原则 | 1. 用精确宽度类型 2. 显式字节序转换 3. 条件编译 4. struct 序列化手动 5. CMake 检测 |

---

## 验收要点

1. ABI 和 API 有什么区别？给一个 struct 加字段为什么 API 兼容但 ABI 不兼容？
2. ILP32 / LLP64 / LP64 的 `long` 各占多少字节？为什么 Windows 选 LLP64、Unix 选 LP64？
3. `int64_t` 和 `long long` 有什么区别？什么时候用哪个？
4. 小端和大端的区别是什么？为什么网络协议必须用大端序？
5. `htonl` 和 `ntohl` 的作用是什么？为什么发送二进制数据必须转换？
6. 两个平台上的同一个 struct 为什么 sizeof 不同？给出一个具体的例子
7. 如何在编译时检测当前的 C 标准版本？C11 和 C99 的 `__STDC_VERSION__` 值各是多少？
8. MSVC 为什么不支持 `_Generic` 和 `<stdatomic.h>`？Windows 上开发 C 项目应该用什么替代方案？
9. CMake 如何检测当前操作系统？如何检测编译器是否支持某个编译选项？
10. 用 `PRIu64` 格式宏打印 `uint64_t` 变量，在 Linux 和 Windows 上会展开成什么？为什么不能直接用 `%lu`？

---

## Stage 2 收尾：你知道了什么

Stage 1 教你怎么写好 C — 内存模型、类型系统、安全编程。Stage 2 教你**代码如何变成二进制、如何跨平台**：

```
01-预处理与编译单元  →  源码如何展开、分割为独立 TU → `static`/`inline` 的可见性规则
     ↓
02-符号与链接        →  `.o` 之间如何通过符号表协作 → 强弱符号、库查找顺序
     ↓
03-ELF与静态动态链接  →  最终产物的内部结构 → GOT/PLT/PIC/懒惰绑定
     ↓
04-ABI与可移植性     →  同一份代码为什么在不同平台上 sizeof 不同 → 字长/字节序/编译器差异
```

**与 Stage 1 的关系**：Stage 1 的 Ch1 §4 讲"编译四阶段是什么"，Stage 2 讲"每个阶段的技术细节是什么、以及如何用这些细节写出跨平台代码"。

**与 Stage 3 的关系**：Stage 3 进入**并发编程** — 你将在 pthread/原子操作/内存序中用到 Stage 2 的可移植性知识（`stdatomic.h` 在 MSVC 不支持 → 需要条件编译），用到 Stage 1 的内存模型知识（栈变量的地址在多个线程间不能共享）。
