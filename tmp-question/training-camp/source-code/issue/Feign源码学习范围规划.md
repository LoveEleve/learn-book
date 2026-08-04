# Feign 源码学习范围规划

> **版本**: main 分支
> **仓库**: `/data/workspace/source-code/code/spring/feign/`
> **规模**: 304 个源文件，核心模块紧凑
> **日期**: 2026-08-03

---

## 一、仓库概况

Feign 是 OpenFeign 项目，声明式 HTTP 客户端——通过 Java 接口 + 注解定义 HTTP API，运行时生成动态代理。Spring Cloud OpenFeign 基于此集成 Ribbon/LoadBalancer 和 Sentinel。核心是 **Contract(解析接口注解) → MethodHandler(方法→HTTP请求) → Client(执行HTTP) → Decoder(解析响应)** 四层架构。

**核心包结构**：

| 包 | 文件数 | 职责 |
|---|---|---|
| `feign/` root | 20+ | 核心：Feign.Builder、ReflectiveFeign、InvocationHandlerFactory、Contract |
| `feign/codec/` | 10+ | 编解码：Encoder/Decoder/ErrorDecoder——Jackson/Gson/JAXB |
| `feign/interceptor/` | 2 | RequestInterceptor |
| `feign/template/` | 20+ | URI 模板：UriTemplate、Template、QueryTemplate、HeaderTemplate |
| `feign/auth/` | 2 | BasicAuth 拦截器 |
| `feign/querymap/` | 若干 | @QueryMap 支持 |
| `feign/stream/` | 若干 | 流式响应 |
| `feign/optionals/` | 若干 | Optional 支持 |

---

## 二、知识域规划

### 🔴 核心域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| F-1 | **Feign.Builder + 动态代理** | Feign(260行), Feign.Builder, ReflectiveFeign(212行), InvocationHandlerFactory(45行), Capability | **Builder 模式**：`Feign.builder()` 链式配置 8 个可替换组件→`target()` → `ReflectiveFeign.newInstance()` → `Contract.parseAndValidateMetadata()` 解析接口→`SynchronousMethodHandler.Factory` 为每个方法创建 `MethodHandler` → `InvocationHandlerFactory.create()` → `FeignInvocationHandler` → `Proxy.newProxyInstance()`；**Capability**：装饰器模式——`Capability.enrich(builder)` 在 Builder 构建阶段注入能力——Spring Cloud LoadBalancer 通过 `CachingCapability` 注入 `LoadBalancerClient`（类似于 gRPC ClientInterceptor 的 Builder 级别）；**AsyncFeign**：`AsyncFeign.builder()` → `AsyncClient`(CompletableFuture) → `AsynchronousMethodHandler`；**DefaultMethodHandler**：Java 8 默认方法的处理（不代理直接调用）
| F-2 | **Contract 注解解析** | Contract, Spring Contract, MethodMetadata, RequestTemplate | **接口→HTTP 映射**：`Contract.parseAndValidateMetadata(Class)` → 遍历 `@RequestLine`/`@Headers`/`@Body`/`@Param` 注解 → 生成 `MethodMetadata`(configKey/returnType/bodyType/indexToName/indexToExpander)—方法级别的 HTTP 元数据 `RequestTemplate.Factory`；**Spring Contract**（Spring Cloud 集成）：`SpringContract` 解析 `@RequestMapping/@GetMapping/@PostMapping` + `@RequestParam/@PathVariable/@RequestHeader` → 桥接 Spring MVC 注解到 Feign |
| F-3 | **Client 执行 + Interceptor** | Client, Client.Default(Java HttpURLConnection), RequestInterceptor, Retryer | **Client 抽象**：`Client.execute(Request, Options)` → 返回 `Response` —— `Client.Default`(JDK HttpURLConnection)、`ApacheHttpClient`、`OkHttpClient`、`LoadBalancerClient`(Spring Cloud LB)；**Interceptor**：`RequestInterceptor.apply(RequestTemplate)`——添加认证 Token/跟踪 Header；**Retryer**：`Retryer.Default`(5 次指数退避 100ms→1.5s) → `continueOrPropagate(e)` |

### 🟡 扩展域（2 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| F-4 | **Encoder/Decoder 编解码** | Encoder, Decoder, ErrorDecoder, JacksonDecoder, GsonDecoder | **Encoder**：对象→HTTP Body(`Response.Body`/`byte[]`)——`JacksonEncoder`/`GsonEncoder`/`SpringEncoder`；**Decoder**：HTTP Response→对象——`JacksonDecoder`/`GsonDecoder`/`SpringDecoder`；`OptionalDecoder` 处理 `Optional<T>` 返回值；**ErrorDecoder**：HTTP 错误状态→`FeignException` 子类——`ErrorDecoder.Default`(基于状态码) |
| F-5 | **URI 模板引擎** | UriTemplate, Template, Expression, VariableDefinition | `@RequestLine("GET /users/{id}")` → `UriTemplate.expand(variables)` → `Template.resolve(Expressions)` → 变量替换 + URL 编码——支持 `{key}` 简单变量、`:method` 模板变量 |

---

## 三、淘汰清单

| 模块/功能 | 理由 |
|---|---|
| `dropwizard-metrics4/5/` `micrometer/` | 指标集成——非核心 |
| `hystrix/` `soap/` `jaxb/` `jaxrs2/` | 特定框架适配 |
| `jackson/` `gson/` `jackson-jr/` `json/` | 编解码（理解 Decoder/Encoder 接口即可）|
| `kotlin/` `java11/` | Kotlin/HTTP2 支持 |
| `slf4j/` `mock/` `test/` | 日志/测试 |
| `annotations/` | 注解定义 |
| `apt-test-generator/` `benchmark/` `example/` | 工具/示例 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 3 |
| 🟡 扩展域 | 2 |
| **总域** | **5** |

---

## 五、学习顺序建议

```
F-1 Builder+动态代理（理解 Feign.builder().target() 全链路）
  → F-2 Contract 注解解析（理解 @RequestLine → MethodMetadata）
    → F-3 Client 执行+Interceptor（理解 HTTP 请求如何发出）
      → F-4/F-5 按需深入
```

以上规划完成，共 **3🔴+2🟡=5 域**。
