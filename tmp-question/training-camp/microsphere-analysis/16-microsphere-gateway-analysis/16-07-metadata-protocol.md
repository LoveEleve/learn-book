# 16-07：跨进程元数据协议 —— 端点从 Controller 到网关的旅程

> **核心命题**：03-07（Web 端点元数据）已讲 provider 侧**进程内**的收集链路（Registrar/Resolver/Registry/ReadyEvent）。本文补 03-07 没讲的**跨进程**部分：序列化协议（`toJSON` ↔ `parseWebEndpointMapping` 的九字段硬约定）、注册中心 metadata 载体（`web.mappings`/`web.context-path`）、发布前置过滤（actuator 剔除）、id 语义（`endpoint.hashCode()`），以及协议无 schema 的隐患。

---

## 一、协议全景图

```
Provider 进程                                  注册中心              网关进程
① 收集（03-07 已述）                                │                    │
   WebEndpointMappingRegistrar(Lifecycle)           │                    │
   → 三种 HandlerMapping → 端点集                    │                    │
② 序列化（本文）                                    │                    │
   WebEndpointMappingsReadyEvent                    │                    │
   → WebServiceRegistryAutoConfiguration            │                    │
     → excludeMappings（actuator/内置组件剔除）       │                    │
     → attachMetadata → web.mappings=URL编码JSON     │                    │
                        web.context-path=context    │                    │
③ 注册（05 模块）───────────────────────────────────► 实例 metadata       │
④ 发现与解析（本文）                                 │◄─────────────────── 订阅实例列表
                                                    │  getWebEndpointMappings
                                                    │  → URL解码 → JSON → 九字段还原
                                                    │  → 编译为 RequestMappingInfo（16-03/16-04）
```

**协议存在于两个进程之间，中间载体是注册中心的实例 metadata**——这是"端点路由"得以成立的物理基础，也是全部隐患的源头（长度、编码、无 schema）。

---

## 二、序列化协议：toJSON ↔ parseWebEndpointMapping

### 2.1 写入端：WebEndpointMapping.toJSON（九字段）

```java
// WebEndpointMapping.java:1641-1654
public String toJSON() {
    StringBuilder stringBuilder = new StringBuilder(LEFT_CURLY_BRACE).append(LINE_SEPARATOR);
    append(stringBuilder, "id", this.id);
    append(stringBuilder, "kind", this.kind, COMMA, LINE_SEPARATOR);
    append(stringBuilder, "negated", this.negated, COMMA, LINE_SEPARATOR);
    append(stringBuilder, "patterns", this.patterns, ...);
    append(stringBuilder, "methods", this.methods, ...);
    append(stringBuilder, "params", this.params, ...);
    append(stringBuilder, "headers", this.headers, ...);
    append(stringBuilder, "consumes", this.consumes, ...);
    append(stringBuilder, "produces", this.produces, ...);
    ...
}
```

### 2.2 读取端：parseWebEndpointMapping（字段名硬约定）

```java
// ServiceInstanceUtils.parseWebEndpointMapping（05 模块）
String kind = jsonObject.optString("kind");
int id = jsonObject.optInt("id");
boolean negated = jsonObject.optBoolean("negated");
String[] patterns = getArray(jsonObject, "patterns");
... // methods/params/headers/consumes/produces 同构
Builder<?> builder = of(valueOf(kind)).endpoint(Integer.valueOf(id)).patterns(patterns)...;
```

**九字段对应表**：

| JSON 字段 | 类型 | 语义 |
|-----------|------|------|
| `id` | int | 端点标识（见第五节） |
| `kind` | String（枚举名） | SERVLET/FILTER/WEB_MVC/WEB_FLUX/CUSTOMIZED |
| `negated` | boolean | 否定匹配 |
| `patterns`/`methods`/`params`/`headers`/`consumes`/`produces` | String 数组 | 六维匹配条件 |

**隐式协议（无 schema 的代价）**：两端靠**字段名硬约定**通信——没有版本号、没有 schema 校验。缺字段时的行为**分两类**（`opt*` 语义）：`kind` 缺失返回 `""` → `Kind.valueOf("")` **抛异常**（整条解析失败）；`id`/`patterns` 等缺失静默取默认值（0/空数组）→ **解析出"错误的空端点"**（id=0 的端点被打标转发）。provider 与网关版本不同步（如新增 Kind 枚举值）时同样抛异常。编解码两端必须同步升级——这是 16 的协议技术债（16-08 H2）。

