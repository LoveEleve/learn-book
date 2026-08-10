# OS 内核 — 知识大纲

> 6本书 84 KPs → 5集群 5阶段 | 规划全文见 `01-OS内核.md` (622行)
> N=6 | P1=≥4书 | P2=2-3书 | P3=1书 | 深度: 8.5-9/10 | 语言: C/POSIX
> 🔴=含struct定义+call flow+可选内核源码 | 🟡=机制+行为解释+为什么这样设计 | 🟢=1-2句

---

## 阶段1: 内存管理 — OS知识的物理地基 (A集群, 零依赖)

### 1.1 物理内存管理 — Buddy与SLAB (🔴)
- **Buddy Allocator(伙伴系统)**: 2^n个连续页为单位分配→11个order链表(free_area[0..10])→alloc_pages分裂→__free_pages合并→抗外部碎片 (B1/Ch5, B3, B4, B5)
  - 关键字段: `struct free_area { struct list_head free_list[MIGRATE_TYPES]; unsigned long nr_free; }` — 每个order一个空闲链表, MIGRATE_TYPES区分可移动/不可移动/可回收
  - 分配流程: alloc_pages→__alloc_pages_nodemask→get_page_from_freelist→__rmqueue→找到合适order→分裂大块→返回小块
  - 迁移类型: MIGRATE_UNMOVABLE(内核核心)/MIGRATE_RECLAIMABLE(可回收)/MIGRATE_MOVABLE(用户页)—减少不可移动页导致的碎片化
- **SLAB/SLUB分配器**: 内核对象缓存—task_struct/mm_struct/inode等高频分配的小对象→预创建slab→per-CPU缓存(array_cache)→SLUB是SLAB简化版去掉队列和着色 (B1/Ch5, B4, B5)
  - SLAB: kmem_cache_create→kmem_cache_alloc→每个cache有slab三链表(full/partial/free)→着色(coloring)减少缓存行冲突
  - SLUB: 取消着色→per-CPU partial list→合并NUMA感知→简化调试→Linux 2.6.23+默认
  - 关键参数: `/proc/slabinfo` 查看所有缓存→`slabtop` 实时排序→`kmemleak` 检测未释放
- **物理内存模型**: UMA(均匀访问—SMP时代)→NUMA(非均匀:本地内存快/远程内存慢,node→zone→page三级) (B1/Ch5, B2/B3/B5)
  - Zone分类: ZONE_DMA(低16MB,ISA设备)/ZONE_DMA32(4GB以下)/ZONE_NORMAL(直接映射)/ZONE_HIGHMEM(32位内核>896MB,64位无)—32位历史遗留但概念重要
- **memblock**: 启动早期分配器→伙伴系统初始化前的过渡→memblock_reserve保留→memblock_free→setup_arch完成后退役→`/sys/kernel/debug/memblock` 可查看 (B4)

### 1.2 虚拟内存 — 分页与页表 (🔴)
- **分页机制**: 4KB标准页→虚拟地址拆分为PGD/PUD/PMD/PTE四级索引→逐级walk→CR3寄存器存PGD基址→每个进程独立页表→内核空间共享(0xFFFF800000000000+) (B1/Ch4, B3/B4/B5)
  - 五级页表(Linux 4.14+): PGD→P4D→PUD→PMD→PTE—57位虚拟地址→Intel 5-level paging (La57)—冰湖+
  - 页表遍历: `mm→pgd→pgd_offset→p4d_offset→pud_offset→pmd_offset→pte_offset_map→pte_page`—每一步+9bit偏移
  - 关键字段: `struct page` — flags/_refcount/_mapcount/mapping/index/private/lru—每个struct page 64字节→1GB物理内存需16MB struct page
- **TLB(Translation Lookaside Buffer)**: 页表硬件缓存→通常64-1024项→全相联→命中<1 cycle/缺失需4次内存访问→ASID(Address Space ID)标记进程→减少上下文切换时全刷新→invlpg刷新单条→CR3写刷新全部 (B1/Ch4, B2/B3/B4/B5)
  - TLB shootdown(跨核刷新): 修改页表→IPI中断其他CPU→其他CPU invlpg→mm_cpumask记录哪些核在用此mm
- **页表项(PTE)**: 64位→Present(bit0)→RW(bit1)→User/Supervisor(bit2)→PWT/PCD(缓存策略)→Accessed(bit5)→Dirty(bit6)→PAT→NX(bit63禁止执行)→物理页帧号(bit12-51) (B4)
- **大页(Huge Page)**: 2MB(PMD级别)→1GB(PUD级别)→减少TLB miss→hugetlbfs→mmap MAP_HUGETLB→THP透明大页(内核自动尝试分配)→khugepaged扫描→MongoDB/Redis建议关(分配延迟+内存碎片) (B1/Ch4, B4)
  - THP: `/sys/kernel/mm/transparent_hugepage/enabled`→always/madvise/never→`defrag`控制紧凑
  - 关键参数: `hugepagesz=2M hugepages=1024`→`/proc/meminfo HugePages_Total`
- **内核页表 vs 进程页表**: init_mm(内核页表模板)→进程fork时拷贝→内核地址映射在pgd[511]→0xffff888000000000(物理内存直接映射区)→vmemmap(struct page数组)→vmalloc区(非连续) (B1/Ch4, B3)

