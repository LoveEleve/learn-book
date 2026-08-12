# stage-4 · 第 11 节：第八节：Spring Cloud LoadBalancer 负载均衡多活架构设计与实现 — 知识点提取

> 课程：stage-4 多活架构 第 11 节（负载均衡组 10-12 第二篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/11. 第八节：Spring Cloud LoadBalancer 负载均衡多活架构设计与实现.md`
> 提取时间：2026-08-12 | 权重：核心（LoadBalancer 现代实现——10 篇 Ribbon 的现代对应 + 区域多活）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**microsphere/Spring Cloud 文档（非 Eureka 文档）——Eureka 仅整合场景（docs:3/247-248）**

> **文档形态**：Spring Cloud LoadBalancer 设计文档 + 源码块（389 行）——主要内容：①Eureka AZ 整合 LoadBalancer（场景）②AZ Locator 整合 LoadBalancer（docs:4）③AZ Locator 整合 K8s API Server（docs:5）；**机制本体 = LoadBalancer 的接口/装配/算法/区域 Supplier/@LoadBalanced**（10 篇 Ribbon 的现代实现——docs 顺序衔接）。

---

## 一、本节概览

- **技术域**：Spring Cloud LoadBalancer（接口/默认装配/算法/Supplier 链/@LoadBalanced/RestTemplate 整合）、区域多活（ZonePreference Supplier/AZ Locator 整合）
- **维度**：`[分布式问题]`（负载均衡/区域路由）+ `[工程问题]`（装配/注解）+ `[性能优化]`（算法）
- **核心命题**：**LoadBalancer 的现代实现与区域多活**——docs 三条主线：①核心接口与默认装配（LoadBalancerClient/Supplier 链）②算法与 Reactive 模型（RoundRobin/Random）③区域偏好与 AZ Locator 整合（ZonePreferenceServiceInstanceListSupplier）；**知识本体 = 响应式客户端负载均衡（10 篇组件模型的现代形态）**
- **知识点数**：7 个
- **前置**：10 篇（组件模型）、07 篇（AZ Locator）、04 篇（NamedContextFactory）

## 前置条件清单
读者需先掌握：
1. **客户端 LB 组件模型**（10 篇——四件套骨架）
2. **AZ Locator 抽象**（07 篇——区域感知层）
3. **NamedContextFactory**（04 篇——LoadBalancerClientFactory 底座）
4. **Reactive 基础**（stage-3 13/17——WebFlux/Mono）
未达前置者，先补：10 篇 / 07 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **机制为本**：LoadBalancer 是 Ribbon 的现代对应（docs 顺序衔接——10 篇组件模型映射）
- **源码实证**：全部类名本地命中（spring-cloud-commons/loadbalancer + my-xhs 双重）
- **区域衔接**：ZonePreference Supplier 与 07 篇 AZ Locator 同域（docs 本篇整合）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 LoadBalancer 核心接口与注解（execute + @LoadBalancerClient vs @RibbonClient 同构）【docs §核心接口/§注解】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：10 篇（Ribbon 组件模型）
- **来源**：docs §核心接口（docs:19-38——LoadBalancerClient 源码块）+ §注解（docs:45-122——@LoadBalancerClient 与 @RibbonClient 对照源码）
- **需求**：**LoadBalancer 的统一调用接口**——docs 明确：`LoadBalancerClient extends ServiceInstanceChooser`——**execute 方法决定执行结果**（docs:22——通过 LoadBalancer 选实例执行请求，可做前后动作/指标）
- **自主实现**：若我设计——客户端 LB 接口三要素：选择器（ServiceInstanceChooser）+ 执行器（execute——请求执行 + 前后钩子）+ 客户端注解（@LoadBalancerClient——独立客户端定制）
- **参考实现**（docs 源码块照录 + 本地实证 + 10 篇对照）：**接口（docs:24-38 源码块照录）**——`LoadBalancerClient extends ServiceInstanceChooser`：`<T> T execute(String serviceId, LoadBalancerRequest<T> request)`（docs:37——**选实例 + 执行 + 前后动作**）；**@LoadBalancerClient（docs:48-81 源码块照录）**——`@Import(LoadBalancerClientConfigurationRegistrar.class)` + name/value（@AliasFor）+ configuration（定制 Configuration Class——docs:79）；**@RibbonClient 对照（docs:85-117 源码块照录——10 篇场景）**——**两注解同构**（name/value/configuration 三成员一致——LoadBalancer 是 Ribbon 的现代版）；**核心逻辑（docs:121-122 照录）**——①加载 @LoadBalancerClients 的 defaultConfiguration ②为 name() 客户端加载 configuration 数组；**本地实证**——`LoadBalancerClient.java`/`ServiceInstanceChooser.java` **`[本地实证：spring-cloud-commons `client/loadbalancer/`]`**；RibbonLoadBalancerClient（docs:41——Ribbon 实现）`[无本地源码：Ribbon]`
- **对比取舍**：**@LoadBalancerClient vs @RibbonClient（同构对照）**——现代 vs 历史——**注解契约继承**（LoadBalancer 演进自 Ribbon——10 篇组件模型映射）
- **机制/说明**：LoadBalancerClient 是**"选实例 + 执行请求"的统一入口**（execute 的前后动作钩子——指标/重试）；@LoadBalancerClient 是**独立客户端的配置声明**（defaultConfiguration 全局 + configuration 单客户端——04 篇 NamedContextFactory 子上下文机制的注解入口）
- **测试佐证**：docs:19-122（源码块照录）+ `LoadBalancerClient.java`/`ServiceInstanceChooser.java`（本地实证）

### KP-02 默认装配（RoundRobinLoadBalancer + ServiceInstanceListSupplier 两实现）【docs §默认配置类/§默认 Supplier Bean】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01、04 篇（NamedContextFactory）
- **来源**：docs §LoadBalancerClientConfiguration（docs:125-199——3 个源码块）
- **需求**：**LoadBalancer 的默认装配**——docs 明确：默认 **ReactorLoadBalancer Bean = RoundRobinLoadBalancer**（docs:127——源码块）；**默认 ServiceInstanceListSupplier**（Reactive/Blocking 双实现——docs:140-192）
- **自主实现**：若我设计——默认装配三件：**LB Bean（RoundRobin——每客户端唯一）+ Supplier（实例来源——Reactive/Blocking 条件化）+ Factory（子上下文——LoadBalancerClientFactory）**
- **参考实现**（docs 源码块照录 + 本地实证）：**默认 LB Bean（docs:129-136 源码块照录）**——`@Bean @ConditionalOnMissingBean ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(Environment, LoadBalancerClientFactory)`——`new RoundRobinLoadBalancer(loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class), name)`（docs:134-135——**Supplier 从 Factory 懒加载**）；**唯一性（docs:138）**——每客户端唯一 Bean（来自 Factory 名称关联的子上下文）；**默认 Supplier——Reactive（docs:155-168 源码块照录）**——`@ConditionalOnReactiveDiscoveryEnabled`（@ConditionalOnClass WebClient + spring.cloud.discovery.reactive.enabled 默认 true——docs:147-151）+ `withDiscoveryClient().withCaching().build(context)`（docs:166——**发现 + 缓存**）；**Blocking（docs:184-191 源码块照录）**——`@ConditionalOnBlockingDiscoveryEnabled` + `withBlockingDiscoveryClient().withCaching()`（docs:190）；**LoadBalancerClientFactory（docs:196-199）**——`PROPERTY_NAME = "loadbalancer.client.name"`（类似 Ribbon "ribbon.client.name"——docs:199）；**本地实证**——`RoundRobinLoadBalancer`/`RandomLoadBalancer`/`ServiceInstanceListSupplier`/`LoadBalancerZoneConfig` **`[本地实证：spring-cloud-loadbalancer `core/` + `config/`]`**
- **对比取舍**：**默认装配（RoundRobin + Supplier 缓存——零配置可用）vs 自定义（configuration 覆盖）**——开箱即用 vs 定制——**"默认可用 + 可覆盖"是 Spring 生态装配哲学**
- **机制/说明**：**Supplier 链（builder 模式）**——`withDiscoveryClient().withCaching()` = **"发现 → 缓存"的组合链**（10 篇 ServerList+Filter 的现代形态——可继续 withZone 等扩展——KP-04）；**懒加载 Provider**（getLazyProvider——子上下文就绪后才解析）
- **测试佐证**：docs:125-199（3 源码块照录）+ loadbalancer 核心类（本地实证）

### KP-03 算法与 Reactive 接口（RoundRobin/Random + ReactiveLoadBalancer vs ReactorLoadBalancer）【docs §算法/§ReactiveLoadBalancer】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-02、stage-3 13/17（Reactive）
- **来源**：docs §算法实现（docs:11-15）+ §ReactiveLoadBalancer（docs:203-224）
- **需求**：**LoadBalancer 的算法与响应式模型**——docs 明确：算法两实现（**轮训 Round-Robin-based/随机 Random——docs:13-15 空节标题**）+ **ReactiveLoadBalancer 基于 Project Reactor 的"LoadBalancerClient"**（docs:204——执行结果 Mono<Response<ServiceInstance>>——**最核心类型 ServiceInstance**）
- **自主实现**：若我设计——响应式 LB：算法实现（轮训/随机）+ Mono 返回（非阻塞）
- **参考实现**（docs 照录 + 本地实证 + 命名对照）：**算法（docs:220-224 照录）**——`RandomLoadBalancer`（随机——列表来源 ServiceInstanceListSupplier）/`RoundRobinLoadBalancer`（轮训——同上）**`[本地实证：loadbalancer `core/RandomLoadBalancer` + `core/RoundRobinLoadBalancer`]`**；**接口命名对照（docs:203 标题 vs 源码——标注）**——docs 标题"ReactiveLoadBalancer" **`[本地实证：commons `client/loadbalancer/reactive/ReactiveLoadBalancer`（早期接口）]`** vs docs:131 源码 `ReactorLoadBalancer<ServiceInstance>` **`[本地实证：loadbalancer `core/ReactorLoadBalancer`]`** + `ReactorServiceInstanceLoadBalancer extends ReactorLoadBalancer<ServiceInstance>`（core/——:28 实证）——**两接口并存：Reactive（早期）→ Reactor（响应式主流）**；**执行语义（docs:204 照录）**——泛型决定执行结果（ReactorServiceInstanceLoadBalancer 返回 Mono<Response<ServiceInstance>>）
- **对比取舍**：**轮训（均匀）vs 随机（分散）**——确定性 vs 统计均匀——**默认轮训（RoundRobin——docs:127）**；**Reactive（Mono 非阻塞）vs Blocking（同步）**——省线程 vs 简单——**LoadBalancer 双模型（Reactive/Blocking 条件化——KP-02）**
- **机制/说明**：算法实现 = **"选择逻辑 + Supplier 数据源"分离**（算法只选下标/随机，实例来自 Supplier 链）——**10 篇 IRule 的现代形态**；ReactorLoadBalancer 是响应式核心接口（Mono 语义——背压友好）
- **测试佐证**：docs:203-224（照录）+ loadbalancer core 类（本地实证）

### KP-04 ZonePreferenceServiceInstanceListSupplier（区域偏好——spring 与 my-xhs 双重实证）【docs §区域实现】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07 篇（AZ Locator）、10 篇 KP-04（区域优先）
- **来源**：docs §基于区域实现（docs:240-248）+ 本地双重实证
- **需求**：**LoadBalancer 的区域偏好**——docs 明确：`ZonePreferenceServiceInstanceListSupplier`（docs:240——区域偏好 Supplier）+ **依赖 LoadBalancerZoneConfig**（"spring.cloud.loadbalancer.zone" 决定当前客户端 zone——docs:244-245——**非强依赖**）+ **Eureka 整合**（"eureka.instance.metadata-map.zone" 设置实例区域——docs:248）
- **自主实现**：若我设计——区域偏好 = Supplier 链加一环（zone 过滤——当前 zone 与实例 zone 匹配优先）
- **参考实现**（docs 照录 + 双重实证 + 07 篇衔接）：**区域配置（docs:244-245 照录）**——`spring.cloud.loadbalancer.zone`（当前客户端 zone——非强依赖）；**实例区域来源（docs:247-248 照录——Eureka 场景）**——Eureka 注册 metadata（`eureka.instance.metadata-map.zone`——**注册时元信息带区域——08 篇 attachZone 的 Eureka 版**）；**双重实证**——`ZonePreferenceServiceInstanceListSupplier` **`[本地实证：spring-cloud-loadbalancer `core/ZonePreferenceServiceInstanceListSupplier` + my-xhs `common/zone/loadbalancer/ZonePreferenceServiceInstanceListSupplier`（同名同构——07 篇 KP-05 已证 my-xhs 版）]`**；**07 篇衔接**——my-xhs 版组合 `ServiceInstanceZoneResolver`（区域解析——07 篇 KP-05）——**spring 版 vs my-xhs 版：同机制（区域偏好过滤）**
- **对比取舍**：**区域偏好 Supplier（链中一环）vs 全局过滤器**——可组合 vs 侵入——**Supplier 链的"过滤即组件"设计**（10 篇 ServerListFilter 的现代形态）
- **机制/说明**：**区域偏好的数据链路**——当前 zone（LoadBalancerZoneConfig——配置）+ 实例 zone（metadata——注册时上报）→ Supplier 过滤匹配——**"写面（attachZone/metadata）+ 读面（ZonePreferenceSupplier）"闭环**（08/10 篇机制延续）；**区域不足回退**（ZonePreference 触发条件——07/08 篇保护性失效衔接）
- **测试佐证**：docs:240-248（照录）+ `ZonePreferenceServiceInstanceListSupplier.java`（spring + my-xhs 双重实证）

### KP-05 @LoadBalanced 与 RestTemplate 整合（@Qualifier 派生 + 拦截器注入）【docs §@LoadBalanced/§接口整合】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：Spring 依赖查找/注入
- **来源**：docs §@LoadBalanced（docs:251-331——注解 + RestTemplate 整合源码块）+ §接口整合（docs:336-359）
- **需求**：**RestTemplate 的负载均衡化**——docs 明确：`@LoadBalanced` 是 **@Qualifier 的"派生"注解**（docs:264——依赖查找条件）+ **RestTemplate 整合机制**（LoadBalancerAutoConfiguration——源码块 docs:287-326）
- **自主实现**：若我设计——RestTemplate 负载均衡化三件：**@LoadBalanced 标记（@Qualifier 派生——依赖条件）+ 收集（注入 RestTemplate 列表）+ 拦截器注入（RestTemplateCustomizer）**
- **参考实现**（docs 源码块照录 + 本地实证）：**@LoadBalanced（docs:255-262 源码块照录）**——`@Qualifier` 派生注解（FIELD/PARAMETER/METHOD——docs:255-259）；**依赖查找条件（docs:265-282 照录）**——名称/类型/名称+类型/构造器参数查找 + **依赖处理（DefaultListableBeanFactory#resolveDependency → AutowireCandidateResolver → QualifierAnnotationAutowireCandidateResolver——@Qualifier 注解处理）**（docs:278-282——**@LoadBalanced 作为依赖处理条件**）；**RestTemplate 整合（docs:287-326 源码块照录）**——`LoadBalancerAutoConfiguration`：`@LoadBalanced @Autowired List<RestTemplate>`（收集——docs:293-295）+ `loadBalancedRestTemplateInitializerDeprecated`（**SmartInitializingSingleton——定制回调**——docs:299-308）+ `LoadBalancerInterceptorConfig`（`RestTemplateCustomizer`——**向 RestTemplate 注入 LoadBalancerInterceptor——docs:317-322**）；**三组件分工（docs:329-331 照录）**——restTemplates（收集）/initializer（定制）/customizer（注入拦截器）；**用法（docs:340-358 源码块照录）**——`@LoadBalanced @Bean RestTemplate` + `http://stores/stores`（**虚拟主机名——服务名寻址**——docs:355）；**本地实证**——`LoadBalanced.java`/`LoadBalancerInterceptor.java` **`[本地实证：commons `client/loadbalancer/`]`**
- **对比取舍**：**@LoadBalanced 注解驱动（拦截器注入）vs 手动加拦截器**——声明式 vs 手动——**"声明即负载均衡"（10 篇 Feign 同思路——声明式客户端）**
- **机制/说明**：@LoadBalanced 的本质 = **@Qualifier 派生标记 + 拦截器织入**——标记收集（@Autowired List）+ 织入（RestTemplateCustomizer 注入 LoadBalancerInterceptor）——**"http://服务名/" 虚拟主机名寻址**（拦截器解析服务名 → LoadBalancerClient 选实例 → 替换 host）——**与 Feign 声明式同构（04 篇）**
- **测试佐证**：docs:251-359（源码块照录）+ `LoadBalanced.java`/`LoadBalancerInterceptor.java`（本地实证）

