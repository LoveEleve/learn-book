# F-4 Encoder/Decoder 编解码 — 请求体的出口与响应的翻译官

> 前置: [[F-6-模板引擎]] (RequestTemplate.body) | 引出: [[F-5-拦截器链]] (下一步) | 对照: Jackson ObjectMapper + Spring HttpMessageConverter
> 🟡 B | 方案 B (标准) | 闭环: q1(三接口契约) q2(状态码语义) q3(RetryAfter 桥接) q4(注解驱动)
> Pass 2 闭环: q1(接口面) q2(Default 族) q3(ErrorDecoder+RetryAfter) q4(JsonCodec+annotation)

**读者处境**: `String body` 和 `byte[] body` 怎么变成 HTTP body? 404/204 响应为什么不抛异常? 服务端返回 Retry-After 头, Feign 怎么知道要重试? 注解式错误码 (@ErrorCodes) 怎么映射到异常类?

### 1. 三接口契约 — Encoder/Decoder/ErrorDecoder 的分工

场景: 编解码的"接口面"边界在哪?
源码路径:
- **Encoder** (codec/Encoder.java:69-82): 单方法 encode(object, bodyType, template); **MAP_STRING_WILDCARD 标记表单编码** (L71) — **真正消费在 RequestTemplateFactoryResolver.java:235** (formParams 打包 LinkedHashMap → encode(formVariables, MAP_STRING_WILDCARD, mutable), L223-244); 异常契约 EncodeException (RuntimeException 包装规则 L239-240); **编码器只管 template.body(), 不设 Content-Type** — 头由 Contract 层 @Headers 元数据注入
- **Decoder** (codec/Decoder.java:70-91): 单方法 decode(response, type); 三异常契约 — IOException (传播) / **DecodeException (检查异常包装)** / **FeignException (解码成功但操作失败)**
- **ErrorDecoder** (codec/ErrorDecoder.java:61-77): decode(methodKey, response) — 非 2xx 时调用; **methodKey = Feign#configKey 共享键** (Javadoc L68 显式引用, 跨域 F-1 §5/F-2/F-3 日志同键); **"如果可重试, 包装 RetryableException" 契约** (L27-30)
- 三者 @Deprecated 内部类 Default 均委托独立类 (DefaultEncoder/DefaultDecoder/DefaultErrorDecoder) — 13.x 抽取
关键设计 (q1): **Decoder 双异常语义** — DecodeException = "格式转换失败", FeignException = "HTTP 层失败但解码成功" (F-3 proceed 的 throw decodeError 走 ErrorDecoder, 非 Decoder); 接口 Javadoc 明确区分, 实现必须遵循。 [模式: 三接口分治]

### 2. Default 族 — 最小实现与 404/204 的"空值语义"

场景: 不配 Encoder/Decoder 时默认行为?
源码路径:
- DefaultEncoder (codec/DefaultEncoder.java:23-34): 仅 String/byte[]/null 三态 — 其他抛 EncodeException; **不支持 Map → 表单接口必须配专用 Encoder (form 模块 FormEncoder 等)**, 否则运行期抛 EncodeException
- **DefaultDecoder extends StringDecoder** (codec/DefaultDecoder.java:23-28): **404/204 → Util.emptyValueOf(type)** (L13) — 返回类型对应的"空值" (null/空集合/空 Map)
- StringDecoder (codec/StringDecoder.java:25-36): 404/204/body-null → null; String 类型 → toString; 其他 → DecodeException
- **双层 404/204 语义互补**: F-3 proceed (dismiss404 时进解码) + Decoder (404/204 → emptyValueOf) — 与 F-3 状态码分派协作
关键设计 (q2): **404/204 = "无内容"而非"错误"** — 204 No Content 语义上无 body, 404 在 dismiss 配置下是合法结果; emptyValueOf 让 List<T> 返回空 List 而非 null (类型感知空值)。 [模式: 类型感知空值]

### 3. ErrorDecoder.Default — 状态码 → FeignException 与 Retry-After 桥接

