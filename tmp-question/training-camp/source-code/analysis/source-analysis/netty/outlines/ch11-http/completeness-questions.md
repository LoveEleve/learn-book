# Ch11 HTTP Codec 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | HttpServerCodec 的 HEAD/CONNECT 跟踪为什么用 long 位队列? | 8.1 §2 |
| 2 | HttpObjectAggregator 三阶段: start→aggregate→finish 各做什么? | 8.2 §1 |
| 3 | FullHttpRequest 和 HttpRequest+HttpContent 的差异是什么? 为什么要聚合? | 8.2 §5 |
| 4 | HttpContentCompressor 的 Accept-Encoding 优先级怎么确定? | 8.2 §4 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | HttpServerCodec 为什么用 CombinedChannelDuplexHandler 而非两个独立 Handler? | 8.1 §2 |
| 6 | handleOversizedMessage 三路分支 — 为什么不能统一处理? | 8.2 §2 |
| 7 | HttpContentCompressor 用 EmbeddedChannel 做 sub-channel — 为什么不用独立线程? | 8.2 §4 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 8 | HttpContent 和 HttpRequest 是什么关系? 为什么请求要分多块? | 8.1 §4 |

## 覆盖: 8 问 / 3 身份 / 100%
