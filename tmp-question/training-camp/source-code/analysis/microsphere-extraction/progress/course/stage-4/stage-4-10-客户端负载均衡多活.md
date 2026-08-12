# stage-4 · 第 10 节：第七节：Spring Cloud Netflix Ribbon 负载均衡多活架构设计与实现 — 知识点提取

> 课程：stage-4 多活架构 第 10 节（负载均衡组 10-12 第一篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/10. 第七节：Spring Cloud Netflix Ribbon 负载均衡多活架构设计与实现.md`
> 提取时间：2026-08-12 | 权重：核心（客户端负载均衡组件模型 + 区域优先——多活路由面）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**Ribbon 已过时（docs 自证"维护状态" :28 + "3.x 之后被移除" :311）——机制用现代对应（LoadBalancer/Nacos）讲，Ribbon 仅 docs 场景（08 SOP §1.3.1 主流性）**

> **文档形态**：Ribbon 设计文档 + 源码块（425 行）——主要内容：①Ribbon 与 Eureka AZ 官方同区域优先/Zone 多活 ②AZ Locator 与 Ribbon 整合；**机制本体 = 客户端负载均衡的组件模型与区域优先**（Ribbon 是历史载体——docs 自证过时）；**与 04 篇交叉**（NamedContextFactory——本篇深化 SpringClientFactory）。

---

## 一、本节概览

- **技术域**：客户端负载均衡（组件模型/ServerList/规则/区域优先）、区域多活路由（ZonePreference/ServerListFilter）、AZ Locator 整合
- **维度**：`[分布式问题]`（负载均衡/区域路由）+ `[工程问题]`（组件模型/配置）+ `[性能优化]`（LB 算法）
- **核心命题**：**客户端负载均衡的区域多活路由**——docs 两条主线：①Ribbon 组件模型（规则/列表/过滤/更新——10 组件 + 8 默认实现）②区域优先与 AZ Locator 整合（Zone 获取顺序 + metadata 键名坑）；**知识本体 = 客户端 LB 的组件化设计与区域感知**（现代 = LoadBalancer + ZonePreferenceServiceInstanceListSupplier）
- **知识点数**：6 个
- **前置**：04 篇（NamedContextFactory）、07 篇（AZ Locator）、stage-1 11/12（监控指标/动态权重 LB）

## 前置条件清单
读者需先掌握：
1. **NamedContextFactory 子上下文**（04 篇 KP-02——Spring Cloud 客户端底座）
2. **AZ Locator 抽象**（07 篇——区域感知层）
3. **负载均衡基础**（stage-1 11/12——监控指标/动态权重）
4. **服务发现**（stage-3 07——ServerList 的数据源）
未达前置者，先补：04 篇 / 07 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **机制为本**：组件模型/区域优先是通用机制——现代对应 LoadBalancer（11 篇）/my-xhs 实证
- **Ribbon 仅场景**：docs 以 Ribbon 讲（docs 自证过时）——机制照提、参考回退现代
- **区域优先衔接**：本篇 ZonePreference 与 07 篇 AZ Locator 同域（docs 本篇即整合篇）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 客户端负载均衡组件模型（10 组件 + 8 默认实现——Ribbon 场景）【docs §核心 API】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[过时→Spring Cloud LoadBalancer（Ribbon 维护状态 docs:28/3.x 移除 docs:311）]` | **置信度**：High
- **前置**：stage-1 11（LB 基础）
- **来源**：docs §Netflix Ribbon/§核心 API（docs:12-83）；**`[跳过：docs:10 §Netflix Eureka 空节标题——无内容（Eureka 机制 02-06 篇已提取）]`**；**`[跳过：docs:46-47 §@FeignClient 空节标题——Feign 机制 04/06 篇已提]`**；**`[跳过：docs:86-87 §Ribbon 配置 IClientConfig 空节标题——无内容（IClientConfig 已在 10 组件表覆盖）]`**
- **需求**：**客户端负载均衡的组件化设计**——docs 明确：Ribbon 定位（客户端负载均衡器 + 中间层服务客户端——docs:13）+ 6 功能（可插拔规则/服务发现集成/故障恢复/云支持/LB 集成客户端/Archaius 配置驱动——docs:15-20）+ 3 子项目（ribbon-core/ribbon-eureka/ribbon-httpclient——docs:24-26）
- **自主实现**：若我设计——客户端 LB 五组件：**规则（选实例算法）+ 列表（实例来源）+ 过滤（列表裁剪）+ 更新（列表刷新）+ 负载均衡器（编排）**
- **参考实现**（docs 表照录 + 现代对照）：**10 组件（docs:62-71 照录）**——IClientConfig/IRule/IPing/ServerList/ServerListUpdater/ILoadBalancer/ServerListFilter/RibbonLoadBalancerContext/RetryHandler/ServerIntrospector；**8 默认实现表（docs:75-83 照录）**——DefaultClientConfigImpl（IClientConfig）/ZoneAvoidanceRule（IRule）/DummyPing（IPing——**默认不探测**）/ConfigurationBasedServerList（ServerList）/ZonePreferenceServerListFilter（ServerListFilter——**区域偏好**）/ZoneAwareLoadBalancer（ILoadBalancer——**区域感知**）/PollingServerListUpdater（ServerListUpdater）；**机制（发散）**——**"规则 + 列表 + 过滤 + 更新"四件套**是客户端 LB 的通用骨架（现代 LoadBalancer：ReactorServiceInstanceLoadBalancer + ServiceInstanceListSupplier 同构）；**版本线（docs:35-36 照录）**——最高支持：SNAPSHOT 2.2.11 / RELEASE 2.2.10.RELEASE（2020 前后最后版本线）；**配置属性 5 个（docs:190-194 照录）**——`<clientName>.ribbon.NFLoadBalancerClassName/NFLoadBalancerRuleClassName/NFLoadBalancerPingClassName/NIWSServerListClassName/NIWSServerListFilterClassName`（**组件类名的属性化配置**）；**过时处理（docs 自证）**——Ribbon"当前项目状态：维护——成熟稳定不主动开发新功能"（docs:28-29）+ "spring-cloud-netflix-eureka-client 3.x 之后被移除"（docs:311）`[过时→Spring Cloud LoadBalancer——11 篇展开]`
- **对比取舍**：**组件化（可插拔——规则/列表/过滤独立）vs 单一实现**——扩展性 vs 简单——**客户端 LB 的标准骨架**（Ribbon 定义了骨架，现代 LoadBalancer 继承此思想）
- **机制/说明**：客户端 LB 四件套语义——**IRule 决定"选哪个"（算法）、ServerList 提供"从哪选"（数据源）、ServerListFilter 裁剪"哪些可被选"（区域/子集）、ServerListUpdater 刷新"列表新鲜度"**——**区域多活路由的挂载点 = ServerListFilter**（KP-04/05）
- **测试佐证**：docs:13-26/62-83（照录）+ stage-1 11（LB 基础交叉）

