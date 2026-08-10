# PoolChunk 与 PoolArena — Netty 的四级内存分配

## 概念依赖链

```
Q1 (Chunk handle) → Q2 (Subpage bitmap) → Q3 (Arena ChunkList)

Q1 编码"怎么找到这块内存" → Q2 管理"小块怎么切" → Q3 组织"上千个 Chunk 怎么找"
```

## 叙事顺序

1. **问题引入** — 从 Ch4 ByteBuf 过渡
   - Ch4 讲了 PooledByteBufAllocator 的三层架构——但 Chunk/Subpage 内部发生了什么未展开
   - 一个 4MB Chunk — 怎么切成 16KB 的 run？怎么切成 64B 的 subpage 元素？上千个 Chunk 怎么管理？

2. **Q1：PoolChunk — handle 编码的革命**
   - 64bit handle (`PoolChunk.java:76-86`): offset(15)+size(15)+isUsed(1)+isSubpage(1)+bitmapIdx(32) — 一次读取 = 全部分配信息
   - runsAvail: `PriorityQueue<Integer>[]` (`PoolChunk.java:163`) — 每个大小一个队列，按 offset 排序
   - `allocateRun()` (`PoolChunk.java:370-399`): `runsAvailLock.lock()` → `runFirstBestFit()` → `queue.poll()` → `splitLargeRun()` — 大了就切
   - `splitLargeRun()`: run > request → used part + trailing free → trailing 回插 runsAvail
   - runsAvailMap: `LongLongHashMap` (`PoolChunk.java:164`) — runOffset → handle O(1) 查找，free 时合并相邻 run
   - isRun/isSubpage: `handle >> IS_SUBPAGE_SHIFT & 1`
   - **不是经典 Buddy 二叉树** — PriorityQueue + firstBestFit

3. **Q2：PoolSubpage — bitmap 位图管理**
   - `maxNumElems = runSize / elemSize` (`PoolSubpage.java:74`)
   - `bitmap: long[]` — 每 long=64bit，ceil(元素数/64) = bitmapLength (`PoolSubpage.java:75-77`)
   - `nextAvail` (`PoolSubpage.java:42`): 快速路径 — 先试 nextAvail bit，已用 → `findNextAvail()` 扫描
   - `doNotDestroy` (`PoolSubpage.java:41`): 即使完全空闲也不销毁 — Arena 的 subpage 链表缓存
   - `allocate()`: test bit → set → numAvail-- → update nextAvail
   - `head.lock()` 保护 Arena 的 smallSubpagePools[sizeIdx] 链表
   - 两级分配: allocateRun (大块) + allocateSubpage (小块 bitmap)

4. **Q3：PoolArena — 6 级 ChunkList**
   - q000(0-25%)→q025→q050→q075→q100 (`PoolArena.java`)
   - `allocateNormal()`: 从 q050 开始→q025→q000→q075→q100→new chunk — 优先中等使用率
   - `free()` 后 check usage → `prevList.add(this)` — 向下晋升到更空闲队列
   - usage() = `100 - freeBytes*100/chunkSize` (`PoolChunk.java:313-323`)
   - ChunkList = 双向链表: prevList/nextList, head pointer
   - `free()` → freeBytes -= pinnedSize → new usage → move() to correct list

5. **收束**: 一级(handle 编码)→二级(subpage bitmap)→三级(arena ChunkList)→四级(allocator 调度)。从 4MB Chunk 到 64B Subpage 元素——全程 CAS + 细粒度锁 (Chunk runsAvailLock)。

## 核心悬念

**"Netty 没有用教科书上的 Buddy 二叉树——而是 PriorityQueue firstBestFit。为什么这个选择更适合网络 I/O？"**
