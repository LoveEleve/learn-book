# ptmalloc 与栈 — 用户态内存的 glibc 实现

> Cluster C: 2 KPs | 依赖: 12-memory-stats-tools | 读者基线: Buddy 分配器 + /proc/meminfo 解读
> 读者处境: 已读 12 篇（监控看的是"内核账本"）；本篇回答"用户态怎么花钱——malloc 怎么拿页、栈为什么自动增长"，并收束内存深度全链路
> 打开新视角: malloc 大多数时候不触发系统调用（用户态缓存层）、glibc 五级 bins 是"迷你伙伴系统"、栈与堆是两条完全不同的路径

---

### 概念依赖链

```
12-memory-stats-tools(内核账本/系统接口) → 本篇: 用户态分配(ptmalloc + 栈)
  ├─ §1 ptmalloc 五级 bins(用户态分配器骨架 — 依赖 12 篇 brk/mmap 接口概念)
  │    ├─ §2 brk vs mmap(与 OS 的分发决策 — 依赖 §1 arena/top chunk)
  │    │    └─ §3 chunk 结构/合并/tcache(复用与安全 — 依赖 §1 bins + §2 归还路径)
  │    └─ §4 进程栈与线程栈(另一条内存路径 — 依赖 04 篇 VMA + 05 篇缺页)
先讲: 分配器骨架 → 系统接口 → chunk 细节 → 栈
后续依赖: 03-文件系统(内存深度终章, 悬念收束全链路)
```

### 叙事顺序

1. 问题引入——malloc/free 是 C 程序最常用的两个函数——每次都触发系统调用？（**Aha: malloc 大多数时候不触发系统调用——glibc 在用户态先缓存一层**）
   - 过渡: 用户态的"缓存层"长什么样？——五级 bins
2. ptmalloc 结构——arena + fastbins/smallbins/largebins/unsorted bin
   - 过渡: 缓存满了/超大请求呢？——谁去问内核要？
3. brk vs mmap——128KB 阈值分发 + 归还策略
   - 过渡: 分出去的块怎么管理、怎么复用？——chunk 结构
4. chunk 结构——prev_size/size/fd/bk 双链表 + 合并 + tcache
   - 过渡: 堆讲完了——进程内存还有一条自动增长的路径？
5. 进程栈与线程栈——VM_GROWSDOWN + guard page + alloca（🟢）
   - 过渡: 堆与栈、用户态与内核态已齐——收束
6. 收束——malloc 到物理页的全链路回顾 + Aha Moment

### 1. ptmalloc 核心结构 — glibc 的五级 bins 体系

场景提示: 你的程序 `malloc(24)` 一百次——每次都调内核？一个进程里能同时存在多少个"堆"？ [写作时展开]

关键设计: glibc 用户态分配器（glibc/malloc/malloc.c）——每进程一主 arena + 多线程附属 arena：

```[pseudocode]
malloc(size) → __libc_malloc(bytes) → arena_get(ar_ptr, bytes) 取/建 arena
fastbins(默认请求 ≤128B, 64位; DEFAULT_MXFAST=64×SIZE_SZ/4, 数组10槽):
  单链表 LIFO, inuse 位保持置位不合并 — 极致速度
smallbins(chunk ≤0x3f0≈1000B 请求, 62 个): 双链表, free 与相邻 free chunk 合并 — unlink_chunk
largebins(chunk >0x400, 63 个): 按大小排序双链表, best-fit 查找
unsorted bin(1 个): 缓存层 — 刚释放的 chunk 先入此, 下次 malloc 先查, 未命中再分类
Arena 数量: MALLOC_ARENA_MAX 默认 8×cpu 核数(64位, NARENAS_FROM_NCORES) — 每 arena 独立锁, 减少多线程竞争
```

Why: 为什么是"五级"而不是一个链表？——**每级 tradeoff 不同**：fastbins 用"不合并"换速度（小对象频繁 alloc/free，合并开销大于碎片收益）；smallbins 用"双链表合并"控制碎片；largebins 用"排序+best-fit"满足大块精确需求；unsorted bin 是**延迟分类的缓存层**（刚释放的块最可能被再次请求）；tcache 在 2.26+ 再叠一层（§3）。**五级 = 五个不同"频率×大小"场景的专用缓存**——与内核 Buddy 的 order 分组（阶段1-01 篇）同一思想：按尺寸分级，热路径最快。**ptmalloc 的边界（对照 tcmalloc/jemalloc）**：ptmalloc 用"arena 池化 + 锁"而非"每线程堆"——高竞争场景（多核密集 malloc）arena 锁仍是瓶颈，tcmalloc/jemalloc 走线程本地缓存（TLS）免锁路线——ptmalloc 选择通用与兼容，牺牲极端多线程吞吐。 [内核: 五级 bins 与阶段1-01 篇 Buddy 的 order 分组同构——差异在层级: ptmalloc 在用户态缓存, Buddy 在内核管物理页]

