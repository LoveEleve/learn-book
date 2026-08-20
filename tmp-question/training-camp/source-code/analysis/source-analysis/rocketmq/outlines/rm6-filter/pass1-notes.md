# RM-6 消息过滤 — Pass 1 探索笔记

> 域: RM-6 消息过滤 | 🟡 B 方案 | 2026-08-14
> 源码: filter 模块 (6135 行/28 文件) + broker/filter (执行面) | RocketMQ 5.3.1

## 调用图

```
类型面 (common/filter):
ExpressionType: SQL92 (完整 SQL 语法: AND/OR/NOT/BETWEEN/IN/IS NULL/=TRUE + 类型) + TAG ("tag1 || tag2", null/* 全订阅); isTagType 判定

SPI 面 (filter 模块):
FilterSpi (compile/ofType) + FilterFactory (注册表, 静态注册 SqlFilter) — 可扩展
SqlFilter: FilterSpi 实现 → JavaCC 解析 (parser/SelectorParser 1401 行) → expression AST
expression/: BooleanExpression/ComparisonExpression/LogicExpression/UnaryInExpression/
  PropertyExpression/ConstantExpression/NowExpression... (ActiveMQ 风格表达式树)
util/: BloomFilter (m/k 数学) + BitsArray (位数组)

执行面 (broker/filter):
ExpressionMessageFilter (两级):
  isMatchedByConsumeQueue (CQ 粗筛): TAG → codeSet.contains(tagsCode) /
    SQL92 → BloomFilter 位图命中 (cqExtUnit.getFilterBitMap + isMsgInLive 时间窗)
  isMatchedByCommitLog (精筛): SQL92 → 编译表达式 evaluate (属性上下文 MessageEvaluationContext)
CommitLogDispatcherCalcBitMap (RM-5 装配): 写时 hashTo 计算消息位图 → 存 CQ Ext (RM-3 交叉)
ConsumerFilterManager/ConsumerFilterData: 过滤元数据 (group/topic/expression/compiledExpression/
  bornTime/deadTime/bloomFilterData/clientVersion) + BloomFilter 实例
```

## 基本元素分解

1. **表达式类型**: SQL92 (语法文档) vs TAG (简单 OR)
2. **SPI 工厂**: FilterSpi + FilterFactory 注册表
3. **解析器**: JavaCC (SelectorParser) → AST
4. **表达式求值**: evaluate (属性上下文)
5. **布隆过滤**: 两级 (CQ 位图粗筛 + CommitLog 精筛) + 写时位图计算
6. **元数据管理**: ConsumerFilterData (注册/过期)

## 标记问题 (20 问)

1. SQL92 支持哪些语法? (AND/OR/NOT/BETWEEN/IN/IS NULL)
2. TAG 表达式怎么匹配? (codeSet 哈希)
3. FilterSpi 扩展面? (编译/类型)
4. JavaCC 解析器生成? (SelectorParser)
5. 表达式 AST 结构? (Comparison/Logic/Unary)
6. evaluate 上下文? (消息属性)
7. 布隆参数怎么定? (f/n → k/m 数学)
8. 双哈希技巧? (Kirsch-Mitzenmacher)
9. 位图存哪? (CQ Ext filterBitMap)
10. 写时位图计算? (CalcBitMap 分发)
11. 两级过滤的先后? (粗筛跳过/精筛兜底)
12. isMsgInLive 时间窗? (消息存活校验)
13. ConsumerFilterData 生命周期? (born/dead)
14. 类过滤模式? (isClassFilterMode)
15. FilterServer 现状? (废弃?)
16. 误判率配置? (默认 f/n)
17. TAG 的 codeSet? (tags 哈希集合)
18. SQL92 属性缺失? (null 语义)
19. 性能面? (位图避免全量属性解析)
20. 测试面? (Parser/Expression/Bloom/FilterSpi)

## 时空溯源 (代码内痕迹)

- 3.x: TAG 过滤 (codeSet 哈希) + FilterServer (独立过滤进程 — 5.x 废弃, BrokerOuterAPI FilterServerList 残留)
- 4.x: SQL92 过滤 (JavaCC 解析 + 表达式树) + **布隆两级过滤** (位图粗筛)
- 5.x: FilterFactory SPI 化 (FilterSpi 注册表) / ConsumerFilterData 增强 (clientVersion/born-dead) / Ext 位图 (RM-3)

## 大域拆分判断

filter 模块 6135 行 + broker 执行面 — 单篇 (🟡 B, 6 闭环)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "ExpressionType/SQL92/过滤链" | SQL92+TAG 双类型; 两级过滤链 (CQ+CommitLog) | **接受** ✅ |
| "FilterServer" (执行计划 RM-4 提) | **5.x 已废弃**: 仅 BrokerOuterAPI FilterServerList 残留 | **修正**: FilterServer 不再独立进程, 过滤在 broker 内嵌 |
| 数字: BloomFilter | f (1-99%) + n → k=ceil(log(0.5,f)), m=n×log2(1/f)×log2(e) 8 对齐 | **补充** ✅ |
| 数字: 测试 | filter 5 文件 1469 行 + broker Filter 2 文件 660 行 | **补充** ✅ |
