# OF-4 编解码 — Pass 2 闭环 Q3: 兼容面 (ResponseEntityDecoder + 错误解码)

> 核心: ResponseEntityDecoder + FeignErrorDecoderFactory | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: ResponseEntity 返回怎么支持? 错误怎么解码?**

## 机制链 (已实证)

```
ResponseEntityDecoder (L41-95) — 装饰器模式:
├── decode 三分支 (L55-67):
│   ├── isParameterizeHttpEntity (ResponseEntity<T>) → **createResponse(decodedObject, response)** (L55)
│   │   ← 先 delegate 解码再包 ResponseEntity
│   ├── isHttpEntity (HttpEntity) → createResponse(null) (L60)
│   └── 否则 → delegate.decode (L65) — 透传
├── isHttpEntity/isParameterizeHttpEntity (L72-83): Class/ParameterizedType 判定
└── createResponse (L81-88): new ResponseEntity(instance, headers, HttpStatusCode.valueOf(status))

FeignErrorDecoderFactory (OF-2 已见): 自定义 ErrorDecoder 工厂
└── 构造时经 FeignClientFactoryBean L206-209 注入 builder (按客户端)
```

## 关键设计 (why)

1. **装饰器透传**: ResponseEntityDecoder 只处理 HttpEntity 类型, 其余委托 — 组合而非侵入
2. **ResponseEntity<T> 泛型**: ParameterizedType 判定 → 解码后包 ResponseEntity (状态码+头)
3. **错误解码可配**: FeignErrorDecoderFactory 每客户端注入 — 错误语义自定义 (Retry-After 等, feign F-4 对照)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 三分支 | ResponseEntityDecoder.java:55-67 |
| isHttpEntity 判定 | ResponseEntityDecoder.java:72-83 |
| createResponse | ResponseEntityDecoder.java:81-88 |
| FeignErrorDecoderFactory 注入 | FeignClientFactoryBean.java:206-209 |

## 负面空间 (Q3 面)

- 不 ResponseEntity 流式 (全量解码)
- 不 HttpEntity 双向 (仅响应)
- 不错误重试 (重试在 Retryer/OF-5)
