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
strace ./strace_test 2>&1 | grep write
# 输出两个独立的 write 调用：
# write(1, "World\n", 6)    ← §3 中直接的 write() 系统调用 — 立即发出
# write(1, "Hello ", 6)     ← exit 时 fclose(stdout) 自动 flush — printf 的缓冲在这时才写出
# 注意：stdio 缓冲和直接 write 是两条独立路径 — 它们的内容不会被合并成一个 syscall

# 对比：重定向到文件（全缓冲）
strace -o trace.log ./strace_test > /dev/null 2>&1
grep write trace.log
# 同样的两个独立的 write — printf 的 "Hello " 仍然在 fclose flush 时发出，
# write(1, "World\n", 6) 在程序退出之前已经调用了
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

### 3.3 stdio 缓冲——三个级别 + 实战控制

```c
// 改变缓冲模式
setvbuf(stdout, NULL, _IONBF, 0);   // 无缓冲——调试用

char buf[8192];
setvbuf(fp, buf, _IOFBF, sizeof(buf)); // 全缓冲——自定义缓冲区

// fflush——强制刷新 stdio 缓冲区
fprintf(stderr, "Error!\n");  // stderr 无缓冲——立即可见
fprintf(stdout, "Processing");  // 行缓冲——没有 \n 不刷新
fflush(stdout);                // 强制刷新——立即输出
```

**ferror/feof——错误与 EOF 检测**：

```c
if (ferror(fp)) {
    perror("Read error");  // errno 由底层 read 设置
    clearerr(fp);          // 清除错误标志——后续操作可以继续
}
if (feof(fp)) {
    printf("End of file reached\n");
}
```

### 3.4 stdio 缓冲——三种级别

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

### 3.4 生产陷阱：短写、EINTR 与 fdatasync

**短写（partial write）**

`write(fd, buf, 4096)` 可能返回 1024——对管道和 socket 尤其常见（内核缓冲满）。正确的写入必须循环直到所有字节写完：

```c
ssize_t writen(int fd, const void *buf, size_t n) {
    size_t total = 0;
    while (total < n) {
        ssize_t w = write(fd, (char *)buf + total, n - total);
        if (w < 0) {
            if (errno == EINTR) continue;
            if (w == 0) { errno = ENODATA; return -1; }  // write 返回 0 防止死循环
            return -1;
        }
        total += w;
    }
    return total;
}
```

**EINTR（信号中断）重试**

`read` 被信号中断后返回 -1 + `errno = EINTR`——必须重试而非报错：

```c
ssize_t ret;
do {
    ret = read(fd, buf, size);
} while (ret == -1 && errno == EINTR);
```

POSIX 提供 `TEMP_FAILURE_RETRY` 宏自动处理重试（仅 Linux/glibc）。

**fdatasync vs fsync**

`fsync(fd)` 强制同时刷新**数据**和**元数据**（inode 的最后修改时间、文件大小）。`fdatasync(fd)` 只刷新数据和文件大小——省略了 inode 时间戳的写入，对大多数日志/数据写入场景足够且开销减半。用对函数可以提升一倍持久化性能。

### 3.5 进阶：pread/pwrite 与 O_APPEND

**`pread`/`pwrite`——原子定位+读写**

```c
ssize_t pread(int fd, void *buf, size_t n, off_t offset);
// 等价于 lseek(offset) + read() — 但不修改 fd 的文件偏移量
// 线程安全——两个线程可以同时 pread 同一 fd 的不同偏移量互不干扰
```

**`O_APPEND`——原子追加写入**

```c
int fd = open("log.txt", O_WRONLY | O_APPEND);
write(fd, "log entry\n", 10);  // 原子地将数据追加到文件末尾
// 即使多个进程同时写入同一文件——每次 write 都是原子的定位+写入
```

`O_APPEND` 是日志文件的标配——多个进程同时往同一个日志文件写不会互相覆盖。

**`mmap` 文件映射（预告）**

`mmap` 将文件直接映射到进程地址空间——读写文件就像读写内存。相比 `read` 的优势：零拷贝（数据直接从 page cache 映射到用户态，不经过 `read` 的缓冲区拷贝）。详见 S4Ch03（IPC 与 mmap）。


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

### 4.3 fcntl——文件描述符控制

`fcntl`（file control）是 fd 的配置入口——获取/设置 flags、设置 FD_CLOEXEC、获取/设置文件锁：

