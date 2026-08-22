# Feign 完整卷重新规划

> 目标：严格区分 OpenFeign core 与 Spring Cloud OpenFeign integration，避免把 Feign 写成“Spring 注解 + HTTP client 使用手册”。  
> 本地 OpenFeign：`/data/workspace/source-code/code/spring/feign`，版本 `13.14-SNAPSHOT`  
> 本地 Spring Cloud OpenFeign：`/data/workspace/source-code/code/spring/spring-cloud-openfeign`，版本 `4.3.2`，实际依赖 Feign `13.6.1`

---

## 一、先给结论：Feign 必须拆成两卷

Feign 在本地有两套相关但不同的源码仓库：

- **OpenFeign core**：负责声明式 HTTP client 的核心运行时，也就是 client-side request/response 执行链本身。  
- **Spring Cloud OpenFeign**：负责把 OpenFeign 接入 Spring / Spring Boot / Spring Cloud 体系，也就是 client 的创建、命名上下文、配置绑定和基础设施包装。

这里要把边界钉死：**Volume A 讲 client runtime，Volume B 讲 client factory / integration / infrastructure。** Spring Cloud OpenFeign 不是第二个 Feign runtime，而是对 OpenFeign runtime 的容器化与基础设施化装配。

两者不能混成一卷平推。

如果混写，读者会把下面两类问题搅在一起：

- OpenFeign 自己如何从接口生成 proxy、构造 request、执行 HTTP、decode response。
- Spring Cloud 如何扫描 `@FeignClient`、创建 named child context、组装 Feign.Builder、接入 LoadBalancer 和 CircuitBreaker。

最准确的分卷判断是：

> OpenFeign 解释“Feign 怎么工作”；Spring Cloud OpenFeign 解释“Spring 怎么制造和装饰 Feign client”。

---

## 二、版本边界必须写死

### OpenFeign core

- 仓库：`/data/workspace/source-code/code/spring/feign`
- 版本：`13.14-SNAPSHOT`
- 主模块：`core`

### Spring Cloud OpenFeign

- 仓库：`/data/workspace/source-code/code/spring/spring-cloud-openfeign`
- 版本：`4.3.2`
- 实际依赖 OpenFeign：`13.6.1`

这意味着：

- OpenFeign core 篇章可以基于 `13.14-SNAPSHOT`。  
- Spring Cloud OpenFeign 篇章的精确源码和行为，必须优先依据 Spring Cloud 项目实际绑定的 Feign `13.6.1`。  
- 本地 OpenFeign `13.14-SNAPSHOT` 只能用于补充概念导航，不能直接拿它的字段、方法和行号替代 `13.6.1`。

再补一条执行规则：**凡是 Spring Cloud OpenFeign 文章中的 `file:line` 证据，优先从 `spring-cloud-openfeign` 仓库取；只有在解释 OpenFeign core 抽象时，才允许把本地 `13.14-SNAPSHOT` 当作概念参照。** 不能反过来拿 `13.14-SNAPSHOT` 的实现细节去证明 Spring Cloud 4.3.2 的行为。

每篇正文开头必须写清：

```text
分析对象：OpenFeign 13.14-SNAPSHOT
```

或：

```text
分析对象：Spring Cloud OpenFeign 4.3.2 + OpenFeign 13.6.1
```

---

## 三、Volume A：OpenFeign Core

### F-MAIN-1 Runtime Spine

题目：

> OpenFeign：从 `Feign.builder()` 到动态代理再到 HTTP 调用

回答：

- `Feign.builder()` 组装什么
- `ReflectiveFeign` 如何生成 proxy
- `Method -> MethodHandler` 如何建立
- `RequestTemplate` 如何在调用期生成 Request
- `Client.execute()` 处在什么位置
- ResponseHandler / InvocationContext 如何处理返回值和异常

主线：

```text
Feign.builder()
    → internalBuild()
    → ReflectiveFeign.newInstance()
    → MethodHandler
    → InvocationHandler
    → RequestTemplate.Factory
    → Target.apply()
    → Client.execute()
    → ResponseHandler
```

### F-MAIN-2 Contract / MethodMetadata / RequestTemplate

题目：

> OpenFeign：Contract、MethodMetadata 与 RequestTemplate

回答：

