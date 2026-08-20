# Ch3 Selector 工程陷阱 — NIO 裸露的坑

> Cluster C: 3 KPs | 依赖 §3.1 §3.2 | §3.2 → §3.3

### 1. select() 空轮询 bug — NIO 最著名的生产级陷阱

场景: 服务端运行良好，突然 CPU 飙到 100%，但日志里没有任何异常，连接量也没暴涨。`select()` 不断返回 `0`，但没有任何 key 就绪——线程陷入死循环般空转。这就是 NIO 在 Linux 上最著名的空轮询 bug。

源码路径: `EPollSelectorImpl.doSelect(...)` 中，`EPoll.wait(epfd, pollArrayAddress, NUM_EPOLLEVENTS, to)` 被调用后，正常情况下：
- 有事件就绪 → `numEntries > 0` → `processEvents` 处理
- 无事件就绪且 timeout 到达 → `numEntries == 0` → 正常返回
- 被中断 → `numEntries == IOStatus.INTERRUPTED` → 调整 timeout 后重试（`EPollSelectorImpl.java:118-130`）

但在某些内核版本 / 系统调用边界条件下，`epoll_wait` 可能返回 0 但并非真正超时——线程醒来后发现 `numEntries == 0`，`processEvents(0, action)` 返回 0，于是 RippleEffect: select 返回 0 给 SocketImpl → 调用方进入下一轮 select() → epoll_wait 再次立刻返回 0 → 死循环。

JDK 11 的 `EPollSelectorImpl` 本身不检测这种情况。它只是忠实地把 epoll_wait 的返回值传给上层。JDK `Selector.java:105-108` 的合同说 select 会"add the keys of channels ready to perform an operation to the selected-key set, or update the ready-operation set of keys already in the selected-key set"——但如果 `numEntries == 0`，既不 add 也不 update，等于什么都没做。

关键设计: 这个 bug 不是 JDK 代码逻辑错误，而是 `epoll_wait` 系统调用本身在特定场景下的假唤醒。Netty 的解法在 `NioEventLoop` 中：用 `SELECTOR_AUTO_REBUILD_THRESHOLD=512` 检测连续空 select，超过阈值后 `rebuildSelector()` 创建新 Selector 并迁移全部 key（见 Ch5 EventLoop）。

数据流: `epoll_wait` 假唤醒 → `numEntries == 0` → processEvents 返回 0 → select() 返回 0 → 调用方 while 循环回到 select() → epoll_wait 再次立刻返回 0 → CPU 100% 空转。

### 2. wakeup 竞态 — 打断 select 的时序窗口

场景: 单线程 select loop 正在 `select()` 阻塞。另一个线程想让 loop 尽快醒来处理新任务，于是调用 `wakeup()`。但如果你对 wakeup 的时序细节理解不够，就可能踩到竞态。

源码路径: `EPollSelectorImpl.wakeup()` 向 pipe 的写端 `fd1` 写入一个字节（`EPollSelectorImpl.java:250-261`）。`doSelect(...)` 中 `EPoll.wait` 监听 pipe 的读端 `fd0`，收到这个字节后返回，然后 `clearInterrupt()` 会 drain 掉 pipe 里的数据并重置 `interruptTriggered` 标志（`EPollSelectorImpl.java:264-268`）。

竞态窗口在于时机：如果 wakeup() 写入的字节在 `clearInterrupt()` 之前就到了 fd0，那 `interruptTriggered` 已被设为 true，这次 wakeup 的确有效；但如果 wakeup() 发生在 `clearInterrupt()` 已经 drain 完 fd0 之后、下一轮 `EPoll.wait` 之前，那写入的字节会留在 pipe 里，下一轮 `EPoll.wait` 立刻读到它而立刻返回——这其实是一种"提前唤醒"，但它不会造成正确性问题，只会造成一次额外的空 select 返回。

关键设计: `wakeup()` 的合同是幂等的——多次调用等价于一次（`Selector.java:254-256`）。JDK 用 `interruptTriggered` 标志确保不会重复写入 pipe 字节，但这个标志只在 `clearInterrupt()` 时才被重置。所以竞态窗口与 wakeup 的"至少一次"语义并不冲突——wakeup 保证 select 一定被打断，但不保证只打断一次。

数据流: 线程A `select()` 阻塞 → 线程B `wakeup()` → `write1(fd1, 0)` → `interruptTriggered=true` → `epoll_wait` 返回 → `processEvents` 发现 fd0 就绪 → `clearInterrupt()` drain fd0 + `interruptTriggered=false` → 线程A 回到下一轮 select()。

### 3. selectedKeys 累积陷阱 — 消费者不收尾的全部后果

场景: §3.1 已经从概念上讲过 selectedKeys 不是每轮新建快照，§3.2 也讲了 `iter.remove()` 必须做。这里把完整的生产级后果拼起来：如果你忘了 remove，会发生什么？

源码路径: `SelectorImpl.processReadyEvents(...)` 中，如果 key 已经在 selectedKeys 里，只会 `translateAndUpdateReadyOps` 按位并上新 ready set，但不会把 key 从 selectedKeys 移除（`SelectorImpl.java:291-294`）。这意味着：

1. **重复处理**: key 处理完没 remove → 下一轮 `selectedKeys.iterator()` 仍然看到它 → 按 readyOps 分流 → 重复执行 read/write/accept。
2. **readyOps 不清零风险**: 如果新的 `rOps` 与旧的 ready set 按位并后仍然包含某个事件位，你以为"消费过了"但其实这个位从未被 clear 过。
3. **cancel 后仍残留**: key 被 `cancel()` 后，`processDeregisterQueue()` 会在下次 select 时将 key 从 selectedKeys 移除（`SelectorImpl.java:259`），但在 cancel 与下次 select 之间的窗口里，遍历 selectedKeys 仍可能看到这个已失效的 key——`isValid()` 返回 false，但 key 还在集合里。

关键设计: 正确模式是遍历 selectedKeys 时：1) `iter.next()` → 2) `if (!key.isValid()) { iter.remove(); continue; }` → 3) 按 readyOps 分流处理 → 4) `iter.remove()`。

数据流: select() 返回 → 遍历 selectedKeys → key1 (readable) → 处理 → 忘了 iter.remove() → 下一轮 select() → key1 仍在 selectedKeys → 如果 processReadyEvents 又更新了它的 readyOps → 遍历到 key1 → 重复处理。

### 核心悬念

**"Ch1-Ch3 的全部陷阱——Buffer flip/compact 状态机、Direct 的 Cleaner 不可预测回收、ByteBuffer 的 array 不可移植、Channel 的 read()==0/accept 阻塞、Selector 的空轮询/wakeup 竞态/selectedKeys 累积——这些都不是 JDK 的偶发 bug，而是 JDK NIO 设计层面的固有局限。Netty 重新发明了 ByteBuf（双指针 + 引用计数 + 池化），重新发明了 EventLoop（空轮询检测 + rebuild + 任务队列），重新发明了 ChannelOutboundBuffer（OP_WRITE 按需）。Ch4 先从缓冲区开始：为什么 Netty 要用 readerIndex/writerIndex 替代 position/limit？flip 的痛点为什么值得一整个新 API 来解决？"**

→ 引出 Ch4 Netty ByteBuf — 重新发明了缓冲区。
