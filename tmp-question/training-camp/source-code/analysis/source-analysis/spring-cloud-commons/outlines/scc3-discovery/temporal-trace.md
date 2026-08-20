# SCC-3 服务发现抽象 — 时空溯源 (代码注释锚, git shallow)

> git shallow (单提交) 无法考古; 溯源以 @Deprecated + probe 演进 + 实现族为锚。

## 演进链 (代码痕迹实证)

| 阶段 | 事件 | 证据 |
|:--:|:--|:--|
| 1.x | **DiscoveryClient 诞生** (Netflix Eureka 时期): description/getInstances/getServices 三方法 + DEFAULT_ORDER | DiscoveryClient.java:32-55 |
| 中期 | **CompositeDiscoveryClient 引入**: 多注册中心组合 + 排序短路 | composite/CompositeDiscoveryClient.java:36-56 |
| 2.x | **ReactiveDiscoveryClient**: Flux 响应式变体 + Composite/Simple 响应式版 | ReactiveDiscoveryClient + composite/reactive/ + simple/reactive/ |
| 当前 | **probe 演进**: DiscoveryClient.probe (default) / ReactiveDiscoveryClient.probe **@Deprecated(forRemoval=true)** → reactiveProbe() | ReactiveDiscoveryClient.java:69-79 |
| 当前 | **SimpleDiscoveryClient**: 属性驱动 (application.yml 实例列表) + order 可配 | simple/SimpleDiscoveryClient.java:34-65 |

## 实现族实证 (外部仓库)

- EurekaDiscoveryClient (Eureka 仓库) — 原始实现
- NacosDiscoveryClient (Nacos 仓库 5.8) — 现代实现
- ConsulDiscoveryClient / ZookeeperDiscoveryClient (各自仓库)

## 版本相关性结论

- **接口面自 1.x 稳定**: description/getInstances/getServices 三方法从未变 (实现族都依赖)
- **probe 是新演化**: 探活方法 (默认 getServices) — Reactive 版已标记 forRemoval 换成 reactiveProbe — 接口仍在演进
- **Composite 短路语义稳定**: 构造排序 + 第一个非空返回, 从引入至今未变
- **Simple 是"零依赖兜底"**: 属性驱动设计让无注册中心环境可用 — 测试/本地开发标配
