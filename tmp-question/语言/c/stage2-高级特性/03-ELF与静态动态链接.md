# ELF 与静态/动态链接

> **对应 Layer**：Layer 3（编译/链接/目标文件），参考 C-C++技术专家学习路线.md § Layer 3  
> **参考书章节**：极致C 第2-3章；高效C/C++调试 第6章  
> **前置依赖**：S2Ch02（符号与链接）  
> **主线标准**：C11/C17

---

## 1. 引言：二进制文件里的秘密

S2Ch02 从符号表角度理解了链接。但链接器生成的产物——可执行文件——本身是一个结构化的二进制文件。在 Linux 上，这个格式叫 ELF（Executable and Linkable Format，可执行与可链接格式）。

理解 ELF，你能回答："为什么我的程序文件有 17KB 但 `size` 只报告 2KB 的代码？""为什么 `ls -l` 看到的文件大小不等于 text+data+bss 的和？""动态链接的程序启动时发生了什么？"

---

## 2. ELF 文件结构

### 2.1 两重视图

ELF 文件有两套组织结构：

```
链接视图（编译视角）              执行视图（运行时视角）
   ELF Header                       ELF Header
   Section Headers                  Program Headers
   .text                            Segment 1 (R-X) ──┐
   .rodata                          Segment 2 (R--)   ├─ 多个节合并为一个段
   .data                            Segment 3 (RW-) ──┘
   .bss
   .symtab
   .strtab
   ...
```

- **链接视图**（Section Headers）：链接器使用 — 细分为 `.text`/`.data`/`.rodata` 等节
- **执行视图**（Program Headers）：OS 加载器使用 — 多个节合并为更少、更大的段

### 2.2 核心节速查

| 节 | 内容 | 权限 | 文件中占空间？ |
|---|---|---|---|
| `.text` | 机器指令 | R-X | 是 |
| `.rodata` | 只读数据（字符串字面量、const 全局） | R-- | 是 |
| `.data` | 已初始化全局/静态变量 | RW- | 是 |
| `.bss` | 未初始化全局/静态变量（OS 加载时清零） | RW- | **否**（仅记录大小） |
| `.symtab` | 符号表 | — | 是（可 strip） |
| `.strtab` | 字符串表（符号名） | — | 是（可 strip） |
| `.plt` | Procedure Linkage Table — 动态链接跳板 | R-X | 是 |
| `.got` / `.got.plt` | Global Offset Table — 全局数据地址表 | RW- | 是 |
| `.init` / `.fini` | 初始化/析构函数 | R-X | 是 |
| `.eh_frame` | 异常处理帧（栈展开信息） | R-- | 是 |

### 2.3 实验：亲眼看 ELF

```bash
cat > elf_test.c << 'EOF'
#include <stdio.h>
const char *msg = "Hello, ELF";
int global_init = 42;
int global_uninit;

int main(void) {
    printf("%s\n", msg);
    return 0;
}
EOF

gcc -O0 elf_test.c -o elf_test

# ELF 头
readelf -h elf_test

# 节头
readelf -S elf_test | grep -E '\.text|\.rodata|\.data|\.bss|\.plt|\.got'

# 段头（Program Headers）
readelf -l elf_test | grep -E 'LOAD|GNU'

# 文件大小 vs text/data/bss
ls -l elf_test
size elf_test
# 比较：文件大小 > text+data（因为还有 ELF 头 + 节头 + .symtab + .strtab 等）
```

---

## 3. 静态链接

### 3.1 原理

静态链接把库代码直接嵌入可执行文件：

```
main.o ─┐
         ├─ 链接器 ─→ a.out（包含 main + printf + 所有用到的 libc 函数）
libc.a ─┘
```

**优点**：独立可移植 — 不依赖系统上的 `.so` 版本
**缺点**：二进制体积大 + 库升级需要重新链接 + 多程序各自包含一份 libc 副本

**静态 vs 动态链接 5 维度对比**：

| 维度 | 静态链接 | 动态链接 |
|---|---|---|
| **时机** | 编译时——库代码合并到可执行文件 | 运行时——ld-linux.so 加载 .so |
| **体积** | 大（每个程序包含完整 libc） | 小（共享 .so，系统只保留一份） |
| **启动速度** | 快（无 .so 加载+重定位） | 慢（需解析 GOT/PLT，懒惰绑定缓解） |
| **升级** | 需重新链接可执行文件 | 替换 .so 即可（ABI 兼容前提下） |
| **安全** | 不受 LD_PRELOAD 劫持 | 依赖 .so 路径安全，可被 LD_PRELOAD 注入 |

### 3.2 实验

