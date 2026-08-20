# C-13 异常处理全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | Controller 抛异常后谁处理？处理链怎么走？ | §1 (processHandlerException → resolver 链) |
| 2 | @ExceptionHandler 方法怎么被找到？ | §2 (ExceptionHandlerMethodResolver 按异常类型匹配) |
| 3 | @ControllerAdvice 全局异常为什么有效？ | §2 (advice 缓存 + findAnnotatedBeans) |
| 4 | 没写 @ExceptionHandler 的 404/400 状态码哪来的？ | §3 (DefaultHandlerExceptionResolver) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 resolver 用责任链 + 排序？ | §1 关键设计 (具体优先) |
| 6 | 为什么用"异常类型→方法"映射缓存而非遍历？ | §2 关键设计 (性能 + Cause 链) |
| 7 | 三种 resolver 的分工谱系？ | §3 (注解驱动/注解状态/标准兜底) |
| 8 | Default 为什么只设状态码不返回视图？ | §3 (标准异常语义明确) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | resolver 返回 null 表示什么？ | §1 (不处理, 交给下一个) |
| 10 | 处理父类异常的方法能接子类异常吗？ | §2 (类型匹配含父类) |

## 覆盖: 10 问 / 3 身份 / 100%
