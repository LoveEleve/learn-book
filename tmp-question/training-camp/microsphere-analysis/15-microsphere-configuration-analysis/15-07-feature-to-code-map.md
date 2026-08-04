# 15-07 功能→代码映射手册

按功能点查找对应的核心类和调用链路。

---

## 功能 1：配置中心注解声明

**功能**：用注解声明从哪个配置中心加载配置。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `@PropertySourceExtension` | `microsphere-spring-context/.../annotation/` | 元注解，定义通用属性 |
| `@NacosPorpertySource` | `nacos-spring/.../annotation/` | Nacos 注解（注意拼写） |
| `@ApolloPropertySource` | `apollo-spring/.../annotation/` | Apollo 注解 |
| `@EtcdPropertySource` | `etcd-spring/.../annotation/` | etcd 注解 |
| `@ZookeeperPropertySource` | `zookeeper-spring/.../annotation/` | ZooKeeper 注解 |

### 调用链路

```
@Configuration 类上标注 @NacosPorpertySource / @ApolloPropertySource 等
  → 注解本身标注 @PropertySourceExtension（Nacos/etcd/ZK）
  → 或标注 @EnableApolloConfig（Apollo 特有）
  → 注解本身标注 @Import(Loader/Registrar.class)
```

### 关键代码

`@PropertySourceExtension` — 元注解定义
`@NacosPorpertySource` — Nacos 注解 + `@AliasFor` 映射

### 关键注意点

1. **两条体系**：Nacos/etcd/ZK 走 `@PropertySourceExtension` 元注解体系；Apollo 走 `@EnableApolloConfig` 原生注解体系——因为 Apollo 有自己的 `PropertySourcesProcessor` 完成加载，15 不需要重复实现。这导致 Apollo 模块无法使用 `first`/`before`/`after` 排序控制。
2. **拼写**：`@NacosPorpertySource` 的 "Porperty" 是源码原始拼写，不是文档笔误。
3. **`@Inherited`**：注解标注在父类上，子类自动继承配置（`@PropertySourceExtension` 声明了 `@Inherited`）。

---

## 功能 2：注解属性包装

**功能**：将注解属性解析为带类型的对象，供 Loader 使用。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `PropertySourceExtensionAttributes<A>` | `microsphere-spring-context/.../annotation/` | 通用属性包装 |
| `NacosPropertySourceAttributes` | `nacos-spring/.../annotation/` | Nacos 特有属性 |
| `EtcdPropertySourceAttributes` | `etcd-spring/.../annotation/` | etcd 特有属性 |
| `ZookeeperPropertySourceAttributes` | `zookeeper-spring/.../annotation/` | ZK 特有属性 |
| `ResolvablePlaceholderAnnotationAttributes` | `microsphere-spring-context/.../annotation/` | 支持 `${...}` 占位符解析 |

### 调用链路

```
Loader 的 loadPropertySource()
  → createAttributes(annotationAttributes)
    → 子类构造器：new NacosPropertySourceAttributes(map, annotationType, propertyResolver)
      → 父类解析通用属性（name, autoRefreshed, first...）
      → 子类提供 getServerAddress() / getVersion() 等特有方法
```

### 关键代码

`NacosPropertySourceAttributes.java` — 50 行的属性包装示例

### 关键注意点

1. **为什么要这层包装**：`AnnotationAttributes` 是 Spring 内部类型，直接用它会导致 Loader 与 Spring 耦合。Attributes 包装层把注解属性转成类型化对象，同时支持 `${...}` 占位符解析（`ResolvablePlaceholderAnnotationAttributes`）。
2. **Apollo 没有 Attributes 类**——它用 `ResolvablePlaceholderAnnotationAttributes` 直接解析，不走 `PropertySourceExtensionAttributes` 继承体系。

---

## 功能 3：配置加载（拉取配置内容）

**功能**：从配置中心拉取配置内容，包装为 Spring Resource。

### 关键类

| 后端 | 实现类 | 拉取方式 |
|------|--------|---------|
| Nacos | `NacosPropertySourceLoader.resolveResources()` | `ConfigClient.getConfigContent(dataId)` HTTP REST |
| etcd | `EtcdPropertySourceLoader.resolveResources()` | `KV.get(key)` gRPC |
| ZooKeeper | `ZookeeperPropertySourceLoader.resolveResources()` | `client.getData().forPath(path)` ZK 协议 |
| Apollo | 不适用（Apollo 原生 `PropertySourcesProcessor` 完成） | 原生 SDK |

