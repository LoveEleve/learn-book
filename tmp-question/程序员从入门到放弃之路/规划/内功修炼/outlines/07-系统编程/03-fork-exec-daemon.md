# fork、exec、wait 与 daemon — 进程如何复制、替换、回收和脱离终端

> Cluster B: 8 KPs | 依赖: 01-file-io-basics、02-mmap-epoll-inotify | 读者基线: PID、fd、地址空间、信号基础
> 读者处境: 01-02 篇讲了 fd、mmap、epoll；本篇回答进程生命周期：fork 后复制了什么、exec 后保留了什么、子进程怎样回收，以及 daemon 为什么要脱离终端
> 打开新视角: UNIX 进程创建是两步组合——**fork 复制进程上下文，exec 替换程序映像，wait 回收退出状态**；daemon 则是重新组织会话与资源继承

---

### 概念依赖链

```
01 fd + 02 mmap/epoll → 本篇: 进程生命周期
  ├─ §1 fork(COW/FD/信号继承)
  ├─ §2 exec(地址空间替换/继承边界)
  ├─ §3 wait/SIGCHLD(退出状态回收)
  ├─ §4 zombie/orphan(异常生命周期)
  └─ §5 daemon(session/process group/stdio)
先讲: 复制 → 替换 → 等待回收 → 僵尸/孤儿 → 守护化
后续依赖: 04-pthread(线程共享地址空间与同步)
```

### 叙事顺序

1. 问题引入——shell 启动一个命令时，为什么既要 fork 又要 exec？子进程退出后父进程还要做什么？
2. fork——COW、fd、信号和地址空间复制
3. exec——把当前进程替换成新程序
4. wait/SIGCHLD——回收退出状态
5. zombie/orphan——两个常见生命周期结果
6. daemon——会话、终端、工作目录和标准 fd
7. 收束——进程从复制到死亡

### 1. fork — 复制执行上下文，但物理页通常延迟复制

场景提示: `fork()` 返回两次，父子进程的变量为什么看起来一样，之后修改又互不影响？ [写作时展开]

关键设计: fork 创建子进程执行上下文，父子地址空间初始通过 COW 共享物理页；文件描述符继承的是对打开文件对象的引用：

```[pseudocode]
pid = fork()

父进程:
  返回 child_pid
  继续执行父分支

子进程:
  返回 0
  继续执行子分支

内存:
  父/子页表初始共享物理页
  → 写入共享页触发 COW fault
  → 分配私有页并复制内容

fd:
  父/子各有 fd 表项
  → 指向同一个 open file description
  → 可能共享文件 offset/status flags
```

Why: 为什么 fork 不直接复制全部物理内存？——**绝大多数 fork 后很快 exec，直接复制会浪费大量内存和时间**；COW 把复制成本推迟到真正写入的页。fork 后父子共享的是打开文件对象引用，不是简单“两个完全独立 fd”，所以 offset 和 close 生命周期要特别注意。 [man 2 fork: COW、fd 继承和 async-signal-safe 约束]

比喻锚点: fork 像复制一套共享只读图纸，父子先共用，谁要在某页上修改才复印自己的副本。 [写作时展开]

### 2. exec — 不创建新 PID，替换当前进程映像

场景提示: shell fork 出子进程后执行新命令，为什么新程序通常仍使用同一个子 PID？ [写作时展开]

关键设计: exec 系列调用用新程序的 ELF 映像替换当前进程地址空间，但进程身份和部分进程属性继续存在：

```[pseudocode]
execve(path, argv, envp)
  → 解析/加载新程序映像
  → 替换 text/data/heap/stack 等地址空间
  → 设置新的入口点和用户栈

通常保留:
  PID/PPID/进程组/会话
  open fd(除 FD_CLOEXEC)
  资源限制、工作目录、部分凭据/属性

被重置/替换:
  用户地址空间
  自定义信号处理器
  多线程映像(执行 exec 的线程继续, 其他线程消失)
```

Why: 为什么要把 fork 和 exec 分成两个接口？——**fork 负责创建执行上下文，exec 负责选择新程序映像**：shell 可以在 exec 前设置 fd 重定向、管道、环境、权限和工作目录。exec 并不是“启动一个全新进程”，而是让当前 PID 运行另一份程序。 [man 2 execve: close-on-exec、信号处理器和属性继承边界]

比喻锚点: fork 是复制一个空白工作间，exec 是把工作间里的整套设备和图纸换成新项目；楼层房间号仍然没变。 [写作时展开]

### 3. wait、waitpid 与 SIGCHLD — 父进程如何收回退出状态

场景提示: 子进程已经退出，为什么 `ps` 里还短暂留下它？父进程怎样拿到退出码？ [写作时展开]

关键设计: 子进程退出后保留少量退出状态，父进程通过 wait 系列“收尸”：

