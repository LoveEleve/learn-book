# S-4 SpringApplication.run 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | run() 一行做了什么？六步各是什么？ | §1 (启动地图) |
| 2 | 环境为什么先于容器就绪？ | §2 (容器创建需要环境) |
| 3 | Servlet/Reactive 容器怎么选的？ | §2/§3 (deduceFromClasspath) |
| 4 | spring.main.* 怎么生效的？ | §2 (bindToSpringApplication) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | refresh/callRunners 为什么复用而不是重讲？ | §1 (06 §2.5 机制复用) |
| 6 | 为什么从 classpath 推断 Web 类型？ | §2 (零配置三类型) |
| 7 | 为什么用 ApplicationContextFactory？ | §3 (类型选择集中+可定制) |
| 8 | prepareContext 做什么？ | §3 (主源/初始化器/环境) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 启动异常在哪处理？ | §1 (handleRunFailure→S-16) |
| 10 | 嵌入式服务器在哪一步创建？ | §3 (refresh 的 onRefresh) |

## 覆盖: 10 问 / 3 身份 / 100%
