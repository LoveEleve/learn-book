# Netty 源码学习范围规划

> **基准**: Netty 4.2.15.Final  
> **数据源**: Netty 仓库 2487 文件逐包扫描（common 200 + buffer 93 + transport 208 + handler 176 + codec 系列 + resolver 等）  
> **边界**: 聚焦 Netty 核心（common / buffer / transport / handler / codec-base），协议编解码按需分层学习；但 HTTP / HTTP2 已证明不能只按边缘模块对待  
> **排除约束**: 当前阶段仍不把 `SSL/TLS` 升格进主线  
> **my-xhs 关联**: Netty 是 Spring WebFlux / Gateway / RocketMQ / Dubbo / gRPC 的底层传输

---

## 修订结论

经过两轮源码深扫，这份规划需要修正的，不是“补几个遗漏类名”，而是三类结构性偏差：

1. **把“类出现过”误当成“主题已规划”**。例如 `ResourceLeakDetector`、`ReferenceCounted`、`ChannelOutboundBuffer`、`FlushConsolidationHandler` 在旧规划里都出现过，但没有被升格为独立机制域。
2. **按包归类太强，按读者真实困惑建域太弱**。结果是“内存管理”“并发工具”“handler 扩展”里塞进了多个本应单独闭环的运行时主线。
3. **低估了出站运行时与 HTTP/2 的中心性**。`ChannelOutboundBuffer`、`WriteBufferWaterMark`、`PendingWriteQueue`、`flush`、`codec-http2` 都不是边角知识，而是会反复回流到上层框架的问题源头。

因此，这一版改按：**机制闭环 + 真实困惑 + 运行时中心性** 重新编排 Netty 学习域。

---

## 第 1 层：核心数据结构（3 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-1 | **ByteBuf 缓冲区模型** | 🔴 | buffer | AbstractByteBuf / ByteBuf / ByteBufAllocator / CompositeByteBuf / FixedCompositeByteBuf / SlicedByteBuf / DuplicatedByteBuf / ReadOnlyByteBuf / UnreleasableByteBuf / WrappedByteBuf / ByteBufUtil / ByteBufInputStream / ByteBufOutputStream / SwappedByteBuf / Unpooled | ByteBuf 双指针（`readerIndex` / `writerIndex`，源码验证：`0 <= readerIndex <= writerIndex <= capacity`）。vs JDK ByteBuffer 的区别（池化 / 组合 / 零拷贝 / 双指针）。`CompositeByteBuf` 如何组合多个 ByteBuf。`slice` / `duplicate` 视图语义。`ByteBufUtil` 在比较、编码、搜索上的位置。 |
| N-2 | **引用计数与对象所有权** | 🔴 | common + buffer | ReferenceCounted / ReferenceCountUtil / AbstractReferenceCounted / AbstractReferenceCountedByteBuf / ByteBufHolder / IllegalReferenceCountException | `retain/release/touch/refCnt` 到底约束什么？谁拥有对象，谁负责释放？跨 handler、跨 codec、跨 HTTP/HTTP2 对象传递时所有权如何转移？`deallocate()` 在何时触发？为什么这是 Netty 运行时基本法，而不是池化附属细节？ |
| N-3 | **派生视图与零拷贝生命周期** | 🔴 | buffer | AbstractDerivedByteBuf / AbstractPooledDerivedByteBuf / SlicedByteBuf / DuplicatedByteBuf / ReadOnlyByteBuf / CompositeByteBuf / DefaultByteBufHolder | 派生视图共享的到底是数据、索引还是生命周期？为什么“零拷贝”不等于“零管理”？派生 ByteBuf、ByteBufHolder、聚合对象在 release 时的责任边界是什么？ |

## 第 2 层：事件循环 + 通道（3 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-4 | **Channel + EventLoop 事件循环** | 🔴 | transport/channel + channel/nio + channel/socket + common/concurrent | NioEventLoop / NioEventLoopGroup / AbstractNioChannel / AbstractNioByteChannel / AbstractNioMessageChannel / NioServerSocketChannel / NioSocketChannel / NioIoHandler / IoHandler / IoHandlerContext / Channel / EventLoop / EventLoopGroup / SingleThreadEventExecutor / SingleThreadIoEventLoop / SingleThreadEventLoop / MultithreadEventExecutorGroup / EventExecutorChooserFactory / SelectedSelectionKeySet / AdaptiveRecvByteBufAllocator / ChannelOption / ChannelConfig | `NioEventLoop` 的线程模型、select 与任务执行如何交错、`IoHandler` 在 4.2 中如何切出 I/O 内核层、`AdaptiveRecvByteBufAllocator` 如何自适应。 |
| N-5 | **ChannelPipeline + ChannelHandler 处理器链** | 🔴 | transport/channel | DefaultChannelPipeline / AbstractChannelHandlerContext / ChannelHandler / ChannelInboundHandler / ChannelOutboundHandler / ChannelHandlerContext / ChannelHandlerMask / ChannelInboundHandlerAdapter / ChannelOutboundHandlerAdapter / ChannelDuplexHandler / ChannelInitializer / ChannelFuture / ChannelFutureListener / CombinedChannelDuplexHandler | `DefaultChannelPipeline` 双向链表。入站 / 出站事件如何传播。`ChannelHandlerMask` 为什么存在。`CombinedChannelDuplexHandler` 如何把两个方向 handler 收成一个逻辑单元。 |
| N-6 | **Bootstrap 启动流程 + 通道组/池** | 🔴 | transport/bootstrap + channel/pool + channel/group | AbstractBootstrap / ServerBootstrap / Bootstrap / AbstractBootstrapConfig / ServerBootstrapConfig / ChannelFactory / ChannelPool / SimpleChannelPool / FixedChannelPool / ChannelPoolMap / ChannelGroup / DefaultChannelGroup / ChannelMatcher | `doBind()` -> `initAndRegister()` -> register -> bind 主线。服务端启动流程。`ChannelPool` / `ChannelGroup` 如何扩展运行时能力。 |

