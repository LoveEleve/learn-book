# A-2 Advice 链 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | proceed() 递归比循环好在哪？为什么不用 for 循环？ | §1 |
| 2 | @Cacheable 命中后不调 proceed — 后续 advice 会执行吗？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | ReflectiveMethodInvocation 的 currentInterceptorIndex 为什么是 int 递增而非 Iterator？ | §1 |
| 4 | AfterReturningAdvice 和 ThrowsAdvice 是互斥的吗？ | §2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | Around advice 和其他 advice 有什么区别？为什么 @Transactional 是 Around？ | §2 |

## 覆盖: 5 问 / 3 身份 / 100%
