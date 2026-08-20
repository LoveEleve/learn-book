# D-8a 传输抽象 + exchange — completeness-questions (全视角提问验证)

## 开发者视角

1. 传输门面? (Exchangers → Transporters → SPI)
2. 换传输框架? (transporter 参数)
3. request 怎么异步? (DefaultFuture)
4. 响应怎么回填? (id → future)
5. 心跳? (60s 默认)
6. 线程模型? (Dispatcher 5 实现)
7. 重连? (NettyClient.doConnect)
8. 多协议端口? (PortUnification)

## 架构师视角

9. 为什么三层门面? (每层可换)
10. 为什么 id→future 表? (无状态网络关联)
11. 为什么 CompletableFuture? (与 D-4 无缝)
12. 为什么心跳成对? (双向连通验证)
13. 为什么 Timer 任务族分工? (保活/清理/超时)
14. 为什么 IO/业务分离? (IO 线程不阻塞)
15. 为什么 5 种线程模型? (场景选择)
16. 为什么自动重连? (网络抖动自愈)
17. 为什么 PortUnification? (端口节约)

## 学生视角

18. 什么是 exchange? (请求响应语义层)
19. 什么是 DefaultFuture? (异步结果)
20. 什么是心跳? (长连接保活)
21. 什么是 IO 线程? (网络收发线程)