## 第 3 层：出站运行时主线（3 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-7 | **ChannelOutboundBuffer + 水位线 + writability** | 🔴 | transport/channel | ChannelOutboundBuffer / WriteBufferWaterMark / PendingBytesTracker / MessageSizeEstimator / ChannelConfig | `write()` 之后消息先进哪里？`totalPendingSize` 如何驱动 high / low watermark？`channelWritabilityChanged` 什么时候触发？`bytesBeforeWritable/Unwritable` 解决什么观察问题？ |
| N-8 | **PendingWriteQueue + CoalescingBufferQueue** | 🔴 | transport/channel | PendingWriteQueue / PendingWrite / AbstractCoalescingBufferQueue / CoalescingBufferQueue / ChannelFlushPromiseNotifier | 业务侧“稍后再写”的消息怎么暂存？为什么这些队列也要纳入 writability / pending bytes 统计？消息合并、Promise 合并、失败清理怎么做？ |
| N-9 | **write / flush 语义 + FlushConsolidation** | 🔴 | transport/channel + handler/flush | AbstractChannel / DefaultChannelPipeline / ChannelOutboundInvoker / FlushConsolidationHandler | `write` vs `flush` 的运行时差异。为什么 flush 往往是 syscall 边界。为什么 Netty 要支持 flush 合并。`FlushConsolidationHandler` 在 read loop / 非 read loop / 不可写时分别怎么工作。 |

## 第 4 层：编解码骨架（3 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-10 | **字节流编解码骨架 + 拆包器** | 🔴 | codec-base | ByteToMessageDecoder / MessageToByteEncoder / ByteToMessageCodec / ReplayingDecoder / ReplayingDecoderByteBuf / DelimiterBasedFrameDecoder / LengthFieldBasedFrameDecoder / LineBasedFrameDecoder / FixedLengthFrameDecoder / LengthFieldPrepender / ProtocolDetectionResult | `ByteToMessageDecoder` 的 cumulation、`callDecode()`、remove/reentry。`MessageToByteEncoder` 的 encode→release→write。4 种拆包器。`ReplayingDecoder` 的 REPLAY 控制流。 |
| N-11 | **对象流编解码骨架** | 🔴 | codec-base | MessageToMessageDecoder / MessageToMessageEncoder / MessageToMessageCodec / MessageAggregator / CodecOutputList / DefaultHeaders / Headers / HeadersUtils / ValueConverter | Netty 不只处理字节，还处理对象流。`MessageAggregator`、`MessageToMessage*` 和 `DefaultHeaders` 构成的上层编解码骨架怎么工作？ |
| N-12 | **HTTP/1.1 编解码 + 聚合 + 压缩** | 🔴 | codec-http | HttpObjectDecoder / HttpRequestDecoder / HttpResponseDecoder / HttpServerCodec / HttpClientCodec / HttpObjectAggregator / HttpContentEncoder / HttpContentCompressor / FullHttpRequest / FullHttpResponse | HTTP 对象流模型。server/client codec 为什么要双工组合。`HttpMessage + HttpContent + LastHttpContent`。`HttpObjectAggregator` 三阶段。`HttpContentCompressor` 的 `Accept-Encoding` 协商与 `EmbeddedChannel` 子通道。 |

## 第 5 层：HTTP/2（3 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-13 | **HTTP/2 协议地基（帧 / Stream / HPACK / 流控）** | 🔴 | codec-http2 | DefaultHttp2FrameReader / DefaultHttp2Connection / HpackStaticTable / HpackEncoder / HpackDecoder / DefaultHttp2LocalFlowController / DefaultHttp2RemoteFlowController / Http2FrameTypes | 9 字节帧头、10 种帧类型、streamId 奇偶归属、连接级动态表、双层流控。为什么 HTTP/2 能在一条 TCP 连接上跑很多请求而不把状态搞混。 |
| N-14 | **HTTP/2 Netty API 层（FrameCodec / Multiplex）** | 🔴 | codec-http2 | Http2FrameCodec / Http2FrameCodecBuilder / Http2MultiplexHandler / Http2MultiplexCodec / Http2StreamChannel / Http2StreamChannelBootstrap / Http2FrameStream / Http2FrameStreamEvent | Netty 对用户暴露的 HTTP/2 pipeline 形态是什么？`Http2FrameCodec` 如何把 wire frame 映射成 `Http2Frame` 对象？`Http2MultiplexHandler` 如何把每个 Stream 变成 child channel？ |
| N-15 | **HTTP/2 encoder / decoder 主链** | 🔴 | codec-http2 | DefaultHttp2ConnectionEncoder / DefaultHttp2ConnectionDecoder / Http2ConnectionHandler / Http2ConnectionHandlerBuilder / StreamBufferingEncoder / WeightedFairQueueByteDistributor / UniformStreamByteDistributor / InboundHttp2ToHttpAdapter / Http2StreamFrameToHttpObjectCodec | 连接级编码/解码主链如何衔接 `Http2Connection`、流控、错误恢复、GOAWAY、upgrade、stream buffering 和 ByteDistributor。 |

