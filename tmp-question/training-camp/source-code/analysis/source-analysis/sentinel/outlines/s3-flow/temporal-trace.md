# S-3 Flow 流控域 — 时空溯源

- `0.1.0 (c92fea5d)`：FlowRule / FlowSlot / FlowRuleChecker / DefaultController 与 LeapArray 主骨架已存在。
- `a2b91a90`：LeapArray 及相关统计类的构造参数与时间单位重构，窗口底座逐步稳定。
- `044cdbb1 (#568)`：引入 occupy 机制，为优先级请求增加 future bucket 与借位统计。
- `08f2f71f (#220)`：加入 warm-up with rate limiting，形成第四种组合 controller `WarmUpRateLimiterController`。
- `d4d63fad`：从 FlowRule 中抽出 `FlowRuleChecker`，规则模型与运行时判定解耦。
- `b215e878` / `a6534e5b`：集群模式规则与 checker 的分支逐步补齐。
- `0176f0ea (#723)`：`LeapArray` 前窗口计算统一使用 `calculateTimeIdx`。
- `d5eb5f47 (#1700)`：预计算 `intervalInSecond`，减少重复换算。
- `e34d5527`：RateLimiter controller 精度改进并支持更高 maxQps。

## 结论

S-3 的演进不是简单增加“更多限流算法”，而是三条线并行：规则模型与 checker 解耦、controller 行为扩展、滑窗与 future bucket 底座增强。