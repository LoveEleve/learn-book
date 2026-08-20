# D-5 注册中心 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: RegistryService 五方法 + AbstractRegistry (本地缓存) + FailbackRegistry (重试) + ZookeeperRegistry/DubboRegistry/RedisRegistry; RegistryDirectory 动态目录 (分类通知 + 增量更新) |
| 2.7.x | CacheableFailbackRegistry (缓存版重试); MetadataService (元数据中心, 2.7 迁移预备); ZK 实现为主 |
| 3.x | **ServiceDiscoveryRegistry + ServiceDiscoveryRegistryDirectory (应用级服务发现)** + MigrationInvoker (D-3) 迁移; AddressListener 地址监听扩展点; Multicast/Multiple 保留 |
| 3.3.x | RegistryDirectory refreshInvoker 空保护/缓存兜底注释完善; ZookeeperRegistry 479 行稳定 |

## 痕迹证据

- AbstractRegistry.java:579-581: "When our Registry has a subscribed failure due to network jitter, we can return at least the existing cache URL" (缓存兜底注释锚)
- FailbackRegistry.java:47: "A template implementation of registry service that provides auto-retry ability" + L67 "unlimited retry" (模板/无限重试注释锚)
- RegistryService.java:29-93: 五方法契约注释 (register 5 条/subscribe 7 条 — 2.x 契约锚)
- RegistryDirectory.java: L279-281 "empty protection" + L310 "Cached invoker urls" (3.x 注释锚)
- ServiceDiscoveryRegistry.java:70-71: "register() aggregates interface level data into MetadataInfo... subscribe() triggers the whole subscribe process of the application level service discovery model" (3.x 应用级锚)
- CacheableFailbackRegistry: 2.7+ 中间层 (类名实证)

## 推断标注

- "2.x 骨架" — Dubbo 2.x 公知版本线 (标注)
- "2.7.x CacheableFailbackRegistry" — 类存在性推断 (标注)
- "3.x 应用级" — ServiceDiscoveryRegistry 注释实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- ZK (阶段4.3 Z-3 DataTree/Z-6 Watcher): 数据树 + Watcher vs Dubbo ZK 适配 — 注册中心底层对照
- Nacos (NC-1~NC-7): NacosRegistry 实现 vs Nacos 自身注册/订阅 — 双视角对照
- Eureka 认知: 纯 AP 注册中心 vs Dubbo 抽象 (契约差异)
