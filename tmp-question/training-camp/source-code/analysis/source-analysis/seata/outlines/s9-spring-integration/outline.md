# S-9 Spring 集成 — 注解装配与拦截链

> 前置: [[S-1-AT两阶段]] (模板消费) + [[S-4-DataSource代理]] (并行代理) + [[S-5-事务传播]] (规则源) | 对照: Spring @Transactional (阶段3)
> 🟡 B | 8 KP | [模式: AOP 扫描 + 双 handler 拦截]
> Pass 2 闭环: q1(注解) q2(扫描) q3(拦截) q4(自动配置)

**读者处境**: 加个 @GlobalTransactional 注解, 方法怎么就被包进全局事务了? 这篇拆 GlobalTransactionScanner (676) + GlobalTransactionalInterceptorHandler + SeataAutoConfiguration。

### 1. 注解解析 — @GlobalTransactional 全参数

场景: 注解怎么转参数?
源码路径:
- **注解参数** (GlobalTransactional → AspectTransactional 9 字段): **timeoutMills** (默认 **60000ms**) / rollbackFor + rollbackForClassName / noRollbackFor + noRollbackForClassName (**Class/String 双形式**) / **propagation 默认 REQUIRED** / lockRetryInterval(0)/**lockRetryTimes(-1!)**/lockStrategyMode (S-12 交叉)
- **参数桥** (GlobalTransactionalInterceptorHandler:207-244): ⚠ **超时重置语义**: <= 0 **|| == 60000** → 用 defaultGlobalTransactionTimeout (L208-211) — 未配置跟随全局默认
- **rollbackRules 构建** (L216-241): rollbackFor → **RollbackRule** / noRollbackFor → **NoRollbackRule** — **S-5 rollbackOn 规则引擎的注解源闭环**
- **name()**: 注解名空 → formatMethod (方法签名)
关键设计 (q1): **8+ 注解参数 → TransactionInfo 桥**; 超时重置语义。[模式: 注解解析]

### 2. 扫描装配 — AbstractAutoProxyCreator + 双集合

场景: 哪些 bean 被增强?
源码路径:
- **继承** (GlobalTransactionScanner:87-88): **AbstractAutoProxyCreator** (Spring AOP) + 监听器/生命周期接口; AT/MT 双模式 DEFAULT=3 (L92-99)
- **wrapIfNecessary** (L307-339): doCheckers (**FactoryBean 排除**) → **NEED_ENHANCE 集合** → **DefaultInterfaceParser 责任链** (SPI parser 族 + 同类型重复 Runtime + order 链式串联, L73-97) → AdapterSpringSeataInterceptor → **未代理: super.wrapIfNecessary / 已代理: addAdvisor 按序织入** (L334-337); ⚠ **SeataInterceptorPosition (Any/After/Before) 相对 Spring 事务自动重排 Order** (L447-470); TCC 织入面 (L300-305)
- **双集合**: PROXYED_SET (去重) + NEED_ENHANCE_BEAN_NAME_SET (目标)
- **ScannerChecker 族 3 个**: ConfigBeans / Package / Scope — 可插拔检查
- **预收集**: findBusinessBeanNamesNeededEnhancement (BeanDefinition 扫描, L541-560)
关键设计 (q2): **AOP 基础设施 + 检查器族 + 已代理按序织入**。[模式: 扫描装配]

### 3. 拦截链 — InterceptorHandler 双 handler

场景: 方法调用怎么拦截?
源码路径:
- **双 handler** (Scanner 注释 L291-297): **handleGlobalTransaction // TM handler** + **handleGlobalLock // GlobalLock handler** (S-12)
- **TM 路径**: AdapterInvocationWrapper + **transactionalTemplate.execute** (L78,195-207) — S-1 消费
- **ExecutionException 处理** (L245-260+): **Participant → 抛原异常**; RollbackDone + **isTimeoutException → 抛 cause** (超时回滚与业务异常区分, L248-253); BeginFailure/CommitFailure/RollbackFailure → **failureHandler.onBeginFailure/onCommitFailure/onRollbackFailure** + throw
- **FailureHandler**: 注入或 **FailureHandlerHolder 兜底** (L121-122)
关键设计 (q3): **双 handler 分发 + 状态化异常处理 + FailureHandler 钩子**。[模式: 拦截链]

### 4. 自动配置 — SeataAutoConfiguration

场景: Boot 怎么自动装配?
源码路径:
- **SeataAutoConfiguration**: **failureHandler()** + **globalTransactionScanner()** 两 bean (L52-59); ⚠ **initClient 双客户端**: TMClient.init + RmClient.init + **DEFAULT_TX_GROUP_OLD 1.5 变更警告** (L236-260)
- **disableGlobalTransaction**: 配置关闭 → 不 initClient (监听动态生效) (L526-533)
- **initClient()** (L535): TM/RM 客户端初始化
- **数据源代理** (S-4): DataSourceAutoProxyCreator 并行
关键设计 (q4): **两 bean 自动装配 + disable 动态开关**。[模式: 自动配置]

## 代码类型
Architecture (Spring 装配面)

## 负面空间 — Spring 集成刻意不做的事

- **不做注解织入拦截器类**: 只拦截业务 bean — 扫描器自身/工厂不增强 (FactoryBean 排除)
- **不做事务方法级重试**: 失败交 FailureHandler — 无 Spring Retry 集成
- **不做表达式 pointcut**: AbstractAutoProxyCreator 全 bean 扫描 + 检查器 — 非 AspectJ 表达式
- **不做事务名自动生成**: name() 方法签名兜底 — 无业务语义命名
- **不做 @Transactional 冲突处理**: 与 Spring 事务注解共存需手动排序 (ORDER_NUM=1024)
- **不做多数据源路由**: 数据源代理全量 (S-4)

→ 引出: SQL 路由细化 → [[S-10-SQL路由]]
