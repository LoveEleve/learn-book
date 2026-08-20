# HANDOFF — Spring Cloud Alibaba 源码分析交接文档 (10/10 全量收官)

> **日期**: 2026-08-16 | **版本**: 2025.0.0.0 (pom.xml:83 实证) | 模块: 13 starter 模块, **主源 223** (全量含测试 287; 规划声称 454 含 examples 口径)
> **给新 AI**: 本文是 Spring Cloud Alibaba 阶段的**唯一入口**。10 域全部交付 (KP + 大纲 + 20 问 + 六层深审 + 每域二轮 REVIEW + 时空溯源 🔴 + harness 🔴 + 全局三轮 REVIEW)。
> **源码**: `/data/workspace/source-code/code/spring/spring-cloud-alibaba` (git 浅克隆单提交 d9d9be6a)
> **规划**: ALI-PLAN.md (09 审计 v1 + 二轮 REVIEW 修正)
> **分工确认**: Dubbo/gRPC/Gateway/OpenFeign 由其他 AI 负责; Alibaba 本会话 10/10 收官 ✅

---

## §零 状态速查 (2026-08-16, 10/10 收官)

| 域 | 模块 | 级别 | 方案 | 大纲节 | questions | harness | 时空溯源 | REVIEW 修正 |
|:--:|:--|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| ALI-A1 Nacos Config 配置加载 | nacos-config (48+10) | 🔴 | A | 7 | 20 | **17/17** | ✅ 三时代叠加 | 深审 5 |
| ALI-A2 配置动态刷新 | nacos-config/refresh | 🔴 | A | 7 | 20 | **18/18** | ✅ 2021→2024 演进 | 深审 5 |
| ALI-A3 服务发现+注册 | nacos-discovery (37) | 🔴 | A | 7 | 20 | **20/20** | ✅ 2021.0.1.x | 深审 4 |
| ALI-A4 NacosLoadBalancer 权重 | discovery/loadbalancer | 🟡 | B | 5 | 20 | — | — | 深审 4 |
| ALI-A5 容错+心跳+优雅关闭 | discovery+registry | 🟡 | B | 6 | 20 | — | — | 深审 4 |
| ALI-A6 Sentinel 三路限流 | sentinel (21) | 🔴 | A | 8 | 20 | **15/15** | ✅ issue#3329 | 深审 4 |
| ALI-A7 Sentinel 数据源 | sentinel-datasource (18) | 🟡 | B | 6 | 20 | — | — | 深审 3 |
| ALI-A8 Sentinel Gateway+断路器 | sentinel-gateway (6)+cb (11) | 🟡 | B | 6 | 20 | — | — | 深审 4 |
| ALI-A9 Seata 分布式事务 | seata (8) | 🔴 | A | 5 | 20 | **13/13** | ✅ 三阶段演进 | 深审 3 |
| ALI-A10 RocketMQ Stream Binder | stream-rocketmq (33) | 🟡 | B | 6 | 20 | — | — | 深审 4 |

**统计**: 10 域全交付 · 5 个 harness **83/83 断言全 PASS** · 200 问 (10×20) · 深审修正 40+ 处 · 每域二轮 REVIEW 锚点复验 29 项 (1 漂移修正) · 全局三轮 REVIEW (数字自洽修正 1 处) · **四轮 REVIEW 全量脚本复验 (67 锚点 + 文字锚清零 + 类名穷举 + 数字自洽)**

**执行序**: A1 → A2 → A3 → A4 → A5 → A6 → A7 → A8 → A9 → A10 (配置 → 发现注册 → 限流 → 事务/消息)

---

## §一 10 域核心知识速查 (全量固化)

### ALI-A1 Nacos Config 配置加载 (🔴, nacos-config)

