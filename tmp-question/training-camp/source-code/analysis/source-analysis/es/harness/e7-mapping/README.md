# E-7 Mapping — harness 验证记录 (MiniMapping 21/21)

> 跑法: `javac MiniMapping.java MiniMappingTest.java && java MiniMappingTest` (JDK 21)
> 结果: **21/21 PASS** (首跑编译错误, 修复 sed 过度替换后全绿)

## 验证矩阵

| # | 机制 | 验证点 | 源码对照 | 结果 |
|:--:|---|---|---|:--:|
| A1-A5 | 类型推断 | 纯数字→LONG / 小数→DOUBLE / 日期→DATE / 文本→TEXT / 纯数字拒绝 date | DynamicFieldsBuilder.java:47-152 (推断), L75-78 (纯数字拒绝注释) | PASS |
| A6-A7 | 检测开关 | numericDetection=false / dateDetection=false 关闭推断 | DynamicFieldsBuilder.java:57,74 (索引级开关) | PASS |
| B1-B3 | dynamic=TRUE | 动态建字段 + 推断类型 + 值保留 | ObjectMapper.java:45-58 + DocumentParser.java:673-682 | PASS |
| B4 | dynamic=FALSE | 新字段忽略 | DocumentParser.java:676-679 (failIfMatchesRoutingPath 后 return) | PASS |
| B5 | dynamic=STRICT | 整篇拒绝 (StrictDynamicMappingException) | DocumentParser.java:680-683 | PASS |
| B6-B7 | dynamic=RUNTIME | 值保留但不固化具体类型 | ObjectMapper.java:53-58 (DYNAMIC_RUNTIME builder) | PASS |
| C1 | TEXT | 原样索引 | TextFieldMapper.java:1243-1265 (new Field) | PASS |
| C2 | KEYWORD | ignoreAbove 超长丢弃 | KeywordFieldMapper.java:898-901 | PASS |
| C3 | NUMBER | 字符串强转 (coerce) | NumberFieldMapper.java:1830-1851 | PASS |
| C4 | DATE | 字符串→epoch millis | DateFieldMapper.java:899-930 | PASS |
| D1-D3 | ignore_malformed | 默认 false 抛错 / true 忽略 | NumberFieldMapper.java:1833-1841 + FieldMapper.java:61-64 | PASS |

## harness 抓到的自身实现缺陷 (2 处)

1. **编译错误 (sed 过度替换)**: 批量替换 `Index.FieldDef.Type` → `MiniMapping.FieldDef.Type` 时把已带前缀的 `MiniMapping.Index.FieldDef.Type` 也替换成 `MiniMapping.MiniMapping.FieldDef.Type` — 非机制缺陷, 是工具误用 (REVIEW 教训: 批量替换后必须编译验证)
2. **A5 推断顺序**: 初版把"纯数字拒绝 date"实现为 date 分支前检查, 与真实算法一致 (Long 分支先拦截) — 测试 A5 确认了该设计意图

## 验证意义

- 类型推断算法 (Long→Double→Date→String) / dynamic 四态 / parseCreateField 四变体 / ignore_malformed 兜底链 — 4 大机制全部可复现
- **未验证面** (harness 边界): Parameter 合并冲突 (需要完整 merge 状态机, 单文件难模拟)、multi-fields 递归、动态模板 match_mapping_type、nested 对象路径
- 结论: "字段定义 → 类型分派 → 值解析 → 兜底" 的 Mapping 生命周期理解验证到位
