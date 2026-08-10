# Ch3 单线程 select 循环 — 工程陷阱与完整 NIO 服务端

> Cluster B: 4 KPs | 依赖 §3.1 | §3.1 → §3.2

### 1. 完整 select 循环 — 三个步骤串联 Channel/Selector/ByteBuffer

场景: Ch1 的 ByteBuffer 解决了缓冲区状态管理, Ch2 的 Channel 解决了数据收发, §3.1 的 Selector 解决了多路复用——现在把它们组装成一个完整的 NIO 服务端: 一个线程 + 一个 Selector + N 个 SocketChannel = Reactor 模式。

源码路径: `while (true) {` `int n = selector.select();` // 阻塞等待就绪 → `Iterator<SelectionKey> iter = selector.selectedKeys().iterator();` → `while (iter.hasNext()) {` `SelectionKey key = iter.next();` `if (key.isAcceptable()) { // Accept 新连接` `ServerSocketChannel server = (ServerSocketChannel) key.channel();` `SocketChannel client = server.accept();` `client.configureBlocking(false);` `client.register(selector, OP_READ, ByteBuffer.allocate(1024));` `} else if (key.isReadable()) { // 读已有连接` `SocketChannel ch = (SocketChannel) key.channel();` `ByteBuffer buf = (ByteBuffer) key.attachment();` `int n = ch.read(buf);` `if (n == -1) { ch.close(); }` // EOF → 关闭 `if (n > 0) { buf.flip(); process(buf); buf.compact(); }` // 处理数据 `}` `iter.remove();` // **必须**手动移除 `} }`。

关键设计: 三个 Accept/Read/Write 的分支是 Reactor 模式的基础——Accept 是"新成员注册", Read 是"成员产生数据", Write 是"成员消费数据"。attachment 避免 Map 查找——`ByteBuffer buf = (ByteBuffer) key.attachment()` 直接从 Key 获取绑定的缓冲区, O(1)。与 BIO 对比: `while(true) { Socket client = server.accept(); new Thread(() -> handle(client)).start(); }` —— NIO 用一线程替代 N 线程, IO 等待占用的线程上下文切换成本归零。

数据流: 主循环 → `select()` 阻塞获取就绪 Key → Accept: `server.accept()` + `register(OP_READ)` 纳入管理 → Read: `read(buf)→flip→process→compact` → Write(仅在有数据时注册 OP_WRITE, 写完移除) → `iter.remove()` 防累积 → 下一轮 `select()`。

### 2. OP_WRITE 陷阱 — 几乎总是就绪的事件

场景: 你在处理完 Read 后做了 `processResponse(buf)`——需要把响应写回客户端。直接调 `channel.write(responseBuf)` 就行了——为什么要先注册 OP_WRITE？

源码路径: `SelectionKey.java:308` — `OP_WRITE = 1<<2`。TCP 的 socket send buffer 默认很大(Linux 通常 ~87KB, 可配 `SO_SNDBUF`), 在连接建立后这个 buffer 几乎总是有空间——所以 select 对 `OP_WRITE` 几乎总是返回就绪。如果注册了 `OP_WRITE` 且**没有**数据要发, select 会在空循环中不断返回——CPU 100%。

关键设计: OP_WRITE 的正确用法是**按需注册, 用后即删**。只有当你需要发送数据且 `channel.write(buf)` 返回 `0`(非阻塞模式下发送缓冲区满了)时, 才注册 `OP_WRITE`: `key.interestOps(key.interestOps() | OP_WRITE)`。下一次 select 返回 OP_WRITE 就绪时, 继续写——写完后立即移除: `key.interestOps(key.interestOps() & ~OP_WRITE)`。Netty 的 `ChannelOutboundBuffer` 内部自动处理这个——`addMessage→addFlush→nioBuffers→doWrite→OP_WRITE 按需 setUnwritable`。

数据流: 有数据要发 → `while(buf.hasRemaining()) { int n = channel.write(buf); if(n==0) break; }` → 若 `buf.hasRemaining()` → `key.interestOps(key.interestOps() | OP_WRITE)` → 下次 select 返回 → OP_WRITE 就绪 → 继续写 → 写完 → `key.interestOps(key.interestOps() & ~OP_WRITE)`。

### 3. cancel 的异步清理 — 两步注销

