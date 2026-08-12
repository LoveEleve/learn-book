# stage-3 · 第 01 节：[公开课] 项目介绍 — my-xhs（案例载体）知识点提取

> 课程：stage-3 三高架构 第 01 节（公开课）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/01. [公开课] 电商项目 Shopizer 介绍.md`
> 提取时间：2026-08-12 | 权重：支撑（公开课介绍 + 案例载体定位）

> **决策记录（2026-08-12，用户 G0 决策 B）**：stage-3 的"被优化对象"由 Shopizer 替换为本地 **my-xhs** 项目（`/data/workspace/my-xhs`）。
> - docs 教学主线（三高优化方法论）不变，01-04 的 Shopizer 优化计划知识点照提
> - 实例讲解与源码验证锚定 my-xhs（docs 场景 vs my-xhs 现状差异显式标注）
> - my-xhs 未实现主题（Istio/etcd/GraalVM 等）→ 方法论照提，参考实现回退官方源码（code/spring）
> - `docs/test-2/` 下业务梳理文档**仅作参考线索**，本篇及后续均以 my-xhs 源码为验证基准

---

## 一、本节概览

- **技术域**：社交+电商微服务系统（小红书克隆）——服务划分/领域建模/网关认证/服务治理/事件驱动/可观测
- **维度**：`[工程问题]`（微服务架构/领域建模/技术栈）
- **核心命题**：建立 my-xhs 的**架构基线图**——后续 02-33 篇的三高优化知识点都将锚定本项目验证
- **知识点数**：10 个
- **前置**：REST API（stage-1 第 3/4 节）、Spring Boot/Spring Cloud、微服务概念、电商领域概念（SPU/SKU）

## 前置条件清单
读者需先掌握：
1. **REST API 服务端/客户端设计**（stage-1 第 3/4 节）
2. **Spring Boot 3 / Spring Cloud / Spring Cloud Alibaba 基础**
3. **微服务概念**：注册中心/配置中心/网关/服务间调用
4. **电商+社交领域概念**：SPU/SKU、笔记/关注（本篇会补基础）
未达前置者，先补：stage-1 第 3/4 节 + 本篇 KP-04/05 基础描述

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **案例定位**：my-xhs 是"被优化对象 + 参考实现验证层"——每篇 docs 知识点用它验证，不重复深挖其业务细节
- **工程化弱**：模块结构/依赖管理补一句即可
- **必做**：全篇源码验证（已对 pom/模块/网关过滤器/消费者/trace 公共模块实证）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 my-xhs 项目定位（社交+电商微服务，替代 Shopizer 的案例载体）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：无
- **来源**：`/data/workspace/my-xhs/pom.xml` + G0 决策记录
- **需求**：认识 stage-3 被优化对象——为什么换掉 Shopizer
- **自主实现**：若我设计——"被优化对象"应是**现代技术栈 + 真实微服务形态 + 本地源码可读**的项目
- **参考实现**（源码 + 决策）：**my-xhs** = 小红书克隆（社交+电商），`pom.xml:12` description「小红书克隆项目 - 社交+电商微服务系统」；19 模块（`pom.xml:14-35`，BOM/common + 15 业务服务 + test/benchmark）；15 个 Spring Boot 启动类（`*Application.java` 每服务一个，find 实证）。**替换动机**（架构师）：Shopizer（Boot 2.5.12/javax/ES7，模块化单体）在「主流性 + 源码可验证性 + 主题覆盖度」上弱于 my-xhs（JDK17/jakarta/Boot 3.2.5，已实现网关/事件/可观测/多活全套三高设施）
- **对比取舍**：**docs 场景 vs my-xhs 现状**——docs 01-04 讲 Shopizer 是"要被优化的单体"；my-xhs **已是微服务且已实现大部分优化目标**，后续篇需标注此差异（方法论照提、实例对照现状）
- **测试佐证**：`pom.xml:14-35`（modules 清单）+ 15 个 `*Application.java`

### KP-02 微服务划分与模块结构（架构基线图）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Maven 多模块、微服务概念
- **来源**：`pom.xml:14-35` + 各模块包结构（grep 验证）
- **需求**：拿到 stage-3 后续 32 篇的"地图"——每个主题落哪个模块
- **自主实现**：若我设计——按业务域拆服务 + 公共依赖层（BOM/common）+ 独立网关
- **参考实现**（源码验证）：**19 模块**（`pom.xml:14-35`）——`my-xhs-bom`（依赖管理）、`my-xhs-common`（公共库）、15 服务：`gateway/user/content/analytics/counter/product/order/inventory/cart/coupon/search/home/im/notification/payment`、`my-xhs-test`（测试）、`my-xhs-benchmark`（压测）。**common 公共库**（`my-xhs-common/src/main/java/com/myxhs/common/`，**27 个包**，ls 实证）含：tcc/trace/mq/loadbalancer/zone/id/cache/datasource/chaos/xxljob/spel/metrics 等——**三高能力的公共实现层**（后续多篇的知识点落点）
- **对比取舍**：**按域拆服务**（用户/内容/商品/订单/支付/库存/购物车/优惠券/搜索/首页/IM/通知/点赞计数/行为分析）——服务粒度=域边界，数据隔离
- **测试佐证**：根 `pom.xml:14-35` modules；`*Application.java` 15 个（find 实证）

### KP-03 技术栈基线（现代主流栈，对照 docs 01 的 Shopizer 旧栈）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]`（my-xhs 栈） / `[过时→已迁移]`（Shopizer 旧栈对照） | **置信度**：High
- **前置**：Spring Boot/Cloud
- **来源**：根 `pom.xml:39-50/61` + 各模块 pom
- **需求**：明确"基线技术栈"——后续容器调优（05）、HTTP 架构（09）、RPC 架构（10）等篇的验证基准
- **自主实现**：若我设计——JDK 当前 LTS + Boot 最新稳定 + SCA 对齐版本
- **参考实现**（源码验证）：**JDK 17 + Boot 3.2.5 + Spring Cloud 2023.0.3 + SCA 2023.0.1.2**（根 pom.xml：39 java.version / 44 spring.boot.version / 45 spring.cloud.version / 46 spring.cloud.alibaba.version）；MyBatis-Plus 3.5.7 + MySQL 8.0.33（49/50）；elasticsearch-java 8.12.2（61 + my-xhs-search/pom.xml:39）；Redis；RocketMQ；Nacos；Sentinel；XXL-Job（common/xxljob）
- **对比取舍**：**javax→jakarta 命名空间迁移**（04 §3.2.1 迁移≠机制）：docs 01 的 Shopizer 基线（Boot 2.5.12/javax）`[过时→3.x/jakarta]`；**my-xhs 已用 jakarta（Boot 3.2.5）**——迁移是 namespace 变化，机制（JPA/Servlet 抽象）时间无关
- **测试佐证**：`pom.xml`（java.version 17 / spring.boot.version 3.2.5 / spring.cloud.version 2023.0.3 / spring.cloud.alibaba.version 2023.0.1.2 / mybatis.plus.version 3.5.7 / mysql.connector.version 8.0.33 / elasticsearch.version 8.12.2）

