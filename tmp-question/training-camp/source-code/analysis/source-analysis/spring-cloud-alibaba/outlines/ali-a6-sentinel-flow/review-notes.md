# ALI-A6 Sentinel 三路限流 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "SentinelInvocationHandler (JDK 动态代理 + SphU.entry + FallbackFactory 降级)" 未提资源命名 | 大纲补: 资源名 `METHOD:url+path` (L103-104), 与 RestTemplate 路同构 |
| 2 | 规划未提 SentinelBeanPostProcessor 的注解发现双路径 | 大纲补: StandardMethodMetadata / ResolvedFactoryMethod (L81-92, issue#3329) |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "Feign 路也有 host 级资源" | 实测只有 `METHOD:url+path` 单级 (SentinelInvocationHandler:103-104) — RestTemplate 才双级 — 三路粒度不对称精确化 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "SentinelWebInterceptor 在 SCA 仓库" | 09 审计确认是 sentinel 仓库类 (adapter.spring.webmvc_v6x) — SCA 只注册 + 组装配置 (三选 BlockExceptionHandler) |

## 审 4: 跨项目概念转移 — 0 (与 SCC-10 对照已验证)

## 审 5: 覆盖率 — 0 缺漏 (注解发现/强校验/动态注册/双 entry/Feign/Web/常量 7 面全覆盖)

## 审 6: 跨层一致性 — 0 (harness 15/15)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| 第 7 步 urlCleaner 断言失败: 第 6 步残留 flow 规则 | harness 状态未清理 — 补 clear (真实: 规则是全局的, 测试需隔离) |
| "feign exit recorded" 断言不符源码 | blocked 时 entry=null, finally 守卫跳过 exit — 修正断言验证 null-entry 语义 (L136-141 源码 `if (entry != null)`) |

## 结论: 4 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | BPP:68-70 (issue#3329+RT 过滤) + 199 (add(0)) / Interceptor:60-63 (资源名) + 80-84 (双 entry) + 122-141 (degrade→fallback/flow→blockHandler 分流) / SentinelFeign:68-72 (锁死) / InvocationHandler:94 (HardCodedTarget) + 103-104 (资源名同构) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | 资源命名 METHOD:url 三路同构; fallback/blockHandler ↔ SCC-10 run+fallback 对照; FeignClientFactory ↔ SCC-13 | 通过 ✅ |
| 3 | 数字自洽 | 三路/双 entry/4 参签名/5 种返回全一致 | 通过 ✅ |

## 四轮 REVIEW (2026-08-16, 全量脚本复验)

文字锚 (#7) 命中修复: 本文档域引用类补行号 (grep 实证, 见 HANDOFF §六 第 6 行); 锚点范围修正见全局记录。
