# Ch12 HTTP/2 Codec 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | HEADERS/DATA/WINDOW_UPDATE 帧的 9 字节头结构是怎样的? streamId 字段在哪个字节? | §2 |
| 2 | 客户端和服务端的 streamId 取值范围有何区别(奇数/偶数)? 为什么? | §3 |
| 3 | HPACK 静态表有 61 项——`:method`、`:path`、`:status` 分别在哪些索引? | §4 |
| 4 | `consumeBytes(stream, numBytes)` 如何触发 WINDOW_UPDATE? `windowUpdateRatio` 默认多少? | §5 |
| 5 | `DefaultHttp2FrameReader` 如何从 9 字节帧头解析帧类型+payload? | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | HTTP/2 多路复用的 streamId 如何解决 HTTP/1.1 的队头阻塞? TCP 层队头阻塞为何仍存在? | §3 |
| 7 | HPACK 动态表是连接级共享的——这对多 Stream 并发场景有什么好处和风险? | §4 |
| 8 | 两层流控(连接级+流级)的设计意图是什么? 为什么不能只用连接级窗口? | §5 |
| 9 | `GrpcHttp2ConnectionHandler` 如何在 Netty HTTP/2 之上构建 gRPC RPC 语义? | 核心悬念 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 10 | HTTP/2 的二进制帧和 HTTP/1.1 的文本报文有什么区别? 为什么二进制更快? | §1 |
| 11 | 一个 gRPC 请求在 HTTP/2 上对应哪些帧? HEADERS 和 DATA 分别承载什么? | §3 |

## 覆盖: 11 问 / 3 身份 / 100%
