# SCC-4 服务注册抽象 — 启动即注册: WebServer 就绪事件与注册生命周期

> 前置: [[SCC-3-服务发现]] (autoRegister 联动) | 引出: [[SCC-5-@LoadBalanced]] (注册后消费) | 对照: Nacos/Eureka 客户端注册
> 🔴 A | 方案 A (全深度) | 闭环: q1(双角色) q2(触发链) q3(生命周期钩子) q4(装配条件)
> Pass 2 闭环: q1(ServiceRegistry 5 方法) q2(WebServerInitializedEvent→start) q3(RegistrationLifecycle 4 钩子) q4(failFast)

**读者处境**: 应用启动后服务怎么"自动"出现在注册中心? 触发时机是"端口就绪"还是"应用启动"? 注册前后能插钩子吗? 管理端口 (management) 为什么单独注册? 没有注册中心实现时启动会怎样?

### 1. ServiceRegistry 接口 — 注册中心的 5 方法契约

场景: 所有注册中心的注册接口长什么样?
源码路径:
- ServiceRegistry\<R extends Registration\> (serviceregistry/ServiceRegistry.java:26): **5 方法** — register (L33) / deregister (L39) / close (L44) / setStatus (L53) / getStatus (L62)
- Registration 泛型约束 — 注册元数据 (hostname/port 等)
- 实现族 (外部): NacosServiceRegistry (Nacos 仓库) / EurekaServiceRegistry / ConsulServiceRegistry — **注册中心插槽**
关键设计 (q1): **register/deregister 是核心, close/setStatus/getStatus 是管理面** — setStatus/getStatus 被 ServiceRegistryEndpoint 消费 (Javadoc 引用 L53/62); Registration 泛型让每个注册中心定义自己的元数据。 [模式: 5 方法契约]

### 2. AbstractAutoServiceRegistration — 双角色: 事件监听 + 生命周期

场景: 注册触发时机和生命周期怎么设计?
源码路径:
- AbstractAutoServiceRegistration\<R extends Registration\> (AbstractAutoServiceRegistration.java:49-50): **implements AutoServiceRegistration + ApplicationListener\<WebServerInitializedEvent\> + ApplicationContextAware** — 双角色
- **onApplicationEvent** (L111-119): ① **management namespace 跳过** (L114-117, ConfigurableWebServerApplicationContext 的 serverNamespace=="management") ② **port.compareAndSet(0, event.getWebServer().getPort())** (L118) ③ start() (L119)
- **isAutoStartup** (L138-140) / getPhase (L248)
- 抽象方法 4 个 (L190-258): getConfiguration / isEnabled / getRegistration / getManagementRegistration
关键设计 (q2): **触发点是 WebServerInitializedEvent 而非应用启动** — "端口就绪才算服务可注册"; management namespace 跳过 (管理端口不触发主注册); port CAS 记录 (0→实际端口, 供注册用)。 [模式: 事件触发 + 双角色]

### 3. start() — 注册的完整仪式: 事件 + 钩子 + 注册

场景: start() 里发生了什么?
源码路径:
- start() (L142-170): ① **isEnabled() 守卫** (L143-147, 禁用则跳过) ② **running 双检** (L151, !running.get()) ③ **InstancePreRegisteredEvent 发布** (L153) ④ **RegistrationLifecycle.postProcessBeforeStartRegister 前钩子** (L154-155) ⑤ **register()** (L156, serviceRegistry.register(getRegistration())) ⑥ **postProcessAfterStartRegister 后钩子** (L157-159) ⑦ **shouldRegisterManagement 三条件判定** (L176-184: ① registerManagement 属性默认 true ② getManagementPort() != null ③ **ManagementServerPortUtils.isDifferent — 管理端口与主端口不同**) → 管理注册面 (L160-166) ⑧ **InstanceRegisteredEvent 发布** (L169) ⑨ running CAS (L170)
- register() (L263-265): 委托 serviceRegistry
- registerManagement() (L268-273): getManagementRegistration 判空注册
关键设计 (q3): **注册是"仪式化"流程** — 前后事件 (Pre/Registered) + 前后钩子 (RegistrationLifecycle) 包住核心 register(); 管理注册独立成面 (management 服务单独注册); running 双检防重复。 [模式: 仪式化流程 + 双事件]

### 4. stop() — 对称注销链

