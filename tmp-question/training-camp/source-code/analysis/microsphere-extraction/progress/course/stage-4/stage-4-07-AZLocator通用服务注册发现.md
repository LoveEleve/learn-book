# stage-4 · 第 07 节：第五节：Spring Cloud 服务注册与发现多活架构通用设计与实现 — 知识点提取

> 课程：stage-4 多活架构 第 07 节（通用化组 07-09 第一篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/07. 第五节：Spring Cloud 服务注册与发现多活架构通用设计与实现.md`
> 提取时间：2026-08-12 | 权重：核心（AZ Locator 抽象——多活架构框架的核心设计 + 通用注册发现）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**本篇为 microsphere 框架设计文档（非 Eureka 文档）——机制 = microsphere-multiactive 源码实证 + my-xhs zone 包应用实证**

> **文档形态**：**microsphere 多活框架设计文档（218 行）**——主要内容：①AZ Locator 抽象设计（ZoneLocator 核心 API/7 原则/内建实现/ZoneContext/ZonePreferenceFilter）②基于抽象实现通用注册发现多活（适配 Eureka/Nacos/ZK/consul——docs:4）；**源码实证层**：`code/microsphere/microsphere-multiactive`（全部类名命中）+ my-xhs `common/zone/`（应用实现同构）——**三层对应：docs 设计 → multiactive 源码 → my-xhs 应用**。

---

## 一、本节概览

- **技术域**：可用区域定位（Availability Zones Locator 抽象）、同区域优先路由（ZonePreferenceFilter）、通用注册发现多活适配
- **维度**：`[分布式理论]`（区域抽象设计）+ `[工程问题]`（框架 API/SPI/原则）+ `[分布式问题]`（同区域优先/多注册适配）
- **核心命题**：**多活架构框架的核心抽象——Availability Zones Locator**——docs 两条主线：①ZoneLocator 抽象设计（定位器/上下文/过滤器——7 设计原则）②通用注册发现多活（一套抽象适配 Eureka/Nacos/ZK/consul）；**知识本体 = 区域感知的通用抽象层**（与注册中心产品无关——05/06 篇多注册机制的"通用化"）
- **知识点数**：6 个
- **前置**：05 篇（多注册中心）、02 篇（Region/AZ）、stage-3 01（my-xhs zone 包基线）

## 前置条件清单
读者需先掌握：
1. **多区域概念**（02 篇 KP-05——Region/AZ 故障域）
2. **多注册中心机制**（05 篇——客户端合并/适配）
3. **Spring SPI/Ordered/组合模式**（本篇 7 原则的基础）
4. **my-xhs zone 包基线**（stage-3 01 KP-09——17 文件实证）
未达前置者，先补：05 篇 / 02 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **源码实证三层对应**：docs 设计 → `microsphere-multiactive` 源码（全部类名实证）→ my-xhs zone 包（应用实现）
- **机制为本**：AZ Locator 是**产品无关抽象**（docs 明确适配 Eureka/Nacos/ZK/consul）——Eureka 仅 docs 适配列表一行
- **实例对照**：my-xhs zone 包 = 抽象的应用落地（ZoneContext/ZonePreferenceFilter 同构实证）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 ZoneLocator 抽象设计（核心 API + 7 设计原则）【docs 主要内容①主体】
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring SPI/Ordered
- **来源**：docs §核心 API/§设计原则（docs:10-43）+ `microsphere-multiactive` 源码实证
- **需求**：**多活架构的区域定位抽象**——docs 明确：ZoneLocator 是可用区域定位器核心 API（docs:12），支持 AWS、Aliyun 等云平台（docs:3），为分布式服务调用/缓存/数据库提供区域能力
- **自主实现**：若我设计——区域定位 = **"当前应用在哪个可用区"的解析抽象**：多种来源（配置/云元数据端点）→ 统一接口 + 优先级裁决
- **参考实现**（docs 原则 + multiactive 源码实证）：**7 设计原则（docs:14-43 逐条）**——①**范围原则**（贴合容器特性：Spring Bean/Spring SPI——SpringFactoriesLoader，docs:16-20）②**优先级原则**（运行环境不确定 → 多实现按优先级裁决，有且仅有一个 ZoneLocator 被应用——实现 Ordered 接口；实现成本由简到繁/依赖版本由低到高/类同实现步进 5-10，docs:22-27）③**兼容性原则**（多方案但不强制依赖，docs:29-30）④**最低依赖原则**（不依赖第三方，docs:32-33）⑤**设计模式原则**（组合模式——CompositeZoneLocator；组合对象 = Primary Bean、成员 = 依赖，核心代码只与单一 ZoneLocator 交互，docs:35-36）⑥**扩展不可信任原则**（SPI/API 的第三方实现不确定，docs:38-39）⑦**异构系统交互原则**（异构系统取交集——如应用与 JVM Agent 依赖 JDK API，docs:41-42）；**源码实证**——`ZoneLocator` 接口 **`[本地实证：microsphere-multiactive-spring `io.microsphere.multiple.active.zone.spring.ZoneLocator`]`** + `AbstractZoneLocator`（同包——抽象基类）
- **对比取舍**：**抽象层（多实现+优先级）vs 单一实现**——环境不确定性（云/本地/容器）决定"可插拔+优先级"是必要设计；**Ordered 优先级（5/10/15/20 步进）**是"环境特化优先于通用"的裁决机制
- **机制/说明**：ZoneLocator 是**"区域感知的 SPI 抽象"**——7 原则本质 = **SPI 设计的最佳实践集合**（范围/优先级/兼容/低依赖/组合/不可信扩展/异构交集）——**可复用为任何"环境感知"抽象的模板**（云平台/容器/本地）
- **测试佐证**：docs:10-43（7 原则逐条）+ `ZoneLocator.java`/`AbstractZoneLocator.java`（本地实证）

### KP-02 内建实现与优先级（Default(20)/AWS 三实现(5/10/15)/Composite）【docs §内建实现】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01（Ordered 优先级）
- **来源**：docs §内建实现（docs:44-58——Default + AWS 表）+ multiactive 源码实证
- **需求**：**ZoneLocator 的内建实现谱系**——docs 明确：默认实现（配置来源）+ AWS 三实现（云元数据）+ 组合实现（Primary）
- **自主实现**：若我设计——实现谱系：默认（配置文件——兜底）+ 环境特化（云元数据端点——自动发现）+ 组合（多实现聚合裁决）
- **参考实现**（docs 表 + 源码实证）：**DefaultZoneLocator（docs:46-47）**——Order=20（**优先级最低——兜底**），区域来自配置 `microsphere.availability.zone` **`[本地实证：multiactive-spring `zone/spring/DefaultZoneLocator`]`**；**AWS 三实现（docs:50-54 表照录）**——`EcsContainerMetadataFileZoneLocator`（ECS container agent 1.15.0+，本地文件，**Order=5**）/`EcsTaskMetadataEndpointV4ZoneLocator`（agent 1.39.0+，Web Endpoint，**Order=10**）/`Ec2AvailabilityZoneEndpointZoneLocator`（EC2，Web Endpoint，**Order=15**）——**`[本地实证：multiactive-aws 三实现全部命中]`**——**Order 值越小越优先：ECS 文件(5) → 任务端点(10) → EC2(15) → 默认配置(20)**——**环境越特化越优先（Order 最小者胜出）**；**CompositeZoneLocator（docs:58 + 设计模式原则）**——组合实现（Primary Bean）**`[本地实证：multiactive-spring `zone/spring/CompositeZoneLocator`]`**
- **对比取舍**：**配置（显式可控）vs 云元数据（自动发现）**——兜底 vs 特化；**组合（多实现聚合）vs 单一**——扩展性 vs 确定性——**优先级裁决 + 组合 = 环境的完备覆盖**
- **机制/说明**：实现谱系的**优先级语义**——**Order 越小越优先**（5=ECS 文件 > 10=任务端点 > 15=EC2 > 20=默认配置）——**自动发现优先于显式配置、特化优先于通用**；配置 `microsphere.availability.zone` 是最终兜底
- **测试佐证**：docs:50-54（AWS 表照录）+ multiactive-aws 三实现（本地实证）+ DefaultZoneLocator/CompositeZoneLocator（本地实证）

### KP-03 ZoneContext 可变状态与同区域优先（5 参数——docs:90 滚动更新案例）【docs §可用区域上下文】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §可用区域上下文（docs:61-101）+ multiactive-commons 源码 + my-xhs ZoneContext 实证
- **需求**：**区域状态的运行时上下文**——docs 明确：ZoneContext 是**可变对象**（对比 ZoneLocator 只读定位），能够切换：激活/区域/**同区域优先**（docs:62-71）
- **自主实现**：若我设计——上下文 = 运行时状态容器：①是否激活（enabled）②当前区域（zone）③同区域优先（**含 5 子参数**：preferenceEnabled/preferenceFilterOrder/preferenceUpstreamZoneReadyPercentage/preferenceUpstreamSameZoneMinAvailable/preferenceUpstreamDisabledZone）
- **参考实现**（docs 参数 + 双重源码实证 + 案例）：**状态组（docs:63-71 照录——2 主参数 + 同区域优先 5 子参数）**——①**enabled**（是否激活）②**zone**（当前区域）③**同区域优先（5 子参数）**：**preferenceEnabled**（激活）/ **preferenceFilterOrder**（多 Filter 优先次序）/ **preferenceUpstreamZoneReadyPercentage**（上游准备率——**低于阈值则区域优先失效**）/ **preferenceUpstreamSameZoneMinAvailable**（单区域最小可用实例数）/ **preferenceUpstreamDisabledZone**（主动失效区域）；**滚动更新案例（docs:90 照录——机制核心）**——Client 2 实例（Zone-A/B）+ Server 4 实例（Zone-A 2 个 + Zone-B 2 个）；Zone-A Server 滚动更新（Server-1 不可用、Server-2 老版本）→ **若 Client-1 流量全打 Server-2 可能导致其不可用** → 设置上游准备率 70%：当前 Zone-A 上游准备率 50% < 70% → **同区域优先被迫失效** → Client-1 请求 Zone-B 的 Server-3/4——**阈值需压测评估，API 提供选项**；**源码实证**——`ZoneContext` **`[本地实证：multiactive-commons `io.microsphere.multiple.active.zone.ZoneContext` + my-xhs `common/zone/ZoneContext.java`——DEFAULT_* 常量与 docs 5 参数完全对应]`**；**ZoneContextChangedEvent/Listener（docs:102-107）**——可变状态与动态配置交互（ZoneContextChangedListener——docs:105）
- **对比取舍**：**准备率阈值（动态失效保护）vs 固定区域优先**——滚动更新/故障时的"保护性失效"vs 简单固定——**上游准备率 + 最小可用数是"区域优先的自我保护机制"**（防单实例被打垮）
- **机制/说明**：同区域优先的本质 = **"就近优先 + 保护性失效"**——正常时流量留本区域（低延迟），上游准备不足/故障时**自动降级为跨区域**（可用性优先）——**与 02 篇"区域隔离"互补：隔离是服务器侧，同区域优先是客户端侧的路由策略**；阈值参数是"压测驱动"的经验值（docs:90 明示）
- **测试佐证**：docs:61-101（5 参数 + 案例照录）+ `ZoneContext.java`（multiactive-commons + my-xhs 双重实证）

### KP-04 ZonePreferenceFilter 过滤逻辑（filter 源码 + 8 条逻辑）【docs §同区域优先过滤器】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03
- **来源**：docs §同区域优先过滤器（docs:109-218——filter 源码块 + 8 条逻辑）+ multiactive-commons + my-xhs 实证
- **需求**：**同区域优先的过滤执行逻辑**——docs 明确：`ZonePreferenceFilter<E>`（泛型 E=Entry——内容/选项抽象）+ filter 方法（docs:113-206 源码块）+ 逻辑处理 8 条（docs:209-216）
- **自主实现**：若我设计——过滤链：数量兜底 → 特性开关 → 区域忽略 → 失效区域过滤 → 准备率检查 → 最小可用检查 → 同区域匹配返回
- **参考实现**（docs 源码块 + 8 条逻辑 + 双重实证）：**8 条逻辑（docs:209-216 照录）**——①entities ≤1 不执行 ②AZ 特性失效不执行 ③同区域特性失效不执行 ④当前区域为默认（本地环境）不执行 ⑤设了失效区域 → 过滤掉该区域选项 ⑥上游准备率低于阈值不执行 ⑦同区域最小实例数低于阈值不执行 ⑧否则执行同区域优先；**filter 源码（docs:113-206 照录核心）**——`isEnabled()`/`isPreferenceEnabled()`/`isIgnored(zone)`（默认区域忽略）/`filterDisabledZone`（失效区域过滤）/`isUpstreamZoneNotReady`（准备率检查——zoneCount/totalSize 对比阈值）/`isUnderSameZoneMinAvailableThreshold`（最小可用检查）/`matches(zone, resolvedZone)`（同区域匹配——`resolveZone(entity)` 解析每个实体区域）；**源码实证（双重）**——`ZonePreferenceFilter` **`[本地实证：multiactive-commons `io.microsphere.multiple.active.zone.ZonePreferenceFilter` + my-xhs `common/zone/ZonePreferenceFilter.java`——isEnabled/isPreferenceEnabled/isIgnored/getPreferenceUpstreamDisabledZone 逻辑对应 docs 8 条]`**；**案例（docs:218）**——E=Spring Cloud 服务发现实例对象，entities=Server 应用 4 实例
- **对比取舍**：**"保护性失效"链（8 条逐级降级）vs 简单区域过滤**——可用性优先的完备逻辑 vs 简单路由——**每条失效逻辑都是"宁跨区域不误杀"的工程决策**
- **机制/说明**：过滤逻辑 = **"正常情况下同区域优先，异常情况下逐级降级到跨区域"**——8 条的①-④是开关/兜底（数量≤1/AZ 特性/同区域特性/默认区域忽略），⑤-⑦是区域调整（⑤失效区域=**主动配置过滤**，⑥准备率/⑦最小可用=**保护性失效**），⑧执行同区域——**"保护性失效"（准备率/最小可用）让区域优先永不成为可用性瓶颈**
- **测试佐证**：docs:113-206（filter 源码）+ docs:209-216（8 条）+ `ZonePreferenceFilter.java`（multiactive-commons + my-xhs 双重实证 + my-xhs 测试 `ZonePreferenceFilterTest.java`）

### KP-05 通用注册发现多活适配（一套抽象适配 Eureka/Nacos/ZK/consul）【docs 主要内容②】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：05 篇（多注册中心）、KP-04
- **来源**：docs 主要内容②（docs:4——"基于 AZ Locator 抽象实现通用服务注册与发现多活架构，适配 Netflix Eureka、Alibaba Nacos、Apache Zookeeper、consul 等注册中心及客户端"）+ 05 篇衔接 + 发散
- **需求**：**一套区域抽象适配多种注册中心**——docs 明确：AZ Locator 抽象之上实现**通用注册发现多活**（Eureka/Nacos/ZK/consul 均适配）
- **自主实现**：若我设计——抽象分两层：①**区域层**（ZoneLocator/ZoneContext/ZonePreferenceFilter——产品无关）②**适配层**（各注册中心接入：区域偏好过滤器接 LoadBalancer 的 ServiceInstance 列表——E=ServiceInstance）
- **参考实现**（docs 声明 + 05 篇衔接 + my-xhs 实证）：**docs 设计（照录）**——AZ Locator 抽象适配 Eureka/Nacos/ZK/consul（docs:4——**Eureka 仅适配列表一行，非机制主体**）；**适配机制（发散 + my-xhs 实证）**——ZonePreferenceFilter 的 **E=ServiceInstance**（docs:218 案例——"E 是 Spring Cloud 服务发现实例对象"）→ **区域过滤作用于 LoadBalancer 的实例列表**（服务发现 → 区域过滤 → 负载均衡——**同区域优先路由链**）；**my-xhs 落地实证（应用层完整）**——`common/zone/loadbalancer/`：`ZonePreferenceServiceInstanceListSupplier`（**区域偏好实例列表供给器——类名指向 Spring Cloud LoadBalancer 的 ServiceInstanceListSupplier 扩展 `[验证程度：类名实证，继承关系待读类内容]`**）+ `ServiceInstanceZoneResolver`（实例区域解析）+ `ZoneLoadBalancerConfiguration`（LB 配置装配——类名实证）——**docs 抽象的 LoadBalancer 适配在 my-xhs 完整实现**；**Nacos 生态（发散 + stage-3 07 交叉）**——Nacos 客户端自身 zone/集群能力（stage-3 07）+ SCA 适配（Spring Cloud 抽象层一致——E=ServiceInstance 统一）
- **对比取舍**：**抽象层（区域能力产品无关）vs 各注册中心自实现**——一次抽象多次适配 vs 每产品重复——**docs 的 AZ Locator 就是"多活的通用抽象层"**（05/06 篇多注册机制的更上层）
- **机制/说明**：通用适配的本质 = **"区域偏好过滤"插在"服务发现 → 负载均衡"之间**（统一 E=ServiceInstance）——**注册中心只管实例列表，区域过滤是路由策略层**——与产品无关（Eureka/Nacos/ZK/consul 同一套）
- **测试佐证**：docs:4（声明照录）+ my-xhs `zone/loadbalancer/ZonePreferenceServiceInstanceListSupplier.java` 等（实证）+ 05 篇（多注册衔接）

### KP-06 现状核对（my-xhs：zone 包 = docs 抽象的应用落地——20 文件完整对照）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~05
- **来源**：my-xhs `common/zone/` 实证 + 架构师整合
- **需求**：以 AZ Locator 为尺——my-xhs 区域抽象的落地对照
- **自主实现**：若我设计——核对：ZoneContext（有）/ZonePreferenceFilter（有）/ZoneResolver（有）/LoadBalancer 适配（有）/自动装配（有）
- **参考实现**（my-xhs 实证——20 文件）：**ZoneContext ✅**（`common/zone/ZoneContext.java`——DEFAULT_* 常量与 docs 5 参数对应——KP-03 实证）；**ZonePreferenceFilter ✅**（`common/zone/ZonePreferenceFilter.java`——8 条逻辑对应——KP-04 实证）；**ZoneResolver ✅**（`common/zone/ZoneResolver.java`——`extends Function<E,String>`——区域解析接口）；**自动装配 ✅**（`ZoneContextAutoConfiguration.java` + `ZoneProperties.java` + `ZoneConstants.java`——配置化）；**LoadBalancer 适配 ✅**（`loadbalancer/ZonePreferenceServiceInstanceListSupplier` + `ServiceInstanceZoneResolver` + `ZoneLoadBalancerConfiguration`——KP-05 实证）；**扩展面 ✅**——`redis/`（`EventPublishingRedisCommandInterceptor` 等 6 文件——区域 Redis 命令拦截——stage-3 16 篇已提取）+ `datasource/DynamicDataSource.java`（区域数据源——docs:3"为缓存/数据库提供"的落地）；**测试 ✅**（`ZonePreferenceFilterTest`/`ZoneResolverTest`/`ZoneContextTest`）；**差异（修正——读 ZoneContext.java import 实证）**——docs 的 **ZoneLocator 定位器（当前应用区域）**在 my-xhs **无独立等价物**：当前区域来自配置（`ZoneContext.java` import `CURRENT_ZONE_PROPERTY_NAME` 实证）——`ZoneResolver` 是**实体区域解析**（`extends Function<E,String>`——KP-04 filter 的 resolveZone(entity) 用途，非当前应用定位）——**语义区分：ZoneLocator（定位自己）vs ZoneResolver（解析实体）**；**无 DefaultZoneLocator/AWS 谱系**（本地/单机环境——`[现状：区域来源为配置，云元数据定位未落地]`）`[决策待定：云平台部署时补 ZoneLocator 谱系]`
- **对比取舍**：**my-xhs zone 包（应用实现）vs docs/multiactive（框架实现）**——应用侧裁剪（无云定位器）vs 框架完整（AWS 谱系）——**区域抽象的"够用裁剪"**（教学/单机场景合理）
- **测试佐证**：my-xhs `common/zone/`（20 文件 find 实证 + 3 测试类）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| ZoneLocator 抽象（7 原则） | 分布式理论 | 核心 | P1 | 🔴 | 时间无关 | High |
| 内建实现与优先级（Default/AWS/Composite） | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| ZoneContext 与同区域优先（5 参数） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| ZonePreferenceFilter 过滤逻辑（8 条） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 通用注册发现适配（多注册中心） | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | Medium |
| 现状核对（my-xhs zone 包落地） | 分布式问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）——本篇源码验证最强**：`microsphere-multiactive`（ZoneLocator/AbstractZoneLocator/CompositeZoneLocator/DefaultZoneLocator + multiactive-aws 三实现 + ZoneContext/ZonePreferenceFilter——**docs 设计全部源码命中**）；my-xhs `common/zone/`（20 文件——应用实现同构）
- **关键实证**（本地 grep）：`ZoneLocator.java`（multiactive-spring `io.microsphere.multiple.active.zone.spring`）；`Ec2AvailabilityZoneEndpointZoneLocator` 等三实现（multiactive-aws）；`ZoneContext`/`ZonePreferenceFilter`（multiactive-commons + my-xhs 双重）
- **诚实标注**：docs 为 **microsphere 框架设计文档**（非 Eureka 文档）——Eureka 仅 docs:4 适配列表一行（KP-05）；docs 的 filter 源码块（docs:113-206）与 8 条逻辑（docs:209-216）照录；**三层对应标注**——docs 设计 → multiactive 源码（实证）→ my-xhs 应用（实证）；my-xhs 与 docs 的差异（无 ZoneLocator 云实现谱系——ZoneResolver 承担解析）显式标注
- **关联标注**：05 篇（多注册机制——通用化衔接）；02 篇（Region/AZ）；stage-3 01（my-xhs zone 基线——本篇深化）；stage-3 16（Redis 命令事件——zone redis 面已提取）

---

## 五、本节小结（三层次视角）

**需求**：多活架构框架的核心抽象——AZ Locator（区域定位/上下文/过滤）+ 通用注册发现适配。

**自主实现核心**：①**区域感知三层**（ZoneLocator 定位 → ZoneContext 状态 → ZonePreferenceFilter 路由）②**7 设计原则** = SPI 抽象的最佳实践模板 ③**同区域优先 = 就近 + 保护性失效**（8 条逻辑逐级降级——正常就近、异常跨区）。

**参考实现**：docs 设计照录 + **microsphere-multiactive 全部源码实证**（唯一全命中篇）+ my-xhs zone 包应用实证（ZoneContext/ZonePreferenceFilter/ZonePreferenceServiceInstanceListSupplier 同构）——**docs → multiactive → my-xhs 三层对应**。

**对比取舍**：知识本体是"**区域感知的通用抽象层**"——定位（多实现+优先级）vs 状态（运行时可变）vs 路由（保护性失效）；一套抽象适配多注册中心（E=ServiceInstance 统一）；my-xhs **zone 包完整落地**（无云定位器谱系——单机场景裁剪合理）。

**待验证汇总**：
- my-xhs 云平台 ZoneLocator 谱系（`[决策待定]`——云部署时补 Default/AWS 实现）
- my-xhs ZonePreferenceFilter 是否全 8 条逻辑实现（测试 `ZonePreferenceFilterTest` 覆盖度 `[待验证：测试内容]`）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| ZoneLocator 定位器 | ⚠️ 由 `ZoneResolver`（Function 接口）承担——无 Default/AWS 谱系 | 现状说明：单机场景；云部署时补（决策待定） |
| ZoneContext（5 参数） | ✅ `ZoneContext.java`（DEFAULT_* 常量对应） | 无 |
| ZonePreferenceFilter（8 条） | ✅ `ZonePreferenceFilter.java` + 测试 | 无（测试覆盖度待核） |
| LoadBalancer 适配 | ✅ `zone/loadbalancer/` 3 类（ServiceInstanceListSupplier） | 无 |
| 扩展面（Redis/数据源） | ✅ `zone/redis/` + `zone/datasource/` | 无（16 篇已提取） |

### 差距清单

1. **P3**：云平台 ZoneLocator 谱系（触发条件：云部署——AWS/Aliyun 元数据定位器）
2. **P3**：ZonePreferenceFilter 测试覆盖度核对（`[待验证]`）

**结论**：07 篇——my-xhs **zone 包 = docs AZ Locator 抽象的应用落地**（ZoneContext/ZonePreferenceFilter/LB 适配全部同构实证）；无 P1/P2 差距（唯一裁剪：云定位器谱系——单机场景合理）。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 microsphere 框架设计文档（218 行）——7 原则/AWS 表/5 参数/8 条逻辑全部 docs 原文照录；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：AZ Locator 的完整认知该讲什么

docs 是框架设计文档。完整还该包含：

1. **"AZ Locator 是多活架构的'区域神经系统'"**（docs + 发散）：区域抽象三件套——**定位**（我在哪个区——ZoneLocator/Resolver）、**状态**（区域配置与开关——ZoneContext）、**路由**（流量怎么按区域走——ZonePreferenceFilter）——**多活的第一性问题"流量怎么落区域"由此回答**；docs 02（区域隔离）是服务器侧、本篇是客户端侧路由——**两侧合成完整区域架构**
2. **"7 原则是 SPI 抽象的可复用模板"**（docs:14-43 + 发散）：范围/优先级/兼容/低依赖/组合/不可信扩展/异构交集——**任何"环境感知抽象"（云/容器/配置源）都可复用这套原则**——优先级裁决（Ordered 步进 5-10）+ 组合 Primary + 不可信扩展兜底——**框架设计的通用智慧**
3. **"同区域优先 = 就近路由 + 保护性失效"**（docs:90 + 发散）：**准备率/最小可用/失效区域**三个保护参数让"区域优先"在**滚动更新/故障**时自动降级——**防止"区域内只剩一个实例还被流量打死"的雪崩**——这是多活路由的**反脆弱设计**（阈值压测驱动——docs 明示）；**my-xhs ZonePreferenceFilter 实现了这套逻辑**（应用层实证）
4. **"通用抽象的价值 = 一次设计、多产品适配"**（docs:4 + 发散）：AZ Locator 之上适配 Eureka/Nacos/ZK/consul——**E=ServiceInstance 的统一**让区域过滤与注册中心解耦——**05/06 篇的多注册机制 + 本篇的区域抽象 = 注册发现多活的完整通用层**；**Nacos 生态（stage-3 07）**——SCA 在 Spring Cloud 抽象层内（同一 ServiceInstance 流）
5. **"框架文档 vs 应用实现的裁剪关系"**（docs + my-xhs）：my-xhs zone 包是**应用的"够用裁剪"**（无云定位器——单机环境）；**microsphere-multiactive 是完整框架**（AWS 谱系/云元数据）——**学框架全貌、用应用裁剪**（08 SOP）
6. **"区域抽象与微服务路由的未来"**（发散）：区域/可用区概念是**云原生路由的基础语义**（K8s topologySpread/拓扑域、Istio 区域路由——stage-3 21/22 衔接）——**AZ Locator 是这套语义的 Spring 生态实现**

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 多实现 + 优先级（Ordered） vs 单一实现 | 环境完备 vs 简单（步进 5-10 裁决） |
| 配置兜底（Default 20） vs 云元数据自动（5/10/15） | 显式可控 vs 自动发现（特化优先） |
| 同区域优先 + 保护性失效 vs 固定就近 | 防雪崩 vs 简单（8 条逐级降级） |
| 准备率/最小可用阈值 | 保护 vs 误降级（压测驱动经验值） |
| 抽象层适配多注册 vs 各产品自实现 | 一次设计 vs 重复建设 |

### 常见坑/反模式

1. **区域优先无保护**：区域内上游只剩 1 实例仍全部引流——滚动更新时把老实例打死（docs:90 案例即此坑）
2. **阈值拍脑袋**：准备率/最小可用需压测评估（docs 明示"实际阈值需要压测来评估"）——拍脑袋阈值导致误降级或失效
3. **抽象层不设优先级**：多实现无 Ordered 裁决——环境不确定时选错实现
4. **扩展实现当可信**：第三方 ZoneLocator 实现不确定（扩展不可信任原则）——核心代码只信任抽象接口
5. **区域状态硬编码**：ZoneContext 是可变对象 + 动态配置交互（ZoneContextChangedListener）——静态化丢失运行时调整能力
6. **只做区域过滤不做降级**：8 条逻辑中保护性失效（准备率/最小可用/失效区域）缺任一——区域优先成为可用性瓶颈

### 生态位置

- **stage-4 教学主线**：**通用化组（07-09 第一篇）**——05/06 多注册机制 → **07 AZ Locator 通用抽象（本篇）** → 08 加餐（通用设计深化）→ 09 Cloud-Native 注册发现 → 10-12 负载均衡 → 16-19 数据面多活
- **前后篇衔接**：05 篇（多注册——本篇"通用适配"的上层）；02 篇（区域隔离——服务器侧）；08 篇（加餐——docs 顺序）；stage-3 01（my-xhs zone 基线）；stage-3 16（zone redis 事件）；stage-3 21/22（K8s 拓扑/Istio 区域——云原生衔接）
- **与源码提取的关系**：**本篇是 microsphere 生态源码提取的先导**（microsphere-multiactive 全部类名实证——source/ 提取时应以 multiactive 仓库为主线）；my-xhs zone 包为应用实证

**架构师视角结论**：本篇为 **microsphere 框架设计文档（218 行）**——AZ Locator 抽象三件套（ZoneLocator 定位/ZoneContext 状态/ZonePreferenceFilter 路由）+ 7 设计原则 + 同区域优先保护性失效 + 通用注册发现适配——**知识本体 = "区域感知的通用抽象层"**（产品无关——Eureka 仅 docs 适配列表一行）；**源码实证最强篇**（microsphere-multiactive 全部命中 + my-xhs zone 包同构落地）；my-xhs **zone 包 = 抽象的应用实现**（无云定位器谱系——单机裁剪合理，云部署时补——决策待定）。