场景: 500 响应怎么变成异常? Retry-After 头怎么让重试器知道?
源码路径:
- DefaultErrorDecoder (codec/DefaultErrorDecoder.java:27-42): decode → **errorStatus(methodKey, response, maxBodyBytes, maxBodyChars)** (L47, static 导入 L18) → FeignException 族; **Retry-After 头解析 → 非空则包装 RetryableException** (L43-53) — 桥接 F-3 runWithRetry
- maxBodyBytesLength/maxBodyCharsLength: 异常消息含 body 的截断上限 (构造可配, L31-41)
- **RetryAfterDecoder** (codec/ErrorDecoder.java:97-131): 两种格式 — `^\d+\.?0*$` 秒数 (L124-128, **支持小数 `86400.0`** L54-55 测试) / **RFC_1123_DATE_TIME HTTP-date** (L129); 溢出/非法 → null 静默 (L130-131, RetryAfterDecoderTest:38-60 四场景实证)
- 秒数 → currentTimeMillis() + delta (L126-127) — **相对秒转绝对 epoch 传递**: F-3 DefaultRetryer 收到后 `retryAfter() - currentTimeMillis()` 还原相对间隔 (DefaultRetryer.java:50-53) — 绝对时间传递避免两次时钟读取误差
关键设计 (q3): **Retry-After 是服务端的反压信号 → 客户端重试器** — DefaultErrorDecoder 解析头 → RetryableException.retryAfter → F-3 DefaultRetryer 优先使用 (F-3 q3); 双格式兼容 (秒/HTTP-date) + 小数兼容 (10.3 #980) + 静默容错。 [模式: 协议头桥接]

### 4. JsonCodec 族 — @Experimental 的 JSON 抽象下沉

场景: 13.x 怎么让 JSON 编解码可插拔?
源码路径:
- Codec 接口 (codec/Codec.java:21, @Experimental): encoder()+decoder() 对 — **成对提供**
- JsonCodec extends Codec (codec/JsonCodec.java:21, @Experimental): 协变返回 JsonEncoder/JsonDecoder (JsonEncoder:21/JsonDecoder:23)
- JsonEncoder extends Encoder 空接口 (codec/JsonEncoder.java:21); **JsonDecoder extends Decoder + convert(object, type)** (codec/JsonDecoder.java:23-26) — 对象转换扩展点
- 实现外包: **5 个模块实现 `implements Codec, JsonCodec`** (jackson/JacksonCodec:27, jackson3/Jackson3Codec:27, gson/GsonCodec:27, moshi/MoshiCodec:27, fastjson2/Fastjson2Codec:27); jackson-jr 未实现 (旧 API); 核心不依赖第三方 JSON
关键设计 (q1): **接口下沉, 实现外包** — 13.x 把 JSON 抽象进 core (JsonCodec 族), 具体库在独立模块; Codec 成对保证 encoder/decoder 同源 (不会 Jackson 编码 Gson 解码)。 [模式: SPI 下沉]

### 5. FeignException 族 — 状态码细分的异常体系

场景: 400/404/500 异常怎么区分?
源码路径:
- FeignException (FeignException.java, 561 行): status/request/message 三要素; **内容访问 contentUTF8()/contentBytes()** (DefaultErrorDecoderTest:78 实证); errorStatus 工厂 L196
- 子类: **17 个异常类** — FeignClientException (4xx 基类, L313) + **11 个状态码特化** (400 BadRequest L324/401 Unauthorized/403 Forbidden/404 NotFound L345/405/406/409/410/415/429/422 L394) + FeignServerException (5xx 基类 L401) + **5 个特化** (500 InternalServerError L412/501/502/503/504 L440) — 10.3 "Fine-grained HTTP error exceptions (#854)"
- errorStatus 三路分派 (L223-234): isClientError (400-499) → clientErrorStatus switch 特化表 (L242-270) / isServerError (500-599) → serverErrorStatus switch (L274-295) / 其他 → 通用 FeignException; 未特化码 → 基类兜底
关键设计 (q2): **状态码 → 异常子类映射在工厂** — errorStatus 按 status 区间选子类 (4xx/5xx/特定码), 用户 catch FeignNotFoundException 精准处理 404; body 内容进异常消息 (maxBodyBytes 截断防消息爆炸)。 [模式: 异常族]

### 6. annotation-error-decoder — 注解驱动的错误映射 (@ErrorCodes)

场景: 怎么用注解声明"400 → ValidationException"?
源码路径:
- AnnotationErrorDecoder (annotation-error-decoder/src/main/java/feign/error/AnnotationErrorDecoder.java, 202 行): **methodKey → MethodErrorHandler Map + 默认回退** (L32-48) — decode 先查 Map, 无则 defaultDecoder (包为 feign.error 非 errorcode)
- Builder.generateErrorHandlerMapFromApi (L79-100): **反射扫描接口注解** — 类级默认 + 方法级 @ErrorCodes/@ErrorHandling → ExceptionGenerator
- ExceptionGenerator (243 行): 状态码/响应体 → 异常构造 (FeignExceptionConstructor + ResponseBody/ResponseHeaders 注解)
- ErrorCodes/ErrorHandling 注解面 (ErrorCodes.java:17-21): @ErrorCodes(codes+generate) / **@ErrorHandling(codeSpecific+defaultException, @Inherited+@Target{TYPE,METHOD})** (ErrorHandling.java:17-24) / **NO_DEFAULT 哨兵类** (L27-34) — 未配置时的占位, 触发默认回退
- 与 DefaultErrorDecoder 关系: AnnotationErrorDecoder 默认回退 DefaultErrorDecoder (L34) — 注解优先, 未覆盖走默认
关键设计 (q4): **声明式错误映射 = 编译期可见的契约** — 注解把"HTTP 错误 → 业务异常"关系写在接口上, 运行时反射建表; 回退链保证未注解状态码仍被处理。 [模式: 注解声明 + 回退链]

## 代码类型
Architecture (编解码核心) + Protocol (Retry-After 桥接)

## 负面空间 — 编解码刻意不做的事

- **不做 JSON 内置**: JsonCodec 仅抽象, jackson/gson 等外包 (13.x 决策); 核心零第三方 JSON 依赖
- **不做压缩处理 (编解码层)**: 请求体 gzip/deflate 由 Client 传输层按 Content-Encoding 头处理 (DefaultClient: 请求 GZIPOutputStream L210-211 / 响应 GZIPInputStream L125-129); Encoder/Decoder 不感知压缩
- **不做流式解码**: 默认 closeAfterDecode 后 body 关闭; StreamDecoder (stream/ 包) 是独立变体
- **不做解码缓存**: 每响应重新 decode, 无缓存
- **不做 Content-Type 自动路由**: Decoder 选择靠 Builder 配置, 不按 Content-Type 头自动路由
- **不做编码对称性校验**: Encoder/Decoder 是独立接口, 框架不保证"能编码的就能解码" (Codec 对是用户选择)

→ 引出: 拦截器怎么在编解码周围插一脚? → F-5 拦截器链
