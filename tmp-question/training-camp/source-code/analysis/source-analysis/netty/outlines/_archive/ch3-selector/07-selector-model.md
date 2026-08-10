# §3.1 Selector 模型 — 文章大纲

## 读者基线

已从第 1 章掌握 ByteBuffer、从第 2 章掌握 Channel 的阻塞/非阻塞双模式和 read/write 语义。从 §2.3 的悬念接过来——"单线程怎么管理 1000 个连接？"

## Pass 0: 设计上下文

- 5 个平台特定实现：Linux=EPollSelectorImpl、macOS=KQueueSelectorImpl、Solaris=DevPoll+EventPort
- `Selector.open()` → `SelectorProvider.provider()` → 自动选择平台最优实现
- `SelectionKey` 是 Channel 注册到 Selector 后返回的"凭证"——四种 readyOps 事件 + interestOps 兴趣集 + attachment 附件
- Selector 解决了 §2.1-2.3 遗留下的核心问题：不轮询就知道哪个 Channel 有数据

## Pass 1: 扫描结果

- 核心类: `Selector` (abstract)、`SelectionKey`、`SelectorImpl` (abstract)
- 平台实现: `EPollSelectorImpl` (Linux, JDK11)、`KQueueSelectorImpl` (macOS) 等 5 种
- 4 种事件: `OP_ACCEPT`(ServerSocket 新连接)、`OP_CONNECT`(客户端连上)、`OP_READ`(有数据可读)、`OP_WRITE`(可写)
- 标记问题 ≥5:
  1. `select()` 在内核里到底做了什么？为什么它能"同时等 1000 个 Channel"？
  2. `selectedKeys()` 返回的 Set 为什么每次用完需要手动 `clear()`？和普通的 Java Set 有什么不同？
  3. `register()` 为什么要求 Channel 必须是非阻塞模式？blocking channel 为什么不能注册？
  4. `SelectionKey` 的 `interestOps` 和 `readyOps` 的区别是什么？`isReadable()` 怎么实现的？
  5. `Selector.open()` 怎么决定用 epoll、kqueue 还是默认 Selector？`SelectorProvider` 的选择链是什么？

## 概念依赖链

```
§2.x 的双模式 + §2.3 的轮询困境 → register(channel, selector, ops) → selectionKey 的四事件 → select() 的内核等待 → selectedKeys 的每次手动清理
```

## 叙事顺序

**开篇场景：用 §2.3 的收发循环处理 2 个连接**

从 §2.3 的结尾接过来。假设你有两个客户端连到了服务端——阻塞模式下一个线程只能等一个 Channel：

```java
// 阻塞模式——第一个 Channel 没数据，代码卡在这里
channel1.read(buf);  // 阻塞——channel2 的数据到了但没人处理
channel2.read(buf);  // 永远执行不到
```

如果非阻塞模式呢？你需要轮询——对每个 Channel 调 `read()` 看是否返回 >0。1000 个 Channel = 每轮 1000 次 `read()`，其中 999 次返回 0。这是 O(n) 空转——NIO 的 `read()` 返回 0 只是不阻塞，不等于"不需要代价"。

**第一层：Selector 的核心概念——register + select + selectedKeys**

Selector 把轮询**推给操作系统**。你告诉内核"我对这 1000 个 Channel 的 OP_READ 感兴趣"，内核在所有 Channel 都没数据时让当前线程**休眠**——等数据到了，内核唤醒线程，Selector 告诉你"这些 Channel 有数据了"。

三步模型：

```java
Selector selector = Selector.open();

// 步骤1: register — 告诉 Selector "我对这个 Channel 的什么事件感兴趣"
serverChannel.register(selector, SelectionKey.OP_ACCEPT);  // 新连接
clientChannel.register(selector, SelectionKey.OP_READ);     // 有数据可读

// 步骤2: select — 阻塞等待，直到至少一个 Channel 有感兴趣的事件就绪
selector.select();  // 内核在 epoll_wait() 中休眠，零 CPU

// 步骤3: selectedKeys — 拿到就绪事件的 Channel 列表
Set<SelectionKey> keys = selector.selectedKeys();
for (SelectionKey key : keys) {
    if (key.isAcceptable()) { /* 有新连接 */ }
    if (key.isReadable()) { channel.read(buf); }
}
```

