# C-8 服务发现 — 知识规划 (KP)

> 域级: 🟡 B | 模块: curator-x-discovery/ (32 文件): ServiceDiscoveryImpl (267+) / ServiceProviderImpl 133 / ServiceCacheImpl 198 / DownInstanceManager 83 / FilteredInstanceProvider / JsonInstanceSerializer / UriSpec 326 / ServiceInstance 202+ / 策略 (RandomStrategy/RoundRobinStrategy/StickyStrategy/DominantStrategy?/PreferenceStrategy?) + DiscoveryPathConstructorImpl
> 日期: 2026-08-15 | 版本: 5.8.0

## 一、机制提取 (逐源)

### M1 ServiceDiscovery: 注册/更新/注销 (ServiceDiscoveryImpl)
- services ConcurrentMap<String, Entry>: Entry{service, cache CuratorCacheBridge} (L85-89)
- **registerService** (L178-190): putIfAbsent → 新条目建 NodeCache → internalRegisterService (创建临时节点)
- **updateService** (L192+): 同步锁内更新本地 service + setData; MAX_TRIES=2 重试 (L210)
- unregisterService (L244+); start: reRegisterServices (L141-148) — **重连后自动重注册**
- 节点结构: basePath/name/id (DiscoveryPathConstructorImpl)
- serializer: JsonInstanceSerializer 默认 (InstanceSerializer)

### M2 ServiceCache: 发现侧缓存 (198)
- 基于 **CuratorCacheBridge** (C-5 新一代) 监听服务名路径
- getInstances() 返回快照; 监听 ServiceCacheListener 实例变更 (L116-132 附近)
- 本地缓存 + watcher 更新 — 与 C-5 同构

### M3 ServiceProvider: 选择器 (133)
- **组合**: ServiceCache (缓存) + FilteredInstanceProvider (过滤链) + ProviderStrategy (策略) + DownInstanceManager (降级管理) (L41-47)
- 过滤链: 用户 filters + **DownInstanceManager + isEnabled** (L79-82)
- getInstance() → providerStrategy.getInstance (L125-127); **noteError → downInstanceManager.add** (L130-132)
- 注释: 实例快照不要持有, 每次现取 (L106-108, L118-120)

### M4 DownInstanceManager: 熔断计数 (83)
- statuses ConcurrentMap<instance, Status{startMs, errorCount}> (L33-38)
- add: 累计错误 (L46-50); **apply (过滤器): 错误数达上限或窗口内 → 排除** (L56-60); purge 周期清理 (L63+)
- DownInstancePolicy: errorThreshold + windowMs

### M5 策略 (strategies/)
- RandomStrategy / RoundRobinStrategy / StickyStrategy (JVM 内粘性)
- (Dominant/Preference 可能在新版本改为 providerStrategy 可插拔)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M1 注册/重注册 | P1 | 服务发现核心 |
| M3 Provider 组合 | P1 | 选择/降级链路 |
| M2 Cache | P2 | 复用 C-5 |
| M4/M5 | P2 | 降级与策略 |

## 三、负面空间

- **不做健康检查**: 只依赖会话临时节点 (无主动探活, 对照 Nacos 主动健康检查)
- **不做权重路由**: 策略无权重配置
- **不做跨机房亲和**: 无 zone 感知 (对照 Spring Cloud 的 zone)
- **不做注册中心集群内部一致**: 依赖 ZK 本身