### KP-06 AZ Locator 整合 LoadBalancer（挂载链——07 篇衔接）【docs §整合】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07/08 篇（AZ Locator）、KP-04
- **来源**：docs §AZ Locator 整合 LoadBalancer（docs:362-374）
- **需求**：**AZ Locator 抽象挂载到 LoadBalancer**——docs 明确：同区域优先整合——自定义 @LoadBalancerClients/@LoadBalancerClient 配置类（docs:366-368）+ **挂载链（docs:370-374 照录）**：`ZonePreferenceServiceInstanceListSupplier` → `ZonePreferenceFilter<ServiceInstance>` + `DiscoveryClientServiceInstanceListSupplier` → `DiscoveryClient`
- **自主实现**：若我设计——整合 = 自定义配置类（@LoadBalancerClients）声明 Supplier 链：区域偏好（ZonePreferenceFilter 过滤）+ 发现（DiscoverySupplier）
- **参考实现**（docs 挂载链照录 + 07 篇衔接 + my-xhs 实证）：**挂载链（docs:370-374 照录）**——**ZonePreferenceServiceInstanceListSupplier 依赖 ZonePreferenceFilter<ServiceInstance>（08 篇过滤器——E=ServiceInstance）与 DiscoveryClientServiceInstanceListSupplier（发现——docs:373）**——**"发现 → 区域过滤 → 供给"链**；**自定义配置类（docs:366-368）**——@LoadBalancerClients/@LoadBalancerClient configuration 挂载（KP-01 机制）；**07 篇衔接**——docs 的挂载链 = my-xhs `ZonePreferenceServiceInstanceListSupplier` + `ServiceInstanceZoneResolver` + `ZoneLoadBalancerConfiguration` 的同构实现（07 篇 KP-05/06 实证）——**区域抽象 + LoadBalancer 的整合在 my-xhs 已落地**
- **对比取舍**：**自定义配置类整合（显式挂载）vs 全局自动**——显式可控 vs 自动——**区域偏好是"按需挂载"的链组件（非默认——KP-02 默认无区域）**
- **机制/说明**：整合链 = **"DiscoveryClient（数据）→ ZonePreferenceFilter（区域策略）→ ZonePreferenceSupplier（供给）"**——**08 篇 ZonePreferenceFilter 的 LoadBalancer 挂载点**（10 篇 KP-05 的现代形态——键名契约一致性问题在 ZoneResolver 统一解析下收敛）
- **测试佐证**：docs:362-374（挂载链照录）+ my-xhs zone/loadbalancer（07 篇实证）

