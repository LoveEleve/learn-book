# N-14 命名集群与监控 — 完备性问题集 (20 问)

## A. 机制理解 (5)
1. ServerStatus 三态流转的条件?
2. READY_ONLY 状态的语义与触发?
3. TPS 监控的统计窗口?
4. TPS 统计的采样窗口与精度?
5. handler 族与 api 请求族的对应?

## B. 源码实证 (6)
6. ServerStatusManager 的状态管理? (grep cluster/)
7. 监控族类名? (grep monitor/)
8. gRPC handler 族? (grep remote/rpc/handler)
9. NamingTpsMonitor 的统计粒度?
10. ServiceTopNCounter 的排序算法?
11. 动态指标刷新服务的周期?

## C. 推理深挖 (5)
12. PerformanceLoggerThread 的日志输出?
13. gRPC handler 的注册方式?
14. TopN 计数器的时间窗口?
15. 性能日志的周期与内容?
16. handler 与 api 请求族的对应?

## D. 跨域扩展 (4)
17. READY_ONLY 状态下的读写行为?
18. 监控指标的内存开销?
19. handler 的异常处理?
20. UDP 遗留面的使用场景?