```bash
# 静态链接
gcc -static elf_test.c -o elf_static

# 对比体积
ls -l elf_test elf_static
# elf_test:   ~17KB（动态链接）
# elf_static: ~800KB（静态链接，包含整个 libc）
```

### 3.3 何时用静态链接

- 容器镜像（所有依赖打包为一个二进制）
- 嵌入式系统（无动态链接器）
- 安全敏感场景（避免 LD_PRELOAD 劫持）
- 但大多数生产系统用动态链接 — 体积小+可升级

---

## 4. 动态链接

### 4.1 原理

动态链接不嵌入库代码 — 运行时由动态链接器（`ld-linux.so`）加载：

```
main.o ─┐
         ├─ 链接器 ─→ a.out（仅包含 main，printf 是 .plt 条目 + U 符号）
libc.so ─┘

运行时：
  a.out 启动 → ld-linux.so 加载 libc.so → 解析符号 → 跳转到 printf
```

### 4.2 两个关键数据结构

**GOT（Global Offset Table，全局偏移表）** — 存储全局数据/函数的实际地址：

```
.got[0]  = 动态段的地址（由动态链接器使用）
.got[1]  = link_map 指针
.got[2]  = _dl_runtime_resolve 入口
.got[3]  = printf 的实际地址（首次调用前 = .plt 桩代码地址，调用后 = 实际 printf 地址）
.got[4]  = msg 的实际地址
```

**PLT（Procedure Linkage Table，过程链接表）** — 跳转表，实现延迟绑定：

```
.plt[0]  = 跳转到动态链接器的解析函数
.plt[1]  = printf 的入口
```

**懒惰绑定流程**（首次调用 `printf`）：

```
                 call printf@plt
                       │
                       ▼
              ┌─────────────────┐
              │  .plt[printf]:  │
              │  jmp *GOT[n]   │──┐ 后续调用（GOT 已解析）→ 直接跳 printf
              └────────┬────────┘  │
                       │首次       │
                       ▼           ▼
              push reloc_index   printf() 实际代码
              jmp .plt[0]           ▲
                  │                 │
                  ▼                 │
         _dl_runtime_resolve() ─────┘
         (解析 printf 地址，写回 GOT[n])
```

**反汇编验证**（PLT 桩代码的实际指令）：

```bash
objdump -d -j .plt.sec main_dyn 2>/dev/null || objdump -d -j .plt main_dyn
# 输出示例：
# 0000000000001050 <printf@plt>:
#   1050:  jmp    *0x2fca(%rip)    # GOT[printf] — 第一次 = 1056，解析后 = printf 实际地址
#   1056:  push   $0x0              # printf 的重定位索引
#   105b:  jmp    1020 <_init+0x20> # 跳转到 PLT[0]（公共解析入口）
```

### 4.3 重定位表：`.rela.plt` vs `.rela.dyn`

链接器需要知道哪些地址在运行时需要修正。这些信息存储在重定位表中：

```bash
readelf -r main_dyn
```

| 重定位类型 | 含义 | 存储位置 |
|---|---|---|
| `R_X86_64_JUMP_SLOT` | 函数地址（PLT 跳转目标） | `.rela.plt` |
| `R_X86_64_GLOB_DAT` | 全局数据地址 | `.rela.dyn` |
| `R_X86_64_RELATIVE` | 相对重定位（基址 + 偏移） | `.rela.dyn` |

**两类重定位的分工**：
- **`.rela.plt`**：存放函数调用的重定位信息（`printf`、`say_hello` 等）。懒惰绑定时动态链接器逐项处理
- **`.rela.dyn`**：存放数据引用的重定位信息（全局变量地址）。**必须在程序启动前全部解析**，不能延迟

**实验**：

```bash
# 对比两表
readelf -r main_dyn | grep JUMP_SLOT    # .rela.plt 条目
readelf -r main_dyn | grep GLOB_DAT     # .rela.dyn 条目
```

### 4.4 共享库的地址空间布局

动态链接器将每个 .so 的 text 段（共享）和 data 段（每进程独立）映射到进程地址空间：

```
高地址   ┌────────────────────────┐
         │ stack                      │
         ├────────────────────────┤
         │ [vdso]                     │  内核映射的虚拟动态共享对象
         ├────────────────────────┤
         │ libhello.so data (RW-)     │  每进程独立
         │ libhello.so text (R-X)     │  多进程共享（同一物理页）
         ├────────────────────────┤
         │ libc.so data (RW-)         │
         │ libc.so text (R-X)         │
         ├────────────────────────┤
         │ ld-linux.so data           │
         │ ld-linux.so text           │
         ├────────────────────────┤
         │ main 的 data/bss           │
         │ main 的 text               │
低地址   └────────────────────────┘
```

