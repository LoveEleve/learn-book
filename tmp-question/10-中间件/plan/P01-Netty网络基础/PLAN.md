# P01：Netty 网络通信基础 — 详细规划

> Phase: P01 | 周期: 1-2周 | 书: 1本 (MinerU) + 1本配套(NIO基础)
> 依赖: 无（独立入口，P02/P03/P06 的基石）

---

## 一、为什么 P01 是第一站

Netty 不是"一个消息队列"，而是**所有 Java 网络中间件的通信层**。以下依赖链说明问题：

```
Netty（Reactor线程模型 + Pipeline责任链 + 零拷贝）
  ├── RocketMQ Remoting模块（底层直接使用 Netty）
  ├── Nacos 2.x gRPC（gRPC Java 基于 Netty transport）
  ├── Dubbo 网络层（Dubbo Protocol 默认 Netty）
  ├── Seata TM/RM 通信（Netty channel 连接）
  ├── Sentinel Dashboard → Client 推送（Netty HTTP）
  └── Elasticsearch Transport（Netty4Transport）
```

**不懂 Netty，你看到的永远是 API 层，看不到通信层发生了什么。**

---

## 二、书单

### 主书：《Netty 原理剖析与实战》（傅健）

| 维度 | 信息 |
|------|------|
| MinerU 路径 | `10-中间件-消息队列/Netty 原理剖析与实战/` |
| MinerU 质量 | ❌ 仅626行，大量HTML span标签，章节内容严重不完整 |
| 处理策略 | **必须重新提取或换源**——现有 full.md 不可用 |
| 原书特点 | Netty 项目贡献者撰写，含300+关键代码、40+常见疑难点、7个扩展案例 |

| 章 | 核心知识点 | 与后续 Phase 的接口 |
|----|-----------|-------------------|
| 第1章 Netty初印象 | EventLoop/Pipeline/ChannelHandler 三大核心组件 | RocketMQ RemotingClient、Nacos GrpcClient 都基于同样的组件 |
| 第2章 准备工作 | 开发环境搭建、第一个 Netty 示例 | 搭建可实验的 Netty 本地环境 |
| 第3章 数据编码 | Encoder/Decoder、粘包/拆包处理 | RocketMQ RemotingCommand、Kafka 协议编解码 |
| 第4章 封帧 | 帧解码器(LineBased/DelimiterBased/LengthFieldBased) | RocketMQ 自定义协议 LengthFieldBasedFrameDecoder |
| 第5章 网络编程模式 | Reactor模式、Boss/Worker线程模型 | P6 Dubbo 的 NettyServer/NettyClient |
| 第6章 线程模型 | EventLoopGroup详解、零拷贝(CompositeByteBuf/FileRegion) | Kafka sendfile 零拷贝对比 |

### 配套书：《NIO与Socket编程技术指南》（高洪岩）

| 维度 | 信息 |
|------|------|
| 位置 | MinerU `12-Java框架-Spring/` 目录 |
| 用途 | 补足 Netty 书缺失的 **Java NIO 底层原理**（Buffer/Channel/Selector） |
| 章节 | Buffer原理、FileChannel、SocketChannel、Selector多路复用、AIO |

> **学习顺序**：先读 NIO 书 → 建立 Java NIO 心智模型 → 再读 Netty 书 → 理解 Netty 如何在 NIO 之上抽象出 Reactor 模式

---

## 三、源码阅读清单

| 源码模块 | 关键类 | 行数/方法 | 阅读目的 |
|---------|--------|----------|---------|
| EventLoop | `NioEventLoop.java` | `run()` → `processSelectedKeys()` → `processSelectedKey()` | 理解 Reactor 主循环到底在干什么 |
| EventLoopGroup | `NioEventLoopGroup.java` | `newChild()`,构造函数链 | 理解 Boss/Worker 线程分组 |
| Channel | `AbstractNioChannel.java` | `doRegister()`, `doBeginRead()` | Channel 如何注册到 Selector |
| Pipeline | `DefaultChannelPipeline.java` | `addLast()`, `fireChannelRead()` | 责任链触发顺序 |
| ChannelHandler | `ChannelInboundHandlerAdapter.java` | `channelRead()` | Handler 如何传递事件 |
| ByteBuf | `PooledByteBuf.java`, `Unpooled.java` | `alloc()`, 引用计数 | 内存池 vs 非池化，refCnt 泄露 |
| 零拷贝 | `CompositeByteBuf.java`, `FileRegion.java` | — | 与 Kafka FileChannel.transferTo 对比 |

---

## 四、关键设计哲学（"为什么这样设计"）

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么用 Reactor 模式而不是 Proactor？** | Linux AIO 真正的异步 IO 不完善 | Java NIO 基于 epoll，是 IO 多路复用（同步非阻塞），Reactor 天然适配 |
| **为什么 Boss 线程单独分组？** | 单线程 accept + 业务处理混在一起 | accept 是串行操作（TCP 握手），业务读写是并行操作——分离避免 accept 卡住 |
| **为什么用 Pipeline 责任链而不是扁平 handler 列表？** | 所有 handler 遍历一遍 | 责任链支持"拦截-处理-传递"，入站出站分离，ByteToMessageDecoder 可以半包暂存 |
| **为什么 ByteBuf 引用计数？** | GC 回收 | 网络数据量大 + 高频，GC 压力不可接受——引用计数 + 对象池 = 零 GC 路径 |
| **为什么 NioEventLoop 绑死一个线程？** | 一个 EventLoop 多个线程 | 无锁化设计——所有 Channel 的 IO 事件在同一个线程处理，不需要同步 |

