# SCC-5 @LoadBalanced 客户端 — 一个注解, 拦截一切: RestTemplate 的负载均衡魔法

> 前置: [[SCC-3-服务发现]] (实例来源) + [[SCC-13-NamedContextFactory]] (LoadBalancerClientFactory) | 引出: [[SCC-11-BlockingLoadBalancer]] (阻塞式执行) + [[SCC-7-ReactorLoadBalancer]] (策略核心) | 对照: Ribbon @LoadBalanced + Nacos LB
> 🔴 A | 方案 A (全深度) | 闭环: q1(注解面) q2(拦截链) q3(阻塞包装) q4(装配面)
> Pass 2 闭环: q1(@Qualifier) q2(intercept→execute) q3(choose block) q4(AutoConfiguration)

**读者处境**: `@LoadBalanced` 标记的 RestTemplate, 请求 `http://orders/api` 怎么变成真实 `http://10.0.0.5:8080/api`? 拦截器从哪提取服务名? "延迟拦截器"解决什么问题? 阻塞式怎么从响应式 LoadBalancer 取实例?

### 1. @LoadBalanced — @Qualifier 限定符的魔力

场景: 一个注解怎么让 RestTemplate "开挂"?
源码路径:
- LoadBalanced (loadbalancer/LoadBalanced.java:34): **@Qualifier 元注解** (L38) + @Target{FIELD,PARAMETER,METHOD} (L34)
- 语义: **标记"这个 RestTemplate 需要负载均衡"** — 收集方: LoadBalancerAutoConfiguration 的 `@LoadBalanced List<RestTemplate>` (L60-62, @Autowired(required=false) 收集所有标记的)
- 与普通 RestTemplate 隔离: @Qualifier 让注入面区分
关键设计 (q1): **@LoadBalanced 是"标记 + 限定符"双语义** — 收集方用它筛选 (注入 List\<RestTemplate\> 时按注解过滤), 使用者可 @Qualifier 区分; 注解本身零逻辑, 魔力全在消费方。 [模式: 标记注解]

### 2. LoadBalancerInterceptor — 拦截 RestTemplate 请求

场景: 请求怎么被拦截? 服务名从哪来?
源码路径:
- LoadBalancerInterceptor (LoadBalancerInterceptor.java:33-52): implements BlockingLoadBalancerInterceptor (4.1.2+)
- **intercept** (L50-55): ① `serviceName = originalUri.getHost()` (L53) — **从 URL 的 host 提取服务名**! ② **Assert.state 校验** (L54, 无 host 抛 "Request URI does not contain a valid hostname") ③ `loadBalancer.execute(serviceName, requestFactory.createRequest(request, body, execution))` (L55)
- 双构造 (L41-44): 带 requestFactory / **backwards compatibility 单参** (L43, 新 LoadBalancerRequestFactory)
- 扩展: RetryLoadBalancerInterceptor (retry 包, 重试变体)
关键设计 (q2): **"http://orders/api" 的 host 就是服务名** — 这是 @LoadBalanced 的核心约定: 逻辑服务名写在 URL host 位置; Assert 防"无 host 的非法 URI"; execute 委托 LoadBalancerClient (SCC-11 阻塞包装)。 [模式: 拦截 + 服务名提取]

### 3. LoadBalancerRequestFactory — 请求变换器与延迟包装

场景: 拦截后请求怎么被"包装"?
源码路径:
- LoadBalancerRequestFactory (LoadBalancerRequestFactory.java:35): 持有 loadBalancer + **transformers** (LoadBalancerRequestTransformer 列表)
- createRequest (L52-55): **BlockingLoadBalancerRequest(loadBalancer, transformers, ClientHttpRequestData)** (L54-55) — 包装请求数据
- transformers: 请求发送前变换 (Javadoc L27-29 明示 "Applies LoadBalancerRequestTransformers to the intercepted HttpRequest"); **transformRequest(HttpRequest, ServiceInstance) 签名** (LoadBalancerRequestTransformer.java:22 — **变换器在实例选定后执行, 可基于实例改请求**) + @Order + DEFAULT_ORDER=0 (L18-21)
关键设计 (q2): **请求包装是"数据 + 变换器"的组合** — BlockingLoadBalancerRequest 持有原始请求数据; transformers 在真正发送前可改请求 (如加 header); 工厂是"包装器工厂"。 [模式: 请求包装 + 变换器]

### 4. LoadBalancerClient — 双 execute + reconstructURI 契约

