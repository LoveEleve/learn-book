# 09-03：自动配置错误案例、测试基建缺陷与生产评估

> **核心命题**：`@ConditionalOnBean(name="io.micrometer.core.instrument.Clock")` 用类全限定名当 bean 名称，导致 `@ConditionalOnEnabledPrometheusMetricsExport` 永远不匹配、Prometheus Push Gateway 整条推送链路完全失效——这是与 09-01 的 `@EventListener(ApplicationPreparedEvent)` 时序 bug 并列为本模块两大严重 bug，二者共同折射出"框架 API 参数语义混淆"和"事件时序边界"两类系统性风险。此外，spring-boot 侧的自动配置测试全部是"装配起来即通过"的浅层测试，没有验证核心业务逻辑——这种测试覆盖差异与本模块的 bug 未被发现，是直接关联的因果关系。

---

## 一、第二个严重 bug：`@ConditionalOnBean(name=...)` 参数语义混淆

### 1.1 三层证据链

**第一层：Spring Boot 官方 Clock bean 的真实名称**

```java
// MetricsAutoConfiguration.java:54（Spring Boot 官方源码）
public Clock micrometerClock() {
    return Clock.SYSTEM;
}
```

Spring 默认按**方法名**生成 bean 名称——Clock Bean 的名字是 `"micrometerClock"`。

**第二层：`ConditionalOnBean.name()` 的参数语义是 bean 名称，不是类名**

```java
// ConditionalOnBean.java:120-124（Spring Boot 官方注解定义）
/**
 * The names of beans to check. The condition matches when all the bean names
 * specified are contained in the BeanFactory.
 * @return the names of beans to check
 */
String[] name() default {};
```

`name` 属性明确按**bean 名称**匹配（容器里 `beanFactory.containsBean(beanName)` 的逻辑），不是按类的全限定名查找。如果要按类型匹配，应该用 `value()` 属性（`Class<?>[]` 类型）。

**第三层：bug 行——把全限定类名当 bean 名称传给了 `name` 参数**

```java
// ConditionalOnEnabledPrometheusMetricsExport.java:42（本模块源码）
@ConditionalOnBean(name = "io.micrometer.core.instrument.Clock")
```

`"io.micrometer.core.instrument.Clock"` ≠ `"micrometerClock"`——这个条件**永远不会匹配成功**。

### 1.2 为什么这个 bug 值得深挖：同一个注解文件里，`name` 在家族内部语义不一致

把镜头拉开——看**同一个文件**里另外一行注解：

```java
// ConditionalOnEnabledPrometheusMetricsExport.java:42-44（同一个注解定义类）
@ConditionalOnBean(name = "io.micrometer.core.instrument.Clock")          // ← 错误：用 FQCN 传 name，期待 bean 名匹配
@ConditionalOnEnabledMetricsExport("prometheus")
@ConditionalOnClass(name = "io.micrometer.prometheus.PrometheusMeterRegistry")  // ← 正确：ConditionalOnClass.name 本来就是传 FQCN 的
```

**在 Spring Boot 的 `@ConditionalOn*` 注解家族里，同名属性 `name` 的语义是不统一的**：

- `@ConditionalOnClass(name=...)` → `name` 接受类的全限定名字符串（因为 Class 可能不在当前 classpath 上，不能直接写 `.class`）
- `@ConditionalOnBean(name=...)` → `name` 接受 Bean 名称（因为 Bean 注册时用的是名字，不是类名）

作者在同一文件里对两种注解都用了 `name="完整包路径字符串"` 的写法——这是从 `ConditionalOnClass` 的使用模式**无意识地迁移到** `ConditionalOnBean` 上了。这不是"参数用反了"这种低级错误，而是 Spring Boot 条件注解 API 本身的**设计陷阱**：同一个词 `name` 在不同上下文的语义不同，但 IDE 和编译器都不会警告。理解这一点比记住"要改成 `value=Clock.class`"更有价值——它解释了为什么这种 bug 在生产代码中反复出现（不是个别作者的粗心，是 API 设计本身的易错性）。

### 1.3 连锁失效路径

```
@ConditionalOnEnabledPrometheusMetricsExport 永远不满足
    ↓
PrometheusMetricsConfiguration 永远不会被激活
    ↓
  ├─ prometheusPushGatewayManager Bean 永远不会被创建
  │   → Prometheus Push Gateway 推送完全失效
  │
  └─ @ConditionalOnEnabledPrometheusPushGateway 也永远不满足
      → 任何依赖 PushGateway 的二级功能同样失效
```

