# D-3 服务引用 — 配置→URL→invoker→代理

> 前置: [[D-1-SPI微内核]] [[D-2-服务导出]] | 引出: [[D-4-RPC调用]] [[D-5-注册中心]] [[D-7-集群容错]] [[D-11-元数据]] | 对照: Spring Bean 获取 + Feign 客户端
> 🔴 A | 8 KP | [模式: 配置装配 + SPI 自适应 + 3.x 迁移]
> Pass 2 闭环: q1(装配面) q2(注册中心引用面) q3(集群接入面) q4(代理与检查面)

**读者处境**: 消费方配置 <dubbo:reference> 后, get() 到底做了什么? 直连和注册中心两条路怎么选? 3.x 服务发现迁移是什么? 这篇拆 ReferenceConfig (910) + RegistryProtocol + 代理生成。

### 1. 装配面 — get → init → createProxy 双路径

场景: 一个引用配置怎么变成可调用的 invoker?
源码路径:
- **get(boolean check)** (L228-248): destroyed 检查 → Deployer prepare/start (3.x 生命周期) → init
- **init** (L332-404): 双检锁 → **refresh** (3.x 配置刷新, AbstractConfig.java:718 在 dubbo-common!) → ServiceDescriptor/ConsumerModel 注册 → createProxy
- **createProxy 双路径** (L489-523): **meshModeHandleUrl** (L530-605: mesh.enable → **triple://providedBy.namespace.svc.cluster.local:80** — K8s Service Mesh 面, Envoy 路由直连) → url 非空 → **parseUrl** (L605-632: 分号多 URL; **registry URL → REFER_KEY / peer-to-peer → ClusterUtils.mergeUrl + PEER_KEY 标记**) / 否则 → **aggregateUrlFromRegistry** (L633 注册中心聚合: loadRegistries + monitor + injvm 标记)
- **injvm 兜底** (L650-655, shouldJvmRefer L854): urls 空 → LOCAL_PROTOCOL localhost:0 — **本地始终可引** (与 D-2 本地始终可导对称); ⚠ 精确判定在 **InjvmProtocol.isInjvmRefer** (L85-101): scope=local/injvm=true → 本地; **generic 引用不算本地 / broadcast 集群不算本地 / exporterMap 有 exporter 才算**
- **checkInvokerAvailable** (L716-767): shouldCheck → isAvailable 轮询等待 → 超时抛 "No provider available"
关键设计 (q1): **双路径 (直连/注册中心) + injvm 兜底 + 启动 fail-fast**。[模式: 装配面]

### 2. 注册中心引用面 — RegistryProtocol.refer → MigrationInvoker

场景: 注册中心怎么参与引用? 3.x 接口级/应用级服务发现怎么共存?
源码路径:
- **refer** (RegistryProtocol.java:555-576): getRegistryUrl → getRegistry (SPI) → **group="a,b"/"*" → MERGEABLE_CLUSTER** (分组合并, CommonConstants.java:292 MERGEABLE_CLUSTER_NAME="mergeable") / 默认 CLUSTER_KEY
- **doRefer** (L578-595): consumerUrl 构建 (CONSUMER 协议) → **getMigrationInvoker** (L601-609 → ServiceDiscoveryMigrationInvoker) → **interceptInvoker** (L623-640: **RegistryProtocolListener.onRefer 回调** — 监听器集合, 非拦截器链!)
- **MigrationInvoker** (registry/client/migration/MigrationInvoker.java:81-115): 双 invoker 持有 (接口级 + 应用级) + **decideInvoker** (L314-323 按规则选) + **迁移规则三态** (MigrationStep.java: FORCE_INTERFACE/APPLICATION_FIRST/FORCE_APPLICATION — 实证锚 L205/246/287) + refreshInterfaceInvoker (L472-497); **默认规则: 配置中心未配置 → INIT 规则 (RegistryConstants.INIT), 解析兜底 "initial step: APPLICATION_FIRST" (MigrationRule.java:167 注释实证)**
关键设计 (q2): **group 合并集群 + 3.x 迁移 invoker (双订阅共存平滑切换)**。[模式: 引用面]

### 3. 集群接入面 — createInvoker 三分支

场景: refer 出的 invoker 怎么接上容错?
源码路径:
- **createInvoker 三分支** (L668-714): 单 URL → Cluster.DEFAULT.join(StaticDirectory) / 多 URL → **ZoneAwareCluster 默认** (CLUSTER_KEY) / 无 registry 直连 → cluster 参数 join
- **Cluster.join(directory, buildFilterChain)** (Cluster.java:48): AbstractCluster → ClusterInvoker + Filter 链包装 (buildFilterChain 标志: 多注册中心避免重复包装)
- **StaticDirectory** (directory/StaticDirectory.java:57-69): 静态 invoker 列表 (BitList), notify 直接替换 — 与 D-5 RegistryDirectory 动态订阅对照
- **包装序注释** (L692-693): ZoneAwareClusterInvoker(StaticDirectory) → FailoverClusterInvoker(RegistryDirectory, routing) → Invoker
关键设计 (q3): **StaticDirectory 静态目录 + zone-aware 多注册中心默认 + Cluster SPI 参数驱动**。[模式: 接入面]

### 4. 代理与检查面 — ProxyFactory → InvokerInvocationHandler

场景: 用户拿到的代理内部是什么? 调用如何进入 invoker?
源码路径:
- **proxyFactory 自适应注入** (L189) + **@SPI 默认 javassist** (ProxyFactory.java:29) + 实现族 (jdk/javassist/stub wrapper/nativestub — META-INF internal 文件)
- **getProxy** (L522): generic → GenericService 面
- **InvokerInvocationHandler** (proxy/InvokerInvocationHandler.java:34-100): Object 方法特判 (toString/$destroy/hashCode/equals) → **RpcInvocation 构建** (serviceModel + methodName + protocolServiceKey + 参数) → ConsumerModel/METHOD_MODEL 挂载 → **InvocationUtil.invoke → D-4 起点**
- **publishServiceDefinition** (createProxy L517): 消费侧也发布服务定义 (D-11 钩子)
关键设计 (q4): **代理门面 + Object 方法拦截 + 3.x 方法模型挂载**。[模式: 代理面]

## 代码类型
Architecture (装配 + 迁移)

## 负面空间 (D-3, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不异步引用 | get() 同步返回, 检查同步轮询 (q1) |
| 不引用热更新 | initialized 后不可变, 销毁需重建 (q1) |
| 不多注册强一致 | refer 循环各自 refer, 不做可用性检查 (q2) |
| 不迁移规则自动推荐 | 需用户/规则中心配置 (q2) |
| 不动态目录 | 本域 StaticDirectory; 动态订阅属 D-5 (q3) |
| 不代理缓存复用 | 每 ReferenceConfig 一代理, destroy 后不可再 get (q4) |

## 结尾桥 OUTBOUND

- → [[D-4-RPC调用]]: InvocationUtil.invoke 把 RpcInvocation 送入调用链 — Filter 链/协议选择从这开始
- → [[D-5-注册中心]]: RegistryDirectory subscribe/notify/路由深潜 (本域黑盒)
- → [[D-7-集群容错]]: ClusterInvoker 容错策略 (本域只到 Cluster.join)
- → [[D-11-元数据]]: publishServiceDefinition + 应用级服务发现迁移收尾
