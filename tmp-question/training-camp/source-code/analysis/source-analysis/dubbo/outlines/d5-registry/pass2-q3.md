# D-5 注册中心 — Pass 2 闭环 Q3: 订阅通知面 (RegistryDirectory 动态目录)

> 核心: RegistryDirectory (integration/) — D-3 黑盒钩子的实现 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: provider 地址变化怎么驱动消费者 invoker 更新? 空保护/增量更新怎么做的?**

## 机制链 (已实证)

```
RegistryDirectory.subscribe (L130) — D-3 引用时注册中心订阅 (首次通知阻塞)
RegistryDirectory.notify (L200-237) — 变更推送处理:
├── 分类分组: groupingBy(judgeCategory) → providers / routers / configurators / consumers
├── configurators → Configurator.toConfigurators (配置覆盖规则, 如权重/禁用)
├── routers → toRouters + addRouters (路由规则, D-7 router 面预装)
├── providers → **AddressListener 链** (3.x 扩展点: 接口在 dubbo-cluster org.apache.dubbo.registry.AddressListener, 无内置实现 — 纯 SPI 扩展面; DynamicDirectory L345-347 同款用法)
└── refreshOverrideAndInvoker(providerURLs) → refreshInvoker

refreshInvoker (L275-387) — 动态 invoker 更新核心:
├── **EMPTY_PROTOCOL 空保护**: 注册中心推 empty:// → forbidden=true + destroyAllInvokers
│   (⚠ empty:// 来源: AbstractRegistry.filterEmpty L179-182 — 订阅结果空列表 → empty URL)
│   (注册中心正常清空 = 禁止访问, 防误连)
├── **cachedInvokerUrls 缓存兜底**: 收到空列表但历史有缓存 → 用缓存 (empty protection)
├── 去重 (duplicated urls 日志)
├── toInvokers(oldUrlInvokerMap, urls): URL → Invoker 增量转换
│   (已存在 URL 复用; **URL 参数变化才重建** — 增量更新)
├── newUrlInvokerMap 空 → 协议不一致保护 (consumer protocol vs provider protocol 错误日志)
├── multiGroup → toMergeInvokerList (多组合并)
├── refreshRouter (预路由) + setInvokers
├── destroyUnusedInvokers (关闭不再使用的 invoker)
└── invokersChanged() (通知变更)
```

## 关键设计 (why)

1. **动态目录 vs 静态目录 (D-3)**: StaticDirectory (D-3 引用侧, 固定列表) vs RegistryDirectory (本面, 订阅驱动) — 目录两种模式完整闭环
2. **EMPTY_PROTOCOL 空保护**: 注册中心"合法清空" (empty://) → 禁止访问; 与"异常丢数据" (缓存兜底) 区分 — 两个方向的容错
3. **增量更新**: toInvokers 对比 old/new — 未变 URL 复用 invoker, 变的重建, 删除的销毁 — 最小化重建开销
4. **分类处理**: 一次 notify 同时更新 configurators (配置) + routers (路由) + providers (地址) — 三分类联动
5. **AddressListener 扩展点**: 3.x 地址监听器链 (getActivateExtension) — 地址变换可插拔 (如 mesh 面)

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| subscribe (首次通知阻塞) | RegistryDirectory.java:130 |
| notify 分类处理 + AddressListener | RegistryDirectory.java:200-237 |
| refreshInvoker 空保护/缓存兜底/增量 | RegistryDirectory.java:275-387 |
| toInvokers 增量转换 | RegistryDirectory.java:452-519 |
| AbstractRegistry.notify 分类通知 | AbstractRegistry.java:545-587 |

## 负面空间 (Q3 面)

- 不推送全量 (注册中心推变更, 目录自己对比增量)
- 不做地址降级 (空保护直接 forbidden, 无降级模式 — 缓存兜底是另一条路)
- 不路由决策 (预路由只做缓存; 实际路由决策在 D-7 ClusterInvoker)
