# HANDOFF — Spring Cloud OpenFeign 源码分析交接文档 (9/9 域收官)

> **日期**: 2026-08-16 | **版本**: 4.3.2 (pom.xml 实证; 与 SCC 4.3.2 同代)
> **给新 AI**: 本文是 OpenFeign 阶段的**唯一入口**。规划权威 = `OPENFEIGN-PLAN.md` (09 审计 v1)。方法论权威 = `talk-method/source-code-analysis/methodology/zh/` (01-09, **09 对既有规划保持怀疑必读**)。
> **源码**: `/data/workspace/source-code/code/spring/spring-cloud-openfeign` (openfeign-core 主源码 **87 文件 / 10018 行**; 顶层 35 类 + 9 子包)
> **定位**: 阶段 5.6 — RPC 与服务治理第六环 **Spring 对 Feign 的声明式 HTTP 封装 (@FeignClient → 代理 → Spring MVC 契约 → LoadBalancer/CircuitBreaker 装饰)**
> **任务**: 9 域执行序 **OF-1 → OF-2 → OF-3 → OF-4 → OF-5 → OF-6 → OF-7 → OF-8 → OF-9** — **9/9 全部定稿 + 深审收敛**, 可进入写作阶段

---

## §零 状态速查 (9 域)

| 域 | 目录 | 类型 | 深审 | 修复 | harness | 状态 |
|:--:|---|:--:|:--:|:--:|:--:|:--|
| OF-1 注册机制 | outlines/of1-registration | 🔴 | 四轮 | 9 | 5/5 | ✅ 收敛 |
| OF-2 代理创建与装配 | outlines/of2-proxy | 🔴 | 四轮 | 9 | 5/5 | ✅ 收敛 |
| OF-3 契约集成 | outlines/of3-contract | 🔴 | 五轮 | 8 | 5/5 | ✅ 收敛 |
| OF-4 编解码 | outlines/of4-codec | 🟡 | 五轮 | 10 | 5/5 | ✅ 收敛 |
| OF-5 负载均衡 | outlines/of5-loadbalancer | 🔴 | 五轮 | 11 | 5/5 | ✅ 收敛 |
| OF-6 熔断 | outlines/of6-circuitbreaker | 🟡 | 五轮 | 9 | 5/5 | ✅ 收敛 |
| OF-7 配置隔离 | outlines/of7-config | 🔴 | 五轮 | 10 | 5/5 | ✅ 收敛 |
| OF-8 HTTP 客户端与压缩 | outlines/of8-client | 🟡 | 五轮 | 10 | 5/5 | ✅ 收敛 |
| OF-9 动态刷新 | outlines/of9-refresh | 🟡 | 七轮 | 9 (含 2 行号) | 5/5 | ✅ 收敛 |

**每域 9 文件** (pass1-notes/pass2-q1~q4/outline/completeness-questions/temporal-trace/review-notes) + **harness/ 1 个 MiniXxx.java**。全部反写测试通过, 可进入写作阶段 (写作时行号仍须 re-grep)。

---

## §一 09 怀疑审计结论 (详细见 OPENFEIGN-PLAN.md)

**双基准**: 执行计划 OF-1~OF-9 (9 域) + `../../../issue/SpringCloudOpenfeign源码学习范围规划.md` (F-1~F-9, 5🔴+4🟡, training-camp/source-code/issue/)。

