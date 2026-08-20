# N-18 配置审计/监控 — 完备性问题集 (20 问)

## A. 机制理解 (5)
1. 历史清理的触发与保留策略?
2. 内存监控的指标?
3. 加密过滤器的服务端语义?
4. 历史清理的保留窗口策略?
5. 加密过滤与 N-15 存储的关系?

## B. 源码实证 (6)
6. HistoryService 的职责? (grep service/)
7. 清理器类名? (grep DefaultHistoryConfigCleaner)
8. 监控七件清单? (grep monitor/)
9. MemoryMonitor 的监控指标?
10. PrintGetConfigResponeTask 的周期?
11. ResponseMonitor 的统计维度?

## C. 推理深挖 (5)
12. ThreadTaskQueueMonitorTask 的队列?
13. 动态指标刷新的注册方式?
14. 响应监控的统计窗口?
15. 线程队列监控的告警阈值?
16. 历史清理与容量切面的关系?

## D. 跨域扩展 (4)
17. 内存监控的告警阈值?
18. 历史清理与迁移的冲突?
19. 加密密钥轮换的流程?
20. 监控数据的上报通道?

