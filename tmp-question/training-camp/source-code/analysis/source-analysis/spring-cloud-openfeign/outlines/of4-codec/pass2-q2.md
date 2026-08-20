# OF-4 编解码 — Pass 2 闭环 Q2: 解码面 (SpringDecoder + FeignResponseAdapter)

> 核心: SpringDecoder.decode | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 响应怎么用 Spring 提取器解码? Feign Response 怎么变 Spring 响应?**

## 机制链 (已实证)

```
SpringDecoder (implements Decoder) (L64-90):
├── decode (L64): 类型检查 — Class/ParameterizedType/WildcardType → 否则
│   **DecodeException("type is not an instance of Class or ParameterizedType")** (L85-87)
├── converters + customizers (同编码面)
├── **HttpMessageConverterExtractor(type, converters)** (L69) — Spring 提取器
└── extractor.extractData(**new FeignResponseAdapter(response)**) (L70)

FeignResponseAdapter (**SpringDecoder 内部类** L84+):
├── implements ClientHttpResponse — 适配器模式
├── getStatusCode/getStatusText/getBody/getHeaders (L122: getHeaders)
└── Feign Response → Spring ClientHttpResponse — 提取器只认 Spring 接口
```

## 关键设计 (why)

1. **HttpMessageConverterExtractor 复用**: Spring 服务端同款响应提取 (读 body + 转换)
2. **适配器模式**: FeignResponseAdapter 桥接两个生态 — 提取器零改动
3. **类型白名单**: Class/ParameterizedType/WildcardType — 泛型响应 (List<T>/Page<T>) 支持
4. **DecodeException 明确**: 不支持的类型直接报错 — 防静默失败

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| decode + 类型检查 | SpringDecoder.java:64-87 |
| HttpMessageConverterExtractor | SpringDecoder.java:69 |
| FeignResponseAdapter 内部类 | SpringDecoder.java:84+ |
| getHeaders | SpringDecoder.java:122 |

## 负面空间 (Q2 面)

- 不流式解码 (Extractor 全量读)
- 不响应缓存 (每次 decode)
- 不类型推断 (注解/泛型显式)
