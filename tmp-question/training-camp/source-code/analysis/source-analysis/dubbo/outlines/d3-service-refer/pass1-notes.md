# D-3 服务引用 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (ReferenceConfig.java 910 行)
> 09 域级审计: 执行计划 DB-3 断言 "ReferenceConfig.get()—RegistryProtocol.refer—Cluster 容错" — 已逐条 grep, 见文末审计表

## 入口展开 (Level-1~3, 已读源码)

### Level-1: ReferenceConfig.get(boolean check) (L228-248)

```
get(boolean check)
├── destroyed 检查 → IllegalStateException (引用已销毁)
├── ref == null →
│   ├── getScopeModel().getDeployer().prepare()  — 生命周期外部托管 (Spring)
│   └── getScopeModel().getDeployer().start()    — 兼容旧 API 用法 (3.x Deployer)
└── init(check)
```

### Level-2: init (L332-404) — 引用初始化 9 步

```
init(boolean check)
├── lock.lock() 双检锁 (initialized && ref != null 提前返回)
├── refresh() — 配置刷新 (3.x ConfigManager 多源合并)
├── proxy 自动检测: 接口继承 DubboStub → NATIVE_STUB
├── initServiceMetadata + generateServiceKey (服务元数据)
├── appendConfig() → referenceParameters (配置→URL 参数)
├── ServiceRepository.registerService(interfaceClass) → ServiceDescriptor (3.x 服务模型)
├── ConsumerModel 构建 + repository.registerConsumer (消费模型注册)
├── ref = createProxy(referenceParameters)          ← 核心
├── check → checkInvokerAvailable(0) — 启动即检查 invoker 可用性
└── 异常 → logAndCleanup(t) (初始化失败清理)
```

### Level-3a: createProxy (L489-523) — 引用双路径

```
createProxy(referenceParameters)
├── meshModeHandleUrl (网格模式特殊面)
├── 双路径:
│   ├── url 非空 → parseUrl (L503: 用户指定 URL = peer-to-peer 直连/注册中心地址)
│   └── 否则 → aggregateUrlFromRegistry (L507: 注册中心 URL 聚合)   ← 主流路径
├── createInvoker()
├── publishServiceDefinition(consumerUrl, serviceModel) — 消费侧也发布服务定义! (D-11 关联)
└── proxyFactory.getProxy(invoker, generic) — JDK 动态代理 (generic → GenericService)
```

### Level-3b: aggregateUrlFromRegistry (L633-663) — 注册 URL 装配

```
aggregateUrlFromRegistry
├── checkRegistry() + ConfigValidationUtils.loadRegistries → 注册中心 URL 列表
├── 循环: monitorUrl 附加 (MONITOR_KEY) → scope/service model 绑定 → injvm 标记 (LOCAL_PROTOCOL) → urls.add(REFER_KEY=参数)
├── urls 空 && shouldJvmRefer → injvm URL 兜底 (LOCAL_PROTOCOL/localhost:0)   ← 本地始终可引 (与 D-2 本地始终可导对称!)
└── 仍空 → IllegalStateException ("No such any registry to reference...")
```

### Level-3c: createInvoker (L668-714) — invoker 生成三分支

```
createInvoker
├── urls.size()==1:
│   ├── invoker = protocolSPI.refer(interfaceClass, url)   ← Protocol 自适应 (D-1 消费)
│   └── 非 registry URL && !unloadClusterRelated → Cluster.DEFAULT.join(new StaticDirectory(url, invokers), true)
├── 多 URL:
│   ├── 循环 protocolSPI.refer (多注册中心各自 refer, 不做可用性检查 — 可后变为可用)
│   ├── registryUrl != null → Cluster(CLUSTER_KEY 默认 ZoneAwareCluster.NAME).join(StaticDirectory, false)
│   │   ← 多订阅默认 zone-aware 策略 (3.x 面!)
│   └── 无 registry → 直连: Cluster(cluster 参数).join(StaticDirectory, true)
└── 包装序注释: ZoneAwareClusterInvoker(StaticDirectory) → FailoverClusterInvoker(RegistryDirectory, routing) → Invoker
```

### Level-4: RegistryProtocol.refer (L555-576) → doRefer (L578-595)

```
RegistryProtocol.refer(type, url)
├── getRegistryUrl (URL 规范化) → getRegistry (Registry SPI 自适应获取)
├── RegistryService 自身引用 → proxyFactory.getInvoker (自举)
├── group="a,b"/"*" → MERGEABLE_CLUSTER (合并集群引用!)  ← 分组聚合面
└── 默认 → Cluster.getCluster(CLUSTER_KEY)
    └── doRefer(cluster, registry, type, url, qs)
        ├── consumerUrl 构建 (CONSUMER 协议, putAttribute CONSUMER_URL_KEY)
        ├── getMigrationInvoker → MigrationInvoker   ← 3.x 接口级→应用级服务发现迁移面!
        └── interceptInvoker (ClusterInterceptor 拦截链)
        (深层: RegistryDirectory subscribe/notify 属 D-5 注册中心域, 本域黑盒)
```

## 09 域级审计表 (执行计划 DB-3 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "ReferenceConfig.get()" | get(boolean check) L228-248 — **3.x 带 check 参数** | 修正: 带参签名 |
| "RegistryProtocol.refer" | RegistryProtocol.refer(Class, URL) L557 (dubbo-registry-api) | 接受 (下一层展开) |
| "Cluster 容错" | createInvoker: Cluster.getCluster.join + 三分支 | 接受 (D-7 前置, 本域黑盒引用) |
| 执行计划未提: 多注册中心 ZoneAwareCluster 默认 | createInvoker L687-693 CLUSTER_KEY=ZoneAwareCluster.NAME | 补锚 |
| 执行计划未提: injvm 本地兜底引用 | aggregateUrlFromRegistry L648-655 shouldJvmRefer → LOCAL_PROTOCOL | 补锚 |
| 执行计划未提: ConsumerModel/ServiceDescriptor 3.x 模型 | init L363-383 | 补锚 |
| 执行计划未提: 消费侧 publishServiceDefinition | createProxy L517 | 补锚 (D-11 关联) |

## 展开完成度 (三次 REVIEW 后)

1. ~~RegistryProtocol.refer~~ ✅ (Level-4: group 合并集群 + MigrationInvoker + interceptInvoker = RegistryProtocolListener)
2. ~~Cluster.join / StaticDirectory~~ ✅ (q3: 三分支 + 静态目录 + buildFilterChain, D-7 只到黑盒)
3. ~~proxyFactory.getProxy~~ ✅ (q4: SPI 家族 + InvokerInvocationHandler + D-4 桥)
4. ~~injvm 引用路径~~ ✅ (深审 T: InjvmProtocol.isInjvmRefer L85-101 — scope/generic/broadcast/exporterMap 判定)
5. ~~MigrationInvoker 内部~~ ✅ (q2 + 三次 REVIEW: 三态规则 + INIT 默认 + RegistryProtocolListener)
