# GW-8 路径重写 — 知识规划 (KP)

> 域级: 🟡 B | 模块: filter/factory/ (4 种: RewritePath 116/SetPath 100/StripPrefix 111/PrefixPath 114)
> 日期: 2026-08-16 | 版本: 4.3.2 | Pass 2 闭环: q1(正则) q2(前缀段) q3(共同模式) — **3/3 全闭环**

## 一、机制提取 (逐源)

### M1 正则替换 (q1)
- RewritePathGatewayFilterFactory (62-71): $\\ 转义 (L62) + replaceAll (L67-69) + addOriginalRequestUrl (L66) + GATEWAY_REQUEST_URL_ATTR 更新 (L71)
- Config: regexp (L44)/replacement (L49)

### M2 前缀/段 (q2)
- StripPrefix (58-75): tokenizeToStringArray (L69) + 跳前 N 段重组 (L71-75); PARTS_KEY (L46)
- PrefixPath (63-85): UriTemplate (L65) + **GATEWAY_ALREADY_PREFIXED 防重复** (L67-71) + expand (L79) + 拼接 (L81)
- SetPath (40-71): 模板设置

### M3 共同模式 (q3)
- addOriginalRequestUrl (ServerWebExchangeUtils 294-296): LinkedHashSet 追加 (原始链)
- GATEWAY_ORIGINAL_REQUEST_URL_ATTR (L111); 变换后 GATEWAY_REQUEST_URL_ATTR 更新

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 正则语义 / M2 防重复幂等 |
| P2 | M3 原始链保留 |

## 三、叙事线

场景: `/api/user/123` 到后端变成什么?读者疑问链: 正则怎么换 (M1) → 前缀/段怎么操作 (M2) → 原始 URL 去哪 (M3)。
