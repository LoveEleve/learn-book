# H-12 代理生成 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 拿到的连接是代理吗?怎么来的? | §3 (ProxyFactory.getProxyConnection) |
| 2 | 为什么不用 JDK 动态代理? | §2 (Javassist 性能/方法级覆盖) |
| 3 | 代理内部怎么转发? | §1 (delegate 委托) |
| 4 | 具体代理类哪来的? | §2 (generateProxyClass 生成) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么字节码生成? | §2 (精确方法体 + 无反射开销) |
| 6 | 为什么 ProxyFactory 方法体注入? | §3 (池只调工厂, 生成透明) |
| 7 | 与 spring-aop CGLIB 区别? | §3 (切面 vs JDBC 包装) |
| 8 | 代理族为什么 6 个? | §1 (Connection/Statement/...) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 代理内部持有啥? | §1 (delegate+poolEntry+leakTask) |
| 10 | JavassistProxyFactory 做什么? | §2 (生成具体代理类) |
| 11 | 代理怎么被创建? | §3 (H-3 createProxyConnection→工厂) |

## 覆盖: 11 问 / 3 身份 / 100%
