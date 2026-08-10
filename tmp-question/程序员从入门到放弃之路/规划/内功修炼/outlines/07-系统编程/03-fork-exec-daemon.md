# fork + exec + daemon — 进程创建全链路 + 僵尸/孤儿 + 守护进程

> Cluster B: 8 KPs | 依赖: A (01 fd + 02 mmap/epoll) | 读者基线: 会用 `ps aux` 和 `kill`，知道 PID 概念

---

### 1. fork — 写时拷贝创建进程
  - `pid_t pid = fork()`: 子进程返回 0，父进程返回子进程 PID → fork 后父子共享 fd 表(同一 fd 指向同一打开文件) (man 2 fork)
  - COW(写时拷贝): fork 不拷贝全部物理页→父子共享页表 entry 标记 RO → 任一方写时触发缺页中断拷贝页 (B1 Ch5 §1-2)
  - fd 继承: fork 后子进程继承全部 fd→fd 引用计数+1→必须 double-close → `O_CLOEXEC` 标志 fork+exec 时自动关 (man 2 open)
  - 信号继承: 子进程继承父进程的信号处理器(handler 地址共享)——但 pending signals 不继承等待队列 (B1 Ch5 §1)
  - 内存语义: `int x = 1; if (fork() == 0) { x = 2; printf("child %d\n", x); } else { printf("parent %d\n", x); }` — parent 仍打印 1

### 2. exec — 替换地址空间
  - `execve(path, argv, envp)` → 六变体: execve(唯一系统调用)/execvp(从 PATH 搜索)/execle(自定义 envp)/execl/execv/execlp (man 3 exec)
  - exec 后保留: PID/PGID/SID(进程标识) + 已连接信号 + `RLIMIT_*`(资源限制) + 文件锁(`fcntl`) + O_CLOEXEC 除外 fd
  - exec 后丢弃: 旧地址空间(text/data/stack) + 旧信号处理器(handler 函数地址无效) + 线程(exec 后只剩一条) (B1 Ch5 §3)
  - 典型用法: `fork()+exec()` — 创建子进程后立即 exec 替换为新程序——nginx、PostgreSQL 都用此模式

### 3. wait/waitpid — 回收子进程
  - `wait(&status)`: 阻塞等任意子进程结束 → `WIFEXITED/WEXITSTATUS/WIFSIGNALED/WTERMSIG/WCOREDUMP` 解析状态 (man 2 wait)
  - `waitpid(pid, &status, WNOHANG|WUNTRACED|WCONTINUED)`: 等特定子进程→WNOHANG 非阻塞→与 epoll 循环集成 (man 2 waitpid)
  - 关键坑: 父进程没有 wait → 子进程变僵尸(进程表项占用但不运行, `ps` 显示 `Z` 状态)——**僵尸不能 kill，只能 wait 回收** (B1 Ch5 §4)
  - `SIGCHLD` 信号: 子进程终止时发→SA_NOCLDWAIT 让内核自动回收→或 sigaction+signalfd 在 epoll 循环中处理 (man 7 signal)

### 4. 僵尸进程与孤儿进程
  - 僵尸(Zombie): 子进程终止但父进程未 wait→进程表条目不释放→占用 PID 号和部分内核资源 (B1 Ch5 §4)
  - 孤儿(Orphan): 父进程先于子进程终止→init(pid=1)领养→init 周期性 wait 回收→**孤儿不会变僵尸** (B1 Ch5 §4)
  - 排查: `ps aux | grep 'Z'` → `cat /proc/PID/status | grep State` → 修复: 父进程补 wait/waitpid (B1 Ch5 §4)

### 5. 守护进程 — 双 fork + SID + 环境清理
  - 步骤: `fork()`(父退出)→ `setsid()`(新会话+脱离终端)→ `fork()`(新父退出, 确保不是会话首进程)→ `chdir("/")`(防 unmount)→ `umask(0)`(回复默认)→ `close(0,1,2)` + 重定向到 `/dev/null` (B1 Ch5 §7)
  - setsid: 创建新会话(session)→新进程组→断开控制终端→不再接收 SIGHUP(终端断开) + SIGINT(Ctrl+C) (man 2 setsid)
  - 进程组/会话: `getpgrp()`/`getpgid()`/`getsid()` — session leader(会话首进程)→控制终端→SIGHUP 分发 (man 2 getpgrp)

### 6. 收束
  - fork+exec 是 UNIX 进程创建的"分手模式"——创建新进程只是复制自己(fork)，新程序用 exec 替代自己
  - 僵尸≠泄漏——本质是"子进程死了但还没人问成绩"——父进程不及时 wait 就卡在进程表里
  - 守护进程的双 fork 核心是脱离终端——不依赖任何 shell 会话，不因 SSH 断开而收到 SIGHUP

---

### 核心悬念
**"进程是内核调度的最小单位，但我们真正在用线程——线程比进程轻在哪？Pthreads 的 1:1 模型在 Linux 内核里到底什么样？"**

→ 引出 04-pthread — 线程创建、同步、死锁与线程安全
