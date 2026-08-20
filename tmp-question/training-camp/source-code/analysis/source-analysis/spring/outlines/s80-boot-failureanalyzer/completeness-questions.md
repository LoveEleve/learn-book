# S-16 诊断 FailureAnalyzer 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 我想自定义一个启动失败诊断器怎么做? | §3 (extends AbstractFailureAnalyzer + spring.factories 注册) |
| 2 | 为什么我的 analyzer 要 BeanFactory/Environment? | §1 (ArgumentResolver 构造注入) |
| 3 | analyze 返回 null 表示什么? | §1/§2 (不处理, 链继续) |
| 4 | 怎么找真正要分析的异常(被多层包装)? | §2 (findCause 沿 cause 链) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 SPI 而非硬编码? | §1 (开放扩展点, spring.factories) |
| 6 | 为什么首个非 null 接管? | §1 (每个 analyzer 认一类异常, 链式) |
| 7 | 为什么注入型用构造函数而非 setter? | §1 (ArgumentResolver 按构造参数类型注入) |
| 8 | 与 C-13 请求期异常解析的区别? | 边界 (启动期 SPI vs 请求期 HandlerExceptionResolver) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | "APPLICATION FAILED TO START" 横幅哪来的? | §3 (LoggingFailureAnalysisReporter.buildMessage) |
| 10 | 泛型参数怎么变成处理类型? | §2 (ResolvableType 反射) |
| 11 | 简单型和注入型差别? | §3 (是否需要容器上下文) |

## 覆盖: 11 问 / 3 身份 / 100%