### KP-02 ServerList 体系与更新（配置/发现两实现 + 轮询更新）【docs §Ribbon 服务器/§ServerList】
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]`（机制）| **置信度**：High
- **前置**：KP-01
- **来源**：docs §Ribbon 服务器（docs:88-97）+ §ServerList（docs:99-173——源码块）
- **需求**：**Server 与 ServerList 的实例模型**——docs 明确：**Server 结构类似 Eureka InstanceInfo 或 Spring Cloud Commons ServiceInstance**（docs:89——**转换关系**）；6 属性（id/host/port/scheme/**zone 默认 UNKNOWN**——docs:93-97）；ServerList 两实现（**ConfigurationBasedServerList**——基于 IClientConfig 配置/docs:101-102；**DiscoveryEnabledNIWSServerList**——基于 EurekaClient 整合/docs:104）
- **自主实现**：若我设计——ServerList = 实例源抽象（配置/发现两实现——插件切换）；Server = 实例模型（含 zone——区域路由的基础数据）
- **参考实现**（docs 源码块照录 + 现代对照）：**Server 6 属性（docs:93-97 照录）**——id/host/port/scheme/**zone（默认 "UNKNOWN"）**——**与 ServiceInstance 同构**（docs:89 明示——Spring Cloud 抽象层统一）；**ServerList 两实现（docs:101-125）**——ConfigurationBasedServerList（配置）/DiscoveryEnabledNIWSServerList（Eureka——`EurekaRibbonClientConfiguration.ribbonServerList` 源码块 docs:106-124：`DiscoveryEnabledNIWSServerList` + `DomainExtractingServerList` 包装——**区域抽取包装**）；**ServerListUpdater 轮询（docs:149-171）**——`PollingServerListUpdater` + `UpdateAction.updateListOfServers`（docs:156-171 源码块——**getUpdatedListOfServers → filter 过滤 → updateAllServerList**——**"取列表→过滤→更新"三步链**）；**现代对照（发散）**——ServiceInstanceListSupplier（LoadBalancer——`ZonePreferenceServiceInstanceListSupplier` my-xhs 实证——07 篇）——**"列表供给 + 过滤"同构**（Ribbon ServerList+Filter → LoadBalancer Supplier 链）
- **对比取舍**：**ServerList 两实现（配置静态/发现动态）**——静态可控 vs 动态实时——**现代 LoadBalancer 用 Supplier 链（可组合）替代两分法**
- **机制/说明**：Server.zone 是**区域路由的数据基础**（默认 UNKNOWN——无区域标记的实例）；"取列表→过滤→更新"三步链 = **列表新鲜度维护**（轮询拉取 + 过滤裁剪——区域/子集）
- **测试佐证**：docs:88-173（照录）+ 07 篇（ZonePreferenceServiceInstanceListSupplier 交叉）

### KP-03 NamedContextFactory 子上下文与 SpringClientFactory（客户端隔离）【docs §SpringClientFactory/§Spring Cloud Commons】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]`（机制通用——Ribbon 场景过时）| **置信度**：High
- **前置**：04 篇 KP-02（NamedContextFactory）
- **来源**：docs §SpringClientFactory（docs:197-301——createContext 源码块）+ §Spring Cloud Commons（docs:217-305）
- **需求**：**每个客户端独立的子上下文**——docs 明确：SpringClientFactory 继承 NamedContextFactory——**每个 @RibbonClient 创建独立子应用上下文**（"ribbon.client.name" 属性注入——docs:198）；**上下文层次**（docs:200-214——main 父 + ribbon-client-1/2 子——各子上下文独立 IRule/IClientConfig）
- **自主实现**：若我设计——客户端配置隔离 = 每客户端子上下文（配置类独立注册——各自 IRule/超时）
- **参考实现**（docs 源码块照录 + 04 篇交叉）：**SpringClientFactory（docs:227-235 源码块照录）**——继承 `NamedContextFactory<RibbonClientSpecification>`；构造器 3 参数：`RibbonClientConfiguration`（默认配置类）/`"ribbon"`（PropertySource 名）/`"ribbon.client.name"`（客户端名属性——docs:232）；**createContext（docs:247-289 源码块照录——核心）**——①`new AnnotationConfigApplicationContext`（JDK11 issue 注释——docs:250-252：BeanFactory 手动构建）②注册配置类（`PropertyPlaceholderAutoConfiguration` + `defaultConfigType`——docs:279）③**MapPropertySource 第一优先级**（"ribbon.client.name"→name——docs:280-281——**setParent 不被合并**）④`context.setParent(parent)`（docs:284——**父上下文合并**）⑤refresh；**注入组件 3 个（docs:55-57 照录）**——@RibbonClientName（注入当前 RibbonClient 名称——单独构建 PropertySource）/PropertiesFactory（配置与 Ribbon 组件映射创建工厂）/Environment（子应用上下文——**大多属性来源于父**——AbstractApplicationContext#setParent）；**子上下文特性（docs:222 照录）**——**子上下文合并父配置但通常仍读父 PropertySources**（不单独配置——docs:222）；**@RibbonClients 两次导入覆盖（docs:138-147 照录）**——RibbonAutoConfiguration 第一次导入 → RibbonEurekaAutoConfiguration 第二次覆盖（**模块驱动可重复导入——后者覆盖前者**——docs:147）；**04 篇交叉**——NamedContextFactory 机制已提取（04 篇 KP-02——本篇深化 SpringClientFactory 实现）
- **对比取舍**：**子上下文隔离（每客户端独立 Bean）vs 全局共享**——配置独立 vs 简单——**客户端级配置隔离是 Spring Cloud 的标准模式**（Feign/LoadBalancer 同构——04 篇）
- **机制/说明**：子上下文 = **"每客户端一个迷你容器"**——MapPropertySource 第一优先级（客户端名属性）+ 父上下文合并（配置共享）——**隔离与共享的平衡**；@RibbonClients 重复导入覆盖 = **Spring 模块驱动的"后到覆盖"语义**
- **测试佐证**：docs:197-305（源码块照录）+ 04 篇（NamedContextFactory 交叉）

