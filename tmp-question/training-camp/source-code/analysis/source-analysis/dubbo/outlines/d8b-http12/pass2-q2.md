# D-8b HTTP 传输栈 http12 — Pass 2 闭环 Q2: 编解码面 (mediaType 驱动 codec 族)

> 核心: CodecUtils + CodecFactory 族 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: HTTP 消息体怎么编解码? 不同 content-type 怎么选 codec?**

## 机制链 (已实证)

```
CodecUtils (message/codec/CodecUtils.java):
├── determineHttpMessageDecoder/Encoder(mediaType) — **mediaType (content-type) 驱动选 codec factory**
│   ├── determineHttpMessageDecoderFactory(mediaType) → Optional<HttpMessageDecoderFactory>
│   ├── 无匹配 → UnsupportedMediaTypeException
│   └── createCodec(url, frameworkModel, mediaType)
├── disallowedContentTypes 过滤 (L49-101): 配置的禁用 content-type 排除
└── CodecFactory SPI 族 (message/codec/):
    ├── BinaryCodecFactory → BinaryCodec — 二进制编解码 (hessian2 类, D-10 序列化关联)
    ├── JsonCodecFactory → JsonCodec — JSON
    ├── HtmlCodecFactory → HtmlCodec — HTML
    └── JsonPbCodecFactory — JSON+Protobuf (D-9 triple 关联!)
```

## 关键设计 (why)

1. **mediaType 驱动**: Content-Type 决定编解码器 — HTTP 语义原生 (标准 content negotiation)
2. **factory 可插拔**: CodecFactory 族 SPI — 新格式加 factory 即可
3. **binary/json/html/jsonpb 四族**: 覆盖 RPC 二进制 (hessian2/protobuf) + REST (json/html) + triple (jsonpb) 场景
4. **禁用列表**: disallowedContentTypes — 安全面 (禁用危险 content-type)
5. **与 D-10 序列化面呼应**: BinaryCodec/JsonPbCodec 是序列化 SPI 在 HTTP 层的适配

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| determineHttpMessageDecoder (mediaType 驱动) | message/codec/CodecUtils.java:40-60 |
| disallowedContentTypes 过滤 | CodecUtils.java:49-101 |
| Codec 族 (Binary/Json/Html/JsonPb) | message/codec/ |

## 负面空间 (Q2 面)

- 不做 content negotiation 协商 (客户端显式 content-type)
- 不做 codec 缓存 (每次 createCodec)
- 不做多格式自动探测 (禁用列表外按 mediaType 精确匹配)
