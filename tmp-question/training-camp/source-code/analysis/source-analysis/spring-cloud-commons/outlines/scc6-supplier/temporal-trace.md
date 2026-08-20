# SCC-6 ServiceInstanceListSupplier 体系 — 时空溯源 (@since 注释锚, git shallow)

> git shallow (单提交) 无法考古; 溯源以 @since 注释 + 类名演进为锚。

## 演进链 (@since 实证)

| 版本 | 事件 | 证据 |
|:--:|:--|:--|
| **2.2.0** | **核心族诞生**: ServiceInstanceListSupplier + DiscoveryClient/Delegating/Caching/HealthCheck Supplier | 各核心类 @since 2.2.0 |
| 2.2.7 | SameInstancePreferenceServiceInstanceListSupplier (同实例偏好) | @since 2.2.7 |
| 3.0.0 | RequestBasedStickySession + RetryAware Supplier (粘性会话/重试感知) | @since 3.0.0 |
| 3.0.2 | HintBasedServiceInstanceListSupplier (hint 提示) | @since 3.0.2 |
| 4.1.0 | SubsetServiceInstanceListSupplier (子集采样) | @since 4.1.0 |

## 版本相关性结论

- **2.2.0 是体系奠基** — 接口 + 基底 + 缓存 + 健康检查一次性建立, 之后渐进扩展
- **策略类 Supplier 按需添加** — SameInstance (2.2.7) → StickySession/RetryAware (3.0.0) → HintBased (3.0.2) → Subset (4.1.0) — 每版本加一两个
- **Builder 与核心同生** — ServiceInstanceListSupplierBuilder 从 2.2.0 就有, with 方法随新 Supplier 增长 (20+)
- **响应式架构贯穿** — Flux\<List\> + CacheFlux + Flux.interval 从诞生即响应式 (loadbalancer 模块定位)
