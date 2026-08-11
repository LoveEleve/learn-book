# stage-2 · 第 20 节：Alibaba Seata 架构和原理（下）— 知识点提取

> 课程：stage-2 模式设计与实现 第 20 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/20. 第二十节：Alibaba Seata 架构和原理（下）.md`
> 提取时间：2026-08-11 | 权重：核心（Seata 架构主线，源码级）

---

## 一、本节概览

- **技术域**：Seata TCC 模式（注解/拦截器/处理器/资源管理器）+ Seata RPC 设计 + 事务协调器
- **维度**：`[分布式问题]`（TCC 分布式事务）+ `[工程问题]`（注解/拦截/资源管理器/RPC，源码级）
- **核心命题**：理解 Seata TCC 模式的实现原理（对比 AT）+ Seata RPC 设计
- **知识点数**：10 个
- **前置**：第 18 节 TCC、第 19 节 Seata 上、Spring AOP

## 前置条件清单
读者需先掌握：
1. **TCC 概念**（第 18 节：Try/Confirm/Cancel）
2. **Seata 上**（第 19 节：TC/TM/RM、BranchType）
3. **Spring AOP 拦截**（第 14/16 节）
4. **RPC 概念**
未达前置者，先补：第 18/19 节 + Spring AOP

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：TCC 注解/拦截器/资源管理器对照 seata 源码讲
- **工程化弱**：TCC Fence/RPC 消息补基础
- **必做**：对照 `code/spring/seata` 完整源码验证（08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Seata TCC 模式（对比 AT）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：第 18/19 节
- **来源**：docs §Seata TCC 模式
- **需求**：理解 Seata TCC 模式实现原理（对比 AT）
- **自主实现**：若我设计——TCC 用自定义 prepare/commit/rollback，不依赖底层数据资源事务
- **参考实现**（docs）：**Seata TCC 模式**——全局事务整体是两阶段提交模型，分支事务满足两阶段；**对比 AT**——AT 一阶段 prepare=本地事务+回滚日志(自动)、二阶段 commit=异步清理/rollback=UNDO 补偿；**TCC** 一阶段 prepare=调用**自定义** prepare 逻辑、二阶段 commit/rollback=调用**自定义** commit/rollback 逻辑；TCC 不依赖底层数据资源的事务支持
- **对比取舍**：**AT 自动 vs TCC 自定义**——AT 自动(无侵入)；TCC 自定义(需写三方法，适合跨资源/非关系库)
- **测试佐证**：seata tcc 模块源码

### KP-02 Seata TCC 注解（TwoPhaseBusinessAction）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §TCC 注解 + 源码验证
- **需求**：用注解标记 TCC 方法并绑定 confirm/cancel
- **自主实现**：若我设计——@TwoPhaseBusinessAction 注解，属性绑定 commit/rollback 方法
- **参考实现**（docs + 源码）：`TwoPhaseBusinessAction` 注解（类似 Hmily @HmilyTCC）——属性——
  - `name()`(49，必须，TCC 名称)
  - `commitMethod()`(56，默认 "commit")/`rollbackMethod()`(63，默认 "rollback")
  - `isDelayReport()`(70，是否延迟上报 TC)
  - `useTCCFence()`(77，是否 TCC Fence——用数据库分布式锁控制互斥)
  - `commitArgsClasses()`(84，默认 BusinessActionContext)/`rollbackArgsClasses()`(91)
- **对比取舍**：**注解绑定两阶段方法**——confirm/cancel 默认名 commit/rollback；useTCCFence 提供互斥
- **测试佐证**：源码 `tcc/.../api/TwoPhaseBusinessAction.java`(49-91)

### KP-03 TccActionInterceptor（TCC 拦截器）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring AOP
- **来源**：docs §TCC 拦截器 + 源码验证
- **需求**：拦截 TCC 方法（try 方法）
- **自主实现**：若我设计——AOP 拦截，非全局事务直接 proceed，TCC 方法绑定 branchType
- **参考实现**（docs + 源码）：`TccActionInterceptor.invoke`——非全局事务(`!RootContext.inGlobalTransaction()`)或禁用直接 `invocation.proceed()`；否则取 `@TwoPhaseBusinessAction` 注解；TCC try 方法——保存 xid、绑定 `BranchType.TCC`(非 TCC 时)、`actionInterceptorHandler.proceed`(处理)、finally 解绑 branchType + 移除 MDC branchId
- **对比取舍**：**拦截 try 方法**——非事务放行，TCC 方法绑定 TCC branchType 后交处理器
- **测试佐证**：docs TccActionInterceptor.invoke 源码 + 源码 `tcc/.../interceptor/`

### KP-04 ActionInterceptorHandler.proceed（TCC 处理核心）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03
- **来源**：docs §拦截处理器 + 源码验证
- **需求**：处理 TCC 拦截（创建上下文/分支/执行/上报）
- **自主实现**：若我设计——getOrCreateActionContext→doTccActionLogStore(申请分支)→执行(带 Fence)→reportContext
- **参考实现**（docs + 源码）：`ActionInterceptorHandler.proceed`(67 行)核心步骤——
  1. `getOrCreateActionContextAndResetToArguments`(创建/获取 BusinessActionContext)
  2. `doTccActionLogStore`(通过 RPC 申请 TCC Branch ID)
  3. 执行目标方法——`useTCCFence` 则 `TCCFenceHandler.prepareFence`(数据库分布式锁)，否则 `targetCallback.execute()`
  4. `BusinessActionContextUtil.reportContext`(上报上下文到 TC)
  5. 重置上下文到上阶段
- **对比取舍**：**五步处理**——上下文→分支申请→执行(Fence 互斥)→上报→重置
- **测试佐证**：源码 `integration-tx-api/.../ActionInterceptorHandler.java`(proceed 67)

### KP-05 BusinessActionContext 初始化（上下文）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §BusinessActionContext 初始化 + 源码验证
- **需求**：初始化 TCC 事务上下文
- **自主实现**：若我设计——设置 xid/actionName/isDelayReport/branchId + 业务/框架上下文
- **参考实现**（docs + 源码）：**初始化**——xid/actionName/isDelayReport/branchId；**doTccActionLogStore**——`fetchActionRequestContext`(取请求上下文)+`initBusinessContext`(PREPARE_METHOD/COMMIT_METHOD/ROLLBACK_METHOD/ACTION_NAME/USE_TCC_FENCE)+`initFrameworkContext`(HOST_NAME 本机 IP)；`getOrCreateActionContextAndResetToArguments`(从参数取或新建，重置 updated)
- **对比取舍**：**上下文承载两阶段信息**——commit/rollback 方法名/参数/Fence 标志传递到二阶段
- **测试佐证**：源码 `ActionInterceptorHandler`(getOrCreateActionContext 176/initBusinessContext/initFrameworkContext)

### KP-06 Seata TCC 服务接口解析器（RemotingParser）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：RPC
- **来源**：docs §RemotingParser + 源码验证
- **需求**：解析 TCC 服务接口（识别远程调用）
- **自主实现**：若我设计——按 RPC 框架解析 TCC 接口
- **参考实现**（docs + 源码）：`RemotingParser` 实现——`LocalTCCRemotingParser`(需 @LocalTCC 注解)/`DubboRemotingParser`/`HSFRemotingParser`/`SofaRpcRemotingParser`
- **对比取舍**：**RPC 框架适配**——本地/Dubbo/HSF/Sofa 各解析器识别 TCC 接口
- **测试佐证**：源码 `tcc/.../remoting/parser/LocalTCCRemotingParser.java`

### KP-07 Seata TCC 资源管理器（TCCResource/TCCResourceManager）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §TCC 资源管理器 + 源码验证
- **需求**：管理 TCC 资源（注册/分支提交/回滚）
- **自主实现**：若我设计——TCCResource(资源模型)+TCCResourceManager(注册/commit/rollback)
- **参考实现**（docs + 源码）：`TCCResource`(资源，含 targetBean/commitMethod/rollbackMethod)；`TCCResourceManager`——`registerResource`(66，缓存 tccResourceCache + 复用父类 AbstractResourceManager，RPC 向 TC 注册)、`branchCommit`(109，从缓存取 TCCResource → 取 commitMethod → `TCCFenceHandler.commitFence`(140，Fence 幂等) 或 `commitMethod.invoke`(145) → 返回 `BranchStatus.PhaseTwo_Committed/CommitFailed_Retryable`)
- **对比取舍**：**二阶段执行**——TCCResourceManager 反射调用 confirm 方法，Fence 提供幂等/防悬挂
- **测试佐证**：源码 `tcc/.../TCCResourceManager.java`(registerResource 66/branchCommit 109/commitFence 140/commitMethod.invoke 145)

### KP-08 branch commit 调用链路（TC→RM）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-07
- **来源**：docs §branch commit 调用链路
- **需求**：理解 TC 到 RM 的分支提交链路
- **自主实现**：若我设计——DefaultResourceManager 按 BranchType 分发到 TCCResourceManager
- **参考实现**（docs）：**调用链路**——`DefaultCoordinator#onRequest`(TC) → `DefaultRMHandler#handle` → `RMHandlerTCC#handle` → `AbstractRMHandler#handle` → `AbstractRMHandler#doBranchCommit` → `DefaultResourceManager#branchCommit` → `TCCResourceManager#branchCommit`；DefaultResourceManager 是 **ClassLoader 级单例**，按 RootContext BranchType 选 RM
- **对比取舍**：**TC→RM 链路**——TC 发起分支提交请求，经 RMHandler 分发到 TCC RM
- **测试佐证**：docs 链路 + 源码 RMHandlerTCC

