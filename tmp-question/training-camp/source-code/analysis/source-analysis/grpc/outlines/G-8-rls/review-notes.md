# G-8 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 10 处引用**内容**逐一验证 | 8/10 一次命中 (核心注释 L84-85 逐字/锁注释 L108 逐字/OV 时间线 L695-700/createOrGet L689-691/AdaptiveThrottler 参数 L45-47 逐字/BackoffPolicy L806/反射 L55-59 逐字); **2 处修正**: rlsStub 实际 **L130** (原 L128 是 RlsLbHelper)、fallbackChildPolicyWrapper 实际 **L136** (原 L137 空行) |
| 2. 数字穷举 | HISTORY 30s/PADDING 8/RATIO 2.0f/minEviction 5s | ✅ 全部实证 |
| 3. 代码块逐字 | "Every single request is routed by the server's decision" (L85)/"All cache status changes (pending, backoff, success) must be under this lock" (L108)/"Dependency for 'io.grpc:grpc-rls' is missing" (L59) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← G-4+G-7 / 悬念收官 (8 域汇聚) | ✅ |
| 6. 五维检查 | 全 ✅ | ✅ |

## 发现与修正

1. **rlsStub/fallback 行号修正**: L128→130, L137→136 (outline + pass2-q1)。
2. **G-7 教训执行**: Pass 1 标记 5 问, Pass 2 **5/5 全闭环** (清单对照确认无遗漏); 闭环全含被放弃方案+跨域; 引用内容写作时逐字验证 (仅 2 处行号偏差)。
3. **completeness 26 问**: 1 处 ⚠️ (#17 LRU 容量记账) 声明写作展开。
4. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 2 处行号修正; 无机制性错误。**达到合格标准**。
