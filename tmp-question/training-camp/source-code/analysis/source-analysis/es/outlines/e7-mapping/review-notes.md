# E-7 Mapping — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 跨域引用核验
> **结论: 深审通过 (2 处行号偏差修复 + 裸行号 50+ 处根治)**

## 第一层: 行号偏差 (2 处)

| # | 文件 | 原写 | 实测 | 修复 |
|:--:|---|---|---|---|
| 1 | 02 大纲 | parseObjectOrNested (L275) | **L263** | ✅ |
| 2 | 02 大纲 | rethrowAsDocumentParsingException (L199-244) | **L206-244** | ✅ |

## 第二层: 机制实证 (全过)

- Q1 Parameter 五元组 (FieldMapper.java:595-660) + mergeValidator 默认规则 (L641-643) ✅
- Q2 推断顺序 Long→Double→Date→String + 纯数字拒绝注释 (DynamicFieldsBuilder.java:75-78) ✅
- Q3 text norms=true / keyword norms=false (TextFieldMapper.java:246 vs KeywordFieldMapper.java:163) ✅
- Q4 **同 key 双默认遮蔽坑**: index.mapping.coerce — FieldMapper=false (L67) / NumberFieldMapper=true (L84) / RangeFieldMapper=true (L59); Builder (L136) 静态遮蔽用本类默认 true — 实测重大发现
- Q5 preview 重读 (FieldMapper.java:207) + 降级 (L216-224) ✅
- Q6 Conflicts 聚合 (L1168-1186) + updateable 成功路径 (L798-810) ✅
- Q7 **dynamic 四态** (TRUE/FALSE/STRICT/RUNTIME) — 09 审计"三态"遗漏 RUNTIME (ObjectMapper.java:45-58) ✅
- Q8 MapperRegistry TypeParser 注册表 (L24-31) ✅

## 第三层: 编造检查 (零)

- 时空溯源断代全部 commit 日期实证: c81dc2b8b7d (2020-08-12 参数化) / a5168572d5b (2020-11-02 合并) ✅
- v0.90 手写 Builder (NumberFieldMapper.java:67-106) 与 v8 声明式对比实证 ✅

## 第四层: 覆盖缺口 (3 项 — completeness ⚠️ 已回补)

- 01-L4 可更新参数成功合并路径 → 补 setValue 覆盖 (L798-810)
- 02-L2 解析性能 → 补单遍遍历 + O(字段数)
- 03-L2 RUNTIME 设计意图 → 补 runtime field 不落盘 + 查询时计算

## 第五层: 裸行号 (铁律 9)

- 修复前: 50+ 处 `(Lxxx)` 简写 (6 个文件)
- 修复后: **0 残留** (全部改 `(File.java:line)` 格式)
- 教训: 本次沿用 E-3 教训, 大纲写完即扫, 未等 REVIEW 抓

## 第六层: 跨域引用核验

- redis/r21-db (无模式对照) / r8-persistence (RDB 序列化) / r22-expire ✅
- redisson/rd3-codec (客户端 codec 对照) ✅
- 全部 [ -d ] 验证通过; 对照声明含"同词+一句摘要" ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 修复后复核

> 目的: 修复是否落地 + 是否有新错误 (沿 E-3 REVIEW-2 教训: 修复后必须再逐锚点 grep)

### 发现 4 项

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 | ObjectMapper (L717) | 类声明 **L35** — L717 是**文件总行数**被误当行号 | 行数/行号混淆 (严重) |
| 2 | 02 大纲 | Date epoch millis (L908) + 三异常 (L910-917) | parse **L910** / catch **L911-918** | +2 |
| 3 | 03 大纲 | strict 抛错 (L688-693) | ensureNotStrict **L680-683** (抛错点 L682) | -8 |
| 4 | 01 大纲 | checkIncomingMergeType (L404-412) | 方法起始 **L402** | -2 |

### 已验证正确 (46 项抽查全过)

- Mapper.java:23 / FieldMapper.java:57,595,641,798,1168 / MapperRegistry.java:24 / MapperService.java:376 ✅
- ObjectMapper Dynamic L45-58 / DocumentParser parseDynamicValue L671-682 / ensureNotStrict 修正后 L680-683 ✅
- 推断算法: Long L51-59 / Double L62-67 / 注释 L75-78 / date L79-91 / VALUE_NUMBER L107-140 ✅
- TextFieldMapper L1243,245-246 / KeywordFieldMapper L874,898 / NumberFieldMapper L1830,84,136 ✅
- coerce 三处 L67 (FieldMapper) / L84 (Number) / L59 (Range) ✅
- v0.90 Builder L67-106,95-106 (git show 实证) / commit 日期 (c81dc2b8b7d 2020-08-12, a5168572d5b 2020-11-02) ✅

### 根因与根治

- #1 根因: 写大纲时把 wc -l 输出 (717) 当类声明行号 — **行数与行号混淆**
- #2/#4 根因: 区间包含 try 块起始行偏差
- **根治方案 (再升级)**: 大纲完成后, 对每个 `(File.java:line)` 引用跑 `grep -n "关键字" File.java` 独立验证方法起始行, 特别警惕"行数当行号" (验证: 行号 ≤ 文件实际行数 wc -l)