```c
#include <fcntl.h>

// 获取 fd 标志
int flags = fcntl(fd, F_GETFL);
if (flags & O_NONBLOCK) { /* fd 是非阻塞模式 */ }

// 设置 fd 标志（加 O_NONBLOCK）
fcntl(fd, F_SETFL, flags | O_NONBLOCK);

// FD_CLOEXEC——exec 时自动关闭 fd
// 没有 FD_CLOEXEC：exec 后子进程继承 fd → 泄漏
fcntl(fd, F_SETFD, FD_CLOEXEC);  // 或 open() 时用 O_CLOEXEC
```

**FD_CLOEXEC 的生产意义**：如果忘了设置，fork+exec 的子进程会继承父进程的所有 fd——包括监听 socket、数据库连接、日志文件。子进程（现在是新程序）不知道这些 fd 的存在→泄漏。

### 4.4 dup/dup2——复制文件描述符

重定向标准 I/O 的核心操作：

```c
int fd = open("output.log", O_WRONLY | O_CREAT, 0644);
dup2(fd, STDOUT_FILENO);  // stdout(1) 现在指向 output.log
close(fd);                 // 关闭原始 fd——stdout 的新连接不受影响
printf("This goes to output.log\n");  // 写入文件！
```

`dup2` 的经典用法：pipe+dup2 实现 `ls | wc -l` 的 shell 管道。

### 4.5 stat/fstat/lstat——文件元数据

```c
#include <sys/stat.h>

struct stat st;
stat("/etc/passwd", &st);              // 跟随符号链接
// or: lstat(path, &st)                // 不跟随符号链接—查看链接本身
// or: fstat(fd, &st)                   // 通过已打开的 fd

printf("Size: %ld bytes\n", st.st_size);       // 文件大小
printf("Mode: %o\n", st.st_mode);              // 权限位
printf("UID: %d, GID: %d\n", st.st_uid, st.st_gid);
printf("Last modified: %ld\n", st.st_mtime);   // Unix 时间戳

// 检查文件类型：
if (S_ISREG(st.st_mode))  printf("Regular file\n");
if (S_ISDIR(st.st_mode))  printf("Directory\n");
if (S_ISLNK(st.st_mode))  printf("Symbolic link\n");
```

### 4.6 fd 泄漏诊断

文件描述符是有限资源（`ulimit -n` 通常默认 1024）。忘记 `close(fd)` 会导致程序长期运行后无法 `open` 新文件或 `accept` 新连接——报错 `EMFILE`（Too many open files）。诊断工具：

```bash
# 查看进程当前打开的 fd
ls -la /proc/PID/fd

# 查看某进程打开的文件（包括 socket/pipe）
lsof -p PID

# 检查 limit
ulimit -n     # 查看软限制
ulimit -n 65536  # 临时提高
```

**常见泄漏模式**：打开文件在错误路径上没有 `close`。解决方案：在出错路径上也 `close`、使用 `goto cleanup` 模式，或使用 `close` 的 `__attribute__((cleanup))`。


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

**为什么内核要用 Page Cache？**

不是"先写到内存再写到磁盘"——Page Cache 是内核的核心 IO 加速层，有三个核心收益：
1. **合并小写**：多个 1 字节的 `write` 在内核中被合并成一个 4KB 的磁盘块写入——大幅减少磁盘操作
2. **读缓存命中**：`read` 首先查 page cache——如果数据已经在缓存中（刚刚写过或读过），直接返回，完全不需要磁盘 I/O
3. **Write-behind**：内核在后台（kworker 线程）异步地将脏页写入磁盘——不阻塞 `write` 调用者

**关键含义**：`write` 返回只意味着"数据在内核的 page cache 中"——还没到磁盘。如果此时机器崩溃，page cache 中的数据丢失。这就是为什么数据库的持久化需要 `fsync` 或 `fdatasync`。

### 5.2 `fsync` — 强制刷新到磁盘

```c
write(fd, data, size);
fsync(fd);   // 等待直到数据写入磁盘——真正的持久化
```

`fsync` 的代价取决于存储介质：HDD 上 100-300ms（寻道+写入），SATA SSD 上 1-10ms，NVMe SSD 上 0.1-1ms。大多数应用不需要 `fsync` 的保证（丢失几秒的日志可接受），但数据库必须在每次事务提交后调用。

---


### 5.3 O_SYNC / O_DSYNC——同步写入标志

