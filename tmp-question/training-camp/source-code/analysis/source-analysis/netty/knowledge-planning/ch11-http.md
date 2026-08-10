# Ch11 HTTP Codec — 知识规划

> 来源: 11 源文件 | ~2700 行 | codec-http/handler/codec/http/
> 基线: Ch10 Codec 框架定义了积攒+拆包 — Ch11 回答 "HTTP/1.1 协议怎么在框架上实现"

---

## 01-02 核心机制 + 分类

### 🔴 Deep
| KP | 为什么🔴 |
|----|---------|
| **HttpObjectAggregator 三阶段聚合** | start→aggregate→finish — chunked 分块拼回完整消息，handleOversizedMessage 三路分支(强制close/可恢复/413) |
| **HttpServerCodec HEAD/CONNECT 跟踪** | long 位队列追踪方法 — HEAD 无 body, CONNECT 隧道模式, 歧义 Transfer-Encoding 强制关闭 |

### 🟡 Working: HttpRequestDecoder/HttpResponseDecoder 行解析、HttpContentCompressor 压缩协商、消息模型三层接口

---

## 03 聚类

### Cluster A: HTTP 编解码管道 (6 KPs)
1. HttpRequestDecoder: 继承 HttpObjectDecoder, createMessage→DefaultHttpRequest
2. HttpResponseDecoder: createMessage→DefaultHttpResponse
3. HttpServerCodec: CombinedChannelDuplexHandler 组合 decoder+encoder, HEAD/CONNECT 位队列
4. HttpClientCodec: 请求响应配对(HttpMethod queue), CONNECT 隧道, AtomicLong 缺失响应计数
5. HttpMessage/HttpRequest/HttpResponse 三层接口
6. HttpContent/LastHttpContent 分块模型 + EMPTY_LAST_CONTENT 哨兵

### Cluster B: 聚合与压缩 (5 KPs)
1. HttpObjectAggregator: start→aggregate→finish 三阶段
2. handleOversizedMessage: Content-Length 超限/Transfer-Encoding chunked 超限
3. 100-continue/413/417 自动响应
4. HttpContentCompressor: Accept-Encoding q值解析 + 编码优先级链
5. EmbeddedChannel 压缩 sub-channel + contentSizeThreshold 过滤

### 教学顺序: A → B