### 2.3 传输编码：单行 + URL 编码

```java
// ServiceInstanceUtils.attachMetadata（82-103 行）
StringJoiner jsonBuilder = new StringJoiner(COMMA + LINE_SEPARATOR, "[", "]");
webEndpointMappings.stream().map(WebEndpointMapping::toJSON).forEach(jsonBuilder::add);
String json = jsonBuilder.toString().replace(LINE_SEPARATOR, "");
String encodedJson = encode(json);                  // URL 编码（URLEncoder，UTF-8）
metadata.put("web.context-path", contextPath);
metadata.put("web.mappings", encodedJson);
```

**协议只传"条件"，不传"对象"**：`WebEndpointMapping` 的 `endpoint`/`source`/`attributes`/`hashCode` 全部是 **transient**（`WebEndpointMapping.java:142/168/170/173`）——HandlerMethod 等运行时对象不跨进程，传输的只有九字段（六维匹配条件 + id/kind/negated）。这是协议的**本质边界**：跨进程的只是"匹配规则"，实例化对象留在各自 JVM 内。

**两个放大**：
- 多行 JSON 压成单行（去掉换行）
- **URL 编码**——`URLUtils.encode` 委托标准 `URLEncoder.encode(value, UTF-8)`（`URLUtils.java:498-501`），空格转 `+`、特殊字符转 `%XX`（一个字符最多放大 3 倍）

**长度隐患（16-08 H1）**：整条端点集压进**一条** metadata 值。服务端点多（几十个 Controller 方法 × 每端点九字段）时，编码后轻松超过注册中心单条 metadata 上限（如 Nacos 默认 4096 字符——外部事实）——**注册直接失败或 metadata 被截断**。代码无分片、无压缩、无告警。对"端点级路由"这种依赖完整元数据的方案，这是规模化的第一道墙。

---

## 三、发布前置过滤（WebServiceRegistryAutoConfiguration）

```java
private void attachWebMappingsMetadata(Registration registration, Collection<WebEndpointMapping> webEndpointMappings) {
    Set<WebEndpointMapping> mappings = newHashSet(webEndpointMappings);
    excludeMappings(mappings);                     // 剔除两类的端点
    attachMetadata(getContextPath(), registration, mappings);
}

void excludeMappings(Collection<WebEndpointMapping> mappings) {
    while (iterator.hasNext()) {
        if (isExcludedMapping(mapping, patterns) || isActuatorWebEndpointMapping(mapping, patterns)) {
            iterator.remove();
        }
    }
}

protected boolean isActuatorWebEndpointMapping(WebEndpointMapping mapping, String[] patterns) {
    for (String pattern : patterns) {
        if (pattern.startsWith(actuatorBasePath)) {   // 默认 "/actuator"
            return true;
        }
    }
    return false;
}
```

**三类被剔除的端点**（发布前就消失，网关永远看不到）：

| 类别 | 实现 | 说明 |
|------|------|------|
| actuator 端点 | `isActuatorWebEndpointMapping`（基类默认，前缀匹配 `management.endpoints.web.base-path`，默认 `/actuator`） | **健康检查/指标不暴露到网关**——安全设计；代价：网关无法路由任何 `/actuator/**`（此类请求只能走其他路由或直达服务） |
| 内置 Filter/Servlet 映射 | WebMvc 版 `isExcludedMapping`（匹配 FilterRegistrationBean/DispatcherServletRegistrationBean 的 `/*` 映射） | 不把 `/*` 这种通配伪端点发布出去 |
| （WebFlux 版无内置排除） | `isExcludedMapping` 返回 false | WebFlux 没有 servlet 注册表概念 |

**context-path 来源**（两版差异，均已验证）：

| 版本 | `getContextPath()` | 来源 |
|------|--------------------|------|
| WebMvc | `server.servlet.context-path`（`@Value` 注入） | 配置属性 |
| WebFlux | `""`（恒空串） | 响应式无 servlet context-path 概念 |

---

## 四、id 语义：endpoint.hashCode()（跨进程不稳定）

```java
// WebEndpointMapping.java:1271
this.id = endpoint == null ? 0 : endpoint.hashCode();
```

