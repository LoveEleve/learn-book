# T-3 Pipeline+双链 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `pipeline.getFirst()` 返回什么？是 basic Valve 还是 addValve？ | §1.1 |
| 2 | 4 个 Valve 的 invoke() 各自做了什么？`request.getHost()`/`getContext()`/`getWrapper()` 的数据从哪来？ | §2.1-2.3 |
| 3 | `filterChain.doFilter()` 内部怎么遍历 Filter？是递归还是循环？ | §3.1 |
| 4 | Filter 的 `chain.doFilter()` 和 Valve 的 `getNext().invoke()` 有什么区别？ | §3.2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | Tomcat 为什么需要两套 Chain of Responsibility（Valve 链 + Filter 链）？不能合并成一套吗？ | §3.2 |
| 6 | Netty 的 ChannelPipeline 是双向的 — Tomcat 的 Pipeline 是单向的 — 为什么 Tomcat 不需要 Outbound？ | §4.2 |
| 7 | 为什么 Tomcat Pipeline 用 `getNext().invoke()` 显式传递 — Netty 用 ctx 封装？哪种设计更好？ | §4.3 |
| 8 | 如果在 Engine/Host/Context 三层各加 AccessLogValve — 一个请求会触发 3 条日志？合理吗？ | §2.4 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | Pipeline 和 Container 的关系 — 是每个 Container 一个 Pipeline 吗？ | §1.3 |
| 10 | Valve 链和 Filter 链的执行顺序 — Valve 先还是 Filter 先？ | §2.3 |
| 11 | Chain of Responsibility 模式是什么？Tomcat 是怎么实现的？ | §1.1-1.2 |

## 覆盖: 11 问 / 3 身份 / 100%
