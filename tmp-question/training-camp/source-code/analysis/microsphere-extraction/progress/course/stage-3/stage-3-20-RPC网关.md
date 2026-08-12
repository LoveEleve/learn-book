# stage-3 · 第 20 节：第十四节："高并发、高性能与高可用" RPC 网关 — 知识点提取

> 课程：stage-3 三高架构 第 20 节（网关组 19-22 第二篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/20. 第十四节："高并发、高性能与高可用"RPC 网关.md`
> 提取时间：2026-08-12 | 权重：核心（HTTP/2+SSL、Dubbo 泛化调用 RPC 网关、FilteringWebHandler 性能分析与优化）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**my-xhs CachingFilteringWebHandler 是 docs 性能优化思想的变体实现**

---

## 一、本节概览

- **技术域**：HTTP/2 服务器（SSL 证书）、Dubbo 泛化调用（RPC 网关）、gRPC 网关、FilteringWebHandler 性能分析与优化（CachingFilteringWebHandler）
- **维度**：`[工程问题]`（HTTP2/泛化调用/Bean 替换）+ `[分布式问题]`（RPC 网关架构）+ `[性能优化]`（网关性能分析）
- **核心命题**：**RPC 网关两主题 + 网关性能优化**——docs 主要内容：①gRPC 网关（HTTP/2）②Dubbo 泛化网关（无缝整合 gRPC+Triple）；docs 后半是 **FilteringWebHandler 源码级性能分析**（每请求 N+M 过滤器合并开销）与优化实现（CachingFilteringWebHandler）——**my-xhs 有同名变体实现**
- **知识点数**：7 个
- **前置**：19 篇（SCG 架构）、10 篇（Dubbo/Triple）、09 篇（HTTP2）

## 前置条件清单
读者需先掌握：
1. **SCG 过滤器链**（19 篇 KP-08）
2. **Dubbo 泛化与 Triple**（10 篇 KP-02/08）
3. **HTTP/2 与 TLS**（09 篇 KP-08）
未达前置者，先补：19/10 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **源码级**：docs 的 FilteringWebHandler 分析（handle/DefaultGatewayFilterChain 源码）+ my-xhs 同名实现对照
- **实例对照**：my-xhs CachingFilteringWebHandler（直接实现 WebHandler 的变体路径）
- **诚实标注**：泛化调用/gRPC 网关在 my-xhs 未用（Feign 栈）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 HTTP/2 服务器与 SSL 证书（keytool/OpenSSL + server.http2 配置）【docs 主要内容①前提】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：09 篇 KP-08（HTTP2）
- **来源**：docs §Spring Boot HTTP/2 服务器（SSL 证书准备/激活配置）+ my-xhs 对照
- **需求**：掌握 **HTTP/2 的 TLS 前提**——docs 主要内容①（gRPC HTTP/2 网关）的基础：证书生成 + server.http2+ssl 配置（09 篇只提了 enabled:true——本篇补 SSL 面）
- **自主实现**：若我设计——keytool/OpenSSL 生成证书（本地开发）→ `server.http2.enabled + server.ssl`（key-store/password/type/alias）
- **参考实现**（docs 命令 + 配置 + my-xhs 对照）：**keytool 生成（docs）**——JKS：`keytool -genkeypair -alias eureka -keyalg RSA -keysize 4096 -storetype JKS -keystore eureka.jks -validity 3650`；PKCS12 同参数（`-storetype PKCS12`）；**OpenSSL 自签名（docs）**——`openssl req -x509`（localhost 自签名 + subjectAltName=DNS:localhost + keyUsage/extendedKeyUsage）+ `openssl pkcs12 -export`（转 p12——Spring Boot 用 PKCS12）+ minica（本地根证书——更逼真）；**激活配置（docs yaml）**——`server.http2.enabled: true` + `server.ssl`（enabled/key-store（classpath:keystore/localhost.p12）/key-store-password/key-store-type: PKCS12/key-alias）；**机制（发散）**——**HTTP/2 的 h2 模式需要 TLS/ALPN**（09 篇坑清单）；**my-xhs 对照**——`user/application.yml:18` `http2.enabled: true` **但无 ssl 配置**（grep 实证）——**HTTP/2 的 TLS 面缺失 `[差距 P2：h2 需 ALPN/TLS，无 ssl 时走 h2c 明文或降级]`**
- **对比取舍**：**h2（TLS）vs h2c（明文）**——标准/穿透 vs 简单——生产必须 h2（TLS）；my-xhs 需补 ssl
- **测试佐证**：docs §SSL 证书（keytool/OpenSSL 命令全文 + 激活 yaml）+ my-xhs `user/application.yml:18`（http2 无 ssl 实证）

### KP-02 Dubbo 泛化调用（GenericService 无 SDK 调用）【docs 主要内容②基础】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：10 篇（Dubbo）
- **来源**：docs §Apache Dubbo 泛化调用（特性/使用场景）
- **需求**：掌握**泛化调用**——docs 主要内容②的基础：调用方无服务 SDK 也能调用（网关不依赖接口 Artifact）
- **自主实现**：若我设计——实现 GenericService 接口（invoke(method, parameterTypes, args)）——按名称/类型动态调用
- **参考实现**（docs 明确）：**特性（docs）**——"在调用方没有服务方提供的 API（SDK）的情况下，对服务方进行调用，并且可以正常拿到调用结果"；**使用场景（docs 两条）**——①**网关服务**：网关作为所有 RPC 服务的调用端，**不能依赖服务提供方接口 API**（否则每有新服务发布就要改网关代码重新部署）②**测试平台**：输入分组/接口/方法名即可测试（同理由）；**Dubbo Triple vs gRPC 对比（docs）**——gRPC 通讯需两端保存 PB 定义；**Dubbo Triple 调用可以不关心具体 gRPC 消息 PB 定义**（Java 接口编程——10 篇 Triple 衔接）
- **对比取舍**：**泛化（无 SDK）vs 强类型（接口）**——动态灵活 vs 类型安全——**网关场景必须泛化**（新服务零改动）
- **测试佐证**：docs §泛化调用（特性原文 + 两场景）

### KP-03 RPC 网关架构（服务发现+元数据中心+路由规则）【docs 主要内容②】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07 篇（注册中心）、KP-02
- **来源**：docs §设计整合 + 架构师发散
- **需求**：掌握 **RPC 网关的三元信息架构**——docs 主要内容②：GenericService 单接口 vs 网关多服务 → 服务发现 + 元数据中心 + 路由规则
- **自主实现**：若我设计——①注册中心拿 Dubbo 服务列表（服务发现）②元数据中心拿接口定义（方法名+参数类型列表）③路由规则（REST 请求 ↔ Dubbo 方法绑定）→ 泛化调用
- **参考实现**（docs 设计整合 + 发散）：**信息三源（docs 明确）**——**泛化调用元信息**：注册中心（Dubbo Service 列表）+ 元数据中心（接口定义：方法名称 + 参数类型列表）；**路由规则元信息**：Apache Dubbo **2.7.6 起 REST 元信息解析**（REST 请求映射信息与 Dubbo 方法绑定关系——docs 明确）；**机制（发散）**——网关把 REST 请求 → 按路由规则匹配 Dubbo 方法 → 泛化调用（GenericService.invoke）→ 响应转回——**RPC 网关 = HTTP 面 + 泛化调用面 + 元数据驱动的桥**；**对照（发散）**——10 篇的 Dubbo 生态（注册中心 URL 模型——07 篇）与 19 篇的 WebEndpointMapping（端点元数据驱动）同构思想
- **对比取舍**：**泛化网关（动态）vs 强类型直连（Feign）**——新服务零改动 vs 类型安全——my-xhs 用 Feign（06 篇）——RPC 网关是"网关接管 RPC 调用"的高级形态
- **测试佐证**：docs §设计整合（三源 + 2.7.6 REST 元信息解析原文）

### KP-04 gRPC 网关（docs 作业标注 → 发散）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：09/10 篇（HTTP2/Triple）
- **来源**：docs §SCG 整合 gRPC（**作业标注**）+ 架构师发散
- **需求**：理解 **gRPC 网关的特殊性**——docs 主要内容①：为 SCG 整合 gRPC HTTP/2（docs 作业：官方参考示例）
- **自主实现**：若我设计——gRPC 走 HTTP/2 帧——SCG 需透传 h2 帧（非普通 HTTP 转发）
- **参考实现**（docs 作业意图 + 发散）：**docs 作业**——"自行完成官方参考示例（Spring Cloud Gateway and gRPC）"（作业标注——发散补全）；**gRPC 网关机制（发散）**——gRPC 基于 HTTP/2 帧（09 篇）——普通 SCG 路由是 HTTP 1.x 语义 → **gRPC 网关需 HTTP/2 帧级转发**（h2 连接复用/流）；**对照（发散）**——Dubbo Triple（10 篇）同为 HTTP/2 协议——**docs 主旨："无缝整合 gRPC 和 Triple"（RPC 网关统一协议面）**；**my-xhs 对照**——无 gRPC（HTTP 栈 Feign）`[现状说明]`
- **对比取舍**：**帧级转发 vs 应用层转发**——gRPC 需 h2 帧透传 vs 普通 REST 应用层——**网关的协议升级面**
- **测试佐证**：docs §SCG 整合 gRPC（作业标注原文）

### KP-05 FilteringWebHandler 性能分析（每请求 N+M 合并/扩容/排序/chain 嵌套）【docs 性能核心】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：19 篇（SCG 过滤器链）
- **来源**：docs §FilteringWebHandler 请求处理分析（源码级全文）
- **需求**：掌握**网关过滤器链的每请求开销**——docs 源码级分析：N+M 合并/ArrayList 扩容/排序反射/chain 嵌套
- **自主实现**：若我设计——先理解开销点再优化：①每请求合并 globalFilters+route filters ②扩容复制 ③排序反射 ④chain 对象嵌套
- **参考实现**（docs 源码分析）：**handle 流程（docs 源码）**——`combined = globalFilters + gatewayFilters`（N+M）→ 排序 → `DefaultGatewayFilterChain(combined).filter(exchange)`；**四开销点（docs 明确）**——①**new ArrayList + toArray**（`Arrays.copyOf(elementData, size)`——native 复制，解释执行时耗时）②**addAll 扩容**（gatewayFilters unmodifiableList 包装 → toArray → 扩容复制 M+N → 老数组 GC）③**排序 AnnotationAwareOrderComparator**（instanceOf/反射 Annotation/JDK 动态代理/Merged 合成）④**DefaultGatewayFilterChain 瑕疵（docs 明确）**——每请求创建 **M+N+1 个 DefaultGatewayFilterChain**（filter 源码：`new DefaultGatewayFilterChain(this, index+1)` 逐链新建）
- **对比取舍**：**每请求重建 vs 缓存**——简单正确 vs 开销（docs 优化的动机——KP-06）
- **测试佐证**：docs §FilteringWebHandler 分析（handle 源码 + 4 开销点 + chain 源码全文）

### KP-06 网关性能优化模式（Bean 替换 + 缓存 + 事件驱动更新）【docs 优化 + my-xhs 变体实证】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-05、Spring Bean 生命周期
- **来源**：docs §FilteringWebHandler 性能优化（Bean 替换分析/CachingFilteringWebHandler 源码）+ my-xhs 同名变体实证
- **需求**：掌握**框架类的性能优化模式**——docs：替换 FilteringWebHandler（BeanDefinitionRegistryPostProcessor）+ 缓存合并结果（RefreshRoutesResultEvent 更新）
- **自主实现**：若我设计——①缓存合并后的过滤器列表（每请求零重建）②路由刷新事件驱动缓存更新 ③Bean 替换（后处理器）
- **参考实现**（docs 方案 + my-xhs 变体）：**替换途径分析（docs）**——RoutePredicateHandlerMapping 强依赖 FilteringWebHandler（源码实证）→ 途径 1（覆盖 RoutePredicateHandlerMapping——@ConditionalOnMissingBean）**仍需要 FilteringWebHandler Bean** → **仅途径 2 可行**：`FilteringWebHandlerBeanDefinitionRegistryPostProcessor`（**BeanDefinitionRegistryPostProcessor 移除原 BeanDefinition + 注册 CachingFilteringWebHandler**——docs 源码 + Spring 编程技巧 3 条：用 BeanDefinitionRegistryPostProcessor 关联 registry/单例 Bean 不提早初始化/不用 Ordered 尽量靠后）；**CachingFilteringWebHandler 实现（docs）**——MethodHandle 查找 globalFilters 字段 + **缓存全局/路由过滤器列表**（`RefreshRoutesResultEvent` 监听更新——路由刷新后才重建）+ 优化 5 条（减少扩容/事件更新/**HashMap 线程安全（不变时）+ volatile 交换**/chain 不嵌套）；**my-xhs 变体实证（重大）**——`gateway/handler/CachingFilteringWebHandler.java`（注释实证："**缓存合并后的 GatewayFilter 列表，避免每次请求都创建 ArrayList 并排序**"——**docs 优化核心痛点的实现**）：**直接实现 WebHandler（而非继承 FilteringWebHandler——注释实证"因其 DefaultGatewayFilterChain 为 private"）**（**docs 途径 2 的变体路径**——更简洁：不替换 Bean，直接换 WebHandler 实现）+ `ConcurrentHashMap` 缓存（vs docs 的 HashMap+volatile——差异点）+ `RefreshRoutesResultEvent` import（docs 事件更新同思路）
- **对比取舍**：**docs（继承+Bean 替换）vs my-xhs（直接实现 WebHandler）**——框架兼容 vs 简洁——**同目标（缓存合并列表）两条路径**；缓存容器（HashMap+volatile vs ConcurrentHashMap）取舍；**延伸资料（docs §参考资料）**——阿里云《SpringCloud Gateway 在微服务架构下的最佳实践》+ 得物自研 API 网关实践（生产网关架构延伸阅读，不展开提取）
- **测试佐证**：docs §性能优化（途径分析/PostProcessor 源码/CachingFilteringWebHandler 源码/5 优化条）+ my-xhs `CachingFilteringWebHandler.java`（注释 + 实现实证）

### KP-07 性能优化原则（提升性能 + 零成本接入）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **来源**：docs §Spring Cloud 性能优化（原则）+ 发散
- **需求**：掌握**性能优化原则**——docs："提升性能 + 接入成本低（零成本）：配置、代码等修改"
- **自主实现**：若我设计——优化必须"收益/成本"双达标：性能提升且接入零成本（配置级优先，代码级要通用）
- **参考实现**（docs 原则 + 发散）：**docs 两原则**——①提升性能 ②**接入成本低（零成本）：配置、代码等修改**（KP-06 的 Bean 替换方案即"改框架不改进应用"——应用零改动）；**发散**——优化方案的"接入成本"是采纳率的关键（配置优先 → 通用组件 → 应用改造递进）
- **对比取舍**：**配置级（零成本）vs 代码级（通用组件）**——接入易 vs 收益大——docs 的 CachingFilteringWebHandler 是"框架级优化，应用零接入"
- **测试佐证**：docs §性能优化原则（原文）+ KP-06

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| HTTP/2 服务器与 SSL 证书 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| Dubbo 泛化调用（无 SDK） | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| RPC 网关架构（三源） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| gRPC 网关（作业发散） | 分布式问题 | 支撑 | P2 | 🟡 | 有效 | High |
| FilteringWebHandler 性能分析 | 性能优化 | 核心 | P1 | 🔴 | 时间无关 | High |
| 网关性能优化模式（缓存+替换） | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 性能优化原则 | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（CachingFilteringWebHandler 变体实证）+ 10/19 篇交叉引用
- **关键源码**（本次实证）：
  - my-xhs `gateway/handler/CachingFilteringWebHandler.java`——注释实证"**缓存合并后的 GatewayFilter 列表，避免每次请求都创建 ArrayList 并排序**" + "**直接实现 WebHandler（而非继承 FilteringWebHandler，因其 DefaultGatewayFilterChain 为 private）**" + `ConcurrentHashMap` 缓存 + `RefreshRoutesResultEvent` import——**docs 优化思想的变体实现**
  - my-xhs `user/application.yml:18`（http2.enabled 无 ssl）
- **诚实标注**：docs §gRPC 网关为**作业标注**（官方示例链接）→ KP-04 发散；docs 的 CachingFilteringWebHandler（microsphere-spring-cloud-gateway）本地无源码 `[无本地源码：docs 源码片段 + my-xhs 变体实证]`；泛化调用/RPC 网关在 my-xhs 未用（Feign 栈，06 篇）`[现状说明]`；docs 引 Spring Cloud Gateway 2.x 时代源码（机制时间无关）
- **关联标注**：10 篇（Dubbo/Triple——泛化调用的框架基础）；09 篇（HTTP2——本篇补 TLS 面）；19 篇（SCG 架构/过滤器链）；07 篇（注册中心——RPC 网关服务发现）；05 篇（my-xhs 网关超时配置）

---

## 五、本节小结（三层次视角）

**需求**：RPC 网关两主题（gRPC HTTP/2 + Dubbo 泛化）+ 网关性能优化（FilteringWebHandler 分析）。

**自主实现核心**：若我设计——①HTTP/2 必须配 TLS（keytool/OpenSSL 证书）②RPC 网关：服务发现（注册中心）+ 接口定义（元数据中心）+ 路由规则（REST↔方法绑定）→ GenericService 泛化调用 ③网关性能：缓存合并过滤器列表（RefreshRoutesResultEvent 更新）+ Bean 替换零接入。

**参考实现**：docs（SSL 命令/泛化调用/FilteringWebHandler 源码级分析/优化实现）+ **my-xhs CachingFilteringWebHandler 变体实证**（直接实现 WebHandler——docs 优化思想的简洁路径）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**RPC 网关与网关性能**"——泛化调用（网关不依赖 SDK）、三元信息架构、HTTP/2 的 TLS 前提、过滤器链开销分析（N+M/扩容/chain 嵌套）与优化模式（缓存+事件+Bean 替换）；my-xhs 的 CachingFilteringWebHandler 是 docs 优化的工程变体（WebHandler 直实现更简洁）。

**待验证汇总**：
- my-xhs HTTP/2 无 SSL 的实际生效面（h2c 明文 or 降级 HTTP/1.1）
- my-xhs CachingFilteringWebHandler 的缓存更新链路（RefreshRoutesResultEvent 监听完整度）
- RPC 网关（泛化）在 my-xhs 的引入评估（Feign 满足则不需要）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标（升级动作） | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| ① gRPC 网关（HTTP/2） | ❌ 无 gRPC（Feign HTTP 栈） | 现状说明：RPC 网关未引入（Feign 满足） |
| ② Dubbo 泛化网关 | ❌ 无 Dubbo（10 篇已证） | 现状说明：同 ① |
| HTTP/2 激活 | ⚠️ `http2.enabled: true`（09 篇）**但无 ssl 配置**（grep 实证） | **差距 P2**：补 SSL（key-store PKCS12）——h2 需 ALPN/TLS |
| 网关性能优化（缓存过滤器列表） | ✅ **已落地**：CachingFilteringWebHandler（注释实证"避免每次请求都创建 ArrayList 并排序"——docs 优化核心痛点）+ RefreshRoutesResultEvent + ConcurrentHashMap | 无（docs 优化思想的工程实现） |

### 差距清单（RPC 网关层）

1. **P2**：HTTP/2 补 SSL（docs 本篇的直接前提——`server.ssl` PKCS12 配置）
2. **P3**：CachingFilteringWebHandler 缓存更新链路核对（RefreshRoutesResultEvent 监听完整度——docs 的 5 优化条逐项对照）
3. **P3**：RPC 网关（泛化）引入评估（Feign 栈满足当前——决策待定）

**结论**：20 篇——my-xhs 的**网关性能优化已落地**（CachingFilteringWebHandler 变体——docs 优化的工程答案）；**最大差距 = HTTP/2 无 SSL**（P2——h2 的 TLS 前提缺失，docs 本篇主题①的直接基础）。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 RPC 网关 + 性能优化文档（FilteringWebHandler 源码级分析）；gRPC 作业标注；my-xhs 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：RPC 网关与网关性能的完整认知该讲什么

docs 覆盖泛化调用与性能优化。完整还该包含：

1. **"网关不依赖服务 SDK"是 RPC 网关的核心约束**（docs 明确 + 发散）：泛化调用（GenericService）让**新服务发布零网关改动**——RPC 网关与 HTTP 网关（19 篇显式路由）的本质差异；**RPC 网关 = 元数据驱动的桥**（服务发现 + 接口定义 + 路由规则三源）
2. **HTTP/2 与 TLS 的绑定关系**（docs 证书节 + 发散）：h2 标准模式需 ALPN/TLS——**"http2.enabled: true" 无 ssl 是常见假配置**（my-xhs 现状——09 篇坑清单的活例）；**证书管理是生产必修**（keytool/OpenSSL 本地 + 生产 CA/mTLS——08 篇 Triple mTLS 衔接）
3. **每请求的开销是网关性能的第一战场**（docs 源码级 + 发散）：N+M 合并/扩容/排序反射/chain 嵌套——**高频路径的每请求微开销放大成吞吐差距**（02 篇排队理论：等待时间非线性）；**缓存 + 事件驱动更新是标准解**（docs 5 优化条）
4. **框架优化要"零接入"**（docs 原则 + 发散）：Bean 替换（PostProcessor）/缓存都在框架层——**应用零改动采纳**（docs 性能优化原则②）——优化方案的设计约束
5. **两条 Bean 替换路径**（docs + my-xhs 对照）：docs 继承 + BeanDefinitionRegistryPostProcessor 替换 vs **my-xhs 直接实现 WebHandler**（DefaultGatewayFilterChain private 的规避）——**同目标不同路径**（my-xhs 更简洁——不碰 BeanDefinition）
6. **性能与可观测的配合**（发散）：缓存了过滤器链 → **变更可观测性**（RefreshRoutesResultEvent 即"路由变更事件"——19 篇动态路由的同一事件面）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| h2（TLS）vs h2c（明文） | 标准穿透 vs 简单（生产必 h2） |
| 泛化调用 vs 强类型 | 新服务零改动 vs 类型安全（网关必须泛化） |
| 继承+Bean 替换 vs 直接实现 WebHandler | 框架兼容 vs 简洁（my-xhs 变体） |
| HashMap+volatile vs ConcurrentHashMap | 读优化 vs 写安全（docs vs my-xhs） |
| 每请求重建 vs 缓存 | 简单 vs 高频开销（缓存+事件更新） |
| 配置级 vs 代码级优化 | 零接入 vs 收益大 |

### 常见坑/反模式

1. **http2 开了没配 SSL**：h2 需 ALPN/TLS——假配置（my-xhs 现状警示）
2. **网关依赖服务 SDK**：新服务发布要改网关——泛化调用（docs 场景）
3. **忽略每请求微开销**：N+M 合并/扩容/排序在高吞吐放大（docs 源码分析）
4. **缓存不随路由刷新**：路由变更后过滤器缓存过期——RefreshRoutesResultEvent 驱动（docs）
5. **继承链复杂化**：DefaultGatewayFilterChain private 继承受限——直接实现 WebHandler（my-xhs 注释教训）
6. **优化接入成本高**：改应用代码的"优化"采纳率低——框架层零接入（docs 原则）

### 生态位置

- **stage-3 教学主线**：网关组（19-22）——19 API 网关 → **20 RPC 网关（本篇）** → 21/22 Istio——HTTP 面 → RPC 面 → 服务网格
- **前后篇衔接**：10 篇（Dubbo/Triple——泛化框架基础）→ 本篇（泛化网关）；09 篇（HTTP2）→ 本篇（TLS 前提）；19 篇（SCG 过滤器链/动态路由）→ 本篇（性能分析/RefreshRoutesResultEvent）；03 篇（my-xhs 网关 CachingFilteringWebHandler 首见）
- **与源码提取的关系**：my-xhs gateway handler 为核心参考源；microsphere-spring-cloud-gateway `[无本地源码]`

**架构师视角结论**：本篇以 **docs 讲 RPC 网关机制与网关性能源码**（泛化调用/三元信息/FilteringWebHandler 四开销点/Bean 替换+CachingFilteringWebHandler 五优化）、**my-xhs 实证**（CachingFilteringWebHandler 变体——直接实现 WebHandler 的简洁路径）——知识本体是"**RPC 网关与网关性能优化**"；my-xhs 的性能优化已落地，**HTTP/2 的 SSL 面缺失是 P2 差距**（docs 主题①的直接前提）。