**这是一个"静默失效"——没有任何异常日志、没有启动失败提示、没有 WARN**。条件不满足只是 Bean 不被注册，Spring 不会报告"某个条件未满足"给用户，除非显式打开 `debug: true` 查看条件评估报告。

### 1.4 与 P1（`@EventListener` 用时错误）的共性

两个 bug 分属不同类别（P1 是事件生命周期理解偏差，P2 是注解参数语义混淆），但在**影响模式**上高度一致：

| 维度 | 09-01 P1 | 09-03 P2 |
|------|---------|---------|
| 错误类型 | `@EventListener` 监听 `ApplicationPreparedEvent` 在时序上不可能接收 | `@ConditionalOnBean(name=全限定类名)` 参数语义误用，永远不匹配 |
| 失败模式 | 静默失效——Kafka Appender 从未挂载，日志静默丢弃 | 静默失效——Bean 不创建，Push Gateway 推送完全失效 |
| 连锁影响 | KafkaClientMetrics 也永不绑定 | 所有依赖 PushGateway 的二级功能同样失效 |
| 是否有运行时错误 | 无 | 无 |
| 修复方式 | 改用 `spring.factories` + `ApplicationListener<ApplicationPreparedEvent>` / 或改为监听晚期事件 | `name="micrometerClock"` 或改用 `value=Clock.class` |

**两类"框架 API 参数陷阱"**：
- **事件时序陷阱**（09-01 P1）：`@EventListener` 注解可以声明早期事件类型，但接收能力受限于容器生命周期顺序——语法上允许，语义上无效
- **参数语义陷阱**（09-03 P2）：`ConditionalOnBean` 的 `name`/`value`/`type` 三个属性有不同的匹配方式，混淆了 `name`（bean 名称）和全限定类名——命名上允许，语义上无效

---

## 二、`MicrometerAutoConfiguration` 全景

`MicrometerAutoConfiguration`（`MicrometerAutoConfiguration.java:91-294`）用 6 个内部 `@Configuration` 类管理所有指标组件：

| 内部配置类 | 条件 | 注册的组件 |
|-----------|------|----------|
| `SystemConfiguration` | `microsphere.micrometer.system.enabled`（默认 true） | `NetworkStatisticsMetrics` + `SystemMemoryMetrics` |
| `CGGroupConfiguration` | `microsphere.micrometer.cgroup.enabled` + `@ConditionalOnCGroup` | `CGroupMemoryMetrics` |
| `JvmConfiguration` | `microsphere.micrometer.jvm.enabled`（默认 true） | 官方 `JvmCompilationMetrics`/`JvmInfoMetrics` + 手动扫描 `ExecutorService` Bean 绑定线程池指标 |
| `SentinelMetricsConfiguration` | `microsphere.micrometer.sentinel.enabled` + `@ConditionalOnSentinelEnabled` | `SentinelCollector`（有 Prometheus 时优先）/ `SentinelMetrics`（退化） |
| `KafkaMetricsConfiguration` | `microsphere.micrometer.kafka.enabled` + `@ConditionalOnClass(KafkaClient)` | `KafkaClientMetrics`（反射拿 KafkaAppender 内部 Producer） |
| `PrometheusMetricsConfiguration` | `microsphere.micrometer.prometheus.enabled` + `@ConditionalOnEnabledPrometheusMetricsExport`（**本文已证：此条件永不满足**） | `PrometheusPushGatewayManager`（**永不创建**） |

其中 PrometheusMetricsConfiguration 是整张表里唯一**完全失效**的配置类——其他 5 个虽然有不同程度的缺陷（环境适配范围、反射脆弱性等），但至少**会被激活**。Prometheus Push Gateway 推送是唯一一条"因条件注解 bug 导致整条装配路径完全走不通"的路径。

---

## 三、测试基建：系统性缺陷的完整对比

### 3.1 spring-boot 侧：3 个"装配完成 = 通过"的浅层测试

| 测试类 | 实际断言 | 验证了什么 | 没验证什么 |
|--------|---------|-----------|-----------|
| `ApplicationLoggingAutoConfigurationTest` | `assertTrue(uncaughtExceptionHandler instanceof LoggingUncaughtExceptionHandler)` | 侧重验证 `registerLoggingUncaughtExceptionHandlerAsDefault()` 的副作用——handler 被成功设置 | **没验证** `onApplicationStartedEvent` 的日志输出语义——debug 级别的 "Spring Boot startup" 日志有没有真正被打印？完全没测 |
| `WebMvcLoggingAutoConfigurationTest` | `GET /` → 断言 HTTP 404 | Spring Boot 能启动 + WebTestClient 能发请求 | **完全没验证** `onServletRequestHandledEvent` 是否有被触发——测试里的 `@Autowired WebMvcLoggingAutoConfiguration` 显示了 Bean 能被注入，但没有去断言该 Bean 的日志/事件是否真被接收。这份测试在"启动能正常收到请求"的层面已经合格，但无法证明"请求处理日志的事件监听器正确工作" |
| `WebServerLoggingAutoConfigurationTest` | `assertNotNull(webServerLoggingAutoConfiguration)` | Bean 存在 | **完全不验证** `onWebServerInitializedEvent` 是否执行——启动时 WebServer 绑定端口的事件有没有被正确接收到，没有检测 |