### 调用链路

```
PropertySourceExtensionLoader.loadPropertySource()
  → 对每个 resourceValue（Nacos 的 dataId、etcd 的 key、ZK 的 path）：
    → resolveResources(attributes, name, resourceValue)
      → 子类从配置中心拉取内容
      → 包装为 ByteArrayResource
  → 包装为 CompositePropertySource
  → 注册到 Environment
```

### 关键代码

`NacosPropertySourceLoader.java:71-80` — `resolveResources()`
`EtcdPropertySourceLoader.java:71-101` — `resolveResources()`
`ZookeeperPropertySourceLoader.java:64-84` — `resolveResources()`

### 关键注意点

1. **Nacos 只支持单 dataId**：`resolveResources()` 返回 `new Resource[1]`，注解的 `value`/`key` 数组即使配了多个也只处理第一个。多配置需要写多个 `@NacosPorpertySource`。
2. **etcd 是精确匹配**：`kv.get(key)` 未使用 `withPrefix()`，实际最多返回 1 个键值对。代码中遍历 `keyValues` 的循环是防御性写法。
3. **ZK 自动建路径**：路径不存在且 `autoRefreshed=true` 时，Loader 会先 `create()` 路径再读取。推测是预留刷新能力，但刷新实际未实现，目前无实际作用。
4. **Apollo 不适用此功能**：配置加载完全由 Apollo 原生 `PropertySourcesProcessor` 完成。

---

## 功能 4：自动刷新监听

**功能**：配置变更时自动更新 Environment，并发布事件。

### 关键类

| 后端 | 实现方式 | 监听注册点 |
|------|---------|-----------|
| Nacos | `ConfigClient.addEventListener()` | `NacosPropertySourceLoader.configureResourcePropertySourcesRefresher()` |
| etcd | `Watch.watch()` | `EtcdPropertySourceLoader.configureResourcePropertySourcesRefresher()` |
| ZooKeeper | ❌ 未实现 | — |
| Apollo | `Config.addChangeListener()` | `ApolloPropertySourceBeanDefinitionRegistrar.postProcessBeanFactory()` |

### 调用链路

```
PropertySourceExtensionLoader.loadPropertySource()
  → 如果 autoRefreshed=true
    → configureResourcePropertySourcesRefresher()
      → 后端注册监听器
        → 配置变更时回调 onConfigChanged()
          → refresher.refresh(resourceValue, resource)
            → 父类 diff 新旧内容
            → 发布 PropertySourcesChangedEvent
```

### 关键代码

`NacosPropertySourceLoader.java:82-104` — 监听注册 + `onConfigChanged()`
`EtcdPropertySourceLoader.java:103-138` — 监听注册 + `onConfigChanged()`
`ApolloPropertySourceBeanDefinitionRegistrar.java:89-119` — `postProcessBeanFactory()` 注册监听

### 关键注意点

1. **ZK 的 `autoRefreshed` 静默无效**：`ZookeeperPropertySourceLoader` 未覆盖 `configureResourcePropertySourcesRefresher()`，父类默认空实现。配置 `autoRefreshed=true` 不会报错，但配置变更后 Environment 不会更新，只能重启生效。
2. **Nacos 监听是长轮询**：`ConfigClient.addEventListener()` 通过 HTTP 长轮询实现，延迟高于 etcd 的 Watch 长连接推送。
3. **Apollo 的监听注册依赖执行顺序**：`postProcessBeanFactory()` 必须等 Apollo 原生 `PropertySourcesProcessor`（`BeanDefinitionRegistryPostProcessor`）执行完才能找到 `ApolloPropertySource`——Spring 保证 BDRPP 先于 BFPP，所以顺序可靠。

---

## 功能 5：配置变更事件发布

**功能**：配置变更时发布 `PropertySourcesChangedEvent`，供消费者监听。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `PropertySourceChangedEvent` | `microsphere-spring-context/.../env/event/` | 单个变更（ADDED/REPLACED/REMOVED） |
| `PropertySourcesChangedEvent` | `microsphere-spring-context/.../env/event/` | 聚合变更 |
| `ResourcePropertySourcesRefresher` | `microsphere-spring-context/.../annotation/` | Loader 内部刷新器 |

### 发布路径

**路径 A（Nacos/etcd/ZK 通用路径）**：

