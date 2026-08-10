# Ch4 ByteBuf 核心抽象 — 为什么重新发明缓冲区

> 3 篇: 核心抽象 → 内存管理 → 视图与零拷贝
> 覆盖: Q1(双指针) / Q8(扩容) / Q2(引用计数)

---

### 1. 双指针模型 — 消灭 flip/compact
  - 回顾: Pipeline 中三个 Handler — 每个都要 flip/compact — 全局状态机 vs 局部操作
  - `readerIndex` / `writerIndex` 两个独立游标 (ByteBuf.java:62-74)
  - 三区域: discardable(0~rI) / readable(rI~wI) / writable(wI~cap) — invariant: `0≤rI≤wI≤cap`
  - `readableBytes()` = `writerIndex - readerIndex` (AbstractByteBuf.java:178) — 恒定公式
  - `writableBytes()` = `capacity() - writerIndex` (AbstractByteBuf.java:183)
  - `discardReadBytes()`: ByteBuf.java:513 — 回收已读空间 (rI→0, wI-=oldRI)
  - `markReaderIndex()` / `resetReaderIndex()`: AbstractByteBuf.java:73,192-199 — peek-without-consume

### 2. 自动扩容 — 不再预设容量
  - `ensureWritable(minWritableBytes)` (ByteBuf.java:535) — 保证至少有 N 字节可写空间
  - `calculateNewCapacity()` (AbstractByteBufAllocator.java:232-259) — 扩容策略
  - CALCULATE_THRESHOLD = 4 MiB (AbstractByteBufAllocator.java:34) — 阈值分界
  - <4MB: 指数翻倍 — `findNextPositivePowerOfTwo(max(min, 64))` — 16 次从 64 到 4MB
  - >=4MB: 线性步进 +4MB — `minNewCapacity / threshold * threshold + threshold`
  - 设计权衡: 小 buffer 指数 (16 次扩容) vs 大 buffer 线性 (控制内存足迹)
  - 扩容触发: `ensureWritable` → check writableBytes → `capacity(newCapacity)` → `System.arraycopy`

### 3. 引用计数 — 绕过 GC 的显式生命周期
  - NIO DirectByteBuffer.cleaner 依赖 GC — 不确定延迟 → OOM
  - `ReferenceCounted` 接口 (ReferenceCounted.java:32): `refCnt()` / `retain()` / `release()` / `touch()`
  - `AbstractReferenceCountedByteBuf` (AbstractReferenceCountedByteBuf.java:24-101) — 78 行核心实现
  - `RefCnt` 三级: `Unsafe→VarHandle→AtomicIntegerFieldUpdater` (RefCnt.java:34-46)
  - `release()` → `handleRelease(RefCnt.release(refCnt))` → `deallocate()` (line 91-96)
  - `deallocate()` 子类分发: `UnpooledHeapByteBuf`(置空byte[]) / `UnpooledDirectByteBuf`(`PlatformDependent.freeDirectBuffer()`) / `PooledByteBuf`(归还池)
  - `isAccessible()`: 非 volatile 读 — best-effort guard (line 34-37)
  - Pipeline 自动处理: Inbound→TailContext `ReferenceCountUtil.release(msg)` (DefaultChannelPipeline.java:1207); Outbound→HeadContext/unsafe (line 1385-1386)

### 4. 收束
  - ByteBuffer 三元凶: flip/compact 状态机 + 固定容量 + GC 驱动的 Direct 回收
  - ByteBuf 三解药: 双指针 + 自动扩容 + 引用计数
  - 三者闭环: 双指针保证零切换吞吐 → 扩容保证不中断 → 引用计数保证不泄漏

---

### 核心悬念
**"为什么 Netty 的每个 Handler 传的都是 ByteBuf，而不是 NIO 的 ByteBuffer？"**
