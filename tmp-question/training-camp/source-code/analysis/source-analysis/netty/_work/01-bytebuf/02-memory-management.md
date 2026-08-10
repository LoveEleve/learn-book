# ByteBuf 的内存管理 — 从哪来到哪去

上一篇讲了 ByteBuf 的核心抽象——双指针模型解决 flip/compact、自动扩容代替固定容量、引用计数管理生命周期。三个设计回答了"为什么重新发明缓冲区"。但细心的读者会注意到一个没有展开的问题：**引用计数管理了释放，但分配呢？每次 `release()` 之后那块内存去哪了？下一次 `retain()` 的内存从哪来？**

这一篇回答三个问题：Heap 和 Direct 的物理差异、池化分配的三层架构、以及泄漏检测的安全网。

---

## Heap vs Direct：字节的两种住法

先看一段代码——如果你在 Netty 里写 `Unpooled.buffer(1024)` 和 `Unpooled.directBuffer(1024)`，底层发生了什么。

`UnpooledHeapByteBuf` 内部就是一个普通的 `byte[]`(`UnpooledHeapByteBuf.java:38`)。所有的字节操作最终落到 `_getByte()`（`UnpooledHeapByteBuf.java:332-334`），它委托给 `HeapByteBufUtil.getByte(array, index)`——就是一次直接的 `array[index]` 数组访问（`HeapByteBufUtil.java:25-27`）。

JVM 对数组做了大量的优化——指针压缩、逃逸分析、TLAB 分配。分配一个 `byte[1024]` 基本上就是一次 TLAB 内的指针移动，比一次 `new Object()` 还快。

但性能的优势区在别的地方暴露了问题。关键判断是 `hasArray()` 和 `hasMemoryAddress()` 两个方法（`ByteBuf.java` 中定义）：

```java
// UnpooledHeapByteBuf.java:141
hasArray() → true     // 内部 byte[] 可被 JNI GetByteArrayRegion 访问

// UnpooledHeapByteBuf.java:157
hasMemoryAddress() → false  // JVM 堆内存没有固定地址——GC 会移动对象
```

第一个问题：NIO 的 Socket 通道（`SocketChannel.write(ByteBuffer)`）在写入堆内数据时，JNI 层必须把堆上的 byte[] 拷贝到一个临时的 Direct buffer 中——因为操作系统需要稳定的物理内存地址做 DMA 传输，堆内存可能随时被 GC 移动。这是 JVM 规范规定的行为，不是 Netty 的设计缺陷。但结果是一样的：**每一次堆内 ByteBuf 写入 Socket，都附带一次隐式的内存拷贝**。

Netty 的设计者当然知道这一点。所以有另一个选择——`UnpooledDirectByteBuf`。

```java
// UnpooledDirectByteBuf.java:39
extends AbstractReferenceCountedByteBuf，内部用 java.nio.ByteBuffer
```

它的 `hasArray()` 返回 false，因为 `ByteBuffer.allocateDirect()` 分配的内存在堆外，不对应任何 Java `byte[]`。但 `hasMemoryAddress()` 返回 true——这块内存有固定的逻辑地址（在 JVM 进程内），操作系统可以直接用它做 DMA 传输。

这带来两个改变：

**好处**：网络 I/O 真正零拷贝。`SocketChannel.write(directBuffer)` 时，数据直接从堆外内存 DMA 到网卡，中间的 Java 堆拷贝被消除。对于每秒数万次读写的高并发框架，这节省的 CPU 时间和避免的全堆 GC 压力是决定性的。

**代价**：堆外内存的分配和释放都比堆内存慢得多。`allocateDirect()` 本质是一次 `malloc()` 系统调用——需要内核从进程的虚拟地址空间中分配连续页。释放时也不能等 GC——已经是上篇的结论，这里用引用计数精确回收。但你想想：如果每次读写都需要 `malloc()` 一个 Direct buffer、用完 `free()` 掉，这开销在高吞吐量下是不堪忍受的。

