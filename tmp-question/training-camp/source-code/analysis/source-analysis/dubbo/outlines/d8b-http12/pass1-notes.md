# D-8b HTTP 传输栈 http12 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-remoting-http12 126 文件)
> 09 域级审计: 执行计划未覆盖 — PLAN 顶层包扫描抓出 (126 文件 ≥50 已过设计决策测试); triple→http12 106 import 实证

## 入口展开 (Level-1~2, 已读源码)

### Level-1: HttpChannel 抽象 (顶层核心)

```
HttpChannel 接口 (http12/HttpChannel.java) — HTTP 通道抽象 (协议无关):
├── writeHeader(HttpMetadata) — 写响应头 (CompletableFuture 异步)
├── writeMessage(HttpOutputMessage) — 写消息体
├── newOutputMessage() — 创建输出消息
├── remoteAddress()/localAddress() — 地址
└── flush() — 冲刷

HttpMetadata (http12/HttpMetadata.java:21-34): headers/contentType/header(name) — 消息元数据
```

### Level-2: 包结构 (126 文件穷举)

```
http12/ 顶层 (~28): HttpChannel/HttpRequest/HttpResponse/HttpMetadata/HttpHeaders/
  HttpHeaderNames/HttpCookie/HttpMethods/HttpStatus/HttpVersion + 流式观察者族
  (AbstractServerHttpChannelObserver/FlowControlStreamObserver — HTTP 流式面, D-9 关联)
├── h1/ (10): HTTP/1.1 消息模型 (Http1RequestMessage/Http1ResponseMessage?)
├── h2/ (17): HTTP/2 消息模型 + 帧处理
├── message/ (37): HttpInputMessage/HttpOutputMessage + codec/ (15):
│   ├── BinaryCodec/BinaryCodecFactory — 二进制编解码 (hessian2 等? D-10 关联)
│   ├── JsonCodec/JsonCodecFactory — JSON
│   ├── HtmlCodec — HTML
│   └── JsonPbCodecFactory — JSON+Protobuf (D-9 triple 关联!)
├── netty4/ (13): HttpWriteQueueHandler + h1/h2 子目录 (NettyHttpChannel 实现)
│   └── NettyHttpHeaders/StringValueIterator (headers 适配)
├── rest/ (8): Mapping/OpenAPI/Operation/Param/Schema — REST 元数据 (D-9 REST_ENABLED 用)
├── command/ (5): 命令面
└── exception/ (8): 异常族
```

## 09 域级审计表 (执行计划未覆盖 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| 执行计划未覆盖 (顶层扫描抓出) | http12 126 文件: h1/h2/message/netty4/rest/command/exception 7 子包 | 接受 (PLAN 已记录, D-8b) |
| D-9 依赖实证 | triple→http12 106 import (PLAN §五) | 接受 |
| 执行计划未提: codec 族 | BinaryCodec/JsonCodec/HtmlCodec/JsonPbCodecFactory — 内容编解码 | 补锚 (D-10 关联) |
| 执行计划未提: REST 元数据 | rest/: Mapping/OpenAPI/Operation/Param/Schema | 补锚 (D-9 REST_ENABLED) |
| 执行计划未提: HTTP 流式 | AbstractServerHttpChannelObserver/FlowControlStreamObserver | 补锚 (D-9 stream) |

## 待展开 (下一层)

1. h1/h2 消息模型差异 (Http1RequestMessage vs Http2RequestMessage)
2. netty4 适配细节 (Http12NettyServer? HttpWriteQueueHandler 背压)
3. codec 族选择机制 (CodecFactory — content-type 驱动?)
4. REST Mapping/OpenAPI 机制 (路径→处理器映射)
5. HttpChannelHolder (通道持有/复用)
