# D-9 Triple 协议 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 协议面: dubbo 协议 (dubbo-rpc-dubbo, 自定义二进制) + injvm; 无 HTTP/2 协议 |
| 3.0 | **Triple 协议新建**: HTTP/2 + protobuf + 三模式调用; GrpcHttp2Protocol (gRPC 兼容); http12 传输栈基座 |
| 3.x | REST_ENABLED 并入 (H2_SETTINGS_REST_ENABLED); TripleHttp2Protocol (WINDOW_UPDATE 接管); ThreadlessExecutor 同步化; 流控控制器 |
| 3.3.x | triple 228 文件稳定 (triple→remoting 121 + triple→http12 106 import) |

## 痕迹证据

- TripleProtocol.java:106-156/193-204: export/refer 双端 (3.0 锚)
- TripleProtocol.java:73,92: REST_ENABLED = H2_SETTINGS_REST_ENABLED (3.x 锚)
- TripleInvoker.java:172: isSync ? ThreadlessExecutor : streamExecutor (3.x 锚)
- ThreadlessExecutor.java:37-38: "Tasks are stored in a blocking queue and will only be executed when a thread calls waitAndDrain()" (注释锚)
- TripleHttp2Protocol.java:221: "This prevents Netty from automatically sending WINDOW_UPDATE frames" (注释锚 — 流控接管)
- TriplePingPongHandler.java:28-35: PING 处理 (3.x 锚)
- GrpcHttp2Protocol.java:22: extends TripleHttp2Protocol (3.0 锚)
- PbUnpack.java:25-41: SingleProtobufUtils.deserialize (3.0 锚)

## 推断标注

- "2.x 无 HTTP/2" — dubbo 协议为 2.x 默认 (公知线标注)
- "3.0 Triple 新建" — 类结构/SPI 实证 (实证)
- "3.x REST_ENABLED" — 常量实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- gRPC (G-1 ProtoBuf 序列化/G-2 服务端/G-3 客户端): 双实现对照 — HTTP/2 RPC
- HTTP/2 协议规范: 帧/流/多路复用 vs TripleHttp2Protocol
- Netty (N-9): HTTP 编解码对照
