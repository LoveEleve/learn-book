# 15-01 项目定位、使用与工程分析

## 目录

- [这是个什么项目](#这是个什么项目)
- [4 个子模块](#4-个子模块)
- [与原生配置中心 SDK 的对比](#与原生配置中心-sdk-的对比)
- [快速开始：从零集成](#快速开始从零集成)
- [完整配置参考手册](#完整配置参考手册)
- [核心概念：元注解体系](#核心概念元注解体系)
- [工程问题分析](#工程问题分析)

---

## 这是个什么项目

`microsphere-configuration` 是一个 **Spring 注解驱动的配置框架**，提供统一的编程模型对接四个配置中心：Nacos、Apollo、etcd、ZooKeeper。

**一句话定位**：用同一套注解属性（`autoRefreshed`、`first`、`before`、`after`）操作四个不同的配置中心，配置变更时统一发布 `PropertySourcesChangedEvent`。

### 它解决的核心问题

Spring 的 `@PropertySource` 只能加载本地资源文件（classpath: 或 file:），不支持配置中心。而 Nacos、Apollo、etcd、ZK 各自有独立的注解体系和监听机制：

```java
// Nacos 原生
@NacosPropertySource(dataId = "app.yml", autoRefreshed = true)

// Apollo 原生
@EnableApolloConfig(namespaces = {"application"})

// etcd — 没有 Spring 注解，需要手动写 Jetcd 代码

// ZooKeeper — 没有 Spring 注解，需要手动写 Curator 代码
```

每个方案各写各的，切换配置中心时需要改大量代码。15-configuration 把这四个统一了：

```java
// Nacos
@NacosPorpertySource(key = "app.json", autoRefreshed = true)

// Apollo  
@ApolloPropertySource(appId = "my-app", namespace = "application", autoRefreshed = true)

// etcd
@EtcdPropertySource(key = "app.json", autoRefreshed = true)

// ZooKeeper
@ZookeeperPropertySource(path = "/config/app.json", autoRefreshed = true)

// 四者的 autoRefreshed / first / before / after 语义完全一致
```

### 它不是什么

- **不是 Nacos/Apollo 的官方客户端 SDK**——Nacos 部分自实现了 REST API 客户端（`OpenApiNacosClient`），没有用 `com.alibaba.nacos` 的 SDK
- **不是 Spring Cloud Config 的替代品**——它不提供配置服务端，只做客户端侧的配置加载和监听
- **不是配置中心**——它是个 Starter，依赖已有的配置中心基础设施

---

## 4 个子模块

| 子模块 | 文件数 | 后端依赖 | 连接方式 |
|--------|-------|---------|---------|
| `nacos-spring` | 3 个 Java 文件 | 无（自实现 `OpenApiNacosClient`） | HTTP REST API（V1/V2） |
| `apollo-spring` | 2 个 Java 文件 | `com.ctrip.framework.apollo:apollo-client:1.9.2` | 复用 Apollo 原生 ConfigPropertySource |
| `etcd-spring` | 3 个 Java 文件 | `io.etcd:jetcd-core` | Jetcd gRPC 客户端 |
| `zookeeper-spring` | 3 个 Java 文件 | `org.apache.curator:curator-recipes` | Curator Framework |

### 依赖关系

```
microsphere-configuration
  ├── nacos-spring     （无外部 SDK 依赖，自实现 HTTP 客户端）
  ├── apollo-spring    （依赖 apollo-client 1.9.2）
  ├── etcd-spring      （依赖 jetcd-core）
  └── zookeeper-spring （依赖 curator-recipes）

所有模块共同依赖：
  microsphere-spring-context
    → @PropertySourceExtension（元注解）
    → PropertySourceExtensionLoader（抽象加载器）
    → PropertySourcesChangedEvent（事件类型）
```

### 各模块规模

| 模块 | 核心文件 | 总行数 |
|------|---------|--------|
| nacos-spring | `NacosPorpertySource.java` + `NacosPropertySourceAttributes.java` + `NacosPropertySourceLoader.java` | ~300 |
| apollo-spring | `ApolloPropertySource.java` + `ApolloPropertySourceBeanDefinitionRegistrar.java` | ~250 |
| etcd-spring | `EtcdPropertySource.java` + `EtcdPropertySourceAttributes.java` + `EtcdPropertySourceLoader.java` | ~250 |
| zookeeper-spring | `ZookeeperPropertySource.java` + `ZookeeperPropertySourceAttributes.java` + `ZookeeperPropertySourceLoader.java` | ~250 |

每个模块都很轻量，核心逻辑都在各自 Loader 的 80-120 行中。

---

## 与原生配置中心 SDK 的对比

### Nacos 对比

| 维度 | 原生 Nacos Spring Boot | 15 nacos-spring |
|------|----------------------|-----------------|
| 注解 | `@NacosPropertySource` | `@NacosPorpertySource`（注意拼写，原始就这样） |
| 客户端 | `com.alibaba.nacos: nacos-client`（完整 SDK） | 自实现 `OpenApiNacosClient`（仅 REST API） |
| 监听 | `ConfigService.addListener()` | `ConfigClient.addEventListener()` |
| 刷新 | `@NacosValue` 自动更新 | 发布 `PropertySourcesChangedEvent` |
| 优势 | 官方维护，功能完整 | 无外部依赖，轻量 |

15 的 Nacos 模块自实现了 REST 客户端，不走 gRPC 长连接，不依赖 Nacos 的 `ConfigService`。这意味着它不需要 Nacos 客户端的复杂依赖树，配置监听通过 HTTP 长轮询实现，但不支持 Nacos 的 gRPC 推送。

### Apollo 对比

| 维度 | 原生 Apollo | 15 apollo-spring |
|------|-----------|-----------------|
| 注解 | `@EnableApolloConfig` | `@ApolloPropertySource` |
| 客户端 | `apollo-client` | 复用 `apollo-client` 的 `Config` 对象 |
| 监听 | `ConfigChangeListener` | `ConfigChangeListener` → 转 `PropertySourcesChangedEvent` |
| 刷新 | `@Value` + `@ApolloConfigChangeListener` | `PropertySourcesChangedEvent` |

Apollo 模块是唯一一个**复用了原生 SDK** 的模块——它直接使用 `apollo-client` 的 `Config` 和 `ConfigChangeListener`，只是在外层加了一层统一注解和事件转换。

### 对比总结

| 模块 | 是否复用原生 SDK | 连接方式 | 风险 |
|------|----------------|---------|------|
| nacos | ❌ 自实现 | HTTP REST | Nacos 服务端 API 版本变动可能不兼容 |
| apollo | ✅ 复用 | 原生 SDK | 低 |
| etcd | ✅ 复用 Jetcd | gRPC | 低 |
| zookeeper | ✅ 复用 Curator | TCP | 低 |

---

## 快速开始：从零集成

### Maven 依赖

```xml
<!-- Nacos -->
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-configuration-nacos-spring</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- Apollo -->
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-configuration-apollo-spring</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- etcd -->
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-configuration-etcd-spring</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- ZooKeeper -->
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-configuration-zookeeper-spring</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Nacos 使用示例

```java
@Configuration
@NacosPorpertySource(
    key = "application.json",
    serverAddress = "http://192.168.0.111:8848",
    openApiVersion = OpenApiVersion.V2,
    autoRefreshed = true          // ← 开启自动刷新
)
public class AppConfig {
}

@Component
public class MyService {
    @Value("${my.name}")
    private String name;

    public void doSomething() {
        System.out.println(name);
        // Nacos 中修改 my.name → PropertySourcesChangedEvent 发布
        // @Value 不会自动更新（需要 @RefreshScope 或手动监听事件）
    }
}
```

### Apollo 使用示例

```java
@Configuration
@ApolloPropertySource(
    appId = "my-app",
    meta = "http://192.168.0.110:8080",
    namespace = "application",
    autoRefreshed = true
)
public class AppConfig {
}
```

### etcd 使用示例

```java
@Configuration
@EtcdPropertySource(
    key = "/config/app.json",
    endpoints = {"http://127.0.0.1:2379"},
    autoRefreshed = true
)
public class AppConfig {
}
```

### ZooKeeper 使用示例

```java
@Configuration
@ZookeeperPropertySource(
    connectString = "127.0.0.1:2181",
    path = "/config/app.json",
    autoRefreshed = true
)
public class AppConfig {
}
```

### 监听配置变更

```java
@Component
public class MyConfigListener
        implements ApplicationListener<PropertySourcesChangedEvent> {

    @Override
    public void onApplicationEvent(PropertySourcesChangedEvent event) {
        Map<String, Object> changed = event.getChangedProperties();
        // 处理变更：刷新缓存、重建连接等
    }
}
```

---

## 完整配置参考手册

所有四个后端的注解共享同一套 `@PropertySourceExtension` 元注解属性：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 自动生成 | PropertySource 名称 |
| `autoRefreshed` | boolean | 元注解 `false`；`@NacosPorpertySource` 和 `@ApolloPropertySource` 默认 `true` | 是否自动刷新 |
| `first` | boolean | `false` | 是否放到 Environment 第一位 |
| `before` | String | `""` | 在此 PropertySource 之前 |
| `after` | String | `""` | 在此 PropertySource 之后 |
| `value` / `key` | String[] | `{}` | 资源 key（Nacos 的 dataId、etcd 的 key、ZK 的 path） |
| `encoding` | String | `"UTF-8"` | 字符编码 |
| `factory` | Class | `DefaultPropertySourceFactory` | PropertySource 工厂 |
| `ignoreResourceNotFound` | boolean | `false` | 找不到资源时是否忽略 |
| `resourceComparator` | Class | `DefaultResourceComparator` | 资源排序器 |

### Nacos 特有属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `serverAddress` | String | `http://127.0.0.1:8848` | Nacos 服务端地址 |
| `openApiVersion` | enum | `V1` | OpenAPI 版本（V1/V2） |

### Apollo 特有属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `appId` | String | — | Apollo AppId |
| `meta` | String | — | Apollo Meta Server 地址 |
| `namespace` | String | `"application"` | 命名空间 |
| `cluster` | String | `"default"` | 集群名 |

### etcd 特有属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `endpoints` | String[] | `{"http://127.0.0.1:2379"}` | etcd 节点地址 |
| `username` | String | — | 认证用户名 |
| `password` | String | — | 认证密码 |

### ZooKeeper 特有属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `connectString` | String | — | ZK 连接字符串 |
| `sessionTimeout` | int | `60000` | 会话超时 |
| `connectionTimeout` | int | `15000` | 连接超时 |

---

## 核心概念：元注解体系

### `@PropertySourceExtension`

定义在 `microsphere-spring-context` 中，是整个框架的基石：

```java
@Target(ANNOTATION_TYPE)    // 只能用来标注其他注解
@Retention(RUNTIME)
@Inherited                   // 子类继承父类的配置
@Documented
public @interface PropertySourceExtension {
    String name() default "";
    boolean autoRefreshed() default false;
    boolean first() default false;
    String before() default "";
    String after() default "";
    String[] value() default {};
    // ...
}
```

**Nacos/etcd/ZK 三个后端**的注解（`@NacosPorpertySource`、`@EtcdPropertySource`、`@ZookeeperPropertySource`）标注了 `@PropertySourceExtension`，并通过 `@AliasFor` 将属性映射到元注解上。**Apollo 不在此列**——它使用 `@EnableApolloConfig`（Apollo 原生注解），不经过 `@PropertySourceExtension` 体系。

### 加载流程

```
Spring 启动
  → 处理 @Configuration 类
  → 发现 @NacosPorpertySource / @EtcdPropertySource / @ZookeeperPropertySource
    → @Import(对应 Loader.class) 触发 ImportSelector
      → Loader 继承 PropertySourceExtensionLoader
        → resolveResources() 从配置中心获取配置内容
        → 包装为 ByteArrayResource
        → 创建 CompositePropertySource
        → 注册到 Environment
        → 如果 autoRefreshed=true
          → configureResourcePropertySourcesRefresher() 注册监听器
          → 配置变更时 → refresher.refresh() → PropertySourcesChangedEvent
  → 发现 @ApolloPropertySource
    → @Import(ApolloPropertySourceBeanDefinitionRegistrar.class) 触发 ImportBeanDefinitionRegistrar
      → 设置 System properties → Apollo 原生 PropertySourcesProcessor 加载配置
      → BeanFactoryPostProcessor 阶段注册 ConfigChangeListener → 转 PropertySourcesChangedEvent
```

### PropertySourceExtensionLoader 模板方法

```java
public abstract class PropertySourceExtensionLoader<A, EA> {

    // 子类实现的抽象方法
    protected abstract Resource[] resolveResources(EA attributes, String name, String value) throws Throwable;

    protected void configureResourcePropertySourcesRefresher(
            EA attributes, List<PropertySourceResource> resources,
            CompositePropertySource propertySource,
            ResourcePropertySourcesRefresher refresher) throws Throwable {
        // 默认不做刷新配置，子类可覆盖
    }

    // 模板方法：加载流程
    public void loadPropertySource(AnnotationAttributes attributes, ...) {
        // 1. 解析注解属性
        // 2. 调用 resolveResources() 获取配置内容
        // 3. 创建 CompositePropertySource 注册到 Environment
        // 4. 如果 autoRefreshed，调用 configureResourcePropertySourcesRefresher()
    }
}
```

四个后端的 Loader 只需要实现 `resolveResources()`（从配置中心拉取内容）和可选的 `configureResourcePropertySourcesRefresher()`（注册监听器）。

---

## 工程问题分析

### 自实现 Nacos 客户端的风险

Nacos 模块自实现了 `OpenApiNacosClient`，通过 HTTP REST API 与 Nacos 服务端通信。这不依赖 Nacos 官方的 `nacos-client` SDK，但也意味着：

1. **API 版本兼容性**——Nacos 服务端的 OpenAPI 在不同版本间可能变化，自实现客户端需要跟随升级
2. **没有长轮询**——官方 SDK 使用长轮询 + gRPC 推送，自实现只支持简单的 HTTP 查询 + 事件监听
3. **没有服务发现**——官方 SDK 内置了 Nacos 集群的服务发现和故障转移，自实现没有

### Apollo 模块的特殊性

唯一一个复用了原生 SDK 的模块。`ApolloPropertySourceBeanDefinitionRegistrar` 直接使用 `apollo-client` 的 `ConfigService.getConfig()` 获取配置，通过 `config.addChangeListener()` 监听变更。这是因为 Apollo 的客户端设计已经非常模块化，复用成本低。

### 注解拼写错误

`@NacosPorpertySource`——原始代码中就是这样的拼写（"Porperty" 而非 "Property"）。注解文件的 `@author` 标注为"Walklown"，Loader 文件的 `@author` 标注为"Mercy"（同一个邮箱 `walklown@gmail.com`）。这不影响使用，但引用这个注解时要小心拼写。

### 事件订阅的缺失（与 18-dynamic 的关系）

虽然 15 在配置变更时发布 `PropertySourcesChangedEvent`，但 15 本身**没有监听自己的事件**来触发 `ZoneContext.setZone()`。这个缺口在 17-08 中已详细分析——需要额外组件来监听事件并调用 `setZone()`。

### 元注解的 `@Inherited` 设计

`@PropertySourceExtension` 标注了 `@Inherited`，但 Java 的 `@Inherited` 只对超类继承生效，对接口实现不生效。这意味着：

```java
@NacosPorpertySource(key = "app.json")  // 标注在父类上
public class BaseConfig {}

public class SubConfig extends BaseConfig {}  // 继承生效 ✅

public class ImplConfig implements SomeInterface {}  // 接口不生效 ❌
```

这在设计上是合理的——配置通常放在 `@Configuration` 类上，而 `@Configuration` 类通常有继承关系。

### 四个模块的代码量对比

| 模块 | Loader/Registrar 行数 | 监听实现 | 客户端 |
|------|---------------------|---------|--------|
| nacos | ~120 | `ConfigClient.addEventListener()`（HTTP 长轮询） | 自实现 HTTP |
| apollo | ~208（Registrar） | `Config.addChangeListener()` | 复用 apollo-client |
| etcd | ~100 | `Client.watch()` Jetcd Watch | 复用 jetcd |
| zookeeper | ~100 | ❌ 未实现（`autoRefreshed` 静默无效） | 复用 curator |

Nacos Loader 最长不是因为逻辑复杂，而是因为自实现了客户端连接池管理（`configCientCache` + ShutdownHook）。