- interface annotation 如何变成 MethodMetadata
- 参数如何分类成 path/query/header/body/queryMap/headerMap/options
- MethodMetadata 保存什么
- RequestTemplate 为什么是 prototype
- build-time 固定什么、invoke-time 填什么

### F-EXT-1 Execution Extensions

题目：

> OpenFeign：Client、Encoder、Decoder、Retryer、ErrorDecoder 与 Capability

回答：

- 请求侧：Client / Encoder / RequestInterceptor
- 响应侧：ResponseInterceptor / Decoder / ErrorDecoder
- `RetryableException -> Retryer` 控制流
- per-client / per-invocation / per-attempt 粒度
- Capability 如何在 build-time 装饰多个 runtime component

这里要特别强调：`Capability` 不是又一个 Client/Encoder 替换点，而是这篇的“第二主角”。它代表的是一类新的 build-time decorator 机制，所以第三篇不应被写成“传统扩展接口清单”，而应写成“执行层 + build-time decoration”双主线。

### F-ADAPTER-1 HTTP Client Adapters

候选题目：

> OpenFeign：Apache HttpClient、HC5、OkHttp、Java11 Client 的适配边界

建议暂缓。不要在 core baseline 初期按 HTTP client 一个模块拆一篇。

只需先把 `Client` SPI 讲清，后续再做适配器对照。

### F-ADAPTER-2 Codec / Serialization Ecosystem

候选范围：

- Jackson
- Gson
- Moshi
- Fastjson2
- Form
- JAXB
- SOAP
- GraphQL

建议暂缓。它们属于 Encoder/Decoder 生态，不应先于 core execution spine。

### F-OBS-1 Core Observability / Capability

候选范围：

- Micrometer
- Dropwizard metrics
- SLF4J
- caching
- validation
- reactive

建议放在 core 三篇之后，重点讲 Capability 如何把观测和缓存装进 runtime，而不是逐个模块介绍。

---

## 四、Volume B：Spring Cloud OpenFeign

### F-SC-1 Client Creation Bridge

题目：

> Spring Cloud OpenFeign：从 `@EnableFeignClients` 到 Feign Proxy

这一篇必须强行控边界：只讲 registrar / factory bean / named context / Feign.Builder 组装，不要提前吞入 LoadBalancer、CircuitBreaker、OAuth2、Refresh、Observability。否则第一篇会从“client creation bridge”膨胀成“Spring Cloud OpenFeign 全景导览”。

主线：

```text
@EnableFeignClients
    → FeignClientsRegistrar
    → FeignClientSpecification
    → FeignClientFactoryBean
    → NamedContextFactory child context
    → FeignClientsConfiguration
    → Feign.Builder
    → Targeter
    → OpenFeign proxy
```

回答：

- Registrar 如何扫描 `@FeignClient`
- BeanDefinition 指向什么
- eager / lazy attributes resolution 的差异
- 每个 named client 为什么有独立 child context
- FeignClientsConfiguration 提供哪些默认 Bean
- SpringMvcContract 处在哪个边界
- FactoryBean 如何把 Spring 组件装进 Feign.Builder

### F-SC-2 Spring Contract / Configuration

候选题目：

> Spring Cloud OpenFeign：SpringMvcContract、配置属性与 Client Context

回答：

- Spring MVC 注解如何进入 Feign MethodMetadata
- `name` / `contextId` / Bean name / qualifier 的区别
- Java configuration 与 properties 的优先级
- default configuration 与 client-specific configuration
- `FeignBuilderCustomizer` / `Capability` 如何接入

### F-SC-3 LoadBalancer Overlay

候选题目：

> Spring Cloud OpenFeign：LoadBalancer 如何接管 Feign Client

回答：

- 没有显式 URL 时，service name 如何变成逻辑 target
- `FeignBlockingLoadBalancerClient` 如何包装底层 Client
- ServiceInstanceListSupplier 如何进入请求选择
- client child context 与 LoadBalancer child context 的关系

### F-SC-4 CircuitBreaker / Fallback

候选题目：

> Spring Cloud OpenFeign：CircuitBreaker、Fallback 与 Targeter

回答：

- `FeignCircuitBreaker.Builder`
- `FeignCircuitBreakerTargeter`
- fallback / fallbackFactory
- circuit name resolver
- group 与 alphanumeric ids
- 失败如何从 Feign core 进入 CircuitBreaker

