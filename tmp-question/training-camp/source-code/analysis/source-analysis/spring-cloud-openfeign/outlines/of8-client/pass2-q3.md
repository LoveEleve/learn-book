# OF-8 HTTP 客户端与压缩 — Pass 2 闭环 Q3: 请求压缩 (Gzip)

> 核心: FeignContentGzipEncodingInterceptor | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 请求体什么时候压缩? 阈值/MIME 怎么定?**

## 机制链 (已实证)

```
FeignContentGzipEncodingInterceptor (extends BaseRequestInterceptor):
├── **requiresCompression** (L60-73) — 双条件:
│   ├── **matchesMimeType(CONTENT_TYPE)** (L99-110): getProperties().getMimeTypes() 包含
│   └── **contentLengthExceedThreshold(CONTENT_LENGTH)** (L78-100):
│       └── length > **getProperties().getMinRequestSize()** (L)
├── 压缩条件满足 → GZIP 压缩请求体
└── 失败容错: NumberFormatException → false (不压缩)

BaseRequestInterceptor (L29): abstract implements RequestInterceptor — apply 流程基类
FeignClientEncodingProperties: requestGzipCompression 等开关 (自动装配读)
```

## 关键设计 (why)

1. **双条件压缩**: MIME 匹配 (可配列表) + 长度超阈值 (minRequestSize) — 只压值得压的
2. **阈值防小请求**: 小 body 压缩收益低 (CPU 开销 > 传输节省)
3. **MIME 可配**: 只压缩文本类 (JSON/XML), 二进制不压
4. **容错**: 长度解析失败不压缩 — 安全默认

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| requiresCompression 双条件 | FeignContentGzipEncodingInterceptor.java:60-73 |
| 阈值判定 | 同上 L78-100 |
| matchesMimeType | 同上 L99-110 |
| BaseRequestInterceptor | encoding/BaseRequestInterceptor.java:29 |

## 负面空间 (Q3 面)

- 不强制压缩 (双条件门槛)
- 不压缩级别配置 (默认级别)
- 不做响应体压缩 (响应是 Accept-Encoding 面, q4)
