# stage-3 · 第 23 节：加餐五：Dubbo 架构设计与实现 — 知识点提取

> 课程：stage-3 三高架构 第 23 节（Dubbo 组 23-24 第一篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/23. 加餐五：Dubbo 架构设计与实现.md`
> 提取时间：2026-08-12 | 权重：核心（Dubbo 分层架构/领域模型/设计原则/三大中心/SPI）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）

> **重复内容处理**：docs 的 §Dubbo SPI（Registry/Cluster/RPC/Filter/问题）五节与 **10 篇（RPC 架构升级）大量重复**（类名/URL 示例/调用链几乎相同）——按 06 纪律**交叉引用不重复提取**，本篇只提取 10 篇未覆盖的新知识（分层架构/领域模型/设计原则/模块分包/三大中心/IoC 演进/代码设计原则/Profiles）。

---

## 一、本节概览

- **技术域**：Dubbo 分层架构（9 层）、领域模型（三域）、设计原则（Microkernel+Plugin/URL）、模块分包、三大中心、SPI 扩展、代码设计原则
- **维度**：`[工程问题]` 主导（架构设计/模块化）+ `[分布式问题]`（三大中心）
- **核心命题**：**Dubbo 架构设计全景**——docs 主要内容：①架构设计（核心模块/协议/中心/Mesh）②三大中心（注册/配置/元数据）③SPI 扩展；docs 后半与 10 篇重复（交叉引用），本篇提取增量
- **知识点数**：8 个
- **前置**：10 篇（Dubbo 调用链/Cluster——本篇交叉引用基础）、07 篇（注册中心）

## 前置条件清单
读者需先掌握：
1. **Dubbo 暴露/消费调用链**（10 篇 KP-02——本篇分层架构的调用视角）
2. **Cluster 选址链**（10 篇 KP-04）
3. **注册中心机制**（07 篇）
未达前置者，先补：10 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **增量提取**：10 篇已提取的（Registry/Cluster/RPC/Filter/问题）交叉引用
- **实例对照**：my-xhs 无 Dubbo（10 篇已证）——分层架构与三大中心对照
- **诚实标注**：docs 的调用链/URL 示例与 10 篇重复（不重复提取）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Dubbo 分层架构（9 层与关系：核心层/外围层）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：10 篇 KP-02（调用链）
- **来源**：docs §代码架构（各层说明 + 关系说明）
- **需求**：掌握 **Dubbo 九层架构**——docs：Config/Proxy/Registry/Cluster/Monitor/Protocol/Exchange/Transport/Serialize（10 篇只讲了调用链——本篇补分层全貌）
- **自主实现**：若我设计——分层：对外配置（Config）→ 透明代理（Proxy）→ 注册发现（Registry）→ 集群路由（Cluster）→ 监控（Monitor）→ 核心 RPC（Protocol）→ 信息交换（Exchange）→ 网络传输（Transport）→ 序列化（Serialize）
- **参考实现**（docs 九层 + 关系）：**九层职责（docs）**——**Config**（对外配置接口：ServiceConfig/ReferenceConfig 为中心）+ **Proxy**（透明代理：生成 Stub/Skeleton，ProxyFactory）+ **Registry**（注册发现：URL 为中心）+ **Cluster**（多提供者路由/负载均衡，桥接注册中心：Invoker 为中心）+ **Monitor**（调用次数/时间监控：Statistics 为中心）+ **Protocol**（RPC 调用：Invocation/Result 为中心）+ **Exchange**（请求响应模式，同步转异步：Request/Response）+ **Transport**（抽象 mina/netty：Message 为中心）+ **Serialize**（可复用序列化工具）；**关系关键（docs 明确）**——①**Protocol 是核心层**（Protocol+Invoker+Exporter 即可完成非透明 RPC，Invoker 主过程上挂 Filter 拦截点）②**Cluster 是外围概念**（多个 Invoker 伪装成一个——单提供者不需要 Cluster）③**Proxy 封装透明化**（去掉 Proxy RPC 可 Run 只是不透明）④**Remoting 是 Dubbo 协议的实现**（选 RMI 协议则整个 Remoting 不用）——Transport 只做单向消息传输，Exchange 封装 Request-Response 语义⑤**Registry 和 Monitor 不是层是独立节点**（全局概览画法）
- **对比取舍**：**分层架构 vs 扁平**——职责隔离/可插拔 vs 简单——**九层是 Dubbo 可扩展性的骨架**（每层都有扩展接口）
- **测试佐证**：docs §各层说明（九层职责原文）+ §关系说明（5 条关键关系）

### KP-02 领域模型与设计原则（Protocol 服务域/Invoker 实体域/Invocation 会话域 + Microkernel+Plugin/URL）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：10 篇 KP-06（Invoker/Invocation）
- **来源**：docs §领域模型 + §基本设计原则
- **需求**：掌握 **Dubbo 三域模型与两设计原则**——docs：领域三域 + Microkernel+Plugin + URL 统一配置（10 篇 KP-06 只讲了 Invoker/Invocation——本篇补 Protocol 服务域与设计原则）
- **自主实现**：若我设计——三域：服务域（Protocol——Invoker 生命周期管理）/实体域（Invoker——核心模型：本地/远程/集群实现）/会话域（Invocation——方法名参数）；两原则：微内核插件化 + URL 统一配置
- **参考实现**（docs）：**三域（docs 明确）**——**Protocol 是服务域**（Invoker 暴露和引用的主功能入口，负责生命周期管理）；**Invoker 是实体域**（**Dubbo 的核心模型，其它模型都向它靠拢或转换**——代表可执行体，可能是本地/远程/集群实现）；**Invocation 是会话域**（持有调用过程变量：方法名、参数）；**两设计原则（docs）**——①**Microkernel + Plugin 模式**（Microkernel 只负责组装 Plugin，**Dubbo 自身功能也通过扩展点实现——所有功能点可被用户自定义扩展替换**）②**URL 作为配置信息统一格式**（所有扩展点通过 URL 携带配置）
- **对比取舍**：**微内核插件化 vs 硬编码**——全功能可替换 vs 简单——**与 stage-2 21（RPC 微内核）同构思想的 Dubbo 版**
- **测试佐证**：docs §领域模型（三域原文）+ §设计原则（两原则原文）

### KP-03 模块分包（8 模块与分层差异）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §模块分包
- **需求**：掌握 **Dubbo 的 Maven 模块划分**——docs 8 模块与 9 层的映射差异
- **自主实现**：若我设计——按层分包 + 差异调整（Container 独立/Protocol+Proxy 合 rpc/Transport+Exchange 合 remoting/Serialize 放 common）
- **参考实现**（docs 8 模块 + 差异）：**8 模块（docs）**——`dubbo-common`（Util/通用模型）/`dubbo-remoting`（Dubbo 协议实现——RMI 协议不需要）/`dubbo-rpc`（抽象协议+动态代理，**只含一对一调用，不关心集群**）/`dubbo-cluster`（多提供方伪装一个：负载均衡/容错/路由——地址列表静态或注册中心下发）/`dubbo-registry`（注册中心下发地址 + 各种注册中心抽象）/`dubbo-monitor`（调用次数/时间/调用链）/`dubbo-config`（对外 API，隐藏细节）/`dubbo-container`（**Standalone 容器——Main 加载 Spring 启动，无需 Web 容器**）；**分包差异（docs）**——Container 不在层中画出（部署运行服务）/Protocol+Proxy 合 rpc 模块（**单提供者时只这两层即可完成 RPC**）/Transport+Exchange 合 remoting/Serialize 放 common（最大复用）
- **对比取舍**：**分层 vs 分包**——概念层 vs 发布单元——**"单提供者只用 rpc 两层的轻量路径"是 Dubbo 的分层灵活性**
- **测试佐证**：docs §模块分包（8 模块 + 4 差异原文）

### KP-04 三大中心（注册/配置/元数据）【docs 主要内容②】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（注册中心 [过时→Nacos 主流]） | **置信度**：High
- **前置**：07 篇（注册中心）、stage-2 23/24（配置中心）
- **来源**：docs 主要内容② + 架构师发散（docs 正文未展开"三大中心"——分散在 Registry/元数据各节）
- **需求**：掌握 **Dubbo 三大中心的职责分工**——docs 主要内容②：注册中心/配置中心/元数据中心（docs 正文分散，发散整合）
- **自主实现**：若我设计——三中心分工：注册中心（实例地址：谁在哪）、配置中心（运行参数：怎么跑）、元数据中心（接口定义：长什么样）
- **参考实现**（docs 分散内容 + 发散 + my-xhs 对照）：**①注册中心（docs §Registry）**——服务地址注册发现（10/07 篇已提取——ZK 路径模式/Nacos 实现，交叉引用）；**②配置中心**——Dubbo 运行配置（超时/重试等——stage-2 23/24 Nacos 配置已提取）；**③元数据中心**——**接口定义（方法名+参数类型——20 篇泛化调用已提"元数据中心拿接口定义"）**——**服务自省架构（docs 链接）**——mercyblitz 服务自省文章（Cloud-Native 实现：metadata-type=composite——URL 示例实证 10 篇）；**my-xhs 对照**——注册中心（Nacos——07 篇 ✅）+ 配置中心（Nacos——03 篇 ✅）+ 元数据中心（**Dubbo 特有概念——my-xhs 无显式元数据中心**（Feign 接口即契约，编译期绑定）`[现状：接口契约静态化 vs Dubbo 运行时元数据]`）
- **对比取舍**：**三中心分离 vs 合一**——职责清晰 vs 组件数多——**Nacos 兼任注册+配置（07 篇），元数据 Dubbo 独有（泛化调用依赖）**
- **测试佐证**：docs 主要内容② + §Registry（交叉 10/07 篇）+ 20 篇（元数据中心）+ my-xhs（Nacos 实证）

### KP-05 Dubbo 2.x 不足与 IoC 演进（Event Sourcing/SPI Priority/IoC 容器）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：SPI 概念
- **来源**：docs §基本设计原则（2.x 版本不足）
- **需求**：了解 **Dubbo 2.x 的三个演进方向**——docs：事件/SPI 优先级/IoC 容器
- **自主实现**：若我设计——补齐 2.x 不足：①Event Sourcing（事件溯源）②SPI Priority（优先级）③IoC 容器（扩展装配）
- **参考实现**（docs 三条 + 发散）：**①缺 Dubbo 事件（Event Sourcing）**——事件化架构（14 篇事件设计衔接）；**②SPI 取消优先级（Priority）**——扩展排序能力；**③缺 IoC 容器（运行时反射）**——两条路径（docs）：**内存型 IoC 容器**（ID → Dubbo SPI 组件）+ **适配已有实现**（**运行时反射 IoC 容器**：Spring/CDI 容器；**运行时字节码提升 IoC 容器**：Dubbo 内部 @Adaptive 自适应——**SpringExtensionFactory 适配 Spring IoC 容器**）
- **对比取舍**：**自建 IoC vs 适配 Spring**——轻量内存 vs 生态复用（SpringExtensionFactory 路线）
- **测试佐证**：docs §2.x 版本不足（3 条 + IoC 两路径原文）

### KP-06 SPI 扩展与内建实现（RegistryFactory$Adaptive）【docs 主要内容③】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：10 篇 KP-03（Registry）
- **来源**：docs §Dubbo SPI（Registry——与 10 篇重复）+ docs 主要内容③
- **需求**：docs 主要内容③（SPI 扩展机制与内建实现）——**与 10 篇 KP-03 重复（RegistryFactory$Adaptive 动态创建/URL 示例/Registry 五接口/调用链）——交叉引用不重复提取**
- **自主实现**：若我设计——SPI 机制（URL 参数 → Adaptive 动态选择实现 → 扩展点可替换）
- **参考实现**（交叉引用 + 增量）：**10 篇 KP-03（已提取）**——RegistryFactory$Adaptive/Registry 五接口/ZK 路径模式/Nacos 实现/服务自省——不重复；**本篇增量（docs 主要内容③意图 + 发散）**——**SPI 是"Microkernel+Plugin"（KP-02）的实现机制**：URL 参数驱动实现选择（registry=zookeeper → ZookeeperRegistryFactory——10 篇已证）+ **所有扩展点可替换**（docs 设计原则）
- **对比取舍**：**SPI 可替换 vs 固定实现**——生态扩展 vs 简单——**SPI 是 Dubbo 生态的基石**
- **测试佐证**：docs §Dubbo SPI（10 篇交叉引用）+ KP-02（Microkernel+Plugin）

### KP-07 优质代码设计三原则 + RPC Profiles 打包模式
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **来源**：docs §优质代码设计 + §RPC Profiles
- **需求**：了解 **Dubbo 的组件设计规范**——docs：可运维/细粒度/最低依赖 + min/default/all 打包
- **自主实现**：若我设计——组件设计三查：可运维（可配置/可观测：日志跟踪指标）/细粒度（耦合度）/最低依赖（外部组件/系统）
- **参考实现**（docs）：**三原则（docs）**——①**组件可运维（Ops）**：可配置（配置调整行为）+ 可观测（拦截器模式 + 上下文：日志/跟踪/指标）②**组件足够细粒度**：关注耦合度（重合度）③**最低依赖原则**：外部组件依赖/外部系统依赖；**RPC Profiles（docs）**——**min**（仅 RPC：简单注册中心和负载均衡）/ **default**（RPC + Registry + LoadBalancer + Routing + SPI...）/ **all**（default + Monitoring + Metrics + Tracing + Logging）
- **对比取舍**：**按需裁剪（Profiles）vs 全量**——轻量 min vs 完整 all——**"可运维+可观测"是组件设计的第一原则**（03 篇可观测纪律呼应）
- **测试佐证**：docs §优质代码设计（三原则）+ §RPC Profiles（三档原文）

### KP-08 架构对照与现状核对（my-xhs：无 Dubbo——分层/三大中心对照）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01/04
- **来源**：docs 架构 + my-xhs 现状 + 架构师整合
- **需求**：docs 的 Dubbo 架构 ↔ my-xhs（无 Dubbo）的分层/中心对照
- **自主实现**：若我设计——对照维度：分层（RPC 栈 vs HTTP 栈）/三大中心（Nacos 兼任 vs Dubbo 三中心）
- **参考实现**（my-xhs 实证 + 发散）：**分层对照**——my-xhs 无 Dubbo 分层（10 篇已证：Feign 栈——HTTP 层天然含 Transport/Exchange/Protocol/Serialize 于框架内）；**三大中心对照（KP-04 扩展）**——注册（Nacos ✅ 07 篇）/配置（Nacos ✅ 03 篇）/元数据（无显式——Feign 契约编译期绑定）；**SPI 对照**——my-xhs 无 Dubbo SPI（Spring 的 @ConditionalOnClass/自动装配承担类似"可替换"——12 篇 GatewayApplication exclude 实证）；**结论（发散）**——**分层架构与三中心是 RPC 框架的设计范式**（机制时间无关）；my-xhs 的 HTTP 栈用 Spring 生态对应实现
- **对比取舍**：**RPC 栈（分层/三中心）vs HTTP 栈（框架内建）**——治理精细 vs 简单——**架构范式学习（本篇）vs 工程选型（my-xhs Feign）分离**
- **测试佐证**：my-xhs（Nacos 07/03 篇 + Feign 06 篇 + exclude 12 篇）+ docs 架构（KP-01/04）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Dubbo 分层架构（9 层与关系） | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 领域模型与设计原则 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 模块分包（8 模块） | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| 三大中心（注册/配置/元数据） | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| Dubbo 2.x 不足与 IoC 演进 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| SPI 扩展（交叉引用 10 篇） | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 代码设计三原则 + Profiles | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |
| 架构对照与现状核对 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：10 篇（Dubbo 源码——RegistryProtocol/RouterChain/TripleProtocol）+ my-xhs（对照）
- **关键源码**（交叉引用）：10 篇（RegistryProtocol.java:145/272/557、RouterChain.java:120、TripleProtocol.java:62）——本篇不重复引用
- **诚实标注**：**docs §Dubbo SPI（Registry/Cluster/RPC/Filter/问题）与 10 篇大量重复**（类名/URL 示例/调用链相同）→ 按 06 纪律交叉引用不重复提取（本篇头部已声明）；docs 三大中心（主要内容②）正文未集中展开 → KP-04 发散整合；my-xhs 无 Dubbo（10 篇已证）——架构范式学习
- **关联标注**：10 篇（调用链/Cluster/SPI——本篇分层全貌的调用视角）；07 篇（注册中心——三大中心之一）；stage-2 23/24（配置中心）；20 篇（元数据中心——泛化调用）；14 篇（事件设计——2.x 不足 Event Sourcing 衔接）；24 节（Dubbo 生态——本篇衔接）

---

## 五、本节小结（三层次视角）

**需求**：Dubbo 架构设计全景——分层 9 层、领域三域、两设计原则、模块分包、三大中心、SPI、代码设计原则（docs 主要内容①②③）。

**自主实现核心**：若我设计——①九层分层（每层扩展接口）②三域模型（Protocol 服务域/Invoker 实体域/Invocation 会话域）③Microkernel+Plugin + URL 统一配置 ④三大中心分工（注册/配置/元数据）。

**参考实现**：docs（分层/领域/模块/不足/Profiles 新知识）+ 10 篇交叉引用（Registry/Cluster/Filter 重复内容不重提）。**分层机制按 docs 提取，未编造**。

**对比取舍**：知识本体是"**RPC 框架的架构设计范式**"——分层可插拔、三域模型、微内核插件化、URL 统一配置、三中心分工；my-xhs（无 Dubbo）以 HTTP 栈 + Spring 生态对应（Nacos 兼任注册配置 + Feign 静态契约）——**范式学习与工程选型分离**。

**待验证汇总**：
- 三大中心在 Dubbo3 的完整形态（docs 未展开——24 节 Dubbo 生态展开）
- my-xhs 若引入 Dubbo 的元数据中心选型（Nacos 服务定义 vs 独立元数据）
- 服务自省架构细节（docs 链接文章）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 分层架构（RPC 栈） | ❌ 无 Dubbo（10 篇已证）——Feign HTTP 栈 | 现状说明：架构范式学习（机制），工程选型已定 |
| 注册中心 | ✅ Nacos（07 篇） | 无 |
| 配置中心 | ✅ Nacos（03 篇：my-xhs-gateway.yaml 等） | 无 |
| 元数据中心 | ❌ 无显式元数据中心（Feign 契约编译期绑定） | 现状说明：静态契约 vs Dubbo 运行时元数据——引入 Dubbo 才需要 |
| SPI 扩展机制 | ⚠️ 无 Dubbo SPI——Spring @ConditionalOnClass/exclude 承担可替换（12 篇实证） | 现状说明：生态机制不同 |
| 可运维/可观测设计 | ✅ 组件设计对齐（可观测三支柱 03 篇 + 拦截器模式 12 篇） | 无 |

### 差距清单（Dubbo 架构层）

1. **P3**：Dubbo 引入评估（分层/三中心/泛化能力——Feign 满足则维持，10/20 篇已述）
2. **P3**：元数据中心若引入的选型（Nacos 服务定义）
3. **P3**：SPI 思想在本项目的借鉴（可替换组件设计——Spring 生态等价物）

**结论**：23 篇——my-xhs 无 Dubbo（现状说明），三大中心中**注册/配置已由 Nacos 覆盖**，元数据中心为 Dubbo 特有（引入才需要）；本篇以架构范式学习为主（分层/领域/原则时间无关）。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Dubbo 官方架构文档（分层/领域精确）+ 与 10 篇重复部分交叉引用；my-xhs 对照；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：RPC 框架架构设计的完整认知该讲什么

docs 覆盖 Dubbo 架构。完整还该包含：

1. **"分层架构 = 可扩展性的骨架"**（docs 九层 + 发散）：每层有扩展接口（RegistryFactory/Protocol/Cluster...）——**Microkernel+Plugin 的实现基础**；10 篇的调用链（export/refer）就是穿层而过的路径——**分层看职责，链路看流程**
2. **"Invoker 是核心模型"的深意**（docs 领域 + 发散）：本地/远程/集群都是 Invoker——**统一抽象让 Cluster 伪装、Proxy 透明成为可能**（docs 关系说明）；与 10 篇 KP-06（Invoker 本质）呼应
3. **三大中心的演进**（docs ② + 发散）：注册（实例）→ 配置（参数）→ 元数据（接口）——**服务治理的数据面三分**；Nacos 兼任注册+配置（07 篇），元数据是 Dubbo 泛化调用的前提（20 篇）——**"接口契约运行时化"是 RPC 栈与 HTTP 栈（Feign 静态契约）的本质差异**
4. **IoC 演进的方向**（docs 2.x 不足 + 发散）：自建内存 IoC → 适配 Spring（SpringExtensionFactory）——**框架与生态融合的路径**（12 篇 Spring 自动装配对照）
5. **代码设计三原则是通用规范**（docs + 发散）：可运维（可配置/可观测）/细粒度/最低依赖——**适用于任何组件设计**（my-xhs 的 common 27 包依赖治理——01 篇"公共层膨胀"坑对照）
6. **Profiles 按需裁剪**（docs + 发散）：min/default/all——**框架交付的裁剪思维**（与 Spring Boot 自动装配可裁剪——12 篇 exclude 对照）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 九层分层 vs 扁平 | 可插拔 vs 简单（每层扩展接口） |
| Microkernel+Plugin vs 硬编码 | 全可替换 vs 简单 |
| 三中心分离 vs 合一 | 职责清晰 vs 组件数（Nacos 兼任注册配置） |
| 自建 IoC vs 适配 Spring | 轻量 vs 生态（SpringExtensionFactory） |
| 静态契约（Feign）vs 运行时元数据（Dubbo） | 编译期安全 vs 泛化动态（20 篇） |
| min/default/all Profiles | 按需裁剪 vs 全量 |

### 常见坑/反模式

1. **分层不看链路**：只学九层不学调用链（export/refer 穿层）——10 篇交叉引用
2. **元数据与注册数据混一**：接口定义与实例地址同存——三中心职责（docs ②）
3. **SPI 无优先级**：多实现顺序不可控（2.x 不足——docs）
4. **组件不可观测**：无日志/指标/跟踪（docs 三原则第一条）
5. **公共模块膨胀**：最低依赖原则违反（my-xhs common 27 包治理对照）
6. **全量依赖不裁剪**：不需要 Monitoring 也拉 all——Profiles 按需（docs）

### 生态位置

- **stage-3 教学主线**：Dubbo 组（23-24）——**23 架构设计（本篇）** → 24 Dubbo 生态——架构范式 → 生态整合
- **前后篇衔接**：10 篇（调用链/Cluster/SPI——本篇分层视角交叉）→ 本篇（架构全景）；07 篇（注册中心）+ stage-2 23/24（配置中心）→ 本篇三大中心；20 篇（元数据中心）→ 本篇；24 节（生态）
- **与源码提取的关系**：Dubbo 源码（10 篇已证）；my-xhs 对照

**架构师视角结论**：本篇以 **docs 讲 RPC 框架架构设计范式**（九层可插拔/三域模型/微内核+Plugin/URL 统一配置/三中心分工/IoC 演进/代码三原则/Profiles）、**10 篇交叉引用**（Registry/Cluster/Filter 重复内容不重提）——知识本体是"**RPC 框架的架构设计**"；my-xhs（无 Dubbo）以 Nacos（注册+配置）+ Feign（静态契约）对应三中心之二的职责——**范式学习与工程选型分离**；24 节（Dubbo 生态）续。
