# S-10 传输与 Dashboard 域 — 全视角提问

## 功能

1. CommandCenter 与 HeartbeatSender 谁负责什么？
2. 17 个 CommandHandler 如何注册与分发？
3. 三种 CommandCenter 实现的选择边界是什么？
4. HeartbeatSender 如何把机器信息注册到 dashboard？
5. Dashboard 如何查询规则、修改规则、再推回机器？
6. MetricController 如何聚合展示指标？
7. 集群 dashboard controller 如何分配 client/server 模式？

## 性能

8. 正则命令映射或 interceptor 是否会影响命令执行性能？
9. 心跳任务的默认周期与线程池策略？
10. Dashboard 规则推送对多机器 app 的 fan-out 开销？

## 并发

11. CommandCenter 的 handler map 是否线程安全？
12. CommandHandlerInterceptor 的链式包装顺序？
13. Dashboard repository 的内存存储与并发一致性？

## 扩展

14. 新增一个 CommandHandler 需要哪些步骤？
15. 新增一种 HeartbeatSender/CommandCenter 实现如何通过 SPI 生效？
16. 新增一种 DynamicRuleProvider/Publisher 如何接入 Dashboard？

## 边界

17. 没有可用 CommandCenter/HeartbeatSender SPI 时会怎样？
18. Provider 拉不到机器规则时怎么处理？
19. 推规则时非健康机器如何处理？
20. MetricController 查不到指标时返回什么？

## 演进

21. 为什么 transport-common 只定义 SPI/handler，而不内置协议实现？
22. Dashboard 为什么既要 repository 又要 provider/publisher，两层存储的边界是什么？
