# Ch1 核心抽象 — NIO 缓冲区的状态机

> Cluster A: 7 KPs | 读者基线: byte[] | Ch1 → §1.2

### 1. 四字段模型 — JDK 如何用一个缓冲区完成读写复用

场景: Java 的 `byte[]` 只有长度一个属性——"写到哪了"和"读到哪了"需要你自己维护两个整数。JDK NIO 把这个模式固化为 Buffer 的四个内置字段。

源码路径: `Buffer.java:197-201` 定义 `mark(-1)/position(0)/limit/capacity` — 构造函数设 position 为 0、limit 为 capacity。`Buffer.java:483` 的 `remaining() = limit - position` 是整组 API 的计算基础。

关键设计: 四字段的不变式 `0 ≤ mark ≤ position ≤ limit ≤ capacity` 让一个缓冲区可以在"正在写"(position 推进)、"写完了准备读"(flip 设 limit=position)、"读完了准备写"(clear 复位)三态之间切换——无需三个独立的数据结构。容量固定不变、mark 用于临时保存/恢复 position——四个字段覆盖了生产者-消费者模式的全部状态。

数据流: HeapByteBuffer/DirectByteBuffer 的子类实现各自读写 position 指针, 但四个字段始终由 Buffer 基类管理——`_get(i)` 计算绝对索引时不改 position, `get()` 时传 `nextGetIndex()`→`ix(checkIndex())` 推进。

### 2. Absolute vs Relative — 为什么读写分两种

场景: 固定位置的字段(如协议头的第 4 个字节是长度字段)需要"跳到指定位置读"而不影响当前的"读到哪了"游标。

源码路径: `X-Buffer.java.template:649-685` — `get(int index)` 调用 `checkIndex(index)` 后 `_get(index)` 直接读, 不推进 position。`X-Buffer.java.template:615-634` — `get()` 调 `nextGetIndex()` 内部先 `checkIndex(position++)` 再 `_get`。`Heap-X-Buffer.java.template:164-207` 把两种调用统一到 `hb[ix(index)]` —— `ix()` 加上数组偏移后直接数组索引。

关键设计: relative 操作用 `nextGetIndex/nextPutIndex` 先检查后推进——position 作为整体前移(普通的 post-increment, 非线程安全), 保证调用前后的 index 一致性。absolute 操作绕过 position, 让协议解析器可以"偷看"固定偏移而不改变 readerIndex——这也是 Netty ByteBuf 的 `getByte(int)` 的源头。

数据流: `nextGetIndex()` 内部用 `checkIndex(position++)` 的副作用推进指针 ➜ `_get(ix(p))` 读取底层字节到调用方。absolute 直接用 `_get(ix(index))` 返回——只读不改。

### 3. flip — 写的终点变成读的起点

场景: 你刚用 `buf.put(data)` 写入了 100 字节, 现在要从中读取数据——但读要从第 0 个字节开始、到第 100 个字节结束。

源码路径: `Buffer.java:449` — `flip()` 三步: `limit = position`(写到的位置变成读的上限) → `position = 0`(从头读) → `mark = -1`(放弃旧标记)。调用链: `flip()` 是 Buffer 的 final 方法 ➜ 所有子类复用同一个实现。

关键设计: flip 的三个赋值操作是不可分的——它们一起描述了"已完成的生产区段"的边界。如果没有 flip, 读写共享一个 position 时, 每次切换模式都要手动设 limit 和 position——四字段模型的 power 正体现在用一个方法调用完成模式切换。

数据流: `put(data)` 推进 position 到 100 ➜ `flip()` 设 limit=100, pos=0 ➜ `channel.write(buf)` 从 pos=0 读到 limit=100(relative get 推进 position) ➜ `compact()` 把 [pos,lim) 移到前面准备下一轮写。

### 4. compact — 未读完的数据不能丢

场景: TCP 不是消息边界——`channel.read(buf)` 可能只读了半个 HTTP 请求, 下次 read 需要追加到缓冲区末尾, 但前半段已经读走的数据占着前面的空间。

源码路径: `Heap-X-Buffer.java.template:261-275` — `compact()` 用 `System.arraycopy(hb, ix(position()), hb, ix(0), remaining())` 把未读区间 [pos,lim) 移到数组头部。然后 `position(remaining)` 设到数据末尾, `limit(capacity)` 重置写空间。

关键设计: O(N) 的 arraycopy 成本换取零额外内存分配——这正是 `byte[]` 模式的升华。Netty ByteBuf 的 `discardReadBytes()` 做了相同的事, 但 CompositeByteBuf 的实现是 O(1) 释放组件引用——从 O(N) arraycopy 到 O(1) free 的演进脉络从这里开始。

数据流: channel.read 推进 position➜compact 把 [pos,lim) 前移➜limit 恢复到 capacity➜下一轮 channel.read 从 position(剩余数据尾)继续读。

### 5. clear / rewind / mark — 三种复位

场景: 读完了、准备重新读、或者需要临时"偷看一眼"然后退回来。

源码路径: `Buffer.java:421` — `clear()` 设 position=0/limit=capacity(准备全新的写)。`Buffer.java:471` — `rewind()` 设 position=0(limit 不变, 重新读同一段)。`Buffer.java:380,396` — `mark()`=记 position 到 mark, `reset()`=恢复 mark→position, 若 mark=-1 抛 InvalidMarkException。

关键设计: clear 不管数据——它只复位指针——这是"性能之上的语义"。rewind 保留 limit 让你重复读同段数据(吞吐量计算/校验和)。mark/reset 是 peek 模式——读完消息头发现是不关心的类型则 reset 退回。

数据流: clear→pos=0,limit=cap(全量可写)➜rewind→pos=0,limit不动(全量可重读)➜mark→存pos➜reset→恢复pos。

### 核心悬念

**"flip/compact 状态机让每个 Handler 都必须知道自己处于写模式还是读模式。Netty Pipeline 的每个 Handler 怎么独立读写同一份数据而不互相踩脚？答案在 Ch4 ByteBuf 的双指针模型——readerIndex/writerIndex 分离让读和写不再共享一个 position。"**

→ 引出 §1.2 分配与内存 — 四字段模型解决了"怎么读/写", 但"数据存在堆还是堆外"决定了 GC 行为和 JNI 传输成本。HeapByteBuffer 和 DirectByteBuffer 从一个 allocate() 分叉。
