# 06-REQ：microsphere-nacos 完整需求规格

> 本文是 microsphere-nacos 的完整需求文档。所有需求分三类：
> 1. **已实现需求**（REQ-001~008，8 项）——每项标注与官方 Nacos SDK 的关系
> 2. **待实现需求**（REQ-D01~D09，9 项）——bug 修复
> 3. **全新发散**（REQ-N01~N04，4 项）——生产环境还需要但项目未覆盖的能力
>
> **基准环境**：Java 8（openapi）+ Java 17（discovery），纯 HTTP，零 gRPC 依赖

---

## 项目定位

**microsphere-nacos 是手写的 Nacos OpenAPI HTTP 客户端**——不依赖官方 `nacos-client` SDK（gRPC），通过 Apache HttpClient 直接调用 Nacos Server 的 `/v1` 和 `/v2` REST API。这是 microsphere 生态对 Nacos 的自有集成方案：不用 gRPC 连接模型，没有本地 failover/snapshot 容错，没有心跳调度器，但换来了更简单的依赖（只需 HttpClient + Gson）和纯粹的 HTTP 可控性。

与官方 SDK 的核心差异：
| 维度 | 官方 nacos-client | microsphere-nacos |
|------|:---:|:---:|
| 传输 | gRPC（长连接 + 双向流） | HTTP（短连接，无连接保活） |
| 配置获取 | ConfigService（含本地容错/snapshot） | HTTP 请求，无本地容错 |
| 服务发现 | NamingService（subset 优化） | HTTP 请求，全量查询 |
| 心跳 | BeatReactor 后台调度 | `sendHeartbeat()` 需显式调用 |
| 端点 | ServerList 故障转移 | serverAddress 单点 |
| 鉴权 | gRPC 连接层 | `AuthorizationManager`（ScheduledExecutor 定时刷新） |

**源码信息**：
- 路径：`/data/workspace/java-training-camp/cloud-native-code/projects/microsphere-nacos/`
- 模块：`openapi`（118 文件，纯 Java HTTP 客户端）+ `discovery-spring-cloud`（10 文件，Spring Cloud 适配）
- 对照实现：microsphere-etcd（8 文件，`/data/workspace/java-training-camp/cloud-native-code/projects/microsphere-etcd/`）
- 序列化：Gson + SPI 注册的 `GsonDeserializer` 体系

---

## 一、HTTP 传输层

### REQ-001：Nacos OpenAPI 的 HTTP 传输抽象

**问题**：Nacos Server 的所有功能（配置管理/服务发现/命名空间管理）都通过 REST API 暴露——需要统一的 HTTP 客户端来拼接 URL、发请求、处理响应、反序列化 JSON。每个 API 如果都自己拼 URL 和 HttpClient 调用，代码重复且错误处理不一致。

**产出**：
- `OpenApiClient`（接口）：`execute(OpenApiRequest, Type) → T`——统一入口，接收请求对象 + 期望返回类型，返回反序列化后的 Java 对象
- `AbstractOpenApiClient`（抽象骨架）：模板方法——`getAccessToken()` 注入鉴权头 → `doExecute()` 执行 HTTP → Gson 反序列化 → 异常处理
- `OpenApiHttpClient`（Apache HttpClient 5 实现）：连接池管理（PoolingHttpClientConnectionManager）→ `doExecute()` 构造 HttpUriRequest → 返回 `OpenApiResponse`
- `OpenApiRequest.Builder`：Builder 模式构建请求——path/queryParams/headers/body/HttpMethod
- `OpenApiResponse`：HTTP 状态码 + body 字符串 + content-type
- `HttpMethod` 枚举：GET/POST/PUT/DELETE
- `OpenApiClientException`：统一异常（code/status/message）

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P3：HttpMethod.DELETE 枚举值写成 "GET"**（`:58`）——当前无调用方，但隐患
- `AuthorizationManager` 初始 token 刷新失败抛异常导致 client 构造失败（无重试）

