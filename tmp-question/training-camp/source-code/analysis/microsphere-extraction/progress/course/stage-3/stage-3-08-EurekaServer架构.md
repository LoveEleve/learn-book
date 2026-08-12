# stage-3 · 第 08 节：第六节："高性能" Eureka Server 架构 — 知识点提取

> 课程：stage-3 三高架构 第 08 节（容器/服务组 08/10）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/08. 第六节："高性能" Eureka Server 架构.md`
> 提取时间：2026-08-12 | 权重：核心（注册中心服务端架构——集群复制/事件化/REST 分层/同步协议选型）
> 案例载体：my-xhs（决策 B）+ **参考实现锚定 Nacos，Eureka 仅 docs 场景（08 SOP，延续 07 篇纪律）**

---

## 一、本节概览

- **技术域**：注册中心服务端架构（集群状态复制/Tomcat 集群/事件化/REST API 分层/引导装配/租约管理/同步协议）
- **维度**：`[分布式问题]`（注册中心服务端）+ `[工程问题]`（双部署模式/事件扩展）
- **核心命题**：**注册中心服务端的四个架构机制**——①集群状态复制（docs 用 Tomcat 上下文复制 + JGroup 广播改造 P2P）②事件化（注册/注销/更新可观测）③REST 分层（/apps 三级）④引导装配与租约管理；参考实现 Nacos（07 篇续）
- **知识点数**：8 个
- **前置**：07 篇（注册发现机制）、05 篇（Tomcat 容器）、stage-2 08（Distro）、stage-1 03/04（REST）

## 前置条件清单
读者需先掌握：
1. **注册中心机制**（07 篇：注册/心跳/拉取/注销/集群）
2. **Nacos Distro/Raft**（stage-2 07/08）
3. **Tomcat 容器基础**（05 篇 KP-09）
4. **REST API 设计**（stage-1 03/04）
未达前置者，先补：07 篇 + stage-2 08

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **机制用 Nacos 讲**：事件中心（NotifyCenter）、REST 分层（InstanceController）、注册表（ServiceManager）、同步（Distro）
- **Eureka 场景位**：docs 的 Tomcat 集群/事件类名/JAX-RS 作场景背景
- **docs 场景 vs 现代**：docs 的"JGroup 组播改造 P2P"是 2016 前后探索，现代主流是 TCP 增量同步（Distro）——同步协议选型对照

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 注册中心集群状态复制（上下文复制思路与改造方向）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Servlet 上下文/Session 概念
- **来源**：docs §Apache Tomcat 集群（上下文复制演示/ReplicatedController）+ 架构师发散
- **需求**：理解"集群状态复制"的一种实现思路——**把注册表状态当作可复制的上下文数据**（ServletContext 复制 + 事件驱动）
- **自主实现**：若我设计——状态写入 ServletContext（容器层自动复制到集群节点）；实例变更包装成事件 + ID 鉴定器，跨节点传播
- **参考实现**（docs 场景 + 发散）：**docs 上下文复制两层**——Session（HttpSession）与 Servlet 应用（ServletContext）上下文复制；**ReplicatedController 示例**——`implements ServletContextAttributeListener, HttpSessionAttributeListener` + REST 端点（set/get context/session 属性）；**扩展思路（docs 明确）**——Eureka Client 注册请求在 HTTP 线程中处理 → 经 `RequestContextHolder` 取 ServletRequest/ServletContext → **InstanceInfo 序列化 JSON 挂到 ServletContext** → Tomcat 集群复制能力传播；**事件化**——不同 InstanceInfo 操作包装成事件，用 ID 作鉴定器（`ReplicationInstance` 参考）；**机制本质（发散）**——"注册表状态 = 可复制上下文 + 变更事件"——与 07 篇"操作复制到 peer + 下个心跳协调"同构（docs 场景：用容器复制层代替自研复制）
- **对比取舍**：**容器层复制（Tomcat 上下文复制）vs 应用层自研复制（Eureka P2P/Distro）**——容器复制省开发但绑定 Servlet 容器、粒度粗（整上下文）；自研复制可控（增量/协调/退避）——**现代注册中心走自研复制（Nacos Distro TCP 增量，stage-2 08）**
- **测试佐证**：docs §上下文复制演示/§ReplicatedController（代码全文）+ stage-2 08（Distro 对照）

### KP-02 双部署模式（FAT JAR vs WAR + Maven Profile）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]`（WAR 部署已非主流但模式有效） | **置信度**：High
- **前置**：Maven Profile、Spring Boot 打包
- **来源**：docs §环境准备（pom 配置全文）
- **需求**：掌握"一套代码双部署形态"的工程模式——**FAT JAR（内嵌容器）vs WAR（外置容器）**
- **自主实现**：若我设计——Maven Profile 切换：spring-boot（FAT JAR + 内嵌 Tomcat）vs web（WAR + 排除内嵌容器）
- **参考实现**（docs pom 全文）：**依赖上下文（docs §当前情况组件列表）**——Eureka Server 构建于 Spring Cloud Netflix + Spring Cloud Commons + Spring Boot Web Starter（含 Tomcat 嵌入式）之上——**"服务器即 Web 应用"的架构前提**（注册中心服务端=Spring Boot Web 应用）；**通用配置**——默认排除 `spring-boot-starter-tomcat`（保留 web 能力但容器可替换）；**spring-boot Profile**（默认激活）——单独加回 starter-tomcat + spring-boot-maven-plugin（可执行 JAR）；**web Profile**——`<packaging>war</packaging>` + maven-war-plugin（外置 Servlet 容器部署）；**WAR 化细节**——`src/main/webapp/WEB-INF/web.xml` + **`<distributable/>`**（声明分布式 Web 应用——集群复制前提，docs 明确）；**构建**——`mvn clean package -Pweb`
- **对比取舍**：**内嵌（FAT JAR）vs 外置（WAR）**——部署简单/版本一致 vs 容器级能力（集群复制/独立管理）；现代微服务默认 FAT JAR，WAR 仅特殊场景（集群上下文复制需要外置容器）
- **测试佐证**：docs §环境准备（两个 Profile pom 全文 + web.xml）

