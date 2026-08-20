# N-16 配置控制器面 — HTTP 入口与切面族

> 前置: [[N-15-配置存储]] (操作服务消费) | 对照: 控制器零业务 + 切面横切
> 🟡 B | 方案 B (重要域) | 闭环: q1(入口面) q2(切面族)

**读者处境**: 配置的 HTTP 入口怎么组织? 容量/变更/日志切面?

### 1. 控制器面 — ConfigController 与九控制器

场景: 配置 HTTP 入口?
源码路径:
- **ConfigController** (controller/ConfigController.java:113): 核心 — **publishConfig** (L161-222: → **configOperationService.publishConfig** L222) + **getConfig** (L240) + getConfigAdvanceInfo (L344) + beta 操作 (L469-500) + export (L536)
- 九控制器: CapacityController / ClientMetricsController / CommunicationController / ConfigOpsController / ConfigServletInner / HealthController / HistoryController / ListenerController + v2/v3
- 全部委托 service 面 (N-15)
关键设计 (q1): **"薄控制器 = 全部委托"** — 与命名面 (N-09) 同构; beta/export 是配置专属能力。 [模式: 薄控制器]

### 2. 切面族 — aspect 四件

场景: 横切逻辑怎么组织?
源码路径:
- **CapacityManagementAspect** (aspect/CapacityManagementAspect.java:48): 容量管理
- **ConfigChangeAspect**: 变更记录
- **ConfigOpFailureAspect**: 操作失败
- **RequestLogAspect**: 请求日志
关键设计 (q2): **"切面 = 横切分离"** — 容量/变更/失败/日志四个切面, 不污染控制器。 [模式: 切面横切]

### 3. 测试与行为锚

场景: 控制器边界?
源码路径:
- 测试: ConfigControllerTest (config test)
- 锚: RestResult 统一返回
关键设计 (q1): **"统一返回 = 契约稳定"**。 [模式: 结果契约]
