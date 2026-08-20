# S2-7 Bean 作用域 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | singleton 为什么不通过 Scope 接口而用独立的 singletonObjects 三级缓存？ | §1 |
| 2 | prototype scope 的 Bean 销毁时 Spring 会调用 @PreDestroy 吗？ | §1 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | 为什么 request/session scope 需要 RequestContextHolder 的 ThreadLocal 而不能用简单 Map？ | §2 |
| 4 | Scoped Proxy 是 CGLIB 代理还是 JDK 代理？什么情况下选哪一种？ | §3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | @Scope("prototype") 和每次都 new 有什么区别？prototype bean 会被 Spring 管理吗？ | §1 |

## 覆盖: 5 问 / 3 身份 / 100%
