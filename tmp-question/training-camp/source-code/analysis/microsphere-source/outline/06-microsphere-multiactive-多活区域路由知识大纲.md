# Microsphere Multiactive 多活区域路由知识大纲（multiactive 触发面）

> 来源：`mapping/06-microsphere-multiactive.md` 16 个 KP 的知识本体提炼
> 性质：**源码侧 outline（L1.5）**——mapping 是文件映射（过程），本大纲是知识本体（结果）
> 组织：按知识维度，非按文件；每个知识点标注来源 KP + 六元元数据 + 现代替代
> 用途：①自学/面试知识图谱（多活/区域路由）②与课程 L1（stage-4 多活）合并成 L3 的源码侧素材
> 覆盖核对：16/16 KP 全部归属（文末核对表）

---

## 一、Zone 自动发现（区域感知源头）[分布式问题]

> **核心命题**：官方 SCL 需要手配 `spring.cloud.loadbalancer.zone`；microsphere 从**云元数据/环境自动发现**——supports/locate 两段式 SPI + 组合探测链。

### 1.1 supports/locate 两段式 SPI [🔴 P1] [时间无关模式]
- **来源**：KP-507（ZoneLocator + AbstractZoneLocator）/ KP-509（DefaultZoneLocator）
- **机制**：**SPI 契约**（supports(Environment) 声明支持性 :20 + locate(Environment) 定位 :27——先探测后定位）；抽象基座（BeanNameAware + Ordered :15）；**兜底定位器**（supports 恒 true + order=20——保证探测链总有结果，属性缺省用 defaultZone——Eureka 兼容）
- **对比**：官方 SCL 单属性 vs 多源探测 SPI
- **my-xhs**：该用没用——多源环境探测

### 1.2 组合定位器（排序+缓存+fast-fail+系统属性回写）[🔴 P1] [时间无关模式]
- **来源**：KP-508（CompositeZoneLocator）
- **机制**：**AnnotationAwareOrderComparator 排序**（:34）+ **volatile 缓存**（:29/:51——一次性定位后直返）+ **fast-fail 两种容错**（默认降级继续 vs 开启失败即抛 :87-89）+ **系统属性回写**（:98——`System.setProperty(CURRENT_ZONE_PROPERTY_NAME)`——**Spring↔非 Spring 全局通道**（与 ZoneContext.getCurrentZone :233 闭环——KP-501 双通道设计自洽））
- **my-xhs**：该用没用——多源探测 + 失败策略

### 1.3 云元数据探测源（AWS 家族）[🔴 P1] [时间无关模式]
- **来源**：KP-517（Ec2/ECS File/ECS V4 三定位器）
- **机制**：**环境变量存在性 = 平台判定**（supports = containsProperty(ECS_CONTAINER_METADATA_FILE/URI_V4) :45-46——非 ECS 环境自动跳过）；**IMDS 端点**（169.254.169.254 链路本地 :24 + HttpUtils.doGet）；**order 链**（ECS File=5 → V4=10 → EC2=15 → Default=20——容器内环境优先）；JSON 解析（ObjectMapper + AvailabilityZone 字段 :37）
- **对比**：国内替代——K8s Downward API/节点标签、各云厂商 metadata
- **my-xhs**：该用没用——云环境自动发现（K8s Downward API 落地）

---

## 二、ZoneContext 全局状态（非 Spring 可达）[分布式问题]

> **核心命题**：Zone 信息要被**非 Spring 代码**访问（Ribbon Filter/Redis/MyBatis/静态工具）——全局单例 + JavaBeans 事件（非 Spring 事件）。

