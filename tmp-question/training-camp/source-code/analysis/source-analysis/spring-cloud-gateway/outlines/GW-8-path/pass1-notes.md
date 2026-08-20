# GW-8 Pass 1 扫描笔记 — 路径重写

> 日期: 2026-08-16 | 版本: 4.3.2 | 🟡 B | 模块: filter/factory/ (4 种: RewritePath 116/SetPath 100/StripPrefix 111/PrefixPath 114)

## 继承树/调用图

```
4 种路径工厂 (AbstractGatewayFilterFactory):
  ├── RewritePath (38): regexp/replacement → Pattern.matcher.replaceAll (L67-69)
  │     └── $\\ → $ 转义 (L62) + addOriginalRequestUrl (L66)
  ├── SetPath (40): 模板路径 (UriTemplate) → GATEWAY_REQUEST_URL_ATTR (L71)
  ├── StripPrefix (40): parts → 去前 N 段 (L69-75)
  └── PrefixPath (43): prefix → UriTemplate.expand + GATEWAY_ALREADY_PREFIXED 防重复 (L67-71)
共同: addOriginalRequestUrl (原始 URL 保留) + GATEWAY_REQUEST_URL_ATTR 更新
```

## 基本元素分解

1. **RewritePath**: 正则替换路径 (regexp/replacement)
2. **SetPath**: 模板设置路径 (UriTemplate + 变量)
3. **StripPrefix**: 去前 N 段
4. **PrefixPath**: 加前缀 (防重复标志)

## 标记问题 (3)

1. **Q1 正则替换**: RewritePath 语义 ($\\ 转义/替换后 GATEWAY_REQUEST_URL 更新)
2. **Q2 前缀/段操作**: StripPrefix/PrefixPath — 防重复 (GATEWAY_ALREADY_PREFIXED) 与模板展开
3. **Q3 共同模式**: addOriginalRequestUrl + 原始 URL 保留语义 (与 GW-4/GW-5 联动)

## 已读测试

- RewritePath/SetPath/StripPrefix/PrefixPath GatewayFilterFactoryTests (factory/ 测试族)