Netty 的默认策略在 `PooledByteBufAllocator.DEFAULT` 的构造中明确了（`PooledByteBufAllocator.java:187-188`）：

```java
public static final PooledByteBufAllocator DEFAULT =
    new PooledByteBufAllocator(!PlatformDependent.isExplicitNoPreferDirect());
```

默认优先 Direct buffer。你可以通过 `-Dio.netty.noPreferDirect=true` 改为优先堆内。对于 IO 密集型场景，Direct 的零拷贝优势足以压倒分配速度的劣势。

但"频繁分配慢"这个问题——靠优先 Heap 还是 Direct 解决不了。Netty 的真正答案是：**能不能不频繁分配？**

---

## 池化分配的三层架构

如果每个 `ByteBuf` 都直接向操作系统申请内存，高频读写下的分配/释放就像一次次的系统调用派对——池化分配的本质是把"申请一块新内存"变成"从已有的缓冲区里拿一块"，用完归还，循环复用。

`PooledByteBufAllocator` 的架构是一棵三层树（`PooledByteBufAllocator.java:95-198`）：

```
PooledByteBufAllocator
  ├── PoolArena[0]        ← 线程1绑定的 Arena
  │     ├── SizeClasses    ← tiny(1-512B)/small(512B-8KB)/normal(8KB-4MB)
  │     │     └── PoolChunk   ← 每个 chunk = 4MB (pageSize=8KB << maxOrder=9)
  │     │           └── Buddy 分配 (二叉树分裂与合并)
  │     └── PoolSubpage     ← < 8KB 的分配——一个 page 分 8 个子页
  ├── PoolArena[1]        ← 线程2绑定的 Arena
  └── ...                 ← Arena 总数 = min(2×cores, maxMem/chunkSize/2/3)
```

### 第一层：多个 Arena 消除锁竞争

如果你的服务器有 16 个核心，按照注释（`PooledByteBufAllocator.java:97-102`），默认会创建 `16 × 2 = 32` 个 Arena。为什么是 2 倍核心数？

注释直接引用了 GitHub issue #3888：Netty 的 EventLoop 线程数也是 2 倍核心数（在 NIO 和 Epoll 模式下）。如果 Arena 数量少于 EventLoop 数量，多个 EventLoop 在分配和释放 ByteBuf 时会竞争同一个 Arena 的内部锁——allocation 和 de-allocation 都需要在 Arena 上同步。32 个 Arena 意味着 16 个 EventLoop 线程几乎不会碰到同一个 Arena，锁竞争趋近于零。

Arena 的具体数量受内存上限约束（`PooledByteBufAllocator.java:106-117`）：

```java
// Heap Arena: min(cores×2, 堆最大内存 / chunkSize / 2 / 3)
DEFAULT_NUM_HEAP_ARENA = min(cores×2, maxMemory / 4MB / 2 / 3)

// Direct Arena: 同上但用 maxDirectMemory
DEFAULT_NUM_DIRECT_ARENA = min(cores×2, maxDirectMemory / 4MB / 2 / 3)
```

这个公式的"除以 2 再除以 3"保证即使所有 Arena 同时分配，也不太可能超过 JVM 的可用内存。它不是精确的上限，而是对"高并发下最坏情况的内存用量"的工程估算。

### 第二层：每线程绑定一个 Arena

当一个 Netty FastThreadLocal 线程首次请求 ByteBuf 时，`PoolThreadLocalCache`（`PooledByteBufAllocator.java:516`）触发初始化：

```java
// PooledByteBufAllocator.java:525-526 (简化)
heapArena = leastUsedArena(heapArenas);
directArena = leastUsedArena(directArenas);
```

`leastUsedArena()`（`PooledByteBufAllocator.java:558`）选出当前活跃线程最少的 Arena——这个线程被"终生绑定"到这个 Arena。注意"活跃线程数最少"而不是"已有分配量最少"——因为 Arena 的锁竞争是由并发线程数决定的，不是由分配量。

