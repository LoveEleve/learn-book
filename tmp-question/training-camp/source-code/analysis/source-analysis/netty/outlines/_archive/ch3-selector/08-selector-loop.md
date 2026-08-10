# §3.2 单线程 select 循环 — 文章大纲

## 读者基线

已从 §3.1 掌握 Selector 的三步模型（register→select→selectedKeys）、SelectionKey 的四事件和 interestOps/readyOps 区别。现在需要理解**一个完整的单线程 select 循环如何结合 accept 和 read 事件**。

## Pass 0: 设计上下文

- 传统的 BIO 模型：每个连接一个线程 → `ServerSocket.accept()` 阻塞 → `new Thread(handler)` → 线程爆炸
- NIO 的 select 循环是 **Reactor 模式**的基础——一个线程通过 Selector 多路复用所有 I/O 事件
- 这个模式是 Netty EventLoop 的思想来源（NioEventLoop = select 循环 + 任务队列）

## Pass 1: 扫描结果

- 核心模式: register(OP_ACCEPT) → select → 遍历 selectedKeys → if acceptable(register new) → if readable(read+process) → keyIterator.remove() → loop
- 关键操作: `keyIterator.remove()` 是 selectedKeys 清理的关键、`key.cancel()` 移除不再需要的 Channel
- 标记问题 ≥5:
  1. select 循环中 accept 新连接后为什么需要立即设为非阻塞？什么时候 register OP_READ？
  2. `selectedKeys` 的 Iterator 为什么需要 `remove()`——直接 `selectedKeys.clear()` 不行吗？
  3. 如果 read 返回 -1（EOF）应该做什么？仅仅是 unregister 还是要 close channel？
  4. 单线程 select 循环如何处理慢业务逻辑？一个 Handler 处理 10 秒会阻塞所有其他 Channel
  5. 什么时候需要用 OP_WRITE 而不是每轮循环都写？

## 概念依赖链

```
§3.1 三步模型 → §2.1 configureBlocking(false) → §2.2 read/write 语义 → 完整 select 循环(accept+read+cleanup) → §2.1 的 beginRead
```

## 叙事顺序

**开篇场景：BIO 的一个连接一个线程**

从 §3.1 的 Selector 三步模型接到真实场景——传统的 BIO echo 服务器：

```java
ServerSocket server = new ServerSocket(8080);
while (true) {
    Socket client = server.accept();        // 阻塞——等新连接
    new Thread(() -> {                       // 每个连接一个线程
        InputStream in = client.getInputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) {          // 阻塞——等数据
            client.getOutputStream().write(buf, 0, n);  // 只写读到的字节
        }
    }).start();
}
```

1000 个并发连接 = 1000 个线程。每个线程的大部分时间在阻塞等待。NIO 的解决方案：单线程 select 循环。

**第一层：完整的 select 循环骨架**

```java
Selector selector = Selector.open();
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.bind(new InetSocketAddress(8080));
serverChannel.configureBlocking(false);                          // §2.1
serverChannel.register(selector, SelectionKey.OP_ACCEPT);        // §3.1

while (true) {
    selector.select();                                            // 阻塞直到有事件

    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
    while (keyIterator.hasNext()) {
        SelectionKey key = keyIterator.next();
        keyIterator.remove();                                     // 🔑 必须手动移除

        if (key.isAcceptable()) {
            // 新连接到达
        } else if (key.isReadable()) {
            // 可读数据到达
        }
    }
}
```

步骤分三块：**注册**（配置 ServerSocketChannel）、**等待**（select 阻塞）、**分发**（遍历 selectedKeys 按事件类型处理）。`selectedKeys` 的清理有两种方式：处理一个 remove 一个（`keyIterator.remove()`），或全部处理完一次性 `selectedKeys.clear()`——本质相同，选哪种取决于是否需要逐个移除。

**第二层：accept 新连接——在循环中动态注册 OP_READ**

```java
if (key.isAcceptable()) {
    ServerSocketChannel server = (ServerSocketChannel) key.channel();
    SocketChannel client = server.accept();          // 非阻塞: null 或无新连接
    if (client == null) continue;

    client.configureBlocking(false);                 // §2.1: 必须非阻塞
    client.register(selector, SelectionKey.OP_READ,
                    ByteBuffer.allocate(1024));        // 第三个参数: attachment
}
```

`accept()` 在非阻塞 ServerSocketChannel 上可能返回 null（§2.1 讲过）。新接到的客户端 Channel **立即**注册 OP_READ 到同一个 Selector——**单线程开始管理多个 Channel**。注意：只注册 OP_READ，不注册 OP_WRITE（§3.1 讲过的陷阱）。

**第三层：处理 read 事件——完整的读取+处理+EOF 清理**

```java
if (key.isReadable()) {
    SocketChannel client = (SocketChannel) key.channel();
    ByteBuffer buf = (ByteBuffer) key.attachment();  // §3.1: attachment
    int n = client.read(buf);                        // §2.2: read 语义

    if (n == -1) {
        key.cancel();                                // EOF: 取消注册
        client.close();                              // 关闭 Channel
        continue;
    }
    if (n == 0) continue;                            // 非阻塞: 暂无数据

    buf.flip();                                      // §1.1: 切换读
    byte[] data = new byte[buf.remaining()];
    buf.get(data);
    processData(data);                               // 业务处理——但这是单线程！
    buf.compact();                                   // §2.3: 腾空间
}
```

三个关键决策：

1. **attachment** 绑定 ByteBuffer——每个 Channel 有自己的 read buffer，不用每次分配
2. **EOF（-1）时 `key.cancel()` + `channel.close()`**——把 Channel 从 Selector 注销，释放资源
3. **processData 在 select 循环里同步执行**——如果一个请求的业务处理需要 10 秒，这 10 秒内整个 Selector 停摆

**第四层：单线程 select 的局限——业务逻辑阻塞**

第三点是最严重的生产问题。1000 个连接的 select 循环被一个慢业务请求拖住 10 秒——999 个连接的数据到了但没人处理。这就是为什么 NIO 编程的最佳实践是：**select 循环只做 I/O 读写，业务逻辑交给线程池**：

```java
// 错误: select 循环内直接处理业务
processBusinessLogic(data);  // 阻塞整个 select 循环

// 正确: 拆为两步
executor.execute(() -> processBusinessLogic(copiedData));  // 异步处理
// select 循环继续处理下一个 key
```

这是 Netty EventLoop 的核心理念——EventLoop 负责 I/O 多路复用，业务处理交给 ChannelHandler（在 EventLoop 上串行执行或提交到业务线程池）。

**结尾悬念**

你已经掌握了 select 循环的完整形态。但 JDK 的 Selector 本身有几个著名的工程陷阱——epoll 空轮询 bug 会让 CPU 100%、`selectedKeys` 的清理如果有遗漏会堆积旧事件。§3.3 将揭示这些问题并展示 Netty 是如何系统性地解决它们的。

## 核心悬念

**读完能回答**：一个完整的 select 循环如何结合 accept、read 和 cleanup？`keyIterator.remove()` 和 `key.cancel()` 的区别是什么？为什么单线程 select 循环的业务处理应该是异步的？attachment 如何避免每轮都分配新的 ByteBuffer？

## 文章边界

- **在本篇内**: select 循环骨架 → accept 动态注册 → read 处理 + EOF 清理 → 单线程阻塞问题 + 线程池分离
- **不属于本篇**: 空轮询 bug（§3.3）、selectedKeys 陷阱（§3.3）、Netty EventLoop 实现（Ch5）
