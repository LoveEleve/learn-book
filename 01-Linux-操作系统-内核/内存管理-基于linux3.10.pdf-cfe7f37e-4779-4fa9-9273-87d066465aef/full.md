# Linux 启动之内存相关

基于 linux3.10

内存启动初始化过程，linux 内核内存管理，进程虚拟地址管理

# Linux3.10 内存管理

# 目录

Linux3.10 内存管理.

1.1物理内存的布局何时获得..   
1.2 CPU 架构.. 4

第二章 物理内存管理模型. 6

2.1 Node... .8   
2.2 Zone 结构.. .10  
2.3 page 结构.. .13

第三章内存初始化. .16

3.1 Linux 分页 . .17  
3.2 Setup_arch . .18   
3.3 per-CPU area 初始化 . ..24  
3.4 节点（node）和域（zone）初始化. ..24  
3.5 启用内核内存分配器. ..28

第四章 物理内存管理. .30

4.1 伙伴系统内存组织.. ..30  
4.2 伙伴系统 API .. ..32  
4.4 slab 分配器 . ..44

第五章 进程虚拟内存.. .54

5.1 进程准备知识.. .54   
5.2 文件和虚拟内存. .58  
5.3 虚拟内存区操作. .58   
5.4 创建地址区间.. ..62   
5.5 删除地址区间.. ..63

Linux内存管理是linux操作系统的子系统之一，是一个非常重要的子系统，这是一个冗杂而又庞大的部分，和网络子系统的区别在于其和CPU架构和存储模型是息息相关的。内存管理到底是个什么意思？这里借用深入理解linux内核架构那本书对内存管理涵盖的领域概况：

内存中物理页的管理  
分配大块内存的伙伴系统（buddy）  
$\bullet$ 分配较小块内存的系统 slab、slub、slob  
$\bullet$ 分配非连续内存块vmalloc  
进程地址空间

# 1.1 物理内存的布局何时获得

在 x86 系统下，在系统还是实模式时就调用 bios 中断获取物理内存的布局了。在arch/x86/boot/main.c 函数中的 main.c 函数会调用 detect_memory()函数获取当前内存布局。调用 BIOS 的功能通常称为 e820，这是因为使用该获取内存布局功能时 ax 寄存的十六进制值是 0xe820。detect_memory()函数定义于 arch/x86/boot/memory.c 文件中。

static int detect_memory_e820(void)   
{ int count $= 0$ . struct biosregs ireg, oreg; struct e820entry \*desc $=$ boot_parameters.e820_map; static struct e820entry buf; /\* static so it is zeroed \*/ initregs(&ireg); ireg.ax $= 0\mathrm{x}e820$ . ireg.cx $=$ sizeof buf; ireg.edx $=$ SMAP; ireg.di $=$ (size_t)&buf; do { intcall(0x15,&ireg,&oreg); ireg.ebx $=$ oreg.ebx;/\* for next iteration... \*/ /\* BIOSes which terminate the chain with CF $= 1$ as opposed to $\% \mathrm{ebx} = 0$ don't always report the SMAP signature on the final, failing, probe. \*/ if (oreg.eflags & X86_EFLAGS_CF) break; /\* Some BIOSes stop returning SMAP in the middle of the search loop.We don't know exactly how the BIOS screwed up the map at that point, we might have a partial map, the full map, or complete garbage, so just return failure. \*/ if (oreg.eax != SMAP){

count $= 0$ break; } \*desc++ $=$ buf; count++; } while (ireg.ebx && count < ARRAY_SIZE.boot_parameters.e820_map)); return boot_parameters.e820_entries $=$ count;   
}

该函数的do while语句是一个遍历语句，其作用是遍历所有可用的物理内存段，并将物理内存段的个数(count 值)记录在 boot_params.e820_entries，boot_params 是系统启动的参数，注意这里是探测可用的物理内存。

为了从纷繁的内存管理代码细节中脱离出来，这里不会像网络子系统那部分那样逐行代码去分析，而侧重功能和管理方法的分析。图 1.1是 X86 32bit情况下的内存使用分布情况。

![](images/3f729f217a16bcb4c1ce6ba24619fa53aa8c99fac9b205806fa761a4e742beeb.jpg)  
图 1.1 linux 虚拟内存

虚拟内存到物理内存到物理内存的变换是由MMU完成的。内核空间的地址内容X86 虚拟机下dmesg导出的内核虚拟地址拓扑如下:

```txt
[ 0.000000] virtual kernel memory layout:  
[ 0.000000] fixmap : 0xffff14000 - 0xFFFFFF000 (940 kB)  
[ 0.000000] pkmap : 0xffc00000 - 0xffe00000 (2048 kB)  
[ 0.000000] vmalloc : 0xf83fe000 - 0xffbfe000 (120 MB)  
[ 0.000000] lowmem : 0xc0000000 - 0xf7bfe000 (891 MB)  
[ 0.000000] .init : 0xc19b9000 - 0xc1a93000 (872 kB)  
[ 0.000000] .data : 0xc1663132 - 0xc19b8200 (3412 kB)  
[ 0.000000] .text : 0xc1000000 - 0xc1663132 (6540 kB) 
```

由这上述信息可以看到，虚拟内存主要分为 lowmen、vmalloc、pkmap（persistent kernelmap）、fixmap这四个类型，对应会有它们各自的管理代码，后面会叙述。

在嵌入式情景下，有略微的差别。下面是一个 arm 嵌入式系统 dmesg 导出的内存拓扑：

```txt
[ 0.000000] Virtual kernel memory layout:  
[ 0.000000] vector : 0xffff0000 - 0xffff1000 (4 kB)  
[ 0.000000] fixmap : 0xfff00000 - 0xfffe0000 (896 kB)  
[ 0.000000] vmalloc : 0x86800000 - 0xfff00000 (1928 MB)  
[ 0.000000] lowmem : 0x80000000 - 0x86600000 (102 MB) 
```

```txt
[ 0.000000] modules: 0x7f000000 - 0x80000000 (16 MB)  
[ 0.000000] .text: 0x80008000 - 0x805c9f04 (5896 kB)  
[ 0.000000] .init: 0x805ca000 - 0x808cdeac (3088 kB)  
[ 0.000000] .data: 0x808ce000 - 0x8091e920 (323 kB)  
[ 0.000000] .bss: 0x8091e920 - 0x80965828 (284 kB) 
```

上述内存拓扑的一个特点是虚拟内存的地址空间可以达到4G而不需要考虑实际物理内存的大小，虚拟内存是内存管理环节中的一环，在认识 linux管理模型之前，了解 CPU架构对内存的影响还是挺有好处的。

# 1.2 CPU 架构

![](images/574caf1a055318ec358b695f0170fbfeb70e18d351e4be7db539ae17c35262c2.jpg)  
图 1.2 CPU 存储架构

图 1.2 可知，按 CPU 访问开销由低到高的存储类型依次是寄存器、L1cache、L2cache、L3cache以及主存。如果没有 cache，只有register和主存，这就意味着 CPU取指令或者数据时，CPU 将会等待数据或者指令将需要很长时间，这降低的 CPU 的利用率。如果和指令相关的操作或者和数据相关操作都存储在寄存器里，那么 CPU 取指令和数据的开销几乎可以忽略不计。

Reg-A、Reg-B：是两个寄存器组，它们的功能和地位是一样的，在实现超线程技术时，两个线程使用不同的寄存器组，这就意味着寄存器内容在线程切换时不需要保护。CPU运算核心访问它们的周期约为一个时钟周期。

L1d和 L1i，分别是数据和指令 cache，数据和指令分开存放，运算核心访问其约需4 个时钟周期。通常它的大小分别约32K。

L2cache：第二级缓存，为L1d和L1i共享，其容量通常是 L1d、L1i总和的4 倍。两个CPU 有其各自的 L2cache。访问约 12cycle。

L3cache：为第三级缓存，其容量通常是单个 L2的 128 倍，依据情况而不同，访问周期40cycle。

Cpu 运算核心访问 DDR 的周期约为 240cycle。

由cpu运算核心访问 cache 和DDR的时钟周期可知，cache在一定程度上缓解了慢速的主存和快速的CPU运算核心不匹配导致的效率变低的问题，但是也带来了内存管理的复杂性，这种复杂性由芯片设计人员和软件设计人员共同解决，芯片设计人员提供了TLB 和MMU（针对分页和分段，实模式不会启用MMU）两个功能模块，软件设计人员需要对这两个功能模块以及cache做些配置和管理。

MMU（memory management unit）的主要作用是虚拟地址转换成实地址。在图 1.1 中，虚拟内存到物理内存的转换就是由 MMU完成的。对内核而言， $\times 8 6$ 的虚拟地址到物理地址的转换关系是：

虚拟地址 $=$ 物理地址 $^ +$ PAGE_OFFSET

从上述的关系也可以得到物理地址，x86 上还有段地址、线性地址，段地址在 arm 上没有类似的概念。

Cache 部分的内容还挺多的，其原理可以参。考 http://en.wikipedia.org/wiki/CPU_cache

# 第二章 物理内存管理模型

如何管理常见的4G或者更大的内存条，当前CPU倾向于不再提高CPU的主频，而朝着多核的方向发展，所以内存模型进一步被复杂化，且分为一致性内存(UMA)和非一致性内存(NUMA)两种情况。对于 UMA 情况，CPU 访问任何一个存储空间的开销是等价的，而对于NUMA它们的开销则是不同的。

![](images/542f5e453e207d1651ee834a8d8c59c3838f775c60c7aef1a4616d7e5f41d7dd.jpg)

![](images/145b0d8a78d59224773ab42a27e091d149189d4ead91e5aae320a9ad04c46a58.jpg)  
图 2.1 UMA 和 NUMA 内存示意

图 2.1 是 UMA 和 NUMA 的示意，左边三个 CPU 访问内存节点 NODE0 的开销是一样的，而右侧访问NODE0的开销CPU1 是大于CPU2的。在后一种情况下，应该将CPU2 需要的指令和数据放在NODE0中，将CPU1 需要的指令和数据放在NODE2中，这样提高系统的效率。处理器核以及内存的两种布局对应两种内存管理模型，linux将UMA这种模型作为 NUMA这种模型的一个特例，当然混合型的也是 NUMA 的一个特例，对于混合型，只需看图2.1右侧的NUMA 模型，两颗CPU访问NODE1 的开销一样，而访问其它NODE节点的开销并不一样。

![](images/3b1e0b14c57d48569998916f4b665de0b77e83f72bfa492007ef58a803edb841.jpg)  
图2.2 内存管理模型

结合图2.2，分析一下 linux内存管理模型，假设图中左上角那根内存条到各个CPU的开销是一样的且整个系统只有这一根内存条，则可以认为该系统的内存模型退化为图2.1中左侧的UMA模型，该内存条使用 node表示，对应的其管理数据结构是 pg_data_t，该 node

又分为若干类型的zone，zone 的类型是有限的，根据架构不同而略有区别，对于IA-32 架构，有 ZONE_DMA，ZONE_NORMAL，ZONE_HIGHMEM 以及 ZONE_MOVABLE；这对应于图的中上部。

ZONE_DMA和早期的 ISA设备是有关系的；

ZONE_NORMAL指示的是可直接映射到内核段的地址空间；

ZONE_HIGH 是超出内核段的物理内存；

ZONE_MOVABLE 旨在防止物理内存碎片。

为了防止内存碎片，将每个 zone 又分为五种类型，放大镜将 ZONE_DMA 类型的 zone 进行了放大，其它类型的 zone 组织内存的方式和其一样，所以这里只对 ZONE_DMA 进行分析，每个zone 采用了反碎片技术，这一技术有别于磁盘的碎片整理，后者依赖于文件系统，将零散的小块合并成连续的大块，而反碎片技术从源头上阻止碎片的产生；依据内存使用的方式，将 zone 分为 unmovable,reclaimable, movale,reserve 类型。分别对应于不可移动，可回收，可移动，保留备用（紧急情况下使用）。

ZONE_DMA 有 16M 大小，ZONE_NORMAL 上界是 896M，如何管理这么多的且变化的内存，仅仅分成五种类型还是不够的，linux使用了页管理技术，每4KB 作为一个进行管理，此外还有一种 4MB 的大页，以提高 TLB 命中的效率。先不考虑大页，仅仅 16M 的存储空间就对应4096页，对于896M，将有229376 个页，如何高效的管理这些页？linux采用了伙伴系统方法，这一方法已经包含在图2.2放大镜图像里了。Buddy system的核心思想是：

根据使用有些情况需要一个页4KB 就可以了，也有需要8KB，依次类推可能需要1M等等，既然如此何不将4KB页合并成8KB，8KB 的页合并成16KB，16KB合并成32KB，这样管理也方便。

图2.2 中的0、1…11 的意识是指2的0 次方、1次方…11次方，2 的0次方等于1，表示该链表所有员的大小是一个页4KB，2的1次方等于2，表示该链表的所有成员大小是8KB，依次类推，这个组织也可以参看图2.3，这样需要多大的内存就到哪个内存链表上去取（还要根据zone类型和反碎片类型）。

单单只有 buddy 还有一个问题，就是在需要 2 字节的内存时，去申请一个页大小的内存4KB，实在有点浪费，所以 linux引入slab管理方法，针对嵌入式和服务器又提出了 slob和slub管理模型，不过到此可以知道其实内存管理的核心的数据结构是node和 zone。

一个具有NUMA的 X86-64系统的实例如下：

<table><tr><td colspan="15">$ cat /proc/pagetypeinfo</td></tr><tr><td colspan="15">Page block order: 9</td></tr><tr><td colspan="15">Pages per block: 512</td></tr><tr><td colspan="3">Free pages count per migrate type at order</td><td>0</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>8</td><td>9</td><td>10</td><td></td></tr><tr><td>Node</td><td>0, zone</td><td>DMA, type</td><td>Unmovable</td><td>1</td><td>0</td><td>1</td><td>0</td><td>2</td><td>1</td><td>1</td><td>0</td><td>1</td><td>0</td><td>0</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA, type</td><td>Reclaimable</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA, type</td><td>Movable</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>3</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA, type</td><td>Reserve</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>1</td><td>0</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA, type</td><td>Isolate</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA32, type</td><td>Unmovable</td><td>276</td><td>395</td><td>221</td><td>1</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA32, type</td><td>Reclaimable</td><td>27246</td><td>21166</td><td>8671</td><td>70</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA32, type</td><td>Movable</td><td>3</td><td>8</td><td>11</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA32, type</td><td>Reserve</td><td>0</td><td>0</td><td>0</td><td>11</td><td>1</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>Node</td><td>0, zone</td><td>DMA32, type</td><td>Isolate</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td></tr></table>

![](images/258eeeb14d4eb28f06784201ddb73d38570550f93a6cad31845adde34c52d301.jpg)

# 2.1 Node

Linux 使用 pg_data_t 管理内存模型，

![](images/3bb296d2387512a05f1d56912151f8230f5774cd59a305cd0690f58d26c3a4a6.jpg)  
图2.3 内存管理数据拓扑

在图2.1中，根据CPU访问内存的代价不一样，将每一个代价节点抽象成一个 node，对于NUMA退化为一个 node，需要注意的node节点数并不一定对应于内存条的个数。每一个node 节点通常分为若干的域(zone)。

```txt
enum zone_type {
    ZONE_DMA,
    ZONE_DMA32,
    ZONE_NORMAL,
} 
```

```txt
ZONE_HIGHMEM, ZONE_MOVABLE, MAX_NR_ZONES }; 
```

因为并不是所有内存都支持 DMA操作，所以这里专门开辟了一个称为 ZONE_DMA 类型的域以支持DMA操作，这块域大小依赖于体系结构，在 i386 上其小于16M，这在图 1.1 中物理内存有所显示。

ZONE_DMA32是针对 x86架构64 为处理器而准备的，因为其可以使用的DMA范围由16M 拓展到了 4G。

对于 i386 ZONE_NORMAL 的上限就是 896M，是这部分内存是直接映射的。

对于 i386 ZONE_HIGHMEM 在 ZONE_NORMAL 之后，这部分的内存页是动态映射的，主要是因为页表项有限，其基本思想是需要高端内存时，申请页表进行映射，用完释放再回收页表。

```c
include/linux/mmzone.h>   
700 typedef struct pglist_data{   
/\*内存管理类型，ZONE_DMA、ZONE_NORMAL、ZONE_HIGH 701 struct zone node Zones[MAX_NR_ZONES]; 
```

/*每次内存申请会落到 zonelist 上，zonelist 是 zone（区）的列表，对于分配内存，第一个区是“全局”的，其它的 zone 是后备 zone，后备 zone 按优先级递减排序，这里的优先级是指访问的代价，代价越大越靠后。对于 UMA，node_zonelists 只有一个成员，而对于 NUMA，MAX_ZONELISTS 的值是 2,[0]是有后备 zone的 zonelist，而[1]没有后备 zone，用于 GFP_THISNODE*/

```c
702 struct zoneList node_zonelists[MAX_ZONELISTS];  
/*701行实际的node Zones成员数*/  
703 int nrzones;  
/*非稀疏内存管理模型时，指向struct page中的第一个页面，其存放在mem_map数组中*/  
704 #ifdef CONFIG_FLATNode_MEM_MAP/\*means！SPARSEMEM\*/  
705 struct page \*node_mem_map;  
/*Page Cgroup,可看做mem_map的扩展，该结构体用于确定cgroup，LXC用到cgroup和命名空间*/  
706 #ifdef CONFIG_MEMCG  
707 struct page_cgroup \*node_page_cgroup;  
708 #endif  
709 #endif  
/*指向内存引导程序*/  
710 #ifdef CONFIG_NO_BOOTMEM  
711 struct bootmem_data\*bdata;  
712 #endif  
713 #ifdef CONFIG MEMORY HOTPLUG  
714 /\*  
715 \*Must be held any time you expect node_start_pfn,node_present_pages  
716 \*or node_spanned_pages stay constant.Holding this will also  
717 \*guarantee that any pfn_valid() stays that way.  
718 \*  
719 \*Nests above zone->lock and zone->size_seqlock.  
720 \*/ 
```

```c
721 spinlock_t node_size_lock;  
722 #endif  
/*起始页帧号，对于UMA，该值是0，对于NUMA，该值随节点不同而不同，node_start_pfn全局唯一，由页帧号全局唯一性决定*/  
723 unsigned long node_start_pfn;  
724 unsigned long node_present_pages; /*total number of physical pages，针对该node而言的总数*/  
725 unsigned long node_spanned_pages; /*total size of physical page  
726 range, including holes */  
//具有全局性，对于UMA，该值是1，对于NUMA从0开始计数  
727 int node_id;  
728 nodemask_t reclaim_nodes; /*Nodes allowed to reclaim from */  
/*swap dameon*/  
729 wait_queue_head_t kswap_wait;  
730 wait_queue_head_t pfmemalloc_wait;  
731 struct task_struct *kswap; /*Protected by lock_memory.hotplug() */  
/*定义需要释放的区域长度*/  
732 int kswap_max_order;  
733 enum zone_type classzoneidx;  
747}pg_data_t; 
```