### 1.3 mmap、缺页异常与 VMA (🔴)
- **mmap(内存映射)**: 文件/匿名→分配虚拟地址空间→不立即分配物理页→访问时缺页异常→page cache关联→flags(MAP_SHARED/MAP_PRIVATE/MAP_ANONYMOUS/MAP_FIXED) (B1/Ch11, B2/B4/B5)
  - mmap类型: File-backed(页缓存)/Anonymous(匿名,demand-zero)/Shared(多进程共享)/Private(COW写时拷贝)
  - brk vs mmap: brk扩展堆→sbrk系统调用→连续增长→mmap任意地址→malloc小用brk大用mmap(>128KB默认)
- **缺页异常(Page Fault)**: 访问虚拟地址→MMU查TLB未命中→查页表→Present=0→触发#PF异常→do_page_fault→handle_mm_fault→三种情况 (B1/Ch4, B4)
  - Minor Fault: 页已分配但页表未建立→只需建立页表映射→最快
  - Major Fault: 页不在物理内存→需要磁盘IO(file-backed)/swap(匿名页)→最慢
  - Invalid Fault: 地址不在VMA内→SIGSEGV段错误→进程终止
- **写时拷贝(COW)**: fork子进程→复制页表→父子页表都标记只读→写时触发#PF→do_wp_page→分配新物理页→复制内容→更新页表为可写→fork优化核心 (B1/Ch12, B4)
  - COW检测: PTE中_RW清零但VMA标记可写→#PF→判断vm_flags&VM_WRITE→COW处理
  - 关键参数: `vm.overcommit_memory`(0=启发式/1=总是/2=严格)→`vm.overcommit_ratio`
- **VMA(虚拟内存区域)**: vm_area_struct→vm_start/vm_end(区间)/vm_flags(权限)/vm_file(映射文件)/vm_ops(操作集)→进程地址空间由VMA链表+红黑树管理→`/proc/PID/maps`可视化 (B1/Ch4, B3)
  - vm_flags: VM_READ(0x1)/VM_WRITE(0x2)/VM_EXEC(0x4)/VM_SHARED(0x8)/VM_MAYSHARE/VM_GROWSDOWN(栈)/VM_GROWSUP/VM_DONTEXPAND
- **mm_struct**: 进程地址空间描述符→mmap(VMA链表头)/mm_rb(红黑树根)/pgd(页表基址)/mm_users(引用计数)/start_code/end_code/start_data/end_data/start_brk/brk/start_stack/arg_start/arg_end (B3/B4)

### 1.4 页缓存与内存回收闭环 (🔴)
- **页缓存(Page Cache)**: 文件IO→读先查页缓存→未命中→读磁盘→加入address_space→radix tree(4.11以前)/xarray(4.20+)索引→标记脏页(dirty)→writeback回写 (B1/Ch11, B3/B4/B5)
  - address_space: 每个inode一个→host(inode)/i_pages(xarray)/a_ops(address_space_operations)→writepage/readpage/direct_IO
  - folio(Linux 5.16+): 代替单个page作为页缓存基本单位→folio可以是compound page→减少page→folio转换→writepages批量
  - 脏页回写: pdflush(旧)→flusher线程(per-backing-device)→dirty_background_ratio(10%)→dirty_ratio(20%)→超过开始阻塞写
- **内存回收(Reclaim)**: LRU链表(active+inactive匿名/active+inactive文件四链表)→kswapd后台回收→水位线(high/low/min)→低于low唤醒kswapd→低于min直接回收(direct reclaim)→shrink_node→shrink_lruvec→isolate_lru_pages→回收或换出 (B3/B4)
  - LRU: lruvec→lists[LRU_INACTIVE_ANON]/[LRU_ACTIVE_ANON]/[LRU_INACTIVE_FILE]/[LRU_ACTIVE_FILE]→refault distance决定升/降级
  - 回收优先级: 文件页(有后备存储)→匿名页(需swap)→unevictable(mlock/memfd)
- **换页(Swap)**: 匿名页→swap分区/文件→PTE替换为swap entry→换出→换入(memory.cgroup压力)→do_swap_page→swappiness(0-100,默认60)控制倾向 (B1/Ch5, B3)
  - swap entry: PTE低12位为0→高位存swap_type+swap_offset→标识"此页在swap中,不在物理内存"
  - swappiness: >60偏swap匿名页/<60偏回收文件页→数据库建议设为0-10
- **OOM Killer**: 内存耗尽→回收无效→out_of_memory→badness评分(进程内存/rss/oom_score_adj)→选择最高分→oom_kill_process→SIGKILL→dmesg查看决策日志 [Gap: AI从内核源码补充] (AI-supplement)
  - oom_score: `/proc/PID/oom_score`(动态调整)→`/proc/PID/oom_score_adj`(手动调整, -1000永不杀)
  - OOM日志解读: "Out of memory: Killed process XXXX (process_name) total-vm:XXXXkB, anon-rss:XXXXkB"

