# T-7 Spring Boot 集成 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `getWebServer()` 一行代码创建了哪些组件？ | §1.1 |
| 2 | `server.tomcat.threads.max=200` 怎么到达 `AbstractEndpoint.setMaxThreads()`？ | §2.1-2.2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | Customizer 为什么设计成三层(properties/Customizer/subclass)？ | §2.1 |
| 4 | `TomcatStarter` 如何绕过 SCI 的 `@HandlesTypes` 扫描？ | §1.2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | `GracefulShutdown` 是怎样的关闭流程？ | §2.3 |

## 覆盖: 5 问 / 3 身份 / 100%