**配置规格**：通过 `NacosClientConfiguration` 配置连接池（maxConnections/maxPerRoute/timeout）。

---

## 二、领域模型

### REQ-002：Nacos 领域实体 + JSON 反序列化体系

**问题**：Nacos API 返回的 JSON 需要反序列化为 Java 对象——`Instance`（服务实例）、`Service`（服务信息）、`Config`（配置内容）、`Namespace`（命名空间）、`Raft`（集群状态）。每个实体的 JSON 结构不同，需要针对性的反序列化器。

**产出**（`common/` 包）：
- 领域模型：`Instance`、`Service`、`ServiceInfo`、`Config`、`Namespace`、`RaftCluster` 等
- `Page<T>`：分页模型——`getElements()`/`getTotalPages()`/`getTotalElements()`/`isFirst()`/`hasNext()`
- Gson 反序列化体系：`GsonDeserializer<T>` SPI → `DefaultDeserializer`（ServiceLoader 加载注册）→ `BaseConfigDeserializer` 模板方法
- SPI 注册的反序列化器：`ConfigDeserializer`、`ServiceInfoDeserializer`、`InstanceDeserializer`、`HistoryConfigDeserializer` 等
- `JsonUtils.toJSON(Object)`：基于 `BeanInfo` 反射的序列化

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P2：Page.isLast()/hasNext()/hasPrevious() off-by-one**（`:isLast = pageNumber == totalPages - 1` 应为 `== totalPages`；`hasNext < totalPages - 1` 应为 `< totalPages`；`hasPrevious > 0` 应为 `> 1`）
- **P2：RaftModelDeserializer 对嵌套 leader 对象用 `getAsString()` 而非 `getAsJsonObject()`**
- **P2：HistoryConfigDeserializer.DATE_FORMAT = "YYYY-MM-DD..." 用了 week-year `YYYY`（应为 `yyyy`）；createdTime 为 null 时 parse(null) 抛异常导致整个反序列化失败**
- `DefaultDeserializer.loadGsonDeserializers` 用 HashMap，同类型 Deserializer 覆盖无警告
- `ConfigDeserializer` 常量 `OPERATOR_IP_MEMBER_NAME="createUser"` / `OPERATOR_MEMBER_NAME="createIp"` 命名与语义反了
- `JsonUtils.toJSON` 私有 getter/循环引用会失败，`DefaultSerializer`/`DefaultDeserializer` 的 Gson 无日期/Lenient 配置（TODO 注释承认）

**配置规格**：通过 `META-INF/services/io.microsphere.nacos.client.common.io.GsonDeserializer` SPI 注册。

---

## 三、配置管理

### REQ-003：Nacos 配置的 CRUD + 长轮询监听

**问题**：配置中心最核心的能力——发布配置、读取配置、删除配置、监听配置变更。Nacos 的配置变更通知通过长轮询实现——发一个 HTTP 请求，服务端 hold 住连接直到有配置变更或超时（默认 30s），客户端立即发起下一次长轮询。

**产出**（V1 + V2 双版本）：
- `OpenApiConfigClient` / `OpenApiConfigClientV2`：`publishConfig()` / `getConfig()` / `removeConfig()` / `getHistoryConfigs()`
- `ConfigListenerManager`：管理配置变更监听器——调用 `getConfigs()` 获取有变更的配置 → `fetchConfigs()` 补更新内容 → 通过 `EventDispatcher` 发布 `ConfigChangedEvent`
- `ConfigChannel`：`addListener(dataId, group, listener)` 注册配置监听，内部判断是否已存在避免重复注册
- `ConfigListener` 函数式接口：`onChanged(ConfigChangedEvent)` / `onException(Throwable)`
- `ConfigChangedEvent`：变更事件（dataId/group/content/type/oldContent）

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P1：ConfigListenerManager.fetchConfigs 对 CopyOnWriteArraySet 迭代器调 `remove()`**（`:199`）——COW 不支持，抛 `UnsupportedOperationException`，被 `catch(Throwable)` 静默吞掉 → configId 永不从 fetchingConfigIds 移除，每次循环空转
- **❌ P1：Constants.EVENT_PROCESSING_TIMEOUT 单位错误**——`SECONDS.toMicros(30)` 应为 `toMillis(30)`——默认值 30,000,000ms ≈ 8.3 小时（应为 30s）
- **P2：OpenApiConfigClientV2.publishConfig `newConfig.getType()==null` 时 `type.toLowerCase()` NPE**——V1 版安全处理 null，V2 版遗漏
- `OpenApiConfigClient.getHistoryConfigs` 校验消息 "must less than or equal 1" 应为 500

