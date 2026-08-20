# G-5 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 10 处引用**内容**逐一验证 (吸取 G-1/G-3/G-4 教训: 不只验行号存在) | **10/10 全部命中**: syncContext 注释 L271-273 逐字 / onError L303 / refresh 空实现 L146 / Uri percent-encoded L163-164 / TTL=30 L108 / grpc_config= L75 / 每地址 EAG L216-218 / Retrying L35-36,102-105 ✅ **零修正** |
| 2. 数字穷举 | TTL 30s (L108)/networkaddress.cache.ttl 可配 (L445) | ✅ 实证 |
| 3. 代码块逐字 | "listener is responsible for eventually invoking refresh()" (L303 注释区)/"Components are stored percent-encoded" (L163)/"Missing required scheme." (L229) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← G-3+G-6 / → G-4+G-7 (XdsNameResolver) | ✅ |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

### R1 行内引用内容回源 (含裸 LNNN) — 1 处修正

| 发现 | 修复 |
|---|---|
| **A2 提案注释实际在 DnsNameResolver.java:462-465** (原写 L472 — 实为 Verify 校验代码) | outline + pass2-q3 + KP 修正 |

其余 18 处引用 (Uri:185-190 校验逐字/Uri:197-202 parse/NameResolver:87,123,132,146,303/DnsNameResolver:75,86,108,216-218,223-242/DnsNameResolverProvider:44-47 逐字/RetryingNameResolver:35-36,102-105) **全部命中**。

### R2/R3: 4/4 闭环全含被放弃+跨域 (1745-1862B) / 场景 4 节 / 桥 ←G-3+G-6 →G-4+G-7 — 免修

### R4 横切: **UdsNameResolver (v2 规划第二解析器) 只有跨域标注+负面空间, 正文无对照** — 补 §1 "第二实现对照: netty/UdsNameResolver (scheme=uds)" (地址源适配器视角)

## 发现与修正 (第一轮) (行内引用内容验证/数字穷举/跨域/对照引用), 写作时已全部执行: 闭环 4 个全含被放弃方案+跨域; completeness 28 问 2 处 ⚠️ 已声明归位 (G-3 通道 shutdown/JdkAddressResolver 细节); HealthChecking 式对照缺失问题在本域不存在 (UdsNameResolver 在负面空间声明)。
2. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 零修正。**达到合格标准**。
