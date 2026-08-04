# Netty 源码学习范围规划

> **基准**: Netty 4.2.15.Final
> **数据源**: Netty 仓库 2487 文件逐包扫描（common 200 + buffer 93 + transport 208 + handler 176 + codec 系列 + resolver 等）
> **边界**: 聚焦 Netty 核心（common/buffer/transport/handler/codec-base），协议编解码按需学习
> **my-xhs 关联**: Netty 是 Spring WebFlux/Gateway/RocketMQ/Dubbo 的底层传输

---

## 第 1 层：核心数据结构（1 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-1 | **ByteBuf 缓冲区** | 🔴 | buffer | AbstractByteBuf/ByteBuf/ByteBufAllocator/AbstractByteBufAllocator/CompositeByteBuf/FixedCompositeByteBuf/SlicedByteBuf/DuplicatedByteBuf/ReadOnlyByteBuf/UnreleasableByteBuf/WrappedByteBuf/ByteBufUtil/ByteBufInputStream/ByteBufOutputStream/SwappedByteBuf/Unpooled | ByteBuf 双指针（`readerIndex`/`writerIndex`，源码验证：`checkIndexBounds(0<=readerIndex<=writerIndex<=capacity)`）。vs JDK ByteBuffer 的区别（池化/组合/零拷贝/双指针）。`CompositeByteBuf`（`Component[] components`组合多个 ByteBuf 零拷贝）。`SlicedByteBuf`/`DuplicatedByteBuf`视图。`Unpooled`工具类。面试必问"ByteBuf vs ByteBuffer 区别" |

## 第 2 层：事件循环 + 通道（2 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-2 | **Channel + EventLoop 事件循环** | 🔴 | transport/channel + channel/nio + channel/socket + common/concurrent | NioEventLoop/NioEventLoopGroup/AbstractNioChannel/AbstractNioByteChannel/AbstractNioMessageChannel/NioServerSocketChannel/NioSocketChannel/NioIoHandler/IoHandler/IoHandlerContext/Channel/EventLoop/EventLoopGroup/SingleThreadEventExecutor/SingleThreadIoEventLoop/SingleThreadEventLoop/MultithreadEventExecutorGroup/EventExecutorChooserFactory/SelectedSelectionKeySet/ChannelOutboundBuffer/AdaptiveRecvByteBufAllocator/ChannelOption/ChannelConfig | `NioEventLoop extends SingleThreadIoEventLoop`。继承体系：`NioEventLoop`→`SingleThreadIoEventLoop`（`IoHandler ioHandler`）→`SingleThreadEventLoop`→`SingleThreadEventExecutor`→`MultithreadEventExecutorGroup`。**Netty 4.2 架构**：`IoHandler`接口核心方法`int run(IoHandlerContext context)`——IO 事件循环入口。`NioIoHandler implements IoHandler`（`Selector`+`SelectedSelectionKeySet`+`SELECTOR_AUTO_REBUILD_THRESHOLD=512`+`selectNowSupplier`）。**`ChannelOutboundBuffer`**（`addMessage`+`remove`+`totalPendingSize`高低水位线）。**`AdaptiveRecvByteBufAllocator`**（`64/2048/65536`+`guess()`自适应）。`NioSocketChannel.doReadBytes`/`doWrite`/`doWriteFileRegion`零拷贝。面试必问 |
| N-3 | **ChannelPipeline + ChannelHandler 处理器链** | 🔴 | transport/channel | DefaultChannelPipeline/AbstractChannelHandlerContext/ChannelHandler/ChannelInboundHandler/ChannelOutboundHandler/ChannelHandlerContext/ChannelHandlerMask/ChannelInboundHandlerAdapter/ChannelOutboundHandlerAdapter/ChannelDuplexHandler/ChannelInitializer/ChannelFuture/ChannelFutureListener | `DefaultChannelPipeline`（`HeadContext head`+`TailContext tail`双向链表+`addFirst`→`newContext`→`addFirst0`）。`fireChannelRead`传播到 next（不在同 EventLoop 线程则`executor().execute()`异步）。**`ChannelHandlerMask`**（源码验证：位掩码`MASK_CHANNEL_READ=1<<5`/`MASK_CHANNEL_ACTIVE=1<<3`等，快速判断处理器是否处理某事件避免不必要方法调用）。`ChannelInitializer`延迟初始化。入站 vs 出站。面试必问"处理器链怎么工作" |