### KP-07 现状核对（my-xhs：LoadBalancer 现代栈完整）+ Reactive 阵营【docs §相关内容】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~06
- **来源**：my-xhs 实证 + 架构师整合；**`[跳过：docs:377-389 §相关内容 Reactive 技术阵营（Spring Stack/Project Reactor/Vert.x/Quarkus——空节标题——stage-3 13/17 已覆盖 Reactive]`**
- **需求**：以 LoadBalancer 为尺——my-xhs 的现代实现对照
- **自主实现**：若我设计——核对：LB（自定义——LeastConnections）/区域（ZonePreference Supplier）/@LoadBalanced（Boot 内建）
- **参考实现**（my-xhs 实证 + 交叉）：**自定义 LB ✅**——`LeastConnectionsLoadBalancer`（10 篇 KP-06 实证——**ReactorLoadBalancer 自定义**——KP-03 的"算法实现"面）；**区域偏好 ✅**——`ZonePreferenceServiceInstanceListSupplier` + `ZoneLoadBalancerConfiguration`（KP-04/06 双重实证——**与 spring 版同名同构**）；**@LoadBalanced ✅ Boot 内建**（`[现状：LoadBalancer 栈完整——Ribbon 无（10 篇已证）]`）；**K8s 整合（docs:5 声明）**——`[现状：K8s 未引入——09 篇 KP-06 同]`
- **对比取舍**：**docs 框架（RoundRobin 默认）vs my-xhs（LeastConnections 自定义）**——默认 vs 业务定制——**10 篇四件套的现代完整落地**
- **测试佐证**：my-xhs `LeastConnectionsLoadBalancer`/`ZonePreferenceServiceInstanceListSupplier`（实证）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 核心接口与注解（execute/@LoadBalancerClient） | 分布式问题 | 核心 | P1 | 🟡 | 有效 | High |
| 默认装配（RoundRobin + Supplier 两实现） | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| 算法与 Reactive 接口 | 性能优化 | 核心 | P1 | 🟡 | 有效 | High |
| ZonePreference Supplier（区域偏好） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| @LoadBalanced 与 RestTemplate 整合 | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| AZ Locator 整合 LoadBalancer（挂载链） | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 现状核对（现代栈完整） | 工程问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）——本篇全部命中**：spring-cloud-loadbalancer（RoundRobinLoadBalancer/RandomLoadBalancer/ServiceInstanceListSupplier/ReactorLoadBalancer/ReactorServiceInstanceLoadBalancer/ZonePreferenceServiceInstanceListSupplier/LoadBalancerZoneConfig——`core/` + `config/`）；spring-cloud-commons（LoadBalancerClient/ServiceInstanceChooser/LoadBalanced/LoadBalancerInterceptor/ReactiveLoadBalancer——`client/loadbalancer/`）；my-xhs（ZonePreferenceServiceInstanceListSupplier/LeastConnectionsLoadBalancer——双重实证）
- **关键实证**（本地 grep）：`LoadBalancerClient.java`/`ServiceInstanceChooser.java`（commons client/loadbalancer）；`RoundRobinLoadBalancer`/`RandomLoadBalancer`/`ZonePreferenceServiceInstanceListSupplier`（loadbalancer core——**与 my-xhs 同名同构**）；`ReactiveLoadBalancer`（commons reactive）vs `ReactorLoadBalancer`（core——**docs:203 标题与 :131 源码的接口对照**）
- **诚实标注**：docs 为 **LoadBalancer 设计文档（389 行）**——**源码块 11 个照录核心 7 处**（LoadBalancerClient/@LoadBalancerClient/@RibbonClient 对照/默认 LB Bean/Reactive+Blocking Supplier/@LoadBalanced/RestTemplate 整合/示例——awk 计数实证）；**docs:203 标题"ReactiveLoadBalancer" vs 源码 :131"ReactorLoadBalancer"（两接口并存——早期/响应式主流——标注对照）**；**docs:11-15 算法空节标题（轮训/随机——KP-03 实现面补全）**；**docs:19-21/41/219/238 空节标题（ServiceInstanceChooser/RibbonLoadBalancerClient 场景/ReactorServiceInstanceLoadBalancer/DiscoveryClientServiceInstanceListSupplier）`[跳过：空节标题——机制已在邻近 KP]`**；**docs:377-389 Reactive 阵营空节 `[跳过：stage-3 13/17 已覆盖]`**；Eureka 仅整合场景（docs:3/247-248——metadata-map.zone）
- **关联标注**：10 篇（组件模型——现代映射）；07/08 篇（AZ Locator——区域面）；04 篇（NamedContextFactory——Factory 底座）；09 篇（K8s——docs:5 声明衔接）；stage-3 13/17（Reactive）