**实验——亲眼看地址空间**：

```bash
# 运行程序并查看其进程映射
./main_rpath &
PID=$!
cat /proc/$PID/maps | grep -E 'main|libhello|libc|ld'
kill $PID
# 观察：libhello.so 的 text 和 data 是否在不同地址？
# text 段有 'x' 权限（可执行），data 段是 'rw' 权限
```

### 4.5 PIE vs non-PIE 实验

PIE（Position Independent Executable）让可执行文件本身也支持 ASLR（地址空间布局随机化）：

```bash
# non-PIE（默认在某些旧系统上）
gcc -no-pie elf_test.c -o elf_nopie

# PIE（现代 Linux 默认）
gcc -pie elf_test.c -o elf_pie

# 查看 ELF 类型
readelf -h elf_nopie | grep Type   # Type: EXEC (Executable file)  — 固定加载地址
readelf -h elf_pie  | grep Type   # Type: DYN (Shared object file) — 位置无关

# 验证 ASLR 效果（每次运行地址不同）
for i in 1 2 3; do
    ./elf_pie &
    cat /proc/$!/maps | grep 'main' | head -1
    kill $! 2>/dev/null
done
# 三次运行的 main text 地址应该不同（ASLR 生效）
```

### 4.6 PIC（Position Independent Code）

共享库被不同进程加载到不同地址 — 不能假设数据/代码在固定位置。**PIC** 通过`@GOTPCREL` 寻址模式解决：

```c
// 非 PIC（假设固定地址）：
mov eax, [global_var]        // 硬编码地址

// PIC（通过 GOT 间接寻址）：
mov eax, [rip + global_var@GOTPCREL]   // 相对于当前指令指针(rip)的偏移
```

**`-fPIC` vs `-fpic`**：
- `-fPIC`：使用大偏移表，适合大型共享库，无符号数限制
- `-fpic`：使用小偏移表，适合小型共享库（GOT ≤ 16KB），符号数有限制

生产代码用 `-fPIC`（安全第一）。

### 4.4 实验

```bash
# 编译共享库
cat > libhello.c << 'EOF'
#include <stdio.h>
void say_hello(void) { printf("Hello from shared lib!\n"); }
EOF

gcc -fPIC -shared libhello.c -o libhello.so

# 编译使用共享库的程序
cat > main.c << 'EOF'
void say_hello(void);
int main(void) { say_hello(); return 0; }
EOF

gcc main.c -L. -lhello -o main_dyn

# 设置库搜索路径并运行
LD_LIBRARY_PATH=. ./main_dyn

# 查看动态依赖
ldd main_dyn
# 查看 PLT
objdump -d -j .plt main_dyn | head -20
# 查看 GOT
readelf -r main_dyn | grep -E 'say_hello|printf'
```

---

## 5. 动态库搜索路径

### 5.1 搜索顺序

1. `LD_LIBRARY_PATH`（环境变量 — 调试用，**不用于生产部署**）
2. `DT_RPATH`（二进制中硬编码的路径 — `-Wl,-rpath,/path`）
3. `DT_RUNPATH`（类似 rpath，但优先级更低，可被 `LD_LIBRARY_PATH` 覆盖）
4. `/etc/ld.so.cache`（由 `ldconfig` 构建的缓存）
5. `/lib`、`/usr/lib`（系统默认路径）

### 5.2 实验

```bash
# ldd 显示库依赖解析
ldd /usr/bin/python3

# ldconfig 缓存
ldconfig -p | grep libcurl

# rpath 嵌入
gcc main.c -L. -lhello -Wl,-rpath,. -o main_rpath
# main_rpath 运行时在 . 下找 libhello.so，不需要 LD_LIBRARY_PATH
```

### 5.3 符号版本控制：.so 版本的真相

`libfoo.so` → `libfoo.so.1` → `libfoo.so.1.0.0` 的三级链接结构防止 .so 地狱：

```bash
ls -l /lib/x86_64-linux-gnu/libcurl.so*
# libcurl.so        → libcurl.so.4       (编译时链接 — ld 使用)
# libcurl.so.4      → libcurl.so.4.8.0   (运行时链接 — ld-linux.so 使用)
# libcurl.so.4.8.0                        (实际文件 — 包含所有符号)
```

**SONAME**（嵌入在 .so 的 ELF 头中）是运行时查找的关键：

```bash
readelf -d /usr/lib/libcurl.so | grep SONAME
# 输出：0x000000000000000e (SONAME)  Library soname: [libcurl.so.4]
```

**符号版本**（symbol versioning）允许同一 .so 的多个不兼容版本共存：