| 结论 | 内容 |
|---|---|
| 域数 | 9 域 (100% 覆盖率) |
| **新增 2 域** | OF-1 注册机制 (执行计划漏, F-1 补: FeignClientsRegistrar 503) / OF-9 动态刷新 (执行计划漏, F-9 补: Refreshable 族) |
| **修正 4 处** | OF-4 拦截器无独立域 (BasicAuth 在 feign 本体, RequestInterceptor 装配并入 OF-2) / OF-6 熔断代际 (4.3.x 面向 Spring Cloud CircuitBreaker, 非 Hystrix/Sentinel) / OF-7 扩展 (OF-9 指标并入配置面, MicrometerCapability 在 feign 本体) / OF-8 扩展 (+客户端选型面) |
| 数字穷举 | **7 处理器** (annotation/) / **14 @Bean** (FeignClientsConfiguration) / **9 组件** (configureUsingConfiguration) / **3 Builder 变体** / **2 Micrometer Capability** / **3 底层 Client** (HttpClient5/Http2/OkHttp) |
| 依赖方向 | core→feign 本体 **56 文件** (codec 18/Feign 13/Client 12/Target 11) / core→commons **17+16 文件** |
| 排除 | security (OAuth2 146)/hateoas (2)/FeignOAuth2Properties — 3 项 |

**二次 REVIEW (09 §3)**: 行数/组件重验 + **35 顶层类全覆盖** → 补锚 9 处 (OF-2 自动装配主线/超时工厂/编程式/缓存/日志, OF-5 Spring Retry, OF-8 连接池, OF-3/OF-4 合并项, OF-6 开关条件)。

---

## §二 拓扑与依赖 (9 域教学顺序)

```
OF-1 注册 (怎么被发现) → OF-2 代理 (怎么生成) → OF-3 契约 (注解怎么解析)
→ OF-4 编解码 (消息转换) → OF-5 负载均衡 (Client 装饰) → OF-6 熔断 (Targeter 装饰)
→ OF-7 配置隔离 (子上下文) → OF-8 客户端/压缩 (底层实现) → OF-9 动态刷新 (URL 动态化收尾)
```

**前置/引出链**:
- OF-1 (无前置) → OF-2 (BeanDefinition 触发) + OF-7 (FeignClientSpecification)
- OF-2 → OF-3 (Contract) / OF-5 (loadBalance 路径) / OF-6 (Targeter 分支点 + circuitBreakerFeignBuilder) / OF-7 (子上下文组件) / OF-9 (RefreshableHardCodedTarget)
- OF-3 → OF-4 (Pageable queryMapIndex) / OF-7 (FeignClientsConfiguration 组装)
- OF-5 → OF-8 (装饰链底层)
- OF-7 → OF-9 (refreshableClient 联动)

**跨仓库依赖**: Feign 本体 (5.1, F-1~F-6 底座, 56 import) + SCC (5.4, 33 import: NamedContextFactory C-13/LoadBalancer C-7/CircuitBreaker C-8/@RefreshScope C-2)。

---

## §三 OF-1 交付内容 (注册机制, 🔴)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| @EnableFeignClients **5 组属性** (value/basePackages/basePackageClasses/defaultConfiguration/clients) | EnableFeignClients.java:50-88 |
| **@FeignClient 12 属性** (含 primary 默认 true) | FeignClient.java |
| FeignClientsRegistrar **ImportBeanDefinitionRegistrar** + registerBeanDefinitions 双注册 | FeignClientsRegistrar.java:71,152-155 |
| **getScanner 覆写 isCandidateComponent** (isIndependent && !isAnnotation → 接口可扫 — 关键!) | L379-391 |
| **getBasePackages 四级兜底** (value→basePackages→basePackageClasses→importingClass 包) | L393+ |
| **懒/急注册双模式** (lazy-attributes-resolution) + **10 属性 BeanDefinition** | L210-245 |
| **name 三级回退** (serviceId→name→value) + contextId | L327-345 |
| **refresh 联动**: refreshableClient = refresh-enabled (默认 false) → setScope("refresh") | L490,499-500 |
| validateFallback **!isInterface** 校验 | L83-87 |
| FeignClientSpecification implements **NamedContextFactory.Specification** (SCC C-13) | FeignClientSpecification.java:29 |

