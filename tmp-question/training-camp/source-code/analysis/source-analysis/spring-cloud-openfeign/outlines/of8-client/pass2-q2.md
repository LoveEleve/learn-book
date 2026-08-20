# OF-8 HTTP 客户端与压缩 — Pass 2 闭环 Q2: Http2 客户端

> 核心: Http2ClientFeignConfiguration + Http2ClientCustomizer | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Java HttpClient (HTTP/2) 怎么配? 定制点?**

## 机制链 (已实证)

```
Http2ClientFeignConfiguration (L40-90):
├── **HttpClient.newBuilder()** (L):
│   ├── followRedirects → **Redirect.ALWAYS / Redirect.NEVER** (属性驱动)
│   └── **version (Http2Properties.getVersion)** — HTTP/2 版本
├── **Http2ClientCustomizer 定制** (L): customizers.forEach(customize(httpClientBuilder))
│   ← http2client/ 子包 (Http2ClientCustomizer.java)
└── httpClient = builder.build()
→ delegate = new Http2Client(httpClient) (OF-5)

Http2Properties (FeignHttpClientProperties 子配置):
└── version (HTTP_2 等)
```

## 关键设计 (why)

1. **Java 原生 HttpClient**: 无需第三方依赖 — JDK 11+ HTTP/2 客户端
2. **重定向可配**: Redirect.ALWAYS/NEVER — 语义明确
3. **Customizer 定制点**: Http2ClientCustomizer — 细粒度 (代理/超时等)
4. **三选型之一**: HttpClient5 (Apache) / Http2 (JDK) / OkHttp (OF-5 组合)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| HttpClient.newBuilder | Http2ClientFeignConfiguration.java:40-50 |
| Redirect 配置 | 同上 |
| Http2ClientCustomizer | http2client/Http2ClientCustomizer.java |
| Http2Properties | FeignHttpClientProperties.java:108 |

## 负面空间 (Q2 面)

- 不做 HTTP/3 (仅 h2)
- 不自动版本协商 (显式)
- 不做连接池显式控制 (JDK 默认)