## 第 6 层：内存管理 + 诊断（5 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-16 | **池化分配器总图** | 🟡 | buffer | PooledByteBufAllocator / PoolArena / PoolChunk / PoolChunkList / PoolSubpage / PoolThreadCache / SizeClasses / PooledByteBufAllocatorMetric | jemalloc 风格池化：arena / chunk / subpage / thread-cache 的整体关系。`PoolChunkList` 六级利用率链。默认 arena/page/chunk 参数如何定。 |
| N-17 | **内存泄漏检测与定位** | 🟡 | common + buffer | ResourceLeakDetector / ResourceLeakDetectorFactory / ResourceLeakTracker / ResourceLeakHint / SimpleLeakAwareByteBuf / AdvancedLeakAwareByteBuf / AbstractByteBufAllocator | 泄漏是怎么被发现的？采样级别 `SIMPLE / ADVANCED / PARANOID` 差别是什么？`touch()` / `record()` / leak-aware 包装如何工作？从 leak 日志如何追到业务代码？ |
| N-18 | **Recycler + FastThreadLocal + 对象复用基础设施** | 🟡 | common + common/concurrent + common/internal | Recycler / FastThreadLocal / FastThreadLocalThread / InternalThreadLocalMap / ObjectPool / RecyclableArrayList | Netty 如何避免到处 new 小对象？为什么 `FastThreadLocal` 用数组槽位而不是普通 `ThreadLocal` map？`Recycler` 如何在单线程 / 跨线程场景复用对象？ |
| N-19 | **Cleaner / 直接内存释放 / 平台适配** | 🟡 | common/internal + buffer | Cleaner / CleanerJava6 / CleanerJava9 / CleanerJava24Linker / CleanerJava25 / DirectCleaner / UnpooledUnsafeNoCleanerDirectByteBuf / VarHandleByteBufferAccess / OutOfDirectMemoryError | 直接内存最终如何被释放？为什么需要多版本 Cleaner 适配？JDK 版本差异怎样映射到 Netty 代码里？ |
| N-20 | **AsciiString / ByteProcessor / 轻量基础设施** | 🟡 | common | AsciiString / AsciiStringUtil / ByteProcessor / CharsetUtil / Signal / TypeParameterMatcher | 哪些“看起来像工具类”的组件其实在 HTTP、HTTP/2、codec、日志、matcher 中高频出现，并值得独立理解？ |

## 第 7 层：并发工具与调度（3 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-21 | **Promise / Future / Listener 通知模型** | 🟡 | common/concurrent | DefaultPromise / Promise / Future / AbstractFuture / PromiseAggregator / PromiseCombiner / DefaultFutureListeners | `setSuccess0` / `setFailure0` / `notifyListeners` 如何工作。为什么要限制 listener 栈深。Promise 聚合如何串联异步链。 |
| N-22 | **任务调度与 EventExecutor 辅助体系** | 🟡 | common/concurrent | EventExecutor / DefaultEventExecutor / DefaultEventExecutorGroup / AbstractScheduledEventExecutor / ScheduledFutureTask / GlobalEventExecutor / NonStickyEventExecutorGroup / UnorderedThreadPoolEventExecutor | I/O 线程之外的普通任务和定时任务如何执行。哪些任务必须回到 event loop，哪些可以旁路。 |
| N-23 | **HashedWheelTimer 时间轮** | 🟡 | common | HashedWheelTimer / Timer / TimerTask / HashedWheelBucket / Timeout | `tick & mask`、`remainingRounds`、绝对时间等待、异步 cancel。适合大量近似 I/O timeout，为什么不是高精度闹钟。 |

## 第 8 层：运行时 handler 扩展（3 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-24 | **超时 / 心跳 handler** | 🟡 | handler/timeout | IdleStateHandler / ReadTimeoutHandler / WriteTimeoutHandler / IdleStateEvent | readerIdle / writerIdle / allIdle 如何触发。它们和 Timer / 连接关闭语义的边界在哪里。 |
| N-25 | **流量整形 / 流控辅助 / 日志** | 🟡 | handler/traffic + handler/logging + handler/flow | AbstractTrafficShapingHandler / ChannelTrafficShapingHandler / GlobalTrafficShapingHandler / TrafficCounter / FlowControlHandler / LoggingHandler | 带宽限制、流量统计、流控辅助与调试日志如何叠加到 pipeline 中。 |
| N-26 | **大对象分块写出** | 🟡 | handler/stream | ChunkedFile / ChunkedInput / ChunkedNioFile / ChunkedStream / HttpChunkedInput | 大文件或大流如何以 chunk 方式写出，与出站缓冲 / flush / 水位线是什么关系。 |

## 第 9 层：平台原生传输（2 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-27 | **epoll 原生传输** | 🟡 | transport-classes-epoll | EpollEventLoop / EpollSocketChannel | Linux epoll 相对 NIO 的实现差异与收益。 |
| N-28 | **io_uring 原生传输** | 🟡 | transport-classes-io_uring | IOUringEventLoop / IOUring*Channel | io_uring 在 Netty 中的落点与边界。 |

---

## 旧规划明显低估、这次已补升格的主题