# 2.2 Zone 结构

Zone的类型如下，对于 x86， ZONE_HIGH 是896M以上的空间，不能直接映射。

```c
enum zone_type {
    #ifdef CONFIGZone_DMA
        ZONE_DMA,
    #endif
    #ifdef CONFIGZone_DMA32
        ZONE_DMA32,
    #endif
        ZONE_NORMAL,
    #ifdef CONFIG_HIGHMEM
        ZONE_HIGHMEM,
    #endif
        ZONE_MOVABLE,
        __MAX_NR Zones
}; 
```

# Zone 结构体

```c
313 struct zone {
/*WMARK_MIN，WMARK_LOW，WMARK_HIGH，用于标记各阈值，影响 swap_dameon 的行为。
*WMARK_HIGH：内存不紧张。
*WMARK_LOW：内存有点紧张，需要将内存页换到外部存储设备（硬盘、SSD）
*WMARK_MIN：内存的最低阈值，这是回收页压力增大
由*_wmark_pages(zone)宏存取该字段
```

```c
/*  
317 unsigned long watermark[NR_WMARK];  
318  
319 /*  
320 * When free pages are below this point, additional steps are taken  
321 * when reading the number of free pages to avoid per-cpu counter  
322 * drift allowing watermarks to be breached  
323 */  
324 unsigned long percpu_drift_mark;  
325  
326 /*每个内存区保留的内存页数。通过sysctl_lowmem_reserved_ratio sysctl可以改变。*/  
334 unsigned long lowmem_reserved[MAX_NR_ZONES];  
335  
336 /*  
337 * 这是每个zone都会保留的页，本身和dirty没什么关系。  
339 */  
340 unsigned long dirty_balance_reserved;  
341/*每个CPU的冷热页帧列表，在高速缓存中的页称为“热”，不在高速缓存中的页称为“冷”*/  
350 struct per_cpu_pagesset __percpu *pageset;  
351 /*  
352 * 释放内存时使用该锁，防止并发  
353 */  
354 spinlock_t lock;  
/*标志是否所有也都不可回收*/  
355 int all_unreclaimable;  
/*buddy系统核心数据结构，相见后面*/  
368 struct free_area free_area[MAX_ORDER];  
/*对称多处理器情况下，可能对该结构的不同部分访问，ZONEADDING是按缓存行大小填充对其，这样并发访问该结构体时，可以通过访问两个缓存行以提高速度*/  
389 ZONE=PADDING(_pad1_)  
390  
391 /*这些是页回收扫描器使用的字段*/  
392 spinlock_t lru_lock;  
393 struct Iruvec Iruvec;  
394  
395 unsigned long pages_scanned; /*since last reclaim*/  
396 unsigned long flags; /*zone flags, see below*/  
397  
398 /* Zone统计，percpu_drift_mark会和这里的统计的空闲也对比，以避免per-CPU计数器漂移*/  
399 atomic_long_t vm_stat[NR_VMZone_STAT_ITEMs];  
400  
401 */  
402 *该zone的LRU链表上的活跃匿名页和非活跃匿名页比例
```

404 \*/   
405 unsigned int inactive_ratio;   
406   
407   
408 ZONE_PULLING(_pad2_)   
409 /* Rarely used or read-mostly fields */   
410   
411 \*/   
412 \* wait_table -- the array holding the hash table   
413 \* wait_table_hash_NR_entries -- the size of the hash table array   
414 \* wait_table_bits -- wait_table_size == (1<< wait_table_bits)   
434 \*/   
435 wait_queue_head_t \* wait_table;   
436 unsigned long wait_table_hash(nr_entries;   
437 unsigned long wait_table_bits;   
438   
439 \*/   
440 \* 该 node节点所属的node节点   
441 \*/   
442 struct pglist_data \*zone_pgdat;   
443 /* zone_start_pfn $= =$ zone_start_paddr >> PAGE_SHIFT，将物理地址转换为起始页帧号\*/   
444 unsigned long zone_start_pfn;   
445   
446 \*/   
447 \* spanned_pages is the total pages spanned by the zone, including   
448 \* holes, which is calculated as:   
449 \* spanned_pages = zone_end_pfn - zone_start_pfn;   
450 \*   
451 \* present_pages is physical pages existing within the zone, which   
452 \* is calculated as:   
453 \* present_pages = spanned_pages - absent_pages(pages in holes);   
454 \*   
455 \* managed_pages is present pages managed by the buddy system, which   
456 \* is calculated as (reserved_pages includes pages allocated by the   
457 \* bootmem allocator):   
458 \* managed_pages = present_pages - reserved_pages;   
459 \*   
460 \* So present_pages may be used by memory hotplug or memory power.   
461 \* management logic to figure out unmanaged pages by checking   
462 \* (present_pages - managed_pages). And managed_pages should be used   
463 \* by page allocator and vm scanner to calculate all kinds of watermarks   
464 \* and thresholds.   
465 \*   
466 \* Locking rules:

```c
467 \*   
468 \* zone_start_pfn and spanned_pages are protected by span_seqlock.   
469 \* It is a seqlock because it has to be read outside of zone->lock,   
470 \* and it is done in the main allocator path. But, it is written   
471 \* quite infrequently.   
472 \*   
473 \* The span_seq lock is declared along with zone->lock because it is   
474 \* frequently read in proximity to zone->lock. It's good to   
475 \* give them a chance of being in the same cacheline.   
476 \*   
477 \* Write access to present_pages and managed_pages at runtime should   
478 \* be protected by lock_memory.hotplug(/unlock_memory.hotplug().   
479 \* Any reader who can't tolerant drift of present_pages and   
480 \* managed_pages should hold memory hotplug lock to get a stable value.   
481 \*/   
482 unsigned long spanned_pages;   
483 unsigned long present_pages;   
484 unsigned long managed_pages;   
485   
486 /\*   
487 \* rarely used fields:   
488 \*/   
489 const char \*name;   
490}\_cacheline_internodealigned_in_smp; 
```

# 2.3 page 结构

每一个物理页都有一个struct page 与之关联，以跟踪页使用情况，

```txt
41 struct page {  
42 /* First double word block */  
43 unsigned long flags;  
45 struct address_space *mapping;  
52 /* Second double word */  
53 struct {  
54 union {  
55 pgoff_t index; /* 在映射页的偏移*/  
56 void *freelist; /* slab/slob 第一个空闲对象 */  
57 bool pfmemalloc; /*如果该标识由页分配器设置，则ALLOC_NO_WATERMARKS也被设置并且空闲内存量不满足 low 水印，这意味着内存有点紧张，调用者必须确保该页用于释放其它页之用。
```

```txt
66 };   
67   
68 union {   
74 \*/   
75 \* Keep_count separate from slab cmpxchg-double data. 
```

```c
76 \*As the rest of the double word is protected by  
77 \*slab_lock but_count is not.  
78 \*/  
79 unsigned counters;  
82 struct{  
83union{  
84union{  
86 //页表中指向该页的页表入口项（PTE）数，也被用作复合页的尾部页引  
用计数  
101 atomic_t_mapcount;  
102  
103 struct{//*SLUB*/  
104 unsigned inuse:16;//在使用的slub对象  
105 unsigned objects:15;//slub对象数  
106 unsigned frozen:1;  
107 };  
108 int units; /* SLOB */  
109 };  
110 atomic_t_count; /*使用计数器，为0则说明没有被内核引用，将可能  
被释放*/  
111 };  
112 };  
113 };  
114  
115 /* Third double word block */  
116 union{  
117 struct list_head lru; /*换出页列表，由zone->lru_lock锁保护*/  
120 struct{//*slub per cpu partial pages*/  
121 struct page *next; /*Nextpartial slab*/  
126 short int pages;  
127 short int pobjects;  
129 };  
130  
131 struct list_head list; /*slobslist of pages*/  
132 struct slab *slab_page; /*slab fields*/  
133 };  
134  
135 /*Remainder is not double word aligned*/  
136 union{  
137 unsigned long private; /*映射私有，用途自定，通常如果设置PagePrivate标志，  
则用于buffer_heads，如果设置PageSwapCache，则用于swp_entry_t，如果设置PG_buddy，则用于伙伴系  
统*/  
147 struct kmem_cache\*slab_cache; /*SL[AU]B: Pointer to slab*/  
148 struct page\*first_page; /* Compound tail pages*/
```

flags 相关的定义在 include/linux/page-flags.h 文件中，flag 用于指示该页是否被锁住、是否是脏页，是否是活动的等，该文件中还定义了一些存取该变量的宏和函数，该标志在某些情况下是可能发生异步更新的。

*mapping如果其最低比特是 0，则其指向inode（信息节点，文件系统相关）地址空间或者是NULL。如果映射成匿名内存，最低比特被置位并且其指向 anon_vma对象。

# 第三章内存初始化

在使用内存时，需要知道内存的一些信息，比如物理内存到底有多大？内存是不是分节点的（访问的代价不一样）？启用分页情况下的页表设置情况？内存相关的初始化工作在start_kernel 函数完成，下列列出了内存相关的函数。

![](images/258c85287f762ddc79010b52e4d4b87a2b5b2f43073e3206d8860b150346598b.jpg)  
图 3.0.1 内存初始化相关函数调用

Build_all_zonelists 的作用是处理系统 node 间 zone 的备用关系，即如果有 node 0，node1，node2，如果 node0 中的某个类型的 zone 申请内存，发现内存不足，去该节点的其它类型zone 也没有，这是向 node1 还是 node2 申请，这就牵涉到 node1 的开销和 node2 的开销大小以及去哪个备用zone 申请，该函数即完成此工作。

Mm_init停用boot内存分配器，启用伙伴内存管理。

进入 setup_arch 函数，不论是 arm 还是 x86，映入眼帘的是_text，_etext，_edata，_end，bss_stop 这些变量。这些变量定义在 arch/x86/kernel/vmlinux.lds.S 文件，类似的将 $\times 8 6 =$ 换成 arm 也能看到 arm 的连接脚本里对这些变量的定义，这些变量的值在链接时才确定，具体来看，有一个 System.map 文件，通常和 vmlinx 在同一个文件夹，在 System.map 文件里可以找到上述变量的定义。该文件有如下几行，随编译结果不同而有区别：

```txt
c1000000 T_text  
c1000000 T startup_32  
c10000e0 t bad_subarch  
c10000e0 W lguest_entry  
c10000e0 W xen_entry  
c10000e4 T start_cpu0  
c10000f8 T_stext 
```

c1000000 即为 $3 G \substack { + 1 6 M }$ ，这从一个方面验证了内核的代码段确实是从 $3 G \substack { + 1 6 M }$ 开始的，这里的地址是虚拟地址。对于内核在物理内存中的分布情况，可以看/proc/iomem文件，该文件的一部分内容摘录如下：

```txt
00000000- 00000fff : reserved  
00001000- 0009eff : System RAM  
0009f000- 0009ffff : reserved  
000a0000- 000bfff : PCI Bus 0000:00  
000a0000- 000bfff : Video RAM area 
```

```txt
000c0000- 000c7fff : Video ROM  
000ca000- 000cbfff : reserved  
000ca000- 000caffff : Adapter ROM  
00100000- 7fedffff : System RAM  
01000000- 01663131 : Kernel code  
01663132- 019b81ff : Kernel data  
01a9b000- 01b81fff : Kernel bss 
```

从上面可以看到内核代码从 1M地方开始存放，前4k 作为一个单独的页，其后的640KBIOS和显卡会使用这段区域，从 640K 到 1M 是 ROM 区域，所以 linux 内核选择从 1M 开始处连续存放。Setup_arch 主要完成以下工作：

![](images/d027fded0a04390eb6d22d9f24e5727a04a6201295bc5f127636a21cc5be8956.jpg)  
图3.0.2 和内存相关的内存初始化

页表初始化会在 setup_arch 函数完成，kernel_physical_mapping_init 用于虚拟地址到物理地址的映射，和页表息息相关，不过在分析页表初始化过程之前先来点linux分页基础，至于分段arm上并不存在这一概念，所以这里直接略过了。

# 3.1 Linux 分页

分页单元将线性地址转换成物理地址，该分页单元将检查访问的物理页是否有效，无效会产生缺页异常通知操作系统。X86上当 CR0的PG位被置位时，则启用分页功能，否则，线性地址就是物理地址。现在的Linux采用了四级分页模型，四级页目录分别是：

页全局目录(Page Global Directory)

页上级目录（Page Upper Directory）

页中间目录（Page Middle Directory）

页表（Page Table）

X86-64 采用了四级页表，启用 PAE特性的x86-32 也此采用了四级页表，对于未启用 PAE特性的32位系统，看一个三级页表映射到物理地址的过程：

32比特按 页目录项10bit， 页表10bit，页内偏移 12bit，总比特数是 $1 0 + 1 0 + 1 2 = 3 2$ ，符合32位系统划分，一个页的大小是4KB，要在这4KB大小里任意寻址，则需要12bit的地址，这正是页内偏移为12bit的原因。线性地址到物理地址的转换分为两步，首先根据线性地址高10比特找到页目录项，然后根据页目录项和线性地址的中间 10比特找到页表，最后的12bit用于在页表中寻址，这一过程如下：

![](images/43ba1c440d25bc3d4ef3339304a849391411edf6fdbca71655fece6969ec21be.jpg)  
图 3.1.1 80x86 处理器分页

假设一个线性地址范围是 $0 { \times } 2 0 0 0 { \_ } 0 0 0 0$ 到 $0 \times 2 0 0 3 .$ _ffff，该地址对应的空间是用户空间，内核从 3G 开始，这一段包括 64（ $0 \times 2 0 0 3 .$ _ffff- $\cdot 0 { \times } 2 0 0 0 _ { - } 0 0 0 0 + 1 = 0 { \times } 4 _ { - } 0 0 0 0 ;$

$0 \times 4 \_ 0 0 0 0 / 1 0 2 4 = 2 5 6 \times 8 ; 2 5 6 \times 8 / 4 \times 8 = 6 4$ ）个页，

页目录项是线性地址最高10比特，对应的就是 $\scriptstyle 0 \times 0 8 0$ ，和 CR3（不同进程CR3里的值不同）里的基地址值相加得到页表项，同理得到的页目录项作为页表的基地址，取线性地址接下来的10比特和页表项相加得到页的基地址，根据偏移量找到对应的比特，这就是这个查找过程。四级过程类似。

内存容量的物理限制源于芯片的地址总线和数据总线的位宽，从奔腾pro开始，芯片的数据总线从32位被扩展到36比特，这样实际可以访问的物理内存由先前的4GB扩展到64GB。而这一特性正是 PAE 特性。另外一个 PSE（Physical Address Extension）从奔三引入。对于 64比特系统，IA64采用了三级分页（不包括页内偏移），而 x86_64使用了四级分页技术。

对于32 位系统，通过将页上级目录和页中间目录设置成0，实际上加0等于什么也没有加。但是为了兼容32位和 64位系统页上级目录和页中间目录还是一直存在的。

线性地址被划分成如下部分，定义在<pgtable-3level-types.h>/<pgtable-2level-types.h>；PAGE_SHIFT：12比特，用于找到页表起始处。

PMD_SHIFT：页内偏移和页表的总比特数，用于找到页中间目录项，当未启用 PAE 时，其值为22（12 位页内偏移以及10 位页表项），当 PAE 启动时其值为21（12 位页内偏移以及9位页表项）。

PUD_SHIFT：用于查找页上级目录，x86 上，其值等于 PMD_SHIFT。

PGDIR_SHIFT：页全局目录的起始地址处，未启用 PAE 时 PGDIR_SHIFT 的值是 22，启用PAE时其值时30（12比特页内偏移，9比特页表长度，9 比特页中间目录长度）。

PTRS_PER_PTE、PTRS_PER_PMD、PTRS_PER_PUD、PTRS_PER_PGD 用于计算各入口项的总数。当PAE未启用时，它们值为 1024、1、1 以及1024，而启用PAE特性时，值为512,512,1和4。

# 3.2 Setup_arch

接着图 3.0.2，在系统启动时 PAE 特性并未启用，所以页表退化为两级页表，临时页全局目

录存放在 swapper_pg_dir 里，swapper_pg_dir 实际存放在 bss 段，在链接时才会确定其地址，但是该变量的值可以看编译生成的 System.map 文件。

```txt
c19ff000 B_bss_start  
c19ff000 R_smp_locks_end  
c19ff000 b initial_pg_pmd  
c1a00000 b initial_pg_fixmap  
c1a01000 B empty_zero_page  
c1a02000 B swapper_pg_dir 
```

未启用 PAE 时，PAGE_SHIFT：12；PMD_SHIFT：22；PGDIR_SHIFT：22；

![](images/9411b89f5536f22860a652ee5c8ebffcfcf8237b5328b388d81c495be2a94847.jpg)  
图 3.2.1 和内存相关的内存初始化

Init_mem_mapping映射低端内存区，首先映射ISA 区域而不管ISA 区是否存在内存空洞，然后映射剩下的内存区，max_low_pfn 记录的就是低端内存区的最后一个页帧号，根据其大小，按照 4MB 对其的方式，循环映射完低端内存。而 early_ioremap_page_table_range_init 负责固定内存映射，图 1.1 中的 fixmap 部分，load_cr3 操作启用分页机制，而 flush 操作是保持cache 的一致性。

# 3.2.1 低端内存映射初始化

![](images/4e6e391a8d41ff7e0d021481d2f2e11b9434a23443165a1795278f4e2e64f074.jpg)  
图 3.2.2 init_mem_mapping 流程

由于ISA 以及其它区域建立映射过程类似，这里剖析 ISA 区域的映射过程：

建立物理内存的直接映射。

//mr[0] .start $= 0$ ; mr[0].end $= 2 5 6 < < 1 2$ ; mr[0].page_size_mask $_ { = 0 }$ , 对应于该函数的三个参数。

```c
unsigned long __init  
kernel_physicalmapping_init(unsigned long start, unsigned long end, unsigned long page_size_mask)  
{ int use_pse = page_size_mask == (1<<PG_LEVEL_2M); unsigned long last_map_addr = end; unsigned long start_pfn, end_pfn; pgd_t *pgd_base = swapper_pg_dir; int pgdidx, pmdidx,pte_ofs; unsigned long pfn; pgd_t *pgd; pmd_t *pmd; pte_t *pte; unsigned pages_2m, pages_4k; int mapping_iter; start_pfn = start >> PAGE_SHIFT; //起始页帧号0 end_pfn = end >> PAGE_SHIFT; //结束的页帧号256 mapping_iter = 1; repeat: pages_2m = pages_4k = 0; pfn = start_pfn; //起始页帧号保存 //页全局目录索引，PAGE_OFFSET是0Xc000_0000,对应pgdidx是0xc00即768，其意义是内核在全局目录项映射的第一个项索引是第768项。 pgdidx = pgd_index((pfn<<PAGE_SHIFT) + PAGE_OFFSET); //页全局目录项基地址+索引 pgd = pgd_base + pgdidx; //知道页全局目录项有1024项，这里要对这1024项进行初始化。 for(; pgdidx < PTRS_PER_PGD; pgd++, pgdidx++) { pmd = one_md_table_init(pgd); //对于未启用PAE机制，则pmd项只有一项, pmdidx = 0; //PAE未启用时，该pmd项的索引值是0，唯一的。 for(; pmdidx < PTRS_PER_PMD && pfn < end_pfn; pmd++, pmdidx++) { unsigned int addr = pfn * PAGE_SIZE + PAGE_OFFSET; //该页帧对应的线性地址，偏移量是3G。 pte = one_page_table_init(pmd); //获得一个页表项。 pte_ofs = pte_index((pfn<<PAGE_SHIFT) + PAGE_OFFSET); //获得页表项的索引。 
```

pte $+ =$ pte_ofs;   
//遍历页表 for(；pte_ofs $<$ PTRS_PER_PTE&&pfn $<$ end_pfn; pte++,pfn++,pte_ofs++,addr $+ =$ PAGE_SIZE){ pgprot_t prot $=$ PAGE_KERNEL; /\* \*first pass will use the same initial \* identity mapping attribute. \*/ pgprot_t init_prot $=$ _pgprot(PTE identitiesATTR); if(is_kernel_text(addr))//内核代码段，则具有可执行权限 prot $=$ PAGE_KERNEL_EXEC; pages_4k++; //记录初始化的4KB页数量   
/将pte和pfn关联/ if (mapping_iter $= = 1$ ){ set_pte(pte,pfn_pte(pfn,init_prot)); last_map_addr $=$ (pfn<<PAGE_SHIFT) $^+$ PAGE_SIZE; } else set_pte(pte,pfn_pte(pfn,prot)); 1 } 1 if (mapping_iter $= = 1$ ) { /\* \*update direct mapping page count only in the first \*iteration. \*/ update_page_count(PG_LEVEL_2M，pages_2m); update_page_count(PG_LEVEL_4K，pages_4k); /\* \*local global flush tlb,which will flush the previous \* mappings present in both small and large page TLB's. \*/ __flush_tlb_all(); /\* \*Second iteration will set the actual desired PTE attributes. \*/ mapping_iter $= 2$ . goto repeat;

```txt
return last_map_addr; } 
```

# 3.2.2高端内区存固定映射初始化

高端内存域的初始化过程和低端内存初始化类似，但是只分配了页表，对应的 PTE 项并没有被初始化，初始化工作留到 set_fixmap()函数建立相关页表和物理内存的关联。固定映射区分为几种索引类型，索引类型由枚举变量 enum fixed_addresses 定义，该初始化工作就是初始化这段区域。

![](images/a2881ad884969f524602334479acc81efae25a699ca6fbc5a3478912361dd0b9.jpg)  
图 3.3.3

一个索引暂用一个 4KB 的页框，固定映射区的结束地址是 FIXADDR_TOP，即 0xfffff000(4G-4K)，见图 1.1。

```c
static void __init  
page_table_range_init(unsigned long start, unsigned long end, pgd_t *pgd_base)  
{  
    int pgd_idx, pmd_idx;  
    unsigned long vaddr;  
    pgd_t *pgd;  
    pmd_t *pmd;  
    pte_t *pte = NULL;  
    unsigned long count = page_table_range_init_count(start, end);  
    void *adr = NULL;  
    if (count)  
        adr = alloc_low_pages(count);  
    vaddr = start;  
    pgd_idx = pgd_index(vaddr);  
    pmd_idx = pmd_index(vaddr);  
    pgd = pgd_base + pgd_idx;  
//遍历固定映射区，建立页表项  
for ( ; (pgd_idx < PTRS_PER_PGD) && (vaddr != end); pgd++, pgd_idx++) {  
        pmd = one_md_table_init(pgd);  
        pmd = pmd + pmd_index(vaddr);  
        for ( ; (pmd_idx < PTRS_PER_PMD) && (vaddr != end); pmd++, pmd_idx++) {  
            //建立PTE项，并检查vaddr是否对应内核临时映射区，若是则重新申请一个页表来保存PTE项。  
            pte = page_table_kmap_check(one_page_table_init(pmd), 
```

pmd，vaddr，pte,&adr); $\mathrm{vaddr + = PMD\_SIZE}$ } $\mathrm{pmd\_idx} = 0$ 1

