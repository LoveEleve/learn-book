# GDB 进阶

> **对应 Layer**：Layer 9（调试与性能优化），参考 C-C++技术专家学习路线.md § Layer 9  
> **参考书章节**：高效C/C++调试 全书（第1-11章）；极致C 第22章  
> **前置依赖**：S4Ch01（strace—系统调用追踪）；S2Ch03（ELF—符号与调试信息）  
> **主线标准**：C11/C17 + GDB 12+

---

## 1. 引言：你从来不调试 C 代码——你只调试崩溃

Java 的 `NullPointerException` 在抛出时有完整调用栈、文件行号、变量值——你不需要 GDB。C 的 segfault 只有一条 `Segmentation fault (core dumped)`——没有栈、没有行号、没有变量——你需要 GDB 从 core dump 中重建程序死前最后一刻的全部状态。

这不是调试——这是**法医学**。技术专家不是"知道 GDB 命令"，而是"能从 2GB 的 core file 中 5 分钟内定位崩溃的根本原因"。

---

## 2. 基础：断点、单步与变量检查

### 2.1 核心命令速查

| 命令 | 缩写 | 作用 |
|---|---|---|
| `break main` | `b main` | 在 main 函数设置断点 |
| `break file.c:42` | `b file.c:42` | 在第 42 行设置断点 |
| `run` | `r` | 运行程序 |
| `next` | `n` | 执行下一行（不进入函数） |
| `step` | `s` | 执行下一行（进入函数） |
| `finish` | `fin` | 执行到当前函数返回 |
| `continue` | `c` | 继续执行到下一个断点 |
| `print x` | `p x` | 打印变量 x |
| `print /x x` | `p/x x` | 以十六进制打印 x |
| `backtrace` | `bt` | 打印调用栈 |
| `info locals` | — | 打印当前函数的局部变量 |

### 2.2 启动方式

```bash
gdb ./myapp                  # 正常启动
gdb -p 12345                 # attach 到运行中的进程
gdb ./myapp core.dump       # 分析 core dump
gdb -tui ./myapp             # TUI 模式——上半源码/下半命令
```

### 2.3 实验——追踪一个简单崩溃

```bash
cat > crash.c << 'EOF'
#include <stdio.h>
void broken(char *s) { s[0] = 'X'; }  // s 是 NULL！
int main() { broken(NULL); return 0; }
EOF
gcc -g -O0 crash.c -o crash
gdb ./crash
(gdb) run
# Program received signal SIGSEGV, Segmentation fault.
# 0x... in broken (s=0x0) at crash.c:3
# 3    void broken(char *s) { s[0] = 'X'; }
(gdb) bt
# #0  broken (s=0x0) at crash.c:3
# #1  0x... in main () at crash.c:4
```

---

## 3. 条件断点与 watchpoint

### 3.1 条件断点——只在特定情况下停

```bash
(gdb) break process_request if id == 9999
# 只在 id=9999 时停下——不用手动 continue 9998 次

(gdb) break foo if x > 100 && flag == 1
# 支持任意表达式（但不能有副作用——GDB 在断点处计算条件）
```

**生产案例**：一个循环处理 10000 条记录的 bug 只在第 8765 条触发——条件断点 `break handle_record if index == 8765` 直接跳到目标，省去 8764 次 `continue`。

### 3.2 watchpoint——变量改变时暂停

```bash
(gdb) watch counter          # counter 被写入时停下
(gdb) rwatch counter         # counter 被读取时停下
(gdb) watch *0x602010        # 监控特定地址
```

**watchpoint 的内核实现**：GDB 在硬件调试寄存器（x86 的 DR0-DR3）和操作系统注入的断点之间做选择。硬件寄存器最多 4 个——超过 4 个 watchpoint 转为软件单步（极慢）。

### 3.3 实验——追踪野指针

```bash
cat > watch.c << 'EOF'
#include <stdlib.h>
int main() {
    int *p = malloc(sizeof(int));
    *p = 42;
    free(p);
    *p = 100;  // UAF — p 是已释放内存
    return 0;
}
EOF
gcc -g -O0 watch.c -o watch
gdb ./watch
(gdb) watch *p            # 监控 p 指向的内存
(gdb) run
# Hardware watchpoint: *p
# Old value = 42
# New value = 0  ← free 把内存归还给分配器——GDB 捕获到变化
(gdb) continue
# New value = 100 ← 又变了！这就是 UAF
```

---

## 4. 逆向调试——让时间倒流

### 4.1 核心命令

```bash
(gdb) record                 # 开始记录执行（同时启动逆向调试）
(gdb) continue               # 执行到 bug
(gdb) reverse-next           # 回到前一行
(gdb) reverse-continue       # 回到上一个断点
(gdb) reverse-step           # 回到前一行（进入函数）
```

**逆向调试的代价**：`record` 记录了每条指令的效果——性能下降 ~1000 倍。仅用于再现 bug 的那一小段代码——不要在 `main()` 开始就开 record。

### 4.2 生产案例——追踪一个随机 bug

```bash
(gdb) break crash_handler
(gdb) run
# 程序崩溃——但我们不知道崩溃前是什么状态
(gdb) bt                   # 调用栈：
# #0  segfault_handler
# #1  <signal handler called>
# #2  process_record (r=0x602010)
# #3  main_loop
(gdb) record               # 现在开 record
(gdb) reverse-continue    # 回到进入 process_record 的时刻
(gdb) info locals
# r = 0x602010, r->id = 0, r->data = NULL  ← 这就是崩溃根因！
```

---

## 5. Python 脚本化 GDB

### 5.1 内置脚本

