# T-4 线程模型 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

> 格式升级: 旧多视角 9 问 → 新 A/B/C/D 四节 20 问 (2026-08-17 查漏补缺)

### A. 机制理解 (5)
1. Acceptor 的 countUpOrAwaitConnection() 在 maxConnections 达到后怎么阻塞? 阻塞在哪里释放?
2. Poller 的 events() 和 selector.select() 的执行顺序 — 为什么先处理注册事件?
3. wakeupCounter CAS 为什么只在 wakeupCounter==0 时才 selector.wakeup()? 目的是什么?
4. Tomcat 的 Acceptor/Poller/Worker 三线程模型和 Netty 的 Boss/Worker 有什么不同?
5. Spring Boot `server.tomcat.threads.max=200` — 这个 200 是 Worker 线程池的最大线程吗? 它控制的是什么?

### B. 源码实证 (6)
6. maxConnections 默认值与类型? (grep tomcat/util/net/AbstractEndpoint.java:548)
7. countUpOrAwaitConnection() 的实现位置与 maxConnections==-1 分支? (grep AbstractEndpoint.java:1477)
8. NioEndpoint.Poller 类声明与 wakeupCounter 类型? (grep tomcat/util/net/NioEndpoint.java:595/605)
9. selector.select() 与 wakeupCounter.getAndSet(-1) 的配合? (grep NioEndpoint.java:750-757)
10. createPollerEvent 的调用场景 (OP_REGISTER/interestOps)? (grep NioEndpoint.java:635/659/731)
11. Acceptor 在哪个类? run() 里 countUpOrAwaitConnection 调用点? (grep tomcat/util/net/Acceptor.java:116)

### C. 推理深挖 (5)
12. keep-alive 连接复用 — Poller 如何在一个 TCP 连接上检测 N 个 HTTP 请求? 状态在哪记录?
13. PollerEvent 缓存池 — createPollerEvent 中怎么实现复用? 池大小谁控制?
14. 为什么不用"一连接一线程"的 BIO 模型? NIO 的 Selector 解决了什么问题? 代价是什么?
15. maxConnections=-1 (无限) 与 limitLatch 的关系 — 什么时候会设 -1?
16. Poller 线程数与 CPU 核数的关系? 为什么默认 2 个? 配置哪个属性?

### D. 跨域扩展 (4)
17. 本域 vs T-2 Connector: Endpoint 的 IO 线程与 Worker 线程池的分工边界?
18. 本域 vs Nacos: Nacos 的 NIO 连接 (GrpcServer) 与 Tomcat NIO 模型对照?
19. 本域 vs Netty: Netty EventLoop 的线程模型 vs Tomcat Poller — 谁更优? 为什么 Tomcat 不用单 EventLoop 模型?
20. 本域 vs openjdk: Java NIO Selector 的 epoll 实现 vs Tomcat Poller 的封装层?