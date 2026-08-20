# OF-3 契约集成 — MVC 注解→Feign 模板的翻译官

> 前置: [[OF-2-代理创建与装配]] (Contract 组件) | 引出: [[OF-4-编解码]] [[OF-7-配置隔离]] | 对照: Feign 本体 F-2 Contract + Spring MVC 注解认知
> 🔴 A | 8 KP | [模式: 模板方法 + 注册表分发 + 组合注解]
> Pass 2 闭环: q1(注解面) q2(请求面) q3(参数面) q4(合并项)

**读者处境**: 接口里写 @GetMapping + @PathVariable, Feign 怎么知道变成 GET /users/{id}? 这篇拆 SpringMvcContract (611) + 7 处理器。

### 1. 注解面 — 类级禁止 + 方法级解析

场景: MVC 注解怎么变请求模板?
源码路径:
- **类级禁止** (L226-231): @RequestMapping 在 @FeignClient 接口上 → IllegalArgumentException
- **方法级** (L287+): 组合注解判定 (isAnnotationPresent, L293-296) → **findMergedAnnotation** (@GetMapping 等) → **HTTP method 默认 GET** (L302-306) + checkOne/checkAtMostOne (L349-355: 恰 1/至多 1 校验) → **path** (L308-317: resolve 占位符 + 补 "/" + removeTrailingSlash) → 四解析 (L326-335)
- ⚠ **4 构造器演进** (L126-168): 无参 → (processors) → (processors, conversionService) → (processors, conversionService, **decodeSlash**) → (…, **removeTrailingSlash**) — 行为参数可配
关键设计 (q1): **类级禁止防歧义 + 组合注解无缝 + GET 默认**。[模式: 注解面]

### 2. 请求面 — 四解析 + CollectionFormat

场景: produces/headers 怎么变请求头?
源码路径:
- **parseProduces** (L420-426): produces → **ACCEPT header**
- **parseConsumes** (L430-434): consumes → **CONTENT_TYPE header**
- **parseHeaders** (L438-447): "key=value" 拆分 + "!=" 排除 + resolve 占位符
- **parseParams** (L451-457): params → NameValueResolver (条件参数)
- **CollectionFormat** (L233-291): 类级+方法级 — 集合格式
关键设计 (q2): **MVC 语义直接映射请求头 + 键值拆分动态化**。[模式: 请求面]

### 3. 参数面 — 注册表 + Pageable + GET 警告

场景: 参数怎么分发? 分页/警告?
源码路径:
- **Pageable 特判** (L362-370): Pageable → **queryMapIndex** (分页对象展开, OF-4 关联; queryMapParamPresent 防冲突)
- **注册表分发** (L381-390): annotatedArgumentProcessors.get(注解) → processArgument; ⚠ **未注解参数 → ConversionService 推断 Expander** (L394-406: TypeDescriptor + canConvert + convertingExpanderFactory → indexToExpander.put — 类型转换序列化)
- ⚠ **PathVariables 处理器细节** (L50-80): name 空校验 → setParameterName → **模板变量匹配** (url/queries/headers 找 {name}) → 找不到 → **formParams.add** (变体参数兜底)
- **7 处理器** (L470-485): MatrixVariable/PathVariable/RequestParam/RequestHeader/CookieValue/QueryMap/RequestPart
- **GET 警告** (L245-260): GET+未注解参数 → warn "may result in fallback to POST at runtime" (生产防坑!)
关键设计 (q3): **注册表可扩展 + Pageable 特判 + GET 警告防降级**。[模式: 参数面]

### 4. 合并项面 — SpringQueryMap + Formatter + CollectionFormat

场景: 对象查询参数/格式化/集合格式?
源码路径:
- **SpringQueryMap** (L33-37) + **QueryMapParameterProcessor** (L34-48): POJO 展开为 query 参数
- **FeignFormatterRegistrar** (FeignClientsConfiguration L88,152-160): 格式化注册 → ConversionService → feignContract (L147)
- **CollectionFormat** 双级 (L233-291)
关键设计 (q4): **对象参数整体展开 + 格式化扩展点 + 集合格式控制**。[模式: 合并项面]

## 代码类型
Architecture (注解翻译) + Spring MVC 语义

## 负面空间 (OF-3, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不类级路径拼接 | 类级 @RequestMapping 禁止 (q1) |
| 不多 method 支持 | checkOne (q1) |
| 不 headers 多值 | TODO 单值 (q2) |
| 不自定义处理器热注册 | 构造时定 (q3) |
| 不 SpringQueryMap 嵌套展开 | 单层 (q4) |
| 不格式化自动发现 | 显式注册 (q4) |

## 结尾桥 OUTBOUND

- → [[OF-4-编解码]]: Pageable → queryMapIndex 分页编码
- → [[OF-7-配置隔离]]: FeignClientsConfiguration 组装 (ConversionService→Contract)
- → 对照: Feign 本体 F-2 Contract (BaseContract 底座) / Spring MVC @RequestMapping