### KP-03 Tomcat 集群机制（Tribes/上下文复制组件）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：Tomcat 架构（stage-1 07）
- **来源**：docs §集群模块 Tomcat Tribes + §相关资料（Tomcat 集群文档）
- **需求**：理解 Tomcat 集群的上下文复制组件——**ReplicatedContext/ReplicatedMap + GroupChannel**
- **自主实现**：若我设计——容器级复制：ServletContext 适配器 + 集群 Channel + 复制 Map
- **参考实现**（docs + 发散）：**ReplicatedContext**——继承 `org.apache.catalina.core.StandardContext`（支持复制的 ServletContext 实现，docs 明确）；**ReplicatedMap**——依赖 **Tomcat Cluster Channel**（默认 `org.apache.catalina.tribes.group.GroupChannel`，docs 明确）；**Tribes**——Tomcat 集群通讯框架（组播发现/消息传递）；**集群配置实例（docs §环境准备）**——Tomcat 8.5+（8.5.61/8.5.90）、cluster-howto 配置 `src/main/webapp/META-INF/conf/server.xml`、`catalina.sh` 双节点 `JAVA_OPTS`（`-Dserver.port=12345/12346` + `-Deureka.client.serviceUrl.defaultZone=互指`——**P2P 拓扑配置实例**："服务注册优先选择第一个配置节点，服务订阅排除第一个非自我节点"）；**Session 复制对照**——DeltaManager/BackupManager（docs 关联 Tomcat cluster-howto 文档 `[待验证：DeltaManager 细节]`）；**场景**——docs 用 Tomcat 集群复制 Eureka Server 的上下文（KP-01 思路的容器层实现）
- **对比取舍**：**容器集群复制 vs 注册中心自复制**——通用机制（任何 web 应用可用）vs 专用（增量/协调）——Tomcat 集群适合"共享状态复制"场景，注册中心状态用自研复制更精细
- **测试佐证**：docs §Tomcat Tribes（ReplicatedContext/ReplicatedMap/GroupChannel 类名）+ §相关资料（cluster-howto 链接）