## 第 3 层：启动 + 编解码（2 🔴）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-4 | **Bootstrap 启动流程 + 通道池/组** | 🔴 | transport/bootstrap + channel/pool + channel/group | AbstractBootstrap/ServerBootstrap/Bootstrap/AbstractBootstrapConfig/ServerBootstrapConfig/ChannelFactory/ChannelPool/SimpleChannelPool/FixedChannelPool/ChannelPoolMap/ChannelGroup/DefaultChannelGroup/ChannelMatcher | `AbstractBootstrap`（源码验证：`doBind()`先`initAndRegister()`初始化+注册，再绑定端口）。ServerBootstrap 服务端启动。**`ChannelPool`/`FixedChannelPool`**通道池（连接复用）。**`ChannelGroup`/`DefaultChannelGroup`**通道组（广播操作）。`ChannelFactory`创建 Channel。面试必问"Netty 服务端启动流程" |
| N-5 | **编解码器框架 + 拆包器** | 🔴 | codec-base | ByteToMessageDecoder/MessageToByteEncoder/ByteToMessageCodec/ReplayingDecoder/ReplayingDecoderByteBuf/DelimiterBasedFrameDecoder/LengthFieldBasedFrameDecoder/LineBasedFrameDecoder/FixedLengthFrameDecoder/LengthFieldPrepender/DefaultHeaders/Headers/ProtocolDetectionResult/MERGE_CUMULATOR | `ByteToMessageDecoder`（`MERGE_CUMULATOR`+`decodeState`+`callDecode()`）。**`ReplayingDecoder`**（重放解码器，数据不够时自动回滚，比 ByteToMessageDecoder 易用但性能稍差）。**4 种拆包器**：`DelimiterBasedFrameDecoder`（分隔符）/`LengthFieldBasedFrameDecoder`（长度字段）/`LineBasedFrameDecoder`（换行符）/`FixedLengthFrameDecoder`（固定长度）。`LengthFieldPrepender`编码时添加长度字段。`ProtocolDetectionResult`协议检测。serialization/string 子包（ObjectDecoder/StringEncoder）。面试必问"Netty 怎么处理粘包/半包" |

## 第 4 层：并发 + 内存管理（2 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-6 | **并发工具（Promise/Future + FastThreadLocal）** | 🟡 | common/concurrent | DefaultPromise/Promise/Future/AbstractFuture/EventExecutor/SingleThreadEventExecutor/DefaultEventExecutor/AbstractScheduledEventExecutor/DefaultEventExecutorGroup/FastThreadLocal/GlobalEventExecutor/PromiseAggregator/PromiseCombiner/ScheduledFutureTask | `DefaultPromise`（源码验证：`setSuccess0`/`setFailure0`/`notifyListeners`——`inEventLoop()`判断+`futureListenerStackDepth`栈深度控制防递归过深+`MAX_LISTENER_STACK_DEPTH`+`notifyListenersNow()`实际通知）。**`FastThreadLocal`**（数组索引代替哈希表）。`PromiseAggregator`/`PromiseCombiner` Promise 聚合。`ScheduledFutureTask` 定时任务 |
| N-7 | **内存管理 + 池化** | 🟡 | buffer + common/AbstractReferenceCounted + common/internal | PooledByteBufAllocator/PoolArena/PoolChunk/PoolChunkList/PoolSubpage/PoolThreadCache/SizeClasses/AdaptiveByteBufAllocator/AdaptivePoolingAllocator/ResourceLeakDetector/SimpleLeakAwareByteBuf/AdvancedLeakAwareByteBuf/AbstractReferenceCounted/RefCnt/UnpooledUnsafeNoCleanerDirectByteBuf/VarHandleByteBufferAccess/Cleaner/CleanerJava6/CleanerJava9/CleanerJava24Linker/CleanerJava25/InternalThreadLocalMap/InternalLoggerFactory | **传统池化**：jemalloc——`PoolArena`（源码验证：**6 级 PoolChunkList**`qInit`/`q000`/`q025`/`q050`/`q075`/`q100`按使用率分组管理 PoolChunk）+`PoolSubpage`/`SizeClasses`+`PoolThreadCache`线程缓存。**4.2 自适应池化**：`AdaptivePoolingAllocator`（Magazine+99-percentile+central queue+128KiB）。两级泄漏检测。`Cleaner`多版本。`InternalLoggerFactory`日志适配。`VarHandleByteBufferAccess`（JDK 9+）|
| N-8 | **HashedWheelTimer 时间轮** | 🟡 | common/HashedWheelTimer | HashedWheelTimer/Timer/TimerTask/HashedWheelBucket/Timeout | 时间轮定时器（源码验证：`workerState`状态机 0-init/1-started/2-shutdown，默认 tick 100ms）。用于大量定时任务（连接超时检测/心跳超时）。比 ScheduledThreadPoolExecutor 更高效（O(1) 添加任务）|