---

## 五、本节小结（三层次视角）

**需求**：LoadBalancer 的现代实现与区域多活——接口/装配/算法/区域 Supplier/@LoadBalanced/AZ Locator 整合。

**自主实现核心**：①**Supplier 链（builder）**——"发现 → 缓存 → 区域过滤"可组合（10 篇 ServerList+Filter 的现代形态）②**@LoadBalanced = @Qualifier 派生 + 拦截器织入**（虚拟主机名寻址——声明即负载均衡）③**区域偏好数据链路**（配置当前 zone + metadata 实例 zone → 过滤匹配——写读闭环）。

**参考实现**：docs 源码块 7 处照录 + **全部类名本地命中**（spring-cloud-loadbalancer/commons + my-xhs 双重——本篇源码验证完整）+ 10 篇组件模型映射。

**对比取舍**：知识本体是"**响应式客户端负载均衡 + 区域多活**"——默认装配（RoundRobin+缓存）vs 自定义（configuration）、Reactive（Mono）vs Blocking（双模型条件化）、区域偏好（链中一环）；**LoadBalancer = Ribbon 的现代实现**（docs 顺序——10 篇骨架的现代形态）。

**待验证汇总**：
- 无（本篇类名全部实证——唯一待核：spring 版 ZonePreferenceServiceInstanceListSupplier 与 my-xhs 版的具体差异 `[待验证：实现细节]`）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 默认装配（RoundRobin） | ✅ LoadBalancer 栈（LeastConnections 自定义——10 篇） | 无（自定义替代默认——业务需求） |
| 区域偏好 Supplier | ✅ `ZonePreferenceServiceInstanceListSupplier`（与 spring 版同名同构） | 无 |
| @LoadBalanced/RestTemplate | ✅ Boot/SCA 内建（Feign 为主——04 篇） | 无 |
| AZ Locator 整合 | ✅ `ZoneLoadBalancerConfiguration`（挂载链落地） | 无 |
| K8s 整合（docs:5） | ❌ K8s 未引入（09 篇同） | 现状说明：触发条件驱动 |