### KP-09 Seata RPC 设计（消息/处理器/Locker）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：RPC
- **来源**：docs §Seata RPC 设计 + 源码验证
- **需求**：理解 Seata RPC 消息与锁实现
- **自主实现**：若我设计——AbstractMessage 消息体系 + RM 处理器 + Locker
- **参考实现**（docs + 源码）：**消息组件**——`AbstractMessage`(抽象消息基类)/`AbstractIdentifyRequest`(抽象唯一性消息，含身份标识)/`RegisterRMRequest`(注册 RM)/`GlobalLockQueryRequest`(全局锁查询)/`BranchCommitRequest`(分支提交)/`BranchRollbackRequest`(分支回滚)——均派生自 AbstractMessage，统一 Seata RPC 协议；**RM 处理器**——`RMHandlerAT`/`RMHandlerTCC`；**互斥**——AT 用 TC 全局锁(`ATCore#lockQuery`，Locker 实现 DB/Redis/File)；TCC 用 **TCC Fence**(数据库分布式锁)
- **对比取舍**：**AT 全局锁 vs TCC Fence**——AT 用 TC 全局锁(DB/Redis/File)；TCC 用 Fence(数据库锁) 实现互斥，不用 Fence 则需实现方保证
- **测试佐证**：源码 `core/.../protocol/`(AbstractMessage/AbstractIdentifyRequest/RegisterRMRequest/BranchCommitRequest/GlobalLockQueryRequest)