场景: 客户端关闭了连接, 服务端 `client.close()` 了——但 Selector 不会立即释放这个 Channel 的 SelectionKey。如果不在下次 select 前处理已 cancel 的 Key, 遍历 selectedKeys 时会遇到 `key.isValid()==false` 而 NPE。

源码路径: `SelectionKey.cancel()` 内部将 Key 加入 `cancelledKeys` 集合 → 下一次 `select()` 开头 `SelectorImpl.processDeregisterQueue()` → `cancelledKeys` 遍历 → 从 `registeredKeys` 移除 → 从 `selectedKeys` 移除 → 调用 `AbstractSelectableChannel.removeKey()`。关键——cancel 是异步两步: 1) `cancel()` 标记 → 2) 下次 select 清理。

关键设计: cancel 异步导致一个过渡期——在 cancel 已执行但 select 尚未处理 deregister queue 的期间, 这个 Key 仍在 `registeredKeys` 和可能的 `selectedKeys` 中, 但 `isValid()==false`。处理 selectedKeys 时必须先检查 `key.isValid()`——跳过失效的 Key。Netty 在 `processSelectedKey()` 中第一步就做此检查: `if (!k.isValid()) { return; }`。

数据流: `channel.close()` → `key.cancel()` → cancelledKeys 入队 → 下次 `select()` 开头 → `processDeregisterQueue()` 清理 registeredKeys/selectedKeys → Key 成为孤儿(Garbage Collected)。

### 4. NIO 服务端完整骨架 — Reactor 模式的最小实现

场景: 把 §3.1 的 register/select 模型、§3.2 的 Accept/Read/Write 循环、OP_WRITE 按需注册——组合成一个完整的、可工作的 NIO 服务端骨架。这个骨架就是 Netty 的 EventLoop + NioIoHandler 的前身。

源码路径: 完整流程六步: 1) `ServerSocketChannel.open().bind(addr).configureBlocking(false)` 2) `Selector.open()` → `server.register(selector, OP_ACCEPT)` 3) select 返回 → isAcceptable → `client = accept().configureBlocking(false)` → `client.register(selector, OP_READ, allocate(1024))` 4) isReadable → `read(buf)→flip→process→compact`, `n==-1→close()` 5) isWritable → `write(responseBuf)`, 写完移除 OP_WRITE 6) `iter.remove()`——每次迭代清理, 防累积。Ch2 的 accept 返回阻塞 SocketChannel 陷阱在这里——`configureBlocking(false)` 是第 2-3 步之间的必须操作。

关键设计: 这个骨架是 Reactor 模式的 NIO 最小实现——单线程驱动全部 I/O 事件。它与 Netty EventLoop 的区别是: Netty 把 Accept/Read/Write 三个分支分别注入 Pipeline 的 ChannelInboundHandler 和 ChannelOutboundHandler——用户不需要写 if-isAcceptable-else-if-isReadable 的 switch 分支, 只需编写 Handler。

数据流: ServerSocketChannel.bind(8080) → register(OP_ACCEPT) → 循环: select() → Accept→register(OP_READ) → Read→process→compact → Write→写完移除OP_WRITE → close 触发 cancel → GC。

### 核心悬念

**"Selector 的空轮询 bug 是 NIO 最著名的生产级陷阱——epoll_wait 被假唤醒, select 返回 0 但无就绪 Channel, CPU 100% 空转。Netty 的 Ch5 EventLoop 用了 SELECTOR_AUTO_REBUILD_THRESHOLD=512 来检测: 连续 512 次 select 都返回 0→认为是 bug→调用 rebuildSelector0() 创建新 Selector 并迁移全部注册→旧 Selector 关闭。但重建期间——新连接和已有连接的事件怎么处理才不会丢？答案在 Ch4 Netty ByteBuf: 在那之前, 先回答一个更基础的问题: Netty 为什么重新发明了缓冲区？JDK NIO 的 ByteBuffer 的 flip/compact 状态机和 hasArray/array 不可移植 API 是 Ch1-Ch3 的全部陷阱的根源。"**

→ 引出 Ch4 Netty ByteBuf — 重新发明了缓冲区。readerIndex/writerIndex 双指针替代单 position, CAS 引用计数替代 Cleaner, retainedSlice 保证视图生命周期。Ch1-Ch3 的全部陷阱(Cleaner 不可预测/array 不可移植/equals remaining 陷阱/selectedKeys 累积/OP_WRITE 永久就绪/accept 阻塞 Socket)——在 Netty 的 Ch4-Ch14 中逐一被解决。
