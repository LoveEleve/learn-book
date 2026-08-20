# C-1 CuratorFramework — completeness-questions (全视角提问验证)

## 开发者视角

1. newClient() 的三个重载差在哪? 默认 session/connection 超时是多少?
2. start() 为什么不幂等? 重复调用发生什么?
3. RetryLoop 三段式怎么用? 为什么每次循环要重取 ZooKeeper 实例?
4. ExponentialBackoffRetry(1000, 3) 的实际睡眠序列怎么算? 29 上限从哪来?
5. inBackground() 提交的操作断连时去哪了? 重连后怎么恢复?
6. blockUntilConnected() 的实现原理? 和 sleep 轮询有何区别?
7. namespace 设置后, 回调事件里的路径是带前缀还是不带?
8. withProtectedEphemeralSequential() 节点名长什么样? 保护了什么?
9. usingNamespace() 每次返回新对象吗? nonNamespaceView() 干嘛的?
10. 事务用 inTransaction() 和 transaction() 的区别?

## 架构师视角

11. 为什么 ConnectionStateManager 要单线程? 队列为什么是 25?
12. SUSPENDED vs LOST 的本质区别? 会话过期注入解决了什么问题?
13. 为什么客户端要"自证"会话过期 (injectSessionExpiration)? ZK 事件不可达时怎么收敛?
14. 后台操作队列为什么用 DelayQueue? 和普通队列差在哪?
15. 重试策略的双门控 (异常类型 + 计数) 各防什么?
16. close() 为什么先 clearSleep 再 drain? 竞态在哪?
17. 首连 CONNECTED 与重连 RECONNECTED 为什么分开? initialConnectMessageSent 语义?
18. 保护模式 (GUID) 与锁配方的关联? 为什么 create 响应丢失是真实问题?
19. 对比原生 ZooKeeper 客户端: Curator 多做了哪五层?
20. 多 ZK 版本兼容 (zookeeperCompatibility) 意味着什么设计约束?

## 学生视角

21. 什么是重试策略? 为什么叫策略 (Strategy) 模式?
22. 什么是会话? Expired 后旧节点为什么消失?
23. 什么是 namespace? 类比多租户?
24. 什么是 fluent API? 为什么 Curator 用它?
25. 指数退避为什么加随机?
