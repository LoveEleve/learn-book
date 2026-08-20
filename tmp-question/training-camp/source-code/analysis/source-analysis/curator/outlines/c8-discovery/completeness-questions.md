# C-8 服务发现 — completeness-questions (全视角提问验证)

## 开发者视角

1. ServiceDiscovery 注册前需要 start() 吗? start() 做了什么?
2. registerService 重复注册同一 id 会怎样? 覆盖还是报错?
3. updateService 什么时候抛 "Service not registered"?
4. ServiceCache.getInstances() 和 provider.getAllInstances() 区别?
5. ServiceProvider.getInstance() 每次调用都会重新选吗? 快照能持有吗?
6. noteError 是同步生效还是异步? 调几次生效?
7. DownInstancePolicy 的参数什么意思? 错误阈值和窗口怎么配?
8. ServiceInstance.Builder 必填字段有哪些? port 必填吗?
9. unregisterService 后缓存多久消失?

## 架构师视角

10. 注册为什么用临时节点? 崩溃下线怎么保证?
11. reRegisterServices 解决什么场景? 启动时 ZK 未就绪?
12. ServiceProvider 为什么组合 ServiceCache + FilteredInstanceProvider + Strategy + DownInstanceManager 四件套?
13. 过滤链的顺序: 用户过滤器、DownInstanceManager、isEnabled 谁先谁后? 影响?
14. DownInstanceManager 的错误计数为什么按实例? 按错误比例还是次数?
15. 与 Nacos 对比: 主动健康检查 vs 会话临时节点, 优缺点?
16. 与 Eureka 对比: self-preservation vs ZK 严格一致?
17. 为什么注释强调"不要持有 getInstance 返回的实例"?
18. 服务名路径结构 basePath/name/id 的职责分层?

## 学生视角

19. 什么是服务发现? 和 DNS 有什么区别?
20. 什么是临时节点? 会话断了下线?
21. 什么是负载均衡策略? 随机/轮询/粘性各是什么?
22. 什么是熔断? noteError 和熔断器什么关系?
23. 什么是注册中心? ZK 能做注册中心吗?
