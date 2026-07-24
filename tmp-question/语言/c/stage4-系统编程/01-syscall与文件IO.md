# 系统调用与文件 I/O

> **对应 Layer**：Layer 4（OS 接口），参考 C-C++技术专家学习路线.md § Layer 4  
> **参考书章节**：Practical 全书（Linux 系统编程入门）；Mastering 第1-3章；极致C 第10-11章  
> **前置依赖**：Stage1 Ch1（编译模型—strace 已出现）  
> **主线标准**：C11/C17 + Linux 系统调用（POSIX）

---

## 1. 引言：`printf` 背后的内核

在任何 C 程序中，`printf("hello\n")` 最终不是写到屏幕——而是调用 `write(1, "hello\n", 6)` 系统调用，从用户态陷入内核，由内核把数据写入终端或文件。本章从"什么是系统调用"出发，深入文件 I/O 的完整路径——从用户态的 `fopen` 到内核的 VFS（Virtual File System，虚拟文件系统）和 page cache。

---

## 2. 系统调用 vs 库函数

### 2.1 根本区别

| 维度 | 系统调用（syscall） | 库函数（libc） |
|---|---|---|
| 实现位置 | **内核态** — 内核代码 | **用户态** — libc（如 glibc/musl） |
| 入口 | `syscall` 指令（x86）/ `svc` 指令（ARM）— **用户态→内核态切换** | 普通函数调用 — `call` 指令 |
| 开销 | 高—模式切换（~50-100ns）+ 可能的内核锁 | 低—纯用户态指令（~1-5ns） |
| 例子 | `read`/`write`/`open`/`close`/`fork`/`mmap` | `printf`/`fread`/`malloc`/`strcpy` |
| 缓冲 | **无**（直接到内核） | **有**（stdio 缓冲区—行缓冲/全缓冲/无缓冲） |
| 错误 | 返回 -1 + `errno` | 返回 -1 或 NULL + `errno`（通过 syscall 传递） |

**关键洞察**：库函数可能调用系统调用（`printf` → `write`），也可能不调用（`strcpy` — 纯用户态内存复制）。系统调用的代价不在指令本身（`syscall` 指令只占几个周期），而在**模式切换**——CPU 从 Ring 3（用户态）切换到 Ring 0（内核态），需要保存/恢复大量寄存器、切换内存映射、可能刷新 TLB（Translation Lookaside Buffer，页表缓存）。

### 2.2 实验——用 `strace` 看系统调用

```bash
cat > strace_test.c << 'EOF'
#include <stdio.h>
#include <unistd.h>

int main() {
    printf("Hello ");           // 行缓冲——没有 \n 不刷新
    write(1, "World\n", 6);    // 直接系统调用——立即输出
    return 0;
}
EOF
gcc -Wall strace_test.c -o strace_test

# 看所有系统调用
strace ./strace_test
# 输出：write(1, "Hello World\n", 12)  — 注意 printf 的 "Hello " 和 write 的 "World\n" 可能被合并
# 因为 printf 把 "Hello " 放入 stdio 缓冲区 → write 触发 → 混合输出
# 然后程序退出 → fclose 自动 flush → 没有额外的 write

# 对比：重定向到文件（全缓冲）
strace -o trace.log ./strace_test > /dev/null
grep write trace.log
# 输出：write(1, "World\nHello ", 12)  — 顺序变了！write 先执行，printf 的 "Hello " 在缓冲区中，最后 fclose flush
```

---

## 3. 文件 I/O：两套 API

### 3.1 syscall 级 API

```c
#include <fcntl.h>
#include <unistd.h>

int fd = open("file.txt", O_RDWR | O_CREAT, 0644);
if (fd < 0) { perror("open"); return -1; }

char buf[1024];
ssize_t n = read(fd, buf, sizeof(buf));    // 返回值 = 实际读到的字节数
if (n < 0) { perror("read"); close(fd); return -1; }

write(fd, "hello", 5);                     // 写入

off_t pos = lseek(fd, 0, SEEK_SET);       // 定位

close(fd);
```

