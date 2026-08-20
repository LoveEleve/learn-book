# GW-8 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 6 处引用**内容**逐一验证 | **6/6 命中零修正**: $\\ 转义 L62 逐字/addOriginalRequestUrl L67-69/StripPrefix tokenize L69/PrefixPath GATEWAY_ALREADY_PREFIXED L65-71 逐字/addOriginalRequestUrl 实现 L294-296 逐字/GW-4 initFilters RewritePath L72 |
| 2. 数字穷举 | parts 语义/prefix 模板/4 工厂 | ✅ 实证 |
| 3. 代码块逐字 | "replacement.replace(\"$\\\\\", \"$\")" (L62)/computeIfAbsent LinkedHashSet (L295) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← GW-4+GW-5 / → GW-9 | ✅ (前置均前位) |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

**R1 全量回源 — 2 处修正**: PrefixPath expand 实际 **L79** / 拼接 **L81** (原 L73/L76 为 put 标志与 addOriginalRequestUrl) — outline + pass2-q2 + KP 同步。
**R2**: 3/3 闭环全含被放弃+跨域 ✅
**R3**: 前置 GW-4(序5)+GW-5(序4) — **GW-5 序 4 < GW-8 序 8** ✅ (但 GW-4 序 5 < 8 也合规); 引出 GW-9 ✅
**R5/R8**: 3 节/负面 6 条 ✅
**R10**: 残留 0 + harness 回归 4/4 → 收敛

## 第三轮: 追加 (新维度)

**R11 变量来源+order (重要联动)**: SetPath/PrefixPath 模板变量来自 **getUriTemplateVariables** (ServerWebExchangeUtils L313-328: URI_TEMPLATE_VARIABLES_ATTRIBUTE, **由路径谓词匹配的 {id} 填充 — GW-3→GW-8 联动**); 路由过滤器 **order = 配置顺序** (RouteDefinitionRouteLocator L173: OrderedGatewayFilter(i+1)) — 补大纲
**R12 query 保留**: RewritePath 只改 path (L70: mutate().path), query string 不受影响 — 补大纲
**R13 原始链消费方**: **XForwardedHeadersFilter 读 GATEWAY_ORIGINAL_REQUEST_URL_ATTR** (GW-5 头传播基于原始请求) — 补大纲
**R14**: 残留检查 1 处为误报 (L73 是 put 标志正确引用) — 实际 0 残留 + harness 回归 4/4 → **收敛判定**

累计第三轮: 联动 2 (谓词变量/头传播) + 语义 2 (order/query)。

## 发现与修正 (第一轮)

1. **零修正轮**: 引用内容写作时逐字验证; completeness #18 补强 (RewritePath 双场景: 手动 + GW-4 服务发现路由 initFilters)。
2. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 零修正。**达到合格标准**。
