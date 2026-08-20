# 闭环笔记 Q2 — AutoConfiguredLoadBalancerFactory: 策略委托链

假设: 默认策略工厂是"策略选择器" — 注册表找 provider, 按 service config 的 loadBalancingPolicy 切换 delegate。

验证过程:
- **构造** (AutoConfiguredLoadBalancerFactory.java:41-49): LoadBalancerRegistry + defaultProvider (registry.getProvider(defaultPolicy))
- **找不到策略兜底** (L51-56): `"Could not find policy '%s'. Make sure its implementation is either registered to LoadBalancerRegistry or included in META-INF/services/io.grpc.LoadBalancerProvider"` → **FixedPickerLoadBalancerProvider(TRANSIENT_FAILURE)** — 明确报错
- **newLoadBalancer** (L68): 返回 AutoConfiguredLoadBalancer (delegate = defaultProvider 的 LB, L81)
- **运行时切换** (L89-105): `acceptResolvedAddresses` → `(PolicySelection) resolvedAddresses.getLoadBalancingPolicyConfig()` → 策略不同 → `delegate = delegateProvider.newLoadBalancer(helper)` (L105) — **service config 的 loadBalancingPolicy 字段驱动换策略**
- parseLoadBalancingPolicyConfig (L186-191): ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig
- 测试: defaultIsPickFirst (AutoConfiguredLoadBalancerFactoryTest.java:130)

代码类型: Implementation (策略委托)

结论: AutoConfigured = **策略选择器**: 默认 PickFirst (1.83 默认, 测试实证), service config 可换; 换策略在 acceptResolvedAddresses 时原子切换 delegate; 找不到策略 → TRANSIENT_FAILURE (快速失败不静默)。**被放弃的方案: 单例策略** — 通道级策略可配 (xds 通道换 xds 策略); provider SPI (META-INF/services) 让自定义策略零改动接入。 [跨域: G-3 exitIdleMode 创建时机; G-7 xds 策略经同一 SPI 注册] [模式: 策略工厂] (AutoConfiguredLoadBalancerFactory.java:41-105,186-191)
