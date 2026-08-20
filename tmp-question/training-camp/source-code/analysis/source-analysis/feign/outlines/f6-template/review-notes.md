# F-6 review-notes — 六层深审记录 (2026-08-15)

## 审法: 探索代理锚点 + 本审抽查 + 极简复现 harness

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "queries LinkedHashMap/headers TreeMap CASE_INSENSITIVE" — RequestTemplate.java:56-57 实证 | 通过 ✅ |
| 2 | 事实 | §1 "模板探测正则 (?<!\\\\)\\?" — L55 实证 | 通过 ✅ |
| 3 | 事实 | §2 "resolve 副本隔离 + query 烧进 uri" — L182-262 (L187/L199-233/L260) | 通过 ✅ |
| 4 | 事实 | §2 "request() 未 resolve 抛" — L287-292 "template has not been resolved." | 通过 ✅ |
| 5 | 事实 | §3 "嵌套花括号字面量" — Template.java:289-300 (Apache CXF 灵感) | 通过 ✅ |
| 6 | 事实 | §4 "语法正则 + 长度上限 10000" — Expressions.java:57, L89-94 (feign.template.expression.maxLength) | 通过 ✅ |
| 7 | 事实 | §4 "正则非法降级字面量" — L138-140 | 通过 ✅ |
| 8 | 事实 | §5 "四模板策略矩阵" — UriTemplate.java:74-76 (REQUIRED/REQUIRED) / QueryTemplate.java:132-150 / HeaderTemplate.java:126-137 (NOT_REQUIRED) / BodyTemplate.java:55-60 (ALLOW_UNRESOLVED) | 通过 ✅ |
| 9 | 事实 | §5 "stripCrlf 头注入防御" — HeaderTemplate.java:193-195 | 通过 ✅ |
| 10 | 事实 | §5 "JSON %7B/%7D 还原" — BodyTemplate.java:62-71 | 通过 ✅ |
| 11 | 事实 | §6 "isEncoded + 分段编码防二次编码" — UriUtils.java:37-45, L111-141 | 通过 ✅ |
| 12 | 事实 | §6 "alreadyEncoded API 已废弃" — RequestTemplate.java:271-278 | 通过 ✅ (代际差异: 13.x 统一幂等编码) |
| 13 | 事实 | §7 "EXPLODED 重复键" — CollectionFormat.java:68-75; 逗号拆解 QueryTemplate.java:206-208 | 通过 ✅ |
| 14 | 事实 | §8 "fragment 切分/拼回" — RequestTemplate.java:464-468, L550-552 | 通过 ✅ |
| 15 | 事实 | §9 "Request 不可变 + requestTemplate() Experimental" — Request.java:36, L506-509 | 通过 ✅ |
| 16 | **代际修正** | 任务提示 requestLine()/build() **13.x 已移除** — 产出入口 request()+url() (RequestTemplate.java:287-292, 543-555) | ✅ 大纲按 13.x 撰写 |
| 17 | 数字 | 锚点密度 ≥8 达标; 关键常量 (10000 上限/RFC 3986 字符集/EXPLODED 默认) 全部双源核对 | 通过 ✅ |

**结论**: 17 项核对 0 修正 (1 项代际差异确认)。harness 验证模板展开语义。

## harness 设计 (MiniTemplate — 模板展开极简复现)

- A. {var} 解析: 简单变量替换
- B. 嵌套花括号: 内层当字面量
- C. URL 编码: 空格/中文 → pct-encode; 已编码值不二次编码
- D. 四位置策略: uri 编码 / header 不编码 / body 保留未解析
- E. CollectionFormat: EXPLODED 重复键 vs CSV 分隔符
