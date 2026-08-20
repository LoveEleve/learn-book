# OF-2 代理创建与装配 — Pass 2 闭环 Q2: builder 装配 (组件 + Capability)

> 核心: feign() + configureUsingConfiguration | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Feign.Builder 从哪来? 组件怎么装配? 还有哪些扩展面?**

## 机制链 (已实证)

```
feign() (L120-145) — builder 基础装配:
├── loggerFactory = get(FeignLoggerFactory.class) → Logger (L136-137)
└── builder = get(Feign.Builder.class) (子上下文!)
    .logger(logger).encoder(get(Encoder.class))
    .decoder(get(Decoder.class)).contract(get(Contract.class))  ← Contract=SpringMvcContract (OF-3)

configureUsingConfiguration (L192-260) — 子上下文组件 (getInheritedAware 族):
├── Logger.Level (L193) / Retryer (L197) / ErrorDecoder (L201) + FeignErrorDecoderFactory (L206)
├── Request.Options (L212, OptionsFactoryBean 来源, q3)
├── Map<RequestInterceptor> (L223, 集合) / ResponseInterceptor (L230)
├── QueryMapEncoder (L234) / ExceptionPropagationPolicy (L241)
└── ⚠ Map<Capability> (L247) — 能力集 (CachingCapability 缓存面!)

configureUsingProperties (L146-190) — Properties 配置面 (OF-7 关联):
└── defaultConfig + contextId 双配置合并 → builder
```

## 关键设计 (why)

1. **Builder 来自子上下文**: get(Feign.Builder.class) — 每客户端独立 Builder (配置隔离 OF-7)
2. **组件可覆盖**: 子上下文组件 (getInheritedAware) 优先, 缺省回退默认 — 配置继承语义
3. **Capability 能力面**: 3.x 新增 — 缓存 (CachingCapability) 等能力注入点
4. **双配置源**: 注解 (configureUsingConfiguration) + Properties (configureUsingProperties) — 声明与配置分离

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| feign() 5 组件 | FeignClientFactoryBean.java:120-145 |
| configureUsingConfiguration 9+Capability | FeignClientFactoryBean.java:192-260 |
| configureUsingProperties | FeignClientFactoryBean.java:146-190 |

## 负面空间 (Q2 面)

- 不组件缓存 (每次构建重新获取)
- 不强制组件齐全 (缺省由 feign 默认)
- 不做能力自动发现 (Capability 显式注册)
