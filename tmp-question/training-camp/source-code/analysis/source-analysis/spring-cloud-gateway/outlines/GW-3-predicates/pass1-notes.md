# GW-3 Pass 1 扫描笔记 — 路由谓词

> 日期: 2026-08-16 | 版本: 4.3.2 | 🔴 A | 模块: handler/predicate/ (18 文件: 14 种工厂 + Abstract + RoutePredicateFactory/GatewayPredicate 接口 + PredicateDefinition)

## 继承树/调用图

```
RoutePredicateFactory<C> (34, extends ShortcutConfigurable + Configurable)
  ├── PATTERN_KEY (L38) / apply(Consumer) DSL (L42-47) / applyAsync(Consumer) (L49-53)
  ├── applyAsync(config) 默认 = toAsyncPredicate(apply) (L70-71)
  ├── name() = normalizeRoutePredicateName (L74)
  └── 14 种实现 (AbstractRoutePredicateFactory 基类):
      ├── Path (PathPattern 匹配) / Header / Cookie / Host / Method / Query
      ├── RemoteAddr / XForwardedRemoteAddr (IP)
      ├── Weight (跨实例权重) / After / Before / Between (时间)
      ├── ReadBody / CloudFoundryRouteService
GatewayPredicate (接口: test + traceMatch)
PredicateDefinition (name + args — 配置形态)
```

## 基本元素分解

1. **RoutePredicateFactory SPI**: apply(config) 同步 + applyAsync 异步 (默认适配); DSL 版 apply(Consumer)
2. **Path 谓词**: Spring PathPattern (basePath/尾斜杠/多 pattern) + exchange 属性缓存 PathContainer
3. **头/IP 族**: Header/Cookie/Host 值匹配; RemoteAddr CIDR
4. **Weight/时间族**: 权重路由 + After/Before/Between

## 标记问题 (6)

1. **Q1 SPI 双通道**: apply/applyAsync — 同步 vs 异步谓词; AsyncPredicate 适配 (toAsyncPredicate)
2. **Q2 Path 匹配**: PathPattern 语义 (basePath/尾斜杠/多 pattern/缓存 PathContainer)
3. **Q3 头/值族**: Header/Cookie/Host 匹配模式 (正则?通配?)
4. **Q4 IP 族**: RemoteAddr/XForwardedRemoteAddr — CIDR 匹配
5. **Q5 Weight**: 跨实例权重路由 (WeightConfig)
6. **Q6 时间族**: After/Before/Between — ZonedDateTime 解析

## 已读测试

- PathRoutePredicateFactoryTests: pathRouteWorks (L52)/trailingSlashReturns404 (L57)/mulitPathRouteWorks (L86)/mulitPathDslRouteWorks (L93)
