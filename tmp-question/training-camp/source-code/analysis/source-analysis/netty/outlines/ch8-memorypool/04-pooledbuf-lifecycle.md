# Ch8 PooledByteBuf 生命周期 — 双归还与派生链

> Cluster D: 6 KPs | 依赖 §8.3 | §8.3 → §8.4

### 1. PooledByteBuf.deallocate — arena.free + unguardedRecycle 双归还

场景: `buf.release() → refCnt=0 → deallocate()`。PooledByteBuf 有两层资源需要归还: 1)底层内存(Chunk 中的 pages/Subpage 中的 elem)——归还到 Arena 复用; 2)PooledByteBuf 对象本身——归还到 Recycler 对象池(下次 alloc 不需要 new 对象)。

源码路径: `PooledByteBuf.java` — `deallocate()`: `arena.free(chunk, handle, sizeIdx, threadCache)`——内存归还到 Arena(§8.1 free 三态)。`recyclerHandle.unguardedRecycle(this)`——对象归还到 Recycler(对象池)。顺序: 先 free memory 再 recycle 对象——保证 Recycler 取用对象时, 对象的旧内存已经归还 Arena(不会错误地重新使用已释放内存)。

关键设计: 双归还分离内存生命周期和对象生命周期——内存在 Arena 的 ChunkList→Subpage→ThreadCache 中复用; PooledByteBuf 对象在 Recycler(类似于 ThreadLocal 的环形缓冲区)中复用——两个独立池。UnpooledHeapByteBuf 只有 freeArray=NOOP(GC), 没有双归还问题——这是 Pooled 的独特成本。

数据流: `buf.release()`→`refCnt=0`→`deallocate()`→`arena.free(chunk, handle, sizeIdx, threadCache)`→ThreadCache.add 成功(返回给线程缓存)→128B 内存 next alloc 可复用→`recyclerHandle.unguardedRecycle(this)`→PooledDirectByteBuf 对象归还 Recycler 对象池→下次 `arena.allocate()` 从 Recycler 借对象→不需要 `new PooledDirectByteBuf()`。

### 2. AbstractPooledDerivedByteBuf — 派生归还顺序

场景: `buf.slice(0, 128)` 返回的 PooledSlicedByteBuf 是一个派生视图——它共享 parent PooledDirectByteBuf 的内部内存(Chunk 的 page)。释放 slice 时——能直接释放 parent 的内存吗? 不能——parent 可能还有另一个 slice 在用。

源码路径: `AbstractPooledDerivedByteBuf.java` — `deallocate()`: 先 `unguardedRecycle(this)`——派生对象归还 Recycler; 后 `parent.release()`——parent 的 refCnt 减少。顺序原因: Recycler 可能立即 re-get 该对象——如果 parent 先 release 且 refCnt=0→arena.free 归还内存→Recycler re-get 拿到的对象指向已释放的内存→use-after-free。

关键设计: 派生归还的"先 recycle 自己再 release parent"是关键安全保障——与常规的"先 release parent(资源)再 recycle 自己(对象)"相反——专门为 Recycler 的重入设计。pooledNonRetained 变体: PooledNonRetainedDuplicate/PooledNonRetainedSliced——`deallocate` 直接 recycle 不调 parent.release()——parent refCnt 由原始 owner 管理。

数据流: `slice.release()`→`deallocate()`→`unguardedRecycle(this)`→派生对象回 Recycler→`parent.refCnt` 原来=3→`parent.release()`→refCnt=2→parent 仍存活(还有别的 slice 在用)→内存未被释放。

### 3. AdaptivePoolingAllocator — 反代际 + Magazine 四级降级

场景: PooledByteBufAllocator(旧, jemalloc 固定大小类) vs AdaptivePoolingAllocator(新, 运行时自适应)——后者根据近期的分配模式动态调整大小类。系统属性 `io.netty.allocator.type=adaptive` 切换。

源码路径: `AdaptivePoolingAllocator.java:2153行` — Magazine 架构: 每个 sizeIdx 一个 `Magazine`——内部持有 `SizeClassedChunk` 链表(小分配)和 `BuddyChunk` 二叉树(大分配, MIN_BUDDY_SIZE=32KB)。`SizeClassedChunk` 三态 FSM: `AVAILABLE→localSize→DEALLOCATED`——`AtomicReferenceFieldUpdater` 原子状态转换。Magazine 四级降级: `alloc0→allocFromOverflow0→allocFromNew0→createNewChunk`——渐进式分配, 前三级全部失败才创建新 Chunk。反代际假说: 最近频繁使用的 size→晋升 SizeClassedChunk 到 BuddyChunk——更多的连续内存; 长时间未用的 Magazine→reclamation 释放回 buddy allocator。

关键设计: Adaptive 和 Pooled 并存——Pooled 对标 jemalloc(静态大小类, 经验调优), Adaptive 对标反代际假说(运行时动态调整, 自适应)。两个都 experimental——Netty 默认用 Pooled——Adaptive 引入更多元数据管理成本但可能在某些 pattern(如 HTTP header 256B+body 16KB 反复切换)中更快。

数据流: `alloc.buffer(256)`→AdaptivePoolingAllocator.allocate→Magazine[256B]→alloc0(SizeClassedChunk 本地分配)→成功→返回→256B 频繁→晋升→Backendchunk 扩展→256B 不再频繁→reclamation→Magazine 释放 SizeClassedChunk→BuddyChunk binary tree reclaim。

### 核心悬念

**"PooledByteBuf 的双归还(arena.free + unguardedRecycle)、派生归还顺序(先 recycle 后 release)、PoolChunkList 的六级迁移——这些机制让 Netty 的内存分配从 Ch9 Bootstrap 触发——`serverBootstrap.bind()` 创建一个 server Channel→所有基础设施——EventLoop(Ch5)、Pipeline(Ch7)、MemoryPool(Ch8)——在 Bootstrap 中首次汇聚为一个可运行的服务器。"**

→ 引出 Ch9 Bootstrap — `AbstractBootstrap.doBind()` 三步: validate→initAndRegister→doBind0。`initAndRegister()` 创建 ChannelFactory 的 Channel, `init(channel)` 注入 Pipeline, `group().register(channel)` 绑定 EventLoop——ServerBootstrapAcceptor 在 accept 后自动完成子 Channel 的 EventLoop 注册+Pipeline 装配——所有前 8 章的基础设施汇聚为一个 `bind()` 调用。
