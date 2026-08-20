# ALI-A1 Nacos Config 配置加载 — 应用启动前, 配置中心的"长臂"如何伸进 Environment

> 前置: 无 (Alibaba 配置面拓扑首位) | 引出: [[ALI-A2-配置动态刷新]] (同模块递进) + [[SCC-1-Bootstrap]] (Commons 的 PropertySourceLocator 插槽) | 对照: Spring Cloud Commons bootstrap 双轨 + Spring Boot ConfigData (spring.config.import)
> 🔴 A | 方案 A (全深度) | 闭环: q1(双轨制) q2(locator 加载链) q3(优先级) q4(快照容灾)

**读者处境**: 项目里写了 `spring.config.import=nacos:xxx.yml`, 应用启动时配置怎么在业务代码运行前就到了? 老项目用 bootstrap.yml 怎么兼容? Nacos 挂了应用还能启动吗 (容灾)? "profile 配置覆盖默认配置" 是谁保证的?

### 1. 双轨制 — ConfigData 新轨 vs Bootstrap 旧轨

场景: 两种接入 Nacos Config 的方式 (spring.config.import / bootstrap.yml) 源码上如何落地?
源码路径:
- **ConfigData 新轨**: `spring.config.import=nacos:...` → Boot 的 ConfigDataEnvironmentPostProcessor 扫描 services 文件 → **NacosConfigDataLocationResolver** (configdata/NacosConfigDataLocationResolver.java:57, SPI 注册于 `META-INF/services/org.springframework.boot.context.config.ConfigDataLocationResolver`) — `PREFIX = "nacos:"` (L62) + `getOrder() = -1` (L75-77)
- **Bootstrap 旧轨**: `spring.factories` 的 `org.springframework.cloud.bootstrap.BootstrapConfiguration` → **NacosConfigBootstrapConfiguration** (spring-cloud-starter-alibaba-nacos-config, 仅装配 SmartConfigurationPropertiesRebinder) / NacosConfigBootstrapConfiguration (core 模块, 装配 NacosConfigManager/NacosConfigProperties/NacosPropertySourceLocator)
- **普通自动配置轨**: AutoConfiguration.imports → **NacosConfigAutoConfiguration** (core) + **NacosConfigSpringCloudAutoConfiguration** (starter, 装配 NacosPropertySourceLocator L50-53) — **非 bootstrap 非 import 也能用?** 靠 NacosPropertySourceLocator 仍在 EnvironmentPostProcessor 阶段被 PropertySourceBootstrapConfiguration 收集 (Commons SCC-1)
- **双轨共享**: SmartConfigurationPropertiesRebinder 双配置类同构装配 (NacosConfigSpringCloudAutoConfiguration.java:42-47 + Bootstrap 版) — 新旧入口同一刷新器
- isResolvable (NacosConfigDataLocationResolver.java:119-129): `location.hasPrefix("nacos:")` + `spring.cloud.nacos.config.enabled` 默认 true (L126-128)
关键设计 (q1): **"双轨 = 一个 SPI 插槽在两个时代分别被 Boot 的两种机制调用"** — 新轨 ConfigData (Boot 2.4+) / 旧轨 bootstrap (Commons SCC-1 的 PropertySourceLocator) / 常轨 AutoConfiguration — 三轨并存是兼容性工程: 老项目不动配置, 新项目原生接入。 [模式: 双轨兼容]

### 2. ConfigData 新轨 — location 解析 → resource → loader