### 1. 诊断与所有权主线
- `ReferenceCounted` / `ReferenceCountUtil`
- `ResourceLeakDetector` / `ResourceLeakDetectorFactory`
- leak-aware buffer 包装链
- 这是“运行时正确性 + 排障”的主线，不再挂在池化域里当附属说明

### 2. 出站运行时主线
- `ChannelOutboundBuffer`
- `WriteBufferWaterMark`
- `PendingWriteQueue`
- `FlushConsolidationHandler`
- 这是背压、OOM、吞吐、批量刷出的交汇处，不能继续被拆散到 EventLoop / handler 扩展里

### 3. HTTP/2 不再按需
- `codec-http2` 经两轮探索，已确认不是边角包，而是 gRPC / Dubbo Triple 的必要地基
- 因此拆成：协议地基 / Netty API / encoder-decoder 主链 3 域

### 4. FastThreadLocal / Recycler 不再只是“并发工具”
- 它们是 Netty 内部对象复用和线程本地运行时的支撑面，必须独立出来理解

---

## 淘汰清单（修订版）

### 已探索并进入主线
| 子模块 | 文件数 | 域 |
|---|---|---|
| common | 200 | N-2/N-17/N-18/N-19/N-20/N-21/N-22/N-23 |
| buffer | 93 | N-1/N-2/N-3/N-16/N-17/N-19 |
| transport | 208 | N-4/N-5/N-6/N-7/N-8/N-9 |
| handler | 176 | N-9/N-24/N-25/N-26 |
| codec-base | 71 | N-10/N-11 |
| codec-http | 262 | N-12 |
| codec-http2 | 129 | N-13/N-14/N-15 |
| resolver | 20 | N-6 可按需引用 |
| resolver-dns | 61 | 网络扩展，可按需引用 |

### 继续按需，但不再简单贴“淘汰”标签
| 子模块 | 文件数 | 说明 |
|---|---|---|
| codec-compression | 61 | 已与 HTTP 压缩主线发生交叉，后续可专题化 |
| codec-http3 | 78 | 若继续 gRPC / QUIC 线需升格 |
| codec-classes-quic | 106 | 同上 |
| transport-classes-kqueue | 33 | macOS/BSD 原生传输，平台特定 |
| transport-native-unix-common | 31 | Unix Domain Socket，按需 |
| handler-proxy | 7 | 代理 handler，按需 |
| codec-dns / mqtt / redis / memcache / smtp / stomp / xml / haproxy / protobuf | — | 协议专项，按需 |

### 当前确认不进入主线
| 子模块 | 文件数 | 理由 |
|---|---|---|
| transport-sctp | 37 | SCTP 非主流 |
| transport-udt | 18 | UDT 非主流 |
| transport-rxtx | 6 | 串口通信，非主流 |
| codec-marshalling | 15 | 过时 |
| example | 208 | 示例代码 |
| microbench | 154 | 基准测试 |
| testsuite* | — | 测试套件 |
| jfr-stub / varhandle-stub / pkitesting / all/bom/dev-tools | — | 构建与工具 |
| handler/ssl | 体量大 | 当前阶段仍按你的要求不纳入主线 |

---

## 统计（修订后）

| | 数量 |
|---|---|
| 🔴 核心域 | **15** |
| 🟡 重要域 | **13** |
| 总计 | **28 域** |
| 相比旧版新增/拆分重点 | 泄漏检测、引用计数、派生视图、出站缓冲、flush、PendingWriteQueue、FastThreadLocal/Recycler、HTTP/2 API、HTTP/2 主链、轻量基础设施 |

---

## 与 Spring / RPC 生态的关联（修订）

| 上层域 | Netty 关联 | 关系 |
|---|---|---|
| WebFlux / Gateway | N-4/N-5/N-7/N-8/N-9/N-12 | EventLoop、Pipeline、出站背压、HTTP |
| Dubbo Triple / gRPC | N-13/N-14/N-15 | HTTP/2 frame/stream/flow-control / multiplex 地基 |
| RocketMQ / 自定义协议 | N-10/N-11/N-7/N-9 | 编解码骨架 + 出站缓冲 + flush |
| 大流量连接治理 | N-23/N-24/N-25 | Timer、Idle、流控、整形 |
| 内存问题排查 | N-2/N-3/N-16/N-17/N-18/N-19 | 引用计数、派生视图、池化、泄漏、对象池、Cleaner |

---

## 当前最值得优先补写的缺口顺序

1. **N-2 引用计数与对象所有权**
2. **N-17 内存泄漏检测与定位**
3. **N-7 ChannelOutboundBuffer + 水位线 + writability**
4. **N-9 write / flush + FlushConsolidation**
5. **N-14 HTTP/2 Netty API 层**
6. **N-18 Recycler + FastThreadLocal + 对象复用基础设施**
7. **N-15 HTTP/2 encoder / decoder 主链**
8. **N-16 池化分配器总图**

这 8 个主题，是修复旧规划失真的第一优先级。

---

## 本轮新增正文的系统总 Review

### Review 范围

本轮复核了以下新增主线正文及其 rewrite plan / review notes：