```
后端监听器回调
  → refresher.refresh()
    → diff 新旧 Resource
    → PropertySourceChangedEvent 列表
    → PropertySourcesChangedEvent 发布
```

**路径 B（Apollo 特殊路径）**：

```
ConfigChangeListener.onChanged()
  → 分类 ADDED / MODIFIED / DELETED
  → 三个 MapPropertySource
  → PropertySourceChangedEvent × 3
  → PropertySourcesChangedEvent 发布
```

### 关键代码

`PropertySourcesChangedEvent.java` — 事件定义
`ApolloPropertySourceBeanDefinitionRegistrar.java:121-166` — `onChanged()` 手动发布

### 关键注意点

1. **事件类型**：`PropertySourceChangedEvent` 的 `Kind` 枚举（ADDED/REPLACED/REMOVED）区分三种变更；`PropertySourcesChangedEvent.getChangedProperties()` 返回 ADDED+REPLACED 的属性集合。
2. **Apollo 的 diff 精度问题**：`MODIFIED` 事件用 `replaced(context, oldPropertySource, modifiedPS)`，其中 `oldPropertySource` 是**前一次快照的完整 PropertySource**，不是具体 key 的旧值——消费者无法定位到单个 key 的旧值。
3. **事件作用域**：继承 `ApplicationContextEvent`，只在发布它的 ApplicationContext 内传播。在 18-dynamic 多层上下文中，子上下文发布的事件父上下文监听器收不到。

---

## 功能 6：客户端管理

**功能**：管理配置中心客户端连接的生命周期。

### 关键类

| 后端 | 缓存字段 | 关闭方式 |
|------|---------|---------|
| Nacos | `configCientCache`（static HashMap） | ShutdownHook 关闭所有客户端 |
| etcd | `clientsCache`（static HashMap） | ShutdownHook 关闭所有客户端 |
| ZooKeeper | `clientsCache`（static HashMap） | ShutdownHook 关闭所有 CuratorFramework |
| Apollo | 无（Apollo 原生管理） | Apollo 原生 |

### 调用链路

```
类加载（static 块）
  → new HashMap<>()
  → addShutdownHookCallback(关闭所有客户端)

第一次使用
  → computeIfAbsent(name, createClient)
    → Nacos: new OpenApiNacosClient(config)
    → etcd: Client.builder().endpoints().build()
    → ZK: CuratorFrameworkFactory.builder().connectString().build()
```

### 关键代码

`NacosPropertySourceLoader.java:50-69` — static 块 + ShutdownHook
`EtcdPropertySourceLoader.java:56-69` — static 块 + ShutdownHook
`ZookeeperPropertySourceLoader.java:49-62` — static 块 + ShutdownHook

### 关键注意点

1. **缓存是 HashMap**：三个模块都用 `HashMap` + `computeIfAbsent`（非 `ConcurrentHashMap`）。JDK 8+ 的 `computeIfAbsent` 是原子的，JDK 7 上存在并发重复创建客户端的问题。
2. **Nacos 的 ShutdownHook 类型转换**：`((OpenApiTemplateClient) value).getOpenApiClient().close()` 假设所有 `ConfigClient` 实现都是 `OpenApiTemplateClient` 子类，未来新增实现会抛 `ClassCastException`。
3. **缓存 key 不同**：Nacos/etcd 按 `attributes.getName()`（属性名）缓存；ZK 按 `connectString` 缓存——两个 ZK 配置共用同一 connectString 会共享客户端。

---

## 功能 7：Apollo 特有的 System property 设置

**功能**：将注解属性写入 System properties，供 Apollo 客户端读取。

### 关键类

`ApolloPropertySourceBeanDefinitionRegistrar`

### 调用链路

```
registerBeanDefinitions()
  → setSystemPropertiesFromAttributes()
    → setSystemProperty(System.getProperties(), APP_ID, appId)
    → setSystemProperty(System.getProperties(), APOLLO_META, meta)
    → setSystemProperty(System.getProperties(), APOLLO_CLUSTER, cluster)
    → setSystemProperty(System.getProperties(), APOLLO_BOOTSTRAP_NAMESPACES, namespace)
```

### 写入的 System properties

| Key | 来源 |
|-----|------|
| `app.id` | `@ApolloPropertySource.appId()` |
| `apollo.meta` | `@ApolloPropertySource.meta()` |
| `apollo.cluster` | `@ApolloPropertySource.cluster()` |
| `apollo.bootstrap.namespaces` | `@ApolloPropertySource.namespace()` |

