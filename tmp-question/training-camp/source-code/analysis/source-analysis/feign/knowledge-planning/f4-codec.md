# F-4 Encoder/Decoder 编解码 — 知识规划 (KP)

> 域: F-4 | 级别: 🟡 | 方案: B | 大纲: outlines/f4-codec/outline.md (6 节)

## §01 域定位

Feign 编解码面 = 请求体出口 (Encoder) 与响应翻译 (Decoder) 与错误映射 (ErrorDecoder)。三接口分治 + 404/204 双层语义 + Retry-After 桥接 F-3 重试 + JsonCodec 抽象下沉 + annotation-error-decoder 注解驱动。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| codec/Encoder.java | 89 | 编码接口 + MAP_STRING_WILDCARD | 1 |
| codec/Decoder.java | 92 | 解码接口 + 三异常契约 | 1 |
| codec/ErrorDecoder.java | 135 | 错误接口 + RetryAfterDecoder | 1,3 |
| codec/DefaultEncoder.java | 36 | String/byte[]/null 三态 | 2 |
| codec/DefaultDecoder.java | 34 | 404/204 emptyValueOf + 委托 StringDecoder | 2 |
| codec/StringDecoder.java | 41 | String 解码 | 2 |
| codec/DefaultErrorDecoder.java | 68 | errorStatus + Retry-After 桥接 | 3 |
| codec/JsonCodec.java | 28 | @Experimental 抽象 | 4 |
| codec/JsonEncoder/JsonDecoder | 21/26 | 协变接口 | 4 |
| FeignException.java | 561 | 状态码异常族 | 5 |
| annotation-error-decoder/ (8 文件) | 646 | 注解驱动错误映射 | 6 |

## §05 闭环要点 (Pass 2 内化)

### q1 三接口契约
Encoder (encode 出口) / Decoder (decode 入口, DecodeException vs FeignException 双异常) / ErrorDecoder (非 2xx → 异常, RetryableException 契约桥接重试)。

### q2 状态码语义
404/204 双层: F-3 proceed (dismiss404 进解码) + Decoder (emptyValueOf 类型感知空值); StringDecoder null 分支。

### q3 RetryAfter 桥接
DefaultErrorDecoder 解析 Retry-After → RetryableException.retryAfter → F-3 DefaultRetryer 优先; 双格式 (秒/HTTP-date) + 小数 + 静默容错。

### q4 注解驱动
AnnotationErrorDecoder: methodKey → MethodErrorHandler Map + 默认回退; 反射扫描 @ErrorCodes → ExceptionGenerator; 回退 DefaultErrorDecoder。

## §06 负面空间 (6 条)

不内置 JSON 实现 / 不处理请求体压缩 / 不流式解码 / 不缓存解码结果 / 不自动适配 content-type / 不校验编码对称性

## §07 交叉引用

- ← F-5 URI 模板 (RequestTemplate.body) + F-3 Client 执行 (proceed 分派 + RetryAfter 消费)
- → F-1 Builder 代理 (BaseBuilder encoder/decoder/errorDecoder 装配)
- → F-7 表单 (FormEncoder 实例)
- 另见: Jackson / Spring HttpMessageConverter / gRPC RetryPolicy
