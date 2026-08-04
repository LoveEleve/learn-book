# /proc 文件系统与文件锁

> **对应 Layer**：Layer 4（OS 接口补充—进程信息 + 并发控制），stage4 补遗第 2 篇  
> **主线标准**：C11/C17 + Linux 系统调用

---

## Part A：/proc 文件系统

### A.1 /proc 是什么

`/proc` 不是真正的磁盘文件——它是内核在内存中维护的虚拟文件系统，每次 `read` 时由内核的回调函数动态生成内容。这意味着：

- **0 磁盘 I/O**——写入 `/proc` 实际上是设置内核参数
- **原子生成**——每次 `read` 从当前内核数据结构中实时生成——不会读到一半被其他进程修改
- **容器差异**——Docker 容器有自己独立的 `/proc` 挂载（PID 命名空间隔离）

**内核实现**：`/proc` 文件用 `seq_file` 接口——这是内核提供的迭代器框架，用于处理大量文本输出。`proc_create()` 注册文件、`seq_show()` 回调生成每一行。

### A.2 核心文件分类

#### 进程级：`/proc/PID/`

```c
// 从 C 代码读进程状态（替代 popen("ps")——更快、更可靠）
FILE *fp = fopen("/proc/self/status", "r");  // self = 当前进程的 PID
char line[256];
while (fgets(line, sizeof(line), fp)) {
    if (strncmp(line, "VmRSS:", 6) == 0)           printf("RSS: %s", line + 6);
    if (strncmp(line, "VmSize:", 7) == 0)          printf("Virt: %s", line + 7);
    if (strncmp(line, "Threads:", 8) == 0)         printf("Threads: %s", line + 8);
    if (strncmp(line, "voluntary_ctxt_switches:", 25) == 0)
        printf("VolCS: %s", line + 25);
}
fclose(fp);
```

| 文件 | 内容 | 关键字段 |
|---|---|---|
| `status` | 内存（VmRSS/VmSize/VmPeak）、线程数、信号掩码、UID/GID | `VmRSS`—物理内存常驻、`Threads`—线程数 |
| `stat` | CPU 时间（utime+stime）、进程状态（R/S/D/Z）、nice 值 | `utime`+`stime`—自启动以来的 CPU 时间 tick |
| `statm` | 内存页统计 | `resident`—物理内存页数 |
| `fd/` | 打开的 fd 列表（符号链接指向实际文件/socket/pipe） | `lsof -p PID` 的等价物 |
| `maps` | 地址空间映射（VMA——Virtual Memory Area） | 共享库加载地址+偏移+权限 |
| `limits` | 资源限制（NOFILE/NPROC/FSIZE） | 与 `getrlimit` 相同的输出——方便 shell 查看 |
| `io` | 磁盘 I/O 统计 | `read_bytes`/`write_bytes`—自启动以来的总量 |
| `cgroup` | cgroup 路径 | 容器/虚拟机环境下的资源限制来源 |

#### 系统级：`/proc/`

| 文件 | 内容 | 用途 |
|---|---|---|
| `meminfo` | 系统内存：Total/Free/Available/Buffers/Cached/Swap | 所有监控脚本的标配——`free -h` 的数据来源 |
| `cpuinfo` | CPU 型号、核数、频率、cache 大小 | 判断硬件能力 + SSE/AVX 指令集支持 |
| `stat` | 系统 CPU 统计（user/nice/system/idle/iowait 总 tick） | 计算 CPU 利用率 |
| `loadavg` | 1/5/15 分钟平均负载 | 负载阈值告警 |
| `uptime` | 系统启动后的秒数 + 空闲时间 | 判断最近是否重启 |
| `version` | 内核版本 | `uname -r` 的等价物 |
| `net/` | 网络统计（TCP 连接/UDP 端口/包计数） | `ss`/`netstat` 的数据来源 |
| `sys/` | 内核参数读写接口 | `sysctl` 的等价物——如 `/proc/sys/net/ipv4/tcp_tw_reuse` |

### A.3 更多 /proc/PID 文件

| 文件 | 内容 | 关键用途 |
|---|---|---|
| **`environ`** | 进程环境变量（零分隔符格式） | 容器 K8s 注入的环境变量——不同于 shell 的 `env` 命令，C 代码读 `/proc/PID/environ` 得到的是 `KUBERNETES_SERVICE_HOST\0PATH=/usr/bin\0...` 的二进制格式 |
| **`cmdline`** | 进程命令行参数（零分隔符） | 判断进程的启动参数——`ps aux` 的数据来源 |
| **`root`** | 符号链接→进程的根目录 | **chroot 安全审计核心**——容器进程的 root 指向容器根，但特权进程可以通过它访问宿主机文件系统（`/proc/PID/root/etc/passwd`） |
| **`cwd`** | 符号链接→进程的当前工作目录 | 判断进程在哪个目录运行——`lsof +D` 的等价物 |