### 关键代码

`ApolloPropertySourceBeanDefinitionRegistrar.java:179-202` — `setSystemPropertiesFromAttributes()` + `setSystemProperty()`

### 关键注意点

1. **为什么需要 System property**：Apollo 客户端通过 `System.getProperty("app.id")`、`System.getProperty("apollo.meta")` 等读取连接配置，不从 Spring Environment 读——所以必须写入 System properties。
2. **`contains()` 的 bug**：`systemProperties.contains(key)` 检查的是**值**是否存在（`Hashtable.contains(Object value)`），不是**键**。应该用 `containsKey(key)`。当前代码"不覆盖已有配置"的防御实际无效。
3. **不覆盖策略**：`setSystemProperty()` 在 System property 已存在时不覆盖——命令行 `-Dapp.id=xxx` 的优先级高于注解。

---

## 功能 8：与 18-dynamic 的事件联动

**功能**：配置变更事件触发 DynamicJdbcConfig 热更新。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `PropagatingDynamicJdbcConfigChangedEventListener` | `microsphere-dynamic/.../context/` | 监听配置变更 + Zone 变更 |
| `DynamicJdbcConfigChangedEvent` | `microsphere-dynamic/.../context/` | 配置变更内部事件 |
| `RefreshingDynamicDataSourceListener` | `microsphere-dynamic/.../datasource/` | DataSource 重建 |

### 调用链路

```
PropertySourcesChangedEvent（15 发布）
  → PropagatingDynamicJdbcConfigChangedEventListener.onPropertySourcesChangedEvent()
    → 检查 key 匹配 microsphere.dynamic.jdbc.configs.*
    → 重新解析 JSON
    → 发布 DynamicJdbcConfigChangedEvent
      → RefreshingDynamicDataSourceListener
        → initializeDataSource() 重建
```

### 关键代码

`PropagatingDynamicJdbcConfigChangedEventListener.java:63-71` — `onPropertySourcesChangedEvent()`

### 关键注意点

1. **监听两种事件**：该监听器同时处理 `PropertySourcesChangedEvent`（15 配置变更）和 `ZoneContextChangedEvent`（17 Zone 切换）。Zone 切换链路不经过配置中心。
2. **只响应特定前缀**：仅 `microsphere.dynamic.jdbc.configs.*` 前缀的 key 变更触发重建；修改其他配置被忽略。
3. **`supportsEventType()` 用 `equals()` 精确匹配**：未来若发布 `PropertySourcesChangedEvent` 子类事件不会被收到。
4. **事件必须在根上下文发布**：监听器注册在根上下文，子上下文发布的事件收不到——15 的注解应标注在根上下文的配置类上。

---

## 功能索引：按类名查找

| 类名 | 负责的功能 |
|------|-----------|
| `@PropertySourceExtension` | 元注解定义（所有后端注解的基础） |
| `PropertySourceExtensionAttributes` | 通用属性包装 |
| `PropertySourceExtensionLoader` | 模板方法（加载 + 刷新） |
| `AnnotatedPropertySourceLoader` | ImportSelector 中间层 |
| `AnnotatedBeanCapableImportSelector` | ImportSelector 基类 |
| `BeanCapableImportCandidate` | Spring 生命周期支持 |
| `PropertySourceChangedEvent` | 单个变更事件 |
| `PropertySourcesChangedEvent` | 聚合变更事件 |
| `ResolvablePlaceholderAnnotationAttributes` | 占位符解析 |
| `@NacosPorpertySource` | Nacos 注解（拼写错误） |
| `NacosPropertySourceAttributes` | Nacos 属性包装 |
| `NacosPropertySourceLoader` | Nacos 加载器 + 监听 |
| `@ApolloPropertySource` | Apollo 注解 |
| `ApolloPropertySourceBeanDefinitionRegistrar` | Apollo 注册器 + System property + 监听 |
| `@EtcdPropertySource` | etcd 注解 |
| `EtcdPropertySourceAttributes` | etcd 属性包装 |
| `EtcdPropertySourceLoader` | etcd 加载器 + Watch |
| `@ZookeeperPropertySource` | ZK 注解 |
| `ZookeeperPropertySourceAttributes` | ZK 属性包装 |
| `ZookeeperPropertySourceLoader` | ZK 加载器（无刷新） |
| `PropagatingDynamicJdbcConfigChangedEventListener` | 18-dynamic 的事件消费者 |
