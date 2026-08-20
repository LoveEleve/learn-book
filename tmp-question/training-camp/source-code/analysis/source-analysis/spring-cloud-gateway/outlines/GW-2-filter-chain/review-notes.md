# GW-2 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 引用**内容**逐一验证 | 7/8 一次命中 (filter 递归 L153-166 逐字/DEFAULT normalize L107-115 逐字/ConfigurationService normalizeProperties L138-141 + doBind L148-149/Backoff.exponential L221-222); **1 处修正**: onApplicationEvent 实际 **L97-101** (原 L89-93 为 loadFilters @Order 区) |
| 2. 数字穷举 | 39 种工厂/3 种 ShortcutType/Retry 字段 9 个 | ✅ 实证 |
| 3. 代码块逐字 | "return Mono.empty(); // complete" (L164)/"Shortcut Configuration Type GATHER_LIST must have shortcutFieldOrder of size 1" (L135) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← GW-1+GW-3 / → GW-5 | ✅ |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16, 多轮至收敛)

**R1 全量回源 — 5 处行号修正**: loadFilters **L79-93** (原 L82-101)/GatewayFilterAdapter **L81** (原 L84)/instanceof Ordered **L82-84** (原 L87-89)/@Order **L86-89** (原 L91-94)/index **L134** + filters **L136** (原 L133-134) — outline + pass2-q1 + KP 同步。

**R2**: 5/5 闭环全含被放弃+跨域 ✅
**R3**: 前置 GW-1+GW-3 (均 < GW-2 序) 合规, 引出 GW-5 ✅
**R4 横切缺口修正**: **WeightCalculatorWebFilter 在 GW-2 域零落点** — 它是 WebFilter (链前) 非 GatewayFilter (链中), GW-3 的"预计算"引用悬空 → **GW-2 补 §2.5 WebFilter 前置面** (预计算 → WEIGHT_ATTR → GW-3 谓词消费) + GW-3 引用同步
**R5 反写测试**: 6 节 (场景/关键设计/被放弃) 可写 ✅

**R6 数字穷举**: **GlobalFilter 实际 18 个** (原 15 — 之前 head -15 截断漏 WebClientHttpRoutingFilter/WebClientWriteResponseFilter/WebsocketRoutingFilter/GlobalLocalResponseCacheGatewayFilter 等) — 修正 4 文件; 39 种工厂/3 种 ShortcutType ✅
**R7 交叉引用双向一致性**: GW-2→GW-1 (q3 事件面/q5 DSL/容错同源) + GW-2→GW-3 (q3 预计算/同体系) + GW-1→GW-2 (工厂族) + GW-3→GW-2 (§2.5 前置面) 全一致 ✅
**R8 负面空间 6 条 + 代码块逐字** (Mono.empty L161/GATHER_LIST 断言 L135/Backoff L221) ✅

**收敛判定 (07)**: R7+R8 连续两轮零新发现 → 审查穷尽。累计本轮: R1 行号 5 处 + R4 WebFilter 缺口 + R6 数字 15→18。

## 发现与修正 (第一轮)

1. **onApplicationEvent 行号修正**: L89-93 → **L97-101** (outline + pass2-q3 + KP 同步)。
2. **completeness 30 问**: 4 处 ⚠️ → 2 处补大纲 (异常传播/Binder 校验失败), 2 处写作展开。
3. **教训执行**: 5/5 闭环全含被放弃方案+跨域 (GW-3 前向引用教训: 前置只声明 GW-1+GW-3, 无违规)。
4. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 1 处行号修正; 无机制性错误。**达到合格标准**。
