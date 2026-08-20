# D-9 Triple 协议 — 装配→调用模式→传输流控→gRPC/REST 面

> 前置: [[D-8a-传输抽象]] [[D-8b-HTTP传输栈]] | 引出: [[D-10-序列化]] | 对照: gRPC (G-1~G-3) + HTTP/2 认知
> 🟡 A | 8 KP | [模式: HTTP/2 应用协议 + 三模式调用 + 双协议面]
> Pass 2 闭环: q1(装配面) q2(调用模式) q3(传输流控) q4(gRPC/protobuf/REST)

**读者处境**: 3.x 默认协议怎么在 HTTP/2 上实现 RPC? 三种调用模式? 为什么 grpc 客户端能调? 这篇拆 TripleProtocol + TripleInvoker + transport + GrpcHttp2Protocol。

### 1. 装配面 — TripleProtocol export/refer

场景: Triple 怎么装配?
源码路径:
- **export** (L106-156): exporterMap + **pathResolver.register** (L135, gRPC 风格路径) + **REST_ENABLED → mappingRegistry** (L138) + bindServerPort + optimizeSerialization
- **refer** (L193-204): streamExecutor → **PortUnificationExchanger.connect (HTTP/2, D-8b 基座)** → TripleInvoker
- pathResolver = PathResolver SPI (L64/79) — 服务寻址注册表
关键设计 (q1): **HTTP/2 多路复用 + gRPC 路径寻址 + RPC/REST 双协议一实例**。[模式: 装配面]

### 2. 调用模式面 — unary/流式 + ThreadlessExecutor

场景: 三种调用模式怎么实现? 同步不占线程?
源码路径:
- **doInvoke** (L144-200): **isSync ? new ThreadlessExecutor() : streamExecutor** (L172) → **invokeUnary (L305) / invokeServerStream / invokeBiOrClientStream** 三模式
- **ThreadlessExecutor** (common/threadpool, L37-56): 注释 L37-38 任务排队 + **waitAndDrain 调用线程自执行** — 零额外线程同步
- ⚠ **DeadlineFuture extends CompletableFuture** (DeadlineFuture.java:38): timeout + **timeoutListeners 回调列表** (L46) + **HashedWheelTimer 30ms tick** (L50) — gRPC deadline 机制
- 返回 AsyncRpcResult (D-4 一致)
关键设计 (q2): **三模式一协议 (gRPC 调用模型) + ThreadlessExecutor 零线程同步**。[模式: 调用面]

### 3. 传输与流控面 — transport/ + PING + GOAWAY

场景: HTTP/2 传输事件/保活/停机?
源码路径:
- **流控**: TripleHttp2LocalFlowController/RemoteFlowController + **WINDOW_UPDATE 接管** (TripleHttp2Protocol:221 — 防 netty 自动发)
- **TripleGoAwayHandler** — GOAWAY 协商关闭; **GracefulShutdown** — 在途请求处理完再关
- **TriplePingPongHandler** (L28: HTTP/2 PING + pingAckTimeout); TripleCommandOutBoundHandler (出站命令, D-8b 呼应)
关键设计 (q3): **自定义流控接管 + GOAWAY 无损停机 + HTTP/2 层 PING**。[模式: 传输面]

### 4. gRPC 兼容 + protobuf + REST 面

场景: 为什么 grpc 客户端能调? protobuf/REST 怎么共存?
源码路径:
- **GrpcHttp2Protocol extends TripleHttp2Protocol** (L22, 标记类) — gRPC 协议变体 (路径/帧/状态兼容)
- ⚠ **gRPC 内置服务** (service/ 5 文件): **TriHealthImpl (健康检查)** + **ReflectionV1AlphaService (gRPC 反射)** + SchemaDescriptorRegistry + TriBuiltinService — gRPC 生态服务面
- ⚠ **压缩面** (compressor/ 7 文件): Compressor/DeCompressor + **Gzip/Bzip2/Identity** + MessageEncoding — 消息压缩 (header 协商)
- ⚠ **h12 服务端面** (h12/ 40 文件): AbstractServerTransportListener + BiStreamServerCallListener + **CompressibleEncoder** + DefaultHttpMessageListener — 服务端调用/传输监听
- **protobuf 面**: PbUnpack (L25-41: **SingleProtobufUtils.deserialize** — writeTo/parseFrom + GLOBAL_REGISTRY 动态消息 L96-132) + PackableMethodFactory 族 + ReflectionPackableMethod — 序列化抽象 (D-10 深入)
- **RestProtocol extends TripleProtocol** (L21) — REST 变体 + mappingRegistry (D-8b Mapping)
关键设计 (q4): **gRPC 生态互通 + protobuf 打包抽象 + 多协议一端口**。[模式: 兼容面]

## 代码类型
Architecture (协议) + Concurrency (流式/同步化)

## 负面空间 (D-9, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不协议降级 | h2 必须, 无 h1 降级 (q1) |
| 不路径通配注册 | 精确路径 (q1) |
| 不客户端流取消恢复 | 取消即断 (q2) |
| 不消息级背压协商 | 固定流控窗口 (q2) |
| 不连接级负载均衡 | h2 复用, 节点选择在 D-6 (q3) |
| 不 gRPC 全特性 | 反射/健康检查部分支持 (q4) |

## 结尾桥 OUTBOUND

- → [[D-10-序列化]]: PbUnpack/PackableMethod — protobuf 序列化深潜
- → 对照: gRPC (G-1 ProtoBuf/G-2 服务端/G-3 客户端) — HTTP/2 RPC 双实现对照
- → 阶段 5.3 gRPC: 生态互通收尾
