# 物理内存管理 — Buddy 分配器 + SLAB/SLUB + memblock + NUMA 节点

> Cluster A: 15 KPs | 依赖: 无 | 读者基线: C 语言基础 + 了解内存地址概念
> 读者处境: 全书开篇——没有任何前置；本篇回答"内核怎么管理物理内存：谁管大块页、谁管小对象、启动早期谁顶着"
> 打开新视角: 外部/内部碎片的分工治理、Buddy 的位运算找伙伴、SLUB 为何取代 SLAB、memblock 的启动使命、NUMA 的本地优先

---

### 概念依赖链

```
无前置 → 本篇: 物理内存管理的四大机制
  ├─ §1 Buddy 分配器(2^n 页块 — 本篇基石)
  │    ├─ §2 SLAB/SLUB(对象缓存 — 依赖 §1 的页块)
  │    ├─ §3 物理内存模型(node/zone/page — 依赖 §1)
  │    └─ §4 memblock(启动早期 — 先于 Buddy 上线)
先讲: 大块(页) → 小块(对象) → 组织(三级) → 早期(memblock)
后续依赖: 02-分页机制(物理页→虚拟地址)
```

### 叙事顺序

1. 问题引入——内核要分配 3 页、64 页、还有 100 万个"小对象"——物理内存怎么管才不浪费？（**Aha: 物理内存是'四套机制分治'——Buddy 管大块、SLUB 管小块、NUMA 管节点、memblock 管启动**）
   - 过渡: 大块页怎么管？——Buddy
2. Buddy——2^n 分组；分裂合并；位运算找伙伴
   - 过渡: 页有了——大量同尺寸小对象呢？——SLUB
3. SLAB/SLUB——对象缓存；三链表→per-CPU partial；SLUB 为何胜出
   - 过渡: 这些内存物理上怎么组织？——NUMA 三级
4. 物理内存模型——node/zone/page；Zone 分类；本地优先
   - 过渡: 启动早期（Buddy 没就绪）呢？——memblock
5. memblock——启动早期分配器；退役移交 Buddy
   - 过渡: 四机制齐了——收束
6. 收束——四机制分工 + 一条完整物理页生命周期

### 1. Buddy 分配器 — 2^n 页分裂与合并

场景提示: 驱动要 3 页、文件系统要 64 页——怎么从碎片化的物理内存里快速找到"任意大小"的连续块？ [写作时展开]

关键设计: 空闲页**按 2^n 分组**（order 0=4KB ... order 10=4MB）：

```[pseudocode]
struct free_area: free_list[MIGRATE_TYPES] + nr_free — 每 zone 11 个 order × 3 迁移类型
分裂: 要 3 页 → 拿 4 页块(2^2)拆成两个 2 页块, 取其一(递归)
合并: 释放 2 页 → 找伙伴(同 order 相邻) → 都空闲 → 合并成 4 页块(递归)
找伙伴: __find_buddy_pfn = page_pfn ^ (1 << order) — 一个异或 O(1) 定位唯一伙伴
MIGRATE_TYPES: MIGRATE_UNMOVABLE(内核核心) / MIGRATE_RECLAIMABLE(可回收) / MIGRATE_MOVABLE(用户页) — 防不可移动页成碎片钉子
```

分配流程: `alloc_pages → __alloc_pages_nodemask → get_page_from_freelist → __rmqueue`（合适 order 无空闲则分裂）。合并流程: `__free_pages → __free_one_page`（检查 buddy 空闲则升级合并）。

Why: 为什么 Buddy 能 O(1) 找到伙伴？——**2^n 分组让"伙伴"唯一确定**：同 order 相邻块只有一个（物理地址差 2^order 页）——`PFN ^ (1<<order)` 一步算出。**分裂是合并的逆操作**：拆大块满足小块、合并小块恢复大块——任何大小请求都能满足，长期运行大块不消失（抵抗外部碎片）。 [内核: 阶段1-01 篇概念版→本篇源码——free_area 结构是后续 02-内存深度 篇的深挖入口]

比喻锚点: Buddy=打包拆包盒——仓库的盒子只有 1/2/4/8/16 件规格（2^n）；要 3 件拿 4 件盒拆两半取一（分裂），用完的盒和旁边同规格空盒拼成大盒（合并）——任何件数都能凑，盒子永远有规矩。 [写作时展开]

### 2. SLAB/SLUB 分配器 — 内核对象缓存

场景提示: 内核要频繁创建/销毁同尺寸对象（task_struct/inode）——每次从 Buddy 拿整页？ [写作时展开]

关键设计: 为每种对象建缓存（mm/slab.c, mm/slub.c）：

```[pseudocode]
SLAB 三链表: kmem_cache_create → kmem_cache_alloc
  slabs_full/partial/free — 分配优先 partial, 全空归还 Buddy
  着色(coloring): 对象按缓存行错开起始位 — 减少多核伪共享
SLUB 改进(2.6.23+ 默认): 取消三链表+着色
  → per-CPU partial list + NUMA 感知 → 更少元数据更少锁
观测: /proc/slabinfo(所有缓存) / slabtop(实时排序) / kmemleak(泄漏检测)
```

Why: 为什么 SLUB 取代 SLAB？——SLAB 的三链表 + 着色 + per-node 队列在大 NUMA 机器上**锁开销高、元数据多**；SLUB 用**"每 slab 一个 freelist 指针 + per-CPU partial"**替代——分配热路径无锁（本地 freelist 直接取）、释放攒批（partial 本地攒够才回流）——**用最少元数据换吞吐**。内部碎片也顺带解决：对象槽固定大小，无整页浪费。