比喻锚点: ptmalloc=五层货架——刚退的货（unsorted）先放门口顺手位，小件（fastbin）放手边格、中件（smallbin）放分类架、大件（largebin）按尺寸摆货架、每线程再加一个"自己的抽屉"（tcache）——取货先看最近的，没有才逐层找。 [写作时展开]

### 2. 系统调用分发 — brk vs mmap 的阈值决策

场景提示: 程序 malloc 掉 1GB 大数组，又反复分配 32B 小对象——它们向内核要内存的方式一样吗？ [写作时展开]

关键设计: 128KB 阈值（MMAP_THRESHOLD）双路径（glibc/malloc/malloc.c）：

```[pseudocode]
brk 路径(≤128KB, DEFAULT_MMAP_THRESHOLD_MIN): 堆扩展 — sys_brk → arena top chunk 不够 → sbrk 扩展 program break
  → 释放时若在 top chunk 顶端 → malloc_trim(pad) → sbrk(-pad) 缩回 OS
  → 中途的小块释放不还 OS (fastbin/tcache 缓存)
mmap 路径(>128KB): sys_mmap 独立匿名映射 — 释放 → sys_munmap 立即归还, 无缓存
  → 2.26+ 动态阈值: 频繁释放大块 → 阈值自动上调 (上限 DEFAULT_MMAP_THRESHOLD_MAX=32MB, 64位)
```

Why: 为什么小请求用 brk、大请求用 mmap？——**brk 是"线性堆"**：只移动 program break 一个指针，成本最低；但它只能从顶端归还（中间释放的 hole 无法还给内核）。**大块用 mmap 独立映射**：释放即归还、互不干扰（大块少，归还收益大），但每次分配/释放都触发系统调用。**128KB 阈值 = 系统调用成本与归还收益的平衡点**——小块的系统调用成本占比高（缓存更值钱），大块的归还收益高（不缓存更值钱）。 [man 2 brk / man 2 mmap: brk 移动 program break, mmap 建匿名映射] [内核: brk/mmap 落地于 04 篇 VMA——sbrk 改 brk VMA 边界、mmap 新建 VMA; 12 篇 meminfo 的 Committed_AS 即这两路径的承诺账]

比喻锚点: brk=单位食堂的餐盘队列（往前推一格，退只在队尾退）；mmap=单独点外卖（一份一份下单结账，吃完就走）——小餐盘排队划算，聚餐（大块）直接下单。 [写作时展开]

### 3. 双链表管理 — chunk 的复用与合并

场景提示: malloc/free 交错 10 万次后，内存还紧凑吗？相邻空闲块会合并吗？glibc 2.26 后快在哪？ [写作时展开]

关键设计: 空闲块即链表节点（glibc/malloc/malloc.c）：

```[pseudocode]
malloc_chunk: prev_size(前一 chunk 大小) + size(本 chunk 大小, 低 3 位标志)
  标志: PREV_INUSE / IS_MMAPPED / NON_MAIN_ARENA
free chunk: fd(前向) + bk(后向) — 仅 small/large/unsorted 用 — unlink_chunk 摘除
合并: _int_free → 检查相邻是否 free → unlink → 合并成大 chunk → 入 unsorted bin
tcache(2.26+): per-thread 缓存 — 每 bin ≤7 entry(TCACHE_FILL_COUNT), 单链表无锁
  → 分配路径: tcache → fastbin → small/large → unsorted → top chunk
```

Why: 为什么 chunk 要"复用头部"？——**malloc_chunk 头（16B）在"已分配"时是用户数据的一部分**（prev_size 被借给前块数据用）——用"双链表的节点就是空闲块本身"（free 时把数据区改写为 fd/bk）避免额外链表内存；**合并解决连续 alloc→free 产生的 hole**（对照 Buddy 的合并，阶段1-01 篇——但 Buddy 合并由内核做，ptmalloc 合并由用户态做）。**tcache 是无锁热路径**：每线程私有，alloc/free 命中即返回，不碰 arena 锁——多线程 malloc 性能的核心来源。**运行统计接口（SRE 排查）**：`malloc_info()` 输出各 arena/bin 占用 XML（进程内 malloc 状态快照）、`mallinfo()` 返回汇总结构（mmap 次数/空闲字节/已分配字节）——排查"进程 malloc 是否泄漏"先看这两者再对照 12 篇 /proc 账本。 [内核: tcache 无锁 ≈ 02 篇 Buddy PCP per-CPU 缓存的用户态版本——同是"每 CPU/每线程私有缓存免锁"思想]

