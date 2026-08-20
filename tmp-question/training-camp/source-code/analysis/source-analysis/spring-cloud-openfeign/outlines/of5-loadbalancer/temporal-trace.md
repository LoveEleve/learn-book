# OF-5 负载均衡 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | FeignBlockingLoadBalancerClient 骨架: choose + reconstructURI + 503; FeignLoadBalancerAutoConfiguration |
| 2.1+ | RetryableFeignBlockingLoadBalancerClient (Spring Retry); LoadBalancerLifecycle 回调; XForwardedHeadersTransformer |
| 3.x | 装饰链明确 (Apache/Http2/OkHttp 配置类); OnRetryNotEnabledCondition; LoadBalancerResponseStatusCodeException (RetryableStatusCodeException) |
| 4.x | @ConditionalOnBean(LoadBalancerClient) 显式化; RetryableRequestContext |

## 痕迹证据

- FeignBlockingLoadBalancerClient.java:118: choose (2.x 锚)
- FeignBlockingLoadBalancerClient.java:127-131: 503 + "does not contain an instance" (2.x 锚)
- FeignBlockingLoadBalancerClient.java:147-154: transformers 链 (2.1+ 锚)
- RetryableFeignBlockingLoadBalancerClient.java:133-138: LoadBalancedRetryPolicy + RetryTemplate (2.1+ 锚)
- RetryableFeignBlockingLoadBalancerClient.java:227-238: 策略三态 (2.1+ 锚)
- FeignLoadBalancerAutoConfiguration.java:47-48: @ConditionalOnBean(LoadBalancer) (2.x 锚)
- OnRetryNotEnabledCondition.java:29: "spring.cloud.loadbalancer.retry.enabled is not set to false" (3.x 锚)

## 推断标注

- "2.x 骨架" — Spring Cloud OpenFeign 公知版本线 (标注)
- "2.1+ Retryable" — 类实证 (实证)
- "3.x 配置族" — 配置类实证 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- SCC C-7 LoadBalancer: choose/reconstructURI 抽象 — 底座对照 (SCC-PLAN 交叉)
- Feign 本体 Client: Client 接口 + 无 LB — 底座对照
- Dubbo D-6 负载均衡: 节点级选择 vs Feign 服务级 — RPC 对照
