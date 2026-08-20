# S2-2 @Configuration 解析 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | ConfigurationClassPostProcessor 什么时候被调用？它在 PriorityOrdered 中的优先级为什么是 LOWEST_PRECEDENCE？ | 篇1 §1 |
| 2 | CCPP 的 do-while 循环为什么不能只执行一次？什么情况下需要多轮？ | 篇1 §1 |
| 3 | doProcessConfigurationClass 的 8 步注解流水线顺序能不能调整？为什么 @ComponentScan 必须在 @Bean 之前？ | 篇1 §4 |
| 4 | static @Bean 方法和 instance @Bean 方法在 BeanDefinitionReader 中的处理方式有什么根本不同？ | 篇3 §4 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | ImportSelector 和 DeferredImportSelector 为什么要分开处理？Spring Boot 的 AutoConfigurationImportSelector 是后者——如果把它做成前者会有什么问题？ | 篇2 §2-3 |
| 6 | ImportStack 为什么同时是 DFS 栈和注册表？两种职责合在一起会不会违反单一职责原则？ | 篇2 §4 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 7 | proxyBeanMethods=false 和 true 有什么区别？为什么 default 是 true？ | 篇3 §1-2 |
| 8 | CGLIB 是怎么拦截 @Bean 方法调用的？ThreadLocal 是什么？ | 篇3 §2 |

## 覆盖: 8 问 / 3 身份 / 100%
