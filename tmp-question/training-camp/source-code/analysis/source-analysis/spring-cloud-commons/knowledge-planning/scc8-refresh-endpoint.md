# SCC-8 RefreshEndpoint + 事件 — 知识规划 (KP)

> 域: SCC-8 | 级别: 🟡 | 方案: B | 大纲: outlines/scc8-refresh-endpoint/outline.md (5 节)

## §01 域定位

刷新触发面 = HTTP 端点 (RefreshEndpoint) + 程序化事件 (RefreshEvent/RefreshEventListener) + 事件链 (EnvironmentChangeEvent 同源) + 健康集成 (RefreshScopeHealthIndicator)。SCC-2 刷新机制的对外窗口。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| endpoint/RefreshEndpoint.java | 51 | /actuator/refresh 薄门面 | 1 |
| endpoint/event/RefreshEvent.java | — | 程序化刷新事件 | 2 |
| endpoint/event/RefreshEventListener.java | — | SmartApplicationListener + ready 守卫 | 2 |
| context/environment/EnvironmentChangeEvent.java | — | 变更键事件 (同源) | 3 |
| context/environment/EnvironmentManager.java | — | manager 属性源 + JMX 管理 | 3 |
| health/RefreshScopeHealthIndicator.java | — | 刷新错误健康面 | 4 |
| autoconfigure/RefreshEndpointAutoConfiguration.java | — | 条件装配 | 5 |

## §05 闭环要点 (Pass 2 内化)

### q1 双触发面
RefreshEndpoint (HTTP @WriteOperation) + RefreshEventListener (RefreshEvent 程序化) + ready 原子标志守卫 (就绪前忽略)。

### q2 事件链
EnvironmentChangeEvent 同源不同发布者: ContextRefresher.refreshEnvironment (SCC-2) + EnvironmentManager.reset/setProperty (L73/92)。

### q3 健康集成
RefreshScopeHealthIndicator 聚合 RefreshScope.getErrors + rebinder.getErrors → up/down。

### q4 装配条件
RefreshEndpoint 四条件 (ConditionalOnBean + AvailableEndpoint + MissingBean) + HealthIndicator @ConditionalOnEnabledHealthIndicator。

## §06 负面空间 (6 条)

不做配置中心推送 / 不做刷新调度 / 不做结果持久化 / 不做分布式协调 / 不做鉴权内建 / 不处理刷新并发

## §07 交叉引用

- ← SCC-2 @RefreshScope (ContextRefresher 委托) + SCC-1 Bootstrap (环境)
- → SCC-9 配置加密 (decrypt 重跑面)
- 另见: Spring Cloud Bus (广播对照) / Apollo (推送对照)
