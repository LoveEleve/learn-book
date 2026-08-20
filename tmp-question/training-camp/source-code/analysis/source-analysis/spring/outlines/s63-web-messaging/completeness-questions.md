# C-21 messaging 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 客户端消息怎么路由到 @MessageMapping 方法？ | §2 (SimpAnnotationMethodMessageHandler) |
| 2 | 服务器怎么主动给所有订阅者推送？ | §2 (SimpMessagingTemplate.convertAndSend) |
| 3 | 方法返回值怎么回推给客户端？ | §2 (@SendTo) |
| 4 | @EnableWebSocketMessageBroker 引入什么？ | §1 (通道/模板/代理) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么引入通道+代理而非直接用 WebSocketHandler？ | §1 关键设计 (解耦+高层抽象) |
| 6 | 收和推为什么分离？ | §2 (@MessageMapping 收 / 模板推) |
| 7 | SimpleBroker vs StompBrokerRelay 选型？ | §3 (单机 vs 集群) |
| 8 | 订阅表怎么维护的？ | §3 (SUBSCRIBE/UNSUBSCRIBE) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 消息在两个通道间怎么流转？ | §1 (inbound→路由→outbound→代理) |
| 10 | 广播时怎么知道发给谁？ | §3 (sendMessageToSubscribers) |

## 覆盖: 10 问 / 3 身份 / 100%
