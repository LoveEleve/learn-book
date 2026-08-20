# OF-2 代理创建与装配 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (FeignClientFactoryBean 742 / FeignClientsConfiguration 260 / OptionsFactoryBean)
> 09 域级审计: OPENFEIGN-PLAN OF-2 (合并 OF-1+F-2, 补锚 6 处) — 断言 "getTarget 两种路径 + 9 组件" 已 grep 验证

## 入口展开 (Level-1~3, 已读源码)

### Level-1: getObject → getTarget (L455-465)

```
getObject (L455) → getTarget (L465)
getTarget:
├── feignClientFactory = beanFactory.getBean(FeignClientFactory.class) — 子上下文工厂 (OF-7)
├── builder = feign(feignClientFactory) (L466)
├── 双路径:
│   ├── **路径 1: 无 url** (L468-482): url = "http://" + name + cleanPath()
│   │   → loadBalance(builder, factory, new HardCodedTarget(type, name, url))
│   │   ← 走负载均衡 (OF-5)
│   └── **路径 2: 有 url** (L484-500): url 前缀补全 (http://)
│       → client = getOptional(Client.class)
│       → FeignBlockingLoadBalancerClient → unwrap ("not load balancing because we have a url")
├── applyBuildCustomizers (L501)
└── targeter.target(this, builder, factory, resolveTarget(...)) (L502-503)
    ← Targeter 分支点 (OF-6 熔断装饰!)
```

### Level-2: feign() builder 装配 (L120-145)

```
Feign.Builder feign(context):
├── loggerFactory = get(FeignLoggerFactory.class) → Logger (L136-137)
└── builder = get(Feign.Builder.class) (子上下文)
    .logger(logger)
    .encoder(get(Encoder.class))
    .decoder(get(Decoder.class))
    .contract(get(Contract.class))   ← Contract = SpringMvcContract (OF-3)
```

### Level-3: configureUsingConfiguration — 9+2 组件 (L192-260)

```
组件获取 (子上下文, getInheritedAware 族):
├── Logger.Level (L193) — 日志级别
├── Retryer (L197) — 重试器
├── ErrorDecoder (L201) + FeignErrorDecoderFactory (L206) — 错误解码
├── Request.Options (L212) — 超时 (OptionsFactoryBean 来源)
├── Map<RequestInterceptor> (L223) — 请求拦截器集合 (排序)
├── ResponseInterceptor (L230) — 响应拦截器
├── QueryMapEncoder (L234) — 查询参数编码
├── ExceptionPropagationPolicy (L241) — 异常传播策略
└── ⚠ Map<Capability> (L247) — 能力集 (CachingCapability 缓存面!)

configureUsingProperties (L146-190): FeignClientProperties
└── defaultConfig + contextId 双配置合并 → builder (OF-7 关联)
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-2 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "getTarget 两种路径" | 无 url→loadBalance (L468-482) / 有 url→unwrap (L484-500) | 接受 |
| "loadBalance 生产陷阱" | Client null → IllegalStateException "Did you forget to include spring-cloud-starter-loadbalancer?" (L449-451) | 接受 (补锚: 精确文案) |
| "9 组件装配" | L193-247: Logger/Retryer/ErrorDecoder/Options/RequestInterceptor/ResponseInterceptor/QueryMapEncoder/ExceptionPropagationPolicy + Capability | 接受 (**+Capability 第 10 面**) |
| "14 默认 @Bean" | FeignClientsConfiguration 14 @Bean (PLAN 已验) | 接受 |
| "OptionsFactoryBean 超时" | L76-82: clientConfiguration 优先 + fallback | 接受 |
| "FeignBuilderCustomizer" | applyBuildCustomizers (L501) | 接受 |
| 补锚: Properties 配置面 | configureUsingProperties defaultConfig+contextId 合并 | 补锚 (OF-7 关联) |
| 补锚: Targeter 分支点 | targeter.target (L502) — OF-6 熔断装饰点 | 补锚 |

## 待展开 (下一层)

1. unwrap 细节 (FeignBlockingLoadBalancerClient 的 delegate 提取)
2. Targeter 分支完整 (DefaultTargeter vs FeignCircuitBreakerTargeter — OF-6)
3. OptionsFactoryBean 完整 (clientConfiguration/fallback 优先级)
4. FeignBuilderCustomizer 应用 (applyBuildCustomizers 排序?)
5. resolveTarget (url 占位符/PropertyBasedTarget — OF-9)
