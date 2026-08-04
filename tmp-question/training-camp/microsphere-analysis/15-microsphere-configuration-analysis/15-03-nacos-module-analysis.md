# 15-03 Nacos 模块源码解析

## 目录

- [模块概述](#模块概述)
- [NacosPorpertySource 注解](#nacosporpertysource-注解)
- [NacosPropertySourceAttributes 属性包装](#nacopropertysourceattributes-属性包装)
- [NacosPropertySourceLoader 加载器](#nacopropertysourceloader-加载器)
- [Nacos 客户端：OpenApiNacosClient](#nacos-客户端openapinacosclient)
- [ShutdownHook 资源清理](#shutdownhook-资源清理)
- [完整工作流程](#完整工作流程)
- [工程问题分析](#工程问题分析)

---

## 模块概述

Nacos 模块是四个后端实现中**唯一一个没有复用官方 SDK**的模块。它自实现了 Nacos REST API 客户端 `OpenApiNacosClient`，通过 HTTP 接口与 Nacos 服务端通信。

3 个文件，约 350 行：

| 文件 | 行数 | 职责 |
|------|------|------|
| `NacosPorpertySource.java` | 185 | 注解声明 |
| `NacosPropertySourceAttributes.java` | 50 | 注解属性包装 |
| `NacosPropertySourceLoader.java` | 121 | 加载器 + 监听器注册 |

依赖的外部类：

```
io.microsphere.nacos.client           ← 自实现的 Nacos REST 客户端（不在本模块内）
  ├─ NacosClientConfig             客户端配置
  ├─ OpenApiVersion                API 版本枚举（V1/V2）
  ├─ OpenApiTemplateClient         模板客户端
  ├─ ConfigClient                  配置操作接口
  └─ ConfigChangedEvent            配置变更事件
```

这个 Nacos 客户端是另一个项目（`microsphere-nacos-client`），不在 15-configuration 中。

---

## NacosPorpertySource 注解

位置：`nacos-spring/.../annotation/NacosPorpertySource.java`（185 行）

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PropertySourceExtension
@Import(NacosPropertySourceLoader.class)
public @interface NacosPorpertySource {

    @AliasFor(annotation = PropertySourceExtension.class)
    String name() default "";

    @AliasFor(annotation = PropertySourceExtension.class)
    boolean autoRefreshed() default true;

    @AliasFor(annotation = PropertySourceExtension.class)
    boolean first() default false;

    @AliasFor(annotation = PropertySourceExtension.class)
    String before() default "";

    @AliasFor(annotation = PropertySourceExtension.class)
    String after() default "";

    @AliasFor(annotation = PropertySourceExtension.class)
    String[] value() default {};

    @AliasFor(annotation = PropertySourceExtension.class, attribute = "value")
    String[] key() default {};

    @AliasFor(annotation = PropertySourceExtension.class)
    Class<? extends Comparator<Resource>> resourceComparator() default DefaultResourceComparator.class;

    @AliasFor(annotation = PropertySourceExtension.class)
    boolean ignoreResourceNotFound() default false;

    @AliasFor(annotation = PropertySourceExtension.class)
    String encoding() default "UTF-8";

    @AliasFor(annotation = PropertySourceExtension.class)
    Class<? extends PropertySourceFactory> factory() default DefaultPropertySourceFactory.class;

    // Nacos 特有属性
    String serverAddress() default "http://127.0.0.1:8848";
    OpenApiVersion openApiVersion() default OpenApiVersion.V1;
}
```

### 设计要点

| 设计 | 说明 |
|------|------|
| `@Import(NacosPropertySourceLoader.class)` | 触发加载器，通过 `ImportSelector.selectImports()` 执行 |
| `@PropertySourceExtension` 元注解 | 所有属性通过 `@AliasFor` 映射到元注解，Loader 只读元注解属性 |
| `default true` 的 `autoRefreshed` | Nacos 模块默认开启自动刷新，与 `@PropertySourceExtension` 的 `default false` 不同 |
| `key` ↔ `value` 互为别名 | 用户可以用 `key = "app.json"` 或 `value = "app.json"`，效果相同 |
| `serverAddress` 和 `openApiVersion` | 这两个属性不映射到元注解，是 Nacos 特有的 |

### 拼写问题

类名是 `NacosPorpertySource` 不是 `NacosPropertySource`（"Property" 中的 "ro" 写成了 "or"）。使用时要写 `@NacosPorpertySource`。

---

## NacosPropertySourceAttributes 属性包装

位置：`nacos-spring/.../annotation/NacosPropertySourceAttributes.java`（50 行）

```java
public class NacosPropertySourceAttributes extends PropertySourceExtensionAttributes<NacosPorpertySource> {

    public NacosPropertySourceAttributes(Map<String, Object> another,
            Class<NacosPorpertySource> annotationType, PropertyResolver propertyResolver) {
        super(another, annotationType, propertyResolver);
    }

    public final String[] getKeys() {
        return getValue();
    }

    public final OpenApiVersion getVersion() {
        return getEnum("openApiVersion");
    }

    public final String getServerAddress() {
        return getString("serverAddress");
    }
}
```

50 行，三个方法：

| 方法 | 作用 |
|------|------|
| `getKeys()` | 返回 `value` 属性值（即 `key` 的别名），传给 Loader 作为 Nacos 的 dataId |
| `getVersion()` | 读取 `openApiVersion` 枚举，决定使用 V1 还是 V2 的 REST API |
| `getServerAddress()` | 读取 `serverAddress`，Nacos 服务端地址 |

父类 `PropertySourceExtensionAttributes` 提供了 `getValue()`、`getString()`、`getEnum()` 等通用方法，子类只需要暴露 Nacos 特有的属性读取方法。

---

## NacosPropertySourceLoader 加载器

位置：`nacos-spring/.../annotation/NacosPropertySourceLoader.java`（121 行）

这是 Nacos 模块的核心。三个关键部分：

### 1. ConfigClient 缓存（static 块）

```java
private static final Map<String, ConfigClient> configCientCache;

static {
    configCientCache = new HashMap<>();
    addShutdownHookCallback(() -> {
        for (ConfigClient value : configCientCache.values()) {
            try {
                ((OpenApiTemplateClient) value).getOpenApiClient().close();
            } catch (Exception e) {
                logger.error("Fail to close nacos client", e);
            }
        }
        configCientCache.clear();
    });
}
```

| 设计 | 说明 |
|------|------|
| `static Map` | 缓存在类加载时初始化，所有实例共享同一个客户端 |
| `computeIfAbsent` | 按 `属性名`（`attributes.getName()`）缓存，同名配置复用客户端 |
| ShutdownHook | JVM 关闭时关闭所有客户端连接 |

### 2. resolveResources()——拉取配置

```java
@Override
protected Resource[] resolveResources(NacosPropertySourceAttributes attrs,
        String propertySourceName, String resourceValue) throws Throwable {

    ConfigClient client = getClient(attrs);
    String value = client.getConfigContent(resourceValue);
    Resource[] resources = new Resource[1];
    resources[0] = new ByteArrayResource(value.getBytes(), resourceValue);
    return resources;
}
```

| 步骤 | 说明 |
|------|------|
| `getClient(attrs)` | 从缓存获取或创建 Nacos 客户端 |
| `client.getConfigContent(resourceValue)` | 调用 Nacos REST API 拉取配置（resourceValue 是 dataId） |
| `new ByteArrayResource(...)` | 将返回的字符串包装为 Spring Resource |
| 返回 `Resource[]` | 父类将 Resource 转为 `ResourcePropertySource` 注册到 Environment |

### 3. configureResourcePropertySourcesRefresher()——注册监听

```java
@Override
protected void configureResourcePropertySourcesRefresher(NacosPropertySourceAttributes attrs,
        List<PropertySourceResource> propertySourceResources,
        CompositePropertySource propertySource,
        ResourcePropertySourcesRefresher refresher) throws Throwable {

    ConfigClient client = getClient(attrs);
    for (PropertySourceResource resource : propertySourceResources) {
        String resourceValue = resource.getResourceValue();
        client.addEventListener(resourceValue, event -> onConfigChanged(event, refresher));
    }
}
```

给每个 dataId 注册一个 `ConfigChangedEvent` 监听器。回调方法：

```java
private void onConfigChanged(ConfigChangedEvent event, ResourcePropertySourcesRefresher refresher) {
    if (event.isCreated() || event.isModified()) {
        ByteArrayResource resource = new ByteArrayResource(event.getContent().getBytes());
        refresher.refresh(event.getDataId(), resource);
    }
}
```

`refresher.refresh()` 会触发父类的 diff 逻辑——比对新旧内容，计算差异，发布 `PropertySourcesChangedEvent`。

### 客户端创建

```java
private ConfigClient getClient(NacosPropertySourceAttributes attrs) {
    String key = attrs.getName();
    return configCientCache.computeIfAbsent(key, k -> {
        NacosClientConfig config = new NacosClientConfig();
        config.setServerAddress(attrs.getServerAddress());
        if (OpenApiVersion.V1.equals(attrs.getVersion())) {
            return new OpenApiNacosClient(config);
        } else if (OpenApiVersion.V2.equals(attrs.getVersion())) {
            return new OpenApiNacosClientV2(config);
        }
        throw new RuntimeException("Unsupported version " + attrs.getVersion());
    });
}
```

---

## Nacos 客户端：OpenApiNacosClient

Nacos 模块不包含 Nacos 客户端的代码——它在独立的 `microsphere-nacos-client` 项目中。从 Import 可以看出客户端结构：

```
OpenApiNacosClient（V1 实现）
  └─ 调用 Nacos REST API（v1）
     GET /nacos/v1/cs/configs?dataId={dataId}&tenant={tenant}
     POST /nacos/v1/cs/configs/listener （长轮询监听）

OpenApiNacosClientV2（V2 实现）
  └─ 调用 Nacos REST API（v2）
     GET /nacos/v2/cs/config?dataId={dataId}&tenant={tenant}
     POST /nacos/v2/cs/configs/listener （长轮询监听）

ConfigClient（接口）
  ├─ getConfigContent(dataId) → String
  └─ addEventListener(dataId, listener)
```

这个客户端的实现对外部依赖为零——它直接通过 HTTP 请求 Nacos API，不依赖 `com.alibaba.nacos:nacos-client`。

---

## ShutdownHook 资源清理

`NacosPropertySourceLoader` 在 static 块中注册了一个 JVM shutdown hook，用于关闭所有缓存的 Nacos 客户端连接。

```java
addShutdownHookCallback(() -> {
    for (ConfigClient value : configCientCache.values()) {
        ((OpenApiTemplateClient) value).getOpenApiClient().close();
    }
    configCientCache.clear();
});
```

| 问题 | 说明 |
|------|------|
| 强制类型转换 | `((OpenApiTemplateClient) value)`——假设 `ConfigClient` 实现都是 `OpenApiTemplateClient` 的子类 |
| 缓存清理顺序 | `close()` 后 `clear()`，但如果 close 抛出异常，clear 不会执行 |
| shutdown hook 注册时机 | static 块在类加载时执行——即使没有使用 Nacos 配置也会注册 hook |

---

## 完整工作流程

```
编译期：
  @NacosPorpertySource(key = "app.json", autoRefreshed = true)
    └─ 元注解 @PropertySourceExtension
       └─ @Import(NacosPropertySourceLoader.class)

启动时：
  Spring 处理 @Configuration 类
    → 遇到 @Import(NacosPropertySourceLoader.class)
      → NacosPropertySourceLoader.selectImports()
        → loadPropertySource()
          → createAttributes() → 创建 NacosPropertySourceAttributes
          → getResourceValues() → ["app.json"]（即 dataId）
          → 对于每个 dataId：
            → resolveResources()
              → getClient(attrs) → 从缓存获取或创建 OpenApiNacosClient(V2)
              → client.getConfigContent("app.json")
                → HTTP GET /nacos/v2/cs/config?dataId=app.json
                → 返回配置内容字符串
              → new ByteArrayResource(content)
            → 包装为 ResourcePropertySource
            → 加入 CompositePropertySource
          → CompositePropertySource 注册到 Environment
          → autoRefreshed=true，调用 configureResourcePropertySourcesRefresher()
            → client.addEventListener("app.json", handler)
              → 客户端发起长轮询请求（POST listener 端点，挂起等待变更）

运行时（配置变更）：
  Nacos 服务端配置变更
    → 长轮询返回
    → ConfigChangedEvent 回调
      → onConfigChanged(event, refresher)
        → event.isCreated() || event.isModified()
        → new ByteArrayResource(event.getContent())
        → refresher.refresh("app.json", resource)
          → 父类 PropertySourceExtensionLoader 比对新旧内容
          → diff 发现变化
          → PropertySourceChangedEvent（ADDED / REPLACED / REMOVED 之一）
          → PropertySourcesChangedEvent 发布
            → ApplicationListener 收到事件
```

---

## 工程问题分析

### 自实现客户端 vs 官方 SDK

| 维度 | 官方 `nacos-client` | 自实现 `OpenApiNacosClient` |
|------|-------------------|---------------------------|
| 连接方式 | gRPC 长连接 | HTTP REST 短连接 |
| 配置监听 | gRPC 双向流推送 | HTTP 长轮询 |
| 服务发现 | gRPC 直连 + 故障转移 | 静态配置 `serverAddress` |
| 依赖大小 | ~10MB | ≈0（仅 HTTP 客户端） |
| API 版本 | 2.x+ gRPC，1.x HTTP | 手动切 V1/V2 |

自实现的收益是零外部依赖，代价是没有故障转移、没有 gRPC 推送、Nacos 服务端 API 升级时需要手动适配。

### 缓存的线程安全性

`configCientCache` 是 `HashMap`（不是 `ConcurrentHashMap`）。JDK 8+ 修复了 `HashMap.computeIfAbsent()` 的并发死循环问题，但 HashMap 的并发修改仍然不是线程安全的——其他并发操作（如遍历）仍可能丢数据。运行在 JDK 7 上并发创建客户端可能重复。

### 单 dataId 限制

`resolveResources()` 返回 `new Resource[1]`——只支持单 dataId。虽然 `getResourceValues()` 可以从注解的 `value`/`key` 获取多个值，但实际实现只处理了第一个。如果需要从 Nacos 拉取多个配置，需要写多个 `@NacosPorpertySource` 注解。

### ShutdownHook 的类型转换风险

`((OpenApiTemplateClient) value).getOpenApiClient().close()`——这里假设所有 `ConfigClient` 实现都是 `OpenApiTemplateClient` 的子类。如果以后添加了不继承 `OpenApiTemplateClient` 的实现，这行会抛 `ClassCastException`。
