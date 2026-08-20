# D-5 注册中心 — 契约→缓存→重试→订阅→实现族

> 前置: [[D-1-SPI微内核]] [[D-2-服务导出]] [[D-3-服务引用]] | 引出: [[D-7-集群容错]] [[D-11-元数据]] | 对照: Nacos (NC 阶段) + ZK (阶段4.3) + Eureka 认知
> 🔴 A (🟡→🔴 升级) | 8 KP | [模式: 契约抽象 + 模板方法 + 动态目录]
> Pass 2 闭环: q1(抽象面) q2(失败重试) q3(订阅通知) q4(实现族)

**读者处境**: 注册中心怎么抽象出五方法契约? 失败/抖动怎么兜底? provider 地址变化怎么驱动消费者? 这篇拆 RegistryService + AbstractRegistry + FailbackRegistry + RegistryDirectory + 4 实现。

### 1. 抽象面 — RegistryService 契约 + AbstractRegistry 缓存

场景: 一个"注册中心"到底做什么?
源码路径:
- **RegistryService 五方法契约** (RegistryService.java:29-93): register/unregister/subscribe/unsubscribe/lookup — **注释即契约** (check=false 后台重试 / dynamic=false 持久化 / category 分类 / 通配符订阅 / 首次通知阻塞)
- **RegistryFactory SPI** (各模块 internal): zookeeper / nacos / service-discovery-registry (3.x) / wrapper (D-1 织入)
- **AbstractRegistry** (support/, 704 行): **本地文件缓存** loadProperties (L339) / saveProperties (L589-618) — 注释 L579-581 "网络抖动时返回缓存 URL"; **notify 分类聚合** (L545-587: providers/routers/configurators 分组 + notified 缓存 + 落盘)
关键设计 (q1): **契约驱动 + 本地缓存兜底抖动 + 分类存储**。[模式: 抽象面]

### 2. 失败重试面 — FailbackRegistry 模板

场景: 注册/订阅失败怎么办?
源码路径:
- **FailbackRegistry** (support/FailbackRegistry.java): 模板方法 (doRegister/doSubscribe...) + **4 种失败任务** (retry/: FailedRegistered/Subscribed/Unregistered/UnsubscribedTask — 继承 **AbstractRetryTask** 模板) + **HashedWheelTimer** (L68) + 注释 L67 "unlimited retry"; ⚠ **重试次数可配**: REGISTRY_RETRY_TIMES 默认 **-1 = 无限** (Constants.java:70), >0 时限制 (AbstractRetryTask L112)
- 继承层次: AbstractRegistry → FailbackRegistry → **CacheableFailbackRegistry** → ZookeeperRegistry
关键设计 (q2): **模板方法 + 4 任务分类 + 无限重试 (配合缓存抖动兜底)**。[模式: 重试面]

### 3. 订阅通知面 — RegistryDirectory 动态目录

场景: provider 地址变化怎么驱动消费者更新?
源码路径:
- **RegistryDirectory.subscribe** (L130): D-3 引用时订阅 (首次通知阻塞)
- **notify** (L200-237): 分类分组 → configurators (配置覆盖) + routers (路由, D-7 预装) + providers → **AddressListener 链** (3.x 扩展点) → refreshOverrideAndInvoker
- **refreshInvoker** (L275-387): **EMPTY_PROTOCOL 空保护** (empty:// → forbidden; ⚠ **empty:// 由注册中心侧生成**: AbstractRegistry.filterEmpty L179-182 空列表 → empty URL) / **cachedInvokerUrls 缓存兜底** (空列表用缓存) / 去重 / **toInvokers 增量更新** (L452-519: URL 参数变才重建) / 协议不一致保护 / multiGroup 合并 / destroyUnusedInvokers
关键设计 (q3): **动态目录 (订阅驱动) vs D-3 静态目录 + 空保护双方向容错 + 增量更新**。[模式: 目录面]

### 4. 实现族 — ZK/Nacos/Multicast/Multiple 对照

场景: 4 个实现差异在哪?
源码路径:
- **ZookeeperRegistry** (479 行): extends CacheableFailbackRegistry (L59); doRegister (L169 临时节点) / doSubscribe (L191 **ChildListener 子节点监听, computeIfAbsent 复用**) / **ZookeeperRegistryNotifier** (L438-479: **延迟节流** — 治理规则立即通知 / 地址通知按 delayTime 合并, 防抖)
- **NacosRegistry**: extends FailbackRegistry (L94); doRegister (L180 心跳续约) / doSubscribe (L249-260 **NacosAggregateListener 聚合**)
- **MulticastRegistry**: MulticastSocket 组播广播 (L38,71) — 无中心局域网
- **MultipleRegistry**: MultipleNotifyListenerWrapper 多中心通知聚合 (L43,51)
- **ServiceDiscoveryRegistry** (3.x 应用级, L75): register 聚合到 MetadataInfo / subscribe 触发应用级流程 — D-11 深入
关键设计 (q4): **契约共享 + 底层适配差异 (临时节点/心跳/组播/聚合) + 3.x 应用级钩子**。[模式: 实现族]

## 代码类型
Architecture (契约抽象) + Concurrency (目录更新/重试)

## 负面空间 (D-5, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不跨注册中心强同步 | 注册各自独立; 多注册由 Multiple 消费端聚合 (q1) |
| 不指数退避 | 固定 retryPeriod, 非退避 (q2) |
| 不持久化失败队列 | 重启丢失, 靠缓存+重订阅恢复 (q2) |
| 不推送全量 | 注册中心推变更, 目录对比增量 (q3) |
| 不地址降级 | 空保护直接 forbidden; 缓存兜底是另一条路 (q3) |
| 不注册中心内部选举 | 依赖底层 ZK/Nacos 一致性 (q4) |

## 结尾桥 OUTBOUND

- → [[D-7-集群容错]]: RegistryDirectory 预路由 → ClusterInvoker 路由决策深潜
- → [[D-11-元数据]]: ServiceDiscoveryRegistry 应用级服务发现 (接口级→应用级迁移完整收尾)
- → 对照: Nacos (NC-1~NC-7) — 注册中心实现面 vs Nacos 自身机制