**`read` 返回值的语义**：
- `> 0` → 读到 n 字节
- `= 0` → EOF（文件结束）— 这对于管道/socket 意味着对端关闭了连接
- `< 0` → 错误— `errno` 可能是 `EAGAIN`（非阻塞读，暂无数据）或 `EINTR`（被信号中断—需要重试）

### 3.2 stdio 级 API

```c
#include <stdio.h>

FILE *fp = fopen("file.txt", "r+");
if (!fp) { perror("fopen"); return -1; }

char line[256];
while (fgets(line, sizeof(line), fp)) {
    printf("%s", line);
}

fprintf(fp, "new data\n");
fclose(fp);
```

### 3.3 stdio 缓冲——三个级别

| 缓冲类型 | 触发刷新条件 | 默认适用对象 |
|---|---|---|
| **无缓冲**（`_IONBF`） | 每次 `fwrite` 立即执行 `write` | `stderr` |
| **行缓冲**（`_IOLBF`） | 遇到 `\n` 或缓冲区满或 `fflush()` | `stdout`（当 fd 是终端时） |
| **全缓冲**（`_IOFBF`） | 缓冲区满（默认 4096/8192 字节）或 `fflush()` | `stdout`（当 fd 被重定向到文件时） + 所有普通文件 |

**这导致了两个经典的 stdio bug**：

**Bug 1：`printf` 后 `fork`——子进程继承了父进程的缓冲**

```c
printf("Hello ");         // "Hello " 在 stdio 缓冲区中——还没刷新到内核
fork();                   // 子进程完整复制父进程的内存（包括 stdio 缓冲区）
                          // 当两个进程退出时，各自 flush → "Hello " 被打印两次！
```

**Bug 2：stdout 重定向到文件后变成全缓冲——看不到输出**

```bash
./myapp > output.log      # stdout 变成全缓冲
# 程序运行 10 秒——你看不到任何进度，因为数据在缓冲区中
# 直到缓冲区满（4-8KB）或程序退出才能看到
```

**解决方案**：`setvbuf(stdout, NULL, _IONBF, 0)` 强制无缓冲（调试用），或定期 `fflush(stdout)`。

---

## 4. 文件描述符的本质

### 4.1 fd 是什么

文件描述符（file descriptor / fd）是一个**整数**——指向内核 **文件描述符表**中的一项。每个进程有自己的文件描述符表：

```
进程 A 的 fd 表：                  内核 open file table：
  fd 0 → stdin                         entry X: inode=12345, offset=0, flags=O_RDONLY
  fd 1 → stdout                        entry Y: inode=67890, offset=1024, flags=O_RDWR
  fd 2 → stderr                        entry Z: pipe, read-end
  fd 3 → open("data.txt") → entry Y   ← 指向同一个内核入口
```

**关键特性**：
- **fd 是进程本地的**——A 的 fd 3 和 B 的 fd 3 可能指向完全不同的文件
- **多个 fd 可以指向同一个内核文件入口**——`dup()` 或父子进程共享
- **内核文件入口共享文件偏移量**——A 的 `fd 3` 和 B 的 `fd 3` 如果指向同一个内核入口，`read` 会影响对方的文件偏移

### 4.2 实验——fork 后父子进程共享文件偏移量

```bash
cat > fd_test.c << 'EOF'
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>

int main() {
    int fd = open("/tmp/fd_shared", O_RDWR | O_CREAT | O_TRUNC, 0644);
    write(fd, "ABCDEFGHIJ", 10);      // 写 10 字节

    lseek(fd, 0, SEEK_SET);            // 回到文件开头
    if (fork() == 0) {
        char buf[6];
        read(fd, buf, 5); buf[5] = '\0';
        printf("Child read: [%s]\n", buf);   // "ABCDE"
    } else {
        sleep(1);
        char buf[6];
        read(fd, buf, 5); buf[5] = '\0';
        printf("Parent read: [%s]\n", buf);  // "FGHIJ" — 子进程移动了偏移量！
    }
    close(fd);
    return 0;
}
EOF
gcc -Wall fd_test.c -o fd_test && ./fd_test
```

