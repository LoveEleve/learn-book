# T-4 线程模型 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | Acceptor 的 `countUpOrAwaitConnection()` 在 maxConnections 达到后怎么阻塞？ | §1.1 |
| 2 | Poller 的 `events()` 和 `selector.select()` 的执行顺序 — 为什么先处理注册事件？ | §2.1 |
| 3 | `wakeupCounter` CAS 为什么只在 `wakeupCounter==0` 时才 `selector.wakeup()`？ | §1.2 |
| 4 | PollerEvent 缓存池 — 怎么在 createPollerEvent 中实现复用？ | §2.2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | Tomcat 的 Acceptor/Poller/Worker 三线程模型和 Netty 的 Boss/Worker 有什么不同？ | §1 全章 |
| 6 | Spring Boot `server.tomcat.threads.max=200` — 这个 200 是 Worker 线程池的最大线程吗？ | §1.3 |
| 7 | keep-alive 连接复用 — Poller 如何在一个 TCP 连接上检测 N 个 HTTP 请求？ | §2.3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 8 | 为什么 Tomcat 不用"一连接一线程"的 BIO 模型？NIO 的 Selector 解决了什么问题？ | §1.2 |
| 9 | Acceptor/Poller/Worker — 通常有几个线程？可以配置吗？ | §1.1-1.3 |

## 覆盖: 9 问 / 3 身份 / 100%