### KP-04 注册中心事件化（注册/注销/更新事件——可观测与扩展点）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：事件驱动概念
- **来源**：docs §扩展 Spring Cloud Netflix Eureka Server 事件 + §Spring Cloud 实现细节（事件类名）+ Nacos NotifyCenter 实证
- **需求**：理解注册中心**核心操作的事件化**——注册/注销/更新发布事件（监控/联动/扩展的钩子）
- **自主实现**：若我设计——注册表核心操作（register/deregister/renew）发事件，监听者做监控/级联/自定义逻辑
- **参考实现**（docs 场景 + Nacos 实证）：**docs 事件（SCA）**——`EurekaInstanceRegisteredEvent`（注册）/`EurekaInstanceCanceledEvent`（注销）/`EurekaInstanceRenewedEvent`（更新）（docs 类名实证：`spring-cloud-netflix-eureka-server.event.*`）+ **扩展思路**（docs 明确）：注册请求在 HTTP 线程处理 → 事件可携带 InstanceInfo（序列化 JSON）+ ID 鉴定器；**Nacos 对照（源码实证）**——**`common/notify/NotifyCenter.java:45`**（服务端事件中心）、`:280`（`publishEvent`）；naming 注册/注销发布 trace 事件（`DeregisterInstanceTraceEvent`，InstanceController:27-28 import 实证）；**机制**——注册中心 = 事件源（服务变更通知的下游：缓存失效/监控/网关刷新）
- **对比取舍**：**事件化 vs 直接调**——解耦（监听者可选）/可观测 vs 直接路径简单——**现代注册中心事件化是标配（Nacos NotifyCenter/Spring Cloud DiscoveryEvent）**
- **测试佐证**：docs §事件扩展（3 事件类名）+ `common/notify/NotifyCenter.java:45/280` + `naming/controllers/InstanceController.java:27-28`

### KP-05 服务器 REST API 分层（/apps 三级 Resource 映射）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：REST 分层（stage-1 03/04）
- **来源**：docs §Eureka Server REST API 实现（映射关系 + Resource 类）+ Nacos InstanceController 实证
- **需求**：掌握注册中心 API 的**资源分层**——集合（/apps）→ 应用（/{appId}）→ 实例（/{appId}/{id}）
- **自主实现**：若我设计——三级资源映射 + 每级操作（注册/注销/心跳/查询）
- **参考实现**（docs 映射 + Nacos 实证）：**docs 三级映射（JAX-RS Resource）**——`/apps` → `ApplicationsResource`（集合查询）；`/apps/{appId}` → `ApplicationResource`（注册 `addInstance` @POST；获取 `getInstanceInfo`）；`/apps/{appId}/{id}` → `InstanceResource`（实例级）；**Nacos 对照（源码实证）**——`naming/controllers/InstanceController.java:88`（`@RequestMapping(NACOS_NAMING_CONTEXT + NACOS_NAMING_INSTANCE_CONTEXT)` 即 `/nacos/v1/ns/instance` 前缀）、`:113-117`（`@PostMapping register` → `getInstanceOperator().registerInstance(namespaceId, serviceName, instance)`）、`:143-144`（`@DeleteMapping deregister` + **@TpsControl 限流注解**——开放 API 的保护）；**机制**——注册表 API = 集合/应用/实例三级资源 + 每级 CRUD（注册/注销/心跳/状态/元数据，07 篇 KP-10 操作集）
- **对比取舍**：**三级资源分层 vs 扁平 API**——语义清晰/粒度控制 vs 简单——REST 资源建模标准做法
- **测试佐证**：docs §REST API 映射（3 映射 + addInstance/getInstanceInfo 源码）+ `InstanceController.java:88/113-117/143-144`

### KP-06 服务器引导与上下文装配（Bootstrap 顺序）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07 篇 KP-09（服务器架构——本篇深化）
- **来源**：docs §Netflix Eureka Server 实现细节（EurekaBootStrap 全文 + 步骤）
- **需求**：理解注册中心服务端**引导顺序**——环境 → 序列化 → 客户端 → 注册表 → peer → 同步 → 开流量（07 篇 KP-09 已提取机制，本篇核实 docs 步骤细节）
- **自主实现**：若我设计——生命周期引导：先配后建（环境 → 协议 → 核心组件 → 集群 → 同步 → 对外）
- **参考实现**（docs 步骤 + 交叉引用）：**docs 明确步骤**——①初始化配置（Environment，datacenter/environment 默认值）②初始化上下文：序列化协议（ServerCodecs——**老 XStream `[过时→Jackson]`**）→ EurekaClient（AWS/非 AWS）→ PeerAwareInstanceRegistry（AwsInstanceRegistry / PeerAwareInstanceRegistryImpl）→ PeerEurekaNodes（副本节点容器）→ **syncUp 邻居同步** → openForTraffic（开流量）→ 监控注册；**交叉引用**——机制与 07 篇 KP-09 同（引导装配 + 三职责接口）；**Nacos 对照**——naming core v2 `ServiceManager`（`naming/core/v2/ServiceManager.java` 实证——服务注册表核心）+ `InstanceOperatorClientImpl`（07 篇已证）——同构职责
- **对比取舍**：**引导顺序是注册中心的"启动契约"**——先内后外（组件齐 → 同步完 → 才开流量）——避免"半状态对外服务"（docs"openForTraffic"语义）
- **测试佐证**：docs §EurekaBootStrap（contextInitialized 全文 + 步骤 8 条）+ `naming/core/v2/ServiceManager.java` + 07 篇 KP-09