### 深审记录 (四轮, 修复 9 处)
- 一轮: 5 属性/10 属性/name 回退/refresh 联动/validate/4 级包
- 二轮: 0 修复 (反写测试全过)
- 三轮: **isCandidateComponent 覆写 (大发现)** + @FeignClient 12 属性 + SpEL URL 排除
- 四轮: 0 修复 (SCC 交叉验证: scc13-named-context 域存在)

### harness (MiniFeignRegistration): 兜底包/过滤/refresh 联动/name 回退/三参

### 负面空间 (6 条): 不运行时扫描/不自定义过滤/不包外扫描/不扫描缓存/不代理预创建/不配置合并

---

## §四 OF-2 交付内容 (代理创建与装配, 🔴)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| **getTarget 双路径**: 无 url→loadBalance / 有 url→unwrap | FeignClientFactoryBean.java:465-500 |
| **生产陷阱**: "Did you forget to include spring-cloud-starter-loadbalancer?" | L449-451 |
| **unwrap**: ((RetryableFeignBlockingLoadBalancerClient)client).getDelegate() | L495-497 |
| **9 组件 + Capability 族** (getInheritedAware) + **inheritParentContext 默认 true** | L192-260,100 |
| **FeignClientsConfiguration 14 @Bean 穷举** (3 Builder 变体含熔断版 + 2 Micrometer Capability) | L104-254 |
| OptionsFactoryBean **三级优先级逐项 fallback** (clientConfiguration 优先) | OptionsFactoryBean.java:60-95 |
| applyBuildCustomizers **AnnotationAwareOrderComparator 排序** | FeignClientFactoryBean.java:153-161 |
| **resolveTarget 三分支**: HardCoded/RefreshableHardCodedTarget (OF-9)/PropertyBasedTarget | L524-542 |
| **FeignAutoConfiguration 条件装配主线** (@ConditionalOnClass(Feign) + 3 Properties + 条件 @Bean) | L106-180 |

### 深审记录 (四轮, 修复 9 处)
- 一轮: 生产陷阱/unwrap/继承开关/超时优先级/Target 三态
- 二轮: 0 修复 (harness 5/5)
- 三轮: **3 Builder 变体 + 2 指标 Capability (大发现)** + inherit 默认 true + CachingCapability 装配点
- 四轮: 0 修复 (OF-6/OF-7/OF-9 三交叉验证)

### harness (MiniFeignProxy): 双路径/生产陷阱/继承开关/逐项 fallback/Target 三态

### 负面空间 (6 条): 不代理缓存/不自动探测 url/不组件缓存/不超时调优/不 URL 校验/不自动选 Targeter

---

## §五 OF-3 交付内容 (契约集成, 🔴)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| **类级 @RequestMapping 禁止** (IllegalArgumentException) | SpringMvcContract.java:226-231 |
| **组合注解** (isAnnotationPresent + findMergedAnnotation, @GetMapping 等) | L293-298 |
| **HTTP method 默认 GET** + checkOne/checkAtMostOne | L302-306,349-355 |
| **四解析**: parseProduces→ACCEPT / parseConsumes→CONTENT_TYPE / parseHeaders 键值拆分 / parseParams | L326-335,420-457 |
| **注册表 7 处理器** (MatrixVariable/PathVariable/RequestParam/RequestHeader/CookieValue/QueryMap/RequestPart) | L470-485 |
| **Pageable → queryMapIndex** (queryMapParamPresent 防冲突) — OF-4 关联 | L362-370 |
| **GET 未注解参数警告** ("may result in fallback to POST at runtime") | L245-260 |
| **未注解参数 → ConversionService 推断 Expander** (大发现) | L394-406 |
| **4 构造器演进** (processors→conversionService→decodeSlash→removeTrailingSlash) | L126-168 |
| 合并项: SpringQueryMap/FeignFormatterRegistrar/CollectionFormat | — |

