# D-8b HTTP 传输栈 http12 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | HTTP 面分散: dubbo-remoting-http (Jetty/Tomcat 嵌入) + rest 协议 (JAX-RS 适配) — 无统一 HTTP 传输栈 |
| 3.0 | **dubbo-remoting-http12 新建**: HttpChannel 统一抽象 + h1/h2 双栈 (netty4) + mediaType 驱动 codec + REST 元数据 (Mapping/OpenAPI) — triple 协议基座 |
| 3.x | REST_ENABLED 并入 triple (H2_SETTINGS_REST_ENABLED) — 旧 rest 模块退役; JsonPbCodec (json+protobuf) |
| 3.3.x | http12 126 文件稳定 (triple→http12 106 import) |

## 痕迹证据

- HttpChannel.java: writeHeader/writeMessage 接口 (3.0 锚, 类名/包实证)
- CodecUtils.java:49-101: disallowedContentTypes 过滤 (3.x 锚)
- NettyHttp2ProtocolSelectorHandler.java:42: SimpleChannelInboundHandler<HttpMetadata> — 协议选择 (3.x 锚)
- HttpWriteQueueHandler.java:25: 写队列背压 (3.x 锚)
- Mapping.java:41-60: @Mapping 注解 (3.x 锚)
- TripleProtocol.java:73,92: REST_ENABLED = H2_SETTINGS_REST_ENABLED (3.x 锚, D-9 呼应)

## 推断标注

- "2.x 无统一栈" — dubbo-remoting-http 存在性推断 (标注)
- "3.0 新建" — 模块结构/类名实证 (实证)
- "3.x jsonpb" — JsonPbCodecFactory 类名实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- Netty (N-9 HTTP 编解码): HttpServerCodec vs http12 NettyHttp1Codec — HTTP 编解码对照
- Tomcat (T-2 Connector): HTTP 连接器 vs Dubbo http12 传输栈 — 服务端对照
- gRPC (G-2): HTTP/2 服务端 vs triple/http12 — 双栈对照