比喻锚点: SLUB=每位收银员的零钱格——SLAB 是"三个共用抽屉+记账本"（三链表+着色，两队人抢抽屉）；SLUB 是"每人一个零钱格"（per-CPU freelist 直接取）——不用排队等抽屉，账也少记（少元数据）。 [写作时展开]

### 3. 物理内存模型 — UMA → NUMA 三级结构

场景提示: 32 路服务器——每个 CPU 插槽有自己的内存（本地快远程慢）；老设备只能访问低 16MB——物理内存怎么组织？ [写作时展开]

关键设计: node → zone → page 三级（include/linux/mmzone.h, mm/page_alloc.c）：

```[pseudocode]
node: NUMA 节点(本地内存快/远程慢) → 距离矩阵 /sys/devices/system/node/node*/distance
zone: 同 node 内按用途/地址分区
  ZONE_DMA(低16MB, ISA遗留) / ZONE_DMA32(4GB以下) / ZONE_NORMAL(直接映射区) / ZONE_HIGHMEM(32位, 64位无)
page: 最小分配单位(4KB)
本地优先: preferred_nid → zonelist 按距离回退
numactl --hardware(拓扑) / numastat -p PID(命中率)
```

Why: 为什么"本地优先"？——**访问延迟不对称**：本地内存快（同 socket 总线）、远程内存慢（跨 QPI/UPI）。分配先找本地 node（preferred_nid），不够才沿 zonelist（按距离排序）回退远程——**把"哪个节点的内存"作为分配的第一决策**，比 Buddy 的"哪块空闲"更优先。ZONE_NORMAL 直接映射区让内核 `__va` 一步访问物理内存（零页表，02 篇展开）。 [内核: ZONE_NORMAL 直接映射=物理地址线性偏移, 02 篇页表的物理基础]

比喻锚点: NUMA=多仓发货——你家（本地 node）仓库存货就近发（快）；本地没货才调隔壁城市仓库（远程 node，要过路费）；老货号（ISA 设备）只能从老仓库（ZONE_DMA）发。 [写作时展开]

### 4. memblock — 启动早期分配器

场景提示: 开机那一刻——Buddy/SLUB 还没初始化，内核却要分配内存（页表/内核镜像）——谁顶着？ [写作时展开]

关键设计: memblock = 启动早期的极简分配器（mm/memblock.c）：

```[pseudocode]
struct memblock: memory(可用区域, 来自 e820/DT) + reserved(已预留)
memblock_add: 注册可用区域 / memblock_reserve: 标记保留 / memblock_alloc: 分配
退役: mm_init → memblock_free_all → 未保留区域批量移交 Buddy → memblock 退役
调试: memblock=debug 内核参数 / /sys/kernel/debug/memblock/reserved
```

Why: 为什么需要 memblock？——**启动早期 Buddy 未就绪**：没有 zone、没有 free_area、没有 SLUB，但已经要分配（页表、内核镜像、驱动）——memblock 用**两个数组**（memory/reserved）极简管理：登记"哪些区域可用"、分配时从"未预留"里划。**它是 Buddy 的"启动代理"**——mm_init 后一次性把剩余区域移交 Buddy（memblock_free_all），功成身退。

比喻锚点: memblock=临时工棚——大部队（Buddy）还没到，先搭个工棚（memblock 两数组）管紧要的事（页表/内核镜像）；大部队一到（mm_init）就清点物资移交（memblock_free_all），工棚拆掉（退役）。 [写作时展开]

### 5. 收束

回到"内核怎么管物理内存"：
- Buddy = 大块页（2^n 分组，位运算找伙伴）
- SLUB = 小块对象（per-CPU partial，无锁热路径）
- NUMA = 三级组织（本地优先）
- memblock = 启动早期（Buddy 的代理）

一条完整物理页生命周期：

```[pseudocode]
启动: e820 → memblock 注册 → 预留内核镜像
建 Buddy: mm_init → memblock_free_all → 未保留区域成为 free_area
运行: kmalloc → SLUB 缓存 → 无槽 → Buddy 取 2^n 页 → 切成对象槽
      mmap → alloc_pages → rmqueue → 用户页(MOVABLE)
释放: SLUB 还槽 → slab 全空 → 还 Buddy → 合并 → 大块重生
```

**Aha Moment**: "物理内存不是'一块大内存'，是四套机制分治：Buddy 管'多大的页'（2^n 拆合）、SLUB 管'多小的对象'（固定槽）、NUMA 管'哪个节点的'（本地优先）、memblock 管'启动那会儿'（代理）。**碎片的两种形态——外部（页块间）由 Buddy 治、内部（页内对象间）由 SLUB 治**——分工是内核内存管理的底色。"
**回答读者三问**: ①任意大小内存怎么给=Buddy 拆大块；②小对象怎么省=SLUB 对象缓存；③启动早期谁分配=memblock 代理。

---

### 核心悬念

**"物理页有了，进程怎么看到一个连续的 4GB 虚拟地址空间？"**

→ 引出 02-分页机制 + 四级/五级页表 + PTE 标志位 + TLB——Buddy 给的是碎片化的物理页，下一篇讲怎么"伪装"成连续虚拟空间。
