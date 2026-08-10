# Ch1 NIO ByteBuffer — 全视角完备性验证

> 7 身份 × 选题方向 | 每问标注对应 outline 节 | 无对应=缺口

---

## 开发者视角 (我要写代码)

| # | 问题 | 对应 outline |
|:--:|------|:--:|
| 1 | ByteBuffer 的 position/limit/capacity 三个指针之间的关系是什么？不变式呢？ | §1.1 §1 |
| 2 | flip() 到底做了什么？position、limit、mark 分别变成什么值？ | §1.1 §3 |
| 3 | compact() 和 flip() 的区别？什么时候用哪个？ | §1.1 §4 |
| 4 | clear() 和 rewind() 都重置 position=0，区别在哪？ | §1.1 §5 |
| 5 | relative get 和 absolute get 的区别？哪个影响 position？ | §1.1 §2 |
| 6 | allocateDirect() 和 allocate() 返回什么类型？性能差异？ | §1.2 §1-2 |
| 7 | wrap(byte[]) 返回的 buffer 修改数据后原始数组会变吗？ | §1.2 §5 |
| 8 | hasArray() 在 HeapByteBuffer 和 DirectByteBuffer 分别返回什么？ | §1.3 §2 |
| 9 | buf.array() 在 DirectByteBuffer 上调会发生什么？ | §1.3 §2 |
| 10 | equals() 在 flip 前后可能返回不同结果——为什么？ | §1.3 §3 |
| 11 | slice() 创建的子 buffer 修改数据后原始 buffer 会变吗？ | §1.3 §1 |
| 12 | mark()/reset() 怎么用？peek 完数据不够怎么回退？ | §1.1 §5 |
| 13 | Direct buffer 分配有什么内存限制？超过限制怎么办？ | §1.2 §4 |

## 性能工程师视角 (我要调优)

| # | 问题 | 对应 outline |
|:--:|------|:--:|
| 1 | HeapByteBuffer 和 DirectByteBuffer 在 Socket I/O 中的拷贝有什么区别？ | §1.2 §1-2 |
| 2 | Direct buffer 的 Deallocator 什么时候被触发？为什么可能出问题？ | §1.2 §3 |
| 3 | Bits.reserveMemory 的指数退避重试策略是什么？（1,2,4,8...ms） | §1.2 §4 |
| 4 | compact() 的 System.arraycopy 是 O(N) 吗？在热路径上有什么影响？ | §1.1 §4 |
| 5 | ByteBuffer 的 order() 设置会影响哪些方法？ | §1.3 §4 |

## 架构师视角 (为什么这样设计)

| # | 问题 | 对应 outline |
|:--:|------|:--:|
| 1 | 为什么 NIO 用单指针 position 模型而不是 Netty 的双指针 readerIndex/writerIndex？ | §1.1 §1 |
| 2 | Cleaner PhantomReference 回收 Direct memory 的设计缺陷是什么？Netty 怎么改进的？ | §1.2 §3 |
| 3 | slice()/duplicate() 共享数据的设计意图是什么？什么时候需要拷贝？ | §1.3 §1 |
| 4 | 为什么 hasArray() 不能用 interface 统一 Heap/Direct？ | §1.3 §2 |
| 5 | equals() 只在 remaining() 范围内比较——这个设计的 tradeoff 是什么？ | §1.3 §3 |

## SRE 视角 (线上出事了)

| # | 问题 | 对应 outline |
|:--:|------|:--:|
| 1 | Direct buffer 内存泄漏怎么看？`-XX:MaxDirectMemorySize` 超限什么现象？ | §1.2 §4 |
| 2 | 多线程共享 ByteBuffer 会有什么问题？ | §1.3 §6 |
| 3 | ReadOnlyBufferException 在哪里会触发？ | §1.3 §1 |
| 4 | BufferUnderflowException / BufferOverflowException 触发条件？ | §1.1 §2 |

## 学生视角 (第一次看)

| # | 问题 | 对应 outline |
|:--:|------|:--:|
| 1 | `byte[]` 和 ByteBuffer 有什么区别？为什么不用 byte[] 传网络数据？ | §1.1 §1 |
| 2 | flip() 和 compact() 的语义为什么反直觉？"flip 就是读完准备写"对吗？ | §1.1 §3-4 |
| 3 | Direct buffer 不是用 GC 回收的吗？为什么还会 OOM？ | §1.2 §3 |
| 4 | slice() 和 copy 有什么区别？什么时候该用哪个？ | §1.3 §1 |
| 5 | ByteBuffer 线程安全吗？ | §1.3 §6 |

---

## 覆盖审计

| 身份 | 提问数 | 可回答 | 覆盖率 |
|------|:--:|:--:|:--:|
| 开发者 | 13 | 13 | 100% |
| 性能工程师 | 5 | 5 | 100% |
| 架构师 | 5 | 5 | 100% |
| SRE | 4 | 4 | 100% |
| 学生 | 5 | 5 | 100% |
| **合计** | **32** | **32** | **100%** |

32 问全部可回答。无缺口。
