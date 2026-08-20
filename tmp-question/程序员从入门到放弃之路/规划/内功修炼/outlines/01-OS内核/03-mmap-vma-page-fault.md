# mmap + VMA + 缺页异常 + COW 写时拷贝

> Cluster A: 10 KPs | 依赖: 02-分页机制 | 读者基线: 了解页表遍历和虚拟地址空间概念
> 读者处境: 已读完 02 篇，知道页表把虚拟地址翻译成物理地址；本篇回答"映射建立后物理页为何不立即分配"
> 打开新视角: 延迟分配的机制与收益、fork 为何快、段错误的判定路径、overcommit 的三种策略

---

### 概念依赖链

```
02-分页(页表遍历/PTE 标志位) → 本篇: 映射与物理页的分离
  ├─ §1 mmap(建立映射, 不分配物理页 — 依赖 02 PTE)
  │    └─ §2 VMA(映射的描述结构 — 依赖 §1 的映射类型)
  │         └─ §3 缺页异常(访问时按 VMA 分配物理页 — 依赖 §1/§2)
  │              └─ §4 COW(写共享页时复制 — 依赖 02 RW 位 + §3 缺页路径)
先讲: 映射(承诺) → 描述(账本) → 触发(兑现) → 复制(写保护)
后续依赖: 04-页缓存(文件映射的缺页从页缓存取)、05-MESI(伪共享背景)
```

### 叙事顺序

1. 问题引入——`mmap` 返回后物理内存没增加，访问才增加（**Aha: 虚拟地址是"承诺"，物理页是"兑现"**）
   - 过渡: 承诺的内容由什么结构记录？
2. VMA——虚拟内存区域：区间/权限/映射源，红黑树+链表双索引
   - 过渡: 承诺如何被兑现？——访问触发缺页
3. 缺页异常——#PF 三分支：Minor/Major/Invalid
   - 过渡: 兑现时若遇到共享只读页——写它怎么办？
4. COW——fork 复制页表不复制数据，写时 #PF 复制
   - 过渡: 承诺可以无限多吗？——overcommit 限制
5. 收束——mmap 承诺/缺页兑现/COW 复制/overcommit 管控 = 虚拟内存四重奏

### 1. mmap — 分配虚拟地址但不分配物理页

场景提示: `mmap` 1GB 匿名内存后 RSS 未增长，访问后才增长——承诺 vs 兑现的差异。 [写作时展开]

关键设计: mmap 只建立"虚拟地址→文件/匿名"的**映射**，不分配物理页（demand paging 的入口）：

- **File-backed**: 映射文件页（缺页时从页缓存取）— 依赖 04 篇页缓存
- **Anonymous**: 匿名映射，demand-zero（缺页时分配零页）
- **Shared (MAP_SHARED)**: 多进程共享同一物理页（写可见，进程间通信方式之一）
- **Private (MAP_PRIVATE)**: COW 语义——读共享，写时复制（fork 同源机制）

brk vs mmap: brk/sbrk 扩展堆（连续增长区）；mmap 任意地址映射。malloc 小分配用 brk（默认 <128KB）、大分配用 mmap（>M_MMAP_THRESHOLD）——大块 mmap 释放时直接 munmap 归还内核（避免堆碎片），brk 区无法单独归还。

标志位 (include/uapi/linux/mman.h): MAP_SHARED/MAP_PRIVATE/MAP_ANONYMOUS/MAP_FIXED（精确指定地址，覆盖已有映射，危险）/MAP_LOCKED（mlock 锁内存防换出）。

Why: 为什么不立即分配物理页？——大部分映射区域从未被访问（如 1GB 预留缓冲）。延迟分配让"承诺"零成本，只有"兑现"才付出物理页代价；这就是虚拟内存的本质——地址空间便宜，物理页昂贵。

### 2. VMA — 虚拟内存区域的描述结构

场景提示: `/proc/PID/maps` 每行对应一个 VMA——进程地址空间的"账本"。 [写作时展开]

关键设计: `struct vm_area_struct` 描述一段连续虚拟地址区间 (include/linux/mm_types.h)：

| 字段 | 含义 |
|------|------|
| vm_start / vm_end | 区间 [start, end) |
| vm_flags | 权限位: VM_READ(0x1)/VM_WRITE(0x2)/VM_EXEC(0x4)/VM_SHARED(0x8)/VM_GROWSDOWN(栈) |
| vm_file | 映射文件（匿名为 NULL） |
| vm_ops | 操作集: fault(缺页回调)/close/split |

索引结构: `mm_struct` 以**红黑树(mm_rb) + 链表(mmap)双重索引**——红黑树按地址 O(log N) 查找"地址属于哪个 VMA"（缺页判据），链表用于遍历（/proc/PID/maps 输出）。`mm_struct` 还含 pgd(页表基址)/mm_users/start_code~start_stack 各段边界。

可视化: `/proc/PID/maps`（文本，每行一个 VMA）/`/proc/PID/smaps`（详细统计含 RSS）/`pmap PID`（摘要）。

