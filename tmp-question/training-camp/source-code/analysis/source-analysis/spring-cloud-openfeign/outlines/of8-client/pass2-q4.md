# OF-8 HTTP 客户端与压缩 — Pass 2 闭环 Q4: 响应解压与配置面

> 核心: AcceptGzip + AutoConfiguration + Properties | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 响应压缩怎么协商? 开关/默认值?**

## 机制链 (已实证)

```
FeignAcceptGzipEncodingInterceptor (L30+):
└── **Accept-Encoding: gzip** (响应压缩协商) — 服务端收到才压缩响应

FeignAcceptGzipEncodingAutoConfiguration (L41-50):
├── @ConditionalOnClass(Feign) + @ConditionalOnBean(Client) (L41-42)
├── **@ConditionalOnProperty("spring.cloud.openfeign.compression.response.enabled")** (L43)
└── ⚠ **OkHttpFeignClientBeanMissingCondition** (L46) — OkHttp 缺失才启用!
    ← OkHttp 自带 Gzip 解压, 不叠加

FeignClientEncodingProperties (L31-41) — 默认值:
├── **mimeTypes 默认 ["text/xml","application/xml","application/json"]** (L36)
└── **minRequestSize 默认 2048** (L41)

HttpEncoding 常量: CONTENT_LENGTH/CONTENT_TYPE/ACCEPT_ENCODING_HEADER/GZIP_ENCODING (L29-49)
```

## 关键设计 (why)

1. **响应协商**: Accept-Encoding: gzip — 服务端按需压缩 (响应面, 与 q3 请求面对称)
2. **OkHttp 特判**: OkHttp 自带解压 — 避免双重处理 (条件缺失才启用)
3. **默认值明确**: 3 MIME (文本类) + 2048 阈值 — 开箱即用
4. **开关驱动**: response.enabled 属性 — 可关闭

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Accept-Encoding | FeignAcceptGzipEncodingInterceptor.java:30+ |
| AutoConfiguration 条件 | FeignAcceptGzipEncodingAutoConfiguration.java:41-50 |
| mimeTypes/minRequestSize 默认 | FeignClientEncodingProperties.java:36,41 |
| OkHttp 缺失条件 | encoding/OkHttpFeignClientBeanMissingCondition.java |

## 负面空间 (Q4 面)

- 不强制响应压缩 (协商)
- 不压缩级别控制 (服务端决定)
- 不做 Brotli 等新格式 (仅 gzip)