线程绑定 Arena 意味着这个线程的绝大部分分配都走同一个 Arena——锁竞争降到极低。但 Arena 内部仍然有 `ReentrantLock`（`PoolArena.java:78`）保护分配和释放路径，因为 ByteBuf 可能跨线程传递：一个线程分配的 ByteBuf，另一个线程调用 `release()` → `deallocate()` → 归还到**分配线程**的 Arena。这种情况下的 deallocation 和 reallocation 都需要 Arena 级别的同步。

### 第三层：大小分类与 Buddy 分配

Arena 内部按分配大小分为三个区间（`SizeClasses.java`）：

- **Tiny**（1-512 字节）— 从 PoolSubpage 分配。一个 page（默认 8KB）被切分成等份的子页——比如 64 字节的请求会得到一个 8KB/64=128 等份之一的子页。这些 tiny 分配是最常见的（HTTP header、短字符串），走线程本地缓存 (`PoolThreadCache`)——分配和释放都不需要碰 Arena 锁。
- **Small**（512B-8KB）— 也始于线程缓存。缓存用尽后从 Arena 的一个 page 分配。一个 page 刚好装一个 small 分配（如一个 2KB 的 buffer 占 1/4 个 page）。
- **Normal**（8KB-4MB）— 从 PoolChunk 分配。每个 chunk = 8KB × 2^9 = 4MB。chunk 内部是一棵完整的二叉树——每一层对应不同大小的分配请求。最大分配请求（正好 4MB）拿整个 chunk 的根节点；请求 2MB 拿第 1 层的某个子节点；请求 1MB 拿第 2 层……依此类推。

这就是 Buddy 分配的核心思想：把一个 4MB 的 chunk 想象成 root=4M、左子=2M、右子=2M 的二叉树。每个节点有一个"使用中"或"空闲"标记。当 2MB 的子块用完后释放，如果它的兄弟也是空闲的，两个 2MB 合并回一个 4MB 块——下次 4MB 的大分配就不需要找新 chunk。

### 线程缓存：最后的加速手段

PoolThreadCache（每线程一个）缓存最近释放的 tiny 和 small 分配。配置参数（`PooledByteBufAllocator.java:120-121`）：

```
smallCacheSize = 256   // 每个线程缓存最多 256 个 small 分配
normalCacheSize = 64   // 每个线程缓存最多 64 个 normal 分配
```

当 Handler 处理完一个请求、release() 一个 small ByteBuf 时，ByteBuf 不会立即归还 Arena——而是放入线程缓存。下一个请求（同一 EventLoop 线程）需要相同大小的 ByteBuf 时直接从线程缓存获取——**全程零系统调用、零 Arena 锁争用**。只有当缓存满了或线程需要的是不同大小的分配时，才回退到 Arena 的 Buddy 分配。

这就是为什么 Netty 能做每秒数十万次 ByteBuf 分配的原因——大部分分配根本没走出线程本地缓存。

### 一图收束

从 `PooledByteBufAllocator.newDirectBuffer()` 开始（`PooledByteBufAllocator.java:397-409`），整个分配路径是：

```
请求 newDirectBuffer(1024, maxCap)
  → threadCache.get()                       ← 获取线程本地缓存
  → directArena.allocate(cache, 1024, maxCap)
      → SizeClasses 分类: 1024 → small      ← 查大小表
      → threadCache.allocateSmall()          ← 先查线程缓存
          → HIT → 返回已缓存的 ByteBuf      ← 零开销
          → MISS → Arena.allocateNormal()
              → PoolChunk.Buddy 分配          ← arena 操作
              → 返回新的 Chunk 节点
  → toLeakAwareBuffer(buf)                  ← 包裹泄漏检测

release()
  → refCnt → 0 → deallocate()
      → 归还 threadCache (small)             ← 缓存备下次用
      或 归还 PoolChunk 节点 (normal)        ← Buddy 合并
```

