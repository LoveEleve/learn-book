# W-4 HandlerMethodArgumentResolver — 参数绑定三机制 (@PathVariable/@RequestParam/@RequestBody)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | HandlerMethodArgumentResolver(63行)+AbstractNamedValueMethodArgumentResolver(366行)+PathVariableMethodArgumentResolver(161行)+RequestParamMethodArgumentResolver(291行)+RequestResponseBodyMethodProcessor(211行)+AbstractMessageConverterMethodArgumentResolver
> 基线: W-3 RequestMappingHandlerAdapter — getMethodArgumentValues 遍历 methodParameters → resolvers.supportsParameter → resolveArgument — 本域展开三种最常用 resolver 的内部机制

---

## §0.8

- 🟡 Working，1篇 — 接口双方法契约(supportsParameter/resolveArgument) → AbstractNamedValueMethodArgumentResolver 模板(getNamedValueInfo→resolveName→defaultValue→convertIfNecessary) → @PathVariable(从 URI_TEMPLATE_VARIABLES_ATTRIBUTE 取) → @RequestParam(从 query/form/multipart 取) → @RequestBody(readWithMessageConverters→校验)
- 设计模式: [模式: 模板方法]—AbstractNamedValue 骨架固定, resolveName/createNamedValueInfo 子类覆写; [模式: 策略模式]—messageConverters 链选择

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HandlerMethodArgumentResolver.java:42,60 | supportsParameter/resolveArgument | **接口双方法**: supportsParameter 判断"归谁管" → resolveArgument 真正解析(parameter/mavContainer/webRequest/binderFactory 四参) | High |
| AbstractNamedValueMethodArgumentResolver.java:104 | resolveArgument() | **模板方法**: getNamedValueInfo(缓存) → resolveEmbeddedValuesAndExpressions(name) → resolveName(子类) → null→defaultValue/required→handleMissingValue → convertIfNecessary(类型转换) → handleResolvedValue | High |
| AbstractNamedValueMethodArgumentResolver.java:157 | getNamedValueInfo() | **NamedValueInfo 缓存**: ConcurrentHashMap(256) — 每个参数只解析一次注解属性 | High |
| AbstractNamedValueMethodArgumentResolver.java:173 | createNamedValueInfo() | **抽象方法**: 子类读取自己的注解(@PathVariable/@RequestParam) → NamedValueInfo(name, required, defaultValue) | High |
| AbstractNamedValueMethodArgumentResolver.java:220 | resolveName() | **抽象方法**: 从请求中按名字取原始值 — @PathVariable 从 URI 模板变量 / @RequestParam 从参数值 | High |
| PathVariableMethodArgumentResolver.java:72 | supportsParameter() | **@PathVariable 匹配**: 有注解→true; Map 参数需显式 value() | High |
| PathVariableMethodArgumentResolver.java:93 | resolveName() | **URI 模板变量**: request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) → Map<String,String> → get(name) — 由 W-2 的 RequestMappingInfoHandlerMapping 写入 | High |
| PathVariableMethodArgumentResolver.java:100 | handleMissingValue() | **缺失处理**: 抛 MissingPathVariableException(name, parameter) | High |
| RequestParamMethodArgumentResolver.java:127 | supportsParameter() | **@RequestParam 匹配**: 注解→true(Map 需 name); 无注解→multipart 参数 true / useDefaultResolution 且简单类型 true | High |
| RequestParamMethodArgumentResolver.java:162 | resolveName() | **参数获取**: multipart 优先 → getParameterValues(name) → 单值取 [0] / 多值取数组 → name+"[]" 兜底 | High |
| RequestResponseBodyMethodProcessor.java:128 | supportsParameter() | **@RequestBody 匹配**: 仅 hasParameterAnnotation(RequestBody.class) | High |
| RequestResponseBodyMethodProcessor.java:146 | resolveArgument() | **@RequestBody 解析**: readWithMessageConverters → WebDataBinder 绑定 → validateIfApplicable(@Valid) → MethodArgumentNotValidException → adaptArgumentIfNecessary | High |
| AbstractMessageConverterMethodArgumentResolver.java:148 | readWithMessageConverters() | **转换器选择**: contentType(默认 octet-stream) → 遍历 messageConverters → GenericHttpMessageConverter.canRead(targetType, contextClass, contentType) / HttpMessageConverter.canRead | High |
| RequestResponseBodyMethodProcessor.java:184 | checkRequired() | **required 检查**: @RequestBody.required() && !isOptional → 缺失抛 HttpMessageNotReadableException | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+基类+3个实现共 ~1100 行 — 核心是"参数注解 → 值解析 → 类型转换"的模板链。1篇 (~45行) 覆盖: 接口契约 → 基类模板 → 三种具体机制(@PathVariable/@RequestParam 同走 AbstractNamedValue 模板, @RequestBody 走转换器链); W-5 再展开 HttpMessageConverter 细节。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | AbstractNamedValueMethodArgumentResolver 模板链 (getNamedValueInfo→resolveName→defaultValue→convertIfNecessary) | 🔴 | **为什么🔴**: @PathVariable/@RequestParam 共用的骨架 — 理解缺省值/必填/类型转换的统一处理是参数绑定核心 |
| P1-2 | @PathVariable 解析 (URI_TEMPLATE_VARIABLES_ATTRIBUTE 读取 + MissingPathVariableException) | 🔴 | **为什么🔴**: REST 风格 URL 参数绑定的唯一机制 — 与 W-2 的模板变量匹配闭环 |
| P1-3 | @RequestBody 解析 (readWithMessageConverters→校验→MethodArgumentNotValidException) | 🔴 | **为什么🔴**: JSON/XML 请求体反序列化的入口 — 结合校验是 Web API 开发核心 |
| P2-1 | HandlerMethodArgumentResolver 接口双方法契约 (supportsParameter/resolveArgument) | 🟡 | **为什么🟡**: 自定义 resolver 的扩展点 — 理解"谁解析什么参数"的分派机制 |
| P2-2 | @RequestParam 解析 (multipart→参数值→name+"[]" 兜底) | 🟡 | **为什么🟡**: query/form 绑定的主机制 — 与 @PathVariable 对照理解 |
| P3-1 | @RequestBody required 检查 (checkRequired→HttpMessageNotReadableException) | 🟢 | **为什么🟢**: 缺失体/空体的保护机制 — 属于边界处理 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **命名值解析模板** (@PathVariable/@RequestParam 共用 AbstractNamedValue 链) | 🔴 | 两个最常用参数注解走同一模板 — 模板的每一步(名字解析/缺省值/必填/类型转换)都是理解参数绑定的关键 |
| B | **消息体解析** (@RequestBody 转换器链 + 校验) | 🔴 | 与命名值完全不同 — 走 HttpMessageConverter 策略链 — REST API 的入口 |
| C | **接口契约与扩展** (双方法 + 自定义 resolver) | 🟡 | 理解"新增参数类型只需注册新 resolver"的扩展机制(W-3 §3 已埋点) |

> **Cluster A (§1-§2)**: 接口契约 + AbstractNamedValue 模板 + @PathVariable/@RequestParam 两种命名值机制(取值差异在 §2)
> **Cluster B (§3 前半)**: @RequestBody 消息体解析 — 转换器链 + 校验 + required 检查
> **Cluster C (§3 后半)**: 三机制对比 + 自定义 resolver 扩展

→ 引出 W-5: HttpMessageConverter — JSON/XML 序列化 — messageConverters 链的 canRead/canWrite 机制与 Jackson 集成
