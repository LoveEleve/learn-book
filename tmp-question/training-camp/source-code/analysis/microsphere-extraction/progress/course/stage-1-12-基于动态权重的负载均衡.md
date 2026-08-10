# stage-1 · 第 12 节：基于动态权重的负载均衡实现 — 知识点提取

> 课程：stage-1 服务治理 第 12 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/12. 第十二节：基于动态权重的负载均衡实现.md`
> 提取时间：2026-08-09 | 权重：核心（负载均衡算法是服务治理核心）
> **参考实现说明**：docs 用 Netflix Ribbon/Servo，按方法论 04 均 **已过时(Netflix OSS)** → 参考实现以 Spring LoadBalancer + Micrometer 为主。

---

## 一、本节概览

- **技术域**：动态权重负载均衡算法（按响应时间/权重随机选择）+ 监控指标(Netflix Servo)
- **维度**：`[分布式问题]`（负载均衡算法）+ `[性能优化]`（监控指标）
- **核心命题**：如何实现"动态权重"负载均衡——按实例实时权重(如响应时间)随机选择，权重动态调整
- **知识点数**：8 个
- **前置**：负载均衡概念、Spring Cloud LoadBalancer、注册中心、监控指标

## 前置条件清单
读者需先掌握：
1. **负载均衡概念**（轮询/随机/权重）
2. **Spring Cloud LoadBalancer**（ReactorServiceInstanceLoadBalancer）
3. **监控指标**（响应时间/权重）
4. **服务注册发现**（实例列表）
未达前置者，先补：负载均衡入门 + 第 11 节监控指标

## 掌握度
目标读者：**本人（读源码多，Spring 熟悉）** — 已确认
讲解策略：负载均衡算法 + LoadBalancer 直接讲（你熟悉）；补权重算法的概率原理

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 动态权重负载均衡的需求与本质
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：负载均衡概念
- **需求**：不用轮询(均分)，而按实例权重(性能/响应时间)分配流量，权重可动态调整
- **自主实现**：维护每个实例权重 → 按权重概率随机选择 → 根据实时指标动态更新权重
- **参考实现**：Ribbon `WeightedResponseTimeRule`（按响应时间算权重，随机选）；现代 Spring LoadBalancer 可自定义
- **对比取舍**：权重负载均衡比轮询更"智能"，但需指标 + 权重计算；"动态"指权重随指标变化
- **待验证**：现代 LoadBalancer 动态权重实现

### KP-02 监控指标数据源类型（GAUGE/COUNTER/INFORMATIONAL）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：监控指标
- **需求**：理解监控指标的类型（采样/计数/信息）
- **自主实现**：区分三类指标——瞬时值(GAUGE)/累计计数(COUNTER)/非数值信息(INFORMATIONAL)
- **参考实现**：Netflix Servo `DataSourceType`——GAUGE(可采样数值，如当前连接数)/COUNTER(事件递增，转为变化率)/INFORMATIONAL(非数值，仅 JMX 查看)
- **对比取舍**：COUNTER 需两次采样算变化率(rate)，GAUGE 直接报告
- **关联 microsphere**：`[待验证]` microsphere-observability 指标类型

### KP-03 权重响应时间负载均衡算法（WeightedResponseTimeRule 核心）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、概率
- **需求**：按响应时间(反比)算权重，响应慢的实例分到更少流量
- **自主实现**：响应时间短的权重高 → 按权重区间随机选
- **参考实现**（docs 算法）：4 端点 A(wt=10)/B(30)/C(40)/D(20)，生成 1~100 随机数，按累计区间分配——1-10→A，11-40→B，41-80→C，81-100→D；**统计不足时回退 RoundRobinRule**
- **对比取舍**：核心是"**按权重累计区间 + 随机数**"概率选择；权重由响应时间动态算出
- **测试佐证**：docs 给出完整权重区间示例

### KP-04 Ribbon 负载均衡规则接口（IRule 及其实现）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[过时→LoadBalancer]` | **置信度**：High
- **前置**：KP-03
- **需求**：定义可插拔的负载均衡策略
- **自主实现**：定义 IRule 接口，各实现(随机/轮询/权重)可替换
- **参考实现**：Ribbon `IRule`——RandomRule(随机)/RoundRobinRule(轮询)/WeightedResponseTimeRule(权重响应时间)
- **对比取舍**：**策略模式**——规则可插拔；现代 LoadBalancer 用 ReactorServiceInstanceLoadBalancer 类似
- **过时说明**：Ribbon 已过时(Netflix OSS)，现代 Spring LoadBalancer 实现等价策略

