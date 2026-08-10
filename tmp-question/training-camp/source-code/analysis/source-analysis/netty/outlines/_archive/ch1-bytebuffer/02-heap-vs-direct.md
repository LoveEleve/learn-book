# §1.2 Heap vs Direct — 文章大纲

## 读者基线

已从 §1.1 掌握四字段模型（mark/position/limit/capacity）、flip/rewind/clear 三件套、compact 原理。尚不知道堆外内存的概念和 `Unsafe`。

## 概念依赖链

```
allocate(1024) → HeapByteBuffer (hb = new byte[1024])
  ↓ 引出问题: GC 暂停
allocateDirect(1024) → DirectByteBuffer (UNSAFE.allocateMemory + Cleaner)
  ↓ 引出问题: GC 收不到 → 推后释放 → 内存堆积
Bits.reserveMemory 的限流机制 → -XX:MaxDirectMemorySize
  ↓ 引出问题: 为什么复杂了这么多，但还是得用？
对比总结: heap vs direct 的性能画像
```

## 叙事顺序

**开篇场景：用 §1.1 的 ByteBuffer 写出第一个网络 I/O**

从 `allocate(1024)` 的源码出发，看看缓冲区分配到哪了。`X-Buffer.java.template:345`：

```java
public static ByteBuffer allocate(int capacity) {
    return new HeapByteBuffer(capacity, capacity);
}
```

一路追到底，`Heap-X-Buffer.java.template:63`：

```java
hb = new byte[cap];  // 底层就是一个 byte[]
```

这不是魔法——**HeapByteBuffer 的本质是在 JVM 堆上分配了一个 `byte[]`，然后包了一层控制字段**。

**第一层：heap buffer —— GC 管得着，但也管得太宽**

```java
ByteBuffer buf = ByteBuffer.allocate(1024);
// buf 是什么？一个 Java 对象，指向一个 JVM 堆上的 byte[] hb
// 当 buf 不再被引用 → GC 触发 → hb 被回收
```

优点：生命周期完全由 GC 管理，不需要手动释放。和普通 Java 对象一样。

缺点：GC 不知道这个 buffer 是"网络 I/O 正在用的"。它只知道引用计数。Full GC 时所有线程暂停——包括正在收网络数据的 I/O 线程。如果你在 I/O 密集的应用中频繁分配大 buffer，每次 GC 都是一次服务中断。

**第二层：direct buffer —— 绕过 GC 的终极方案**

```java
ByteBuffer buf = ByteBuffer.allocateDirect(1024);
// 翻开底牌: allocateDirect 做了什么？
```

翻开 `DirectByteBuffer(int cap)` 源码（`Direct-X-Buffer.java.template:112-140`）：

```java
// 步骤1: 记账检查——是否超过 MaxDirectMemorySize
Bits.reserveMemory(size, cap);  // 如果超限，此方法会阻塞

// 步骤2: 用 Unsafe 在堆外分配内存
long base = UNSAFE.allocateMemory(size);  // 等价于 C 的 malloc(size)

// 步骤3: 清零
UNSAFE.setMemory(base, size, (byte) 0);

// 步骤4: 注册 Cleaner → GC 时自动回收
cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
```

这是一个完全不同的范式：
- `allocate` = JVM 堆上 `new byte[]` → GC 标记→清扫
- `allocateDirect` = 堆外 `UNSAFE.allocateMemory` → GC 的 Cleaner 异步回收

**第三层：Cleaner 和 Deallocator —— "GC 帮忙回收"的代价**

```java
// Direct-X-Buffer.java.template:69-92
private static class Deallocator implements Runnable {
    private long address;    // 堆外内存地址 — 注意：非 final（需要 address=0 防 double-free）
    private long size;       // 分配大小 — 用于 Bits.unreserveMemory 释放记账配额
    private int capacity;    // 容量 — 用于 Bits.unreserveMemory 释放记账配额

    public void run() {
        if (address == 0) {          // 防 double-free：已被清理过则跳过
            return;
        }
        UNSAFE.freeMemory(address);   // 释放堆外内存
        address = 0;                  // 清零地址——防止 Cleaner 被重复调用
        Bits.unreserveMemory(size, capacity);
    }
}
```

注意 Deallocator 的三个参数和三步安全设计：`address` 用于释放内存（释放后必须清零防 double-free），`size + capacity` 分别用于释放 `Bits` 的记账配额。三者一个都不能少：光 freeMemory 不 unreserveMemory = `MaxDirectMemorySize` 配额永远被占用，最终 `allocateDirect` 会误判 OOM。

