# N-15-02 配置存储面 — 操作服务与查询链 (操作篇)

> 前置: N-15-01 (PersistService 族) | 对照: 读写分离的操作面
> 🔴 A | 方案 A (全深度) | 闭环: q1(操作入口) q2(查询链) q3(迁移面)

**读者处境**: publishConfig/getConfig 的服务端业务逻辑在哪? 查询怎么走 handler 链? 迁移 (migrate) 是什么?

### 1. 操作入口 — ConfigOperationService

场景: 配置写操作的服务端逻辑?
源码路径:
- **ConfigOperationService** (service/ConfigOperationService.java:64): **publishConfig** (L88): **灰度/Beta 分派** (L109-120: BetaGrayRule.TYPE_BETA / TagGrayRule.TYPE_TAG) + 普通发布
- **ConfigDetailService** (service/ConfigDetailService.java:44): 详情查询
- **ConfigMigrateService** (service/ConfigMigrateService.java:79): 迁移服务 (最大类)
- **ConfigChangePublisher** (service/ConfigChangePublisher.java:29): 变更发布
关键设计 (q1): **"操作入口 = 灰度/普通分流"** — 发布时按灰度规则分派; 迁移服务 (1143 行) 是历史数据迁移面。 [模式: 灰度分流]

### 2. 查询链 — ConfigQueryChainService 与 handler 链

场景: 配置查询怎么走链式处理?
源码路径:
- **ConfigQueryChainService** (query/ConfigQueryChainService.java:37) / **ConfigQueryChainRequestExtractor** (query/ConfigQueryChainRequestExtractor.java:29): 链服务与请求提取
- **ConfigQueryHandlerChain** (query/ConfigQueryHandlerChain.java:32): **addHandler 链式构建** (L44-51) — handler 链
- **AbstractConfigQueryHandler** (query/handler/AbstractConfigQueryHandler.java:25) + **ConfigChainEntryHandler** (query/handler/ConfigChainEntryHandler.java:36): 处理器基类与入口
- **DefaultConfigQueryHandlerChainBuilder** (query/DefaultConfigQueryHandlerChainBuilder.java:30): 默认链构建
- **DefaultChainRequestExtractor** (query/DefaultChainRequestExtractor.java:38): 默认提取器
关键设计 (q2): **"查询链 = 可插拔 handler"** — 查询请求按链逐级处理 (校验/解析/存储查询); builder 构建默认链, 用户可定制。 [模式: 查询链]

### 3. 迁移面 — ConfigMigrateService

场景: 配置迁移 (灰度→正式) 怎么工作?
源码路径:
- **ConfigMigrateService** (service/ConfigMigrateService.java:79, 1143 行): 迁移核心 — publishConfigGrayMigrate (N-15-01 消费)
- 场景: beta/gray 转正式 / 跨租户迁移
关键设计 (q3): **"迁移 = 灰度收敛路径"** — 灰度配置验证后转正式。 [模式: 迁移收敛]

### 4. 测试与行为锚

场景: 操作链边界?
源码路径:
- 测试: ConfigOperationServiceTest / ConfigQueryChainTest (config test)
- 锚: ConfigForm/ConfigRequestInfo 请求模型
关键设计 (q1): **"请求模型 = 操作契约"** — 表单与请求信息分离。 [模式: 请求契约]
