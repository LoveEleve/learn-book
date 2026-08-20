# Spring Cloud Alibaba — 知识网络化规划 (ALI-A1~A10, 09 怀疑审计后 v1)

> **日期**: 2026-08-16 | **依据**: issue/源码分析执行计划.md 阶段5.7 (10 域) + issue/SpringCloudAlibaba源码学习范围规划.md (2025.0.0.0 基准, 10 域) + 09 对既有规划保持怀疑
> **源码**: `/data/workspace/source-code/code/spring/spring-cloud-alibaba` (**2025.0.0.0**, pom.xml:83 `revision` 实证; git 单提交 d9d9be6a "prepare for 2025.0.0.0 release"; 主源 **223** / 全量含测试 **287** — 规划文档声称 454 含 examples 口径)
> **定位**: 阶段 5.7 — RPC 与服务治理第六环 **Spring Cloud 中间件集成层 (Nacos Config/Discovery + Sentinel + Seata + RocketMQ Stream)**
> **知识网络**: 承接 Spring Cloud Commons (5.4, 已收官 13/13) 的 SPI 插槽 (PropertySourceLocator/DiscoveryClient/ServiceRegistry/ReactorLoadBalancer/CircuitBreaker) 实证实现方; 为 Nacos (5.8) + Sentinel (5.9) 提供集成面前置; Seata/RocketMQ 独立中间件在 my-xhs 生态关联
> **分工确认**: Dubbo/gRPC/Gateway/OpenFeign 已被其他 AI 占用; Spring Cloud Alibaba 无人占用, 本会话开工 ✅ **2026-08-16 10/10 全量收官**

---

## 〇、09 怀疑审计表 (Spring Cloud Alibaba, 2026-08-16) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| 版本基准 | pom.xml:83 | **revision=2025.0.0.0** — 与规划基准完全一致 (2025.0.0.0 + Boot 3.5.16) | **接受** ✅ |
| "454 文件逐包扫描" | find 实测 | 全仓库 Java 318 (含测试) / **主源 223** / starter 目录 287; 454 是含 examples 的口径 | **修正口径** ⚠ 主源 223 为权威 |
| 域编号 A-1~A-10 | 双文档对照 | **执行计划 5.7 与详细规划的 A-1~A-10 内容完全不同**: 执行计划 A-1=Nacos 服务注册/含 Sidecar/ANS/OSS; 详细规划 A-1=配置加载/含 NacosLoadBalancer/Sentinel 数据源/Seata/RocketMQ | **裁决** ⚠ 以详细规划 (逐文件扫描产物) 为准, 统一编号 **ALI-A1~A10** |
| 执行计划 A-8/A-9/A-10 (Sidecar/ANS/OSS) | 模块扫描 + 依赖反查 | sidecar 13 文件/模块, schedulerx 10, ANS/OSS **仓库零文件**; sidecar/schedulerx/bus-rocketmq **主源码零外部 import** (仅 coverage/dependencies BOM 聚合) | **采纳淘汰** ✅ (实证) |
| commons 淘汰条目 | pom 依赖反查 + import 统计 | **反证**: spring-cloud-alibaba-commons 被 3 核心模块 pom 依赖 (sentinel-datasource/nacos-config/nacos-discovery) + `com.alibaba.cloud.commons.lang.StringUtils` **17 处 import** (NacosPropertySourceLocator/NacosServiceRegistry/NacosBalancer/RuleType 等) — 是工具底座, 非"不用"; io 包 (Charsets/FileUtils/IOUtils) 主源码 0 引用 | **修正** ⚠ 从淘汰清单移出 → 工具底座面 (引用面已并入各域) |
| 45 个核心类穷举 | find 逐个验证 | **42/45 存在**; 3 个缺失 (SentinelWebInterceptor/SentinelGatewayFilter/SentinelGatewayBlockExceptionHandler) 实为 **sentinel 仓库类** (com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x / adapter.gateway.sc), 集成层仅注册不实现 — 边界内精确化 | **接受+精确化** ✅ |
| A-1 路径 "spring-alibaba-nacos-config" | find 实证 | NacosPropertySourceLocator 在 **spring-cloud-starter-alibaba-nacos-config** (starter 模块, 10 文件) 非 core 模块; core 模块 (48 文件) 持 NacosConfigManager/PropertySourceBuilder | **路径修正** ✅ |
| A-2 刷新机制单轨 | grep 装配 | **双机制并存**: core NacosConfigAutoConfiguration 装配 NacosContextRefresher (L70) + starter NacosConfigSpringCloudAutoConfiguration 装配 NacosConfigRefreshEventListener → SmartConfigurationPropertiesRebinder — 新旧架构并存待 A-2 厘清 | **接受+扩充** ✅ |
| A-6 Sentinel 三路限流 | grep | SentinelBeanPostProcessor (custom/) + SentinelFeign (feign/) + SentinelWebAutoConfiguration 注册 sentinel 适配器 — 全实证 | **接受** ✅ |
| A-9 Seata 8 文件 | find | 8/8 文件: feign/ 3 + rest/ 3 + web/ 2 — 三路 XID 透传实证 | **接受** ✅ |
| A-10 RocketMQ 33 文件 | find | 33/33: binder 核心 9 类全在 integration/ + properties/ + provisioning/ + convert/ | **接受** ✅ |