流程：DirectByteBuffer 对象被 GC 标记为垃圾 → GC 触发 Cleaner → Cleaner 调用 Deallocator.run() → `freeMemory`。

问题来了：这个链条的每个环节都有不确定的延迟：
- GC 什么时候触发不确定——可能堆内存够用就一直不 GC
- 堆外内存已经用光了，但 JVM 堆还很小 → GC 不触发 → Cleaner 不运行 → 堆外 OOM

这不是理论问题——**生产环境中这是真实的堆外内存泄漏场景**。

**第四层：Bits.reserveMemory —— "柜台限制"**

在分配堆外内存之前，JDK 执行 `Bits.reserveMemory(size, cap)`（`Bits.java:109`）——检查累计分配的堆外内存是否超过 `-XX:MaxDirectMemorySize`（默认等于 `-Xmx`）。

如果超限，`Bits.java:125-176` 的三步流程：

```
1. waitForReferenceProcessing() → 等待已有的 Cleaner 回收（Reference 队列）
2. System.gc()                  → 触发一次 VM 的 Reference 处理
3. 指数退避重试（9 轮）:
   sleepTime = 1, 2, 4, 8, 16, 32, 64, 128, 256 ms
   每轮重试 tryReserveMemory
   9 轮后仍未成功 → throw OutOfMemoryError("Direct buffer memory")
```

源码注释（`Bits.java:100-103`）解释了设计意图：

```java
// max. number of sleeps during try-reserving with exponentially
// increasing delay before throwing OutOfMemoryError:
// 1, 2, 4, 8, 16, 32, 64, 128, 256 (total 511 ms ~ 0.5 s)
// which means that OOME will be thrown after 0.5 s of trying
```

关键点：**不是无限阻塞**——511ms 后直接抛 OOM。这就是为什么大 pressure 下 `allocateDirect` 会失败：0.5s 内 Cleaner 来不及回收足够的空间。

**第五层：对比总结**

| 维度 | HeapByteBuffer | DirectByteBuffer |
|------|------|------|
| 分配位置 | JVM 堆（`new byte[]`） | 堆外（`UNSAFE.allocateMemory`） |
| 底层存储 | `final byte[] hb` | `long address`（内存地址） |
| 访问方式 | `hb[ix(index)]` | `UNSAFE.getByte(address + index)` |
| 释放方式 | GC 标记→清扫 | GC → Cleaner → Deallocator.freeMemory |
| 释放确定性 | GC 决定 | 非确定（依赖 GC 触发时点） |
| I/O 性能 | 需 JNI 临时拷贝到 direct | 零拷贝（传指针给 OS socket） |
| 分配速度 | 快（TLAB 内部分配） | 慢（系统调用 malloc） |
| 并发上限 | `-Xmx` 限制 | `-XX:MaxDirectMemorySize` 限制 |

为什么 HeapByteBuffer 的 I/O 需要 JNI 拷贝？`write()` 系统调用（`writev`/`sendmsg`）需要**连续的物理内存地址**来做 DMA 传输。堆上的 `byte[]` 对象由 GC 管理——GC 可能在任何时候**移动这个数组**（compaction），OS 拿到的地址瞬间失效。所以 NIO 框架在调用 `write()` 之前，必须把 heap 数据**拷贝**到一个不会被 GC 移动的临时 direct buffer 中。

DirectByteBuffer 绕过这个问题——`UNSAFE.allocateMemory` 返回的地址在堆外，GC 永远不会碰它，可以直接传给 OS 做零拷贝 I/O。

**结尾悬念**

Heap 方便但 GC 干扰 I/O 线程。Direct 零拷贝但释放不确定。两者都有致命缺陷——有没有一种方式，既能零拷贝 I/O，又能**确定性**释放？这需要自己管理内存的引用计数。后续章节将给出答案。

但在那之前，ByteBuffer 还有一些隐藏的陷阱需要看清——它还有一套"共享式视图"机制，理解它们才能完整把握 ByteBuffer 的数据安全边界。下一节。

## 核心悬念

**读完能回答**：`allocate(1024)` 和 `allocateDirect(1024)` 在内存分配路径上有什么根本区别？DirectByteBuffer 的 Cleaner/Deallocator 机制为什么不能确保堆外内存的及时释放？`Bits.reserveMemory` 的限流机制在什么条件下会工作失效？

## 文章边界

- **在本篇内**: HeapByteBuffer 的 byte[] → DirectByteBuffer 的 Unsafe+Cleaner → Bits 限流 → heap vs direct 对比
- **不属于本篇**: Netty 的引用计数（§4.2）、PooledByteBufAllocator 池化（§8.1）、Selector（§3.1）
