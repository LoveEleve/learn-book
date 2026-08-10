# Netty 源码分析 — 书级全局规划

> 状态: 待用户确认
> 原则: 读者基线 = `byte[]` + Java 基础，不假设 NIO 知识
> 上卷讲 NIO（Netty 的地基），下卷讲 Netty（从地基到高楼）
> 产出日期: 2026-08-06

---

## 读者基线

```
读者知道: byte[], Object, 基本 I/O 概念 (InputStream/OutputStream)
读者不知道: NIO (ByteBuffer / Channel / Selector) — 由上卷讲述
读者不知道: Netty 任何概念 — 由下卷讲述
```

---

## 上卷: Java NIO 基础

### 第1章 NIO ByteBuffer — 缓冲区怎么用？

| 篇 | 标题 | 核心问题 |
|:--:|------|------|
| 1.1 | ByteBuffer 核心抽象 | position/limit/capacity 三指针是什么？和 `byte[]` 的区别在哪？ |
| 1.2 | HeapBuffer vs DirectBuffer | 堆内和堆外分配有什么区别？Direct 的堆外内存谁来管？ |
| 1.3 | ByteBuffer 的视图与陷阱 | duplicate/slice 数据共享、wrap 复用陷阱、equals 只比较 remaining、线程不安全 |

**域间连接**: 第1章最后留下悬念——"如果用 ByteBuffer 来接收网络数据，怎么写？谁告诉你数据到了？引出 Channel。"

**依赖**: 零（读者基线: `byte[]`）

### 第2章 NIO Channel — 怎么收发数据？

| 篇 | 标题 | 核心问题 |
|:--:|------|------|
| 2.1 | SocketChannel 和 ServerSocketChannel | NIO 的 Channel 和传统 Socket 的区别？阻塞模式 vs 非阻塞模式？ |
| 2.2 | Channel 的 read/write 语义 | read() 返回 0 是什么意思？write() 只写了一部分怎么办？ |
| 2.3 | Channel 和 ByteBuffer 的协作 | 读进 ByteBuffer、从 ByteBuffer 写出——完整的收发循环 |

**域间连接**: 第2章最后留下悬念——"如果我有 1000 个 Channel，怎么知道哪个 Channel 有数据可读？一个一个轮询吗？引出 Selector。"

**依赖**: 第1章（ByteBuffer 概念）

### 第3章 NIO Selector — 谁通知我数据到了？

| 篇 | 标题 | 核心问题 |
|:--:|------|------|
| 3.1 | Selector 模型 | select() 怎么工作的？SelectionKey 的四种事件 (OP_READ/OP_WRITE/OP_CONNECT/OP_ACCEPT) |
| 3.2 | 单线程 select 循环 | 一个线程 + 一个 Selector 怎么管理多个连接？和 BIO 的线程-per-连接对比 |
| 3.3 | Selector 的工程陷阱 | select() 空轮询 CPU 100%、过早唤醒、selectedKeys 要手动 remove——NIO 裸露的坑 |

**域间连接**: 第3章最后留下悬念——"Selector 的空轮询 bug 是 NIO 最著名的坑。Netty 怎么解决这个问题的？有没有一个更优雅的 select 循环？引出 EventLoop。"

**依赖**: 第2章（Channel）+ 第1章（ByteBuffer）

---

## 下卷: Netty 源码分析

### 第4章 ByteBuf — 为什么重新发明缓冲区？

| 篇 | 标题 | 核心问题 |
|:--:|------|------|
| 4.1 | ByteBuf 核心抽象 | 上卷第1章讲过的 flip/compact 痛点，ByteBuf 的双指针如何解决？容量扩展策略 (4MB 分界线)？ |
| 4.2 | 引用计数与字节序 | 上卷第1.2章讲过的 Direct 内存泄漏问题——retain/release 如何精确管理？BigEndian/LE 后缀/SwappedByteBuf |
| 4.3 | 内存管理 | 池化分配 (PoolArena/Chunk/Buddy/ThreadCache)，Heap vs Direct 的内存布局差异，ResourceLeakDetector 四个级别 |

**域间连接**: 第4章最后留下悬念——"ByteBuf 是 Netty 的血液，但血需要心脏来泵。数据什么时候读写、谁来驱动整个数据流？引出 EventLoop。"

**依赖**: 第1章（ByteBuffer）、第2章（Channel 收发）、第3章（select 循环）

### 第5章 EventLoop — 谁来驱动数据流？

预估: 4-5 篇文章（NioEventLoop + IoHandler + 双 EventLoopGroup + Selector 优化）

**域间连接**: "EventLoop 执行任务时，怎么知道任务成功还是失败了？异步结果怎么传递？引出 Promise/Future。"

**依赖**: 第4章（ByteBuf 是读写的数据载体）+ 第3章（Selector 基础）

### 第6章 Promise/Future — 异步结果怎么传递？

预估: 1-2 篇文章（DefaultPromise + listener + cause 传播）

**域间连接**: "EventLoop 驱动数据读写，数据流进 Channel 后，谁来处理这些数据？引出 Pipeline+Handler。"

**依赖**: 第5章（依赖 EventLoop 执行上下文）

