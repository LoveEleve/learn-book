# T-2 Connector+Adapter 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `Http11NioProtocol` 只有 76 行 — 它到底做了什么？真正的 HTTP/1.1 解析逻辑在哪？ | §1.1 |
| 2 | `AbstractProtocol.release()` 中 `recycledProcessors.push(processor)` — 如果池满了(>200)会怎样？ | §2.2 |
| 3 | `CoyoteAdapter.service()` 中 `req.getNote(ADAPTER_NOTES)` 返回 null — 首次请求如何创建 catalina Request？ | §3.1 |
| 4 | `response.getOutputStream()` 和 `response.getWriter()` 为什么不能同时调用？错误在哪行触发？ | §4.2 |
| 5 | `request.recycle()` 有 43 行代码 — 为什么不直接 new 一个新的？ | §4.3 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么需要 coyote 和 catalina 两层 Request/Response？如果合并成一个类会有什么问题？ | §4 全章 |
| 7 | Protocol 和 Endpoint 的解耦 — 如果不分离，换 NIO→APR 需要改多少代码？ | §1.2 |
| 8 | `mapper.map()` 返回 void — 为什么用"传入可变对象→填充"而非"返回新对象"？这对 GC 有什么影响？ | §3.2 |
| 9 | URI 安全管线在 Adapter 层而非 Container 层 — 为什么？放在 StandardEngineValve 里有什么问题？ | §3.3 |
| 10 | `proxyName/proxyPort` 和反向代理场景 — Tomcat 怎么知道原始 Host header 是什么？ | §3.3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 11 | 一个 HTTP 请求从 `accept()` 到 `servlet.service()` — 经历了哪些对象？流程是什么样的？ | §2→§3→§4 |
| 12 | Processor 和 Pipeline 的关系 — Processor 处理 HTTP 协议，Pipeline 处理请求路由 — 它们怎么衔接？ | §2.1→§3.1 |

## 覆盖: 12 问 / 3 身份 / 100%
