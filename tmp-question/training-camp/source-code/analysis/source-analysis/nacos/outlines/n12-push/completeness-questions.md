# N-12 推送面 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. PushExecutorDelegate 的 SPI 优先 + 默认兜底语义?
2. 延迟合并推送怎么削峰? 合并窗口内多次变更怎么办?
3. 订阅三实现 (本地/聚合/V2) 的分工?
4. 模糊推送为什么独立任务链?
5. NoRequiredRetryException 的语义与场景?

## B. 源码实证 (6)

6. Delegate 的双执行器字段? (grep PushExecutorDelegate:38-42)
7. SPI 查找代码? (grep L66-71)
8. PushExecuteTask.run 的逻辑? (grep L57-60)
9. 订阅服务的三个实现? (grep push/)
10. FuzzyWatch 任务族类名? (grep v2/task/FuzzyWatch*)
11. PushResultHook 的实现? (grep v2/hook)

## C. 推理深挖 (5)

12. 推送失败重试的策略 (PushConfig 重试次数)?
13. rpc vs udp 的选择依据 (ClientInfo 协议判定)?
14. 聚合订阅 (AggregationImpl) 怎么跨集群?
15. 延迟窗口的合并条件 (相同 service)?
16. 推送回调在客户端断开时的处理?

## D. 跨域扩展 (4)

17. 本域 vs NC-1 客户端推送接收 (NamingPushRequestHandler): 两端闭环?
18. 延迟合并 vs openjdk 的 JIT 队列: 批量削峰对照?
19. SPI 推送 vs NC-5 Distro 组件: 注册表模式对照?
20. 本域 vs ALI-A5 客户端心跳: 推送与心跳的关系?