- `endpoint` 是 HandlerMethod 等实例，`hashCode()` 对 HandlerMethod 包含 **bean 实例的 identity hash**——**跨 JVM 重启不稳定**。
- 但"不稳定"不影响现有设计：id 只用于**当前进程对**的标识——provider 注册时生成 id → 网关读同一 id 打 `microsphere_wem_id` 头 → 下游 `ReversedProxyHandlerMapping`（03 模块）用同一 id 反查自己的 `WebEndpointMappingRegistry` 命中 handler。**三方共享同一个运行期的 id，进程内一致即成立**。
- 误区提醒：不要把它当"稳定的端点指纹"（幂等去重、跨环境对齐都不可靠）。

---

## 五、网关反向解析（与 16-03/16-04 的接缝）

```java
// ServiceInstanceUtils（05 模块，网关侧读取）
public static Collection<WebEndpointMapping> getWebEndpointMappings(ServiceInstance serviceInstance) {
    String encodedJSON = getMetadata(serviceInstance, "web.mappings");
    return parseWebEndpointMappings(encodedJSON);   // decode → jsonArray → 逐条 parseWebEndpointMapping
}
```

网关侧（16-03/16-04）拿到 `WebEndpointMapping` 集合后编译为 `RequestMappingInfo`——**协议还原与匹配编译是两个进程里的两段独立逻辑**：

```
provider:  Controller → WebEndpointMapping（对象）→ toJSON → 编码 → metadata
gateway:   metadata → 解码 → parseWebEndpointMapping（对象）→ 编译 RequestMappingInfo → 匹配
```

**context-path 的协议断裂（呼应 16-03 P2）**：provider 认真发布 `web.context-path`（WebMvc 版还区分了配置来源），但网关侧消费它的 `buildPath` 是死代码（16-02 L1）——**协议字段发布方在维护、消费方已停用**。字段还在，功能已断：context-path 部署的服务经网关转发 404。这是"协议与实现脱节"的完整标本：协议层没删字段（怕破坏兼容），实现层忘了接（重构丢失）。

---

## 六、协议隐患清单（引用用）

> 编号 H1-H3 为协议层专属；全局缺口表（G1-GN）在 16-08 统一汇总。

| # | 隐患 | 证据 | 影响 |
|---|------|------|------|
| H1 | 单条 metadata 无分片，URL 编码放大 | `attachMetadata` 82-103 行 | 大服务端点集超注册中心长度限制，注册失败/截断 |
| H2 | 隐式 schema：字段名硬约定、无版本号 | `toJSON` ↔ `parseWebEndpointMapping` | 两端不同步时静默解析异常或 `Kind.valueOf` 抛错 |
| H3 | 协议字段与实现脱节：`web.context-path` 发布但网关不消费 | `buildPath` 死代码（16-03 P2） | context-path 部署服务网关转发 404 |
| — | id 跨进程不稳定 | `endpoint.hashCode()`（1271 行） | 不影响进程内配对，但不可用于跨环境指纹 |

---

## 七、与 03-07 的边界（交叉引用约定）

| 内容 | 归属 |
|------|------|
| WebEndpointMapping 模型、Builder、工厂链（SmartWebEndpointMappingFactory 等） | 03-07（不重复） |
| Resolver 收集（三种 HandlerMapping）、Registry、Registrar Lifecycle、WebEventPublisher | 03-07（不重复） |
| @EnableWebExtension 开关语义 | 03-07（不重复） |
| **序列化协议、metadata 载体、发布过滤、id 语义、网关解析** | **本文（16-07）** |
| 05 的注册流程（MultipleServiceRegistry、EventPublishingRegistrationAspect） | 05-01/05-03（引用） |

---

## 八、小结

- **协议 = 九字段 JSON + URL 编码 + 注册中心 metadata 载体**，无版本号、无 schema、无分片——"能用"的协议，不是"可演进"的协议。
- **发布前过滤是安全设计**：actuator 端点与内置 `/*` 映射不进路由表。
- **id 是进程内不透明标识**（`endpoint.hashCode()`），与 03 的 `ReversedProxyHandlerMapping` 构成"打标 ↔ 还原"配对。
- **context-path 协议断裂是 16-02 重构的最终遗产**：发布方还在维护字段，消费方已经停用。
