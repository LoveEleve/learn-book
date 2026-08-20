# S-7 规则管理域 — 审查记录

## Pass 1 / Pass 2

- 确认 `RuleManager<R>` 是统一规则容器，支持简单规则 + 正则规则，`regexCacheRules` 缓存正则匹配结果。
- 确认 `SentinelProperty` 发布-订阅底座，`updateValue` 只在值变化时通知 listener。
- 确认 `DynamicSentinelProperty` 用 `CopyOnWriteArraySet` 存 listener，`addListener` 时立即 `configLoad`。
- 确认 Authority 黑白名单判定：origin 精确匹配 limitApp，一资源一规则。
- 确认 SystemRuleManager 系统状态采样与 IN 流量判定，负载用 BBR 二次确认。
- 确认五种 RuleManager 共享统一骨架，差异只在规则编译逻辑。

## 深审修正

1. 上篇锚点修正：`RuleManager.getRules` 在 87，`setRules` 在 172，`DynamicSentinelProperty.updateValue` 在 48。
2. 中篇锚点核对：`AuthorityRuleChecker` 在 25-52，`SystemRuleManager` 采样在 95-99，判定在 240-285，BBR 在 287-293。
3. 下篇锚点修正：`AuthorityRuleManager` 静态块在 48-50，`register2Property` 在 52-67，`loadRules` 在 69-70，`SystemRuleManager` 采样在 95-99，`configUpdate` 在 186-211，`ParamFlowRuleManager` 清理在 101-125。

## 三篇正文

- `01-rule-manager.md`：RuleManager 容器、正则缓存、property 底座、统一骨架、RuleConstant
- `02-authority-system.md`：Authority 黑白名单、System 状态采样、BBR、IN 流量判定
- `03-rule-pattern.md`：五种 RuleManager 骨架对比、动态更新链路、数据源推送入口

## 遗留

1. `SystemStatusListener` 的具体采样实现（如何取 CPU/负载）未深入，属于系统相关，非纯 Java 侧逻辑。
2. BBR 公式 `maxSuccessQps * minRt / 1000` 只描述了行为，未逐项推导其与标准 BBR 理论的对应。
3. `ParamFlowRuleUtil.buildParamRuleMap` 的规则解析细节未单独展开。
4. 各 RuleManager 的正则缓存是 `RuleManager` 统一实现，各规则类型共享，未逐一验证所有规则类型都正确使用了 `isRegex`。
