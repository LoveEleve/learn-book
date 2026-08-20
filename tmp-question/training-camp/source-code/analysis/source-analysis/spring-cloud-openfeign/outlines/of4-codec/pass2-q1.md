# OF-4 编解码 — Pass 2 闭环 Q1: 编码面 (SpringEncoder)

> 核心: SpringEncoder.encode | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 请求体怎么用 Spring HttpMessageConverter 编码?**

## 机制链 (已实证)

```
SpringEncoder (implements Encoder) (L75-81):
├── ObjectFactory<HttpMessageConverters> — 转换器来源 (Spring Boot 装配)
├── ObjectProvider<HttpMessageConverterCustomizer> — 定制器
└── converters 惰性加载: getConverters() + customizers.forEach(accept)

encodeWithMessageConverter (L120-160):
├── initConvertersIfRequired
├── converters 循环 (L124): 找能处理的 converter
│   ├── **GenericHttpMessageConverter 分支** (L127-130): 泛型类型 (bodyType)
│   └── 普通分支 (L131): checkAndWrite(body, contentType, converter, request)
├── 失败 → **EncodeException("Error converting request body")** (L136-138)
├── 成功 → request.headers(null) 清空 + **converter 改过的 headers 更新** (L143-146)
├── charset: FeignEncoderProperties.isCharsetFromContentType (L)
└── request.body(outputMessage 字节, charset) (L)
```

## 关键设计 (why)

1. **HttpMessageConverter 复用**: 与 Spring MVC 服务端同一套转换器 (Jackson/Protobuf/ByteArray...) — 生态一致
2. **Generic 分支**: 泛型请求体 (List<T> 等) 走 GenericHttpMessageConverter — 类型安全
3. **Customizer 定制**: HttpMessageConverterCustomizer 可增删改转换器 — 客户端独立配置
4. **headers 回流**: converter 可改 Content-Type 等头 — 转换器决策被尊重
5. **生产友好错误**: "no suitable HttpMessageConverter found for request type [...]" — 明确缺哪种转换器

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| converters 来源 + 定制器 | SpringEncoder.java:75-81 |
| Generic 分支 | SpringEncoder.java:127-130 |
| EncodeException 包装 | SpringEncoder.java:136-138 |
| headers 回流 | SpringEncoder.java:143-146 |
| charsetFromContentType | FeignEncoderProperties.java |

## 负面空间 (Q1 面)

- 不转换器缓存失效 (惰性加载一次)
- 不自定义 converter 优先级 (循环顺序)
- 不做 body 流式编码 (全量字节)
