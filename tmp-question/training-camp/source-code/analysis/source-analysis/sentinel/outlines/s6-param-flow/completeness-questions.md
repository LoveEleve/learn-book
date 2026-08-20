# S-6 热点参数限流域 — 全视角提问

## 功能

1. 热点参数限流和普通 FlowSlot 有什么区别？
2. 参数值从哪来？`paramIdx` 和 `ParamFlowArgument` 如何确定统计 key？
3. 三种 grade/behavior 组合分别是什么行为？
4. 特殊热点项(`paramFlowItemList`/`hotItems`)如何获得独立阈值？
5. 规则更新后，已删除资源/规则的统计如何回收？

## 性能

6. 为什么参数统计 map 有容量上限？淘汰策略是什么？
7. 参数统计为什么用 `putIfAbsent` + 原子累加？
8. 集群模式下热点限流的开销点在哪？

## 并发

9. 线程级热点限流如何并发计数？`addThreadCount`/`decreaseThreadCount` 是否配对？
10. 令牌桶判定的 CAS 循环在什么条件下会忙等？
11. 匀速排队的时间线用 CAS 推进，竞争失败会怎样？

## 扩展

12. 如何新增一种参数流控行为？
13. `ParamFlowArgument` 的作用是什么？自定义热点 key 怎么接入？

## 边界

14. 参数值为 null、Collection、数组时分别怎么处理？
15. `paramIdx` 为负值（倒数）如何解析？
16. `paramIdx` 越界时是否拦截？
17. 令牌桶 `burstCount` 的作用是什么？
18. `durationInSec` 与 `count` 如何共同决定速率？

## 演进

19. `HotParamSlotChainBuilder` 为什么废弃？
20. 热点限流为什么从 0.2.0 就存在，却一直放在 extension 而不是 core？