> 注: 规划文档自述"逐个方法体验证" — 按 09 铁律, 每个断言开工时独立 grep 复验, 数字/类名/行号一律以 2025.0.0.0 源码为准。

---

## 一、入口点与主线 (待 Pass 0 确认)

`spring.config.import=nacos:... → NacosConfigDataLocationResolver → NacosConfigDataLoader → NacosConfigManager → NacosPropertySourceLocator` (配置加载主线) / `Nacos 配置变更 → NacosConfigRefreshEvent → RefreshEvent → SmartConfigurationPropertiesRebinder` (配置刷新主线) / `NacosDiscoveryClient → NacosServiceDiscovery → namingService.selectInstances + NacosAutoServiceRegistration → NacosServiceRegistry.register` (发现注册主线) / `SentinelBeanPostProcessor/SentinelFeign/SentinelWebAutoConfiguration 三路 SphU.entry` (限流主线) / `SeataFeignRequestInterceptor/SeataRestTemplateInterceptor/SeataHandlerInterceptor 三路 XID 透传` (事务主线) / `RocketMQMessageChannelBinder → InboundChannelAdapter/ProducerMessageHandler` (消息主线)

## 二、域清单 (10 域: 5🔴 + 5🟡, 表内标注实证 — 规划文档自称 6🔴+4🟡 与自身表内标注矛盾, 按表内修正)

| # | 域 | 模块 | 核心主题 | 方案 |
|:--:|---|---|---|---|
| 🔴 ALI-A1 | **Nacos Config 配置加载** | spring-alibaba-nacos-config (48) + spring-cloud-starter-alibaba-nacos-config (10) | NacosPropertySourceLocator / ConfigData 双轨 / NacosConfigManager 单例 / 快照与容灾 / 优先级 | 🔴 A |
| 🔴 ALI-A2 | **Nacos 配置动态刷新** | spring-alibaba-nacos-config/refresh + starter | NacosContextRefresher 旧轨 vs NacosConfigRefreshEventListener+SmartConfigurationPropertiesRebinder 新轨 / RefreshBehavior / @NacosConfig 注解 | 🔴 A |
| 🔴 ALI-A3 | **Nacos 服务发现+注册** | spring-cloud-starter-alibaba-nacos-discovery (37) | NacosDiscoveryClient/NacosServiceDiscovery 发现 + NacosServiceRegistry/NacosAutoServiceRegistration 注册 + ServiceCache 容错 | 🔴 A |
| 🟡 ALI-A4 | **NacosLoadBalancer 权重** | discovery/loadbalancer | NacosLoadBalancer 算法族 / NacosBalancer 权重随机 / 元数据过滤 | 🟡 B |
| 🟡 ALI-A5 | **Nacos 容错+心跳+优雅关闭** | discovery | ServiceCache 容错 / NacosWatch 订阅 / HeartBeatPublisher / GracefulShutdown / HealthIndicator | 🟡 B |
| 🔴 ALI-A6 | **Sentinel 三路限流** | spring-cloud-starter-alibaba-sentinel (21) + circuitbreaker-sentinel (11) | RestTemplate/Feign/Web MVC 三路 SphU.entry / SentinelProtectInterceptor / SentinelFeign 代理 / Block 处理 | 🔴 A |
| 🟡 ALI-A7 | **Sentinel 数据源** | sentinel-datasource (18) + starter | SentinelDataSourceHandler 注册 / 5 种数据源 / 6 类规则 RuleManager / preCheck | 🟡 B |
| 🟡 ALI-A8 | **Sentinel Gateway+断路器** | sentinel-gateway (6) + circuitbreaker-sentinel (11) | SentinelSCGAutoConfiguration 网关限流 / SentinelCircuitBreaker 实现 Commons C-8 | 🟡 B |
| 🔴 ALI-A9 | **Seata 分布式事务集成** | spring-cloud-starter-alibaba-seata (8) | 三路 XID 透传 (Feign/RestTemplate/Web) / SeataFeignBuilderBeanPostProcessor | 🔴 A |
| 🟡 ALI-A10 | **RocketMQ Stream Binder** | spring-cloud-starter-stream-rocketmq (33) | RocketMQMessageChannelBinder / inbound pull+push / outbound / 头映射 / 健康指标 | 🟡 B |

