# ALI-A6 Sentinel 三路限流 — 知识规划 (KP)

> 🔴 A | 模块: spring-cloud-starter-alibaba-sentinel (21) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 定义期注解发现 | SentinelBeanPostProcessor:66-79 | MergedBeanDefinition + 双路径 (issue#3329) |
| 2 | 三型强校验 | SentinelBeanPostProcessor:94-174 | blockHandler/fallback/urlCleaner 签名硬校验 |
| 3 | 动态拦截器注册 | SentinelBeanPostProcessor:176-217 | 编码 Bean 名 + add(0) |
| 4 | 双 entry 分级 | SentinelProtectInterceptor:59-84 | host 级 + host+path 级 |
| 5 | 异常分流 | SentinelProtectInterceptor:118-142 | Degrade→fallback / Flow→blockHandler |
| 6 | Feign 接管 | SentinelFeign:59-131 | internalBuild 覆写 + 锁死 invocationHandlerFactory |
| 7 | Feign 代理限流 | SentinelInvocationHandler:94-141 | HardCodedTarget + METHOD:url+path + fallbackFactory |
| 8 | fallback 解析 | SentinelFeign:106-159 | FeignClientFactoryBean + getFromContext + assignable 校验 |
| 9 | Web 组装 | SentinelWebAutoConfiguration:66-94 | 拦截器 + 三选 BlockExceptionHandler |
| 10 | 常量面 | SentinelConstants | PROPERTY_PREFIX/COLD_FACTOR/BLOCK_TYPE |

## 02 高频坑

1. blockHandler 必须静态方法 + 4 参签名 (HttpRequest, byte[], Execution, BlockException)
2. Degrade 走 fallback, Flow 走 blockHandler — 别混
3. 双 entry 的 exit 顺序: 先 path 后 host
4. 无 fallback 时 Feign 抛异常, 无 blockHandler 时空响应 (RestTemplate)
5. Feign 只有单级资源 (METHOD:url+path), RestTemplate 双级
6. SentinelFeign.Builder 禁止用户 invocationHandlerFactory
7. Web 路 filter.enabled 默认 true, 可关
8. urlCleaner 特例签名 (String)→String

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| Bean 生命周期 | MergedBeanDefinitionPostProcessor / postProcessAfterInitialization / 动态 BeanDefinition |
| 反射校验 | getStaticMethod / 返回类型 / 参数签名 / fail-fast |
| 资源 | host 级 / host+path 级 / urlCleaner / METHOD:url 同构 |
| 降级 | Degrade→fallback / Flow→blockHandler / fallbackFactory.create(ex) / 空响应兜底 |
| 代理 | Feign.Builder 覆写 / InvocationHandler / HardCodedTarget |
| 装配 | 三条件 (Web/Class/Property) / Optional 扩展点 / 三选 Block 处理器 |

## 04 跨域桥接

- ← SCC-13: FeignClientFactory.getInstance (NamedContextFactory 消费)
- → Sentinel 5.9: SphU.entry/BlockException 内核
- → OpenFeign 5.6: FeignClientFactoryBean/fallback 配置面
- → 面试: "Sentinel 怎么限流" — 三路 + 双 entry + 异常分流