**配置规格**：`nacos.config.event.processing.timeout` 系统属性控制长轮询超时。

---

## 四、服务发现

### REQ-004：Nacos 服务实例查询与心跳

**问题**：微服务架构中需要知道"某个服务有哪些实例在运行、是否健康"——查询 Nacos 服务实例列表，并向 Nacos 上报心跳保持实例存活。

**产出**（V1 + V2 双版本）：
- `OpenApiServiceClient` / `OpenApiServiceClientV2`：`registerInstance()` / `getServiceList()` / `getInstancesList()` / `sendHeartbeat()` / `updateInstance()`
- `OpenApiInstanceClient`：`getInstanceDetail()` / `batchUpdateInstanceMetadata()`
- `OpenApiRaftClient`：`getRaftLeader()` 查询集群选举状态
- `InstanceDeserializer`：`jsonObject.getAsJsonObject("metadata")` → Map 转换（支持嵌套元数据对象）

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- 无本地心跳调度——`sendHeartbeat()` 需业务代码显式调用（官方 SDK 有 BeatReactor 后台调度）
- serverAddress 单点——无集群故障转移
- `NacosServiceInstance.isSecure()` 恒 false、`getScheme()` 恒 "http"——不支持 HTTPS metadata 探测

**配置规格**：无配置项（serverAddress 在 client 构造时指定）。

---

## 五、Spring Cloud Discovery 适配

### REQ-005：Nacos DiscoveryClient 的 Spring Cloud 集成

**问题**：Spring Cloud 应用需要标准 `DiscoveryClient` 接口来查询服务实例——microsphere-nacos 需要把自己的 HTTP 客户端包装成 Spring Cloud 的标准接口，包括自动配置和实例模型适配。

**产出**（`discovery-spring-cloud`，10 文件）：
- `NacosDiscoveryClient`：`getInstances(serviceId)` → `instanceClient.getInstancesList(namespaceId, serviceName)`；`getServices()` → `serviceClient.getServiceList()` 分页查询
- `NacosServiceInstance`：`DefaultServiceInstance` 子类——port/uri/metadata 映射自 Nacos Instance 模型
- `NacosDiscoveryConfiguration`：`@Configuration` 注册 `NacosClient`/各子 client bean + `NacosClientProperties`
- `NacosDiscoveryAutoConfiguration`：`AutoConfiguration.imports` 注册
- `NacosDiscoveryContextFactory`：基于 `NamedContextFactory` 支持多 Nacos 源（不同 namespace）
- `NacosClientProperties`：`@ConfigurationProperties(prefix="microsphere.nacos.client")` 绑定客户端配置
- `NacosClientAutoConfiguration`：`OpenApiNacosClientV2` bean + `OpenApiNacosClient` bean 同时注册

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P1：NacosDiscoveryClient.getServices() 分页双重 bug**（`:77-86`）——`pageNumber=0`（应为 1）+ 翻页 `pageNumber=serviceNames.getPageSize()`（应为 `pageNumber+1`）——叠加 Page 的 off-by-one，多页场景漏页/死循环
- **没有 `ServiceRegistry`/`Registration` 实现**——无 register/deregister（etcd 侧则有 `EtcdServiceRegistry`）

