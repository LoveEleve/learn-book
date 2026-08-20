# ALI-A9 Seata 分布式事务 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划列 6 类 (SeataFeignRequestInterceptor/SeataFeignClientAutoConfiguration/SeataRestTemplateInterceptor/SeataRestTemplateAutoConfiguration/SeataHandlerInterceptor/SeataHandlerInterceptorConfiguration) | 实测 **8 文件** (多 SeataFeignBuilderBeanPostProcessor + SeataRestTemplateInterceptorAfterPropertiesSet) — 规划遗漏 2 个核心装配类, 大纲补 |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "RestTemplate 拦截器直接 add" | 实测 **复制列表 + 重设** (AfterPropertiesSet:32-36, ArrayList 包装后 setInterceptors) — 不可变保护精确化 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "AfterPropertiesSet 是辅助类" | 实测是关键装配: 遍历所有 RestTemplate 全量注入 — 职责重估 |

## 审 4: 跨项目概念转移 — 0 (RootContext 引用已验证 @see)

## 审 5: 覆盖率 — 0 缺漏 (透传三路/收尾校验/Retryer 接管/装配面/可观测 5 面全覆盖)

## 审 6: 跨层一致性 — 0 (harness 13/13)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| 初版 preHandle 模拟条件与源码 (xid 空 && rpcXid 非空) 语义有偏差 (isBlank vs isEmpty) | 修正为 isBlank 语义 — 与源码 L45-46 对齐 |
| 新增 full-chain 断言 | 三路联动实证 (feign 出站 → web 入站) — 补足单一断言盲区 |

## 结论: 3 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | FeignInterceptor:32-37 (空 xid return+header) / Handler:45-52 (bind 条件) + 59-76 (unbind+校验回绑) / BPP (Retryer NEVER_RETRY) / AfterPropertiesSet (全量注入) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | RootContext ThreadLocal ↔ SCC-2 ThreadLocal 缓存对照; 三路透传 ↔ A6 三路限流同构对照 | 通过 ✅ |
| 3 | 数字自洽 | 三路/八文件/KEY_XID 单常量全文一致 | 通过 ✅ |

## 四轮 REVIEW (2026-08-16, 全量脚本复验)

文字锚 (#7) 命中修复: 本文档域引用类补行号 (grep 实证, 见 HANDOFF §六 第 6 行); 锚点范围修正见全局记录。