**读 `/proc/PID/environ` 的 C 代码**：

```c
char buf[4096];
int fd = open("/proc/self/environ", O_RDONLY);
ssize_t n = read(fd, buf, sizeof(buf)-1);
buf[n] = '\0';
// 遍历零分隔的键值对
for (char *p = buf; p < buf + n; p += strlen(p) + 1) {
    printf("%s\n", p);  // e.g., "PATH=/usr/bin"
}
close(fd);
```

### A.4 /proc/PID/fd 的内核实现

`/proc/PID/fd/N` 不是磁盘 inode 的符号链接——是**伪符号链接**。`readlink("/proc/PID/fd/3")` 调用 `proc_pid_fd_link()` → 在 `/proc` 的 `dentry->d_op->d_readlink` 中**动态生成**路径字符串：

```
fd → current->files->fdt->fd[3] → struct file → file->f_path → dentry
        → proc_pid_fd_link() → d_path() 动态构造 "/pipe:[12345]" 或 "/tmp/myfile"
```

这就是为什么已删除但未 close 的 fd 在 `/proc/PID/fd/3` 下显示为 `"/tmp/myfile (deleted)"`。

### A.5 生产案例

**Nginx 的 `ngx_proc_stat` 模块**——直接从 `/proc/self/status` 和 `/proc/self/stat` 读取 `VmRSS` 和 CPU 时间，暴露为 HTTP endpoint（`/status` 页面）。不依赖外部工具（`ps`）——更快，且不产生 fork/exec 的开销。

**Redis 的 `INFO memory` 输出**——Redis 用 `/proc/self/statm` 和自己的内存跟踪（`zmalloc`）做交叉验证。如果两者差异过大→内存泄漏。

**容器 OOM 检测**——Kubernetes 的 kubelet 读取 `/proc/PID/oom_score` 判断哪些进程最可能被 OOM killer 选中。值越高越危险。

### A.4 /proc 实验

```bash
cat > proc_test.c << 'EOF'
#include <stdio.h>
#include <unistd.h>

int main() {
    printf("PID: %d\n", getpid());

    FILE *fp = fopen("/proc/self/status", "r");
    char line[256];
    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, "VmRSS", 5) == 0)  printf("▶ %s", line);
        if (strncmp(line, "Threads", 7) == 0) printf("▶ %s", line);
    }
    fclose(fp);

    fp = fopen("/proc/meminfo", "r");
    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, "MemAvailable", 12) == 0) { printf("▶ %s", line); break; }
    }
    fclose(fp);
    return 0;
}
EOF
gcc -Wall proc_test.c -o proc_test && ./proc_test
```

---

## Part B：文件锁——`flock` vs `fcntl`

### B.1 两种锁的根本区别

```c
// BSD lock (flock)——基于内核 struct file
#include <sys/file.h>
int fd = open("/tmp/lock", O_RDWR | O_CREAT, 0644);
flock(fd, LOCK_EX);   // 排他锁——阻塞
flock(fd, LOCK_UN);

// POSIX record lock (fcntl)——基于进程 PID
#include <fcntl.h>
struct flock lk = { .l_type = F_WRLCK, .l_whence = SEEK_SET, .l_start = 0, .l_len = 100 };
fcntl(fd, F_SETLKW, &lk);  // 排他写锁——阻塞
lk.l_type = F_UNLCK;
fcntl(fd, F_SETLK, &lk);   // 释放
```

| 维度 | `flock`（BSD） | `fcntl`（POSIX record lock） |
|---|---|---|
| **粒度** | 整个文件 | 文件内的字节范围 |
| **锁的所属** | 内核 `struct file`（open 文件描述） | 进程 PID |
| **fork 后** | 父子共享同一把锁（因为共享同一个 open file table entry） | 父子不共享——不同 PID 持有不同的锁 |
| **dup/dup2 后** | 共享（共享同一 struct file） | 共享（但只能释放一次） |
| **自动释放** | 最后一个引用 fd 关闭时释放 | 进程退出时释放，或显式 `F_UNLCK` |
| **NFS 支持** | 不完全（依赖 lockd 守护进程） | 不完全（同样依赖 lockd） |
| **标准** | BSD（Linux/BSD/macOS 都支持） | POSIX（所有 Unix） |
| **阻塞版本** | `flock(fd, LOCK_EX)` | `fcntl(fd, F_SETLKW, &lk)`（末尾的 W） |
| **非阻塞版本** | `flock(fd, LOCK_EX \| LOCK_NB)` | `fcntl(fd, F_SETLK, &lk)` |

