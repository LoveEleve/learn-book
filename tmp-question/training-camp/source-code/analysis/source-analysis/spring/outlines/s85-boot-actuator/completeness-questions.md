# S-21 Actuator 端点 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么写一个自定义端点? | §1/§2 (@Endpoint + @ReadOperation) |
| 2 | /actuator/health 怎么实现的? | §3 (HealthEndpoint + HealthIndicator) |
| 3 | 端点路径怎么定的? | §3 (base-path + id) |
| 4 | 怎么控制端点访问级别? | §1 (defaultAccess) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么注解驱动而非接口? | §1 (解耦声明与实现) |
| 6 | 为什么发现器 + 自动装配注册? | §2 (端点开放, 复用 S-2) |
| 7 | 为什么复用 RequestMappingInfoHandlerMapping? | §3 (MVC 内核/拦截器/异常全生效) |
| 8 | 发现结果怎么传给下游? | §2 (WebEndpointsSupplier) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 端点抽象长什么样? | §1 (ExposableEndpoint.getOperations) |
| 10 | 三种操作注解对应什么? | §1 (Read/Write/Delete = GET/POST/DELETE) |
| 11 | HealthIndicator 怎么聚合? | §3 (HealthEndpoint 遍历 contributors) |

## 覆盖: 11 问 / 3 身份 / 100%
