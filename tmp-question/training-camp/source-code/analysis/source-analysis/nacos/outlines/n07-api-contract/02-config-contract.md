# N-07-02 API 契约面 — ConfigService 接口族与注解面 (Config 契约篇)

> 前置: [[NC-2-ConfigService]] (实现方) | 引出: N-07-03 (Remote 协议) | 对照: 接口重载与实现漏斗
> 🟡 B | 方案 B (重要域) | 闭环: q1(接口面) q2(监听/过滤面) q3(注解族)

**读者处境**: 配置的接口契约 (getConfig/addListener/publishConfigCas) 在 api 怎么定义? 监听器抽象怎么分层? @NacosValue 等注解族?

### 1. 接口面 — ConfigService 与 ConfigFactory

场景: 配置对外契约?
源码路径:
- **ConfigService** (api/config/ConfigService.java:32): **getConfig** (L43) + **getConfigAndSignListener** (L60, 查+注册原子) + **addListener** (L74) + **publishConfig 双载** (L85/98) + **publishConfigCas 双载** (L110/123, casMd5 乐观锁) + removeConfig + **fuzzyWatch 族** + getServerStatus
- **ConfigFactory** (ConfigFactory.java:30): 客户端工厂
- **ConfigType** (ConfigType.java:29): 配置类型枚举 (properties/yaml/json/xml 等)
- **PropertyChangeType** (PropertyChangeType.java:24): 变更类型 (ADD/MODIFY/DELETE)
- model/ 族: ConfigDetailInfo/ConfigHistoryDetailInfo/ConfigCloneInfo/ConfigGrayInfo/ConfigBasicInfo — 管理与查询 DTO
关键设计 (q1): **"接口 = 三种读 + 三种写"** — 读 (getConfig/AndSignListener/fuzzyWatch), 写 (publishConfig/Cas/remove), 监听 (addListener); CAS 是 3.x 新增的乐观锁面。 [模式: 接口面]

### 2. 监听/过滤面 — Listener 抽象链与 ConfigFilter

场景: 监听器与过滤器怎么分层?
源码路径:
- **Listener** (listener/Listener.java:26): 基础接口 (getExecutor/receiveConfigInfo)
- **AbstractListener** (listener/AbstractListener.java:26): 简单适配 (NC-1 客户端 AbstractSharedListener 同族不同面)
- **AbstractSharedListener** (listener/AbstractSharedListener.java:27): **共享监听器** — fillContext(dataId/group) 上下文填充 (NC-2 CacheData 通知链消费)
- **ConfigChangeParser** (listener/ConfigChangeParser.java:29): 变更解析 SPI 接口 (NC-2 ConfigChangeHandler 消费)
- **ConfigChangeEvent/ConfigChangeItem** (ConfigChangeEvent.java:27 + ConfigChangeItem.java:24): 变更事件与变更项
- **filter/ 族** (6): IConfigFilter/IConfigRequest/IConfigResponse/IConfigFilterChain/IConfigContext/AbstractConfigFilter — 加解密过滤链契约 (NC-2 ConfigFilterChainManager 消费)
关键设计 (q2): **"监听分层 = 内容/上下文/变更三态"** — 基础 Listener 只收内容, AbstractSharedListener 补上下文, ConfigChangeParser+Event 产变更明细; filter 链独立契约。 [模式: 监听分层]

### 3. 注解族 — 配置注解契约

场景: 注解式配置的契约?
源码路径:
- **@NacosValue** (annotation/NacosValue.java:34): 字段注入 (ALI-A2 面)
- **@NacosConfigurationProperties** (annotation/NacosConfigurationProperties.java:42): 配置属性绑定
- **@NacosConfigListener** (annotation/NacosConfigListener.java:41): 方法监听 (ALI-A2 NacosAnnotationProcessor 消费)
- **@NacosProperty/@NacosIgnore** (annotation/): 属性/忽略
- **NacosConfigConverter** (convert/NacosConfigConverter.java:26): 类型转换 SPI
关键设计 (q3): **"注解契约 = 集成层消费面"** — ALI-A2 的 NacosAnnotationProcessor 是这些注解的实现方; api 定义契约, SCA 消费。 [模式: 注解契约]

### 4. 测试与行为锚

场景: 契约边界?
源码路径:
- 测试: api/config test
- 常量锚: ConfigType 枚举完整 (properties/yaml/json/xml/text/html)
关键设计 (q1): **"枚举即文档"** — ConfigType/PropertyChangeType 枚举定义合法值域。 [模式: 枚举契约]
