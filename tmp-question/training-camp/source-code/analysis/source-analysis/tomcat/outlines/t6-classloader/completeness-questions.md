# T-6 + T-7 全视角验证

## T-6 ClassLoader
| # | 身份 | 问题 | 答案位置 |
|:--:|:--:|------|------|
| 1 | 开发者 | `delegate=false` 和 `delegate=true` 有什么区别？哪个先加载本地类？ | §1.1 |
| 2 | 架构师 | Filter 名单为什么不允许应用覆盖 `jakarta.servlet.*`？ | §1.2 |
| 3 | 学生 | 双亲委派是什么？为什么要打破它？ | §1.1 |

## T-7 Spring Boot 集成
| # | 身份 | 问题 | 答案位置 |
|:--:|:--:|------|------|
| 4 | 开发者 | `getWebServer()` 一行代码创建了哪些组件？ | §1.1 |
| 5 | 架构师 | `server.tomcat.threads.max=200` 怎么到达 `AbstractEndpoint.setMaxThreads()`？ | §2.1-2.2 |
| 6 | 学生 | `GracefulShutdown` 是怎样的关闭流程？ | §2.3 |

## 覆盖: 6 问 / 3 身份 / 100%
