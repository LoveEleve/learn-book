# Java 分布式架构师 · 完整学习计划

> 24 周、66 课时。每课包含：读什么文档、看什么代码、做什么练习、怎么检验。
> 不是知识清单，是逐日可执行的学习路线。

---

## 目录

[第一阶段：工程地基](#一)
[第二阶段：分布式理论](#二)
[第三阶段：手写中间件](#三)
[第四阶段：事务体系](#四)
[第五阶段：数据架构](#五)
[第六阶段：可观测性](#六)
[第七阶段：容错模式](#七)
[第八阶段：性能实战](#八)
[第九阶段：云原生部署](#九)
[第十阶段：多活架构](#十)
[第十一阶段：平台工程与排障](#十一)

---

<h2 id="一">第一阶段：工程地基（Week 1-2）</h2>

### 为什么从这里开始
工程能力是后面所有学习的地基。看不懂 Maven 多模块，就看不懂 stage-2 的 rpc-project；不会写 REST API，就看不懂 Shopizer 的 Controller 层。

### Week 1 · Day 1（全天 ~4小时）

**课时 01：Maven 多模块工程**

📖 **读**（~2小时）：
1. `stage-1/docs/01. 第一节：基础框架工程构建.md` —— 从头读到尾
2. 重点圈出：POM 继承 vs 聚合、依赖仲裁"最近定义优先"、6 种依赖作用域

🔍 **看代码**（~1小时）：
```bash
cd stage-1/src/biz-project
cat biz-dependencies/pom.xml    # BOM 怎么写
cat pom.xml                      # 根 POM 的 <modules> 和 <parent>
```

✏️ **动手**（~1小时）：
1. `mvn dependency:tree > /tmp/tree.txt` —— 把依赖树导出
2. 在 tree.txt 中找出一个被仲裁替换的依赖（同 groupId:artifactId，不同 version），解释为什么 Maven 选了这个版本
3. 故意在 biz-web/pom.xml 中声明一个比 BOM 更低的版本，再跑 `mvn dependency:tree`，看仲裁结果

✅ **算过关**：新人问你"为什么我的依赖版本不对"→ 你能在 5 分钟内定位是 BOM 冲突、传递依赖、还是仲裁规则导致

---

### Week 1 · Day 2（全天 ~4小时）

**课时 02：REST API 服务端**

📖 **读**（~1.5小时）：
1. `stage-1/docs/03. 第三节：REST API 服务端设计.md` —— 从头读到尾
2. 重点：ApiBase/ApiRequest/ApiResponse 的泛型嵌套结构、DispatcherServlet 完整处理链路

🔍 **看代码**（~1小时）：
```bash
# 找这些文件
biz-api/src/main/java/com/acme/biz/api/ApiRequest.java
biz-api/src/main/java/com/acme/biz/api/ApiResponse.java
biz-api/src/main/java/com/acme/biz/api/enums/StatusCode.java
biz-web/src/main/java/com/acme/biz/web/mvc/exception/ExceptionHandlerConfiguration.java
biz-web/src/main/java/com/acme/biz/web/mvc/method/annotation/ApiResponseHandlerMethodReturnValueHandler.java
```

✏️ **动手**（~1.5小时）：
1. 打开 `ApiRequest.java`，理解泛型 `<T>` 怎么在 response 里收窄类型
2. 打开 `StatusCode.java`，找到 `{status-code.continue}` 这个占位符——它表示国际化
3. 跟着 DispatcherServlet → HandlerMapping → HandlerAdapter → ModelAndView 的调用链走一遍

✅ **算过关**：
- 为什么不用 HTTP 200/400/500 直接返回，还要套一层 `code` 字段？（提示：业务错误码 ≠ HTTP 状态码）
- 如果让你实现请求去重（幂等性），用什么数据结构？放哪里？（提示：Redis + Token）

---

### Week 1 · Day 3（全天 ~4小时）

**课时 03：REST API 客户端**

📖 **读**（~1.5小时）：
`stage-1/docs/04. 第四节：REST API 客户端设计.md` —— 从头读到尾

**RestTemplate 三层扩展架构**（必须手画图）：
```
HttpMessageConverter       ← 序列化层：Java对象 ↔ JSON/XML
ClientHttpRequestFactory   ← 网络层：JDK URL / Apache / OkHttp3
ClientHttpRequestInterceptor ← 拦截器链：装饰器模式
```

🔍 **看代码**（~1小时）：
```bash
biz-api/src/main/java/com/acme/biz/api/interfaces/UserService.java  # @FeignClient
biz-client/src/main/java/com/acme/biz/client/cloud/BizClientApplication.java
biz-web/src/main/java/com/acme/biz/web/client/rest/RestTemplateConfiguration.java
```

✏️ **动手**（~1.5小时）：
1. 找到 RestTemplate 配置类，看 `ClientHttpRequestFactory` 被设成什么
2. 看一下 `@FeignClient` 接口，理解声明式 RPC——你写一个接口，Feign 自动生成 HTTP 调用代码

✅ **算过关**：`InterceptingClientHttpRequestFactory` 怎么用装饰器模式实现拦截器链？（提示：每层包装前一个 Factory，逐层嵌套）

---

### Week 1 · Day 4-5（2天 ~6小时）

**课时 04：Spring 脚手架 + 自动装配**

📖 **读**（~2小时）：
1. `stage-1/docs/23. 第二十三节：Spring 脚手架运用、架构与定制.md`
2. `stage-1/docs/24. 第二十四节：Spring 脚手架原理、实现与扩展.md`
3. segfault-lessons：`spring-boot/lesson-20/` —— `PersonAutoConfiguration.java`

**Spring Boot 自动装配三件套**（看这个类就懂了）：
```java
@ConditionalOnWebApplication                              // 只在 Web 环境生效
@ConditionalOnProperty(prefix="person", name="enabled")   // 配置开关
@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class) // 顺序控制
```

**这就是 microsphere-spring-boot 所有 AutoConfiguration 的原型。** 看懂了这一个类，训练营里所有的 `@Enable*` 注解你都懂了。

✏️ **动手**（~2小时）：
1. 跟着 `ProjectRequest → ProjectDescription → ProjectGenerator.generate()` 的调用链走一遍
2. 试着在你的 IDE 里自己写一个 AutoConfiguration：用 `@ConditionalOnClass` 控制是否加载 Redis 配置

✅ **算过关**：
- 如果你写的 Starter 需要同时支持单机和集群模式，条件注解怎么写？（提示：`@ConditionalOnProperty(prefix="my.service", name="mode", havingValue="cluster")`）
- `spring.factories` 文件和 `@ComponentScan` 的自动发现有什么区别？

---

<h2 id="二">第二阶段：分布式理论（Week 3-4）</h2>

### 为什么从这里开始
不懂 Paxos/Raft，就看不懂 ZK 选举、Nacos 一致性、JRaft 注册中心。这是整个训练营最硬核的理论基础，面试大厂必考。

### Week 3 · Day 1-2

**课时 05：CAP + BASE 理论**

📖 **读**（~1小时）：`stage-2/docs/01. 第一节：CAP 与 BASE 理论.md`

**三个问题刻在脑子里**：
1. C (Consistency)：所有节点同一时刻看到相同数据
2. A (Availability)：每个请求都能获得非错误响应
3. P (Partition Tolerance)：网络分区时系统继续工作

**P 必选 → 只能在 CP 和 AP 之间选**。ZK=CP，Eureka=AP，Nacos=两个都能切。

✏️ **动手**：画一张图——用户请求 → 两个机房（WAN 连接），机房间网络断了。分别画出 CP 系统和 AP 系统的行为。

---

**课时 06：Paxos 算法**

📖 **读**（~2小时）：
1. `stage-2/docs/02. 第二节：分布式共识算法 - Paxos.md`（全文）
2. `stage-2/papers/paxos-simple-Copy.pdf`（读 Phase 1 和 Phase 2 协议描述）

**Paxos 两阶段**：
```
Phase 1 (Prepare/Promise): Proposer → "我提议编号 N"
Phase 2 (Accept/Accepted): Proposer → "我提议值 V"
```

**三个硬约束**：
1. Quorum: N = 2F + 1（容忍 F 个节点故障）
2. Proposal Number 单调递增
3. Phase 2 的值必须沿用被 Promise 过的最新值

✏️ **动手**：
1. 手画 Paxos 的消息流时序图（包括 Proposer、Acceptor1、Acceptor2、Acceptor3）
2. 模拟一个场景：两个 Proposer 同时提议——P1 给 A1/A2 发了 Prepare(1)，P2 给 A2/A3 发了 Prepare(2)——画出完整的消息流

✅ **算过关**：如果两个 Proposer 同时提议会发生什么？怎么解决？（活锁问题 → Multi-Paxos 用 Leader 避免）

---

### Week 3 · Day 3-4

**课时 07：Raft 算法**

📖 **读**（~3小时）：
1. `stage-2/docs/03.` + `04.`
2. `stage-2/papers/raft.pdf`（重点读 Section 5 Leader Election + Figure 2 状态机）

**Raft 三大子问题**：
1. Leader 选举：Follower 超过 election timeout → Candidate → RequestVote → 获得多数票 → Leader
2. 日志复制：Leader 收到请求 → 追加到本地 → 并发 AppendEntries → 多数确认 → commit
3. 安全性：新 Leader 必须拥有所有已提交的日志（Leader Completeness Property）

**Raft vs Paxos 关键区别**：
- Raft 强制单一 Leader（Paxos 可以多 Proposer）
- Raft 日志不空洞（Paxos 允许空洞）
- Raft 用随机选举超时避免脑裂

✏️ **动手**：手画 Raft 状态转换图（Follower ↔ Candidate ↔ Leader），标注每个转换的触发条件

✅ **算过关**：如果网络分区导致出现两个 Leader（旧 Leader 收不到心跳以为自己还是 Leader），会发生什么？（提示：旧 Leader 无法提交，因为拿不到多数派的 AppendEntries 确认）

---

**课时 08：ZAB 协议**

📖 **读**（~2小时）：
1. `stage-2/docs/05. 第四节：原子广播算法 - ZAB.md`
2. `stage-2/papers/zab.pdf`

**ZAB 三阶段**：Discovery → Synchronization → Broadcast
**ZXID**：高 32 位 epoch + 低 32 位 counter
**与 Paxos 区别**：ZAB 支持多未完成事务、高效崩溃恢复

---

### Week 4 · Day 1-2

**课时 09：SOFAJRaft 源码走读**

📖 **读**（~2小时）：`stage-2/docs/06.` + `07.`（全文，含 NodeImpl.handleAppendEntriesRequest 完整源码）

🔍 **看代码**（~2小时）：
```bash
cd stage-2/src/middleware-projects && mvn compile -pl rpc-project
```
找 `ServiceDiscoveryServer.java`、`ServiceDiscoveryStateMachine.java`、`RegistrationRpcProcessor.java`

**核心组件**：
```
Node → LogStorage(RocksDB) / StateMachine(onApply) / 
        Replicator / FSMCaller / BallotBox / AppendBatcher
```

✅ **算过关**：onApply 什么时候被调用？收到的日志一定是已提交的吗？（不一定——Leader 上的日志不一定已提交，需要通过 commitIndex 确认）

---

**课时 10：Nacos 2.x 双协议 + ZK 选举**

📖 **读**（~2小时）：`stage-2/docs/08.` + `09.` + `10.` + `11.` + `12.` + `13.`

**Nacos Raft(CP) vs Distro(AP)**：
- Raft：配置管理（配置错了整个系统可能出问题，必须强一致）
- Distro：临时实例（实例挂了的消息可以延迟，但不能因为 Leader 宕机导致全部不可发现）

**ZK FastLeaderElection.lookForLeader() 完整流程**：
1. 递增 logicalclock + updateProposal()
2. sendNotifications() 广播选票
3. 主循环 → totalOrderPredicate: epoch > zxid > sid 三层比较
4. 多数票 → LEADING

**补充：EventDispatcher 模式**（microsphere-java，纯 Java SPI 事件框架）：
- DirectEventDispatcher：同步执行（`DIRECT_EXECUTOR = Runnable::run`）
- ParallelEventDispatcher：默认 ForkJoinPool 并行执行
- EventListener 通过反射推断泛型类型 `<E extends Event>`

对比 segfault-lessons Spring Cloud lesson-1 的 `ApplicationEvent` —— 一个是通用 SPI 框架，一个是 Spring 内置机制。理解两者是理解事件驱动架构的关键。

---

## 第三阶段：手写中间件（Week 5-7）

### 为什么从这里开始
市面 90% 的"微服务教程"只教你调 Spring Cloud API。亲手写一个 RPC 框架后，Spring Cloud 在你眼里就是透明壳。

### ⭐ rpc-project（训练营最强代码，10/10）

`stage-2/src/middleware-projects/rpc-project/` —— 52 个 Java 源文件，Netty + SOFAJRaft + Protobuf 完整实现。

### Week 5 · Day 1-2

**课时 11：RPC 调用链路全景**

📖 **读**（~2小时）：`stage-2/docs/21.` + `22.`

🔍 **完整调用链路**（跟着走一遍）：
```
ServiceConsumer.echo("Hello")
  → ServiceInvocationHandler.invoke()   // JDK 动态代理
    → createRequest(serviceName, methodName, parameters)
    → selectServiceProviderInstance()   // 负载均衡
    → rpcClient.connect(instance)        // Netty Channel
    → channel.writeAndFlush(request)     // MessageEncoder 编码 [4字节长度][数据]
    → ExchangeFuture.create()            // Promise 模式
    → exchangeFuture.get()               // 阻塞等待...

=== 网络传输 ===

Server: MessageDecoder → InvocationRequestHandler.channelRead0()
  → serviceContext.getService(serviceName)
  → MethodUtils.invokeMethod(反射调用)
  → 返回 InvocationResponse

Client: MessageDecoder → InvocationResponseHandler.channelRead0()
  → ExchangeFuture.promise.setSuccess(result) → 唤醒阻塞
```

✏️ **动手**（~2小时）：
```bash
cd stage-2/src/middleware-projects && mvn compile -pl rpc-project
```
1. 打开 `ServiceInvocationHandler.java`，看 `invoke()` → `execute()` 的完整链路
2. 打开 `InvocationRequest.java`，理解 RPC 协议——它包含什么字段？为什么不直接用 HTTP+JSON？
3. 打开 `ExchangeFuture.java`，理解 Promise 模式——异步网络回调怎么变成同步阻塞？

---

### Week 5 · Day 3-4

**课时 12：Netty 通讯 + JRaft 注册中心**

📖 **读**（~2小时）：`stage-2/docs/22.`（Netty 部分）+ `stage-2/docs/23.`（JRaft 注册中心）

🔍 **看代码**（~2小时）：
1. `MessageEncoder.java` + `MessageDecoder.java` —— 协议格式：`[4字节长度][序列化后的对象]`。半包处理怎么做的？
2. `ServiceDiscoveryServer.java` —— 启动 Raft 节点 → 注册 3 个 Processor → 启动心跳检查线程（每5s）
3. `ServiceDiscoveryStateMachine.java` —— onApply 怎么处理 REGISTRATION / DEREGISTRATION / GET / BEAT 四种操作？

**心跳机制**：客户端每 5s HeartBeat（不写 Raft 日志，直接在 Leader 内存处理），30s 无心跳自动摘除

**三种服务发现实现**：FileSystem（单机测试） / Zookeeper（骨架未实现） / JRaft（完整可用）

**已知 7 个 Bug**（给你练手用的）：
1. RoundRobin 取模 Bug：`(counter.getAndIncrement()-1) % size`，首次 index=-1
2. Channel 无复用：每次 invoke 新建连接
3. ExchangeFuture EventLoop 不明确
4. ServiceInstanceBeatThread while(true) 无退出机制
5. Protobuf 生成类提交到了 src
6. hostname 用了 logback 的 ContextUtil
7. RpcClient 无 connection close

---

### Week 6-7：分布式缓存 + 配置中心

**课时 13-15**：
- `distributed-cache-project/`：MyQueuedSynchronizer（自定义 AQS）+ DistributedSessionFilter + RedisDistributedHttpSession
- Redisson RLock 可重入锁——用 Redis HASH 实现：key=lock_name, field=thread_id, value=持有次数
- `distributed-config-project/` ⚠️ 仅 POM 骨架。设计思想参考 microsphere-configuration 的四源统一抽象（Apollo/etcd/Nacos/ZK）

---

## 第四阶段：事务体系（Week 8-9）

### Week 8 · Day 1-2

**课时 16：本地事务 → 两阶段提交**

📖 **读**（~3小时）：
1. `stage-2/docs/14.` —— ACID / 隔离级别 / JDBC 底层
2. `stage-2/docs/15.` —— JTA/XA 两阶段提交
3. **必读论文**：`stage-2/papers/Life beyond Distributed Transactions.pdf`（前 5 页）

**JDBC 隔离级别在 MySQL 中的实现**：
| 级别 | MySQL InnoDB 实现 |
|------|------------------|
| READ UNCOMMITTED | 不做任何锁 |
| READ COMMITTED | 每次 SELECT 读最新快照 |
| REPEATABLE READ | 事务内第一次 SELECT 建快照（MySQL 默认）|
| SERIALIZABLE | SELECT 加共享锁 |

**XA 两阶段提交的问题**（这就是为什么要找替代方案）：
1. 锁时间长：Prepare 到 Commit 之间所有资源被锁
2. TM 单点：TM 挂了，参与者不知道该 commit 还是 rollback
3. 网络分区：收不到第二阶段指令 → 悬挂事务

---

### Week 8 · Day 3-4

**课时 17：可靠事件队列 + TCC**

📖 **读**（~2小时）：`stage-2/docs/17.` + `18.` + `19.`

**可靠事件队列核心思路**：
```
本地事务 { 业务操作 + 写入消息表 } → 提交 → 异步发 Kafka → 消费者处理
```
核心：**先保证本地事务的原子性，再靠消息队列异步保证最终一致**。

✏️ **动手**（~2小时）：
1. 用文档里的 SQL 建 users 表和 transactions 表
2. 模拟一次"扣款+下单"——先写 users 表（扣款）和 tx_messages 表（插入"下单"事件），提交本地事务，然后异步发 Kafka

✅ **算过关**：如果消费者处理失败了怎么办？消息丢了怎么办？

---

### Week 9 · Day 1-4

**课时 18-20：Seata AT + TCC 源码**（⭐ 重点，值得 4 天）

📖 **读**（~4小时）：`stage-2/docs/20.` + `21.`（全文，含完整源码分析）

**Seata AT 模式核心——ConnectionProxy.commit()**：
```
1. lockRetryPolicy 重试获取全局锁
2. processGlobalTransactionCommit → register 到 TC
3. UndoLogManager.flushUndoLogs(beforeImage, afterImage)
4. targetConnection.commit（本地提交）
5. report 上报到 TC
// 二阶段：成功→异步删 undo_log / 失败→undo_log 反向补偿
```

**TccActionInterceptor 200+ 行拦截链路**：
```
1. RootContext.inGlobalTransaction() 检查
2. @TwoPhaseBusinessAction 注解提取
3. RootContext.bindBranchType(BranchType.TCC)
4. doTccActionLogStore() → 向 TC 申请 Branch ID + 记录方法元数据
5. 执行目标方法（TCCFenceHandler 分布式锁 / 直接执行）
6. BusinessActionContextUtil.reportContext() 上报业务上下文
```

🔍 **看代码**：
```bash
cd stage-2/src/middleware-projects && mvn compile -pl distributed-transaction-project
```
打开 `ConnectionProxy.java` → `commit()` → `processGlobalTransactionCommit()`

✅ **算过关**：
- Seata AT 的 undolog 和 MySQL 的 undolog 有什么区别？（AT 是业务层反向补偿，MySQL 是物理回滚）
- AT 模式的"脏写"问题是什么？全局锁怎么解决？（事务 A 修改后未提交，事务 B 修改同一行 → 全局锁在事务 A commit 前阻止事务 B 获取锁）

---

## 后续阶段速查

**第五阶段（Week 10-11）**详细版见 `05-数据架构/README.md`
**第六阶段（Week 12-13）**详细版见 `06-可观测性/README.md`
**第七阶段（Week 14）**详细版见 `07-容错模式/README.md`
**第八阶段（Week 15-17）**详细版见 `08-性能实战/README.md`
**第九阶段（Week 18-20）**详细版见 `09-云原生部署/README.md`
**第十阶段（Week 21-23）**详细版见 `10-多活架构/README.md`
**第十一阶段（Week 24）**详细版见 `11-平台工程/README.md`

**Java 基础补充**：`JAVA-DEEP.md` —— 「一入 Java 深似海」8 期学习路线
**segfault-lessons 全部资源**：`SEGFAULT-LESSONS.md`
**训练营缺失补充**：`APPENDIX/GAP-FILL.md`
**过时→现代对照**：`APPENDIX/MODERN-EQUIVALENTS.md`
