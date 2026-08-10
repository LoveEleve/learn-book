# PCB(task_struct) + fork/clone + 上下文切换 + 系统调用 + 信号处理 + ELF

> Cluster C: 10 KPs | 依赖: 10-CFS 调度器 | 读者基线: 理解红黑树调度和进程挂起概念

---

### 1. task_struct — 内核视角的"进程"
  - `struct task_struct`: pid(进程 ID) / tgid(线程组 ID, getpid 返回) / state(TASK_RUNNING/TASK_INTERRUPTIBLE/TASK_UNINTERRUPTIBLE/TASK_STOPPED/EXIT_ZOMBIE) / stack(内核栈指针) / mm(地址空间) / files(打开文件表) / signal(信号处理队列) / sched_entity(CFS 调度实体) (include/linux/sched.h:732)
  - 进程状态转换: fork → TASK_RUNNING → 等资源 → TASK_INTERRUPTIBLE(可信号唤醒) / TASK_UNINTERRUPTIBLE(不响应信号, D 状态) → 条件满足 → TASK_RUNNING → exit → EXIT_ZOMBIE → 父 wait → 释放
  - 僵尸进程: 子进程 exit 但父未 wait → 资源已释放但 task_struct 还在(占 PID) → 看不到进程但 `ps aux | grep Z` → 父是 init 则 init 回收

### 2. fork/clone — 创建新进程/线程
  - fork 实现: `_do_fork → copy_process` → `copy_mm`(复制页表, 触发 COW 标记只读) → `copy_files`(文件描述符表) → `copy_fs`(文件系统信息) → `copy_sighand`(信号处理) → `alloc_pid`(分配 PID) (kernel/fork.c:2329)
  - clone 细粒度: CLONE_VM(共享地址空间) / CLONE_FS(共享文件系统) / CLONE_FILES(共享文件表) / CLONE_SIGHAND(共享信号) / CLONE_THREAD(创建线程, 同 tgid) — 线程本质是 clone 共享了大部分资源 (kernel/fork.c:2672)
  - vfork: 不拷贝页表 → 子进程在父地址空间运行 → 阻塞父进程 → exec 后父继续 → 比 fork 轻量(历史优化)
  - 纤程/协程(用户态): 内核线程=clone+CLONE_VM / 用户态协程=`makecontext`/`swapcontext` 切换用户栈, 无系统调用开销 → ucontext 系列 API → 现代 C++20 coroutine/Go goroutine 均基于类似原理 (no kernel source, userspace mechanism)

### 3. 上下文切换 — 从进程 A 到进程 B
  - `schedule → __schedule → context_switch` → `switch_mm`(切换页表 → 写 CR3 → TLB 刷新) → `switch_to`(保存寄存器 rsp/rbp/rip/通用 → 切换内核栈 → 恢复新进程寄存器) → `__switch_to_asm`(纯汇编) (kernel/sched/core.c:4856)
  - 切换开销: ~1-3 微秒(保存+恢复寄存器) + TLB 刷新(额外) + 缓存冷启动(第一个 ~100 微秒缓存 miss)
  - 切换时机: 主动(进程调用 sleep/yield/wait) / 被动(时间片用完 → tick → scheduler_tick → set_tsk_need_resched → 返回用户态时 schedule)

### 4. 系统调用 — 用户态到内核态的桥梁
  - 用户态 → `syscall` 指令(synthetic)或 `sysenter` → MSR 寄存器存入口地址 → `entry_SYSCALL_64 → do_syscall_64` → `sys_call_table[rax]`(系统调用号表) → `sys_read/sys_write/sys_futex` → 恢复 → `sysret` 返回 (arch/x86/entry/entry_64.S:118)
  - 参数传递: rdi(第 1 参数) / rsi / rdx / rcx / r8 / r9 → 返回值 rax → System V AMD64 ABI
  - strace 追踪: `strace -p PID` 打印所有系统调用 → `strace -c`(统计耗时) → `strace -e trace=file,network`(过滤类型)

### 5. 信号处理 — 异步通知机制
  - 发送: `kill/tkill/tgkill` → `send_signal → __send_signal` → 目标 `task_struct->pending` 队列 → `signal_wake_up` 设置 TIF_SIGPENDING (kernel/signal.c:1112)
  - 处理: 返回用户态/内核态前 → `do_signal → handle_signal → setup_rt_frame`(设置用户态栈) → 用户态信号处理函数 → `sigreturn`(恢复上下文) (arch/x86/kernel/signal.c:641)
  - 信号分类: 不可靠信号 1-31(可能丢失) / 可靠信号 34-64(排队, `rt_sigqueueinfo`) → SIGKILL(9, 必杀) / SIGTERM(15, 可捕获) / SIGSTOP(19) / SIGCONT(18)

### 6. ELF 格式 — 可执行文件的内部结构
  - ELF 头 → Program Header(段信息, 加载用) → Section Header(节信息, 链接用) → `.text`(代码段) / `.data`(初始化数据) / `.bss`(零初始化) / `.rodata`(只读) (include/uapi/linux/elf.h:36)
  - 动态链接: PT_INTERP(指定 ld.so 路径) → `.plt`(过程链接表) / `.got`(全局偏移表) → 延迟绑定(第一次调用时才解析符号) (fs/binfmt_elf.c:921)

### 7. 收束
  - task_struct 是进程的全息图 — 每个子系统都在这 2000+ 行 struct 中有字段
  - fork=复制 + COW / clone=共享按位掩码 / exec=替换地址空间
  - 上下文切换 = 页表切换(switch_mm) + 栈切换(switch_to) = ~1-3 微秒

---

### 核心悬念
**"多进程在 epoll 上用 SO_REUSEPORT 同时 accept — 一个连接来了唤醒所有进程然后只有一个人 accept 成功，其他人白醒了？惊群怎么解决？"**

→ 引出 12-惊群问题 + SO_REUSEPORT + EPOLLEXCLUSIVE + Nginx 案例 + epoll 三轮