### KP-04 领域建模（社交域 + 电商 SPU/SKU 域）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：SPU/SKU 概念
- **来源**：`my-xhs-product/src/main/java/com/myxhs/product/entity/`（Sku.java/Spu.java/Category.java）+ 模块划分（docs 01 §产品 API 框架对照）
- **需求**：理解 my-xhs 的两大领域模型——社交（内容）与电商（商品），后续数据/缓存优化（11/12 节）的领域背景
- **自主实现**：若我设计——商品用 SPU/SKU 两层（规格组 vs 可售单元）；社交用笔记/关注关系
- **参考实现**（源码验证）：**商品域**——`entity/Spu.java`（标准产品单元）、`entity/Sku.java`（库存销售单元）、`entity/Category.java`（类目）；`dto/request/SkuCreateRequest.java`、`dto/response/SkuVO.java`/`SpuDetailVO.java`。**docs 01 框架对照**：Shopizer 用「产品定义 vs 实例」两层，my-xhs 用 **SPU vs SKU** 两层——同一建模范式（可变属性下沉到 SKU，避免属性爆炸）；**docs 产品 API 细分（选项/变体/实例组/品牌/类型）在 my-xhs 商品域无完整对应**——my-xhs 建模更简（SPU/SKU/类目 3 实体），差异属实例简化，标注不硬套；**社交域**——content 模块（笔记/关注），analytics 模块消费者（Follow/Like/Favorite 事件）
- **对比取舍**：**SPU/SKU vs 定义/实例**——同为"不可变基础 + 可变销售单元"两层建模（时间无关模式）；名称不同，机制相同
- **测试佐证**：product/entity/{Spu,Sku,Category}.java + analytics/consumer/{Follow,LikeUnlike,FavoriteUnlike}Consumer.java

