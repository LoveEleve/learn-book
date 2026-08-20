# G-6 全视角提问验证 (completeness-questions)

> 🟡 B 域: 30 问 / 5 身份。每问标注大纲覆盖 (✅ / ⚠️ / ❌)。

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 双模式 | retryPolicy 和 hedgingPolicy 能同时配吗? | ✅ §1 互斥 |
| 2 | 开发者 | 双模式 | commit 后还在跑的对冲流会怎样? | ✅ §1 CANCELLED_BECAUSE_COMMITTED |
| 3 | 开发者 | 缓冲 | perRpcBufferLimit 默认值? | ✅ 已补默认 1M (ManagedChannelImplBuilder.java:120) |
| 4 | 开发者 | 决策 | maxAttempts 包含首次尝试吗? | ✅ §3 attempt+1 |
| 5 | 开发者 | 对冲 | hedgingDelayNanos 从哪个流开始算? | ✅ §4 定时错开 |
| 6 | 开发者 | 退避 | jitter 是加还是乘? | ✅ §5 uniform(±0.2c) |
| 7 | 开发者 | pushback | 服务端 pushback 负值语义? | ✅ §6 禁止重试 |
| 8 | 开发者 | 决策 | transparent retry 消耗 attempt 吗? | ✅ §3 不消耗 |
| 9 | 架构师 | 双模式 | Retry 与 Hedging 的本质策略差异? | ✅ §1 赌失败 vs 赌延迟 |
| 10 | 架构师 | 缓冲 | 超限为什么选 commit 而非丢弃? | ✅ §2 优雅降级 |
| 11 | 架构师 | 决策 | throttle 解决什么问题? | ✅ §3 重试风暴 |
| 12 | 架构师 | 对冲 | 为什么定时错开而非全开? | ✅ §4 流量放大 |
| 13 | 架构师 | 退避 | 抖动为什么必要? | ✅ §5 thundering herd |
| 14 | 架构师 | pushback | 服务端为什么需要否决权? | ✅ §6 粒度不足 |
| 15 | SRE | 决策 | 线上重试风暴怎么防? | ✅ §3 throttle |
| 16 | SRE | 缓冲 | 重试调用占多少内存? | ✅ §2 双层限额 |
| 17 | SRE | 退避 | 指数退避到 2min 意味着什么? | ✅ §5 封顶 |
| 18 | SRE | pushback | 服务端怎么告诉客户端别重试? | ✅ §6 |
| 19 | SRE | 双模式 | 重试与幂等的关系? | ✅ 负面空间 #6 声明 |
| 20 | 学生 | 双模式 | "透明重试"透明在哪? | ✅ §1 无用户代码 |
| 21 | 学生 | 缓冲 | 重试时消息从哪来? | ✅ §2 缓冲重放 |
| 22 | 学生 | 决策 | 什么状态码可重试? | ✅ 无默认集 — service config 必填 (ServiceConfigUtil.java:185), 大纲 §3 已补 |
| 23 | 学生 | 对冲 | 对冲会浪费流量吗? | ✅ §4 放大 |
| 24 | 学生 | 退避 | 重试间隔是固定的吗? | ✅ §5 指数 |
| 25 | 学生 | pushback | pushback 是什么? | ✅ §6 |
| 26 | 研究者 | 双模式 | vs Spring Retry/Resilience4j 对比? | ✅ 对照声明 |
| 27 | 研究者 | 缓冲 | 缓冲记账 vs 引用计数方案? | ✅ §2 记账语义 |
| 28 | 研究者 | 决策 | 客户端节流 vs 服务端 pushback 分工? | ✅ §6 双向 |
| 29 | 研究者 | 对冲 | 对冲的适用场景边界? | ✅ §4 慢请求 |
| 30 | 研究者 | 退避 | 抖动分布选择 (uniform vs 全抖动)? | ⚠️ 未讨论分布选择 |

⚠️ 3 项 (#3/#22/#30) → 处理:
- #3 perRpcBufferLimit 默认值 → 补大纲 §2 一句 (PerRpcBufferLimit.DEFAULT, 需要 grep 验证数值)
- #22 默认可重试状态码集合 → 补 §3 一句 (需要 grep RetryPolicy 默认/DEFAULT_RETRYABLE_STATUS_CODES)
- #30 抖动分布 → 写作时展开 (研究性话题, 声明即可)
