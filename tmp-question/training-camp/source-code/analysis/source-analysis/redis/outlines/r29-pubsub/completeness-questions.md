# R-29 pubsub+notify — completeness-questions

## 开发者视角

1. 订阅一个频道后, 数据存在哪几个表里?
2. PUBLISH 怎么找到订阅者? (频道直发)
3. PSUBSCRIBE 的模式匹配算法?
4. 退订最后一个订阅者时频道表怎么处理?
5. RESP2 vs RESP3 的消息格式差异?
6. RESP2 订阅状态下能执行什么命令? 为什么?
7. notify-keyspace-events 的字母→位映射?
8. __keyspace@ 和 __keyevent@ 频道格式?

## 架构师视角

9. pubsubtype 抽象解决什么 (全局/分片双轨)?
10. 为什么 PUBLISH 非 cluster 时复制到从库, cluster 时走 gossip?
11. keyspace 通知默认关闭的原因?
12. A (NOTIFY_ALL) 为什么不含 K/E/m?
13. 慢消费者的输出缓冲三元组 (32/8/60) 设计?
14. shard pubsub 为什么不做模式匹配?
15. 客户端 dict 与服务器镜像的双向注册为什么 O(1)?
16. 通知在从库上的行为 (复制命令也会触发)?

## 学生视角

17. PUBLISH 的返回值是什么? (接收客户端数)
18. 模式订阅的 receivers 怎么数?
19. CLIENT_PUSHING 标志什么时候置位?
20. NUMSUB 与 NUMPAT 的区别?
