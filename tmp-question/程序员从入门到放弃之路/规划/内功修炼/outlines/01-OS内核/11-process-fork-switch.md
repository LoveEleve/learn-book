# PCB(task_struct) + fork/clone + 上下文切换 + 系统调用 + 信号处理 + ELF

> Cluster C: 10 KPs | 依赖: 10-CFS 调度器 | 读者基线: 理解红黑树调度和进程挂起概念
> 读者处境: 已读完 10 篇，知道调度器选"下一个进程"；本篇回答"被选中的进程是什么结构？进程怎么诞生/切换/加载？"
> 打开新视角: task_struct 的全息图、fork 快的原因（COW 复用）、上下文切换的 1-3µs 构成、系统调用/信号/ELF 的用户态内核态边界

---

### 概念依赖链

```
10-CFS(调度器选进程) → 本篇: 进程的本体
  ├─ §1 task_struct(进程的结构 — 依赖 10 的 sched_entity)
  │    ├─ §2 fork/clone(进程的诞生 — 依赖 03 COW)
  │    │    └─ §3 上下文切换(进程的交接 — 依赖 10 调度 + §1 task_struct)
  │    ├─ §4 系统调用(用户态↔内核态入口 — 依赖 09 陷阱门)
  │    ├─ §5 信号处理(异步通知 — 依赖 §1 的 pending 队列)
  │    └─ §6 ELF(程序的载体 — 依赖 §2 exec)
先讲: 本体(结构) → 诞生(fork) → 交接(切换) → 入口(系统调用) → 通知(信号) → 载体(ELF)
后续依赖: 12-惊群(多进程 epoll)、16-容器(进程隔离)
```

### 叙事顺序

1. 问题引入——`ps` 里的每个进程，内核眼里长什么样？（**Aha: 进程不是一个"东西"，是一堆结构的集合——task_struct 是入口**）
   - 过渡: 进程怎么诞生？——fork
2. fork/clone——复制 vs 共享；COW；线程=clone 共享
   - 过渡: 诞生后怎么"换人跑"？——上下文切换
3. 上下文切换——switch_mm + switch_to；1-3µs 构成；主动/被动切换
   - 过渡: 用户代码怎么进内核？——系统调用
4. 系统调用——syscall 指令/入口/参数传递/返回值；strace
   - 过渡: 内核怎么"主动"通知用户进程？——信号
5. 信号处理——发送队列/TIF_SIGPENDING/处理路径/分类
   - 过渡: 程序本身长什么样？——ELF
6. ELF——头/段/节；动态链接 PLT/GOT 延迟绑定
   - 过渡: 全貌已齐——回到"进程的一生"
7. 收束——进程从诞生到加载的完整旅程

### 1. task_struct — 内核视角的"进程"

场景提示: `ps aux` 一行进程——内核里是一个巨大的结构体（不同版本上千行），每个子系统都有字段。 [写作时展开]

关键设计: `struct task_struct`——内核的"进程身份证" (include/linux/sched.h)：

| 字段 | 含义 |
|------|------|
| pid / tgid | 进程 ID / 线程组 ID（getpid 返回 tgid） |
| state | TASK_RUNNING/INTERRUPTIBLE/UNINTERRUPTIBLE/STOPPED/EXIT_ZOMBIE |
| stack | 内核栈指针（8/16KB，含 thread_info） |
| mm | 地址空间（03 篇的 mm_struct） |
| files | 打开文件表 |
| signal | 信号处理队列（§5） |
| sched_entity | CFS 调度实体（10 篇） |

进程状态转换: fork → TASK_RUNNING → 等资源 → TASK_INTERRUPTIBLE（可信号唤醒）/TASK_UNINTERRUPTIBLE（D 状态，不响应信号）→ 条件满足 → RUNNING → exit → EXIT_ZOMBIE → 父 wait → 释放。

僵尸进程: 子进程 exit 但父未 wait → 资源已释放但 task_struct 还在（占 PID）→ `ps aux | grep Z`——父是 init 则由 init 回收。

Why: 为什么 task_struct 如此巨大？——进程是所有子系统的"交汇点"：调度（sched_entity）、内存（mm）、文件（files）、信号（signal）都要挂在进程上。task_struct 不是"进程的定义"，而是**所有子系统的挂载点**——每个子系统往这里加自己的字段。