场景: `nacos:order-service.yml?group=G&refreshEnabled=true` 这一行字符串怎么变成 Environment 里的属性?
源码路径:
- **resolveProfileSpecific** (NacosConfigDataLocationResolver.java:144-159): ① loadProperties (L79-108: bootstrapContext 已注册 → 直接取 / 否则 Binder 绑定 `spring.cloud.nacos[.config]`) ② **registerIfAbsent(NacosConfigProperties)** (L153-154) ③ **registerConfigManager** (L198-204: `NacosConfigManager.getInstance(properties)` 注册到 BootstrapRegistry) ④ loadConfigDataResources (L161-180)
- **URI 三段解析** (L186-266): ① getUri: `properties.getServerAddr() + path` 补 http:// 前缀 (L194/L207-208) ② **dataIdFor** (L255-266): path 去掉 `/` 后**必须只有一段** (parts.length != 1 → "illegal dataId") ③ groupFor/suffixFor/refreshEnabledFor/preferenceFor: query 参数 `group`/`refreshEnabled`/`preference` + 默认取 properties (L220-253)
- **NacosConfigDataResource** (configdata/NacosConfigDataResource.java:33): ConfigDataResource 子类 + **NacosItemConfig 五元组** (L103-108: group/dataId/suffix/refreshEnabled/preference)
- **NacosConfigDataLoader.load** (configdata/NacosConfigDataLoader.java:66-100): ① getBean(NacosConfigManager) → configService (L74-75) ② **pullConfig** (L140-148: `configService.getConfig(dataId, group, timeout)` + NacosDataParserHandler 按 suffix 解析) ③ new NacosPropertySource (L85-87) ④ **NacosPropertySourceRepository.collectNacosPropertySource** (L89) ⑤ new ConfigData + **getOptions** (L102-114)
- **getOptions 优先语义** (L102-114): IGNORE_IMPORTS + IGNORE_PROFILES + **preference=REMOTE → Option.PROFILE_SPECIFIC** (L107-112, issue#2455 修复: 远程配置覆盖本地同 key)
- 异常语义 (L93-99): optional=false → ConfigDataResourceNotFoundException 抛出; optional → log.error 返回 null
关键设计 (q2): **ConfigData 是"资源化"的配置加载** — location 字符串 → URI 解析 → Resource 对象 → Loader 拉取 → ConfigData 包裹 PropertySource; Boot 负责顺序/去重/Profile 展开, Nacos 只负责"给数据"。 [模式: 资源化加载]

### 3. NacosPropertySourceLocator — 老轨的加载链与优先级

场景: bootstrap 老轨怎么拉配置? 为什么 profile 配置优先级最高?
源码路径:
- **locate** (NacosPropertySourceLocator.java:74-101): ① configService null → warn + return null (L78-81, 容错) ② new NacosPropertySourceBuilder (L83-84) ③ **dataIdPrefix 三选** (L87-94): prefix → name → `spring.application.name` (环境兜底)
- **loadApplicationConfiguration 三级递进** (L106-124): ① 默认 dataId=prefix (L112-113) ② `dataId + "." + fileExtension` 后缀 (L115-116) ③ **for profile**: `dataId + "-" + profile + "." + fileExtension` (L118-122) — **每级 addFirst, 后加载的排前面 = profile 最高优先**
- **loadNacosDataIfPresent** (L148-160): dataId/group 空校验 → loadNacosPropertySource → addFirstPropertySource
- **loadNacosPropertySource 刷新节流** (L162-172): **`NacosContextRefresher.getRefreshCount() != 0 && !isRefreshable` → 从 Repository 直接取旧源不重新拉取** (L164-169) — 刷新时非 refreshable 配置不重复请求
- **addFirstPropertySource** (L178-187): ignoreEmpty=true 时空源丢弃 — **空配置不占位**
- 扩展配置: loadNacosConfiguration (L126-134) 遍历 sharedConfigs/extensionConfigs (NacosConfigProperties.Config 列表, refresh 标记各自独立)
关键设计 (q3): **"三级递进 + addFirst 反转" 实现优先级** — 默认→后缀→profile 逐级 addFirst, 后者永远压前者; 与 Commons SCC-1 的 insertPropertySources 同构 (反转 addFirst 保序)。 [模式: addFirst 保序]

### 4. NacosConfigManager — 单例双检锁与 ConfigService 静态持有

场景: 双轨都拉配置, ConfigService 会不会建两个?
源码路径:
- **getInstance(properties)** (NacosConfigManager.java:49-60): 非空直接返回 + **synchronized 双检锁** (L53-58) — 静态 INSTANCE
- **静态 ConfigService** (L35): `private static ConfigService service` — 类级持有, 进程内唯一
- **createConfigService** (L65-79): NacosFactory.createConfigService(assembleConfigServiceProperties) + **失败 → NacosConnectionFailureException** (L75-77, 对接 diagnostics/analyzer/NacosConnectionFailureAnalyzer)
- **懒建** (L81-86): getConfigService 时 null → 再建 — 启动失败容忍
- 装配: NacosConfigAutoConfiguration.java:64-66 `@Bean nacosConfigManager` → getInstance(properties) + BootstrapRegistry 侧 NacosConfigDataLocationResolver.java:198-204 双注册 — **双入口同一单例**
关键设计 (q4): **"静态单例 = 跨装配面共享"** — AutoConfiguration 与 ConfigData 引导期 (BootstrapRegistry) 可能都创建, 静态 INSTANCE + 双检锁保证同一; ConfigService 静态字段让"进程唯一"延伸到 Nacos 客户端。 [模式: 进程级单例]

### 5. 快照容灾与解析链 — Nacos 挂了配置从哪来?

场景: Nacos 不可达时, 应用为什么还能拿到上次的配置?
源码路径:
- **NacosSnapshotConfigManager** (refresh/NacosSnapshotConfigManager.java:30): 静态 ConcurrentHashMap + **MAX_SNAPSHOT_COUNT=100** (L41) + put 时超限丢最旧 (L58-63)
- **getAndRemoveConfigSnapshot** (L47-52): **读后即删** (get + remove) — 单次消费语义, 刷新区间内只有一次
- **Builder 优先快照** (NacosPropertySourceBuilder.java:86-95): loadNacosData 先取快照, 空才 `configService.getConfig` (L89) — **本地内存快照是请求的"插队者"**
- 快照写入方: NacosConfigManager 配置变更时 put (A2 域衔接, NacosPropertySourceRefreshListener)
- **解析链** (L107): NacosDataParserHandler.getInstance().parseNacosData(dataId, data, fileExtension) — 按扩展名分派 (yaml/json/xml/properties); PropertySourceLoader SPI 注册 NacosJsonPropertySourceLoader/NacosXmlPropertySourceLoader (spring.factories)
- 空配置语义 (L96-101): 空 → warn + Collections.emptyList() — 不报错
- NacosException (L110-112) / 解析异常 (L113-115) → log.error + emptyList — **拉取失败静默降级**
- **缺失检查**: NacosConfigDataMissingEnvironmentPostProcessor (starter/configdata): **bootstrap/legacy 跳过** (L51) + ORDER = ConfigDataEnvironmentPostProcessor.ORDER+1000 (L41) + config.enabled && import-check.enabled → 检查 spring.config.import 缺 nacos: 则 ImportException + FailureAnalyzer 描述
关键设计 (q5): **"快照 = 内存级容灾, 静默 = 容错哲学"** — 拉不到不抛异常, 有快照用快照 (还是最新), 没快照给空源不占位; 但 import 缺失是配置错误要大声报 (ImportException)。 [模式: 容灾降级 + 配置缺失大声报错]

### 6. NacosConfigProperties 与装配面 — 719 行配置类的关键字段

场景: 一个配置类如何支撑双轨三入口?
源码路径:
- **NacosConfigProperties** (NacosConfigProperties.java:68, 719 行): 关键默认值 — `group = "DEFAULT_GROUP"` (L117) / `fileExtension = "properties"` (L125) / `timeout = 3000` (L129) / `refreshEnabled = true` (L196) / DEFAULT_ADDRESS "127.0.0.1:8848" (L88)
- **init → overrideFromEnv** (L199-229): server-addr 先从 `${prefix}.config.server-addr` 再 `${prefix}.server-addr:127.0.0.1:8848` 占位符解析 — **环境覆盖 > 配置类字段**
- **Config 内部类** (L627+): dataId/group=默认 DEFAULT_GROUP/**refresh=false** (L627-641) — sharedConfigs/extensionConfigs 的条目级配置, **默认不刷新**
- 装配面三文件: AutoConfiguration.imports (NacosConfigAutoConfiguration + Endpoint) / spring.factories (BootstrapConfiguration + FailureAnalyzer + PropertySourceLoader) / services (ConfigDataLocationResolver + ConfigDataLoader)
- NacosConfigEnabledCondition: `@Conditional` 判 enabled 属性 (默认 true)
关键设计 (q6): **"装配面分离 = 三轨各取所需"** — Boot 3 的 imports/services/spring.factories 三机制同时存在, 分别服务普通自动配置/ConfigData/旧 bootstrap/诊断/解析器。 [模式: 多机制装配]

### 7. 排序测试与行为锚 — 测试如何固定优先级?

场景: 谁保证 profile 配置真的覆盖默认配置?
源码路径:
- 测试: NacosPropertySourceLocatorTests / NacosConfigDataLoaderTests (test 目录) — loader 顺序断言
- 行为锚: addFirstPropertySource (L186) + loadApplicationConfiguration 三级 (L112-122) — 源码内注释明言 "higher priority" (L114/L117)
- issue 锚: 2455 (PROFILE_SPECIFIC 修复) / 2906 (parseNacosData configName 格式) — NacosConfigDataLoader.java:110/145 注释
- @since 锚: NacosConfigDataLocationResolver `@since 2021.0.1.0` (L55) — ConfigData 轨引入版本
关键设计 (q1): **"注释明言优先级 + issue 锚定行为"** — 优先级语义在代码注释里写死 (higher priority), 修复历史留 issue 号可溯源。 [模式: 行为锚]