### 1.5 NUMA 与进阶内存主题 (🟡🟢)
- **NUMA架构**: node→zone→page→CPU和内存分node→本地访存快/远端慢→`numactl --membind`→`numastat`→`/sys/devices/system/node/node*/distance`矩阵→内存分配策略(mbind/set_mempolicy) (B1/Ch5, B2/B3/B5)
- **透明大页(THP)**: khugepaged扫描→尝试合并4KB页为2MB→分配时可能compaction→碎片严重时分配延迟数十ms→MongoDB/Redis建议`transparent_hugepage=never` [Gap: AI补充] (AI-supplement)
- **KSM(内核同页合并)**: 扫描内存→找到相同内容的匿名页→合并为一个写保护→COW触发时重新分离→KVM虚拟化收益大(多VM相同OS页)→`/sys/kernel/mm/ksm/`控制 (AI-supplement)
- **SLUB vs SLAB**: SLUB取消着色和per-node队列→更简→Linux 2.6.23+默认→SLAB仅保留历史兼容 [Gap: AI补充SLUB细节] (AI-supplement)
- **memblock**: bootmem替代→early alloc→`memblock=debug`内核参数→`/sys/kernel/debug/memblock/reserved` (B4)

---

## 阶段2: 并发与同步 — 内核最优雅的设计 (B集群, 依赖A内存)

### 2.1 硬件基础 — MESI缓存一致性 (🔴)
- **MESI协议**: 每个缓存行的4种状态: Modified(已修改,独占)/Exclusive(独占,干净)/Shared(共享,多核有副本)/Invalid(无效)→snooping总线嗅探→状态转换图: M←E→S→I循环 (B1, B6)
  - 状态转换触发: Local Read→Local Write→Remote Read→Remote Write→每个转换对应一个总线事务
  - Store Buffer: CPU写不等待invalidate-ack→先写store buffer→Store Forwarding(本核读先查store buffer)→StoreLoad屏障清空→x86的TSO模型
- **伪共享(False Sharing)**: 两个CPU写不同变量但位于同一缓存行(64字节)→一个写导致另一CPU的缓存行Invalid→触发MESI→频繁无效→性能暴跌 (B1/B2/B6)
  - 检测: `perf c2c`(cache-to-cache)→"False Sharing"检测→热点行HITM计数
  - 修复: `__cacheline_aligned`→padding填充到64字节边界→Java `@Contended`→C `____cacheline_aligned`
- **缓存类型**: 虚拟索引虚拟标记(VIVT→别名问题)/虚拟索引物理标记(VIPT→Way数限制)/物理索引物理标记(PIPT→慢但无别名)→现代CPU多用VIPT L1+PIPT L2+ / 异步存储模型(写操作可缓冲→Store Buffer/Write-Combining Buffer→内存模型基础) (B6)

### 2.2 原子操作与内存屏障 (🔴)
- **原子操作**: LOCK指令前缀→锁总线或锁缓存行→`cmpxchg`(Compare-and-Swap)→`atomic_add`/`atomic_sub`/`atomic_cmpxchg`→CAS自旋→`xchg`(原子交换)→`cmpxchg16b`(16字节CAS) (B1/Ch10, B2/B3/B5)
  - CAS循环: `do { old = atomic_read(&v); new = old + 1; } while (!atomic_cmpxchg(&v, old, new));`→自旋等待→非阻塞
  - LOCK前缀开销: 锁缓存行~数十cycles→锁总线~数百cycles(已被MESI优化)
- **内存屏障**: 四种屏障→禁止编译器和CPU重排序→x86实现 (B1/B2/B3/B5/B6)
  - StoreLoad(最贵): `mfence`(完整屏障)→Store Buffer刷新→等待所有pending store完成→后继load可见
  - StoreStore: `sfence`→写顺序保证→写合并缓冲区(WC Buffer)刷新
  - LoadLoad+LoadStore: `lfence`→读顺序+读不越过写
  - 编译器屏障: `barrier()`→禁止编译器重排跨屏障→不生成CPU指令→`READ_ONCE/WRITE_ONCE`→禁止撕裂读/写
  - Linux屏障API: `smp_mb()/smp_rmb()/smp_wmb()/smp_read_barrier_depends()`→SMP编译为实际屏障→UP编译为`barrier()`

### 2.3 锁家族 — 从Mutex到RCU (🔴🟡)
- **互斥锁(mutex)**: 睡眠等待→当锁被持有时调用者阻塞→schedule放弃CPU→醒来后重试→`optimistic spinning`(OSQ)→`PI`(优先级继承)→生产最常用 (B1/Ch9, B2/B3/B4/B5/B6)
  - 数据结构: `struct mutex { atomic_long_t owner; spinlock_t wait_lock; struct list_head wait_list; }`
  - optimistic spinning: mutex持有时间短→不自旋→可能MCS锁排队→减少缓存行争用
  - PI(优先级继承): 高优先任务等低优先级持锁→临rb时提升持有者优先级→避免优先级反转
- **自旋锁(spinlock)**: 忙等(while循环)→不睡眠→关内核抢占→临界区极短(几行代码)→不可在自旋锁持有时睡眠→中断上下文安全 (B1/B2/B3/B4/B5/B6)
  - ticket lock→qspinlock: 排队自旋→FIFO→减少缓存行ping-pong→4字节→嵌入其他结构
  - 不可递归: 自旋锁不支持同一CPU重复加锁→无owner跟踪→死锁
- **读写锁(rwlock)**: 读并发(多个读者同时持锁)/写排他→读者优先→写者可能饥饿→RCU是更好的读方案→`rwlock_t`→`read_lock/read_unlock`→`write_lock/write_unlock` (B1/B2/B3/B4/B5/B6)
- **信号量(semaphore)**: 计数型→允N个并发(N≥1)→`down`(获取)/`up`(释放)→`down_interruptible`(可中断)→互斥锁是N=1的信号量特例→`sem_init/sem_wait/sem_post` POSIX接口 (B1/B2/B3/B4/B5/B6)
- **futex(Fast Userspace muTEX)**: 用户态无竞争时纯用户态操作(原子CAS)→无系统调用→有竞争时`futex(FUTEX_WAIT)`陷入内核→`futex(FUTEX_WAKE)`唤醒等待→`PI-futex`支持优先级继承→Java LockSupport.park()底层→pthread_mutex基于futex (B1/Ch10, B3/B5)
  - 关键结构: `struct futex_q { struct plist_node list; struct task_struct *task; union futex_key key; }`