### 差距清单

1. **P3**：spring 版 vs my-xhs 版 ZonePreferenceSupplier 差异核对（`[待验证]`——source/ 提取时）
2. **P3**：多规则 LB（LeastConnections 单一 vs 多算法可插拔——触发条件：多场景诉求）

**结论**：11 篇——my-xhs **LoadBalancer 现代栈完整**（LeastConnections + ZonePreference Supplier + @LoadBalanced 内建——10 篇组件模型的现代落地）；无 P1/P2 差距。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 LoadBalancer 设计文档（389 行）——源码块照录 + 全部类名本地实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：LoadBalancer 多活的完整认知该讲什么

docs 是 LoadBalancer 设计文档。完整还该包含：

1. **"LoadBalancer = Ribbon 的响应式继承者"**（docs + 10 篇对照）：注解（@LoadBalancerClient vs @RibbonClient——同构）、Factory（LoadBalancerClientFactory vs SpringClientFactory——同底座 NamedContextFactory）、区域（ZonePreference Supplier vs ServerListFilter——同挂载点）——**一一映射**（10 篇组件模型的现代形态）；**Ribbon 的组件化思想完整保留，实现换成响应式**
2. **"Supplier 链是响应式时代的'列表+过滤+更新'合一体"**（docs + 发散）：`withDiscoveryClient().withCaching().withZonePreference()`——**发现/缓存/区域全是链组件**——10 篇的 ServerList/Filter/Updater 三件套在 Supplier 链中合并为"可组合的供给管道"——**管道化设计是 LoadBalancer 的架构创新**
3. **"@LoadBalanced 的'声明即负载均衡'"**（docs + 发散）：@Qualifier 派生（依赖条件）+ 拦截器织入（LoadBalancerInterceptor——虚拟主机名寻址）——**与 Feign（04 篇）同哲学：声明式客户端**——"http://服务名/" 是 Spring Cloud 的寻址惯例（RestTemplate/WebClient 共用）
4. **"区域多活的 LoadBalancer 面 = 链中一环"**（docs + 07/10 篇衔接）：ZonePreference Supplier = **区域偏好的现代挂载点**（10 篇 ServerListFilter 的响应式版）——**"发现 → 区域过滤 → 算法选择"完整链路**；my-xhs 已落地（ZonePreferenceServiceInstanceListSupplier + ZoneResolver）
5. **"Reactive vs Blocking 双模型的条件化"**（docs + 发散）：@ConditionalOnReactive/BlockingDiscoveryEnabled——**同一 LoadBalancer 抽象双实现**（响应式/阻塞——按依赖自动选择）——**"抽象统一、实现条件化"的 Spring 装配哲学**
6. **"my-xhs 的定制 vs docs 默认"**（发散）：docs 默认 RoundRobin（通用）+ my-xhs LeastConnections（业务定制——最小连接数）——**"默认可用 + 业务可定制"的完整实践**（10 篇 KP-06 延续）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 默认 RoundRobin vs 自定义算法 | 通用 vs 业务（my-xhs LeastConnections） |
| Supplier 链（管道化） vs 组件三件套 | 可组合 vs 分离清晰（响应式创新） |
| Reactive（Mono） vs Blocking | 省线程 vs 简单（双模型条件化） |
| @LoadBalanced 声明式 vs 手动拦截器 | 声明即用 vs 显式控制 |
| 区域偏好链组件 vs 默认无区域 | 按需挂载 vs 零配置 |