```c
int fd = open("data.bin", O_WRONLY | O_SYNC);  // 每次 write 自动 fsync——数据+元数据
int fd = open("data.bin", O_WRONLY | O_DSYNC); // 只同步数据+文件大小——不刷新 inode 时间戳
```

`O_DSYNC` 相比 `O_SYNC` 省略了 inode 时间戳同步（类似 fdatasync vs fsync 的关系）。对大多数日志写入场景足够且性能更好。但每次 write 都等磁盘——对频繁小写入不友好。

### 5.4 sendfile——零拷贝文件传输

```c
sendfile(out_fd, in_fd, NULL, file_size);
// 从 in_fd（文件）→ out_fd（socket）——全程在内核中完成，不经过用户态缓冲区
```

Web 服务器的静态文件传输标准方案：`read` 文件→用户态 buffer→`write` socket = 两次内存拷贝。`sendfile` 在内核中直接完成——零拷贝（实际共享 page cache 页）。

### 5.5 fallocate/ftruncate——预分配与截断

```c
// 预分配磁盘空间——避免后续 write 时的 block 碎片化
fallocate(fd, 0, 0, 1024 * 1024 * 1024);  // 预分配 1GB

// 截断文件——如果文件大于 size，截断；如果小于，扩展并填零
ftruncate(fd, 1024 * 1024);               // 文件变为 1MB
```

数据库和虚拟机镜像用 `fallocate` 确保连续物理空间——减少磁盘寻道开销。

### 5.6 O_DIRECT——绕过 Page Cache

`O_DIRECT` 让 `read`/`write` 直接操作磁盘块，绕过 page cache。数据库（MySQL InnoDB, PostgreSQL）通常自己管理缓存池——如果 page cache 也缓存同样的数据，就形成了"双重缓存"问题：同样一份数据在数据库自己的缓存池和内核 page cache 中各有一份，浪费了一半内存。

`O_DIRECT` 的代价：不能利用 page cache 的合并写入和预读取——每次 I/O 都是真实的磁盘操作。只在有自己缓存策略的场景（数据库、虚拟机镜像）中使用。

---

## 6. 总结

| 概念 | 一句话 |
|---|---|
| 系统调用 vs 库函数 | syscall = 用户态→内核态切换（~50-100ns），库函数 = 纯用户态调用 |
| `strace` | 追踪进程的所有系统调用——调试 I/O 行为最直观的工具（非生产环境，有性能开销）|
| stdio 缓冲 | 行缓冲（`\n` 触发）/ 全缓冲（缓冲区满触发）/ 无缓冲 |
| fork 缓冲陷阱 | 子进程继承父进程的 stdio 缓冲区——可能重复输出 |
| 文件描述符 | 整数 → 内核 fd 表 → open file table → inode |
| Page Cache | `write` 先写入内核内存、`fsync` 再刷新到磁盘 |
| fcntl | fd 控制——get/set flags、FD_CLOEXEC、文件锁 |
| dup/dup2 | 复制 fd——重定向标准 I/O、shell 管道实现 |
| stat/fstat | 文件元数据——大小/权限/时间戳/类型判断 |
| `O_SYNC`/`O_DSYNC` | 同步写入——每次 write 自动同步磁盘 |
| sendfile | 零拷贝文件传输——数据在内核中直接转发 |
| fallocate | 预分配磁盘空间——数据库/虚拟机防碎片化 |
| `O_DIRECT` | 绕过 page cache——数据库自做缓存时避免双重缓存 |

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
10. 短写（partial write）是什么？如何正确写出 `writen()` 循环？
11. `EINTR` 是什么？`read` 返回 `EINTR` 后应该如何正确处理？
12. `fdatasync` 和 `fsync` 的区别是什么？什么时候用 `fdatasync` 就够了？
13. `pread` 和 `read` 的区别是什么？为什么 `pread` 是线程安全的？
14. `O_APPEND` 解决什么问题？多个进程同时写同一个日志文件安全吗？
15. `O_DIRECT` 绕过 page cache——什么场景下应该用它？什么场景下不应该？
16. `fcntl(fd, F_SETFD, FD_CLOEXEC)` 解决什么问题？不设置的后果是什么？
17. `dup2(fd, STDOUT_FILENO)` 做了什么？给一个重定向的代码示例
18. `stat` 能获取哪些文件元数据？`S_ISREG` 和 `S_ISDIR` 的用途是什么？
19. `sendfile` 相比 `read+write` 的零拷贝优势是如何实现的？
20. `fallocate` 和 `ftruncate` 的区别是什么？各自的适用场景？
