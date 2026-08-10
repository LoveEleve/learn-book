# Ch4 CompositeByteBuf — 零拷贝聚合的 Component 体系

> Cluster F+G: 16 KPs | 依赖 §4.4 视图 | §4.4 → §4.5

### 1. 虚拟拼接 — 多个 ByteBuf 呈现为一个

场景: HTTP 响应 = header buffer(256B) + body buffer(16KB)。如果不用 Composite, 你需要 `alloc.buffer(256+16384)` → `writeBytes(header)` → `writeBytes(body)`——每次响应都拷贝一次 header 和 body。Composite 让它们零拷贝拼成一个大缓冲区。

源码路径: `CompositeByteBuf.java:44-48` — Composite 是"虚拟缓冲区"——不拷贝数据, 只维护 `components[]` 数组引用原始 buffer。`CompositeByteBuf.java:58-75` — `components[]` 动态数组 + `componentCount` 计数器, `capacity()` 通过 `components[size-1].endOffset` 累加偏移计算(CompositeByteBuf.java:838-841)。`maxNumComponents` 上限 + `consolidateIfNeeded()`——组件数超限时自动触发合并, 防止组件过多导致访问性能退化(CompositeByteBuf.java:566-573)。

关键设计: Composite 的构造有 `direct` 标记——决定 `allocBuffer(capacity)` 返回 heapBuffer 还是 directBuffer(CompositeByteBuf.java:1902-1904), 但组件本身可以是任意类型——direct Composite 可以包含 heap 组件(反之也可)。`capacity(newCapacity)` 双路径: 扩容→追加 padding 组件+超限触发 consolidate; 缩容→从尾部组件裁剪+removeCompRange 释放(CompositeByteBuf.java:844-886)。

数据流: `composite.addComponent(header, true)` → `newComponent(header)` → components[0] = new Component → `composite.addComponent(body, true)` → components[1] = new Component → `composite.capacity()` = header.readable + body.readable → `composite.getByte(0)` → `findComponent(0)` → components[0].buf.getByte(0)。

### 2. Component 双引用 — srcBuf / buf

场景: `composite.addComponent(pooledSlicedBuf)` ——pooledSlicedBuf 是一个 PooledSlicedByteBuf, 它的 parent 是 PooledDirectByteBuf。component 保存到两个引用: srcBuf=pooledSlicedBuf(原始), buf=PooledDirectByteBuf(解包后)。释放时需要操作原始 buf(因为引用计数在原始 buf 上)。

源码路径: `CompositeByteBuf.java:1913-1985` — `Component` 内部类, 双引用: `srcBuf`(原始)负责引用计数, `buf`(解包后)负责数据访问。`CompositeByteBuf.java:49` — 为什么双引用: PooledSlicedByteBuf.release() 需要作用在原始 buf(PooledDirectByteBuf)上, 而非切片包装器上。`CompositeByteBuf.java:1936-1942` — `srcIdx(index)` 映射到原始 buf(用于 release), `idx(index)` 映射到解包后 buf(用于数据访问)。`CompositeByteBuf.java:1962-1968` — `Component.slice()` 惰性创建并缓存——首次 `srcBuf.slice()` 后缓存, 复用避免重复创建。

关键设计: newComponent 逐层解包链(CompositeByteBuf.java:320-348): `WrappedByteBuf→SwappedByteBuf→SlicedByteBuf/DuplicatedByteBuf→真实底层`。每个 Component 直接引用最底层真实数据, 避免嵌套视图链降低访问性能。`addComponent0` 带 finally 回滚: 添加失败时调用 `buffer.release()`(CompositeByteBuf.java:280-310)。

数据流: `addComponent(pooledSlicedBuf)` → `newComponent(pooledSlicedBuf)` → 解包 Wrapped→SlicedByteBuf→提取 adjustment → 底层 = PooledDirectByteBuf → Component(srcBuf=pooledSlicedBuf, buf=PooledDirectByteBuf) → `Component.release()` → `srcBuf.release()`(释放的是 pooledSlicedBuf, 委托到 parent PooledDirectByteBuf)。

### 3. findComponent — 逻辑偏移到物理组件, O(log N)→O(1)

场景: `composite.getByte(50000)` ——你要在第几个组件里找? 50000 在 component[0](0-9999)还是 component[1](10000-49999)还是 component[2](50000-59999)?

源码路径: `CompositeByteBuf.java:920-945` — `toComponentIndex0(int offset)` 二分查找——O(log N)。`CompositeByteBuf.java:1615-1654` — `findComponent(int offset)` 带 lastAccessed 弱缓存——先检查 `lastAccessed.endOffset > offset && 前一个组件的 endOffset <= offset`, 如果命中返回(摊销 O(1)); 未命中回退到 `toComponentIndex0` 二分(CompositeByteBuf.java:1615-1654)。`CompositeByteBuf.java:1948-1954` — `Component.reposition(newOffset)`——组件插入/删除后整体偏移重算: `endOffset += move, srcAdjustment -= move, adjustment -= move`。

