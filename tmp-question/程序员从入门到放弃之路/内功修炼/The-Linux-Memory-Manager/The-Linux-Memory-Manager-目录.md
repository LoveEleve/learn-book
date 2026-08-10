# 《The Linux Memory Manager》—— 完整目录

> 作者：Lorenzo Stoakes | Early Access 2025 | 14章 | 从物理内存→虚拟内存→进程内存→缺页→反向映射→页缓存→回写→回收→交换→OOM→实践

---

### 第1章 INTRODUCTION（绪论）
1.1 Approach
1.2 Who Is This Book For?
1.3 Book Overview

### 第2章 PHYSICAL MEMORY（物理内存）
2.1 struct page（Metadata/struct slab/Page flags）
2.2 struct folio
2.3 Physical memory model（Sections/PFN validity/转换/Page block flags/查询struct page/folio）
2.4 Nodes and Zones（Low memory reserve/Total reserved pages）
2.5 Migrate types
2.6 GFP flags（zone modifiers/mobility和placement/Watermark/Reclaim/Action/预定义组合/Memalloc Flags）
2.7 Buddy allocator（Algorithm/Free lists/Per-CPU free Pages PCPs/Pagesets）
2.8 Allocator implementation（Visualising/__alloc_pages/rmqueue/rmqueue_buddy/rmqueue_pcplist/Migrate type fallback/New page preparation）
2.9 Freeing pages（__free_one_page）

### 第3章 VIRTUAL MEMORY（虚拟内存）
3.1 Page Tables（Page Table Operations/Page Table Flags/Flag Combinations/Traversal/Locking）
3.2 The Address Space
3.3 Direct Mapping（Bootstrapping/Initialization）
3.4 An introduction to the Transaction Lookaside Buffer（TLB）
3.5 The Kernel Virtual Memory Allocator vmalloc（Finding Free Block/Inserting/Physical Allocation/Virtual Mapping）

### 第4章 PROCESS MEMORY（进程内存）
4.1 Overcommit
4.2 Userland memory from 50,000 feet（Describing userland memory）
4.3 The process address space（mm_struct引用计数/初始进程地址空间/内核PGD维护/地址空间锁/地址空间flags）
4.4 Virtual Memory Areas VMAs（VMA flags/分配释放/布局/插入移除/遍历）
4.5 An introduction to the page cache
4.6 Per-process memory management statistical counters

### 第5章 MEMORY MAPPING（内存映射）
5.0.1 Program break
5.0.2 mmap()
5.0.3 mmap map flags
5.0.4 mmap kernel implementation
5.0.5 Choosing where to map
5.0.6 Unmapping memory
5.1 VMA merge and split（VMA merge/Mergeability/VMA adjust/VMA split）

### 第6章 PAGE FAULTS（缺页异常）
6.1 Hardware page fault handling（内核缺页/用户态缺页/Success条件/Informational/Error条件）
6.2 Page fault handling（Edge cases/Page flags）
6.3 Anonymous page fault（Zero page/anon_vma准备/物理页分配/PTE entry设置/反向映射+LRU+PTE设置）
6.4 Non-anonymous page fault
6.5 Non-anonymous read page fault
6.6 Non-anonymous MAP_PRIVATE Copy on Write page fault
6.7 Non-anonymous shared write fault
6.8 Shared non-anonymous page fault logic
6.9 Write-protected page fault（非匿名folios/匿名folios/Folio/PFN sharing/Page reuse/Folio copying）
6.10 Stack expansion
6.11 Userland bad area handling
6.12 Special mappings

### 第7章 REVERSE MAPPINGS（反向映射）
7.0 核心要点（匿名反向映射/文件反向映射类型/匿名反向映射对象/复用相邻VMA anon_vma/连接/克隆/fork时复用/fork克隆/VMA split和merge/folio操作/解除链接/Walking反向映射/Walking VMA）
7.1 Freeing userland memory（Unmapping内存映射区域/MMU gather初始化/映射内存范围/释放页表/Flushing TLB/释放页/Lazy TLB mode）

### 第8章 MANIPULATING USERLAND MEMORY（操作用户态内存）
8.1 Accessing User Access from the Kernel
8.1.1 Get User Pages GUP
8.1.2 Pinning Folios
8.1.3 GUP Flags
8.1.4 Following Folios
8.1.5 Walking Page Tables
8.1.6 Faulting in Pages
8.1.7 Fast GUP Functions
8.1.8 GUP Helper Functions
8.2 Userland Memory Manipulation APIs（mlock/mprotect/madvise）

