# S-7 规则管理域 — 大纲

## 上篇: 规则如何被管理 — 01-rule-manager.md

1. `RuleManager` 的简单/正则规则与缓存
2. `SentinelProperty` 发布-订阅基础设施
3. 各 RuleManager 的统一模式
4. `RuleConstant` 语义字典

## 中篇: 黑白名单与系统保护 — 02-authority-system.md

1. `AuthorityRule` 黑白名单判定
2. `AuthorityRuleManager` 一资源一规则
3. `SystemRuleManager` 系统状态采样
4. `SystemSlot` 的 IN 流量判定与 BBR

## 下篇: 规则管理的一致性 — 03-rule-pattern.md

1. 五种 RuleManager 的骨架对比
2. 差异点：系统采样 / 断路器 / 参数统计
3. 规则动态更新的完整链路
4. 规则管理作为数据源推送的入口
