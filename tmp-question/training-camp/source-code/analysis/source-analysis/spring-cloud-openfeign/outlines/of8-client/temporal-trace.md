# OF-8 HTTP 客户端与压缩 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | FeignAcceptGzipEncodingInterceptor + FeignContentGzipEncodingInterceptor (双面 Gzip); 默认 Client (JDK) |
| 2.1+ | OkHttp 支持 (OkHttpFeignClientBeanMissingCondition); FeignClientEncodingProperties (mimeTypes/minRequestSize) |
| 3.x | HttpClient5 连接池 (PoolingHttpClientConnectionManagerBuilder); Http2Client (JDK 11+); FeignHttpClientProperties 三子配置 |
| 4.x | Http2ClientCustomizer; PoolReusePolicy; response.enabled 开关 |

## 痕迹证据

- FeignContentGzipEncodingInterceptor.java:60-73: requiresCompression 双条件 (2.x 锚)
- FeignClientEncodingProperties.java:36,41: mimeTypes 3 默认 + minRequestSize 2048 (2.1+ 锚)
- FeignAcceptGzipEncodingAutoConfiguration.java:41-46: OkHttp 缺失条件 (2.1+ 锚)
- HttpClient5FeignConfiguration.java: PoolingHttpClientConnectionManagerBuilder (3.x 锚)
- Http2ClientFeignConfiguration.java:40-50: HttpClient.newBuilder (3.x 锚)
- Http2ClientCustomizer: 定制点 (4.x 锚)

## 推断标注

- "2.x 双面 Gzip" — 类实证 (实证)
- "2.1+ OkHttp/Properties" — 条件类/默认值实证 (实证)
- "3.x HttpClient5/Http2" — 配置类实证 (实证)
- "4.x Customizer" — 类实证 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- Feign 本体 Client: Client.Default (JDK)/OkHttp/Apache — 底座对照
- Dubbo 传输层选型 (D-8a): netty3/netty4 切换 vs Feign 三选型 — RPC 对照
- Netty HTTP 编解码 (N-9): 传输层 vs 应用层压缩 — 网络对照
