# D-8b HTTP 传输栈 http12 — 通道抽象→编解码→双栈→REST 面

> 前置: [[D-8a-传输抽象]] | 引出: [[D-9-Triple协议]] [[D-10-序列化]] | 对照: Netty HTTP codec (N-9) + Tomcat Connector (T-2)
> 🟡 B | 8 KP | [模式: 协议无关抽象 + 双栈 + mediaType 驱动]
> Pass 2 闭环: q1(通道抽象) q2(编解码) q3(netty4 双栈) q4(REST 元数据)

**读者处境**: HTTP/1.1 和 HTTP/2 怎么共存一个端口? 消息体怎么按 content-type 编解码? triple 的 REST 面基座是什么? 这篇拆 http12 126 文件。

### 1. 通道抽象面 — HttpChannel + h1/h2 消息模型

场景: HTTP 传输抽象出什么?
源码路径:
- **HttpChannel 接口**: writeHeader/writeMessage (异步 CompletableFuture) / newOutputMessage / flush — 协议无关
- **HttpMetadata** (L21-34): headers/contentType — 消息元数据
- **h1 vs h2 消息模型**: h1/ (完整消息: Http1Request/Http1Metadata/Default*) vs h2/ (流式帧: H2StreamChannel/Http2InputMessageFrame/CancelableTransportListener)
- HttpChannelHolder.getHttpChannel (L21)
关键设计 (q1): **协议无关通道 (上层无感 h1/h2) + h1 完整消息 vs h2 流式帧**。[模式: 抽象面]

### 2. 编解码面 — mediaType 驱动 codec 族

场景: 消息体怎么编解码?
源码路径:
- **CodecUtils.determineHttpMessageDecoder/Encoder(mediaType)** (L40-60): **content-type 驱动选 codec** → UnsupportedMediaTypeException
- **disallowedContentTypes 禁用过滤** (L49-101)
- **CodecFactory 族**: BinaryCodec (二进制) / JsonCodec / HtmlCodec / **JsonPbCodecFactory** (JSON+Protobuf, D-9 关联)
关键设计 (q2): **mediaType 驱动 (HTTP 原生 content negotiation) + factory 可插拔 + 与 D-10 序列化呼应**。[模式: 编解码面]

### 3. netty4 适配面 — h1/h2 双栈

场景: netty4 怎么跑双协议?
源码路径:
- **h1**: NettyHttp1Codec + NettyHttp1ConnectionHandler (SimpleChannelInboundHandler<Http1Request>, L29) + NettyHttp1Channel
- **h2**: NettyHttp2FrameCodec/FrameHandler + **NettyHttp2ProtocolSelectorHandler** (L42: 首帧判定选协议) + NettyH2StreamChannel (流式) + **HttpTransportListener** (onMetadata/onData — h2 传输事件)
- **写队列背压**: HttpWriteQueueHandler (L25) + ⚠ **命令模型 + 批量执行**: HttpWriteQueue **extends BatchExecutorQueue** (command/HttpWriteQueue.java:24, enqueue/prepare/flush L32-43 — **写批量合并执行**) + DataQueueCommand/HeaderQueueCommand/**ResetQueueCommand** (h2 流重置) + Http2WriteQueueChannel
- **流控面**: ⚠ **FlowControlStreamObserver** — **gRPC 式 request(count) 手动流量控制** (L31-38 注释 "onNext unless request()ed" — 背压流控, D-9 流式面); CompositeInputStream (L26: addInputStream 分帧 body 组合); 异常族 8 类 (Decode/Encode/HttpOverPayload/HttpRequestTimeout/UnsupportedMediaType 等)
- **服务端流式观察者**: AbstractServerHttpChannelObserver (L35: onError/onCompleted) — D-9 流式面基座; HttpRequest extends **RequestMetadata** (method/path, L27-35)
关键设计 (q3): **同端口双栈 (选择器首帧判定) + 帧/消息双处理 + 统一 HttpChannel 出口 + 写背压**。[模式: 双栈面]

### 4. REST 元数据面 — Mapping/OpenAPI

场景: REST 面怎么描述服务?
源码路径:
- **Mapping 注解** (rest/Mapping.java:41-60): path/value — 路径→处理器映射
- Operation/Param/ParamType/Schema — 操作/参数模型
- OpenAPI 族 — 文档生成
- **D-9 消费**: TripleProtocol REST_ENABLED → mappingRegistry (triple 的 REST 面基座)
关键设计 (q4): **注解驱动静态映射 + OpenAPI 自描述 + triple REST 双协议基座**。[模式: REST 面]

## 代码类型
Architecture (双栈抽象) + Concurrency (流式/背压)

## 负面空间 (D-8b, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不消息缓存 | h2 流式直接处理 (q1) |
| 不 content negotiation | 客户端显式 content-type (q2) |
| 不 codec 缓存 | 每次 createCodec (q2) |
| 不 ALPN 协商 | 固定配置, 非 TLS ALPN (q3) |
| 不动态路由注册 | Mapping 注解静态声明 (q4) |
| 不 REST 鉴权 | 认证在 D-4 Filter 面 (q4) |

## 结尾桥 OUTBOUND

- → [[D-9-Triple协议]]: triple 构建于 http12 之上 (106 import) — HTTP/2 应用协议
- → [[D-10-序列化]]: BinaryCodec/JsonPbCodec — 序列化 HTTP 层适配
- → 对照: Netty (N-9 HTTP 编解码) / Tomcat (T-2 Connector)