### 深审记录 (五轮, 修复 8 处)
- 一轮: 类级禁止/GET 默认/四解析/Pageable/警告/4 构造器
- 二轮: 0 修复
- 三轮: expander 推断 (大发现) + PathVariable 模板变量匹配 + formParams 兜底
- 四轮: 0 修复 (**与 feign F-2 边界干净**: 0 交叉重复)
- 五轮: 0 修复 (锚点终验)

### harness (MiniContract): 类级禁止/组合注解/四解析/注册表/警告/Pageable 防冲突

### 负面空间 (6 条): 不类级路径/不多 method/不 headers 多值/不处理器热注册/不 SpringQueryMap 嵌套/不格式化自动发现

---

## §六 OF-4 交付内容 (编解码, 🟡)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| **HttpMessageConverter 复用** (converters 循环 + Generic 分支) | SpringEncoder.java:120-160 |
| **canWrite 双分支** (普通 Class+contentType / Generic genericType+Class) | L189,203 |
| **SpringFormEncoder 表单底座** (2 构造器) | L83-92 |
| **FeignResponseAdapter 是 SpringDecoder 内部类** (适配器桥接) | SpringDecoder.java:84+ |
| **HttpMessageConverterExtractor** + 类型白名单 (Class/ParameterizedType/WildcardType) | L64-87 |
| **ResponseEntityDecoder 三分支** (Parameterized/HttpEntity/透传) | L41-88 |
| **PageableSpringEncoder 组合模式** (supports = Pageable\|\|Sort + delegate fallback) | L38-110,135-136 |
| **PageJacksonModule: Page/Pageable 双反序列化** (SimplePageImpl + SimplePageable) | L62-88 |
| **PageableSpringQueryMapEncoder extends BeanQueryMapEncoder** (feign 本体继承) | L38 |
| Customizer = **Consumer 函数式** | — |

### 深审记录 (五轮, 修复 10 处)
- 一轮: canWrite 双分支/内部类适配器/charset 默认 false/supports/错误工厂
- 二轮: 0 修复
- 三轮: **SpringFormEncoder 底座 + Page/Pageable 双反序列化 + BeanQueryMapEncoder 继承 + Consumer** (4 处)
- 四轮: 0 修复 (queryMapIndex 交叉: OF-3 产出 ↔ OF-4 消费)
- 五轮: 0 修复 (边界: 与 feign F-4 零交叉)

### harness (MiniCodec): canWrite 判定/无转换器错误/类型白名单/三分支/分页双面

### 负面空间 (6 条): 不转换器缓存失效/不流式编码/不流式解码/不类型推断/不 HttpEntity 双向/不 Pageable 嵌套

---

## §七 OF-5 交付内容 (负载均衡, 🔴)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| **execute**: serviceId=URI host → hint → Lifecycle.onStart → **choose** | FeignBlockingLoadBalancerClient.java:118 |
| **instance==null → 503** (HttpStatus.SERVICE_UNAVAILABLE) + warn 文案 | L127-131 |
| reconstructURI + **transformers 链** + **getDelegate (OF-2 unwrap 目标)** | L135-162 |
| **条件装配 = OF-2 生产陷阱根源**: @ConditionalOnBean({LoadBalancerClient, Factory}) | FeignLoadBalancerAutoConfiguration.java:48 |
| **Retryable 版**: LoadBalancedRetryPolicy + RetryTemplate + **RetryableRequestContext** | Retryable...java:133-160 |
| **buildRetryTemplate 三态**: BackOff (可配/null→NoBackOff) / **NeverRetryPolicy** (未启用) / **InterceptorRetryPolicy** | L227-238 |
| **三底层组合**: HttpClient5 (L53-64) / **OkHttp 双版本** (L58-73) / **Http2 条件** (@ConditionalOnClass L45-57) | 配置类 |
| **默认底层 = feign Client.Default** (JDK) | DefaultFeignLoadBalancerConfiguration.java:54 |
| **executeWithLoadBalancerLifecycleProcessing 统一执行** + buildRequestData (LoadBalancerUtils L80-83) | L77,186 |
| XForwardedHeadersTransformer (xForwarded.isEnabled) + **getHint** (default 兜底+serviceId 覆盖) | L52,161-165 |

