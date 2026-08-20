# N-16 配置控制器面 — 完备性问题集 (20 问)

## A. 机制理解 (5)
1. 九控制器的职责划分?
2. 四个切面的横切点?
3. publishConfig 的请求参数组装?
4. 九个控制器中哪些是运维专属?
5. v2/v3 分代的 URL 策略?

## B. 源码实证 (6)
6. ConfigController.publishConfig 转发? (grep L222)
7. 九控制器清单? (grep controller/)
8. 切面类名? (grep aspect/)
9. CapacityController 的容量查询?
10. ListenerController 的监听管理?
11. HistoryController 的历史查询?

## C. 推理深挖 (5)
12. ConfigOpsController 的运维操作?
13. CommunicationController 的通信面?
14. 容量切面的限制语义?
15. beta 操作与灰度 (N-15) 的关系?
16. export 配置的批量语义?

## D. 跨域扩展 (4)
17. 容量切面的限制触发与响应?
18. 变更切面的记录内容?
19. 失败切面的错误码语义?
20. 请求日志切面的采样?

