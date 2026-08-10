# Ch10 Codec 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | Cumulator 双策略什么时候选 MERGE, 什么时候选 COMPOSITE? | 7.1 §1 |
| 2 | callDecode 循环怎么判断 "需要等更多数据" vs "已经解码完了"? | 7.1 §3 |
| 3 | 三级状态机重入保护 — 什么操作会触发重入? inputMessages 队列存什么? | 7.1 §4 |
| 4 | LengthFieldBasedFrameDecoder 的四参数各控制什么? 给一个协议帧[2字节长度+payload]=>怎么配? | 7.2 §4 |
| 5 | ReplayingDecoder 的 REPLAY Signal 不是异常 — 那是怎么实现的回退? | 7.2 §6 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 四种拆包器都是 return null 让父类管理积攒 — 这体现了什么设计模式? 为什么不自己管理 buffer? | 7.2 §7 |
| 7 | MessageToByteEncoder 的 write 先 encode 再 release 原 msg — 如果 encode 抛异常原 msg 会泄漏吗? | 7.2 §1 |

## 学生/新人视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 8 | 为什么 TCP 需要拆包? 协议不是已经定义了消息格式吗? | 7.1 §2 |
| 9 | FixedLengthFrameDecoder 和 DelimiterBasedFrameDecoder 各适合什么协议? | 7.2 §2-3 |

## 覆盖: 9 问 / 3 身份 / 100%