### 深审记录 (五轮, 修复 11 处)
- 一轮: 503 文案/陷阱根源/三态/默认 Client.Default/hint
- 二轮: 0 修复
- 三轮: buildRequestData 位置/重试上下文/OkHttp 双版本/Http2 条件/统一执行 (5 处)
- 四轮: 0 修复 (unwrap↔getDelegate 闭环 + scc11-blocking-retry 存在)
- 五轮: 0 修复 (503 用枚举非字面 + onComplete(DISCARD))

### harness (MiniLoadBalancerClient): 200/503/陷阱/三态/hint

### 负面空间 (6 条): 不实例缓存/不自动重试/不粘滞/不 Lifecycle 自动发现/不无限制重试/不 XForwarded 默认开

---

## §八 OF-6 交付内容 (熔断, 🟡)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| **Targeter 三分支**: 普通 (非 CircuitBreaker.Builder) / fallback / fallbackFactory | FeignCircuitBreakerTargeter.java:48-60 |
| **contextId 优先命名** + getFromContext (**FactoryBean unwrap** + 生产友好错误) | L52,75-90 |
| **invoke**: resolveName → create(name, group 可选) → **run(supplier, fallbackFunction)** / 无降级 run(supplier) | FeignCircuitBreakerInvocationHandler.java:99-117 |
| **asSupplier 异步线程上下文恢复** (isAsync + setRequestAttributes — TraceId 不丢) | L135-143 |
| **Builder.build 注入熔断 InvocationHandler** (大发现) | FeignCircuitBreaker.java:92-93 |
| **Builder extends Feign.Builder** (OF-2 circuitBreakerFeignBuilder) + 3 配置字段 | L33-77 |
| **双条件开关**: @ConditionalOnMissingClass(CircuitBreaker) 或 enabled=false | DisabledConditions.java:23-45 |
| **默认 NameResolver 双实现**: Default + Alphanumeric (alphanumeric-ids.enabled) | FeignAutoConfiguration.java:217-226 |
| unwrapAndRethrow **特判 NoFallbackAvailableException** | L118-122 |
| FallbackFactory **Default 内部类** (constant 兜底) + toFallbackMethod (同名映射) | — |

### 深审记录 (五轮, 修复 9 处)
- 一轮: invoke 核心/异步上下文/默认命名双实现/Builder 链式/特判
- 二轮: 0 修复
- 三轮: **build 注入 handler (大发现)** + getFromContext 完整 + 熔断 Bean 双条件 + alphanumeric 开关
- 四轮: 0 修复 (Builder 交叉: OF-2 circuitBreakerFeignBuilder ↔ OF-6)
- 五轮: 0 修复 (边界: feign 本体无 CircuitBreaker)

### harness (MiniCircuitBreaker): 三分支/熔断降级/异常降级/无降级直抛/异步上下文/命名/双条件

### 负面空间 (6 条): 不双降级/不降级缓存/不熔断器缓存/不跨线程池清洗/不 Builder 自动发现/不配置热更新

---