### 第7章 Pipeline+Handler — 数据流过谁？

预估: 3-4 篇文章（DefaultChannelPipeline + HeadContext/TailContext + ChannelHandler 入站/出站 + ChannelHandlerContext 传播）

**域间连接**: "Pipeline 中的 Handler 怎么把一个对象变成字节流、反之亦然？引出 Codec 框架。"

**依赖**: 第5章（EventLoop 驱动 Pipeline）+ 第4章（ByteBuf 是流经的数据载体）

### 第8章 内存池化 — 内存从哪来？

预估: 2-3 篇文章（PoolArena + PoolChunk Buddy + PoolThreadCache + AdaptivePooling）

**域间连接**: 无（共享第4章 ByteBuf 的内存管理视角，这里深入池化分配算法）

**依赖**: 第4章（ByteBuf + ByteBufAllocator）

### 第9章 Bootstrap — 怎么启动 Netty？

预估: 1-2 篇文章（ServerBootstrap + Channel 初始化 + ServerBootstrapAcceptor + ChannelPool 连接复用）

**域间连接**: 共享第4-7章的根基——Bootstrap 是概念的"组装者"而非"发明者"

**依赖**: 第5章（EventLoopGroup）+ 第7章（Pipeline 初始化）

### 第10章 Codec 框架 — 怎么编解码？

预估: 3-4 篇文章（ByteToMessageDecoder + MessageToByteEncoder + 4 种拆包器 + ReplayingDecoder 状态机）

**域间连接**: "$10.1 讲透 TCP 粘包/拆包的编解码本质、10.2-4 讲 HTTP 作为最复杂编解码实例"

**依赖**: 第7章（Pipeline Handler 机制）

### 第11章 HTTP Codec — 最复杂编解码实例

预估: 2-3 篇文章（HttpRequestDecoder + HttpObjectAggregator + 分块传输 + WebSocket 升级）

**依赖**: 第10章（Codec 框架）

### 第12章 HTTP/2 Codec — 二进制帧革命

预估: 1-2 篇文章（二进制帧 + 流多路复用 + HPACK + 流控）

**依赖**: 第10章（Codec 框架）——注意：不依赖第11章（HTTP/2 是独立协议，不是 HTTP/1.1 的进化）

### 第13章 Epoll 原生传输 — Linux 内核的捷径

预估: 1-2 篇文章（JNI 直调 epoll + EdgeTriggered + writev + eventFd vs wakeup）

**域间连接**: "第5章 EventLoop 讲了 NIO 的 select 循环——第13章讲 epoll 替代 NIO selector。读者已经有对比基线。"

**依赖**: 第5章（EventLoop）+ 第3章（Selector 基础）

### 第14章 HashedWheelTimer — 时间的轮盘

预估: 1-2 篇文章（时间轮数据结构 + 100ms tick + workerState 三态）

**域间连接**: 无强依赖（独立算法）

**依赖**: 零（独立算法域）

---

## 全局统计

| 位置 | 域 | 预估篇数 | 优先级 |
|:--:|------|:--:|:--:|
| 第1章 | NIO ByteBuffer | 3 | — |
| 第2章 | NIO Channel | 3 | — |
| 第3章 | NIO Selector | 3 | — |
| 第4章 | ByteBuf | 3 | 🔴 A |
| 第5章 | EventLoop | 4-5 | 🔴 A |
| 第6章 | Promise/Future | 1-2 | 🟡 B |
| 第7章 | Pipeline+Handler | 3-4 | 🔴 A |
| 第8章 | 内存池化 | 2-3 | 🔴 A |
| 第9章 | Bootstrap | 1-2 | 🟡 B |
| 第10章 | Codec 框架 | 3-4 | 🟡 B |
| 第11章 | HTTP Codec | 2-3 | 🟡 B |
| 第12章 | HTTP/2 Codec | 1-2 | 🟡 B |
| 第13章 | Epoll 原生传输 | 1-2 | 🟡 B |
| 第14章 | HashedWheelTimer | 1-2 | 🟡 B |
| **合计** | **14 章** | **32-41 篇** | |

---

## 域间叙事连接

每一章最后都留一个悬念，自然引出下一章：

```
NIO ByteBuffer → "但数据怎么来？" → NIO Channel
NIO Channel → "1000个连接谁通知？" → NIO Selector
NIO Selector → "selector空轮询有解吗？" → Netty EventLoop
Netty ByteBuf → "谁来驱动读写？" → Netty EventLoop
EventLoop → "异步结果怎么回传？" → Promise/Future
Promise/Future → "数据流进Pipeline谁处理？" → Pipeline+Handler
Pipeline → "字节流怎么变对象？" → Codec 框架
Codec → "最复杂的编解码长什么样？" → HTTP Codec
```

---

## 方法论新增步骤

在方法论/01 中，**书级全局规划**应放在 00 域发现 + 04 方案选择之后、Pre-Pass 3（大域拆分）之前：

```
00 域发现 → 04 方案选择 → [书级全局规划] → Pre-Pass 3(大域拆分) → Per-Article Outline → 写作
```

书级规划产出: 读者基线 + 全局目录 + 每域预估篇数 + 域间叙事连接。
