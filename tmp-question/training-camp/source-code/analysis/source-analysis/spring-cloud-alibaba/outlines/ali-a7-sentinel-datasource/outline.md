# ALI-A7 Sentinel 数据源 — 规则从哪来: 六源七类型的动态装配

> 前置: [[ALI-A6-三路限流]] (规则消费者) | 引出: [[Sentinel-5.9]] (RuleManager 内核) | 对照: Nacos 配置中心接入面
> 🟡 B | 方案 B (重要域) | 闭环: q1(单源校验) q2(动态注册) q3(转换器选择) q4(规则落位)

**读者处境**: `spring.cloud.sentinel.datasource.ds1.nacos.dataId=xxx` 配置后, 限流规则怎么自动加载? 为什么同时配了 nacos 和 file 会报错? ruleType 和 dataType 分别管什么?

### 1. 单源校验 — getValidField 的反射探测

场景: 一个数据源同时配了两种后端怎么处理?
源码路径:
- **DataSourcePropertiesConfiguration** (config/DataSourcePropertiesConfiguration.java:39): 六字段 (file/nacos/zk/apollo/redis/consul, L41-51)
- **getValidField 反射扫描** (L128-146): 遍历字段, 非空即有效 (L135) — 返回有效字段名列表
- **getValidDataSourceProperties** (L148-166): size==1 才取 (L151) — 反射取属性
- **单源强校验** (SentinelDataSourceHandler.java:82-88): **validFields.size() != 1 → log.error "multi datasource active and won't loaded" + return** (L83-88) — 多源配置整个数据源跳过
关键设计 (q1): **"多源即弃, 单源才载"** — 反射探测有效字段, 多于一个直接放弃 (防规则重复加载语义不明); 校验错误显式 log.error。 [模式: 配置唯一性校验]

### 2. 动态注册 — afterSingletonsInstantiated 的 Bean 工厂

场景: 数据源 Bean 何时、以何名注册?
源码路径:
- **SentinelDataSourceHandler** (custom/SentinelDataSourceHandler.java:51): **SmartInitializingSingleton** — 所有单例实例化后执行
- **afterSingletonsInstantiated** (L77-101): 遍历 sentinelProperties.getDatasource() Map → 单源校验 → setEnv → **preCheck(dataSourceName)** (L92, 各数据源自检钩子) → **registerBean(name + "-sentinel-" + type + "-datasource")** (L93-94)
- **registerBean** (L201-213): parseBeanDefinition → beanFactory.registerBeanDefinition (L205) → **getBean 立即初始化** (L208-209) → **postRegister(newDataSource)** (L212)
关键设计 (q2): **"初始化后回调 + 动态 Bean 注册"** — SmartInitializingSingleton 保证所有用户 Bean 就绪后才注册数据源 (避免依赖缺失); 注册即初始化 (getBean 触发), 立即生效。 [模式: 初始化后装配]

### 3. 转换器选择 — dataType 三分支 (json/xml/custom)

场景: 规则内容怎么解析? 自定义解析器怎么接入?
源码路径:
- **parseBeanDefinition 字段反射** (L103-122): 反射读数据源 Properties 所有字段 (L105-121) + 补 converterClass/dataType (L122-123)
- **dataType 三分支** (L134-188): ① **custom** → 必须配 converterClass (L136-144, 缺 → 抛) → **动态注册自定义 converter Bean** (L147-156, Class.forName) → addPropertyReference("converter") (L157) ② **json/xml** (L172-187) → 校验合法性 (L173-179) → 引用内置 converter Bean `sentinel-{type}-{ruleType}-converter` (L183-186) ③ 其他字段反射注入 (L189-195)
- **内置 converter**: JsonConverter/XmlConverter (converter 包) — 按 ruleType 生成专用 converter
关键设计 (q3): **"dataType 决定解析器, ruleType 决定规则形态"** — 两个维度正交: 格式 (json/xml) × 规则类型 (flow/degrade/...); custom 让用户带自己的解析器。 [模式: 双维度正交]

### 4. 规则落位 — postRegister 的 RuleManager 分派

场景: 数据源解析出的规则属性注册到哪?
源码路径:
- **postRegister 七分派** (AbstractDataSourceProperties.java:100-108): switch ruleType → **FlowRuleManager/DegradeRuleManager/ParamFlowRuleManager/SystemRuleManager/AuthorityRuleManager/GatewayRuleManager/GatewayApiDefinitionManager.register2Property(dataSource.getProperty())**
- **RuleType 七枚举** (RuleType.java:42-67): FLOW/DEGRADE/PARAM_FLOW/SYSTEM/AUTHORITY/GW_FLOW/GW_API_GROUP + 各自 Rule 类
- register2Property: Sentinel 内核的规则属性监听 (5.9 域) — 数据源变更 → 规则热更新
- **preCheck 钩子** (AbstractDataSourceProperties.java:96-98): 默认空, 子类可校验 (如文件存在性)
关键设计 (q4): **"规则注册 = 属性驱动的热更新"** — 数据源 ReadableDataSource 暴露 Property 给 RuleManager, 规则变更即推送 — 与 A2 的配置刷新同构但目标不同 (规则 vs 应用配置)。 [模式: 属性监听]

### 5. 六数据源族与装配面

场景: 六种数据源怎么选? 装配条件?
源码路径:
- **六 Properties** (config/): NacosDataSourceProperties (serverAddr/dataId/groupId= DEFAULT_GROUP L40 等) / Apollo / File / Redis / Zookeeper / Consul — 各自字段
- **六 FactoryBean** (factorybean/): NacosDataSourceFactoryBean 等 — 生产 ReadableDataSource
- SentinelProperties.datasource: Map<String, DataSourcePropertiesConfiguration> (ds1/ds2...)
- 装配: SentinelAutoConfiguration + DataSourcePropertiesConfiguration
关键设计 (q5): **"配置外壳 + 工厂内核"** — Properties 类管绑定, FactoryBean 管构造, Handler 管注册 — 三层分工, 新增数据源只需加 Properties+FactoryBean。 [模式: 配置工厂分离]

### 6. 测试与行为锚

场景: 数据源的边界行为?
源码路径:
- 测试: NacosDataSourceTests / SentinelDataSourceHandlerTests (test 目录)
- 注释锚: "multi datasource active and won't loaded" (L85) — 单源语义官方声明
- dataType 支持列表: "please using these types: [json, xml]" (L178)
关键设计 (q1): **"错误信息 = 契约文档"** — 异常信息直接告知支持类型和修正方式, 把配置错误变成可修复提示。 [模式: 错误即指南]