### KP-04 区域优先机制（ZonePreferenceServerListFilter + Zone 获取 3 顺序）【docs §区域相关】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07 篇（AZ Locator）、KP-01
- **来源**：docs §区域意识/§区域偏好（docs:179-185 + 317-394——源码块）
- **需求**：**负载均衡的区域偏好**——docs 明确：ServerListFilter 家族（**ZoneAffinityServerListFilter**（区域意识）/ServerListSubsetFilter（子集）/DefaultNIWSServerListFilter/ZonePreferenceServerListFilter（区域偏好）——docs:179-185）+ **Zone 获取 3 顺序**（docs:390-394）
- **自主实现**：若我设计——区域路由 = ①实例带区域信息（Server.zone——KP-02）②过滤时按当前区域偏好裁剪 ③当前区域不足时回退
- **参考实现**（docs 源码块照录 + 07 篇衔接）：**Zone 获取 3 顺序（docs:390-394 照录）**——①**metadata "zone"**（`DomainExtractingServer` 构造器——docs:376-377：`metadata.containsKey("zone")` → setZone）②**hostname 近似**（`ZoneUtils.extractApproximateZone(host)`——approximateZoneFromHostname——docs:379-380）③**EurekaClient 配置**（`getAvailabilityZones(region)`——docs:383/343 + `InstanceInfo.getZone`——docs:352-366：availZones[0] 默认 + **AWS AmazonInfo 覆盖**——docs:356-363）；**DiscoveryEnabledServer.createServer（docs:337-348 源码块照录）**——`clientConfig.getAvailabilityZones(region)` + `InstanceInfo.getZone(availZones, instanceInfo)` → `server.setZone`；**过滤器家族（docs:179-185 照录）**——ZoneAffinity（区域意识——优先同区）/ServerListSubset（子集——限制规模）/ZonePreference（区域偏好——08 篇 ZonePreferenceFilter 的 LB 挂载）；**07 篇衔接**——docs 的 ZonePreferenceServerListFilter 与 08 篇 ZonePreferenceFilter（AZ Locator）同域——**本篇是"区域偏好的负载均衡实现"、08 篇是"通用抽象"**
- **对比取舍**：**Zone 获取 3 顺序（metadata > hostname 近似 > 注册中心配置）**——精确优先 vs 兜底——**metadata 是首选（显式精确），hostname 近似是便捷兜底，注册中心配置最后**
- **机制/说明**：区域路由链 = **Server.zone（数据）→ ZonePreference 过滤（策略）→ ZoneAwareLoadBalancer（编排）**——"同区域优先、不足回退"的负载均衡实现；**AWS 特化**（AmazonInfo availabilityZone——docs:356-363）——云平台区域自动感知
- **测试佐证**：docs:179-185/317-394（源码块照录）+ 07/08 篇（AZ Locator 交叉）

