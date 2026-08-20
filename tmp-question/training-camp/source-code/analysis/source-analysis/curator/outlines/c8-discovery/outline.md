# C-8 服务发现 — 注册表之上的选择器: 缓存、过滤与熔断

> 前置: [[C-1-CuratorFramework]] (fluent) + [[C-5-缓存与监听]] (CuratorCache) + [[C-7-持久节点与组成员]] (临时节点注册) | 引出: 阶段5 Spring Cloud Zookeeper (服务发现消费面) | 对照: Nacos 注册中心 + Eureka
> 🟡 B | 5 KP | [模式: 注册表 + 缓存 + 过滤链 + 策略]
> Pass 2 闭环: q1(注册) q2(缓存) q3(选择) q4(降级)

**读者处境**: 服务 100 个实例, 调用方怎么拿到"当前在线"的列表? 拿到后选哪个? 调挂了要不要立刻换下一个?

### 1. 注册 — 临时节点 + 重连重注册

场景: 实例启动时注册, 崩溃后自动下线
源码路径:
- services ConcurrentMap + Entry{service, cache} (ServiceDiscoveryImpl.java:85-89)
- registerService: putIfAbsent → 新条目建 NodeCache → 内部创建临时节点 (ServiceDiscoveryImpl.java:178-190)
- updateService: 同步锁内更新 + setData, MAX_TRIES=2 (ServiceDiscoveryImpl.java:192-210); unregisterService (ServiceDiscoveryImpl.java:244+)
- **start() → reRegisterServices** (ServiceDiscoveryImpl.java:141-148): 注册失败的实例重连后自动重注册 — 会话语义兜底
关键设计 (q1): **注册=创建临时节点, 下线=会话回收** — 服务端无需主动探活 (对比 Nacos 心跳); reRegister 兜住"启动时 ZK 未就绪"。 [模式: 临时节点注册]

### 2. 发现缓存 — ServiceCache 复用 CuratorCache

场景: 客户端要"秒读"实例列表, 不能每次 getChildren
源码路径:
- ServiceCacheImpl (ServiceCacheImpl.java:198) 基于 **CuratorCacheBridge** (C-5) 监听服务路径 (ServiceProviderImpl.java:77)
- getInstances() 返回缓存快照; ServiceCacheListener 实例变更通知
关键设计 (q2): **读路径零 ZK 往返** — 数据面全部走 C-5 缓存机制; 这就是为什么 C-5 是 8 域中的枢纽。 [模式: 缓存读]

### 3. 选择 — Provider 组合与策略

场景: 拿 5 个实例, 调哪个?
源码路径:
- ServiceProviderImpl 组合四件套 (L41-47): **ServiceCache + FilteredInstanceProvider + ProviderStrategy + DownInstanceManager**
- 过滤链 (ServiceProviderImpl.java:79-82): 用户 filters + **DownInstanceManager + isEnabled** 恒等过滤
- getInstance() → providerStrategy.getInstance (ServiceProviderImpl.java:125-127)
- 策略: RandomStrategy / RoundRobinStrategy / StickyStrategy
- 注释: 快照勿持有, 每次现取 (L106-108)
关键设计 (q3): **过滤与策略分离** — 过滤决定"可用集", 策略决定"选谁"; Sticky 是 JVM 内粘性 (同类实例复用减少重建开销)。 [模式: 策略模式]

### 4. 降级 — noteError 熔断

场景: 选中的实例调挂了, 立刻换还是再试?
源码路径:
- **noteError(instance) → downInstanceManager.add** (ServiceProviderImpl.java:130-132)
- DownInstanceManager: Status{startMs, errorCount} (DownInstanceManager.java:37-39); **apply 过滤器: 错误数达阈值或超窗口 → 排除** (DownInstanceManager.java:56-60); 周期 purge (L63+)
- DownInstancePolicy: errorThreshold + windowMs
关键设计 (q4): **调用方反馈驱动降级** — 错误计数在窗口内累积, 达阈值即从"可用集"剔除; 窗口过期自动恢复 — 客户端级熔断, 不依赖注册中心。 [模式: 计数熔断]

### 5. 组合全景 — 一条完整链路

场景: 从注册到调用的全链路
源码路径:
- registerService → (临时节点) → ServiceCache (持久 watcher 同步) → FilteredInstanceProvider (过滤 down/enabled) → ProviderStrategy (选一个) → 调用 → 失败 noteError → DownInstanceManager 剔除 → 下一个
- 序列化: JsonInstanceSerializer; URI 模板: UriSpec (L326)
关键设计 (q1): **每个环节都是前面域的配方组合** — 临时节点 (C-7 语义) + 缓存 (C-5) + 锁无关; 这是 Curator 配方体系的应用层收束。 [模式: 组合收束]

## 代码类型
Architecture (分布式原语)

## 负面空间 — Curator 服务发现刻意不做的事

- **不做主动健康检查**: 只依赖 ZK 会话 (对照 Nacos 心跳探活 / Eureka 续约)
- **不做权重/机房亲和**: 策略无权重、无 zone
- **不做注册中心级多活**: 单 ZK 集群, 无跨集群同步
- **不做灰度/版本路由**: 无 metadata 路由规则引擎
- **不做本地故障切换缓存**: 注册中心全挂则无实例可用 (对照 Eureka self-preservation)

→ 引出: 阶段5 Spring Cloud Zookeeper 怎么消费这套 API?