# 3.2.3 persistent 内存初始化

图 1.1 中只剩下的是 persistent memory 初始化了。这一工作由图 3.2.1 中的 paging_init 完成。该函数还完成了解除虚拟内核0 地址页的映射关系，这就是 NULL 指针所在的区域，用于异常捕捉。

```txt
<arch/x86/mm/init_32.c>   
void __init paging_init(void)   
{ pagetable_init(); _flush_tlb_all(); kmap_init(); /\* \* NOTE: at this point the bootmem allocator is fully available. \*/ olpc_dt_build_devicetree(); sparse_memory_present_with.active_regions(MAX_NUMNODES); sparse_init(); zone_sizes_init();   
} 
```

Persistent memory 页表在 pagetable_init 函数分配，映射在 kmap_init 完成。

```c
static void __init permanent_kmaps_init(pgd_t *pgd_base)  
{  
    unsigned long vaddr;  
    pgd_t *pgd;  
    pud_t *pud;  
    pmd_t *pmd;  
   pte_t *pte;  
    vaddr = PKMAP_BASE;  
//为 persistent 内存分配页表  
page_table_range_init(vaddr, vaddr + PAGE_SIZE*LAST_PKMAP, pgd_base);  
pgd = swapper_pg_dir + pgd_index(vaddr);  
pud = pud_offset(pgd, vaddr);  
pmd = pmd_offset(pud, vaddr);
```

```c
pte = pte_offset_kernel(pmd, vaddr);
pkmap_page_table = pte; //persistent页表项保存
}  
static void __init kmap_init(void)  
{  
    unsigned long kmap_vstart;  
    /* Cache the first kmap pte: */  
    kmap_vstart = __fix_to_virt(FIX_KMAP_BEGIN); //persistent memory的物理地址到虚拟地址转换  
    kmap_pte = kmap_get_fixmap_pte(kmap_vstart); //获得固定映射区的临时映射页表项的起始项  
    kmap PROT = PAGE_KERNEL;  
} 
```

# 3.3 per-CPU area 初始化

在图 3.0.1 中，setup_per_cpu_areas 用于初始化 per-CPU 区域，将.data.percpu 中的数据拷贝到每个 cpu 的数据段，在 SMP 情况下，per-CPU 可以提高并发性，是免锁算法的一种，有利于提高系统性能，一个经典的应用是：

网卡接收到数据包存在若干个队列中，队列个数和 CPU的个数是一样的，这样多个 CPU可以并发访问各自的接收队列而不需要锁。

# 3.4 节点（node）和域（zone）初始化

在图 3.2.1 中，提到了 native_pagetable_init 函数，该函数将剩下页表的映射工作完成。

![](images/1d2592d56f998554e8f746dbee97ca7a110e1723f4f34f5064eaf820303418d6.jpg)  
图3.3 节点初始化

对于node、zone和 page的关系，从图2.2 可以看出，这里涉及带代码分析如何完成这种内存管理拓扑的。zone_sizes_init 函数还是很好理解的。

```txt
573void__initzone_sizes_init(void)   
574{ 
```

```c
575 unsigned long max-zone_pfns[MAX_NR_ZONES];  
576  
577 memset(max-zone_pfns, 0, sizeof(max-zone_pfns));  
578  
579 #ifdef CONFIGZone_DMA  
580 max-zone_pfns[ZONE_DMA] = MAX_DMA_PFN; // MAX_DMA_PFN=16M  
581 #endif  
582 #ifdef CONFIGZone_DMA32//ia32未定义  
583 max-zone_pfns[ZONE_DMA32] = MAX_DMA32_PFN;  
584 #endif  
585 max-zone_pfns[ZONE_NORMAL] = max_low_pfn; //max_low_pfn低端页帧的上限  
586 #ifdef CONFIG_HIGHMEM  
587 max-zone_pfns[ZONE_HIGHMEM] = max_pfn; //最大页帧号，用于高端内存  
588 #endif  
589  
590 free_area_init_nodes(max-zone_pfns);  
591} 
```

free_area_init_nodes 的参数是 zone 数组，其作用是初始化每一个 node 节点，在 mm/page_alloc.c 文件定义了两个局部全局变量：

static unsigned long __meminitdata arch_zone_lowest_possible_pfn[MAX_NR_ZONES]; static unsigned long __meminitdata arch_zone_highest_possible_pfn[MAX_NR_ZONES]; 这两个变量用于标记每一个 zone的起止边界页帧号。

```c
5043 void __init free_area_init_nodes(unsigned long *max-zone_pfn)  
5044 {  
5045 unsigned long start_pfn, end_pfn;  
5046 int i, nid;  
5047  
5048 /* 5048~5068 确定每一个 zone 类型的起止页帧号 */  
5049 memset(arch-zone_lowestossible_pfn, 0, sizeof(arch-zone_lowestossible_pfn));  
5050 memset(arch-zone_highestossible_pfn, 0, sizeof(arch-zone_highestossible_pfn));  
5053 arch-zone_lowestossible_pfn[0] = find_min_pfn_with.active_regions();  
5054 arch-zone_highestossible_pfn[0] = max-zone_pfn[0];  
5055 for (i = 1; i < MAX_NR_ZONES; i++) {  
5056 if (i == ZONE_MOVABLE)  
5057 continue;  
5058 arch-zone_lowestossible_pfn[i] =  
5059 arch-zone_highestossible_pfn[i-1];  
5060 arch-zone_highestossible_pfn[i] =  
5061 max(max-zone_pfn[i], arch-zone_lowestpossible_pfn[i]);  
5062 }  
5063 arch-zone_lowest Possible_pfn[ZONE_MOVABLE] = 0;  
5064 arch-zone_highestPossible_pfn[ZONE_MOVABLE] = 0; 
```

```c
5065  
5066 /\*Find the PFNs that ZONE_MOVABLE begins at in each node \*/  
5067 memset(zone movable_pfn, 0, sizeof(zone movable_pfn));  
5068 find-zone movable_pfns_for_nodes();  
5100 /\*初始化每一个 node \*/  
//遍历每一个 node  
5103 for_each_online_node(nid) {  
/*定义于include/linux/mmzone.h,对于单一节点，NODE_DATA定义如下：  
#define NODE_DATA(nid) (&contig_page_data), 5104行的node节点  
*/  
5104 pg_data_t *pgdat = NODE_DATA(nid);  
5105 free_area_init_node(nid, NULL,  
5106 find_min_pfn_for_node(nid), NULL);  
5107  
5108 /\*如果node上存在页，则将其设置该node有内存状态为N_MEMORY，表示该node上有可用的内存。\*/  
5109 if (pgdat->node_present_pages)  
5110 node_set_state(nid, N_MEMORY);  
5111 check_for_memory(pgdat, nid);  
5112 }  
5113} 
```

5015 行的 free_area_init_node 第一个参数是 node（节点）ID，第二个参数在 zone 的大小，第三个参数的是该zone 的其实页帧号，最后一个参数是该zone的空洞大小，在 3.10版本。Node 的核心初始化函数是 free_area_init_core;

4592 static void __paginginit free_area_init_core(struct pglist_data *pgdat,  
4593 unsigned long *zones_size, unsigned long *zholes_size)  
4594{  
4595 enum zone_type j;  
4596 int nid = pgdat->node_id; //节点ID号  
4597 unsigned long zone_start_pfn = pgdat->node_start_pfn; //该node的起始页帧号  
4598 int ret;  
4599  
4600 pgdatresize_init(pgdat); //热插拔情况内存量可能会变化，重新计算  
4606 init_waitqueue_head(&pgdat->kswapd_wait); //换页守护进程队列头初始化。  
4607 init_waitqueue_head(&pgdat->pfmemalloc_wait); //尽最大努力分配内存队列头初始化。  
4608 pgdat_page_cgroup_init(pgdat); //node节点页cgroup初始化  
4609//迭代每一个zone  
4610 for $(j = 0; j < MAX\_NR\_ZONES; j++)$ {  
4611 struct zone *zone = pgdat->node Zones + j;  
4612 unsigned long size, realsize, freesize, memmap_pages;  
4613 //j类型的zone的页总数，可能包含空洞  
4614 size = zone_spanned_pages_in_node(nid, j, zones_size);  
4615 realsize = freesize = size - zone_absent_pages_in_node(nid, j, //realsize是真实长度，减去了hole

4616 zholes_size);   
4617   
4618 /\*调整页，将其按4KB边界对其，影响水印值。   
4623 memmap_pages $\equiv$ calc_memmap_size(size,realsize);   
4635 /\*DMA预留内存保留，不会计入到zone里\*/   
4636 if(j==0&&freesize>dma_reserved){   
4637 freesize $\equiv$ dma_reserved;   
4638 printf(KERN_DEBUG" $\% s$ zone:%lu pagesreserved\n",   
4639 zone_names[0],dma_reserved);   
4640 }   
//nr_kernel_pages记录了DMA和NORMAL类型的页数，对不是高端内存情况需要加上。如果内核页足够多，需要进行调整，内核页的物理地址范围小于896M。   
4642 if(!is_highmemidx(j))   
4643 nr_kernel_pages $+ =$ freesize;   
4644 /\*Charge for highmem memmap if there are enough kernel pages\*/   
4645 else if (nr_kernel_pages $\rightharpoondown$ memmap_pages $* 2)$ 4646 nr_kernel_pages $\rightharpoonup$ memmap_pages;   
4647 nr_all_pages $+ =$ freesize;   
4648//4649到4667初始化zone的相关成员   
4649 zone->spanned_pages $\equiv$ size;   
4650 zone->present_pages $\equiv$ realsize;   
4656 zone->managed_pages $\equiv$ is_highmem_idx(j)?realsize: freesize;   
4663 zone->name $\equiv$ zone_names[j];   
4664 spin_lock_init(&zone->lock);   
4665 spin_lock_init(&zone->lru_lock);   
4666 zone_seqlock_init(zone);   
4667 zone->zone_pgdat $\equiv$ pgdat;   
4668   
4669 zone_pcp_init(zone);//zone的per-CPU成员初始化   
4670 lruvec_init(&zone->lruvec);   
4671 if(！size)   
4672 continue;   
4673   
//伙伴系统的各阶初始化再次完成，从0阶到11阶   
4676 ret $\equiv$ initcurrently_empty-zone(zone,zone_start_pfn,   
4677 size,MEMMAP_EARLY);   
4678 BUG_ON(ret);   
4679 memmap_init(size,nid,j,zone_start_pfn);//处理内存域中的page实例，将其标记为可移动的。   
4680 zone_start_pfn $+ =$ size;   
4681 }   
4682}

# 3.5 启用内核内存分配器

依然还是图 3.0.1，这次是 mm_init 函数。

```c
static void __init mm_init(void)   
{ /\* \* page_cgroup requires contiguous pages, \* bigger than MAX_ORDER unless SPARSEMEM. \*/ page_cgroup_init_FLatmem(); mem_init(); kmem_cache_init(); percpu_init_late(); pgtable_cache_init(); vmalloc_init();   
} 
```

```txt
740 void __init mem_init(void)  
741 {  
742 int codesize, reservedpages, datasize, initsize;  
743 int tmp;  
761 /* this will put all low memory onto the freelists */  
762 totalram_pages += free_all.bootmem(); //释放 boot 内存分配器  
763  
764 reservedpages = 0;  
765 for (tmp = 0; tmp < max_low_pfn; tmp++)  
769 if (page_is_ram(tmp) && PageReserved(pfn_to_page(tmp)))  
770 reservedpages++;  
771  
772 after.bootmem = 1;  
773  
774 codesize = (unsigned long) &_etext - (unsigned long) &_text; //内核代码段  
775 datasize = (unsigned long) &_edata - (unsigned long) &_etext; //  
776 initsize = (unsigned long) &_init_end - (unsigned long) &_init_begin; 
```

下列dmesg出来的信息，就在该函数打印的。

```txt
[ 0.000000] Memory: 2044624K/2096632K available (6539K kernel code, 640K rwdata, 2764K rodata, 872K init, 924K bss, 52008K reserved, 1183624K highmem)  
[ 0.000000] virtual kernel memory layout:  
[ 0.000000] fixmap : 0xff14000 - 0xFFFFFF000 (940 kB)  
[ 0.000000] pkmap : 0xffc00000 - 0xffe00000 (2048 kB)  
[ 0.000000] vmalloc : 0xf83fe000 - 0xffbfe000 (120 MB)  
[ 0.000000] lowmem : 0xc0000000 - 0xf7bfe000 (891 MB)  
[ 0.000000] .init : 0xc19b9000 - 0xc1a93000 (872 kB)  
[ 0.000000] .data : 0xc1663132 - 0xc19b8200 (3412 kB) 
```

[ 0.000000] .text : 0xc1000000 - 0xc1663132 (6540 kB)

kmem_cache_init 初始化 cache，vmalloc 内存池初始化。

至此内存初始化流程结束。

# 第四章 物理内存管理

图2.2 中已经展示了伙伴系统对内存的组织管理，但是并未关联具体的实现代码，Linux内核并未将伙伴系统的管理代码单独列在一个buddy.c的文件里。

# 4.1 伙伴系统内存组织

由图2.2可知，每一个内存域（zone）的管理代码的组织是伙伴系统，所以其必然有字段来抽象这一管理方法。

```txt
include/linux/mmzone.h>   
struct zone{   
... struct free_area free_area[MAX_ORDER];   
} 
```

如果在编译内核时没有强制内存大小，则将是 11，即由2^11 个页组成一个连续的区域可以被分配使用。伙伴系统的页块组织如图4.1.1。

![](images/84611144d98062a17a83a611957bfb2340aae09c57a86c86f6948d2838119f90.jpg)  
图4.1.1 伙伴系统页块组织

