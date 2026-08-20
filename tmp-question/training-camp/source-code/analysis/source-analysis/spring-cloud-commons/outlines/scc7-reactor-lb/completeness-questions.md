# SCC-7 ReactorLoadBalancer 策略 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. ReactorLoadBalancer 的 choose(Request) → Mono\<Response\> 响应式语义?
2. position 的 & Integer.MAX_VALUE 为什么是"忽略符号位"? 溢出会怎样?
3. 单实例为什么不转位置? 与健康过滤 Supplier 的联动?
4. 随机种子 (new Random().nextInt(1000)) 防什么?
5. RoundRobin 与 Random 的同构/差异点?

## B. 源码实证 (5)

6. seedPosition 构造的默认值? (grep RoundRobin:60)
7. SingletonSupplier.of 的兜底是什么? (grep L64-66)
8. getInstanceResponse 三态的精确行号? (grep L98-116)
9. ThreadLocalRandom 在哪用? (grep Random:86)
10. LoadBalancerClientFactory.getInstance 返回什么类型? (grep L79-80)

## C. 推理深挖 (5)

11. 为什么 choose 里 supplier.get(request).next() 取"第一个 Flux 元素"?
12. & MAX_VALUE 与取模的配合? position 循环到 MAX 后怎么回 0?
13. SelectedInstanceCallback 回调在 hasServer() 条件下的意义?
14. 多实例同时冷启动, seed 随机怎么防惊群?
15. 每服务一个 LoadBalancer 实例 vs 全局共享 — 状态隔离的意义?

## D. 跨域扩展 (5)

16. RoundRobin vs Ribbon 的 RoundRobinRule 实现对照?
17. ocelli 源码引用 vs ZAB 的选举轮次 (阶段 4.3)?
18. 响应式 choose vs SCC-5 阻塞 execute 的桥接 (Mono.block)?
19. SingletonSupplier vs SCC-8 的 ObjectProvider 延迟解析?
20. 如果加"加权轮询策略", 应该改哪层? (SCC-6 WeightedSupplier vs 新 LoadBalancer)
