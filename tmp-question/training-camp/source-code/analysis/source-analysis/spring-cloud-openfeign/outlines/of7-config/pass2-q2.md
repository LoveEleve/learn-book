# OF-7 配置隔离 — Pass 2 闭环 Q2: 默认组件面 (FeignClientsConfiguration)

> 核心: 14 @Bean 默认组件 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 子上下文默认提供什么组件? 指标怎么装配?**

## 机制链 (已实证)

```
FeignClientsConfiguration (260 行) — 子上下文默认配置类 (super 构造传入):
├── 9 功能 Bean (L104-174): feignDecoder/feignEncoder/feignEncoderPageable/
│   feignQueryMapEncoderPageable/feignContract/feignConversionService/
│   feignRetryer/feignLoggerFactory/feignClientConfigurer
├── **3 个 Feign.Builder 变体** (L210-232): feignBuilder/defaultFeignBuilder/
│   **circuitBreakerFeignBuilder (OF-6!)** — @Scope("prototype")
├── **2 个 Micrometer Capability** (L247-254):
│   micrometerObservationCapability (ObservationRegistry 条件)
│   micrometerCapability (MeterRegistry 条件)
│   ← 指标面 (OF-9 指标并入本域!)
└── FeignFormatterRegistrar 注入 (L88) + feignConversionService 组装 (L152-160)

装配链 (OF-2 消费): 子上下文组件 → FeignClientFactoryBean.configureUsingConfiguration
```

## 关键设计 (why)

1. **默认组件齐全**: 14 Bean 覆盖编解码/契约/重试/日志/格式化 — 开箱即用
2. **Builder 三态**: 普通/默认/熔断 — 场景选择 (OF-2/OF-6 消费)
3. **指标条件装配**: Micrometer 2 Bean 按 Registry 条件 — 有监控依赖才激活
4. **prototype 作用域**: Builder 每客户端独立实例 — 配置隔离基础

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 9 功能 Bean | FeignClientsConfiguration.java:104-174 |
| 3 Builder 变体 | FeignClientsConfiguration.java:210-232 |
| 2 Micrometer Capability | FeignClientsConfiguration.java:247-254 |
| FeignFormatterRegistrar 注入 | FeignClientsConfiguration.java:88 |

## 负面空间 (Q2 面)

- 不组件自动发现 (显式 @Bean)
- 不 Builder 变体自动选 (配置驱动)
- 不指标默认开 (依赖条件)