Why: 为什么需要 VMA 而非直接靠页表？——页表只管"虚拟页→物理帧"翻译，不记录"这段地址的用途/权限/来源"。VMA 是**语义层**：缺页时查 VMA 决定怎么分配（文件/匿名/共享）、写保护时查 VMA 判断是 COW 还是非法写、munmap 时按 VMA 批量拆除。页表是翻译器，VMA 是土地局。 [内核: VMA 是 mm_struct 的语义元数据——页表无语义, 一切"该不该/怎么"的判断都在 VMA 层; mmap/munmap/mprotect 全部操作 VMA 而非页表]

### 3. 缺页异常 — 从 #PF 到物理页的三个分支

场景提示: 段错误(Segmentation Fault)与正常缺页的区别——内核如何区分"该分配"与"不该访问"。 [写作时展开]

关键设计: 访问虚拟地址 → TLB miss → 查页表 → Present=0 → 触发 #PF → `do_page_fault` → `handle_mm_fault`。三个分支：

| 分支 | 条件 | 动作 | 开销 |
|------|------|------|------|
| Minor Fault | 页已存在（页缓存/swap 缓存）但页表未建立 | 建立映射即可 | 最快，无 IO |
| Major Fault | 页不在内存 | 磁盘 IO（文件）或 swap 换入 | 最慢，可阻塞数十 ms |
| Invalid Fault | 地址不在任何 VMA 内 | `bad_area` → `force_sig(SIGSEGV)` → 进程终止 | 段错误 |

- Minor: `do_anonymous_page`（匿名）或 `do_fault`→`filemap_fault`（文件）
- Major: `do_swap_page`（swap 换入）或 `do_fault`→`readpage`（磁盘读）
- Invalid: 这是"该分配"与"不该访问"的分水岭——VMA 是判据

比喻锚点: 缺页=入住登记——VMA 是预订记录（承诺），Minor 是房间已备好只差钥匙（建映射），Major 是从仓库调货（磁盘 IO），Invalid 是没有预订闯入（SIGSEGV 撵人）。 [写作时展开]

Why: 为什么"访问不存在的页"不一律报错？——因为承诺(映射)与兑现(物理页)分离是设计核心：内核用 VMA 判断"这个地址在不在承诺范围内"，在则兑现（分配页），不在则报错。没有 VMA 判据，延迟分配根本无法工作。

### 4. COW 写时拷贝 — fork 的灵魂优化

场景提示: fork 一个 10GB 进程为何毫秒级返回——数据没复制，只复制了承诺。 [写作时展开]

关键设计: fork 子进程 → `copy_page_range` 复制**页表**（父子共享物理页）→ 父子 PTE 都清 RW 位（只读）→ 任一写触发 #PF → `do_wp_page` → 分配新物理页 → 复制内容 → 更新 PTE 为可写（本进程私有）：

```[pseudocode]
fork: 复制页表 + 全部 PTE 清 RW → 物理页零复制
写入: 任一进程写共享页 → #PF → do_wp_page → 复制物理页 → 各自私有可写
```

COW 判定链 (完整): 访问写共享页 → #PF（错误码含 write=1, present=1）→ `do_wp_page`：

```[pseudocode]
检查 VMA: vm_flags & VM_WRITE?
  ├─ 是 → COW: 复制物理页 → 本进程 PTE 改可写（私有化）
  └─ 否 → 检查 vm_flags & VM_SHARED?
       ├─ 是 → 合法只读共享: 更新 PTE 只读映射即可（不复制）
       └─ 否 → 非法写 → SIGSEGV
```

**判定依赖 02 篇的 PTE RW 位 + 本篇的 VMA 权限（VM_WRITE/VM_SHARED 两分支）**——两章机制在此交汇。 [内核: 判定核心是"PTE 硬件位(读/写/存在) + VMA 语义位(可写/共享)"双源——硬件只管权限, 语义归属由 VMA 决定]

Why: fork 为何不直接复制物理页？——子进程通常立刻 exec 抛弃全部映射（复制=浪费）；即使不 exec，大部分页只读共享。COW 把"可能复制"推迟到"确定写入"，平均成本趋近零。

overcommit 管控: `vm.overcommit_memory`（0=启发式/1=总是允许/2=严格按比例）+ `vm.overcommit_ratio`（严格模式阈值）——承诺可以虚开，但不能无限虚开；数据库建议 2（严格模式，防止内存耗尽时无页可兑现）。

### 5. 收束

回到 mmap 承诺 vs 兑现的完整图景：
- mmap = 承诺（建映射不分配页）→ VMA = 账本（记录承诺）
- 缺页 = 兑现（访问时按 VMA 分配）→ COW = 写保护下的延迟复制
- overcommit = 承诺管控（防止虚开过多）

**Aha Moment**: "`mmap` 返回的是一张借条（映射），不是现金（物理页）——进程访问时才去兑现（缺页分配）；fork 更是只复制借条，谁真用钱谁去银行（COW）。虚拟内存的优雅全在'延迟'二字。"
**回答读者三问**: ①mmap 大内存 RSS 不涨=延迟分配；②fork 快=页表复制+COW；③段错误=地址不在 VMA 承诺内。

---

### 核心悬念

**"文件映射的缺页（Major Fault）要从磁盘读——但读到的数据先进哪里？页缓存如何成为文件 IO 与缺页之间的中间层？回收时又怎么挑驱逐哪页？"**

→ 引出 04-页缓存 + 脏页回写 + LRU 回收 + Swap + OOM Killer——承诺兑现后，内存的管理才真正开始。
