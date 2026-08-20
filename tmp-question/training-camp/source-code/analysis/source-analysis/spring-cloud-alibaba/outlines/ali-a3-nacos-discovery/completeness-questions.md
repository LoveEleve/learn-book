# ALI-A3 Nacos 服务发现+注册 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. 发现双通道 (Remote+Cached) 中, 缓存为什么是"读时写穿"而非主动维护?
2. getInstances 失败抛异常 vs getServices 失败返回空列表 — 为何不对称?
3. hostToServiceInstance 为什么客户端还要二次过滤? 服务端不是已经过滤了吗?
4. Nacos 的 UP/DOWN 状态用 enabled 表达 — 这个设计的前提是什么?
5. getManagementRegistration 返回 null 对管理端口注册意味着什么?

## B. 源码实证 (6)

6. failureToleranceEnabled 的默认值和配置前缀? (grep NacosDiscoveryClient:47-48)
7. selectInstances 的第三个参数 true 是什么? (grep NacosServiceDiscovery:58)
8. 六键元数据注入的具体位置? (grep L98-106)
9. NacosAutoServiceRegistration 的端口仲裁逻辑? (grep L56-61)
10. ServiceCache 的 instancesMap 是什么 Map? (grep L48)
11. NacosServiceManager 的 NamingService 为什么 volatile? (grep L40/L86-95)

## C. 推理深挖 (5)

12. 如果实例 health 变 false, 客户端缓存多久能反映? 谁负责更新?
13. failFast=true 时 register 失败会怎样? rethrowRuntimeException 后启动流程如何?
14. setStatus DOWN 后重新 registerInstance — 幂等性靠什么保证?
15. NacosRegistration.init 的 management 端口检测在何时触发? @PostConstruct 阶段 WebServer 就绪了吗?
16. ServiceCache 的 unmodifiableList 包装防止什么? 业务代码修改列表会怎样?

## D. 跨域扩展 (4)

17. NacosDiscoveryClient vs Commons SCC-3 的 DiscoveryClient 契约: 四方法哪些默认哪些实现?
18. NacosAutoServiceRegistration vs SCC-4 的 AbstractAutoServiceRegistration: 哪些模板方法被覆写?
19. ServiceCache 的"写穿+非实时" vs SCC-6 的 CacheFlux 响应式缓存: 两种缓存哲学?
20. NacosServiceRegistry.setStatus 的 enabled 翻转 vs Nacos 服务端的健康检查: 状态语义谁权威?
