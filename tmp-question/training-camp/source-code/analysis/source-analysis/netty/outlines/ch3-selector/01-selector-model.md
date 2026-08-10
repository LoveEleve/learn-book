# Ch3 Selector 模型 — 四种事件与 selectedKeys 陷阱

> Cluster A: 5 KPs | 依赖 Ch2 Channel | Ch3 → §3.2

### 1. register — Channel 正式进入 Selector 管理

场景: Ch2 的非阻塞 SocketChannel 已经创建并配置好了——但谁负责告诉"这个 Channel 有数据可读了"？Selector 是调度中心——每个 Channel 需要向它注册自己关心的事件。

源码路径: `SelectableChannel.register(Selector sel, int ops, Object att)` — `SocketChannel` 和 `ServerSocketChannel` 都继承自 `SelectableChannel`。注册流程: `SelectionKey key = new SelectionKeyImpl()` → `key.interestOps = ops` → `selector.registeredKeys.add(key)` → `key.attach(att)`。`SelectorImpl` 内部维护三个集合: `keys`(所有注册的 Key)、`selectedKeys`(select 后更新的就绪 Key)、`cancelledKeys`(已取消等待清除的 Key)。

关键设计: register 必须在 EventLoop 所在的线程执行——否则 `AbstractSelectableChannel.register()` 内部有 `synchronized (regLock)` 保护, 但多线程注册仍可能导致 Selector 行为不确定。Netty 通过 `inEventLoop()` 检查保证 register 始终在 EventLoop 线程执行。`att`(attachment)参数让每个 Channel 可以附带一块数据(通常是 ByteBuffer 或 Handler 引用), select 返回后 `key.attachment()` 直接拿到——不额外维护 Map。

数据流: `channel.register(selector, OP_READ, buf)` → `SelectionKeyImpl` 创建, ops=OP_READ → selector.registeredKeys.add(key) → select() 后若 Channel 可读 → key.readyOps 含 OP_READ → `key.attachment()` 取 buf → 读取数据。

### 2. 四种事件 — 位掩码区分四类就绪

场景: `select()` 返回了——SelectionKey 集里混着可读的、可写的、accept 新连接的、connect 完成的。怎么区分？

源码路径: `SelectionKey.java:296-332` — `OP_READ=1<<0`(有数据可读)、`OP_WRITE=1<<2`(发送缓冲区有空间——几乎总是为 true)、`OP_CONNECT=1<<3`(非阻塞 connect 完成)、`OP_ACCEPT=1<<4`(ServerSocketChannel 有新连接)。位掩码组合: `OP_READ | OP_WRITE | OP_ACCEPT`。`SelectionKey.isReadable() = (readyOps() & OP_READ) != 0` — 等价位与。

关键设计: `OP_WRITE` 是四事件中的"异常值"——TCP 发送缓冲区几乎总是有空闲, 所以 `OP_WRITE` 几乎是**永久就绪**。如果你注册了 `OP_WRITE` 却没有数据要发送, select 会立刻返回——线程空转。正确用法: 只有有数据要发送时才注册 `OP_WRITE`, 发送完后立即 `key.interestOps(interestOps & ~OP_WRITE)` 移除写兴趣。NIO 新手最常见的 bug 是注册了 `OP_READ | OP_WRITE` 导致 select 永远不会阻塞。

数据流: `select()` 返回 → 遍历 `selectedKeys` → `key.readyOps()` 检查位掩码 → `OP_READ`→调用 `channel.read(buf)` → `OP_ACCEPT`→`server.accept()` → `OP_CONNECT`→`channel.finishConnect()` → `OP_WRITE`→小心陷阱(只是通知"可以写了"不是"有数据就绪")。

### 3. selectedKeys — 不清理就累积

场景: 你的 `select()` 返回了 3 个就绪 Channel——你处理了它们。但下一次 `select()` 返回时, selectedKeys 集合里**还有**上一次的 3 个 Key。你不手动移除的话, 循环会重复处理已经消费掉的数据——Channel 空转 CPU 100%。

源码路径: `Selector.selectedKeys()` 返回 `Set<SelectionKey>` ——它不是每次 select 重创的, 是新就绪的 Key **追加**到集合中。`SelectorImpl.processReadyKeys()` 内部: `selectedKeys.add(key)`——select 返回后, 就绪的 Key 被加入但**从不自动移除**。正确做法: 遍历时 `iterator.remove()` 或遍历后 `selectedKeys.clear()`。

