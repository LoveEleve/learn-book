# S-7 规则管理域 — 全视角提问

## 功能

1. `RuleManager` 如何区分简单规则与正则规则？
2. `regexCacheRules` 缓存的作用与失效时机？
3. Authority 黑白名单如何精确匹配 origin？
4. SystemRule 的四种指标如何采样与判定？
5. 系统保护为什么只针对 IN 流量？
6. BBR 算法在系统负载判定中的作用？

## 性能

7. 正则规则缓存如何避免每次请求都跑正则？
8. `DynamicSentinelProperty` 的 listener 集合用什么结构？
9. 系统状态采样线程的周期与开销？

## 并发

10. `RuleManager` 的规则更新与查询是否线程安全？
11. `SystemRuleManager` 的 volatile 阈值如何保证可见性？
12. 多规则取最小值时并发更新是否一致？

## 扩展

13. 如何新增一种规则类型？
14. `NoOpSentinelProperty` 的用途？
15. `SimplePropertyListener` 简化了什么？

## 边界

16. origin 为空或 limitApp 为空时 Authority 如何判定？
17. 一个资源多条 authority 规则会怎样？
18. 系统规则未设置任何指标时是否检查？
19. CPU 使用率超过 1 的规则如何处理？

## 演进

20. `RuleManager` 的正则缓存是何时引入的？
21. 各 RuleManager 的统一模式是刻意设计还是自然收敛？
