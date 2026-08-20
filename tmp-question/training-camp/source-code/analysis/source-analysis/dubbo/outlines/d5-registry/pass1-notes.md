# D-5 注册中心 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-registry-api 704+ 行 / zookeeper / nacos)
> 09 域级审计: 执行计划 DB-5 断言 "Registry—ZookeeperRegistry/NacosRegistry—subscribe/notify" + PLAN §八 修正 (4 本地实现 + ServiceDiscoveryRegistry) — 已逐条 grep, 见文末审计表

## 入口展开 (Level-1~3, 已读源码)

### Level-1: RegistryService 接口契约 (RegistryService.java:29-93) — 五方法 + 契约注释

```
Registry extends Node, RegistryService (Registry.java:31-47)
register(url)    — 注册: check=false 后台重试 / dynamic=false 持久化 / category 分类 (providers 默认) / 同 URL 异参共存
unregister(url)  — 注销: 全 URL 匹配
subscribe(url, listener) — 订阅: category 分类 + 通配符 (interface=*&group=*...) / 网络抖动自动恢复 / 首次通知阻塞返回
unsubscribe(url, listener) — 退订
lookup(url)      — 拉模式查询 (返回 List<URL>, 与 push 对称)
```

### Level-2: RegistryFactory SPI + AbstractRegistry (支撑)

```
RegistryFactory SPI (internal 注册表实证):
├── zookeeper=ZookeeperRegistryFactory (dubbo-registry-zookeeper)
├── nacos=NacosRegistryFactory (dubbo-registry-nacos)
├── service-discovery-registry=ServiceDiscoveryRegistryFactory (3.x 应用级! D-11 深入)
└── wrapper=RegistryFactoryWrapper (D-1 Wrapper 织入)

AbstractRegistry (support/AbstractRegistry.java, 704 行) — 模板基类:
├── 本地文件缓存: loadProperties (L339) / saveProperties (L589-618, 通知后落盘)
│   └── "When our Registry has a subscribed failure due to network jitter, we can return at least the existing cache URL" (L579-581 注释)
├── notify (L545-587): 空列表忽略 (ANY_VALUE 除外) → 分类聚合 (providers/routers/configurators) → notified 缓存 → listener.notify
└── saveProperties 重试: savePropertiesRetryTimes + MAX_RETRY_TIMES_SAVE_PROPERTIES (L285-308)
```

### Level-3: FailbackRegistry — 失败重试模板 (support/FailbackRegistry.java)

```
"A template implementation of registry service that provides auto-retry ability" (L47 注释)
├── 4 种失败任务: FailedRegisteredTask / FailedSubscribedTask / FailedUnregisteredTask / FailedUnsubscribedTask (retry/ 包)
├── HashedWheelTimer retryTimer (L68) — 定时器, "regular check if there is a request for failure, and if there is, an unlimited retry" (L67 注释)
└── retryPeriod 重试周期 (L65)
```

### Level-4: RegistryDirectory — 动态目录 (D-3 黑盒钩子的实现!)

```
RegistryDirectory.subscribe (L130) → 注册中心 subscribe (首次通知阻塞)
RegistryDirectory.notify (L200-237) — 变更推送处理:
├── 分类分组: groupingBy(judgeCategory) → providers / routers / configurators / consumers
├── configurators → Configurator.toConfigurators (配置覆盖规则)
├── routers → toRouters + addRouters (路由规则, D-7 router 面)
├── providers → **AddressListener 链** (3.x getActivateExtension 消费, 地址监听器扩展点!)
└── refreshOverrideAndInvoker(providerURLs) → refreshInvoker (L275: 动态 invoker 重建)
```

## 09 域级审计表 (执行计划 DB-5 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "Registry—ZookeeperRegistry/NacosRegistry" | ZookeeperRegistryFactory/NacosRegistryFactory SPI 注册 (各模块 internal) | 接受 (PLAN 已修正: +Multicast/Multiple) |
| "subscribe/notify" | RegistryService.subscribe (契约注释阻塞首次通知) / AbstractRegistry.notify 分类通知 / RegistryDirectory.notify 三分类处理 | 接受 (执行计划未提: 分类 category 面 + AddressListener) |
| 执行计划未提: 失败重试 | FailbackRegistry + 4 任务 + HashedWheelTimer 无限重试 | 补锚 |
| 执行计划未提: 本地缓存 | AbstractRegistry saveProperties/loadProperties (网络抖动回退) | 补锚 |
| 执行计划未提: 应用级服务发现 | ServiceDiscoveryRegistryFactory (3.x) | 补锚 (D-11 深入) |
| 执行计划未提: 动态目录 | RegistryDirectory (D-3 黑盒钩子) — 分类/路由/配置/地址监听 | 补锚 |

## 待展开 (下一层)

1. ZookeeperRegistry 实现 (CuratorZookeeperClient: 节点监听/重连恢复)
2. NacosRegistry 实现 (Nacos 长轮询/监听)
3. refreshInvoker (L275): provider URL → invoker 转换 (toInvokers)
4. ServiceDiscoveryRegistry (3.x 应用级: 实例注册/订阅) — D-11 深入, 本域提钩子
5. NotifyListener 接口面 (notify(List<URL>))
