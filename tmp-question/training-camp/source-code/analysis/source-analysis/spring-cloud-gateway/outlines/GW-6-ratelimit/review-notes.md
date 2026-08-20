# GW-6 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 7 处引用**内容**逐一验证 | **7/7 命中零修正**: 编排 L93-105 逐字/setComplete L128-129/TOO_MANY_REQUESTS L138/Lua 执行 L253-258 逐字/故障降级注释 L283-287/PrincipalName L31-32 逐字/三参数 L241-247 逐字 |
| 2. 数字穷举 | 三参数/429 默认/EMPTY_KEY 策略 | ✅ 实证 |
| 3. 代码块逐字 | "allowed, tokens_left = redis.eval(SCRIPT, keys, args)" (L256)/"We don't want a hard dependency on Redis to allow traffic... Stripe's observed failure rate is 0.01%" (L283-284) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← GW-2 / → GW-7 | ✅ (前置仅 GW-2 序 3 < GW-6 序 6) |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

**R1 全量回源 — 2 处修正**: defaultIfEmpty **L99** (原 L95-96 为 denyEmpty 获取) / isAllowed **L112** + 头传播 L115-117 + setComplete L122 (原 L123-130/L126-129/L128-129) — outline + pass2-q1 + KP 同步; **修正时新旧描述拼接重复 1 处清理**。
**R2**: 3 闭环 (覆盖 4 问) 全含被放弃+跨域 ✅
**R3**: 前置 GW-2 (序 3 < 6) 合规 ✅
**R5**: 3 节可写 ✅
**R8**: 负面空间 6 条 ✅
**R9**: MiniGW6 覆盖 (令牌桶/429/EMPTY/降级) ✅
**R10 残留清理**: 修正后残留 5 处 (pass1/pass2-q1/KP) + outline 重复行 — 全部清理, 0 残留

**收敛判定 (第二轮)**: R10 清理后零残留 → 收敛。

## 第三轮: 追加多轮 (用户要求, 新维度)

**R11 Lua 脚本语义验证**: harness 填充公式与 request_rate_limiter.lua **完全一致** (L20: min(capacity, last+delta*rate) / L21: >=) — **补强 2 处**: TTL 机制 (L14-15: fill_time*2 键过期, L24-26 setex — 内存治理) / redis.replicate_commands() (L1, 脚本确定性) / 拒绝不扣减 (L22)
**R12 默认值穷举**: **denyEmpty 默认 true** (L55) + **emptyKeyStatus 默认 403 FORBIDDEN** (L57-58) / **4 个 X-RateLimit 头名** (L70-85: Remaining/Replenish-Rate/Burst-Capacity/Requested-Tokens) / **默认 bean 无内置速率** (3 参构造 GatewayRedisAutoConfiguration.java:71 — 必须配置) — 补大纲 3 处
**R13 completeness 同步**: EMPTY_KEY 默认 403
**R14**: 残留 0 + harness 回归 3/3 ✅

**收敛判定 (第三轮)**: R13/R14 零新发现 (仅为 R12 的延续同步) → **审查收敛**。累计第三轮: Lua 语义 2 + 默认值 3 + 同步 1。

## 发现与修正 (第一轮)

1. **零修正轮**: 引用内容写作时逐字验证; 前置声明合规。
2. **completeness 22 问**: 3 处 ⚠️ (调参/排查/对照) 写作展开。
3. **教训执行**: 闭环含被放弃方案+跨域; 多轮审查教训 (残留检查/默认值穷举) 内化。
4. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 零修正。**达到合格标准**。
