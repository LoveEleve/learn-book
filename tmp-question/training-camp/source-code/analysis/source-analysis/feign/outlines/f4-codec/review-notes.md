# F-4 Encoder/Decoder 编解码 — REVIEW 记录 (2026-08-15)

## 二轮 REVIEW (07 换维度: 机制语义 vs 源码对照 + 负面空间 + 前向引用)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 10 | 语义补强 | MAP_STRING_WILDCARD 消费点: **RequestTemplateFactoryResolver.java:235** (formParams 打包 LinkedHashMap → encode) — 大纲只写声明没写消费 | 已修 |
| 11 | **语义重大** | DefaultEncoder 不支持 Map → **表单接口必须配专用 Encoder** (FormEncoder), 否则运行期抛 EncodeException (BuildFormEncodedTemplateFromArgs L223-244 实证) | 已修 |
| 12 | 语义补强 | Retry-After 绝对时间传递: F-4 秒→epoch + F-3 `retryAfter()-now` 还原 — 避免两次时钟读取误差 | 已修 |
| 13 | 语义精确化 | FeignException 族: **17 个异常类** (11 个 4xx 特化 + 5 个 5xx 特化 + 2 基类); errorStatus 三路分派 + 双 switch 特化表 (L223-295) | 已修 |
| 14 | 遗漏哨兵 | AnnotationErrorDecoder **NO_DEFAULT 哨兵类** + @Inherited 注解继承性未交代 (ErrorHandling.java:17-34) | 已修 |
| 15 | 实现名单精确化 | **5 模块实现 `implements Codec, JsonCodec`** (jackson/jackson3/gson/moshi/fastjson2 各 L27); jackson-jr 未实现 | 已修 |
| 16 | **负面空间失准** | "不处理请求体压缩" — 源码: DefaultClient 请求侧 GZIPOutputStream (L210-211) + 响应侧 GZIPInputStream (L125-129); 准确: **编解码层不压缩, 传输层按 Content-Encoding 头处理** | 已修 |
| 17 | **死链** | 大纲引出 `[[F-7-表单]]` — PLAN 二轮 REVIEW 已把 form 并入 F-4, F-7 域不存在 | 已修 |
| 18 | 锚点残留 | DefaultErrorDecoder errorStatus (L18) — 实为 import 行, 调用点在 **L47** | 已修 |
| 19 | 验证通过 | 负面空间其余 5 条 (零 JSON 依赖 core/test scope/gzip 在 Client/StreamDecoder 独立/无缓存) 全部与源码一致 | 通过 |
| 20 | 验证通过 | 前向引用清理后无死链 (F-1 引出/F-3/F-5 前置) | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | Encoder 接口实际 **L69-82** (大纲写 L11-27 — 那是 Javadoc 示例区) | 已修 |
| 2 | 锚点漂移 | Decoder 接口实际 **L70-91** (大纲写 L11-27) | 已修 |
| 3 | 锚点漂移 | ErrorDecoder 接口实际 **L61-77** (大纲写 L11-34) | 已修 |
| 4 | 锚点漂移 | DefaultEncoder 实际 **L23-34** (大纲写 L11-17) | 已修 |
| 5 | 锚点漂移 | DefaultDecoder **L23-28**; StringDecoder **L25-36** (大纲写 L11-18/L11-21) | 已修 |
| 6 | 锚点漂移 | DefaultErrorDecoder **L27-42**; errorStatus 调用 L43; maxBody 构造 L31-41 | 已修 |
| 7 | 锚点漂移 | RetryAfterDecoder 实际 **L97-131**; 秒数匹配 L124-128; RFC 解析 L129; 容错 L130-131 (大纲写 L44-79 严重偏移) | 已修 |
| 8 | 锚点补强 | JsonCodec 族 L21-23; FeignException FeignClientException L313 + errorStatus L196; AnnotationErrorDecoder L32-48 + generateErrorHandlerMapFromApi L79 | 已修 |
| 9 | 验证通过 | RetryAfterDecoderTest L38-60 (四场景) / retryAfterHeaderThrowsRetryableException L134 — 与大纲一致 | 通过 |

## 锚点密度统计

- file:line 锚点数: **35+** (🟡B 标准 ≥4 — 大幅超出)
- 全部锚点逐条 sed/grep 重验

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (不内置 JSON/不压缩/不流式/不缓存/不自动适配/不校验对称性)
- [x] 每条有对照物 (Jackson/Spring HttpMessageConverter/StreamDecoder)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (String/byte[] 变 body/404 不抛/Retry-After 重试/注解错误码)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (Encoder 出口 → Decoder 入口 → ErrorDecoder 异常面)
- [x] 边界交代: 404/204 双层语义/Retry-After 双格式/回退链/截断上限

## 方法论教训

- **Javadoc 区与接口声明区分**: 三接口的 L11-34 全是 Javadoc, 声明在 L61-91 — 行号必须落在"声明"而非"文档"
- **RetryAfter 是跨域桥**: F-3 的 retryAfter 优先语义在 F-4 源头 (ErrorDecoder 解析头) — 跨域引用要双向验证
