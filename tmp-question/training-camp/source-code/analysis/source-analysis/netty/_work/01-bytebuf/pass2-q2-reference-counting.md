## Loop Note: Q2 — 引用计数 vs GC

**Hypothesis**: ByteBuf 用引用计数代替 GC 管理 DirectByteBuf 的生命周期——GC 只回收 DirectByteBuffer 的 Java 对象壳，真正的堆外内存需要显式 release。

**Verification** (grep against source):
- `ReferenceCounted.java:32` — 接口: refCnt() / retain() / release() / touch()
- `AbstractReferenceCountedByteBuf.java:27` — `private final RefCnt refCnt = new RefCnt()` (VarHandle 实现)
- `AbstractReferenceCountedByteBuf.java:61` — `retain() { RefCnt.retain(refCnt); }`
- `AbstractReferenceCountedByteBuf.java:82-83` — `release() { return handleRelease(RefCnt.release(refCnt)); }`
- `AbstractReferenceCountedByteBuf.java:91-96` — `handleRelease(boolean result)` → if true → `deallocate()`
- `AbstractReferenceCountedByteBuf.java:101` — `protected abstract void deallocate()` — 子类实现
- `AbstractReferenceCountedByteBuf.java:34-37` — `isAccessible()` 使用 non-volatile read (性能优化，best-effort guard)

**Code type**: Implementation

**设计权衡**:
- Java NIO `DirectByteBuffer.cleaner` 依赖 GC Finalizer → 不确定延迟 → 大量堆外分配时 OOM
- Netty 引用计数 → 确定性地释放 (`release() → deallocate()`) → 精确控制内存
- touch() 配合 ResourceLeakDetector → 被动泄漏检测（gc 时还在 refCnt 的 buf 被报告）
- 非 volatile read 在 `isAccessible()` → 性能优化，因为 `ensureAccessible()` 已经是 racy best-effort

**关键规则**: 
- 最后调用 `release()` 的持有者负责 `deallocate()` 
- `retain()/release()` 必须成对调用
- Netty Pipeline 自动处理: HeadContext 最后 release Inbound message，TailContext 最后 release Outbound

**Conclusion**: 引用计数 = GC 的补充。Direct memory 绕过 GC → 需显式生命周期管理。AbstractReferenceCountedByteBuf 用 VarHandle RefCnt + abstract deallocate() 实现主子类分离。source: AbstractReferenceCountedByteBuf.java:27-101, ReferenceCounted.java:32-76
