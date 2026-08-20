# ALI-A2 Nacos 配置动态刷新 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划文档把 NacosConfigRefreshEventListener 归入 core 模块 | 实测在 **starter 模块** configdata 包 (spring-cloud-starter-alibaba-nacos-config) — 大纲/PLAN 路径修正 |
| 2 | NacosContextRefresher 类名与 SCC-2 的 ContextRefresher 易混 | 大纲明示: Nacos 版是 **ApplicationReadyEvent 监听 + 长轮询注册**, 与 Commons 的 ContextRefresher (refresh 执行器) 职责不同 |

## 审 2: API/实现路径编造 — 0 (全部行号支撑)

## 审 3: 文件名/目录名推断 — 2 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "NacosPropertySourceRefreshListener 是纯刷新监听" | 双角色: BeanPostProcessor 收集 ConfigurationPropertiesBean + SmartApplicationListener 仲裁换源 |
| 2 | "Smart rebinder 直接读 beans" | 实测反射读私有字段 (fillBeanMap L66-75) — Spring 未开放 API |

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | NacosConfigRefreshEvent 会直接触发 rebind | 实测不触发: 它只是中转事件, 新轨经 RefreshEvent→Commons 链路, 旧轨经 containsBean 仲裁换源 — 与 SCC-8 RefreshEvent 关系精确化 |

## 审 5: 覆盖率 — 0 缺漏 (7 节覆盖: 双轨/仲裁/全链路/Smart/注解/历史/互斥)

## 审 6: 跨层一致性 — 0 (harness 18/18 与大纲数字一致)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| 第 4 项断言原设计引用"refreshCount 驱动 A1 节流" — 初版 harness 未联动 | 修正: 显式断言 refreshCount 递增, 并标注 A1 消费方 (NacosPropertySourceLocator:164) — **跨域联动实证** |
| rebindSpecificBean 中 refreshedSet 只防同 Bean 重入, 不防同 key 多 Bean | 修正断言: 两次同前缀变更只 rebind 一次, 不同前缀各 rebind — 语义精确化 |

## 结论: 5 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | Refresher:86-91 (ready CAS) / 116-117 (computeIfAbsent) / PSRListener:96-99 (containsBean 仲裁) / RefreshEventListener:41-43+52 (转发) / SmartRebinder:68-70 (反射) + 89-91 (双源) + 101-107 (前缀+去重) / BPP:69-71 (order=0) / History:38 (MAX 20) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | 事件链 NacosConfigRefreshEvent→RefreshEvent→(SCC-8)→EnvironmentChangeEvent→rebind 与 SCC-2/8 对齐; 快照写入 ↔ A1 消费闭环 | 通过 ✅ |
| 3 | 数字自洽 | 双轨/双源判断/MAX 20/2021.0.1.1 版本锚全文一致 | 通过 ✅ |
| 4 | 语言错误 | NacosConfigSpringCloudAutoConfiguration:45 注释 "te possibility" (原文拼写错误) — 大纲引用时标注原文如此 | 标注 ✅ |
