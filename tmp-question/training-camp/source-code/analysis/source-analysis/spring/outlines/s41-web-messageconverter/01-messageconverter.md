# W-5 HttpMessageConverter — 消息转换器链 (JSON 读写)

> 依赖 W-4 HandlerMethodArgumentResolver | 🟡 Working | 6 KP | [模式: 模板方法 + 策略模式 + 内容协商]

**读者处境**: W-4 §3 中 @RequestBody 调了 `readWithMessageConverters` — 里面"遍历 messageConverters → canRead → read"每一步内部怎么工作？canRead 的"目标类型+Content-Type"双维度怎么判定？@ResponseBody 写响应时 Content-Type 怎么协商？JSON 转换器为什么能处理 `List<User>` 泛型？

### 1. 接口四方法 + AbstractHttpMessageConverter 模板 — 骨架与三变化点

场景: `@PostMapping("/api/users")` 收到 JSON 请求体、返回对象 — Spring 需要知道"谁负责读/写这种类型+媒体"。接口只定义 4 个方法: 2 个判断(canRead/canWrite) + 2 个动作(read/write) — 具体实现(String/ByteArray/Jackson)却各不相同。

源码路径:
- `HttpMessageConverter.java:47,56,92,107` — **接口四方法**: canRead/canWrite 各带 (Class, MediaType) 双参数 → 判断"目标类型+媒体类型"双维度; read/write 执行动作
- `AbstractHttpMessageConverter.java:134,166` — **双维度判定**: `supports(clazz)`(子类判类型) && `canRead/canWrite(mediaType)`(判媒体: canRead 用 supportedMediaTypes.includes, canWrite 用 isCompatibleWith 且 null/`*/*` 直通)
- `AbstractHttpMessageConverter.java:195,206` — **final read/write 模板**: read→readInternal(L332 抽象); write→addDefaultHeaders(L246)→writeInternal(L342 抽象; StreamingHttpOutputMessage 走 Body.writeTo 延迟写, 普通流写完 flush)
- `AbstractHttpMessageConverter.java:246,285,298` — **addDefaultHeaders**: Content-Type 缺失→getDefaultContentType(L285 取 supportedMediaTypes[0])+charset 附加; Content-Length→getContentLength(L298)
- `StringHttpMessageConverter.java:51,89,94,122` — **String 实现**: supports 仅 String.class; readInternal 按 Content-Length+charset 解码; writeInternal 按 charset 编码写; `ByteArrayHttpMessageConverter.java:49,54` — supports 仅 byte[], readInternal→readNBytes(length)/readAllBytes; 同族: xml/ 包 Jaxb2RootElement/MappingJackson2Xml(按 classpath 条件加载)

关键设计: **Why 接口只留 4 个方法、骨架放抽象类？** 所有转换器的差异收敛为三个抽象点 — supports/readInternal/writeInternal — 判定逻辑(双维度)、默认头、缓冲 flush 全由模板固定。子类只写 20% 差异; 新增格式(如 XML)只需继承 AbstractHttpMessageConverter 实现三处。[模式: 模板方法]

数据流: @RequestBody UserDto → 遍历转换器 → String.canRead(UserDto, json) → 模板 L134: supports(UserDto)=false(仅 String.class)→跳过 → Jackson.canRead(UserDto, json) → L247→L253: canRead(mediaType)(supported 含 json, includes=true) → canDeserialize(UserDto)=true → 选中 → read → L195 模板 → readInternal(L360) → ObjectMapper 反序列化 → UserDto。写方向: write(userDto, json, outputMessage) → L206 addDefaultHeaders(已有 Content-Type 则跳过) → writeInternal → serialize → flush。

### 2. 读链路 vs 写链路 — 方向的不对称性

场景: 客户端 POST JSON 体, Spring 按**请求的 Content-Type** 找转换器; 返回响应, Spring 按**客户端 Accept 头**定 Content-Type — 读方向"客户端已声明类型", 写方向"服务端猜客户端要什么" — 两方向逻辑完全不同。

源码路径:
- `AbstractMessageConverterMethodArgumentResolver.java:148` — **readWithMessageConverters**: L160-166 contentType 非法→HttpMediaTypeNotSupportedException; L167-169 缺失→APPLICATION_OCTET_STREAM 兜底; L179 遍历 messageConverters → L183 Generic.canRead(targetType, contextClass, contentType) / L191 Smart.canRead / L195 base.canRead(targetClass, contentType) → 首个可读: L201 beforeBodyRead(RequestBodyAdvice)→read→afterBodyRead→break; 全不可读且 body 非空→L235 抛 HttpMediaTypeNotSupportedException
- `AbstractMessageConverterMethodProcessor.java:208` — **writeWithMessageConverters**: L247-253 响应 Content-Type 已预设(ResponseEntity)→直接用; 否则协商: L259 getAcceptableMediaTypes(ContentNegotiationManager 解析 Accept 头) → L272 getProducibleMediaTypes(produces 属性→空则遍历 canWrite(valueClass,null) 收集 supportedMediaTypes→全空则[ALL]) → L278 determineCompatibleMediaTypes(双重循环 isCompatibleWith) → L295 sortBySpecificity → L297-305 首个 concrete 选中, 仅 `application/*` 泛型→octet-stream → L318-346 遍历 canWrite→beforeBodyWrite(advice)→write; 无匹配→L358-367 抛 HttpMediaTypeNotAcceptableException(406)
- `RequestMappingHandlerAdapter.java:616,620,621,623` — **initMessageConverters**: 用户配置过则跳过(L617); 默认 3 个: ByteArray+String+AllEncompassingForm(表单, 构造器按 classpath 条件 addPartConverter Jackson/JAXB — **仅服务 multipart part**) — **顶层无 JSON 转换器**: 纯 MVC 下 @RequestBody JSON 抛 HttpMediaTypeNotSupportedException, Boot 自动装配才补顶层 Jackson

