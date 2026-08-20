# D-8b HTTP 传输栈 http12 — 通道抽象与双栈

> 项目: Dubbo | 🟡 Deep / 1 篇 | HttpChannel+HttpMetadata+h1/h2 消息模型+CodecUtils+NettyHttp2ProtocolSelectorHandler+Mapping
> 基线: DUBBO-PLAN D-8b (网络层, 126 文件) — 前置: **D-8a (传输抽象)** — 展开 通道→编解码→双栈→REST 面

---

## §0.8

- 🟡 Deep，1篇 — 通道抽象(**HttpChannel: writeHeader/writeMessage 异步[CompletableFuture]/newOutputMessage/flush; HttpMetadata L21-34[headers/contentType]; h1 完整消息 vs h2 流式帧[H2StreamChannel/Http2InputMessageFrame/CancelableTransportListener]**) → 编解码(**CodecUtils.determineHttpMessageDecoder(mediaType) L40-60: content-type 驱动选 codec+UnsupportedMediaTypeException+disallowedContentTypes 禁用列表 L49-101; CodecFactory 族: Binary/Json/Html/JsonPb**) → 双栈(**netty4/h1: NettyHttp1Codec+ConnectionHandler[SimpleChannelInboundHandler<Http1Request> L29]+NettyHttp1Channel; netty4/h2: NettyHttp2FrameCodec/FrameHandler+**ProtocolSelectorHandler 首帧判定 L42**+NettyH2StreamChannel; HttpWriteQueueHandler L25+写命令队列[HttpWriteQueue extends BatchExecutorQueue L24[enqueue/prepare/flush 批量合并]+DataQueue/HeaderQueue/ResetQueueCommand]; FlowControlStreamObserver request(count) gRPC 式流控 L31-38**) → REST 面(**Mapping 注解 L41-60+Operation/Param/Schema+OpenAPI 族 — triple REST_ENABLED 基座[mappingRegistry 消费]**)
- 设计模式: [模式: 协议无关抽象+双栈+mediaType 驱动]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HttpChannel.java | 抽象 | **writeHeader/writeMessage 异步 + flush** — 协议无关 | High |
| CodecUtils.java:40-60 | 编解码 | **mediaType 驱动选 codec** + UnsupportedMediaTypeException | High |
| CodecUtils.java:49-101 | 禁用 | **disallowedContentTypes 过滤** — 安全面 | High |
| NettyHttp2ProtocolSelectorHandler.java:42 | 双栈 | **首帧判定选 h1/h2** — 同端口双协议 | High |
| HttpWriteQueue.java:24 | 写队列 | **extends BatchExecutorQueue: enqueue/prepare/flush 批量合并写** | High |
| FlowControlStreamObserver.java:31-38 | 流控 | **gRPC 式 request(count) 手动流控** — 背压式 | High |
| Mapping.java:41-60 | REST | **@Mapping path/value — 路径→处理器映射** | High |
| AbstractServerHttpChannelObserver.java:35 | 流式 | **服务端流式观察者 onError/onCompleted** — D-9 基座 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: http12 单机制 (抽象+编解码+双栈) — 1篇按四段展开; triple 构建其上 (106 import 导航 D-9), 序列化在 D-10 (导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | HttpChannel 协议无关抽象 | 🔴 | **为什么🔴**: 上层无感 |
| P1-2 | mediaType 驱动 codec | 🔴 | **为什么🔴**: HTTP 原生语义 |
| P1-3 | 双栈协议选择 (首帧判定) | 🔴 | **为什么🔴**: 同端口 |
| P1-4 | 写命令队列批量合并 | 🔴 | **为什么🔴**: 背压核心 |
| P2-1 | gRPC 式流控 request(count) | 🟡 | **为什么🟡**: 流式面 |
| P2-2 | REST 元数据 (Mapping/OpenAPI) | 🟡 | **为什么🟡**: D-9 基座 |
| P3-1 | CompositeInputStream 分帧组合 | 🟢 | **为什么🟢**: 读路径细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **通道抽象** | 🔴 | 主线 |
| B | **编解码面** | 🔴 | 消息处理 |
| C | **双栈适配** | 🔴 | 传输面 |
| D | **REST/流式面** | 🟡 | 应用面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 通道抽象 | HttpChannel 统一出口 — h1 完整消息 vs h2 流式帧差异被隔离; 上层 (triple/rest) 无感 | HttpChannel.java; h1/; h2/ |
| q2 | 编解码 | **content-type 驱动选 codec (Binary/Json/Html/JsonPb)**; 禁用列表安全过滤; UnsupportedMediaTypeException 明确报错 | CodecUtils.java:40-101 |
| q3 | 双栈 | ProtocolSelectorHandler 首帧判定同端口 h1/h2; **写路径 = 命令化 + BatchExecutorQueue 批量合并 (减 syscall) + request(count) 背压流控**; 读路径 CompositeInputStream 分帧组合 | NettyHttp2ProtocolSelectorHandler.java:42; HttpWriteQueue.java:24; FlowControlStreamObserver.java:31-38 |
| q4 | REST 面 | @Mapping 注解声明式路径映射 + OpenAPI 自描述 — **triple REST_ENABLED 的基座 (mappingRegistry)** | Mapping.java:41-60 |

→ 引出 D-9 Triple 协议 (106 import 基座); D-10 序列化 (BinaryCodec/JsonPbCodec HTTP 层适配)。