### KP-10 Seata 事务协调器（TC 处理细节）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-09
- **来源**：docs §Seata 事务协调器（仅标题，架构师发散 + 源码）
- **需求**：理解 TC 事务消息处理
- **自主实现**：若我设计——TC 协调全局事务(注册/提交/回滚)，DefaultCoordinator 处理请求
- **参考实现**（docs 仅标题 + 源码）：TC(事务协调器) 处理——`DefaultCoordinator`(onRequest 接收 RM 请求)、全局事务提交/回滚驱动(分支提交/回滚经 KP-08 链路)；TC 维护全局/分支事务状态
- **对比取舍**：**TC 协调核心**——维护状态、驱动两阶段；docs 仅标题，架构师发散 + 源码补全
- **测试佐证**：源码 `server/.../DefaultCoordinator.java` `[待验证]` 详细时序

> **docs 空节标注（穷尽性）**：docs §Seata TCC 用户场景（行为）（5 行）仅标题无正文——TCC 用户场景(发起者/参与者调用) 已由 KP-03~KP-08(拦截器/处理器/资源管理器) 覆盖；docs §Seata 事务协调器（406 行）仅标题——已由 KP-10 架构师发散补全。标注符合 08 §2。

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Seata TCC 模式(对比 AT) | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| TwoPhaseBusinessAction 注解 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| TccActionInterceptor | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| ActionInterceptorHandler.proceed | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| BusinessActionContext 初始化 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| RemotingParser | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| TCCResourceManager | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| branch commit 链路 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| Seata RPC 设计 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 事务协调器(TC) | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/seata`——TwoPhaseBusinessAction/TCCResourceManager/ActionInterceptorHandler/RemotingParser/BranchCommitRequest/RegisterRMRequest/GlobalLockQueryRequest 全部验证
- **关键源码类**（本次实证）：`tcc/.../api/TwoPhaseBusinessAction`(commitMethod 56/rollbackMethod 63/isDelayReport 70/useTCCFence 77)、`integration-tx-api/.../ActionInterceptorHandler.proceed`(67)、`tcc/.../TCCResourceManager`(registerResource 66/branchCommit 109/commitFence 140/commitMethod.invoke 145)、`core/.../protocol/`(BranchCommitRequest/RegisterRMRequest/GlobalLockQueryRequest)
- **关联标注**：microsphere 用 Seata `[待验证]`；衔接第 18 节 TCC、第 19 节 Seata 上

---

## 五、本节小结（三层次视角）

**需求**：理解 Seata TCC 模式实现原理（对比 AT）+ Seata RPC 设计。

**自主实现核心**：若我设计——
1. TCC 自定义 prepare/commit/rollback(对比 AT 自动)
2. @TwoPhaseBusinessAction 注解绑定两阶段方法
3. TccActionInterceptor + ActionInterceptorHandler(上下文→分支→执行→上报)
4. TCCResourceManager(注册/反射 commit/rollback)
5. RPC 消息体系 + AT 全局锁 vs TCC Fence

**参考实现**：seata 源码(`code/spring/seata` 完整验证) + docs。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**Seata TCC 模式与 RPC 设计**"。核心洞察：**TCC 自定义两阶段、注解绑定、拦截器处理、TCCResourceManager 反射执行、AT 全局锁 vs TCC Fence**。

**待验证汇总**：
- microsphere 用 Seata 的具体场景
- TC DefaultCoordinator 详细时序

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 TCC 组件 + 源码片段 + 本地 seata 源码验证；补全聚焦"Seata TCC 与 AT 的对比、RPC 设计价值"。

### 完整认知：Seata TCC/RPC 在真实架构中完整该讲什么

docs 覆盖了 TCC 注解/拦截器/资源管理器/RPC。作为架构师，这个主题完整还该包含：

1. **Seata TCC vs AT 是"自定义 vs 自动"**：AT 无侵入(自动 UNDO)；TCC 自定义三方法(灵活，适合非关系库/跨资源)——按业务选
2. **TCC Fence 解决幂等/防悬挂**：useTCCFence 用数据库分布式锁控制互斥，保证 confirm/cancel 幂等、防悬挂/空回滚——TCC 工程关键
3. **BusinessActionContext 是两阶段桥梁**：上下文承载 commit/rollback 方法名/参数/标志，跨服务传递到二阶段
4. **AT 全局锁 vs TCC Fence**：AT 用 TC 全局锁(DB/Redis/File Locker)；TCC 用 Fence(数据库锁)——两种互斥机制
5. **Seata RPC 消息体系**：AbstractMessage 派生 RegisterRMRequest/BranchCommit/GlobalLockQuery 等——统一网络协议
6. **DefaultResourceManager 单例分发**：ClassLoader 级单例按 BranchType 分发到各 RM——SPI 设计(呼应第 19 节)
7. **与 TCC 通用模式对照**：docs 第 18 节 Hmily 的 Handler/Role 与 Seata 的 Interceptor/ResourceManager 思想一致(第 18 节对照)

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| AT vs TCC | AT 自动无侵入；TCC 自定义灵活(跨资源) |
| TCC Fence 开/关 | Fence 幂等防悬挂(需数据库锁)；关则需实现方保证 |
| AT 全局锁 vs TCC Fence | AT 集中 TC 锁；TCC 本地数据库锁 |
| Seata vs Hmily | Seata 多模式全面；Hmily 柔性专注 |
| 反射调用 commit | 灵活(反射)；性能/类型安全需注意 |

### 常见坑/反模式

1. **TCC 不用 Fence**：confirm/cancel 不幂等、悬挂/空回滚——用 useTCCFence
2. **commitMethod/rollbackMethod 命名错**：默认 "commit"/"rollback"——方法名不匹配失败
3. **忽略 BusinessActionContext 参数**：两阶段方法参数约定(默认 BusinessActionContext)
4. **AT 误用 TCC**：关系库自动场景用 AT；TCC 适合跨资源/非关系库
5. **context 不重置**：上下文残留影响下次——finally 重置

### 生态位置

- **分布式问题维度**：Seata TCC/RPC 是 **Seata 架构下半部分**——承接第 18 节 TCC、第 19 节 Seata 上，Seata 组收官；衔接补充大纲(现代实践)
- **衔接**：TCC(18) → Seata 上(19) → Seata 下(本篇) → RPC 组(第 21-22 节) → 补充大纲(Outbox)
- **与源码提取的关系**：seata tcc/core 模块是核心源码

**架构师视角结论**：本篇不只是背 TCC 注解，而是"**理解 Seata TCC 模式与 AT 的对比实现**"——注解绑定两阶段、拦截器处理、TCCResourceManager 反射执行、AT 全局锁 vs TCC Fence 互斥、RPC 消息体系；这是 Seata 四模式的工程实现，也需结合现代实践(Outbox)选型。