这段代码的意义是：**单线程可以同时等 1000 个 Channel——只有有数据的 Channel 才醒来处理，没有则休眠。**

**第二层：SelectionKey — interestOps vs readyOps**

`SelectionKey` 是 Channel 注册到 Selector 的"凭证"。`SelectionKey.java` 定义了 4 种事件：

| 常量 | 值 | 触发条件 |
|------|:--:|------|
| `OP_CONNECT` | 8 | SocketChannel 非阻塞 `connect()` 未立即完成时，连接就绪信号 |
| `OP_READ` | 1 | Channel 接收缓冲区有数据可读 |
| `OP_WRITE` | 4 | Channel 发送缓冲区有空间可写——**几乎总是就绪**（详见下文） |

为什么 OP_WRITE 几乎总是就绪？§2.2 讲过 `SO_SNDBUF`——socket 发送缓冲区。绝大多数时间它都是有空间的（除非极端拥塞），所以如果注册了 OP_WRITE，`select()` 几乎每轮都会把它当作就绪事件返回——**回到轮询模式**。正确做法：只有在你确实有数据要发送时才把 OP_WRITE 注册到 interestOps，发送完立即取消。Netty 对此有完整的优化策略（后续详述）。

每个 SelectionKey 维护两个独立的操作集：
- `interestOps`：你对什么事件感兴趣（`register` 时设置，后续可通过 `key.interestOps()` 修改）
- `readyOps`：当前实际上什么事件就绪了（`select()` 返回后，由内核填充）

`OP_CONNECT` 的特殊性：非阻塞 `SocketChannel.connect()` 调用后可能立即返回 `false`（连接未完成）——此时用 OP_CONNECT 注册到 Selector，等连接完成时内核会通过 readyOps 通知。这是非阻塞连接的完整流程：`connect() → false → register(OP_CONNECT) → select() → finishConnect()`。

`key.isReadable()` 实质是 `(readyOps() & OP_READ) != 0`（`SelectionKey.java:353-354`）。`key.attachment()` 可以关联任意对象——比如把对应的 ByteBuffer 绑在 key 上，就绪时直接取用。

**第三层：select() 在内核里做什么**

`select()` 的 Java 实现因平台而异。在 Linux 上（JDK11），`Selector.open()` 返回 `EPollSelectorImpl`——底层是 `epoll_create` → `epoll_ctl(ADD)` → `epoll_wait` 三步。

```
Selector.open()  → new EPollSelectorImpl()
channel.register() → epoll_ctl(fd, EPOLL_CTL_ADD, cfd, EPOLLIN)
selector.select()  → epoll_wait(epfd, events, timeout) → 内核休眠
                   → 有数据到达 → 内核唤醒 → 填充 readyOps
selectedKeys()     → 返回就绪的 SelectionKey 集合
```

`OP_READ` 映射到 `EPOLLIN`，`OP_WRITE` 映射到 `EPOLLOUT`，`OP_ACCEPT`/`OP_CONNECT` 也是 epoll 事件的映射。关键是 `epoll_wait` 的复杂度——它是 O(就绪事件数) 而不是 O(总 Channel 数)。1000 个 Channel 中只有 5 个有数据，epoll 只处理 5 个。

**结尾悬念**

你学会了 Selector 三步——但 `selectedKeys()` 返回后还有一个隐藏的陷阱：**处理完一次就绪事件后，需要手动 `selectedKeys.clear()`**，否则下次 select 返回的集合会包含已经被处理过的旧事件。这不是 bug 是设计——但这个设计让几乎所有 NIO 初学者都踩过坑。下一节将揭示 Selector 的所有工程陷阱。

## 核心悬念

**读完能回答**：`select()` 在内核中做了什么——为什么它能同时等 1000 个 Channel 而 CPU 零消耗？`interestOps` 和 `readyOps` 的区别是什么？`SelectionKey` 的四种事件什么时候触发？为什么 `register()` 要求 Channel 必须是非阻塞模式？

## 文章边界

- **在本篇内**: Selector 三步模型 → SelectionKey 四事件 → interestOps vs readyOps → select() 内核原理 → 平台实现选择链
- **不属于本篇**: selectedKeys 手动清理陷阱（§3.3）、空轮询 bug（§3.3）、单线程 select 循环完整代码（§3.2）
