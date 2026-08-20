# Pass 2 闭环笔记 Q9: 规则更新时断路器如何重建

## 验证过程

- `DegradeRuleManager` 同时维护 `RuleManager<CircuitBreaker>` 和 `RuleManager<DegradeRule>`；property listener 收到配置后先构建断路器，再从断路器反推规则 map (`DegradeRuleManager.java:40-45, 207-219`)。
- 构建每条新规则时，manager 会按 resource 查找旧断路器；如果 `rule.equals(cb.getRule())`，复用旧断路器，否则创建新实现 (`DegradeRuleManager.java:139-153`)。
- 因此旧式规则内容不变时，断路器状态和统计窗口得以保留；规则内容发生变化时，新的断路器从 CLOSED 开始。
- `DefaultCircuitBreakerRuleManager` 也有 `getExistingSameCbOrNew`，但默认规则的资源名是 `"*"`；reload 时会为已经缓存过的具体 resource 重建 breaker 列表 (`DefaultCircuitBreakerRuleManager.java:183-223`)。因此它不能简单等同于旧 manager 的按 resource 精确复用，默认规则的缓存/重建语义应单独看。
- 旧式规则配置具有优先级：`DefaultCircuitBreakerSlot` 会先检查 `DegradeRuleManager.hasConfig(resource)`，有旧式规则时不启用默认规则。

## 结论

规则更新不是简单替换参数：旧式 resource-specific 规则内容不变时复用旧断路器，保留 CLOSED/OPEN 状态与滑窗统计；规则内容发生变化时创建新断路器，状态从 CLOSED 开始。默认 `"*"` 规则由另一套 manager 为已缓存资源重建，不能套用完全相同的复用结论。规则 identity 决定状态 continuity，但 identity 的查找范围取决于 manager。