**核心机制**: 三轨并存的配置加载
- **三轨**: ConfigData 新轨 (spring.config.import=nacos: → NacosConfigDataLocationResolver SPI order=-1 → Loader) / Bootstrap 旧轨 (spring.factories → NacosPropertySourceLocator @Order(0)) / 常轨 (AutoConfiguration.imports)
- **三级递进优先级** (NacosPropertySourceLocator.java:112-122): 默认 dataId → dataId.后缀 → dataId-profile.后缀 — **每级 addFirst, profile 最高优先**
- **单例双检** (NacosConfigManager.java:49-60): 静态 INSTANCE + **static ConfigService** (L35) — 进程内唯一
- **快照容灾** (NacosSnapshotConfigManager:47-52): 读后即删 + MAX 100 — 刷新区间插队读
- **刷新节流** (Locator:164-169): refreshCount!=0 && !refreshable → 复用 Repository 不重拉
- **锚**: PROFILE_SPECIFIC preference (issue#2455) · `@since 2021.0.1.0` (ConfigData 轨) · @DeprecatedConfigurationProperty "use spring.config.import instead"
- **harness MiniNacosConfig 17/17**: 优先级反转/复合 key/快照单次消费/节流

### ALI-A2 Nacos 配置动态刷新 (🔴, nacos-config/refresh)

**核心机制**: 新旧双轨刷新
- **旧轨**: NacosContextRefresher (ApplicationReadyEvent → 遍历 Repository 注册 refreshable 监听) — 回调五连 (计数/历史/快照/事件) 发 NacosConfigRefreshEvent, **只换源不发 EnvironmentChangeEvent**
- **新轨**: NacosConfigRefreshEventListener (@since 2024.10.17) → **转发 RefreshEvent (Commons SCC-8 消费方实证!)** → ContextRefresher.refresh() 全链路
- **新旧仲裁** (NacosPropertySourceRefreshListener:98): **containsBean("nacosConfigSpringCloudRefreshEventListener")** — 新轨在则旧轨让贤
- **Smart rebind** (SmartConfigurationPropertiesRebinder, @since 2021.0.1.1): SPECIFIC_BEAN 前缀匹配 + refreshedSet 去重; 反射读私有字段; **默认 ALL_BEANS 不用 Smart** (官方注释求稳)
- **@NacosConfig 注解族**: 类/字段/方法三级 + groupKeyCache 双检 + refreshed=false 不监听
- **锚**: REFRESH_COUNT 静态 (A1 节流闸门) · NacosRefreshHistory MAX 20 + MD5
- **harness MiniNacosRefresh 18/18**: ready CAS/防重/仲裁/前缀匹配/历史环形

### ALI-A3 Nacos 服务发现+注册 (🔴, nacos-discovery)

**核心机制**: 发现双通道 + 注册仪式
- **发现双通道** (NacosDiscoveryClient:59-75): 成功**写穿 ServiceCache** / 失败 failureToleranceEnabled (默认 false) ? 缓存 : 抛异常
- **不对称失败**: getInstances 抛 / getServices 返回 emptyList (L87-88)
- **双段过滤** (NacosServiceDiscovery:58/89): 服务端 selectInstances(true) + 客户端 enabled/healthy
- **六键元数据** (L98-106): nacos.instanceId/weight/healthy/cluster/ephemeral + 用户 putAll
- **注册仪式** (NacosServiceRegistry:59-89): registerInstance + failFast 双分支; **UP/DOWN = enabled 翻转重注册** (L140-148); getStatus ip+port 反查
- **端口仲裁** (NacosAutoServiceRegistration:56-61): port<0 用 WebServer 端口 + Assert; getManagementRegistration=null; @EventListener 变更重注册
- **锚**: ServiceCache @since 2021.0.1.0 / set→setServiceIds @since 2021.0.1.1 · NamingService volatile 双检可复位 (非静态)
- **harness MiniNacosDiscovery 20/20**: 写穿/容错/过滤/不可变/状态翻转/单例复位

### ALI-A4 NacosLoadBalancer 权重 (🟡, discovery/loadbalancer)

**核心机制**: 算法插槽 + 内核复用
- **算法插槽** (LoadBalancerAlgorithm): DEFAULT_SERVICE_ID="defaultServiceId" — 每服务可插拔; Map putIfAbsent 首个生效
- **内核复用** (NacosBalancer:38 **extends nacos-client Balancer**): 权重随机零重写; **nacos.weight/nacos.healthy 从 metadata 回读** (L69-70 — A3 写入方实证)
- **选择流水线**: 空列表 EmptyResponse → cluster 隔离 (nacos.cluster) → IPv6 双栈过滤 → ServiceInstanceFilter 链 → 算法
- **子上下文装配** (NacosLoadBalancerClientConfiguration): getLazyProvider + Reactive/Blocking 分流

### ALI-A5 Nacos 容错+心跳+优雅关闭 (🟡, discovery+registry)

**核心机制**: 生命周期三段陪伴
- **订阅回写** (NacosWatch): subscribe + **ip+port 自我过滤** + metadata 回写 properties
- **心跳三条件** (HeartBeatConfiguration:33-58): AnyNestedCondition — Gateway locator / Spring Boot Admin / heart-beat.enabled — **默认不装配** (issue#2868/#3258)
- **优雅关闭** (NacosGracefulShutdownDelegate:67-81): **反注册 → sleep(waitTime) → 放行** + 子上下文过滤 (L56-62) + supportsAsyncExecution=false
- **健康镜像** (NacosDiscoveryHealthIndicator @since 2.2.0): getServerStatus 三态直读

### ALI-A6 Sentinel 三路限流 (🔴, sentinel)

**核心机制**: 三路 SphU 的织入
- **定义期注解发现** (SentinelBeanPostProcessor MergedBeanDefinitionPostProcessor): StandardMethodMetadata/ResolvedFactoryMethod 双路径 (issue#3329) + **三型强校验** (blockHandler/fallback/urlCleaner 签名硬校验 fail-fast)
- **动态拦截器注册** (L176-217): 编码 Bean 名 + **add(0) 最优先**
- **双 entry 分级** (SentinelProtectInterceptor:59-84): `METHOD:scheme://host:port` + path 级; **Degrade→fallback / Flow→blockHandler** (L118-142); finally 先 path 后 host exit
- **Feign 接管** (SentinelFeign:59): internalBuild 覆写 + **invocationHandlerFactory 锁死** + fallback 三选; SentinelInvocationHandler 只处理 HardCodedTarget, 资源 `METHOD:url+path`
- **Web 组装** (SentinelWebAutoConfiguration): 内核在 sentinel 仓库 (adapter.spring.**webmvc_v6x**) + BlockExceptionHandler 三选
- **harness MiniSentinelFlow 15/15**: 双 entry/分流/urlCleaner/Feign fallback

### ALI-A7 Sentinel 数据源 (🟡, sentinel-datasource)

**核心机制**: 六源七类型的动态装配
- **单源强校验** (SentinelDataSourceHandler:82-88): **validFields.size()!=1 → 放弃** (反射探测)
- **SmartInitializingSingleton** (L77): 所有单例后注册 + preCheck 钩子 + 动态 Bean 注册即初始化
- **转换器三分支** (L134-188): custom 需 converterClass 动态注册 / json/xml 引用内置 `sentinel-{type}-{ruleType}-converter`
- **规则七分派** (AbstractDataSourceProperties:100-108): 七 RuleManager.register2Property (含 GW_API_GROUP)

### ALI-A8 Sentinel Gateway 限流+断路器 (🟡, sentinel-gateway+cb)

**核心机制**: 网关降级双闸
- **网关薄装配** (SentinelSCGAutoConfiguration): 内核在 sentinel 仓库 (adapter.gateway.sc); 降级三途径: **用户 Bean > fallback-msg-response > redirect** (L79-126); Filter @Order(-1)
- **断路器核心** (SentinelCircuitBreaker implements SCC-10): **规则构造期 loadRules 注入** (L70-83, setResource 绑定) + run 三分支: 通过 / **BlockException 降级不 trace** (L94-99) / 业务异常 trace+降级
- **工厂族** (SentinelCircuitBreakerFactory): computeIfAbsent 幂等 + ConfigBuilder + 阻塞/Reactive 双面

### ALI-A9 Seata 分布式事务 (🔴, seata)

**核心机制**: XID 三路透传闭环
- **透传三路**: Feign header (xid 空 return) / RestTemplate 包装器 add / Web 入站 bind (本地空+rpc 非空)
- **收尾校验** (SeataHandlerInterceptor:59-76): unbind + **一致性校验 (变更 warn + 回绑)** — 防线程复用 XID 串线
- **Retryer 强制接管** (SeataFeignBuilderBeanPostProcessor): Feign.Builder → **NEVER_RETRY** (事务幂等)
- **全量注入** (SeataRestTemplateInterceptorAfterPropertiesSet): 遍历所有 RestTemplate 复制重设 — 无注解侵入
- **harness MiniSeata 13/13**: 三路透传/bind 条件/校验回绑/线程隔离

### ALI-A10 RocketMQ Stream Binder (🟡, stream-rocketmq)

**核心机制**: Stream 方言适配
- **Binder 三方法** (RocketMQMessageChannelBinder): 生产 (enabled 校验+分区拦截器) / push 消费 (DLQ 必须配 group) / pull 消费 (PolledConsumerResources)
- **三段式生产** (RocketMQProducerMessageHandler): 转换 → 队列选择 (事务与分区**互斥**, L115-116) → 发送 (TransactionMQProducer + TransactionListener)
- **错误语义**: maxAttempts>1 → retryTemplate / =1 → errorChannel; pull 错误 → **ErrorAcknowledgeHandler 三选** (用户配置/Default)

---

## §二 方法论执行报告

### 09 怀疑审计修正汇总 (ALI-PLAN)

| 修正 | 证据 | 落点 |
|:--|:--|:--|
| 版本 2025.0.0.0 | pom.xml:83 revision 实证 | ALI-PLAN v1 |
| "454 文件" → **主源 223** | find 实测 (287 含测试) | ALI-PLAN v1 |
| **域编号双文档冲突** (执行计划 A-1~A-10 vs 详细规划 A-1~A-10 定义完全不同) | 双文档对照: 执行计划含 Sidecar/ANS/OSS (仓库零文件), 详细规划含 NacosLoadBalancer/Seata/RocketMQ | **裁决: 以详细规划为准, 统一 ALI 编号** |
| 45 核心类穷举 | 42/45 存在; 3 缺失 (SentinelWebInterceptor/SentinelGatewayFilter/SentinelGatewayBlockExceptionHandler) 实为 **sentinel 仓库类** (webmvc_v6x/gateway.sc) | 边界精确化 ✅ |
| A-1 路径 | NacosPropertySourceLocator 在 **starter 模块** 非 core | 路径修正 ✅ |
| 双刷新机制并存 | core NacosContextRefresher (L70) + starter RefreshEventListener | 扩充 ✅ |
| 🔴/🟡 计数 | 规划自称 6+4, 表内标注实为 **5+5** | 二轮 REVIEW 修正 |

### 各域深审发现 (代表性)

| 域 | 最重大发现 |
|:--|:--|
| A1 | "三轨"并存 (ConfigData/Bootstrap/常轨); 快照"读后即删"单次消费 |
| A2 | 新旧仲裁 containsBean; **旧轨只换源 vs 新轨全链路** (@RefreshScope 差异) |
| A3 | getInstances 抛 vs getServices 空 — 不对称; 缓存"读时写穿"非主动 |
| A4 | **继承 nacos-client Balancer** 零重写; 权重从 metadata 回读 |
| A5 | 心跳**默认不装配** (三条件 OR); 关闭三步"反注册→sleep→放行" |
| A6 | 三路资源粒度不对称 (RT 双级 vs Feign 单级); 动态 Bean 注册织入 |
| A7 | 单源强校验"多源即弃"; 规划五源→**六源**/六规则→**七规则** |
| A8 | 规则**构造期注入** (与 A7 数据源路独立); BlockException 不 trace |
| A9 | Retryer 强制 NEVER_RETRY; 规划漏 2 个核心装配类 |
| A10 | 事务与分区**互斥**; 转换双层 (Stream 面+MQ 面) |

### harness 自抓缺陷汇总 (5 个, 每域 1-3)

- A1: Repository 静态污染 (实证进程级全局语义) / addFirst 命名语义
- A2: 跨域联动实证 (refreshCount 驱动 A1 节流) / refreshedSet 语义
- A3: 字段命名编译错 / 不对称失败+unsupported status 补断言
- A6: 规则残留污染 (全局规则隔离) / **blocked 时 entry=null 无 exit 语义实证**
- A9: preHandle 语义 isBlank 对齐 / 三路联动链断言

---

## §三 高频坑汇总 (跨域 30 条)

### 配置面 (A1/A2)
1. 三轨并存: spring.config.import / bootstrap / 常轨 — 兼容性工程
2. "profile 优先" 是 addFirst 反转实现的
3. 快照读后即删 — 单次消费不是常驻缓存
4. 刷新节流: refreshCount!=0 && !refreshable 复用旧源
5. **旧轨只换源, 新轨走 Commons 全链路** (RefreshEvent)
6. Smart rebinder 默认不用 (ALL_BEANS 求稳)
7. @NacosConfig refreshed=false 注入一次即止

### 发现注册 (A3/A4/A5)
8. failure-tolerance-enabled 默认 false — 挂了直接抛
9. getInstances 抛 vs getServices 空 — 不对称
10. UP/DOWN = enabled 翻转重注册
11. 元数据 secure 键决定 http/https
12. 权重在 metadata (nacos.weight) 不在 properties
13. clusterName 空 warn 跨集群调用
14. 心跳默认关闭 (生态条件驱动)
15. 优雅关闭先反注册再 sleep — 顺序反了摘流量就晚

### 限流 (A6/A7/A8)
16. blockHandler 必须静态 + 4 参签名 (HttpRequest, byte[], Execution, BlockException)
17. Degrade 走 fallback / Flow 走 blockHandler
18. Feign 路单级资源, RestTemplate 双级
19. 多数据源同配 = 整体放弃
20. custom dataType 必须配 converter-class
21. 断路器规则构造期注入 — 与数据源路独立
22. BlockException 降级不 trace (不算业务异常)

### 事务/消息 (A9/A10)
23. Feign retryer 被强制 NEVER_RETRY
24. XID 收尾校验: 变更 warn + 回绑
25. RestTemplate 注入是复制重设 (非直接 add)
26. DLQ topic 必须配 group
27. 事务消息不支持自定义 MessageQueueSelector (与分区互斥)
28. maxAttempts 决定重试 vs errorChannel
29. 空 rpcXid 不清理 (防误清他人 XID)
30. NamingService 可 shutDown 重建 (非静态)

---

## §四 文件路径

```
analysis/source-analysis/spring-cloud-alibaba/
├── ALI-PLAN.md                    ← 09 审计 v1 + 二轮 REVIEW (权威规划)
├── HANDOFF-SPRING-CLOUD-ALIBABA.md ← 本文 (10/10 收官唯一入口)
├── outlines/ (10 域, 每域 3-4 文件)
│   ├── ali-a1-nacos-config/  ali-a2-nacos-refresh/  ali-a3-nacos-discovery/
│   ├── ali-a4-nacos-loadbalancer/  ali-a5-nacos-fault-tolerance/
│   ├── ali-a6-sentinel-flow/  ali-a7-sentinel-datasource/  ali-a8-sentinel-gateway-cb/
│   ├── ali-a9-seata/  ali-a10-rocketmq-binder/
│   └── (每域: outline.md + completeness-questions.md (20 问) + review-notes.md (+temporal-trace.md 🔴))
├── knowledge-planning/ (10 个 KP 文件)
├── harness/ (5 个 🔴 域)
│   ├── ali-a1-nacos-config/MiniNacosConfig.java (17/17)
│   ├── ali-a2-nacos-refresh/MiniNacosRefresh.java (18/18)
│   ├── ali-a3-nacos-discovery/MiniNacosDiscovery.java (20/20)
│   ├── ali-a6-sentinel-flow/MiniSentinelFlow.java (15/15)
│   └── ali-a9-seata/MiniSeata.java (13/13)
源码: /data/workspace/source-code/code/spring/spring-cloud-alibaba/ (2025.0.0.0)
上级: ../HANDOFF-STAGE5.md (阶段 5 唯一总入口) — 5.7 已标注 ✅ 10/10 收官
后续: 阶段 5.8 Nacos (空闲?) → 5.9 Sentinel (空闲?) — 需先确认无其他 AI
```

---

## §五 完成检查单

- [x] 10/10 域全量交付 (2026-08-16): §零 状态表 + §一 10 域速查 + §三 30 坑
- [x] 每域: 大纲 + 20 问 + 六层深审 + 时空溯源 🔴 (5 个) + harness 🔴 (5 个 83/83) + 2 轮 REVIEW
- [x] 09 审计: 版本实证 + 域编号双文档裁决 + 45 核心类穷举 (3 边界外) + 计数修正
- [x] 分工确认: Gateway/OpenFeign/Dubbo/gRPC 其他 AI; Alibaba 本会话收官
- [x] 下一步: 阶段 5.8 Nacos / 5.9 Sentinel (空闲, 需先确认无其他 AI)

---

## §六 交接 REVIEW (2026-08-16, 文档发布前)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点抽查 | 4 项关键锚点全部精确 (A1 addFirst L186 / A1 双检 L53-58 / A2 前缀匹配 L104 / A3 容错 L69-70) | 通过 ✅ |
| 2 | 目录整洁度 | 0 个 .class 残留 (与 §四 声称 5 个 Mini*.java 一致); harness 全编译验证后清理 | 通过 ✅ |
| 3 | 结构完整性 | 10 outline / 10 KP / 5 harness / 5 时空溯源全部与文档一致 | 通过 ✅ |
| 4 | 数字自洽 | 规划文档自称 6🔴+4🟡 vs 表内标注 5🔴+5🟡 | **已修正** ALI-PLAN |
| 5 | **三轮 REVIEW (每域二轮)** | 10 域 review-notes 各追加二轮 REVIEW 段: **锚点复验 29 项抽查 28 精确 + 1 漂移修正** (A1 MissingEP L25-29→L51, 实证 L51/L41); 跨域一致性 10 项闭环全通过; 数字自洽全通过 | 通过 ✅ + A1 outline 修正 |
| 6 | **四轮 REVIEW (全量脚本复验)** | ① **锚点全量复验 67 个**: 文件存在性+行号有效性脚本化全通过 + **1 范围修正** (A7 postRegister L100-119→**L100-108**, 文件实 112 行) ② **文字锚清零**: 缺陷档案 #7 命中 10 处全部补行号 (A4/A5/A6/A8/A9/A10) ③ **类名穷举**: 大纲/KP/PLAN 引用类全部存在于源码 (0 编造) ④ **数字自洽**: 20 问×10=200 ✅ / harness 断言 88−5(方法定义行)=**83 与运行时 83/83 精确一致** ✅ | 通过 ✅ + A7 三文件修正 |
| 7 | **五轮 REVIEW (淘汰清单反查)** | 用户质疑淘汰清单 → **pom 依赖反查 + import 统计实证**: ① **commons 反证**: 被 3 核心模块 pom 依赖 + StringUtils **17 处 import** (NacosPropertySourceLocator/NacosServiceRegistry/NacosBalancer/RuleType 等) — 规划"公共工具不独立成域"正确但"淘汰"表述误导, 已移出淘汰清单 → **工具底座面** (io 包 0 引用, lang.StringUtils/context.PropertySourcesUtils 全量引用) ② sidecar/schedulerx/bus-rocketmq 实证淘汰: 主源码零外部 import (仅 BOM 聚合) | 通过 ✅ + ALI-PLAN 修正 |
| 8 | **六轮 REVIEW (examples 验证面)** | 136 文件全量映射 + 6 组代表性深读: ① sentinel-resttemplate (双注解叠加/loadRules 双路/资源名逐字一致) ② sclb RandomLoadBalancer (与 NacosLoadBalancer 结构同构) ③ seata 三服务 (无注解 RT 全量注入实证) ④ circuitbreaker fallback 链 ⑤ rocketmq-tx TransactionListener 三态 ⑥ orderly 选择器 + gateway BlockRequestHandler 用户优先 | 通过 ✅ + 3 处大纲强化 (A6/A8/A10) + EXAMPLES-VERIFICATION.md |

> 注: 接手后运行 harness 需先 javac 编译 (源码在 harness/*/Mini*.java, class 不保留)