大部分 small 分配命中了线程缓存——这是 Netty 在每次 I/O 读写中创建 ByteBuf 而不会成为分配瓶颈的秘密。

---

## 泄漏检测：池化的安全网

池化解决了"频繁分配的慢"，但引入了一个新的风险：**如果某个 ByteBuf 被 release 但忘了——它永远回不到池中**。池中的这块内存被标记为"已分配"，直到 JVM 重启才被回收。

引用计数管理了正常生命周期。泄漏检测是补充——它告诉你"哪些对象应该被 release 了但忘了"。

`ResourceLeakDetector` 在 `Level` 枚举中定义了四个级别（`ResourceLeakDetector.java:65-84`）：

| 级别 | 采样率 | 行为 | 场景 |
|------|:--:|------|------|
| `DISABLED` | 0% | 完全不检测 | 经压测确认无泄漏后启用 |
| `SIMPLE` | 1/128 (~0.78%) | 检测到泄漏时报告"有泄漏" | **生产默认** |
| `ADVANCED` | 1/128 (~0.78%) | 记录泄漏对象的最后一次访问栈 | 排查生产泄漏 |
| `PARANOID` | 100% | 每次分配/释放全量检测 | 开发调试 |

默认级别是 `SIMPLE`，默认采样间隔是 128（`ResourceLeakDetector.java:53`）。这意味着每 128 个 ByteBuf 中只有 1 个会被跟踪——99.2% 的 ByteBuf 完全不受影响。

检测机制利用了 Java 的引用队列。当一个被采样的 ByteBuf 创建时，`ResourceLeakDetector` 创建一个 `PhantomReference` 指向它。当 ByteBuf 被 GC 回收时（因为所有强引用都消失了），PhantomReference 进入引用队列。此时检测器检查它的 `refCnt`——如果大于 0，说明 GC 回收了它但没有人调用过 `release()`。这就是一次泄漏。

重复一遍：**泄漏检测不负责释放内存**——它只报告"这个对象被 GC 了但引用计数没到 0"。真正的内存释放还是由 `release() → deallocate()` 完成。泄漏检测是说"你应该释放但没有释放"的第二意见。

为什么默认只采样 1/128？因为 PHANTOM REFERENCE 的创建和追踪本身有内存和 CPU 开销。不需要对每个 ByteBuf 做——128 个中只要 1 个能发现泄漏，对于高频分配的场景足够了。生产环境如果发现泄漏日志，可以临时切换到 `ADVANCED`（同样采样率但记录完整栈）或 `PARANOID`（100% 检测但仅在开发环境可承受）来定位泄漏源。

可以随时通过系统属性覆盖默认值：

```
-Dio.netty.leakDetection.level=PARANOID     # 全部检测
-Dio.netty.leakDetection.level=DISABLED      # 关闭检测
-Dio.netty.leakDetection.samplingInterval=64 # 2× 采样率
```

---

## 三者合一

现在可以勾勒出 ByteBuf 内存管理的完整画面：

```
分配路径:
  PooledByteBufAllocator
    → 选 Arena (leastUsed)
      → 线程缓存命中 → 直接返回
      → 线程缓存未命中 → Arena.allocate → Buddy 分配/Subpage

释放路径:
  release() → refCnt → 0 → deallocate()
    → 归还线程缓存 (small/tiny)
    → 归还 Chunk 节点 (normal)
    → Buddy 合并空闲兄弟节点

监控路径 (SIMPLE 默认):
  PhantomReference 被 GC 回收 → 检查 refCnt > 0
    → 报告泄漏: "LEAK: ByteBuf.release() was not called"
```

三层设计各自解决一个层面的问题：

1. **Heap vs Direct** — 解释"字节数据存在哪里"。直接回答了上篇末尾的"堆外内存和 GC 的关系"——堆内用数组、堆外用直接地址。Direct 的零拷贝优势决定了 Netty 的默认偏好，但需要池化来补偿分配速度。