比喻锚点: task_struct=员工档案袋——身份证（pid）、排班表（sched_entity）、工位图（mm）、门禁卡（files）、来电记录（signal）全装在一个袋子里；每个部门（子系统）往袋里加自己的材料。 [写作时展开]

### 2. fork/clone — 创建新进程/线程

场景提示: fork 一个 10GB 进程毫秒级返回（03 篇 COW）——除了页表，还复制了什么？ [写作时展开]

关键设计: fork = `_do_fork → copy_process` 逐个复制：

```[pseudocode]
copy_mm(复制页表, COW 标记只读) → copy_files(文件描述符表)
→ copy_fs(文件系统信息) → copy_sighand(信号处理) → alloc_pid(分配 PID)
```

clone 细粒度（线程的本质）: CLONE_VM（共享地址空间）/CLONE_FS/CLONE_FILES/CLONE_SIGHAND/CLONE_THREAD（同 tgid）——**线程 = clone 共享大部分资源的进程**。

- **vfork**: 不拷贝页表，子进程在父地址空间运行，阻塞父进程直到 exec——历史优化（如今 COW 已使 fork 足够快）
- **协程（用户态）**: 内核线程=clone+CLONE_VM；用户态协程=`makecontext`/`swapcontext` 切换用户栈（零系统调用）——C++20 coroutine/Go goroutine 同源原理

Why: 为什么 fork 快而 exec 慢？——fork 只复制"进程描述"（页表+文件表，COW 让物理页零复制）；exec 要**重建地址空间**（清空旧映射→加载 ELF→初始化栈/堆）。fork 是"复印身份证"，exec 是"换一个人住进来"。

比喻锚点: fork=复印身份证（秒级）——正本副本长得一样，谁改内容才各自重印（COW）；exec=换住户（慢）——先清空房间（旧映射）再搬进新家具（ELF 加载）。 [写作时展开]

### 3. 上下文切换 — 从进程 A 到进程 B

场景提示: 两个进程轮流跑——换人的瞬间 CPU 做了什么？为什么切换要 1-3µs？ [写作时展开]

关键设计: `schedule → __schedule → context_switch` 两件大事：

```[pseudocode]
switch_mm: 切换页表 → 写 CR3 → TLB 刷新
switch_to: 保存寄存器(rsp/rbp/rip/通用) → 切换内核栈 → 恢复新进程寄存器
→ __switch_to_asm(纯汇编, 零 C 代码)
```

- **切换开销 ~1-3µs**: 寄存器保存/恢复（数十 cycles）+ TLB 刷新（数百 miss）+ 缓存冷启动（新进程第一个 ~100µs 大量 cache miss）——**大头是缓存/TLB 而非寄存器**
- **切换时机**: 主动（sleep/yield/wait）/ 被动（时间片用完 → tick → `scheduler_tick` → `set_tsk_need_resched` → 返回用户态时 `schedule`）

Why: 为什么"返回用户态时才真正切换"？——内核态切换立即执行会打断当前系统调用（复杂化）；`set_tsk_need_resched` 只**标记**，等当前系统调用返回用户态的边界上才 `schedule`——让切换发生在"干净的边界"（09 篇陷阱门返回处）。

比喻锚点: 上下文切换=医院交接班——换班（切页表）只在交班时刻（用户态边界）进行，查房到一半（系统调用中）不换人；标记（set_tsk_need_resched）像"下班铃响了"，交班点才真正换。 [写作时展开]

### 4. 系统调用 — 用户态到内核态的桥梁

场景提示: `read()` 一个库函数——中间发生了什么？为什么不能直接跳进内核？ [写作时展开]

关键设计: x86-64 系统调用路径 (arch/x86/entry/entry_64.S)：

```[pseudocode]
用户态 syscall 指令 → MSR 寄存器存入口地址 → entry_SYSCALL_64 → do_syscall_64
→ sys_call_table[rax](系统调用号表) → sys_read/sys_write/sys_futex → 恢复 → sysret 返回
```

- **参数传递**: rdi/rsi/rdx/rcx/r8/r9（前 6 参数）→ 返回值 rax（System V AMD64 ABI）
- **strace**: `strace -p PID`（跟踪所有系统调用）/ `-c`（统计耗时）/ `-e trace=file,network`（过滤类型）——"进程卡在哪个系统调用"第一排查工具