### KP-07 租约管理与复制标志（LeaseManager register 三参数）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07 篇 KP-02（租约）
- **来源**：docs §租约管理 LeaseManager（接口源码）+ §服务实例注册（addInstance 的 isReplication 头）
- **需求**：理解注册表写路径的**复制语义**——`isReplication` 标志区分"本地注册 vs 副本同步"
- **自主实现**：若我设计——register(r, leaseDuration, isReplication)：复制来的注册不重复广播（防回环）
- **参考实现**（docs 接口 + 场景）：**LeaseManager.register 三参数**——`register(T r, int leaseDuration, boolean isReplication)`（docs 接口源码：r=被注册对象（存在租约期）、leaseDuration=租约期、**isReplication=是否来自副本节点**）；**REST 头**——`ApplicationResource#addInstance` 读取 `PeerEurekaNode.HEADER_REPLICATION` 头（`@HeaderParam`）→ `registry.register(info, "true".equals(isReplication))`（docs 源码实证）；**机制意义（发散）**——复制标志防无限循环（A 同步给 B，B 不再回同步给 A）+ 副本注册不触发本地驱逐统计；**Nacos 对照**——Distro 同步数据带复制标志（stage-2 08 已深挖 `[交叉引用]`）
- **对比取舍**：**复制标志（防回环）vs 无状态复制**——复制协议必修课；无标志会循环风暴
- **测试佐证**：docs §LeaseManager（接口全文）+ §注册（addInstance + HEADER_REPLICATION 源码）+ stage-2 08

### KP-08 集群同步协议选型（P2P 复制 vs JGroup 组播 vs Distro TCP 增量）【docs 主要内容①】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]`（JGroup 改造方向 [过时→TCP 增量同步为主流]） | **置信度**：High
- **前置**：07 篇 KP-08（P2P）、stage-2 08（Distro）
- **来源**：docs §主要内容（JGroup 改造）+ §相关资料（JGroup）+ 架构师对照
- **需求**：理解集群同步协议的**三种形态与选型**——docs 主要内容第 1 条：用 JGroup 广播代替 Eureka 原生 P2P 复制
- **自主实现**：若我设计——同步协议候选：①点对点复制（Eureka）②组播广播（JGroup）③TCP 增量同步（Distro 类）；按集群规模/消息量/一致性选
- **参考实现**（docs 场景 + 对照）：**docs 意图**——**P2P 协议改造：基于 JGroup 广播 Eureka Server 服务实例状态，代替 Eureka Server 原生 P2P 复制**（docs 主要内容原文）；**JGroup**（docs 相关资料）——Java 组通讯库（组播/可靠消息，长期用于集群通讯）；**三种形态对照（发散）**——①**P2P 复制**（Eureka 原生：每个 peer 两两复制 + 下个心跳协调，07 篇 KP-08）——简单但 N 节点 O(N²) 消息 ②**组播广播**（JGroup：一发全收）——广播效率高但需组播网络支持（云环境常不可用）、无确认 ③**TCP 增量同步**（**Nacos Distro：主节点 + 增量 + 校验**，stage-2 08 已深挖）——**现代主流**：增量传输省流量 + 校验一致性 + TCP 可靠；**选型结论（发散）**——云环境组播不可靠 → 现代注册中心（Nacos/Consul）走 TCP 增量/复制而非组播；docs 的 JGroup 改造是 2016 前后探索，方向（优化复制效率）正确但形态已被 Distro 类取代
- **对比取舍**：**广播（一发全收）vs 增量（按需传输）**——同步效率 vs 网络依赖/可靠性——**现代选型：TCP 增量同步 + 校验（Distro）；组播仅局域网可控场景**
- **测试佐证**：docs §主要内容（JGroup 改造原文）+ §相关资料（JGroup 官网链接）+ stage-2 08（Distro 增量同步）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 集群状态复制（上下文复制思路） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 双部署模式（FAT JAR vs WAR） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| Tomcat 集群机制（Tribes） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| 注册中心事件化（3 事件） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| REST API 分层（/apps 三级） | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| 服务器引导与上下文装配 | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| 租约管理与复制标志 | 分布式问题 | 核心 | P2 | 🟡 | 时间无关 | High |
| 集群同步协议选型（3 形态） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：Nacos（`code/spring/nacos`）+ my-xhs + stage-2 07/08 交叉引用
- **关键源码**（本次实证）：
  - `common/notify/NotifyCenter.java:45/280`（事件中心 + publishEvent——注册中心事件化对照）
  - `naming/controllers/InstanceController.java:88`（`/nacos/v1/ns/instance` 前缀）/`:113-117`（@PostMapping register → `getInstanceOperator().registerInstance`）/`:143-144`（@DeleteMapping + @TpsControl 限流）
  - `naming/core/v2/ServiceManager.java`（v2 服务注册表）+ `naming/core/InstanceOperatorClientImpl.java`（07 篇已证）
  - stage-2 08（Distro 增量同步——KP-07/08 交叉引用）