- **顺序锁(seqlock)**: 写不阻塞读→读时记sequence→读完检查sequence是否变化→变化则重读→适合写极少读极多的场景→jiffies使用seqlock (B5)
  - `write_seqlock(&lock); /* write */ write_sequnlock(&lock);`→写时sequence递增(奇数=写进行中)
  - `do { seq = read_seqbegin(&lock); /* read */ } while (read_seqretry(&lock, seq));`→偶数=无写者

### 2.4 高级同步 — RCU与死锁 (🔴🟡)
- **RCU(Read-Copy-Update)**: 读零开销→完全无锁→写先复制→修改副本→替换指针→等待宽限期(Grace Period)→原有读者都离开→释放旧数据→`rcu_read_lock/rcu_read_unlock`(空操作,关内核抢占) (B2/B3/B4/B5)
  - Grace Period: 每个CPU经历一次上下文切换→证明所有读者已经完成→call_rcu→synchronize_rcu→blocking等
  - 使用场景: 链表遍历(无锁)/radix tree/nf_conntrack→多读者单写者→Linux内核最优雅的设计决策
- **死锁**: 4必要条件(互斥/持有等待/不可剥夺/循环等待)→wait-for graph→检测(每次请求锁时DFS找环)→预防(全部资源预分配)→避免(银行家算法Safe State判断)→恢复(杀进程或回滚) (B1/Ch9, B6)
  - 内核死锁检测: `lockdep`→跟踪锁获取顺序→发现潜在循环→报告"possible circular locking dependency detected"
  - `echo t > /proc/sysrq-trigger`→dmesg所有任务栈→分析spinlock/mutex持有者→定位死锁
- **活锁**: 两个线程不断改变状态但都无法进展→区别于死锁(死=不进展,活=进展但无意义)→示例:皮特森算法在没有正确实现时的行为→检测难→靠超时 (B1/Ch9)

### 2.5 同步机制与案例 (🟡🟢)
- **per-CPU变量**: `DEFINE_PER_CPU(type, name)`→`get_cpu_var/put_cpu_var`→每个CPU独立副本→无锁→适合统计计数器→`this_cpu_inc`→底层用`%gs`偏移寻址 (B1/B2/B3)
- **内核抢占**: voluntary(CONFIG_PREEMPT_NONE)→preemptible(CONFIG_PREEMPT)→fully preemptible(CONFIG_PREEMPT_RT)→`preempt_count`跟踪→自旋锁自动关抢占→`preempt_disable/preempt_enable` (B3/B4/B5)
- **Dekker算法**: 两个线程的纯软件互斥→flag+turn变量→历史意义→硬件原子操作出现前最后的纯软件方案 (B6)
- **皮特森算法**: 两个进程的软件互斥→`flag[i]=true; turn=j; while(flag[j]&&turn==j);`→先重置turn再等对方→保证互斥+无饥饿 (B1/Ch10)
- **事件计数/定序器**: 非阻塞同步原语→await(E, V)→advance(E)→适合生产者消费者 (B6)
- **Redis无锁案例**: 单线程事件循环+原子操作(计数器/bitmap)+Lua原子脚本→无锁高并发模型→`INCR`原子性由单线程保证 (B2)
- **Nginx进程模型**: master(配置管理)+worker(N个,各自独立epoll)→共享监听socket→SO_REUSEPORT→惊群处理 (B2)
- **引用计数(kref)**: `kref_init→kref_get→kref_put(release)`→内核对象生命周期管理→防止use-after-free (B5)

---

## 阶段3: 进程、调度与中断 — OS运转的核心 (C集群, 依赖A内存+B同步)

### 3.1 中断处理 — 从硬件到软中断 (🔴)
- **中断处理全链路**: 设备触发IRQ→PIC/APIC路由→CPU响应→保存上下文→IDT表查找→中断门→`common_interrupt`→`do_IRQ`→`handle_irq`→ISR(中断服务例程)→`irq_exit`→触发软中断 (B1/B2/B3/B4/B5)
  - IDT(中断描述符表): 256项→前32个为CPU异常(除0/NMI/page fault)→系统调用(vector 0x80/syscall)→设备中断(vector 32-255)
  - 中断门 vs 陷阱门: 中断门自动关IF(禁止嵌套中断)→陷阱门不关IF(用于系统调用)
- **上半部与下半部**: 上半部(ISR:关中断,极短,通常<100微秒)→下半部(软中断/tasklet/workqueue:开中断,可延迟) (B1/B2/B3/B4/B5)
  - 软中断(softirq): 静态定义(HI_SOFTIRQ/TIMER_SOFTIRQ/NET_TX_SOFTIRQ/NET_RX_SOFTIRQ)→do_softirq→最多4个CPU同时执行→不能睡眠
  - tasklet: 基于软中断(TASKLET_SOFTIRQ)→同一tasklet不能同时在多CPU执行→比workqueue更轻→适合简单延迟操作
