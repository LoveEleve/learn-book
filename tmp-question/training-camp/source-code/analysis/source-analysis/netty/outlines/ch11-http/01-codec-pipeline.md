# Ch11 HTTP 编解码管道 — HttpServerCodec 与 HttpClientCodec

> §11.1 → §11.2 | 依赖 Ch10 Codec

### 1. HttpServerCodec — CombinedChannelDuplexHandler 组合解码+编码

场景: HTTP 请求到达→需要解码 request line + headers + body → 业务处理 → 编码 response line + headers + body。两个独立 Handler 通过 CombinedChannelDuplexHandler 伪装为一个 Handler。

源码路径: `HttpServerCodec.java` — `extends CombinedChannelDuplexHandler<HttpRequestDecoder, HttpResponseEncoder>`——Ch10 Codec 框架的 `ByteToMessageDecoder` + `MessageToByteEncoder` 在这里被 HTTP 协议实例化。`HttpRequestDecoder` — `createMessage→DefaultHttpRequest`, `isDecodingRequest=true`, 继承 `HttpObjectDecoder`(行解析: request line→headers→Content-Length/Transfer-Encoding chunked body)。`HttpResponseDecoder` — `createMessage→DefaultHttpResponse`, `isDecodingRequest=false`。

关键设计: CombinedChannelDuplexHandler 的分离代理让 HttpServerCodec 在 Pipeline 中占一个位置——内部两个 Handler 分别为不同的事件方向工作(inbound→decoder, outbound→encoder)。HEAD/CONNECT 方法跟踪: long 位队列(32 entries 内联+溢出 ArrayDeque)——HEAD 无 body、CONNECT 隧道——两种 HTTP 方法的特殊情况需要特殊处理。

数据流: `channelRead(msg)`→`HttpServerCodec`(inbound)→`HttpRequestDecoder.decode(...)`→`DefaultHttpRequest`+`HttpContent`+`LastHttpContent`→write(response)→`HttpServerCodec`(outbound)→`HttpResponseEncoder.encode(...)`→`write(buf)`→flush→Socket。

### 2. HttpClientCodec — 请求响应配对

场景: Netty 作为 HTTP 客户端——发送请求→接收响应→解析。请求和响应需要一一配对——请求 A 发了, 响应 A 回来了, 但响应 B 可能先回来。

源码路径: `HttpClientCodec.java` — `decoder=HttpResponseDecoder + encoder=HttpRequestEncoder`。`HttpMethod queue`: 发送请求时 enqueue HttpMethod→接收响应时 dequeue→匹配对应的请求方法。HEAD/CONNECT 方法: HEAD 响应无 body, CONNECT 隧道 done pass-through。`AtomicLong` 缺失响应计数: 记录发送了多少请求还没收到响应→`PrematureChannelClosureException` 当连接关闭时有未响应请求。

关键设计: 请求响应 FIFO 排队——HTTP pipelining 允许连续发送多个请求——第一个请求的响应必须先回来——然后是第二个——不能乱序。`HttpMethod queue` 按 FIFO 顺序匹配——保证请求-响应的配对。

数据流: `channel.write(new DefaultFullHttpRequest(GET, "/"))`→enqueue GET→`HttpClientCodec(encoder).write()`→`HttpRequestEncoder.encode(req)`→`channel.flush()`→response 到达→`HttpClientCodec(decoder).channelRead()`→`HttpResponseDecoder.decode()`→dequeue GET→返回 `FullHttpResponse`。

### 3. HttpMessage 三层消息模型 + HttpContent 分块

场景: HTTP 消息不是一次到达的——request/response line 先到, headers 随后, body 可能分多个 TCP 段到达。每种部分对应一个 Java 类型。

源码路径: `HttpMessage` — `HttpVersion+HttpHeaders+DecoderResult`(基础)。`HttpRequest extends HttpMessage + HttpMethod+uri`(请求)。`HttpResponse extends HttpMessage + HttpResponseStatus`(响应)。`HttpContent extends ByteBufHolder`——body 的一个分块。`LastHttpContent extends HttpContent + HttpHeaders trailingHeaders`——最后一个 body 块+trailing headers。`EMPTY_LAST_CONTENT` 哨兵——无 body 的消息复用此单例(不创建新的 `LastHttpContent` 对象)。

关键设计: 三层模型让 Handler 可以按需选择关注级别——基础 Handler 用 `HttpObject`(只关心"这是个 HTTP 对象")；解码 Handler 用 `HttpRequest/HttpResponse`(关心"请求/响应的语义")；body 处理 Handler 用 `HttpContent`(关心"body 数据")。当不需要 body(如 GET 请求)时, 不会创建 HttpContent 对象——`EMPTY_LAST_CONTENT` 哨兵直接表示"没有 body"。

数据流: TCP: `GET / HTTP/1.1\r\nHost: example.com\r\n\r\n`→`HttpRequestDecoder.decode`→`DefaultHttpRequest(GET, /, HTTP/1.1)`→`HttpObjectAggregator` 收到没有 body 的 request→`EMPTY_LAST_CONTENT` 哨兵→`FullHttpRequest(request, EMPTY_BUFFER)`。

→ 引出 §11.2 聚合与压缩 — 解码器输出的消息是分块的(HttpContent)。HttpObjectAggregator 三阶段(start→aggregate→finish)把分块拼成完整消息。HttpContentCompressor 根据 Accept-Encoding 协商压缩算法——EmbeddedChannel 作为子通道进行压缩——让 HTTP 响应从 100KB 压缩到 15KB。
