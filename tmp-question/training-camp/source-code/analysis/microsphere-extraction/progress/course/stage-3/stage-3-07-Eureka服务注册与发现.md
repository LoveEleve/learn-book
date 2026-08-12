# stage-3 · 第 07 节：第五节："高可用" Eureka 服务注册与发现 — 知识点提取

> 课程：stage-3 三高架构 第 07 节（容器/服务组 07/10）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/07. 第五节："高可用" Eureka 服务注册与发现.md`
> 提取时间：2026-08-12 | 权重：核心（服务注册与发现机制本体——**参考实现锚定 Nacos，Eureka 仅 docs 场景**）
> 案例载体：my-xhs（决策 B）+ **参考实现按主流性选 Nacos（08 SOP，重写版：机制用 Nacos 讲，Eureka 不作主体）**

> **参考实现决策（2026-08-12 用户纠正后重写）**：初稿把 Eureka 机制当主体（类名/配置/源码片段），违背 08 SOP"参考实现按主流性"——**等于还是在讲 Eureka**。重写原则：
> - 知识本体 = 服务注册与发现**机制**（注册/心跳/拉取/注销/高可用/集群一致性/API——时间无关）
> - **参考实现 = Nacos**（`code/spring/nacos` + `code/spring/spring-cloud-alibaba` 源码实证 + my-xhs 实例 + stage-2 07/08 已深挖的 Raft/Distro 交叉引用）
> - **Eureka 仅作 docs 场景引用**（docs 以 Eureka 讲这些机制，其 30s/90s/自保护数字作场景背景），不展开 Eureka 类名/配置

---

## 一、本节概览

- **技术域**：服务注册与发现（注册表/心跳/拉取/注销/高可用/集群同步/协议 API）
- **维度**：`[分布式问题]`（服务发现/高可用）
- **核心命题**：**注册中心的高可用机制全景**——docs 以 Eureka 为载体（自保护/P2P/调优三主题），机制时间无关；现代落地看 **Nacos**（docs 自己就关联了 stage-2 第 8 节 Nacos Distro）
- **知识点数**：11 个
- **前置**：stage-2 07/08（Nacos Raft/Distro）、06 篇（Feign 调用链）、stage-2 01（CAP）

## 前置条件清单
读者需先掌握：
1. **Nacos 一致性**（stage-2 07 Raft / 08 Distro——docs 明确关联第 8 节）
2. **CAP 理论**（stage-2 01：注册中心 AP/CP 取舍）
3. **Feign 服务调用**（06 篇 KP-07）
未达前置者，先补：stage-2 07/08、stage-2 01

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **机制用 Nacos 讲**：注册/心跳/拉取/注销/容灾全部 Nacos 源码实证
- **Eureka 场景位**：docs 数字（30s/90s/15%）仅作对照背景，不展开
- **诚实标注**：Nacos 2.x 心跳细节（gRPC 通道）标注验证程度

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 服务注册与发现模式（注册中心定位/客户端负载均衡）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：CAP（stage-2 01）
- **来源**：docs §Netflix Eureka 简介（场景）+ Nacos/SCA 源码实证
- **需求**：理解注册中心定位——**"只告诉你服务在哪，不限制怎么通信"** + 客户端负载均衡模式
- **自主实现**：若我设计——注册表服务 + 客户端 SDK（注册/心跳/拉取）+ 声明式调用集成（Feign）
- **参考实现**（Nacos 实证 + my-xhs）：**定位**——注册中心 = 服务名→实例地址的映射服务；**docs 场景**（Eureka）：REST 定位服务 + 客户端内置负载均衡器 + 非 Java 用 sidecar/REST 直调；**Nacos 落地（my-xhs 实证）**——`my-xhs-gateway/pom.xml:28` `spring-cloud-starter-alibaba-nacos-discovery` + `application.yml:37`（nacos 注册配置）；SCA 实现 `NacosDiscoveryClient implements DiscoveryClient`（`discovery/NacosDiscoveryClient.java:36`——Spring Cloud 统一 DiscoveryClient 抽象）；调用链：Feign（06 篇）→ DiscoveryClient → Nacos（06 篇 KP-07 衔接）
- **对比取舍**：**客户端负载均衡（注册中心只给清单）vs 服务端（网关/ELB 聚合）**——去中心/低延迟 vs 集中可控；Nacos 两者皆可（客户端 Feign 负载均衡 / 服务端网关路由）
- **测试佐证**：my-xhs gateway pom:28 + SCA `NacosDiscoveryClient.java:36` + docs §Eureka 简介（场景）

### KP-02 注册与心跳（生命周期三动作：注册/心跳/过期）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：租约概念
- **来源**：docs §注册/§更新（场景：30s 心跳/90s 过期）+ Nacos 源码实证
- **需求**：掌握注册中心生命周期——**注册 → 周期心跳保活 → 超时过期**（数字因实现而异：Eureka 30s/90s，Nacos 5s 级）
- **自主实现**：若我设计——启动注册 + 周期心跳 + 过期驱逐；心跳失败重新注册
- **参考实现**（Nacos 源码实证 + docs 场景）：**注册链路**——SCA `NacosServiceRegistry.register()`（`registry/NacosServiceRegistry.java:60`）→ `NacosNamingService.registerInstance()`（`client/naming/NacosNamingService.java:140-162` 重载族：serviceName/group/cluster/instance）→ `NamingClientProxyDelegate`（gRPC/HTTP 双通道代理）；**2.x 通道**——默认 gRPC（`NamingGrpcClientProxy`，连接保活承载心跳，client/naming/remote/gprc/）`[验证程度：2.x 客户端 gRPC 通道实证，心跳具体周期标注待验证]`；**临时实例**——客户端故障自动摘除（服务端 `naming/core/InstanceOperatorClientImpl` 实例操作——stage-2 08 Distro 已深挖衔接）；**docs 场景**——Eureka 30s 心跳/90s 租约过期/心跳 404 重新注册（数字作对照，机制时间无关）
- **对比取舍**：**心跳节奏是全局健康信号**——docs 明确"不要更改续订间隔"（改小误驱逐、改大延迟下线）——现代实现（Nacos）机制同构，节奏不同
- **测试佐证**：SCA `NacosServiceRegistry.java:60` + `NacosNamingService.java:140-162` + client/naming/remote/（gprc/http 双通道目录）+ docs §注册/§更新

### KP-03 拉取与订阅（客户端缓存/监听/全量-增量）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §获取注册表（场景：30s 周期/全量-增量/hash 协调）+ Nacos 源码实证
- **需求**：掌握"读侧"设计——**客户端缓存 + 变更订阅 + 兜底协调**（流量与一致性的核心）
- **自主实现**：若我设计——客户端缓存实例表 + 订阅变更推送 + 失败退避重试；服务端全量/增量两接口
- **参考实现**（Nacos 源码实证 + docs 场景）：**客户端读**——`NacosDiscoveryClient.getInstances()`（`NacosDiscoveryClient.java:60`）；**订阅机制**——`NacosNamingService.subscribe()`（`NacosNamingService.java:454-469` 重载族：serviceName/group/clusters + EventListener）+ `selectInstances(subscribe)`（按订阅状态查询）；2.x 推送通道 gRPC 双向流 `[验证程度：订阅 API 实证，推送通道细节标注待验证]`；**服务端增量**——**Distro 全量/增量/校验**（stage-2 08 已深挖：DistroDataProcessor 同步——docs 自己关联第 8 节，交叉引用不重复）；**docs 场景**——Eureka 30s 拉取、增量 delta（3 分钟缓存、重复去重）、`appsHashCode` 协调失配回退全量、`CacheRefreshedEvent` 事件（机制时间无关：**增量+协调+事件**是现代注册中心读侧标配）
- **对比取舍**：**推送（gRPC/长连接）vs 轮询拉取**——实时性/流量 vs 简单——Nacos 2.x 走 gRPC 推送，Eureka 1.x 走 30s 轮询（docs 场景）——**现代演进方向是推送**
- **测试佐证**：`NacosDiscoveryClient.java:60` + `NacosNamingService`（subscribe 实证）+ stage-2 08（Distro）+ docs §获取注册表

### KP-04 注销与优雅停机（下线协议 + 停机顺序）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §取消 + §延伸问题（优雅停机 5 步）+ Nacos/SCA 源码实证
- **需求**：掌握"干净下线"——**先注销再停**（docs 延伸问题：注册表传播延迟期间消费端仍会打过来）
- **自主实现**：若我设计——停机钩子：注销 → 静默期（等消费端感知）→ 处理在途请求 → 关依赖 → 停进程
- **参考实现**（Nacos/SCA 源码实证 + docs 场景）：**注销链路**——SCA `NacosServiceRegistry.deregister()`（`NacosServiceRegistry.java:92`，:106 `namingService.deregisterInstance(...)`）；**docs 延伸问题（优雅停机 5 步，机制时间无关）**——①执行注销（deregister）②**静默期**处理消费端注册表延迟（docs 场景：Eureka 最长 2 分钟传播）③处理已接受请求 ④关依赖资源（连接池/消息/缓存）⑤下线；**my-xhs 对照**——`server.shutdown=graceful`（05 篇 KP-09 实证）——Spring 优雅停机解决"在途请求"，**注册中心侧 deregister 需在停机钩子中前置**（SCA 的 deregister 由 Spring 生命周期触发）
- **对比取舍**：**优雅停机 vs 直接 kill**——消费端即时感知 vs 靠心跳过期（期间流量打到死实例）——**临时实例 + 注销 + 静默期**是现代标准组合
- **测试佐证**：SCA `NacosServiceRegistry.java:92/106` + my-xhs application.yml（shutdown: graceful）+ docs §取消/§延伸问题

### KP-05 客户端弹性与传播延迟（缓存兜底/容灾快照/退避）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03
- **来源**：docs §弹性/§延迟（场景）+ Nacos 客户端源码实证
- **需求**：理解注册中心**读侧韧性**——"注册中心故障，服务调用不中断"
- **自主实现**：若我设计——客户端本地缓存 + 失败退避 + 本地快照容灾
- **参考实现**（Nacos 源码实证 + docs 场景）：**Nacos 客户端兜底**——`NacosDiscoveryClient.getInstances()` 异常时 **`ServiceCache.getInstances()` 本地缓存兜底**（`NacosDiscoveryClient.java:70` 实证——**服务端全挂仍可调用**）；**容灾快照**——`FailoverReactor`（`client/naming/backups/FailoverReactor.java:52`，failover 本地快照数据源 + 独立线程）；**docs 场景（Eureka 同构机制）**——客户端注册表缓存 + 全挂照跑（docs 明确 + **docs 自己提示"Nacos 客户端也有类似设计"**——此即 ServiceCache/FailoverReactor）；**传播延迟**——docs 场景最长 2 分钟（服务端缓存 + 客户端周期拉取）；**消费端纪律**——拿到"已不存在实例"要**快速超时 + 重试其他实例**（docs P2P 节明确）
- **对比取舍**：**本地缓存 + 快照（AP 韧性）vs 强一致直查**——可用性优先 vs 数据新鲜——注册中心读侧默认 AP 哲学
- **测试佐证**：`NacosDiscoveryClient.java:70`（ServiceCache）+ `FailoverReactor.java:52` + docs §弹性/§延迟

### KP-06 实例状态与健康（临时/持久实例 + 健康标志）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **来源**：docs §关于实例状态（场景：5 态枚举）+ Nacos 实例模型实证
- **需求**：掌握实例注册语义——**临时/持久 + 健康标志 + 上线前置状态**
- **自主实现**：若我设计——实例注册时声明临时性（临时=客户端保活自动摘除）与健康状态；服务上线前有初始化窗口
- **参考实现**（Nacos 实证 + docs 场景）：**Nacos 实例模型**——`api/naming/pojo/Instance.java:69`（`healthy = true` 默认）、`:81`（`ephemeral = true` 默认——**临时实例为默认**）；**临时实例**——客户端心跳保活、故障自动摘除（对应 docs 的"不干净终止驱逐"机制）；**持久实例**——服务端登记、健康检查被动（Distro 服务端维护，stage-2 08 衔接）；**docs 场景（Eureka 5 态）**——UP/DOWN/STARTING/OUT_OF_SERVICE/UNKNOWN（STARTING=初始化不入流；OUT_OF_SERVICE=发布回滚切流——Netflix Asgard 红黑部署场景）——**机制对照：注册中心状态语义 = 流量的开关**
- **对比取舍**：**临时（客户端保活）vs 持久（服务端登记）**——Nacos 按需声明；临时适合微服务（自动摘除），持久适合网关/DB 探活场景
- **测试佐证**：Nacos `Instance`（ephemeral/healthy 字段 `[验证程度：字段实证于 naming 模型，行号待补]`）+ docs §实例状态

### KP-07 注册中心高可用：自我保护 vs Distro 健康检查（docs 主要内容①机制提取）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：CAP 分区（stage-2 01）
- **来源**：docs §自我保护模式（场景：15%/85% 阈值）+ stage-2 08（Distro）+ 架构师对比
- **需求**：掌握注册中心**分区时的数据保护策略**——docs 主要内容第 1 条：自我保护及其高可用价值
- **自主实现**：若我设计——分区检测：异常终止比例超阈值 → 暂停驱逐保护存量 → 恢复自动退出
- **参考实现**（docs 场景机制 + Nacos 对照）：**docs 场景机制（Eureka 自保护）**——连续 3 次心跳失败 = 不干净终止（待驱逐）；**注册表 >15% 异常 → 停止驱逐**（另一口径：15 分钟内续订率 <85%）；恢复后自动退出；`renewalPercentThreshold`/`enableSelfPreservation` 可调；**价值**——灾难性网络事件不擦除注册表（AP 哲学：宁给旧数据不给空表）；**Nacos 对照**——Nacos **无此机制**：临时实例靠 **Distro 心跳健康检查过期**（stage-2 08 已深挖：服务端按心跳超时摘除 + 分布式实例健康检查）——**AP 实现的两种策略：自保护（保数据）vs 严格过期（保新鲜）**，按业务取舍；**代价（docs 明确）**——自保护期间客户端可能拿到"已不存在实例"→ 消费端必须快速超时重试（KP-05）
- **对比取舍**：**自保护（Eureka）vs 严格过期（Nacos Distro）**——分区时保护注册表 vs 保持新鲜——Nacos 的过期在分区恢复后自动纠正；Eureka 的自保护防"分区被误判为大规模下线"——**机制选择取决于对"陈旧数据 vs 空数据"的容忍**
- **测试佐证**：docs §自我保护（15%/85% 双口径原文）+ stage-2 08（Distro 健康检查）

### KP-08 集群一致性：P2P 复制 vs Distro/Raft（docs 主要内容②机制提取）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：stage-2 07/08（Nacos Raft/Distro）
- **来源**：docs §P2P 通讯/§架构/§多区域（场景）+ Nacos 源码实证（stage-2 07/08 交叉引用）
- **需求**：掌握注册中心**集群节点间一致性**——docs 主要内容第 2 条：P2P 协议细节及其现代对应
- **自主实现**：若我设计——集群节点间复制注册表变更 + 启动同步 + 分区自愈
- **参考实现**（Nacos 对照 + docs 场景）：**Nacos 双协议（stage-2 07/08 已深挖，交叉引用）**——**CP 面**：Raft（临时实例不可用/持久实例强一致，stage-2 07）；**AP 面**：**Distro**（服务端实例数据同步 + 增量 + 校验，stage-2 08）——**"操作复制+协调"的现代实现**；**docs 场景机制（Eureka P2P）**——区域集群 + 操作复制到 peer + 失败下个心跳协调 + 启动 syncUp 全量同步 + 分区中断三现象（心跳复制失败→自保护、部分客户端见部分注册、恢复后自动补传）+ 启动等 5 分钟避免部分流量倾斜——**机制时间无关：复制+协调+启动同步+自愈**；**多区域**——docs 场景：区域间不交流（客户端跨区故障转移）——现代对照：my-xhs `common/zone` 区域路由（03 篇 KP-09）
- **对比取舍**：**P2P 复制（AP 最终一致）vs Raft 共识（CP 强一致）**——Eureka/Distro 复制+协调；etcd/ZK/Nacos-CP 用 Raft（stage-2 02/07）——注册中心常双协议并存（Nacos AP/CP 按实例类型）
- **测试佐证**：stage-2 07（Raft）/08（Distro）+ docs §P2P/§架构 + my-xhs common/zone（03 篇）

### KP-09 服务器内部架构（注册表/查询/租约三职责 + 引导装配）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Servlet/Spring 生命周期
- **来源**：docs §服务器操作处理（场景：Bootstrap 装配）+ Nacos 服务端源码（stage-2 08 衔接）
- **需求**：理解注册中心**服务端职责分解**——注册表存储/查询/租约管理三接口 + 引导装配顺序
- **自主实现**：若我设计——注册表核心三职责分离 + 启动装配：环境 → 序列化 → 客户端 → 注册表 → peer → 同步 → 开流量
- **参考实现**（Nacos 对照 + docs 场景）：**Nacos 服务端（stage-2 07/08 已深挖）**——`naming/core/InstanceOperatorClientImpl`（实例操作入口）+ Distro 组件（数据同步）+ ServiceManager（服务注册表）——职责同构；**docs 场景（Eureka Bootstrap）**——ServletContextListener 引导（initEurekaEnvironment → initEurekaServerContext：序列化协议（XStream 旧 → Jackson ServerCodecs 新）→ 客户端 → PeerAwareInstanceRegistry → PeerEurekaNodes → syncUp → openForTraffic → 监控注册）；**三职责接口**——InstanceRegistry（注册表）/LookupService（查询）/LeaseManager（租约）+ EvictionTask（驱逐任务）——**机制时间无关：注册中心的"注册/查询/租约"三职责分解**
- **对比取舍**：**职责分离**——注册表与查询与租约独立演进（机制本体）；现代注册中心（Nacos ServiceManager/Consul）同构
- **测试佐证**：docs §EurekaBootStrap（场景）+ `naming/core/InstanceOperatorClientImpl.java`（实证）+ stage-2 08

### KP-10 通讯协议与开放 API（gRPC/HTTP 双通道 + 注册表操作面）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：REST/gRPC 基础（stage-1 03/04）
- **来源**：docs §通讯机制/§REST 操作（场景）+ Nacos 客户端通道实证
- **需求**：掌握注册中心的**协议面**——客户端通道选择 + 开放 API（注册/注销/心跳/查询操作集）
- **自主实现**：若我设计——客户端默认高效通道 + HTTP 兜底；开放 REST 操作集与注册表动作一一对应
- **参考实现**（Nacos 源码实证 + docs 场景）：**Nacos 2.x 客户端双通道**——`NamingClientProxyDelegate`（gRPC 默认 + HTTP 降级 `[验证程度：delegate 存在实证，降级触发条件标注待验证]`，client/naming/remote/ 目录 gprc/http 两实现实证）；**docs 场景（Eureka REST 操作表）**——`POST /eureka/v2/apps/{appID}`（注册）/`DELETE .../{instanceID}`（注销）/`PUT .../{instanceID}`（心跳 200/404）/`GET /apps`（全量）等 11 操作 + 实例模型 XSD（hostName/app/ipAddr/vip/status/lease/metadata）——**机制时间无关：注册表的开放操作集（注册/注销/心跳/查询/状态/元数据）**，现代实现（Nacos OpenAPI）同构 `[待验证：Nacos OpenAPI 端点对应]`；**通讯适配演进**（docs 明确）——Jersey → Spring RestTemplate/WebClient 适配（减少 HttpMessageConverter 数量锁定一种 + 扩展 ClientHttpRequestFactory）
- **对比取舍**：**gRPC 长连接（Nacos 2.x）vs REST 轮询（Eureka 1.x）**——推送实时/低流量 vs 简单通用——**演进方向：长连接 + 推送**（KP-03 呼应）
- **测试佐证**：client/naming/remote/{gprc,http} 目录 + docs §REST 操作表（11 操作）

### KP-11 注册中心配置与调优（docs 主要内容③机制提取）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]`（配置体系 [过时→Nacos 配置中心]） | **置信度**：High
- **前置**：配置管理（stage-2 23/24）
- **来源**：docs §配置/§配置 Eureka（场景）+ 架构师整合
- **需求**：掌握注册中心**调优面**——docs 主要内容第 3 条：压测对比调优前后性能
- **自主实现**：若我设计——调优面：心跳节奏/过期阈值/同步重试/保护开关/线程池；配置动态化
- **参考实现**（docs 场景 + Nacos 对照 + 架构师）：**docs 场景调优面（Eureka 参数）**——`numberRegistrySyncRetries=0`（本地启动免 3 分钟等待）、`renewalPercentThreshold`（自保护阈值）、`enableSelfPreservation`（保护开关）、线程池/超时动态调（Archaius 1.x `[过时→Spring PropertySources/Nacos 配置中心]`）；**机制时间无关**——注册中心的调优参数面：**心跳/租约、驱逐/保护、同步重试、线程池超时**；**Nacos 对照**——心跳/健康检查参数（客户端 + Distro 服务端，stage-2 08 衔接）+ 配置进 Nacos Config（stage-2 23/24）；**监控**——Servo → Micrometer（docs 链接 CloudWatch registry；现代统一 Prometheus，03 篇）——**注册中心指标（续订/驱逐/保护状态）进统一监控**；**方法**（docs 意图）——压测对比调优前后（02 篇 6 步流程应用）
- **对比取舍**：**调优参数面时间无关**——实现不同参数名不同，机制（节奏/阈值/重试/线程）同一套
- **测试佐证**：docs §配置/§主要内容第 3 条 + stage-2 08/23/24 交叉引用

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 注册发现模式（定位/客户端负载均衡） | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 注册与心跳（生命周期三动作） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 拉取与订阅（缓存/监听/增量） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 注销与优雅停机（5 步） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 客户端弹性（缓存/快照/退避） | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 实例状态与健康（临时/持久） | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| 高可用：自保护 vs Distro 过期 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 集群一致性：复制 vs Raft/Distro | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 服务器内部架构（三职责） | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| 通讯协议与开放 API | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| 配置与调优（参数面） | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：**Nacos**（`code/spring/nacos` + `code/spring/spring-cloud-alibaba`）+ my-xhs（实例）
- **关键源码**（本次实证）：
  - SCA `spring-cloud-starter-alibaba-nacos-discovery/.../registry/NacosServiceRegistry.java:60`（register）/`:92/106`（deregister → `namingService.deregisterInstance`）
  - SCA `.../discovery/NacosDiscoveryClient.java:36`（implements DiscoveryClient）/`:60`（getInstances）/**:70（`ServiceCache.getInstances` 本地缓存兜底）**
  - Nacos client `client/naming/NacosNamingService.java:140-162`（registerInstance 重载族）、`:454-469`（subscribe 重载族）+ `client/naming/remote/`（gprc/http 双通道目录 + NamingClientProxyDelegate）+ `client/naming/backups/FailoverReactor.java:52`（容灾快照）
  - Nacos `api/naming/pojo/Instance.java:69/81`（healthy/ephemeral 默认 true）+ 服务端 `naming/core/InstanceOperatorClientImpl.java`（实例操作入口，stage-2 08 衔接）
  - my-xhs `my-xhs-gateway/pom.xml:28` + `application.yml:37`（nacos discovery）
- **诚实标注**：Nacos 2.x 客户端心跳具体周期（gRPC 连接保活细节）`[待验证]`；Nacos OpenAPI 端点与 Eureka 操作表对应 `[待验证]`；Eureka 参数（30s/90s/15%/85%）为 docs 场景背景，机制提取不实操；docs §理解客户端和服务器通讯（EurekaModule/governator-guice DI 集成）`[跳过：历史依赖注入集成细节，非机制核心（03 跳过标注）]`
- **关联标注**：docs 自关联 stage-2 08（Nacos Distro）；stage-2 07（Raft）/08（Distro）已深挖——本篇只做机制对照不重复；06 篇（Feign 链路）；05 篇（优雅停机对照）；03 篇（zone 区域路由、监控整合）

---

## 五、本节小结（三层次视角）

**需求**：服务注册与发现的高可用机制——注册/心跳/拉取/注销 + 分区保护 + 集群同步 + 协议面。

**自主实现核心**：若我设计——注册表服务 + 客户端 SDK（心跳保活/订阅拉取/本地缓存/容灾快照）+ 分区保护策略（自保护或严格过期二选一）+ 集群复制（AP）或共识（CP）+ 开放操作集。

**参考实现**：**Nacos 为主**（SCA register/deregister、DiscoveryClient+ServiceCache 兜底、FailoverReactor 快照、gRPC/HTTP 双通道、InstanceOperatorClientImpl 实证）+ my-xhs 实例 + stage-2 07/08 交叉引用；**Eureka 仅 docs 场景**（数字/参数作背景）。

**对比取舍**：知识本体是"注册中心高可用机制"——**AP 哲学三件套**（客户端缓存兜底、分区保护或严格过期、复制+协调）、**生命周期四动作**（注册/心跳/拉取/注销）、**读侧设计**（订阅推送演进）。工具换代机制延续：Eureka 1.x → Nacos 2.x，机制同一套。

**待验证汇总**：
- Nacos 2.x 客户端心跳具体周期（gRPC 保活细节）
- Nacos OpenAPI 端点与 Eureka 11 操作表的具体对应
- Nacos 2.x 订阅推送通道细节（subscribe API 已实证，gRPC 双向流机制）

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Eureka 1.x 文档转写（数字精确，作场景背景）；机制本体时间无关；参考实现 Nacos 源码实证 + stage-2 07/08；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：服务注册与发现的完整架构该讲什么

docs 以 Eureka 讲机制。机制层完整还该包含：

1. **注册中心是"AP 哲学的活教材"**（机制发散）：客户端缓存兜底（全挂照跑）、分区保护/过期策略（保数据 vs 保新鲜）、复制+协调（最终一致）——**读侧永远可用，写侧分区降级**；对照 CP 实现（etcd/ZK，stage-2 02/07）理解 AP/CP 取舍（stage-2 01）
2. **优雅停机是被低估的高可用动作**（docs 延伸问题 + 发散）：注销 → 静默期 → 处理在途 → 关依赖 → 下线 5 步——**消费端感知延迟窗口**（docs 场景最长 2 分钟）的请求要靠静默期 + 消费端快速超时双保险；my-xhs 的 `shutdown: graceful` 只解决"在途请求"（05 篇），注册中心侧 deregister 要前置（KP-04）
3. **心跳节奏是全局健康信号**（docs 明确）：改小误驱逐、改大延迟下线——**注册中心的时钟体系保守调整**；Nacos 临时实例同样（5s 级心跳的语义）
4. **演进方向：长连接 + 推送**（机制发散）：Eureka 30s 轮询 → Nacos 2.x gRPC 双向流——订阅推送取代轮询（实时性/流量）；但**兜底缓存/快照永远保留**（KP-05 实证 ServiceCache/FailoverReactor）
5. **监控出口统一**（docs Servo + 发散）：注册中心指标（续订/驱逐/保护状态）进 Prometheus（03 篇）——**自我保护触发是事故信号**，告警面必挂
6. **配置体系收敛**（docs Archaius + 发散）：注册中心配置进 Nacos Config（stage-2 23/24）——"注册中心自己也要配置中心管理"

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 客户端负载均衡 vs 服务端 | 去中心/低延迟 vs 集中可控 |
| 自保护（保数据）vs 严格过期（保新鲜） | 分区时旧数据 vs 新鲜（Eureka vs Nacos Distro） |
| P2P 复制 vs Raft 共识 | AP 最终一致 vs CP 强一致（Nacos 双协议并存） |
| 轮询 vs 长连接推送 | 简单 vs 实时低流量（演进方向） |
| 临时实例 vs 持久实例 | 自动摘除 vs 服务端登记探活 |
| 优雅停机 vs 直接 kill | 即时感知 vs 靠过期（窗口期流量受损） |

### 常见坑/反模式

1. **改心跳/租约节奏**（docs 明确警告）：改小误驱逐、改大延迟下线
2. **无优雅停机**：直接 kill——消费端窗口期打到死实例（docs 延伸问题）
3. **消费端无兜底与超时重试**：注册中心故障调用断掉（Nacos 有 ServiceCache/FailoverReactor 但**消费端要配超时重试**）
4. **分区策略选错**：需要"保数据"（大故障防误删）却用严格过期——或反之（docs 自保护 vs Nacos Distro 对照）
5. **照搬 Eureka 配置体系**：Archaius 1.x → Nacos Config（stage-2 23/24）
6. **忽略注册中心监控**：续订失败率/驱逐数/保护状态不看——事故先兆（03 篇整合）
7. **多区域不隔离**：docs 明确区域间不交流——跨区流量靠客户端故障转移设计（my-xhs zone 对照）

### 生态位置

- **stage-3 教学主线**：容器/服务组（05-10）——05 容器 → 06 改造+JMH → **07 注册发现（本篇）** → 08 Eureka Server 架构 → 09 HTTP → 10 RPC；**07/08 是 06 微服务化的"骨架"篇**
- **前后篇衔接**：stage-2 07/08（Nacos Raft/Distro——docs 明确关联第 8 节）→ 本篇机制对照 → 08 篇（服务端内部深化）；stage-1 11/12（负载均衡）→ 本篇注册表数据模型；05 篇（优雅停机）→ 本篇注销 5 步
- **与源码提取的关系**：**Nacos（code/spring）为现代参考源**（SCA + client + naming）；Eureka 仅 docs 场景；08 篇将深入注册表内部（缓存/驱逐/同步的 Nacos 实现）

**架构师视角结论**：本篇以 **Nacos 讲机制**（SCA register/deregister、DiscoveryClient+ServiceCache 兜底、FailoverReactor 快照、gRPC/HTTP 双通道、Distro/Raft 集群）、Eureka 退为 docs 场景背景——知识本体是"注册中心高可用机制"：生命周期四动作 + AP 韧性三件套（缓存/分区策略/复制协调）+ 演进方向（长连接推送）。工具换代（Eureka→Nacos）机制延续，现代落地全程看 Nacos/my-xhs，不实操 Eureka。