**配置规格**：
```properties
microsphere.nacos.client.server-address = http://localhost:8848
microsphere.nacos.client.namespace-id = public
```

---

## 六、V1/V2 双版本适配

### REQ-006：Nacos V1 和 V2 API 的并行封装

**问题**：Nacos Server 有两个 API 版本——`/v1`（稳定，所有核心功能）和 `/v2`（新增 client 管理接口、不同的请求/响应格式）。两个版本的 endpoint path、请求参数格式、返回结构不完全一致。客户端需要同时支持两个版本并透明选择。

**产出**：
- `NacosClient`（V1 Facade）：聚合 `OpenApiConfigClient`、`OpenApiServiceClient`、`OpenApiInstanceClient`、`OpenApiNamespaceClient`、`OpenApiServerClient`、`OpenApiRaftClient`
- `NacosClientV2`（V2 Facade）：聚合对应 V2 子 client + 额外 `getAllClientIds()`/`getClientDetail()` V2 专有方法
- `OpenApiNacosClient`（V1 实现）：委托各 V1 子 client
- `OpenApiNacosClientV2`（V2 实现）：委托各 V2 子 client + 实现 client 管理接口
- `OpenApiVersion` 枚举：`V1 = "/v1"` / `V2 = "/v2"`——response() 方法按版本区分 HTTP 响应处理
- `OpenApiTemplateClient`：模板方法——`response(response, type)` 调用 `OpenApiVersion.response()` 分支处理

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P1：OpenApiNacosClientV2 注入的是 V1 客户端，不是 V2**——构造器中 `new OpenApiConfigClient/ServiceClient/InstanceClient/...`（V1 类），而非对应 V2 类。6 个 V2 子 client 类**只被测试引用**，主聚合类不用——V2 Facade 下 config/discovery/namespace 全部实际走 `/v1` endpoint
- `OpenApiNamespaceClient.getNamespace()` / V2 版 `catch(OpenApiClientException)` 后吞异常返回 null

**配置规格**：通过 `NacosClientConfiguration` 选择 V1 或 V2 客户端。

---

## 七、鉴权管理

### REQ-007：Nacos 访问令牌的定时刷新

**问题**：Nacos Server 开启鉴权后，所有 API 调用需要 Authorization 头携带 accessToken——token 有过期时间，需要定时刷新。

**产出**：
- `AuthorizationManager`：`ScheduledExecutorService` 定时刷新——TTL/2 间隔执行 `login(username, password)` 重新获取 token；`getAccessToken()` 返回当前有效 token
- `AbstractOpenApiClient.execute()` → `getAccessToken()` 注入 `Authorization: Bearer xxx` 请求头

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- 初始刷新失败抛异常导致 client 构造失败——无重试

**配置规格**：通过 `NacosClientConfiguration` 配置 username/password。

---

## 八、etcd 对照实现

### REQ-008：etcd 作为 Nacos 的设计对照

**问题**：同一个寄存器发现抽象，用 etcd 的 KV + Watch API 再实现一遍——不是为了生产用 etcd，而是为了**对照验证**"同样的问题，不同的后端怎么解决"。这个对照实现帮助理解 Nacos 和 etcd 在服务发现上的本质差异。

**产出**（`microsphere-etcd`，8 文件）：
- `EtcdDiscoveryClient`：基于 `ServiceInstancesCache` + Watch 事件缓存
- `EtcdReactiveDiscoveryClient`：响应式版本
- `ServiceInstancesCache`：`computeIfAbsent` 首次同步加载 + Watch 增量更新 + `synchronized(this)` 锁
- `EtcdServiceRegistry`：register/deregister（但 autoConfigure 未注册 bean——**孤儿类**）
- `EtcdClientAdapter`：etcd KV API HTTP 封装

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- `ServiceInstancesCache.addOrUpdateServiceInstance` UPDATE 分支重复 add（`:148` + `:161` 都 add，可能重复）
- `EtcdServiceRegistry` 只被测试引用（grep 确认 autoconfigure 未注册该 bean，属孤儿类）
- `EtcdClientAdapter.getKeyValues` `catch(Throwable)` 静默吞异常
- `EtcdServiceRegistry.resolvePath` 硬编码 `rootPath="/services"` 不读配置