## §九 OF-7 交付内容 (配置隔离, 🔴)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| **FeignClientFactory extends NamedContextFactory** + 构造 (配置类+属性前缀) | FeignClientFactory.java:39,47-48 |
| **3 实例获取** (WithoutAncestors ×2 + getBean) — OF-2 继承开关消费 | L52-67 |
| **NamedContextFactory 惰性创建** (getContext) + **setConfigurations 按名注册** + **destroy→close** | SCC L98-119,109-114 |
| **14 @Bean 穷举**: 9 功能 + 3 Builder 变体 + 2 Micrometer Capability | FeignClientsConfiguration.java:104-254 |
| **isDefaultToProperties 默认 true** (Properties 覆盖注解组件) | FeignClientProperties.java:52 + FactoryBean:174-188 |
| **FeignClientConfiguration 8 字段** + defaultConfig+config 双级 | FeignClientProperties.java:31-33,131-151 |
| **decodeSlash/removeTrailingSlash 配置** (默认 true/false) — **OF-3 构造参数来源!** | L62-67 |
| **inheritParentConfiguration 默认 true** (FeignClientConfigurer) — OF-2 继承开关来源链 | — |
| **AOT 双处理器**: BeanFactoryInitialization (反射提示) + ChildContextInitializer (AotGenerator 生成 + per-contextId) | aot/ 2 文件 |
| 指标面 (micrometer.enabled 默认开, matchIfMissing) | FeignClientsConfiguration.java |

### 深审记录 (五轮, 修复 10 处)
- 一轮: defaultToProperties 默认/熔断 Builder 条件/指标默认开/继承开关链/AOT 双处理器
- 二轮: 0 修复
- 三轮: **decodeSlash 跨域闭环 (大发现)** + setConfigurations + AOT 反射提示 + destroy
- 四轮: 0 修复 (双 IN 承接 + scc13 交叉)
- 五轮: 0 修复 (11 处类名引用均为自然提及)

### harness (MiniNamedContext): 惰性复用/熔断条件/双级覆盖/继承开关

### 负面空间 (6 条): 不子上下文复用/不组件自动发现/不配置热更新/不 Properties 校验/不 AOT 全量/不反射全量注册

---

## §十 OF-8 交付内容 (HTTP 客户端与压缩, 🟡)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| **HttpClient5 连接池**: PoolingHttpClientConnectionManagerBuilder (SSL/连接数/PoolReusePolicy FIFO/SoTimeout) | HttpClient5FeignConfiguration.java:60-140 |
| **默认值穷举**: MAX_CONNECTIONS=**200** / PER_ROUTE=**50** / SSL=false / FOLLOW_REDIRECTS=true / TIMEOUT=**2000** | FeignHttpClientProperties.java:42-72 |
| **Http2**: HttpClient.newBuilder (Redirect.ALWAYS/NEVER) + **Http2ClientCustomizer** | Http2ClientFeignConfiguration.java:40-90 |
| **请求压缩双条件**: matchesMimeType && contentLengthExceedThreshold (minRequestSize) | ContentGzip:60-100 |
| **默认值**: mimeTypes 3 个 + minRequestSize=**2048** + contentEncodingTypes [gzip, deflate] | FeignClientEncodingProperties.java:36,41,46 |
| **apply = addHeader(Content-Encoding)** | ContentGzip:30-45 |
| **响应协商**: Accept-Encoding: gzip + **OkHttp 双条件** (缺失或 okhttp.enabled=false) | AcceptGzip:30+ / OkHttp 条件:35-55 |
| request/response.enabled **对称开关** | 2 AutoConfiguration |
| 三底层 → **OF-5 装饰链** (ApacheHttp5Client/Http2Client/OkHttp) | — |

### 深审记录 (五轮, 修复 10 处)
- 一轮: 默认值/2 Bean/双条件/OkHttp/对称开关/Customizer
- 二轮: 0 修复
- 三轮: PoolReusePolicy FIFO/Content-Encoding 执行/contentEncodingTypes/OkHttp 双条件 (4 处)
- 四轮: 0 修复 (装饰链交叉: OF-5 组合 ↔ OF-8 配置)
- 五轮: 0 修复 (默认值初始化表达式形式确认)

### harness (MiniHttpClient): 默认 200/50/Redirect/双条件/OkHttp 特判

### 负面空间 (6 条): 不连接池动态/不协议自动协商/不 HTTP/3/不强制压缩/不压缩级别/不 Brotli

---

## §十一 OF-9 交付内容 (动态刷新, 🟡)

### 核心机制速查

