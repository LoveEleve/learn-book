# GW-9 review-notes — 六层深审 + 07 全量维度审查记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 第一轮: 六层深审 (交付时)

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 6 处引用**内容**逐一验证 | **6/6 命中**: setCorsConfigurations L45 (AutoConfig)/onApplicationEvent L86/metadata.cors L126/X-XSS "1 ; mode=block" L41 逐字/HSTS L51 逐字/X-Frame "DENY" L61 逐字/withDefaults L107 + fallback L243-248 逐字 |
| 2. 数字穷举 | 7+ 头/默认值 3 项实证 | ✅ |
| 3. 代码块逐字 | "sensible defaults are applied" (L39)/"max-age=631138519" (L51) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← GW-1+GW-3 / 收官 (悬念收束) | ✅ |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

**R1 全量回源 — 1 处修正**: CorsGatewayFilterApplicationListener 的 setCorsConfigurations 实际 **L104** (原 L110 为 getPathPredicate 注释区) — outline + pass2-q1 + KP 同步。
**R2**: 3/3 闭环全含被放弃+跨域 ✅
**R3**: 前置 GW-1(序1)+GW-3(序2) < GW-9(序9) 合规 ✅
**R5/R8**: 3 节/负面 6 条 ✅
**R10**: 残留 0 + harness 回归 3/3 → **收敛**
**harness 语义修正 (2 次)**: ① lookup 需路径模式匹配 (/** 前缀) 非精确匹配 ② /** 兜底不得覆盖具体匹配 (最具体优先) — 修正后 3/3

## 发现与修正

1. **交付遗漏修正**: review-notes 初交付时漏写 (与 outline 同批), 收官时补全。
2. **harness 语义**: 路由级 CORS 的路径绑定 = 模式匹配 (最具体优先), 非精确 key 查找。

## 结论

大纲机制全部有源码实证; 1 处行号修正; 无机制性错误。**达到合格标准**。