---

## 五、生产陷阱（可复现）

| # | 陷阱 | 复现代码思路 | 根因 |
|---|------|------------|------|
| 1 | **ByteBuf 内存泄露** | 在 `channelRead()` 中不调用 `ReferenceCountUtil.release(msg)`，持续压测 | Netty 默认使用池化内存，不释放导致 OOM |
| 2 | **Handler 顺序错误导致解码失败** | 把 `LengthFieldBasedFrameDecoder` 放在 `StringDecoder` 之后 | 帧解码器必须在编解码器之前——先拆包再解码 |
| 3 | **EventLoop 阻塞导致大量 Channel 超时** | 在 channelRead 中 `Thread.sleep(5000)` 或执行数据库同步查询 | EventLoop 一个线程处理 N 个 Channel，一个阻塞全卡 |
| 4 | **写操作不判断 isWritable()** | 高并发下不停 `ctx.writeAndFlush()` 不看 Channel 水位 | 发送缓冲区满 → OOM / 写超时 |
| 5 | **SO_BACKLOG 太小导致连接被拒绝** | 调小 `SO_BACKLOG=1`，启动大量并发连接 | TCP 三次握手队列满，新连接直接 RST |

---

## 六、面试 Q&A（含面试官意图）

| 问题 | 面试官意图 | 标准回答主线 |
|------|----------|-------------|
| "Netty 的线程模型是怎样的？" | 考察是否理解 Reactor 模式 | Boss线程(accept) → Worker线程(IO读写) → EventLoop绑定Channel(无锁) |
| "Netty 的零拷贝和操作系统零拷贝有什么区别？" | 考察是否混淆了概念 | Netty的"零拷贝"= CompositeByteBuf合并/FileRegion传输；OS零拷贝= sendfile syscall 绕过用户态；Kafka用sendfile，Netty用堆外内存+FileRegion |
| "Pipeline 中 inbound 和 outbound 的执行顺序？" | 考察责任链理解 | inbound: head→tail, outbound: tail→head；关键：write 是 outbound 事件 |
| "Netty 的 ByteBuf 为什么要引用计数？" | 考察内存管理意识 | 避免GC + 池化复用 + 多handler间安全共享引用 |
| "你怎么排查 Netty 的内存泄露？" | 考察生产排障能力 | `-Dio.netty.leakDetection.level=PARANOID` + 堆dump + ResourceLeakDetector 报告 |

---

## 七、章节依赖

```
上游：无（P01 是整个中间件学习的入口）

下游（P01 被引用）：
  P02 Kafka：      Kafka 用 sendfile 零拷贝 vs Netty FileRegion 零拷贝对比
  P02 RocketMQ：   RocketMQ Remoting 模块直接使用 Netty（RemotingCommand编解码 ↔ Ch3/Ch4）
  P03 Etcd：       Etcd 用 Go 的 epoll 但概念对等（gnet vs Netty），比较线程模型差异
  P06 Dubbo：      Dubbo Protocol 默认 Netty transport（Boss/Worker ↔ Ch5）
  P06 Sharding：   代理模式可能与 Netty 异步 IO 对比
```

---

## 八、MinerU 质量应对

当前 Netty 书 full.md 只有 626 行 + 大量 HTML span 标签，**不可用于学习**。

建议方案（按优先级）：
1. **重新用 MinerU 提取 PDF**（使用 `layout` 模式而非默认模式，可能改善表格/代码块提取）
2. **寻找替代源**——GitHub 上有 Netty 源码分析系列（netty-in-action, netty-learning），可作补充
3. **直接读 Netty 4.x 源码 + Javadoc**——Netty 的 Javadoc 质量极高，EventLoop/Pipeline/ChannelHandler 的类注释就是设计文档

---

## 九、产出物（Phase C）

```
md/P01-Netty/
├── D-01-EventLoop与Reactor线程模型.md     # 核心：Boss/Worker分组 + NioEventLoop run() 主循环
├── D-02-Pipeline责任链与Handler.md         # 入站/出站事件流，ByteToMessageDecoder半包处理
├── D-03-ByteBuf内存管理与零拷贝.md          # 池化/引用计数/CompositeByteBuf/FileRegion
├── D-04-编解码与封帧协议.md                # TCP粘包拆包 + LengthFieldBasedFrameDecoder
├── D-05-Netty在生产中间件中的应用汇总.md    # RocketMQ Remoting / Nacos gRPC / Dubbo transport 对比
└── D-06-生产陷阱与面试Q&A.md               # 5个可复现陷阱 + 面试问题精讲
```

## 十、学习检查清单

- [ ] 能画出 `NioEventLoop.run()` 的完整调用链（select → processSelectedKeys → pipeline.fireChannelRead → handler.channelRead）
- [ ] 能手写一个 LengthFieldBasedFrameDecoder 配置解决自定义协议粘包
- [ ] 能对比 Netty CompositeByteBuf vs Kafka sendfile 的零拷贝差异
- [ ] 能写出 5 个生产陷阱的复现代码
- [ ] 能解释为什么 NioEventLoop 不阻塞（绑死单线程 + 异步化）
