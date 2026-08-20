# S-5 事务传播 — 6 传播语义与挂起恢复

> 前置: [[S-1-AT两阶段]] (消费面实证) + [[S-4-DataSource代理]] (savepoint 面) | 引出: [[S-9-Spring集成]] | 对照: Spring 事务传播 (7 种)
> 🔴 A | 8 KP | [模式: 枚举语义 + 挂起恢复栈]
> Pass 2 闭环: q1(传播枚举) q2(挂起恢复) q3(嵌套矩阵) q4(上下文 SPI)

**读者处境**: @GlobalTransactional 的 propagation 参数到底是什么? 嵌套调用时事务怎么挂起/恢复? 这篇拆 Propagation 枚举 (定义面) + suspend/resume (机制面) + RootContext (上下文面)。

### 1. 传播枚举 — 6 值语义 + 对照 Spring

场景: 传播策略有哪些?
源码路径:
- **枚举 6 值** (Propagation:57-176): **REQUIRED / REQUIRES_NEW / NOT_SUPPORTED / SUPPORTS / NEVER / MANDATORY** — 每值 Javadoc 伪代码
- **语义**: REQUIRED (有 join 无新建) / REQUIRES_NEW (**挂起+独立新事务**) / NOT_SUPPORTED (**挂起+无事务执行**) / SUPPORTS (有 join 无直跑) / NEVER (**有则抛**) / MANDATORY (**无则抛**)
- ⚠ **对照 Spring 7 种: 无 NESTED** — Spring NESTED 基于 savepoint 内嵌, Seata 无此语义 (S-4 已证 savepoint 仅记录)
- **配置载体** (TransactionInfo:27-84): timeOut / propagation / lockRetryInterval / lockRetryTimes; ⚠ **rollbackOn = Spring RollbackRule 算法移植** (L65-83): rollbackRules + **getDepth 继承深度匹配** (RollbackRule:49-53) + NoRollbackRule 默认 — S-1 "rollbackOn ? rollback : commit" 的规则引擎
关键设计 (q1): **6 传播 = 参与/挂起/拒绝三族**; 无 NESTED (无 savepoint 内嵌语义)。[模式: 枚举语义]

### 2. 挂起恢复 — suspend/resume + 上下文解绑

场景: 挂起怎么实现?
源码路径:
- **suspend** (DefaultGlobalTransaction:207-228): unbind → **clean ? null : SuspendedResourcesHolder(xid)** — clean=true (事务结束) 不返回 holder; 注释 "first get and then unbind" (L213)
- **resume** (L231-240): holder null → return; **RootContext.bind(xid)**
- **RootContext** (L124-176): bind (**空 xid → 转 unbind** L127) + **MDC.put/remove 同步**; unbind → CONTEXT_HOLDER.remove + MDC.remove
- **SuspendedResourcesHolder** (44): 单 xid + null 检查 — 最小挂起载体
- **结束解绑**: commit/rollback finally → suspend(true) (L154-156)
关键设计 (q2): **unbind+bind 线程上下文切换 + MDC 同步**; clean 区分挂起/结束。[模式: 挂起恢复]

### 3. 嵌套决策矩阵 — 6×2 场景穷举

场景: 嵌套调用怎么决策?
源码路径:
- **决策矩阵** (TransactionalTemplate:66-113, S-1 实证): 6×2 = 12 决策点 — REQUIRED: JOIN/CREATE_NEW; REQUIRES_NEW: SUSPEND+NEW/CREATE_NEW; NOT_SUPPORTED: SUSPEND+裸跑/裸跑; SUPPORTS: JOIN/裸跑; NEVER: THROW/裸跑; MANDATORY: JOIN/THROW
- **嵌套场景**:
  - **REQUIRES_NEW 内 REQUIRES_NEW**: 两段挂起 (栈式 holder) — 独立事务链
  - **NOT_SUPPORTED 内 MANDATORY**: 挂起后无事务 → **MANDATORY 抛** — 环境清空效应
  - **NEVER 内 REQUIRED**: 无事务环境 → REQUIRED 新建 — 挂起后重新决策
- **挂起恢复闭环** (L147-152): 外层 finally resume — **每层挂起必恢复** (栈式); ⚠ **挂起不冻结超时计时**: suspend 不改变 createTime — 长挂起可能触发客户端超时 (TransactionalTemplate:162-165)
关键设计 (q3): **6×2 决策矩阵 + 栈式挂起恢复**; 环境清空效应。[模式: 决策矩阵]

### 4. 上下文存储 SPI — ContextCore 双实现

场景: 线程上下文存哪?
源码路径:
- **ContextCore SPI** (ContextCoreLoader:27-41): EnhancedServiceLoader.load — 可插拔
- **双实现**: **ThreadLocalContextCore** (普通) / **FastThreadLocalContextCore** (Netty 事件循环场景)
- **RootContext 键**: KEY_XID / KEY_TIMEOUT / **KEY_GLOBAL_LOCK_FLAG** (@GlobalLock, S-4 交叉) + MDC 同步
- **可替换面**: ⚠ **推断面** — ContextCore 仅 2 内置实现 (ThreadLocal/FastThreadLocal), **spring 无内置响应式实现** (SPI 机制支持自定义)
关键设计 (q4): **上下文存储可插拔**; 传播与存储解耦。[模式: SPI 存储]

## 代码类型
Architecture (传播语义)

## 负面空间 — 传播刻意不做的事

- **不做 NESTED/savepoint 内嵌** — 无 Spring NESTED 语义 (嵌套=独立事务或 join)
- **不做传播超时继承** — 每层独立 timeOut (TransactionInfo)
- **不做事务传播到子线程** — ThreadLocal 默认不继承 (需显式 xid 传递)
- **不做异步传播** — 响应式需换 ContextCore
- **不做传播日志追踪** — 仅 MDC xid 关联
- **不做多 xid 并存** — 单 xid 线程绑定 (挂起换出)

→ 引出: Spring 集成装配 → [[S-9-Spring集成]]