### 3.2 micrometer 侧：有真断言，但有其他问题

| 测试类 | 断言 | 问题 |
|--------|------|------|
| `CGroupMemoryMetricsTest` | 断言 16 个具体 metric 名存在 + 数值正确 | ✅ 充分——项目里少数有实质性断言的测试 |
| `NetworkStatisticsMetricsTest` | 断言 `registry.getMeters().isEmpty()` 为 false | WatchService 监听线程从未 `.start()`，`thread.join(5000)` 对未启动线程立即返回——动态发现新接口的验证是摆设 |
| `MicrometerJdbcEventListenerTest` | 调用 `addMetrics` 但**无任何断言**，传入值恰好等于慢 SQL 阈值 | P5 高基数 tag 代码行从未被测试覆盖 |

### 3.3 完全无测试的组件

- `Log4j2AutoConfiguration` / `KafkaAppenderConfiguration`——P1 的严重 bug 处于零测试保护
- `InMemoryAppender`——核心组件，无对应单元测试
- `SentinelMetrics` / `AbstractSentinelMetrics` / `SentinelCollector`——三套指标实现，零测试
- `MBeanMetrics` / `MBeanAttributeMeterBinder`——死代码骨架，零测试

### 3.4 测试缺陷与 bug 的因果关系

**两个严重 bug 都没有被测试发现，不是因为测试写得不好——是因为根本没有测试**。P1（`@EventListener` 时序错误）如果有一个集成测试验证 `initializeKafkaAppender()` 真的被执行、或者验证 `KafkaAppender` 真的出现在 `LoggerContext` 的 Appender 列表里，可以在开发阶段就被发现。P2（`ConditionalOnBean(name=...)` bug）如果 `PrometheusMetricsConfigurationTest` 验证 `prometheusPushGatewayManager` Bean 真的被创建，也可以在编码阶段就被发现。

需要格外注意这里因果关系的方向：**两个 bug 的根因是 API 参数语义混淆**（P1 是事件类型选择错误，P2 是 §1.2 所述的 ConditionalOnClass/ConditionalOnBean `name` 语义迁移），而不是"没有测试"。测试的缺失是**为什么 bug 没有被发现**的原因，不是**为什么 bug 存在**的原因。把这两条因果链分开，对于理解这类 bug 的防治策略有关键意义：修 bug 靠改代码（正确的事件类型 / 正确的 bean name），防 bug 靠补测试（让类似问题在编码阶段就能被捕获），两条路径不能混为一谈。

---

## 四、生产评估

### 4.1 本模块的历史与生态位置

- **创建时间**：2025-02-21（与 14-druid 2025-02-14 同一周，是 microsphere 最早的一批项目）
- **版本**：`0.0.1-SNAPSHOT`——从未正式发布，是本系列迄今最不活跃的项目
- **git 历史**：只有 1 个提交（shallow clone），与其他项目（13-mybatis 450 提交、14-druid 386 提交）形成显著反差
- **实际可用部分**：系统级指标采集（System/CGroup/Network）+ Sentinel 指标接 Micrometer 通用路径（`SentinelMetrics`）——这两部分在本地验证中可以正常工作
- **已证实完全失效的部分**：启动期日志缓冲（Kafka 路径）——P1；Prometheus Push Gateway 推送——P2

### 4.2 风险清单（汇总 09-01 至 09-03）

| 来源 | 风险 | 严重度 | 影响范围 |
|------|------|--------|---------|
| 09-01 P1 | `@EventListener(ApplicationPreparedEvent)` 永不触发 → Kafka Appender 从未挂载 → 启动期日志静默丢弃 + KafkaClientMetrics 永不绑定 | **高** | 所有依赖 Log4j2 Kafka Appender 的部署 |
| 09-03 P2 | `@ConditionalOnBean(name=FQCN)` 永不匹配 → Prometheus Push Gateway 推送完全失效 | **高** | 所有依赖 Push Gateway 的场景 |
| 09-01 P2 | `ConcurrentSkipListSet` 同毫秒日志去重丢失 | 中 | InMemoryAppender 活跃期间的高频日志 |
| 09-02 P1 | `parseStats` 无异常防护 → `ScheduledThreadPoolExecutor` 静默终止 | 低概率+高影响 | 极端运维场景 |
| 09-01 P5 | `I18nLogger` 仅 trace 一个重载实现 | 中 | 使用 i18n 日志装饰器的场景 |
| 09-02 P4/P5 | `MicrometerJdbcEventListener` static 字段污染 + 高基数 Tag | 中 | 多数据源 + Prometheus 后端 |
| 其他 | 一个抽象基类无实例（`AbstractSentinelMetrics`）+ 一个完整骨架零实现（`MBeanMetrics`） | 低 | 需要扩展功能的开发者需注意 |

