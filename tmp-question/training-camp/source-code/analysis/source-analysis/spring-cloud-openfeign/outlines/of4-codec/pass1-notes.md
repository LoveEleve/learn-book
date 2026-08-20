# OF-4 编解码 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (SpringEncoder 273 / SpringDecoder 128 / ResponseEntityDecoder / PageableSpringEncoder 139 / PageJacksonModule 297)
> 09 域级审计: OPENFEIGN-PLAN OF-4 (接受, 合并 F-7) — 断言 "HttpMessageConverter 编解码 + 分页" 已 grep 验证

## 入口展开 (Level-1~3, 已读源码)

### Level-1: SpringEncoder.encode (L140-200)

```
SpringEncoder (implements Encoder) (L75-81):
├── ObjectFactory<HttpMessageConverters> + ObjectProvider<HttpMessageConverterCustomizer>
├── converters 惰性加载: messageConverters.getObject().getConverters() + customizers.forEach(accept)
├── encode (L140+): 找 converter → checkAndWrite (contentType + converter.write)
│   ├── request.headers 更新 (converter 可能改头)
│   ├── charset 处理: FeignEncoderProperties.isCharsetFromContentType (L)
│   └── request.body(outputMessage 字节, charset)
└── 错误信息: "no suitable HttpMessageConverter found for request type [...] and content type [...]"
    ← 生产友好 (明确缺哪种转换器)

AbstractFormWriter (L38-71): 表单编码 — write(key, object) + writeAsString 抽象 (JsonFormWriter 实现)
```

### Level-2: SpringDecoder.decode (L64-90)

```
SpringDecoder (implements Decoder):
├── decode (L64): 类型检查 (Class/ParameterizedType/WildcardType) → 否则 DecodeException
├── HttpMessageConverterExtractor(type, converters) — Spring 提取器
└── extractor.extractData(**new FeignResponseAdapter(response)**) — 内部类 (L84+):
    Feign Response → ClientHttpResponse 适配 (getStatusCode/getBody/headers)
```

### Level-3: 兼容与分页面

```
ResponseEntityDecoder (L41-88): ResponseEntity 兼容 — createResponse (instance+headers+HttpStatusCode)
PageableSpringEncoder (L38-88): **组合模式** — Pageable 特判 + delegate fallback (编码委托)
PageableSpringQueryMapEncoder: 分页 QueryMap 编码 (queryMapIndex 消费, OF-3 关联)
PageJacksonModule (L39-88): **Page 反序列化** — @JsonDeserialize(as=SimplePageImpl) + SimplePageImpl(content/pageable)
SortJacksonModule + SortJsonComponent: Sort 排序参数支持
FeignErrorDecoderFactory (OF-2 已见): 自定义 ErrorDecoder 工厂
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-4 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "SpringEncoder→HttpMessageConverter" | converters + checkAndWrite (L140-200) | 接受 |
| "SpringDecoder→HttpMessageConverterExtractor" | L69 | 接受 |
| "ResponseEntity 支持" | ResponseEntityDecoder (L41-88) | 接受 |
| "分页" | PageableSpringEncoder (组合) + PageJacksonModule (SimplePageImpl) | 接受 |
| 补锚: FeignResponseAdapter 是内部类 | SpringDecoder L84+ | 补锚 (适配器位置) |
| 补锚: charsetFromContentType | FeignEncoderProperties | 补锚 |
| 补锚: 错误信息生产友好 | "no suitable HttpMessageConverter" (L) | 补锚 |

## 待展开 (下一层)

1. SpringEncoder.encode 完整 (converter 选择逻辑/写请求)
2. FeignResponseAdapter 适配方法 (getStatusCode/getBody)
3. PageableSpringEncoder 特判细节 (Pageable → query 参数)
4. PageJacksonModule SimplePageImpl 结构
5. FeignEncoderProperties 配置面
