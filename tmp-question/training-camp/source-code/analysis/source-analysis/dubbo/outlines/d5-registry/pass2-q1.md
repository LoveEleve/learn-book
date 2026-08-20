# D-5 注册中心 — Pass 2 闭环 Q1: 注册中心抽象面 (RegistryService + AbstractRegistry)

> 入口: RegistryService 五方法契约 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 注册中心抽象出什么能力? 本地缓存怎么兜底网络抖动?**

## 机制链 (已实证)

```
RegistryService (RegistryService.java:29-93) — 五方法 + 契约注释 (注册契约 = 教科书!)
├── register(url): check=false → 后台重试不抛; dynamic=false → 持久化 (异常退出不删);
│   category 分类存储 (providers 默认); 注册中心重启/网络抖动数据不丢; 同 URL 异参共存
├── unregister(url): 全 URL 匹配; 持久数据找不到 → IllegalStateException
├── subscribe(url, listener): category 分类 + 通配符 (interface=*&group=*&version=*);
│   **网络抖动自动恢复订阅; 首次通知阻塞返回** (L85-88 契约注释)
├── unsubscribe(url, listener)
└── lookup(url): 拉模式 (与 push 对称)

Registry extends Node, RegistryService (Registry.java:31-47) + isServiceDiscovery() (3.x 应用级标记)

RegistryFactory SPI (internal 注册表实证):
├── zookeeper=ZookeeperRegistryFactory / nacos=NacosRegistryFactory (各模块)
├── service-discovery-registry=ServiceDiscoveryRegistryFactory (3.x 应用级, D-11 深入)
└── wrapper=RegistryFactoryWrapper (D-1 Wrapper 织入)

AbstractRegistry (support/AbstractRegistry.java, 704 行) — 模板基类:
├── 本地文件缓存: loadProperties (L339) / saveProperties (L589-618, 每次通知后落盘)
│   └── 注释 (L579-581): "When our Registry has a subscribed failure due to network jitter,
│        we can return at least the existing cache URL"  ← 缓存 = 抖动兜底
├── notify (L545-587): 空列表忽略 (ANY_VALUE 通配订阅除外) → 分类聚合 (providers/routers/
│   configurators) → notified 缓存 → listener.notify(categoryList) → localCacheEnabled 时 saveProperties
└── saveProperties 重试: savePropertiesRetryTimes + MAX_RETRY_TIMES_SAVE_PROPERTIES (L285-308)
```

## 关键设计 (why)

1. **契约即文档**: RegistryService 注释详细定义行为契约 (重试/持久化/分类/通配/阻塞) — 实现族只需遵守
2. **本地文件缓存**: 网络抖动/注册中心不可用时, 消费端用缓存 URL 继续工作 — 高可用兜底
3. **分类存储 + 分类通知**: providers/routers/configurators 三分类隔离 — 服务地址/路由/配置规则互不干扰
4. **Factory SPI + Wrapper**: 注册中心实现可插拔 (ZK/Nacos/Multicast) + Wrapper 织入统计等
5. **首次通知阻塞**: subscribe 同步等到第一波数据 — 避免启动竞态 (与 D-3 checkInvokerAvailable 呼应)

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| 五方法契约 (register 5 条/unsubscribe 2 条/subscribe 7 条) | RegistryService.java:29-93 |
| Registry 接口 + isServiceDiscovery | Registry.java:31-47 |
| Factory SPI 注册表 (zk/nacos/service-discovery/wrapper) | dubbo-registry 各模块 META-INF |
| 本地缓存 load/saveProperties | AbstractRegistry.java:339, 589-618 |
| 分类通知 + 缓存落盘 | AbstractRegistry.java:545-587 |
| saveProperties 重试 | AbstractRegistry.java:285-308 |

## 负面空间 (Q1 面)

- 不做跨注册中心强同步 (注册各自独立, 多注册由 Multiple 组合)
- 不做数据分片 (单注册中心全量)
- 不做本地缓存加密 (文件明文, 仅本地)