### 2.1 全局单例 + 保守默认值 + 双事件桥接 [🔴 P1] [时间无关模式]
- **来源**：KP-501（ZoneContext）/ KP-510（ZoneContextChangedListener）
- **机制**：**静态单例**（:34 + get()）+ **7 volatile 属性**（:36-48——enabled=true/preferenceEnabled=**false**（安全第一）/readyPercentage=**100**（极保守）/minAvailable=5/disabledZone=null）+ **PropertyChangeSupport 值变才触发**（:195-204——避免重复事件）+ **静态 getCurrentZone 读系统属性**（:233）
- **桥接**：ZoneContextChangedListener——**类名探测多容器**（resolveClass 检测 EnvironmentChangeEvent/ApplicationStartedEvent :72-82——spring 模块无 boot/cloud 依赖支持三者）+ **临时监听器收集变更**（add/remove PropertyChangeListener :147/:157——只上报真实变化）+ 策略映射表（属性名→Consumer :125-133）+ **ORIGINAL_ZONE 回退**（:176——配置设 originalZone 重新定位）+ 属性删除=重置（:230-232）
- **生态呼应**：JavaBeans 事件 ↔ Spring 事件双系统桥接（microsphere-spring KP-214 双钩模式的跨事件模型版）
- **my-xhs**：该用没用——配置变更事件桥接（Nacos 动态配置改路由策略）

### 2.2 单例 Bean 化 + 双通道装配 [🔴 P1] [时间无关模式]
- **来源**：KP-512（ZoneAutoConfiguration）/ KP-513（条件注解家族）
- **机制**：**单例→Bean 适配**（@Bean 返回 ZoneContext.get() :39——全局与容器统一）；**ZoneLocator 双通道收集**（SpringFactories loadFactories :47 + Bean 收集 :50——第三方定位器零配置接入 + 排序组装 Composite）；**条件组合注解**（可用 ⊃ 启用——@ConditionalOnClass 嵌套 @ConditionalOnProperty(matchIfMissing=true)）
- **my-xhs**：该用没用——单例 Bean 化 + SPI 扩展点装配

---

## 三、区域优先路由（多级保护）[分布式问题]

> **核心命题**：官方 SCL 仅"同区过滤"；microsphere 叠加**禁用区/就绪率/最小可用**三重保护（REQ 表实证）。

### 3.1 多级保护过滤算法 [🔴 P1] [时间无关模式]
- **来源**：KP-503（ZonePreferenceFilter）
- **机制**：**七级流程**（null/单元素 → 总开关 → 偏好开关（默认关）→ defaultZone 忽略（大小写不敏感）→ 禁用区剔除（≤1 回退防剔光 :74-77）→ **就绪率保护**（zoneCount*100/entitiesSize 整数百分比 :165——zone 元数据缺失的上游不算 ready）→ **最小可用保护**（<阈值回退全量））
- **与官方对照**：SCL 只按 zone metadata 过滤——无三重保护
- **my-xhs**：该用没用——同区优先路由（同城双活）

### 3.2 LoadBalancer 优化版 Supplier + 装饰链 [🔴 P1] [时间无关模式]
- **来源**：KP-516（CustomizedLoadBalancer* + ZonePreferenceServiceInstanceListSupplier）
- **机制**：**官方同名重写**（extends DelegatingServiceInstanceListSupplier :34——与官方 `org.springframework.cloud.loadbalancer.core.ZonePreferenceServiceInstanceListSupplier` 同名——IDE import 混淆风险）；**builder 装饰链**（withDiscoveryClient().withCaching().with(ZonePreference... ) :64-67——官方扩展点）；**双栈配置**（Reactive/Blocking @Order 193827465/466 :54-72）；**客户端级条件**（LoadBalancerEnvironmentPropertyUtils "configurations"=optimized-zone-preference :97-98）；**默认关闭**（customized=true 才装配——不干扰官方）
- **my-xhs**：该用没用——官方 SCL ZonePreference 可起步，精细策略为增量

### 3.3 Ribbon 集成（过时适配）[🔴 P2] [过时→替代]
- **来源**：KP-518（netflix 族）
- **机制**：Ribbon ServerListFilter 集成（ZonePreferenceServerListFilter 委托过滤）+ **Server 包装 ServiceInstance**（super(scheme,host,port) + setZone :38-41——跨生态桥接）+ DiscoveryClientServerList（发现结果→Ribbon Server 流式映射）+ Eureka PreRegistrationHandler 原生钩子附加 zone（beforeRegistration :30-33）
- **过时**：Ribbon 2.7.18 已过时（Spring Cloud LoadBalancer 替代）——模块整体过时但模式可借鉴
- **my-xhs**：不该用——Ribbon 过时；Eureka metadata 机制可借鉴

