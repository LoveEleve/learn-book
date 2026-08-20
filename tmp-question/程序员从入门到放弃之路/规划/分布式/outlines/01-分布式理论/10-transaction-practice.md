# 分布式事务实战 — XA、Spring、Seata 与 TCC 为什么“配置了却没生效”

> Cluster C: 14 KPs | 依赖: 08-2pc-3pc-tcc、09-saga-reliable-message | 读者基线: Spring/Spring Boot、MySQL 事务、微服务调用
> 读者处境: 理论已经说明 2PC/TCC/Saga/可靠消息的取舍；本篇进入代码边界：为什么 `@Transactional` 不回滚，框架模式如何选择，如何验证协调者和补偿真的工作
> 打开新视角: 分布式事务框架不是“一行注解获得全局原子性”，而是**本地代理/线程上下文、RM/TM 状态、日志/锁和重试恢复**共同组成的运行时系统

---

### 概念依赖链

```
08 2PC/TCC + 09 Saga/可靠消息 → 本篇: XA/Spring/Seata/Hmily/ShardingSphere
  ├─ §1 MySQL XA(语法/prepare/recover)
  ├─ §2 Spring @Transactional(AOP/ThreadLocal/失效边界)
  ├─ §3 Seata(AT/TCC/Saga/XA/TC-RM)
  └─ §4 Hmily/ShardingSphere(框架定位与日志恢复)
先讲: 数据库 XA → 本地 Spring 事务 → 分布式协调框架 → 框架选型/验证
后续依赖: 11-leader-election-broadcast(失败检测/选主/广播)
```

### 叙事顺序

1. 问题引入——代码加了 `@Transactional`，异常发生后数据库为什么还提交？
2. MySQL XA——Prepare/Commit/Recover 的数据库边界
3. Spring 事务——AOP 代理、ThreadLocal 和七类失效
4. Seata——TC/RM 与 AT/TCC/Saga/XA 模式
5. Hmily/ShardingSphere——专注 TCC 与分片 XA
6. 收束——框架不是语义替代品

### 1. MySQL XA — 数据库层的两阶段状态机

场景提示: 两个资源管理器需要一起提交，MySQL XA 语句如何把事务停在 prepared 状态等待协调者？ [写作时展开]

关键设计: XA 把本地事务控制暴露为 start/end/prepare/commit/rollback/recover 接口：

```[pseudocode]
XA START xid
  → 执行 INSERT/UPDATE
XA END xid
  → 结束分支工作
XA PREPARE xid
  → 资源管理器持久化 prepared 状态
XA COMMIT xid / XA ROLLBACK xid
  → 协调者最终决策

协调者恢复:
  XA RECOVER
  → 列出遗留 prepared xid
  → 根据外部事务状态逐一决策
```

Why: 为什么 XA RECOVER 是生产必需而不是调试命令？——**协调者在 Prepare 后宕机时，参与者可能保留 prepared 事务和资源；重启后必须从持久化协调状态恢复决策**。Redo/Undo/binlog 分别承担引擎恢复、逻辑回滚/MVCC 和复制/归档等不同职责，不能把它们混成一份“事务日志”。 [MySQL: XA 语法、prepared 事务和驱动行为需按目标版本核对]

比喻锚点: XA 是多家银行先冻结账户并拿到冻结凭证，中央清算所重启后必须根据凭证决定统一入账还是解冻。 [写作时展开]

### 2. Spring `@Transactional` — AOP 代理与线程上下文的失效边界

场景提示: 方法上加了注解，异常发生后却没有回滚；先问“这个调用是否经过代理”，而不是先怀疑数据库。 [写作时展开]

关键设计: Spring 声明式事务通常由代理拦截，在当前线程绑定事务资源和同步回调：

```[pseudocode]
外部调用 proxy.method()
  → TransactionInterceptor
  → begin/join/suspend transaction
  → TransactionSynchronizationManager(ThreadLocal)
  → target method
  → commit 或 rollback
  → cleanup ThreadLocal/resources

常见失效:
  private/final/不可代理方法或代理配置不匹配
  同类 this.method() 绕过代理
  异常被 catch/转换后不满足回滚规则
  checked exception 未配置 rollbackFor
  底层表/驱动不支持事务
  新线程不自动继承当前事务上下文
  propagation 语义与预期不符
```

