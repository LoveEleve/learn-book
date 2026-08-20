# OF-8 HTTP 客户端与压缩 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (HttpClient5FeignConfiguration 177 / Http2ClientFeignConfiguration / encoding/ 8 文件)
> 09 域级审计: OPENFEIGN-PLAN OF-8 (扩展: +客户端选型面) — 断言 "HttpClient5 连接池 + Gzip 阈值" 已 grep 验证

## 入口展开 (Level-1~3, 已读源码)

### Level-1: HttpClient5 连接池 (L60-140)

```
HttpClient5FeignConfiguration:
├── @ConditionalOnMissingBean(CloseableHttpClient) (L60) / (HttpClientConnectionManager) (L)
├── **PoolingHttpClientConnectionManagerBuilder.create()** (L):
│   ├── setSSLSocketFactory (**disableSslValidation 开关**) (L)
│   ├── **setMaxConnTotal (getMaxConnections)** (L)
│   ├── **setMaxConnPerRoute (getMaxConnectionsPerRoute)** (L)
│   ├── **setConnPoolPolicy (PoolReusePolicy)** (L)
│   ├── setPoolConcurrencyPolicy / setConnectionTimeToLive
│   └── setDefaultSocketConfig (**SoTimeout**) (L)
└── HttpClientBuilderCustomizer 定制 (L)
→ delegate = new ApacheHttp5Client(httpClient5) (OF-5 装饰链)
```

### Level-2: Http2 客户端 (L40-90)

```
Http2ClientFeignConfiguration:
├── **HttpClient.newBuilder()** (L): followRedirects → **Redirect.ALWAYS/NEVER** + version
├── **Http2ClientCustomizer 定制** (L: customizers.forEach(customize)) — http2client/ 子包
└── httpClient = builder.build()
→ delegate = new Http2Client(httpClient) (OF-5)
```

### Level-3: Gzip 压缩 (encoding/ 8 文件)

```
FeignContentGzipEncodingInterceptor (请求压缩):
└── **requiresCompression = matchesMimeType(CONTENT_TYPE) && contentLengthExceedThreshold(CONTENT_LENGTH)** (L60-95)
    ← 双条件: MIME 匹配 + 长度超阈值
FeignAcceptGzipEncodingInterceptor (响应解压):
└── **Accept-Encoding: gzip** (L22-30)
BaseRequestInterceptor (基类) + HttpEncoding (常量) +
FeignAcceptGzipEncodingAutoConfiguration + FeignContentGzipEncodingAutoConfiguration
OkHttpFeignClientBeanMissingCondition (OkHttp 条件)
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-8 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "HttpClient5 连接池" | PoolingHttpClientConnectionManagerBuilder (L) | 接受 (补锚: 完整配置项) |
| "Http2" | HttpClient.newBuilder + Http2ClientCustomizer | 接受 |
| "Gzip 阈值" | contentLengthExceedThreshold (L78) + requiresCompression 双条件 (L70-73) | 接受 (补锚: MIME+长度双条件) |
| 补锚: disableSslValidation | setSSLSocketFactory 开关 | 补锚 |
| 补锚: PoolReusePolicy | setConnPoolPolicy | 补锚 |
| 补锚: Accept-Encoding gzip | FeignAcceptGzipEncodingInterceptor (L22-30) | 补锚 |
| 补锚: OkHttp 条件 | OkHttpFeignClientBeanMissingCondition | 补锚 |

## 待展开 (下一层)

1. FeignHttpClientProperties 完整 (连接池配置字段)
2. contentLengthExceedThreshold 精确阈值 (min 值来源)
3. matchesMimeType 语义 (哪些 MIME 压缩)
4. BaseRequestInterceptor 基类 (apply 流程)
5. AutoConfiguration 条件 (2 个 Gzip 自动装配)
