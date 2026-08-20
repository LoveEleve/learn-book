# GW-4 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 8 处引用**内容**逐一验证 | **8/8 命中零修正**: "Invalid host" 注释 L81-84 逐字/合并 L88-97/lb 触发 L99-101/NotFoundException L121-123 逐字/overrideScheme L132-133/reconstructURI L162-163/DelegatingServiceInstance L108-110 |
| 2. 数字穷举 | gateway→SCC 9 文件 (v2) | ✅ |
| 3. 代码块逐字 | "Load balanced URIs should always have a host" (L81-82)/"Unable to find instance for" (L123) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← GW-1+GW-2 / → GW-5 | ✅ (前置均前位) |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

**R1 全量回源**: 4 带文件名 + 12 裸行号 — **2 处修正**: buildRouteDefinition **L107** (原 L108 空行) / SpEL 求值 **L115,125,146** (原 L117-130 为谓词添加区) — outline + pass2-q4 + KP 同步。

**R2**: 4/4 闭环全含被放弃+跨域 (1444-1767B) ✅
**R3**: 前置 GW-1(序1)+GW-2(序3) < GW-4(序5) 合规 ✅
**R5**: 4 节可写 ✅
**R6**: 数字 (9 文件 SCC 依赖) ✅
**R7**: SCC 交叉 11 处 ✅
**R10 交叉引用错位修正 (重要)**: GW-4 引用 SCC-5 — 但 SCC-PLAN 的 **SCC-5 是 @LoadBalanced 客户端**, 策略工厂 (LoadBalancerClientFactory/RoundRobin) 在 **SCC-7 (ReactorLoadBalancer 策略)** → 3 文件修正 (outline/pass2-q3/KP) — 教训: 跨仓库交叉引用必须回 SCC-PLAN 域清单核对域号, 不能凭名称猜测

**R9 harness 一致性**: MiniGW4 覆盖 4 机制 (URL 装配/choose/404/服务发现路由) vs 大纲 4 节 ✅
**R10b**: SCC-13 (NamedContextFactory L60/createContext L130) 确认正确 ✅
**R8/R9/R10b 连续零新发现 → 收敛判定成立** (07: 连续两轮不同维度零发现 = 穷尽)。累计本轮: R1 行号 2 处 + R10 SCC 域号错位 1 处。

## 发现与修正 (第一轮)

1. **零修正轮**: 引用内容写作时逐字验证; 前置声明合规 (GW-1 序1/GW-2 序3 < GW-4 序5 — 之前 GW-5 的前向引用问题在此域不重演)。
2. **SCC 交叉引用原则**: 消费声明以 SCC-PLAN.md 为基准 (SCC-7/13), 不重复探索 SCC 源码 — 并行域协作规范。
3. **completeness 24 问**: 2 处 ⚠️ (SCC 面细节) 声明写作展开。
4. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 零修正。**达到合格标准**。
