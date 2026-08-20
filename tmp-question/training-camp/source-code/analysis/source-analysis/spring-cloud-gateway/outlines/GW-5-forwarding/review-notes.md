# GW-5 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 9 处引用**内容**逐一验证 | **9/9 命中零修正**: GATEWAY_REQUEST_URL_ATTR L61+112/请求发出 L133-134/cacheRequestBody L66/order L80/TrustedProxies 注释 L242/HttpClient.create L83/writeAndFlushWith L96-100/CLIENT_RESPONSE_CONN_ATTR L71 |
| 2. 数字穷举 | 5 头/5 开关/order HIGHEST+1000 | ✅ 实证 |
| 3. 代码块逐字 | "Defer committing the response until all route filters have run" (L149 区)/"match xforwarded for against trusted proxies" (L242) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← GW-2+GW-4 / → GW-6/7/8 | ✅ (前置 GW-4 序 5 < GW-5 序 4? — 检查) |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

**R1 全量回源**: 7 带文件名 + 17 裸行号 — 16/17 命中, **1 处修正**: TrustedProxies 警告日志实际 **L115** (原 L99 空行) — outline + pass2-q4 同步。

**R2**: 6/6 闭环全含被放弃+跨域 (1420-1862B) ✅
**R3 前向引用违规修正**: GW-5 前置声明依赖 GW-4 (序 5 > GW-5 序 4) — 但 GW-4 是**链中流程前位** (lb:// 解析在转发前) 非机制依赖 → GW-4 移入"对照 (链中流程前位)" (07 §维度3: 机制依赖才需前置声明)
**R4 交叉引用**: GW-2 链内协作/GW-3 ReadBody+IP 谓词消费 全一致 ✅

**R4 横切**: **响应缓存面 (v4 cache/ 11) 正文零对照** (只有负面空间提及 — GW-4 HealthChecking 教训重演风险) → §6 补"响应缓存对照" (缓存层在回写前介入)
**R5 反写测试**: 6 节/6 关键设计/6 场景 可写 ✅
**R6 数字穷举**: 5 头/5 开关/11 文件 全实证 ✅
**R7 交叉引用**: GW-2/3/4/6 共 6 处一致 ✅
**R8 负面空间 6 条 + 代码块逐字** ("Defer committing the response until all route filters have run") ✅
**R9 文件级覆盖**: 转发家族 6 种 (4 种正文 + 2 种负面空间声明) ✅
**R10 harness 一致性**: MiniGW5 覆盖核心 3 机制 (两阶段/body 缓存/可信代理) ✅

**收敛判定 (07)**: R5-R10 连续零新发现 → 审查穷尽。累计本轮: R1 行号 1 处 (L115) + R3 前向引用 1 处 (GW-4 移对照) + R4 横切 1 处 (响应缓存对照)。

## 发现与修正 (第一轮)

1. **桥链检查**: 前置声明 GW-4 (负载均衡, 序 5) — GW-5 是序 4, **GW-4 在 GW-5 之后**!这是前向引用违规 (GW-3 教训重演?)。但 GW-5 的 lb:// 解析确实在转发前 (GW-4 的 ReactiveLoadBalancerClientFilter 是链前置)。处理: GW-5 的前置声明改为 "GW-4 负载均衡 (lb:// 解析 — 拓扑后位, 对照声明)" 或重排拓扑。**先标记, 收官时统一处理** (GW-4/GW-5 的依赖方向与 GW-1→GW-3→GW-2→GW-5→GW-4 拓扑序冲突: GW-5 序 4 前置依赖 GW-4 序 5)。
2. **completeness 30 问**: 6 处 ⚠️ 全为对照/性能类 (写作展开), 无核心缺失。
3. **教训执行**: 6/6 闭环全含被放弃方案+跨域; 引用内容写作时逐字验证 (9/9 零修正)。

## 结论

大纲机制全部有源码实证; 1 处桥链拓扑待收官处理; 无机制性错误。