关键设计: **Why 读"直接按 Content-Type 匹配"、写"协商后再匹配"？** 读: Content-Type 是请求方写死的事实, 只需找"支持该类型+媒体"的转换器; 写: Accept 表达"偏好", 服务端要算 producible(produces 声明+转换器能力)与 acceptable 的**交集**, 按特异性排序取最具体 — 交集空且 body 非空→406。这是 HTTP 内容协商的标准语义, Spring 收敛在 writeWithMessageConverters 一处。[模式: 内容协商]

数据流: 读: POST /users Content-Type:application/json → readWithMessageConverters(L148) → contentType=application/json → String.canRead(UserDto, json)→supports(UserDto)=false → Jackson.canRead→true → beforeBodyRead→read→UserDto。写: Accept:application/json → 无预设 Content-Type → L259 acceptable=[json] → L272 producible=[json, *+json](Jackson supported) → L278 交集=[json](getMostSpecificMediaType 取更具体) → L295 排序 → L297-305 选中 application/json → 遍历转换器: Jackson.canWrite→true → beforeBodyWrite→write→响应 Content-Type:application/json。Accept:text/html→交集空→L290 HttpMediaTypeNotAcceptableException→406。

### 3. MappingJackson2HttpMessageConverter — Jackson 集成与泛型

场景: `@RestController @GetMapping("/users") public List<User> list()` 返回 JSON 数组 — 转换器怎么知道目标类型是 `List<User>` 而非 `List<Object>`？ObjectMapper 从哪来、怎么自定义？

源码路径:
- `MappingJackson2HttpMessageConverter.java:48,62,71,72` — **类与构造**: extends AbstractJackson2HttpMessageConverter; 默认构造=Jackson2ObjectMapperBuilder.json().build(); 自定义 ObjectMapper 构造可覆盖; L72 super 支持 APPLICATION_JSON + `application/*+json`
- `AbstractJackson2HttpMessageConverter.java:253,272` — **Jackson 判定**: canRead(Type, contextClass, mediaType): canRead(mediaType)→getJavaType→selectObjectMapper→`objectMapper.canDeserialize(javaType)`; canWrite 加 charset 校验+`canSerialize` — 用 Jackson 自身能力判定
- `AbstractJackson2HttpMessageConverter.java:536,360,442` — **泛型与读写**: getJavaType=constructType(GenericTypeResolver.resolveType(type, contextClass)) — 泛型不丢失; readJavaType→ObjectMapper.reader().forType(javaType).readValue; writeInternal→getJsonEncoding(L545, 默认 UTF8)→JsonGenerator→writePrefix(防劫持前缀)→serialize; 定制: setObjectMapper(L142)/registerObjectMappersForType(L171)
- `RequestResponseBodyMethodProcessor.java:190,134` — **@ResponseBody 出口**: supportsReturnType=类/方法级 @ResponseBody; handleReturnValue→setRequestHandled(true)(跳过视图解析)→writeWithMessageConverters

关键设计: **Why Jackson 判定交给 canDeserialize/canSerialize 而非 supports()？** AbstractJackson2 是泛化转换器 — supports(clazz) 恒 true(AbstractGenericHttpMessageConverter.java:77 覆写), 真正判定由 ObjectMapper 完成: canDeserialize(canSerialize) 检查"目标类型能否被 Jackson 处理", 且 getJavaType 先把方法签名泛型解析成 JavaType — 泛型信息在判定与读写全链路保留。[模式: 策略 — 委托 ObjectMapper]

数据流: @GetMapping("/users") List<User> → handleReturnValue(L190) → writeWithMessageConverters → targetType=List<User>(GenericTypeResolver 解析) → 协商选中 application/json → Jackson.canWrite(List<User>, json): canWrite(mediaType)+canSerialize → write(body, List<User>, json, outputMessage) → writeInternal(L442): getJsonEncoding(UTF8)→JsonGenerator→writePrefix→objectMapper.writer()(容器 List 再 .forType(javaType))→writeValue(generator, list) → 响应 `[{"id":1,"name":"Alice"}]`。读方向对称: 若 @RequestBody List<User> → reader().forType(List<User> 的 JavaType).readValue。

→ 引出 W-6: ViewResolver — 无 @ResponseBody 的返回值走视图解析(InternalResourceViewResolver 逻辑视图→JSP) — 与消息转换器的"内容协商"对照: 同是"返回值→HTTP 响应"的两条分岔路。