### KP-05 自研 API 网关（19000）与认证（JWT/HMAC/灰度/限流）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：Spring Cloud Gateway、JWT、限流概念
- **来源**：`my-xhs-gateway/src/main/java/com/myxhs/gateway/`（config/filter/handler）+ `application.yml`
- **需求**：理解 my-xhs 网关面——后续 19/20 节（API 网关/RPC 网关）的验证主体
- **自主实现**：若我设计——统一入口做认证/限流/灰度/可观测横切
- **参考实现**（源码验证）：**7 个自定义过滤器**（`filter/`）：`GatewayAuthFilter`（JWT 认证）、`HmacSignatureFilter`（HMAC 签名校验）、`RateLimitFilter`（限流）、`GrayRouteFilter`（灰度路由）、`TrafficColoringFilter`（流量染色）、`ApiVersionFilter`（API 版本）、`RequestLogFilter`（请求日志）；**handler/**：`CachingFilteringWebHandler`、`GlobalExceptionHandler`；**config/**：`GatewayConfig`（含 Sentinel 网关限流规则 Bean `sentinel-json-gw-flow-converter`，规则从 Nacos 拉取）、`RateLimiterConfig`、`AuthProperties`；端口 **19000**（application.yml:2）；**JWT 认证链路（代码实证）**——`user/AuthController.java:18` `@RequestMapping("/api/user/auth")`、`:28` GET `/captcha`、`:49` POST `/login`（LoginRequest 校验 + userService.login）；网关侧 `GatewayAuthFilter.java:34-37`（白名单放行 → 提取 Bearer Token → 解析 JWT 校验签名/过期/Token 类型）、`:59` 黑名单 Redis Key `myxhs:user:token:blacklist:`（注销/踢出场景）、`:98-100`（Bearer 提取 + 解析）
- **对比取舍**：**自研过滤器链 vs 纯 SCG 内建**——my-xhs 在 Spring Cloud Gateway 基础上扩展 7 类横切（灰度/染色/签名/版本）——网关分层模式（框架路由 + 业务横切过滤器）
- **测试佐证**：gateway/filter/ 7 文件 + GatewayConfig.java:32-34（sentinel-json-gw-flow-converter）+ application.yml:2/37/56

### KP-06 服务治理基础设施（Nacos 注册/配置 + Sentinel 限流 + XXL-Job + 自定义负载均衡）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：注册中心/配置中心（stage-2 23/24）、限流
- **来源**：`my-xhs-gateway/src/main/resources/application.yml`（nacos config import：`my-xhs-gateway.yaml`）+ common/loadbalancer + common/xxljob
- **需求**：理解 my-xhs 的服务治理底座——衔接 stage-2 已提取的 Nacos/Sentinel 知识点在真实项目的落地
- **自主实现**：若我设计——注册+配置用 Nacos、限流用 Sentinel（规则中心化）、定时任务独立调度
- **参考实现**（源码验证）：**Nacos**——注册中心（各服务 `spring.cloud.nacos`，gateway application.yml:37）+ **配置中心**（`spring.config.import` 拉取 `my-xhs-gateway.yaml`，application.yml:56，密钥已迁至 Nacos Config）；**Sentinel**——服务限流 + 网关流控（`sentinel-json-gw-flow-converter` 从 Nacos 数据源加载 `GatewayFlowRule`）；**XXL-Job**——common/xxljob（定时任务），部署 `config/docker-compose.yml:493`（xuxueli/xxl-job-admin:2.4.2，:59 端口 18080）；**自定义负载均衡**——common/loadbalancer/`LeastConnectionsLoadBalancer.java`（最小连接数，扩展 Spring Cloud LoadBalancer）
- **对比取舍**：**配置中心化**——secret/hmac-secret 迁移 Nacos（注释实证）——治理一致性 vs 本地明文简单性
- **测试佐证**：gateway application.yml:37/51-57 + GatewayConfig.java:26-34 + common/loadbalancer/LeastConnectionsLoadBalancer.java

### KP-07 事件驱动与消息一致性（RocketMQ + 幂等 + DLQ + TCC）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：消息队列（stage-2 17 本地消息表）、幂等、TCC（stage-2 18）
- **来源**：common/mq（MessageIdempotentHelper/DlqMessageHandler）+ common/tcc（TccFenceService）+ analytics/consumer/* + RocketMQ 引用 68 文件（grep 实证）
- **需求**：理解 my-xhs 的异步化与一致性方案——16/17 节（分布式事件/Reactive）的验证主体
- **自主实现**：若我设计——业务事件发 MQ，消费端幂等 + 死信兜底；强一致场景 TCC
- **参考实现**（源码验证）：**RocketMQ 事件驱动**——analytics 消费者 `FollowConsumer/UnfollowConsumer/LikeUnlikeConsumer/FavoriteUnlikeConsumer`（关注/点赞/收藏异步化），全项目 68 个 java 文件引用 RocketMQ（grep 实证）；**幂等**——common/mq/`MessageIdempotentHelper`（消费幂等）；**DLQ**——common/mq/`DlqMessageHandler`（死信处理）；**TCC**——common/tcc/`TccFenceService`（防悬挂/空回滚，衔接 stage-2 18 节 Seata TCC）
- **对比取舍**：**MQ 事件 + 幂等 + DLQ vs 同步调用**——解耦/削峰 vs 一致性强依赖；**TCC 仅用于强一致场景**（fence 防幂等问题）
- **测试佐证**：analytics/consumer/ 4 消费者 + common/mq/2 类 + common/tcc/TccFenceService.java

### KP-08 可观测性设施（SkyWalking + Prometheus + Grafana + Logstash + 链路追踪公共层）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：可观测性三支柱（stage-1 13-20）
- **来源**：`/data/workspace/my-xhs/skywalking-agent-9.6.0/` + `config/docker-compose.yml` + common/trace/ 6 文件
- **需求**：理解 my-xhs 的可观测底座——29/30 节（日志平台/监控平台）的验证主体
- **自主实现**：若我设计——Agent 无侵入埋点 + 指标/日志旁路采集 + trace 上下文线程透传
- **参考实现**（源码验证）：**链路追踪**——SkyWalking Agent 9.6.0（本地 agent 目录）+ common/trace/：`TraceContextHolder`（trace 上下文）、`MdcAwareExecutorService`（**线程池 MDC 透传——异步场景 trace 不断链**）、`FeignTraceInterceptorConfig`（Feign 调用透传）、`MqTraceHelper`（MQ 链路透传）、`BizSpanHelper`/`TraceContext`；**指标**——Prometheus（docker-compose）+ Grafana（config/grafana）；**日志**——Logstash（config/logstash）
- **对比取舍**：**Agent 无侵入 vs 代码埋点**——采集端解耦 vs 自定义能力受限；**MDC 线程透传**是异步化后 trace 完整性的关键实现（易踩坑）
- **测试佐证**：common/trace/ 6 类 + skywalking-agent-9.6.0 目录 + config/{prometheus,grafana,logstash}

### KP-09 多活/区域路由与数据层（zone + 多实例 MySQL）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：Medium
- **前置**：多活概念、MySQL 主从（stage-2 25/26）
- **来源**：common/zone/（17 个 java 文件：6 顶层类 + loadbalancer/redis/datasource 子包）+ config 部署 compose
- **需求**：理解 my-xhs 的区域路由与数据层形态——11/12 节（MySQL 高可用/数据存储）的验证主体
- **自主实现**：若我设计——按 zone 隔离数据源/Redis/调用，请求按 zone 亲和路由
- **参考实现**（源码验证）：**zone 公共层**（common/zone/，17 个 java 文件）——`ZoneContext`（区域上下文）、`ZoneResolver`（区域解析）、`ZonePreferenceFilter`（区域偏好路由过滤器）、`ZoneContextAutoConfiguration`、`ZoneProperties`、`ZoneConstants`，子包：`loadbalancer/`（`ZonePreferenceServiceInstanceListSupplier`、`ServiceInstanceZoneResolver`、`ZoneLoadBalancerConfiguration`——区域亲和负载均衡）、`redis/interceptor/`（`RedisMethodInterceptor`、`EventPublishingRedisCommandInterceptor`——按区 Redis 命令拦截）、`datasource/`（按区数据源）——**多活基础设施雏形**；**数据层**——MySQL **主从架构**：生产部署 compose 实证 **3306 主 / 3307 从**（config/production-env-config/.../docker-compose.yml:143/190）；`config/docker-compose.yml:83` 注释「MySQL 4主4从→1(3306)」（2026-08-08 简化记录）——**注意 test-2 交接文档的 13306-13309 四实例为旧架构记录，以当前 config 为准**；MyBatis-Plus、Redis 6379
- **对比取舍**：**区域亲和路由**（请求落就近 zone）vs 全局单活——可用性提升 vs 数据一致性/复杂度上升
- **测试佐证**：common/zone/ZoneContext.java + ZonePreferenceFilter.java + zone/{datasource,loadbalancer,redis}

### KP-10 搜索（Elasticsearch 8.12.2 + MQ 同步 + 推荐）
- **维度**：`[工程问题]` | **权重**：`[边缘]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[有效]` | **置信度**：Medium
- **前置**：ES 概念
- **来源**：`my-xhs-search/pom.xml:39`（elasticsearch-java）+ search 模块包结构（consumer/job/recommend）
- **需求**：了解搜索链路（docs 01 §产品搜索 的 my-xhs 对应落地）
- **自主实现**：若我设计——业务库变更发 MQ，消费端同步 ES；检索与事务库分离
- **参考实现**（源码验证）：elasticsearch-java 8.12.2；search 模块 `consumer`（MQ 消费同步）、`job`（定时同步兜底）、`recommend`（推荐）、`feign`（服务调用）
- **对比取舍**：**ES 8.12.2 vs docs 01 Shopizer ES 7.5.2**——my-xhs 为现代版本（8.x 破坏性升级后）；**搜索与主库分离** = CQRS 读模型
- **测试佐证**：my-xhs-search/pom.xml:39 + search/{consumer,job,recommend,feign}

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| my-xhs 项目定位（案例载体决策） | 工程问题 | 支撑 | P1 | 🟢 | 有效 | High |
| 微服务划分与模块结构（架构基线图） | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 技术栈基线（Boot3.2.5/SCA/jakarta） | 工程问题 | 支撑 | P1 | 🟡 | 有效 | High |
| 领域建模（SPU/SKU + 社交域） | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| 自研网关与认证（JWT/HMAC/灰度/限流） | 工程问题 | 支撑 | P1 | 🟡 | 有效 | High |
| 服务治理（Nacos/Sentinel/XXL-Job/负载均衡） | 分布式问题 | 支撑 | P1 | 🟡 | 有效 | High |
| 事件驱动（RocketMQ/幂等/DLQ/TCC） | 分布式问题 | 支撑 | P1 | 🟡 | 有效 | High |
| 可观测（SkyWalking/Prometheus/Grafana/trace 透传） | 性能优化 | 支撑 | P2 | 🟡 | 有效 | High |
| 多活/区域路由（zone + MySQL 主从 3306/3307） | 分布式问题 | 支撑 | P2 | 🟡 | 有效 | Medium |
| 搜索（ES 8.12.2 + MQ 同步） | 工程问题 | 边缘 | P3 | 🟢 | 有效 | Medium |

> **权重说明**：KP-02 架构基线图核心 P1（后续 32 篇的地图）；其余支撑/边缘。docs 01 的 Shopizer 具体功能清单（类目/营销/支付/运输模块等）经本表 KP 对照 my-xhs 模块落位（product/cart/coupon/order/payment/notification），不再逐项展开——案例载体不深挖。

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`/data/workspace/my-xhs`（19 模块，15 服务）——**stage-3 的参考实现验证主体**（替代 Shopizer 轻量验证）
- **关键源码**（本次实证）：
  - 根 `pom.xml`（modules 14-35 / JDK17:39 / Boot 3.2.5:44 / SCA 2023.0.1.2:46 / ES 8.12.2:61）
  - `my-xhs-common/src/main/java/com/myxhs/common/`（tcc/trace/mq/loadbalancer/zone/id/cache/xxljob 等 27 包）
  - gateway（filter 7 类 / GatewayConfig / application.yml:2/37/56）
  - product/entity/{Spu,Sku,Category}.java；analytics/consumer/4 消费者；search pom ES 8.12.2
- **诚实标注**：`docs/test-2/` 业务文档仅作线索（本篇未引用其结论，所有事实均重新 grep/读码实证；认证链路/XXL-Job 端口/MySQL 拓扑均已由代码与 config 实证，test-2 中的 13306-13309 旧架构信息已弃用）；zone 包的完整机制（如 ZoneResolver 的解析策略）未深读，后续 11/12 节再展开
- **关联标注**：衔接 stage-2 已提取知识——Nacos（23/24 配置、7/8 一致性）、Sentinel（stage-1 08 容错）、Seata TCC（stage-2 18）、MQ 幂等（stage-2 17）；后续篇将逐点深化

---

## 五、本节小结（三层次视角）

**需求**：建立 stage-3 的"被优化对象"——my-xhs 的架构基线，替换 docs 案例 Shopizer。

**自主实现核心**：若我设计三高架构——按域拆服务 + common 公共能力层（tcc/trace/mq/zone）+ 自研网关横切（认证/限流/灰度）+ Nacos 治理 + RocketMQ 事件化 + SkyWalking 可观测。

**参考实现**：my-xhs 已全部落地（**已源码验证**）：19 模块/15 服务、7 类网关过滤器、RocketMQ 68 处引用、common 27 包公共能力层、zone 区域路由、ES 8.12.2。

**对比取舍**：知识本体是"**三高优化方法论**"（docs 主线）；my-xhs 提供"**现代工程实例**"。与 Shopizer 对照——docs 的"被优化单体" vs my-xhs 的"已优化微服务"，后续每篇标注差异。

**待验证汇总**：
- zone 包机制细节（ZoneResolver 解析策略/Redis 命令拦截器）——11/12 节展开
- 网关路由规则（Nacos 配置 `my-xhs-gateway.yaml` 内容未读）——19/20 节展开
- RocketMQ 消息拓扑全貌——16/17 节展开
- my-xhs 商品域与 docs 产品 API 细分（选项/变体/实例组）差异细节——12 节数据存储展开
- MySQL 主从读写分离是否启用（3306/3307 实证存在，配置路径待确认）——11 节展开

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 01 为「Shopizer 介绍」骨架（功能表格 + API 清单），my-xhs 实例内容全部源码实证；"docs 明确内容"为 docs 原文，"架构师发散"为补全。

### 完整认知：一个现代微服务电商系统完整该讲什么

docs 01 讲了功能/API/技术栈。以 my-xhs 为实例，完整架构认知还该包含：

1. **服务划分的域边界**（架构师发散）：15 服务按业务域拆（用户/内容/商品/订单/支付/库存/购物车/优惠券/搜索/首页/IM/通知/点赞计数/行为分析）——**域内数据自持、域间事件通信**是微服务的核心命题（第 06 节微服务升级的落地形态）
2. **common 公共能力层是三高设施的载体**（源码实证）：tcc/trace/mq/zone/loadbalancer 全部下沉公共层——**横切能力复用** vs 各服务自实现（工程权衡，衔接第 03/04 节优化准备）
3. **网关是全站横切汇聚点**（源码实证）：认证/签名/限流/灰度/染色/版本 7 类过滤器——网关不只是路由，是**安全+治理+发布策略**的统一入口（第 19/20 节网关主题）
4. **事件驱动是服务解耦的默认选择**（源码实证）：关注/点赞/收藏等社交行为全部异步化（analytics 消费者）+ 幂等/DLQ/TCC 兜底（第 16/17 节）
5. **可观测三支柱落地**（源码实证）：SkyWalking 链路 + Prometheus/Grafana 指标 + Logstash 日志，且 **MDC/线程池透传**保证异步场景链路完整（第 29/30 节）
6. **多活/区域路由是更高阶可用性**（源码实证）：zone 上下文 + 区域亲和（数据源/Redis/负载均衡分 zone）——单集群高可用之上的一层（第 11/12 节数据高可用衔接）
7. **docs 01 的 Shopizer 功能清单 ↔ my-xhs 模块对照**（docs 明确 → 实例落地）：类目产品管理→product、购物车→cart、营销/促销→coupon、订购→order、付款→payment、运输→（my-xhs 未实现 `[待验证]`）、内容管理→content、搜索→search——**同一个"电商系统功能全景"两份实现**，docs 框架 + my-xhs 实例

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 案例载体 Shopizer → my-xhs | 现代栈+源码可验+主题全覆盖 vs docs 01-04 场景错位（需标注差异） |
| 按域拆 15 服务 | 独立扩容/故障隔离 vs 跨域一致性成本（事件化解决） |
| common 27 包公共能力层 | 横切复用 vs 公共层变"大杂烩"风险（需依赖治理） |
| 自研 7 类网关过滤器 | 灰度/染色/签名等定制能力 vs 框架升级兼容成本 |
| 配置中心化（Nacos） | 动态/审计 vs 强依赖基础设施（启动链路） |
| 事件 + 幂等 + DLQ + TCC | 解耦削峰 vs 最终一致复杂度；强一致才用 TCC |
| zone 区域路由 | 可用性/延迟 vs 一致性与运维复杂度 |
| JWT 无状态认证 | 水平扩展友好 vs 撤销/续期复杂 |

### 常见坑/反模式

1. **把 docs 案例（Shopizer）当知识本体**（本组最大坑）：已决策换 my-xhs，但 01-04 的方法论知识点仍需按 docs 提取，只换实例
2. **docs 场景硬套 my-xhs**：docs 06 讲"单体拆微服务"，my-xhs 已是微服务——差异标注，不硬套
3. **common 层膨胀**：公共层包罗万象（27 包）——后续提取时注意按依赖方向收敛
4. **异步链路断 trace**：异步/线程池/MQ 场景不透传 MDC——my-xhs 用 `MdcAwareExecutorService`/`MqTraceHelper` 解决，这是生产高频坑
5. **幂等只做消费端**：RocketMQ 至少一次投递，消费必须幂等（`MessageIdempotentHelper`）——消息重复是默认假设
6. **命名空间迁移**：javax→jakarta 是迁移≠机制（my-xhs 已 jakarta，docs Shopizer 旧栈对照标注）

### 生态位置

- **stage-3 教学主线**：本篇是 01-04「项目准备组」第一篇——建立架构基线图（KP-02）→ 02 优化计划 → 03/04 优化准备 → 05 起逐项实操验证
- **前后篇衔接**：stage-1 REST/容错/可观测（3/4、8、13-20）→ stage-2 Nacos/事务/MQ（7/8、17/18、23/24）→ 本篇整合 → 后续每篇以 my-xhs 落点验证（05 容器、09 HTTP、10 RPC、11/12 数据、16/17 事件、19/20 网关、29/30 可观测）
- **与源码提取的关系**：my-xhs 作 stage-3 验证主体（支撑/边缘权重），不进入 source/ 主提取；`03-MYXHS-DEEP-READ`（15 周深读计划）与提取互补：提取管知识结构，深读管代码 WHY

**架构师视角结论**：本篇的产出不是"背功能清单"，而是**一张可定位的架构基线图**：15 服务边界 + common 能力层 + 网关横切 + 治理底座 + 事件化 + 可观测 + 区域路由。后续 32 篇的每个知识点都能在这张图上找到落点——"docs 方法论 × my-xhs 实例"是本 stage 的验证范式。
