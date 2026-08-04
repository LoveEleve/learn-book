# Dubbo 源码学习范围规划

> **版本**: 3.3 (main分支)
> **仓库**: `/data/workspace/source-code/code/spring/dubbo/`
> **规模**: 2469 个源文件，20+ 子模块
> **日期**: 2026-08-03

---

## 一、仓库概况

Apache Dubbo 是高性能 Java RPC 框架，核心是 **服务导出(Export) → 服务发现(Registry) → 远程调用(Remoting) → 集群容错(Cluster)** 四层架构。3.x 版本全面拥抱云原生（Triple 协议/应用级服务发现/Proxyless Mesh）。

**核心模块**：

| 模块 | 职责 | 状态 |
|---|---|---|
| `dubbo-common/` | 公共工具：URL 总线、ExtensionLoader SPI、线程模型 | ✅ |
| `dubbo-rpc/` | RPC 协议层：Triple(gRPC 兼容)、Dubbo2、REST、injvm | ✅ |
| `dubbo-remoting/` | 远程通信：Netty 传输、序列化(Hessian2/Fastjson2/Kryo/Protobuf) | ✅ |
| `dubbo-registry/` | 注册中心：ZooKeeper/Nacos/Consul/etcd/K8s API Server | ✅ |
| `dubbo-cluster/` | 集群容错：Failover/Failfast/Failsafe/Forking/Broadcast + LoadBalance | ✅ |
| `dubbo-config/` | 配置层：XML/Annotation/API/Spring Boot Starter | ✅ |
| `dubbo-metadata/` | 元数据中心：应用级服务发现（v3 核心改进） | ✅ |
| `dubbo-filter/` | 过滤器链：AccessLog/ActiveLimit/Token/Exception/Cache | ✅ |
| `dubbo-metrics/` | 指标采集：Micrometer 集成 | ✅ |
| `dubbo-plugin/` | 插件适配：Sentry/Prometheus/Tracing | 淘汰 |
| `dubbo-monitor/` | 监控中心 | 淘汰 |
| `dubbo-serialization/` | 多序列化支持 | 淘汰 |
| `dubbo-demo/` `dubbo-compatible/` `dubbo-dependencies-bom/` `dubbo-distribution/` | Demo/兼容/BOM/分发 | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| D-1 | **ExtensionLoader SPI 机制** | ExtensionLoader, @SPI, @Adaptive, @Activate | **Dubbo 微内核**：`ExtensionLoader.getExtensionLoader(Class).getExtension(name)` 加载 SPI 实现——支持 `@SPI` 默认实现、`@Adaptive` 动态代理（URL 参数决定选哪个实现）、`@Activate` 自动激活（按 group/provider 激活过滤器链）；Wapper 类自动包装（AOP 模式——构造函数注入同类型自动包装）；**URL 总线**：所有配置通过 URL 传递（`protocol://user:pass@host:port/path?key=val`） |
| D-2 | **服务导出与注册** | ServiceConfig, ServiceRepository, RegistryProtocol | **Provider 端**：`ServiceConfig.export()` → `doExportUrls()` → 每个注册中心一个 URL → 组装 URL 参数链（`RegistryProtocol.getRegisteredUrl()` 移除 export 参数 + `override` 目录监听）→ `registry.register(url)` 注册到 ZooKeeper/Nacos 等→`Protocol.export()` 启动 Netty Server；**应用级服务发现**（v3）：`MetadataService` 上报接口→方法→参数类型元数据，注册中心只存 app→host:port 映射，大幅减少注册中心数据量 |
| D-3 | **服务引用与直连调用** | ReferenceConfig(910行), ReferenceBean, DubboInvoker(199行), FailoverClusterInvoker(143行) | **Consumer 端**：`ReferenceConfig.get()` → `createProxy()` 创建动态代理（`InvokerInvocationHandler`）→ `RegistryProtocol.refer()` 订阅注册中心→`RegistryDirectory` 监听 Provider 列表变更→`Cluster.join()` 创建 `ClusterInvoker`（包装负载均衡+容错）；**调用链**：`proxy.method()` → `InvokerInvocationHandler.invoke()` → `MockClusterInvoker`(降级) → `FailoverClusterInvoker.doInvoke()`：每次重试 `list(invocation)` 重新拉取 Provider 列表→`select(loadbalance, invocation, copyInvokers, invoked)` 避免重选已试过的 Provider→`invokeWithContext(invoker, invocation)` Netty 发送→**业务异常不重试**（`e.isBiz()` 直接抛）只重试网络/传输异常→成功时日志记录失败过的 Provider 列表；`calculateInvokeTimes(methodName)` = `retries + 1` |
| D-4 | **集群容错与负载均衡** | AbstractClusterInvoker, FailoverClusterInvoker(143行), LoadBalance, StickyInvoker | **Provider 选择三层机制**：① **StickyInvoker**（粘性连接）：`stickyInvoker` 字段缓存上次选择→`sticky=true` + `invoker.isAvailable()` → 优先复用，减少重新选择开销；② **Reselect**（重新选择）：上次选中的 Invoker 不可用时→随机取 `reselectCount` 个候选 Invoker→loadbalance 再选→避免遍历全部；③ **AvailableCheck**（可用性检查）：`invoker.isAvailable()` 提前过滤不可用实例；**7 种容错**：`Failover`(重试→每次重选 Provider→业务异常不重试)、`Failfast`/`Failsafe`/`Failback`(ScheduledExecutor 5s 重试)/`Forking`/`Broadcast`/`Available`(取第一个可用)/`Mergeable`(合并 Group 结果)；**6 种负载均衡**：`Random`(加权 ThreadLocalRandom)、`RoundRobin`、`LeastActive`(并发数最少+预热)、`ConsistentHash`(首参哈希)、`ShortestResponse`(滑动窗口估算)、`Adaptive`(动态调整权重)