### 常见坑/反模式

1. **Ribbon API 照搬**：IRule/ServerList 已过时——LoadBalancer 的 Supplier 链是响应式模型（10 篇教训延续）
2. **Supplier 链顺序错误**：`withDiscoveryClient()` 必须在 `withZonePreference()` 前（数据源先于过滤——docs:370-374 挂载链）
3. **区域配置缺失**：`spring.cloud.loadbalancer.zone` 未配 + metadata 无 zone → 区域偏好失效（静默——10 篇键名坑延续）
4. **@LoadBalanced 忘加**：RestTemplate 无 @LoadBalanced → 虚拟主机名不解析（直接 DNS 失败）
5. **缓存 Supplier 的时效**：withCaching 有 TTL——高频变更服务需调缓存策略（Supplier 链配置）
6. **Reactive/Blocking 混用**：条件注解冲突——依赖环境决定（ReactiveDiscoveryClient 存在时走 Reactive——docs:141）

### 生态位置

- **stage-4 教学主线**：**负载均衡组（10-12 第二篇）**——10 组件模型（Ribbon 场景）→ **11 LoadBalancer（本篇：现代实现 + 区域多活）** → 12 REST Client → 14-15 网关 → 16-19 数据面多活
- **前后篇衔接**：10 篇（组件模型——现代映射）；07/08 篇（AZ Locator——区域面）；04 篇（NamedContextFactory——Factory 底座）；09 篇（K8s——docs:5 声明）；stage-3 13/17（Reactive）
- **与源码提取的关系**：**本篇源码验证完整**（spring-cloud-loadbalancer 全部命中 + my-xhs 双重）——source/ 提取的 LoadBalancer 面可基于本篇

**架构师视角结论**：本篇为 **LoadBalancer 设计文档（389 行）**——核心接口（execute/@LoadBalancerClient）、默认装配（RoundRobin + Supplier 链双实现）、算法与 Reactive 接口、**区域偏好（ZonePreferenceServiceInstanceListSupplier——spring 与 my-xhs 双重实证）**、@LoadBalanced（@Qualifier 派生 + 拦截器织入）、**AZ Locator 整合（挂载链：发现 → 区域过滤 → 供给）**——知识本体是"**响应式客户端负载均衡 + 区域多活**"（10 篇 Ribbon 组件模型的现代形态——docs 顺序衔接）；**全部类名本地命中（本篇源码验证完整）**；my-xhs **LoadBalancer 现代栈完整落地**（LeastConnections + ZonePreference Supplier——区域多活面就绪）。
