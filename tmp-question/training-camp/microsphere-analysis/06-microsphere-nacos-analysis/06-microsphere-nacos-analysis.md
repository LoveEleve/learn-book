# Microsphere-Nacos 深度分析

> **核心命题**：不依赖官方 `nacos-client` SDK，手写一套 Nacos HTTP 客户端。
> **本文回答**：128 个文件、2 个子模块，每个模块解决什么问题？用了什么设计原则？

---

## 总体结构

```
microsphere-nacos（2 子模块，128 个主文件）
│
├── microsphere-nacos-openapi（118 文件）
│   ├── transport/          HTTP 传输层（接口 + Apache HttpClient 实现）
│   ├── io/                 序列化/反序列化
│   ├── common/             领域接口 + 模型（Instance / Config / Namespace ...）
│   ├── v1/                 V1 实现（endpoint prefix = /v1）
│   └── v2/                 V2 实现（endpoint prefix = /v2 + 客户端管理）
│   └── 依赖：仅 Apache HttpClient + Gson
│
└── microsphere-nacos-discovery-spring-cloud（10 文件）
    └── Spring Boot 自动装配 + Spring Cloud DiscoveryClient 适配
    └── 依赖：openapi 子模块 + microsphere-spring-boot starter
```

**核心设计原则：领域层和集成层分离。** openapi 子模块纯 Java，零框架依赖。integration 子模块只有 10 个文件，负责把领域层适配到 Spring Cloud。换框架（比如适配 Quarkus）只需要换这 10 个文件。

---

## 1. 传输层设计（transport/）

### 代码

```java
// 接口：传输抽象
public interface OpenApiClient extends AutoCloseable {
    <T> T execute(OpenApiRequest request, Type payloadType);
    <T> T executeAsResult(OpenApiRequest request, Type dataType);
}

// 抽象：统一接入 token、序列化、错误处理
public abstract class AbstractOpenApiClient implements OpenApiClient {
    protected abstract OpenApiResponse doExecute(OpenApiRequest request);
    protected abstract String getAccessToken();
    // execute() 调用 doExecute() + 序列化 + 鉴权注入
}

// 实现：Apache HttpClient 连接池
public class OpenApiHttpClient extends AbstractOpenApiClient {
    private final CloseableHttpClient httpClient;

    public OpenApiHttpClient(NacosClientConfig config) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(config.getMaxConnections());
        cm.setDefaultMaxPerRoute(config.getMaxPerRoute());
        // ...
    }
}
```

### 为什么这样设计

**问题：** 如果每个 API 调用都自己拼 URL、发请求、判响应——改连接池配置要改 20 个文件，加统一鉴权要改 20 个文件。

**方案：** 三层的传输隔离。接口定义"能做什么"，抽象实现"怎么做"（鉴权、异常处理），具体实现"用什么工具"（Apache HttpClient）。调用方只和 `OpenApiClient` 接口打交道。

```java
// 调用方代码——只依赖接口，不依赖实现
OpenApiClient client = new OpenApiHttpClient(config);
Instance instance = client.execute(request, Instance.class);
// 不知道内部是 Apache HttpClient、OkHttp 还是 Mock
```

### 设计原则：传递依赖的方向

OpenApiClient（接口）← AbstractOpenApiClient（抽象）← OpenApiHttpClient（具体）

依赖从具体流向抽象：OpenApiHttpClient 依赖 AbstractOpenApiClient，AbstractOpenApiClient 依赖 OpenApiClient。反过来不行。这是**依赖倒置**——高层模块（业务代码）不依赖低层模块（HTTP 实现），双方都依赖抽象（OpenApiClient 接口）。

### 什么时候可以不这样做

一个 Controller 里调一个外部 API，不需要抽象。但如果你的项目是写一个给 100 个服务用的中间件客户端，抽象传输层是必须的。

---

## 2. 序列化设计（io/ 和 OpenApiTemplateClient）

### 代码