- **诚实标注**：docs §自我保护模式（标题空节）→ 交叉引用 07 篇 KP-07 不重复；docs §更新（Renew）/§获取注册表（Fetch Registry）/§服务实例更新（标题空节）→ 内容已含 07 篇 KP-02/03，交叉引用 `[跳过：重复内容交叉引用]`；docs §服务实例淘汰任务 EvictionTask（标题）→ 07 篇 KP-02 过期驱逐已提取，交叉引用；docs §API 数据序列化（两处标题空节）`[跳过：标题空节无内容]`；docs §Spring Cloud 实现细节（配置适配/引导/CodecWrapper 标题空节）→ 配置适配已含于 07 篇 KP-11 `[跳过/交叉引用]`；docs 的 JGroup 改造为场景探索，现代主流 Distro TCP 增量（选型发散）
- **关联标注**：07 篇（机制全集——本篇服务端深化）；05 篇（Tomcat 容器参数——本篇 Tomcat 集群）；stage-2 08（Distro——同步协议对照核心）

---

## 五、本节小结（三层次视角）

**需求**：注册中心服务端架构——集群状态复制、事件化、REST 分层、引导装配、租约复制标志、同步协议选型。

**自主实现核心**：若我设计——①状态变更事件化（注册/注销/更新，NotifyCenter 类事件中心）②REST 三级资源（/apps/{appId}/{id}）③引导顺序"先内后外"（组件齐 → 同步 → 开流量）④register 带 isReplication 防回环 ⑤同步协议选 TCP 增量（Distro 类）而非组播。

**参考实现**：**Nacos 实证**（NotifyCenter、InstanceController、ServiceManager）+ stage-2 08（Distro）+ my-xhs；**Eureka 仅 docs 场景**（Tomcat 集群复制/JAX-RS/事件类名作背景）。

**对比取舍**：知识本体是"注册中心服务端的四个机制"——**复制思路（容器层 vs 自研）、事件化、资源分层、复制标志防回环**；同步协议三形态（P2P/组播/增量）选型结论：现代走 TCP 增量（Distro），docs 的 JGroup 组播是历史探索。

**待验证汇总**：
- Tomcat DeltaManager/BackupManager Session 复制细节（docs 关联 cluster-howto 文档）
- Nacos NotifyCenter 的 naming 事件类型清单（已证存在，具体事件类待展）
- Nacos OpenAPI 与 docs /eureka/v2/apps 三级映射的逐端点对应

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标/知识点 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 服务端架构（注册表/事件/REST） | ⚠️ Nacos 服务端为第三方组件（my-xhs 不维护）；my-xhs 侧直接消费 Nacos 能力 | 无直接动作项 |
| 集群同步（P2P/组播/增量） | ✅ 采用 Distro 增量（Nacos 内建，stage-2 08）——docs 场景的 JGroup 组播未采用（正确选型） | 无 |
| 事件化消费 | ⚠️ my-xhs 侧对 Nacos 注册/注销事件的监听未验证 | `[待验证]`：如需联动（网关路由刷新等）补事件监听 |

**结论**：08 篇服务端架构为基础设施（Nacos 提供），my-xhs 侧动作少；唯一待核对 = 服务变更事件的消费联动。

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Eureka Server 架构探索文档（Tomcat 集群 + JGroup 改造，2016 前后）；机制时间无关；参考实现 Nacos 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：注册中心服务端架构的完整认知该讲什么

docs 覆盖复制/事件/REST/引导。完整还该包含：

