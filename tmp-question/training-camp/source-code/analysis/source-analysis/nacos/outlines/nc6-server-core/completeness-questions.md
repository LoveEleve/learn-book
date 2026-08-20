# NC-6 服务端核心 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. Controller → Operator → ClientService 三层各管什么?
2. HealthCheckReactor 的 futureMap 去重语义?
3. UDP 推送与 gRPC 推送的共存理由?
4. ephemeral→Distro / persistent→JRaft 的路由依据?
5. 3.x 的 clientId 模型与 1.x 的差异?

## B. 源码实证 (6)

6. NamingApp 的 scanBasePackages? (grep NamingApp)
7. InstanceController.registerInstance 的转发? (grep L127)
8. scheduleCheck(BeatCheckTask) 的周期与去重? (grep HealthCheckReactor:56-64)
9. InstanceOperatorClientImpl.registerInstance 的落点? (grep L106-113)
10. push 包的双通道类? (grep UdpPushService/v2)
11. consistency 的双存储目录? (grep ephemeral/persistent)

## C. 推理深挖 (5)

12. 客户端断连 (gRPC 断开) 时 ClientServiceImpl 怎么清理实例?
13. BeatCheckTask 5s 周期与 v2 HealthCheckTaskV2 的差异?
14. 推送失败 (UDP 不可达) 的降级?
15. Datum/KeyBuilder 在 Distro/JRaft 里的角色?
16. config/server 的 DB 与文件双写?

## D. 跨域扩展 (4)

17. 服务端推送 vs NC-1 客户端订阅: 推送链路两端闭合?
18. ephemeral/persistent 路由 vs NC-5 的 AP/CP: 双轨落点实证?
19. 本域健康检查 vs 执行计划 5.8 NC-5 (TcpSuperSenseProcessor): 3.x 变化?
20. config/server vs NC-2 客户端: 配置链路两端闭合?