```java
// 序列化接口，不绑定 JSON 库
public interface Serializer<T> {
    String serialize(T source) throws SerializationException;
}
public interface Deserializer<T> {
    T deserialize(String json, Type type) throws DeserializationException;
}

// Gson 实现
public class GsonDeserializer implements Deserializer<Object> { ... }

// V1/V2 响应格式差异在模板类里处理
public abstract class OpenApiTemplateClient extends AbstractClient {
    protected <T> T response(OpenApiRequest request, Type payloadType) {
        if (isOpenApiV1()) {
            return this.openApiClient.execute(request, payloadType);       // V1：直接 JSON
        }
        return this.openApiClient.executeAsResult(request, payloadType);   // V2：解包 Result<T>
    }
}

// V1 子类继承 response() 方法，V1 直接返回
public class OpenApiInstanceClient extends OpenApiTemplateClient
        implements InstanceClient { ... }

// V2 子类只重写 getOpenApiVersion()，response() 自动走 V2 路径
public class OpenApiInstanceClientV2 extends OpenApiInstanceClient {
    public OpenApiVersion getOpenApiVersion() { return V2; }
}
```

### 为什么这样设计

**问题：** Nacos 的 V1 和 V2 API 响应格式不同：

```
V1: GET /v1/ns/instance/list → {"hosts": [...], "dom": "xxx"}
V2: GET /v2/ns/instance/list → {"code": 200, "data": {"hosts": [...]}}
```

如果每个 Client 自己解析响应，升级 V2 时所有 Client 都要改。

**方案：** 在 `OpenApiTemplateClient.response()` 里统一处理。V1 子类调用 `execute()` 直接解析。V2 子类只用重写 `getOpenApiVersion()` 返回 V2，`response()` 自动走 `executeAsResult()` 解包 `Result<T>`。`register()`、`getInstancesList()` 等业务方法一行不改。

```java
// V1 和 V2 的业务代码完全一样，差异只在响应解析
// OpenApiInstanceClient.register():
OpenApiRequest request = instanceRequestBuilder(instance, POST).build();
return responseBoolean(request);  // ← responseBoolean() 在 V1/V2 中行为不同
```

### 设计原则：开闭原则 + 模板方法模式

对扩展开放（加 V2 子类即可）、对修改关闭（V1 代码不改）。通过 `OpenApiTemplateClient` 定义请求-响应处理的模板，子类通过 `getOpenApiVersion()` 定制其中一步。

`OpenApiNacosClientV2` 虽然名字叫 V2，但它的构造函数 new 的是 V1 的实现类：

```java
public OpenApiNacosClientV2(...) {
    this.instanceClient = new OpenApiInstanceClient(openApiClient, config); // V1 的类
    this.configClient   = new OpenApiConfigClient(openApiClient, config);   // V1 的类
    // ...
}
```

加上 V2 独有的客户端管理 API（`getAllClientIds`、`getClientDetail` 等——这些 API 查的是 Nacos 2.x 的客户端信息，不存在于 V1）。所以 V2 实际上是 "V1 + 额外能力"，不是"V2 endpoint 的实现"。

### 什么时候可以不这样做

只对接一个版本的 API，并且确定不会升级。否则，模板方法模式是最轻量的版本兼容方案。

---

## 3. 领域 Client 设计（common/ 和 v1/）

### 代码

Nacos REST API 有 6 个资源维度，microsphere 拆成 6 个接口：

```java
// common/discovery/InstanceClient.java（6 个核心方法）
boolean register(NewInstance newInstance);
boolean deregister(DeleteInstance instance);
Instance getInstance(QueryInstance queryInstance);
InstancesList getInstancesList(...);
Heartbeat sendHeartbeat(Instance instance);
boolean updateHealth(UpdateHealthInstance updateHealthInstance);

// common/discovery/ServiceClient.java（6 个核心方法）
boolean createService(Service service);
boolean deleteService(...);
Service getService(...);
Page<String> getServiceNames(...);

// common/config/ConfigClient.java（8 个核心方法）
String getConfigContent(...);
Config getConfig(...);
boolean publishConfig(NewConfig newConfig);
boolean deleteConfig(...);
void addEventListener(...);

// common/namespace/NamespaceClient.java（5 个核心方法）
List<Namespace> getAllNamespaces();
Namespace getNamespace(String namespaceId);
boolean createNamespace(...);
boolean updateNamespace(...);
boolean deleteNamespace(...);

// common/ServerClient.java（5 个核心方法）
// v1/raft/RaftClient.java（1 个核心方法）
```

每种操作有独立的请求模型（不是共用一个 "NacosRequest"）：

```
BaseInstance ← GenericInstance ← NewInstance / UpdateInstance / DeleteInstance / QueryInstance
```

`NewInstance` 用于注册，`QueryInstance` 用于查询，`DeleteInstance` 用于注销。每个模型只有自己需要的字段，没有多余的。

### 为什么这样设计

