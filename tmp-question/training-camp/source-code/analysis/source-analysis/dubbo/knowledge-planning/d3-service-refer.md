# D-3 服务引用 — 双路径装配与 3.x 迁移面

> 项目: Dubbo | 🔴 Deep / 1 篇 | ReferenceConfig(910)+RegistryProtocol(integation)+MigrationInvoker+ProxyFactory
> 基线: DUBBO-PLAN D-3 (生命周期) — 前置: **D-1 (SPI) + D-2 (导出对称)** — 展开 装配面→注册引用面→集群接入面→代理面

---

## §0.8

- 🔴 Deep，1篇 — 入口(**ReferenceConfig.get(boolean check) L228-248: destroyed 检查→Deployer prepare/start→init**) → init 9 步(**L332-404: 双检锁→refresh[AbstractConfig:718]→ServiceDescriptor/ConsumerModel→createProxy**) → 双路径(**createProxy L489-523: url 非空→parseUrl 直连 L605[分号多 URL+registry vs peer-to-peer PEER_KEY]/否则→aggregateUrlFromRegistry L633[loadRegistries+monitor+injvm 兜底 L650-655 shouldJvmRefer L854]**) → 三分支(**createInvoker L668-714: 单 URL→Cluster.DEFAULT.join(StaticDirectory)/多 URL→ZoneAwareCluster 默认[CLUSTER_KEY L697]/直连**) → 注册引用(**RegistryProtocol.refer L555-576: group 合并→MERGEABLE_CLUSTER[CommonConstants:292]/默认 CLUSTER_KEY→doRefer L578-595→getMigrationInvoker L601-609→ServiceDiscoveryMigrationInvoker; interceptInvoker=RegistryProtocolListener.onRefer L623-640**) → 迁移面(**MigrationInvoker L81-115: 双 invoker+decideInvoker L314-323+三态 MigrationStep[FORCE_INTERFACE/APPLICATION_FIRST/FORCE_APPLICATION]+默认 INIT→APPLICATION_FIRST[MigrationRule.java:167]**) → 代理面(**proxyFactory 自适应 L189+@SPI javassist L29+InvokerInvocationHandler L34-100[Object 特判+RpcInvocation 构建+ConsumerModel 挂载]→InvocationUtil.invoke D-4 桥**)
- 设计模式: [模式: 配置装配+双路径+黑盒 SPI+迁移策略]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ReferenceConfig.java:228-248 | 入口 | **get(boolean check): destroyed 检查→Deployer prepare/start→init** — 3.x 生命周期 | High |
| ReferenceConfig.java:332-404 | init | **init 9 步: 双检锁→refresh→NATIVE_STUB 检测→ConsumerModel→createProxy→checkInvokerAvailable** | High |
| ReferenceConfig.java:489-523 | 双路径 | **createProxy: meshModeHandleUrl(L530-605)→url 非空 parseUrl/否则 aggregateUrlFromRegistry→createInvoker→proxyFactory.getProxy** | High |
| ReferenceConfig.java:605-632 | 直连 | **parseUrl: 分号多 URL; registry URL→REFER_KEY / peer-to-peer→mergeUrl+PEER_KEY 标记** | High |
| ReferenceConfig.java:633-663 | 注册 | **aggregateUrlFromRegistry: loadRegistries→monitor+injvm 标记→REFER_KEY; urls 空→injvm 兜底 (shouldJvmRefer L854)** | High |
| ReferenceConfig.java:668-714 | 集群 | **createInvoker 三分支: 单 URL Cluster.DEFAULT/多 URL ZoneAwareCluster 默认/直连** | High |
| RegistryProtocol.java:555-595 | 注册引用 | **refer: group 合并→MERGEABLE_CLUSTER/默认→doRefer→MigrationInvoker→interceptInvoker (RegistryProtocolListener)** | High |
| MigrationInvoker.java:81-115 | 迁移 | **双 invoker 持有+decideInvoker 按规则选+三态 (MigrationStep)** | High |
| MigrationRule.java:167 | 默认 | **INIT 规则→"initial step: APPLICATION_FIRST"** — 双订阅共存默认 | High |
| InvokerInvocationHandler.java:34-100 | 代理 | **Object 特判+RpcInvocation 构建+ConsumerModel 挂载→InvocationUtil.invoke (D-4 起点)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 引用是单管线 (装配→引用→集群→代理) — 1篇按四段展开; RegistryDirectory 动态目录属 D-5 (导航), ClusterInvoker 容错属 D-7 (导航), 消费侧 publishServiceDefinition 属 D-11 (导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 双路径 (直连 parseUrl vs 注册中心聚合) | 🔴 | **为什么🔴**: 引用主线 |
| P1-2 | injvm 兜底 (本地始终可引, D-2 对称) | 🔴 | **为什么🔴**: 本地引用语义 |
| P1-3 | createInvoker 三分支 + ZoneAware 默认 | 🔴 | **为什么🔴**: 集群接入 |
| P1-4 | MigrationInvoker 三态迁移 | 🔴 | **为什么🔴**: 3.x 核心面 |
| P2-1 | group 合并集群 (MERGEABLE) | 🟡 | **为什么🟡**: 分组场景 |
| P2-2 | mesh 网格直连 (K8s Envoy) | 🟡 | **为什么🟡**: 网格面 |
| P2-3 | 代理门面 + InvokerInvocationHandler | 🟡 | **为什么🟡**: 调用入口 |
| P3-1 | checkInvokerAvailable 启动检查 | 🟢 | **为什么🟢**: fail-fast 细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配双路径** (直连/注册中心/injvm) | 🔴 | 主线 |
| B | **注册引用面** (RegistryProtocol/迁移) | 🔴 | 3.x 核心 |
| C | **集群接入面** (三分支) | 🔴 | D-7 前置 |
| D | **代理面** (门面+检查) | 🟡 | 调用入口 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 装配面 | get→init 9 步→createProxy 双路径 (直连/注册中心聚合)→injvm 兜底; refresh 在 dubbo-common AbstractConfig:718 (3.x 配置模型) | ReferenceConfig.java:228-404,489-663 |
| q2 | 注册引用面 | RegistryProtocol.refer: group="a,b"/"*"→MERGEABLE_CLUSTER; 默认 CLUSTER_KEY→doRefer→**MigrationInvoker (3.x 接口级→应用级迁移)**; interceptInvoker=RegistryProtocolListener.onRefer 监听器 (非拦截器链!) | RegistryProtocol.java:555-640 |
| q3 | 集群接入面 | createInvoker 三分支: 单 URL→Cluster.DEFAULT.join(StaticDirectory)/多 URL→**ZoneAwareCluster 默认**/无 registry 直连; StaticDirectory 静态目录 vs D-5 RegistryDirectory 动态 | ReferenceConfig.java:668-714 |
| q4 | 代理面 | proxyFactory @SPI javassist 默认; InvokerInvocationHandler: Object 特判 (toString/$destroy/hashCode/equals)→RpcInvocation→**InvocationUtil.invoke = D-4 起点**; checkInvokerAvailable 轮询 fail-fast | ReferenceConfig.java:189,522; InvokerInvocationHandler.java:34-100 |
| q5 | 迁移默认 | 无配置中心→INIT 规则→getStep 兜底 "initial step: APPLICATION_FIRST" — 双订阅共存是 3.x 默认 | MigrationRule.java:167 |

→ 引出 D-4 调用链 (InvocationUtil.invoke 桥); D-5 注册中心 (RegistryDirectory 深潜); D-7 集群容错 (ClusterInvoker 深潜); D-11 元数据 (消费侧 publishServiceDefinition)。
