# F-2 契约解析 — 注解界的"翻译官": 从 @RequestLine 到 MethodMetadata

> 前置: [[F-6-模板引擎]] (MethodMetadata.template 载体) | 引出: [[F-3-代理与调用链]] (元数据消费) + [[F-4-编解码]] (后继域) | 对照: Retrofit 注解解析 + JAX-RS
> 🔴 A | 7 KP | [模式: 模板方法 + 注册表分发]
> Pass 2 闭环: q1(参数循环) q2(注册表) q3(formParams) q4(异步剥离)

**读者处境**: 接口里写了 `@RequestLine("GET /users/{id}")` + `@Param("id") Long id`, 调用时 Feign 怎么知道 id 对应 {id}? 多出来的参数怎么变成 body? 继承接口的泛型返回类型怎么解析?

### 1. BaseContract — 一个"责任划分"的骨架

场景: 所有注解 (Feign/JAX-RS/Spring) 的解析流程为什么一样?
源码路径:
- 唯一入口 parseAndValidateMetadata(Class) (Contract.java:40); BaseContract 实现 (L49-80): 校验 (拒绝泛型类型参数 L50-53) → 过滤 Object/static/default/@FeignIgnore (L60-65)
- **configKey 去重 + 协变合并** (L67-77): Types.resolveReturnType (L71) — javac 泛型桥接方法处理
- 参数循环 (L117-159): **注解标记 + URI 参数 + 未消费推断 body** (L138-158)
关键设计 (q1): **参数与 body 的责任划分留在基类** — 注解只标记 (indexToName/queryMapIndex), 谁当 body 由基类统一推断 — 保证所有 Contract 行为一致。 [模式: 模板方法]

### 2. 参数循环 — 谁是参数, 谁是 body?

场景: 接口方法 3 个参数, 怎么分配角色?
源码路径:
- 参数注解分发 (L125) → Kotlin Continuation 忽略 (L132-134) → **URI 参数 → urlIndex** (L136-137) → 未消费参数 → body (L138-158)
- **isAlreadyProcessed 判定** (MethodMetadata.java:212-222): 已被 urlIndex/bodyIndex/indexToName 等占用的索引
- body+formParams 冲突检查 (L142-149); Map key String 校验 (L161-172)
关键设计 (q1): **角色分配是"剩余法"** — 已被注解标记的参数不再参与, 剩下的就是 body; 一个方法只能一个 body (多 body 报错)。 [模式: 剩余分配]

### 3. DeclarativeContract — 13.x 的"注册表革命"

场景: 加一种新注解支持, 老版本要覆写方法, 新版呢?
源码路径:
- 三注册表 (DeclarativeContract.java:32-35); **parseAndValidateMetadata final 化** (L38-41) — 强制走注册表
- **未识别注解 → warning** (L68-82/L102-107/L141-155): 13.x 从静默忽略改显式警告 — "注解没生效" 即时反馈
- registerClassAnnotation 双重载 (L166-181, Predicate 版按值匹配)
关键设计 (q2): **注解处理从 if/else 变声明式注册** — jaxrs/spring/graphql 模块只需 registerXxxAnnotation; warning 让"注解被忽略"不再无声。 [模式: 注册表分发]

### 4. DefaultContract — 默认注解的处理器注册

场景: @RequestLine 怎么变成 RequestTemplate?
源码路径:
- @RequestLine (DefaultContract.java:48-69): **REQUEST_LINE_PATTERN = ^([A-Z]+)[ ]*(.*)$** (L32) → 动词+URI (L57-66); decodeSlash/collectionFormat 传导 (L67-68)
- @Body (L70-83): **含 `{` → bodyTemplate() 模板 / 否则 body() 字面量** (L78-82)
- 类级 @Headers 合并 (L35-47): 父接口类级头与子接口都要生效 (L44-46 先取旧再重写)
关键设计 (q2): **注解语义的决定性差异: 模板 vs 字面量** — body 含 { 就当模板展开, 否则静态; 类级头合并保证继承链。 [模式: 语义分支]

### 5. @Param — 名字解析与 formParams 推断

场景: @Param 没写 value, 名字从哪来? 模板里没有的参数去哪了?
源码路径:
- 名解析 (L94-120): value 空 → **回退 parameter.getName() (需 -parameters 编译)** (L100-104); 空名抛错带 Hint (L105-111)
- indexToName 记录 (L112); expander 类 (L113-116)
- **未出现在 URI 模板中的名 → formParams** (L117-119): POST 表单参数推断
关键设计 (q3): **"模板里没有"是表单参数的信号** — @Param 参数若不在 URL/query 模板里, 必然是 body 表单的一部分; -parameters 回退让 13.x 少写一个 value。 [模式: 缺席推断]

### 6. MethodInfo — 异步返回类型的剥离

场景: 返回 CompletableFuture<String> 的接口, 解码器看到什么?
源码路径:
- MethodInfo (L23-44): **CompletableFuture<T> → asyncReturnType=true + 底层 T** (L36-39)
- 消费: AsynchronousMethodHandler.java:319 解析 / L232 解码类型; Capability 可装饰 (Capability.java:144); Kotlin 协程特化 (KotlinMethodInfo.java:26)
关键设计 (q4): **同步/异步/协程共享同一套元数据** — 解码器只见底层类型; 异步性抽成可插拔抽象 (13.x 新架构)。 [模式: 关注点剥离]

### 7. Types — 泛型消解的隐形引擎

场景: 继承接口 `ParameterizedBaseApi<String, Long>` 的返回类型怎么解析?
源码路径:
- getRawType (Types.java:44-81) / **resolve 消解循环** (L207-277): TypeVariable → 声明处实参 (L210-215), ParameterizedType 递归 (L233-253)
- resolveReturnType 继承合并 (L312-330); 移植自 Retrofit
- 调用点: Contract.java:95/156 + MethodInfo.java:34
关键设计 (q4): **泛型消解是契约正确性的地基** — 返回 List<String> 的泛型信息不消解, 解码器拿不到 String; 桥接方法去重靠它。 [模式: 类型消解]

## 代码类型
Architecture (元数据解析)

## 负面空间 — Feign 契约解析刻意不做的事

- **不做注解继承深度扫描**: 只直接注解 + 父接口类级头
- **不做 JAX-RS/Spring 内建**: DefaultContract 仅原生注解 (扩展靠注册表 + 独立模块)
- **不做运行时注解变更**: 一次解析, 不可热更新
- **不做契约缓存**: 每次 newInstance 重解析 (性能换简单)
- **不做参数名反射兜底**: 无 -parameters 时 @Param 空名直接报错 (带 Hint)

→ 引出: 方法参数和返回类型怎么序列化/反序列化? → F-4 编解码
