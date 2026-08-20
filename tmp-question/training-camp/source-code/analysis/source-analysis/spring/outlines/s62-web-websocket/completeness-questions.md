# C-20 WebSocket 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @EnableWebSocket + addHandler 怎么把路径映射到处理器？ | §1 (注册链) |
| 2 | 服务器怎么主动给客户端推消息？ | §2 (session.sendMessage) |
| 3 | 收到客户端消息在哪个回调处理？ | §2 (handleMessage) |
| 4 | 握手时怎么鉴权/拒绝连接？ | §3 (HandshakeInterceptor.beforeHandshake) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么复用 HandlerMapping 而非新机制？ | §1 关键设计 (同构于 MVC) |
| 6 | 为什么用四回调而非一个 handle？ | §2 (生命周期分明 + 模板方法) |
| 7 | 为什么鉴权放握手拦截器而非连接回调？ | §3 (连接前最后防线) |
| 8 | allowedOrigins 解决什么问题？ | §3 (跨域白名单) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | HTTP 怎么变成 WebSocket 的？ | §3 (doHandshake 升级) |
| 10 | TextWebSocketHandler 免去了什么？ | §2 (文本消息类型判断) |

## 覆盖: 10 问 / 3 身份 / 100%
