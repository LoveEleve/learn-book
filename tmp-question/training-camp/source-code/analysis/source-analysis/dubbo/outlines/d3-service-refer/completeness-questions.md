# D-3 服务引用 — completeness-questions (全视角提问验证)

## 开发者视角

1. 引用入口? (get → init → createProxy)
2. 双路径? (直连 parseUrl / 注册中心 aggregateUrlFromRegistry)
3. 本地兜底? (injvm localhost:0)
4. 启动检查? (checkInvokerAvailable 轮询)
5. invoker 怎么接入集群? (Cluster.join 三分支)
6. 代理怎么生成? (proxyFactory.getProxy → InvokerInvocationHandler)
7. generic 泛化引用? (GenericService)
8. 多注册中心? (ZoneAwareCluster 默认)

## 架构师视角

9. 为什么双路径? (直连调试 vs 注册中心动态发现)
10. 为什么本地始终可引? (与 D-2 本地始终可导对称)
11. 为什么 MigrationInvoker? (接口级→应用级迁移平滑)
12. 为什么 StaticDirectory? (静态列表 vs RegistryDirectory 动态订阅)
13. 为什么 buildFilterChain 标志? (多注册中心避免重复包装)
14. 为什么 ConsumerModel 挂载? (3.x 方法模型供过滤器用)
15. 为什么启动 fail-fast? (无 provider 早暴露)
16. 为什么 ProxyFactory 是 SPI? (javassist/jdk 可切, Wrapper 织入)

## 学生视角

17. 什么是引用? (消费者拿服务)
18. 什么是 invoker? (可调用单元)
19. 什么是 ClusterInvoker? (容错外壳)
20. 什么是服务发现迁移? (2.x 接口级 → 3.x 应用级)