Why: 为什么 Spring 本地事务不能自动跨 RPC？——**事务资源绑定在当前调用线程/资源上下文，远端服务有独立连接、事务和故障边界**；`REQUIRES_NEW` 是挂起/新建本地事务，不是跨服务协调。多线程、异步和消息也需要显式事务边界与幂等。 [Spring: 代理、TransactionSynchronizationManager、异常回滚规则和 propagation 依赖调用方式/配置]

比喻锚点: `@Transactional` 像给本地柜台套了一层自动收银流程；走旁边的内部员工通道、另一个柜台或另一座城市，不会自动共享这本账。 [写作时展开]

### 3. Seata — TC、TM、RM 与四种模式的不同代价

场景提示: 微服务要跨多个数据库完成订单/库存操作，Seata 的 TC/RM 如何协调，AT/TCC/Saga/XA 又怎样选择？ [写作时展开]

关键设计: Seata 把全局协调、分支资源和业务调用拆开，不同模式把复杂度放在不同位置：

```[pseudocode]
TC:
  维护 GlobalSession/分支状态/超时恢复

TM:
  开始/提交/回滚全局事务

RM:
  注册分支、锁/日志/提交或回滚本地资源

AT:
  自动记录前后镜像/undo_log
  → 框架尝试自动回滚

TCC:
  Try/Confirm/Cancel 业务实现

Saga:
  状态机/步骤补偿

XA:
  依赖资源管理器的 XA prepare/commit
```

Why: 为什么 Seata AT 不是“零成本零侵入”？——**它需要代理 SQL/记录 undo、维护全局锁和处理数据库/SQL/DDL 限制**；TCC 减少部分锁占用却增加业务代码，Saga 接受中间态，XA 承受 Prepare/阻塞。框架模式名称不能替代对业务补偿和故障恢复的验证。 [分布式事务: AT/TCC/Saga/XA 是不同一致性/开发/性能权衡]

比喻锚点: Seata 像总调度中心：AT 自动保存修改前后凭据，TCC 要各分店手写预留/确认/取消，Saga 管步骤状态机，XA 要各分店提供标准冻结接口。 [写作时展开]

### 4. Hmily 与 ShardingSphere — 框架定位比“谁更快”重要

场景提示: 分库分表事务和微服务业务补偿需要的能力不同，为什么不能只比较框架 benchmark？ [写作时展开]

关键设计: 框架能力边界不同：

```[pseudocode]
Hmily:
  偏 TCC/业务补偿
  → Try/Confirm/Cancel 元数据/恢复日志
  → 定时重试未完成动作

ShardingSphere + XA manager:
  偏分库分表路由 + XA 协调
  → Atomikos/Narayana 等 TM
  → 跨分片资源 prepare/commit

共同检查:
  幂等/重试/超时/恢复/监控/回滚
  → 不能只看注解是否生效
```

Why: 为什么“支持 XA/TCC”不等于框架能自动覆盖所有异常？——**驱动、数据库、代理、线程/异步调用、事务边界和业务副作用仍在框架外**；框架日志和恢复任务也需要持久化、告警和人工对账。具体组件版本、维护状态和部署拓扑必须单独验证。 [工程: 分布式事务框架只是协调层，不能替代业务幂等与故障演练]

### 5. 收束

实战验证闭环：

```[pseudocode]
本地注解/框架配置
  → 确认调用经过代理/协调器
  → 确认分支资源注册
  → 注入异常/超时/进程宕机/重复回调
  → 检查数据库、消息、undo/log、全局状态
  → 验证重试/补偿/恢复与幂等
```

**Aha Moment**: "分布式事务最常见的 bug 不是协议不会背，而是**事务边界没有真正经过代理/协调器，或异常路径没有幂等、补偿和恢复状态**。"
**回答读者三问**: ①`@Transactional` 为什么失效=代理、线程、异常、引擎和 propagation 边界；②Seata 模式怎么选=按锁/业务改造/中间态/资源支持取舍；③框架是否可靠怎么验=故障注入、重复回调、恢复和最终账务校验。

---

### 核心悬念

**"事务框架能协调提交，但系统上线后怎样发现 Leader 挂了、节点失联，怎样广播配置并避免重复选主？"**

→ 引出 11-leader-election-broadcast — 失败检测、Leader 选举、可靠广播与 Gossip。