- Ch4-06：对象所有权与引用计数协议
- Ch4-07：内存泄漏检测与定位
- Ch5-03：FastThreadLocal、InternalThreadLocalMap 与 Recycler
- Ch7-05：ChannelOutboundBuffer 与 writability
- Ch7-06：write、flush 与 FlushConsolidation
- Ch7-07：ChannelOutboundBuffer.Entry 与 WriteTask 复用
- Ch8-05：池化分配器总图
- Ch8-06：PoolThreadCache 线程本地缓存与回收路径
- Ch8-07：池化分配器指标、ThreadCache 调优与诊断
- Ch12-02：HTTP/2 FrameCodec 与 Multiplex API 层
- Ch12-03：HTTP/2 ConnectionHandler、Encoder/Decoder 主链
- Ch12-04：gRPC / Dubbo Triple over HTTP/2 桥接
- Ch12-05：WeightedFairQueueByteDistributor

### 总体结论

- **主线闭环已成立**：所有权 -> leak detector -> 出站托管/背压/flush -> HTTP/2 API -> HTTP/2 连接主链 -> gRPC/Triple 桥接，已经形成跨模块叙事闭环。
- **方法论执行基本合格**：各篇都有问题开场、失败方案、文字心智图、源码证据、边界和篇末桥接；正文机械检查均无禁用词、无 fenced code block。
- **主要残余风险不是结构性错误，而是表述边界**：涉及“更快”“噪声”“公平”“指标下降”“性能收益”的句子，必须区分源码事实、设计动机和需要基准验证的性能判断。
- **篇间分工基本稳定**：Ch12-02 负责 frame/stream/child channel API 投影；Ch12-03/05 负责连接状态、流控资格和可发送额度；Ch12-04 负责上层 RPC 语义桥接；Ch8-05/06/07 分别负责池化总图、thread cache 使用方和指标诊断。

### 需要保留的系统性审查规则

1. **所有权和指标不能混为一谈**：`release()` 归零、thread cache 回收、arena metric 变化、chunk 销毁、OS 内存下降是不同层级事件。
2. **流控器和分配器不能混为一谈**：flow controller 判断 stream 是否 streamable；`WeightedFairQueueByteDistributor` 只在候选 stream 中分配可发送额度。
3. **RPC 桥接不能写成重做传输栈**：gRPC 更偏连接 handler 封装，Triple 更偏 FrameCodec/Multiplex/pipeline 组合，但二者都复用 Netty HTTP/2 主链。
4. **复用壳不能写成复用消息本体**：Entry、WriteTask、CodecOutputList、PoolThreadCache entry 的复用层级不同，均不能覆盖业务对象 ownership。
5. **测试证据不能外推成全局性能结论**：测试证明当前实现行为和边界，不自动证明所有部署环境的吞吐、延迟或 RSS 收益。

### 本轮确认的表述修复点

- `Ch4-06/07`：已将 ownership、refCnt、leak tracker、safeRelease 的职责边界分开。
- `Ch5-03`：已将 FastThreadLocal 的实现结构与性能结论分开，并统一清理/快速路径术语。
- `Ch7-05/06/07`：已明确 pipeline pending bytes、ChannelOutboundBuffer pending bytes、write/flush/remove 和 Entry/WriteTask 的阶段差异。
- `Ch8-05/06/07`：已将池化保留、thread cache 延迟、arena metrics 和真正 leak 分开；指标不再被写成 OS 内存真相。
- `Ch12-02/03/04/05`：已明确 FrameCodec/Multiplex/API、ConnectionHandler/Encoder/Decoder、RPC 桥接和额度分配器的分工。

---

## 规划域落地核查

