# ALI-A6 Sentinel 三路限流 — 时空溯源 (2025.0.0.0 实证)

> 仓库为浅克隆单提交 — 演进证据取自代码内注释 (issue 号/属性开关)

## 演化主线: 三路限流从"各自为政"到"注解统一"

| 面 | 机制 | 证据 (2025.0.0.0 源码) | 演进信号 |
|:--|:--|:--|:--|
| RestTemplate | @SentinelRestTemplate 注解 + MergedBeanDefinition 双路径 | SentinelBeanPostProcessor:68 "Fixes #3329: Support custom RestTemplate" | **issue#3329 修复**: 自定义 RestTemplate (非 @Bean 方法) 支持 |
| Feign | SentinelFeign.Builder 覆写 | SentinelFeign.java:47 (187 行) | invocationHandlerFactory 锁死 — 强制接管 |
| Web | SentinelWebInterceptor 组装 | SentinelWebAutoConfiguration (112 行) | 适配器版本化: adapter.spring.**webmvc_v6x** (Sentinel 仓库) |
| 降级 | blockHandler/fallback/urlCleaner 三型 | SentinelConstants BLOCK_TYPE/FALLBACK_TYPE/URLCLEANER_TYPE | 常量集中 + 校验统一 |
| 资源 | host 级 + host+path 级双 entry | SentinelProtectInterceptor:59-84 | 粗+细两级粒度 |

## 关键事件锚

- **issue#3329** (SentinelBeanPostProcessor.java:68): "Support custom RestTemplate" — 注解发现从单路径 (StandardMethodMetadata) 扩到双路径 (ResolvedFactoryMethod)
- **FEIGN_LAZY_ATTR_RESOLUTION** (SentinelFeign.java:49): `spring.cloud.openfeign.lazy-attributes-resolution` — 懒解析开关解决初始化顺序循环依赖 (L90-101)
- **webmvc_v6x 适配器包名** (SentinelWebAutoConfiguration import): Sentinel 适配 Spring 6 的版本化适配器 — 与 Boot 3 对齐的证据
- SentinelConstants.COLD_FACTOR = "3" (L28): 预热冷启动因子默认值

## 架构递进逻辑

```
早期: RestTemplate 拦截器手写 + Feign 用 Hystrix fallback 各自独立
中期: @SentinelRestTemplate 注解统一配置面 (blockHandler/fallback/urlCleaner)
后期: webmvc_v6x 版本化适配 + lazy-attributes 懒解析 (Boot 3 时代)
```
→ 演进方向: 三路限流共用一套注解语义与资源命名 (METHOD:url), 内核逐步迁移到 Sentinel 仓库的版本化适配器, 集成层退化为"配置组装"。
