# D-1 Spring 运行时诊断 — 循环依赖、事务失效、AOP 代理、条件注解、启动慢的诊断方法

> 依赖全部主干层 | 🟡 Working | 3 KP | [模式: 实践分析]

**读者处境**: Bean 创建失败了，堆栈看不懂；事务没生效，不知道为什么；`@Conditional` 不满足，不知道哪个条件没过——这些怎么诊断？

### 1. 循环依赖诊断：`BeanCurrentlyInCreationException` 堆栈解读

场景: `BeanCurrentlyInCreationException: Error creating bean with name 'a': Requested bean is currently in creation: Is there an unresolvable circular reference?`

诊断路径:
- 堆栈里的 `doGetBean` → `getSingleton` → `beforeSingletonCreation` → 抛异常
- 看堆栈里的 Bean 创建顺序：A 正在创建 → B 需要 A → A 还没创建完
- 三级缓存是否能解决：看是构造器注入还是字段注入

关键诊断点:
- 构造器注入的循环依赖无法解决
- `@Async` + 循环依赖可能需要 `@Lazy` 打破
- 看堆栈中 `getBean` 的调用顺序，找到互相依赖的两个 Bean

### 2. 事务失效诊断

场景: `@Transactional` 加了，但数据没有回滚。

诊断方法:
- 开启事务日志：`logging.level.org.springframework.transaction=DEBUG`
- 检查是否自调用：`this.method()` 绕过代理
- 检查异常类型：checked 异常默认不回滚
- 检查传播行为：`NESTED` / `REQUIRES_NEW` 是否符合预期
- 检查 `@Transactional` 是否在 public 方法上

关键诊断点:
- 代理是否生效：看返回的对象是否是代理（`AopUtils.isAopProxy(obj)`）
- 事务是否开启：看日志里的 `Creating new transaction` / `Participating in existing transaction`
- 回滚是否触发：看日志里的 `Initiating transaction rollback`

### 3. AOP 代理诊断

场景: `@Transactional` / `@Async` / `@Cacheable` 不生效。

诊断方法:
- 检查对象是否被代理：`obj.getClass().getName()` 是否包含 `$$EnhancerBySpringCGLIB` 或 `$Proxy`
- 检查自调用：同类内部调用是否经过代理
- 检查 final/private 方法：CGLIB 无法代理
- 检查 `@EnableTransactionManagement` / `@EnableAsync` 是否生效

### 4. 条件注解诊断

场景: `@ConditionalOnBean` / `@ConditionalOnClass` 不满足，Bean 没注册。

诊断方法:
- 开启条件评估日志：`logging.level.org.springframework.boot.autoconfigure.condition=DEBUG`
- 使用 `ConditionEvaluationReport`：`/actuator/conditions`（Boot）
- 检查 `@ConditionalOnClass` 的类是否在 classpath 上
- 检查 `@ConditionalOnBean` 的 Bean 是否在当前阶段已注册

### 5. 启动慢诊断

场景: 应用启动耗时过长。

诊断方法:
- `spring.main.lazy-initialization=true` 试试是否能缩短启动时间
- 开启 Bean 创建耗时日志
- 检查是否有重量级初始化逻辑在 `@PostConstruct` 中
- 检查是否有 `SmartInitializingSingleton` 做了耗时操作

→ 引出 D-2: Spring 性能调优（启动加速、运行时优化、内存优化）。
