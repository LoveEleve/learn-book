# NC-3 gRPC 通信 + Redo 重做 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. RpcClient 的双队列 (事件队列 + 重连信号量) 各管什么? 为什么分开?
2. 重做四态机里 expectedRegistered 与 registered 的区别? 谁设置谁?
3. RedoScheduledTask 为什么先检查 connected? 断线重做会怎样?
4. removeInstanceForRedo 为什么要求 "!isExpectedRegistered" 才删?
5. AbstractRedoService 的泛型 Map 设计为谁铺路?

## B. 源码实证 (6)

6. RpcClient 状态机枚举值有哪些? (grep RpcClientStatus)
7. onServerListChange 的切换条件? (grep RpcClient:216-231)
8. 重连循环的 keepAlive 超时后做什么? (grep L270-290)
9. RedoData 四组合的语义表? (grep L100-110)
10. onDisConnect 标记了哪些数据? (grep NamingGrpcRedoService:100-113)
11. redoDelayTime 的默认值来源? (grep Constants.DEFAULT_REDO_DELAY_TIME)

## C. 推理深挖 (5)

12. 注册成功后又断线, 重连后 getRedoType 返回什么? 为什么?
13. 服务端列表变更导致 switchServerAsync — 重连期间 redo 队列怎么协同?
14. healthCheck 失败 → UNHEALTHY → 重连的完整流转?
15. 如果 redo 任务执行时服务端又断开, 会发生什么? (connected 检查的时机)
16. 批量注册 (BatchInstanceRedoData) 与单实例重做的差异?

## D. 跨域扩展 (4)

17. redo 队列 vs NC-2 的 listenExecutebell: 两种"待办"机制的差异?
18. ConnectionEventListener vs ALI-A5 NacosWatch 的订阅: 感知断线的层次?
19. 本域 RpcClient vs 规划声称的 client-basic/remote: 09 审计路径修正的意义?
20. Redo 机制 vs SCC-6 CacheFlux 的 onCacheMissResume: 故障恢复的两种哲学?