### F-SC-5 Refresh / OAuth2 / Observability

候选范围：

- refreshable client
- OAuth2 interceptor
- Micrometer capability
- caching capability
- request/response observation

建议按需拆分，不纳入第一批 baseline。

---

## 五、两卷之间的硬边界

### OpenFeign core 要回答

- 一次 Feign 方法调用怎样运行
- 接口 metadata 怎样产生
- RequestTemplate 怎样生成 Request
- Client 怎样执行 HTTP
- Decoder/ErrorDecoder/Retryer/Capability 怎样插入

### Spring Cloud OpenFeign 要回答

- Spring 怎样发现并注册 Feign client
- 每个 named client 怎样得到独立配置空间
- Spring MVC annotations 怎样适配 Feign Contract
- LoadBalancer / CircuitBreaker / OAuth2 / Metrics 怎样包裹 core

### 明确不能混写

| 不应混在一起 | 原因 |
|---|---|
| `Feign.builder()` 与 `@FeignClient` | 一个是 core builder，一个是 Spring 注册入口 |
| `ReflectiveFeign` 与 `FeignClientFactoryBean` | 一个生成代理，一个从 Spring child context 组装 builder |
| OpenFeign `Contract` 与 Spring `SpringMvcContract` | 一个是 core contract，另一个是 integration adapter |
| `Client.execute()` 与 `FeignBlockingLoadBalancerClient` | 一个是 HTTP 执行 SPI，一个是 Spring Cloud 包装 client |
| OpenFeign Capability 与 Spring Bean auto-configuration | 一个是 core build-time decoration，一个是 Spring 装配来源 |

---

## 六、第一批正文优先级

### 优先级 A：OpenFeign Core 必须先写

1. Runtime Spine：`Feign.builder -> proxy -> MethodHandler -> Client`
2. Contract / MethodMetadata / RequestTemplate
3. Client / Encoder / Decoder / Retryer / ErrorDecoder / Capability

这三篇完成后，OpenFeign core 就形成了一个完整第一轮闭环。

### 优先级 B：Spring Cloud OpenFeign 接入

4. `@EnableFeignClients` / Registrar / FactoryBean / NamedContext
5. SpringMvcContract / properties / per-client configuration

### 优先级 C：Spring Cloud 基础设施覆盖

6. LoadBalancer overlay
7. CircuitBreaker / fallback
8. OAuth2 / refresh / observability / caching

---

## 七、当前 Feign 规划中的关键陷阱

1. **版本错位**
   - OpenFeign checkout 是 `13.14-SNAPSHOT`
   - Spring Cloud OpenFeign 绑定的是 `13.6.1`
2. **Spring integration 污染 core 主线**
   - 第一篇不应出现 `@FeignClient`、named context、LoadBalancer
3. **Contract 过早吞掉 runtime spine**
   - Contract 应该是第二篇，不应成为第一篇开场
4. **Capability 被遗漏**
   - 新版 OpenFeign 扩展层不能只写传统 Client/Encoder/Decoder
5. **HTTP adapters 过早拆篇**
   - HttpClient/OkHttp/Java11 先作为 Client SPI 适配层，不要一开始逐个展开
6. **配置矩阵先行**
   - Spring Cloud 的配置很多，但表格不能替代 factory/child-context runtime 主线
7. **把 OpenFeign `spring/` 模块误当成 Spring Cloud OpenFeign**
   - 前者是 OpenFeign 自带的 contract/support 模块，后者是独立 Spring Cloud 集成仓库
8. **按 HTTP client 模块数量拆文章**
   - HttpClient / HC5 / OkHttp / Java11 不应一上来逐个写，而应先收束到 `Client` SPI 的适配边界上统一理解

## 八、最终结论

Feign 的完整卷应拆成两部分：

- **OpenFeign Core：client-side HTTP runtime 与扩展协议**
- **Spring Cloud OpenFeign：Spring 容器、配置和基础设施接入桥**

当前最合理的落笔顺序是：

```text
OpenFeign runtime spine
    → Contract / MethodMetadata / RequestTemplate
    → Execution extensions / Capability
    → Spring Cloud client creation bridge
    → SpringMvcContract / configuration
    → LoadBalancer / CircuitBreaker / observability
```

因此，当前可以正式开始 OpenFeign 第一篇，而不应提前切入 Spring Cloud OpenFeign。