比喻锚点: chunk 复用=快递盒改造——盒子没拆前是包装（数据），拆开后盒盖内侧写上"下一个盒子在哪"（fd/bk）当货架标签；两个空盒靠一起就粘成一个大箱（合并）。 [写作时展开]

### 4. 进程栈与线程栈 — VM_GROWSDOWN + 保护页

场景提示: 无限递归会"栈溢出"——栈为什么自动往下长？为什么递归能撑到耗尽前最后一刻才崩？ [写作时展开]

关键设计: 栈是**向下生长的映射**，靠缺页自动扩展（mm/mmap.c, mm/memory.c）：

```[pseudocode]
主线程栈: mm→start_stack 向下 → VM_GROWSDOWN 标志
  → 访问未映射低地址 → 缺页 → expand_stack 扩展 VMA(mmap.c) → do_anonymous_page 分配匿名页(memory.c)
  → 上限: RLIMIT_STACK(默认 8MB) → 超限 → SIGSEGV
线程栈: pthread_create → allocate_stack → mmap MAP_ANONYMOUS|MAP_STACK(无 GROWSDOWN, 固定大小)
  → 默认 = RLIMIT_STACK(通常 8MB), 下限 PTHREAD_STACK_MIN → pthread_attr_setstacksize 可调
  → /proc/PID/maps [stack:THREAD_ID] 可查
保护页: guard page(1 页, 线程栈低地址端/栈底) — 触到 → SIGSEGV 而非继续扩展
调试: pthread_getattr_np 取运行中线程栈属性
```

Why: 为什么栈用"自动扩展"而堆用"显式分配"？——**栈的生命周期是 LIFO**（函数返回即释放），内核用"触到就长一页"（按需）让程序无需管理——只分配用到的栈深；**guard page 是"刹车"**：线程栈固定上限（不像主栈 RLIMIT_STACK 还可扩展），guard 页防止溢出写坏相邻映射。🟢 **alloca 的根基**：alloca 在栈帧内分配（随函数返回自动释放），比 malloc 快（无锁无 bin），但容量受栈大小限制（大 alloca 易栈溢出）——07-系统编程篇引用。 [man 3 alloca: 栈上分配, 随函数返回释放] [man 3 pthread_attr_setstacksize / man 7 pthreads: 线程栈大小与 guardsize 属性] [内核: 栈自动扩展是 05 篇匿名缺页的特例——缺页路径先 expand_stack 扩 VMA(mmap.c) 再 do_anonymous_page 分配页(memory.c), 比普通匿名缺页多 VMA 扩展一步] [x86: 栈向低地址增长——push 递减 rsp, VM_GROWSDOWN 正是匹配 x86 栈方向]

比喻锚点: 栈=向下挖的井（水位自动下降补页），guard page=井底的警示线（挖到线就响警报 SIGSEGV）；堆=往仓库里搬箱子（要多少拿多少，可还）；alloca=井里的吊桶（用完随吊绳自动收走）。 [写作时展开]

### 5. 收束

回到"用户态内存的 glibc 实现"全链路：

```
malloc(32) → tcache → fastbin → smallbin → unsorted → top chunk
  → brk 扩展 arena (≤128KB) | mmap 独立映射 (>128KB)
    → Buddy 分配物理页 (阶段1-01 篇) → 缺页建立映射 (05 篇)
栈: 缺页自动扩展 (do_anonymous_page) → guard page 兜底
监控: 12 篇 meminfo/smaps 看这两条路径的账
```

**Aha Moment**: "malloc 大多数时候根本不碰内核——**glibc 在用户态叠了五层缓存（bins），只有缓存耗尽才走 brk/mmap 向 Buddy 要页**；而栈是另一条路径：**内核按需自动生长，guard page 兜底**。用户态分配的真相是'先问 glibc，再问内核'——12 篇的监控账本记录的就是最终问内核的那部分。"
**回答读者三问**: ①malloc 每次都调系统调用吗=不，五级 bins+tcache 先兜；②brk vs mmap 怎么选=128KB 阈值，小线性堆大独立映射；③栈为什么自动长=VM_GROWSDOWN 触到就扩展，guard page 防溢出。

---

### 核心悬念

**"从 struct page 到 swap out，我们走完了 Linux 内存管理的完整闭环——但用户进程不只有内存，还有'文件'：磁盘上的数据怎么组织？Ext2 的 inode/块位图、页缓存与 writeback 怎么衔接内存篇？"**

→ 引出 03-文件系统 — 从 Ext2 磁盘布局到 VFS 跨域文件系统——内存管理的下一站：把"字节流"落盘。