- **工作队列(workqueue)**: 内核线程池→`schedule_work`→events线程取work执行→`cmwq`(Concurrency Managed Workqueue)→`alloc_ordered_workqueue`(保序)→`flush_workqueue`(等待全部完成)→可睡眠 (B2/B3)
- **时间管理**: jiffies(时钟中断次数,通常HZ=250)→xtime(wall time)→`hrtimer`(高精度定时器,纳秒级)→tickless(无Tick,idle时不发时钟中断,节能)→clockevent设备→`ktime_get()`→`schedule_timeout` (B3/B4)

### 3.2 进程调度 — CFS核心 (🔴)
- **CFS(完全公平调度器)**: 核心概念→每个进程积累vruntime→vruntime小的优先运行→红黑树按vruntime排序→pick_next_task_fair取最左节点→nice值映射为weight→vruntime增长=实际运行时间/weight→I/O密集型自然积累更少vruntime→隐式优先级 (B1/Ch7, B2/B3/B4/B5)
  - 数据结构: `struct cfs_rq { struct rb_root_cached tasks_timeline; u64 min_vruntime; unsigned int nr_running; struct load_weight load; }` / `struct sched_entity { u64 vruntime; u64 exec_start; u64 sum_exec_runtime; struct load_weight load; }`
  - nice值映射: nice 0 → weight 1024 / nice -1 → 1277 / nice -20 → 88761 / vruntime增长=实际时间×1024/weight
  - 调度周期: `sysctl_sched_latency`(默认6ms)→保证每个可运行进程在周期内至少运行一次→`sched_min_granularity`(默认0.75ms)防止过多切换
- **调度器演进**: O(N)(遍历,2.4)→O(1)(优先级数组位图,2.6早期)→CFS(红黑树,2.6.23+)→EEVDF(最早虚拟截至时间优先,6.6+) (B1/Ch7, B3/B4/B5)
- **负载均衡**: CPU间迁移→`load_balance`→调度域(sched_domain)/调度组(sched_group)→`SD_BALANCE_NEWIDLE`/`SD_BALANCE_WAKE`/`SD_BALANCE_FORK`→`active_load_balance`→NUMA域→migration内核线程 (B1/Ch7, B3/B5)
- **CGroup CPU**: `cpu.shares`(权重)/`cpu.cfs_period_us`(周期,默认100ms)/`cpu.cfs_quota_us`(配额)→容器CPU限制→`cpu.max "$MAX $PERIOD"`(cgroup v2)→`cpu.stat`(usage_usec/user_usec/system_usec) (B1/B2)
- **实时调度**: SCHED_FIFO(固定优先级,不抢占运行到阻塞或主动放弃)/SCHED_RR(同优先级时间片轮转)→POSIX实时→`chrt`设置→SCHED_DEADLINE(最早截止时间优先,每周期预算)→`sched_setattr` (B4)
- **经典调度策略**: FCFS(先到先服务,convoy效应)/SJF(最短作业优先,需预测)/RR(轮转,时间片)→MLFQ(多级反馈队列:多个队列+优先级+时间片大小变化+提升机制防止饥饿)→现代调度器基础 (B1/Ch7)

### 3.3 进程模型 — 从fork到上下文切换 (🔴🟡)
- **进程生命周期**: fork(拷贝进程)→exec(加载程序,替换地址空间)→exit(退出,do_exit,设置EXIT_ZOMBIE)→wait(父进程等待子进程退出,回收task_struct)→僵尸进程(exit但父未wait)/孤儿进程(父先退出→init领养) (B1/Ch6, B2/B3/B4/B5)
  - fork实现: `_do_fork→copy_process→copy_mm(页表)→copy_files→copy_fs→copy_sighand→alloc_pid`
  - vfork: 不拷贝页表→子进程在父地址空间运行→阻塞父进程→子进程exec后父继续→轻量
  - clone: 细粒度共享→标志(CLONE_VM/CLONE_FS/CLONE_FILES/CLONE_SIGHAND/CLONE_THREAD)→实现线程
- **PCB(task_struct)**: 内核视角的"进程"→`pid`/`tgid`(线程组ID)/`state`(TASK_RUNNING/TASK_INTERRUPTIBLE/TASK_UNINTERRUPTIBLE/TASK_STOPPED/EXIT_ZOMBIE)/`stack`(内核栈)/`mm`(地址空间)/`files`(打开文件表)/`signal`(信号处理)/`sched_entity`(调度实体) (B1/Ch6, B3/B4)
  - 进程状态转换: fork→TASK_RUNNING→等待资源→TASK_INTERRUPTIBLE(可被信号唤醒→TASK_RUNNING)/TASK_UNINTERRUPTIBLE(不被信号唤醒)→条件满足→TASK_RUNNING→exit→EXIT_ZOMBIE→wait→释放
- **上下文切换**: `schedule()→__schedule→context_switch→switch_mm(切换页表→写CR3→TLB刷新)→switch_to(保存寄存器(rsp/rbp/rip等)→切换内核栈→恢复新进程寄存器)→__switch_to_asm`→一次切换开销~1-3微秒 (B1/Ch6, B2/B3/B4/B5/B6)
  - 切换时机: 主动(进程调用sleep/yield)/被动(时间片用完→tick→scheduler_tick→need_resched→schedule)
