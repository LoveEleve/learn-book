# SCC-1 Bootstrap 上下文 — 主应用诞生前的"引路人": bootstrap.yml 与配置中心的秘密

> 前置: 无 (上下文面拓扑首位) | 引出: [[SCC-2-@RefreshScope]] (刷新链路) + [[SCC-9-配置加密]] (同源 encrypt 面) | 对照: Spring Boot ConfigData (spring.config.import) + Nacos/Apollo 配置中心
> 🔴 A | 方案 A (全深度) | 闭环: q1(双轨制) q2(子上下文构建) q3(排序仲裁) q4(Locator SPI)
> Pass 2 闭环: q1(三守卫) q2(bootstrapServiceContext) q3(insertPropertySources) q4(PropertySourceLocator)

**读者处境**: `bootstrap.yml` 在 `application.yml` 之前怎么被加载? 配置中心 (Nacos/Apollo) 的配置怎么在应用启动前就注入 Environment? "bootstrap 属性高优先级, 本地配置不可覆盖" 是谁实现的? 为什么新项目用 `spring.config.import` 而老项目还能用 bootstrap?

### 1. 双轨制 — bootstrap 的启用与 legacy 兼容

场景: 什么条件下 bootstrap 机制才启动?
源码路径:
- BootstrapApplicationListener (512 行, bootstrap/BootstrapApplicationListener.java:77): `implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered` — **监听环境准备事件**
- **DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5** (L87) — 极早执行, 仅晚于核心
- **入口双守卫** (L97-104): ① `!bootstrapEnabled && !useLegacyProcessing → return` (L99) ② 环境已含 BOOTSTRAP_PROPERTY_SOURCE_NAME → return (bootstrap 上下文内不再递归, L103)
- **apply 双保护** (L289-295): ③ BootstrapMarkerConfiguration 已含 → return (防重复 apply, L291-293) ④ filterListeners 过滤日志监听器 (L195, apply 内部 — 非入口守卫)
- bootstrapEnabled (util/PropertyUtils.java:48-50): `spring.cloud.bootstrap.enabled 属性 || MARKER_CLASS_EXISTS` — **标记类双保险**: 显式开启或旧版 marker 类存在 (org.springframework.cloud.bootstrap.marker.Marker, L37-38)
- useLegacyProcessing (L52-54): `spring.config.use-legacy-processing` 属性 — **Boot 2.4+ 新配置处理前的旧式开关**
关键设计 (q1): **bootstrap 是"老机制+新开关"双轨** — 新项目用 spring.config.import (Boot 原生), 老项目靠 bootstrap.enabled/marker 兼容; 监听 ApplicationEnvironmentPreparedEvent 而非 ApplicationStartedEvent = 在 Environment 定型前介入。 [模式: 事件驱动 + 双轨兼容]

### 2. 子上下文构建 — bootstrapServiceContext 的"空环境"工厂

场景: bootstrap 上下文是怎么"凭空"建出来的?
源码路径:
- onApplicationEvent 主流程 (L97-118): 守卫通过 → 检查 initializers 里是否有 ParentContextApplicationContextInitializer → **findBootstrapContext 反射取 parent** (L121-129, 复用已有上下文) / 无则 bootstrapServiceContext (L142)
- **bootstrapServiceContext** (L142-200): ① 新建空 AbstractEnvironment (L143-144) ② configName 解析 `${spring.cloud.bootstrap.name:bootstrap}` (L107) → **spring.config.name 注入** (L151) ③ `spring.cloud.bootstrap.location/additional-location` 解析 (L147-149) → 注入 (L159-162) ④ SpringApplicationBuilder 构建子应用 (L172-174) ⑤ **context.setId("bootstrap")** (L203)
- **addAncestorInitializer** (L205): 把 bootstrap context 设为**主应用的 parent** — 父子上下文关系建立
- 守卫③: 过滤 LoggingApplicationListener/LoggingSystemShutdownListener (filterListeners L214-221) — bootstrap 阶段不重复初始化日志
- mergeDefaultProperties (L223+): defaultProperties 合并到主环境
关键设计 (q2): **"bootstrap 上下文 = 空环境 + 配置中心"** — 它没有任何应用 Bean, 只承载 PropertySourceLocator 拉取的配置; 作为主应用 parent, 主应用的 Environment 能"看到"bootstrap 的属性 (父子上下文属性可见性)。 [模式: 父子上下文]

### 3. PropertySourceLocator — 配置中心的 SPI 入口

场景: Nacos/Apollo 怎么把自己的配置"塞"进 bootstrap 上下文?
源码路径:
- **PropertySourceLocator 接口** (bootstrap/config/PropertySourceLocator.java:36-48): 单方法 `locate(Environment)` 返回 PropertySource 或 null
- **locateCollection 默认方法** (L44-47): locate 结果展开 — CompositePropertySource 扁平化 / null → 空列表 / 单个包装
- 消费方: PropertySourceBootstrapConfiguration.initialize (config/PropertySourceBootstrapConfiguration.java:107) — 遍历 locators 收集 PropertySource
- 实现族 (外部): NacosPropertySourceLocator (Nacos 仓库) / ApolloPropertySourceLocator (Apollo 仓库) — **所有配置中心的接入点**
关键设计 (q4): **locate 单方法是配置中心的唯一契约** — 返回 PropertySource 即完成"配置注入"; Composite 支持多源; null 表示"无配置"不报错 — 这是配置中心生态的插槽。 [模式: SPI 插槽]

