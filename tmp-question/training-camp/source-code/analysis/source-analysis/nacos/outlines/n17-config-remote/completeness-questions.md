# N-17 配置长轮询与 gRPC 通信 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. 长轮询"提前 500ms"的语义?
2. AsyncContext 挂起与线程占用的关系?
3. gRPC 批量监听 vs HTTP 长轮询的演进?
4. RpcConfigChangeNotifier 的推送粒度?
5. 模糊订阅的 gRPC 面与命名面差异?

## B. 源码实证 (6)

6. compareMd5 立即响应路径? (grep LongPollingService:181-193)
7. timeout 的计算? (grep L210-212)
8. gRPC handler 族类名? (grep remote/)
9. ClientLongPolling 的 run? (grep L271)
10. checkLimit 的语义? (grep L202-208)
11. RpcConfigChangeNotifier 的推送? (grep remote/)

## C. 推理深挖 (5)

12. 长轮询挂起的连接上限? 超限怎么响应 (503)?
13. 500ms 提前量的可配置性 (FIXED_DELAY_TIME)?
14. 批量监听与单监听的手势差异?
15. 集群同步 (ConfigClusterRpcClientProxy) 的变更传播?
16. 连接断开时监听上下文的清理?

## D. 跨域扩展 (4)

17. 本域 vs NC-2 客户端: 两端监听闭合?
18. 双通道 (HTTP/gRPC) vs N-12 推送双执行器: 演进对照?
19. 长轮询 vs openjdk 的 safepoint 等待: 挂起模型对照?
20. 本域与 N-15 缓存: 变更检测的数据源?