/proc/buddyinfo 包含了伙伴系统的相关信息。

对 free_area 和迁移类型的定义如下：

include/linux/mmzone.h>   
enum{ MIGRATE_UNMOVABLE, MIGRATE_RECLAIMABLE, MIGRATE_MOVABLE, MIGRATE_PCPTYPES, /\* the number of types on the pcp lists \*/ MIGRATE_RESERVE $=$ MIGRATE_PCPTYPES, #ifdef CONFIG_CMA MIGRATE_CMA, #endif #ifdef CONFIG MEMORY_ISOLATION MIGRATE_ISOLATE, /\* can't allocate from here \*/ #endif MIGRATETYPES   
};   
struct free_area{ struct list_head free_list[MIGRATE_TYPES];

};

unsigned long nr_free; //当前该链表上空闲成员数

迁移类型的目标就是反内存碎片，Linux3.10 内核有多达 7 中迁移类型，比较明显的是per-CPU类型的迁移类型的增加。

MIGRATE_UNMOVABLE标识类型的内存在内存中有固定的地址范围且不能够被移动，多数的内核核心代码从此类型内存申请空间。

MIGRATE_RECLAIMABLE，不能够直接移动，但是内存可以被回收，数据结构可以被重建。由文件映射的数据就是这一类型。

MIGRATE_MOVABLE：可以被移动，属于用户空间的页就是这一类型的。

MIGRATE_PCPTYPES：管理 per-CPU 变量的类型。

MIGRATE_RESERVE：其和 MIGRATE_PCPTYPES 是一样的类型的，数据为紧急情况预留的类型。

MIGRATE_ISOLATE：是一个特殊的虚拟内存域，用于 NUMA 情况下夸节点移动物理页，不能从其申请内存。

当一个迁移类型对应的链表无法满足用户申请内存需求时，就可能会到其它迁移类型的链表上申请内存，Linux对去其它迁移类型链表申请内存也给出了一套优先级数组。