2. **PooledByteBufAllocator** — 回答"怎么不频繁 malloc"。多 Arena 消除锁竞争，线程绑定减少上下文切换，Buddy 分配管理大块内存的重用，线程缓存让绝大部分分配不离开本地线程。

3. **ResourceLeakDetector** — 回答"怎么知道有没有漏"。四个等级的检测从零开销到全量覆盖，1/128 默认采样在性能和覆盖度之间取平衡。它不修复泄漏，只告诉你泄漏在哪。

三者的关系不是时间顺序——是工程上的三层共同承诺：

- **Heap/Direct 说了"我在哪"**（物理布局）
- **PooledAllocator 说了"我从哪来、回哪去"**（分配与回收策略）
- **LeakDetector 说了"有没有东西回不来"**（健康监控）

读完这篇，你应该知道每个 ByteBuf 的整条生命线：它是堆内还是堆外、从哪个 Arena 分配的、走在哪个线程的快速缓存中、是不是被采样跟踪……最后回到池中，等待下一次复用。

下一篇进入最后一个主题——**当你不创建新的 ByteBuf，而是从已有的 ByteBuf 派生出新的视图时，到底发生了什么？** slice、copy、CompositeByteBuf 的共享与陷阱。这也是 ByteBuf 和 NIO ByteBuffer 相比的一条关键差异线——ByteBuffer 的视图操作（slice/duplicate/asReadOnlyBuffer）有隐式的共享陷阱，ByteBuf 是怎么处理这些问题的？

---

## 源码路径记录

| 概念 | 源码位置 | 行号 |
|------|---------|:--:|
| UnpooledHeapByteBuf extends ReferenceCounted | `UnpooledHeapByteBuf.java` | 38 |
| _getByte() → HeapByteBufUtil.getByte(array, index) | `UnpooledHeapByteBuf.java` | 332-334 |
| HeapByteBufUtil.getByte → memory[index] | `HeapByteBufUtil.java` | 25-27 |
| hasArray() = true | `UnpooledHeapByteBuf.java` | 141 |
| hasMemoryAddress() = false | `UnpooledHeapByteBuf.java` | 157 |
| UnpooledDirectByteBuf extends ReferenceCounted | `UnpooledDirectByteBuf.java` | 39 |
| hasArray() = false | `UnpooledDirectByteBuf.java` | 216 |
| hasMemoryAddress() = true | `UnpooledDirectByteBuf.java` | 231 |
| PooledByteBufAllocator DEFAULT (preferDirect) | `PooledByteBufAllocator.java` | 187-188 |
| Arena 数量 = cores×2 (issue #3888) | `PooledByteBufAllocator.java` | 97-104 |
| Heap Arena 上限 = min(cores×2, mem/chunk/2/3) | `PooledByteBufAllocator.java` | 106-117 |
| Direct Arena 上限 (maxDirectMemory) | `PooledByteBufAllocator.java` | 112-117 |
| smallCacheSize=256, normalCacheSize=64 | `PooledByteBufAllocator.java` | 120-121 |
| heapArenas / directArenas | `PooledByteBufAllocator.java` | 190-191 |
| PoolThreadLocalCache extends FastThreadLocal | `PooledByteBufAllocator.java` | 516 |
| leastUsedArena() | `PooledByteBufAllocator.java` | 558 |
| newDirectBuffer allocate flow | `PooledByteBufAllocator.java` | 397-409 |
| PoolArena ReentrantLock | `PoolArena.java` | 78 |
| ResourceLeakDetector 默认级别 = SIMPLE | `ResourceLeakDetector.java` | 46 |
| SAMPLING_INTERVAL = 128 | `ResourceLeakDetector.java` | 53 |
| Level 枚举 (DISABLED/SIMPLE/ADVANCED/PARANOID) | `ResourceLeakDetector.java` | 65-84 |
| old/new 系统属性兼容 (io.netty.leakDetectionLevel) | `ResourceLeakDetector.java` | 44-45 |