### KP-05 AZ Locator 与负载均衡整合（ServerListFilter 挂载 + metadata 键名坑）【docs §整合 Availability Zones Locator】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07/08 篇（AZ Locator 三件套）、KP-04
- **来源**：docs §整合 Availability Zones Locator（docs:399-402）+ §DomainExtractingServer（docs:370-396——**键名坑 docs:396**）
- **需求**：**AZ Locator 抽象挂载到负载均衡**——docs 明确：**利用 ZonePreferenceFilter 实现 ServerListFilter**（docs:401-402——08 篇的 ZonePreferenceFilter 作 LB 过滤）
- **自主实现**：若我设计——整合 = ①ZonePreferenceFilter 适配为 ServerListFilter（E=Server）②区域信息链路打通（定位→上下文→过滤）
- **参考实现**（docs 照录 + 坑 + my-xhs 实证）：**整合（docs:401-402 照录）**——AZ Locator 内建 ZonePreferenceFilter → 实现 ServerListFilter（**区域过滤统一入口**）；**metadata 键名坑（docs:396 照录——核心）**——**ZoneAttachmentHandler 写 "microsphere.availability.zone" 元信息，但 Ribbon 读 "zone"**——键名不一致 → **若实例存在 "zone" 且与 "microsphere.availability.zone" 不等 → 整合失败**（docs:396——**键名契约不一致是整合失败源**）；**my-xhs 实证（现代整合）**——`ZonePreferenceServiceInstanceListSupplier`（common/zone/loadbalancer/——07 篇 KP-05：**LoadBalancer 的 Supplier 挂载点**）+ `ServiceInstanceZoneResolver`（区域解析）——**AZ Locator 与 LoadBalancer 的现代整合**（Ribbon 场景的 LoadBalancer 版）
- **对比取舍**：**ServerListFilter（Ribbon）vs ServiceInstanceListSupplier（LoadBalancer）**——挂载点不同、机制同构（过滤链）——**现代整合 = Supplier 链（可组合：zone 解析 → 偏好过滤 → 兜底）**
- **机制/说明**：**整合的契约 = 区域元信息键名统一**——"写方"（attachZone）与"读方"（DomainExtractingServer/ZoneResolver）必须用**同一键**（"zone" vs "microsphere.availability.zone"——docs:396 的教训：**跨组件契约不一致 → 静默失败**）；现代 my-xhs 用 ZoneResolver 统一解析（键名契约收敛）
- **测试佐证**：docs:396/401-402（照录）+ my-xhs `ZonePreferenceServiceInstanceListSupplier`/`ServiceInstanceZoneResolver`（实证）