- **系统调用**: 用户态→syscall/sysenter指令→MSR寄存器存入口地址→entry_SYSCALL_64→保存寄存器→`do_syscall_64`→`sys_call_table[rax]→sys_read/sys_write/sys_futex`→恢复寄存器→`sysret/sysexit`→返回用户态→strace/pstack可追踪 (B1/Ch3, B2/B3/B4/B5)
- **信号处理**: `kill/tkill/tgkill`发送→`send_signal→__send_signal`→目标进程`task_struct→pending`队列→返回用户态/内核态→`do_signal→handle_signal→setup_rt_frame→用户态信号处理函数→sigreturn` (B1/B2/B3/B4/B5)
  - 不可靠信号(1-31) vs 可靠信号(34-64): 不可靠信号可能丢失/可靠信号排队
  - SIGKILL(9)必杀/SIGTERM(15)可捕获/SIGSTOP(19)挂起/SIGCONT(18)继续/SIGSEGV(11)段错误
- **ELF格式与ABI**: 可执行文件→ELF头→Program Header(段信息,加载用)→Section Header(节信息,链接用)→`.text`(代码)/`.data`(初始化数据)/`.bss`(零初始化)/`.rodata`(只读)→动态链接→`PT_INTERP`(ld.so)→`.plt/.got`→System V AMD64 ABI(参数:rdi,rsi,rdx,rcx,r8,r9→返回:rax) (B3/B4/B5)

### 3.4 惊群问题 (🔴)
- **惊群现象**: 多进程/线程同时`accept`同一个socket→新连接到达→所有等待者被唤醒→只有一个accept成功→其余继续睡眠→大量无效唤醒→浪费CPU (B2)
  - 4.5内核前: `accept`惊群→每个子进程都被唤醒→nginx使用`accept_mutex`规避→互斥accept
  - 4.5内核+: `SO_REUSEPORT`→socket按hash分配到特定进程→不惊群→`EPOLLEXCLUSIVE`→epoll只唤醒一个等待者→nginx用`socket(SO_REUSEPORT)`+`epoll(EPOLLEXCLUSIVE)`消除惊群
- **Nginx案例**: master(配置管理,signal→worker重载配置)+worker(N个,通常=CPU核心)→共享监听socket→每个worker独立epoll事件循环→`use epoll`→`multi_accept on`(一次性接受所有连接) (B2)

---

## 阶段4: 文件系统与 I/O — 数据持久化的基础 (D集群, 依赖A内存+C进程)

### 4.1 VFS 虚拟文件系统 (🔴)
- **VFS统一接口**: super_block(文件系统超级块)→inode(文件元数据)→dentry(目录项)→file(打开文件)→统一接口: `open/read/write/lseek/close/ioctl`→具体文件系统实现各自操作集 (B1/Ch11, B2/B3/B4/B5)
  - 四大对象操作集: `super_operations`(alloc_inode/destroy_inode/write_super)/`inode_operations`(create/link/mkdir/rename)/`dentry_operations`(d_compare/d_hash/d_delete)/`file_operations`(read/write/mmap/open/ioctl)
  - 路径解析: `namei→path_lookup→link_path_walk`→逐分量查找dentry→`do_last`处理最后分量→open或create
- **inode**: `struct inode`→`i_ino`(inode号)/`i_mode`(类型+权限)/`i_uid`/`i_gid`/`i_size`/`i_blocks`/`i_atime/i_mtime/i_ctime`/`i_mapping`(address_space)→`i_op`(inode_operations)→`i_fop`(file_operations)→`ls -i`查看inode号 (B1/Ch11, B3/B4)
- **ext4文件系统**: 区段(extent:起始块+长度)→日志(JBD2:元数据/ordered/data/writeback)→block group(8K-64K块一组)→inode table→inode bitmap→block bitmap→支持文件最大16TB/文件系统1EB (B2/B3/B4/B5)
  - extent树: `ext4_extent_header→ext4_extent_idx(索引)→ext4_extent(叶,ee_block+ee_len+ee_start)`→比间接块减少元数据开销→大文件连续分配=1 extent
  - 日志模式: ordered(元数据日志+数据先写,默认)/writeback(仅元数据日志,最快但可能旧数据出现在崩溃后)/data(元数据+数据都日志,最安全但最慢)

### 4.2 页缓存与 I/O 路径 (🔴)
- **页缓存全流程**: 读→`do_generic_file_read→find_get_page→页缓存未命中→page_cache_sync_readahead(预读)→readpage(磁盘IO)`→加入address_space→radix tree/xarray→再次访问→页缓存命中→零IO→写→`generic_perform_write→__block_write_begin(读块到页)→写入修改→mark_buffer_dirty→标记页脏→set_page_dirty→dirty_expire_interval(30s)→回写` (B1/Ch11, B3/B4/B5)
- **直接I/O(Direct I/O)**: O_DIRECT→绕过页缓存→用户空间buffer直接→磁盘→应用自己管理缓存→数据库(MySQL/PostgreSQL)常用→buffer对齐要求(512字节)
  - 缺点: 没有预读(无readahead)/没有回写合并(需应用自行batch)→`fio --direct=1`测试→`dd iflag=direct`验证
- **mmap I/O**: 文件映射→`do_mmap→addr→缺页异常→filemap_fault→readpage→page cache`→msync刷回→共享内存方式→`/proc/PID/maps`→`pmap PID`查看→比read/write减少一次拷贝(不经过用户buffer的额外拷贝) (B1/Ch11, B2/B4/B5)

