# W-5 HttpMessageConverter — 消息转换器链 (JSON 读写)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | HttpMessageConverter(110行)+AbstractHttpMessageConverter(345行)+StringHttpMessageConverter(169行)+ByteArrayHttpMessageConverter(74行)+MappingJackson2HttpMessageConverter(109行)+AbstractJackson2HttpMessageConverter(578行)+AbstractMessageConverterMethodArgumentResolver(434行)+AbstractMessageConverterMethodProcessor(573行)+RequestResponseBodyMethodProcessor(211行)+RequestMappingHandlerAdapter
> 基线: W-4 §3 — @RequestBody 走 readWithMessageConverters(L148) — canRead 按"目标类型+Content-Type"双维度匹配 — 本域展开转换器内部 (接口契约→模板方法→Jackson 集成→写响应协商)

---

## §0.8

- 🟡 Working，1篇 — 接口四方法契约(canRead/canWrite/read/write) → AbstractHttpMessageConverter 模板(supports/readInternal/writeInternal + addDefaultHeaders) → 读链路(readWithMessageConverters: contentType 兜底 octet-stream→遍历 canRead→首个可读执行) → 写链路(writeWithMessageConverters: Accept 协商→producible 收集→兼容匹配) → MappingJackson2HttpMessageConverter(ObjectMapper 集成/泛型 JavaType) + XML 族/顶层 3 转换器(条件加载)
- 设计模式: [模式: 模板方法]—read/write 骨架固定, supports/readInternal/writeInternal 子类覆写; [模式: 策略模式]—messageConverters 链按 canRead/canWrite 匹配选择; [模式: 内容协商]—Accept/Content-Type 双向匹配

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HttpMessageConverter.java:47,56,92,107 | canRead/canWrite/read/write | **接口四方法**: 两个判断(canRead/canWrite)+两个动作(read/write) — 判断用 Class+MediaType 双参数"目标类型+媒体类型"双维度 | High |
| AbstractHttpMessageConverter.java:134 | canRead() | **模板 canRead**: supports(clazz) && canRead(mediaType) — 类型维度(子类 supports)+媒体维度(supportedMediaTypes.includes) 双重校验 | High |
| AbstractHttpMessageConverter.java:166,178 | canWrite() | **模板 canWrite**: supports(clazz) && canWrite(mediaType) — mediaType null 或 ALL 直接 true; isCompatibleWith(兼容而非包含, 写方向宽) | High |
| AbstractHttpMessageConverter.java:195,206 | read()/write() | **final 模板方法**: read→readInternal; write→addDefaultHeaders→writeInternal(+flush; StreamingHttpOutputMessage 走 Body.writeTo 延迟写) | High |
| AbstractHttpMessageConverter.java:246,285,298 | addDefaultHeaders/getDefaultContentType/getContentLength | **默认头**: Content-Type 缺失→getDefaultContentType(取 supportedMediaTypes[0])+charset 附加; Content-Length→getContentLength(子类可覆写) | High |
| AbstractHttpMessageConverter.java:322,332,342 | supports/readInternal/writeInternal | **三个抽象点**: 子类只需实现"类型判定+读写内部实现" — String/ByteArray/Jackson 差异全部收敛在这三处 | High |
| StringHttpMessageConverter.java:51,89,94,122 | supports/readInternal/writeInternal | **String 实现**: supports 仅 String.class; DEFAULT_CHARSET=ISO_8859_1; readInternal 按 Content-Length/charset 解码; writeInternal 按 charset 编码写 | High |
| xml/ 包家族 | 条件加载 | **XML 转换器族**: Jaxb2RootElementHttpMessageConverter(JAXB)/MappingJackson2XmlHttpMessageConverter(Jackson-XML)/MarshallingHttpMessageConverter — 与 JSON 同走 AbstractHttpMessageConverter 模板, 由 classpath 依赖决定是否注册 | Medium |
| AbstractMessageConverterMethodArgumentResolver.java:148 | readWithMessageConverters() | **读链路**: contentType=headers.getContentType → L169 缺失→APPLICATION_OCTET_STREAM → EmptyBodyCheckingHttpInputMessage 包装 → L179 遍历 messageConverters → L183 Generic.canRead(targetType,contextClass,contentType)/L191 Smart.canRead/L195 base.canRead → 首个可读: RequestBodyAdvice.beforeBodyRead → read → afterBodyRead → break | High |
| AbstractMessageConverterMethodArgumentResolver.java:235 | 无转换器 | **读失败**: body==NO_VALUE → 有方法体抛 HttpMediaTypeNotSupportedException(contentType, 支持的媒体类型列表); 空体/无 Content-Type 返回 null | High |
| AbstractMessageConverterMethodProcessor.java:208 | writeWithMessageConverters() | **写链路**: CharSequence→String; L247-253 响应已预设 Content-Type(ResponseEntity/显式设置)→直接用 → 否则 Accept 协商: getAcceptableMediaTypes(ContentNegotiationManager 解析 Accept 头) → getProducibleMediaTypes(produces 属性 or 遍历 canWrite 收集 supportedMediaTypes or [ALL]) → determineCompatibleMediaTypes 双重循环 isCompatibleWith → sortBySpecificity 最具体优先 → 遍历转换器 canWrite 匹配 → beforeBodyWrite(@ResponseBodyAdvice) → write | High |
| AbstractMessageConverterMethodProcessor.java:449 | getAcceptableMediaTypes() | **Accept 解析**: contentNegotiationManager.resolveMediaTypes(ServletWebRequest) — 策略可扩展(默认 Accept 头, 可配参数/路径后缀协商) | High |
| AbstractMessageConverterMethodProcessor.java:421 | getProducibleMediaTypes() | **可产出收集**: PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE(@RequestMapping produces)→空则遍历转换器 canWrite(valueClass,null) 收集 getSupportedMediaTypes → 全空→[ALL] | High |
| MappingJackson2HttpMessageConverter.java:62,71,72 | 构造器 | **Jackson 集成**: 默认构造 Jackson2ObjectMapperBuilder.json().build(); 自定义 ObjectMapper 构造覆盖 — L72 super 支持 APPLICATION_JSON + application/*+json | High |
| AbstractJackson2HttpMessageConverter.java:253,272 | canRead(Type)/canWrite() | **Jackson 判定**: canRead(mediaType)→getJavaType(泛型)→selectObjectMapper→objectMapper.canDeserialize(javaType); canWrite 增加 charset 校验+canSerialize — 用 Jackson 自己的能力判定而非仅类型匹配 | High |
| AbstractJackson2HttpMessageConverter.java:360,442,536 | readInternal/writeInternal/getJavaType | **Jackson 读写**: readJavaType→ObjectMapper.reader().forType(javaType).readValue; writeInternal→getJsonEncoding(L545, 默认 UTF8)→JsonGenerator→writePrefix(jsonPrefix)→serialize; getJavaType=constructType(GenericTypeResolver.resolveType) — 泛型 List<User> 不丢失 | High |
| RequestResponseBodyMethodProcessor.java:190,134 | handleReturnValue/supportsReturnType | **@ResponseBody 出口**: supportsReturnType=类/方法级 @ResponseBody; handleReturnValue→setRequestHandled(true)(不查视图)→writeWithMessageConverters | High |
| RequestMappingHandlerAdapter.java:616,620,621,623 | initMessageConverters | **默认转换器**: messageConverters 非空(用户配置过)→跳过; 默认 3 个: ByteArray+String+AllEncompassingForm(构造器按 classpath 条件 addPartConverter Jackson/JAXB, 仅服务 multipart part) — 顶层无 JSON 转换器: 纯 MVC 下 @RequestBody JSON 报 415, Boot 自动装配补顶层 Jackson | High |
| AbstractMessageConverterMethodProcessor.java:335 | beforeBodyWrite | **@ResponseBodyAdvice 钩子**: 写前回调(加密/包装/统一响应) — 与读链路 beforeBodyRead/afterBodyRead 对称 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+模板基类+2个简单实现+Jackson 家族+2个处理器链约 2000 行 — 但骨架只有一条: "接口四方法 → 模板三个抽象点 → 读/写两条处理器链 → Jackson 一个实现"。1篇 (~49行) 按"契约→模板→链路→集成"线性展开; 若分 2 篇则 Jackson 泛型细节与写协商被割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | AbstractHttpMessageConverter 模板 (canRead/canWrite 双维度 + final read/write + 三抽象点) | 🔴 | **为什么🔴**: 所有转换器的骨架 — String/ByteArray/Jackson 差异全收敛在 supports/readInternal/writeInternal — 理解"类型+媒体"双维度判定是转换器匹配核心 |
| P1-2 | readWithMessageConverters 读链路 (contentType 兜底 octet-stream→遍历 canRead→advice 钩子) | 🔴 | **为什么🔴**: W-4 §3 @RequestBody 的实现细节展开 — Content-Type 缺失兜底与"首个可读即用"是反序列化入口的完整语义 |
| P1-3 | writeWithMessageConverters 写链路 (Accept 协商→producible 收集→兼容匹配→最具体) | 🔴 | **为什么🔴**: @ResponseBody 写出 + 406 错误来源 — 内容协商(Accept 头 vs produces/转换器支持)是 HTTP 语义最复杂的部分 |
| P2-1 | 接口四方法契约 (canRead/canWrite/read/write) | 🟡 | **为什么🟡**: 自定义转换器的扩展入口 — W-4 §3 的 canRead 调用点在此展开 — "判断与动作分离"的接口设计 |
| P2-2 | MappingJackson2HttpMessageConverter (ObjectMapper 构建/泛型 JavaType/canDeserialize) | 🟡 | **为什么🟡**: JSON 是默认场景但只是转换器族的一员 — 泛型解析(getJavaType)与 ObjectMapper 定制是可复用知识点 |
| P3-1 | 默认转换器 (RequestMappingHandlerAdapter.initMessageConverters 顶层 3 个) | 🟢 | **为什么🟢**: 纯配置事实 — 但解释"为什么 Spring MVC 单独跑不支持 JSON"(AllEncompassingForm 的 part 仅服务 multipart, Boot 自动装配才补顶层 Jackson) |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **转换器本体** (接口四方法 + 模板基类 + String/ByteArray 实现) | 🔴 | 一切转换器的骨架 — 模板三个抽象点决定了子类 90% 的写法 |
| B | **读/写处理器链** (readWithMessageConverters + writeWithMessageConverters) | 🔴 | @RequestBody/@ResponseBody 的完整链路 — 与 W-4 直接衔接 — 写方向有内容协商是 Web API 必知 |
| C | **Jackson 集成** (MappingJackson2 + ObjectMapper + 泛型) | 🟡 | 最常见的生产实现 — 但机制上只是"模板的一个子类+ObjectMapper 委托" |
| D | **默认配置与扩展** (顶层 3 转换器 + AllEncompassingForm 条件 part + @ResponseBodyAdvice) | 🟢 | 配置事实 + 扩展点 |

> **Cluster A (§1)**: 接口四方法 + AbstractHttpMessageConverter 模板(双维度判定 + final 读写 + 三抽象点) + String/ByteArray 两个简单实现对照
> **Cluster B (§2)**: 读链路(readWithMessageConverters 兜底/遍历/advice) + 写链路(Accept 协商/producible/兼容匹配/advice) + 顶层 3 转换器(AllEncompassingForm 条件 part)与 Boot 扩充
> **Cluster C (§3)**: MappingJackson2HttpMessageConverter — ObjectMapper 构建/getJavaType 泛型/readValue+serialize 实际执行 — @ResponseBody 端到端示例收束

→ 引出 W-6: ViewResolver — 非 @ResponseBody 返回值的视图解析(InternalResourceViewResolver/ContentNegotiatingViewResolver) — 与消息转换器的"内容协商"对照

(End of file - total 60 lines)
