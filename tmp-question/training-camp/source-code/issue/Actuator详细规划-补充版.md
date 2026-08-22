# Actuator 详细规划（补充版）

> 目标：把当前 `vol-spring-boot` 中已经起步的 Actuator / Metrics / Health 正文，收束成一条完整、可连续写作、与 `vol-spring` / `vol-spring-boot` 主干紧密相连的运维子系统主线。
> 适用位置：Spring Boot 卷后半段，位于 `ApplicationAvailability` 之后、Cloud 之前。

---

## 一、为什么要单独补 Actuator 详细规划

当前 `vol-spring-boot` 已经写到：

- `21-actuator-endpoints.md`
- `22-metrics-and-health.md`
- `20-application-availability.md`
- `17-failure-analyzer.md`
- `18-configdata.md`
- `19-logging-system.md`

这些篇已经把 Boot 生产层的核心能力拆开了，但仍然存在一个结构问题：

- 这些正文还像“生产能力若干篇”，还没有完全收束成一条**Actuator / 观测 / 诊断 / 运行入口**的完整子系统主线。

更具体地说，当前缺的是：

1. **端点重要性排序还不够清楚**
   - 哪些是每个项目都应该重点理解的核心端点
   - 哪些是事故排查专项端点
   - 哪些只是场景增强端点

2. **端点和能力源的映射还没完全卷级化**
   - Health / Metrics / Info / Loggers / Conditions / ConfigProps / Mappings / Startup
   - 它们分别消费哪些内部能力源
   - 这些能力源和 `vol-spring`、`vol-spring-boot` 既有正文怎么桥接

3. **事故排查专项端点还没纳入明确规划**
   - `heapdump`
   - `threaddump`

你已经明确指出：

- 这些不是边缘能力
- 至少在生产事故排查视角里，它们是核心端点

这个判断是对的。

因此，Actuator 不能只停在“总论 + Health/Metrics 两篇”，而要补成一条完整规划。

---

## 二、Actuator 在整卷中的准确定位

Actuator 不应该被看成：

- 多几个监控 URL
- 或 Spring Boot 附带工具箱

更准确的定位是：

- **Spring Boot 为“运行中的应用”建立的一套统一运维子系统**

它负责把：

- 健康状态
- 指标
- 构建信息
- 自动配置诊断
- 配置绑定结果
- Web 路由信息
- 日志级别调整
- 启动过程信息
- 线程与堆转储

统一组织成：

- 可扩展的内部能力源模型
- 可桥接的 endpoint 模型
- 可受控暴露的运维入口

也就是说，Actuator 不是业务主线的附属，而是：

- **Boot 生产运行模型的外部可观测接口层。**

---

## 三、Actuator 详细分层

我建议把 Actuator 详细规划拆成五层：

### A. 端点总论层

回答：
- 什么是 endpoint 模型
- 为什么不用普通 Controller
- Web / JMX 桥接怎么建立
- 暴露控制如何工作

### B. 日常运行核心端点层

回答：
- 一个应用在正常运行时，运维最常看的核心入口是什么

### C. 自动配置诊断核心端点层

回答：
- Boot 自动配置到底装了什么、没装什么、映射了什么、绑定了什么

### D. 启动观测层

回答：
- 启动慢怎么查
- 启动过程中发生了什么

### E. 事故排查核心端点层

回答：
- 应用出故障或性能异常时，哪些端点真正帮助定位问题

### F. 扩展点模型层

回答：
- 第三方如何扩展 Health / Info / Metrics

---

## 四、建议保留的 Actuator 核心端点分组

### 1. 日常运行核心

这些是每个项目都应该重点理解的：

- `health`
- `metrics`
- `info`
- `loggers`

### 2. 自动配置诊断核心

这些是最能体现 Boot 主线价值的：

- `conditions`
- `configprops`
- `mappings`
- `beans`（可选，放进 mappings / conditions 诊断篇中附带）

这里必须额外强调：

- `conditions` 回答“为什么装 / 没装”
- `configprops` 回答“最后绑定成了什么”
- `mappings` 回答“最后运行出来的 Web 入口是什么”

这三者合起来，不是三个并列端点，而是：

- **Boot 自动配置诊断的最小闭环**

### 3. 启动观测核心

- `startup`

### 4. 事故排查核心

这些不一定天天用，但出问题时非常关键：

- `threaddump`
- `heapdump`
- `env`（可作为诊断附带，而不是单独顶层核心）

这里也必须补一句：

- `threaddump` / `heapdump` 不只是工具型端点
- 它们还是把前面日志、metrics、基础设施和资源问题重新串起来的事故证据入口

### 5. 扩展点模型

不是 endpoint 本身，但需要被单独讲清：

- `HealthIndicator` / `HealthContributor`
- `InfoContributor`
- `MeterBinder`
- `Availability` 状态模型作为 Health 的能力源之一

---

## 五、建议篇目结构

### Actuator-1：Actuator 端点体系总论

对应当前：
- `21-actuator-endpoints.md`

核心回答：
- 为什么 Actuator 是独立端点子系统
- `@Endpoint` / `@ReadOperation` / `@WriteOperation`
- Web / JMX 暴露桥接
- 暴露控制 `management.endpoints.web.exposure.*`

