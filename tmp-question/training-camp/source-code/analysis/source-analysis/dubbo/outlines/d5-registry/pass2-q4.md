# D-5 注册中心 — Pass 2 闭环 Q4: 实现族对照 (ZK/Nacos/Multicast/Multiple)

> 核心: 4 本地实现 + ServiceDiscoveryRegistry 钩子 | 锚点已 grep 验证 (2026-08-16)

## 机制链 (已实证)

```
继承层次:
AbstractRegistry (本地缓存/分类通知, q1)
└── FailbackRegistry (失败重试, q2)
    ├── CacheableFailbackRegistry — ⚠ 注释 L73: "adds a URLAddress and URLParam cache to save RAM space"
    │   (URL 地址/参数去重缓存, 省内存面 — 非"缓存版重试"!) ──→ ZookeeperRegistry (479 行, dubbo-registry-zookeeper)
    ├── NacosRegistry (dubbo-registry-nacos)
    ├── MulticastRegistry (dubbo-registry-multicast)
    └── ServiceDiscoveryRegistry (3.x 应用级, D-11 深入)
└── MultipleRegistry (dubbo-registry-multiple, 多注册组合)

各实现核心:
├── ZookeeperRegistry (L59 extends CacheableFailbackRegistry):
│   ├── doRegister (L169) — ZK 节点创建 (临时节点, 连接断自动删 = dynamic 语义)
│   ├── doSubscribe (L191) — ChildListener 子节点监听 (computeIfAbsent 复用) + 通配订阅
│   ├── ZookeeperRegistryNotifier (L438-479) — **延迟节流**: 治理规则立即通知 / 地址通知 delayTime 合并防抖
│   └── ZK 连接断 → 临时节点自动删 (dynamic 语义自愈)
├── NacosRegistry (L94 extends FailbackRegistry):
│   ├── doRegister (L180) — Nacos 实例注册 (心跳续约)
│   ├── doSubscribe (L249-260) — NacosAggregateListener 聚合监听 (多服务名聚合!)
│   └── Nacos 长轮询订阅
├── MulticastRegistry (L71 extends FailbackRegistry):
│   └── MulticastSocket (L38) — 组播广播注册表 (无中心, 局域网)
├── MultipleRegistry (L43 extends AbstractRegistry):
│   └── MultipleNotifyListenerWrapper (L51,185) — 多注册中心通知聚合 (一订阅多中心合并)
└── ServiceDiscoveryRegistry (L75 extends FailbackRegistry):
    └── 注释 (L70-71): register() 聚合接口级数据到 MetadataInfo (与 MetadataService 交互);
        subscribe() 触发应用级服务发现完整流程 — 3.x 面, D-11 深入
```

## 关键设计 (why)

1. **实现即适配**: 4 实现共享同一契约 (register/subscribe), 差异只在"底层协调系统" — ZK 临时节点/心跳 vs Nacos 实例注册+长轮询 vs 组播广播 vs 多中心聚合
2. **CacheableFailbackRegistry 中间层**: ZK 走 URL 去重缓存 (URLAddress/URLParam cache, 注释 L73 "save RAM space") — 内存优化面
3. **Nacos 聚合监听**: NacosAggregateListener 把多 serviceNames 订阅聚合 — 适配 Nacos 无分层节点模型
4. **Multiple 多注册**: 一订阅多中心 (MultipleNotifyListenerWrapper 合并通知) — 跨注册中心容灾
5. **应用级服务发现**: ServiceDiscoveryRegistry 是 3.x 迁移目标 (接口级→应用级, D-3 MigrationInvoker + D-11 深入) — 本域埋钩子

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| ZookeeperRegistry 继承 + doRegister/doSubscribe | dubbo-registry-zookeeper ZookeeperRegistry.java:59,169,191 |
| ZookeeperRegistryNotifier | 同上 L438 |
| NacosRegistry 继承 + 聚合监听 | dubbo-registry-nacos NacosRegistry.java:94,249-260 |
| MulticastRegistry + MulticastSocket | dubbo-registry-multicast MulticastRegistry.java:71 |
| MultipleRegistry + 通知聚合 | dubbo-registry-multiple MultipleRegistry.java:43,51 |
| ServiceDiscoveryRegistry 应用级 | dubbo-registry-api client/ServiceDiscoveryRegistry.java:75 |

## 负面空间 (Q4 面)

- 不做注册中心内部选举/一致性 (依赖底层 ZK/Nacos)
- 不做跨中心数据同步 (Multiple 是消费端聚合, 非同步)
- 不实现组播的可靠投递 (MulticastRegistry 依赖 UDP 尽力而为 + FailbackRegistry 兜底)