场景: 应用停止时怎么注销?
源码路径:
- stop() (L294-311): ① **running CAS(true→false) + isEnabled** (L295-296) ② postProcessBeforeStopRegister (L298) ③ **deregister()** (L299) ④ postProcessAfterStopRegister (L301) ⑤ shouldRegisterManagement → 管理注销链 (L302-307) ⑥ **serviceRegistry.close()** (L311)
- deregister() (L280-281) / deregisterManagement() (L287-291)
关键设计 (q1): **stop 是 start 的镜像** — 同构的事件/钩子/注册顺序 (Before/After 包住 deregister); close() 收尾 (释放注册中心资源)。 [模式: 对称生命周期]

### 5. RegistrationLifecycle — 注册的 4 钩子扩展点

场景: 注册前后想插自定义逻辑 (如设置元数据)?
源码路径:
- RegistrationLifecycle\<R\> (RegistrationLifecycle.java:27): **extends Ordered** + DEFAULT_ORDER=0 (L32)
- **4 钩子** (L38-56): postProcessBeforeStartRegister / postProcessAfterStartRegister / postProcessBeforeStopRegister / postProcessAfterStopRegister
- **RegistrationManagementLifecycle extends RegistrationLifecycle** (RegistrationManagementLifecycle.java:25) — **新增 4 个管理专用方法** (*StartRegisterManagement/*StopRegisterManagement L29-43) + registrationManagementLifecycles **独立字段** (AbstractAutoServiceRegistration.java:70, 构造器注入 L82-86) — 管理钩子与主钩子分离
- 消费: AbstractAutoServiceRegistration.registrationLifecycles 列表遍历 (L153-159/301-304)
关键设计 (q3): **钩子是 Ordered 扩展点** — 多个生命周期实现按 order 执行; 4 钩子覆盖"注册前后 + 注销前后"全周期; 管理注册有独立接口 (管理面扩展)。 [模式: Ordered 钩子]

### 6. 装配条件 — auto-registration.enabled + failFast

场景: 什么条件下注册机制装配? 没有实现时?
源码路径:
- **AutoServiceRegistrationAutoConfiguration** (AutoServiceRegistrationAutoConfiguration.java:29-40): @Import(AutoServiceRegistrationConfiguration) + **@ConditionalOnProperty("spring.cloud.service-registry.auto-registration.enabled", matchIfMissing=true)** — SCC-3 autoRegister=false 注入的属性在此生效
- **failFast** (afterPropertiesSet L38-44): 无 AutoServiceRegistration bean 且 properties.isFailFast() → **IllegalStateException "Auto Service Registration has been requested"** (L42) — **⚠ failFast 默认 false** (AutoServiceRegistrationProperties.java:29-31, "Defaults to false") — 默认无实现不报错静默; 另 enabled/registerManagement 默认 true (L25-28)
- **ServiceRegistryAutoConfiguration** (ServiceRegistryAutoConfiguration.java:31-44): **ServiceRegistryEndpoint 装配** (@ConditionalOnBean(ServiceRegistry) + @ConditionalOnAvailableEndpoint, 注册状态端点 /service-registry)
- AutoServiceRegistrationProperties (67 行): failFast 配置面
关键设计 (q4): **"请求了注册但没有实现" = 启动失败** — failFast 快速暴露配置错误; 属性默认 matchIfMissing=true (不显式配也启用); SCC-3 autoRegister 联动闭环 (属性注入 → 条件装配)。 [模式: 条件装配 + failFast]

## 代码类型
Architecture (注册抽象) + SPI (注册中心插槽)

## 负面空间 — 服务注册抽象刻意不做的事

- **不做注册中心客户端内建**: 只定义接口, Nacos/Eureka 各自实现 (SPI)
- **不做心跳续约**: 心跳由各注册中心实现 (Nacos 的 BeatReactor/Eureka 的 Renew), 抽象层无
- **不做重试注册**: register 失败直接抛 (依赖上层/实现重试), 无自动重试
- **不做注册失败降级**: 注册失败 = 启动失败 (failFast), 无"跳过继续启动"
- **不做多实例注册**: 一次注册一个 registration, 无批量
- **不做注销保护**: deregister 幂等性由实现保证, 抽象层不检查

→ 引出: 注册好之后, 客户端怎么负载均衡消费? → SCC-5 @LoadBalanced