| 机制 | 锚点 |
|:--|:--|
| **RefreshableHardCodedTarget extends HardCodedTarget** + **url() 覆写** (每次取最新) | RefreshableHardCodedTarget.java:28,49-51 |
| ⚠ **RefreshableUrl 不可变** (final) — 刷新靠 **FactoryBean 重建换实例** (非换内部值!) | RefreshableUrl.java |
| **刷新机制 = setScope("refresh") + ScopedProxyUtils.createScopedProxy** (非 @RefreshScope 注解!) | FeignClientsRegistrar.java:485-503 |
| **isClientRefreshEnabled** (refresh-enabled 默认 false) — OF-1 refreshableClient 来源闭环 | L499-500 |
| **refreshableClient 3 处影响**: options 条件 (L279) + **getOptionsByName 超时也动态** (L447-450) + resolveTarget (L529) | FeignClientFactoryBean.java |
| **resolveTarget 完整回退链**: url → Refreshable (非空) → isUrlAvailableInConfig → PropertyBased | L524-560 |
| **PropertyBasedTarget.url() 懒计算 + 缓存** (AOT 场景) | PropertyBasedTarget.java:50-62 |
| **getUrl 统一规范化**: SpEL 排除 → 前缀补全 → URI 校验 (malformed 抛) | FeignClientsRegistrar.java:115-127 |
| RefreshableUrlFactoryBean **null 容错** (无配置 → RefreshableUrl(null)) | L61-66 |

### 深审记录 (七轮, 修复 9 处)
- 一轮: 语义修正 (不可变→换实例) + **作用域代理 (大发现)** + 开关闭环 + 懒计算 + getUrl
- 二轮: 0 修复
- 三轮: refreshableClient 3 处影响 + **Options 超时动态** + resolveTarget 回退链
- 四轮: 0 修复 (开关/消费双闭环)
- 五轮: **2 处行号漂移** (url() L63-66→L49-51 / isClientRefreshEnabled L504-506→L499-500) — 09 §3 实战验证!
- 六轮: 0 修复 (修正后复核)
- 七轮: 0 修复 (harness 9 域全量复跑)

### harness (MiniRefreshableUrl): 换实例生效/刷新重建/懒计算缓存/规范化/SpEL

### 负面空间 (6 条): 不 URL 缓存/不刷新传播 in-flight/不 URL 模板/不 URL 刷新 (懒加载非动态)/不多 RefreshableUrl/不 SpEL 求值

---

## §十二 harness 汇总 (9 个, 全部 5/5 PASS)

| harness | 验证点 |
|---|---|
| MiniFeignRegistration | 兜底包/类型过滤/refresh 联动/name 回退/Specification 三参 |
| MiniFeignProxy | 双路径/生产陷阱/继承开关/逐项 fallback/Target 三态 |
| MiniContract | 类级禁止/组合注解/四解析/注册表/警告/Pageable 防冲突 |
| MiniCodec | canWrite/无转换器错误/类型白名单/三分支/分页双面 |
| MiniLoadBalancerClient | 200/503/陷阱/重试三态/hint |
| MiniCircuitBreaker | 三分支/熔断降级/无降级直抛/异步上下文/双条件 |
| MiniNamedContext | 惰性复用/熔断条件/双级覆盖/继承开关 |
| MiniHttpClient | 默认值/Redirect/双条件/OkHttp 特判 |
| MiniRefreshableUrl | 换实例/刷新重建/懒计算/规范化/SpEL |

---

## §十三 过程教训 (本阶段新增)

