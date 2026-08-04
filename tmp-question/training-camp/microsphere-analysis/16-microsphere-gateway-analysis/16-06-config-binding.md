# 16-06：配置绑定 —— excludes 从扁平 YAML 到对象

> **核心命题**：`spring.cloud.gateway.routes[0].metadata` 是 `Map<String,Object>`，但网关代码要的是 `metadata["web-endpoint"]` = 一个 `WebEndpointConfig` **对象**（`ConfigUtils` 有 `instanceof` 检查）。Spring Boot Binder 对 Map 只填扁平值，不做对象转换——谁在什么时候把扁平 YAML 变成了对象塞进 Map？本文拆解三层机制（注册层 → 接入层 → 执行层），并回答"为什么不用 15 的注解模型"。

---

## 一、问题：Map 绑定不做对象转换

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: web-endpoint-mapping
          uri: we://all
          metadata:                    # ← RouteDefinition.metadata 是 Map<String, Object>
            web-endpoint:              # ← 目标：metadata["web-endpoint"] = WebEndpointConfig 对象
              excludes:
                - patterns: /internal/**
                  methods: GET
```

`RouteDefinition.metadata`（SCG）是 `Map<String, Object>`——Binder 对 Map 类型只做"键→值"扁平填充，`web-endpoint` 会成为一个嵌套 `Map`，**永远不会自动变成 `WebEndpointConfig`**。而消费端（16-03/16-04 的 `getWebEndpointConfig(metadata)`）是 `instanceof WebEndpointConfig` 判断，拿不到对象就直接跳过 excludes。**必须在绑定管线上插一道钩子**。

---

## 二、三层机制全景

```
① 注册层（何时注册）
   spring.factories: ApplicationContextInitializer
   → WebEndpointApplicationContextInitializer（早于一切 @ConfigurationProperties 绑定）
     → 注册 WebEndpointConfigurationPropertiesBindListener 为 bean（名字：webEndpointConfigurationPropertiesBindListener）

② 接入层（如何进入 Binder，自动生效——用户零配置）
   04 模块：ConfigurationPropertiesAutoConfiguration（AutoConfiguration.imports 注册，第 41 行自动标注
   @EnableConfigurationPropertiesExtension）→ Registrar 注册
   ListenableConfigurationPropertiesBindHandlerAdvisor（implements ConfigurationPropertiesBindHandlerAdvisor——官方扩展点）
     → apply(bindHandler)：getSortedBeans(BeanFactory, BindListener.class) 按【类型】收集全部 BindListener bean
     → 包装为 ListenableBindHandlerAdapter 替换官方绑定 handler
   官方侧（Boot 3.x 字节码验证）：ConfigurationPropertiesBinder.getBindHandlerAdvisors()
   ——从 ApplicationContext 按 ConfigurationPropertiesBindHandlerAdvisor 类型收集（getBeanProvider(...).orderedStream()），
   在 getBindHandler() 里逐个 apply() 包装 → 所有 @ConfigurationProperties bean 的绑定都经过它（GatewayProperties 是其中之一）

③ 执行层（绑定发生什么）
   GatewayProperties 绑定 → route[N].metadata 绑完 → onFinish(name, target, context, result)
   → 命中条件 → 二次 bind web-endpoint → metadata.put("web-endpoint", WebEndpointConfig)
```

**关键**：接入层按**类型**收集 `BindListener` bean——16 的监听器**不需要任何注册协议**，只要是个 bean 就自动进入所有配置绑定流程（名字只是标识）。且接入层**自动生效**（04 的 `ConfigurationPropertiesAutoConfiguration` 在 `AutoConfiguration.imports` 里、自动标注扩展注解）——**使用 16 的应用无需显式启用任何注解**，16 的 `GatewayAutoConfiguration` 本身也不需要标注。这正是 16-01 说的"16 是纯消费者"的又一例证：接入机制完全在 04。

---

## 三、执行层：onFinish 钩子（WebEndpointConfigurationPropertiesBindListener，76 行）

```java
public class WebEndpointConfigurationPropertiesBindListener implements BindListener, EnvironmentAware {

    private final String gatewayRoutesPropertyNamePrefix;   // 构造参数：webflux="spring.cloud.gateway.routes"、webmvc="spring.cloud.gateway.mvc.routes"

    @Override
    public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        String propertyName = name.toString();
        // ① 命中条件：绑定的节点是"某条路由的 metadata"
        if (propertyName.startsWith(gatewayRoutesPropertyNamePrefix) && propertyName.endsWith("metadata") && result != null) {
            // ② 二次绑定：用 Binder 把 web-endpoint 子配置绑成对象（带校验）
            ConfigurationPropertyName webEndpointName = name.append("web-endpoint");
            WebEndpointConfig webEndpointConfig = getWebEndpointConfig(environment, webEndpointName.toString());
            if (webEndpointConfig != null) {
                Map<String, Object> metadata = (Map<String, Object>) result;
                metadata.put("web-endpoint", webEndpointConfig);   // ③ 回填对象
            }
        }
    }
}
```

三个细节：

**① 命中条件**：`startsWith(prefix) && endsWith("metadata") && result != null`——`onFinish` 对绑定树的**每个节点**都会触发，这里过滤出 `spring.cloud.gateway.routes[N].metadata` 节点。**不会误伤其他配置**（前缀 + 结尾双重限定）。

**② 二次绑定**：`ConfigUtils.getWebEndpointConfig(environment, prefix)`：

```java
public static WebEndpointConfig getWebEndpointConfig(Environment environment, String configPrefix) {
    Binder binder = get(environment, springValidatorBindHandler);   // ValidationBindHandler + LocalValidatorFactoryBean
    return binder.bind(configPrefix, WEB_ENDPOINT_CONFIG_BINDABLE).orElse(null);
}
```

`ConfigUtils` 静态块里 `new LocalValidatorFactoryBean(); validator.afterPropertiesSet();`——**每次调用都 new 一个校验器**（绑定路径上的微开销，refresh 时会重复发生，服务多路由多时放大）。`@Validated`/`@NotNull`/`@Valid` 校验在此生效（`patterns` 必填、`excludes` 列表可空）。

**③ 回填**：把对象 put 进 metadata——从"扁平 Map"升级为"强类型对象"。**注意它是可变操作**：绑定期间塞进的对象，`EnvironmentChangeEvent` 触发 `GatewayProperties` rebind 时（16-05 的配置变更双通道），`onFinish` 同样执行、对象保持新鲜。

**监听的是绑定过程，不是配置变更**：`BindListener` 是 Binder 的回调钩子，不是事件监听器。实时性靠 16-05 的事件链（`EnvironmentChangeEvent → RefreshRoutesEvent → 缓存重建`）补齐——绑定负责"配置 → 对象"，事件链负责"对象变化 → 缓存刷新"，**两段各司其职，缺一段 excludes 都不生效**。

**静默失败缺口（已证）**：`BindListener` 接口还有 `onFailure` 回调（`BindListener.java:85`），`WebEndpointConfigurationPropertiesBindListener` **只实现了 `onFinish`**——若二次绑定失败（如 excludes 的 `patterns` 漏配触发 `@NotNull` 校验失败），`onFinish` 不会执行、metadata 里没有对象，**无任何日志**，excludes 静默失效（请求全部按"无 excludes"放行）。配置格式错误在网关侧完全不可见，只能靠"端点未按预期被排除"反向发现。

---

## 四、WebEndpointConfig 模型（140 行）与默认值语义

```java
@Validated
public class WebEndpointConfig {
    @NotNull @Valid
    private List<Mapping> excludes = new LinkedList<>();

    public static class Mapping {
        @NotNull @Valid
        private String[] patterns;      // 必填：路径模式
        @Nullable private RequestMethod[] methods;   // 缺省 = 所有方法
        @Nullable private String[] params;           // 如 "version=v1"
        @Nullable private String[] headers;          // 如 "Authorization:*"
        @Nullable private String[] consumes;
        @Nullable private String[] produces;
    }
}
```

**默认值语义（决定 excludes 匹配行为）**：

| 字段 | 缺省行为 | 匹配语义 |
|------|----------|----------|
| `patterns` | 无缺省（校验必填） | Ant 模式（`/internal/**` 匹配 `/internal/health`），**不是正则** |
| `methods` | `getMethods()` 返回**全部** `RequestMethod.values()` | 缺省 = 对所有方法生效 |
| `headers` | 空数组 | Spring 语义：`名称:值` 精确或 `!` 否定，不是正则 |
| `params`/`consumes`/`produces` | 可空 | 精确匹配 |

**模型演进**：这是 16-02 演进史中 `Config` 内部类（含 `exclude.services` 整服务排除、`loadBalancer` 配置名）的**裁剪版**——2025-11-11 拆分 commons 时定稿（58f5f4b 创建，136 行），后续 7b1cfb7 迁移过滤逻辑。

---

## 五、ConfigUtils 的两条读取路径

```java
// 路径 A：从 metadata 直接取（请求/刷新路径，零绑定开销）
public static WebEndpointConfig getWebEndpointConfig(Map<String, Object> metadata) {
    Object webEndpoint = metadata.get("web-endpoint");
    return webEndpoint instanceof WebEndpointConfig ? (WebEndpointConfig) webEndpoint : null;
}

// 路径 B：从 Environment 再绑定（onFinish 用）
public static WebEndpointConfig getWebEndpointConfig(Environment environment, String configPrefix) { ... }
```

- **路径 A** 是 16-03/16-04 的 `isExcludedRequest` 依赖的：绑定阶段注入的对象在 `GatewayProperties` 持有的 `RouteDefinition.metadata` 里存活，刷新/请求时直接 `instanceof` 取用——**热路径零成本**。
- **路径 B** 只在绑定钩子里用。

---

## 六、与 15-configuration 的关系（两条技术路线）

| | 15-configuration | 16 的绑定钩子 |
|---|---|---|
| 核心抽象 | `@PropertySourceExtension` 元注解 + `PropertySourcesChangedEvent` | `BindListener` + `ConfigurationPropertiesBindHandlerAdvisor` |
| 作用层 | PropertySource 层（配置源管理） | 绑定层（配置 → 对象） |
| 变更通知 | `PropertySourcesChangedEvent`（15 自有事件） | `EnvironmentChangeEvent`（spring-cloud-context 标准） |
| 两者关系 | **互不感知** | 16 的 excludes 绑定不依赖 15 的注解模型；15 的配置源变更经 Spring Cloud refresh 通道进入 16 的事件链（16-05） |

**一句话**：15 管"配置从哪来"，16 管"配置变对象"——同一套配置系统（microsphere），两条独立扩展技术。这也解释了 16-05 的"双通道"：配置中心的实时性最终落在 `EnvironmentChangeEvent` 上，与 15 是否在场无关。

---

## 七、绑定全链时序（引用用）

```
启动
 → spring.factories 注册 WebEndpointApplicationContextInitializer（上下文创建早期）
   → 注册 BindListener bean（webEndpointConfigurationPropertiesBindListener）
 → 04 的 EnableConfigurationPropertiesExtension 注册 Advisor（ConfigurationPropertiesBindHandlerAdvisor 官方扩展点）
 → GatewayProperties（@ConfigurationProperties）绑定
   → ConfigurationPropertiesBindingPostProcessor → Advisor.apply → ListenableBindHandlerAdapter 包装
   → 绑定 route[N].metadata 完成 → onFinish
     → 二次 bind web-endpoint（ValidationBindHandler 校验）
     → metadata.put("web-endpoint", WebEndpointConfig)
 → ContextRefreshedEvent → 刷新链（16-05）→ buildExcludedRequestMappingInfoSet → excludes 编译为 RequestMappingInfo
 → 请求 → isExcludedRequest → getMatchingCondition 命中 → 放行
```

---

## 八、小结（引用要点）

- **三层机制**：`ApplicationContextInitializer`（注册，spring.factories）→ `ConfigurationPropertiesBindHandlerAdvisor`（接入，04 模块按类型收集）→ `onFinish`（执行，二次绑定 + 回填）。
- **绑定与事件各司其职**：绑定管"配置 → 对象"，事件链管"对象变化 → 缓存刷新"——缺一段 excludes 都不生效。
- **默认值陷阱**：`methods` 缺省 = 全方法、Ant 模式非正则——写 excludes 时最易踩的两点。
- **与 15 无耦合**：15 管配置源、16 管绑定，互不感知，实时性统一落在 `EnvironmentChangeEvent`。