### KP-05 服务列表管理与更新（ServerList/ServerListUpdater）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[过时→LoadBalancer]` | **置信度**：High
- **前置**：注册中心、KP-04
- **需求**：负载均衡器需要最新的服务实例列表
- **自主实现**：从注册中心获取实例列表 + 定期更新
- **参考实现**：Ribbon `ServerList`（getInitialListOfServers/getUpdatedListOfServers）+ `ServerListUpdater`（PollingServerListUpdater 30s 更新）；DiscoveryEnabledNIWSServerList 整合 DiscoveryClient
- **对比取舍**：服务列表动态更新是负载均衡的基础；现代用 ServiceInstanceListSupplier
- **过时说明**：Ribbon 过时，现代 LoadBalancer 用 ServiceInstanceListSupplier（第 11 节）
- **待验证**：现代 LoadBalancer 实例列表更新

### KP-06 Ribbon 与 OpenFeign 整合
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[过时→LoadBalancer]` | **置信度**：High
- **前置**：OpenFeign、KP-04
- **需求**：Feign 调用走负载均衡
- **自主实现**：Feign 客户端请求经 LoadBalancer 选实例
- **参考实现**：`LoadBalancerFeignClient`（OpenFeign Client 接口 + LoadBalancer）；@FeignClient 指向服务传给 Ribbon 上下文；注意 Feign 与 Ribbon 各自超时设置
- **对比取舍**：Feign 通过 LoadBalancer 实现客户端负载均衡；超时需协调(Feign 用 Ribbon ReadTimeout/ConnectTimeout)
- **过时说明**：Ribbon 过时，现代 spring-cloud-loadbalancer 整合 Feign

### KP-07 @RibbonClient / 独立上下文
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[过时→LoadBalancer]` | **置信度**：Medium
- **前置**：Spring 上下文
- **需求**：每个服务(客户端)有独立负载均衡配置
- **自主实现**：为每个目标服务创建独立 Spring 上下文
- **参考实现**：`@RibbonClient` 指向目标服务，@FeignClient 针对每个服务创建独立应用上下文（独立配置/网络/序列化）；RibbonClientConfiguration / PropertiesFactory
- **对比取舍**：独立上下文隔离各服务配置；现代 LoadBalancer 用 NamedContextFactory 类似
- **待验证**：现代 LoadBalancer 独立上下文

### KP-08 监控指标上报（基于服务注册接口）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：服务注册、监控
- **需求**：把监控指标上报，供负载均衡/监控用
- **自主实现**：通过服务注册接口把指标放进实例 metadata
- **参考实现**：docs"基于 Spring Cloud 服务注册接口实现监控指标上报"（呼应第 11 节实例 metadata 上报 cpu-usage）
- **对比取舍**：指标随实例注册上报，负载均衡读 metadata

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| 动态权重需求与本质 | 分布式 | 核心 | P1 | 🔴 | High |
| 指标类型(GAUGE/COUNTER) | 性能 | 核心 | P1 | 🟡 | High |
| 权重响应时间算法 | 分布式 | 核心 | P1 | 🔴 | High |
| 负载均衡规则 IRule | 工程 | 核心 | P1 | 🟡 | High（过时→LoadBalancer） |
| 服务列表管理 | 分布式 | 核心 | P1 | 🟡 | High（过时→LoadBalancer） |
| Ribbon×Feign | 工程 | 支撑 | P2 | 🟡 | High（过时→LoadBalancer） |
| @RibbonClient 上下文 | 工程 | 支撑 | P3 | 🟢 | Medium（过时→LoadBalancer） |
| 指标上报 | 性能 | 支撑 | P3 | 🟢 | Medium |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **负载均衡源码**：`code/spring/spring-cloud-commons/spring-cloud-loadbalancer`（ReactorServiceInstanceLoadBalancer 等）
- **第 11 节衔接**：CpuUsageLoadBalancer(读实例 cpu-usage) + ServiceInstanceListSupplier
- `[待验证]` microsphere 是否有动态权重负载均衡

---

## 五、本节小结（三层次视角）

**需求**：按实例权重(如响应时间)动态分配流量，权重随指标变化。

**自主实现核心**：若我设计——
1. 实例上报指标(响应时间)到注册中心 metadata
2. 负载均衡算权重(响应时间反比)
3. **按权重累计区间 + 随机数**选择实例（概率选择）
4. 权重动态更新（随指标）

**参考实现**：Ribbon `WeightedResponseTimeRule`（docs 完整算法：A/B/C/D 权重区间 1-10/11-40/41-80/81-100 + 随机数）。**Ribbon/Servo 是 Netflix OSS 已过时**，现代用 Spring LoadBalancer（有源码）+ Micrometer。

**对比取舍**：知识本体是"**动态权重负载均衡算法**"（概率选择 + 权重累计区间）。Ribbon 是参考实现的过时版本，按方法论 04 标过时→LoadBalancer。核心算法思想（按权重区间随机）是通用的。

**待验证汇总**：
- 现代 LoadBalancer 动态权重实现
- 现代 LoadBalancer 独立上下文/实例列表更新
- microsphere 动态权重负载均衡
