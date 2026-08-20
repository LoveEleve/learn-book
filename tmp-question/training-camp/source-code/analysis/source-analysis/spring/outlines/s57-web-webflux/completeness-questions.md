# C-15 WebFlux 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | WebFlux 和 Spring MVC 什么关系？ | §1 (同构前端控制器) + §3 (对照) |
| 2 | RouterFunction 和 @RequestMapping 区别？ | §2 (函数式 vs 注解) |
| 3 | WebClient 和 RestTemplate 区别？ | §3 (非阻塞 vs 阻塞) |
| 4 | 响应式请求怎么被处理？ | §1 (DispatcherHandler Flux 链) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 Flux 声明式组合而非命令式循环？ | §1 关键设计 (非阻塞) |
| 6 | 为什么用函数式路由而非注解？ | §2 关键设计 (可组合/可测) |
| 7 | 为什么 WebClient 取代 RestTemplate？ | §3 关键设计 (线程耗尽) |
| 8 | 两套栈怎么迁移(MVC→Flux)？ | §2/§3 (组件对照) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | Mono/Flux 组合算子各自做什么？ | §1 (concatMap/next/switchIfEmpty/flatMap) |
| 10 | HandlerFunction.handle 返回什么？ | §2 (Mono<ServerResponse>) |

## 覆盖: 10 问 / 3 身份 / 100%