| 域 | 当前状态 | 已落地正文 | 核查结论 |
|---|---|---|---|
| N-1 ByteBuf 缓冲区模型 | ✅ 完整 | Ch4-01~05 | 双指针、allocator、heap/direct、视图、Composite 已覆盖 |
| N-2 引用计数与对象所有权 | ✅ 完整 | Ch4-01 + Ch4-06 | Ch4-06 已补跨 handler/codec/write/HTTP2 ownership 主线 |
| N-3 派生视图与零拷贝生命周期 | 🟡 部分 | Ch4-04 + Ch4-06/07 | 基础已写，leak-aware 派生视图仍可做专门深审 |
| N-4 Channel + EventLoop | ✅ 完整 | Ch2~Ch5 | 事件循环、I/O、任务调度和通道基础已覆盖 |
| N-5 Pipeline + Handler | ✅ 完整 | Ch7-01~04 | pipeline、handler 类型、初始化和生命周期已覆盖 |
| N-6 Bootstrap + 通道组/池 | ✅ 完整 | Ch9 | Bootstrap 主线已覆盖，ChannelPool 可再做专项但非当前阻塞缺口 |
| N-7 OutboundBuffer + writability | ✅ 完整 | Ch7-03 + Ch7-05 | 主线和用户位/水位线已覆盖 |
| N-8 PendingWriteQueue + CoalescingBufferQueue | ❌ 未落地 | 仅在 Ch7-05/06 桥接 | 当前最明显的出站缺口，应优先补写 |
| N-9 write/flush + FlushConsolidation | ✅ 完整 | Ch7-06 | read loop、非 read loop、边界兜底已覆盖 |
| N-10 字节流编解码骨架 | ✅ 完整 | Ch10-01/02 | decoder、encoder、拆包器已覆盖 |
| N-11 对象流编解码骨架 | 🟡 部分 | Ch10-02 + Ch7-07 | MessageToMessage/aggregator/headers 仍缺独立闭环 |
| N-12 HTTP/1.1 编解码/聚合/压缩 | ✅ 完整 | Ch11-01/02 | 主链已覆盖 |
| N-13 HTTP/2 协议地基 | ✅ 完整 | Ch12-01 | frame、stream、HPACK、flow control 已覆盖 |
| N-14 HTTP/2 Netty API 层 | ✅ 完整 | Ch12-02 | FrameCodec、FrameStream、Multiplex、child channel 已覆盖 |
| N-15 HTTP/2 encoder/decoder 主链 | ✅ 完整 | Ch12-03/05 | 连接主编排、Encoder/Decoder、buffering、fair allocation 已覆盖 |
| N-16 池化分配器总图 | ✅ 完整 | Ch8-01~07 | 总图、thread cache、metrics/diagnostics 已覆盖 |
| N-17 泄漏检测与定位 | ✅ 完整 | Ch4-07 | detector、sampling、wrapper、report、排障路径已覆盖 |
| N-18 Recycler/FastThreadLocal | ✅ 完整 | Ch5-03 + Ch7-07 + Ch8-06 | 公共底盘和真实使用方已覆盖 |
| N-19 Cleaner/直接内存释放 | ❌ 未落地 | Ch4-03 仅有背景 | JDK 版本适配、Cleaner、OutOfDirectMemoryError 缺独立闭环 |
| N-20 AsciiString/ByteProcessor/轻量基础设施 | 🟡 部分 | Ch11/12/Ch5 零散引用 | 尚未形成轻量基础设施独立正文 |
| N-21 Promise/Future/Listener | ✅ 完整 | Ch6-01~03 | 状态、监听器、组合、ChannelPromise、scheduled 已覆盖 |
| N-22 EventExecutor 调度辅助 | 🟡 部分 | Ch5 + Ch6-03 | 基础存在，NonSticky/Global/Unordered 等未独立闭环 |
| N-23 HashedWheelTimer | ✅ 完整 | Ch14-01 | 时间轮主线已覆盖 |
| N-24 超时/心跳 handler | ❌ 未落地 | 无独立正文 | IdleState/ReadTimeout/WriteTimeout 缺正文 |
| N-25 流量整形/流控辅助/日志 | ❌ 未落地 | 无独立正文 | TrafficShaping/FlowControl/Logging 缺正文 |
| N-26 大对象分块写出 | ❌ 未落地 | 无独立正文 | ChunkedInput/File/Stream 与出站主线缺闭环 |
| N-27 epoll 原生传输 | ❌ 未落地 | 无独立正文 | 平台传输主线缺正文 |
| N-28 io_uring 原生传输 | ❌ 未落地 | 无独立正文 | 4.2 I/O 抽象与 io_uring 对接缺正文 |

### 落地统计

- ✅ 完整：**18 域**
- 🟡 部分：**5 域**
- ❌ 未落地：**8 域**
- 总规划域：**28 域**

---

## 下一阶段写作优先级

### 第一批：先补核心缺口

1. **N-8 PendingWriteQueue + CoalescingBufferQueue**
   - 理由：它是 Ch7 出站主线唯一明显断点，直接承接 Ch7-05/06/07。
2. **N-19 Cleaner / 直接内存释放**
   - 理由：它连接 Ch4-03、Ch4-07、Ch8 池化和真实 direct memory 排障。
3. **N-11 对象流编解码骨架深化**
   - 理由：现有 Ch10 已有素材，但 `MessageAggregator`、MessageToMessage、Headers/ValueConverter 还没有单独闭环。

### 第二批：运行时扩展

4. **N-24 超时/心跳 handler**
5. **N-25 流量整形/流控辅助/日志**
6. **N-26 大对象分块写出**

这三篇可以组成“连接治理与大对象传输”小组，并与已有 Timer、writability、flush、ChunkedInput 互相桥接。

### 第三批：平台专项

7. **N-27 epoll 原生传输**
8. **N-28 io_uring 原生传输**

平台专项最后写，原因是它们依赖 NIO/EventLoop/Channel/出站主线已经稳定，而且平台差异不应再次打断核心机制叙事。

### 暂不独立成篇

- **N-3**：先作为派生视图与 leak-aware 的深审补充，不立即开新大篇。
- **N-20**：先在 HTTP/2、HTTP/1、codec 和 FastThreadLocal 文章中建立索引；素材不足以优先于 N-8/N-19。
- **N-22**：等 N-24/N-25 调度场景更完整后，再决定是否写成 EventExecutor 专题。

---

## 本轮总 Review 结论

本轮新增内容已经把 Netty 旧规划中最严重的结构缺口补上，但不能因此把工作判断为全部完成。当前最合理的状态是：**核心主线已闭环，出站队列、Cleaner、handler 扩展和平台传输仍是明确缺口。**

下一篇优先写 **N-8 PendingWriteQueue + CoalescingBufferQueue**，它能最大程度延续当前读者理解路径，并直接验证前面关于 ownership、writability、flush 和 thread-local 复用的结论。

这次 Netty 规划暴露出一套不能复用到其他框架的旧方法：

### 1. 用“类名出现”替代“机制闭环完成"

旧规划把 `ResourceLeakDetector`、`ReferenceCounted`、`ChannelOutboundBuffer`、`FlushConsolidationHandler` 列进了某个域，就默认它们已经被覆盖。实际情况是：类名只是素材线索，不等于读者的问题已经被回答。

后续规划必须为每个候选域补齐：

