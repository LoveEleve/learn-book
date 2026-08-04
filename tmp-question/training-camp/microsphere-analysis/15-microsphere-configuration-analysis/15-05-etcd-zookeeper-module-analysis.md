# 15-05 etcd + ZooKeeper 模块源码解析

## 目录

- [模块概述](#模块概述)
- [etcd 模块](#etcd-模块)
- [ZooKeeper 模块](#zookeeper-模块)
- [两个模块的对比](#两个模块的对比)
- [四个后端模块横向对比](#四个后端模块横向对比)
- [工程问题分析](#工程问题分析)

---

## 模块概述

etcd 和 ZooKeeper 模块是四个后端中最简单的两个。它们都通过 `PropertySourceExtensionLoader` 模板方法实现，结构一致：注解（~50行）+ Attributes（~50行）+ Loader（100-170行）。

| 模块 | 文件数 | 客户端依赖 | Loader 行数 | 自动刷新 |
|------|--------|-----------|------------|---------|
| etcd | 3 | `io.etcd:jetcd-core` | 172 | ✅ Watch |
| ZooKeeper | 3 | `org.apache.curator:curator-recipes` | 115 | ❌ 未实现 |

两模块都复用了原生 SDK（Jetcd、Curator），没有自实现客户端。

---

## etcd 模块

### 文件结构

| 文件 | 行数 | 职责 |
|------|------|------|
| `EtcdPropertySource.java` | ~60 | 注解 |
| `EtcdPropertySourceAttributes.java` | ~50 | 属性包装 |
| `EtcdPropertySourceLoader.java` | 172 | 加载器 + Watch 监听 |

### EtcdPropertySource 注解

```java
@PropertySourceExtension
@Import(EtcdPropertySourceLoader.class)
public @interface EtcdPropertySource {
    // @PropertySourceExtension 通用属性（name, autoRefreshed, first, before, after 等）
    // etcd 特有属性
    String[] endpoints() default {"http://127.0.0.1:2379"};
    String username() default "";
    String password() default "";
}
```

### EtcdPropertySourceLoader

#### resolveResources()——拉取配置

```java
@Override
protected Resource[] resolveResources(EtcdPropertySourceAttributes attrs,
        String propertySourceName, String resourceValue) throws Throwable {

    Client client = getClient(attrs);
    KV kv = client.getKVClient();
    ByteSequence key = toByteSequence(resourceValue, attrs);
    GetResponse getResponse = kv.get(key).get();
    List<KeyValue> keyValues = getResponse.getKvs();

    if (keyValues.isEmpty()) return null;

    Resource[] resources = new Resource[keyValues.size()];
    Charset charset = Charset.forName(attrs.getEncoding());
    for (int i = 0; i < keyValues.size(); i++) {
        KeyValue kv = keyValues.get(i);
        resources[i] = new ByteArrayResource(kv.getValue().getBytes(), kv.getKey().toString(charset));
    }
    return resources;
}
```

| 步骤 | 说明 |
|------|------|
| `client.getKVClient()` | 从 Jetcd Client 获取 KV 操作接口 |
| `kv.get(key)` | 异步调用 etcd 的 KV 查询，`future.get()` 阻塞等待 |
| `keyValues` | `kv.get()` 是精确匹配——返回 0 或 1 个键值对。代码中遍历 `keyValues` 的循环是防御性写法，实际 size 最多为 1 |
| 返回 `Resource[]` | 每个 key 对应一个 Resource，description 为 key 名 |

#### configureResourcePropertySourcesRefresher()——Watch 监听

```java
@Override
protected void configureResourcePropertySourcesRefresher(EtcdPropertySourceAttributes attrs, ...) {
    Client client = getClient(attrs);
    Watch watchClient = client.getWatchClient();
    Charset charset = Charset.forName(attrs.getEncoding());

    for (PropertySourceResource resource : propertySourceResources) {
        String resourceValue = resource.getResourceValue();
        ByteSequence key = toByteSequence(resourceValue, attrs);
        watchClient.watch(key, response -> {
            response.getEvents().forEach(event -> onConfigChanged(event, charset, refresher));
        });
    }
}

private void onConfigChanged(WatchEvent event, Charset charset, ResourcePropertySourcesRefresher refresher) {
    if (WatchEvent.EventType.PUT.equals(event.getEventType())) {
        KeyValue keyValue = event.getKeyValue();
        String resourceValue = keyValue.getKey().toString(charset);
        ByteArrayResource resource = new ByteArrayResource(keyValue.getValue().getBytes());
        refresher.refresh(resourceValue, resource);
    }
}
```

etcd 的 Watch 机制是**长连接推送**——一旦注册 Watch，etcd 服务端在有变更时主动推送给客户端。相比于 Nacos 的长轮询（客户端定期拉取），Watch 的延迟更低（实时推送）。

#### getClient()——客户端创建

```java
private Client getClient(EtcdPropertySourceAttributes attrs) {
    String key = attrs.getName();
    return clientsCache.computeIfAbsent(key, k -> {
        ClientBuilder clientBuilder = Client.builder();
        String target = attrs.getTarget();
        if (StringUtils.hasText(target)) {
            // clientBuilder.target(target);        // ← 被注释掉的代码
        } else {
            clientBuilder.endpoints(attrs.getEndpoints());
        }
        // TODO support more settings                   // ← 未实现
        return clientBuilder.build();
    });
}
```

这里有一个隐蔽的 bug：

```java
String target = attrs.getTarget();
if (StringUtils.hasText(target)) {
    // clientBuilder.target(target);   // ← 被注释掉
} else {
    clientBuilder.endpoints(attrs.getEndpoints());
}
```

如果用户配置了 `target` 属性：**既不调用 `target()` 也不调用 `endpoints()`**——`Client.builder().build()` 会使用 Jetcd 默认的 `http://localhost:2379`，静默连错地址。如果没配 `target`：正常走 `endpoints`。

另外 `// TODO support more settings` 意味着 `username`/`password` 认证尚未实现。

---

## ZooKeeper 模块

### 文件结构

| 文件 | 行数 | 职责 |
|------|------|------|
| `ZookeeperPropertySource.java` | ~50 | 注解 |
| `ZookeeperPropertySourceAttributes.java` | ~50 | 属性包装 |
| `ZookeeperPropertySourceLoader.java` | 115 | 加载器（无刷新） |

### ZookeeperPropertySource 注解

```java
@PropertySourceExtension
@Import(ZookeeperPropertySourceLoader.class)
public @interface ZookeeperPropertySource {
    // @PropertySourceExtension 通用属性
    // ZK 特有属性
    String connectString() default "";
    int sessionTimeout() default 60000;
    int connectionTimeout() default 15000;
}
```

### ZookeeperPropertySourceLoader

#### resolveResources()——拉取配置

```java
@Override
protected Resource[] resolveResources(ZookeeperPropertySourceAttributes attrs,
        String propertySourceName, String resourceValue) throws Throwable {

    CuratorFramework client = getClient(attrs);

    boolean pathNotExisted = client.checkExists().forPath(resourceValue) == null;
    boolean autoRefreshed = attrs.isAutoRefreshed();

    if (pathNotExisted) {
        if (!autoRefreshed) return null;
        client.create().forPath(resourceValue);    // ← 路径不存在时自动创建
    }

    byte[] bytes = client.getData().forPath(resourceValue);
    return ArrayUtils.of(new ByteArrayResource(bytes, "path: " + resourceValue));
}
```

| 设计 | 说明 |
|------|------|
| 路径不存在时`autoRefreshed=true`则自动创建 | 推测是预留刷新能力——Watch 需要路径先存在。但实际刷新未实现，这个自动创建目前没有实际作用 |
| `autoRefreshed=false`且路径不存在则返回 null | 跳过配置加载 |
| 返回单一 Resource | ZK 的 `getData()` 只查单个路径 |

#### 没有 configureResourcePropertySourcesRefresher()

`ZookeeperPropertySourceLoader` **没有覆盖** `configureResourcePropertySourcesRefresher()`。父类的默认实现是空方法。这意味着：

- **ZooKeeper 不支持自动刷新**
- 即使 `autoRefreshed=true`，配置变更后 Environment 不会更新
- 只能通过重启应用来加载新配置

```java
public abstract class PropertySourceExtensionLoader<A, EA> {
    protected void configureResourcePropertySourcesRefresher(...) {
        // 默认空实现——ZooKeeper 没有覆盖
    }
}
```

#### getClient()——客户端创建

```java
private CuratorFramework getClient(ZookeeperPropertySourceAttributes attrs) {
    String connectString = attrs.getConnectString();
    return clientsCache.computeIfAbsent(connectString, k -> {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new RetryForever(300))
                .build();
        client.start();
        return client;
    });
}
```

注意：`sessionTimeout` 和 `connectionTimeout` 属性虽然定义在注解中，但在 `getClient()` 中没有使用——Curator 客户端使用的是默认值。

---

## 两个模块的对比

| 维度 | etcd | ZooKeeper |
|------|------|-----------|
| Loader 行数 | 172 | 115 |
| 自动刷新 | ✅ Watch 长连接推送 | ❌ 未实现 |
| 刷新机制 | `Watch.watch()` + `WatchEvent` | 无 |
| 前缀查询 | ❌ `kv.get()` 是精确匹配（未用 withPrefix） | ❌ 只查单路径 |
| 路径不存在时的行为 | 返回空 | `autoRefreshed=true` 时自动创建路径 |
| 已注释/未实现的代码 | `target()` 注释、`TODO support more settings` | `sessionTimeout`、`connectionTimeout` 未使用 |
| 客户端创建 | `Client.builder().endpoints()` | `CuratorFrameworkFactory.builder().connectString()` |
| ShutdownHook | ✅ 关闭 Client | ✅ 关闭 CuratorFramework |

### 实现质量对比

```
etcd Loader:   [░░░░░░░░░░░░░░░░░░░░] 172 行
  实现了解析、Watch 刷新，但认证未实现、target 废弃

ZK Loader:     [░░░░░░░░░░░] 115 行
  实现了基本加载，但刷新未实现、超时参数未使用
```

---

## 四个后端模块横向对比

| 维度 | Nacos | Apollo | etcd | ZooKeeper |
|------|-------|--------|------|-----------|
| **复用 SDK** | ❌ 自实现 | ✅ 官方 client | ✅ Jetcd | ✅ Curator |
| **自动刷新** | ✅ 长轮询 | ✅ ConfigChangeListener | ✅ Watch | ❌ 未实现 |
| **加载器行数** | 121 | 208（Registrar） | 172 | 115 |
| **Load 方式** | HTTP REST | 原生 Processor | gRPC KV | ZK 协议 |
| **配置格式** | dataId 对应单一配置 | namespace 多配置 | key 可前缀查询 | path 单一节点 |
| **特有风险** | REST API 兼容性 | BeanFactoryPostProcessor 顺序 | 认证未实现 | 刷新未实现 |
| **客户端缓存** | `configCientCache`(HashMap) | 无（用 Apollo 的） | `clientsCache`(HashMap) | `clientsCache`(HashMap) |
| **ShutdownHook** | ✅ | ✅（Apollo 原生） | ✅ | ✅ |

---

## 工程问题分析

### etcd 认证未实现

`EtcdPropertySourceLoader` 中有 `// TODO support more settings`，且 `username`/`password` 属性虽然在注解中定义了，但 Loader 没有读取它们。如果需要认证的 etcd 集群，这个模块无法使用。

### ZooKeeper 刷新能力的缺失

`ZookeeperPropertySourceLoader` 没有实现 `configureResourcePropertySourcesRefresher()`，即使 `autoRefreshed=true` 也不会注册 Watch。要实现的话，需要添加 Curator 的 `CuratorCache` 监听：

```java
// 理论上应该添加的代码（但实际没有）
CuratorCache cache = CuratorCache.build(client, resourceValue);
cache.listenable().addListener((type, oldData, data) -> {
    if (type == CuratorCacheListener.Type.NODE_CHANGED) {
        ByteArrayResource resource = new ByteArrayResource(data.getData());
        refresher.refresh(resourceValue, resource);
    }
});
cache.start();
```

### 缓存线程安全性

与 Nacos 模块一样，etcd 和 ZK 的客户端缓存也是 `HashMap` + `computeIfAbsent`。在 JDK 7 上存在并发问题。

### etcd 的 target 配置是陷阱

`ClientBuilder.target(target)` 被注释了，但 `EtcdPropertySourceAttributes` 中仍然有 `getTarget()` 方法。如果用户配置了 `target` 属性：代码既不调用 `target()` 也不调用 `endpoints()`，Client 使用 Jetcd 默认的 `http://localhost:2379`——**静默连错地址**，且不会报错。这个配置项是废弃的，但注解和 Attributes 没有移除它，用户配置了也不会有任何提示。
