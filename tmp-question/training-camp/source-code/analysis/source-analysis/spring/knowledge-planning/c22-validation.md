# C-22 Bean Validation — 方法参数校验 (@Validated → MethodValidationPostProcessor → Interceptor)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | MethodValidationPostProcessor(151行)+MethodValidationInterceptor(305行)+LocalValidatorFactoryBean(470行)+SpringValidatorAdapter(500行)+SpringConstraintValidatorFactory(64行)
> 基线: C-14 @InitBinder 提到 @Valid 校验、C-13 MethodArgumentNotValidException — 本域展开方法级校验的 AOP 机制 (替代计划 2-A 过薄覆盖); 原始执行计划 2-A 深化

---

## §0.8

- 🟡 Working，1篇 — 激活(注册 MethodValidationPostProcessor bean, Boot ValidationAutoConfiguration 自动) → AOP 织入(AbstractBeanFactoryAwareAdvisingPostProcessor: 对 @Validated 类建 PointcutAdvisor) → 拦截(MethodValidationInterceptor.invoke: determineValidationGroups→invokeValidatorForArguments/ForReturnValue) → 校验器(LocalValidatorFactoryBean: 包装 JSR-303, SpringConstraintValidatorFactory 让约束校验器可注入依赖) → 与 Web 校验衔接(@RequestBody @Valid → C-14 binder)
- 设计模式: [模式: AOP 环绕通知]—MethodValidationInterceptor; [模式: BPP 织入]—MethodValidationPostProcessor; [模式: 适配]—SpringValidatorAdapter 包 JSR-303

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MethodValidationPostProcessor.java:69,136 | 织入 | **后处理器**: extends AbstractBeanFactoryAwareAdvisingPostProcessor — L136 建 DefaultPointcutAdvisor(pointcut, MethodValidationInterceptor) — 对匹配 @Validated 的类织入; 激活=注册该 bean (Boot ValidationAutoConfiguration 自动) | High |
| MethodValidationPostProcessor.java:107 | 注入 | **setValidator**: 指定 Validator(LocalValidatorFactoryBean) — 默认容器找 Validator bean | High |
| MethodValidationInterceptor.java:78,143 | 拦截 | **invoke L143**: 环绕 — determineValidationGroups(L152)→invokeValidatorForArguments(参数,L166)→proceed(真实方法,L172)→invokeValidatorForReturnValue(返回值,L178) → 违反抛 ConstraintViolationException(L168/L180) | High |
| MethodValidationInterceptor.java:226 | 分组 | **determineValidationGroups**: 读 @Validated value(分组) — 默认 Default 组 | High |
| LocalValidatorFactoryBean.java:83,256 | 校验器 | **ValidatorFactory**: afterPropertiesSet L256 buildValidatorFactory — 提供 JSR-303 Validator; 继承 SpringValidatorAdapter 适配 | High |
| SpringConstraintValidatorFactory.java:39 | 约束工厂 | **注入约束校验器**: ConstraintValidator 由 BeanFactory 创建 — 约束校验器可 @Autowired 依赖(不是裸 new) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 后处理器+拦截器+工厂约 1500 行 — 知识主线: "@Validated 类 → BPP 织入 AOP → Interceptor 校验参数/返回值 → Validator(可注入)". 1篇 (~46行) 按"织入→拦截→校验器→Web 衔接"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | MethodValidationPostProcessor AOP 织入 (@Validated → Advisor) | 🔴 | **为什么🔴**: 方法校验如何生效 — BPP+AOP 的组合模式(与 @Transactional 同构) |
| P1-2 | MethodValidationInterceptor (invoke: 分组→参数校验→返回值校验→异常) | 🔴 | **为什么🔴**: 校验的执行时机与范围 — 参数/返回值双校验、分组语义 |
| P1-3 | LocalValidatorFactoryBean + SpringConstraintValidatorFactory | 🔴 | **为什么🔴**: 校验器怎么构建、约束校验器怎么注入依赖 — Spring 与 JSR-303 的桥 |
| P2-1 | determineValidationGroups (@Validated 分组) | 🟡 | **为什么🟡**: 分组校验语义 — 不同场景不同约束 |
| P2-2 | @Valid vs @Validated 区别 (Web 参数绑定 vs AOP 方法) | 🟡 | **为什么🟡**: 两个注解的分工 — C-14 binder 与 C-22 AOP |
| P3-1 | SpringValidatorAdapter (约束→Spring Errors 适配) | 🟢 | **为什么🟢**: 与 BindingResult/Errors 体系衔接 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **AOP 织入** (MethodValidationPostProcessor) | 🔴 | 校验如何生效 |
| B | **拦截执行** (MethodValidationInterceptor) | 🔴 | 校验时机/范围/分组 |
| C | **校验器与适配** (LocalValidatorFactoryBean + 约束工厂) | 🟡 | 构建与注入 |

> **Cluster A (§1)**: MethodValidationPostProcessor 激活与织入(织入+setValidator)
> **Cluster B (§2)**: MethodValidationInterceptor.invoke(分组/参数/返回值/异常)
> **Cluster C (§3)**: LocalValidatorFactoryBean + SpringConstraintValidatorFactory + 与 Web @Valid(C-14) 衔接

→ 引出 Stage 7: Spring Boot — ValidationAutoConfiguration(B-23) 自动装配校验器, 开启 Boot 自动装配核心域

(End of file - total 61 lines)