**问题：** Nacos SDK 用 `NamingService` 一个接口包含实例操作和服务操作。调用方只需要注册实例，却依赖了 `createService`、`selectInstances`、`subscribe` 等十几个方法。

**方案：** 1 个 REST 资源 = 1 个 Client 接口 = 1 个实现类。映射关系一目了然：

```
/nacos/v1/ns/instance  → InstanceClient  → OpenApiInstanceClient
/nacos/v1/ns/service   → ServiceClient   → OpenApiServiceClient
/nacos/v1/cs/configs   → ConfigClient    → OpenApiConfigClient
/nacos/v1/ns/namespace → NamespaceClient → OpenApiNamespaceClient
/nacos/v1/ns/operator  → ServerClient    → OpenApiServerClient
/nacos/v1/ns/raft      → RaftClient      → OpenApiRaftClient
```

操作模型独立：注册一个有 namespaceId、ip、port、weight 的实例，不需要像 `NamingService.registerInstance()` 那样传一个包含大量不相关字段的通用对象。

### 设计原则：接口隔离 + 单一职责

一个接口只对一个 REST 资源。6 个接口各管各的，互不依赖。如果需要"一个对象做所有事"，用 `OpenApiNacosClient` 组合它们：

```java
OpenApiNacosClient client = new OpenApiNacosClient(config);
client.getInstanceClient().register(instance);  // 走 InstanceClient
client.getConfigClient().getConfig(dataId);      // 走 ConfigClient
```

### 什么时候可以不这样做

应用开发者直接使用时，`NamingService` 更方便（一个注入全搞定）。框架作者应该选细粒度拆分——上层可以聚合成大门面，但大门面拆不开细接口。

---

## 4. 长轮询设计（ConfigListenerManager）

### 代码

```java
class ConfigListenerManager {
    // 三个独立线程池
    private final ExecutorService fetchingConfigsExecutor;  // 拉取配置内容（单线程）
    private final ScheduledExecutorService listeningConfigsScheduler;  // 长轮询调度（单线程）
    private final ExecutorService publishingEventExecutor;  // 回调用户监听器（单线程）

    ConfigListenerManager(...) {
        // 调度线程：每 30 秒执行一次 listen()
        this.listeningConfigsScheduler = newSingleThreadScheduledExecutor();
        listeningConfigsScheduler.scheduleAtFixedRate(
            this::listen, 30_000, 30_000, MILLISECONDS);
    }

    void listen() {
        // 1. 发 POST /v1/cs/configs/listener（挂起最多 30 秒等变更）
        String changedIds = doLongPolling();
        // 2. 有变更的配置 ID 放入队列
        if (changedIds != null) {
            for (String id : changedIds) {
                fetchingConfigIds.add(id);  // fetchingConfigsExecutor 会取走
            }
        }
        // 3. fetchingConfigsExecutor 拉取配置内容并更新缓存
        // 4. publishingEventExecutor 执行 listener.onChanged()
    }
}
```

长轮询的 HTTP 协议（Nacos 定义，不是 microsphere 发明的）：

```
POST /v1/cs/configs/listener
Body: Listening-Configs=dataId%02group%02md5%01...
Header: Long-Pulling-Timeout=30000

服务器：
  ├→ 有变更 → 立即返回 Changed dataId
  └→ 30 秒无变更 → 返回空
客户端：
  ├→ 收到变更 → GET /v1/cs/configs 拉取新配置
  └→ 收到空 → 等下次调度
```

### 为什么这样设计

**问题：** 手写 HTTP 客户端没有 gRPC 双向流，配置变更推送只能靠长轮询。但如果一个线程既做长轮询又做配置拉取又做回调，任何一个慢操作都会阻塞其他两个。

**方案：** 三个线程各司其职，`scheduleAtFixedRate` 保证调度不堆积：

```
调度线程：  POST (阻塞 28s) → 处理 → ─2s─→ POST (阻塞 28s) → ...
拉取线程：  ← wait() ─→ 拉取配置 → 更新缓存 → ← wait() ─→ ...
回调线程：  ← 取任务 ─→ listener.onChanged() → ← 取任务 ─→ ...
```

`scheduleAtFixedRate` 比 `while(true)` 更安全：如果 `listen()` 因网络异常阻塞了 38 秒，`scheduleAtFixedRate` 在结束后立即执行下一次，不会堆积线程。

### 设计原则：关注点分离（线程级别）

