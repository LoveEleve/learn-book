# NC-7 安全+监控+限流/加密 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. ProtocolAuthService 泛型的意义? gRPC/HTTP 双实现差异?
2. SecurityProxy 的登录上下文怎么注入请求头?
3. Limiter 的按 key 限流与缓存衰减?
4. encryptedDataKey 为什么需要独立容灾?
5. 三处 MetricsMonitor 的分层依据?

## B. 源码实证 (6)

6. ProtocolAuthService 的接口方法? (grep 接口)
7. authEnabled 主开关在哪配置? (grep NacosAuthConfig)
8. SecurityProxy.login 的调用链? (grep L78)
9. Limiter 默认 QPS? (grep L47-49)
10. LocalEncryptedDataKeyProcessor extends 谁? (grep L38)
11. core MetricsMonitor 的 RAFT 指标? (grep L64-79)

## C. 推理深挖 (5)

12. 认证失败 (token 过期) 时客户端怎么重新登录?
13. Limiter 的 tryAcquire(1000ms) 超时语义? 阻塞还是立即失败?
14. 加密配置在 failover 路径的键缺失会怎样?
15. ResourceParser 族 (grpc/http) 的解析差异?
16. 三处 MetricsMonitor 的注册中心统一性?

## D. 跨域扩展 (4)

17. 本域认证 vs NC-2 的 ConfigChangeParser SPI: 插件机制的两种形态?
18. Limiter 限流 vs Sentinel 5.9 的流控: 客户端内部 vs 独立框架?
19. encryptedDataKey vs ALI-A1 的 {cipher} 双路径: 键与密文的分工?
20. 本域与执行计划 5.8 NC-7 的对照: 规划断言修正后的完整性?