### 🟡 扩展域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| D-5 | **Triple 协议（gRPC 兼容）** | dubbo-rpc-triple, TripleProtocol, ProtobufCodec | **Dubbo3 原生协议**——`dubbo-rpc/dubbo-rpc-triple/` 子模块实现；**4 种协议**：dubbo-rpc-dubbo(原生)、dubbo-rpc-triple(HTTP/2+gRPC兼容)、dubbo-rpc-injvm(进程内)、dubbo-rpc-api(抽象层)；**Triple 特性**：基于 HTTP/2、支持 Unary/ServerStream/ClientStream/BiStream 四种调用模式、Protobuf/JSON 双编码、单端口同时支持 Dubbo 和 gRPC 调用、`@DubboService(protocol="tri")` 切换 |
| D-6 | **Filter 过滤器链** | Filter, InvokerChainBuilder, AccessLogFilter, ActiveLimitFilter, TokenFilter | **SPI 激活**：`@Activate(group={PROVIDER,CONSUMER})` 控制过滤器在 Provider/Consumer 端自动加载；**过滤顺序**：`META-INF/dubbo/internal/org.apache.dubbo.rpc.Filter` 定义加载顺序；内置 20+ 过滤器：AccessLog(调用日志)、ActiveLimit(并发限制)、Token(令牌验证)、Exception(异常包装) 等 |
| D-7 | **Spring Boot Starter 集成** | DubboAutoConfiguration, @DubboService, @DubboReference | `@DubboService` 替代 `@Service` 标注 Provider；`@DubboReference` 替代 `@Reference` 注入 Consumer；`@EnableDubbo` 启动 Dubbo 配置；`dubbo.application.name/registry/protocol` YAML 配置 |

---

## 三、淘汰清单

| 子模块/功能 | 理由 |
|---|---|
| `dubbo-serialization/` | 多序列化支持——基础设施 |
| `dubbo-monitor/` `/actuator` | 监控中心——用 SkyWalking/Prometheus 替代 |
| `dubbo-plugin/` | 外部集成适配——Sentry/Prometheus 等 |
| `dubbo-compatible/` | 旧版 API 兼容 |
| `dubbo-configcenter/` | 配置中心（Apollo/Nacos config）——边缘功能 |
| `dubbo-metadata-report/` | 元数据上报存储 |
| `dubbo-qos/` | QOS 运维命令 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 3 |
| **总域** | **7** |
| 淘汰模块 | 7+ 个 |

---

## 五、学习顺序建议

```
D-1 ExtensionLoader SPI（理解 Dubbo 微内核，所有模块的基础）
  → D-2 服务导出与注册（理解 Provider 如何暴露）
    → D-3 服务引用与调用（理解 Consumer 如何调用）
      → D-4 集群容错与负载均衡（理解生产级可靠性）
        → D-5/D-6/D-7 按需深入
```

以上规划完成，共 **4🔴+3🟡=7 域**。
