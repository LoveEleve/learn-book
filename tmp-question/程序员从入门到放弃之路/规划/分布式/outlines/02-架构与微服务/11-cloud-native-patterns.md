# CI/CD、GitOps、12-Factor 与弹性 — 微服务怎样安全交付和持续演进

> Cluster D: 13 KPs | 依赖: 10-container-orchestration、08-resilience-patterns | 读者基线: Git、Docker、Kubernetes、灰度与回滚基础
> 读者处境: 10 篇解决了“怎么部署和编排”；本篇回答“怎么把代码、配置、环境、灰度、扩缩和回滚组织成可重复流水线”
> 打开新视角: 云原生不是“上了 K8s”就完成，而是**把构建、配置、部署、回滚、弹性和观测变成可审计的控制闭环**

---

### 概念依赖链

```
10 容器编排 + 08 韧性 → 本篇: 云原生交付模式
  ├─ §1 CI/CD(验证与发布流水线)
  ├─ §2 GitOps(声明式运维与回滚)
  ├─ §3 12-Factor(应用交付原则)
  ├─ §4 HPA/VPA/Cluster Autoscaler(弹性与成本)
  └─ §5 Serverless/AIOps(边界与趋势)
先讲: 提交到部署 → Git即真理源 → 应用设计原则 → 扩缩容 → 新范式
后续依赖: 12-architecture-evolution(真实大型系统演进)
```

### 叙事顺序

1. 问题引入——代码写完后，为什么“人肉 SSH + kubectl apply”迟早会成为事故来源？
2. CI/CD——从提交到发布的验证链
3. GitOps——把运维状态收编进 Git
4. 12-Factor——让应用适合自动化平台
5. 弹性伸缩——容量与成本不是只靠 HPA
6. Serverless/AIOps——新范式与边界
7. 收束

### 1. CI/CD — 流水线的价值是缩短反馈并标准化发布

场景提示: 一个简单配置改动从提交到上线要 3 小时，为什么慢的不只是构建，而是缺少统一验证和可回滚步骤？ [写作时展开]

关键设计: CI/CD 把编译、测试、镜像、部署和回滚组织成可重复流程：

```[pseudocode]
git push
  → compile/lint/unit test
  → package/build image
  → security/license/static checks
  → integration/regression/smoke tests
  → deploy to env
  → health check / rollout / rollback gate

CDelivery:
  自动到待发布状态, 人工批准进入生产

CDeploy:
  满足策略后自动到生产
```

Why: 为什么 CI 不是“能 build 通过就算完”？——**真正价值在于快速失败、统一验证和构建产物可追溯**；CD 也不是 `rsync` 或 `kubectl apply` 的别名，而是带健康检查、灰度、回滚和审批边界的发布流程。目标反馈时间不能机械承诺 5 分钟，取决于测试层次和风险模型。 [云原生工程: 流水线应把质量门禁、可回滚性和产物追踪编码进流程]

比喻锚点: CI/CD 像机场安检加登机流程，不只是确认你有行李，而是确认行李、身份、目的地和紧急返航路线都准备好了。 [写作时展开]

### 2. GitOps — 把“当前应该是什么”写进 Git

场景提示: 运维同学在生产集群里临时改了一个 YAML，几天后没人记得改过什么；如何避免“漂移配置”？ [写作时展开]

关键设计: GitOps 把期望状态放进 Git，由控制器持续比较 Git 和集群实际状态：

```[pseudocode]
Git repo
  → 声明镜像版本、配置、Deployment/Service 等期望状态

controller (Argo CD/Flux ...)
  → 监视 Git revision
  → 对比集群实际状态
  → sync/apply drift correction

rollback:
  revert Git commit
  → controller 同步回历史状态
```

Why: 为什么 GitOps 不是“用 Git 存 YAML”这么简单？——**它还要求自动同步、漂移检测、审计、权限和回滚语义**；如果生产允许大量手工改动，Git 就不再是真理源。GitOps 也不会自动解决密钥管理、数据库 schema 迁移和跨环境差异。 [云原生工程: Git 是期望状态源，控制器负责持续 reconcile，二者都必须纳入权限和审计]

比喻锚点: GitOps 像建筑蓝图的唯一正版仓库，施工队按蓝图持续校正现场；现场偷偷改动会被巡检拉回蓝图。 [写作时展开]

### 3. 12-Factor — 为什么“能跑”不等于“适合云原生”

