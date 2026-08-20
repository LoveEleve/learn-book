# G-3 review-notes — 六层深审 + 07 全量维度审查记录

> 2026-08-16 | 锚点回源全部重新 grep, 零发现=不合格原则执行

## 第一轮: 六层深审 (交付时)

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 12 个唯一锚点, 抽查 10 处命中 | ✅ |
| 2. 数字穷举 | idle 默认 30min (ManagedChannelImplBuilder.java:103 实证) / MIN 1s (L108) / ±100 年 (Deadline L40-43) | ✅ |
| 3. 代码块逐字 | "a racing due timer..."/"must check queue again here"/"overwrite the onClose() details" | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← G-1+G-2 / → G-4+G-5+G-6 | ✅ |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16 用户要求按方法论再深审)

### R1 锚点回源 + 行内引用准确性

12/12 锚点命中, 但**行内引用 3 处偏差** (G-1 Codec.GZIP 教训: 必须验证引用内容, 不只行号存在):

| 发现 | 严重度 | 修复 |
|---|---|---|
| **Context.java:169 是注释** ("code stealing the ability to cancel arbitrarily"), keyValueEntries 字段实际在 **L180** | 高 (字段行号错误) | 3 文件修正 (outline/KP/pass2-q5) |
| **DelayedClientCall:216 是 if 条件**, DelayedListener 包装实际在 **L217** | 中 | outline + pass2-q7 修正 |
| **ClientCallImpl:212-215 是 applyMethodConfig**, ContextRunnable 回调实际在 **L206-213** (runInContext L209-211, closeObserver L210, execute L212) | 高 (引用内容错) | outline + pass2-q4 修正 |

### R2 内容深度

8/8 闭环全部含"被放弃的方案"+跨域标注 (1835-2325 字符) — G-1/G-2 教训执行到位, 无需修复。

### R3 结构完整性

桥/负面空间/开篇/核心悬念全 ✅ — 无需修复。

### R4 横切关注点

| 横切面 | 发现 | 修复 |
|---|---|---|
| **消息帧消费链** | **MessageDeframer 在域清单但 outline 无落点** (格式面 G-1 §7 已讲, 但客户端接收链 [NettyClientHandler → Http2ClientStream → MessageDeframer → Listener] 未提) | **§8 补"消息接收链落点"** — 与 G-1 桥接不重复 |
| 取消传播链 (Context→cancelled→stream.cancel) | §4+§5 覆盖 (7 处 cancel 引用) | ✅ |
| 超时链 (Deadline→调度器→cancel) | §6 覆盖 | ✅ |
| 缓冲链 (PendingCall→DelayedClientCall) | §3+§7 覆盖 | ✅ |

### 反写测试

14 个锚点 + 8 节 (场景/关键设计/被放弃方案 16 处) → 可写 ✅

## 结论

两轮审查累计修复: **行内引用 3 处** (Context:169→180 / Delayed:216→217 / CallImpl:212-215→206-213)、**MessageDeframer 落点补 §8**。机制性错误 0 残留。**达到合格标准**。

