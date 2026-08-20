# D-8b HTTP 传输栈 http12 — Pass 2 闭环 Q1: HTTP 通道抽象面

> 核心: HttpChannel + HttpMetadata + h1/h2 消息模型 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: HTTP 通道抽象出什么? HTTP/1.1 与 HTTP/2 消息模型差异?**

## 机制链 (已实证)

```
HttpChannel 接口 (http12/HttpChannel.java) — 协议无关通道抽象:
├── writeHeader(HttpMetadata) / writeMessage(HttpOutputMessage) — 异步写 (CompletableFuture)
├── newOutputMessage() / remoteAddress() / localAddress() / flush()
HttpMetadata (HttpMetadata.java:21-34): headers()/contentType()/header(name) — 消息元数据

消息模型 (h1 vs h2):
├── h1/ (10 文件): Http1Request/Http1Response/Http1Metadata/Http1InputMessage/Http1OutputMessage
│   + DefaultHttp1Request/Response — 完整消息 (一行式 head + body)
├── h2/ (17 文件): H2StreamChannel/Http2Header/Http2InputMessageFrame/CancelableTransportListener
│   + Http2ChannelDelegate — 流式帧模型 (stream-based)
└── 差异: h1 消息完整读入; h2 按帧流式处理 (流控/取消/多路复用)

HttpChannelHolder: getHttpChannel() — 通道获取 (上下文持有)
```

## 关键设计 (why)

1. **协议无关通道**: 上层 (协议/triple/rest) 只面对 HttpChannel 抽象 — h1/h2 差异被隔离
2. **异步写**: writeHeader/writeMessage 返回 CompletableFuture — 与 D-8a 异步底座一致
3. **h1/h2 双模型**: h1 完整消息 vs h2 流式帧 — HTTP/2 多路复用/流控的基础
4. **元数据抽象**: HttpMetadata 统一 headers/content-type — 编解码/路由可用

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| HttpChannel 接口 | http12/HttpChannel.java |
| HttpMetadata | http12/HttpMetadata.java:21-34 |
| h1 消息族 | http12/h1/ (10 文件) |
| h2 流式族 | http12/h2/ (17 文件) |
| HttpChannelHolder | http12/HttpChannelHolder.java:21 |

## 负面空间 (Q1 面)

- 不做消息缓存 (h2 流式直接处理)
- 不做协议嗅探 (h1/h2 由 netty 层选择器定, q3)
- 不做多路复用应用面 (h2 复用是底层帧特性)