1. **09 §3 行号漂移真实存在**: OF-9 五轮深审仍抓到 2 处行号漂移 (L63-66→L49-51 等) — **任何深审轮次都要 re-grep 行号**, 不能因前几轮通过就跳过。
2. **"非常详细"不等于"讲机制重复"**: 每域大纲与相邻域保持边界干净 (OF-3 不提 @RequestLine, OF-4 不提 SpringMvcContract, OF-8 不提 LB 机制) — 跨域交叉用引用不重述。
3. **默认值 = 穷举对象**: 200/50/2048/60s/FIFO/3 MIME 等 — 每个默认值都有源码锚 (深审中多次抓到)。
4. **代际修正要早**: OF-6 熔断的 Hystrix/Sentinel → Spring Cloud CircuitBreaker 代际差异 — PLAN 阶段修正避免大纲返工。
5. **生产陷阱闭环**: OF-2 的 loadBalance 报错 ↔ OF-5 的条件装配 — 跨域闭环解释 (无 starter-loadbalancer 的完整失败链)。
6. **HANDOFF REVIEW 抓 2 类问题**: ①路径层级 (issue 双基准在 training-camp/source-code/issue/ 与 analysis/source-analysis/issue/ 两层) ②**声明修复 ≠ 实际落地** — OF-6 三轮 #7 "build 注入" 声明修到大纲 §4 但 outline 未实际更新, REVIEW 时用 HANDOFF↔outline 关键词交叉核对抓到 → 交接文档写作时应做全量交叉核对。

---

## §十四 遗留与待办

- [ ] 9 域大纲可进入写作阶段 (每篇写作时行号 re-grep)
- [ ] 阶段 5 收尾对照: Gateway (5.5, 其他 AI 进行中) / Alibaba (5.7) / Nacos (5.8) / Sentinel (5.9)
- [ ] 每域写完文章后更新本文 (写作状态列)
- [ ] OPENFEIGN-PLAN.md §七 检查单: "偏差已同步 HANDOFF" 勾选 (本文即同步)

---

## §十五 文件路径

| 东西 | 路径 |
|:--|:--|
| 域规划 (09 审计 v1) | `spring-cloud-openfeign/OPENFEIGN-PLAN.md` |
| 本交接文档 | `spring-cloud-openfeign/HANDOFF-OPENFEIGN.md` (本文) |
| 源码 | `/data/workspace/source-code/code/spring/spring-cloud-openfeign` (4.3.2, 87 文件) |
| 大纲 (9 域 × 9 文件) | `spring-cloud-openfeign/outlines/of1~of9-*/` |
| harness (9 个) | `spring-cloud-openfeign/harness/of1~of9-*/` |
| 方法论 (01-09) | `talk-method/source-code-analysis/methodology/zh/` |
| 执行计划 (原始 9 域) | `../issue/源码分析执行计划.md` 阶段5.6 (analysis/source-analysis/issue/) |
| issue 规划 (双基准之一) | `../../../issue/SpringCloudOpenfeign源码学习范围规划.md` (F-1~F-9, training-camp/source-code/issue/) |
| 底座仓库 | feign (5.1, HANDOFF-FEIGN.md) / spring-cloud-commons (5.4, SCC-PLAN.md) |

---

## §十六 下一步行动

```
✅ 9 域全部定稿 + 深审收敛 (每域 outline + 4 闭环 + 负面空间 + harness 5/5 + 反写测试全过)

1. 每域可进入写作阶段 (大纲反写测试全过, 锚点已验证; 写作时行号仍须 re-grep)
2. 阶段 5 收尾: Gateway (5.5, 另一 AI) / Alibaba (5.7) / Nacos (5.8) / Sentinel (5.9)
3. 写作顺序建议: 按拓扑 OF-1 → OF-2 → ... → OF-9 (与规划一致)
```

## §十七 完成检查单

- [x] 09 双基准审计 (执行计划 + issue 规划, 全部断言验证)
- [x] 域清单 9 域 (修正 4 处 + 新增 2 域)
- [x] 数字穷举 (7 处理器/14 Bean/9 组件/3 Builder/默认值全)
- [x] 依赖方向 (core→feign 56 / core→commons 33)
- [x] 9 域全部深审收敛 (每域 4~7 轮, harness 5/5)
- [x] 本交接文档 (唯一入口, 已同步)