关键设计: 不清理 selectedKeys 导致 CPU 100% 空转——这是 NIO 最著名的坑。因为 select 返回时 Key 还在, 你循环从头处理——发现没有数据(已经被上一次消费了)但还是循环一次。Netty 在 `NioEventLoop.processSelectedKeys()` 中通过 `selectedKeys.reset(i+1)` 重置 SelectedSelectionKeySet 数组来清理——用数组替代 HashSet 让清理更高效。

数据流: `select()` → selectedKeys.add(就绪key1, 就绪key2) → 遍历处理 → **未清理** → 下次`select()` → 没有新就绪Channel → selectedKeys 仍有 key1,key2 → 重复处理(空操作) → CPU 空转。

### 4. wakeup — 远程打断阻塞的 select

场景: 线程 A 正在 `selector.select()` 阻塞等待——线程 B 有一个新 Channel 需要立即注册到 Selector。怎么唤醒正在阻塞的线程 A？

源码路径: `Selector.wakeup()` — 内部使用 `EPollSelectorImpl.wakeup()`: 创建一个 `eventFd`(Linux eventfd 系统调用) → `eventFd.write(1)` 写入 1 字节 → `epoll_wait` 监听到 eventFd 可读 → 立即返回。kqueue(macOS) 用 `pipe()` 替代。wakeup 幂等——多次 wakeup 在 `epoll_wait` 返回后即被消费, 不累积。

关键设计: wakeup 机制让多线程与 Selector 可以协作——一个线程负责注册新 Channel, 一个线程负责 select 循环。但 wakeup 有竞态: 线程 A 刚从 select 返回, 线程 B 在 A 的 `selectedKeys.clear()` 之前注册了新 Channel 并调了 wakeup——但 select 已经返回了, wakeup 被下次 select 消费。Netty 用 `wakenUp` 标记(AtomicBoolean)来处理这个竞态——select 返回后检查 `wakenUp.get()`, 若为 true 则再调一次 `selector.wakeup()` 防止唤醒信号丢失。

数据流: Thread B: `selector.wakeup()` → `eventFd.write(1)` → epoll_wait 返回 → Thread A: `select()` → 处理就绪事件 → `selectedKeys.clear()` → 下一轮 `select()`。

### 5. cancel — Channel 从 Selector 注销

场景: Channel 关闭时需要从 Selector 注销——不能直接调 `channel.close()` 就不再管——Selector 内部还持有这个 Channel 的 SelectionKey 引用。

源码路径: `SelectionKey.cancel()` — 内部将 Key 加入 `cancelledKeys` 集合(不执行注册表修改)→ 下一次 `select()` 开头 `SelectorImpl.processDeregisterQueue()` → 遍历 cancelledKeys → 从 registeredKeys 移除 → 从 selectedKeys 移除 → 调 `AbstractSelectableChannel.removeKey()`。不立即处理 deregister 是因为 `select()` 流程进行中不允许修改注册表。

关键设计: cancel 是异步的两步操作——`key.cancel()` 标记→下次 select 开头清理。这导致一个副作用: cancel 后到下次 select 之间, 这个 Key 仍然在 selectedKeys 中(如果之前在)且 `isValid()` 返回 false。正确代码: 处理 selectedKeys 时, 对每个 Key 先检查 `key.isValid()` 和 `key.isAcceptable()/isReadable()/isWritable()`。Netty 在 `processSelectedKey()` 中第一步就检查 `key.isValid()`, 失效的 Key 直接跳过。

数据流: `channel.close()` → `key.cancel()` → cancelledKeys.add(key) → 下次 `select()` → `processDeregisterQueue()` → 从 registeredKeys/selectedKeys 移除 → GC 回收。

### 核心悬念

**"Selector 把 1000 个 Channel 的等待从 1000 个线程的忙等变成一个线程的 epoll_wait。但 epoll 有一个 JDK 公认的 bug(JDK-6427854): epoll_wait() 有时会被虚假唤醒——select 返回 0 但没有任何 Channel 真的就绪——导致 select 循环空转 CPU 100%。Netty 的 SELECTOR_AUTO_REBUILD_THRESHOLD=512 怎么检测这个 bug, 检测到后怎么用 rebuildSelector0() 创建新 Selector 并迁移全部注册? §3.2 的单线程 select 循环会展开完整的事件处理流程——selectedKeys 遍历、OP_WRITE 有条件注册、cancel 的异步清理。"**

→ 引出 §3.2 单线程 select 循环 — 四种事件、selectedKeys 清理、OP_WRITE 陷阱、cancel 异步——这些机制合起来就是 Netty `NioIoHandler.run()` 的前身。
