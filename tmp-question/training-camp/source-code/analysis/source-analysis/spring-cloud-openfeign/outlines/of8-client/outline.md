# OF-8 HTTP 客户端与压缩 — 三选型与 Gzip 双面

> 前置: [[OF-5-负载均衡]] (装饰链底层) | 引出: [[OF-9-动态刷新]] | 对照: Feign 本体 Client + OkHttp 认知
> 🟡 B | 8 KP | [模式: 配置类选型 + 拦截器双面]
> Pass 2 闭环: q1(HttpClient5 连接池) q2(Http2 客户端) q3(请求压缩) q4(响应解压配置)

**读者处境**: 底层 HTTP 客户端怎么选? 连接池怎么配? 请求/响应压缩怎么协商? 这篇拆 HttpClient5 (177) + Http2 + encoding/ 8 文件。

### 1. HttpClient5 连接池 — PoolingHttpClientConnectionManagerBuilder

场景: Apache 客户端怎么配?
源码路径:
- **PoolingHttpClientConnectionManagerBuilder** (L60-140): **setSSLSocketFactory (disableSslValidation)** / **setMaxConnTotal/PerRoute** / **setConnPoolPolicy (PoolReusePolicy)** / SoTimeout; ⚠ **2 Bean 条件** (L72-92: hc5ConnectionManager @ConditionalOnMissingBean + httpClient5)
- **FeignHttpClientProperties** (L79-108): 7 字段 + **hc5/okHttp/http2 三子配置**; ⚠ **默认值穷举** (L42-72): **MAX_CONNECTIONS=200 / PER_ROUTE=50 / SSL_VALIDATION=false / FOLLOW_REDIRECTS=true / CONNECTION_TIMEOUT=2000**; ⚠ **Hc5Properties: PoolReusePolicy 默认 FIFO** (L208) + socketTimeout; OkHttp 子配置 (readTimeout 60s)
- HttpClientBuilderCustomizer + ApacheHttp5Client → OF-5 装饰链
关键设计 (q1): **连接池显式配置 + SSL 可关 + 复用策略**。[模式: 连接池面]

### 2. Http2 客户端 — Java HttpClient

场景: JDK HTTP/2 怎么配?
源码路径:
- **HttpClient.newBuilder()** (L40-50): followRedirects → **Redirect.ALWAYS/NEVER** + version
- **Http2ClientCustomizer** (http2client/ 子包): 深度定制
- Http2Properties (子配置)
关键设计 (q2): **Java 原生 + 重定向可配 + Customizer 定制点**。[模式: Http2 面]

### 3. 请求压缩 — 双条件 Gzip

场景: 请求体什么时候压?
源码路径:
- **requiresCompression** (L60-73): **matchesMimeType (mimeTypes 列表) && contentLengthExceedThreshold (minRequestSize)** — 双条件; ⚠ **apply = addHeader(Content-Encoding, getContentEncodings())** (L30-45)
- **FeignClientEncodingProperties**: **mimeTypes 默认 [text/xml, application/xml, application/json] + minRequestSize 默认 2048 + contentEncodingTypes 默认 [gzip, deflate]** (L36,41,46)
关键设计 (q3): **双条件门槛 (只压值得压的) + 默认值明确 + 容错**。[模式: 请求压缩面]

### 4. 响应解压与配置面 — Accept-Encoding + 开关

场景: 响应压缩怎么协商?
源码路径:
- **FeignAcceptGzipEncodingInterceptor**: **Accept-Encoding: gzip** (响应协商)
- **AutoConfiguration** (L41-50): **response.enabled 开关** (L43) + ⚠ **OkHttpFeignClientBeanMissingCondition** (L46: @ConditionalOnMissingClass("feign.okhttp.OkHttpClient") — OkHttp 缺失才启用, 自带解压不叠加); ⚠ **请求面 request.enabled 对称开关** (FeignContentGzipEncodingAutoConfiguration L43)
关键设计 (q4): **响应协商 + OkHttp 特判 + 请求/响应对称**。[模式: 响应面]

## 代码类型
Architecture (选型配置) + 网络优化

## 负面空间 (OF-8, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不连接池动态调整 | 启动配置 (q1) |
| 不做协议自动协商 | 显式选型 (q1) |
| 不做 HTTP/3 | 仅 h2 (q2) |
| 不强制压缩 | 双条件门槛 (q3) |
| 不压缩级别配置 | 默认级别 (q3) |
| 不做 Brotli 等新格式 | 仅 gzip (q4) |

## 结尾桥 OUTBOUND

- → [[OF-9-动态刷新]]: 客户端配置与刷新联动
- → 对照: Feign 本体 Client (Client.Default/OkHttp/Apache) / Dubbo 传输层选型