关键设计: lastAccessed 弱缓存是基于"顺序访问"的乐观假设——在处理读取消息时, 通常按顺序从 component[0]→component[1]→...读取, 缓存命中率接近 100%。只有一个缓存槽(不是 LRU)——因为场景是顺序的, 不需要多个槽。

数据流: `getByte(50000)` → `findComponent(50000)` → 检查 lastAccessed.endOffset(49999) > 50000? 否 → `toComponentIndex0(50000)` 二分 → component[2](50000-59999) → 更新 lastAccessed → `component[2].buf.getByte(50000 - 50000)`。

### 4. consolidate — 合并组件, 从零拷贝到一次拷贝

场景: HTTP 响应写完——header(bufferA) + body(bufferB) → Composite → 客户端需要完整字节流。`consolidate()` 把两个组件合并为一个连续 buffer——不再需要考虑跨组件读写。

源码路径: `CompositeByteBuf.java:1774-1793` — `consolidate0(int startComponent, int endComponent)`: `allocBuffer(totalCapacity)` → 遍历组件 → 每个 `component.transferTo(dst)`(writeBytes+free) → 替换首批组件的 Component。`Component.transferTo(dst)`(CompositeByteBuf.java:1957-1960): `dst.writeBytes(buf, readerIndex, length)` + `free()`——写入+释放一体。

关键设计: consolidate 的触发有两个场景——手动(`composite.consolidate()`)和自动(`maxNumComponents`超限→`consolidateIfNeeded()`)。consolidate 是一次性成本(所有组件的字节拷贝), 换取后续访问的 O(1) 性能(不需要 findComponent)。与 `discardReadComponents()` 的区别: discardReadComponents 释放已读组件(O(1), 不拷贝), consolidate 合并剩余组件(O(N), 拷贝数据)。

数据流: 3 个组件(header 256B, body 16KB, footer 128B) → `consolidate()` → `allocBuffer(256+16384+128)` → `transferTo(header)→transferTo(body)→transferTo(footer)` → 新 Component 替换 components[0], components[1..2] 移除 → `findComponent()` O(1)。

### 5. discardReadComponents — O(1) 释放已读组件

场景: `composite.readBytes(256)` 读完了 header——header 不再需要。`discardReadComponents()` 释放已读完的组件(header component), 调整 offset。

源码路径: `CompositeByteBuf.java:1798-1843` — `discardReadComponents()`: 遍历 components, 释放 `endOffset <= readerIndex` 的组件 → `removeCompRange(0, i)` 移除 → `adjustOffsets(i, -move)` 调整剩余组件的 offset → `readerIndex -= move`(CompositeByteBuf.java:1846-1900)。与 `discardReadBytes()` 的区别: discardReadComponents 释放整个组件(O(1)), discardReadBytes 对首个剩余组件做 `slice(readerIndex - first.offset, ...)` 保留已读指针后的未读部分。

关键设计: Composite 的 discardReadComponents 是 O(1)——直接释放组件引用。普通 ByteBuf 的 discardReadBytes 是 O(N) System.arraycopy(§4.1)。这是 Composite 性能优势的核心——当你有一个 1000 个 1KB 组件的 Composite 并顺序读取时, discardReadComponents 每读一个组件释放一个——O(1) 每次。

数据流: `composite.readBytes(256)` 读完 header → readerIndex=256 → `discardReadComponents()` → header component endOffset(256) <= 256 → `removeCompRange(0, 0)` 释放 → body/footer components offset 各减 256 → readerIndex=0(重新对齐)。

### 核心悬念

**"CompositeByteBuf 的 O(1) discardReadComponents 解决了 Ch1 NIO ByteBuffer 的 O(N) compact 问题。但 Component 的双引用(srcBuf+buf)让释放路径变得复杂——release 要操作 srcBuf(原始 pooled buffer), 但数据读取要操作 buf(解包后的底层 buffer)。§4.1 的引用计数让这个双引用体系成为可能——但 Composite 持有的多个子 buffer, 每个都有独立的引用计数, 泄漏检测怎么知道 'Composite 还没 release 但所有 component 都 release 了' 是一种 bug? Ch5 EventLoop 提供了答案——单线程事件循环让 Composite 的所有操作都在同一个线程上, 消除了多线程下双引用计数的不确定性。"**

→ 引出 Ch5 EventLoop — ByteBuf 的双指针(§4.1)、分配器体系(§4.2)、内存管理(§4.3)、视图操作(§4.4)和 Composite 聚合(§4.5)构成了 Netty 内存模型的全景。但数据在什么时候被读写? 谁在驱动这些操作？Ch5 EventLoop 的单线程事件循环和 SELECTOR_AUTO_REBUILD_THRESHOLD 将解答事件驱动架构的核心。
