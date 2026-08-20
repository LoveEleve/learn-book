# D-8b HTTP 传输栈 http12 — Pass 2 闭环 Q3: netty4 适配面 (h1/h2 双栈)

> 核心: netty4/h1 + netty4/h2 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: netty4 上怎么跑 h1/h2 双栈? 协议怎么选择? 写队列怎么背压?**

## 机制链 (已实证)

```
netty4/h1 (4 文件):
├── NettyHttp1Codec — HTTP/1.1 编解码 (netty HttpServerCodec 适配)
├── NettyHttp1ConnectionHandler — SimpleChannelInboundHandler<Http1Request> (L29)
│   → 完整消息处理 → HttpChannel 抽象
└── NettyHttp1Channel — HttpChannel 的 h1 实现

netty4/h2 (5 文件):
├── NettyHttp2FrameCodec / NettyHttp2FrameHandler — HTTP/2 帧编解码/处理
├── **NettyHttp2ProtocolSelectorHandler** — SimpleChannelInboundHandler<HttpMetadata> (L42)
│   ← h1/h2 协议选择 (连接首帧判定 + Http2Connection/Http2StreamChannel)
├── NettyHttp2SettingsHandler — HTTP/2 设置帧
└── NettyH2StreamChannel — HttpChannel 的 h2 实现 (流式)

写队列 (netty4/HttpWriteQueueHandler.java:25 + command/):
├── ChannelInboundHandlerAdapter — 写队列管理 (背压/合并)
├── ⚠ HttpWriteQueue **extends BatchExecutorQueue<HttpChannelQueueCommand>** (command/HttpWriteQueue.java:24):
│   ├── enqueue (L32) — 命令入队
│   ├── prepare (L38) — 批量准备
│   └── flush (L43) — 批量冲刷执行 (写合并, 减少 syscall)
└── ⚠ 流控: FlowControlStreamObserver — **gRPC 式 request(count) 手动流量控制** (L31-38:
    "StreamObserver#onNext unless it is request()ed" — 背压式流控, D-9 流式面)
```

## 关键设计 (why)

1. **双栈共存**: 同一端口 h1/h2 — ProtocolSelectorHandler 首帧判定 (多协议端口面, 呼应 PortUnification)
2. **帧 vs 消息**: h2 帧处理 (FrameCodec/FrameHandler) vs h1 消息处理 (ConnectionHandler) — 底层差异隔离
3. **HttpChannel 统一出口**: NettyHttp1Channel / NettyH2StreamChannel 都实现 HttpChannel — 上层无感
4. **写队列背压**: HttpWriteQueueHandler — 高水位写合并 (流式响应不压垮 netty)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| NettyHttp1ConnectionHandler | netty4/h1/NettyHttp1ConnectionHandler.java:29 |
| NettyHttp1Codec | netty4/h1/NettyHttp1Codec.java |
| NettyHttp2ProtocolSelectorHandler (协议选择) | netty4/h2/NettyHttp2ProtocolSelectorHandler.java:42 |
| NettyHttp2FrameCodec/FrameHandler | netty4/h2/ |
| HttpWriteQueueHandler (背压) | netty4/HttpWriteQueueHandler.java:25 |

## 负面空间 (Q3 面)

- 不做 ALPN 协商 (固定配置, 非 TLS ALPN)
- 不做 h2 优先级树 (netty 默认)
- 不做连接迁移 (HTTP/2 GOAWAY 面留给上层)
