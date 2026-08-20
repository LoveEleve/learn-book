# N-09 命名控制器面 — HTTP 入口与 v2/v3 分代

> 前置: [[NC-6-服务端核心]] (Operator 层) | 对照: 1.x 单代控制器 → 3.x v2/v3 分代
> 🟡 B | 方案 B (重要域) | 闭环: q1(入口面) q2(分代) q3(trace)

**读者处境**: 客户端/控制台的 HTTP 请求怎么进服务端? v2/v3 控制器差异?

### 1. 控制器面 — 六控制器

场景: HTTP 入口怎么组织?
源码路径:
- **InstanceController** (controllers/InstanceController.java:90): 实例面 — **registerInstance** (L127 → getInstanceOperator) + removeInstance (L154 + **DeregisterInstanceTraceEvent** L155-156) + updateInstance (L180) + **batchUpdateMetadata** (L213)
- **ServiceController**: 服务面 (创建/查询)
- **ClusterController**: 集群面
- **HealthController**: 健康面
- **CatalogController**: 目录面
- **OperatorController**: 运维面
- 统一走 **InstanceOperator** 抽象 (L127)
关键设计 (q1): **"控制器 → Operator 单跳"** — 控制器无业务逻辑, 全部委托 Operator; trace 事件在入口发布。 [模式: 薄控制器]

### 2. 分代 — v2/v3 变体

场景: 分代控制器怎么共存?
源码路径:
- **controllers/v2/** (6): v2 控制器 (2.x http api)
- **controllers/v3/** (7): v3 控制器 (3.x 新 API)
- 同功能多代并存 — 兼容期策略
关键设计 (q2): **"分代共存 = 向后兼容"** — 老客户端走 v2, 新能力走 v3。 [模式: 分代兼容]

### 3. 测试与行为锚

场景: 控制器边界?
源码路径:
- 测试: InstanceControllerTest (naming test)
- trace: DeregisterInstanceTraceEvent 入口发布
关键设计 (q1): **"trace 入口 = 注册变更全链路可观测"**。 [模式: 入口 trace]