### KP-06 现状核对（my-xhs：自定义 LoadBalancer + ZonePreference Supplier——现代对应）【docs 相关议题发散】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~05
- **来源**：my-xhs 实证 + 架构师整合
- **需求**：以组件模型为尺——my-xhs 客户端 LB 的现代对应
- **自主实现**：若我设计——核对：LB 实现（有——自定义）/区域偏好（有——Zone Supplier）/规则（LeastConnections）
- **参考实现**（my-xhs 实证 + 交叉）：**负载均衡 ✅ 现代栈**——`common/loadbalancer/LeastConnectionsLoadBalancer.java`（自定义——最小连接数，扩展 Spring Cloud LoadBalancer——stage-3 01/06 实证）——**docs 的 ILoadBalancer/IRule 在现代 = ReactorServiceInstanceLoadBalancer 自定义**（11 篇展开）；**区域偏好 ✅**——`zone/loadbalancer/ZonePreferenceServiceInstanceListSupplier` + `ZoneLoadBalancerConfiguration`（07 篇 KP-05 实证）——**docs 的 ZonePreferenceServerListFilter 在现代 = ZonePreference ServiceInstanceListSupplier**；**Ribbon ❌ 未用**（`[现状：无 Ribbon——Boot 3/SCA 2023 栈——LoadBalancer 内建]`——docs 的"Ribbon 维护状态/3.x 移除"在 my-xhs 已是既成事实）；**相关议题（docs:404-425——开发/运维/安全泛议）`[跳过：泛泛议题无知识增量]`**
- **对比取舍**：**Ribbon 组件模型（历史）vs LoadBalancer Supplier 链（现代）**——骨架继承、实现演进——**docs 的组件模型是"机制教材"，my-xhs 是现代落地**
- **测试佐证**：my-xhs `LeastConnectionsLoadBalancer.java`/`ZonePreferenceServiceInstanceListSupplier`（实证——stage-3 01/07 交叉）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 客户端 LB 组件模型（10+8） | 分布式问题 | 核心 | P1 | 🟡 | 过时→LoadBalancer | High |
| ServerList 体系与更新 | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| NamedContextFactory 子上下文 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 区域优先机制（Zone 3 顺序） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| AZ Locator 与 LB 整合（键名坑） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 现状核对（现代对应） | 工程问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（LeastConnectionsLoadBalancer/ZonePreferenceServiceInstanceListSupplier——现代对应实证）；spring-cloud-commons（NamedContextFactory——04 篇已实证）；`[无本地源码：Ribbon 未在本地 code/spring]`——Ribbon 源码块按 docs 照录
- **关键实证**（交叉引用）：07 篇（AZ Locator——整合衔接）；04 篇（NamedContextFactory——本篇深化）；stage-3 01/06（LeastConnectionsLoadBalancer 实证）
- **诚实标注**：docs 为 **Ribbon 设计文档（425 行）**——**Ribbon 已过时（docs 自证：:28 维护状态 + :311 3.x 移除）**——机制照提、参考回退现代（LoadBalancer/my-xhs）；Ribbon 类名（SpringClientFactory/DomainExtractingServer 等）`[无本地源码：Ribbon——docs 源码块照录]`；**docs:396 键名坑照录**（metadata "zone" vs "microsphere.availability.zone"）；**相关议题（docs:404-425）`[跳过：泛泛议题]`**；Eureka 仅整合场景（docs 10/24-26 子项目等——非主体）
- **关联标注**：04 篇（NamedContextFactory）；07/08 篇（AZ Locator——区域面）；11 篇（LoadBalancer——docs 顺序展开）；stage-1 11/12（LB 基础）；stage-3 01/06（my-xhs LB 实证）

