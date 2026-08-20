# S-7 规则管理域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 范围: `slots/block/authority/` + `slots/system/` + `property/` + `RuleManager`/`RuleConstant`

## 核心骨架

- 黑白名单: `AuthoritySlot` / `AuthorityRule` / `AuthorityRuleChecker` / `AuthorityRuleManager` / `AuthorityException`
- 系统保护: `SystemSlot` / `SystemRule` / `SystemRuleManager` / `SystemBlockException` / `SystemStatusListener`
- 规则管理基础设施: `RuleManager` / `RuleConstant`
- property 基础设施: `SentinelProperty` / `DynamicSentinelProperty` / `NoOpSentinelProperty` / `PropertyListener` / `SimplePropertyListener`

## Pass 1 观察

- `RuleManager<R>` 是统一规则容器，支持简单规则 + 正则规则，用 `regexCacheRules` 缓存正则匹配结果以降低发布规则时的性能损耗。
- `SentinelProperty` 是发布-订阅接口，`updateValue` 只在值变化时通知 listener。
- `DynamicSentinelProperty` 用 `CopyOnWriteArraySet` 存 listener，`addListener` 时立即 `configLoad` 当前值。
- `AuthorityRuleManager` / `SystemRuleManager` 大概率复用 `RuleManager` + property/listener 模式。
- `SystemRuleManager` 还涉及系统状态采样（CPU/负载/RT/线程/QPS），`SystemStatusListener` 可能是采样回调。

## 标记问题

1. `RuleManager` 如何区分简单规则与正则规则？`regexCacheRules` 的作用？
2. `SentinelProperty` 的发布-订阅如何与 `RuleManager` 联动？
3. `AuthorityRule` 的 limitApp 与策略(白名单/黑名单)如何判定？
4. `AuthorityRuleChecker` 如何匹配 origin？
5. `SystemRule` 的四种指标(CPU/RT/线程/QPS)如何采样与判定？
6. `SystemRuleManager` 如何维护系统状态采样？
7. `SystemSlot` 与 `SystemRuleManager` 的分工？
8. `RuleConstant` 定义了哪些常量？
9. `NoOpSentinelProperty` / `SimplePropertyListener` 的用途？
10. 各 RuleManager 的 `register2Property` / `loadRules` 是否统一模式？