## 三、执行顺序 (拓扑: 配置 → 发现注册 → 限流 → 事务/消息)

**ALI-A1 → ALI-A2 → ALI-A3 → ALI-A4 → ALI-A5 → ALI-A6 → ALI-A7 → ALI-A8 → ALI-A9 → ALI-A10**

> 拓扑理由: 配置面 (加载 A1 → 刷新 A2, 同模块递进) → 发现注册 (A3, 消费 Commons SPI) → 负载均衡 (A4, 消费 A3 发现) → 容错运维 (A5, 消费 A3 注册) → Sentinel 面 (三路限流 A6 → 数据源 A7 → 网关/断路器 A8, 递进) → Seata (A9, 独立) → RocketMQ Binder (A10, 独立收束)。

## 四、知识网络图

```
← 承接: Commons 5.4 (已收官 13/13): PropertySourceLocator (SCC-1) → A1 NacosPropertySourceLocator 实证; RefreshEvent 外部发布者 (SCC-8) → A2 NacosConfigRefreshEventListener 实证; DiscoveryClient/ServiceRegistry (SCC-3/4) → A3; ReactorLoadBalancer (SCC-7) → A4; CircuitBreaker (SCC-10) → A8 SentinelCircuitBreaker
→ 引出: Nacos 5.8 (namingService 客户端面) + Sentinel 5.9 (SphU 内核) — 本阶段为集成层, 内核归独立中间件阶段
另见: Gateway 5.5/OpenFeign 5.6 (其他 AI) — Sentinel 网关限流消费 Gateway 的 GlobalFilter 面
```

## 五、完成检查单