## 第 5 层：协议 + 扩展（3 🟡）

| # | 知识域 | 级别 | 核心包 | 核心类 | 核心问题 |
|---|---|---|---|---|---|
| N-9 | **HTTP 编解码** | 🟡 | codec-http（262 文件） | HttpRequestEncoder/HttpResponseDecoder/HttpObjectAggregator/HttpClientCodec/HttpServerCodec | HTTP 请求/响应编解码。HttpObjectAggregator 聚合 HTTP 消息 |
| N-10 | **handler 扩展（心跳/流量整形/分块/合并刷新）** | 🟡 | handler/timeout + handler/traffic + handler/stream + handler/flush + handler/logging + handler/flow | IdleStateHandler/ReadTimeoutHandler/WriteTimeoutHandler/AbstractTrafficShapingHandler/ChannelTrafficShapingHandler/GlobalTrafficShapingHandler/TrafficCounter/ChunkedFile/ChunkedInput/ChunkedNioFile/ChunkedStream/FlushConsolidationHandler/LoggingHandler/FlowControlHandler | **心跳检测**：`IdleStateHandler`（readerIdleTime/writerIdleTime/allIdleTime）+`IdleStateEvent`。**流量整形**：`AbstractTrafficShapingHandler`（带宽限制）+`TrafficCounter`（流量统计）+Channel/Global/GlobalChannel 三级。**分块写入**：`ChunkedFile`/`ChunkedInput`（大文件分块传输）。**合并刷新**：`FlushConsolidationHandler`（减少 flush 次数提升性能）。**日志**：`LoggingHandler`（调试）。**流控**：`FlowControlHandler` |
| N-11 | **epoll/io_uring 原生传输** | 🟡 | transport-classes-epoll/transport-classes-io_uring | EpollEventLoop/EpollSocketChannel/IOUringEventLoop | 原生 epoll/io_uring 支持（Linux）。比 NIO 性能更好（zero-copy/边缘触发） |

---

## 淘汰清单（45 个子模块全覆盖审计）

### 已探索（7 个）
| 子模块 | 文件数 | 域 |
|---|---|---|
| common | 200 | N-6/N-7/N-8 |
| buffer | 93 | N-1/N-7 |
| transport | 208 | N-2/N-3/N-4 |
| handler | 176 | N-10 |
| codec-base | 71 | N-5 |
| resolver | 20 | N-4（合并） |
| resolver-dns | 61 | N-4（合并） |

### 规划提到未深入（3 个）
| 子模块 | 文件数 | 域 | 说明 |
|---|---|---|---|
| codec-http | 262 | N-9 | HTTP 编解码，已提 |
| transport-classes-epoll | 38 | N-11 | Linux epoll |
| transport-classes-io_uring | 47 | N-11 | Linux io_uring |