**配置规格**：`io.microsphere.etcd.spring.cloud` 前缀。

---

## 九、待实现需求（bug 修复）

### REQ-D01：OpenApiNacosClientV2 替换为 V2 客户端

**方案**：构造器中 `new OpenApiConfigClient/ServiceClient/InstanceClient/...` → `new OpenApiConfigClientV2/ServiceClientV2/InstanceClientV2/...`

**状态**：[待修复] — 当前 V2 Facade 实际走 V1 endpoint，6 个 V2 子 client 类为孤儿。

---

### REQ-D02：NacosDiscoveryClient.getServices 分页修复

**方案**：`pageNumber = 1`（起始）+ `pageNumber++`（翻页）+ Page off-by-one 配套修复（见 D03）。

**状态**：[待修复] — 当前多页场景漏页/死循环。

---

### REQ-D03：Page 分页模型 off-by-one 修复

**方案**：`isLast() = pageNumber == totalPages` / `hasNext() = pageNumber < totalPages` / `hasPrevious() = pageNumber > 1`

**状态**：[待修复] — 3 个方法全部差 1。

---

### REQ-D04：OpenApiConfigClientV2.publishConfig NPE 修复

**方案**：`type = newConfig.getType() == null ? null : newConfig.getType()` 后再 `toLowerCase()`——与 V1 版安全处理对齐。

**状态**：[待修复] — `type==null` 时直接 NPE。

---

### REQ-D05：ConfigListenerManager.fetchConfigs COW remove 修复

**方案**：`CopyOnWriteArraySet` → `ConcurrentHashMap.newKeySet()` 或删除用 `remove(configId)` 替代 `iterator.remove()`

**状态**：[待修复] — 当前 remove 被 COW 禁止，静默吞异常导致空转。

---

### REQ-D06：EVENT_PROCESSING_TIMEOUT 单位修复

**方案**：`SECONDS.toMicros(30)` → `SECONDS.toMillis(30)`

**状态**：[待修复] — 当前默认约 8.3 小时（应为 30s）。

---

### REQ-D07：RaftModelDeserializer 嵌套对象修复

**方案**：`getString("leader")` → `jsonObject.getAsJsonObject("leader")` + `context.deserialize()`

**状态**：[待修复] — 嵌套 leader 对象时 getAsString 抛异常。

---

### REQ-D08：HistoryConfigDeserializer 日期格式修复

**方案**：`DATE_FORMAT` 的 `YYYY` → `yyyy`；参数 null 时安全返回 null 而非抛 ParseException

**状态**：[待修复] — 年尾周错误 + null 反序列化崩溃。

---

### REQ-D09：HttpMethod.DELETE 常量修复

**方案**：`DELETE("GET")` → `DELETE("DELETE")`

**状态**：[待修复] — 当前无调用方，但字面值的字面错误。

---

## 十、发散需求（生产环境需要的全新能力）

### REQ-N01：本地配置容错与 snapshot

**生产痛点**：Nacos Server 宕机时，应用无法读取配置——启动失败或使用过期配置。官方 SDK 有本地 failover/snapshot 机制——配置变更时写本地文件，启动时优先读本地。

**产出**：`LocalConfigSnapshot` —— 每次 `getConfig()` 成功后写本地文件（按 dataId/group/namespace 建目录），`publishConfig` 成功后更新本地文件。应用启动时若 Nacos 不可达，从本地 snapshot 恢复配置。内存中缓存当前值减少 HTTP 调用。