场景: LoadBalancerClient 接口承诺什么?
源码路径:
- LoadBalancerClient (LoadBalancerClient.java:29): **extends ServiceInstanceChooser** (choose 双形态: choose(serviceId) / choose(serviceId, Request) L31-46)
- **execute 双形态** (L42-50/L54-60): execute(serviceId, request) / execute(serviceId, serviceInstance, request) — 前者自己选实例, 后者指定实例
- **reconstructURI** (L67-72): 把逻辑服务名 host 换成实例 host:port (Javadoc 明示 "http://myservice/path → host:port")
- ServiceInstanceChooser (L31-46): choose 契约
关键设计 (q3): **execute 双形态 = "选+发" 或 "指定+发"** — 灵活支持不同场景; reconstructURI 是"逻辑名 → 真实地址"的转换契约; extends ServiceInstanceChooser 让客户端同时是"选择器"。 [模式: 双形态执行 + URI 重建]

### 5. BlockingLoadBalancerClient — 阻塞式包装响应式

场景: 阻塞式怎么从响应式 LoadBalancer 取实例?
源码路径:
- BlockingLoadBalancerClient (loadbalancer/blocking/client/BlockingLoadBalancerClient.java:59): implements LoadBalancerClient
- execute (L68-84): ① **getHint(serviceId)** (L69) ② **LoadBalancerRequestAdapter + buildRequestContext** (L70-71, RequestDataContext/DefaultRequestContext 二选一 L86-96) ③ **LoadBalancerLifecycle.onStart** (L73-74, 生命周期开始回调) ④ choose (L75) ⑤ **实例 null → onComplete(DISCARD) + IllegalStateException "No instances available"** (L77-81) ⑥ execute 指定实例 (L83, DefaultResponse 包装 L89+)
- **choose** (L158-163): `Mono.from(loadBalancer.choose(request)).block()` — **阻塞式从响应式 ReactorLoadBalancer 取** (SCC-7 消费!)
- reconstructURI (L148-149): LoadBalancerUriTools.reconstructURI 委托
关键设计 (q3): **"阻塞式 = 响应式 + block()"** — BlockingLoadBalancerClient 不实现均衡算法, 只是把 ReactorLoadBalancer 的 Mono 结果 block 成同步值; 这是"响应式内核 + 阻塞门面"的桥 (SCC-11 详)。 [模式: 阻塞门面包响应式]

### 6. 装配面 — LoadBalancerAutoConfiguration 的收集与延迟拦截器

场景: @LoadBalanced RestTemplate 怎么被装上拦截器?
源码路径:
- LoadBalancerAutoConfiguration (LoadBalancerAutoConfiguration.java:55-58): **@Conditional(BlockingRestClassesPresentCondition)** + @ConditionalOnBean(LoadBalancerClient)
- **@LoadBalanced List\<RestTemplate\> 收集** (L60-62) + transformers 收集 (L65-67)
- **SmartInitializingSingleton loadBalancedRestTemplateInitializerDeprecated** (L68-77): 遍历 @LoadBalanced RestTemplate 应用 RestTemplateCustomizer — **拦截器注入点**
- **DeferringLoadBalancerInterceptor** (L86-101, 4.1.2+): 延迟拦截器 + **LoadBalancerRestTemplateBuilderBeanPostProcessor/RestClientBuilder 后处理器** — 解决"RestTemplate 创建早于 LoadBalancer 可用"的时序; **实现: ObjectProvider.getIfAvailable 首次 intercept 时解析真实拦截器** (DeferringLoadBalancerInterceptor.java:40-44, 无则抛 "LoadBalancer interceptor not available")
关键设计 (q4): **SmartInitializingSingleton 是"所有单例就绪后"的注入时机** — 避免循环依赖; DeferringLoadBalancerInterceptor 延迟解析 (4.1.2 解决时序问题); 条件装配保证"无 LoadBalancer 不装"。 [模式: 后初始化注入 + 延迟解析]

## 代码类型
Architecture (客户端拦截) + Integration (RestTemplate 集成)

## 负面空间 — @LoadBalanced 客户端刻意不做的事

- **不做 Ribbon 兼容**: LoadBalancerClient 是新一代抽象, Ribbon 已淘汰 (对照历史)
- **不做实例缓存**: 每次拦截实时 choose, 无缓存 (对比 Ribbon 的 ServerList 缓存)
- **不做重试内建**: RetryLoadBalancerInterceptor 是独立扩展, 核心无重试
- **不做异步支持**: @LoadBalanced 只拦阻塞式 RestTemplate; WebClient/响应式走 ReactiveLoadBalancer (loadbalancer 模块)
- **不做 URL 协议校验**: 只取 host 做服务名, 协议 (http/https) 由实例 URI 决定
- **不做请求重放保护**: 变换器/拦截器顺序固定, 无重放检测

→ 引出: 阻塞式执行和重试怎么实现? → SCC-11 BlockingLoadBalancer 重试
