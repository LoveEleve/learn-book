# Ch11 HTTP 聚合与压缩 — HttpObjectAggregator 三阶段 + Content-Encoding 协商

> §11.1 → §11.2 | 依赖 §11.1

### 1. HttpObjectAggregator 三阶段 — start→aggregate→finish

场景: POST 一个 1MB JSON body——TCP 把它切成 10 个 `HttpContent` chunks。Pipeline 中的业务 Handler 需要完整的 body——不能处理碎片。Aggregator 把 chunks 拼回完整消息。

源码路径: `HttpObjectAggregator.java` — `isStartMessage(msg)`: 首次到达的 `HttpRequest/HttpResponse` → 缓存 HttpHeaders → 创建聚合状态。`isContentMessage(msg)`: `HttpContent` 到达→累计 body。`isLastContentMessage(msg)`: `LastHttpContent` 到达(含 trailingHeaders)→`finishAggregation()`→创建 `FullHttpRequest/FullHttpResponse`→`ctx.fireChannelRead(fullMsg)`。`MessageAggregator<I,O>` 泛型框架——I=入站分块类型, O=出站完整类型——Ch10 MessageToMessage 的典型应用。

关键设计: `Content-Length` 和 `Transfer-Encoding: chunked` 双路径——Content-Length 已知→一次性计算 needed bytes→等够→读取。chunked→每 chunk 有 `hex_size\r\n` 头→逐块累积→`0\r\n\r\n` 结束。Aggregator 对两种编码透明——业务 Handler 拿到 `FullHttpRequest` 时不知道 body 是如何传输的。

数据流: `HttpRequest(headers, Content-Length=1048576)`→start→`HttpContent(chunk1, 65536B)`→aggregate→`HttpContent(chunk2, 65536B)`→aggregate→...→`LastHttpContent(chunk16, 剩余)`→finish→`FullHttpRequest(request+1048576B body)`→`ctx.fireChannelRead(fullRequest)`→业务 Handler 拿到完整 body。

### 2. handleOversizedMessage — Content-Length / chunked 超限三路分支

场景: `maxContentLength=1MB`, 但客户端发了 10MB——需要尽早拒绝而不是累积完 10MB 才报错。

源码路径: `HttpObjectAggregator.java` — `handleOversizedMessage`: 根据当前累积大小 vs maxContentLength 做分支。Content-Length 已知且 >max→直接发 `413 Request Entity Too Large`→`ctx.close()`。Transfer-Encoding: chunked→不确定总大小→每个 chunk 到来时检查 total>max→同发 413。三个自动响应: `100-continue`(客户端 Expect: 100-continue→告知可以继续发送 body)、`413`(body 超限→拒绝)、`417 Expectation Failed`(客户端期望不满足→拒绝)。

关键设计: 三种响应的发送是同步的——Aggregator 在 `handleOversizedMessage` 中直接 `ctx.writeAndFlush()` 而不走后续 Handler——因为超大 body 不应该流经业务 Handler。

数据流: POST 10MB→Content-Length=10485760→第一个 chunk→检查 10485760 > 1048576(max)→`ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, REQUEST_ENTITY_TOO_LARGE))`→`ctx.close()`→后续 chunks 被忽略。

### 3. HttpContentCompressor — Accept-Encoding 协商 + 子通道压缩

场景: 客户端 `Accept-Encoding: gzip, deflate;q=0.8`——优先 gzip(无 q=默认 1.0), 备选 deflate(q=0.8)。服务器需要根据这个 header 决定压缩算法。

源码路径: `HttpContentCompressor.java` — q 值解析: `gzip=1.0 > deflate=0.8`→选择 gzip。编码优先级链: `br(brotli) > zstd > snappy > gzip > deflate`——按服务器支持+客户端 q 值排序。`EmbeddedChannel` 作为压缩子通道: `write(HttpContent)` → 子通道 Pipeline(ZlibEncoder/JdkZlibEncoder) → 压缩后 `readOutbound()` → 替换原 `HttpContent`→`ctx.write(compressedContent)`。`contentSizeThreshold` 过滤: body < 阈值(如 1KB)→跳过压缩(压缩收益<开销+压缩头大小)。`content-type` 过滤: 图片/视频已压缩类型→跳过。

关键设计: EmbeddedChannel 是"虚拟 Channel"——它拥有完整的 Pipeline(Decoder→Handler→Encoder)但不绑定到任何网络连接——压缩/解压缩完全在内存中完成——零 I/O。压缩是 CPU 密集操作→通过 `contentSizeThreshold` 和 content-type 过滤避免对小 body 和已压缩 content(如 image/jpeg)做无效压缩。

数据流: `Accept-Encoding: gzip, deflate;q=0.8`→解析: gzip(q=1.0) > deflate(q=0.8)→选择 gzip→`write(HttpContent)`→子通道: `ZlibEncoder.encode(HttpContent)` → `readOutbound()` → 压缩后 `HttpContent`(body size: 100KB→15KB)→`ctx.write(compressed)`→`Content-Encoding: gzip` 添加到 response header→发送给客户端。

### 4. FullHttpRequest/FullHttpResponse — 聚合完成的完整消息

场景: Aggregator 输出了 `FullHttpRequest`——业务 Handler 直接 `request.content()` 拿到完整 body ByteBuf——不需要判断 "这是 HttpContent 还是 LastHttpContent"。

源码路径: `FullHttpRequest extends HttpRequest, LastHttpContent`——请求+body+trailingHeaders 合一。`FullHttpResponse extends HttpResponse, LastHttpContent`——响应+body+trailingHeaders 合一。聚合前: 3 个对象(`HttpRequest` + `HttpContent` + `LastHttpContent`)。聚合后: 1 个 `FullHttpRequest`——业务 Handler 用 `request.content().retain()` 拿到 ByteBuf。

关键设计: `FullHttpRequest` 的 `replace(ByteBuf content)` 返回新 `FullHttpRequest`——用于中间 Handler 修改 body 后传递新请求。`FullHttpResponse` 的 `copy()` 拷贝完整消息(含 headers+body)——用于需要修改响应的场景(如 API Gateway)。

数据流: `handleHttpRequest(ctx, FullHttpRequest req, ...)`→`ByteBuf body = req.content()`→`String json = body.toString(UTF_8)`→`process(json)`→`FullHttpResponse resp = new DefaultFullHttpResponse(...)`→`ctx.writeAndFlush(resp)`。

→ 引出 Ch14 HashedWheelTimer — HTTP 的 Keep-Alive 超时、连接池的 idle 检测、重试的退避延迟——这些定时逻辑的底层依赖是 HashedWheelTimer。512 bucket 时间轮 + remainingRounds 让定时器的 O(1) 插入成为 Netty 超高并发定时管理的基础。
