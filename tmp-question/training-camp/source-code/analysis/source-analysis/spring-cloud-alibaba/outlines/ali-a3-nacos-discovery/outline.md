# ALI-A3 Nacos 服务发现+注册 — Nacos 版 DiscoveryClient/ServiceRegistry 的完整实现

> 前置: [[SCC-3-服务发现抽象]] + [[SCC-4-服务注册抽象]] (Commons 双 SPI 契约) | 引出: [[ALI-A4-NacosLoadBalancer]] (消费发现) + [[ALI-A5-容错心跳]] (ServiceCache 延伸) | 对照: 规划 A-3 (服务发现+注册合并域)
> 🔴 A | 方案 A (全深度) | 闭环: q1(发现双链) q2(注册仪式) q3(容错缓存) q4(健康过滤)

**读者处境**: Nacos 里的服务实例列表怎么变成 RestTemplate 能用的 ServiceInstance? 应用启动后实例怎么"自动"注册进 Nacos? Nacos 挂了为什么还能拿到上次的实例列表? 实例的 metadata 里的 nacos.weight/nacos.healthy 是谁塞进去的?

### 1. 发现双链 — NacosDiscoveryClient 的 Remote+Cache 双通道

场景: getInstances 的返回值从哪来? 失败时怎么办?
源码路径:
- **NacosDiscoveryClient** (discovery/NacosDiscoveryClient.java:36): implements DiscoveryClient (SCC-3 契约) — DESCRIPTION = "Spring Cloud Nacos Discovery Client" (L43)
- **getInstances 双通道** (L59-75): ① 成功 → serviceDiscovery.getInstances + **ServiceCache.setInstances 写缓存** (L64, Optional.map 链路) ② 失败 → **failureToleranceEnabled (默认 false, L47-48) ? ServiceCache.getInstances 读缓存 : throw RuntimeException** (L68-74)
- **getServices 同构** (L77-90): 成功写缓存 / 失败 → 容错 ? cache : **Collections.emptyList()** (L87-88, 静默空 — 与 getInstances 抛异常不同!)
- **NacosServiceDiscovery.getInstances** (NacosServiceDiscovery.java:56-61): `namingService.selectInstances(serviceId, group, true)` (true=健康过滤由 Nacos 服务端做) → hostToServiceInstanceList
关键设计 (q1): **"双通道 = 写穿缓存 + 失败降级"** — 缓存不是主动维护而是**读时写穿** (getInstances 成功即回写), 失败时容错开关决定抛异常 (默认, 快速失败) 还是吃缓存 (容错模式)。 [模式: 写穿缓存]

### 2. 健康过滤与元数据 — hostToServiceInstance 的转换契约

场景: Nacos 的 Instance 对象怎么变成 Spring Cloud 的 ServiceInstance?
源码路径:
- **hostToServiceInstance** (NacosServiceDiscovery.java:87-114): **双过滤** (L89): `instance == null || !isEnabled() || !isHealthy()` → null — 客户端侧二次过滤 (服务端已过滤一次)
- **六键元数据注入** (L98-106): nacos.instanceId / nacos.weight / nacos.healthy / nacos.cluster / nacos.ephemeral + 用户 metadata putAll 合并 (L103-105)
- **secure 回读** (L109-112): metadata 含 "secure" → isSecure 设置 — 协议选择 (http/https)
- NacosServiceInstance 继承 DefaultServiceInstance (SCC-4 的 ServiceInstance 实体)
关键设计 (q2): **"过滤两段式 + 元数据命名空间化"** — 服务端 selectInstances(true) 一次, 客户端 !enabled/!healthy 二次 — 双保险; 元数据以 "nacos." 前缀命名空间隔离, 用户元数据 putAll 在后覆盖。 [模式: 命名空间元数据]

### 3. 注册仪式 — NacosServiceRegistry 五方法 + 触发链

场景: register/deregister/setStatus/getStatus 怎么落到 NamingService?
源码路径:
- **NacosServiceRegistry** (registry/NacosServiceRegistry.java:41): implements ServiceRegistry<Registration> (SCC-4 契约, 5 方法全实现)
- **register** (L59-89): serviceId 空 → warn 不注册 (L62-65) → getNacosInstanceFromRegistration (L177-187: ip/port/weight/clusterName/enabled/metadata/ephemeral) → `namingService.registerInstance(serviceId, group, instance)` (L74) → 失败 **failFast ? rethrowRuntimeException : warn** (L78-88)
- **deregister** (L91-115): deregisterInstance(serviceId, group, host, port, clusterName)
- **setStatus UP/DOWN** (L127-154): 只认两态 (L130-134) → **setEnabled 翻转后重新 registerInstance** (L140-148) — 状态即 enabled 标志
- **getStatus** (L156-175): getAllInstances 遍历 **ip+port 双匹配** (L165-166) → enabled ? UP : DOWN; 无匹配 → null
- **触发链**: NacosAutoServiceRegistration extends AbstractAutoServiceRegistration (SCC-4) — getRegistration 端口仲裁 (L56-61: registration.port<0 用 WebServer 端口 + Assert "service.port has not been set") + register 的 registerEnabled 守卫 (L70-79) + **getManagementRegistration = null** (L64-67, Nacos 无独立管理注册) + **@EventListener NacosDiscoveryInfoChangedEvent → restart** (L107-115)
关键设计 (q3): **"状态 = enabled 标志翻转"** — Nacos 没有独立状态字段, UP/DOWN 用 enabled 表达, 翻转后重新注册实例 (服务端幂等); getStatus 靠 ip+port 反查。 [模式: 状态即标志]

