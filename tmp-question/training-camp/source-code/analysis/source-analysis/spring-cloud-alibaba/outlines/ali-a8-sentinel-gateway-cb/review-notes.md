# ALI-A8 Sentinel Gateway 限流+断路器 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划将 SentinelSCGAutoConfiguration/SentinelGatewayFilter 归为 SCA 类 | 09 审计确认: 过滤器内核在 **sentinel 仓库** (adapter.gateway.sc), SCA 只注册 (L139-147) — 大纲薄装配精确化 |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "fallback 只有重定向一种" | 实测双模式: fallback-msg-response (自定义响应) + fallback-redirect (L103-126) — 模式数量修正 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "SentinelConfigBuilder 只是配置对象" | 实测 implements ConfigBuilder (SCC-10 契约) + build 默认值 + 内部 Configuration 类 — 契约面精确化 |

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | "规则注入靠外部数据源 (A7)" | 实测断路器规则**构造期 loadRules 自注入** (L70-83), 与 A7 数据源注入是两条独立路 — 修正对照关系 |

## 审 5: 覆盖率 — 0 缺漏 (网关组装/降级双模式/断路器核心/工厂族/装配/注释契约 6 面全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | SCG:58-60 (双条件) + 79-80 (低优先注释) + 103-121 (双模式) + 129-147 (两 Bean @Order) / CB:70-83 (规则注入 loadRules) + 94-104 (BlockException 不 trace 注释) / Factory:42 (computeIfAbsent) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | SCC-10 工厂族契约完整实现; 规则注入 vs A7 数据源路独立 (双路对照修正) | 通过 ✅ |
| 3 | 数字自洽 | 三途径/三分支/HIGHEST_PRECEDENCE 与 -1 全文一致 | 通过 ✅ |

## 四轮 REVIEW (2026-08-16, 全量脚本复验)

文字锚 (#7) 命中修复: 本文档域引用类补行号 (grep 实证, 见 HANDOFF §六 第 6 行); 锚点范围修正见全局记录。