**状态**：[待实现]（官方 SDK 已有，microsphere 缺此能力）

---

### REQ-N02：后台心跳调度器

**生产痛点**：当前 `sendHeartbeat()` 需业务代码显式调用——如果你忘记调，实例 30 秒后就被 Nacos 标记为不健康。官方 SDK 的 `BeatReactor` 自动后台调度心跳。

**产出**：`HeartbeatScheduler` —— 服务注册时自动启动 `ScheduledExecutorService` 定时发送心跳，`deregister()` 时停止调度。心跳间隔从 Nacos 实例元数据读取。

**状态**：[待实现]（依赖 REQ-N04 ServiceRegistry）

---

### REQ-N03：DiscoveryClient 服务列表本地缓存与增量更新

**生产痛点**：每次 `getInstances(serviceId)` 都全量 HTTP 请求 Nacos Server——在高 QPS 服务调用场景下，HTTP 往返延迟不可接受。

**产出**：`CachingDiscoveryClient` —— 包装现有 `NacosDiscoveryClient`，内存缓存（`ConcurrentHashMap<serviceId, List<ServiceInstance>>`）+ TTL 过期 + 后台异步刷新。

**状态**：[待实现]

---

### REQ-N04：ServiceRegistry 实现（register/deregister）

**生产痛点**：当前 discovery 模块没有 `ServiceRegistry`/`Registration` 实现——这意味着服务只能"被发现"，无法"注册自己"。etcd 侧有 `EtcdServiceRegistry`（虽然是孤儿类），Nacos 侧完全缺失——需要补齐标准 Spring Cloud 服务注册流程。

**产出**：`NacosServiceRegistry`（实现 `ServiceRegistry<NacosRegistration>`）+ `NacosAutoServiceRegistration`（自动注册）。与 05-microsphere-spring-cloud 的 `MultipleServiceRegistry` 配合——作为多注册中心的底层注册提供者。

**状态**：[待实现]（配合 05-REQ-001 多注册中心，Nacos 作为默认 provider；etcd 侧需修复 orphan state）

---

## 十一、与官方 Nacos SDK 的能力对照表

> 经对 `/data/workspace/source-code/code/spring/nacos/` 官方源码的对照，标注每项 REQ 与官方 SDK 的关系。

| 能力 | 官方 SDK | microsphere | 评价 |
|------|:---:|:---:|------|
| 配置 CRUD + 长轮询 | ✅ gRPC + 本地容错 | ✅ HTTP，无容错 | 官方更健壮 |
| 服务实例查询 | ✅ NamingService（subset） | ✅ HTTP 全量 | 官方更高效 |
| 心跳调度 | ✅ BeatReactor 自动 | ❌ 需显式调用 | 官方更好 |
| 端点故障转移 | ✅ serverList 集群 | ❌ 单点 | 官方更好 |
| gRPC 连接管理 | ✅ 长连接保活 | N/A（纯 HTTP） | 架构差异 |
| 双版本 V1/V2 | ✅ gRPC 有双版本 | ✅ HTTP 双版本 | 等同 |
| DiscoveryClient 适配 | ✅ Spring Cloud 集成 | ✅ Spring Cloud 集成 | 等同 |
| ServiceRegistry | ✅ 自动注册 | ❌ 缺失 | microsphere 缺失 |
| 本地配置容错 | ✅ 本地 snapshot | ❌ 缺失 | microsphere 缺失 |
| 构建依赖 | nacos-client + gRPC | HttpClient + Gson | microsphere 依赖更少 |

**结论**：microsphere 的纯 HTTP 方案依赖更轻（不需要 gRPC），但缺失心跳、容错、故障转移等生产必需能力。官方 SDK 更适用于生产，microsphere 更适用于需要"完全掌控 HTTP 调用链"的场景。

---

## 十二、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| — | 2026-08-02 | REQ 文档编写（第一版） |