### B.2 内核实现差异

**`flock` 在内核中**：锁挂在 `struct file_lock` 上，`fl_owner = (fl_owner_t)filp`（指向 `struct file *`）。因此 fork/dup 后父子共享同一个 file 引用的锁。内核在 `flock_lock_file_wait()` 中实现（挂入 `inode->i_flctx->flc_flock` 链表）。对同一个 fd 多次 `flock` 会**覆盖**之前的锁。

**`fcntl` 在内核中**：锁挂在 `struct file_lock` 上，`fl_owner = (fl_owner_t)current->files`（指向 `struct files_struct *`）。fork 后父子进程不共享 fcntl 锁。内核在 `posix_lock_inode()` 中实现（挂入 `inode->i_flctx->flc_posix` 链表）。重叠字节范围的 `fcntl` 会**合并**。

**修正**：两者的锁都挂在 `inode->i_flctx`（文件内部锁上下文）的不同链表上，而不是不同的内核对象上。区分在 `fl_owner` 字段——fork 语义不同，但内核存储位置在同一 inode 下。

**为什么这个差异重要**：`flock` 在父子进程间共享（因为 `fork` 复制了 `struct file` 引用）→适合父子进程协作的场景。`fcntl` 不跨进程共享——适合独立的进程间互斥。

### B.3 生产案例——SQLite 的 WAL 锁协议

SQLite 用 4 种文件锁实现多读单写的 WAL 并发控制：

```
读者路径：
  SHARED 锁（多读者可同时持有）
    → 读数据

写者路径：
  SHARED 锁（先当读者）→ 读数据
    → RESERVED 锁（声明"我要写"——阻止其他写者，但不阻止读者）
    → 写 WAL
    → PENDING 锁（等待所有读者释放 SHARED）
    → EXCLUSIVE 锁（独享——现在可以更新 WAL 索引）

升级过程：SHARED → RESERVED → PENDING → EXCLUSIVE
```

**`flock` 实现**：SQLite 默认用 `flock`（文件描述符锁），因为它在 fork/exec 时的共享语义更简单。SQLite 也支持 `fcntl` 记录锁（`--enable-pthread-locking`），在多线程环境下更可靠。

### B.4 生产案例——cron 防重复

```c
// 确保同一脚本不会同时运行两次
int fd = open("/var/run/myscript.lock", O_RDWR | O_CREAT, 0644);
if (flock(fd, LOCK_EX | LOCK_NB) == -1) {
    // 锁已被持有——另一个实例在运行
    fprintf(stderr, "Another instance is running, exiting\n");
    exit(0);
}
// 临界区——只有我持有锁
```

### B.5 `O_EXCL`——唯一真正原子的排他创建

```c
int fd = open("/tmp/lock.pid", O_CREAT | O_EXCL | O_WRONLY, 0644);
if (fd == -1 && errno == EEXIST) {
    // 文件已存在——另一个进程持有"锁"
    return -1;
}
// 成功——我是唯一持有者
// 清理：unlink("/tmp/lock.pid") + close(fd)
```

**为什么 `O_EXCL` 比其他锁更安全**：`F_GETLK→判断空闲→F_SETLK` 两步之间有 **TOCTOU**（Time-Of-Check To Time-Of-Use）窗口——在检查和加锁之间另一个进程可能抢走锁。`O_EXCL` 在内核中原子执行——不存在这个窗口。

**生产案例**：Docker 用 `O_EXCL` 创建 `/var/run/docker.pid` 确保只有一个 dockerd 进程运行。cron 守护进程、System V 信号量初始化文件也用同样的模式。

### B.6 强制锁（Mandatory Locking）

与 advisory lock（劝告锁，`flock/fcntl` 是约定的）不同，**强制锁**让内核在每次 `read()/write()` 时都检查锁——应用层不需要显式调用 `flock/fcntl` 就能被阻塞。

```bash
# 启用强制锁（几乎没人用了——性能灾难 + 死锁风险）
chmod +l file        # 或 mount -o mand
```

**面试常问**："Linux 有两种锁模式，劝告锁和强制锁，区别是什么？"——强制锁已经被废弃（POSIX 不再建议使用），但恰恰因此常出现在面试中作为区分度问题。

### B.7 死锁场景

