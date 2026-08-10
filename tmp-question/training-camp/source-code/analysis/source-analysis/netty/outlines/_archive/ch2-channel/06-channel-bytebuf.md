# §2.3 Channel 和 ByteBuffer 的协作 — 文章大纲

## 读者基线

已从第 1 章掌握 ByteBuffer 全部 API（四字段、Heap/Direct、视图与陷阱），已从 §2.1-2.2 理解 Channel 双模式和 read/write 语义。现在需要把两套知识**组装成一个完整的收发循环**。

## Pass 0: 设计上下文

- 传统 `Socket` 的收发是两套独立 API：`InputStream.read(byte[])` 读进 `byte[]`、`OutputStream.write(byte[])` 从 `byte[]` 写出
- NIO 的统一抽象使得 `Channel.read(ByteBuffer)` 和 `Channel.write(ByteBuffer)` 共享同一个 buffer 对象——但这个统一也引入了 position/limit 的管理负担
- 完整的收发循环 = §1.1 的四字段 + §2.2 的返回值语义 + 组装代码

## Pass 1: 扫描结果

- 接收循环: read → flip → get → compact → read — 标准 NIO 四步
- 发送循环: put → flip → write（处理部分写）→ compact → 继续
- 回声循环: read → flip → write — 最简单的收发组合
- 标记问题 ≥5:
  1. 接收循环中 `read()` 之后、`flip()` 之后——如果 `read()` 返回 0 意味着什么？需要 flip 吗？
  2. 发送循环中 `write()` 返回比请求少的字节，ByteBuffer 的 position 已经被推进了多少？
  3. 回声循环为什么可以直接 write 不用 compact/clear——因为 write 消耗的正是 flip 后 readable 的字节？
  4. 多轮接收中 `compact()` vs `clear()` 的选择——什么时候只能用 compact？
  5. HeapByteBuffer vs DirectByteBuffer 的选择如何受收发循环影响？

## 概念依赖链

```
§1.1 四字段 + §2.2 read/write 语义 → 接收循环 (read→flip→get→compact) → 发送循环 (put→flip→write→loop) → 回声组合 → buffer 大小的工程选择
```

## 叙事顺序

**开篇场景：两个学过的工具，还没组装过**

第 1 章理解了 ByteBuffer 能把字节读到哪、第 2.1-2.2 章理解了 Channel 能收数据。现在把两者拼起来——一个最简单的"读取 channel 数据并打印"程序：

```java
ByteBuffer buf = ByteBuffer.allocate(1024);  // §1.1 学过的 allocate
SocketChannel channel = ...;                  // §2.1 学过的 SocketChannel
int n = channel.read(buf);                   // §2.2 学过的 read 语义
// 此时 buffer 处于"写模式"——数据在 [0, position) 之间
// 要读出来，需要 §1.1 学过的 flip()
```

**第一层：接收循环 — read → flip → get → compact**

本节先用阻塞模式演示完整循环——`read()` 在无数据时阻塞，离开循环意味着 EOF。非阻塞模式下的轮询留到 Selector 章节处理。

```java
while (channel.read(buf) != -1) {
    buf.flip();                              // 写↦读切换
    while (buf.hasRemaining()) {
        System.out.print((char) buf.get());  // 逐字节消费
    }
    buf.compact();                           // 把未读数据移到开头
}
```

每一步的 ByteBuffer 状态（用四字段 %s 标注）：

```
read 前:    [pos=0, lim=1024, cap=1024] — 准备接收
read 后:    [pos=5, lim=1024, cap=1024] — 读到 5 字节
flip 后:    [pos=0, lim=5, cap=1024]    — 切换到读
get × 5 后: [pos=5, lim=5, cap=1024]    — 全部消费
compact 后: [pos=0, lim=1024, cap=1024] — 腾出空间，准备下一轮
```

`compact()` vs `clear()` 的选择——多轮接收中用 `compact()` 是唯一正确的通用选择：它既保留未读数据（中途 break 出循环），又在全部消费后等价于 `clear()`（remaining=0 → pos=0, lim=capacity）。`clear()` 只能用于"确定全部读完且马上一口气读完不中断"的场景——有风险。

**第二层：发送循环 — put → flip → write（带循环）**

```java
buf.put("Hello, World!".getBytes());
buf.flip();
while (buf.hasRemaining()) {
    channel.write(buf);  // §2.2 讲过，可能部分写
}
```

发送循环的关键是 `write()` 返回后的 ByteBuffer 状态——position 已经推进了 `n` 步。如果 write 只写了部分字节（n < remaining），`hasRemaining()` 仍为 true，下一轮 write 从推进后的 position 继续写。**ByteBuffer 的 position 自动跟踪了"还剩多少要写"——你不需要手动管理 offset。**

**第三层：回声服务器 — 收发组合的极简版**

```java
while (channel.read(buf) != -1) {
    buf.flip();
    channel.write(buf);         // 把刚才读到的全部写回去
    buf.compact();
}
```

这是所有 NIO 代理/网关/中间件的骨架——读到什么就转发什么。回声循环中的 `compact()` 看起来多余（数据都写完了，remaining=0），但它是正确的通用写法：**`compact()` 在 remaining=0 时等价于 `clear()`**——都是 pos=0, lim=capacity。用 compact 即使未来改为非阻塞模式也不会有 bug——它永远安全。

**第四层：Buffer 大小的工程选择**

`ByteBuffer.allocate(1024)` 中的 1024 应该设多大？太小了每次读一点触发多轮 compact（每次 O(n) 拷贝），太大了浪费内存。经验法则：不小于 TCP 接收缓冲区的典型大小（通常 8KB~64KB），与 `SO_RCVBUF` 的默认值匹配。对于静态内容（如 HTTP response body），用 `allocateDirect` 获得零拷贝 I/O。

**结尾悬念**

收发循环学会了吗？现在可以手动轮询 1000 个 Channel 各读一轮。但这和一个线程处理 1000 个连接的差距还差最后一块拼图——怎么知道哪个 Channel 有数据？Selector 是 NIO 的最后一道门。下一章。

## 核心悬念

**读完能回答**：完整的接收循环中 ByteBuffer 的 position/limit 在每一步（read→flip→get→compact）如何变化？发送循环中 write 部分写后 position 如何自动跟踪"还剩多少"？为什么回声循环不需要显式的 compact/clear？什么场景必须用 compact 而非 clear？

## 文章边界

- **在本篇内**: 接收循环四步状态 → 发送循环部分写自动跟踪 → 回声组装 → buffer 大小选择
- **不属于本篇**: Selector（§3.1）、Netty 的 ByteBuf 收发循环（§4-7）