- 读者真实困惑
- 入口与出口
- 状态/线程/所有权关系
- 失败路径
- 与其他域的调用关系
- 至少一个可独立成篇的理解闭环

### 2. 按包或目录切域，导致跨包机制被拆散

Netty 的关键机制经常横跨多个包：

- 泄漏检测：`common` + `buffer`
- 引用计数：`common` + `buffer` + `transport` + `codec`
- 背压：`transport` + `handler` + `codec-http2`
- ThreadLocal/对象池：`common/concurrent` + `common/internal` + `buffer` + `transport`

后续不能把目录边界直接当知识域边界，必须先画跨包调用图，再决定域划分。

### 3. 把“排障能力”当成实现细节

旧规划重视池化如何分配，却没有同等重视泄漏如何发现、日志如何解释、`touch()` 如何记录业务位置、测试如何开启高级检测。对于基础设施框架，运行时诊断能力本身就是核心能力，不能只放在备注里。

### 4. 把“资源生命周期”拆成零散 release 细节

`retain/release`、Promise 完成、Channel 关闭、Pipeline 移除、出站失败、聚合中断，其实共同组成资源所有权协议。后续应先建立“谁拥有资源、谁转移、谁兜底”的总图，再分别写 ByteBuf、Channel、HTTP 对象等实例。

### 5. 把出站路径误归为 EventLoop 的附属部分

EventLoop 只解释“谁运行”；出站缓冲、flush、水位线、PendingWriteQueue 解释“写入如何排队、何时可写、何时真正触发传输”。这是两个不同的问题，后续框架规划也要避免把调度线程和 I/O 缓冲混成一个域。

### 6. 把大模块标记为“按需”，但没有按调用中心性复核

旧规划把 `codec-http2` 标为按需，但源码图谱显示它与 codec-base、transport、handler 存在高密度调用和状态连接；HTTP/2 又是 gRPC、Dubbo Triple 的传输地基。模块大小或协议名称不能单独决定是否按需，必须结合调用中心性、上层复用度和跨域连接数判断。

### 7. 统计文件覆盖率，却没有统计机制覆盖率

“748 文件覆盖”只能说明扫过哪些文件，不能说明是否回答了：

- 内存泄漏怎么定位
- 出站何时背压
- flush 如何合并
- Stream 如何映射 child channel
- HPACK 错误为什么影响整条连接

后续规划应同时维护：模块覆盖、核心类覆盖、调用链覆盖、机制问题覆盖、失败路径覆盖。

### 8. 没有把测试目录当作设计证据

本次复核发现大量测试直接暴露机制边界：

- `ResourceLeakDetectorTest`
- `AbstractReferenceCountedTest`
- `ChannelOutboundBufferTest`
- `PendingWriteQueueTest`
- `ReentrantChannelTest`
- `Http2FrameCodecTest`
- `Http2MultiplexHandlerTest`

后续规划不能只扫 `src/main`；测试名、测试夹具和异常断言往往比类注释更能说明真正的设计问题。

### 9. 没有单独建立“排除理由”和“待复核边界"

本版继续排除 `handler/ssl`，但明确记录理由和边界；其他模块不再简单写“按需/淘汰”，而是区分：

- 当前主线
- 已探索但暂缓
- 平台特定
- 协议专项
- 构建/测试/工具

这样后续扩展时不会把历史判断误读成永久淘汰。

---

## 第三轮源码审计记录

### 审计方法

本轮不是只查关键词，而是按以下顺序交叉扫描：

1. 读取 `common`、`common/concurrent`、`common/internal`、`buffer`、`transport/channel`、`handler`、`codec-base`、`codec-http`、`codec-http2` 的完整主源码目录。
2. 用代码图谱查看包、调用边、继承边、装饰边和测试边，确认跨模块中心节点。
3. 对 `ResourceLeakDetector`、`ReferenceCounted`、`FastThreadLocal`、`ChannelOutboundBuffer`、`PendingWriteQueue`、`Http2FrameCodec`、`Http2MultiplexHandler` 等关键词做全库引用扫描。
4. 对 `retain/release/touch/leak/writability/flush/TODO/issue` 等关键词做实现与测试交叉扫描。
5. 把“类出现位置”和“调用/测试/资源路径”分开判断，避免再次把类名列表当成域覆盖。

### 本轮确认的跨模块中心

- **泄漏中心**：`AbstractByteBufAllocator.toLeakAwareBuffer()` -> `ResourceLeakDetector.track()` -> `Simple/AdvancedLeakAwareByteBuf` -> `touch/record/close`。
  - 证据：`buffer/src/main/java/io/netty/buffer/AbstractByteBufAllocator.java:40`
  - 证据：`common/src/main/java/io/netty/util/ResourceLeakDetector.java:253`
  - 证据：`buffer/src/main/java/io/netty/buffer/SimpleLeakAwareByteBuf.java:27`
- **所有权中心**：`AbstractReferenceCounted` / `AbstractReferenceCountedByteBuf` 通过 `RefCnt.release(...) -> deallocate()` 建立统一释放协议，`ReferenceCountUtil` 负责跨模块兜底调用。
  - 证据：`common/src/main/java/io/netty/util/AbstractReferenceCounted.java:57`
  - 证据：`buffer/src/main/java/io/netty/buffer/AbstractReferenceCountedByteBuf.java:82`
  - 证据：`common/src/main/java/io/netty/util/ReferenceCountUtil.java:88`