```python
# save as dump_bt.py
import gdb

class DumpBacktrace(gdb.Command):
    """dump_bt — 保存当前栈到文件"""
    def __init__(self):
        super().__init__("dump_bt", gdb.COMMAND_USER)

    def invoke(self, arg, from_tty):
        gdb.execute("set logging file bt.log")
        gdb.execute("set logging on")
        gdb.execute("thread apply all backtrace full")
        gdb.execute("set logging off")

DumpBacktrace()
```

```bash
(gdb) source dump_bt.py
(gdb) dump_bt
```

### 5.2 自动化——遍历所有栈帧的局部变量

```python
import gdb

frame = gdb.selected_frame()
while frame:
    print(f"# {frame.name()}:")
    block = frame.block()
    for sym in block:
        if sym.is_variable:
            val = frame.read_var(sym)
            print(f"  {sym.name} = {val}")
    frame = frame.older()
```

---

## 6. Core Dump 分析

### 6.1 启用 core dump

```bash
ulimit -c unlimited                         # 允许 core dump 无大小限制
echo '/var/core/core.%e.%p.%t' | sudo tee /proc/sys/kernel/core_pattern
# %e = 可执行文件名, %p = PID, %t = 时间戳
```

### 6.2 从 core dump 中提取信息

```bash
gdb ./myapp /var/core/core.myapp.12345.1700000000

(gdb) bt                          # 崩溃时的调用栈
(gdb) frame 0                     # 切到崩溃帧
(gdb) info registers              # 崩溃时的寄存器值
(gdb) x/10x $rsp                  # 查看栈内存（前 10 个 16 进制值）
(gdb) info proc mappings          # 进程地址空间映射
(gdb) thread apply all bt         # 所有线程的调用栈
```

### 6.3 `backtrace_symbols`——应用层调用栈

```c
#include <execinfo.h>
#include <signal.h>

void crash_handler(int sig) {
    void *bt[32];
    int n = backtrace(bt, 32);
    // 写入文件而非 stderr——信号处理器用 write 而非 fprintf
    backtrace_symbols_fd(bt, n, 2);  // fd 2 = stderr
    _exit(1);
}

int main() {
    signal(SIGSEGV, crash_handler);
    // ...
}
```

**重要**：`backtrace_symbols_fd` 是 glibc 扩展——musl libc 和 macOS 不支持。跨平台方案用 `libunwind`。

### 6.4 符号化——addr2line

```bash
# 从崩溃日志中提取地址
addr2line -e ./myapp -f -C 0x401234 0x401500
# 输出：
# process_request
# /home/user/src/server.c:156
# handle_reply
# /home/user/src/server.c:89
```

---

## 7. 多线程调试

```bash
(gdb) info threads              # 列出所有线程
(gdb) thread 3                  # 切换到线程 3
(gdb) thread apply all bt       # 所有线程的调用栈
(gdb) set scheduler-locking on  # 只运行当前线程，其他冻结
```

**致命陷阱**：`continue` 默认让**所有线程**运行——不是只有当前线程。如果你需要单步当前线程而其他线程不运行——必须 `set scheduler-locking step`。

---

## 8. 总结

| 概念 | 核心知识 |
|---|---|
| `break` 条件 | `break foo if x>100` — 跳过不需要的调用 |
| watchpoint | 硬件寄存器 DR0-DR3 支持——4 个限额 |
| `record + reverse` | 逆向调试——1000x 性能代价，仅用于小范围再现 |
| GDB Python | 自动化栈帧分析、批量检查、定制命令 |
| core dump | `ulimit -c unlimited` + `bt/info registers` + `addr2line` 符号化 |
| 多线程调试 | `scheduler-locking` — 防止其他线程干扰单步 |

**核心观点**：GDB 不是"断点+printf"的替代品——它是**法医学工具**。技术专家不是"知道 GDB 有哪些命令"，而是"知道如何在 core dump 中从寄存器值重建崩溃时刻的完整状态——`$rip` 告诉你在哪一行、`$rsp` 告诉你在哪个栈帧、`$rax` 告诉你函数返回值——这三个寄存器加上调用栈构成了崩溃分析的完整法医学证据链"。

---

## 验收要点

1. `break file.c:42 if x > 100` — 解释条件断点如何使用
2. watchpoint 的硬件限制是什么？超过 4 个 watchpoint 后 GDB 做什么？
3. `record + reverse-continue` 的代价是什么？为什么不能全程开？
4. 如何用 GDB Python 脚本遍历所有栈帧的局部变量？
5. core dump 分析的三步法：`bt` + `info registers` + `addr2line`
6. 多线程调试中 `scheduler-locking step` 的作用？
7. `backtrace_symbols_fd` 的可移植性限制是什么？
---

## 面试题

**Q1：硬件 watchpoint vs 软件 watchpoint，GDB 如何选择？**

x86 的调试寄存器 DR0-DR3 支持 4 个硬件 watchpoint——CPU 在每次内存访问时自动比较地址与 DR0-DR3，命中断点。第 5 个 watchpoint 开始，GDB 切换为**软件 watchpoint**——单步执行每条指令并在每一步后比较值。软件 watchpoint 比硬件 watchpoint 慢约 1000 倍。

**追问**："逆向调试（record）为什么也是 ~1000x 慢？"——GDB 记录每条指令执行前的内存和寄存器快照。每条指令都消耗 1 次检查+1 次记录，等于每条指令都达到了断点——这就是 1000x 的来源。

**Q2：core dump 没有激活怎么办？三步排查。**

1) `ulimit -c` → 如果是 0，`ulimit -c unlimited`（当前 shell 有效）。2) `/proc/sys/kernel/core_pattern` → 确认路径可写。3) 如果服务通过 systemd 启动，需要设 `LimitCORE=infinity` 在 service 文件中。systemd 继承 ulimit 但不继承 shell 的 ulimit——这是常见陷阱。