三个独立线程对应三个不同的延迟敏感度：
- 调度线程：必须准时（延迟影响配置变更发现时间）
- 拉取线程：可以慢（JSON 解析、网络延迟）
- 回调线程：必须隔离（用户代码可能阻塞）

### 什么时候可以不这样做

只用服务发现不做配置管理，不需要长轮询。做配置管理时可以用 gRPC 方案（官方 SDK）替代。

---

## 5. Spring Cloud 集成（discovery-spring-cloud/）

### 代码

10 个文件完成从纯 Java 客户端到 Spring Cloud 的集成：

```java
// 1. 配置绑定
@ConfigurationProperties(prefix = "microsphere.nacos.client")
public class NacosClientProperties extends NacosClientConfig {
    // NacosClientConfig 定义在 openapi 子模块（纯 POJO，无 Spring）
    // NacosClientProperties 加上 @ConfigurationProperties 注解
}

// 2. 自动装配（只在配置了 microsphere.nacos.client 前缀时生效）
@ConditionalOnPropertyPrefix("microsphere.nacos.client")
@EnableConfigurationProperties(NacosClientProperties.class)
@Import(NacosClientConfiguration.class)
public class NacosClientAutoConfiguration {}

// 3. Bean 创建
@Configuration
public class NacosClientConfiguration {
    @Bean(destroyMethod = "close")
    public OpenApiHttpClient openApiHttpClient(NacosClientConfig config) {
        return new OpenApiHttpClient(config);
    }
}

// 3. DiscoveryClient SPI 适配
public class NacosDiscoveryClient implements DiscoveryClient {
    public List<ServiceInstance> getInstances(String serviceId) {
        InstancesList list = instanceClient.getInstancesList(namespaceId, serviceId);
        return list.getHosts().stream()
                   .map(NacosServiceInstance::new)
                   .collect(Collectors.toList());
    }
    public List<String> getServices() {
        // 分页拉取所有服务名
        Page<String> page = serviceClient.getServiceNames(namespaceId, 0);
        // ... hasNext() 循环拉取全部
    }
}
```

### 没有做什么

**没有实现 `ServiceRegistry` 接口。** 这意味着 `@EnableDiscoveryClient` 能发现服务（从 Nacos 读），但不能注册自己（把当前进程注册到 Nacos）。注册需要手动调用 `InstanceClient.register()`。

对比同一项目里的 etcd 模块——它实现了 `EtcdServiceRegistry implements ServiceRegistry`，支持自动注册。

### 为什么这样设计

**问题：** 手写一个 HTTP 客户端后，怎么让 Spring Cloud 的应用用它？

**方案：** 四步走——配置绑定（读取 yaml）→ Bean 创建（new OpenApiHttpClient）→ SPI 适配（实现 DiscoveryClient）→ 多客户端（NamedContextFactory）。10 个文件全搞定。

不做 ServiceRegistry 的考虑：注册只需要启动时一次 `POST /ns/instance`（5 行代码的事），不值得为此在框架层做实现。发现需要频繁调用、连接池管理、缓存刷新——值得优化。

### 设计原则：领域和框架的分离

Nacos 领域代码（openapi 子模块）不知道 Spring 的存在。Spring 集成代码（discovery-spring-cloud 子模块）只做适配，不做业务逻辑。这保证：
- 换框架（Quarkus、Micronaut）：只重写 10 个文件
- 领域逻辑测试：不需要 Spring 上下文
- 框架版本升级：Spring Boot 3.x → 4.x，集成模块改，领域模块不动

### 什么时候可以不这样做

如果你 100% 确定只用 Spring，并且不会换框架——但"确定"在软件工程里很难成立。

---

## 总结：5 个设计问题的代码答案

| 设计问题 | 代码位置 | 答案（设计原则） |
|---------|---------|----------------|
| **传输层怎么做？** | transport/OpenApiClient + OpenApiHttpClient | 接口抽象 → 依赖倒置 |
| **V1/V2 响应格式不同怎么办？** | common/OpenApiTemplateClient.response() | 模板方法模式 → 开闭原则 |
| **API 客户端怎么组织？** | common/ + v1/ 的 *Client | 1 个 REST 资源 = 1 个接口 → 接口隔离 |
| **长轮询怎么做？** | v1/config/ConfigListenerManager | 三线程分离 → 关注点分离 |
| **Spring 集成怎么写？** | discovery-spring-cloud/ 的 10 个文件 | 领域/框架分层 → 可移植性 |
