# G-6 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep, 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 6 个唯一锚点 (🟡 ≥4 ✅) + 行内引用验证 | 抽查 10 处命中; **1 处修正**: CANCELLED_BECAUSE_COMMITTED 取消实际在 **L191** (常量 L64), 原写 L199-202 |
| 2. 数字穷举 | 退避 1s/2min/1.6/0.2 (L38-43 逐字) / **缓冲默认 16M+1M** (ManagedChannelImplBuilder.java:119-120) / **retryableStatusCodes 无默认 — service config 必填** (ServiceConfigUtil.java:185, 禁含 OK L186) | ✅ 全部实证 |
| 3. 代码块逐字 | "Should not provide both retryPolicy and hedgingPolicy" (L146-147)/"grpc-retry-pushback-ms" (L62)/hasPotentialHedging 注释 (L811) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← G-3+G-2 / → G-4+G-5 (RetryingNameResolver 复用退避) | ✅ |
| 6. 五维检查 | 全 ✅ | ✅ |

## 发现与修正

1. **CANCELLED_BECAUSE_COMMITTED 行号修正**: 取消调用在 L191 (非 L199-202), 常量定义 L64 — outline + pass2-q1 已修。
2. **completeness 30 问**: 3 处 ⚠️ → 缓冲默认 1M/16M 补大纲 §2, "无默认状态码集" 修正预设 (retryableStatusCodes 必填) 补 §3 — 这是重要事实 (文档常误称"默认可重试码")。
3. **R1 二轮回源修正**: "Unneeded hedging" 取消实际在 **L509** (原写 L499-501, 实为 freezeHedging 区) — outline + pass2-q4 已修; 其余行内引用 8 处回源命中 (throttle L1075-1079/pushback L1089-1091/缓冲注释 L1407-1410/perRpc 超限 L1429-1430/调度 L512-517/freezeHedging L823-824)。
4. **R4 横切补充**: transparent retry 语义细化 (MISCARRIED 无限次 + 1000 次防护 L941-944 / REFUSED 仅一次 L951-952) 补 §3 — 这是重试与连接生命周期 (G-3/G-4) 的桥。
5. **harness 实测发现 (退避语义)**: MiniG6 初始断言"抖动不超封顶" FAIL — 回源源码证实: **封顶的是无抖动值 (L50), 返回时加抖动 (L51-52) 可小幅超出 maxBackoffNanos** — 修正 harness 断言 + 补 outline §5 细节。这验证了 harness 的费曼价值 (理解错了会被运行打脸)。
6. **教训执行**: 6 闭环全部含"被放弃的方案"+跨域; outline 锚点 file:line 格式。
7. **时空溯源**: 见 temporal-trace.md。

## 结论

两轮审查累计修复: "Unneeded hedging" L509 行号 / transparent retry 语义细化 / (交付时) L191 + 缓冲默认 + 必填状态码集 + 抖动超封顶。机制性错误 0 残留。**达到合格标准**。