mm/page_alloc.c   
static int fallbacks[MIGRATETYPES][4] = { [MIGRATE_UNMOVABLE] $=$ {MIGRATE_RECLAIMABLE,MIGRATE_MOVABLE, MIGRATE_RESERVE}, [MIGRATE_RECLAIMABLE] $=$ {MIGRATE_UNMOVABLE, MIGRATE_MOVABLE, MIGRATE_RESERVE}, #ifdef CONFIG_CMA [MIGRATE_MOVABLE] $=$ {MIGRATE_CMA, MIGRATE_RECLAIMABLE, MIGRATE_UNMOVABLE, MIGRATE_RESERVE}, [MIGRATE_CMA] $=$ {MIGRATE_RESERVE},/* Never used */ #else [MIGRATE_MOVABLE] $=$ {MIGRATE_RECLAIMABLE,MIGRATE_UNMOVABLE, MIGRATE_RESERVE}, #endif [MIGRATE_RESERVE] $=$ {MIGRATE_RESERVE},/* Never used */ #ifdef CONFIG MEMORY_ISOLATION [MIGRATE_ISOLATE] $=$ {MIGRATE_RESERVE},/* Never used */ #endif }；

第一个数组元素的意义是，当在 MIGRATE_UNMOVABLE 迁移类型的链表申请内存不能满足要求时，会依次去 MIGRATE_RECLAIMABLE、MIGRATE_MOVABLE、MIGRATE_RESERVE 类型的迁移链表上查找内存。页迁移类型的设置接口如下：

mm/page_alloc.c   
void set_pageblock_migratetype(struct page \*page,int migratetype)   
{ if (unlikely(page_group_by_motion_disable)) migratetype $=$ MIGRATE_UNMOVABLE;   
set_pageblock_flags_group(page,(unsigned long)migratetype, PB_migrate,PB_migrate_end);

# 4.2 伙伴系统 API

伙伴系统是基于页的思想来管理内存的，所以其API操作的内存都是基于2的指数被进行的。细粒度的分配使用的是 slab分配方法。include/linux/gfp.h包含了伙伴系统的 API。本节就是对gfp.h文件的剖析。

alloc_pages(mask, order) 分配 2 的阶次大小的内存，返回 struct page 类型的实例，其表示的是图 4.1.1 中链接各个内存块链表头的那一个页，即和箭头有直接关联的页。定义于include/linux/gfp.h

# 4.2.1 内存申请 flag

关于申请内存的 flag 及其作用部分的定义如下（GFP 是 Get Free Page 缩写）：

```c
/* 裸GFP位掩码，API接口类调用并不直接使用这些位掩码，它们的前面下划线有三个*/  
#define __GFP_DMA 0x01u  
#define __GFP_HIGHMEM 0x02u  
#define __GFP_DMA32 0x04u  
#define __GFP_MOVABLE 0x08u  
#define __GFP_WAIT 0x10u  
#define __GFP_HIGH 0x20u  
#define __GFP_IO 0x40u  
#define __GFP_FS 0x80u  
#define __GFP_COLD 0x100u  
#define __GFP NOWARN 0x200u  
#define __GFP_REPEAT 0x400u  
#define __GFP_NOFAIL 0x800u  
#define __GFP_NORETRY 0x1000u  
#define __GFP_MEMALLOC 0x2000u  
#define __GFP_COMP 0x4000u  
#define __GFP_ZERO 0x8000u  
#define __GFP_NOMEMALLOC 0x10000u  
#define __GFP-hardwALL 0x20000u  
#define __GFPTHISNODE 0x40000u  
#define __GFP_RECLAIMABLE 0x80000u  
#define __GFP_KMEMCG 0x100000u  
#define __GFP_NOTTRACK 0x200000u  
#define __GFP_NO_KSWAPD 0x400000u  
#define __GFP_OTHERNode 0x800000u  
#define __GFP_WRITE 0x100000u  
/*如果修改了上述位掩码，_GFP_BITS_SHIFT也许需要修改*/  
/** GFP位掩码.  
*/  
*域修改符(linux/mmzone.h文件的低三比特定义了选择的zone类型)  
*/  
#define __GFP_DMA ((__force gfp_t)_GFP_DMA)//DMA类型的zone申请页  
#define __GFP_HIGHMEM ((__force gfp_t)_GFP_HIGHMEM)  
#define __GFP_DMA32 ((__force gfp_t)_GFP_DMA32)//x86-64采用该标志  
#define __GFP_MOVABLE ((__force gfp_t)_GFP_MOVABLE) /*Page is movable*/  
#define GFPZoneMASK (__GFP_DMA|__GFP_HIGHMEM|__GFP_DMA32|__GFP_MOVABLE)  
/*分配方式修改符－并不改变申请内存的域类型。  
*/  
*__GFP_REPEAT：努力尝试分配内存，但是也可能失败，这取决于VM的实现  
*__GFP_NOFAIL：无休止尝试申请内存，知道成功，目前已遗弃*  
*__GFP_NORETRY:VM实现不能无休止尝试申请内存。  
*/  
*__GFP_MOVABLE：申请的页类型是可以被迁移类型改变或者能够被回收  
*/  
#define __GFP_WAIT ((__force gfp_t)_GFP_WAIT) /*能够等待和调度？*/  
#define __GFP_HIGH ((__force gfp_t)_GFP_HIGH) /*是否应该从紧急内存池申请？*/  
#define __GFP_IO ((__force gfp_t)_GFP_IO)/*能够执行物理IO操作？可能存在换页操作？*/  
#define __GFP_FS ((__force gfp_t)_GFP_FS)/*能够调用底层文件操作？*/
```

define_GFP_COLD ((_force_gfp_t)_GFP_COLD) /*需要cache的冷页*/  
#define_GFP NOWARN ((_force_gfp_t)_GFP NOWARN) /*禁止内存分配失败警告*/  
#define_GFP_REPEAT ((_force_gfp_t)_GFP_REPEAT) /*See above*/  
#define_GFP_NOFAIL ((_force_gfp_t)_GFP_NOFAIL) /*See above*/  
#define_GFP_NORETRY ((_force_gfp_t)_GFP_NORETRY) /*See above*/  
#define_GFP_MEMALLOC((_force_gfp_t)_GFP_MEMALLOC)/*允许从紧急备用内存块申请*/  
#define_GFP_COMP ((_force_gfp_t)_GFP_COMP) /*添加复合页元数据*/  
#define_GFP_ZERO ((_force_gfp_t)_GFP_ZERO) /*成功时页全部被清零了*/  
#define_GFP_NOPARAMALOC((_force_gfp_t)_GFP_NOPARAMALOC)/*不要使用紧急备用内存.  
#define_GFP-hardwall ((_force_gfp_t)_GFP-hardwall)/*Enforce hardwall cpuset memory allocs*/  
#define_GFP_THISNODE ((_force_gfp_t)_GFP_THISNODE)/*只在该 node申请*/  
#define_GFP_RECLAIMABLE ((_force_gfp_t)_GFP_RECLAIMABLE)/*页可以回收*/  
#define_GFP_NOTTRACK ((_force_gfp_t)_GFP_NOTTRACK) /*Don't track with kmemcheck*/  
#define_GFP_NO_KSWAPD ((_force_gfp_t)_GFP_NO_KSWAPD)  
#define_GFP_OTHER_MODE ((_force_gfp_t)_GFP_OTHER_MODE)/*On behalf of other node*/  
#define_GFP_KMEMCG ((_force_gfp_t)_GFP_KMEMCG)/*Allocation comes from a memcg-accounted resource*/  
#define_GFP_WRITE ((_force_gfp_t)_GFP_WRITE) /*Allocator intends to dirty page*/  
/*等于0，但是使用常量以防止后续改变*/  
#define GFP NOWAIT (GFP_ATOMIC&~_GFP_HIGH)  
/*GFP_ATOMIC意味着既不等待（_GFP_WAIT不设置）并且可以使用紧急备用内存池*/  
#define GFP_ATOMIC ( _GFP_HIGH)  
#define GFP_NOIO ( _GFP_WAIT)  
#define GFP_NOFS ( _GFP_WAIT | _GFP_IO)  
#define GFP_KERNEL ( _GFP_WAIT | _GFP_IO | _GFP_FS)  
#define GFP_TEMPORARY ( _GFP_WAIT | _GFP_IO | _GFP_FS | $_\mathrm{GFP}$ RECLAIMABLE)  
#define GFP_USER ( _GFP_WAIT | _GFP_IO | _GFP_FS | _GFP-hardwall)  
#define GFP_HIGHUSER ( _GFP_WAIT | _GFP_IO | _GFP_FS | _GFP-hardwall | $_\mathrm{GFP}$ HIGHMEM)  
#define GFP_HIGHUSER movable ( _GFP_WAIT | _GFP_IO | _GFP_FS | $_\mathrm{GFP}$ HARDWALL | _GFP_HIGHMEM | $_\mathrm{GFP}$ MOVABLE)  
#define GFP_IOFS ( _GFP_IO | _GFP_FS)  
#define GFP_TRANSHUGE (GFP_HIGHUSER movable | _GFP_COMP | $_\mathrm{GFP}$ NOMEMALLOC | _GFP_NORETRY | _GFP NOWARN | $_\mathrm{GFP}$ NO_KSWAPD)  
#ifndef CONFIG_NUMA  
#define GFPTHISNODE ( _GFP,thisNODE| _GFP NOWARN| _GFP_NORETRY)  
#else  
#define GFP,thisNODE ((_force_gfp_t)0)  
#endif  
/*This mask makes up all the page movable related flags*/  
#define GFP_MOVABLE_MASK ( _GFP_RECLAIMABLE| _GFP_MOVABLE)  
/*Control page allocator reclaim behavior*/  
#define GFP_RECLAIM_MASK ( _GFP_WAIT| _GFP_HIGH| _GFP_IO| _GFP_FS| $_\mathrm{GFP}$ NOWARN| _GFP_REPEAT| _GFP_NOFAIL| $_\mathrm{GFP}$ NOMEMALLOC| _GFP_NOMEMALLOC)  
/*Control slab gfp mask during early boot*/  
#define GFP_BOOT_MASK ( _GFP BITS_MASK & ~(_GFP_WAIT| _GFP_IO| _GFP_FS))  
/*控制分配限制*/  
#define GFPCONSTRAINT_MASK ( _GFP-hardwall| _GFP,thisNODE)  
/*slab分配器不允许使用下面的分配掩码*/  
#define GFP_SLAB_bug_MASK ( (_GFP_DMA32| _GFP_HIGHMEM|~_GFP BITS_MASK)  
/*Flag-indicates that the buffer will be suitable for DMA. Ignored on some platforms, used as appropriate on others */  
#define GFP_DMA ( _GFP_DMA)  
/*4GB DMA on some platforms */  
#define GFP_DMA32 ( _GFP_DMA32

根据上面定义掩码可以获得迁移类型和 zone 类型，这通过

```txt
static inline enum zone_type gfp-zone(gfp_t flags)  
static inline int allocflags_to_migrate_type(gfp_t gfp_flags) 
```

实际上这类的操作比较比较比较简单，为节省篇幅，后面对单独获取掩码的位函数将不再列出。

# 4.2.2 内存申请释放 API

alloc_pages(gfp_mask, order)，根据 NUMA 启用情况，有两个版本 alloc_pages 接口，这里的接口是没有使用 NUMA 的情况，gfp_mask 是 4.1.1 节中申请标志，order 是分配的阶，底数是 2，对于 order 等于 0 的情况则退化为申请一个页。即alloc_pages(gfp_mask, 0)，gfp.h 文件专门定义了申请一页的接口：

Include/linux/gfp.h   
```python
define alloc_page(gfp_mask) alloc_pages(gfp_mask, 0) 
```

get_zeroed_page(gfp_t gfp_mask)，用于申请一个页，该页会被 0 填充。  
alloc_page_vma(gfp_mask, vma, addr) ，为 VMA 申请一页。  
__get_dma_pages(gfp_mask, order)，从 DMA 域申请内存。

如果内存分配失败，则返回值是 NULL 或者 0，所以调用者需要检查返回值，这些接口最后会调用核心函数__alloc_pages_nodemask 完成实际的内存分配。

![](images/a4cd5afd0ea18bbfd34682d4dae4288de960582c8f0d971c0be3ebeede356a53.jpg)  
图4.2.2 伙伴系统内存申请函数

水印影响系统内存分配行为，加上NUMA 存储架构，内存分配复杂性大大增加。在zone的定义中：

```c
enum zone_watermarks{ WMARK_MIN, WMARK_LOW, WMARK_HIGH, NR_WMARK   
}；   
struct zone{ unsigned long watermark[NR_WMARK];   
1
```

WMARK_HIGH：表示的是可用内存如果超过 watermark[WMARK_HIGH]，则说明可用内存很充裕，在这种情况下，内存分配过程代码相对简单，接着如果超过

watermark[WMARK_LOW]，则说明内存不是非常充裕，但是也不是处于紧张的状态，这时分配过程相对而言比第一种情况稍微复杂点，如果达到 WMARK_MIN 状态，在分配内存时会立即唤醒内存回收守护进程 kswap，进行换页操作以腾出更多空间，这一过程也是最复杂的。为了让分配的流程变的明晰，下列的分析对一些对分配流程中的一些不太重要的代码（NUMA 也被去掉，但并不是不重要）进行了精简。

```txt
mm/page_alloc.c 
```

```c
struct page * __alloc_pages_nodemask(gfp_t gfp_mask, unsigned int order, struct zoneList *zoneList, nodemask_t *nodemask) 
```

{enum zone_type high-zoneidx $\equiv$ gfp-zone(gfp_mask);//获得zone索引，DMA、NORMAL...struct zone \*preferred-zone;struct page \*page $=$ NULL;int migratetype $\equiv$ allocflags_to_migratetype(gfp_mask);//迁移类型，Movable....unsigned int cpuset_mems_cookie;intalloc_flags $\equiv$ ALLOC_WMARK_LOW|ALLOC_CPUSET;structmem_cgroup\*memcg $\equiv$ NULL;gfp_mask&=gfp Allowed_mask;might_sleep_if(gfp_mask&_GFP_WAIT);//如果线程设置了TIF NEED_RESCHED标志，则会执行调度，当前进程休眠if(should_fail_alloc_page(gfp_mask,order))//检查分配阶和掩码的合理性，return NULL; //见查zone是否存在，通常zone是存在的，但是GFPTHISNODE指定了申请内存的node之后就未必了。if (unlikely(!zonelist->_zonerefs->zone))return NULL;retry_cpuset:/*找到合适的zone，该zone的索引值小于参数high-zoneidx，找到该zone后，将该zone的地址存在参数zone中。*/first Zones_zonelist(zonelist,high-zoneidx,nodemask?&cpuset_current_mems Allowed,&preferred-zone);//如果没有找到合适的zone，则直接返回。if(!preferred-zone)goto out;/\*初次分配内存尝试，buddy系统分配内存核心函数之一，该函数分析见后文\*/page $\equiv$ get_page_from_freelist(gfp_mask)_GFP-hardwALL,nodemask,order,zonelist,high-zoneidx,alloc_flags,preferred-zone,migratetype);//失败情况下，会进一步申请内存if(!unlikely(!page)){gfp_mask $\equiv$ memalloc_noio_flags(gfp_mask);page $\equiv$ __alloc_pages_slowpath(gfp_mask,order,zonelist,high-zoneidx,nodemask,preferred-zone,migratetype);1out:if(!unlikely(!put_mems Allowed(cpuset_mems_cookie)&&!page))goto retry_cpuset;return page;

伙伴系统的核心分配函数 get_page_from_freelist 定义如下，该函数会验证空闲页数量和水印的关系，如果满足则会调用buffered_rmqueue 从伙伴系统上移除分配的页。

static struct page * get_page_from_freelist(gfp_t gfp_mask, nodemask_t *nodemask, unsigned int order, struct zoneList *zonelist, int high-zoneidx, int alloc_flags, struct zone *preferred-zone, int migratetype)   
{ struct zoneref \*z; struct page \*page $=$ NULL; int classzone_idx; struct zone \*zone; nodemask_t \*allowednodes $=$ NULL;/\*zonelist_cache approximation \*/ int zlc.active $= 0$ /\*set if using zoneList_cache\*/ int did_zlc_setup $= 0$ /\*just call zlc_setup() one time\*/ classzone_idx $=$ zone_idx(preferred-zone);//保持zone的索引号   
zonelist_scan: for_each-zone_zonelist_nodemask(zone,z,zonelist, high-zoneidx, nodemask){ if((alloc_flags&ALLOC_CPUSET)&&//CPU SET用于将CPU资源和进程捆绑。 !cpuset-zoneAllowed磋ll(zone,gfp_mask))//检查这里的zone是否属于允许运行该进程的CPU上 continue; //如果没有启用水印检查，则跳过if语句。 if(!(alloc_flags&ALLOC_NO_WATERMARKS)){ unsigned long mark;

```txt
int ret;  
mark = zone->watermark[alloc_flags & ALLOC_WMARK_MASK]; //根据分配flag获得对应内存的水印值。//根据上述水印值和需要分配内存的阶判断该zone是否能够用来分配内存，如果能够则到try.this-zone标号尝试内存分配。if (zone_watermark.ok(zone, order, mark, classzoneidx, alloc_flags)) goto try.this-zone;  
//如果没有启用NUMA，ret的返回值是0，这时需要回收一些内存，ret = zone_reclaim(zone, gfp_mask, order); switch(ret){ case ZONE_RECLAIM_NOSCAN: /* did not scan */ continue; case ZONE_RECLAIM_FULL: /* scanned but unreachable */ continue; default: /* 回收可以异步进行，这里判断是否回收足够的内存了*/ if (zone_watermark.ok(zone, order, mark, classzoneidx, alloc_flags)) goto try.this-zone; continue; }  
}  
//分配函数，和水印判断函数一并在后面。try.this-zone: page = buffered_rmqueue(preferred_zone, zone, order, gfp_mask, migratetype); if (page) break; } if (page) page->pfmemalloc = !!(alloc_flags & ALLOC_NO WATERMARKS); return page;
```

从上面的代码注释可以看出，在未启用 NUMA 的情况下，get_page_from_freelist 的主要功能集中于两个函数：

zone_watermark_ok，判断水印是否满足   
buffered_rmqueue，执行从伙伴系统将空闲页移除给申请内存的进程

free_pages 是 NR_FREE_PAGES 统计信息得到的当前 zone 的空闲页数目，

static bool __zone_watermark.ok(struct zone *z, int order, unsigned long mark, int classzone_idx, int alloc_flags, long free_pages)//free_pages存放的是zone当前空闲页  
{ //mark值是watermark[WMARK_HIGH]，或者watermark[WMARK_LOW]或者watermark[WMARK_MIN]，即系统要求的空闲水印值 long min = mark; long lowmem_reserved = z->lowmem_reserved.classzone_idx;//该zone保留的页数量，紧急情况会使用这些页 int o; //free_pages存放的是zone当前空闲页-需要分配出去的页==分配出去以后的空闲页，可能小于0，但没有影响 free_pages $= (1 << \text{order}) - 1$ if(alloc_flags&ALLOC_HIGH)//如果启用的是ALLOC_HIGH，则将系统水印值除以2 min $= \min /2$ ; if(alloc_flags&ALLOC_HARDER)//HARDER情况继续减去四分之一 min $= \min /4$ ; if(free_pages $<   =$ min $^+$ lowmem_reserved)//如果分配出去需要的阶内存后，不能满足系统水印，则返回错误吧 return false; /*上述条件满足，还不能确定一定就能分配成功，for循环将剩余内存总量，分配到各阶上。 \*将系统剩余的内存量减去o阶，nr_free是对应的空闲对象书，左移o阶后，得到的是空闲页数量， \* for $(\mathrm{o} = 0;\mathrm{o} <   \mathrm{order};\mathrm{o} + + )$ { /\*At the next order,this order's pages become unavailable \*/ free_pages $= -z->$ free_area[o].nr_free<<o;//减去o阶后，剩下的空闲页数。

```c
/* 阶越大，需要的空闲页越少，该值记录的是根据内存分配紧张标识调整水印后的需要空闲的页数量 */
min >= 1;
if (free_pages <= min)//比较空闲页数量是否满足水印的要求
return false;
}
return true;
```

# 4.2.3移除选定的页

移除页的函数执行流程

![](images/517e49c3b9b2240579246917592fb6fe8f00f5f31d000476ee4547327bb8be88.jpg)  
图 4.2.3 buffered_rmqueue 代码调用流程

```c
static inline
struct page *buffered_rmqueue(struct zone *preferred-zone, struct zone *zone, int order,
gfp_t gfp_flags, int migratetype)
{
    unsigned long flags;
    struct page *page;
    int cold = !!(gfp_flags & __GFP_COLD);
}
again://阶是0，申请一页的情况，可以从per-CPU冷热页链表上取。
if (likely(order == 0)) {
/***********per_cpu_pages的定义如下********** */
***********struct per_cpu_pages {
***********int count; /*链表上的页总数*/
***********int high; /*high watermark*/
***********int batch; /*伙伴系统添加和移除块大小*/
***********/页链表，每一个迁移类型对应一个pcp链表*/
***********struct list_head lists[MIGRATE_PCPTYPES];
***********};
struct per_cpu_pages *pcp;
struct list_head *list;
local_irq_save(flags); //保持中断标志。
pcp = &this_cpu_ptr(zone->pageset) ->pcp; //获得该CPU对应的pcp链表。
list = &pcp->lists[migratetype]; //获得对应迁移类型的pcp链表
if (list_empty(list)) { //如果该链表是空，则没法直接从该链表获得
/*从伙伴系统获得指定的元素大小的页，并将它们添加到pcp链表上，返回值是得到的可用的页数。其调用_rmqueue函数完成核心分配，在申请多余一页的情况下也会调用_rmqueue函数。
    pcp->count += rmqueue_bulk(zone, 0,
        pcp->batch, list,
        migratetype, cold);
}
```

if (unlikely(list_empty(list))) goto failed;   
}   
//根据申请的是冷页还是热（所谓的冷热是相对在不在cache中，热指的是在cache中，）   
//页标志，在该链表的首部是热页，尾部是冷页。 if(cold) page $=$ list_entry(list->prev,struct page,lru); else page $=$ list_entry(list->next,struct page,lru); list_del(&page->lru);//将该page从page的管理链表上删除。 pcp->count--; }else{ spin_lock_irqsave(&zone->lock,flags); page $=$ _rmqueue(zone,order,migratetype);//从伙伴系统选择一个合适的页块，失败返回NULL spin_unlock(&zone->lock); if(!page) goto failed; __mod-zone_freepage_state(zone,-(1<<order), get_pageblock_migratetype(page)); } count-zone_vm_events(PGALLOC,zone,1<<order); zone_statistics(preferred-zone,zone,gfp_flags);//跟新zone统计数据 local_irq_restart(flags); VM_BUG_ON(bad_range(zone.page)); //进行一些检查和设置各种flag标志，通常这里不该应有错，如果这里有错，意味着内核其它地方有问题。 if(prep_new_page(page,order,gfp_flags)) goto again; return page;   
failed: local_irq.Restore(flags); return NULL;

__rmqueue 会调用__rmqueue_smallest 函数获得申请的内存，但是如果内存域、阶和迁移类型不满足，则会调用__rmqueue_fallback 从其它迁移列表获取内存。__rmqueue_smallest的主要核心代码如下：

//遍历每一个order，只有order比当前要求order大才有可能成功申请到内存页。for(current_order $=$ order;current_order $<$ MAX_ORDER; $+ +$ current_order){area $=$ &zone->free_area[current_order]);//获得该zone的伙伴管理核心数据结构。  
//如果该伙伴对应的迁移类型的链表上没有空闲元素，则遍历下一阶。if(list_empty(&area->free_list[migratetype]))continue;  
//如果有空闲元素，则获得该空闲元素的起始项，在图4.1.1中，可以看到取首页就可以了。page $=$ list_entry(area->free_list[migratetype].next,struct page,lru);list_del(&page->lru);//对应的页已经被分配，从空闲链表删除。rmv_page_order(page);//设置页的flag，这是页不在属于buddy了，area->nr_free--;  
//如果申请的阶大于需要的阶，则需要将高阶的进行拆解并插入低阶buddy链表上，expand完成此工作。expand(zone，page,order, current_order,area,migratetype);return page;

Expand函数如下，low 是要分配的阶，high是找到的当前能够满足分配要求的阶：

static inline void expand(struct zone \*zone, struct page \*page, int low, int high, struct free_area \*area, int migratetype)   
{ unsigned long size $= 1 <   <   \mathrm{high}; / /$ 得到总页数

```c
while (high > low) {  
    area--;  
    high--;  
    size >= 1; //遍历一次，  
//size 定位拆分后伙伴块的起始页框描述符，并将其作为第一个块添加到下一级 order 的块链表中。  
list_add(&page[size].lru, &area->free_list[migratetype]);  
area->nr_free++;  
set_page_order(&page[size], high);  
}
```

其工作方式使用一个简单的实例可以看出：预期分配的阶是1，满足索取要求的内存阶3，index 设为 0，即：expand(zone, page,1,3,area,migratetype);

图 4.2.4 展示了 expand 执行的流程，图中 1_1，表示的是第一次循环时执行的第一步，1_2 是第一次循环时第二步，2_1 是第二个循环时执行的第一步，依次类推。

1_1 将 area 由阶三指向阶是 2 的 free_area 区，1_2 将 high 值减一，high 值变为 2,1_3 将 size除以2，由8 变为4，1_4是关键的一步，以size为索引，&page[size]获得后一半（4 页）内存区地址，并通过 list_add将其添加到阶为后一半大小的 area链表上，第二次的循环依次类推，只不过其后一半内存区，变成了前4 页的后一半（蓝色框所示）。

![](images/22fc8f23de2e2cee89631f1c7af323075ac22c2ab3400d20bd13a414ca56f8f8.jpg)  
图 4.2.4 expand 函数逻辑

如果调用__rmqueue_fallback 从其它迁移列表获取内存，则说明该迁移类型内存缺少，这是内核倾向于申请一块大的而不是正适合申请内存大小。

static inline struct page\* _rmqueue_fallback(struct zone \*zone,int order,int start_migratetype)   
{ struct free_area \* area; int current_order; struct page \*page; int migratetype,i; /\*从高阶开始，尽量获取一个大块内存，因为该迁移类型还要满足后续内存请求\*/ for(current_order $=$ MAX_ORDER-1;current_order $\rightharpoondown$ order; --current_order){ for $(\mathrm{i} = 0;\mathrm{i} + + )$ { migratetype $=$ fallbacks[start_migratetype][i];//获得备用的迁移类型。 /\*MIGRATE_RESERVE handled later if necessary\*/ if(migratetype $= =$ MIGRATE_RESERVE) break; //如果该free_area没有空闲块，则遍历更低一阶

area $=$ &(zone->free_area[current_order]); if(list_empty(&area->free_list[migratetype])) continue; //如果free_area有空闲块，则获得该块的第一个page实例，该页包含了块的管理信息 page $=$ list_entry(area->free_list[migratetype].next, struct page,lru); area->nr_free--; /\* is_migrate_cma判断迁移类型是否是MIGRATE_CMA，该迁移类型是contiguousMemoryAllocator的缩 写。在2011年该类型支持分配大的、物理上连续的内存块，关于该迁移类型的显示解释见http://lwn.net/Articles/486301/,这里 假设内核没有使用该迁移类型以简化从备用迁移类型申请内存这一过程。 如果申请的是大块内存，则将所有空闲页移动到优先选用的分配列表，如果是可回收类型，则更积极的获得的页所有权。 \*/ if(!is_migrate_cma(migratetype)&& (unlikely(current_order $> =$ pageblock_order/2)|| start_migratetype $= =$ MIGRATE_RECLAIMABLE || page_group_bymobility_disable)) { int pages; pages $=$ move_freepages_block(zone,page, start_migratetype); /*块有一半是空闲的，则获取整个块*/ if (pages $> =$ 1<< (pageblock_order-1)) || page_group_bymobility_disable) set_pageblock_migratetype(page, start_migratetype); migratetype $=$ start_migratetype; } /\*从空闲列表移除页\*/ list_del(&page->lru); rmv_page_order(page); /\* 获取 orders $> =$ pageblock_order的所有权\*/ if(current_order $> =$ pageblock_order&& !is_migrate_cma(migratetype)) change_pageblock_range(page,current_order, start_migratetype); expand(zone,page,order,current_order, area, is_migrate_cma(migratetype)?migratetype:start_migratetype); trace_mm_page_alloc_extfrag(page,order,current_order, start_migratetype,migratetype); return page; } } return NULL;

# 4.3.4 释放页

对应的释放内存的API有：

```c
void __free_pages(struct page *page, unsigned int order);  
void free_pages(unsigned long addr, unsigned int order);  
#define __free_page(page) __free_pages((page), 0)  
#define free_page(addr) free_pages((addr), 0) 
```

上面调用的核心释放函数是__free_pages，该函数针对一页和多页进行了不同处理，主要体现在是存放在per-CPU变量中，还是释放给buddy 系统。

//将 page 的引用计数（&page->_count）减一，如果为 0，则说明没有被使用，则需要进行释放工作。

void__free_pages(struct page \*page,unsigned int order)   
{ if (put_page_testzero(page)){ //如果是一页，则添加到per-CPU缓存中 if (order $= = 0$ 1 free_hot_cold_page(page,0); else   
//s释放多个页的情况 __free_pages.ok(page,order); }

free_hot_cold_page 释放单个页，如果 cold 是 1，则释放的是冷页，是 0，则释放的是热页。

void free-hot_cold_page(struct page \*page,int cold)   
{ struct zone \*zone $=$ page-zone(page); //有page获得其所在的zone struct per_cpu_pages \*pcp; unsigned long flags; int migratetype; if(!free_pagesprepare(page,0))//释放页之前要检查一下，是否真的适合释放。 return; migratetype $\equiv$ get_pageblock_migratetype(page); //获得该页所在块的迁移类型。 set_freepage_migratetype(page,migratetype); local_irq_save(flags); //获得per-CPU缓存链表 pcp $=$ &this_cpu_ptr(zone->pageset->pcp; if(cold)//冷页 list_addTAIL(&page->lru,&pcp->lists[migratetype]); else//热页 list_add(&page->lru,&pcp->lists[migratetype]); pcp->count++; //如果per-CPU缓存中页数目超出pcp->high，则需要进行惰性合并，将数量为pcp->batch的页退还给伙伴系统 if (pcp->count $\coloneqq$ pcp->high){ free_pcppages_bulk(zone,pcp->batch,pcp); pcp->count $= =$ pcp->batch; } out: local_irq.Restore(flags);

当释放多页时，会牵涉到将释放的页退还给 buddy 管理系统，这一过程到不复杂，不过却有点绕，首先看几个该过程的辅助函数。

//根据给定的页帧号和释放的阶找到 buddy系统其另一半，比如，如果page_idx是10，order 是0，则，该函数的返回值是11，说明页帧号10、11原本是order是1的一个块。如果 order 是 1，而 page_idx 仍然是 10，那么该函数的返回值是 8，说明 8、9 以及 10、11 可以组成一个阶为2的块。

```c
static inline unsigned long find_buddy_index(unsigned long page_idx, unsigned int order) { return page_idx^ (1 << order); } 
```

上述的函数只是说明存在合并的可能，至于能不能合并取决于如下条件：

1、找到的伙伴页不能是 hole，而必须是存在对应物理页的。  
2、需要组合成一个更高阶的两个页块的域号必须一致，不能 DMA 域和 NORMAL 域的页合并。  
3、两个要合并的页块它们的阶必须是相同的，不同阶之间的页块不能直接合并  
4、 当然该伙伴块还必须属于伙伴系统 page_is_guard(buddy)。

5、 另外，该伙伴页块必须空闲，这样才能和正被释放的页块合并。

static inline int page_is_buddy(struct page \*page, struct page \*buddy, int order)   
{ if(!PFN_valid_within(page_to_pfn(buddy))) return 0; if (page-zone_id(page) != page-zone_id(buddy)) return 0; if(PageBuddy(buddy)&&page_order(buddy) $= =$ order){ VM_bug_ON(page_count(buddy) $! = 0$ return 1; } return 0;

释放页的时候会检查是否需要合并页，这是由函数__free_one_page 完成的，为了使这个有点绕的过程不绕人，以一个例子展示这一过程。

释放一个 0 阶内存块（一页），页帧号是 10，页帧号 $0 \sim 1 5$ 均处于空闲状态且在 buddy系统中，页帧号为16的为已经分配出去的页，该页所处的页块不能被合并。

![](images/c507b3e31a14da9b97152bcd407e35a7e2c811f5efc0ea79aa88a6f2fed5b61e.jpg)  
图 4.2.5 页合并实例

```c
static inline void __free_one_page(struct page *page, struct zone *zone, unsigned int order, int migratetype)  
{ unsigned long page_idx; unsigned long combined_idx; unsigned long uninitialized_var(buddy_idx); struct page *buddy; //获得页帧号 page_idx = page_to_pfn(page) & ((1 << MAX_ORDER) - 1); //该while循环完成了图4.2.5的合并过程。 while (order < MAX_ORDER-1) { buddy_idx = __find_buddy_index(page_idx, order); //找到伙伴索引 buddy = page + (buddy_idx - page_idx); //根据偏移找到伙伴页 if (!page_is_buddy(page, buddy, order))//见前面 break; //如何可以进行页块合并，则将伙伴从该阶对应的链表上删除 list_del(&buddy->lru); zone->free_area[order].nr_free--; //更新该order对应的空闲块数 //更新该页标志，因要被合并而不再是伙伴页框的第一个单元，清除伙伴标志和阶信息 rmv_page_order(buddy); //找到这两个可以合并页框的起始的那个，之前是由函数计算的，这里用一句代替以前封装的一个函数。 combined_idx = buddy_idx & page_idx; page = page + (combined_idx - page_idx); //合并后块的起始第一个页帧。 page_idx = combined_idx; //下一次循环准备 order++; //阶信息递增一个 } set_page_order(page, order); // rmv_page_order(buddy); 的逆过程 /\* 如果当前合并后的页块的阶并非最大，则将其放大伙伴链表的末尾，这样其不会很快被使用到，更有可能合并到更高阶。\*/
```

if((order $<$ MAX_ORDER-2)&&pfn_valid_within(page_to_pfn(buddy))){ struct page \*higher_page,\*higher_buddy; combined_idx $=$ buddy_idx&page_idx; higher_page $=$ page $^+$ (combined_idx-page_idx); buddy_idx $=$ _find_buddy_index(combined_idx,order $+1$ ); higher_buddy $=$ higher_page $^+$ (buddy_idx-combined_idx); if(page_is_buddy(higher_page,higher_buddy,order $+1$ )){ list_addTAIL(&page->lru, &zone->free_area[order].free_list[migratetype]); goto out; }   
//如果更高阶已经分配出去，则合并成更高阶的可能性变小，将其放在链表前面 list_add(&page->lru,&zone->free_area[order].free_list[migratetype]);   
out: zone->free_area[order].nr_free++;

表 4-1 图 4.2.5 合并过程的计算  

<table><tr><td>order</td><td>page_idx</td><td>buddy_idx - page_idx</td><td>combined_idx</td></tr><tr><td>0</td><td>10</td><td>1</td><td>10</td></tr><tr><td>1</td><td>10</td><td>-2</td><td>8</td></tr><tr><td>2</td><td>8</td><td>4</td><td>8</td></tr><tr><td>3</td><td>8</td><td>-8</td><td>0</td></tr></table>

# 4.4 slab 分配器

Slab 是内存管理中一个重要部分，以前遇到一个图像卡顿的问题就是通过查看 slab 信息才定位到问题所在子模块的，slab 变动比较小，我接触到的版本 2.6.38，3.10，3.13 使用的都是2.1 版本的slab分配器，这节就来看看slab分配器吧。

和伙伴系统一样是为了管理内存，但是其管理的粒度要比伙伴系统小。查看当前系统的slab信息可以使用cat /proc/slabinfo命令查看。该命令的第一行已经说明了各个字段的意义，第一列的字段(name)是 slab 的名称，即 slab 创建函数 kmem_cache_create 的第一个参数，这在后面分析该函数时可以看到。

```txt
# cat /proc/slabinfo slabinfo - version: 2.1 # name <activeobjs> <num objs> <objsize> <objperslab> <pagesperslab> : tunables <limit> <batchcount> <sharedfactor>: slabdata <active_slabs> <num_slabs> <sharedavail> ubifs_inode_slab 39 40 384 10 1: tunables 54 27 0: slabdata 4 4 0 SCTPv6 1 4 928 4 1: tunables 54 27 0: slabdata 1 1 0 SCTP 0 0 800 5 1: tunables 54 27 0: slabdata 0 0 0 sctp_chunk 0 0 192 20 1: tunables 120 60 0: slabdata 0 0 0 sctp_bind_bucket 0 0 16 203 1: tunables 120 60 0: slabdata 0 0 0 xfrm6_tunnel_spi 0 0 64 59 1: tunables 120 60 0: slabdata 0 0 0 fib6_nodes 14 113 32 113 1: tunables 120 60 0: slabdata 1 1 0 ip6Dst_cache 15 15 256 15 1: tunables 120 60 0: slabdata 1 1 0 ndisc_cache 13 24 160 24 1: tunables 120 60 0: slabdata 1 1 0 ip6_mrt_cache 0 0 128 30 1: tunables 120 60 0: slabdata 0 0 0 RAWv6 5 6 640 6 1: tunables 54 27 0: slabdata 1 1 0 UDPLITEv6 0 0 640 6 1: tunables 54 27 0: slabdata 0 0 0 UDPv6 6 6 640 6 1: tunables 54 27 0: slabdata 1 1 0 tw_sock_TCPv6 0 0 160 24 1: tunables 120 60 0: slabdata 0 0 0 request_sock_TCPv6 0 0 128 30 1: tunables 120 60 0: slabdata 0 0 0 TCPv65688883222bf_contrack expects fcontrack_c04fa208919289191: tunables12060fslabdata11bf flow_cache o o8o481: tunables12060fslabdatao o oubi wl_entry_slab256290241451: tunables12060fslabdata22o cfq_io_context o o64591: tunables12060fslabdatao o ocfq_queue o o16o241: tunables12o6o:fslabdatao o oqueue_inode_cache o o8o4888322cifs_small_rq303644891: tunables5427o:fslabdata44o cifs_request441648o18tunables84o:fslabdata44o 
```

<table><tr><td>cifs_mpx_ids</td><td>3</td><td>59</td><td>64</td><td>59</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>cifs_inode_cache</td><td>0</td><td>0</td><td>352</td><td>11</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>nfs_direct_cache</td><td>0</td><td>0</td><td>72</td><td>53</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>nfs_write_data</td><td>72</td><td>72</td><td>448</td><td>9</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>8</td><td>8</td><td>0</td></tr><tr><td>nfs_read_data</td><td>34</td><td>36</td><td>416</td><td>9</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>4</td><td>4</td><td>0</td></tr><tr><td>nfs_inode_cache</td><td>13</td><td>14</td><td>544</td><td>7</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>2</td><td>2</td><td>0</td></tr><tr><td>nfs_page</td><td>35</td><td>59</td><td>64</td><td>59</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>fat_inode_cache</td><td>0</td><td>0</td><td>352</td><td>11</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>fat_cache</td><td>0</td><td>0</td><td>24</td><td>145</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>ext2_inode_cache</td><td>333</td><td>333</td><td>416</td><td>9</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>37</td><td>37</td><td>0</td></tr><tr><td>configfs_dir_cache</td><td>1</td><td>67</td><td>56</td><td>67</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>kioctx</td><td>0</td><td>0</td><td>192</td><td>20</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>arp_cache</td><td>11</td><td>24</td><td>160</td><td>24</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>RAW</td><td>5</td><td>8</td><td>480</td><td>8</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>UDP</td><td>8</td><td>8</td><td>480</td><td>8</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>tw_sock.tcp</td><td>10</td><td>30</td><td>128</td><td>30</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>request_sock.tcp</td><td>0</td><td>0</td><td>96</td><td>40</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>TCP</td><td>14</td><td>14</td><td>1088</td><td>7</td><td>2: tunables</td><td>24</td><td>12</td><td>0: slabdata</td><td>2</td><td>2</td><td>0</td></tr><tr><td>sock_inode_cache</td><td>66</td><td>66</td><td>352</td><td>11</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>6</td><td>6</td><td>0</td></tr><tr><td>skbuff_fclone_cache</td><td>10</td><td>10</td><td>384</td><td>10</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>skbuff_head_cache</td><td>120</td><td>120</td><td>192</td><td>20</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>6</td><td>6</td><td>0</td></tr><tr><td>file_lock_cache</td><td>8</td><td>40</td><td>96</td><td>40</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>1</td><td>1</td><td>0</td></tr><tr><td>shmem_inode_cache</td><td>719</td><td>730</td><td>392</td><td>10</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>73</td><td>73</td><td>0</td></tr><tr><td>proc_inode_cache</td><td>84</td><td>84</td><td>320</td><td>12</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>7</td><td>7</td><td>0</td></tr><tr><td>anon_vma_chain</td><td>555</td><td>580</td><td>24</td><td>145</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>4</td><td>4</td><td>0</td></tr><tr><td>anon_vma</td><td>360</td><td>406</td><td>16</td><td>203</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>2</td><td>2</td><td>0</td></tr><tr><td>pid</td><td>177</td><td>177</td><td>64</td><td>59</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>3</td><td>3</td><td>0</td></tr><tr><td>radix_tree_node</td><td>598</td><td>611</td><td>296</td><td>13</td><td>1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>47</td><td>47</td><td>0</td></tr><tr><td>idr_layer_cache</td><td>260</td><td>260</td><td>152</td><td>26</td><td>1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>10</td><td>10</td><td>0</td></tr><tr><td>size-4194304</td><td>0</td><td colspan="2">0 4194304</td><td colspan="2">1 1024 : tunables</td><td>1</td><td>1</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>size-2097152</td><td>0</td><td colspan="2">0 2097152</td><td colspan="2">1 512 : tunables</td><td>1</td><td>1</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>size-1048576</td><td>3</td><td colspan="2">3 1048576</td><td colspan="2">1 256 : tunables</td><td>1</td><td>1</td><td>0: slabdata</td><td>3</td><td>3</td><td>0</td></tr><tr><td>size-524288</td><td>2</td><td colspan="2">2 524288</td><td colspan="2">1 128 : tunables</td><td>1</td><td>1</td><td>0: slabdata</td><td>2</td><td>2</td><td>0</td></tr><tr><td>size-262144</td><td>0</td><td colspan="2">0 262144</td><td colspan="2">1 64 : tunables</td><td>1</td><td>1</td><td>0: slabdata</td><td>0</td><td>0</td><td>0</td></tr><tr><td>size-131072</td><td>2</td><td colspan="2">2 131072</td><td colspan="2">1 32 : tunables</td><td>8</td><td>4</td><td>0: slabdata</td><td>2</td><td>2</td><td>0</td></tr><tr><td>size-65536</td><td>2</td><td colspan="2">2 65536</td><td colspan="2">1 16 : tunables</td><td>8</td><td>4</td><td>0: slabdata</td><td>2</td><td>2</td><td>0</td></tr><tr><td>size-32768</td><td>17</td><td colspan="2">17 32768</td><td colspan="2">1 8 : tunables</td><td>8</td><td>4</td><td>0: slabdata</td><td>17</td><td>17</td><td>0</td></tr><tr><td>size-16384</td><td>16</td><td colspan="2">16 16384</td><td colspan="2">1 4 : tunables</td><td>8</td><td>4</td><td>0: slabdata</td><td>16</td><td>16</td><td>0</td></tr><tr><td>size-8192</td><td>5</td><td colspan="2">5 8192</td><td colspan="2">1 2 : tunables</td><td>8</td><td>4</td><td>0: slabdata</td><td>5</td><td>5</td><td>0</td></tr><tr><td>size-4096</td><td>25</td><td colspan="2">25 4096</td><td colspan="2">1 1: tunables</td><td>24</td><td>12</td><td>0: slabdata</td><td>25</td><td>25</td><td>0</td></tr><tr><td>size-2048</td><td>144</td><td colspan="2">168 2048</td><td colspan="2">2 1: tunables</td><td>24</td><td>12</td><td>0: slabdata</td><td>78</td><td>84</td><td>0</td></tr><tr><td>size-1024</td><td>100</td><td colspan="2">100 1024</td><td colspan="2">4 1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>25</td><td>25</td><td>0</td></tr><tr><td>size-512</td><td>304</td><td colspan="2">304 512</td><td colspan="2">8 1: tunables</td><td>54</td><td>27</td><td>0: slabdata</td><td>38</td><td>38</td><td>0</td></tr><tr><td>size-256</td><td>240</td><td colspan="2">345 256</td><td colspan="2">1 1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>21</td><td>23</td><td>0</td></tr><tr><td>size-192</td><td>300</td><td colspan="2">300 192</td><td colspan="2">1 1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>15</td><td>15</td><td>0</td></tr><tr><td>size-128</td><td>450</td><td colspan="2">450 128</td><td colspan="2">1 1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>15</td><td>15</td><td>0</td></tr><tr><td>size-96</td><td>748</td><td colspan="2">760 96</td><td colspan="2">1 1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>19</td><td>19</td><td>0</td></tr><tr><td>size-64</td><td>1062</td><td colspan="2">1062 64</td><td colspan="2">1 1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>18</td><td>18</td><td>0</td></tr><tr><td>size-32</td><td>3390</td><td colspan="2">3390 32</td><td colspan="2">1 1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>30</td><td>30</td><td>0</td></tr><tr><td>kmem_cache</td><td>124</td><td colspan="2">160 96</td><td colspan="2">1 1: tunables</td><td>120</td><td>60</td><td>0: slabdata</td><td>4</td><td>4</td><td>0</td></tr></table>

slab关系的内存分配函数如下：

kmalloc/__kmalloc/kmalloc_node，其申请的内存来源于/proc/slabinfo 输出的第一列以 size开始的slab类型。  
kmem_cache_alloc/kmem_cache_alloc_node 特定类型的有名称的内存缓存类型，/proc/slabinfo 输出的第一列左上部，如 skbuff_head_cache 是特定于网卡接收数据包头 cache，其就是取自于slab分配器。

![](images/dfcdd55395fb58e3a7655213096d6ba091572bb1e289a5d2524553efa26d443a.jpg)  
图4.1linux内核内存管理关系图

# 4.4.1 slab 的实现

![](images/fb785846aca4ef2308a43d865b12879999851b47e5970d1320487e26cac07a88.jpg)  
图 4.2 slab 管理结构图

/proc/slabinfo 输出的第一列的每种类型的 slab 都对应一个 kmem_cache，每个 slab 包含2^gfporder 个连续的物理内存页，缺省情况下，最多1024个物理页框构成。每个NUMAnode都有三个 slab 链表，full 对应于所有对象均被分配出去，无空闲对象；free 链表正好是 full的对立面，而partial介于二者之间。

slab 缓存由 kmem_cache 定义，其定义位置由 slab.c 文件移到了 slab_def.h。内核对这一变动的解释是：如果在编译期间 kmalloc 的调用被赋予了确定的 size，那么在编译时就可以通过选择合适的通用cache 来对其进行优化。

```c
struct kmem_cache {
/* 1) cache 可调参数。被 cache_chain_mutex 保护*/
unsigned int batchcount; //当 per-CPU 缓存列表为空时，从 slab 获取对象数目，或者从 cache Grow 一次分配对象数。
unsigned int limit; //per-CPU 数组中保存的最大对象数
unsigned int shared; //是否存在共享 CPU 的高速缓存
unsigned int size; //slab 管理对象的大小（包括 padding）
u32 reciprocal_buffer_size; //buffer_size 的倒数，使用乘法代替除法，Newton-Raphson 方法。
/* 2) alloc & free 将要会修改的字段*/
```

unsigned int flags; /* constant flags */ unsigned int num; /* 每个slab具有的对象数*/   
/*3)cache动态增加或者减少*/ /\*order of pgs per slab $(2^{\wedge}\mathfrak{n})^{*} /$ unsigned int gfporder; /\* force GFP flags,e.g.GFP_DMA\*/ gfp_t allocflags; size_t colour; /* cache colouring range \*/ unsigned int colour_off; /* 着色偏移，不同的颜色代表不同的偏移，不同的偏移意味着映射不到同一个缓存行 \*/ struct kmem_cache\*slabp_cache; unsigned int slab_size; /\*构造函数，面向对象中的一种程序设计方法\*/ void (*ctor)(void \*obj);   
/\*4)cache创建和销毁\*/ const char\*name; struct list_head list; int refcount; int object_size; int align; struct kmem_cache_node\*\*node;   
//5)早先的版本，这一项只有NR_CPUS大小，并且在kmem_cache的最开始， struct array_cache\*array[NR_CPUS + MAX_NUMNODES];

描述slab对象的结构体是struct slab，其定义如下：

```c
struct slab {
    union {
        struct {
            struct list_head list; //链入slab_full、slab_partial、slab_free之一的链表中。
            unsigned long colouroff; //着色偏移，见图4.2
            void *s_mem; /*指向slab地址*/
            unsigned int inuse; /*num of obj active in slab*/
        }
    //kmem_bufctl_t数组中第一个空闲object在kmem_bufctl_t数组中的索引号，
        kmem_bufctl_t free;
        unsigned short nodeid;
    };
    struct slab_rcu_slab_cover_slab_rcu;
};
```

图4.2中还有一个比较重要的就是per-CPU slab缓存，释放的slab对象以及新分配的slab对象会优先从该表中分配或退还到该表中，如果此列表中没有可用的对象，则从 slab 链表一次取 batchcount 个对象。

```c
struct array_cache {
    unsigned int avail; //列表中当前可用的对象数目
    unsigned int limit; //列表中最大的对象数目，通mem_cache->limit。
    unsigned int batchcount; //通mem_cache->batchcount
    unsigned int touched;
    spinlock_t lock;
    void *entry[]; //存放被释放的slab对象的数组，放在末尾好对齐
};
```

# 4.4.2 slab 创建

Slab 的创建函数定义如下，这个版本的 slab 融入了 memory cgroup 技术，cgroup 和namespace 的组合得到了轻量级虚拟化技术 linux container (LXC)，不过这里 memory cgroup就不再这里叙述了，其使用可以参考《精通 linux 内核必会的 75 个绝技》的第 12 个绝技<使用 Memory Cgroup 限制内存使用量>：

```txt
mm/slab_comm.c  
struct kmem_cache*  
kmem_cache_create(const char *name, size_t size, size_t align,
```

```c
unsigned long flags, void (*ctor)(void *))  
{ return kmem_cache_create_memcg(NULL, name, size, align, flags, ctr, NULL); } 
```

kmem_cache_create_memcg 函数参数的意义如下：

name：/proc/slabinfo 显示的 slab 名称字符串

size：该 cache要创建的对象大小

align：对象对其

flag： SLAB 标志

ctor：对象的构造函数，源于面向对象技术。

返回指向该 slab 的指针，如果失败返回 NULL，中断环境不能调用该函数，但是该函数可以被中断，ctor 是当该 slab被创建时要调用的函数。

Flag 的标志如下：

SLAB_POSION：使用（a5a5a5a5）对 slab 进行填充，这样在分配 slab 时，如果上述填充字段发生改变，则可以知道发了非法访问。

SLAB_RED_ZONE：在 slab 对象的开始和结束处增加额外的区域，这样检查 slab 使用时缓存溢出问题。

SLAB_HWCACHE_ALIGN：将该 cache 对象和硬件 cache 行对齐。

```c
mm/slab.c
struct kmem_cache *
kmem_cache_create_memcg(struct mem_cgroup *memcg, const char *name, size_t size,
					 size_t align, unsigned long flags, void (*ctor)(void *),
					 struct kmem_cache *parent_cache)
\{
	 struct kmem_cache *s = NULL;
	int err = 0;
	mutex_lock(&slab_mutex);
	//下面一行实际上等价于 kmem_cache_alloc(k, flags | __GFP_ZERO), 成功则获得 kmem_cache 对象, 这个函数见后文
	s = kmem_cache_zalloc(kmem_cache, GFP_KERNEL);
	if (s) \{
		s->object_size = s->size = size;
		s->align = calculate Alignment(flags, align, size);
		s->ctor = ctr;
	//kmem_cache 名称字符串赋值
	s->name = kstrdup(name, GFP_KERNEL);
	.err = __kmem_cache_create(s, flags); //创建一个 slab cache
	s->refcount = 1; //增加引用计数。
	//见图 4.2, 所有 slab 链链接到了 slab caches 链表上统一管理
(list_add(&s->list, &slab_caches);
} else//出错的返回值
(err = -ENOMEIM;
out.locked:
	mutex_unlock(&slab_mutex);
.put_online_cpus(   );
	return s;
\} 
```

由上面函数可以看出主要的工作放在了 kmem_cache_alloc 和__kmem_cache_create 这两个函数。kmem_cache_alloc 实际上是对 slab_alloc 的封装，直接来看 slab_alloc 好了。

mm/slab.c   
static __always_inline void \*   
slab_alloc(struct kmem_cache *cachep,gfp_t flags, unsigned long caller)   
{ unsigned long save_flags; void \*objp; flags $= =$ gfp Allowed_mask;

local_irq_save(save_flags); objp $=$ _do_cache_alloc(cachep, flags);//执行slab分配。 local_irqRestore(save_flags); //清零操作。 if (unlikely((flags&__GFP_ZERO)&&objp)) memset(objp,0,cachep->object_size); return objp;   
}

对应 NUMA 和 UMA 两种情况，____cache_alloc 的定义处理是不一样的，这里简化只看UMA 的情况。

mm/slab.c  
static inline void *cache_alloc(struct kmem_cache *cachep, gfp_t flags)  
{void *obj;struct array_cache *ac;bool force_refill $=$ false;  
// cpu_cache_get 在 UMA 情况下退化为 cachep->array[smp Processor_id()], 见图 4.2。ac $=$ cpu_cache_get(cachep);if (likely(ac->avail)){//很可能该 slab 是有空余的（即 avail>=1）ac->touched $= 1$ //存取过了，跟新标志  
//这里要获得 obj 对象。实际上只要这么一句就可以从 slab 中获取对象了。objp $=$ ac->entry[-ac->avail];objp $=$ ac_get_obj(cachep, ac, flags, false);if(objp){//成功获得，则更新 cache 分配命中计数直接返回STATS_INC_ALLOCHIT(cachep);goto out;}force_refill $=$ true;  
}STATS_INC_ALLOCMISS(cachep);//更新 cache 分配没有命中统计信息  
//从原 slab 中分配 batchcount 个到 per-CPUcache 上objp $=$ cache_alloc_refill(cachep, flags, force_refill); /\*  
///ac $=$ cpu_cache_get(cachep);  
out:if(objp)kmemleak_lease(&ac->entry[ac->avail]);return objp;

在 per-CPU 缓存中没有 cache 的对象时，将由 cache_alloc_refill 对 per-CPU 缓存进行充填。

```c
mm/slab.c  
static void *cache_alloc_refill(struct kmem_cache *cachep, gfp_t flags, bool force_refill)  
{  
...  
while (batchcount > 0) {  
//选择获取对象slab的链表（首先是slabs_partial，然后是slabs_free）  
...  
slabp = list_entry(entry, struct slab, list);  
check_slabp(cachep, slabp);  
check_spinlock_acquired(cachep);  
while (slabp->inuse < cachep->num && batchcount--){  
//下面是对ac->entry[ac->avail++] = objp;的封装，获取对象的指针  
ac_put_obj(cachep, ac, slab_get_obj(cachep, slabp, node));  
}  
check_slabp(cachep, slabp);  
/*将slab移动到正确的slab链表上*/  
list_del(&slabp->list);  
if (slabp->free == BUFCTL_END)  
list_add(&slabp->list, &n->slabs_full); 
```

```txt
else list_add(&slabp->list, &n->slabs_partial); } 
```

到这里能获得slab对象（obj）了，不过这一过程还没结束，前面还遗留了slab 创建函数__kmem_cache_create。

mm/slab.c  
int __kmem_cache_create(struct kmem_cache *cachep, unsigned long flags)  
{size_t left_over, slab_size, align;gfp_t gfp;int err;size_t size $=$ cachep->size;/\*将slab的对象大小按照字方式对其/if(size&(BYTES_PER_WORD-1)){size+= (BYTES_PER_WORD-1);size $\& =$ ~(BYTES_PER_WORD-1);}/*危险区和用户存储区需要按字或者更大的方式进行对齐，但是可以被体系结构或者用户强制设置（设置值大于字长度）更改。\*/if flags&SLAB_STORE_USERrlalign $=$ BYTES_PER_WORD;if flags&SLAB_REDZone){ralign $=$ REDZONEALIGN;rsize $+ =$ REDZONE ALIGN-1;rsize $\& = \sim$ (REDZONE ALIGNN-1);}/\*3)调用者强制设置对齐长度\*/if (ralign $<$ cachep->align){ralign $=$ cachep->align;//存储对齐值。cachep->align $=$ ralign;//下面这行对对 cachep->node $=$ (struct kmem_cache_node \*\*)&cachep->array[nr_cpu_ids]的封装，其取CPU后第一个数组指向的成员作为kmem_cache指向的节点。见图4.2。setup_node_pointer(cachep);/\*如果对象的长度大于页帧的1/8，则将头部管理数据存储在slab之外（即CFLGS_OFF_SLAB)，否则存储在slab上，启动阶段并不处理这种情况。\*/if((size $\coloneqq$ (PAGE_SIZE>>3))&&!slab早早_init&&!(flags&SLAB_NOLEAKTRACE))flags|=CFLGS_OFF_SLAB;//slab的大小，安装前面要求的方式进行对其。size=ALIGN(size,cachep->align);//迭代查找slab的最佳长度值（页的阶），同时也计算每个slab的对象数。其过程见后文left_over $=$ calculate_slab_order(cachep,size,cachep->align,flags);//经过上述函数之后，cachep->num是slab缓存的对象个数，这里将slab的大小按前面计算的对其值对其slab_size $=$ ALIGN(cachep->num\*sizeof(kmem_bufctl_t)+sizeof(struct slab),cachep->align);/\*如果设置了CFLGS_OFF_SLAB标志，但是剩余的空闲大于slab大小，则需要将slab控制结构和slab对象放在一起。\*/if (flags&CFLGS_OFF_SLAB&&left_over $\coloneqq$ slab_size){flags $\& =$ ~CFLGS_OFF_SLAB;left_over $\coloneqq$ slab_size;}if (flags&CFLGS_OFF_SLAB){/\*如果slab控制结构和slab对象确实需要分开存放，则不需要手动设置对其\*/slab_size=cachep->num\*sizeof(kmem_bufctl_t)+sizeof(struct slab);}

/*下面这行的意义是将缓存行的大小最为着色偏移，由于 slab 在内存页的布局非常相似，这就意味着同一种类型的 slab对象很可能被映射到同一个缓存行中，这样存在的多个 slab 对象间因为频繁的刷入缓存和刷出缓存而导致性能降低，通过着色的方式，修改同类型 对象的页内偏移，可以使它们在被放到缓存中时位于不同的缓存行，这样带来的好处是不需要为映射新的slab对象而刷出先前存放的slab对象。不刷新缓存的话，TLB失效带来的开销也会降低*/cachep->colour_off $=$ cache_line_size();/* 着色偏移必须是对齐的倍数*/

```c
if (cachep->colour_off < cachep->align)  
    cachep->colour_off = cachep->align;  
    cachep->colour = left_over / cachep->colour_off; //这里计算颜色值  
    cachep->slab_size = slab_size; //slab对象大小  
    cachep->flags = flags;  
    cachep->allocflags = 0;  
    cachep->size = size;  
    cachep->reciprocal_buffer_size = reciprocal_value(size); //获得倒数，以乘法替代除法  
//slab对象和slab控制结构不放在一起的slab分配方式。  
if (flags & CFLGS_OFF_SLAB) {  
//从kmalloc_caches数组中获取一个slab控制结构  
    cachep->slabp_cache = kmalloc_slab(slab_size, 0u);  
}  
//c初始化上述申请的slab控制结构  
err = setup_cpu_cache(cachep, gfp);  
return 0;
```

calculate_slab_order 的参数意义如下：

cachep 将要创建的 kmem_cache 对象， size：slab 对象的大小， align：slab 对象要求的对其值，flags：slab分配标志。

```c
mm/slab.c  
static size_t calculate_slab_order(struct kmem_cache *cachep,  
size_t size, size_t align, unsigned long flags)  
{  
unsigned long offslab_limit;  
size_t left_over = 0;  
int gfporder;  
for (gfporder = 0; gfporder <= KMALLOC_MAX_ORDER; gfporder++) {  
unsigned int num;  
size_t remainder;  
//查找最优的过程是在 cache Estimate 中完成的，num 是计算得到的 slab 对象数，是 0 则放不下一个 slab 对象。  
cache Estimate(gfporder, size, align, flags, &remainder, &num);  
if (!num)  
continue;  
//slab 头和 slab 对象分开存放时，slab 的大小需要减去 slab 头长度，  
if (flags & CFLGS_OFF_SLAB) {  
offslab_limit = size - sizeof(struct slab);  
offslab_limit /= sizeof(kmem_bufctl_t);  
//如果由 cache Estimate 计算得到的 slab 对象数大于 slab 能存放的最大对象数，则返回，不再尝试更大的 order。  
if (num > offslab_limit)  
break;  
}  
/* 保存合适的 slab 缓存块的相关信息*/  
cache->num = num;  
cache->gfporder = gfporder;  
left_over = remainder;  
/* SLAB_RECLAIMACCOUNT 表示 slab 所占用的页面时可回收的，当内核检测是否有足够的页面满足用户态的需求时，此类页面被计算在内。由于可回收性而不需要做碎片检测 */  
if (flags & SLAB_RECLAIMACCOUNT)  
break;  
/*申请 slab 对象的数据较大是好的，但是太大的 slab 对于 gfp()s 之类的函数并不友好，所以这里限制了申请的最高阶为 slab_max_order，该值目前设置值是 0，以后很可能因为申请高阶页内存变得方便，该值而有所提升。*/  
if (gfporder >= slab_max_order)  
break;  
/* slab 所占的内部未用空间小于申请空间 1/8，则可接受*/  
if (left_over * 8 <= (PAGE_SIZE << gfporder))  
break;  
}  
return left_over;
```

# 4.4.3 slab 对象释放

Slab 对象的释放由 kmem_cache_free 完成，该函数实际上是对__cache_free 的封装，释放时分为两种情况，一种是释放的 slab对象给per-CPU缓存对象，另一种情况是返回给 slab分配器（返回的量是 array_cache->batchcount 个），如果返回给 slab 分配器则需要将最不可能未来要被使用到的对象返回给slab分配器，而cache_flusharray正是对slab对象进行排序，将要被返回和 slab 分配器的 slab 对象存放在数组的首部，将首部对象（batchcount 个）返回，后面的对象向前移。

![](images/2bd2eb396439887070f425fa240d713a250cf6b3f92f6e182a7978f8280ce07c.jpg)  
图 4.3 kmem_cache_free 代码流程

mm/slab.c  
static inline void __cache_free(struct kmem_cache *cachep, void *objp, unsigned long caller)  
{struct array_cache *ac = cpu_cache_get(cachep);if (likely(ac->avail < ac->limit)){STATS_INC-FreeEHT(cachep);//per-CPU数组中有空闲可用，则简单更新计数} else {//per-CPU数组中无空闲可用，则刷新miss计数，同时按先进先出原理排序per-CPU数组STATS_INC_FREEMISS(cachep);//cache Flusharray(cachep,ac);1//下面的函数可以看成是对ac->entry[ac->avail $+ + ] =$ objp;的封装。ac_put_obj(cachep,ac,objp);//具体的释放函数。}  
static void cacheFlusharray(struct kmem_cache *cachep, struct array_cache *ac){int batchcount;struct kmem_cache_node \*n;int node $=$ numa_mem_id();batchcount $=$ ac->batchcount;n $=$ cachepe->node[node];spin_lock(&n->list_lock);//判断node share的slab空闲大小，这样可以将属于cpu的slab扔给nodeshare的slab来管理，而不需要实际的回退给kmem_cacheif(n->shared){struct array_cache\*shared_array $\equiv$ n->shared;int max $=$ shared_array->limit - shared_array->avail;if (max){if (batchcount $>$ max)batchcount $=$ max; //由share的数组来接管per-CPU数组memcpy&(shared_array->entry[shared_array->avail]),ac->entry,sizeof(void\*)\*batchcount);//上移avail，间接实现了先进先出shared_array->avail $+ =$ batchcount;goto free_done;

}   
1   
//如果到这里说明share数组也没法接管释放的slab，则需要将cpu管理的 free_block(cachep,ac->entry,batchcount,node); free_done: spin_unlock(&n->list_lock); ac->avail $=$ batchcount; //将cpu上的slab对象向前移动batchcount个。 memmove(ac->entry,&(ac->entry[batchcount]),sizeof(void\*)*ac->avail);   
}   
static void free.block(struct kmem_cache\*cachep,void\*\*objpp,int nr_objects, int node)   
{ int i; struct kmem_cache_node \*n;   
//遍历objpp中的所有slab对象 for $(i = 0;i <   n r_{-}$ objects;i++) { void\*objp; structslab\*slabp; objp $\equiv$ objpp[i];   
//将虚拟地址转换成指向structslab对象的指针，structpage的lru prevail就指向slab。 slabp $\equiv$ virt_to_slab(objp); n $\equiv$ cachep->node[node],//取指定的node。 list_del(&slap->list);//将该slab从原链表删除。   
//修正slab缓存的bufferctl数组。 slab_put_obj(cachep,slabp,objp,node); n->free_objectse++; /*修正slab链表*/ if(slab->inuse $= = 0$ ）{   
//如果空闲对象数大于大于空闲对象数的界限，则需要进行slab回收。 if(n->free_objectse>n->free_limit){   
//释放slab管理结构体空间以及所有slab对象占用的空间。 n->free_objectse $=$ cachep->num; slabdestroy(cachep,slabp); }else{ list_add(&slap->list,&n->slabs_free); } } else{ /\*无条件将一个slab添加到partial链表的末尾 \*/ list_addTAIL(&slap->list,&n->slabs_partial); }   
}   
}

# 4.4.4 kmalloc

Kmalloc属于通用的 slab缓存申请，是没有名称的，但是和有名称的slab申请过程的差异不大，且主要调用的函数的功能和代码已经在前面有分析了，这里只粘贴其封装的_do_kmalloc 函数了。

static __always_inline void *__do_kmalloc(size_t size, gfp_t flags, unsigned long caller)   
{ struct kmem_cache \*cachep; void \*ret; cachep $=$ kmalloc_slab(size, flags); if (unlikely(ZERO_OR_NULL_PTR(cachep))) return cachep; ret $=$ slab_alloc(cachep, flags, caller); return ret;

# 第五章 进程虚拟内存

进程的虚拟地址空间和内核的虚拟地址管理方法不一样，不论应用程序如何切换，内核始终是一个并且其一直驻留在内存中，而进程则不同，可以有多个进程同时驻留在内存中，并且从各个进程的角度来看，呈现的系统是一样的，并且它们并不会彼此干扰。

有一篇文章，《linux 应用程序如何运行》分析的是应用程序调用 execve()执行系统调用时发生的一些事，该文章有助于理解本章内容，图 5.1 的右下角给出了 execve的主要功能。

# 5.1 进程准备知识

各进程的虚拟地址空间从 0 开始，最大到 TASK_SIZE-1，这一范围就是通常所述的 3G，从 $3 6 \sim 4 6 \cdot 1$ 是内核的地址空间，一个进程通常包括如下几个部分：

$\bullet$ 二进制可执行代码，比如 hello.c 编译得到的可执行代码，内核中将其称之为 text。  
$\bullet$ 程序使用的动态库，.so结尾的文件，该库是对系统调用的封装。  
$\bullet$ 堆，存储全局和动态产生的数据。  
$\bullet$ 栈，局部变量以及函数调用时对寄存器和返回地址的保存。  
$\bullet$ 环境变量和命名参数  
$\bullet$ 将文件内容映射到虚拟地址空间。

来看一个小例子：

```c
hello.c  
#include<stdio.h>  
#include<unistd.h>  
void main(void)  
{printf("\nHello,world \~!\\n");sleep(15);printf("Goodby,world \~!\\n");} 
```

编译

```txt
gcc -o hello hello.c 
```

执行及输出结果

```txt
ge@u:\~\\(. /hello &   
[1] 7931   
ge@u:\~\\(   
Hello,world \~!   
Goodby,world \~!   
[1]+ Exit 17 ./hello   
ge@u:\~\\) 
```

进程的内存使用情况，也可以使用 pmap PID 命令查看

```shell
ge@u:\~\$ cat /proc/7931/maps   
08048000-08049000 r-xp 00000000 08:01 1447992 /home/ge/hello   
08049000-0804a000 r--p 00000000 08:01 1447992 /home/ge/hello   
0804a000-0804b000 rw-p 00001000 08:01 1447992 /home/ge/hello   
b7545000-b7546000 rw-p 00000000 00:00 0   
b7546000-b76ef000 r-xp 00000000 08:01 411089 /lib/i386-linux-gnu/libc-2.19.so   
b76ef000-b76f000---p 001a9000 08:01 411089 /lib/i386-linux-gnu/libc-2.19.so   
b76f000b76f2oOo r--p 001a9Ooo 08:01 411O89 /lib/i386-linux-gnu/libc-2.19.so   
b76f2oOo-b76f3Ooo rw-p 0o1abOoo 08:01 411O89 /lib/i386-linux-gnu/libc-2.19.so   
b76f3Ooo-b76f6Ooo rw-p 0oOooOOOO 0o:Oo O   
b77oCoo-b77oFoo rw-p 0oOooOOOO 0o:Oo O   
b77oFoo-b771Ooo 1r-xp 0oOooOOOO 0o:Oo O [vdso]   
b771Oooo-b773Oooo r-xp 0oOooOOOO 08:O1 411O98 /lib/i386-linux-gnu/ld-2.19.so   
b773Oooo-b7731Oooo r--p 0oO1fOoo 08:O1 411O98 /lib/i386-linux-gnu/ld-2.19.so   
b7731Oooo-b7732Oooo rw-p 0oO2OooOO 08:O1 411O98 /lib/i386-linux-gnu/ld-2.19.so 
```

bf8e7000-bf908000 rw-p 00000000 00:00 0 [stack]

每个应用程序的起始地址都是一样的 0x08048000（IA-32 架构），如下查看cat进程的内存布局。

```txt
ge@u:\~\$ cat /proc/self/maps   
08048000-08053000 r-xp 00000000 08:01 1966103 /bin/cat   
08053000-08054000 r--p 0000a000 08:01 1966103 /bin/cat   
08054000-08055000 rw-p 0000b000 08:01 1966103 /bin/cat   
089c0000-089e1000 rw-p 00000000 00:00 0 [heap]   
b7223000-b7388000 r--p 001c8000 08:01 1188875 /usr/lib/local/locale-archive   
b7388000-b7588000 r--p 00000000 08:01 1188875 /usr/lib/local/locale-archive   
b7588000-b7589000 rw-p 00000000 0o:oo 0   
b758900o-b7732OOO r-xp 0oOoOooOoo 08:O1 411O89 /lib/i386-linux-gnu/libc-2.19.so   
b7732Oooo-b7733Oooo --p 0o1a9Ooo 08:O1 411O89 /lib/i386-linux-gnu/libc-2.19.so   
b7733Oooo-b7735Oooo r--p 0o1a9Ooo 08:O1 411O89 /lib/i386-linux-gnu/libc-2.19.so   
b7735Oooo-b7736Oooo rw-p 0o1abOoo 08:O1 411O89 /lib/i386-linux-gnu/libc-2.19.so   
b7736Oooo-b7739Oooo rw-p 0oOooOOoo 0o:oo 0   
b774fOooo-b775Oooo r--p 0o855Oooo 08:O1 1188875 /usr/lib/local/locale-archive   
b775Oooo-b7752Oooo rw-p 0oOooOOoo 0o:oo 0   
b7752Oooo-b7753Oooo r-xp 0oOooOOoo 0o:oo 0 [vdso]   
b7753Oooo-b7773Oooo r-xp 0oOooOOoo 08:O1 411O98 /lib/i386-linux-gnu/ld-2.19.so   
b7773Oooo-b7774Oooo r--p 0oO1fOoo 08:O1 411O98 /lib/i386-linux-gnu/ld-2.19.so   
b7774Oooo-b7775Oooo rw-p 0oO2oOooo 08:O1 411O98 /lib/i386-linux-gnu/ld-2.19.so   
bfefeoo-bff1fOOrw-p 0oOooOOOO oo:oo [stack] 
```

它们对应的内存布局情况如图 5.1 所示。Vdso（virtual Dynamic Shared Object）是一个比较小的共享库，内核动态将该库映射到所有应用程序的地址空间中。这主要是因为 glibc 更新太慢导致的，其详细的简介请看 http://www.man7.org/linux/man-pages/man7/vdso.7.html。

![](images/c7792204080933bc2ed42fab0a34808a2f2f8a049d495eb87b8420d8a774ee04.jpg)  
图 5.1 虚拟内存布局

每个进程都由一个 struct task_struct 结构体表示，其内存信息保存在 struct mm_struct成员中，下面的结构体的部分成员的意义结合图5.1 看更为清晰。

```c
struct mm_struct{ struct vm_area_struct \* mmap; /\*虚拟内存区域列表\*/ struct rb_root mm_rb; struct vm_area_struct \* mmap_cache; /\*最近一次find_vma的结果\*/ #ifdef CONFIG_MMU unsigned long (\*get_unmapped_area) (struct file \*filp, unsigned long addr, unsigned long len, unsigned long pgoff, unsigned long flags); void (\*unmap_area) (struct mm_struct \*mm, unsigned long addr); #endif unsigned long mmap_base; /\*Memory Mapping Segment 起始地址\*/ unsigned long task_size; /\*进程虚拟地址空间大小，通常是3G\*/ /\*if non-zero,the largest hole below free_area_cache\*/ unsigned long cached_hole_size; /\* first hole of size cached_hole_size or larger\*/ unsigned long free_area_cache; unsigned long highest_vm_end; /\*highest vma end address\*/ pgd_t \*pgd; atomic_t mm_users; /\*How many users with user space? \*/ atomic_t mm_count; /\*对该结构体的引用计数\*/ int map_count; /\*number of VMAs\*/ spinlock_t page_table_lock; /\*Protects page tables and some counters\*/ struct rw_semaphore mmap_sem; struct list_head mmlist; /\*List of maybe swapped mm's. These are globally strung \* together off init_mm.mmlist,and are protected \* by mmlist_lock \*/ unsigned long hiwater_rss; /\*High-watermark of RSS usage \*/ unsigned long hiwater_vm; /\*High-water virtual memory usage \*/ unsigned long total_vm; /\*Total pages mapped \*/ unsigned long locked_vm; /\*Pages that have PG_mlocked set \*/ unsigned long pinned_vm; /\*Refcount permanently increased \*/ unsigned long shared_vm; /\*Shared pages (files)\*/ unsigned long exec_vm; /\*VM_EXEC&~VM_WRITE\*/ unsigned long stack_vm; /\*VM_GROWSUP/DOWN\*/ unsigned long def_flags; unsigned long nr_ptes; /\*Page table pages\*/ //见图5.1右侧，start_code、end_code代码段起止，start_data、end_data数据段起止。 unsigned long start_code,end_code,start_data,end_data; //堆 unsigned long start_brk,brk,start_stack; //参数和环境变量 unsigned long arg_start,arg_end,env_start,env_end; unsigned long saved_auxv[AT_VECTOR_SIZE];/\*for /proc/PID/auxv\*/ /\* *Special counters,in some configurations protected by the \*page_table_lock,in other configurations by being atomic. \*/ struct mm_rss_stat rss_stat; struct linux_binfmt \*binfmt; cpumask_var_t cpu_vm_mask_var; /\*Special counters,in some configurations protected by the \*page_table_lock,in other configurations by being atomic. \*/ struct mm_rss_stat rss_stat; 
```

```sql
struct file *exe_file;
struct uprobes_state uprobes_state; 
```

mm_struct 和 VMA 的关系如 5.2 所示。

![](images/c68aac4959987bf3fa69b7588f28fc0806917947412cd886ff6812e6c597d297.jpg)  
图 5.2 进程管理的 vm_area_struct 与进程虚拟地址空间的关联

```c
struct vm_area_struct {
    /* 以下两个成员的信息可用于遍历VMA树，其存储在第一缓存行中*/
    unsigned long vm_start; /* Our start address within vm_mm. */
    unsigned long vm_end; /* 在vm_mm内结束地址之后的一个字节*/
    /* 各进程的VM（虚拟内存）链表，按地址升序排列，vm_RB是红黑树的管理*/
    struct vm_area_struct *vm_next, *vm Prev;
    struct rb_node vm_RB;
    /* 
        *该虚拟内存区的左侧最大空闲内存间隙，该间隙要么是VMA和vma->prev之间的间隙，要么就是该VMA之下的VMA去的红黑树和其自身的vm-Prev的间隙，其作用是帮助get_unmapped_area找到一个大小合适空闲的空间。*/
    unsigned long rb_subtree_gap;
    /* 第二个cache的缓存行存储从此开始.*/
    struct mm_struct *vm_mm; /* vm_area_struct属于的地址空间*/
    pgprot_t vm_pageprot; /* Access permissions of this VMA. */
    unsigned long vm_flags; /* Flags, see mm.h. */
    /*对于有地址空间和后备存储器的区域将被链接到address_space-i_mmap优先树或者被链接到address_space-i_mmap_nonlinear链表上*/
    union {
        struct {
            struct rb_node rb;
            unsigned long rb_subtree_last;
        } linear;
        struct list_head nonlinear;
    } shared;
```

/*在对文件的某一页执行了 COW（copy on write）操作之后，一个文件的 MAP_PRIVATE 虚拟内存区可能在 i_mmap树和 anon_vma 链表这两个地方，一个 MAP_SHARED 类型的虚拟内存区只能映射到 i_mmap 树。一个匿名 MAP_PRIVATE 虚拟内存区，栈或者堆只能被映射到 anon_vma 链表。*/

```c
struct list_head anon_vma_chain; /* mmap_semp & page_table_lock 确保存取串行*/
struct anon_vma *anon_vma; /*page_table_lock 确保存取串行（锁防止并发）*/ 
```

/* 处理这个结构体的函数操作集，缺页异常时的处理函数，就是该操作集的fault 指针*/const struct vm_operations_struct *vm_ops;

```c
/*后备存储器的信息*/  
unsigned long vm_pgoff; /* Offset (within vm_file) in PAGE_SIZE units, *not* PAGE_CACHE_SIZE */  
struct file *vm_file; /* 指向映射的文件，如果没有则为 NULL*/  
void *vm_private_data; /* was vm_pte (shared mem) */  
struct vm_region *vm_region; /* NOMMU mapping region */ 
```

# 5.2 文件和虚拟内存

![](images/17e0878c14467dcd6085ad59c3ff1e6e7a8fee30a7781a1725f8b95a44065668.jpg)  
图5.3 文件和虚拟地址空间的映射关系

struct address_space 的 i_mmap 成 员 指 向 的 是 私 有 和 共 享 映 射 的 红 黑 树 ， 而i_mmap_nonlinear 则是 VM_NONLINEAR 映射的链表，它们链表的指向的成员对象均是vm_area_struct，

# 5.3 虚拟内存区操作

由 5.1 和 5.2 节可以看出，虚拟内存和 struct vm_area_struct 关系还是挺大的，这节就来看看和 vm_area_struct 相关的一些操作。

# 5.3.1 find_vma

在5.1节提到struct mm_struct的mmap_cache成员存放的是最近一次find_vma的结果。该函数用于查找用户空间地址中结束地址在给定地址之后的第一个区域，即满足addr>vm_area_struct->vm_end 条件的第一个区域。

struct vm_area_struct *find_vma(struct mm_struct *mm, unsigned long addr) {
    struct vm_area_struct *vma = NULL;
    /* 首先查找查找 cache，命中率约 $35\%$ */
    vma = ACCESS_ONCE(mm->mmap_cache);
    if (!(vma && vma->vm_end > addr && vma->vm_start <= addr)) {
        //cache 没有命中，则执行下面的查找。
        struct rb_node *rb_node;
        //获得红黑树根节点
        rb_node = mm->mm_rb_rb_node;
        vma = NULL;
        while (rb_node) {
            //不要陌生，linux 内核的 c 文件早就可以在使用一个变量时再定义了
            struct vm_area_struct *vma_tmp;
            vma_tmp = rb_entry(rb_node, struct vm_area_struct, vm_rb);
        }
    }
}

//如果获得的 vma 去的结束地址大于指定的地址，这就意味着找到了合适的 vma 区，否则决定是从左子树还是右子树开始查找，有点类似于二分查找。

```c
if (vma_tmp->vm_end > addr) {  
    vma = vma_tmp;  
    if (vma_tmp->vm_start <= addr)  
        break;  
    rb_node = rb_node->rb_left;  
} else  
    rb_node = rb_node->rb_right;  
}  
if (vma)  
    mm->mmap_cache = vma;  
}  
return vma; 
```

# 5.3.2 vma_merge

vma_merge 用于区域合并，当新插入的区域可以和一个或多个区域合并时，合并操作将被执行。给定一个映射需求（addr，end，vm_flags，file，pgoff），判断是否可以进行区域合并。绝大多数情况下，当调用 mmap，brk 或者 mremap 时，vma_merged 地址范围参数[addr,end)肯定还没被映射；但是当调用 mprotect 时，这段区域肯定被映射了。prev 是紧邻着新区域之前的区域，addr、end以及 vm_flags分别是新区域的开始地址、结束地址以及标志。如果是一个文件映射，file还会指向struct file实例，最后 pgoff 反映的是映射在文件数据内的偏移量。先来说明这一过程，首先截取图5.2 的一部分。

![](images/924d72632ae843eb6ce822a7f2ae765e04881cd7193489f6e47918b2176caa71.jpg)

![](images/ae27ee38570906da6b02a1510bab21c08d9bfd40521f4069f87e1ee8feed83cf.jpg)  
图 5.4 VMA 和插入 VMA 关系

根据原VMA 和插入VMA的关系分为两大类，共八中情况。A表示要插入的VMA 区，P表示在要插入的VMA 区的前面，N则表示位置处在要插入的 VMA区的后面。

在所有情况中，能够合并的分为两大类，一类是前驱合并，即case1/2/3，对于 case1，P区、插入的VMA 区，以及 N区，可连接组合和一个整块VMA，case2 插入的VMA 区和P可以合并其它情况依次类推。X 意思是不关心的区域，因为这时添加进入的VMA区的end和next->vm_end是相等的，即它们是重叠的区域，所以会将N 区后移，找N的后驱作为真

正的后驱。  
```c
mm/mmap.c
struct vm_area_struct *vma_merge(struct mm_struct *mm,
						(struct vm_area_struct *prev, unsigned long addr,
						 unsigned long end, unsigned long vm_flags,
						(struct anon_vma *anon_vma, struct file *file,
						pgoff_t pgoff, struct mempolicy *policy)
\{
	pgoff_t pglen = (end - addr) >> PAGE_SHIFT;
struct vm_area_struct *area, *next;
int err;
//如果紧邻新区域之前的区域非空，将获得 next 成员，这有点类似断开链表插入一个元素，否则获得该 mm 的第一个 VMA
元素
if (prev)
		 next = prev->vm_next;
	else
		 next = mm->mmap;
area = next;
//如果存在 next 存在，并且 vma 的结束地址和给定的结束地址相同，则有重叠，需要调整 vma
if (next && next->vm_end == end) /* cases 6, 7, 8 */
		 next = next->vm_next;
/* 是否和前一个 vma 进行合并？
前一个 vma 存在（prev 非空）并且其结束地址等于新区域的开始地址。两者的 struct mempolicy（NUMA 使用）属性相同，
另外还有 vm_flags 以及映射同一个文件等属性也要一致。前驱合并
*/
if (prev && prev->vm_end == addr && mpol_equal(vma_policy Prev), policy) && can_vma_merge_after Prev, vm_flags, //如果可以合并返回 true。
(anon_vma, file, pgoff)) {
/* 代码到这里，说明可以合并。现在查看是否可以对后一个 vma 也进行合并 */
*/
if (next && end == next->vm_start && can_vma_merge_before(next, vm_flags,
				anon_vma, file, pgoff+pglen) &&is_mergeable_anon_vma(prev->anon_vma,
				>anon_vma, NULL)) {
/* cases 1, 6 */
err = vma_adjust(Prev, prev->vm_start,
				>vm_end, prev->vm_pgoff, NULL);
} else /* cases 2, 5, 7 */
err = vma_adjust(Prev, prev->vm_start,
				end, prev->vm_pgoff, NULL);
if (err)
return NULL;
khugepaged-enter_vma_merge Prev);
return prev;
}
/*如果前面的失败，则从后面的 vma 开始进行和上面类似的合并操作。后驱合并
* Can this new request be merged in front of next?
*/
if (next && end == next->vm_start &&mpolequal(policy, vm_policy(next)) &&can_vma_merge_before(next, vm_flags,
				anon_vma, file, pgoff+pglen)) {
if (prev && addr < prev->vm_end) /* case 4 */
err = vma_adjustPrev, prev->vm_start,
addr, prev->vm_pgoff, NULL);
else /* cases 3, 8 */
err = vma_adjust(area, addr, next->vm_end,
				>vm_pgoff - pglen, NULL);
if (err)
return NULL;
khugepaged-enter_vma_merge(area);
return area;
}
```

vma_adjust用于完成实际的合并操作。

# 5.3.3 insert_vm_struct

该函数用于将 vm 结构体插入按地址排列的进程链表和 inode 的 i_mmap 树中，如果vm_file 非空，则需要获得i_mmap_mutex互斥信号量。属于红黑树的插入。

int insert_vm_struct(struct mm_struct *mm, struct vm_area_struct *vma)   
{ structvm_area_struct \*prev; struct rb_node $^{**}\mathrm{rb}$ link,\*rb_parent; if(!vma->vm_file){ BUG_ON(vma->anon_vma); vma->vm_pgoff $\equiv$ vma->vm_start>>PAGE_SHIFT; } if (find_vmalinks(mm,vma->vm_start,vma->vm_end, &prev,&rb_link,&rb_parent)) return -ENOMEM; if((vma->vm_flags&VM ACCOUNT)&& security_vma_enough_memory_mm(mm,vma_pages(vma))) return -ENOMEM; vma_link(mm,vma,prev,rb_link,rb_parent); return 0;

# 5.3.4 创建区域

在插入新分配的内存区域之前，内核必须确认虚拟地址空间中是否有足够的空闲空间用于映射给定长度的区域。这一工作由 get_unmapped_area 函数完成，该函数是和体系结构是息息相关，具体体系结构实现的细节这里就不关心了。

# 5.4 创建地址区间

mmap,do_mmap

内核在创建一个新的 VMA区时，会判断和一个已存在的 VMA区是否可以合并，如果可以合并，则会合并成一个区域，而不创建一个一个新的 VMA,但如果无法合并，则会创建一个新的 VMA 区。

do_mmap 会调用 do_mmap_pgoff 完 成 实 际 的 创 建 过 程 ，该 函 数 首 先 调 用get_unmapped_area 在虚拟地址空间中找到一个适当的区域用于映射。而 calc_vm_prot_bits则是计算新 VMA 的权限标识。准备工作做好后，开始进行实际的区域映射工作，其由mmap_region来实现，mmap_region 首先检查资源是否超出进程的资源限制，如果超出限制并且是固定映射则返回，否则计算可以映射多少 VMA 区，然后调用 find_vma_links 查找是否已有现存区域，如果有则需要调用 do_munmap 将该区域腾出来给新的 VMA 区。然后尝试不创建新的 vm_area_struct 实例，即使用 vma_merge 方法尝试将新的 VMA 区域合并到已有的 vm_area_struct 实例里去，如果合并不成功，则需要创建 vm_area_struct 实例，如果是文件映射，则 file->f_op->mmap 将文件和新创建的 VMA 区关联起来。最后调用 vma_link 将新的VMA区插入到线性虚拟地址空间去。

![](images/8e3727e4f77b49bcbdc248404d21440206b0df0f2cfc5e830f75cd26b1afa825.jpg)  
图5.5 创建新区域核心流程

# 5.5 删除地址区间

mumap,do_munmap()

用于从特定的进程地址空间中删除指定的VMA 区，

mm/map.c

int do_munmap(struct mm_struct $^ { * } { \sf m m }$ , unsigned long start, size_t len)

第一个参数用于删除所在的地址空间，第二个参数指定删除的 VMA 的起始地址，第三个参数指定删除的长度。

mm/map.c   
int vm_munmap(unsigned long start, size_t len)   
{ int ret; struct mm_struct \*mm $=$ current->mm; down_write(&mm->mmap_sem); ret $=$ do_munmap(mm,start,len); up_write(&mm->mmap_sem); return ret;   
}   
EXPORT_SYMBOL(vm_munmap);   
SYSCALL DEFINE2(munmap,unsigned long,addr,size_t,len) { profile_munmap(addr); return vm_munmap(addr, len);

删除一个 VMA 区的流程如图 5.6 所示。该函数首先调用 find_vma 查找和要删除 VMA的 start 地址重叠的 VMA 区，如果没有重叠 VMA 区，则直接返回，如果需要分离 VMA 区，

则调用__split_vma 分离 VMA 区，然后调用 find_vma 判断删除区域的 end 是否也有重叠的VMA 区，有也要分离。detach_vmas_to_be_unmapped 用于将要删除的 VMA 解映射掉，其遍历所有的vm_area_struct实例线性表，直到要解除映射的地址范围已经全部涵盖在内，然后调用 unmap_region 从表页中删除与映射相关的所有项，同时内核会刷新相关的 TLB 项。最后调用 remove_vma_list 使用 vm_area_struct 实例占用的空间。

![](images/edab6f9fc812c581e54d01d62557dfdeac4aeb3aa612766a3150ba8a6d9b3752.jpg)  
图 5.6 删除 VMA 区流程