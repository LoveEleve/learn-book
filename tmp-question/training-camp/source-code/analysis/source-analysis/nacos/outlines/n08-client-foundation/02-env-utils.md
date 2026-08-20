# N-08-02 客户端基础 — env 属性体系与工具面 (env/工具/监控篇)

> 前置: 无 (客户端底座) | 引出: NC-1/NC-2 (所有客户端类消费 NacosClientProperties)
> 🟡 B | 方案 B (重要域) | 闭环: q1(属性体系) q2(校验面) q3(工具/监控)

**读者处境**: 所有客户端类构造第一行 `NacosClientProperties.PROTOTYPE.derive(properties)` — 这个属性体系怎么设计? 参数校验 (ValidatorUtils) 校验什么?

### 1. 属性体系 — NacosClientProperties 的 PROTOTYPE 模式

场景: 客户端属性怎么统一管理?
源码路径:
- **NacosClientProperties** (client-basic/env/NacosClientProperties.java:27): 接口 — **PROTOTYPE 常量** (L36: SearchableProperties.INSTANCE) + **derive()** (子属性继承父) + getProperty/getInteger/getLong (L44-106)
- **SearchableProperties** (env/SearchableProperties.java:38): 可搜索属性 (多源合并)
- **SourceType 族** (SourceType + SystemEnvPropertySource/JvmArgsPropertySource/PropertiesPropertySource/AbstractPropertySource): **系统环境/JVM 参数/Properties 三源**
- **convert 族** (AbstractPropertyConverter/Integer/Long/Boolean/CompositeConverter): 类型转换
- 消费: NC-1/NC-2/NC-3 全部通过 PROTOTYPE.derive 使用 — 属性统一入口
关键设计 (q1): **"PROTOTYPE 原型模式 = 属性继承链"** — 全局 PROTOTYPE → derive 出子属性 (继承+覆盖); 三源 (env/jvm/properties) 可搜索合并; 注释明言 "all the NacosClientProperties object must be created by PROTOTYPE" (L30-31)。 [模式: 原型继承]

### 2. 校验与参数面 — ValidatorUtils/ParamUtil/InitUtils

场景: 客户端参数怎么校验与初始化?
源码路径:
- **ValidatorUtils** (client/utils/ValidatorUtils.java:31): **checkInitParam** (NC-1/NC-2 init 首步) — 参数校验
- **ParamUtil** (client/utils/ParamUtil.java:29): 常用参数常量与解析
- **PreInitUtils** (utils/PreInitUtils.java:32): 预加载 (asyncPreLoadCostComponent)
- **ClientBasicParamUtil** (client-basic/utils/ClientBasicParamUtil.java:37): 基础参数解析 (parseNamespace)
- **EnvUtil** (client/utils/EnvUtil.java:31): 环境工具
- **LogUtils** (client/utils/LogUtils.java:30): 日志门面 (NAMING_LOGGER/CONFIG_LOGGER)
关键设计 (q2): **"校验前置 = 参数失败快速暴露"** — init 第一行 checkInitParam; 与 NC-1/NC-2 的 init 链闭合。 [模式: 前置校验]

### 3. 工具/监控面 — ConcurrentDiskUtil/MetricsMonitor/日志/锁

场景: 客户端工具与可观测?
源码路径:
- **ConcurrentDiskUtil** (utils/ConcurrentDiskUtil.java:38): 并发磁盘写 (NC-4 DiskCache 消费)
- **MetricsMonitor** (client/monitor/MetricsMonitor.java:28): 客户端指标 (getServiceInfoMapSizeMonitor — NC-1 消费 L146)
- **NacosLogging** (logging/NacosLogging.java:37): 日志框架 (log4j2 面)
- **NacosLockService** (lock/NacosLockService.java:43): 客户端分布式锁
- **AppNameUtils/ContextPathUtil/TenantUtil/TemplateUtils**: 工具面
关键设计 (q3): **"工具面 = 客户端公共底座"** — 磁盘/监控/日志/锁各司其职, 被核心面消费。 [模式: 公共底座]

### 4. 测试与行为锚

场景: 属性边界?
源码路径:
- 测试: NacosClientPropertiesTest (client-basic test)
- 注释锚: "all the NacosClientProperties object must be created by PROTOTYPE" (L30-31) — 唯一创建约束
关键设计 (q1): **"创建约束注释 = 架构规则"** — 原型模式的唯一入口写死在注释。 [模式: 架构规则注释]
