# D-5 注册中心 — 契约抽象与动态目录

> 项目: Dubbo | 🔴 Deep (🟡→🔴) / 1 篇 | RegistryService+AbstractRegistry(704)+FailbackRegistry+RegistryDirectory+ZookeeperRegistry(479)
> 基线: DUBBO-PLAN D-5 (治理) — 前置: **D-1 (Factory/Wrapper) + D-2/D-3 (注册契约/RegistryProtocol)** — 展开 契约→缓存重试→订阅通知→实现族

---

## §0.8

- 🔴 Deep，1篇 — 契约(**RegistryService L29-93: register/unregister/subscribe/unsubscribe/lookup 五方法+注释即契约[check=false 重试/dynamic 持久化/category 分类/通配/首次通知阻塞]**) → 工厂(**RegistryFactory SPI: zookeeper/nacos/service-discovery-registry/wrapper**) → 基类(**AbstractRegistry 704: 本地文件缓存 loadProperties L339/saveProperties L589-618[网络抖动兜底注释 L579-581]+notify 分类聚合 L545-587[providers/routers/configurators]+saveProperties 重试 L285-308**) → 重试(**FailbackRegistry: 模板方法+4 失败任务[FailedRegistered/Subscribed/Unregistered/UnsubscribedTask 继承 AbstractRetryTask]+HashedWheelTimer L68+默认无限重试[-1 Constants.java:70 可配限制 L112]**) → 动态目录(**RegistryDirectory: subscribe L130[首次通知阻塞]→notify L200-237[分类分组+configurators+routers+providers→AddressListener 链+refreshOverrideAndInvoker]→refreshInvoker L275-387[EMPTY 空保护[filterEmpty L179 信号源]+缓存兜底+增量 toInvokers L452-519]**) → 实现族(**ZookeeperRegistry 479[临时节点+ChildListener+Notifier 延迟节流 L438-479]/NacosRegistry[心跳+NacosAggregateListener]/MulticastRegistry[组播]/MultipleRegistry[多中心聚合]/ServiceDiscoveryRegistry[3.x 应用级 D-11]**)
- 设计模式: [模式: 契约抽象+模板方法+目录模式+失败重试]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RegistryService.java:29-93 | 契约 | **五方法+契约注释**: register 5 条 (check/dynamic/category/重启不丢/同 URL 异参共存) / subscribe 7 条 (分类/通配/自动恢复/阻塞) | High |
| AbstractRegistry.java:545-587 | 通知 | **notify 分类聚合**: 空列表忽略→分类分组→notified 缓存→listener.notify→localCacheEnabled 落盘 | High |
| AbstractRegistry.java:339,589-618 | 缓存 | **本地文件缓存**: 网络抖动时返回缓存 URL (注释 L579-581) | High |
| FailbackRegistry.java:47-68 | 重试 | **模板方法+4 任务+HashedWheelTimer+"unlimited retry"** (默认 -1, 可配限制) | High |
| AbstractRetryTask.java:112 | 重试次数 | **retryTimes > 0 && times > retryTimes → 停止** — 默认 -1 无限 | High |
| RegistryDirectory.java:200-237 | 目录 | **notify 三分类**: configurators 配置覆盖/routers 路由/providers→AddressListener 链 | High |
| RegistryDirectory.java:275-387 | 刷新 | **refreshInvoker**: EMPTY 空保护 (forbidden)/缓存兜底/去重/增量/协议不一致保护 | High |
| AbstractRegistry.java:179-182 | empty 源 | **filterEmpty: 订阅空列表→empty URL** — 空保护信号源 | High |
| ZookeeperRegistry.java:59,169,191 | ZK | **临时节点注册+ChildListener 子节点监听+Notifier 延迟节流 (L438-479)** | High |
| ServiceDiscoveryRegistry.java:75 | 应用级 | **3.x 应用级: register 聚合到 MetadataInfo/subscribe 应用级流程** (D-11 深入) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 注册中心单机制 (契约+重试+目录+实现) — 1篇按四层展开; RegistryProtocol 在 D-3 (导航), ServiceDiscoveryRegistry 在 D-11 (导航), 集群容错在 D-7 (导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 五方法契约 (注释即契约) | 🔴 | **为什么🔴**: 实现族行为规范 |
| P1-2 | 三层容错 (本地缓存/无限重试/缓存兜底) | 🔴 | **为什么🔴**: 高可用核心 |
| P1-3 | 动态目录 (分类+增量+空保护) | 🔴 | **为什么🔴**: 服务发现核心 |
| P1-4 | 分类存储 (providers/routers/configurators) | 🔴 | **为什么🔴**: 数据隔离 |
| P2-1 | 实现族差异 (ZK 临时节点/Nacos 心跳/组播) | 🟡 | **为什么🟡**: 适配面 |
| P2-2 | filterEmpty empty:// 信号源 | 🟡 | **为什么🟡**: 空保护语义 |
| P3-1 | AddressListener 3.x 扩展点 | 🟢 | **为什么🟢**: 扩展细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **契约+缓存** | 🔴 | 抽象层 |
| B | **失败重试** | 🔴 | 可靠性 |
| C | **动态目录** | 🔴 | 发现核心 |
| D | **实现族** | 🟡 | 适配层 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 抽象面 | 五方法契约 (注释即教科书): check=false 后台重试/dynamic=false 持久化/category 分类/通配订阅/首次通知阻塞; Factory SPI 4 项可插拔 | RegistryService.java:29-93 |
| q2 | 失败重试 | FailbackRegistry 模板方法 (doRegister 等) + 4 任务独立重试; HashedWheelTimer 定时检查; **重试次数默认 -1 无限, >0 时限制** (Constants:70 + AbstractRetryTask:112) | FailbackRegistry.java:47-68; AbstractRetryTask.java:112 |
| q3 | 订阅通知 | RegistryDirectory: 分类分组→configurators/routers/providers 三分类处理→AddressListener 链→refreshInvoker; **EMPTY 空保护 (filterEmpty 生成 empty://→forbidden) vs 缓存兜底 (异常丢数据用缓存) 双容错**; toInvokers 增量更新 (URL 参数变才重建) | RegistryDirectory.java:200-387; AbstractRegistry.java:179-182 |
| q4 | 实现族 | ZK 临时节点 (断连自动删=dynamic 语义)+ChildListener 复用+Notifier 延迟节流; Nacos 实例心跳+聚合监听; 组播无中心; Multiple 多中心聚合; ServiceDiscoveryRegistry 应用级 (D-11) | ZookeeperRegistry.java:59-479; NacosRegistry.java:94-260 |

→ 引出 D-7 集群容错 (预路由→路由决策); D-11 元数据 (ServiceDiscoveryRegistry 应用级收尾)。