### 第9章 THE PAGE CACHE（页缓存）
9.1 The Virtual File System VFS
9.2 A brief digression: extensible Arrays xarrays（API/The Rest）
9.3 Reading From Cache
9.4 Reading Page Cache Entries
9.5 File-Backed Read Faults（批量读入/单folio读入/添加folio到页缓存）
9.6 Reading Folios From Disk
9.7 Readahead（同步预读/异步预读/按需预读/命令预读代码/物理预读）
9.8 Fault-Around
9.9 Removing Page Cache Folios（Unmapping/Folio Truncation/Dropping/Eviction）
9.10 Buffers and Block I/O（Buffer Heads/Blockdev Buffer Heads/Accessing Blocks/Block Writes/Buffer Head I/O Operations）
9.11 Folio Locking and Waiting

### 第10章 WRITEBACK（回写机制）
10.1 Dirty Tracking in the Kernel
10.2 Marking the Folio Dirty
10.3 Marking the inode Dirty
10.4 Page Fault Dirty Tracking
10.5 File Write Dirty Tracking
10.6 Synchronising to Disk
10.7 sync / syncfs / fsync
10.10 Writing Back to Disk
10.11 File System and Background Writeback
10.12 Flusher Thread Operation
10.13 File Writeback
10.14 Dirty Throttling（脏页限流）
10.15 Rate-Limited Dirty Throttling
10.16-10.25 脏页限流详细参数（统计/带宽/位置控制比率/限制/轮询间隔/速率限制/暂停时间/核心限流/Writeback Chunk Size/Background Writeback阈值）

### 第11章 RECLAIM AND MEMORY PRESSURE（内存回收与内存压力）
11.1 Physical Allocation Slow Path
11.2 LRU Vectors（struct lruvec/lruvec Operations）
11.3 Direct Reclaim
11.4 Indirect Reclaim（初始化/kswapd线程/kswapd睡眠/Node Balancing）
11.5 The Reclaim Mechanism（Scan Control/Working Set概述/Shrinking节点/确定扫描平衡/Shrinking LRU Vectors/Shrinking单个LRU List/Shrinking Active List/隔离LRU Folios/Shrinking Inactive List/Reclaim引用检查/Reclaim Page Out/Reclaim映射移除）
11.6 Reclaim Throttling（通用限流/Direct Reclaim限流）
11.7 Folio Batches（CPU Folio Batches/LRU Rotation/mlock/Folio Operations/添加/激活/旋转/Deactivation/File Folio Deactivation/Lazy Free/mlock Batch/Drain）

### 第12章 SWAP MEMORY（交换内存）
12.1 The Swap Cache（初始化/分配folio到Swap Cache/页表映射）
12.2 Swapping Out（添加folio到Swap Cache/设置页表Swap Entries/Swap出盘/释放Swapped Out Folio）
12.3 Swapping In（缺页异常/Looking Up/从磁盘读/Cluster Readahead/VMA Readahead）

### 第13章 THE OUT OF MEMORY (OOM) KILLER（OOM杀手）
13.1 Causes of Out of Memory Conditions（内存分配/sysrq-f）
13.2 OOM Killer Score Adjustment（OOM Score）
13.3 OOM Killer Alternative Behaviours（vm.panic_on_oom/vm.oom_kill_allocating_task）
13.4 Kernel Interface（风险内存分配/手动sysrq-f触发/OOM Killer/Victim选择/Victim Killing/OOM Reaper）

### 第14章 PRACTICAL MEMORY MANAGEMENT（内存管理实践）
14.1 Measuring free memory（MemAvailable内核实现/Higher order folios）
14.2 Measuring the memory usage of a process（Proportional Set Size PSS）
14.3 Memory mapping using mmap()（映射匿名内存/映射文件/私有文件映射/固定映射）
14.4 Interpreting Out Of Memory reports（分配统计/栈追溯/全局空闲页/每节点统计/Zone统计/Buddy页统计/全局统计/OOM killer报告）
14.5 procfs memory interfaces（物理内存/虚拟内存/页表introspection）
14.6 Memory budgets
14.7 Sharing memory（System V共享内存/POSIX共享内存/匿名共享内存forked进程/memfd共享内存）