- **出站中心**：`ChannelOutboundBuffer` 同时连接 `ReferenceCountUtil`、`FastThreadLocal`、Promise、NIO write、high/low watermark 和关闭失败清理。
  - 证据：`transport/src/main/java/io/netty/channel/ChannelOutboundBuffer.java:67`
  - 证据：`transport/src/main/java/io/netty/channel/ChannelOutboundBuffer.java:114`
  - 证据：`transport/src/main/java/io/netty/channel/ChannelOutboundBuffer.java:185`
  - 证据：`transport/src/main/java/io/netty/channel/ChannelOutboundBuffer.java:437`
  - 证据：`transport/src/main/java/io/netty/channel/ChannelOutboundBuffer.java:720`
- **背压中心**：`WriteBufferWaterMark` 提供 high/low 语义，`PendingBytesTracker` 把 estimator 结果连接到 pipeline 或 outbound buffer，`PendingWriteQueue` 把“稍后写”的消息也纳入可写性计算。
  - 证据：`transport/src/main/java/io/netty/channel/WriteBufferWaterMark.java:21`
  - 证据：`transport/src/main/java/io/netty/channel/PendingBytesTracker.java:35`
  - 证据：`transport/src/main/java/io/netty/channel/PendingWriteQueue.java:30`
- **HTTP2 中心**：`Http2FrameCodec` 同时连接 connection handler、flow controller、frame object、reference counting 和 upgrade；`Http2MultiplexHandler` 再把 Stream 映射到 child channel，并把 writability 与流控挂钩。
  - 证据：`codec-http2/src/main/java/io/netty/handler/codec/http2/Http2FrameCodec.java:45`
  - 证据：`codec-http2/src/main/java/io/netty/handler/codec/http2/Http2FrameCodec.java:131`
  - 证据：`codec-http2/src/main/java/io/netty/handler/codec/http2/Http2MultiplexHandler.java:45`
  - 证据：`codec-http2/src/main/java/io/netty/handler/codec/http2/Http2MultiplexHandler.java:83`
- **线程本地/对象复用中心**：`InternalThreadLocalMap` 不只存 indexed variables，还存 `StringBuilder`、charset encoder/decoder cache、`TypeParameterMatcher` cache；`FastThreadLocal`、`Recycler`、`CodecOutputList` 都建立在这层复用体系之上。
  - 证据：`common/src/main/java/io/netty/util/concurrent/FastThreadLocal.java:29`
  - 证据：`common/src/main/java/io/netty/util/internal/InternalThreadLocalMap.java:66`
  - 证据：`common/src/main/java/io/netty/util/internal/InternalThreadLocalMap.java:213`
  - 证据：`common/src/main/java/io/netty/util/Recycler.java:39`
  - 证据：`codec-base/src/main/java/io/netty/handler/codec/CodecOutputList.java:38`

### 本轮新增的应关注主题

- **`ThreadLocal` 不只是 FastThreadLocal 一个类**：应同时研究 `InternalThreadLocalMap`、`FastThreadLocalThread`、`FastThreadLocalRunnable`、`ThreadExecutorMap`，以及 allocator、Recycler、ChannelOutboundBuffer、codec output list 如何使用它。
- **字符串/名称缓存不是普通工具细节**：`InternalThreadLocalMap` 实际保存 `StringBuilder`、`CharsetEncoder/Decoder`、`TypeParameterMatcher` cache，这说明“name / string / matcher cache”是一条真实存在的运行时线索，不是偶然实现细节。
- **接收侧自适应也是独立机制**：`AdaptiveRecvByteBufAllocator` 通过 `attemptedBytesRead == bytes` 触发增长，通过 `readComplete()` 记录总读取量；它与 `MaxMessagesRecvByteBufAllocator`、`MaxBytesRecvByteBufAllocator`、`MessageSizeEstimator` 一起决定读循环批次和内存压力，不能只在 EventLoop 中一句带过。
- **ByteBuf 派生视图 + leak-aware 包装需要单独讲**：`retainedSlice()`、`retainedDuplicate()`、`SimpleLeakAwareByteBuf.unwrappedDerived(...)` 显示“派生对象共享底层内存但延续泄漏跟踪”是一条单独机制线，而不是 `ByteBuf` 小节中的次要细节。
- **对象流 codec 不是字节流 codec 的附属品**：`MessageToMessageEncoder.write()` 会先 `encode`，再 `ReferenceCountUtil.release(cast)`，再借助 `CodecOutputList` 和 `PromiseCombiner` 批量写出；这意味着对象流编解码自带所有权转移与小对象复用语义。
- **协议对象到传输对象的桥也是独立机制**：`Http2FrameCodec`、`Http2MultiplexHandler`、`Http2StreamChannel`、`Http2StreamChannelBootstrap` 解释了 HTTP/2 如何落到 Netty Pipeline API。
- **失败清理是跨域主线**：`ChannelOutboundBuffer.close/failFlushed`、`PendingWriteQueue.removeAndFailAll`、HTTP2 flow controller cancel、codec handlerRemoved 都在实现“异常时释放对象 + 完成 Promise + 恢复状态”，应在后续正文中统一建立索引。

### 仍然明确排除

- `handler/ssl` 与 `handler-ssl-ocsp`：按当前任务约束不纳入主线。
- 纯平台特定、过时协议、构建工具、测试工具：继续保持按需或排除，但保留审计记录，不再把它们和“未扫描”混淆。
