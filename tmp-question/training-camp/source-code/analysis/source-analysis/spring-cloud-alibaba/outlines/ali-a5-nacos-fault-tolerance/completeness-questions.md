# ALI-A5 Nacos 容错+心跳+优雅关闭 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. NacosWatch 订阅的是服务列表, 为什么只回写自身 metadata? 过滤条件是什么?
2. 心跳默认关闭的原因? issue#2868/#3258 大致是什么问题?
3. 优雅关闭为什么要 sleep? 不 sleep 会怎样?
4. 子上下文过滤对多 context 应用 (如 SCC-13 子上下文) 的意义?
5. 健康检查为什么镜像服务端状态而非本地探测?

## B. 源码实证 (6)

6. NacosWatch 的 listenerMap key 是什么格式? (grep buildKey)
7. resetIfNeeded 的更新条件? (grep L108-112)
8. 心跳的调度方式与间隔来源? (grep L67-68)
9. 优雅关闭三步的顺序? (grep L67-81)
10. supportsAsyncExecution 返回值与理由? (grep L83-87)
11. 健康检查的三种状态分支? (grep L65-69)

## C. 推理深挖 (5)

12. 控制台把实例 metadata 改了, 本地 properties 更新后谁消费新 metadata? (回链 A3 注册)
13. HeartbeatEvent 的 index 递增有什么用? (对照 SCC-3 HeartbeatMonitor)
14. 为什么 stop() 先 cancel future 再 shutdown scheduler? 顺序可换吗?
15. 优雅关闭里 autoServiceRegistration.stop() 与 ContextClosedEvent 的注册链是什么关系?
16. AnyNestedCondition 的 Gateway/SBA 两个生态条件为什么不直接 @ConditionalOnClass?

## D. 跨域扩展 (4)

17. NacosWatch vs SCC-3 的 probe 演进: 订阅制 vs 轮询制两种发现更新?
18. HeartbeatEvent vs SCC-3 HeartbeatMonitor: 发布者消费者关系?
19. 优雅关闭 vs 普通 Spring 停机 (ContextClosedEvent 默认行为): Nacos 加了几步?
20. 本域心跳/订阅 vs Nacos 5.8 的客户端心跳 (BeatReactor): 集成层与内核的分工?