---

## 四、注册元数据与消费闭环 [分布式问题]

> **核心命题**：**生产者（注册前附加）→ 消费者（metadata 解析）** 同一属性名契约。

### 4.1 Zone 元数据附加（双框架双实现）[🔴 P1] [时间无关模式]
- **来源**：KP-504（ZoneAttachmentHandler）/ KP-514（ZoneCloudAutoConfiguration + ZoneAttachmentListener）
- **机制**：**attachZone**（:28-42——不可变 Map 防御 try-catch :32-38 + isNotBlank 守卫 :30）；**cloud 侧**（ApplicationListener\<RegistrationPreRegisteredEvent> :23——05 仓库注册事件体系消费——@ConditionalOnBean(EventPublishingRegistrationAspect) 跨仓库条件装配 :49 + @AutoConfigureAfter 值/名混合 :35-42）；**netflix 侧**（Eureka PreRegistrationHandler 原生钩子）——**双路径并存幂等**（历史 17-01 实证）
- **时序风险**：定位失败→区域空→不写 metadata→就绪率受影响（历史 17-07 实证）
- **my-xhs**：该用没用——注册前元数据附加（Nacos 注册前事件）

### 4.2 实例元数据解析（消费者）[🟡 P2] [时间无关模式]
- **来源**：KP-515（CloudServerZoneResolver）/ KP-502（ZoneResolver 泛型）/ KP-506（HttpUtils）
- **机制**：**单例无状态解析器**（INSTANCE :21——metadata.get(ZONE_PROPERTY_NAME) :27——与生产端同名契约闭环）；泛型 ZoneResolver 多实体适配（ServiceInstance/InstanceInfo/Server）
- **my-xhs**：已用——Nacos 服务 metadata 读 zone 同机制

---

## 五、配套与工程面 [工程问题]

### 5.1 常量/工具/条件注解 [🟢 P3] [时间无关模式]
- **来源**：KP-505（ZoneConstants）/ KP-511（ZoneUtils + 事件载体）/ KP-513
- **机制**：**前缀分层属性族**（zone.preference.upstream.disabled-zone 三段式）+ **拼写错误实证**（PREFERENCE_FILER :108——FILER 应为 FILTER——API 稳定约束）；Bean 名常量 + 按名取 Bean；ZoneContextChangedEvent extends ApplicationContextEvent + unmodifiableList 载荷（:36）
- **my-xhs**：不该用——官方 @ConfigurationProperties 覆盖

### 5.2 JDK 原生 HTTP（过时）[🟢 P3] [过时→替代]
- **来源**：KP-506（HttpUtils）
- **机制**：HttpURLConnection 双超时 + try-with-resources + disconnect（:40-47）；非 HTTP 协议静默 null（:38）；info 级日志打印响应（:44——生产隐患）
- **替代**：JDK11 java.net.http.HttpClient / Spring WebClient
- **my-xhs**：不该用——Spring RestTemplate/WebClient 覆盖

---

## 覆盖核对（16/16）

| 维度 | 覆盖 KP | 数 |
|------|---------|:--:|
| 一、Zone 自动发现 | 507,508,509,517 | 4 |
| 二、ZoneContext 全局状态 | 501,510,512,513 | 4 |
| 三、区域优先路由 | 503,516,518 | 3 |
| 四、注册元数据闭环 | 502,504,506,514,515 | 5 |
| 五、配套与工程面 | 505,511 | 2 |

**去重后唯一 KP**：501-518 全部 = **16/16 ✓**（含 KP-513 与 512 并列归属——计数按表内去重）
**无孤儿 KP** ✓
