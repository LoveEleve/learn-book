# ALI-A6 Sentinel 三路限流 — RestTemplate/Feign/Web MVC 的限流三叉戟

> 前置: [[SCC-10-断路器抽象]] (fallback 语义对照) | 引出: [[Sentinel-5.9]] (SphU 内核) + [[ALI-A7-数据源]] (规则来源) | 对照: OpenFeign 5.6 (Feign.Builder 覆写面)
> 🔴 A | 方案 A (全深度) | 闭环: q1(注解发现) q2(双 entry) q3(拦截器动态注册) q4(Feign 代理)

**读者处境**: 给 RestTemplate 加 @SentinelRestTemplate 注解后, 限流拦截器怎么"自动"挂上去的? Feign 接口的 fallback 怎么和 Sentinel 的 BlockException 挂钩? 为什么限流资源名是 "GET:http://host:8080" 这种格式? blockHandler 方法签名为什么必须是那 4 个参数?

### 1. 注解发现 — SentinelBeanPostProcessor 的 MergedBeanDefinition 双路径

场景: @SentinelRestTemplate 注解怎么在 Bean 创建前被发现?
源码路径:
- **SentinelBeanPostProcessor** (custom/SentinelBeanPostProcessor.java:52): implements **MergedBeanDefinitionPostProcessor** — 在 BeanDefinition 合并阶段介入
- **postProcessMergedBeanDefinition** (L66-79): ① beanName null 或非 RestTemplate → return (L69) ② **getSentinelRestTemplateFromBeanDefinition 双路径** (L81-92): StandardMethodMetadata source (L83-85) / ResolvedFactoryMethod 注解 (L87-89) — 兼容 @Bean 注解方法两种元数据形态 (issue#3329 "Support custom RestTemplate")
- **checkSentinelRestTemplate 三校验** (L94-105): blockHandler/fallback/urlCleaner 三类逐个 checkBlock4RestTemplate
- **cache.put** (L77) — 记录待处理 Bean
关键设计 (q1): **"MergedBeanDefinitionPostProcessor = 先发现后织入"** — 定义阶段探测注解 + 强校验 (失败抛 IllegalArgumentException), 初始化阶段 (postProcessAfterInitialization) 才织入拦截器; 校验前置避免"运行时才发现配置错"。 [模式: 定义期校验]

### 2. 强校验契约 — blockHandler 方法签名的硬性要求

场景: blockHandler 方法写错签名会怎样?
源码路径:
- **checkBlock4RestTemplate** (L107-174): ① 类/方法属性配对校验 (L109-125: 有类无方法/有方法无类 → IllegalArgumentException) ② **签名强校验** (L126-146): `(HttpRequest, byte[], ClientHttpRequestExecution, BlockException)` 静态方法 (urlCleaner 特例: (String)→String L127-129) — ClassUtils.getStaticMethod 找不到 → 抛带签名提示的异常 (L136-146) ③ **返回类型校验** (L148-163): ClientHttpResponse (urlCleaner → String)
- **注册回调** (L164-173): BlockClassRegistry.updateBlockHandlerFor/updateFallbackFor/updateUrlCleanerFor — 静态注册表
- 错误信息含完整签名提示 (L139-145): "please check your class name, method name and arguments"
关键设计 (q2): **"反射查找 + 编译期式校验"** — 运行时用 getStaticMethod 精确匹配签名, 失败即抛 (fail-fast 配置哲学) — 与 SCC-2 的 Assert 式守卫同构。 [模式: 反射强校验]

### 3. 拦截器动态注册 — postProcessAfterInitialization 织入

场景: SentinelProtectInterceptor 的 Bean 哪来的? 怎么挂到 RestTemplate?
源码路径:
- **postProcessAfterInitialization** (L176-202): cache 命中 → **动态注册 BeanDefinition** (L204-217: DefaultListableBeanFactory.registerBeanDefinition + 两构造参数) → getBean 取拦截器 → **restTemplate.getInterceptors().add(0, interceptor)** (L199 — 插到 0 位, 最优先执行)
- **interceptorBeanName 组装** (L181-195): `sentinelProtectInterceptor_{blockClass}{blockMethod}_{fallback...}_{urlCleaner...}@{bean}` — 唯一 Bean 名 (含 RestTemplate toString 防冲突)
- 为什么动态注册而非直接 new: 拦截器可被应用直接注入使用
关键设计 (q3): **"运行时注册 = 注解驱动的动态装配"** — 每个注解 RT 生成专属拦截器 Bean (配置绑定), 插入拦截器链 0 位; Bean 名编码配置摘要, 防重防冲突。 [模式: 动态 Bean 注册]
- **示例实证**: examples/sentinel-resttemplate-example RestTemplateConfiguration — **@LoadBalanced + @SentinelRestTemplate 叠加同一 Bean**, 两拦截器链 (LB 拦截器 + Sentinel 拦截器) 共存; SentinelRulesConfiguration 用 `FlowRuleManager.loadRules/DegradeRuleManager.loadRules` 编程式加载 (与 A8 断路器同 API, 与 A7 数据源 register2Property 是两条独立路)

### 4. 双 entry — SentinelProtectInterceptor 的资源粒度

场景: 限流资源名为什么是 host 级 + host+path 级两个?
源码路径:
- **SentinelProtectInterceptor** (custom/SentinelProtectInterceptor.java:44): ClientHttpRequestInterceptor
- **资源名构建** (L59-63): `hostResource = METHOD:scheme://host[:port]` (L60-62) + `hostWithPathResource = hostResource + path` (L63) — **两级资源**
- **双 entry** (L79-84): SphU.entry(host, OUT) + entryWithPath 时 SphU.entry(host+path, OUT) — 流量同时计入两级
- **urlCleaner** (L68-74): hostWithPathResource 过 cleaner (如 /order/1,/order/2 → /order/{id})
- **Tracer.trace** (L85-88): ErrorHandler.hasError → trace 异常 (熔断统计)
- **BlockException 分流** (L91-107): BlockException → handleBlockException; 其他 → traceEntry + 按类型重抛
- **handleBlockException** (L118-142): **DegradeException → fallback** (L122-131, 无 fallback → SentinelClientHttpResponse 空响应) / **其他 (Flow) → blockHandler** (L133-141, 无 → 空响应)
- **finally 双 exit** (L108-115): hostWithPath 先退再 host 退 — 严格配对
关键设计 (q4): **"两级资源 = 粗粒度兜底 + 细粒度精确"** — host 级控"服务总流量", host+path 级控"接口流量"; degrade 走 fallback (业务降级), flow 走 blockHandler (限流处理) — 异常类型决定降级路径。 [模式: 资源分级 + 异常分流]

### 5. Feign 路 — SentinelFeign.Builder 覆写 internalBuild

场景: Feign 客户端怎么换成 Sentinel 版?
源码路径:
- **SentinelFeign.builder()** (feign/SentinelFeign.java:55-57): 返回自定义 Builder
- **Builder extends Feign.Builder** (L59): **invocationHandlerFactory 抛 UnsupportedOperationException** (L68-72 — 禁止用户覆盖) + **internalBuild 覆写** (L81-131): 自定义 InvocationHandlerFactory.create
- **FeignClientFactoryBean 获取双路径** (L86-105): lazy-attributes-resolution=true → BeanDefinition attribute 取 (L93-101, 避免循环依赖); false → getBean("&"+type) (L103-105)
- **fallback 三选** (L113-131): fallback → FallbackFactory.Default 包装 / fallbackFactory → 原样 / 无 → 裸 SentinelInvocationHandler
- **getFromContext** (L133-159): feignClientFactory.getInstance (SCC-13 NamedContextFactory!) + FactoryBean 解包 (L143-151) + **assignable 校验** (L153-157)
关键设计 (q5): **"禁止覆写 + 内部接管"** — invocationHandlerFactory 被锁死, 保证所有 Feign 客户端必经 SentinelInvocationHandler; fallback 从 FeignClientFactoryBean 解析 (兼容 OpenFeign 的 fallback 配置面)。 [模式: 锁死扩展点]

### 6. SentinelInvocationHandler — 方法级资源与 fallbackFactory

场景: Feign 方法调用怎么限流? fallback 怎么触发?
源码路径:
- **SentinelInvocationHandler** (feign/SentinelInvocationHandler.java:47): InvocationHandler — equals/hashCode/toString 特判 (L73-89) + **只处理 HardCodedTarget** (L94, 其他 target 走默认 L144-147)
- **MethodMetadata 查找** (L95-103): SentinelContractHolder.METADATA_MAP — **key = 接口全名 + configKey** (L96-97)
- **资源名** (L103-104): `METHOD:url + path` (与 RestTemplate 路同构但无 host 级)
- **entry 包裹** (L106-141): ContextUtil.enter + SphU.entry(resource, OUT, 1, args) → invoke → catch: **非 BlockException 才 Tracer.traceEntry** (L113-115) → **fallbackFactory.create(ex) + fallbackMethodMap 方法调用** (L116-130) / 无 fallback → 原样抛出 (L131-134) → finally: entry.exit(1, args) + ContextUtil.exit
- **fallbackMethodMap** (L62): dispatch 映射到 fallback 接口方法 — 签名对齐
关键设计 (q6): **"同构资源命名 + fallback 双态"** — Feign 与 RestTemplate 资源名同构 (METHOD:url), 便于统一控制台观察; fallbackFactory 有异常入参 (可针对异常降级), 无 fallback 则异常上抛 (调用方处理)。 [模式: 代理降级]

### 7. Web MVC 路 — SentinelWebAutoConfiguration 的配置面

场景: Web 请求的限流拦截器怎么装配?
源码路径:
- **SentinelWebAutoConfiguration** (SentinelWebAutoConfiguration.java:34): @ConditionalOnWebApplication(SERVLET) + @ConditionalOnProperty(enabled, matchIfMissing=true) + @ConditionalOnClass(SentinelWebInterceptor)
- **三 Bean** (L66-94): SentinelWebInterceptor (L66-69) + **SentinelWebMvcConfig 组装** (L73-89: httpMethodSpecify/webContextUnify + **BlockExceptionHandler 三选**: 用户 Optional → blockPage 重定向 → DefaultBlockExceptionHandler) + urlCleaner/requestOriginParser Optional (L90-91) + SentinelWebMvcConfigurer (L93)
- 拦截器类在 sentinel 仓库 (adapter.spring.webmvc_v6x) — 集成层只组装配置
- filter.enabled 条件 (L68) — 可关
关键设计 (q7): **"集成层 = 配置组装器"** — 拦截器内核在 sentinel 仓库 (09 审计确认), SCA 只做: 条件装配 + 三选 Block 处理器 + Optional 注入扩展点。 [模式: 薄组装]

### 8. 测试与行为锚

场景: 三路限流的行为锚点?
源码路径:
- 测试: SentinelRestTemplateTests / SentinelFeignTests (test 目录)
- 注释锚: issue#3329 (SentinelBeanPostProcessor:68 — custom RestTemplate 支持) / lazy-attributes-resolution 说明 (SentinelFeign:90-92)
- SentinelConstants: PROPERTY_PREFIX = "spring.cloud.sentinel" (L20) / COLD_FACTOR "3" (L28)
关键设计 (q1): **"issue 锚 + 常量集中"** — 关键修复留 issue 号; 常量集中定义避免魔法值。 [模式: 行为锚]
