# ALI-A7 Sentinel 数据源 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "支持 Nacos/Apollo/File/Redis/Zookeeper 五种数据源" | 实测 **六种** (多 Consul, DataSourcePropertiesConfiguration:49) — 数量修正 |
| 2 | 规划称 "6 种规则类型注册到对应 RuleManager" | 实测 **7 种** (多 GW_API_GROUP, RuleType:67) — 数量修正 |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "dataType 只有 json/xml" | 实测三分支含 custom (L60 CUSTOM_DATA_TYPE + L136-158 动态注册自定义 converter) |

## 审 3: 文件名/目录名推断 — 0 (全源码路径实证)

## 审 4: 跨项目概念转移 — 0

## 审 5: 覆盖率 — 0 缺漏 (单源校验/动态注册/转换器/规则落位/数据源族/错误契约 6 面全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 3 项修正, 全部落盘大纲; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | Handler:77-79 (SmartInit) + 82-88 (多源即弃) + 136-144 (custom 缺 converter 抛) + 183-186 (内置 converter 名) / AbstractProps:100-108 (七分派) / Config:129-146 (反射探测) / RuleType:42-67 (七枚举) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | register2Property ↔ Sentinel 5.9 RuleManager; 动态 Bean 注册 ↔ A6 拦截器动态注册对照 | 通过 ✅ |
| 3 | 数字自洽 | 六源/七规则/三分支/唯一 Bean 名格式全文一致 | 通过 ✅ |

## 四轮 REVIEW (2026-08-16, 全量脚本复验)

文字锚 (#7) 命中修复: 本文档域引用类补行号 (grep 实证, 见 HANDOFF §六 第 6 行); 锚点范围修正见全局记录。