### 4.3 I/O 调度与块设备 (🟡)
- **I/O调度器**: 请求合并(相邻扇区合并)+排序(LBA排序)→减少磁盘寻道→multiqueue→kyber/mq-deadline/none→NVMe用none(无调度) (B2/B3/B5)
  - 电梯算法: 按LBA排序→磁头单向扫→折返→latency增加→Deadline:读写分开队列+超时保证→CFQ:按进程分队列+轮转公平→已废弃
  - blk-mq: 多队列→per-CPU软件队列→硬件派发队列→NVMe多队列深度→减少锁竞争
- **块设备层**: `bio`(块I/O操作,vec+起始扇区+大小)→`request`(加入scheduler)→`request_queue`→gendisk→`submit_bio→generic_make_request→blk_mq_make_request→dispatch→驱动` (B3/B5)
- **零拷贝**: 绕过CPU拷贝→`sendfile(out_fd, in_fd, &offset, count)`→DMA磁盘→DMA网卡→无CPU参与→`splice`(管道零拷贝)→`tee`(管道复制)→Kafka/NGINX高性能秘诀→`SO_ZEROCOPY`(send) (B1/B2)

### 4.4 网络 I/O — epoll (🔴)
- **epoll三轮**: `epoll_create(int size)`创建epoll实例→`epoll_ctl(epfd, EPOLL_CTL_ADD/EPOLL_CTL_MOD/EPOLL_CTL_DEL, fd, &event)`→`epoll_wait(epfd, events, maxevents, timeout)`等待事件 (B2/B4)
  - epoll vs select/poll: select(遍历fd_set,O(n),fd限制1024)→poll(链表,无fd限制,仍需遍历O(n))→epoll(回调注册,O(1),事件驱动)
  - 红黑树存储所有注册fd→就绪链表→epoll_wait→检查就绪链表→非空返回→空则睡眠→事件到达→ep_poll_callback→加入就绪链表→唤醒epoll_wait
  - 触发模式: Level-Triggered(默认,未处理完持续通知)/Edge-Triggered(只通知一次,需配合非阻塞IO直到EAGAIN)
- **socket层**: `socket()`→`bind`→`listen`→`accept`→`struct sock`(协议无关)/`struct sk_buff`(数据包,data+len+head+tail+end)→协议栈:TCP→IP→netfilter→驱动→网卡 (B1/B2/B5)
- **Netfilter/iptables**: 5链(PREROUTING/INPUT/FORWARD/OUTPUT/POSTROUTING)→4表(raw/mangle/nat/filter)→hook函数(每个链点注册)→`nf_register_net_hook`→Docker/K8s网络底层→`iptables -t nat -L -n -v` (B1/B2/B5)

### 4.5 文件系统高级主题 (🟢)
- **日志(JBD2)**: ext4的日志→`jbd2`→journal block device→`jbd2_journal_start/stop`→事务→`ordered`模式(元数据日志,数据先写)/`writeback`(仅元数据)/`data`(全部)→崩溃恢复→重放完成的事务 (B1/Ch12)
- **COW文件系统**: Btrfs/ZFS→写不覆盖→新位置写→指针切换→快照(零成本)/checksum(数据完整性)/压缩→Btrfs的COW通过`btrfs_cow_block`→ZFS的`dmu_tx_assign` (B1/Ch12, B4)
- **Soft updates**: 依赖追踪→保证磁盘上块写入顺序→无日志→一致性→FreeBSD UFS→Linux未采用 (B1/Ch12)
- **LFS(日志结构FS)**: 所有写追加到日志尾→顺序写→segment cleaning(回收碎片)→SSD友好→F2FS(Flash-Friendly)→`append-only`→segment→GC (B1)
- **FUSE**: user→VFS→FUSE内核模块→`/dev/fuse`→用户态daemon→实际文件系统操作→`sshfs/s3fs/gcsfuse`→`libfuse`→`fuse_operations`→性能比内核FS差但灵活 (B1/Ch11)
- **fsck**: 文件系统一致性检查→`e2fsck`→检查superblock→inode bitmap一致性→`lost+found`→日志恢复后一般不需要fsck→ext4默认每N次mount自动检查→`tune2fs -c`设置

---

## 阶段5: 虚拟化、容器与 IPC — 现代基础设施 (E集群, 依赖A内存+C进程)

### 5.1 容器技术 — Namespace + Cgroup (🔴)
- **Namespace(七种)**: `clone(CLONE_NEW*)`→`unshare`→`setns`→每种namespace独立 (B1/B2/B4)
  - UTS: 主机名/域名隔离→`hostname`
  - PID: 进程树隔离→子namespace的PID 1→`/proc`显示新视角→pid_for_children
  - NET: 网卡/路由/iptables隔离→veth pair连接ns→docker0网桥
  - MNT: 挂载点隔离→`pivot_root`切换根文件系统→容器镜像层(overlay)
  - IPC: System V IPC/POSIX消息队列隔离
  - USER: UID/GID映射→非root用户在容器内可映射为root→安全关键
  - CGROUP: cgroup namespace(v4.6+)→`/proc/self/cgroup`显示路径仅当前cgroup
- **Cgroups(v1+v2)**: 限制/统计/隔离进程组→`cpu/memory/blkio/net_cls/net_prio/devices/freezer/hugetlb/pids/perf_event/rdma`→v1(cgroupfs层级挂载)→v2(统一层级,`cgroup2`文件系统) (B1/B2/B4)
  - memory cgroup: `memory.limit_in_bytes`/`memory.usage_in_bytes`/`memory.stat`→OOM→`memory.oom_control`→`docker run -m 512m`
  - cpu cgroup: `cpu.shares`(v1权重)/`cpu.cfs_quota_us`(v1配额)→`cpu.max`(v2:"$MAX $PERIOD")→`cpu.stat`

