# OF-8 HTTP 客户端与压缩 — Pass 2 闭环 Q1: HttpClient5 连接池

> 核心: HttpClient5FeignConfiguration | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: HttpClient5 连接池怎么配? SSL/连接数/策略?**

## 机制链 (已实证)

```
HttpClient5FeignConfiguration:
├── @ConditionalOnMissingBean(CloseableHttpClient / HttpClientConnectionManager) (L60)
├── **PoolingHttpClientConnectionManagerBuilder.create()** (L):
│   ├── **setSSLSocketFactory (disableSslValidation 开关)** — 生产禁用 SSL 校验 (内网)
│   ├── **setMaxConnTotal (getMaxConnections)** — 总连接数
│   ├── **setMaxConnPerRoute (getMaxConnectionsPerRoute)** — 每路由连接数
│   ├── **setConnPoolPolicy (PoolReusePolicy)** — 连接复用策略
│   ├── setPoolConcurrencyPolicy / setConnectionTimeToLive (timeToLive)
│   └── setDefaultSocketConfig (**SoTimeout**)
├── HttpClientBuilderCustomizer 定制 (L)
└── → delegate = new ApacheHttp5Client(httpClient5) (OF-5 装饰链)

FeignHttpClientProperties (L79-108): disableSslValidation/maxConnections/
maxConnectionsPerRoute/timeToLive/followRedirects/connectionTimeout + **hc5/okHttp/http2 三子配置**
```

## 关键设计 (why)

1. **连接池显式配置**: 总连接/每路由 — 生产容量控制
2. **SSL 校验可关**: disableSslValidation — 内网场景 (证书信任简化)
3. **复用策略可配**: PoolReusePolicy — 连接生命周期语义
4. **Customizer 扩展**: HttpClientBuilderCustomizer — 深度定制
5. **OF-5 装饰链**: ApacheHttp5Client → FeignBlockingLoadBalancerClient — LB 外层

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 条件 + Builder | HttpClient5FeignConfiguration.java:60-140 |
| SSL/连接数/策略 | 同上 (setSSLSocketFactory 等) |
| Properties 字段 | FeignHttpClientProperties.java:79-108 |

## 负面空间 (Q1 面)

- 不连接池动态调整 (启动配置)
- 不连接数自动计算 (显式)
- 不做协议自动协商 (显式 HttpClient5)
