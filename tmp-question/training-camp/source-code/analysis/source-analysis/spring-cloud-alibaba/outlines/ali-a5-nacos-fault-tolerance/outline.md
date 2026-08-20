# ALI-A5 Nacos 容错+心跳+优雅关闭 — 应用生命周期与 Nacos 的三段式陪伴

> 前置: [[ALI-A3-发现注册]] (ServiceCache/注册链消费) | 引出: [[Nacos-5.8]] (subscribe 内核) | 对照: SCC-3 HeartbeatMonitor / 健康检查双模式
> 🟡 B | 方案 B (重要域) | 闭环: q1(订阅回写) q2(心跳打点) q3(优雅关闭) q4(健康检查)

**读者处境**: Nacos 控制台改了实例 metadata, 本地 properties 怎么自动同步? "每 30 秒发一次 HeartbeatEvent"是干嘛的? 应用停机怎么保证先反注册再退出? /actuator/health 的 nacos 状态从哪来?

### 1. NacosWatch — 订阅服务变更, 回写自身 metadata

场景: 服务端改了实例 metadata, 本地怎么知道?
源码路径:
- **NacosWatch** (discovery/NacosWatch.java:45): SmartLifecycle + DisposableBean — **isAutoStartup=true** (L64) + running CAS (L76)
- **listenerMap.computeIfAbsent** (L77): key = `service:group` (buildKey L104-106) — 防重复订阅
- **subscribe** (L92-99): `namingService.subscribe(service, group, [cluster], listener)` (L94-95)
- **onEvent → selectCurrentInstance** (L80-88 + L114-119): NamingEvent 实例列表按 **ip+port 匹配自身** → **resetIfNeeded** (L108-112: metadata 不等 → 回写 properties.setMetadata)
- **stop 对称** (L122-136): unsubscribe + running CAS 翻转
关键设计 (q1): **"订阅 = 服务端变更推送, 本地只收自身"** — 订阅的是服务实例列表变更, 但只关心"我自己的 metadata 是否被改" (控制台运维操作); ip+port 匹配保证只看自己。 [模式: 订阅+自我过滤]

### 2. NacosDiscoveryHeartBeatPublisher — 心跳打点与三条件装配

场景: HeartbeatEvent 是谁发的? 什么时候发?
源码路径:
- **NacosDiscoveryHeartBeatPublisher** (discovery/NacosDiscoveryHeartBeatPublisher.java:39): ApplicationEventPublisherAware + SmartLifecycle — isAutoStartup=true (L85) + running CAS (L65)
- **scheduleWithFixedDelay** (L67-68): 每 `watchDelay` 毫秒 (默认 30s) publishHeartBeat
- **publishHeartBeat** (L102-105): `HeartbeatEvent(this, index.getAndIncrement())` — Spring Cloud 标准心跳事件 (SCC-3 的 HeartbeatMonitor 消费)
- **注释锚** (L99-100): "nacos doesn't support watch now, publish an event every 30 seconds" — 打点替代 watch 的退路
- **三条件装配** (NacosDiscoveryHeartBeatConfiguration.java: L33-58): **AnyNestedCondition** — 任一满足即装配: ① Gateway locator enabled ② Spring Boot Admin 的 InstanceDiscoveryListener 存在 ③ `spring.cloud.nacos.discovery.heart-beat.enabled` — **默认不装配** (注释: "no longer enabled by default", issue#2868/#3258)
关键设计 (q2): **"心跳默认关闭 = 生态场景按需启用"** — 心跳只对依赖 HeartbeatEvent 的生态 (Gateway locator/SBA) 有意义, 普通服务不需要; 三条件 OR 保证生态在则自动开。 [模式: 生态条件装配]

### 3. NacosGracefulShutdownDelegate — 先反注册, 再等一等, 然后关

场景: ContextClosedEvent 时 Nacos 客户端怎么收尾?
源码路径:
- **NacosGracefulShutdownDelegate** (registry/NacosGracefulShutdownDelegate.java:34): ApplicationListener<ContextClosedEvent>
- **子上下文过滤** (L56-62): `!applicationContext.equals(event.getApplicationContext())` → 跳过 — **子上下文关闭不触发** (防止每子上下文都关一次)
- **doGracefulShutdown** (L67-81): ① `autoServiceRegistration.stop()` (L69 — 先反注册!) ② **gracefulShutdownWaitTime > 0 → ThreadUtils.sleep** (L70-73 — 等注册中心感知) ③ 异常 catch 不阻断
- **supportsAsyncExecution=false** (L83-87): 同步执行 — 注释 "need wait for graceful shutdown" — 必须等关闭完成
关键设计 (q3): **"停机序列 = 反注册 → 等待 → 放行"** — stop() 反注册让 Nacos 立即摘流量, sleep 窗口让调用方感知下线 (负载均衡列表刷新), 同步执行保证进程真正退出前完成。 [模式: 优雅停机序列]

### 4. NacosDiscoveryHealthIndicator — 服务端状态直读

场景: nacos 健康检查的 UP/DOWN 哪来的?
源码路径:
- **NacosDiscoveryHealthIndicator** (discovery/actuate/health/NacosDiscoveryHealthIndicator.java:33): AbstractHealthIndicator
- **doHealthCheck** (L59-70): `namingService.getServerStatus()` (L62) → UP → builder.up / DOWN → down / 其他 → unknown (L65-69)
- **@since 2.2.0** (L30) — 引入锚
- @Deprecated 构造器 (L47-48/L54-57): 旧 NamingService 注入弃用 → 新 NacosServiceManager
关键设计 (q4): **"健康 = 服务端状态镜像"** — 不自己探测, 直接镜像 Nacos 服务端 getServerStatus (UP/DOWN); unknown 兜底非标准状态。 [模式: 状态直读镜像]

### 5. 事件面 — NacosDiscoveryInfoChangedEvent 与重注册联动

场景: 发现配置运行时变更怎么生效?
源码路径:
- **NacosDiscoveryInfoChangedEvent** (event/NacosDiscoveryInfoChangedEvent.java:26): 变更事件载体
- 消费方 (A3 已见): NacosAutoServiceRegistration @EventListener → restart (stop+start 重注册)
- NacosServiceManager.isNacosDiscoveryInfoChanged (A3:66-73): equals 比较 properties — 变更检测
关键设计 (q5): **"变更事件 = 运行时重配置闭环"** — 配置变更 → 事件 → 重注册, 无需重启; 与 A2 的配置刷新同构 (事件驱动重绑定)。 [模式: 事件驱动重配置]

### 6. 测试与行为锚

场景: 订阅/心跳的边界行为怎么固定?
源码路径:
- 注释锚: "nacos doesn't support watch now" (L99-100) / issue#2868/#3258 (HeartBeatConfiguration L38-39)
- 版本锚: NacosDiscoveryHealthIndicator @since 2.2.0 (L30)
- NacosWatch stop 对称设计 (L122-136) — start/stop 完全镜像
关键设计 (q1): **"issue 锚 + 版本锚"** — 心跳关闭决策有 issue 溯源; 2.2.0 引入健康指示器。 [模式: 行为锚]