---

## 五、本节小结（三层次视角）

**需求**：客户端负载均衡的区域多活路由——组件模型 + 区域优先 + AZ Locator 整合。

**自主实现核心**：①**客户端 LB 四件套**（规则/列表/过滤/更新——可插拔骨架）②**Zone 获取 3 顺序**（metadata > hostname > 注册中心——精确优先）③**整合契约 = 区域元信息键名统一**（写读方同键——docs:396 坑）。

**参考实现**：docs 组件表/源码块照录（ServerList/SpringClientFactory/DomainExtractingServer）+ **现代对照**（LoadBalancer Supplier 链——my-xhs ZonePreferenceServiceInstanceListSupplier/LeastConnectionsLoadBalancer 实证）+ **Ribbon 过时标注**（docs 自证——维护状态/3.x 移除）。

**对比取舍**：知识本体是"**客户端 LB 的组件化设计与区域感知**"——四件套骨架（机制时间无关）、ZonePreference 挂载点（ServerListFilter → Supplier）、键名契约（整合成败关键）；**Ribbon 是历史载体（docs 自证过时）——现代 = LoadBalancer**（11 篇展开）。

**待验证汇总**：
- Ribbon 源码细节（`[无本地源码]`——docs 源码块照录）
- 11 篇 LoadBalancer 现代实现展开（docs 顺序）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 负载均衡组件模型 | ✅ LoadBalancer 栈（`LeastConnectionsLoadBalancer`——自定义 LB） | 无（Ribbon 已过时——现代栈内建） |
| 区域偏好（ZonePreference） | ✅ `ZonePreferenceServiceInstanceListSupplier` + `ZoneLoadBalancerConfiguration`（07 篇实证） | 无（区域路由已落地） |
| ServerList 更新（轮询） | ✅ LoadBalancer 内建（Supplier 缓存/刷新） | 无 |
| AZ Locator 整合 | ✅ ZoneResolver + Supplier（键名收敛） | 无（docs:396 坑已规避） |

### 差距清单

1. **P3**：LB 规则面核对（LeastConnections 单一规则 vs 多规则可插拔——触发条件：多场景 LB 诉求）

**结论**：10 篇——my-xhs **LoadBalancer 现代栈完整对应 docs 组件模型**（LeastConnections + ZonePreference Supplier——区域路由已落地）；**Ribbon 未用（既成事实——docs 自证过时）**；无 P1/P2 差距。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Ribbon 设计文档（425 行）——组件表/源码块照录；Ribbon 过时（docs 自证）；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：客户端负载均衡多活的完整认知该讲什么

docs 是 Ribbon 设计文档。完整还该包含：