1. **"状态复制"是注册中心高可用的核心命题**（docs 主线 + 发散）：docs 给出两条路线——容器层复制（Tomcat 上下文复制）与协议层复制（P2P/JGroup 改造）；现代答案（发散）——**自研增量同步（Distro）**，因为注册表变更小而频繁，增量 + 校验远比整上下文/组播复制精细（stage-2 08）
2. **事件化是注册中心的"可观测与联动"钩子**（docs 明确 + 发散）：注册/注销/更新事件 → 监控告警（续订率/驱逐风暴）、级联刷新（网关路由/缓存）、审计；Nacos NotifyCenter 即此机制（实证）——**注册中心不仅是存储，还是事件源**
3. **开放 API 的防护是必修**（发散）：docs 的 REST 表只讲操作；Nacos `@TpsControl`（实证）提醒——**注册 API 面向所有服务，必须限流/鉴权**（生产事故源：注册风暴打垮注册中心）
4. **复制标志防回环是复制协议必修课**（docs 源码 + 发散）：`isReplication` 头/参数——A→B 的同步不再回 B→A；**任何集群同步实现都要防循环**（Distro 同样，stage-2 08）
5. **引导顺序是"启动契约"**（docs 明确）：组件齐 → syncUp → openForTraffic——**半状态不对外**（先内后外）；Nacos v2 ServiceManager 同构
6. **部署形态的取舍**（docs pom + 发散）：FAT JAR（现代默认）vs WAR（容器级能力：集群复制/独立运维）——**微服务默认 FAT JAR；需要容器集群能力才 WAR**——docs 的 Tomcat 集群路径随 Eureka 一起退场，但"双 Profile 部署"工程模式仍有效
7. **组播的教训**（docs JGroup + 发散）：组播依赖网络支持（云 VPC 常禁组播）——**云原生时代的同步协议必须 TCP 可靠通道**（gRPC/HTTP）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 容器层复制 vs 自研复制 | 省开发绑容器 vs 可控增量（现代走自研） |
| 事件化 vs 直接调 | 解耦可观测 vs 直接简单 |
| REST 三级分层 vs 扁平 | 语义粒度 vs 简单 |
| P2P vs 组播 vs TCP 增量 | O(N²) / 网络依赖 / 可靠增量（现代选增量） |
| FAT JAR vs WAR | 简单一致 vs 容器级能力 |
| isReplication 标志 | 防回环必修 vs 多一次头传递 |

### 常见坑/反模式

1. **同步无防回环**：复制不带来源标志——循环风暴（docs isReplication 头教训）
2. **组播依赖网络**：云环境组播不可用——同步协议必须 TCP（docs JGroup 场景的教训）
3. **注册 API 无防护**：注册风暴打垮注册中心（Nacos @TpsControl 实证——限流必配）
4. **半状态对外**：引导未完成就开流量（docs openForTraffic 语义——先内后外）
5. **整上下文复制**：粒度粗（Session/ServletContext 全量）——注册表状态应增量（Distro）
6. **事件不接监控**：注册/注销/更新事件是事故先兆（驱逐风暴）——不监听=盲
7. **WAR 化过度**：无容器级需求却 WAR 部署——运维复杂度上升（FAT JAR 默认）

### 生态位置

- **stage-3 教学主线**：容器/服务组（05-10）——07 注册发现机制 → **08 服务端架构（本篇）** → 09 HTTP 架构升级 → 10 RPC 架构升级；**07/08 合为注册中心完整认知（客户端机制 + 服务端架构）**
- **前后篇衔接**：07 篇（客户端机制/集群 P2P）→ 本篇（服务端：复制/事件/REST/引导/同步选型）；stage-2 08（Distro——本篇同步选型的现代答案）；05 篇（Tomcat 容器）→ 本篇 Tomcat 集群
- **与源码提取的关系**：Nacos（code/spring）为现代参考源；08 篇收官注册中心主题，09/10 转向 HTTP/RPC 架构

**架构师视角结论**：本篇以 **Nacos 讲服务端机制**（NotifyCenter 事件化、InstanceController 三级 REST + TpsControl、ServiceManager 注册表、Distro 增量同步）、Eureka 服务端探索（Tomcat 上下文复制/JGroup 组播）作 docs 场景——知识本体是"注册中心服务端四机制"：**状态复制（选型：自研增量）、事件化（可观测钩子）、REST 分层（资源建模 + 限流防护）、复制标志（防回环）**；docs 的 JGroup 改造方向正确但形态被 Distro 类 TCP 增量取代，是"机制演进"的活案例。
