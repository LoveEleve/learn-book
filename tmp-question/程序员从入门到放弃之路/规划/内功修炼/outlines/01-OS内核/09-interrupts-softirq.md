# 中断处理 — IDT/APIC + 上半部/下半部 + softirq/tasklet/workqueue + 时间管理

> Cluster C: 9 KPs | 依赖: 05-MESI + 06-原子操作 + 07-锁家族 | 读者基线: 理解多核并发和屏障概念

---

### 1. 中断全链路 — IRQ 从硬件到内核
  - 设备触发 IRQ → PIC/APIC 路由到目标 CPU → 保存上下文(压栈 rip/rflags) → IDT 表查找 → 中断门 → `common_interrupt` → `do_IRQ` → `handle_irq`(调用注册的 ISR) → `irq_exit` → 触发软中断 (arch/x86/kernel/irq.c:204)
  - IDT(中断描述符表): 256 项 → 前 32 个为 CPU 异常(除 0/NMI/page fault) → vector 0x80 系统调用 → 设备中断 vector 32-255 (arch/x86/include/asm/desc_defs.h:18)
  - 中断门 vs 陷阱门: 中断门自动关 IF(禁止嵌套中断) → 陷阱门不关 IF(用于系统调用, 允许中断) (arch/x86/kernel/idt.c:90)

### 2. 上半部与下半部 — 紧急处理 + 延迟处理
  - 上半部 ISR: 关中断, 极短(通常 <100 微秒) → 只做最必要的事(确认中断源, 禁用设备中断, 调度下半部) → 不能睡眠 (kernel/irq/handle.c:175)
  - 下半部三种机制:
    - 软中断(softirq): 静态定义(HI_SOFTIRQ/TIMER_SOFTIRQ/NET_TX_SOFTIRQ/NET_RX_SOFTIRQ) → `open_softirq` 注册 → `do_softirq` 执行 → 最多 4 个 CPU 同时执行 → 不能睡眠 → 网络接收用 NET_RX (kernel/softirq.c:514)
    - tasklet: 基于 TASKLET_SOFTIRQ → 同一 tasklet 不能同时多 CPU 执行(提供天然序列化) → `tasklet_schedule` 调度 → 比软中断简单(不用考虑多核并发) (kernel/softirq.c:828)
    - workqueue: 内核线程池 → `schedule_work` 加入工作队列 → events 线程取出执行 → cmwq(并发管理工作队列) → `alloc_ordered_workqueue`(保序) → 可睡眠 → 适合耗时操作 (kernel/workqueue.c:1243)

### 3. 时间管理 — jiffies + hrtimer + tickless
  - jiffies: 时钟中断次数(jiffies++ 每次 tick) → HZ=250(现代内核, 4ms 间隔) → `jiffies_to_msecs` 转换 (include/linux/jiffies.h:71)
  - hrtimer 高精度定时器: 纳秒级 → 红黑树按 expires 排序 → `hrtimer_start` 插入 → `hrtimer_run_queues` 检查超时 → `hrtimer_cancel` 删除 (kernel/time/hrtimer.c:1125)
  - tickless(Dynamic Tick): idle 时不产生时钟中断(节能) → `tick_nohz_idle_enter` → `clockevent_device` 编程下一次到期时间 → 有任务时恢复 tick (kernel/time/tick-sched.c:1398)
  - `ktime_get()` 获取当前时间 → `schedule_timeout`(睡眠指定时间)

### 4. 收束
  - 中断全链路 = IDT 查表 + 上半部 ISR(关中断, 极短) + 下半部 softirq(开中断, 可并发)
  - softirq 最快但最危险(无序列化), tasklet 折中(单核序列化), workqueue 最重但最安全(可睡眠)
  - jiffies(粗粒度) + hrtimer(精粒度) + tickless(节能) = 内核时间管理三层

---

### 核心悬念
**"中断打断进程 → 进程被挂起 → 那内核怎么决定接下来该运行哪个进程？CFS 调度器怎么用红黑树做公平调度？"**

→ 引出 10-CFS 调度器(vruntime/红黑树/weight) + 负载均衡 + EEVDF + 实时调度
