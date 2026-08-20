# D-9 Triple 协议 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-rpc-triple 228 文件)
> 09 域级审计: 执行计划未覆盖 — PLAN 顶层包扫描抓出 (228 文件 ≥50 已过设计决策测试); 基座: triple→remoting 121 + triple→http12 106 import

## 入口展开 (Level-1~3, 已读源码)

### Level-1: TripleProtocol export/refer (L62-233)

```
TripleProtocol (protocol/tri/TripleProtocol.java, 233 行):
├── export (L106-156, D-2 已见): exporterMap + pathResolver.register + REST_ENABLED→mappingRegistry + executor + bindServerPort + optimizeSerialization
├── refer (L193-204): optimizeSerialization → streamExecutor (ExecutorRepository) →
│   **Http3Exchanger.isEnabled ? Http3.connect : PortUnificationExchanger.connect (HTTP/2, D-8b 基座!)**
│   → TripleInvoker (type, url, acceptEncodings, connectionClient, ...)
└── pathResolver (PathResolver SPI, L64/79/135/116): gRPC 路径注册/注销
```

### Level-2: TripleInvoker + 流式面

```
TripleInvoker.doInvoke (L144+):
├── 连接不可用检查 (L148)
├── stream 创建 → **isSync(methodDescriptor, invocation) ? new ThreadlessExecutor() : streamExecutor** (L172)
│   ← 同步调用用 ThreadlessExecutor 等待 (非阻塞 IO 线程)
└── 流式 API: StreamObserver (common/stream) + ClientStreamObserver + CancelableStreamObserver

stream/ 面: Stream 接口 + AbstractStream + AbstractTripleClientStream + ClientStreamFactory
transport/ 面:
├── H2TransportListener + AbstractH2TransportListener — HTTP/2 传输事件
├── TripleHttp2LocalFlowController/RemoteFlowController — 流控 (呼应 D-8b FlowControl)
├── TripleGoAwayHandler — HTTP/2 GOAWAY 处理 (优雅停机)
├── TripleCommandOutBoundHandler — 出站命令 (呼应 D-8b 写命令队列)
└── GracefulShutdown — 优雅停机
```

### Level-3: protobuf + gRPC 兼容 + REST

```
protobuf 面:
├── PbUnpack (L25-41): **SingleProtobufUtils.deserialize** — protobuf 反序列化 (D-10 关联)
├── PbArrayPacker / PackableMethodFactory / DefaultPackableMethodFactory — 方法打包抽象
└── DescriptorUtils — protobuf Descriptor 工具 (gRPC 反射)

gRPC 兼容面:
├── GrpcHttp2Protocol extends TripleHttp2Protocol (L22) — gRPC 协议变体
├── TriplePingPongHandler (L28): HTTP/2 PING 处理 (pingAckTimeout)
└── pathResolver: gRPC 风格路径 (/package.Service/Method)

REST 面 (REST_ENABLED, D-8b 基座):
└── RestProtocol extends TripleProtocol (L21) + rest/ 子包 + mappingRegistry (D-8b Mapping 注解消费)
```

## 09 域级审计表 (执行计划未覆盖 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| 执行计划未覆盖 (顶层扫描抓出) | triple 228 文件: protocol/tri 13 子包 + rpc.stub | 接受 (PLAN 已记录, D-9) |
| 基座实证 | PortUnificationExchanger.connect (HTTP/2, D-8b) + 流控/命令 (呼应 D-8b) | 接受 |
| 执行计划未提: gRPC 兼容 | GrpcHttp2Protocol + pathResolver + TriplePingPongHandler | 补锚 |
| 执行计划未提: 同步调用 ThreadlessExecutor | TripleInvoker.doInvoke L172 | 补锚 |
| 执行计划未提: protobuf 面 | PbUnpack/SingleProtobufUtils/PackableMethodFactory | 补锚 (D-10 关联) |
| 执行计划未提: 优雅停机 | TripleGoAwayHandler + GracefulShutdown | 补锚 |

## 待展开 (下一层)

1. TripleInvoker.doInvoke 完整 (stream 请求/响应绑定)
2. ThreadlessExecutor 同步等待机制
3. SingleProtobufUtils 序列化细节
4. TripleHttp2Protocol (协议配置: 帧/头)
5. stub 面 (rpc/stub — StubInvocationUtil, 3.x native stub)