### 5.2 虚拟化 — CPU/内存/IO (🟡)
- **CPU虚拟化**: VMX(VT-x)→VM-Entry(进入guest)/VM-Exit(退出guest,trap)→VMCS(控制结构)→EPT(扩展页表,GPA→HPA二级翻译)→影子页表(软件维护GVA→HPA) (B1/B4/B5)
- **内存虚拟化**: EPT(硬件二级翻译)/NPT(AMD)→内存过量分配→`balloon driver`(guest释放内存给host)→`KSM`合并相同页→`THP`减少EPT miss
- **IOMMU**: DMA重映射(设备DMA→IOVA→MPA)→中断重映射→设备直通(`passthrough`/VIFO)→SR-IOV(一个物理网卡分多个VF)→`intel_iommu=on` (B4/B5)
- **virtio**: 半虚拟化IO→guest写virto queue→kick hypervisor→host处理→inject中断→性能接近原生→`virtio-blk/virtio-net/virtio-scsi`→`vhost`(内核态加速) (B4)

### 5.3 IPC 进程间通信 (🟡)
- **管道(pipe)**: `pipe(int fd[2])`→fd[0]读/fd[1]写→环形缓冲区(通常16页=64KB)→写满阻塞→读空阻塞→shell `|`→匿名管道→FIFO(命名管道,`mkfifo`,持久化到文件系统) (B1/Ch8, B2/B3/B4)
- **共享内存**: 最快IPC→物理页同时映射到两个进程→无需拷贝→`shmget+shmat`(System V)→`mmap MAP_SHARED`(POSIX)→`/dev/shm`(tmpfs,默认RAM的一半)→需要额外同步(信号量/mutex) (B1/Ch8, B2/B3/B4)
- **消息队列**: POSIX MQ(`mq_open→mq_send→mq_receive`)→System V MQ(`msgget→msgsnd→msgrcv`)→有优先级→消息类型→异步→持久化(可设) (B1/Ch8)
- **Binder IPC**: Android专用→`/dev/binder`→一次拷贝(发送方→binder驱动→接收方)→service manager(binder context manager,类似DNS)→AIDL→`ioctl(BINDER_WRITE_READ)`→比Socket+共享内存更高效→安全(UIC/PID认证) (B1/Ch8, B4)
- **Netlink**: 内核与用户态通信→`netlink_kernel_create`→`nlmsg_put→nlmsg_unicast`→用户态`socket(AF_NETLINK)→bind→sendmsg/recvmsg`→iproute2(ip命令)→优于ioctl(异步,多播) (B5)

### 5.4 内核架构与启动 (🟡)
- **宏内核 vs 微内核**: Linux(宏内核:所有服务在内核空间→高效→模块化但耦合→一个驱动crash→全系统crash)/Minix/L4(微内核:最小内核+用户态服务→隔离好→IPC代价→性能差) (B1/Ch2, B3)
- **内核启动流程**: bootloader(GRUB)→解压内核→`startup_32/startup_64`(early setup)→`start_kernel`(核心初始化)→`setup_arch`(架构相关)→`mm_init`(内存)→`sched_init`(调度)→`rest_init`→`kernel_init`(第一个用户进程PID=1)→`init`→`idle` (B4/B5)
- **内核模块**: `.ko`→`insmod/rmmod/modprobe`→`lsmod`→内核符号表(`/proc/kallsyms`)→`MODULE_LICENSE("GPL")`→`module_init/module_exit`→`dmesg | grep module`→编译: `make -C /lib/modules/$(uname -r)/build M=$PWD modules` (B5)
- **内核数据结构**: list_head(双向循环,`list_for_each_entry`/`list_add`/`list_del`)→hlist(哈希链表,单指针头,节省空间)→rb_node(红黑树,`rb_insert_color`/`rb_erase`/`rb_first`→CFS/VMAs/mm计时器)→基数树(radix tree→xarray) (B4/B5)

### 5.5 调试与诊断工具 (🟡)
- **strace**: 追踪系统调用→`strace -p PID`→`strace -c`(统计/耗时汇总)→`strace -e trace=file,network`→`strace -f`(追踪子进程)→`strace -T`(显示耗时)→`strace -o output.log`→解读"为什么卡" (B2)
- **perf**: `perf top`(实时热点)→`perf record -g`→`perf report`→`perf stat`(性能计数器)→`perf sched`(调度分析)→`perf lock`(锁分析)→`perf c2c`(伪共享检测)
- **ftrace**: tracefs(`/sys/kernel/tracing`)→`trace-cmd`→`function_graph`→函数调用耗时→`irqsoff/preemptoff`延迟分析
- **bpftrace/eBPF**: 内核动态追踪→`bpftrace -e 'kprobe:do_sys_open { printf("%s: %s\n", comm, str(arg1)) }'`→比strace开销小

---

> **来源**: 6本书 84 KPs → 5集群 → 5阶段教学顺序
> **规划文件**: `内功修炼/01-OS内核.md` (622行)
> **深度**: 🔴=含struct/call flow+内核源码 | 🟡=机制+行为+设计原因 | 🟢=1-2句
