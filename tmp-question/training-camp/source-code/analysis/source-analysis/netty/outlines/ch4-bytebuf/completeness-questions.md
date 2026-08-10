# Ch4 ByteBuf 全视角验证

> 从多角色视角提问，验证每问都能在 outline 中找到答案

---

## 开发者视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | JDK NIO ByteBuffer 的单 position 和 Netty ByteBuf 的双指针根本差异是什么？ | 1.1 §1-2 |
| 2 | discardReadBytes 和 discardSomeReadBytes 有什么不同？各适用于什么场景？ | 1.1 §4 |
| 3 | ensureWritable 返回四状态值分别表示什么？调用者怎么区分 "已经扩容了" 和 "扩不动了"？ | 1.1 §5 |
| 4 | retain/release 用的 CAS 操作在哪里定义？为什么不用 synchronized？ | 1.1 §6 |
| 5 | slice() 和 duplicate() 共享数据但不增加引用计数 — 这安全吗？什么情况下会悬空？ | 1.4 §1-3 |
| 6 | readSlice/readRetainedSlice 和 slice/retainedSlice 有什么不同？ | 1.4 §1 |
| 7 | writeUtf8 写了 5 条快速路径 — 每条路径对应什么场景？为什么需要这么多？ | 1.5 §11 |
| 8 | toLeakAwareBuffer 什么时候返回 Advanced？什么时候返回 Simple？为什么 Composite 需要单独包装？ | 1.2 §4 |
| 9 | calculateNewCapacity 的三级策略为什么要精确匹配 4 MiB 边界值？不匹配会导致什么问题？ | 1.2 §3 |

## 性能工程师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 10 | discardReadBytes 的 O(N) arraycopy 和 Composite discardReadComponents 的 O(1) 释放哪个更好？什么场景下有意义？ | 1.1 §4, 1.5 §8 |
| 11 | Unsafe Heap 用 allocateUninitializedArray 跳过零填充 — 实际场景下能省多少？什么时候不该用？ | 1.3 §5 |
| 12 | Direct 扩容为什么要 buffer.put(oldBuffer) 而不是直接用 Unsafe.copyMemory？ | 1.3 §8 |
| 13 | VarHandle 加速在什么时候生效？为什么 Heap Unsafe 变体不用 VarHandle？ | 1.3 §5,9 |
| 14 | 4 MiB 扩容阈值是硬编码常量 — 修改它会影响什么？有没有场景应该调大或调小？ | 1.2 §3 |
| 15 | CompositeByteBuf 的 consolidate 内部触发 transferTo — 什么时候该主动 consolidate？什么时候宁可组件多？ | 1.5 §7 |

## SRE/运维视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 16 | 泄漏检测报告 "LEAK: ByteBuf.release() was not called" — 怎么定位到具体业务代码？ | 1.4 §9 |
| 17 | 系统属性 io.netty.allocator.type 三模式的性能特征差异是什么？生产该用哪个？ | 1.2 §8 |
| 18 | io.netty.buffer.checkBounds 和 io.netty.buffer.checkAccessible 在生产可以关闭吗？风险是什么？ | 1.1 §8 |

## 架构师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 19 | Netty ByteBuf 的引用计数和 C++ shared_ptr 的引用计数有什么区别？为什么 ByteBuf 不会循环引用？ | 1.1 §6, 1.4 §2 |
| 20 | slice/duplicate 默认不 retain 的设计 — 是 "allocation-free 零拷贝" 还是 "悬空等待事故"？设计者为什么做这个选择？ | 1.4 §1 |
| 21 | AbstractDerived 把引用计数全委托给 unwrap() — 如果一个 slice 被 release, 原始 buffer 也被 release, 其他 slice 会悬空吗？ | 1.4 §2-3 |
| 22 | CompositeByteBuf 的 Component 存储了 srcBuf 和 buf 两个引用 — 为什么不只用一个？什么场景下它们不一样？ | 1.5 §2 |
| 23 | CompositeByteBuf.consolidate 和直接 copy 一个新 buffer 有什么区别？为什么不是默认行为？ | 1.5 §7 |

## 研究者视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 24 | AdvancedLeakAwareByteBuf 的 ~80+ 个方法都调 recordLeakNonRefCountingOperation — 这个模式有没有性能成本？为什么不直接在抽象层做？ | 1.4 §9 |
| 25 | 为什么 UnpooledUnsafeDirectByteBuf 包装外部 ByteBuffer 时设置 doFree=false？Java 9+ 做了什么变化导致这个需求？ | 1.3 §9 |
| 26 | HeapByteBufUtil 用 VarHandle 做多字节访问 — VarHandle 和 Unsafe 在 get/set 上的区别是什么？为什么 Heap 不用 Unsafe 直接做？ | 1.3 §2 |
| 27 | SWAR 搜索算法在什么条件下退化为线性？硬件不支持 unaligned 访问时 ByteBufUtil 怎么处理？ | 1.5 §11 |

## 学生/新人视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 28 | 为什么 Netty 不用 Java 的 try-with-resources 来释放 ByteBuf？ | 1.1 §6 |
| 29 | wrappedBuffer(new byte[]{1,2,3}) 修改底层数组会影响 buffer 吗？ | 1.2 §5 |
| 30 | copiedBuffer 和 wrappedBuffer 的区别 — 什么时候该用哪个？ | 1.2 §5 |

---

## 覆盖统计

| 角色 | 问题数 | 覆盖 |
|------|:--:|:--:|
| 开发者 | 9 | 1.1§1-2,§4-6, 1.2§3-4,§7, 1.4§1-3,§6,§8-9, 1.5§11 |
| 性能工程师 | 6 | 1.1§4, 1.2§3, 1.3§5-9, 1.5§8 |
| SRE/运维 | 3 | 1.1§5, 1.4§9, 1.2§8 |
| 架构师 | 5 | 1.1§6, 1.4§1-3, 1.5§2,§7 |
| 研究者 | 4 | 1.4§9, 1.3§2-9, 1.5§11 |
| 学生/新人 | 3 | 1.1§6, 1.2§5 |
| **合计** | **30** | **100%** |

## 缺口

无缺口 — 30 问全部能在 5 篇 outline 中找到答案。