### 确认淘汰（35 个）
| 子模块 | 文件数 | 理由 |
|---|---|---|
| transport-classes-kqueue | 33 | macOS/BSD 原生传输（KQueueChannel），按需 |
| transport-native-unix-common | 31 | Unix Domain Socket（DomainDatagramChannel），按需 |
| transport-sctp | 37 | SCTP 协议传输，非主流 |
| transport-udt | 18 | UDT 协议传输，非主流 |
| transport-rxtx | 6 | 串口通信（RxtxChannel），非主流 |
| handler-proxy | 7 | 代理处理器（HttpProxyHandler/SocksProxyHandler），按需 |
| handler-ssl-ocsp | 8 | OCSP 证书状态，按需 |
| codec-compression | 61 | 压缩编解码（Brotli/Zlib），按需 |
| codec-socks | 85 | SOCKS 代理编解码，按需 |
| codec-classes-quic | 106 | QUIC 协议编解码，新特性按需 |
| codec-http2 | 129 | HTTP/2 编解码，按需 |
| codec-http3 | 78 | HTTP/3 编解码，按需 |
| codec-dns | 41 | DNS 编解码，按需 |
| codec-memcache | 34 | Memcache 编解码，按需 |
| codec-mqtt | 39 | MQTT 编解码，按需 |
| codec-protobuf | 7 | Protobuf 编解码，按需 |
| codec-redis | 25 | Redis 编解码，按需 |
| codec-smtp | 14 | SMTP 编解码，按需 |
| codec-stomp | 17 | STOMP 编解码，按需 |
| codec-xml | 18 | XML 编解码，按需 |
| codec-marshalling | 15 | JBoss Marshalling，过时 |
| codec-haproxy | 11 | HAProxy 协议，低频 |
| jfr-stub | 16 | JFR stub（JDK jfr 包存根），构建工具 |
| pkitesting | 8 | PKI 测试工具（CertificateBuilder），测试 |
| varhandle-stub | 3 | VarHandle stub（JDK invoke 包存根），构建工具 |
| resolver-dns-classes-macos | 3 | macOS DNS 解析，平台特定 |
| example | 208 | 示例代码 |
| microbench | 154 | 微基准测试 |
| testsuite | 65 | 测试套件 |
| testsuite-autobahn | 4 | Autobahn 测试 |
| testsuite-common | 3 | 测试公共 |
| testsuite-http2 | 6 | HTTP/2 测试 |
| testsuite-jpms | 7 | JPMS 测试 |
| testsuite-native-image | 5 | Native Image 测试 |
| testsuite-native-image-client | 2 | Native Image 客户端测试 |
| testsuite-native-image-client-runtime-init | 2 | Native Image 客户端运行时初始化测试 |
| all/bom/dev-tools | — | 构建/BOM/工具 |

---

## 统计

| | 数量 |
|---|---|
| 🔴 核心域 | **5** |
| 🟡 重要域 | **7** |
| 总计 | **12 域** |
| 预计产出文章 | 5 篇（🟡 子域在对应 🔴 中附带）|
| 核心子模块覆盖 | common(200) + buffer(93) + transport(208) + handler(176) + codec-base(71) = 748 文件 |

## 与 Spring 生态的关联

| Spring 域 | Netty 关联 | 关系 |
|---|---|---|
| Boot B-20 WebFlux | N-2/N-3 EventLoop+Pipeline | WebFlux 底层用 Netty EventLoop + Pipeline |
| Boot B-7 嵌入式容器 | N-4 Bootstrap | Boot 嵌入式 Netty 用 Netty Bootstrap |
| Cloud G-4 NettyRoutingFilter | N-1 ByteBuf + N-2 Channel | Gateway 用 Netty 转发请求 |
| Cloud B-20 WebFlux+Netty | N-2 EventLoop | WebFlux 默认服务器是 Netty |