### 4.3 可迁移的检测规则

针对本模块发现的两类系统性 bug，提炼两条检验规则和对应的实操方法，供后续 07-sentinel 等模块复用：

**规则一：`@EventListener` 事件类型检查**

如果 `@EventListener` 注解的方法监听的是 `ApplicationPreparedEvent` 或更早的事件（`ApplicationEnvironmentPreparedEvent` 等），**必须验证该事件是否晚于 `finishBeanFactoryInitialization()` 完成**。

实操方法：
1. 用 `grep -rn "@EventListener"` 扫描项目所有 Spring 配置类
2. 对每个捕获到的事件类型，查 `AbstractApplicationContext.refresh()` 里该事件在 publish/subscribe 链路中的位置
3. 如果事件早于 `finishBeanFactoryInitialization()`（第 628 行），且监听器是 `@EventListener` 注解方法（而非 `spring.factories` 注册的原生 `ApplicationListener`），标记为"可能无法触发"——需要进一步确认

**规则二：`ConditionalOnBean.name` 参数值检查**

如果 `name` 属性的值看起来像类全限定名（有包路径和至少一个 `.`），大概率是 `value`（类型匹配）和 `name`（bean 名称匹配）两个属性用混了。

实操方法：
1. 用 `grep -rn "ConditionalOnBean\(name"` 扫描项目所有使用
2. 检查 `name` 的值是否包含 `.` 分隔符——是 → 可能是 FQCN 误用，需要进一步确认实际 bean 名称
3. 查找目标 bean 的实际注册代码（`@Bean` 方法名 / `@Component` 类名），确认 bean 名称是否与 `name` 值匹配
4. 同文件内如果同时存在 `ConditionalOnClass(name=FQCN)`（正确用法），特别注意——这说明作者有"在 `@ConditionalOn*` 的 `name` 属性里写 FQCN"的习惯，在 `ConditionalOnBean` 上也可能延续这个模式，构成 §1.2 描述的同文件跨注解语义迁移误用

---

## 五、问题清单（汇总）

| 来源 | 编号 | 问题 | 证据 |
|------|------|------|------|
| 09-01 | P1 | `@EventListener` 时序 bug | 已证 |
| 09-03 | P2 | `@ConditionalOnBean(name=...)` 参数语义混淆 | 本文 §1 完整证 |
| 09-03 | P3 | spring-boot 侧 3 个自动配置测试全是"装配完成=通过"的浅层测试 | 本文 §3.1 |
| 09-03 | P4 | P1/P2 对应的核心组件完全没有测试——两个严重 bug 因此未被发现 | 本文 §3.4 |

---

## 六、小结（引用要点）

- **P2 与 P1 构成同源的双重失效模式**：`ConditionalOnBean(name=...)` 参数语义混淆（P2）和 `@EventListener` 事件类型选择错误（P1）都是"看似正确、实因参数语义误用而完全失效"的隐蔽陷阱，且都**完全静默**——无异常、无 WARN、无日志——这在 Spring Boot 条件装配体系里是最危险的一类 bug
- **测试覆盖缺失是 bug 未被发现的直接原因**：P1 的 `KafkaAppenderConfiguration` 和 P2 的 `PrometheusMetricsConfiguration` 对应的核心组件都完全没有测试——这不是偶然的测试疏忽，而是与"作者把早期阶段的项目骨架搭建后，还没来得及为每个配置类写对应的验证测试"的开发状态一致
- **项目整体状态**：`0.0.1-SNAPSHOT` 从未发布、1 提交的 shallow clone——这是本系列迄今最不活跃的项目，其"需求清晰但实现半成品"的状态与同期项目（如 14-druid 的 386 提交、0.2.19 已发布）形成显著反差
- **两条可推广的检测规则**：`@EventListener` 事件类型的前后时序检查 + `ConditionalOnBean.name` 的参数语义检查——已记录为可迁移规则，供后续 07-sentinel 等模块复用
