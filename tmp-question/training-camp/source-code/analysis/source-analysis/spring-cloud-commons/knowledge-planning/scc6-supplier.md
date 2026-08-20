# SCC-6 ServiceInstanceListSupplier 体系 — 知识规划 (KP)

> 域: SCC-6 | 级别: 🔴 | 方案: A | 大纲: outlines/scc6-supplier/outline.md (6 节)

## §01 域定位

ServiceInstanceListSupplier = LoadBalancer 实例列表的"加工流水线"。响应式 Supplier 接口 + Delegating 委托链 + 基底 (DiscoveryClient) + 缓存 (CacheFlux) + 健康过滤 (周期) + Builder 20+ 方法装配。

## §02 源文件清单

| 文件 | 职责 | 归属节 |
|:--|:--|:--:|
| core/ServiceInstanceListSupplier.java | 接口 + builder 工厂 | 1 |
| core/DelegatingServiceInstanceListSupplier.java | 委托基类 + 回调传递 | 2 |
| core/DiscoveryClientServiceInstanceListSupplier.java | 基底 + timeout 30s | 3 |
| core/CachingServiceInstanceListSupplier.java | CacheFlux 响应式缓存 | 4 |
| core/HealthCheckServiceInstanceListSupplier.java | 周期健康过滤 + 双 Repeat | 5 |
| core/ServiceInstanceListSupplierBuilder.java | 20+ with 链式装配 | 6 |
| core/ 其余 9 个 Supplier | 扩展策略 (SCC-12) | 6 |

## §05 闭环要点 (Pass 2 内化)

### q1 接口面
Supplier\<Flux\<List\>\> (L33) + getServiceId (L35) + get(Request) default (L37-39) + builder (L41)。

### q2 委托链
Delegating (L32): delegate 字段 (L35) + getServiceId 委托 (L47-48) + selectedServiceInstance 传递 (L52-55)。

### q3 缓存/健康
Caching: CacheFlux.lookup + onCacheMissResume (L70); HealthCheck: refetchInstances (L71-72) + repeatHealthCheck (L91-92) 双周期。

### q4 Builder
20+ with 方法 (L74-352) + with(DelegateCreator) 自定义 (L352)。

## §06 负面空间 (6 条)

不做实例排序/选择 / 不做多注册中心聚合 / 不做权重内建 / 不做健康协议内建 / 不做缓存淘汰内建 / 不做故障转移

## §07 交叉引用

- ← SCC-3 服务发现 (DiscoveryClient 实例源) + SCC-13 NamedContextFactory
- → SCC-7 ReactorLoadBalancer (消费实例) + SCC-12 扩展策略
- 另见: Ribbon ServerList (历史对照)
