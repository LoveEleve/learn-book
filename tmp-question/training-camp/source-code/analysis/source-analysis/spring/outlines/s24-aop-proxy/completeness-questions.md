# A-1 代理机制 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Service 有接口和无接口 — AOP 代理分别是 JDK 还是 CGLIB？ | §1 |
| 2 | AopContext.currentProxy() 怎么让 this 调用也走代理？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | DefaultAopProxyFactory 的五条件决策为什么先查 proxyTargetClass 再查接口？ | §1 |
| 4 | JdkDynamicAopProxy.invoke 和 CglibAopProxy.intercept 核心逻辑共享在哪？ | §2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | JDK 动态代理和 CGLIB 代理的本质区别是什么？为什么 Java 标准库不能代理类？ | §1 |

## 覆盖: 5 问 / 3 身份 / 100%