- [x] 顶层包扫描 ↔ 域覆盖矩阵 (10 域, 13 模块实证; 淘汰 sidecar/schedulerx/bus-rocketmq; **commons 实证为工具底座非淘汰项**, 见四轮 REVIEW)
- [x] 09 审计: 版本实证 (pom.xml:83), 域编号双文档冲突裁决, 45 核心类穷举 (42 存在 + 3 边界外), 路径修正 2 处, 双刷新机制并存发现
- [x] **ALI-A1 ✅ 2026-08-16** (方案 A: 大纲 7 节/20+ 锚点/20 问 + 深审 6 项 5 修正 + 时空溯源 (三时代叠加/@since 锚) + **harness 17/17 自抓 2 缺陷** + KP; 核心: ConfigData 新轨+Bootstrap 旧轨+常轨三轨/三级递进 addFirst/静态单例/快照容灾)
- [x] **ALI-A2 ✅ 2026-08-16** (方案 A: 大纲 7 节/20+ 锚点/20 问 + 深审 6 项 5 修正 + 时空溯源 (2021 Smart → 2024 新轨演进) + **harness 18/18 双轨仲裁实证** + KP; 核心: 新旧双轨 (换源 vs 全链路)/containsBean 仲裁/Smart SPECIFIC_BEAN/@NacosConfig 注解族)
- [x] **ALI-A3 ✅ 2026-08-16** (方案 A: 大纲 7 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + 时空溯源 (2021.0.1.x 缓存/改名演进) + **harness 20/20 双通道容错实证** + KP; 核心: 发现双通道写穿缓存/不对称失败/双段过滤/六键元数据/状态翻转/端口仲裁/命名服务单例)
- [x] **ALI-A4 ✅ 2026-08-16** (方案 B: 大纲 5 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + KP; 核心: 算法插槽 SPI/继承 nacos-client 权重随机/元数据重建/选择流水线/子上下文装配)
- [x] **ALI-A5 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + KP; 核心: 订阅自我过滤/心跳三条件默认关/优雅关闭三步/健康镜像/变更事件)
- [x] **ALI-A6 ✅ 2026-08-16** (方案 A: 大纲 8 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + 时空溯源 (issue#3329/webmvc_v6x 演进) + **harness 15/15 双 entry 分流实证** + KP; 核心: 定义期注解发现双路径/三型强校验/动态拦截器注册/双 entry 分级/异常分流/Feign 接管/Web 组装)
- [x] **ALI-A7 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 6 项 3 修正 (五→六源/六→七规则) + KP; 核心: 单源强校验/反射探测/初始化后装配/转换器三分支/七 RuleManager 分派/配置工厂分离)
- [x] **ALI-A8 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + KP; 核心: 网关薄装配/降级三途径/规则构造注入/run 三分支/工厂族契约/双面装配)
- [x] **ALI-A9 ✅ 2026-08-16** (方案 A: 大纲 5 节/20+ 锚点/20 问 + 深审 6 项 3 修正 (规划漏 2 装配类) + 时空溯源 (单向携带→完整管理演进) + **harness 13/13 三路透传实证** + KP; 核心: 三路 XID 透传闭环/bind-unbind 校验回绑/Retryer 强制接管/全量注入)
- [x] **ALI-A10 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + KP; 核心: Binder 三方法契约/三段式生产 (事务+分区互斥)/push-pull 双消费/DLQ 约束/可插拔 ack)
- [x] **二轮 REVIEW (2026-08-16)**: 锚点抽查 4 项全精确 (addFirst L186/双检 L53-58/前缀匹配 L104/容错 L69-70); 目录整洁 (0 .class 残留); 结构完整 (10 outline/10 KP/5 harness); **数字自洽修正** (规划自称 6🔴+4🟡 vs 表内标注 5🔴+5🟡 — 按表内修正)
- [x] **三轮 REVIEW (2026-08-16, 每域二轮 REVIEW 全覆盖)**: 10 域各追加二轮 REVIEW 段 (review-notes.md) — 锚点复验 **29 项抽查 28 精确 + 1 漂移修正** (A1 MissingEP L25-29→L51); 跨域一致性 10 项全通过 (A1↔A2 refreshCount/快照闭环, A3↔A4 元数据回读闭环, A5↔A3 事件 restart 闭环, A6/A9 三路同构, A7↔A8 双路规则注入等); 数字自洽全通过; harness 83/83 回归通过
- [x] **四轮 REVIEW (2026-08-16, 全量脚本复验)**: ① 锚点全量 67 个 (文件+行号有效性) 脚本化验证全通过 + **1 范围修正** (A7 postRegister 100-119→100-108) ② **文字锚清零** (#7 命中 10 处全补行号: A4×4/A5×1/A6×2/A8×2/A9×2/A10×3) ③ 类名穷举 0 编造 ④ 数字自洽 (200 问 ✅ / 83 断言 = 88−5 方法定义行 ✅)
- [x] **五轮 REVIEW (2026-08-16, 淘汰清单反查 — 用户质疑驱动)**: ① **commons 反证**: 被 3 核心模块 pom 依赖 (sentinel-datasource/nacos-config/nacos-discovery) + `com.alibaba.cloud.commons.lang.StringUtils` **17 处 import** (含 NacosPropertySourceLocator:21/NacosServiceRegistry:21/NacosBalancer/NacosLoadBalancer/RuleType) + PropertySourcesUtils (NacosDiscoveryProperties:32) — 规划"公共工具不独立成域"合理但"淘汰"表述误导, **修正为工具底座面**; io 包 (Charsets/FileUtils/IOUtils/StringBuilderWriter) 主源码 0 引用 ② **sidecar/schedulerx/bus-rocketmq 实证淘汰**: 主源码零外部 import, 仅 coverage/dependencies BOM 聚合, 淘汰成立 ✅
- [x] **六轮 REVIEW (2026-08-16, examples 验证面)**: 136 文件全量映射 + 6 组代表性深读 (resttemplate 双注解/loadRules 双路/sclb 结构同构/seata 全量注入/tx 三态/orderly 选择器) — 零未覆盖机制; 3 处大纲强化回填 (A6/A8/A10) + 产出 EXAMPLES-VERIFICATION.md
- [x] 🎉 **阶段 5.7 Spring Cloud Alibaba 10/10 全量收官 (2026-08-16)**: 5 harness 83/83 断言全 PASS · 200 问 (10×20) · 交付 ALI-PLAN + 10 outline + 10 KP + 5 时空溯源 + 5 harness + 10 域二轮 REVIEW + 四轮全量脚本复验 + 五轮淘汰清单反查 + 六轮 examples 验证面
