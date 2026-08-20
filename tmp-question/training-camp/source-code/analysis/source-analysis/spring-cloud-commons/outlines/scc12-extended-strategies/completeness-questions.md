# SCC-12 LoadBalancer 扩展策略 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. 六种扩展 Supplier 的共同模式? 数据来源差异?
2. 权重默认从哪取? WeightFunction 怎么自定义?
3. zone 过滤的回退语义? 为什么"绝不空手"?
4. hint 的双来源? 无 hint 时行为?
5. 粘性会话的 cookie 流程? 无 cookie 时?

## B. 源码实证 (5)

6. METADATA_WEIGHT_KEY 的值? (grep Weighted:41)
7. ZonePreference 的 ZONE 键? (grep L42)
8. Subset 的分桶公式? (grep L71-78)
9. SameInstancePreference 的 selectedServiceInstance 覆写? (grep L94-95)
10. StickySession 的 cookie 名配置? (grep L60)

## C. 推理深挖 (5)

11. 权重展开后怎么影响 SCC-7 的选择? (列表顺序 vs 重复实例?)
12. zone 回退全部与"区域故障"的关系? 为什么不全失败?
13. hint 过滤与 SCC-5 的 hint 提取 (getHint) 的关系?
14. 粘性 cookie 匹配不到实例 (实例下线) 时会发生什么?
15. 同实例偏好会导致"热实例过载"吗? 为什么没有保护?

## D. 跨域扩展 (5)

16. Weighted vs Ribbon 的 WeightedResponseTimeRule (动态响应时间权重)?
17. ZonePreference vs Ribbon 的 ZoneAwareLoadBalancer?
18. Subset 分桶 vs Kafka 的分区分配算法?
19. StickySession vs Spring Session 的会话保持?
20. 如果组合"加权 + 区域 + 粘性", 应该在 SCC-6 Builder 怎么排? 顺序影响?