场景提示: 同一镜像在开发环境能跑，到了测试环境端口冲突、日志丢失、配置硬编码，问题出在哪？ [写作时展开]

关键设计: 12-Factor 把应用设计约束成适合自动部署和弹性运行的形态：

```[pseudocode]
codebase:
  每个应用一个主代码库/发布源

dependencies:
  明确声明, 可重复构建

config:
  与代码分离, 通过环境/配置注入

backing services:
  数据库/缓存/队列视为可替换附着资源

logs:
  视为事件流, 交给平台收集

processes:
  无状态/可水平扩展, 把状态放外部存储
```

Why: 为什么 12-Factor 不是逐条教条执行清单？——**它是云平台友好的设计原则，不是所有系统一刀切答案**；例如“无状态”并不意味着业务没有状态，而是运行实例本身不私藏关键状态。环境变量也不是唯一配置手段，关键在于配置与镜像/代码解耦。 [云原生工程: 12-Factor 解决的是部署可移植性、自动化和环境一致性]

比喻锚点: 12-Factor 像把产品设计成标准集装箱：配置标签贴在箱外，日志和状态不散落在车厢里，任何港口都能按同样方式装卸。 [写作时展开]

### 4. 弹性伸缩与成本 — HPA 不是自动省钱魔法

场景提示: CPU 80% 就加 2 个 Pod，结果成本翻倍、延迟却没降；弹性伸缩究竟依赖哪些前提？ [写作时展开]

关键设计: HPA/VPA/Cluster Autoscaler 解决的是不同层次的容量问题：

```[pseudocode]
HPA:
  根据指标调整 Pod 副本数
  → 依赖 request/metric 和扩容窗口

VPA:
  调整 Pod CPU/memory request/limit
  → 适合资源画像不稳定的工作负载

Cluster Autoscaler:
  Pod 调度不下/节点空闲
  → 扩/缩 Node

成本策略:
  request/right-sizing
  混合实例/抢占式实例
  伸缩冷启动与最低容量
```

Why: 为什么 HPA 不能自动解决数据库瓶颈或热点分片？——**它扩的是无状态副本，而不是下游容量、单锁、单队列、单分片或外部 API 配额**；若指标只看 CPU，也可能错过队列堆积和 P99。伸缩策略还要与预热、缓存、连接池和冷启动成本协调。 [系统性能: 自动扩容前必须先知道瓶颈在哪一层，避免把问题复制到更多 Pod]

### 5. Serverless 与 AIOps — 不是下一代万能平台

场景提示: 函数计算看起来按调用计费、无需管服务器，为什么很多长连接或大状态服务并不适合搬过去？ [写作时展开]

关键设计: Serverless 与 AIOps 代表两种平台化趋势，但各有边界：

```[pseudocode]
Serverless:
  事件触发/函数实例/按使用计费
  → 适合短时、突发、无状态任务
  → 冷启动、执行时长、状态与连接约束

AIOps:
  异常检测/容量预测/告警聚合/根因建议
  → 帮助运维决策
  → 不能替代故障模型、Runbook 和人工校验
```

Why: 为什么“全迁到 Serverless/全靠 AIOps”通常不现实？——**长连接、长事务、高状态和低延迟系统常常需要持续驻留资源**；AIOps 也只能在足够好指标、事件和历史数据上工作，错误自动修复可能放大事故。趋势值得吸收，但不能把范式当药到病除。 [云原生工程: 新范式解决的是资源抽象和运维效率，不消灭状态和故障边界]

### 6. 收束

云原生交付闭环：

```[pseudocode]
code commit
  → CI quality gates
  → image artifact
  → GitOps desired state
  → deploy/rollout/rollback
  → 12-factor runtime discipline
  → HPA/VPA/CA capacity loop
  → observability + operations feedback
```

**Aha Moment**: "云原生不是‘用上 Kubernetes’这一件事，而是**从代码、配置、镜像、发布、回滚到伸缩都进入同一条声明式、可审计、可回放的控制循环**。"
**回答读者三问**: ①CI/CD 真正解决什么=标准化验证和发布边界；②GitOps 的核心是什么=Git 成为期望状态源并由控制器持续 reconcile；③为什么扩容不等于解决问题=副本增加不能替代下游容量和架构瓶颈。

---

### 核心悬念

**"通信、RPC、缓存、消息、分片、微服务、韧性、Mesh、容器、云原生都讲完了；真实大型网站是如何把这些阶段一层层叠上去，从单体演进到多活与单元化？"**

→ 引出 12-architecture-evolution — 架构演进全景。