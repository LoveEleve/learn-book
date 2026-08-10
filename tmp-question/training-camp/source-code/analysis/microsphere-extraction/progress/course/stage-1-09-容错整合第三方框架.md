# stage-1 · 第 9 节：容错整合第三方框架 — 知识点提取

> 课程：stage-1 服务治理 第 9 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/09. 第九节：Resilience4j 整合第三方框架.md`
> 提取时间：2026-08-09 | 权重：核心（扩展点/拦截器是框架整合的关键）
> **参考实现说明**：docs 标题为 Resilience4j，按方法论 08 §1.3.1 参考实现以 **Sentinel** 为主。

---

## 一、本节概览

- **技术域**：把容错能力整合进第三方框架（Feign/MyBatis/Redis）——核心是**扩展点/拦截器机制**
- **维度**：`[工程问题]`（扩展点/拦截器/装饰器）+ `[分布式问题]`（容错接入）
- **核心命题**：如何通过第三方框架的扩展点（拦截器/装饰器），把容错（或任意横切能力）接入 Feign/MyBatis/Redis
- **知识点数**：6 个
- **前置**：OpenFeign、MyBatis Interceptor、Spring Redis、装饰器模式

## 前置条件清单
读者需先掌握：
1. **Spring Cloud OpenFeign**（InvocationHandler/MethodHandler 机制）
2. **MyBatis 插件机制**（Interceptor/Plugin.wrap）
3. **Spring Redis**（RedisConnection/RedisTemplate）
4. **装饰器模式**（FeignDecorator 组合）
未达前置者，先补：OpenFeign 原理 + MyBatis 插件 + 装饰器模式

## 掌握度
目标读者：**本人（读源码多，Spring/扩展机制熟悉）** — 已确认
讲解策略：扩展点/装饰器直接讲（你熟悉）；补具体框架的扩展点细节

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 容错整合第三方框架的需求与本质（扩展点）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **需求**：把容错能力接入 Feign/MyBatis/Redis，而不侵入业务代码
- **自主实现**：找每个框架的**扩展点**（拦截器/装饰器/切面），在扩展点织入容错逻辑
- **参考实现**：Feign 用 InvocationHandler 装饰；MyBatis 用 Interceptor；Redis 用 RedisConnectionInterceptor；Sentinel 用各层 Filter/Adapter
- **对比取舍**：**本质是"扩展点织入横切能力"**——容错/Tracing 等都是通过扩展点接入（docs 明说"学会寻找扩展点，尤其是拦截器部分"）
- **测试佐证**：microsphere-alibaba-sentinel 有 mybatis/redis/druid/spring-web 多层扩展

### KP-02 装饰器模式在框架整合中的应用
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：装饰器模式
- **需求**：组合多个容错能力（熔断+隔离）装饰同一调用
- **自主实现**：用装饰器把原始调用逐层包装，可组合多个能力
- **参考实现**（docs）：Feign 的 `FeignDecorators` 组合多个 `FeignDecorator`（Bulkhead + CircuitBreaker）→ `DecoratorInvocationHandler` 把每个 `MethodHandler` 装饰成 `CheckedFunction`
- **对比取舍**：装饰器可组合、可嵌套，是框架整合横切能力的核心模式
- **测试佐证**：`DecoratorInvocationHandler.decorateMethodHandlers` 遍历 dispatch，用 invocationDecorator.decorate() 包装每个 methodHandler

### KP-03 容错接入 OpenFeign
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：OpenFeign、KP-02
- **需求**：Feign 调用自动熔断/限流
- **自主实现**：拦截 Feign 的 InvocationHandler，包装方法调用
- **参考实现**：docs 用 `FeignDecorator` + `DecoratorInvocationHandler`（Resilience4jFeign）；Sentinel 有 Feign 适配（spring-cloud-alibaba sentinel feign）
- **对比取舍**：Feign 的扩展点在 InvocationHandler（JDK 动态代理），通过装饰 MethodHandler 织入容错
- **测试佐证**：`code/spring/spring-cloud-openfeign` 源码可验证 Feign InvocationHandler 机制

### KP-04 容错接入 MyBatis（Executor 拦截）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：MyBatis 插件机制
- **需求**：MyBatis 执行 SQL 时自动限流/熔断
- **自主实现**：实现 MyBatis `Interceptor`，在 `plugin()` 包装 Executor，拦截 SQL 执行
- **参考实现**：
  - docs：`Resilience4jMyBatisInterceptor implements Interceptor`，plugin() 里 `decorateExecutor((Executor)target)` 包装 Executor
  - Sentinel：`SentinelMyBatisExecutorFilter`（microsphere-alibaba-sentinel-mybatis）
- **对比取舍**：MyBatis 扩展点是 `Interceptor` + `Plugin.wrap`（动态代理）；拦截 Executor 即可覆盖 SQL 执行
- **测试佐证**：microsphere-alibaba-sentinel `mybatis/executor/SentinelMyBatisExecutorFilter.java` 存在

### KP-05 容错接入 Spring Redis（命令拦截）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring Redis、装饰器
- **需求**：Redis 命令执行时自动容错/监控
- **自主实现**：包装 RedisConnection，在命令执行前后织入逻辑
- **参考实现**：
  - microsphere-redis 定义扩展点 `RedisConnectionInterceptor`（命令级拦截）
  - Sentinel 实现 `SentinelRedisCommandInterceptor`（继承 AbstractSentinelPlugin、实现 RedisConnectionInterceptor，用 SentinelTemplate 织入容错）
- **对比取舍**：Redis 扩展点是 `RedisConnectionInterceptor`（命令级），Sentinel 通过实现它接入——**扩展点(接口)与容错实现(插件)分离**
- **测试佐证**（已源码验证）：`stage-4/microsphere-redis/.../RedisConnectionInterceptor.java` + microsphere-alibaba-sentinel `SentinelRedisCommandInterceptor.java`（extends AbstractSentinelPlugin implements RedisConnectionInterceptor）

### KP-06 扩展点/拦截器机制（贯穿总结）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02~05
- **需求**：理解"扩展点"的通用本质，能迁移到任意框架
- **自主实现**：识别框架的调用链入口 → 找可拦截点（动态代理/拦截器/装饰器）→ 织入横切逻辑
- **参考实现**：Feign(InvocationHandler)/MyBatis(Interceptor)/Redis(Connection 包装)都是"在调用链入口加装饰/拦截"
- **对比取舍**：**扩展点的本质 = 在调用链的可插拔位置织入横切关注点**（容错/Tracing/监控）。学会找扩展点比学某个容错框架更重要（docs 明说）
- **关联 microsphere**：microsphere-alibaba-sentinel 用各层 Filter/Interceptor 把 Sentinel 接入 mybatis/redis/druid/web

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| 整合需求与本质(扩展点) | 工程 | 核心 | P1 | 🔴 | High |
| 装饰器模式应用 | 工程 | 核心 | P1 | 🔴 | High |
| 接入 OpenFeign | 工程 | 核心 | P1 | 🟡 | High |
| 接入 MyBatis | 工程 | 核心 | P1 | 🔴 | High |
| 接入 Redis | 工程 | 核心 | P1 | 🟡 | High |
| 扩展点机制总结 | 工程 | 核心 | P1 | 🔴 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **Sentinel×MyBatis**：microsphere-alibaba-sentinel `mybatis/executor/SentinelMyBatisExecutorFilter.java`
- **Sentinel×Redis**：microsphere-alibaba-sentinel `SentinelRedisCommandInterceptor.java`（extends AbstractSentinelPlugin implements RedisConnectionInterceptor）
- **Redis 扩展点**：`stage-4/microsphere-redis` 的 `RedisConnectionInterceptor.java`
- **多层扩展**：microsphere-alibaba-sentinel 覆盖 mybatis/redis/druid/spring-web
- **OpenFeign**：`code/spring/spring-cloud-openfeign` 源码可验证 InvocationHandler

---

## 五、本节小结（三层次视角）

**需求**：把容错能力整合进第三方框架（Feign/MyBatis/Redis），不侵入业务代码。

**自主实现核心**：若我设计——
1. 识别每个框架的扩展点（调用链可插拔位置）
2. **扩展点(接口)与容错实现(插件)分离**——框架定义扩展点接口，容错框架实现它接入
3. 用装饰器/拦截器在扩展点织入容错
4. Feign:InvocationHandler 装饰 MethodHandler
5. MyBatis:Interceptor + Plugin.wrap 包装 Executor
6. Redis:实现 RedisConnectionInterceptor 拦截命令

**参考实现**：docs 的 FeignDecorator/MyBatisInterceptor + Sentinel 实现 microsphere 定义的扩展点（`SentinelRedisCommandInterceptor implements RedisConnectionInterceptor`）实证。

**对比取舍**：本篇知识本体是"**扩展点/拦截器机制**"——比具体容错框架更重要。**关键洞察：框架(microsphere)定义扩展点接口，容错实现(Sentinel)实现该接口接入**——扩展点与实现分离。docs 标题是 Resilience4j，但按方法论 08，参考实现用 Sentinel（有源码）。

**待验证汇总**：
- Feign InvocationHandler 具体机制（可用 code/spring/spring-cloud-openfeign 验证）
- MyBatis Plugin.wrap 动态代理细节
