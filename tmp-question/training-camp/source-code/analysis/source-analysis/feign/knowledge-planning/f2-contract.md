# F-2 契约解析 — 知识规划 (KP)

> 域级: 🔴 A | 模块: Contract (254) + DeclarativeContract (267) + DefaultContract (153) + MethodInfoResolver (23) + MethodInfo (53) + MethodMetadata (269) + 注解族 (RequestLine 95/Param 60/Headers 83/HeaderMap 67/QueryMap 105/Body 45/CollectionFormat 90/FeignIgnore 26) + Types (531)
> 日期: 2026-08-15 | 版本: 13.14-SNAPSHOT

## 一、机制提取 (逐源)

### M1 Contract 接口 + BaseContract 模板方法 (254)
- 唯一入口 parseAndValidateMetadata(Class) (L40); **BaseContract 实现** (L49-80): 校验 (拒绝泛型类型参数 L50-53/仅单继承 L54-57) → 遍历 getMethods (L59) → 过滤 Object/static/default/@FeignIgnore (L60-65) → 逐方法委托 (L66)
- **configKey 去重 + 返回类型协变合并** (L67-77): Types.resolveReturnType (L71) — javac 泛型桥接方法处理
- **逐方法解析核心** (L91-175): 初始化 metadata (Types.resolve 返回类型 L95/configKey L96) → 类级注解先父后自身 (L101-104) → 方法级 (L106-108) → isIgnored 短路 (L109-111) → HTTP 动词缺失校验 (L112-116) → **参数循环** (L117-159): 注解分发 (L125)/Kotlin Continuation 忽略 (L132-134)/URI 参数 urlIndex (L136-137)/**未消费参数推断为 body** (L138-158, 依赖 isAlreadyProcessed) → Map key String 校验 (L161-172)
- 三个抽象钩子 processAnnotationOnClass/OnMethod/OnParameters (L219/226/241)

### M2 DeclarativeContract 声明式架构 (267)
- 三注册表: class/method List + parameter Map<注解类型, Processor> (L32-35)
- **parseAndValidateMetadata final 化** (L38-41): 强制子类走注册表, 不能覆写入口
- **未识别注解 warning** (L68-82 类/L102-107 方法/L141-155 参数): 13.x 从静默忽略改显式警告 — errorMessageOnMixedContracts 测试实证
- **参数处理固定返回 false** (L157): 消费判定交给 MethodMetadata.isAlreadyProcessed (MethodMetadata.java:212) — 与旧版 true 语义的关键差异
- 注册 API: registerClassAnnotation 双重载 (L166-181, Predicate 版按值匹配) / registerMethodAnnotation (L189-204) / registerParameterAnnotation (L212-216)
- GuardedAnnotationProcessor (L244-266) 谓词包装

### M3 DefaultContract 默认注解处理器 (153)
- 类级 @Headers (L35-47): toMap 解析 "Name: value" → 合并到现有 headers (L44-46 先取旧值再清空重写 — 父接口类级头生效)
- 方法级 @RequestLine (L48-69): REQUEST_LINE_PATTERN = ^([A-Z]+)[ ]*(.*)$ (L32) → 动词+URI 解析 (L57-66 非动词抛 "didn't start with an HTTP verb"); decodeSlash (L67); collectionFormat (L68)
- 方法级 @Body (L70-83): 含 `{` → bodyTemplate() 模板 / 否则 body() 字面量 (L78-82)
- **参数级 @Param** (L94-120): 名解析 (value 空回退 parameter.getName() 需 -parameters, L100-104); 空名抛错带 Hint (L105-111); indexToName (L112); expander 类 (L113-116); **未出现在 URI 模板的参加入 formParams** (L117-119)
- @QueryMap (L121-129): 重复检查 + queryMapIndex + mapEncoder 实例; @HeaderMap (L130-137) 同理
- toMap (L140-152): "Name: value" → LinkedHashMap, 同名合并

### M4 MethodInfo/MethodInfoResolver (53+23)
- @Experimental; resolve(Class, Method) (MethodInfoResolver.java:20-22)
- **CompletableFuture<T> → asyncReturnType=true + 底层类型 T** (MethodInfo.java:36-39)
- 消费: AsynchronousMethodHandler.java:319 (代理调用时解析) / L72 isAsyncReturnType / L232 解码类型; Capability 可装饰 enrich (Capability.java:144); Kotlin 协程特化 (KotlinMethodInfo.java:26)

### M5 MethodMetadata (269)
- 字段: configKey (L28)/returnType transient (L29)/urlIndex (L30)/bodyIndex+bodyType (L31,36)/headerMapIndex (L32)/queryMapIndex+encoder (L33-34)/alwaysEncodeBody (L35)/**template 核心** (L37)/formParams (L38)/indexToName (L39)/indexToExpander (L41,44)/indexToEncoded (L43)/parameterToIgnore BitSet (L45)/bodyRequired (L47)/warnings 13.x (L50)
- **isAlreadyProcessed(index)** (L212-222): 已消费索引判定 — BaseContract 参数循环的 body 推断依据
- ignoreMethod/isIgnored (L224-230); ignoreParamater (L185-188)

### M6 注解属性族
- @RequestLine: value() 动词+URI (L52)/decodeSlash 默认 true (L75)/collectionFormat 默认 EXPLODED (L94)
- @Param: value() 默认 "" (L32)/expander() (L35)/encoded() 弃用 (L45); 内嵌 Expander 接口 (L47-51)
- @Headers: String[] "Name: value" 支持 {var} (L82); @HeaderMap marker 无属性 (L67)
- @QueryMap: encoded 弃用 (L80)/MapEncoder 枚举 BEAN/FIELD/DEFAULT (L89-104)
- @Body: value() (L44); @FeignIgnore marker (L26)

### M7 Types 泛型解析 (531, 移植自 Retrofit)
- getRawType (L44-81)/equals 结构相等 (L84-130)/getGenericSupertype 继承链 (L137-169)/**resolve 消解循环** (L207-277: TypeVariable→resolveTypeVariable L210-215/ParameterizedType 递归 L233-253)/resolveReturnType 合并 (L312-330)/resolveLastTypeParameter (L344-361)
- 调用点: Contract.java:95 (返回)/L156 (body)/MethodInfo.java:34 (异步剥离)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M1 参数循环 + body 推断 | P1 | 契约解析核心 |
| M2 声明式注册表 + warning | P1 | 13.x 架构特征 |
| M3 @Param formParams 推断 | P1 | 使用面核心 |
| M5 isAlreadyProcessed | P1 | 消费判定机制 |
| M4 异步剥离 | P2 | 13.x 新抽象 |
| M6 注解族 | P2 | 属性面 |
| M7 Types | P2 | 工具面 |

## 三、负面空间

- **不做注解继承扫描**: 只处理直接注解 + 父接口类级 (不递归扫描父类方法)
- **不做 JAX-RS/Spring 默认支持**: DefaultContract 仅 Feign 原生注解 (jaxrs/spring 是独立模块)
- **不做运行时注解变更**: 契约解析一次, 运行时不可改
- **不做 method 级泛型擦除保留**: 部分泛型信息 transient
- **不做契约缓存**: 每次 newInstance 重新解析 (ReflectiveFeign)
