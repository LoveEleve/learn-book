# stage-4 · 第 14 节：第十一节：Spring Cloud Gateway 多活架构设计与实现 — 知识点提取

> 课程：stage-4 多活架构 第 14 节（网关组 14-15 第一篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/14. 第十一节：Spring Cloud Gateway 多活架构设计与实现.md`
> 提取时间：2026-08-12 | 权重：核心（SCG 多活整合意图 + SCG Server MVC 新形态——网关面）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**microsphere/Spring Cloud 文档（非 Eureka 文档）——机制 = AZ Locator + SCG（Reactive/MVC 双形态）**

> **文档形态**：SCG 多活设计文档（233 行，java 块 3 个——awk 计数实证）——主要内容 3 条为**多活整合意图**（①AZ Locator 同区域优先/故障转移 ②Zone 标识统一 ③动态配置）；正文 = **SCG Reactive Server 架构**（19 篇交叉）+ **SCG Server MVC 新形态**（RouterFunctionMapping/refresh scope——docs 主体）+ 内容关联（第一期 19 节/第三期 13 节）。

---

## 一、本节概览

- **技术域**：SCG 多活（AZ Locator 整合/Zone 标识统一/动态配置）、SCG Server MVC（RouterFunctionMapping/refresh scope 动态路由）
- **维度**：`[分布式问题]`（网关多活/路由）+ `[工程问题]`（架构/装配）+ `[规范]`（Request/Response API 演进）
- **核心命题**：**网关层的区域多活与动态路由**——docs 三意图：①AZ Locator 实现同区域优先/故障转移（07 篇机制挂网关）②Zone 标识统一（上下游路由规则统一）③动态实时变更（04 篇链）；**知识本体 = 网关区域路由 + SCG Server MVC 的动态路由机制**（docs 正文主体）
- **知识点数**：6 个
- **前置**：07 篇（AZ Locator）、stage-3 19（SCG Reactive——交叉）、04 篇（动态配置）、stage-3 20（FilteringWebHandler）

## 前置条件清单
读者需先掌握：
1. **AZ Locator 抽象**（07 篇——区域感知层）
2. **SCG Reactive 网关**（stage-3 19/20——路由/过滤器/WebHandler）
3. **动态配置事件链**（04 篇——refresh scope/EnvironmentChangeEvent）
4. **WebMVC/WebFlux 双栈**（stage-3 09/13——Servlet/Reactive）
未达前置者，先补：stage-3 19 / 07 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **意图 vs 正文分离**：主要内容 3 条是意图（多活整合）——正文是 SCG 架构基础（Reactive 交叉 + MVC 新形态）
- **源码实证**：Server MVC 类名本地命中（spring-cloud-gateway-server-mvc——写入时验证）
- **动态路由为核心**：refresh scope + DelegatingRouterFunction（docs 正文主体——与 04 篇链衔接）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 SCG 多活整合意图（AZ Locator 同区域优先/故障转移 + Zone 标识统一 + 动态配置）【docs 主要内容】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：07 篇（AZ Locator）、04 篇（动态配置）
- **来源**：docs 主要内容（docs:1-5——3 条意图照录；docs 正文未展开多活整合细节）
- **需求**：**网关层的区域多活**——docs 明确：①基于 AZ Locator API 实现 SCG **同区域优先和故障转移**（docs:3）②SCG **统一抽象 Availability Zone 标识**——服务调用上下游路由规则统一（docs:4）③整合 Microsphere Spring Config——**配置与规则动态实时变更**（docs:5）
- **自主实现**：若我设计——网关多活三件：**区域路由**（Zone 标识 → 路由按区域优先/故障转移）+ **标识统一**（上下游同一 Zone 语义）+ **规则动态化**（配置驱动）
- **参考实现**（docs 意图照录 + 07/04 篇衔接 + 发散）：**①同区域优先/故障转移（docs:3）**——07 篇 ZonePreference 机制挂 SCG 路由（区域偏好 → 就近上游；故障转移 → 跨区域兜底——07 篇保护性失效）；**②Zone 标识统一（docs:4）**——**上下游客服的 Zone 语义统一**（10-12 篇 Zone 元信息 + 08 篇键名契约教训——统一标识防"写读键名不一致"）；**③动态配置（docs:5）**——04 篇事件链（配置 → 事件 → 路由规则重载）——**本篇正文的动态路由机制（KP-04）即此意图的实现**；**docs 正文未展开**（`[空节标注：多活整合细节 docs 未写——正文为 SCG 架构基础]`）
- **对比取舍**：**网关区域路由（中心化——单点统一）vs 客户端区域路由（10-12 篇——分散）**——集中治理 vs 客户端自治——**网关面 = 区域多活的中心化入口**（流量统一收口处）
- **机制/说明**：网关多活的本质 = **"区域策略的中心化挂载"**——流量统一经过网关 → 区域路由/故障转移在入口决策（对比客户端 LB 的分散决策）——**Zone 标识统一是上下游协作的契约**（键名/语义一致——08 篇教训）
- **测试佐证**：docs:1-5（意图照录）+ 07/04 篇（机制交叉）+ `[空节标注]`

### KP-02 SCG Reactive Server 架构（WebFlux/DiscoveryClient/LoadBalancer——19 篇交叉）【docs §Reactive Server】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-3 19/20（SCG Reactive）
- **来源**：docs §SCG Reactive Server（docs:16-42——核心架构/核心设计）+ 内容关联（docs:41-42）
- **需求**：**SCG Reactive 架构面**——docs 明确：核心架构（**Spring WebFlux/DiscoveryClient/LoadBalancer/Spring Boot 配置（YAML——docs:28）/扩展机制（路由：条件/目标——docs:32-34）**）+ 核心设计（Route Predicate——docs:38 空节）+ **内容关联（docs:41-42——第一期 19 节服务网关稳定性 + 第三期 13 节 API 网关）**
- **自主实现**：若我设计——SCG 架构 = WebFlux 运行时 + 服务发现 + 负载均衡 + 路由（条件/目标）
- **参考实现**（docs 照录 + stage-3 19/20 交叉）：**架构四件（docs:20-34 照录）**——WebFlux（Reactive 运行时）/DiscoveryClient（服务发现）/LoadBalancer（负载均衡）/Boot 配置（**YAML——贴近主流**）+ 扩展机制（路由=条件+目标）；**Route Predicate（docs:38 空节标题 `[跳过：机制 stage-3 19 已提取]`）**；**内容关联（docs:41-42 照录）**——第一期 19 节（服务网关稳定性——stage-1 19）+ 第三期 13 节（API 网关——**stage-3 19 交叉——8+ 路由/7 过滤器 my-xhs 实证**）；**my-xhs 衔接**——stage-3 19/20 已深挖（CachingFilteringWebHandler 等）
- **对比取舍**：**Reactive SCG（WebFlux——非阻塞）vs MVC SCG（本篇 KP-03——Servlet 栈）**——**双形态并存**（docs 两节对比——新形态 MVC）
- **机制/说明**：SCG Reactive = **WebFlux + 发现 + LB + 路由**的四件架构（19 篇已提取——本篇为架构面回顾交叉）
- **测试佐证**：docs:16-42（照录）+ stage-3 19/20（交叉）+ my-xhs（19 篇实证）

### KP-03 SCG Server MVC 新形态（WebMVC RouterFunctionMapping——MVC 版网关）【docs §Server MVC】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-3 09（WebMVC）、stage-3 13（WebFlux）
- **来源**：docs §SCG Server MVC（docs:45-100——核心 API/SPI/架构）
- **需求**：**SCG 的 MVC 形态**——docs 明确：**SCG Server MVC 基于 Spring Boot + Spring WebMvc.fn**（docs:46——**Reactive 库不适用**；传统 Servlet 运行时 Tomcat/Jetty——docs:46）；**核心 API**（HandlerFilterFunction（Filter）/HandlerFunction（Handler）/RequestPredicate（Predicate）/GatewayMvcProperties——docs:51-57）；**SPI 扩展点**（FilterSupplier/HandlerSupplier/PredicateSupplier——docs:62-66）；**核心架构**——**基于 Spring Framework WebMVC 5.2+ RouterFunctionMapping**（docs:70）
- **自主实现**：若我设计——MVC 网关 = **WebMvc.fn 函数式路由**（HandlerFunction/FilterFunction/Predicate——Servlet 栈的 RouterFunction 模型）
- **参考实现**（docs 照录 + 本地实证）：**定位（docs:46 照录）**——基于 Boot + WebMvc.fn（**异步/Reactive 库不适用**——Servlet 栈）——**Tomcat/Jetty 传统运行时**；**核心 API 4 件（docs:51-57 照录——空节标题）**——HandlerFilterFunction（Filter）/HandlerFunction（Handler）/RequestPredicate（Predicate）/GatewayMvcProperties（@ConfigurationProperties）；**SPI 3 件（docs:62-66 照录——空节标题）**——FilterSupplier/HandlerSupplier/PredicateSupplier（**组件提供者 SPI**）；**本地实证**——`GatewayMvcProperties`/`GatewayMvcPropertiesBeanDefinitionRegistrar`/`FilterSupplier`/`HandlerSupplier`/`PredicateSupplier` **`[本地实证：spring-cloud-gateway `server-mvc/`（config/filter/handler/predicate 包）——写入时验证]`**；**RouterFunction 双模块（docs:70 标注）**——RouterFunction 在 **spring-webmvc（servlet/function）+ spring-webflux（reactive/function/server）两模块并存**（MVC 版用 webmvc 的）
- **对比取舍**：**MVC SCG（Servlet 栈——传统运行时）vs Reactive SCG（WebFlux——非阻塞）**——传统兼容 vs 响应式——**双形态满足不同部署诉求**（docs 两节对比的意图）
- **机制/说明**：SCG Server MVC = **函数式 WebMVC 网关**（WebMvc.fn——HandlerFunction 模型 + SPI 提供者）——**"MVC 栈也能网关"的新形态**（传统 Servlet 项目平滑接入网关能力）
- **测试佐证**：docs:45-100（照录）+ server-mvc 5 类（本地实证）

### KP-04 动态路由刷新机制（RouterFunctionHolder refresh scope + DelegatingRouterFunction）【docs 核心——§配置定义 RouterFunction】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：04 篇（动态配置/refresh scope）、KP-03
- **来源**：docs §配置定义 RouterFunction（docs:102-177——2 个源码块 + 机制说明）
- **需求**：**配置路由的动态刷新**——docs 明确：`GatewayMvcPropertiesBeanDefinitionRegistrar`（docs:102——注册 RouterFunctionHolder（**scope="refresh" 代理**）+ DelegatingRouterFunction（docs:124-129））；**刷新语义（docs:133-136）**——RouterFunctionHolder 以 refresh scope 代理注册（**具备 ContextRefresher 刷新特性**）；DelegatingRouterFunction 构造器注入被代理 Holder——**ContextRefresher#refresh() 时代理对象重新创建**（基于 Supplier 方法引用）
- **自主实现**：若我设计——配置路由动态化：配置规则 → RouterFunctionHolder（refresh scope——刷新即重建）→ DelegatingRouterFunction（门面——持有代理）→ RouterFunctionMapping 合并
- **参考实现**（docs 源码块照录 + 本地实证 + 04 篇衔接）：**Registrar（docs:103-130 源码块照录）**——`GatewayMvcPropertiesBeanDefinitionRegistrar`：RouterFunctionHolder BeanDefinition（**`this::routerFunctionHolderSupplier`——Supplier 方法引用**——docs:111）+ **`setScope("refresh")` + ScopedProxyUtils.createScopedProxy**（docs:115-118——**refresh scope 代理**）+ DelegatingRouterFunction Bean（"gatewayCompositeRouterFunction"——docs:127-129）；**机制三要点（docs:133-136 照录）**——①Holder 装载配置路由的 RouterFunction 合并对象（docs:133）②**Holder 以 scope="refresh" 代理注册——具备 ContextRefresher 刷新特性**（docs:134）③**ContextRefresher#refresh() → 代理重建（routerFunctionHolderSupplier 重调）**（docs:136）；**holderSupplier（docs:138-168 源码块照录）**——`Binder.get(env).bindOrCreate(GatewayMvcProperties...)`（docs:139）+ routes/routesMap 遍历构建 RouterFunction（docs:145-154）+ 空则 NEVER_ROUTE（docs:157-158）+ withAttribute 装载（docs:164）；**合并（docs:172-177 照录）**——RouterFunctionMapping 合并**自定义 Bean + 配置规则**（自定义 RouterFunction Bean + Holder 的配置规则 → 门面 RouterFunction——docs:173-177）；**本地实证**——`GatewayMvcPropertiesBeanDefinitionRegistrar` **`[本地实证：server-mvc `config/`——2026-08-12 精确化：RouterFunctionHolder（:85 嵌套类 `public static class`）与 DelegatingRouterFunction（:103 嵌套类 `implements RouterFunction<ServerResponse>`）为 Registrar 文件内的嵌套类定义（docs 视作独立类——本地实现为嵌套类）+ `RouterFunctionHolderFactory` 存在——docs 为早期实现快照]`**；**04 篇衔接**——refresh scope + ContextRefresher（04 篇 @RefreshScope vs rebinder 配合——**本篇是"refresh scope 代理路由"的具体应用**——HANDOVER 教训 4 现场延续）
- **对比取舍**：**refresh scope 代理（刷新重建——路由热更新）vs 重启生效**——动态 vs 简单——**路由规则动态化的标准实现**（docs 主要内容③的落地）
- **机制/说明**：动态路由 = **"配置 → refresh scope 代理 → 重建门面"**——RouterFunctionHolder（refresh 代理）被 DelegatingRouterFunction 注入——**ContextRefresher 刷新 → 代理重建（Supplier 重调）→ 新配置路由生效**——**与 04 篇"@RefreshScope 重建 Bean"同机制**（路由面应用）
- **测试佐证**：docs:102-177（2 源码块照录）+ `GatewayMvcPropertiesBeanDefinitionRegistrar`（本地实证）+ 04 篇（refresh scope 交叉）

### KP-05 RouterFunction 合并与 Request/Response API 演进（ROOT 树 + 三代 API）【docs §关联内容】
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §RouterFunction 合并（docs:179-186）+ §关联内容（docs:190-224）
- **需求**：**RouterFunction 的合并形态 + Web API 演进**——docs 明确：RouterFunction 树（**ROOT → A（B/C）/D（E/F）——docs:180-186**）；关联内容（Request/Response API 演进——docs:192-224）
- **自主实现**：若我设计——路由合并 = 树形组合（andOther 连接——根节点分支）
- **参考实现**（docs 照录 + 发散）：**RouterFunction 树（docs:180-186 照录）**——ROOT → A（B/C）/D（E/F）——**RouterFunction::andOther 合并的树形结构**；**API 演进（docs:192-224 照录——三代）**——Spring 2.0（`WebRequest`/2.5.2（`NativeWebRequest`）→ Spring 3.0（`ServerHttpRequest`/`ServerHttpResponse`——http/server 包）→ Spring 5.0（`reactive.ServerHttpRequest`——**响应式化**）→ **WebMVC 5.2+（`servlet.function.ServerRequest/ServerResponse`——函数式）** + WebFlux 5（`reactive.function.server.ServerRequest/ServerResponse`）；**本地实证**——`RouterFunction`（spring-webmvc `servlet/function/` + spring-webflux `reactive/function/server/`——双模块）
- **对比取舍**：**Request/Response API 演进主线**——`WebRequest`（2.0 上下文）→ `ServerHttpRequest`（3.0 HTTP 抽象）→ `reactive`（5.0 响应式）→ **`function`（5.2 函数式 MVC/Flux）**——**"抽象上移 + 响应式化 + 函数式化"三线演进**
- **机制/说明**：RouterFunction 树 = **"配置路由 + 自定义路由"的合并形态**（andOther 连接——门面 RouterFunction）；Request/Response API 演进 = Spring Web 抽象的三代（上下文 → HTTP → 响应式 → 函数式）
- **测试佐证**：docs:179-224（照录）+ `RouterFunction`（webmvc/webflux 双模块实证）

### KP-06 现状核对（my-xhs：Reactive SCG + CachingFilteringWebHandler——MVC 版未用）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~05
- **来源**：my-xhs 实证（stage-3 19/20 交叉）+ 架构师整合
- **需求**：以 SCG 双形态为尺——my-xhs 网关现状
- **自主实现**：若我设计——核对：网关形态（Reactive SCG）/动态路由（Nacos 配置）/区域路由（ZonePreference）
- **参考实现**（my-xhs 实证 + 交叉）：**Reactive SCG ✅**——`spring-cloud-starter-gateway`（stage-3 19 实证——8+ 路由/7 过滤器）+ `CachingFilteringWebHandler`（stage-3 20 实证——WebHandler 直实现变体）；**动态路由 ✅**——路由配置在 Nacos（`my-xhs-gateway.yaml`——stage-3 25 实证）`[现状：配置中心化——刷新链路 P2-5 差距项（HANDOVER-session004）]`；**区域路由 ✅**——ZonePreference Supplier（11 篇——网关转发面）；**MVC SCG ❌ 未用** `[现状：Reactive 版为主——MVC 版为 Servlet 栈诉求（决策待定）]`；**AZ Locator 网关整合 ❌**——`[待验证：my-xhs 网关是否挂 Zone 过滤——ZonePreferenceFilter 在网关面的使用未核]`
- **对比取舍**：**Reactive SCG（当前）vs MVC SCG（Servlet 栈——未用）**——非阻塞主流 vs 传统兼容——**my-xhs 用 Reactive 版（WebFlux 网关——与 WebFlux 运行时一致）**
- **测试佐证**：my-xhs（stage-3 19/20/25 实证）+ `[待验证]` 标注

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| SCG 多活整合意图（3 条） | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | Medium |
| SCG Reactive 架构（19 篇交叉） | 工程问题 | 支撑 | P2 | 🟢 | 有效 | High |
| SCG Server MVC 新形态 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 动态路由刷新（refresh scope） | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| RouterFunction 合并与 API 演进 | 规范 | 支撑 | P3 | 🟢 | 有效 | High |
| 现状核对（Reactive 网关） | 工程问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：spring-cloud-gateway（server-mvc 5 类——GatewayMvcProperties/GatewayMvcPropertiesBeanDefinitionRegistrar/FilterSupplier/HandlerSupplier/PredicateSupplier + FilteringWebHandler（server 模块——stage-3 19/20 已证））；spring-framework（RouterFunction——webmvc `servlet/function/` + webflux `reactive/function/server/` 双模块）
- **关键实证**（本地 grep——写入时验证）：`GatewayMvcProperties.java`/`GatewayMvcPropertiesBeanDefinitionRegistrar.java`（server-mvc `config/`）；`FilterSupplier`（filter/）/`HandlerSupplier`（handler/）/`PredicateSupplier`（predicate/）；`FilteringWebHandler.java`（server `handler/`——docs:95 对比参考）
- **诚实标注**：docs 为 **SCG 多活设计文档（233 行，java 块 3 个 awk 实证）**——主要内容 3 条为**意图**（docs 正文未展开多活整合细节 `[空节标注]`）；**空节标题多处 `[跳过]`**——docs:9（SCG X Multi-Active 整合）/docs:38（Route Predicate——stage-3 19 已提）/docs:51-57（核心 API 4 空节——KP-03 发散）/docs:62-66（SPI 3 空节——KP-03）/docs:94（GatewayDelegatingRouterFunction——docs 仅对比参考 FilteringWebHandler）/docs:228-233（Microsphere 开源工程 2 空节）；RouterFunctionHolder/DelegatingRouterFunction **docs 类名在 Registrar 内引用（本地 `RouterFunctionHolderFactory` 存在——docs 早期实现快照）**；内容关联（docs:41-42——第一期 19 节/第三期 13 节）照录
- **关联标注**：07 篇（AZ Locator——网关整合意图）；04 篇（动态配置——refresh scope 衔接）；stage-3 19/20（SCG Reactive——my-xhs 实证）；stage-3 09/13（WebMVC/WebFlux——双栈）；10-12 篇（区域路由）

---

## 五、本节小结（三层次视角）

**需求**：网关层的区域多活与动态路由——AZ Locator 整合（意图）+ SCG 双形态（Reactive/MVC）+ 动态配置。

**自主实现核心**：①**网关区域多活三件**（区域路由/标识统一/规则动态化）②**MVC 版网关**（WebMvc.fn 函数式——Servlet 栈）③**动态路由 = refresh scope 代理**（Holder 刷新重建 → DelegatingRouterFunction 门面）④**RouterFunction 树合并**（andOther）。

**参考实现**：docs 意图照录 + Server MVC 5 类本地实证 + 3 源码块照录 + stage-3 19/20 交叉（Reactive 面）+ 04 篇衔接（refresh scope）。

**对比取舍**：知识本体是"**网关区域路由与动态路由机制**"——Reactive vs MVC 双形态（部署诉求）、refresh scope 代理（路由热更新——04 篇机制应用）、RouterFunction 树（配置+自定义合并）；**主要内容 3 条为意图**（正文为基础——空节标注）。

**待验证汇总**：
- my-xhs 网关 Zone 过滤使用面（`[待验证]`——ZonePreferenceFilter 网关挂载未核）
- my-xhs 路由刷新链路（P2-5 差距项延续）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| SCG Reactive（19 篇） | ✅ WebFlux 网关（8+ 路由/7 过滤器——stage-3 19） | 无 |
| 动态路由（refresh scope） | ✅ Nacos 配置中心（`my-xhs-gateway.yaml`——stage-3 25） | P2-5：刷新链路确认（差距项） |
| SCG Server MVC | ❌ 未用（Reactive 为主） | 现状说明：Servlet 栈诉求未触发（决策待定） |
| 区域路由（AZ Locator 网关面） | ⚠️ `[待验证]`（ZonePreferenceFilter 网关挂载未核） | P3：网关 Zone 过滤核对 |

### 差距清单

1. **P2**：动态路由刷新链路确认（P2-5 差距项延续——HANDOVER-session004）
2. **P3**：网关 Zone 过滤挂载核对（`[待验证]`）
3. **P3**：MVC SCG 评估（Servlet 栈诉求——决策待定）

**结论**：14 篇——my-xhs **Reactive SCG 网关 + Nacos 配置中心化**（19/25 篇实证）；MVC 版未用（决策待定）；网关 Zone 过滤待核（P3）；无新增 P1 差距。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 SCG 多活设计文档（233 行）——意图照录 + Server MVC 类名实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：SCG 多活的完整认知该讲什么

docs 是 SCG 多活设计文档。完整还该包含：

1. **"网关是区域多活的中心化入口"**（docs 意图 + 发散）：客户端 LB（10-12 篇）分散决策 + 网关（本篇）集中决策——**流量统一收口处做区域路由/故障转移**（就近上游 + 跨区兜底——07 篇保护性失效）——**"入口治理"是区域多活的关键面**（比客户端分散更可控）
2. **"Zone 标识统一 = 上下游协作契约"**（docs:4 + 发散）：SCG 统一抽象 Zone 标识——**路由规则（条件/目标）以 Zone 语义统一表达**——08 篇键名契约教训的"统一标识"答案（写读同键 + 语义一致）——**网关是 Zone 语义的"翻译层"**（上游 Zone 标识 → 下游路由）
3. **"SCG 双形态的演进"**（docs + 发散）：Reactive（WebFlux——2019 起主流）→ **MVC（WebMvc.fn——Spring Cloud 2023.0 起引入的网关新形态）**——**Servlet 栈项目的网关选项**（传统 WebMVC 项目平滑接入网关能力）——**双形态并存满足不同栈诉求**（my-xhs 用 Reactive——与 WebFlux 运行时一致）
4. **"refresh scope 代理 = 路由动态化的标准实现"**（docs:133-136 + 04 篇衔接）：RouterFunctionHolder（refresh 代理）+ DelegatingRouterFunction（门面）——**ContextRefresher 刷新 → 代理重建 → 新路由生效**——04 篇"@RefreshScope 重建 Bean"机制的路由面应用——**HANDOVER 教训 4（@RefreshScope 与 rebinder 配合）的网关现场**
5. **"RouterFunction 树 = 可组合路由模型"**（docs:179-186 + 发散）：andOther 合并（ROOT 树）——**配置路由 + 自定义路由的声明式组合**——函数式路由（WebMvc.fn/WebFlux.fn 双栈共用模型）
6. **"docs 意图 vs 正文的差距"**（docs + 发散）：主要内容 3 条（多活整合）docs 正文未展开——**意图先行、实现后补**的文档形态——提取时意图照录 + 机制从正文/已提取篇补全

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 网关区域路由（中心化） vs 客户端 LB（分散） | 集中可控 vs 客户端自治 |
| Reactive SCG vs MVC SCG | 非阻塞主流 vs Servlet 栈兼容（双形态并存） |
| refresh scope 代理（热更新） vs 重启生效 | 动态 vs 简单（路由规则动态化） |
| Zone 标识统一（契约） vs 各组件自定 | 协作一致 vs 局部自由（08 篇键名教训） |

### 常见坑/反模式

1. **Zone 标识不统一**：上下游各写各的键名/语义——区域路由静默失效（08 篇键名坑延续——docs:4 的"统一抽象"即为此）
2. **refresh scope 误用**：RouterFunctionHolder 必须 refresh scope（docs:115-118）——非 refresh 则 ContextRefresher 不重建（路由不热更新）
3. **网关单点**：网关区域路由集中化 → 网关本身要高可用（02 篇协调层单点教训——网关集群化）
4. **Reactive/MVC 混用**：双形态的运行时不同（WebFlux vs Servlet）——混用导致栈冲突
5. **路由规则无动态化**：静态路由 + 重启生效——区域切换/演练无法实时（docs 主要内容③价值流失）

### 生态位置

- **stage-4 教学主线**：**网关组（14-15 第一篇）**——13 Dubbo → **14 SCG 多活（本篇：意图 + 双形态 + 动态路由）** → 15 网关优化 → 16-19 数据面多活
- **前后篇衔接**：07 篇（AZ Locator——网关整合）；04 篇（动态配置——refresh scope）；stage-3 19/20（SCG Reactive——my-xhs 实证）；stage-3 09/13（WebMVC/WebFlux 双栈）；10-12 篇（区域路由）
- **与源码提取的关系**：server-mvc 5 类本地实证（source/ 提取可基于本篇——spring-cloud-gateway 仓库）

**架构师视角结论**：本篇为 **SCG 多活设计文档（233 行，java 块 3 个）**——主要内容 3 条意图（AZ Locator 同区域优先/故障转移 + Zone 标识统一 + 动态配置——docs 正文未展开）+ **SCG Server MVC 新形态**（WebMvc.fn 函数式——Servlet 栈）+ **动态路由机制**（RouterFunctionHolder refresh scope 代理 + DelegatingRouterFunction——ContextRefresher 刷新重建——04 篇机制的路由面应用）；知识本体是"**网关区域路由与动态路由机制**"；my-xhs **Reactive SCG 网关 + Nacos 配置中心化**（MVC 版未用——决策待定；网关 Zone 过滤待核 P3）。
