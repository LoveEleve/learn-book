# NC-1 NamingService 注册发现 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. 重载漏斗的意义? 接口 20+ 重载 vs 核心方法的关系?
2. ephemeral 实例走 gRPC、持久实例走 HTTP — 这个路由的合理性?
3. subscribe 为什么"先 redo 后发送"? 顺序反了会怎样?
4. processServiceInfo 为什么忽略空/错误推送? pushEmptyProtection 关掉会怎样?
5. 发现三路 (failover > 缓存 > 直查) 的优先级依据?

## B. 源码实证 (6)

6. init 里 NotifyCenter 注册了什么? (grep NacosNamingService:122-124)
7. getExecuteClientProxy 的路由条件? (grep Delegate:197-203)
8. subscribe 三连的精确顺序? (grep NamingGrpcClientProxy:392-410)
9. doDiff 的过期数据处理? (grep InstancesDiffer:48-54)
10. 无监听器才 unsubscribe 的条件? (grep NacosNamingService:531-534)
11. ProtectMode 默认阈值? (grep ProtectMode:27)

## C. 推理深挖 (5)

12. 服务端推送空列表 (服务下架) 时, pushEmptyProtection 开/关的差异?
13. 为什么 batch 操作强制 gRPC 不走 HTTP?
14. 断线重连后, 重做队列怎么恢复注册/订阅? (衔接 NC-3)
15. doDiff 用 toInetAddr 做 key, 同 ip:port 实例的元数据变化怎么算 modified?
16. ServiceInfoUpdateService 的 UpdateTask 与推送的竞态?

## D. 跨域扩展 (4)

17. NacosNamingService 重载漏斗 vs ALI-A3 的端口仲裁: 两种参数收敛?
18. selectInstances 过滤 vs ALI-A3 hostToServiceInstance 过滤: 双段式验证?
19. InstancesChangeEvent/NotifyCenter vs ALI-A5 NacosWatch 的 NamingEvent: 通知链路异同?
20. 本域订阅缓存 vs SCC-6 CacheFlux: 客户端缓存 vs 集成层缓存的层次?