Why: 为什么不能直接"跳进"内核函数？——特权级隔离（09 篇 User/Supervisor）：用户态不能访问内核地址（PTE 权限），必须经**受控入口**（syscall 指令 + 系统调用号查表）——内核只暴露表里的函数。参数经寄存器传递（零内存访问，性能考量）；涉及用户内存的读写，内核用 `copy_from_user/copy_to_user` 校验后再访问（安全考量）。 [x86: syscall/sysret 指令切换特权级, MSR 存入口地址]

### 5. 信号处理 — 异步通知机制

场景提示: `kill -9 PID`——内核怎么让进程"知道"自己该死了？ [写作时展开]

关键设计:

**发送**: `kill/tkill/tgkill` → `send_signal → __send_signal` → 目标 `task_struct->pending` 队列 → `signal_wake_up` 设置 **TIF_SIGPENDING** 标志。

**处理**: 返回用户态前检查 TIF_SIGPENDING → `do_signal → handle_signal → setup_rt_frame`（在用户栈搭信号处理帧）→ 用户态处理函数 → `sigreturn` 恢复上下文。

**分类**: 不可靠信号 1-31（可能丢失，不排队）/ 可靠信号 34-64（排队，rt_sigqueueinfo）；SIGKILL(9) 必杀 / SIGTERM(15) 可捕获 / SIGSTOP(19) / SIGCONT(18)。

Why: 为什么信号是"异步"的？——发送方不等待接收方处理（kill 立即返回）；接收方在**返回用户态的边界**检查标志并处理——与上下文切换（§3）同一个"边界处理"模式。这保证了信号处理不打断内核态关键路径。

比喻锚点: 信号=电话留言——打过来（kill）就挂断不等接听（异步）；对方回办公室（返回用户态边界）看到留言灯（TIF_SIGPENDING）才回电（处理）。 [写作时展开]

### 6. ELF 格式 — 可执行文件的内部结构

场景提示: `./a.out`——内核怎么把磁盘上的文件变成可执行进程？ [写作时展开]

关键设计: ELF 三层结构 (include/uapi/linux/elf.h)：

```[pseudocode]
ELF 头 → Program Header(段信息, 加载用) → Section Header(节信息, 链接用)
.text(代码) / .data(初始化数据) / .bss(零初始化) / .rodata(只读)
```

动态链接: PT_INTERP（指定 ld.so 路径）→ **PLT/GOT 延迟绑定**——第一次调用函数时才解析符号（省启动时间）；`ldd` 查看依赖、`readelf -h` 查看头。

Why: 为什么分 Program Header 和 Section Header？——**两个消费者**：内核加载只需 Program Header（哪些段映射到哪）；链接器需要 Section Header（符号/重定位）。同一文件两种视角——运行视角（段）vs 构建视角（节）。

比喻锚点: ELF=集装箱货轮——Program Header 是装卸清单（哪个箱子放哪个舱位，内核照着卸货）；Section Header 是货物明细（箱子里具体什么货，链接器照着组装）。 [写作时展开]

### 7. 收束

进程的一生：
- 诞生 = fork（复制描述+COW）→ 换芯 = exec（重建地址空间）
- 交接 = 上下文切换（页表+寄存器，1-3µs）
- 入口 = 系统调用（受控查表）→ 通知 = 信号（边界处理）
- 载体 = ELF（段加载/节链接）

**Aha Moment**: "进程不是一个'东西'，是一堆结构的集合——task_struct 是挂载点，fork 只复印身份证（COW 不碰数据），exec 才换人住进来。而系统调用/信号/切换都选择在'用户态边界'处理——内核态路径保持干净，这是 Linux 的一致设计。"
**回答读者三问**: ①fork 为什么快=COW 只复制描述；②切换为什么 1-3µs=寄存器+TLB+缓存冷启动；③kill -9 怎么生效=TIF_SIGPENDING 边界检查。

---

### 核心悬念

**"多进程在 epoll 上用 SO_REUSEPORT 同时 accept — 一个连接来了唤醒所有进程然后只有一个人 accept 成功，其他人白醒了？惊群怎么解决？"**

→ 引出 12-惊群问题 + SO_REUSEPORT + EPOLLEXCLUSIVE + Nginx 案例——进程有了，多进程协作的问题来了。
