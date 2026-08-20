# OF-4 编解码 — Spring 转换器与分页

> 前置: [[OF-3-契约集成]] (Pageable queryMapIndex) | 引出: [[OF-7-配置隔离]] | 对照: Feign 本体 F-4 Codec + Spring MVC HttpMessageConverter
> 🟡 B | 8 KP | [模式: 适配器 + 装饰器 + 组合 + Jackson Module]
> Pass 2 闭环: q1(编码面) q2(解码面) q3(兼容面) q4(分页面)

**读者处境**: 请求体/响应体怎么用 Spring HttpMessageConverter? ResponseEntity 返回? 分页怎么传/收? 这篇拆 SpringEncoder (273) + SpringDecoder (128) + 分页族。

### 1. 编码面 — SpringEncoder + HttpMessageConverter

场景: 请求体怎么编码?
源码路径:
- **converters 来源** (L75-81): ObjectFactory<HttpMessageConverters> + HttpMessageConverterCustomizer 定制; ⚠ **SpringFormEncoder 表单底座** (L83-87: 单参构造默认 SpringFormEncoder + FeignEncoderProperties + EmptyObjectProvider); Customizer = **Consumer<List<HttpMessageConverter>>** 函数式
- **encodeWithMessageConverter** (L120-160): converters 循环 → **GenericHttpMessageConverter 分支** (泛型 L127-130) / 普通 → checkAndWrite → **EncodeException("Error converting request body")** (L136-138) → headers 回流 + body 写出
- **charsetFromContentType** (FeignEncoderProperties) + 生产友好错误 "no suitable HttpMessageConverter"
关键设计 (q1): **与 MVC 同套转换器 + Generic 分支 + headers 回流**。[模式: 编码面]

### 2. 解码面 — SpringDecoder + FeignResponseAdapter

场景: 响应怎么解码?
源码路径:
- **decode 类型检查** (L64-87): Class/ParameterizedType/WildcardType → 否则 DecodeException
- **HttpMessageConverterExtractor** (L69) + **extractData(new FeignResponseAdapter)** (L70)
- ⚠ **FeignResponseAdapter 是内部类** (L84+): Feign Response → ClientHttpResponse 适配 (getHeaders L122)
关键设计 (q2): **Spring 同款提取器 + 适配器桥接 + 类型白名单**。[模式: 解码面]

### 3. 兼容面 — ResponseEntityDecoder + 错误解码

场景: ResponseEntity 返回? 错误?
源码路径:
- **ResponseEntityDecoder 三分支** (L55-67): Parameterized HttpEntity → createResponse(decoded) / HttpEntity → createResponse(null) / 否则 delegate
- **FeignErrorDecoderFactory** (OF-2: L206-209 注入) — 每客户端错误解码
关键设计 (q3): **装饰器透传 + 泛型 ResponseEntity 支持**。[模式: 兼容面]

### 4. 分页面 — Pageable 编码 + Page 反序列化

场景: 分页怎么传/收?
源码路径:
- **PageableSpringEncoder** (L38-110): **Pageable → page/size query 参数** (L87-99) + Sort (sortParameter 可配) + delegate fallback
- **PageJacksonModule** (L39-88): **@JsonDeserialize(as=SimplePageImpl)** (L76) — Page 接口用实现类反序列化 (经典技巧); ⚠ **Pageable 也反序列化** (L62-82: PageableMixIn + SimplePageable)
- **PageableSpringQueryMapEncoder extends BeanQueryMapEncoder** (L38) — QueryMap 面 (OF-3 queryMapIndex 消费) + page/size/sort 编码 (L73-85); SortJacksonModule (Sort 序列化) + JsonFormWriter (ObjectMapper 表单)
关键设计 (q4): **组合模式委托 + SimplePageImpl 技巧 + 请求/响应双分页**。[模式: 分页面]

## 代码类型
Architecture (适配/装饰) + Spring MVC 生态

## 负面空间 (OF-4, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不转换器缓存失效 | 惰性加载一次 (q1) |
| 不 body 流式编码 | 全量字节 (q1) |
| 不流式解码 | Extractor 全量读 (q2) |
| 不类型推断 | 注解/泛型显式 (q2) |
| 不 HttpEntity 双向 | 仅响应 (q3) |
| 不 Pageable 嵌套 | 单层 (q4) |

## 结尾桥 OUTBOUND

- → [[OF-7-配置隔离]]: FeignClientsConfiguration 装配 Encoder/Decoder Bean (14 @Bean 中 feignEncoder/feignDecoder/feignEncoderPageable)
- → 对照: Feign 本体 F-4 Codec (Encoder/Decoder 接口) / Spring MVC HttpMessageConverter 体系
