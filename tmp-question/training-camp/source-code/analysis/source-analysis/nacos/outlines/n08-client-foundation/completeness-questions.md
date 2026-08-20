# N-08 客户端基础 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. PROTOTYPE 原型模式的继承链怎么工作? 为什么必须 derive 创建?
2. SearchableProperties 的多源合并优先级?
3. ClientAuthService SPI 集合与 SecurityProxy 的遍历关系?
4. RAM 认证的"凭证+注入+签名"三步?
5. ValidatorUtils.checkInitParam 校验什么?

## B. 源码实证 (6)

6. PROTOTYPE 的定义? (grep NacosClientProperties:36)
7. SourceType 有哪些? (grep env/SourceType.java:23)
8. SecurityProxy 遍历 SPI 的代码? (grep L82-83)
9. ResourceInjector 的四个实现? (grep auth/ram/injector)
10. ConcurrentDiskUtil 被谁消费? (grep DiskCache)
11. MetricsMonitor 的客户端指标? (grep client/monitor)

## C. 推理深挖 (5)

12. STS 凭证轮换 (StsCredentialHolder) 的过期处理?
13. derive 的属性覆盖语义? 子属性改父不动?
14. 认证失败时 SecurityProxy 的降级?
15. JvmArgsPropertySource 与 SystemEnv 的优先级冲突?
16. NacosLogging 与日志框架的适配?

## D. 跨域扩展 (4)

17. 本域属性体系 vs NC-1 init 链: 属性源头闭合?
18. 客户端认证 SPI vs NC-7 服务端 ProtocolAuthService: 双 SPI 对照?
19. RAM 签名 vs ALI-A3 的 NacosServiceManager: 请求面的认证注入?
20. 本域 vs openjdk 的 globals.hpp/flags 面: 参数体系的规划对照?