---

## 5. 文件 I/O 的内核实现

### 5.1 从 `write` 到磁盘的完整路径

```
用户态：
  write(fd, "hello", 5)
      ↓ syscall 指令 — 陷入内核
内核态：
  1. VFS 层 — 根据 fd 找到 struct file → struct inode → struct address_space
  2. Page Cache 层 — 将数据写入页（struct page）— 标记为"脏"（dirty）
  3. write 系统调用返回 → 用户态（此时数据还在 page cache 中！）
      ↓ 后台：内核线程（pdflush / kworker）
  4. 脏页刷写 — 将"脏"的 page 写入磁盘块
```

**关键含义**：`write` 返回只意味着"数据在内核的 page cache 中"——还没到磁盘。如果此时机器崩溃，page cache 中的数据丢失。这就是为什么数据库的持久化需要 `fsync` 或 `fdatasync`。

### 5.2 `fsync` — 强制刷新到磁盘

```c
write(fd, data, size);
fsync(fd);   // 等待直到数据写入磁盘——真正的持久化
```

`fsync` 的代价极高——它可能等待几百毫秒（磁盘寻道+写入延迟）。大多数应用不需要 `fsync` 的保证（丢失几秒的日志可接受），但数据库必须在每次事务提交后调用。

---

## 6. 总结

| 概念 | 一句话 |
|---|---|
| 系统调用 vs 库函数 | syscall = 用户态→内核态切换（~50-100ns），库函数 = 纯用户态调用 |
| `strace` | 追踪进程的所有系统调用——调试 I/O 行为的第一工具 |
| stdio 缓冲 | 行缓冲（`\n` 触发）/ 全缓冲（缓冲区满触发）/ 无缓冲 |
| fork 缓冲陷阱 | 子进程继承父进程的 stdio 缓冲区——可能重复输出 |
| 文件描述符 | 整数 → 内核 fd 表 → open file table → inode |
| Page Cache | `write` 先写入内核内存、`fsync` 再刷新到磁盘 |
| `O_DIRECT` | 绕过 page cache——适用于数据库自做缓存（如 MySQL InnoDB） |

**核心观点**：系统调用是用户态和内核态之间的"收费门"——每次 `write` 都付出模式切换的代价。stdio 在用户态做缓冲来减少系统调用频率——这就是为什么 `printf` 不立即输出、`fork` 后缓冲会重复。技术专家不是"知道这些 API 怎么用"，而是"知道 strace 下每次 API 调用背后发出的系统调用序列，以及这些序列在不同缓冲模式下的差异"。

---

## 验收要点

1. 系统调用和库函数的根本区别是什么？各举三个例子
2. `printf("Hello")` 在 strace 下看到了什么系统调用？在没有 `\n` 和有 `\n` 时有什么区别？
3. stdio 的三种缓冲模式是什么？`stdout` 在终端和重定向到文件时分别是什么模式？
4. `fork` 后为什么可能看到 `printf` 的输出重复两次？
5. 文件描述符（fd）的本质是什么？`fork` 后父子进程共享 fd 吗？
6. `write` 返回后数据真的到了磁盘吗？`fsync` 做了什么？
7. `O_DIRECT` 是什么？什么时候应该用它？
8. `read` 返回 0 代表什么？对于文件和管道有什么不同的含义？
9. `strace` 如何追踪一个已运行的进程？给出命令
10. 你写的 C 程序只用了 `FILE *fp = fopen("log.txt", "a")`，在程序崩溃时（SIGSEGV）最后几行日志丢失——为什么？如何修复？