### 4. ServiceCache — 静态写穿缓存与 unmodifiableList

场景: 容错缓存的数据结构长什么样?
源码路径:
- **ServiceCache** (discovery/ServiceCache.java:41): 静态 services (L46, 默认 emptyList) + **instancesMap ConcurrentHashMap** (L48)
- **setInstances 包装** (L55-57): `Collections.unmodifiableList` — 读方不可变
- **getInstances 兜底** (L64-67): 缺 key → emptyList (Optional.orElse)
- **API 演进**: set/get @Deprecated `since 2021.0.1.1` (L72/L91) → setServiceIds/getServiceIds (L84/L103)
- **不实时性注释** (L34-36): "not real-time, depends on getServices/getInstances invoke" — 明确声明非实时
关键设计 (q4): **"写穿 + 不可变 + 非实时"三声明** — 缓存语义写死在 javadoc: 不主动更新, 不保证实时, 读方只读。 [模式: 显式非实时缓存]

### 5. NacosServiceManager — NamingService 双检锁单例与优雅关闭

场景: NamingService 谁创建? 关闭时怎么收尾?
源码路径:
- **volatile + 双检锁** (NacosServiceManager.java:40/86-95): namingService 懒建 — createNamingService 失败 → RuntimeException (L97-104)
- **nacosServiceShutDown** (L115-124): shutDown + 置 null — 可重建 (关停后能再拉起来)
- **isNacosDiscoveryInfoChanged** (L66-73): equals 比较 — 配置变更检测 (A5 域衔接)
- 双服务: NamingService + NamingMaintainService (L42/59-64, 维护面单独懒建)
关键设计 (q5): **"服务管理器 = 进程级命名服务门面"** — 懒建 + 双检 + shutDown 复位, 生命周期全包; 与 ConfigManager 的静态单例不同, 这里是 volatile 实例字段 (可关停重建)。 [模式: 可复位单例]

### 6. NacosRegistration — metadata 组装仪式 (management + 心跳保活键)

场景: 注册进 Nacos 的 metadata 里 management.port 和 heart-beat 键哪来的?
源码路径:
- **NacosRegistration** (registry/NacosRegistration.java:39): implements Registration — 四常量 (L44-59: MANAGEMENT_PORT/CONTEXT_PATH/ADDRESS/ENDPOINT_BASE_PATH)
- **init 三段组装** (L75-113): ① management 面: endpointBasePath + ManagementServerPortUtils.getPort (L81-88) + contextPath/address (L89-97) ② **心跳保活键**: HEART_BEAT_INTERVAL/TIMEOUT/IP_DELETE_TIMEOUT (L100-111, PreservedMetadataKeys) ③ **customizer 链** (L112 + L115-122: NacosRegistrationCustomizer 逐个 customize)
- 端口来源: getPort → nacosDiscoveryProperties.getPort (L135-137) — @PostConstruct 由 WebServer 端口回填
关键设计 (q6): **"注册信息 = 配置面 + 运行面 + 扩展面"** — management 元数据让健康检查走独立端口, 保活键让 Nacos 服务端调心跳参数, customizer 让用户改注册信息 — 三面组装, 一处 init。 [模式: 元数据组装链]

### 7. 装配面与条件 — 10 个 AutoConfiguration 的分工

场景: 哪个条件决定 Nacos 发现启用?
源码路径:
- **AutoConfiguration.imports 10 配置** (discovery/registry/loadbalancer/reactive/configclient/util 各司其职)
- **条件双闸**: @ConditionalOnDiscoveryEnabled (Spring Cloud 全局开关) + **@ConditionalOnNacosDiscoveryEnabled** (Nacos 专属, discovery/NacosDiscoveryAutoConfiguration.java:32-33) + NacosDiscoveryClientConfiguration @ConditionalOnBlockingDiscoveryEnabled (L42, 阻塞/响应式分流)
- **ConditionalOnNacosDiscoveryEnabled** (NacosDiscoveryProperties + spring.cloud.nacos.discovery.enabled)
- 装配链: NacosDiscoveryClientConfiguration 装配 NacosDiscoveryClient (L59) + NacosWatch @ConditionalOnProperty watch.enabled (L60-61)
关键设计 (q7): **"条件叠加 = 全局开关 × 专属开关 × 模式开关"** — 三层条件决定装配与否, 与 SCC-3 的 autoRegister 属性分流互补。 [模式: 条件叠加装配]