1. **"客户端 LB 的组件骨架是时间无关的"**（docs 表 + 发散）：**规则（选谁）+ 列表（从哪选）+ 过滤（哪些可被选）+ 更新（新鲜度）**——Ribbon（IRule/ServerList/ServerListFilter/Updater）→ LoadBalancer（ReactorLoadBalancer/Supplier 链）——**骨架继承、实现演进**——学组件模型不学 Ribbon（08 SOP）
2. **"区域路由的挂载点 = 列表过滤"**（docs + 发散）：区域优先不是在规则（IRule）里做，而是**过滤层（ServerListFilter）**——先按区域裁剪再选实例——**"过滤先行、规则后选"是区域路由的标准架构**（现代 Supplier 链同构）；**区域不足回退**（ZoneAwareLoadBalancer/ZonePreference 的触发条件——07/08 篇保护性失效衔接）
3. **"Zone 信息的 3 级来源与精确性"**（docs:390-394 + 发散）：metadata（显式精确）> hostname 近似（便捷但不可靠）> 注册中心配置（兜底）——**区域信息的质量决定区域路由的正确性**——**生产必须 metadata 精确标记**（08 篇 attachZone 的写面 + 本篇读面闭环）
4. **"整合失败的根源 = 键名契约不一致"**（docs:396 + 发散）：attachZone 写 "microsphere.availability.zone"、Ribbon 读 "zone"——**"写方与读方的键名契约"是跨组件整合的第一性问题**——现代 my-xhs 用 ZoneResolver 统一解析收敛契约；**这类"看起来整合了实际没生效"的坑 = 静默失败**（与 04 篇 SCA 事件线同类教训）
5. **"Ribbon 的历史定位"**（docs:28/311 + 发散）：维护状态（Netflix 停新）+ Spring Cloud 3.x 移除（2020.0 起 LoadBalancer 替代）——**docs 是 2016-2019 时代的设计快照**——机制学习价值（组件骨架/区域优先/子上下文），生产零使用（my-xhs 实证）
6. **"客户端 LB 与现代替代的对应关系"**（发散 + my-xhs）：IRule → ReactorLoadBalancer 自定义；ServerList → Supplier；ServerListFilter → Supplier 链过滤；SpringClientFactory → LoadBalancerClientFactory（**同 NamedContextFactory 底座**——04 篇）——**一一映射**（11 篇 LoadBalancer 展开）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 规则/列表/过滤/更新四件套 | 可插拔扩展 vs 单一实现（组件化标准） |
| Zone 3 级来源（metadata > hostname > 配置） | 精确 vs 兜底（生产必须 metadata） |
| 过滤先行（ServerListFilter） vs 规则内做区域 | 架构清晰 vs 算法耦合（标准：过滤先行） |
| Ribbon（维护） vs LoadBalancer（现代） | 历史稳定 vs 生态演进（2020.0 移除——必迁） |
| 键名契约（"zone" vs "microsphere.availability.zone"） | 统一收敛 vs 各自为政（整合成败） |

### 常见坑/反模式

1. **区域信息缺失/默认 UNKNOWN**：Server.zone 默认 "UNKNOWN"——无 metadata 标记的区域路由全部失效（静默）
2. **hostname 近似当精确**：extractApproximateZone 是便捷兜底——生产用 metadata（docs:390-394 顺序）
3. **键名契约不一致**：写方读方不同键 → 整合"看起来做了"实际不生效（docs:396 现场——**静默失败**）
4. **区域优先无回退**：同区域实例不足仍硬选 → 单实例打垮（07/08 篇保护性失效衔接）
5. **Ribbon 思维照搬**：IRule/ServerList API 已过时——现代 LoadBalancer 的 Supplier 链是响应式模型（学习骨架、迁移实现）
6. **过滤层做算法**：区域过滤进规则（IRule）——违背"过滤先行、规则后选"（扩展性受损）

### 生态位置

- **stage-4 教学主线**：**负载均衡组（10-12 第一篇）**——09 注册发现 → **10 客户端 LB 组件模型（本篇：Ribbon 场景 + 区域优先）** → 11 LoadBalancer（docs 顺序——现代实现）→ 12 REST Client → 14-15 网关
- **前后篇衔接**：04 篇（NamedContextFactory——子上下文底座）；07/08 篇（AZ Locator——区域面）；11 篇（LoadBalancer——现代展开）；stage-1 11/12（LB 基础）；stage-3 01/06（my-xhs LB 实证）
- **与源码提取的关系**：Ribbon `[无本地源码]`；my-xhs 现代栈实证（LoadBalancer/Zone Supplier——07 篇衔接）

**架构师视角结论**：本篇为 **Ribbon 设计文档（425 行）**——客户端 LB 组件模型（10 组件/8 默认实现——四件套骨架）+ 区域优先（ZonePreference 过滤 + Zone 3 级来源）+ AZ Locator 整合（**键名契约坑——docs:396 静默失败现场**）；**知识本体 = "客户端 LB 的组件化设计与区域感知"**（机制时间无关——现代 LoadBalancer 继承骨架）；**Ribbon 已过时（docs 自证：维护状态/3.x 移除）——参考回退现代**（my-xhs LoadBalancer + ZonePreference Supplier 完整对应）；负载均衡组开篇，11 篇进入 LoadBalancer 现代实现。