```bash
nm /lib/x86_64-linux-gnu/libc.so.6 | grep ' pthread_create'
# pthread_create@@GLIBC_2.34   ← @@ = 默认版本
# pthread_create@GLIBC_2.2.5   ← @ = 旧版兼容符号
```

这使得 glibc 可以同时提供新版和旧版接口，旧程序链接到 `@GLIBC_2.2.5`，新程序链接到 `@@GLIBC_2.34`。

### 5.4 LD_PRELOAD：原理与防御

LD_PRELOAD 使指定 .so 的符号优先级**高于** libc.so——这意味着可以拦截 `open()`、`malloc()`、`printf()`：

```bash
# 原理：LD_PRELOAD 的 .so 在符号查找中排在最前面
# 如果 libevil.so 定义了 open()，所有 open() 调用都会先进入 libevil.so

# 防御 1：Full RELRO（-z now）强制启动时解析所有符号
# 防御 2：检查 /proc/self/maps 确认 LD_PRELOAD 没有注入未知 .so
# 防御 3：静态链接（消除动态链接的 LD_PRELOAD 攻击面）
```

### 5.5 静态链接高级选项

`--whole-archive` 强制包含 `.a` 中所有 `.o`（即使无符号引用）：

```bash
gcc -static main.c -Wl,--whole-archive libplugins.a -Wl,--no-whole-archive
# plugin 的 init 函数可能没有被任何符号引用，但必须链接
```

`--start-group ... --end-group` 解决库之间的循环依赖：

```bash
gcc main.o -Wl,--start-group -lA -lB -Wl,--end-group
# 链接器会在 A 和 B 之间反复重扫，直到所有符号都解析完毕
```

**陷阱**：`__attribute__((constructor))` 函数在静态链接时可能不被执行（如果链接器认为包含它的 .o 没有被任何符号引用，可能被丢弃——这时需要 `--whole-archive`）。

---

## 6. 总结

| 概念 | 一句话 |
|---|---|
| ELF | Linux 标准的可执行文件格式 — 链接视图（节） vs 执行视图（段） |
| 静态链接 | 库代码嵌入可执行文件 — 独立但体积大 |
| 动态链接 | 运行时由 ld-linux.so 加载 — 体积小、可升级、有 LD_PRELOAD 风险 |
| GOT | 全局偏移表 — 存储函数/数据运行时地址，动态链接核心数据结构 |
| PLT | 过程链接表 — 首次调用触发懒惰绑定，后续一次间接查找 |
| 懒惰绑定 | `_dl_runtime_resolve` 按需解析 — 首次慢，后续快 |
| PIC | 位置无关代码 — @GOTPCREL 寻址，共享库必备 |
| `.rela.plt` vs `.rela.dyn` | plt=函数重定位（可延迟），dyn=数据重定位（必须启动前完成） |
| PIE | 可执行文件本身也支持 ASLR — `readelf -h` 看 Type: DYN |
| SONAME | .so 的运行时标识 — `libcurl.so.4`，防止版本冲突 |
| 符号版本 | 同一 .so 提供多版本兼容接口 — `@@GLIBC_2.34` vs `@GLIBC_2.2.5` |
| LD_PRELOAD | 符号劫持 — 优先级高于 libc → 用 Full RELRO 或静态链接防御 |
| `--whole-archive` | 强制链接 .a 中所有 .o — 静态链接的插件系统基础 |
| `readelf -r` | 查看重定位表 — `.rela.plt` + `.rela.dyn` |

**核心观点**：ELF 不是"一个可执行文件"——它是一套双重视图的结构化容器。动态链接通过 GOT/PLT/重定位表的协作实现"共享库代码一份、数据各进程独立"——这是现代 Linux 程序体积小的根本原因。但也是 LD_PRELOAD、符号版本冲突等生产问题的根源。

---

## 验收要点

1. ELF 的"双重视图"是什么？链接视图和执行视图的区别？
2. `.bss`、`.plt`、`.got` 三个节各是什么？哪个在文件中不占空间？
3. 懒惰绑定的完整流程是什么？用 GOT/PLT 的协作解释
4. `.rela.plt` 和 `.rela.dyn` 的区别？为什么函数重定位可以延迟但数据重定位必须启动前完成？
5. PIE vs non-PIE：如何通过 `readelf -h` 区分？PIE 与 ASLR 的关系？
6. 动态库的搜索顺序是什么？soname、rpath、LD_LIBRARY_PATH 各自的优先级？
7. LD_PRELOAD 为什么能劫持符号？三种防御方式是什么？
8. 符号版本控制解决什么问题？`@@` 和 `@` 的区别？
9. 如何用 `/proc/PID/maps` 查看进程的地址空间布局？
10. `--whole-archive` 的使用场景是什么？静态链接为什么不执行 constructor 函数？