### Actuator-2：Health 与 Metrics 深化

对应当前：
- `22-metrics-and-health.md`

核心回答：
- Health = 判断语义
- Metrics = 量化语义
- `ApplicationAvailability` 如何成为 Health 能力源之一
- `HealthIndicator` / `MeterBinder` 为什么是不同内容源模型

### Actuator-3：Info 端点与 InfoContributor

建议新增正文：
- `33-actuator-info.md`

核心回答：
- 为什么 `info` 不是静态文本
- `InfoContributor` 怎样聚合构建、Git、OS 等信息
- 为什么它属于日常运行信息入口，而不是业务接口

### Actuator-4：Loggers 端点与运行时日志级别调节

建议新增正文：
- `34-actuator-loggers.md`

核心回答：
- `loggers` 为什么是运行时调试入口而不是日志系统本身
- 如何理解日志系统初始化 vs 运行时日志级别调节
- 为什么它属于“运行时操作入口”，而不是简单信息展示

### Actuator-5：Conditions / ConfigProps / Mappings 诊断闭环

建议新增正文：
- `35-actuator-conditions-configprops-mappings.md`

核心回答：
- `conditions`：自动配置为什么命中 / 未命中
- `configprops`：最终配置绑定结果是什么
- `mappings`：Web 路由最终长什么样
- 为什么三者构成 Boot 自动配置诊断的最小闭环

这一篇应该和前面的：
- Boot 条件体系
- `@ConfigurationProperties`
- Web MVC 自动装配

形成强回链。

### Actuator-6：Startup 端点与启动观测

建议新增正文：
- `36-actuator-startup.md`

核心回答：
- `ApplicationStartup`
- 启动阶段埋点
- `startup` endpoint 如何把启动时序暴露出来
- 它与 `FailureAnalyzer`、日志系统、`ConfigData` 的关系

### Actuator-7：ThreadDump / HeapDump 事故排查端点

建议新增正文：
- `37-actuator-dumps.md`

核心回答：
- 为什么 `threaddump` / `heapdump` 是事故排查核心，不是边缘端点
- 它们和日志、metrics、startup 的关系
- 它们怎样回链到前面的 DataSource / Redis / Cache / 线程模型等基础设施问题
- 什么时候看线程栈，什么时候看堆快照
- 暴露这些端点为什么必须更加谨慎

---

## 六、为什么这 6 篇顺序合理

### 1. 先总论

不先立 endpoint 模型，后面的 health / metrics / conditions 都会被理解成零散功能。

### 2. 再 Health / Metrics

这是日常运行最核心的两条能力源主线。

### 3. 再 Info / Loggers

把“日常运行时信息”补完整。

### 4. 再 Conditions / ConfigProps / Mappings

把“自动配置与 Web 运行结果如何诊断”补完整。

### 5. 再 Startup

把“启动过程怎么观测”补完整。

### 6. 最后 Dumps

把“事故排查专项能力”补完整。

这样顺序是：

```text
端点模型
  -> 日常运行状态（Health / Metrics / Info / Loggers）
  -> 自动配置诊断闭环
  -> 启动观测
  -> 事故排查
```

这比把端点按名字平铺更符合读者认知路径。

---

## 七、和已有正文的映射关系

### 已有可直接保留

- `21-actuator-endpoints.md`
- `22-metrics-and-health.md`
- `20-application-availability.md`
- `17-failure-analyzer.md`
- `18-configdata.md`
- `19-logging-system.md`

### 需要新增的 Actuator 专项正文

- `33-actuator-info-and-loggers.md`
- `34-actuator-conditions-configprops-mappings.md`
- `35-actuator-startup.md`
- `36-actuator-dumps.md`

### 需要加强交叉引用

- `04-boot-conditional-system.md` ↔ `conditions`
- `06-configurationproperties.md` ↔ `configprops`
- `08-webmvc-autoconfiguration.md` / `10-dispatcherservlet-registration.md` / `11-httpmessageconverters-and-json.md` ↔ `mappings`
- `05-springapplication-run.md` / `17-failure-analyzer.md` / `18-configdata.md` / `19-logging-system.md` ↔ `startup`
- `20-application-availability.md` ↔ `health`

---

## 八、哪些不应继续无限扩张

以下端点可以提及，但不建议现在单独拉出很多篇：

- `beans`
- `env`
- `caches`
- `scheduledtasks`
- `quartz`
- `liquibase`
- `flyway`
- `httpexchanges`
- `sbom`
- `logfile`

理由不是“不重要”，而是：

- 当前阶段应先补齐运维主线核心
- 这些更适合作为诊断篇中的附带能力，或者在后续专项卷里展开

---

## 九、一句话结论

**Actuator 这一段不应只停在“总论 + Health/Metrics”，而应补成 6 篇完整子线：端点模型、日常运行核心、自动配置诊断、启动观测、事故排查专项。优先把 `health / metrics / info / conditions / configprops / mappings / loggers / startup / heapdump / threaddump` 收口完，再进入 SpringCloudCommons 与 SpringCloudAlibaba。**