```[pseudocode]
wait(&status)
  → 等任意可回收子进程

waitpid(pid, &status, WNOHANG|WUNTRACED|WCONTINUED)
  → 等特定子进程/非阻塞查询/状态变化

解析:
  WIFEXITED / WEXITSTATUS
  WIFSIGNALED / WTERMSIG
  WIFSTOPPED / WIFCONTINUED

SIGCHLD:
  子进程状态变化通知父进程
  → handler 中只做安全操作
  → 或 signalfd + epoll 统一处理
  → 最终仍需 wait/waitpid 回收
```

Why: 为什么收到 SIGCHLD 就不等于子进程已经回收？——**信号是通知，退出状态回收仍需要 wait 系列；信号可能合并，handler 也不能执行任意复杂逻辑**。SA_NOCLDWAIT/SIGCHLD 忽略等策略会改变僵尸和等待语义，必须按目标平台文档确认。 [man 2 waitpid/man 7 signal: 状态解析、信号合并和异步上下文]

比喻锚点: 子进程退出像学生交卷，SIGCHLD 是教务处通知“卷子到了”，wait 才是父进程把成绩单领走并释放档案柜。 [写作时展开]

### 4. Zombie 与 orphan — 两种“父子生命周期错位”

场景提示: `ps` 显示 `Z` 状态的进程为什么 kill 不掉？父进程退出后孤儿又由谁负责？ [写作时展开]

关键设计: zombie 和 orphan 的问题不同：

```[pseudocode]
Zombie:
  子进程已退出
  → 父进程尚未 wait
  → 保留退出状态/少量进程表信息
  → 不能靠 kill 让它“再死一次”
  → 修复父进程 wait/waitpid 或退出

Orphan:
  父进程先退出, 子进程仍运行
  → 被系统指定的子收养者接管
  → 后续退出仍需要被回收
```

Why: 为什么孤儿不等于僵尸？——**orphan 描述的是父进程关系，zombie 描述的是子进程已退出但状态未回收**：孤儿仍可能继续运行，也可能之后成为需要回收的退出子进程；现代 Linux 的 subreaper/container init 可能成为实际收养者，不应简单只写“永远由 PID 1 领养”。 [man 2 prctl: subreaper 会影响孤儿重新归属]

比喻锚点: orphan 是孩子还在走路但监护人不在，zombie 是孩子已经交卷但档案还没归档；一个是监护关系，一个是回收状态。 [写作时展开]

### 5. daemon — 脱离控制终端并重新整理资源

场景提示: SSH 会话断开后，为什么普通前台程序可能收到 SIGHUP，而服务进程却能继续运行？ [写作时展开]

关键设计: 传统 daemon 化通常包含 fork、setsid、工作目录、umask、标准 fd 和信号/日志处理；现代服务也常由 systemd 等 supervisor 管理，不一定需要手写双 fork：

```[pseudocode]
传统流程:
  fork → 父退出
  → setsid() 创建新 session/process group, 脱离控制终端
  → 可再次 fork, 避免成为 session leader
  → chdir("/") 或明确工作目录
  → 设置 umask(按安全需求, 不要盲目 0)
  → 关闭/重定向 stdin/stdout/stderr
  → pidfile/日志/信号/权限降级

现代 supervisor:
  systemd/container runtime
  → 负责 stdout/stderr、重启、cgroup、工作目录和生命周期
```

Why: 为什么 double fork 不是所有 daemon 的必选步骤？——**它是传统脱离终端和避免重新获得控制终端的惯用模式，现代 supervisor 可用服务管理语义替代部分步骤**；`umask(0)`、关闭标准 fd 和 chdir 都要按服务安全/部署需求设置，不能机械复制。 [man 2 setsid/chdir/umask: session、工作目录和文件创建掩码语义]

比喻锚点: 传统 daemon 像离开前台剧场、换到独立办公室并重新整理电话线；systemd 则像物业统一管理办公室，不必每个租户自己断电关门。 [写作时展开]

### 6. 收束

进程生命周期：

```[pseudocode]
fork
  → 父子共享部分内核对象/暂时共享 COW 页
  → 子 exec 替换地址空间
  → 父 wait/waitpid 回收退出状态
  → 未回收 → zombie
  → 父先退出 → orphan 重新归属
  → daemon/supervisor 重新组织会话和资源
```

**Aha Moment**: "fork、exec、wait 不是三个孤立 API：**fork 复制执行上下文，exec 替换程序映像，wait 完成父子生命周期闭环**；fd、地址空间、信号和会话分别有自己的继承/重置规则。"
**回答读者三问**: ①fork 后内存是否复制=先 COW，写时复制；②exec 是否新建 PID=不是，当前进程映像被替换；③zombie 怎么处理=父进程 wait/waitpid 回收，kill 无法直接消除。

---

### 核心悬念

**"进程是内核调度单位，但多个线程如何共享地址空间、fd 和文件 offset？Pthreads 的 1:1 模型、互斥锁与死锁又怎样落到 Linux 内核？"**

→ 引出 04-pthread-threads — 线程创建、共享状态、同步与死锁。