### 4. PropertySourceBootstrapConfiguration — 属性注入与"优先级仲裁"

场景: 配置中心的属性插到 Environment 的哪个位置?
源码路径:
- PropertySourceBootstrapConfiguration (L68): `implements ApplicationListener<ContextRefreshedEvent>` + initialize (L107) — 事件触发注入
- **insertPropertySources** (L184-230): **排序仲裁核心** — ① 反转 composite 列表 addFirst (L188-194, 反转保序) ② Binder 绑定 spring.cloud.config 三开关 (L196-198)
- **三策略真实逻辑** (L200-227): ① `!allowOverride || (!overrideNone && overrideSystemProperties)` → **addFirst 最高优先级** (L202-205, DECRYPTED 源后特判) ② `overrideNone` → **addLast 最低** (L209-213) ③ 否则 → **相对 systemEnvironment 定位**: `!overrideSystemProperties → addAfter(systemEnvironment)` (L217-219) / **`overrideSystemProperties → addBefore(systemEnvironment)`** (L222-224) — 大纲原漏 addBefore 分支
- DECRYPTED_PROPERTY_SOURCE_NAME 特判 (L203-204): 解密源后插入
关键设计 (q3): **"高优先级"是 addFirst 实现的, 不是概念** — 配置中心属性默认插到 Environment 最前, 本地配置在后; overrideNone 反转语义 (配置中心最弱); 三开关是"谁覆盖谁"的完整矩阵 — 面试"bootstrap 配置为什么优先"的源码答案。 [模式: 优先级仲裁]

### 5. 排序测试与行为锚 — BootstrapSourcesOrderingTests

场景: 多 PropertySourceLocator 的顺序怎么保证?
源码路径:
- BootstrapSourcesOrderingTests (test/.../BootstrapSourcesOrderingTests.java:33): `then(firstConstructedClass)` — **断言 locator 构造顺序**
- TestHigherPriorityBootstrapConfiguration.firstToBeCreated (import L27, 断言 L35): 高优先级配置的构造标记
- 测试集: BootstrapOrderingCustomPropertySource/SpringApplicationJson/SystemProperties 三场景 (BootstrapOrdering*IntegrationTests)
- @SpringBootTest(properties = "spring.cloud.bootstrap.enabled=true") (L29) — 测试显式开启 bootstrap
关键设计 (q1): **构造顺序 = 插入顺序** — locators 按配置类优先级构造, 构造早的插前面; 测试用静态标记验证"先构造的先进 Environment"。 [模式: 顺序实证]

### 6. 解密面 — TextEncryptorBindHandler (SCC-9 前置)

场景: bootstrap 阶段怎么处理 {cipher} 加密属性?
源码路径:
- TextEncryptorBindHandler (bootstrap/TextEncryptorBindHandler.java, SCC-9 主体)
- EncryptEnvironmentPostProcessor / EnvironmentDecryptApplicationInitializer (bootstrap/encrypt/, L45) — 解密在 Environment 准备期; **"No reason to decrypt bootstrap twice" 防重复守卫** (L81); addBootstrapDecryptInitializer 注册链 (BootstrapApplicationListener.java:299/310)
- DECRYPTED_PROPERTY_SOURCE_NAME 源 (AbstractEnvironmentDecrypt) — 解密结果单独成源, insertPropertySources 特判 addAfter (PropertySourceBootstrapConfiguration.java:200-201)
关键设计 (q2): **解密是 Environment 后处理** — 先有 bootstrap 源, 后解密生成解密源, 再在 insertPropertySources 里特判插入位置 (L200-201)。 [模式: 后处理链]

## 代码类型
Architecture (启动引导) + SPI (配置中心插槽)

## 负面空间 — Bootstrap 上下文刻意不做的事

- **不做应用 Bean 加载**: bootstrap 上下文只有配置相关 Bean, 无业务 Bean (职责单一)
- **不做配置中心客户端内建**: PropertySourceLocator 是 SPI, Nacos/Apollo 各自实现 — 核心零实现
- **不缓存远程配置**: 每次启动重新 locate (PropertySourceBootstrapConfiguration.java:121), 无远程缓存 (对比 Nacos 客户端快照); 注意区分: **bootstrap.yml 文件解析有进程内 loadDocumentsCache** (BootstrapConfigFileApplicationListener.java:317/630, DocumentsCacheKey→List 防重复解析) — 缓存的是本地文件解析非远程配置
- **不做运行时刷新**: bootstrap 是启动期一次性; 刷新是 SCC-2 @RefreshScope 的职责
- **不保证多 locator 顺序语义**: 依赖配置类声明顺序, 无显式排序契约 (测试实证行为但非 API 承诺)
- **不做 bootstrap 热更新**: 上下文构建后不可变

→ 引出: 配置拉取之后, 运行时变更怎么热刷新? → SCC-2 @RefreshScope 热刷新