```c
// 进程 A
fcntl(fd, F_SETLKW, &(struct flock){F_WRLCK, SEEK_SET, 0, 100});    // 锁 [0, 100)
fcntl(fd, F_SETLKW, &(struct flock){F_WRLCK, SEEK_SET, 100, 100});  // 还想锁 [100, 200)

// 进程 B
fcntl(fd, F_SETLKW, &(struct flock){F_WRLCK, SEEK_SET, 100, 100});  // 锁 [100, 200)
fcntl(fd, F_SETLKW, &(struct flock){F_WRLCK, SEEK_SET, 0, 100});    // 还想锁 [0, 100)

// 死锁！A 持有 [0,100) 等 [100,200)，B 持有 [100,200) 等 [0,100)
// Linux 内核检测到死锁→选择其中一个进程返回 EDEADLK
```

**检测**：`fcntl(F_GETLK)` 会报告**谁**持有冲突的锁（`lk.l_pid`）。`flock` 不支持这个查询。

---

## 面试题

### 面试题：flock vs fcntl 锁的实现差异

**面试官**：`flock` 和 `fcntl(F_SETLK)` 在内核中有什么区别？fork 后行为一样吗？

**回答**：内核归属不同——flock 的锁挂在 `struct file` 上（open 文件描述），fcntl 的锁挂在 `struct files_struct`（进程文件描述符表）上。fork 后——flock 锁父子进程共享（因为共享同一个 `struct file` 引用），fcntl 锁不共享（不同 `files_struct`）。这就是为什么 SQLite 用 flock 做多进程 WAL 锁——fork/exec 子进程自然继承父进程的锁状态，无需重新获取。

**追问 1（面试官）**：SQLite 的 WAL 锁协议 4 步升级为什么设计成这样？为什么不能直接从 SHARED 跳到 EXCLUSIVE？

**追问 1 回答**：多读单写的要求——SHARED 同时允许多个读者，RESERVED 声明"我要写"阻止其他写者，PENDING 等所有现有读者释放 SHARED（新读者看到 PENDING 就阻塞），EXCLUSIVE 最后获得独占。不能直接跳——从 SHARED 跳到 EXCLUSIVE 中间其他读者可能在读同一页→写者需要知道当前是否有读者。RESERVED→PENDING 的等待阶段就是"等旧读者退出+阻止新读者进来"。

**追问 2（面试官）**：`flock(LOCK_EX | LOCK_NB)` 防重复启动——为什么比 PID 文件更可靠？

**追问 2 回答**：PID 文件有无法避免的竞态——`open+read PID` → `kill 0, PID` → `write new PID`，在检查和写入之间另一进程可能启动。`flock` 是原子操作——要么获取锁要么失败。文件被意外删除不影响锁（锁在 inode 上不是路径）。进程崩溃→内核自动释放锁。`O_EXCL` 创建锁文件是另一原子方案——Docker 和 cron 都用的这个模式。

---

## 总结

| 概念 | 核心知识 |
|---|---|
| `/proc` | 虚拟文件系统——内核 `seq_file` 接口——每次 read 动态生成 |
| `/proc/PID/status` | VmRSS/Threads/上下文切换——生产监控的 C 代码入口 |
| `/proc/meminfo` | 系统内存全貌——所有监控脚本的基础 |
| `flock` vs `fcntl` | 内核归属不同——`struct file` vs `struct files_struct`——fork 语义不同 |
| SQLite WAL 锁 | 4 级锁升级——flock 实现——SHARED→RESERVED→PENDING→EXCLUSIVE |
| cron 防重复 | `flock(LOCK_EX \| LOCK_NB)` 非阻塞尝试——最简单有效的单实例守护 |
| `fcntl` 死锁 | 进程 A 持区段 X 等 Y，B 持 Y 等 X——内核检测后选一个返回 EDEADLK |

---

## 验收要点

1. `/proc` 文件和普通磁盘文件的本质区别是什么？为什么不消耗磁盘 I/O？
2. `/proc/PID/status` 的 `VmRSS`、`VmSize`、`Threads` 各代表什么？
3. 如何在 C 代码中读 `/proc/self/status` 而不通过 `popen("ps")`？为什么更快？
4. 为什么 Nginx 和 Redis 都从 `/proc` 读取自身信息而不依赖外部监控工具？
5. `flock` 和 `fcntl(F_SETLK)` 在内核中的归属差异是什么？fork 后有什么不同？
6. SQLite 的 WAL 写者锁升级路径是什么？（4 步：SHARED→RESERVED→PENDING→EXCLUSIVE）
7. 如何用 `flock` 实现 cron 脚本的防重复执行？
8. `fcntl` 记录锁如何检测死锁？死锁发生后哪个进程会失败？
