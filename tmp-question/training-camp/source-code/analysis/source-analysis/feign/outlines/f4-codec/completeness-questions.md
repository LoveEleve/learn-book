# F-4 Encoder/Decoder 编解码 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. Encoder/Decoder/ErrorDecoder 三接口的异常契约分别是什么? 为什么 Decoder 要区分 DecodeException 和 FeignException?
2. DefaultDecoder 的 emptyValueOf 为什么是"类型感知空值"? 与直接返回 null 的差异?
3. Retry-After 头怎么桥接到 F-3 的重试器? 双格式 (秒/HTTP-date) 兼容的动机?
4. JsonCodec 族"接口下沉实现外包"的设计权衡? Codec 成对的意义?
5. annotation-error-decoder 的回退链结构? 未注解状态码怎么处理?

## B. 源码实证 (5)

6. DefaultEncoder 支持哪三种 bodyType? 其余类型抛什么? (grep DefaultEncoder)
7. DefaultDecoder 的 404/204 分支返回什么? 与 StringDecoder 的 null 分支差异? (grep emptyValueOf)
8. RetryAfterDecoder 的秒数正则 `^\d+\.?0*$` 支持哪些输入? 小数怎么处理? (grep 正则)
9. DefaultErrorDecoder 的 maxBodyBytesLength 语义? 默认值? (grep 构造)
10. FeignException 的 FeignClientException/FeignServerException 分界在哪个 status? (grep L313 附近)

## C. 推理深挖 (5)

11. 404 的双层语义 (F-3 dismiss404 进解码 + Decoder emptyValueOf) 为什么不冲突?
12. errorStatus 怎么决定用哪个 FeignException 子类? body 截断在哪一步?
13. AnnotationErrorDecoder 的 methodKey → handler 表是运行时建还是构建期建? 性能影响?
14. RetryAfterDecoder 的 `^[0-9]+\.?0*$` 为什么允许小数? 10.3 #980 的背景?
15. 如果 Encoder 抛 EncodeException, 走 F-3 的哪个路径? 会重试吗?

## D. 跨域扩展 (5)

16. Feign Decoder vs Spring HttpMessageConverter 的适配哲学?
17. JsonCodec 抽象 vs Jackson ObjectMapper 直接注入的取舍?
18. Retry-After 处理 vs gRPC 的 RetryPolicy 服务端配置?
19. annotation-error-decoder vs OpenAPI 生成器的异常模型?
20. 给 Feign 加"Content-Type 自动路由 Decoder", 应该在 F-4 还是 F-1 Builder? 为什么当前不做?
