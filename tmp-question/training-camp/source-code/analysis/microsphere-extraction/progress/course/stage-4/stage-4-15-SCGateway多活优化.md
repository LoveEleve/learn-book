# stage-4 · 第 15 节：第十二节：Spring Cloud Gateway 多活架构优化 — 知识点提取

> 课程：stage-4 多活架构 第 15 节（网关组 14-15 收官）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/15. 第十二节：Spring Cloud Gateway 多活架构优化.md`
> 提取时间：2026-08-12 | 权重：核心（区域化路由优化 + Dubbo 上游探测——网关多活优化面）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**microsphere/Spring Cloud 文档（非 Eureka 文档）——机制 = AZ Locator 网关整合 + Dubbo 元信息**

> **文档形态**：SCG 多活优化文档（162 行，java 块 5 个——awk 计数实证）——主要内容 3 条：①区域化 Web Endpoints 路由规则优化（内存足迹/运行效率）②自动探测 Dubbo 上游（**Triple/gRPC**——stage-3 10/23 交叉）③路由规则统一抽象（**区域优先/灰度路由/全链路区域切换/故障转移四特性内聚**——docs:4）。

---

## 一、本节概览

- **技术域**：SCG 双栈（WebFlux/WebMVC 基础设施）、AZ Locator 网关整合（ServiceInstancePredicate 两方案）、Dubbo 元信息（MetadataService/gRPC Protobuf）
- **维度**：`[分布式问题]`（区域路由/网关多活）+ `[工程问题]`（整合方案/元信息暴露）+ `[性能优化]`（内存足迹/响应速度）
- **核心命题**：**网关多活的三个优化方向**——①区域化路由（内存足迹优化——Web Endpoints 区域化）②Dubbo 上游探测（Triple/gRPC——响应速度）③路由规则统一抽象（四特性内聚——可维护性）；**知识本体 = 网关区域过滤的两种实现方案 + Dubbo 元信息暴露机制**
- **知识点数**：5 个
- **前置**：14 篇（SCG 双形态）、07 篇（AZ Locator）、stage-3 10/23（Dubbo/Triple）

## 前置条件清单
读者需先掌握：
1. **SCG 双形态**（14 篇——Reactive/MVC）
2. **AZ Locator 三件套**（07 篇——ZonePreferenceFilter/ZoneContext/ZoneResolver）
3. **Dubbo/Triple**（stage-3 10/23——Triple 协议/元数据中心）
4. **负载均衡整合**（11 篇——Supplier 链）
未达前置者，先补：14 篇 / 07 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **源码实证**：CustomizedLoadBalancerClientConfiguration/RequestCondition 双模块/WebSocketClient——写入时验证；ServiceInstancePredicate `[未找到]`
- **交叉为主**：Dubbo 元信息（stage-3 23 交叉）、Triple（stage-3 10 交叉）
- **意图 vs 实现**：主要内容 ③（四特性内聚）docs 正文未展开——发散

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 SCG 双栈回顾与基础设施（4.x 双实现 + RequestCondition 双模块）【docs §SCG 回顾/§基础设施】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：14 篇（SCG 双形态）
- **来源**：docs §SCG 回顾（docs:7-19）+ §基础设施（docs:21-49）+ 本地实证
- **需求**：**SCG 双栈的基础设施面**——docs 明确：**SCG 4.x 支持 WebFlux 与 WebMVC 两类实现**（docs:9——配置差异大同小异）；**Spring Framework 5.2 将 WebMVC 与 WebFlux 功能对齐**（docs:10——**@Controller 开发模式 + Functional 开发模式**——docs:12-13）
- **自主实现**：若我设计——双栈基础设施认知：Spring Web（通用——@RequestMapping/@Controller/@RestController——docs:25-27）+ WebMVC（**Servlet 引擎——HTTP 1.1 + WebSocket**——docs:31 + **RequestCondition（servlet.mvc.condition）**——docs:34）+ WebFlux（**Reactor + Netty**——docs:36 + WebSocketClient（reactive.socket.client）——docs:42 + **RequestCondition（reactive.result.condition）**——docs:45）
- **参考实现**（docs 照录 + 本地实证 + 14 篇交叉）：**SCG 4.x 双实现（docs:9）**——14 篇 KP-03 已提取（交叉）；**5.2 功能对齐（docs:10）**——WebMVC/WebFlux 双模式对齐（@Controller + Functional）；**基础设施（docs:21-45 照录）**——Spring Web 通用组件（@RequestMapping/@Controller/@RestController）+ WebMVC（Servlet——HTTP 1.1/WebSocket）+ WebFlux（Reactor/Netty）+ **RequestCondition 双模块（docs:34/45——MVC/Flux 各一）**；**`[跳过：docs:38-39 孤立字样 JMS/JMX（WebFlux 节下无上下文——docs 笔记残留，无知识增量——2026-08-12 穷尽性补标]`**——**`[本地实证：spring-webmvc `servlet/mvc/condition/RequestCondition` + spring-webflux `reactive/result/condition/RequestCondition`——写入时验证]`** + WebSocketClient **`[本地实证：spring-webflux `reactive/socket/client/WebSocketClient`]`**；**核心特性（docs:16-18 空节标题 `[跳过：路由/判断/过滤——stage-3 19 已提取]`）**；**Microsphere SCG（docs:49）**——判断请求直接转发匹配实例（内建负载均衡）
- **对比取舍**：**双栈（WebMVC/WebFlux——5.2 对齐）vs 单一栈**——覆盖两种运行时 vs 简单——**SCG 4.x 的双实现是部署形态的完整覆盖**（14 篇延续）
- **机制/说明**：SCG 双栈基础设施 = **Spring Web 通用 + MVC/Flux 各栈特化**（RequestCondition 双实现——MVC 与 Flux 的条件判断各自实现）——**"通用层 + 双实现"的框架结构**
- **测试佐证**：docs:7-49（照录）+ RequestCondition 双模块/WebSocketClient（本地实证）

### KP-02 SCG 整合 AZ Locator（原生"lb:" + WebEndpointMapping 两方案）【docs §整合 Availability Zones Locator】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：07 篇（AZ Locator）、11 篇（LoadBalancer）
- **来源**：docs §整合 AZ Locator（docs:52-141——4 个源码块）
- **需求**：**SCG 的区域过滤整合**——docs 两条路：①**原生 SCG**——"lb:" 负载均衡 + AZ Locator 与 LoadBalancer 整合（参考 `CustomizedLoadBalancerClientConfiguration`——docs:55）②**Microsphere SCG**——**WebEndpointMapping 特性**：实现 `ServiceInstancePredicate` 接口（docs:58）
- **自主实现**：若我设计——网关区域过滤先定**挂载点**：**LB 面**（实例列表供给时过滤——复用 11 篇 ZonePreference Supplier 链，零新组件）vs **端点面**（网关转发前按实例逐个判定——需要区域判定接口）。**我选 LB 面**（复用 LoadBalancer 生态——CustomizedLoadBalancerClientConfiguration 思路）——端点面留给"需要按请求上下文（exchange）逐实例判定"的特殊场景。若必须端点面：我会设计一个 `boolean test(exchange, instance)` 判定接口（区域相等 → true），实现复用 ZonePreferenceFilter（过滤集合的语义等价于"该实例是否被区域偏好保留"——不需要重写区域逻辑）
- **参考实现**（docs 方案对比 + 本地验证——**简述而非照录**）：**原生整合（docs:54-55）**——"lb:" 组件 + AZ Locator × LoadBalancer 整合——`CustomizedLoadBalancerClientConfiguration` `[本地实证：multiactive-spring-cloud `zone/spring/cloud/loadbalancer/`——即 11 篇 Supplier 链的定制装配]`；**方案一：ServiceInstancePredicate 接口（docs:60-128）**——`boolean test(ServerWebExchange, ServiceInstance)`（docs:77）——**两实现**：简单版（ZoneContext + CloudServerZoneResolver.INSTANCE 直接比较 zone——docs:84-101，docs 标"不推荐"）+ **ZonePreferenceFilter 适配版**（test 委托 filter.filter 非空判断——docs:115-127——**复用 08 篇过滤器**）；**`[未找到：本地 microsphere 生态无 microsphere-spring-cloud-gateway 项目（ls 目录实证）——类名待验证]`**；**方案二（推荐——docs:130-141）**——改造 WebEndpointMappingGlobalFilter（全局过滤器内做区域过滤——统一处理而非逐端点判定）
- **对比取舍**：**端点面（Predicate——按实例判定，粒度细）vs 全局面（GlobalFilter——统一处理，docs 推荐）**——docs 选全局（统一/可维护——docs:130）；**LB 面（复用 LoadBalancer——我选）vs 端点面（自定义接口——docs 方案）**——生态复用 vs 灵活判定——**我的选择与 docs 不同：LB 面零新组件（11 篇已备），docs 端点面需新接口（且本地 microsphere 无实现）**
- **机制/说明**：网关区域过滤的**两个挂载点**——**LB 面**（CustomizedLoadBalancerClientConfiguration——实例列表过滤）vs **端点面**（ServiceInstancePredicate/GlobalFilter——实例选择时过滤）——**区域策略的网关双挂载**（12 篇四形态 + 本篇网关面延续）
- **测试佐证**：docs:52-141（4 源码块照录）+ CustomizedLoadBalancerClientConfiguration/CloudServerZoneResolver（本地实证）+ `[未找到]` 标注

### KP-03 路由规则统一抽象（四特性内聚——区域优先/灰度/切换/故障转移）【docs 主要内容③】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：05 篇（灰度——GrayRouteFilter）、KP-02
- **来源**：docs 主要内容③（docs:4）+ 发散 + 已提取篇交叉
- **需求**：**SCG 路由规则的统一抽象**——docs 明确：统一抽象 SCG 路由规则——**内聚区域优先、灰度路由、全链路区域切换、故障转移等特性**（docs:4）——便于后续开发和理解
- **自主实现**：若我设计——路由规则四特性统一抽象：**区域优先**（ZonePreference——KP-02）+ **灰度路由**（版本/标记分流）+ **全链路区域切换**（区域级故障切换——07 篇保护性失效）+ **故障转移**（跨区域兜底）——**统一为"路由策略模型"**（配置驱动）
- **参考实现**（docs 意图照录 + 发散 + 交叉）：**四特性（docs:4 照录）**——区域优先（KP-02 机制）/灰度路由（**05 篇发布策略——my-xhs GrayRouteFilter 实证**）/全链路区域切换（**07 篇 ZoneContext 动态化 + 08 篇 ZoneContextChangedListener**）/故障转移（**07 篇保护性失效**）；**统一抽象（发散）**——路由规则模型化（区域/灰度/切换/故障——配置声明 + 内聚处理——对比各特性分散实现）；**docs 正文未展开 `[空节标注：主要内容③ docs 无正文——发散]`**
- **对比取舍**：**统一抽象（四特性内聚——可维护）vs 分散实现（各特性独立过滤器）**——内聚清晰 vs 灵活独立——**docs 明确选统一抽象**（"便于后续开发和理解"——docs:4）
- **机制/说明**：路由规则统一抽象 = **"区域/灰度/切换/故障"四特性收敛为统一路由模型**——配置驱动（规则声明）+ 内聚执行（统一处理）——**与 05 篇发布策略三要素（元数据/状态/路由）衔接**（统一模型的构成面）
- **测试佐证**：docs:4（意图照录）+ 05/07/08 篇（交叉）+ `[空节标注]`

### KP-04 自动探测 Dubbo 上游（元信息暴露：MetadataService/gRPC Protobuf）【docs §自动探测 Dubbo】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-3 10/23（Dubbo/Triple/元数据中心）
- **来源**：docs §自动探测 Dubbo 上游服务（docs:142-161）
- **需求**：**SCG 自动探测 Dubbo 上游**——docs 明确：引入 SCG 自动探测 Dubbo 上游服务——集合 Dubbo 多活能力——**提升请求响应速度 + gRPC 处理能力（依赖 Dubbo 3 Triple 协议）**（docs:3）；**Dubbo 元信息暴露**（docs:144——两代：<3.x Java 接口元信息 / >=3.x gRPC 元信息）
- **自主实现**：若我设计——网关探测 Dubbo 上游：**元信息获取**（Dubbo 服务的接口/gRPC 元信息——泛化调用/gRPC 处理的前提）——两种暴露方式
- **参考实现**（docs 照录 + stage-3 交叉）：**<3.x Java 接口元信息（docs:145-149 照录）**——用于**泛化调用**；**Dubbo2.7.5+ MetadataService 获取**目标服务暴露的 Dubbo Java 接口 URL 元信息（docs:147——**stage-3 23 元数据中心交叉**）；Dubbo Spring Cloud 项目：MetadataService URL JSON Encode（docs:148）+ Dubbo 用 ServiceInstance metadata（Map）存储 MetadataService URL 核心信息（**兼容 Dubbo Spring Cloud 方式**——docs:149——**注册中心元信息承载——08 篇 attachZone 同通道**）；**>=3.x gRPC 元信息（docs:151-159 照录）**——发布 Google Protobuf 元信息**两方式**：①**配置中心**（优势：应用透明——docs:155 / 不足：配置客户端与中心需支持推送——docs:156）②**Maven 插件编入 Artifact**（优势：构建期发布——docs:158 / 不足：需了解新 Artifact 坐标——docs:159）；**Triple（docs:3——stage-3 10 交叉：Dubbo3 Triple——HTTP/2+gRPC 兼容协议）**——gRPC 处理能力依赖 Triple
- **对比取舍**：**配置中心（应用透明）vs Maven 插件（构建期发布）**——运行时透明 vs 构建期确定——**两种元信息分发方式的权衡**（docs:154-159 明示优劣势）
- **机制/说明**：自动探测 Dubbo 上游 = **"元信息先行"**——网关要调 Dubbo 服务需先知其接口/协议元信息（泛化调用需接口描述、gRPC 需 Protobuf）——**元信息暴露（配置中心/Artifact）+ Triple 协议 = 网关 × Dubbo 互操作的基础**；MetadataService（<3.x）与 Protobuf 元信息（>=3.x）为两代方案
- **测试佐证**：docs:142-161（照录）+ stage-3 10/23（Triple/元数据中心交叉）

### KP-05 现状核对（my-xhs：Reactive SCG + ZonePreference Supplier——区域化路由面；Dubbo 无）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~04
- **来源**：my-xhs 实证（stage-3 19/20 交叉）+ 架构师整合
- **需求**：以网关优化三方向为尺——my-xhs 网关现状
- **自主实现**：若我设计——核对：区域化路由（ZonePreference Supplier——有）/路由规则统一抽象（灰度 header——部分）/Dubbo 探测（无）
- **参考实现**（my-xhs 实证 + 交叉）：**区域化路由 ✅**——`ZonePreferenceServiceInstanceListSupplier`（11 篇——**LB 面整合（KP-02 原生方案）**）；**路由规则（⚠️ 部分）**——灰度 header 路由（GrayRouteFilter——05 篇）+ 区域（ZonePreference）——**四特性未统一抽象**（`[现状：各特性独立实现——统一抽象为演进项]`）；**Dubbo 探测 ❌**——无 Dubbo（stage-3 10/23 实证——Feign HTTP 栈）`[现状：Triple/gRPC 面未触发]`；**内存足迹优化（docs:2 意图）`[待验证：my-xhs 网关路由内存面未核]`**
- **对比取舍**：**LB 面区域整合（当前——CustomizedLoadBalancerClientConfiguration 思路）vs 端点面（ServiceInstancePredicate——未用）**——复用 LoadBalancer vs 自定义端点——**my-xhs 走 LB 面（11 篇——现代栈）**
- **测试佐证**：my-xhs（stage-3 19/20 + 11 篇实证）+ `[待验证]` 标注

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| SCG 双栈基础设施（RequestCondition） | 工程问题 | 支撑 | P2 | 🟢 | 有效 | High |
| SCG 整合 AZ Locator（两方案） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | Medium |
| 路由规则统一抽象（四特性） | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | Medium |
| 自动探测 Dubbo 上游（元信息） | 分布式问题 | 核心 | P1 | 🟡 | 有效 | High |
| 现状核对（LB 面区域整合） | 工程问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：multiactive-spring-cloud（CustomizedLoadBalancerClientConfiguration——`zone/spring/cloud/loadbalancer/` + CloudServerZoneResolver——08 篇已证）；spring-webmvc/webflux（RequestCondition 双模块）；spring-webflux（WebSocketClient）
- **关键实证**（本地 ls——写入时验证）：`CustomizedLoadBalancerClientConfiguration.java`（multiactive spring-cloud loadbalancer——**docs:55 参考实现命中**）；`RequestCondition.java`（webmvc `servlet/mvc/condition/` + webflux `reactive/result/condition/`——docs:34/45 双实现）；`WebSocketClient.java`（webflux `reactive/socket/client/`）
- **诚实标注**：docs 为 **SCG 多活优化文档（162 行，java 块 5 个 awk 实证）**——**`ServiceInstancePredicate`/`ZonePreferenceServiceInstancePredicate`/`WebEndpointMappingGlobalFilter` `[未找到：本地 microsphere 生态无 microsphere-spring-cloud-gateway 项目（ls 目录实证——microsphere-spring-cloud 仅有 commons/dependencies/openfeign/parent 模块）——docs 类名待验证（早期项目或独立仓库）]`**——**docs 源码块照录（方案一/二——机制照提）**；**主要内容③（四特性内聚）docs 正文未展开 `[空节标注]`**；核心特性空节（docs:16-18）`[跳过：stage-3 19 已提取]`；图 2 张（docs:47-48）`[跳过：图示佐证]`
- **关联标注**：14 篇（SCG 双形态）；07/08 篇（AZ Locator——ZonePreferenceFilter/CloudServerZoneResolver）；11 篇（LB 整合）；05 篇（灰度）；stage-3 10/23（Triple/Dubbo 元数据）；stage-3 19/20（SCG——my-xhs 实证）

---

## 五、本节小结（三层次视角）

**需求**：网关多活的三个优化——区域化路由（内存足迹）、Dubbo 上游探测（响应速度/gRPC）、路由规则统一抽象（可维护性）。

**自主实现核心**：①**网关区域过滤双挂载**（LB 面 CustomizedLoadBalancerClientConfiguration vs 端点面 ServiceInstancePredicate/GlobalFilter）②**路由规则四特性统一**（区域/灰度/切换/故障——内聚模型）③**Dubbo 元信息先行**（<3.x MetadataService vs >=3.x Protobuf——配置中心/Artifact 两分发）。

**参考实现**：docs 源码块 5 个照录（ServiceInstancePredicate 两实现/ZonePreferenceFilter Bean）+ 本地实证（CustomizedLoadBalancerClientConfiguration——docs:55 命中/RequestCondition 双模块）+ `[未找到]` 标注（microsphere-spring-cloud-gateway 项目本地无）+ stage-3 10/23 交叉。

**对比取舍**：知识本体是"**网关区域过滤的两方案 + Dubbo 元信息暴露机制**"——Predicate（端点级）vs GlobalFilter（全局——推荐）、配置中心（透明）vs Artifact（构建期）、LB 面 vs 端点面挂载；**路由规则统一抽象为意图**（正文未展开——发散）。

**待验证汇总**：
- `ServiceInstancePredicate`/`WebEndpointMappingGlobalFilter`（`[未找到]`——本地 microsphere 无 gateway 项目）
- my-xhs 网关路由内存面（`[待验证]`——docs:2 优化意图）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 区域化路由优化（docs:2） | ✅ LB 面区域整合（ZonePreference Supplier——11 篇） | 无（内存足迹面待核 P3） |
| 路由规则统一抽象（docs:4） | ⚠️ 灰度 header（GrayRouteFilter）+ 区域独立 | P3：四特性统一抽象（演进项） |
| Dubbo 上游探测（docs:3） | ❌ 无 Dubbo（stage-3 10/23） | 现状说明：Triple/gRPC 未触发（决策待定） |
| SCG 双栈 | ✅ Reactive（stage-3 19——8+ 路由/7 过滤器） | 无（MVC 版未用——14 篇同） |

### 差距清单

1. **P3**：路由规则统一抽象（区域/灰度/切换/故障四特性内聚——docs:4 意图——演进项）
2. **P3**：Dubbo 上游探测（触发条件：RPC 栈诉求——Triple 面）
3. **P3**：网关路由内存足迹核对（`[待验证]`）

**结论**：15 篇——my-xhs **LB 面区域整合已落地**（11 篇——docs:55 思路对应）；统一抽象/Dubbo 探测为演进项（触发条件驱动）；无 P1/P2 差距。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 SCG 多活优化文档（162 行）——源码块照录 + 本地实证；主要内容③发散；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：SCG 多活优化的完整认知该讲什么

docs 是优化设计文档。完整还该包含：

1. **"网关区域过滤的两个挂载点"**（docs + 发散）：**LB 面**（CustomizedLoadBalancerClientConfiguration——实例列表层过滤——11 篇 Supplier 链）vs **端点面**（ServiceInstancePredicate/GlobalFilter——端点选择层过滤）——**"过滤层决定过滤粒度"**（列表层粗/端点层细）——docs 方案二（GlobalFilter）推荐因"全局统一"（docs:130）
2. **"路由规则统一抽象 = 多活策略的收敛"**（docs:4 + 发散）：区域优先/灰度/切换/故障转移**四特性内聚为统一路由模型**——**"策略模型化"是现代网关的方向**（对比分散过滤器链）——配置驱动（规则声明）+ 内聚执行——**my-xhs 灰度/区域独立实现 → 统一抽象为演进路径**（05/07 篇机制衔接）
3. **"网关 × Dubbo = 元信息先行"**（docs:142-161 + 发散）：网关探测 Dubbo 上游需**接口/gRPC 元信息**——两代方案（MetadataService URL——注册中心 metadata 承载 vs Protobuf——配置中心/Artifact）——**"元信息分发"是跨栈互操作的通用问题**（08 篇 attachZone 同通道——注册中心 metadata 是标准载体）
4. **"Triple 是网关 × Dubbo 的协议桥梁"**（docs:3 + stage-3 10 交叉）：Dubbo3 Triple（HTTP/2 + gRPC 兼容）——**网关（HTTP 栈）无需 Dubbo 协议即可调 Dubbo 服务**（gRPC 处理能力）——**协议统一让网关天然接 Dubbo 上游**
5. **"优化意图 vs 实现缺失"**（docs + 发散）：主要内容 3 条为意图（docs 正文只有基础+方案）——**ServiceInstancePredicate 等类本地 microsphere 无**（gateway 项目缺失）——**意图先行、实现待 source/ 核对**（08 篇同类——docs 与本地仓库的版本差异常态化）
6. **"内存足迹优化的本质"**（docs:2 + 发散）：区域化 Web Endpoints 路由规则——**路由规则的区域化 = 减少无效路由匹配/实例候选**（内存/计算足迹↓）——**区域感知的"资源优化"面**（不只路由正确性，还有效率）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| Predicate 接口（端点级） vs GlobalFilter 改造（全局） | 扩展点 vs 全局统一（docs 推荐方案二） |
| LB 面 vs 端点面挂载 | 列表层过滤 vs 端点层过滤（粒度差异） |
| 配置中心（透明） vs Maven 插件（构建期） | 运行时透明 vs 构建期确定（docs 明示优劣势） |
| 统一抽象（内聚） vs 分散实现 | 可维护 vs 灵活（docs 选统一——docs:4） |

### 常见坑/反模式

1. **区域过滤两层都做**：LB 面 + 端点面重复过滤——粒度选择要明确（避免双重过滤开销）
2. **统一抽象过度**：四特性硬耦合一个模型——特性间独立性丧失（内聚 vs 灵活的平衡）
3. **Dubbo 元信息缺失**：网关探测不到上游（无元信息）——泛化调用/gRPC 全挂（元信息先行）
4. **Triple 依赖遗漏**：gRPC 处理依赖 Triple（docs:3）——版本不符则协议不兼容
5. **路由规则分散**：区域/灰度/切换各写各的过滤器——维护困难（docs:4 要解决的反模式）
6. **docs 类名照搬**：ServiceInstancePredicate 本地 microsphere 无——照搬编译失败（以本地为准）

### 生态位置

- **stage-4 教学主线**：**网关组（14-15 收官）**——14 SCG 多活 → **15 SCG 优化（本篇：区域化/Dubbo 探测/统一抽象——网关组收官）** → 16-17 MySQL 多活 → 18-19 Redis 多活
- **前后篇衔接**：14 篇（SCG 双形态）；07/08 篇（AZ Locator）；11 篇（LB 整合）；05 篇（灰度）；stage-3 10/23（Triple/Dubbo 元数据）；stage-3 19/20（SCG——my-xhs 实证）
- **与源码提取的关系**：ServiceInstancePredicate 等 `[未找到]`——source/ 提取核对（microsphere 无 gateway 项目）；CustomizedLoadBalancerClientConfiguration 实证（multiactive）

**架构师视角结论**：本篇为 **SCG 多活优化文档（162 行，java 块 5 个）**——三优化方向：**区域化路由**（LB 面 CustomizedLoadBalancerClientConfiguration 实证 vs 端点面 ServiceInstancePredicate `[未找到]`——两方案对比）、**Dubbo 上游探测**（元信息先行：<3.x MetadataService / >=3.x Protobuf 两分发——stage-3 10/23 交叉）、**路由规则统一抽象**（四特性内聚——意图发散）；知识本体是"**网关区域过滤的两方案 + Dubbo 元信息暴露机制**"；my-xhs **LB 面区域整合已落地**（统一抽象/Dubbo 探测为演进项）；**网关组（14-15）收官**。
